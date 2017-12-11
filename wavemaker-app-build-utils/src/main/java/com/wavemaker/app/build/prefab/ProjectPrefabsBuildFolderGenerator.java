package com.wavemaker.app.build.prefab;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.filtering.MavenFilteringException;
import org.apache.maven.shared.filtering.MavenResourcesExecution;
import org.apache.maven.shared.filtering.MavenResourcesFiltering;

import com.wavemaker.app.build.util.PrefabsPropertiesExtractor;
import com.wavemaker.commons.WMRuntimeException;
import com.wavemaker.commons.io.File;
import com.wavemaker.commons.io.Folder;
import com.wavemaker.commons.properties.PropertiesWriter;
import com.wavemaker.commons.util.WMIOUtils;

/**
 * Created by srujant on 23/8/17.
 */
public class ProjectPrefabsBuildFolderGenerator {

    private static final String ENCODING = "UTF-8";
    private static final String PREFAB_DEFAULT_BUILD_DIR = "build";
    private static final String PREFAB_DEFAULT_CONF_DIR = "config";

    private Folder prefabsFolder;
    private MavenProject mavenProject;
    private MavenResourcesFiltering mavenResourcesFiltering;
    private MavenSession mavenSession;
    private File profilePropertyFile;


    public ProjectPrefabsBuildFolderGenerator(Folder prefabsFolder, File profilePropertyFile, MavenProject mavenProject, MavenResourcesFiltering mavenResourcesFiltering, MavenSession mavenSession) {
        this.prefabsFolder = prefabsFolder;
        this.mavenProject = mavenProject;
        this.mavenResourcesFiltering = mavenResourcesFiltering;
        this.mavenSession = mavenSession;
        this.profilePropertyFile = profilePropertyFile;
    }

    public void generate() {
        List<Folder> prefabFoldersList = prefabsFolder.list().folders().fetchAll();
        Map<String, Properties> prefabProfilePropertiesMap = PrefabsPropertiesExtractor.getPrefabProfilePropertiesMap(profilePropertyFile.getContent().asInputStream());
        for (Folder prefabFolder : prefabFoldersList) {
            File tempPrefabProfilePropertiesFile = WMIOUtils.createTempFile("tempPrefabProfileProperties", ".properties");
            try {
                //  create build directory
                String prefabName = prefabFolder.getName();
                Folder prefabRootFolder = prefabsFolder.getFolder(prefabName);
                Folder prefabBuildFolder = prefabRootFolder.getFolder(PREFAB_DEFAULT_BUILD_DIR);
                prefabBuildFolder.createIfMissing();

                Properties prefabProperties = prefabProfilePropertiesMap.get(prefabName);
                List<String> filtersList = new ArrayList<>(1);
                if (prefabProperties != null) {
                    PropertiesWriter.newWriter(prefabProperties).setSansDate(true).setSortProperties(true)
                            .write(tempPrefabProfilePropertiesFile.getContent().asOutputStream());
                    filtersList.add(0, WMIOUtils.getJavaIOFile(tempPrefabProfilePropertiesFile).getAbsolutePath());
                }

                List<org.apache.maven.model.Resource> resourceList = new ArrayList();
                org.apache.maven.model.Resource resource = new org.apache.maven.model.Resource();

                //   adding prefabConfigFolder to list of resources to be filtered.
                Folder prefabConfigFolder = prefabRootFolder.getFolder(PREFAB_DEFAULT_CONF_DIR);
                resource.setTargetPath(null);
                resource.setDirectory(WMIOUtils.getJavaIOFile(prefabConfigFolder).getAbsolutePath());
                resource.setFiltering(true);
                resourceList.add(resource);
                MavenResourcesExecution mavenResourcesExecution = new MavenResourcesExecution(resourceList, WMIOUtils.getJavaIOFile(prefabBuildFolder),
                        mavenProject, ENCODING, filtersList, Collections.emptyList(), mavenSession);

                mavenResourcesFiltering.filterResources(mavenResourcesExecution);
            } catch (MavenFilteringException e) {
                throw new WMRuntimeException("Failed to filter mavenResources", e);
            } finally {
                if (tempPrefabProfilePropertiesFile != null) {
                    tempPrefabProfilePropertiesFile.delete();
                }
            }
        }
    }
}

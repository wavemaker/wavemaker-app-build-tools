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
import com.wavemaker.commons.io.Resource;
import com.wavemaker.commons.io.Resources;
import com.wavemaker.commons.io.local.LocalFile;
import com.wavemaker.commons.io.local.LocalFolder;
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
    private List<String> filtersList;


    public ProjectPrefabsBuildFolderGenerator(Folder prefabsFolder, File profilePropertyFile, MavenProject mavenProject, MavenResourcesFiltering mavenResourcesFiltering, MavenSession mavenSession) {
        this.prefabsFolder = prefabsFolder;
        this.mavenProject = mavenProject;
        this.mavenResourcesFiltering = mavenResourcesFiltering;
        this.mavenSession = mavenSession;
        this.profilePropertyFile = profilePropertyFile;
        this.filtersList = new ArrayList<>(1);
    }

    public void generate() {
        Resources<Resource> resources = prefabsFolder.list();
        Map<String, Properties> prefabProfilePropertiesMap = PrefabsPropertiesExtractor.getPrefabProfilePropertiesMap(profilePropertyFile.getContent().asInputStream());
        File tempPrefabProfilePropertiesFile = WMIOUtils.createTempFile("tempPrefabProfileProperties", ".properties");
        try {
            for (Resource resource : resources) {
                if (resource instanceof Folder) {
                    //  create build directory
                    String prefabName = resource.getName();
                    Folder prefabRootFolder = prefabsFolder.getFolder(prefabName);
                    Folder prefabBuildFolder = prefabRootFolder.getFolder(PREFAB_DEFAULT_BUILD_DIR);
                    prefabBuildFolder.createIfMissing();

                    PropertiesWriter.newWriter(prefabProfilePropertiesMap.get(prefabName)).setSansDate(true).setSortProperties(true)
                            .write(tempPrefabProfilePropertiesFile.getContent().asOutputStream());
                    filtersList.add(0, ((LocalFile) tempPrefabProfilePropertiesFile).getLocalFile().getAbsolutePath());

                    //  setting output directory of filtered resources to build folder
                    String outputDirectory = ((LocalFolder) prefabBuildFolder).getLocalFile().getAbsolutePath();

                    List<org.apache.maven.model.Resource> resourceList = new ArrayList();
                    org.apache.maven.model.Resource resourceToBeFiltered = new org.apache.maven.model.Resource();

                    //   adding prefabConfigFolder to list of resources to be filtered.
                    Folder prefabConfigFolder = prefabRootFolder.getFolder(PREFAB_DEFAULT_CONF_DIR);
                    String absolutePath = ((LocalFolder) prefabConfigFolder).getLocalFile().getAbsolutePath();
                    resourceToBeFiltered.setTargetPath(null);
                    resourceToBeFiltered.setDirectory(absolutePath);
                    resourceToBeFiltered.setFiltering(true);
                    resourceList.add(resourceToBeFiltered);
                    MavenResourcesExecution mavenResourcesExecution = new MavenResourcesExecution(resourceList, new java.io.File(outputDirectory),
                            mavenProject, ENCODING, filtersList, Collections.emptyList(), mavenSession);

                    mavenResourcesFiltering.filterResources(mavenResourcesExecution);
                }
            }
        } catch (MavenFilteringException e) {
            throw new WMRuntimeException("Failed to filter mavenResources", e);
        } finally {
            if (tempPrefabProfilePropertiesFile != null) {
                tempPrefabProfilePropertiesFile.delete();
            }
        }
    }
}

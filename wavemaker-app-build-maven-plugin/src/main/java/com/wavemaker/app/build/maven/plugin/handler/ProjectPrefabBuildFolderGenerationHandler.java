package com.wavemaker.app.build.maven.plugin.handler;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.filtering.MavenResourcesFiltering;

import com.wavemaker.app.build.prefab.ProjectPrefabsBuildFolderGenerator;
import com.wavemaker.commons.io.File;
import com.wavemaker.commons.io.Folder;

/**
 * Created by srujant on 23/8/17.
 */
public class ProjectPrefabBuildFolderGenerationHandler implements AppBuildHandler {
    private Folder prefabsFolder;
    private File profilePropertiesFile;
    private MavenProject mavenProject;
    private MavenResourcesFiltering mavenResourcesFiltering;
    private MavenSession mavenSession;

    public ProjectPrefabBuildFolderGenerationHandler(MavenProject mavenProject, Folder prefabsFolder, File profilePropertiesFile, MavenResourcesFiltering mavenResourcesFiltering, MavenSession mavenSession) {
        this.mavenProject = mavenProject;
        this.prefabsFolder = prefabsFolder;
        this.profilePropertiesFile = profilePropertiesFile;
        this.mavenResourcesFiltering = mavenResourcesFiltering;
        this.mavenSession = mavenSession;
    }

    @Override
    public void handle() {
        ProjectPrefabsBuildFolderGenerator projectPrefabsBuildFolderGenerator = new ProjectPrefabsBuildFolderGenerator(prefabsFolder, profilePropertiesFile,
                mavenProject,mavenResourcesFiltering,mavenSession );
        projectPrefabsBuildFolderGenerator.generate();
    }
}

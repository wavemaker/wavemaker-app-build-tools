package com.wavemaker.app.build.maven.plugin.handler;

import com.wavemaker.app.build.validations.ProjectDbValidationsGenerator;
import com.wavemaker.commons.io.Folder;

/**
 * Created by srujant on 23/8/17.
 */
public class ProjectDbValidationsGenerationHandler implements AppBuildHandler {

    private Folder rootFolder;

    public ProjectDbValidationsGenerationHandler(Folder rootFolder) {
        this.rootFolder = rootFolder;
    }

    @Override
    public void handle() {
        ProjectDbValidationsGenerator projectDbValidationsGenerator = new ProjectDbValidationsGenerator(rootFolder);
        projectDbValidationsGenerator.generate();
    }
}

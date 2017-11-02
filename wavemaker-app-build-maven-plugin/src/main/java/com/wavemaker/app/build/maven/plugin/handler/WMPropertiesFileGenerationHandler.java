package com.wavemaker.app.build.maven.plugin.handler;

import com.wavemaker.app.build.i18n.WMPropertiesFileGenerator;
import com.wavemaker.commons.io.Folder;

/**
 * @author Kishore Routhu on 1/11/17 4:12 PM.
 */
public class WMPropertiesFileGenerationHandler implements AppBuildHandler {

    private Folder rootFolder;
    private Folder buildFolder;
    private Folder localeFolder;

    public WMPropertiesFileGenerationHandler(Folder rootFolder, Folder buildFolder, Folder localeFolder) {
        this.rootFolder = rootFolder;
        this.buildFolder = buildFolder;
        this.localeFolder = localeFolder;
    }

    @Override
    public void handle() {
        WMPropertiesFileGenerator propertiesFileGenerator = new WMPropertiesFileGenerator(rootFolder.getName(), rootFolder, buildFolder, localeFolder);
        propertiesFileGenerator.generate();
    }
}

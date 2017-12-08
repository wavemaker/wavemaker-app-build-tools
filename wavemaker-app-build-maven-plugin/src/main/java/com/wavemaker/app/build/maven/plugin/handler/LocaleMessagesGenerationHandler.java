package com.wavemaker.app.build.maven.plugin.handler;

import com.wavemaker.app.build.i18n.LocaleMessagesGenerator;
import com.wavemaker.commons.io.Folder;

/**
 * @author Kishore Routhu on 4/7/17 6:58 PM.
 */
public class LocaleMessagesGenerationHandler implements AppBuildHandler {


    private Folder rootFolder;

    private Folder outputFolder;

    public LocaleMessagesGenerationHandler(Folder rootFolder, Folder localeOutputFolder) {
        this.rootFolder = rootFolder;
        this.outputFolder = localeOutputFolder;
    }

    @Override
    public void handle()  {
        LocaleMessagesGenerator localeMessagesGenerator = new LocaleMessagesGenerator(rootFolder, outputFolder);
        localeMessagesGenerator.generate();
    }
}

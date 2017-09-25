package com.wavemaker.app.build.i18n;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.io.FilenameUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.wavemaker.commons.WMRuntimeException;
import com.wavemaker.commons.io.File;
import com.wavemaker.commons.io.Folder;
import com.wavemaker.commons.io.Resources;
import com.wavemaker.commons.json.JSONUtils;

/**
 * @author Kishore Routhu on 4/7/17 6:56 PM.
 */
public class LocaleMessagesGenerator {

    /**
     * used to identify if a lang file need to be regenerated
     */
    private static long SYSTEM_START_TIME = System.currentTimeMillis();

    private static final String JSON_EXTENSION = ".json";

    private Folder localeFolder;

    private Folder outputFolder;

    public LocaleMessagesGenerator(Folder localeFolder, Folder outputFolder) {
        this.localeFolder = localeFolder;
        this.outputFolder = outputFolder;
    }

    public void generate() {
        Resources<File> files = localeFolder.list().files();
        List<String> configuredLocales = new ArrayList<>();
        for (File file : files) {
            String language = FilenameUtils.removeExtension(file.getName());
            configuredLocales.add(language);
            File outputFile = outputFolder.getFile(language + JSON_EXTENSION);
            if (shouldBeRegenerated(file, outputFile)) {
                mergedLocaleResource(language, file, outputFile);
            }
        }
        addMissingSystemMessages(configuredLocales);
    }

    private boolean shouldBeRegenerated(File file, File outputFile) {
        return (!outputFile.exists() || outputFile.getLastModified() < file.getLastModified() || outputFile.getLastModified() < SYSTEM_START_TIME);
    }

    private void mergedLocaleResource(String locale, File file, File outputFile) {
        Map<String, String> systemLocaleMessages = SystemLocaleContext.getSystemLocaleMessages(locale);
        if (systemLocaleMessages == null) {
            systemLocaleMessages = SystemLocaleContext.getDefaultLanguageLocaleMessages();
        }

        Map<String, String> newLocaleMessages = new TreeMap<>(systemLocaleMessages);
        Map<String, String> localeMessages = readLocaleMessages(file);
        newLocaleMessages.putAll(localeMessages);

        outputFile.createIfMissing();
        writeLocaleMessages(outputFile, newLocaleMessages);
    }

    private void addMissingSystemMessages(List<String> configuredLocales) {
        String[] locales = SystemLocaleContext.getDefaultLanguagesToAdd();
        for (String locale : locales) {
            if (!configuredLocales.contains(locale)) {
                File outputFile = outputFolder.getFile(locale + JSON_EXTENSION);
                if (outputFile.exists() && SYSTEM_START_TIME <= outputFile.getLastModified()) {
                    continue;
                }
                outputFile.createIfMissing();
                writeLocaleMessages(outputFile, SystemLocaleContext.getSystemLocaleMessages(locale));
            }
        }
    }

    private Map<String, String> readLocaleMessages(File file) {
        try {
            return JSONUtils.toObject(file.getContent().asInputStream(), new TypeReference<Map<String, String>>() {
            });
        } catch (IOException e) {
            throw new WMRuntimeException("Failed to read messages from file " + file.getName(), e);
        }
    }

    private void  writeLocaleMessages(File file, Map<String, String> messages) {
        try {
            JSONUtils.toJSON(file.getContent().asOutputStream(), messages);
        } catch (IOException e) {
            throw new WMRuntimeException("Failed to write messages to file " + file.getName(), e);
        }
    }
}

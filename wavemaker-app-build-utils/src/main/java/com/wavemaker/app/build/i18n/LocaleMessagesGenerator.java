package com.wavemaker.app.build.i18n;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.io.FilenameUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.wavemaker.commons.WMRuntimeException;
import com.wavemaker.commons.io.File;
import com.wavemaker.commons.io.Folder;
import com.wavemaker.commons.io.LatestLastModified;
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
    private static final String EN = "en";
    private static final String MISMATCH_LOCALE = "mismatchLocale";
    private static final String I18N = "i18n";

    private Folder rootFolder;
    private Folder outputFolder;

    public LocaleMessagesGenerator(Folder rootFolder, Folder outputFolder) {
        this.rootFolder = rootFolder;
        this.outputFolder = outputFolder;
    }

    public void generate() {
        Map<String, List<File>> configuredLocaleMap = getProjectLocalesMap();
        List<String> configuredLocales = new ArrayList<>();
        for (Map.Entry<String, List<File>> entry : configuredLocaleMap.entrySet()) {
            String language = entry.getKey();
            configuredLocales.add(language);
            File outputFile = outputFolder.getFile(language + JSON_EXTENSION);
            if (shouldBeRegenerated(entry.getValue(), outputFile)) {
                mergedLocaleResource(language, entry.getValue(), outputFile);
            }
        }
        addMissingSystemMessages(configuredLocales);
    }


    private Map<String, List<File>> getProjectLocalesMap() {
        List<String> projectLocales = getProjectLocaleNames();
        Map<String, List<File>> configuredLocalesMap = new LinkedHashMap<>();
        //Add all prefab locale files to map
        updateLocalesConfiguredMapForPrefabs(projectLocales, configuredLocalesMap);
        // Adds project locale files to map
        Folder localeFolder = rootFolder.getFolder(I18N);
        Resources<File> localeFiles = localeFolder.list().files();
        for (File file : localeFiles) {
            String language = FilenameUtils.removeExtension(file.getName());
            List<File> locales = configuredLocalesMap.get(language);
            if (locales == null) {
                locales = new ArrayList<>();
                configuredLocalesMap.put(language, locales);
            }
            locales.add(file);
        }
        return configuredLocalesMap;
    }


    private List<String> getProjectLocaleNames() {
        List<String> configuredLocales = new ArrayList<>();
        Folder localeFolder = rootFolder.getFolder("i18n");
        Resources<File> localeFiles = localeFolder.list().files();
        for (File file : localeFiles) {
            String language = FilenameUtils.removeExtension(file.getName());
            configuredLocales.add(language);
        }
        return configuredLocales;
    }

    private void updateLocalesConfiguredMapForPrefabs(List<String> projectLocales, Map<String, List<File>> configuredLocalesMap) {
        Folder prefabsFolder = rootFolder.getFolder("src/main/webapp/WEB-INF/prefabs");
        Resources<Folder> prefabsFolderList = prefabsFolder.list().folders();
        for (Folder prefabFolder : prefabsFolderList) {
            Folder localeFolder = prefabFolder.getFolder("webapp/resources/i18n");
            for (String locale : projectLocales) {
                File localeFile = localeFolder.getFile(locale + JSON_EXTENSION);
                if (!localeFile.exists()) {
                    localeFile = localeFolder.getFile(EN + JSON_EXTENSION);
                }
                if (localeFile.exists()) {
                    if (configuredLocalesMap.get(locale) == null) {
                        configuredLocalesMap.put(locale, new ArrayList<>());
                    }
                    configuredLocalesMap.get(locale).add(localeFile);
                }
            }
        }
    }

    private boolean shouldBeRegenerated(List<File> files, File outputFile) {
        if (!outputFile.exists() || outputFile.getLastModified() < SYSTEM_START_TIME) {
            return true;
        }
        LatestLastModified latestLastModified = new LatestLastModified();
        for (File file : files) {
            file.performOperation(latestLastModified);
        }
        return latestLastModified.getValue() > outputFile.getLastModified();
    }

    private void mergedLocaleResource(String locale, List<File> files, File outputFile) {
        Map<String, String> systemLocaleMessages = SystemLocaleContext.getSystemLocaleMessages(locale);
        if (systemLocaleMessages == null) {
            systemLocaleMessages = SystemLocaleContext.getDefaultLanguageLocaleMessages();
        }
        Map<String, String> finalLocaleMessages = new TreeMap<>(systemLocaleMessages);
        for (File file : files) {
            Map<String, String> localeMessages = readLocaleMessages(file);
            finalLocaleMessages.putAll(localeMessages);
        }
        outputFile.createIfMissing();
        writeLocaleMessages(outputFile, finalLocaleMessages);
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

    private void writeLocaleMessages(File file, Map<String, String> messages) {
        try {
            JSONUtils.toJSON(file.getContent().asOutputStream(), messages);
        } catch (IOException e) {
            throw new WMRuntimeException("Failed to write messages to file " + file.getName(), e);
        }
    }
}

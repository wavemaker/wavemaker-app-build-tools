package com.wavemaker.app.build.i18n;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.wavemaker.commons.WMRuntimeException;
import com.wavemaker.commons.io.File;
import com.wavemaker.commons.io.Folder;
import com.wavemaker.commons.json.JSONUtils;
import com.wavemaker.commons.util.PropertiesFileUtils;

/**
 * @author Kishore Routhu on 1/11/17 2:31 PM.
 */
public class WMPropertiesFileGenerator {

    private static final Logger logger = LoggerFactory.getLogger(WMPropertiesFileGenerator.class);

    private static final String SECURITY_SERVICE_DESIGNTIME_DIR = "/services/securityService/designtime/";
    private static final String GENERAL_OPTIONS_FILE = "general-options.json";
    private static final String PROJECT_PROPERTIES = ".wmproject.properties";

    private String[] uiProperties = new String[]{
            "version",
            "defaultLanguage",
            "type",
            "homePage",
            "platformType",
            "activeTheme",
            "displayName",
            "dateFormat",
            "timeFormat" };

    private String projectName;
    private Folder rootFolder;
    private Folder buildFolder;
    private Folder localeFolder;

    public WMPropertiesFileGenerator(String projectName, Folder rootFolder, Folder buildFolder, Folder localeFolder) {
        this.projectName = projectName;
        this.rootFolder = rootFolder;
        this.buildFolder = buildFolder;
        this.localeFolder = localeFolder;
    }

    public void generate() {
        File propertiesFile = rootFolder.getFile(PROJECT_PROPERTIES);
        Map<String, String> propertiesMap = new TreeMap<>();
        if (propertiesFile.exists()) {
            InputStream is = propertiesFile.getContent().asInputStream();
            Properties properties = PropertiesFileUtils.loadFromXml(is);
            propertiesMap.put("name", projectName);
            for (String property : uiProperties) {
                if (properties.containsKey(property)) {
                    propertiesMap.put(property, properties.getProperty(property));
                }
            }
        }

        boolean enforceSecurity = isSecurityEnabled(rootFolder);
        propertiesMap.put("securityEnabled", String.valueOf(enforceSecurity));
        propertiesMap.put("supportedLanguages", getSupportedLocales(localeFolder));

        StringBuilder sb = new StringBuilder();
        sb.append("var _WM_APP_PROPERTIES = ");
        try {
            String jsonString = JSONUtils.toJSON(propertiesMap, true);
            sb.append(jsonString).append(";");
        } catch (IOException e) {
            throw new WMRuntimeException("Failed to convert to json", e);
        }
        buildFolder.getFile("wmProperties.js").getContent().write(sb.toString());
    }


    private boolean isSecurityEnabled(Folder rootFolder) {
        Folder securityService = rootFolder.getFolder(SECURITY_SERVICE_DESIGNTIME_DIR);
        if (securityService.exists()) {
            File generalOptionsFile = securityService.getFile(GENERAL_OPTIONS_FILE);
            if (generalOptionsFile.exists()) {
                JsonNode jsonNode = JSONUtils.readTree(generalOptionsFile.getContent().asInputStream());
                if (jsonNode.has("enforceSecurity")) {
                    BooleanNode enforceSecurityNode = (BooleanNode) jsonNode.get("enforceSecurity");
                    return enforceSecurityNode.asBoolean();
                } else {
                    logger.warn("Missing field enforeSecurity in the file {}", GENERAL_OPTIONS_FILE);
                }
            }
        }
        return false;
    }

    private String getSupportedLocales(Folder localeFolder) {
        List<File> localeFiles = localeFolder.list().files().fetchAll();
        String supportedLocales = localeFiles.stream()
                .map(file -> FilenameUtils.removeExtension(file.getName()))
                .collect(Collectors.joining(","));
        return StringUtils.isBlank(supportedLocales) ? "en" : supportedLocales;
    }

}

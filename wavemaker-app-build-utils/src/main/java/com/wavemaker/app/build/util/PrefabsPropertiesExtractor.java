package com.wavemaker.app.build.util;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.wavemaker.commons.util.PropertiesFileUtils;

/**
 * Created by srujant on 29/11/17.
 */
public class PrefabsPropertiesExtractor {

    private static Pattern pattern = Pattern.compile("prefab.([^.]+).(.*)");

    //   Creates a map between prefabName and it's corresponding profile properties
    public static Map<String, Properties> getPrefabProfilePropertiesMap(InputStream inputStream) {
        Properties profileProperties = PropertiesFileUtils.loadProperties(inputStream);
        Map<String, Properties> prefabProfilePropertiesMap = new LinkedHashMap<>();
        for (Map.Entry<Object, Object> entry : profileProperties.entrySet()) {
            Matcher matcher = pattern.matcher((String) entry.getKey());
            if (matcher.find()) {
                String prefabName = matcher.group(1);
                Properties properties = prefabProfilePropertiesMap.get(prefabName);
                if (properties == null) {
                    properties = new Properties();
                    prefabProfilePropertiesMap.put(prefabName, properties);
                }
                properties.put(matcher.group(2), entry.getValue());
            }
        }
        return prefabProfilePropertiesMap;
    }
}

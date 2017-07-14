package com.wavemaker.app.build.i18n;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FilenameUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.wavemaker.commons.WMRuntimeException;
import com.wavemaker.commons.classloader.ClassLoaderUtils;
import com.wavemaker.commons.json.JSONUtils;

/**
 * @author Kishore Routhu on 21/6/17 4:49 PM.
 */
public final class SystemLocaleContext {

    private static String DEFAULT_LOCALE = "en";
    private static String SYSTEM_I18N_RESOURCES_DIR = "com/wavemaker/runtime/i18n";

    private static List<String> SYSTEM_SUPPORTED_LOCALES;
    private static Cache<String, Map<String, String>> systemMessagesCache = CacheBuilder.newBuilder().expireAfterAccess(5, TimeUnit.MINUTES).build();

    static {
        SYSTEM_SUPPORTED_LOCALES = new ArrayList<>();
        init();
    }

    public static Map<String, String> getSystemLocaleMessages(String locale) {
        Map<String, String> systemMessages = systemMessagesCache.getIfPresent(locale);
        if (systemMessages == null) {
            synchronized (systemMessagesCache) {
                systemMessages = systemMessagesCache.getIfPresent(locale);
                if (systemMessages == null) {
                    InputStream inputStream = ClassLoaderUtils.getResourceAsStream(SYSTEM_I18N_RESOURCES_DIR + "/" + locale + ".json");
                    if (inputStream != null) {
                        systemMessages = getSystemLocaleMessages(inputStream);
                        systemMessagesCache.put(locale, systemMessages);
                    }
                }
            }
        }
        return systemMessages;
    }

    public static Map<String, String> getDefaultLanguageLocaleMessages() {
        Map<String, String> systemLocaleMessages = getSystemLocaleMessages(DEFAULT_LOCALE);
        if (systemLocaleMessages == null) {
            throw new IllegalStateException();
        }
        return systemLocaleMessages;
    }

    public static List<String> getAllSystemLocales() {
        return SYSTEM_SUPPORTED_LOCALES;
    }

    private static Map<String, String> getSystemLocaleMessages(InputStream inputStream) {
        try {
            return JSONUtils.toObject(inputStream, new TypeReference<Map<String, String>>() {
            });
        } catch (IOException e) {
            throw new WMRuntimeException("Failed to read messages", e);
        }
    }

    private static void init() {
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources(SYSTEM_I18N_RESOURCES_DIR + "/*.json");
            for (Resource resource : resources) {
                String locale = FilenameUtils.removeExtension(resource.getFilename());
                SYSTEM_SUPPORTED_LOCALES.add(locale);
            }
        } catch (IOException e) {
            throw new WMRuntimeException("Failed to load locales", e);
        }
    }
}

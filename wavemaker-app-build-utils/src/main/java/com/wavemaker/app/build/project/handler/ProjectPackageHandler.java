package com.wavemaker.app.build.project.handler;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import com.wavemaker.app.build.project.model.AppPackageConfig;
import com.wavemaker.commons.WMRuntimeException;
import com.wavemaker.commons.io.DeleteTempFileOnCloseInputStream;
import com.wavemaker.commons.io.File;
import com.wavemaker.commons.io.FilterOn;
import com.wavemaker.commons.io.Folder;
import com.wavemaker.commons.io.ResourceFilter;
import com.wavemaker.commons.io.local.LocalFolder;
import com.wavemaker.commons.util.WMIOUtils;
import com.wavemaker.commons.zip.ZipArchive;


/**
 * Created by kishore on 16/3/17.
 */
public class ProjectPackageHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProjectPackageHandler.class);

    private AppPackageConfig appPackageConfig;

    public ProjectPackageHandler(AppPackageConfig appPackageConfig) {
        this.appPackageConfig = appPackageConfig;
    }

    public InputStream exportAsZipInputStream(CustomProjectPackageHandlerCallback customProjectPackageHandlerCallback) {
        Folder packageFolder = null;
        try {
            packageFolder = WMIOUtils.createTempFolder();
            exportIntoFolder(customProjectPackageHandlerCallback, packageFolder);

            java.io.File zipFile = java.io.File.createTempFile("projectExport", ".zip");
            compressToZip(packageFolder, zipFile);
            return new DeleteTempFileOnCloseInputStream(zipFile);
        } catch (Exception e) {
            throw new WMRuntimeException(e);
        } finally {
            WMIOUtils.deleteResourceSilently(packageFolder);
        }
    }

    public void exportIntoFolder(CustomProjectPackageHandlerCallback customProjectPackageHandlerCallback, Folder packageFolder) {
        List<String> ignorePatterns = readIgnorePatterns();
        addExtraIgnorePatterns(ignorePatterns);

        String antPatterns[] = new String[ignorePatterns.size()];
        ignorePatterns.toArray(antPatterns);
        try {
            if (packageFolder.exists()) {
                FileUtils.cleanDirectory(((LocalFolder) packageFolder).getLocalFile());
            }
            ResourceFilter excludeFilter = FilterOn.antPattern(antPatterns);
            appPackageConfig.getBasedir().find().files().exclude(excludeFilter).copyTo(packageFolder);

            if (customProjectPackageHandlerCallback != null) {
                customProjectPackageHandlerCallback.doPackage(packageFolder);
            }
        } catch (Exception e) {
            throw new WMRuntimeException(e);
        }
    }

    private void addExtraIgnorePatterns(List<String> ignorePatterns) {
        List<String> extraPatterns = appPackageConfig.getExtraIgnorePatterns();
        if (!CollectionUtils.isEmpty(extraPatterns)) {
            ignorePatterns.addAll(extraPatterns);
        }
    }

    private List<String> readIgnorePatterns() {
        List<String> ignorePatterns = new ArrayList<>();
        if (StringUtils.isNotBlank(appPackageConfig.getIgnorePatternFile())) {
            BufferedReader br = null;
            try {
                File ignoreFile = appPackageConfig.getBasedir().getFile(appPackageConfig.getIgnorePatternFile());
                if (!ignoreFile.exists()) {
                    LOGGER.error("{} not found at location {}", appPackageConfig.getIgnorePatternFile(), appPackageConfig.getBasedir());
                    throw new WMRuntimeException(appPackageConfig.getIgnorePatternFile() + " not found at location " + appPackageConfig.getBasedir());
                }
                br = new BufferedReader(ignoreFile.getContent().asReader());
                String pattern = null;
                while ((pattern = br.readLine()) != null) {
                    if (StringUtils.isNotBlank(pattern)) {
                        ignorePatterns.add(pattern);
                    }
                }
            } catch (FileNotFoundException e) {
                LOGGER.error("IgnorePatternFile {} not found ", appPackageConfig.getIgnorePatternFile(), e);
                throw new WMRuntimeException("IgnorePatternFile " + appPackageConfig.getIgnorePatternFile() + "not found", e);
            } catch (IOException e) {
                throw new WMRuntimeException(e);
            } finally {
                WMIOUtils.closeSilently(br);
            }
        } else {
            LOGGER.warn("IgnorePatterFile was not provided, it will copy everything to the target");
        }
        return ignorePatterns;
    }

    private void compressToZip(Folder sourceFolder, java.io.File zipFile) {
        InputStream zipInputStream = null;
        OutputStream zipOutputStream = null;
        try {
            zipInputStream = ZipArchive.compress(sourceFolder.find().files());
            zipOutputStream = new FileOutputStream(zipFile);
            WMIOUtils.copy(zipInputStream, zipOutputStream);
        } catch (FileNotFoundException e) {
            throw new WMRuntimeException("FileNotFound " + zipFile.getAbsolutePath(), e);
        } catch (IOException e) {
            throw new WMRuntimeException(e);
        } finally {
            WMIOUtils.closeSilently(zipInputStream);
            WMIOUtils.closeSilently(zipOutputStream);
        }
    }

    public interface CustomProjectPackageHandlerCallback {
        void doPackage(Folder packageFolder) throws Exception;
    }

    public static class NoOpProjectPackageHandlerCallback implements CustomProjectPackageHandlerCallback {
        public void doPackage(Folder packageFolder) {
        }
    }
}

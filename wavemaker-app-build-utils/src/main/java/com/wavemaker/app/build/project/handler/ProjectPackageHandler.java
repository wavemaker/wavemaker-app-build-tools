package com.wavemaker.app.build.project.handler;

import java.io.*;
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
import com.wavemaker.commons.util.IOUtils;
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

    public InputStream pack(CustomProjectPackageHandlerCallback customProjectPackageHandlerCallback) {
        List<String> ignorePatterns = readIgnorePatterns();
        addExtraIgnorePatterns(ignorePatterns);

        String antPatterns[] = new String[ignorePatterns.size()];
        ignorePatterns.toArray(antPatterns);

        java.io.File tempDirectory = null;
        try {
            tempDirectory = IOUtils.createTempDirectory();
            Folder packageFolder = new LocalFolder(tempDirectory);
            if (packageFolder.exists()) {
                FileUtils.cleanDirectory(((LocalFolder) packageFolder).getLocalFile());
            }
            ResourceFilter excludeFilter = FilterOn.antPattern(antPatterns);
            appPackageConfig.getBasedir().find().files().exclude(excludeFilter).copyTo(packageFolder);

            if (customProjectPackageHandlerCallback != null) {
                customProjectPackageHandlerCallback.doPackage(packageFolder);
            }

            java.io.File zipFile = java.io.File.createTempFile("projectExport", ".zip");
            compressToZip(packageFolder, zipFile);
            return new DeleteTempFileOnCloseInputStream(zipFile);
        } catch (Exception e) {
            throw new WMRuntimeException(e);
        } finally {
            IOUtils.deleteDirectorySilently(tempDirectory);
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
                IOUtils.closeSilently(br);
            }
        } else {
            LOGGER.warn("IgnorePatterFile was not provided, it will copy everything to the target");
        }
        return ignorePatterns;
    }

    private void compressToZip(Folder sourceFolder, java.io.File zipFile) {
        try {
            InputStream zipInputStream = ZipArchive.compress(sourceFolder.find().files());
            OutputStream zipOutputStream = new FileOutputStream(zipFile);
            IOUtils.copy(zipInputStream, zipOutputStream, true, true);
        } catch (FileNotFoundException e) {
            throw new WMRuntimeException("FileNotFound " + zipFile.getAbsolutePath(), e);
        } catch (IOException e) {
            throw new WMRuntimeException(e);
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
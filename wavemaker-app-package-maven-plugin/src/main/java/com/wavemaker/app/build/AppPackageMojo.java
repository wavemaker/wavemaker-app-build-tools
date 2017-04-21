package com.wavemaker.app.build;

import java.io.*;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import com.wavemaker.app.build.project.handler.ProjectPackageHandler;
import com.wavemaker.app.build.project.model.AppPackageConfig;
import com.wavemaker.commons.io.Folder;
import com.wavemaker.commons.io.local.LocalFolder;
import com.wavemaker.commons.util.IOUtils;

/**
 * Created by kishore on 16/3/17.
 */
@Mojo(name = "package-app", defaultPhase = LifecyclePhase.PREPARE_PACKAGE, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class AppPackageMojo extends AbstractMojo {


    @Parameter(name = "basedir", readonly = true, defaultValue = "${basedir}")
    private String basedir;

    @Parameter(name = "targetDir", defaultValue = "target/export")
    private String targetDir;

    @Parameter(name = "projectName", defaultValue = "${project.name}")
    private String projectName;

    @Parameter(name = "ignorePatternFile", defaultValue = ".gitignore")
    private String ignorePatternFile;

    @Parameter(name = "extraIgnorePatterns")
    private List<String> extraIgnorePatterns;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        Folder baseFolder = new LocalFolder(basedir);

        AppPackageConfig appPackageConfig = new AppPackageConfig.Builder()
                .basedir(baseFolder)
                .ignorePatternFile(ignorePatternFile)
                .extraIgnorePatterns(extraIgnorePatterns).build();

        File zipFile = null;
        InputStream inputStream = null;
        try {
            ProjectPackageHandler projectPackageHandler = new ProjectPackageHandler(appPackageConfig);

            inputStream = projectPackageHandler.pack(new ProjectPackageHandler.NoOpProjectPackageHandlerCallback());

            zipFile = createZipFile();
            getLog().info("Creating " + zipFile + " from the " + basedir);
            IOUtils.copy(inputStream, new FileOutputStream(zipFile), true, true);
        } catch (FileNotFoundException e) {
            throw new MojoExecutionException("File not found " + zipFile, e);
        } catch (IOException e) {
            throw new MojoExecutionException("Failed copy to zip file " + zipFile, e);
        } finally {
            IOUtils.closeSilently(inputStream);
        }
    }


    private File createTempDirectory() throws MojoExecutionException {
        try {
            return IOUtils.createTempDirectory();
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to create temp directory", e);
        }
    }

    private File createZipFile() throws MojoExecutionException {
        String zipFilePath = targetDir + "/" + projectName + ".zip";
        try {
            new File(targetDir).mkdirs();
            return IOUtils.createFile(zipFilePath);
        } catch (IOException e) {
            getLog().error(e);
            throw new MojoExecutionException("Failed to create zip file " + zipFilePath, e);
        }
    }


}

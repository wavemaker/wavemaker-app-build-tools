/**
 * Copyright © 2013 - 2017 WaveMaker, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.wavemaker.app.build.maven.plugin.mojo;

import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Build;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.filtering.MavenFilteringException;
import org.apache.maven.shared.filtering.MavenResourcesExecution;
import org.apache.maven.shared.filtering.MavenResourcesFiltering;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.wavemaker.app.build.maven.plugin.handler.AppBuildHandler;
import com.wavemaker.app.build.maven.plugin.handler.LocaleMessagesGenerationHandler;
import com.wavemaker.app.build.maven.plugin.handler.PageMinFileGenerationHandler;
import com.wavemaker.app.build.maven.plugin.handler.ProjectDbValidationsGenerationHandler;
import com.wavemaker.app.build.maven.plugin.handler.ProjectPrefabBuildFolderGenerationHandler;
import com.wavemaker.app.build.maven.plugin.handler.SwaggerDocGenerationHandler;
import com.wavemaker.app.build.maven.plugin.handler.VariableServiceDefGenerationHandler;
import com.wavemaker.app.build.maven.plugin.handler.WMPropertiesFileGenerationHandler;
import com.wavemaker.commons.WMRuntimeException;
import com.wavemaker.commons.io.File;
import com.wavemaker.commons.io.Folder;
import com.wavemaker.commons.io.local.LocalFolder;
import com.wavemaker.commons.util.WMIOUtils;

/**
 * Created by saddhamp on 12/4/16.
 */
@Mojo(name = "generate", defaultPhase = LifecyclePhase.PREPARE_PACKAGE, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class AppBuildMojo extends AbstractMojo {

    public static final String ENCODING = "UTF-8";
    public static final String MAVEN_RESOURCES_PLUGIN = "maven-resources-plugin";
    private static final String NON_FILTERED_FILE_EXTENSIONS = "nonFilteredFileExtensions";
    private static final String PROFILE_PROPERTY_FILE = "profile.props.file";

    @Parameter(property = "project", required = true, readonly = true)
    private MavenProject project;

    @Parameter(property = "basedir", required = true, readonly = true)
    private String baseDirectory;


    @Parameter(name = "pages-directory", defaultValue = "src/main/webapp/pages/")
    private String pagesDirectory;

    @Parameter(name = "services-directory", defaultValue = "services")
    private String servicesDirectory;

    @Parameter(name = "locale-directory", defaultValue = "i18n")
    private String localeDirectory;

    @Parameter(name = "outputDirectory", defaultValue = "target/classes")
    private String outputDirectory;

    @Parameter(name = "localeOutputDirectory", defaultValue = "src/main/webapp/resources/i18n/")
    private String localeOutputDirectory;

    @Parameter(name = "webAppDirectory", defaultValue = "src/main/webapp/")
    private String webAppDirectory;

    @Parameter(defaultValue = "${session}")
    private MavenSession session;

    @Parameter(name = "prefabs-directory", defaultValue = "src/main/webapp/WEB-INF/prefabs")
    private String prefabsDirectory;

    @Component
    private MavenResourcesFiltering mavenResourcesFiltering;

    private List<AppBuildHandler> appBuildHandlers;

    private static final Logger logger = LoggerFactory.getLogger(AppBuildMojo.class);

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            doExecute();
        } catch (Exception e) {
            logger.warn(e.getMessage(), e);
            throw new WMRuntimeException(e);
        }
    }

    private void doExecute() throws MojoFailureException {
        Folder rootFolder = new LocalFolder(baseDirectory);
        String profilePropertyName = (String) project.getProperties().get(PROFILE_PROPERTY_FILE);
        initializeHandlers(rootFolder, profilePropertyName);

        for (AppBuildHandler appBuildHandler : appBuildHandlers) {
            appBuildHandler.handle();
        }

        final Build build = project.getBuild();
        final List<String> nonFilteredFileExtensions = getNonFilteredFileExtensions(build.getPlugins());

        Folder outputFolder = rootFolder.getFolder(outputDirectory);
        MavenResourcesExecution mavenResourcesExecution =
                new MavenResourcesExecution(build.getResources(), WMIOUtils.getJavaIOFile(outputFolder), project,
                        ENCODING, build.getFilters(), nonFilteredFileExtensions, session);

        try {
            mavenResourcesFiltering.filterResources(mavenResourcesExecution);
        } catch (MavenFilteringException e) {
            throw new WMRuntimeException("Failed to execute resource filtering ", e);
        }
    }

    private void initializeHandlers(Folder rootFolder, String profilePropertyName) throws MojoFailureException {
        if (appBuildHandlers == null) {
            appBuildHandlers = new ArrayList<>();

            Folder pagesFolder = rootFolder.getFolder(pagesDirectory);
            if (pagesFolder.exists()) {
                appBuildHandlers.add(new PageMinFileGenerationHandler(pagesFolder));
            }
            Folder servicesFolder = rootFolder.getFolder(servicesDirectory);
            if (servicesFolder.exists()) {
                URL[] runtimeClasspathElements = getRuntimeClasspathElements();
                appBuildHandlers.add(new SwaggerDocGenerationHandler(servicesFolder, runtimeClasspathElements));
                appBuildHandlers.add(new VariableServiceDefGenerationHandler(rootFolder));
            }

            Folder localeFolder = rootFolder.getFolder(localeDirectory);
            Folder localeOutputFolder = rootFolder.getFolder(localeOutputDirectory);
            appBuildHandlers.add(new LocaleMessagesGenerationHandler(rootFolder, localeOutputFolder));
            appBuildHandlers.add(new WMPropertiesFileGenerationHandler(rootFolder, rootFolder.getFolder(webAppDirectory), localeFolder));

            appBuildHandlers.add(new ProjectDbValidationsGenerationHandler(rootFolder));
            Folder prefabsFolder = rootFolder.getFolder(prefabsDirectory);
            File profilePropertyFile = rootFolder.getFolder("profiles").getFile(profilePropertyName);
            appBuildHandlers
                    .add(new ProjectPrefabBuildFolderGenerationHandler(project, prefabsFolder, profilePropertyFile, mavenResourcesFiltering,
                            session));
        }
    }

    private URL[] getRuntimeClasspathElements() throws MojoFailureException {
        URL[] runtimeUrls = null;
        try {
            List<String> compileClasspathElements = project.getCompileClasspathElements();
            List<String> runtimeClasspathElements = project.getRuntimeClasspathElements();
            Set<String> allClassPathElements = new LinkedHashSet<>(compileClasspathElements.size());
            allClassPathElements.addAll(compileClasspathElements);
            allClassPathElements.addAll(runtimeClasspathElements);
            runtimeUrls = new URL[allClassPathElements.size()];
            int index = 0;
            for (String s : allClassPathElements) {
                runtimeUrls[index++] = new java.io.File(s).toURI().toURL();
            }
        } catch (Exception exception) {
            throw new MojoFailureException("Failed resolve project dependencies", exception);
        }

        return runtimeUrls;
    }

    private List<String> getNonFilteredFileExtensions(List<Plugin> plugins) {
        List<String> nonFilteredFileExtensionsList = new ArrayList<>();
        for (Plugin plugin : plugins) {
            if (MAVEN_RESOURCES_PLUGIN.equals(plugin.getArtifactId())) {
                final Object configuration = plugin.getConfiguration();
                if (configuration != null) {
                    Xpp3Dom xpp3Dom = (Xpp3Dom) configuration;
                    final Xpp3Dom nonFilteredFileExtensions = xpp3Dom.getChild(NON_FILTERED_FILE_EXTENSIONS);
                    final Xpp3Dom[] children = nonFilteredFileExtensions.getChildren();
                    for (Xpp3Dom child : children) {
                        nonFilteredFileExtensionsList.add(child.getValue());
                    }
                }
            }
        }
        return nonFilteredFileExtensionsList;
    }
}


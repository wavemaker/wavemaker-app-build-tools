/**
 * Copyright (C) 2016 WaveMaker, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.wavemaker.studio.app.build.maven.plugin.mojo;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import com.wavemaker.studio.app.build.maven.plugin.handler.AppBuildHandler;
import com.wavemaker.studio.app.build.maven.plugin.handler.PageMinFileGenerationHandler;
import com.wavemaker.studio.app.build.maven.plugin.handler.SwaggerDocGenerationHandler;
import com.wavemaker.studio.app.build.maven.plugin.handler.VariableServiceDefGenerationHandler;
import com.wavemaker.studio.common.io.Folder;
import com.wavemaker.studio.common.io.local.LocalFolder;

/**
 * Created by saddhamp on 12/4/16.
 */
@Mojo(name = "generate", defaultPhase = LifecyclePhase.PREPARE_PACKAGE, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class AppBuildMojo extends AbstractMojo {

    @Parameter(property = "project", required = true, readonly = true)
    private MavenProject project;

    @Parameter(property = "basedir", required = true, readonly = true)
    private String baseDirectory;;

    @Parameter(name="pages-directory", defaultValue = "src/main/webapp/pages/")
    private String pagesDirectory;

    @Parameter(name = "services-directory", defaultValue = "services")
    private String servicesDirectory;

    @Parameter(name = "outputDirectory", defaultValue = "target/classes")
    private String outputDirectory;

    private List<AppBuildHandler> appBuildHandlers;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        initializeHandlers();

        for(AppBuildHandler appBuildHandler : appBuildHandlers){
            appBuildHandler.handle();
        }
    }

    private void initializeHandlers() throws MojoFailureException {
        if(appBuildHandlers == null) {
            appBuildHandlers = new ArrayList<AppBuildHandler>();
            Folder rootFolder = new LocalFolder(baseDirectory);

            Folder pagesFolder = rootFolder.getFolder(pagesDirectory);
            if (pagesFolder.exists()) {
                appBuildHandlers.add(new PageMinFileGenerationHandler(pagesFolder));
            }


            Folder servicesFolder = rootFolder.getFolder(servicesDirectory);
            if (servicesFolder.exists()) {
                Folder targetClassesFolder = rootFolder.getFolder(outputDirectory);
                URL [] runtimeClasspathElements = getRuntimeClasspathElements();
                appBuildHandlers.add(new SwaggerDocGenerationHandler(servicesFolder, runtimeClasspathElements));
                appBuildHandlers.add(new VariableServiceDefGenerationHandler(servicesFolder,targetClassesFolder));
            }
        }
    }

    private URL [] getRuntimeClasspathElements() throws MojoFailureException{
        URL[] runtimeUrls = null;
        try {
            List<String> runtimeClasspathElements = project.getRuntimeClasspathElements();
            runtimeUrls = new URL[runtimeClasspathElements.size()];
            String runtimeClasspathElement;
            for (int i = 0; i < runtimeClasspathElements.size(); i++) {
                runtimeClasspathElement = runtimeClasspathElements.get(i);
                runtimeUrls[i] = new File(runtimeClasspathElement).toURI().toURL();
            }
        } catch (Exception exception){
            throw new MojoFailureException("Failed resolve project dependencies", exception);
        }

        return runtimeUrls;
    }
}


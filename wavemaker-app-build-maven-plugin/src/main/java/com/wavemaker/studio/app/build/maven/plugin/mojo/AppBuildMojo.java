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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import com.wavemaker.studio.app.build.maven.plugin.handler.AppBuildHandler;
import com.wavemaker.studio.app.build.maven.plugin.handler.PageMinFileGenerationHandler;
import com.wavemaker.studio.app.build.maven.plugin.handler.SwaggerDocGenerationHandler;
import com.wavemaker.studio.common.io.local.LocalFolder;

/**
 * Created by saddhamp on 12/4/16.
 */
@Mojo(name = "generate", defaultPhase = LifecyclePhase.PREPARE_PACKAGE)
public class AppBuildMojo extends AbstractMojo {

    @Parameter(property = "basedir", required = true, readonly = true)
    private String baseDirectory;

    @Parameter(property = "project.build.outputDirectory", required = true, readonly = true)
    private String outputDirectory;

    @Parameter(name="pages-directory", defaultValue = "src/main/webapp/pages/")
    private String pagesDirectory;

    @Parameter(name = "services-directory", defaultValue = "services")
    private String servicesDirectory;

    private List<AppBuildHandler> appBuildHandlers;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        initializeHandlers();

        for(AppBuildHandler appBuildHandler : appBuildHandlers){
            appBuildHandler.handle();
        }
    }

    private void initializeHandlers() {
        if(appBuildHandlers == null) {
            appBuildHandlers = new ArrayList<AppBuildHandler>();
            appBuildHandlers.add(new PageMinFileGenerationHandler(new LocalFolder(baseDirectory+"/"+pagesDirectory)));
            appBuildHandlers.add(new SwaggerDocGenerationHandler(new LocalFolder(baseDirectory+"/"+servicesDirectory), outputDirectory));
        }
    }
}


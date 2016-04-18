/**
 * Copyright (C) 2015 WaveMaker, Inc.
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

import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import com.wavemaker.studio.app.build.page.min.PageMinFileGenerator;
import com.wavemaker.studio.common.io.Folder;
import com.wavemaker.studio.common.io.local.LocalFolder;

/**
 * Created by saddhamp on 12/4/16.
 */
@Mojo(name = "generate")
public class AppBuildMojo extends AbstractMojo {

    @Parameter(defaultValue = "${basedir}", required = true)
    private String basedir;

    @Parameter(defaultValue = "/src/main/webapp/pages/")
    private String pagesFolderPath;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        generatePageMinFilesForProjectPages();
    }

    private void generatePageMinFilesForProjectPages() {
        Folder pagesFolder = new LocalFolder(basedir+pagesFolderPath);
        List<Folder> pageFolders = pagesFolder.list().folders().fetchAll();
        PageMinFileGenerator pageMinFileGenerator = new PageMinFileGenerator(pageFolders);
        pageMinFileGenerator.setForceOverwrite(true).generate();
    }
}

/**
 * Copyright © 2013 - 2017 WaveMaker, Inc.
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
package com.wavemaker.app.build.pages;

import java.util.List;

import com.wavemaker.app.build.constants.AppBuildConstants;
import com.wavemaker.commons.WMRuntimeException;
import com.wavemaker.commons.io.File;
import com.wavemaker.commons.io.Folder;

/**
 * Created by saddhamp on 14/4/16.
 */
public class PageMinFileGenerator {
    private List<Folder> pageFolders;
    private boolean forceOverwrite;

    public PageMinFileGenerator(List<Folder> pageFolders){
        if(pageFolders == null || pageFolders.size() < 1)
            throw new WMRuntimeException("List of page folders is either null or empty");
        this.pageFolders = pageFolders;
    }

    public PageMinFileGenerator setForceOverwrite(boolean forceOverwrite) {
        this.forceOverwrite = forceOverwrite;
        return this;
    }

    public void generate() {
        for (Folder pageFolder : pageFolders) {
            File pageMinFile = pageFolder.getFile(AppBuildConstants.PAGE_MIN_FILE);
            if (forceOverwrite || !pageMinFile.exists()) {
                generate(pageFolder);
            }
        }
    }

    private static void generate(Folder pageFolder) {
            StringBuilder pageMinFileContent = new StringBuilder();
            pageMinFileContent.append(Page.CSS.constructTemplate(pageFolder));
            pageMinFileContent.append(Page.JS.constructTemplate(pageFolder));
            pageMinFileContent.append(Page.JSON.constructTemplate(pageFolder));
            pageMinFileContent.append(Page.HTML.constructTemplate(pageFolder));

            File pageMinFile = pageFolder.getFile(AppBuildConstants.PAGE_MIN_FILE);
            pageMinFile.createIfMissing();

            pageMinFile.getContent().write(pageMinFileContent.toString().trim());
    }
}
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
package com.wavemaker.studio.app.build.pages;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.wavemaker.studio.app.build.constants.AppBuildConstants;
import com.wavemaker.studio.common.WMRuntimeException;
import com.wavemaker.studio.common.io.File;
import com.wavemaker.studio.common.io.Folder;
import com.wavemaker.studio.common.util.RegexConstants;

/**
 * Created by saddhamp on 15/4/16.
 */
public class PageMinFileUpdator {
    private Folder pageFolder;
    private Page page;

    public PageMinFileUpdator(Folder pageFolder, Page page) {
        if(pageFolder == null)
            throw new WMRuntimeException("Page folder is null");
        if(page == null)
            throw new WMRuntimeException("Page type is null");
        this.pageFolder = pageFolder;
        this.page = page;
    }

    public void update() {
        File pageMinFile = pageFolder.getFile(AppBuildConstants.PAGE_MIN_FILE);
        String pageMinFileContent = pageMinFile.getContent().asString();

        String pageName = pageFolder.getName();
        String startElement = page.getStartElement(pageName);

        validatePageMinFile(pageMinFileContent, startElement);

        String endElement = page.getEndElement();
        String pageContent = page.constructTemplate(pageFolder);
        String newPageMinFileContent = replacePageContent(pageMinFileContent, startElement, endElement, pageContent.trim());
        pageMinFile.getContent().write(newPageMinFileContent.trim());
    }

    private static String replacePageContent(String fileContent, String startElement, String endElement, String pageContent) {
        String pageRegex = RegexConstants.MULTILINE_FLAG+ Pattern.quote(startElement)+RegexConstants.FIRST_OCCURENCE_OF_ANY_CHARSEQUENCE+Pattern.quote(endElement);
        Pattern pageContentPattern = Pattern.compile(pageRegex);
        String newPageMinFileContent = pageContentPattern.matcher(fileContent).replaceFirst(Matcher.quoteReplacement(pageContent));
        return newPageMinFileContent;
    }

    private static void validatePageMinFile(String string, String subString) {
        if (StringUtils.countMatches(string, subString) != 1) {
            throw new WMRuntimeException("Page resource update not supported");
        }
    }

}

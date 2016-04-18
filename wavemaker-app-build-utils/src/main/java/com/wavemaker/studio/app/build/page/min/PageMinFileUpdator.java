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
package com.wavemaker.studio.app.build.page.min;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.wavemaker.studio.app.build.constants.AppBuildConstants;
import com.wavemaker.studio.common.WMRuntimeException;
import com.wavemaker.studio.common.io.File;
import com.wavemaker.studio.common.io.Folder;
import com.wavemaker.studio.common.io.local.LocalFile;
import com.wavemaker.studio.common.util.RegexConstants;
import com.wavemaker.studio.common.util.WMFileUtils;
import com.wavemaker.studio.common.util.WMUtils;

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
        String fileContent = pageMinFile.getContent().asString();
        String extension = page.getFileSuffix();

        Page page = Page.getPage(extension);
        String pageName = pageFolder.getName();
        String startElement = page.getStartElement(pageName);

        validatePageMinFile(fileContent, startElement);

        String endElement = page.getEndElement();
        String content = page.constructTemplate(pageFolder);
        content = replacePageContent(fileContent, startElement, endElement, content);
        pageMinFile.getContent().write(content.trim());
    }

    private static String replacePageContent(String fileContent, String startElement, String endElement, String newPageContent) {
        String pageRegex = RegexConstants.MULTILINE_FLAG+ Pattern.quote(startElement)+RegexConstants.FIRST_OCCURENCE_OF_ANY_CHARSEQUENCE+Pattern.quote(endElement);
        Pattern pageContentPattern = Pattern.compile(pageRegex);
        newPageContent = pageContentPattern.matcher(fileContent).replaceFirst(Matcher.quoteReplacement(newPageContent));
        return newPageContent;
    }

    private static void validatePageMinFile(String string, String subString) {
        if (StringUtils.countMatches(string, subString) != 1) {
            throw new WMRuntimeException("Page resource update not supported");
        }
    }

}

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

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.wavemaker.commons.WMRuntimeException;
import com.wavemaker.commons.io.File;
import com.wavemaker.commons.io.Folder;
import com.wavemaker.commons.io.local.LocalFile;
import com.wavemaker.commons.util.SystemUtils;
import com.wavemaker.commons.util.WMFileUtils;

/**
 * Created by saddhamp on 14/4/16.
 */
public enum Page {
    CSS(
            "css",
            "{Content}",
            "<style id=\"{PageName}.css\">",
            "</style>",
            ""),
    JS(
            "js",
            "{Content}",
            "<script id=\"{PageName}.js\">",
            "</script>",
            ""),
    JSON(
            "variables.json",
            "var _{PageName}Page_Variables_ ={Content}",
            "<script id=\"{PageName}.variables.json\">",
            "</script>",
            "{}"),
    HTML(
            "html",
            "{Content}",
            "<script id=\"{PageName}.html\" type=\"text/ng-template\">",
            "</script>",
            "");

    private static final Pattern PAGE_NAME_PATTERN = Pattern.compile("\\{PageName\\}");
    private static final Pattern CONTENT_PATTERN = Pattern.compile("\\{Content\\}");

    private String fileSuffix;
    private String contentTemplate;
    private String startElement;
    private String endElement;
    private String defaultContent;

    Page(String fileSuffix, String contentTemplate, String startElement, String endElement, String defaultContent) {
        this.fileSuffix = fileSuffix;
        this.contentTemplate = contentTemplate;
        this.startElement = startElement;
        this.endElement = endElement;
        this.defaultContent = defaultContent;
    }

    public String getStartElement(String pageName) {
        String manipulateStartElement = PAGE_NAME_PATTERN.matcher(startElement).replaceAll(pageName);
        return manipulateStartElement;
    }

    public String getEndElement() {
        return endElement;
    }

    private String getPageFileName(String pageName){
        return pageName + "." + fileSuffix;
    }

    private String getTemplateContent(String pageName, String originalContent){
        String lineBreak = SystemUtils.getLineBreak();

        String startElement = this.getStartElement(pageName);
        startElement = startElement+lineBreak;

        String manipulatedContent = PAGE_NAME_PATTERN.matcher(contentTemplate).replaceAll(pageName);
        manipulatedContent = CONTENT_PATTERN.matcher(manipulatedContent).replaceFirst(Matcher.quoteReplacement(originalContent));
        manipulatedContent = StringUtils.isNotBlank(manipulatedContent)?manipulatedContent+lineBreak:manipulatedContent;

        String endElement = this.getEndElement();
        endElement = endElement+lineBreak;

        String templateContent = startElement+manipulatedContent+endElement;
        return templateContent;
    }

    public String constructTemplate(Folder pageFolder){
        String templateContent = null;

        try {
            String pageName = pageFolder.getName();
            File templateFile = pageFolder.getFile(getPageFileName(pageName));

            templateContent = templateFile.exists() ? WMFileUtils.readFileToString(((LocalFile) templateFile).getLocalFile()) : "";
            templateContent = StringUtils.isBlank(templateContent) ? defaultContent : templateContent.trim();
            templateContent = getTemplateContent(pageName, templateContent);

        } catch (IOException ioException){
            throw new WMRuntimeException("Failed to construct template for project page", ioException);
        }

        return templateContent;
    }

    public static Page getPage(String pageName){
        return (StringUtils.isNotBlank(pageName) ? Page.valueOf(pageName.toUpperCase()) : null);
    }
}


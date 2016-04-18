package com.wavemaker.studio.app.build.page.min;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.wavemaker.studio.common.WMRuntimeException;
import com.wavemaker.studio.common.io.File;
import com.wavemaker.studio.common.io.Folder;
import com.wavemaker.studio.common.io.local.LocalFile;
import com.wavemaker.studio.common.util.SystemUtils;
import com.wavemaker.studio.common.util.WMFileUtils;

/**
 * Created by saddhamp on 14/4/16.
 */
public enum Page {
    CSS(
            "css",
            "{Content}{LineBreak}",
            "<style id=\"{Page}.css\">{LineBreak}",
            "</style>{LineBreak}",
            ""),
    JS(
            "js",
            "{Content}{LineBreak}",
            "<script id=\"{Page}.js\">{LineBreak}",
            "</script>{LineBreak}",
            ""),
    JSON(
            "variables.json",
            "var _{Page}Page_Variables_ ={Content}{LineBreak}",
            "<script id=\"{Page}.variables.json\">{LineBreak}",
            "</script>{LineBreak}",
            "{}"),
    HTML(
            "html",
            "{Content}{LineBreak}",
            "<script id=\"{Page}.html\" type=\"text/ng-template\">{LineBreak}",
            "</script>{LineBreak}",
            "");

    private static final Pattern PAGE_NAME_PATTERN = Pattern.compile("\\{Page\\}");
    private static final Pattern CONTENT_PATTERN = Pattern.compile("\\{Content\\}");
    private static final Pattern  LINE_BREAK_PATTERN = Pattern.compile("\\{LineBreak\\}");

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

    public String getFileSuffix() {
        return fileSuffix;
    }

    public String getStartElement(String pageName) {
        String manipulateStartElement = PAGE_NAME_PATTERN.matcher(startElement).replaceAll(pageName);
        manipulateStartElement = LINE_BREAK_PATTERN.matcher(manipulateStartElement).replaceAll(SystemUtils.getLineBreak());
        return manipulateStartElement;
    }

    public String getEndElement() {
        return LINE_BREAK_PATTERN.matcher(endElement).replaceAll(SystemUtils.getLineBreak());
    }

    private String getDefaultContent(){
        return defaultContent;
    }

    private String getPageFileName(String pageName){
        return pageName + "." + fileSuffix;
    }

    private String getTemplateContent(String pageName, String originalContent){
        String contentLineBreak = StringUtils.isNotBlank(originalContent)?SystemUtils.getLineBreak():"";

        String manipulatedContent = LINE_BREAK_PATTERN.matcher(contentTemplate).replaceAll(contentLineBreak);
        manipulatedContent = PAGE_NAME_PATTERN.matcher(manipulatedContent).replaceAll(pageName);
        manipulatedContent = CONTENT_PATTERN.matcher(manipulatedContent).replaceFirst(Matcher.quoteReplacement(originalContent));
        manipulatedContent = this.getStartElement(pageName)+manipulatedContent+this.getEndElement();

        return manipulatedContent;
    }

    public String constructTemplate(Folder pageFolder){
        try {
            String pageName = pageFolder.getName();
            File templateFile = pageFolder.getFile(getPageFileName(pageName));
            String templateContent = templateFile.exists() ? WMFileUtils.readFileToString(((LocalFile) templateFile).getLocalFile()) : "";
            templateContent = StringUtils.isBlank(templateContent) ? getDefaultContent() : templateContent.trim();
            return getTemplateContent(pageName, templateContent);
        } catch (IOException ioException){
            throw new WMRuntimeException("Failed to construct template for project page", ioException);
        }
    }

    public static Page getPage(String pageName){
        return (StringUtils.isNotBlank(pageName)? Page.valueOf(pageName.toUpperCase()):null);
    }
}


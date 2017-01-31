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
package com.wavemaker.studio.app.build.maven.plugin.handler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.wavemaker.studio.app.build.BasePackage;
import com.wavemaker.studio.app.build.swaggerdoc.SwaggerGenerator;
import com.wavemaker.commons.WMRuntimeException;
import com.wavemaker.commons.io.File;
import com.wavemaker.commons.io.Folder;
import com.wavemaker.commons.util.IOUtils;
import com.wavemaker.tools.apidocs.tools.core.model.Info;
import com.wavemaker.tools.apidocs.tools.core.model.Swagger;

/**
 * Created by saddhamp on 20/4/16.
 */
public class SwaggerDocGenerationHandler implements AppBuildHandler {
    public static final String SERVICE_DEF_XML = "servicedef.xml";
    public static final String SECURITY_SERVICE_TYPE = "SecurityServiceType";
    public static final String FEED_SERVICE_TYPE = "FeedService";
    public static final String SECURITY_SERVICE_CONTROLLER_CLAZZ = "com.wavemaker.runtime.security.controller.SecurityController";
    public static final String FEED_SERVICE_CONTROLLER_CLAZZ = "com.wavemaker.runtime.feed.controller.FeedServiceController";
    private static final Logger logger = LoggerFactory.getLogger(SwaggerDocGenerationHandler.class);
    private static final String DESIGN_TIME_FOLDER = "designtime";
    private static final String SRC_FOLDER = "src";
    private static final String API_EXTENSION = "_API.json";
    private ObjectMapper objectMapper;
    private Folder servicesFolder;
    private URLClassLoader urlClassLoader;
    private URL[] classPathURLs;

    public SwaggerDocGenerationHandler(Folder servicesFolder, URL[] classPathURLs) {
        if (servicesFolder == null || !servicesFolder.exists()) {
            throw new WMRuntimeException("Services folder is null or does not exist");
        }
        if (classPathURLs == null || classPathURLs.length == 0) {
            throw new WMRuntimeException("No class path url provided");
        }

        objectMapper = new ObjectMapper();
        objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        this.servicesFolder = servicesFolder;
        this.classPathURLs = classPathURLs;
    }

    @Override
    public void handle() {
        try {
            urlClassLoader = new URLClassLoader(classPathURLs, Thread.currentThread().getContextClassLoader());

            List<Folder> serviceFolders = servicesFolder.list().folders().fetchAll();
            if (serviceFolders.size() > 0) {
                for (Folder serviceFolder : serviceFolders) {
                    generateSwaggerDoc(serviceFolder);
                }
            }
        } finally {
            if (urlClassLoader != null) {
                try {
                    urlClassLoader.close();
                } catch (IOException e) {
                    logger.warn("Failed to close classloader");
                }
            }
        }
    }

    protected void generateSwaggerDoc(Folder serviceFolder) {

        if (serviceFolder.exists()) {
            String basePackage = getBasePackageName(serviceFolder);

            if (StringUtils.isNotBlank(basePackage)) {
                SwaggerGenerator swaggerGenerator = new SwaggerGenerator(basePackage);
                final Info swaggerInfo = buildSwaggerInfo(serviceFolder.getName());
                Swagger swagger = swaggerGenerator.setClassLoader(urlClassLoader).setSwaggerInfo(swaggerInfo).generate();

                marshallAndWriteToFile(swagger, serviceFolder.getFolder(DESIGN_TIME_FOLDER));
            }
        }
    }

    private String getBasePackageName(final Folder serviceFolder) {
        final File serviceDefXML = serviceFolder.getFolder(DESIGN_TIME_FOLDER).getFile(SERVICE_DEF_XML);
        String serviceType = findServiceType(serviceDefXML);
        if (serviceType != null && serviceType.equals(SECURITY_SERVICE_TYPE)) {
            return SECURITY_SERVICE_CONTROLLER_CLAZZ;
        } else if (serviceType != null && serviceType.equals(FEED_SERVICE_TYPE)) {
            return FEED_SERVICE_CONTROLLER_CLAZZ;
        }
        final BasePackage basePackage = new BasePackage(serviceFolder.getFolder(SRC_FOLDER));
        return basePackage.getBasePackageName();
    }

    private String findServiceType(final File serviceDefXML) {
        String serviceType;
        DocumentBuilderFactory dbFactory
                = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = null;
        InputStream is = null;
        try {
            dBuilder = dbFactory.newDocumentBuilder();
            is = serviceDefXML.getContent().asInputStream();
            Document doc = dBuilder.parse(is);
            doc.getDocumentElement().normalize();
            serviceType = doc.getDocumentElement().getAttribute("type");
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new WMRuntimeException("failed to find serviceType from servicedef xml file", e);
        } finally {
            org.apache.commons.io.IOUtils.closeQuietly(is);
        }
        return serviceType;
    }

    protected void marshallAndWriteToFile(Swagger swagger, Folder designTimeFolder) {
        OutputStream outputStream = null;
        try {
            File swaggerFile = designTimeFolder.getFile(designTimeFolder.getParent().getName() + API_EXTENSION);
            outputStream = swaggerFile.getContent().asOutputStream();
            objectMapper.writeValue(outputStream, swagger);
        } catch (Exception e) {
            throw new WMRuntimeException("Failed to parse file ", e);
        } finally {
            IOUtils.closeSilently(outputStream);
        }
    }

    protected Info buildSwaggerInfo(String serviceId) {
        Info info = new Info();
        info.setDescription("Swagger API documentation");
        info.setServiceId(serviceId);
        info.setServiceType(null);
        info.setVersion("2.0");
        info.setTitle("Service Swagger Documentation");

        return info;
    }
}

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
package com.wavemaker.studio.app.build.maven.plugin.handler;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.wavemaker.studio.app.build.BasePackage;
import com.wavemaker.studio.app.build.swaggerdoc.SwaggerGenerator;
import com.wavemaker.studio.common.WMRuntimeException;
import com.wavemaker.studio.common.io.File;
import com.wavemaker.studio.common.io.Folder;
import com.wavemaker.studio.common.util.IOUtils;
import com.wavemaker.tools.apidocs.tools.core.model.Info;
import com.wavemaker.tools.apidocs.tools.core.model.Swagger;

/**
 * Created by saddhamp on 20/4/16.
 */
public class SwaggerDocGenerationHandler extends AbstractLogEnabled implements AppBuildHandler {
    private static final String DESIGN_TIME_FOLDER = "designtime";
    private static final String SRC_FOLDER = "src";
    private static final String API_EXTENSION = "_API.json";

    private ObjectMapper objectMapper;
    private Folder servicesFolder;
    private URLClassLoader urlClassLoader;
    private URL [] classPathURLs;

    public SwaggerDocGenerationHandler(Folder servicesFolder, URL [] classPathURLs) {
        if(servicesFolder == null || !servicesFolder.exists()) {
            throw new WMRuntimeException("Services folder is null or does not exist");
        }
        if(classPathURLs == null || classPathURLs.length == 0) {
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
            if(serviceFolders.size() > 0){
                for(Folder serviceFolder : serviceFolders){
                    generateSwaggerDoc(serviceFolder);
                }
            }
        } finally {
            if (urlClassLoader != null) {
                try {
                    urlClassLoader.close();
                } catch (IOException e) {
                    getLogger().warn("Failed to close classloader");
                }
            }
        }
    }

    @Override
    protected Logger getLogger() {
        Logger logger = super.getLogger();

        if (logger == null) {
            logger = new ConsoleLogger(Logger.LEVEL_INFO, "swagger-doc-generation-handler");

            enableLogging(logger);
        }

        return logger;
    }

    protected void generateSwaggerDoc(Folder serviceFolder) {

        if(serviceFolder.exists()) {
            String basePackage = new BasePackage(serviceFolder.getFolder(SRC_FOLDER)).getBasePackageName();

            if (StringUtils.isNotBlank(basePackage)) {
                SwaggerGenerator swaggerGenerator = new SwaggerGenerator(basePackage);
                Swagger swagger = swaggerGenerator.setClassLoader(urlClassLoader).setSwaggerInfo(buildSwaggerInfo()).generate();

                marshallAndWriteToFile(swagger, serviceFolder.getFolder(DESIGN_TIME_FOLDER));
            }
        }
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

    protected Info buildSwaggerInfo() {
        Info info = new Info();
        info.setDescription("Swagger API documentation");
        info.setServiceId(null);
        info.setServiceType(null);
        info.setVersion("2.0");
        info.setTitle("Service Swagger Documentation");

        return info;
    }
}

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
package com.wavemaker.app.build.maven.plugin.handler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.wavemaker.app.build.exception.ServiceDefGenerationException;
import com.wavemaker.app.build.servicedef.ServiceDefGenerator;
import com.wavemaker.commons.WMRuntimeException;
import com.wavemaker.commons.io.File;
import com.wavemaker.commons.io.FilterOn;
import com.wavemaker.commons.io.Folder;
import com.wavemaker.commons.io.Resources;
import com.wavemaker.commons.json.JSONUtils;
import com.wavemaker.commons.servicedef.model.ServiceDefinition;
import com.wavemaker.commons.util.WMIOUtils;
import com.wavemaker.tools.apidocs.tools.core.model.Swagger;

/**
 * @author <a href="mailto:sunil.pulugula@wavemaker.com">Sunil Kumar</a>
 * @since 29/4/16
 */
public class VariableServiceDefGenerationHandler implements AppBuildHandler {
    private static final Logger logger = LoggerFactory.getLogger(VariableServiceDefGenerationHandler.class);

    private static final String SERVICE_DEFS = "servicedefs";
    private static final String WM_SERVICE_VARIABLE = "wm.ServiceVariable";
    private static final String WEBSOCKET_VARIABLE = "wm.WebSocketVariable";
    private static final String DESIGN_TIME_FOLDER = "designtime";
    private static final String API_EXTENSION = "_API.json";
    private static final String REST_SERVICE_API_EXTENSION = "_API_REST_SERVICE.json";
    private static final String WEBSOCKET_SERVICE_API_EXTENSION = "_API_WEBSOCKET_SERVICE.json";
    public static final String SERVICE_SRC_DIR = "src";
    private static String SERVICE_DEF_RESOURCE_NAME = "{}-service-definitions.json";
    private static final String[] SWAGGER_EXTENSIONS = new String[] {WEBSOCKET_SERVICE_API_EXTENSION, REST_SERVICE_API_EXTENSION, API_EXTENSION};


    private final String servicesDirectory = "services";
    private final Folder servicesFolder;
    private final Folder rootFolder;

    private Map<String, Map<String, ServiceDefinition>> serviceVsServiceDefs = new HashMap<>();
    private Map<String, Map<String, ServiceDefinition>> filteredServiceVsServiceDefinitions = new HashMap<>();

    public VariableServiceDefGenerationHandler(Folder rootFolder) {
        this.rootFolder = rootFolder;
        this.servicesFolder = rootFolder.getFolder(servicesDirectory);
    }

    @Override
    public void handle() {
        init();
        generateServiceDefs();
        persistServiceDefs();
    }

    private void init() {
        buildServiceDefsForAllServices(servicesFolder);
    }


    private void buildServiceDefsForAllServices(final Folder servicesFolder) {
        if (servicesFolder.exists()) {
            List<Folder> serviceFolders = servicesFolder.list().folders().fetchAll();
            if (serviceFolders.size() > 0) {
                for (final Folder serviceFolder : serviceFolders) {
                    serviceVsServiceDefs.put(serviceFolder.getName(), buildServiceDefs(serviceFolder));
                }
            }
        }
    }

    private Map<String, ServiceDefinition> buildServiceDefs(final Folder serviceFolder) {
        Folder designFolder = serviceFolder.getFolder(DESIGN_TIME_FOLDER);
        Swagger swagger = null;
        boolean swaggerFileFound = false;
        for (String swaggerExtension : SWAGGER_EXTENSIONS) {
            File swaggerFile = designFolder.getFile(designFolder.getParent().getName() + swaggerExtension);
            if (swaggerFile.exists()) {
                swaggerFileFound = true;
                swagger = unmarshallSwagger(swaggerFile);
                break;
            }
        }
        if (!swaggerFileFound) {
            logger.error("Swagger File does not exist for service {}", serviceFolder.getName());
        }
        try {
            return swagger != null ? new ServiceDefGenerator(swagger).generate() : new HashMap<>();
        } catch (ServiceDefGenerationException e) {
            throw new WMRuntimeException("Failed to build service def for service " + swagger.getInfo().getServiceId(), e);
        }
    }


    private void generateServiceDefs() {
        Resources<File> files = rootFolder.find().files().exclude(FilterOn.antPattern("/app/prefabs/**")).include(FilterOn.names().ending(".variables.json"));
        for (final File file : files) {
            try {
                generateServiceDefs(file);
            } catch (JSONException e) {
                logger.warn("Failed to build service definitions for variable json file {}", file.getName(), e);
            }
        }
    }

    private void generateServiceDefs(File file) throws JSONException {
        String s = file.getContent().asString();
        if (StringUtils.isBlank(s)) {
            return;
        }
        JSONObject jsonObject = new JSONObject(s);
        Iterator keys = jsonObject.keys();
        while (keys.hasNext()) {
            String variableName = (String) keys.next();
            JSONObject o = (JSONObject) jsonObject.get(variableName);
            if (o.has("category")) {
                String category = o.getString("category");
                if (!(WM_SERVICE_VARIABLE.equals(category) || WEBSOCKET_VARIABLE.equals(category))) {
                    continue;
                }
                String serviceId = o.optString("service");
                String operationId = o.optString("operationId");
                if (StringUtils.isBlank(operationId)) {
                    logger.warn("Service variable with name {} does not have operationId property", variableName);
                    continue;
                }
                if (StringUtils.isBlank(serviceId)) {
                    logger.warn("Service variable with name {} does not have service property", variableName);
                    continue;
                }
                Map<String, ServiceDefinition> serviceDefinitions = serviceVsServiceDefs.get(serviceId);
                ServiceDefinition serviceDefinition = (serviceDefinitions == null) ? null: serviceDefinitions.get(operationId);
                if (serviceDefinition == null) {
                    logger.warn("service {} doesn't exist with for the service variable with name {} and operationId {}",serviceId,variableName,operationId);
                    continue;
                }
                Map<String, ServiceDefinition> filteredServiceDefinitions = filteredServiceVsServiceDefinitions.get(serviceId);
                if (filteredServiceDefinitions == null) {
                    filteredServiceDefinitions = new HashMap<>();
                    filteredServiceVsServiceDefinitions.put(serviceId, filteredServiceDefinitions);
                }
                filteredServiceDefinitions.put(operationId, serviceDefinition);
            }
        }
    }


    protected void persistServiceDefs() {
        for (final String service : filteredServiceVsServiceDefinitions.keySet()) {
            persistServiceDefs(service, filteredServiceVsServiceDefinitions.get(service));
        }
    }

    protected void persistServiceDefs(final String serviceId, final Map<String, ServiceDefinition> serviceDefMap) {
        File serviceDefResource = getServiceDefResource(serviceId);
        OutputStream outputStream = null;
        try {
            outputStream = serviceDefResource.getContent().asOutputStream();
            JSONUtils.toJSON(outputStream, serviceDefMap, true);
        } catch (IOException e) {
            throw new WMRuntimeException("Failed to persist service definition in resource " + serviceDefResource.getName(), e);
        } finally {
            WMIOUtils.closeSilently(outputStream);
        }
    }

    protected File getServiceDefResource(final String serviceId) {
        final Folder serviceFolder = servicesFolder.getFolder(serviceId).getFolder(SERVICE_SRC_DIR);
        File file = serviceFolder.getFolder(SERVICE_DEFS).getFile(SERVICE_DEF_RESOURCE_NAME.replace("{}", serviceId));
        if (!file.exists()) {
            file.createIfMissing();
        }
        return file;
    }

    protected Swagger unmarshallSwagger(File file) {
        InputStream is = null;
        try {
            is = file.getContent().asInputStream();
            return JSONUtils.toObject(is, Swagger.class);
        } catch (Exception e) {
            throw new WMRuntimeException("Failed to parse swagger file ", e);
        } finally {
            WMIOUtils.closeSilently(is);
        }
    }
}

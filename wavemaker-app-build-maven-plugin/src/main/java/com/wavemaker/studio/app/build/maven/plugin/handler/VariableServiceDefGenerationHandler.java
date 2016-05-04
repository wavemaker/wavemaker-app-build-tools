package com.wavemaker.studio.app.build.maven.plugin.handler;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;

import com.wavemaker.studio.app.build.servicedef.ServiceDefGenerator;
import com.wavemaker.studio.common.WMRuntimeException;
import com.wavemaker.studio.common.io.File;
import com.wavemaker.studio.common.io.Folder;
import com.wavemaker.studio.common.json.JSONUtils;
import com.wavemaker.studio.common.servicedef.model.ServiceDefinition;
import com.wavemaker.studio.common.util.IOUtils;
import com.wavemaker.tools.apidocs.tools.core.model.Swagger;

/**
 * @author <a href="mailto:sunil.pulugula@wavemaker.com">Sunil Kumar</a>
 * @since 29/4/16
 */
public class VariableServiceDefGenerationHandler extends AbstractLogEnabled implements AppBuildHandler {

    public static final String SERVICE_DEFS = "servicedefs";
    private static final String DESIGN_TIME_FOLDER = "designtime";
    private static final String RUNTIME_TIME_FOLDER = "src";
    private static final String API_EXTENSION = "_API.json";
    private static final String REST_SERVICE_API_EXTENSION = "_API_REST_SERVICE.json";
    private static String SERVICE_DEF_RESOURCE_NAME = "{}-service-definitions.json";
    private final Folder servicesFolder;
    private final Folder targetClassesFolder;

    public VariableServiceDefGenerationHandler(Folder servicesFolder, Folder targetClassesFolder) {
        this.servicesFolder = servicesFolder;
        this.targetClassesFolder = targetClassesFolder;
    }

    @Override
    public void handle() {
        if (servicesFolder.exists()) {
            List<Folder> serviceFolders = servicesFolder.list().folders().fetchAll();
            if (serviceFolders.size() > 0) {
                for (Folder serviceFolder : serviceFolders) {
                    try {
                        generateServiceDef(serviceFolder);
                    } catch (FileNotFoundException e) {
                        throw new WMRuntimeException(e);
                    }
                }
            }
        }
    }

    private void generateServiceDef(final Folder serviceFolder) throws FileNotFoundException {

        Folder designFolder = serviceFolder.getFolder(DESIGN_TIME_FOLDER);
        Folder runtimeFolder = serviceFolder.getFolder(RUNTIME_TIME_FOLDER);
        File swaggerFile = designFolder.getFile(designFolder.getParent().getName() + API_EXTENSION);
        File restSwaggerFile = designFolder.getFile(designFolder.getParent().getName() + REST_SERVICE_API_EXTENSION);
        Swagger swagger = null;
        if (swaggerFile.exists()) {
            swagger = unmarshallSwagger(swaggerFile);
        } else if (restSwaggerFile.exists()) {
            swagger = unmarshallSwagger(restSwaggerFile);
        } else {
            getLogger().error("Swagger api documentation swaggerFile does not exist for service " + serviceFolder.getName());
            throw new FileNotFoundException("Swagger api documentation swaggerFile does not exist for service " + serviceFolder.getName());
        }

        getLogger().info("Generating service def for service id " + serviceFolder.getName());
        final List<ServiceDefinition> serviceDefs = new ServiceDefGenerator(swagger).generate();
        Map<String, ServiceDefinition> serviceDefMap = buildServiceDefsMap(serviceDefs);
        putServiceDefinition(runtimeFolder, serviceDefMap);
        putServiceDefinition(targetClassesFolder, serviceDefMap);

    }

    protected Swagger unmarshallSwagger(File file) {
        InputStream is = null;
        try {
            is = file.getContent().asInputStream();
            Swagger swagger = JSONUtils.toObject(is, Swagger.class);
            return swagger;
        } catch (Exception e) {
            throw new WMRuntimeException("Failed to parse swagger file ", e);
        } finally {
            IOUtils.closeSilently(is);
        }
    }

    protected void putServiceDefinition(final Folder runtimeFolder, final Map<String, ServiceDefinition> serviceDefMap) {
        File serviceDefResource = getServiceDefResource(runtimeFolder);
        OutputStream outputStream = null;
        try {
            outputStream = serviceDefResource.getContent().asOutputStream();
            JSONUtils.toJSON(outputStream, serviceDefMap, true);
        } catch (IOException e) {
            throw new WMRuntimeException("Failed to persist service definition in resource " + serviceDefResource.getName(), e);
        } finally {
            org.apache.commons.io.IOUtils.closeQuietly(outputStream);
        }
    }

    protected File getServiceDefResource(Folder runtimeFolder) {
        File file = runtimeFolder.getFolder(SERVICE_DEFS).getFile(SERVICE_DEF_RESOURCE_NAME.replace("{}", runtimeFolder.getParent().getName()));
        if (!file.exists()) {
            file.createIfMissing();
        }
        return file;
    }

    private Map buildServiceDefsMap(final List<ServiceDefinition> serviceDefs) {
        final Map<String, ServiceDefinition> serviceDefMap = new HashMap<>();
        for (ServiceDefinition serviceDef : serviceDefs) {
            serviceDefMap.put(serviceDef.getId(), serviceDef);
        }
        return serviceDefMap;
    }

    @Override
    protected Logger getLogger() {
        Logger logger = super.getLogger();
        if (logger == null) {
            logger = new ConsoleLogger(Logger.LEVEL_INFO, "service-def-generation-handler");
            enableLogging(logger);
        }
        return logger;
    }
}

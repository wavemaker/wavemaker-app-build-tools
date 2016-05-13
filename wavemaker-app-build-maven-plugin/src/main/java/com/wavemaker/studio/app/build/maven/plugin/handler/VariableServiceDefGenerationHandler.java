package com.wavemaker.studio.app.build.maven.plugin.handler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
import java.util.concurrent.*;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.json.JSONException;
import org.json.JSONObject;

import com.wavemaker.studio.app.build.servicedef.ServiceDefGenerator;
import com.wavemaker.studio.common.WMRuntimeException;
import com.wavemaker.studio.common.io.File;
import com.wavemaker.studio.common.io.FilterOn;
import com.wavemaker.studio.common.io.Folder;
import com.wavemaker.studio.common.io.Resources;
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
    public static final String WM_SERVICE_VARIABLE = "wm.ServiceVariable";
    private static final String DESIGN_TIME_FOLDER = "designtime";
    private static final String API_EXTENSION = "_API.json";
    private static final String REST_SERVICE_API_EXTENSION = "_API_REST_SERVICE.json";
    private static String SERVICE_DEF_RESOURCE_NAME = "{}-service-definitions.json";


    private final String servicesDirectory = "services";
    private final String outputDirectory = "target/classes";
    private final Folder servicesFolder;
    private final Folder targetClassesFolder;
    private final Folder rootFolder;

    private ExecutorService executorService = Executors.newFixedThreadPool(5);
    private Map<String, Future<Map<String, ServiceDefinition>>> serviceVsServiceDefs = new HashMap<>();
    private Map<String, Map<String, ServiceDefinition>> filteredServiceVsServiceDefs = new ConcurrentHashMap<>();

    public VariableServiceDefGenerationHandler(Folder rootFolder) {
        this.rootFolder = rootFolder;
        this.servicesFolder = rootFolder.getFolder(servicesDirectory);
        this.targetClassesFolder = rootFolder.getFolder(outputDirectory);
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


    private Map<String, Future<Map<String, ServiceDefinition>>> buildServiceDefsForAllServices(final Folder servicesFolder) {
        if (servicesFolder.exists()) {
            List<Folder> serviceFolders = servicesFolder.list().folders().fetchAll();
            if (serviceFolders.size() > 0) {
                for (final Folder serviceFolder : serviceFolders) {
                    serviceVsServiceDefs.put(serviceFolder.getName(), executorService.submit(new Callable<Map<String, ServiceDefinition>>() {
                        @Override
                        public Map<String, ServiceDefinition> call() throws Exception {
                            return buildServiceDefs(serviceFolder);
                        }
                    }));
                }
            }
        }
        return serviceVsServiceDefs;
    }

    private Map<String, ServiceDefinition> buildServiceDefs(final Folder serviceFolder) {
        Folder designFolder = serviceFolder.getFolder(DESIGN_TIME_FOLDER);
        File swaggerFile = designFolder.getFile(designFolder.getParent().getName() + API_EXTENSION);
        File restSwaggerFile = designFolder.getFile(designFolder.getParent().getName() + REST_SERVICE_API_EXTENSION);
        Swagger swagger = null;
        if (swaggerFile.exists()) {
            swagger = unmarshallSwagger(swaggerFile);
        } else if (restSwaggerFile.exists()) {
            swagger = unmarshallSwagger(restSwaggerFile);
        } else {
            getLogger().error("Swagger api documentation swaggerFile does not exist for service " + serviceFolder.getName());
        }
        return swagger != null ? new ServiceDefGenerator(swagger).generate() : new HashMap<String, ServiceDefinition>();

    }

    private void generateServiceDefs() {
        Resources<File> files = rootFolder.find().files().exclude(FilterOn.antPattern("/app/prefabs/**")).include(FilterOn.names().ending(".variables.json"));
        Collection<Callable> callables = new ArrayList<>();
        Collection<Future> futures = new ArrayList<>();
        for (final File file : files) {
            callables.add(new Callable<Object>() {
                @Override
                public Object call() throws Exception {
                    try {
                        generateServiceDefs(file);
                    } catch (JSONException e) {
                        getLogger().error("Failed to build service definitions for variable json file " + file.getName());
                    }
                    return this;
                }
            });
        }
        try {
            for (Callable callable : callables) {
                futures.add(executorService.submit(callable));
            }
            for (Future<Object> future : futures) {
                future.get();
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    private void generateServiceDefs(final File file) throws JSONException {
        String s = file.getContent().asString();
        if (StringUtils.isBlank(s)) {
            return;
        }
        JSONObject jsonObject = new JSONObject(s);
        Iterator keys = jsonObject.keys();
        while (keys.hasNext()) {
            String key = (String) keys.next();
            JSONObject o = (JSONObject) jsonObject.get(key);
            if (o.has("category") && o.getString("category").equals(WM_SERVICE_VARIABLE)) {
                validateServiceVariable(key, o);
                String operationId = o.getString("operationId");
                String service = o.getString("service");
                if (!serviceVsServiceDefs.containsKey(service)) {
                    getLogger().error("Service does not exist for the service variable" + key);
                }
                try {
                    synchronized (filteredServiceVsServiceDefs) {
                        if (!filteredServiceVsServiceDefs.containsKey(service)) {
                            filteredServiceVsServiceDefs.put(service, new ConcurrentHashMap<String, ServiceDefinition>());
                        }
                    }
                    ServiceDefinition serviceDefinition = serviceVsServiceDefs.get(service).get().get(operationId);
                    Map<String, ServiceDefinition> serviceDefinitionMap = filteredServiceVsServiceDefs.get(service);
                    serviceDefinitionMap.put(operationId, serviceDefinition);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void validateServiceVariable(final String key, final JSONObject o) {
        if (!o.has("operationId")) {
            getLogger().error("Service variable " + key + " does not have operation id ");
        }
        if (!o.has("service")) {
            getLogger().error("Service variable " + key + " does not have service name property ");
        }
    }

    private void persistServiceDefs() {
        for (final String service : filteredServiceVsServiceDefs.keySet()) {
            if (filteredServiceVsServiceDefs.get(service).size() > 0) {
                executorService.submit(new Runnable() {
                    @Override
                    public void run() {
                        persistServiceDefs(service, filteredServiceVsServiceDefs.get(service));
                    }
                });
            }
        }
    }

    protected void persistServiceDefs(final String serviceId, final Map<String, ServiceDefinition> serviceDefMap) {
        File serviceDefResource = getServiceDefResource(targetClassesFolder, serviceId);
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

    protected File getServiceDefResource(final Folder runtimeFolder, final String serviceId) {
        File file = runtimeFolder.getFolder(SERVICE_DEFS).getFile(SERVICE_DEF_RESOURCE_NAME.replace("{}", serviceId));
        if (!file.exists()) {
            file.createIfMissing();
        }
        return file;
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
}

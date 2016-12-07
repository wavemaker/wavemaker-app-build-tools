package com.wavemaker.studio.app.build.maven.plugin.handler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.wavemaker.studio.app.build.servicedef.ServiceDefGenerator;
import com.wavemaker.studio.app.build.exception.ServiceDefGenerationException;
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


    private final String servicesDirectory = "services";
    private final Folder servicesFolder;
    private final Folder rootFolder;

    private ExecutorService executorService = Executors.newFixedThreadPool(5);
    private Map<String, Future<Map<String, ServiceDefinition>>> serviceVsServiceDefs = new HashMap<>();
    private Map<String, Map<String, ServiceDefinition>> filteredServiceDefinitions = new ConcurrentHashMap<>();

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
                    serviceVsServiceDefs.put(serviceFolder.getName(), executorService.submit(new Callable<Map<String, ServiceDefinition>>() {
                        @Override
                        public Map<String, ServiceDefinition> call() throws Exception {
                            return buildServiceDefs(serviceFolder);
                        }
                    }));
                }
            }
            for (String service : serviceVsServiceDefs.keySet()) {
                handleFutureIfException(serviceVsServiceDefs.get(service));
            }
        }
    }

    private Map<String, ServiceDefinition> buildServiceDefs(final Folder serviceFolder) {
        Folder designFolder = serviceFolder.getFolder(DESIGN_TIME_FOLDER);
        String[] possibleSwaggerJsonExtensions = new String[] {API_EXTENSION, REST_SERVICE_API_EXTENSION, WEBSOCKET_SERVICE_API_EXTENSION};
        Swagger swagger = null;
        boolean swaggerFileFound = false;
        for (String possibleSwaggerJsonExtension : possibleSwaggerJsonExtensions) {
            File swaggerFile = designFolder.getFile(designFolder.getParent().getName() + possibleSwaggerJsonExtension);
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
            return swagger != null ? new ServiceDefGenerator(swagger).generate() : new HashMap<String, ServiceDefinition>();
        } catch (ServiceDefGenerationException e) {
            throw new WMRuntimeException("Failed to build service def for service " + swagger.getInfo().getServiceId(), e);
        }

    }


    private void generateServiceDefs() {
        Resources<File> files = rootFolder.find().files().exclude(FilterOn.antPattern("/app/prefabs/**")).include(FilterOn.names().ending(".variables.json"));
        Collection<Callable> callables = new ArrayList<>();
        Collection<Future> futures = new ArrayList<>();
        try {
            for (final File file : files) {
                callables.add(new Callable<Object>() {
                    @Override
                    public Object call() throws Exception {
                        try {
                            generateServiceDefs(file);
                        } catch (JSONException e) {
                            logger.error("Failed to build service definitions for variable json file " + file.getName());
                        }
                        return this;
                    }
                });
            }
            for (Callable callable : callables) {
                futures.add(executorService.submit(callable));
            }
            for (Future<Object> future : futures) {
                handleFutureIfException(future);
            }
        } finally {
            executorService.shutdown();
        }
    }

    private <V> V handleFutureIfException(Future<V> future) {
        Throwable t = null;
        V v = null;
        try {
            v = future.get();
        } catch (CancellationException ce) {
            t = ce;
        } catch (ExecutionException ee) {
            t = ee;
        } catch (InterruptedException e) {
            t = e;
        }
        if (t != null) {
            throw new WMRuntimeException(t);
        }
        return v;
    }

    private void generateServiceDefs(final File file) throws JSONException, ExecutionException, InterruptedException {
        String s = file.getContent().asString();
        if (StringUtils.isBlank(s)) {
            return;
        }
        JSONObject jsonObject = new JSONObject(s);
        Iterator keys = jsonObject.keys();
        while (keys.hasNext()) {
            String key = (String) keys.next();
            JSONObject o = (JSONObject) jsonObject.get(key);
            if (o.has("category")) {
                String category = o.getString("category");
                if (!(WM_SERVICE_VARIABLE.equals(category) || WEBSOCKET_VARIABLE.equals(category))) {
                    continue;
                }
                if (!o.has("operationId")) {
                    logger.warn("Service variable " + key + " does not have operation id ");
                    continue;
                }
                if (!o.has("service")) {
                    logger.warn("Service variable " + key + " does not have service name property ");
                    continue;
                }
                String operationId = o.getString("operationId");
                String service = o.getString("service");
                if (serviceVsServiceDefs.get(service) == null) {
                    logger.warn("Service " + service + " does not exist for the service variable" + key);
                    continue;
                }
                synchronized (filteredServiceDefinitions) {
                    if (!filteredServiceDefinitions.containsKey(service)) {
                        filteredServiceDefinitions.put(service, new ConcurrentHashMap<String, ServiceDefinition>());
                    }
                }
                final Map<String, ServiceDefinition> serviceDefinitions = serviceVsServiceDefs.get(service).get();
                if (serviceDefinitions.containsKey(operationId)) {
                    ServiceDefinition serviceDefinition = serviceDefinitions.get(operationId);
                    Map<String, ServiceDefinition> serviceDefinitionMap = filteredServiceDefinitions.get(service);
                    serviceDefinitionMap.put(operationId, serviceDefinition);
                }
            }
        }
    }


    protected void persistServiceDefs() {
        for (final String service : filteredServiceDefinitions.keySet()) {
            if (filteredServiceDefinitions.get(service).size() > 0) {
                persistServiceDefs(service, filteredServiceDefinitions.get(service));
            }
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
            org.apache.commons.io.IOUtils.closeQuietly(outputStream);
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
            Swagger swagger = JSONUtils.toObject(is, Swagger.class);
            return swagger;
        } catch (Exception e) {
            throw new WMRuntimeException("Failed to parse swagger file ", e);
        } finally {
            IOUtils.closeSilently(is);
        }
    }
}

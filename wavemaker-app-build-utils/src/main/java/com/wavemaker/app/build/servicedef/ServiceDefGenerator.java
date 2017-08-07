/**
 * Copyright © 2013 - 2017 WaveMaker, Inc.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.wavemaker.app.build.servicedef;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.wavemaker.app.build.adapter.ServiceDefPropertiesAdapter;
import com.wavemaker.app.build.exception.ServiceDefGenerationException;
import com.wavemaker.commons.OperationNotExistException;
import com.wavemaker.commons.json.JSONUtils;
import com.wavemaker.commons.servicedef.model.Parameter;
import com.wavemaker.commons.servicedef.model.RuntimeProxySettings;
import com.wavemaker.commons.servicedef.model.ServiceDefinition;
import com.wavemaker.commons.servicedef.model.WMServiceOperationInfo;
import com.wavemaker.commons.swaggerdoc.constants.WebSwaggerConstants;
import com.wavemaker.commons.swaggerdoc.handler.OperationHandler;
import com.wavemaker.commons.swaggerdoc.handler.PathHandler;
import com.wavemaker.commons.swaggerdoc.util.SwaggerDocUtil;
import com.wavemaker.tools.apidocs.tools.core.model.Info;
import com.wavemaker.tools.apidocs.tools.core.model.Operation;
import com.wavemaker.tools.apidocs.tools.core.model.Path;
import com.wavemaker.tools.apidocs.tools.core.model.Swagger;
import com.wavemaker.tools.apidocs.tools.core.model.VendorUtils;
import com.wavemaker.tools.apidocs.tools.core.model.auth.SecuritySchemeDefinition;
import com.wavemaker.tools.apidocs.tools.core.model.parameters.AbstractParameter;

/**
 * @author <a href="mailto:sunil.pulugula@wavemaker.com">Sunil Kumar</a>
 * @since 29/4/16
 */
public class ServiceDefGenerator {

    public static final int PARAMETERS_DEPTH = 1;
    private final ServiceDefPropertiesAdapter serviceDefPropertiesAdapter = new ServiceDefPropertiesAdapter();

    private final Swagger swagger;

    public ServiceDefGenerator(final Swagger swagger) {
        this.swagger = swagger;
    }

    /**
     * Generates service definitions for all operation from swagger.
     */
    public Map<String, ServiceDefinition> generate() throws ServiceDefGenerationException {
        Map<String, ServiceDefinition> serviceDefs = new HashMap<>();
        if (swagger.getPaths() != null) {
            try {
                for (Map.Entry entry : swagger.getPaths().entrySet()) {
                    Path path = (Path) entry.getValue();
                    for (Operation operation : path.getOperations())
                        if (operation != null) {

                            final String operationHttpType = new PathHandler(entry.getKey().toString(), path).getOperationType(operation.getOperationId());
                            final String operationType = new OperationHandler(operation, swagger.getDefinitions()).getFullyQualifiedReturnType();
                            final String serviceOperationRelativePath = getServiceOperationRelativePath(swagger, path);
                            final WMServiceOperationInfo operationInfo = buildWMServiceOperationInfo(swagger,
                                    operation, operationHttpType, serviceOperationRelativePath, path.getCompletePath());


                            serviceDefs.put(operation.getOperationId(), new ServiceDefinition().getNewInstance()
                                    .addId(operation.getOperationId())
                                    .addController(operation.getTags().get(0))
                                    .addType(operationType)
                                    .addOperationType(operationType)
                                    .addService(swagger.getInfo().getServiceId())
                                    .addWmServiceOperationInfo(operationInfo));
                        }
                }
            } catch (Exception e) {
                throw new ServiceDefGenerationException(e);
            }
        }
        return serviceDefs;
    }

    /**
     * Generates service definition for given operation Id.
     *
     * @param operationId
     * @return service definition for given operation Id.
     * @throws OperationNotExistException when operationId does not exist in the swagger object.
     */
    public ServiceDefinition generate(String operationId) throws OperationNotExistException, ServiceDefGenerationException {
        try {
            for (Map.Entry entry : swagger.getPaths().entrySet()) {
                Path path = (Path) entry.getValue();
                for (Operation operation : path.getOperations())
                    if (operation != null) {
                        if (operation.getOperationId().equals(operationId)) {
                            final String operationHttpType = new PathHandler(entry.getKey().toString(), path).getOperationType(operation.getOperationId());
                            final String operationType = new OperationHandler(operation, swagger.getDefinitions()).getFullyQualifiedReturnType();
                            final String serviceOperationRelativePath = getServiceOperationRelativePath(swagger, path);
                            final WMServiceOperationInfo operationInfo = buildWMServiceOperationInfo(swagger,
                                    operation, operationHttpType, serviceOperationRelativePath, path.getCompletePath());


                            return new ServiceDefinition().getNewInstance()
                                    .addId(operation.getOperationId())
                                    .addController(operation.getTags().get(0))
                                    .addType(operationType)
                                    .addOperationType(operationType)
                                    .addService(swagger.getInfo().getServiceId())
                                    .addWmServiceOperationInfo(operationInfo);
                        }
                    }
            }
        } catch (Exception e) {
            throw new ServiceDefGenerationException(e);
        }
        throw new OperationNotExistException("Operation Id " + operationId + " does not exist in service " + swagger.getInfo().getServiceId());
    }


    private WMServiceOperationInfo buildWMServiceOperationInfo(final Swagger swagger, final Operation operation,
                                                               final String httpMethod, final String relativePath,
                                                               final String directPath) {
        List<Parameter> parameters = buildParameters(swagger, operation);
        Map<String, Set<Parameter>> definitions = buildDefinitions(swagger, operation);
        RuntimeProxySettings proxySettings = getProxySettings(swagger);
        List<SecuritySchemeDefinition> securityDefinitions = getSecurityDefinitions(swagger, operation);
        return WMServiceOperationInfo.getNewInstance()
                .addName(operation.getMethodName())
                .addHttpMethod(httpMethod)
                .addRelativePath(relativePath)
                .addDirectPath(directPath)
                .addConsumes(operation.getConsumes())
                .addProduces(operation.getProduces())
                .addMethodType(httpMethod)
                .addParameters(parameters)
                .addDefinitions(definitions)
                .addProxySettings(proxySettings)
                .addSecurityDefinitions(securityDefinitions);
    }

    private List<SecuritySchemeDefinition> getSecurityDefinitions(Swagger swagger, Operation operation) {
        List<Map<String, List<String>>> securityList = operation.getSecurity();
        Map<String, SecuritySchemeDefinition> securitySchemeDefinitionMap = swagger.getSecurityDefinitions();
        if (securitySchemeDefinitionMap == null || securitySchemeDefinitionMap.isEmpty()) {
            return null;
        }
        List<SecuritySchemeDefinition> securitySchemeDefinitions = new ArrayList<>();
        for (Map<String, List<String>> security : securityList) {
            String authenticationType = security.keySet().iterator().next();
            securitySchemeDefinitions.add(securitySchemeDefinitionMap.get(authenticationType));
        }
        return securitySchemeDefinitions;
    }

    private Map<String, Set<Parameter>> buildDefinitions(final Swagger swagger, final Operation operation) {
        Map<String, Set<Parameter>> definitions = new HashMap<>();
        for (com.wavemaker.tools.apidocs.tools.core.model.parameters.Parameter parameter : operation.getParameters()) {
            buildDefinitions(swagger, parameter, definitions);
        }
        return definitions;
    }

    private void buildDefinitions(final Swagger swagger, final com.wavemaker.tools.apidocs.tools.core.model.parameters.Parameter parameter, final Map<String, Set<Parameter>> definitions) {
        final ServiceDefDefinitionsAdapter serviceDefDefinitionsAdapter = new ServiceDefDefinitionsAdapter(swagger, new ServiceDefParameterCriteria() {
            @Override
            public boolean meetCriteria(final Parameter parameter) {
                return true;
            }
        });
        final Map<String, Set<Parameter>> requiredDefinitions = serviceDefDefinitionsAdapter.adaptToDefinitions(parameter, PARAMETERS_DEPTH);
        if (requiredDefinitions != null) {
            definitions.putAll(requiredDefinitions);
        }
    }

    private RuntimeProxySettings getProxySettings(final Swagger swagger) {
        Info info = swagger.getInfo();
        Object webProxy = VendorUtils.getWMExtension(info, WebSwaggerConstants.USE_PROXY_FOR_WEB);
        Object mobileProxy = VendorUtils.getWMExtension(info, WebSwaggerConstants.USE_PROXY_FOR_MOBILE);
        boolean useProxyForWeb = (webProxy != null) ? Boolean.valueOf(webProxy.toString()) : true;
        boolean useProxyForMobile = (mobileProxy != null) ? Boolean.valueOf(mobileProxy.toString()) : true;
        return new RuntimeProxySettings(useProxyForWeb, useProxyForMobile);
    }

    private List<Parameter> buildParameters(final Swagger swagger, final Operation operation) {
        List<Parameter> parameters = new ArrayList<>();
        for (com.wavemaker.tools.apidocs.tools.core.model.parameters.Parameter parameter : operation.getParameters()) {
            Parameter defParameter = buildParameter(swagger, parameter);
            parameters.add(defParameter);
        }
        buildSecurityParameters(swagger, operation, parameters);
        return parameters;
    }

    private Parameter buildParameter(final Swagger swagger, final com.wavemaker.tools.apidocs.tools.core.model.parameters.Parameter parameter) {
        final String fullyQualifiedName = SwaggerDocUtil.getParameterType(parameter);
        final String name = (parameter.getName() == null) ? parameter.getIn().toLowerCase() : parameter.getName();
        final String contentType = ((AbstractParameter) parameter).getContentType();

        return Parameter.getNewInstance()
                .addName(name)
                .addParameterType(parameter.getIn())
                .addRequired(parameter.getRequired())
                .addType(fullyQualifiedName)
                .addContentType(contentType);
    }

    private void buildSecurityParameters(final Swagger swagger, final Operation operation, final List<Parameter> parameters) {
        if (swagger.getSecurityDefinitions() != null) {
            SecuritySchemeDefinition securitySchemeDefinition = swagger.getSecurityDefinitions().get("WM_Rest_Service_Authorization");
            final List<Map<String, List<String>>> operationSecurity = operation.getSecurity();
            if (securitySchemeDefinition != null && securitySchemeDefinition.getType().equals("basic")) {
                if (operationSecurity != null && operationSecurity.size() > 0) {
                    if (operationSecurity.get(0).get("WM_Rest_Service_Authorization") != null) {
                        final Parameter userNameParameter = new Parameter();
                        userNameParameter.setName("wm_auth_username");
                        userNameParameter.setParameterType("auth");
                        parameters.add(userNameParameter);

                        final Parameter passwordParameter = new Parameter();
                        passwordParameter.setName("wm_auth_password");
                        passwordParameter.setParameterType("auth");
                        parameters.add(passwordParameter);
                    }
                }

            }
        }
    }

    private String getServiceOperationRelativePath(Swagger swagger, Path path) {
        String swaggerBasePath = swagger.getBasePath();
        String pathBasePath = path.getBasePath();
        if (swaggerBasePath == null) {
            return pathBasePath + path.getRelativePath();
        } else if (swaggerBasePath.endsWith("/") && pathBasePath.startsWith("/")) {
            swaggerBasePath = swaggerBasePath.substring(0, swaggerBasePath.length() - 1);
        }
        return swaggerBasePath + pathBasePath + path.getRelativePath();
    }

    public static void main(String[] args) throws OperationNotExistException, ServiceDefGenerationException {
        File file = new File("/home/sunilp/Softwares/studio_setup/workspace/saas/users/j44nsmxt39/workspace/default/projects/T1/services/MyJavaService/designtime/MyJavaService_API.json");
        try {
            final Swagger swagger = JSONUtils.toObject(file, Swagger.class);
            ServiceDefGenerator serviceDefGenerator = new ServiceDefGenerator(swagger);
            final ServiceDefinition myJavaController_sampleJavaOperation = serviceDefGenerator.generate("MyJavaController_sampleJavaOperation");
            System.out.println(myJavaController_sampleJavaOperation);
        } catch (IOException e) {

        }
    }

}

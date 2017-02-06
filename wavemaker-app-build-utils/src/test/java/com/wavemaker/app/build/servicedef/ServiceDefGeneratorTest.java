package com.wavemaker.app.build.servicedef;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import com.wavemaker.app.build.exception.ServiceDefGenerationException;
import com.wavemaker.commons.OperationNotExistException;
import com.wavemaker.commons.json.JSONUtils;
import com.wavemaker.commons.servicedef.model.ServiceDefinition;
import com.wavemaker.tools.apidocs.tools.core.model.Swagger;

/**
 * @author <a href="mailto:sunil.pulugula@wavemaker.com">Sunil Kumar</a>
 * @since 6/2/17
 */
public class ServiceDefGeneratorTest {

    //test case to generate service defs from swagger.
    public static void main(String[] args) {
        try {
            final Swagger swagger = JSONUtils.toObject(new File("../designtime/hrdb_API.json"), Swagger.class);
            ServiceDefGenerator serviceDefGenerator = new ServiceDefGenerator(swagger);

            final ServiceDefinition serviceDefinition = serviceDefGenerator.generate("UserController_createUser");
            System.out.println(serviceDefinition);

            final Map<String, ServiceDefinition> serviceDefinitions = serviceDefGenerator.generate();
            System.out.println(serviceDefinitions);
        } catch (IOException | OperationNotExistException | ServiceDefGenerationException e) {
            throw new RuntimeException(e);
        }
    }

}

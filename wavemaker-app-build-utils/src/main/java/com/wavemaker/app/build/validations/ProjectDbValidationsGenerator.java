package com.wavemaker.app.build.validations;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wavemaker.app.build.ProjectServicesHelper;
import com.wavemaker.commons.WMRuntimeException;
import com.wavemaker.commons.io.File;
import com.wavemaker.commons.io.Folder;
import com.wavemaker.commons.json.JSONUtils;
import com.wavemaker.commons.validations.DbMetaData;
import com.wavemaker.commons.validations.DbValidationsConstants;
import com.wavemaker.commons.validations.TableColumnMetaData;
import com.wavemaker.tools.apidocs.tools.core.model.Constraint;
import com.wavemaker.tools.apidocs.tools.core.model.Model;
import com.wavemaker.tools.apidocs.tools.core.model.ModelImpl;
import com.wavemaker.tools.apidocs.tools.core.model.Swagger;
import com.wavemaker.tools.apidocs.tools.core.model.properties.AbstractProperty;
import com.wavemaker.tools.apidocs.tools.core.model.properties.Property;

/**
 * Created by srujant on 23/8/17.
 */
public class ProjectDbValidationsGenerator {

    private static final String SERVICES = "services";
    private static final String WEB_INF = "src/main/webapp/WEB-INF";
    private static final String API_JSON = "_API.json";
    private static final String DESIGN_TIME_FOLDER = "designtime";
    private static final String DATA_SERVICE = "DataService";
    private ObjectMapper objectMapper;
    private Folder servicesFolder;
    private Folder webInfFolder;

    Logger logger = LoggerFactory.getLogger(ProjectDbValidationsGenerator.class);

    public ProjectDbValidationsGenerator(Folder rootFolder) {
        this.servicesFolder = rootFolder.getFolder(SERVICES);
        this.webInfFolder = rootFolder.getFolder(WEB_INF);
        objectMapper = new ObjectMapper();
    }

    public void generate() {
        List<Swagger> projectDbSwaggersList = new ArrayList<>();
        List<Folder> serviceFolders = servicesFolder.list().folders().fetchAll();
        if (serviceFolders.size() > 0) {
            for (Folder serviceFolder : serviceFolders) {
                addSwaggerToListIfServiceTypeIsDbService(projectDbSwaggersList, serviceFolder);
            }
        }
        if (projectDbSwaggersList.isEmpty()) {
            return;
        }
        logger.info("Generating dbValidationsJson file");
        File dbValidationsJsonFile = webInfFolder.getFile(DbValidationsConstants.DB_VALIDATIONS_JSON_FILE);
        if (!dbValidationsJsonFile.exists()) {
            dbValidationsJsonFile.createIfMissing();
        }

        Map<String, DbMetaData> projectDbValidationsMap = getValidationsMap(projectDbSwaggersList);
        try {
            JSONUtils.toJSON(dbValidationsJsonFile.getContent().asOutputStream(), projectDbValidationsMap, true);
        } catch (IOException e) {
            throw new WMRuntimeException(e);
        }
    }


    private void addSwaggerToListIfServiceTypeIsDbService(List<Swagger> swaggers, Folder serviceFolder) {
        if (serviceFolder.exists()) {
            String serviceType = ProjectServicesHelper.findServiceType(serviceFolder);
            if (DATA_SERVICE.equals(serviceType)) {
                File swaggerFile = serviceFolder.getFolder(DESIGN_TIME_FOLDER).getFile(serviceFolder.getName() + API_JSON);
                try {
                    swaggers.add(objectMapper.readValue(swaggerFile.getContent().asInputStream(), Swagger.class));
                } catch (IOException e) {
                    throw new WMRuntimeException(e);
                }
            }
        }
    }

    private Map<String, DbMetaData> getValidationsMap(List<Swagger> projectDbSwaggersList) {
        Map<String, DbMetaData> dbConstraintsMap = new LinkedHashMap<>();
        projectDbSwaggersList.forEach(swagger -> {
            Map<String, Model> definitions = swagger.getDefinitions();
            definitions.forEach((tableName, model) -> {
                if (model instanceof ModelImpl) {
                    Map<String, Property> propertyMap = model.getProperties();
                    if (propertyMap != null) {
                        Map<String, TableColumnMetaData> tableColumnsMetaDataMap = new LinkedHashMap<>();
                        propertyMap.forEach((columnName, property) -> {
                            List<Constraint> constraints = ((AbstractProperty) property).getConstraints();
                            if (!CollectionUtils.isEmpty(constraints)) {
                                TableColumnMetaData tableColumnMetaData = new TableColumnMetaData();
                                tableColumnMetaData.setConstraints(constraints);
                                tableColumnsMetaDataMap.put(columnName, tableColumnMetaData);
                            }
                        });

                        if (!tableColumnsMetaDataMap.isEmpty()) {
                            String fullyQualifiedName = ((ModelImpl) model).getFullyQualifiedName();
                            DbMetaData dbMetaData = new DbMetaData();
                            dbMetaData.setProperties(tableColumnsMetaDataMap);
                            dbConstraintsMap.put(fullyQualifiedName, dbMetaData);
                        }
                    }
                }
            });

        });
        return dbConstraintsMap;
    }

}

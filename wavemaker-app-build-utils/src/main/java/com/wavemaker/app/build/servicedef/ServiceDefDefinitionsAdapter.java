package com.wavemaker.app.build.servicedef;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.wavemaker.app.build.swaggerdoc.handler.PropertyHandler;
import com.wavemaker.tools.apidocs.tools.core.model.*;
import com.wavemaker.tools.apidocs.tools.core.model.parameters.BodyParameter;
import com.wavemaker.tools.apidocs.tools.core.model.parameters.Parameter;
import com.wavemaker.tools.apidocs.tools.core.model.properties.ArrayProperty;
import com.wavemaker.tools.apidocs.tools.core.model.properties.Property;
import com.wavemaker.tools.apidocs.tools.core.model.properties.RefProperty;

/**
 * @author <a href="mailto:sunil.pulugula@wavemaker.com">Sunil Kumar</a>
 * @since 9/2/17
 */
public class ServiceDefDefinitionsAdapter {

    private final Swagger swagger;
    private final ServiceDefParameterCriteria criteria;
    Map<String, List<com.wavemaker.commons.servicedef.model.Parameter>> parameters = new HashMap<>();

    public ServiceDefDefinitionsAdapter(final Swagger swagger, final ServiceDefParameterCriteria criteria) {
        this.swagger = swagger;
        this.criteria = criteria;
    }

    public Map<String, List<com.wavemaker.commons.servicedef.model.Parameter>> adaptToDefinitions(final Parameter parameter, final int depth) {
        final Map<String, Model> definitions = swagger.getDefinitions();
        if (parameter instanceof BodyParameter) {
            BodyParameter bodyParameter = (BodyParameter) parameter;
            Model model = bodyParameter.getSchema();
            if (model instanceof RefModel) {
                // when body is Object or Object<Object>
                RefModel refModel = (RefModel) model;
                generateFieldsFromRefModels(refModel, depth);
            }
            if (model instanceof ArrayModel) {
                //When Body is List<String>,Set<Object>...
                ArrayModel arrayModel = (ArrayModel) model;
                if (!arrayModel.isList()) {

                }
                Property property = arrayModel.getItems();
                PropertyHandler propertyHandler = new PropertyHandler(property, swagger.getDefinitions());
                if (propertyHandler.isPrimitive()) {
                    return null;
                } else {
                    if (property instanceof RefProperty) {
                        String refName = ((RefProperty) property).getSimpleRef();
                        final Model actualModel = definitions.get(refName);
                        generateFields(actualModel, depth);
                    }
                    if (property instanceof ArrayProperty) {
                        //FIXME check if this case exist i,e List<List<Integer>> or Array[List<Object>]..
                        return null;
                    }
                }
            }
        }
        return parameters;
    }

    private void generateFieldsFromRefModels(final RefModel refModel, final int depth) {
        final Map<String, Model> definitions = swagger.getDefinitions();
        List<Model> argumentModel = refModel.getTypeArguments();
        if (argumentModel.size() > 0) {
            //considering first argument if model have multiple types like Employee<OldEmployee,NewEmployee,.....>
            Model argModel = argumentModel.get(0);
            if (argModel instanceof RefModel) {
                RefModel refArgModel = (RefModel) argModel;
                Model actualArgModel = definitions.get(refArgModel.getSimpleRef());
                if (actualArgModel != null) {
                    generateFields(actualArgModel, depth);
                }
            }
        } else {
            final Model model = definitions.get(refModel.getSimpleRef());
            generateFields(model, depth);
        }
    }


    private void generateFields(final Model model, final int depth) {
        if (model instanceof ModelImpl) {
            generateFieldsFromModel(model);
        } else if (model instanceof ComposedModel) {
            ComposedModel composedModel = (ComposedModel) model;
            final List<Model> allOf = composedModel.getAllOf();
            for (Model eachModel : allOf) {
                generateFields(eachModel, depth);
            }
            final Map<String, Property> properties = composedModel.getProperties();
            if (properties != null) {
                for (Map.Entry<String, Property> propertyEntry : properties.entrySet()) {
                    final com.wavemaker.commons.servicedef.model.Parameter parameter = buildParameter(propertyEntry.getKey(), propertyEntry.getValue());
                    addParameter(composedModel.getFullyQualifiedName(), parameter);
                   /* if (propertyEntry.getValue() instanceof RefProperty) {
                        handleRefProperty(propertyEntry.getKey(), (RefProperty) propertyEntry.getValue(),depth-1);
                    }*/
                }
            }
        }
    }


    private void generateFieldsFromModel(Model model) {
        ModelImpl actualModel = (ModelImpl) model;
        final Map<String, Property> properties = actualModel.getProperties();
        if (properties != null) {
            for (String propertyName : properties.keySet()) {
                final Property property = properties.get(propertyName);
                final PropertyHandler propertyHandler = new PropertyHandler(property, swagger.getDefinitions());
                if (propertyHandler.isPrimitive()) {
                    final com.wavemaker.commons.servicedef.model.Parameter parameter = buildParameter(propertyName, property);
                    addParameter(((ModelImpl) model).getFullyQualifiedName(), parameter);
                } else if (property instanceof ArrayProperty) {
                    //handleArrayProperty(actualModel,propertyName, (ArrayProperty) property);
                } else if (property instanceof RefProperty) {
                    final com.wavemaker.commons.servicedef.model.Parameter parameter = buildParameter(propertyName, property);
                    RefProperty refProperty = (RefProperty) property;
                    addParameter(actualModel.getFullyQualifiedName(), parameter);
                    //handleRefProperty(propertyName, (RefProperty) property, depth);
                }
            }
        }
    }


    protected com.wavemaker.commons.servicedef.model.Parameter buildParameter(final String name, final Property property) {
        com.wavemaker.commons.servicedef.model.Parameter parameter = new com.wavemaker.commons.servicedef.model.Parameter();
        PropertyHandler propertyHandler = new PropertyHandler(property, swagger.getDefinitions());
        parameter.addName(name)
                .addType(property.getType())
                .addRequired(property.getRequired());
        return parameter;
    }

    protected void addParameter(final String key, final com.wavemaker.commons.servicedef.model.Parameter value) {
        if (!parameters.keySet().contains(key)) {
            parameters.put(key, new ArrayList<com.wavemaker.commons.servicedef.model.Parameter>());
        }
        parameters.get(key).add(value);
    }
}

package com.wavemaker.app.build.servicedef;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.wavemaker.app.build.swaggerdoc.handler.PropertyHandler;
import com.wavemaker.tools.apidocs.tools.core.model.ArrayModel;
import com.wavemaker.tools.apidocs.tools.core.model.ComposedModel;
import com.wavemaker.tools.apidocs.tools.core.model.Model;
import com.wavemaker.tools.apidocs.tools.core.model.ModelImpl;
import com.wavemaker.tools.apidocs.tools.core.model.RefModel;
import com.wavemaker.tools.apidocs.tools.core.model.Swagger;
import com.wavemaker.tools.apidocs.tools.core.model.parameters.BodyParameter;
import com.wavemaker.tools.apidocs.tools.core.model.parameters.FormParameter;
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
    Map<String, Set<com.wavemaker.commons.servicedef.model.Parameter>> parameters = new HashMap<>();

    public ServiceDefDefinitionsAdapter(final Swagger swagger, final ServiceDefParameterCriteria criteria) {
        this.swagger = swagger;
        this.criteria = criteria;
    }

    public Map<String, Set<com.wavemaker.commons.servicedef.model.Parameter>> adaptToDefinitions(final Parameter parameter, final int depth) {
        final Map<String, Model> definitions = swagger.getDefinitions();
        if (parameter instanceof BodyParameter || parameter instanceof FormParameter) {
            Model model;
            if(parameter instanceof FormParameter) {
                model = ((FormParameter) parameter).getSchema();
            }else {
                model = ((BodyParameter)parameter).getSchema();
            }
            if (model instanceof RefModel) {
                // when body is Object or Object<Object>
                RefModel refModel = (RefModel) model;
                generateFieldsFromRefModels(refModel, depth);
            }
            if (model instanceof ArrayModel) {
                //When Body is List<String>,Set<Object>...
                ArrayModel arrayModel = (ArrayModel) model;
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
        if (!argumentModel.isEmpty()) {
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
        if (depth > 0) {
            if (model instanceof ModelImpl) {
                generateFieldsFromModel(model, depth);
            } else if (model instanceof ComposedModel) {
                ComposedModel composedModel = (ComposedModel) model;
                final List<Model> allOf = composedModel.getAllOf();
                for (Model eachModel : allOf) {
                    if (eachModel instanceof ModelImpl) {
                        final Map<String, Property> properties = composedModel.getProperties();
                        if (properties != null) {
                            for (Map.Entry<String, Property> propertyEntry : properties.entrySet()) {
                                final com.wavemaker.commons.servicedef.model.Parameter parameter = buildParameter(propertyEntry.getKey(), propertyEntry.getValue());
                                addParameter(composedModel.getFullyQualifiedName(), parameter);
                                if (propertyEntry.getValue() instanceof RefProperty) {
                                    handleRefProperty(propertyEntry.getKey(), (RefProperty) propertyEntry.getValue(), depth - 1);
                                } else if (propertyEntry.getValue() instanceof ArrayProperty) {
                                    handleArrayProperty(propertyEntry.getKey(), (ArrayProperty) propertyEntry.getValue(), depth - 1);
                                }
                            }
                        }
                    } else {
                        generateFields(eachModel, depth);
                    }
                }
            }
        }
    }


    private void generateFieldsFromModel(Model model, final int depth) {
        ModelImpl actualModel = (ModelImpl) model;
        final Map<String, Property> properties = actualModel.getProperties();
        if (properties != null) {
            for (String propertyName : properties.keySet()) {
                final Property property = properties.get(propertyName);
                final com.wavemaker.commons.servicedef.model.Parameter parameter = buildParameter(propertyName, property);
                addParameter(actualModel.getFullyQualifiedName(), parameter);
                if (property instanceof ArrayProperty) {
                    handleArrayProperty(propertyName, (ArrayProperty) property, depth - 1);
                } else if (property instanceof RefProperty) {
                    handleRefProperty(propertyName, (RefProperty) property, depth - 1);
                }
            }
        }
    }


    private void handleRefProperty(final String propertyName, final RefProperty property, final int depth) {
        RefProperty refProperty = property;
        // TODO this case needs to be handle
        //this case occurs when property is Object<Object,Object....> eq : Page<Employee>
        /*List<Property> argProperties = refProperty.getTypeArguments();
        if (argProperties.size() > 0) {
            for (Property argProperty : argProperties) {
                handleProperty(propertyName, argProperty);
            }
        }*/
        final Model model = swagger.getDefinitions().get(((RefProperty) property).getSimpleRef());
        generateFields(model, depth);

    }

    private void handleArrayProperty(final String propertyName, final ArrayProperty property, final int depth) {
        //this case occurs what property is List<int> || Set<Emp> || List<User> || Set<String> || ......
        ArrayProperty arrayProperty = property;
        boolean isList = arrayProperty.isList();
        if (isList) {
            Property argProperty = arrayProperty.getItems();
            if (argProperty instanceof RefProperty) {
                // case : List<someObject> or Set<someObject>
                RefProperty refProperty = (RefProperty) argProperty;
                final Model refModel = swagger.getDefinitions().get(refProperty.getSimpleRef());
                generateFields(refModel, depth);
            }
        }
    }


    protected com.wavemaker.commons.servicedef.model.Parameter buildParameter(final String name, final Property property) {
        com.wavemaker.commons.servicedef.model.Parameter parameter = new com.wavemaker.commons.servicedef.model.Parameter();
        PropertyHandler propertyHandler = new PropertyHandler(property, swagger.getDefinitions());
        String type = propertyHandler.getFullyQualifiedType();
        parameter.addName(name)
                .addType(type)
                .addRequired(property.getRequired())
                .addReadOnly(property.getReadOnly());
        return parameter;
    }

    protected void addParameter(final String key, final com.wavemaker.commons.servicedef.model.Parameter value) {
        if (!parameters.keySet().contains(key)) {
            parameters.put(key, new HashSet<>());
        }
        if (criteria.meetCriteria(value)) {
            parameters.get(key).add(value);
        }
    }
}

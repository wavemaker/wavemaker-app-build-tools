package com.wavemaker.app.build.swaggerdoc.handler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.wavemaker.tools.apidocs.tools.core.model.ComposedModel;
import com.wavemaker.tools.apidocs.tools.core.model.Model;
import com.wavemaker.tools.apidocs.tools.core.model.ModelImpl;
import com.wavemaker.tools.apidocs.tools.core.model.RefModel;
import com.wavemaker.tools.apidocs.tools.core.model.properties.ArrayProperty;
import com.wavemaker.tools.apidocs.tools.core.model.properties.Property;
import com.wavemaker.tools.apidocs.tools.core.model.properties.RefProperty;

/**
 * Created by sunilp on 12/8/15.
 */
public class ModelHandler {

    private final Model model;
    private final Map<String, Model> definitions;

    public ModelHandler(Model model, Map<String, Model> definitions) {
        this.model = model;
        this.definitions = definitions;
    }

    public Map<String, Property> getProperties() {
        Map<String, Property> properties = new HashMap<>();
        return buildProperties(model, properties);
    }

    // recursive api to build all properties from model,parent model and so on.
    private Map<String, Property> buildProperties(Model model, Map<String, Property> properties) {

        if (model != null) {
            //some times model can be composed,Composed means if a class inherits another class or interface.
            if (model instanceof ComposedModel) {
                List<Model> allModels = ((ComposedModel) model).getAllOf();
                for (Model eachModel : allModels) {
                    properties.putAll(buildProperties(eachModel, properties));
                }
            } else if (model instanceof RefModel) {
                buildProperties(definitions.get(((RefModel) model).getSimpleRef()), properties);
            } else {
                // in other models such as array,modelImpl,abstract getting properties are straight forward.
                if (model.getProperties() != null) {
                    properties.putAll(model.getProperties());
                }
            }
        }

        return properties;
    }


    public Map<String, Property> listProperties(Model model, int level) {
        Map<String, Property> propertiesMap = new HashMap<>();
        listPropertiesByModel(model, level, propertiesMap);
        return propertiesMap;
    }

    private void listPropertiesByModel(final Model model, final int level, final Map<String, Property> propertiesMap) {
        if (model instanceof ModelImpl) {
            listProperties(model, level, propertiesMap);
        } else if (model instanceof ComposedModel) {
            ComposedModel composedModel = (ComposedModel) model;
            final List<Model> allOf = composedModel.getAllOf();
            for (Model eachModel : allOf) {
                listPropertiesByModel(eachModel, level, propertiesMap);
            }
            final Map<String, Property> properties = composedModel.getProperties();
            if (properties != null) {
                for (Map.Entry<String, Property> propertyEntry : properties.entrySet()) {
                    if (propertyEntry.getValue() instanceof RefProperty) {
                        handleRefProperty(propertyEntry.getKey(), (RefProperty) propertyEntry.getValue(), propertiesMap, level);
                    }
                }
            }
        }
    }

    private void listProperties(Model model, int level, Map<String, Property> propertiesMap) {
        ModelImpl actualModel = (ModelImpl) model;
        if (level > 0) {
            final Map<String, Property> properties = actualModel.getProperties();
            if (properties != null) {
                final List<String> required = actualModel.getRequired();
                for (String propertyName : properties.keySet()) {
                    final Property property = properties.get(propertyName);
                    if (required != null && required.contains(propertyName)) {
                        final PropertyHandler propertyHandler = new PropertyHandler(property, definitions);
                        if (propertyHandler.isPrimitive()) {
                            propertiesMap.put(propertyName, property);
                        } else if (property instanceof ArrayProperty) {
                            handleArrayProperty(propertyName, (ArrayProperty) property, propertiesMap, level);

                        } else if (property instanceof RefProperty) {
                            handleRefProperty(propertyName, (RefProperty) property, propertiesMap, level);

                        }
                    }
                }
            }
        }
    }

    private void handleArrayProperty(final String propertyName, final ArrayProperty property, final Map<String, Property> propertiesMap, final int level) {
        //this case occurs what property is List<int> || Set<Emp> || List<User> || Set<String> || ......
        ArrayProperty arrayProperty = property;
        boolean isList = arrayProperty.isList();
        if (isList) {
            Property argProperty = arrayProperty.getItems();
            if (argProperty instanceof RefProperty) {
                // case : List<someObject> or Set<someObject>
                RefProperty refProperty = (RefProperty) argProperty;
                PropertyHandler refPropertyHandler = new PropertyHandler(refProperty, definitions);
                final Model refModel = definitions.get(refProperty.getSimpleRef());
                propertiesMap.put(propertyName, refProperty);
                listProperties(refModel, level - 1, propertiesMap);
            } else {
                //case : List<primitive> or Set<primitive>
                propertiesMap.put(propertyName, argProperty);
            }
        }
    }

    private void handleRefProperty(final String propertyName, final RefProperty property, final Map<String, Property> propertiesMap, final int level) {
        //this case occurs when property is Object<Object,Object....> eq : Page<Employee>
        RefProperty refProperty = property;
        List<Property> argProperties = refProperty.getTypeArguments();
        if (argProperties.size() > 0) {
            for (Property argProperty : argProperties) {
                handleProperty(propertyName, propertiesMap, level, argProperty);
            }
        } else {
            handleProperty(propertyName, propertiesMap, level, refProperty);
        }
    }

    private void handleProperty(final String propertyName, final Map<String, Property> propertiesMap, final int level, final Property property) {
        PropertyHandler propertyHandler = new PropertyHandler(property, definitions);
        if (propertyHandler.isPrimitive()) {
            propertiesMap.put(propertyName, property);
        } else if (propertyHandler.isArray()) {
            handleArrayProperty(propertyName, (ArrayProperty) property, propertiesMap, level);
        } else {
            final Model argModel = definitions.get(((RefProperty) property).getSimpleRef());
            propertiesMap.put(propertyName, property);
            listProperties(argModel, level - 1, propertiesMap);
        }
    }
}

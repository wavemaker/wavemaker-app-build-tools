package com.wavemaker.app.build.swaggerdoc.handler;

import java.util.*;

import com.wavemaker.commons.swaggerdoc.util.SwaggerDocUtil;
import com.wavemaker.tools.apidocs.tools.core.model.AbstractModel;
import com.wavemaker.tools.apidocs.tools.core.model.Model;
import com.wavemaker.tools.apidocs.tools.core.model.properties.*;

/**
 * Created by sunilp on 1/6/15.
 */
public class PropertyHandler {


    private Property property;

    private Map<String, Model> definitions;

    public PropertyHandler(Property property, Map<String, Model> definitions) {
        this.property = property;
        this.definitions = definitions;
    }

    public boolean isList() {
        if (property instanceof ArrayProperty) {
            return ((ArrayProperty) property).isList();
        }
        return false;
    }

    public boolean isArray() {
        if (property instanceof ArrayProperty && !((ArrayProperty) property).isList()) {
            return true;
        }
        return false;
    }

    public boolean isPrimitive() {
        if (property instanceof BooleanProperty || property instanceof DateProperty ||
                property instanceof DateTimeProperty || property instanceof DoubleProperty ||
                property instanceof FloatProperty || property instanceof DecimalProperty ||
                property instanceof IntegerProperty || property instanceof LongProperty ||
                property instanceof StringProperty) {
            return true;
        }
        return false;
    }

    public String getFullyQualifiedType() {
        //have to give fully qualified type for primitive properties as well
        if (property instanceof RefProperty) {
            String refName = ((RefProperty) property).getSimpleRef();
            return getModelFullyQualifiedName(refName);
        }
        if (property instanceof ArrayProperty) {
            boolean isList = isList();
            ArrayProperty arrayProperty = (ArrayProperty) property;
            if (isList && !isArray()) {
                //if property is List<Object> or Set<Object>
                if (arrayProperty.getUniqueItems() != null && arrayProperty.getUniqueItems()) {
                    return Set.class.getName();
                } else {
                    return List.class.getName();
                }
            } else {
                if (arrayProperty.getItems() instanceof RefProperty) {
                    //if property is object array eg, Animal[]
                    RefProperty refProperty = (RefProperty) arrayProperty.getItems();
                    if (refProperty != null) {
                        String refName = refProperty.getSimpleRef();
                        return getModelFullyQualifiedName(refName);
                    }
                } else {
                    //if property is primitive array eg, int[],String[]
                    return SwaggerDocUtil.getWrapperPropertyFQType(this.property);
                }
            }
        }
        return null;
    }

    public List<String> geFullyQualifiedTypeArguments() {
        if (property instanceof ArrayProperty) {
            //this case occurs what property is List<int> || Set<Emp> || List<User> || Set<String> || ......
            ArrayProperty arrayProperty = (ArrayProperty) property;
            boolean isList = arrayProperty.isList();
            if (isList) {
                Property argProperty = arrayProperty.getItems();
                if (argProperty instanceof RefProperty) {
                    // case : List<someObject> or Set<someObject>
                    RefProperty refProperty = (RefProperty) argProperty;
                    PropertyHandler propertyHandler = new PropertyHandler(refProperty, definitions);
                    return Arrays.asList(propertyHandler.getFullyQualifiedType());
                } else {
                    //case : List<primitive> or Set<primitive>
                    return Arrays.asList(argProperty.getType());
                }
            }
        }
        if (property instanceof RefProperty) {
            //this case occurs when property is Object<Object,Object....> eq : Page<Employee>
            RefProperty refProperty = (RefProperty) property;
            List<Property> propertyList = refProperty.getTypeArguments();
            List<String> typeArguments = new ArrayList<>();
            for (Property argProperty : propertyList) {
                PropertyHandler propertyHandler = new PropertyHandler(argProperty, definitions);
                String fullyQualifiedName = propertyHandler.getFullyQualifiedType();
                if (fullyQualifiedName != null) {
                    typeArguments.add(fullyQualifiedName);
                }
            }
            return typeArguments;
        }

        // when property is primitive type
        return null;
    }

    private String getModelFullyQualifiedName(String refName) {
        Model model = definitions.get(refName);
        AbstractModel abstractModel = ((AbstractModel) model);
        if (abstractModel != null) {
            return abstractModel.getFullyQualifiedName();
        }
        return null;
    }


}

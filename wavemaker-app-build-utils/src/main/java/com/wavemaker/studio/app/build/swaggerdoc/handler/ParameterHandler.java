package com.wavemaker.studio.app.build.swaggerdoc.handler;

import java.util.List;
import java.util.Map;

import com.wavemaker.tools.apidocs.tools.core.model.AbstractModel;
import com.wavemaker.tools.apidocs.tools.core.model.ArrayModel;
import com.wavemaker.tools.apidocs.tools.core.model.Model;
import com.wavemaker.tools.apidocs.tools.core.model.RefModel;
import com.wavemaker.tools.apidocs.tools.core.model.parameters.AbstractParameter;
import com.wavemaker.tools.apidocs.tools.core.model.parameters.BodyParameter;
import com.wavemaker.tools.apidocs.tools.core.model.parameters.Parameter;
import com.wavemaker.tools.apidocs.tools.core.model.properties.Property;

/**
 * Created by sunilp on 29/5/15.
 */
public class ParameterHandler {

    private final Parameter parameter;

    private final Map<String, Model> models;

    public ParameterHandler(Parameter parameter, Map<String, Model> models) {
        this.parameter = parameter;
        this.models = models;
    }

    public String getFullyQualifiedType() {
        return ((AbstractParameter) parameter).getFullyQualifiedType();
    }

    public String getFullyQualifiedTypeArgument() {
        if (parameter instanceof BodyParameter) {
            BodyParameter bodyParameter = (BodyParameter) parameter;
            Model model = bodyParameter.getSchema();
            if (model instanceof RefModel) {
                // when body is Object or Object<Object>
                RefModel refModel = (RefModel) model;
                Model actualModel = models.get(refModel.getSimpleRef());
                if (actualModel != null) {
                    List<Model> argumentModel = refModel.getTypeArguments();
                    if (argumentModel.size() > 0) {
                        Model argModel = argumentModel.get(0);
                        if (argModel instanceof RefModel) {
                            RefModel refArgModel = (RefModel) argModel;
                            Model actualArgModel = models.get(refArgModel.getSimpleRef());
                            if (actualArgModel != null) {
                                return ((AbstractModel) actualArgModel).getFullyQualifiedName();
                            }
                        }
                    }
                    return null;
                }
                //when body is primitive like string
                return refModel.getSimpleRef();
            }
            if (model instanceof ArrayModel) {
                //When Body is List<String>,Set<Object>...
                ArrayModel arrayModel = (ArrayModel) model;
                if (!arrayModel.isList()) {
                    return null;
                }
                Property property = arrayModel.getItems();
                PropertyHandler propertyHandler = new PropertyHandler(property, models);
                if (propertyHandler.isPrimitive()) {
                    return property.getType();
                } else {
                    return propertyHandler.getFullyQualifiedType();
                }
            }
        }
        return null;
    }

    public boolean isList() {
        if (parameter instanceof BodyParameter) {
            BodyParameter bodyParameter = (BodyParameter) parameter;
            Model model = bodyParameter.getSchema();
            if (model instanceof ArrayModel) {
                ArrayModel arrayModel = (ArrayModel) model;
                if (arrayModel.isList()) {
                    return true;
                }
                return false;
            }
        }
        return false;
    }

}

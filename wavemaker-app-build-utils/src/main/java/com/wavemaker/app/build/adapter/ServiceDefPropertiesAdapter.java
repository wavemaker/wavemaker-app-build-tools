package com.wavemaker.app.build.adapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.wavemaker.app.build.swaggerdoc.handler.ModelHandler;
import com.wavemaker.app.build.swaggerdoc.handler.PropertyHandler;
import com.wavemaker.tools.apidocs.tools.core.model.*;
import com.wavemaker.tools.apidocs.tools.core.model.parameters.BodyParameter;
import com.wavemaker.tools.apidocs.tools.core.model.parameters.Parameter;
import com.wavemaker.tools.apidocs.tools.core.model.properties.ArrayProperty;
import com.wavemaker.tools.apidocs.tools.core.model.properties.Property;
import com.wavemaker.tools.apidocs.tools.core.model.properties.RefProperty;

/**
 * @author <a href="mailto:sunil.pulugula@wavemaker.com">Sunil Kumar</a>
 * @since 30/1/17
 */
public class ServiceDefPropertiesAdapter {

    public static final String FILE = "file";
    public static final String ARRAY = "array";
    public static final int MODEL_PROPERTIES_LEVEL = 1;

    public List<String> adaptToRequiredFields(final Swagger swagger, final Parameter parameter) {
        final Map<String, Model> definitions = swagger.getDefinitions();
        if (parameter instanceof BodyParameter) {
            BodyParameter bodyParameter = (BodyParameter) parameter;
            Model model = bodyParameter.getSchema();
            if (model instanceof RefModel) {
                // when body is Object or Object<Object>
                RefModel refModel = (RefModel) model;
                ModelImpl actualModel = ( ModelImpl)definitions.get(refModel.getSimpleRef());
                if (actualModel != null) {
                    List<Model> argumentModel = refModel.getTypeArguments();
                    if (argumentModel.size() > 0) {
                        Model argModel = argumentModel.get(0);
                        if (argModel instanceof RefModel) {
                            RefModel refArgModel = (RefModel) argModel;
                            ModelImpl actualArgModel = (ModelImpl) definitions.get(refArgModel.getSimpleRef());
                            if (actualArgModel != null) {
                                return generateFields(actualArgModel, definitions);
                            }
                        }
                    }else {
                        return generateFields(actualModel, definitions);
                    }
                    return null;
                }
                //when body is primitive like string
                return null;
            }
            if (model instanceof ArrayModel) {
                //When Body is List<String>,Set<Object>...
                ArrayModel arrayModel = (ArrayModel) model;
                if (!arrayModel.isList()) {
                    return null;
                }
                Property property = arrayModel.getItems();
                PropertyHandler propertyHandler = new PropertyHandler(property, swagger.getDefinitions());
                if (propertyHandler.isPrimitive()) {
                    return null;
                } else {
                    if (property instanceof RefProperty) {
                        String refName = ((RefProperty) property).getSimpleRef();
                        final ModelImpl refModel = (ModelImpl)definitions.get(refName);
                        return generateFields(refModel, definitions);
                    }
                    if (property instanceof ArrayProperty) {
                        //FIXME check if this case exist i,e List<List<Integer>> or Array[List<Object>]..
                        return null;
                    }
                }
            }
        }
        return null;

    }

    private List<String> generateFields(final ModelImpl model, final Map<String, Model> models) {
        ModelHandler modelHandler = new ModelHandler(model, models);
        final Map<String,Property> propertiesMap = modelHandler.listProperties(model, MODEL_PROPERTIES_LEVEL);
        List<String> fields = new ArrayList<>();
        for (Map.Entry<String, Property> entry : propertiesMap.entrySet()) {
            if (entry.getValue().getRequired()) {
                fields.add(entry.getKey());
            }
        }
        return fields;
    }


}

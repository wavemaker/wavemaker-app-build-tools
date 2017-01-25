/**
 * Copyright (C) 2015 WaveMaker, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.wavemaker.studio.app.build.swaggerdoc;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import com.wavemaker.commons.WMRuntimeException;
import com.wavemaker.commons.classloader.ResourceClassLoaderUtils;
import com.wavemaker.commons.classloader.WMCallable;
import com.wavemaker.tools.apidocs.tools.core.model.Info;
import com.wavemaker.tools.apidocs.tools.core.model.Swagger;
import com.wavemaker.tools.apidocs.tools.parser.config.SwaggerConfiguration;
import com.wavemaker.tools.apidocs.tools.parser.runner.SwaggerParser;
import com.wavemaker.tools.apidocs.tools.parser.scanner.FilterableClassScanner;
import com.wavemaker.tools.apidocs.tools.parser.scanner.FilterableModelScanner;
import com.wavemaker.tools.apidocs.tools.spring.SpringSwaggerParser;
import com.wavemaker.tools.apidocs.tools.spring.resolver.MultiPartFileResolver;
import com.wavemaker.tools.apidocs.tools.spring.resolver.MultiPartRequestResolver;
import com.wavemaker.tools.apidocs.tools.spring.resolver.PageParameterResolver;
import com.wavemaker.tools.apidocs.tools.spring.resolver.ServletMetaTypesResolver;

/**
 * Created by saddhamp on 18/4/16.
 */
public class SwaggerGenerator {
    private String basePackage;
    private Info swaggerInfo;
    private ClassLoader classLoader;

    public SwaggerGenerator(String basePackage){
        if(StringUtils.isBlank(basePackage))
            throw new WMRuntimeException("Base package is null or empty");
        this.basePackage = basePackage;
    }

    public SwaggerGenerator setSwaggerInfo(Info swaggerInfo) {
        this.swaggerInfo = swaggerInfo;
        return this;
    }

    public SwaggerGenerator setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
        return this;
    }

    public Swagger generate(){
        Swagger swagger = null;

        FilterableClassScanner classScanner = new FilterableClassScanner();
        classScanner.includePackage(basePackage);
        FilterableModelScanner modelScanner = new FilterableModelScanner();
        modelScanner.excludePackage("javax.servlet.http");

        final SwaggerConfiguration.Builder builder = new SwaggerConfiguration.Builder("", classScanner);
        builder.setModelScanner(modelScanner);

        if(classLoader != null) {
            builder.setClassLoader(classLoader);
        } else{
            builder.setClassLoader(Thread.currentThread().getContextClassLoader());
        }

        if(swaggerInfo != null) {
            builder.setInfo(swaggerInfo);
        } else {
            builder.setInfo(new Info());
        }

        swagger = ResourceClassLoaderUtils.runInClassLoaderContext(new WMCallable<Swagger>() {
            @Override
            public Swagger call() {
                builder.addParameterResolver(Pageable.class, new PageParameterResolver());
                builder.addParameterResolver(MultipartFile.class, new MultiPartFileResolver());
                builder.addParameterResolver(MultipartHttpServletRequest.class, new MultiPartRequestResolver());
                builder.addParameterResolver(HttpServletRequest.class, new ServletMetaTypesResolver());
                builder.addParameterResolver(HttpServletResponse.class, new ServletMetaTypesResolver());
                SwaggerParser swaggerParser = new SpringSwaggerParser(builder.build());
                return swaggerParser.generate();
            }
        }, classLoader);

        return swagger;
    }
}

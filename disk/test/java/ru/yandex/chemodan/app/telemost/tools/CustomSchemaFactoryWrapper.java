package ru.yandex.chemodan.app.telemost.tools;

/*
MIT License

Copyright (c) 2019 Rohit Mistry

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/


import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonObjectFormatVisitor;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import com.fasterxml.jackson.module.jsonSchema.factories.FormatVisitorFactory;
import com.fasterxml.jackson.module.jsonSchema.factories.JsonSchemaFactory;
import com.fasterxml.jackson.module.jsonSchema.factories.ObjectVisitor;
import com.fasterxml.jackson.module.jsonSchema.factories.SchemaFactoryWrapper;
import com.fasterxml.jackson.module.jsonSchema.factories.VisitorContext;
import com.fasterxml.jackson.module.jsonSchema.factories.WrapperFactory;
import com.fasterxml.jackson.module.jsonSchema.types.AnySchema;
import com.fasterxml.jackson.module.jsonSchema.types.ContainerTypeSchema;
import com.fasterxml.jackson.module.jsonSchema.types.ObjectSchema;
import com.fasterxml.jackson.module.jsonSchema.types.ReferenceSchema;



public class CustomSchemaFactoryWrapper extends SchemaFactoryWrapper {
    public CustomSchemaFactoryWrapper() {
        this(null);
    }

    public CustomSchemaFactoryWrapper(SerializerProvider p) {
        super(p);
        this.schemaProvider = new CustomJsonSchemaFactory();
        this.visitorFactory = new CustomFormatVisitorFactory(this);
        this.globalDefinitionClasses = new LinkedHashMap<String, JsonSchema>();
    }

    public Map<String, JsonSchema> globalDefinitionClasses;

    public class CustomObjectSchema extends ObjectSchema {
        @JsonProperty
        public Map<String, JsonSchema> definitions;
    }

    public class CustomAnySchema extends AnySchema {
        @JsonProperty
        public Map<String, JsonSchema> definitions;
    }

    public class CustomJsonSchemaFactory extends JsonSchemaFactory {
        @Override
        public AnySchema anySchema() {
            return new CustomAnySchema();
        }
        @Override
        public ObjectSchema objectSchema() {
            return new CustomObjectSchema();
        }
    }

    public static CustomSchemaFactoryWrapper getNewWrapper(CustomSchemaFactoryWrapper visitor) {
        CustomSchemaFactoryWrapper wrapper = new CustomSchemaFactoryWrapper();
        wrapper.globalDefinitionClasses = visitor.globalDefinitionClasses;
        return wrapper;
    }

    public class CustomWrapperFactory extends WrapperFactory {
        private CustomSchemaFactoryWrapper schemaFactory;

        public CustomWrapperFactory(CustomSchemaFactoryWrapper sf) {
            schemaFactory = sf;
        }

        @Override
        public SchemaFactoryWrapper getWrapper(SerializerProvider provider) {
            CustomSchemaFactoryWrapper wrapper = getNewWrapper(schemaFactory);
            wrapper.setProvider(provider);
            return wrapper;
        }

        @Override
        public SchemaFactoryWrapper getWrapper(SerializerProvider provider, VisitorContext rvc) {
            CustomSchemaFactoryWrapper wrapper = getNewWrapper(schemaFactory);
            wrapper.setProvider(provider);
            wrapper.setVisitorContext(rvc);
            return wrapper;
        }
    }

    public static List<Class<?>> getAllJsonSubTypeClasses (Class<?> classToTest) {
        JsonSubTypes[] subTypesAnnotations = classToTest.getAnnotationsByType(JsonSubTypes.class);
        List<Class<?>> output = new LinkedList<Class<?>>();
        for (JsonSubTypes subTypeAnnotation : subTypesAnnotations) {
            for (JsonSubTypes.Type subType : subTypeAnnotation.value()) {
                output.add(subType.value());
            }
        }
        return output;
    }

    public static String javaTypeToUrn(Class<?> classObj) {
        return "urn:jsonschema:" + classObj.getCanonicalName().replace('.', ':').replace('$', ':');
    }

    private class CustomObjectVisitor extends ObjectVisitor {

        private CustomSchemaFactoryWrapper schemaFactory;

        public CustomObjectVisitor(SerializerProvider provider,
                                   ObjectSchema schema,
                                   WrapperFactory wrapperFactory,
                                   CustomSchemaFactoryWrapper sf) {
            super(provider, schema, wrapperFactory);
            schemaFactory = sf;
        }

        @Override
        public void property(BeanProperty prop) throws JsonMappingException {
            JavaType type = prop.getType();
            if (isModel(type)) {
                ObjectSchema propSchema = new ObjectSchema();
                Class<?> rawClass = type.getRawClass();
                String refId = CustomSchemaFactoryWrapper.javaTypeToUrn(rawClass);

                Set<Object> inheritingClasses = new HashSet<Object>();

                for (Class<?> subClass : getAllJsonSubTypeClasses(rawClass)) {
                    inheritingClasses.add(new ReferenceSchema(javaTypeToUrn(subClass)));
                    SchemaFactoryWrapper visitor = getNewWrapper(schemaFactory);
                    try {
                        JsonSchema subClassSchema = GenerateSchemas.generateSchemaFromJavaClass(visitor, subClass);
                        if (subClassSchema instanceof ObjectSchema) {
                            ((ObjectSchema) subClassSchema).rejectAdditionalProperties();
                        }
                        schemaFactory.globalDefinitionClasses.put(javaTypeToUrn(subClass), subClassSchema);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        throw JsonMappingException.from(visitor.getProvider(), "Exception occured", ex);
                    }
                }

                if (propSchema instanceof ContainerTypeSchema
                        &&  inheritingClasses.size() > 0) {
                    ((ContainerTypeSchema)propSchema).setOneOf(inheritingClasses);
                } else {
                    propSchema.set$ref(refId);
                    SchemaFactoryWrapper visitor = getNewWrapper(schemaFactory);
                    try {
                        JsonSchema subClassSchema = GenerateSchemas.generateSchemaFromJavaClass(visitor, rawClass);
                        if (subClassSchema.getId() == null) {
                            subClassSchema.setId(javaTypeToUrn(type.getRawClass()));
                        }
                        schemaFactory.globalDefinitionClasses.put(refId, subClassSchema);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        throw JsonMappingException.from(visitor.getProvider(), "Exception occured", ex);
                    }
                }

                schema.putProperty(prop.getName(), propSchema);
                return;
            }

            schema.putOptionalProperty(prop.getName(), propertySchema(prop));
        }
    }

    public static boolean isModel(JavaType type) {
        return type.getRawClass() != String.class
                && !isBoxedPrimitive(type.getRawClass())
                && !type.isPrimitive()
                && !type.isMapLikeType()
                && !type.isCollectionLikeType()
                && !type.isEnumType();
    }

    public static boolean isBoxedPrimitive(Class<?> type) {
        return type == Boolean.class
                || type == Byte.class
                || type == Long.class
                || type == Integer.class
                || type == Short.class
                || type == Float.class
                || type == Double.class;
    }

    public class CustomFormatVisitorFactory extends FormatVisitorFactory {
        private CustomSchemaFactoryWrapper schemaFactory;

        public CustomFormatVisitorFactory(CustomSchemaFactoryWrapper schemaFactoryWrapper) {
            super(new CustomWrapperFactory(schemaFactoryWrapper));
            this.schemaFactory = schemaFactoryWrapper;
        }

        @Override
        public JsonObjectFormatVisitor objectFormatVisitor(SerializerProvider provider, ObjectSchema objectSchema, VisitorContext rvc) {
            CustomObjectVisitor v = new CustomObjectVisitor(provider,
                    objectSchema,
                    new CustomWrapperFactory(schemaFactory),
                    schemaFactory);
            v.setVisitorContext(rvc);
            return v;
        }
    }
}

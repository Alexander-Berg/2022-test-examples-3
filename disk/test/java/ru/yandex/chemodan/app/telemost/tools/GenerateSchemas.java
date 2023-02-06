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


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import com.fasterxml.jackson.module.jsonSchema.factories.SchemaFactoryWrapper;


/**
 * @author rmistry
 */
public class GenerateSchemas {

    public static JsonSchema generateSchemaFromJavaClass(SchemaFactoryWrapper visitor, ObjectMapper mapper,
                                                         Class<?> classToGenerate) throws JsonMappingException {

        mapper.acceptJsonFormatVisitor(mapper.constructType(classToGenerate), visitor);

        JsonSchema schema = visitor.finalSchema();

        return schema;
    }

    public static JsonSchema generateSchemaFromJavaClass(SchemaFactoryWrapper visitor, Class<?> classToGenerate)
            throws JsonMappingException {
        ObjectMapper mapper = new ObjectMapper();

        return generateSchemaFromJavaClass(visitor, mapper, classToGenerate);
    }

    public static JsonSchema generateSchemaFromJavaClass(Class<?> classToGenerate)
            throws JsonMappingException, JsonProcessingException {

        CustomSchemaFactoryWrapper visitor = new CustomSchemaFactoryWrapper();

        JsonSchema schema = generateSchemaFromJavaClass(visitor, classToGenerate);

        // Need to clone global list of dependencies for this schema
        if (schema instanceof CustomSchemaFactoryWrapper.CustomAnySchema) {
            ((CustomSchemaFactoryWrapper.CustomAnySchema) schema).definitions = visitor.globalDefinitionClasses;
        } else if (schema instanceof CustomSchemaFactoryWrapper.CustomObjectSchema) {
            ((CustomSchemaFactoryWrapper.CustomObjectSchema) schema).definitions = visitor.globalDefinitionClasses;
        }

        return schema;
    }
}

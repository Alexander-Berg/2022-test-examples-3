/*
 * Copyright (C) 2015 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package toolkit.converter_factory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import org.jetbrains.annotations.NotNull;
import retrofit2.Converter;
import retrofit2.Retrofit;

/**
 * A {@linkplain Converter.Factory converter} which uses Jackson.
 *
 * <p>Because Jackson is so flexible in the types it supports, this converter assumes that it can
 * handle all types. If you are mixing JSON serialization with something else (such as protocol
 * buffers), you must {@linkplain Retrofit.Builder#addConverterFactory(Converter.Factory) add this
 * instance} last to allow the other converters a chance to see their types.
 */
public final class ConverterFactory extends Converter.Factory {
    private final ObjectMapper mapper;

    private ConverterFactory(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * Create an instance using {@code mapper} for conversion.
     */
    @SuppressWarnings("ConstantConditions") // Guarding public API nullability.
    public static ConverterFactory create(ObjectMapper mapper) {
        if (mapper == null) {
            throw new NullPointerException("mapper == null");
        }
        return new ConverterFactory(mapper);
    }

    @Override
    public Converter<ResponseBody, ?> responseBodyConverter(
            @NotNull Type type, Annotation @NotNull [] annotations, @NotNull Retrofit retrofit) {
        JavaType javaType = mapper.getTypeFactory().constructType(type);
        ObjectReader reader = mapper.readerFor(javaType);
        return new JacksonResponseBodyConverter<>(reader);
    }

    @Override
    public Converter<?, RequestBody> requestBodyConverter(
            @NotNull Type type,
            Annotation @NotNull [] parameterAnnotations,
            Annotation @NotNull [] methodAnnotations,
            @NotNull Retrofit retrofit) {
        JavaType javaType = mapper.getTypeFactory().constructType(type);
        ObjectWriter writer = mapper.writerFor(javaType);
        return new JacksonRequestBodyConverter<>(writer);
    }
}

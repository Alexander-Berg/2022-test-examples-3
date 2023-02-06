package ru.yandex.market.logistics.lom.utils;

import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import ru.yandex.market.common.mds.s3.client.content.ContentConsumer;
import ru.yandex.market.common.mds.s3.client.content.consumer.StreamCopyContentConsumer;
import ru.yandex.market.common.mds.s3.client.model.ResourceLocation;
import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client;
import ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static ru.yandex.market.logistics.test.integration.utils.QueryParamUtils.toParams;

public final class TestUtils {
    public static final String INCORRECT_EMAIL_ERROR_MESSAGE = "must be a well-formed email address";
    public static final String NOT_NULL_ERROR_MESSAGE = "must not be null";
    public static final String NOT_BLANK_ERROR_MESSAGE = "must not be blank";
    public static final String OPTIONAL_NOT_BLANK_ERROR_MESSAGE = "field value must be not blank OR null";
    public static final String POSITIVE_ERROR_MESSAGE = "must be greater than or equal to 0";
    public static final String NOT_EMPTY_ERROR_MESSAGE = "must not be empty";
    public static final String ITEM_ARTICLES_UNIQUENESS_ERROR_MESSAGE = "item articles must be unique for order";
    public static final String NON_NEGATIVE_ERROR_MESSAGE = "must be greater than 0";

    private TestUtils() {
        throw new UnsupportedOperationException();
    }

    public static MultiValueMap<String, String> toParamWithCollections(Object obj) {
        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        try {
            for (Field f : obj.getClass().getDeclaredFields()) {
                f.setAccessible(true);

                if (f.get(obj) instanceof Collection) {
                    List<String> values = new ArrayList<>();
                    for (Object o : (Collection) f.get(obj)) {
                        values.add(o == null ? null : o.toString());
                    }
                    parameters.put(f.getName(), values);
                } else {
                    parameters
                        .put(f.getName(), Collections.singletonList(f.get(obj) == null ? null : f.get(obj).toString()));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return parameters;
    }

    @Nonnull
    public static HttpHeaders toHttpHeaders(Map<String, ? extends Collection<String>> paramMap) {
        return new HttpHeaders(toParams(paramMap));
    }

    @Nonnull
    public static ResultMatcher validationErrorsJsonContent(String field, String message) {
        return content().json(
            "{\"message\":\"Following validation errors occurred:\\n" +
                "Field: '" + field + "', message: '" + message + "'\"}",
            true
        );
    }

    @Nonnull
    public static ResultMatcher fieldValidationErrorMatcher(String fieldName, String message) {
        return jsonPath("message").value(fieldErrorMessage(fieldName, message));
    }

    @Nonnull
    public static String fieldErrorMessage(String fieldName, String message) {
        return String.format(
            "Following validation errors occurred:\nField: '%s', message: '%s'",
            fieldName,
            message
        );
    }

    @Nonnull
    public static ResultMatcher objectValidationErrorMatcher(String fieldName, String message) {
        return jsonPath("message").value(String.format(
            "Following validation errors occurred:\nObject: '%s', message: '%s'",
            fieldName,
            message
        ));
    }

    @Nonnull
    public static ResultMatcher propertyValidationErrorMatcher(String fieldName, String message) {
        return jsonPath("message").value(String.format(
            "[FieldError(propertyPath=%s, message=%s)]",
            fieldName,
            message
        ));
    }

    public static void mockMdsS3ClientDownload(MdsS3Client mdsS3Client, String filepath) {
        when(mdsS3Client.download(any(ResourceLocation.class), any(ContentConsumer.class))).thenAnswer(
            invocation -> invocation.<StreamCopyContentConsumer<OutputStream>>getArgument(1)
                .consume(IntegrationTestUtils.inputStreamFromResource(filepath))
        );
    }
}

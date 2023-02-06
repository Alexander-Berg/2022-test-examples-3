package ru.yandex.market.api.partner.controllers.order;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Objects;

import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.web.client.RequestMatcher;

/**
 * Вспомогательные методы для тестов.
 */
public interface ResourceUtilitiesMixin {

    default String resourceAsString(String path) {
        try {
            return IOUtils.toString(
                    Objects.requireNonNull(getClass().getResourceAsStream(path), path),
                    StandardCharsets.UTF_8
            );
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    default RequestMatcher json(Path expectedBodyPath) {
        return json(resourceAsString(expectedBodyPath.toString()));
    }

    default RequestMatcher json(String json) {
        return request -> {
            try {
                JSONAssert.assertEquals(
                        json,
                        ((MockClientHttpRequest) request).getBodyAsString(),
                        false
                );
            } catch (JSONException e) {
                throw new AssertionError(e);
            }
        };
    }

}

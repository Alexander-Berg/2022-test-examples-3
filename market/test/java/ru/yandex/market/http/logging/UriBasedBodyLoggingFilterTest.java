package ru.yandex.market.http.logging;

import java.net.URI;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("Тесты фильтра тел outbound запросов/ответов")
public class UriBasedBodyLoggingFilterTest extends AbstractTest {

    @ParameterizedTest
    @MethodSource("filters")
    @DisplayName("Фильтр работает корректно")
    void testFilter(
        UriBasedBodyLoggingFilter filter,
        URI uri,
        boolean expectedShouldSkipRequest,
        boolean expectedShouldSkipResponse
    ) {
        softly.assertThat(filter.shouldSkipRequestBody(uri))
            .as("Asserting should skip request value")
            .isEqualTo(expectedShouldSkipRequest);

        softly.assertThat(filter.shouldSkipResponseBody(uri))
            .as("Asserting should skip response value")
            .isEqualTo(expectedShouldSkipResponse);
    }

    private static Stream<Arguments> filters() {
        return Stream.of(
            Arguments.of(
                UriBasedBodyLoggingFilter.create("delivery_city")
                    .setSkipRequests(false)
                    .setSkipResponses(false),
                URI.create("http://localhost:8080/delivery_city?kladr=300000000"),
                false,
                false
            ),
            Arguments.of(
                UriBasedBodyLoggingFilter.create("delivery_city")
                    .setSkipRequests(true)
                    .setSkipResponses(false),
                URI.create("http://localhost:8080/delivery_city?kladr=300000000"),
                true,
                false
            ),
            Arguments.of(
                UriBasedBodyLoggingFilter.create("delivery_city")
                    .setSkipRequests(false)
                    .setSkipResponses(true),
                URI.create("http://localhost:8080/delivery_city?kladr=300000000"),
                false,
                true
            ),
            Arguments.of(
                UriBasedBodyLoggingFilter.create("delivery_city")
                    .setSkipRequests(true)
                    .setSkipResponses(true),
                URI.create("http://localhost:8080/delivery_city?kladr=300000000"),
                true,
                true
            ),
            Arguments.of(
                UriBasedBodyLoggingFilter.create("delivery_city")
                    .setSkipRequests(true)
                    .setSkipResponses(true),
                URI.create("http://localhost:8080/another_request?kladr=300000000"),
                false,
                false
            )
        );
    }
}

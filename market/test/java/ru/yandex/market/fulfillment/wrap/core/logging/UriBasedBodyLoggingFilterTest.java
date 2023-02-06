package ru.yandex.market.fulfillment.wrap.core.logging;

import java.net.URI;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.test.integration.SoftAssertionSupport;

class UriBasedBodyLoggingFilterTest extends SoftAssertionSupport {

    private static Stream<Arguments> data() throws Exception {
        return Stream.of(
            Arguments.of(
                UriBasedBodyLoggingFilter.create("delivery_city",
                    false,
                    false
                ),
                new URI("http://localhost:8080/delivery_city?kladr=300000000"),
                false,
                false
            ),
            Arguments.of(
                UriBasedBodyLoggingFilter.create("delivery_city",
                    true,
                    false
                ),
                new URI("http://localhost:8080/delivery_city?kladr=300000000"),
                true,
                false
            ),
            Arguments.of(
                UriBasedBodyLoggingFilter.create("delivery_city",
                    false,
                    true
                ),
                new URI("http://localhost:8080/delivery_city?kladr=300000000"),
                false,
                true
            ),
            Arguments.of(
                UriBasedBodyLoggingFilter.create("delivery_city",
                    true,
                    true
                ),
                new URI("http://localhost:8080/delivery_city?kladr=300000000"),
                true,
                true
            ),
            Arguments.of(
                UriBasedBodyLoggingFilter.create("delivery_city",
                    true,
                    true
                ),
                new URI("http://localhost:8080/another_request?kladr=300000000"),
                false,
                false
            )
        );
    }

    @MethodSource("data")
    @ParameterizedTest
    void testFilter(
        UriBasedBodyLoggingFilter filter,
        URI uri,
        boolean expectedShouldSkipRequest,
        boolean expectedShouldSkipResponse
    ) {
        boolean actualShouldSkipRequest = filter.shouldSkipRequestBody(uri);
        boolean actualShouldSKipResponse = filter.shouldSkipResponseBody(uri);

        softly.assertThat(actualShouldSkipRequest)
            .as("Asserting should skip request value")
            .isEqualTo(expectedShouldSkipRequest);

        softly.assertThat(actualShouldSKipResponse)
            .as("Asserting should skip response value")
            .isEqualTo(expectedShouldSkipResponse);
    }
}

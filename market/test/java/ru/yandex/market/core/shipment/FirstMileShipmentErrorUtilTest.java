package ru.yandex.market.core.shipment;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import com.google.common.base.Preconditions;
import org.apache.commons.io.IOUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.nesu.client.model.error.ClientError;
import ru.yandex.market.logistics.nesu.client.model.error.PartnerShipmentConfirmationError;
import ru.yandex.market.logistics.nesu.client.model.error.ResourceNotFoundError;
import ru.yandex.market.logistics.nesu.client.model.error.ValidationError;
import ru.yandex.market.logistics.util.client.exception.HttpTemplateException;

/**
 * Тесты для {@link FirstMileShipmentErrorUtil}.
 */
class FirstMileShipmentErrorUtilTest {

    @ParameterizedTest(name = "testConversionType: {0}")
    @MethodSource("testConversionTypeData")
    void testConversionType(final String inputJson, final Class<?> expectedClass) {
        final ClientError clientError = extractError(inputJson);
        Assertions.assertThat(clientError).isExactlyInstanceOf(expectedClass);
    }

    private static Stream<Arguments> testConversionTypeData() {
        return Stream.of(
                Arguments.of("SHIPMENT_PARTNER_SHIPMENT_CONFIRMATION_VALIDATION.json", PartnerShipmentConfirmationError.class),
                Arguments.of("SHIPMENT_RESOURCE_NOT_FOUND.json", ResourceNotFoundError.class),
                Arguments.of("SHIPMENT_VALIDATION_ERROR.json", ValidationError.class)
        );
    }

    private static ClientError extractError(final String fileName) {
        try (final InputStream resource = FirstMileShipmentErrorUtilTest.class.getResourceAsStream(fileName)) {
            final String body = IOUtils.toString(
                    Preconditions.checkNotNull(resource, "File not found: " + resource),
                    StandardCharsets.UTF_8
            );

            return extractClientError(body);
        } catch (final IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private static ClientError extractClientError(final String body) {
        final HttpTemplateException exception = new HttpTemplateException(/* неважно */ 0, body);
        return FirstMileShipmentErrorUtil.extractClientError(exception);
    }

}

package ru.yandex.market.logistics.lom.converter.combinator;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.google.protobuf.util.JsonFormat;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import yandex.market.combinator.common.Common;
import yandex.market.combinator.v0.CombinatorOuterClass;

import ru.yandex.market.logistics.lom.AbstractTest;
import ru.yandex.market.logistics.lom.model.dto.CombinatorRoute;
import ru.yandex.market.logistics.lom.model.enums.PaymentMethod;

import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;

class CombinatorConverterTest extends AbstractTest {

    private final CombinatorConverter converter = new CombinatorConverter();

    @SneakyThrows
    private static CombinatorOuterClass.DeliveryRoute createExpectedRoute() {
        CombinatorOuterClass.DeliveryRoute.Builder builder = CombinatorOuterClass.DeliveryRoute.newBuilder();
        JsonFormat.parser().ignoringUnknownFields().merge(
            extractFileContent("converter/combinator/after/expected_route.json"),
            builder
        );
        return builder.build();
    }

    @SneakyThrows
    @DisplayName("Успешная конвертация из CombinatorRoute в CombinatorOuterClass.DeliveryRoute")
    @Test
    void successRouteConvert() {
        CombinatorRoute combinatorRoute = objectMapper
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .readValue(
                extractFileContent("converter/combinator/before/combinator_route.json"),
                CombinatorRoute.class
            );

        CombinatorOuterClass.DeliveryRoute deliveryRoute = converter.convertDeliveryRoute(combinatorRoute);
        CombinatorOuterClass.DeliveryRoute expectedRoute = createExpectedRoute();
        softly.assertThat(deliveryRoute).isEqualTo(expectedRoute);
    }

    @ParameterizedTest
    @EnumSource(PaymentMethod.class)
    @DisplayName("Конвертация PaymentMethod model->combinator")
    void paymentMethodToCombinator(PaymentMethod paymentMethod) {
        softly.assertThat(converter.convertPaymentMethod(paymentMethod))
            .isNotNull()
            .isNotEqualTo(Common.PaymentMethod.UNKNOWN);
    }
}

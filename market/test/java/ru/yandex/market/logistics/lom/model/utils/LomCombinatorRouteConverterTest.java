package ru.yandex.market.logistics.lom.model.utils;

import java.io.IOException;

import org.json.JSONException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils;

@DisplayName("Конвертация proto-модели в подходящую LOM")
class LomCombinatorRouteConverterTest extends AbstractTest {

    private final LomCombinatorRouteConverter routeConverter = LomCombinatorConversionFactory.routeConverter();

    @Test
    @DisplayName("Успешное преобразование в строку")
    void success() throws IOException, JSONException {
        IntegrationTestUtils.assertJson(
            "route/ff_sc_courier.json",
            routeConverter.toLomRouteString(CombinatorFactory.deliveryRoute())
        );
    }
}

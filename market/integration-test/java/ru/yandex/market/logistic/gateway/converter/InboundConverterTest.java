package ru.yandex.market.logistic.gateway.converter;

import java.util.Optional;

import org.junit.Test;

import ru.yandex.market.logistic.api.model.common.Inbound;
import ru.yandex.market.logistic.api.model.common.InboundType;
import ru.yandex.market.logistic.api.model.common.ResourceId;
import ru.yandex.market.logistic.api.utils.DateTimeInterval;
import ru.yandex.market.logistic.gateway.BaseTest;
import ru.yandex.market.logistic.gateway.service.converter.common.InboundConverter;

public class InboundConverterTest extends BaseTest {

    @Test
    public void convertTest() {
        String dateTimeInterval = "2019-08-07/2019-08-10";

        Inbound inboundApi = Inbound.builder(ResourceId.builder().build(),
                InboundType.INVENTARIZATION, DateTimeInterval.fromFormattedValue(dateTimeInterval))
                .build();

        ru.yandex.market.logistic.gateway.common.model.common.Inbound inbound =
                ru.yandex.market.logistic.gateway.common.model.common.Inbound.builder(
                        ru.yandex.market.logistic.gateway.common.model.common.ResourceId.builder().build(),
                        ru.yandex.market.logistic.gateway.common.model.common.InboundType.INVENTARIZATION,
                        ru.yandex.market.logistic.gateway.common.model.common.
                                DateTimeInterval.fromFormattedValue(dateTimeInterval))
                        .build();

        assertions.assertThat(Optional.of(inbound))
                .as("Inbound should be equal to the converted inbound from api")
                .isEqualTo(InboundConverter.convertFromApi(inboundApi));
        assertions.assertThat(Optional.of(inboundApi))
                .as("Inbound from api should be equal to the converted inbound")
                .isEqualTo(InboundConverter.convertToApi(inbound));
    }

}

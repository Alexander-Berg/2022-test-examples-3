package ru.yandex.market.logistic.gateway.converter;

import java.util.Optional;

import org.junit.Test;

import ru.yandex.market.logistic.api.model.common.Outbound;
import ru.yandex.market.logistic.api.model.common.OutboundType;
import ru.yandex.market.logistic.api.model.common.ResourceId;
import ru.yandex.market.logistic.api.utils.DateTimeInterval;
import ru.yandex.market.logistic.gateway.BaseTest;
import ru.yandex.market.logistic.gateway.service.converter.common.OutboundConverter;

public class OutboundConverterTest extends BaseTest {

    @Test
    public void convertTest() {
        String dateTimeInterval = "2019-08-07/2019-08-10";

        ru.yandex.market.logistic.api.model.common.Outbound outboundApi =
                Outbound.builder(ResourceId.builder().build(),
                        DateTimeInterval.fromFormattedValue(dateTimeInterval))
                        .setOutboundType(OutboundType.FIX_LOST_INVENTARIZATION)
                        .build();

        ru.yandex.market.logistic.gateway.common.model.common.Outbound outbound =
                ru.yandex.market.logistic.gateway.common.model.common.Outbound.builder(
                        ru.yandex.market.logistic.gateway.common.model.common.ResourceId.builder().build(),
                        ru.yandex.market.logistic.gateway.common.model.common.
                                DateTimeInterval.fromFormattedValue(dateTimeInterval))
                        .setOutboundType(ru.yandex.market.logistic.gateway.common.model.common.
                                OutboundType.FIX_LOST_INVENTARIZATION)
                        .build();

        assertions.assertThat(Optional.of(outbound))
                .as("Outbound should be equal to the converted outbound from api")
                .isEqualTo(OutboundConverter.convertFromApi(outboundApi));
        assertions.assertThat(Optional.of(outboundApi))
                .as("Outbound from api should be equal to the converted outbound")
                .isEqualTo(OutboundConverter.convertToApi(outbound));
    }

}

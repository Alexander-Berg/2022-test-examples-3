package ru.yandex.market.wrap.infor.service.inbound.converter;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logistic.api.model.fulfillment.RemainingLifetimes;
import ru.yandex.market.logistic.api.model.fulfillment.ShelfLife;
import ru.yandex.market.logistic.api.model.fulfillment.ShelfLives;
import ru.yandex.market.logistics.test.integration.SoftAssertionSupport;
import ru.yandex.market.wrap.infor.service.inbound.converter.meta.RemainingLifetimesMeta;

class RemainingLifetimesConverterTest extends SoftAssertionSupport {

    @Test
    void shouldReturnNullForAllFieldsIfRemainingLifetimesIsNull() {
        RemainingLifetimesMeta result = RemainingLifetimesConverter.fromRemainingLifetimes(null);

        softly.assertThat(result).isNotNull();
        softly.assertThat(result.getInboundDays()).isNull();
        softly.assertThat(result.getOutboundDays()).isNull();
        softly.assertThat(result.getInboundPercentage()).isNull();
        softly.assertThat(result.getOutboundPercentage()).isNull();
    }

    @Test
    void shouldConvertSafetyNullableFields() {
        RemainingLifetimes lifetimes = new RemainingLifetimes(
            new ShelfLives(new ShelfLife(null), new ShelfLife(10)),
            new ShelfLives(new ShelfLife(15), new ShelfLife(null))
        );

        RemainingLifetimesMeta result = RemainingLifetimesConverter.fromRemainingLifetimes(lifetimes);

        softly.assertThat(result).isNotNull();
        softly.assertThat(result.getInboundDays()).isNull();
        softly.assertThat(result.getOutboundDays()).isEqualTo(15);
        softly.assertThat(result.getInboundPercentage()).isEqualTo(10);
        softly.assertThat(result.getOutboundPercentage()).isNull();
    }

    @Test
    void shouldConvertSuccessIfInboundDaysIsNull() {
        RemainingLifetimesMeta remainingLifetimesMeta = RemainingLifetimesMeta.newBuilder()
            .setInboundDays(null)
            .setOutboundDays(10)
            .setInboundPercentage(25)
            .setOutboundPercentage(20)
            .build();

        RemainingLifetimes result = RemainingLifetimesConverter.toRemainingLifetimes(remainingLifetimesMeta);

        softly.assertThat(result).isNotNull();
        softly.assertThat(result.getInbound().getDays()).isNull();
        softly.assertThat(result.getInbound().getPercentage().getValue()).isEqualTo(25);
        softly.assertThat(result.getOutbound().getDays().getValue()).isEqualTo(10);
        softly.assertThat(result.getOutbound().getPercentage().getValue()).isEqualTo(20);
    }
}

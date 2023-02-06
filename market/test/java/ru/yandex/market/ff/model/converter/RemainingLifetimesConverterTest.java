package ru.yandex.market.ff.model.converter;

import org.junit.jupiter.api.Test;

import ru.yandex.market.ff.model.entity.RequestItem;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.RemainingLifetimes;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

public class RemainingLifetimesConverterTest {

    @Test
    void shouldSuccessConvertRemainingLifetimes() {
        RequestItem requestItem = new RequestItem();
        requestItem.setInboundRemainingLifetimeDays(20);
        requestItem.setOutboundRemainingLifetimeDays(15);
        requestItem.setInboundRemainingLifetimePercentage(30);
        requestItem.setOutboundRemainingLifetimePercentage(30);

        RemainingLifetimes lifetimes = RemainingLifetimesConverter.toLgwRemainingLifetimes(requestItem);

        assertSoftly(s -> {
            s.assertThat(lifetimes.getInbound()).isNotNull();
            s.assertThat(lifetimes.getInbound().getDays().getValue()).isEqualTo(20);
            s.assertThat(lifetimes.getInbound().getPercentage().getValue()).isEqualTo(30);
            s.assertThat(lifetimes.getOutbound().getDays().getValue()).isEqualTo(15);
            s.assertThat(lifetimes.getOutbound().getPercentage().getValue()).isEqualTo(30);
        });
    }

    @Test
    void shouldReturnNullIfInboundRemainingLifetimesDaysAndPercentageAreNotPresent() {
        RequestItem requestItem = new RequestItem();
        requestItem.setInboundRemainingLifetimeDays(null);
        requestItem.setOutboundRemainingLifetimeDays(15);
        requestItem.setInboundRemainingLifetimePercentage(null);
        requestItem.setOutboundRemainingLifetimePercentage(30);

        RemainingLifetimes lifetimes = RemainingLifetimesConverter.toLgwRemainingLifetimes(requestItem);

        assertSoftly(s -> s.assertThat(lifetimes).isNull());
    }

    @Test
    void shouldNotConvertPairOfDaysIfInboundRemainingLifetimesDaysIsNotPresent() {
        RequestItem requestItem = new RequestItem();
        requestItem.setInboundRemainingLifetimeDays(null);
        requestItem.setOutboundRemainingLifetimeDays(15);
        requestItem.setInboundRemainingLifetimePercentage(35);
        requestItem.setOutboundRemainingLifetimePercentage(30);

        RemainingLifetimes lifetimes = RemainingLifetimesConverter.toLgwRemainingLifetimes(requestItem);

        assertSoftly(s -> {
            s.assertThat(lifetimes).isNotNull();
            s.assertThat(lifetimes.getInbound().getDays()).isNull();
            s.assertThat(lifetimes.getInbound().getPercentage().getValue()).isEqualTo(35);
            s.assertThat(lifetimes.getOutbound().getDays()).isNull();
            s.assertThat(lifetimes.getOutbound().getPercentage().getValue()).isEqualTo(30);
        });
    }

    @Test
    void shouldNotConvertPairOfPercentageIfInboundPercentageLessOutbound() {
        RequestItem requestItem = new RequestItem();
        requestItem.setInboundRemainingLifetimeDays(20);
        requestItem.setOutboundRemainingLifetimeDays(15);
        requestItem.setInboundRemainingLifetimePercentage(25);
        requestItem.setOutboundRemainingLifetimePercentage(30);

        RemainingLifetimes lifetimes = RemainingLifetimesConverter.toLgwRemainingLifetimes(requestItem);

        assertSoftly(s -> {
            s.assertThat(lifetimes.getInbound()).isNotNull();
            s.assertThat(lifetimes.getInbound().getDays().getValue()).isEqualTo(20);
            s.assertThat(lifetimes.getInbound().getPercentage()).isNull();
            s.assertThat(lifetimes.getOutbound().getDays().getValue()).isEqualTo(15);
            s.assertThat(lifetimes.getOutbound().getPercentage()).isNull();
        });
    }
}

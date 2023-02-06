package ru.yandex.market.logistics.iris.domain.converter;

import org.junit.Test;

import ru.yandex.market.logistics.iris.converter.RemainingLifetimesConverter;
import ru.yandex.market.logistics.iris.core.index.complex.RemainingLifetime;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

public class RemainingLifetimesConverterTest {

    private final RemainingLifetimesConverter converter = new RemainingLifetimesConverter();

    @Test
    public void shouldSuccessConvertRemainingLifetimes() {
        final RemainingLifetime inboundDays = createRemainingLifetime(10, 8400023);
        final RemainingLifetime outboundDays = createRemainingLifetime(15, 8400023);
        final RemainingLifetime inboundPercentage = createRemainingLifetime(20, 8400023);
        final RemainingLifetime outboundPercentage = createRemainingLifetime(25, 8400023);

        ru.yandex.market.logistic.gateway.common.model.fulfillment.RemainingLifetimes
                result = converter.toLgwRemainingLifetimes(inboundDays, inboundPercentage, outboundDays, outboundPercentage);

        assertSoftly(assertions -> {
            assertions.assertThat(result).isNotNull();

            assertions.assertThat(result.getInbound().getDays().getValue()).isEqualTo(10);
            assertions.assertThat(result.getInbound().getPercentage().getValue()).isEqualTo(20);
            assertions.assertThat(result.getOutbound().getDays().getValue()).isEqualTo(15);
            assertions.assertThat(result.getOutbound().getPercentage().getValue()).isEqualTo(25);
        });
    }

    @Test
    public void shouldSuccessConvertRemainingLifetimesIfInboundDaysValueIsNull() {
        final RemainingLifetime inboundDays = createRemainingLifetime(null, 8400023);
        final RemainingLifetime outboundDays = createRemainingLifetime(15, 8400023);
        final RemainingLifetime inboundPercentage = createRemainingLifetime(20, 8400023);
        final RemainingLifetime outboundPercentage = createRemainingLifetime(25, 8400023);

        ru.yandex.market.logistic.gateway.common.model.fulfillment.RemainingLifetimes
                result = converter.toLgwRemainingLifetimes(inboundDays, inboundPercentage, outboundDays, outboundPercentage);

        assertSoftly(assertions -> {
            assertions.assertThat(result).isNotNull();

            assertions.assertThat(result.getInbound().getDays()).isNull();
            assertions.assertThat(result.getInbound().getPercentage().getValue()).isEqualTo(20);
            assertions.assertThat(result.getOutbound().getDays().getValue()).isEqualTo(15);
            assertions.assertThat(result.getOutbound().getPercentage().getValue()).isEqualTo(25);
        });
    }

    @Test
    public void shouldReturnNullIfRemainingLifetimesAreNull() {
        final RemainingLifetime outboundDays = createRemainingLifetime(15, 8400023);
        final RemainingLifetime inboundPercentage = createRemainingLifetime(20, 8400023);

        ru.yandex.market.logistic.gateway.common.model.fulfillment.RemainingLifetimes
                result = converter.toLgwRemainingLifetimes(null, inboundPercentage, outboundDays, null);

        assertSoftly(assertions -> {
            assertions.assertThat(result).isNotNull();

            assertions.assertThat(result.getInbound().getDays()).isNull();
            assertions.assertThat(result.getInbound().getPercentage().getValue()).isEqualTo(20);
            assertions.assertThat(result.getOutbound().getDays().getValue()).isEqualTo(15);
            assertions.assertThat(result.getOutbound().getPercentage()).isNull();
        });
    }

    private RemainingLifetime createRemainingLifetime(Integer value, long updatedTimestamp) {
        return RemainingLifetime.of(value, updatedTimestamp);
    }
}

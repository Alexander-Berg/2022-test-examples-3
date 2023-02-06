package ru.yandex.market.logistics.iris.utils;

import org.junit.Test;

import ru.yandex.market.logistics.iris.core.index.complex.RemainingLifetime;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.logistics.iris.util.RemainingLifetimesValidationUtils.isValid;

public class RemainingLifetimesValidationUtilsTest {

    @Test
    public void shouldNotPassValidationIfOutboundRemainingLifetimesAreNull() {
        final RemainingLifetime inboundDays = createRemainingLifetime(15, 8400023);
        final RemainingLifetime outboundDays = RemainingLifetime.of(null, 8400023);
        final RemainingLifetime inboundPercentage = createRemainingLifetime(20, 8400023);
        final RemainingLifetime outboundPercentage = RemainingLifetime.of(null, 8400023);

        boolean resultOfValidation = isValid(inboundDays, inboundPercentage, outboundDays, outboundPercentage);

        assertThat(resultOfValidation).isEqualTo(false);
    }

    @Test
    public void shouldNotPassValidationIfOutboundRemainingLifetimesAreEmpty() {
        final RemainingLifetime inboundDays = createRemainingLifetime(15, 8400023);
        final RemainingLifetime outboundDays = RemainingLifetime.of(null, 8400023);
        final RemainingLifetime inboundPercentage = createRemainingLifetime(20, 8400023);
        final RemainingLifetime outboundPercentage = RemainingLifetime.of(null, 8400023);

        boolean resultOfValidation = isValid(inboundDays, inboundPercentage, outboundDays, outboundPercentage);

        assertThat(resultOfValidation).isEqualTo(false);
    }

    @Test
    public void shouldPassValidationIfInboundDaysValueIsNull() {
        final RemainingLifetime inboundDays = createRemainingLifetime(null, 8400023);
        final RemainingLifetime outboundDays = createRemainingLifetime(15, 8400023);
        final RemainingLifetime inboundPercentage = createRemainingLifetime(25, 8400023);
        final RemainingLifetime outboundPercentage = createRemainingLifetime(20, 8400023);

        boolean resultOfValidation = isValid(inboundDays, inboundPercentage, outboundDays, outboundPercentage);

        assertThat(resultOfValidation).isEqualTo(true);
    }

    @Test
    public void shouldPassValidationIfInboundDaysAndOutboundPercentAreNull() {
        final RemainingLifetime outboundDays = createRemainingLifetime(15, 8400023);
        final RemainingLifetime inboundPercentage = createRemainingLifetime(20, 8400023);

        boolean resultOfValidation = isValid(null, inboundPercentage, outboundDays, null);

        assertThat(resultOfValidation).isEqualTo(true);
    }

    @Test
    public void shouldNotPassValidationIfInboundDaysMoreThatOutboundDays() {
        final RemainingLifetime inboundDays = createRemainingLifetime(15, 8400023);
        final RemainingLifetime outboundDays = RemainingLifetime.of(16, 8400023);
        final RemainingLifetime inboundPercentage = createRemainingLifetime(20, 8400023);
        final RemainingLifetime outboundPercentage = RemainingLifetime.of(null, 8400023);

        boolean resultOfValidation = isValid(inboundDays, inboundPercentage, outboundDays, outboundPercentage);

        assertThat(resultOfValidation).isEqualTo(false);
    }

    @Test
    public void shouldNotPassValidationIfInboundPercentageMoreThatOutboundPercentage() {
        final RemainingLifetime inboundDays = createRemainingLifetime(15, 8400023);
        final RemainingLifetime outboundDays = RemainingLifetime.of(14, 8400023);
        final RemainingLifetime inboundPercentage = createRemainingLifetime(20, 8400023);
        final RemainingLifetime outboundPercentage = RemainingLifetime.of(25, 8400023);

        boolean resultOfValidation = isValid(inboundDays, inboundPercentage, outboundDays, outboundPercentage);

        assertThat(resultOfValidation).isEqualTo(false);
    }

    @Test
    public void shouldPassValidationIfInboundDaysEqualsOutboundDays() {
        final RemainingLifetime inboundDays = createRemainingLifetime(15, 8400023);
        final RemainingLifetime outboundDays = RemainingLifetime.of(15, 8400023);
        final RemainingLifetime inboundPercentage = createRemainingLifetime(25, 8400023);
        final RemainingLifetime outboundPercentage = RemainingLifetime.of(20, 8400023);

        boolean resultOfValidation = isValid(inboundDays, inboundPercentage, outboundDays, outboundPercentage);

        assertThat(resultOfValidation).isEqualTo(true);
    }

    @Test
    public void shouldPassValidationIfInboundEqualsOutboundPercentage() {
        final RemainingLifetime inboundDays = createRemainingLifetime(15, 8400023);
        final RemainingLifetime outboundDays = RemainingLifetime.of(14, 8400023);
        final RemainingLifetime inboundPercentage = createRemainingLifetime(25, 8400023);
        final RemainingLifetime outboundPercentage = RemainingLifetime.of(25, 8400023);

        boolean resultOfValidation = isValid(inboundDays, inboundPercentage, outboundDays, outboundPercentage);

        assertThat(resultOfValidation).isEqualTo(true);
    }

    private RemainingLifetime createRemainingLifetime(Integer value, long updatedTimestamp) {
        return RemainingLifetime.of(value, updatedTimestamp);
    }
}

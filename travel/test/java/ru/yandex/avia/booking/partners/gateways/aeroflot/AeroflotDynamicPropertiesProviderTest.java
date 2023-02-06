package ru.yandex.avia.booking.partners.gateways.aeroflot;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import ru.yandex.avia.booking.partners.gateways.aeroflot.AeroflotDynamicPropertiesProvider.DynamicProperty;
import ru.yandex.travel.testing.time.SettableClock;

import static org.assertj.core.api.Assertions.assertThat;

public class AeroflotDynamicPropertiesProviderTest {
    @Test
    public void testDynamicProperty() {
        SettableClock clock = new SettableClock();
        AeroflotDynamicPropertiesProvider provider = new AeroflotDynamicPropertiesProvider(clock);

        DynamicProperty property = provider.getAggregatorId();
        property.setValue("initial_value");
        property.setValue("promo_value", Instant.parse("2020-12-07T05:00:00Z"));
        property.setValue("src_value", Instant.parse("2020-12-08T05:00:00Z"));

        clock.setCurrentTime(Instant.parse("1900-12-07T05:00:00Z"));
        assertThat(property.getValue()).isEqualTo("initial_value");
        clock.setCurrentTime(Instant.parse("2020-12-07T05:00:00Z").minusMillis(1));
        assertThat(property.getValue()).isEqualTo("initial_value");

        clock.setCurrentTime(Instant.parse("2020-12-07T05:00:00Z"));
        assertThat(property.getValue()).isEqualTo("promo_value");
        clock.setCurrentTime(Instant.parse("2020-12-08T05:00:00Z").minusMillis(1));
        assertThat(property.getValue()).isEqualTo("promo_value");

        clock.setCurrentTime(Instant.parse("2020-12-08T05:00:00Z"));
        assertThat(property.getValue()).isEqualTo("src_value");
    }
}

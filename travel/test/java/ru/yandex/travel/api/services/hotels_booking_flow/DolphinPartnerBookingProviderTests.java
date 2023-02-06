package ru.yandex.travel.api.services.hotels_booking_flow;

import java.util.List;

import org.junit.Test;

import ru.yandex.travel.api.config.hotels.DolphinConfigurationProperties;
import ru.yandex.travel.api.services.localization.Inflector;
import ru.yandex.travel.api.services.localization.LocalizationService;
import ru.yandex.travel.commons.health.HealthCheckedSupplier;
import ru.yandex.travel.hotels.common.partners.dolphin.DolphinClient;
import ru.yandex.travel.hotels.common.partners.dolphin.utils.RoomNameNormalizer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class DolphinPartnerBookingProviderTests {
    @Test
    public void testBedInfo() {
        Inflector inflector = new Inflector();
        LocalizationService localizationService = new LocalizationService(inflector);
        BedInflector bedInflector = new BedInflector(localizationService, inflector);
        DolphinPartnerBookingProvider provider = new DolphinPartnerBookingProvider(
                new DolphinConfigurationProperties(),
                mock(DolphinClient.class), bedInflector, localizationService, mock(TimezoneDetector.class),
                new HealthCheckedSupplier<>(mock(RoomNameNormalizer.class), "test_normalizer"));
        assertThat(provider.getBedInfo(List.of(1, 1, 1, 5))).isEqualTo("три основных места, один ребенок без места");
    }
}

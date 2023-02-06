package ru.yandex.market.billing.distribution.share;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

/**
 * Тесты для {@link CustomDistributionTariffService}.
 */
class CustomDistributionTariffServiceTest {

    private static final Set<Long> YA_STATION_MSKU = ImmutableSet.of(
            100307940933L, 100307940934L, 100307940935L
    );

    private static final Set<Long> YA_STATION_MINI_MSKU = ImmutableSet.of(
            100783340856L, 100783340857L
    );
    private static final BigDecimal YA_STATION_MSKU_2019_DECEMBER_TARIFF = new BigDecimal("0.1");
    private static final BigDecimal YA_STATION_MSKU_2020_APRIL_TARIFF = new BigDecimal("0.045");
    private static final BigDecimal YA_STATION_MINI_MSKU_2020_APRIL_TARIFF = new BigDecimal("0.11");
    private LocalDate DATE_2019_12_25 = LocalDate.of(2019, 12, 25);
    private CustomDistributionTariffService customDistributionTariffService = new CustomDistributionTariffService();

    @Test
    void testNotYaStationTariff() {
        Optional<BigDecimal> yaStationTariff = customDistributionTariffService.getYaStationTariff(-100500L, LocalDate.of(2015, 1, 1));

        assertThat(yaStationTariff, equalTo(Optional.empty()));
    }

    @Test
    void testYaStationDecemberTariff() {
        Set<Optional<BigDecimal>> tariffs = Stream.concat(YA_STATION_MSKU.stream(), YA_STATION_MINI_MSKU.stream())
                .map(it -> customDistributionTariffService.getYaStationTariff(it, DATE_2019_12_25))
                .collect(Collectors.toSet());

        assertThat(tariffs, hasSize(1));
        assertThat(tariffs, contains(Optional.of(YA_STATION_MSKU_2019_DECEMBER_TARIFF)));
    }

    @Test
    void testYaStationAprilTariff() {
        Set<Optional<BigDecimal>> tariffs = YA_STATION_MSKU.stream()
                .map(it -> customDistributionTariffService.getYaStationTariff(it, LocalDate.of(2020, 4, 13)))
                .collect(Collectors.toSet());

        assertThat(tariffs, hasSize(1));
        assertThat(tariffs, contains(Optional.of(YA_STATION_MSKU_2020_APRIL_TARIFF)));
    }

    @Test
    void testYaStationMiniAprilTariff() {
        Set<Optional<BigDecimal>> tariffs = YA_STATION_MINI_MSKU.stream()
                .map(it -> customDistributionTariffService.getYaStationTariff(it, LocalDate.of(2020, 4, 19)))
                .collect(Collectors.toSet());

        assertThat(tariffs, hasSize(1));
        assertThat(tariffs, contains(Optional.of(YA_STATION_MINI_MSKU_2020_APRIL_TARIFF)));
    }

    @Test
    void testNotYaStationTariff20OfApril() {
        Optional<BigDecimal> yaStationTariff = customDistributionTariffService.getYaStationTariff(-100500L, LocalDate.of(2020, 4, 20));

        assertThat(yaStationTariff, equalTo(Optional.empty()));
    }
}

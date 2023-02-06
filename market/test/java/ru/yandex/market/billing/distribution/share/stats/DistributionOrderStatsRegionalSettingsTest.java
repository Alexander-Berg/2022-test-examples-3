package ru.yandex.market.billing.distribution.share.stats;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.core.billing.distribution.share.stats.model.DistributionOrderCalculationOrder;
import ru.yandex.market.core.geobase.RegionService;
import ru.yandex.market.core.geobase.model.Region;
import ru.yandex.market.core.geobase.model.RegionType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

public class DistributionOrderStatsRegionalSettingsTest {

    private final RegionService regionService = Mockito.mock(RegionService.class);
    private final DistributionOrderStatsRegionalSettings settings =
            new DistributionOrderStatsRegionalSettings(regionService,
                date("2022-04-01"),
                Set.of(103L, 104L));


    @BeforeEach
    public void setup() {
        when(regionService.getParentRegion(RegionType.REPUBLIC, 35L))
                .thenReturn(new Region(10995L, "Краснодарский край", 3L));
        when(regionService.getParentRegion(RegionType.REPUBLIC, 3L))
                .thenReturn(null);
    }

    @Test
    public void testNullRegionId() {
        assertFalse(settings.isCancelledByRegion(null, date("2022-01-23"), false));
    }

    @Test
    public void testWhitelistMatch() {
        assertFalse(settings.isCancelledByRegion(103L, date("2022-04-03"), false));
    }

    @Test
    public void testWhitelistNotMatch() {
        assertTrue(settings.isCancelledByRegion(108L, date("2022-04-03"), false));
    }

    @Test
    public void testWhitelistTooEarly() {
        assertFalse(settings.isCancelledByRegion(108L, date("2022-03-31"), false));
    }

    @Test
    public void testMobileInstallNotCancelled() {
        assertFalse(settings.isCancelledByRegion(108L, date("2022-07-02"), true));
    }

    @Test
    public void testNonMobileInstallCancelled() {
        assertTrue(settings.isCancelledByRegion(108L, date("2022-07-02"), false));
    }

    @Test
    public void testResolveRegionRepublicLevelNull() {
        assertThat(settings.resolveRegionRepublicLevel(null)).isNull();
    }

    @Test
    public void testResolveRegionRepublicLevelCity() {
        assertThat(settings.resolveRegionRepublicLevel(35L)).isEqualTo(10995L);
    }

    @Test
    public void testResolveRegionRepublicLevelCountry() {
        assertThat(settings.resolveRegionRepublicLevel(3L)).isEqualTo(3L);
    }

    private static LocalDateTime date(String dt) {
        return LocalDate.parse(dt).atStartOfDay();
    }
}
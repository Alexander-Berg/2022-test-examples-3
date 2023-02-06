package ru.yandex.market.loyalty.back.usecase;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.region.RegionService;
import ru.yandex.market.loyalty.core.model.RegionSettings;
import ru.yandex.market.loyalty.core.service.RegionSettingsService;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertFalse;

public class RegionSettingsTest extends MarketLoyaltyBackProdDataMockedDbTest {

    private static final int EXPECTED_DISABLED_THRESHOLD_REGIONS_COUNT = 1094;
    private static final int EXPECTED_DISABLED_COIN_EMISSION_COUNT = 70;

    @Autowired
    RegionSettingsService regionSettingsService;
    @Autowired
    RegionService regionService;

    @Test
    public void shouldCheckPresenceAndCountOfEnabledThresholdRegions() {
        ImmutableMap<Integer, BigDecimal> enabledThresholdByRegionId = ImmutableMap.of();

        enabledThresholdByRegionId
                .forEach((regionId, expectedThreshold) -> {
                    RegionSettings withEnabledThreshold = regionSettingsService.findIfWithEnabledThreshold(
                            regionId).orElseThrow(AssertionError::new);
                    assertThat(expectedThreshold, comparesEqualTo(withEnabledThreshold.getThresholdValue()));
                });
        allRegionIdsPresentInRegionService(enabledThresholdByRegionId.keySet());
    }

    @Test
    public void shouldCheckDisabledThresholdRegionsCountAndPresenceInRegionTree() {
        Set<Integer> withDisabledThreshold =
                regionSettingsService.getAllWithDisabledThreshold()
                        .stream()
                        .map(RegionSettings::getRegionId)
                        .collect(ImmutableSet.toImmutableSet());
        assertThat(withDisabledThreshold.size(), comparesEqualTo(EXPECTED_DISABLED_THRESHOLD_REGIONS_COUNT));
        allRegionIdsPresentInRegionService(withDisabledThreshold);
    }

    @Test
    @Ignore
    public void shouldCheckExcludedEmissionRegionsCountAndPresenceInRegionTree() {
        Collection<RegionSettings> withDisabledEmission =
                regionSettingsService.getAllWithDisabledCoinEmission();
        assertThat(withDisabledEmission.size(), equalTo(EXPECTED_DISABLED_COIN_EMISSION_COUNT));
        allRegionIdsPresentInRegionService(
                withDisabledEmission.stream().map(RegionSettings::getRegionId).collect(Collectors.toList())
        );
    }

    @Test
    @Ignore
    public void shouldCheckAllRegionSettingsHasDisabledDeliveryWelcomeBonusByDefault() {
        Stream.concat(
                regionSettingsService.getAllWithEnabledThreshold().stream(),
                regionSettingsService.getAllWithDisabledThreshold().stream()
        )
                .forEach(regionSettings -> assertFalse(regionSettings.isDeliveryWelcomeBonusEnabled()));
    }

    private void allRegionIdsPresentInRegionService(Collection<Integer> regionIds) {
        regionIds
                .forEach(id -> Assert.assertNotNull(
                        "Region id not present in region tree: " + id,
                        regionService.getRegionTree().getRegion(id)
                        )
                );
    }
}

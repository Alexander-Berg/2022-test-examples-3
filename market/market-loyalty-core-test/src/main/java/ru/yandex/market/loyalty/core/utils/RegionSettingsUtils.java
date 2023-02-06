package ru.yandex.market.loyalty.core.utils;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import ru.yandex.market.loyalty.core.model.PropertyStateType;
import ru.yandex.market.loyalty.core.model.RegionSettings;
import ru.yandex.market.loyalty.core.service.RegionSettingsService;

import static ru.yandex.market.loyalty.core.utils.OperationContextFactory.DEFAULT_REGION;

@Component
@AllArgsConstructor
public class RegionSettingsUtils {
    public static final BigDecimal DEFAULT_THRESHOLD = BigDecimal.valueOf(2499L);
    public static final BigDecimal LITTLE_LESS_THAN_DEFAULT_THRESHOLD = DEFAULT_THRESHOLD.subtract(BigDecimal.ONE);

    private final RegionSettingsService regionSettingsService;

    public void setupDefaultThresholdsForDefaultRegion() {
        setupThresholdsForRegion(DEFAULT_THRESHOLD, DEFAULT_REGION);
    }

    public void setupThresholdsForRegion(BigDecimal threshold, long regionId) {
        saveRegionSettings(
                regionId,
                false,
                true,
                threshold,
                true,
                threshold
        );
    }

    public void setupWelcomeBonusAndThresholdsForRegion(long regionId, int threshold) {
        setupWelcomeBonusAndThresholdsForRegion(regionId, threshold, threshold);
    }

    public void setupWelcomeBonusAndThresholdsForRegion(long regionId, int yaPlusThreshold, int threshold) {
        saveRegionSettings(
                Math.toIntExact(regionId),
                true,
                true,
                BigDecimal.valueOf(yaPlusThreshold),
                true,
                BigDecimal.valueOf(threshold)
        );
    }

    public void setupWelcomeBonusAndYaPlusThresholdForRegion(long regionId, int yaPlusThreshold) {
        saveRegionSettings(
                Math.toIntExact(regionId),
                true,
                true,
                BigDecimal.valueOf(yaPlusThreshold),
                false,
                null
        );
    }

    public void saveRegionSettings(
            long regionId,
            boolean welcomeBonus,
            boolean yaPlusThresholdEnabled,
            BigDecimal yaPlusThresholdValue,
            boolean thresholdEnabled,
            BigDecimal thresholdValue
    ) {
        regionSettingsService.clearThresholdData();
        regionSettingsService.saveOrUpdateRegionSettings(
                RegionSettings.builder()
                        .withWelcomeBonusEnabledValue(welcomeBonus)
                        .withYandexPlusThresholdEnabled(
                                yaPlusThresholdEnabled ? PropertyStateType.ENABLED : PropertyStateType.DISABLED
                        )
                        .withYandexPlusThresholdValue(yaPlusThresholdValue)
                        .withThresholdEnabledValue(thresholdEnabled)
                        .withThresholdValue(thresholdValue)
                        .withRegionId(Math.toIntExact(regionId))
                        .build()
        );
        regionSettingsService.reloadCache();
    }
}

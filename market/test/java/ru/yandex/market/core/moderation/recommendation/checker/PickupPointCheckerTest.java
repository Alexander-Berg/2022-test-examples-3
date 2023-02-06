package ru.yandex.market.core.moderation.recommendation.checker;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.moderation.recommendation.PartnerSettingsRecommendationServiceAbstractTest;
import ru.yandex.market.core.moderation.recommendation.SettingType;

/**
 * Тест для {@link PickupPointChecker}.
 */
public class PickupPointCheckerTest extends PartnerSettingsRecommendationServiceAbstractTest {

    @Test
    @DisplayName("Тест проверяет, что магазину, у которого есть ПВЗ, рекомендация не нужна")
    @DbUnitDataSet(before = "csv/PartnerSettingsRecommendationServiceTest.testPickupPointsOk.before.csv")
    void testPickupPointsOk() {
        Assertions.assertFalse(getCheckerResult(SettingType.PICKUP_POINT, 1L));
    }

    @Test
    @DisplayName("Тест проверяет, что магазину, у которого нет ни одной точки, рекомендация нужна")
    void testNoPickupPoints() {
        Assertions.assertTrue(getCheckerResult(SettingType.PICKUP_POINT, 1L));
    }

    @Test
    @DisplayName("Тест проверяет, что магазину, у которого есть только точки типа RETAIL, рекомендация нужна")
    @DbUnitDataSet(before = "csv/PartnerSettingsRecommendationServiceTest.testPickupPointsRetail.before.csv")
    void testPickupPointsRetail() {
        Assertions.assertTrue(getCheckerResult(SettingType.PICKUP_POINT, 1L));
    }
}

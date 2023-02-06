package ru.yandex.market.core.moderation.recommendation.checker;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.moderation.recommendation.PartnerSettingsRecommendationServiceAbstractTest;
import ru.yandex.market.core.moderation.recommendation.SettingType;

/**
 * Тест для {@link PromoChecker}.
 */
@DbUnitDataSet(before = "csv/PromoCheckerTest.before.csv")
class PromoCheckerTest extends PartnerSettingsRecommendationServiceAbstractTest {

    @Test
    @DisplayName("Тест, что не нужна рекомендация, если в одном фиде из двух есть акции")
    void testHavePromosWithTwoFeeds() {
        Assertions.assertFalse(getCheckerResult(SettingType.PROMO, 774L));
    }

    @Test
    @DisplayName("Тест, что не нужна рекомендация, если акции есть в фиде в плейншифте, " +
            "когда в основной индекс магазин еще не попадал")
    void testHavePromosInPlaneshift() {
        Assertions.assertFalse(getCheckerResult(SettingType.PROMO, 1L));
    }

    @Test
    @DisplayName("Тест, что нужна рекомендация, если акции есть в фиде в плейншифте, а в боевом индексе - уже нет")
    void testHavePromosInPlaneshiftButNoneInMain() {
        Assertions.assertTrue(getCheckerResult(SettingType.PROMO, 776L));
    }

    @Test
    @DisplayName("Тест, что не нужна рекомендация, если акции есть в боевом индексе, но в пш их не было")
    void testHavePromosInMainButNoneInPlaneshift() {
        Assertions.assertFalse(getCheckerResult(SettingType.PROMO, 777L));
    }

    @Test
    @DisplayName("Тест, что нужна рекомендация, если в последнем поколении акций нет, даже если в предыдущих были")
    void testHaveNoPromosInLastGen() {
        Assertions.assertTrue(getCheckerResult(SettingType.PROMO, 775L));
    }

    @Test
    @DisplayName("Тест, что не нужна рекомендация, если промо-фича выключена (DONT_WANT или REVOKE)")
    void testHaveNoFeature() {
        Assertions.assertFalse(getCheckerResult(SettingType.PROMO, 778L)); // DONT_WANT
        Assertions.assertFalse(getCheckerResult(SettingType.PROMO, 779L)); // REVOKE
    }

    @Test
    @DisplayName("Тест, что нужна рекомендация, если промо-фича выключена не окончательно (FAIL)")
    void testHaveFailedFeature() {
        Assertions.assertTrue(getCheckerResult(SettingType.PROMO, 781L)); // FAIL
    }

    @Test
    @DisplayName("Тест, что не нужна рекомендация, если нет последнего полного поколения")
    void testHaveNoLastFullGen() {
        Assertions.assertFalse(getCheckerResult(SettingType.PROMO, 780L));
    }
}

package ru.yandex.market.core.moderation.recommendation.checker;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.moderation.recommendation.PartnerSettingsRecommendationServiceAbstractTest;
import ru.yandex.market.core.moderation.recommendation.SettingType;

/**
 * Тест для {@link RoundTheClockPlacementChecker}.
 */
class RoundTheClockPlacementCheckerTest extends PartnerSettingsRecommendationServiceAbstractTest {

    @Test
    @DisplayName("Тест для расписания Пн-Вс 00:00 - 23:59")
    @DbUnitDataSet(before = "csv/PartnerSettingsRecommendationServiceTest.testOneLineScheduleOk.before.csv")
    void testOneLineScheduleOk() {
        Assertions.assertFalse(getCheckerResult(SettingType.ROUND_THE_CLOCK, 1L));
    }

    @Test
    @DisplayName("Тест проверяет, что расписание вида Пн-Вс 00:00 - 23:58 не считается круглосуточным")
    @DbUnitDataSet(before = "csv/PartnerSettingsRecommendationServiceTest.testOneLineScheduleNotOk.before.csv")
    void testOneLineScheduleNotOk() {
        Assertions.assertTrue(getCheckerResult(SettingType.ROUND_THE_CLOCK, 1L));
    }

    @Test
    @DisplayName("Тест проверяет, что расписание вида Пн-Сб 00:00 - 23:59 не считается круглосуточным")
    @DbUnitDataSet(before = "csv/PartnerSettingsRecommendationServiceTest.testOneLineScheduleNotOkByDaysCount.before.csv")
    void testOneLineScheduleNotOkByDaysCount() {
        Assertions.assertTrue(getCheckerResult(SettingType.ROUND_THE_CLOCK, 1L));
    }

    @Test
    @DisplayName("Тест проверяет расписание вида Пн 00:00 - 23:59, ..., Вс 00:00 - 23:59")
    @DbUnitDataSet(before = "csv/PartnerSettingsRecommendationServiceTest.test7DaysScheduleOk.before.csv")
    void test7DaysScheduleOk() {
        Assertions.assertFalse(getCheckerResult(SettingType.ROUND_THE_CLOCK, 1L));
    }

    @Test
    @DisplayName("Тест проверяет расписание с разнообразными интервалами, которое покрывает круглосуточность")
    @DbUnitDataSet(before = "csv/PartnerSettingsRecommendationServiceTest.testDifferentIntervalsOk.before.csv")
    void testDifferentIntervalsOk() {
        Assertions.assertFalse(getCheckerResult(SettingType.ROUND_THE_CLOCK, 1L));
    }

    @Test
    @DisplayName("Тест проверяет расписание с разнообразными интервалами, которое не покрывает круглосуточность")
    @DbUnitDataSet(before = "csv/PartnerSettingsRecommendationServiceTest.testDifferentIntervalsNotOk.before.csv")
    void testDifferentIntervalsNotOk() {
        Assertions.assertTrue(getCheckerResult(SettingType.ROUND_THE_CLOCK, 1L));
    }

    @Test
    @DisplayName("Тест проверяет, что расписание без среды не считается круглосуточным")
    @DbUnitDataSet(before = "csv/PartnerSettingsRecommendationServiceTest.test7DaysScheduleMissedDay.before.csv")
    void test7DaysScheduleMissedDay() {
        Assertions.assertTrue(getCheckerResult(SettingType.ROUND_THE_CLOCK, 1L));
    }

    @Test
    @DisplayName("Тест проверяет, что расписание на семь дней, где один из дней интервал 00:00 - 23:58, не считается круглосуточным")
    @DbUnitDataSet(before = "csv/PartnerSettingsRecommendationServiceTest.test7DaysScheduleMissedMinute.before.csv")
    void test7DaysScheduleMissedMinute() {
        Assertions.assertTrue(getCheckerResult(SettingType.ROUND_THE_CLOCK, 1L));
    }
}
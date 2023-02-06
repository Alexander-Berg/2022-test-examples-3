#include "utils.h"

#include <library/cpp/testing/unittest/registar.h>

using namespace NGeosearch::NWorkingHours;

Y_UNIT_TEST_SUITE(TUtilsTest) {
    Y_UNIT_TEST(TestWeekTimeFromUTCTime) {
        {
            std::tm tm;
            TInstant::ParseIso8601("20190304T011600+03").GmTime(&tm);
            auto time = WeekTimeFromUTCTime(tm, 3 * MINUTES_IN_HOUR);
            UNIT_ASSERT_EQUAL(time.GetDay(), MONDAY);
            UNIT_ASSERT_VALUES_EQUAL(time.GetHours(), 1);
            UNIT_ASSERT_VALUES_EQUAL(time.GetMinutes(), 16);
            UNIT_ASSERT_VALUES_EQUAL(time.GetMinutesFromWeekStart(), 76);
        }
        {
            std::tm tm;
            TInstant::ParseIso8601("20190303T235900+03").GmTime(&tm);
            auto time = WeekTimeFromUTCTime(tm, 3 * MINUTES_IN_HOUR);
            UNIT_ASSERT_EQUAL(time.GetDay(), SUNDAY);
            UNIT_ASSERT_VALUES_EQUAL(time.GetHours(), 23);
            UNIT_ASSERT_VALUES_EQUAL(time.GetMinutes(), 59);
            UNIT_ASSERT_VALUES_EQUAL(time.GetMinutesFromWeekStart(), 6 * MINUTES_IN_DAY + 23 * MINUTES_IN_HOUR + 59);
        }
        {
            std::tm tm;
            TInstant::ParseIso8601("20190304T030400+03").GmTime(&tm);
            auto time = WeekTimeFromUTCTime(tm, 3 * MINUTES_IN_HOUR);
            UNIT_ASSERT_EQUAL(time.GetDay(), MONDAY);
            UNIT_ASSERT_VALUES_EQUAL(time.GetHours(), 3);
            UNIT_ASSERT_VALUES_EQUAL(time.GetMinutes(), 4);
            UNIT_ASSERT_VALUES_EQUAL(time.GetMinutesFromWeekStart(), 3 * MINUTES_IN_HOUR + 4);
        }
    }

    Y_UNIT_TEST(TestTimezoneOffset) {
        const auto test = [](const double longitude, const double tz) {
            double expectedSeconds = tz * 3600;
            UNIT_ASSERT_EQUAL(GuessTimezoneOffset(longitude), expectedSeconds);
            UNIT_ASSERT_EQUAL(GuessTimezoneOffset(longitude + 360), expectedSeconds);
            UNIT_ASSERT_EQUAL(GuessTimezoneOffset(longitude - 360), expectedSeconds);

            expectedSeconds = -expectedSeconds;
            UNIT_ASSERT_EQUAL(GuessTimezoneOffset(-longitude), expectedSeconds);
            UNIT_ASSERT_EQUAL(GuessTimezoneOffset(-longitude + 360), expectedSeconds);
            UNIT_ASSERT_EQUAL(GuessTimezoneOffset(-longitude - 360), expectedSeconds);
        };

        test(0.0, 0);
        test(4.0, 0);
        test(12.0, 1);
        test(55.6, 4);
        test(132.0, 9);
        test(170.0, 11);
        test(190.0, -11);
        test(-74.0, -5);
        test(-122.0, -8);
    }
}

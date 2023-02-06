#include "helpers.h"

#include <extsearch/geo/kernel/working_hours/timeinterval_mask.h>

#include <library/cpp/testing/unittest/registar.h>

#include <util/string/cast.h>

using namespace NGeosearch;
using namespace NGeosearch::NWorkingHours;

Y_UNIT_TEST_SUITE(TTimeintervalMaskTest) {
    Y_UNIT_TEST(TestTimeIntervalMask) {
        TTimeIntervalSet intervals;
        intervals.Add(NWorkingHours::TTime(MONDAY, Parse("19:35")), NWorkingHours::TTime(SATURDAY, Parse("5:17")));

        TTimeIntervalMask mask = MakeTimeIntervalsMask(intervals, 0);
        TTimeIntervalMask validMask = {101945, 1020, 1021, 1022, 1023, 11, 12, 13, 14, 1500, 1501, 1502, 1503, 1504, 150500};
        UNIT_ASSERT_EQUAL(mask, validMask);
    }

    Y_UNIT_TEST(TestOnDutyMask) {
        TTimeIntervalSet intervals;
        intervals.Add(NWorkingHours::TTime(SUNDAY, Parse("19:35")), NWorkingHours::TTime(MONDAY, Parse("23:00")));
        intervals.Add(NWorkingHours::TTime(WEDNESDAY, Parse("0:01")), NWorkingHours::TTime::EndOfDay(WEDNESDAY));

        TTimeIntervalMask mask = MakeOnDutyMask(intervals, 0);
        TTimeIntervalMask validMask = {
            // begin of Monday
            1000, 1001, 1002, 1003, 1004, 1005, 1006, 1007, 100800, 100815,

            // no Wednesday

            // end of Sunday
            160830, 160845, 1609, 1610, 1611, 1612, 1613, 1614, 1615, 1616, 1617, 1618, 1619, 1620, 1621, 1622, 1623};

        UNIT_ASSERT_EQUAL(mask, validMask);
    }

    Y_UNIT_TEST(TestOnDutyMaskBoundary) {
        TTimeIntervalSet intervals;
        intervals.Add(NWorkingHours::TTime(MONDAY, Parse("19:35")), NWorkingHours::TTime::EndOfDay(MONDAY));
        // not on duty on MONDAY - TUESDDAY
        intervals.Add(NWorkingHours::TTime::BeginOfDay(FRIDAY), NWorkingHours::TTime(FRIDAY, Parse("12:00")));
        // on duty on THURSDAY - FRIDAY

        TTimeIntervalMask mask = MakeOnDutyMask(intervals, 0);
        TTimeIntervalMask validMask = {
            // thursday
            130830, 130845, 1309, 1310, 1311, 1312, 1313, 1314, 1315, 1316, 1317, 1318, 1319, 1320, 1321, 1322, 1323,
            // friday morning
            1400, 1401, 1402, 1403, 1404, 1405, 1406, 1407, 140800, 140815};

        UNIT_ASSERT_EQUAL(mask, validMask);
    }

    Y_UNIT_TEST(Test24HoursMask) {
        TTimeIntervalSet intervals = MakeIntervalSet(TTimeOfDay::BeginOfDay(), TTimeOfDay::EndOfDay(), EVERYDAY);

        TTimeIntervalMask mask = MakeTimeIntervalsMask(intervals, 0);
        TTimeIntervalMask validMask = {1};
        UNIT_ASSERT_EQUAL(mask, validMask);
    }

    Y_UNIT_TEST(Test24HoursOnDuty) {
        TTimeIntervalSet intervals = MakeIntervalSet(TTimeOfDay::BeginOfDay(), TTimeOfDay::EndOfDay(), EVERYDAY);

        TTimeIntervalMask mask = MakeOnDutyMask(intervals, 0);
        TTimeIntervalMask validMask = {1};
        UNIT_ASSERT_EQUAL(mask, validMask);
    }

    Y_UNIT_TEST(TestTimezone) {
        TTimeIntervalSet intervals;
        intervals.Add(TTimeInterval(NWorkingHours::TTime(SUNDAY, TTimeOfDay(11, 20)), NWorkingHours::TTime(SUNDAY, TTimeOfDay(23, 30))));

        TTimeIntervalMask mask = MakeTimeIntervalsMask(intervals, 5 * 3600);

        TTimeIntervalMask validMask = {160630, 160645, 1607, 1608, 1609, 1610, 1611, 1612, 1613, 1614, 1615, 1616, 1617, 161800, 161815};
        UNIT_ASSERT_EQUAL(mask, validMask);
    }
}

#include "helpers.h"

#include <extsearch/geo/kernel/working_hours/timeinterval_set.h>

#include <library/cpp/testing/unittest/registar.h>

using namespace NGeosearch;
using namespace NGeosearch::NWorkingHours;

Y_UNIT_TEST_SUITE(TTimeIntervalSetTest) {
    Y_UNIT_TEST(TestSplitToDays) {
        TTimeIntervalSet intervals = MakeIntervalSet(Parse("20:30"), Parse("02:15"), EVERYDAY);

        {
            auto split = intervals.SplitToDays();
            UNIT_ASSERT_VALUES_EQUAL(split.size(), 14);
            UNIT_ASSERT_EQUAL(split.back(),
                              TTimeInterval(NWorkingHours::TTime(SUNDAY, Parse("20:30")),
                                            NWorkingHours::TTime(NEXT_WEEK_FIRST_DAY, Parse("00:00"))));
        }
        {
            auto split = intervals.SplitToDaysPassingMidnight();
            UNIT_ASSERT_VALUES_EQUAL(split.size(), 7);
            UNIT_ASSERT_EQUAL(split.back(),
                              TTimeInterval(NWorkingHours::TTime(SUNDAY, Parse("20:30")),
                                            NWorkingHours::TTime(NEXT_WEEK_FIRST_DAY, Parse("02:15"))));
        }

        intervals.UniteWith(MakeIntervalSet(Parse("02:00"), Parse("05:59"), EVERYDAY));
        UNIT_ASSERT_EQUAL(intervals.SplitToDays().size(), 14);
        UNIT_ASSERT_EQUAL(intervals.SplitToDaysPassingMidnight().size(), 7);

        intervals.UniteWith(MakeIntervalSet(Parse("06:00"), Parse("20:29"), EVERYDAY));
        UNIT_ASSERT_EQUAL(intervals.SplitToDays().size(), 7);
        UNIT_ASSERT_EQUAL(intervals.SplitToDaysPassingMidnight().size(), 7);
    }

    Y_UNIT_TEST(TestSplitToDays2) {
        TTimeIntervalSet intervals;
        intervals.Add(TTimeInterval(NWorkingHours::TTime(MONDAY, Parse("09:00")), NWorkingHours::TTime(FRIDAY, Parse("18:00"))));

        auto split = intervals.SplitToDays();
        UNIT_ASSERT_VALUES_EQUAL(split.size(), 5);
        UNIT_ASSERT_EQUAL(split.back(),
                          TTimeInterval(NWorkingHours::TTime(FRIDAY, Parse("00:00")),
                                        NWorkingHours::TTime(FRIDAY, Parse("18:00"))));

        UNIT_ASSERT_EQUAL(split, intervals.SplitToDaysPassingMidnight());
    }

    Y_UNIT_TEST(TestMerging) {
        TTimeIntervalSet intervals;
        intervals.Add(TTimeInterval(NWorkingHours::TTime(MONDAY, Parse("12:30")), NWorkingHours::TTime(MONDAY, Parse("14:00"))));
        intervals.Add(TTimeInterval(NWorkingHours::TTime(MONDAY, Parse("13:30")), NWorkingHours::TTime(MONDAY, Parse("15:00"))));
        UNIT_ASSERT_EQUAL(intervals.GetIntervals().size(), 1);
        UNIT_ASSERT_EQUAL(intervals.GetIntervals().begin()->GetFrom(), NWorkingHours::TTime(MONDAY, Parse("12:30")));
        UNIT_ASSERT_EQUAL(intervals.GetIntervals().begin()->GetTo(), NWorkingHours::TTime(MONDAY, Parse("15:00")));
    }

    Y_UNIT_TEST(TestMergingMultiday) {
        TTimeIntervalSet intervals;
        intervals.Add(TTimeInterval(NWorkingHours::TTime(TUESDAY, Parse("0:00")), NWorkingHours::TTime(TUESDAY, Parse("2:00"))));
        intervals.Add(TTimeInterval(NWorkingHours::TTime(MONDAY, Parse("18:00")), NWorkingHours::TTime(MONDAY, Parse("23:59"))));
        UNIT_ASSERT_EQUAL(intervals.GetIntervals().size(), 1);
        UNIT_ASSERT_EQUAL(intervals.GetIntervals().begin()->GetFrom(), NWorkingHours::TTime(MONDAY, Parse("18:00")));
        UNIT_ASSERT_EQUAL(intervals.GetIntervals().begin()->GetTo(), NWorkingHours::TTime(TUESDAY, Parse("2:00")));
    }

    Y_UNIT_TEST(TestMergingWithoutOverlap) {
        TTimeIntervalSet intervals;
        intervals.Add(TTimeInterval(NWorkingHours::TTime(MONDAY, Parse("12:30")), NWorkingHours::TTime(MONDAY, Parse("14:29"))));
        intervals.Add(TTimeInterval(NWorkingHours::TTime(MONDAY, Parse("14:30")), NWorkingHours::TTime(MONDAY, Parse("15:00"))));
        UNIT_ASSERT_EQUAL(intervals.GetIntervals().size(), 1);
        UNIT_ASSERT_EQUAL(intervals.GetIntervals().begin()->GetFrom(), NWorkingHours::TTime(MONDAY, Parse("12:30")));
        UNIT_ASSERT_EQUAL(intervals.GetIntervals().begin()->GetTo(), NWorkingHours::TTime(MONDAY, Parse("15:00")));

        intervals.Add(TTimeInterval(NWorkingHours::TTime(MONDAY, Parse("15:01")), NWorkingHours::TTime(MONDAY, Parse("16:58"))));
        UNIT_ASSERT_EQUAL(intervals.GetIntervals().size(), 1);

        intervals.Add(TTimeInterval(NWorkingHours::TTime(MONDAY, Parse("17:00")), NWorkingHours::TTime(MONDAY, Parse("18:00"))));
        UNIT_ASSERT_EQUAL(intervals.GetIntervals().size(), 2);
    }

    Y_UNIT_TEST(TestInsertIntoMiddle) {
        TTimeIntervalSet intervals;
        intervals.Add(TTimeInterval(NWorkingHours::TTime(MONDAY, Parse("12:00")), NWorkingHours::TTime(MONDAY, Parse("14:00"))));
        intervals.Add(TTimeInterval(NWorkingHours::TTime(MONDAY, Parse("15:00")), NWorkingHours::TTime(MONDAY, Parse("16:20"))));
        intervals.Add(TTimeInterval(NWorkingHours::TTime(MONDAY, Parse("17:30")), NWorkingHours::TTime(MONDAY, Parse("19:00"))));
        UNIT_ASSERT_EQUAL(intervals.GetIntervals().size(), 3);

        intervals.Add(TTimeInterval(NWorkingHours::TTime(MONDAY, Parse("14:00")), NWorkingHours::TTime(MONDAY, Parse("20:00"))));
        UNIT_ASSERT_EQUAL(intervals.GetIntervals().size(), 1);

        UNIT_ASSERT_EQUAL(intervals.GetIntervals().begin()->GetFrom(), NWorkingHours::TTime(MONDAY, Parse("12:00")));
        UNIT_ASSERT_EQUAL(intervals.GetIntervals().begin()->GetTo(), NWorkingHours::TTime(MONDAY, Parse("20:00")));
    }

    Y_UNIT_TEST(TestInsertSame) {
        TTimeIntervalSet intervals;
        intervals.Add(TTimeInterval(NWorkingHours::TTime(MONDAY, Parse("12:00")), NWorkingHours::TTime(MONDAY, Parse("14:00"))));
        intervals.Add(TTimeInterval(NWorkingHours::TTime(MONDAY, Parse("12:00")), NWorkingHours::TTime(MONDAY, Parse("14:00"))));
        UNIT_ASSERT_EQUAL(intervals.GetIntervals().size(), 1);
    }

    Y_UNIT_TEST(TestAddEndOfWeek) {
        TTimeIntervalSet intervals;

        intervals.Add(NWorkingHours::TTime(SUNDAY, Parse("24:00")), NWorkingHours::TTime(MONDAY, Parse("0:00")));
        UNIT_ASSERT_EQUAL(intervals.GetIntervals().size(), 0);

        intervals.Add(NWorkingHours::TTime(SUNDAY, Parse("24:00")), NWorkingHours::TTime(MONDAY, Parse("3:00")));
        UNIT_ASSERT_EQUAL(intervals.GetIntervals().size(), 1);
        UNIT_ASSERT_EQUAL(intervals.GetIntervals().begin()->GetFrom().GetDay(), MONDAY);
    }

    Y_UNIT_TEST(TestPassingWeekBoundary) {
        auto check = [](const TTimeIntervalSet& intervalSet) {
            const auto intervals = intervalSet.GetIntervalSeq();
            UNIT_ASSERT_VALUES_EQUAL(intervals.size(), 2);
            UNIT_ASSERT_EQUAL(intervals[0].GetFrom(), TTime(MONDAY, Parse("00:00")));
            UNIT_ASSERT_EQUAL(intervals[0].GetTo(), TTime(MONDAY, Parse("06:00")));
            UNIT_ASSERT_EQUAL(intervals[1].GetFrom(), TTime(SUNDAY, Parse("20:00")));
            UNIT_ASSERT_EQUAL(intervals[1].GetTo(), TTime(SUNDAY, Parse("24:00")));
        };

        {
            TTimeIntervalSet intervals;
            intervals.Add(TTimeInterval(NWorkingHours::TTime(MONDAY, Parse("00:00")), NWorkingHours::TTime(MONDAY, Parse("06:00"))));
            intervals.Add(TTimeInterval(NWorkingHours::TTime(SUNDAY, Parse("20:00")), NWorkingHours::TTime(SUNDAY, Parse("24:00"))));
            check(intervals);
        }
        {
            TTimeIntervalSet intervals;
            intervals.Add(TTime(SUNDAY, Parse("20:00")), TTime(MONDAY, Parse("06:00")));
            check(intervals);
        }
        {
            TTimeIntervalSet intervals;
            intervals.Add(TTimeInterval(NWorkingHours::TTime(SUNDAY, Parse("20:00")), NWorkingHours::TTime(NEXT_WEEK_FIRST_DAY, Parse("06:00"))));
            check(intervals);
        }
    }
}

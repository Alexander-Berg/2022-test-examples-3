#include "helpers.h"

#include <extsearch/geo/kernel/working_hours/actualize.h>

#include <library/cpp/testing/unittest/registar.h>

#include <util/datetime/base.h>

using namespace NGeosearch::NWorkingHours;
namespace NB = NGeosearch::NMmsTypes::NBusiness;
namespace NWH = NGeosearch::NWorkingHours;

/**
 * TODO(sobols@): Rewrite this tests without MMS
 */

Y_UNIT_TEST_SUITE(TScheduledTest) {
    Y_UNIT_TEST(TestHoursTrivialActualization1) {
        TMmsHoursMutable hrs;
        hrs.Intervals.push_back(NB::CreateTimeInterval(0, MINUTES_IN_DAY * 1));
        hrs.Intervals.push_back(NB::CreateTimeInterval(MINUTES_IN_DAY * 1 + MINUTES_IN_HOUR * 6, MINUTES_IN_DAY * 1 + MINUTES_IN_HOUR * 11));
        hrs.Intervals.push_back(NB::CreateTimeInterval(MINUTES_IN_DAY * 1 + MINUTES_IN_HOUR * 12, MINUTES_IN_DAY * 1 + MINUTES_IN_HOUR * 14));
        hrs.Intervals.push_back(NB::CreateTimeInterval(MINUTES_IN_DAY * 2, MINUTES_IN_WEEK));
        TTimeIntervalSeq splittedIntervals = FromHours(hrs).SplitToDaysPassingMidnight();

        TMmsScheduledHoursMutable scheduledHours;

        TDate userLocalDate = {};

        TTimeIntervalSeq actualIntervals = ActualizeWorkingHours(splittedIntervals, scheduledHours, userLocalDate);
        UNIT_ASSERT_EQUAL(actualIntervals.size(), splittedIntervals.size());
        for (size_t i = 0; i < actualIntervals.size(); ++i) {
            UNIT_ASSERT_EQUAL(actualIntervals[i], splittedIntervals[i]);
        }
    }

    Y_UNIT_TEST(TestHoursTrivialActualization2) {
        TMmsHoursMutable hrs;
        hrs.Intervals.push_back(NB::CreateTimeInterval(0, MINUTES_IN_DAY * 1));
        hrs.Intervals.push_back(NB::CreateTimeInterval(MINUTES_IN_DAY * 1 + MINUTES_IN_HOUR * 6, MINUTES_IN_DAY * 1 + MINUTES_IN_HOUR * 11));
        hrs.Intervals.push_back(NB::CreateTimeInterval(MINUTES_IN_DAY * 1 + MINUTES_IN_HOUR * 12, MINUTES_IN_DAY * 1 + MINUTES_IN_HOUR * 14));
        hrs.Intervals.push_back(NB::CreateTimeInterval(MINUTES_IN_DAY * 2, MINUTES_IN_WEEK));
        TTimeIntervalSeq splittedIntervals = FromHours(hrs).SplitToDaysPassingMidnight();

        TDayToTimeIntervalsMap schInervals;
        schInervals["20181105"] = TTimeIntervalSet();
        TMmsScheduledHoursMutable scheduledHours = ToMmsScheduledHours(schInervals, 0);

        TDate userLocalDate = {};

        TTimeIntervalSeq actualIntervals = ActualizeWorkingHours(splittedIntervals, scheduledHours, userLocalDate);
        UNIT_ASSERT_EQUAL(actualIntervals.size(), splittedIntervals.size());
        for (size_t i = 0; i < actualIntervals.size(); ++i) {
            UNIT_ASSERT_EQUAL(actualIntervals[i], splittedIntervals[i]);
        }
    }

    Y_UNIT_TEST(TestHoursAddHolidayActualization) {
        TMmsHoursMutable hrs;
        hrs.Intervals.push_back(NB::CreateTimeInterval(0, MINUTES_IN_DAY * 1));
        hrs.Intervals.push_back(NB::CreateTimeInterval(MINUTES_IN_DAY * 1 + MINUTES_IN_HOUR * 6, MINUTES_IN_DAY * 1 + MINUTES_IN_HOUR * 11));
        hrs.Intervals.push_back(NB::CreateTimeInterval(MINUTES_IN_DAY * 1 + MINUTES_IN_HOUR * 12, MINUTES_IN_DAY * 1 + MINUTES_IN_HOUR * 14));
        hrs.Intervals.push_back(NB::CreateTimeInterval(MINUTES_IN_DAY * 2, MINUTES_IN_WEEK));
        TTimeIntervalSeq splittedIntervals = FromHours(hrs).SplitToDaysPassingMidnight();

        TDayToTimeIntervalsMap schInervals;
        schInervals["20181105"] = TTimeIntervalSet();
        TMmsScheduledHoursMutable scheduledHours = ToMmsScheduledHours(schInervals, 0);

        TDate userLocalDate = TDate("20181105");

        TTimeIntervalSeq actualIntervals = ActualizeWorkingHours(splittedIntervals, scheduledHours, userLocalDate);
        UNIT_ASSERT_EQUAL(actualIntervals.size(), 7);
        for (size_t i = 0; i < actualIntervals.size(); ++i) {
            UNIT_ASSERT_EQUAL(actualIntervals[i], splittedIntervals[i + 1]);
        }
    }

    Y_UNIT_TEST(TestHoursAddHolidaysWeekActualization) {
        TMmsHoursMutable hrs;
        hrs.Intervals.push_back(NB::CreateTimeInterval(0, MINUTES_IN_DAY * 1));
        hrs.Intervals.push_back(NB::CreateTimeInterval(MINUTES_IN_DAY * 1 + MINUTES_IN_HOUR * 6, MINUTES_IN_DAY * 1 + MINUTES_IN_HOUR * 11));
        hrs.Intervals.push_back(NB::CreateTimeInterval(MINUTES_IN_DAY * 1 + MINUTES_IN_HOUR * 12, MINUTES_IN_DAY * 1 + MINUTES_IN_HOUR * 14));
        hrs.Intervals.push_back(NB::CreateTimeInterval(MINUTES_IN_DAY * 2, MINUTES_IN_WEEK));
        TTimeIntervalSeq splittedIntervals = FromHours(hrs).SplitToDaysPassingMidnight();

        TDayToTimeIntervalsMap schInervals;
        for (const auto& day : {"05", "06", "07", "08", "09", "10", "11"}) {
            schInervals[TString("201811") + day] = TTimeIntervalSet();
        }
        TMmsScheduledHoursMutable scheduledHours = ToMmsScheduledHours(schInervals, 0);

        TDate userLocalDate = TDate("20181105");

        TTimeIntervalSeq actualIntervals = ActualizeWorkingHours(splittedIntervals, scheduledHours, userLocalDate);
        UNIT_ASSERT_EQUAL(actualIntervals.size(), 0);
    }

    Y_UNIT_TEST(TestHoursAnotherActualization) {
        TMmsHoursMutable hrs;
        hrs.Intervals.push_back(NB::CreateTimeInterval(0, MINUTES_IN_DAY * 1));
        hrs.Intervals.push_back(NB::CreateTimeInterval(MINUTES_IN_DAY * 1 + MINUTES_IN_HOUR * 6, MINUTES_IN_DAY * 1 + MINUTES_IN_HOUR * 11));
        hrs.Intervals.push_back(NB::CreateTimeInterval(MINUTES_IN_DAY * 1 + MINUTES_IN_HOUR * 12, MINUTES_IN_DAY * 1 + MINUTES_IN_HOUR * 14));
        hrs.Intervals.push_back(NB::CreateTimeInterval(MINUTES_IN_DAY * 2, MINUTES_IN_WEEK));
        TTimeIntervalSeq splittedIntervals = FromHours(hrs).SplitToDaysPassingMidnight();

        TTimeIntervalSet intervals1;
        intervals1.Add(NWH::TTime(MONDAY, Parse("00:00")), NWH::TTime(MONDAY, Parse("05:00")));
        intervals1.Add(NWH::TTime(MONDAY, Parse("06:00")), NWH::TTime(MONDAY, Parse("11:00")));
        TTimeIntervalSet intervals2;
        intervals2.Add(NWH::TTime(TUESDAY, Parse("00:00")), NWH::TTime(TUESDAY, Parse("24:00")));
        TTimeIntervalSet intervals3;
        intervals3.Add(NWH::TTime(FRIDAY, Parse("00:00")), NWH::TTime(FRIDAY, Parse("24:00")));
        TDayToTimeIntervalsMap schInervals;
        schInervals["20181105"] = intervals1;
        schInervals["20181106"] = intervals2;
        schInervals["20181109"] = intervals3;
        TMmsScheduledHoursMutable scheduledHours = ToMmsScheduledHours(schInervals, 0);

        TDate userLocalDate = TDate("20181105");

        TTimeIntervalSeq actualIntervals = ActualizeWorkingHours(splittedIntervals, scheduledHours, userLocalDate);

        UNIT_ASSERT_EQUAL(actualIntervals.size(), 8);
        UNIT_ASSERT_EQUAL(actualIntervals[0], TTimeInterval(TTime(EDay::MONDAY, 0, 0), TTime(EDay::MONDAY, 5, 0)));
        UNIT_ASSERT_EQUAL(actualIntervals[1], TTimeInterval(TTime(EDay::MONDAY, 6, 0), TTime(EDay::MONDAY, 11, 0)));
        UNIT_ASSERT_EQUAL(actualIntervals[2], TTimeInterval(TTime(EDay::TUESDAY, 0, 0), TTime(EDay::WEDNESDAY, 0, 0)));
        for (size_t i = 3; i < actualIntervals.size(); ++i) {
            UNIT_ASSERT_EQUAL(actualIntervals[i], splittedIntervals[i]);
        }
    }

    Y_UNIT_TEST(TestHoursActualizationOnWeekBoundary) {
        const int tzOffsetMinutes = 3 * MINUTES_IN_HOUR;

        // Open on Sat, Sun from 12:00 till 06:00
        TTimeIntervalSet intervals;
        intervals.Add(TTime::BeginOfWeek(), TTime(EDay::MONDAY, 6, 0));
        intervals.Add(TTime(EDay::SATURDAY, 12, 0), TTime(EDay::SUNDAY, 6, 0));
        intervals.Add(TTime(EDay::SUNDAY, 12, 0), TTime::EndOfWeek());
        const TMmsHoursMutable hrs = ToMmsHours(intervals, tzOffsetMinutes);
        const TTimeIntervalSeq splitIntervals = FromHours(hrs).SplitToDaysPassingMidnight();

        {
            // Changes on Sunday: from 12:00 till 18:00
            TTimeIntervalSet intervals1;
            intervals1.Add(TTime(SUNDAY, 12, 0), TTime(SUNDAY, 18, 0));
            TDayToTimeIntervalsMap schIntervals;
            schIntervals["20190303"] = std::move(intervals1);
            TMmsScheduledHoursMutable scheduledHours = ToMmsScheduledHours(schIntervals, tzOffsetMinutes);

            TDate userLocalDate = TDate("20190301");

            TTimeIntervalSeq actualIntervals = ActualizeWorkingHours(splitIntervals, scheduledHours, userLocalDate);

            UNIT_ASSERT_VALUES_EQUAL(actualIntervals.size(), 2);
            UNIT_ASSERT_EQUAL(actualIntervals[0], TTimeInterval(TTime(EDay::SATURDAY, 12, 0), TTime(EDay::SUNDAY, 6, 0)));
            UNIT_ASSERT_EQUAL(actualIntervals[1], TTimeInterval(TTime(EDay::SUNDAY, 12, 0), TTime(EDay::SUNDAY, 18, 0)));
        }
    }

    Y_UNIT_TEST(TestHoursActualizationForNext7Days) {
        const int tzOffsetMinutes = 3 * MINUTES_IN_HOUR;

        TTimeIntervalSet intervals;
        intervals.Add(TTime::BeginOfWeek(), TTime(EDay::MONDAY, 6, 0));
        const TMmsHoursMutable hrs = ToMmsHours(intervals, tzOffsetMinutes);
        const TTimeIntervalSeq splitIntervals = FromHours(hrs).SplitToDaysPassingMidnight();

        {
            TTimeIntervalSet intervals;
            intervals.Add(TTime(EDay::MONDAY, 12, 0), TTime(EDay::MONDAY, 18, 0));
            TDayToTimeIntervalsMap schIntervals;
            schIntervals["20200720"] = std::move(intervals); // Monday
            TMmsScheduledHoursMutable scheduledHours = ToMmsScheduledHours(schIntervals, tzOffsetMinutes);

            TDate userLocalDate = TDate("20200719"); // Sunday

            // Check EActualizeFor::CurrentWeek - expecting common schedule
            {
                TTimeIntervalSeq actualIntervals = ActualizeWorkingHours(splitIntervals, scheduledHours, userLocalDate, EActualizeFor::CurrentWeek);

                UNIT_ASSERT_VALUES_EQUAL(actualIntervals.size(), 1);
                UNIT_ASSERT_EQUAL(actualIntervals[0], TTimeInterval(TTime::BeginOfWeek(), TTime(EDay::MONDAY, 6, 0)));
            }

            // Check EActualizeFor::Next7Days - expecting custom schedule
            {
                TTimeIntervalSeq actualIntervals = ActualizeWorkingHours(splitIntervals, scheduledHours, userLocalDate, EActualizeFor::Next7Days);

                UNIT_ASSERT_VALUES_EQUAL(actualIntervals.size(), 1);
                UNIT_ASSERT_EQUAL(actualIntervals[0], TTimeInterval(TTime(EDay::MONDAY, 12, 0), TTime(EDay::MONDAY, 18, 0)));
            }
        }
    }
}

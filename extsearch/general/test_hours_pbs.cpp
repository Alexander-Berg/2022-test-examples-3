#include "helpers.h"
#include "proto2ro.h"

#include <extsearch/geo/kernel/working_hours/hours.h>
#include <extsearch/geo/kernel/working_hours/print_hours.h>
#include <extsearch/geo/kernel/working_hours/timeinterval_mask.h>

#include <library/cpp/testing/unittest/registar.h>

#include <util/datetime/base.h>

using namespace NGeosearch::NWorkingHours;

using TProtoHours = NGeosearch::NProtos::THours;
using TROHours = NGeosearch::NProtos::NRO::THours;

namespace {
    TProtoHours MakeProtoHours(std::initializer_list<std::pair<unsigned, unsigned>> intervals, int tzOffset = 0) {
        TProtoHours result;
        for (const auto& pair : intervals) {
            auto* pbi = result.AddIntervals();
            pbi->SetFrom(pair.first);
            pbi->SetTo(pair.second);
        }
        result.SetTimezoneOffset(tzOffset);
        return result;
    }
} // namespace

Y_UNIT_TEST_SUITE(TPbsHoursTest) {
    static std::tm MakeTime(EDay wday, unsigned hr, unsigned min) {
        std::tm date = std::tm();
        date.tm_year = 113;
        date.tm_mon = 10;
        date.tm_mday = 4 + Number(wday);
        date.tm_hour = hr;
        date.tm_min = min;
        date.tm_sec = 0;
        std::mktime(&date);
        return date;
    }

    Y_UNIT_TEST(TestIsTimeInWorkingHours1) {
        TROHoursHolder roHrs{MakeProtoHours({
                                                {0, MINUTES_IN_WEEK},
                                            }, 120)};
        const TROHours& hrs = *roHrs;

        UNIT_ASSERT(IsTimeInWorkingHours(MakeTime(EDay::MONDAY, 0, 0), hrs));
        UNIT_ASSERT(IsTimeInWorkingHours(MakeTime(EDay::THURSDAY, 13, 30), hrs));
        UNIT_ASSERT(IsTimeInWorkingHours(MakeTime(EDay::SUNDAY, 22, 00), hrs));
        UNIT_ASSERT(IsTimeInWorkingHours(MakeTime(EDay::SUNDAY, 22, 01), hrs));
        UNIT_ASSERT(IsTimeInWorkingHours(MakeTime(EDay::SUNDAY, 23, 59), hrs));
    }

    Y_UNIT_TEST(TestIsTimeInWorkingHours2) {
        TROHoursHolder roHrs{MakeProtoHours({
                                                {MINUTES_IN_DAY * 1 + MINUTES_IN_HOUR * 8, MINUTES_IN_DAY * 1 + MINUTES_IN_HOUR * 14},
                                                {MINUTES_IN_DAY * 2 + MINUTES_IN_HOUR * 14, MINUTES_IN_DAY * 3 + MINUTES_IN_HOUR * 2},
                                            }, 120)};
        const TROHours& hrs = *roHrs;

        UNIT_ASSERT(!IsTimeInWorkingHours(MakeTime(EDay::MONDAY, 12, 0), hrs));
        UNIT_ASSERT(!IsTimeInWorkingHours(MakeTime(EDay::TUESDAY, 05, 59), hrs));
        UNIT_ASSERT(IsTimeInWorkingHours(MakeTime(EDay::TUESDAY, 06, 00), hrs));
        UNIT_ASSERT(IsTimeInWorkingHours(MakeTime(EDay::TUESDAY, 12, 00), hrs));
        UNIT_ASSERT(!IsTimeInWorkingHours(MakeTime(EDay::TUESDAY, 12, 01), hrs));

        UNIT_ASSERT(IsTimeInWorkingHours(MakeTime(EDay::THURSDAY, 00, 00), hrs));
        UNIT_ASSERT(!IsTimeInWorkingHours(MakeTime(EDay::THURSDAY, 00, 01), hrs));
    }

    Y_UNIT_TEST(TestIsOnDutyNow1) {
        TROHoursHolder roHrs{MakeProtoHours({
                                                {MINUTES_IN_HOUR * 22, MINUTES_IN_HOUR * 24 + 2},
                                                {MINUTES_IN_DAY * 4 + MINUTES_IN_HOUR * 23, MINUTES_IN_DAY * 4 + MINUTES_IN_HOUR * 25},
                                            }, 120)};
        const TROHours& hrs = *roHrs;

        UNIT_ASSERT(!IsOnDutyNow(MakeTime(EDay::MONDAY, 6, 0), hrs));
        UNIT_ASSERT(IsOnDutyNow(MakeTime(EDay::MONDAY, 7, 0), hrs));
        UNIT_ASSERT(IsOnDutyNow(MakeTime(EDay::MONDAY, 22, 0), hrs));

        UNIT_ASSERT(IsOnDutyNow(MakeTime(EDay::TUESDAY, 6, 0), hrs));
        UNIT_ASSERT(!IsOnDutyNow(MakeTime(EDay::TUESDAY, 7, 0), hrs));
        UNIT_ASSERT(!IsOnDutyNow(MakeTime(EDay::TUESDAY, 22, 0), hrs));

        UNIT_ASSERT(!IsOnDutyNow(MakeTime(EDay::WEDNESDAY, 6, 0), hrs));
        UNIT_ASSERT(!IsOnDutyNow(MakeTime(EDay::WEDNESDAY, 7, 0), hrs));

        UNIT_ASSERT(!IsOnDutyNow(MakeTime(EDay::THURSDAY, 6, 0), hrs));
        UNIT_ASSERT(!IsOnDutyNow(MakeTime(EDay::THURSDAY, 7, 0), hrs));

        UNIT_ASSERT(!IsOnDutyNow(MakeTime(EDay::FRIDAY, 6, 0), hrs));
        UNIT_ASSERT(IsOnDutyNow(MakeTime(EDay::FRIDAY, 7, 0), hrs));
        UNIT_ASSERT(IsOnDutyNow(MakeTime(EDay::SATURDAY, 6, 0), hrs));
        UNIT_ASSERT(!IsOnDutyNow(MakeTime(EDay::SATURDAY, 7, 0), hrs));
    }

    Y_UNIT_TEST(TestIsOnDutyNow2) {
        TTimeIntervalSet intervals;
        intervals.Add(TTime(MONDAY, Parse("19:35")), TTime::EndOfDay(MONDAY));
        // not on duty on MONDAY - TUESDDAY
        intervals.Add(TTime::BeginOfDay(FRIDAY), TTime(FRIDAY, Parse("12:00")));
        // on duty on THURSDAY - FRIDAY
        intervals.Add(TTime(SATURDAY, Parse("00:01")), TTime(SATURDAY, Parse("22:00")));
        // not on duty on FRIDAY - SATURDAY

        TProtoHours protoHrs = ToProtoHours(intervals, 120 * MINUTES_IN_HOUR);
        TROHoursHolder roHrs{protoHrs};
        const TROHours& hrs = *roHrs;

        UNIT_ASSERT(!IsOnDutyNow(MakeTime(EDay::MONDAY, 22, 0), hrs));
        UNIT_ASSERT(!IsOnDutyNow(MakeTime(EDay::TUESDAY, 6, 0), hrs));
        UNIT_ASSERT(IsOnDutyNow(MakeTime(EDay::THURSDAY, 22, 0), hrs));
        UNIT_ASSERT(IsOnDutyNow(MakeTime(EDay::FRIDAY, 6, 0), hrs));
        UNIT_ASSERT(!IsOnDutyNow(MakeTime(EDay::FRIDAY, 22, 0), hrs));
        UNIT_ASSERT(!IsOnDutyNow(MakeTime(EDay::SATURDAY, 6, 0), hrs));
    }

    Y_UNIT_TEST(TestContainsWeekend1) {
        TROHoursHolder roHrs{MakeProtoHours({
            {1, MINUTES_IN_HOUR * 24},
            {MINUTES_IN_DAY * 4 + MINUTES_IN_HOUR * 23, MINUTES_IN_DAY * 4 + MINUTES_IN_HOUR * 23 + 58},
        })};
        const TROHours& hrs = *roHrs;
        UNIT_ASSERT(!ContainsWeekend(hrs));
    }

    Y_UNIT_TEST(TestContainsWeekend2) {
        TROHoursHolder roHrs{MakeProtoHours({
            {MINUTES_IN_DAY * 6 + MINUTES_IN_HOUR * 23, MINUTES_IN_DAY * 6 + MINUTES_IN_HOUR * 27},
        })};
        const TROHours& hrs = *roHrs;
        UNIT_ASSERT(ContainsWeekend(hrs));
    }

    Y_UNIT_TEST(TestContainsWeekend3) {
        TROHoursHolder roHrs{MakeProtoHours({
            {MINUTES_IN_DAY * 4 + MINUTES_IN_HOUR * 23, MINUTES_IN_DAY * 4 + MINUTES_IN_HOUR * 27},
        })};
        const TROHours& hrs = *roHrs;
        UNIT_ASSERT(ContainsWeekend(hrs));
    }

    Y_UNIT_TEST(TestContainsWeekend4) {
        TROHoursHolder roHrs{MakeProtoHours({
            {MINUTES_IN_DAY * 5 + MINUTES_IN_HOUR * 12, MINUTES_IN_DAY * 5 + MINUTES_IN_HOUR * 12 + 30},
        })};
        const TROHours& hrs = *roHrs;
        UNIT_ASSERT(ContainsWeekend(hrs));
    }

    Y_UNIT_TEST(TestGetMinutesToOpen1) {
        TROHoursHolder roHrs{MakeProtoHours({
                                                {0, MINUTES_IN_WEEK},
                                            }, 120)};
        const TROHours& hrs = *roHrs;

        UNIT_ASSERT_EQUAL(GetMinutesToOpen(MakeTime(EDay::MONDAY, 0, 0), hrs), 0);
        UNIT_ASSERT_EQUAL(GetMinutesToOpen(MakeTime(EDay::THURSDAY, 13, 30), hrs), 0);
        UNIT_ASSERT_EQUAL(GetMinutesToOpen(MakeTime(EDay::SUNDAY, 22, 00), hrs), 0);
        UNIT_ASSERT_EQUAL(GetMinutesToOpen(MakeTime(EDay::SUNDAY, 22, 01), hrs), 0);
        UNIT_ASSERT_EQUAL(GetMinutesToOpen(MakeTime(EDay::SUNDAY, 23, 59), hrs), 0);
    }

    Y_UNIT_TEST(TestGetMinutesToOpen2) {
        TROHoursHolder roHrs{MakeProtoHours({
                                                {MINUTES_IN_DAY * 1 + MINUTES_IN_HOUR * 8, MINUTES_IN_DAY * 1 + MINUTES_IN_HOUR * 14},
                                                {MINUTES_IN_DAY * 2 + MINUTES_IN_HOUR * 14, MINUTES_IN_DAY * 3 + MINUTES_IN_HOUR * 2},
                                            }, 120)};
        const TROHours& hrs = *roHrs;

        UNIT_ASSERT_EQUAL(GetMinutesToOpen(MakeTime(EDay::MONDAY, 12, 0), hrs), 1080);
        UNIT_ASSERT_EQUAL(GetMinutesToOpen(MakeTime(EDay::TUESDAY, 05, 59), hrs), 1);
        UNIT_ASSERT_EQUAL(GetMinutesToOpen(MakeTime(EDay::TUESDAY, 06, 00), hrs), 0);
        UNIT_ASSERT_EQUAL(GetMinutesToOpen(MakeTime(EDay::TUESDAY, 12, 00), hrs), 0);
        UNIT_ASSERT_EQUAL(GetMinutesToOpen(MakeTime(EDay::TUESDAY, 12, 01), hrs), 1439);
        UNIT_ASSERT_EQUAL(GetMinutesToOpen(MakeTime(EDay::THURSDAY, 00, 00), hrs), 0);
        UNIT_ASSERT_EQUAL(GetMinutesToOpen(MakeTime(EDay::THURSDAY, 00, 01), hrs), 7559);
    }

    Y_UNIT_TEST(TestGetMinutesToClose1) {
        TROHoursHolder roHrs{MakeProtoHours({
                                                {0, MINUTES_IN_WEEK},
                                            }, 120)};
        const TROHours& hrs = *roHrs;

        UNIT_ASSERT(GetMinutesToClose(MakeTime(EDay::MONDAY, 0, 0), hrs) >= static_cast<int>(MINUTES_IN_WEEK));
        UNIT_ASSERT(GetMinutesToClose(MakeTime(EDay::THURSDAY, 13, 30), hrs) >= static_cast<int>(MINUTES_IN_WEEK));
        UNIT_ASSERT(GetMinutesToClose(MakeTime(EDay::SUNDAY, 22, 00), hrs) >= static_cast<int>(MINUTES_IN_WEEK));
        UNIT_ASSERT(GetMinutesToClose(MakeTime(EDay::SUNDAY, 22, 01), hrs) >= static_cast<int>(MINUTES_IN_WEEK));
        UNIT_ASSERT(GetMinutesToClose(MakeTime(EDay::SUNDAY, 23, 59), hrs) >= static_cast<int>(MINUTES_IN_WEEK));
    }

    Y_UNIT_TEST(TestGetMinutesToClose2) {
        TROHoursHolder roHrs{MakeProtoHours({
                                                {MINUTES_IN_DAY * 1 + MINUTES_IN_HOUR * 8, MINUTES_IN_DAY * 1 + MINUTES_IN_HOUR * 14},
                                                {MINUTES_IN_DAY * 2 + MINUTES_IN_HOUR * 14, MINUTES_IN_DAY * 3 + MINUTES_IN_HOUR * 2},
                                            }, 120)};
        const TROHours& hrs = *roHrs;

        UNIT_ASSERT_EQUAL(GetMinutesToClose(MakeTime(EDay::MONDAY, 12, 00), hrs), 0);
        UNIT_ASSERT_EQUAL(GetMinutesToClose(MakeTime(EDay::TUESDAY, 06, 00), hrs), 360);
        UNIT_ASSERT_EQUAL(GetMinutesToClose(MakeTime(EDay::TUESDAY, 10, 00), hrs), 120);
        UNIT_ASSERT_EQUAL(GetMinutesToClose(MakeTime(EDay::TUESDAY, 12, 00), hrs), 0);
        UNIT_ASSERT_EQUAL(GetMinutesToClose(MakeTime(EDay::TUESDAY, 12, 01), hrs), 0);
        UNIT_ASSERT_EQUAL(GetMinutesToClose(MakeTime(EDay::WEDNESDAY, 10, 05), hrs), 0);
        UNIT_ASSERT_EQUAL(GetMinutesToClose(MakeTime(EDay::WEDNESDAY, 14, 05), hrs), 595);
        UNIT_ASSERT_EQUAL(GetMinutesToClose(MakeTime(EDay::THURSDAY, 00, 01), hrs), 0);
    }

    Y_UNIT_TEST(TestGetMinutesToClose3) {
        TROHoursHolder roHrs{MakeProtoHours({
                                                {MINUTES_IN_DAY * 1, MINUTES_IN_DAY * 2},
                                                {MINUTES_IN_DAY * 2, MINUTES_IN_DAY * 3},
                                                {MINUTES_IN_DAY * 3, MINUTES_IN_DAY * 4},
                                            }, 120)};
        const TROHours& hrs = *roHrs;

        UNIT_ASSERT_EQUAL(GetMinutesToClose(MakeTime(EDay::MONDAY, 0, 0), hrs), 0);
        UNIT_ASSERT_EQUAL(GetMinutesToClose(MakeTime(EDay::TUESDAY, 13, 30), hrs), 3390);
        UNIT_ASSERT_EQUAL(GetMinutesToClose(MakeTime(EDay::WEDNESDAY, 13, 30), hrs), 1950);
        UNIT_ASSERT_EQUAL(GetMinutesToClose(MakeTime(EDay::THURSDAY, 13, 30), hrs), 510);
        UNIT_ASSERT_EQUAL(GetMinutesToClose(MakeTime(EDay::SUNDAY, 22, 00), hrs), 0);
    }

    Y_UNIT_TEST(TestGetMinutesToClose4) {
        TROHoursHolder roHrs{MakeProtoHours({
                                                {0, MINUTES_IN_HOUR * 2},
                                                {MINUTES_IN_DAY * 6 + MINUTES_IN_HOUR * 22, MINUTES_IN_DAY * 6 + MINUTES_IN_HOUR * 24},
                                            }, 0)};
        const TROHours& hrs = *roHrs;

        UNIT_ASSERT_EQUAL(GetMinutesToClose(MakeTime(EDay::MONDAY, 0, 0), hrs), 120);
        UNIT_ASSERT_EQUAL(GetMinutesToClose(MakeTime(EDay::TUESDAY, 13, 30), hrs), 0);
        UNIT_ASSERT_EQUAL(GetMinutesToClose(MakeTime(EDay::SUNDAY, 21, 59), hrs), 0);
        UNIT_ASSERT_EQUAL(GetMinutesToClose(MakeTime(EDay::SUNDAY, 22, 00), hrs), 240);
    }

    Y_UNIT_TEST(TestTwoFullDays) {
        TTimeIntervalSet intervals;
        intervals.Add(TTime(MONDAY, Parse("00:00")), TTime(TUESDAY, Parse("24:00")));

        TProtoHours hrs = ToProtoHours(intervals, 0);
        UNIT_ASSERT_EQUAL(hrs.IntervalsSize(), 1);
        UNIT_ASSERT_EQUAL(hrs.GetIntervals(0).GetFrom(), 0);
        UNIT_ASSERT_EQUAL(hrs.GetIntervals(0).GetTo(), MINUTES_IN_DAY * 2);
    }
}

#include "helpers.h"

#include <extsearch/geo/kernel/working_hours/timeinterval_mask.h>

#include <library/cpp/testing/unittest/registar.h>

#include <util/string/cast.h>

using namespace NGeosearch;
using namespace NGeosearch::NWorkingHours;

Y_UNIT_TEST_SUITE(TTimeTest) {
    Y_UNIT_TEST(TestTimeIO) {
        UNIT_ASSERT_EQUAL(FromString<TTimeOfDay>("0:00").GetMinutesFromDayStart(), 0);
        UNIT_ASSERT_EQUAL(FromString<TTimeOfDay>("00:00").GetMinutesFromDayStart(), 0);
        UNIT_ASSERT_EQUAL(FromString<TTimeOfDay>("8:35").GetMinutesFromDayStart(), 515);
        UNIT_ASSERT_EQUAL(FromString<TTimeOfDay>("08:35").GetMinutesFromDayStart(), 515);

        UNIT_ASSERT_EQUAL(FromString<TTimeOfDay>("20:35:00").GetMinutesFromDayStart(), 1235);
        UNIT_ASSERT_EQUAL(FromString<TTimeOfDay>("20:35:50").GetMinutesFromDayStart(), 1235);
        UNIT_ASSERT_EQUAL(FromString<TTimeOfDay>("20:35").GetMinutesFromDayStart(), 1235);

        UNIT_ASSERT_EXCEPTION(FromString<TTimeOfDay>("20:15b00").GetMinutesFromDayStart(), yexception);
        UNIT_ASSERT_EXCEPTION(FromString<TTimeOfDay>("20:35:00b").GetMinutesFromDayStart(), yexception);
        UNIT_ASSERT_EXCEPTION(FromString<TTimeOfDay>("20:63:00").GetMinutesFromDayStart(), yexception);
        UNIT_ASSERT_EXCEPTION(FromString<TTimeOfDay>("20:03:00:31").GetMinutesFromDayStart(), yexception);

        UNIT_ASSERT_EQUAL(ToString(TTimeOfDay(20, 30)), "20:30");
    }

    Y_UNIT_TEST(TestTimeConstructors) {
        UNIT_ASSERT_EQUAL(TTimeOfDay(12, 30).GetMinutesFromDayStart(), 750);
        UNIT_ASSERT_EQUAL(NWorkingHours::TTime(TUESDAY, TTimeOfDay(12, 30)).GetMinutesFromWeekStart(), 2190);
        UNIT_ASSERT_EQUAL(NWorkingHours::TTime(TUESDAY, 12, 30).GetMinutesFromWeekStart(), 2190);

        UNIT_ASSERT_NO_EXCEPTION(TTimeOfDay(24, 00));
        UNIT_ASSERT_EXCEPTION(TTimeOfDay(24, 01), yexception);
        UNIT_ASSERT_EXCEPTION(TTimeOfDay(12, 70), yexception);

        UNIT_ASSERT_EXCEPTION(TTimeInterval(NWorkingHours::TTime(SUNDAY, TTimeOfDay(12, 30)), NWorkingHours::TTime(MONDAY, TTimeOfDay(14, 30))), yexception);
        UNIT_ASSERT_NO_EXCEPTION(TTimeInterval(NWorkingHours::TTime(SUNDAY, TTimeOfDay(12, 30)), NWorkingHours::TTime(NEXT_WEEK_FIRST_DAY, TTimeOfDay(14, 30))));
    }

    Y_UNIT_TEST(TestWrapping) {
        auto t1 = TTime(SUNDAY, TTimeOfDay(23, 58));

        auto t2 = TTime(NEXT_WEEK_FIRST_DAY, TTimeOfDay(0, 1));
        UNIT_ASSERT_EQUAL(t2.GetDay(), NEXT_WEEK_FIRST_DAY);
        UNIT_ASSERT_EQUAL(t2.Wrap(), TTime(MONDAY, TTimeOfDay(0, 1)));

        auto t3 = TTime(NEXT_WEEK_FIRST_DAY, TTimeOfDay(0, 2));

        UNIT_ASSERT_NO_EXCEPTION(TTimeInterval(t1, t2));
        UNIT_ASSERT_EXCEPTION(TTimeInterval(t2, t3), yexception);

        UNIT_ASSERT_NO_EXCEPTION(TTime(NEXT_WEEK_FIRST_DAY, TTimeOfDay(23, 59)));
        UNIT_ASSERT_EXCEPTION(TTime(NEXT_WEEK_FIRST_DAY, TTimeOfDay(24, 0)), yexception);
    }

    Y_UNIT_TEST(TestDaySequence) {
        UNIT_ASSERT_EQUAL(Next(MONDAY), TUESDAY);
        UNIT_ASSERT_EQUAL(Next(SUNDAY), MONDAY);

        UNIT_ASSERT_EQUAL(Previous(FRIDAY), THURSDAY);
        UNIT_ASSERT_EQUAL(Previous(MONDAY), SUNDAY);
    }
}

#include "helpers.h"

#include <extsearch/geo/kernel/working_hours/status.h>
#include <extsearch/geo/kernel/working_hours/timeinterval_set.h>

#include <library/cpp/testing/unittest/registar.h>

using namespace NGeosearch::NWorkingHours;

Y_UNIT_TEST_SUITE(TStatusTest) {
    Y_UNIT_TEST(TestUsual) {
        TTimeIntervalSet ts;
        ts.UniteWith(MakeIntervalSet(TOD("9:00"), TOD("13:00"), WEEKDAYS));
        ts.UniteWith(MakeIntervalSet(TOD("14:00"), TOD("18:00"), WEEKDAYS));

        const TTimeIntervalSeq seq = ts.GetIntervalSeq();
        {
            TStatus st = GetStatus(seq, seq, 1, TTime(MONDAY, TOD("8:00")), {});
            UNIT_ASSERT_VALUES_EQUAL(st.Current, EStatus::Closed);
            UNIT_ASSERT(st.NextChange);
            UNIT_ASSERT_VALUES_EQUAL(st.NextChange->Time, TTime(MONDAY, TOD("9:00")));
            UNIT_ASSERT_VALUES_EQUAL(st.NextChange->MinutesLeft, 60);
            UNIT_ASSERT_VALUES_EQUAL(st.NextChange->WeeksAfterCurrent, 0);
        }
        {
            TStatus st = GetStatus(seq, seq, 1, TTime(MONDAY, TOD("8:59")), {});
            UNIT_ASSERT_VALUES_EQUAL(st.Current, EStatus::Closed);
            UNIT_ASSERT(st.NextChange);
            UNIT_ASSERT_VALUES_EQUAL(st.NextChange->MinutesLeft, 1);
            UNIT_ASSERT_VALUES_EQUAL(st.NextChange->WeeksAfterCurrent, 0);
        }
        {
            TStatus st = GetStatus(seq, seq, 1, TTime(MONDAY, TOD("9:00")), {});
            UNIT_ASSERT_VALUES_EQUAL(st.Current, EStatus::Open);
            UNIT_ASSERT(st.NextChange);
            UNIT_ASSERT_VALUES_EQUAL(st.NextChange->MinutesLeft, 9 * MINUTES_IN_HOUR);
            UNIT_ASSERT_VALUES_EQUAL(st.NextChange->Time, TTime(MONDAY, TOD("18:00")));
            UNIT_ASSERT_VALUES_EQUAL(st.NextChange->WeeksAfterCurrent, 0);

            UNIT_ASSERT(st.Break);
            UNIT_ASSERT_VALUES_EQUAL(st.Break->From.MinutesLeft, 4 * MINUTES_IN_HOUR);
            UNIT_ASSERT_VALUES_EQUAL(st.Break->From.Time, TTime(MONDAY, TOD("13:00")));
            UNIT_ASSERT_VALUES_EQUAL(st.Break->To.MinutesLeft, 5 * MINUTES_IN_HOUR);
            UNIT_ASSERT_VALUES_EQUAL(st.Break->To.Time, TTime(MONDAY, TOD("14:00")));
        }
        {
            TStatus st = GetStatus(seq, seq, 1, TTime(MONDAY, TOD("13:00")), {});
            UNIT_ASSERT_VALUES_EQUAL(st.Current, EStatus::Break);
            UNIT_ASSERT(st.NextChange);
            UNIT_ASSERT_VALUES_EQUAL(st.NextChange->MinutesLeft, 5 * MINUTES_IN_HOUR);
            UNIT_ASSERT_VALUES_EQUAL(st.NextChange->Time, TTime(MONDAY, TOD("18:00")));
            UNIT_ASSERT_VALUES_EQUAL(st.NextChange->WeeksAfterCurrent, 0);

            UNIT_ASSERT(st.Break);
            UNIT_ASSERT_VALUES_EQUAL(st.Break->From.MinutesLeft, 0);
            UNIT_ASSERT_VALUES_EQUAL(st.Break->From.Time, TTime(MONDAY, TOD("13:00")));
            UNIT_ASSERT_VALUES_EQUAL(st.Break->To.MinutesLeft, MINUTES_IN_HOUR);
            UNIT_ASSERT_VALUES_EQUAL(st.Break->To.Time, TTime(MONDAY, TOD("14:00")));
        }
        {
            TStatus st = GetStatus(seq, seq, 1, TTime(MONDAY, TOD("14:00")), {});
            UNIT_ASSERT_VALUES_EQUAL(st.Current, EStatus::Open);
            UNIT_ASSERT(st.NextChange);
            UNIT_ASSERT_VALUES_EQUAL(st.NextChange->MinutesLeft, 4 * MINUTES_IN_HOUR);
            UNIT_ASSERT_VALUES_EQUAL(st.NextChange->Time, TTime(MONDAY, TOD("18:00")));
            UNIT_ASSERT_VALUES_EQUAL(st.NextChange->WeeksAfterCurrent, 0);

            UNIT_ASSERT(!st.Break);
        }
        {
            TStatus st = GetStatus(seq, seq, 1, TTime(MONDAY, TOD("18:01")), {});
            UNIT_ASSERT_VALUES_EQUAL(st.Current, EStatus::Closed);
            UNIT_ASSERT(st.NextChange);
            UNIT_ASSERT_VALUES_EQUAL(st.NextChange->MinutesLeft, 15 * MINUTES_IN_HOUR - 1);
            UNIT_ASSERT_VALUES_EQUAL(st.NextChange->Time, TTime(TUESDAY, TOD("9:00")));
            UNIT_ASSERT_VALUES_EQUAL(st.NextChange->WeeksAfterCurrent, 0);

            UNIT_ASSERT(!st.Break);
        }
        {
            TStatus st = GetStatus(seq, seq, 1, TTime(FRIDAY, TOD("18:02")), {});
            UNIT_ASSERT_VALUES_EQUAL(st.Current, EStatus::Closed);
            UNIT_ASSERT(st.NextChange);
            UNIT_ASSERT_VALUES_EQUAL(st.NextChange->MinutesLeft, 2 * MINUTES_IN_DAY + 15 * MINUTES_IN_HOUR - 2);
            UNIT_ASSERT_VALUES_EQUAL(st.NextChange->Time, TTime(MONDAY, TOD("9:00")));
            UNIT_ASSERT_VALUES_EQUAL(st.NextChange->WeeksAfterCurrent, 1);

            UNIT_ASSERT(!st.Break);
        }
    }

    Y_UNIT_TEST(TestAlways) {
        TTimeIntervalSet ts = MakeIntervalSet(TOD("0:00"), TOD("24:00"), EVERYDAY);

        const TTimeIntervalSeq seq = ts.GetIntervalSeq();
        {
            TStatus st = GetStatus(seq, seq, 1, TTime(MONDAY, TOD("00:00")), {});
            UNIT_ASSERT_VALUES_EQUAL(st.Current, EStatus::Open);
            UNIT_ASSERT(!st.NextChange);
            UNIT_ASSERT(!st.Break);
        }
        {
            TStatus st = GetStatus(seq, seq, 1, TTime(MONDAY, TOD("10:00")), {});
            UNIT_ASSERT_VALUES_EQUAL(st.Current, EStatus::Open);
            UNIT_ASSERT(!st.NextChange);
            UNIT_ASSERT(!st.Break);
        }
        {
            TStatus st = GetStatus(seq, seq, 1, TTime(SUNDAY, TOD("23:59")), {});
            UNIT_ASSERT_VALUES_EQUAL(st.Current, EStatus::Open);
            UNIT_ASSERT(!st.NextChange);
            UNIT_ASSERT(!st.Break);
        }
    }

    Y_UNIT_TEST(TestTwoBreaks) {
        TTimeIntervalSet ts;
        ts.UniteWith(MakeIntervalSet(TOD("10:00"), TOD("13:00"), WEEKDAYS));
        ts.UniteWith(MakeIntervalSet(TOD("14:00"), TOD("17:00"), WEEKDAYS));
        ts.UniteWith(MakeIntervalSet(TOD("18:00"), TOD("21:00"), WEEKDAYS));

        const TTimeIntervalSeq seq = ts.GetIntervalSeq();
        {
            TStatus st = GetStatus(seq, seq, 1, TTime(WEDNESDAY, TOD("11:00")), {});
            UNIT_ASSERT_VALUES_EQUAL(st.Current, EStatus::Open);
            UNIT_ASSERT(st.NextChange);
            UNIT_ASSERT_VALUES_EQUAL(st.NextChange->MinutesLeft, 10 * MINUTES_IN_HOUR);
            UNIT_ASSERT_VALUES_EQUAL(st.NextChange->Time, TTime(WEDNESDAY, TOD("21:00")));
            UNIT_ASSERT_VALUES_EQUAL(st.NextChange->WeeksAfterCurrent, 0);

            UNIT_ASSERT(st.Break);
            UNIT_ASSERT_VALUES_EQUAL(st.Break->From.MinutesLeft, 2 * MINUTES_IN_HOUR);
            UNIT_ASSERT_VALUES_EQUAL(st.Break->From.Time, TTime(WEDNESDAY, TOD("13:00")));
            UNIT_ASSERT_VALUES_EQUAL(st.Break->To.MinutesLeft, 3 * MINUTES_IN_HOUR);
            UNIT_ASSERT_VALUES_EQUAL(st.Break->To.Time, TTime(WEDNESDAY, TOD("14:00")));
        }
        {
            TStatus st = GetStatus(seq, seq, 1, TTime(WEDNESDAY, TOD("13:30")), {});
            UNIT_ASSERT_VALUES_EQUAL(st.Current, EStatus::Break);
            UNIT_ASSERT(st.NextChange);
            UNIT_ASSERT_VALUES_EQUAL(st.NextChange->MinutesLeft, 7 * MINUTES_IN_HOUR + 30);
            UNIT_ASSERT_VALUES_EQUAL(st.NextChange->Time, TTime(WEDNESDAY, TOD("21:00")));
            UNIT_ASSERT_VALUES_EQUAL(st.NextChange->WeeksAfterCurrent, 0);

            UNIT_ASSERT(st.Break);
            UNIT_ASSERT_VALUES_EQUAL(st.Break->From.Time, TTime(WEDNESDAY, TOD("13:00")));
            UNIT_ASSERT_VALUES_EQUAL(st.Break->To.MinutesLeft, 30);
            UNIT_ASSERT_VALUES_EQUAL(st.Break->To.Time, TTime(WEDNESDAY, TOD("14:00")));
        }
        {
            TStatus st = GetStatus(seq, seq, 1, TTime(THURSDAY, TOD("15:00")), {});
            UNIT_ASSERT_VALUES_EQUAL(st.Current, EStatus::Open);
            UNIT_ASSERT(st.NextChange);
            UNIT_ASSERT_VALUES_EQUAL(st.NextChange->MinutesLeft, 6 * MINUTES_IN_HOUR);
            UNIT_ASSERT_VALUES_EQUAL(st.NextChange->Time, TTime(THURSDAY, TOD("21:00")));
            UNIT_ASSERT_VALUES_EQUAL(st.NextChange->WeeksAfterCurrent, 0);

            UNIT_ASSERT(st.Break);
            UNIT_ASSERT_VALUES_EQUAL(st.Break->From.MinutesLeft, 2 * MINUTES_IN_HOUR);
            UNIT_ASSERT_VALUES_EQUAL(st.Break->From.Time, TTime(THURSDAY, TOD("17:00")));
            UNIT_ASSERT_VALUES_EQUAL(st.Break->To.MinutesLeft, 3 * MINUTES_IN_HOUR);
            UNIT_ASSERT_VALUES_EQUAL(st.Break->To.Time, TTime(THURSDAY, TOD("18:00")));
        }
        {
            TStatus st = GetStatus(seq, seq, 1, TTime(FRIDAY, TOD("17:30")), {});
            UNIT_ASSERT_VALUES_EQUAL(st.Current, EStatus::Break);
            UNIT_ASSERT(st.NextChange);
            UNIT_ASSERT_VALUES_EQUAL(st.NextChange->MinutesLeft, 3 * MINUTES_IN_HOUR + 30);
            UNIT_ASSERT_VALUES_EQUAL(st.NextChange->Time, TTime(FRIDAY, TOD("21:00")));
            UNIT_ASSERT_VALUES_EQUAL(st.NextChange->WeeksAfterCurrent, 0);

            UNIT_ASSERT(st.Break);
            UNIT_ASSERT_VALUES_EQUAL(st.Break->From.Time, TTime(FRIDAY, TOD("17:00")));
            UNIT_ASSERT_VALUES_EQUAL(st.Break->To.Time, TTime(FRIDAY, TOD("18:00")));
        }
        {
            TStatus st = GetStatus(seq, seq, 1, TTime(FRIDAY, TOD("20:59")), {});
            UNIT_ASSERT_VALUES_EQUAL(st.Current, EStatus::Open);
            UNIT_ASSERT(st.NextChange);
            UNIT_ASSERT_VALUES_EQUAL(st.NextChange->MinutesLeft, 1);
            UNIT_ASSERT_VALUES_EQUAL(st.NextChange->Time, TTime(FRIDAY, TOD("21:00")));
            UNIT_ASSERT_VALUES_EQUAL(st.NextChange->WeeksAfterCurrent, 0);

            UNIT_ASSERT(!st.Break);
        }
    }

    Y_UNIT_TEST(TestStrangeBreak) {
        TTimeIntervalSet ts;
        ts.Add(TTime(MONDAY, TOD("0:15")), TTime(SUNDAY, TOD("23:45")));

        const TTimeIntervalSeq seq = ts.GetIntervalSeq();
        {
            TStatus st = GetStatus(seq, seq, 1, TTime(MONDAY, TOD("00:10")), {});
            UNIT_ASSERT_VALUES_EQUAL(st.Current, EStatus::Break);
            UNIT_ASSERT(!st.NextChange);
            UNIT_ASSERT(st.Break);
            UNIT_ASSERT_VALUES_EQUAL(st.Break->From.MinutesLeft, 0);
            UNIT_ASSERT_VALUES_EQUAL(st.Break->From.Time, TTime(SUNDAY, TOD("23:45")));
            UNIT_ASSERT_VALUES_EQUAL(st.Break->To.MinutesLeft, 5);
            UNIT_ASSERT_VALUES_EQUAL(st.Break->To.Time, TTime(MONDAY, TOD("00:15")));
        }
        {
            TStatus st = GetStatus(seq, seq, 1, TTime(SUNDAY, TOD("23:45")), {});
            UNIT_ASSERT_VALUES_EQUAL(st.Current, EStatus::Break);
            UNIT_ASSERT(!st.NextChange);
            UNIT_ASSERT(st.Break);
            UNIT_ASSERT_VALUES_EQUAL(st.Break->From.Time, TTime(SUNDAY, TOD("23:45")));
            UNIT_ASSERT_VALUES_EQUAL(st.Break->To.MinutesLeft, 30);
            UNIT_ASSERT_VALUES_EQUAL(st.Break->To.Time, TTime(MONDAY, TOD("00:15")));
        }
        {
            TStatus st = GetStatus(seq, seq, 1, TTime(SUNDAY, TOD("23:40")), {});
            UNIT_ASSERT_VALUES_EQUAL(st.Current, EStatus::Open);
            UNIT_ASSERT(!st.NextChange);
            UNIT_ASSERT(st.Break);
            UNIT_ASSERT_VALUES_EQUAL(st.Break->From.MinutesLeft, 5);
            UNIT_ASSERT_VALUES_EQUAL(st.Break->From.Time, TTime(SUNDAY, TOD("23:45")));
            UNIT_ASSERT_VALUES_EQUAL(st.Break->To.MinutesLeft, 35);
            UNIT_ASSERT_VALUES_EQUAL(st.Break->To.Time, TTime(MONDAY, TOD("00:15")));
        }
        {
            TStatus st = GetStatus(seq, seq, 1, TTime(TUESDAY, TOD("12:00")), {});
            UNIT_ASSERT_VALUES_EQUAL(st.Current, EStatus::Open);
            UNIT_ASSERT(!st.NextChange);
            UNIT_ASSERT(!st.Break);
        }
        {
            TStatus st = GetStatus(seq, seq, 1, TTime(SUNDAY, TOD("12:00")), {});
            UNIT_ASSERT_VALUES_EQUAL(st.Current, EStatus::Open);
            UNIT_ASSERT(!st.NextChange);
            UNIT_ASSERT(st.Break);
            UNIT_ASSERT_VALUES_EQUAL(st.Break->From.Time, TTime(SUNDAY, TOD("23:45")));
            UNIT_ASSERT_VALUES_EQUAL(st.Break->To.Time, TTime(MONDAY, TOD("00:15")));
        }
    }

    Y_UNIT_TEST(TestStrangeBreakWithHolidays) {
        TTimeIntervalSet ts1;
        ts1.Add(TTime(MONDAY, TOD("0:15")), TTime(SUNDAY, TOD("23:45")));
        const TTimeIntervalSeq seq1 = ts1.GetIntervalSeq();
        TTimeIntervalSet ts2;
        ts2.Add(TTime(MONDAY, TOD("9:00")), TTime(MONDAY, TOD("18:00")));
        const TTimeIntervalSeq seq2 = ts2.GetIntervalSeq();
        {
            TStatus st = GetStatus(seq1, {}, 1, TTime(MONDAY, TOD("00:10")), {});
            UNIT_ASSERT_VALUES_EQUAL(st.Current, EStatus::Break);
            UNIT_ASSERT(!st.NextChange);
            UNIT_ASSERT(st.Break);
            UNIT_ASSERT_VALUES_EQUAL(st.Break->From.MinutesLeft, 0);
            UNIT_ASSERT_VALUES_EQUAL(st.Break->From.Time, TTime(SUNDAY, TOD("23:45")));
            UNIT_ASSERT_VALUES_EQUAL(st.Break->To.MinutesLeft, 5);
            UNIT_ASSERT_VALUES_EQUAL(st.Break->To.Time, TTime(MONDAY, TOD("00:15")));
        }
        {
            TStatus st = GetStatus(seq1, seq2, 1, TTime(MONDAY, TOD("00:10")), {});
            UNIT_ASSERT_VALUES_EQUAL(st.Current, EStatus::Break);
            UNIT_ASSERT(st.Break);
            UNIT_ASSERT_VALUES_EQUAL(st.Break->From.MinutesLeft, 0);
            UNIT_ASSERT_VALUES_EQUAL(st.Break->From.Time, TTime(SUNDAY, TOD("23:45")));
            UNIT_ASSERT_VALUES_EQUAL(st.Break->To.MinutesLeft, 5);
            UNIT_ASSERT_VALUES_EQUAL(st.Break->To.Time, TTime(MONDAY, TOD("00:15")));
            UNIT_ASSERT(st.NextChange);
            UNIT_ASSERT_VALUES_EQUAL(st.NextChange->MinutesLeft, MINUTES_IN_WEEK - 25);
            UNIT_ASSERT_VALUES_EQUAL(st.NextChange->Time, TTime(SUNDAY, TOD("23:45")));
            UNIT_ASSERT_VALUES_EQUAL(st.NextChange->WeeksAfterCurrent, 0);
        }
    }

    Y_UNIT_TEST(TestWorksAlmostAllWeek) {
        TTimeIntervalSet ts;
        ts.UniteWith(MakeIntervalSet(TOD("09:00"), TOD("24:00"), {MONDAY}));
        ts.UniteWith(MakeIntervalSet(TOD("00:00"), TOD("24:00"), {TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY}));

        const TTimeIntervalSeq seq = ts.GetIntervalSeq();
        {
            TStatus st = GetStatus(seq, seq, 1, TTime(MONDAY, TOD("10:00")), {});
            UNIT_ASSERT_VALUES_EQUAL(st.Current, EStatus::Open);
            UNIT_ASSERT(st.NextChange);
            UNIT_ASSERT_VALUES_EQUAL(st.NextChange->MinutesLeft, 14 * MINUTES_IN_HOUR + 6 * MINUTES_IN_DAY);
            UNIT_ASSERT_VALUES_EQUAL(st.NextChange->Time, TTime(SUNDAY, TOD("24:00")));
            UNIT_ASSERT_VALUES_EQUAL(st.NextChange->Time, TTime(NEXT_WEEK_FIRST_DAY, TOD("00:00")));
            UNIT_ASSERT_VALUES_EQUAL(st.NextChange->WeeksAfterCurrent, 0);
        }
    }

    Y_UNIT_TEST(TestClosed) {
        TTimeIntervalSet ts;
        ts.UniteWith(MakeIntervalSet(TOD("09:00"), TOD("18:00"), EVERYDAY));
        const TTimeIntervalSeq seq = ts.GetIntervalSeq();
        {
            TClosedInfo info;
            info.IsClosedPermanently = true;
            TStatus st = GetStatus(seq, seq, 1, TTime(MONDAY, TOD("10:00")), info);
            UNIT_ASSERT_VALUES_EQUAL(st.Current, EStatus::ClosedPermanently);
            UNIT_ASSERT(!st.NextChange);
            UNIT_ASSERT(!st.Break);
        }
        {
            TClosedInfo info;
            info.IsClosedTemporarily = true;
            info.IsUnreliable = true;
            TStatus st = GetStatus(seq, seq, 1, TTime(MONDAY, TOD("10:00")), info);
            UNIT_ASSERT_VALUES_EQUAL(st.Current, EStatus::ClosedTemporarily);
        }
        {
            TClosedInfo info;
            info.IsUnreliable = true;
            TStatus st = GetStatus(seq, seq, 1, TTime(MONDAY, TOD("10:00")), info);
            UNIT_ASSERT_VALUES_EQUAL(st.Current, EStatus::Unreliable);
        }
    }

    Y_UNIT_TEST(TestPassMidnight) {
        TTimeIntervalSet ts;
        ts.UniteWith(MakeIntervalSet(TOD("14:00"), TOD("05:00"), EVERYDAY));

        const TTimeIntervalSeq seq = ts.GetIntervalSeq();
        for (EDay day : EVERYDAY) {
            TStatus st = GetStatus(seq, seq, 1, TTime(day, TOD("01:30")), {});
            UNIT_ASSERT_VALUES_EQUAL(st.Current, EStatus::Open);
            UNIT_ASSERT(st.NextChange);
            UNIT_ASSERT_VALUES_EQUAL(st.NextChange->MinutesLeft, 3 * MINUTES_IN_HOUR + 30);
            UNIT_ASSERT_VALUES_EQUAL(st.NextChange->WeeksAfterCurrent, 0);
        }
    }

    Y_UNIT_TEST(TestPassMidnight2) {
        TTimeIntervalSet ts;
        ts.UniteWith(MakeIntervalSet(TOD("00:00"), TOD("06:00"), EVERYDAY));
        ts.UniteWith(MakeIntervalSet(TOD("12:00"), TOD("24:00"), EVERYDAY));

        const TTimeIntervalSeq seq = ts.GetIntervalSeq();
        for (EDay day : EVERYDAY) {
            TStatus st = GetStatus(seq, seq, 1, TTime(day, TOD("05:30")), {});
            UNIT_ASSERT_VALUES_EQUAL(st.Current, EStatus::Open);
            UNIT_ASSERT(st.NextChange);
            UNIT_ASSERT_VALUES_EQUAL(st.NextChange->MinutesLeft, 30);
            UNIT_ASSERT_EQUAL(st.NextChange->Time, TTime(day, 6, 0));
            UNIT_ASSERT_VALUES_EQUAL(st.NextChange->WeeksAfterCurrent, 0);
        }
        for (EDay day : EVERYDAY) {
            TStatus st = GetStatus(seq, seq, 1, TTime(day, TOD("00:10")), {});
            UNIT_ASSERT_VALUES_EQUAL(st.Current, EStatus::Open);
            UNIT_ASSERT(st.NextChange);
            UNIT_ASSERT_VALUES_EQUAL(st.NextChange->MinutesLeft, 350);
            UNIT_ASSERT_EQUAL(st.NextChange->Time, TTime(day, 6, 0));
            UNIT_ASSERT_VALUES_EQUAL(st.NextChange->WeeksAfterCurrent, 0);
        }
        for (EDay day : EVERYDAY) {
            TStatus st = GetStatus(seq, seq, 1, TTime(day, TOD("23:59")), {});
            UNIT_ASSERT_VALUES_EQUAL(st.Current, EStatus::Open);
            UNIT_ASSERT(st.NextChange);
            UNIT_ASSERT_VALUES_EQUAL(st.NextChange->MinutesLeft, 361);
            UNIT_ASSERT_EQUAL(st.NextChange->Time, TTime(Next(day), 6, 0));
            UNIT_ASSERT_VALUES_EQUAL(st.NextChange->WeeksAfterCurrent, (day == SUNDAY ? 1 : 0));
        }
        for (EDay day : EVERYDAY) {
            TStatus st = GetStatus(seq, seq, 1, TTime(day, TOD("07:00")), {});
            UNIT_ASSERT_VALUES_EQUAL(st.Current, EStatus::Closed);
            UNIT_ASSERT(st.NextChange);
            UNIT_ASSERT_VALUES_EQUAL(st.NextChange->MinutesLeft, 300);
            UNIT_ASSERT_EQUAL(st.NextChange->Time, TTime(day, 12, 0));
            UNIT_ASSERT_VALUES_EQUAL(st.NextChange->WeeksAfterCurrent, 0);
        }
    }

    Y_UNIT_TEST(TestOpeningOnNextWeekOnlyByHoliday) {
        const TTimeIntervalSeq seq1;

        TTimeIntervalSet ts2;
        ts2.UniteWith(MakeIntervalSet(TOD("09:00"), TOD("18:00"), {MONDAY}));
        const TTimeIntervalSeq seq2 = ts2.GetIntervalSeq();

        TStatus st = GetStatus(seq1, seq2, 1, TTime(MONDAY, TOD("10:00")), {});
        UNIT_ASSERT_VALUES_EQUAL(st.Current, EStatus::Closed);
        UNIT_ASSERT(st.NextChange);
        UNIT_ASSERT_VALUES_EQUAL(st.NextChange->MinutesLeft, MINUTES_IN_WEEK - MINUTES_IN_HOUR);
        UNIT_ASSERT_EQUAL(st.NextChange->Time, TTime(MONDAY, 9, 0));
        UNIT_ASSERT_VALUES_EQUAL(st.NextChange->WeeksAfterCurrent, 1);
    }

    Y_UNIT_TEST(TestEndlessHolidaysFromNextWeek) {
        TTimeIntervalSet ts1;
        ts1.UniteWith(MakeIntervalSet(TOD("09:00"), TOD("18:00"), EVERYDAY));
        const TTimeIntervalSeq seq1 = ts1.GetIntervalSeq();

        const TTimeIntervalSeq seq2;
        {
            TStatus st = GetStatus(seq1, seq2, 1, TTime(SUNDAY, TOD("19:00")), {});
            UNIT_ASSERT_VALUES_EQUAL(st.Current, EStatus::Closed);
            UNIT_ASSERT(!st.NextChange);
        }
        {
            TStatus st = GetStatus(seq1, seq2, 1, TTime(SATURDAY, TOD("19:00")), {});
            UNIT_ASSERT_VALUES_EQUAL(st.Current, EStatus::Closed);
            UNIT_ASSERT(st.NextChange);
            UNIT_ASSERT_VALUES_EQUAL(st.NextChange->MinutesLeft, 14 * MINUTES_IN_HOUR);
            UNIT_ASSERT_EQUAL(st.NextChange->Time, TTime(SUNDAY, 9, 0));
            UNIT_ASSERT_VALUES_EQUAL(st.NextChange->WeeksAfterCurrent, 0);
        }
    }

    Y_UNIT_TEST(TestClosedFromWeekBegining) {
        TTimeIntervalSet ts;
        ts.UniteWith(MakeIntervalSet(TOD("09:00"), TOD("18:00"), EVERYDAY));
        const TTimeIntervalSeq seq = ts.GetIntervalSeq();

        TStatus st = GetStatus(seq, seq, 1, TTime(MONDAY, TOD("08:00")), {});
        UNIT_ASSERT_VALUES_EQUAL(st.Current, EStatus::Closed);
        UNIT_ASSERT_VALUES_EQUAL(st.NextChange->MinutesLeft, MINUTES_IN_HOUR);
        UNIT_ASSERT_EQUAL(st.NextChange->Time, TTime(MONDAY, 9, 0));
        UNIT_ASSERT_VALUES_EQUAL(st.NextChange->WeeksAfterCurrent, 0);
    }

    Y_UNIT_TEST(TestClosedForWholeWeeks) {
        TTimeIntervalSet ts;
        ts.UniteWith(MakeIntervalSet(TOD("09:00"), TOD("18:00"), EVERYDAY));
        const TTimeIntervalSeq seq = ts.GetIntervalSeq();

        TStatus st = GetStatus(seq, seq, 5, TTime(SUNDAY, TOD("19:00")), {});
        UNIT_ASSERT_VALUES_EQUAL(st.Current, EStatus::Closed);
        UNIT_ASSERT_VALUES_EQUAL(st.NextChange->MinutesLeft, MINUTES_IN_WEEK * 4 + 14 * MINUTES_IN_HOUR);
        UNIT_ASSERT_EQUAL(st.NextChange->Time, TTime(MONDAY, 9, 0));
        UNIT_ASSERT_VALUES_EQUAL(st.NextChange->WeeksAfterCurrent, 5);
    }

    Y_UNIT_TEST(TestPassMidnight3) {
        TTimeIntervalSet ts;
        ts.UniteWith(MakeIntervalSet(TOD("13:00"), TOD("01:00"), EVERYDAY));
        const TTimeIntervalSeq seq = ts.GetIntervalSeq();
        for (EDay day : EVERYDAY) {
            TStatus st = GetStatus(seq, seq, 1, TTime(day, TOD("23:50")), {});
            UNIT_ASSERT_VALUES_EQUAL(st.Current, EStatus::Open);
            UNIT_ASSERT(st.NextChange);
            Cerr << "Day " << static_cast<int>(day) << "\n";
            UNIT_ASSERT_VALUES_EQUAL(st.NextChange->MinutesLeft, 1 * MINUTES_IN_HOUR + 10);
            UNIT_ASSERT_VALUES_EQUAL(st.NextChange->WeeksAfterCurrent, day == SUNDAY ? 1 : 0);
        }
    }
}

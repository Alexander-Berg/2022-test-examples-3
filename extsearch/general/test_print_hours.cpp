#include <extsearch/geo/kernel/working_hours/print_hours.h>
#include <extsearch/geo/kernel/working_hours/timeinterval_set.h>

#include <extsearch/geo/kernel/xml_writer/xml.h>

#include <library/cpp/testing/unittest/registar.h>

#include <util/datetime/base.h>
#include <util/generic/array_ref.h>

using namespace NGeosearch::NWorkingHours;

Y_UNIT_TEST_SUITE(TPrintHoursTest) {
    Y_UNIT_TEST(TestSetToAvailabilities1) {
        TTimeIntervalSet intervals;
        intervals.Add(TTimeInterval::MakeFromMinutes(0, MINUTES_IN_WEEK));
        TAvailabilities a = TimeIntervalSetToAvailabilities(intervals);
        UNIT_ASSERT_EQUAL(a.size(), 1);
        UNIT_ASSERT_EQUAL(a[0].Days, EVERYDAY_MASK);
        UNIT_ASSERT_EQUAL(a[0].Intervals.size(), 1);
        UNIT_ASSERT(a[0].Intervals[0].IsFullDay());
    }

    Y_UNIT_TEST(TestHoursToAvailabilities2) {
        TTimeIntervalSet intervals;
        intervals.Add(TTimeInterval::MakeFromMinutes(MINUTES_IN_DAY * 1, MINUTES_IN_DAY * 3));
        intervals.Add(TTimeInterval::MakeFromMinutes(MINUTES_IN_DAY * 4, MINUTES_IN_DAY * 5));
        TAvailabilities a = TimeIntervalSetToAvailabilities(intervals);
        UNIT_ASSERT_EQUAL(a.size(), 1);
        UNIT_ASSERT_EQUAL(a[0].Days, TUESDAY_MASK | WEDNESDAY_MASK | FRIDAY_MASK);
        UNIT_ASSERT_EQUAL(a[0].Intervals.size(), 1);
        UNIT_ASSERT(a[0].Intervals[0].IsFullDay());
    }

    Y_UNIT_TEST(TestHoursToAvailabilities3) {
        TTimeIntervalSet intervals;
        intervals.Add(TTimeInterval::MakeFromMinutes(MINUTES_IN_DAY * 1 + MINUTES_IN_HOUR * 8, MINUTES_IN_DAY * 1 + MINUTES_IN_HOUR * 14));
        intervals.Add(TTimeInterval::MakeFromMinutes(MINUTES_IN_DAY * 2 + MINUTES_IN_HOUR * 14, MINUTES_IN_DAY * 3 + MINUTES_IN_HOUR * 2));
        intervals.Add(TTimeInterval::MakeFromMinutes(MINUTES_IN_DAY * 6 + MINUTES_IN_HOUR * 14, MINUTES_IN_DAY * 7));
        intervals.Add(TTimeInterval::MakeFromMinutes(0, MINUTES_IN_HOUR * 2));
        TAvailabilities a = TimeIntervalSetToAvailabilities(intervals);
        UNIT_ASSERT_EQUAL(a.size(), 2);
        UNIT_ASSERT_EQUAL(a[0].Days, TUESDAY_MASK);
        UNIT_ASSERT_EQUAL(a[0].Intervals.size(), 1);
        UNIT_ASSERT_EQUAL(a[0].Intervals[0].GetFrom().GetMinutesFromWeekStart(), MINUTES_IN_HOUR * 8);
        UNIT_ASSERT_EQUAL(a[0].Intervals[0].GetTo().GetMinutesFromWeekStart(), MINUTES_IN_HOUR * 14);
        UNIT_ASSERT_EQUAL(a[1].Days, WEDNESDAY_MASK | SUNDAY_MASK);
        UNIT_ASSERT_EQUAL(a[1].Intervals.size(), 1);
        UNIT_ASSERT_EQUAL(a[1].Intervals[0].GetFrom().GetMinutesFromWeekStart(), MINUTES_IN_HOUR * 14);
        UNIT_ASSERT_EQUAL(a[1].Intervals[0].GetTo().GetMinutesFromWeekStart(), MINUTES_IN_DAY + MINUTES_IN_HOUR * 2);
    }

    static TString WorkingTimeXmlString(const TAvailabilities& a) {
        NXmlWr::TDocument doc{"a"};
        NXmlWr::TNode root = doc.Root();
        AvailabilitiesToXml(&root, a);
        return doc.Root().AsString(); // no pretty formatting
    }

    Y_UNIT_TEST(TestWorkingTimeXML1) {
        TAvailabilities a;
        a.push_back(TAvailability());
        a.back().Days = EVERYDAY_MASK;
        a.back().Intervals.push_back(TTimeInterval::FullDayInterval(EDay::FIRST_WEEK_DAY));

        UNIT_ASSERT_STRINGS_EQUAL(WorkingTimeXmlString(a), "<a><Availability><Everyday/><TwentyFourHours/></Availability></a>");
    }

    Y_UNIT_TEST(TestWorkingTimeXML2) {
        TAvailabilities a;
        a.push_back(TAvailability());
        a.back().Days = MONDAY_MASK | WEDNESDAY_MASK | THURSDAY_MASK;
        a.back().Intervals.push_back(TTimeInterval::FullDayInterval(EDay::FIRST_WEEK_DAY));

        UNIT_ASSERT_EQUAL(WorkingTimeXmlString(a), "<a><Availability><Monday/><Wednesday/><Thursday/><TwentyFourHours/></Availability></a>");
    }

    Y_UNIT_TEST(TestWorkingTimeXML3) {
        TAvailabilities a;
        a.push_back(TAvailability());
        a.back().Days = MONDAY_MASK | TUESDAY_MASK;
        a.back().Intervals.push_back(TTimeInterval::MakeFromMinutes(MINUTES_IN_HOUR * 8, MINUTES_IN_HOUR * 14 + 10));
        a.push_back(TAvailability());
        a.back().Days = THURSDAY_MASK | FRIDAY_MASK | SATURDAY_MASK;
        a.back().Intervals.push_back(TTimeInterval::MakeFromMinutes(MINUTES_IN_HOUR * 12, MINUTES_IN_HOUR * 15));

        UNIT_ASSERT_EQUAL(WorkingTimeXmlString(a), "<a><Availability><Monday/><Tuesday/><Interval from=\"08:00:00\" to=\"14:10:00\"/></Availability><Availability><Thursday/><Friday/><Saturday/><Interval from=\"12:00:00\" to=\"15:00:00\"/></Availability></a>");
    }

    Y_UNIT_TEST(TestWorkingTimeXML4) {
        TAvailabilities a;
        a.push_back(TAvailability());
        a.back().Days = TUESDAY_MASK;
        a.back().Intervals.push_back(TTimeInterval::MakeFromMinutes(MINUTES_IN_HOUR * 12, MINUTES_IN_HOUR * 20));
        a.back().Intervals.push_back(TTimeInterval::MakeFromMinutes(MINUTES_IN_HOUR * 22, MINUTES_IN_HOUR * 26));
        a.push_back(TAvailability());
        a.back().Days = WEDNESDAY_MASK;
        a.back().Intervals.push_back(TTimeInterval::MakeFromMinutes(MINUTES_IN_HOUR * 8, MINUTES_IN_HOUR * 10));
        a.back().Intervals.push_back(TTimeInterval::MakeFromMinutes(MINUTES_IN_HOUR * 10 + 10, MINUTES_IN_HOUR * 14));
        a.back().Intervals.push_back(TTimeInterval::MakeFromMinutes(MINUTES_IN_HOUR * 14 + 15, MINUTES_IN_HOUR * 22));

        UNIT_ASSERT_EQUAL(WorkingTimeXmlString(a), "<a><Availability><Tuesday/><Interval from=\"12:00:00\" to=\"20:00:00\"/><Interval from=\"22:00:00\" to=\"02:00:00\"/></Availability><Availability><Wednesday/><Interval from=\"08:00:00\" to=\"10:00:00\"/><Interval from=\"10:10:00\" to=\"14:00:00\"/><Interval from=\"14:15:00\" to=\"22:00:00\"/></Availability></a>");
    }

    Y_UNIT_TEST(TestWorkingTimeXML5) {
        TAvailabilities a;
        UNIT_ASSERT_EQUAL(WorkingTimeXmlString(a), "<a/>");
    }

    Y_UNIT_TEST(TestPrintScheduledHoursAttribute) {
        TVector<NGeosearch::NProtos::NRO::TScheduledHoursItem> scheduledHours;

        NGeosearch::NPbsTypes::THours hours1;
        TVector<NGeosearch::NPbsTypes::TTimeInterval> intervals1;
        {
            NGeosearch::NPbsTypes::TTimeInterval interval;
            interval.From = 600;
            interval.To = 720;
            intervals1.push_back(interval);
        }
        {
            NGeosearch::NPbsTypes::TTimeInterval interval;
            interval.From = 750;
            interval.To = 1080;
            intervals1.push_back(interval);
        }
        hours1.Intervals = MakeArrayRef(intervals1);
        hours1.TimezoneOffset = 180;
        NGeosearch::NProtos::NRO::TScheduledHoursItem scheduledHoursItem1;
        scheduledHoursItem1.Key = "2021-01-28";
        scheduledHoursItem1.Value = hours1;
        scheduledHours.push_back(scheduledHoursItem1);

        NGeosearch::NPbsTypes::THours hours2;
        TVector<NGeosearch::NPbsTypes::TTimeInterval> intervals2;
        {
            NGeosearch::NPbsTypes::TTimeInterval interval;
            interval.From = 0;
            interval.To = 0;
            intervals2.push_back(interval);
        }
        hours2.Intervals = MakeArrayRef(intervals2);
        hours2.TimezoneOffset = 180;
        NGeosearch::NProtos::NRO::TScheduledHoursItem scheduledHoursItem2;
        scheduledHoursItem2.Key = "2021-01-29";
        scheduledHoursItem2.Value = hours2;
        scheduledHours.push_back(scheduledHoursItem2);

        NGeosearch::NPbsTypes::THours hours3;
        TVector<NGeosearch::NPbsTypes::TTimeInterval> intervals3;
        hours3.Intervals = MakeArrayRef(intervals3);
        hours3.TimezoneOffset = 180;
        NGeosearch::NProtos::NRO::TScheduledHoursItem scheduledHoursItem3;
        scheduledHoursItem3.Key = "2021-01-30";
        scheduledHoursItem3.Value = hours3;
        scheduledHours.push_back(scheduledHoursItem3);

        UNIT_ASSERT_STRINGS_EQUAL(PrintScheduledHoursAttribute(MakeArrayRef(scheduledHours)),
                                  "{\"2021-01-28\":{\"from\":750,\"to\":1080},\"time_zone\":180,\"2021-01-29\":[],\"2021-01-30\":[]}");
    }
}

#include "helpers.h"

#include <extsearch/geo/kernel/working_hours/parse_hours.h>

#include <library/cpp/testing/unittest/registar.h>

using namespace NGeosearch;
using namespace NGeosearch::NWorkingHours;
namespace NWH = NGeosearch::NWorkingHours;

void AddInterval(TVector<NSpravExport::TWorkingTime>& hoursProto, const TString& from,
                 const TString& to, const TString& day) {
    NSpravExport::TWorkingTime newInterval;
    newInterval.SetFrom(from);
    newInterval.SetTo(to);
    NSpravExport::TWorkingTime_EDay dayProto;
    Y_ENSURE(NSpravExport::TWorkingTime_EDay_Parse(day, &dayProto));
    newInterval.SetDay(dayProto);
    hoursProto.push_back(newInterval);
}

void AddInterval(TVector<NSpravExport::TScheduledWorkingTime>& schHoursProto, const TString& from,
                 const TString& to, const TString& date) {
    NSpravExport::TScheduledWorkingTime newInterval;
    newInterval.SetFrom(from);
    newInterval.SetTo(to);
    newInterval.SetIsoDate(date);
    schHoursProto.push_back(newInterval);
}

Y_UNIT_TEST_SUITE(TParseXmlTest) {
    Y_UNIT_TEST(TestParse) {
        const NXml::TDocument doc("<Hours>"
                                  "  <Availability>"
                                  "    <Monday/>"
                                  "    <Interval from='12:30' to='15:00'/>"
                                  "  </Availability>"
                                  "</Hours>",
                                  NXml::TDocument::String);

        const auto intervals = ParseHours(doc.Root());
        UNIT_ASSERT_EQUAL(intervals.GetIntervals().size(), 1);
        UNIT_ASSERT_EQUAL(intervals.GetIntervals().begin()->GetFrom(), NWH::TTime(MONDAY, Parse("12:30")));
        UNIT_ASSERT_EQUAL(intervals.GetIntervals().begin()->GetTo(), NWH::TTime(MONDAY, Parse("15:00")));
    }

    Y_UNIT_TEST(TestObsolete) {
        const NXml::TDocument doc("<Hours xmlns='http://maps.yandex.ru/business/1.x'>"
                                  "  <Availability>"
                                  "    <Weekend/>"
                                  "    <Interval from='12:30' to='15:00'/>"
                                  "  </Availability>"
                                  "</Hours>",
                                  NXml::TDocument::String);
        // doc.addNamespace("b", "http://maps.yandex.ru/business/1.x"); - this was required for maps::xml3

        const auto intervals = ParseHours(doc.Root());
        UNIT_ASSERT_EQUAL(intervals.GetIntervals().size(), 2);
        UNIT_ASSERT_EQUAL(intervals.GetIntervals().begin()->GetFrom(), NWH::TTime(SATURDAY, Parse("12:30")));
        UNIT_ASSERT_EQUAL(intervals.GetIntervals().begin()->GetTo(), NWH::TTime(SATURDAY, Parse("15:00")));
    }

    Y_UNIT_TEST(TestOverMidnight) {
        const NXml::TDocument doc("<Hours>"
                                  "  <Availability>"
                                  "    <Interval from='23:59:59' to='03:00'/>"
                                  "    <Everyday/>"
                                  "  </Availability>"
                                  "</Hours>",
                                  NXml::TDocument::String);

        const auto intervals = ParseHours(doc.Root());
        const auto seq = intervals.GetIntervalSeq();

        UNIT_ASSERT_EQUAL(seq.size(), 8);

        UNIT_ASSERT_EQUAL(seq[0].GetFrom(), NWH::TTime(MONDAY, Parse("0:00")));
        UNIT_ASSERT_EQUAL(seq[0].GetTo(), NWH::TTime(MONDAY, Parse("3:00")));

        UNIT_ASSERT_EQUAL(seq[1].GetFrom(), NWH::TTime(MONDAY, Parse("23:59")));
        UNIT_ASSERT_EQUAL(seq[1].GetTo(), NWH::TTime(TUESDAY, Parse("3:00")));

        UNIT_ASSERT_EQUAL(seq[6].GetFrom(), NWH::TTime(SATURDAY, Parse("23:59")));
        UNIT_ASSERT_EQUAL(seq[6].GetTo(), NWH::TTime(SUNDAY, Parse("3:00")));

        UNIT_ASSERT_EQUAL(seq[7].GetFrom(), NWH::TTime(SUNDAY, Parse("23:59")));
        UNIT_ASSERT_EQUAL(seq[7].GetTo(), NWH::TTime(NEXT_WEEK_FIRST_DAY, Parse("0:00")));
    }

    Y_UNIT_TEST(TestFromMidnight) {
        const NXml::TDocument doc("<Hours>"
                                  "  <Availability>"
                                  "    <Interval from='00:00' to='03:00'/>"
                                  "    <Everyday/>"
                                  "  </Availability>"
                                  "</Hours>",
                                  NXml::TDocument::String);

        const auto intervals = ParseHours(doc.Root());

        UNIT_ASSERT_EQUAL(intervals.GetIntervals().size(), 7);

        UNIT_ASSERT_EQUAL(intervals.GetIntervals().begin()->GetFrom(), NWH::TTime(MONDAY, Parse("0:00")));
        UNIT_ASSERT_EQUAL(intervals.GetIntervals().begin()->GetTo(), NWH::TTime(MONDAY, Parse("3:00")));

        UNIT_ASSERT_EQUAL(intervals.GetIntervals().rbegin()->GetFrom(), NWH::TTime(SUNDAY, Parse("0:00")));
        UNIT_ASSERT_EQUAL(intervals.GetIntervals().rbegin()->GetTo(), NWH::TTime(SUNDAY, Parse("3:00")));
    }

    Y_UNIT_TEST(TestTillMidnight) {
        const NXml::TDocument doc("<Hours>"
                                  "  <Availability>"
                                  "    <Interval from='10:00' to='00:00'/>"
                                  "    <Everyday/>"
                                  "  </Availability>"
                                  "</Hours>",
                                  NXml::TDocument::String);

        const auto intervals = ParseHours(doc.Root());

        UNIT_ASSERT_EQUAL(intervals.GetIntervals().size(), 7);

        UNIT_ASSERT_EQUAL(intervals.GetIntervals().begin()->GetFrom(), NWH::TTime(MONDAY, 10, 0));
        UNIT_ASSERT_EQUAL(intervals.GetIntervals().begin()->GetTo(), NWH::TTime(MONDAY, 24, 0));
        UNIT_ASSERT_EQUAL(intervals.GetIntervals().rbegin()->GetFrom(), NWH::TTime(SUNDAY, 10, 0));
        UNIT_ASSERT_EQUAL(intervals.GetIntervals().rbegin()->GetTo(), NWH::TTime(SUNDAY, 24, 0));
    }

    Y_UNIT_TEST(TestParseAlways) {
        const NXml::TDocument doc("<Hours>"
                                  "  <Availability>"
                                  "    <Everyday/>"
                                  "    <Interval from='00:00' to='24:00'/>"
                                  "  </Availability>"
                                  "</Hours>",
                                  NXml::TDocument::String);

        const auto intervals = ParseHours(doc.Root());
        UNIT_ASSERT_EQUAL(intervals.GetIntervals().size(), 1);
        UNIT_ASSERT_EQUAL(intervals.GetIntervals().begin()->GetFrom(), NWH::TTime(MONDAY, Parse("00:00")));
        UNIT_ASSERT_EQUAL(intervals.GetIntervals().begin()->GetTo(), NWH::TTime(SUNDAY, Parse("24:00")));
    }

    Y_UNIT_TEST(TestParseTwoFullDays) {
        const NXml::TDocument doc("<Hours>"
                                  "  <Availability>"
                                  "    <Monday/>"
                                  "    <Interval from='00:00' to='24:00'/>"
                                  "  </Availability>"
                                  "  <Availability>"
                                  "    <Tuesday/>"
                                  "    <Interval from='00:00' to='24:00'/>"
                                  "  </Availability>"
                                  "</Hours>",
                                  NXml::TDocument::String);

        const auto intervals = ParseHours(doc.Root());
        UNIT_ASSERT_EQUAL(intervals.GetIntervals().size(), 1);
        UNIT_ASSERT_EQUAL(intervals.GetIntervals().begin()->GetFrom(), NWH::TTime(MONDAY, Parse("00:00")));
        UNIT_ASSERT_EQUAL(intervals.GetIntervals().begin()->GetTo(), NWH::TTime(TUESDAY, Parse("24:00")));
    }

    Y_UNIT_TEST(TestParseScheduledHours) {
        const NXml::TDocument doc("<ScheduledHours>"
                                  "  <Availability iso_date='2018-11-05'>"
                                  "    <Interval from='00:00' to='05:00'/> "
                                  "    <Monday/>"
                                  "  </Availability>"
                                  "  <Availability iso_date='2018-11-05'>"
                                  "    <Interval from='06:00' to='10:00'/>"
                                  "    <Monday/>"
                                  "  </Availability>"
                                  "  <Availability iso_date='2018-11-05'>"
                                  "    <Interval from='11:00' to='24:00'/>"
                                  "    <Monday/>"
                                  "  </Availability>"
                                  "  <Availability iso_date='2018-11-06'>"
                                  "    <Interval from='00:00' to='10:00'/>"
                                  "    <Tuesday/>"
                                  "  </Availability>"
                                  "  <Availability iso_date='2018-11-07'>"
                                  "    <Interval from='00:00' to='00:00'/>"
                                  "    <Wednesday/>"
                                  "  </Availability>"
                                  "  <Availability iso_date='2018-11-11'>"
                                  "    <Interval from='00:00' to='05:00'/>"
                                  "    <Sunday/>"
                                  "  </Availability>"
                                  "  <Availability iso_date='2018-11-11'>"
                                  "    <Interval from='05:00' to='24:00'/>"
                                  "    <Sunday/>"
                                  "  </Availability>"
                                  "</ScheduledHours>",
                                  NXml::TDocument::String);
        const auto dateToIntervals = ParseScheduledHours(doc.Root());
        UNIT_ASSERT_EQUAL(dateToIntervals.at("20181105").GetIntervalSeq().size(), 3);
        UNIT_ASSERT_EQUAL(dateToIntervals.at("20181105").GetIntervalSeq()[0].GetFrom(), NWH::TTime(MONDAY, Parse("00:00")));
        UNIT_ASSERT_EQUAL(dateToIntervals.at("20181105").GetIntervalSeq()[0].GetTo(), NWH::TTime(MONDAY, Parse("05:00")));
        UNIT_ASSERT_EQUAL(dateToIntervals.at("20181105").GetIntervalSeq()[1].GetFrom(), NWH::TTime(MONDAY, Parse("06:00")));
        UNIT_ASSERT_EQUAL(dateToIntervals.at("20181105").GetIntervalSeq()[1].GetTo(), NWH::TTime(MONDAY, Parse("10:00")));
        UNIT_ASSERT_EQUAL(dateToIntervals.at("20181105").GetIntervalSeq()[2].GetFrom(), NWH::TTime(MONDAY, Parse("11:00")));
        UNIT_ASSERT_EQUAL(dateToIntervals.at("20181105").GetIntervalSeq()[2].GetTo(), NWH::TTime(MONDAY, Parse("24:00")));
        UNIT_ASSERT_EQUAL(dateToIntervals.at("20181106").GetIntervalSeq().size(), 1);
        UNIT_ASSERT_EQUAL(dateToIntervals.at("20181106").GetIntervalSeq()[0].GetFrom(), NWH::TTime(TUESDAY, Parse("00:00")));
        UNIT_ASSERT_EQUAL(dateToIntervals.at("20181106").GetIntervalSeq()[0].GetTo(), NWH::TTime(TUESDAY, Parse("10:00")));
        UNIT_ASSERT_EQUAL(dateToIntervals.at("20181107").GetIntervalSeq().size(), 0);
        UNIT_ASSERT_EQUAL(dateToIntervals.at("20181111").GetIntervalSeq().size(), 1);
        UNIT_ASSERT_EQUAL(dateToIntervals.at("20181111").GetIntervalSeq()[0].GetFrom(), NWH::TTime(SUNDAY, Parse("00:00")));
        UNIT_ASSERT_EQUAL(dateToIntervals.at("20181111").GetIntervalSeq()[0].GetTo(), NWH::TTime(SUNDAY, Parse("24:00")));
    }
}

Y_UNIT_TEST_SUITE(TParseProtoTest) {
    Y_UNIT_TEST(TestParse) {
        TVector<NSpravExport::TWorkingTime> hoursProto;
        AddInterval(hoursProto, "12:30", "15:00", "Monday");

        const auto intervals = ParseHours(hoursProto);
        UNIT_ASSERT_EQUAL(intervals.GetIntervals().size(), 1);
        UNIT_ASSERT_EQUAL(intervals.GetIntervals().begin()->GetFrom(), NWH::TTime(MONDAY, Parse("12:30")));
        UNIT_ASSERT_EQUAL(intervals.GetIntervals().begin()->GetTo(), NWH::TTime(MONDAY, Parse("15:00")));
    }

    Y_UNIT_TEST(TestOverMidnight) {
        TVector<NSpravExport::TWorkingTime> hoursProto;
        AddInterval(hoursProto, "23:59:59", "03:00", "Everyday");

        const auto intervals = ParseHours(hoursProto);
        const auto seq = intervals.GetIntervalSeq();

        UNIT_ASSERT_EQUAL(seq.size(), 8);

        UNIT_ASSERT_EQUAL(seq[0].GetFrom(), NWH::TTime(MONDAY, Parse("0:00")));
        UNIT_ASSERT_EQUAL(seq[0].GetTo(), NWH::TTime(MONDAY, Parse("3:00")));

        UNIT_ASSERT_EQUAL(seq[1].GetFrom(), NWH::TTime(MONDAY, Parse("23:59")));
        UNIT_ASSERT_EQUAL(seq[1].GetTo(), NWH::TTime(TUESDAY, Parse("3:00")));

        UNIT_ASSERT_EQUAL(seq[6].GetFrom(), NWH::TTime(SATURDAY, Parse("23:59")));
        UNIT_ASSERT_EQUAL(seq[6].GetTo(), NWH::TTime(SUNDAY, Parse("3:00")));

        UNIT_ASSERT_EQUAL(seq[7].GetFrom(), NWH::TTime(SUNDAY, Parse("23:59")));
        UNIT_ASSERT_EQUAL(seq[7].GetTo(), NWH::TTime(NEXT_WEEK_FIRST_DAY, Parse("0:00")));
    }

    Y_UNIT_TEST(TestObsolete) {
        TVector<NSpravExport::TWorkingTime> hoursProto;
        AddInterval(hoursProto, "12:30", "15:00", "Weekend");

        const auto intervals = ParseHours(hoursProto);
        ;
        UNIT_ASSERT_EQUAL(intervals.GetIntervals().size(), 2);
        UNIT_ASSERT_EQUAL(intervals.GetIntervals().begin()->GetFrom(), NWH::TTime(SATURDAY, Parse("12:30")));
        UNIT_ASSERT_EQUAL(intervals.GetIntervals().begin()->GetTo(), NWH::TTime(SATURDAY, Parse("15:00")));
    }

    Y_UNIT_TEST(TestFromMidnight) {
        TVector<NSpravExport::TWorkingTime> hoursProto;
        AddInterval(hoursProto, "00:00", "03:00", "Everyday");

        const auto intervals = ParseHours(hoursProto);

        UNIT_ASSERT_EQUAL(intervals.GetIntervals().size(), 7);

        UNIT_ASSERT_EQUAL(intervals.GetIntervals().begin()->GetFrom(), NWH::TTime(MONDAY, Parse("0:00")));
        UNIT_ASSERT_EQUAL(intervals.GetIntervals().begin()->GetTo(), NWH::TTime(MONDAY, Parse("3:00")));

        UNIT_ASSERT_EQUAL(intervals.GetIntervals().rbegin()->GetFrom(), NWH::TTime(SUNDAY, Parse("0:00")));
        UNIT_ASSERT_EQUAL(intervals.GetIntervals().rbegin()->GetTo(), NWH::TTime(SUNDAY, Parse("3:00")));
    }

    Y_UNIT_TEST(TestTillMidnight) {
        TVector<NSpravExport::TWorkingTime> hoursProto;
        AddInterval(hoursProto, "10:00", "00:00", "Everyday");

        const auto intervals = ParseHours(hoursProto);

        UNIT_ASSERT_EQUAL(intervals.GetIntervals().size(), 7);

        UNIT_ASSERT_EQUAL(intervals.GetIntervals().begin()->GetFrom(), NWH::TTime(MONDAY, 10, 0));
        UNIT_ASSERT_EQUAL(intervals.GetIntervals().begin()->GetTo(), NWH::TTime(MONDAY, 24, 0));
        UNIT_ASSERT_EQUAL(intervals.GetIntervals().rbegin()->GetFrom(), NWH::TTime(SUNDAY, 10, 0));
        UNIT_ASSERT_EQUAL(intervals.GetIntervals().rbegin()->GetTo(), NWH::TTime(SUNDAY, 24, 0));
    }

    Y_UNIT_TEST(TestParseAlways) {
        TVector<NSpravExport::TWorkingTime> hoursProto;
        AddInterval(hoursProto, "00:00", "24:00", "Everyday");

        const auto intervals = ParseHours(hoursProto);

        UNIT_ASSERT_EQUAL(intervals.GetIntervals().size(), 1);
        UNIT_ASSERT_EQUAL(intervals.GetIntervals().begin()->GetFrom(), NWH::TTime(MONDAY, Parse("00:00")));
        UNIT_ASSERT_EQUAL(intervals.GetIntervals().begin()->GetTo(), NWH::TTime(SUNDAY, Parse("24:00")));
    }

    Y_UNIT_TEST(TestParseTwoFullDays) {
        TVector<NSpravExport::TWorkingTime> hoursProto;
        AddInterval(hoursProto, "00:00", "24:00", "Monday");
        AddInterval(hoursProto, "00:00", "24:00", "Tuesday");

        const auto intervals = ParseHours(hoursProto);

        UNIT_ASSERT_EQUAL(intervals.GetIntervals().size(), 1);
        UNIT_ASSERT_EQUAL(intervals.GetIntervals().begin()->GetFrom(), NWH::TTime(MONDAY, Parse("00:00")));
        UNIT_ASSERT_EQUAL(intervals.GetIntervals().begin()->GetTo(), NWH::TTime(TUESDAY, Parse("24:00")));
    }

    Y_UNIT_TEST(TestParseScheduledHours) {
        TVector<NSpravExport::TScheduledWorkingTime> schHoursProto;
        AddInterval(schHoursProto, "00:00", "05:00", "2018-11-05");
        AddInterval(schHoursProto, "06:00", "10:00", "2018-11-05");
        AddInterval(schHoursProto, "11:00", "24:00", "2018-11-05");
        AddInterval(schHoursProto, "00:00", "10:00", "2018-11-06");
        AddInterval(schHoursProto, "00:00", "00:00", "2018-11-07");
        AddInterval(schHoursProto, "00:00", "05:00", "2018-11-11");
        AddInterval(schHoursProto, "05:00", "24:00", "2018-11-11");
        const auto dateToIntervals = ParseScheduledHours(schHoursProto);

        UNIT_ASSERT_EQUAL(dateToIntervals.at("20181105").GetIntervalSeq().size(), 3);
        UNIT_ASSERT_EQUAL(dateToIntervals.at("20181105").GetIntervalSeq()[0].GetFrom(), NWH::TTime(MONDAY, Parse("00:00")));
        UNIT_ASSERT_EQUAL(dateToIntervals.at("20181105").GetIntervalSeq()[0].GetTo(), NWH::TTime(MONDAY, Parse("05:00")));
        UNIT_ASSERT_EQUAL(dateToIntervals.at("20181105").GetIntervalSeq()[1].GetFrom(), NWH::TTime(MONDAY, Parse("06:00")));
        UNIT_ASSERT_EQUAL(dateToIntervals.at("20181105").GetIntervalSeq()[1].GetTo(), NWH::TTime(MONDAY, Parse("10:00")));
        UNIT_ASSERT_EQUAL(dateToIntervals.at("20181105").GetIntervalSeq()[2].GetFrom(), NWH::TTime(MONDAY, Parse("11:00")));
        UNIT_ASSERT_EQUAL(dateToIntervals.at("20181105").GetIntervalSeq()[2].GetTo(), NWH::TTime(MONDAY, Parse("24:00")));
        UNIT_ASSERT_EQUAL(dateToIntervals.at("20181106").GetIntervalSeq().size(), 1);
        UNIT_ASSERT_EQUAL(dateToIntervals.at("20181106").GetIntervalSeq()[0].GetFrom(), NWH::TTime(TUESDAY, Parse("00:00")));
        UNIT_ASSERT_EQUAL(dateToIntervals.at("20181106").GetIntervalSeq()[0].GetTo(), NWH::TTime(TUESDAY, Parse("10:00")));
        UNIT_ASSERT_EQUAL(dateToIntervals.at("20181107").GetIntervalSeq().size(), 0);
        UNIT_ASSERT_EQUAL(dateToIntervals.at("20181111").GetIntervalSeq().size(), 1);
        UNIT_ASSERT_EQUAL(dateToIntervals.at("20181111").GetIntervalSeq()[0].GetFrom(), NWH::TTime(SUNDAY, Parse("00:00")));
        UNIT_ASSERT_EQUAL(dateToIntervals.at("20181111").GetIntervalSeq()[0].GetTo(), NWH::TTime(SUNDAY, Parse("24:00")));
    }
}

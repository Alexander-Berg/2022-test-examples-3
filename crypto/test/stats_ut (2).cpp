#include <library/cpp/testing/unittest/registar.h>
#include <crypta/graph/soup/edge_weights/lib/stats.h>

Y_UNIT_TEST_SUITE(TUnitStatsTest) {
    const double EPS = 1e-9;
    const int DAY = 24 * 60 * 60;

    Y_UNIT_TEST(TestSurvivalFunction) {
        NEdgeStats::TStatsCollector collector;
        collector.UpdateWith(1, 2 * DAY);
        collector.UpdateWith(2, 2 * DAY);
        auto hist = collector.ConvertToHistograms();

        UNIT_ASSERT_DOUBLES_EQUAL(NEdgeStats::GetSurvivalFunctionValue(1, 1, hist), 1., EPS);
        UNIT_ASSERT_DOUBLES_EQUAL(NEdgeStats::GetSurvivalFunctionValue(1, 2, hist), 1., EPS);
        UNIT_ASSERT_DOUBLES_EQUAL(NEdgeStats::GetSurvivalFunctionValue(1, 3, hist), 0., EPS);

        collector.UpdateWith(3, 0 * DAY);
        collector.UpdateWith(3, 1 * DAY);
        collector.UpdateWith(3, 1 * DAY);
        collector.UpdateWith(3, 2 * DAY);
        hist = collector.ConvertToHistograms();
        UNIT_ASSERT_DOUBLES_EQUAL(NEdgeStats::GetSurvivalFunctionValue(3, 1, hist), 0.75, EPS);
        UNIT_ASSERT_DOUBLES_EQUAL(NEdgeStats::GetSurvivalFunctionValue(3, 2, hist), 0.25, EPS);
    }

    Y_UNIT_TEST(TestDefaultSurvivalFunction) {
        UNIT_ASSERT_DOUBLES_EQUAL(NEdgeStats::GetDefaultSurvivalFunctionValue(2, 1), 1., EPS);
        UNIT_ASSERT_DOUBLES_EQUAL(NEdgeStats::GetDefaultSurvivalFunctionValue(2, 2), 1., EPS);
        UNIT_ASSERT_DOUBLES_EQUAL(NEdgeStats::GetDefaultSurvivalFunctionValue(2, 3), 0.75, EPS);
        UNIT_ASSERT_DOUBLES_EQUAL(NEdgeStats::GetDefaultSurvivalFunctionValue(2, 4), 0.5625, EPS);
    }

    Y_UNIT_TEST(TestEdgeFields) {
        TString id1Type = "yandexuid";
        NYT::TNode row = NYT::TNode()
            ("dates", NYT::TNode::CreateList().Add("2018-01-02").Add("2018-01-04").Add("2017-03-03"))
            ("sourceType", "account-manager")
            ("logSource", "mm")
            ("id1Type", id1Type)
            ("id2Type", "login");

        auto timestamps = NEdgeStats::NEdgeFields::GetSortedTimestamps(row);
        UNIT_ASSERT_EQUAL(timestamps.back(), 1515009600);
        TEdgeType edgeType;
        Y_PROTOBUF_SUPPRESS_NODISCARD edgeType.ParseFromString(NEdgeStats::NEdgeFields::GetEdgeType(row));
        UNIT_ASSERT_EQUAL(edgeType.GetId1Type(), id1Type);
    }

    Y_UNIT_TEST(TestStatsCollector) {
        NEdgeStats::TStatsCollector collector;
        NYT::TNode row = NYT::TNode()("dates", NYT::TNode::CreateList().Add("2018-01-02").Add("2018-01-03"));
        auto timestamps = NEdgeStats::NEdgeFields::GetSortedTimestamps(row);
        auto currentTimestamp = timestamps.back() + 24 * 60 * 60;
        ui64 usersCount = 10;
        for (size_t i = 0; i < usersCount; ++i) {
            collector.UpdateWith(timestamps, currentTimestamp, 30);
        }
        UNIT_ASSERT_EQUAL(collector.GetUsersCount(), usersCount);
        UNIT_ASSERT_EQUAL(collector.GetCounts()[1][1], usersCount);
    }
}

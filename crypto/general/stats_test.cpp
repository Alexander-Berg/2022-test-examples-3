#include <crypta/lib/native/stats/stats.h>
#include <crypta/lib/native/stats/stats_registry.h>

#include <util/generic/singleton.h>

#include <library/cpp/testing/unittest/registar.h>

static constexpr double EPS = 1e-7;

Y_UNIT_TEST_SUITE(StatsTest) {
    Y_UNIT_TEST(Default) {
        TStats::UnregisterAll();

        TStats::TSettings statsSettings = {
                .HistMin = 0,
                .HistMax = 100,
                .HistBinCount = 100,
                .PercentMax = 400,
                .PercentPrecision = 3,
                .Percentiles = {100, 50}
        };

        TStats::RegisterDefault("test", statsSettings);

        auto stats1 = TStats::GetDefault();
        auto stats2 = TStats::GetDefault();

        stats1.Count->Add("count", 10);
        stats2.Count->Add("count", 20);

        stats1.Percentile->Add("percentile", 500);
        stats2.Percentile->Add("percentile", 200);

        stats1.Hist->Add("hist", 0);
        stats1.Hist->Add("hist", 1);

        stats2.Hist->Add("hist", 100);
        stats2.Hist->Add("hist", 101);


        const auto& snapshot = Singleton<TStatsRegistry>()->GetSnapshot();

        UNIT_ASSERT_EQUAL(102 + 3, snapshot.size());
        UNIT_ASSERT_DOUBLES_EQUAL(30, snapshot.at("test.count"), EPS);
        UNIT_ASSERT_DOUBLES_EQUAL(200, snapshot.at("test.percentile.p50"), EPS);
        UNIT_ASSERT_DOUBLES_EQUAL(500, snapshot.at("test.percentile.p100"), EPS);
        UNIT_ASSERT_DOUBLES_EQUAL(1, snapshot.at("test.hist.b0"), EPS);
        UNIT_ASSERT_DOUBLES_EQUAL(1, snapshot.at("test.hist.b1"), EPS);
        UNIT_ASSERT_DOUBLES_EQUAL(1, snapshot.at("test.hist.b100"), EPS);
        UNIT_ASSERT_DOUBLES_EQUAL(1, snapshot.at("test.hist.b101"), EPS);
    }

    Y_UNIT_TEST(Named) {
        TStats::UnregisterAll();

        TStats::TSettings statsSettings {
                .HistMin = 0,
                .HistMax = 1,
                .HistBinCount = 1,
                .PercentMax = 1,
                .PercentPrecision = 1,
                .Percentiles = {100, 50}
        };

        TStats::Register("test1", statsSettings);
        TStats::Register("test2", statsSettings);

        auto stats1 = TStats::Get("test1");
        auto stats2 = TStats::Get("test2");

        stats1.Count->Add("count", 10);
        stats2.Count->Add("count", 20);

        const auto& snapshot = Singleton<TStatsRegistry>()->GetSnapshot();

        UNIT_ASSERT_EQUAL(2, snapshot.size());

        UNIT_ASSERT_DOUBLES_EQUAL(10, snapshot.at("test1.count"), EPS);
        UNIT_ASSERT_DOUBLES_EQUAL(20, snapshot.at("test2.count"), EPS);
    }

    Y_UNIT_TEST(UnregisteredDefault) {
        TStats::UnregisterAll();

        TStats::TSettings statsSettings;
        TStats::Register("test", statsSettings);

        UNIT_ASSERT_EXCEPTION(TStats::GetDefault(), yexception);
    }

    Y_UNIT_TEST(UnregisteredNamed) {
        TStats::UnregisterAll();

        TStats::TSettings statsSettings;
        TStats::RegisterDefault("default", statsSettings);
        TStats::Register("test", statsSettings);

        UNIT_ASSERT_EXCEPTION(TStats::Get("test2"), yexception);
    }
}

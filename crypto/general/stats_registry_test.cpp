#include <crypta/lib/native/stats/stats_registry.h>
#include <crypta/lib/native/stats/count_stat.h>
#include <crypta/lib/native/stats/avg_stat.h>
#include <crypta/lib/native/stats/max_stat.h>
#include <crypta/lib/native/stats/multi_stat.h>
#include <crypta/lib/native/stats/hist_stat.h>
#include <crypta/lib/native/stats/percentile_stat.h>

#include <library/cpp/testing/unittest/registar.h>

static constexpr double EPS = 1e-7;

Y_UNIT_TEST_SUITE(StatsRegistryTest) {
    Y_UNIT_TEST(RegisterAndGetSnapshot) {
        TStatsRegistry stats;
        auto count = stats.Create<TCountStat>("count");
        auto avg = stats.Create<TAvgStat>("avg");
        auto max = stats.Create<TMaxStat>("max");
        auto multiCount = stats.Create<TMultiStat<TCountStat>>("multi_count");
        auto hist = stats.Create<THistStat<>>("hist", 0, 1, 2);
        auto multiHist = stats.Create<TMultiHistStat>("multi_hist", 0, 1, 2);
        auto percentile = stats.Create<TPercentileStat<>>("percentile", 400, 3, TVector<float>({100, 50}));
        auto multiPercentile = stats.Create<TMultiPercentileStat>("multi_percentile", 400, 3, TVector<float>({100, 50}));

        count->Add(10);

        avg->Add(1.5);
        avg->Add(0.47);
        avg->Add(40);

        max->Add(1.5);
        max->Add(40.1);
        max->Add(0.47);

        multiCount->Add("x", 3);
        multiCount->Add("y", 10);
        multiCount->Add("y", 20);

        hist->Add(0, 1);
        hist->Add(0.2, 1);
        hist->Add(0.3, 1);
        hist->Add(0.6, 1);
        hist->Add(0.7, 2);
        hist->Add(1.1, 4);

        multiHist->Add("alpha", 0, 1);
        multiHist->Add("alpha", 0.2, 1);
        multiHist->Add("alpha", 0.3, 1);
        multiHist->Add("alpha", 0.6, 1);
        multiHist->Add("alpha", 0.7, 2);
        multiHist->Add("beta", 0.7, 2);
        multiHist->Add("alpha", 1.1, 4);

        percentile->Add(500);
        percentile->Add(200);

        multiPercentile->Add("gamma", 500);
        multiPercentile->Add("gamma", 200);
        multiPercentile->Add("delta", 200);

        const auto& snapshot = stats.GetSnapshot();

        UNIT_ASSERT_EQUAL(23u, snapshot.size());

        UNIT_ASSERT_DOUBLES_EQUAL(10, snapshot.at("count"), EPS);
        UNIT_ASSERT_DOUBLES_EQUAL(13.99, snapshot.at("avg"), EPS);
        UNIT_ASSERT_DOUBLES_EQUAL(40.1, snapshot.at("max"), EPS);
        UNIT_ASSERT_DOUBLES_EQUAL(3, snapshot.at("multi_count.x"), EPS);
        UNIT_ASSERT_DOUBLES_EQUAL(30, snapshot.at("multi_count.y"), EPS);

        UNIT_ASSERT_DOUBLES_EQUAL(1, snapshot.at("hist.b0"), EPS);
        UNIT_ASSERT_DOUBLES_EQUAL(2, snapshot.at("hist.b1"), EPS);
        UNIT_ASSERT_DOUBLES_EQUAL(3, snapshot.at("hist.b2"), EPS);
        UNIT_ASSERT_DOUBLES_EQUAL(4, snapshot.at("hist.b3"), EPS);

        UNIT_ASSERT_DOUBLES_EQUAL(1, snapshot.at("multi_hist.alpha.b0"), EPS);
        UNIT_ASSERT_DOUBLES_EQUAL(2, snapshot.at("multi_hist.alpha.b1"), EPS);
        UNIT_ASSERT_DOUBLES_EQUAL(3, snapshot.at("multi_hist.alpha.b2"), EPS);
        UNIT_ASSERT_DOUBLES_EQUAL(4, snapshot.at("multi_hist.alpha.b3"), EPS);

        UNIT_ASSERT_DOUBLES_EQUAL(0, snapshot.at("multi_hist.beta.b0"), EPS);
        UNIT_ASSERT_DOUBLES_EQUAL(0, snapshot.at("multi_hist.beta.b1"), EPS);
        UNIT_ASSERT_DOUBLES_EQUAL(2, snapshot.at("multi_hist.beta.b2"), EPS);
        UNIT_ASSERT_DOUBLES_EQUAL(0, snapshot.at("multi_hist.beta.b3"), EPS);

        UNIT_ASSERT_DOUBLES_EQUAL(200, snapshot.at("percentile.p50"), EPS);
        UNIT_ASSERT_DOUBLES_EQUAL(500, snapshot.at("percentile.p100"), EPS);

        UNIT_ASSERT_DOUBLES_EQUAL(200, snapshot.at("multi_percentile.gamma.p50"), EPS);
        UNIT_ASSERT_DOUBLES_EQUAL(500, snapshot.at("multi_percentile.gamma.p100"), EPS);
        UNIT_ASSERT_DOUBLES_EQUAL(200, snapshot.at("multi_percentile.delta.p50"), EPS);
        UNIT_ASSERT_DOUBLES_EQUAL(200, snapshot.at("multi_percentile.delta.p100"), EPS);
    }
}

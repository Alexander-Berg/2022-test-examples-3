#include <crypta/lib/native/rtmr/rtcrypta_solomon_utils/lib/solomon_metrics_reducer/sensors_registry_merger.h>

#include <library/cpp/monlib/encode/json/json.h>
#include <library/cpp/testing/unittest/registar.h>

using namespace NCrypta;

namespace {
    constexpr double EPS = 1e-6;

    void CheckHist(NMonitoring::IHistogramSnapshotPtr hist, const NMonitoring::TBucketBounds& bounds, const NMonitoring::TBucketValues& values) {
        for (ui32 i = 0; i < hist->Count(); ++i) {
            UNIT_ASSERT_EQUAL_C(bounds.at(i), hist->UpperBound(i), i << ": " << bounds.at(i) << " " << hist->UpperBound(i));
            UNIT_ASSERT_EQUAL_C(values.at(i), hist->Value(i), i << ": " << values.at(i) << " " << hist->Value(i));
        }
    }
}

Y_UNIT_TEST_SUITE(TClient) {
    Y_UNIT_TEST(Merge) {
        NMonitoring::TMetricRegistry registries[2];

        NMonitoring::TMetricRegistry resultRegistry;
        TSensorsRegistryMerger merger(resultRegistry);

        NMonitoring::TLabels gaugeLabels{{"type", "gauge"}};
        NMonitoring::TLabels intGaugeLabels{{"type", "int_gauge"}};
        NMonitoring::TLabels counterLabels{{"type", "counter"}};
        NMonitoring::TLabels rateLabels{{"type", "rate"}};

        NMonitoring::TBucketBounds bounds{0, 1, 2, 3};
        NMonitoring::TLabels histCounterLabels{{"type", "hist_counter"}};
        NMonitoring::TLabels histRateLabels{{"type", "hist_rate"}};

        for (auto& registry: registries) {
            registry.Gauge(gaugeLabels)->Set(1.3);
            registry.IntGauge(intGaugeLabels)->Set(2);
            registry.Counter(counterLabels)->Add(3);
            registry.Rate(rateLabels)->Add(4);

            auto histCounter = registry.HistogramCounter(histCounterLabels, NMonitoring::ExplicitHistogram(bounds));
            histCounter->Record(1, 5);
            histCounter->Record(0, 10);

            auto histRate = registry.HistogramRate(histRateLabels, NMonitoring::ExplicitHistogram(bounds));
            histRate->Record(2, 5);
            histRate->Record(3, 10);

            registry.Accept(TInstant::Now(), &merger);
        }

        UNIT_ASSERT_DOUBLES_EQUAL(2.6, resultRegistry.Gauge(gaugeLabels)->Get(), EPS);
        UNIT_ASSERT_EQUAL(4, resultRegistry.IntGauge(intGaugeLabels)->Get());
        UNIT_ASSERT_EQUAL(6, resultRegistry.Counter(counterLabels)->Get());
        UNIT_ASSERT_EQUAL(8, resultRegistry.Rate(rateLabels)->Get());

        bounds.push_back(Max<NMonitoring::TBucketBound>());
        CheckHist(resultRegistry.HistogramCounter(histCounterLabels, []() { return nullptr; })->TakeSnapshot(), bounds, {20, 10, 0, 0, 0});
        CheckHist(resultRegistry.HistogramRate(histRateLabels, []() { return nullptr; })->TakeSnapshot(), bounds, {0, 0, 10, 20, 0});
    }
}

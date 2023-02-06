#include <market/idx/datacamp/routines/tasks/lib/metric_collector.h>

#include <util/stream/file.h>

#include <library/cpp/json/json_reader.h>
#include <library/cpp/testing/unittest/gtest.h>

using namespace NJson;
using namespace NMarket::NDataCamp;

TEST(MetricsCollector, TimeMetrics) {
    TMetricsCollector metricsCollector;
    auto timer1 = metricsCollector.StartTimer("timer_1");
    auto timer2 = metricsCollector.StartTimer("timer_2");
    auto timer3 = metricsCollector.StartTimer("timer_3");
    Sleep(TDuration::Seconds(3));
    timer1.Stop();
    Sleep(TDuration::Seconds(3));
    timer2.Stop();
    Sleep(TDuration::Seconds(3));
    timer3.Stop();
    metricsCollector.SerializeToFile("app-metrics.json");
    TUnbufferedFileInput input("app-metrics.json");
    TJsonValue json = ReadJsonTree(&input);
    EXPECT_NE(json.GetValueByPath("timer_1"), NULL);
    EXPECT_NE(json.GetValueByPath("timer_2"), NULL);
    EXPECT_NE(json.GetValueByPath("timer_3"), NULL);
    EXPECT_GE(json.GetValueByPath("timer_1")->GetInteger(), 3);
    EXPECT_LE(json.GetValueByPath("timer_1")->GetInteger(), 4);
    EXPECT_GE(json.GetValueByPath("timer_2")->GetInteger(), 6);
    EXPECT_LE(json.GetValueByPath("timer_2")->GetInteger(), 7);
    EXPECT_GE(json.GetValueByPath("timer_3")->GetInteger(), 9);
    EXPECT_LE(json.GetValueByPath("timer_3")->GetInteger(), 10);
}

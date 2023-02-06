#include <gtest/gtest.h>
#include <gmock/gmock.h>
#include <mail/ymod_queuedb_worker/unistat/cpp/metrics.h>

using namespace ::testing;

namespace unistat {

TEST(MetricTest, shouldUpdateMetric) {
    WorkerTaskStat m;

    m.update({{"status", "success"}, {"task.type", "great_job"}});

    auto expected = std::make_tuple<std::string, std::size_t>("ctype=great_job;success_summ", 1);
    EXPECT_THAT(m.get(), UnorderedElementsAre(expected));
}

TEST(MetricTest, shouldNotUpdateMetric) {
    WorkerTaskStat m;

    m.update({});
    m.update({{"service", "barbet"}});
    m.update({{"status", "success"}});
    m.update({{"task.type", "great_job"}});

    EXPECT_TRUE(m.get().empty());
}

}
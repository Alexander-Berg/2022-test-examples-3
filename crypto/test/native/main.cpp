#include <library/cpp/testing/unittest/registar.h>

#include <crypta/lab/lib/native/stats.h>

using NLab::TUserDataStats;
using NLab::TUserDataStatsAggregator;

Y_UNIT_TEST_SUITE(Aggregators) {
    Y_UNIT_TEST(UserDataStatsAggregator) {
        TUserDataStatsAggregator<> aggregator;
        TUserDataStats stats;
        aggregator.UpdateWith(stats);
        TUserDataStats finalStats;
        aggregator.MergeInto(finalStats);
    }
}

#include <iostream>

#include <util/folder/filelist.h>
#include <library/cpp/testing/unittest/registar.h>

#include <market/replenishment/algorithms/vicugna/algorithm/random_greedy_algorithm.h>

#include <market/replenishment/algorithms/vicugna/test_util/world_fixture.h>

Y_UNIT_TEST_SUITE_F(DeliveryLimiters, TWorldFixture)
{
    Y_UNIT_TEST(WorldTest1)
    {
        World.SetPlanningPeriod(56);
        World.SetStartDate(TDate(2020, 03, 30));
        LoadFromYson("/market/replenishment/algorithms/vicugna/ut/test_delivery_limiters/test_data");
        TRandomGreedyAlgorithm randomGreedyAlgorithm(World, 10000, 100);
        randomGreedyAlgorithm.Optimize();

        for (auto* line : World.GetRecommendationLines()) {
            auto count = line->GetCount();
            if (count > 0) {
                bool success = line->DeliveryLimiterChange(-count);
                UNIT_ASSERT(success);
            }
        }

        for (const auto& [_, group] : World.GetDeliveryLimiter()) {
            UNIT_ASSERT_VALUES_EQUAL(0, group->CurrentCount);
        }
    }
}

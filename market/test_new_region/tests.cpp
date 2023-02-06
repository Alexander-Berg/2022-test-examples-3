#include <iostream>

#include <util/folder/filelist.h>
#include <library/cpp/testing/unittest/registar.h>

#include <market/replenishment/algorithms/vicugna/mutation/add_quant_mutator.h>
#include <market/replenishment/algorithms/vicugna/algorithm/simulated_annealing_algorithm.h>

#include <market/replenishment/algorithms/vicugna/test_util/world_fixture.h>

Y_UNIT_TEST_SUITE_F(Mutations, TWorldFixture)
{
    Y_UNIT_TEST(NEW_REGION)
    {
        World.SetPlanningPeriod(56);
        World.SetStartDate(TDate(2021, 03, 25));
        LoadFromYson("/market/replenishment/algorithms/vicugna/ut/test_new_region/test_data");
        TSimulatedAnnealingAlgorithm simulatedAnnealing(World, 10000, 100, 1);
        simulatedAnnealing.Optimize();

        ui64 countQty = 0;

        for (const auto& recommendation : World.GetRecommendations()) {
            for (const auto& line : recommendation->GetLines()) {
                countQty += line->GetMskuId();
            }
        }

        Y_ENSURE(countQty > 0);

        std::cout << "Test output2.";
    }
}

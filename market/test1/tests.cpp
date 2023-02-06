#include <iostream>

#include <util/folder/filelist.h>
#include <library/cpp/testing/unittest/registar.h>

#include <market/replenishment/algorithms/vicugna/mutation/add_quant_mutator.h>
#include <market/replenishment/algorithms/vicugna/algorithm/random_greedy_algorithm.h>

#include <market/replenishment/algorithms/vicugna/test_util/world_fixture.h>

Y_UNIT_TEST_SUITE_F(Mutations, TWorldFixture)
{
    Y_UNIT_TEST(WorldTest1)
    {
        World.SetPlanningPeriod(56);
        World.SetStartDate(TDate(2020, 03, 30));
        LoadFromYson("/market/replenishment/algorithms/vicugna/ut/test1/test_data");
        TRandomGreedyAlgorithm randomGreedyAlgorithm(World, 10000, 1);
        randomGreedyAlgorithm.Optimize();
        std::cout << "Test output2.";
    }
}

#include <iostream>

#include <util/folder/filelist.h>
#include <library/cpp/testing/unittest/registar.h>

#include <market/replenishment/algorithms/vicugna/mutation/add_quant_mutator.h>
#include <market/replenishment/algorithms/vicugna/algorithm/simulated_annealing_algorithm.h>

#include <market/replenishment/algorithms/vicugna/test_util/world_fixture.h>

Y_UNIT_TEST_SUITE_F(Mutations, TWorldFixture)
{
    Y_UNIT_TEST(TENDER_TEST)
    {
        World.SetPlanningPeriod(56);
        World.SetStartDate(TDate(2021, 07, 19));
        LoadFromYsonRealTables("/market/replenishment/algorithms/vicugna/ut/test_tenders_wo_capacity/test_data");
        TSimulatedAnnealingAlgorithm simulatedAnnealing(World, 10000, 0, 1);
        simulatedAnnealing.Optimize();

        ui64 countQtyTender = 0;

        for (const auto& recomendation : World.GetRecomendations()) {
            for (const auto& line : recomendation->GetLines()) {
                if (line->GetDeliveryType() == EDeliveryType::Tender) {
                    countQtyTender += line->GetMskuId();
                }
            }
        }

        Y_ASSERT(countQtyTender > 0);

        std::cout << "Test output2.";
    }
}

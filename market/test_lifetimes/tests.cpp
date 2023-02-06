#include <iostream>

#include <util/folder/filelist.h>
#include <library/cpp/testing/unittest/registar.h>

#include <market/replenishment/algorithms/vicugna/domain/msku.h>
#include <market/replenishment/algorithms/vicugna/domain/msku_seller.h>
#include <market/replenishment/algorithms/vicugna/domain/region.h>
#include <market/replenishment/algorithms/vicugna/mutation/add_quant_mutator.h>
#include <market/replenishment/algorithms/vicugna/algorithm/random_greedy_algorithm.h>

#include <market/replenishment/algorithms/vicugna/test_util/world_fixture.h>

Y_UNIT_TEST_SUITE_F(LifetineSimple, TWorldFixture)
{
    // во всех тесткейсах сс=0 спрос=1

    Y_UNIT_TEST(DirectSimple)  // есть 8 штук с лайфтаймом 7 и можно привозить с лидтаймом 1 и лайфтаймом 10 сколько угодно.
                               // проверим что спишется не более одной.
    {
        World.SetPlanningPeriod(28);
        World.SetStartDate(TDate("2020-06-01"));
        LoadFromYson("/market/replenishment/algorithms/vicugna/ut/test_lifetimes/test_data_simple");

        double initialScore = World.GetScore().Double();
        TRandomGreedyAlgorithm randomGreedyAlgorithm(World, 1000, 1);
        randomGreedyAlgorithm.Optimize();
        UNIT_ASSERT_GT_C(World.GetScore().Double(), initialScore,
            TStringBuilder() << "\nFinal score less or equal then initial(" << World.GetScore() << " <= " << initialScore << ")\n");

        TMskuSeller seller(*World.GetMskus().front());
        double sales = 0;
        double demand = 0;
        double writeOff = 0;
        for (size_t date = 0; date < 28; ++date) {
            seller.ProcessOneDay();
            sales += seller.TotalSale();
            demand += seller.TotalDemand();
            writeOff += seller.GetSupplierWriteOff(TWarehouseIndex(0), 0);
        }

        UNIT_ASSERT_LE_C(std::abs(sales - demand), 1e9, TStringBuilder() << "\nSales less then demand (" << sales << " != " << demand << ")\n");
        UNIT_ASSERT_LE(std::abs(sales - 28), 1e9);
        UNIT_ASSERT_LE(std::abs(writeOff - 1), 1e9);
    }

    Y_UNIT_TEST(WithoutDO)  // есть 7 штук с лайфтаймом 7 и 100 с лайфтаймом 100, проверим что сначала распродадутся 7 а потом 100.
    {
        World.SetPlanningPeriod(28);
        World.SetStartDate(TDate("2020-06-01"));
        LoadFromYson("/market/replenishment/algorithms/vicugna/ut/test_lifetimes/test_data_without_do");

        TMskuSeller seller(*World.GetMskus().front());

        for (size_t date = 0; date < 28; ++date) {
            seller.ProcessOneDay();
            UNIT_ASSERT_LE(std::abs(seller.TotalSale() - 1.), 1e9);  // sales == 1
            UNIT_ASSERT_LE(std::abs(seller.GetSupplierWriteOff(TWarehouseIndex(0), 0)), 1e9);  // write-off == 0
        }
    }
}

Y_UNIT_TEST_SUITE_F(Lifetime, TWorldFixture)
{
    Y_UNIT_TEST(SmallWriteOff)  // спрос 100, сг 7, квант 710. проверим что будем стабильно заказывать по 710 на неделю
    {
        World.SetPlanningPeriod(28);
        World.SetStartDate(TDate("2020-06-01"));
        LoadFromYson("/market/replenishment/algorithms/vicugna/ut/test_lifetimes/test_data_quant_small");

        double initialScore = World.GetScore().Double();
        TRandomGreedyAlgorithm randomGreedyAlgorithm(World, 1000, 1);
        randomGreedyAlgorithm.Optimize();

        UNIT_ASSERT_GT_C(World.GetScore().Double(), initialScore,
            TStringBuilder() << "\nFinal score less or equal then initial(" << World.GetScore() << " <= " << initialScore << ")\n");

        TMskuSeller seller(*World.GetMskus().front());

        for (size_t date = 0; date < 28; ++date) {
            seller.ProcessOneDay();
            if (date > 0) {
                UNIT_ASSERT_LE(std::abs(seller.TotalSale() - 100.), 1e9);
            }
        }
    }
    Y_UNIT_TEST(BigWriteOff)  // спрос 10, сг 7, квант 710. проверим что будем стабильно не заказывать
    {
        World.SetPlanningPeriod(28);
        World.SetStartDate(TDate("2020-06-01"));
        LoadFromYson("/market/replenishment/algorithms/vicugna/ut/test_lifetimes/test_data_quant_big");

        TRandomGreedyAlgorithm randomGreedyAlgorithm(World, 1000, 1);
        randomGreedyAlgorithm.Optimize();

        TMskuSeller seller(*World.GetMskus().front());

        for (size_t date = 0; date < 28; ++date) {
            seller.ProcessOneDay();
            UNIT_ASSERT_LE(std::abs(seller.TotalStockExcess()), 1e9);
        }
    }
}

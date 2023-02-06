#include <market/dynamic_pricing/deprecated/autostrategy/optimizer/margin_v1.h>
#include <market/dynamic_pricing/deprecated/autostrategy/price_randomizer/price_randomizer.h>

#include <library/cpp/logger/global/global.h>
#include <library/cpp/testing/unittest/gtest.h>

using namespace NMarket::NDynamicPricing;
using namespace NMarket::NDynamicPricing::NOptimizer;

namespace {
    const TMargin TARGET_MARGIN_THRESHOLD = 1e-4;

    void AddSku(
        TGroupSkuData& data,
        TMarketSkuId skuId, TPrice buyPrice, TStock stock,
        TMap<TPrice, TDemand>&& demand
    ) {
        TMarketSkuData msku;
        TShopSkuData ssku;
        ssku.BuyPrice = buyPrice;
        ssku.PurchasePrice = buyPrice;
        ssku.CurrentPrice = buyPrice;
        ssku.Stock = stock;
        msku.DemandForecast = std::move(demand);
        msku.ShopsData.push_back(ssku);
        data.emplace(skuId, msku);
    }

    const auto nullPriceRandomizer = NPriceRandomizer::TPriceRandomizer();
    const auto metricsCalculatorWithLimitedStock = NOptimizer::TMetricsCalculator();
}

TEST(TestPricingMarginV1, TestRealData_1)
{
    // Does not work now.
    InitGlobalLog2Null();

    TGroupSkuData data;
    {
        AddSku(
            data,
            1, 3544.300048828125, 2,
            {
                {3205, 11.589906},
                {3220, 11.416792},
                {3235, 11.19803},
                {3250, 10.981257},
                {3265, 10.748671},
                {3280, 10.501529},
                {3295, 10.261473},
                {3310, 9.958802},
                {3325, 9.620426},
                {3340, 9.294188},
                {3355, 8.972148},
                {3370, 8.67245},
                {3385, 8.364622}
            });
        AddSku(
            data,
            2, 2626.5, 11,
            {
                {2483, 4.837498},
                {2496, 4.837498},
                {2509, 4.837498},
                {2522, 4.823296},
                {2535, 4.785976},
                {2548, 4.716537},
                {2560, 4.61333},
                {2573, 4.524992},
                {2586, 4.429913},
                {2599, 4.315764},
                {2612, 4.187215}
            });
        AddSku(
            data,
            3, 1436.5, 16,
            {
                {1353, 1.045927},
                {1360, 1.022703},
                {1367, 0.999566},
                {1374, 0.978085},
                {1381, 0.955171},
                {1388, 0.930746},
                {1395, 0.904463},
                {1402, 0.874278},
                {1409, 0.843657},
                {1416, 0.81259},
                {1423, 0.777347},
                {1430, 0.743509},
            });
        AddSku(
            data,
            4, 2755, 3,
            {
                {2752, 1.608118},
                {2768, 1.559542},
                {2784, 1.518277},
                {2800, 1.481352},
                {2816, 1.449462},
                {2832, 1.417907},
                {2848, 1.38772},
                {2864, 1.356296},
                {2880, 1.324798},
                {2896, 1.292444},
                {2912, 1.257767}
            });
        AddSku(
            data,
            5, 4720, 0,
            {
                {4602, 4.304467},
                {4626, 4.169293},
                {4649, 4.034612},
                {4673, 3.920681},
                {4696, 3.809813},
                {4720, 3.694605},
                {4744, 3.592591},
                {4767, 3.487566},
                {4791, 3.399916},
                {4814, 3.326672},
                {4838, 3.27176}
            });
        AddSku(
            data,
            6, 3561.489990234375, 6,
            {
                {3383, 3.798534},
                {3402, 3.653512},
                {3421, 3.500937},
                {3439, 3.357037},
                {3458, 3.213738},
                {3477, 3.07004},
                {3496, 2.951407},
                {3515, 2.84307},
                {3533, 2.748525},
                {3552, 2.658061},
                {3571, 2.580124}
            });
        AddSku(
            data,
            7, 3808.800048828125, 442,
            {
                {3486, 9.057668},
                {3504, 8.958283},
                {3523, 8.819968},
                {3541, 8.648056},
                {3559, 8.471837},
                {3577, 8.255421},
                {3596, 8.042727},
                {3614, 7.795812},
                {3632, 7.53371},
                {3650, 7.233552},
                {3669, 6.910737},
                {3687, 6.568816}
            });
        AddSku(
            data,
            8, 6551.7998046875, 35,
            {
                {5903, 0.275902},
                {5933, 0.263111},
                {5962, 0.250363},
                {5992, 0.239575},
                {6021, 0.230826},
                {6051, 0.223195},
                {6080, 0.217137}
            });
        AddSku(
            data,
            9, 4666.490234375, 27,
            {
                {4484, 2.602852},
                {4507, 2.555665},
                {4530, 2.514369},
                {4552, 2.466801},
                {4575, 2.412584},
                {4597, 2.34678},
                {4620, 2.277185},
                {4642, 2.208113},
                {4665, 2.134331},
                {4687, 2.051862},
                {4710, 1.968965},
                {4732, 1.883245}
            });
        AddSku(
            data,
            10, 3816.489990234375, 25,
            {
                {3576, 1.646592},
                {3594, 1.621466},
                {3612, 1.596892},
                {3630, 1.571841},
                {3648, 1.546611},
                {3666, 1.518256},
                {3684, 1.489648},
                {3702, 1.459908},
                {3720, 1.42792},
                {3738, 1.397548},
                {3756, 1.366061},
                {3774, 1.335382},
                {3792, 1.303346}
            });
    }
    const TMargin targetMargin = -0.048;
    TMarginCalculatorV1 calculator(data, nullPriceRandomizer, metricsCalculatorWithLimitedStock, targetMargin, TARGET_MARGIN_THRESHOLD);
    const TOutputResult result = calculator.Calculate();
    // Was brute-forced.
    const TMap<TShopSkuId, TPrice> expected{
        {"1", 3385},
        {"2", 2548},
        {"3", 1381},
        {"4", 2752},
        {"5", 4720},
        {"6", 3383},
        {"7", 3559},
        {"8", 5903},
        {"9", 4575},
        {"10", 3702}
    };
    for (const auto&[id, price] : result.Prices) {
        EXPECT_NEAR(price.NewPrice, expected.Value(price.SkuData.ShopSkuId, -100), 1);
    }
}

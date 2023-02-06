#include <crypta/graph/rt/fp/model/catboost_applier.h>
#include <crypta/graph/rt/fp/model/proto/stats.pb.h>

#include <library/cpp/testing/unittest/registar.h>

#include <cmath>

namespace {
    NCrypta::THerschelStats GetHerschelStatsProto(ui32 gaidCount, ui32 idfaCount, ui32 yandexuidCount) {
        NCrypta::THerschelStats herschelStats;
        herschelStats.SetGaidCount(gaidCount);
        herschelStats.SetIdfaCount(idfaCount);
        herschelStats.SetYandexuidCount(yandexuidCount);
        return herschelStats;
    };


    static const TString goodIp = "2a02:6b8:b081:a516::1:19";
    static const TString goodUseragent = "Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:47.0) Gecko/20100101 Firefox/47.0";
    static const NCrypta::THerschelStats goodHerschelStats = GetHerschelStatsProto(1, 0, 0);

    static const TString badIp = "2001:14bb:c4:513::4258:b901";
    static const TString badUseragent = "Mozilla/5.0 (Linux; Android 12; SM-G998B Build/SP1A.210812.016; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/96.0.4664.61 Mobile Safari/537.36";
    static const NCrypta::THerschelStats badHerschelStats = GetHerschelStatsProto(100, 500, 0);
}

Y_UNIT_TEST_SUITE(THerschelCatboostApplier) {
    Y_UNIT_TEST(Apply) {
        NGeobase::TLookup lookup("geodata6.bin");
        NCrypta::THerschelCatboostApplierScore thresholds;
        thresholds.SetIp(0.5);
        thresholds.SetIpUseragent(0.0);
        NCrypta::THerschelCatboostApplier model(lookup, "mlh.bin", thresholds);

        const auto goodRes = model.Apply(goodIp, goodUseragent, goodHerschelStats);
        UNIT_ASSERT_EQUAL(true, goodRes.GetIp());
        UNIT_ASSERT_EQUAL(true, goodRes.GetIpUseragent());

        const auto badRes = model.Apply(badIp, badUseragent, badHerschelStats);
        UNIT_ASSERT_EQUAL(false, badRes.GetIp());
        UNIT_ASSERT_EQUAL(true, badRes.GetIpUseragent());
    }

    Y_UNIT_TEST(Score) {
        NGeobase::TLookup lookup("geodata6.bin");
        NCrypta::THerschelCatboostApplierScore thresholds;
        thresholds.SetIp(0.5);
        thresholds.SetIpUseragent(0.0);
        NCrypta::THerschelCatboostApplier model(lookup, "mlh.bin", thresholds);

        const auto goodRes = model.Score(goodIp, goodUseragent, goodHerschelStats);
        UNIT_ASSERT_DOUBLES_EQUAL(goodRes.GetIp(),  0.786, 0.001);
        UNIT_ASSERT_DOUBLES_EQUAL(goodRes.GetIpUseragent(),  0.828, 0.001);

        const auto badRes = model.Score(badIp, badUseragent, badHerschelStats);
        UNIT_ASSERT_DOUBLES_EQUAL(badRes.GetIp(),  0.0626, 0.0001);
        UNIT_ASSERT_DOUBLES_EQUAL(badRes.GetIpUseragent(),  0.0636, 0.0001);
    }
}

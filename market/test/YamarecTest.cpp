#include <market/library/recom/recom_src/RecomenderLoader.h>
#include <market/library/recom/recom_src/Yamarec.h>
#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/env.h>

using namespace Market;

TEST(YamarecTest, ParsesFileCorrectly)
{
    TYamarec yamarec;
    TSplitUserInfo userInfo;
    ParseYamarecConfig(SRC_("data"), false, EBackend::GURUDAEMON, [](const TString&){}, yamarec);

    userInfo.YandexUid = "123";
    auto data1 = yamarec.FormulaPlaces.GetPlaceData(EPlaceId::POPULAR_MODEL, userInfo);
    EXPECT_FALSE(data1.Empty());
    EXPECT_TRUE(data1->SpecialData.FormulaId == "TestFormula1");

    userInfo.YandexUid = "124";
    auto data2 = yamarec.FormulaPlaces.GetPlaceData(EPlaceId::POPULAR_MODEL, userInfo);
    EXPECT_TRUE(data2.Empty());

}

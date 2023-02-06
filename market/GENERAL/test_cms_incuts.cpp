#include "../cms_incuts.h"

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/env.h>

#include <util/system/file.h>


TEST(CMS_INCUT, LoadingFromFile) {
    const auto path = SRC_("data/cms_blender_incuts.pbsn");

    TStringStream errorMsg;
    NMarket::NMarketBlender::TCmsIncutsStorage storage(path, errorMsg);

    auto growingCashBackDesktop = storage.GrowingCashback.Get("desktop");
    EXPECT_TRUE(growingCashBackDesktop != nullptr);
    EXPECT_EQ(growingCashBackDesktop->OrdersCount, 3);
}

TEST(CMS_INCUT, CashBackInvalaidJsonAndCatalog) {
    const auto path = SRC_("data/invalid_cashback_cms_blender_incuts.pbsn");
    TStringStream errorMsg;
    NMarket::NMarketBlender::TCmsIncutsStorage storage(path, errorMsg);

    auto growingCashBackDesktop = storage.GrowingCashback.Get("phone");
    EXPECT_TRUE(growingCashBackDesktop == nullptr);

    auto quizIncutByHid = storage.CatalogQuiz.GetByHid(1);
    EXPECT_EQ(quizIncutByHid->Name, "name123");

    auto quizIncutByNid = storage.CatalogQuiz.GetByNid(1);
    EXPECT_EQ(quizIncutByNid->Name, "name123");

}

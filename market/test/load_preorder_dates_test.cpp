#include <market/library/preorder_dates/preorder_dates.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/env.h>

TEST(TestPreorderDates, ParseJsonFile) {
    using namespace NMarket::NPreorderDates;

    TPreorderDatesContainer testingPreorderDates;
    TPreorderDatesContainer productionPreorderDates;

    const auto testingPath = SRC_("./../../svn-data/package-data/preorder_dates.testing.json");
    EXPECT_NO_THROW(NReader::LoadPreorderDates(testingPath, testingPreorderDates));

    const auto productionPath = SRC_("./../../svn-data/package-data/preorder_dates.production.json");
    EXPECT_NO_THROW(NReader::LoadPreorderDates(productionPath, productionPreorderDates));
}

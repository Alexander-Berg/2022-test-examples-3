#include <market/library/bnpl_conditions/bnpl_conditions.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/env.h>


TEST(TestBnplConditions, ParseJsonFile) {
    using namespace NMarket::NBnplConditions;

    TBnplConditions conditions;

    const auto path = SRC_("./../../svn-data/package-data/bnpl_conditions.json");

    EXPECT_NO_THROW(NBnplReader::LoadBnplConditions(path, conditions));
}

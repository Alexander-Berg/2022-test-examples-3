#include <market/library/recom/src/CategoryFeatures.h>
#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/env.h>

using namespace Market;

#include <iostream>
TEST(CategoryFeaturesTest, ParsesFileCorrectly)
{
    auto features = LoadCategoryFeatures(SRC_("data/category.features"));
    EXPECT_EQ(std::distance(features->begin(), features->end()), 9);
    float val = (*features)[90407]["174:1_avg"];
    EXPECT_NEAR(118405.1802, val, 0.001);
}

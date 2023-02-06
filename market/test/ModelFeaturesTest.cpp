#include <market/library/recom/src/ModelFeatures.h>
#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/env.h>

using namespace Market;

TEST(ItemFeaturesTest, ParsesFileCorrectly)
{
    TItemFeatures features(SRC_("data/gurudaemon/model.features"), {"model_id", "category_id"});
    EXPECT_EQ(std::distance(features.begin(), features.end()), 9);

    using TFeaturePair = std::pair<size_t, TFeatures>;

    auto it = std::find_if(features.begin(), features.end(), [](const TFeaturePair& p) { return p.first == 11028534;});
    EXPECT_FALSE(it == features.end());
    auto minPriceIndex = features.GetFeatureIndex().find("min_price")->second;
    const int minPrice = it->second.at(minPriceIndex);
    EXPECT_EQ(minPrice, 1199);

    const auto& models = features.GetItemGroup("category_id", 160043);
    auto it2 = std::find_if(models.begin(), models.end(), [](size_t p) { return p == 10725078;});
    EXPECT_FALSE(it2 == models.end());
    auto it21 = std::find_if(features.begin(), features.end(), [](const TFeaturePair& p) { return p.first == 10725078;});
    auto offerCountIndex = features.GetFeatureIndex().find("offer_count")->second;
    const int offerCount = it21->second.at(offerCountIndex);
    EXPECT_EQ(offerCount, 1744);

    TItemFeatures features_woc(SRC_("data/gurudaemon/model_without_category.features"), {"model_id"});
    EXPECT_EQ(std::distance(features_woc.begin(), features_woc.end()), 10);

    auto it3 = std::find_if(features_woc.begin(), features_woc.end(), [](const TFeaturePair& p) { return p.first == 11028534;});
    EXPECT_FALSE(it3 == features_woc.end());
    auto opinionsTotalIndex = features.GetFeatureIndex().find("opinions_total")->second;
    const int opinionsTotal = it3->second.at(opinionsTotalIndex);
    EXPECT_EQ(opinionsTotal, 108);
}

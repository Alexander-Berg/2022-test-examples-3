#include <market/idx/offers/lib/iworkers/OfferCtx.h>

#include <market/idx/offers/processors/model_id_processor/model_validities.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/env.h>

TEST(TModelValiditiesTests, ReadingBlueModelIds)
{
    TModelValidities map(SRC_("data/blue_model_ids.gz"));

    ASSERT_EQ(4, map.size());

    TModelValidity one { 1234567, 123, true, false, false };
    TModelValidity two { 7654321, 123, true, false, false };
    TModelValidity three { 5555555, 123, true, false, false };
    TModelValidity four { 6666666, 123, false, true, false };

    auto validitiesEqual = [](const TModelValidity& one, const TModelValidity& two) {
        return
            one.ModelId == two.ModelId &&
            one.CategoryId == two.CategoryId &&
            one.IsPublishedOnMarket == two.IsPublishedOnMarket &&
            one.IsPublishedOnBlueMarket == two.IsPublishedOnBlueMarket
        ;
    };

    ASSERT_TRUE(validitiesEqual(map[1234567], one));
    ASSERT_TRUE(validitiesEqual(map[7654321], two));
    ASSERT_TRUE(validitiesEqual(map[5555555], three));
    ASSERT_TRUE(validitiesEqual(map[6666666], four));
}

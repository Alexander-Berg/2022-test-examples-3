#include <market/idx/models/lib/models-indexer/blue_offer_models.h>

#include <library/cpp/testing/unittest/gtest.h>

#include <util/folder/tempdir.h>
#include <util/stream/file.h>


TEST(BlueOfferModels, Simple) {
    TTempDir tmp("tmp");
    const TString blueOfferModelsFile = tmp() + "/merged_blue_offer_models.txt";

    {
        TUnbufferedFileOutput output(blueOfferModelsFile);
        output << "11111\n"
                  "22222\n";
    }

    NMarket::TBlueOfferModelsStorage blueOfferModelsStorage(blueOfferModelsFile);

    ASSERT_EQ(blueOfferModelsStorage.Size(), 2);
    ASSERT_TRUE(blueOfferModelsStorage.HasModel(11111));
    ASSERT_TRUE(blueOfferModelsStorage.HasModel(22222));
    ASSERT_FALSE(blueOfferModelsStorage.HasModel(44444));
}

TEST(BlueOfferModels, Repeated) {
    TTempDir tmp("tmp");
    const TString blueOfferModelsFile = tmp() + "/merged_blue_offer_models.txt";

    {
        TUnbufferedFileOutput output(blueOfferModelsFile);
        output << "11111\n"
                  "22222\n"
                  "\n"
                  "11111\n"
                  "33333\n";
    }

    NMarket::TBlueOfferModelsStorage blueOfferModelsStorage(blueOfferModelsFile);

    ASSERT_EQ(blueOfferModelsStorage.Size(), 3);
    ASSERT_TRUE(blueOfferModelsStorage.HasModel(11111));
    ASSERT_TRUE(blueOfferModelsStorage.HasModel(22222));
    ASSERT_TRUE(blueOfferModelsStorage.HasModel(33333));
}

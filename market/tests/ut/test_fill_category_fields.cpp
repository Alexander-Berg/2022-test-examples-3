#include <market/idx/offers/lib/iworkers/FillCategoryFields.cpp>
#include <market/idx/offers/lib/iworkers/Feeds.h>
#include <market/library/environment/environment.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/tests_data.h>

#include <util/system/fs.h>

class FilterAlcoTobacoTest: public ::testing::Test
{
public:
    void BaseTest(const FillCategoryFields::TCategoryToForbiddenRegionsMap& categ_to_forbidden_regions)
    {
        TFileInput categoryRestrictionsInput(
            ArcadiaSourceRoot() + "/market/idx/offers/yatf/resources/offers_indexer/stubs/category-restrictions.pb"
        );
        TCategoryRestrictions categoryRestrictions = TCategoryRestrictions(
            categoryRestrictionsInput,
            NCategoryTreeHelper::GetCategoryTree()
        );
        {
            MarketIndexer::GenerationLog::Record glRecord;
            TOfferCtx offerContext;

            glRecord.set_category_id(91359); // Спиртные напитки
            glRecord.add_int_regions(213); // Москва
            ASSERT_TRUE(
                    FilterOfferByCategory(&glRecord, &offerContext, categ_to_forbidden_regions, categoryRestrictions));
        }

        {
            MarketIndexer::GenerationLog::Record glRecord;
            TOfferCtx offerContext;

            glRecord.set_category_id(91359); // Спиртные напитки
            glRecord.add_int_regions(143); // Киев
            ASSERT_FALSE(
                    FilterOfferByCategory(&glRecord, &offerContext, categ_to_forbidden_regions, categoryRestrictions));
        }

        {
            MarketIndexer::GenerationLog::Record glRecord;
            TOfferCtx offerContext;

            glRecord.set_category_id(6069353); // Спиртные аксессуары
            glRecord.add_int_regions(213); // Москва
            glRecord.add_int_regions(143); // Киев 
            ASSERT_FALSE(
                    FilterOfferByCategory(&glRecord, &offerContext, categ_to_forbidden_regions, categoryRestrictions));
            Market::regions_list_type regions = {glRecord.int_regions().begin(), glRecord.int_regions().end()};
            ASSERT_TRUE(regions.find(REGION_MOSCOW) != regions.end());
            ASSERT_TRUE(regions.find(REGION_KIEV) != regions.end());
        }

        {
            MarketIndexer::GenerationLog::Record glRecord;
            TOfferCtx offerContext;

            glRecord.set_category_id(91359); // Спиртные напитки
            for (auto r : {213, 143}) { // Москва Киев
                glRecord.add_int_regions(r);
            }
            ASSERT_FALSE(
                    FilterOfferByCategory(&glRecord, &offerContext, categ_to_forbidden_regions, categoryRestrictions));
            Market::regions_list_type regions = {glRecord.int_regions().begin(), glRecord.int_regions().end()};
            ASSERT_TRUE(regions.find(REGION_MOSCOW) == regions.end());
            ASSERT_TRUE(regions.find(REGION_KIEV) != regions.end());
        }


        {
            MarketIndexer::GenerationLog::Record glRecord;
            TOfferCtx offerContext;

            glRecord.set_category_id(91363); // Водка
            glRecord.add_int_regions(213); // Москва
            glRecord.add_int_regions(143); // Киев
            ASSERT_FALSE(
                    FilterOfferByCategory(&glRecord, &offerContext, categ_to_forbidden_regions, categoryRestrictions));
            Market::regions_list_type regions = {glRecord.int_regions().begin(), glRecord.int_regions().end()};
            ASSERT_TRUE(regions.find(REGION_MOSCOW) == regions.end());
            ASSERT_TRUE(regions.find(REGION_KIEV) != regions.end());
        }

        {
            MarketIndexer::GenerationLog::Record glRecord;
            TOfferCtx offerContext;

            glRecord.set_category_id(91401); // Сигареты
            glRecord.add_int_regions(143); // Киев
            ASSERT_TRUE(
                    FilterOfferByCategory(&glRecord, &offerContext, categ_to_forbidden_regions, categoryRestrictions));
        }

        {
            MarketIndexer::GenerationLog::Record glRecord;
            TOfferCtx offerContext;

            glRecord.set_category_id(91401); // Сигареты
            for (auto r : {59, 10174, 213, 162, 40, 143}) { // В том числе и Киев
                glRecord.add_int_regions(r);
            }
            ASSERT_FALSE(
                    FilterOfferByCategory(&glRecord, &offerContext, categ_to_forbidden_regions, categoryRestrictions));
            Market::regions_list_type regions = {glRecord.int_regions().begin(), glRecord.int_regions().end()};
            ASSERT_TRUE(regions.find(REGION_KIEV) == regions.end());
        }

        {
            MarketIndexer::GenerationLog::Record glRecord;
            TOfferCtx offerContext;

            glRecord.set_category_id(91401); // Сигареты
            glRecord.add_int_regions(166); // СНГ
            ASSERT_FALSE(
                    FilterOfferByCategory(&glRecord, &offerContext, categ_to_forbidden_regions, categoryRestrictions));
            Market::regions_list_type regions = {glRecord.int_regions().begin(), glRecord.int_regions().end()};
            ASSERT_TRUE(regions.find(REGION_UKRAIN) == regions.end());
            ASSERT_TRUE(regions.find(REGION_RUSSIA) == regions.end());
            ASSERT_TRUE(regions.find(REGION_BELORUS) != regions.end());
            ASSERT_TRUE(regions.find(REGION_TURKMENIA) != regions.end());
        }

        {
            MarketIndexer::GenerationLog::Record glRecord;
            TOfferCtx offerContext;

            glRecord.set_category_id(5090510); // Портсигары
            glRecord.add_int_regions(213); // Москва
            glRecord.add_int_regions(143); // Киев
            ASSERT_FALSE(
                    FilterOfferByCategory(&glRecord, &offerContext, categ_to_forbidden_regions, categoryRestrictions));
            Market::regions_list_type regions = {glRecord.int_regions().begin(), glRecord.int_regions().end()};
            ASSERT_TRUE(regions.find(REGION_MOSCOW) == regions.end());
            ASSERT_TRUE(regions.find(REGION_KIEV) != regions.end());
        }
        {
            MarketIndexer::GenerationLog::Record glRecord;
            TOfferCtx offerContext;

            glRecord.set_category_id(91403); // Табачные аксессуары
            glRecord.add_int_regions(213); // Москва
            glRecord.add_int_regions(143); // Киев
            ASSERT_FALSE(
                    FilterOfferByCategory(&glRecord, &offerContext, categ_to_forbidden_regions, categoryRestrictions));
            Market::regions_list_type regions = {glRecord.int_regions().begin(), glRecord.int_regions().end()};
            ASSERT_TRUE(regions.find(REGION_MOSCOW) == regions.end());
            ASSERT_TRUE(regions.find(REGION_KIEV) != regions.end());
        }

        {
            MarketIndexer::GenerationLog::Record glRecord;
            TOfferCtx offerContext;

            glRecord.set_category_id(90531); // Аптека -> Оправы для очков
            glRecord.add_int_regions(149); // Беларусь
            glRecord.add_int_regions(157); // Минск
            ASSERT_TRUE(
                    FilterOfferByCategory(&glRecord, &offerContext, categ_to_forbidden_regions, categoryRestrictions));
            Market::regions_list_type regions = {glRecord.int_regions().begin(), glRecord.int_regions().end()};
            ASSERT_TRUE(regions.find(REGION_BELORUS) == regions.end());
            ASSERT_TRUE(regions.find(REGION_MINSK) == regions.end());
        }

    }
private:
    virtual void SetUp()
    {
        // Ugly as hell. Gotta get rid of those singletons someday...
        InitCategoryTree(SRC_("data/tovar-tree.pb"));
        GEO.loadTree(SRC_("geobase/geo.c2p").c_str());
        GEO.loadInfo(SRC_("geobase/geobase.xml").c_str());
        GEO.cache_heatup();
        NCategoryTreeHelper::InitCategoryTree(SRC_("data/tovar-tree.pb"));
    }
};


TEST_F(FilterAlcoTobacoTest, test_filter_offer_by_categ_from_file)
{
    {
        const auto filename = SRC_("data/forbidden_category_regions.xml");
        ASSERT_TRUE(NFs::Exists(TString(filename)));
        auto forbiddenCats = ParseForbiddenCatRegions(filename);
        BaseTest(forbiddenCats);
    }
}


TEST_F(FilterAlcoTobacoTest, test_filter_offer_by_categ_invalid_path_to_file)
{
    {
        ASSERT_THROW(
            ParseForbiddenCatRegions("file_which_is_not_exist.xml"), // invalid file name
            TFileError
        );
    }
}

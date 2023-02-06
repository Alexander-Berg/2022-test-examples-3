#include <market/idx/offers/lib/iworkers/OfferCtx.h>
#include <market/idx/offers/lib/iworkers/OfferDocumentBuilder.h>
#include <market/library/trees/category_tree_helper.h>
#include <market/library/snappy-protostream/lenval_stream.h>
#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/tests_data.h>

class AdultCategoryTest: public ::testing::Test
{
public:
    void BaseTest() {
        const auto& tree = NCategoryTreeHelper::GetCategoryTree();
        {

            TGlRecord glRecord;
            glRecord.set_category_id(90531);
            CheckAdultCategory(&glRecord, tree.FindNode(90531));
            ASSERT_FALSE(glRecord.flags() & NMarket::NDocumentFlags::ADULT);
        }

        {
            TGlRecord glRecord;
            glRecord.set_category_id(tree.GetRoot().GetId());
            CheckAdultCategory(&glRecord, &tree.GetRoot());
            ASSERT_FALSE(glRecord.flags() & NMarket::NDocumentFlags::ADULT);
        }

        {
            TGlRecord glRecord;
            glRecord.set_category_id(Market::BOOKS_FOR_ADULTS_CATEG_ID);
            CheckAdultCategory(&glRecord, tree.FindNode(Market::BOOKS_FOR_ADULTS_CATEG_ID));
            ASSERT_TRUE(glRecord.flags() & NMarket::NDocumentFlags::ADULT);
        }

        {
            TGlRecord glRecord;
            glRecord.set_category_id(Market::GOODS_FOR_ADULTS_CATEG_ID);
            CheckAdultCategory(&glRecord, tree.FindNode(Market::GOODS_FOR_ADULTS_CATEG_ID));
            ASSERT_TRUE(glRecord.flags() & NMarket::NDocumentFlags::ADULT);
        }
    }
private:
    virtual void SetUp()
    {
        NCategoryTreeHelper::InitCategoryTree(SRC_("data/tovar-tree.pb"));
    }
};

TEST_F(AdultCategoryTest, test_adult_category)
{
    BaseTest();
}

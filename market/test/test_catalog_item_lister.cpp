#include <market/library/market_servant/logger/logger.h>
#include <market/library/snappy-protostream/mbo_stream.h>
#include <market/proto/content/mbo/MboParameters.pb.h>

#include <market/cataloger/src/components/user_data.h>
#include <market/cataloger/src/navigation/catalog_item_lister.h>
#include <market/cataloger/src/navigation/output.h>

#include <library/cpp/testing/unittest/gtest.h>

#include <util/system/tempfile.h>

#include <iostream>
#include <string>


static const TString MAGIC = "MBOC";

class TTestCatalogLister
{
public:
    TTestCatalogLister()
    {
        theLogger::Instance().SetBinLogLevel(LogLevel::ERROR ^ LogLevel::WARN);
        lister_ptr = new catalog_item_lister();
        lister_ptr->load_catalog("data/catalog.xml");
        lister_ptr->load_toca("data/data.xml");

        TTempFileHandle tmpFileWithCats;
        {
            NMarket::NMbo::TWriter writer(tmpFileWithCats.Name(), MAGIC);
            using TCategoryProto = Market::Mbo::Parameters::Category;
            TCategoryProto a;
            a.set_hid(91491);
            a.set_goods_return_rules("test1");
            writer.Write(a);
            TCategoryProto b;
            b.set_hid(91464);
            b.set_goods_return_rules("test2");
            writer.Write(b);
        }
        lister_ptr->load_attributes_from_pb(tmpFileWithCats.Name());
        lister_ptr->SetRecursiveInfo();
    }

    ~TTestCatalogLister()
    {
        delete lister_ptr;
    }

    const catalog_item_lister& Get()
    {
        return *lister_ptr;
    }

private:
    catalog_item_lister* lister_ptr;
};

static const catalog_item_lister& lister()
{
    static TTestCatalogLister test_lister;
    return test_lister.Get();
}

TEST(TestCatalogItemLister, GenPathTree)
{
    const catalog_item* item = lister().get_item_ref_by_id(90401);
    ASSERT_TRUE(item != nullptr);
    ASSERT_EQ(item->id(), 90401);
    ASSERT_EQ(item->parent_id(), 0);

    const std::string root0 = generate_tree(item, 0, UserData(), false);
    ASSERT_EQ(generate_tree(item, -1, UserData(), false), root0);
    const std::string path91491 = generate_path(lister().get_item_ref_by_id(91491), UserData(), false);
    ASSERT_NE(path91491 , "");
}

TEST(TestCatalogItemLister, Attributes)
{
    const catalog_item* item = lister().get_item_ref_by_id(91491);
    const catalog_item::TAttributeVector& attrs91491 = item->getAttributes();
    ASSERT_TRUE(attrs91491.size() == 1);
    ASSERT_EQ(attrs91491[0].first, "goods_return_rules");
    ASSERT_EQ(attrs91491[0].second, "test1");

    item = lister().get_item_ref_by_id(91464);
    const catalog_item::TAttributeVector& attrs91464 = item->getAttributes();
    ASSERT_TRUE(attrs91464.size() == 1);
    ASSERT_EQ(attrs91464[0].first, "goods_return_rules");
    ASSERT_EQ(attrs91464[0].second, "test2");
}

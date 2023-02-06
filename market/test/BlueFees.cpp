#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/env.h>
#include <market/library/snappy-protostream/mbo_stream.h>
#include <market/proto/content/mbo/MboParameters.pb.h>
#include <market/report/library/global/blue_fee/blue_fee.h>
#include <market/report/library/relevance/money/blue_fee.h>
#include <util/folder/tempdir.h>
#include <util/stream/file.h>

using namespace NMarket::NReport;
using TMboCategory = Market::Mbo::Parameters::Category;
using EOutputType = Market::Mbo::Parameters::OutputType;

void WriteTextFile(TString text, TString path) {
    TFileOutput file(path);
    file.Write(text);
    file.Flush();
}

class BlueFees : public ::testing::Test {
protected:
    void SetUp() override {
        NMarket::NMbo::TWriter writer("tovar-tree.pb", "MBOC");

        {
            TMboCategory category;
            category.set_hid(10);
            category.set_tovar_id(0);
            category.set_output_type(EOutputType::SIMPLE);
            category.set_published(true);

            auto uniqueName = category.add_unique_name();
            uniqueName->set_name("");
            uniqueName->set_lang_id(225);

            auto name = category.add_name();
            name->set_name("Все товары");
            name->set_lang_id(225);

            writer.Write(category);
        }

        {
            TMboCategory category;
            category.set_hid(11);
            category.set_tovar_id(1);
            category.set_parent_hid(10);
            category.set_output_type(EOutputType::SIMPLE);
            category.set_published(true);

            auto uniqueName = category.add_unique_name();
            uniqueName->set_name("");
            uniqueName->set_lang_id(225);

            auto name = category.add_name();
            name->set_name("level1");
            name->set_lang_id(225);

            writer.Write(category);
        }

        {
            TMboCategory category;
            category.set_hid(12);
            category.set_tovar_id(2);
            category.set_parent_hid(11);
            category.set_output_type(EOutputType::SIMPLE);
            category.set_published(true);

            auto uniqueName = category.add_unique_name();
            uniqueName->set_name("");
            uniqueName->set_lang_id(225);

            auto name = category.add_name();
            name->set_name("level2");
            name->set_lang_id(225);

            writer.Write(category);
        }
    }
};

TEST(BlueFees, Load) {
    TTempDir temporaryDirectory("tmp");
    auto path = temporaryDirectory() + "/supplier-category-fee.xml";
    WriteTextFile(R"(<?xml version='1.0' encoding='UTF-8'?>)"
                  R"(<supplierCategoryFees>)"
                  R"(    <fee type="FEE" hyper_id="1" supplier_id="2" value="3"/>)"
                  R"(    <fee type="FEE" hyper_id="10" supplier_id="" value="30"/>)"
                  R"(    <fee type="AGENT" hyper_id="100" supplier_id="20" value="300"/>)"
                  R"(    <fee type="AGENT" hyper_id="1000" supplier_id="" value="3000"/>)"
                  R"(</supplierCategoryFees>)", path);
    NMarketReport::NGlobal::LoadBlueFee(path);
    ASSERT_EQ(3, NMarketReport::NGlobal::BlueFee().at(1));
    ASSERT_EQ(30, NMarketReport::NGlobal::BlueFee().at(10));
    ASSERT_EQ(300, NMarketReport::NGlobal::BlueFee().at(100));
    ASSERT_EQ(3000, NMarketReport::NGlobal::BlueFee().at(1000));
}

TEST_F(BlueFees, GetBlueFeeForCategory) {
    const auto categories = Market::CreateCategoryTreeFromProtoFile("tovar-tree.pb");
    NMarket::NReport::TBlueFee blueFee;
    blueFee.insert(std::make_pair(10, 10));
    blueFee.insert(std::make_pair(11, 20));
    blueFee.insert(std::make_pair(12, 30));
    ASSERT_EQ(30, ::GetBlueFeeForCategory(12, blueFee, *categories).GetOrElse(-1));
}

TEST_F(BlueFees, GetBlueFeeForCategory_UnexistentCategory) {
    const auto categories = Market::CreateCategoryTreeFromProtoFile("tovar-tree.pb");
    NMarket::NReport::TBlueFee blueFee;
    blueFee.insert(std::make_pair(10, 10));
    blueFee.insert(std::make_pair(11, 20));
    blueFee.insert(std::make_pair(12, 30));
    ASSERT_FALSE(::GetBlueFeeForCategory(1, blueFee, *categories));
}

TEST_F(BlueFees, GetBlueFeeForCategory_Inheritance1) {
    const auto categories = Market::CreateCategoryTreeFromProtoFile("tovar-tree.pb");
    NMarket::NReport::TBlueFee blueFee;
    blueFee.insert(std::make_pair(10, 10));
    blueFee.insert(std::make_pair(11, 20));
    ASSERT_EQ(20, ::GetBlueFeeForCategory(12, blueFee, *categories).GetOrElse(-1));
}

TEST_F(BlueFees, GetBlueFeeForCategory_Inheritance2) {
    const auto categories = Market::CreateCategoryTreeFromProtoFile("tovar-tree.pb");
    NMarket::NReport::TBlueFee blueFee;
    blueFee.insert(std::make_pair(10, 10));
    ASSERT_EQ(10, ::GetBlueFeeForCategory(12, blueFee, *categories).GetOrElse(-1));
}


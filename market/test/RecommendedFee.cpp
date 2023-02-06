#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/env.h>
#include <market/library/snappy-protostream/mbo_stream.h>
#include <market/proto/content/mbo/MboParameters.pb.h>
#include <util/folder/tempdir.h>
#include <util/stream/file.h>
#include <util/generic/maybe.h>

#include <market/report/library/money/load_fees/recommended_fee.h>

using namespace NMarketReport;
using TMboCategory = Market::Mbo::Parameters::Category;
using EOutputType = Market::Mbo::Parameters::OutputType;

class ReservePriceFee : public ::testing::Test {
protected:
    void SetUp() override {
        NMarket::NMbo::TWriter writer("tovar-tree.pb", "MBOC");

        {
            TMboCategory category;
            category.set_hid(90401);
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
            category.set_hid(1);
            category.set_tovar_id(1);
            category.set_parent_hid(90401);
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
            category.set_hid(2);
            category.set_tovar_id(2);
            category.set_parent_hid(1);
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

namespace{
    void PrepareMockFile(TString text, TString path) {
        TFileOutput file(path);
        file.Write(text);
        file.Flush();
    }
}

TEST(RecommendedFee, Load) {
    TTempDir temporaryDirectory("tmp");
    const TString recommendedBids = temporaryDirectory.Path() / "recommended_bids.tsv";

    PrepareMockFile("90401\t0.07\n2\t0.05\n", recommendedBids);
    NMarketReport::LoadRecommendedFeeMap(recommendedBids);

    ASSERT_EQ(500, NMarketReport::GetRecommendedFee(2).GetOrElse(-1));
    ASSERT_EQ(700, NMarketReport::GetRecommendedFee(1).GetOrElse(-1));
}

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/env.h>
#include <market/library/snappy-protostream/mbo_stream.h>
#include <market/proto/content/mbo/MboParameters.pb.h>
#include <util/folder/tempdir.h>
#include <util/stream/file.h>
#include <util/generic/maybe.h>

#include <market/report/library/money/load_fees/reserve_price_fee.h>

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

TEST(ReservePriceFee, Load) {
    TTempDir temporaryDirectory("tmp");
    const TString reservePriceFeeFile = temporaryDirectory.Path() / "reserve_price.tsv";

    PrepareMockFile("90401\t0.06\n2\t0.04\n", reservePriceFeeFile);
    NMarketReport::LoadReservePriceFeeMap(reservePriceFeeFile);

    ASSERT_EQ(400, NMarketReport::GetReservePriceFee(2).GetOrElse(-1));
    ASSERT_EQ(600, NMarketReport::GetReservePriceFee(1).GetOrElse(-1));
}

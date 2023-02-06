#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/env.h>
#include <market/library/snappy-protostream/mbo_stream.h>
#include <market/library/trees/category_tree.h>
#include <market/proto/content/mbo/MboParameters.pb.h>
#include <market/report/library/relevance/money/bid_correction.h>

#include <functional>

namespace {

using namespace NMarket::NReport;
using namespace NMarket::NBidCorrection;
using TMboCategory = Market::Mbo::Parameters::Category;
using EOutputType = Market::Mbo::Parameters::OutputType;
using TKey = NVersion2::TDataSegment<NMms::TMmapped>;

class TReaderMock : public TReaderStub {
public:
    THashMap<TKey, TData> Data;

    const TMaybe<TData::TReference> GetData(const TKey& segment) const override {
        const auto found = Data.find(segment);
        if (found) {
            return std::cref(found->second);
        } else {
            return Nothing();
        }
    }

    bool DataIsLoaded() const override {
        return true;
    }
};

class BidCorrection : public ::testing::Test {
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
            name->set_name("level1");
            name->set_lang_id(225);

            writer.Write(category);
        }

        {
            TMboCategory category;
            category.set_hid(3);
            category.set_tovar_id(3);
            category.set_parent_hid(2);
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

void CheckCalculateBidCorrection(
    const IReader& reader,
    const TClientId clientId,
    const NMarketReport::THyperCategoryId categoryId,
    const Market::TRegionIdList& geoTreePath,
    const TPpId ppId,
    const TString domain,
    const double expectedBidCoefficient)
{
    const auto categories = Market::CreateCategoryTreeFromProtoFile("tovar-tree.pb");
    const auto coefficient = CalculateBidCorrection(reader, *categories, clientId, categoryId, geoTreePath, ppId, domain, nullptr);
    EXPECT_DOUBLE_EQ(expectedBidCoefficient, coefficient);
}

void CheckCalculateBidCorrection(
    const IReader& reader,
    const TClientId clientId,
    const NMarketReport::THyperCategoryId categoryId,
    const Market::TRegionIdList& geoTreePath,
    const TPpId ppId,
    const double expectedBidCoefficient)
{
    CheckCalculateBidCorrection(reader, clientId, categoryId, geoTreePath, ppId, "", expectedBidCoefficient);
}

TEST_F(BidCorrection, CalculateBidCorrection_smoke) {
    TReaderMock reader;
    reader.Data[TKey{{1, 2, 3, 4}, ""}] = {0.523f};
    reader.ConfigStub.GeoGroups[33] = 4;
    reader.ConfigStub.PpGroups[44] = 3;

    CheckCalculateBidCorrection(reader, 1, 2, {33}, 44, 0.523f);
}

TEST_F(BidCorrection, CalculateBidCorrection_GeoGroups) {
    TReaderMock reader;
    reader.Data[TKey{{11, 1, 13, 4}, ""}] = {0.34f};
    reader.Data[TKey{{11, 1, 13, 5}, ""}] = {0.3f};
    reader.ConfigStub.GeoGroups[2] = 4;
    reader.ConfigStub.GeoGroups[3] = 5;
    reader.ConfigStub.PpGroups[113] = 13;

    CheckCalculateBidCorrection(reader, 11, 1, {1, 2, 3}, 113, 0.34f);
    CheckCalculateBidCorrection(reader, 11, 1, {3, 2, 1}, 113, 0.3f);
    CheckCalculateBidCorrection(reader, 11, 1, {7, 8, 9}, 113, 1.0f);
}

TEST_F(BidCorrection, CalculateBidCorrection_GeoMissing) {
    TReaderMock reader;
    reader.Data[TKey{{21, 1, 23, 24}, ""}] = {0.8f};
    reader.Data[TKey{{21, 1, 23, 0}, ""}] = {0.7f};
    reader.ConfigStub.GeoGroups[111] = 24;
    reader.ConfigStub.PpGroups[222] = 23;

    CheckCalculateBidCorrection(reader, 21, 1, {333}, 222, 0.7f);
}

TEST_F(BidCorrection, CalculateBidCorrection_ClientMissing) {
    TReaderMock reader;
    reader.Data[TKey{{13, 1, 14, 4}, ""}] = {0.11f};
    reader.Data[TKey{{DEFAULT_CLIENT, 1, 14, 4}, ""}] = {0.22f};
    reader.ConfigStub.GeoGroups[21] = 4;
    reader.ConfigStub.PpGroups[134] = 14;

    CheckCalculateBidCorrection(reader, 12, 1, {21}, 134, 0.22f);
}

TEST_F(BidCorrection, CalculateBidCorrection_CategoryMissing) {
    TReaderMock reader;
    reader.Data[TKey{{176, Market::ROOT_MARKET_CATEG_ID, 149, 4}, ""}] = {0.12f};
    reader.ConfigStub.GeoGroups[91] = 4;
    reader.ConfigStub.PpGroups[1149] = 149;

    CheckCalculateBidCorrection(reader, 176, 22, {91}, 1149, 0.12f);
}

TEST_F(BidCorrection, CalculateBidCorrection_CategoryAndClientMissing) {
    TReaderMock reader;
    reader.Data[TKey{{123, 1, 17, 44}, ""}] = {0.122f};
    reader.Data[TKey{{DEFAULT_CLIENT, Market::ROOT_MARKET_CATEG_ID, 17, 44}, ""}] = {0.42f};
    reader.ConfigStub.GeoGroups[111] = 44;
    reader.ConfigStub.PpGroups[177] = 17;

    CheckCalculateBidCorrection(reader, 321, 22, {111}, 177, 0.42f);
}

TEST_F(BidCorrection, CalculateBidCorrection_PpGroups) {
    TReaderMock reader;
    reader.Data[TKey{{21, 1, 33, 31}, ""}] = {0.1};
    reader.Data[TKey{{21, 1, 44, 31}, ""}] = {0.2};
    reader.ConfigStub.GeoGroups[131] = 31;
    reader.ConfigStub.PpGroups[6] = 33;
    reader.ConfigStub.PpGroups[7] = 44;

    CheckCalculateBidCorrection(reader, 21, 1, {131}, 6, 0.1f);
    CheckCalculateBidCorrection(reader, 21, 1, {131}, 7, 0.2f);
    CheckCalculateBidCorrection(reader, 21, 1, {131}, 8, 1.0f);
}

TEST_F(BidCorrection, CalculateBidCorrection_Domain) {
    TReaderMock reader;
    reader.Data[TKey{{21, 1, 33, 31}, "testdomain.ru"}] = {0.1231};
    reader.ConfigStub.GeoGroups[131] = 31;
    reader.ConfigStub.PpGroups[6] = 33;

    CheckCalculateBidCorrection(reader, 21, 1, {131}, 6, "testdomain.ru", 0.1231f);
}

TEST_F(BidCorrection, CalculateBidCorrection_MissingDomain) {
    TReaderMock reader;
    reader.Data[TKey{{21, 1, 33, 31}, "testdomain.ru"}] = {0.1231};
    reader.ConfigStub.GeoGroups[131] = 31;
    reader.ConfigStub.PpGroups[6] = 33;

    CheckCalculateBidCorrection(reader, 21, 1, {131}, 6, "testdomain.com", 1.0f);
}

TEST_F(BidCorrection, CalculateBidCorrection_MissingDomainFallbackToEmptyDomain) {
    TReaderMock reader;
    reader.Data[TKey{{21, 1, 33, 31}, "testdomain.ru"}] = {0.1231};
    reader.Data[TKey{{21, 1, 33, 31}, ""}] = {0.3232};
    reader.ConfigStub.GeoGroups[131] = 31;
    reader.ConfigStub.PpGroups[6] = 33;

    CheckCalculateBidCorrection(reader, 21, 1, {131}, 6, "testdomain.com", 0.3232f);
}

TEST_F(BidCorrection, CalculateBidCorrection_MissingDomainFallbackToParentCategory) {
    TReaderMock reader;
    reader.Data[TKey{{21, 0, 33, 31}, "testdomain.com"}] = {0.1231};
    reader.Data[TKey{{21, 1, 33, 31}, ""}] = {0.3232};
    reader.Data[TKey{{21, 2, 33, 31}, ""}] = {0.3444};
    reader.ConfigStub.GeoGroups[131] = 31;
    reader.ConfigStub.PpGroups[6] = 33;

    CheckCalculateBidCorrection(reader, 21, 2, {131}, 6, "testdomain.com", 0.3444f);
}

}

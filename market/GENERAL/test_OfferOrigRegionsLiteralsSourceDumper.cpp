#include <market/idx/generation/genlog_dumper/dumpers/OfferOrigRegionsLiteralsSourceDumper.h>

#include <market/library/flat_helpers/flat_helpers.h>
#include <market/library/mmap_versioned/mmap.h>
#include <library/cpp/testing/unittest/gtest.h>
#include <util/folder/path.h>
#include <util/folder/tempdir.h>

using namespace MarketIndexer::GenerationLog;

class TOfferOrigRetionsLiteralsSourceTest : public ::NTesting::TTest {
public:
    TTempDir Dir;

public:
    explicit TOfferOrigRetionsLiteralsSourceTest() = default;
    virtual ~TOfferOrigRetionsLiteralsSourceTest() = default;

    TString GetFileName() {
        return Dir.Path() / "offer-orig-regions-literals-source.fb";
    }

protected:
    virtual void SetUp() override;
};

void TOfferOrigRetionsLiteralsSourceTest::SetUp() {
    NDumpers::TDumperContext context(Dir.Name(), false);

    auto dumper = NDumpers::MakeOfferOrigRegionsLiteralsSourceDumper(context);

    Record record00;
    record00.set_orig_regions_literals_source(Record_SearchLiteralRegionsSource_BUCKETS);

    Record record02;

    Record record03;
    record03.set_orig_regions_literals_source(Record_SearchLiteralRegionsSource_SHOPS_DAT);

    Record record04;
    record04.set_orig_regions_literals_source(Record_SearchLiteralRegionsSource_EXTERNAL_TABLE);

    dumper->ProcessGenlogRecord(record00, 0);
    dumper->ProcessGenlogRecord(record02, 2);
    dumper->ProcessGenlogRecord(record03, 3);
    dumper->ProcessGenlogRecord(record04, 4);

    dumper->Finish();
}

TEST_F(TOfferOrigRetionsLiteralsSourceTest, CheckTmpDir) {
    ASSERT_TRUE(TOfferOrigRetionsLiteralsSourceTest::Dir.Path().Exists());
}

TEST_F(TOfferOrigRetionsLiteralsSourceTest, CheckOutputFileExists) {
    ASSERT_TRUE(TFsPath(GetFileName()).Exists());
}

TEST_F(TOfferOrigRetionsLiteralsSourceTest, CheckFileContents) {
    const auto mmaped = MakeHolder<Market::MMap>(GetFileName().c_str(), PROT_READ, MAP_PRIVATE, 0, Market::EMMapWarmup::YES);
    ASSERT_NE(mmaped, nullptr);
    const auto data = NMarket::NFlatbufferHelpers::GetTOffersRegionsSourceVec(*mmaped.Get());
    ASSERT_NE(data, nullptr);

    ASSERT_EQ(data->OfferRegionsSourceFlag()->size(), 5);
    uint8_t expectedContents[] = {
        0x1,
        0x0, // default value
        0x0, // default value
        0x2,
        0x4
    };

    for (size_t i = 0;i < data->OfferRegionsSourceFlag()->size(); ++i) {
        ASSERT_EQ(data->OfferRegionsSourceFlag()->Get(i), expectedContents[i]);
    }
}
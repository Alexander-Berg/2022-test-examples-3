#include <market/idx/generation/genlog_dumper/dumpers/BookNowDumper.h>

#include <market/library/book_now/reader.h>

#include <library/cpp/testing/unittest/gtest.h>

#include <util/folder/tempdir.h>

namespace {
    const THashSet<i32> BookNowFeeds{1001, 1002};

    struct TOfferWithOutlets {
        Market::NBookNow::TOfferLocalId OfferID;
        Market::NBookNow::TOfferOutlets Outlets;

        std::weak_ordering operator<=>(const TOfferWithOutlets& other) const {
            return OfferID <=> other.OfferID;
        }
    };

    struct TOfferOutletsSaver {
        TOfferOutletsSaver(TVector<TOfferWithOutlets>& offers)
            : Offers(offers) {}

        void operator()(Market::NBookNow::TOfferLocalId offerId,
                        const Market::NBookNow::TOfferOutlets& outlets) const {
            Offers.emplace_back(TOfferWithOutlets{offerId, outlets});
        }
    public:
        TVector<TOfferWithOutlets>& Offers;
    };

    TVector<TOfferWithOutlets> DumpRecords(const TVector<MarketIndexer::GenerationLog::Record>& records) {
        TTempDir dir;
        NDumpers::TDumperContext context(dir.Name(), false);
        auto dumper = NDumpers::MakeBookNowDumper(context, BookNowFeeds);
        for (size_t i = 0; i < records.size(); ++i) {
            dumper->ProcessGenlogRecord(records[i], i);
        }
        dumper->Finish();
        auto reader = Market::NBookNow::CreateOfferBookingInfoReader(
            Market::NMmap::IMemoryRegion::MmapFile(dir.Path() / "book_now_offer.mmap"));
        TVector<TOfferWithOutlets> result;
        TOfferOutletsSaver saver(result);
        reader->EnumerateAllOffers(saver);
        Sort(result);
        return result;
    }

    // N.B. We can set only fields from EXPECTED_GENLOG_FIELDS
    void AddRecord(TVector<MarketIndexer::GenerationLog::Record>& records,
                   const ui32 feedID,
                   const TVector<ui32>& outletsData) {
        // Coz outlets_data is interleaved data: [PointID, RegionID, InStock, ...]
        ASSERT_EQ(outletsData.size() % 3, 0);

        records.emplace_back();
        MarketIndexer::GenerationLog::Record& record = records.back();
        record.set_feed_id(feedID);
        record.mutable_outlets_data()->Add(outletsData.begin(), outletsData.end());
    }
}

TEST(BookNowDumper, Empty) {
    auto result = DumpRecords({});
    ASSERT_EQ(result.size(), 0);
}

TEST(BookNowDumper, NotBookNowRecord) {
    TVector<MarketIndexer::GenerationLog::Record> records;
    AddRecord(records, 1000, {1, 2, 3});

    auto result = DumpRecords(records);
    ASSERT_EQ(result.size(), 0);
}

TEST(BookNowDumper, BookNowRecordWithoutOutletsData) {
    TVector<MarketIndexer::GenerationLog::Record> records;
    AddRecord(records, 1001, {});

    auto result = DumpRecords(records);
    ASSERT_EQ(result.size(), 1);

    {
        TOfferWithOutlets data = result[0];
        ASSERT_EQ(data.OfferID, 0);
        ASSERT_EQ(data.Outlets.size(), 0);
    }
}

TEST(BookNowDumper, BookNowRecordWithOutletsData) {
    TVector<MarketIndexer::GenerationLog::Record> records;
    AddRecord(records, 1001, {1, 2, 3});

    auto result = DumpRecords(records);
    ASSERT_EQ(result.size(), 1);

    {
        TOfferWithOutlets data = result[0];
        ASSERT_EQ(data.OfferID, 0);
        TVector<Market::NBookNow::TOnStockInfo>& outlets = data.Outlets;
        ASSERT_EQ(outlets.size(), 1);
        ASSERT_EQ(outlets[0].Amount, 2);
        ASSERT_EQ(outlets[0].OutletId, 1);
        ASSERT_EQ(outlets[0].YmlDate, 3);
    }
}

TEST(BookNowDumper, ManyRecords) {
    TVector<MarketIndexer::GenerationLog::Record> records;
    AddRecord(records, 1001, {1, 2, 3, 4, 5, 6});
    AddRecord(records, 1000, {7, 8, 9});
    AddRecord(records, 1002, {10, 11, 12});

    auto result = DumpRecords(records);
    ASSERT_EQ(result.size(), 2);

    {
        TOfferWithOutlets data = result[0];
        ASSERT_EQ(data.OfferID, 0);
        TVector<Market::NBookNow::TOnStockInfo>& outlets = data.Outlets;
        ASSERT_EQ(outlets.size(), 2);

        ASSERT_EQ(outlets[0].Amount, 2);
        ASSERT_EQ(outlets[0].OutletId, 1);
        ASSERT_EQ(outlets[0].YmlDate, 3);

        ASSERT_EQ(outlets[1].Amount, 5);
        ASSERT_EQ(outlets[1].OutletId, 4);
        ASSERT_EQ(outlets[1].YmlDate, 6);
    }

    {
        TOfferWithOutlets data = result[1];
        ASSERT_EQ(data.OfferID, 2);
        ASSERT_EQ(data.Outlets.size(), 1);
        TVector<Market::NBookNow::TOnStockInfo>& outlets = data.Outlets;
        ASSERT_EQ(outlets.size(), 1);
        ASSERT_EQ(outlets[0].Amount, 11);
        ASSERT_EQ(outlets[0].OutletId, 10);
        ASSERT_EQ(outlets[0].YmlDate, 12);
    }
}

#include <market/idx/generation/genlog_dumper/dumpers/AllDumpers.h>
#include <market/idx/generation/genlog_dumper/dumpers/DumperGenlogFields.h>
#include <market/idx/generation/genlog_dumper/dumpers/QBidReportInstructionFinalizer.h>

#include <market/library/flat_helpers/flat_helpers.h>

#include <market/flat/indexer/AmoreData.fbs.h>
#include <market/flat/indexer/BaseOfferProps.fbs.h>
#include <market/flat/indexer/BaseOfferPropsExt.fbs64.h>
#include <market/flat/indexer/BidsTimestamps.fbs.h>
#include <market/library/cmagicid/cmagicid.h>
#include <market/library/currency_exchange/currency_exchange.h>
#include <market/library/interface/indexer_report_interface.h>
#include <market/library/libshopsdat/shopsdat.h>
#include <market/library/libsku/common.h>
#include <market/library/libsku/reader.h>
#include <market/library/local_delivery_mms/reader.h>
#include <market/library/offer_hash_id/offer_hash_id.h>
#include <market/library/regional_delivery_mms/reader.h>
#include <market/library/taxes/taxes.h>
#include <market/proto/feedparser/OffersData.pb.h>
#include <market/qpipe/qbid/qbidengine/dumper.h>

#include <library/cpp/string_utils/base64/base64.h>
#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/gtest.h>

#include <util/folder/path.h>
#include <util/folder/tempdir.h>
#include <util/generic/hash.h>
#include <util/generic/string.h>
#include <util/stream/output.h>
#include <util/string/hex.h>
#include <util/string/split.h>
#include <util/string/vector.h>
#include <util/system/fstat.h>

#include <fstream>
#include <limits>
#include <string>
#include <vector>

using TAddBucketInfoMethod = ::delivery_calc::mbi::BucketInfo *(delivery_calc::mbi::OffersDeliveryInfo::*)();

template <typename T>
T ReadStream(std::ifstream& input)
{
    T data;
    input.read((char*)&data, sizeof(data));
    return data;
}

NDumpers::TBidInfo ReadBid(std::ifstream& input)
{
    return ReadStream<NDumpers::TBidInfo>(input);
}

uint8_t ReadBidFlags(std::ifstream& input)
{
    return ReadStream<uint8_t>(input);
}

static Market::TMinBids CreateMinBids(Market::TMinBids::TBidValue minBid, Market::TMinBids::TBidValue minFee) {
    Market::TMinBids res;
    res.YBid = minBid;
    res.MBid = minBid;
    res.CBid = minBid;
    res.Fee = minFee;
    return res;
}

TEST(ListDumperRequiredFields, testSimple)
{
    THashSet<TString> columns;
    ASSERT_NO_THROW(columns = NDumpers::GetAllDumpersGenlogColumns());
    ASSERT_FALSE(columns.empty());
}

TEST(BidsDumperTests, testDumping)
{
    TVector<MarketIndexer::GenerationLog::Record> records;

    const auto addNewRecord = [&records](const Market::TMinBids minBids) {
        records.emplace_back();
        MarketIndexer::GenerationLog::Record& record = records.back();

        auto genlog_min_bids = record.mutable_minbids();
        genlog_min_bids->set_ybid(minBids.YBid);
        genlog_min_bids->set_mbid(minBids.MBid);
        genlog_min_bids->set_cbid(minBids.CBid);
        genlog_min_bids->set_fee(minBids.Fee);
    };

    {
        addNewRecord(CreateMinBids(1, 11));
        auto& record = records.back();

        record.set_bid(1301);
        record.set_cbid(1302);
        record.set_mbid(0);
        record.set_fee(1303);
        record.set_dont_pull_up_bids(1);
    }

    {
        addNewRecord(CreateMinBids(2, 22));
        auto& record = records.back();

        record.set_bid(1401);
        record.set_cbid(1402);
        record.set_mbid(0);
        record.set_fee(1403);
        record.set_dont_pull_up_bids(1);
    }

    {
        addNewRecord(CreateMinBids(3, 33));
        auto& record = records.back();

        record.set_bid(1501);
        record.set_cbid(1502);
        record.set_mbid(0);
        record.set_fee(1503);
        record.set_dont_pull_up_bids(0);
    }

    {
        addNewRecord(CreateMinBids(4, 44));
        auto& record = records.back();

        record.set_bid(1601);
        record.set_cbid(1602);
        record.set_mbid(0);
        record.set_fee(1603);
        record.set_dont_pull_up_bids(0);
    }

    TTempDir dir;

    NDumpers::TDumperContext context(dir.Name(), false);

    auto bidsDumper = NDumpers::MakeBidsDumper(context);
    auto minimalBidsDumper = NDumpers::MakeMinimalBidsDumper(context);

    bidsDumper->ProcessGenlogRecord(records[0], 0);
    minimalBidsDumper->ProcessGenlogRecord(records[0], 0);

    bidsDumper->ProcessGenlogRecord(records[1], 1);
    minimalBidsDumper->ProcessGenlogRecord(records[1], 1);

    bidsDumper->ProcessGenlogRecord(records[2], 2);
    minimalBidsDumper->ProcessGenlogRecord(records[2], 2);

    bidsDumper->ProcessGenlogRecord(records[3], 4);
    minimalBidsDumper->ProcessGenlogRecord(records[3], 4);

    bidsDumper->Finish();
    minimalBidsDumper->Finish();

    // verify data
    std::ifstream flagFile((dir.Path() / NDumpers::BIDS_FLAGS_FILE).c_str(),
            std::ifstream::in | std::ifstream::binary);

    Market::MMap MB((dir.Path() / NDumpers::MINIMAL_BIDS2_FILE).c_str(),
                    PROT_READ, MAP_PRIVATE);

    const Market::TMinBids* mb = static_cast<const Market::TMinBids*>(MB.data());

    uint8_t flags;

    ASSERT_EQ(1, mb[0].YBid);
    ASSERT_EQ(1, mb[0].MBid);
    ASSERT_EQ(1, mb[0].CBid);

    ASSERT_EQ(2, mb[1].YBid);
    ASSERT_EQ(2, mb[1].MBid);
    ASSERT_EQ(2, mb[1].CBid);

    ASSERT_EQ(3, mb[2].YBid);
    ASSERT_EQ(3, mb[2].MBid);
    ASSERT_EQ(3, mb[2].CBid);

    ASSERT_EQ(4, mb[4].YBid);
    ASSERT_EQ(4, mb[4].MBid);
    ASSERT_EQ(4, mb[4].CBid);

    flags = ReadBidFlags(flagFile);
    ASSERT_EQ(flags, 1);

    flags = ReadBidFlags(flagFile);
    ASSERT_EQ(flags, 1);

    flags = ReadBidFlags(flagFile);
    ASSERT_EQ(flags, 0);

    flags = ReadBidFlags(flagFile);
    ASSERT_EQ(flags, 0);
}

TEST(BidsTsTest, testDumping)
{
    using namespace NMarket::NIndexer::NOfferCollection;

    MarketIndexer::GenerationLog::Record record1;
    {
        record1.mutable_bids_timestamps()->set_bid_ts(50);
        record1.mutable_bids_timestamps()->set_fee_ts(50);
        record1.mutable_bids_timestamps()->set_dont_pull_up_bids_ts(50);
    }

    MarketIndexer::GenerationLog::Record record2;
    {
        record2.mutable_bids_timestamps()->set_bid_ts(100);
        record2.mutable_bids_timestamps()->set_fee_ts(100);
        record2.mutable_bids_timestamps()->set_dont_pull_up_bids_ts(100);
    }

    MarketIndexer::GenerationLog::Record record3;
    {
        record3.mutable_bids_timestamps()->set_bid_ts(150);
        record3.mutable_bids_timestamps()->set_fee_ts(150);
        record3.mutable_bids_timestamps()->set_dont_pull_up_bids_ts(150);
    }

    MarketIndexer::GenerationLog::Record record4;
    {
        record4.mutable_bids_timestamps()->set_bid_ts(350);
        record4.mutable_bids_timestamps()->set_fee_ts(350);
        record4.mutable_bids_timestamps()->set_dont_pull_up_bids_ts(350);
    }

    TTempDir dir;
    NDumpers::TDumperContext context(dir.Name(), false);
    auto bidsTsDumper = NDumpers::MakeBidsTimestampsDumper(context);

    bidsTsDumper->ProcessGenlogRecord(record1, 0);
    bidsTsDumper->ProcessGenlogRecord(record2, 1);
    bidsTsDumper->ProcessGenlogRecord(record3, 3);
    bidsTsDumper->ProcessGenlogRecord(record4, 5);

    bidsTsDumper->Finish();

    const TString inputFile = dir.Path() / "bids-timestamps.fb";

    ASSERT_TRUE(TFsPath(inputFile).Exists());

    TBlob dataBlob = TBlob::FromFile(inputFile);
    const auto data = GetTBidsTimestampsVec(dataBlob.AsUnsignedCharPtr());

    ASSERT_NE(data, nullptr);
    ASSERT_NE(data->BidsTimestamps(), nullptr);
    ASSERT_EQ(data->BidsTimestamps()->size(), 6);

    const auto& bidsTimestamps = *data->BidsTimestamps();

    ASSERT_EQ(50, bidsTimestamps[0]->Bid());
    ASSERT_EQ(50, bidsTimestamps[0]->Fee());
    ASSERT_EQ(50, bidsTimestamps[0]->DontPullUpBids());

    ASSERT_EQ(100, bidsTimestamps[1]->Bid());
    ASSERT_EQ(100, bidsTimestamps[1]->Fee());
    ASSERT_EQ(100, bidsTimestamps[1]->DontPullUpBids());

    ASSERT_EQ(0, bidsTimestamps[2]->Bid());
    ASSERT_EQ(0, bidsTimestamps[2]->Fee());
    ASSERT_EQ(0, bidsTimestamps[2]->DontPullUpBids());

    ASSERT_EQ(150, bidsTimestamps[3]->Bid());
    ASSERT_EQ(150, bidsTimestamps[3]->Fee());
    ASSERT_EQ(150, bidsTimestamps[3]->DontPullUpBids());

    ASSERT_EQ(0, bidsTimestamps[4]->Bid());
    ASSERT_EQ(0, bidsTimestamps[4]->Fee());
    ASSERT_EQ(0, bidsTimestamps[4]->DontPullUpBids());

    ASSERT_EQ(350, bidsTimestamps[5]->Bid());
    ASSERT_EQ(350, bidsTimestamps[5]->Fee());
    ASSERT_EQ(350, bidsTimestamps[5]->DontPullUpBids());
}

TEST(AmoreDataTest, testDumping)
{
    using namespace NMarket::NIndexer::NOfferCollection;
    TVector<MarketIndexer::GenerationLog::Record> records;

    {
        records.emplace_back();
        MarketIndexer::GenerationLog::Record& record = records.back();
        record.set_amore_data("cpodrrpos0");
        record.set_amore_beru_supplier_data("cpodrrpos0-bs");
        record.set_amore_beru_vendor_data("cpodrrpos0-bv");
    }

    {
        records.emplace_back();
        MarketIndexer::GenerationLog::Record& record = records.back();
        record.set_amore_data("cpodrrpos1");
        record.set_amore_beru_supplier_data("cpodrrpos1-bs");
        record.set_amore_beru_vendor_data("cpodrrpos1-bv");
    }

    {
        records.emplace_back();
        MarketIndexer::GenerationLog::Record& record = records.back();
        record.set_amore_data("cpodrrpos2");
        record.set_amore_beru_supplier_data("cpodrrpos2-bs");
        record.set_amore_beru_vendor_data("cpodrrpos2-bv");
    }

    {
        records.emplace_back();
        MarketIndexer::GenerationLog::Record& record = records.back();
        record.set_amore_data("cpodrrpos3");
        record.set_amore_beru_supplier_data("cpodrrpos3-bs");
        record.set_amore_beru_vendor_data("cpodrrpos3-bv");
    }

    {
        records.emplace_back();
        MarketIndexer::GenerationLog::Record& record = records.back();
        record.set_amore_data("cpodrrpos4");
        record.set_amore_beru_supplier_data("cpodrrpos4-bs");
        record.set_amore_beru_vendor_data("cpodrrpos4-bv");
    }

    TTempDir dir;

    NDumpers::TDumperContext context(dir.Name(), false);
    auto amoreDataDumper = NDumpers::MakeAmoreDataDumper(context);

    for (size_t i = 0; i < records.size(); ++i) {
        amoreDataDumper->ProcessGenlogRecord(records[i], i);
    }

    amoreDataDumper->Finish();

    const TString inputFile = dir.Path() / "amore-data.fb";

    ASSERT_TRUE(TFsPath(inputFile).Exists());

    TBlob dataBlob = TBlob::FromFile(inputFile);
    const auto data = GetTAmoreDataVec(dataBlob.AsUnsignedCharPtr());

    ASSERT_NE(data, nullptr);
    ASSERT_NE(data->AmoreData(), nullptr);
    ASSERT_EQ(data->AmoreData()->size(), 5);
    ASSERT_NE(data->AmoreBeruSupplierData(), nullptr);
    ASSERT_EQ(data->AmoreBeruSupplierData()->size(), 5);
    ASSERT_NE(data->AmoreBeruVendorData(), nullptr);
    ASSERT_EQ(data->AmoreBeruVendorData()->size(), 5);

    {
        const auto& part = *data->AmoreData();
        TString part_0 = reinterpret_cast<const char*>(part[0]->Value()->data());
        ASSERT_STREQ("cpodrrpos0", part_0.substr(0, part[0]->Value()->size()));
        TString part_1 = reinterpret_cast<const char*>(part[1]->Value()->data());
        ASSERT_STREQ("cpodrrpos1", part_1.substr(0, part[1]->Value()->size()));
        TString part_3 = reinterpret_cast<const char*>(part[3]->Value()->data());
        ASSERT_STREQ("cpodrrpos3", part_3.substr(0, part[3]->Value()->size()));
        TString part_4 = reinterpret_cast<const char*>(part[4]->Value()->data());
        ASSERT_STREQ("cpodrrpos4", part_4.substr(0, part[4]->Value()->size()));
    }
    {
        const auto& part = *data->AmoreBeruSupplierData();
        TString part_0 = reinterpret_cast<const char*>(part[0]->Value()->data());
        ASSERT_STREQ("cpodrrpos0-bs", part_0.substr(0, part[0]->Value()->size()));
        TString part_1 = reinterpret_cast<const char*>(part[1]->Value()->data());
        ASSERT_STREQ("cpodrrpos1-bs", part_1.substr(0, part[1]->Value()->size()));
        TString part_3 = reinterpret_cast<const char*>(part[3]->Value()->data());
        ASSERT_STREQ("cpodrrpos3-bs", part_3.substr(0, part[3]->Value()->size()));
        TString part_4 = reinterpret_cast<const char*>(part[4]->Value()->data());
        ASSERT_STREQ("cpodrrpos4-bs", part_4.substr(0, part[4]->Value()->size()));
    }
    {
        const auto& part = *data->AmoreBeruVendorData();
        TString part_0 = reinterpret_cast<const char*>(part[0]->Value()->data());
        ASSERT_STREQ("cpodrrpos0-bv", part_0.substr(0, part[0]->Value()->size()));
        TString part_1 = reinterpret_cast<const char*>(part[1]->Value()->data());
        ASSERT_STREQ("cpodrrpos1-bv", part_1.substr(0, part[1]->Value()->size()));
        TString part_3 = reinterpret_cast<const char*>(part[3]->Value()->data());
        ASSERT_STREQ("cpodrrpos3-bv", part_3.substr(0, part[3]->Value()->size()));
        TString part_4 = reinterpret_cast<const char*>(part[4]->Value()->data());
        ASSERT_STREQ("cpodrrpos4-bv", part_4.substr(0, part[4]->Value()->size()));
    }
}

TEST(InstructionDumperTest, testDumping)
{
    TVector<MarketIndexer::GenerationLog::Record> records;

    const auto addNewRecord = [&records](ui32 feed_id, const TString& offer_id) {
        records.emplace_back();
        MarketIndexer::GenerationLog::Record& record = records.back();
        record.set_feed_id(feed_id);
        record.set_offer_id(offer_id);
    };

    addNewRecord(5234, "34");
    addNewRecord(5235, "35");
    addNewRecord(5442, "42");
    addNewRecord(7788, "hehe88");
    addNewRecord(9093, "haha93");
    addNewRecord(9094, "haha94");

    TTempDir dir;

    NDumpers::TDumperContext context(dir.Name(), false);
    auto qBidDumper = NDumpers::MakeQBidReportInstructionDumper(context);

    qBidDumper->ProcessGenlogRecord(records[0], 0);
    qBidDumper->ProcessGenlogRecord(records[1], 1);
    qBidDumper->ProcessGenlogRecord(records[2], 2);
    qBidDumper->ProcessGenlogRecord(records[3], 3);
    qBidDumper->ProcessGenlogRecord(records[4], 4);
    qBidDumper->ProcessGenlogRecord(records[5], 5);

    qBidDumper->Finish();

    Market::MMap digitOF((dir.Path() /
                         NDumpers::TMP_FEEDID_OFFERDIGITID_FILE).c_str(),
                         PROT_READ, MAP_PRIVATE);

    const NQBid::NSearch::TFeedIdOfferDigitId* record =
            static_cast<const NQBid::NSearch::TFeedIdOfferDigitId*>(digitOF.data());
    ASSERT_EQ(sizeof(*record) * 3, digitOF.length());
    ASSERT_EQ(5234, record->FeedId);
    ASSERT_EQ(34,   record->OfferDigitId);
    ASSERT_EQ(0,    record->SequenceNumber);

    ++record;
    ASSERT_EQ(5235, record->FeedId);
    ASSERT_EQ(35,   record->OfferDigitId);
    ASSERT_EQ(1,    record->SequenceNumber);

    ++record;
    ASSERT_EQ(5442, record->FeedId);
    ASSERT_EQ(42,   record->OfferDigitId);
    ASSERT_EQ(2,    record->SequenceNumber);

    Market::MMap stringOF((dir.Path() /
                          NDumpers::TMP_FEEDID_OFFERID_FILE).c_str(),
                          PROT_READ, MAP_PRIVATE);
    Market::MMap strings((dir.Path() /
                         NDumpers::TMP_OFFERID_STRINGS_FILE).c_str(),
                         PROT_READ, MAP_PRIVATE);
    const char* offerid_strings = static_cast<const char*>(strings.data());

    const NQBid::NSearch::TFeedIdOfferOffsetId* srecord =
            static_cast<const NQBid::NSearch::TFeedIdOfferOffsetId*>(stringOF.data());

    ASSERT_EQ(7788, srecord->FeedId);
    ASSERT_STREQ("hehe88", offerid_strings + srecord->OfferOffsetId);
    ASSERT_EQ(3, srecord->SequenceNumber);

    ++srecord;
    ASSERT_EQ(9093, srecord->FeedId);
    ASSERT_STREQ("haha93", offerid_strings + srecord->OfferOffsetId);
    ASSERT_EQ(4, srecord->SequenceNumber);

    ++srecord;
    ASSERT_EQ(9094, srecord->FeedId);
    ASSERT_STREQ("haha94", offerid_strings + srecord->OfferOffsetId);
    ASSERT_EQ(5, srecord->SequenceNumber);

    NDumpers::Finalize(context);

    Market::MMap search_part((dir.Path() /
                             NDumpers::FEEDID_OFFERID_RESULT_FILE).c_str(),
                             PROT_READ, MAP_PRIVATE);
    const size_t* psize = static_cast<const size_t*>(search_part.data());
    ASSERT_EQ(3 * sizeof(NQBid::NSearch::TFeedIdOfferDigitId), *psize);

    record = reinterpret_cast<const NQBid::NSearch::TFeedIdOfferDigitId*>(psize + 1);
    ASSERT_EQ(5234, record->FeedId);
    ASSERT_EQ(34,   record->OfferDigitId);
    ASSERT_EQ(0,    record->SequenceNumber);

    ++record;
    ASSERT_EQ(5235, record->FeedId);
    ASSERT_EQ(35,   record->OfferDigitId);
    ASSERT_EQ(1,    record->SequenceNumber);

    ++record;
    ASSERT_EQ(5442, record->FeedId);
    ASSERT_EQ(42,   record->OfferDigitId);
    ASSERT_EQ(2,    record->SequenceNumber);

    ++record;
    psize = reinterpret_cast<const size_t*>(record);
    ASSERT_EQ(3 * sizeof(NQBid::NSearch::TFeedIdOfferOffsetId), *psize);

    srecord = reinterpret_cast<const NQBid::NSearch::TFeedIdOfferOffsetId*>(psize + 1);
    offerid_strings = reinterpret_cast<const char*>(srecord) + *psize + sizeof(size_t);


    ASSERT_EQ(7788, srecord->FeedId);
    ASSERT_STREQ("hehe88", offerid_strings + srecord->OfferOffsetId);
    ASSERT_EQ(3, srecord->SequenceNumber);

    ++srecord;
    ASSERT_EQ(9093, srecord->FeedId);
    ASSERT_STREQ("haha93", offerid_strings + srecord->OfferOffsetId);
    ASSERT_EQ(4, srecord->SequenceNumber);

    ++srecord;
    ASSERT_EQ(9094, srecord->FeedId);
    ASSERT_STREQ("haha94", offerid_strings + srecord->OfferOffsetId);
    ASSERT_EQ(5, srecord->SequenceNumber);
}

TEST(TWareMD5Dumper, CreationFile)
{
    /* dir preparation */
    TTempDir dir;

    NDumpers::TDumperContext context(dir.Name(), false);
    auto wareMD5Dumper = NDumpers::MakeWareMD5Dumper(context);

    // 8 records
    const std::tuple<const char*, uint32_t> TestInput[] = {
        {"-0xSrPmAWnZSutGvW1YrQQ,,", 0},
        {"-1hhDxW9lyYqfvK4bMze9g,,", 1},
        {"-2tyRCWj1U3kh0zq-nseQw,,", 2},
        {"-3BwDkm6aj61hW1dArlNRQ,,", 4},
    };

    for (const auto& [wareMd5, sequenceNumber] : TestInput) {
        std::array<char, 16> binaryWareMD5;
        Base64Decode(binaryWareMD5.begin(), wareMd5, wareMd5 + 24);

        MarketIndexer::GenerationLog::Record record;
        record.set_binary_ware_md5(binaryWareMD5.data(), binaryWareMD5.size());

        wareMD5Dumper->ProcessGenlogRecord(record, sequenceNumber);
    }
    wareMD5Dumper->Finish();

    TUnbufferedFileInput f0(dir.Path() / "ware_md5.values.binary");

    char buffer[16];

    // f0

    f0.Load(buffer, 16);
    ASSERT_EQ(std::get<0>(TestInput[0]), Base64EncodeUrl(TStringBuf(buffer, 16)));

    f0.Load(buffer, 16);
    ASSERT_EQ(std::get<0>(TestInput[1]), Base64EncodeUrl(TStringBuf(buffer, 16)));

    f0.Load(buffer, 16);
    ASSERT_EQ(std::get<0>(TestInput[2]), Base64EncodeUrl(TStringBuf(buffer, 16)));

    f0.Load(buffer, 16);  // skip over an empty slot

    f0.Load(buffer, 16);
    ASSERT_EQ(std::get<0>(TestInput[3]), Base64EncodeUrl(TStringBuf(buffer, 16)));
}


TEST(TSkuDumper, CreationFile)
{
    /* dir preparation */
    TTempDir dir;

    NDumpers::TDumperContext context(dir.Name(), false);

    // SkuDumper is now part of RegionalDeliveryDumper
    const TString calendarPath = ArcadiaSourceRoot() + "/market/idx/offers/tests/ut/data/delivery_holidays.xml";
    auto regDeliveryDumper = NDumpers::MakeRegionalDeliveryDumper(context, calendarPath);

    for (int i = 0; i < 16; ++i) {
        MarketIndexer::GenerationLog::Record record;
        record.set_feed_id(i * 2);
        record.set_offer_id(ToString<int>(i));
        record.set_sc_sku(TString(18, 'A' + i));
        record.set_shop_id(371);
        record.set_fulfillment_shop_id(12345);
        record.set_shop_sku(TString(18, 'K' + i));
        record.mutable_delivery_offset()->set_days_from(i);
        record.mutable_delivery_offset()->set_days_to(i * 2);

        regDeliveryDumper->ProcessGenlogRecord(record, i);
    }

    regDeliveryDumper->Finish();

    const TString path(dir.Path() / "offer_sku.mmap");

    auto CheckContentFile = [&](const TString &path)
    {
        using namespace NMarket::NSKU;
        auto reader = CreateOfferSKUReader(
            Market::NMmap::IMemoryRegion::MmapFile(path));

        const auto sampleFlagsMask =
            NMarket::NDocumentFlags::PICKUP |
            NMarket::NDocumentFlags::POST_TERM |
            NMarket::NDocumentFlags::STORE |
            NMarket::NDocumentFlags::CPA |
            NMarket::NDocumentFlags::AVAILABLE |
            NMarket::NDocumentFlags::FULFILLMENT_LIGHT;

        for (int i = 0; i < 16; ++i)
        {
            auto skuAccessor = reader->GetOfferSKUDataAccessor(i);
            ASSERT_EQ(true, skuAccessor.operator bool());

            ASSERT_EQ(sampleFlagsMask , skuAccessor->GetOfferFlagsMask());

            // ShopSku have to be able always
            ASSERT_STREQ(TString(18, 'K' + i), skuAccessor->GetShopSKU());

            ASSERT_EQ(i, skuAccessor->GetDeliveryOffset().DaysFrom);
            ASSERT_EQ(i * 2, skuAccessor->GetDeliveryOffset().DaysTo);

        }
    };

    CheckContentFile(path);
}


TEST(TSkuDumper, DumpContexMsku)
{
    /* dirs preparation */
    TTempDir dir;

    NDumpers::TDumperContext context(dir.Name(), false);

    // SkuDumper is now part of RegionalDeliveryDumper
    const TString calendarPath = ArcadiaSourceRoot() + "/market/idx/offers/tests/ut/data/delivery_holidays.xml";
    auto regDeliveryDumper = NDumpers::MakeRegionalDeliveryDumper(context, calendarPath);

    TVector<MarketIndexer::GenerationLog::Record> records;

    // White offer without contex_info
    {
        records.emplace_back();
        MarketIndexer::GenerationLog::Record& record = records.back();
        record.set_shop_sku("1");
        auto deliveryOffset = record.mutable_delivery_offset();
        deliveryOffset->set_days_from(1);
        deliveryOffset->set_days_to(5);
    }
    // Blue offer without contex_info
    {
        records.emplace_back();
        MarketIndexer::GenerationLog::Record& record = records.back();
        record.set_flags(NMarket::NDocumentFlags::BLUE_OFFER);
        record.set_shop_sku("2");
        record.set_market_sku(1002);
    }
    // Fake msku without contex_info
    {
        records.emplace_back();
        MarketIndexer::GenerationLog::Record& record = records.back();
        record.set_flags(NMarket::NDocumentFlags::MARKET_SKU);
        record.set_shop_sku("3");
        record.set_market_sku(1003);
    }
    // White offer with contex_info
    {
        records.emplace_back();
        MarketIndexer::GenerationLog::Record& record = records.back();
        record.set_shop_sku("4");
        auto deliveryOffset = record.mutable_delivery_offset();
        deliveryOffset->set_days_from(10);
        deliveryOffset->set_days_to(15);
        auto contexInfo = record.mutable_contex_info();
        contexInfo->set_experiment_id("white");
        contexInfo->set_original_msku_id(2004);
    }
    // Blue offer with contex_info
    {
        records.emplace_back();
        MarketIndexer::GenerationLog::Record& record = records.back();
        record.set_flags(NMarket::NDocumentFlags::BLUE_OFFER);
        record.set_shop_sku("5");
        record.set_market_sku(1005);
        auto contexInfo = record.mutable_contex_info();
        contexInfo->set_experiment_id("blue");
        contexInfo->set_original_msku_id(2005);
    }
    // Fake msku with contex_info
    {
        records.emplace_back();
        MarketIndexer::GenerationLog::Record& record = records.back();
        record.set_flags(NMarket::NDocumentFlags::MARKET_SKU);
        record.set_shop_sku("6");
        record.set_market_sku(1006);
        auto contexInfo = record.mutable_contex_info();
        contexInfo->set_experiment_id("fake");
        contexInfo->set_original_msku_id(2006);
    }

    for (size_t i = 0; i < records.size(); ++i) {
        regDeliveryDumper->ProcessGenlogRecord(records[i], i);
    }
    regDeliveryDumper->Finish();

    const TString path(dir.Path() / "offer_sku.mmap");

    auto reader = NMarket::NSKU::CreateOfferSKUReader(Market::NMmap::IMemoryRegion::MmapFile(path));

    // White offer without contex_info
    {
        auto skuAccessor = reader->GetOfferSKUDataAccessor(0);
        ASSERT_EQ(true, skuAccessor.operator bool());
        EXPECT_EQ("", skuAccessor->GetSKU());
        EXPECT_EQ("1", skuAccessor->GetShopSKU());
    }
    // Blue offer without contex_info
    {
        auto skuAccessor = reader->GetOfferSKUDataAccessor(1);
        ASSERT_EQ(true, skuAccessor.operator bool());
        EXPECT_EQ("1002", skuAccessor->GetSKU());
        EXPECT_EQ("2", skuAccessor->GetShopSKU());
    }
    // Fake maku without contex_info
    {
        auto skuAccessor = reader->GetOfferSKUDataAccessor(2);
        ASSERT_EQ(true, skuAccessor.operator bool());
        EXPECT_EQ("1003", skuAccessor->GetSKU());
        EXPECT_EQ("3", skuAccessor->GetShopSKU());
    }
    // White offer with contex_info
    {
        auto skuAccessor = reader->GetOfferSKUDataAccessor(3);
        ASSERT_EQ(true, skuAccessor.operator bool());
        EXPECT_EQ("", skuAccessor->GetSKU());
        EXPECT_EQ("4", skuAccessor->GetShopSKU());
    }
    // Blue offer with contex_info
    {
        auto skuAccessor = reader->GetOfferSKUDataAccessor(4);
        ASSERT_EQ(true, skuAccessor.operator bool());
        EXPECT_EQ("2005", skuAccessor->GetSKU());
        EXPECT_EQ("5", skuAccessor->GetShopSKU());
    }
    // Fake maku with contex_info
    {
        auto skuAccessor = reader->GetOfferSKUDataAccessor(5);
        ASSERT_EQ(true, skuAccessor.operator bool());
        EXPECT_EQ("2006", skuAccessor->GetSKU());
        EXPECT_EQ("6", skuAccessor->GetShopSKU());
    }
}


TEST(TOfferContentDumper, CreationFile)
{
    /* dir preparation */
    TTempDir dir;

    NDumpers::TDumperContext context(dir.Name(), false);
    auto offerContentDumper = NDumpers::MakeOfferContentDumper(context);


    // 8 records
    const char* TestInput[] = {
            "-0xSrPmAWnZSutGvW1YrQQ", // 0
            "-1hhDxW9lyYqfvK4bMze9g", // 1
            "-2tyRCWj1U3kh0zq-nseQw", // 2
            "-3BwDkm6aj61hW1dArlNRQ", // 3

            "-5gkjQmfTbQ2kuX7G4Aw3w", // 4
            "-8G2JA34espBndysTExPfQ", // 5
            "-AkwxTpvFWLqoXxvkfaEJQ", // 6
            "-BQjh8rLuAkwQOZ3sIeq-Q", // 7
    };

    for (int i = 0; i < 8; ++i)
    {
        MarketIndexer::GenerationLog::Record record;
        record.set_ware_md5(TestInput[i]);
        record.set_feed_id(i * 2);
        record.set_offer_id(ToString<int>(i));

        offerContentDumper->ProcessGenlogRecord(record, i);
    }

    offerContentDumper->Finish();

    TUnbufferedFileInput f(dir.Path() / "content-offer.tsv");

    auto CheckContentFile = [&](TUnbufferedFileInput &f) {
        TString line;
        size_t i = 0;
        while (f.ReadLine(line)) {
            TString ware_md5;
            size_t feed_id;
            TString offer_id;
            size_t partNumber;

            Split(line, '\t', ware_md5, feed_id, offer_id, partNumber);

            ASSERT_EQ(TestInput[i], ware_md5);
            ASSERT_EQ(2 * i,  feed_id);
            ASSERT_EQ(ToString(i), offer_id);
            ASSERT_EQ(0, partNumber);
            ++i;
        }
    };

    CheckContentFile(f);
}


const char META_CONTENT[] = R"(7
BYN
EUR
KZT
RUR
UAH
USD
)";


void CheckBasePropertiesForShard(
    const size_t number_of_offers,
    const uint32_t offset,
    TUnbufferedFileInput& fb)
{
    using TBasePropRecord = NMarket::NIndexer::NOfferCollection::TBaseProperties;
    using TOfferBasePropRecord = NMarket::NIndexer::NOfferCollection::TOfferBaseProperties;

    size_t offer_index = 0;
    const TString fileData = fb.ReadAll();
    const TBasePropRecord* basePropRecord = NMarket::NFlatbufferHelpers::GetTBaseProperties(fileData);

    ASSERT_TRUE(basePropRecord);
    const flatbuffers::Vector<flatbuffers::Offset<TOfferBasePropRecord>>* baseProps = basePropRecord->BaseProperties();
    ASSERT_TRUE(baseProps);
    ASSERT_EQ(baseProps->size(), number_of_offers + offset);
    for (size_t index = 0; index < baseProps->size(); ++index) {
        if (index < offset) {
            continue;
        }

        const TOfferBasePropRecord *offerBaseProp = baseProps->Get(index);
        auto expectedPrice = static_cast<uint64_t>(100 * offer_index);

        ASSERT_TRUE(offerBaseProp != nullptr);
        ASSERT_EQ(offerBaseProp->Price(), expectedPrice);
        ASSERT_EQ(offerBaseProp->OldPrice()->Value(), 100 * offer_index);
        ASSERT_EQ(offerBaseProp->CurrencyId(), 3); //3 == RUR
        ASSERT_EQ(offerBaseProp->HistoryPrice()->Value(), 100 * offer_index);
        ASSERT_EQ(offerBaseProp->HistoryCurrencyId(), 3); //3 == RUR
        ASSERT_EQ(
                NMarket::NDocumentFlags::GetDocumentFlag(offerBaseProp->Flags64(), NMarket::NDocumentFlags::ENABLE_AUTO_DISCOUNTS),
                index % 2);
        ASSERT_EQ(
                NMarket::NDocumentFlags::GetDocumentFlag(offerBaseProp->Flags64(), NMarket::NDocumentFlags::IS_PUSH_PARTNER),
                (index + 1) % 2);
        ASSERT_EQ(offerBaseProp->HistoryPriceIsValid(), index % 2);
        ASSERT_EQ(offerBaseProp->PurchasePrice(), expectedPrice / 2);
        offer_index++;
    }
}

TEST(TBasePropertiesDumper, CreationFile)
{
    Market::NCurrency::TCurrencyExchange currencyExchange;
    currencyExchange.Load(ArcadiaSourceRoot() + "/market/idx/offers/tests/ut/data/currency_rates.xml");
    const size_t number_of_offers = 128;

    /* dir preparation */
    TTempDir dir;

    NDumpers::TDumperContext context(dir.Name(), false);
    auto basePropertiesDumper = NDumpers::MakeBaseOfferPropertiesDumper(context, currencyExchange);

    constexpr uint32_t offset = 20;
    for (size_t i = 0; i < number_of_offers; ++i)
    {
        auto row_price = static_cast<uint64_t>(i*100);
        const auto price = TFixedPointNumber::CreateFromRawValue(TFixedPointNumber::DefaultPrecision, row_price);

        MarketIndexer::GenerationLog::Record record;

        record.mutable_binary_price()->set_price(price.AsRaw());
        record.mutable_binary_price()->set_id("RUR");
        record.mutable_binary_price()->set_ref_id("RUR");

        record.mutable_binary_oldprice()->set_price(price.AsRaw());
        record.mutable_binary_oldprice()->set_id("RUR");
        record.mutable_binary_oldprice()->set_ref_id("RUR");

        record.mutable_binary_history_price()->set_price(price.AsRaw());
        record.mutable_binary_history_price()->set_id("RUR");
        record.mutable_binary_history_price()->set_ref_id("RUR");

        record.mutable_binary_unverified_oldprice()->set_price(price.AsRaw());
        record.mutable_binary_unverified_oldprice()->set_id("RUR");
        record.mutable_binary_unverified_oldprice()->set_ref_id("RUR");

        record.set_history_price_is_valid(bool(i % 2));
        record.set_flags(bool(i % 2) ? NMarket::NDocumentFlags::ENABLE_AUTO_DISCOUNTS : NMarket::NDocumentFlags::IS_PUSH_PARTNER);
        record.set_purchase_price(price.AsDouble() / 2);

        basePropertiesDumper->ProcessGenlogRecord(record, i + offset);
    }

    basePropertiesDumper->Finish();

    TUnbufferedFileInput  m0(dir.Path() / "base_docs_props.meta");
    TUnbufferedFileInput fb0(dir.Path() / "base-offer-props.fb");

    ASSERT_EQ(m0.ReadAll(), META_CONTENT);

    CheckBasePropertiesForShard(number_of_offers, offset, fb0);
}

void CheckBasePropertiesExtForShard(
        const size_t number_of_offers_in_shard,
        const uint32_t offset,
        TUnbufferedFileInput& fb)
{
    using TBasePropExtRecord = NMarket::NIndexer::NOfferCollection::TBasePropertiesExt;
    using TOfferBasePropExtRecord = NMarket::NIndexer::NOfferCollection::TOfferBasePropertiesExt;
    using TInstallmentInfo = NMarket::NIndexer::NOfferCollection::NInstallmentInfo::TInstallmentInfo;

    TVector<const TInstallmentInfo*> installmentCollector;
    uint64_t offer_index = 0;
    const TString fileData = fb.ReadAll();
    const TBasePropExtRecord* basePropExtRecord = NMarket::NFlatbufferHelpers::GetTBasePropertiesExt(fileData);

    ASSERT_TRUE(basePropExtRecord);
    const flatbuffers64::Vector<flatbuffers64::Offset<TOfferBasePropExtRecord>>* basePropsExt = basePropExtRecord->BaseProperties();
    ASSERT_TRUE(basePropsExt);
    ASSERT_EQ(basePropsExt->size(), number_of_offers_in_shard + offset);
    for (size_t index = 0; index < basePropsExt->size(); ++index) {
        if (index < offset) {
            continue;
        }

        const TOfferBasePropExtRecord *offerBasePropExt = basePropsExt->Get(index);
        ASSERT_TRUE(offerBasePropExt != nullptr);
        ASSERT_STREQ(offerBasePropExt->VendorCode()->str(), "vendor" + std::to_string(index));
        ASSERT_EQ(offerBasePropExt->Length(), offer_index);
        ASSERT_EQ(offerBasePropExt->Width(), offer_index * offer_index);
        ASSERT_EQ(offerBasePropExt->Height(), offer_index + 2);
        ASSERT_EQ(offerBasePropExt->ClassifierCategoryConfidenceForFilteringStupids(), offer_index + 3);
        ASSERT_EQ(offerBasePropExt->IsNotTsar(), offer_index % 7 == 0);
        ASSERT_EQ(offerBasePropExt->MpSupplierOgrn()->str(), "supplier" + ToString(3 * offer_index));
        ASSERT_EQ(offerBasePropExt->FeedId(), 7 * offer_index);
        if (offer_index % 5 == 0) {
            ASSERT_EQ(offerBasePropExt->VirtualModelId()->Value(), offer_index);
        } else {
            ASSERT_EQ(offerBasePropExt->VirtualModelId()->Value(), offer_index * offer_index + 1);
        }
        if (offer_index <= 50) {
            installmentCollector.push_back(offerBasePropExt->InstallmentInfo());
            ASSERT_EQ(offerBasePropExt->InstallmentInfo()->Days()->size(), 2);
            ASSERT_EQ(offerBasePropExt->InstallmentInfo()->Days()->Get(0), 30);
            ASSERT_EQ(offerBasePropExt->InstallmentInfo()->Days()->Get(1), 120);
            ASSERT_TRUE(offerBasePropExt->InstallmentInfo()->HasBnpl());
        } else {
            ASSERT_EQ(offerBasePropExt->InstallmentInfo(), nullptr);
        }

        const auto& cMagicIdFb = offerBasePropExt->ClassifierMagicId();
        Market::TCMagicId cMagicId(cMagicIdFb->Lower(), cMagicIdFb->Upper());
        ASSERT_EQ(cMagicId.to_string(), ToString(offer_index % 10) + "9090151b9f536e4e4fb7cd15a7022f9");
        ASSERT_EQ(offerBasePropExt->DynamicPricingData(), 0x42000000000004d2);

        ASSERT_EQ(offerBasePropExt->MedicalFlags(), 255);

        ++offer_index;
    }
    ASSERT_TRUE(installmentCollector.size() > 1);
    // Проверка кэширования рассрочки. Т.к все оффера или без опций, или с одной общей опцией, то все указатели на опции должны совпадать
    for (size_t i = 0; i < installmentCollector.size() - 1; ++i) {
        ASSERT_EQ(installmentCollector[i], installmentCollector[i + 1]);
    }
}

TEST(TBasePropertiesExtDumper, CreationFile)
{
    Market::NCurrency::TCurrencyExchange currencyExchange;
    currencyExchange.Load(ArcadiaSourceRoot() + "/market/idx/offers/tests/ut/data/currency_rates.xml");
    const size_t number_of_offers = 128;

    /* dir preparation */
    TTempDir dir;

    NDumpers::TDumperContext context(dir.Name(), false);
    auto basePropertiesExtDumper = NDumpers::MakeBaseOfferPropertiesExtDumper(context, currencyExchange);

    constexpr uint32_t offset = 20;
    for (size_t i = 0; i < number_of_offers; ++i)
    {
        MarketIndexer::GenerationLog::Record record;

        record.set_length(i);
        record.set_width(i * i);
        record.set_height(i + 2);
        record.set_vendor_code("vendor" + ToString(i + offset));
        record.set_is_blue_offer(false);
        record.set_feed_id(7 * i);
        record.set_supplier_name("supplier" + ToString(3 * i));
        record.set_market_sku(i);
        record.set_classifier_magic_id(ToString(i % 10) + "9090151b9f536e4e4fb7cd15a7022f9");
        record.set_classifier_category_confidence_for_filtering_stupids(i + 3);
        record.set_is_not_tsar(i % 7 == 0);

        record.set_dynamic_pricing_type(2);
        record.set_dynamic_pricing_threshold_is_percent(true);
        record.set_dynamic_pricing_threshold_value(1234);

        record.set_is_fast_sku(i % 5 == 0);
        record.set_virtual_model_id(i * i + 1);

        if (i <= 50) {
            // первая опция на 120 дней, вторая на 30, третья только с bnpl доставкой
            record.mutable_installment_options()->Add()->mutable_installment_time_in_days()->Add(120);
            record.mutable_installment_options()->Add()->mutable_installment_time_in_days()->Add(30);
            record.mutable_installment_options()->Add()->set_bnpl_available(true);
        }

        record.set_medical_flags(255);

        basePropertiesExtDumper->ProcessGenlogRecord(record, i + offset);
    }

    basePropertiesExtDumper->Finish();

    TUnbufferedFileInput fb(dir.Path() / "base-offer-props-ext.fb64");

    CheckBasePropertiesExtForShard(number_of_offers, offset, fb);
}

TEST(TQVatDumper, CreationFile)
{
    /* dir preparation */
    TTempDir dir;

    NDumpers::TDumperContext context(dir.Name(), false);
    auto qVatDumper = NDumpers::MakeQVatDumper(context);

    auto getTestVat = [](size_t index) {
        static const TMaybe<NMarket::NTaxes::EVat> vats[] = {
            Nothing(),
            NMarket::NTaxes::EVat::VAT_20,
            NMarket::NTaxes::EVat::VAT_10,
            NMarket::NTaxes::EVat::VAT_20_120,
            NMarket::NTaxes::EVat::VAT_10_110,
            NMarket::NTaxes::EVat::VAT_0,
            NMarket::NTaxes::EVat::NO_VAT
        };

        return vats[index % (sizeof(vats) / sizeof(vats[0]))];
    };

    uint32_t offset = 100;
    for (int i = 0; i < 128; ++i)
    {
        MarketIndexer::GenerationLog::Record record;
        const auto vat = getTestVat(i);
        if (vat.Defined()) {
            record.set_vat(static_cast<unsigned>(*(vat)));
        }

        qVatDumper->ProcessGenlogRecord(record, i + offset);
    }

    qVatDumper->Finish();

    TUnbufferedFileInput f(dir.Path() / "vat_props.values.binary");

    uint32_t i = 0;
    uint32_t i0 = 0;
    NMarket::NTaxes::TIndexToReportVatPropsRecord record;
    while (f.Load(&record, sizeof(record)))
    {
        if (i++ < offset) {
            continue;
        }
        ASSERT_EQ(record.Get(), getTestVat(i0));
        ++i0;
    }
}

TEST(LocalDeliveryDumper, Works) {
    /* dir preparation */
    TTempDir dir;

    const TString cexchangePath = dir.Path() / "cexchange.xml";
    {
        TFileOutput cexchangeOut(cexchangePath);
        cexchangeOut
            << "<?xml version='1.0' encoding='utf-8'?>\n"
            << "<exchange>\n"
            << "  <currencies>\n"
            << "    <currency name=\"RUR\"/>\n"
            << "  </currencies>\n"
            << "  <banks>\n"
            << "    <bank name=\"CBRF\">\n"
            << "      <region>225</region>\n"
            << "      <currency>RUR</currency>\n"
            << "      <rates>\n"
            << "        <rate from=\"RUR\" to=\"RUR\">1.0</rate>\n"
            << "      </rates>\n"
            << "    </bank>\n"
            << "  </banks>\n"
            << "</exchange>\n";
    }

    Market::NCurrency::TCurrencyExchange cexchange;
    cexchange.Load(cexchangePath);

    const TString holidaysPath = dir.Path() / "holidays.xml";
    {
        TFileOutput holidaysOut(holidaysPath);
        holidaysOut
            << "<?xml version=\"1.0\"?>\n"
            << "<calendars start-date=\"04.04.2019\" depth-days=\"84\">\n"
            << "  <shops>\n"
            << "  </shops>\n"
            << "</calendars>\n";
    }

    NDumpers::TDumperContext context(dir.Name(), false);
    auto dumper = NDumpers::MakeLocalDeliveryDumper(context, cexchange, holidaysPath);

    for (uint32_t index = 0; index < 10; index += 2) {
        MarketIndexer::GenerationLog::Record record;
        record.set_offer_id(ToString(index));
        record.set_shop_id(1234);
        record.set_feed_id(2345);
        record.set_use_yml_delivery(true);
        record.set_delivery_currency("RUR");

        NMarketIndexer::Common::TDeliveryOption option;
        option.SetDaysMin(1);
        option.SetDaysMax(2);
        option.SetOrderBeforeHour(21);
        option.SetCost(index + 1);
        *(record.add_offer_delivery_options()) = option;

        dumper->ProcessGenlogRecord(record, index);
    }

    dumper->Finish();

    auto reader = Market::CreateLocalDeliveryReader(dir.Path() / "local_delivery_yml.mmap");
    for (uint32_t index = 0; index < 10; index += 2) {
        ASSERT_EQ(reader->GetOfferDeliveryOptionCount(index), 1);
        auto option = reader->GetOfferDeliveryOption(index, 0);
        EXPECT_EQ(option.PriceValue, (index + 1) * 10000000);
    }
}

TEST(RegionalDeliveryDumper, Works) {
    /* dir preparation */
    TTempDir dir;

    const TString holidaysPath = dir.Path() / "holidays.xml";
    {
        TFileOutput holidaysOut(holidaysPath);
        holidaysOut
            << "<?xml version=\"1.0\"?>\n"
            << "<calendars start-date=\"04.04.2019\" depth-days=\"84\">\n"
            << "  <shops>\n"
            << "  </shops>\n"
            << "</calendars>\n";
    }

    NDumpers::TDumperContext context(dir.Name(), false);
    auto dumper = NDumpers::MakeRegionalDeliveryDumper(context, holidaysPath);

    for (uint32_t index = 0; index < 10; index += 2) {
        MarketIndexer::GenerationLog::Record record;
        record.set_delivery_currency("RUR");
        record.set_shop_id(1234);
        record.add_delivery_bucket_ids(index);
        record.add_pickup_bucket_ids(100 * index);
        record.add_post_bucket_ids(10000 * index);
        dumper->ProcessGenlogRecord(record, index);
    }

    dumper->Finish();

    auto reader = Market::NDelivery::CreateOfferDeliveryBucketsReader(
            Market::NMmap::IMemoryRegion::MmapFile(
                    dir.Path() / "offer_delivery_buckets.mmap"
            )
    );
    for (uint32_t index = 0; index < 10; index += 2) {
        ASSERT_EQ(reader->GetBucketsCount(index), 1);
        EXPECT_EQ(reader->GetBucket(index, 0), index);
        ASSERT_EQ(reader->GetPickupBucketsCount(index), 1);
        EXPECT_EQ(reader->GetPickupBucket(index, 0), 100 * index);
        ASSERT_EQ(reader->GetPostBucketsCount(index), 1);
        EXPECT_EQ(reader->GetPostBucket(index, 0), 10000 * index);
    }
}

TString RunOffersDeliveryInfoDumperFromRecord(TVector<MarketIndexer::GenerationLog::Record>& records, ui32 offset = 0)
{
    TTempDir dir;

    NDumpers::TDumperContext context(dir.Name(), false);
    auto dumper = NDumpers::MakeOffersDeliveryInfoDumper(context);
    for (size_t i = 0; i < records.size(); ++i) {
        dumper->ProcessGenlogRecord(records[i], i + offset);
    }
    dumper->Finish();

    TUnbufferedFileInput fb(dir.Path() / "offers-delivery-info.fb");

    return fb.ReadAll();
}

TEST(OffersDeliveryInfoDumper, CreationFile)
{
    constexpr ui32 OFFSET = 20;
    constexpr size_t NUMBER_OF_OFFERS = 128;
    TVector<MarketIndexer::GenerationLog::Record> records(NUMBER_OF_OFFERS);

    const auto fileData = RunOffersDeliveryInfoDumperFromRecord(records, OFFSET);

    const NMarket::NIndexer::NOfferCollection::TDeliveryProperties* deliveryInfo = NMarket::NFlatbufferHelpers::GetTDeliveryProperties(fileData);

    ASSERT_TRUE(deliveryInfo);
    const flatbuffers::Vector<flatbuffers::Offset<NMarket::NIndexer::NOfferCollection::TOfferDeliveryProperties>>* deliveryProps = deliveryInfo->delivery_info();
    ASSERT_TRUE(deliveryProps);
    ASSERT_EQ(deliveryProps->size(), NUMBER_OF_OFFERS + OFFSET);
    for (size_t index = 0; index < deliveryProps->size(); ++index) {
        if (index < OFFSET) {
            continue;
        }

        const NMarket::NIndexer::NOfferCollection::TOfferDeliveryProperties* info = deliveryProps->Get(index);
        ASSERT_TRUE(info);
    }
}

TVector<MarketIndexer::GenerationLog::Record> PrepareRecords(TAddBucketInfoMethod add = nullptr)
{
    delivery_calc::mbi::BucketInfo bucket;
    bucket.set_bucket_id(123);
    bucket.add_cost_modifiers_ids(1);
    bucket.add_time_modifiers_ids(2);
    bucket.add_services_modifiers_ids(3);
    bucket.add_region_availability_modifiers_ids(4);

    TVector<MarketIndexer::GenerationLog::Record> records;
    for (size_t i = 0; i < 3; ++i) {
        records.emplace_back();
        auto& record = records.back();
        record.set_offer_id("offer");
        record.set_feed_id(i);
        if (add) {
            *(record.mutable_offers_delivery_info_renumerated()->*add)() = bucket;
        }
    }
    records[0].mutable_contex_info()->set_experiment_id("exp");
    records[0].mutable_contex_info()->set_is_experimental(true);
    records[1].set_delivery_flag(false);
    records[2].set_delivery_flag(true);
    records[2].set_feed_id(1);  // коллизия

    return records;
}

TEST(OffersHashMappingDumper, DumpOffsetMapping)
{
    auto records = PrepareRecords();

    TTempDir dir;

    NDumpers::TDumperContext context(dir.Name(), false);
    auto dumper = NDumpers::MakeOffersHashMappingDumper(context);
    for (size_t i = 0; i < records.size(); ++i) {
        dumper->ProcessGenlogRecord(records[i], i);
    }
    dumper->Finish();

    TUnbufferedFileInput fb(dir.Path() / "offers-hash-mapping.fb");

    const auto fileData = fb.ReadAll();
    const auto offersHashMapping = NMarket::NFlatbufferHelpers::GetTOffersHashMapping(fileData);

    ASSERT_TRUE(offersHashMapping);
    const auto offsetMapping = offersHashMapping->offer_to_offset_map();
    ASSERT_TRUE(offsetMapping);
    ASSERT_EQ(offsetMapping->size(), records.size());
    {
        uint64_t index = 0;
        TMaybe<Market::OffersData::ContexInfo> contexInfo = Market::OffersData::ContexInfo();
        contexInfo->set_experiment_id("exp");
        contexInfo->set_is_experimental(true);
        auto hash = NOfferHashId::GetOfferHashId(index, "offer", contexInfo);
        auto value = offsetMapping->LookupByKey(hash);
        ASSERT_TRUE(value);
        auto offset = value->offer_offset();
        ASSERT_EQ(offset, index);
    }
    {
        // коллизия: порядок не гарантируется
        uint64_t index = 1;
        auto hash = NOfferHashId::GetOfferHashId(index, "offer", Nothing());
        auto value = offsetMapping->LookupByKey(hash);
        ASSERT_TRUE(value);
        auto offset = value->offer_offset();
        ASSERT_TRUE(offset == 1 || offset == 2);
    }
    {
        // значение не найдено
        uint64_t index = 2;
        auto hash = NOfferHashId::GetOfferHashId(index, "offer", Nothing());
        auto value = offsetMapping->LookupByKey(hash);
        ASSERT_FALSE(value);
    }
}

TEST(OffersHashMappingDumper, DumpOffsetMappingFromGenlog)
{
    auto records = PrepareRecords();

    TTempDir dir;

    NDumpers::TDumperContext context(dir.Name(), false);
    auto dumper = NDumpers::MakeOffersHashMappingDumper(context);
    for (size_t i = 0; i < records.size(); ++i) {
        dumper->ProcessGenlogRecord(records[i], i);
    }
    dumper->Finish();

    TUnbufferedFileInput fb(dir.Path() / "offers-hash-mapping.fb");

    const auto fileData = fb.ReadAll();
    const auto offersHashMapping = NMarket::NFlatbufferHelpers::GetTOffersHashMapping(fileData);

    ASSERT_TRUE(offersHashMapping);
    const auto offsetMapping = offersHashMapping->offer_to_offset_map();
    ASSERT_TRUE(offsetMapping);
    ASSERT_EQ(offsetMapping->size(), records.size());
    {
        uint64_t index = 0;
        TMaybe<Market::OffersData::ContexInfo> contexInfo = Market::OffersData::ContexInfo();
        contexInfo->set_experiment_id("exp");
        contexInfo->set_is_experimental(true);
        auto hash = NOfferHashId::GetOfferHashId(index, "offer", contexInfo);
        auto value = offsetMapping->LookupByKey(hash);
        ASSERT_TRUE(value);
        auto offset = value->offer_offset();
        ASSERT_EQ(offset, index);
    }
    {
        // коллизия: порядок не гарантируется
        uint64_t index = 1;
        auto hash = NOfferHashId::GetOfferHashId(index, "offer", Nothing());
        auto value = offsetMapping->LookupByKey(hash);
        ASSERT_TRUE(value);
        auto offset = value->offer_offset();
        ASSERT_TRUE(offset == 1 || offset == 2);
    }
    {
        // значение не найдено
        uint64_t index = 2;
        auto hash = NOfferHashId::GetOfferHashId(index, "offer", Nothing());
        auto value = offsetMapping->LookupByKey(hash);
        ASSERT_FALSE(value);
    }
}

template <typename TBucketInfo>
void CheckFullDeliveryInfo(const flatbuffers::Vector<flatbuffers::Offset<TBucketInfo>>* buckets, size_t index)
{
    ASSERT_TRUE(buckets);
    ASSERT_EQ(buckets->size(), 1);
    auto bucket = buckets->Get(0);
    ASSERT_EQ(bucket->is_new(), index == 0);
    ASSERT_EQ(bucket->bucket_id(), 123);

    ASSERT_NE(bucket->cost_modifiers_ids(), nullptr);
    ASSERT_EQ(bucket->cost_modifiers_ids()->size(), 1);
    ASSERT_EQ(bucket->cost_modifiers_ids()->Get(0), 1);

    ASSERT_NE(bucket->time_modifiers_ids(), nullptr);
    ASSERT_EQ(bucket->time_modifiers_ids()->size(), 1);
    ASSERT_EQ(bucket->time_modifiers_ids()->Get(0), 2);

    ASSERT_NE(bucket->services_modifiers_ids(), nullptr);
    ASSERT_EQ(bucket->services_modifiers_ids()->size(), 1);
    ASSERT_EQ(bucket->services_modifiers_ids()->Get(0), 3);

    ASSERT_NE(bucket->region_availability_modifiers_ids(), nullptr);
    ASSERT_EQ(bucket->region_availability_modifiers_ids()->size(), 1);
    ASSERT_EQ(bucket->region_availability_modifiers_ids()->Get(0), 4);
}

TEST(OffersDeliveryInfoDumper, DumpCourierIds)
{
    auto records = PrepareRecords(&delivery_calc::mbi::OffersDeliveryInfo::add_courier_buckets_info);
    records[0].mutable_offers_delivery_info_renumerated()->mutable_courier_buckets_info(0)->set_is_new(true);

    const auto fileData = RunOffersDeliveryInfoDumperFromRecord(records);
    const NMarket::NIndexer::NOfferCollection::TDeliveryProperties* deliveryInfo = NMarket::NFlatbufferHelpers::GetTDeliveryProperties(fileData);

    ASSERT_TRUE(deliveryInfo);
    const flatbuffers::Vector<flatbuffers::Offset<NMarket::NIndexer::NOfferCollection::TOfferDeliveryProperties>>* deliveryProps = deliveryInfo->delivery_info();
    ASSERT_TRUE(deliveryProps);
    ASSERT_EQ(deliveryProps->size(), records.size());
    for (size_t index = 0; index < deliveryProps->size(); ++index) {
        const NMarket::NIndexer::NOfferCollection::TOfferDeliveryProperties* info = deliveryProps->Get(index);
        ASSERT_TRUE(info);
        auto ids = info->offer_modifiers_delivery_info();
        ASSERT_TRUE(ids);
        ASSERT_FALSE(ids->pickup_buckets_info());
        ASSERT_FALSE(ids->post_buckets_info());
        if (index != 2) {
            ASSERT_FALSE(ids->courier_buckets_info());
        } else {
            auto courierBuckets = ids->courier_buckets_info();
            CheckFullDeliveryInfo(courierBuckets, index);
        }
    }
}

TEST(OffersDeliveryInfoDumper, DumpPickupIds)
{
    auto records = PrepareRecords(&delivery_calc::mbi::OffersDeliveryInfo::add_pickup_buckets_info);
    records[0].mutable_offers_delivery_info_renumerated()->mutable_pickup_buckets_info(0)->set_is_new(true);

    const auto fileData = RunOffersDeliveryInfoDumperFromRecord(records);
    const NMarket::NIndexer::NOfferCollection::TDeliveryProperties* deliveryInfo = NMarket::NFlatbufferHelpers::GetTDeliveryProperties(fileData);

    ASSERT_TRUE(deliveryInfo);
    const flatbuffers::Vector<flatbuffers::Offset<NMarket::NIndexer::NOfferCollection::TOfferDeliveryProperties>>* deliveryProps = deliveryInfo->delivery_info();
    ASSERT_TRUE(deliveryProps);
    ASSERT_EQ(deliveryProps->size(), records.size());
    for (size_t index = 0; index < deliveryProps->size(); ++index) {
        const NMarket::NIndexer::NOfferCollection::TOfferDeliveryProperties* info = deliveryProps->Get(index);
        ASSERT_TRUE(info);
        auto ids = info->offer_modifiers_delivery_info();
        ASSERT_TRUE(ids);
        ASSERT_FALSE(ids->courier_buckets_info());
        ASSERT_FALSE(ids->post_buckets_info());
        auto pickupBuckets = ids->pickup_buckets_info();
        CheckFullDeliveryInfo(pickupBuckets, index);
    }
}

TEST(OffersDeliveryInfoDumper, DumpPostIds)
{
    auto records = PrepareRecords(&delivery_calc::mbi::OffersDeliveryInfo::add_post_buckets_info);
    records[0].mutable_offers_delivery_info_renumerated()->mutable_post_buckets_info(0)->set_is_new(true);

    const auto fileData = RunOffersDeliveryInfoDumperFromRecord(records);

    const NMarket::NIndexer::NOfferCollection::TDeliveryProperties* deliveryInfo = NMarket::NFlatbufferHelpers::GetTDeliveryProperties(fileData);

    ASSERT_TRUE(deliveryInfo);
    const flatbuffers::Vector<flatbuffers::Offset<NMarket::NIndexer::NOfferCollection::TOfferDeliveryProperties>>* deliveryProps = deliveryInfo->delivery_info();
    ASSERT_TRUE(deliveryProps);
    ASSERT_EQ(deliveryProps->size(), records.size());
    for (size_t index = 0; index < deliveryProps->size(); ++index) {
        const NMarket::NIndexer::NOfferCollection::TOfferDeliveryProperties* info = deliveryProps->Get(index);
        ASSERT_TRUE(info);
        auto ids = info->offer_modifiers_delivery_info();
        ASSERT_TRUE(ids);
        ASSERT_FALSE(ids->courier_buckets_info());
        ASSERT_FALSE(ids->pickup_buckets_info());
        auto postBuckets = ids->post_buckets_info();
        CheckFullDeliveryInfo(postBuckets, index);
    }
}

TEST(OffersDeliveryInfoDumper, DumpCourierIdsFromGenlog)
{
    auto records = PrepareRecords(&delivery_calc::mbi::OffersDeliveryInfo::add_courier_buckets_info);
    records[0].mutable_offers_delivery_info_renumerated()->mutable_courier_buckets_info(0)->set_is_new(true);

    const auto fileData = RunOffersDeliveryInfoDumperFromRecord(records);

    const NMarket::NIndexer::NOfferCollection::TDeliveryProperties* deliveryInfo = NMarket::NFlatbufferHelpers::GetTDeliveryProperties(fileData);

    ASSERT_TRUE(deliveryInfo);
    const flatbuffers::Vector<flatbuffers::Offset<NMarket::NIndexer::NOfferCollection::TOfferDeliveryProperties>>* deliveryProps = deliveryInfo->delivery_info();
    ASSERT_TRUE(deliveryProps);
    ASSERT_EQ(deliveryProps->size(), records.size());
    for (size_t index = 0; index < deliveryProps->size(); ++index) {
        const NMarket::NIndexer::NOfferCollection::TOfferDeliveryProperties* info = deliveryProps->Get(index);
        ASSERT_TRUE(info);
        auto ids = info->offer_modifiers_delivery_info();
        ASSERT_TRUE(ids);
        ASSERT_FALSE(ids->pickup_buckets_info());
        ASSERT_FALSE(ids->post_buckets_info());
        if (index != 2) {
            ASSERT_FALSE(ids->courier_buckets_info());
        } else {
            auto courierBuckets = ids->courier_buckets_info();
            CheckFullDeliveryInfo(courierBuckets, index);
        }
    }
}

TEST(OffersDeliveryInfoDumper, DumpPickupIdsFromGenlog)
{
    auto records = PrepareRecords(&delivery_calc::mbi::OffersDeliveryInfo::add_pickup_buckets_info);
    records[0].mutable_offers_delivery_info_renumerated()->mutable_pickup_buckets_info(0)->set_is_new(true);

    const auto fileData = RunOffersDeliveryInfoDumperFromRecord(records);

    const NMarket::NIndexer::NOfferCollection::TDeliveryProperties* deliveryInfo = NMarket::NFlatbufferHelpers::GetTDeliveryProperties(fileData);

    ASSERT_TRUE(deliveryInfo);
    const flatbuffers::Vector<flatbuffers::Offset<NMarket::NIndexer::NOfferCollection::TOfferDeliveryProperties>>* deliveryProps = deliveryInfo->delivery_info();
    ASSERT_TRUE(deliveryProps);
    ASSERT_EQ(deliveryProps->size(), records.size());
    for (size_t index = 0; index < deliveryProps->size(); ++index) {
        const NMarket::NIndexer::NOfferCollection::TOfferDeliveryProperties* info = deliveryProps->Get(index);
        ASSERT_TRUE(info);
        auto ids = info->offer_modifiers_delivery_info();
        ASSERT_TRUE(ids);
        ASSERT_FALSE(ids->courier_buckets_info());
        ASSERT_FALSE(ids->post_buckets_info());
        auto pickupBuckets = ids->pickup_buckets_info();
        CheckFullDeliveryInfo(pickupBuckets, index);
    }
}

TEST(OffersDeliveryInfoDumper, DumpPostIdsFromGenlog)
{
    auto records = PrepareRecords(&delivery_calc::mbi::OffersDeliveryInfo::add_post_buckets_info);
    records[0].mutable_offers_delivery_info_renumerated()->mutable_post_buckets_info(0)->set_is_new(true);

    const auto fileData = RunOffersDeliveryInfoDumperFromRecord(records);
    const NMarket::NIndexer::NOfferCollection::TDeliveryProperties* deliveryInfo = NMarket::NFlatbufferHelpers::GetTDeliveryProperties(fileData);

    ASSERT_TRUE(deliveryInfo);
    const flatbuffers::Vector<flatbuffers::Offset<NMarket::NIndexer::NOfferCollection::TOfferDeliveryProperties>>* deliveryProps = deliveryInfo->delivery_info();
    ASSERT_TRUE(deliveryProps);
    ASSERT_EQ(deliveryProps->size(), records.size());
    for (size_t index = 0; index < deliveryProps->size(); ++index) {
        const NMarket::NIndexer::NOfferCollection::TOfferDeliveryProperties* info = deliveryProps->Get(index);
        ASSERT_TRUE(info);
        auto ids = info->offer_modifiers_delivery_info();
        ASSERT_TRUE(ids);
        ASSERT_FALSE(ids->courier_buckets_info());
        ASSERT_FALSE(ids->pickup_buckets_info());
        auto postBuckets = ids->post_buckets_info();
        CheckFullDeliveryInfo(postBuckets, index);
    }
}

TEST(TRegionalDeliveryDumper, OfferDeliveryBucketsWhiteCpaPositive)
{
    TTempDir dir;

    const TString holidaysPath = dir.Path().Child("holidays.xml");
    {
        TFileOutput holidaysOut(holidaysPath);
        holidaysOut
            << "<?xml version=\"1.0\"?>\n"
            << "<calendars start-date=\"04.04.2019\" depth-days=\"84\">\n"
            << "  <shops>\n"
            << "  </shops>\n"
            << "</calendars>\n";
    }

    NDumpers::TDumperContext context(dir.Name(), false);
    // при clearBucketIdsForWhiteCpaOffers == false мы не должны чистить информацию о бакетах белых cpa офферов
    // не смотря на clearOldBucketIdsFromWhiteOffers
    auto dumper = NDumpers::MakeRegionalDeliveryDumper(
        context,
        holidaysPath,
        /* clearOldBucketIdsFromWhiteOffers */ true,
        /* clearBucketIdsForWhiteCpaOffers */ false
    );

    TVector<MarketIndexer::GenerationLog::Record> records;
    {
        // White cpa
        MarketIndexer::GenerationLog::Record& record = records.emplace_back();
        record.set_cpa(static_cast<i32>(NMarket::NBind::ECpa::REAL));
        record.set_shop_id(100500);
        record.set_is_blue_offer(false);
        record.add_delivery_bucket_ids(0);
        record.add_pickup_bucket_ids(0);
        record.add_post_bucket_ids(0);
    }
    {
        // White not cpa
        MarketIndexer::GenerationLog::Record& record = records.emplace_back();
        record.set_cpa(static_cast<i32>(NMarket::NBind::ECpa::NO));
        record.set_shop_id(100501);
        record.set_is_blue_offer(false);
        record.add_delivery_bucket_ids(1);
        record.add_pickup_bucket_ids(1);
        record.add_post_bucket_ids(1);
    }
    {
        // Blue
        MarketIndexer::GenerationLog::Record& record = records.emplace_back();
        record.set_cpa(static_cast<i32>(NMarket::NBind::ECpa::REAL));
        record.set_shop_id(100502);
        record.set_is_blue_offer(true);
        record.add_delivery_bucket_ids(2);
        record.add_pickup_bucket_ids(2);
        record.add_post_bucket_ids(2);
        record.set_flags(NMarket::NDocumentFlags::BLUE_OFFER);
    }

    for (size_t i = 0; i < records.size(); ++i) {
        dumper->ProcessGenlogRecord(records[i], i);
    }

    dumper->Finish();

    auto reader = Market::NDelivery::CreateOfferDeliveryBucketsReader(
            Market::NMmap::IMemoryRegion::MmapFile(
                    dir.Path().Child("offer_delivery_buckets.mmap")
            )
    );

    // White cpa
    {
        ASSERT_EQ(reader->GetBucketsCount(0), 1);
        EXPECT_EQ(reader->GetBucket(0, 0), 0);
        ASSERT_EQ(reader->GetPickupBucketsCount(0), 1);
        EXPECT_EQ(reader->GetPickupBucket(0, 0), 0);
        ASSERT_EQ(reader->GetPostBucketsCount(0), 1);
        EXPECT_EQ(reader->GetPostBucket(0, 0), 0);
    }

    // White not cpa
    {
        ASSERT_EQ(reader->GetBucketsCount(1), 0);
        ASSERT_EQ(reader->GetPickupBucketsCount(1), 0);
        ASSERT_EQ(reader->GetPostBucketsCount(1), 0);
    }

    // Blue
    {
        ASSERT_EQ(reader->GetBucketsCount(2), 1);
        EXPECT_EQ(reader->GetBucket(2, 0), 2);
        ASSERT_EQ(reader->GetPickupBucketsCount(2), 1);
        EXPECT_EQ(reader->GetPickupBucket(2, 0), 2);
        ASSERT_EQ(reader->GetPostBucketsCount(2), 1);
        EXPECT_EQ(reader->GetPostBucket(2, 0), 2);
    }
}

TEST(TRegionalDeliveryDumper, OfferDeliveryBucketsWhiteCpaNegative)
{
    TTempDir dir;

    const TString holidaysPath = dir.Path().Child("holidays.xml");
    {
        TFileOutput holidaysOut(holidaysPath);
        holidaysOut
            << "<?xml version=\"1.0\"?>\n"
            << "<calendars start-date=\"04.04.2021\" depth-days=\"84\">\n"
            << "  <shops>\n"
            << "  </shops>\n"
            << "</calendars>\n";
    }

    NDumpers::TDumperContext context(dir.Name(), false);
    // c clearBucketIdsForWhiteCpaOffers == true мы чистим информацию о бакетах белых cpa офферов,
    // если clearOldBucketIdsFromWhiteOffers == true
    auto dumper = NDumpers::MakeRegionalDeliveryDumper(
        context,
        holidaysPath,
        /* clearOldBucketIdsFromWhiteOffers */ true,
        /* clearBucketIdsForWhiteCpaOffers */ true
    );

    TVector<MarketIndexer::GenerationLog::Record> records;
    {
        // White cpa
        MarketIndexer::GenerationLog::Record& record = records.emplace_back();
        record.set_cpa(static_cast<i32>(NMarket::NBind::ECpa::REAL));
        record.set_shop_id(100500);
        record.set_is_blue_offer(false);
        record.add_delivery_bucket_ids(0);
        record.add_pickup_bucket_ids(0);
        record.add_post_bucket_ids(0);
    }
    {
        // White not cpa
        MarketIndexer::GenerationLog::Record& record = records.emplace_back();
        record.set_cpa(static_cast<i32>(NMarket::NBind::ECpa::NO));
        record.set_shop_id(100501);
        record.set_is_blue_offer(false);
        record.add_delivery_bucket_ids(1);
        record.add_pickup_bucket_ids(1);
        record.add_post_bucket_ids(1);
    }
    {
        // Blue
        MarketIndexer::GenerationLog::Record& record = records.emplace_back();
        record.set_cpa(static_cast<i32>(NMarket::NBind::ECpa::REAL));
        record.set_shop_id(100502);
        record.set_is_blue_offer(true);
        record.add_delivery_bucket_ids(2);
        record.add_pickup_bucket_ids(2);
        record.add_post_bucket_ids(2);
        record.set_flags(NMarket::NDocumentFlags::BLUE_OFFER);
    }

    for (size_t i = 0; i < records.size(); ++i) {
        dumper->ProcessGenlogRecord(records[i], i);
    }

    dumper->Finish();

    auto reader = Market::NDelivery::CreateOfferDeliveryBucketsReader(
            Market::NMmap::IMemoryRegion::MmapFile(
                    dir.Path().Child("offer_delivery_buckets.mmap")
            )
    );

    // White cpa
    {
        ASSERT_EQ(reader->GetBucketsCount(0), 0);
        ASSERT_EQ(reader->GetPickupBucketsCount(0), 0);
        ASSERT_EQ(reader->GetPostBucketsCount(0), 0);
    }

    // White not cpa
    {
        ASSERT_EQ(reader->GetBucketsCount(1), 0);
        ASSERT_EQ(reader->GetPickupBucketsCount(1), 0);
        ASSERT_EQ(reader->GetPostBucketsCount(1), 0);
    }

    // Blue
    {
        ASSERT_EQ(reader->GetBucketsCount(2), 1);
        EXPECT_EQ(reader->GetBucket(2, 0), 2);
        ASSERT_EQ(reader->GetPickupBucketsCount(2), 1);
        EXPECT_EQ(reader->GetPickupBucket(2, 0), 2);
        ASSERT_EQ(reader->GetPostBucketsCount(2), 1);
        EXPECT_EQ(reader->GetPostBucket(2, 0), 2);
    }
}

TEST(TContext, ServiceModeTest)
{
    const auto& dirPath = TFsPath("/some_dir");
    NDumpers::TDumperContext context(dirPath, true);

    ASSERT_EQ(context.IndexerDirectoryFileName("test.dat"), "/some_dir/test.service.dat");
}

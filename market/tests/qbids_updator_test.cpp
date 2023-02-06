#include "util.h"
#include "legacy_for_test.h"

#include <market/qpipe/qbid/qbids_updator/qbids_updator.cpp>
#include <market/qpipe/qbid/qbidengine/mbi_bids.h>
#include <market/qpipe/qbid/minbids/minbids_file.h>

#include <library/cpp/testing/unittest/gtest.h>

#include <fstream>
#include <string>

namespace
{

//TODO(baggins@): позиция 0x01 во флагах уже в 4-х местах - здесь, основном коде updater, в оферном индексаторе и в репорте.
//Следующим коммитом нужно утащить это в общее место.
constexpr uint8_t BIDS_FLAG_DONT_PULL_UP_BIDS = 0x01;
constexpr size_t TEST_INDEX_OFFERS_COUNT = 155;

char* AppendToBuffer(char* buffer_position, const char* string)
{
    const char* ptr = string;
    while((*buffer_position++ = *ptr++));
    return buffer_position;
}


size_t StoreAndGetOffset(const char* begin, char*& ptr, const char* string)
{
    size_t offset = ptr - begin;
    ptr = AppendToBuffer(ptr, string);
    return offset;
}


void CreateInstruction(const TString& instruction_filepath)
{
    using namespace NQBid;
    using namespace NQBid::NSearch;

    /* mapping to file structure */
    struct TestInstruction
    {
        size_t DigitRecordsSectionSize;
        TFeedIdOfferDigitId D0;
        TFeedIdOfferDigitId D1;
        TFeedIdOfferDigitId D2;
        TFeedIdOfferDigitId D3;
        TFeedIdOfferDigitId D4;
        TFeedIdOfferDigitId D5;
        TFeedIdOfferDigitId D6;

        size_t StringRecordsSectionSize;
        TFeedIdOfferOffsetId S0;
        TFeedIdOfferOffsetId S1;

        size_t StringsSize;
        char StringBuffer[1024];

        TestInstruction()
        {
            DigitRecordsSectionSize = \
                    offsetof(TestInstruction, StringRecordsSectionSize) -
                    offsetof(TestInstruction, DigitRecordsSectionSize) -
                    sizeof(DigitRecordsSectionSize);

            StringRecordsSectionSize = \
                    offsetof(TestInstruction, StringsSize) -
                    offsetof(TestInstruction, StringRecordsSectionSize) -
                    sizeof(StringRecordsSectionSize);

            StringsSize = sizeof(StringBuffer);

        }

    } __attribute__((__packed__)) my_instruction;


    /* Перечисляем здесь в отсортированном порядке, а то поиск работать не будет */
    /* Сортируем по возрастанию feed_id, а внутри по возрастанию offer_id */

    my_instruction.D0.FeedId = 1003;
    my_instruction.D0.OfferDigitId = 11777;
    my_instruction.D0.SequenceNumber = 0;

    my_instruction.D1.FeedId = 5225;
    my_instruction.D1.OfferDigitId = 98777;
    my_instruction.D1.SequenceNumber = 1;

    my_instruction.D2.FeedId = 5225;
    my_instruction.D2.OfferDigitId = 98888;
    my_instruction.D2.SequenceNumber = 4;

    my_instruction.D3.FeedId = 9000;
    my_instruction.D3.OfferDigitId = 5;
    my_instruction.D3.SequenceNumber = 5;

    my_instruction.D4.FeedId = 9000;
    my_instruction.D4.OfferDigitId = 6;
    my_instruction.D4.SequenceNumber = 6;

    my_instruction.D5.FeedId = 9000;
    my_instruction.D5.OfferDigitId = 7;
    my_instruction.D5.SequenceNumber = 7;

    my_instruction.D6.FeedId = 9000;
    my_instruction.D6.OfferDigitId = 8;
    my_instruction.D6.SequenceNumber = 8;


    char* buffer_ptr = my_instruction.StringBuffer;

    my_instruction.S0.FeedId = 5155;
    my_instruction.S0.OfferOffsetId = StoreAndGetOffset(
                                        my_instruction.StringBuffer,
                                        buffer_ptr,
                                        "Луноход 1");
    my_instruction.S0.SequenceNumber = 2;


    my_instruction.S1.FeedId = 5155;
    my_instruction.S1.OfferOffsetId = StoreAndGetOffset(
                                        my_instruction.StringBuffer,
                                        buffer_ptr,
                                        "Марсоход Curiosity");
    my_instruction.S1.SequenceNumber = 3;


    /* dumping mapping to file */
    std::ofstream ofile(instruction_filepath.c_str(),
            std::ofstream::trunc | std::ofstream::out | std::ofstream::binary);
    ofile.write((char*)&my_instruction, sizeof(my_instruction));
    ofile.close();
}


void CreateMetaBidsValues(const TString& meta_filepath)
{
    std::ofstream ofile(meta_filepath.c_str(),
            std::ofstream::trunc | std::ofstream::out | std::ofstream::binary);

    using namespace NQBid;
    class TMetaStorage
    {
    private:
        std::ostream& MetaFile;
    public:
        TMetaStorage(std::ostream& theOfile)
            : MetaFile(theOfile)
        {
            uint64_t size = sizeof(TBidInfo);
            MetaFile.write((char*)&size, sizeof(uint64_t));
        }
        ~TMetaStorage()
        {
            MetaFile.flush();
        }

        TMetaStorage& operator() (const unsigned bid_name, const unsigned bid_offset)
        {
            MetaFile.write((char*)&bid_name, sizeof(bid_name));
            MetaFile.write((char*)&bid_offset, sizeof(bid_offset));
            return *this;
        }
    } ms(ofile);


    //storing
    ms(E_YBID, offsetof(TBidInfo, YBid))
      (E_MBID, offsetof(TBidInfo, MBid))
      (E_CBID, offsetof(TBidInfo, CBid))
      (E_FEE , offsetof(TBidInfo, Fee))
      ;
}


void CreateMinimalBids(const TString& minimal_bids_filepath) {
    TMinBidsWriter writer((minimal_bids_filepath + ".v2").c_str());
    uint32_t sequenceNumber = 0;
    for (unsigned i=0; i<=155; i++) {
        Market::TMinBids minBids;
        minBids.YBid = i;
        minBids.MBid = i;
        minBids.CBid = i;
        minBids.Fee = i;

        writer.Add(minBids, sequenceNumber++);
    }
}


void CreateBidsValues(const TString& file, bool set_zero=false)
{
    using namespace NQBid;

    std::ofstream ofile(file.c_str(),
            std::ofstream::trunc | std::ofstream::out | std::ofstream::binary);

    for(unsigned i = 100; i <= 100 + TEST_INDEX_OFFERS_COUNT; i++)
    {
        TBidInfo info(i, i, 0, 0); //set CBid && Fee to zeros always
        if (set_zero)
            info = TBidInfo(0, 0, 0, 0); //set all to zeros
        ofile.write((char*)&info, sizeof(info));
    }
    ofile.close();
}

void CreateFlagsValues(const TString& file, bool set_zero=false)
{
    std::ofstream ofile(file.c_str(),
            std::ofstream::trunc | std::ofstream::out | std::ofstream::binary);

    for(unsigned i = 0; i <= TEST_INDEX_OFFERS_COUNT; i++)
    {
        //i == 0 BIDS_FLAG_DONT_PULL_UP_BIDS: should be untouched
        //i == 6 BIDS_FLAG_DONT_PULL_UP_BIDS: should be deleted

        uint8_t flags = (i == 0 || i == 6) ? BIDS_FLAG_DONT_PULL_UP_BIDS : 0;
        if (set_zero)
            flags = 0;

        ofile.write((char*)&flags, sizeof(flags));
    }
    ofile.close();
}

void CreateBidsTimestampsFb(const TString& file)
{
    using namespace NMarket::NIndexer::NOfferCollection;

    flatbuffers::FlatBufferBuilder Builder;
    TVector<flatbuffers::Offset<NMarket::NIndexer::NOfferCollection::TBidsTimestamps>> Data;

    Data.reserve(TEST_INDEX_OFFERS_COUNT);
    for(size_t i = 0; i < TEST_INDEX_OFFERS_COUNT; ++i) {
        Data.emplace_back(CreateTBidsTimestamps(Builder, i, i, i));
    }

    Builder.Finish(CreateTBidsTimestampsVec(Builder, Builder.CreateVector(Data)), TBidsTimestampsVecIdentifier());
    TFileOutput(file).Write(Builder.GetBufferPointer(), Builder.GetSize());
}

}

static TString TMP_DIR() { return "tmp/qbids_updator"; }
static TString INSTRUCTION() { return TMP_DIR() + "/instruction.binary"; }
static TString MINIMAL_BID() { return TMP_DIR() + "/minimal_bid.binary"; }
static TString INPUT_SNAPSHOT() { return TMP_DIR() + "/input_snapshot.binary"; }
static TString INPUT_DELTA() { return TMP_DIR() + "/input_delta.binary"; }
static TString INPUT_DELTA2() { return TMP_DIR() + "/input_delta2.binary"; }
static TString INDELTA_LIST() { return TMP_DIR() + "/input_delta.list"; }
static TString META_VALUES() { return TMP_DIR() + "/input_values.meta"; }
static TString QBID_VALUES() { return TMP_DIR() + "/bids.values.binary"; }
static TString QBID_FLAGS() { return TMP_DIR() + "/bids.flags.binary"; }
static TString BEFORE_VALUES() { return TMP_DIR() + "/before.bids.values.binary"; }
static TString OFFERID_VALUES() { return TMP_DIR() + "/offerid.bids.values.binary"; }
static TString BIDS_TIMESTAMPS_FB() { return TMP_DIR() + "/bids-timestamps.fb"; }


class TQBidsUpdator : public ::testing::Test, public IMbiProtocolLegacyWrap
{
public:
    TQBidsUpdator()
    {}
    virtual ~TQBidsUpdator() {};

protected:
    // Sets up the test fixture.
    virtual void SetUp()
    {
        create_test_environment(TMP_DIR());
        CreateInstruction(INSTRUCTION());
        CreateMinimalBids(MINIMAL_BID());
        CreateSnapshot();
        CreateInputDelta();
        CreateInputDelta2();

        CreateMetaBidsValues(META_VALUES());
        CreateBidsValues(BEFORE_VALUES());
        CreateBidsValues(QBID_VALUES());
        CreateFlagsValues(QBID_FLAGS());
        CreateBidsValues(OFFERID_VALUES(), true);
        CreateBidsTimestampsFb(BIDS_TIMESTAMPS_FB());
    }



private:

    void CreateInputDelta()
    {
        MBI::TDumper dumper(INPUT_DELTA());
        MBI::Bid bid;


        // 0:
        bid.set_domain_type(MBI::Bid::FEED_OFFER_ID);
        bid.set_partner_id(155);
        SetFeedOfferId(bid, 1003, "11777");
        bid.mutable_value_for_search()->set_delta_operation(MBI::Bid::MODIFY);
        bid.mutable_value_for_search()->set_value(300);
        bid.mutable_value_for_market_search_only()->set_delta_operation(MBI::Bid::MODIFY);
        bid.mutable_value_for_market_search_only()->set_value(400);
        dumper.Dump(bid);

        // 1:
        bid.set_domain_type(MBI::Bid::FEED_OFFER_ID);
        bid.set_partner_id(155);
        SetFeedOfferId(bid, 5225, "98777");
        bid.mutable_value_for_search()->set_delta_operation(MBI::Bid::MODIFY);
        bid.mutable_value_for_search()->set_value(800);
        dumper.Dump(bid);

        // 2:
        bid.set_domain_type(MBI::Bid::FEED_OFFER_ID);
        bid.set_partner_id(155);
        SetFeedOfferId(bid, 5155, "Луноход 1");
        bid.mutable_value_for_search()->set_delta_operation(MBI::Bid::DELETE);
        bid.mutable_value_for_market_search_only()->set_delta_operation(MBI::Bid::DELETE);
        bid.mutable_value_for_card()->set_delta_operation(MBI::Bid::DELETE);
        bid.mutable_value_for_marketplace()->set_delta_operation(MBI::Bid::MODIFY);
        bid.mutable_value_for_marketplace()->set_value(900);
        dumper.Dump(bid);

        // 3:
        bid.set_domain_type(MBI::Bid::FEED_OFFER_ID);
        bid.set_partner_id(155);
        SetFeedOfferId(bid, 5155, "Марсоход Curiosity");
        bid.mutable_value_for_market_search_only()->set_delta_operation(MBI::Bid::MODIFY);
        bid.mutable_value_for_market_search_only()->set_value(500);
        bid.mutable_value_for_card()->set_delta_operation(MBI::Bid::DELETE);
        bid.mutable_value_for_marketplace()->set_delta_operation(MBI::Bid::DELETE);
        dumper.Dump(bid);

        // 4:
        bid.set_domain_type(MBI::Bid::FEED_OFFER_ID);
        bid.set_partner_id(155);
        SetFeedOfferId(bid, 5225, "98888");
        bid.mutable_value_for_card()->set_delta_operation(MBI::Bid::MODIFY);
        bid.mutable_value_for_card()->set_value(111);
        dumper.Dump(bid);

        // 5: with flag_dont_pull_up_bids=1
        bid.set_domain_type(MBI::Bid::FEED_OFFER_ID);
        bid.set_partner_id(155);
        SetFeedOfferId(bid, 9000, "5");
        bid.mutable_value_for_card()->set_delta_operation(MBI::Bid::MODIFY);
        bid.mutable_value_for_card()->set_value(111);
        bid.mutable_flag_dont_pull_up_bids()->set_delta_operation(MBI::Bid::MODIFY);
        bid.mutable_flag_dont_pull_up_bids()->set_value(1);
        dumper.Dump(bid);

        // 6: with delete flag_dont_pull_up_bids
        bid.set_domain_type(MBI::Bid::FEED_OFFER_ID);
        bid.set_partner_id(155);
        SetFeedOfferId(bid, 9000, "6");
        bid.mutable_value_for_card()->set_delta_operation(MBI::Bid::MODIFY);
        bid.mutable_value_for_card()->set_value(111);
        bid.mutable_flag_dont_pull_up_bids()->set_delta_operation(MBI::Bid::DELETE);
        bid.mutable_flag_dont_pull_up_bids()->set_value(0);
        dumper.Dump(bid);

        // 7: flag_dont_pull_up_bids=1 only
        bid.set_domain_type(MBI::Bid::FEED_OFFER_ID);
        bid.set_partner_id(155);
        SetFeedOfferId(bid, 9000, "7");
        bid.mutable_flag_dont_pull_up_bids()->set_delta_operation(MBI::Bid::MODIFY);
        bid.mutable_flag_dont_pull_up_bids()->set_value(1);
        dumper.Dump(bid);

        // 8: base fild didn't have flag_dont_pull_up_bids. Snapshot set it, Delta deleted it.
        bid.set_domain_type(MBI::Bid::FEED_OFFER_ID);
        bid.set_partner_id(155);
        SetFeedOfferId(bid, 9000, "8");
        bid.mutable_flag_dont_pull_up_bids()->set_delta_operation(MBI::Bid::DELETE);
        bid.mutable_flag_dont_pull_up_bids()->set_value(0);
        dumper.Dump(bid);
    }

    void CreateInputDelta2()
    {
        MBI::TDumper dumper(INPUT_DELTA2());
        MBI::Bid bid;

        // 0:
        bid.set_domain_type(MBI::Bid::FEED_OFFER_ID);
        bid.set_partner_id(155);
        SetFeedOfferId(bid, 1003, "11777");
        bid.mutable_value_for_search()->set_delta_operation(MBI::Bid::DELETE);
        dumper.Dump(bid);
    }

    void CreateSnapshot()
    {
        MBI::TDumper dumper(INPUT_SNAPSHOT());
        MBI::Bid bid;


        // 0:
        bid.set_domain_type(MBI::Bid::FEED_OFFER_ID);
        bid.set_partner_id(155);
        SetFeedOfferId(bid, 1003, "11777");
        bid.mutable_value_for_search()->set_value(300);
        bid.mutable_flag_dont_pull_up_bids()->set_value(1);
        /* потерли mbid = 400 */
        dumper.Dump(bid);

        // 1:
        bid.set_domain_type(MBI::Bid::FEED_OFFER_ID);
        bid.set_partner_id(155);
        SetFeedOfferId(bid, 5225, "98777");
        bid.mutable_value_for_search()->set_value(800);
        dumper.Dump(bid);

        // 2:
        bid.set_domain_type(MBI::Bid::FEED_OFFER_ID);
        bid.set_partner_id(155);
        SetFeedOfferId(bid, 5155, "Луноход 1");
        bid.mutable_value_for_marketplace()->set_value(900);
        dumper.Dump(bid);

        // 3:
        bid.set_domain_type(MBI::Bid::FEED_OFFER_ID);
        bid.set_partner_id(155);
        SetFeedOfferId(bid, 5155, "Марсоход Curiosity");
        bid.mutable_value_for_market_search_only()->set_value(500);
        dumper.Dump(bid);

        // 6:
        bid.set_domain_type(MBI::Bid::FEED_OFFER_ID);
        bid.set_partner_id(155);
        SetFeedOfferId(bid, 9000, "6");
        bid.mutable_flag_dont_pull_up_bids()->set_value(1);
        dumper.Dump(bid);

        // 8:
        bid.set_domain_type(MBI::Bid::FEED_OFFER_ID);
        bid.set_partner_id(155);
        SetFeedOfferId(bid, 9000, "8");
        bid.mutable_flag_dont_pull_up_bids()->set_value(1);
        dumper.Dump(bid);

        /* полностью удалили 4  */
    }
};

void CheckInsertIntoReportBids()
{
    {
        std::ofstream f(INDELTA_LIST().c_str(), std::ofstream::trunc);
        f << INPUT_SNAPSHOT() << "\n"
            << INPUT_DELTA() << "\n"
            << INPUT_DELTA2();
    }

    NQBid::UpdateQBids(INSTRUCTION(),
        MINIMAL_BID(),
        META_VALUES(),
        QBID_VALUES(),
        QBID_FLAGS(),
        BEFORE_VALUES(),
        OFFERID_VALUES(),
        INDELTA_LIST(),
        BIDS_TIMESTAMPS_FB()
    );

    const Market::MMap result(QBID_VALUES().c_str(), PROT_READ, MAP_PRIVATE);
    const NQBid::TBidInfo* info = static_cast<const NQBid::TBidInfo*>(result.data());

    const Market::MMap flagsResult(QBID_FLAGS().c_str(), PROT_READ, MAP_PRIVATE);
    const uint8_t* flagsInfo = static_cast<const uint8_t*>(flagsResult.data());

    ASSERT_EQ(info[0].YBid, 100);
    ASSERT_EQ(info[0].MBid, 400);
    ASSERT_EQ(info[0].CBid, 400);
    ASSERT_EQ(info[0].Fee, 0);
    ASSERT_EQ(flagsInfo[0], BIDS_FLAG_DONT_PULL_UP_BIDS);

    ASSERT_EQ(info[1].YBid, 800);
    ASSERT_EQ(info[1].MBid, 800);
    ASSERT_EQ(info[1].CBid, 800);
    ASSERT_EQ(info[1].Fee, 1);
    ASSERT_EQ(flagsInfo[1], 0);

    ASSERT_EQ(info[2].YBid, 102);
    ASSERT_EQ(info[2].MBid, 102);
    ASSERT_EQ(info[2].CBid, 2);
    ASSERT_EQ(info[2].Fee, 900);
    ASSERT_EQ(flagsInfo[2], 0);

    ASSERT_EQ(info[3].YBid, 103);
    ASSERT_EQ(info[3].MBid, 500);
    ASSERT_EQ(info[3].CBid, 500);
    ASSERT_EQ(info[3].Fee, 3);
    ASSERT_EQ(flagsInfo[3], 0);

    ASSERT_EQ(info[4].YBid, 104);
    ASSERT_EQ(info[4].MBid, 104);
    ASSERT_EQ(info[4].CBid, 111);
    ASSERT_EQ(info[4].Fee, 4);
    ASSERT_EQ(flagsInfo[4], 0);

    //check flag_dont_pull_up_bids. It should not affect main data
    ASSERT_EQ(info[5].YBid, 105);
    ASSERT_EQ(info[5].MBid, 105);
    ASSERT_EQ(info[5].CBid, 111);
    ASSERT_EQ(info[5].Fee, 5);
    ASSERT_EQ(flagsInfo[5], BIDS_FLAG_DONT_PULL_UP_BIDS);

    ASSERT_EQ(info[6].YBid, 106);
    ASSERT_EQ(info[6].MBid, 106);
    ASSERT_EQ(info[6].CBid, 111);
    ASSERT_EQ(info[6].Fee, 6);
    ASSERT_EQ(flagsInfo[6], 0);

    ASSERT_EQ(info[7].YBid, 107);
    ASSERT_EQ(info[7].MBid, 107);
    ASSERT_EQ(info[7].CBid, 7);
    ASSERT_EQ(info[7].Fee, 7);
    ASSERT_EQ(flagsInfo[7], BIDS_FLAG_DONT_PULL_UP_BIDS);

    ASSERT_EQ(info[8].YBid, 108);
    ASSERT_EQ(info[8].MBid, 108);
    ASSERT_EQ(info[8].CBid, 8);
    ASSERT_EQ(info[8].Fee, 8);
    ASSERT_EQ(flagsInfo[8], 0);
}

void CheckInsertIntoReportSnapshotOnly()
{
    {
        std::ofstream f(INDELTA_LIST().c_str(), std::ofstream::trunc);
        f << INPUT_SNAPSHOT();
    }

    NQBid::UpdateQBids(INSTRUCTION(),
                       MINIMAL_BID(),
                       META_VALUES(),
                       QBID_VALUES(),
                       QBID_FLAGS(),
                       BEFORE_VALUES(),
                       OFFERID_VALUES(),
                       INDELTA_LIST(),
                       BIDS_TIMESTAMPS_FB()
                       );

    const Market::MMap result(QBID_VALUES().c_str(), PROT_READ, MAP_PRIVATE);
    const NQBid::TBidInfo* info = static_cast<const NQBid::TBidInfo*>(result.data());

    const Market::MMap flagsResult(QBID_FLAGS().c_str(), PROT_READ, MAP_PRIVATE);
    const uint8_t* flagsInfo = static_cast<const uint8_t*>(flagsResult.data());

    ASSERT_EQ(info[0].YBid, 300);
    ASSERT_EQ(info[0].MBid, 300);
    ASSERT_EQ(info[0].CBid, 300);
    ASSERT_EQ(info[0].Fee,    0);
    ASSERT_EQ(flagsInfo[0], BIDS_FLAG_DONT_PULL_UP_BIDS);

    ASSERT_EQ(info[1].YBid, 800);
    ASSERT_EQ(info[1].MBid, 800);
    ASSERT_EQ(info[1].CBid, 800);
    ASSERT_EQ(info[1].Fee,    1);
    ASSERT_EQ(flagsInfo[1], 0);

    ASSERT_EQ(info[2].YBid, 102);
    ASSERT_EQ(info[2].MBid, 102);
    ASSERT_EQ(info[2].CBid,   2);
    ASSERT_EQ(info[2].Fee,  900);
    ASSERT_EQ(flagsInfo[2], 0);

    ASSERT_EQ(info[3].YBid, 103);
    ASSERT_EQ(info[3].MBid, 500);
    ASSERT_EQ(info[3].CBid, 500);
    ASSERT_EQ(info[3].Fee,    3);
    ASSERT_EQ(flagsInfo[3], 0);

    ASSERT_EQ(info[4].YBid, 104);
    ASSERT_EQ(info[4].MBid, 104);
    ASSERT_EQ(info[4].CBid,   4);
    ASSERT_EQ(info[4].Fee,    4);
    ASSERT_EQ(flagsInfo[4], 0);

    ASSERT_EQ(info[6].YBid, 106);
    ASSERT_EQ(info[6].MBid, 106);
    ASSERT_EQ(info[6].CBid, 6);
    ASSERT_EQ(info[6].Fee, 6);
    ASSERT_EQ(flagsInfo[6], BIDS_FLAG_DONT_PULL_UP_BIDS);

    ASSERT_EQ(info[7].YBid, 107);
    ASSERT_EQ(info[7].MBid, 107);
    ASSERT_EQ(info[7].CBid, 7);
    ASSERT_EQ(info[7].Fee, 7);
    ASSERT_EQ(flagsInfo[7], 0);

    ASSERT_EQ(info[8].YBid, 108);
    ASSERT_EQ(info[8].MBid, 108);
    ASSERT_EQ(info[8].CBid, 8);
    ASSERT_EQ(info[8].Fee, 8);
    ASSERT_EQ(flagsInfo[8], BIDS_FLAG_DONT_PULL_UP_BIDS);
}

void CheckInsertIntoReportDeltaAndSnapshot()
{
    {
        std::ofstream f(INDELTA_LIST().c_str(), std::ofstream::trunc);
        f << INPUT_DELTA()   << "\n"
          << INPUT_SNAPSHOT();
    }

    NQBid::UpdateQBids(INSTRUCTION(),
                       MINIMAL_BID(),
                       META_VALUES(),
                       QBID_VALUES(),
                       QBID_FLAGS(),
                       BEFORE_VALUES(),
                       OFFERID_VALUES(),
                       INDELTA_LIST(),
                       BIDS_TIMESTAMPS_FB()
                       );

    const Market::MMap result(QBID_VALUES().c_str(), PROT_READ, MAP_PRIVATE);
    const NQBid::TBidInfo* info = static_cast<const NQBid::TBidInfo*>(result.data());

    const Market::MMap flagsResult(QBID_FLAGS().c_str(), PROT_READ, MAP_PRIVATE);
    const uint8_t* flagsInfo = static_cast<const uint8_t*>(flagsResult.data());

    ASSERT_EQ(info[0].YBid, 300);
    ASSERT_EQ(info[0].MBid, 300);
    ASSERT_EQ(info[0].CBid, 300);
    ASSERT_EQ(info[0].Fee,  0);
    ASSERT_EQ(flagsInfo[0], BIDS_FLAG_DONT_PULL_UP_BIDS);

    ASSERT_EQ(info[1].YBid, 800);
    ASSERT_EQ(info[1].MBid, 800);
    ASSERT_EQ(info[1].CBid, 800);
    ASSERT_EQ(info[1].Fee,  1);
    ASSERT_EQ(flagsInfo[1], 0);

    ASSERT_EQ(info[2].YBid, 102);
    ASSERT_EQ(info[2].MBid, 102);
    ASSERT_EQ(info[2].CBid,   2);
    ASSERT_EQ(info[2].Fee,  900);
    ASSERT_EQ(flagsInfo[2], 0);

    ASSERT_EQ(info[3].YBid, 103);
    ASSERT_EQ(info[3].MBid, 500);
    ASSERT_EQ(info[3].CBid, 500);
    ASSERT_EQ(info[3].Fee,    3);
    ASSERT_EQ(flagsInfo[3], 0);

    ASSERT_EQ(info[4].YBid, 104);
    ASSERT_EQ(info[4].MBid, 104);
    ASSERT_EQ(info[4].CBid,   4);
    ASSERT_EQ(info[4].Fee,    4);
    ASSERT_EQ(flagsInfo[4], 0);

    ASSERT_EQ(info[6].YBid, 106);
    ASSERT_EQ(info[6].MBid, 106);
    ASSERT_EQ(info[6].CBid, 6);
    ASSERT_EQ(info[6].Fee, 6);
    ASSERT_EQ(flagsInfo[6], BIDS_FLAG_DONT_PULL_UP_BIDS);

    ASSERT_EQ(info[7].YBid, 107);
    ASSERT_EQ(info[7].MBid, 107);
    ASSERT_EQ(info[7].CBid, 7);
    ASSERT_EQ(info[7].Fee, 7);
    ASSERT_EQ(flagsInfo[7], 0);

    ASSERT_EQ(info[8].YBid, 108);
    ASSERT_EQ(info[8].MBid, 108);
    ASSERT_EQ(info[8].CBid, 8);
    ASSERT_EQ(info[8].Fee, 8);
    ASSERT_EQ(flagsInfo[8], BIDS_FLAG_DONT_PULL_UP_BIDS);
}


class TQBidsUpdatorLegacy : public TQBidsUpdator
{
    MBI_PROTOCOL_DEPRECATED_IMPL
};
class TQBidsUpdatorNew : public TQBidsUpdator
{
    MBI_PROTOCOL_NEW_IMPL
};

TEST_F(TQBidsUpdatorLegacy, InsertIntoReportBidsLegacy) { CheckInsertIntoReportBids(); }
TEST_F(TQBidsUpdatorNew, InsertIntoReportBidsNew) { CheckInsertIntoReportBids(); }

TEST_F(TQBidsUpdatorLegacy, InsertIntoReportSnapshotOnlyLegacy) { CheckInsertIntoReportSnapshotOnly(); }
TEST_F(TQBidsUpdatorNew, InsertIntoReportSnapshotOnlyNew) { CheckInsertIntoReportSnapshotOnly(); }

TEST_F(TQBidsUpdatorLegacy, InsertIntoReportDeltaAndSnapshotLegacy) { CheckInsertIntoReportDeltaAndSnapshot(); }
TEST_F(TQBidsUpdatorNew, InsertIntoReportDeltaAndSnapshotNew) { CheckInsertIntoReportDeltaAndSnapshot(); }

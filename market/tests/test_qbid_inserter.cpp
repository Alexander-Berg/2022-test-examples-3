#include <market/qpipe/qbid/delta_inserter/delta_inserter.cpp>
#include <market/qpipe/qbid/qbidengine/legacy.h>
#include <market/qpipe/qbid/qbidengine/qbid_validator.h>

#include "util.h"
#include "legacy_for_test.h"
#include <library/cpp/testing/unittest/gtest.h>


using NMarket::BLUE_VIRTUAL_FEED_ID;


TEST(NQBid, DeltaInserter_NUtil)
{
    MBI::Bid bid;
    ASSERT_FALSE(NQBid::NUtil::IsAny(bid));

    bid.mutable_value_for_search()->set_delta_operation(MBI::Bid::ADD);
    ASSERT_TRUE(NQBid::NUtil::IsAny(bid));

    bid.mutable_value_for_market_search_only()->set_delta_operation(MBI::Bid::ADD);
    ASSERT_TRUE(NQBid::NUtil::IsAny(bid));

    bid.mutable_value_for_card()->set_delta_operation(MBI::Bid::ADD);
    ASSERT_TRUE(NQBid::NUtil::IsAny(bid));

    bid.mutable_value_for_marketplace()->set_delta_operation(MBI::Bid::DELETE);
    ASSERT_TRUE(NQBid::NUtil::IsAny(bid));

    bid.mutable_flag_dont_pull_up_bids()->set_delta_operation(MBI::Bid::ADD);
    ASSERT_TRUE(NQBid::NUtil::IsAny(bid));

    /**
     * ybid - add 1
     * mbid - add 2
     * cbid - mod 3
     * fee  - del 4
     * flag_dont_pull_up_bids - add 5
     */

    MBI::Bid onlyFlagBid;
    onlyFlagBid.mutable_flag_dont_pull_up_bids()->set_delta_operation(MBI::Bid::ADD);
    ASSERT_TRUE(NQBid::NUtil::IsAny(onlyFlagBid));

    bid.mutable_value_for_search()->set_delta_operation(MBI::Bid::ADD);
    bid.mutable_value_for_market_search_only()->set_delta_operation(MBI::Bid::ADD);
    bid.mutable_value_for_card()->set_delta_operation(MBI::Bid::MODIFY);
    bid.mutable_value_for_marketplace()->set_delta_operation(MBI::Bid::DELETE);
    bid.mutable_flag_dont_pull_up_bids()->set_delta_operation(MBI::Bid::ADD);

    bid.mutable_value_for_search()->set_value(1);
    bid.mutable_value_for_market_search_only()->set_value(2);
    bid.mutable_value_for_card()->set_value(3);
    bid.mutable_value_for_marketplace()->set_value(4);
    bid.mutable_flag_dont_pull_up_bids()->set_value(1);

    bid.mutable_value_for_card()->set_modification_time(55555);

    MBI::Bid original;
    original.mutable_value_for_search()->set_value(5);
    //marketsearchonly not presents
    original.mutable_value_for_search()->set_value(7);
    original.mutable_value_for_search()->set_value(8);

    NQBid::NUtil::CleverMerge(&original, &bid);
    ASSERT_TRUE(original.has_value_for_search());
    ASSERT_TRUE(original.has_value_for_market_search_only());
    ASSERT_TRUE(original.has_value_for_card());
    ASSERT_FALSE(original.has_value_for_marketplace());
    ASSERT_TRUE(original.has_flag_dont_pull_up_bids());

    ASSERT_EQ(1, original.value_for_search().value());
    ASSERT_EQ(2, original.value_for_market_search_only().value());
    ASSERT_EQ(3, original.value_for_card().value());
    ASSERT_EQ(55555, original.value_for_card().modification_time());
    ASSERT_EQ(1, original.flag_dont_pull_up_bids().value());
}

TEST(NQBid, NUtil_FilterOutDeletedRecords)
{
    MBI::Bid bid;
    bid.mutable_value_for_search()->set_delta_operation(MBI::Bid::DELETE);
    bid.mutable_value_for_search()->set_value(0);

    bid.mutable_flag_dont_pull_up_bids()->set_delta_operation(MBI::Bid::DELETE);
    bid.mutable_flag_dont_pull_up_bids()->set_value(0);

    bid.mutable_value_for_market_search_only()->set_delta_operation(MBI::Bid::MODIFY);
    bid.mutable_value_for_market_search_only()->set_value(1000);

    bid.mutable_value_for_card()->set_delta_operation(MBI::Bid::DELETE);
    bid.mutable_value_for_card()->set_value(0);

    NQBid::NUtil::FilterOutDeletedBids(&bid);
    ASSERT_FALSE(bid.has_value_for_search());
    ASSERT_FALSE(bid.has_flag_dont_pull_up_bids());
    ASSERT_TRUE(bid.has_value_for_market_search_only());
    ASSERT_FALSE(bid.has_value_for_card());

    ASSERT_EQ(1000, bid.value_for_market_search_only().value());
}

static TString TMP_DIR() { return "tmp/qbids_delta_inserter"; }
static TString INPUT_SNAPSHOT() { return TMP_DIR() + "/input_snapshot.pbuf.sn"; }
static TString INPUT_SNAPSHOT_META() { return TMP_DIR() + "/input_snapshot.meta"; }
static TString OUTPUT_SNAPSHOT() { return TMP_DIR() + "/output_snapshot.pbuf.sn"; }
static TString DELTA() { return TMP_DIR() + "/delta.pbuf.sn"; }
static TString DELTA_META() { return TMP_DIR() + "/delta.meta"; }


class TQBidDeltaInserterTest : public ::testing::Test, public IMbiProtocolLegacyWrap
{
public:
    TQBidDeltaInserterTest() {}
    virtual ~TQBidDeltaInserterTest() {}

protected:
    // Sets up the test fixture.
    virtual void SetUp()
    {
        create_test_environment(TMP_DIR());
        CreateDelta();
        CreateInputSnapshot();
    }

private:
    void Dump(MBI::TDumper& dumper, NQBid::TMetaCreator& meta, MBI::Bid& bid)
    {
        if (NQBid::TCheck::IsDigit<NQBid::TOfferDigitId>(bid.domain_id()))
            meta.ProcessOfferDigitIdBid(bid);
        else
            meta.ProcessOfferStringIdBid(bid);
        dumper.Dump(bid);
    }

    void CreateDelta()
    {
        MBI::TDumper dumper(DELTA());
        NQBid::TMetaCreator meta(DELTA_META());
        MBI::Bid bid;

        /* result position = 0, modified in delta */
        bid.set_domain_type(MBI::Bid::FEED_OFFER_ID);
        SetFeedOfferId(bid, 5235, "44554");
        bid.mutable_value_for_search()->set_delta_operation(MBI::Bid::MODIFY);
        bid.mutable_value_for_search()->set_value(104);
        Dump(dumper, meta, bid);

        /* result position = 1, modified in delta */
        bid.set_domain_type(MBI::Bid::FEED_OFFER_ID);
        SetFeedOfferId(bid, 5235, "44556");
        bid.mutable_value_for_search()->set_delta_operation(MBI::Bid::DELETE);
        Dump(dumper, meta, bid);

        /* result position = None, deleted in delta */
        bid.set_domain_type(MBI::Bid::FEED_OFFER_ID);
        SetFeedOfferId(bid, 5235, "44555");
        bid.mutable_value_for_search()->set_delta_operation(MBI::Bid::MODIFY);
        bid.mutable_value_for_search()->set_value(100);
        bid.mutable_value_for_card()->set_delta_operation(MBI::Bid::MODIFY);
        bid.mutable_value_for_card()->set_value(100);
        bid.mutable_value_for_market_search_only()->set_delta_operation(MBI::Bid::MODIFY);
        bid.mutable_value_for_market_search_only()->set_modification_time(20140925);
        bid.mutable_value_for_market_search_only()->set_value(400);
        bid.mutable_value_for_marketplace()->set_delta_operation(MBI::Bid::DELETE);
        Dump(dumper, meta, bid);

        /* result position = 5, added from delta  */
        bid.set_domain_type(MBI::Bid::FEED_OFFER_ID);
        SetFeedOfferId(bid, 5225, "44555");
        bid.mutable_value_for_search()->set_delta_operation(MBI::Bid::MODIFY);
        bid.mutable_value_for_search()->set_value(110);
        Dump(dumper, meta, bid);

        /* result position = 6, added from delta  */
        bid.set_domain_type(MBI::Bid::FEED_OFFER_ID);
        SetFeedOfferId(bid, 5235, "44557");
        bid.mutable_value_for_search()->set_delta_operation(MBI::Bid::DELETE);
        bid.mutable_value_for_search()->set_value(0);
        bid.mutable_value_for_market_search_only()->set_delta_operation(MBI::Bid::MODIFY);
        bid.mutable_value_for_market_search_only()->set_value(1000);
        bid.mutable_value_for_card()->set_delta_operation(MBI::Bid::DELETE);
        bid.mutable_value_for_card()->set_value(0);
        Dump(dumper, meta, bid);

        //result position = 7 - snapshot doesn't have any bids, delta has bids + flag_dont_pull_up_bids=1
        bid.set_domain_type(MBI::Bid::FEED_OFFER_ID);
        SetFeedOfferId(bid, 5235, "50000");
        bid.mutable_value_for_search()->set_delta_operation(MBI::Bid::MODIFY);
        bid.mutable_value_for_search()->set_value(100);
        bid.mutable_value_for_search()->set_modification_time(200000);
        bid.mutable_value_for_card()->set_delta_operation(MBI::Bid::MODIFY);
        bid.mutable_value_for_card()->set_value(200);
        bid.mutable_value_for_card()->set_modification_time(200000);
        bid.mutable_flag_dont_pull_up_bids()->set_delta_operation(MBI::Bid::MODIFY);
        bid.mutable_flag_dont_pull_up_bids()->set_value(1);
        bid.mutable_flag_dont_pull_up_bids()->set_modification_time(200000);
        Dump(dumper, meta, bid);

        //result position = 8 - snapshot doesn't have any bids, delta has only flag_dont_pull_up_bids=1
        bid.set_domain_type(MBI::Bid::FEED_OFFER_ID);
        SetFeedOfferId(bid, 5235, "50001");
        bid.mutable_flag_dont_pull_up_bids()->set_delta_operation(MBI::Bid::MODIFY);
        bid.mutable_flag_dont_pull_up_bids()->set_value(1);
        bid.mutable_flag_dont_pull_up_bids()->set_modification_time(200000);
        Dump(dumper, meta, bid);

        /* result position = 9, added from delta, virtual feed  */
        bid.set_domain_type(MBI::Bid::FEED_OFFER_ID);
        SetFeedOfferId(bid, BLUE_VIRTUAL_FEED_ID, "33222.very-long-sku-id-much-longer-than-20-symbols");
        bid.mutable_value_for_search()->set_delta_operation(MBI::Bid::MODIFY);
        bid.mutable_value_for_search()->set_value(311);
        Dump(dumper, meta, bid);

        /* result position = 10, added from delta, virtual feed, not virtualized  */
        bid.set_domain_type(MBI::Bid::FEED_OFFER_ID);
        SetFeedOfferId(bid, BLUE_VIRTUAL_FEED_ID, "332231100");
        bid.mutable_value_for_search()->set_delta_operation(MBI::Bid::MODIFY);
        bid.mutable_value_for_search()->set_value(312);
        Dump(dumper, meta, bid);

        /* result position = 11, added from delta, virtual feed, bad supplier feed id  */
        bid.set_domain_type(MBI::Bid::FEED_OFFER_ID);
        SetFeedOfferId(bid, BLUE_VIRTUAL_FEED_ID, "qqq33224ww.sku1");
        bid.mutable_value_for_search()->set_delta_operation(MBI::Bid::MODIFY);
        bid.mutable_value_for_search()->set_value(313);
        Dump(dumper, meta, bid);

        //result position = 12 - snapshot doesn't have any bids, delta has deleted flag_dont_pull_up_bids
        bid.set_domain_type(MBI::Bid::FEED_OFFER_ID);
        SetFeedOfferId(bid, 5235, "50003");
        bid.mutable_flag_dont_pull_up_bids()->set_delta_operation(MBI::Bid::DELETE);
        bid.mutable_flag_dont_pull_up_bids()->set_modification_time(200000);
        Dump(dumper, meta, bid);

        //result position = 2 - snapshot has bids, delta has flag_dont_pull_up_bids=1
        bid.set_domain_type(MBI::Bid::FEED_OFFER_ID);
        SetFeedOfferId(bid, 5235, "50004");
        bid.mutable_flag_dont_pull_up_bids()->set_delta_operation(MBI::Bid::MODIFY);
        bid.mutable_flag_dont_pull_up_bids()->set_value(1);
        bid.mutable_flag_dont_pull_up_bids()->set_modification_time(200000);
        Dump(dumper, meta, bid);

        //result position = 3 - snapshot has bids + flag_dont_pull_up_bids=1, delta has deleted flag_dont_pull_up_bids
        bid.set_domain_type(MBI::Bid::FEED_OFFER_ID);
        SetFeedOfferId(bid, 5235, "50006");
        bid.mutable_flag_dont_pull_up_bids()->set_delta_operation(MBI::Bid::DELETE);
        bid.mutable_flag_dont_pull_up_bids()->set_modification_time(200000);
        Dump(dumper, meta, bid);

        //result position = 4 - snapshot has only flag_dont_pull_up_bids, delta adds bids
        bid.set_domain_type(MBI::Bid::FEED_OFFER_ID);
        SetFeedOfferId(bid, 5235, "50007");
        bid.mutable_value_for_search()->set_delta_operation(MBI::Bid::MODIFY);
        bid.mutable_value_for_search()->set_value(100);
        bid.mutable_value_for_search()->set_modification_time(200000);
        bid.mutable_value_for_card()->set_delta_operation(MBI::Bid::MODIFY);
        bid.mutable_value_for_card()->set_value(200);
        bid.mutable_value_for_card()->set_modification_time(200000);
        Dump(dumper, meta, bid);
    }

    void CreateInputSnapshot()
    {
        MBI::TDumper dumper(INPUT_SNAPSHOT());
        NQBid::TMetaCreator meta(INPUT_SNAPSHOT_META());
        MBI::Bid bid;

        /* result position = 0, modified in delta*/
        bid.set_domain_type(MBI::Bid::FEED_OFFER_ID);
        SetFeedOfferId(bid, 5235, "44554");
        bid.mutable_value_for_search()->set_value(14);
        Dump(dumper, meta, bid);

        /* result position = None, deleted in delta*/
        bid.set_domain_type(MBI::Bid::FEED_OFFER_ID);
        SetFeedOfferId(bid, 5235, "44556");
        bid.mutable_value_for_search()->set_value(15);
        Dump(dumper, meta, bid);

        /* result position = 1, modified in delta + deleted marketplace*/
        bid.set_domain_type(MBI::Bid::FEED_OFFER_ID);
        SetFeedOfferId(bid, 5235, "44555");
        bid.mutable_value_for_search()->set_value(10);
        bid.mutable_value_for_card()->set_value(10);
        bid.mutable_value_for_market_search_only()->set_value(40);
        bid.mutable_value_for_marketplace()->set_value(5);
        Dump(dumper, meta, bid);

        //result position = 2 - snapshot has bids, delta has flag_dont_pull_up_bids=1
        bid.set_domain_type(MBI::Bid::FEED_OFFER_ID);
        SetFeedOfferId(bid, 5235, "50004");
        bid.mutable_value_for_search()->set_value(100);
        bid.mutable_value_for_card()->set_value(200);
        Dump(dumper, meta, bid);

        //result position = 3 -snapshot has bids + flag_dont_pull_up_bids, delta has deleted flag_dont_pull_up_bids
        bid.set_domain_type(MBI::Bid::FEED_OFFER_ID);
        SetFeedOfferId(bid, 5235, "50006");
        bid.mutable_value_for_search()->set_value(100);
        bid.mutable_value_for_card()->set_value(200);
        bid.mutable_flag_dont_pull_up_bids()->set_value(1);
        Dump(dumper, meta, bid);

        //result position = 4 - snapshot has only flag_dont_pull_up_bids, delta adds bids
        bid.set_domain_type(MBI::Bid::FEED_OFFER_ID);
        SetFeedOfferId(bid, 5235, "50007");
        bid.mutable_flag_dont_pull_up_bids()->set_value(1);
        Dump(dumper, meta, bid);

        // result position = 5-10, must be added in delta/
    }
};


void CheckInserter()
{
    using namespace NQBid;

    NQBid::Insert(OUTPUT_SNAPSHOT(),
                  INPUT_SNAPSHOT(),
                  DELTA(),
                  DELTA_META());

    MBI::Parcel parcel;
    NMarket::TSnappyProtoReader reader(OUTPUT_SNAPSHOT(), "BIDS");
    reader.Load(parcel);

    /* result position = 0 */
    MBI::Bid* bid = parcel.mutable_bids(0);
    ASSERT_EQ(5235, NLegacy::GetFeedId(*bid));
    ASSERT_EQ("44554", NLegacy::GetOfferId(*bid));
    ASSERT_EQ(104, bid->value_for_search().value());
    ASSERT_TRUE(NQBid::NValidator::BidTarget_Offer::ValidateFeedId(*bid));
    ASSERT_TRUE(NQBid::NValidator::BidTarget_Offer::ValidateOfferId(*bid));

    /* result position = None (deleted)*/

    /* result position = 1 */
    bid = parcel.mutable_bids(1);
    ASSERT_EQ(5235, NLegacy::GetFeedId(*bid));
    ASSERT_EQ("44555", NLegacy::GetOfferId(*bid));
    ASSERT_EQ(100, bid->value_for_search().value());
    ASSERT_EQ(100, bid->value_for_card().value());
    ASSERT_EQ(400, bid->value_for_market_search_only().value());
    ASSERT_FALSE(bid->has_value_for_marketplace());
    ASSERT_TRUE(NQBid::NValidator::BidTarget_Offer::ValidateFeedId(*bid));
    ASSERT_TRUE(NQBid::NValidator::BidTarget_Offer::ValidateOfferId(*bid));

    //result position = 2 - snapshot has bids, delta has flag_dont_pull_up_bids=1
    bid = parcel.mutable_bids(2);
    ASSERT_EQ(5235, NLegacy::GetFeedId(*bid));
    ASSERT_EQ("50004", NLegacy::GetOfferId(*bid));
    ASSERT_EQ(100, bid->value_for_search().value());
    ASSERT_EQ(200, bid->value_for_card().value());
    ASSERT_EQ(1, bid->flag_dont_pull_up_bids().value());
    ASSERT_TRUE(NQBid::NValidator::BidTarget_Offer::ValidateFeedId(*bid));
    ASSERT_TRUE(NQBid::NValidator::BidTarget_Offer::ValidateOfferId(*bid));

    //result position = 3 -snapshot has bids + flag_dont_pull_up_bids, delta has deleted flag_dont_pull_up_bids
    bid = parcel.mutable_bids(3);
    ASSERT_EQ(5235, NLegacy::GetFeedId(*bid));
    ASSERT_EQ("50006", NLegacy::GetOfferId(*bid));
    ASSERT_EQ(100, bid->value_for_search().value());
    ASSERT_EQ(200, bid->value_for_card().value());
    ASSERT_FALSE(bid->has_flag_dont_pull_up_bids());
    ASSERT_TRUE(NQBid::NValidator::BidTarget_Offer::ValidateFeedId(*bid));
    ASSERT_TRUE(NQBid::NValidator::BidTarget_Offer::ValidateOfferId(*bid));

    //result position = 4 - snapshot has only flag_dont_pull_up_bids, delta adds bids
    bid = parcel.mutable_bids(4);
    ASSERT_EQ(5235, NLegacy::GetFeedId(*bid));
    ASSERT_EQ("50007", NLegacy::GetOfferId(*bid));
    ASSERT_EQ(100, bid->value_for_search().value());
    ASSERT_EQ(200, bid->value_for_card().value());
    ASSERT_EQ(1, bid->flag_dont_pull_up_bids().value());
    ASSERT_TRUE(NQBid::NValidator::BidTarget_Offer::ValidateFeedId(*bid));
    ASSERT_TRUE(NQBid::NValidator::BidTarget_Offer::ValidateOfferId(*bid));

    /* result position = 5 */
    bid = parcel.mutable_bids(5);
    ASSERT_EQ(5225, NLegacy::GetFeedId(*bid));
    ASSERT_EQ("44555", NLegacy::GetOfferId(*bid));
    ASSERT_EQ(110, bid->value_for_search().value());
    ASSERT_TRUE(NQBid::NValidator::BidTarget_Offer::ValidateFeedId(*bid));
    ASSERT_TRUE(NQBid::NValidator::BidTarget_Offer::ValidateOfferId(*bid));

    /* result position - 6 */
    bid = parcel.mutable_bids(6);
    ASSERT_EQ(5235, NLegacy::GetFeedId(*bid));
    ASSERT_EQ("44557", NLegacy::GetOfferId(*bid));
    ASSERT_FALSE(bid->has_value_for_search());
    ASSERT_EQ(1000, bid->value_for_market_search_only().value());
    ASSERT_FALSE(bid->has_value_for_card());
    ASSERT_TRUE(NQBid::NValidator::BidTarget_Offer::ValidateFeedId(*bid));
    ASSERT_TRUE(NQBid::NValidator::BidTarget_Offer::ValidateOfferId(*bid));

    //result position = 7 - snapshot doesn't have any bids, delta has bids + flag_dont_pull_up_bids=1
    bid = parcel.mutable_bids(7);
    ASSERT_EQ(5235, NLegacy::GetFeedId(*bid));
    ASSERT_EQ("50000", NLegacy::GetOfferId(*bid));
    ASSERT_EQ(100, bid->value_for_search().value());
    ASSERT_EQ(200, bid->value_for_card().value());
    ASSERT_EQ(1, bid->flag_dont_pull_up_bids().value());
    ASSERT_TRUE(NQBid::NValidator::BidTarget_Offer::ValidateFeedId(*bid));
    ASSERT_TRUE(NQBid::NValidator::BidTarget_Offer::ValidateOfferId(*bid));

    //result position = 8 - snapshot doesn't have any bids, delta has only flag_dont_pull_up_bids=1
    bid = parcel.mutable_bids(8);
    ASSERT_EQ(5235, NLegacy::GetFeedId(*bid));
    ASSERT_EQ("50001", NLegacy::GetOfferId(*bid));
    ASSERT_FALSE(bid->has_value_for_search());
    ASSERT_FALSE(bid->has_value_for_card());
    ASSERT_EQ(1, bid->flag_dont_pull_up_bids().value());
    ASSERT_TRUE(NQBid::NValidator::BidTarget_Offer::ValidateFeedId(*bid));
    ASSERT_TRUE(NQBid::NValidator::BidTarget_Offer::ValidateOfferId(*bid));

    /* result position = 9, devirtualization */
    bid = parcel.mutable_bids(9);
    ASSERT_EQ(33222, NLegacy::GetFeedId(*bid));
    ASSERT_EQ("very-long-sku-id-much-longer-than-20-symbols", NLegacy::GetOfferId(*bid));
    ASSERT_EQ(311, bid->value_for_search().value());
    ASSERT_TRUE(NQBid::NValidator::BidTarget_Offer::ValidateFeedId(*bid));
    ASSERT_TRUE(NQBid::NValidator::BidTarget_Offer::ValidateOfferId(*bid));

    /* result position = 10, devirtualization fails */
    bid = parcel.mutable_bids(10);
    ASSERT_EQ(BLUE_VIRTUAL_FEED_ID, NLegacy::GetFeedId(*bid));
    ASSERT_EQ("332231100", NLegacy::GetOfferId(*bid));
    ASSERT_EQ(312, bid->value_for_search().value());

    /* result position = 11, devirtualization fails */
    bid = parcel.mutable_bids(11);
    ASSERT_EQ(BLUE_VIRTUAL_FEED_ID, NLegacy::GetFeedId(*bid));
    ASSERT_EQ("qqq33224ww.sku1", NLegacy::GetOfferId(*bid));
    ASSERT_EQ(313, bid->value_for_search().value());

    //result position = 12 - snapshot doesn't have any bids, delta has deleted flag_dont_pull_up_bids
    //offer should not be in snapshot
    ASSERT_EQ(parcel.bids_size(), 12);
}

class TQBidDeltaInserterTestLegacy : public TQBidDeltaInserterTest
{
    MBI_PROTOCOL_DEPRECATED_IMPL
};
class TQBidDeltaInserterTestNew : public TQBidDeltaInserterTest
{
    MBI_PROTOCOL_NEW_IMPL
};


TEST_F(TQBidDeltaInserterTestLegacy, InserterLegacy) { CheckInserter(); }
TEST_F(TQBidDeltaInserterTestNew, InserterNew) { CheckInserter(); }

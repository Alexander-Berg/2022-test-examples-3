#include <market/qpipe/qbid/delta_sorter/delta_sorter.cpp>
#include <market/qpipe/qbid/qbidengine/legacy.h>

#include "util.h"
#include "legacy_for_test.h"
#include <library/cpp/testing/unittest/gtest.h>

static TString TMP_DIR() { return "tmp/qbids_delta_sorter"; }
static TString DELTA() { return TMP_DIR() + "/delta.pbuf.sn"; }
static TString META() { return TMP_DIR() + "/delta.meta"; }

static TString BAD_DELTA_TYPE() { return TMP_DIR() + "/bad.type.delta.pbuf.sh"; }
static TString BAD_DELTA_VALUE() { return TMP_DIR() + "/bad.value.delta.pbuf.sh"; }
static TString BAD_DELTA_FEED() { return TMP_DIR() + "/bad.feed.delta.pbuf.sn"; }
static TString BAD_DELTA_EMPTY() { return TMP_DIR() + "/bad.empty.delta.pbuf.sn"; }

class TQBidDeltaSorterTest : public ::testing::Test, public IMbiProtocolLegacyWrap
{
public:
    TQBidDeltaSorterTest() {}
    virtual ~TQBidDeltaSorterTest() {}

protected:
    // Sets up the test fixture.
    virtual void SetUp()
    {
        create_test_environment(TMP_DIR());
        CreateDelta();
        CreateBadTypedDelta();
        CreateBadValuedDelta();
        CreateBadFeededDelta();
        CreateBadEmptyDelta();
    }

private:
    void CreateDelta()
    {
        MBI::TDumper dumper(DELTA());
        MBI::Bid bid;

        /* 1 */
        bid.set_domain_type(MBI::Bid::FEED_OFFER_ID);
        SetFeedOfferId(bid, 5235, "44554");
        bid.mutable_value_for_search()->set_delta_operation(MBI::Bid::MODIFY);
        bid.mutable_value_for_search()->set_value(104);
        dumper.Dump(bid);

        /* 3 */
        bid.set_domain_type(MBI::Bid::FEED_OFFER_ID);
        SetFeedOfferId(bid, 5235, "44556");
        bid.mutable_value_for_search()->set_delta_operation(MBI::Bid::MODIFY);
        bid.mutable_value_for_search()->set_value(105);
        dumper.Dump(bid);

        /* 2 */
        bid.set_domain_type(MBI::Bid::FEED_OFFER_ID);
        SetFeedOfferId(bid, 5235, "44555");
        bid.mutable_value_for_search()->set_delta_operation(MBI::Bid::MODIFY);
        bid.mutable_value_for_search()->set_value(100);
        bid.mutable_value_for_card()->set_delta_operation(MBI::Bid::MODIFY);
        bid.mutable_value_for_card()->set_value(100);
        dumper.Dump(bid);

        /* 0 */
        bid.set_domain_type(MBI::Bid::FEED_OFFER_ID);
        SetFeedOfferId(bid, 5225, "44555");
        bid.mutable_value_for_search()->set_delta_operation(MBI::Bid::MODIFY);
        bid.mutable_value_for_search()->set_value(110);
        dumper.Dump(bid);

        /* 2 */
        bid.set_domain_type(MBI::Bid::FEED_OFFER_ID);
        SetFeedOfferId(bid, 5235, "44555");
        bid.mutable_value_for_card()->set_delta_operation(MBI::Bid::DELETE);
        bid.mutable_value_for_market_search_only()->set_delta_operation(MBI::Bid::DELETE);
        dumper.Dump(bid);

        /* 2 */
        bid.set_domain_type(MBI::Bid::FEED_OFFER_ID);
        SetFeedOfferId(bid, 5235, "44555");
        bid.mutable_value_for_market_search_only()->set_delta_operation(MBI::Bid::MODIFY);
        bid.mutable_value_for_market_search_only()->set_modification_time(20140925);
        bid.mutable_value_for_market_search_only()->set_value(400);
        dumper.Dump(bid);

        /* 2 */
        bid.set_domain_type(MBI::Bid::FEED_OFFER_ID);
        SetFeedOfferId(bid, 5235, "44555");
        bid.mutable_value_for_market_search_only()->set_delta_operation(MBI::Bid::MODIFY);
        bid.mutable_value_for_market_search_only()->set_modification_time(20140923);
        bid.mutable_value_for_market_search_only()->set_value(300);
        dumper.Dump(bid);

        //=========tests for flag_dont_pull_up_bids=========================
        //offer had bids, add flag_dont_pull_up_bids=1
        bid.set_domain_type(MBI::Bid::FEED_OFFER_ID);
        SetFeedOfferId(bid, 5235, "50000");
        bid.mutable_value_for_search()->set_delta_operation(MBI::Bid::MODIFY);
        bid.mutable_value_for_search()->set_value(100);
        bid.mutable_value_for_search()->set_modification_time(200000);
        bid.mutable_value_for_card()->set_delta_operation(MBI::Bid::MODIFY);
        bid.mutable_value_for_card()->set_value(200);
        bid.mutable_value_for_card()->set_modification_time(200000);
        dumper.Dump(bid);

        bid.set_domain_type(MBI::Bid::FEED_OFFER_ID);
        SetFeedOfferId(bid, 5235, "50000");
        bid.mutable_flag_dont_pull_up_bids()->set_delta_operation(MBI::Bid::MODIFY);
        bid.mutable_flag_dont_pull_up_bids()->set_value(1);
        bid.mutable_flag_dont_pull_up_bids()->set_modification_time(200001);
        dumper.Dump(bid);

        //offer had bids, add flag_dont_pull_up_bids=1, delete flag_dont_pull_up_bids
        bid.set_domain_type(MBI::Bid::FEED_OFFER_ID);
        SetFeedOfferId(bid, 5235, "50002");
        bid.mutable_value_for_search()->set_delta_operation(MBI::Bid::MODIFY);
        bid.mutable_value_for_search()->set_value(100);
        bid.mutable_value_for_search()->set_modification_time(200000);
        bid.mutable_value_for_card()->set_delta_operation(MBI::Bid::MODIFY);
        bid.mutable_value_for_card()->set_value(200);
        bid.mutable_value_for_card()->set_modification_time(200000);
        bid.mutable_flag_dont_pull_up_bids()->set_delta_operation(MBI::Bid::MODIFY);
        bid.mutable_flag_dont_pull_up_bids()->set_value(1);
        bid.mutable_flag_dont_pull_up_bids()->set_modification_time(200000);
        dumper.Dump(bid);

        bid.set_domain_type(MBI::Bid::FEED_OFFER_ID);
        SetFeedOfferId(bid, 5235, "50002");
        bid.mutable_flag_dont_pull_up_bids()->set_delta_operation(MBI::Bid::DELETE);
        bid.mutable_flag_dont_pull_up_bids()->set_modification_time(200001);
        dumper.Dump(bid);

        //offer didn't have bids, add flag_dont_pull_up_bids=1
        bid.set_domain_type(MBI::Bid::FEED_OFFER_ID);
        SetFeedOfferId(bid, 5235, "50003");
        bid.mutable_flag_dont_pull_up_bids()->set_delta_operation(MBI::Bid::MODIFY);
        bid.mutable_flag_dont_pull_up_bids()->set_value(1);
        bid.mutable_flag_dont_pull_up_bids()->set_modification_time(200000);
        dumper.Dump(bid);

        //offer had only flag_dont_pull_up_bids=1, set delete flag_dont_pull_up_bids
        SetFeedOfferId(bid, 5235, "50005");
        bid.set_domain_type(MBI::Bid::FEED_OFFER_ID);
        bid.mutable_flag_dont_pull_up_bids()->set_delta_operation(MBI::Bid::MODIFY);
        bid.mutable_flag_dont_pull_up_bids()->set_value(1);
        bid.mutable_flag_dont_pull_up_bids()->set_modification_time(200000);
        dumper.Dump(bid);

        bid.set_domain_type(MBI::Bid::FEED_OFFER_ID);
        SetFeedOfferId(bid, 5235, "50005");
        bid.mutable_flag_dont_pull_up_bids()->set_delta_operation(MBI::Bid::DELETE);
        bid.mutable_flag_dont_pull_up_bids()->set_modification_time(200001);
        dumper.Dump(bid);

        //offer had bids, set flag_dont_pull_up_bids=1 together with new values of bids
        bid.set_domain_type(MBI::Bid::FEED_OFFER_ID);
        SetFeedOfferId(bid, 5235, "50006");
        bid.mutable_value_for_search()->set_delta_operation(MBI::Bid::MODIFY);
        bid.mutable_value_for_search()->set_value(100);
        bid.mutable_value_for_search()->set_modification_time(200000);
        bid.mutable_value_for_card()->set_delta_operation(MBI::Bid::MODIFY);
        bid.mutable_value_for_card()->set_value(200);
        bid.mutable_value_for_card()->set_modification_time(200000);
        dumper.Dump(bid);

        bid.set_domain_type(MBI::Bid::FEED_OFFER_ID);
        SetFeedOfferId(bid, 5235, "50006");
        bid.mutable_value_for_search()->set_delta_operation(MBI::Bid::MODIFY);
        bid.mutable_value_for_search()->set_value(101);
        bid.mutable_value_for_search()->set_modification_time(200001);
        bid.mutable_value_for_card()->set_delta_operation(MBI::Bid::MODIFY);
        bid.mutable_value_for_card()->set_value(202);
        bid.mutable_value_for_card()->set_modification_time(200001);
        bid.mutable_flag_dont_pull_up_bids()->set_delta_operation(MBI::Bid::MODIFY);
        bid.mutable_flag_dont_pull_up_bids()->set_value(1);
        bid.mutable_flag_dont_pull_up_bids()->set_modification_time(200001);
        dumper.Dump(bid);
    }

    void CreateBadTypedDelta()
    {
        MBI::TDumper dumper(BAD_DELTA_TYPE());
        MBI::Bid bid;

        /* type != ID */
        bid.set_domain_type(MBI::Bid::OFFER_TITLE);
        SetFeedOfferId(bid, 5235, "44554");
        bid.mutable_value_for_search()->set_value(104);
        bid.mutable_value_for_search()->set_delta_operation(MBI::Bid::MODIFY);
        dumper.Dump(bid);
    }

    void CreateBadValuedDelta()
    {
        MBI::TDumper dumper(BAD_DELTA_VALUE());
        MBI::Bid bid;

        /* there is no delta_operation() */
        bid.set_domain_type(MBI::Bid::FEED_OFFER_ID);
        SetFeedOfferId(bid, 5235, "44554");
        bid.mutable_value_for_search()->set_value(104);
        dumper.Dump(bid);
    }

    void CreateBadFeededDelta()
    {
        MBI::TDumper dumper(BAD_DELTA_FEED());
        MBI::Bid bid;

        /* there is no feed_id() (or there is no offer_id in MBI_PROTOCOL_NEW_IMPL */
        bid.set_domain_type(MBI::Bid::FEED_OFFER_ID);
        SetDomainId(bid, "44554");
        bid.mutable_value_for_search()->set_value(104);
        bid.mutable_value_for_search()->set_delta_operation(MBI::Bid::MODIFY);
        dumper.Dump(bid);
    }

    void CreateBadEmptyDelta()
    {
        MBI::TDumper dumper(BAD_DELTA_EMPTY());
        MBI::Bid bid;

        /* there is no bids at all */
        bid.set_domain_type(MBI::Bid::FEED_OFFER_ID);
        SetFeedOfferId(bid, 55555, "55555");
        dumper.Dump(bid);
    }
};


void CheckSorter()
{
    using namespace NQBid;
    NQBid::SortDelta(DELTA(), META());

    MBI::Parcel parcel;
    NMarket::TSnappyProtoReader reader(DELTA(), "BIDS");
    reader.Load(parcel);

    MBI::Bid* bid = parcel.mutable_bids(0);
    ASSERT_EQ(5225, NLegacy::GetFeedId(*bid));
    ASSERT_EQ("44555", NLegacy::GetOfferId(*bid));

    bid = parcel.mutable_bids(1);
    ASSERT_EQ(5235, NLegacy::GetFeedId(*bid));
    ASSERT_EQ("44554", NLegacy::GetOfferId(*bid));

    bid = parcel.mutable_bids(2);
    ASSERT_EQ(5235, NLegacy::GetFeedId(*bid));
    ASSERT_EQ("44555", NLegacy::GetOfferId(*bid));
    ASSERT_EQ(100, bid->value_for_search().value());
    ASSERT_EQ(400, bid->value_for_market_search_only().value());
    ASSERT_FALSE(bid->has_value_for_card());

    bid = parcel.mutable_bids(3);
    ASSERT_EQ(5235, NLegacy::GetFeedId(*bid));
    ASSERT_EQ("44556", NLegacy::GetOfferId(*bid));

    //=========tests for flag_dont_pull_up_bids=========================
    //offer had bids, add flag_dont_pull_up_bids=1
    bid = parcel.mutable_bids(4);
    ASSERT_EQ(5235, NLegacy::GetFeedId(*bid));
    ASSERT_EQ("50000", NLegacy::GetOfferId(*bid));
    ASSERT_EQ(100, bid->value_for_search().value());
    ASSERT_EQ(200, bid->value_for_card().value());
    ASSERT_EQ(1, bid->flag_dont_pull_up_bids().value());

    //offer had bids, add flag_dont_pull_up_bids=1, delete flag_dont_pull_up_bids
    bid = parcel.mutable_bids(5);
    ASSERT_EQ(5235, NLegacy::GetFeedId(*bid));
    ASSERT_EQ("50002", NLegacy::GetOfferId(*bid));
    ASSERT_EQ(100, bid->value_for_search().value());
    ASSERT_EQ(200, bid->value_for_card().value());
    ASSERT_FALSE(bid->has_flag_dont_pull_up_bids());

    //offer didn't have bids, add flag_dont_pull_up_bids=1
    bid = parcel.mutable_bids(6);
    ASSERT_EQ(5235, NLegacy::GetFeedId(*bid));
    ASSERT_EQ("50003", NLegacy::GetOfferId(*bid));
    ASSERT_FALSE(bid->has_value_for_search());
    ASSERT_FALSE(bid->has_value_for_card());
    ASSERT_FALSE(bid->has_value_for_market_search_only());
    ASSERT_FALSE(bid->has_value_for_marketplace());
    ASSERT_EQ(1, bid->flag_dont_pull_up_bids().value());

    //offer had only flag_dont_pull_up_bids=1, set delete flag_dont_pull_up_bids
    //NB: empty delta is OK - it should be ignored in updater
    bid = parcel.mutable_bids(7);
    ASSERT_EQ(5235, NLegacy::GetFeedId(*bid));
    ASSERT_EQ("50005", NLegacy::GetOfferId(*bid));
    ASSERT_FALSE(bid->has_value_for_search());
    ASSERT_FALSE(bid->has_value_for_card());
    ASSERT_FALSE(bid->has_value_for_market_search_only());
    ASSERT_FALSE(bid->has_value_for_marketplace());
    ASSERT_FALSE(bid->has_flag_dont_pull_up_bids());

    //offer had bids, set flag_dont_pull_up_bids=1 together with new values of bids
    bid = parcel.mutable_bids(8);
    ASSERT_EQ(5235, NLegacy::GetFeedId(*bid));
    ASSERT_EQ("50006", NLegacy::GetOfferId(*bid));
    ASSERT_EQ(101, bid->value_for_search().value());
    ASSERT_EQ(202, bid->value_for_card().value());
    ASSERT_EQ(1, bid->flag_dont_pull_up_bids().value());

    /* validator tests */
    ASSERT_THROW(NQBid::SortDelta(BAD_DELTA_TYPE(), META()), yexception);
    ASSERT_THROW(NQBid::SortDelta(BAD_DELTA_VALUE(), META()), yexception);
    ASSERT_THROW(NQBid::SortDelta(BAD_DELTA_FEED(), META()), yexception);
    ASSERT_THROW(NQBid::SortDelta(BAD_DELTA_EMPTY(), META()), yexception);
}

class TQBidDeltaSorterTestLegacy : public TQBidDeltaSorterTest
{
    MBI_PROTOCOL_DEPRECATED_IMPL
};
class TQBidDeltaSorterTestNew : public TQBidDeltaSorterTest
{
    MBI_PROTOCOL_NEW_IMPL
};

TEST_F(TQBidDeltaSorterTestLegacy, SorterLegacy) { CheckSorter(); }
TEST_F(TQBidDeltaSorterTestNew, SorterNew) { CheckSorter(); }

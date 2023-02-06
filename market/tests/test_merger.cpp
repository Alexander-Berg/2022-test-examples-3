#include <market/qpipe/qbid/merger/merger.cpp>


#include <market/library/snappy-protostream/proto_snappy_stream.h>
#include <market/qpipe/qbid/qbidengine/mbi_bids.h>
#include <market/qpipe/qbid/qbidengine/legacy.h>


#include "util.h"
#include "legacy_for_test.h"
#include <util/string/vector.h>
#include <library/cpp/testing/unittest/gtest.h>

static TString TMP_DIR() { return "tmp/qbid_merger_temp"; }

class TMergerTest : public ::testing::Test, public IMbiProtocolLegacyWrap
{
public:
    virtual ~TMergerTest() {}

private:
    MBI::Parcel OR;
    MBI::Parcel R1;
    MBI::Parcel R2;
    MBI::Parcel R3;
    MBI::Parcel R4;


    void DumpStatus(MBI::Bid* bid, MBI::Bid::PublicationStatus status)
    {
        bid->mutable_value_for_search()->set_publication_status(status);
        bid->mutable_value_for_market_search_only()->set_publication_status(status);
        bid->mutable_value_for_card()->set_publication_status(status);
        bid->mutable_value_for_marketplace()->set_publication_status(status);
        bid->mutable_value_for_model_search()->set_publication_status(status);
        bid->mutable_flag_dont_pull_up_bids()->set_publication_status(status);
    }

    void SetSwitchValue(MBI::Bid* bid, unsigned switched_value)
    {
        bid->set_switch_feed_id(switched_value);

        const TString str_switched_value = ::ToString(switched_value);
        bid->set_switch_offer_id(str_switched_value.data(), str_switched_value.length());
    }

    void DumpResults(MBI::Bid::PublicationStatus s1,
                     MBI::Bid::PublicationStatus s2,
                     MBI::Bid::PublicationStatus s3,
                     MBI::Bid::PublicationStatus s4)
    {
        DumpStatus(R1.add_bids(), s1);
        DumpStatus(R2.add_bids(), s2);
        DumpStatus(R3.add_bids(), s3);
        DumpStatus(R4.add_bids(), s4);
    }
    void SwitchedDumpResults(
            unsigned switched_value,
            MBI::Bid::PublicationStatus s1,
            MBI::Bid::PublicationStatus s2,
            MBI::Bid::PublicationStatus s3,
            MBI::Bid::PublicationStatus s4)
    {
        MBI::Bid* b1 = R1.add_bids();
        DumpStatus(b1, s1);
        SetSwitchValue(b1, switched_value);

        MBI::Bid* b2 = R2.add_bids();
        DumpStatus(b2, s2);
        SetSwitchValue(b2, switched_value);

        MBI::Bid* b3 = R3.add_bids();
        DumpStatus(b3, s3);
        SetSwitchValue(b3, switched_value);

        MBI::Bid* b4 = R4.add_bids();
        DumpStatus(b4, s4);
        SetSwitchValue(b4, switched_value);
    }

    void DumpOriginalValue(MBI::Bid* bid, unsigned value)
    {
        /* set value */
        bid->mutable_value_for_search()->set_value(value);
        bid->mutable_value_for_market_search_only()->set_value(value);
        bid->mutable_value_for_card()->set_value(value);
        bid->mutable_value_for_marketplace()->set_value(value);
        bid->mutable_value_for_model_search()->set_value(value);
        bid->mutable_flag_dont_pull_up_bids()->set_value(value);

        /* set modification time */
        bid->mutable_value_for_search()->set_modification_time(value);
        bid->mutable_value_for_market_search_only()->set_modification_time(value);
        bid->mutable_value_for_card()->set_modification_time(value);
        bid->mutable_value_for_marketplace()->set_modification_time(value);
        bid->mutable_value_for_model_search()->set_modification_time(value);
        bid->mutable_flag_dont_pull_up_bids()->set_modification_time(value);
    }

public:
    void DoSetUp()
    {
        create_test_environment(TMP_DIR());

        /* dump test data */
        {
            NMarket::TSnappyProtoWriter originalStream(TMP_DIR()+"/original.pbuf.sn", "BIDS");
            NMarket::TSnappyProtoWriter r1Stream(TMP_DIR()+"/results_1.pbuf.sn", "BIDS");
            NMarket::TSnappyProtoWriter r2Stream(TMP_DIR()+"/results_2.pbuf.sn", "BIDS");
            NMarket::TSnappyProtoWriter r3Stream(TMP_DIR()+"/results_3.pbuf.sn", "BIDS");
            NMarket::TSnappyProtoWriter r4Stream(TMP_DIR()+"/results_4.pbuf.sn", "BIDS");

            this->InitTestData();

            originalStream.Write(OR);
            r1Stream.Write(R1);
            r2Stream.Write(R2);
            r3Stream.Write(R3);
            r4Stream.Write(R4);

            //flush to disk
        }

        const TString output_file   (TMP_DIR()+"/mbi.result.pbuf.sn");
        const TString original_file (TMP_DIR()+"/original.pbuf.sn"  );
        TVector<TString> input_files;
        input_files.reserve(4);
        for (unsigned idx=1; idx<=4; idx++)
            input_files.push_back(TMP_DIR() + "/results_" +
                    ::ToString(idx) + ".pbuf.sn");

        NQBid::Merge(output_file, original_file, input_files);
    }

    virtual void SetUp()
    {
        DoSetUp();
    }


private:
    void InitTestData()
    {
        using MBI::Bid;
        Bid* bid = nullptr;

        //0
        bid = OR.add_bids();
        bid->set_partner_id(10);
        bid->set_domain_type(MBI::Bid::SHOP_ID);
        this->DumpOriginalValue(bid, 10);
        this->DumpResults(Bid::NOT_FOUND, Bid::NOT_FOUND, Bid::APPLIED, Bid::APPLIED);

        //1
        bid = OR.add_bids();
        bid->set_partner_id(20);
        bid->set_domain_type(MBI::Bid::CATEGORY_ID);
        SetDomainId(*bid, "9");
        this->DumpOriginalValue(bid, 20);
        this->DumpResults(Bid::NOT_FOUND, Bid::NOT_FOUND, Bid::NOT_ALLOWED, Bid::NOT_FOUND);

        //2
        bid = OR.add_bids();
        bid->set_partner_id(30);
        bid->set_domain_type(MBI::Bid::OFFER_TITLE);
        SetDomainId(*bid, "THIS IS THE OFFER TITLE");
        this->DumpOriginalValue(bid, 30);
        this->SwitchedDumpResults(3333,
                Bid::APPLIED, Bid::NOT_FOUND, Bid::APPLIED, Bid::APPLIED_CORRECTED);

        //3
        bid = OR.add_bids();
        bid->set_partner_id(40);
        bid->set_domain_type(MBI::Bid::FEED_OFFER_ID);
        SetFeedOfferId(*bid, 44, "444");
        this->DumpOriginalValue(bid, 40);
        this->DumpResults(Bid::NOT_FOUND, Bid::NOT_FOUND, Bid::APPLIED, Bid::NOT_FOUND);

        //4
        bid = OR.add_bids();
        bid->set_partner_type(MBI::Bid::VENDOR);
        bid->set_target(MBI::Bid::MODEL);
        bid->set_partner_id(50);
        bid->set_domain_type(MBI::Bid::VENDOR_CATEGORY_ID);
        bid->add_domain_ids("555");
        bid->add_domain_ids("5555");
        this->DumpOriginalValue(bid, 50);
        this->DumpResults(Bid::APPLIED, Bid::NOT_ALLOWED, Bid::APPLIED, Bid::NOT_FOUND);
    }

};

void ASSERT_QBID_PUBLICATION_STATUS(const MBI::Bid::PublicationStatus status, const MBI::Bid* bid)
{
    ASSERT_EQ(status, bid->value_for_search().publication_status());
    ASSERT_EQ(status, bid->value_for_market_search_only().publication_status());
    ASSERT_EQ(status, bid->value_for_card().publication_status());
    ASSERT_EQ(status, bid->value_for_marketplace().publication_status());
    ASSERT_EQ(status, bid->value_for_model_search().publication_status());
    ASSERT_EQ(status, bid->flag_dont_pull_up_bids().publication_status());
}

void CheckAssertResults()
{
    using namespace NQBid;

    NMarket::TSnappyProtoReader reader(TMP_DIR()+"/mbi.result.pbuf.sn", "BIDS");
    MBI::Parcel input;
    reader.Load(input);

    const MBI::Bid* bid = nullptr;

    //0
    bid = &input.bids(0);
    ASSERT_EQ(10, bid->partner_id());
    ASSERT_EQ(MBI::Bid::SHOP_ID, bid->domain_type());

    ASSERT_EQ(10, bid->value_for_search().value());
    ASSERT_EQ(10, bid->value_for_model_search().value());
    ASSERT_QBID_PUBLICATION_STATUS(MBI::Bid::APPLIED, bid);
    ASSERT_FALSE(bid->has_switch_feed_id());
    ASSERT_FALSE(bid->has_switch_offer_id());


    //1
    bid = &input.bids(1);
    ASSERT_EQ(20, bid->partner_id());
    ASSERT_EQ(MBI::Bid::CATEGORY_ID, bid->domain_type());
    ASSERT_EQ("9", NLegacy::GetCategoryId(*bid));

    ASSERT_EQ(20, bid->value_for_search().value());
    ASSERT_EQ(20, bid->value_for_model_search().value());
    ASSERT_QBID_PUBLICATION_STATUS(MBI::Bid::NOT_ALLOWED, bid);
    ASSERT_FALSE(bid->has_switch_feed_id());
    ASSERT_FALSE(bid->has_switch_offer_id());


    //2
    bid = &input.bids(2);
    ASSERT_EQ(30, bid->partner_id());
    ASSERT_EQ(MBI::Bid::OFFER_TITLE, bid->domain_type());
    ASSERT_EQ("THIS IS THE OFFER TITLE", NLegacy::GetOfferTitle(*bid));

    ASSERT_EQ(30, bid->value_for_search().value());
    ASSERT_EQ(30, bid->value_for_model_search().value());
    ASSERT_QBID_PUBLICATION_STATUS(MBI::Bid::APPLIED_CORRECTED, bid);
    ASSERT_TRUE(bid->has_switch_feed_id());
    ASSERT_TRUE(bid->has_switch_offer_id());
    ASSERT_EQ(3333, bid->switch_feed_id());
    ASSERT_EQ("3333", bid->switch_offer_id());


    //3
    bid = &input.bids(3);
    ASSERT_EQ(40, bid->partner_id());
    ASSERT_EQ(MBI::Bid::FEED_OFFER_ID, bid->domain_type());
    ASSERT_EQ("444", NLegacy::GetOfferId(*bid));
    ASSERT_EQ(44, NLegacy::GetFeedId(*bid));

    ASSERT_EQ(40, bid->value_for_search().value());
    ASSERT_EQ(40, bid->value_for_model_search().value());
    ASSERT_QBID_PUBLICATION_STATUS(MBI::Bid::APPLIED, bid);
    ASSERT_FALSE(bid->has_switch_feed_id());
    ASSERT_FALSE(bid->has_switch_offer_id());


    //4
    bid = &input.bids(4);
    ASSERT_EQ(50, bid->partner_id());
    ASSERT_EQ(MBI::Bid::VENDOR, bid->partner_type());
    ASSERT_EQ(MBI::Bid::MODEL, bid->target());
    ASSERT_EQ(MBI::Bid::VENDOR_CATEGORY_ID, bid->domain_type());
    ASSERT_EQ("555", bid->domain_ids(0));
    ASSERT_EQ("5555", bid->domain_ids(1));

    ASSERT_EQ(50, bid->value_for_search().value());
    ASSERT_EQ(50, bid->value_for_model_search().value());
    ASSERT_EQ(50, bid->value_for_search().value());
    ASSERT_EQ(50, bid->value_for_model_search().value());
    ASSERT_QBID_PUBLICATION_STATUS(MBI::Bid::NOT_ALLOWED, bid);
    ASSERT_FALSE(bid->has_switch_feed_id());
    ASSERT_FALSE(bid->has_switch_offer_id());
}

class TMergerTestLegacy : public TMergerTest
{
    MBI_PROTOCOL_DEPRECATED_IMPL
};
class TMergerTestNew : public TMergerTest
{
    MBI_PROTOCOL_NEW_IMPL
};


TEST_F(TMergerTestLegacy, AssertResultsLegacy) { CheckAssertResults(); }
TEST_F(TMergerTestNew, AssertResultsNew) { CheckAssertResults(); }

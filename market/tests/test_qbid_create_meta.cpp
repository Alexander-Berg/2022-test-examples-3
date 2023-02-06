#include <market/qpipe/qbid/create_meta/create_meta.cpp>
#include <market/qpipe/qbid/qbidengine/qbid_utils.h>
#include <google/protobuf/descriptor.h>

#include "util.h"
#include <util/stream/file.h>
#include <library/cpp/testing/unittest/gtest.h>
#include <utility>
#include <vector>
#include <type_traits>

using namespace NQBid;

class TQBidCreateMetaTestBase : public ::testing::Test
{
public:
    static TString TMP_DIR(){ return "tmp/qbid_temp"; }

protected:
    void DumpBidValues(MBI::Bid* bid, const std::vector<int> &bids)
    {
        if (bids[0] != -1)
            bid->mutable_value_for_search()->set_value(bids[0]);
        if (bids[1] != -1)
            bid->mutable_value_for_card()->set_value(bids[1]);
        if (bids[2] != -1)
            bid->mutable_value_for_marketplace()->set_value(bids[2]);
        if (bids[3] != -1)
            bid->mutable_value_for_market_search_only()->set_value(bids[3]);
        if (bids[4] != -1)
            bid->mutable_value_for_model_search()->set_value(bids[4]);
        if (bids[5] != -1)
            bid->mutable_flag_dont_pull_up_bids()->set_value(bids[5]);
    }

    void DumpBid(MBI::Bid* bid
        , int64_t partner_id
        , MBI::Bid::PartnerType partner_type
        , MBI::Bid::BidTarget bid_target
        , MBI::Bid::DomainType domain_type
        , const TString& domain_id0
        , const TString& domain_id1
        , const std::vector<int> &bids
        , bool deprecated_mode = false)
    {
        bid->set_partner_id(partner_id);
        bid->set_partner_type(partner_type);
        bid->set_target(bid_target);
        bid->set_domain_type(domain_type);

        if (domain_id1 == "") {
            if (deprecated_mode) {
                bid->set_domain_id(domain_id0);     //deprecated MARKETINDEXER-6229
            } else {
                bid->add_domain_ids(domain_id0);
            }
        } else {  // DomainType =  FEED_OFFER_ID / VENDOR_CATEGORY_ID
            if (deprecated_mode) { //deprecated MARKETINDEXER-6229
                bid->set_feed_id(::FromString<int64_t>(domain_id0));
                bid->set_domain_id(domain_id1);
            } else {
                bid->add_domain_ids(domain_id0);
                bid->add_domain_ids(domain_id1);
            }
        }
        DumpBidValues(bid, bids);
    }
public:
    TQBidCreateMetaTestBase() = default;
    virtual ~TQBidCreateMetaTestBase() = default;

    void DoSetUp()
    {
        create_test_environment(TMP_DIR());

        MBI::Parcel parcel;
        /* dump test data */
        {
            NMarket::TSnappyProtoWriter outputStream(TMP_DIR()+"/qbid_test.pbuf.sn",
                                                     "BIDS");
            InitTestData(parcel);
            outputStream.Write(parcel);
        }
    }

    virtual void SetUp()
    {
        DoSetUp();
    }

protected:
    virtual void InitTestData(MBI::Parcel& parcel) = 0;
};

//=======================================================================
class TValidOfferSnapshot : public TQBidCreateMetaTestBase
{
protected:
    void InitTestData(MBI::Parcel& parcel) override
    {
        DumpBid(parcel.add_bids(), 1, MBI::Bid::SHOP, MBI::Bid::OFFER, MBI::Bid::FEED_OFFER_ID, "100", "1000", { 10, 10, 10, 10, -1, -1 });
        DumpBid(parcel.add_bids(), 2, MBI::Bid::SHOP, MBI::Bid::OFFER, MBI::Bid::FEED_OFFER_ID, "100", "string_id", { 10, 10, 10, 10, -1, -1 });
        DumpBid(parcel.add_bids(), 3, MBI::Bid::SHOP, MBI::Bid::OFFER, MBI::Bid::OFFER_TITLE,   "title", "", { 20, 20, 20, 20, -1, -1 });
        DumpBid(parcel.add_bids(), 4, MBI::Bid::SHOP, MBI::Bid::OFFER, MBI::Bid::CATEGORY_ID,   "300", "", { 30, 30, 30, 30, -1, -1 });
        DumpBid(parcel.add_bids(), 5, MBI::Bid::SHOP, MBI::Bid::OFFER, MBI::Bid::SHOP_ID,       "400", "", { 40, 40, 40, 40, -1, -1 });

        Affect(parcel);
    }

    virtual void Affect(MBI::Parcel& /*parcel*/) {}
};

class TValidModelSnapshot : public TQBidCreateMetaTestBase
{
protected:
    void InitTestData(MBI::Parcel& parcel) override
    {
        DumpBid(parcel.add_bids(), 1, MBI::Bid::VENDOR, MBI::Bid::MODEL, MBI::Bid::MODEL_ID, "100", "", { -1, -1, -1, -1, 10, -1 });
        DumpBid(parcel.add_bids(), 2, MBI::Bid::VENDOR, MBI::Bid::MODEL, MBI::Bid::MODEL_ID, "200", "", { -1, -1, -1, -1, 10, -1 });
        // могут быть даже на одну модель. снапшот все равно валидный
        DumpBid(parcel.add_bids(), 3, MBI::Bid::VENDOR, MBI::Bid::MODEL, MBI::Bid::MODEL_ID, "200", "", { -1, -1, -1, -1, 10, -1 });

        DumpBid(parcel.add_bids(), 4, MBI::Bid::VENDOR, MBI::Bid::MODEL, MBI::Bid::VENDOR_ID, "3", "", { -1, -1, -1, -1, 10, -1 });
        DumpBid(parcel.add_bids(), 2, MBI::Bid::VENDOR, MBI::Bid::MODEL, MBI::Bid::VENDOR_ID, "4", "", { -1, -1, -1, -1, 10, -1 });

        //domain_ids(0) - vendor_id, domain_ids(1) - category_id
        DumpBid(parcel.add_bids(), 3, MBI::Bid::VENDOR, MBI::Bid::MODEL, MBI::Bid::VENDOR_CATEGORY_ID, "3", "30", { -1, -1, -1, -1, 10, -1 });

        Affect(parcel);
    }

    virtual void Affect(MBI::Parcel& /*parcel*/) {}
};

TEST_F(TValidOfferSnapshot, test_invalidBidTarget)
{
    // если передать невалидный агрумент на кого ставим, будет исключение
    ASSERT_THROW(RunMetaProcessor(TMP_DIR() + "/qbid_test.pbuf.sn", TMP_DIR() + "/qbid_test.meta", "wtf"), yexception);
}

TEST_F(TValidOfferSnapshot, test_validOfferSnapshot)
{
    // валидируем офферные ставки, создаем мета-файлик
    RunMetaProcessor(TMP_DIR() + "/qbid_test.pbuf.sn", TMP_DIR() + "/qbid_test.meta", "offer");

    // проверяем метафайлик
    TKeyTabNumber metaReader(TMP_DIR() + "/qbid_test.meta");
    ASSERT_EQ(metaReader("TOTAL"), 5);
    ASSERT_EQ(metaReader("ID"), 1);
    ASSERT_EQ(metaReader("DIGITID"), 1);
    ASSERT_EQ(metaReader("TITLE"), 1);
    ASSERT_EQ(metaReader("CATEGORY"), 1);
    ASSERT_EQ(metaReader("SHOP"), 1);
    ASSERT_EQ(metaReader("MODELID"), 0);
    ASSERT_EQ(metaReader("TITLE_CSTRINGS"), strlen("title") + 1 /*'\0'*/ );
    ASSERT_EQ(metaReader("ID_CSTRING"), strlen("string_id") + 1 /*'\0'*/ );

    // Имеем 5 ставок, в каждой по 4 значения ставки, одна из ставок - по OFFER_TITLE
    ASSERT_EQ(metaReader("BIDS_MEMORY"), 5 * (sizeof(TMeta) + 4 * sizeof(TBid::TBidValueStatus)) +
        sizeof(TFeedId) + sizeof(TOfferDigitId) /*см. AdditionalTitleSize для OFFER_TITLE ставки*/);

    // в качестве ставок на модель наши ставки валидацию не проходят
    ASSERT_THROW(RunMetaProcessor(TMP_DIR() + "/qbid_test.pbuf.sn", TMP_DIR() + "/qbid_test.meta", "model"), TInvalidInputMBIDataException);
}

TEST_F(TValidModelSnapshot, test_validModelSnapshot)
{
    // валидируем модельные ставки, создаем мета-файлик
    RunMetaProcessor(TMP_DIR() + "/qbid_test.pbuf.sn", TMP_DIR() + "/qbid_test.meta", "model");

    // проверяем метафайлик
    TKeyTabNumber metaReader(TMP_DIR() + "/qbid_test.meta");
    ASSERT_EQ(metaReader("TOTAL"), 6);
    ASSERT_EQ(metaReader("ID"), 0);
    ASSERT_EQ(metaReader("DIGITID"), 0);
    ASSERT_EQ(metaReader("TITLE"), 0);
    ASSERT_EQ(metaReader("CATEGORY"), 0);
    ASSERT_EQ(metaReader("SHOP"), 0);
    ASSERT_EQ(metaReader("MODELID"), 3);
    ASSERT_EQ(metaReader("VENDORID"), 2);
    ASSERT_EQ(metaReader("VENDOR_CATEGORYID"), 1);

    // в качестве ставок на оффер наши ставки валидацию не проходят
    ASSERT_THROW(RunMetaProcessor(TMP_DIR() + "/qbid_test.pbuf.sn", TMP_DIR() + "/qbid_test.meta", "offer"), TInvalidInputMBIDataException);
}

//===================================================================================
// Различные случаи, когда валидация не проходит из-за плохих входных данных

class TOfferSnapshot_InvalidParentType : public TValidOfferSnapshot {
    void Affect(MBI::Parcel& parcel) override
    {
        parcel.mutable_bids(3)->set_partner_type(MBI::Bid::VENDOR);
    }
};

class TModelSnapshot_InvalidBidTarget : public TValidModelSnapshot {
    void Affect(MBI::Parcel& parcel) override
    {
        parcel.mutable_bids(1)->set_target(MBI::Bid::OFFER);
    }
};

class TOfferSnapshot_InvalidDomainId : public TValidOfferSnapshot {
    void Affect(MBI::Parcel& parcel) override
    {
        parcel.mutable_bids(4)->set_domain_type(MBI::Bid::MODEL_ID);
    }
};

class TModelSnapshot_InvalidDomainId : public TValidModelSnapshot {
    void Affect(MBI::Parcel& parcel) override
    {
        parcel.mutable_bids(1)->set_domain_type(MBI::Bid::OFFER_TITLE);
    }
};

class TOfferSnapshot_EmptyValues : public TValidOfferSnapshot {
    void Affect(MBI::Parcel& parcel) override
    {
        parcel.mutable_bids(3)->clear_value_for_search();
        parcel.mutable_bids(3)->clear_value_for_card();
        parcel.mutable_bids(3)->clear_value_for_marketplace();
        parcel.mutable_bids(3)->clear_value_for_market_search_only();
        parcel.mutable_bids(3)->clear_flag_dont_pull_up_bids();
    }
};

class TOfferSnapshot_NegativeValue : public TValidOfferSnapshot {
    void Affect(MBI::Parcel& parcel) override
    {
        parcel.mutable_bids(3)->mutable_value_for_card()->set_value(-300);
    }
};

class TOfferSnapshot_OverflowValue : public TValidOfferSnapshot {
    void Affect(MBI::Parcel& parcel) override
    {
        parcel.mutable_bids(3)->mutable_value_for_card()->set_value(static_cast<int32_t>(Max<TBidValue>()) + 1);
    }
};

class TOfferSnapshot_ModelSearchValue : public TValidOfferSnapshot {
    void Affect(MBI::Parcel& parcel) override
    {
        parcel.mutable_bids(3)->mutable_value_for_model_search()->set_value(300);
    }
};

class TModelSnapshot_EmptyValue : public TValidModelSnapshot {
    void Affect(MBI::Parcel& parcel) override
    {
        parcel.mutable_bids(1)->clear_value_for_model_search();
    }
};

class TModelSnapshot_SomeOfferValue : public TValidModelSnapshot {
    void Affect(MBI::Parcel& parcel) override
    {
        parcel.mutable_bids(1)->mutable_value_for_search()->set_value(30);
    }
};

class TOfferSnapshot_InvalidPartnerId : public TValidOfferSnapshot {
    void Affect(MBI::Parcel& parcel) override
    {
        parcel.mutable_bids(3)->set_partner_id(static_cast<int64_t>(Max<TShopId>()) + 1);
    }
};

class TModelSnapshot_InvalidPartnerId : public TValidModelSnapshot {
    void Affect(MBI::Parcel& parcel) override
    {
        parcel.mutable_bids(1)->set_partner_id(0);
    }
};

class TOfferSnapshot_InvalidCategoryId : public TValidOfferSnapshot {
    void Affect(MBI::Parcel& parcel) override
    {
        // это ставка с DomainType = CATEGORY_ID
        parcel.mutable_bids(3)->set_domain_ids(0, "string");
    }
};

class TOfferSnapshot_InvalidOfferId : public TValidOfferSnapshot {
    void Affect(MBI::Parcel& parcel) override
    {
        // это ставка с DomainType = FEED_OFFER_ID
        const TString tooLongId(81, '1');
        parcel.mutable_bids(0)->set_domain_ids(1, tooLongId);
    }
};

class TModelSnapshot_InvalidModelId : public TValidModelSnapshot {
    void Affect(MBI::Parcel& parcel) override
    {
        parcel.mutable_bids(1)->clear_domain_ids();
    }
};

class TOfferSnapshot_InvalidDomainId2 : public TValidOfferSnapshot {
    void Affect(MBI::Parcel& parcel) override
    {
        parcel.mutable_bids(4)->set_domain_type(MBI::Bid::VENDOR_ID);
    }
};

class TModelSnapshot_InvalidVendorId : public TValidModelSnapshot {
    void Affect(MBI::Parcel& parcel) override
    {
        // это ставка по VENDOR_ID
        // 0 - плохой vendor_id (и IsDigit для него false, см. qbidvalidator.cpp::ValidateVendorId)
        parcel.mutable_bids(3)->set_domain_ids(0, "0");
    }
};

class TModelSnapshot_InvalidCategoryId : public TValidModelSnapshot {
    void Affect(MBI::Parcel& parcel) override
    {
        // это ставка по VENDOR_CATEGORY_ID, в domain_ids лежит category_id
        parcel.mutable_bids(5)->set_domain_ids(1, "0");
    }
};

class TOfferSnapshot_InvalidBidValueZero : public TValidOfferSnapshot {
    void Affect(MBI::Parcel& parcel) override
    {
        //для офферных ставок значение ставки 0 не валидно
        parcel.mutable_bids(4)->mutable_value_for_search()->set_value(0);
    }
};

class TOfferSnapshot_InvalidFlagDontPullUpBidsZero : public TValidOfferSnapshot {
    void Affect(MBI::Parcel& parcel) override
    {
        //для флага flag_dont_pull_up_bids значение 0 тоже не валидно (false это отсутствие флага)
        parcel.mutable_bids(4)->mutable_flag_dont_pull_up_bids()->set_value(0);
    }
};

class TModelSnapshot_ValidBidValueZero : public TValidModelSnapshot {
    void Affect(MBI::Parcel& parcel) override
    {
        //а для модельных - вполне
        parcel.mutable_bids(4)->mutable_value_for_model_search()->set_value(0);
    }
};

class TModelSnapshot_InvalidBidValue : public TValidModelSnapshot {
    void Affect(MBI::Parcel& parcel) override
    {
        // а -1 для модельных - уже не валид
        parcel.mutable_bids(4)->mutable_value_for_model_search()->set_value(-1);
    }
};

class TDeprecatedButStillValidOfferSnapshot : public TValidOfferSnapshot
{
protected:
    void InitTestData(MBI::Parcel& parcel) override
    {
        DumpBid(parcel.add_bids(), 1, MBI::Bid::SHOP, MBI::Bid::OFFER, MBI::Bid::FEED_OFFER_ID, "100", "1000", { 10, 10, 10, 10, -1, -1 }, true);
        DumpBid(parcel.add_bids(), 2, MBI::Bid::SHOP, MBI::Bid::OFFER, MBI::Bid::FEED_OFFER_ID, "100", "string_id", { 10, 10, 10, 10, -1, -1 }, true);
        DumpBid(parcel.add_bids(), 3, MBI::Bid::SHOP, MBI::Bid::OFFER, MBI::Bid::OFFER_TITLE, "title", "", { 20, 20, 20, 20, -1, -1 }, true);
        DumpBid(parcel.add_bids(), 4, MBI::Bid::SHOP, MBI::Bid::OFFER, MBI::Bid::CATEGORY_ID, "300", "", { 30, 30, 30, 30, -1, -1 }, true);
        DumpBid(parcel.add_bids(), 5, MBI::Bid::SHOP, MBI::Bid::OFFER, MBI::Bid::SHOP_ID, "400", "", { 40, 40, 40, 40, -1, -1 }, true);

        Affect(parcel);
    }

    void Affect(MBI::Parcel& /*parcel*/) override {}
};

class TOfferSnapshot_InvalidNoFeedId : public TDeprecatedButStillValidOfferSnapshot
{
protected:
    void Affect(MBI::Parcel& parcel) override
    {
        // нет feed_id ни в deprecated, ни в новом поле
        parcel.mutable_bids(1)->clear_feed_id();
    }
};

class TOfferSnapshot_InvalidNoOfferId : public TDeprecatedButStillValidOfferSnapshot
{
protected:
    void Affect(MBI::Parcel& parcel) override
    {
        // нет offer_id ни в deprecated, ни в новом поле
        parcel.mutable_bids(1)->clear_domain_id();
    }
};


//========================================================================
#define CHECK_BIDTARGET(CLASS, BIDTARGET)                                                                                                            \
        /* проверим, что случайно не перепутали BIDTARGET*/                                                                                          \
        if (strcmp(BIDTARGET, "offer") == 0) {                                                                                                       \
            ASSERT_TRUE(static_cast<bool>(std::is_base_of<TValidOfferSnapshot, CLASS>::value));                                                      \
        } else {                                                                                                                                     \
            ASSERT_TRUE(static_cast<bool>(std::is_base_of<TValidModelSnapshot, CLASS>::value));                                                      \
        }                                                                                                                                            \

#define ASSERT_INVALID_MBI_DATA(CLASS, TESTNAME, BIDTARGET)                                                                                          \
    TEST_F(CLASS, TESTNAME)                                                                                                                          \
    {                                                                                                                                                \
        CHECK_BIDTARGET(CLASS, BIDTARGET)                                                                                                            \
        ASSERT_THROW(RunMetaProcessor(TMP_DIR() + "/qbid_test.pbuf.sn", TMP_DIR() + "/qbid_test.meta", BIDTARGET), TInvalidInputMBIDataException);   \
    }                                                                                                                                                \

#define ASSERT_MBI_DATA_IS_OK(CLASS, TESTNAME, BIDTARGET)                                                                                            \
    TEST_F(CLASS, TESTNAME)                                                                                                                          \
    {                                                                                                                                                \
        CHECK_BIDTARGET(CLASS, BIDTARGET)                                                                                                            \
        ASSERT_NO_THROW(RunMetaProcessor(TMP_DIR() + "/qbid_test.pbuf.sn", TMP_DIR() + "/qbid_test.meta", BIDTARGET));                               \
    }                                                                                                                                                \


ASSERT_INVALID_MBI_DATA(TOfferSnapshot_InvalidParentType, test_invalidParentType, "offer")
ASSERT_INVALID_MBI_DATA(TModelSnapshot_InvalidBidTarget, test_invalidBidTarget, "model")
ASSERT_INVALID_MBI_DATA(TOfferSnapshot_InvalidDomainId, test_invalidParentType, "offer")
ASSERT_INVALID_MBI_DATA(TModelSnapshot_InvalidDomainId, test_invalidParentTypeModel, "model")
ASSERT_INVALID_MBI_DATA(TOfferSnapshot_EmptyValues, test_emptyValues, "offer")
ASSERT_INVALID_MBI_DATA(TOfferSnapshot_ModelSearchValue, test_modelSearchValue, "offer")
ASSERT_INVALID_MBI_DATA(TOfferSnapshot_NegativeValue, test_negativeValue, "offer")
ASSERT_INVALID_MBI_DATA(TOfferSnapshot_OverflowValue, test_overflowValue, "offer")
ASSERT_INVALID_MBI_DATA(TModelSnapshot_EmptyValue, test_emptyValueModel, "model")
ASSERT_INVALID_MBI_DATA(TModelSnapshot_SomeOfferValue, test_someOfferValueModel, "model")
ASSERT_INVALID_MBI_DATA(TOfferSnapshot_InvalidPartnerId, test_invalidPartnerId, "offer")
ASSERT_INVALID_MBI_DATA(TModelSnapshot_InvalidPartnerId, test_invalidPartnerIdModel, "model")
ASSERT_INVALID_MBI_DATA(TOfferSnapshot_InvalidPartnerId, test_invalidCategoryId, "offer")
ASSERT_INVALID_MBI_DATA(TOfferSnapshot_InvalidOfferId, test_invalidOfferId, "offer")
ASSERT_INVALID_MBI_DATA(TModelSnapshot_InvalidModelId, test_invalidModelId, "model")
ASSERT_INVALID_MBI_DATA(TOfferSnapshot_InvalidDomainId2, test_invalidDomainId2, "offer")
ASSERT_INVALID_MBI_DATA(TModelSnapshot_InvalidVendorId, test_invalidVendorId, "model")
ASSERT_INVALID_MBI_DATA(TModelSnapshot_InvalidCategoryId, test_invalidCategoryIdModel, "model")
ASSERT_INVALID_MBI_DATA(TOfferSnapshot_InvalidBidValueZero, test_invalidBidValueOffer, "offer")
ASSERT_INVALID_MBI_DATA(TOfferSnapshot_InvalidFlagDontPullUpBidsZero, test_invalidFlagDontPullUpBidsZeroOffer, "offer")
ASSERT_INVALID_MBI_DATA(TModelSnapshot_InvalidBidValue, test_invalidBidValueModel, "model")


ASSERT_MBI_DATA_IS_OK(TModelSnapshot_ValidBidValueZero, test_validBidValueZeroModel, "model")

// deprecated test
ASSERT_MBI_DATA_IS_OK(TDeprecatedButStillValidOfferSnapshot, test_deprecatedValidOfferSnapshot, "offer")
ASSERT_INVALID_MBI_DATA(TOfferSnapshot_InvalidNoFeedId, test_invalidNoFeedIdOffer, "offer")
ASSERT_INVALID_MBI_DATA(TOfferSnapshot_InvalidNoOfferId, test_invalidNoOfferId, "offer")

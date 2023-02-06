#include <market/idx/generation/genlog_dumper/dumpers/PromoDumper.h>

#include <market/library/libpromo/reader.h>

#include <library/cpp/testing/unittest/gtest.h>

#include <util/folder/tempdir.h>

namespace {
    struct TOfferWithPromo {
        NMarket::NPromo::TOfferLocalID OfferID;
        NMarket::NPromo::TOfferPromo Promo;

        std::weak_ordering operator<=>(const TOfferWithPromo& other) const {
            return OfferID <=> other.OfferID;
        }
    };

    struct TOfferPromoSaver {
        TOfferPromoSaver(TVector<TOfferWithPromo>& offers)
            : Offers(offers) {}

        bool operator()(NMarket::NPromo::TOfferLocalID offerID,
                        const NMarket::NPromo::TOfferPromoUnsafe& promo) const {

            Offers.emplace_back(
                TOfferWithPromo{
                    .OfferID=offerID,
                    .Promo=NMarket::NPromo::TOfferPromo{
                        .PromoKeys=TVector<TString>(promo.PromoKeys.begin(), promo.PromoKeys.end()),
                        .PromoType=promo.PromoType,
                        .PromoPrice=promo.PromoPrice,
                        .BenefitPrice=promo.BenefitPrice,
                    }
                });
            return false;
        }
    public:
        TVector<TOfferWithPromo>& Offers;
    };

    TVector<TOfferWithPromo> DumpRecords(const TVector<MarketIndexer::GenerationLog::Record>& records) {
        TTempDir dir;
        NDumpers::TDumperContext context(dir.Name(), false);
        auto dumper = NDumpers::MakePromoDumper(context);
        for (size_t i = 0; i < records.size(); ++i) {
            dumper->ProcessGenlogRecord(records[i], i);
        }
        dumper->Finish();
        auto reader = NMarket::NPromo::CreateOfferPromoInfoReader(
            Market::NMmap::IMemoryRegion::MmapFile(dir.Path() / "offer_promo.mmap"));
        TVector<TOfferWithPromo> result;
        TOfferPromoSaver saver(result);
        reader->EnumerateAllOffers(saver);
        Sort(result);
        return result;
    }

    // N.B. We can set only fields from EXPECTED_GENLOG_FIELDS
    void AddRecord(TVector<MarketIndexer::GenerationLog::Record>& records,
                  const TVector<TString>& binaryPromosMD5,
                  const NMarket::NPromo::TPromoType promoType,
                  const ui64 benefitPrice,
                  const ui64 promoPrice) {
        records.emplace_back();
        MarketIndexer::GenerationLog::Record& record = records.back();
        record.mutable_binary_promos_md5_base64()->Add(binaryPromosMD5.begin(), binaryPromosMD5.end());
        record.set_promo_type(promoType);
        record.set_benefit_price(benefitPrice);
        record.set_promo_price(promoPrice);
    }
}

TEST(PromoDumper, Empty) {
    auto result = DumpRecords({});
    ASSERT_EQ(result.size(), 0);
}

TEST(PromoDumper, WithoutPromoMD5Record) {
    TVector<MarketIndexer::GenerationLog::Record> records;
    AddRecord(records, {}, NMarket::NPromo::EPromoType::NPlusM, 200, 300);

    auto result = DumpRecords(records);
    ASSERT_EQ(result.size(), 0);
}

TEST(PromoDumper, WithPromoMD5Record) {
    TVector<MarketIndexer::GenerationLog::Record> records;
    AddRecord(
        records,
        {"some_promo_md5"},
        NMarket::NPromo::EPromoType::GiftWithPurchase,
        400,
        500);

    auto result = DumpRecords(records);
    ASSERT_EQ(result.size(), 1);

    {
        TOfferWithPromo data = result[0];
        ASSERT_EQ(data.OfferID, 0);

        const auto& promo = data.Promo;
        ASSERT_EQ(promo.BenefitPrice, 400);
        ASSERT_EQ(promo.PromoPrice, 500);

        ASSERT_EQ(promo.PromoKeys.size(), 1);
        ASSERT_STREQ(promo.PromoKeys[0], "some_promo_md5");
    }
}

TEST(PromoDumper, WithCouplePromoMD5Record) {
    TVector<MarketIndexer::GenerationLog::Record> records;
    AddRecord(
        records,
        {"some_promo_md5", "another_promo_md5"},
        NMarket::NPromo::EPromoType::ServiceWithPurchase,
        600,
        700);

    auto result = DumpRecords(records);
    ASSERT_EQ(result.size(), 1);

    {
        TOfferWithPromo data = result[0];
        ASSERT_EQ(data.OfferID, 0);

        const auto& promo = data.Promo;
        ASSERT_EQ(promo.BenefitPrice, 600);
        ASSERT_EQ(promo.PromoPrice, 700);

        ASSERT_EQ(promo.PromoKeys.size(), 2);
        ASSERT_STREQ(promo.PromoKeys[0], "some_promo_md5");
        ASSERT_STREQ(promo.PromoKeys[1], "another_promo_md5");
    }
}

TEST(PromoDumper, SeveralRecords) {
    TVector<MarketIndexer::GenerationLog::Record> records;
    AddRecord(
        records,
        {},
        NMarket::NPromo::EPromoType::NPlusM,
        200,
        300);
    AddRecord(
        records,
        {"some_promo_md5"},
        NMarket::NPromo::EPromoType::GiftWithPurchase,
        400,
        500);
    AddRecord(
        records,
        {"some_promo_md5", "another_promo_md5"},
        NMarket::NPromo::EPromoType::ServiceWithPurchase,
        600,
        700);

    auto result = DumpRecords(records);
    ASSERT_EQ(result.size(), 2);

    {
        TOfferWithPromo data = result[0];
        ASSERT_EQ(data.OfferID, 1);

        const auto& promo = data.Promo;
        ASSERT_EQ(promo.BenefitPrice, 400);
        ASSERT_EQ(promo.PromoPrice, 500);

        ASSERT_EQ(promo.PromoKeys.size(), 1);
        ASSERT_STREQ(promo.PromoKeys[0], "some_promo_md5");
    }

    {
        TOfferWithPromo data = result[1];
        ASSERT_EQ(data.OfferID, 2);

        const auto& promo = data.Promo;
        ASSERT_EQ(promo.BenefitPrice, 600);
        ASSERT_EQ(promo.PromoPrice, 700);

        ASSERT_EQ(promo.PromoKeys.size(), 2);
        ASSERT_STREQ(promo.PromoKeys[0], "some_promo_md5");
        ASSERT_STREQ(promo.PromoKeys[1
        ], "another_promo_md5");
    }
}

#include <market/idx/offers/lib/loaders/load_biz_logic.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/env.h>

#include <market/proto/indexer/GenerationLog.pb.h>
#include <market/library/taxes/taxes.h>
#include <market/library/libpromo/common.h>

#include <util/generic/ptr.h>
#include <util/generic/vector.h>
#include <util/generic/string.h>
#include <util/string/cast.h>


const static TString RU = "ru";
TEST(TestLoader, LANG) {
    TOfferCtx offerEmptyContext;
    NMarket::NIdx::TPictureList pictures;

    {
        MarketIndexer::GenerationLog::Record record;
        NLoaders::FillOfferFieldsManually(record, offerEmptyContext, nullptr, nullptr, pictures);

        ASSERT_EQ(record.lang(), RU);
    }

    {
        MarketIndexer::GenerationLog::Record record;
        record.set_lang(RU);
        NLoaders::FillOfferFieldsManually(record, offerEmptyContext, nullptr, nullptr, pictures);

        ASSERT_EQ(record.lang(), RU);
    }

    {
        MarketIndexer::GenerationLog::Record record;
        record.set_lang("en");
        NLoaders::FillOfferFieldsManually(record, offerEmptyContext, nullptr, nullptr, pictures);

        ASSERT_EQ(record.lang(), RU);
    }
}


TEST(TestLoader, DIMENSIONS) {
    TOfferCtx offerEmptyContext;
    NMarket::NIdx::TPictureList pictures;

    {
        // минимальное значение после 0
        MarketIndexer::GenerationLog::Record record;
        record.set_length(10.001);
        record.set_width(20.001);
        record.set_height(30.001);
        NLoaders::FillOfferFieldsManually(record, offerEmptyContext, nullptr, nullptr, pictures);

        ASSERT_EQ(record.dimensions(), "10.001/20.001/30.001");
    }

    {
        MarketIndexer::GenerationLog::Record record;
        record.set_length(100.0);
        NLoaders::FillOfferFieldsManually(record, offerEmptyContext, nullptr, nullptr, pictures);

        ASSERT_FALSE(record.has_dimensions());
    }

    {
        MarketIndexer::GenerationLog::Record record;
        record.set_width(200.0);
        NLoaders::FillOfferFieldsManually(record, offerEmptyContext, nullptr, nullptr, pictures);

        ASSERT_FALSE(record.has_dimensions());
    }

    {
        MarketIndexer::GenerationLog::Record record;
        record.set_height(300.0);
        NLoaders::FillOfferFieldsManually(record, offerEmptyContext, nullptr, nullptr, pictures);

        ASSERT_FALSE(record.has_dimensions());
    }

    {
        MarketIndexer::GenerationLog::Record record;
        NLoaders::FillOfferFieldsManually(record, offerEmptyContext, nullptr, nullptr, pictures);

        ASSERT_FALSE(record.has_dimensions());
    }

    // тесты на правильное округление
    {
        MarketIndexer::GenerationLog::Record record;
        record.set_length(10);
        record.set_width(20);
        record.set_height(30);
        NLoaders::FillOfferFieldsManually(record, offerEmptyContext, nullptr, nullptr, pictures);

        ASSERT_EQ(record.dimensions(), "10/20/30");
    }

    {
        // значение больше трех знаков после запятой, округление вниз по 4му числу
        MarketIndexer::GenerationLog::Record record;
        record.set_length(10.123456789);
        record.set_width(20.123456789);
        record.set_height(30.123456789);
        NLoaders::FillOfferFieldsManually(record, offerEmptyContext, nullptr, nullptr, pictures);

        ASSERT_EQ(record.dimensions(), "10.123/20.123/30.123");
    }

    {
        // значение больше трех знаков после запятой, округление вверх по 4му числу
        MarketIndexer::GenerationLog::Record record;
        record.set_length(10.123556789);
        record.set_width(20.123556789);
        record.set_height(30.123556789);
        NLoaders::FillOfferFieldsManually(record, offerEmptyContext, nullptr, nullptr, pictures);

        ASSERT_EQ(record.dimensions(), "10.124/20.124/30.124");
    }

    {
        // если не выходим за 3 знака, то округления до целого не будет
        MarketIndexer::GenerationLog::Record record;
        record.set_length(10.999);
        record.set_width(20.999);
        record.set_height(30.999);
        NLoaders::FillOfferFieldsManually(record, offerEmptyContext, nullptr, nullptr, pictures);

        ASSERT_EQ(record.dimensions(), "10.999/20.999/30.999");
    }

    {
        // если выходим за 3 знака, то округление будет математически до целого
        MarketIndexer::GenerationLog::Record record;
        record.set_length(10.9999);
        record.set_width(20.9999);
        record.set_height(30.9999);
        NLoaders::FillOfferFieldsManually(record, offerEmptyContext, nullptr, nullptr, pictures);

        ASSERT_EQ(record.dimensions(), "11/21/31");
    }
}

TEST(TestLoader, WEIGHT) {
    TOfferCtx offerEmptyContext;
    NMarket::NIdx::TPictureList pictures;

    {
        // минимальное значение после 0
        MarketIndexer::GenerationLog::Record record;
        record.set_weight(1500);
        NLoaders::FillOfferFieldsManually(record, offerEmptyContext, nullptr, nullptr, pictures);

        ASSERT_EQ(record.snippet_weight(), "1500");
    }

    {
        // минимальное значение после 0
        MarketIndexer::GenerationLog::Record record;
        record.set_weight(0.001);
        NLoaders::FillOfferFieldsManually(record, offerEmptyContext, nullptr, nullptr, pictures);

        ASSERT_EQ(record.snippet_weight(), "0.001");
    }

    {
        MarketIndexer::GenerationLog::Record record;
        NLoaders::FillOfferFieldsManually(record, offerEmptyContext, nullptr, nullptr, pictures);

        ASSERT_FALSE(record.has_snippet_weight());
    }

    // тесты на правильное округление
    {
        // значение больше трех знаков после запятой, округление вниз по 4му числу
        MarketIndexer::GenerationLog::Record record;
        record.set_weight(1500.123456789);
        NLoaders::FillOfferFieldsManually(record, offerEmptyContext, nullptr, nullptr, pictures);

        ASSERT_EQ(record.snippet_weight(), "1500.123");
    }

    {
        // значение больше трех знаков после запятой, округление вниз по 4му числу
        MarketIndexer::GenerationLog::Record record;
        record.set_weight(1500.123556789);
        NLoaders::FillOfferFieldsManually(record, offerEmptyContext, nullptr, nullptr, pictures);

        ASSERT_EQ(record.snippet_weight(), "1500.124");
    }

    {
        // если не выходим за 3 знака, то округления до целого не будет
        MarketIndexer::GenerationLog::Record record;
        record.set_weight(1500.999);
        NLoaders::FillOfferFieldsManually(record, offerEmptyContext, nullptr, nullptr, pictures);

        ASSERT_EQ(record.snippet_weight(), "1500.999");
    }

    {
        // если выходим за 3 знака, то округление будет математически до целого
        MarketIndexer::GenerationLog::Record record;
        record.set_weight(1500.9999);
        NLoaders::FillOfferFieldsManually(record, offerEmptyContext, nullptr, nullptr, pictures);

        ASSERT_EQ(record.snippet_weight(), "1501");
    }
}

TEST(TestLoader, VAT) {
    TOfferCtx offerEmptyContext;
    NMarket::NIdx::TPictureList pictures;


    for (unsigned int vat = 1; vat < 9; vat++) {
        MarketIndexer::GenerationLog::Record recordValidVat;
        recordValidVat.set_vat(vat);
        NLoaders::FillOfferFieldsManually(recordValidVat, offerEmptyContext, nullptr, nullptr, pictures);

        auto expectedVat = NMarket::NTaxes::GetCorrectVat(static_cast<NMarket::NTaxes::EVat>(vat));
        ASSERT_EQ(static_cast<NMarket::NTaxes::EVat>(recordValidVat.vat()), expectedVat);
    }

    {
        MarketIndexer::GenerationLog::Record record;
        record.set_vat(0);
        NLoaders::FillOfferFieldsManually(record, offerEmptyContext, nullptr, nullptr, pictures);

        ASSERT_FALSE(record.has_vat());
    }

    {
        MarketIndexer::GenerationLog::Record record;
        record.set_vat(37);

        NLoaders::FillOfferFieldsManually(record, offerEmptyContext, nullptr, nullptr, pictures);
        ASSERT_FALSE(record.has_vat());
    }
}

TEST(TestLoader, MBO_MODEL_FAKE_MSKU_OFFER) {
    TString mboModel = "It Is Wednesday My Dudes!!";
    TOfferCtx offerEmptyContext;
    NMarket::NIdx::TPictureList pictures;

    MarketIndexer::GenerationLog::Record recordWithMbo;
    recordWithMbo.set_mbo_model(mboModel);
    MarketIndexer::GenerationLog::Record recordWithMboFakeMsku;
    recordWithMboFakeMsku.set_is_fake_msku_offer(true);
    recordWithMboFakeMsku.set_mbo_model(mboModel);
    ASSERT_EQ(recordWithMbo.mbo_model(), mboModel);

    NLoaders::FillOfferFieldsManually(recordWithMbo, offerEmptyContext, nullptr, nullptr, pictures);
    NLoaders::FillOfferFieldsManually(recordWithMboFakeMsku, offerEmptyContext, nullptr, nullptr, pictures);

    ASSERT_FALSE(recordWithMbo.has_mbo_model());
    ASSERT_EQ(recordWithMboFakeMsku.mbo_model(), mboModel);
}

TEST(TestLoader, PICTURES_FLAGS) {
    TOfferCtx offerEmptyContext;

    {
        MarketIndexer::GenerationLog::Record record;
        NMarket::NIdx::TPictureList pictures;
        NLoaders::FillOfferFieldsManually(record, offerEmptyContext, nullptr, nullptr, pictures);

        ASSERT_FALSE(record.has_picture_flags());
    }

    {
        MarketIndexer::GenerationLog::Record record;
        NMarket::NIdx::TPictureList pictures;
        NMarket::NIdx::TPicture picture;
        picture.set_crc("http://ya.ru");
        pictures.emplace_back(picture);
        NLoaders::FillOfferFieldsManually(record, offerEmptyContext, nullptr, nullptr, pictures);

        ASSERT_FALSE(record.has_picture_flags());
    }

    {
        MarketIndexer::GenerationLog::Record record;
        NMarket::NIdx::TPictureList pictures;
        NMarket::NIdx::TPicture picture;
        picture.set_crc("http://shock_content.jpg");
        picture.set_dups_id(10);
        pictures.emplace_back(picture);
        NLoaders::FillOfferFieldsManually(record, offerEmptyContext, nullptr, nullptr, pictures);

        ASSERT_TRUE(record.has_picture_flags());
        ASSERT_EQ(record.picture_flags(), "10");
    }

    {
        MarketIndexer::GenerationLog::Record record;
        NMarket::NIdx::TPictureList pictures;
        {
            NMarket::NIdx::TPicture picture;
            picture.set_crc("http://market.jpg");
            picture.set_dups_id(100);
            pictures.emplace_back(picture);
        }
        {
            NMarket::NIdx::TPicture picture;
            picture.set_crc("https://beru.png");
            picture.set_dups_id(200);
            pictures.emplace_back(picture);
        }
        NLoaders::FillOfferFieldsManually(record, offerEmptyContext, nullptr, nullptr, pictures);

        ASSERT_TRUE(record.has_picture_flags());
        ASSERT_EQ(record.picture_flags(), "100|200");
}
}


TEST(TestLoader, BARCODE) {
    TOfferCtx offerEmptyContext;
    NMarket::NIdx::TPictureList pictures;

    {
        MarketIndexer::GenerationLog::Record record;
        record.set_barcode("a|a|b|b|c");
        NLoaders::FillOfferFieldsManually(record, offerEmptyContext, nullptr, nullptr, pictures);

        ASSERT_EQ(record.barcode(), "a|b|c");
    }
}

TEST(TestLoader, PROMO) {
    TOfferCtx offerEmptyContext;
    NMarket::NIdx::TPictureList pictures;

// SecretSale, NPlusM , DirectDiscount, FlashDiscount
    {
        MarketIndexer::GenerationLog::Record record;

        auto promoType = (NMarket::NPromo::TPromoType{} | NMarket::NPromo::EPromoType::NPlusM | NMarket::NPromo::EPromoType::BlueCashback);
        Market::Promo::OfferPromo promo;
        promo.set_promo_type(promoType);
        record.mutable_price_history()->set_price_expression("RUR 2000000000");
        record.mutable_price_history()->set_is_valid(false); // false, but it should set history price

//        auto row_history_price = static_cast<uint64_t>(200);
//        const auto history_price = TFixedPointNumber::CreateFromRawValue(TFixedPointNumber::DefaultPrecision, row_price);
//        record.mutable_binary_history_price()->set_price(price.AsRaw());
//        record.set_history_price_is_valid(false);
        NLoaders::FillOfferFieldsManually(record, offerEmptyContext, nullptr, &promo, pictures);


        ASSERT_EQ(record.binary_history_price().price(), 2000000000);
    }
    {
        MarketIndexer::GenerationLog::Record record;

        auto promoType = (NMarket::NPromo::TPromoType{} | NMarket::NPromo::EPromoType::FlashDiscount);
        Market::Promo::OfferPromo promo;
        promo.set_promo_type(promoType);
        record.mutable_price_history()->set_price_expression("RUR 2000000000");
        record.mutable_price_history()->set_is_valid(false);


        NLoaders::FillOfferFieldsManually(record, offerEmptyContext, nullptr, &promo, pictures);

        ASSERT_FALSE(record.has_binary_history_price());
    }

    {
        MarketIndexer::GenerationLog::Record record;

        auto promoType = (NMarket::NPromo::TPromoType{} | NMarket::NPromo::EPromoType::FlashDiscount | NMarket::NPromo::EPromoType::BlueCashback);
        Market::Promo::OfferPromo promo;
        promo.set_promo_type(promoType);
        record.mutable_price_history()->set_price_expression("RUR 2000000000");
        record.mutable_price_history()->set_is_valid(false); // false, but it should set history price


        NLoaders::FillOfferFieldsManually(record, offerEmptyContext, nullptr, &promo, pictures);
        ASSERT_FALSE(record.has_binary_history_price());
    }

    {
        MarketIndexer::GenerationLog::Record record;

        auto promoType = (NMarket::NPromo::TPromoType{} | NMarket::NPromo::EPromoType::FlashDiscount | NMarket::NPromo::EPromoType::NPlusM);
        Market::Promo::OfferPromo promo;
        promo.set_promo_type(promoType);
        record.mutable_price_history()->set_price_expression("RUR 2000000000");
        record.mutable_price_history()->set_is_valid(false); // false, but it should set history price


        NLoaders::FillOfferFieldsManually(record, offerEmptyContext, nullptr, &promo, pictures);

        ASSERT_EQ(record.binary_history_price().price(), 2000000000);
    }
}
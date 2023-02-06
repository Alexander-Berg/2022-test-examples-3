#include <market/library/offers_common/types.h>
#include <market/idx/offers/lib/iworkers/OfferCtx.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/env.h>

#include <market/idx/offers/lib/iworkers/OfferPricesCalculator.h>
#include <market/library/currency_exchange/currency_exchange.h>
#include <market/idx/offers/lib/loaders/load_biz_logic.h>


using namespace Market::NCurrency;

class TOfferPricesCalculatorTest : public ::testing::Test {
protected:
    virtual void SetUp() {
        TString currencyRatesFile(SRC_("data/new_currency_rates.xml").c_str());
        CEXCHANGE.Load(currencyRatesFile);
    }
};

static void CalcFeedInfoMap(THashMap<unsigned, TFeedInfo>& feedInfoMap) {
    for (const auto& feed: Feeds::Instance()) {
        feedInfoMap.emplace(std::pair<int, TFeedInfo>(feed.first, TFeedInfo(&feed.second)));
    }
}

TEST_F(TOfferPricesCalculatorTest, DeliveryCurrency)
{
    TFeedLog feedlog;
    THashMap<unsigned, TFeedInfo> feedInfoMap;
    CalcFeedInfoMap(feedInfoMap);
    TOfferPricesCalculator priceCalculator(&feedlog, &feedInfoMap);

    // Test currency alias resolution.
    TGlRecord glRecord;
    TOfferCtx offerContext;


    glRecord.set_delivery_currency("BYR");
    priceCalculator.ProcessOffer(&glRecord, &offerContext);
    ASSERT_EQ("BYN", glRecord.delivery_currency());

    glRecord.set_delivery_currency("BYN");
    priceCalculator.ProcessOffer(&glRecord, &offerContext);
    ASSERT_EQ("BYN", glRecord.delivery_currency());

    glRecord.set_delivery_currency("USD");
    priceCalculator.ProcessOffer(&glRecord, &offerContext);
    ASSERT_EQ("USD", glRecord.delivery_currency());

    glRecord.set_delivery_currency("EUR");
    priceCalculator.ProcessOffer(&glRecord, &offerContext);
    ASSERT_EQ("EUR", glRecord.delivery_currency());

    glRecord.set_delivery_currency("RUB");
    priceCalculator.ProcessOffer(&glRecord, &offerContext);
    ASSERT_EQ("RUR", glRecord.delivery_currency());
}

TEST(TOfferPricesCalculatorTest, HistoryPriceExpression)
{
    TFeedLog feedlog;
    THashMap<unsigned, TFeedInfo> feedInfoMap;
    CalcFeedInfoMap(feedInfoMap);
    TOfferPricesCalculator priceCalculator(&feedlog, &feedInfoMap);

    {
        // Test valid price.
        TGlRecord glRecord;
        TOfferCtx offerContext;
        glRecord.mutable_price_history()->set_price_expression("RUR 12000000");
        glRecord.mutable_price_history()->set_min_price_expression("RUR 11000000");
        NLoaders::FillOfferFieldsManually(glRecord, offerContext, &feedInfoMap, nullptr, {});

        glRecord.mutable_binary_reference_old_price()->set_price(1300000000000);

        priceCalculator.ProcessOffer(&glRecord, &offerContext);

        const auto& price = glRecord.binary_history_price();
        ASSERT_EQ(12000000, price.price());
        ASSERT_EQ("1", price.rate());
        ASSERT_EQ("RUR", price.id());
        ASSERT_EQ("RUR", price.ref_id());

        const auto& minPrice = glRecord.binary_min_price();
        ASSERT_EQ(11000000, minPrice.price());
        ASSERT_EQ("1", minPrice.rate());
        ASSERT_EQ("RUR", minPrice.id());
        ASSERT_EQ("RUR", minPrice.ref_id());

        auto referenceOldPrice = glRecord.binary_reference_old_price();
        ASSERT_EQ(1300000000000, referenceOldPrice.price());
        ASSERT_EQ("1", referenceOldPrice.rate());
        ASSERT_EQ("RUR", referenceOldPrice.id());
        ASSERT_EQ("RUR", referenceOldPrice.ref_id());
    }

    {
        // Test invalid price 1.
        TGlRecord glRecord;
        TOfferCtx offerContext;
        glRecord.mutable_price_history()->set_price_expression("RUR");

        EXPECT_THROW(NLoaders::FillOfferFieldsManually(glRecord, offerContext, &feedInfoMap, nullptr, {}), TBadRecord);
    }

    {
        // Test invalid price 2.
        TGlRecord glRecord;
        TOfferCtx offerContext;
        glRecord.mutable_price_history()->set_price_expression("12000000");

        EXPECT_THROW(NLoaders::FillOfferFieldsManually(glRecord, offerContext, &feedInfoMap, nullptr, {}), TBadRecord);
    }

    {
        // Test invalid price 3.
        TGlRecord glRecord;
        TOfferCtx offerContext;
        glRecord.mutable_price_history()->set_price_expression("RUR price");

        EXPECT_THROW(NLoaders::FillOfferFieldsManually(glRecord, offerContext, &feedInfoMap, nullptr, {}), TFromStringException);
    }
}

TEST(TOfferPricesCalculatorTest, OldPriceValidation)
{
    THashMap<unsigned, TFeedInfo> feedInfoMap;
    CalcFeedInfoMap(feedInfoMap);
    {
        // Test valid oldprice with validation and autoDiscounts on
        TFeedLog feedlog;
        TOfferPricesCalculator priceCalculator(&feedlog, &feedInfoMap);

        TGlRecord glRecord;
        TOfferCtx offerContext;

        glRecord.mutable_price_history()->set_price_expression("RUR 44440000000");
        NLoaders::FillOfferFieldsManually(glRecord, offerContext, &feedInfoMap, nullptr, {});

        glRecord.mutable_binary_price()->set_price(11110000000);
        glRecord.set_enable_auto_discounts(true);

        priceCalculator.ProcessOffer(&glRecord, &offerContext);

        auto oldprice = glRecord.binary_oldprice().price();
        ASSERT_EQ(44440000000, oldprice);
        oldprice = glRecord.binary_raw_oldprice().price();
        ASSERT_EQ(44440000000, oldprice);
    }

    {
        // Test valid oldprice with validation and autoDiscounts on
        // disabled 'cause of cutprice
        TFeedLog feedlog;
        TOfferPricesCalculator priceCalculator(&feedlog, &feedInfoMap);

        TGlRecord glRecord;
        TOfferCtx offerContext;
        glRecord.set_flags(glRecord.flags() | NMarket::NDocumentFlags::IS_CUTPRICE);

        glRecord.mutable_price_history()->set_price_expression("RUR 44440000000");
        NLoaders::FillOfferFieldsManually(glRecord, offerContext, &feedInfoMap, nullptr, {});

        glRecord.set_enable_auto_discounts(true);

        priceCalculator.ProcessOffer(&glRecord, &offerContext);

        ASSERT_FALSE(glRecord.has_binary_oldprice());
        ASSERT_FALSE(glRecord.has_binary_raw_oldprice());
    }

    {
        // Test invalid oldprice with validation and autoDiscounts on
        TFeedLog feedlog;
        TOfferPricesCalculator priceCalculator(&feedlog, &feedInfoMap);

        TGlRecord glRecord;
        TOfferCtx offerContext;

        glRecord.mutable_price_history()->set_price_expression("RUR 22220000000");
        NLoaders::FillOfferFieldsManually(glRecord, offerContext, &feedInfoMap, nullptr, {});

        glRecord.mutable_binary_price()->set_price(11110000000);
        glRecord.set_enable_auto_discounts(true);

        priceCalculator.ProcessOffer(&glRecord, &offerContext);

        ASSERT_EQ(22220000000, glRecord.binary_oldprice().price());
    }

    {
        // Test discount with no old price
        TFeedLog feedlog;
        TOfferPricesCalculator priceCalculator(&feedlog, &feedInfoMap);

        TGlRecord glRecord;
        TOfferCtx offerContext;

        glRecord.mutable_price_history()->set_price_expression("RUR 22220000000");
        NLoaders::FillOfferFieldsManually(glRecord, offerContext, &feedInfoMap, nullptr, {});

        glRecord.mutable_binary_price()->set_price(11110000000);
        glRecord.set_enable_auto_discounts(true);

        priceCalculator.ProcessOffer(&glRecord, &offerContext);

        ASSERT_EQ(22220000000, glRecord.binary_oldprice().price());
    }

    {
        // Test discount with no PricedropsTestsPassed
        TFeedLog feedlog;
        TOfferPricesCalculator priceCalculator(&feedlog, &feedInfoMap);

        TGlRecord glRecord;
        TOfferCtx offerContext;

        glRecord.mutable_price_history()->set_price_expression("RUR 22220000000");
        NLoaders::FillOfferFieldsManually(glRecord, offerContext, &feedInfoMap, nullptr, {});

        glRecord.set_enable_auto_discounts(true);
        glRecord.set_pricedrops_tests_passed(false);

        priceCalculator.ProcessOffer(&glRecord, &offerContext);

        ASSERT_FALSE(glRecord.has_binary_raw_oldprice());
    }

    {
        // Test discount with no PricedropsTestsPassed and no autodiscount
        TFeedLog feedlog;
        TOfferPricesCalculator priceCalculator(&feedlog, &feedInfoMap);

        TGlRecord glRecord;
        TOfferCtx offerContext;

        glRecord.mutable_price_history()->set_price_expression("RUR 22220000000");
        NLoaders::FillOfferFieldsManually(glRecord, offerContext, &feedInfoMap, nullptr, {});

        glRecord.set_enable_auto_discounts(false);
        glRecord.set_pricedrops_tests_passed(false);

        priceCalculator.ProcessOffer(&glRecord, &offerContext);

        ASSERT_FALSE(glRecord.has_binary_raw_oldprice());
    }

    {
        // Test enable_auto_discounts not true
        TFeedLog feedlog;
        TOfferPricesCalculator priceCalculator(&feedlog, &feedInfoMap);

        TGlRecord glRecord;
        TOfferCtx offerContext;

        glRecord.mutable_price_history()->set_price_expression("RUR 22220000000");
        NLoaders::FillOfferFieldsManually(glRecord, offerContext, &feedInfoMap, nullptr, {});

        priceCalculator.ProcessOffer(&glRecord, &offerContext);

        ASSERT_FALSE(glRecord.has_binary_raw_oldprice());
    }

    {
        // Test invalid oldprice with validation and autoDiscounts on and small diff
        TFeedLog feedlog;
        TOfferPricesCalculator priceCalculator(&feedlog, &feedInfoMap);

        TGlRecord glRecord;
        TOfferCtx offerContext;

        glRecord.mutable_price_history()->set_price_expression("RUR 22220000000");
        NLoaders::FillOfferFieldsManually(glRecord, offerContext, &feedInfoMap, nullptr, {});

        glRecord.set_enable_auto_discounts(true);

        priceCalculator.ProcessOffer(&glRecord, &offerContext);

        ASSERT_FALSE(glRecord.has_binary_raw_oldprice());
    }

    {
        // Test absent history with validation and autoDiscounts off
        TFeedLog feedlog;
        TOfferPricesCalculator priceCalculator(&feedlog, &feedInfoMap);

        TGlRecord glRecord;
        TOfferCtx offerContext;
        NLoaders::FillOfferFieldsManually(glRecord, offerContext, &feedInfoMap, nullptr, {});

        glRecord.set_enable_auto_discounts(true);

        priceCalculator.ProcessOffer(&glRecord, &offerContext);

        ASSERT_FALSE(glRecord.has_binary_raw_oldprice());
    }

    {
        // Test absent history with validation and autoDiscounts on
        TFeedLog feedlog;
        TOfferPricesCalculator priceCalculator(&feedlog, &feedInfoMap);

        TGlRecord glRecord;
        TOfferCtx offerContext;
        glRecord.mutable_binary_oldprice()->set_price(3333000000);
        NLoaders::FillOfferFieldsManually(glRecord, offerContext, &feedInfoMap, nullptr, {});

        glRecord.set_enable_auto_discounts(true);

        priceCalculator.ProcessOffer(&glRecord, &offerContext);

        ASSERT_FALSE(glRecord.has_binary_raw_oldprice());
    }
}

TEST(TOfferPricesCalculatorTest, MultiPromo)
{
    TFeedLog feedlog;
    THashMap<unsigned, TFeedInfo> feedInfoMap;
    CalcFeedInfoMap(feedInfoMap);
    TOfferPricesCalculator priceCalculator(&feedlog, &feedInfoMap);

    {
        // promo price is less than history price, it is high grade promo!
        TGlRecord glRecord;
        TOfferCtx offerContext;

        glRecord.mutable_price_history()->set_price_expression("RUR 8880000000");
        NLoaders::FillOfferFieldsManually(glRecord, offerContext, &feedInfoMap, nullptr, {});
        glRecord.set_promo_price(77700);
        glRecord.set_promo_type(ToUnderlying(NMarket::NPromo::EPromoType::PromoCode | NMarket::NPromo::EPromoType::NPlusM));

        priceCalculator.ProcessOffer(&glRecord, &offerContext);
    }
}

TEST(TOfferPricesCalculatorTest, PromoCodeQuality)
{
    TFeedLog feedlog;
    THashMap<unsigned, TFeedInfo> feedInfoMap;
    CalcFeedInfoMap(feedInfoMap);
    TOfferPricesCalculator priceCalculator(&feedlog, &feedInfoMap);

    {
        // promo price is less than history price, it is high grade promo!
        TGlRecord glRecord;
        TOfferCtx offerContext;

        glRecord.mutable_price_history()->set_price_expression("RUR 8880000000");
        NLoaders::FillOfferFieldsManually(glRecord, offerContext, &feedInfoMap, nullptr, {});
        glRecord.set_promo_price(77700);
        glRecord.set_promo_type(ToUnderlying(NMarket::NPromo::EPromoType::PromoCode));

        priceCalculator.ProcessOffer(&glRecord, &offerContext);
    }
}

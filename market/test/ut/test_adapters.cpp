#include <market/idx/promos/yt_promo_indexer/src/yt_promo_indexer.h>

#include <market/library/libpromo/common.h>
#include <market/library/libpromo/utils/protobufhelpers.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

#include <util/charset/utf8.h>
#include <util/folder/path.h>
#include <tuple>


Y_UNIT_TEST_SUITE(YtPromoIndexerAdapterTests) {
    Y_UNIT_TEST_DECLARE(testRowToMSKU);
    Y_UNIT_TEST_DECLARE(testRowToBlue3PPromo);
    Y_UNIT_TEST_DECLARE(testRowToBluePromoGwp);
    Y_UNIT_TEST_DECLARE(testRowToBlueSecretSale);
    Y_UNIT_TEST_DECLARE(testRowToBluePromoCheapestAsGift);
    Y_UNIT_TEST_DECLARE(testRowToBluePromoBlueFlash);
    Y_UNIT_TEST_DECLARE(testRowToBluePromoBlueSet);
    Y_UNIT_TEST_DECLARE(testRowToBluePromoBlueSetVariations);
    Y_UNIT_TEST_DECLARE(testRowToBluePromoDirectDiscount);
    Y_UNIT_TEST_DECLARE(testRowToBlueCashback);
    Y_UNIT_TEST_DECLARE(testRowToBluePromocode);
    Y_UNIT_TEST_DECLARE(testRowToBlueSpreadDiscountCount);
    Y_UNIT_TEST_DECLARE(testRowToBlueSpreadDiscountReceipt);
    Y_UNIT_TEST_DECLARE(testMerge);
}

class TestTYtPromoIndexer: public YtPromoIndexer::TYtPromoIndexer {
    Y_UNIT_TEST_FRIEND(YtPromoIndexerAdapterTests, testRowToMSKU);
    Y_UNIT_TEST_FRIEND(YtPromoIndexerAdapterTests, testRowToBlue3PPromo);
    Y_UNIT_TEST_FRIEND(YtPromoIndexerAdapterTests, testRowToBluePromoGwp);
    Y_UNIT_TEST_FRIEND(YtPromoIndexerAdapterTests, testRowToBlueSecretSale);
    Y_UNIT_TEST_FRIEND(YtPromoIndexerAdapterTests, testRowToBluePromoCheapestAsGift);
    Y_UNIT_TEST_FRIEND(YtPromoIndexerAdapterTests, testRowToBluePromoBlueFlash);
    Y_UNIT_TEST_FRIEND(YtPromoIndexerAdapterTests, testRowToBluePromoBlueSet);
    Y_UNIT_TEST_FRIEND(YtPromoIndexerAdapterTests, testRowToBluePromoBlueSetVariations);
    Y_UNIT_TEST_FRIEND(YtPromoIndexerAdapterTests, testRowToBluePromoDirectDiscount);
    Y_UNIT_TEST_FRIEND(YtPromoIndexerAdapterTests, testRowToBlueCashback);
    Y_UNIT_TEST_FRIEND(YtPromoIndexerAdapterTests, testRowToBluePromocode);
    Y_UNIT_TEST_FRIEND(YtPromoIndexerAdapterTests, testRowToBlueSpreadDiscountCount);
    Y_UNIT_TEST_FRIEND(YtPromoIndexerAdapterTests, testRowToBlueSpreadDiscountReceipt);
    Y_UNIT_TEST_FRIEND(YtPromoIndexerAdapterTests, testMerge);
public:
    using TYtPromoIndexer::TYtPromoIndexer;
};


Y_UNIT_TEST_SUITE_IMPLEMENTATION(YtPromoIndexerAdapterTests) {

using namespace YtPromoIndexer;

Y_UNIT_TEST(testRowToMSKU) {
    const TFsPath outPath = "./tmp";
    TFsPath(outPath).MkDirs();
    NMarket::YtPromoindexer::TOptions params;
    params.set_outdir(outPath);

    TestTYtPromoIndexer indexer(params);
    // valid row with source promo id saved in number format
    NYT::TNode row1;
    row1["promo_id"] = 10204;
    row1["msku"] = "TestMSKU1";
    row1["price"] = 100;
    row1["old_price"] = 300;

    const uint64_t precision = 100000; // лоялти выгружает цену х100, то есть без копеек
    NMarket::NPromo::TMSKUDetails expectedPromo1;
    expectedPromo1.SourcePromoId = "10204";
    expectedPromo1.MarketPromoPrice = TFixedPointNumber::CreateFromRawValue(100 * precision);
    expectedPromo1.MarketOldPrice = TFixedPointNumber::CreateFromRawValue(300 * precision);

    // valid row with source promo id saved in string format
    NYT::TNode row2;
    row2["promo_id"] = "TestSourcePromoId2";
    row2["msku"] = "TestMSKU2";
    row2["price"] = 150;
    row2["old_price"] = 350;

    NMarket::NPromo::TMSKUDetails expectedPromo2;
    expectedPromo2.SourcePromoId = "TestSourcePromoId2";
    expectedPromo2.MarketPromoPrice = TFixedPointNumber::CreateFromRawValue(150 * precision);
    expectedPromo2.MarketOldPrice = TFixedPointNumber::CreateFromRawValue(350 * precision);

    // not valid row with empty source promo id
    NYT::TNode row3;
    row3["promo_id"] = "";
    row3["msku"] = "TestMSKU3";
    row3["price"] = 1500;
    row3["old_price"] = 2500;

    // not valid row with empty msku
    NYT::TNode row4;
    row4["promo_id"] = "TestSourcePromoId4";
    row4["msku"] = "";
    row4["price"] = 1500;
    row4["old_price"] = 2500;

    // not valid row with old price < promo price
    NYT::TNode row5;
    row5["promo_id"] = "TestSourcePromoId2";
    row5["msku"] = "TestMSKU2";
    row5["price"] = 1500;
    row5["old_price"] = 250;

    // invalid row with 4% discount
    NYT::TNode row6;
    row6["promo_id"] = "TestSourcePromoId2";
    row6["msku"] = "TestMSKU3";
    row6["price"] = 721000;
    row6["old_price"] = 750000;

    // not valid row with 96% discount
    NYT::TNode row7;
    row7["promo_id"] = "TestSourcePromoId2";
    row7["msku"] = "TestMSKU4";
    row7["price"] = 1900;
    row7["old_price"] = 50000;

    // valid row with 1% discount, but in absolute sum of discount is > 500
    NYT::TNode row8;
    row8["promo_id"] = "TestSourcePromoId2";
    row8["msku"] = "TestMSKU4";
    row8["price"] = 9940000;
    row8["old_price"] = 10000000;

    // invalid row with < 1% discount, and in absolute sum of discount is > 500
    NYT::TNode row9;
    row9["promo_id"] = "TestSourcePromoId2";
    row9["msku"] = "TestMSKU5";
    row9["price"] = 9999930000;
    row9["old_price"] = 10000000000;

    ASSERT_TRUE(indexer.ValidateMSKURow(row1));
    ASSERT_TRUE(indexer.ValidateMSKURow(row2));
    ASSERT_FALSE(indexer.ValidateMSKURow(row3));
    ASSERT_FALSE(indexer.ValidateMSKURow(row4));
    ASSERT_FALSE(indexer.ValidateMSKURow(row5));
    ASSERT_FALSE(indexer.ValidateMSKURow(row6));
    ASSERT_FALSE(indexer.ValidateMSKURow(row7));
    ASSERT_TRUE(indexer.ValidateMSKURow(row8));
    ASSERT_FALSE(indexer.ValidateMSKURow(row9));

    const auto gotPromo1 = indexer.RowToMSKU(row1);
    const auto gotPromo2 = indexer.RowToMSKU(row2);

    ASSERT_EQ(expectedPromo1.SourcePromoId, gotPromo1.SourcePromoId);
    ASSERT_EQ(expectedPromo1.MarketPromoPrice.AsRaw(), gotPromo1.MarketPromoPrice.AsRaw());
    ASSERT_EQ(expectedPromo1.MarketOldPrice.AsRaw(), gotPromo1.MarketOldPrice.AsRaw());
    ASSERT_EQ(expectedPromo2.SourcePromoId, gotPromo2.SourcePromoId);
    ASSERT_EQ(expectedPromo2.MarketPromoPrice.AsRaw(), gotPromo2.MarketPromoPrice.AsRaw());
    ASSERT_EQ(expectedPromo2.MarketOldPrice.AsRaw(), gotPromo2.MarketOldPrice.AsRaw());
}


Y_UNIT_TEST(testRowToBlue3PPromo) {
    const TFsPath outPath = "./tmp";
    TFsPath(outPath).MkDirs();
    NMarket::YtPromoindexer::TOptions params;
    params.set_outdir(outPath);

    TestTYtPromoIndexer indexer(params);
    // valid row with promo id saved in number format
    NYT::TNode row1;
    row1["promo_id"] = 10204;
    row1["description"] = "TestPromo1";
    row1["start_date"] = 1000000;
    row1["end_date"] = 2000000;
    NYT::TNode regions;
    regions.Add(10);
    regions.Add(20);
    row1["promo_regions"] = regions;
    NYT::TNode allowed_payment_types1;
    allowed_payment_types1.Add("YANDEX");
    allowed_payment_types1.Add("CARD_ON_DELIVERY");
    row1["allowed_payment_types"] = allowed_payment_types1;

    // valid row with promo id saved in string format
    NYT::TNode row2;
    row2["promo_id"] = "Blue3PPromo2";
    row2["description"] = "TestPromo2";
    row2["start_date"] = 1000000;
    row2["end_date"] = 2000000;
    row2["promo_regions"] = NYT::TNode::CreateList();
    NYT::TNode allowed_payment_types2;
    allowed_payment_types2.Add("CASH_ON_DELIVERY");
    row2["allowed_payment_types"] = allowed_payment_types2;

    // not valid row with end date < start date
    NYT::TNode row3;
    row3["promo_id"] = "Blue3PPromo3";
    row3["description"] = "TestPromo3";
    row3["start_date"] = 2000000;
    row3["end_date"] = 1000000;
    row3["promo_regions"] = NYT::TNode::CreateList();
    row3["allowed_payment_types"] = NYT::TNode::CreateList();

    // not valid row with empty promo id
    NYT::TNode row4;
    row4["promo_id"] = "";
    row4["description"] = "TestPromo4";
    row4["start_date"] = 1000000;
    row4["end_date"] = 2000000;
    row4["promo_regions"] = NYT::TNode::CreateList();
    row4["allowed_payment_types"] = NYT::TNode::CreateList();

    // valid row with empty allowed empty list
    NYT::TNode row5;
    row5["promo_id"] = "Blue3PPromo5";
    row5["description"] = "TestPromo5";
    row5["start_date"] = 1000000;
    row5["end_date"] = 2000000;
    row5["promo_regions"] = NYT::TNode::CreateList();
    row5["allowed_payment_types"] = NYT::TNode::CreateList();

    // valid row with all types of payment allowed
    NYT::TNode row6;
    row6["promo_id"] = "TestPromo6";
    row6["description"] = "TestPromo6";
    row6["start_date"] = 1000000;
    row6["end_date"] = 2000000;
    row6["promo_regions"] = NYT::TNode::CreateList();
    NYT::TNode allowed_payment_types6;
    allowed_payment_types6.Add("YANDEX");
    allowed_payment_types6.Add("CASH_ON_DELIVERY");
    allowed_payment_types6.Add("CARD_ON_DELIVERY");
    row6["allowed_payment_types"] = allowed_payment_types6;

    // invalid row with wrong payment type
    NYT::TNode row7;
    row7["promo_id"] = "TestPromo7";
    row7["description"] = "TestPromo7";
    row7["start_date"] = 1000000;
    row7["end_date"] = 2000000;
    row7["promo_regions"] = NYT::TNode::CreateList();
    NYT::TNode allowed_payment_types7;
    allowed_payment_types7.Add("YANDEX"); // correct one
    allowed_payment_types7.Add("CASH_ON_DELIVERY_WRONG"); // incorrect one
    row7["allowed_payment_types"] = allowed_payment_types7;

    NMarket::NPromo::TPromoDetails expectedPromo1;
    expectedPromo1.Description = "TestPromo1";
    expectedPromo1.StartDateUTC = 1000000;
    expectedPromo1.EndDateUTC = 2000000;
    expectedPromo1.AllowedPaymentMethods = NMarket::NPaymentMethods::EPaymentMethods::CARD_ON_DELIVERY | NMarket::NPaymentMethods::EPaymentMethods::YANDEX;

    NMarket::NPromo::TPromoDetails expectedPromo2;
    expectedPromo2.Description = "TestPromo2";
    expectedPromo2.StartDateUTC = 1000000;
    expectedPromo2.EndDateUTC = 2000000;
    expectedPromo2.AllowedPaymentMethods = NMarket::NPaymentMethods::EPaymentMethods::CASH_ON_DELIVERY;

    NMarket::NPromo::TPromoDetails expectedPromo5;
    expectedPromo5.Description = "TestPromo5";
    expectedPromo5.StartDateUTC = 1000000;
    expectedPromo5.EndDateUTC = 2000000;
    expectedPromo5.AllowedPaymentMethods = NMarket::NPaymentMethods::EPaymentMethods::All;

    NMarket::NPromo::TPromoDetails expectedPromo6;
    expectedPromo6.Description = "TestPromo6";
    expectedPromo6.StartDateUTC = 1000000;
    expectedPromo6.EndDateUTC = 2000000;
    expectedPromo6.AllowedPaymentMethods = NMarket::NPaymentMethods::EPaymentMethods::All;

    ASSERT_TRUE(indexer.ValidateBlue3PPromo(row1));
    ASSERT_TRUE(indexer.ValidateBlue3PPromo(row2));
    ASSERT_FALSE(indexer.ValidateBlue3PPromo(row3));
    ASSERT_FALSE(indexer.ValidateBlue3PPromo(row4));
    ASSERT_TRUE(indexer.ValidateBlue3PPromo(row5));
    ASSERT_TRUE(indexer.ValidateBlue3PPromo(row6));
    ASSERT_FALSE(indexer.ValidateBlue3PPromo(row7));

    const auto gotPromo1 = *NProtobufHelpers::ProtobufToPromoDetails(indexer.RowToBlue3PPromo(row1));
    const auto gotPromo2 = *NProtobufHelpers::ProtobufToPromoDetails(indexer.RowToBlue3PPromo(row2));
    const auto gotPromo5 = *NProtobufHelpers::ProtobufToPromoDetails(indexer.RowToBlue3PPromo(row5));
    const auto gotPromo6 = *NProtobufHelpers::ProtobufToPromoDetails(indexer.RowToBlue3PPromo(row6));

    ASSERT_EQ(expectedPromo1.Description, gotPromo1.Description);
    ASSERT_EQ(expectedPromo1.StartDateUTC, gotPromo1.StartDateUTC);
    ASSERT_EQ(expectedPromo1.EndDateUTC, gotPromo1.EndDateUTC);
    ASSERT_EQ(expectedPromo1.AllowedPaymentMethods, gotPromo1.AllowedPaymentMethods);

    ASSERT_EQ(expectedPromo2.Description, gotPromo2.Description);
    ASSERT_EQ(expectedPromo2.StartDateUTC, gotPromo2.StartDateUTC);
    ASSERT_EQ(expectedPromo2.EndDateUTC, gotPromo2.EndDateUTC);
    ASSERT_EQ(expectedPromo2.AllowedPaymentMethods, gotPromo2.AllowedPaymentMethods);

    ASSERT_EQ(expectedPromo5.Description, gotPromo5.Description);
    ASSERT_EQ(expectedPromo5.StartDateUTC, gotPromo5.StartDateUTC);
    ASSERT_EQ(expectedPromo5.EndDateUTC, gotPromo5.EndDateUTC);
    ASSERT_EQ(expectedPromo5.AllowedPaymentMethods, gotPromo5.AllowedPaymentMethods);

    ASSERT_EQ(expectedPromo6.Description, gotPromo6.Description);
    ASSERT_EQ(expectedPromo6.StartDateUTC, gotPromo6.StartDateUTC);
    ASSERT_EQ(expectedPromo6.EndDateUTC, gotPromo6.EndDateUTC);
    ASSERT_EQ(expectedPromo6.AllowedPaymentMethods, gotPromo6.AllowedPaymentMethods);
}

Y_UNIT_TEST(testRowToBluePromoGwp) {
    const TFsPath outPath = "./tmp";
    TFsPath(outPath).MkDirs();
    NMarket::YtPromoindexer::TOptions params;
    params.set_outdir(outPath);
    params.set_usefastpromosmode(false);

    TestTYtPromoIndexer indexer(params);

    constexpr auto url = "https://beru.ru/promo/1";
    constexpr auto title = "Подарки от Huawei";
    constexpr auto shop_promo_id = "huawei_gifts";
    constexpr auto start_date = 1561939200;
    constexpr auto end_date = 1564531200;
    constexpr auto feed_id = 123456;
    constexpr auto type = NMarket::NPromo::EPromoType::GenericBundle;
    constexpr auto primary_items_count = 1;
    constexpr auto secondary_items_count = 1;
    constexpr auto secondary_price = 5577;
    constexpr auto secondary_currency = "RUR";
    constexpr auto restrict_refund = true;
    constexpr auto spread_discount = 76.54;
    constexpr auto allow_berubonus = false;
    constexpr auto allow_promocode = true;
    constexpr auto allowed_payment_methods = static_cast<uint32_t>(NMarket::NPaymentMethods::EPaymentMethods::All);
    constexpr auto disabled_by_default = true;

    const TVector <TString> BundlePrimaryOfferIds{"principal_offer_id1", "principal_offer_id2"};
    const TVector <TString> SecondaryOfferIds{"secondary_оффер_ид", "secondary_оффер_ид2"};

    NProtobufHelpers::TPromoDetailsProto proto;
    proto.set_url(url);
    proto.set_title(title);
    proto.set_shop_promo_id(shop_promo_id);
    proto.set_start_date(start_date);
    proto.set_end_date(end_date);
    proto.set_feed_id(feed_id);
    proto.set_type(static_cast<uint64_t>(type));
    proto.set_allowed_payment_methods(allowed_payment_methods);
    proto.set_disabled_by_default(disabled_by_default);
    proto.set_generation_ts(123);

    auto bundle = proto.mutable_generic_bundle();
    bundle->set_restrict_refund(restrict_refund);
    bundle->set_spread_discount(spread_discount);
    bundle->set_allow_berubonus(allow_berubonus);
    bundle->set_allow_promocode(allow_promocode);

    for (size_t i = 0; i < BundlePrimaryOfferIds.size(); i++) {
        auto bundle_content = bundle->mutable_bundles_content()->Add();
        bundle_content->mutable_primary_item()->set_offer_id(BundlePrimaryOfferIds.at(i));
        bundle_content->mutable_primary_item()->set_count(primary_items_count);
        bundle_content->mutable_secondary_item()->mutable_item()->set_offer_id(SecondaryOfferIds.at(i));
        bundle_content->mutable_secondary_item()->mutable_item()->set_count(secondary_items_count);
        bundle_content->mutable_secondary_item()->mutable_discount_price()->set_value(secondary_price);
        bundle_content->mutable_secondary_item()->mutable_discount_price()->set_currency(secondary_currency);
    }

    NYT::TNode row;
    row["promo_id"] = "test_promo";
    row["promo"] = proto.SerializeAsStringOrThrow();
    auto promoDetails = *NProtobufHelpers::ProtobufToPromoDetails(*(indexer.RowToBluePromoGwp(row)));

    UNIT_ASSERT_EQUAL(promoDetails.StartDateUTC, start_date);
    UNIT_ASSERT_EQUAL(promoDetails.EndDateUTC, end_date);
    UNIT_ASSERT_EQUAL(promoDetails.FeedId, feed_id);
    UNIT_ASSERT_EQUAL(promoDetails.ShopPromoId, shop_promo_id);
    UNIT_ASSERT_EQUAL(promoDetails.Title, title);
    UNIT_ASSERT_EQUAL(promoDetails.Type, type);
    UNIT_ASSERT_EQUAL(promoDetails.Url, url);
    UNIT_ASSERT_EQUAL(promoDetails.AllowedPaymentMethods, allowed_payment_methods);
    UNIT_ASSERT_EQUAL(promoDetails.DisabledByDefault, disabled_by_default);
    UNIT_ASSERT_EQUAL(promoDetails.GenerationTs, 0); // должно быть стёрто

    auto& gb = *(promoDetails.GenericBundle);
    UNIT_ASSERT_EQUAL(gb.RestrictRefund, restrict_refund);
    UNIT_ASSERT_EQUAL(gb.SpreadDiscount, spread_discount);
    UNIT_ASSERT_EQUAL(gb.AllowBeruBonus, allow_berubonus);
    UNIT_ASSERT_EQUAL(gb.AllowPromocode, allow_promocode);

    UNIT_ASSERT_EQUAL(gb.BundlesContent.size(), BundlePrimaryOfferIds.size());
    {
        const auto& content = gb.BundlesContent.at(NMarket::NPromo::TFeedOfferId{feed_id, BundlePrimaryOfferIds[0]});
        UNIT_ASSERT_EQUAL(content.PrimaryItem.OfferId, BundlePrimaryOfferIds[0]);
        UNIT_ASSERT_EQUAL(content.PrimaryItem.Count, primary_items_count);
        UNIT_ASSERT_EQUAL(content.SecondaryItem.Item.OfferId, SecondaryOfferIds[0]);
        UNIT_ASSERT_EQUAL(content.SecondaryItem.Item.Count, secondary_items_count);
        ASSERT_EQ(content.SecondaryItem.DiscountPrice.AsString(), "55.77");
    }
    {
        const auto& content = gb.BundlesContent.at(NMarket::NPromo::TFeedOfferId{feed_id, BundlePrimaryOfferIds[1]});
        UNIT_ASSERT_EQUAL(content.PrimaryItem.OfferId, BundlePrimaryOfferIds[1]);
        UNIT_ASSERT_EQUAL(content.PrimaryItem.Count, primary_items_count);
        UNIT_ASSERT_EQUAL(content.SecondaryItem.Item.OfferId, SecondaryOfferIds[1]);
        UNIT_ASSERT_EQUAL(content.SecondaryItem.Item.Count, secondary_items_count);
        ASSERT_EQ(content.SecondaryItem.DiscountPrice.AsString(), "55.77");
    }
}

Y_UNIT_TEST(testRowToBlueSecretSale) {
    using namespace NMarket::NPromo;

    const TFsPath outPath = "./tmp";
    TFsPath(outPath).MkDirs();

    NMarket::YtPromoindexer::TOptions params;
    params.set_outdir(outPath);

    TestTYtPromoIndexer indexer(params);

    constexpr auto promo_id = "TestSecretSale";
    constexpr auto type = EPromoType::SecretSale;
    constexpr auto title = "Закрытая распродажа для клиентов Сбербанка";
    constexpr auto description = "Типичное описание распродажи";
    constexpr auto url = "https://beru.ru/special/secret-sale";
    constexpr auto start_date = 1561939200;
    constexpr auto end_date = 1564531200;
    constexpr auto offer_discounts_size = 1;
    constexpr auto msku = 10;
    constexpr auto percent = 5.5;

    NProtobufHelpers::TPromoDetailsProto proto;
    proto.set_url(url);
    proto.set_title(title);
    proto.set_description(description);
    proto.set_start_date(start_date);
    proto.set_end_date(end_date);
    proto.set_type(static_cast<uint64_t>(type));
    proto.set_source_promo_id(promo_id);

    auto discount = proto.mutable_secret_sale_details()->mutable_offer_discounts()->Add();
    discount->set_msku(msku);
    discount->set_percent(percent);

    NYT::TNode row;
    row["promo_id"] = promo_id;
    row["promo"] = proto.SerializeAsStringOrThrow();

    auto promoDetails = *NProtobufHelpers::ProtobufToPromoDetails(indexer.RowToBlueSecretSale(row));
    ASSERT_EQ(promoDetails.SourcePromoId, promo_id);
    ASSERT_EQ(promoDetails.Type, type);
    ASSERT_EQ(promoDetails.Title, title);
    ASSERT_EQ(promoDetails.Description, description);
    ASSERT_EQ(promoDetails.Url, url);
    ASSERT_EQ(promoDetails.StartDateUTC, start_date);
    ASSERT_EQ(promoDetails.EndDateUTC, end_date);

    const auto& offerDiscounts = promoDetails.SecretSaleDetails.OfferDiscounts;
    ASSERT_EQ(offerDiscounts.size(), offer_discounts_size);
    ASSERT_TRUE(offerDiscounts.contains(msku));
    ASSERT_EQ(offerDiscounts.at(msku), percent);
}

Y_UNIT_TEST(testRowToBluePromoCheapestAsGift) {
    const TFsPath outPath = "./tmp";
    TFsPath(outPath).MkDirs();
    NMarket::YtPromoindexer::TOptions params;
    params.set_outdir(outPath);

    TestTYtPromoIndexer indexer(params);

    constexpr auto url = "https://beru.ru/promo/1";
    constexpr auto title = "Подарки от Huawei";
    constexpr auto shop_promo_id = "test_shop_promo_id";
    constexpr auto start_date = 1561939200;
    constexpr auto end_date = 1564531200;
    constexpr auto feed_id = 123456;
    constexpr auto type = NMarket::NPromo::EPromoType::CheapestAsGift;
    constexpr auto count = 44;
    constexpr auto promo_url = "promo_url";
    constexpr auto link_text = "link_text";
    constexpr auto allow_berubonus = false;
    constexpr auto allow_promocode = true;

    NProtobufHelpers::TPromoDetailsProto proto;
    proto.set_url(url);
    proto.set_title(title);
    proto.set_shop_promo_id(shop_promo_id);
    proto.set_start_date(start_date);
    proto.set_end_date(end_date);
    proto.set_feed_id(feed_id);
    proto.set_type(static_cast<uint64_t>(type));

    auto cheapest_as_gift_proto = proto.mutable_cheapest_as_gift();
    cheapest_as_gift_proto->set_count(count);
    cheapest_as_gift_proto->set_promo_url(promo_url);
    cheapest_as_gift_proto->set_link_text(link_text);
    cheapest_as_gift_proto->set_allow_berubonus(allow_berubonus);
    cheapest_as_gift_proto->set_allow_promocode(allow_promocode);

    NYT::TNode row;
    row["promo_id"] = "test_promo";
    row["promo"] = proto.SerializeAsStringOrThrow();
    auto promoDetails = *NProtobufHelpers::ProtobufToPromoDetails(*(indexer.RowToBluePromoGwp(row)));

    UNIT_ASSERT_EQUAL(promoDetails.StartDateUTC, start_date);
    UNIT_ASSERT_EQUAL(promoDetails.EndDateUTC, end_date);
    UNIT_ASSERT_EQUAL(promoDetails.FeedId, feed_id);
    UNIT_ASSERT_EQUAL(promoDetails.ShopPromoId, shop_promo_id);
    UNIT_ASSERT_EQUAL(promoDetails.Title, title);
    UNIT_ASSERT_EQUAL(promoDetails.Type, type);
    UNIT_ASSERT_EQUAL(promoDetails.Url, url);

    auto& cag = *(promoDetails.CheapestAsGift);
    UNIT_ASSERT_EQUAL(cag.Count, count);
    UNIT_ASSERT_EQUAL(cag.PromoUrl, promo_url);
    UNIT_ASSERT_EQUAL(cag.LinkText, link_text);
    UNIT_ASSERT_EQUAL(cag.AllowBeruBonus, allow_berubonus);
    UNIT_ASSERT_EQUAL(cag.AllowPromocode, allow_promocode);
}

Y_UNIT_TEST(testRowToBluePromoBlueFlash) {
    const TFsPath outPath = "./tmp";
    TFsPath(outPath).MkDirs();
    NMarket::YtPromoindexer::TOptions params;
    params.set_outdir(outPath);

    TestTYtPromoIndexer indexer(params);

    constexpr auto url = "https://beru.ru/promo/1";
    constexpr auto title = "Подарки от Huawei";
    constexpr auto shop_promo_id = "test_shop_promo_id";
    constexpr auto start_date = 1561939200;
    constexpr auto end_date = 1564531200;
    constexpr auto feed_id = 123456;
    constexpr auto type = NMarket::NPromo::EPromoType::BlueFlash;
    constexpr auto offer_id_1 = "12345";
    constexpr auto offer_id_2 = "offer_id";
    constexpr auto offer_id_3 = "яяя";
    constexpr auto offer_id_4 = "👍";
    constexpr auto offer_id_5 = "old";
    constexpr auto allow_berubonus = false;
    constexpr auto allow_promocode = true;
    constexpr auto currency = "RUR";

    NProtobufHelpers::TPromoDetailsProto proto;
    proto.set_url(url);
    proto.set_title(title);
    proto.set_shop_promo_id(shop_promo_id);
    proto.set_start_date(start_date);
    proto.set_end_date(end_date);
    proto.set_feed_id(feed_id);
    proto.set_type(static_cast<uint64_t>(type));

    {
        auto blue_flash = proto.mutable_blue_flash();
        {
            auto item1 = blue_flash->mutable_items()->Add();
            item1->mutable_price()->set_value(100);
            item1->mutable_price()->set_currency(currency);
            item1->mutable_offer()->set_feed_id(feed_id);
            item1->mutable_offer()->set_offer_id(offer_id_1);
        }
        {
            auto item2 = blue_flash->mutable_items()->Add();
            item2->mutable_price()->set_value(200);
            item2->mutable_price()->set_currency(currency);
            item2->mutable_offer()->set_feed_id(feed_id + 1);
            item2->mutable_offer()->set_offer_id(offer_id_2);
        }
        {
            auto item3 = blue_flash->mutable_items()->Add();
            item3->mutable_price()->set_value(3345);
            item3->mutable_price()->set_currency(currency);
            item3->mutable_offer()->set_feed_id(feed_id);
            item3->mutable_offer()->set_offer_id(offer_id_3);
        }
        {
            auto item4 = blue_flash->mutable_items()->Add();
            item4->mutable_price()->set_value(1);
            item4->mutable_price()->set_currency(currency);
            item4->mutable_offer()->set_feed_id(feed_id);
            item4->mutable_offer()->set_offer_id(offer_id_4);
        }
        {
            auto item5 = blue_flash->mutable_items()->Add();
            item5->mutable_price()->set_value(100);
            item5->mutable_price()->set_currency(currency);
            item5->mutable_old_price()->set_value(120);
            item5->mutable_old_price()->set_currency(currency);
            item5->mutable_offer()->set_feed_id(feed_id);
            item5->mutable_offer()->set_offer_id(offer_id_5);
        }
        blue_flash->set_allow_berubonus(allow_berubonus);
        blue_flash->set_allow_promocode(allow_promocode);
    }

    NYT::TNode row;
    row["promo_id"] = "test_promo";
    row["promo"] = proto.SerializeAsStringOrThrow();
    auto promoDetails = *NProtobufHelpers::ProtobufToPromoDetails(*(indexer.RowToBluePromoGwp(row)));

    UNIT_ASSERT_EQUAL(promoDetails.StartDateUTC, start_date);
    UNIT_ASSERT_EQUAL(promoDetails.EndDateUTC, end_date);
    UNIT_ASSERT_EQUAL(promoDetails.FeedId, feed_id);
    UNIT_ASSERT_EQUAL(promoDetails.ShopPromoId, shop_promo_id);
    UNIT_ASSERT_EQUAL(promoDetails.Title, title);
    UNIT_ASSERT_EQUAL(promoDetails.Type, type);
    UNIT_ASSERT_EQUAL(promoDetails.Url, url);

    auto& bf = *(promoDetails.BlueFlash);
    UNIT_ASSERT_EQUAL(bf.Items.size(), 5);
    {
        const auto item = bf.Items.at(NMarket::NPromo::TFeedOfferId{feed_id, offer_id_1});
        UNIT_ASSERT_EQUAL(item.Price.Value.AsString(), "1");
        UNIT_ASSERT(!item.OldPrice.Defined());
    }
    {
        const auto item = bf.Items.at(NMarket::NPromo::TFeedOfferId{feed_id + 1, offer_id_2});
        UNIT_ASSERT_EQUAL(item.Price.Value.AsString(), "2");
        UNIT_ASSERT(!item.OldPrice.Defined());
    }
    {
        const auto item = bf.Items.at(NMarket::NPromo::TFeedOfferId{feed_id, offer_id_3});
        UNIT_ASSERT_EQUAL(item.Price.Value.AsString(), "33.45");
        UNIT_ASSERT(!item.OldPrice.Defined());
    }
    {
        const auto item = bf.Items.at(NMarket::NPromo::TFeedOfferId{feed_id, offer_id_4});
        UNIT_ASSERT_EQUAL(item.Price.Value.AsString(), "0.01");
        UNIT_ASSERT(!item.OldPrice.Defined());
    }
    {
        const auto item = bf.Items.at(NMarket::NPromo::TFeedOfferId{feed_id, offer_id_5});
        UNIT_ASSERT_EQUAL(item.Price.Value.AsString(), "1");
        UNIT_ASSERT(item.OldPrice.Defined());
        UNIT_ASSERT_EQUAL(item.OldPrice->Value.AsString(), "1.2");
    }

    UNIT_ASSERT_EQUAL(bf.AllowBeruBonus, allow_berubonus);
    UNIT_ASSERT_EQUAL(bf.AllowPromocode, allow_promocode);
}


Y_UNIT_TEST(testRowToBluePromoBlueSet) {
    const TFsPath outPath = "./tmp";
    TFsPath(outPath).MkDirs();
    NMarket::YtPromoindexer::TOptions params;
    params.set_outdir(outPath);

    TestTYtPromoIndexer indexer(params);

    constexpr auto url = "https://beru.ru/promo/1";
    constexpr auto title = "Подарки от Huawei";
    constexpr auto shop_promo_id = "test_shop_promo_id";
    constexpr auto start_date = 1561939200;
    constexpr auto end_date = 1564531200;
    constexpr auto feed_id = 123456;
    constexpr auto type = NMarket::NPromo::EPromoType::BlueSet;
    constexpr auto offer_id_1 = "12345";
    constexpr auto offer_id_2 = "offer_id";
    constexpr auto offer_id_3 = "яяя";
    constexpr bool allow_berubonus = false;
    constexpr bool allow_promocode = false;
    constexpr bool restrict_refund = true;
    constexpr auto discount1 = 7.7f;
    constexpr auto discount2 = 30.1f;

    NProtobufHelpers::TPromoDetailsProto proto;
    proto.set_url(url);
    proto.set_title(title);
    proto.set_shop_promo_id(shop_promo_id);
    proto.set_start_date(start_date);
    proto.set_end_date(end_date);
    proto.set_feed_id(feed_id);
    proto.set_type(static_cast<uint64_t>(type));

    {
        auto blue_set = proto.mutable_blue_set();
        blue_set->set_allow_berubonus(allow_berubonus);
        blue_set->set_allow_promocode(allow_promocode);
        blue_set->set_restrict_refund(restrict_refund);
        {
            auto content1 = blue_set->mutable_sets_content()->Add();
            content1->set_linked(true);
            {
                auto item1 = content1->mutable_items()->Add();
                item1->set_offer_id(offer_id_1);
                item1->set_count(1);
                item1->set_discount(discount1);
            }
            {
                auto item2 = content1->mutable_items()->Add();
                item2->set_offer_id(offer_id_2);
                item2->set_count(1);
            }
            {
                auto item3 = content1->mutable_items()->Add();
                item3->set_offer_id(offer_id_3);
                item3->set_count(2);
                item3->set_discount(discount2);
            }
        }
    }

    NYT::TNode row;
    row["promo_id"] = "test_promo";
    row["promo"] = proto.SerializeAsStringOrThrow();
    auto promoDetails = *NProtobufHelpers::ProtobufToPromoDetails(*(indexer.RowToBluePromoGwp(row)));

    UNIT_ASSERT_EQUAL(promoDetails.StartDateUTC, start_date);
    UNIT_ASSERT_EQUAL(promoDetails.EndDateUTC, end_date);
    UNIT_ASSERT_EQUAL(promoDetails.FeedId, feed_id);
    UNIT_ASSERT_EQUAL(promoDetails.ShopPromoId, shop_promo_id);
    UNIT_ASSERT_EQUAL(promoDetails.Title, title);
    UNIT_ASSERT_EQUAL(promoDetails.Type, type);
    UNIT_ASSERT_EQUAL(promoDetails.Url, url);

    auto& bs = *(promoDetails.BlueSet);
    UNIT_ASSERT_EQUAL(bs.AllowBeruBonus, allow_berubonus);
    UNIT_ASSERT_EQUAL(bs.AllowPromocode, allow_promocode);
    UNIT_ASSERT_EQUAL(bs.RestrictRefund, restrict_refund);
    UNIT_ASSERT_EQUAL(bs.SetsContent.size(), 1);
    auto& content1 = bs.SetsContent[0];
    UNIT_ASSERT_EQUAL(content1.Linked, true);
    UNIT_ASSERT_EQUAL(content1.Items.size(), 3);
    auto& item1 = content1.Items[0];
    auto& item2 = content1.Items[1];
    auto& item3 = content1.Items[2];
    UNIT_ASSERT_EQUAL(item1.OfferId, offer_id_1);
    UNIT_ASSERT_EQUAL(item1.Count, 1);
    UNIT_ASSERT_EQUAL(item1.Discount, discount1);
    UNIT_ASSERT_EQUAL(item2.OfferId, offer_id_2);
    UNIT_ASSERT_EQUAL(item2.Count, 1);
    ASSERT_FALSE(item2.Discount.Defined());
    UNIT_ASSERT_EQUAL(item3.OfferId, offer_id_3);
    UNIT_ASSERT_EQUAL(item3.Count, 2);
    UNIT_ASSERT_EQUAL(item3.Discount, discount2);
}

Y_UNIT_TEST(testRowToBluePromoBlueSetVariations) {
    const TFsPath outPath = "./tmp";
    TFsPath(outPath).MkDirs();
    NMarket::YtPromoindexer::TOptions params;
    params.set_outdir(outPath);

    TestTYtPromoIndexer indexer(params);

    constexpr auto url = "https://beru.ru/promo/1";
    constexpr auto title = "Подарки от Huawei";
    constexpr auto shop_promo_id = "test_shop_promo_id";
    constexpr auto start_date = 1561939200;
    constexpr auto end_date = 1564531200;
    constexpr auto feed_id = 123456;
    constexpr auto type = NMarket::NPromo::EPromoType::BlueSet;
    constexpr auto offer_id_1 = "12345";
    constexpr auto offer_id_11 = "offer_id";
    constexpr auto offer_id_12 = "яяя";
    constexpr auto offer_id_13 = "111";
    constexpr bool allow_berubonus = false;
    constexpr bool allow_promocode = false;
    constexpr bool restrict_refund = true;
    constexpr auto discount1 = 7.7f;
    constexpr auto discount2 = 30.1f;

    NProtobufHelpers::TPromoDetailsProto proto;
    proto.set_url(url);
    proto.set_title(title);
    proto.set_shop_promo_id(shop_promo_id);
    proto.set_start_date(start_date);
    proto.set_end_date(end_date);
    proto.set_feed_id(feed_id);
    proto.set_type(static_cast<uint64_t>(type));

    {
        auto blue_set = proto.mutable_blue_set();
        blue_set->set_allow_berubonus(allow_berubonus);
        blue_set->set_allow_promocode(allow_promocode);
        blue_set->set_restrict_refund(restrict_refund);
        {
            auto content1 = blue_set->mutable_sets_content()->Add();
            content1->set_linked(false);
            {
                auto item1 = content1->mutable_items()->Add();
                item1->set_offer_id(offer_id_1);
                item1->set_count(1);
                item1->set_discount(discount1);
            }
            {
                auto item2 = content1->mutable_items()->Add();
                item2->set_offer_id(offer_id_11);
                item2->set_count(1);
                item2->set_discount(discount1);
            }
            {
                auto item3 = content1->mutable_items()->Add();
                item3->set_offer_id(offer_id_12);
                item3->set_count(2);
                item3->set_discount(discount2);
            }
        }
        {
            auto content2 = blue_set->mutable_sets_content()->Add();
            content2->set_linked(false);
            {
                auto item1 = content2->mutable_items()->Add();
                item1->set_offer_id(offer_id_1);
                item1->set_count(1);
                item1->set_discount(discount1);
            }
            {
                auto item2 = content2->mutable_items()->Add();
                item2->set_offer_id(offer_id_12);
                item2->set_count(1);
                item2->set_discount(discount1);
            }
            {
                auto item3 = content2->mutable_items()->Add();
                item3->set_offer_id(offer_id_13);
                item3->set_count(2);
                item3->set_discount(discount2);
            }
        }
        {
            auto content3 = blue_set->mutable_sets_content()->Add();
            content3->set_linked(false);
            {
                auto item1 = content3->mutable_items()->Add();
                item1->set_offer_id(offer_id_1);
                item1->set_count(1);
                item1->set_discount(discount1);
            }
            {
                auto item2 = content3->mutable_items()->Add();
                item2->set_offer_id(offer_id_11);
                item2->set_count(1);
                item2->set_discount(discount1);
            }
            {
                auto item3 = content3->mutable_items()->Add();
                item3->set_offer_id(offer_id_13);
                item3->set_count(2);
                item3->set_discount(discount2);
            }
        }
    }

    NYT::TNode row;
    row["promo_id"] = "test_promo";
    row["promo"] = proto.SerializeAsStringOrThrow();
    auto promoDetails = *NProtobufHelpers::ProtobufToPromoDetails(*(indexer.RowToBluePromoGwp(row)));

    UNIT_ASSERT_EQUAL(promoDetails.StartDateUTC, start_date);
    UNIT_ASSERT_EQUAL(promoDetails.EndDateUTC, end_date);
    UNIT_ASSERT_EQUAL(promoDetails.FeedId, feed_id);
    UNIT_ASSERT_EQUAL(promoDetails.ShopPromoId, shop_promo_id);
    UNIT_ASSERT_EQUAL(promoDetails.Title, title);
    UNIT_ASSERT_EQUAL(promoDetails.Type, type);
    UNIT_ASSERT_EQUAL(promoDetails.Url, url);

    auto& bs = *(promoDetails.BlueSet);
    UNIT_ASSERT_EQUAL(bs.AllowBeruBonus, allow_berubonus);
    UNIT_ASSERT_EQUAL(bs.AllowPromocode, allow_promocode);
    UNIT_ASSERT_EQUAL(bs.RestrictRefund, restrict_refund);
    UNIT_ASSERT_EQUAL(bs.SetsContent.size(), 3);
    {
        auto& content = bs.SetsContent[0];
        UNIT_ASSERT_EQUAL(content.Linked, false);
        UNIT_ASSERT_EQUAL(content.Items.size(), 3);
        {
            auto& item = content.Items[0];
            UNIT_ASSERT_EQUAL(item.OfferId, offer_id_1);
            UNIT_ASSERT_EQUAL(item.Count, 1);
            UNIT_ASSERT_EQUAL(item.Discount, discount1);
        }
        {
            auto& item = content.Items[1];
            UNIT_ASSERT_EQUAL(item.OfferId, offer_id_11);
            UNIT_ASSERT_EQUAL(item.Count, 1);
            UNIT_ASSERT_EQUAL(item.Discount, discount1);
        }
        {
            auto& item = content.Items[2];
            UNIT_ASSERT_EQUAL(item.OfferId, offer_id_12);
            UNIT_ASSERT_EQUAL(item.Count, 2);
            UNIT_ASSERT_EQUAL(item.Discount, discount2);
        }
    }
   {
        auto& content = bs.SetsContent[1];
        UNIT_ASSERT_EQUAL(content.Linked, false);
        UNIT_ASSERT_EQUAL(content.Items.size(), 3);
        {
            auto& item = content.Items[0];
            UNIT_ASSERT_EQUAL(item.OfferId, offer_id_1);
            UNIT_ASSERT_EQUAL(item.Count, 1);
            UNIT_ASSERT_EQUAL(item.Discount, discount1);
        }
        {
            auto& item = content.Items[1];
            UNIT_ASSERT_EQUAL(item.OfferId, offer_id_12);
            UNIT_ASSERT_EQUAL(item.Count, 1);
            UNIT_ASSERT_EQUAL(item.Discount, discount1);
        }
        {
            auto& item = content.Items[2];
            UNIT_ASSERT_EQUAL(item.OfferId, offer_id_13);
            UNIT_ASSERT_EQUAL(item.Count, 2);
            UNIT_ASSERT_EQUAL(item.Discount, discount2);
        }
    }
       {
        auto& content = bs.SetsContent[2];
        UNIT_ASSERT_EQUAL(content.Linked, false);
        UNIT_ASSERT_EQUAL(content.Items.size(), 3);
        {
            auto& item = content.Items[0];
            UNIT_ASSERT_EQUAL(item.OfferId, offer_id_1);
            UNIT_ASSERT_EQUAL(item.Count, 1);
            UNIT_ASSERT_EQUAL(item.Discount, discount1);
        }
        {
            auto& item = content.Items[1];
            UNIT_ASSERT_EQUAL(item.OfferId, offer_id_11);
            UNIT_ASSERT_EQUAL(item.Count, 1);
            UNIT_ASSERT_EQUAL(item.Discount, discount1);
        }
        {
            auto& item = content.Items[2];
            UNIT_ASSERT_EQUAL(item.OfferId, offer_id_13);
            UNIT_ASSERT_EQUAL(item.Count, 2);
            UNIT_ASSERT_EQUAL(item.Discount, discount2);
        }
    }
}

Y_UNIT_TEST(testRowToBluePromoDirectDiscount) {
    const TFsPath outPath = "./tmp";
    TFsPath(outPath).MkDirs();
    NMarket::YtPromoindexer::TOptions params;
    params.set_outdir(outPath);

    TestTYtPromoIndexer indexer(params);

    const uint64_t precision = 10'000'000; // Для directDiscount цены в YT выгружаются домноженными на 10^7

    constexpr auto title = "Прямая скидка от Huawei";
    constexpr auto shop_promo_id = "test_shop_promo_id";
    constexpr auto start_date = 1561939200;
    constexpr auto end_date = 1564531200;
    constexpr auto feed_id = 123456;
    constexpr auto type = NMarket::NPromo::EPromoType::DirectDiscount;
    const auto budget_limit_value = 777 * precision;
    constexpr auto budget_limit_currency = "RUR";
    constexpr auto offer_id_1 = "offer_1";
    const auto old_price_value = 12345 * precision;
    constexpr auto old_price_currency = "RUR";
    const auto discount_price_value = 1234 * precision;
    constexpr auto discount_price_currency = "RUR";
    const auto subsidy_value = 111 * precision;
    constexpr auto subsidy_currency = "RUR";
    constexpr auto max_discount_percent = 12.5;
    const auto max_discount_value = 122 * precision;
    constexpr auto max_discount_currency = "RUR";
    constexpr auto feed_id_2 = 123457;
    constexpr auto offer_id_2 = "offer_2";
    constexpr auto feed_id_3 = 123458;
    constexpr auto offer_id_3 = "offer_3";
    constexpr double discount_percent{7.0};
    const TVector<NMarketReport::THyperCategoryId> categories_list_1{100, 101, 102};
    constexpr double discount_by_category_discount_percent_1{10.0};
    const TVector<NMarketReport::THyperCategoryId> categories_list_2{300, 301, 302, 303};
    constexpr double discount_by_category_discount_percent_2{15.0};
    constexpr auto allow_berubonus = true;
    constexpr auto allow_promocode = true;

    NProtobufHelpers::TPromoDetailsProto proto;
    proto.set_title(title);
    proto.set_shop_promo_id(shop_promo_id);
    proto.set_start_date(start_date);
    proto.set_end_date(end_date);
    proto.set_feed_id(feed_id);
    proto.set_type(static_cast<uint64_t>(type));

    auto direct_discount_proto = proto.mutable_direct_discount();
    auto dd_item = direct_discount_proto->mutable_items()->Add();
    dd_item->set_feed_id(feed_id);
    dd_item->set_offer_id(offer_id_1);
    dd_item->mutable_old_price()->set_value(old_price_value);
    dd_item->mutable_old_price()->set_currency(old_price_currency);
    dd_item->mutable_discount_price()->set_value(discount_price_value);
    dd_item->mutable_discount_price()->set_currency(discount_price_currency);
    dd_item->mutable_subsidy()->set_value(subsidy_value);
    dd_item->mutable_subsidy()->set_currency(subsidy_currency);
    dd_item->set_max_discount_percent(max_discount_percent);
    dd_item->mutable_max_discount()->set_value(max_discount_value);
    dd_item->mutable_max_discount()->set_currency(max_discount_currency);

    auto dd_item_2 = direct_discount_proto->mutable_items()->Add();
    dd_item_2->set_feed_id(feed_id_2);
    dd_item_2->set_offer_id(offer_id_2);

    auto dd_item_3 = direct_discount_proto->mutable_items()->Add();
    dd_item_3->set_feed_id(feed_id_3);
    dd_item_3->set_offer_id(offer_id_3);
    dd_item_3->set_discount_percent(discount_percent);

    auto proto_elem_1{direct_discount_proto->mutable_discounts_by_category()->Add()};
    proto_elem_1->set_discount_percent(discount_by_category_discount_percent_1);
    for (const auto cat: categories_list_1) {
        auto proto_cat{proto_elem_1->mutable_category_restriction()->mutable_categories()->Add()};
        *proto_cat = cat;
    }

    auto proto_elem_2{direct_discount_proto->mutable_discounts_by_category()->Add()};
    proto_elem_2->set_discount_percent(discount_by_category_discount_percent_2);
    for (const auto cat: categories_list_2) {
        auto proto_cat{proto_elem_2->mutable_category_restriction()->mutable_categories()->Add()};
        *proto_cat = cat;
    }

    direct_discount_proto->set_allow_berubonus(allow_berubonus);
    direct_discount_proto->set_allow_promocode(allow_promocode);
    direct_discount_proto->mutable_budget_limit()->set_value(budget_limit_value);
    direct_discount_proto->mutable_budget_limit()->set_currency(budget_limit_currency);

    NYT::TNode row;
    row["promo_id"] = "test_promo";
    row["promo"] = proto.SerializeAsStringOrThrow();
    auto promoDetails = *NProtobufHelpers::ProtobufToPromoDetails(*(indexer.RowToBluePromoGwp(row)));

    UNIT_ASSERT_EQUAL(promoDetails.StartDateUTC, start_date);
    UNIT_ASSERT_EQUAL(promoDetails.EndDateUTC, end_date);
    UNIT_ASSERT_EQUAL(promoDetails.FeedId, feed_id);
    UNIT_ASSERT_EQUAL(promoDetails.ShopPromoId, shop_promo_id);
    UNIT_ASSERT_EQUAL(promoDetails.Title, title);
    UNIT_ASSERT_EQUAL(promoDetails.Type, type);

    auto& dd = *(promoDetails.DirectDiscount);
    UNIT_ASSERT_EQUAL(dd.Items.size(), 3);
    {
        const auto item = dd.Items.at(NMarket::NPromo::TFeedOfferId{feed_id, offer_id_1});
        UNIT_ASSERT_EQUAL(item.OldPrice->Value.AsString(), ToString(old_price_value / precision));
        UNIT_ASSERT_EQUAL(item.OldPrice->Currency.AlphaCode(), old_price_currency);
        UNIT_ASSERT_EQUAL(item.DiscountPrice->Value.AsString(), ToString(discount_price_value / precision));
        UNIT_ASSERT_EQUAL(item.DiscountPrice->Currency.AlphaCode(), discount_price_currency);
        UNIT_ASSERT_EQUAL(item.Subsidy->Value.AsString(), ToString(subsidy_value / precision));
        UNIT_ASSERT_EQUAL(item.Subsidy->Currency.AlphaCode(), subsidy_currency);
        UNIT_ASSERT_EQUAL(item.MaxDiscountPercent, max_discount_percent);
        UNIT_ASSERT_EQUAL(item.MaxDiscount->Value.AsString(), ToString(max_discount_value / precision));
        UNIT_ASSERT_EQUAL(item.MaxDiscount->Currency.AlphaCode(), max_discount_currency);
    }
    {
        const auto item = dd.Items.at(NMarket::NPromo::TFeedOfferId{feed_id_2, offer_id_2});
        ASSERT_FALSE(item.OldPrice.Defined());
        ASSERT_FALSE(item.DiscountPrice.Defined());
    }
    {
        const auto item = dd.Items.at(NMarket::NPromo::TFeedOfferId{feed_id_3, offer_id_3});
        ASSERT_FALSE(item.OldPrice.Defined());
        ASSERT_FALSE(item.DiscountPrice.Defined());
        ASSERT_TRUE(item.DiscountPercent.Defined());
        UNIT_ASSERT_EQUAL(item.DiscountPercent, discount_percent);
    }
    UNIT_ASSERT_EQUAL(dd.AllowBeruBonus, allow_berubonus);
    UNIT_ASSERT_EQUAL(dd.AllowPromocode, allow_promocode);
    UNIT_ASSERT_EQUAL(dd.BudgetLimit->Value.AsString(), ToString(budget_limit_value / precision));
    UNIT_ASSERT_EQUAL(dd.BudgetLimit->Currency.AlphaCode(), budget_limit_currency);

    const auto& cats{dd.DiscountsByCategory};
    UNIT_ASSERT_EQUAL(cats.size(), 2);
    UNIT_ASSERT_EQUAL(*cats[0].DiscountPercent, discount_by_category_discount_percent_1);
    UNIT_ASSERT_EQUAL(cats[0].CategoryRestriction.Categories, categories_list_1);
    UNIT_ASSERT_EQUAL(*cats[1].DiscountPercent, discount_by_category_discount_percent_2);
    UNIT_ASSERT_EQUAL(cats[1].CategoryRestriction.Categories, categories_list_2);
}

Y_UNIT_TEST(testRowToBlueCashback) {
    const TFsPath outPath = "./tmp";
    TFsPath(outPath).MkDirs();
    NMarket::YtPromoindexer::TOptions params;
    params.set_outdir(outPath);

    TestTYtPromoIndexer indexer(params);

    constexpr auto title = "cashback_id";
    constexpr auto shop_promo_id = "cashback_id";
    constexpr auto start_date = 1561939200;
    constexpr auto end_date = 1564531200;
    constexpr auto feed_id = 123456;
    constexpr auto type = NMarket::NPromo::EPromoType::BlueCashback;
    constexpr auto share = 0.4;
    constexpr auto version = 2;
    constexpr auto priority = 3;
    constexpr auto promo_bucket_name = "somename";
    TVector<TString> required_promo_buckets = {"first", "second", "third"};
    TVector<NMarket::NPromo::EUserDeviceType> user_device_types = {
        NMarket::NPromo::EUserDeviceType::DESKTOP,
        NMarket::NPromo::EUserDeviceType::TOUCH,
        NMarket::NPromo::EUserDeviceType::MARKET_GO
    };
    constexpr auto parent_promo_id = "parent_promo";
    constexpr auto cms_description_semantic_id = "default-cashback";
    constexpr auto partner_id = 12345678;
    constexpr auto tariff_version_id = 123;
    TVector<NMarket::NPaymentMethods::TPaymentMethods> allowed_payment_methods =
        {NMarket::NPaymentMethods::EPaymentMethods::CARD_ON_DELIVERY, NMarket::NPaymentMethods::EPaymentMethods::YANDEX};
    constexpr auto details_group_key = "somegroupkey";
    constexpr auto details_group_name = "somegroupname";
    TVector<TString> max_offer_total_thresholds_codes = {"code1", "blabla2", "code3", "emptycode"};
    TVector<uint64_t> max_offer_total_thresholds_values = {123, 456, 678};
    TVector<TString> min_order_total_thresholds_codes = {"blabla1", "code2", "something3"};
    TVector<uint64_t> min_order_total_thresholds_values = {234, 567, 789};
    constexpr auto max_offer_cashback = 1000;  // 10rub
    constexpr auto perk1 = Market::PERK_YANDEX_CASHBACK;
    constexpr auto perk2 = Market::PERK_BERU_PLUS;
    constexpr auto perk3 = "non_deterministic_perk";
    constexpr auto loyaltyStatus = Market::Promo::LoyaltyProgramStatus::ENABLED;
    constexpr auto atSupplierWarehouse = true;
    constexpr auto deliveryPartnerTypes = NMarketReport::NOutput::EDeliveryPartnerType::YANDEX_MARKET;
    constexpr auto flag1 = "flag-for-promo-1";
    constexpr auto flag2 = "flag-for-promo-2";
    auto uiPromoFlags = TVector{"extra-cashback", "sometag"};

    NProtobufHelpers::TPromoDetailsProto proto;
    proto.set_title(title);
    proto.set_shop_promo_id(shop_promo_id);
    proto.set_start_date(start_date);
    proto.set_end_date(end_date);
    proto.set_feed_id(feed_id);
    proto.set_type(static_cast<uint64_t>(type));
    proto.set_promo_bucket_name(promo_bucket_name);
    for (const auto& bucket_name : required_promo_buckets) {
        proto.add_required_promo_buckets(bucket_name);
    }
    for (const auto& device : user_device_types) {
        proto.add_user_device_types(static_cast<Market::Promo::UserDeviceType>(device));
    }
    proto.set_parent_promo_id(parent_promo_id);
    proto.set_cms_description_semantic_id(cms_description_semantic_id);
    for (const auto& flagValue : uiPromoFlags){
        proto.add_ui_promo_tags(flagValue);
    }

    auto blue_cashback_proto = proto.mutable_blue_cashback();
    blue_cashback_proto->set_share(share);
    blue_cashback_proto->set_version(version);
    blue_cashback_proto->set_priority(priority);
    for (const auto& payment_method : allowed_payment_methods) {
        blue_cashback_proto->add_allowed_payment_methods(static_cast<NMarket::Common::Promo::EPaymentMethods>(payment_method));
    }
    blue_cashback_proto->set_details_group_key(details_group_key);
    blue_cashback_proto->set_details_group_name(details_group_name);
    blue_cashback_proto->mutable_partner_info()->set_market_tariffs_version_id(tariff_version_id);
    blue_cashback_proto->mutable_partner_info()->set_partner_id(partner_id);
    for (size_t i = 0; i < max_offer_total_thresholds_codes.size(); i++) {
        auto thresholdProto = blue_cashback_proto->mutable_max_offer_total_thresholds()->Add();
        thresholdProto->set_code(max_offer_total_thresholds_codes[i]);

        if (i < max_offer_total_thresholds_values.size()) {
            thresholdProto->set_value(max_offer_total_thresholds_values[i]);
        }
    }
    for (size_t i = 0; i < min_order_total_thresholds_codes.size(); i++) {
        auto thresholdProto = blue_cashback_proto->mutable_min_order_total_thresholds()->Add();
        thresholdProto->set_code(min_order_total_thresholds_codes[i]);

        if (i < min_order_total_thresholds_values.size()) {
            thresholdProto->set_value(min_order_total_thresholds_values[i]);
        }
    }

    auto & predicate = *(blue_cashback_proto->add_predicates());
    predicate.add_perks(perk1);               // we can parse both lower case ...
    predicate.add_perks(ToUpperUTF8(perk2));  // and upper case perk names
    predicate.add_perks(perk3);               // произвольный перк
    predicate.set_at_supplier_warehouse(atSupplierWarehouse);
    predicate.set_loyalty_program_status(loyaltyStatus);
    predicate.add_delivery_partner_types(static_cast<Market::Promo::DeliveryPartnerType>(deliveryPartnerTypes));
    predicate.add_experiment_rearr_flags(flag1);
    predicate.add_experiment_rearr_flags(flag2);
    blue_cashback_proto->set_max_offer_cashback(max_offer_cashback);

    NYT::TNode row;
    row["promo_id"] = "test_promo";
    row["promo"] = proto.SerializeAsStringOrThrow();
    auto promoDetails = *NProtobufHelpers::ProtobufToPromoDetails(*(indexer.RowToBluePromoGwp(row)));

    UNIT_ASSERT_EQUAL(promoDetails.StartDateUTC, start_date);
    UNIT_ASSERT_EQUAL(promoDetails.EndDateUTC, end_date);
    UNIT_ASSERT_EQUAL(promoDetails.FeedId, feed_id);
    UNIT_ASSERT_EQUAL(promoDetails.ShopPromoId, shop_promo_id);
    UNIT_ASSERT_EQUAL(promoDetails.Title, title);
    UNIT_ASSERT_EQUAL(promoDetails.Type, type);
    UNIT_ASSERT_EQUAL(promoDetails.UiPromoTags.size(),  2);
    UNIT_ASSERT_EQUAL(promoDetails.UiPromoTags[0], "extra-cashback");
    UNIT_ASSERT_EQUAL(promoDetails.UiPromoTags[1], "sometag");
    UNIT_ASSERT_EQUAL(promoDetails.PromoBucketName, promo_bucket_name);
    UNIT_ASSERT_EQUAL(promoDetails.RequiredPromoBuckets.size(), required_promo_buckets.size());
    for (size_t i = 0; i < required_promo_buckets.size(); i++) {
        UNIT_ASSERT_EQUAL(promoDetails.RequiredPromoBuckets[i], required_promo_buckets[i]);
    }
    UNIT_ASSERT_EQUAL(promoDetails.UserDeviceTypes.size(), user_device_types.size());
    for (size_t i = 0; i < user_device_types.size(); i++) {
        UNIT_ASSERT_EQUAL(promoDetails.UserDeviceTypes[i], user_device_types[i]);
    }
    UNIT_ASSERT_EQUAL(promoDetails.ParentPromoId, parent_promo_id);
    UNIT_ASSERT_EQUAL(promoDetails.CmsDescriptionSemanticId, cms_description_semantic_id);

    auto& blueCashback = *(promoDetails.BlueCashback);
    UNIT_ASSERT_EQUAL(blueCashback.Share, share);
    UNIT_ASSERT_EQUAL(blueCashback.Version, version);
    UNIT_ASSERT_EQUAL(blueCashback.Priority, priority);
    UNIT_ASSERT_EQUAL(blueCashback.AllowedPaymentMethods.size(), allowed_payment_methods.size());
    for (size_t i = 0; i < allowed_payment_methods.size(); i++) {
        UNIT_ASSERT_EQUAL(blueCashback.AllowedPaymentMethods[i], allowed_payment_methods[i]);
    }
    UNIT_ASSERT_EQUAL(blueCashback.DetailsGroupKey, details_group_key);
    UNIT_ASSERT_EQUAL(blueCashback.DetailsGroupName, details_group_name);
    UNIT_ASSERT_EQUAL(blueCashback.PartnerInfo->MarketTariffsVersionId, tariff_version_id);
    UNIT_ASSERT_EQUAL(blueCashback.PartnerInfo->PartnerId, partner_id);
    UNIT_ASSERT_EQUAL(blueCashback.MaxOfferTotalThresholds.size(), 4);
    for (size_t i = 0; i < max_offer_total_thresholds_codes.size(); i++) {
        UNIT_ASSERT_EQUAL(blueCashback.MaxOfferTotalThresholds[i].Code, max_offer_total_thresholds_codes[i]);
        if (i < max_offer_total_thresholds_values.size()) {
            UNIT_ASSERT_EQUAL(blueCashback.MaxOfferTotalThresholds[i].Value, max_offer_total_thresholds_values[i]);
        } else {
            UNIT_ASSERT(!blueCashback.MaxOfferTotalThresholds[i].Value);
        }
    }
    UNIT_ASSERT_EQUAL(blueCashback.MinOrderTotalThresholds.size(), 3);
    for (size_t i = 0; i < min_order_total_thresholds_codes.size(); i++) {
        UNIT_ASSERT_EQUAL(blueCashback.MinOrderTotalThresholds[i].Value,
            min_order_total_thresholds_values[i]);
        UNIT_ASSERT_EQUAL(blueCashback.MinOrderTotalThresholds[i].Code,
            min_order_total_thresholds_codes[i]);
    }
    UNIT_ASSERT_EQUAL(blueCashback.Predicates.size(), 1);
    UNIT_ASSERT(blueCashback.Predicates[0].Perks.contains(perk1));
    UNIT_ASSERT(blueCashback.Predicates[0].Perks.contains(perk2));
    UNIT_ASSERT(blueCashback.Predicates[0].Perks.contains(perk3));
    UNIT_ASSERT_EQUAL(blueCashback.Predicates[0].AtSupplierWarehouse, atSupplierWarehouse);
    UNIT_ASSERT_EQUAL(blueCashback.Predicates[0].LoyaltyProgramStatus, static_cast<NMarket::NBind::ELoyaltyProgramStatus>(loyaltyStatus));
    UNIT_ASSERT_EQUAL(blueCashback.Predicates[0].DeliveryPartnerTypes.size(), 1);
    UNIT_ASSERT_EQUAL(*blueCashback.Predicates[0].DeliveryPartnerTypes.begin(), static_cast<NMarketReport::NOutput::EDeliveryPartnerType>(deliveryPartnerTypes));
    UNIT_ASSERT_EQUAL(blueCashback.Predicates[0].ExperimentRearrFlags.size(), 2);
    UNIT_ASSERT_EQUAL(blueCashback.Predicates[0].ExperimentRearrFlags[0], flag1);
    UNIT_ASSERT_EQUAL(blueCashback.Predicates[0].ExperimentRearrFlags[1], flag2);
    UNIT_ASSERT(blueCashback.MaxOfferCashbackRub.Defined());
    UNIT_ASSERT_EQUAL(*blueCashback.MaxOfferCashbackRub, TFixedPointNumber(2, 10.0));
}

Y_UNIT_TEST(testRowToBluePromocode) {
    const TFsPath outPath = "./tmp";
    TFsPath(outPath).MkDirs();
    NMarket::YtPromoindexer::TOptions params;
    params.set_outdir(outPath);

    TestTYtPromoIndexer indexer(params);

    constexpr auto title = "promocode_title";
    constexpr auto shop_promo_id = "promocode_shop_promo_id";
    constexpr auto start_date = 1561939200;
    constexpr auto end_date = 1564531200;
    constexpr auto feed_id = 123456;
    constexpr auto type = NMarket::NPromo::EPromoType::PromoCode;
    constexpr auto discount_value = 300;
    constexpr auto discount_currency = "RUR";
    constexpr auto url = "promocode_url";
    constexpr auto landing_url = "promocode_landing_url";
    constexpr auto conditions = "conditions";
    constexpr NMarket::NPromo::EMechanicsPaymentType mechanics_payment_type = NMarket::NPromo::EMechanicsPaymentType::CPA;
    constexpr auto source_type = NMarket::NPromo::EPromoSourceType::PARTNER_SOURCE;
    constexpr auto source_reference = "source_reference";
    constexpr auto priority = 7;
    const TVector<uint32_t> regions = {50, 51, 52};
    const TVector<uint32_t> excludedRegions = {53, 54};
    constexpr auto order_min_price_value{300};
    constexpr auto order_min_price_currency{"RUR"};
    constexpr auto order_max_price_value{500};
    constexpr auto order_max_price_currency{"RUR"};
    constexpr auto price_restriction_category_id {40521};
    constexpr auto price_restriction_min_price_value {99};
    constexpr auto price_restriction_min_price_currency{"RUR"};
    constexpr auto price_restriction_max_price_value {9999};
    constexpr auto price_restriction_max_price_currency{"RUR"};
    constexpr auto restricted_promo_types = NMarket::NPromo::EPromoType::GenericBundle | NMarket::NPromo::EPromoType::BlueFlash;
    constexpr auto market_division = "market_division";

    NProtobufHelpers::TPromoDetailsProto proto;
    proto.set_title(title);
    proto.set_shop_promo_id(shop_promo_id);
    proto.set_start_date(start_date);
    proto.set_end_date(end_date);
    proto.set_feed_id(feed_id);
    proto.set_type(static_cast<uint64_t>(type));
    proto.set_url(url);
    proto.set_landing_url(landing_url);
    proto.set_conditions(conditions);
    proto.set_mechanics_payment_type(static_cast<Market::Promo::MechanicsPaymentType>(mechanics_payment_type));
    proto.set_source_type(source_type);
    proto.set_source_reference(source_reference);
    proto.set_priority(priority);

    proto.mutable_discount()->set_value(discount_value);
    proto.mutable_discount()->set_currency(discount_currency);

    for (auto regionId: regions) {
        proto.mutable_restrictions()->mutable_region_restriction()->add_regions(regionId);
    }
    for (auto excludedRegionId: excludedRegions) {
        proto.mutable_restrictions()->mutable_region_restriction()->add_excluded_regions(excludedRegionId);
    }
    proto.mutable_restrictions()->mutable_order_min_price()->set_value(order_min_price_value);
    proto.mutable_restrictions()->mutable_order_min_price()->set_currency(order_min_price_currency);
    proto.mutable_restrictions()->mutable_order_max_price()->set_value(order_max_price_value);
    proto.mutable_restrictions()->mutable_order_max_price()->set_currency(order_max_price_currency);
    proto.mutable_restrictions()->add_restricted_promo_types(NMarket::Common::Promo::EPromoType::GenericBundle);
    proto.mutable_restrictions()->add_restricted_promo_types(NMarket::Common::Promo::EPromoType::BlueFlash);
    auto& category_price_restriction {*proto.mutable_restrictions()->add_category_price_restrictions()};
    category_price_restriction.set_category_id(price_restriction_category_id);
    category_price_restriction.mutable_min_price()->set_value(price_restriction_min_price_value);
    category_price_restriction.mutable_min_price()->set_currency(price_restriction_min_price_currency);
    category_price_restriction.mutable_max_price()->set_value(price_restriction_max_price_value);
    category_price_restriction.mutable_max_price()->set_currency(price_restriction_max_price_currency);

    proto.mutable_budget_sources()->add_budget_source_type(Market::Promo::BudgetSourceType::VENDOR);
    proto.set_market_division(market_division);

    NYT::TNode row;
    row["promo_id"] = "promocode_id";
    row["promo"] = proto.SerializeAsStringOrThrow();
    auto promoDetails = *NProtobufHelpers::ProtobufToPromoDetails(*(indexer.RowToBluePromoGwp(row)));

    UNIT_ASSERT_EQUAL(promoDetails.StartDateUTC, start_date);
    UNIT_ASSERT_EQUAL(promoDetails.EndDateUTC, end_date);
    UNIT_ASSERT_EQUAL(promoDetails.FeedId, feed_id);
    UNIT_ASSERT_EQUAL(promoDetails.ShopPromoId, shop_promo_id);
    UNIT_ASSERT_EQUAL(promoDetails.Title, title);
    UNIT_ASSERT_EQUAL(promoDetails.Type, type);
    UNIT_ASSERT_EQUAL(promoDetails.Discount.Value.AsString(), std::to_string(discount_value));
    UNIT_ASSERT_EQUAL(promoDetails.Discount.Currency.AlphaCode(), discount_currency);
    UNIT_ASSERT_EQUAL(promoDetails.Url, url);
    UNIT_ASSERT_EQUAL(promoDetails.LandingUrl, landing_url);
    UNIT_ASSERT_EQUAL(promoDetails.Conditions, conditions);
    UNIT_ASSERT_EQUAL(promoDetails.MechanicsPaymentType, mechanics_payment_type);
    UNIT_ASSERT_EQUAL(promoDetails.SourceType, source_type);
    UNIT_ASSERT_EQUAL(promoDetails.SourceReference, source_reference);
    UNIT_ASSERT_EQUAL(promoDetails.Priority, priority);
    ASSERT_TRUE(promoDetails.Restrictions);
    ASSERT_TRUE(promoDetails.Restrictions->RegionRestriction);
    const auto& regionRestriction{promoDetails.Restrictions->RegionRestriction};
    ASSERT_EQ(regions.size(), regionRestriction->Regions.size());
    for (auto regionId: regions) {
        ASSERT_TRUE(regionRestriction->Regions.contains(regionId));
    }
    ASSERT_EQ(excludedRegions.size(), regionRestriction->ExcludedRegions.size());
    for (auto excludedRegionId: excludedRegions) {
        ASSERT_TRUE(regionRestriction->ExcludedRegions.contains(excludedRegionId));
    }
    ASSERT_TRUE(promoDetails.Restrictions->OrderMinPrice);
    UNIT_ASSERT_EQUAL(promoDetails.Restrictions->OrderMinPrice->Value.AsString(), std::to_string(order_min_price_value));
    UNIT_ASSERT_EQUAL(promoDetails.Restrictions->OrderMinPrice->Currency.AlphaCode(), order_min_price_currency);
    ASSERT_TRUE(promoDetails.Restrictions->OrderMaxPrice);
    UNIT_ASSERT_EQUAL(promoDetails.Restrictions->OrderMaxPrice->Value.AsString(), std::to_string(order_max_price_value));
    UNIT_ASSERT_EQUAL(promoDetails.Restrictions->OrderMaxPrice->Currency.AlphaCode(), order_max_price_currency);
    ASSERT_TRUE(promoDetails.Restrictions->RestrictedPromoTypes);
    UNIT_ASSERT_EQUAL(promoDetails.Restrictions->RestrictedPromoTypes, restricted_promo_types);
    UNIT_ASSERT_EQUAL(promoDetails.Restrictions->CategoryPriceRestrictions.size(), 1);
    const auto* categoryPriceRestriction {promoDetails.Restrictions->CategoryPriceRestrictions.FindPtr(price_restriction_category_id)};
    ASSERT_TRUE(categoryPriceRestriction);
    ASSERT_TRUE(categoryPriceRestriction->MinPrice);
    UNIT_ASSERT_EQUAL(categoryPriceRestriction->MinPrice->Value.AsString(), std::to_string(price_restriction_min_price_value));
    UNIT_ASSERT_EQUAL(categoryPriceRestriction->MinPrice->Currency.AlphaCode(), price_restriction_min_price_currency);
    ASSERT_TRUE(categoryPriceRestriction->MaxPrice);
    UNIT_ASSERT_EQUAL(categoryPriceRestriction->MaxPrice->Value.AsString(), std::to_string(price_restriction_max_price_value));
    UNIT_ASSERT_EQUAL(categoryPriceRestriction->MaxPrice->Currency.AlphaCode(), price_restriction_max_price_currency);

    ASSERT_TRUE(promoDetails.BudgetSources);
    UNIT_ASSERT_EQUAL(promoDetails.BudgetSources->BudgetSourceTypes.size(), 1);
    UNIT_ASSERT_EQUAL(promoDetails.BudgetSources->BudgetSourceTypes[0], NMarket::NPromo::EBudgetSourceType::VENDOR);
    UNIT_ASSERT_EQUAL(promoDetails.MarketDivision, market_division);
}

Y_UNIT_TEST(testRowToBlueSpreadDiscountCount) {
    const TFsPath outPath = "./tmp";
    TFsPath(outPath).MkDirs();
    NMarket::YtPromoindexer::TOptions params;
    params.set_outdir(outPath);

    TestTYtPromoIndexer indexer(params);

    constexpr auto title = "Прогрессирующая скидка от количества";
    constexpr auto shop_promo_id = "spdc_shop_promo_id";
    constexpr auto start_date = 1561939200;
    constexpr auto end_date = 1564531200;
    constexpr auto feed_id = 123456;
    constexpr auto type = NMarket::NPromo::EPromoType::SpreadDiscountCount;
    constexpr auto url = "spread_discount_count_url";
    constexpr auto landing_url = "spread_discount_count_landing_url";
    constexpr auto source_type = NMarket::NPromo::EPromoSourceType::PARTNER_SOURCE;
    constexpr auto source_reference = "spread_discount_count_source_reference";
    constexpr auto restricted_promo_types = NMarket::NPromo::EPromoType::BlueCashback;
    constexpr auto msku1 = 6820;
    constexpr auto count1_1 = 3;
    constexpr auto percent_discount1_1 = 12.0;
    constexpr auto count1_2 = 4;
    constexpr auto percent_discount1_2 = 14.0;
    constexpr auto msku2 = 6821;
    constexpr auto count2_1 = 5;
    constexpr auto percent_discount2_1 = 16.0;

    NProtobufHelpers::TPromoDetailsProto proto;
    proto.set_title(title);
    proto.set_shop_promo_id(shop_promo_id);
    proto.set_start_date(start_date);
    proto.set_end_date(end_date);
    proto.set_feed_id(feed_id);
    proto.set_type(static_cast<uint64_t>(type));
    proto.set_url(url);
    proto.set_landing_url(landing_url);
    proto.set_source_type(source_type);
    proto.set_source_reference(source_reference);

    proto.mutable_restrictions()->add_restricted_promo_types(NMarket::Common::Promo::EPromoType::BlueCashback);
    auto spread_discount_count_proto = proto.mutable_spread_discount_count();
    {
        auto item = spread_discount_count_proto->mutable_discount_items()->Add();
        item->set_msku(msku1);
        auto bound1 = item->mutable_count_bounds()->Add();
        bound1->set_count(count1_1);
        bound1->set_percent_discount(percent_discount1_1);
        auto bound2 = item->mutable_count_bounds()->Add();
        bound2->set_count(count1_2);
        bound2->set_percent_discount(percent_discount1_2);
    }
    {
        auto item = spread_discount_count_proto->mutable_discount_items()->Add();
        item->set_msku(msku2);
        auto bound1 = item->mutable_count_bounds()->Add();
        bound1->set_count(count2_1);
        bound1->set_percent_discount(percent_discount2_1);
    }

    NYT::TNode row;
    row["promo_id"] = "spread_discount_count_id";
    row["promo"] = proto.SerializeAsStringOrThrow();
    auto promoDetails = *NProtobufHelpers::ProtobufToPromoDetails(*(indexer.RowToBluePromoGwp(row)));

    UNIT_ASSERT_EQUAL(promoDetails.StartDateUTC, start_date);
    UNIT_ASSERT_EQUAL(promoDetails.EndDateUTC, end_date);
    UNIT_ASSERT_EQUAL(promoDetails.FeedId, feed_id);
    UNIT_ASSERT_EQUAL(promoDetails.ShopPromoId, shop_promo_id);
    UNIT_ASSERT_EQUAL(promoDetails.Title, title);
    UNIT_ASSERT_EQUAL(promoDetails.Type, type);
    UNIT_ASSERT_EQUAL(promoDetails.Url, url);
    UNIT_ASSERT_EQUAL(promoDetails.LandingUrl, landing_url);
    UNIT_ASSERT_EQUAL(promoDetails.SourceType, source_type);
    UNIT_ASSERT_EQUAL(promoDetails.SourceReference, source_reference);
    ASSERT_TRUE(promoDetails.Restrictions);
    ASSERT_TRUE(promoDetails.Restrictions->RestrictedPromoTypes);
    UNIT_ASSERT_EQUAL(promoDetails.Restrictions->RestrictedPromoTypes, restricted_promo_types);
    ASSERT_TRUE(promoDetails.SpreadDiscountCount);
    const auto& sdc {*promoDetails.SpreadDiscountCount};
    UNIT_ASSERT_EQUAL(sdc.Items.size(), 2);
    {
        ASSERT_TRUE(sdc.Items.FindPtr(msku1));
        const auto& bounds {*sdc.Items.FindPtr(msku1)};
        UNIT_ASSERT_EQUAL(bounds.size(), 2);
        UNIT_ASSERT_EQUAL(bounds[0].Count, count1_1);
        UNIT_ASSERT_EQUAL(bounds[0].PercentDiscount, percent_discount1_1);
        UNIT_ASSERT_EQUAL(bounds[1].Count, count1_2);
        UNIT_ASSERT_EQUAL(bounds[1].PercentDiscount, percent_discount1_2);
    }
    {
        ASSERT_TRUE(sdc.Items.FindPtr(msku2));
        const auto& bounds {*sdc.Items.FindPtr(msku2)};
        UNIT_ASSERT_EQUAL(bounds.size(), 1);
        UNIT_ASSERT_EQUAL(bounds[0].Count, count2_1);
        UNIT_ASSERT_EQUAL(bounds[0].PercentDiscount, percent_discount2_1);
    }

}

Y_UNIT_TEST(testRowToBlueSpreadDiscountReceipt) {
    const TFsPath outPath = "./tmp";
    TFsPath(outPath).MkDirs();
    NMarket::YtPromoindexer::TOptions params;
    params.set_outdir(outPath);

    TestTYtPromoIndexer indexer(params);

    constexpr auto title = "Прогрессирующая скидка от подборки";
    constexpr auto shop_promo_id = "spdr_shop_promo_id";
    constexpr auto start_date = 1561939200;
    constexpr auto end_date = 1564531200;
    constexpr auto feed_id = 123456;
    constexpr auto type = NMarket::NPromo::EPromoType::SpreadDiscountReceipt;
    constexpr auto url = "spread_discount_receipt_url";
    constexpr auto landing_url = "spread_discount_receipt_landing_url";
    constexpr auto source_type = NMarket::NPromo::EPromoSourceType::ROBOT;
    constexpr auto source_reference = "spread_discount_receipt_source_reference";
    constexpr auto discount_price1_value = 6820;
    constexpr auto discount_price1_currency = "RUR";
    constexpr auto percent_discount1 = 12.0;
    constexpr auto discount_price2_value = 6821;
    constexpr auto discount_price2_currency = "RUR";
    constexpr auto absolute_discount2_value = 21;
    constexpr auto absolute_discount2_currency = "RUR";

    NProtobufHelpers::TPromoDetailsProto proto;
    proto.set_title(title);
    proto.set_shop_promo_id(shop_promo_id);
    proto.set_start_date(start_date);
    proto.set_end_date(end_date);
    proto.set_feed_id(feed_id);
    proto.set_type(static_cast<uint64_t>(type));
    proto.set_url(url);
    proto.set_landing_url(landing_url);
    proto.set_source_type(source_type);
    proto.set_source_reference(source_reference);

    auto spread_discount_receipt_proto = proto.mutable_spread_discount_receipt();
    {
        auto bound1 = spread_discount_receipt_proto->mutable_receipt_bounds()->Add();
        bound1->mutable_discount_price()->set_value(discount_price1_value);
        bound1->mutable_discount_price()->set_currency(discount_price1_currency);
        bound1->set_percent_discount(percent_discount1);
    }
    {
        auto bound2 = spread_discount_receipt_proto->mutable_receipt_bounds()->Add();
        bound2->mutable_discount_price()->set_value(discount_price2_value);
        bound2->mutable_discount_price()->set_currency(discount_price2_currency);
        bound2->mutable_absolute_discount()->set_value(absolute_discount2_value);
        bound2->mutable_absolute_discount()->set_currency(absolute_discount2_currency);
    }

    NYT::TNode row;
    row["promo_id"] = "spread_discount_receipt_id";
    row["promo"] = proto.SerializeAsStringOrThrow();
    auto promoDetails = *NProtobufHelpers::ProtobufToPromoDetails(*(indexer.RowToBluePromoGwp(row)));

    UNIT_ASSERT_EQUAL(promoDetails.StartDateUTC, start_date);
    UNIT_ASSERT_EQUAL(promoDetails.EndDateUTC, end_date);
    UNIT_ASSERT_EQUAL(promoDetails.FeedId, feed_id);
    UNIT_ASSERT_EQUAL(promoDetails.ShopPromoId, shop_promo_id);
    UNIT_ASSERT_EQUAL(promoDetails.Title, title);
    UNIT_ASSERT_EQUAL(promoDetails.Type, type);
    UNIT_ASSERT_EQUAL(promoDetails.Url, url);
    UNIT_ASSERT_EQUAL(promoDetails.LandingUrl, landing_url);
    UNIT_ASSERT_EQUAL(promoDetails.SourceType, source_type);
    UNIT_ASSERT_EQUAL(promoDetails.SourceReference, source_reference);
    ASSERT_TRUE(promoDetails.SpreadDiscountReceipt);
    const auto& sdr {*promoDetails.SpreadDiscountReceipt};
    UNIT_ASSERT_EQUAL(sdr.ReceiptBounds.size(), 2);
    UNIT_ASSERT_EQUAL(sdr.ReceiptBounds[0].DiscountPrice.Value.AsString(), ToString(discount_price1_value));
    UNIT_ASSERT_EQUAL(sdr.ReceiptBounds[0].DiscountPrice.Currency.AlphaCode(), discount_price1_currency);
    ASSERT_TRUE(sdr.ReceiptBounds[0].PercentDiscount);
    UNIT_ASSERT_EQUAL(*sdr.ReceiptBounds[0].PercentDiscount, percent_discount1);

    UNIT_ASSERT_EQUAL(sdr.ReceiptBounds[1].DiscountPrice.Value.AsString(), std::to_string(discount_price2_value));
    UNIT_ASSERT_EQUAL(sdr.ReceiptBounds[1].DiscountPrice.Currency.AlphaCode(), discount_price2_currency);
    ASSERT_TRUE(sdr.ReceiptBounds[1].AbsoluteDiscount);
    UNIT_ASSERT_EQUAL(sdr.ReceiptBounds[1].AbsoluteDiscount->Value.AsString(), std::to_string(absolute_discount2_value));
    UNIT_ASSERT_EQUAL(sdr.ReceiptBounds[1].AbsoluteDiscount->Currency.AlphaCode(), absolute_discount2_currency);

}

Y_UNIT_TEST(testMerge) {
    const TFsPath outPath = "./tmp";
    TFsPath(outPath).MkDirs();
    NMarket::YtPromoindexer::TOptions params;
    params.set_outdir(outPath);

    TestTYtPromoIndexer indexer(params);

    constexpr auto shop_promo_id = "promo_id";
    constexpr auto type = NMarket::NPromo::EPromoType::DirectDiscount;
    constexpr auto source_type = NMarket::NPromo::EPromoSourceType::PARTNER_SOURCE;

    auto&& funcGetPromoProto = [&](const auto& data) {
        NProtobufHelpers::TPromoDetailsProto proto;
        proto.set_shop_promo_id(shop_promo_id);
        proto.set_type(static_cast<uint64_t>(type));
        proto.set_source_type(source_type);

        auto direct_discount_proto = proto.mutable_direct_discount();
        for (const auto& item : data) {
            auto dd_item = direct_discount_proto->mutable_items()->Add();
            dd_item->set_feed_id(std::get<0>(item));
            dd_item->set_offer_id(TString(std::get<1>(item)));
            dd_item->mutable_old_price()->set_value(std::get<2>(item));
        }

        return proto;
    };

    static constexpr std::array<std::tuple<int, TStringBuf, int>, 2> d1 = {
        std::make_tuple(1, TStringBuf("товар"), 22),
        std::make_tuple(3, TStringBuf("другой"), 44)
    };

    static constexpr std::array<std::tuple<int, TStringBuf, int>, 1> d2 = {
        std::make_tuple(777, TStringBuf("777"), 777)
    };

    Market::Promo::PromoDetails promo1 = funcGetPromoProto(d1);
    TMaybe<Market::Promo::PromoDetails> promo2 = funcGetPromoProto(d2);

    indexer.MergeOffersToPromo(promo2, promo1);

    static constexpr std::array<std::tuple<int, TStringBuf, int>, 3> d3 = {
        std::make_tuple(1, TStringBuf("товар"), 22),
        std::make_tuple(3, TStringBuf("другой"), 44),
        std::make_tuple(777, TStringBuf("777"), 777)
    };
    Market::Promo::PromoDetails promo3 = funcGetPromoProto(d3);
    UNIT_ASSERT_EQUAL(promo1.SerializeAsStringOrThrow(), promo3.SerializeAsStringOrThrow());
}

} // YtPromoIndexerAdapterTests

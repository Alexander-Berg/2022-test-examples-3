#include <market/idx/feeds/qparser/tests/parser_test_runner.h>
#include <market/idx/feeds/qparser/tests/test_utils.h>

#include <market/idx/feeds/qparser/src/feed_parsers/common/csv/feed_parser.h>
#include <market/idx/feeds/qparser/src/feed_parsers/common/yml/feed_parser.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

using namespace NMarket;

NMarket::TFeedInfo GetDefaultFeedInfo(NMarket::EFeedType feedType) {
    NMarket::TFeedInfo feedInfo;
    feedInfo.FeedType = feedType;
    feedInfo.SessionId = 20190701;
    feedInfo.IsDiscountsEnabled = false;
    feedInfo.IsRegularParsing = false;
    feedInfo.Timestamp = google::protobuf::util::TimeUtil::SecondsToTimestamp(1000);
    feedInfo.PushFeedClass = Market::DataCamp::API::FeedClass::FEED_CLASS_ASSORTMENT_BASIC_PATCH_UPDATE;
    feedInfo.MarketColor = EMarketColor::MC_WHITE;

    return feedInfo;
}



// Поля min_quantity и count не относятся к базовым частям, не должны их парсить и светить по ним ошибки
// Отсутствует колонка цены, тоже не должны падать в связи с этим
static const TString INPUT_CSV(R"wrap(shop-sku;description;picture;country_of_origin;box_count;min_quantity;count
    offer-with-all-good-fields;description1;https://some/pic.png;CHINA;12;;
    offer-with-some-invalid-basic-field;description2;bad\bad url;;5;;
    offer-with-some-invalid-service-field;description3;;USA;4;-1;-1
    offer-with-service-fields;description4;https://pic/url.jpg;RUSSIA;4;2;3
)wrap");

// Те же оффера, что и в CSV, но в формате YML
static const TString INPUT_YML(R"wrap(<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE yml_catalog SYSTEM 'shops.dtd'>
<yml_catalog>
    <shop>
    <offers>
        <offer>
            <shop-sku>offer-with-all-good-fields</shop-sku>
            <description>description1</description>
            <picture>https://some/pic.png</picture>
            <country_of_origin>CHINA</country_of_origin>
            <box-count>12</box-count>
        </offer>
        <offer>
            <shop-sku>offer-with-some-invalid-basic-field</shop-sku>
            <description>description2</description>
            <picture>bad\bad url</picture>
            <country_of_origin></country_of_origin>
            <box-count>5</box-count>
        </offer>
        <offer>
            <shop-sku>offer-with-some-invalid-service-field</shop-sku>
            <description>description3</description>
            <picture></picture>
            <country_of_origin>USA</country_of_origin>
            <box-count>4</box-count>
            <min_quantity>-1</min_quantity>
            <count>-1</count>
        </offer>
        <offer>
            <shop-sku>offer-with-service-fields</shop-sku>
            <description>description4</description>
            <picture>https://pic/url.jpg</picture>
            <country_of_origin>RUSSIA</country_of_origin>
            <box-count>4</box-count>
            <min_quantity>2</min_quantity>
            <count>3</count>
        </offer>
    </offers>
    </shop>
</yml_catalog>)wrap");

static const TString EXPECTED_JSON = TString(R"wrap(
[
    {
        "OfferId": "offer-with-all-good-fields",
        "Description": "description1",
        "Picture": "https://some/pic.png",
        "CountryOfOrigin": "CHINA",
        "BoxCount": 12
    },
    {
        "OfferId": "offer-with-some-invalid-basic-field",
        "Description": "description2",
        "BoxCount": 5
    },
    {
        "OfferId": "offer-with-some-invalid-service-field",
        "Description": "description3",
        "CountryOfOrigin": "USA",
        "BoxCount": 4
    },
    {
        "OfferId": "offer-with-service-fields",
        "Description": "description4",
        "Picture": "https://pic/url.jpg",
        "CountryOfOrigin": "RUSSIA",
        "BoxCount": 4
    }
]
)wrap");

auto processOffer = [](const TQueueItem& item) {
    NSc::TValue result;
    result["OfferId"] = item->DataCampOffer.identifiers().offer_id();

    if (item->DataCampOffer.content().partner().original().has_description()) {
        result["Description"] = item->DataCampOffer.content().partner().original().description().value();
    }
    if (item->DataCampOffer.pictures().partner().original().source().size() > 0) {
        result["Picture"] = item->DataCampOffer.pictures().partner().original().source().at(0).url();
    }
    if (item->DataCampOffer.content().partner().original().country_of_origin().value().size() > 0) {
        result["CountryOfOrigin"] = item->DataCampOffer.content().partner().original().country_of_origin().value().at(0);
    }
    if (item->DataCampOffer.content().partner().original_terms().has_box_count()) {
        result["BoxCount"] = item->DataCampOffer.content().partner().original_terms().box_count().value();
    }
    if (item->DataCampOffer.content().partner().original_terms().has_quantity()) {
        result["MinQuantity"] = item->DataCampOffer.content().partner().original_terms().quantity().min();
    }
    if (item->DataCampOffer.stock_info().has_partner_stocks()) {
        result["StockCount"] = item->DataCampOffer.stock_info().partner_stocks().count();
    }
    if (item->RawPrice) {
        result["Price"] = *item->RawPrice;
    }
    if (item->RawOldPrice) {
        result["OldPrice"] = *item->RawOldPrice;
    }
    return result;
};

TEST(TestCsvAssortmentFeed, NotWriteServiceFieldErrors) {
    auto feedInfo = GetDefaultFeedInfo(EFeedType::CSV);
    feedInfo.CheckFeedMode = true;
    feedInfo.IgnoreExtraTags = true;

    const auto [actual, checkResult] = RunFeedParserWithCheckFeed<NMarket::NCommon::TCsvAssortmentFeedParser>(
            INPUT_CSV,
            processOffer,
            feedInfo
    );

    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);

    ASSERT_EQ(checkResult.log_message().size(), 1); // только ошибка про невалидный урл картинки
    const auto& error = checkResult.log_message().at(0);
    ASSERT_EQ(error.code(), "352");
    ASSERT_EQ(error.offer_supplier_sku(), "offer-with-some-invalid-basic-field");
}

TEST(TestYmlAssortmentFeed, NotWriteServiceFieldErrors) {
    auto feedInfo = GetDefaultFeedInfo(EFeedType::YML);
    feedInfo.CheckFeedMode = true;
    feedInfo.IgnoreExtraTags = true;

    const auto [actual, checkResult] = RunFeedParserWithCheckFeed<NMarket::NCommon::TYmlAssortmentFeedParser>(
            INPUT_YML,
            processOffer,
            feedInfo
    );

    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);

    ASSERT_EQ(checkResult.log_message().size(), 1); // только ошибка про невалидный урл картинки
    const auto& error = checkResult.log_message().at(0);
    ASSERT_EQ(error.code(), "352");
    ASSERT_EQ(error.offer_supplier_sku(), "offer-with-some-invalid-basic-field");
}




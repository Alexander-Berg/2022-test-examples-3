#include <market/idx/feeds/qparser/tests/parser_test_runner.h>
#include <market/idx/feeds/qparser/tests/test_utils.h>

#include <market/idx/feeds/qparser/src/feed_parsers/blue/csv/feed_parser.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

using namespace NMarket;

TEST(BlueCsvParser, UnknownDialect) {
    TSizedLFQueue<IFeedParser::TMsgPtr> inputQueue(10000);
    const TString inputCsv("shop-sku|price");
    auto parser = CreateFeedParser<NBlue::TCsvFeedParser>(inputCsv, GetDefaultBlueFeedInfo(EFeedType::CSV));
    parser->Start(inputQueue);
    ASSERT_TRUE(parser->GetLastError());
    ASSERT_TRUE(parser->GetLastError()->Contains("unknown CSV dialect"));
}

TEST(BlueCsvParser, RequiredColumnMissing) {
    TSizedLFQueue<IFeedParser::TMsgPtr> inputQueue(10000);
    const TString inputCsv("count");
    auto parser = CreateFeedParser<NBlue::TCsvFeedParser>(inputCsv, GetDefaultBlueFeedInfo(EFeedType::CSV));
    parser->Start(inputQueue);
    ASSERT_TRUE(parser->GetLastError());
    ASSERT_TRUE(parser->GetLastError()->Contains("cannot find required columns in header"));
}

TEST(BlueCsvParser, RequiredColumnMissingCheckFeedMode) {
    TSizedLFQueue<IFeedParser::TMsgPtr> inputQueue(10000);
    const TString inputCsv("shop-sku");
    auto feedInfo = GetDefaultBlueFeedInfo(EFeedType::CSV);
    feedInfo.CheckFeedMode = true;
    auto parser = CreateFeedParser<NBlue::TCsvFeedParser>(inputCsv, feedInfo);
    parser->Start(inputQueue);
    ASSERT_TRUE(parser->GetLastError());
    ASSERT_TRUE(parser->GetLastError()->Contains("cannot find required columns in header"));
}

TEST(BlueCsvParser, UsingAlias) {
    TSizedLFQueue<IFeedParser::TMsgPtr> inputQueue(10000);
    const TString inputCsv("shop_sku;price");
    auto parser = CreateFeedParser<NBlue::TCsvFeedParser>(inputCsv, GetDefaultBlueFeedInfo(EFeedType::CSV));
    parser->Start(inputQueue);
    ASSERT_FALSE(parser->GetLastError());
}

TEST(BlueCsvParser, SplitError) {
    TSizedLFQueue<IFeedParser::TMsgPtr> inputQueue(10000);
    // cannot find end of quoted field
    const TString inputCsv("shop-sku;price\n1;\"2");
    auto parser = CreateFeedParser<NBlue::TCsvUpdateFeedParser>(inputCsv, GetDefaultBlueFeedInfo(EFeedType::CSV));
    parser->Start(inputQueue);
    ASSERT_FALSE(parser->GetLastError());
}

TEST(BlueCsvParser, RequiredColumnsForUpdateFeed) {
    TSizedLFQueue<IFeedParser::TMsgPtr> inputQueue(10000);
    const TString inputCsv("shop-sku\n1234");
    NMarket::TFeedInfo feedInfo = GetDefaultBlueFeedInfo(EFeedType::CSV);
    feedInfo.PushFeedClass = Market::DataCamp::API::FEED_CLASS_UPDATE;
    auto parser = CreateFeedParser<NBlue::TCsvUpdateFeedParser>(inputCsv, feedInfo);
    parser->Start(inputQueue);
    ASSERT_FALSE(parser->GetLastError());
}

TEST(BlueCsvParser, RequiredColumnsForStockFeed) {
    TSizedLFQueue<IFeedParser::TMsgPtr> inputQueue(10000);
    const TString inputCsv("shop-sku\n1235");
    NMarket::TFeedInfo feedInfo = GetDefaultBlueFeedInfo(EFeedType::CSV);
    feedInfo.PushFeedClass = Market::DataCamp::API::FEED_CLASS_STOCK;
    auto parser = CreateFeedParser<NBlue::TCsvStockFeedParser>(inputCsv, feedInfo);
    parser->Start(inputQueue);
    ASSERT_FALSE(parser->GetLastError());
}

TEST(BlueCsvParser, AllowedFieldsForStockFeed) {
    TSizedLFQueue<IFeedParser::TMsgPtr> inputQueue(10000);
    const TString inputCsv("shop-sku;price;count\n1235;1000;1");
    NMarket::TFeedInfo feedInfo = GetDefaultBlueFeedInfo(EFeedType::CSV);
    feedInfo.PushFeedClass = Market::DataCamp::API::FEED_CLASS_STOCK;
    auto parser = CreateFeedParser<NBlue::TCsvStockFeedParser>(inputCsv, feedInfo);
    parser->Start(inputQueue);
    ASSERT_FALSE(parser->GetLastError());
}

TEST(BlueCsvParser, RequiredColumnsForStockFeedMissOne) {
    TSizedLFQueue<IFeedParser::TMsgPtr> inputQueue(10000);
    const TString inputCsv("count\n1235");
    NMarket::TFeedInfo feedInfo = GetDefaultBlueFeedInfo(EFeedType::CSV);
    feedInfo.PushFeedClass = Market::DataCamp::API::FEED_CLASS_STOCK;
    auto parser = CreateFeedParser<NBlue::TCsvStockFeedParser>(inputCsv, feedInfo);
    parser->Start(inputQueue);
    ASSERT_TRUE(parser->GetLastError());
    ASSERT_TRUE(parser->GetLastError()->Contains("cannot find required columns in header"));
}

TEST(BlueCsvParser, UnknownColumn) {
    TSizedLFQueue<IFeedParser::TMsgPtr> inputQueue(10000);
    const TString inputCsv("shop-sku;price;what-a-column\n1;\"2\";some value");
    auto parser = CreateFeedParser<NBlue::TCsvFeedParser>(inputCsv, GetDefaultBlueFeedInfo(EFeedType::CSV));
    parser->Start(inputQueue);
    ASSERT_TRUE(parser->GetLastError()->Contains("undefined column name \"what-a-column\""));
}

TEST(BlueCsvParser, ColumnSellerWarrantyShouldBeKnown) {
    // Колонка seller_warranty теперь не поддерживается
    // Значения не читаем, а наличие колонки не должно приводить к ошибке парсинга
    TSizedLFQueue<IFeedParser::TMsgPtr> inputQueue(10000);
    const TString inputCsv("shop-sku;seller_warranty;id;price");
    auto parser = CreateFeedParser<NBlue::TCsvFeedParser>(inputCsv, GetDefaultBlueFeedInfo(EFeedType::CSV));
    parser->Start(inputQueue);
    ASSERT_FALSE(parser->GetLastError().Defined());
}

TEST(BlueCsvParser, CBidColumn) {
    // cbid - is old legacy column, but many old white partners still have it in their feeds
    TSizedLFQueue<IFeedParser::TMsgPtr> inputQueue(10000);
    const TString inputCsv("shop-sku;price;cbid\n1;\"2\";ignored cbid value");
    auto parser = CreateFeedParser<NBlue::TCsvFeedParser>(inputCsv, GetDefaultBlueFeedInfo(EFeedType::CSV));
    parser->Start(inputQueue);
    ASSERT_FALSE(parser->GetLastError().Defined());
}

TEST(BlueCsvParser, CreditTemplateIdsColumn) {
    // credit-template-ids - is old legacy column, but many old white partners still have it in their feeds
    TSizedLFQueue<IFeedParser::TMsgPtr> inputQueue(10000);
    const TString inputCsv("shop-sku;price;credit-template-ids\n1;\"2\";ignored credit-template-ids value");
    auto parser = CreateFeedParser<NBlue::TCsvFeedParser>(inputCsv, GetDefaultBlueFeedInfo(EFeedType::CSV));
    parser->Start(inputQueue);
    ASSERT_FALSE(parser->GetLastError().Defined());
}

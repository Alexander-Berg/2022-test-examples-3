#include <market/idx/feeds/qparser/tests/test_utils.h>
#include <market/idx/feeds/qparser/tests/parser_test_runner.h>

#include <market/idx/feeds/qparser/src/feed_parsers/blue/csv/feed_parser.h>
#include <market/idx/feeds/qparser/src/feed_parsers/white/csv/feed_parser.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

#include <util/folder/path.h>
#include <util/stream/file.h>

using namespace NMarket;

TEST(BlueCsvParser, MARKETINDEXER_42896) {
    TSizedLFQueue<IFeedParser::TMsgPtr> inputQueue(10000);
    const TString inputCsv("shop-sku");
    auto parser = CreateFeedParser<NBlue::TCsvFeedParser>(inputCsv, GetDefaultBlueFeedInfo(EFeedType::CSV));
    parser->Start(inputQueue);
    ASSERT_FALSE(parser->GetLastError());
}

TEST(BlueCsvParser, MARKETINDEXER_42896_CheckFeedMode) {
    auto feedInfo = GetDefaultBlueFeedInfo(EFeedType::CSV);
    feedInfo.CheckFeedMode = true;
    TSizedLFQueue<IFeedParser::TMsgPtr> inputQueue(10000);
    const TString inputCsv("shop-sku");
    auto parser = CreateFeedParser<NBlue::TCsvFeedParser>(inputCsv, feedInfo);
    parser->Start(inputQueue);
    ASSERT_TRUE(parser->GetLastError());
    ASSERT_TRUE(parser->GetLastError()->Contains("cannot find required columns in header"));
    ASSERT_TRUE(parser->GetLastError()->Contains("price"));
}

TEST(WhiteCsvParser, MARKETINDEXER_42896) {
    TSizedLFQueue<IFeedParser::TMsgPtr> inputQueue(10000);
    const TString inputCsv("shop-sku");
    auto parser = CreateFeedParser<TCsvFeedParser>(inputCsv, GetDefaultBlueFeedInfo(EFeedType::CSV));
    parser->Start(inputQueue);
    ASSERT_FALSE(parser->GetLastError());
}

TEST(WhiteCsvParser, MARKETINDEXER_42896_CheckFeedMode) {
    auto feedInfo = GetDefaultBlueFeedInfo(EFeedType::CSV);
    feedInfo.CheckFeedMode = true;
    TSizedLFQueue<IFeedParser::TMsgPtr> inputQueue(10000);
    const TString inputCsv("shop-sku");
    auto parser = CreateFeedParser<TCsvFeedParser>(inputCsv, feedInfo);
    parser->Start(inputQueue);
    ASSERT_TRUE(parser->GetLastError());
    ASSERT_TRUE(parser->GetLastError()->Contains("cannot find required columns in header"));
    ASSERT_TRUE(parser->GetLastError()->Contains("price"));
}


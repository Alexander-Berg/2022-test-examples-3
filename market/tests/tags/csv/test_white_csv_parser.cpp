#include <market/idx/feeds/qparser/tests/parser_test_runner.h>
#include <market/idx/feeds/qparser/tests/test_utils.h>

#include <market/idx/feeds/qparser/src/feed_parsers/white/csv/feed_parser.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

using namespace NMarket;

TEST(WhiteCsvParser, CBidColumn) {
    // cbid - is old legacy column, but many old white partners still have it in their feeds
    TSizedLFQueue<IFeedParser::TMsgPtr> inputQueue(10000);
    const TString inputCsv("id;price;cbid\n\"1\";2;ignored cbid value");
    auto parser = CreateFeedParser<TCsvFeedParser>(inputCsv, GetDefaultWhiteFeedInfo(EFeedType::CSV));
    parser->Start(inputQueue);
    ASSERT_FALSE(parser->GetLastError().Defined());
}

TEST(WhiteCsvParser, CreditTemplateIdsColumn) {
    // cbid - is old legacy column, but many old white partners still have it in their feeds
    TSizedLFQueue<IFeedParser::TMsgPtr> inputQueue(10000);
    const TString inputCsv("id;price;credit-template-ids\n\"1\";2;ignored credit-template-ids value");
    auto parser = CreateFeedParser<TCsvFeedParser>(inputCsv, GetDefaultWhiteFeedInfo(EFeedType::CSV));
    parser->Start(inputQueue);
    ASSERT_FALSE(parser->GetLastError().Defined());
}

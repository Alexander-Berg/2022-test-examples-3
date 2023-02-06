#pragma once

#include "test_utils.h"

#include <market/idx/feeds/qparser/inc/feed_parser.h>
#include <market/idx/feeds/qparser/inc/queue_iterator.h>
#include <market/idx/feeds/qparser/lib/category_tree.h>
#include <market/idx/feeds/qparser/lib/price_calculator.h>
#include <market/idx/feeds/qparser/lib/uniq.h>
#include <market/idx/feeds/qparser/src/feed_parsers/white/yml/feed_parser.h>

#include <market/idx/library/explanation_log/explanation_log.h>

#include <market/library/currency_exchange/currency_exchange.h>
#include <market/library/process_log/checker_process_log.h>
#include <market/library/process_log/process_log.h>

#include <library/cpp/scheme/scheme.h>
#include <library/cpp/testing/unittest/env.h>

#include <util/generic/string.h>
#include <util/generic/maybe.h>
#include <util/stream/mem.h>

#include <functional>

namespace NMarket {

using TQueueItem = TQueueIterator<const IFeedParser::TMsg>::TMsgPtr;
using TQueueItemSelector = std::function<TMaybe<NSc::TValue>(const TQueueItem&)>;

template<class YmlFeedParser>
NSc::TValue RunWhiteYmlFeedParser(
    const TString& inputXml,
    const TQueueItemSelector& queueItemSelector,
    const TFeedInfo& feedInfo = GetDefaultWhiteFeedInfo()
) {
    THolder<IFeedParser> feedParser;
    return RunWhiteYmlFeedParser<YmlFeedParser>(inputXml, queueItemSelector, feedParser, feedInfo);
}

    template<class YmlFeedParser>
    NSc::TValue RunWhiteYmlFeedParser(
        const TString& inputXml,
        const TQueueItemSelector& queueItemSelector,
        THolder<IFeedParser>& feedParser,
        const TFeedInfo& feedInfo = GetDefaultWhiteFeedInfo()
    ) {
        if (!CEXCHANGE.IsLoaded()) {
            CEXCHANGE.Load(SRC_("data/currency_rates.xml"));
        }

        THolder<IInputStream> feed = MakeHolder<TMemoryInput>(inputXml);
        THolder<IFeedParser> parser = MakeHolder<YmlFeedParser>(std::move(feed), feedInfo);

        TSizedLFQueue<IFeedParser::TMsgPtr> inputQueue(10000);
        parser->Start(inputQueue);

        NSc::TValue actual;
        for (const auto& msgPtr: TQueueRange(inputQueue)) {
            auto item = queueItemSelector(msgPtr);
            if (item.Defined()) {
                actual.GetArrayMutable().push_back(item.GetRef());
            }
        }
        feedParser = std::move(parser);
        return actual;
    }


template<class YmlFeedParser>
NSc::TValue RunWhiteYmlFeedParserWithCheckFeed(
        const TString& inputXml,
        const TQueueItemSelector& queueItemSelector,
        const TFeedInfo& feedInfo = GetDefaultWhiteFeedInfo(),
        const TString outputDir = ""
) {
    TString failsDumpFile = JoinFsPaths(outputDir, "check_feed_log_failures.log");
    NMarket::NProcessLog::TGlobalProcessLogRAII<NMarket::NProcessLog::TCheckerProcessLog> checkProcessLog(
            true,
            NMarket::NProcessLog::TProcessLogOptions()
                    .CrashOnFailure(false)
                    .ErrorDumpFile(failsDumpFile)
                    .OutputDir(outputDir)
    );

    return RunWhiteYmlFeedParser<YmlFeedParser>(inputXml, queueItemSelector, feedInfo);
}

template<class YmlFeedParser>
NSc::TValue RunWhiteYmlFeedParserWithExplanation(
        const TString& inputXml,
        const TQueueItemSelector& queueItemSelector,
        const TFeedInfo& feedInfo = GetDefaultWhiteFeedInfo(),
        const TString feedErrorFile = ""
) {
    auto options = NMarket::NExplanationLog::TExplanationLogOptions()
            .LogLevel(Market::DataCamp::Explanation::DEBUG)
            .OutputFileName(feedErrorFile);

    EXPLANATION_PROTO_LOG.Init(options);
    // после каждого теста этот синглтон будет уничтожаться
    auto explanationProtoLogGuard = MakeHolder<NMarket::NExplanationLog::TExplanationProtoLogRAIIGuard>(EXPLANATION_PROTO_LOG);

    return RunWhiteYmlFeedParser<YmlFeedParser>(inputXml, queueItemSelector, feedInfo);
}

}  //  namespace NMarket

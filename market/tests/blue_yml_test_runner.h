#pragma once

#include "test_utils.h"

#include <market/idx/feeds/qparser/inc/feed_parser.h>
#include <market/idx/feeds/qparser/inc/queue_iterator.h>
#include <market/idx/feeds/qparser/lib/category_tree.h>
#include <market/idx/feeds/qparser/lib/price_calculator.h>
#include <market/idx/feeds/qparser/lib/uniq.h>
#include <market/idx/feeds/qparser/src/feed_parsers/blue/yml/feed_parser.h>

#include <market/library/currency_exchange/currency_exchange.h>
#include <market/library/process_log/checker_process_log.h>
#include <market/library/process_log/process_log.h>

#include <library/cpp/scheme/scheme.h>
#include <library/cpp/testing/unittest/env.h>

#include <util/folder/tempdir.h>
#include <util/generic/maybe.h>
#include <util/generic/string.h>
#include <util/stream/mem.h>
#include <util/system/fs.h>

#include <functional>

namespace NMarket {

using TQueueItem = TQueueIterator<const IFeedParser::TMsg>::TMsgPtr;
using TQueueItemSelector = std::function<TMaybe<NSc::TValue>(const TQueueItem&)>;

template<class YmlFeedParser>
NSc::TValue RunBlueYmlFeedParser(
    const TString& inputXml,
    const TQueueItemSelector& queueItemSelector,
    const TFeedInfo& feedInfo = GetDefaultBlueFeedInfo()
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
    return actual;
}

template<class YmlFeedParser>
std::pair<NSc::TValue, NMarket::NBlue::CheckResult>  RunBlueYmlFeedParserWithCheckFeed(
        const TString& inputXml,
        const TQueueItemSelector& queueItemSelector,
        const TFeedInfo& feedInfo = GetDefaultBlueFeedInfo()
) {
    TTempDir outputDir;

    NSc::TValue offers;
    NMarket::NBlue::CheckResult checkResult;

    {
        TString failsDumpFile = JoinFsPaths(outputDir.Name(), "check_feed_log_failures.log");
        NMarket::NProcessLog::TGlobalProcessLogRAII<NMarket::NProcessLog::TCheckerProcessLog> checkProcessLog(
                true,
                NMarket::NProcessLog::TProcessLogOptions()
                        .CrashOnFailure(false)
                        .ErrorDumpFile(failsDumpFile)
                        .OutputDir(outputDir.Name())
        );

        offers = RunBlueYmlFeedParser<YmlFeedParser>(inputXml, queueItemSelector, feedInfo);
    }

    const auto& processLog = NMarket::NProcessLog::TCheckerProcessLog::Instance();
    TString outputFile = processLog.GetOutputFileName();

    if (NFs::Exists(outputFile)) {
        NMarket::TSnappyProtoReader reader(outputFile, processLog.GetOutputMagic());
        reader.Load(checkResult);
    }

    return {offers, checkResult};
}

}  //  namespace NMarket

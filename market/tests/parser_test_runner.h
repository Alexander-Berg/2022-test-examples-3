#pragma once

#include <market/idx/feeds/qparser/inc/feed_info.h>
#include <market/idx/feeds/qparser/inc/feed_parser.h>
#include <market/idx/feeds/qparser/inc/queue_iterator.h>
#include <market/idx/feeds/qparser/lib/logger.h>
#include <market/idx/feeds/qparser/lib/price_calculator.h>
#include <market/idx/feeds/qparser/lib/uniq.h>

#include <market/library/currency_exchange/currency_exchange.h>
#include <market/library/process_log/checker_process_log.h>
#include <market/library/process_log/process_log.h>

#include <library/cpp/scheme/scheme.h>
#include <library/cpp/testing/common/env.h>

#include <util/folder/tempdir.h>
#include <util/generic/maybe.h>
#include <util/system/fs.h>

namespace NMarket {

using TQueueItem = TQueueIterator<const IFeedParser::TMsg>::TMsgPtr;
using TQueueItemSelector = std::function<TMaybe<NSc::TValue>(const TQueueItem&)>;

template <typename TFeedParser>
THolder<IFeedParser> CreateFeedParser(const TString& inputData, const TFeedInfo& feedInfo) {
    THolder<IInputStream> feed = MakeHolder<TMemoryInput>(inputData);
    THolder<IFeedParser> parser = MakeHolder<TFeedParser>(std::move(feed), feedInfo);
    return parser;
}

template <typename TFeedParser>
NSc::TValue RunFeedParser(
    const TString& inputData,
    const TQueueItemSelector& queueItemSelector,
    const TFeedInfo& feedInfo
) {
    if (!CEXCHANGE.IsLoaded()) {
        CEXCHANGE.Load(SRC_("data/currency_rates.xml"));
    }

    THolder<IInputStream> feed = MakeHolder<TMemoryInput>(inputData);
    auto parser = CreateFeedParser<TFeedParser>(inputData, feedInfo);

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

template <typename TFeedParser>
NSc::TValue RunFeedParserWithTrace(
    const TString& inputData,
    const TQueueItemSelector& queueItemSelector,
    const TFeedInfo& feedInfo,
    const TString& offerTraceLogFilename
) {
    NSc::TValue result;

    TRACELOG = MakeHolder<TOffersTraceLog>(offerTraceLogFilename, "", "", true);
    result = RunFeedParser<TFeedParser>(inputData, queueItemSelector, feedInfo);
    TRACELOG.Reset();

    return result;
}

template <typename TFeedParser>
std::pair<NSc::TValue, NMarket::NBlue::CheckResult> RunFeedParserWithCheckFeed(
        const TString& inputData,
        const TQueueItemSelector& queueItemSelector,
        const TFeedInfo& feedInfo
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
        offers = RunFeedParser<TFeedParser>(inputData, queueItemSelector, feedInfo);
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

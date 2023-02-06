#pragma once

#include "test_utils.h"

#include <market/idx/feeds/qparser/inc/feed_parser.h>
#include <market/idx/feeds/qparser/inc/queue_iterator.h>

#include <library/cpp/scheme/scheme.h>
#include <library/cpp/testing/common/env.h>

namespace NMarket {

using TQueueItem = TQueueIterator<const IFeedParser::TMsg>::TMsgPtr;
using TQueueItemSelector = std::function<TMaybe<NSc::TValue>(const TQueueItem&)>;

template<class RssFeedParser>
NSc::TValue RunRssFeedParser(
    const TString& inputXml,
    const TQueueItemSelector& queueItemSelector,
    const TFeedInfo& feedInfo = GetDefaultWhiteFeedInfo(EFeedType::RSS)
) {
    THolder<IFeedParser> feedParser;
    return RunRssFeedParser<RssFeedParser>(inputXml, queueItemSelector, feedParser, feedInfo);
}

template<class RssFeedParser>
NSc::TValue RunRssFeedParser(
    const TString& inputXml,
    const TQueueItemSelector& queueItemSelector,
    THolder<IFeedParser>& feedParser,
    const TFeedInfo& feedInfo = GetDefaultWhiteFeedInfo(EFeedType::RSS)
) {
    if (!CEXCHANGE.IsLoaded()) {
        CEXCHANGE.Load(SRC_("data/currency_rates.xml"));
    }

    THolder<IInputStream> feed = MakeHolder<TMemoryInput>(inputXml);
    THolder<IFeedParser> parser = MakeHolder<RssFeedParser>(std::move(feed), feedInfo);

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

}

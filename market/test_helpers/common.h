#pragma once
#include <util/generic/string.h>
#include <util/generic/maybe.h>
#include <util/generic/deque.h>
#include <util/generic/vector.h>
#include <util/folder/path.h>
#include <market/proto/indexer/qpipe.pb.h>
#include <market/proto/indexer/GenerationLog.pb.h>
#include <market/library/interface/indexer_report_interface.h>

using TFeedId = uint32_t;
using TOfferId = TString;
using TMarketSku = uint64_t;
using TDataSource = NMarket::NQPipe::DataSource;
using TTimestamp = uint32_t;
using TQPipeRecord = NMarket::NQPipe::Offer;
using TQPipeData = NMarket::NQPipe::Data;
using TQIdxRecord = MarketIndexer::GenerationLog::Record;

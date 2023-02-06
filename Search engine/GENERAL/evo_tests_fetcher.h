#pragma once

#include "deep_uniproxy_fetcher.h"
#include "util/system/tempfile.h"

#include <alice/uniproxy/mapper/fetcher/lib/protos/voice_input.pb.h>

#include <alice/uniproxy/mapper/library/flags/container.h>
#include <alice/uniproxy/mapper/library/logging/logging.h>
#include <alice/uniproxy/mapper/fetcher/lib/request.h>

#include <library/cpp/logger/filter.h>
#include <library/cpp/logger/stream.h>
#include <library/cpp/protobuf/yt/proto2yt.h>
#include <library/cpp/protobuf/yt/yt2proto.h>
#include <library/cpp/timezone_conversion/civil.h>
#include <yt/yt/core/logging/log.h>

namespace NScraperOverYT {
    class TWrongEvoRequest: public yexception {};
    class TEmptyUniproxyResponse: public yexception {};
    class TEvoTestsBinaryFailed: public yexception {};

    class TEvoTestsFetcher: public TDeepUniproxyFetcher {
    private:
        void DebugRequest(const TMaybe<NYT::TNode>& requestEVO, const char requestMapper[]);
        void DebugFile(const TString& fileName);
        void DebugEvoTestsEnd();
        void AddEvoTestsFilter(TStringBuilder& evoTestsFilters, const NYT::TNode& row) const;
        void StartEvoTests(const TString& filters);
        ui64 BuildEvoTestsBinary(NYT::TTableReader<NYT::TNode>* input);
        void ConstructUniproxySettings(TMaybe<NAlice::NUniproxy::NFetcher::TUniproxyRequestPerformer>& performer, const TMaybe<NYT::TNode>& request);
        TMaybe<NYT::TNode> GetUniproxyClientRespose(const TMaybe<NYT::TNode>& requestEVO, NAlice::NUniproxy::NFetcher::TUniproxyRequestPerformer& performer);
        TString ParseEvoRequest(const TMaybe<NYT::TNode>& requestEVO, TMaybe<NAlice::NUniproxy::NFetcher::TUniproxyRequestPerformer>& performer,
                                TRowHandleResult& handleResult, const TString& basket);

    public:
        TEvoTestsFetcher(const TFetcherCommonParams& commonParams, const TString& resolveTablePath, const TString& args);
        // c.tor for unit tests
        TEvoTestsFetcher(const TUniproxyFetcherInputParams& params);

        void BuildEvoTestsBinaryTesting(const NYT::TNode& rows, bool oneRow);
        TRowHandleResult ProcessRequests(const NYT::TNode& row);
        void Do(NYT::TTableReader<NYT::TNode>* input, NYT::TTableWriter<TFetchResult>* output) override;
    };

}

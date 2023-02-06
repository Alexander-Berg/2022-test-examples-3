#pragma once

#include "options.h"
#include "generate_requests.h"

#include <search/tools/test_shard/proto/query.pb.h>

#include <search/session/compression/report.h>

#include <library/cpp/progress_bar/progress_bar.h>

#include <library/cpp/deprecated/atomic/atomic.h>
#include <util/thread/pool.h>

namespace NTestShard {

class TOptions;

using TDocHash = ui64;

struct TResponse {
    ui64 SummaryDuration = 0;
    ui32 SuccessRequests = 0;
    NMetaProtocol::TReport Report;
};

class TGunslinger {
public:
    TGunslinger(TOptions& opts);

    void Start();
    void Join();
    void Validate();

    void MutateQueries();

    void PrintTimes();

private:
    void LoadQueries(TOptions& opts);
    void LoadQueries(const NProto::TQueryVector& queries);

    void Worker(ui32 begin, ui32 end);
    TString GetDocInfo(const TString& zhash);

private:
    THolder<NProgressBar::TProgressBar<ui64>> Bar_;
    TAtomic Progress_ = 0;

    THolder<TRequestsGenerator> Generator_;
    TString Address_;
    THolder<IThreadPool> Requesters_;
    TVector<TQuery> Queries_;
    TVector<TResponse> Responses_;
    ui32 MaxThreads_ = 0;
    ui32 MaxRetries_ = 10;
    ui32 Repeats_ = 10;
};

}

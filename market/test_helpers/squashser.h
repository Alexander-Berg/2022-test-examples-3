#pragma once

#include "market/qpipe/prices/test_helpers/common.h"
#include "market/qpipe/prices/test_helpers/qpipe_delta.h"
#include "market/qpipe/prices/test_helpers/qidx_delta.h"
#include <market/qpipe/qindex/test_util/common.h>

class TPricesSquasher
{
public:
    TPricesSquasher(const TString& binPath, const TString& workingDir, const TVector<TQPipeDelta>& apiSources, size_t nshards)
        : BinPath(binPath)
        , WorkingDir(workingDir)
        , ApiSources(apiSources)
        , NShards(nshards)
    {
    }

    void Launch()
    {
        if (ApiSources.empty())
            throw yexception() << "empty api sources";

        const auto inputDeltaPath = [&](size_t n) -> TString {
            return JoinFsPaths(WorkingDir, "squasher-input-delta-" + ToString(n) + ".pbuf.sn");
        };

        const auto outputShardPath = [&](size_t n) -> TString {
            return JoinFsPaths(WorkingDir, "squasher-output-delta-shard-" + ToString(n) + ".pbuf.sn");
        };

        TList<TString> args;
        args.push_back("--shards");  args.push_back(ToString(NShards));
        for (size_t i = 0; i < NShards; ++i) {
            args.push_back(outputShardPath(i));
        }

        for (size_t i = 0; i < ApiSources.size(); ++i) {
            ApiSources[i].Save(inputDeltaPath(i));
            args.push_back(inputDeltaPath(i));
        }

        RunCmd(BinPath, args);

        OutputDeltaShards.reserve(NShards);
        OutputDeltaShards.clear();
        for (size_t i = 0; i < NShards; ++i) {
            OutputDeltaShards.push_back(TQPipeDelta(outputShardPath(i)));
        }
    }

    const TVector<TQPipeDelta>& GetOutputDeltaShards() const {return OutputDeltaShards;}

private:
    const TString BinPath;
    const TString WorkingDir;
    const TVector<TQPipeDelta> ApiSources;
    const size_t NShards;

    TVector<TQPipeDelta> OutputDeltaShards;
};


#pragma once

#include "market/qpipe/prices/test_helpers/common.h"
#include "market/qpipe/prices/test_helpers/qpipe_delta.h"
#include "market/qpipe/prices/test_helpers/qidx_delta.h"
#include <market/qpipe/qindex/test_util/common.h>

class TPricesMerger
{
public:
    TPricesMerger(const TString& binPath, const TString& workingDir, const TQIdxDelta& feedSource, const TQPipeDelta& apiSource)
        : BinPath(binPath)
        , WorkingDir(workingDir)
        , FeedSource(feedSource)
        , ApiSource(apiSource)
    {
    }

    void Launch()
    {
        const TString deltaFeedPath = JoinFsPaths(WorkingDir, "merger-input-delta-feed.pbuf.sn");
        const TString deltaApiPath = JoinFsPaths(WorkingDir, "merger-input-delta-api.pbuf.sn");
        const TString deltaMergedPath = JoinFsPaths(WorkingDir, "merger-output-delta.pbuf.sn");

        FeedSource.Save(deltaFeedPath);
        ApiSource.Save(deltaApiPath);

        TList<TString> args;
        args.push_back("--feed");   args.push_back(deltaFeedPath);
        args.push_back("--api");    args.push_back(deltaApiPath);
        args.push_back("--output"); args.push_back(deltaMergedPath);

        RunCmd(BinPath, args);

        OutputDelta.Load(deltaMergedPath);
    }

    const TQPipeDelta& GetOutputDelta() const {return OutputDelta;}

private:
    const TString BinPath;
    const TString WorkingDir;
    const TQIdxDelta FeedSource;
    const TQPipeDelta ApiSource;

    TQPipeDelta OutputDelta;
};


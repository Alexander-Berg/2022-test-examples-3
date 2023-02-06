#pragma once

#include "options.h"
#include <util/generic/string.h>

namespace NTestShard {

struct TPrsOptions {
    bool SkipPrs = false;

    EStage Stage = EStage::Search;

    TString MiddleParams;
    TString QueriesFile;
    TString Upper;

    TOutputStreamByTier Output;
};

int RunPRS(const TPrsOptions& options);

}

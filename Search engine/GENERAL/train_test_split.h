#pragma once

#include <library/cpp/getopt/last_getopt.h>

#include <util/generic/string.h>

namespace NItdItp {

struct TTrainTestSplitOptions {
    TString YtCluster;
    TString PoolInput;
    TString TrainPoolOutput;
    TString TestPoolOutput;
    TString RandomFeature;
    float TestSizePercent;

    TTrainTestSplitOptions(int argc, const char* argv[]);
};

int TrainTestSplit(int argc, const char* argv[]);

} // namespace NItdItp

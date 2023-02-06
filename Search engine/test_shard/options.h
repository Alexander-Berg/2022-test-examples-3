#pragma once

#include <search/tools/test_shard/common/shard_info.h>
#include <search/tools/test_shard/proto/config.pb.h>

#include <quality/query_pool/prs_ops/lib/settings.h>

#include <library/cpp/getopt/last_getopt.h>

#include <util/generic/string.h>
#include <util/system/info.h>

namespace NTestShard {

enum class EMode : ui8 {
    Default,
    GenerateRequests,
    KillBasesearch,
    SaveRequests,
    Validate,
};

enum class EOutputFormat : ui8 {
    Tsv,
    Plain,
};

enum EStage : ui8 {
    None   = 0b00,
    Search = 0b01,
    Factor = 0b10,
    SearchAndFactor = Search | Factor,
};

class TOptions {
public:
    EMode Mode = EMode::Default;
    EStage Stage = EStage::Search;
    EOutputFormat OutputFormat = EOutputFormat::Tsv;
    TString QueriesFile;
    TString Upper;
    TString OutputDir;
    TString MiddleSearchParams;
    TString BaseSearchParams;
    TString Host;
    TString GeneratedQueriesPath;
    TString WizardedQueriesPath;
    TString Cache;
    ui16 Port = 0;
    ui32 RequestsCount = 10;
    ui32 MaxThreads = NSystemInfo::NumberOfCpus();
    ui32 ShootingRepeats = 100;
    ui32 DocGroups = 150;
    bool NoPrs = false;
    THashMap<EShardTier, TShardInfo> Shards;
    TVector<TString> Suppressors;
    NProto::TConfig Config;

public:
    TOptions() = default;
    TOptions(int argc, const char* argv[]);

    void PrintHelp();
    const TShardInfo& FirstShard() const;

private:
    NLastGetopt::TOpts Opts_;
};

}

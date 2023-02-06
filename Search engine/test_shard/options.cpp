#include "options.h"

#include <search/tools/test_shard/common/shard_info.h>

#include <util/string/vector.h>
#include <util/system/fs.h>

#include <library/cpp/getopt/last_getopt.h>
#include <library/cpp/resource/resource.h>

#include <google/protobuf/text_format.h>

namespace NTestShard {

TOptions::TOptions(int argc, const char* argv[]) {
    TString configFile;
    Opts_.SetTitle("Test shard for basesearch");
    Opts_
        .AddLongOption("save-requests", "Save basesearch requests")
        .NoArgument()
        .StoreValue(&Mode, EMode::SaveRequests);
    Opts_
        .AddLongOption("generate-requests", "Generate requests for basesearch")
        .NoArgument()
        .StoreValue(&Mode, EMode::GenerateRequests);
    Opts_
        .AddLongOption("validate", "Validate basesearch answers")
        .NoArgument()
        .StoreValue(&Mode, EMode::Validate);
    Opts_
        .AddLongOption("kill-basesearch", "Kill basesearch")
        .NoArgument()
        .StoreValue(&Mode, EMode::KillBasesearch);
    Opts_
        .AddLongOption('q', "queries", "Path to queries file")
        .Optional()
        .RequiredArgument("FILE")
        .DefaultValue("none")
        .StoreResult(&QueriesFile);
    Opts_
        .AddLongOption('u', "upper", "Upper server address")
        .RequiredArgument("ADDRESS")
        .DefaultValue("hamster.yandex.ru")
        .StoreResult(&Upper);
    Opts_
        .AddLongOption("host", "Address of running basesearch instance")
        .RequiredArgument("ADDRESS")
        .DefaultValue("127.0.0.1")
        .StoreResult(&Host);
    Opts_
        .AddLongOption('p', "port", "Port of running basesearch instance")
        .Optional()
        .RequiredArgument("PORT")
        .DefaultValue(0)
        .StoreResult(&Port);
    Opts_
        .AddLongOption("no-prs", "Do not collect wizardings for debug purposes")
        .Optional()
        .NoArgument()
        .StoreValue(&NoPrs, true);
    Opts_
        .AddLongOption('o', "output", "Path to output dir")
        .Optional()
        .RequiredArgument("DIR")
        .DefaultValue(".")
        .StoreMappedResultT<TStringBuf>(&OutputDir, [](const TStringBuf val) {
            TFsPath path = val;
            if (path.IsRelative()) {
                path = NFs::CurrentWorkingDirectory() / path;
            }
            return path;
        });
    Opts_
        .AddLongOption("output-format", "Output format, tsv or plain")
        .Optional()
        .RequiredArgument("FORMAT")
        .DefaultValue("tsv")
        .StoreMappedResultT<TStringBuf>(&OutputFormat, [](const TStringBuf val) {
            if (val == "tsv") {
                return EOutputFormat::Tsv;
            } else {
                return EOutputFormat::Plain;
            }
        });
    Opts_
        .AddLongOption('m', "middlesearch-params", "Additional CGI params for middlesearch")
        .Optional()
        .OptionalArgument("PARAMS")
        .DefaultValue("")
        .StoreResult(&MiddleSearchParams);
    Opts_
        .AddLongOption('b', "basesearch-params", "Additional CGI params for basesearch")
        .Optional()
        .OptionalArgument("PARAMS")
        .DefaultValue("")
        .StoreResult(&BaseSearchParams);
    Opts_
        .AddLongOption('s', "shard", "Shard directory")
        .Optional()
        .RequiredArgument("DIR")
        .Handler1T<TString>([this](const TString& str) {
            TShardInfo info(str);
            EShardTier tier = info.GetTier();
            Shards.emplace(tier, std::move(info));
        });
    Opts_
        .AddLongOption('n', "number", "Number of requests to generate")
        .Optional()
        .RequiredArgument("NUM")
        .DefaultValue(10)
        .StoreResult(&RequestsCount);
    Opts_
        .AddLongOption('t', "threads", "Maximum number of threads")
        .Optional()
        .RequiredArgument("NUM")
        .DefaultValue(NSystemInfo::NumberOfCpus())
        .StoreResult(&MaxThreads);
    Opts_
        .AddLongOption("repeats", "Number of repeats in killing mode")
        .Optional()
        .RequiredArgument("NUM")
        .DefaultValue(100)
        .StoreResult(&ShootingRepeats);
    Opts_
        .AddLongOption("doc-groups", "Number of doc groups")
        .Optional()
        .RequiredArgument("NUM")
        .DefaultValue(150)
        .StoreResult(&DocGroups);
    Opts_
        .AddLongOption("save-generated-queries", "Save plain queries generated from shard")
        .Optional()
        .OptionalArgument("FILE")
        .DefaultValue("")
        .StoreResult(&GeneratedQueriesPath);
    Opts_
        .AddLongOption("save-wizarded-queries", "Save wizarded queries")
        .Optional()
        .OptionalArgument("FILE")
        .DefaultValue("")
        .StoreResult(&WizardedQueriesPath);
    Opts_
        .AddLongOption('c', "config", "Config for attribute requests")
        .Optional()
        .OptionalArgument("FILE")
        .DefaultValue("")
        .StoreResult(&configFile);
    Opts_
        .AddLongOption('f', "filters", "Names of filters to apply, see filters/*")
        .Optional()
        .RequiredArgument("FILTERS")
        .DefaultValue("")
        .SplitHandler(&Suppressors, ',');
    Opts_
        .AddLongOption("stage", "Search or Factor with comma as delimiter")
        .Optional()
        .RequiredArgument("STAGE")
        .DefaultValue("Search")
        .StoreMappedResultT<TStringBuf>(&Stage, [](TStringBuf val) {
            TVector<TString> stages = SplitString(val.data(), val.size(), ",");
            unsigned char result = 0;
            for (const TString& s : stages) {
                if (s == "Search") {
                    result |= static_cast<unsigned char>(EStage::Search);
                } else if (s == "Factor") {
                    result |= static_cast<unsigned char>(EStage::Factor);
                }
            }
            return static_cast<EStage>(result);
        });
    Opts_
        .AddLongOption("cache", "Cache directory for inverse index")
        .Optional()
        .OptionalArgument("DIR")
        .DefaultValue("")
        .StoreResult(&Cache);

    Opts_.AddHelpOption('h');
    NLastGetopt::TOptsParseResult(&Opts_, argc, argv);

    TString serializedConfig;
    if (!configFile.empty()) {
        TFileInput fin(configFile);
        serializedConfig = fin.ReadAll();
    } else {
        serializedConfig = NResource::Find("config");
    }
    if (!google::protobuf::TextFormat::ParseFromString(serializedConfig, &Config)) {
        throw yexception() << "Cannot parse config";
    }
}

const TShardInfo& TOptions::FirstShard() const {
    Y_ENSURE(!Shards.empty());
    return Shards.begin()->second;
}

void TOptions::PrintHelp() {
    Opts_.PrintUsage("test_shard");
}

}

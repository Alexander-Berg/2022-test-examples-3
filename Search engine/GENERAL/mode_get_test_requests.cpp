#include <mapreduce/yt/client/client.h>
#include <quality/ytlib/tools/nodeio/transform.h>
#include <library/cpp/getopt/last_getopt.h>
#include <mapreduce/yt/common/helpers.h>
#include <mapreduce/yt/common/node_visitor.h>
#include <library/cpp/yson/writer.h>
#include <library/cpp/yson/json/json_writer.h>
#include <util/string/join.h>

struct TGetReqsConfig {
    bool ForceFormatForJson = true;
    THashSet<TString> AllowedSources;

    TString SearchLogsYtPrefix = "//home/apphost_event_log_filter/1h";
    TString LogsSuffix = "WEB/begemot-workers/input-dumps_success";
    TString Cluster = "hahn";
    TMaybe<TString> ExactPath;

    size_t LinesToSave = 1000;

    void Parse(int argc, const char** argv) {
        NLastGetopt::TOpts opts;
        opts.SetFreeArgsMax(0);

        opts.AddLongOption('e', "exact", "exact path")
            .Optional()
            .StoreResultT<TString>(&ExactPath);

        opts.AddLongOption('n', "num", "number of lines to save")
            .Optional()
            .StoreResult(&LinesToSave)
            .DefaultValue(LinesToSave);

        opts.AddLongOption('s', "source", "allowed source type")
            .Required()
            .InsertTo(&AllowedSources);

        NLastGetopt::TOptsParseResult(&opts, argc, argv);
    }
};

TString ToJsonStringNonUtf8(const NYT::TNode& n) {
    TStringStream stream;
    {
        NYT::TJsonWriter writer(&stream,
            NJson::TJsonWriterConfig{}.SetValidateUtf8(false),
            NYson::EYsonType::Node);
        NYT::TNodeVisitor visitor(&writer);
        visitor.Visit(n);
    }
    return stream.Str();
}

int mode_get_requests(int argc, const char** argv) {
    TGetReqsConfig config;
    config.Parse(argc, argv);

    auto cl = NYT::CreateClient(config.Cluster);
    TVector<TString> resultPaths;
    if (config.ExactPath) {
        resultPaths = {{ *config.ExactPath }};
    } else {
        TVector<TString> paths;
        auto rawPaths = cl->List(config.SearchLogsYtPrefix);
        for(auto& p : rawPaths) {
            paths.push_back(p.AsString());
            if (paths.back().Contains("tmp")) {
                paths.pop_back();
            }
        };
        Sort(paths, std::greater<TString>());
        resultPaths.resize(paths.size());
        Transform(
            paths.begin(),
            paths.end(),
            resultPaths.begin(),
            [&](const TString& path) { return Join("/", config.SearchLogsYtPrefix, path, config.LogsSuffix); }
        );
    }

    size_t writtenNum = 0;
    THashMap<TString, ui32> stats;
    for (const auto& path : resultPaths) {
        Cerr << "will read: " << path << Endl;
        if (!cl->Exists(path)) {
            Cerr << TString{path}.Quote() << "does not exist" << Endl;
            continue;
        }
        for (auto iter = cl->CreateTableReader<NYT::TNode>(path); iter->IsValid(); iter->Next()) {
            const NYT::TNode& row = iter->GetRow();
            if (!EqualToOneOf(row["event_name"], "TInputDump", "TSourceRequest")) {
                continue;
            }

            if (!row["dict"]["Source"].IsString()) {
                continue;
            }
            TString sourceType = row["dict"]["Source"].AsString();
            if (!config.AllowedSources.contains(sourceType)) {
                continue;
            }
            NYT::TNode data = row["dict"]["Data"]["answers"];
            if (!data.IsList()) {
                continue;
            }

            if (config.ForceFormatForJson) {
                for(auto& n : data.AsList()) {
                    auto results = n;
                    results.AsMap().erase("source");
                    auto source = n["source"];
                    n.Clear();
                    n["results"] = results;
                    n["name"] = source;

                    if (n["name"] == "BEGEMOT_CONFIG") {
                        if (n["results"].HasKey("binary") && n["results"]["binary"].HasKey("binary")) {
                            n["results"]["binary"]["binary"] = false;
                        }
                    }
                }
            }

            stats[sourceType] += 1;
            Cout << ToJsonStringNonUtf8(data) << "\n";
            writtenNum += 1;
            if (writtenNum >= config.LinesToSave) {
                break;
            }
        }

        if (writtenNum >= config.LinesToSave) {
            break;
        }
    }
    Cerr << "-I- written " << writtenNum << " rows" << Endl;
    for(auto [k,v] : stats) {
        Cerr << "-I- written for " << k << ": " << v << " rows" << Endl;
    }
    return 0;
}

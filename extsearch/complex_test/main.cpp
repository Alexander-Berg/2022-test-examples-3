#include <extsearch/images/robot/library/logger/linklogger.h>
#include <extsearch/video/kernel/static_factors/proto/static_factors.pb.h>
#include <yweb/video/mediadb/mediaplanner/records.h>
#include <yweb/video/mr_protos/index.pb.h>
#include <yweb/video/protos/planner.pb.h>
#include <yweb/video/protos/relate.pb.h>
#include <extsearch/video/kernel/protobuf/reader.h>
#include <extsearch/video/kernel/protobuf/writer.h>
#include <yweb/video/util/util.h>

#include <kernel/indexann/protos/data.pb.h>

#include <library/cpp/getopt/last_getopt.h>
#include <mapreduce/lib/all.h>

#include <util/folder/path.h>
#include <util/generic/map.h>
#include <util/generic/set.h>
#include <util/stream/file.h>
#include <util/string/vector.h>
#include <library/cpp/string_utils/url/url.h>

using namespace NLastGetopt;
using namespace NMediaDB;
using namespace NMR;
using namespace NVideo;

namespace {
    class TLoggerStream : public TStringStream {
    public:
        ~TLoggerStream() override {
            TLinkLogger::LogMessage(Str().data());
        }
    };

    struct TOptions {
        TString Server;
        TFsPath DataPath;
        TFsPath IndexRoot;
        TFsPath StaticFactorsRoot;
        TFsPath UrlbaseRoot;
        TFsPath IndexannRoot;
        TFsPath VegasRoot;
        size_t ClusterCount = 0;
        bool Verbose = false;
    };

    void GetOptions(int argc, const char **argv, TOptions &options) {
        TOpts opt;
        opt.AddLongOption('s', "server")
                .Required()
                .RequiredArgument()
                .StoreResult(&options.Server);
        opt.AddLongOption("data-path")
                .Required()
                .RequiredArgument()
                .StoreResult(&options.DataPath);
        opt.AddLongOption("index-root")
                .Required()
                .RequiredArgument()
                .StoreResult(&options.IndexRoot);
        opt.AddLongOption("sf-root")
                .DefaultValue("static_factors")
                .RequiredArgument()
                .StoreResult(&options.StaticFactorsRoot);
        opt.AddLongOption("urlbase-root")
                .DefaultValue("urlbase")
                .RequiredArgument()
                .StoreResult(&options.UrlbaseRoot);
        opt.AddLongOption("indexann-root")
                .DefaultValue("indexann")
                .RequiredArgument()
                .StoreResult(&options.IndexannRoot);
        opt.AddLongOption("vegas-root")
                .DefaultValue("vegas")
                .RequiredArgument()
                .StoreResult(&options.VegasRoot);
        opt.AddLongOption("clusters")
                .DefaultValue("252")
                .RequiredArgument()
                .StoreResult(&options.ClusterCount);
        opt.AddLongOption("verbose")
                .NoArgument()
                .SetFlag(&options.Verbose);
        TOptsParseResult optRes(&opt, argc, argv);
    }

    struct TDocIdInfo {
        ui64 GlobalId = 0;
        ui64 ClusterId = 0;
        ui64 Yid = 0;

        void SetId(size_t clusterCount, TStringBuf id) {
            TStringBuf cluster = id.NextTok(';');
            ClusterId = FromString<ui64>(cluster);
            Yid = FromString<ui64>(id);
            GlobalId = MRIndexDocIdToGlobalId(Yid, ClusterId, clusterCount);
        }
        void SetId(size_t clusterCount, ui64 globalId) {
            GlobalId = globalId;
            ClusterId = MRIndexGlobalIdToCluster(globalId, clusterCount);
            Yid = MRIndexGlobalIdToDocId(globalId, clusterCount);
        }
    };

    struct TDocInfo {
        TString Url;
        TDocIdInfo DbId;
        TDocIdInfo TestId;
        TSet<TString> MediaUrls;
    };

    class TLoader {
    public:
        TLoader(const TOptions &options)
                : Options(options)
                , Server(options.Server) {
            SaveTimestamp();
            GetDocIds();
        }

        void LoadRelatedItems() {
            TLoggerGuard logger("LoadRelatedItems");
            LoadProtoTable<NRelDupMerge::TRelateUrls>(
                    "index/input/related_items",
                    "related_items");
        }

        void LoadIndexAnnInitialTable() {
            TLoggerGuard logger("LoadIndexAnnInitialTable");
            const TString relTableName = "indexann";
            const TString relFileName = "indexann";

            TClient client(Server);
            TTable table(client, Options.IndexannRoot / relTableName);

            TUnbufferedFileOutput output (Options.DataPath / relFileName);

            for (const auto &key : Keys) {
                TString dbKey = key.Url;
                WriteTableRows<NIndexAnn::TIndexAnnSiteData>(
                        table,
                        dbKey,
                        dbKey,
                        output);
            }
        }

        void LoadHostFactors() {
            TLoggerGuard logger("LoadHostFactors");
            TSet<TString> hosts;
            for (const auto &key : Keys) {
                hosts.insert(TString(GetOnlyHost(key.Url)));
            }

            TClient client(Server);
            TTable table(client, Options.StaticFactorsRoot / "host_factors/current");
            TUnbufferedFileOutput output (Options.DataPath / "hostfactors");

            for (const auto &host : hosts) {
                WriteTableRows<NVideoStaticFactors::TStaticFactors>(
                        table,
                        host,
                        host,
                        output);
            }
        }

        void LoadUrlFactors() {
            TLoggerGuard logger("LoadUrlFactors");

            TClient client(Server);
            TTable table(client, Options.StaticFactorsRoot / "current");
            TUnbufferedFileOutput output (Options.DataPath / "urlfactors");

            for (const auto &key : Keys) {
                if (Options.Verbose) {
                    TLoggerStream stream;
                    stream << "Getting url factors for url " << key.Url;
                }
                for (const auto &url : key.MediaUrls) {
                    WriteTableRows<NVideoStaticFactors::TStaticFactors>(
                            table,
                            url,
                            url,
                            output);
                }
            }
        }

        void LoadUrl2Author() {
            TLoggerGuard logger("LoadUrl2Author");

            TClient client(Server);
            TTable table(client, Options.VegasRoot / "url2author");
            TUnbufferedFileOutput output (Options.DataPath / "url2author");

            for (const auto &key : Keys) {
                if (Options.Verbose) {
                    TLoggerStream stream;
                    stream << "Getting url2author for url " << key.Url;
                }
                for (const auto &url : key.MediaUrls) {
                    auto iterator = table.Find(url);
                    if (Options.Verbose) {
                        TLoggerStream stream;
                        stream << url << ": data " << (iterator.IsValid() ? "found" : "not found");
                    }
                    for ( ; iterator.IsValid(); ++iterator) {
                        TStringBuf author = iterator.GetValue().AsStringBuf();
                        AllAuthors.insert(TString(author));
                        if (Options.Verbose) {
                            TLoggerStream stream;
                            stream << "Got author " << author;
                        }
                        output << url << '\t'
                                << iterator.GetSubKey().AsStringBuf() << '\t'
                                << author << '\n';
                    }
                }
            }
        }

        void LoadAuthorFactors() {
            TLoggerGuard logger("LoadAuthorFactors");
            TClient client(Server);
            TTable table(client, Options.StaticFactorsRoot / "author_factors/current");
            TUnbufferedFileOutput output (Options.DataPath / "authorfactors");

            for (const auto &author : AllAuthors) {
                WriteTableRows<NVideoStaticFactors::TStaticFactors>(
                        table,
                        author,
                        author,
                        output);
            }
        }

        void LoadShardHosts() {
            TLoggerGuard logger("LoadShardHosts");
            TUnbufferedFileOutput output (Options.DataPath / "shardhosts");
            for (const auto &key : Keys) {
                output << key.TestId.ClusterId << ';' << GetOnlyHost(key.Url)
                        << "\tplan\t" << Sprintf("%019lu", key.TestId.Yid) << '\n';
            }
        }

        void LoadMediaPlanKeys() {
            TLoggerGuard logger("LoadMediaPlanKeys");
            TClient client(Server);
            TTable table(client, Options.IndexRoot / "planner/out/plan");

            for (auto &key : Keys) {
                TString docId = ToString(key.DbId.GlobalId);
                if (Options.Verbose) {
                    TLoggerStream stream;
                    stream << "Checking plan for url " << key.Url;
                }
                for (auto iter = table.Find(docId); iter.IsValid(); ++iter) {
                    NMediaDB::TPlannerInput rec;
                    TProtoReader::FromString(iter.GetValue().AsString(), rec);
                    if (Options.Verbose) {
                        TLoggerStream stream;
                        stream << "Found media url " << TString(rec.GetPageUrl().GetValue());
                    }
                    key.MediaUrls.insert(TString(rec.GetPageUrl().GetValue()));
                    AllMediaKeys.insert(TString(rec.GetPageUrl().GetValue()));
                }
            }
        }

        void LoadMediaPlan() {
            TLoggerGuard logger("LoadMediaPlan");
            TClient client(Server);
            TTable table(client, Options.IndexRoot / "planner/out/mediaplan");
            TUnbufferedFileOutput output (Options.DataPath / "mediaplan");
            TUnbufferedFileOutput url2Doc (Options.DataPath / "url2doc");

            for (const auto &key : Keys) {
                if (Options.Verbose) {
                    TLoggerStream stream;
                    stream << "Getting mediaplan for url " << key.Url;
                }
                for (const auto &url : key.MediaUrls) {
                    if (Options.Verbose) {
                        TLoggerStream stream;
                        stream << "Checking url " << url;
                    }
                    for( auto iter = table.Find(url); iter.IsValid(); ++iter ) {
                        TMediaPlanRec planRec(iter.GetValue().AsStringBuf());
                        if (Options.Verbose) {
                            TLoggerStream stream;
                            stream << "Found rec with gid " << planRec.GlobalId
                                    << (planRec.GlobalId == key.DbId.GlobalId ?
                                            ", will keep" :
                                            ", will skip");
                        }
                        if (planRec.GlobalId != key.DbId.GlobalId) {
                            continue;
                        }
                        planRec.GlobalId = key.TestId.GlobalId;
                        output << url << "\t \t" << planRec.ComposeRecord() << '\n';
                        url2Doc << url << "\turl\t"
                                << ToString(key.TestId.ClusterId) << ';'
                                << Sprintf("%019lu", key.TestId.Yid) << '\n';
                    }
                }
            }
        }

        void LoadMediaData() {
            TLoggerGuard logger("LoadMediaData");
            TClient client(Server);
            TTable mediaTable(client, Options.UrlbaseRoot / "prevdata/media");
            TUnbufferedFileOutput mediaOutput (Options.DataPath / "media");
            TTable mediaThumbTable(client, Options.UrlbaseRoot / "prevdata/media.thumb.data");
            TUnbufferedFileOutput mediaThumbOutput (Options.DataPath / "media.thumb.data");

            for (const auto& url : AllMediaKeys) {
                WriteTableRows<TMediaProperties>(
                        mediaTable,
                        url,
                        url,
                        mediaOutput);
                WriteTableRows<TMediaProperties>(
                        mediaThumbTable,
                        url,
                        url,
                        mediaThumbOutput);
            }
        }

    private:
        TOptions Options;
        TServer Server;
        TVector<TDocInfo> Keys;
        TSet<TString> AllMediaKeys;
        TSet<TString> AllAuthors;

        TMap<TString, ui64> LoadUrls() {
            TMap<TString, ui64> result;
            TUnbufferedFileInput input(Options.DataPath / "update_config" / "urls.txt");
            TString line;
            while (input.ReadLine(line)) {
                TStringBuf url(line);
                ui64 testId = FromString<ui64>(url.NextTok('\t'));
                result[TString(url)] = testId;
            }
            return result;
        }

        void SaveTimestamp() {
            TUnbufferedFileOutput tsOutput (Options.DataPath / "timestamp");
            tsOutput << time(nullptr);
        }


        void GetDocIds() {
            TLoggerGuard logger("GetDocIds");

            auto allUrls = LoadUrls();

            TClient client(Server);
            TTable url2doc(client, Options.IndexRoot / "planner/out/url2doc");

            for(const auto& urlData : allUrls) {
                //testId ui64 = urlData.second;
                TStringBuf url = urlData.first;
                auto iterator = url2doc.Find(url);
                if(iterator.IsValid()) {
                    Keys.emplace_back();
                    auto &key = Keys.back();
                    key.Url = url;
                    key.TestId.SetId(1, urlData.second);
                    key.DbId.SetId(Options.ClusterCount, iterator.GetValue().AsStringBuf());
                    if (Options.Verbose) {
                        TLoggerStream stream;
                        stream << "Found docid " << key.DbId.ClusterId << ';' << key.DbId.GlobalId << " for url " << url
                                << "; will add as docid " << key.TestId.ClusterId << ';' << key.TestId.GlobalId;
                    }
                } else {
                    TLoggerStream stream;
                    stream << "Could not find docid for url " << url;
                }
            }
        }

        template<typename TProto>
        void WriteTableRows(
                TTable &table,
                const TString &dbKey,
                const TString &testKey,
                IOutputStream &output) {
            auto iterator = table.Find(dbKey);
            if (Options.Verbose) {
                TLoggerStream stream;
                stream << dbKey << ": data " << (iterator.IsValid() ? "found" : "not found");
            }
            for ( ; iterator.IsValid(); ++iterator) {
                TProto item;
                TProtoReader::FromString(iterator.GetValue().AsString(), item);
                output << testKey << '\t'
                        << iterator.GetSubKey().AsStringBuf() << '\t'
                        << TProtoWriter::ToString(item) << '\n';
            }
        }

        template<typename TProto>
        void LoadProtoTable(
                const TString &relTableName,
                const TString &relFileName) {
            TClient client(Server);
            TTable table(client, Options.IndexRoot / relTableName);

            TUnbufferedFileOutput output (Options.DataPath / relFileName);

            for (const auto &key : Keys) {
                TString dbKey = Sprintf("%019lu", key.DbId.GlobalId);
                if (Options.Verbose) {
                    TLoggerStream stream;
                    stream << key.Url;
                }
                WriteTableRows<TProto>(
                        table,
                        dbKey,
                        Sprintf("%019lu", key.TestId.GlobalId),
                        output);
            }
        }
    };

}

int main(int argc, const char* argv[]) {
    Initialize(argc, argv);

    TOptions options;
    GetOptions(argc, argv, options);

    TLoader loader(options);

    loader.LoadMediaPlanKeys();
    loader.LoadUrl2Author();
    loader.LoadAuthorFactors();
    loader.LoadUrlFactors();
    loader.LoadIndexAnnInitialTable();
    loader.LoadMediaPlan();
    loader.LoadMediaData();
    loader.LoadRelatedItems();
    loader.LoadHostFactors();
    loader.LoadShardHosts();
}

#include "options.h"
#include "prs_ops.h"
#include "save_requests.h"

#include <search/tools/test_shard/common/shard_info.h>
#include <search/tools/test_shard/common/attribute_tree.h>
#include <search/tools/test_shard/idx_read/url_hash.h>
#include <search/tools/test_shard/proto/query.pb.h>

#include <util/folder/path.h>
#include <util/folder/tempdir.h>
#include <util/random/random.h>
#include <util/stream/file.h>
#include <util/stream/null.h>
#include <util/string/vector.h>
#include <util/system/fs.h>
#include <util/system/tempfile.h>

namespace NTestShard {

namespace {

TString DumpQuery(const NProto::TQuery& query) {
    TStringBuilder res;
    res << query.GetInfo().GetQid() << '\t'
        << TAttrSchemeTree(query.GetQuery()).Serialize() << '\t'
        << query.GetInfo().GetRegion() << '\t'
        << query.GetInfo().GetCountry() << '\t'
        << query.GetInfo().GetPlatform();
    return res;
}

using TQidType = ui64;

class TRequest {
public:
    TRequest() = default;

    TRequest(TStringStream& sstream) {
        sstream >> Qid >> Request;
    }

    void AddDH(TString&& zHash) {
        Request += "&dh=" + zHash + ":";
    }

    TQidType GetQid() const {
        return Qid;
    }

    const TString& GetRequest() const {
        return Request;
    }

private:
    TQidType Qid;
    TString Request;
};

class TRequestHeap {
public:
    TRequestHeap() = default;

    void Push(TRequest&& request) {
        Requests_.push_back(std::move(request));
    }

    void Finish(IOutputStream& output, const TFetchOptions& opts, const bool isFactorStage, const EShardTier tier) {
        if (isFactorStage && !Requests_.empty() && !AddRandomDH(opts, tier)) {
            WARNING_LOG << "Error occured while trying to add Z-hashes" << Endl;
        }

        Sort(Requests_.begin(), Requests_.end(), [](const TRequest& lhs, const TRequest& rhs) {
            return lhs.GetQid() < rhs.GetQid();
        });

        for (const TRequest& request : Requests_) {
            if (opts.OutputFormat() == EOutputFormat::Tsv) {
                output << request.GetQid() << '\t' << request.GetRequest() << opts.BaseParams() << Endl;
            } else {
                output << request.GetRequest() << opts.BaseParams() << Endl;
            }
        }
    }

    bool AddRandomDH(const TFetchOptions& opts, const EShardTier tier) {
        Y_ENSURE(opts.HasShard(tier), "Cannot find shard for tier " + ToString(tier));
        TUrlHasher hasher(opts.Shard(tier).GetPath());

        for (TRequest& request : Requests_) {
            request.AddDH(hasher.GetZHash(RandomNumber(hasher.DocCount())));
        }

        return true;
    }

private:
    TVector<TRequest> Requests_;
};

class TTempWorkingDir {
public:
    TTempWorkingDir()
        : WorkingDir_(NFs::CurrentWorkingDirectory())
    {
        NFs::SetCurrentWorkingDirectory(Temp_.Name());
    }

    ~TTempWorkingDir() {
        NFs::SetCurrentWorkingDirectory(WorkingDir_);
    }

    const TString& GetWorkingDir() const {
        return WorkingDir_;
    }

private:
    TTempDir Temp_;
    TString WorkingDir_;
};

void SortRequestsFile(IInputStream& in, IOutputStream& out, const TFetchOptions& opts, const bool isFactorStage, const EShardTier tier) {
    TRequestHeap heap;
    TString buf;
    while (in.ReadLine(buf)) {
        TStringStream sstream(buf);
        TRequest request(sstream);
        heap.Push(std::move(request));
    }
    heap.Finish(out, opts, isFactorStage, tier);
}

void FinalizeWizardedQueries(NProto::TQueryVector& result, const TOptions& opts) {
    /* https://wiki.yandex-team.ru/jandekspoisk/kachestvopoiska/basesearch/groupings/#parametrygruppirovok */
    for (NProto::TQuery& query : *result.MutableQuery()) {
        TStringBuf path;
        TStringBuf cgi = query.GetRequest();
        cgi.Split('?', path, cgi);
        TCgiParameters params(cgi);
        TString g = params.Get("g");
        TVector<TString> tokens = SplitString(g, ".", 0, KEEP_EMPTY_TOKENS);
        if (tokens.size() >= 3) {
            tokens[2] = ToString(opts.DocGroups);
            params.ReplaceUnescaped("g", JoinStrings(tokens, "."));
            query.SetRequest(TString{path} + "?" + params.Print());
        } else {
            WARNING_LOG << "Cannot replace gGroups in wizarded query #" << query.GetInfo().GetQid() << Endl;
        }
    }
}

}

int SaveRequests(TOptions& opts) {
    NFs::MakeDirectory(opts.OutputDir);
    NFs::EnsureExists(opts.OutputDir);

    if ((EStage::Factor & opts.Stage) && opts.Shards.empty()) {
        WARNING_LOG << "No shard specified for factor queries generation" << Endl;
    }

    static const THashMap<EStage, TString> suffixes{
        {EStage::Search, ""},
        {EStage::Factor, ".factor"}
    };
    TOutputByTier out;
    for (const EShardTier tier : GetMainTiers()) {
        for (const auto& p : suffixes) {
            const EStage stage = p.first;
            const TStringBuf suffix = p.second;
            if (stage == EStage::Factor && !opts.Shards.contains(tier)) {
                continue;
            }
            const TString name = ToString(tier) + suffix;
            if (stage & opts.Stage) {
                const TString outputFile = JoinFsPaths(opts.OutputDir, name + ".tsv");
                out.RegisterOutput(name, MakeHolder<TFileOutput>(outputFile));
            } else {
                out.RegisterOutput(name, MakeHolder<TNullOutput>());
            }
        }
    }

    TRequestsFetcher fetcher(opts);
    TFetchOptions fetchOptions(opts);
    fetcher.Fetch(fetchOptions, out);
    return EXIT_SUCCESS;
}

TRequestsFetcher::TRequestsFetcher(const TOptions& opts)
    : Upper_(opts.Upper)
{
}

void TRequestsFetcher::Fetch(const TFetchOptions& opts, const TOutputByTier& output) {
    Y_ENSURE(!opts.Queries().empty(), "Specify queries file with --queries");
    if (opts.Stage() & EStage::Factor) {
        Y_ENSURE(!opts.ShardsEmpty(), "Specify shard for factor stage");
    }

    TTempWorkingDir tmp;

    TPrsOptions prsOpts = FillOptions(opts);
    if (TFsPath(prsOpts.QueriesFile).IsRelative()) {
        prsOpts.QueriesFile = JoinFsPaths(tmp.GetWorkingDir(), prsOpts.QueriesFile);
    }

    THashMap<TString, TSimpleSharedPtr<TStringStream>> prsOutput;
    for (const auto& out : output) {
        const TString tierName = out.first;
        prsOutput[tierName] = MakeSimpleShared<TStringStream>();
        prsOpts.Output[tierName] = prsOutput[tierName];
    }
    int result = RunPRS(prsOpts);
    if (result == EXIT_SUCCESS) {
        for (const auto& out : output) {
            TStringBuf tierStr = out.first;
            TStringBuf stageStr;
            tierStr.TrySplit('.', tierStr, stageStr);
            const EShardTier tier = FromString(tierStr);
            const bool isFactorStage = (stageStr == ".factor");
            SortRequestsFile(*prsOutput[out.first], *out.second, opts, isFactorStage, tier);
        }
    } else {
        Y_ENSURE(false, "prs_ops has failed");
    }
}

void TRequestsFetcher::FillRequests(NProto::TQueryVector& queries, const TOptions& opts) {
    TString queriesFileName;
    THolder<TTempFile> tmp;
    if (opts.GeneratedQueriesPath.empty()) {
        queriesFileName = "plain_queries.tmp.tsv";
        tmp = MakeHolder<TTempFile>(queriesFileName);
    } else {
        queriesFileName = opts.GeneratedQueriesPath;
    }

    TFileOutput file(queriesFileName);
    for (const NProto::TQuery& q : queries.GetQuery()) {
        file << DumpQuery(q) << Endl;
    }
    file.Finish();

    TFetchOptions fetchOpts(queriesFileName, opts.NoPrs);
    TOutputByTier output;
    EShardTier tier = opts.FirstShard().GetTier();
    output.RegisterOutput(ToString(tier), MakeHolder<TStringStream>());
    Fetch(fetchOpts, output);

    TString line;
    auto it = queries.MutableQuery()->begin();
    auto end = queries.MutableQuery()->end();
    TStringStream* stream = static_cast<TStringStream*>(output.GetStream(ToString(tier)));
    while (stream->ReadLine(line)) {
        TVector<TString> tokens = SplitString(line, "\t");
        ui32 qid = FromString<ui32>(tokens[0]);
        while (it != end && it->GetInfo().GetQid() < qid) {
            ++it;
        }
        if (it == end) {
            break;
        }
        it->SetRequest(tokens[1]);
        ++it;
    }
    NProto::TQueryVector result;
    for (const NProto::TQuery& query : queries.GetQuery()) {
        if (!query.GetRequest().empty()) {
            *result.AddQuery() = query;
        }
    }
    FinalizeWizardedQueries(result, opts);
    if (!opts.WizardedQueriesPath.empty()) {
        TFileOutput wizarded(opts.WizardedQueriesPath);
        for (const NProto::TQuery& query : result.GetQuery()) {
            wizarded << query.GetRequest() << Endl;
        }
    }
    queries = std::move(result);
}

TPrsOptions TRequestsFetcher::FillOptions(const TFetchOptions& opts) {
    TPrsOptions result;
    result.QueriesFile = opts.Queries();
    result.SkipPrs = opts.SkipPrs();
    result.Stage = opts.Stage();
    result.MiddleParams = opts.MiddleParams();
    result.Upper = Upper_;
    return result;
}

}

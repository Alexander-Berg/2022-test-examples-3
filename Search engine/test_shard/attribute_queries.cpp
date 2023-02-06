#include "attribute_queries.h"

#include "options.h"
#include "save_requests.h"

#include <search/tools/test_shard/common/attribute_tree.h>
#include <search/tools/test_shard/common/index_cache.h>
#include <search/tools/test_shard/common/memory_usage.h>
#include <search/tools/test_shard/common/random_subset.h>
#include <search/tools/test_shard/proto/query.pb.h>

#include <util/digest/murmur.h>
#include <util/folder/path.h>
#include <util/generic/string.h>
#include <util/random/shuffle.h>
#include <library/cpp/string_utils/quote/quote.h>
#include <util/system/fs.h>
#include <util/system/mem_info.h>
#include <util/thread/pool.h>

namespace NTestShard {

namespace {

TSingleAttribute BuildInverseMap(IIndexIterator* it) {
    TAttributeWithDocs doc;
    TSingleAttribute result;
    while (it->Read(&doc)) {
        const TString attribute = UrlUnescapeRet(doc.Attribute());
        for (TDocId docId : doc.Docs()) {
            result.Add(docId, attribute);
        }
    }
    return result;
}

}

TAttributesQueriesBuilder::TAttributesQueriesBuilder(const TOptions& opts)
    : Factory_(opts)
    , Filter_(opts.Suppressors)
    , Hasher_(opts.FirstShard().GetPath())
    , Shard_(opts.FirstShard())
    , ThreadsCount_(opts.MaxThreads)
{
    if (!opts.Cache.empty()) {
        CacheDir_ = JoinFsPaths(opts.Cache, Shard_.GetName());
    }
    TString inverseIndexConfig;
    auto adder = [&inverseIndexConfig](const auto& field) {
        TString serialized;
        Y_PROTOBUF_SUPPRESS_NODISCARD field.SerializeToString(&serialized);
        inverseIndexConfig += serialized;
    };
    if (opts.Config.HasQuotes()) {
        adder(opts.Config.GetQuotes());
    }
    if (opts.Config.HasAsterisks()) {
        adder(opts.Config.GetAsterisks());
    }
    ConfigHash_ = MurmurHash<ui32>(inverseIndexConfig.data(), inverseIndexConfig.size());
}

void TAttributesQueriesBuilder::PrepareCache(const THashSet<TStringBuf>& attrs) {
    TMemoryUsage usage("Inverse index");
    TIndexCache diskCache(CacheDir_, Shard_.GetDocs(), ConfigHash_);
    TMutex mtx;
    THolder<IThreadPool> cacheBuilders = CreateThreadPool(ThreadsCount_);
    for (TStringBuf attr : attrs) {
        if (Cache_.Has(TString{attr})) {
            continue;
        }
        cacheBuilders->SafeAddFunc([&, attr](){
            const TString attribute = TString{attr};
            TMaybe<TSingleAttribute> inverse = diskCache.Get(TString{attr});
            if (inverse.Empty()) {
                INFO_LOG << "Building inverse table for attribute " << attribute << Endl;

                THolder<IIndexIterator> iter = Factory_.CreateIterator(attribute);
                inverse = BuildInverseMap(iter.Get());

                Y_ENSURE(inverse);
                diskCache.Add(TString{attr}, inverse.GetRef());
            } else {
                INFO_LOG << "Successfully loaded inverse index for " << attribute << " from cache" << Endl;
            }

            with_lock (mtx) {
                Cache_.Add(attribute, std::move(inverse.GetRef()));
            }

            INFO_LOG << "Inverse index for " << attribute << " finished" << Endl;
        });
    }
    cacheBuilders->Stop();
}

void TAttributesQueriesBuilder::Build(ui32 size, const TAttrSchemeTree& attrs, TSafeQueryVector& output) {
    if (!BuildersQueue_) {
        BuildersQueue_ = CreateThreadPool(ThreadsCount_);
    }

    BuildersQueue_->SafeAddFunc([&, this, size](){
        this->Worker(size, attrs, output);
    });
}

void TAttributesQueriesBuilder::Join() {
    if (BuildersQueue_) {
        BuildersQueue_->Stop();
    }
}

void TAttributesQueriesBuilder::Worker(ui32 size, const TAttrSchemeTree& attrs, TSafeQueryVector& output) {
    TString typeStr = attrs.Serialize();

    INFO_LOG << "Generating queries of type &text=" << typeStr << Endl;

    TInverseIndex keyByDoc = attrs.BuildInvIndex(Cache_);

    struct THashFn {
        size_t operator()(const TAttrRefTree& tree) {
            return THash<TString>{}(tree.Serialize());
        }
    };
    THashMap<TAttrRefTree, TVector<ui32>, THashFn> docsByKey;
    size_t filtered = 0;
    for (const auto& p : keyByDoc) {
        if (Filter_.IsPassed(p.second)) {
            docsByKey[p.second].push_back(p.first);
        } else {
            ++filtered;
        }
    }

    INFO_LOG << "Fitered " << filtered << " / " << docsByKey.size() + filtered
             << " queries based on given filters" << Endl;

    TVector<size_t> selection = GetRandomSubset<size_t>(docsByKey.size(), size);
    Sort(selection.begin(), selection.end());
    auto selectionIt = selection.begin();
    size_t qid = 0;
    TVector<TQuery> result;

    for (const auto& p : docsByKey) {
        if (qid == *selectionIt) {
            TQuery query;
            query.MutableInfo()->SetQid(0);
            query.MutableInfo()->SetRegion(213);
            query.MutableInfo()->SetCountry("RU");
            query.MutableInfo()->SetPlatform("desktop");
            *query.MutableQuery() = p.first.ToNode();
            for (ui32 id : p.second) {
                NProto::TDoc* doc = query.AddExpectedDocs();
                doc->SetId(id);
                doc->SetHash(Hasher_.GetHash(id));
            }
            result.push_back(query);
            ++selectionIt;
            if (selectionIt == selection.end()) {
                break;
            }
        }
        ++qid;
    }

    with_lock (output.Lock) {
        TQueryVector& out = *output.Output;
        for (auto& query : result) {
            query.MutableInfo()->SetQid(out.QuerySize() + 1);
            *out.AddQuery() = query;
        }
        ui32 newRequests = result.size();
        if (newRequests < size) {
            WARNING_LOG << "Found " << newRequests << " instead of " << size << " queries of type &text=" << typeStr << Endl;
        } else {
            INFO_LOG << "Found " << newRequests << " queries of type &text=" << typeStr << Endl;
        }
    }
}

}

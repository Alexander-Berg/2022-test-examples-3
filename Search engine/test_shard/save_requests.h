#pragma once

#include "prs_ops.h"

#include <search/tools/test_shard/options.h>

#include <util/generic/hash.h>


namespace NProto {

class TQueryVector;

}

namespace NTestShard {

int SaveRequests(TOptions& opts);

class TOutputByTier {
    using Container = THashMap<TString, THolder<IOutputStream>>;
    using Iterator = typename Container::iterator;
    using ConstIterator = typename Container::const_iterator;

public:
    void RegisterOutput(const TString& tier, THolder<IOutputStream>&& stream) {
        Output_[tier] = std::move(stream);
    }

    IOutputStream* GetStream(const TString& tier) const {
        Y_ENSURE(Output_.contains(tier));
        return Output_.at(tier).Get();
    }

    Iterator begin() {
        return Output_.begin();
    }

    ConstIterator begin() const {
        return Output_.begin();
    }

    Iterator end() {
        return Output_.end();
    }

    ConstIterator end() const {
        return Output_.end();
    }

private:
    Container Output_;
};

class TFetchOptions {
public:
    TFetchOptions(const TOptions& opts)
        : SkipPrs_(opts.NoPrs)
        , Stage_(opts.Stage)
        , OutputFormat_(opts.OutputFormat)
        , Queries_(opts.QueriesFile)
        , MiddleSearchParams_(opts.MiddleSearchParams)
        , BaseSearchParams_(opts.BaseSearchParams)
        , Shards_(opts.Shards)
    {
    }

    TFetchOptions(const TString& queriesFile, bool skipPrs = false)
        : SkipPrs_(skipPrs)
        , Stage_(EStage::Search)
        , OutputFormat_(EOutputFormat::Tsv)
        , Queries_(queriesFile)
    {
    }

    bool SkipPrs() const {
        return SkipPrs_;
    }

    EStage Stage() const {
        return Stage_;
    }

    EOutputFormat OutputFormat() const {
        return OutputFormat_;
    }

    const TString& Queries() const {
        return Queries_;
    }

    const TString& MiddleParams() const {
        return MiddleSearchParams_;
    }

    const TString& BaseParams() const {
        return BaseSearchParams_;
    }

    const TShardInfo& Shard(EShardTier tier) const {
        return Shards_.at(tier);
    }

    bool ShardsEmpty() const {
        return Shards_.empty();
    }

    bool HasShard(EShardTier tier) const {
        return Shards_.contains(tier);
    }

private:
    bool SkipPrs_ = false;
    EStage Stage_ = EStage::Search;
    EOutputFormat OutputFormat_ = EOutputFormat::Tsv;

    TString Queries_;
    TString MiddleSearchParams_;
    TString BaseSearchParams_;

    THashMap<EShardTier, TShardInfo> Shards_;
};

class TRequestsFetcher {
    using TOutputFileByTier = THashMap<TString, THolder<IOutputStream>>;

public:
    TRequestsFetcher(const TOptions& opts);

    void Fetch(const TFetchOptions& opts, const TOutputByTier& output);
    void FillRequests(NProto::TQueryVector& queries, const TOptions& opts);

private:
    TPrsOptions FillOptions(const TFetchOptions& opts);

private:
    TString Upper_;
};

}

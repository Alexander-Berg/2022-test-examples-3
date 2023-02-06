#pragma once

#include <util/generic/hash.h>
#include <util/generic/string.h>
#include <util/generic/vector.h>

enum class EShardTier {
    PlatinumTier0,
    WebTier0,
    WebFreshTier0,
    WebTier1,
    Unknown,
};

inline const TVector<EShardTier>& GetMainTiers() {
    static const TVector<EShardTier> tiers { EShardTier::PlatinumTier0, EShardTier::WebTier0, EShardTier::WebTier1 };
    return tiers;
}

class TShardInfo {
public:
    TShardInfo(const TString& shardDir);

    EShardTier GetTier() const;
    const TString& GetName() const;
    const TString& GetPath() const;
    size_t GetDocs() const;

private:
    void ParseStampTag();
    void ParseDocCount();
    void ParseLine(TStringBuf line);

    const TString& SafeGetValue(TStringBuf key) const;

private:
    const TString ShardDir_;
    THashMap<TString, TString> KeyValue_;
    size_t DocCount_ = 0;
};

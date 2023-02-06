#include "index_cache.h"

#include <search/tools/test_shard/proto/cache.pb.h>

#include <util/digest/murmur.h>
#include <util/generic/maybe.h>
#include <util/generic/string.h>
#include <util/stream/file.h>
#include <util/system/fs.h>
#include <util/ysaveload.h>

#include <cmath>

namespace NTestShard {

namespace {

TStringBuf GetRawAttribute(TStringBuf attribute) {
    return attribute.After('\"').RBefore('\"');
}

TString GetFullAttribute(const TString& attribute, const TString& prefix, const TString& suffix) {
    return prefix + attribute + suffix;
}

void SerializeFormat(NProto::TCache* result, TStringBuf begin) {
    TStringBuf prefix;
    TStringBuf suffix;
    size_t pos = begin.find_first_of('\"');
    if (pos != TStringBuf::npos) {
        prefix = begin.SubStr(0, pos + 1);
    }
    pos = begin.find_last_of('\"');
    if (pos != TStringBuf::npos) {
        suffix = begin.SubStr(pos);
    }
    result->SetPrefix(TString{prefix});
    result->SetSuffix(TString{suffix});
}

void SerializeDict(NProto::TCache* result, const TSingleAttribute& attr) {
    result->SetType(NProto::ECacheType::Dict);
    SerializeFormat(result, attr.begin()->second);
    for (const auto& p : attr) {
        NProto::TAttrPair* pair = result->MutableDict()->AddData();
        pair->SetDoc(p.first);
        pair->SetAttribute(TString{GetRawAttribute(p.second)});
    }
}

void SerializeFlat(NProto::TCache* result, const TSingleAttribute& attr, ui32 docs) {
    result->SetType(NProto::ECacheType::Flat);
    SerializeFormat(result, attr.begin()->second);
    for (ui32 doc = 0; doc < docs; ++doc) {
        auto attribute = result->MutableFlat()->AddData();
        if (attr.Has(doc)) {
            *attribute = GetRawAttribute(attr.Get(doc));
        }
    }
}

TSingleAttribute LoadDict(const NProto::TCache& cache) {
    TSingleAttribute res;
    for (const NProto::TAttrPair& p : cache.GetDict().GetData()) {
        res.Add(p.GetDoc(), GetFullAttribute(p.GetAttribute(), cache.GetPrefix(), cache.GetSuffix()));
    }
    return res;
}

TSingleAttribute LoadFlat(const NProto::TCache& cache) {
    TSingleAttribute res;
    ui32 doc = 0;
    for (const TString& attr : cache.GetFlat().GetData()) {
        if (!attr.empty()) {
            res.Add(doc, GetFullAttribute(attr, cache.GetPrefix(), cache.GetSuffix()));
        }
        ++doc;
    }
    return res;
}

}

TIndexCache::TIndexCache(const TString& path, ui32 docs, ui32 uniqueId)
    : Dir_(path)
    , Docs_(docs)
    , UniqueId_(uniqueId)
    , Disabled_(path.empty())
{
    if (!Disabled_) {
        NFs::MakeDirectoryRecursive(Dir_);
        NFs::EnsureExists(Dir_);
    }
}

void TIndexCache::Add(const TString& name, const TSingleAttribute& attr) {
    if (Disabled_ || Exists(name)) {
        return;
    }
    const TFsPath path = GetPath(name);
    if (!NFs::Exists(path.Parent())) {
        NFs::MakeDirectoryRecursive(path.Parent());
    }
    TFileOutput output(path);
    Serialize(&output, attr);
}

TMaybe<TSingleAttribute> TIndexCache::Get(const TString& name) {
    if (Disabled_ || !Exists(name)) {
        return {};
    }
    TFileInput input(GetPath(name));
    return Load(&input);
}

bool TIndexCache::Exists(const TString& name) {
    return NFs::Exists(GetPath(name));
}

void TIndexCache::Serialize(IOutputStream* output, const TSingleAttribute& attr) {
    NProto::TCache cache;

    const ui32 size = attr.Size();
    Y_ENSURE(size <= Docs_, "Invalid inverse index");
    const ui32 docIdWidth = sizeof(ui32);
    const ui32 dictOverhead = size * docIdWidth;
    const ui32 flatOverhead = (Docs_ - size) * sizeof(ui32);
    if (dictOverhead < flatOverhead) {
        SerializeDict(&cache, attr);
    } else {
        SerializeFlat(&cache, attr, Docs_);
    }
    cache.SerializeToArcadiaStream(output);
}

TFsPath TIndexCache::GetPath(const TString& name) const {
    return Dir_ / ToString(UniqueId_) / ("attr." + name);
}

TSingleAttribute TIndexCache::Load(IInputStream* input) {
    NProto::TCache cache;
    cache.ParseFromArcadiaStream(input);

    switch (cache.GetType()) {
    case NProto::ECacheType::Dict:
        return LoadDict(cache);
    case NProto::ECacheType::Flat:
        return LoadFlat(cache);
    default:
        Y_ENSURE(false);
        break;
    }
}

}

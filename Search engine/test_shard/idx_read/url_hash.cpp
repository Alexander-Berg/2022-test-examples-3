#include "url_hash.h"

#include <search/tools/test_shard/common/zhash.h>

#include <kernel/doom/wad/wad.h>
#include <library/cpp/offroad/flat/flat_searcher.h>
#include <ysite/yandex/erf/inv_url_hashes.h>
#include <ysite/yandex/erf/flat_hash_table.h>

#include <util/folder/path.h>
#include <util/system/fs.h>

namespace NTestShard {

void TUrlHasher::InitFromWad(const TString& invHashPath) {
    static const NDoom::TWadLumpId invHashLump(NDoom::EWadIndexType::InvUrlHashesIndexType, NDoom::EWadLumpRole::Hits);
    THolder<NDoom::IWad> localWad(NDoom::IWad::Open(invHashPath));
    NOffroad::TFlatSearcher<ui32, ui64, NOffroad::TUi32Vectorizer, NOffroad::TUi64Vectorizer> flatSearcher(localWad->LoadGlobalLump(invHashLump));

    ui32 realSize = 0;
    for (size_t i = 0; i < flatSearcher.Size(); ++i) {
        ui32 docId = flatSearcher.ReadKey(i);
        if (docId != TFlatHashTable::EmptyDocId) {
            if (Hash_.size() <= docId) {
                Hash_.resize(docId + 1);
            }
            Hash_[docId] = flatSearcher.ReadData(i);
            ++realSize;
        }
    }
    Y_ENSURE(realSize == Hash_.size());
}

void TUrlHasher::InitFromNonWad(const TString& invHashPath) {
    TInvUrlHashesIterator it(invHashPath);
    Hash_.resize(it.Length());
    for (size_t i = 0; i < it.Length(); ++i) {
        TInvUrlHashesPair hash = it.At(i);
        Y_ENSURE(hash.DocId < Hash_.size());
        Hash_[hash.DocId] = hash.Hash;
    }
}

TUrlHasher::TUrlHasher(const TString& shardDir) {
    if (NFs::Exists(TFsPath(shardDir) / "indexinvhash.wad")) {
        InitFromWad(TFsPath(shardDir) / "indexinvhash.wad");
    } else if (NFs::Exists(TFsPath(shardDir) / "indexinvhash")) {
        InitFromNonWad(TFsPath(shardDir) / "indexinvhash");
    } else {
        ythrow yexception() << "Can't determine number of documents in shard";
    }
}

TDocHash TUrlHasher::GetHash(TDoc doc) const {
    return Hash_[doc];
}

TString TUrlHasher::GetZHash(TDoc doc) const {
    return GetDocZHash(GetHash(doc));
}

size_t TUrlHasher::DocCount() const {
    return Hash_.size();
}

}

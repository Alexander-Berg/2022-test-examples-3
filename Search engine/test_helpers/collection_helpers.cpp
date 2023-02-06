#include "collection_helpers.h"

#include <search/plutonium/impl/chunkler/chunk_collection.h>
#include <search/plutonium/impl/chunkler/helpers.h>
#include <search/plutonium/impl/chunkler/multi_lump_packer.h>
#include <search/plutonium/impl/chunkler/multi_lump_unpacker.h>

#include <library/cpp/testing/gtest/gtest.h>

#include <util/generic/map.h>
#include <util/system/unaligned_mem.h>

namespace NPlutonium::NChunkler {

void TCollectionHelpers::CheckCollection(
    const TChunkCollection* chunkCollection,
    const TVector<ui32>& expectedKeys,
    const TVector<ui64>* expectedTimestamps,
    const IReadOnlyFileSystem* workerFs,
    const TExternalDocFetcher& externalDocFetcher,
    TVector<TString>* actualLumpIds
) {
    const auto& keys = chunkCollection->GetAllKeys();
    TVector<ui32> actualKeys;
    TMap<ui32, ui64> actualKeysWithTs;
    THashSet<TString> lumpIdsSet;
    for (size_t chunkId = 0; chunkId < keys.size(); ++chunkId) {
        const TKeysView& keysView = keys[chunkId];
        const auto& chunkMeta = chunkCollection->GetChunkMeta(chunkId);
        const bool multipleLumps = chunkMeta.GetMultipleLumps();
        const TVector<TString> lumpIds{chunkMeta.GetLumpIds().begin(), chunkMeta.GetLumpIds().end()};
        lumpIdsSet.insert(chunkMeta.GetLumpIds().begin(), chunkMeta.GetLumpIds().end());
        keysView.ForEach([&](ui32 localDocId, TDocKey keyBlob) {
            TVector<TBlob> docBlobs(Reserve(2));
            if (!chunkCollection->GetConfig().BodylessMode) {
                docBlobs.push_back(chunkCollection->GetDocBody(chunkId, localDocId, workerFs));
            }
            if (!!externalDocFetcher) {
                docBlobs.push_back(externalDocFetcher(chunkId, localDocId));
            }
            ASSERT_FALSE(docBlobs.empty());
            TMaybe<ui32> lastKey;
            for (const TBlob& docBlob : docBlobs) {
                ui32 key = 0;
                CheckKeyBodyPair(keyBlob, docBlob, &key, multipleLumps, lumpIds);
                if (lastKey.Defined()) {
                    ASSERT_EQ(*lastKey, key);
                } else {
                    lastKey = key;
                }
                if (expectedTimestamps) {
                    actualKeysWithTs[key] = chunkCollection->GetDocTimestamp(chunkId, localDocId, TInstant::Max()).MicroSeconds();
                }
            }
            ASSERT_TRUE(lastKey.Defined());
            actualKeys.push_back(*lastKey);
        });
    }
    Sort(actualKeys);
    ASSERT_EQ(actualKeys, expectedKeys);
    if (expectedTimestamps != nullptr) {
        ASSERT_EQ(expectedTimestamps->size(), expectedKeys.size());
        ASSERT_EQ(expectedTimestamps->size(), actualKeysWithTs.size());
        for (size_t i = 0; i < expectedKeys.size(); ++i) {
            ASSERT_EQ(actualKeysWithTs.at(expectedKeys[i]), (*expectedTimestamps)[i]);
        }
    }
    if (actualLumpIds != nullptr) {
        actualLumpIds->assign(lumpIdsSet.begin(), lumpIdsSet.end());
        Sort(*actualLumpIds);
    }
}

TVector<TDocKey> TCollectionHelpers::ConvertKeys(const TVector<ui32>& keys) {
    TVector<TDocKey> result(Reserve(keys.size()));
    for (size_t i = 0; i < keys.size(); ++i) {
        result.emplace_back(reinterpret_cast<const ui8*>(&keys[i]), sizeof(ui32));
    }
    return result;
}

TVector<TDocBody> TCollectionHelpers::ConvertBodies(const TVector<TBlob>& docs) {
    TVector<TDocKey> result(Reserve(docs.size()));
    for (const TBlob& blob : docs) {
        result.emplace_back(blob);
    }
    return result;
}

TVector<TBlob> TCollectionHelpers::GenerateDocsByKeys(const TVector<ui32>& keys, bool multipleLumps, const TVector<TString>& lumpIds) {
    TVector<TBlob> docs(Reserve(keys.size()));
    for (ui32 key : keys) {
        docs.push_back(GenerateDocByKey(key, multipleLumps, lumpIds));
    }
    return docs;
}

TBlob TCollectionHelpers::GenerateDocByKey(ui32 key, bool multipleLumps, const TVector<TString>& lumpIds) {
    Y_ENSURE(!lumpIds.empty());
    if (!multipleLumps) {
        return TBlob::FromStringSingleThreaded(GenerateLump(key, lumpIds[0]));
    }
    TMultiLumpPacker packer;
    for (const TString& lumpId : lumpIds) {
        packer.AddLump(lumpId, AsBytesRef(GenerateLump(key, lumpId)));
    }
    TArrayRef<const ui8> packed = packer.Finish();
    return TBlob::Copy(packed.data(), packed.size());
}

TString TCollectionHelpers::GenerateLump(ui32 key, TStringBuf lumpId) {
    if (lumpId == "lump1"sv) {
        return "doc #" + ToString(key);
    }
    if (lumpId == "lump2"sv) {
        return "another lump #" + ToString(key);
    }
    return TString::Join("lump ", lumpId, "; doc #", ToString(key));
}

void TCollectionHelpers::CheckKeyBodyPair(TDocKey key, TDocBody body, ui32* retKey, bool multipleLumps, const TVector<TString>& lumpIds) {
    ASSERT_EQ(key.size(), sizeof(ui32));
    ui32 origKey = ReadUnaligned<ui32>(key.data());
    if (retKey) {
        *retKey = origKey;
    }
    if (!multipleLumps) {
        TBlob expectedBody = GenerateDocByKey(origKey, false, lumpIds);
        ASSERT_EQ(TDocBody{expectedBody}, body);
    } else {
        THashSet<TString> lumpSet{lumpIds.begin(), lumpIds.end()};
        TMultiLumpUnpacker unpacker(body);
        ASSERT_GT(unpacker.Size(), 0u);
        for (auto&& [lumpId, lumpBody] : unpacker) {
            ASSERT_TRUE(lumpSet.contains(lumpId));
            ASSERT_EQ(AsStrBuf(lumpBody), GenerateLump(origKey, lumpId));
        }
    }
}

}

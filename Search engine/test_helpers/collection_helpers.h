#pragma once

#include <search/plutonium/impl/chunkler/fwd.h>

#include <util/memory/blob.h>

namespace NPlutonium {
    class IReadOnlyFileSystem;
}

namespace NPlutonium::NChunkler {

struct TCollectionHelpers {
    using TExternalDocFetcher = std::function<TBlob (ui32 chunkId, ui32 docId)>;

    static void CheckCollection(
        const TChunkCollection* chunkCollection,
        const TVector<ui32>& expectedKeys,
        const TVector<ui64>* expectedTimestamps,
        const IReadOnlyFileSystem* workerFs,
        const TExternalDocFetcher& externalDocFetcher,
        TVector<TString>* actualLumpIds);

    static TVector<TDocKey> ConvertKeys(const TVector<ui32>& keys);
    static TVector<TDocBody> ConvertBodies(const TVector<TBlob>& docs);

    static TVector<TBlob> GenerateDocsByKeys(const TVector<ui32>& keys, bool multipleLumps, const TVector<TString>& lumpIds);
    static TBlob GenerateDocByKey(ui32 key, bool multipleLumps, const TVector<TString>& lumpIds);
    static TString GenerateLump(ui32 key, TStringBuf lumpId);

    static void CheckKeyBodyPair(TDocKey key, TDocBody body, ui32* retKey, bool multipleLumps, const TVector<TString>& lumpIds);
};

}

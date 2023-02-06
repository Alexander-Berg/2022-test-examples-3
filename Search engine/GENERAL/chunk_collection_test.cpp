#include "chunk_collection.h"
#include "config.h"
#include "external_chunk_builder.h"
#include "helpers.h"
#include "multi_lump_packer.h"
#include "multi_lump_unpacker.h"

#include <search/plutonium/impl/chunkler/test_helpers/collection_helpers.h>
#include <search/plutonium/impl/chunkler/test_helpers/random_data_source.h>
#include <search/plutonium/impl/fs_cache/cache_storage.h>
#include <search/plutonium/impl/state/id_generator/factory.h>
#include <search/plutonium/impl/state/local_disk_for_tests/local_disk_for_tests_state_machine.h>
#include <search/plutonium/core/state/state_mutation.h>

#include <yt/yt/core/misc/shutdown.h>

#include <library/cpp/testing/gtest/gtest.h>

#include <util/folder/tempdir.h>
#include <util/generic/map.h>

namespace NPlutonium::NChunkler {
namespace {

NJupiter::TTierConfig GetTestTierConfig(ui32 totalChunks, ui32 deltaChunks, bool enablePudge2) {
    return NJupiter::TTierConfig(
        totalChunks, /* number of chunks */
        deltaChunks, /* min. chunks to rebuild */
        0.2, /* max. additional space */
        0, /* max. largest to smallest chunk ratio */
        enablePudge2
    );
}

struct TExternalChunkStorage {
    TBlob GetBlob(ui32 chunkId, ui32 docId) const {
        Y_ENSURE(chunkId < Chunks_.size(), "chunk #" << chunkId << " doesn't exist");
        const TVector<TBlob>& chunk = Chunks_[chunkId];
        Y_ENSURE(docId < chunk.size(), "doc #" << docId << " is out of range, size of chunk #" << chunkId << " is " << chunk.size());
        Y_ENSURE(!chunk[docId].IsNull(), "doc #" << docId << " is null in chunk #" << chunkId);
        return chunk[docId];
    }

    void ReplaceChunk(ui32 chunkId, TVector<TBlob> blobs) {
        if (Chunks_.size() <= chunkId) {
            Chunks_.resize(chunkId + 1);
        }
        Chunks_[chunkId] = std::move(blobs);
    }

private:
    TVector<TVector<TBlob>> Chunks_;
};

struct TExternalChunkBuilder : IExternalChunkBuilder {
    TExternalChunkBuilder(TExternalChunkStorage* chunkStorage, TVector<TBlob> newDocuments)
        : ChunkStorage_(chunkStorage)
        , NewDocuments_(std::move(newDocuments))
    {
        Y_ENSURE(ChunkStorage_);
    }

    void StartChunk(ui32 dstChunkId, ui32 chunkSize, const TVector<TString>& /*lumpIds*/) final {
        FinalizeCurrentChunk();
        Y_ENSURE(NewChunks_[dstChunkId].size() == 0, "dst chunk #" << dstChunkId << " already exists");
        NewChunks_[dstChunkId].resize(chunkSize);
        NewChunkId_ = dstChunkId;
    }

    void CopyOldDocument(ui32 srcChunkId, ui32 srcDocId, ui32 dstChunkId, ui32 dstDocId) final {
        CheckDestination(dstChunkId, dstDocId);
        NewChunks_[dstChunkId][dstDocId] = ChunkStorage_->GetBlob(srcChunkId, srcDocId);
    }

    void WriteNewDocument(ui32 newDocumentIndex, ui32 dstChunkId, ui32 dstDocId) final {
        CheckDestination(dstChunkId, dstDocId);
        Y_ENSURE(newDocumentIndex < NewDocuments_.size(), newDocumentIndex << " >= " << NewDocuments_.size());
        Y_ENSURE(!NewDocuments_[newDocumentIndex].IsNull(), "new doc #" << newDocumentIndex << " is null");
        NewChunks_[dstChunkId][dstDocId] = std::move(NewDocuments_[newDocumentIndex]);
        NewDocuments_[newDocumentIndex] = TBlob{};
    }

    void Finish() final {
        FinalizeCurrentChunk();
        for (size_t i = 0; i < NewDocuments_.size(); ++i) {
            Y_ENSURE(NewDocuments_[i].IsNull(), "New doc #" << i << " is not written yet");
        }
        for (auto& [chunkId, blobs] : NewChunks_) {
            ChunkStorage_->ReplaceChunk(chunkId, std::move(blobs));
        }
    }

private:
    void CheckDestination(ui32 dstChunkId, ui32 dstDocId) {
        Y_ENSURE(NewChunkId_.Defined(), dstChunkId << " != null");
        Y_ENSURE(dstChunkId == *NewChunkId_, dstChunkId << " != " << *NewChunkId_);
        Y_ENSURE(NewChunks_.contains(dstChunkId), "dst chunk #" << dstChunkId << " is not allocated yet");
        auto& newChunk = NewChunks_[dstChunkId];
        Y_ENSURE(dstDocId < newChunk.size(), dstDocId << " is out of range [0; " << newChunk.size() << ")");
        Y_ENSURE(newChunk[dstDocId].IsNull(), "chunk #" << dstChunkId << " already has a doc #" << dstDocId);
    }

    void FinalizeCurrentChunk() {
        if (NewChunkId_.Defined()) {
            auto& newChunk = NewChunks_[*NewChunkId_];
            for (size_t i = 0; i < newChunk.size(); ++i) {
                Y_ENSURE(!newChunk[i].IsNull(), "Chunk #" << *NewChunkId_ << " has no document #" << i);
            }
            NewChunkId_.Clear();
        }
    }

    TExternalChunkStorage* ChunkStorage_ = nullptr;
    TVector<TBlob> NewDocuments_;
    TMaybe<ui32> NewChunkId_;
    THashMap<ui32, TVector<TBlob>> NewChunks_;
};

struct TTestEnvironmentParams {
    ui32 NumberOfChunks = 0;
    ui32 DeltaChunks = 0;
    TTtlConfig TtlConfig;
    bool EnableCompression = false;
    bool BodylessMode = false;
    bool ExternalStorage = false;
    bool MultipleLumps = false;
    TVector<TString> LumpIds;
    bool MeasureStats = false;
    bool WriteAdditionalDocInfo = false;
    ui32 InitThreads = 0;
    bool EnablePudge2 = false;
};
struct TTestEnvironmentParamsPatch {
    ui32 NumberOfChunks = 0;
    ui32 DeltaChunks = 0;
    bool EnableCompression = false;
    bool MultipleLumps = false;
    TVector<TString> LumpIds;
    bool MeasureStats = false;
    bool EnablePudge2 = false;
};

struct TTestEnvironment {
    TTestEnvironment(const TTestEnvironmentParams& params) {
        Y_ENSURE(!params.BodylessMode || params.ExternalStorage);

        Config_.Stream = "test_shard";
        Config_.CollectionPathPrefix = "chunk";
        Config_.TierConfig = GetTestTierConfig(params.NumberOfChunks, params.DeltaChunks, params.EnablePudge2);
        Config_.TtlConfig = params.TtlConfig;
        Config_.Compression.Enabled = params.EnableCompression;
        Config_.Compression.MainCodec = "zstd08d-1";
        Config_.Compression.FallbackCodec = "lz4";
        Config_.BodylessMode = params.BodylessMode;
        Config_.MultipleLumps = params.MultipleLumps;
        Config_.LumpIds = params.LumpIds.empty() ? TVector<TString>{"lump1"} : params.LumpIds;
        Config_.MeasureStats = params.MeasureStats;
        Config_.WriteAdditionalDocInfo = params.WriteAdditionalDocInfo;
        Config_.InitThreads = params.InitThreads;

        if (params.ExternalStorage) {
            ExternalChunkStorage_ = MakeHolder<TExternalChunkStorage>();
        }

        WorkerFsCache_ = NFsCache::TCacheStorage{
            NFsCache::TFileAllocatorConfig{
                .DirPath = CacheDir_.Path(),
                .SizeCapacity = 100 * 1024,
                .SignalCollector = MakeAtomicShared<TBlackHole>()
            }
        };
        TLocalDiskForTestsStateMachine::InitializeWorkingDir(WorkDir_.Path());
        StateMachine_ = MakeHolder<NPlutonium::TLocalDiskForTestsStateMachine>(
            WorkDir_.Path(),
            CreateStateIdGenerator("numeric"),
            WorkerFsCache_);

        CreateChunkCollection(ChunkCollection_);
    }

    ~TTestEnvironment() {
        ChunkCollection_.Clear();
        StateMachine_.Reset();
    }

    ui32 GetChunkCount() const {
        return ChunkCollection_->GetAllKeys().size();
    }

    void ExpectWorkerFsFileExists(const TString& path, bool exists, ui64 minSize) const {
        auto fs = StateMachine_->CurrentState()->WorkerFs;
        const bool actuallyExists = fs->Exists(path);
        EXPECT_EQ(actuallyExists, exists);
        if (actuallyExists) {
            EXPECT_GE(fs->GetFileMetaInfo(path)->Size, minSize);
        }
    }

    void RecreateChunkCollection(const TVector<ui32>& expectedKeys, const TVector<ui64>* expectedTimestamps, const TTestEnvironmentParamsPatch& params) {
        Config_.Compression.Enabled = params.EnableCompression;
        Config_.TierConfig = GetTestTierConfig(params.NumberOfChunks, params.DeltaChunks, params.EnablePudge2);
        Config_.MultipleLumps = params.MultipleLumps;
        if (params.LumpIds.empty()) {
            Config_.LumpIds = params.MultipleLumps ? TVector<TString>{"lump1", "lump2"} : TVector<TString>{"lump1"};
        } else {
            Config_.LumpIds = params.LumpIds;
        }
        Config_.MeasureStats = params.MeasureStats;
        ChunkCollection_.Clear();
        CreateChunkCollection(ChunkCollection_);

        CheckCollection(
            ChunkCollection_.Get(),
            expectedKeys,
            expectedTimestamps,
            StateMachine_->CurrentState()->WorkerFs,
            ExternalChunkStorage_.Get(),
            &ActualLumpIds_);
    }

    void ApplyUpdates(const TVector<ui32>& keysToRemove, const TVector<ui32>& keysToUpdate, const TVector<ui32>& expectedKeys) {
        return ApplyUpdates(keysToRemove, keysToUpdate, expectedKeys, TInstant::Now().MicroSeconds(), nullptr);
    }

    void ApplyUpdates(
        const TVector<ui32>& keysToRemove,
        const TVector<ui32>& keysToUpdate,
        const TVector<ui32>& expectedKeys,
        ui64 currentTs,
        const TVector<ui64>* expectedTimestamps
    ) {
        const auto toRemove = TCollectionHelpers::ConvertKeys(keysToRemove);
        const auto toUpdate = TCollectionHelpers::ConvertKeys(keysToUpdate);
        const auto generatedBodies = TCollectionHelpers::GenerateDocsByKeys(keysToUpdate, Config_.MultipleLumps, Config_.LumpIds);
        const auto convertedBodies = TCollectionHelpers::ConvertBodies(generatedBodies);

        ASSERT_EQ(toUpdate.size(), convertedBodies.size());
        for (size_t i = 0; i < toUpdate.size(); ++i) {
            TCollectionHelpers::CheckKeyBodyPair(toUpdate[i], convertedBodies[i], nullptr, Config_.MultipleLumps, Config_.LumpIds);
        }

        TMaybe<TExternalChunkBuilder> externalBuilder;
        if (ExternalChunkStorage_) {
            externalBuilder.ConstructInPlace(ExternalChunkStorage_.Get(), generatedBodies);
        }

        ApplyStateMutation(StateMachine_.Get(), [&](const TCurrentState* currentState, TNewState* newState) {
            ChunkCollection_->ApplyUpdates(
                toRemove,
                toUpdate,
                convertedBodies,
                Config_.MultipleLumps,
                currentState->WorkerFs,
                newState->WorkerFs,
                TInstant::MicroSeconds(currentTs),
                nullptr,
                externalBuilder.Get()
            );
        });

        CheckCollection(
            ChunkCollection_.Get(),
            expectedKeys,
            expectedTimestamps,
            StateMachine_->CurrentState()->WorkerFs,
            ExternalChunkStorage_.Get(),
            &ActualLumpIds_);

        TMaybe<TChunkCollection> chunkCollection;
        CreateChunkCollection(chunkCollection);
        CheckCollection(
            chunkCollection.Get(),
            expectedKeys,
            expectedTimestamps,
            StateMachine_->CurrentState()->WorkerFs,
            ExternalChunkStorage_.Get(),
            &ActualLumpIds_);
    }

    void ExpectLumpIds(const TVector<TString>& expectedLumpIds) {
        ASSERT_EQ(ActualLumpIds_, expectedLumpIds);
    }

private:
    static void CheckCollection(
        const TChunkCollection* chunkCollection,
        const TVector<ui32>& expectedKeys,
        const TVector<ui64>* expectedTimestamps,
        const IReadOnlyFileSystem* workerFs,
        const TExternalChunkStorage* externalChunkStorage,
        TVector<TString>* actualLumpIds
    ) {
        TCollectionHelpers::TExternalDocFetcher docFetcher;
        if (externalChunkStorage != nullptr) {
            docFetcher = [externalChunkStorage](ui32 chunkId, ui32 docId) -> TBlob {
                return externalChunkStorage->GetBlob(chunkId, docId);
            };
        }
        TCollectionHelpers::CheckCollection(chunkCollection, expectedKeys, expectedTimestamps, workerFs, docFetcher, actualLumpIds);
    }

    void CreateChunkCollection(TMaybe<TChunkCollection>& chunkCollection) const {
        chunkCollection.ConstructInPlace(Config_);
        chunkCollection->SyncWithCurrentState(StateMachine_->CurrentState()->WorkerFs);
    }

    TChunkCollectionConfig Config_;
    TTempDir WorkDir_;
    TTempDir CacheDir_;
    NFsCache::TCacheStorage WorkerFsCache_;
    THolder<IStateMachine> StateMachine_;
    TMaybe<TChunkCollection> ChunkCollection_;
    THolder<TExternalChunkStorage> ExternalChunkStorage_;
    TVector<TString> ActualLumpIds_;
};

} // anonymous namespace

TEST(TChunkCollectionTest, Simple) {
    const TVector<std::pair<ui32, ui32>> tiers{
        {1, 1},
        {2, 1},
        {10, 1},
        {2, 2},
        {10, 2}
    };
    for (auto [nChunks, deltaChunks] : tiers) {
        TTestEnvironment env({
            .NumberOfChunks = nChunks,
            .DeltaChunks = deltaChunks
        });
        env.ApplyUpdates({1, 2, 3}, {}, {});
        env.ApplyUpdates({5}, {1, 2, 3, 4}, {1, 2, 3, 4});
        env.ApplyUpdates({2}, {3, 4, 6}, {1, 3, 4, 6});
        env.ApplyUpdates({}, {4, 7}, {1, 3, 4, 6, 7});
        env.ApplyUpdates({3, 4, 5}, {}, {1, 6, 7});
        env.ApplyUpdates({1, 6, 7}, {}, {});
    }
}

TEST(TChunkCollectionTest, MoreDocs) {
    TTestEnvironment env({
        .NumberOfChunks = 10,
        .DeltaChunks = 2
    });
    TRandomDataSource data(1, 1000000, 10, 20, 42);

    TVector<ui32> keysToRemove;
    TVector<ui32> keysToUpdate;
    TVector<ui32> expectedKeys;
    for (size_t i = 0; i < 100; ++i) {
        data.NextIteration(&keysToRemove, &keysToUpdate, &expectedKeys);
        env.ApplyUpdates(keysToRemove, keysToUpdate, expectedKeys);
    }
}

TEST(TChunkCollectionTest, MoreDocsWithTimestamps) {
    TTestEnvironment env({
        .NumberOfChunks = 10,
        .DeltaChunks = 2,
        .WriteAdditionalDocInfo = true
    });
    TRandomDataSource data(1, 1000000, 10, 20, 42);

    TVector<ui32> keysToRemove;
    TVector<ui32> keysToUpdate;
    TVector<ui32> expectedKeys;
    TVector<ui64> expectedTimestamps;
    for (size_t i = 0; i < 100; ++i) {
        const ui64 ts = data.NextIteration(&keysToRemove, &keysToUpdate, &expectedKeys, &expectedTimestamps);
        env.ApplyUpdates(keysToRemove, keysToUpdate, expectedKeys, ts, &expectedTimestamps);
    }
}

TEST(TChunkCollectionTest, MoreDocsWithTimestampsAndTTL) {
    const ui32 nChunks = 10;
    const ui64 ttl = 5;
    TTestEnvironment env({
        .NumberOfChunks = nChunks,
        .DeltaChunks = 2,
        .TtlConfig = TTtlConfig{
            .Ttl = TDuration::MicroSeconds(ttl),
            .ChunksPerIteration = nChunks
        },
        .WriteAdditionalDocInfo = true,
    });

    TRandomDataSource data(1, 1000000, 100, 20, 42);

    TVector<ui32> keysToRemove;
    TVector<ui32> keysToUpdate;
    TVector<ui32> expectedKeys;
    TVector<ui64> expectedTimestamps;
    for (size_t i = 0; i < 100; ++i) {
        const ui64 ts = data.NextIteration(&keysToRemove, &keysToUpdate, &expectedKeys, &expectedTimestamps, ttl);
        env.ApplyUpdates(keysToRemove, keysToUpdate, expectedKeys, ts, &expectedTimestamps);
    }
}

TEST(TChunkCollectionTest, ShrinkMode) {
    for (ui32 rebuildRate = 1; rebuildRate < 5; ++rebuildRate) {
        const ui32 initialChunkCount = 10;
        const ui32 reducedChunkCount = 5;
        ASSERT_GT(initialChunkCount, reducedChunkCount);

        TTestEnvironment env({
            .NumberOfChunks = initialChunkCount,
            .DeltaChunks = rebuildRate
        });
        TRandomDataSource data(1, 1000000, 10, 20, 42);

        TVector<ui32> keysToRemove;
        TVector<ui32> keysToUpdate;
        TVector<ui32> expectedKeys;
        for (size_t i = 0; i < initialChunkCount + 1; ++i) {
            data.NextIteration(&keysToRemove, &keysToUpdate, &expectedKeys);
            env.ApplyUpdates(keysToRemove, keysToUpdate, expectedKeys);
        }

        data.GetExpectedKeysAndTimestamp(&expectedKeys, nullptr);
        env.RecreateChunkCollection(expectedKeys, nullptr, {
            .NumberOfChunks = reducedChunkCount,
            .DeltaChunks = rebuildRate
        });

        ui32 chunkCount = env.GetChunkCount();
        EXPECT_EQ(chunkCount, initialChunkCount);

        const ui32 nIterations = initialChunkCount - reducedChunkCount + 1;
        for (size_t i = 0; i < nIterations; ++i) {
            data.NextIteration(&keysToRemove, &keysToUpdate, &expectedKeys);
            env.ApplyUpdates(keysToRemove, keysToUpdate, expectedKeys);

            const ui32 newChunkCount = env.GetChunkCount();
            EXPECT_GE(newChunkCount, reducedChunkCount);
            if (chunkCount != newChunkCount) {
                ASSERT_GT(chunkCount, newChunkCount);
                EXPECT_LE(chunkCount - newChunkCount, rebuildRate);
                chunkCount = newChunkCount;
            }
        }

        EXPECT_EQ(chunkCount, reducedChunkCount);
    }
}

TEST(TChunkCollectionTest, ExpandMode) {
    for (ui32 rebuildRate = 1; rebuildRate < 5; ++rebuildRate) {
        const ui32 initialChunkCount = 5;
        const ui32 expandChunkCount = 10;
        ASSERT_GT(expandChunkCount, initialChunkCount);

        TTestEnvironment env({
            .NumberOfChunks = initialChunkCount,
            .DeltaChunks = rebuildRate
        });
        TRandomDataSource data(1, 1000000, 10, 20, 42);

        TVector<ui32> keysToRemove;
        TVector<ui32> keysToUpdate;
        TVector<ui32> expectedKeys;
        for (size_t i = 0; i < initialChunkCount + 1; ++i) {
            data.NextIteration(&keysToRemove, &keysToUpdate, &expectedKeys);
            env.ApplyUpdates(keysToRemove, keysToUpdate, expectedKeys);
        }

        data.GetExpectedKeysAndTimestamp(&expectedKeys, nullptr);
        env.RecreateChunkCollection(expectedKeys, nullptr, {
            .NumberOfChunks = expandChunkCount,
            .DeltaChunks = rebuildRate
        });

        ui32 chunkCount = env.GetChunkCount();
        EXPECT_EQ(chunkCount, initialChunkCount);

        const ui32 nIterations = expandChunkCount - initialChunkCount + 1;
        for (size_t i = 0; i < nIterations; ++i) {
            data.NextIteration(&keysToRemove, &keysToUpdate, &expectedKeys);
            env.ApplyUpdates(keysToRemove, keysToUpdate, expectedKeys);

            const ui32 newChunkCount = env.GetChunkCount();
            EXPECT_GE(expandChunkCount, newChunkCount);
            if (chunkCount != newChunkCount) {
                ASSERT_GT(newChunkCount, chunkCount);
                EXPECT_LE(newChunkCount - chunkCount, rebuildRate);
                chunkCount = newChunkCount;
            }
        }

        EXPECT_EQ(chunkCount, expandChunkCount);
    }
}

TEST(TChunkCollectionTest, MoreDocsWithCompression) {
    TTestEnvironment env({
        .NumberOfChunks = 10,
        .DeltaChunks = 2,
        .EnableCompression = true
    });
    TRandomDataSource data(1, 1000000, 10, 20, 42);

    TVector<ui32> keysToRemove;
    TVector<ui32> keysToUpdate;
    TVector<ui32> expectedKeys;
    for (size_t i = 0; i < 100; ++i) {
        data.NextIteration(&keysToRemove, &keysToUpdate, &expectedKeys);
        env.ApplyUpdates(keysToRemove, keysToUpdate, expectedKeys);
    }
}

TEST(TChunkCollectionTest, MoreDocsWithMixedCompressionModes) {
    const ui32 nChunks = 10;
    const ui32 chunksToRebuild = 2;
    TTestEnvironment env({
        .NumberOfChunks = nChunks,
        .DeltaChunks = chunksToRebuild,
        .EnableCompression = false
    });
    TRandomDataSource data(1, 1000000, 10, 20, 42);

    TVector<ui32> keysToRemove;
    TVector<ui32> keysToUpdate;
    TVector<ui32> expectedKeys;
    for (size_t i = 0; i < nChunks * 2; ++i) {
        data.NextIteration(&keysToRemove, &keysToUpdate, &expectedKeys);
        env.ApplyUpdates(keysToRemove, keysToUpdate, expectedKeys);
    }

    // enable compression
    data.GetExpectedKeysAndTimestamp(&expectedKeys, nullptr);
    env.RecreateChunkCollection(expectedKeys, nullptr, {
        .NumberOfChunks = nChunks,
        .DeltaChunks = chunksToRebuild,
        .EnableCompression = true
    });

    for (size_t i = 0; i < nChunks * 2; ++i) {
        data.NextIteration(&keysToRemove, &keysToUpdate, &expectedKeys);
        env.ApplyUpdates(keysToRemove, keysToUpdate, expectedKeys);
    }

    // disable compression again
    data.GetExpectedKeysAndTimestamp(&expectedKeys, nullptr);
    env.RecreateChunkCollection(expectedKeys, nullptr, {
        .NumberOfChunks = nChunks,
        .DeltaChunks = chunksToRebuild,
        .EnableCompression = false
    });

    for (size_t i = 0; i < chunksToRebuild; ++i) {
        data.NextIteration(&keysToRemove, &keysToUpdate, &expectedKeys);
        env.ApplyUpdates(keysToRemove, keysToUpdate, expectedKeys);
    }

    // enable compression again
    data.GetExpectedKeysAndTimestamp(&expectedKeys, nullptr);
    env.RecreateChunkCollection(expectedKeys, nullptr, {
        .NumberOfChunks = nChunks,
        .DeltaChunks = chunksToRebuild,
        .EnableCompression = true
    });

    for (size_t i = 0; i < nChunks; ++i) {
        data.NextIteration(&keysToRemove, &keysToUpdate, &expectedKeys);
        env.ApplyUpdates(keysToRemove, keysToUpdate, expectedKeys);
    }
}

TEST(TChunkCollectionTest, MultipleLumps) {
    const ui32 nChunks = 10;
    const ui32 chunksToRebuild = 2;
    TTestEnvironment env({
        .NumberOfChunks = nChunks,
        .DeltaChunks = chunksToRebuild
    });
    TRandomDataSource data(1, 1000000, 10, 20, 42);

    TVector<ui32> keysToRemove;
    TVector<ui32> keysToUpdate;
    TVector<ui32> expectedKeys;
    for (size_t i = 0; i < nChunks * 2; ++i) {
        data.NextIteration(&keysToRemove, &keysToUpdate, &expectedKeys);
        env.ApplyUpdates(keysToRemove, keysToUpdate, expectedKeys);
    }
    env.ExpectLumpIds(TVector<TString>{"lump1"});

    // enable multiple lumps
    data.GetExpectedKeysAndTimestamp(&expectedKeys, nullptr);
    env.RecreateChunkCollection(expectedKeys, nullptr, {
        .NumberOfChunks = nChunks,
        .DeltaChunks = chunksToRebuild,
        .MultipleLumps = true
    });

    for (size_t i = 0; i < nChunks * 2; ++i) {
        data.NextIteration(&keysToRemove, &keysToUpdate, &expectedKeys);
        env.ApplyUpdates(keysToRemove, keysToUpdate, expectedKeys);
    }
    env.ExpectLumpIds(TVector<TString>{"lump1", "lump2"});

    // return to single lump mode
    data.GetExpectedKeysAndTimestamp(&expectedKeys, nullptr);
    env.RecreateChunkCollection(expectedKeys, nullptr, {
        .NumberOfChunks = nChunks,
        .DeltaChunks = chunksToRebuild,
        .MultipleLumps = false
    });

    for (size_t i = 0; i < nChunks * 2; ++i) {
        data.NextIteration(&keysToRemove, &keysToUpdate, &expectedKeys);
        env.ApplyUpdates(keysToRemove, keysToUpdate, expectedKeys);
    }
    env.ExpectLumpIds(TVector<TString>{"lump1"});

    // enable multiple lumps again
    data.GetExpectedKeysAndTimestamp(&expectedKeys, nullptr);
    env.RecreateChunkCollection(expectedKeys, nullptr, {
        .NumberOfChunks = nChunks,
        .DeltaChunks = chunksToRebuild,
        .MultipleLumps = true
    });

    for (size_t i = 0; i < nChunks; ++i) {
        data.NextIteration(&keysToRemove, &keysToUpdate, &expectedKeys);
        env.ApplyUpdates(keysToRemove, keysToUpdate, expectedKeys);
    }

    env.ExpectLumpIds(TVector<TString>{"lump1", "lump2"});
}

TEST(TChunkCollectionTest, ExternalStorage) {
    TTestEnvironment env({
        .NumberOfChunks = 10,
        .DeltaChunks = 2,
        .BodylessMode = true,
        .ExternalStorage = true
    });
    TRandomDataSource data(1, 1000000, 10, 20, 42);

    TVector<ui32> keysToRemove;
    TVector<ui32> keysToUpdate;
    TVector<ui32> expectedKeys;

    for (size_t i = 0; i < 100; ++i) {
        data.NextIteration(&keysToRemove, &keysToUpdate, &expectedKeys);
        env.ApplyUpdates(keysToRemove, keysToUpdate, expectedKeys);
    }
}

TEST(TChunkCollectionTest, ExternalStoragePlusDefaultStorage) {
    TTestEnvironment env({
        .NumberOfChunks = 10,
        .DeltaChunks = 2,
        .BodylessMode = false,
        .ExternalStorage = true
    });
    TRandomDataSource data(1, 1000000, 10, 20, 42);

    TVector<ui32> keysToRemove;
    TVector<ui32> keysToUpdate;
    TVector<ui32> expectedKeys;

    for (size_t i = 0; i < 100; ++i) {
        data.NextIteration(&keysToRemove, &keysToUpdate, &expectedKeys);
        env.ApplyUpdates(keysToRemove, keysToUpdate, expectedKeys);
    }
}

TEST(TChunkCollectionTest, DifferentLumpIds) {
    const ui32 nChunks = 10;
    const ui32 chunksToRebuild = 2;
    TTestEnvironment env({
        .NumberOfChunks = nChunks,
        .DeltaChunks = chunksToRebuild,
        .MultipleLumps = true,
        .LumpIds = TVector<TString>{"lump1", "lump3"}
    });
    TRandomDataSource data(1, 1000000, 10, 20, 42);

    TVector<ui32> keysToRemove;
    TVector<ui32> keysToUpdate;
    TVector<ui32> expectedKeys;
    for (size_t i = 0; i < 2; ++i) {
        data.NextIteration(&keysToRemove, &keysToUpdate, &expectedKeys);
        env.ApplyUpdates(keysToRemove, keysToUpdate, expectedKeys);
    }
    env.ExpectLumpIds(TVector<TString>{"lump1", "lump3"});

    // enable multiple lumps
    data.GetExpectedKeysAndTimestamp(&expectedKeys, nullptr);
    env.RecreateChunkCollection(expectedKeys, nullptr, {
        .NumberOfChunks = nChunks,
        .DeltaChunks = chunksToRebuild,
        .MultipleLumps = true,
        .LumpIds = TVector<TString>{"lump1", "lump4"}
    });

    for (size_t i = 0; i < nChunks * 2; ++i) {
        data.NextIteration(&keysToRemove, &keysToUpdate, &expectedKeys);
        env.ApplyUpdates(keysToRemove, keysToUpdate, expectedKeys);
    }

    env.ExpectLumpIds(TVector<TString>{"lump1", "lump3", "lump4"});
}

TEST(TChunkCollectionTest, MeasureStats) {
    const ui32 nChunks = 10;
    const ui32 chunksToRebuild = 2;
    TTestEnvironment env({
        .NumberOfChunks = nChunks,
        .DeltaChunks = chunksToRebuild
    });
    TRandomDataSource data(1, 1000000, 10, 20, 42);

    TVector<ui32> keysToRemove;
    TVector<ui32> keysToUpdate;
    TVector<ui32> expectedKeys;
    for (size_t i = 0; i < 5; ++i) {
        data.NextIteration(&keysToRemove, &keysToUpdate, &expectedKeys);
        env.ApplyUpdates(keysToRemove, keysToUpdate, expectedKeys);
        env.ExpectWorkerFsFileExists(TChunkPath::GetCollectionStatsPath(), false/*exists*/, 0/*minSize*/);
    }

    // enable MeasureStats
    data.GetExpectedKeysAndTimestamp(&expectedKeys, nullptr);
    env.RecreateChunkCollection(expectedKeys, nullptr, {
        .NumberOfChunks = nChunks,
        .DeltaChunks = chunksToRebuild,
        .MeasureStats = true
    });

    for (size_t i = 0; i < 5; ++i) {
        data.NextIteration(&keysToRemove, &keysToUpdate, &expectedKeys);
        env.ApplyUpdates(keysToRemove, keysToUpdate, expectedKeys);
        env.ExpectWorkerFsFileExists(TChunkPath::GetCollectionStatsPath(), true/*exists*/, 50/*minSize*/);
    }
}

TEST(TChunkCollectionTest, InitThreads) {
    const ui32 nChunks = 20;
    const ui32 chunksToRebuild = 1;
    TTestEnvironment env({
        .NumberOfChunks = nChunks,
        .DeltaChunks = chunksToRebuild,
        .BodylessMode = true,
        .ExternalStorage = true,
        .InitThreads = 4
    });
    TRandomDataSource data(1, 1000000, 10, 20, 43);

    TVector<ui32> keysToRemove;
    TVector<ui32> keysToUpdate;
    TVector<ui32> expectedKeys;

    for (size_t i = 0; i < 10; ++i) {
        data.NextIteration(&keysToRemove, &keysToUpdate, &expectedKeys);
        env.ApplyUpdates(keysToRemove, keysToUpdate, expectedKeys);
    }

    env.RecreateChunkCollection(expectedKeys, nullptr, {
        .NumberOfChunks = nChunks,
        .DeltaChunks = chunksToRebuild
    });
    for (size_t i = 0; i < 100; ++i) {
        data.NextIteration(&keysToRemove, &keysToUpdate, &expectedKeys);
        env.ApplyUpdates(keysToRemove, keysToUpdate, expectedKeys);
    }

    env.RecreateChunkCollection(expectedKeys, nullptr, {
        .NumberOfChunks = nChunks,
        .DeltaChunks = chunksToRebuild
    });
    for (size_t i = 0; i < 100; ++i) {
        data.NextIteration(&keysToRemove, &keysToUpdate, &expectedKeys);
        env.ApplyUpdates(keysToRemove, keysToUpdate, expectedKeys);
    }
}

TEST(TChunkCollectionTest, InitShardNumberZero) {
    const ui32 nChunks = 20;
    const ui32 chunksToRebuild = 1;
    TTestEnvironment env({
        .NumberOfChunks = nChunks,
        .DeltaChunks = chunksToRebuild,
        .EnablePudge2 = true
    });
    TRandomDataSource data(1, 1000000, 10, 20, 43);

    TVector<ui32> keysToRemove;
    TVector<ui32> keysToUpdate;
    TVector<ui32> expectedKeys;

    data.NextIteration(&keysToRemove, &keysToUpdate, &expectedKeys);
    env.ApplyUpdates(keysToRemove, keysToUpdate, expectedKeys);
    ASSERT_EQ(env.GetChunkCount(), 2u);

    env.RecreateChunkCollection(expectedKeys, nullptr, {
        .NumberOfChunks = nChunks,
        .DeltaChunks = chunksToRebuild,
        .EnablePudge2 = true
    });
    ASSERT_EQ(env.GetChunkCount(), 2u);

    data.NextIteration(&keysToRemove, &keysToUpdate, &expectedKeys);
    env.ApplyUpdates(keysToRemove, keysToUpdate, expectedKeys);
    ASSERT_EQ(env.GetChunkCount(), 3u);

    env.RecreateChunkCollection(expectedKeys, nullptr, {
        .NumberOfChunks = nChunks,
        .DeltaChunks = chunksToRebuild,
        .EnablePudge2 = true
    });
    ASSERT_EQ(env.GetChunkCount(), 3u);
}

}

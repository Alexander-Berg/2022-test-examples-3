#include "runtime_chunk_builder.h"
#include "runtime_chunk_context.h"
#include "runtime_chunk_reader.h"
#include "runtime_chunk_writer.h"

#include <search/plutonium/core/state/current_state.h>
#include <search/plutonium/core/state/state_mutation.h>
#include <search/plutonium/impl/chunkler/chunk_collection.h>
#include <search/plutonium/impl/chunkler/helpers.h>
#include <search/plutonium/impl/chunkler/multi_lump_packer.h>
#include <search/plutonium/impl/chunkler/test_helpers/collection_helpers.h>
#include <search/plutonium/impl/chunkler/test_helpers/random_data_source.h>
#include <search/plutonium/impl/state/id_generator/factory.h>
#include <search/plutonium/impl/state/local_disk_for_tests/local_disk_for_tests_state_machine.h>

#include <library/cpp/testing/gtest/gtest.h>

#include <util/digest/city.h>
#include <util/folder/tempdir.h>

namespace NPlutonium::NWorkers {

static constexpr ui32 defaultItemType = 0;

struct TTestEnvironment {
    TTestEnvironment() {
        TLocalDiskForTestsStateMachine::InitializeWorkingDir(WorkDir_.Path());
        StateMachine_ = MakeHolder<NPlutonium::TLocalDiskForTestsStateMachine>(
            WorkDir_.Path(),
            CreateStateIdGenerator("numeric"));
    }

    ~TTestEnvironment() {
        StateMachine_.Reset();
    }

    void CreateNewState(const TStateModifierFunction& callback) {
        ApplyStateMutation(StateMachine_.Get(), callback);
    }

    const TCurrentState* CurrentState() const {
        return StateMachine_->CurrentState();
    }

private:
    TTempDir WorkDir_;
    THolder<IStateMachine> StateMachine_;
};

struct TDocFetcher {
    TDocFetcher(TRuntimeChunkReaderContextPtr ctx, ui32 itemType)
        : Ctx_(std::move(ctx))
        , ItemType_(itemType)
    {
    }

    TDocFetcher(const TDocFetcher&) = delete;

    explicit operator bool() const noexcept {
        return true;
    }

    TBlob operator()(ui32 chunkId, ui32 docId) const {
        const TRuntimeChunkReader& reader = GetOrCreateReader(chunkId);
        NChunkler::TMultiLumpPacker packer;
        reader.EnumerateLumps(docId, [&packer](TStringBuf lumpId, TArrayRef<const ui8> lumpBody) {
            if (lumpId == "check_sum.struct"sv || lumpBody.empty()) {
                return;
            }
            packer.AddLump(lumpId, lumpBody);
        });
        const TArrayRef<const ui8> packed = packer.Finish();
        return TBlob::Copy(packed.data(), packed.size());
    }

    void CheckItemKey(ui32 chunkId, ui32 docId, NChunkler::TDocKey expectedDocKey) const {
        const TRuntimeChunkReader& reader = GetOrCreateReader(chunkId);
        Y_ENSURE(expectedDocKey.size() == sizeof(ui32));
        NDoom::NItemStorage::TItemKey expectedItemKey = ReadUnaligned<ui32>(expectedDocKey.data());
        ASSERT_EQ(reader.GetItemKey(docId), expectedItemKey);
    }

private:
    const TRuntimeChunkReader& GetOrCreateReader(ui32 chunkId) const {
        auto inserted = ChunkReaders_.try_emplace(chunkId, Ctx_, ItemType_, chunkId);
        return inserted.first->second;
    }

    const TRuntimeChunkReaderContextPtr Ctx_;
    const ui32 ItemType_;
    mutable THashMap<ui32, TRuntimeChunkReader> ChunkReaders_;
};

NDoom::NItemStorage::TItemKey CalcItemKey(TStringBuf s) {
    uint128 hash128 = CityHash128(s.data(), s.size());
    return {hash128.first, hash128.second};
}

using TTestDataRow = std::map<TString, TString>;

void WriteChunk(TRuntimeChunkWriterContextPtr ctx, ui32 itemType, ui32 chunkId, const TVector<TTestDataRow>& chunkData) {
    TRuntimeChunkWriter chunkWriter(std::move(ctx), itemType, chunkId);
    for (size_t i = 0; i < chunkData.size(); ++i) {
        const auto& row = chunkData[i];
        Y_ENSURE(row.contains("key"));
        const NDoom::NItemStorage::TItemKey key = CalcItemKey(row.at("key"));
        for (auto& [lumpId, lumpBody] : row) {
            chunkWriter.AddLump(i, key, lumpId, NChunkler::AsBytesRef(lumpBody));
        }
    }
    chunkWriter.Finish();
}

void ReadAndCheckChunk(TRuntimeChunkReaderContextPtr ctx, ui32 itemType, ui32 chunkId, const TVector<TTestDataRow>& expectedData) {
    TRuntimeChunkReader reader(std::move(ctx), itemType, chunkId);
    ASSERT_EQ(reader.Size(), expectedData.size());
    for (ui32 i = 0; i < expectedData.size(); ++i) {
        const auto& expectedRow = expectedData[i];
        ui32 hitCount = 0;
        TMaybe<NDoom::NItemStorage::TItemKey> expectedKey;
        reader.EnumerateLumps(i, [&expectedRow, &hitCount, &expectedKey](TStringBuf lumpId, TArrayRef<const ui8> lumpBody) {
            if (lumpId == "check_sum.struct"sv) {
                return;
            }
            if (lumpId == "key"sv) {
                expectedKey = CalcItemKey(NChunkler::AsStrBuf(lumpBody));
            }
            ASSERT_TRUE(expectedRow.contains(ToString(lumpId)));
            ASSERT_EQ(expectedRow.at(ToString(lumpId)), NChunkler::AsStrBuf(lumpBody));
            ++hitCount;
        });
        ASSERT_EQ(hitCount, expectedRow.size());
        ASSERT_TRUE(expectedKey.Defined());
        if (expectedKey.Defined()) {
            ASSERT_EQ(*expectedKey, reader.GetItemKey(i));
        }
    }
}

template <typename TSomeState>
auto CreateContext(TSomeState* state, TString stream, TVector<TString> lumpIds) {
    using TWorkerFsType = typename std::remove_pointer<decltype(state->WorkerFs)>::type;
    using TRuntimeFsType = typename std::remove_pointer<decltype(state->RuntimeFs)>::type;
    static_assert(std::is_same_v<TWorkerFsType, TRuntimeFsType>);
    using TContext = TRuntimeChunkContext<TRuntimeFsType>;
    return MakeSimpleShared<TContext>(TContext{
        .Stream = std::move(stream),
        .StateId = state->Id,
        .WorkerFs = state->WorkerFs,
        .RuntimeFs = state->RuntimeFs,
        .LumpIds = std::move(lumpIds)
    });
}

TEST(RuntimeChunk, SimpleReadAndWriteSingleChunk) {
    const TString testStream = "test_stream";
    const TVector<TString> testLumpIds{"key", "lump1"};
    const ui32 testChunkId = 10;
    const TVector<TTestDataRow> testData{
        {{"key", "42"}, {"lump1", "something"}},
        {{"key", "43"}, {"lump1", "anything"}},
        {{"key", "qwerty"}, {"lump1", "whatever"}},
        {{"key", "uiop"}, {"lump1", ""}},
        {{"key", ""}, {"lump1", "nothing"}}
    };

    TTestEnvironment env;
    env.CreateNewState([&](const TCurrentState*, TNewState* newState) {
        TRuntimeChunkWriterContextPtr wrContext = CreateContext(newState, testStream, testLumpIds);
        WriteChunk(wrContext, defaultItemType, testChunkId, testData);
        ReadAndCheckChunk(wrContext->ReadOnly(), defaultItemType, testChunkId, testData);
    });
    ReadAndCheckChunk(CreateContext(env.CurrentState(), testStream, testLumpIds), defaultItemType, testChunkId, testData);
}

TEST(RuntimeChunk, SimpleReadAndWriteTwoChunks) {
    const TString testStream = "test_stream";
    const TVector<TString> testLumpIds{"key", "lump1"};

    const ui32 testChunkId1 = 10;
    const TVector<TTestDataRow> chunk1{
        {{"key", "42"}, {"lump1", "something"}},
        {{"key", "43"}, {"lump1", "anything"}},
        {{"key", "qwerty"}, {"lump1", "whatever"}},
        {{"key", "uiop"}, {"lump1", ""}},
        {{"key", ""}, {"lump1", "nothing"}}
    };
    const ui32 testChunkId2 = 20;
    const TVector<TTestDataRow> chunk2{
        {{"key", "42"}, {"lump1", "abc"}},
        {{"key", "2324"}, {"lump1", "def"}},
        {{"key", "qwerty"}, {"lump1", "something else"}},
        {{"key", "ert"}, {"lump1", "ert_value"}},
        {{"key", "uiop"}, {"lump1", "second"}}
    };

    TTestEnvironment env;
    env.CreateNewState([&](const TCurrentState*, TNewState* newState) {
        TRuntimeChunkWriterContextPtr wrContext = CreateContext(newState, testStream, testLumpIds);

        WriteChunk(wrContext, defaultItemType, testChunkId1, chunk1);
        WriteChunk(wrContext, defaultItemType, testChunkId2, chunk2);

        TRuntimeChunkReaderContextPtr readContext = wrContext->ReadOnly();
        ReadAndCheckChunk(readContext, defaultItemType, testChunkId1, chunk1);
        ReadAndCheckChunk(readContext, defaultItemType, testChunkId2, chunk2);
    });
    TRuntimeChunkReaderContextPtr readContext = CreateContext(env.CurrentState(), testStream, testLumpIds);
    ReadAndCheckChunk(readContext, defaultItemType, testChunkId1, chunk1);
    ReadAndCheckChunk(readContext, defaultItemType, testChunkId2, chunk2);
}

TEST(RuntimeChunk, TreatNonExistentChunkAsEmpty) {
    const TString testStream = "test_stream";
    const TVector<TString> testLumpIds{"key", "lump1"};
    const ui32 testChunkId = 10;

    TTestEnvironment env;
    ReadAndCheckChunk(CreateContext(env.CurrentState(), testStream, testLumpIds), defaultItemType, testChunkId, {});
}

void UpdateCollection(
    const TVector<ui32>& keysToRemove,
    const TVector<ui32>& keysToUpdate,
    const TVector<ui32>& expectedKeys,
    const ui32 itemType,
    TTestEnvironment* env,
    NChunkler::TChunkCollection* collection
) {
    Y_ENSURE(env != nullptr);
    Y_ENSURE(collection != nullptr);

    const NChunkler::TChunkCollectionConfig& collectionConfig = collection->GetConfig();

    env->CreateNewState([&](const TCurrentState* currentState, TNewState* newState) {
        constexpr bool genMultipleLumps = true;
        const auto toRemove = NChunkler::TCollectionHelpers::ConvertKeys(keysToRemove);
        const auto toUpdate = NChunkler::TCollectionHelpers::ConvertKeys(keysToUpdate);
        const auto generatedBodies = NChunkler::TCollectionHelpers::GenerateDocsByKeys(keysToUpdate, genMultipleLumps, collectionConfig.LumpIds);
        const auto convertedBodies = NChunkler::TCollectionHelpers::ConvertBodies(generatedBodies);
        TVector<NDoom::NItemStorage::TItemKey> itemKeys(Reserve(toUpdate.size()));
        for (ui32 updKey : keysToUpdate) {
            itemKeys.push_back(updKey);
        }

        ASSERT_EQ(toUpdate.size(), convertedBodies.size());
        for (size_t i = 0; i < toUpdate.size(); ++i) {
            NChunkler::TCollectionHelpers::CheckKeyBodyPair(toUpdate[i], convertedBodies[i], nullptr, genMultipleLumps, collectionConfig.LumpIds);
        }

        TRuntimeChunkReaderContextPtr readContext = CreateContext(currentState, collectionConfig.Stream, collectionConfig.LumpIds);
        TRuntimeChunkWriterContextPtr wrContext = CreateContext(newState, collectionConfig.Stream, collectionConfig.LumpIds);

        THolder<NChunkler::IExternalChunkBuilder> externalBuilder = CreateRuntimeChunkBuilder(
            readContext,
            wrContext,
            itemType,
            convertedBodies,
            std::move(itemKeys),
            genMultipleLumps ? ESerializedProfileFormat::MultipleLumps : ESerializedProfileFormat::SingleLump);

        collection->ApplyUpdates(
            toRemove,
            toUpdate,
            convertedBodies,
            genMultipleLumps,
            currentState->WorkerFs,
            newState->WorkerFs,
            TInstant::Now(),
            nullptr,
            externalBuilder.Get());

        TRuntimeChunkReaderContextPtr newReadContext = CreateContext(newState, collectionConfig.Stream, collectionConfig.LumpIds)->ReadOnly();
        TDocFetcher docFetcher(std::move(newReadContext), itemType);
        NChunkler::TCollectionHelpers::CheckCollection(
            collection,
            expectedKeys,
            nullptr/*expectedTimestamps*/,
            nullptr/*workerFs*/,
            [&docFetcher](ui32 chunkId, ui32 docId) {
                return docFetcher(chunkId, docId);
            },
            nullptr);

        for (ui32 chunkId = 0, maxChunk = collection->GetAllKeys().size(); chunkId < maxChunk; ++chunkId) {
            const NChunkler::TKeysView& keysView = collection->GetAllKeys()[chunkId];
            for (ui32 docId = 0; docId < keysView.Size(); ++docId) {
                docFetcher.CheckItemKey(chunkId, docId, keysView[docId]);
            }
        }
    });
}

TEST(RuntimeChunk, ChunkCollectionWithExternalBuilder) {
    const ui32 nChunks = 10;
    const ui32 deltaChunks = 2;
    const ui32 defaultItemType = 23;
    const TVector<TString> testLumpIds{"lump1", "lump2"};
    NChunkler::TRandomDataSource data(1, 1000000, 10, 20, 42);

    NChunkler::TChunkCollectionConfig collectionConfig;
    collectionConfig.Stream = "test_stream";
    collectionConfig.CollectionPathPrefix = "chunk";
    collectionConfig.TierConfig = NJupiter::TTierConfig(
        nChunks, /* number of chunks */
        deltaChunks, /* min. chunks to rebuild */
        0.2, /* max. additional space */
        0 /* max. largest to smallest chunk ratio */
    );
    collectionConfig.LumpIds = testLumpIds;
    collectionConfig.MultipleLumps = true;
    collectionConfig.BodylessMode = true;
    NChunkler::TChunkCollection collection(collectionConfig);

    TTestEnvironment env;
    collection.SyncWithCurrentState(env.CurrentState()->WorkerFs);

    TVector<ui32> keysToRemove;
    TVector<ui32> keysToUpdate;
    TVector<ui32> expectedKeys;
    for (size_t i = 0; i < 100; ++i) {
        data.NextIteration(&keysToRemove, &keysToUpdate, &expectedKeys);
        UpdateCollection(keysToRemove, keysToUpdate, expectedKeys, defaultItemType, &env, &collection);
    }
}

TVector<TString> CollectLumpIds(const TCurrentState* state, const TString& stream, ui32 nChunks, ui32 itemType) {
    THashSet<TString> lumpIdsSet;
    TRuntimeChunkReaderContextPtr readContext = CreateContext(state, stream, {});
    for (ui32 i = 0; i < nChunks; ++i) {
        TRuntimeChunkReader reader(readContext, itemType, i);
        auto chunkLumpIds = reader.GetLumpIds();
        for (TStringBuf lumpId : chunkLumpIds) {
            lumpIdsSet.insert(ToString(lumpId));
        }
    }
    TVector<TString> lumpIds{lumpIdsSet.begin(), lumpIdsSet.end()};
    Sort(lumpIds);
    return lumpIds;
}

TEST(RuntimeChunk, ExternalBuilderChangingLumpIds) {
    const ui32 nChunks = 10;
    const ui32 deltaChunks = 2;
    const ui32 defaultItemType = 23;
    NChunkler::TRandomDataSource data(1, 1000000, 10, 20, 42);

    NChunkler::TChunkCollectionConfig collectionConfig;
    collectionConfig.Stream = "test_stream";
    collectionConfig.CollectionPathPrefix = "chunk";
    collectionConfig.TierConfig = NJupiter::TTierConfig(
        nChunks, /* number of chunks */
        deltaChunks, /* min. chunks to rebuild */
        0.2, /* max. additional space */
        0 /* max. largest to smallest chunk ratio */
    );
    collectionConfig.LumpIds = TVector<TString>{"lump1", "lump2"};
    collectionConfig.MultipleLumps = true;
    collectionConfig.BodylessMode = true;

    TMaybe<NChunkler::TChunkCollection> collection;
    collection.ConstructInPlace(collectionConfig);

    TTestEnvironment env;
    collection->SyncWithCurrentState(env.CurrentState()->WorkerFs);

    TVector<ui32> keysToRemove;
    TVector<ui32> keysToUpdate;
    TVector<ui32> expectedKeys;
    for (size_t i = 0; i < nChunks * 2; ++i) {
        data.NextIteration(&keysToRemove, &keysToUpdate, &expectedKeys);
        UpdateCollection(keysToRemove, keysToUpdate, expectedKeys, defaultItemType, &env, collection.Get());
    }

    collectionConfig.LumpIds = TVector<TString>{"lump1", "lump5"};
    collection.ConstructInPlace(collectionConfig);
    collection->SyncWithCurrentState(env.CurrentState()->WorkerFs);

    for (size_t i = 0; i < nChunks * 2; ++i) {
        data.NextIteration(&keysToRemove, &keysToUpdate, &expectedKeys);
        UpdateCollection(keysToRemove, keysToUpdate, expectedKeys, defaultItemType, &env, collection.Get());
    }

    const TVector<TString> lumpIds = CollectLumpIds(env.CurrentState(), collectionConfig.Stream, nChunks, defaultItemType);
    const TVector<TString> expectedLumpIds{"check_sum.struct", "lump1", "lump2", "lump5"};
    ASSERT_EQ(lumpIds, expectedLumpIds);
}

}

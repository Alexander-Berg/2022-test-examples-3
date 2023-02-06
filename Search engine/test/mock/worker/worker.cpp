#include "worker.h"

#include <kernel/doom/item_storage/test/generate.h>
#include <kernel/doom/item_storage/test/index.h>
#include <kernel/doom/item_storage/test/write.h>

#include <kernel/doom/erasure/identity.h>
#include <kernel/doom/erasure/wad_writer.h>

#include <search/base/blob_storage/config/protos/remote_chunked_blob_storage_index_config.pb.h>
#include <search/base_search/rs_proxy/protos/shard_config.pb.h>
#include <search/plutonium/impl/state/id_generator/iso8601/iso8601_state_id_generator.h>

#include <util/generic/deque.h>
#include <util/string/printf.h>

#include <library/cpp/iterator/enumerate.h>

#include <google/protobuf/text_format.h>

namespace NBlobStorage::NProxy::NMock {

TFsPath CreateChild(const TFsPath& path, const TString& name) {
    TFsPath res = path.Child(name);
    res.MkDirs();
    return res;
}

using namespace NDoom::NItemStorage;
using namespace NDoom::NItemStorage::NTest;

struct TRemoteStorageItemWriterFactoryOptions {
    TFsPath Prefix;
    TString Stream;
    TString SnapshotId;
    TItemType ItemType = {};
    NBlobStorage::NProxy::TItemTypeConfig* ResultConfig = nullptr;
    TSnapshotResources* Resources = nullptr;
};

class TRemoteStorageItemWriterFactory final : public NTest::IItemWriterFactory {
    inline static constexpr TStringBuf ChunkConfName = "chunk.conf";
    inline static constexpr TStringBuf ChunkDataWadName = "chunk.data.wad";

    struct TChunkInfo {
        TChunkId Id;
        TString Uuid;
    };

public:
    TRemoteStorageItemWriterFactory(TRemoteStorageItemWriterFactoryOptions options)
        : Prefix_{std::move(options.Prefix)}
        , Stream_{std::move(options.Stream)}
        , SnapshotId_{std::move(options.SnapshotId)}
        , ItemType_{std::move(options.ItemType)}
        , ResultConfig_{options.ResultConfig}
        , Resources_{options.Resources}
    {}

    TWadItemStorageGlobalWriter MakeGlobalWriter() override {
        return TWadItemStorageGlobalWriter{CreateChild(WorkerPath(), "global")};
    }

    TWadItemStorageMappingWriter MakeMappingWriter() override {
        TVector<TBlob> chunkKeys;
        for (TChunkInfo chunk : Chunks_) {
            TFileInput in{ChunkKeysPath(chunk.Id)};
            chunkKeys.emplace_back(TBlob::FromStreamSingleThreaded(in));
        }

        return TWadItemStorageMappingWriter{chunkKeys, OpenFile(MinHashMappingPath())};
    }

    TWadItemStorageChunkWriter MakeChunkWriter(TChunkId chunk, TString uuid) override {
        Chunks_.push_back(TChunkInfo{.Id  = chunk, .Uuid = uuid});
        WriteChunkConf(chunk);
        FillChunkResource(chunk);

        IOutputStream* mappingOutput = OpenFile(ErasureMappingPath(chunk));
        IOutputStream* chunkOutput = OpenFile(ChunkDataPath(chunk));
        IOutputStream* itemsOutput = OpenFile(ChunkKeysPath(chunk));

        auto& erasureWriter = ErasureWriters_.emplace_back();
        erasureWriter.Reset({mappingOutput}, {chunkOutput}, false);

        return TWadItemStorageChunkWriter{erasureWriter.GetWriterAdapter(0), itemsOutput};
    }

    void FillChunkResource(TChunkId chunk) {
        TFsPath rootPath = Prefix_ / "chunks";
        TFsPath localPath = ChunkPath(chunk).RelativeTo(rootPath);

        Resources_->Chunks[chunk] = TResource{
            .Namespace = "chunks",
            .RootPath = rootPath,
            .LocalPath = localPath,
        };
    }

    void WriteChunkConf(TChunkId chunk) {
        auto path = ChunkConfPath(chunk);

        TRemoteBlobStorageChunkConfig config;
        config.SetNamespace(Stream_);
        config.SetStateId(SnapshotId_);
        config.SetItemType(chunk.ItemType);
        config.SetId(chunk.Id);
        config.MutablePath()->assign(ChunkDataWadName);

        TString str;
        ::google::protobuf::TextFormat::PrintToString(config, &str);

        path.Parent().MkDirs();
        TFileOutput{path}.Write(str);
    }

    TVector<TFsPath> ListChunkItemWads() override {
        TVector<TFsPath> wads;
        for (ui32 i = 0; true; ++i) {
            TFsPath items = ChunkKeysPath(TChunkId{.Id = i});
            if (!items.Exists()) {
                break;
            }
            wads.push_back(items);
        }
        return wads;
    }

    void FinishChunks() override {
        for (auto& writer : ErasureWriters_) {
            writer.Finish();
        }

        for (auto& output : FileOutputs_) {
            output.Finish();
        }

        WriteItemConfig();
    }

    void WriteItemConfig() {
        Y_ENSURE(!Chunks_.empty());
        TItemType itemType = Chunks_[0].Id.ItemType;
        for (auto [i, chunk] : Enumerate(Chunks_)) {
            Y_ENSURE(chunk.Id.Id == i);
            Y_ENSURE(chunk.Id.ItemType == itemType);
        }

        ResultConfig_->SetItemType(itemType);
        ResultConfig_->SetNumChunks(Chunks_.size());

        for (auto&& chunk : Chunks_) {
            ResultConfig_->AddChunkRevisions()->SetUuid(chunk.Uuid);
        }
    }

    IOutputStream* OpenFile(const TFsPath& path) {
        path.Parent().MkDirs();
        return &FileOutputs_.emplace_back(path);
    }

private:
    TFsPath MappingPath() {
        return Prefix_ / "mappings" / Stream_ / SnapshotId_ / "shard" / ToString(ItemType_);
    }

    TFsPath ChunksPath() {
        return Prefix_ / "chunks" / Stream_ / SnapshotId_ / "chunk" / ToString(ItemType_);
    }

    TFsPath WorkerPath() {
        return Prefix_ / "worker" / Stream_ / SnapshotId_ / ToString(ItemType_);
    }

    TFsPath ErasureMappingPath(TChunkId chunk) {
        return MappingPath() / Sprintf("%d.mapping.wad", chunk.Id);
    }

    TFsPath ChunkPath(TChunkId chunk) {
        return ChunksPath() / ToString(chunk.Id);
    }

    TFsPath ChunkDataPath(TChunkId chunk) {
        return ChunkPath(chunk) / ChunkDataWadName;
    }

    TFsPath ChunkConfPath(TChunkId chunk) {
        return ChunkPath(chunk) / "chunk.conf";
    }

    TFsPath ChunkKeysPath(TChunkId chunk) {
        return WorkerPath() / (ToString(chunk.Id) + ".items");
    }

    TFsPath MinHashMappingPath() {
        return MappingPath() / "item_chunk.mapping.wad";
    }

private:
    TFsPath Prefix_;

    TString Stream_;
    TString SnapshotId_;
    TItemType ItemType_ = {};

    TDeque<TFileOutput> FileOutputs_;
    TDeque<NDoom::TErasureWadWriter<NDoom::TIdentityCodec<1>>> ErasureWriters_;

    TVector<TChunkInfo> Chunks_;

    NBlobStorage::NProxy::TItemTypeConfig* ResultConfig_ = nullptr;
    TSnapshotResources* Resources_ = nullptr;
};

class TRemoteStorageWriterFactory final : public IWriterFactory {
public:
    TRemoteStorageWriterFactory(TFsPath prefix, TString stream, TString snapshotId, TSnapshotResources* resources)
        : Prefix_{std::move(prefix)}
        , Stream_{std::move(stream)}
        , SnapshotId_{std::move(snapshotId)}
        , Resources_{resources}
    {
        ShardConfig_.SetStateId(SnapshotId_);
        ShardConfig_.SetNamespace(Stream_);
        ShardConfig_.SetStartBuildTimestamp(Now().ToString());
    }

    THolder<IItemWriterFactory> MakeItemWriter(TItemType itemType) override {
        return THolder(new TRemoteStorageItemWriterFactory{TRemoteStorageItemWriterFactoryOptions{
            .Prefix = Prefix_,
            .Stream = Stream_,
            .SnapshotId = SnapshotId_,
            .ItemType = itemType,
            .ResultConfig = ShardConfig_.AddItemTypes(),
            .Resources = Resources_,
        }});
    }

    void Finish() override {
        ShardConfig_.SetFinishBuildTimestamp(Now().ToString());

        WriteShardConfig();
        FillMappingsResource();
    }

    void WriteShardConfig() {
        TString str;
        ::google::protobuf::TextFormat::PrintToString(ShardConfig_, &str);

        TFileOutput{MappingShardPath() / "shard.conf"}.Write(str);
    }

    void FillMappingsResource() {
        TFsPath rootPath = Prefix_ / "mappings";
        TFsPath localPath = MappingShardPath().Parent().RelativeTo(rootPath);

        Resources_->Mappings = TResource{
            .Namespace = "mappings",
            .RootPath = rootPath,
            .LocalPath = localPath,
        };
    }

    TFsPath MappingShardPath() {
        return Prefix_ / "mappings" / Stream_ / SnapshotId_ / "shard";
    }

private:
    TFsPath Prefix_;
    TString Stream_;
    TString SnapshotId_;
    NBlobStorage::NProxy::TShardConfig ShardConfig_;

    TSnapshotResources* Resources_ = nullptr;
};

void WriteIndex(TSnapshot& snapshot, const TFsPath& folder) {
    TRemoteStorageWriterFactory writers{folder, snapshot.Stream, snapshot.Id, &snapshot.Resources};
    NTest::WriteIndex(snapshot.Index, &writers);
}


TWorker::TWorker(TString stream, TFsPath path, TIndexParams params)
    : Stream_{std::move(stream)}
    , Path_{std::move(path)}
    , Params_{std::move(params)}
{
}

// /storage/mappings/<stream>/<snapshot>/shard/<item type>/<chunk id>.mapping.wad
// /storage/  chunks/<stream>/<snapshot>/chunk/<item type>/<chunk id>/chunk.conf
TSnapshot TWorker::RunIteration(const TMaybe<TSnapshot>& prevSnapshot, double deltaChunksRatio) {
    Y_VERIFY(deltaChunksRatio == 1.0, "Unimplemented");
    Y_VERIFY(prevSnapshot == Nothing(), "Unimplemented");

    TSnapshotId snapshotId = GenerateSnapshotId(prevSnapshot ? MakeMaybe(prevSnapshot->Id) : Nothing());
    // TFsPath path = GetSnapshotPath(snapshotId);
    // path.MkDirs();

    TIndexData data = GenerateIndex(Params_);

    TSnapshot snapshot{
        .Stream = Stream_,
        .Id = snapshotId,
        .Index = std::move(data),
    };

    WriteIndex(snapshot, Path_);

    return snapshot;
}

TFsPath TWorker::GetSnapshotPath(const TSnapshotId& snapshot) const {
    return Path_.Child(Stream_).Child(snapshot);
}

TSnapshotId TWorker::GenerateSnapshotId(const TMaybe<TSnapshotId>& prevSnapshotId) {
    return NPlutonium::TIso8601StateIdGenerator{}.GenerateNewStateId(prevSnapshotId.GetOrElse(""), Now());
}

} // namespace NBlobStorage::NProxy::NMock

#pragma once

#include <kernel/doom/item_storage/test/generate.h>

#include <search/base_search/rs_proxy/test/mock/deploy/resource.h>

#include <util/folder/path.h>
#include <util/generic/maybe.h>

namespace NBlobStorage::NProxy::NMock {

using namespace NDoom::NItemStorage;
using namespace NDoom::NItemStorage::NTest;

using TSnapshotId = TString;

struct TSnapshotResources {
    TResource Mappings;
    THashMap<NDoom::NItemStorage::TChunkId, TResource> Chunks;
};

struct TSnapshot {
    TString Stream;
    TSnapshotId Id;

    TIndexData Index;
    TSnapshotResources Resources;
};

class TWorker {
public:
    TWorker(TString stream, TFsPath path, TIndexParams params);

    TSnapshot RunIteration(const TMaybe<TSnapshot>& prevSnapshot = Nothing(), double deltaChunksRatio = 1.0);

private:
    TFsPath GetSnapshotPath(const TSnapshotId& snapshot) const;

    static TSnapshotId GenerateSnapshotId(const TMaybe<TSnapshotId>& prevSnapshotId);

private:
    TString Stream_;
    TFsPath Path_;
    TIndexParams Params_;
};

} // namespace NBlobStorage::NProxy::NMock

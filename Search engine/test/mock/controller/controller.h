#pragma once

#include <search/base/blob_storage/config/protos/topology.pb.h>
#include <search/base_search/rs_proxy/test/mock/deploy/service.h>
#include <search/base_search/rs_proxy/test/mock/remote_storage/remote_storage.h>
#include <search/base_search/rs_proxy/test/mock/remote_storage_proxy/remote_storage_proxy.h>
#include <search/base_search/rs_proxy/test/mock/worker/worker.h>

#include <library/cpp/iterator/zip.h>
#include <library/cpp/logger/log.h>

#include <util/generic/array_ref.h>
#include <util/string/printf.h>
#include <util/system/fs.h>

#include <google/protobuf/text_format.h>

namespace NBlobStorage::NProxy::NMock {

class TController {
    // FIXME(sskvor): template <typename ICodec>
    inline static constexpr size_t kReplicationFactor = 3;

public:
    TController(TFsPath workingDirectory)
        : WorkingDirectory_{std::move(workingDirectory)}
    {
        WorkingDirectory_.MkDirs();
        Log_ = TLog{WorkingDirectory_ / "log.txt"};
    }

    void RunIteration(TConstArrayRef<TSnapshot> snapshots, TArrayRef<TRemoteStorage> storages, TRemoteStorageProxy& proxy) {
        // FIXME(sskvor): Seed
        TReallyFastRng32 rng{1234};

        TVector<TVector<TResource>> chunks(storages.size());
        TVector<TResource> proxyResources;

        for (const TSnapshot& snapshot : snapshots) {
            PlanSnapshot(snapshot, storages, chunks, proxyResources, rng);
        }

        for (auto [rs, resources] : Zip(storages, chunks)) {
            MakeAvailable(rs, resources);
        }
        MakeAvailable(proxy, proxyResources);
    }

private:
    void PlanSnapshot(
        const TSnapshot& snapshot,
        TArrayRef<TRemoteStorage> storages,
        TArrayRef<TVector<TResource>> chunks,
        TVector<TResource>& proxyResources,
        TReallyFastRng32& rng)
    {
        Log_ << TLOG_INFO << "Planning shapshot " << snapshot.Stream << "/" << snapshot.Id << Endl;

        NBlobStorage::TStateTopology topology;
        topology.SetNamespace(snapshot.Stream);
        topology.SetState(snapshot.Id);

        for (auto&& [id, resource] : snapshot.Resources.Chunks) {
            TSet<size_t> replicas;
            while (replicas.size() < kReplicationFactor) {
                replicas.insert(rng() % storages.size());
            }

            for (size_t rs : replicas) {
                chunks[rs].push_back(resource);
                Log_ << TLOG_INFO << "Assigned chunk " << id << " to remote storage localhost:" << storages[rs].Port() << Endl;
            }

            auto* chunk = topology.AddChunks();
            chunk->MutableChunk()->SetId(id.Id);
            chunk->MutableChunk()->SetItemType(id.ItemType);

            chunk->SetType("");
            chunk->AddParts();
            for (size_t rs : replicas) {
                auto* replica = chunk->MutableParts(0)->AddReplicas();

                TString url = Sprintf("http://localhost:%d/remote_storage", storages[rs].Port());
                replica->SetUrl(std::move(url));
            }
        }

        proxyResources.push_back(snapshot.Resources.Mappings);
        proxyResources.push_back(WriteTopology(snapshot, topology));
    }

    TResource WriteTopology(const TSnapshot& snapshot, const NBlobStorage::TStateTopology& topology) {
        TFsPath root = WorkingDirectory_ / "topology";
        TFsPath localPath = TFsPath{snapshot.Stream} / snapshot.Id / "topology.1.conf";

        TFsPath path = root / localPath;
        path.Parent().MkDirs();

        TString str;
        ::google::protobuf::TextFormat::PrintToString(topology, &str);
        TFileOutput{path}.Write(str);

        return TResource{
            .Namespace = "topology",
            .RootPath = root,
            .LocalPath = localPath,
        };
    }

    void MakeAvailable(IService& service, TConstArrayRef<TResource> workerResources) {
        TVector<TResource> deployedResources = DeployResources(service.Storage(), workerResources);

        TVector<TStatus> statuses(deployedResources.size());
        service.Rescan(deployedResources, statuses);

        for (auto&& status : statuses) {
            Y_ENSURE(status.Valid);
        }
    }

    TVector<TResource> DeployResources(const TFsPath& root, TConstArrayRef<TResource> resources) {
        TVector<TResource> res(Reserve(resources.size()));
        for (const TResource& resource : resources) {
            auto deployedResource = DeployResource(root, resource);
            res.push_back(std::move(deployedResource));
        }
        return res;
    }

    TResource DeployResource(const TFsPath& root, const TResource& resource) {
        TFsPath rootPath = root / resource.Namespace / "__resources__";
        TFsPath dstPath = rootPath / resource.LocalPath;
        resource.RealPath().CopyTo(dstPath, true);
        return TResource{
            .Namespace = resource.Namespace,
            .RootPath = rootPath,
            .LocalPath = resource.LocalPath,
        };
    }

private:
    TFsPath WorkingDirectory_;
    TLog Log_;
};

} // namespace NBlobStorage::NProxy::NMock

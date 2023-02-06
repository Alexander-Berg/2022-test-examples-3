#pragma once


#include <search/plutonium/impl/file_system/local_disk_for_tests/local_disk_for_tests_file_system.h>
#include <search/plutonium/impl/file_system/local_disk_for_tests/local_disk_for_tests_read_only_file_system.h>

#include <search/plutonium/core/state/state_id_generator.h>
#include <search/plutonium/core/state/state_machine.h>

#include <library/cpp/json/json_reader.h>
#include <library/cpp/json/json_writer.h>
#include <library/cpp/yson/node/node_io.h>

#include <util/system/fs.h>



namespace NPlutonium {

NJson::TJsonValue NodeToJsonValue(const NYT::TNode& node);

class TLocalDiskForTestsStateMachine: public IStateMachine {
public:
    TLocalDiskForTestsStateMachine(TFsPath dir, THolder<IStateIdGenerator> stateIdGenerator, NFsCache::TCacheStorage wfsCache = {}, NFsCache::TCacheStorage rfsCache = {})
        : Dir_(std::move(dir))
        , StateIdGenerator_(std::move(stateIdGenerator))
        , WorkerFsCache_(std::move(wfsCache))
        , RuntimeFsCache_(std::move(rfsCache))
    {
        ConstructCurrentState();
    }

    const TCurrentState* CurrentState() const override {
        with_lock(Lock_) {
            return CurrentStateHolder_->CurrentState();
        }
    }

    THolder<IStateHolder> GetState(const TString& stateId) const override {
        return MakeHolder<TLocalDiskForTestsStateHolder>(Dir_, stateId,  WorkerFsCache_, RuntimeFsCache_);
    }

    TNewState* InitializeNewState() override {
        with_lock(Lock_) {
            Y_ENSURE(!NewState_);

            const TString currentStateId = CurrentStateHolder_->CurrentState()->Id;

            TString newStateId = StateIdGenerator_->GenerateNewStateId(currentStateId, TInstant::Now());
            Y_ENSURE(currentStateId < newStateId);

            auto [workerFsTree, runtimeFsTree] = CurrentStateHolder_->GetFsTrees();

            NewStateWorkerFsTree_ = MakeHolder<TFileSystemTree<TMetaInfo>>(*workerFsTree);
            NewStateWorkerFs_ = MakeHolder<TLocalDiskForTestsFileSystem>(NewStateWorkerFsTree_.Get(), Dir_ / "worker_content", WorkerFsCache_);

            NewStateRuntimeFsTree_ = MakeHolder<TFileSystemTree<TMetaInfo>>(*runtimeFsTree);
            NewStateRuntimeFs_ = MakeHolder<TLocalDiskForTestsFileSystem>(NewStateRuntimeFsTree_.Get(), Dir_ / "runtime_content", RuntimeFsCache_);

            NewState_ = MakeHolder<TNewState>();
            NewState_->Id = std::move(newStateId);
            NewState_->WorkerFs = NewStateWorkerFs_.Get();
            NewState_->RuntimeFs = NewStateRuntimeFs_.Get();

            TNewState* newState = NewState_.Get();
            newState->Statistic.SetWorkerProcessingStartTimestamp(TInstant::Now().Seconds());
            return newState;
        }
    }

    void FinalizeAndSwitchToNewState() override {
        with_lock(Lock_) {
            Y_ENSURE(NewState_);

            const TFsPath newStateDir = Dir_ / "states" / NewState_->Id;
            NFs::RemoveRecursive(newStateDir);
            newStateDir.MkDirs();

            NewStateWorkerFs_.Reset();
            NewStateRuntimeFs_.Reset();

            SerializeFileSystemTree(*NewStateWorkerFsTree_, newStateDir / "__worker_file_system_tree__");
            SerializeFileSystemTree(*NewStateRuntimeFsTree_, newStateDir / "__runtime_file_system_tree__");

            TOFStream(Dir_ / "__current_state__").Write(NewState_->Id);

            CurrentStateHolder_->SwitchToNewState(
                Dir_,
                std::move(NewState_->Id),
                std::move(NewStateWorkerFsTree_),
                std::move(NewStateRuntimeFsTree_));

            NewStateWorkerFsTree_.Reset();
            NewStateRuntimeFsTree_.Reset();

            NewState_.Reset();
        }
    }

    static void InitializeWorkingDir(const TFsPath& dir, const TString& initialStateId = "0");

private:
    class TLocalDiskForTestsStateHolder : public IStateHolder {
    public:
        TLocalDiskForTestsStateHolder(const TFsPath& rootDir, TString stateId, NFsCache::TCacheStorage wfsCache, NFsCache::TCacheStorage rfsCache)
            : WorkerFsCache_(std::move(wfsCache))
            , RuntimeFsCache_(std::move(rfsCache))
        {
            const TFsPath currentStateDir = rootDir / "states" / stateId;
            SwitchToNewState(
                rootDir,
                std::move(stateId),
                MakeHolder<TFileSystemTree<TMetaInfo>>(DeserializeFileSystemTree(currentStateDir / "__worker_file_system_tree__")),
                MakeHolder<TFileSystemTree<TMetaInfo>>(DeserializeFileSystemTree(currentStateDir / "__runtime_file_system_tree__"))
            );
        }

        const TCurrentState* CurrentState() const final {
            return CurrentState_.Get();
        }

        std::pair<const TFileSystemTree<TMetaInfo>*, const TFileSystemTree<TMetaInfo>*> GetFsTrees() const {
            return {CurrentStateWorkerFsTree_.Get(), CurrentStateRuntimeFsTree_.Get()};
        }

        void SwitchToNewState(
            const TFsPath& rootDir,
            TString id,
            THolder<TFileSystemTree<TMetaInfo>> newStateWorkerFsTree,
            THolder<TFileSystemTree<TMetaInfo>> newStateRuntimeFsTree
        ) {
            CurrentStateWorkerFsTree_ = std::move(newStateWorkerFsTree);
            CurrentStateWorkerFs_ = MakeHolder<TLocalDiskForTestsReadOnlyFileSystem>(CurrentStateWorkerFsTree_.Get(), rootDir / "worker_content", WorkerFsCache_);

            CurrentStateRuntimeFsTree_ = std::move(newStateRuntimeFsTree);
            CurrentStateRuntimeFs_ = MakeHolder<TLocalDiskForTestsReadOnlyFileSystem>(CurrentStateRuntimeFsTree_.Get(), rootDir / "runtime_content", RuntimeFsCache_);

            CurrentState_ = MakeHolder<TCurrentState>();
            CurrentState_->Id = std::move(id);
            CurrentState_->WorkerFs = CurrentStateWorkerFs_.Get();
            CurrentState_->RuntimeFs = CurrentStateRuntimeFs_.Get();
        }

    private:
        NFsCache::TCacheStorage WorkerFsCache_;
        NFsCache::TCacheStorage RuntimeFsCache_;
        THolder<TFileSystemTree<TMetaInfo>> CurrentStateWorkerFsTree_;
        THolder<TLocalDiskForTestsReadOnlyFileSystem> CurrentStateWorkerFs_;
        THolder<TFileSystemTree<TMetaInfo>> CurrentStateRuntimeFsTree_;
        THolder<TLocalDiskForTestsReadOnlyFileSystem> CurrentStateRuntimeFs_;
        THolder<TCurrentState> CurrentState_;
    };

    TCurrentStateInfo ReadCurrentState() const {
        const TString currentStateId = TIFStream(Dir_ / "__current_state__").ReadAll();
        return TCurrentStateInfo{
            .Id = currentStateId,
            .NextId = currentStateId,
        };
    }

    void ConstructCurrentState() {
        with_lock(Lock_) {
            TCurrentStateInfo currentState = ReadCurrentState();
            CurrentStateHolder_ = MakeHolder<TLocalDiskForTestsStateHolder>(Dir_, currentState.Id, WorkerFsCache_, RuntimeFsCache_);
        }
    }

    static TFileSystemTree<TMetaInfo> DeserializeFileSystemTree(const TFsPath& file) {
        TIFStream input(file);

        NJson::TJsonValue json;
        Y_ENSURE(NJson::ReadJsonTree(&input, &json, true));
        Y_ENSURE(json.IsArray());

        TFileSystemTree<TMetaInfo> tree;
        tree.AddFiles(
            json.GetArray(),
            [](const NJson::TJsonValue& fileJson) {
                Y_ENSURE(fileJson.IsMap());
                Y_ENSURE(fileJson.GetMap().contains("Path"));
                const NJson::TJsonValue& pathJson = fileJson.GetMap().at("Path");
                Y_ENSURE(pathJson.IsString());
                return TFileSystemPath(pathJson.GetString());
            },
            [](const NJson::TJsonValue& fileJson) {
                Y_ENSURE(fileJson.IsMap());
                Y_ENSURE(fileJson.GetMap().contains("FileId"));
                auto& jsonMap = fileJson.GetMap();
                const NJson::TJsonValue& fileIdJson = jsonMap.at("FileId");
                Y_ENSURE(fileIdJson.IsString());

                TMetaInfo metaInfo;
                metaInfo.FileId = fileIdJson.GetStringSafe();
                if (jsonMap.contains("Hash")) {
                    metaInfo.Hash = jsonMap.at("Hash").GetStringSafe();
                }
                if (jsonMap.contains("Size")) {
                    metaInfo.Size = jsonMap.at("Size").GetUIntegerRobust();
                }
                if (jsonMap.contains("CreatedAt")) {
                    metaInfo.CreatedAt = TInstant::ParseIso8601(jsonMap.at("CreatedAt").GetStringSafe());
                }
                if (jsonMap.contains("Labels")) {
                    metaInfo.Labels = NYT::NodeFromJsonValue(jsonMap.at("Labels"));
                }

                return metaInfo;
            });

        return tree;
    }

    static void SerializeFileSystemTree(const TFileSystemTree<TMetaInfo>& tree, const TFsPath& file) {
        NJson::TJsonValue json;

        tree.WithAllFiles([&](const TFileSystemPath& path, TAtomicSharedPtr<TMetaInfo> metaInfo) {
            NJson::TJsonValue fileJson;
            fileJson["Path"] = path.ToString();
            fileJson["FileId"] = metaInfo->FileId;
            fileJson["Hash"] = metaInfo->Hash;
            fileJson["Size"] = metaInfo->Size;
            fileJson["CreatedAt"] = metaInfo->CreatedAt.ToString();
            if (!metaInfo->Labels.IsUndefined()) {
                fileJson["Labels"] = NodeToJsonValue(metaInfo->Labels);
            }
            json.AppendValue(std::move(fileJson));
        });

        TOFStream output(file);
        NJson::WriteJson(&output, &json);
        output.Finish();
    }


    TFsPath Dir_;
    THolder<IStateIdGenerator> StateIdGenerator_;
    NFsCache::TCacheStorage WorkerFsCache_;
    NFsCache::TCacheStorage RuntimeFsCache_;

    TMutex Lock_;

    // current state
    THolder<TLocalDiskForTestsStateHolder> CurrentStateHolder_;

    // new state
    THolder<TFileSystemTree<TMetaInfo>> NewStateWorkerFsTree_;
    THolder<TLocalDiskForTestsFileSystem> NewStateWorkerFs_;
    THolder<TFileSystemTree<TMetaInfo>> NewStateRuntimeFsTree_;
    THolder<TLocalDiskForTestsFileSystem> NewStateRuntimeFs_;
    THolder<TNewState> NewState_;
};

}

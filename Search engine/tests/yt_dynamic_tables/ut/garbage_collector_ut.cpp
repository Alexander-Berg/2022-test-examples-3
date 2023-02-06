#include <search/plutonium/core/garbage_collector/cell_garbage_collector.h>
#include <search/plutonium/impl/state/yt_dynamic_tables/state_machine.h>
#include <search/plutonium/impl/garbage_collector/yt_dynamic_tables/public.h>
#include <search/plutonium/impl/file_system/yt_dynamic_tables/tables.h>
#include <search/plutonium/impl/file_system/yt_dynamic_tables/helpers.h>

#include <library/cpp/testing/gtest/gtest.h>

#include <kernel/yt/dynamic/client.h>

#include <util/system/env.h>
#include <util/generic/string.h>
#include <util/stream/format.h>
#include <util/string/builder.h>

using namespace NPlutonium;
using namespace NPlutonium::NDynTables;
using namespace NYT::NTableClient;

namespace {
    struct TTrivialStateIdGenerator: public IStateIdGenerator {
        TString GenerateNewStateId(const TString& currentStateId, TInstant now) const override {
            Y_UNUSED(currentStateId);
            return TStringBuilder() << LeftPad(now.MilliSeconds(), 10, '0');
        }
    };

    void CreateAndMountTable(const NYT::NApi::IClientPtr& client, const NYT::NYPath::TYPath path, const TTableSchema& schema) {
        CreateTable(client, path, schema);
        MountTable(client, path);
    }

    void CreateAndMountTables(const NYT::NApi::IClientPtr& client, const TStateMachineConfig& config) {
        CreateAndMountTable(client, config.CurrentStateTablePath, TCurrentStateTable::Schema());
        CreateAndMountTable(client, config.PublicStatesTablePath, TPublicStatesTable::Schema());

        CreateAndMountTable(client, config.WorkerFsConfig.ContentTablePath, TContentTable::Schema());
        CreateAndMountTable(client, config.WorkerFsConfig.MetaTablePath, TMetaTable::Schema());
        CreateAndMountTable(client, config.WorkerFsConfig.FileInfoTablePath, TFileInfoTable::Schema());

        CreateAndMountTable(client, config.RuntimeFsConfig.ContentTablePath, TContentTable::Schema());
        CreateAndMountTable(client, config.RuntimeFsConfig.MetaTablePath, TMetaTable::Schema());
        CreateAndMountTable(client, config.RuntimeFsConfig.FileInfoTablePath, TFileInfoTable::Schema());
    }

    void CheckConfigsForSamePaths(const TStateMachineConfig& smConfig, const TGarbageCollectorConfig& gcConfig) {
        Y_ENSURE(smConfig.CurrentStateTablePath == gcConfig.Tables.CurrentStateTablePath);
        Y_ENSURE(smConfig.PublicStatesTablePath == gcConfig.Tables.PublicStatesTablePath);

        Y_ENSURE(smConfig.RuntimeFsConfig.ContentTablePath == gcConfig.RuntimeFs.Tables.ContentTablePath);
        Y_ENSURE(smConfig.RuntimeFsConfig.MetaTablePath == gcConfig.RuntimeFs.Tables.MetaTablePath);
        Y_ENSURE(smConfig.RuntimeFsConfig.FileInfoTablePath == gcConfig.RuntimeFs.Tables.FileInfoTablePath);

        Y_ENSURE(smConfig.WorkerFsConfig.MetaTablePath == gcConfig.WorkerFs.Tables.MetaTablePath);
        Y_ENSURE(smConfig.WorkerFsConfig.FileInfoTablePath == gcConfig.WorkerFs.Tables.FileInfoTablePath);
        Y_ENSURE(smConfig.WorkerFsConfig.ContentTablePath == gcConfig.WorkerFs.Tables.ContentTablePath);
    }

    class DynTablesGcSmFixture: public testing::Test {
    private:
        const TString Dir = "//dir";
        const TString Namespace = "namespace1";

        void SetUp() override {
            Client_ = NYT::NApi::CreateClientWithoutUser(GetEnv("YT_PROXY"));

            StateMachineConfig_ = GetDefaultStateMachineConfig(Dir, Namespace, true/*publishStates*/);
            GarbageCollectorConfig_ = GetDefaultGarbageCollectorConfig(Dir);
            CheckConfigsForSamePaths(StateMachineConfig_, GarbageCollectorConfig_);

            CreateAndMountTables(Client_, StateMachineConfig_);

            SaveCurrentState(Client_, StateMachineConfig_.CurrentStateTablePath, Namespace, ToStateId(0), ToStateId(0));
        }

        void TearDown() override {
            Client_->RemoveNode(Dir, NYT::NApi::TRemoveNodeOptions{ .Recursive = true, .Force = true}).Get().ThrowOnError();
        }
    protected:
        struct TFsState {
            TVector<TString> Files;
            TVector<std::tuple<TString, TString>> States;
        };

    protected:
        THolder<IStateMachine> NewStateMachine() const {
            return CreateStateMachine(Client_, StateMachineConfig_, MakeHolder<TTrivialStateIdGenerator>(), MakeBlackHole(), NFsCache::TCacheStorage{}, NFsCache::TCacheStorage{});
        }

        THolder<IGarbageCollector> NewGarbageCollector() const {
            return CreateGarbageCollector(Client_, GarbageCollectorConfig_);
        }

        TString ToStateId(size_t sId) {
            TString stateId = ToString(sId);
            while (stateId.size() < 8) {
                stateId = "0" + stateId;
            }
            return stateId;
        }

        template<class Table>
        void SelectRows(TVector<typename Table::TRow>& records, const NYT::NYPath::TYPath& tablePath) const {
            auto selectFuture = Client_->SelectRows(TStringBuilder{} << "* from [" << tablePath << "]");
            NYT::NConcurrency::WaitFor(selectFuture).ThrowOnError();

            const auto rowSet = selectFuture.Get().Value().Rowset;
            for (auto row : rowSet->GetRows()) {
                records.push_back(Table::ParseRow(row, RemapColumnIds<Table>(rowSet->GetNameTable())));
            }
        }

        TVector<TString> GetAllFiles(const NYT::NYPath::TYPath& tablePath) {
            TVector<TString> files;

            TVector<TContentTable::TRow> rows;
            SelectRows<TContentTable>(rows, tablePath);
            for (const auto& row : rows) {
                if (*row.PartNumber == 0) {
                    files.push_back(*row.FileId);
                }
            }
            Sort(files.begin(), files.end());

            return files;
        }

        TVector<std::tuple<TString, TString>> GetAllStates(const NYT::NYPath::TYPath& tablePath) {
            TVector<std::tuple<TString, TString>> states;

            TVector<TMetaTable::TRow> rows;
            SelectRows<TMetaTable>(rows, tablePath);
            for (const auto& row : rows) {
                states.push_back({*row.Namespace, *row.StateId});
            }

            Sort(states.begin(), states.end());
            auto last = std::unique(states.begin(), states.end());
            states.erase(last, states.end());

            return states;
        }

        TFsState GetFsState(const TFileSystemConfig& fsConfig) {
            TFsState fsState;

            fsState.Files = GetAllFiles(fsConfig.ContentTablePath);
            Sort(fsState.Files.begin(), fsState.Files.end());

            fsState.States = GetAllStates(fsConfig.MetaTablePath);
            Sort(fsState.Files.begin(), fsState.Files.end());

            return fsState;
        }

        void CheckEqualFsStates(const TFsState& actual, const TFsState& expected) {
            ASSERT_EQ(actual.States.size(), expected.States.size());
            for (size_t i = 0; i < expected.States.size(); ++i) {
                ASSERT_EQ(actual.States[i], expected.States[i]);
            }

            ASSERT_EQ(actual.Files.size(), expected.Files.size());
            for (size_t i = 0; i < expected.Files.size(); ++i) {
                ASSERT_EQ(actual.Files[i], expected.Files[i]);
            }
        }

        void CheckEmptyGarabageCollectorIteration(IGarbageCollector* garbageCollector) {
            TFsState expectedRuntimeFsState = GetFsState(StateMachineConfig_.RuntimeFsConfig);
            TFsState expectedWorkerFsState = GetFsState(StateMachineConfig_.WorkerFsConfig);

            garbageCollector->LoopIteration();

            TFsState actualRuntimeFsState = GetFsState(StateMachineConfig_.RuntimeFsConfig);
            TFsState actualWorkerFsState = GetFsState(StateMachineConfig_.WorkerFsConfig);

            CheckEqualFsStates(expectedRuntimeFsState, actualRuntimeFsState);
            CheckEqualFsStates(expectedWorkerFsState, actualWorkerFsState);
        }

        void StateMachineLoopIteration(IStateMachine* stateMachine, size_t iteration) {
            TNewState* newState = stateMachine->InitializeNewState();

            auto runtimeFile = newState->RuntimeFs->WriteFile(TStringBuilder{} << "runtime_file_" << iteration);
            runtimeFile->Write('r');
            runtimeFile->Finish();

            auto workerFile = newState->WorkerFs->WriteFile(TStringBuilder{} << "worker_file_" << iteration);
            workerFile->Write('w');
            workerFile->Finish();

            stateMachine->FinalizeAndSwitchToNewState();
        }

    protected:
        NYT::NApi::IClientPtr Client_;

        TGarbageCollectorConfig GarbageCollectorConfig_;
        TStateMachineConfig StateMachineConfig_;
    };
}

TEST_F(DynTablesGcSmFixture, NothingToRemoveFirstStateAfterInit) {
    THolder<IStateMachine> stateMachine = NewStateMachine();
    THolder<IGarbageCollector> garbageCollector = NewGarbageCollector();

    TNewState* newState = stateMachine->InitializeNewState();

    CheckEmptyGarabageCollectorIteration(garbageCollector.Get());

    auto runtimeFile = newState->RuntimeFs->WriteFile(TStringBuilder{} << "runtime_file_" << 1);
    runtimeFile->Write('r');
    runtimeFile->Finish();

    auto workerFile = newState->WorkerFs->WriteFile(TStringBuilder{} << "worker_file_" << 1);
    workerFile->Write('w');
    workerFile->Finish();

    stateMachine->FinalizeAndSwitchToNewState();

    CheckEmptyGarabageCollectorIteration(garbageCollector.Get());
}

TEST_F(DynTablesGcSmFixture, NothingToRemoveFirstStateAfterWriteFile) {
    THolder<IStateMachine> stateMachine = NewStateMachine();
    THolder<IGarbageCollector> garbageCollector = NewGarbageCollector();

    TNewState* newState = stateMachine->InitializeNewState();

    auto runtimeFile = newState->RuntimeFs->WriteFile(TStringBuilder{} << "runtime_file_" << 1);
    runtimeFile->Write('r');
    runtimeFile->Finish();

    auto workerFile = newState->WorkerFs->WriteFile(TStringBuilder{} << "worker_file_" << 1);
    workerFile->Write('w');
    workerFile->Finish();

    CheckEmptyGarabageCollectorIteration(garbageCollector.Get());

    stateMachine->FinalizeAndSwitchToNewState();

    CheckEmptyGarabageCollectorIteration(garbageCollector.Get());
}

TEST_F(DynTablesGcSmFixture, NothingToRemoveFirstStateAfterFinalize) {
    THolder<IStateMachine> stateMachine = NewStateMachine();
    THolder<IGarbageCollector> garbageCollector = NewGarbageCollector();

    StateMachineLoopIteration(stateMachine.Get(), 0);

    CheckEmptyGarabageCollectorIteration(garbageCollector.Get());
}

TEST_F(DynTablesGcSmFixture, NothingToRemoveAfterInit) {
    THolder<IStateMachine> stateMachine = NewStateMachine();
    THolder<IGarbageCollector> garbageCollector = NewGarbageCollector();

    StateMachineLoopIteration(stateMachine.Get(), 0);

    TNewState* newState = stateMachine->InitializeNewState();

    CheckEmptyGarabageCollectorIteration(garbageCollector.Get());

    auto runtimeFile = newState->RuntimeFs->WriteFile(TStringBuilder{} << "runtime_file_" << 1);
    runtimeFile->Write('r');
    runtimeFile->Finish();

    auto workerFile = newState->WorkerFs->WriteFile(TStringBuilder{} << "worker_file_" << 1);
    workerFile->Write('w');
    workerFile->Finish();

    stateMachine->FinalizeAndSwitchToNewState();

    CheckEmptyGarabageCollectorIteration(garbageCollector.Get());
}

TEST_F(DynTablesGcSmFixture, NothingToRemoveAfterWriteFile) {
    THolder<IStateMachine> stateMachine = NewStateMachine();
    THolder<IGarbageCollector> garbageCollector = NewGarbageCollector();

    StateMachineLoopIteration(stateMachine.Get(), 0);

    TNewState* newState = stateMachine->InitializeNewState();

    auto runtimeFile = newState->RuntimeFs->WriteFile(TStringBuilder{} << "runtime_file_" << 1);
    runtimeFile->Write('r');
    runtimeFile->Finish();

    auto workerFile = newState->WorkerFs->WriteFile(TStringBuilder{} << "worker_file_" << 1);
    workerFile->Write('w');
    workerFile->Finish();

    CheckEmptyGarabageCollectorIteration(garbageCollector.Get());

    stateMachine->FinalizeAndSwitchToNewState();

    CheckEmptyGarabageCollectorIteration(garbageCollector.Get());
}

TEST_F(DynTablesGcSmFixture, NothingToRemoveAfterFinalize) {
    THolder<IStateMachine> stateMachine = NewStateMachine();
    THolder<IGarbageCollector> garbageCollector = NewGarbageCollector();

    for (size_t i = 0; i < 5; ++i) {
        StateMachineLoopIteration(stateMachine.Get(), i);
    }

    CheckEmptyGarabageCollectorIteration(garbageCollector.Get());
}

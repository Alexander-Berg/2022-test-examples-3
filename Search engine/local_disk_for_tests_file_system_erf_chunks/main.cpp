#include <search/plutonium/core/state/state_id_generator.h>
#include <search/plutonium/examples/protos/example_erf.pb.h>
#include <search/plutonium/impl/queue/yt_static_table/yt_static_table_queue.h>
#include <search/plutonium/impl/state/local_disk_for_tests/local_disk_for_tests_state_machine.h>
#include <search/plutonium/impl/state/id_generator/numeric/numeric_state_id_generator.h>
#include <search/plutonium/impl/worker/default_worker.h>

#include <library/cpp/getopt/last_getopt.h>
#include <library/cpp/protobuf/util/pb_io.h>

#include <util/digest/multi.h>
#include <util/generic/set.h>
#include <util/random/random.h>



class TSimpleErfChunksStateModifier: public NPlutonium::IStateModifier<NPlutonium::NExamples::TExampleErf> {
public:
    static constexpr size_t NumChunks = 72;
    static constexpr size_t NumDeltaChunks = 9;

    void Initialize(const NPlutonium::TCurrentState* currentState) override {
        Y_UNUSED(currentState);
    }

    void ApplyModifications(const NPlutonium::TCurrentState* currentState, const TVector<NPlutonium::NExamples::TExampleErf>& modifications, NPlutonium::TNewState* newState) override {
        Y_UNUSED(currentState);
        TSet<size_t> deltaChunks;
        while (deltaChunks.size() < NumDeltaChunks) {
            deltaChunks.insert(RandomNumber<size_t>(NumChunks));
        }
        newState->WorkerFs->WriteFile("delta_chunks")->Write(JoinSeq(",", deltaChunks));

        TVector<TSimpleSharedPtr<IOutputStream>> outputs(Reserve(NumDeltaChunks));
        for (size_t deltaChunk : deltaChunks) {
            outputs.push_back(newState->RuntimeFs->WriteFile("chunks_" + ToString(deltaChunk)));
        }

        for (const NPlutonium::NExamples::TExampleErf& erf : modifications) {
            outputs[MultiHash(erf.GetHost(), erf.GetPath()) % outputs.size()]->Write(erf.GetFinalErf());
        }
    }
};

struct TOptions {
    TString CalculatedAttrsTablePath;
    TString WorkingDir;
    TString WorkerConfigPath;
    TString YtCluster = "arnold";
    bool DontInitializeWorkingDir = false;
};

TOptions ParseOptions(int argc, const char* argv[]) {
    TOptions options;

    NLastGetopt::TOpts opts;
    opts.AddLongOption("calculated-attrs-table-path").Required().RequiredArgument("<ypath>").StoreResult(&options.CalculatedAttrsTablePath);
    opts.AddLongOption("working-dir").Required().RequiredArgument("<path>").StoreResult(&options.WorkingDir);
    opts.AddLongOption("worker-config-path").Required().RequiredArgument("<path>").StoreResult(&options.WorkerConfigPath);
    opts.AddLongOption("yt-cluster").Optional().RequiredArgument("<string>").StoreResult(&options.YtCluster);
    opts.AddLongOption("dont-initialize-working-dir").Optional().NoArgument().SetFlag(&options.DontInitializeWorkingDir);

    NLastGetopt::TOptsParseResult parseResult(&opts, argc, argv);
    Y_UNUSED(parseResult);

    return options;
}

TString SerializeFileSystem(const TVector<std::pair<TString, TGUID>>& files) {
    NJson::TJsonValue result(NJson::JSON_ARRAY);

    for (const auto& file : files) {
        NJson::TJsonValue fileJson;
        fileJson["Path"] = file.first;
        fileJson["FileId"] = GetGuidAsString(file.second);
        result.AppendValue(std::move(fileJson));
    }

    TString serialized;
    TStringOutput output(serialized);
    NJson::WriteJson(&output, &result);
    output.Finish();

    return serialized;
}

void InitializeWorkingDir(const TFsPath& dir) {
    NFs::RemoveRecursive(dir);
    dir.MkDirs();

    TOFStream(dir / "__current_state__").Write("0");

    (dir / "states").MkDirs();
    (dir / "states/0").MkDirs();

    (dir / "worker_content").MkDirs();
    (dir / "runtime_content").MkDirs();

    NPlutonium::TYtStaticTableQueueOffset queueOffset;
    queueOffset.Proto.SetRowIndex(0);

    TGUID queueOffsetFileId;
    CreateGuid(&queueOffsetFileId);

    TOFStream queueOffsetFileOutput(dir / "worker_content" / GetGuidAsString(queueOffsetFileId));
    queueOffset.Save(&queueOffsetFileOutput);
    queueOffsetFileOutput.Finish();

    TOFStream(dir / "states/0/__worker_file_system_tree__").Write(SerializeFileSystem({ { "__modifications_queue_offset__", queueOffsetFileId } }));
    TOFStream(dir / "states/0/__runtime_file_system_tree__").Write(SerializeFileSystem({}));
}

struct TModificationTraits {
    using TModificationsQueueOffset = NPlutonium::TYtStaticTableQueueOffset;
    using TModification = NPlutonium::NExamples::TExampleErf;
};

int main(int argc, const char* argv[]) {
    const TOptions options = ParseOptions(argc, argv);

    if (!options.DontInitializeWorkingDir) {
        InitializeWorkingDir(options.WorkingDir);
    }

    NPlutonium::TDefaultWorkerConfig workerConfig = ParseFromTextFormat<NPlutonium::TDefaultWorkerConfig>(options.WorkerConfigPath);

    THolder<NPlutonium::IStateMachine> stateMachine = MakeHolder<NPlutonium::TLocalDiskForTestsStateMachine>(
        options.WorkingDir,
        MakeHolder<NPlutonium::TNumericStateIdGenerator>());

    TIntrusivePtr<NPlutonium::IQueue<NPlutonium::TYtStaticTableQueueOffset, NPlutonium::NExamples::TExampleErf>> queue = MakeIntrusive<NPlutonium::TYtStaticTableQueue<NPlutonium::NExamples::TExampleErf>>(
        NYT::CreateClient(options.YtCluster),
        NYT::TRichYPath(options.CalculatedAttrsTablePath).Columns({ "Host", "Path", "FinalErf" }));

    THolder<NPlutonium::IStateModifier<NPlutonium::NExamples::TExampleErf>> stateModifier = MakeHolder<TSimpleErfChunksStateModifier>();

    NPlutonium::TDefaultWorker<TModificationTraits> worker(
        std::move(workerConfig),
        std::move(stateMachine),
        std::move(queue),
        std::move(stateModifier),
        "");

    worker.Initialize();
    while (true) {
        worker.LoopIteration();
    }

    return 0;
}

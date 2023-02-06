#include "static_files_replicator.h"

#include <search/plutonium/core/state/current_state.h>
#include <search/plutonium/core/state/state_mutation.h>
#include <search/plutonium/core/file_system/traverse.h>
#include <search/plutonium/helpers/hasher/calc_hash.h>
#include <search/plutonium/impl/state/id_generator/factory.h>
#include <search/plutonium/impl/state/local_disk_for_tests/local_disk_for_tests_state_machine.h>

#include <library/cpp/testing/gtest/gtest.h>

#include <util/folder/tempdir.h>


namespace NPlutonium::NWorkers {

const TStringBuf REMOTE_STATIC_FILES = "static_files";

struct TTestEnvironment {
    TTestEnvironment() {
        TLocalDiskForTestsStateMachine::InitializeWorkingDir(WorkDir_.Path());
        StateMachine_ = MakeHolder<NPlutonium::TLocalDiskForTestsStateMachine>(
            WorkDir_.Path() / "state_machine",
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

    const TFsPath GetTmpDir() const {
        return WorkDir_.Path() / "tmp";
    }

private:
    TTempDir WorkDir_;
    THolder<IStateMachine> StateMachine_;
};

TString CutPrefix(const TString& s, const TString& prefix) {
    return s.StartsWith(prefix) ? s.substr(prefix.size()) : s;
}

void ListLocalFiles(const TFsPath& localPath, const TString& prefix, TMap<TString, std::pair<ui64, TString>>& output) {
    if (!localPath.IsDirectory()) {
        if (!localPath.Exists()) {
            return;
        }
        const ui64 size = GetFileLength(localPath);
        const TBlob blob = TBlob::FromFile(localPath);
        TString hash = CalcHash(blob.data(), blob.size());
        output.emplace(CutPrefix(localPath, prefix), std::pair<ui64, TString>{size, std::move(hash)});
        return;
    }

    TVector<TFsPath> children;
    localPath.List(children);
    for (const TFsPath& childPath : children) {
        ListLocalFiles(childPath, prefix, output);
    }
}

TMap<TString, std::pair<ui64, TString>> ListLocalFiles(const TFsPath& localPath) {
    TMap<TString, std::pair<ui64, TString>> result;
    ListLocalFiles(localPath, localPath, result);
    return result;
}

void ListRemoteFiles(const TFileSystemPath& remotePath, const IReadOnlyFileSystem* fs, TMap<TString, std::pair<ui64, TString>>& output) {
    const TString prefix = remotePath.ToString();
    TraverseReadOnlyFileSystem(
        *fs,
        remotePath,
        [&](const TFileSystemPath& path, EFileSystemObjectType objectType) -> bool {
            if (objectType == EFileSystemObjectType::FileObjectType) {
                auto meta = fs->GetFileMetaInfo(path);
                output.emplace(CutPrefix(path.ToString(), prefix), std::pair<ui64, TString>{meta->Size, meta->Hash});
            }
            return true;
        }
    );
}

TMap<TString, std::pair<ui64, TString>> ListRemoteFiles(const TFileSystemPath& remotePath, const IReadOnlyFileSystem* fs) {
    TMap<TString, std::pair<ui64, TString>> output;
    ListRemoteFiles(remotePath, fs, output);
    return output;
}

void FillLocalDirectoryFromJson(const TFsPath& rootPath, const NJson::TJsonValue& json) {
    Y_ENSURE(json.IsMap());
    rootPath.MkDirs();
    for (const auto& [name, item] : json.GetMapSafe()) {
        const TFsPath itemPath = rootPath / name;
        if (item.IsMap()) {
            FillLocalDirectoryFromJson(itemPath, item);
        } else {
            Y_ENSURE(item.IsString(), "Cannot create " << itemPath << " from non-string element");
            TFileOutput output(itemPath);
            output.Write(item.GetStringSafe());
            output.Finish();
        }
    }
}

void FillLocalDirectoryFromJsonStr(const TFsPath& rootPath, TStringBuf jsonStr) {
    FillLocalDirectoryFromJson(rootPath, NJson::ReadJsonFastTree(jsonStr));
}

void RunTest(const TVector<TString>& jsons) {
    TTestEnvironment env;
    const TFsPath tmpDir = env.GetTmpDir();
    for (const TString& json : jsons) {
        tmpDir.ForceDelete();
        FillLocalDirectoryFromJsonStr(tmpDir, json);
        auto localFiles = ListLocalFiles(tmpDir);

        for (size_t i = 0; i < 2; ++i) {
            env.CreateNewState([&](const TCurrentState* currentState, TNewState* newState) {
                ReplicateStaticFiles(tmpDir, REMOTE_STATIC_FILES, currentState->RuntimeFs, newState->RuntimeFs);
            });

            auto remoteFiles = ListRemoteFiles(REMOTE_STATIC_FILES, env.CurrentState()->RuntimeFs);

            ASSERT_EQ(localFiles, remoteFiles);
        }
    }
}

TEST(StaticFilesReplicator, EverythingAtOnce) {
    RunTest({
        "{}",

        R"json({
            "single_file": "file_content"
        })json",

        R"json({
            "single_file": "smaller"
        })json",

        R"json({
            "single_file": "smalle_"
        })json",

        "{}",

        R"json({
            "folder": {
                "child1": "43",
                "subfolder": {
                    "another_folder": {
                        "child2": "sdfasdf"
                    }
                },
                "empty_folder": {}
            },
            "some_file": "something"
        })json",

        R"json({
            "folder": "not_a_folder",
        })json",

        R"json({
            "folder": {}
        })json",

        R"json({
            "folder": {
                "smth": "somethingg"
            }
        })json",

        R"json({
            "smth": "somethingg"
        })json",

        R"json({
            "smth": {
                "item1": "1",
                "item2": "2"
            }
        })json",

        R"json({
            "smth": {
                "item1": "456",
                "item3": "2"
            }
        })json"
    });
}

}

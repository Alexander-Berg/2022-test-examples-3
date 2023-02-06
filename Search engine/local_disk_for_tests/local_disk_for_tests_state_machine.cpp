#include "local_disk_for_tests_state_machine.h"

#include <library/cpp/yson/node/node_visitor.h>
#include <library/cpp/json/yson/json2yson.h>

namespace NPlutonium {
namespace {

TString SerializeEmptyFileSystem() {
    NJson::TJsonValue result(NJson::JSON_ARRAY);

    TString serialized;
    TStringOutput output(serialized);
    NJson::WriteJson(&output, &result);
    output.Finish();

    return serialized;
}

}

NJson::TJsonValue NodeToJsonValue(const NYT::TNode& node) {
    NJson::TJsonValue jsonValue;
    NJson::TParserCallbacks parser(jsonValue);
    NJson2Yson::TJsonBuilder consumer(&parser);
    NYT::TNodeVisitor visitor(&consumer);
    visitor.Visit(node);
    return jsonValue;
}

void TLocalDiskForTestsStateMachine::InitializeWorkingDir(const TFsPath& dir, const TString& initialStateId) {
    NFs::RemoveRecursive(dir);
    dir.MkDirs();

    TOFStream(dir / "__current_state__").Write(initialStateId);

    (dir / "states").MkDirs();
    (dir / "states" / initialStateId).MkDirs();

    (dir / "worker_content").MkDirs();
    (dir / "runtime_content").MkDirs();

    TOFStream(dir / "states" / initialStateId / "__worker_file_system_tree__").Write(SerializeEmptyFileSystem());
    TOFStream(dir / "states" / initialStateId / "__runtime_file_system_tree__").Write(SerializeEmptyFileSystem());
}

}

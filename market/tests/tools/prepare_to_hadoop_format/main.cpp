#include <market/gumoful/tools/utils/defines.h>
#include <market/gumoful/tools/utils/protobuf_reader.h>
#include <market/gumoful/tools/utils/protobuf_writer.h>
#include <library/cpp/string_utils/base64/base64.h>
#include <util/stream/file.h>
#include <util/folder/path.h>

int main() {
    const auto message = Cin.ReadAll();

    TVarIntLenValueProtoReader protoReader(message);
    while(const auto pbModel = protoReader.Read<TExportReportModel>()) {
        TString memoryBuffer;
        Y_PROTOBUF_SUPPRESS_NODISCARD pbModel->SerializeToString(&memoryBuffer);
        Cout << pbModel->id() << '\t' << Base64Encode(memoryBuffer) << Endl;
    }

    return 0;
}

#include <market/gumoful/tools/utils/defines.h>
#include <market/gumoful/tools/utils/protobuf_reader.h>
#include <market/gumoful/tools/utils/protobuf_writer.h>
#include <util/stream/file.h>
#include <util/folder/path.h>
#include <library/cpp/getopt/last_getopt.h>
#include <library/cpp/json/json_prettifier.h>
#include <library/cpp/protobuf/json/proto2json.h>

int main(int argc, char** argv) {
    using namespace NLastGetopt;

    TString categoriesFilePath;
    int hid = 0;
    TString outputDirPath;

    TOpts opts = TOpts::Default();
    opts.AddLongOption('f', "category-file", "file with categories in pb message format")
        .StoreResult(&categoriesFilePath)
        .Required();

    opts.AddLongOption('h', "hid", "hid to retrieve from file")
        .StoreResult(&hid)
        .Required();

    opts.AddLongOption('o', "output-dir", "directory to store output files")
        .StoreResult(&outputDirPath)
        .Optional()
        .DefaultValue("./");

    TOptsParseResult res(&opts, argc, argv);

    NProtobufJson::TProto2JsonConfig config;
    config.SetEnumMode(NProtobufJson::TProto2JsonConfig::EnumName);

    const auto outputDir = TFsPath(outputDirPath);

    const auto message = TFileInput(categoriesFilePath).ReadAll();

    TVarIntLenValueProtoReader protoReader(message);

    while(const auto categoryModel = protoReader.Read<TExportCategory>()) {
        if (categoryModel->hid() == hid)
        {
            {
                TString json;
                NProtobufJson::Proto2Json(*categoryModel, json, config);
                json = Default<NJson::TJsonPrettifier>().Prettify(json);

                const auto outputJsonPath = outputDir / TFsPath(ToString("category_") + ToString(hid) + ".json" );
                auto outputJson = TUnbufferedFileOutput(outputJsonPath.GetPath());
                outputJson.Write(json);
            }
            {
                const auto outputPbPath = outputDir / TFsPath(ToString("category_") + ToString(hid) + ".pb" );
                auto outputPb = TUnbufferedFileOutput(outputPbPath.GetPath());
                TVarIntLenValueProtoWriter::Write<TExportCategory>(outputPb, *categoryModel);
            }

            return 0;
        }
    }

    Cerr << "Can't find category with hid = " << hid << Endl;
    return 1;
}

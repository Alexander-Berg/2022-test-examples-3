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

    TString modelsFilePath;
    int hyper_id = 0;
    TString outputDirPath;

    TOpts opts = TOpts::Default();
    opts.AddLongOption('f', "model-file", "file with models in pb message format")
        .StoreResult(&modelsFilePath)
        .Required();

    opts.AddLongOption('h', "hyper_id", "hyper_id to retrieve model from file")
        .StoreResult(&hyper_id)
        .Required();

    opts.AddLongOption('o', "output-dir", "directory to store output files")
        .StoreResult(&outputDirPath)
        .Optional()
        .DefaultValue("./");

    TOptsParseResult res(&opts, argc, argv);

    NProtobufJson::TProto2JsonConfig config;
    config.SetEnumMode(NProtobufJson::TProto2JsonConfig::EnumName);

    const auto outputDir = TFsPath(outputDirPath);

    const auto message = TFileInput(modelsFilePath).ReadAll();

    TVarIntLenValueProtoReader protoReader(message);
    while(const auto pbModel = protoReader.Read<TExportReportModel>()) {
        if (pbModel->id() == hyper_id)
        {
            {
                TString json;
                NProtobufJson::Proto2Json(*pbModel, json, config);
                json = Default<NJson::TJsonPrettifier>().Prettify(json);

                const auto outputJsonPath = outputDir / TFsPath(ToString("model_") + ToString(hyper_id) + ".json" );
                auto outputJson = TUnbufferedFileOutput(outputJsonPath.GetPath());
                outputJson.Write(json);
            }
            {
                const auto outputPbPath = outputDir / TFsPath(ToString("model_") + ToString(hyper_id) + ".pb" );
                auto outputPb = TUnbufferedFileOutput(outputPbPath.GetPath());
                TVarIntLenValueProtoWriter::Write<TExportReportModel>(outputPb, *pbModel);
            }

            return 0;
        }
    }

    Cerr << "Can't find model with id = " << hyper_id << Endl;
    return 1;
}

#include <extsearch/images/kernel/new_runtime/doc_factors/fill_factors.h>

#include <extsearch/images/robot/library/identifier/indexdocument.h>
#include <extsearch/images/robot/library/io/io.h>
#include <extsearch/images/robot/library/opt/opt.h>
#include <extsearch/images/robot/library/tables/indexbuilder_paths.h>
#include <extsearch/images/robot/library/tables/remapindex_paths.h>

#include <extsearch/images/protos/metadoc.pb.h>

#include <library/cpp/getopt/small/modchooser.h>
#include <library/cpp/protobuf/json/json2proto.h>
#include <library/cpp/protobuf/json/proto2json.h>

#include <mapreduce/yt/client/init.h>
#include <mapreduce/yt/interface/client.h>
#include <mapreduce/yt/interface/protos/yamr.pb.h>

#include <util/stream/file.h>


namespace {

    constexpr ui32 DEFAULT_METADOC_NUM_ENTRIES = 100;
    constexpr ui32 CURRENT_I2T_VERSION = 11;
    constexpr ui32 DEFAULT_N_DIGITS = 5;

    NJson::TJsonWriterConfig GetJsonWriterConfig() {
        NJson::TJsonWriterConfig jsonWriterConfig;
        jsonWriterConfig.SetFormatOutput(true);
        jsonWriterConfig.DoubleNDigits = DEFAULT_N_DIGITS;
        jsonWriterConfig.FloatNDigits = DEFAULT_N_DIGITS;
        return jsonWriterConfig;
    }
}

void TestFillFeaturesOutput(const TString& inputFile) {
    NJson::TJsonValue metaDocJson;

    {
        TFileInput fin(inputFile);
        NJson::ReadJsonTree(&fin, &metaDocJson);
    }

    NJson::TJsonWriterConfig jsonWriterConfig = GetJsonWriterConfig();
    NJson::TJsonWriter jsonWriter(&Cout, jsonWriterConfig);
    jsonWriter.OpenArray();
    for (const auto& elem : metaDocJson.GetArray()) {
        NImages::NIndex::TMetaDocPB metaDoc;
        NProtobufJson::Json2Proto(elem, metaDoc, NProtobufJson::TJson2ProtoConfig().AddStringTransform(new NProtobufJson::TBase64DecodeBytesTransform()));

        jsonWriter.OpenMap();
        TString docId = metaDoc.GetImageDoc().GetDocumentId();
        NProtobufJson::TBase64EncodeBytesTransform().TransformBytes(docId);
        jsonWriter.Write("doc id", docId);
        const auto values =  NImages::NNewRuntimeRelevance::FillFeatures(metaDoc.GetImgDlErf(),
                                                                         CURRENT_I2T_VERSION,
                                                                         metaDoc.GetOmniIndexData(),
                                                                         metaDoc.GetErf(),
                                                                         metaDoc.GetText2text(),
                                                                         metaDoc.GetRegErf());
        jsonWriter.Write("values size", values.size());
        jsonWriter.OpenArray("values");
        for (const auto& value : values) {
            jsonWriter.Write(value);
        }
        jsonWriter.CloseArray();
        jsonWriter.CloseMap();
    }
    jsonWriter.CloseArray();
}

int TestFillFeatures(int argc, const char* argv[]) {
    NImages::TOptions options;
    NImages::TStartupArgsParser parser(options);
    parser.AddInputFile();
    parser.Parse(argc, argv);

    TestFillFeaturesOutput(options.InputFile);
    return 0;
}

int DumpMetadocEntries(int argc, const char* argv[]) {
    NYT::Initialize(argc, argv);

    NImages::TOptions options;
    size_t numEntries = DEFAULT_METADOC_NUM_ENTRIES;

    NImages::TStartupArgsParser parser(options);
    parser.AddAllMrParams();
    parser.AddIndexPrefix();
    parser.AddIndexState();
    parser.AddOutputFile();
    parser.GetCmdParams().AddOptional("num-entries", "number of entries to dump from metadoc", "<number>", &numEntries);
    parser.Parse(argc, argv);

    const NImages::TRemapIndexPaths remapIndexTables(options.IndexPrefix, options.IndexState);
    const auto metaDocTable = remapIndexTables.MetaDoc();

    TFixedBufferFileOutput out(options.OutputFile);
    NJson::TJsonWriterConfig jsonWriterConfig = GetJsonWriterConfig();
    NJson::TJsonWriter jsonWriter(&out, jsonWriterConfig);

    auto client = NYT::CreateClient(options.ServerName);
    const auto reader = client->CreateTableReader<NYT::TYamr>(NYT::TRichYPath(metaDocTable.GetYtPath()));

    jsonWriter.OpenArray();
    for (; reader->IsValid() && numEntries > 0; reader->Next(), --numEntries) {
        auto row = reader->MoveRow();
        NImages::NIndex::TMetaDocPB metaDoc;
        NImages::NIO::Read(row.GetValue(), metaDoc);
        NProtobufJson::Proto2Json(metaDoc, jsonWriter, NProtobufJson::TProto2JsonConfig().AddStringTransform(new NProtobufJson::TBase64EncodeBytesTransform()));
    }
    jsonWriter.CloseArray();

    return 0;
}

int main(int argc, const char* argv[]) {
    TModChooser modChooser;
    modChooser.AddMode("TestFillFeatures", TestFillFeatures, "run FillFeatures() test");
    modChooser.AddMode("DumpMetadocEntries", DumpMetadocEntries, "dump entries from metadoc to file");
    return modChooser.Run(argc, argv);
}

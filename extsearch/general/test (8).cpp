#include <extsearch/images/kernel/triggers/neural_net_trigger/proto/neural_net_trigger.pb.h>
#include <extsearch/images/protos/genericimageattrs.pb.h>
#include <extsearch/video/kernel/protobuf/writer.h>
#include <extsearch/video/quality/classifiers/danet_image_processor/lib/danet_image_processor.h>

#include <yweb/robot/kiwi/protos/kwworm.pb.h>

#include <library/cpp/getopt/last_getopt.h>
#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/tests_data.h>

#include <util/folder/path.h>
#include <util/stream/file.h>

int main(int argc, const char *argv[]) {
    TString inFile;
    TString outFile;
    TString configPath;
    ui32 nImages;
    NLastGetopt::TOpts opts;
    opts.AddLongOption('i', "in", "input file").Required().StoreResult(&inFile);
    opts.AddLongOption('o', "out", "output file").Required().StoreResult(&outFile);
    opts.AddLongOption('c', "config", "config data path").Required().StoreResult(&configPath);
    opts.AddLongOption('n', "num_images", "Maximum number of images to process").Required().StoreResult(&nImages);
    NLastGetopt::TOptsParseResult args(&opts, argc, argv);

    NVideo::TGeneralImageProcessor imageProcessor(configPath);

    TFileInput in(inFile);
    TFixedBufferFileOutput out(outFile);
    google::protobuf::io::TCopyingInputStreamAdaptor input(&in);
    ui32 iImage = 0;
    NKiwiWorm::TCalcCallParams record;
    while (google::protobuf::io::ParseFromZeroCopyStreamSeq(&record,& input) && iImage++ < nImages) {
        TBlob imageBlob = TBlob::FromString(record.GetParams(0).GetRawData());
        const TString& imageAttrsStr = record.GetParams(1).GetRawData();
        NImages::TGenericImageAttrs imageAttrs;
        Y_PROTOBUF_SUPPRESS_NODISCARD imageAttrs.ParseFromArray(imageAttrsStr.data(), imageAttrsStr.size());
        const TString& kiwiKey = record.GetParams(2).GetRawData();

        out << record.GetId() << '\t';

        try {
            NImages::TNNResultsProto resultsProto;
            imageProcessor.FillImageFeaturesAndAttributes(imageBlob, resultsProto);
            resultsProto.SetSignature(imageAttrs.GetSignature());
            resultsProto.SetImageMd5(imageAttrs.GetImageMd5());
            resultsProto.SetDate(1);
            resultsProto.SetKiwiKey(kiwiKey);

            const TString resultsProtoStr = NVideo::TProtoWriter::ToString(resultsProto, NVideo::STORE_JSON);
            out << resultsProtoStr;
        } catch (...) {
            out << "Got exception";
        }
        out << '\n';
    }
}

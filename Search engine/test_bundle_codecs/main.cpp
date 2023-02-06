#include <search/lingboost/saas/codecs/stripped_bundle.h>
#include <search/lingboost/saas/codecs/stripped_bundle_v2.h>
#include <search/lingboost/saas/codecs/enum_bundle.h>
#include <search/lingboost/saas/codecs/bundle_codecs.h>

#include <kernel/reqbundle/serializer.h>

#include <library/cpp/json/json_value.h>
#include <library/cpp/json/writer/json.h>
#include <library/cpp/json/json_reader.h>
#include <library/cpp/getopt/last_getopt.h>

#include <util/stream/buffer.h>
#include <util/datetime/cputimer.h>
#include <library/cpp/string_utils/base64/base64.h>

using namespace NLingBoostSaas;
using namespace NReqBundle;

void Main(const TString& codecName, IInputStream& in)
{
    size_t numBundles = 0;
    TProfileTimer timer;

    TString qbundle;
    while (in.ReadLine(qbundle)) {
        TReqBundleDeserializer deser;
        TString binary = Base64Decode(qbundle);
        TReqBundlePtr bundle = new TReqBundle;
        deser.Deserialize(binary, *bundle);
        TReqBundle bundle2;

        for (auto request : bundle->Requests()) {
            for (auto entry : request.Facets().Entries()) {
                entry.ValueToFrac();
            }
        }

        if ("enum" == codecName) {
            TCompressedReqBundle compBundle;
            TEnumReqBundleSerializer ser(TLingBoostCodes::Instance().GetEncodeContext());
            ser.Serialize(*bundle, compBundle);
            TEnumReqBundleDeserializer deser(TLingBoostCodes::Instance().GetDecodeContext());
            deser.Deserialize(compBundle, bundle2);
        } else if ("stripped" == codecName) {
            TStringStream strIO;
            TStrippedReqBundleSerializer ser(TLingBoostCodes::Instance().GetEncodeContext());
            ser.Serialize(*bundle, &strIO);
            TStrippedReqBundleDeserializer deser(TLingBoostCodes::Instance().GetDecodeContext());
            deser.Deserialize(&strIO, bundle2);
        } else if ("stripped_v2" == codecName) {
            TStringStream strIO;
            TStrippedReqBundleV2Serializer ser(TLingBoostCodesV2::Instance().GetEncodeContext(), SV2_FACETS | SV2_REGION);
            ser.Serialize(*bundle, &strIO);
            TStrippedReqBundleV2Deserializer deser(TLingBoostCodesV2::Instance().GetDecodeContext());
            deser.Deserialize(strIO.Str(), bundle2);
        } else {
            Y_FAIL("unknown codec name");
        }

        TReqBundleSerializer::TOptions options;
        options.Format = TCompressorFactory::NO_COMPRESSION;
        options.BlocksFormat = TCompressorFactory::NO_COMPRESSION;
        options.OrderBlocks = true;
        TReqBundleSerializer ser(options);

        TString srcQbundle;
        {
            TStringStream binaryOut;
            ser.Serialize(*bundle, &binaryOut);
            srcQbundle = Base64Encode(binaryOut.Str());
        }

        TString dstQbundle;
        {
            TStringStream binaryOut;
            ser.Serialize(bundle2, &binaryOut);
            dstQbundle = Base64Encode(binaryOut.Str());
        }

        if (dstQbundle != srcQbundle) {
            Cout << "0\t" << srcQbundle << Endl;
            Cout << "1\t" << dstQbundle << Endl;
        }
        numBundles += 1;
    }

    Cerr << "bundles per second = " << (1000.f) * float(numBundles) / timer.Get().MilliSeconds() << Endl;
}

int main(int argc, const char* argv[])
{
    NLastGetopt::TOpts opts;

    TString codecName;
    opts.AddLongOption('c', "codec-name", "Name of bundle codec to test")
        .RequiredArgument("PATH")
        .Required()
        .StoreResult(&codecName);

    TString inputFile;
    opts.AddLongOption('i', "input-file", "Name of input file")
        .RequiredArgument("PATH")
        .Optional()
        .DefaultValue("-")
        .StoreResult(&inputFile);

    NLastGetopt::TOptsParseResult optsParsed(&opts, argc, argv);
    Y_UNUSED(optsParsed);

    IInputStream* inputPtr = &Cin;
    THolder<TFileInput> inputHolder;
    if ("-" != inputFile) {
        inputHolder.Reset(new TFileInput(inputFile));
        inputPtr = inputHolder.Get();
    }

    Main(codecName, *inputPtr);

    return 0;
}


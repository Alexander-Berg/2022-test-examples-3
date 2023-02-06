#include <search/begemot/server/proto/begemot.pb.h>

#include <library/cpp/dolbilo/planner/distribution.h>
#include <library/cpp/getopt/last_getopt.h>
#include <library/cpp/streams/factory/factory.h>
#include <library/cpp/string_utils/base64/base64.h>

int main(int argc, const char* argv[])
{
    NLastGetopt::TOpts opts;
    bool removeLogParams = false;
    bool generateTimestamps = false;
    THashMap<TString, TString> modelsProxyParams;
    TString inputFileName, outputFileName;
    ui32 rps = 100;
    TString distrType;
    opts.AddLongOption('i', "input", "Input file").Required().RequiredArgument("FILE").StoreResult(&inputFileName);
    opts.AddLongOption('o', "output", "Output file").Required().RequiredArgument("FILE").StoreResult(&outputFileName);
    opts.AddLongOption("remove-log-params", "Remove LogSelf*/LogSubSource* params").NoArgument().StoreValue(&removeLogParams, true);
    opts.AddLongOption("set-param", "Add or replace ModelsProxyRequest.CustomParams")
        .RequiredArgument("KEY=VALUE")
        .KVHandler([&modelsProxyParams](TStringBuf key, TStringBuf value){
            modelsProxyParams[TString(key)].assign(value);
        });
    opts.AddLongOption("generate-timestamps", "Generate timestamps for dolbilo planner").NoArgument().StoreValue(&generateTimestamps, true);
    opts.AddLongOption("rps", "RPS for timestamps").RequiredArgument("NUM").StoreResult(&rps);
    opts.AddLongOption("distribution-type", "Distribution type for timestamps, poisson or uniform").RequiredArgument("STRING").DefaultValue("poisson").StoreResult(&distrType);
    opts.SetFreeArgsNum(0);
    NLastGetopt::TOptsParseResult cmdLine(&opts, argc, argv);

    IDistributionRef distr;
    if (generateTimestamps)
        distr = TDistribFactory::Instance(rps).Find(distrType);

    THolder<IInputStream> in = OpenInput(inputFileName);
    THolder<IOutputStream> out = OpenOutput(outputFileName);

    const TString logparams[] = {"LogSubSourceRequestBody", "LogSubSourceResponse", "LogSelfRequestBody", "LogSelfResponse"};
    TString line;
    NBg::NProto::TBegemotRequest request;
    while (in->ReadLine(line)) {
        TStringBuf v(line);
        TStringBuf timedelta, query;
        if (!v.TrySplit('\t', timedelta, query))
            query = v;
        Y_ENSURE(request.ParseFromString(Base64StrictDecode(query)));
        if (removeLogParams) {
            for (const TString& p : logparams)
                request.MutableModelsProxyRequest()->MutableCustomParams()->erase(p);
        }
        for (const auto& p : modelsProxyParams)
            (*request.MutableModelsProxyRequest()->MutableCustomParams())[p.first] = p.second;
        if (generateTimestamps)
            *out << distr->Delta() << '\t';
        else if (timedelta.IsInited())
            *out << timedelta << '\t';
        *out << Base64Encode(request.SerializeAsString()) << '\n';
    }
    return 0;
}

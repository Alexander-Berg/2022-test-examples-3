#include <search/web/blender/dynamic_factors/factors_builder.h>
#include <search/web/blender/factor_calculators/vowpal_wabbit/vowpal_wabbit_blender_predictor.h>
#include <search/web/blender/core/simple_logger.h>
#include <search/web/rearrange/dumper/grouping_url_matcher.h>
#include <search/web/rearrange/dumper/dumper.h>
#include <search/web/rearrange/dumper/extractor.h>
#include <search/web/rearrange/dumper/converter.h>
#include <search/web/rearrange/dumper/applier.h>
#include <search/web/util/fml_config/master.h>
#include <search/web/util/fml_config/vertical.h>
#include <search/web/util/ut_mocks/meta_mock.h>

#include <kernel/formula_storage/formula_storage.h>

#include <library/cpp/getopt/last_getopt.h>
#include <library/cpp/getopt/modchooser.h>
#include <library/cpp/scheme/scheme.h>

#include <util/generic/hash.h>
#include <util/generic/string.h>
#include <util/stream/output.h>

#include <google/protobuf/descriptor.h>
#include <google/protobuf/dynamic_message.h>
#include <google/protobuf/text_format.h>

static NSc::TValue LoadJsonFromFile(const TString& filename) {
    const TString& json = TFileInput(filename).ReadAll();
    return NSc::TValue::FromJsonThrow(json);
}

// expected errors for all storage scenarios (they were determined manually)
static const THashMap<TString, TVector<TString>> FmlsPathToExpectedErrors = {
    {"cycle_graph",
        {
            "Errored chain of subcalcers-descendants"
            , "It contains cycle"
        }
    },
    {"dangling_dep_graph",
        {
            "Errored chain of subcalcers-descendants"
            , "one is missing"
        }
    }
};


static void DoMasterManagerTest(NSc::TValue factoriesConf, NSc::TValue fmlConfig, int version) {
    NFmlConfig::NVertical::TFactoryStorage factoryStorage(factoriesConf);
    NFmlConfig::TMasterManager masterManager(factoryStorage, false);
    masterManager.Init(fmlConfig, version);
    if (masterManager.HasError()) {
        Cerr << "Failed to initialize NFmlConfig::TMasterManager properly" << Endl;
        masterManager.DumpErrors(Cerr);
    }
}

static void DoMasterManagerTestForRearrange(NSc::TValue rearrConfig, const TString& rdPath, const TString& fmlsName) {
    NSc::TValue confScheme;
    for (const NSc::TValue& ruleParams : rearrConfig.Get("rearrange_rules").GetArray()) {
        if (ruleParams.Get("name") == fmlsName) {
            for (auto& optionEntry : ruleParams.Get("options").GetDict()) {
                if (auto* targetPtr = confScheme.TrySelectOrAdd(optionEntry.first)) {
                    *targetPtr = optionEntry.second.GetString();
                }
            }
        }
    }
    const TString& fmlConfigPath = rdPath + "/blender/" + TString{confScheme.Get("Config").GetString()};
    DoMasterManagerTest(confScheme.Get("Factories"), LoadJsonFromFile(fmlConfigPath), confScheme.Get("Version").GetIntNumber(0));
}

static void DoSimpleMasterManagerTest(NSc::TValue fmlConfig) {
    NSc::TValue factoriesConf;
    for (const auto& keyValue : fmlConfig.GetDict()) {
        TStringBuf verticalName = keyValue.first;
        factoriesConf[verticalName]["Path"] = TStringBuilder() << "Vertical/" << verticalName;
    }
    DoMasterManagerTest(factoriesConf, fmlConfig, 0);
}

int FormulasTest(int argc, const char* argv[]) {
    TVector<TString> pathList;
    TString confJsonPath;
    TString fmlConfigJsonPath;
    TString rootPath;
    NLastGetopt::TOpts opts;
    opts
        .AddLongOption('p', "path", "Path to the folder with blender formulas, repeated parameter")
        .Required()
        .AppendTo(&pathList);
    opts
        .AddLongOption("conf", "Path to conf.json file for rearrange rules on UPPER")
        .StoreResult(&confJsonPath);
    opts
        .AddLongOption("fml_config", "Path to fml_config.json file to try to load")
        .StoreResult(&fmlConfigJsonPath);
    opts
        .AddLongOption("root_path", "Path to the root of rearrange.dynamic folder")
        .StoreResult(&rootPath);
    opts.AddHelpOption();
    NLastGetopt::TOptsParseResult optsRes(&opts, argc, argv);

    InitBlenderFormulasStorage([&pathList] (TDynamicFormulasStorage& fs) {
            for (const TString& path : pathList) {
                fs.AddDynamicFormulaFromDirectoryRecursive(path);
            }
            fs.Finalize();
        });
    const TDynamicFormulasStorage& storage = GetBlenderFormulasStorage();

    Cout << storage.GetFmlCount() << Endl;

    NBlender::NDynamicFactors::TFactorBuilder builder(storage.GetDynamicFormulaStorage(), false);

    if (confJsonPath) {
        NSc::TValue rearrConfig = LoadJsonFromFile(confJsonPath);
        DoMasterManagerTestForRearrange(rearrConfig, rootPath, "BlenderFmls");
        DoMasterManagerTestForRearrange(rearrConfig, rootPath, "ImgBlenderFmls");
        DoMasterManagerTestForRearrange(rearrConfig, rootPath, "VideoBlenderFmls");
    }

    if (fmlConfigJsonPath) {
        DoSimpleMasterManagerTest(LoadJsonFromFile(fmlConfigJsonPath));
    }

    return 0;
}

int JsonTest(int argc, const char* argv[]) {
    TString path;
    NLastGetopt::TOpts opts;
    opts
        .AddLongOption('p', "path", "Path to file with json data")
        .Required()
        .StoreResult(&path);
    opts.AddHelpOption();
    NLastGetopt::TOptsParseResult optsRes(&opts, argc, argv);

    TFileInput in(path);
    TString data = in.ReadAll();
    Y_UNUSED(NSc::TValue::FromJsonThrow(data));
    return 0;
}

int JsonTestFromString(int argc, const char* argv[]) {
    TString data;
    NLastGetopt::TOpts opts;
    opts
        .AddLongOption('d', "data", "Data for checking")
        .Required()
        .StoreResult(&data);
    opts.AddHelpOption();
    NLastGetopt::TOptsParseResult optsRes(&opts, argc, argv);

    Y_UNUSED(NSc::TValue::FromJsonThrow(data));
    return 0;
}

int SchemePathTest(int argc, const char* argv[]) {
    TString path;
    NLastGetopt::TOpts opts;
    opts
        .AddLongOption('p', "path", "Path to test validity of")
        .Required()
        .StoreResult(&path);
    opts.AddHelpOption();
    NLastGetopt::TOptsParseResult optsRes(&opts, argc, argv);

    if (!NSc::TValue::PathValid(path))
        ythrow yexception() << "JSON path is not valid";
    NSc::TValue val;
    val.TrySelectOrAdd(path);
    return 0;
}

int ProtobufTextFormatTest(int argc, const char* argv[]) {
    TString path, messageType;
    NLastGetopt::TOpts opts;
    opts
        .AddLongOption('p', "path", "Path to file with data in protobuf text format")
        .Required()
        .StoreResult(&path);
    opts
        .AddLongOption('m', "message-type", "Protobuf message type (must be compiled in this binary)")
        .Required()
        .StoreResult(&messageType);
    opts.AddHelpOption();
    NLastGetopt::TOptsParseResult optsRes(&opts, argc, argv);

    const NProtoBuf::Descriptor* messageDescriptor = NProtoBuf::DescriptorPool::generated_pool()->FindMessageTypeByName(messageType);
    if (!messageDescriptor)
        ythrow yexception() << "Can't find message with type '" << messageType << "'";

    const NProtoBuf::Message* prototypeMessage = NProtoBuf::MessageFactory::generated_factory()->GetPrototype(messageDescriptor);
    if (!prototypeMessage)
        ythrow yexception() << "Can't get message prototype" << Endl;

    THolder<NProtoBuf::Message> msg = THolder(prototypeMessage->New());
    if (!msg)
        ythrow yexception() << "Can't create message" << Endl;

    if (!NProtoBuf::TextFormat::ParseFromString(TUnbufferedFileInput(path).ReadAll(), msg.Get()))
        ythrow yexception() << "The test file is not in proper format";

    return 0;
}

int FmlsStorageHandlingTest(int argc, const char* argv[]) {
    TString path;
    NLastGetopt::TOpts opts;
    opts
        .AddLongOption('p', "path", "Path to root directory with fmls-graphs-directories")
        .Required()
        .StoreResult(&path);
    opts.AddHelpOption();
    NLastGetopt::TOptsParseResult optsRes(&opts, argc, argv);

    TVector<TFsPath> children;
    TFsPath(path).List(children);
    for (TVector<TFsPath>::const_iterator it = children.begin(); it != children.end(); ++it) {
        if (it->IsDirectory() && !it->IsSymlink()) {
            TFormulasStorage storage(true);
            storage.AddFormulasFromDirectoryRecursive(it->GetPath());
            storage.Finalize();
            const auto* messages = FmlsPathToExpectedErrors.FindPtr(it->GetName());
            if (!messages) {
                Cerr << storage.GetErrorLog();
            } else {
                bool ok = true;
                for (const TString& expectedError : *messages) {
                    if (!storage.GetErrorLog().Contains(expectedError)) {
                        ok = false;
                        Cerr << "Formulas-storage loaded from " << it->GetPath()
                             << " but didn't catch expected error: " << expectedError << Endl;
                    }
                }
                if (!ok) {
                    Cerr << storage.GetErrorLog();
                }
            }
        }
    }

    return 0;
}

int InitGroupingUrlMatcherTest(int argc, const char* argv[]) {
    TString path;
    NLastGetopt::TOpts opts;
    opts
        .AddLongOption('p', "path", "Path to directory with factors json")
        .Required()
        .StoreResult(&path);
    opts.AddHelpOption();
    NLastGetopt::TOptsParseResult optsRes(&opts, argc, argv);

    NDumper::TGroupingUrlMatcher matcher;
    matcher.InitFromJson(path);

    return 0;
}

int VowpalWabbitModelTest(int argc, const char* argv[]) {
    TString path, configName;
    NLastGetopt::TOpts opts;
    opts
        .AddLongOption('p', "path", "Path to directory with vw models and json config")
        .Required()
        .StoreResult(&path);
    opts
        .AddLongOption('c', "config-name", "Json config name")
        .Required()
        .StoreResult(&configName);
    opts.AddHelpOption();
    NLastGetopt::TOptsParseResult optsRes(&opts, argc, argv);

    NBlender::TVowpalWabbitBlenderPredictor predictor(path, configName, true);
    NBlender::TSimpleLogger logger(true);

    TVector<float> prediction;
    const THashMap<TString, TVector<TString>> params = {
        {"language", {"ru"}}, {"platform", {"desktop"}}, {"position", {"0"}},
        {"dnorm", {"авито", "авто", "нижний", "новгород", "пробег"}},
        {"dnorm_with_uid", {"авито#y1424750591438774071", "авто#y1424750591438774071", "нижний#y1424750591438774071",
                            "новгород#y1424750591438774071", "пробег#y1424750591438774071"}},
        {"uid", {"y1424750591438774071"}}, {"region", {"10750"}},
        {"query", {"авито", "нижний", "новгород", "авто", "c", "пробег"}},
        {"query_with_uid", {"авито#y1424750591438774071", "авто#y1424750591438774071", "нижний#y1424750591438774071",
                            "новгород#y1424750591438774071", "пробег#y1424750591438774071", "c#y1424750591438774071"}},
        {"url", {"alexfitness", "vk", "msk"}},
        {"unique_url", {"alexfitness", "vk", "msk"}}
    };
    predictor.Predict(params, prediction, logger);

    return 0;
}

const static TSet<TString> platforms = {NDumper::PL_IZNANKA, NDumper::PL_NOT_IZNANKA, NDumper::PL_ANY};

int ReadDumperExtractionConfigTest(int argc, const char* argv[]) {
    TString path;
    NLastGetopt::TOpts opts;
    opts
        .AddLongOption('p', "path", "Path to directory with dumper extraction json config")
        .Required()
        .StoreResult(&path);
    opts.AddHelpOption();
    NLastGetopt::TOptsParseResult optsRes(&opts, argc, argv);

    NSc::TValue config;
    try {
        config = NDumper::ReadExtractionConfig(path);
    } catch (const yexception& exception) {
        ythrow yexception() << "config is not valid.";
    }

    if (!config.Has("geoobject")) {
        ythrow yexception() << "'geoobject' section not found in config; fix config or test.";
    }
    THashSet<TString> stages = {
        NDumper::AFTER_MERGE, NDumper::AFTER_FETCH, NDumper::AFTER_BLEND,
        NDumper::AFTER_BLENDER_FACTORS_READY, NDumper::AFTER_INTENT_WEIGHTS_READY
    };

    for (const auto& client : config.GetDict()) {
        for (const auto& rule : client.second["rules"].GetArray()) {
            TString stage = rule["stage"].ForceString();
            if (!stages.contains(stage)) {
                ythrow yexception() << "'Incorrect stage for rule " << rule["name"] << Endl;
            }

            const NSc::TValue& platform = rule.TrySelect("platform");
            if (!platform.IsNull() && !platforms.contains(platform.GetString())) {
                ythrow yexception() << "Unknown platform " << platform << " for rule " << rule["name"] << Endl;
            }

            //...............initialize environment.................
            NSc::TValue Scheme(NSc::TValue::FromJson("{some_key: some_value}"));
            NSc::TValue grouping(NSc::TValue::FromJson("{Groups : [ { MetaDocs: [ {Url: 'yandex.ru', Gta: { price: 12 }}] } ]}"));
            NRearrUT::TMetaSearchMock Search;
            NRearrUT::TMetaSearchContextMock Context(Search);
            Context.FillGrouping(grouping);
            auto rearrParams = IMetaRearrangeContext::TRearrangeParams(Context.Ctx.MR(), Context.MainGrouping);
            TVector<float> factors;
            //......................................................

            THolder<NDumper::IExtractor> extractor;
            THolder<NDumper::IConverter> converter;
            THolder<NDumper::IApplier> applier;
            try {
                extractor = NDumper::BuildExtractor(rule["extractor"], Scheme, &Context.SearcherProps(), Context.MainGrouping->second);
                converter = NDumper::BuildConverter(rule["converter"]);
                applier = NDumper::BuildApplier(rule["applier"], Scheme, rearrParams, factors);
            } catch (const yexception& exception) {
                ythrow yexception() << rule["name"] << ": " << exception.what() << Endl;
            }

            if (!extractor && rule["extractor"]["name"] != "TDynamicFactorsExtractor") {
                ythrow yexception() << rule["name"] << ": " << "extractor is not valid" << Endl;
            }
            if (!converter && rule.Has("converter")) {
                ythrow yexception() << rule["name"] << ": " << "converter is not valid" << Endl;
            }
            if (!applier) {
                ythrow yexception() << rule["name"] << ": " << "applier is not valid" << Endl;
            }
        }
    }
    return 0;
}

int main(int argc, const char* argv[]) {
    TModChooser modes;
    modes.AddMode("formulas", FormulasTest, "Formulas test");
    modes.AddMode("json", JsonTest, "JSON test");
    modes.AddMode("json_from_string", JsonTestFromString, "JSON test from string");
    modes.AddMode("scheme_path", SchemePathTest, "Scheme path test");
    modes.AddMode("proto_text_format", ProtobufTextFormatTest, "Protobuf text format test");
    modes.AddMode("catch_storage_errors", FmlsStorageHandlingTest, "Test formulas-storage for correct catching errors");
    modes.AddMode("init_grouping_url_matcher", InitGroupingUrlMatcherTest, "Test for init of TGroupingUrlMatcher");
    modes.AddMode("vowpal_wabbit", VowpalWabbitModelTest, "Test vowpal wabbit model loading and applying");
    modes.AddMode("dumper_extraction", ReadDumperExtractionConfigTest, "Test for Dumper::Extraction reading config.");
    return modes.Run(argc, argv);
}

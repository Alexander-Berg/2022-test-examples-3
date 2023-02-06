#include <search/web/rearrange/blender_dssm_features/model_loader/model_loader.h>
#include <library/cpp/getopt/last_getopt.h>
#include <util/folder/path.h>

using namespace NLastGetopt;
using namespace NBlenderDssmFeatures;

int main(int argc, char* argv[]) {
    TOpts opts = TOpts::Default();
    TString path;
    opts.AddCharOption('p', "path to config dir (^/rearrange.dynamic/blender_dssm_features)").Required().StoreResult(&path);
    TOptsParseResult parseResult(&opts, argc,argv);

    TFsPath fsPath(path);

    THashMap<TString, TModelWithMeta> models;
    const bool noThrow = false;
    auto& dataRegistry = NRearr::TDataRegistry::Instance();

    NSc::TValue config = LoadSerpModelConfig(fsPath / "config.json", noThrow);

    THashSet<TString> calculated_models;
    for (const auto& [feature, meta]: config.Get("Features").GetDict()) {
        calculated_models.insert(TString(meta.Get("Model").GetString()));
    }

    for (const auto& [model, meta]: config.Get("Models").GetDict()) {
        TString model_name(model);
        // check if model exist and loads correctly
        models[model_name] = NBlenderDssmFeatures::LoadModel(model_name, meta, path, dataRegistry, noThrow);
        // check if conveyor model is calculated
        if (meta.Get("IsConveyor").GetBool()) {
            Y_ENSURE(calculated_models.contains(model_name), TStringBuilder() << "Feature is not calculated for conveyor model " << model_name);
        }
    }

    return 0;
}

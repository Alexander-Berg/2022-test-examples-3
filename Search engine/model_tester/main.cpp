#include <search/web/rearrange/blender_online_learning/model/fast_data.h>
#include <search/web/rearrange/blender_online_learning/model/predictor.h>

#include <library/cpp/getopt/last_getopt.h>
#include <util/folder/path.h>

using namespace NLastGetopt;
using namespace NBlenderOnlineLearning;

bool TestModels(const TString& pathStr) {
    const TFsPath path(pathStr);

    TStringBuilder loadErrors;
    TFastDataModelStorage models;
    if (!LoadModelsFromFastData(models, TFsPath(path), &loadErrors)) {
        Cerr << loadErrors << Endl;
        return false;
    }

    bool validationOk = true;
    for (const auto& m2h: models) {
        const auto* model = m2h.second.GetOrLoadModel();
        if (!model) {
            Cerr << "Null model [" << m2h.first << "]" << Endl;
            validationOk = false;
            continue;
        }
        TString error;
        const auto& predictor = GetPredictor(*model);
        if (!predictor.Validate(*model, error)) {
            validationOk = false;
            Cerr << "Model [" << m2h.first << "] validation failed with error: " << error << Endl;
            validationOk = false;
        }
        Cout << "Model [" << m2h.first << "] is ok " << Endl;
    }
    return validationOk;
}

int main(int argc, char* argv[]) {
    TOpts opts = TOpts::Default();
    TString path;
    opts.AddCharOption('p', "path").Required().StoreResult(&path);
    TOptsParseResult parseResult(&opts, argc, argv);
    auto success = TestModels(path);
    return success ? 0 : 1;
}

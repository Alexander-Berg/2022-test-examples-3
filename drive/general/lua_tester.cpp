#include "lua_tester.h"

#include <drive/backend/offers/ranking/features.h>
#include <drive/backend/offers/ranking/model.h>

#include <rtline/library/json/builder.h>
#include <rtline/library/json/cast.h>

#include <library/cpp/getopt/last_getopt.h>
#include <library/cpp/json/json_reader.h>
#include <library/cpp/json/json_value.h>

#include <util/generic/vector.h>
#include <util/stream/file.h>
#include <util/string/cast.h>
#include <util/string/strip.h>

int main_test_lua_model(int argc, const char** argv) {
    NLastGetopt::TOpts options = NLastGetopt::TOpts::Default();
    options.AddHelpOption();
    options.AddLongOption("features", "Input file with features").RequiredArgument("FILE").DefaultValue("features.json");
    options.AddLongOption("model", "Model source code").RequiredArgument("FILE").DefaultValue("model.lua");
    options.AddLongOption("expected", "Expected answers").RequiredArgument("TABLE");
    NLastGetopt::TOptsParseResult res(&options, argc, argv);
    auto featuresStr = Strip(TIFStream(res.Get("features")).ReadAll());
    auto featuresJson = NJson::ReadJsonFastTree(featuresStr);
    auto featuresBatch = NJson::FromJson<TVector<NDrive::TOfferFeatures>>(featuresJson);
    auto script = Strip(TIFStream(res.Get("model")).ReadAll());
    NJson::TJsonValue modelDescription = NJson::TMapBuilder
        ("name", "lua_model")
        ("type", NDrive::TLuaModel::Type())
        ("script", std::move(script))
    ;
    auto model = NDrive::IOfferModel::Construct(modelDescription);
    Y_ENSURE(model);
    TVector<double> answers;
    answers.reserve(featuresBatch.size());
    for(auto& features : featuresBatch) {
        answers.push_back(model->Calc(features));
    }
    if (res.Has("expected")) {
        auto expectedStr = Strip(TIFStream(res.Get("expected")).ReadAll());
        auto expectedJson = NJson::ReadJsonFastTree(expectedStr);
        auto expected = NJson::FromJson<TVector<double>>(expectedJson);
        Y_ENSURE(answers.size() == expected.size(), "size of answers do not match: " << answers.size() << " != " << expected.size());
        for (ui32 i = 0; i < answers.size(); ++i) {
            Cout << "Test #" << i + 1 << "/" << answers.size() << Endl;
            Y_ENSURE(abs(answers[i] - expected[i]) < 1e-6, "[failure] test # " << i << ": expected " << expected[i] << ", got " << answers[i]);
        }
        Cout << "OK :)" << Endl;
    } else {
        Cout << NJson::ToJson(answers) << Endl;
    }
    return EXIT_SUCCESS;
}

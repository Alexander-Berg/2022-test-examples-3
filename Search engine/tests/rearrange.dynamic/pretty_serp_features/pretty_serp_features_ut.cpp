#include <search/web/core/configuration/conf_json/conf_json.h>

#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/resource/resource.h>
#include <library/cpp/scheme/ut_utils/scheme_ut_utils.h>

class TPrettySerpFeaturesDataTest : public TTestBase {
private:
UNIT_TEST_SUITE(TPrettySerpFeaturesDataTest);
    UNIT_TEST(TestForbiddenOptions)
UNIT_TEST_SUITE_END();

public:
    void CheckForbiddenOptions(const NSc::TValue& defaultValues) {
        for (const auto& key : defaultValues.DictKeys()) {
            const NSc::TValue& plugin = defaultValues[key];
            for (const auto& option : FORBIDDEN_OPTIONS) {
                UNIT_ASSERT_C(!plugin.Has(option), key << " has forbidden option " << option);
            }
        }
    }

    void TestForbiddenOptions() {
        NSc::TValue plugins = NSc::TValue::FromJsonThrow(NResource::Find("/plugins.json"));
        CheckForbiddenOptions(plugins["DefaultValues"]);

        const NSc::TValue confJson = NSc::TValue::FromJsonThrow(NResource::Find("/conf.json"));
        NRearr::TDebugInfo debugInfo;
        auto rc = NRearr::ParseConfJson(confJson, "WEB", debugInfo);
        for (const auto& mri : rc.GetMultiRules()) {
            if (mri.GetName() != "PrettySerpFeatures") {
                continue;
            }
            for (const auto& var : mri.GetRuleVariants()) {
                CheckForbiddenOptions(var.GetScheme()["DefaultValues"]);
            }
        }
    }

private:
    const TVector<TString> FORBIDDEN_OPTIONS = {"extra_filter"};
};

UNIT_TEST_SUITE_REGISTRATION(TPrettySerpFeaturesDataTest);


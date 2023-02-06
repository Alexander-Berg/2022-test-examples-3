#include <search/web/util/unisearch/template_parser.h>

#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/scheme/ut_utils/scheme_ut_utils.h>

class TUnisearchDataTest : public TTestBase {
UNIT_TEST_SUITE(TUnisearchDataTest);
    UNIT_TEST(TestApplicationsFormat)
    UNIT_TEST(TestEducationFormat)
UNIT_TEST_SUITE_END();

private:
    void TryAndCompare(const TTextHighlighterCleaner& hl, NSc::TValue document, NSc::TValue tmplt, NSc::TValue expectedResult, TString errorKey) const {
        NSc::TValue actualResult;
        UNIT_ASSERT_C(NUnisearch::GenerateItemJson(hl, document, tmplt, actualResult), errorKey << ": failed to generate json item");
        UNIT_ASSERT_JSON_EQ_JSON_C(expectedResult, actualResult, errorKey);
    }

    void TryAndCompare(TString query, TString docsPath, TString templatePath, TString canonPath) const {
        NSc::TValue docs = NSc::TValue::FromJsonThrow(NResource::Find(docsPath));
        NSc::TValue templates = NSc::TValue::FromJsonThrow(NResource::Find(templatePath));
        NSc::TValue canonData = NSc::TValue::FromJsonThrow(NResource::Find(canonPath));

        TCreateTreeOptions options(LI_BASIC_LANGUAGES);
        TRichTreePtr qtree = CreateRichTree(UTF8ToWide(query), options);
        TTextHighlighterCleaner hl;
        hl.SetRichTreeIfAvailable(qtree);

        for (const auto& templateKey : templates.DictKeys()) {
            UNIT_ASSERT_C(canonData.Has(templateKey), "No canon data for " << templateKey);
            NSc::TValue tmplt = templates[templateKey];
            if (auto key = tmplt.Get("inherits").GetString()) {
                tmplt.ReverseMerge(templates[key]);
            }
            const NSc::TValue& canonForTemplate = canonData[templateKey];
            if (canonForTemplate == "ignore") {
                continue;
            }
            for (size_t i = 0; i < docs.ArraySize(); ++i) {
                TryAndCompare(hl, docs[i], tmplt, canonForTemplate[i], TStringBuilder() << "Template: " << templateKey << ", Doc: " << i);
            }
        }
    }

public:
    void TestApplicationsFormat() {
        TryAndCompare("приложения для фитнеса", "/applications_data.json", "/applications_format.json", "/applications_canon_data.json");
    }

    void TestEducationFormat() {
        TryAndCompare("python курсы", "/education_data.json", "/education_format.json", "/education_canon_data.json");
    }
};

UNIT_TEST_SUITE_REGISTRATION(TUnisearchDataTest);


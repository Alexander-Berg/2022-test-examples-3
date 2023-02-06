#include <market/report/library/reqwizard/wizard_answer_impl.h>

#include <market/report/library/relevance/Utils.h>
#include <market/report/library/read_whole_file/read_whole_file.h>

#include <library/cpp/scheme/scheme.h>
#include <library/cpp/cgiparam/cgiparam.h>
#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/env.h>

using namespace NMarketReport;

TEST(TJsonWizardAnswer, Parse) {
    TString response = readWholeFile(SRC_("wizard.answer.json"));
    NSc::TValue json = NSc::TValue::FromJson(TStringBuf(response.begin(), response.end()));
    TJsonWizardAnswer answer(json, "relev");
    EXPECT_EQ(true, answer.Has("Market", "qtree4market"));
    EXPECT_EQ(false, answer.Has("Market", "qtree2market"));
    EXPECT_EQ(false, answer.Has("Bazar", "qtree4market"));

    EXPECT_EQ(1, answer.GetCount("Market", "qtree4market"));
    EXPECT_EQ(1, answer.GetCount("IsNav", "IsNav"));
    EXPECT_EQ(0, answer.GetCount("Market", "qtree2market"));
    EXPECT_EQ(3, answer.GetCount("FakeMultivalue", "array"));

    EXPECT_EQ("0.554491", answer.Get("CommercialMx", "TovarModelRank"));
    EXPECT_EQ("0.554491", answer.Get("CommercialMx", "TovarModelRank", 0));
    EXPECT_EQ("0", answer.Get("FakeMultivalue", "array"));
    EXPECT_EQ("relev", answer.GetRelevFactors());

    for (size_t i = 0; i < answer.GetCount("FakeMultivalue", "array"); ++i) {
        EXPECT_EQ(ToString(i), answer.Get("FakeMultivalue", "array", i));
    }
}

TEST(TProtoWizardAnswer, Parse) {
    TString response = readWholeFile(SRC_("wizard.answer.cgi"));
    TProtoWizardAnswer answer(response);

    EXPECT_EQ(true, answer.Has("Market", "qtree4market"));
    EXPECT_EQ(false, answer.Has("Market", "qtree2market"));
    EXPECT_EQ(false, answer.Has("Bazar", "qtree4market"));

    EXPECT_EQ(1, answer.GetCount("Market", "qtree4market"));
    EXPECT_EQ(0, answer.GetCount("Market", "qtree2market"));

    EXPECT_EQ("0.554491", answer.Get("CommercialMx", "TovarModelRank"));
    EXPECT_EQ("0.554491", answer.Get("CommercialMx", "TovarModelRank", 0));
}

#include <market/report/src/Request.h>
#include <market/report/library/reqwizard/ReqWizard.h>
#include <market/report/library/relevance/Utils.h>
#include <market/report/library/read_whole_file/read_whole_file.h>
#include <library/cpp/json/json_reader.h>
#include <library/cpp/json/json_value.h>
#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/env.h>

#include <util/stream/str.h>

using namespace NMarketReport;

TEST(ReqWizard, ParseReqWizardAnswer) {
    TString answer = readWholeFile(SRC_("reqwizard.answer.txt"));

    TString answerStr(answer);
    TStringInput answerStream(answerStr);
    NJson::TJsonValue root;
    EXPECT_TRUE(NJson::ReadJsonTree(&answerStream, &root));
    EXPECT_TRUE(root.Has("Extensions"));

    TReqWizardAnswer rwa(answer);
    const TExtensions& exts = rwa.GetExtensions();
    EXPECT_EQ(1, exts.size());
    EXPECT_EQ(TExtension(6, 12, "айфон"), exts[0]);
}

TEST(ReqWizard, FilterExtensions) {
    TExtensions extensions;
    extensions.push_back(TExtension(2, 10, "msk"));
    extensions.push_back(TExtension(2, 10, "moscow"));

    TExtensions filtered_exts;
    FilterExtensions(extensions, &filtered_exts);

    EXPECT_EQ(1U, filtered_exts.size());
    EXPECT_EQ("msk | moscow", filtered_exts[0].Ext);
}

TEST(ReqWizard, ApplyExtensions1) {
    TExtensions extensions;
    extensions.push_back(TExtension(2, 9, "msk"));
    extensions.push_back(TExtension(2, 9, "moscow"));

    EXPECT_EQ("la(lalalal | msk | moscow)a", ApplyExtensions("lalalalala", extensions));
}

TEST(ReqWizard, ApplyExtensions) {
    TString request = "жить в москве - вредно для здоровья и нервов";

    TExtensions extensions;
    EXPECT_EQ(request, ApplyExtensions(request, extensions));

    {
        // a token that has extensions
        size_t begin = 7; // "москве"
        size_t end = 13;
        extensions.push_back(TExtension(begin, end, "msk"));
        extensions.push_back(TExtension(begin, end, "moscow"));
    }
    {
        TString expected = "жить в (москве | msk | moscow) - вредно для здоровья и нервов";
        EXPECT_EQ(expected, ApplyExtensions(request, extensions));
    }

    {
        // a token that has extensions
        size_t begin = 27; // "здоровья"
        size_t end = 35;
        extensions.push_back(TExtension(begin, end, "health"));
        extensions.push_back(TExtension(begin, end, "zdorov'ichko"));
    }
    {
        TString expected = "жить в (москве | msk | moscow) - вредно для (здоровья | health | zdorov'ichko) и нервов";
        EXPECT_EQ(expected, ApplyExtensions(request, extensions));
    }
}

TEST(ReqWizard, ParseReqWizardRegionAnswer) {
    TString answer = readWholeFile(SRC_("reqwizard_tv_izhevsk.json"));
    TReqWizardAnswer rwa(answer);

    Market::RegionSet regions = rwa.GetRegions();
    EXPECT_EQ(1, regions.size());
    EXPECT_EQ(44, *(regions.begin()));
}

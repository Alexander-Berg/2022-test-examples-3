#include <market/report/src/wizards/wizard_util.h>
#include <library/cpp/testing/unittest/gtest.h>

using namespace NMarketReport;

TEST(NMarketReport, JoinSeqWithStrictCut) {
    const TVector<TString> testVec({"First text", "Second text"});
    const TMap<size_t, TString> testMap({{24, "First text, Second text"},
                                         {23, "First text, Second text"},
                                         {22, "First text, Second ..."},
                                         {16, "First text, S..."},
                                         {15, "First text"},
                                         {14, "First text"},
                                         {13, "First text"},
                                         {12, "First text"},
                                         {11, "First text"},
                                         {10, "First text"},
                                         { 9, "First ..."},
                                         { 4, "F..."},
                                         { 3, "Fir"},
                                         { 2, "Fi"},
                                         { 1, "F"},
                                         { 0, ""}});
    for (const auto& pair : testMap) {
        TString str = JoinSeqWithStrictCut(", ", testVec, "...", pair.first);
        EXPECT_EQ(pair.second, str);
    }
    {
        TString str = JoinSeqWithStrictCut(", ", testVec, "......", 4);
        EXPECT_EQ("Firs", str);
    }
    {
        TVector<TString> emptyVec;
        TString str = JoinSeqWithStrictCut(", ", emptyVec, "...", 10);
        EXPECT_EQ(true, str.empty());
    }
}

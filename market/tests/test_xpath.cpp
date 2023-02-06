#include <market/idx/feeds/qparser/lib/xpath.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

#include <util/string/join.h>


static const TString delim("|");
static const TString joiner("/");

TEST(XPath, Simple) {
    const TVector<TString> actual = NMarket::SimpleXPath::MakePathCombinations("a/b/c", delim, joiner);
    const TVector<TString> expected{"a/b/c"};
    ASSERT_EQ(actual, expected);
}

TEST(XPath, OneVariant) {
    const TVector<TString> actual = NMarket::SimpleXPath::MakePathCombinations("a/(b|c)", delim, joiner);
    const TVector<TString> expected {"a/b", "a/c"};
    ASSERT_EQ(actual, expected);
}

TEST(XPath, MultiVariant) {
    const TVector<TString> actual = NMarket::SimpleXPath::MakePathCombinations("a/(b|c)/d/(e|f|g)", delim, joiner);
    const TVector<TString> expected {"a/b/d/e", "a/b/d/f", "a/b/d/g", "a/c/d/e", "a/c/d/f", "a/c/d/g"};
    ASSERT_EQ(actual, expected);
}

TEST(XPath, GenerateXpathNoVariants) {
    const TString actual = NMarket::SimpleXPath::GenerateVariadicPath("a/b/c/", {});
    const TString expected = "a/b/c/";
    ASSERT_EQ(actual, expected);
}

TEST(XPath, GenerateXpathOneVariant) {
    const TString actual = NMarket::SimpleXPath::GenerateVariadicPath("a/b/c/", {"d"});
    const TString expected = "a/b/c/d";
    ASSERT_EQ(actual, expected);
}

TEST(XPath, GenerateXpathMultyVariants) {
    const THashSet<TString> variants = {"d", "e", "f"};
    const TString actual = NMarket::SimpleXPath::GenerateVariadicPath("a/b/c/", variants);
    const TString expected = "a/b/c/(" + JoinRange("|", variants.begin(), variants.end()) + ")";
    ASSERT_EQ(actual, expected);
}

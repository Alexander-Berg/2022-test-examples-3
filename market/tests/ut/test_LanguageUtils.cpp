#include <market/library/offers_common/LanguageUtils.h>
#include <library/cpp/testing/unittest/gtest.h>

// This eliminates the middle part from a name
TEST (LanguageUtilTests, simplifyAuthors)
{
    ASSERT_EQ("Donald Knuth, Donald Knuth, Donald Knuth, Donald Knuth, Donald Knuth, Donald Knuth, Knuth",
        GetSimplifiedBookAuthors("  Donald E. Knuth , Donald E. Knuth, Donald E Knuth, Donald James Knuth, Donald  E.  Knuth , Donald Knuth, Knuth"));
    ASSERT_EQ("", GetSimplifiedBookAuthors("I am a wrong author name"));
}

// This extracts a numeric token from the end if found
TEST (LanguageUtilTests, testSpecialToken)
{
    ASSERT_EQ("aaa4444", MakeNoPlusMinusAlias("aaa4444+"));
    ASSERT_EQ("aaa4444", MakeNoPlusMinusAlias("aaa4444-"));
    ASSERT_EQ("aaa444 4", MakeNoPlusMinusAlias("aaa444+4+"));
    ASSERT_EQ("", MakeNoPlusMinusAlias("aaa4444"));
    ASSERT_EQ("dpr1 25", MakeNoPlusMinusAlias("dpr1-25+"));
    ASSERT_EQ("DPR-25", MakeNoPlusMinusAlias("DPR-25+"));
    ASSERT_EQ("Sony345 DPR1 25", MakeNoPlusMinusAlias("Sony345+DPR1-25+"));
    ASSERT_EQ("", MakeNoPlusMinusAlias(""));
    ASSERT_EQ("", MakeNoPlusMinusAlias("+"));
    ASSERT_EQ("+Sony1", MakeNoPlusMinusAlias("+Sony1+"));
    ASSERT_EQ("aaa4444  stripping letters is wrong", MakeNoPlusMinusAlias("aaa4444+ stripping letters is wrong"));
    ASSERT_EQ("", MakeNoPlusMinusAlias(" +"));
    ASSERT_EQ("", MakeNoPlusMinusAlias("Тестовый синий оффер с флэш-акцией"));
    ASSERT_EQ("Тестовый оффер4 1", MakeNoPlusMinusAlias("Тестовый оффер4-1"));
    ASSERT_EQ("Тестовый оффер4 1 2", MakeNoPlusMinusAlias("Тестовый оффер4-1+2"));
}

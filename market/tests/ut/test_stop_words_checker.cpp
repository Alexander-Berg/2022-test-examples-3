#include <market/idx/offers/lib/checkers/stop_words_checker.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <util/charset/utf8.h>


TEST(StopWordsChecker, CheckEngTitle) {
    TString str = "asdf qwer zxcv";
    TStringInput stream(str);
    auto checker = NMarket::NChecker::CreateStopWordsChecker(stream);

    EXPECT_TRUE(checker->Check(""));
    EXPECT_TRUE(checker->Check("a"));
    EXPECT_TRUE(checker->Check("asdf1"));
    EXPECT_TRUE(checker->Check("asdf1 qwe"));
    EXPECT_TRUE(checker->Check("asdf1.qw2er"));

    EXPECT_FALSE(checker->Check("asdf"));
    EXPECT_FALSE(checker->Check("AsDf"));
    EXPECT_FALSE(checker->Check("Asdf "));
    EXPECT_FALSE(checker->Check(" Asdf"));
    EXPECT_FALSE(checker->Check("  Asdf"));
    EXPECT_FALSE(checker->Check("Asdf  "));
    EXPECT_FALSE(checker->Check("asdf1-qWer  "));
    EXPECT_FALSE(checker->Check("asdf. wer"));
    EXPECT_FALSE(checker->Check("asd wer. zxcV-"));
}


TEST(StopWordsChecker, CheckUTF8Title) {
    TString str = "али asdf";
    TStringInput stream(str);
    auto checker = NMarket::NChecker::CreateStopWordsChecker(stream);

    EXPECT_TRUE(checker->Check("А фыва"));
    EXPECT_TRUE(checker->Check("Али1"));
    EXPECT_TRUE(checker->Check("123 малина 1asdf"));

    EXPECT_FALSE(checker->Check("asdf"));
    EXPECT_FALSE(checker->Check("23 asdf-1qwer"));
    EXPECT_FALSE(checker->Check("asdf. малина"));
    EXPECT_FALSE(checker->Check("asd wer. Али-Экспресс"));
}

#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <mail/sendbernar/composer/include/fold_header.h>

namespace {

using namespace testing;

typedef Test FoldTest;

template <std::string::size_type treshold>
std::string foldHeader(const std::string &str)
{
    return foldHeaderTemplate<treshold>(str);
}

TEST(FoldTest, foldHeader_shortHeader_doesNotChange) {
    const std::string::size_type treshold = 10;
    const std::string str = "ab cd ef";
    ASSERT_EQ(str, foldHeader<treshold>(str));
}

TEST(FoldTest, foldHeader_longHeader_folds) {
    const std::string::size_type treshold = 10;
    const std::string str = "abc def ghij klm";
    ASSERT_EQ("abc def\r\n\tghij klm", foldHeader<treshold>(str));
}

TEST(FoldTest, foldHeader_veryLongHeader_foldsTwice) {
    const std::string::size_type treshold = 5;
    const std::string str = "abc de f ghij";
    ASSERT_EQ("abc\r\n\tde f\r\n\tghij", foldHeader<treshold>(str));
}

TEST(FoldTest, foldHeader_partlyFoldedHeader_foldsUp) {
    const std::string::size_type treshold = 10;
    const std::string str = "abc def\r\n\tghij klm nopqrst";
    ASSERT_EQ("abc def\r\n\tghij klm\r\n\tnopqrst", foldHeader<treshold>(str));
}

TEST(FoldTest, foldHeader_noWhitespaceJustCommas_foldsByCommas) {
    const std::string::size_type treshold = 5;
    const std::string str = "abc,de,f,ghij";
    ASSERT_EQ("abc,\r\n\tde,f,\r\n\tghij", foldHeader<treshold>(str));
}

TEST(FoldTest, foldHeader_noWhitespaceNoComma_foldsByTreshold) {
    const std::string::size_type treshold = 10;
    const std::string str = "abcdefghijklmnopqrstuvwxyz";
    ASSERT_EQ("abcdefghij\r\n\tklmnopqrst\r\n\tuvwxyz", foldHeader<treshold>(str));
}

TEST(FoldTest, foldHeader_invalidHeader) {
    const std::string src = "<DUB110-W86585B9760E1E9C700CAAED5A80@phx.gbl>,"
                            "<1013913\n ,,9,1,3,2,7,0,8,0,,@web17j.yandex.ru>,"
                            "<DUB110-W119B3131CF82E03AE8D97DFD5A80@phx.gbl>";
    const std::string::size_type treshold = 54;
    const std::string dst = "<DUB110-W86585B9760E1E9C700CAAED5A80@phx.gbl>,"
                            "<1013913\r\n\t,,9,1,3,2,7,0,8,0,,@web17j.yandex.ru>,\r\n\t"
                            "<DUB110-W119B3131CF82E03AE8D97DFD5A80@phx.gbl>";
    ASSERT_EQ(dst, foldHeader<treshold>(src));
}

TEST(FoldTest, foldHeader_emptyString) {
    const std::string src = "";
    const std::string::size_type treshold = 3;
    ASSERT_EQ(src, foldHeader<treshold>(src));
}

TEST(FoldTest, foldHeader_valuesSeparatedBySpaces_replacesSpacesBySeparator) {
    const std::string::size_type treshold = 3;
    const std::string str = "aaa   aa";
    ASSERT_EQ("aaa\r\n\taa", foldHeader<treshold>(str));
}

TEST(FoldTest, foldHeader_valuesAndCommas_foldByTreshold) {
    const std::string::size_type treshold = 3;
    const std::string str = "aaa,,,";
    ASSERT_EQ("aaa\r\n\t,,,", foldHeader<treshold>(str));
}

TEST(FoldTest, foldHeader_valuesAndCommasSeparatedBySpaces_foldsAndReplacesSpacesBySeparator) {
    const std::string::size_type treshold = 3;
    const std::string str = "aaa   ,,,";
    ASSERT_EQ("aaa\r\n\t,,,", foldHeader<treshold>(str));
}

TEST(FoldTest, foldHeader_values_doesNotChange) {
    const std::string::size_type treshold = 3;
    const std::string str = "aaa";
    ASSERT_EQ(str, foldHeader<treshold>(str));
}

TEST(FoldTest, foldHeader_commas_doesNotChange) {
    const std::string::size_type treshold = 3;
    const std::string str = ",,,";
    ASSERT_EQ(str, foldHeader<treshold>(str));
}

TEST(FoldTest, foldHeader_spacesFitTreshold_replacesByEmptyString) {
    const std::string::size_type treshold = 3;
    const std::string str = "   ";
    ASSERT_EQ("", foldHeader<treshold>(str));
}

TEST(FoldTest, foldHeader_spacesDoesntFitTreshold_replacesByEmptyString) {
    const std::string::size_type treshold = 3;
    const std::string str = "      ";
    ASSERT_EQ("", foldHeader<treshold>(str));
}

TEST(FoldTest, foldHeader_commas_foldsByTreshold) {
    const std::string::size_type treshold = 3;
    const std::string str = ",,,,,,";
    ASSERT_EQ(",,,\r\n\t,,,", foldHeader<treshold>(str));
}

TEST(FoldTest, foldHeader_longCommasHeaderSeparatedBySpaces_foldsByTresholdAndReplacesSpacesBySeparator) {
    const std::string::size_type treshold = 3;
    const std::string str = ",,,,   ,,";
    ASSERT_EQ(",,,\r\n\t,\r\n\t,,", foldHeader<treshold>(str));
}

TEST(FoldTest, foldHeader_headerContainsSpaces_foldsAndSavesInternalSpaces) {
    const std::string::size_type treshold = 3;
    const std::string str = ", , , ,";
    ASSERT_EQ(", ,\r\n\t, ,", foldHeader<treshold>(str));
}

TEST(FoldTest, foldHeader_startAndEndsWithSpaces_removesSpaces) {
    const std::string::size_type treshold = 3;
    const std::string str = "   a   ";
    ASSERT_EQ("a", foldHeader<treshold>(str));
}

}

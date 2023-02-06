
#include <string>
#include <vector>
#include <gtest/gtest.h>

#include <butil/StrUtils/StrUtils.h>

namespace {
using namespace std;

using namespace TStrUtils;

TEST(StringUtils, testFoldSmallLine) {
    string line(50, 'a');
    string foldedLine = foldLine(line);
    ASSERT_EQ(line, foldedLine);
}

TEST(StringUtils, testFoldLongLine) {
    static const string DELIMITER = "\r\n\t";

    string line(78 + 77 + 1, 'a');
    string foldedLine = foldLine(line);
    size_t pos = foldedLine.find(DELIMITER);
    ASSERT_EQ(static_cast<size_t>(78), pos);
    foldedLine.erase(0, pos + DELIMITER.length());
    pos = foldedLine.find(DELIMITER);
    ASSERT_EQ(static_cast<size_t>(77), pos);
}

TEST(StringUtils, isNum_numberString_returnTrue) {
    ASSERT_TRUE(isNum("12345678"));
}

TEST(StringUtils, isNum_emptyString_returnFalse) {
    ASSERT_FALSE(isNum(""));
}

TEST(StringUtils, isNum_stringWithLetters_returnFalse) {
    ASSERT_FALSE(isNum("1234abcd5678ef"));
}

TEST(StringUtils, unescape_stringWithEscapedSymbols_unescape) {
    ASSERT_EQ("abcd\\efghijklmn\n", unescape("\\abcd\\\\efghi\\jklm\\n\n"));
}

TEST(StringUtils, removeSurroundQuotes_stringInQuotes_returnStringWithoutSurroundQuotes) {
    ASSERT_EQ("abcde", removeSurroundQuotes("\"abcde\""));
}

TEST(StringUtils, removeSurroundQuotes_singleFrontQuote_returnUnchanged) {
    ASSERT_EQ("\"abcde", removeSurroundQuotes("\"abcde"));
}

TEST(StringUtils, removeSurroundQuotes_singleBackQuote_returnUnchanged) {
    ASSERT_EQ("abcde\"", removeSurroundQuotes("abcde\""));
}

TEST(StringUtils, removeSurroundQuotes_empty_returnEmpty) {
    ASSERT_EQ("", removeSurroundQuotes(""));
}

}

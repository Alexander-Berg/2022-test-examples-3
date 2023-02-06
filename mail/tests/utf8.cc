
#include <iterator>
#include <string>
#include <locale>
#include <iostream>
#include <boost/range/as_literal.hpp>
#include <boost/bind.hpp>
#include <boost/algorithm/string/case_conv.hpp>
#include <boost/algorithm/string/find.hpp>
#include <boost/iterator/counting_iterator.hpp>
#include <boost/algorithm/string/trim.hpp>
#include <boost/algorithm/string/replace.hpp>
#include <boost/algorithm/string/predicate.hpp>
#include <gtest/gtest.h>

#include <butil/StrUtils/utf8.h>

#pragma GCC diagnostic push
#pragma GCC diagnostic ignored "-Wunused-variable"

namespace {

using namespace std;

std::locale loc("ru_RU.UTF-8");

// Returns a string consisting of the full unicode codepoint range
std::string getFullUnicodeTable()
{
    std::wstring wstr;
    for (int i=1; i<0x10FFFF; ++i) {
        wstr.push_back(wchar_t(i));
    }
    std::string d;
    std::copy(wstr.begin(),
	      wstr.end(),
	      make_utf8_wo_iterator(std::back_inserter(d))
	      );
    return d;
}

const std::string fullUnicodeTable = getFullUnicodeTable();

/*********************************************************************************/

/*
    Copies an input string s to a local string per character.
    Verifies that the string copied is exactly the same as the input string.
*/
bool copyAndCheckEqual(string s)
{
    string d;
    for (string::const_iterator it=s.begin(); *it; it=next_utf8_char(it)) {
        copy_utf8_char(it, back_inserter(d));
    }
    return s == d;
}

TEST(Utf8, CopyUtf8Char_CopyRussian_CopiedEquals) {
    ASSERT_TRUE( copyAndCheckEqual("ТЕСТОВАЯ строка В UTF-8") );
}

TEST(Utf8, CopyUtf8Char_CopyArabic_CopiedEquals) {
    ASSERT_TRUE( copyAndCheckEqual("الله العظيم") );
}

TEST(Utf8, CopyUtf8Char_CopyStylishDigits_CopiedEquals) {
    ASSERT_TRUE( copyAndCheckEqual("０１２３４５６７８９") );
}

TEST(Utf8, CopyUtf8Char_CopyUnprintableUtf8_CopiedEquals) {
    ASSERT_TRUE( copyAndCheckEqual("𝐀𝐴𝐁𝐵𝐂𝐶") );
}

TEST(Utf8, CopyUtf8Char_CopyFullUnicodeTable_CopiedEquals) {
    ASSERT_TRUE( copyAndCheckEqual(fullUnicodeTable) );
}

/*********************************************************************************/

bool copyOrSkipUtf8CharsAndCheckEqual(string s)
{
    std::string d;
    typedef std::string::const_iterator it_t;
    typedef std::back_insert_iterator<std::string> ot_t;
    it_t src(s.begin());
    it_t end(s.end());
    ot_t dst(std::back_inserter(d));
    while (src != end) {
        src = is_valid_utf8_char(src, end) ? copy_utf8_char(src, dst) : next_utf8_char(src, end);
    }
    return s == d;
}

TEST(Utf8, copyOrSkipUtf8Char_CopyRussianAlphabet_CopiedEquals) {
    ASSERT_TRUE( copyOrSkipUtf8CharsAndCheckEqual("АБВГДЕЁЖЗИКЛМНОПРСТУФХЦЧШЩЬЪЭЮЯабвгдеёжзиклмнопрстуфхцчшщьъэюя") );
}

TEST(Utf8, copyOrSkipUtf8Char_CopyArabic_CopiedEquals) {
    ASSERT_TRUE( copyOrSkipUtf8CharsAndCheckEqual("الله العظيم") );
}

TEST(Utf8, copyOrSkipUtf8Char_CopyStylishDigits_CopiedEquals) {
    ASSERT_TRUE( copyOrSkipUtf8CharsAndCheckEqual("０１２３４５６７８９") );
}

TEST(Utf8, copyOrSkipUtf8Char_CopyUnprintableUtf8_CopiedEquals) {
    ASSERT_TRUE( copyOrSkipUtf8CharsAndCheckEqual("𝐀𝐴𝐁𝐵𝐂𝐶") );
}

TEST(Utf8, copyOrSkipUtf8Char_CopyEmpty_CopiedEquals) {
    ASSERT_TRUE( copyOrSkipUtf8CharsAndCheckEqual(""));
}

TEST(Utf8, copyOrSkipUtf8Char_CopyFullUnicodeTable_CopiedEquals) {
    ASSERT_TRUE( copyOrSkipUtf8CharsAndCheckEqual(fullUnicodeTable));
}

/*********************************************************************************/

/*
    Converts an input string s1 to a lowercase local copy and verifies that the result equals the
    second input string s2.
*/
bool toLowerAndCheckEqual(string s1, string s2)
{
    string d;
    for (string::const_iterator it=s1.begin(); *it; it=next_utf8_char(it)) {
        wchar_t w = utf8_char_to_wchar(it);
        wchar_t lw = tolower(w, loc);
        wchar_to_utf8_char(lw, back_inserter(d));
    }
    return s2 == d;
}

TEST(Utf8, toLower_RussianToLower_StringLowercased) {
    ASSERT_TRUE( toLowerAndCheckEqual("ТЕСТОВАЯ строка В UTF-8", "тестовая строка в utf-8") );
}

TEST(Utf8, toLower_UnprintableUtf8ToLower_StringLowercased) {
    ASSERT_TRUE( toLowerAndCheckEqual("𝐀𝐴𝐁𝐵𝐂𝐶", "𝐀𝐴𝐁𝐵𝐂𝐶") );
}

TEST(Utf8, toLower_FullUnicodeTableToLower_NotEqualsSelf) {
    ASSERT_TRUE(!toLowerAndCheckEqual(fullUnicodeTable, fullUnicodeTable) );
}

/*********************************************************************************/

bool toLowerExplicitAndCheckEqual(string s1, string s2)
{
    std::string d;
    typedef std::string::const_iterator it_t;
    typedef utf8_wo_iterator<std::back_insert_iterator<std::string> > ot_t;
    it_t end(s1.end());
    ot_t dst(make_utf8_wo_iterator(std::back_inserter(d)));
    for (it_t src=s1.begin(); src!=end; src=next_utf8_char(src,end)) {
        if (is_valid_utf8_char(src, end)) {
            *dst++ = std::tolower<wchar_t>(utf8_char_to_wchar(src), loc);
        }
    }
    return s2 == d;
}

TEST(Utf8, toLowerExplicit_RussianToLower_StringLowercased) {
    ASSERT_TRUE( toLowerExplicitAndCheckEqual("ТЕСТОВАЯ строка В UTF-8", "тестовая строка в utf-8") );
}

TEST(Utf8, toLowerExplicit_UnprintableUtf8_StringLowercased) {
    ASSERT_TRUE( toLowerExplicitAndCheckEqual("𝐀𝐴𝐁𝐵𝐂𝐶", "𝐀𝐴𝐁𝐵𝐂𝐶") );
}

TEST(Utf8, toLowerExplicit_AsciiInLowercase_NoChange) {
    ASSERT_TRUE( toLowerExplicitAndCheckEqual("hello there", "hello there") );
}

/*********************************************************************************/

size_t getUtf8Length(string s1)
{
    return boost::distance(boost::make_iterator_range(make_utf8_ro_iterator(s1.begin()), make_utf8_ro_iterator(s1.end())));
}

TEST(Utf8, getUtf8Length_Russian_CorrectLength) {
    ASSERT_TRUE( 23 == getUtf8Length("ТЕСТОВАЯ строка В UTF-8") );
}

TEST(Utf8, getUtf8Length_StylishDigits_CorrectLength) {
    ASSERT_TRUE( 10 == getUtf8Length("０１２３４５６７８９") );
}

TEST(Utf8, getUtf8Length_UnprintableUtf8_CorrectLength) {
    ASSERT_TRUE( 6 == getUtf8Length("𝐀𝐴𝐁𝐵𝐂𝐶") );
}

TEST(Utf8, getUtf8Length_French_CorrectLength) {
    ASSERT_TRUE( 55 == getUtf8Length("Au côte de la mer azurée un garçon observait du goéland") );
}

TEST(Utf8, getUtf8Length_FullUnicodeTable_CorrectLength) {
    ASSERT_TRUE( 0x10FFFF-1 == getUtf8Length(fullUnicodeTable) );
}

/********************* Test iterators ********************************************/

/*
    Copies an input string s to a local string using utf8 iterators.
    Verifies that the string copied is exactly the same as the input string.
*/
bool copyByIteratorAndCheckEqual(string s)
{
    char* d = new char[s.length() * 4 + 1];
    utf8_ro_iterator<const char*> s_it(s.c_str());
    utf8_ro_iterator<const char*> s_it_end(s.c_str() + s.length());
    utf8_wo_iterator<char*> d_it(d);
    while (s_it != s_it_end) {
        *d_it++ = *s_it++;
    }
    *d_it = wchar_t(0);
    bool rval =  (s == string(d));
    delete[] d;
    return rval;
}

TEST(Utf8, copyByIterator_Russian_CopiedEquals) {
    ASSERT_TRUE( copyByIteratorAndCheckEqual("ТЕСТОВАЯ строка В UTF-8") );
}

TEST(Utf8, copyByIterator_Arabic_CopiedEquals) {
    ASSERT_TRUE( copyByIteratorAndCheckEqual("الله العظيم") );
}

TEST(Utf8, copyByIterator_StylishDigits_CopiedEquals) {
    ASSERT_TRUE( copyByIteratorAndCheckEqual("０１２３４５６７８９") );
}

TEST(Utf8, copyByIterator_UnprintableUtf8_CopiedEquals) {
    ASSERT_TRUE( copyByIteratorAndCheckEqual("𝐀𝐴𝐁𝐵𝐂𝐶") );
}

TEST(Utf8, copyByIterator_FullUnicodeTable_CopiedEquals) {
    ASSERT_TRUE( copyByIteratorAndCheckEqual(fullUnicodeTable) );
}

/*********************************************************************************/

/*
    Copies an input string s to a local string using utf8 iterators and standard algorithms.
    Verifies that the string copied is exactly the same as the input string.
*/
bool copyByStdAndCheckEqual(string s)
{
    string d;
    std::copy(make_utf8_ro_iterator(s.begin()),
        make_utf8_ro_iterator(s.end()),
        make_utf8_wo_iterator(back_inserter(d)));
    return s == d;
}

TEST(Utf8, copyByStd_Russian_CopiedEquals) {
    ASSERT_TRUE( copyByStdAndCheckEqual("ТЕСТОВАЯ строка В UTF-8") );
}

TEST(Utf8, copyByStd_StylishDigits_CopiedEquals) {
    ASSERT_TRUE( copyByStdAndCheckEqual("０１２３４５６７８９") );
}

TEST(Utf8, copyByStd_UnprintableUtf8_CopiedEquals) {
    ASSERT_TRUE( copyByStdAndCheckEqual("𝐀𝐴𝐁𝐵𝐂𝐶") );
}

TEST(Utf8, copyByStd_FullUnicodeTable_CopiedEquals) {
    ASSERT_TRUE( copyByStdAndCheckEqual(fullUnicodeTable) );
}

/*********************************************************************************/

bool toLowerTransformAndCheckEqual(string s1, string s2)
{
    string d;
    std::transform(make_utf8_ro_iterator(s1.begin()),
        make_utf8_ro_iterator(s1.end()),
        make_utf8_wo_iterator(back_inserter(d)),
        boost::bind(std::tolower<wchar_t>, _1, loc)
    );
    return s2 == d;
}

TEST(Utf8, toLowerTransform_RussianToLower_StringLowercased) {
    ASSERT_TRUE( toLowerTransformAndCheckEqual("ТЕСТОВАЯ строка В UTF-8", "тестовая строка в utf-8") );
}

TEST(Utf8, toLowerTransform_UnprintableUtf8_StringLowercased) {
    ASSERT_TRUE( toLowerTransformAndCheckEqual("𝐀𝐴𝐁𝐵𝐂𝐶", "𝐀𝐴𝐁𝐵𝐂𝐶") );
}

/*********************************************************************************/

bool toLowerBoostAndCheckEqual(string s1, string s2)
{
    string d;
    boost::to_lower_copy(make_utf8_wo_iterator(back_inserter(d)),
        boost::make_iterator_range(make_utf8_ro_iterator(s1.begin()), make_utf8_ro_iterator(s1.end())),
        loc);
    return s2 == d;
}

TEST(Utf8, toLowerBoost_RussianToLower_StringLowercased) {
    ASSERT_TRUE( toLowerBoostAndCheckEqual("ТЕСТОВАЯ строка В UTF-8", "тестовая строка в utf-8") );
}

TEST(Utf8, toLowerBoost_UnprintableUtf8_StringLowercased) {
    ASSERT_TRUE( toLowerBoostAndCheckEqual("𝐀𝐴𝐁𝐵𝐂𝐶", "𝐀𝐴𝐁𝐵𝐂𝐶") );
}

/*********************************************************************************/

TEST(Utf8, Utf8RoWoIteratorsStripWhitespaces_StringWithWhitespaces_CopyStringWithoutWhitespaces) {
    string orig("Т  ЕСТ		ОВАЯ строка В UTF-8");
    string actual;
    std::remove_copy_if(make_utf8_ro_iterator(orig.begin()),
        make_utf8_ro_iterator(orig.end()),
        make_utf8_wo_iterator(back_inserter(actual)),
        boost::bind(std::isspace<wchar_t>, _1, loc)
    );

    string expected("ТЕСТОВАЯстрокаВUTF-8");
    ASSERT_TRUE(expected == actual);
}

/*********************************************************************************/

TEST(Utf8, Utf8WoIterator_IncAndDeref_writeUtf8StringToBuf) {
    char str[256];
    utf8_wo_iterator<char*> it(str);
    *it++ = "А";
    *it++ = "Б";
    *it++ = "В";
    *it++ = "Г";
    *it++ = "Д";
    *it++ = wchar_t(0x0020); // space
    *it++ = wchar_t(0x20AC); // euro currency sign
    *it++ = wchar_t(0x00A3); // pound sign
    *it++ = wchar_t(0x00A5); // yen sign
    *it++ = wchar_t(0);
    ASSERT_TRUE(string(str) == "АБВГД €£¥");
}

/*********************************************************************************/

/*
    Head extraction from utf8-string s1
*/
string getFirstSymbols(string s1, int c)
{
    typedef boost::iterator_range<utf8_ro_iterator<string::const_iterator> > range_t;
    range_t r(make_utf8_ro_iterator(s1.begin()), make_utf8_ro_iterator(s1.end()));
    range_t rhead = boost::find_head(r, c);
    return string(rhead.begin().base(), rhead.end().base());
}

TEST(Utf8, getFirstSymbols_Russian_CorrectSubstring) {
    ASSERT_TRUE( "Тест" == getFirstSymbols("Тестовая строка", 4) );
}

TEST(Utf8, getFirstSymbols_FullLength_ReturnCopy) {
    ASSERT_TRUE( "Тестовая строка" == getFirstSymbols("Тестовая строка", 120) );
}

/*********************************************************************************/

std::string getFirstBytes(string s1, int n)
{
    string::iterator end =
        std::lower_bound(boost::make_counting_iterator(make_utf8_ro_iterator(s1.begin())),
            boost::make_counting_iterator(make_utf8_ro_iterator(s1.end())),
            n+1,
            boost::bind(std::less<std::iterator_traits<string::iterator>::difference_type>(),
                boost::bind(std::distance<string::iterator>,
                    s1.begin(),
                    boost::bind(next_utf8_char<string::iterator>, boost::bind(&utf8_ro_iterator<string::iterator>::base, _1))
                ),
                _2
            )
        )->base();

    return std::string(s1.begin(), end);
}

TEST(Utf8, getFirstBytes_ZeroLength_EmptyString) {
    ASSERT_TRUE( "" == getFirstBytes("АБВГДЕЁЖ", 0) );
}

TEST(Utf8, getFirstBytes_RussianIncompleteNumberOfBytes_CorrectSubstring) {
    ASSERT_TRUE( "АБВ" == getFirstBytes("АБВГДЕЁЖ", 7) );
}

TEST(Utf8, getFirstBytes_RussianCompleteNumberOfBytes_CorrectSubsctring) {
    ASSERT_TRUE( "АБВГ" == getFirstBytes("АБВГДЕЁЖ", 8) );
}

TEST(Utf8, getFirstBytes_Ascii_CorrectSubstring) {
    ASSERT_TRUE( "ABCDEFG" == getFirstBytes("ABCDEFG", 7) );
}

TEST(Utf8, getFirstBytes_RussianWithAscii_CorrectSubstring) {
    ASSERT_TRUE( "Привет!!" == getFirstBytes("Привет!!\ntest", 14) );
}

/*********************************************************************************/

string getFirstBytesSelectBegin(string s1, int n)
{
    string::iterator beg = s1.begin();
    n = std::min<int>(n, int(s1.size()));
    string::iterator e1 = n ? next_utf8_char(beg + std::max<int>(std::min<int>(n, int(s1.size()))-4, 0), beg + n) : beg;
    string::iterator e2 = n ? next_utf8_char(beg + std::max<int>(std::min<int>(n, int(s1.size())-1), 0), s1.end()) : beg;

    string::iterator end =
        std::lower_bound(boost::make_counting_iterator(make_utf8_ro_iterator(e1)),
            boost::make_counting_iterator(make_utf8_ro_iterator(e2)),
            n+1,
            boost::bind(std::less<std::iterator_traits<string::iterator>::difference_type>(),
                boost::bind(std::distance<string::iterator>,
                    beg,
                    boost::bind(next_utf8_char<string::iterator>, boost::bind(&utf8_ro_iterator<string::iterator>::base, _1))
                ),
                _2
            )
        )->base();

    return std::string(beg, end);
}

TEST(Utf8, getFirstBytesSelectBegin_ZeroLength_EmptyString) {
    ASSERT_TRUE( "" == getFirstBytesSelectBegin("АБВГДЕЁЖ", 0) );
}

TEST(Utf8, getFirstBytesSelectBegin_RussianIncompleteNumberOfBytes_CorrectSubstring) {
    ASSERT_TRUE( "АБВ" == getFirstBytesSelectBegin("АБВГДЕЁЖ", 7) );
}

TEST(Utf8, getFirstBytesSelectBegin_RussianCompleteNumberOfBytes_CorrectSubsctring) {
    ASSERT_TRUE( "АБВГ" == getFirstBytesSelectBegin("АБВГДЕЁЖ", 8) );
}

TEST(Utf8, getFirstBytesSelectBegin_Ascii_CorrectSubstring) {
    ASSERT_TRUE( "ABCDEFG" == getFirstBytesSelectBegin("ABCDEFG", 7) );
}

TEST(Utf8, getFirstBytesSelectBegin_RussianWithAscii_CorrectSubstring) {
    ASSERT_TRUE( "Привет!!" == getFirstBytesSelectBegin("Привет!!\ntest", 14) );
}

/*********************************************************************************/

// Tests trim_copy with utf8 iterators
bool trimAndCheckEqual(string s1, string s2)
{
    typedef boost::iterator_range<utf8_ro_iterator<string::const_iterator> > range_t;
    range_t r(make_utf8_ro_iterator(s1.begin()), make_utf8_ro_iterator(s1.end()));
    range_t tr = boost::trim_copy(r, loc);
    return s2 == string(tr.begin().base(), tr.end().base());
}

TEST(Utf8, trim_RussianUtf8WithWhitespaces_TrimmedRussianUtf8) {
    ASSERT_TRUE( trimAndCheckEqual(" 		АБВГД Е  Ё Ж  	\n", "АБВГД Е  Ё Ж") );
}

TEST(Utf8, trim_FullUnicodeTable_NoChange) {
    ASSERT_TRUE( trimAndCheckEqual(fullUnicodeTable, fullUnicodeTable) );
}

/*********************************************************************************/

// Removes slashes from the input string (used in libfeed)
bool removeSlashesAndCheckEqual(string s1, string s2)
{
    string d;
    typedef utf8_ro_iterator<string::const_iterator> iter_t;
    typedef boost::iterator_range<iter_t> range_t;
    range_t r(iter_t(s1.begin()), iter_t(s1.end()));
    boost::replace_all_copy(
        make_utf8_wo_iterator(std::back_inserter(d)),
        r,
        boost::as_literal(L"\\"),
        boost::as_literal(L"")
    );
    return d == s2;
}

TEST(Utf8, removeSlashes_RussianUtf8WithSlashes_RussianUtf8WithoutSlashes) {
    ASSERT_TRUE( removeSlashesAndCheckEqual("\\АБВ\\\\\\ГД\\Е\\", "АБВГДЕ") );
}

TEST(Utf8, removeSlashes_FullUnicodeTable_NotEqualsToSelf) {
    ASSERT_TRUE(!removeSlashesAndCheckEqual(fullUnicodeTable, fullUnicodeTable) );
}

/*********************************************************************************/

// Verifies that pattern is a case-insensitive substring of text
bool containsCaseInsensitive(const string& text, const string& pattern)
{
    return boost::contains(
        boost::make_iterator_range(make_utf8_ro_iterator(text.begin(), text.end()), make_utf8_ro_iterator(text.end(), text.end())),
        boost::make_iterator_range(make_utf8_ro_iterator(pattern.begin(), pattern.end()), make_utf8_ro_iterator(pattern.end(), pattern.end())),
        boost::bind(std::equal_to<wchar_t>(),
            boost::bind(std::tolower<wchar_t>, _1, loc),
            boost::bind(std::tolower<wchar_t>, _2, loc))
	);
}

TEST(Utf8, containsCaseInsensitive_RussianSubstringWrongCase_ReturnTrue) {
    ASSERT_TRUE( containsCaseInsensitive("Я из ЛЕСУ вышел, был сильный мороз","лес") );
}

TEST(Utf8, containsCaseInsensitive_NotSubstring_ReturnFalse) {
    ASSERT_TRUE(!containsCaseInsensitive("invalid utf-8 string test", "test  "));
}

/*********************************************************************************/

} // namespace


#pragma GCC diagnostic pop

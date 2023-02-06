#define BOOST_TEST_MODULE utf8_unit_tests
#include <tests_common.h>

#include <mimeparser/utf8.h>

#include <iterator>
#include <string>
#include <cassert>
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

#ifdef  __clang__
#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Winvalid-source-encoding"
#endif

using namespace std;
using namespace mulca_mime;

std::locale loc("ru_RU.UTF-8");

/*
 * Copies an input string s to a local string per character.
 * Verifies that the string copied is exactly the same as the input string.
 */
bool test1(string s)
{
    string d;
    for (string::const_iterator it=s.begin(); *it; it=next_utf8_char(it)) {
        copy_utf8_char(it, back_inserter(d));
    }
    return s == d;
}

/*
 *  Copies an input string s to a local string using utf8 iterators.
 * Verifies that the string copied is exactly the same as the input string.
 */
bool test1_1(string s)
{
    char* d = new char[s.length() * 4 + 1];
    utf8_ro_iterator<const char*> s_it(s.c_str());
    utf8_ro_iterator<const char*> s_it_end(s.c_str() + s.length());
    utf8_wo_iterator<char*> d_it(d);
    while (s_it != s_it_end) {
        *d_it++ = *s_it++;
    }
    *d_it = wchar_t(0);
    bool rval = (s == string(d));
    delete[] d;
    return rval;
}

/*
 * Copies an input string s to a local string using utf8 iterators and standard
 *   algorithms.
 * Verifies that the string copied is exactly the same as the input string.
 */
bool test1_2(string s)
{
    string d;
    std::copy(make_utf8_ro_iterator(s.begin()),
              make_utf8_ro_iterator(s.end()),
              make_utf8_wo_iterator(back_inserter(d)));
    return s == d;
}

bool test1_3(string s)
{
    std::string d;
    typedef std::string::const_iterator it_t;
    typedef std::back_insert_iterator<std::string> ot_t;
    it_t src(s.begin());
    it_t end(s.end());
    ot_t dst(std::back_inserter(d));
    while (src != end)
        src = is_valid_utf8_char(src, end)
              ? copy_utf8_char(src, dst)
              : next_utf8_char(src, end);
    return s == d;
}

/*
 * Converts an input string s1 to a lowercase local copy and verifies that the
 *   result equals the second input string s2.
 */
bool test2(string s1, string s2)
{
    string d;
    for (string::const_iterator it=s1.begin(); *it; it=next_utf8_char(it)) {
        wchar_t w = utf8_char_to_wchar(it);
        wchar_t lw = tolower(w, loc);
        wchar_to_utf8_char(lw, back_inserter(d));
    }
    return s2 == d;
}

/*
 *  Does the same as test2 but using utf8 iterators and standard algorithms.
 */
bool test2_1(string s1, string s2)
{
    string d;
    std::transform(make_utf8_ro_iterator(s1.begin()),
                   make_utf8_ro_iterator(s1.end()),
                   make_utf8_wo_iterator(back_inserter(d)),
                   boost::bind(std::tolower<wchar_t>, _1, loc)
                  );
    return s2 == d;
}

/*
 *  Does the same as test2 but using boost range concept.
 */
bool test2_2(string s1, string s2)
{
    string d;
    boost::to_lower_copy(make_utf8_wo_iterator(back_inserter(d)),
                         boost::make_iterator_range(make_utf8_ro_iterator(s1.begin()),
                                 make_utf8_ro_iterator(s1.end())),
                         loc);
    return s2 == d;
}

bool test2_3(string s1, string s2)
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


/*
 *  Verifies the length of an input string.
 */
bool test3(string s1, int len)
{
    return boost::distance(boost::make_iterator_range(
                               make_utf8_ro_iterator(s1.begin()),
                               make_utf8_ro_iterator(s1.end())))
           == len;
}

/*
 * Strips whitespace off the first argument and verifies that the result
 *   equals the second argument string.
 */
bool test4(string s1, string s2)
{
    string d;
    std::remove_copy_if(make_utf8_ro_iterator(s1.begin()),
                        make_utf8_ro_iterator(s1.end()),
                        make_utf8_wo_iterator(back_inserter(d)),
                        boost::bind(std::isspace<wchar_t>, _1, loc)
                       );
    return s2 == d;
}

/*
 * Tests basic functionality of utf8_wo_iterator.
 */
bool test5()
{
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
    return string(str) == "АБВГД €£¥";
}

/*
 * Tests head extraction from utf8-string s1 and verifies the result using s2.
 */
bool test6(string s1, int c, string s2)
{
    typedef boost::iterator_range<utf8_ro_iterator<string::const_iterator> > range_t;
    range_t r(make_utf8_ro_iterator(s1.begin()), make_utf8_ro_iterator(s1.end()));
    range_t rhead = boost::find_head(r, c);
    return string(rhead.begin().base(), rhead.end().base()) == s2;
}

/*
 * Does the same as test6 but the limit is in bytes,
 *   as opposed to chars as in the previous test.
 */
bool test7(string s1, int n, string s2)
{
    string::iterator end =
        std::lower_bound(
            boost::make_counting_iterator(make_utf8_ro_iterator(s1.begin())),
            boost::make_counting_iterator(make_utf8_ro_iterator(s1.end())),
            n+1,
            boost::bind(
                std::less<std::iterator_traits<string::iterator>::difference_type>(),
                boost::bind(std::distance<string::iterator>,
                            s1.begin(),
                            boost::bind(next_utf8_char<string::iterator>,
                                        boost::bind(
                                            &utf8_ro_iterator<string::iterator>::base,
                                            _1))
                           ),
                _2))->base();
    return std::string(s1.begin(), end) == s2;
}

/*
 * Does the same as test7, but in a more efficient way.
 */
bool test7_1(string s1, int n, string s2)
{
    string::iterator beg = s1.begin();
    n = std::min<int>(n, static_cast<int>(s1.size()));
    string::iterator e1 = n ? next_utf8_char(beg +
                          std::max<int>(std::min<int>(n, static_cast<int>(s1.size()))-4, 0),
                          beg + n) : beg;
    string::iterator e2 = n ? next_utf8_char(
                              beg + std::max<int>(std::min<int>(n, static_cast<int>(s1.size()))-1, 0),
                              s1.end()) : beg;
    string::iterator end =
        std::lower_bound(
            boost::make_counting_iterator(make_utf8_ro_iterator(e1)),
            boost::make_counting_iterator(make_utf8_ro_iterator(e2)),
            n+1,
            boost::bind(std::less<std::iterator_traits<string::iterator>::difference_type>(),
                        boost::bind(std::distance<string::iterator>,
                                    beg,
                                    boost::bind(
                                        next_utf8_char<string::iterator>,
                                        boost::bind(
                                            &utf8_ro_iterator<string::iterator>::base,
                                            _1))
                                   ),
                        _2))->base();
    return std::string(beg, end) == s2;
}

/*
 * Tests trim_copy with utf8 iterators.
 */
bool test8(string s1, string s2)
{
    typedef boost::iterator_range<utf8_ro_iterator<string::const_iterator> > range_t;
    range_t r(make_utf8_ro_iterator(s1.begin()), make_utf8_ro_iterator(s1.end()));
    range_t tr = boost::trim_copy(r, loc);
    return s2 == string(tr.begin().base(), tr.end().base());
}

/*
 * Removes slashes from the input string (used in libfeed).
 */
bool test9(string s1, string s2)
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

/*
 * Verifies that pattern is a case-insensitive substring of text.
 */
bool test10(const string& text, const string& pattern)
{
    return boost::contains(
               boost::make_iterator_range(make_utf8_ro_iterator(text.begin(),
                                          text.end()),
                                          make_utf8_ro_iterator(text.end(), text.end())),
               boost::make_iterator_range(make_utf8_ro_iterator(pattern.begin(),
                                          pattern.end()),
                                          make_utf8_ro_iterator(pattern.end(), pattern.end())),
               boost::bind(std::equal_to<wchar_t>(),
                           boost::bind(std::tolower<wchar_t>, _1, loc),
                           boost::bind(std::tolower<wchar_t>, _2, loc))
           );
}

 /*
 * Verifies that wchar_to_utf8_char() throw runtime_error on bad wchar_t
 */
bool test11() {
    wchar_t ch = 0x2000000;
    std::string r;
    try {
        wchar_to_utf8_char(ch, std::back_inserter(r));
        return false;
    } catch (const std::runtime_error&) {
        return true;
    }
}

/*
 * Verifies that wchar_to_utf8_char(.., std::nothrow_t) don't throw runtime_error on bad wchar_t
 */
bool test11_1() {
    wchar_t ch = 0x2000000;
    std::string r;
    try {
        bool res = wchar_to_utf8_char(ch, std::back_inserter(r), std::nothrow);
        return !res && r.empty();
    } catch (const std::runtime_error&) {
        return false;
    }
}

/*
 * Returns a string consisting of the full unicode codepoint range.
 */
std::string test_helper()
{
    std::wstring wstr;
    for (int i = 1; i < 0x10FFFF; ++i) {
        if (i >= 0xD800 && i <= 0xDFFF) {
            // UTF-16 surrogate pairs, invalid for wchar_t
            continue;
        }
        wstr.push_back(wchar_t(i));
    }
    std::string d;
    std::copy(wstr.begin(),
              wstr.end(),
              make_utf8_wo_iterator(std::back_inserter(d))
             );
    return d;
}

BOOST_AUTO_TEST_SUITE(utf8_unit_tests)

BOOST_AUTO_TEST_CASE(utf8_unit_tests)
{
    std::string str(test_helper());
    // Test basic functions
    BOOST_CHECK(test1("ТЕСТОВАЯ строка В UTF-8"));
    BOOST_CHECK(test1("الله العظيم"));
    BOOST_CHECK(test1("０１２３４５６７８９"));
    BOOST_CHECK(test1("𝐀𝐴𝐁𝐵𝐂𝐶"));
    BOOST_CHECK(test1(str));
    BOOST_CHECK(test1_3("АБВГДЕЁЖЗИКЛМНОПРСТУФХЦЧШЩЬЪЭЮЯабвгдеёжзиклмнопрстуфхцчшщьъэюя"));
    BOOST_CHECK(test1_3("الله العظيم"));
    BOOST_CHECK(test1_3("０１２３４５６７８９"));
    BOOST_CHECK(test1_3("𝐀𝐴𝐁𝐵𝐂𝐶"));
    BOOST_CHECK(!test1_3("������"));
    BOOST_CHECK(test1_3(str));
    BOOST_CHECK(test2("ТЕСТОВАЯ строка В UTF-8", "тестовая строка в utf-8"));
    BOOST_CHECK(test2("𝐀𝐴𝐁𝐵𝐂𝐶", "𝐀𝐴𝐁𝐵𝐂𝐶"));
    BOOST_CHECK(!test2(str, str));
    BOOST_CHECK(test2_3("ТЕСТОВАЯ строка В UTF-8", "тестовая строка в utf-8"));
    BOOST_CHECK(test2_3("𝐀𝐴𝐁𝐵𝐂𝐶", "𝐀𝐴𝐁𝐵𝐂𝐶"));
    BOOST_CHECK(test2_3("hello ������there", "hello there"));
    BOOST_CHECK(test3("ТЕСТОВАЯ строка В UTF-8", 23));
    BOOST_CHECK(test3("０１２３４５６７８９", 10));
    BOOST_CHECK(test3("𝐀𝐴𝐁𝐵𝐂𝐶", 6));
    BOOST_CHECK(test3("Au côte de la mer azurée un garçon observait du goéland", 55));
    BOOST_CHECK(test3(str, 0x10FFFF - (0xDFFF - 0xD7FF) - 1));
    // Test iterators
    BOOST_CHECK(test1_1("ТЕСТОВАЯ строка В UTF-8"));
    BOOST_CHECK(test1_1("الله العظيم"));
    BOOST_CHECK(test1_1("０１２３４５６７８９"));
    BOOST_CHECK(test1_1("𝐀𝐴𝐁𝐵𝐂𝐶"));
    BOOST_CHECK(test1_1(str));
    BOOST_CHECK(test1_2("ТЕСТОВАЯ строка В UTF-8"));
    BOOST_CHECK(test1_2("０１２３４５６７８９"));
    BOOST_CHECK(test1_2("𝐀𝐴𝐁𝐵𝐂𝐶"));
    BOOST_CHECK(test1_2(str));
    BOOST_CHECK(test2_1("ТЕСТОВАЯ строка В UTF-8", "тестовая строка в utf-8"));
    BOOST_CHECK(test2_1("𝐀𝐴𝐁𝐵𝐂𝐶", "𝐀𝐴𝐁𝐵𝐂𝐶"));
    BOOST_CHECK(test2_2("ТЕСТОВАЯ строка В UTF-8", "тестовая строка в utf-8"));
    BOOST_CHECK(test2_2("𝐀𝐴𝐁𝐵𝐂𝐶", "𝐀𝐴𝐁𝐵𝐂𝐶"));
    BOOST_CHECK(test4("Т  ЕСТ		ОВАЯ строка В UTF-8", "ТЕСТОВАЯстрокаВUTF-8"));
    BOOST_CHECK(test5());
    BOOST_CHECK(test6("Тестовая строка", 4, "Тест"));
    BOOST_CHECK(test6("Тестовая строка", 120, "Тестовая строка"));
    BOOST_CHECK(test7("АБВГДЕЁЖ", 0, ""));
    BOOST_CHECK(test7("АБВГДЕЁЖ", 7, "АБВ"));
    BOOST_CHECK(test7("АБВГДЕЁЖ", 8, "АБВГ"));
    BOOST_CHECK(test7("ABCDEFG", 7, "ABCDEFG"));
    BOOST_CHECK(test7("Привет!!\ntest", 14, "Привет!!"));
    BOOST_CHECK(test7_1("АБВГДЕЁЖ", 0, ""));
    BOOST_CHECK(test7_1("АБВГДЕЁЖ", 7, "АБВ"));
    BOOST_CHECK(test7_1("АБВГДЕЁЖ", 8, "АБВГ"));
    BOOST_CHECK(test7_1("ABCDEFG", 7, "ABCDEFG"));
    BOOST_CHECK(test7_1("Привет!!\ntest", 14, "Привет!!"));
    BOOST_CHECK(test8(" 		АБВГД Е  Ё Ж  	\n", "АБВГД Е  Ё Ж"));
    BOOST_CHECK(test8(str, str));
    BOOST_CHECK(test9("\\АБВ\\\\\\ГД\\Е\\", "АБВГДЕ"));
    BOOST_CHECK(!test9(str, str));
    BOOST_CHECK(test10("Я из ЛЕСУ вышел, был сильный мороз","лес"));
    BOOST_CHECK(!test10("invalid utf-8 string test", "test ���������� ������"));
}

BOOST_AUTO_TEST_SUITE_END()

#ifdef  __clang__
#pragma clang diagnostic pop
#endif

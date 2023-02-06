#include <yandex/functions_stl.h>

#include <cppunit/extensions/HelperMacros.h>

using namespace std;

class FunctionsTest : public CppUnit::TestFixture
{
    CPPUNIT_TEST_SUITE( FunctionsTest );

    CPPUNIT_TEST (UrlEncodeTest1);
    CPPUNIT_TEST (ToLowerTest1);
    CPPUNIT_TEST (ToUpperTest1);
    CPPUNIT_TEST (TrimTest1);
    CPPUNIT_TEST (TrimTest2);
    CPPUNIT_TEST (TrimTest3);
    CPPUNIT_TEST (RemoveBad1);
    /* see comments below
    CPPUNIT_TEST (testShowtime);
    */

    CPPUNIT_TEST (testlong2str1);
    CPPUNIT_TEST (testlong2str2);
    CPPUNIT_TEST (testlong2str3);
    CPPUNIT_TEST (testlong2str4);
    CPPUNIT_TEST (testlong2str5);
    CPPUNIT_TEST (testlong2str6);
    CPPUNIT_TEST (testlong2str7);
    CPPUNIT_TEST (testlong2str8);
    CPPUNIT_TEST (ReplaceTest);

    CPPUNIT_TEST_SUITE_END();

public:
    FunctionsTest() {};
    virtual ~FunctionsTest() {};

    void UrlEncodeTest1()
    {
        string dec = "упкуп%20wret werабвгдеЁжзиклмнопрсАБВГДЕёЖЗИКЛМНОПРСwefr%20324-=.__";
        string enc = "%F3%EF%EA%F3%EF%2520wret+wer%E0%E1%E2%E3%E4%E5%A8%E6%E7%E8%EA%EB%EC%ED%EE%EF%F0%F1%C0%C1%C2%C3%C4%C5%B8%C6%C7%C8%CA%CB%CC%CD%CE%CF%D0%D1wefr%2520324-%3D.__";
        string res1 = urlencode(dec);
        string res2 = urldecode(enc);
        CPPUNIT_ASSERT_EQUAL(enc, res1);
        CPPUNIT_ASSERT_EQUAL(dec, res2);
    }

    void ReplaceTest()
    {
        string str1 = "&amp;amp;";
        string str2 = "&amp;amp;amp;";
        string res = strreplace("&", "&amp;", str1);
        CPPUNIT_ASSERT_EQUAL(str2, res);

        str_replace("&", "&amp;", str1);
        CPPUNIT_ASSERT_EQUAL(str1, res);
    }

    void ToLowerTest1()
    {
        string str1 = "ASS != hands, ЖОПА != ручки";
        string str2 = "ass != hands, ЖОПА != ручки";
        string res = tolower(str1);
        CPPUNIT_ASSERT_EQUAL(str2, res);
    }

    void ToUpperTest1()
    {
        string str1 = "ASS != hands, ЖОПА != ручки";
        string str2 = "ASS != HANDS, ЖОПА != ручки";
        string res = toupper(str1);
        CPPUNIT_ASSERT_EQUAL(str2, res);
    }
    void TrimTest1()
    {
        string str1 = "  trimmed string  ";
        string str2 = "trimmed string";
        string res = trim(str1);
        CPPUNIT_ASSERT_EQUAL(str2, res);
    }
    void TrimTest2()
    {
        string str1 = "\ttrimmed\t \tstring\r\n";
        string str2 = "trimmed\t \tstring";
        string res = trim(str1);
        CPPUNIT_ASSERT_EQUAL(str2, res);
    }
    void TrimTest3()
    {
        string str1 = "trimmed\t \tstring";
        string str2 = "trimmed\t \tstring";
        string res = trim(str1);
        CPPUNIT_ASSERT_EQUAL(str2, res);
    }
    void RemoveBad1()
    {
        string str1 = "\x04\x98\x91жопа'\xab\x97полная\x85\n\"Ass is\"";
        string str2 = " \"'жопа'\"-полная...\n\"Ass is\"";
        string res = remove_bad_symbols(str1);
        CPPUNIT_ASSERT_EQUAL(str2, res);
    }

    /*
     * This test is completely incorrect as, it appears, the function
     * show_time iteslf. Yelds different results i different timezones, that's
     * expected but isn't controlable.
    void testShowtime()
    {
        string res = show_time(1094000000);
        CPPUNIT_ASSERT_EQUAL(string("2004-09-01 04:53:20"), res);
    }
    */

    void testlong2str1()
    {
        int i = 123456;
        string res = long2str(i);
        CPPUNIT_ASSERT_EQUAL(string("123456"), res);
    }

    void testlong2str2()
    {
        int i = -123456;
        string res = long2str(i);
        CPPUNIT_ASSERT_EQUAL(string("-123456"), res);
    }

    void testlong2str3()
    {
        long i = -123456L;
        string res = long2str(i);
        CPPUNIT_ASSERT_EQUAL(string("-123456"), res);
    }

    void testlong2str4()
    {
        unsigned long i = 123456L;
        string res = long2str(i);
        CPPUNIT_ASSERT_EQUAL(string("123456"), res);
    }

    void testlong2str5()
    {
        unsigned long long i = 123456123456LL;
        string res = long2str(i);
        CPPUNIT_ASSERT_EQUAL(string("123456123456"), res);
    }

    void testlong2str6()
    {
        long long i = 123456LL;
        string res = long2str(i);
        CPPUNIT_ASSERT_EQUAL(string("123456"), res);
    }

    void testlong2str7()
    {
        short i = 1234;
        string res = long2str(i);
        CPPUNIT_ASSERT_EQUAL(string("1234"), res);
    }

    void testlong2str8()
    {
        size_t i = 123456;
        string res = long2str(i);
        CPPUNIT_ASSERT_EQUAL(string("123456"), res);
    }
};

CPPUNIT_TEST_SUITE_REGISTRATION( FunctionsTest );

#include <yandex/string_helpers_stl.h>
#include <yandex/test_iter.h>

#include <cppunit/extensions/HelperMacros.h>

using namespace std;


class StringHelpersTest : public CppUnit::TestFixture
{
    CPPUNIT_TEST_SUITE( StringHelpersTest );

    CPPUNIT_TEST (testSplit);
    CPPUNIT_TEST (testSplit2);
    CPPUNIT_TEST (testSplit3);
    CPPUNIT_TEST (testJoin1);
    CPPUNIT_TEST (testJoin2);
    CPPUNIT_TEST (testD2L1);
    CPPUNIT_TEST (testD2L2);

    CPPUNIT_TEST (testBaseName);

    CPPUNIT_TEST_SUITE_END();

public:
    StringHelpersTest() {};
    virtual ~StringHelpersTest() {};

    void testSplit()
    {
        DataVector dv = Split("name,name2,phone,timetocall,address,dop,email", ",");

        DataVector::iterator i = dv.begin();
        CPPUNIT_ASSERT_EQUAL(*i, string("name"));
        ++i;
        CPPUNIT_ASSERT_EQUAL(*i, string("name2"));
        ++i;
        CPPUNIT_ASSERT_EQUAL(*i, string("phone"));
        ++i;
        CPPUNIT_ASSERT_EQUAL(*i, string("timetocall"));
        ++i;
        CPPUNIT_ASSERT_EQUAL(*i, string("address"));
        ++i;
        CPPUNIT_ASSERT_EQUAL(*i, string("dop"));
        ++i;
        CPPUNIT_ASSERT_EQUAL(*i, string("email"));
        ++i;
        CPPUNIT_ASSERT(i == dv.end());
    }

    void testSplit2()
    {
        DataVector dv = Split(",,,", ",");

        DataVector::iterator i = dv.begin();
        CPPUNIT_ASSERT_EQUAL(string(""), *i);
        ++i;
        CPPUNIT_ASSERT_EQUAL(string(""), *i);
        ++i;
        CPPUNIT_ASSERT_EQUAL(string(""), *i);
        ++i;
        CPPUNIT_ASSERT_EQUAL(string(""), *i);
        ++i;
        CPPUNIT_ASSERT(i == dv.end());
    }

    void testSplit3()
    {
        DataVector dv = Split("aa--bb--cc", "--");

        DataVector::iterator i = dv.begin();
        CPPUNIT_ASSERT_EQUAL(string("aa"), *i);
        ++i;
        CPPUNIT_ASSERT_EQUAL(string("bb"), *i);
        ++i;
        CPPUNIT_ASSERT_EQUAL(string("cc"), *i);
        ++i;
        CPPUNIT_ASSERT(i == dv.end());
    }

    void testJoin1()
    {
        DataVector a;
        a.push_back("123");
        a.push_back("234");
        a.push_back("345");
        string res = Join(", ", a);
        CPPUNIT_ASSERT_EQUAL(res, string("123, 234, 345"));
    }

    void testJoin2()
    {
        LongVector a;
        a.push_back(123);
        a.push_back(234);
        a.push_back(345);
        string res = Join(", ", a);
        CPPUNIT_ASSERT_EQUAL(res, string("123, 234, 345"));
    }


    void testBaseName()
    {
        pair<string, string> split = BaseName("/home/bacek/someshit");
        CPPUNIT_ASSERT_EQUAL(string("/home/bacek"), split.first);
        CPPUNIT_ASSERT_EQUAL(string("someshit"), split.second);

        pair<string, string> split2 = BaseName("./someshit/");
        CPPUNIT_ASSERT_EQUAL(string("./someshit"), split2.first);
        CPPUNIT_ASSERT_EQUAL(string(""), split2.second);
    }

    void testD2L1()
    {
        string test = "2,3,5655,43,4,33,55,76,-44,32,0";
        DataVector x = Split(test, ",");
        LongVector y = Data2LongVector(x);
        string res = Join(",", y);
        CPPUNIT_ASSERT_EQUAL(res, test);
    }

    void testD2L2()
    {
        string test = "2,3,5655,43.0,4.03,33,aaa,76,-44,32,";
        string tres = "2,3,5655,43,4,33,0,76,-44,32,0";
        DataVector x = Split(test, ",");
        LongVector y = Data2LongVector(x);
        string res = Join(",", y);
        CPPUNIT_ASSERT_EQUAL(res, tres);
    }
};


CPPUNIT_TEST_SUITE_REGISTRATION( StringHelpersTest );

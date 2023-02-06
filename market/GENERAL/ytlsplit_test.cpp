#include <stdlib.h>
#include <string>
#include <vector>
#include <cstring>
#include <yandex/ytlsplit.h>
#include <yandex/test_iter.h>

#include <cppunit/extensions/HelperMacros.h>


class ytlSplitTest : public CppUnit::TestFixture
{
    CPPUNIT_TEST_SUITE( ytlSplitTest );

    CPPUNIT_TEST (testSplit0);
    CPPUNIT_TEST (testSplit1);
    CPPUNIT_TEST (testSplit2);

    CPPUNIT_TEST_SUITE_END();

public:
    ytlSplitTest() {};
    virtual ~ytlSplitTest() {};

    void testSplit0() {
        const char * src = ",single,";
        ytl::stdstrvec result;
        ytl::split(src, src + strlen(src), ytl::ifchar(','), ytl::strvecbuild(result));
        ytl::stdstrvec::iterator cit = result.begin();
        CPPUNIT_ASSERT(cit != result.end());
        CPPUNIT_ASSERT_EQUAL(*cit, std::string(""));
        ++cit;
        CPPUNIT_ASSERT_EQUAL(*cit, std::string("single"));
        ++cit;
        CPPUNIT_ASSERT_EQUAL(*cit, std::string(""));
        ++cit;
        CPPUNIT_ASSERT(cit == result.end());
    }

    void testSplit1() {
        const std::string src = "bacek is a cool hacker ";
        ytl::stdstrvec result;
        ytl::equator<char> eq(' ');
        ytl::split(src.begin(), src.end(), eq, ytl::strvecbuild(result));
        CPPUNIT_ASSERT_EQUAL(static_cast<size_t>(6), result.size());
        CPPUNIT_ASSERT_EQUAL(result[0], std::string("bacek"));
        CPPUNIT_ASSERT_EQUAL(result[2], std::string("a"));
        CPPUNIT_ASSERT_EQUAL(result[4], std::string("hacker"));
        CPPUNIT_ASSERT_EQUAL(result[5], std::string(""));
    }

    typedef std::vector<long> reslong;
    typedef std::vector<reslong> vecreslong;
    class vecvecbuild {
        vecreslong * vres;
        vecvecbuild();
        public:
        vecvecbuild(vecreslong & in): vres(&in) {}
        template<typename Iter>
        inline void operator()(Iter start, Iter end) {
            vres->push_back(reslong(start, end));
        }
    };

    void testSplit2() {
        const long src[] = {1, 2, 3, 0, 0, 15, 0};
        vecreslong result;
        ytl::equator<long> eq(0);
        ytl::split(src, src + sizeof(src)/sizeof(src[0]), eq, vecvecbuild(result));
        CPPUNIT_ASSERT_EQUAL(static_cast<size_t>(4), result.size());
        CPPUNIT_ASSERT_EQUAL(result[0][0], static_cast<long>(1));
        CPPUNIT_ASSERT_EQUAL(static_cast<size_t>(0), result[1].size());
        CPPUNIT_ASSERT_EQUAL(result[2][0], static_cast<long>(15));
        CPPUNIT_ASSERT_EQUAL(static_cast<size_t>(0), result[3].size());
    }
};


CPPUNIT_TEST_SUITE_REGISTRATION( ytlSplitTest );



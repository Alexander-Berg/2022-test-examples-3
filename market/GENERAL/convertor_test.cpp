#include <string>
#include <fstream>
#include <iterator>
#include <cppunit/TestFixture.h>
#include <cppunit/extensions/HelperMacros.h>

#include "yandex/convertor.h"

class ConvertorTest : public CppUnit::TestFixture
{
public:
        void testEmpty();
        void testLatin();
        void testMixedDefault();
        void testMixedIgnoring();
        void testMixedEscaping();
        void testCompat1();
        void testCompat2();
        void testCompat3();

private:
        CPPUNIT_TEST_SUITE(ConvertorTest);
        CPPUNIT_TEST(testEmpty);
        CPPUNIT_TEST(testLatin);
        CPPUNIT_TEST(testMixedDefault);
        CPPUNIT_TEST(testMixedIgnoring);
        CPPUNIT_TEST(testMixedEscaping);
        CPPUNIT_TEST(testCompat1);
        CPPUNIT_TEST(testCompat2);
        CPPUNIT_TEST(testCompat3);
        CPPUNIT_TEST_SUITE_END();
};

CPPUNIT_TEST_SUITE_REGISTRATION(ConvertorTest);

void
ConvertorTest::testEmpty() {

        namespace yx = yandex;

        std::string str;
        std::auto_ptr<yx::Convertor> conv = yx::Convertor::createEscaping("utf-8", "koi8-r");
        CPPUNIT_ASSERT_EQUAL(str, conv->convert(str));
}

void
ConvertorTest::testLatin() {

        namespace yx = yandex;

        std::string str("12345 abcde");
        std::auto_ptr<yx::Convertor> conv = yx::Convertor::createEscaping("koi8-r", "utf-8");
        CPPUNIT_ASSERT_EQUAL(str, conv->convert(str));
}

void
ConvertorTest::testMixedDefault() {

        namespace yx = yandex;

        std::auto_ptr<yx::Convertor> conv = yx::Convertor::createDefault("utf-8", "cp1251");

        CPPUNIT_ASSERT_EQUAL(std::string("?stone"), conv->convert("¹stone"));
        CPPUNIT_ASSERT_EQUAL(std::string("r?sum?"), conv->convert("résumé"));
        CPPUNIT_ASSERT_EQUAL(std::string("??????"), conv->convert("ტექსტი"));
        CPPUNIT_ASSERT_EQUAL(std::string("deportaci?n"), conv->convert("deportación"));
}

void
ConvertorTest::testMixedIgnoring() {

        namespace yx = yandex;

        std::auto_ptr<yx::Convertor> conv = yx::Convertor::createIgnoring("utf-8", "cp1251");

        CPPUNIT_ASSERT_EQUAL(std::string("stone"), conv->convert("¹stone"));
        CPPUNIT_ASSERT_EQUAL(std::string("rsum"), conv->convert("résumé"));
        CPPUNIT_ASSERT_EQUAL(std::string(""), conv->convert("ტექსტი"));
        CPPUNIT_ASSERT_EQUAL(std::string("deportacin"), conv->convert("deportación"));
}

void
ConvertorTest::testMixedEscaping() {

        namespace yx = yandex;

        std::auto_ptr<yx::Convertor> conv = yx::Convertor::createEscaping("utf-8", "cp1251");

        CPPUNIT_ASSERT_EQUAL(std::string("&#185;stone"), conv->convert("¹stone"));
        CPPUNIT_ASSERT_EQUAL(std::string("r&#233;sum&#233;"), conv->convert("résumé"));
        CPPUNIT_ASSERT_EQUAL(std::string("&#4322;&#4308;&#4325;&#4321;&#4322;&#4312;"), conv->convert("ტექსტი"));
        CPPUNIT_ASSERT_EQUAL(std::string("deportaci&#243;n"), conv->convert("deportación"));
}

void
ConvertorTest::testCompat1() {

        Convertor conv("UTF-8", "CP1251");

        CPPUNIT_ASSERT_EQUAL(std::string("&#185;stone"), conv.conv2("¹stone"));
        CPPUNIT_ASSERT_EQUAL(std::string("r&#233;sum&#233;"), conv.conv2("résumé"));
        CPPUNIT_ASSERT_EQUAL(std::string("&#4322;&#4308;&#4325;&#4321;&#4322;&#4312;"), conv.conv2("ტექსტი"));
        CPPUNIT_ASSERT_EQUAL(std::string("deportaci&#243;n"), conv.conv2("deportación"));
}

void
ConvertorTest::testCompat2() {

        Convertor conv("UTF-8", "CP1251");
        std::string to;
        conv.conv2(to, "¹stone");
        CPPUNIT_ASSERT_EQUAL(std::string("&#185;stone"), to);
        conv.conv2(to, "résumé");
        CPPUNIT_ASSERT_EQUAL(std::string("r&#233;sum&#233;"), to);
        conv.conv2(to, "ტექსტი");
        CPPUNIT_ASSERT_EQUAL(std::string("&#4322;&#4308;&#4325;&#4321;&#4322;&#4312;"), to);
        conv.conv2(to, "deportación");
        CPPUNIT_ASSERT_EQUAL(std::string("deportaci&#243;n"), to);
}

void
ConvertorTest::testCompat3() {

        Convertor conv("UTF-8", "CP1251");
        std::string to;
        to = conv.conv2(to, "¹stone");
        CPPUNIT_ASSERT_EQUAL(std::string("&#185;stone"), to);
        to = conv.conv2(to, "résumé");
        CPPUNIT_ASSERT_EQUAL(std::string("r&#233;sum&#233;"), to);
        to = conv.conv2(to, "ტექსტი");
        CPPUNIT_ASSERT_EQUAL(std::string("&#4322;&#4308;&#4325;&#4321;&#4322;&#4312;"), to);
        to = conv.conv2(to, "deportación");
        CPPUNIT_ASSERT_EQUAL(std::string("deportaci&#243;n"), to);
}

#include <extsearch/geo/kernel/xml_writer/dom.h>

#include <library/cpp/testing/unittest/registar.h>

Y_UNIT_TEST_SUITE(TXmlDomTest) {
    Y_UNIT_TEST(Pool) {
        NXmlWr::NDetail::TPool pool;
        UNIT_ASSERT_VALUES_EQUAL(pool.Store(123), "123");

        UNIT_ASSERT(pool.Store("").empty());

        TString abacaba = "abacaba";
        UNIT_ASSERT_VALUES_EQUAL(pool.Store(abacaba), abacaba);

        TStringBuf twoZeroBytes = pool.Store("\x00\x00");
        UNIT_ASSERT_VALUES_EQUAL(twoZeroBytes.length(), 2);

        const char* emptyCStr = "\x00\x00";
        TStringBuf empty = pool.Store(emptyCStr);
        UNIT_ASSERT_VALUES_EQUAL(empty.length(), 0);

        const char arrayOfZeros[] = "\x00\x00\x00";
        UNIT_ASSERT_VALUES_EQUAL(pool.Store(arrayOfZeros).length(), 3);

        UNIT_ASSERT_VALUES_EQUAL(pool.Store("aba"
                                            "caba"),
                                 "abacaba");

        UNIT_ASSERT_VALUES_EQUAL(pool.Store(TString{"aba"} + "caba"), "abacaba");
    }
}

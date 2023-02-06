#include <crypta/lib/native/unixtime_parser/unixtime_parser.h>

#include <library/cpp/testing/unittest/registar.h>


using namespace NCrypta;

Y_UNIT_TEST_SUITE(NUnixtimeParser) {
    Y_UNIT_TEST(Positive) {
        UNIT_ASSERT_EQUAL(NUnixtimeParser::Parse("1500000000"), 1500000000u);
    }

    Y_UNIT_TEST(Negative) {
        UNIT_ASSERT_EQUAL(NUnixtimeParser::Parse("150000000"), Nothing());
        UNIT_ASSERT_EQUAL(NUnixtimeParser::Parse("15000000000"), Nothing());
        UNIT_ASSERT_EQUAL(NUnixtimeParser::Parse("150000000x"), Nothing());
    }
}

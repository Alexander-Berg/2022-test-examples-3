#include <gtest/gtest.h>
#include <gmock/gmock.h>
#include <pgg/numeric_cast.h>

namespace {

using namespace testing;

struct NumericCastTest : public Test {};

TEST(NumericCastTest, from_int_to_int_should_succeed) {
    EXPECT_EQ(PGG_NUMERIC_CAST(int, 42), 42);
}

TEST(NumericCastTest, from_int_to_short_with_overflow_should_throw_exception) {
    const std::string expected =
        "Bad numeric cast of value which is 10000 in "
        "virtual void {anonymous}::NumericCastTest_from_int_to_short_with_overflow_should_throw_exception_Test::TestBody(): "
        "bad numeric conversion: positive overflow";
    const int value = 10000;
    try {
        PGG_NUMERIC_CAST(short, value);
    } catch (const pgg::NumericCastError& error) {
        EXPECT_EQ(std::string(error.what()), expected);
    }
}

} // namespace

#include <gtest/gtest.h>
#include <macs/types.h>
#include <macs/check_arguments.h>

namespace {

using namespace macs;
using namespace testing;

TEST(CheckArgumentsNotEmptyArgTest, forEmptyMid_throwsException) {
    Mid arg;
    EXPECT_THROW(ASSERT_NOT_EMPTY_ARG(arg), ParamsException);
}

TEST(CheckArgumentsNotEmptyArgTest, forEmptyMids_throwsException) {
    Mids arg = {};
    EXPECT_THROW(ASSERT_NOT_EMPTY_ARG(arg), ParamsException);
}

TEST(CheckArgumentsNoEmptyItemsTest, forMidsWithEmptyMid_throwsException) {
    Mids arg = {"1", "2", ""};
    EXPECT_THROW(ASSERT_NO_EMPTY_ITEMS(arg), ParamsException);
}

TEST(CheckArgumentsBigintArgTest, forEmptyMid_throwsException) {
    Mid arg;
    EXPECT_THROW(ASSERT_BIGINT_ARG(arg), ParamsException);
}

TEST(CheckArgumentsBigintArgTest, forNonNumericMid_throwsException) {
    Mid arg = "null";
    EXPECT_THROW(ASSERT_BIGINT_ARG(arg), ParamsException);
}

TEST(CheckArgumentsBigintArgTest, forTooBigMid_throwsException) {
    Mid arg = "18446744073709551599";
    EXPECT_THROW(ASSERT_BIGINT_ARG(arg), ParamsException);
}

TEST(CheckArgumentsBigintArgTest, forBigintMid_throwsNothing) {
    Mid arg = "8446744073709551599";
    EXPECT_NO_THROW(ASSERT_BIGINT_ARG(arg));
}

TEST(CheckArgumentsIntegerArgTest, forEmptyLid_throwsException) {
    Lid arg;
    EXPECT_THROW(ASSERT_INTEGER_ARG(arg), ParamsException);
}

TEST(CheckArgumentsIntegerArgTest, forNonNumericLid_throwsException) {
    Lid arg = "null";
    EXPECT_THROW(ASSERT_INTEGER_ARG(arg), ParamsException);
}

TEST(CheckArgumentsIntegerArgTest, forTooBigLid_throwsException) {
    Lid arg = "3147483647";
    EXPECT_THROW(ASSERT_INTEGER_ARG(arg), ParamsException);
}

TEST(CheckArgumentsIntegerArgTest, forIntegerLid_throwsNothing) {
    Lid arg = "1147483647";
    EXPECT_NO_THROW(ASSERT_INTEGER_ARG(arg));
}

TEST(CheckArgumentsBigintItemsTest, forMidsWithEmptyMid_throwsException) {
    Mids arg = {"1", "2", ""};
    EXPECT_THROW(ASSERT_BIGINT_ITEMS(arg), ParamsException);
}

TEST(CheckArgumentsBigintItemsTest, forMidsWithNonNumericMid_throwsException) {
    Mids arg = {"1", "2", "null"};
    EXPECT_THROW(ASSERT_BIGINT_ITEMS(arg), ParamsException);
}

TEST(CheckArgumentsBigintItemsTest, forMidsWithTooBigMid_throwsException) {
    Mids arg = {"1", "2", "18446744073709551599"};
    EXPECT_THROW(ASSERT_BIGINT_ITEMS(arg), ParamsException);
}

TEST(CheckArgumentsBigintItemsTest, forBigintMids_throwsNothing) {
    Mids arg = {"1", "2", "8446744073709551599"};
    EXPECT_NO_THROW(ASSERT_BIGINT_ITEMS(arg));
}

TEST(CheckArgumentsIntegerItemsTest, forLidsWithEmptyLid_throwsException) {
    Lids arg = {"1", "2", ""};
    EXPECT_THROW(ASSERT_INTEGER_ITEMS(arg), ParamsException);
}

TEST(CheckArgumentsIntegerItemsTest, forLidsWithNonNumericLid_throwsException) {
    Lids arg = {"1", "2", "null"};
    EXPECT_THROW(ASSERT_INTEGER_ITEMS(arg), ParamsException);
}

TEST(CheckArgumentsIntegerItemsTest, forLidsWithTooBigLid_throwsException) {
    Lids arg = {"1", "2", "3147483647"};
    EXPECT_THROW(ASSERT_INTEGER_ITEMS(arg), ParamsException);
}

TEST(CheckArgumentsIntegerItemsTest, forIntegerLids_throwsNothing) {
    Lids arg = {"1", "2", "1147483647"};
    EXPECT_NO_THROW(ASSERT_INTEGER_ITEMS(arg));
}

}

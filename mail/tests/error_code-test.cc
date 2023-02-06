#include <gtest/gtest.h>
#include <gmock/gmock.h>
#include "errors.h"

namespace {

using namespace testing;

struct ErrorCodeTest : public Test {
    ErrorCodeTest() {}
};

TEST_F(ErrorCodeTest, error_code_constructed_from_enum_has_apropriate_category) {
    EXPECT_EQ(mail_errors::error_code(errors1::logic).category(), errors1::getCategory());
}

TEST_F(ErrorCodeTest, error_code_constructed_from_enum_has_apropriate_value) {
    EXPECT_EQ(mail_errors::error_code(errors1::logic).value(), errors1::logic);
}

TEST_F(ErrorCodeTest, bool_operator_for_default_error_code_returns_false) {
    EXPECT_FALSE(mail_errors::error_code());
}

TEST_F(ErrorCodeTest, negation_operator_for_default_error_code_returns_true) {
    EXPECT_TRUE(!mail_errors::error_code());
}

TEST_F(ErrorCodeTest, bool_operator_for_error_code_with_non_zero_value_returns_true) {
    EXPECT_TRUE(mail_errors::error_code(errors1::logic));
}

TEST_F(ErrorCodeTest, negation_operator_for_error_code_with_non_zero_value_returns_false) {
    EXPECT_FALSE(!mail_errors::error_code(errors1::logic));
}

TEST_F(ErrorCodeTest, bool_operator_for_error_code_with_zero_value_returns_false) {
    EXPECT_FALSE(mail_errors::error_code(errors1::ok));
}

TEST_F(ErrorCodeTest, negation_operator_for_error_code_with_zero_value_returns_true) {
    EXPECT_TRUE(!mail_errors::error_code(errors1::ok));
}

TEST_F(ErrorCodeTest, error_codes_with_same_category_but_different_value_are_not_equal) {
    EXPECT_NE(mail_errors::error_code(errors1::logic), mail_errors::error_code(errors1::input));
}

TEST_F(ErrorCodeTest, error_codes_with_same_value_but_different_category_are_not_equal) {
    EXPECT_NE(mail_errors::error_code(errors1::logic), mail_errors::error_code(errors2::logic));
}

TEST_F(ErrorCodeTest, error_codes_with_same_category_and_value_but_different_custom_message_are_equal) {
    EXPECT_EQ(mail_errors::error_code(errors1::logic, "custom msg1"),
            mail_errors::error_code(errors1::logic, "custom msg2"));
}

TEST_F(ErrorCodeTest, error_code_with_custom_message_and_error_code_without_it_are_equal) {
    EXPECT_EQ(mail_errors::error_code(errors1::logic, "custom msg1"),
            mail_errors::error_code(errors1::logic));
}

TEST_F(ErrorCodeTest, error_code_constructed_from_enum_returns_corresponding_message) {
    EXPECT_EQ(mail_errors::error_code(errors1::logic).message(), "logic error");
}

TEST_F(ErrorCodeTest, error_code_constructed_from_enum_and_custom_message_returns_custom_message) {
    EXPECT_EQ(mail_errors::error_code(errors1::logic, "custom msg1").message(), "custom msg1");
}

} // namespace

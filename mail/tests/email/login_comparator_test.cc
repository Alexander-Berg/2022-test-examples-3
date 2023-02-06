
#include <gtest/gtest.h>

#include <butil/email/login_comparator.h>

TEST(LoginComparatorTest, alphaNumericLoginsAreEquals) {
    ASSERT_TRUE(loginsEquals("abc123", "abc123"));
}

TEST(LoginComparatorTest, loginsWithDotAreEquals) {
    ASSERT_TRUE(loginsEquals("abc.123", "abc.123"));
}

TEST(LoginComparatorTest, loginsWithDashAreEquals) {
    ASSERT_TRUE(loginsEquals("abc-123", "abc-123"));
}

TEST(LoginComparatorTest, loginWithDotEqualsToLoginWithDash) {
    ASSERT_TRUE(loginsEquals("abc.123", "abc-123"));
}

TEST(LoginComparatorTest, loginWithPlusEqualsToLoginWithoutPlus) {
    ASSERT_TRUE(loginsEquals("abc+123", "abc"));
}

TEST(LoginComparatorTest, loginAreEqualsIgnoreCase) {
    ASSERT_TRUE(loginsEquals("abc", "ABC"));
}

TEST(LoginComparatorTest, alphaNumericLoginNotEqualsToLoginWithDot) {
    ASSERT_FALSE(loginsEquals("abc123", "abc.123"));
}

TEST(LoginComparatorTest, loginsWithDotInDiferrentPlacesNotEquals) {
    ASSERT_FALSE(loginsEquals("a.bc123", "abc.123"));
}

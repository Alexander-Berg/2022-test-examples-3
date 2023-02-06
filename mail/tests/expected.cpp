#include <yamail/expected.h>
#include <boost/system/error_code.hpp>

#include <gtest/gtest.h>
#include <gmock/gmock.h>


TEST(ValueOrThrowTest, shouldThrowOnUnexpected) {
    const mail_errors::error_code ec = make_error_code(boost::system::errc::bad_address);
    const yamail::expected<int> unexpected = yamail::make_unexpected(ec);
    const yamail::expected<void> unexpectedVoid = yamail::make_unexpected(ec);

    EXPECT_THROW(unexpected.value_or_throw(), mail_errors::system_error);
    EXPECT_THROW(unexpectedVoid.value_or_throw(), mail_errors::system_error);
}

TEST(ValueOrThrowTest, shouldNotThrowOnExpected) {
    const yamail::expected<int> expected(1);
    const yamail::expected<int> empty;
    const yamail::expected<void> emptyVoid;

    EXPECT_NO_THROW(empty.value_or_throw());
    EXPECT_NO_THROW(emptyVoid.value_or_throw());
    EXPECT_NO_THROW(expected.value_or_throw());
}

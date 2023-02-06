#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <mail/webmail/corgi/include/trycatch.h>
#include <mail/webmail/corgi/include/types_error.h>


using namespace ::testing;

namespace corgi::tests {

TEST(TryCatchHandlerTest, shouldNotRaiseExceptionOnSuccessCallback) {
    yamail::expected<void> result;

    EXPECT_NO_THROW(trycatch(result, [] () {}));
    EXPECT_TRUE(static_cast<bool>(result));
}

TEST(TryCatchHandlerTest, shouldRethrowForcedUndind) {
    using T = boost::coroutines::detail::forced_unwind;
    yamail::expected<void> result;

    EXPECT_THROW(trycatch(result, [] () { throw T(); }), T);
}

TEST(TryCatchHandlerTest, shouldSetResultInCaseOfSystemError) {
    yamail::expected<void> result;
    const auto ec = make_error(AccessError::accessDenied, "what");

    EXPECT_NO_THROW(trycatch(result, [&] () { throw mail_errors::system_error(ec); }));
    EXPECT_EQ(result.error(), ec);
}

TEST(TryCatchHandlerTest, shouldSetResultInCaseOfException) {
    yamail::expected<void> result;
    const std::string what = "what";
    const auto ec = make_error(UnexpectedError::exception, what);

    EXPECT_NO_THROW(trycatch(result, [&] () { throw std::runtime_error(what); }));
    EXPECT_EQ(result.error(), ec);
}

}

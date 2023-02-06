#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <mail/hound/include/internal/wmi/service/passport.h>

namespace {

using namespace ::testing;
using namespace ::hound;


struct PassportResponseTest : Test {
    const PassportResponse withRetriableError = {.status="error", .errors=std::vector<std::string>{"backend.blackbox_failed"}, .httpCode=200};
    const PassportResponse withNotRetriableError = {"error", std::vector<std::string>{"uzumymw"}, 200};
    const PassportResponse withMixedErrors = {"error", std::vector<std::string>{"uzumymw", "backend.blackbox_failed"}, 200};
};

TEST_F(PassportResponseTest, initialStateShouldHaveNotRetriableErrors) {
    PassportResponse r;
    EXPECT_TRUE(r.hasError());
    EXPECT_FALSE(r.hasOnlyRetriableErrors());
}

TEST_F(PassportResponseTest, hasErrorShouldReturnTrueForAnyError) {
    EXPECT_TRUE(withRetriableError.hasError());
    EXPECT_TRUE(withNotRetriableError.hasError());
}

TEST_F(PassportResponseTest, hasOnlyRetriableErrorsShouldReturnTrueOnlyForRetriableError) {
    EXPECT_TRUE(withRetriableError.hasOnlyRetriableErrors());
    EXPECT_FALSE(withNotRetriableError.hasOnlyRetriableErrors());
    EXPECT_FALSE(withMixedErrors.hasOnlyRetriableErrors());
}

TEST_F(PassportResponseTest, httpCode5xxShouldBeRetriable) {
    PassportResponse r = {.errors=std::nullopt, .httpCode=500};
    EXPECT_TRUE(r.hasError());
    EXPECT_TRUE(r.hasOnlyRetriableErrors());
}

TEST_F(PassportResponseTest, httpCode4xxShouldBeNotRetriable) {
    PassportResponse r = {.errors=std::nullopt, .httpCode=400};
    EXPECT_TRUE(r.hasError());
    EXPECT_FALSE(r.hasOnlyRetriableErrors());
}

}

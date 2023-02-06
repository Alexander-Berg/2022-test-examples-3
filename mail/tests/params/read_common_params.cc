#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include "parse.h"


namespace sendbernar {
namespace tests {

TEST(CommonParams, shouldParseSuccessfully) {
    CREATE_REQ;
    RETURN_ARG("uid", "1");
    RETURN_ARG_OPT("caller", "2");
    RETURN_HEADER("X-Request-Id", "reqid");
    RETURN_HEADER_TWICE("X-Real-Ip", "127.0.0.1");
    RETURN_HEADER("X-Original-Host", "mail.ru");
    RETURN_HEADER_OPT_EMPTY("X-Forwarded-For");

    const auto params = getCommonParams(REQ, "");

    EXPECT_EQ(params.uid, "1");
    EXPECT_EQ(params.caller, "2");
    EXPECT_EQ(params.requestId, "reqid");
    EXPECT_EQ(params.realIp, "127.0.0.1");
    EXPECT_EQ(params.originalHost, "mail.ru");
}

TEST(CommonParams, shouldParseSuccessfullyWithXForwardedFor) {
    CREATE_REQ;
    RETURN_ARG("uid", "1");
    RETURN_ARG_OPT("caller", "2");
    RETURN_HEADER("X-Request-Id", "reqid");
    RETURN_HEADER_TWICE("X-Real-Ip", "127.0.0.1");
    RETURN_HEADER("X-Original-Host", "mail.ru");
    RETURN_HEADER("X-Forwarded-For", "::1");

    const auto params = getCommonParams(REQ, "");

    EXPECT_EQ(params.uid, "1");
    EXPECT_EQ(params.caller, "2");
    EXPECT_EQ(params.requestId, "reqid");
    EXPECT_EQ(params.realIp, "::1");
    EXPECT_EQ(params.originalHost, "mail.ru");
}

TEST(CommonParams, shouldThrowExceptionOnMissingUid) {
    CREATE_REQ;
    RETURN_ARG_OPT("caller", "2");
    RETURN_ARG_OPT_EMPTY("uid");

    ASSERT_THROW(getCommonParams(REQ, ""), NoSuchEntry);
}

TEST(CommonParams, shouldNotThrowExceptionOnMissingCallerOrEmptyHeaders) {
    CREATE_REQ;
    RETURN_ARG("uid", "2");
    RETURN_ARG_OPT_EMPTY("caller");
    RETURN_HEADER_OPT_EMPTY("X-Request-Id");
    RETURN_HEADER_OPT_EMPTY_TWICE("X-Real-Ip");
    RETURN_HEADER_OPT_EMPTY("X-Forwarded-For");
    RETURN_HEADER_OPT_EMPTY("X-Original-Host");

    const auto params = getCommonParams(REQ, "");

    EXPECT_EQ(params.uid, "2");
    EXPECT_EQ(params.caller, "");

    EXPECT_EQ(params.requestId, "");
    EXPECT_EQ(params.realIp, "");
    EXPECT_EQ(params.originalHost, "");
}

TEST(CommonParams, shouldAssingRemoteAddressWhichIsPassedToFunctionIfXRealIpIsEmptyOrAbsent) {
    CREATE_REQ;
    RETURN_ARG("uid", "2");
    RETURN_ARG_OPT_EMPTY("caller");
    RETURN_HEADER_OPT_EMPTY("X-Request-Id");
    RETURN_HEADER_OPT_EMPTY_TWICE("X-Real-Ip");
    RETURN_HEADER_OPT_EMPTY("X-Original-Host");
    RETURN_HEADER_OPT_EMPTY("X-Forwarded-For");

    const auto params = getCommonParams(REQ, "123");

    EXPECT_EQ(params.realIp, "123");
}

}
}

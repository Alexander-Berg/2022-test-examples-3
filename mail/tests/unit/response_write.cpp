#include <gtest/gtest.h>

#include <ymod_smtpserver/response.h>

using namespace ymod_smtpserver;
using namespace testing;

TEST(ResponseWrite, ResponseContainsOnlyCode) {
    Response response(250, "");
    std::ostringstream os;
    os << response;
    EXPECT_EQ("250\r\n", os.str());
}

TEST(ResponseWrite, OneLineResponseWithoutEnhancedCode) {
    Response response(555, "Syntax error");
    std::ostringstream os;
    os << response;
    EXPECT_EQ("555 Syntax error\r\n", os.str());
}

TEST(ResponseWrite, OneLineResponseWithEmptyText) {
    Response response(451, "", EnhancedStatusCode(451));
    std::ostringstream os;
    os << response;
    EXPECT_EQ("451 4.5.1\r\n", os.str());
}

TEST(ResponseWrite, OneLineResponseWithEnhancedCode) {
    Response response(555, "Syntax error", EnhancedStatusCode(500));
    std::ostringstream os;
    os << response;
    EXPECT_EQ("555 5.0.0 Syntax error\r\n", os.str());
}

TEST(ResponseWrite, MultiLineResponseWithoutEnhancedCode) {
    Response response(555, "Line 1\nLine 2\nLine 3\n");
    std::ostringstream os;
    os << response;
    EXPECT_EQ("555-Line 1\r\n555-Line 2\r\n555 Line 3\r\n", os.str());
}

TEST(ResponseWrite, MultiLineResponseWithEnhancedCode) {
    Response response(555, "Line 1\nLine 2\nLine 3\n", EnhancedStatusCode(200));
    std::ostringstream os;
    os << response;
    EXPECT_EQ("555-2.0.0 Line 1\r\n555-2.0.0 Line 2\r\n555 2.0.0 Line 3\r\n", os.str());
}


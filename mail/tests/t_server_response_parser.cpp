#include <gtest/gtest.h>
#include <server_response.h>

using namespace ymod_smtpclient;
using namespace ymod_smtpclient::server;
using namespace testing;

TEST(parseResponse, SimpleLine) {
    std::string simple = "250 Ok";
    auto parsed = parseResponse(simple);

    EXPECT_TRUE(parsed.second); // is the last line
    auto resp = parsed.first;
    EXPECT_EQ(resp.replyCode, 250);
    EXPECT_EQ(resp.data, "Ok");
    EXPECT_FALSE(resp.enhancedCode.is_initialized());
}

TEST(parseResponse, SimpleLineWithCRLF) {
    std::string simple = "250 Ok: Ok, Ok.\n\r\r\n\r";
    auto parsed = parseResponse(simple);

    EXPECT_TRUE(parsed.second); // is the last line
    auto resp = parsed.first;
    EXPECT_EQ(resp.replyCode, 250);
    EXPECT_EQ(resp.data, "Ok: Ok, Ok.");
    EXPECT_FALSE(resp.enhancedCode.is_initialized());
}

TEST(parseResponse, SimpleNoLastLine) {
    std::string respStr = "220-example.com some info...";
    auto parsed = parseResponse(respStr);

    EXPECT_FALSE(parsed.second); // is the last line
    auto resp = parsed.first;
    EXPECT_EQ(resp.replyCode, 220);
    EXPECT_EQ(resp.data, "example.com some info...");
    EXPECT_FALSE(resp.enhancedCode.is_initialized());
}

TEST(parseResponse, SimpleNoLastLineWithDigits) {
    std::string respStr = "550-1.2.3 line begins with digits";
    auto parsed = parseResponse(respStr);

    EXPECT_FALSE(parsed.second); // is the last line
    auto resp = parsed.first;
    EXPECT_EQ(resp.replyCode, 550);
    EXPECT_EQ(resp.data, "1.2.3 line begins with digits");
    EXPECT_FALSE(resp.enhancedCode.is_initialized());
}

TEST(parseResponse, LineWithEnhancedStatusCode) {
    std::string withEnhancedCode = "250 2.0.0 Ok: queued on mxback1j.yandex.net";
    auto parsed = parseResponse(withEnhancedCode);

    EXPECT_TRUE(parsed.second);
    auto resp = parsed.first;
    EXPECT_EQ(resp.replyCode, 250);
    EXPECT_EQ(resp.data, "Ok: queued on mxback1j.yandex.net");
    ASSERT_TRUE(resp.enhancedCode.is_initialized());
    EXPECT_EQ(resp.enhancedCode->classType, EnhancedStatusCode::ClassType::Success);
    EXPECT_EQ(resp.enhancedCode->subjectType, EnhancedStatusCode::SubjectType::Undefined);
    EXPECT_EQ(resp.enhancedCode->detail, 0);

    withEnhancedCode = "450 5.12.345 Temp error!";
    parsed = parseResponse(withEnhancedCode);

    EXPECT_TRUE(parsed.second);
    resp = parsed.first;
    EXPECT_EQ(resp.replyCode, 450);
    EXPECT_EQ(resp.data, "Temp error!");
    ASSERT_TRUE(resp.enhancedCode.is_initialized());
    EXPECT_EQ(resp.enhancedCode->classType, EnhancedStatusCode::ClassType::PermanentFailure);
    EXPECT_EQ(resp.enhancedCode->subjectType, static_cast<EnhancedStatusCode::SubjectType>(12));
    EXPECT_EQ(resp.enhancedCode->detail, 345);
}

TEST(parseResponse, InvalidEnhancedCode_Ignore) {
    std::string line = "334 1.5.890 line with invalid enhanced code: ignore !";
    auto parsed = parseResponse(line);

    EXPECT_TRUE(parsed.second);
    auto resp = parsed.first;
    EXPECT_EQ(resp.replyCode, 334);
    EXPECT_EQ(resp.data, "1.5.890 line with invalid enhanced code: ignore !");
    EXPECT_FALSE(resp.enhancedCode.is_initialized());
}

TEST(parseResponse, InvalidReplyCodeFewDigits_Throw) {
    std::string line = "1 Invalid code: too few digits";
    EXPECT_THROW(parseResponse(line), std::runtime_error);
}

TEST(parseResponse, InvalidReplyCodeManyDigits_Throw) {
    std::string line = "123456 Invalid Code: too many digits";
    EXPECT_THROW(parseResponse(line), std::runtime_error);
}

TEST(parseResponse, InvalidReplyCodeNotNumber_Throw) {
    std::string line = "1xy Invalid Code: code is not a number";
    EXPECT_THROW(parseResponse(line), std::runtime_error);
}

TEST(parseResponse, MissingReplyCode_Throw) {
    std::string line = "Line begins with alphabetic symbols";
    EXPECT_THROW(parseResponse(line), std::runtime_error);
}

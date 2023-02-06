#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <mail/sendbernar/composer/include/attach_helpers.h>

namespace {

using namespace testing;

TEST(CalcAttachHashTest, should_return_same_result_for_same_input) {
    const std::string body = "body";
    const std::string name = "name";

    const auto res1 = sendbernar::calcHashAttachment(body, name);
    const auto res2 = sendbernar::calcHashAttachment(body, name);

    ASSERT_EQ(res1, res2);
}

TEST(CalcAttachHashTest, should_return_different_results_for_different_name) {
    const std::string body = "body";
    const std::string name = "name";
    const std::string anotherName = "another name";

    const auto res1 = sendbernar::calcHashAttachment(body, name);
    const auto res2 = sendbernar::calcHashAttachment(body, anotherName);

    ASSERT_NE(res1, res2);
}

TEST(CalcAttachHashTest, should_return_different_results_for_different_body) {
    const std::string body = "body";
    const std::string anotherBody = "another body";
    const std::string name = "name";

    const auto res1 = sendbernar::calcHashAttachment(body, name);
    const auto res2 = sendbernar::calcHashAttachment(anotherBody, name);

    ASSERT_NE(res1, res2);
}

}

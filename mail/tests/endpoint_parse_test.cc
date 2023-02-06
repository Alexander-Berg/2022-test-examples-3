#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <mail/http_getter/client/include/endpoint_reflection.h>
#include <yamail/data/deserialization/yaml.h>

using namespace testing;

namespace http_getter::tests {

const std::string usual = R"(
url: http://yandex.ru
method: /mail
tvm_service: name
timeout_ms:
    connect: 500
    total: 1000
tries: 5
log_response_body: true
log_post_body: true
keep_alive: true
)";

const std::string withFallback = R"(
url: http://yandex.ru
method: /mail
fallback: http://google.com
tvm_service: name
timeout_ms:
    connect: 500
    total: 1000
tries: 2
log_response_body: true
log_post_body: true
keep_alive: true
)";

const std::string withFallbackAndManyTries = R"(
url: http://yandex.ru
method: /mail
fallback: http://google.com
tvm_service: name
timeout_ms:
    connect: 500
    total: 1000
tries: 3
log_response_body: true
log_post_body: true
keep_alive: true
)";

const std::string withFallbackAndWrongNumberOfTries = R"(
url: http://yandex.ru
method: /mail
fallback: http://google.com
tvm_service: name
timeout_ms:
    connect: 500
    total: 1000
tries: 1
log_response_body: true
log_post_body: true
keep_alive: true
)";

TEST(EndpointParseTest, shouldParseParseHttpConfigWithoutFallback) {
    EXPECT_NO_THROW(yamail::data::deserialization::fromYaml<Endpoint>(withFallback));
}

TEST(EndpointParseTest, shouldParseParseHttpConfigWithFallback) {
    EXPECT_NO_THROW(yamail::data::deserialization::fromYaml<Endpoint>(withFallback));
    EXPECT_NO_THROW(yamail::data::deserialization::fromYaml<Endpoint>(withFallbackAndManyTries));
}

TEST(EndpointParseTest, shouldThrowAnExceptionOnWrongNumberOfTriesWithFallback) {
    EXPECT_THROW(yamail::data::deserialization::fromYaml<Endpoint>(withFallbackAndWrongNumberOfTries),
                 std::runtime_error);
}

}

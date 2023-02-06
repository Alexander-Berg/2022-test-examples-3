#include "mocks.hpp"
#include <ymod_webserver_helpers/format/json.hpp>
#include <gtest/gtest.h>

namespace {

using namespace testing;
using namespace ymod_webserver::helpers::tests;
using namespace ymod_webserver::helpers::format;

struct MockedOutputStreamWithWrite {
    MOCK_METHOD(void, write, (const char*, std::size_t), ());
};

struct JsonTest : public Test {};

TEST(JsonTest, write_for_string_should_call_write) {
    const std::string value("foo");
    const auto formatted = json(value);
    MockedOutputStreamWithWrite stream;
    const InSequence s;
    EXPECT_CALL(stream, write(StrEq("\""), 1)).WillOnce(Return());
    EXPECT_CALL(stream, write(StrEq("foo"), 3)).WillOnce(Return());
    EXPECT_CALL(stream, write(StrEq("\""), 1)).WillOnce(Return());
    formatted.write(stream);
}

TEST(JsonTest, apply_for_body_should_call_argument_function_with_serialized_into_json_value) {
    const std::string value("foo");
    const auto formatted = json(value);
    MockedStringFunction callback;
    EXPECT_CALL(callback, call("\"foo\""));
    formatted.apply_for_body(callback);
}

TEST(JsonTest, content_type_for_string_should_return_application_json) {
    const auto& result = Json<std::string>::content_type();
    EXPECT_EQ(result.type, "application");
    EXPECT_EQ(result.subType, "json");
}

} // namespace

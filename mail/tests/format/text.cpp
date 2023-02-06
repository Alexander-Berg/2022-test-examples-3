#include "mocks.hpp"
#include <ymod_webserver_helpers/format/text.hpp>
#include <gtest/gtest.h>
#include <gmock/gmock.h>

namespace {

using namespace testing;
using namespace ymod_webserver::helpers::tests;
using namespace ymod_webserver::helpers::format;

struct TextTest : public Test {};

TEST(TextTest, write_for_string_should_call_operator_left_shift) {
    std::string value("foo");
    const auto formatted = text(value);
    MockedStringStream stream;
    EXPECT_CALL(stream, write(value)).WillOnce(Return());
    formatted.write(stream);
}

struct MockedConstCharPtrStream {
    MOCK_METHOD(void, write, (const char*), ());

    MockedConstCharPtrStream& operator<<(const char* value) {
        write(value);
        return *this;
    }
};

TEST(TextTest, write_for_const_char_ptr_should_call_operator_left_shift) {
    const char* value = "foo";
    const auto formatted = text(value);
    MockedConstCharPtrStream stream;
    EXPECT_CALL(stream, write(value)).WillOnce(Return());
    formatted.write(stream);
}

struct MockedIntStream {
    MOCK_METHOD(void, write, (int), ());

    MockedIntStream& operator<<(int value) {
        write(value);
        return *this;
    }
};

TEST(TextTest, write_for_int_should_call_operator_left_shift) {
    const int value = 42;
    const auto formatted = text(value);
    MockedIntStream stream;
    EXPECT_CALL(stream, write(value)).WillOnce(Return());
    formatted.write(stream);
}

TEST(TextTest, apply_for_body_on_string_should_call_argument_function_with_original_value) {
    std::string value("foo");
    const auto formatted = text(value);
    MockedStringFunction callback;
    EXPECT_CALL(callback, call(value));
    formatted.apply_for_body(callback);
}

struct MockedConstCharPtrFunction {
    MOCK_METHOD(void, call, (const char*), ());

    void operator ()(const char* v) {
        call(v);
    }
};

TEST(TextTest, apply_for_body_on_const_char_ptr_should_call_argument_function_with_original_value) {
    const char* value = "foo";
    const auto formatted = text(value);
    MockedConstCharPtrFunction callback;
    EXPECT_CALL(callback, call(value));
    formatted.apply_for_body(callback);
}

TEST(TextTest, apply_for_body_on_int_should_call_argument_function_with_serialized_into_string_value) {
    const int value = 42;
    const auto formatted = text(value);
    MockedStringFunction callback;
    EXPECT_CALL(callback, call("42"));
    formatted.apply_for_body(callback);
}

TEST(TextTest, content_type_for_string_should_text_plain) {
    const auto& result = Text<std::string>::content_type();
    EXPECT_EQ(result.type, "text");
    EXPECT_EQ(result.subType, "plain");
}

TEST(TextTest, content_type_for_const_char_ptr_should_return_text_pain) {
    const auto& result = Text<const char*>::content_type();
    EXPECT_EQ(result.type, "text");
    EXPECT_EQ(result.subType, "plain");
}

TEST(TextTest, content_type_for_int_should_return_text_plain) {
    const auto& result = Text<int>::content_type();
    EXPECT_EQ(result.type, "text");
    EXPECT_EQ(result.subType, "plain");
}

} // namespace

#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <src/server/error.hpp>
#include <src/server/handlers/parameters.hpp>

namespace collie::server {

template <class T, class Tag>
bool operator ==(const JsonParameter<T, Tag>& lhs, const JsonParameter<T, Tag>& rhs) {
    return lhs.value == rhs.value;
}

template <class T, class Tag>
bool operator ==(const LexicalCastParameter<T, Tag>& lhs, const LexicalCastParameter<T, Tag>& rhs) {
    return lhs.value == rhs.value;
}

} // namespace collie::server

namespace {

using namespace testing;
using namespace boost::hana::literals;

using collie::error_code;
using collie::expected;
using collie::make_expected_from_error;
using collie::server::Error;
using collie::server::JsonParameter;
using collie::server::LexicalCastParameter;
using collie::server::StringParameter;

struct TestServerHandlersUid : Test {};

using StringParam = StringParameter<decltype(""_s)>;

TEST(TestServerHandlersStringParameter, ctor_for_string_should_construct_object) {
    const std::string value{"uid"};
    EXPECT_EQ(StringParam{value}.value, value);
}

struct TestServerHandlersJsonParameter : Test {};

using JsonParam = JsonParameter<std::vector<std::int64_t>, decltype(""_s)>;

TEST(TestServerHandlersJsonParameter, make_for_json_array_of_integers_should_return_object_with_vector_of_int) {
    EXPECT_EQ(
        JsonParam::make("[1, 2]"),
        (std::variant<JsonParam, expected<void>>(JsonParam {{1, 2}}))
    );
}

TEST(TestServerHandlersJsonParameter, ctor_for_invalid_json_should_return_error) {
    EXPECT_EQ(
        JsonParam::make("1, 2"),
        (std::variant<JsonParam, expected<void>>(make_expected_from_error<void>(error_code(Error::invalidParameter))))
    );
}

struct TestServerHandlersLexicalCastParameter : Test {};

using LexicalCastParam = LexicalCastParameter<std::int64_t, decltype(""_s)>;

TEST(TestServerHandlersLexicalCastParameter, make_for_string_with_integer_should_return_object_with_int) {
    EXPECT_EQ(
        LexicalCastParam::make("42"),
        (std::variant<LexicalCastParam, expected<void>>(LexicalCastParam {42}))
    );
}

TEST(TestServerHandlersLexicalCastParameter, make_for_invalid_json_should_return_error) {
    EXPECT_EQ(
        LexicalCastParam::make("foo"),
        (std::variant<LexicalCastParam, expected<void>>(make_expected_from_error<void>(error_code(Error::invalidParameter))))
    );
}

} // namespace

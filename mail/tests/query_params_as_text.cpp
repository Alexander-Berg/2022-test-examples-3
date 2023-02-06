#include <ozo/yandex/logdog/tskv.h>
#include <ozo/ext/std/vector.h>
#include <ozo/query_builder.h>

#include <gtest/gtest.h>
#include <gmock/gmock.h>

BOOST_FUSION_DEFINE_STRUCT((), test_composite,
    (std::string, name)
    (int64_t, id)
)

BOOST_FUSION_DEFINE_STRUCT((), test_composite_with_composite,
    (std::string, text)
    (std::vector<test_composite>, items)
)

namespace {

using namespace ::testing;
using namespace std::literals;

TEST(stream_query_param, should_quote_string) {
    std::ostringstream s;
    ozo::yandex::stream_query_param(s, "text"s);
    EXPECT_EQ(s.str(), "\"text\""s);
}

TEST(stream_query_param, should_quote_cstring) {
    std::ostringstream s;
    ozo::yandex::stream_query_param(s, "text");
    EXPECT_EQ(s.str(), "\"text\""s);
}

TEST(stream_query_param, should_quote_string_view) {
    std::ostringstream s;
    ozo::yandex::stream_query_param(s, "text"sv);
    EXPECT_EQ(s.str(), "\"text\""s);
}

TEST(stream_query_param, should_stream_array_in_curly_braces_with_comma_delimiter) {
    std::ostringstream s;
    ozo::yandex::stream_query_param(s, std::vector<int64_t>{1, 2, 3, 777});
    EXPECT_EQ(s.str(), "{1,2,3,777}"s);
}

TEST(stream_query_param, should_stream_tuple_in_braces_with_comma_delimiter) {
    std::ostringstream s;
    ozo::yandex::stream_query_param(s, std::make_tuple(1, 2, 3, "tuple is streamed for free"));
    EXPECT_EQ(s.str(), "(1,2,3,\"tuple is streamed for free\")"s);
}

TEST(stream_query_param, should_stream_composite_in_braces_with_comma_delimiter) {
    std::ostringstream s;
    ozo::yandex::stream_query_param(s, test_composite("composite is streamed", 555));
    EXPECT_EQ(s.str(), "(\"composite is streamed\",555)"s);
}

TEST(stream_query_param, should_stream_nested_composite_in_braces_with_comma_delimiter) {
    std::ostringstream s;
    ozo::yandex::stream_query_param(s, test_composite_with_composite("some text", {
        test_composite("first", 1), test_composite("last", 99) }));
    EXPECT_EQ(s.str(), "(\"some text\",{(\"first\",1),(\"last\",99)})"s);
}

TEST(stream_query_param, should_stream_duration_with_units) {
    std::ostringstream s;
    ozo::yandex::stream_query_param(s, std::chrono::microseconds(500));
    EXPECT_EQ(s.str(), "\"500us\""s);
}

TEST(query_params_as_text, should_print_params_in_right_order) {
    using namespace ozo::literals;
    const auto query = "SELECT * FROM a WHERE A="_SQL + int64_t(42) + " AND B="_SQL + "test"s;
    std::ostringstream s;
    s << ozo::yandex::query_params_as_text(query.build());
    EXPECT_EQ(s.str(), "$1=42,$2=\"test\""s);
}

} // namespace

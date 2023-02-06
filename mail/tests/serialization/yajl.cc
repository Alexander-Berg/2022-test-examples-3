#include <gtest/gtest.h>

#include <yamail/data/serialization/yajl.h>

namespace {

using namespace testing;

using namespace yamail::data::serialization;

TEST(toJson, bool_should_return_buffer_with_bool) {
    EXPECT_EQ("true", toJson<bool>(true).str());
}

TEST(toJson, short_should_return_buffer_with_number) {
    EXPECT_EQ("42", toJson<short>(42).str());
}

TEST(toJson, unsigned_short_should_return_buffer_with_number) {
    EXPECT_EQ("42", toJson<unsigned short>(42).str());
}

TEST(toJson, int_should_return_buffer_with_number) {
    EXPECT_EQ("42", toJson<int>(42).str());
}

TEST(toJson, unsigned_int_should_return_buffer_with_number) {
    EXPECT_EQ("42", toJson<unsigned int>(42).str());
}

TEST(toJson, long_should_return_buffer_with_number) {
    EXPECT_EQ("42", toJson<long>(42).str());
}

TEST(toJson, unsigned_long_should_return_buffer_with_number) {
    EXPECT_EQ("42", toJson<unsigned long>(42).str());
}

TEST(toJson, long_long_should_return_buffer_with_number) {
    EXPECT_EQ("42", toJson<long long>(42).str());
}

TEST(toJson, unsigned_long_long_should_return_buffer_with_number) {
    EXPECT_EQ("42", toJson<unsigned long long>(42).str());
}

TEST(toJson, std_string_view_should_return_buffer_with_number) {
    EXPECT_EQ("\"string view\"", toJson(std::string_view("string view")).str());
}

TEST(writeJson, int_should_write_to_stream) {
    std::ostringstream stream;
    writeJson(stream, 42);
    EXPECT_EQ("42", stream.str());
}

TEST(writeJson, int_with_root_should_write_to_stream) {
    std::ostringstream stream;
    writeJson(stream, 42, "root");
    EXPECT_EQ(R"json({"root":42})json", stream.str());
}

TEST(writeJson, vector_of_optional_should_skip_none_item) {
    std::ostringstream stream;
    writeJson(stream, std::vector<boost::optional<int>>({42, boost::none, 13}));
    EXPECT_EQ("[42,13]", stream.str());
}

TEST(writeJson, with_OptForceNull_vector_of_optional_should_serialize_none_item_as_null) {
    std::ostringstream stream;
    writeJson(stream,
        std::vector<boost::optional<int>>({42, boost::none, 13}),
        options(yajl::OptForceNull{}));
    EXPECT_EQ("[42,null,13]", stream.str());
}

TEST(writeJson, map_int_to_optional_should_skip_none_item) {
    std::ostringstream stream;
    writeJson(stream, std::map<std::string, boost::optional<int>>({{"a", 42}, {"b", boost::none}}));
    EXPECT_EQ(R"json({"a":42})json", stream.str());
}

TEST(writeJson, with_OptForceNull_map_to_optional_should_serialize_none_item_as_null) {
    std::ostringstream stream;
    writeJson(stream,
        std::map<std::string, boost::optional<int>>({{"a", 42}, {"b", boost::none}}),
        options(yajl::OptForceNull{}));
    EXPECT_EQ(R"json({"a":42,"b":null})json", stream.str());
}

} // namespace

BOOST_FUSION_DEFINE_STRUCT((),WithOptinalInt, (boost::optional<int>, optinalInt))

namespace {

TEST(writeJson, struct_with_none_optinal_should_skip_none_value) {
    std::ostringstream stream;
    writeJson(stream, WithOptinalInt {boost::none});
    EXPECT_EQ("{}", stream.str());
}

TEST(writeJson, with_OptForceNull_struct_with_none_optinal_should_serialize_none_value_as_null) {
    std::ostringstream stream;
    writeJson(stream, WithOptinalInt {boost::none},
        options(yajl::OptForceNull{}));
    EXPECT_EQ(R"json({"optinalInt":null})json", stream.str());
}

} // namespace

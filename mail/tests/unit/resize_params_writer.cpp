#include <src/services/resize/resize_params_writer.hpp>

#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wold-style-cast"
#pragma clang diagnostic ignored "-Wsign-conversion"
#include <yamail/data/reflection/details/types.h>
#pragma clang diagnostic pop

#include <boost/fusion/adapted/struct/define_struct.hpp>
#include <mail/retriever/tests/unit/gtest.h>
#include <mail/retriever/tests/unit/gmock.h>

BOOST_FUSION_DEFINE_STRUCT((), OneStringParam,
    (std::string, param)
)

BOOST_FUSION_DEFINE_STRUCT((), TwoStringParams,
    (std::string, first)
    (std::string, second)
)

BOOST_FUSION_DEFINE_STRUCT((), OneBoolParam,
    (bool, param)
)

using Map = std::map<std::string, std::string>;

BOOST_FUSION_DEFINE_STRUCT((), MapParam,
    (Map, param)
)

BOOST_FUSION_DEFINE_STRUCT((), OptionalParam,
    (boost::optional<std::string>, param)
)

BOOST_FUSION_DEFINE_STRUCT((), IntParam,
    (int, param)
)

namespace {

using namespace testing;
using namespace retriever;

using yamail::data::reflection::applyVisitor;
using yamail::data::reflection::namedItemTag;

struct ResizeParamsWriterTest : public Test {};

TEST(ResizeParamsWriterTest, apply_visitor_to_one_string_param_struct) {
    OneStringParam params {"foo"};
    std::ostringstream stream;
    ResizeParamsWriter writer(stream);
    applyVisitor(params, writer, namedItemTag(""));
    EXPECT_EQ(stream.str(), "param=foo");
}

TEST(ResizeParamsWriterTest, apply_visitor_to_two_string_params_struct) {
    TwoStringParams params {"foo", "bar"};
    std::ostringstream stream;
    ResizeParamsWriter writer(stream);
    applyVisitor(params, writer, namedItemTag(""));
    EXPECT_EQ(stream.str(), "first=foo&second=bar");
}

TEST(ResizeParamsWriterTest, apply_visitor_to_one_bool_param_struct_with_value_true) {
    OneBoolParam params {true};
    std::ostringstream stream;
    ResizeParamsWriter writer(stream);
    applyVisitor(params, writer, namedItemTag(""));
    EXPECT_EQ(stream.str(), "param=yes");
}

TEST(ResizeParamsWriterTest, apply_visitor_to_one_bool_param_struct_with_value_false) {
    OneBoolParam params {false};
    std::ostringstream stream;
    ResizeParamsWriter writer(stream);
    applyVisitor(params, writer, namedItemTag(""));
    EXPECT_EQ(stream.str(), "param=no");
}

TEST(ResizeParamsWriterTest, apply_visitor_to_optional_param_struct_with_initialized_value) {
    OptionalParam params {std::string("foo")};
    std::ostringstream stream;
    ResizeParamsWriter writer(stream);
    applyVisitor(params, writer, namedItemTag(""));
    EXPECT_EQ(stream.str(), "param=foo");
}

TEST(ResizeParamsWriterTest, apply_visitor_to_optional_param_struct_with_uninitialized_value) {
    OptionalParam params {boost::none};
    std::ostringstream stream;
    ResizeParamsWriter writer(stream);
    applyVisitor(params, writer, namedItemTag(""));
    EXPECT_EQ(stream.str(), "");
}

TEST(ResizeParamsWriterTest, apply_visitor_to_int_param_struct) {
    IntParam params {42};
    std::ostringstream stream;
    ResizeParamsWriter writer(stream);
    applyVisitor(params, writer, namedItemTag(""));
    EXPECT_EQ(stream.str(), "param=42");
}

} // namespace

#ifdef YMOD_WEBSERVER_HELPERS_ENABLE_XML

#include "mocks.hpp"
#include <ymod_webserver_helpers/format/xml.hpp>
#include <gtest/gtest.h>

namespace {

using namespace testing;
using namespace ymod_webserver::helpers::tests;
using namespace ymod_webserver::helpers::format;

struct XmlTest : public Test {};

TEST(XmlTest, write_for_string_should_call_write) {
    const std::map<std::string, std::string> value({{"foo", "bar"}});
    const auto formatted = xml(value);
    MockedStringStream stream;
    EXPECT_CALL(stream, write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<foo>bar</foo>\n")).WillOnce(Return());
    formatted.write(stream);
}

TEST(XmlTest, apply_for_body_should_call_argument_function_with_serialized_into_xml_value) {
    const std::map<std::string, std::string> value({{"foo", "bar"}});
    const auto formatted = xml(value);
    MockedStringFunction callback;
    EXPECT_CALL(callback, call("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<foo>bar</foo>\n"));
    formatted.apply_for_body(callback);
}

TEST(XmlTest, content_type_for_string_should_return_text_xml) {
    const auto& result = Xml<std::map<std::string, std::string>>::content_type();
    EXPECT_EQ(result.type, "text");
    EXPECT_EQ(result.subType, "xml");
}

} // namespace

#endif // YMOD_WEBSERVER_HELPERS_ENABLE_XML

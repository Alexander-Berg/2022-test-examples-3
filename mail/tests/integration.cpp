#include "mocks.hpp"
#include "transfer_encoding/mocks.hpp"
#include <ymod_webserver_helpers/helpers.hpp>
#include <gtest/gtest.h>

namespace {

using namespace testing;
using namespace ymod_webserver::helpers::tests;
using namespace ymod_webserver::helpers;
using namespace ymod_webserver::helpers::format;
using namespace ymod_webserver::helpers::transfer_encoding;

struct IntegrationTest : public Test {};

TEST(IntegrationTest, ok_with_fixed_size_text_should_call_response_methods) {
    MockedResponse response;

    const InSequence s;

    EXPECT_CALL(response, set_code(ymod_webserver::codes::ok, _)).WillOnce(Return());
    EXPECT_CALL(response, set_content_type("text", "plain")).WillOnce(Return());
    EXPECT_CALL(response, result_body("foo")).WillOnce(Return());

    Response(response).ok(fixed_size(text("foo")));
}

TEST(IntegrationTest, ok_with_chunked_text_should_call_response_methods) {
    MockedResponse response;
    const auto stream = boost::make_shared<MockedStreamable>();

    const InSequence s;

    EXPECT_CALL(response, set_code(ymod_webserver::codes::ok, _)).WillOnce(Return());
    EXPECT_CALL(response, set_connection(false)).WillOnce(Return());
    EXPECT_CALL(response, set_content_type("text", "plain")).WillOnce(Return());
    EXPECT_CALL(response, result_chunked()).WillOnce(Return(stream));
    EXPECT_CALL(*stream, client_stream()).WillOnce(Invoke([] { return yplatform::net::streamer_wrapper(new Streamer); }));

    Response(response).ok(chunked(text("foo")));

    EXPECT_TRUE(Mock::VerifyAndClearExpectations(stream.get()));
}

TEST(IntegrationTest, ok_with_fixed_size_json_should_call_response_methods) {
    MockedResponse response;
    const std::string value("foo");

    const InSequence s;

    EXPECT_CALL(response, set_code(ymod_webserver::codes::ok, _)).WillOnce(Return());
    EXPECT_CALL(response, set_content_type("application", "json")).WillOnce(Return());
    EXPECT_CALL(response, result_body("\"foo\"")).WillOnce(Return());

    Response(response).ok(fixed_size(json(value)));
}

TEST(IntegrationTest, ok_with_chunked_json_should_call_response_methods) {
    MockedResponse response;
    const auto stream = boost::make_shared<MockedStreamable>();
    const std::string value("foo");

    const InSequence s;

    EXPECT_CALL(response, set_code(ymod_webserver::codes::ok, _)).WillOnce(Return());
    EXPECT_CALL(response, set_connection(false)).WillOnce(Return());
    EXPECT_CALL(response, set_content_type("application", "json")).WillOnce(Return());
    EXPECT_CALL(response, result_chunked()).WillOnce(Return(stream));
    EXPECT_CALL(*stream, client_stream()).WillOnce(Invoke([] { return yplatform::net::streamer_wrapper(new Streamer); }));

    Response(response).ok(chunked(json(value)));

    EXPECT_TRUE(Mock::VerifyAndClearExpectations(stream.get()));
}

#ifdef YMOD_WEBSERVER_HELPERS_ENABLE_XML

TEST(IntegrationTest, ok_with_fixed_size_xml_should_call_response_methods) {
    MockedResponse response;
    const std::map<std::string, std::string> value({{"foo", "bar"}});

    EXPECT_CALL(response, set_code(ymod_webserver::codes::ok, _)).WillOnce(Return());
    EXPECT_CALL(response, set_content_type("text", "xml")).WillOnce(Return());
    EXPECT_CALL(response, result_body("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<foo>bar</foo>\n")).WillOnce(Return());

    const InSequence s;

    Response(response).ok(fixed_size(xml(value)));
}

TEST(IntegrationTest, ok_with_chunked_xml_should_call_response_methods) {
    MockedResponse response;
    const auto stream = boost::make_shared<MockedStreamable>();
    const std::map<std::string, std::string> value({{"foo", "bar"}});

    const InSequence s;

    EXPECT_CALL(response, set_code(ymod_webserver::codes::ok, _)).WillOnce(Return());
    EXPECT_CALL(response, set_connection(false)).WillOnce(Return());
    EXPECT_CALL(response, set_content_type("text", "xml")).WillOnce(Return());
    EXPECT_CALL(response, result_chunked()).WillOnce(Return(stream));
    EXPECT_CALL(*stream, client_stream()).WillOnce(Invoke([] { return yplatform::net::streamer_wrapper(new Streamer); }));

    Response(response).ok(chunked(xml(value)));

    EXPECT_TRUE(Mock::VerifyAndClearExpectations(stream.get()));
}

#endif // YMOD_WEBSERVER_HELPERS_ENABLE_XML

} // namespace

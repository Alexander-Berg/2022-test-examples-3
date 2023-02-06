#include "mocks.hpp"
#include "../mocks.hpp"
#include <ymod_webserver_helpers/transfer_encoding/chunked.hpp>
#include <ymod_webserver_helpers/content_type.hpp>
#include <gtest/gtest.h>

namespace {

using namespace testing;
using namespace ymod_webserver::helpers::tests;
using namespace ymod_webserver::helpers;
using namespace ymod_webserver::helpers::transfer_encoding;

struct ChunkedTest : public Test {};

TEST(ChunkedTest, write_should_call_formatted_and_response_methods) {
    const MockedFormatted formatted;
    MockedResponse response;
    const ContentType content_type {"foo", "bar"};
    const auto stream = boost::make_shared<MockedStreamable>();
    const auto encoded = chunked(MockedFormattedWrapper {formatted});

    const InSequence s;

    EXPECT_CALL(formatted, content_type()).WillOnce(ReturnRef(content_type));
    EXPECT_CALL(response, set_connection(false)).WillOnce(Return());
    EXPECT_CALL(response, set_content_type("foo", "bar")).WillOnce(Return());
    EXPECT_CALL(response, result_chunked()).WillOnce(Return(stream));
    EXPECT_CALL(*stream, client_stream()).WillOnce(Invoke([] { return yplatform::net::streamer_wrapper(new Streamer); }));
    EXPECT_CALL(formatted, write(_)).WillOnce(Return());

    encoded.write(response);

    EXPECT_TRUE(Mock::VerifyAndClearExpectations(stream.get()));
}

} // namespace

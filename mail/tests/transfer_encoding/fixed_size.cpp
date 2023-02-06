#include "mocks.hpp"
#include "../mocks.hpp"
#include <ymod_webserver_helpers/transfer_encoding/fixed_size.hpp>
#include <ymod_webserver_helpers/content_type.hpp>
#include <gtest/gtest.h>

namespace {

using namespace testing;
using namespace ymod_webserver::helpers::tests;
using namespace ymod_webserver::helpers;
using namespace ymod_webserver::helpers::transfer_encoding;

struct FixedSizeTest : public Test {};

TEST(FixedSizeTest, write_should_call_formatted_and_response_methods) {
    const MockedFormatted formatted;
    MockedResponse response;
    const ContentType content_type {"foo", "bar"};
    const std::string body("baz");
    const auto encoded = fixed_size(MockedFormattedWrapper {formatted});

    const InSequence s;

    EXPECT_CALL(formatted, content_type()).WillOnce(ReturnRef(content_type));
    EXPECT_CALL(response, set_content_type("foo", "bar")).WillOnce(Return());
    EXPECT_CALL(formatted, apply_for_body(_)).WillOnce(Return());

    encoded.write(response);
}

} // namespace

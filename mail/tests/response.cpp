#include "mocks.hpp"
#include <ymod_webserver_helpers/response.hpp>
#include <gtest/gtest.h>

namespace {

using namespace testing;
using namespace ymod_webserver::helpers::tests;
using namespace ymod_webserver::helpers;

struct ResponseTest : public Test {};

struct MockedTrasferEncoded {
    MOCK_METHOD(void, write, (ymod_webserver::response&), (const));
};

TEST(ResponseTest, ok_should_call_set_result_code_with_ok) {
    MockedResponse response;
    MockedTrasferEncoded encoded;
    Response wrapper(response);

    EXPECT_CALL(response, set_code(ymod_webserver::codes::ok, _)).WillOnce(Return());
    EXPECT_CALL(encoded, write(_)).WillOnce(Return());

    wrapper.ok(encoded);
}

} // namespace

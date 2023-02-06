#include <tests/unit/ymod_webserver_mocks.hpp>

#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <src/server/utils.hpp>

#include <unordered_map>

namespace {

using namespace testing;

using Map = std::unordered_map<std::string, std::string>;
using collie::server::getOrNone;
using collie::server::getRequestId;
using collie::tests::MockStream;

struct TestServerGetOrNone : Test {};

TEST(TestServerGetOrNone, for_existing_key_should_return_value) {
    EXPECT_EQ(getOrNone(Map({{"key", "value"}}), "key"), std::optional<std::string_view>("value"));
}

TEST(TestServerGetOrNone, for_absent_key_should_return_none) {
    EXPECT_EQ(getOrNone(Map(), "key"), std::nullopt);
}

struct TestServerGetXRequestId : Test {
    boost::shared_ptr<StrictMock<MockStream>> stream = boost::make_shared<StrictMock<MockStream>>();
};

TEST_F(TestServerGetXRequestId, for_stream_with_x_request_id_header_should_return_its_value) {
    const auto request = boost::make_shared<ymod_webserver::request>();
    request->headers = ymod_webserver::header_map_t({{"x-request-id", "foo"}});

    EXPECT_CALL(*stream, request()).WillOnce(Return(request));
    EXPECT_EQ(getRequestId(*stream), "foo");
}

TEST_F(TestServerGetXRequestId, for_stream_without_x_request_id_header_should_return_default) {
    const auto request = boost::make_shared<ymod_webserver::request>();

    EXPECT_CALL(*stream, request()).WillOnce(Return(request));
    EXPECT_EQ(getRequestId(*stream), "-");
}

} // namespace

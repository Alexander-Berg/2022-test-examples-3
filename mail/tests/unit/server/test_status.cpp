#include <gtest/gtest.h>

#include <src/server/status.hpp>

#include <yamail/data/serialization/yajl.h>

namespace {

using namespace testing;

using collie::server::Error;
using collie::server::StatusOk;
using collie::server::StatusError;
using yamail::data::serialization::toJson;

struct TestServerStatusOkToJson : Test {};

TEST(TestServerStatusOkToJson, should_return_json) {
    EXPECT_EQ(toJson(StatusOk {}).str(), R"json({"status":"ok"})json");
}

struct TestServerStatusErrorToJson : Test {};

TEST(TestServerStatusErrorToJson, should_return_json) {
    EXPECT_EQ(
        toJson(StatusError {Error::routeError, "route error"}).str(),
        R"json({"status":"error","code":1,"message":"route error"})json"
    );
}

} // namespace

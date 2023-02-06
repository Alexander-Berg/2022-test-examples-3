#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <logdog/format/json.h>

#include <system_error>

namespace {

using namespace ::testing;
namespace log = ::logdog;
namespace json = ::logdog::json;

struct JsonLogTest : public Test {
};

TEST_F(JsonLogTest, BoostErrorCode) {
    boost::system::error_code ec;
    EXPECT_EQ(
        json::formatter(std::make_tuple(log::level=log::notice, log::message="msg", log::error_code=ec)),
        R"json({"level":"notice","message":"msg","error_code":{"message":"Success","value":0,"category":"system"}})json");
}

TEST_F(JsonLogTest, StdErrorCode) {
    std::error_code ec = make_error_code(std::errc::invalid_argument);
    EXPECT_EQ(
        json::formatter(std::make_tuple(log::level=log::notice, log::message="abc", log::error_code=ec)),
        R"json({"level":"notice","message":"abc","error_code":{"message":"Invalid argument","value":22,"category":"generic"}})json");
}

TEST_F(JsonLogTest, MailErrorCode) {
    boost::system::error_code boost_ec;
    mail_errors::error_code ec(boost_ec);
    EXPECT_EQ(
        json::formatter(std::make_tuple(log::level=log::notice, log::message="k", log::error_code=ec)),
        R"json({"level":"notice","message":"k","error_code":{"message":"Success","value":0,"category":"system"}})json");
}


TEST_F(JsonLogTest, Exception) {
    std::runtime_error ex("err");
    EXPECT_EQ(
        json::formatter(std::make_tuple(log::level=log::notice, log::message="k", log::exception=ex)),
        R"json({"level":"notice","message":"k","exception":{"what":"err","type":"std::runtime_error"}})json");
}
}

#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <fstream>

#include <mail/akita/service/include/blackbox/parse.h>
#include <mail/akita/service/include/reflection/sessionid_response.h>
#include <yamail/data/deserialization/json_reader.h>
#include <library/cpp/testing/unittest/env.h>

namespace akita {
namespace tests {

const std::string root = std::string(ArcadiaSourceRoot().c_str()) + "/mail/akita/tests/bb_json/";

std::string json(const std::string& name) {
    std::ifstream in(root + name + ".json");
    return std::string(std::istreambuf_iterator<char>(in), std::istreambuf_iterator<char>());
}

using namespace testing;
using namespace blackbox::sessionid;
using ymod_webserver::codes::code;
using akita::server::Reason;

TEST(BlackboxTest, shouldParseUsualBbResponse) {
    const std::string resp = json("usual");
    const auto data = CheckCookies(boost::get<Response>(process(resp)));

    EXPECT_EQ(data.response.status.id, StatusId::NEED_RESET);
    EXPECT_EQ(data.response.users.size(), 2ul);

    EXPECT_EQ(data.response.users[0].status.id, StatusId::NEED_RESET);
    EXPECT_EQ(data.response.users[1].status.id, StatusId::NEED_RESET);

    EXPECT_EQ(data.uid(), "4007849558");
    EXPECT_EQ(data.bbConnectionId(), "s:1532011146000:PTZv_v_XgdYIBAAAuAYCKg:9");
    EXPECT_EQ(data.childUids(), std::vector<std::string>{"4007849559"});
    EXPECT_EQ(data.timeZone(), "Europe/Moscow");
    EXPECT_EQ(data.offset(), 14400);
}

TEST(BlackboxTest, shouldParseBbResponseWithInvalidCookies) {
    const std::string resp = json("invalid_cookie");
    const boost::variant<Error, Response> data = process(resp);

    const std::string expectedReasonMsg = "key with specified id isn't found. " \
            "Probably cookie is too old or cookie was got in wrong environment (production/testing)";
    EXPECT_EQ(std::make_tuple(Reason::authNoAuth, expectedReasonMsg, code::ok), boost::get<Error>(data));
}

TEST(BlackboxTest, shouldParseEmptyBbResponse) {
    const std::string resp = json("empty");
    const boost::variant<Error, Response> data = process(resp);

    const std::string expectedReasonMsg = "unexpected response from blackbox";
    EXPECT_EQ(std::make_tuple(Reason::unknown, expectedReasonMsg, code::internal_server_error), boost::get<Error>(data));
}

TEST(BlackboxTest, shouldParseBbResponseWithInvalidUserStatus) {
    const std::string resp = json("invalid_user_status");
    const boost::variant<Error, Response> data = process(resp);

    EXPECT_EQ(std::make_tuple(Reason::authNoAuth, toString(Reason::authNoAuth), code::ok), boost::get<Error>(data));
}

TEST(BlackboxTest, shouldParseUnexpectedBbResponse) {
    const std::string resp = json("unexpected");
    const boost::variant<Error, Response> data = process(resp);

    const std::string expectedReasonMsg = "unexpected response from blackbox";
    EXPECT_EQ(std::make_tuple(Reason::unknown, expectedReasonMsg, code::internal_server_error), boost::get<Error>(data));
}

TEST(BlackboxTest, shouldParseBbResponseWithException) {
    const std::string resp = json("with_exception");
    const boost::variant<Error, Response> data = process(resp);

    const std::string expectedReasonMsg = "BlackBox error: Missing userip argument";
    EXPECT_EQ(std::make_tuple(Reason::authInternalProblem, expectedReasonMsg, code::internal_server_error), boost::get<Error>(data));
}

} // namespace tests
} // namespace akita


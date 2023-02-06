#include <mailbox/local/get_attachments_sids_op.h>
#include <catch.hpp>

using namespace xeno::mailbox;
using local::get_attachments_sids_op;

uid_t TEST_UID = 12345;
mid_t TEST_MID = 100;
std::vector<std::string> TEST_HIDS = { "1.3", "1.2", "1.1" };
std::string TEST_HOUND_ERROR_BODY = R"({
            "error":{
                "code":1,
                "message":"unknown error",
                "reason":"unknown error on server-side"
            }
        })";

std::shared_ptr<get_attachments_sids_op> create_get_attachments_sids_op(
    const std::vector<std::string>& hids)
{
    auto ctx = boost::make_shared<xeno::context>();
    auto op = std::make_shared<get_attachments_sids_op>(
        ctx, TEST_UID, "user_ticket", TEST_MID, hids, sids_cb{});
    return op;
}

std::string make_hound_response_body(const std::vector<std::string>& hids)
{
    xeno::json::value res;
    for (auto& hid : hids)
    {
        res[hid] = hid + "_FAKE_SID_SUFFIX";
    }
    res["all"] = "FAKE_SID_FOR_ALL";
    return xeno::json::to_string(res);
}

TEST_CASE("get_attachment_sids_op: parse hound error response")
{
    auto op = create_get_attachments_sids_op(TEST_HIDS);
    REQUIRE_NOTHROW(op->parse_error_response(TEST_HOUND_ERROR_BODY));
}

TEST_CASE("get_attachment_sids_op: parse hound response and return sids in correct order")
{
    auto op = create_get_attachments_sids_op(TEST_HIDS);
    std::vector<std::string> sids;
    REQUIRE_NOTHROW(sids = op->parse_response(make_hound_response_body(TEST_HIDS)));
    REQUIRE(sids.size() == TEST_HIDS.size());
    for (std::size_t i = 0; i < sids.size(); ++i)
    {
        CHECK(sids[i].starts_with(TEST_HIDS[i]));
    }
}

TEST_CASE("get_attachment_sids_op: thows if hid is missed in hound result")
{
    std::vector<std::string> extended_hids = TEST_HIDS;
    extended_hids.emplace_back("one_more_hid");
    auto op = create_get_attachments_sids_op(extended_hids);
    REQUIRE_THROWS(op->parse_response(make_hound_response_body(TEST_HIDS)));
}

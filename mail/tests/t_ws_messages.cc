#include "detail/xivaws_special_messages.h"
#include "web/messages.h"
#include "web/formatters/json.h"
#include <catch.hpp>
#include <string>

using namespace yxiva;
using namespace yxiva::web;
using namespace std::string_literals;

#define CUMULATIVE_CHECK(result, expr)                                                             \
    UNSCOPED_INFO(#expr << " := " << ((expr) ? "true" : "false"));                                 \
    (result) = (result) && (expr);

bool invariants_preserved_for_serialized(string serialized);
string make_user_message(string data);

template <typename Message>
bool invariants_preserved(Message message)
{
    string message_type = detail::type_name<Message>();
    detail::global_checked_types().insert(message_type);
    UNSCOPED_INFO(message_type);
    return invariants_preserved_for_serialized(message.to_string());
}

bool invariants_preserved_for_serialized(string serialized)
{
    json_value json;
    auto error = json.parse(serialized);
    UNSCOPED_INFO(serialized);
    UNSCOPED_INFO(json.stringify());

    bool result = true;
    CUMULATIVE_CHECK(result, !error);
    CUMULATIVE_CHECK(result, json.is_object());
    CUMULATIVE_CHECK(result, json.has_member("operation"));
    CUMULATIVE_CHECK(result, json.has_member("message"));
    CUMULATIVE_CHECK(result, json["message"].is_string());
    CUMULATIVE_CHECK(result, json["operation"].is_string());
    if (json["operation"] == "ping"s)
    {
        CUMULATIVE_CHECK(result, json.has_member("server-interval-sec"));
        CUMULATIVE_CHECK(result, json["server-interval-sec"].is_uint());
    }
    return result;
}

TEST_CASE("ws_messages/special/invariants_preserved")
{
    CHECK(invariants_preserved(ping_message{ seconds(60) }));
    CHECK(invariants_preserved(error_message{ "error-test"s }));
    CHECK(
        invariants_preserved(position_message{ user_id("123455"), service_name("mail"), 0, 100 }));
    CHECK(invariants_preserved(connected_message{ user_id("123455"), service_name("mail") }));
    CHECK(invariants_preserved(disconnected_message{ user_id("123455"), service_name("mail") }));

    REQUIRE(detail::global_all_types() == detail::global_checked_types());
}

TEST_CASE("ws_messages/user/invariants_preserved")
{
    CHECK(invariants_preserved_for_serialized(make_user_message("")));
    CHECK(invariants_preserved_for_serialized(make_user_message("test")));
    CHECK(invariants_preserved_for_serialized(make_user_message("{}")));
    CHECK(invariants_preserved_for_serialized(make_user_message(R"({"key" : "value"}")")));
}

TEST_CASE("ws_messages/any/invariants_broken")
{
    CHECK_FALSE(invariants_preserved_for_serialized(
        R"(
      text
    )"));

    CHECK_FALSE(invariants_preserved_for_serialized(
        R"(
      { "key" "value }
    )"));

    CHECK_FALSE(invariants_preserved_for_serialized(
        R"(
      { "key": "value
    )"));

    CHECK_FALSE(invariants_preserved_for_serialized(
        R"(
      {
      }
    )"));

    CHECK_FALSE(invariants_preserved_for_serialized(
        R"(
      {
        "key" : "value"
      }
    )"));

    CHECK_FALSE(invariants_preserved_for_serialized(
        R"(
      {
        "message" : {}
      }
    )"));

    CHECK_FALSE(invariants_preserved_for_serialized(
        R"(
      {
        "operation" : "abstract"
      }
    )"));

    CHECK_FALSE(invariants_preserved_for_serialized(
        R"(
      {
        "operation" : "abstract",
        "message": {}
      }
    )"));

    CHECK_FALSE(invariants_preserved_for_serialized(
        R"(
      {
        "operation" : "abstract",
        "message": 0
      }
    )"));

    CHECK_FALSE(invariants_preserved_for_serialized(
        R"(
      {
        "operation" : { "name": "abstract"},
        "message": ""
      }
    )"));
}

TEST_CASE("ws_messages/special/ping_invariants_broken")
{
    CHECK_FALSE(invariants_preserved_for_serialized(
        R"(
      text
    )"));

    CHECK_FALSE(invariants_preserved_for_serialized(
        R"(
      { "key" "value }
    )"));

    CHECK_FALSE(invariants_preserved_for_serialized(
        R"(
      { "key": "value
    )"));

    CHECK_FALSE(invariants_preserved_for_serialized(
        R"(
      {
      }
    )"));

    CHECK_FALSE(invariants_preserved_for_serialized(
        R"(
      {
        "key" : "value"
      }
    )"));

    CHECK_FALSE(invariants_preserved_for_serialized(
        R"(
      {
        "operation" : "ping"
      }
    )"));

    CHECK_FALSE(invariants_preserved_for_serialized(
        R"(
      {
        "operation" : "ping",
        "message" : ""
      }
    )"));

    CHECK_FALSE(invariants_preserved_for_serialized(
        R"(
      {
        "operation" : "ping",
        "server-interval-sec" : 5
      }
    )"));

    CHECK_FALSE(invariants_preserved_for_serialized(
        R"(
      {
        "operation" : "ping",
        "server-interval-sec" : 5,
        "message" : {}
      }
    )"));

    CHECK_FALSE(invariants_preserved_for_serialized(
        R"(
      {
        "operation" : "ping",
        "server-interval-sec" : 5,
        "message" : 5
      }
    )"));

    CHECK_FALSE(invariants_preserved_for_serialized(
        R"(
      {
        "operation" : "ping",
        "server-interval-sec" : "",
        "message" : ""
      }
    )"));
}

string make_user_message(string data)
{
    std::stringstream ss;
    formatters::json formatter;
    struct message message;
    message.uid = "123";
    message.service = "fake";
    message.operation = "event";
    message.raw_data = data;
    formatter(user_info(user_id("123")), message, ""s, ss);
    return ss.str();
}
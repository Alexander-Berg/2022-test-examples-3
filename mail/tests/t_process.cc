#include "fixtures.h"
#include <catch.hpp>

TEST_CASE("response_processor/needs_unsubscribe", "")
{
  auto log = fake_log();
  json_value cases, res;
  unsubscribe_task_ptr task;
  REQUIRE(read_test_data("data/test_needs_unsubscribe.json",
    cases, res).error_reason == "");
  REQUIRE(cases.has_member("events"));
  REQUIRE(cases.has_member("subscriptions"));
  auto&& events = cases["events"];
  auto&& subscriptions = cases["subscriptions"];
  REQUIRE(events.size() == res.size());

  for (size_t i = 0; i < events.size(); ++i) {
    auto&& expect = res[i];
    REQUIRE(create_task(events[i], task, nullptr, make_shared<task_context>(),
        nullptr, default_settings(), default_services(), log).error_reason == "");
    REQUIRE(expect.size() == subscriptions.size());
    response_processor process(task);
    auto event_str = "event: " + json_write(events[i]);
    for (size_t j = 0; j < subscriptions.size(); ++j) {
      auto sec_name = event_str + ", subscription: "
        + json_write(subscriptions[j]);
      SECTION(sec_name) {
        CHECK(process.unsubscribe_required(subscriptions[j])
          == expect[j].to_bool());
      }
    }
  }
}

TEST_CASE("response_processor/needs_last_push", "")
{
  auto log = fake_log();
  json_value sub, event;
  event["name"] = "account.invalidate";
  event["uid"] = "123";
  event["timestamp"] = 0;
  unsubscribe_task_ptr task;
  REQUIRE(create_task(event, task, nullptr, make_shared<task_context>(),
    nullptr, default_settings(), default_services(), log).error_reason == "");
  response_processor process(task);
  sub["url"] = "http://some_url";
  REQUIRE(process.last_push_required(sub) == true);
  sub["url"] = callback_uri::webpush_uri("some_webpush_subscription");
  REQUIRE(process.last_push_required(sub) == true);
  sub["url"] = callback_uri::mobile_uri("some_app", "some_token");
  REQUIRE(process.last_push_required(sub) == false);
}

#include "fixtures.h"
#include <catch.hpp>

TEST_CASE("unsubscribe_task/parse_events", "")
{
  auto log = fake_log();
  json_value cases, res;
  unsubscribe_task_ptr task;
  REQUIRE(read_test_data("data/test_events.json", cases, res).error_reason == "");
  REQUIRE(cases.size() == res.size());

  for (size_t i = 0; i < cases.size(); ++i) {
    auto sec_name = json_write(cases[i]);
    SECTION(sec_name) {
      CHECK(create_task(cases[i], task, nullptr, make_shared<task_context>(), nullptr,
        default_settings(), default_services(), log).error_reason == res[i].to_string());
    }
  }
}

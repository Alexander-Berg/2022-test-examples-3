#pragma once

#include "../src/settings.h"
#include "../src/unsubscribe_task.h"
#include "../src/unsubscribe_coro.h"

#include <yxiva/core/message.h>
#include <yxiva/core/packing.hpp>
#include <ymod_httpclient/call.h>
#include <ymod_webserver/server.h>
#include <yplatform/util/sstream.h>
#include <boost/smart_ptr/make_shared_object.hpp>

using namespace yxiva;
using namespace reaper;

inline settings_ptr default_settings()
{
  auto default_settings = std::make_shared<settings>();
  return default_settings;
}

inline std::shared_ptr<std::vector<string>> default_services()
{
  auto default_services = std::make_shared<std::vector<string>>();
  default_services->push_back("mail");
  return default_services;
}

inline operation::result read_test_data(const string& filename,
  json_value_ref& cases, json_value_ref& results)
{
  std::fstream data_file(filename);
  json_value root;
  auto res = json_parse(root, string(
    std::istreambuf_iterator<char>(data_file),
    std::istreambuf_iterator<char>()));
  if (!res) return res;
  if (!root.has_member("cases")) return "cases key is missing";
  if (!root.has_member("results")) return "results key is missing";
  cases = root["cases"];
  results = root["results"];
  return operation::success;
}

inline std::shared_ptr<mod_log> fake_log()
{
  auto log = std::make_shared<mod_log>();
  log->init(yplatform::ptree{});
  return log;
}

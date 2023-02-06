#pragma once

#include <string>
#include <string_view>

#include <userver/server/handlers/http_handler_base.hpp>
#include <userver/storages/redis/client.hpp>
#include <userver/storages/redis/component.hpp>

namespace drive::handlers::api_telematics_cache_api_v1_test_data {

class Processor final : public server::handlers::HttpHandlerBase {
 public:
  static constexpr std::string_view kName = "handler-api-telematics-cache-api-v1-test-data";

  Processor(const components::ComponentConfig& config, const components::ComponentContext& context);

  std::string HandleRequestThrow(const server::http::HttpRequest& request, server::request::RequestContext&) const override;

 private:
  std::string GetValue(std::string_view key, const server::http::HttpRequest& request) const;
  std::string PutValue(std::string_view key, const server::http::HttpRequest& request) const;
  std::string DeleteValue(std::string_view key, const server::http::HttpRequest& request) const;

  storages::redis::ClientPtr redis_client_;
  storages::redis::CommandControl redis_cc_;
};

}  // namespace drive::handlers::api_telematics_cache_api_v1_test_data

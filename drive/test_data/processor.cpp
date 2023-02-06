#include "processor.hpp"

#include <fmt/format.h>

#include <userver/components/component_context.hpp>

#include <string>

namespace drive::handlers::api_telematics_cache_api_v1_test_data {

Processor::Processor(const components::ComponentConfig& config, const components::ComponentContext& context)
    : server::handlers::HttpHandlerBase(config, context)
    , redis_client_{ context.FindComponent<components::Redis>().GetClient("telematics-cache-api") }
{
}

std::string Processor::HandleRequestThrow(const server::http::HttpRequest& request, server::request::RequestContext& /*context*/) const {
  const auto& key = request.GetArg("key");
  if (key.empty()) {
    throw server::handlers::ClientError(
        server::handlers::ExternalBody{"No 'key' query argument"});
  }

  switch (request.GetMethod()) {
    case server::http::HttpMethod::kGet:
      return GetValue(key, request);
    case server::http::HttpMethod::kPut:
      return PutValue(key, request);
    case server::http::HttpMethod::kDelete:
      return DeleteValue(key, request);
    default:
      throw server::handlers::ClientError(server::handlers::ExternalBody{
          fmt::format("Unsupported method {}", request.GetMethod())});
  }
}

std::string Processor::GetValue(std::string_view key, const server::http::HttpRequest& request) const {
  const auto result = redis_client_->Get(std::string{key}, redis_cc_).Get();
  if (!result) {
    throw server::handlers::ResourceNotFound(
        server::handlers::ExternalBody{fmt::format("Not found key {} in Redis", key)});
  }
  request.SetResponseStatus(server::http::HttpStatus::kOk);
  return *result;
}

std::string Processor::PutValue(std::string_view key, const server::http::HttpRequest& request) const {
  const auto& value = request.GetArg("value");
  redis_client_->Set(std::string{key}, value, redis_cc_).Get();
  request.SetResponseStatus(server::http::HttpStatus::kOk);
  return std::string{value};
}

std::string Processor::DeleteValue(std::string_view key, const server::http::HttpRequest& request) const {
  const auto result = redis_client_->Del(std::string{key}, redis_cc_).Get();
  request.SetResponseStatus(server::http::HttpStatus::kOk);
  return std::to_string(result);
}

}  // namespace drive::handlers::api_telematics_cache_api_v1_test_data

#include "utils.hpp"

namespace collie::tests {

ymod_webserver::request_ptr makeRequestWithRawBody(const std::string_view body,
        yplatform::zerocopy::streambuf& buffer) {
    const auto request = boost::make_shared<ymod_webserver::request>();
    std::ostream stream(&buffer);
    stream << body;
    request->raw_body = buffer.detach(buffer.end());
    return request;
}

ymod_webserver::request_ptr makeRequestWithUriParams(ymod_webserver::param_map_t params) {
    auto request{boost::make_shared<ymod_webserver::request>()};
    request->url.params = std::move(params);
    return request;
}

} // namespace collie::tests

#pragma once

#include <ymod_webserver/request.h>
#include <yplatform/zerocopy/streambuf.h>

namespace collie::tests {

ymod_webserver::request_ptr makeRequestWithRawBody(const std::string_view body,
        yplatform::zerocopy::streambuf& buffer);
ymod_webserver::request_ptr makeRequestWithUriParams(ymod_webserver::param_map_t params);

} // namespace

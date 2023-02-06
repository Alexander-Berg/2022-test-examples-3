#pragma once

#include <ymod_webserver/server.h>

namespace apq_tester::server {

using ymod_webserver::handler_ptr;
using ymod_webserver::server;
using ymod_webserver::response_ptr;
using ymod_webserver::request_ptr;
using ymod_webserver::methods::mth_get;
using ymod_webserver::codes::internal_server_error;
using ymod_webserver::codes::ok;
using ymod_webserver::codes::method_not_allowed;

}

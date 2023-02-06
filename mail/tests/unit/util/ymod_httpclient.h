#pragma once

#include <ymod_httpclient/call.h>

#include <yamail/data/reflection/reflection.h>

#include <iostream>

YREFLECTION_ADAPT_ENUM(ymod_httpclient::request::method_t,
    GET,
    HEAD,
    POST,
    PUT,
    DELETE
)

namespace ymod_httpclient {

inline bool operator==(const request& left, const request& right) {
    return (left.method == right.method) && (left.url == right.url) && (left.headers == right.headers) &&
        ((left.body && right.body && *left.body == *right.body) || (!left.body && !right.body));
}

inline bool operator==(const timeouts& left, const timeouts& right) {
    return (left.connect == right.connect) && (left.total == right.total);
}

inline bool operator==(const options& left, const options& right) {
    return (left.log_post_body == right.log_post_body) && (left.log_headers == right.log_headers) &&
        (left.reuse_connection == right.reuse_connection) && (left.timeouts == right.timeouts);
}

inline std::ostream& operator<<(std::ostream& os, const request& req) {
    return os << yamail::data::reflection::to_string(req.method) << " " << req.url << ", headers: " << req.headers;
}

}

#pragma once

#include "common.hpp"

namespace std {
namespace chrono {

inline std::ostream& operator <<(std::ostream& stream, const yplatform::time_traits::duration& value) {
    return stream << "yplatform::time_traits::duration(" << value.count() << ")";
}

} // namespace chrono
} // namespace std

namespace ymod_httpclient {

inline bool operator ==(const request& lhs, const request& rhs) {
    return lhs.url == rhs.url && lhs.headers == rhs.headers &&
            ((lhs.body && rhs.body && *lhs.body == *rhs.body) || (!lhs.body && !rhs.body));
}

inline bool operator ==(const timeouts& lhs, const timeouts& rhs) {
    return lhs.connect == rhs.connect && lhs.total == rhs.total;
}

inline bool operator ==(const options& lhs, const options& rhs) {
    return lhs.log_post_body == rhs.log_post_body
            && lhs.log_headers == rhs.log_headers
            && lhs.reuse_connection == rhs.reuse_connection
            && lhs.timeouts == rhs.timeouts;
}

inline std::ostream& operator <<(std::ostream& stream, const request& value) {
    return stream << "yhttp::request {"
        << '"' << value.url << "\", "
        << '"' << value.headers << "\", "
        << static_cast<const void *>(value.body.get())
        << "}";
}

inline std::ostream& operator <<(std::ostream& stream, const timeouts& value) {
    return stream << "ymod_httpclient::timeouts {" << value.connect << ", " << value.total << "}";
}

inline std::ostream& operator <<(std::ostream& stream, const options& value) {
    return stream << "yhttp::options {"
        << value.log_post_body << ", "
        << value.log_headers << ", "
        << value.reuse_connection << ", "
        << value.timeouts
        << "}";
}

} // namespace ymod_httpclient

namespace {

using namespace testing;

using Response = yhttp::response;
using Request = yhttp::request;

struct HttpClientMock : public yhttp::simple_call {
    using future_void_t = ymod_httpclient::future_void_t;
    using options = yhttp::options;
    using remote_point_info_ptr = ymod_httpclient::remote_point_info_ptr;
    using response_handler_ptr = yhttp::response_handler_ptr;
    using string_ptr = ymod_httpclient::string_ptr;
    using timeouts = ymod_httpclient::timeouts;

    MOCK_METHOD(Response, run, (task_context_ptr, Request), (override));
    MOCK_METHOD(Response, run, (task_context_ptr, Request, const options&), (override));

    MOCK_METHOD(void, async_run, (task_context_ptr ctx, Request req, callback_type), (override));
    MOCK_METHOD(void, async_run, (task_context_ptr ctx, Request req, const options&, callback_type), (override));
};

struct GetHttpClientMock {
    struct Impl {
        MOCK_METHOD(std::shared_ptr<yhttp::simple_call>, call, (), (const));
    };

    std::shared_ptr<const Impl> impl = std::make_shared<const Impl>();

    std::shared_ptr<yhttp::simple_call> operator ()() const {
        return impl->call();
    }
};

struct ClusterClientMock : public ymod_httpclient::cluster_call {
    ClusterClientMock(std::unique_ptr<boost::asio::io_service> io = std::make_unique<boost::asio::io_service>())
        : io(std::move(io)) {
    }

    MOCK_METHOD(void, async_run, (task_context_ptr ctx, Request req, callback_type), (override));
    MOCK_METHOD(void, async_run, (task_context_ptr ctx, Request req, const options&, callback_type), (override));

    std::unique_ptr<boost::asio::io_service> io;
};

struct GetClusterClientMock {
    struct Impl {
        MOCK_METHOD(std::shared_ptr<ymod_httpclient::cluster_call>, call, (), (const));
    };

    std::shared_ptr<const Impl> impl = std::make_shared<const Impl>();

    std::shared_ptr<ymod_httpclient::cluster_call> operator ()() const {
        return impl->call();
    }
};

} // namespace

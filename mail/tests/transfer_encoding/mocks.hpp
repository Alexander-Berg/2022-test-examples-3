#pragma once

#include <ymod_webserver_helpers/content_type.hpp>
#include <yplatform/net/streamable.h>
#include <gmock/gmock.h>

namespace ymod_webserver {
namespace helpers {
namespace tests {

using namespace testing;

struct MockedFormatted {
    MOCK_METHOD(const ContentType&, content_type, (), (const));
    MOCK_METHOD(void, write, (yplatform::net::streamer_wrapper&), (const));
    MOCK_METHOD(void, apply_for_body, (std::function<void (const std::string&)>), (const));
};

struct MockedFormattedWrapper {
    const MockedFormatted& impl;

    const ContentType& content_type() const {
        return impl.content_type();
    }

    template <class Stream>
    void write(Stream& stream) const {
        return impl.write(stream);
    }

    template <class Function>
    void apply_for_body(Function&& function) const {
        return impl.apply_for_body(std::forward<Function>(function));
    }
};

struct Streamer : yplatform::net::streamer_base {};

struct MockedStreamable : public yplatform::net::streamable {
    MOCK_METHOD(void, send_client_stream, (const yplatform::net::buffers::const_chunk_buffer&), (override));
    MOCK_METHOD(void, send_client_stream2, (const yplatform::net::buffers::const_chunk_buffer&, bool), (override));
    MOCK_METHOD(yplatform::net::streamer_wrapper, client_stream, (), (override));
    MOCK_METHOD(bool, is_open, (), (const, override));
};

} // namespace tests
} // namespace helpers
} // namespace ymod_webserver

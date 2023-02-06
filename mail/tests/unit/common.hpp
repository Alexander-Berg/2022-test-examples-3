#pragma once

#include <src/services/hound/hound_client.hpp>
#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wold-style-cast"
#include <macs/mime_part.h>
#include <yamail/data/serialization/json_writer.h>
#include <boost/asio/spawn.hpp>
#pragma clang diagnostic pop
#include <mail/retriever/tests/unit/gtest.h>
#include <mail/retriever/tests/unit/gmock.h>

namespace boost {
namespace asio {

inline std::ostream& operator <<(std::ostream& stream, const yield_context&) {
    return stream;
}

} // namespace asio
} // namespace boost

namespace macs {

inline bool operator ==(const MimePart& lhs, const MimePart& rhs) {
    return lhs.hid() == rhs.hid()
            && lhs.contentType() == rhs.contentType()
            && lhs.contentSubtype() == rhs.contentSubtype()
            && lhs.boundary() == rhs.boundary()
            && lhs.name() == rhs.name()
            && lhs.charset() == rhs.charset()
            && lhs.encoding() == rhs.encoding()
            && lhs.contentDisposition() == rhs.contentDisposition()
            && lhs.fileName() == rhs.fileName()
            && lhs.cid() == rhs.cid()
            && lhs.offsetBegin() == rhs.offsetBegin()
            && lhs.offsetEnd() == rhs.offsetEnd();
}

} // namespace macs

namespace boost {

inline std::ostream& operator <<(std::ostream& stream, const retriever::OptMessageParts& value) {
    return yamail::data::serialization::writeJson(stream, value);
}

} // namespace boost

template <class Handler>
void withIoService(const std::string& requestId, Handler&& handler) {
#if BOOST_VERSION < 106600
    boost::asio::io_service ioService;
#else
    boost::asio::io_context ioService;
#endif
    boost::asio::spawn(ioService, [&] (boost::asio::yield_context yield) {
        handler(boost::make_shared<retriever::TaskContext>("uniqId", requestId, yield));
    }, boost::coroutines::attributes(1024 * 1024));
    ioService.run();
}

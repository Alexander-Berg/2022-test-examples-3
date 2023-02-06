#pragma once

#include <src/network/session.h>
#include <src/network/client_stream.h>
#include <boost/algorithm/string.hpp>

using namespace yimap;
using namespace yimap::server;

class TestSession
    : public Session
    , public boost::enable_shared_from_this<TestSession>
{
public:
    typedef zerocopy::Buffer Buffer;
    typedef zerocopy::BufferRange BufferRange;

    TestSession(ImapContextPtr context) : context(context), fakeReadStream(&fakeReadBuffer)
    {
    }

    void asyncStartTLS(ErrorCodeFunction hook)
    {
        throw std::runtime_error("not implemented");
    }

    bool isOpen() const
    {
        return true;
    }

    void asyncRead(std::size_t atleast, ErrorCodeFunction handler)
    {
        cancelRunningOperations();
        readCallback = handler;
    }

    BufferRange readBuffer()
    {
        return boost::make_iterator_range(fakeReadBuffer.begin(), fakeReadBuffer.end());
    }

    Segment consumeReadBuffer(size_t bytes)
    {
        assert(fakeReadBuffer.size() >= bytes);
        return fakeReadBuffer.detach(fakeReadBuffer.begin() + bytes);
    }

    Segment consumeEntireReadBuffer()
    {
        return fakeReadBuffer.detach(fakeReadBuffer.end());
    }

    ClientStream clientStream()
    {
        return ClientStream(shared_from(this), context);
    }

    void sendClientStream(const yplatform::net::buffers::const_chunk_buffer& s, bool, const string&)
    {
        auto data = string(reinterpret_cast<const char*>(s.data()), s.size());
        for (auto&& chunk : splitDataRows(data))
        {
            boost::trim_right_if(chunk, boost::is_any_of("\r"));
            outgoingData.push_back(chunk);
        }
    }

    void cancelRunningOperations()
    {
        if (readActive())
        {
            auto cb = readCallback;
            readCallback = ErrorCodeFunction{};
            cb(boost::asio::error::operation_aborted);
        }
    }

    void shutdown()
    {
        shutdownCalled = true;
    }

    void setDefaultTimeouts()
    {
        timeouts = "default";
    }

    void disableTimeouts()
    {
        timeouts = "none";
    }

    void setIdleTimeouts()
    {
        timeouts = "idle";
    }

    ImapContextPtr getContext()
    {
        return context;
    }

    const ServerEndpointSettings& endpoint() const
    {
        throw std::runtime_error("not implemented");
    }

    std::vector<string> splitDataRows(const string& src)
    {
        std::vector<string> ret;
        boost::split(ret, src, boost::is_any_of("\n"), boost::token_compress_off);
        if (ret.size() && ret.rbegin()->empty())
        {
            ret.erase(std::next(ret.rbegin()).base());
        }
        return ret;
    }

    bool readActive() const
    {
        return bool(readCallback);
    }

    string dumpOutput() const
    {
        string lines;
        for (auto line : outgoingData)
        {
            lines += line += "\r\n";
        }
        return lines;
    }

    ImapContextPtr context;
    std::vector<string> outgoingData;

    Buffer fakeReadBuffer;
    std::ostream fakeReadStream;
    ErrorCodeFunction readCallback;

    bool shutdownCalled = false;

    string timeouts = "default";
};

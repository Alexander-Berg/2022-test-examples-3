#pragma once

#include <yplatform/net/universal_socket.h>

using namespace yplatform::time_traits;
using namespace boost::asio::error;
using error_code = boost::system::error_code;

struct socket_mock : yplatform::log::contains_logger
{
    typedef boost::asio::io_context::executor_type executor_type;
    typedef socket_mock lowest_layer_type;
    typedef boost::asio::ip::tcp::endpoint endpoint_type;

    socket_mock(boost::asio::io_service& io) : io_(io), wstream_(&wbuf_)
    {
    }

    lowest_layer_type& lowest_layer()
    {
        return *this;
    }

    const lowest_layer_type& lowest_layer() const
    {
        return *this;
    }

    executor_type get_executor()
    {
        return io_.get_executor();
    }

    template <typename ConstBufferSequence>
    std::size_t send(const ConstBufferSequence& buffers)
    {
        std::size_t bytes = 0;
        for (auto buffer : buffers)
        {
            wstream_.clear();
            wstream_.write((char*)(buffer.data()), buffer.size());
            if (!wstream_.good())
            {
                throw std::runtime_error("failed to write");
            }
            bytes += buffer.size();
        }
        YLOG_L(info) << "write " << bytes << " bytes";
        return bytes;
    }

    template <typename ConstBufferSequence>
    std::size_t send(const ConstBufferSequence& buffers, int /*flags*/)
    {
        return send(buffers);
    }

    template <typename ConstBufferSequence>
    std::size_t send(
        const ConstBufferSequence& buffers,
        int /*flags*/,
        boost::system::error_code& ec)
    {
        ec = boost::system::error_code();
        return send(buffers);
    }

    template <typename ConstBufferSequence, typename WriteHandler>
    void async_send(const ConstBufferSequence& buffers, WriteHandler&& handler)
    {
        io_.post([handler, buffers, this]() mutable {
            std::size_t bytes = send(buffers);
            handler(boost::system::error_code(), bytes);
        });
    }

    template <typename ConstBufferSequence, typename WriteHandler>
    void async_send(const ConstBufferSequence& buffers, int /*flags*/, WriteHandler&& handler)
    {
        async_send(buffers, handler);
    }

    template <typename MutableBufferSequence>
    std::size_t receive(MutableBufferSequence buffers)
    {
        if (!rstream_) throw std::runtime_error("fake_stream_socket: no rstream_");
        std::size_t bytes = 0;
        std::size_t requested_bytes = 0;
        for (auto& buffer : buffers)
        {
            if (!buffer.size()) continue;
            requested_bytes += buffer.size();
            rstream_->clear();
            rstream_->read(static_cast<char*>(buffer.data()), buffer.size());
            bytes += rstream_->gcount();
            if (static_cast<std::size_t>(rstream_->gcount()) < buffer.size()) break;
        }
        YLOG_L(info) << "receive " << bytes << " bytes (requested " << requested_bytes << ")";
        return bytes;
    }

    template <typename MutableBufferSequence>
    std::size_t receive(const MutableBufferSequence& buffers, int /*flags*/)
    {
        return receive(buffers);
    }

    template <typename MutableBufferSequence>
    std::size_t receive(
        const MutableBufferSequence& buffers,
        int /*flags*/,
        boost::system::error_code& ec)
    {
        ec = boost::system::error_code();
        return receive(buffers);
    }

    template <typename MutableBufferSequence, typename ReadHandler>
    void async_receive(const MutableBufferSequence& buffers, ReadHandler&& handler)
    {
        io_.post([handler, buffers, this]() mutable {
            std::size_t bytes = receive(buffers);
            if (bytes || !get_requested_bytes_count(buffers))
            {
                handler(boost::system::error_code(), bytes);
            }
            else
            {
                io_.post([handler, buffers, this]() mutable { async_receive(buffers, handler); });
            }
        });
    }

    template <typename MutableBufferSequence, typename ReadHandler>
    void async_receive(const MutableBufferSequence& buffers, int /*flags*/, ReadHandler&& handler)
    {
        async_receive(buffers, handler);
    }

    template <typename ConstBufferSequence>
    std::size_t write_some(const ConstBufferSequence& buffers)
    {
        return send(buffers);
    }

    template <typename ConstBufferSequence>
    std::size_t write_some(const ConstBufferSequence& buffers, boost::system::error_code& ec)
    {
        return send(buffers, ec);
    }

    template <typename ConstBufferSequence, typename WriteHandler>
    void async_write_some(const ConstBufferSequence& buffers, WriteHandler&& handler)
    {
        async_send(buffers, handler);
    }

    template <typename MutableBufferSequence>
    std::size_t read_some(const MutableBufferSequence& buffers)
    {
        return receive(buffers);
    }

    template <typename MutableBufferSequence>
    std::size_t read_some(const MutableBufferSequence& buffers, boost::system::error_code& ec)
    {
        return receive(buffers, ec);
    }

    template <typename MutableBufferSequence, typename ReadHandler>
    void async_read_some(const MutableBufferSequence& buffers, ReadHandler&& handler)
    {
        async_receive(buffers, handler);
    }

    template <typename MutableBufferSequence>
    std::size_t get_requested_bytes_count(MutableBufferSequence buffers)
    {
        std::size_t requested_bytes = 0;
        for (auto& buffer : buffers)
        {
            if (!buffer.size()) continue;
            requested_bytes += buffer.size();
        }
        return requested_bytes;
    }

    bool is_open() const
    {
        return true;
    }

    void cancel()
    {
        YLOG_L(info) << "cancel";
        cancelled = true;
    }

    error_code shutdown(boost::asio::socket_base::shutdown_type /*what*/, error_code& ec)
    {
        YLOG_L(info) << "shutdown";
        closed = true;
        return ec;
    }

    error_code close(error_code& ec)
    {
        YLOG_L(info) << "close";
        closed = true;
        return ec;
    }

    template <typename Handler>
    void async_connect(const endpoint_type& /*ep*/, Handler&& handler)
    {
        using handler_t = typename std::decay<Handler>::type;
        YLOG_L(info) << "connect";
        auto connect_with_delay = [this, handler = handler_t(std::forward<Handler>(handler))]() {
            std::this_thread::sleep_for(microseconds(100));
            if (cancelled)
            {
                io_.post(std::bind(std::move(handler), operation_aborted));
            }
            else if (clock::now() >= connect_at)
            {
                io_.post(std::bind(std::move(handler), error_code()));
            }
            else
            {
                async_connect({}, std::move(handler));
            }
        };
        io_.post(std::move(connect_with_delay));
    }

    boost::asio::io_service& io_;
    bool cancelled = false;
    bool closed = false;
    time_point connect_at = clock::now();
    std::stringbuf wbuf_;
    std::iostream wstream_;
    std::iostream* rstream_ = nullptr;
};

inline void connect_sockets(socket_mock& l, socket_mock& r)
{
    l.rstream_ = &r.wstream_;
    r.rstream_ = &l.wstream_;
}

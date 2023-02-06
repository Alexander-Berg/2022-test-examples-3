#include <net_session.h>
#include <ymod_webserver/context.h>
#include <ymod_webserver/settings.h>

#include <catch.hpp>
#include <boost/asio.hpp>

namespace ymod_webserver {

class stub_handler : public handler
{
    void execute(request_ptr, response_ptr)
    {
    }
};

struct stub_session
    : public net_session
    , public boost::enable_shared_from_this<stub_session>
    , public yplatform::net::streamable
{
public:
    boost::asio::io_service io_;
    ymod_webserver::settings settings_;
    ymod_webserver::endpoint endpoint_;
    context_ptr ctx_;
    ymod_webserver::handler_ptr handler_;
    boost::asio::ip::address remote_addr_;
    boost::asio::ip::address local_addr_;

    struct
    {
        bool opened;
        bool shutdown;
        bool secure;
        boost::system::error_code err;
    } state;

    struct
    {
        std::string read_buffer;
        std::string write_buffer;
    } buffers;

    struct
    {
        read_hook_t read_hook;
        std::size_t min;
        mutable_read_buffer_t buffer;
    } read_op;

    stub_session()
        : io_(), settings_(), ctx_(new context()), handler_(boost::make_shared<stub_handler>())
    {
        remote_addr_ = boost::asio::ip::address::from_string("127.0.0.1");
        local_addr_ = boost::asio::ip::address::from_string("127.0.0.1");
        state.opened = true;
        state.shutdown = false;
        state.secure = false;
    }

    context_ptr ctx()
    {
        return ctx_;
    }

    const ymod_webserver::settings& session_settings() const
    {
        return settings_;
    }

    bool is_open() const
    {
        return state.opened;
    }

    bool is_secure() const
    {
        return state.secure;
    }

    void begin_read(read_hook_t const& hook, mutable_read_buffer_t buffer, std::size_t min)
    {
        read_op.read_hook = hook;
        read_op.min = min;
        read_op.buffer = buffer;
    }

    bool read_is_active()
    {
        return read_op.read_hook;
    }

    std::size_t read_available_bytes()
    {
        return std::min(buffers.read_buffer.size(), boost::asio::buffer_size(read_op.buffer));
    }

    void complete_read(boost::system::error_code err = boost::system::error_code())
    {
        REQUIRE(read_is_active());

        read_hook_t hook_copy = read_op.read_hook;
        read_op.read_hook = read_hook_t();

        if (state.err)
        {
            err = state.err;
        }
        else
        {
            if (state.opened == false)
            {
                err = boost::asio::error::connection_aborted;
            }
        }

        std::size_t bytes_transfered = 0;
        if (!err)
        {
            std::size_t bytes_available = read_available_bytes();

            bytes_transfered = boost::asio::buffer_copy(
                read_op.buffer, boost::asio::buffer(buffers.read_buffer.data(), bytes_available));
            REQUIRE(bytes_transfered == bytes_available);
            buffers.read_buffer.erase(0, bytes_transfered);
        }

        hook_copy(err, bytes_transfered);
        io_.poll();
    }

    void async_close(close_hook_t const& hook)
    {
        state.shutdown = true;
        state.opened = false;
        state.err = boost::asio::error::connection_aborted;
        if (read_is_active())
        {
            complete_read();
        }
        hook(boost::system::error_code());
        io_.poll();
    }

    void do_shutdown(const shutdown_hook_t& hook, bool graceful)
    {
        state.shutdown = true;
        if (graceful)
        {
            state.err = boost::asio::error::operation_aborted;
        }
        else
        {
            state.opened = false;
            state.err = boost::asio::error::eof;
        }
        if (read_is_active())
        {
            complete_read();
        }
        hook();
        io_.poll();
    }

    void cancel_operations(const cancel_hook_t& hook)
    {
        state.shutdown = true;
        if (read_is_active())
        {
            complete_read(boost::asio::error::operation_aborted);
        }
        hook();
        io_.poll();
    }

    boost::asio::io_service& current_io_service()
    {
        return io_;
    }

    const ymod_webserver::endpoint& endpoint() const
    {
        return endpoint_;
    }

    const ymod_webserver::handler_ptr& handler() const
    {
        return handler_;
    }

    void send_client_stream(yplatform::net::buffers::const_chunk_buffer const& buf)
    {
        buffers.write_buffer.append(reinterpret_cast<const char*>(buf.data()), buf.size());
    }

    yplatform::net::streamer_wrapper client_stream()
    {
        boost::shared_ptr<stub_session> s = this->shared_from_this();
        yplatform::net::streamer_wrapper wrp(new yplatform::net::streamer<stub_session>(s));
        return wrp;
    }

    unsigned requests_count() const
    {
        return 1;
    }

    void increment_requests_count()
    {
    }

    const boost::asio::ip::address& remote_addr() const
    {
        return remote_addr_;
    }
    unsigned short remote_port() const
    {
        return 2222;
    }
    const boost::asio::ip::address& local_addr() const
    {
        return local_addr_;
    }
    unsigned short local_port() const
    {
        return 1111;
    }

    void set_write_error_hook(write_error_hook_t const&)
    {
    }

    const session_stats& stats()
    {
        static session_stats fake_stats;
        return fake_stats;
    }
};

}

#pragma once

#include "../mod_apns/push_line.h"
#include "init_log.h"
#include <yplatform/time_traits.h>

namespace yxiva { namespace mobile { namespace apns {

#define READ_TIMEOUT 44
#define READ_TIMEOUT_NS READ_TIMEOUT * 1e6
#define WRITE_TIMEOUT 55

struct test_timer;
struct test_session;

struct test_io_service
{
    test_timer* read_timer = 0;
    test_session* session = 0;
};

struct test_timer
{
    typedef boost::function<void(const boost::system::error_code&)> callback_t;
    typedef yplatform::time_traits::time_point time_point;
    typedef yplatform::time_traits::duration duration_type;
    typedef yplatform::time_traits::clock clock_type;

    test_timer(test_io_service& io) : io(io)
    {
    }

    ~test_timer()
    {
        io.read_timer = 0;
    }

    void expires_from_now(duration_type t)
    {
        time = clock_type::now() + t;
        if (t.count() == READ_TIMEOUT_NS) io.read_timer = this;
    }

    duration_type expires_from_now()
    {
        return time - clock_type::now();
    }

    void expires_at(time_point t)
    {
        time = t;
    }

    time_point expires_at()
    {
        return time;
    }

    template <typename Handler>
    void async_wait(Handler h)
    {
        if (cb) cb(boost::asio::error::operation_aborted);
        cb = h;
    }

    void cancel()
    {
        auto copy = cb;
        cb.clear();
        if (copy) copy(boost::asio::error::operation_aborted);
    }

    void complete_wait()
    {
        auto copy = cb;
        cb.clear();
        copy(boost::system::error_code());
    }

    bool active()
    {
        return !!cb;
    }

private:
    test_io_service& io;
    callback_t cb;
    time_point time;
};

struct test_strand
{
    template <typename Handler>
    Handler wrap(Handler h)
    {
        return h;
    }
};

//------------------------------------------------------------------------------

struct test_net_client;
struct test_session
{
    typedef boost::system::error_code error_code;
    typedef boost::function<void(void)> cancel_callback_t;
    typedef boost::function<void(const error_code&)> close_callback_t;
    typedef boost::function<
        void(const error_code&, const size_t, const boost::asio::mutable_buffers_1&)>
        write_callback_t;
    typedef boost::function<void(const error_code&, const size_t)> read_callback_t;

    yplatform::net::client_settings settings;
    bool connected = false;

    bool read_active()
    {
        return !!read_cb;
    }
    bool write_active()
    {
        return !!write_cb;
    }
    bool close_active()
    {
        return !!close_cb;
    }
    bool cancel_active()
    {
        return !!cancel_cb;
    }

    test_session(const yplatform::net::client_settings& settings) : settings(settings)
    {
    }

    template <typename Host, typename Port, typename Handler, typename DeadlineHandler>
    void connect(Host /*host*/, Port /*port*/, Handler h, DeadlineHandler /*fh*/)
    {
        connected = true;
        h(error_code());
    }

    template <typename Handler, typename DeadlineHandler>
    void start_tls(Handler h, DeadlineHandler /*fh*/)
    {
        h(error_code());
    }

    template <typename Buffer, typename Handler>
    void async_write(Buffer /*buffer*/, Handler h)
    {
        assert(!write_cb);
        write_cb = h;
    }

    void complete_write()
    {
        auto copy = write_cb;
        write_cb.clear();
        copy(error_code(), 100, boost::asio::mutable_buffers_1(0, 0));
    }

    template <typename Buffer, typename Handler>
    void async_read(Handler h, Buffer buffer)
    {
        assert(boost::asio::buffer_size(buffer) >= 6);
        assert(!read_cb);
        read_cb = h;
        read_buffer = boost::asio::buffer_cast<uint8_t*>(buffer);
    }

    void complete_read(apns_error value)
    {
        assert(read_buffer);
        encode_error(value, read_buffer);
        auto copy = read_cb;
        read_cb.clear();
        copy(error_code(), 6);
    }

    void complete_read(const boost::system::error_code& ec)
    {
        auto copy = read_cb;
        read_cb.clear();
        copy(ec, 0);
    }

    template <typename Handler>
    void cancel_operations(Handler h)
    {
        assert(!cancel_cb);
        cancel_cb = h;
    }

    void complete_cancel_operations()
    {
        if (read_cb)
        {
            read_cb(boost::asio::error::operation_aborted, 0);
            read_cb.clear();
        }
        if (write_cb)
        {
            write_cb(
                boost::asio::error::operation_aborted, 0, boost::asio::mutable_buffers_1(0, 0));
            write_cb.clear();
        }
        if (cancel_cb)
        {
            cancel_cb();
            cancel_cb.clear();
        }
    }

    template <typename Handler>
    void async_close(Handler h)
    {
        assert(!close_cb);
        close_cb = h;
    }

    void complete_close()
    {
        auto copy = close_cb;
        close_cb.clear();
        copy(error_code());
    }

    test_strand strand()
    {
        return test_strand();
    }

private:
    write_callback_t write_cb;
    read_callback_t read_cb;
    cancel_callback_t cancel_cb;
    close_callback_t close_cb;
    uint8_t* read_buffer = 0;
};

struct test_net_client
{
    test_io_service* get_io()
    {
        return &io;
    }

    boost::shared_ptr<test_session> create_session(const yplatform::net::client_settings& st)
    {
        auto psession = boost::make_shared<test_session>(st);
        io.session = &*psession;
        return psession;
    }

private:
    test_io_service io;
};

}}}
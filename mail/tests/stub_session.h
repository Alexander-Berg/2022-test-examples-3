#pragma once

#include <catch.hpp>

#include <boost/asio.hpp>
#include "../src/pool_manager.h"
#include "../src/session/messenger_session.h"

using namespace ymod_messenger;

struct stub_session
    : public messenger_session
    , public boost::enable_shared_from_this<stub_session>
{
    typedef boost::function<void(const error_code&)> connect_hook_t;
    typedef boost::function<void(const error_code&, const boost::asio::ip::address&)>
        resolve_hook_t;
    typedef boost::function<void()> timer_hook_t;

    stub_session(const host_info& address = host_info("127.0.0.1", 8080))
        : opened(true), addr(address)
    {
    }

    void start_read() override
    {
    }

    void send(segment_t seg) override
    {
        send_queue.push_back(seg);
    }

    void async_close() override
    {
        opened = false;
        if (error_hook_) error_hook_(this->shared_from_this(), error_code());
    }

    size_t send_queue_size() override
    {
        return send_queue.size();
    }

    bool is_open() const override
    {
        return opened;
    }

    session_stats_ptr get_stats() override
    {
        return stats_;
    }

    string get_description() const override
    {
        return "stub " + addr.to_string();
    }

    bool opened;
    host_info addr;
    std::vector<segment_t> send_queue;
    session_stats_ptr stats_{ new session_stats() };
};

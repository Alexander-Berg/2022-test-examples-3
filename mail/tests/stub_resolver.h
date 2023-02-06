#pragma once

#include <catch.hpp>

#include "defines.h"

using namespace ymod_messenger;

class stub_resolver
{
    using records = std::vector<std::string>;

public:
    using iterator_aaaa = records::iterator;
    using iterator_a = records::iterator;
    using iterator_ptr = records::iterator;

    stub_resolver(
        boost::asio::io_service& io,
        time_traits::duration delay_duration = time_traits::seconds(0),
        bool emulate_delay = false)
        : io_(io), delay_duration_(delay_duration), emulate_delay_(emulate_delay)
    {
    }

    void cancel()
    {
    }

    template <typename Handler>
    void async_resolve_aaaa(const std::string& q, Handler h)
    {
        if (emulate_delay_)
        {
            delay([this, q, h]() { async_resolve_aaaa(q, h); });
            return;
        }
        if (q == T_HOST.addr || q == T_HOST_RESOLVED.addr || q == T_HOST_6_ONLY.addr)
        {
            io_.post(std::bind(h, boost::system::error_code(), aaaa_.begin()));
        }
        else
        {
            io_.post(std::bind(
                h,
                boost::system::errc::make_error_code(boost::system::errc::bad_address),
                iterator_aaaa()));
        }
    }

    template <typename Handler>
    void async_resolve_a(const std::string& q, Handler h)
    {
        if (emulate_delay_)
        {
            delay([this, q, h]() { async_resolve_a(q, h); });
            return;
        }
        if (q == T_HOST.addr || q == T_HOST_RESOLVED.addr || q == T_HOST_4_ONLY.addr)
        {
            io_.post(std::bind(h, boost::system::error_code(), a_.begin()));
        }
        else
        {
            io_.post(std::bind(
                h,
                boost::system::errc::make_error_code(boost::system::errc::bad_address),
                iterator_a()));
        }
    }

    template <typename Handler>
    void async_resolve_ptr(const std::string& q, Handler h)
    {
        if (emulate_delay_)
        {
            delay([this, q, h]() { async_resolve_ptr(q, h); });
            return;
        }
        if (q == T_HOST_IP || q == T_HOST_IP_6)
        {
            io_.post(std::bind(h, error_code(), ptr_.begin()));
        }
        else
        {
            io_.post(std::bind(
                h,
                boost::system::errc::make_error_code(boost::system::errc::bad_address),
                iterator_ptr()));
        }
    }

private:
    template <typename Func>
    void delay(Func&& f)
    {
        emulate_delay_ = false;
        auto timer = std::make_shared<time_traits::timer>(io_);
        timer->expires_from_now(delay_duration_);
        timer->async_wait([f = std::move(f), timer](const boost::system::error_code&) { f(); });
    }

    std::vector<std::string> aaaa_{ T_HOST_IP_6 };
    std::vector<std::string> a_{ T_HOST_IP };
    std::vector<std::string> ptr_{ T_HOST_RESOLVED.addr };
    boost::asio::io_service& io_;
    time_traits::duration delay_duration_;
    bool emulate_delay_;
};

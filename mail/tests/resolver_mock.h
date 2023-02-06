#pragma once

#include <yplatform/net/dns/resolver.h>

using namespace yplatform::time_traits;
using namespace boost::asio::error;
using error_code = boost::system::error_code;
using string = std::string;

struct resolver_mock
{
    struct iterator
    {
        iterator() = default;
        iterator(const string& str) : p_(&str)
        {
        }

        iterator operator++(int)
        {
            p_ = nullptr;
            return *this;
        }

        bool operator==(const iterator& other) const
        {
            return p_ == other.p_;
        }

        const string& operator*() const
        {
            return *p_;
        }

    private:
        const string* p_ = nullptr;
    };
    typedef iterator iterator_a;
    typedef iterator iterator_aaaa;

    resolver_mock(boost::asio::io_service& io) : io_(io)
    {
    }

    template <typename Handler>
    void async_resolve_a(const string& host, Handler&& handler)
    {
        address = host.find("ipv4") != string::npos ? "87.250.250.242" : "";
        prepare_mock();
        async_cancelable_resolve_with_delay(std::forward<Handler>(handler));
    }

    template <typename Handler>
    void async_resolve_aaaa(const string& host, Handler&& handler)
    {
        address = host.find("ipv6") != string::npos ? "2a02:6b8::2:242" : "";
        prepare_mock();
        async_cancelable_resolve_with_delay(std::forward<Handler>(handler));
    }

    static duration resolve_step_duration;

private:
    template <typename Handler>
    void async_cancelable_resolve_with_delay(Handler&& handler)
    {
        using handler_t = typename std::decay<Handler>::type;
        auto delay = [this, handler = handler_t(std::forward<Handler>(handler))]() mutable {
            std::this_thread::sleep_for(microseconds(100));
            if (cancelled)
            {
                address = "";
                io_.post(std::bind(std::move(handler), operation_aborted, iterator(address)));
            }
            else if (clock::now() >= resolve_at)
            {
                auto err = address.empty() ? host_not_found : error_code();
                io_.post(std::bind(std::move(handler), err, iterator(address)));
            }
            else
            {
                async_cancelable_resolve_with_delay(std::move(handler));
            }
        };
        io_.post(std::move(delay));
    }

    void prepare_mock()
    {
        resolve_at = clock::now() + resolve_step_duration;
        cancelled = false;
    }

    boost::asio::io_service& io_;
    string address;
    time_point resolve_at;
    bool cancelled;
};

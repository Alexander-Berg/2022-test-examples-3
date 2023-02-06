#include <catch.hpp>

#include <list>
#include <boost/asio.hpp>
#include "../src/pool_manager.h"
#include "../src/session/messenger_session.h"
#include "../src/session_factory.h"
#include "stub_session.h"
#include "stub_session_factory.h"
#include "stub_resolver.h"
#include "defines.h"

namespace ymod_messenger {

struct stub_pool;

int send_calls;
int incoming_connections;
int pool_created;
int incoming_pool_created;

class stub_messages_notifier : public messages_notifier
{
    typedef message_hook_t hook_t;

public:
    hook_id_t add_hook(const hook_t&, const message_type) override
    {
        return 1;
    }
    void notify(const address_t&, message_type, const shared_buffers&) override
    {
    }
};

class stub_events_notifier : public events_notifier
{
    typedef event_notification notification_t;
    typedef event_hook_t hook_t;

public:
    hook_id_t add_hook(const hook_t&) override
    {
        return 1;
    }
    void notify(const address_t&, const notification_t&) override
    {
    }
};

struct stub_pool : public yplatform::log::contains_logger
{
    typedef boost::function<void(const host_info& address)> void_hook_t;
    typedef boost::function<
        void(const host_info& address, message_type type, const shared_buffers& seq)>
        message_hook_t;

    stub_pool(const host_info&, session_factory_ptr, const pool_settings&, stats_ptr /**/)
    {
        pool_created++;
        opened_ = false;
        detached_ = false;
        empty = true;
    }

    bool is_empty()
    {
        return empty;
    }

    size_t real_size()
    {
        return empty ? 0 : 1;
    }

    void open()
    {
        opened_ = true;
    }

    void close()
    {
        opened_ = false;
    }

    void detach()
    {
        detached_ = true;
    }

    void send(segment_t)
    {
        send_calls++;
    }

    template <typename T1, typename T2>
    void set_hooks(const T1&, const T1&, const T2&)
    {
    }

    bool opened_;
    bool detached_;
    bool empty;
};

struct stub_incoming_pool : public yplatform::log::contains_logger
{
    typedef boost::function<void(const host_info& address)> void_hook_t;
    typedef boost::function<void(const host_info& address, message_type, const shared_buffers&)>
        message_hook_t;

    stub_incoming_pool(const host_info&, stats_ptr /**/)
    {
        incoming_pool_created++;
        opened_ = false;
        detached_ = false;
        empty = true;
    }

    bool is_empty()
    {
        return empty;
    }

    size_t real_size()
    {
        return empty ? 0 : 1;
    }

    void open()
    {
        opened_ = true;
    }

    void close()
    {
        opened_ = false;
    }

    void detach()
    {
        detached_ = true;
    }

    void send(segment_t)
    {
        send_calls++;
    }

    void add_incoming_session(messenger_session_ptr)
    {
        incoming_connections++;
    }

    template <typename T1, typename T2>
    void set_hooks(const T1&, const T1&, const T2&)
    {
    }

    bool opened_;
    bool detached_;
    bool empty;
};

struct T_POOL_MAN
{
    using resolve_order_t = yplatform::net::client_settings::resolve_order_t;

    T_POOL_MAN()
        : factory(new stub_session_factory())
        , messages_notifier_(new stub_messages_notifier())
        , events_notifier_(new stub_events_notifier())
        , pool_man(new pool_manager<stub_pool, stub_incoming_pool, stub_resolver, resolve_order_t>(
              std::make_shared<pool_resolver<stub_resolver, resolve_order_t>>(
                  io_,
                  stub_resolver(io_),
                  stub_resolver(io_),
                  resolve_order_t::ipv6_ipv4,
                  time_traits::seconds(5)),
              factory,
              messages_notifier_,
              events_notifier_,
              pool_settings(),
              boost::make_shared<stats>()))
    {
        //        boost::log::init_log_to_console();
        send_calls = 0;
        incoming_connections = 0;
        pool_created = 0;
    }

    ~T_POOL_MAN()
    {
        pool_man->close();
    }

    shared_ptr<stub_session_factory> factory;
    shared_ptr<stub_messages_notifier> messages_notifier_;
    shared_ptr<stub_events_notifier> events_notifier_;
    shared_ptr<pool_manager<stub_pool, stub_incoming_pool, stub_resolver, resolve_order_t>>
        pool_man;
    boost::asio::io_service io_;
};

TEST_CASE_METHOD(T_POOL_MAN, "pool_man/open/normal", "")
{
    pool_man->open_pool(T_HOST);
    io_.run();
    REQUIRE(pool_created == 1);
}

TEST_CASE_METHOD(T_POOL_MAN, "pool_man/open/bad-host", "")
{
    pool_man->open_pool(T_BAD_HOST);
    io_.run();
    REQUIRE(pool_created == 1);
}

TEST_CASE_METHOD(T_POOL_MAN, "pool_man/open/incoming", "")
{
    pool_man->add_incoming_session(
        messenger_session_ptr(new stub_session()), host_info(T_HOST_IP, T_HOST.port));
    io_.run();
    REQUIRE(pool_created == 0);
    REQUIRE(incoming_pool_created == 1);
}

}

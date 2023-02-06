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
#include "../src/outgoing_pool.h"

namespace ymod_messenger {

struct T_POOL
{

    T_POOL() : factory(new manual_stub_session_factory()), open_cb_calls(0), close_cb_calls(0)
    {
        st.min_size = 4;
        st.max_size = 6;
        st.reconnect_delay = time_traits::duration::min();
        pool.reset(
            new ymod_messenger::outgoing_pool(T_HOST, factory, st, boost::make_shared<stats>()));

        pool->set_hooks(
            boost::bind(&T_POOL::on_open, this, _1),
            boost::bind(&T_POOL::on_close, this, _1),
            ymod_messenger::outgoing_pool::message_hook_t());
    }

    ~T_POOL()
    {
        pool->close();
    }

    void on_open(const host_info&)
    {

        open_cb_calls++;
    }

    void on_close(const host_info&)
    {

        close_cb_calls++;
    }

    void create_many_connections(const size_t count)
    {
        size_t current_requests = 0;

        while (current_requests < count)
        {

            REQUIRE(factory->requests.size() == current_requests + 1);

            messenger_session_ptr s = factory->make_session(T_HOST);
            sessions.push_back(s);

            factory->requests[current_requests].connect_hook(s, error_code());

            current_requests++;

            REQUIRE(pool->real_size() == current_requests);
        }
    }

    pool_settings st;
    shared_ptr<manual_stub_session_factory> factory;
    shared_ptr<ymod_messenger::outgoing_pool> pool;

    size_t open_cb_calls;
    size_t close_cb_calls;

    std::vector<messenger_session_ptr> sessions;
};

TEST_CASE_METHOD(T_POOL, "pool/open/basic-test", "")
{

    pool->open();
    REQUIRE(open_cb_calls == 0);
    REQUIRE(factory->requests.size() == 1);
    REQUIRE(pool->real_size() == 0);
    REQUIRE(pool->size() == 1);

    messenger_session_ptr s = factory->make_session(T_HOST);
    sessions.push_back(s);
    factory->requests.back().connect_hook(s, error_code());

    REQUIRE(open_cb_calls == 1);
    REQUIRE(factory->requests.size() == 2);
    REQUIRE(s->is_open());
    REQUIRE(pool->real_size() == 1);
    REQUIRE(pool->size() == 2);
}

TEST_CASE_METHOD(T_POOL, "pool/open/min", "")
{

    pool->open();

    create_many_connections(st.min_size);

    REQUIRE(pool->size() == st.min_size);
    REQUIRE(pool->size() == pool->real_size());
}

TEST_CASE_METHOD(T_POOL, "pool/open/timeout", "")
{

    pool->open();

    create_many_connections(st.min_size - 1);

    REQUIRE(factory->requests.size() == st.min_size);
    factory->requests.back().timeout_hook();

    REQUIRE(factory->requests.size() == st.min_size + 1);
}

TEST_CASE_METHOD(T_POOL, "pool/session-closed/one", "")
{
    pool->open();

    create_many_connections(st.min_size);
    REQUIRE(pool->size() == st.min_size);
    REQUIRE(factory->requests.size() == st.min_size);
    REQUIRE(sessions.size() == st.min_size);

    REQUIRE(sessions[st.min_size / 2].use_count() == 2);
    sessions[st.min_size / 2]->async_close();
    REQUIRE(sessions[st.min_size / 2].use_count() == 1);

    REQUIRE(factory->requests.size() == st.min_size + 1);
    REQUIRE(pool->size() == st.min_size);
    REQUIRE(pool->size() == pool->real_size() + 1);
    REQUIRE(close_cb_calls == 0);
}

TEST_CASE_METHOD(T_POOL, "pool/session-closed/all", "")
{
    pool->open();

    create_many_connections(st.min_size);

    REQUIRE(sessions.size() == st.min_size);

    for (size_t i = 0; i < st.min_size; i++)
    {
        sessions[i]->async_close();
    }

    REQUIRE(close_cb_calls == 1);

    REQUIRE(!pool->is_empty());
    REQUIRE(pool->real_size() == 0);
    REQUIRE(pool->size() == st.min_size);
}

TEST_CASE_METHOD(T_POOL, "pool/close", "")
{
    pool->open();

    create_many_connections(st.min_size);

    REQUIRE(sessions.size() == st.min_size);

    pool->close();

    for (size_t i = 0; i < st.min_size; i++)
    {
        REQUIRE(!sessions[i]->is_open());
        REQUIRE(sessions[i].use_count() == 1);
    }

    REQUIRE(close_cb_calls == 1);
    REQUIRE(pool->is_empty());
}

TEST_CASE_METHOD(T_POOL, "pool/detach", "")
{
    pool->open();

    create_many_connections(st.min_size);

    REQUIRE(sessions.size() == st.min_size);

    pool->detach();

    for (size_t i = 0; i < st.min_size; i++)
    {
        REQUIRE(!sessions[i]->is_open());
        REQUIRE(sessions[i].use_count() == 1);
    }

    REQUIRE(close_cb_calls == 0);
    REQUIRE(pool->is_empty());
}

TEST_CASE_METHOD(T_POOL, "pool/balance/send", "")
{
    pool->open();

    create_many_connections(st.min_size);
    REQUIRE(factory->requests.size() == st.min_size);
    REQUIRE(sessions.size() == st.min_size);

    for (size_t i = 0; i < st.min_size; i++)
    {
        REQUIRE(sessions[i]->send_queue_size() == 0);
        pool->send(detail::create_segment(message_type{ 0 }, buffer({ 'a', 'b', 'c' })));
        REQUIRE(sessions[i]->send_queue_size() == 1);
        REQUIRE(factory->requests.size() == st.min_size);
    }

    pool->send(
        detail::create_segment(message_type{ 0 }, buffer(std::vector<char>{ 'a', 'b', 'c' })));
    REQUIRE(factory->requests.size() == st.min_size + 1);
}

}

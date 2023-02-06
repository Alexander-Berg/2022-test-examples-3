#pragma once

#include "../src/master_impl.h"
#include "../src/slave_impl.h"

#include "rands.hpp"
#include <catch.hpp>
#include <boost/make_shared.hpp>

using namespace multipaxos;

inline value_t make_value(std::string const& s)
{
    return buffer_t::create_from(s);
}

inline learn_message make_answer(accept_message const& msg)
{
    learn_message answer;
    answer.ballot = msg.ballot;
    answer.accepted_value = msg.pvalue;
    return answer;
}

inline promise_message make_answer(prepare_message const& msg)
{
    promise_message answer;
    answer.requested_ballot = msg.ballot;
    answer.acceptor_ballot = msg.ballot;
    answer.requested_slot = msg.slot;
    answer.max_slot = -1;
    return answer;
}

template <typename T>
inline bool value_in(T value, std::vector<T> collection)
{
    return std::find(collection.begin(), collection.end(), value) != collection.end();
}

inline void log_func(const string& s)
{
    std::cout << "**** " << s << "\n";
}

template <typename Message>
struct send_hook
{
    void send(Message const& msg)
    {
        msgs.push_back(msg);
    }

    std::vector<Message> msgs;
};

typedef send_hook<prepare_message> send_prepare_message_test;
typedef send_hook<accept_message> send_accept_message_test;
typedef send_hook<sync_request_message> send_sync_message_test;
typedef send_hook<master_announce_message> send_announce_message_test;

struct dropped_values_test
{
    void drop_func(value_t val)
    {
        //        std::cout << "## drop : " << (val ? string(val->begin(), val->end()) : "?") <<
        //        "\n";
        dropped_values.push_back(val);
    }
    std::vector<value_t> dropped_values;
};

struct T_slave
{
    T_slave()
    {
        frame.init(timers::queue_ptr(), 0, 1000);
        settings.send_sync_message = boost::bind(&send_sync_message_test::send, &sync_hook, _1);
        settings.drop_func = boost::bind(&dropped_values_test::drop_func, &drop_hook, _1);
        //        settings.log_func = log_func;

        impl = boost::make_shared<slave_impl>(frame, settings, timers::queue_ptr());
    }

    void frame_init(slot_n slot = 0, slot_n slots_count = 1000)
    {
        frame.init(timers::queue_ptr(), slot, slots_count);
    }

    ~T_slave()
    {
    }

    frame_t frame;
    stats_t stats;
    send_sync_message_test sync_hook;
    dropped_values_test drop_hook;
    shared_ptr<slave_impl> impl;
    settings_t settings;
};

struct T_master
{
    T_master()
    {
        frame.init(timers::queue_ptr(), 0, 1000);
        settings.send_accept_message = boost::bind(&send_accept_message_test::send, &send_hook, _1);
        settings.send_prepare_message =
            boost::bind(&send_prepare_message_test::send, &prepare_hook, _1);
        settings.send_announce_message =
            boost::bind(&send_announce_message_test::send, &announce_hook, _1);
        settings.drop_func = boost::bind(&dropped_values_test::drop_func, &drop_hook, _1);
        //        settings.log_func = log_func;
        impl = boost::make_shared<master_impl>(frame, 0, settings, stats, timers::queue_ptr());
    }

    ~T_master()
    {
    }

    void frame_init(slot_n slot = 0, slot_n slots_count = 1000)
    {
        frame.init(timers::queue_ptr(), slot, slots_count);
    }

    void fake_prepare(ballot_t ballot = 1, slot_n slot = -1)
    {
        if (slot != -1) frame.init(timers::queue_ptr(), slot, 1000);
        impl->ballot = ballot;
        impl->activate();
        promise_message msg = make_answer(prepare_hook.msgs[0]);
        impl->promise_received(msg, acceptor_id_t("1"));
        impl->promise_received(msg, acceptor_id_t("2"));
        if (!impl->is_prepared()) throw std::runtime_error("fake prepare failed");
        prepare_hook.msgs.clear();
    }

    void fake_next_round()
    {
        prepare_hook.msgs.clear();
        impl->timeout(frame.write_zone_begin());
        if (!prepare_hook.msgs.size())
            throw std::runtime_error("fake_next_round failed on prepare reset");
        promise_message msg = make_answer(prepare_hook.msgs[0]);
        impl->promise_received(msg, acceptor_id_t("1"));
        impl->promise_received(msg, acceptor_id_t("2"));
        if (!impl->is_prepared()) throw std::runtime_error("fake_next_round failed");
        prepare_hook.msgs.clear();
    }

    void fast_submit(value_t test_value)
    {
        if (!impl->is_prepared()) throw std::runtime_error("fast submit failed: not prepared");
        impl->submit(test_value);
        if (!send_hook.msgs.size()) throw std::runtime_error("fast submit failed: no message");
        if (send_hook.msgs.back().pvalue.value != test_value)
            throw std::runtime_error("fast submit failed: wrong value");
        impl->learn_received(make_answer(send_hook.msgs.back()), "1");
        impl->learn_received(make_answer(send_hook.msgs.back()), "2");
        send_hook.msgs.clear();
    }

    frame_t frame;
    stats_t stats;
    send_accept_message_test send_hook;
    send_prepare_message_test prepare_hook;
    send_announce_message_test announce_hook;
    dropped_values_test drop_hook;
    shared_ptr<master_impl> impl;
    settings_t settings;
};

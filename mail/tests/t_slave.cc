#include "t_log.h"

#include "rands.hpp"
#include <catch.hpp>

TEST_CASE_METHOD(T_slave, "slave/learn/too_old", "")
{
    frame_init(2);

    impl->activate();

    value_t value1 = make_value("A");
    learn_message msg1;
    msg1.ballot = 11;
    msg1.accepted_value.slot = 1;
    msg1.accepted_value.value = value1;

    REQUIRE(
        !impl->pick_value(msg1.accepted_value.slot, msg1.ballot, msg1.accepted_value.value, "1"));
}

TEST_CASE_METHOD(T_slave, "slave/learn/commited", "")
{
    frame.get_slot(1).num = 1;
    frame.get_slot(1).set_state(state_t::committed);

    impl->activate();

    value_t value1 = make_value("A");
    learn_message msg1;
    msg1.ballot = 11;
    msg1.accepted_value.slot = 1;
    msg1.accepted_value.value = value1;

    REQUIRE(
        !impl->pick_value(msg1.accepted_value.slot, msg1.ballot, msg1.accepted_value.value, "1"));
}

#define LEARN(_slot, _ballot, _acceptor, check_result)                                             \
    {                                                                                              \
        value_t value1 = make_value("A");                                                          \
        learn_message msg1;                                                                        \
        msg1.ballot = (_ballot);                                                                   \
        msg1.accepted_value.slot = (_slot);                                                        \
        msg1.accepted_value.value = value1;                                                        \
        REQUIRE(                                                                                   \
            check_result ==                                                                        \
            impl->pick_value(                                                                      \
                msg1.accepted_value.slot, msg1.ballot, msg1.accepted_value.value, (_acceptor)));   \
    }

// TODO fix test name
TEST_CASE_METHOD(T_slave, "slave/corp01h-bug", "")
{
    const iid_t FAIL_IID = 33767421;
    const iid_t FAR_IID = 33898464;

    frame_init(FAIL_IID, N_SLOTS);

    frame.get_slot(FAIL_IID).init(
        FAIL_IID - frame.preallocated_slots_count(),
        9,
        state_t::committed,
        make_value("old-value"));

    impl->activate();

    LEARN(FAR_IID, 9, "01f", true);
    LEARN(FAR_IID, 9, "01h", true);
    REQUIRE(frame.write_zone_begin() == FAIL_IID);
}

TEST_CASE_METHOD(T_slave, "slave/sync/no_announce", "")
{
    frame.get_slot(0).init(0, 1, state_t::committed, make_value("v1"));
    frame.get_slot(1).init(1, 1, state_t::committed, make_value("v2"));
    frame.get_slot(2).init(2, 1, state_t::committed, make_value("v3"));
    frame.get_slot(3).init(3, 1, state_t::committed, make_value("v4"));
    frame_init(4);

    impl->synchronization_timeout();

    REQUIRE(sync_hook.msgs.size() == 0);
}

TEST_CASE_METHOD(T_slave, "slave/sync/simple/announce", "")
{
    frame.get_slot(0).init(0, 1, state_t::committed, make_value("v1"));
    frame.get_slot(1).init(1, 1, state_t::committed, make_value("v2"));
    frame.get_slot(2).init(2, 1, state_t::committed, make_value("v3"));
    frame.get_slot(3).init(3, 1, state_t::committed, make_value("v4"));
    frame_init(4);

    impl->activate();

    impl->master_announce_received({ 4, 8 });

    impl->synchronization_timeout();

    REQUIRE(sync_hook.msgs.size() == 1);
    REQUIRE(sync_hook.msgs[0].slots.size() == 4);
}

TEST_CASE_METHOD(T_slave, "log/sync/continue_slave_sync", "")
{
    frame.get_slot(0).init(0, 1, state_t::committed, make_value("v1"));
    frame.get_slot(1).init(1, 1, state_t::committed, make_value("v2"));
    frame.get_slot(2).init(2, 1, state_t::committed, make_value("v3"));
    frame.get_slot(3).init(3, 1, state_t::committed, make_value("v4"));
    frame_init(4);

    impl->activate();
    impl->master_announce_received({ 4, 8 });
    impl->synchronization_timeout();

    impl->deactivate();
    impl->activate();
    impl->master_announce_received({ 4, 9 });
    impl->synchronization_timeout();

    REQUIRE(sync_hook.msgs.size() == 2);
    REQUIRE(sync_hook.msgs[1].slots.size() == 5);
}

TEST_CASE_METHOD(T_slave, "slave/announces/receive", "")
{
    impl->activate();
    impl->master_announce_received({ 4, 8 });

    REQUIRE(frame.write_zone_begin() == 0);
    REQUIRE(impl->get_sync_top() == 8);
}

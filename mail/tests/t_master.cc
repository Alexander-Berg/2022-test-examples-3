#include "t_log.h"

#include "rands.hpp"
#include <catch.hpp>

TEST_CASE_METHOD(T_master, "master/activate/before_activate_not_active", "")
{
    REQUIRE(impl->is_active() == false);
}

TEST_CASE_METHOD(T_master, "master/activate/after_activate_is_active", "")
{
    impl->activate();
    REQUIRE(impl->is_active() == true);
}

TEST_CASE_METHOD(T_master, "master/activate/after_activate_not_prepared", "")
{
    impl->activate();
    REQUIRE(impl->is_prepared() == false);
}

TEST_CASE_METHOD(T_master, "master/activate/after_activate_will_send_prepare_message", "")
{
    impl->activate();
    REQUIRE(prepare_hook.msgs.size() == 1);
}

TEST_CASE_METHOD(T_master, "master/activate/after_deactivate_not_active", "")
{
    impl->activate();
    impl->submit(make_value("A"));
    impl->submit(make_value("B"));
    impl->deactivate();
    REQUIRE(impl->is_active() == false);
}

TEST_CASE_METHOD(T_master, "master/activate/after_deactivate_not_prepared", "")
{
    impl->activate();
    impl->submit(make_value("A"));
    impl->submit(make_value("B"));
    impl->deactivate();
    REQUIRE(impl->is_prepared() == false);
}

TEST_CASE_METHOD(T_master, "master/prepare/not_prepared_with_one_promise", "")
{
    impl->activate();
    promise_message msg = make_answer(prepare_hook.msgs.at(0));
    impl->promise_received(msg, acceptor_id_t("1"));
    REQUIRE(impl->is_prepared() == false);
}

TEST_CASE_METHOD(T_master, "master/prepare/prepared_with_two_promises", "")
{
    impl->activate();
    promise_message msg = make_answer(prepare_hook.msgs.at(0));
    impl->promise_received(msg, acceptor_id_t("1"));
    impl->promise_received(msg, acceptor_id_t("2"));
    REQUIRE(impl->is_prepared() == true);
}

TEST_CASE_METHOD(T_master, "master/prepare/prepared_finished_dont_send_another_prepare", "")
{
    impl->activate();
    promise_message msg = make_answer(prepare_hook.msgs.at(0));
    impl->promise_received(msg, acceptor_id_t("1"));
    impl->promise_received(msg, acceptor_id_t("2"));
    REQUIRE(prepare_hook.msgs.size() == 1);
}

TEST_CASE_METHOD(
    T_master,
    "master/prepare/promised_values",
    "if there are some values in prepare responses")
{
    frame_init(100);
    impl->ballot = 55;
    impl->activate();

    promise_message msg1 = make_answer(prepare_hook.msgs.at(0));
    promise_message msg2 = make_answer(prepare_hook.msgs.at(0));

    value_t value1 = make_value("AAA");
    value_t value2 = make_value("BBB");
    value_t value3 = make_value("CCC");

    msg1.accepted_values.add(
        ballot_slot_value_triplet(impl->ballot - 2, frame.write_zone_begin() + 1, value1));
    msg2.accepted_values.add(
        ballot_slot_value_triplet(impl->ballot - 1, frame.write_zone_begin() + 1, value2));
    msg2.accepted_values.add(
        ballot_slot_value_triplet(impl->ballot - 3, frame.write_zone_begin() + 3, value3));

    impl->promise_received(msg1, acceptor_id_t("1"));
    impl->promise_received(msg2, acceptor_id_t("2"));

    REQUIRE(impl->is_prepared() == true);
    REQUIRE(impl->ballot == msg1.acceptor_ballot);
    REQUIRE(!frame.get_slot(frame.write_zone_begin() + 0).value);
    REQUIRE(!frame.get_slot(frame.write_zone_begin() + 2).value);
    REQUIRE(frame.get_slot(frame.write_zone_begin() + 1).get_state() == state_t::wait);
    REQUIRE(frame.get_slot(frame.write_zone_begin() + 3).get_state() == state_t::wait);
    REQUIRE(frame.get_slot(frame.write_zone_begin() + 1).ballot == impl->ballot - 1);
    REQUIRE(frame.get_slot(frame.write_zone_begin() + 3).ballot == impl->ballot - 3);
    REQUIRE(frame.get_slot(frame.write_zone_begin() + 1).value.data() == value2.data());
    REQUIRE(frame.get_slot(frame.write_zone_begin() + 3).value.data() == value3.data());
}

TEST_CASE_METHOD(
    T_master,
    "master/prepare/promised_and_submitted_values_mix",
    "if there are some values in prepare responses and some submitted values too")
{
    frame_init(100);
    impl->ballot = 55;
    impl->activate();

    value_t value1 = make_value("AAA");
    value_t value2 = make_value("BBB");
    value_t value3 = make_value("CCC");

    impl->submit(value1);

    promise_message msg1 = make_answer(prepare_hook.msgs.at(0));
    promise_message msg2 = make_answer(prepare_hook.msgs.at(0));

    msg1.accepted_values.add(
        ballot_slot_value_triplet(impl->ballot - 1, frame.write_zone_begin(), value1));

    impl->promise_received(msg1, acceptor_id_t("1"));
    impl->promise_received(msg2, acceptor_id_t("2"));

    REQUIRE(impl->is_prepared() == true);
    REQUIRE(impl->ballot == msg1.acceptor_ballot);
    REQUIRE(frame.get_slot(frame.write_zone_begin() + 0).get_state() == state_t::propose);
    REQUIRE(frame.get_slot(frame.write_zone_begin() + 1).num == -1);
}

//-----------------------------------------

TEST_CASE_METHOD(T_master, "master/submit/no_slots_available", "")
{
    fake_prepare(1, 2);

    value_t vfirst = make_value("F");
    impl->submit(vfirst);
    for (slot_n i = 1; i < frame.write_zone_size() - 1; i++)
    {
        impl->submit(make_value(std::to_string(i)));
    }
    value_t vlast = make_value("L");
    impl->submit(vlast);

    // Q will be dropped because
    value_t vexcess = make_value("Q");
    impl->submit(vexcess);
    REQUIRE(frame.get_slot(frame.write_zone_end() - 1).value.data() == vlast.data());
    REQUIRE(value_in(vexcess, drop_hook.dropped_values));

    //
    learn_message msg1;
    msg1.ballot = impl->ballot;
    msg1.accepted_value.slot = 2;
    msg1.accepted_value.value = vfirst;
    impl->learn_received(msg1, acceptor_id_t("1"));
    impl->learn_received(msg1, acceptor_id_t("2"));

    value_t val;
    slot_profile_t prof;
    REQUIRE(get(frame, settings.log_func, 2, val, prof) == get_status_t::ok);

    impl->submit(vexcess);
    REQUIRE(frame.get_slot(frame.write_zone_end() - 1).value.data() == vexcess.data());
}

TEST_CASE_METHOD(T_master, "master/complete_prepare/no_slots_available/accept_one_but_N_SLOTS", "")
{
    impl->ballot = frame.write_zone_size() * 2;
    frame_init(17, 16);
    impl->activate();

    REQUIRE(impl->is_active() == true);
    REQUIRE(impl->is_prepared() == false);
    REQUIRE(prepare_hook.msgs.size() == 1);

    value_t submited = make_value("submited");
    impl->submit(submited);

    promise_message promise1 = make_answer(prepare_hook.msgs[0]);
    promise_message promise2 = make_answer(prepare_hook.msgs[0]);
    value_t value1 = make_value("1");
    for (slot_n i = 0; i < frame.write_zone_size(); i++)
    {
        promise2.accepted_values.add(
            ballot_slot_value_triplet(i + 1, frame.write_zone_begin() + i, value1));
    }

    impl->promise_received(promise1, acceptor_id_t("1"));
    impl->promise_received(promise2, acceptor_id_t("2"));

    REQUIRE(drop_hook.dropped_values.size() == 1);
    REQUIRE(value_in(submited, drop_hook.dropped_values));
    REQUIRE(!value_in(value1, drop_hook.dropped_values));
}

TEST_CASE_METHOD(T_master, "master/complete_prepare/no_slots_available/accept_N_SLOTS", "")
{
    impl->ballot = frame.write_zone_size() * 2;
    frame_init(100);
    impl->activate();

    REQUIRE(impl->is_active() == true);
    REQUIRE(impl->is_prepared() == false);
    REQUIRE(prepare_hook.msgs.size() == 1);

    value_t submited = make_value("submited");
    impl->submit(submited);

    promise_message promise1 = make_answer(prepare_hook.msgs[0]);
    promise_message promise2 = make_answer(prepare_hook.msgs[0]);
    value_t value1 = make_value("1");
    for (slot_n i = 0; i < frame.write_zone_size() + 1; i++)
    {
        promise1.accepted_values.add(
            ballot_slot_value_triplet(i + 1, frame.write_zone_begin() + i, value1));
    }

    impl->promise_received(promise1, acceptor_id_t("1"));
    impl->promise_received(promise2, acceptor_id_t("2"));

    REQUIRE(value_in(submited, drop_hook.dropped_values));
    REQUIRE(!value_in(value1, drop_hook.dropped_values));
    REQUIRE(drop_hook.dropped_values.size() == 1);
}

TEST_CASE_METHOD(
    T_master,
    "master/complete_prepare/no_slots_available/accept_more_than_N_SLOTS",
    "")
{
    impl->ballot = frame.write_zone_size() * 2;
    frame_init(100);
    impl->activate();

    REQUIRE(impl->is_active() == true);
    REQUIRE(impl->is_prepared() == false);
    REQUIRE(prepare_hook.msgs.size() == 1);

    value_t submited = make_value("submited");
    impl->submit(submited);

    const int dropped_count = 10;

    promise_message promise1 = make_answer(prepare_hook.msgs[0]);
    promise_message promise2 = make_answer(prepare_hook.msgs[0]);
    value_t value1 = make_value("1");
    for (slot_n i = 0; i < (frame.write_zone_size()) + (dropped_count - 1); i++)
    {
        promise1.accepted_values.add(
            ballot_slot_value_triplet(i + 1, frame.write_zone_begin() + i, value1));
    }

    impl->promise_received(promise1, acceptor_id_t("1"));
    impl->promise_received(promise2, acceptor_id_t("2"));

    REQUIRE(value_in(submited, drop_hook.dropped_values));
    REQUIRE(!value_in(value1, drop_hook.dropped_values));
    REQUIRE(drop_hook.dropped_values.size() == 1);
}

TEST_CASE_METHOD(T_master, "master/commit/no_slots_available", "")
{
    impl->ballot = 1;
    frame_init(0, 1024);
    impl->activate();

    REQUIRE(impl->is_active() == true);
    REQUIRE(impl->is_prepared() == false);
    REQUIRE(prepare_hook.msgs.size() == 1);

    value_t value1 = make_value("A");
    for (int i = 0; i < frame.write_zone_size(); i++)
    {
        impl->submit(value1);
        learn_message msg;
        msg.ballot = impl->ballot;
        msg.accepted_value.slot = i;
        msg.accepted_value.value = value1;

        impl->learn_received(msg, acceptor_id_t("1"));
        impl->learn_received(msg, acceptor_id_t("2"));
    }
    slot_n test_slot = frame.write_zone_end() - 1;
    value_t value2 = make_value("B");
    learn_message msg1;
    msg1.ballot = impl->ballot;
    msg1.accepted_value.slot = test_slot;
    msg1.accepted_value.value = value2;

    REQUIRE(!impl->learn_received(msg1, acceptor_id_t("1")));

    REQUIRE(!value_in(value2, drop_hook.dropped_values));
    REQUIRE(drop_hook.dropped_values.size() == 0);
}

TEST_CASE_METHOD(T_master, "master/common/mixed", "")
{
    frame_init(1);

    fake_prepare();

    value_t value1 = make_value("A");
    value_t value2 = make_value("B");
    value_t value3 = make_value("C");
    value_t value4 = make_value("C");

    fast_submit(value1);
    fast_submit(value2);
    fast_submit(value3);
    impl->submit(value4);

    value_t val;
    slot_profile_t prof;
    REQUIRE(get(frame, settings.log_func, 0, val, prof) == get_status_t::too_old);
    REQUIRE(get(frame, settings.log_func, 1, val, prof) == get_status_t::ok);
    REQUIRE(val.data() == value1.data());
    REQUIRE(get(frame, settings.log_func, 2, val, prof) == get_status_t::ok);
    REQUIRE(val.data() == value2.data());
    REQUIRE(get(frame, settings.log_func, 3, val, prof) == get_status_t::ok);
    REQUIRE(val.data() == value3.data());
    REQUIRE(get(frame, settings.log_func, 4, val, prof) == get_status_t::not_ready);
    REQUIRE(get(frame, settings.log_func, 5, val, prof) == get_status_t::not_ready);
}

TEST_CASE_METHOD(T_master, "master/prepare/timeout", "a prepare operation is timed out")
{
    impl->activate();

    promise_message msg = make_answer(prepare_hook.msgs.at(0));
    value_t value1 = make_value("AAA");
    msg.accepted_values.add(
        ballot_slot_value_triplet(impl->ballot, frame.write_zone_begin(), value1));

    impl->promise_received(msg, acceptor_id_t("1"));

    impl->prepare_timeout();

    REQUIRE(impl->is_prepared() == false);
    REQUIRE(impl->ballot > msg.acceptor_ballot);
}

TEST_CASE_METHOD(T_master, "master/prepare/promise-duplicate", "")
{
    impl->activate();

    promise_message msg = make_answer(prepare_hook.msgs.at(0));
    value_t value1 = make_value("AAA");
    msg.accepted_values.add(
        ballot_slot_value_triplet(impl->ballot, frame.write_zone_begin(), value1));

    impl->promise_received(msg, acceptor_id_t("1"));
    impl->promise_received(msg, acceptor_id_t("1"));

    REQUIRE(impl->is_prepared() == false);
    REQUIRE(impl->ballot == msg.acceptor_ballot);
    REQUIRE(prepare_hook.msgs.size() == 1);
}

TEST_CASE_METHOD(T_master, "master/prepare/old-ballot", "our current ballot is too old")
{
    impl->activate();

    promise_message msg = make_answer(prepare_hook.msgs.at(0));
    msg.acceptor_ballot = impl->ballot + 2;

    impl->promise_received(msg, acceptor_id_t("1"));

    REQUIRE(impl->is_prepared() == false);
    REQUIRE(impl->ballot > msg.acceptor_ballot);
    REQUIRE(prepare_hook.msgs.size() == 2);
    REQUIRE(impl->ballot > prepare_hook.msgs.at(0).ballot);
    REQUIRE(prepare_hook.msgs.at(1).ballot == impl->ballot);
}

TEST_CASE_METHOD(T_master, "master/submit/not-prepared", "")
{
    impl->activate();
    value_t value1 = make_value("AAAA");
    impl->submit(value1);

    REQUIRE(impl->is_prepared() == false);
    REQUIRE(prepare_hook.msgs.size() == 1);
    REQUIRE(send_hook.msgs.size() == 0);
    REQUIRE(frame.write_zone_begin() == 0);
}

TEST_CASE_METHOD(T_master, "master/submit/if_timeout_occured_before_learn_then_reset_prepare", "")
{
    fake_prepare();
    value_t value1 = make_value("AAAA");
    impl->submit(value1);

    REQUIRE(send_hook.msgs.size() == 1);

    impl->timeout(frame.write_zone_begin());

    REQUIRE(impl->is_prepared() == false);
    REQUIRE(prepare_hook.msgs.size() == 1);
}

TEST_CASE_METHOD(T_master, "master/submit/timeout-after-learn", "")
{
    fake_prepare();
    value_t value1 = make_value("AAAA");
    impl->submit(value1);

    REQUIRE(send_hook.msgs.size() == 1);
    impl->learn_received(make_answer(send_hook.msgs[0]), acceptor_id_t("1"));
    impl->learn_received(make_answer(send_hook.msgs[0]), acceptor_id_t("2"));

    impl->timeout(send_hook.msgs[0].pvalue.slot);

    REQUIRE(impl->is_prepared() == true);
}

TEST_CASE_METHOD(T_master, "master/submit/resend-after-prepare", "")
{
    value_t value1 = make_value("AAAA");
    value_t value2 = make_value("BBBB");
    impl->activate();

    impl->submit(value1);
    impl->submit(value2);

    REQUIRE(send_hook.msgs.size() == 0);

    promise_message msg = make_answer(prepare_hook.msgs.at(0));
    impl->promise_received(msg, acceptor_id_t("1"));
    impl->promise_received(msg, acceptor_id_t("2"));

    REQUIRE(send_hook.msgs.size() == 2);
    impl->learn_received(make_answer(send_hook.msgs[0]), acceptor_id_t("1"));
    impl->learn_received(make_answer(send_hook.msgs[0]), acceptor_id_t("2"));

    REQUIRE(frame.write_zone_begin() == 1);
    REQUIRE(impl->is_prepared() == true);
}

TEST_CASE_METHOD(T_master, "T_master/submit/fast_submit_changes_read_n_write_zones", "")
{
    fake_prepare();

    fast_submit(make_value("AAAA"));
    fast_submit(make_value("BBBB"));
    fast_submit(make_value("CCCC"));

    REQUIRE(frame.read_zone_begin() == 0);
    REQUIRE(frame.write_zone_begin() == 3);
}

TEST_CASE_METHOD(T_master, "T_master/submit/window_limit", "")
{
    fake_prepare();

    impl->settings.max_parallel_accepts = 3;

    impl->submit(make_value("AAAA"));
    impl->submit(make_value("BBBB"));
    impl->submit(make_value("CCCC"));
    impl->submit(make_value("DDDD"));

    REQUIRE(send_hook.msgs.size() == 3);

    REQUIRE(frame.state_char(frame.get_slot(0)) == 'P');
    REQUIRE(frame.state_char(frame.get_slot(1)) == 'P');
    REQUIRE(frame.state_char(frame.get_slot(2)) == 'P');
    REQUIRE(frame.state_char(frame.get_slot(3)) == 'W');

    REQUIRE(frame.write_zone_begin() == 0);
}

TEST_CASE_METHOD(T_master, "master/submit/resubmit", "")
{
    fake_prepare();

    value_t value_check_resubmit = make_value("BBBB");

    impl->submit(make_value("AAAA"));
    impl->submit(value_check_resubmit);
    impl->submit(make_value("CCCC"));

    learn_message msg1;
    msg1.ballot = impl->ballot;
    msg1.accepted_value.slot = 1;
    msg1.accepted_value.value = make_value("DDDD");

    impl->learn_received(msg1, acceptor_id_t("1"));
    impl->learn_received(msg1, acceptor_id_t("2"));

    REQUIRE(send_hook.msgs.size() == 4);
    REQUIRE(send_hook.msgs.back().pvalue.value.data() == value_check_resubmit.data());
}

TEST_CASE_METHOD(T_master, "T_master/submit/resubmit_window_limit", "")
{
    fake_prepare();

    impl->settings.drop_submits_while_preparing = false;
    impl->settings.max_parallel_accepts = 3;

    impl->submit(make_value("AAAA"));
    impl->submit(make_value("BBBB"));
    impl->submit(make_value("CCCC"));
    impl->submit(make_value("DDDD"));

    REQUIRE(send_hook.msgs.size() == 3);

    fake_next_round();

    REQUIRE(send_hook.msgs.size() == 6);

    REQUIRE(frame.state_char(frame.get_slot(0)) == 'P');
    REQUIRE(frame.state_char(frame.get_slot(1)) == 'P');
    REQUIRE(frame.state_char(frame.get_slot(2)) == 'P');
    REQUIRE(frame.state_char(frame.get_slot(3)) == 'W');

    REQUIRE(frame.write_zone_begin() == 0);
}

TEST_CASE_METHOD(T_master, "master/submit/if_reject_then_reset_prepare", "")
{
    fake_prepare(11);

    impl->submit(make_value("A"));
    impl->submit(make_value("B"));

    reject_message msg1;
    msg1.acceptor_ballot = impl->ballot + 3;
    msg1.request_ballot = impl->ballot;
    msg1.request_slot = 0;

    impl->reject_received(msg1, acceptor_id_t("1"));

    REQUIRE(prepare_hook.msgs.size() == 1);
    REQUIRE(impl->is_active() == true);
    REQUIRE(impl->is_prepared() == false);
}

TEST_CASE_METHOD(T_master, "master/learn/too_fresh_ballot", "")
{
    fake_prepare(11);
    learn_message msg1;
    msg1.ballot = impl->ballot + 1;
    msg1.accepted_value.slot = 0;
    msg1.accepted_value.value = make_value("A");
    REQUIRE(!impl->learn_received(msg1, "1"));
}

TEST_CASE_METHOD(T_master, "master/learn/2", "")
{
    fake_prepare(11);
    learn_message msg1;
    msg1.ballot = impl->ballot - 1;
    msg1.accepted_value.slot = 0;
    msg1.accepted_value.value = make_value("A");
    REQUIRE(!impl->learn_received(msg1, "1"));
}

TEST_CASE_METHOD(T_master, "master/learn/3", "")
{
    fake_prepare(11);
    learn_message msg1;
    msg1.ballot = impl->ballot;
    msg1.accepted_value.slot = 0;
    msg1.accepted_value.value = make_value("A");

    REQUIRE(!impl->learn_received(msg1, "1"));
}

TEST_CASE_METHOD(T_master, "master/learn/4", "")
{
    fake_prepare(11);
    fast_submit(make_value("BB"));
    fast_submit(make_value("CC"));
    fast_submit(make_value("DD"));
    learn_message msg1;
    msg1.ballot = impl->ballot;
    msg1.accepted_value.slot = 1;
    msg1.accepted_value.value = make_value("A");
    REQUIRE(!impl->learn_received(msg1, "1"));
}

TEST_CASE_METHOD(T_master, "master/learn/5", "")
{
    fake_prepare(11);
    frame.get_slot(0).num = 0;
    frame.get_slot(0).set_state(state_t::propose);
    learn_message msg1;
    msg1.ballot = impl->ballot;
    msg1.accepted_value.slot = 0;
    msg1.accepted_value.value = make_value("A");
    REQUIRE(impl->learn_received(msg1, "1"));
    REQUIRE(impl->learn_received(msg1, "1"));
    REQUIRE(impl->learn_received(msg1, "2"));
}

TEST_CASE_METHOD(T_master, "master/learn/ignore_conflicting_values", "")
{
    fake_prepare(11);

    frame.get_slot(1).num = 1;
    frame.get_slot(1).set_state(state_t::propose);

    learn_message msg1;
    msg1.ballot = impl->ballot;
    msg1.accepted_value.slot = 1;
    msg1.accepted_value.value = make_value("A");
    learn_message msg2;
    msg2.ballot = impl->ballot;
    msg2.accepted_value.slot = 1;
    msg2.accepted_value.value = make_value("B");

    REQUIRE(impl->learn_received(msg1, "1"));
    REQUIRE(impl->learn_received(msg2, "2"));
    REQUIRE(impl->is_prepared() == true); // ignored
}

TEST_CASE_METHOD(T_master, "master/accumulate/reset_after_deactivate", "")
{
    impl->activate();

    promise_message msg = make_answer(prepare_hook.msgs.at(0));
    msg.accepted_values.add({ 11, 1, make_value("123") });
    impl->promise_received(msg, "1");

    REQUIRE(frame.get_slot(1).get_state() == state_t::accumulate);

    impl->deactivate();

    REQUIRE(frame.get_slot(1).num == -1);
    REQUIRE(!frame.get_slot(1).is_inited());
}

TEST_CASE_METHOD(T_master, "master/accumulate/with-missing", "")
{
    impl->activate();

    promise_message msg1 = make_answer(prepare_hook.msgs.at(0));
    msg1.accepted_values.add({ 11, 0, make_value("123") });
    msg1.accepted_values.add({ 11, 1, make_value("345") });
    msg1.accepted_values.add({ 11, 2, make_value("678") });
    msg1.accepted_values.add({ 11, 3, make_value("901") });
    promise_message msg2 = make_answer(prepare_hook.msgs.at(0));
    msg2.accepted_values.add({ 11, 0, make_value("123") });
    msg2.accepted_values.add({ 11, 2, make_value("678") });
    impl->promise_received(msg1, "1");
    impl->promise_received(msg2, "2");

    REQUIRE(frame.state_char(frame.get_slot(0)) == 'C');
    REQUIRE(frame.state_char(frame.get_slot(1)) == 'P');
    REQUIRE(frame.state_char(frame.get_slot(2)) == 'C');
    REQUIRE(frame.state_char(frame.get_slot(3)) == 'P');

    REQUIRE(send_hook.msgs.size() == 2);
    REQUIRE(frame.write_zone_begin() == 1);
}

TEST_CASE_METHOD(T_master, "master/accumulate/collisions", "")
{
    impl->activate();

    promise_message msg1 = make_answer(prepare_hook.msgs.at(0));
    msg1.accepted_values.add({ 11, 0, make_value("123") });
    msg1.accepted_values.add({ 11, 1, make_value("345") });
    msg1.accepted_values.add({ 11, 2, make_value("678") });
    msg1.accepted_values.add({ 11, 3, make_value("901") });
    promise_message msg2 = make_answer(prepare_hook.msgs.at(0));
    msg2.accepted_values.add({ 11, 0, make_value("123") });
    msg2.accepted_values.add({ 11, 2, make_value("876") });
    impl->promise_received(msg1, "1");
    impl->promise_received(msg2, "2");

    REQUIRE(frame.state_char(frame.get_slot(0)) == 'C');
    REQUIRE(frame.state_char(frame.get_slot(1)) == 'P');
    REQUIRE(frame.state_char(frame.get_slot(2)) == '-');
    REQUIRE(frame.state_char(frame.get_slot(3)) == 'W');

    impl->submit(make_value("678"));

    REQUIRE(frame.state_char(frame.get_slot(2)) == 'P');
    REQUIRE(frame.state_char(frame.get_slot(3)) == 'P');
}

TEST_CASE_METHOD(
    T_master,
    "master/fixed-bugs/no_free_slots_bug",
    "false free slot learned before became a master")
{
    impl->activate();

    value_t value1 = make_value("A");

    learn_message msg1;
    msg1.ballot = 2;
    msg1.accepted_value.slot = 271;
    msg1.accepted_value.value = value1;
    REQUIRE(!impl->learn_received(msg1, "1"));
}

TEST_CASE_METHOD(
    T_master,
    "master/prepare/limited_promise_by_acceptors_gives_max_promised_value_and_finished_prepare",
    "")
{
    impl->activate();
    promise_message msg = make_answer(prepare_hook.msgs.at(0));
    msg.max_slot = prepare_hook.msgs.at(0).end_slot;
    impl->promise_received(msg, acceptor_id_t("1"));
    impl->promise_received(msg, acceptor_id_t("2"));
    REQUIRE(impl->is_prepared() == true);
}

TEST_CASE_METHOD(
    T_master,
    "master/prepare/"
    "limited_promise_by_acceptors_with_full_values_range_gives_immediate_reset_prepare",
    "")
{
    impl->activate();
    auto req_message = prepare_hook.msgs.at(0);
    promise_message msg1 = make_answer(prepare_hook.msgs.at(0));
    promise_message msg2 = make_answer(prepare_hook.msgs.at(0));
    msg1.max_slot = prepare_hook.msgs.at(0).end_slot;
    msg2.max_slot = prepare_hook.msgs.at(0).end_slot;
    for (auto i = req_message.slot; i < req_message.end_slot; ++i)
    {
        auto value = make_value("value-" + std::to_string(i));
        msg1.accepted_values.add(ballot_slot_value_triplet(req_message.ballot, i, value));
        msg2.accepted_values.add(ballot_slot_value_triplet(req_message.ballot, i, value));
    }
    impl->promise_received(msg1, acceptor_id_t("1"));
    impl->promise_received(msg2, acceptor_id_t("2"));

    SECTION("immediately_gets_not_prepared", "")
    {
        REQUIRE(impl->is_prepared() == false);
    }

    SECTION("prepare_message_not_sent_while_no_space_available_in_write_zone", "")
    {
        REQUIRE(frame.write_zone_size() == 0);
        REQUIRE(prepare_hook.msgs.size() == 1);
    }

    SECTION("prepare_sent_after_some_space_is_available_in_write_zone", "")
    {
        value_t value;
        slot_profile_t profile;
        impl->get(0, value, profile);
        REQUIRE(prepare_hook.msgs.size() == 2);
    }
}

TEST_CASE_METHOD(
    T_master,
    "master/prepare/"
    "limited_promise_with_partial_values_range_gives_submits_until_promised_slot_exceeded",
    "")
{
    impl->activate();
    auto req_message = prepare_hook.msgs.at(0);
    promise_message msg1 = make_answer(prepare_hook.msgs.at(0));
    promise_message msg2 = make_answer(prepare_hook.msgs.at(0));
    msg1.max_slot = prepare_hook.msgs.at(0).end_slot;
    msg2.max_slot = prepare_hook.msgs.at(0).end_slot;
    for (auto i = req_message.slot; i < req_message.end_slot - 1; ++i)
    {
        auto value = make_value("value-" + std::to_string(i));
        msg1.accepted_values.add(ballot_slot_value_triplet(req_message.ballot, i, value));
        msg2.accepted_values.add(ballot_slot_value_triplet(req_message.ballot, i, value));
    }
    impl->promise_received(msg1, acceptor_id_t("1"));
    impl->promise_received(msg1, acceptor_id_t("2"));
    value_t value;
    slot_profile_t profile;
    impl->get(0, value, profile);
    fast_submit(make_value("last_value"));
    REQUIRE(impl->is_prepared() == false);
    REQUIRE(prepare_hook.msgs.size() == 2);
}

TEST_CASE_METHOD(T_master, "master/drop_submits_option_disabled_no_drops", "")
{
    impl->settings.drop_submits_while_preparing = false;
    impl->activate();
    impl->submit(make_value("ABC"));
    REQUIRE(drop_hook.dropped_values.size() == 0);
}

TEST_CASE_METHOD(T_master, "master/drop_submits_option_works_while_preparing", "")
{
    impl->settings.drop_submits_while_preparing = true;
    impl->activate();
    impl->submit(make_value("ABC"));
    REQUIRE(drop_hook.dropped_values.size() == 1);
}

TEST_CASE_METHOD(T_master, "master/drop_submits_option_doesnt_work_after_prepared", "")
{
    impl->settings.drop_submits_while_preparing = true;
    fake_prepare();
    impl->submit(make_value("ABC"));
    REQUIRE(drop_hook.dropped_values.size() == 0);
}

TEST_CASE_METHOD(T_master, "master/announces/check", "")
{
    fake_prepare(1, 4);
    frame.get_slot(0).init(0, 1, state_t::committed, make_value("v1"));
    frame.get_slot(1).init(1, 1, state_t::committed, make_value("v2"));
    frame.get_slot(2).init(2, 1, state_t::committed, make_value("v3"));
    frame.get_slot(3).init(3, 1, state_t::committed, make_value("v4"));

    impl->master_announce_timeout();
    REQUIRE(announce_hook.msgs.size() == 1);

    impl->deactivate();
    impl->master_announce_timeout();
    REQUIRE(announce_hook.msgs.size() == 1);

    impl->activate();
    impl->master_announce_timeout();
    REQUIRE(announce_hook.msgs.size() == 2);
}

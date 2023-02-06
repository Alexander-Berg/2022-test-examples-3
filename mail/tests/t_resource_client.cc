#include "../src/resource_client.h"
#include "../src/messages.h"

#include <ymod_lease/ymod_lease.h>
#include <sintimers/test.h>

#include <catch.hpp>
#include <boost/lexical_cast.hpp>

using timers::test_timer;
using timers::test_queue;

using namespace ylease;

#define RES "ABC"
#define NODE "node-123"
#define TAG "mytag"
#define ANOTHER_NODE "node-8923897"
#define N_OF_ARBITERS 3
#define MAJORITY (N_OF_ARBITERS / 2 + 1)
#define VALUE "value1"
#define ANOTHER_VALUE "value2"

struct prepare_func
{
    prepare_func() : calls(0)
    {
    }

    void operator()(const prepare_msg& _msg)
    {
        ++calls;
        msg = _msg;
    }
    int calls;
    prepare_msg msg;
};

struct accept_func
{
    accept_func() : calls(0)
    {
    }

    void operator()(const accept_msg& _msg)
    {
        ++calls;
        msg = _msg;
    }
    int calls;
    accept_msg msg;
};

struct win_func
{

    win_func() : calls(0), ballot(0)
    {
    }

    void operator()(const lease& _lease, const ballot_t _ballot)
    {
        ++calls;
        lease = _lease;
        ballot = _ballot;
    }
    int calls;
    lease lease;
    ballot_t ballot;
};

struct lose_func
{

    lose_func() : calls(0)
    {
    }

    void operator()()
    {
        ++calls;
    }
    int calls;
};

struct T_RESCLIENT
{
public:
    prepare_func prepare_f;
    accept_func accept_f;
    win_func win_f;
    lose_func lose_f;
    boost::shared_ptr<test_queue> timers_queue;
    boost::shared_ptr<resource_client> resource;
    settings st;

    T_RESCLIENT()
    {
        timers_queue.reset(new test_queue());
        resource.reset(new resource_client(
            NODE,
            RES,
            TAG,
            st.acquire_lease_timeout,
            st.max_lease_time,
            N_OF_ARBITERS,
            *timers_queue,
            boost::ref(prepare_f),
            boost::ref(accept_f),
            boost::ref(win_f),
            boost::ref(lose_f),
            log_function_t()));
    }
    ~T_RESCLIENT()
    {
    }
};

TEST_CASE_METHOD(T_RESCLIENT, "node-res/create", "check not actve on create, resource is not holds")
{
    REQUIRE(!resource->active());
    REQUIRE(!resource->is_mine());
    REQUIRE(prepare_f.calls == 0);
    REQUIRE(accept_f.calls == 0);
    REQUIRE(win_f.calls == 0);
    REQUIRE(lose_f.calls == 0);
    REQUIRE(timers_queue->size() == 0);
}

TEST_CASE_METHOD(T_RESCLIENT, "node-res/start-aquire", "")
{
    resource->start_acquire_lease();
    REQUIRE(resource->active());
    REQUIRE(!resource->is_mine());
    REQUIRE(prepare_f.calls == 1);
    REQUIRE(prepare_f.msg.ballot == 0);
    REQUIRE(prepare_f.msg.resource_name == RES);
    REQUIRE(timers_queue->size() == 1);
    //    REQUIRE(timers_queue->timers.back());
    //    REQUIRE(timers_queue->timers.back()->active());
}

TEST_CASE_METHOD(T_RESCLIENT, "node-res/stop-aquire/simple", "")
{
    resource->start_acquire_lease();
    resource->stop_acquire_lease();
    REQUIRE(!resource->active());
    REQUIRE(!resource->is_mine());
    // timer not changed but cancelled
    REQUIRE(timers_queue->size() == 1);
    //    REQUIRE(!timers_queue->timers.back()->active());
}

TEST_CASE_METHOD(T_RESCLIENT, "node-res/stop-aquire/promise-after-stop", "")
{
    resource->start_acquire_lease();
    resource->stop_acquire_lease();

    // promise
    REQUIRE(!resource->promise(promise_msg("0", RES, 0)));

    REQUIRE(prepare_f.calls == 1);
    REQUIRE(accept_f.calls == 0);
    REQUIRE(win_f.calls == 0);
    REQUIRE(lose_f.calls == 1);
}

TEST_CASE_METHOD(T_RESCLIENT, "node-res/promise/no-start", "")
{
    // promise
    REQUIRE(!resource->promise(promise_msg("0", RES, 0)));

    REQUIRE(prepare_f.calls == 0);
    REQUIRE(accept_f.calls == 0);
    REQUIRE(timers_queue->size() == 0);
    REQUIRE(win_f.calls == 0);
    REQUIRE(lose_f.calls == 0);
}

TEST_CASE_METHOD(T_RESCLIENT, "node-res/promise/normal", "")
{
    resource->start_acquire_lease();

    // promise
    REQUIRE(resource->promise(promise_msg("0", RES, 0)));

    REQUIRE(prepare_f.calls == 1);
    REQUIRE(accept_f.calls == 0);
    REQUIRE(timers_queue->size() == 1);
    REQUIRE(win_f.calls == 0);
    REQUIRE(lose_f.calls == 0);
}

TEST_CASE_METHOD(T_RESCLIENT, "node-res/promise/duplicates", "")
{
    resource->start_acquire_lease();

    // promise
    REQUIRE(resource->promise(promise_msg("0", RES, 0)));
    REQUIRE(!resource->promise(promise_msg("0", RES, 0)));

    REQUIRE(prepare_f.calls == 1);
    REQUIRE(accept_f.calls == 0);
    REQUIRE(timers_queue->size() == 1);
    REQUIRE(win_f.calls == 0);
    REQUIRE(lose_f.calls == 0);
}

TEST_CASE_METHOD(T_RESCLIENT, "node-res/promise/majority", "")
{
    resource->start_acquire_lease();

    // promise
    for (int i = 0; i < MAJORITY - 1; i++)
    {
        REQUIRE(resource->promise(promise_msg("0", RES, 0)));
        REQUIRE(accept_f.calls == 0);
    }

    REQUIRE(resource->promise(promise_msg(boost::lexical_cast<string>(MAJORITY), RES, 0)));
    REQUIRE(prepare_f.calls == 1);
    REQUIRE(accept_f.calls == 1);
    REQUIRE(timers_queue->size() == 1);
    REQUIRE(win_f.calls == 0);
    REQUIRE(lose_f.calls == 1);
}

TEST_CASE_METHOD(T_RESCLIENT, "node-res/promise/majority-with-rejects", "")
{
    resource->start_acquire_lease();

    // promise
    for (int i = 0; i < MAJORITY - 1; i++)
    {
        REQUIRE(resource->promise(promise_msg(boost::lexical_cast<string>(i), RES, 0)));
        REQUIRE(accept_f.calls == 0);
    }

    REQUIRE(resource->reject(reject_msg(boost::lexical_cast<string>(MAJORITY - 1), RES, 0, 1)));
    REQUIRE(!resource->promise(promise_msg(boost::lexical_cast<string>(MAJORITY), RES, 0)));
    REQUIRE(prepare_f.calls == 2);
    REQUIRE(accept_f.calls == 0);
    REQUIRE(timers_queue->size() == 2);
    REQUIRE(win_f.calls == 0);
    REQUIRE(lose_f.calls == 0);
}

TEST_CASE_METHOD(T_RESCLIENT, "node-res/promise/discard-another-ballot", "")
{
    resource->start_acquire_lease();

    REQUIRE(!resource->promise(promise_msg("0", RES, 1)));
    REQUIRE(prepare_f.calls == 1);
    REQUIRE(accept_f.calls == 0);
    REQUIRE(win_f.calls == 0);
    REQUIRE(lose_f.calls == 0);
}

TEST_CASE_METHOD(T_RESCLIENT, "node-res/reject/discard-another-ballot", "")
{
    resource->start_acquire_lease();

    REQUIRE(!resource->reject(reject_msg("0", RES, 1, 100)));
    REQUIRE(prepare_f.calls == 1);
    REQUIRE(accept_f.calls == 0);
    REQUIRE(win_f.calls == 0);
    REQUIRE(lose_f.calls == 0);
}

TEST_CASE_METHOD(T_RESCLIENT, "node-res/reject/single", "")
{
    resource->start_acquire_lease();

    REQUIRE(resource->reject(reject_msg("0", RES, 0, 1)));
    REQUIRE(prepare_f.calls == 2);
    REQUIRE(accept_f.calls == 0);
    REQUIRE(win_f.calls == 0);
    REQUIRE(lose_f.calls == 0);
}

TEST_CASE_METHOD(
    T_RESCLIENT,
    "node-res/reject/majority",
    "after reject majority must restart preparing with highest ballot")
{
    resource->start_acquire_lease();
    REQUIRE(resource->reject(reject_msg("0", RES, 0, 100)));
    REQUIRE(prepare_f.calls == 2);
    REQUIRE(prepare_f.msg.ballot == 101);
    REQUIRE(timers_queue->size() == 2);
    REQUIRE(accept_f.calls == 0);
    REQUIRE(win_f.calls == 0);
    REQUIRE(lose_f.calls == 0);
}

TEST_CASE_METHOD(T_RESCLIENT, "node-res/accept/duplicate", "")
{
    resource->start_acquire_lease();

    for (int i = 0; i < MAJORITY; i++)
    {
        REQUIRE(resource->promise(promise_msg(boost::lexical_cast<string>(i), RES, 0)));
    }

    REQUIRE(resource->win(accepted_msg("0", RES, 0, accept_f.msg.lease)));
    REQUIRE(!resource->win(accepted_msg("0", RES, 0, accept_f.msg.lease)));

    REQUIRE(resource->active());
    REQUIRE(!resource->is_mine());
}

TEST_CASE_METHOD(T_RESCLIENT, "node-res/accept/majority", "")
{
    resource->start_acquire_lease();
    resource->update_acquire_value(VALUE);

    for (int i = 0; i < MAJORITY; i++)
    {
        REQUIRE(resource->promise(promise_msg(boost::lexical_cast<string>(i), RES, 0)));
    }

    for (int i = 0; i < MAJORITY - 1; i++)
    {
        REQUIRE(resource->win(
            accepted_msg(boost::lexical_cast<string>(i), RES, 0, accept_f.msg.lease)));
    }

    REQUIRE(resource->active());
    REQUIRE(!resource->is_mine());

    REQUIRE(win_f.calls == 0);
    REQUIRE(lose_f.calls == 1);
    REQUIRE(timers_queue->size() == 1);

    REQUIRE(resource->win(
        accepted_msg(boost::lexical_cast<string>(MAJORITY), RES, 0, accept_f.msg.lease)));

    REQUIRE(!resource->active());
    REQUIRE(resource->is_mine());

    REQUIRE(win_f.calls == 1);
    REQUIRE(win_f.lease.node == NODE);
    REQUIRE(win_f.ballot == 0);
    REQUIRE(win_f.lease.value == VALUE);
    REQUIRE(timers_queue->size() == 3); // 2 new calls: acquire and expiry timeout
                                        //    REQUIRE(timers_queue->timers.back());
                                        //    timers_queue->timers.back().second();
    REQUIRE(lose_f.calls == 1);
}

TEST_CASE_METHOD(T_RESCLIENT, "node-res/acquire-timeout/before-promise", "")
{
    resource->start_acquire_lease();

    for (int i = 0; i < MAJORITY - 1; i++)
    {
        REQUIRE(resource->promise(promise_msg(boost::lexical_cast<string>(i), RES, 0)));
    }

    REQUIRE(resource->acquire_lease_timeout(0));
    REQUIRE(prepare_f.calls == 2);
    REQUIRE(prepare_f.msg.ballot == 1);
    REQUIRE(lose_f.calls == 1); // not winner
    REQUIRE(win_f.calls == 0);  // not winner

    REQUIRE(resource->promise(promise_msg(boost::lexical_cast<string>(MAJORITY), RES, 1)));
}

TEST_CASE_METHOD(T_RESCLIENT, "node-res/acquire-timeout/before-win", "")
{
    resource->start_acquire_lease();

    for (int i = 0; i < MAJORITY; i++)
    {
        REQUIRE(resource->promise(promise_msg(boost::lexical_cast<string>(i), RES, 0)));
    }

    for (int i = 0; i < MAJORITY - 1; i++)
    {
        REQUIRE(resource->win(
            accepted_msg(boost::lexical_cast<string>(i), RES, 0, accept_f.msg.lease)));
    }

    REQUIRE(resource->acquire_lease_timeout(0));
    REQUIRE(prepare_f.calls == 2);
    REQUIRE(prepare_f.msg.ballot == 1);
    REQUIRE(lose_f.calls == 2);
    REQUIRE(win_f.calls == 0);

    REQUIRE(resource->promise(promise_msg("0", RES, 1)));
}

TEST_CASE_METHOD(T_RESCLIENT, "node-res/acquire-timeout/after-win", "")
{
    resource->start_acquire_lease();

    for (int i = 0; i < MAJORITY; i++)
    {
        REQUIRE(resource->promise(promise_msg(boost::lexical_cast<string>(i), RES, 0)));
    }

    for (int i = 0; i < MAJORITY; i++)
    {
        REQUIRE(resource->win(
            accepted_msg(boost::lexical_cast<string>(i), RES, 0, accept_f.msg.lease)));
    }

    REQUIRE(resource->acquire_lease_timeout(0));
    REQUIRE(prepare_f.calls == 2);
    REQUIRE(prepare_f.msg.ballot == 1);
    REQUIRE(lose_f.calls == 1);
    REQUIRE(win_f.calls == 1);

    REQUIRE(resource->promise(promise_msg("0", RES, 1)));
}

TEST_CASE_METHOD(T_RESCLIENT, "node-res/extend-timeout/before-promise", "")
{
    resource->start_acquire_lease();

    for (int i = 0; i < MAJORITY - 1; i++)
    {
        REQUIRE(resource->promise(promise_msg(boost::lexical_cast<string>(i), RES, 0)));
    }

    REQUIRE(!resource->expiry_lease_timeout(0));
}

TEST_CASE_METHOD(T_RESCLIENT, "node-res/expiry-timeout/before-win", "")
{
    resource->start_acquire_lease();

    for (int i = 0; i < MAJORITY; i++)
    {
        REQUIRE(resource->promise(promise_msg(boost::lexical_cast<string>(i), RES, 0)));
    }

    for (int i = 0; i < MAJORITY - 1; i++)
    {
        REQUIRE(resource->win(
            accepted_msg(boost::lexical_cast<string>(i), RES, 0, accept_f.msg.lease)));
    }

    REQUIRE(!resource->expiry_lease_timeout(0));
}

TEST_CASE_METHOD(T_RESCLIENT, "node-res/expiry-timeout/after-win", "")
{
    resource->start_acquire_lease();

    for (int i = 0; i < MAJORITY; i++)
    {
        REQUIRE(resource->promise(promise_msg(boost::lexical_cast<string>(i), RES, 0)));
    }

    for (int i = 0; i < MAJORITY; i++)
    {
        REQUIRE(resource->win(
            accepted_msg(boost::lexical_cast<string>(i), RES, 0, accept_f.msg.lease)));
    }

    REQUIRE(resource->expiry_lease_timeout(0));
    REQUIRE(prepare_f.calls == 2);
    REQUIRE(prepare_f.msg.ballot == 1);
    REQUIRE(lose_f.calls == 2);
    REQUIRE(win_f.calls == 1);

    REQUIRE(resource->promise(promise_msg("0", RES, 1)));
}

TEST_CASE_METHOD(T_RESCLIENT, "node-res/extend/rejected", "")
{
    resource->start_acquire_lease();

    for (int i = 0; i < MAJORITY; i++)
    {
        REQUIRE(resource->promise(promise_msg(boost::lexical_cast<string>(i), RES, 0)));
    }

    for (int i = 0; i < MAJORITY; i++)
    {
        REQUIRE(resource->win(
            accepted_msg(boost::lexical_cast<string>(i), RES, 0, accept_f.msg.lease)));
    }

    REQUIRE(resource->acquire_lease_timeout(0));
    REQUIRE(prepare_f.calls == 2);
    REQUIRE(prepare_f.msg.ballot == 1);
    REQUIRE(lose_f.calls == 1);
    REQUIRE(win_f.calls == 1);

    REQUIRE(resource->reject(reject_msg("0", RES, 1, 2)));
    REQUIRE(prepare_f.calls == 3);
    REQUIRE(lose_f.calls == 1);
    REQUIRE(win_f.calls == 1);
}

TEST_CASE_METHOD(T_RESCLIENT, "node-res/extend/timeout", "")
{
    resource->start_acquire_lease();

    for (int i = 0; i < MAJORITY; i++)
    {
        REQUIRE(resource->promise(promise_msg(boost::lexical_cast<string>(i), RES, 0)));
    }

    for (int i = 0; i < MAJORITY; i++)
    {
        REQUIRE(resource->win(
            accepted_msg(boost::lexical_cast<string>(i), RES, 0, accept_f.msg.lease)));
    }

    const ballot_t WIN_BALLOT = 0;

    REQUIRE(resource->acquire_lease_timeout(0));
    REQUIRE(prepare_f.calls == 2);
    REQUIRE(prepare_f.msg.ballot == 1);
    REQUIRE(lose_f.calls == 1);
    REQUIRE(win_f.calls == 1);

    REQUIRE(resource->promise(promise_msg("0", RES, 1)));

    REQUIRE(resource->acquire_lease_timeout(1));
    REQUIRE(prepare_f.calls == 3);
    REQUIRE(prepare_f.msg.ballot == 2);
    REQUIRE(lose_f.calls == 1);
    REQUIRE(win_f.calls == 1);

    REQUIRE(resource->promise(promise_msg("0", RES, 2)));

    REQUIRE_NOTHROW(resource->expiry_lease_timeout(WIN_BALLOT));
    REQUIRE(prepare_f.calls == 4);
    REQUIRE(prepare_f.msg.ballot == 3);
    REQUIRE(lose_f.calls == 2);
    REQUIRE(win_f.calls == 1);
}

TEST_CASE_METHOD(T_RESCLIENT, "node-res/reject/accept", "")
{
    resource->start_acquire_lease();

    for (int i = 0; i < MAJORITY; i++)
    {
        REQUIRE(resource->promise(promise_msg(boost::lexical_cast<string>(i), RES, 0)));
    }

    REQUIRE(resource->reject(reject_msg("0", RES, 0, 0)));

    REQUIRE(prepare_f.calls == 2);
    REQUIRE(prepare_f.msg.ballot == 1);
    REQUIRE(lose_f.calls == 1);
    REQUIRE(win_f.calls == 0);

    REQUIRE(!resource->promise(promise_msg("0", RES, 0)));
}

TEST_CASE_METHOD(
    T_RESCLIENT,
    "node-res/accept/another-node-lease",
    "check not starting accept phase if another node holds resource")
{
    resource->start_acquire_lease();

    for (int i = 0; i < MAJORITY; i++)
    {
        REQUIRE(resource->promise(promise_msg(
            boost::lexical_cast<string>(i), RES, 0, 0, lease(ANOTHER_NODE, 1000, VALUE))));
    }

    // check ignored
    REQUIRE(accept_f.calls == 0);
}

TEST_CASE_METHOD(T_RESCLIENT, "node-res/accept/already-expired", "")
{
    resource->start_acquire_lease();

    for (int i = 0; i < MAJORITY; i++)
    {
        REQUIRE(resource->promise(promise_msg(boost::lexical_cast<string>(i), RES, 0)));
    }

    accept_f.msg.lease.duration = 0;
    REQUIRE(resource->win(accepted_msg("0", RES, 0, accept_f.msg.lease)));
    REQUIRE(lose_f.calls == 1);
    REQUIRE(win_f.calls == 0);
}

TEST_CASE_METHOD(T_RESCLIENT, "node-res/on-accepted/bad-ballot", "")
{
    resource->start_acquire_lease();
    REQUIRE(resource->reject(reject_msg("0", RES, 0, 0)));
    resource->stop_acquire_lease();
    resource->start_acquire_lease();

    for (int i = 0; i < MAJORITY; i++)
    {
        REQUIRE(resource->promise(promise_msg(boost::lexical_cast<string>(i), RES, 1)));
    }

    REQUIRE(!resource->win(accepted_msg("0", RES, 0, accept_f.msg.lease)));
}
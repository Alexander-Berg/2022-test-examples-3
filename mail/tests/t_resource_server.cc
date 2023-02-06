#include "../src/resource_server.h"
#include <ymod_lease/ymod_lease.h>
#include <sintimers/test.h>
#include <catch.hpp>
using timers::test_timer;
using timers::test_queue;

using namespace ylease;

#define RES "ABC"
#define VALUE "value1"
#define ANOTHER_VALUE "value2"

struct promise_func
{

    promise_func() : calls(0)
    {
    }

    void operator()(const promise_msg& _msg)
    {
        ++calls;
        msg = _msg;
    }
    int calls;
    promise_msg msg;
};

struct reject_func
{

    reject_func() : calls(0)
    {
    }

    void operator()(const reject_msg& _msg)
    {
        ++calls;
        msg = _msg;
    }
    int calls;
    reject_msg msg;
};

struct accepted_func
{

    accepted_func() : calls(0)
    {
    }

    void operator()(const accepted_msg& _msg)
    {
        ++calls;
        msg = _msg;
    }
    int calls;
    accepted_msg msg;
};

struct T_RESSERVER
{
public:
    boost::shared_ptr<test_queue> timers_queue;
    boost::shared_ptr<resource_server> resource;

    T_RESSERVER()
    {
        timers_queue.reset(new test_queue());
        resource.reset(new resource_server("0", RES, *timers_queue, log_function_t()));
    }
    ~T_RESSERVER()
    {
    }
};

TEST_CASE_METHOD(T_RESSERVER, "arbiter-res/create-free", "check is free on create")
{
    REQUIRE(resource->is_free());
}

TEST_CASE_METHOD(T_RESSERVER, "arbiter-res/prepare/single", "check single prepare function call")
{
    promise_func promise_func;
    reject_func reject_func;

    prepare_msg msg(RES, 0);
    resource->prepare(msg, boost::ref(promise_func), boost::ref(reject_func));

    REQUIRE(promise_func.calls == 1);
    REQUIRE(reject_func.calls == 0);
    REQUIRE(promise_func.msg.arbiter_id == "0");
    REQUIRE(promise_func.msg.ballot == 0);
    REQUIRE(promise_func.msg.accepted_ballot == -1);
    REQUIRE(promise_func.msg.resource_name == RES);
}

TEST_CASE_METHOD(T_RESSERVER, "arbiter-res/prepare/twice", "twice prepare must be success")
{
    promise_func promise_func;
    reject_func reject_func;

    resource->prepare(prepare_msg(RES, 0), boost::ref(promise_func), boost::ref(reject_func));

    REQUIRE(promise_func.calls == 1);
    REQUIRE(reject_func.calls == 0);

    resource->prepare(prepare_msg(RES, 0), boost::ref(promise_func), boost::ref(reject_func));

    REQUIRE(promise_func.calls == 2);
    REQUIRE(reject_func.calls == 0);
}

TEST_CASE_METHOD(
    T_RESSERVER,
    "arbiter-res/prepare/grow",
    "prepare for next ballot without any accepts")
{
    promise_func promise_func;
    reject_func reject_func;

    resource->prepare(prepare_msg(RES, 0), boost::ref(promise_func), boost::ref(reject_func));

    REQUIRE(promise_func.calls == 1);
    REQUIRE(reject_func.calls == 0);
    REQUIRE(promise_func.msg.ballot == 0);

    resource->prepare(prepare_msg(RES, 1), boost::ref(promise_func), boost::ref(reject_func));

    REQUIRE(promise_func.calls == 2);
    REQUIRE(reject_func.calls == 0);
    REQUIRE(promise_func.msg.ballot == 1);
}

TEST_CASE_METHOD(T_RESSERVER, "arbiter-res/prepare/reject", "reject earlier ballots")
{
    promise_func promise_func;
    reject_func reject_func;

    resource->prepare(prepare_msg(RES, 54), boost::ref(promise_func), boost::ref(reject_func));
    resource->prepare(prepare_msg(RES, 155), boost::ref(promise_func), boost::ref(reject_func));

    resource->prepare(prepare_msg(RES, 54), boost::ref(promise_func), boost::ref(reject_func));

    REQUIRE(promise_func.calls == 2);
    REQUIRE(reject_func.calls == 1);
    REQUIRE(reject_func.msg.ballot == 54);
    REQUIRE(reject_func.msg.highest_promised == 155);
    REQUIRE(reject_func.msg.resource_name == RES);
}

TEST_CASE_METHOD(T_RESSERVER, "arbiter-res/accept/success", "")
{
    promise_func promise_func;
    reject_func reject_func;
    accepted_func accepted_func;
    const ballot_t BALLOT = 100;

    resource->prepare(prepare_msg(RES, BALLOT), boost::ref(promise_func), boost::ref(reject_func));
    resource->accept(
        accept_msg(RES, BALLOT, lease("node-id", 300, VALUE)),
        boost::ref(accepted_func),
        boost::ref(reject_func));

    REQUIRE(promise_func.calls == 1);
    REQUIRE(reject_func.calls == 0);
    REQUIRE(accepted_func.calls == 1);
    REQUIRE(accepted_func.msg.ballot == BALLOT);
    REQUIRE(accepted_func.msg.lease.duration == 300);
    REQUIRE(accepted_func.msg.lease.node == "node-id");
    REQUIRE(accepted_func.msg.lease.value == VALUE);
    REQUIRE(timers_queue->timers.size() == 1);
    //    REQUIRE(timers_queue->timers.back()->active());
}

TEST_CASE_METHOD(T_RESSERVER, "arbiter-res/accept/ballot-changed", "")
{
    promise_func promise_func;
    reject_func reject_func;
    accepted_func accepted_func;
    const ballot_t BALLOT = 100;

    resource->prepare(prepare_msg(RES, BALLOT), boost::ref(promise_func), boost::ref(reject_func));
    resource->prepare(
        prepare_msg(RES, BALLOT + 1), boost::ref(promise_func), boost::ref(reject_func));
    resource->accept(
        accept_msg(RES, BALLOT, lease("node-id", 300, VALUE)),
        boost::ref(accepted_func),
        boost::ref(reject_func));

    REQUIRE(promise_func.calls == 2);
    REQUIRE(reject_func.calls == 1);
}

TEST_CASE_METHOD(T_RESSERVER, "arbiter-res/accept/reject-concurrent", "")
{
    promise_func promise_func;
    reject_func reject_func;
    accepted_func accepted_func;
    const ballot_t BALLOT = 100;

    resource->prepare(prepare_msg(RES, BALLOT), boost::ref(promise_func), boost::ref(reject_func));
    resource->accept(
        accept_msg(RES, BALLOT, lease("node-id1", 300, VALUE)),
        boost::ref(accepted_func),
        boost::ref(reject_func));
    resource->accept(
        accept_msg(RES, BALLOT, lease("node-id2", 300, VALUE)),
        boost::ref(accepted_func),
        boost::ref(reject_func));

    REQUIRE(reject_func.calls == 1);
    REQUIRE(accepted_func.calls == 1);
    REQUIRE(reject_func.msg.ballot == BALLOT);
    REQUIRE(reject_func.msg.highest_promised == BALLOT + 1);
    REQUIRE(reject_func.msg.resource_name == RES);
}

TEST_CASE_METHOD(T_RESSERVER, "arbiter-res/timer/timeout", "")
{
    promise_func promise_func;
    reject_func reject_func;
    accepted_func accepted_func;
    const ballot_t BALLOT = 100;

    resource->prepare(prepare_msg(RES, BALLOT), boost::ref(promise_func), boost::ref(reject_func));
    resource->accept(
        accept_msg(RES, BALLOT, lease("node-id", 500, VALUE)),
        boost::ref(accepted_func),
        boost::ref(reject_func));
    REQUIRE(resource->is_busy());
    timers_queue->timers.back().second(); // call timer hook
    REQUIRE(resource->is_free());
}

TEST_CASE_METHOD(T_RESSERVER, "arbiter-res/prepare/promise-in-lease-interval", "")
{
    promise_func promise_func;
    reject_func reject_func;
    accepted_func accepted_func;
    const ballot_t BALLOT = 100;

    resource->prepare(prepare_msg(RES, BALLOT), boost::ref(promise_func), boost::ref(reject_func));
    resource->accept(
        accept_msg(RES, BALLOT, lease("node-id", 300, VALUE)),
        boost::ref(accepted_func),
        boost::ref(reject_func));
    resource->prepare(
        prepare_msg(RES, BALLOT + 1), boost::ref(promise_func), boost::ref(reject_func));
    REQUIRE(promise_func.calls == 2);
    REQUIRE(reject_func.calls == 0);
    REQUIRE(promise_func.msg.ballot == BALLOT + 1);
    REQUIRE(promise_func.msg.accepted_ballot == BALLOT);
    REQUIRE(promise_func.msg.lease.node == "node-id");
    REQUIRE(promise_func.msg.lease.value == VALUE);
}

TEST_CASE_METHOD(T_RESSERVER, "arbiter-res/prepare/prepare-again-after-timeout", "")
{
    promise_func promise_func;
    reject_func reject_func;
    accepted_func accepted_func;
    const ballot_t BALLOT = 100;

    resource->prepare(prepare_msg(RES, BALLOT), boost::ref(promise_func), boost::ref(reject_func));
    resource->accept(
        accept_msg(RES, BALLOT, lease("node-id", 300, VALUE)),
        boost::ref(accepted_func),
        boost::ref(reject_func));
    REQUIRE(timers_queue->timers.size());
    timers_queue->timers.back().second();

    resource->prepare(prepare_msg(RES, BALLOT), boost::ref(promise_func), boost::ref(reject_func));
    REQUIRE(promise_func.calls == 1);
    REQUIRE(reject_func.calls == 1);
    REQUIRE(reject_func.msg.highest_promised == BALLOT + 1);
    resource->prepare(
        prepare_msg(RES, BALLOT + 1), boost::ref(promise_func), boost::ref(reject_func));
    REQUIRE(promise_func.calls == 2);
    REQUIRE(reject_func.calls == 1);
    REQUIRE(promise_func.msg.accepted_ballot == -1);
    REQUIRE(promise_func.msg.ballot == BALLOT + 1);
}
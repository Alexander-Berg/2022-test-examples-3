#include "rands.hpp"
#include <multipaxos/multipaxos.h>
#include <multipaxos/acceptor.h>
#include <multipaxos/acceptor_storage_memory.h>
#include <multipaxos/acceptor_storage_bdb.h>
#include <catch.hpp>

#define DB_NAME "bdb"

#define JOURNAL_SIZE 10000
#define SYNC_SIZE 10

using namespace multipaxos;

template <typename T>
struct FakeSender
{
    std::vector<T> msgs;

    void operator()(const T& msg)
    {
        msgs.push_back(msg);
    }

    size_t sentCount()
    {
        return msgs.size();
    }
};

struct T_ACCEPTOR
{
public:
    shared_ptr<::multipaxos::acceptor> acceptor;
    FakeSender<promise_message> promiseSender;
    FakeSender<learn_message> learnSender;
    FakeSender<reject_message> rejectSender;
    FakeSender<sync_response_message> syncResponseSender;

    T_ACCEPTOR()
    {
        acceptor.reset(new ::multipaxos::acceptor(
            std::shared_ptr<acceptor_storage_interface>(
                new acceptor_storage_bdb(DB_NAME, JOURNAL_SIZE, true)),
            "local"));
        //        acceptor.reset(new
        //        ::multipaxos::acceptor(std::shared_ptr<acceptor_storage_interface>(new
        //        acceptor_storage_memory())));
    }
    ~T_ACCEPTOR()
    {
    }
    value_t makeValue()
    {
        return buffer_t::create_from(Rands::getString(16, 16));
    }
};

TEST_CASE_METHOD(T_ACCEPTOR, "acceptor/prepare/first", "")
{
    acceptor->prepare(2, 1, SYNC_SIZE, boost::ref(promiseSender));
    REQUIRE(promiseSender.sentCount() == 1);
    REQUIRE(promiseSender.msgs[0].requested_ballot == 2);
    REQUIRE(promiseSender.msgs[0].acceptor_ballot == 2);
    REQUIRE(promiseSender.msgs[0].requested_slot == 1);
    REQUIRE(promiseSender.msgs[0].accepted_values.items().size() == 0);
}

TEST_CASE_METHOD(T_ACCEPTOR, "acceptor/prepare/repeat-same-ballot", "")
{
    acceptor->prepare(2, 1, SYNC_SIZE, boost::ref(promiseSender));
    acceptor->prepare(2, 1, SYNC_SIZE, boost::ref(promiseSender));
    REQUIRE(promiseSender.sentCount() == 2);
    REQUIRE(promiseSender.msgs[0].acceptor_ballot == 2);
}

TEST_CASE_METHOD(T_ACCEPTOR, "acceptor/prepare/earlier-ballot", "")
{
    acceptor->prepare(5, 1, SYNC_SIZE, boost::ref(promiseSender));
    acceptor->prepare(4, 1, SYNC_SIZE, boost::ref(promiseSender));
    REQUIRE(promiseSender.sentCount() == 2);
    REQUIRE(promiseSender.msgs[1].requested_ballot == 4);
    REQUIRE(promiseSender.msgs[1].acceptor_ballot == 5);
}

TEST_CASE_METHOD(T_ACCEPTOR, "acceptor/prepare/increase-ballot", "")
{
    acceptor->prepare(5, 1, SYNC_SIZE, boost::ref(promiseSender));
    acceptor->prepare(6, 1, SYNC_SIZE, boost::ref(promiseSender));
    REQUIRE(promiseSender.sentCount() == 2);
    REQUIRE(promiseSender.msgs[1].requested_ballot == 6);
    REQUIRE(promiseSender.msgs[1].acceptor_ballot == 6);
}

TEST_CASE_METHOD(T_ACCEPTOR, "acceptor/accept/same-ballot", "")
{
    acceptor->prepare(5, 1, SYNC_SIZE, boost::ref(promiseSender));

    value_t value = makeValue();
    slot_value_pair pair = { 1, value };

    acceptor->accept(5, pair, boost::ref(learnSender), boost::ref(rejectSender));
    REQUIRE(rejectSender.sentCount() == 0);
    REQUIRE(learnSender.sentCount() == 1);
    REQUIRE(learnSender.msgs[0].ballot == 5);
    REQUIRE(learnSender.msgs[0].accepted_value.slot == 1);
    REQUIRE(buffer_equals(learnSender.msgs[0].accepted_value.value, value));
}

TEST_CASE_METHOD(T_ACCEPTOR, "acceptor/accept/new-ballot", "")
{
    acceptor->prepare(5, 1, SYNC_SIZE, boost::ref(promiseSender));

    value_t value = makeValue();
    slot_value_pair pair = { 1, value };
    acceptor->accept(6, pair, boost::ref(learnSender), boost::ref(rejectSender));
    REQUIRE(rejectSender.sentCount() == 0);
    REQUIRE(learnSender.sentCount() == 1);
    REQUIRE(learnSender.msgs[0].ballot == 6);
    REQUIRE(learnSender.msgs[0].accepted_value.slot == 1);
    REQUIRE(buffer_equals(learnSender.msgs[0].accepted_value.value, value));
}

TEST_CASE_METHOD(T_ACCEPTOR, "acceptor/accept/old-ballot", "")
{
    acceptor->prepare(5, 1, SYNC_SIZE, boost::ref(promiseSender));

    slot_value_pair pair = { 1, makeValue() };

    acceptor->accept(4, pair, boost::ref(learnSender), boost::ref(rejectSender));
    REQUIRE(learnSender.sentCount() == 0);
    REQUIRE(rejectSender.sentCount() == 1);
    REQUIRE(rejectSender.msgs[0].request_ballot == 4);
    REQUIRE(rejectSender.msgs[0].acceptor_ballot == 5);
}

TEST_CASE_METHOD(T_ACCEPTOR, "acceptor/accept/rewrite-slot/success", "")
{
    acceptor->prepare(4, 1, SYNC_SIZE, boost::ref(promiseSender));

    slot_value_pair pair = { 1, makeValue() };

    acceptor->accept(4, pair, boost::ref(learnSender), boost::ref(rejectSender));
    acceptor->accept(5, pair, boost::ref(learnSender), boost::ref(rejectSender));
    REQUIRE(learnSender.sentCount() == 2);
    REQUIRE(rejectSender.sentCount() == 0);
}

TEST_CASE_METHOD(
    T_ACCEPTOR,
    "acceptor/prepare/new-leader/log",
    "check all values received in promise to new leader")
{
    acceptor->prepare(5, 1, SYNC_SIZE, boost::ref(promiseSender));

    // simulate random receive order
    slot_value_pair pair1 = { 3, makeValue() };
    slot_value_pair pair2 = { 1, makeValue() };
    slot_value_pair pair3 = { 2, makeValue() };
    acceptor->accept(6, pair1, boost::ref(learnSender), boost::ref(rejectSender));
    acceptor->accept(6, pair2, boost::ref(learnSender), boost::ref(rejectSender));

    // leader changed
    acceptor->prepare(6, 0, SYNC_SIZE, boost::ref(promiseSender));
    REQUIRE(promiseSender.sentCount() == 2);
    REQUIRE(promiseSender.msgs[1].accepted_values.items().size() == 0);

    acceptor->prepare(7, 0, SYNC_SIZE, boost::ref(promiseSender));
    REQUIRE(promiseSender.sentCount() == 3);
    REQUIRE(promiseSender.msgs[2].accepted_values.items().size() == 2);

    acceptor->accept(7, pair3, boost::ref(learnSender), boost::ref(rejectSender));

    acceptor->prepare(8, 0, SYNC_SIZE, boost::ref(promiseSender));
    REQUIRE(promiseSender.sentCount() == 4);
    REQUIRE(promiseSender.msgs[3].accepted_values.items().size() == 3);
}

TEST_CASE_METHOD(T_ACCEPTOR, "acceptor/prepare/leader/full-scenario", "")
{
    acceptor->prepare(5, 1, SYNC_SIZE, boost::ref(promiseSender));

    // simulate random receive order
    slot_value_pair pair1 = { 3, makeValue() };
    slot_value_pair pair2 = { 1, makeValue() };
    slot_value_pair pair3 = { 2, makeValue() };
    acceptor->accept(6, pair1, boost::ref(learnSender), boost::ref(rejectSender));
    acceptor->accept(6, pair2, boost::ref(learnSender), boost::ref(rejectSender));

    // leader changed
    acceptor->prepare(7, 1, SYNC_SIZE, boost::ref(promiseSender));
    REQUIRE(promiseSender.msgs[1].accepted_values.items().size() == 2);

    // need reject all accepts for previous ballot
    acceptor->accept(6, pair3, boost::ref(learnSender), boost::ref(rejectSender));
    REQUIRE(rejectSender.sentCount() == 1);
    REQUIRE(learnSender.sentCount() == 2);
}

TEST_CASE_METHOD(T_ACCEPTOR, "acceptor/prepare/range_end", "")
{
    acceptor->prepare(5, 1, SYNC_SIZE, boost::ref(promiseSender));

    // simulate random receive order
    slot_value_pair pair1 = { 1, makeValue() };
    slot_value_pair pair2 = { 2, makeValue() };
    acceptor->accept(6, pair1, boost::ref(learnSender), boost::ref(rejectSender));
    acceptor->accept(6, pair2, boost::ref(learnSender), boost::ref(rejectSender));

    // leader changed
    acceptor->prepare(7, 1, 2, boost::ref(promiseSender));
    REQUIRE(promiseSender.msgs[1].accepted_values.items().size() == 1);
}

TEST_CASE_METHOD(T_ACCEPTOR, "acceptor/sync/basic", "")
{
    acceptor->prepare(5, 1, SYNC_SIZE, boost::ref(promiseSender));

    // accept some values
    slot_value_pair pair1 = { 3, makeValue() };
    slot_value_pair pair2 = { 1, makeValue() };
    slot_value_pair pair3 = { 2, makeValue() };
    acceptor->accept(6, pair1, boost::ref(learnSender), boost::ref(rejectSender));
    acceptor->accept(6, pair2, boost::ref(learnSender), boost::ref(rejectSender));
    acceptor->accept(6, pair3, boost::ref(learnSender), boost::ref(rejectSender));

    // check sync
    std::set<iid_t> slots;
    slots.insert(1);
    slots.insert(3);
    acceptor->sync(slots, boost::ref(syncResponseSender));
    REQUIRE(syncResponseSender.sentCount() == 1);
    REQUIRE(syncResponseSender.msgs[0].pvalues.items().size() == 2);
    REQUIRE(syncResponseSender.msgs[0].pvalues.items()[0].slot == 1);
    REQUIRE(buffer_equals(syncResponseSender.msgs[0].pvalues.items()[0].value, pair2.value));
    REQUIRE(syncResponseSender.msgs[0].pvalues.items()[1].slot == 3);
    REQUIRE(buffer_equals(syncResponseSender.msgs[0].pvalues.items()[1].value, pair1.value));
}

TEST_CASE_METHOD(T_ACCEPTOR, "acceptor/persistent/save-restore", "")
{
    acceptor->prepare(5, 1, SYNC_SIZE, boost::ref(promiseSender));
    REQUIRE(acceptor->state().current_ballot == 5);

    // accept some values
    slot_value_pair pair1 = { 3, makeValue() };
    slot_value_pair pair2 = { 1, makeValue() };
    slot_value_pair pair3 = { 2, makeValue() };
    acceptor->accept(6, pair1, boost::ref(learnSender), boost::ref(rejectSender));
    REQUIRE(acceptor->state().current_ballot == 6);
    acceptor->accept(6, pair2, boost::ref(learnSender), boost::ref(rejectSender));
    REQUIRE(acceptor->state().current_ballot == 6);
    acceptor->accept(7, pair3, boost::ref(learnSender), boost::ref(rejectSender));
    REQUIRE(acceptor->state().current_ballot == 7);

    REQUIRE(learnSender.sentCount() == 3);

    acceptor.reset();
    acceptor.reset(new ::multipaxos::acceptor(
        std::shared_ptr<acceptor_storage_interface>(
            new acceptor_storage_bdb(DB_NAME, JOURNAL_SIZE, false)),
        "local"));
    REQUIRE(acceptor->state().current_ballot == 7);
    acceptor->prepare(8, 0, SYNC_SIZE, boost::ref(promiseSender));
    REQUIRE(promiseSender.sentCount() == 2);
    REQUIRE(promiseSender.msgs[1].accepted_values.items().size() == 3);
}

TEST_CASE_METHOD(T_ACCEPTOR, "acceptor/values-rotation", "")
{
}

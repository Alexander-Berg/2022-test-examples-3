#include "rands.hpp"
#include <multipaxos/acceptor_storage_bdb.h>
#include <catch.hpp>

#include <stdlib.h>
#include <stdio.h>
#include <memory.h>

using namespace multipaxos;

#define JOURNAL_SIZE 10000
#define TEST_KEY 1234

TEST_CASE("bdb/ported", "")
{
    acceptor_storage_bdb storage("./acceptor_disk_test.bdb", JOURNAL_SIZE, true);
    std::unique_ptr<acceptor_record> to_put, got;

    // Make sure record does not exist
    got = storage.lookup_record(TEST_KEY);
    REQUIRE(!got.get());

    for (int i = 0; i < 10; ++i)
    {
        // Create a record to write
        to_put.reset(new acceptor_record());
        to_put->slot = TEST_KEY;
        to_put->ballot = Rands::getInt(100, 200);
        string strval = Rands::getString(128, 256);
        to_put->value = buffer_t::create_from(strval);

        REQUIRE_NOTHROW(storage.update_record(*to_put));

        got = storage.lookup_record(TEST_KEY);
        REQUIRE(got.get());

        // And compare with original (excluding pointer to value)
        REQUIRE(to_put->slot == got->slot);
        REQUIRE(to_put->ballot == got->ballot);
        REQUIRE(to_put->value.size() == got->value.size());

        REQUIRE(buffer_equals(to_put->value, got->value));

        // Free
        got.reset();

        ///// PART 2
        // Alterate original record
        to_put->ballot = 104;

        // Put again (overwrite)
        REQUIRE_NOTHROW(storage.update_record(*to_put));
    }
}

TEST_CASE("bdb/persistent", "")
{
    string strval = Rands::getString(128, 256);

    {
        acceptor_storage_bdb storage("./acceptor_disk_test.bdb", JOURNAL_SIZE, true);
        std::unique_ptr<acceptor_record> to_put, got;

        // Make sure record does not exist
        got = storage.lookup_record(TEST_KEY);
        REQUIRE(!got.get());
        // Create a record to write
        to_put.reset(new acceptor_record());
        to_put->slot = TEST_KEY;
        to_put->ballot = Rands::getInt(100, 200);
        to_put->value = buffer_t::create_from(strval);

        REQUIRE_NOTHROW(storage.update_record(*to_put));
    }

    {
        acceptor_storage_bdb storage2("./acceptor_disk_test.bdb", JOURNAL_SIZE, false);
        std::unique_ptr<acceptor_record> got;
        // Get
        got = storage2.lookup_record(TEST_KEY);
        REQUIRE(got.get());
        // And compare with original (excluding pointer to value)
        REQUIRE(TEST_KEY == got->slot);
        REQUIRE(strval.size() == got->value.size());
        REQUIRE(
            buffer_equals(got->value, static_cast<const byte_t*>(strval.data()), strval.size()));
    }

    {
        acceptor_storage_bdb storage3("./acceptor_disk_test.bdb", JOURNAL_SIZE, true);
        std::unique_ptr<acceptor_record> got;
        REQUIRE(!storage3.lookup_record(TEST_KEY).get());
    }
}

TEST_CASE("bdb/state", "")
{
    acceptor_storage_bdb storage("./acceptor_disk_test.bdb", JOURNAL_SIZE, true);
    acceptor_state state;
    state.current_ballot = 43;
    state.min_slot = 1;
    state.max_slot = 44;

    storage.update_state(state);

    std::unique_ptr<acceptor_state> state2 = storage.lookup_state();
    REQUIRE(state2.get());
    REQUIRE(state.current_ballot == state2->current_ballot);
    REQUIRE(state.min_slot == state2->min_slot);
    REQUIRE(state.max_slot == state2->max_slot);
}

#include "multipaxos/detail/event_tracker.h"
#include <catch.hpp>

using multipaxos::detail::event_tracker;

TEST_CASE("event_tracker/produce_zero_need_no_action", "")
{
    event_tracker tracker;
    REQUIRE(!tracker.produce(0));
}

TEST_CASE("event_tracker/produce_need_action", "")
{
    event_tracker tracker;
    REQUIRE(tracker.produce(1));
}

TEST_CASE("event_tracker/consume_zero_need_action", "")
{
    event_tracker tracker;
    tracker.produce(1);
    REQUIRE(tracker.consume(0));
}

TEST_CASE("event_tracker/consume_need_no_action", "")
{
    event_tracker tracker;
    tracker.produce(1);
    REQUIRE(!tracker.consume(1));
}

TEST_CASE("event_tracker/double_produce_need_no_action", "")
{
    event_tracker tracker;
    tracker.produce(1);
    REQUIRE(!tracker.produce(1));
}

TEST_CASE("event_tracker/double_produce_and_consume_need_no_action", "")
{
    event_tracker tracker;
    tracker.produce(1);
    tracker.produce(1);
    REQUIRE(tracker.consume(1));
}

TEST_CASE("event_tracker/double_produce_and_double_consume_need_action", "")
{
    event_tracker tracker;
    tracker.produce(1);
    tracker.produce(1);
    REQUIRE(tracker.consume(1));
}

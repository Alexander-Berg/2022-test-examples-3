#include "t_log.h"

#include <multipaxos/frame.h>
#include "../src/common_methods.h"

typedef multipaxos::frame_t frame_t;

struct T_frame
{
    frame_t frame;

    T_frame()
    {
        frame.init(timers::queue_ptr(), 0, 1000, 0);
    }
};

TEST_CASE("frame/initial_size", "")
{
    frame_t frame1;
    frame1.init(timers::queue_ptr(), 0, 4);
    frame_t frame2;
    frame2.init(timers::queue_ptr(), 0, 6);
    frame_t frame3;
    frame3.init(timers::queue_ptr(), 0, 1024);
    frame_t frame4;
    frame4.init(timers::queue_ptr(), 0, 1023);
    frame_t frame5;
    frame5.init(timers::queue_ptr(), 0, 32700);

    REQUIRE(frame1.preallocated_slots_count() == 4);
    REQUIRE(frame2.preallocated_slots_count() == 8);
    REQUIRE(frame3.preallocated_slots_count() == 1024);
    REQUIRE(frame4.preallocated_slots_count() == 1024);
    REQUIRE(frame5.preallocated_slots_count() == 32768);
}

TEST_CASE("frame/initial_zones", "")
{
    frame_t frame;
    frame.init(timers::queue_ptr(), 0, 1000, 0);
    SECTION("read_zone_empty", "")
    {
        REQUIRE(frame.read_zone_size() == 0);
        REQUIRE(frame.read_zone_begin() == frame.read_zone_end());
    }
    SECTION("write_zone_full", "")
    {
        REQUIRE(frame.write_zone_size() == 1024);
        REQUIRE(frame.write_zone_begin() == frame.read_zone_end());
    }
}

TEST_CASE("frame/extend_read_zone", "")
{
    frame_t frame;
    frame.init(timers::queue_ptr(), 0, 1000, 0);
    for (auto i = 0; i < 1024; ++i)
    {
        frame.get_slot(i).init(i, 1, state_t::committed, value_t());
    }
    SECTION("extend_0", "")
    {
        frame.extend_read_zone(0);
        REQUIRE(frame.read_zone_begin() == 0);
        REQUIRE(frame.write_zone_begin() == 0);
    }
    SECTION("extend_1_correct_read_zone", "")
    {
        frame.extend_read_zone(1);
        REQUIRE(frame.read_zone_begin() == 0);
        REQUIRE(frame.read_zone_end() == 1);
    }
    SECTION("extend_1_correct_write_zone", "")
    {
        frame.extend_read_zone(1);
        REQUIRE(frame.write_zone_begin() == 1);
        REQUIRE(frame.write_zone_end() == 1024);
    }
    SECTION("extend_full_size_correct_read_zone", "")
    {
        frame.extend_read_zone(1024);
        REQUIRE(frame.read_zone_begin() == 0);
        REQUIRE(frame.read_zone_end() == 1024);
    }
    SECTION("extend_full_size_empty_write_zone", "")
    {
        frame.extend_read_zone(1024);
        REQUIRE(frame.write_zone_begin() == 1024);
        REQUIRE(frame.write_zone_end() == 1024);
        REQUIRE(frame.write_zone_size() == 0);
    }
}

TEST_CASE("frame/cut_read_zone", "")
{
    frame_t frame;
    frame.init(timers::queue_ptr(), 0, 1000, 0);
    for (auto i = 0; i < 1024; ++i)
    {
        frame.get_slot(i).init(i, 1, state_t::committed, value_t());
    }
    frame.extend_read_zone(1024);

    SECTION("cut_0_correct_read_zone", "")
    {
        frame.cut_read_zone(0);
        REQUIRE(frame.read_zone_begin() == 0);
    }
    SECTION("cut_1000_correct_write_zone", "")
    {
        frame.cut_read_zone(1000);
        REQUIRE(frame.write_zone_begin() == 1024);
        REQUIRE(frame.write_zone_end() == 2024);
    }
    SECTION("cut_1024_correct_read_zone", "")
    {
        frame.cut_read_zone(1024);
        REQUIRE(frame.read_zone_begin() == 1024);
        REQUIRE(frame.read_zone_end() == 1024);
    }
    SECTION("cut_1024_correct_write_zone", "")
    {
        frame.cut_read_zone(1024);
        REQUIRE(frame.write_zone_begin() == 1024);
        REQUIRE(frame.write_zone_end() == 2048);
    }
}

#define CATCH_CONFIG_MAIN
#include "window.h"
#include <catch.hpp>

using namespace pipeline;

TEST_CASE("window/ctor", "")
{
    const std::size_t max_size = 5;
    Window win(0, max_size);

    REQUIRE(win.begin_id() == 0);
    REQUIRE(win.end_id() == 0);
    REQUIRE(win.next_interval_available() == false);
    REQUIRE(win.next_interval() == win.next_interval());
    REQUIRE(win.next_interval().first == 0);
    REQUIRE(win.next_interval().second == 0);
}

TEST_CASE("window/begin_id", "")
{
    const std::size_t max_size = 5;
    Window win(0, max_size);

    for (unsigned int num : {5, 7, 10}) {
        win.begin_id(num);
        REQUIRE(win.begin_id() == num);
        REQUIRE(win.end_id() == num);
    }
}

TEST_CASE("window/end_id", "")
{
    const unsigned int max_size = 5;
    Window win(0, max_size);

    for (unsigned int num : {2, 3, 7, 10}) {
        win.end_id(num, 0);
        REQUIRE(win.begin_id() == 0);
        REQUIRE(win.end_id() == std::min(num, max_size));
    }
}

TEST_CASE("window/end_id", "with exclude")
{
    const unsigned int max_size = 5;
    const size_t exclude = 5;
    Window win(0, max_size);

    for (unsigned int num : {2, 3, 7, 10}) {
        win.end_id(num, std::min(win.total_size(), exclude));
        REQUIRE(win.begin_id() == 0);
        REQUIRE(win.end_id() == num);
    }
}

TEST_CASE("window/end_id", "with exclude")
{
    const unsigned int max_size = 5;
    const size_t exclude = 5;
    Window win(0, max_size);

    for (unsigned int num : {2, 3, 7, 10}) {
        win.end_id(num, std::min(win.total_size(), exclude));
        REQUIRE(win.begin_id() == 0);
        REQUIRE(win.end_id() == num);
    }
    win.end_id(20, exclude);
    REQUIRE(win.begin_id() == 0);
    REQUIRE(win.end_id() == 10);
}

TEST_CASE("window/next_interval", "")
{
    const unsigned int max_size = 10;
    Window win(0, max_size);

    unsigned int prev = 0;
    for (unsigned int num : {2, 3, 7, 10}) {
        win.end_id(num, 0);
        REQUIRE(win.next_interval_available());
        auto interval = win.next_interval();
        REQUIRE(interval.first == prev);
        REQUIRE(interval.second == num);
        REQUIRE(!win.next_interval_available());
        prev = num;
    }
}

TEST_CASE("window/next_interval/1", "with exclude")
{
    const unsigned int max_size = 5;
    Window win(0, max_size);

    win.end_id(5, 0);
    win.next_interval();

    win.end_id(7, 5);
    auto interval = win.next_interval();
    REQUIRE(interval.first == 5);
    REQUIRE(interval.second == 7);
}
//
//WINDOW interval=[80110,80110) new_data.size=614 impl.begin=77189 impl.end=87063 total_commited=2821
//
//TEST_CASE("window/next_interval/2", "with exclude")
//{
//    const unsigned int max_size = 100;
//    Window win(0, max_size);
//
//    win.begin_id(77189);
//    win.end_id(87063, 2821);
//
//    auto interval = win.next_interval();
//    REQUIRE(interval.first == 5);
//    REQUIRE(interval.second == 7);
//}

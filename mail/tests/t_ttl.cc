#include <catch.hpp>
#include <yxiva/core/message.h>

using namespace yxiva;

TEST_CASE("ttl/effective_ttl")
{
    for (ttl_t i = 0; i < 5; ++i)
    {
        CHECK(effective_ttl(i) == 0);
    }
    for (ttl_t i = 5; i < 10; ++i)
    {
        CHECK(effective_ttl(i) == i);
    }
}

TEST_CASE("ttl/remaining_ttl")
{
    message msg;
    auto now = std::time(nullptr);
    msg.event_ts = now - 1;
    for (ttl_t i = 0; i < 6; ++i)
    {
        msg.ttl = i;
        CHECK(remaining_ttl(msg, now) == 0);
    }
    for (ttl_t i = 6; i < 10; ++i)
    {
        msg.ttl = i;
        CHECK(remaining_ttl(msg, now) == i - 1);
    }
    msg.event_ts = now + 1;
    for (ttl_t i = 0; i < 10; ++i)
    {
        msg.ttl = i;
        CHECK(remaining_ttl(msg, now) <= i);
    }
}

TEST_CASE("ttl/packet_ttl")
{
    message msg;
    sub_t sub;
    auto now = std::time(nullptr);
    msg.event_ts = now - 1;
    for (ttl_t i = 0; i < 6; ++i)
    {
        msg.ttl = i;
        CHECK(packet(nullptr, msg, sub, now).ttl == 0);
    }
    for (ttl_t i = 6; i < 10; ++i)
    {
        msg.ttl = i;
        CHECK(packet(nullptr, msg, sub, now).ttl == i - 1);
    }
}

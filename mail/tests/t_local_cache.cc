#include <local_cache.h>
#include "catch.hpp"
#include <thread>

using namespace rcache;

TEST_CASE("should store value")
{
    local_cache cache(time_traits::seconds(30));
    cache.set("key", cache_entry("value", time_traits::seconds(30)));
    auto entry = cache.get("key");
    REQUIRE(entry.is_initialized());
    REQUIRE(entry->value == "value");
}

TEST_CASE("should return empty entry if not contains it")
{
    local_cache cache(time_traits::seconds(30));
    auto entry = cache.get("key");
    REQUIRE(!entry.is_initialized());
}

TEST_CASE("should reset value after ttl")
{
    local_cache cache(time_traits::seconds(30));
    cache.set("key", cache_entry("value", time_traits::seconds(0)));
    std::this_thread::sleep_for(time_traits::milliseconds(1));
    auto entry = cache.get("key");
    REQUIRE(!entry.is_initialized());
}

TEST_CASE("should clear expired values")
{
    local_cache cache(time_traits::milliseconds(1));
    cache.set("key1", cache_entry("value1", time_traits::seconds(30)));
    cache.set("key2", cache_entry("value2", time_traits::milliseconds(1)));
    REQUIRE(cache.size() == 2);
    std::this_thread::sleep_for(time_traits::milliseconds(2));
    REQUIRE(cache.size() == 1);
    REQUIRE(cache.get("key1").is_initialized());
    REQUIRE(!cache.get("key2").is_initialized());
}
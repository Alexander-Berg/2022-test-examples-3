#include <yplatform/algorithm/lru_cache.h>
#include <catch.hpp>
#include <boost/utility.hpp>

using namespace yplatform;

using cache = lru_cache<std::string, int>;
const int cache_capacity = 10;
const int cache_value = 100;

struct non_copyable_value
{
public:
    non_copyable_value() = default;
    non_copyable_value(non_copyable_value&&) = default;
    non_copyable_value& operator=(non_copyable_value&&) = default;

    non_copyable_value(const non_copyable_value&) = delete;
    non_copyable_value& operator=(const non_copyable_value&) = delete;
};

TEST_CASE("lru_cache/get/not_existed")
{
    cache c(cache_capacity);
    auto val = c.get("key");
    REQUIRE(!val.has_value());
}

TEST_CASE("lru_cache/get/returns_put_value")
{
    cache c(cache_capacity);
    c.put("key", cache_value);
    REQUIRE(c.get("key") == cache_value);
}

TEST_CASE("lru_cache/put/updates_existing_value")
{
    cache c(cache_capacity);
    c.put("key", cache_value);
    c.put("key", cache_value * 2);
    REQUIRE(c.get("key") == cache_value * 2);
}

TEST_CASE("lru_cache/put/removes_old_values")
{
    cache c(2);
    c.put("key1", cache_value);
    c.put("key2", cache_value * 2);
    c.put("key3", cache_value * 3);

    REQUIRE(c.get("key3") == cache_value * 3);
    REQUIRE(c.get("key2") == cache_value * 2);
    REQUIRE(!c.get("key1"));
}

TEST_CASE("lru_cache/put/dont_remove_recently_used")
{
    cache c(2);
    c.put("key1", cache_value);
    c.put("key2", cache_value * 2);
    c.get("key1");
    c.put("key3", cache_value * 3);

    REQUIRE(c.get("key3") == cache_value * 3);
    REQUIRE(!c.get("key2"));
    REQUIRE(c.get("key1") == cache_value);
}

TEST_CASE("lru_cache/put/dont_copy")
{
    using cache_with_non_copyable_value = lru_cache<std::string, non_copyable_value>;
    cache_with_non_copyable_value cache(10);
    cache.put("key", non_copyable_value{});
}

TEST_CASE("lru_cache/custom_capacity_functor/dont_stores_big_elements")
{
    auto get_capacity = [](std::string /*key*/, size_t val) { return val; };
    using cache_with_custom_capacity = lru_cache<std::string, int, decltype(get_capacity)>;
    cache_with_custom_capacity cache(10, get_capacity);

    cache.put("key", 11);
    REQUIRE(!cache.get("key"));
}

TEST_CASE("lru_cache/custom_capacity_functor/removes_on_capacity_exceeded")
{
    auto get_capacity = [](std::string /*key*/, size_t val) { return val; };
    using cache_with_custom_capacity = lru_cache<std::string, int, decltype(get_capacity)>;
    cache_with_custom_capacity cache(10, get_capacity);

    cache.put("key", 5);
    cache.put("key2", 6);
    REQUIRE(!cache.get("key"));
    REQUIRE(cache.get("key2") == 6);
}

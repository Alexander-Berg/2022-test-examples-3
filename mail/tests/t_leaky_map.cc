#include <yplatform/algorithm/leaky_map.h>
#include <catch.hpp>
#include <thread>

namespace yplatform {

TEST_CASE("leaky_map/leak")
{
    leaky_map map(1, 100);
    map.add("", 1);
    REQUIRE(map.get("") == 1);
    std::this_thread::sleep_for(time_traits::milliseconds(50));
    REQUIRE(map.get("") == 1);
    std::this_thread::sleep_for(time_traits::milliseconds(50));
    REQUIRE(map.get("") == 0);
}

TEST_CASE("leaky_map/keys")
{
    leaky_map map(1, 100);
    map.add("a", 1);
    REQUIRE(map.get("a") == 1);
    REQUIRE(map.get("b") == 0);
    map.add("b", 1);
    REQUIRE(map.get("a") == 1);
    REQUIRE(map.get("b") == 1);
    map.add("a", 1);
    REQUIRE(map.get("a") == 2);
    REQUIRE(map.get("b") == 1);
}

TEST_CASE("leaky_map/cleanup")
{
    leaky_map map(1, 1, 100);
    map.add("a", 1);
    map.add("b", 1);
    map.add("c", 1);
    REQUIRE(map.size() == 3);
    std::this_thread::sleep_for(time_traits::milliseconds(100));
    map.add("d", 1);
    REQUIRE(map.size() == 1);
}

}

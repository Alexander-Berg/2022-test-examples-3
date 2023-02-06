#include <yplatform/algorithm/leaky_bucket.h>
#include <catch.hpp>
#include <thread>

namespace yplatform {

TEST_CASE("leaky_bucket/leak")
{
    leaky_bucket bucket(1, 100);
    bucket.add(1);
    REQUIRE(bucket.get() == 1);
    std::this_thread::sleep_for(time_traits::milliseconds(50));
    REQUIRE(bucket.get() == 1);
    std::this_thread::sleep_for(time_traits::milliseconds(50));
    REQUIRE(bucket.get() == 0);
}

TEST_CASE("leaky_bucket/zero_leak_interval")
{
    leaky_bucket bucket(1, 0);
    bucket.add(1);
    REQUIRE(bucket.get() == 1);
    std::this_thread::sleep_for(time_traits::milliseconds(50));
    REQUIRE(bucket.get() == 1);
}

TEST_CASE("leaky_bucket/zero_leak_factor")
{
    leaky_bucket bucket(0, 1);
    bucket.add(1);
    REQUIRE(bucket.get() == 1);
    std::this_thread::sleep_for(time_traits::milliseconds(50));
    REQUIRE(bucket.get() == 1);
}

}

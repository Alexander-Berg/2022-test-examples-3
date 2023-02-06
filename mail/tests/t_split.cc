#include <yplatform/util/split.h>
#include <catch.hpp>
#include <vector>
#include <set>

namespace util = yplatform::util;

TEST_CASE("split/no_empties", "")
{
    std::string raw = "1,2,3,4,5,6,7,8,9";
    std::vector<std::string> target;
    util::split(target, raw, ",");
    for (int i = 1; i <= 9; ++i)
    {
        REQUIRE(target[i - 1] == std::to_string(i));
    }
    REQUIRE(target.size() == 9);
}

TEST_CASE("split/empties_in_the_middle", "")
{
    std::string raw = "1,2,,3,,4,5,,,,6,7,8,,,9";
    std::vector<std::string> target;
    util::split(target, raw, ",");
    REQUIRE(target.size() == 9);
}

TEST_CASE("split/empties_at_the_end", "")
{
    std::string raw = "1,2,,3,4,5,6,7,8,9,,,";
    std::vector<std::string> target;
    util::split(target, raw, ",");
    REQUIRE(target.size() == 9);
}

TEST_CASE("split/empties_at_the_beginning", "")
{
    std::string raw = ",,,1,2,,3,4,5,6,7,8,9";
    std::vector<std::string> target;
    util::split(target, raw, ",");
    REQUIRE(target.size() == 9);
}

TEST_CASE("split/set", "")
{
    std::string raw = ",,,1,2,,3,4,5,6,7,,8,9,";
    std::set<std::string> target;
    util::split(target, raw, ",");
    REQUIRE(target.size() == 9);
}

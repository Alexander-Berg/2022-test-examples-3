#include <yplatform/util/per_second_accumulator.h>

#include <catch.hpp>

namespace yplatform {

TEST_CASE("yplatform/per_second_accumulator/sequently", "sequently")
{
    per_second_accumulator<int> acc(5);
    acc.add(44, 16);
    acc.add(45, 2);
    acc.add(46, 2);
    acc.add(47, 4);
    REQUIRE(acc.get_last(44) == 0);
    REQUIRE(acc.get_last(45) == 16);
    REQUIRE(acc.get_last(46) == 2);
    REQUIRE(acc.get_last(47) == 2);
    REQUIRE(acc.get_last(48) == 4);
    REQUIRE(acc.get_avg(48, 1) == 4);
    REQUIRE(acc.get_avg(48, 2) == 3);
    REQUIRE(acc.get_avg(48, 4) == 6);
    acc.add(107, 99);
    acc.add(108, 77);
    REQUIRE(acc.get_current(104) == 0);
    REQUIRE(acc.get_current(105) == 0);
    REQUIRE(acc.get_current(106) == 0);
    REQUIRE(acc.get_current(107) == 99);
    REQUIRE(acc.get_current(108) == 77);
}

TEST_CASE("yplatform/per_second_accumulator/circular-check", "")
{
    per_second_accumulator<int> acc(5);
    acc.add(4, 1);
    acc.add(44, 3);

    for (auto i = 50; i < 60; ++i)
    {
        REQUIRE(acc.get_last(i) == 0);
    }
}

TEST_CASE("yplatform/per_second_accumulator/max_element_check", "")
{
    per_second_accumulator<int> acc(7);
    acc.add(1, 7);
    acc.add(2, 3);
    acc.add(3, 8);
    acc.add(4, 5);
    acc.add(5, 0);
    acc.add(6, 4);
    acc.add(7, 2);
    REQUIRE(acc.get_max(7, 7) == 8);
}

}

#include <yplatform/ptree.h>
#include <catch.hpp>
#include <iostream>

using ptree = yplatform::ptree;
using yplatform::read_ptree;

template <typename T>
ptree build_ptree_array(const std::string& key, const T& data)
{
    ptree node;
    for (auto&& val : data)
    {
        auto& child_node = node.add_child(key, ptree());
        child_node.put_value(val);
    }
    return node;
}

template <typename T>
ptree build_ptree_map(const std::string& key, const T& data)
{
    ptree node;
    auto& map = node.put_child(key, ptree());
    for (auto&& [key, val] : data)
    {
        map.put(key, val);
    }
    return node;
}

std::vector<int> test_array = { 1, 2, 10, 20, 37 };
yplatform::ptree test_ptree_with_array = build_ptree_array("array", test_array);

std::map<std::string, int> test_map = { { "one", 1 },
                                        { "two", 2 },
                                        { "ten", 10 },
                                        { "twenty", 20 } };
yplatform::ptree test_ptree_with_map = build_ptree_map("map", test_map);

TEST_CASE("ptree/get_duration/success")
{
    yplatform::ptree conf;
    conf.put("t", "30.300");
    auto duration = conf.get<yplatform::time_traits::duration>("t");
    REQUIRE(duration == yplatform::time_traits::milliseconds(30300));
}

TEST_CASE("ptree/get_duration/with_suffixes")
{
    yplatform::ptree conf;
    conf.put("t", "1.5h");
    REQUIRE(conf.get<yplatform::time_traits::duration>("t") == yplatform::time_traits::minutes(90));
    conf.put("t", "1.5m");
    REQUIRE(conf.get<yplatform::time_traits::duration>("t") == yplatform::time_traits::seconds(90));
    conf.put("t", "1.5s");
    REQUIRE(
        conf.get<yplatform::time_traits::duration>("t") ==
        yplatform::time_traits::milliseconds(1500));
    conf.put("t", "1.5ms");
    REQUIRE(
        conf.get<yplatform::time_traits::duration>("t") ==
        yplatform::time_traits::microseconds(1500));
    conf.put("t", "1.5us");
    REQUIRE(
        conf.get<yplatform::time_traits::duration>("t") ==
        yplatform::time_traits::nanoseconds(1500));
}

TEST_CASE("ptree/get_duration/boost_style")
{
    yplatform::ptree conf;
    conf.put("t", "00:00:12");
    REQUIRE(conf.get<yplatform::time_traits::duration>("t") == yplatform::time_traits::seconds(12));
    conf.put("t", "00:01:10");
    REQUIRE(conf.get<yplatform::time_traits::duration>("t") == yplatform::time_traits::seconds(70));
    conf.put("t", "11:11:11");
    REQUIRE(
        conf.get<yplatform::time_traits::duration>("t") == yplatform::time_traits::seconds(40271));
}

TEST_CASE("ptree/put_duration/success")
{
    yplatform::ptree conf;
    yplatform::time_traits::duration duration = yplatform::time_traits::milliseconds(30300);
    conf.put("t", duration);
    REQUIRE(conf.get<std::string>("t") == "30.300000");
}

TEST_CASE("ptree/get_duration/throws_if_bad_value")
{
    yplatform::ptree conf;
    conf.put("t", "abc");
    REQUIRE_THROWS(conf.get<yplatform::time_traits::duration>("t"));
    conf.put("t", "");
    REQUIRE_THROWS(conf.get<yplatform::time_traits::duration>("t"));
}

TEST_CASE("ptree/get_duration/throws_if_bad_suffix")
{
    yplatform::ptree conf;
    conf.put("t", "30d");
    REQUIRE_THROWS(conf.get<yplatform::time_traits::duration>("t"));
}

TEST_CASE("ptree/get_duration/throws_if_bad_boost_styled")
{
    yplatform::ptree conf;
    conf.put("t", "00:ab:00");
    REQUIRE_THROWS(conf.get<yplatform::time_traits::duration>("t"));
}

TEST_CASE("ptree/get_bool/true_if_value__yes_on_1_true")
{
    yplatform::ptree conf;
    conf.put("t", "true");
    REQUIRE(conf.get<bool>("t") == true);
    conf.put("t", "on");
    REQUIRE(conf.get<bool>("t") == true);
    conf.put("t", "1");
    REQUIRE(conf.get<bool>("t") == true);
    conf.put("t", "yes");
    REQUIRE(conf.get<bool>("t") == true);
}

TEST_CASE("ptree/get_bool/false_if_value__no_off_0_false")
{
    yplatform::ptree conf;
    conf.put("t", "false");
    REQUIRE(conf.get<bool>("t") == false);
    conf.put("t", "off");
    REQUIRE(conf.get<bool>("t") == false);
    conf.put("t", "no");
    REQUIRE(conf.get<bool>("t") == false);
    conf.put("t", "0");
    REQUIRE(conf.get<bool>("t") == false);
}

TEST_CASE("ptree/get_bool/throws_if_bad_or_empry_str")
{
    yplatform::ptree conf;
    conf.put("t", "");
    REQUIRE_THROWS(conf.get<bool>("t"));
    conf.put("t", "a");
    REQUIRE_THROWS(conf.get<bool>("t"));
    conf.put("t", "7");
    REQUIRE_THROWS(conf.get<bool>("t"));
}

TEST_CASE("ptree/read_ptree/container_type")
{
    std::vector<int> actual;
    read_ptree(actual, test_ptree_with_array, "array");
    REQUIRE(actual == test_array);
}

TEST_CASE("ptree/read_ptree/map_type")
{
    std::map<std::string, int> actual;
    read_ptree(actual, test_ptree_with_map, "map");
    REQUIRE(actual == test_map);
}

TEST_CASE("ptree/read_ptree/value_type")
{
    yplatform::ptree conf;
    conf.put("duration", "30.300");
    yplatform::time_traits::duration res;
    read_ptree(res, conf, "duration");
    REQUIRE(res == yplatform::time_traits::milliseconds(30300));
}

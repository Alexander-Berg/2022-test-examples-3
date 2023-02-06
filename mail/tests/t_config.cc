#include <yplatform/application.h>
#include <catch.hpp>
#include <algorithm>
#include <string>

TEST_CASE("config/multi_level_include")
{
    yplatform::configuration conf;
    conf.load_from_file("config.yml");

    REQUIRE(conf.modules().size() == 1);
    REQUIRE(conf.modules().front().options.count("options") == 1);
    auto& options = conf.modules().front().options.get_child("options");
    // Config values.
    CHECK(options.get("key1", std::string{}) == "val1");
    CHECK(options.get("key2", 0) == 123);
    // Include values.
    CHECK(options.get("key3", std::string{}) == "val2");
    CHECK(options.get("key4", 0) == 123123);

    REQUIRE(options.count("section") == 1);
    auto section = options.get_child("section");
    // Config values.
    CHECK(section.get("key5", std::string{}) == "xxx");
    CHECK(section.get("key6", 0) == 456);
    // Include values.
    CHECK(section.get("key7", std::string{}) == "val5");
    CHECK(section.get("key8", 0) == 456456);
}

TEST_CASE("config/key_with_dot")
{
    yplatform::configuration conf;
    conf.load_from_file("config.yml");

    REQUIRE(conf.modules().size() == 1);
    REQUIRE(conf.modules().front().options.count("options") == 1);
    auto& options = conf.modules().front().options.get_child("options");

    // Not using ptree::get() because it also uses dot delimited path.
    CHECK(std::count_if(options.begin(), options.end(), [](auto p) {
              return p.first == "key.containing.dot" &&
                  p.second.get_value(std::string{}) == "value";
          }) == 1);
}

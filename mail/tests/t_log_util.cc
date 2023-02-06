#include "src/log_util.h"
#include <catch.hpp>

using ymod_webserver::replace_column_name_chars;

TEST_CASE("log_util/", "")
{
    REQUIRE("" == replace_column_name_chars(""));
    REQUIRE("" == replace_column_name_chars("_"));
    REQUIRE("a" == replace_column_name_chars("a"));
    REQUIRE("a" == replace_column_name_chars("_a"));
    REQUIRE("a" == replace_column_name_chars("_0a"));
    REQUIRE("a" == replace_column_name_chars("0_a"));
    REQUIRE("a_" == replace_column_name_chars("0a_"));
    REQUIRE("a_b_0" == replace_column_name_chars("a_b_0"));
    REQUIRE("a_b_0" == replace_column_name_chars("a_b_%0"));
    REQUIRE("a_b" == replace_column_name_chars("a__b"));
    REQUIRE("a_" == replace_column_name_chars("a__"));
    REQUIRE("a_b" == replace_column_name_chars("a_%_b"));
    REQUIRE("a" == replace_column_name_chars("%a"));
    REQUIRE("a" == replace_column_name_chars("%_a"));
}

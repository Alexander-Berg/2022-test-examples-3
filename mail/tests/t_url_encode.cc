#include <catch.hpp>
#include <ymod_httpclient/url_encode.h>

using ymod_httpclient::url_encode;

TEST_CASE("url_encode/empty", "")
{
    REQUIRE(url_encode({}) == "");
}

TEST_CASE("url_encode/single", "")
{
    REQUIRE(url_encode({ { "abc", "def" } }) == "?abc=def");
    REQUIRE(url_encode({ { "abc", "def@#$q\\das" } }) == "?abc=def%40%23%24q%5cdas");
}

TEST_CASE("url_encode/multi", "")
{
    REQUIRE(
        url_encode({ { "text", "def" }, { "text_second", "jkl" } }) == "?text=def&text_second=jkl");
    REQUIRE(
        url_encode({ { "text", "abcde&f" }, { "text_second", "j^@kl" } }) ==
        "?text=abcde%26f&text_second=j%5e%40kl");
    REQUIRE(url_encode({ { "text", "abc&f" }, { "num", 1 } }) == "?text=abc%26f&num=1");
    REQUIRE(url_encode({ { "text", "" }, { "num", 1 } }) == "?text=&num=1");
}

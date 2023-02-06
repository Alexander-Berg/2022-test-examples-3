#include "protect_url_params.h"
#include <catch.hpp>

using namespace ymod_webserver;

const std::vector<string> protected_params{ "secret", "sign" };

TEST_CASE("protect_url_params/empty_url")
{
    string url = "";
    protect_url_params(url, protected_params);
    REQUIRE(url == "");
}

TEST_CASE("protect_url_params/empty_protected_value")
{
    string url;

    url = "&sign=";
    protect_url_params(url, protected_params);
    REQUIRE(url == "&sign=");

    url = "?sign=";
    protect_url_params(url, protected_params);
    REQUIRE(url == "?sign=");

    url = "&secret=&";
    protect_url_params(url, protected_params);
    REQUIRE(url == "&secret=&");

    url = "?sign=&secret=&";
    protect_url_params(url, protected_params);
    REQUIRE(url == "?sign=&secret=&");

    url = "?sign=&secret=#";
    protect_url_params(url, protected_params);
    REQUIRE(url == "?sign=&secret=#");
}

TEST_CASE("protect_url_params/protected_param_as_part_of_domain")
{
    string url;

    url = "www.secret.ru?";
    protect_url_params(url, protected_params);
    REQUIRE(url == "www.secret.ru?");

    url = "secret.ya.ru";
    protect_url_params(url, protected_params);
    REQUIRE(url == "secret.ya.ru");

    url = "www.secret.sign/";
    protect_url_params(url, protected_params);
    REQUIRE(url == "www.secret.sign/");
}

TEST_CASE("protect_url_params/protected_param_as_part_of_another_param")
{
    string url;

    url = "?sign_other=value&awesome_secret=abc#";
    protect_url_params(url, protected_params);
    REQUIRE(url == "?sign_other=value&awesome_secret=abc#");

    url = "?some_secret=value";
    protect_url_params(url, protected_params);
    REQUIRE(url == "?some_secret=value");

    url = "?secret_sign=value&";
    protect_url_params(url, protected_params);
    REQUIRE(url == "?secret_sign=value&");
}

TEST_CASE("protect_url_params/same_value_as_protected_param")
{
    string url = "?param1=secret&param2=sign";
    protect_url_params(url, protected_params);
    REQUIRE(url == "?param1=secret&param2=sign");
}

TEST_CASE("protect_url_params/protect_when_first")
{
    string url = "?secret=abcdef&param2=value";
    protect_url_params(url, protected_params);
    REQUIRE(url == "?secret=xxxxxx&param2=value");
}

TEST_CASE("protect_url_params/protect_when_last")
{
    string url = "?param=value&secret=abcdef";
    protect_url_params(url, protected_params);
    REQUIRE(url == "?param=value&secret=xxxxxx");
}

TEST_CASE("protect_url_params/protect_when_in_the_middle")
{
    string url = "?param=value&secret=abcdef&param2=value";
    protect_url_params(url, protected_params);
    REQUIRE(url == "?param=value&secret=xxxxxx&param2=value");
}

TEST_CASE("protect_url_params/protect_whith_anchor")
{
    string url = "?secret=abcdef#anchor";
    protect_url_params(url, protected_params);
    REQUIRE(url == "?secret=xxxxxx#anchor");
}

TEST_CASE("protect_url_params/protect_all_occurences")
{
    string url = "?secret=abcdef&param=value&secret=xyz";
    protect_url_params(url, protected_params);
    REQUIRE(url == "?secret=xxxxxx&param=value&secret=xxx");
}

TEST_CASE("protect_url_params/protect_all_protected")
{
    string url = "?secret=abcdef&sign=xyz&param=value";
    protect_url_params(url, protected_params);
    REQUIRE(url == "?secret=xxxxxx&sign=xxx&param=value");
}

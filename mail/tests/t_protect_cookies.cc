#include "protect_cookies.h"
#include <catch.hpp>
#include <iostream>

using namespace ymod_webserver;

TEST_CASE("protect_cookies/empty_string")
{
    std::string empty;
    protect_sensitive_cookies(empty);
    REQUIRE(empty.empty());
}

static void validate_cookie_protected(const std::string& cookie_name)
{
    const std::string not_replaced = cookie_name + "=this.is.not.replaced.";
    const std::string replaced = "but_this_is";

    std::string cookies = not_replaced + replaced;
    const std::size_t original_cookies_size = cookies.size();

    protect_sensitive_cookies(cookies);

    REQUIRE(cookies.size() == original_cookies_size);

    std::string actual_not_replaced = cookies.substr(0, not_replaced.size());
    std::string actual_replaced = cookies.substr(not_replaced.size(), replaced.size());

    REQUIRE(actual_not_replaced == not_replaced);
    REQUIRE(actual_replaced != replaced);
}

static void validate_replacement_in_the_middle(
    const std::string& original,
    const std::string& actual,
    std::size_t replaced_offset,
    std::size_t replaced_size)
{
    std::string original_1 = original.substr(0, replaced_offset);
    std::string actual_1 = actual.substr(0, replaced_offset);

    REQUIRE(original_1 == actual_1);

    std::string original_2 = original.substr(replaced_offset, replaced_size);
    std::string actual_2 = actual.substr(replaced_offset, replaced_size);

    REQUIRE(original_2 != actual_2);

    std::string original_3 = original.substr(replaced_offset + replaced_size);
    std::string actual_3 = actual.substr(replaced_offset + replaced_size);

    REQUIRE(original_3 == actual_3);
}

TEST_CASE("protect_cookies/sensitive_cookies_are_protected_regardless_of_case", "")
{
    validate_cookie_protected("session_id");
    validate_cookie_protected("sessionid2");
    validate_cookie_protected("secure_session_id");
    validate_cookie_protected("golem_session");

    validate_cookie_protected("SESSION_ID");
    validate_cookie_protected("Sessionid2");

    validate_cookie_protected("prefix_session_id");
    validate_cookie_protected("session_id_postfix");
    validate_cookie_protected("prefix_session_id_postfix");
}

TEST_CASE("protect_cookies/other_cookies_are_not_protected", "")
{
    const std::string original_cookies = "my_cookie=my_value";
    std::string cookies = original_cookies;

    protect_sensitive_cookies(cookies);

    REQUIRE(cookies == original_cookies);
}

TEST_CASE("protect_cookies/multiple_cookies", "")
{
    const std::string replaced = "replace_me";
    const std::string original_cookies =
        "cookie1=value1.not_me; session_id=stuff." + replaced + "; last=me.neither";
    const std::size_t replaced_offset = original_cookies.find(replaced);

    std::string cookies = original_cookies;

    protect_sensitive_cookies(cookies);

    validate_replacement_in_the_middle(original_cookies, cookies, replaced_offset, replaced.size());
}

TEST_CASE("protect_cookies/no_name_cookie", "")
{
    const std::string replaced = "replace_me";
    const std::string original_cookies = "=value1.not_me; session_id=stuff." + replaced;
    const std::size_t replaced_offset = original_cookies.find(replaced);

    std::string cookies = original_cookies;

    protect_sensitive_cookies(cookies);

    validate_replacement_in_the_middle(original_cookies, cookies, replaced_offset, replaced.size());
}

TEST_CASE("protect_cookies/no_value_cookie", "")
{
    const std::string replaced = "replace_me";
    const std::string original_cookies = "cookie=; session_id=stuff." + replaced;
    const std::size_t replaced_offset = original_cookies.find(replaced);

    std::string cookies = original_cookies;

    protect_sensitive_cookies(cookies);

    validate_replacement_in_the_middle(original_cookies, cookies, replaced_offset, replaced.size());
}

TEST_CASE("protect_cookies/no_space_between_cookies", "")
{
    const std::string replaced = "replace_me";
    const std::string original_cookies =
        "cookie=value.not_me;session_id=stuff." + replaced + "; other=stuff";
    const std::size_t replaced_offset = original_cookies.find(replaced);

    std::string cookies = original_cookies;

    protect_sensitive_cookies(cookies);

    validate_replacement_in_the_middle(original_cookies, cookies, replaced_offset, replaced.size());
}

TEST_CASE("protect_cookies/session_id_in_value")
{
    const std::string replaced = "replace_me";
    const std::string original_cookies =
        "cookie=value.session_id=stuff.not_replaced; session_id=other." + replaced;
    const std::size_t replaced_offset = original_cookies.find(replaced);

    std::string cookies = original_cookies;

    protect_sensitive_cookies(cookies);

    validate_replacement_in_the_middle(original_cookies, cookies, replaced_offset, replaced.size());
}

TEST_CASE("protect_cookies/session_id_ends_with_dot")
{
    const std::string original_cookies = "session_id=will|not|be|replaced.";

    std::string cookies = original_cookies;

    protect_sensitive_cookies(cookies);

    REQUIRE(cookies == original_cookies);
}

TEST_CASE("protect_cookies/session_id_does_not_have_a_dot")
{
    const std::string original_cookies = "session_id=will|not|be|replaced";

    std::string cookies = original_cookies;

    protect_sensitive_cookies(cookies);

    REQUIRE(cookies == original_cookies);
}

#include <yxiva/core/gid.h>
#include <catch.hpp>

TEST_CASE("gid/from_uid/corner_cases", "")
{
    REQUIRE_NOTHROW(yxiva::gid_from_uid("01a"));
    REQUIRE_NOTHROW(yxiva::gid_from_uid(""));
    unsigned long long gid;
    REQUIRE_NOTHROW(gid = yxiva::gid_from_uid("1"));
    REQUIRE(gid == 1UL);
    REQUIRE_NOTHROW(yxiva::gid_from_uid("-1"));
    REQUIRE_NOTHROW(yxiva::gid_from_uid("10 a"));
    REQUIRE_NOTHROW(yxiva::gid_from_uid("a 10"));
}

TEST_CASE("gid/from_uid/tail", "")
{
    using yxiva::gid_from_uid;

    REQUIRE(gid_from_uid("head") == gid_from_uid("head+tail"));
    REQUIRE(gid_from_uid("1") == gid_from_uid("1+2"));
    REQUIRE(gid_from_uid("head+tail1") == gid_from_uid("head+tail2"));
    REQUIRE(gid_from_uid("1+1") == gid_from_uid("1+2"));
    REQUIRE(gid_from_uid("+tail1") == gid_from_uid("+tail2"));
    REQUIRE(gid_from_uid("+1") == gid_from_uid("+2"));

    REQUIRE(gid_from_uid("kopf+tail") != gid_from_uid("head+tail"));
    REQUIRE(gid_from_uid("7+2") != gid_from_uid("1+2"));
}

TEST_CASE("gid/try_strtoull", "")
{
    unsigned long long value;
    REQUIRE(yxiva::detail::try_strtoull("1", value));
    REQUIRE(value == 1UL);
    REQUIRE(yxiva::detail::try_strtoull("18446744073709551615", value));
    REQUIRE(value == 18446744073709551615UL);

    REQUIRE(!yxiva::detail::try_strtoull("-1", value));
    REQUIRE(!yxiva::detail::try_strtoull("abc", value));
    REQUIRE(!yxiva::detail::try_strtoull("10 abc", value));
    REQUIRE(!yxiva::detail::try_strtoull("abc 10", value));
    REQUIRE(!yxiva::detail::try_strtoull(" 10", value));
    REQUIRE(!yxiva::detail::try_strtoull(" 10 ", value));
    REQUIRE(!yxiva::detail::try_strtoull("10 ", value));
    REQUIRE(!yxiva::detail::try_strtoull("", value));
    REQUIRE(!yxiva::detail::try_strtoull("18446744073709551616", value));
    REQUIRE(!yxiva::detail::try_strtoull("999999999999999999999999999999", value));
}

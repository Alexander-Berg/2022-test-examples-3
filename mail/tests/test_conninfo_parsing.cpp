#include <gtest/gtest.h>
#include <gmock/gmock.h>
#include <apq/detail/connection_info.hpp>

using namespace testing;
using namespace apq::detail;

TEST(test_connection_info, empty_string)
{
    connection_info conninfo = parse_connection_info("");
    EXPECT_EQ(conninfo.size(), 0u);
}

TEST(test_connection_info, invalid_string)
{
    EXPECT_ANY_THROW(parse_connection_info("abcde"));
}

TEST(test_connection_info, invalid_key)
{
    EXPECT_ANY_THROW(parse_connection_info("hooooost=localhost"));
}

TEST(test_connection_info, empty_value)
{
    connection_info conninfo = parse_connection_info("port=");
    EXPECT_THAT(conninfo, ElementsAre(std::pair{ "port", "" }));
}

TEST(test_connection_info, invalid_value)
{
    connection_info conninfo = parse_connection_info("port=six-four-three-two");
    EXPECT_THAT(conninfo, ElementsAre(std::pair{ "port", "six-four-three-two" }));
}

TEST(test_connection_info, swapped_key_value)
{
    EXPECT_ANY_THROW(parse_connection_info("6432=port"));
}

TEST(test_connection_info, uri_without_scheme)
{
    EXPECT_ANY_THROW(parse_connection_info("user@localhost:6432/db"));
}

TEST(test_connection_info, uri_invalid_scheme)
{
    EXPECT_ANY_THROW(parse_connection_info("http://"));
}

TEST(test_connection_info, empty_uri)
{
    connection_info conninfo = parse_connection_info("postgresql://");
    EXPECT_EQ(conninfo.size(), 0u);
}

TEST(test_connection_info, parse_key_value)
{
    connection_info conninfo =
        parse_connection_info("host=localhost port=6432 dbname=db user=user");
    EXPECT_THAT(
        conninfo,
        UnorderedElementsAre(
            std::pair{ "host", "localhost" },
            std::pair{ "port", "6432" },
            std::pair{ "dbname", "db" },
            std::pair{ "user", "user" }));
}

TEST(test_connection_info, double_key)
{
    connection_info conninfo = parse_connection_info("port=1234 port=6432");
    EXPECT_THAT(conninfo, ElementsAre(std::pair{ "port", "6432" }));
}

TEST(test_connection_info, parse_uri)
{
    connection_info conninfo = parse_connection_info("postgresql://user@localhost:6432/db");
    EXPECT_THAT(
        conninfo,
        UnorderedElementsAre(
            std::pair{ "host", "localhost" },
            std::pair{ "port", "6432" },
            std::pair{ "dbname", "db" },
            std::pair{ "user", "user" }));
}

TEST(test_connection_info, multihost)
{
    connection_info conninfo = parse_connection_info(
        "host=host1,host2 port=6432 dbname=db user=user target_session_attrs=any");
    EXPECT_THAT(
        conninfo,
        UnorderedElementsAre(
            std::pair{ "host", "host1,host2" },
            std::pair{ "port", "6432" },
            std::pair{ "dbname", "db" },
            std::pair{ "user", "user" },
            std::pair{ "target_session_attrs", "any" }));
}

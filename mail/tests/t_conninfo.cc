#include "catch.hpp"
#include "conninfo.h"

using namespace ymod_pq;

TEST_CASE("bad conninfo", "")
{
    REQUIRE_THROWS(conninfo("not a conninfo"));
    REQUIRE_THROWS(conninfo("unknown_parameter=value"));
}

TEST_CASE("conninfo serialization", "")
{
    REQUIRE(conninfo("host=123 dbname=456").to_string() == "host=123 dbname=456");
    REQUIRE(conninfo("host = 123 dbname = 456").to_string() == "host=123 dbname=456");
    REQUIRE(conninfo("host = '123 678' dbname = 456").to_string() == "host='123 678' dbname=456");
    REQUIRE(conninfo("host = '\\' \\\\'").to_string() == "host='\\' \\\\'");
}

TEST_CASE("conninfo extracts parameters from string", "")
{
    conninfo c("host=123 dbname=456 sslmode=verify-full");
    REQUIRE(c.get("host") == "123");
    REQUIRE(c.get("dbname") == "456");
    REQUIRE(c.get("sslmode") == "verify-full");
    REQUIRE(c.get("user") == "");
}

TEST_CASE("conninfo allows to set parameter values")
{
    conninfo c("host=123 sslmode=verify-full dbname=456");
    REQUIRE(c.to_string() == "host=123 sslmode=verify-full dbname=456");
    c.set("dbname", "x");
    c.set("user", "y");
    c.set("empty", "");
    REQUIRE(c.to_string() == "host=123 empty=\'\' user=y sslmode=verify-full dbname=x");
}

TEST_CASE("conninfo provides multiple hosts in a vector", "")
{
    conninfo c("host=h1,h2,h3");
    REQUIRE(c.hosts() == (std::vector<std::string>{ "h1", "h2", "h3" }));
}

TEST_CASE("conninfo allows to reset parameters")
{
    conninfo c("host=123 dbname=456");
    c.reset("dbname");
    c.reset("user"); // no-op
    REQUIRE(c.to_string() == "host=123");
}
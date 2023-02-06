#include <yplatform/hash/md5.h>
#include <catch.hpp>
#include <string>

TEST_CASE("md5/single_c_str", "")
{
    REQUIRE(yplatform::md5("value") == "2063c1608d6e0baf80249c42e2be5804");
}

TEST_CASE("md5/single_str", "")
{
    REQUIRE(yplatform::md5(std::string("value")) == "2063c1608d6e0baf80249c42e2be5804");
}

TEST_CASE("md5/mixed", "")
{
    REQUIRE(yplatform::md5("012345", std::string("value")) == "4eeae62807cb87f71d29167888d37360");
    REQUIRE(yplatform::md5(std::string("012345"), "value") == "4eeae62807cb87f71d29167888d37360");
}

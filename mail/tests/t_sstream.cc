#include <yplatform/encoding/base64.h>
#include <yplatform/util/sstream.h>
#include <catch.hpp>
#include <string>

TEST_CASE("sstream/reserve")
{
    std::string str;
    yplatform::sstream(str, 50);
    REQUIRE(str.capacity() >= 50);
}

TEST_CASE("sstream/basic_write")
{
    std::string str;
    char ch1[] = "Hello";
    char ch2[] = "world";
    char* pch1 = ch2;
    int i = 33;
    double d = 12.3;
    std::string result = "Hello world33" + std::to_string(d) + "!";
    yplatform::sstream(str) << ch1 << " " << pch1 << i << d << '!';
    REQUIRE(str == result);
}

TEST_CASE("sstream/custom_write")
{
    std::string str;
    std::string message = "hello world";
    std::string result = "aGVsbG8gd29ybGQ";
    yplatform::sstream(str) << yplatform::base64_urlsafe_encode(message.begin(), message.end());
    REQUIRE(str == result);
}

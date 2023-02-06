#define CATCH_CONFIG_MAIN

#include <yplatform/range.h>
#include <yplatform/encoding/base64.h>
#include <catch.hpp>
#include <string>

const std::string EMPTY_STRING = "";
const std::string TEST = "test string! Q> y?=";
const std::string RESULT = "dGVzdCBzdHJpbmchIFE+IHk/PQ==";
const std::string RESULT_SAFE = "dGVzdCBzdHJpbmchIFE-IHk_PQ";

const std::string ONE = "1";
const std::string TWO = "12";
const std::string THREE = "123";
const std::string ONE_RES = "MQ==";
const std::string ONE_RES_SAFE = "MQ";
const std::string TWO_RES = "MTI=";
const std::string TWO_RES_SAFE = "MTI";
const std::string THREE_RES = "MTIz";
const std::string THREE_RES_SAFE = "MTIz";
const std::string FILLER = "---";
const std::string FILLER_RES = "LS0t";
const int PADDING_TEST_ITERS = 5;

TEST_CASE("empty string encode")
{
    auto empty_b64 = yplatform::base64_encode(EMPTY_STRING.begin(), EMPTY_STRING.end());
    std::string empty_b64_str(empty_b64.begin(), empty_b64.end());
    REQUIRE(empty_b64_str == EMPTY_STRING);
}

TEST_CASE("empty string urlsafe encode")
{
    auto empty_b64 = yplatform::base64_urlsafe_encode(EMPTY_STRING.begin(), EMPTY_STRING.end());
    std::string empty_b64_str(empty_b64.begin(), empty_b64.end());
    REQUIRE(empty_b64_str == EMPTY_STRING);
}

TEST_CASE("empty string decode")
{
    auto empty_b64 = yplatform::base64_decode(EMPTY_STRING.begin(), EMPTY_STRING.end());
    std::string empty_b64_str(empty_b64.begin(), empty_b64.end());
    REQUIRE(empty_b64_str == EMPTY_STRING);
}

TEST_CASE("empty string urlsafe decode")
{
    auto empty_b64 = yplatform::base64_urlsafe_decode(EMPTY_STRING.begin(), EMPTY_STRING.end());
    std::string empty_b64_str(empty_b64.begin(), empty_b64.end());
    REQUIRE(empty_b64_str == EMPTY_STRING);
}

TEST_CASE("correct base64 table encode")
{
    auto res_b64 = yplatform::base64_encode(TEST.begin(), TEST.end());
    std::string res_b64_str(res_b64.begin(), res_b64.end());
    REQUIRE(res_b64_str == RESULT);
}

TEST_CASE("correct base64 urlsafe table encode")
{
    auto res_b64 = yplatform::base64_urlsafe_encode(TEST.begin(), TEST.end());
    std::string res_b64_str(res_b64.begin(), res_b64.end());
    REQUIRE(res_b64_str == RESULT_SAFE);
}

TEST_CASE("correct base64 table decode")
{
    auto res_b64 = yplatform::base64_decode(RESULT.begin(), RESULT.end());
    std::string res_b64_str(res_b64.begin(), res_b64.end());
    REQUIRE(res_b64_str == TEST);
}

TEST_CASE("correct base64 urlsafe table decode")
{
    auto res_b64 = yplatform::base64_urlsafe_decode(RESULT_SAFE.begin(), RESULT_SAFE.end());
    std::string res_b64_str(res_b64.begin(), res_b64.end());
    REQUIRE(res_b64_str == TEST);
}

TEST_CASE("wrong base64 symbols throw")
{
    REQUIRE_THROWS([] {
        auto res_b64 = yplatform::base64_decode(RESULT_SAFE.begin(), RESULT_SAFE.end());
        std::string res_b64_str(res_b64.begin(), res_b64.end());
    }());
}

TEST_CASE("wrong base64 urlsafe symbols throw")
{
    REQUIRE_THROWS([] {
        auto res_b64 = yplatform::base64_urlsafe_decode(RESULT.begin(), RESULT.end());
        std::string res_b64_str(res_b64.begin(), res_b64.end());
    }());
}

TEST_CASE("padding - 1 byte")
{
    std::string str = ONE;
    std::string res = ONE_RES;
    std::string res_safe = ONE_RES_SAFE;
    for (int i = 0; i < PADDING_TEST_ITERS;
         str = FILLER + str, res = FILLER_RES + res, res_safe = FILLER_RES + res_safe, ++i)
    {
        auto b64 = yplatform::base64_encode(str.begin(), str.end());
        std::string b64_str(b64.begin(), b64.end());
        CHECK(b64_str == res);
        auto b64_safe = yplatform::base64_urlsafe_encode(str.begin(), str.end());
        std::string b64_safe_str(b64_safe.begin(), b64_safe.end());
        CHECK(b64_safe_str == res_safe);
    }
}

TEST_CASE("padding - 2 bytes")
{
    std::string str = TWO;
    std::string res = TWO_RES;
    std::string res_safe = TWO_RES_SAFE;
    for (int i = 0; i < PADDING_TEST_ITERS;
         str = FILLER + str, res = FILLER_RES + res, res_safe = FILLER_RES + res_safe, ++i)
    {
        auto b64 = yplatform::base64_encode(str.begin(), str.end());
        std::string b64_str(b64.begin(), b64.end());
        CHECK(b64_str == res);
        auto b64_safe = yplatform::base64_urlsafe_encode(str.begin(), str.end());
        std::string b64_safe_str(b64_safe.begin(), b64_safe.end());
        CHECK(b64_safe_str == res_safe);
    }
}

TEST_CASE("padding - 3 bytes")
{
    std::string str = THREE;
    std::string res = THREE_RES;
    std::string res_safe = THREE_RES_SAFE;
    for (int i = 0; i < PADDING_TEST_ITERS;
         str = FILLER + str, res = FILLER_RES + res, res_safe = FILLER_RES + res_safe, ++i)
    {
        auto b64 = yplatform::base64_encode(str.begin(), str.end());
        std::string b64_str(b64.begin(), b64.end());
        CHECK(b64_str == res);
        auto b64_safe = yplatform::base64_urlsafe_encode(str.begin(), str.end());
        std::string b64_safe_str(b64_safe.begin(), b64_safe.end());
        CHECK(b64_safe_str == res_safe);
    }
}

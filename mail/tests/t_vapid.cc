#include "mod_webpush/vapid.h"
#include <yxiva/core/ec_crypto.h>
#include <yplatform/encoding/base64.h>
#include <yplatform/time_traits.h>
#include <yplatform/spinlock.h>
#include <catch.hpp>
#include <boost/asio.hpp>
#include <boost/date_time/posix_time/posix_time.hpp>
#include <boost/regex.hpp>
#include <unistd.h>
#include <string>
#include <memory>
#include <chrono>
#include <thread>
#include <iostream>
#include <ctime>
#include <system_error>
#include <future>

const std::string VAPID_SUB = "test_sub";
const std::string TEST_ORIGIN = "https://yandex.com";
const std::string TESTING_PUBLIC_KEY =
    "BIYCHd8m-aT6emx7F_HNOGiOkOBXlPOJDEDpkeyFQVys3pQ1lpW5Va3R6XIDqpL8crYOswn1CSNfoGBkubTWIsA";
const std::string TEST_KEY_LOCATION = "vapid-private-test.pem";
const unsigned int TEST_VALID = 1234;
const unsigned int TEST_CONCURRENCY = 5;

using yxiva::json_value;
using namespace yxiva::mobile;
using namespace yxiva::ec_crypto;
using namespace yplatform::time_traits;

TEST_CASE("webpush/vapid_basic", "")
{
    vapid_store<yplatform::spinlock> vapids(hours(1), minutes(30));
    vapids.reset(TEST_KEY_LOCATION, VAPID_SUB);

    auto vapid = vapids.get(TEST_ORIGIN);
    REQUIRE(vapid);
    auto v2 = vapids.get(TEST_ORIGIN);
    REQUIRE(v2 == vapid);
    auto v3 = vapids.get(TEST_ORIGIN + ".tr");
    REQUIRE(v3->key == vapid->key);
    REQUIRE(v3->auth != vapid->auth);
}

TEST_CASE("webpush/vapid_regeneration", "")
{
    vapid_store<yplatform::spinlock> vapids(hours(1), hours(1));
    vapids.reset(TEST_KEY_LOCATION, VAPID_SUB);

    auto vapid = vapids.get(TEST_ORIGIN);
    REQUIRE(vapid);
    auto v2 = vapids.get(TEST_ORIGIN);
    REQUIRE(v2);
    REQUIRE(v2 != vapid);
}

TEST_CASE("webpush/vapid_check_fields", "")
{
    vapid_store<yplatform::spinlock> vapids(seconds(TEST_VALID), seconds(0));
    vapids.reset(TEST_KEY_LOCATION, VAPID_SUB);

    auto vapid = vapids.get(TEST_ORIGIN);
    REQUIRE(vapid);

    boost::regex re(R"(^(\S+)\.(\S+)\.(\S+)$)");
    boost::smatch m;
    REQUIRE(boost::regex_match(vapid->auth, m, re));
    REQUIRE(m[1].matched);
    REQUIRE(m[2].matched);
    REQUIRE(m[3].matched);

    std::string header, claim;
    REQUIRE_NOTHROW(header += yplatform::base64_urlsafe_decode(m[1].first, m[1].second));
    REQUIRE_NOTHROW(claim += yplatform::base64_urlsafe_decode(m[2].first, m[2].second));

    json_value json_header, json_claim;
    // check no parse errors
    REQUIRE(!json_header.parse(header.data()));
    REQUIRE(!json_claim.parse(claim.data()));

    REQUIRE(json_header.has_member("typ"));
    CHECK(json_header["typ"].to_string() == "JWT");
    REQUIRE(json_header.has_member("alg"));
    CHECK(json_header["alg"].to_string() == "ES256");

    REQUIRE(json_claim.has_member("sub"));
    CHECK(json_claim["sub"].to_string() == VAPID_SUB);
    REQUIRE(json_claim.has_member("aud"));
    CHECK(json_claim["aud"].to_string() == TEST_ORIGIN);
    REQUIRE(json_claim.has_member("exp"));
    CHECK(json_claim["exp"].is_number());
    auto now = time(nullptr);
    auto exp = now + TEST_VALID;
    CHECK(json_claim["exp"].to_uint64() >= now);
    CHECK(json_claim["exp"].to_uint64() <= exp);
}

TEST_CASE("webpush/vapid_multi_access", "")
{
    vapid_store<yplatform::spinlock> vapids(seconds(TEST_VALID), seconds(0));
    vapids.reset(TEST_KEY_LOCATION, VAPID_SUB);

    if (TEST_CONCURRENCY <= 1) return;
    try
    {
        std::vector<std::future<std::shared_ptr<vapid_record>>> tasks;
        std::vector<std::shared_ptr<vapid_record>> results;
        tasks.reserve(TEST_CONCURRENCY);
        results.reserve(TEST_CONCURRENCY);

        for (unsigned int i = 0; i < TEST_CONCURRENCY; ++i)
            tasks.push_back(std::async(
                std::launch::async, &vapid_store<yplatform::spinlock>::get, &vapids, TEST_ORIGIN));

        for (auto& task : tasks)
            results.push_back(task.get());

        for (unsigned int i = 1; i < TEST_CONCURRENCY; ++i)
        {
            CHECK(results[i] == results[i - 1]);
        }
    }
    catch (const std::system_error&)
    {
        std::cout << "\033[1;31m"
                  << "vapid_multi_access test can't be run due to exception "
                  << "when trying to complete async operation"
                  << "\033[0m\n";
    }
}

TEST_CASE("webpush/vapid_check_sign", "")
{
    vapid_store<yplatform::spinlock> vapids(seconds(TEST_VALID), seconds(0));
    vapids.reset(TEST_KEY_LOCATION, VAPID_SUB);

    auto vapid = vapids.get(TEST_ORIGIN);
    REQUIRE(bool(vapid));
    auto dot_pos = vapid->auth.rfind(".");
    REQUIRE(dot_pos != std::string::npos);
    std::string vapid_part = vapid->auth.substr(0, dot_pos);
    std::string vapid_sign = vapid->auth.substr(dot_pos + 1);
    std::cout << "part: " << vapid_part << " sign = " << vapid_sign << std::endl;

    auto sig_decode = yplatform::base64_urlsafe_decode(vapid_sign);
    std::vector<unsigned char> sign_bin;
    REQUIRE_NOTHROW(sign_bin.assign(sig_decode.begin(), sig_decode.end()));
    REQUIRE(sign_bin.size() == 64);

    auto key_decode = yplatform::base64_urlsafe_decode(vapid->key);
    std::vector<unsigned char> key_bin;
    REQUIRE_NOTHROW(key_bin.assign(key_decode.begin(), key_decode.end()));

    auto ecgrp = std::shared_ptr<EC_GROUP>(
        EC_GROUP_new_by_curve_name(OBJ_txt2nid("prime256v1")), EC_GROUP_free);
    auto key = evp_from_public_key(key_bin, ecgrp.get());
    REQUIRE(verify_sign(vapid_part.data(), vapid_part.size(), sign_bin, key));
}

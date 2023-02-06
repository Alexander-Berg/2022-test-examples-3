#include "mod_webpush/payload_encryption.h"
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
#include <thread>
#include <iostream>
#include <ctime>
#include <system_error>
#include <future>

using namespace yxiva::mobile;

const unsigned int TEST_CONCURRENCY = 5;

TEST_CASE("webpush/keystore_basic", "")
{
    boost::asio::io_service svc;
    keys_store<yplatform::spinlock> keys(svc, yplatform::time_traits::hours(1));

    auto key = keys.get();
    REQUIRE(bool(key));
    CHECK(!key->public_key.empty());
    CHECK(!key->public_key_base64.empty());
    CHECK(bool(key->keypair));
    CHECK(bool(key->ecgrp));
    auto k2 = keys.get();
    REQUIRE(k2 == key);
}

TEST_CASE("webpush/keystore_check_key", "")
{
    boost::asio::io_service svc;
    keys_store<yplatform::spinlock> keys(svc, yplatform::time_traits::hours(1));

    auto key = keys.get();
    REQUIRE(bool(key));

    std::string b64;
    b64 += yplatform::base64_urlsafe_encode(key->public_key.begin(), key->public_key.end());
    CHECK(b64 == key->public_key_base64);

    CHECK(key->public_key.size() == 65); // 64b uncompressed key + 1b flag
    CHECK(key->public_key[0] == 4);      // uncompressed key flag
}

TEST_CASE("webpush/keystore_generate", "")
{
    boost::asio::io_service svc;
    auto keys = std::make_shared<keys_store<yplatform::spinlock>>(
        svc, yplatform::time_traits::milliseconds(5));

    auto key = keys->get();
    REQUIRE(bool(key));

    keys->run();
    usleep(10000);
    svc.run_one();

    auto key2 = keys->get();
    REQUIRE(bool(key2));
    REQUIRE(key != key2);
}

TEST_CASE("webpush/keystore_multi_access", "")
{
    boost::asio::io_service svc;
    keys_store<yplatform::spinlock> keys(svc, yplatform::time_traits::hours(1));

    if (TEST_CONCURRENCY <= 1) return;
    try
    {
        std::vector<std::future<std::shared_ptr<server_keys>>> tasks;
        std::vector<std::shared_ptr<server_keys>> results;
        tasks.reserve(TEST_CONCURRENCY);
        results.reserve(TEST_CONCURRENCY);

        for (unsigned int i = 0; i < TEST_CONCURRENCY; ++i)
            tasks.push_back(
                std::async(std::launch::async, &keys_store<yplatform::spinlock>::get, &keys));

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
                  << "keystore_multi_access test can't be run due to exception "
                  << "when trying to complete async operation"
                  << "\033[0m\n";
    }
}

#include <iostream>

#include <arpa/inet.h>
#include <blowfish.h>
#include <catch.hpp>

struct t_encryption
{
    std::string encrypt(const std::string val)
    {
        return blowfish::password::encrypt(val, keys, IV);
    }

    std::string decrypt(const std::string val)
    {
        return blowfish::password::decrypt(val, keys, IV);
    }

    const std::string IV = "iviviviv";
    blowfish::password::versioned_keys keys =
        blowfish::password::compute_derived_keys("data/versioned_keys");
};

TEST_CASE_METHOD(t_encryption, "encrypts correctly")
{
    REQUIRE(encrypt("password") == "\x09\x09YMhRf40jcdH+5CBO6cgv7od5MmU=");
}

TEST_CASE_METHOD(t_encryption, "decrypts correctly")
{
    auto value = "password";
    auto encrypted = encrypt(value);
    REQUIRE(decrypt(encrypted) == value);
}


#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <butil/crypt.h>
#include <butil/butil.h>

namespace {

using namespace crypto;
using namespace testing;

const std::string key = "KeyKeyKeyKeyKey!";
const std::string iv = "Anything";

TEST(BlowfishTest, encrypt_emptyString_returnEncrypted) {
    const std::vector<unsigned char> expectedBuf =
        {0xa0, 0xbe, 0xe0, 0x23, 0x1e, 0xaa, 0x60, 0x4d};
    ASSERT_EQ(std::string(expectedBuf.begin(), expectedBuf.end()), encrypt_string("", key, iv));

}

TEST(BlowfishTest, encrypt_sampleString_returnEncrypted) {
    const std::vector<unsigned char> expectedBuf =
        {0xe1, 0x3a, 0x96, 0x84, 0x92, 0x47, 0xae, 0xb4, 0x4c, 0x54, 0xda, 0x0b, 0xb9, 0xbf, 0x4c, 0x44};
    ASSERT_EQ(std::string(expectedBuf.begin(), expectedBuf.end()), encrypt_string("abcdefghijklm", key, iv));
}

TEST(BlowfishTest, decrypt_emptyStringEncrypted_returnEmptyString) {
    const std::vector<unsigned char> encryptedBuf =
        {0xa0, 0xbe, 0xe0, 0x23, 0x1e, 0xaa, 0x60, 0x4d};
    const std::string encrypted(encryptedBuf.begin(), encryptedBuf.end());
    ASSERT_EQ("", decrypt_string(encrypted, key, iv));

}

TEST(BlowfishTest, decrypt_sampleStringEncrypted_returnOriginal) {
    const std::vector<unsigned char> encryptedBuf =
        {0xe1, 0x3a, 0x96, 0x84, 0x92, 0x47, 0xae, 0xb4, 0x4c, 0x54, 0xda, 0x0b, 0xb9, 0xbf, 0x4c, 0x44};
    const std::string encrypted(encryptedBuf.begin(), encryptedBuf.end());
    ASSERT_EQ("abcdefghijklm", decrypt_string(encrypted, key, iv));
}

struct AesTest : public Test {
    AesTest()
        : iv({0xa4, 0x12, 0xee, 0xa2, 0x75, 0x58, 0x33, 0xe0, 0x48, 0xf1, 0xfb, 0x54, 0x7f, 0x18, 0xd8, 0xd1})
        , key("a3c7cc75688104eecf470920ceb0b834")
    {}

    const blob iv;
    const AesKey key;
};

TEST_F(AesTest, encryptDecryptEmptyStringShouldNotChangeText) {
    const std::string plainText = "";

    ASSERT_EQ(plainText, aesDecrypt(aesEncrypt(plainText, key, iv), key, iv));
}

TEST_F(AesTest, encryptDecryptRandomHexStringShouldNotChangeText) {
    const std::string plainText = "1173e8027545e7f7c17b186d8958532bb0d27697f7da92f430babed04fba0ce687de4a26e"
                                  "73fd359554ec894042f4bba1f5bffc7b5b824d9eb2f81b32b1626aa448327aff805567d25"
                                  "26369e013f9e59f2b8a1a92e590180faffd42e151e1cd3e99da49a7d6899e2fe1ece40d7b"
                                  "692644bcd62d90aae86e4f6ac39a87125f0527d778acbc25cbf9e4798206ef121f902b613"
                                  "09eb6eef4e888896417cbec4efe72c2897a2647666163c785f7a07bb46de82ef114552c44"
                                  "91e037d513862048095cc748ac5433869b8da2364bd29caebcd04c0467af748ac32df0287"
                                  "29640d73f80d5412a446eadeb3bb96e00751e1be0500b22ffbd9efdf808377b6ee644beaf"
                                  "87d475166faec1114d01574fca54940b9b641e5a75fe62297fc33774e6006a53bdb8ede6b"
                                  "87a70efe7b2cdfca";

    ASSERT_EQ(plainText, aesDecrypt(aesEncrypt(plainText, key, iv), key, iv));
}

TEST_F(AesTest, encryptDecryptRandomBinaryStringShouldNotChangeText) {
    const std::string plainText = "\xE3\x33\x79\x1A\xBD\xB9\x3B\xDF\x76\x96\xBE\x8B"
                                  "\x14\xB5\x38\xF1\xEC\xAF\xD2\xEA\x16\x77\xE5\xEF";

    const blob expectedCipherText = blob{0x3e, 0xd9, 0x48, 0x53, 0x4f, 0x39, 0x0b, 0xc7, 0x00, 0x23, 0xdb, 0x4e,
                                         0xa5, 0xdd, 0xa4, 0x12, 0xe5, 0x27, 0x0d, 0x6b, 0x9f, 0x25, 0xa2, 0xa6,
                                         0x2d, 0x31, 0x32, 0x90, 0x53, 0xff, 0x27, 0xbc};

    const blob cipherText = aesEncrypt(plainText, key, iv);
    ASSERT_EQ(expectedCipherText, cipherText);
    ASSERT_EQ(plainText, aesDecrypt(cipherText, key, iv));
}

struct HmacTest : public Test {
    HmacTest()
        : iv({0xa4, 0x12, 0xee, 0xa2, 0x75, 0x58, 0x33, 0xe0, 0x48, 0xf1, 0xfb, 0x54, 0x7f, 0x18, 0xd8, 0xd1})
        , key("a3c7cc75688104eecf470920ceb0b834")
    {}

    const blob iv;
    const HmacKey key;
};

TEST_F(HmacTest, hmacEmptyString) {
    const std::string plainText = "";

    const std::string expected = "\x93\x11\xA4\xFD\xEA\xD1\xF2\vP\xAEk\xB5\x84I\x15"
                                 "\xC2\x80@\b=\xE1o\xEB\x83:\x1A:\x92$D\x82\x1B";

    ASSERT_EQ(expected, hmac(plainText, key));
}

TEST_F(HmacTest, hashRandomHexString) {
    const std::string plainText = "1173e8027545e7f7c17b186d8958532bb0d27697f7da92f430babed04fba0ce687de4a26e"
                                  "73fd359554ec894042f4bba1f5bffc7b5b824d9eb2f81b32b1626aa448327aff805567d25"
                                  "26369e013f9e59f2b8a1a92e590180faffd42e151e1cd3e99da49a7d6899e2fe1ece40d7b"
                                  "692644bcd62d90aae86e4f6ac39a87125f0527d778acbc25cbf9e4798206ef121f902b613";

    const std::string expected = "0mh\x92u\xB4\x8C\x89\xC0@\xC3"
                                 "b\xC8}4\xF\xC1r\xF1\x10\xE5MC\xFFm\xD0\r\xC5L\xC8\xB3\x5";

    ASSERT_EQ(expected, hmac(plainText, key));
}

TEST(GenerateIvTest, checkIvLength) {
    ASSERT_EQ(32u, generateIv(32).size());
}

TEST(AesKeyTest, shouldThrowExceptionOnKeyWithWrongSize) {
    EXPECT_THROW(AesKey("1"), std::invalid_argument);
}

TEST(HmacKeyTest, shouldThrowExceptionOnKeyWithWrongSize) {
    EXPECT_THROW(HmacKey("1"), std::invalid_argument);
}

}


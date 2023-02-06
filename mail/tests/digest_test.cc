
#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <butil/digest.h>

namespace {

TEST(MD5Test, md5hex_emptyString_returnMd5) {
    ASSERT_EQ("d41d8cd98f00b204e9800998ecf8427e", md5_hex(""));
}

TEST(MD5Test, md5hex_quickBrownFox_returnMd5) {
    ASSERT_EQ("9e107d9d372bb6826bd81d3542a419d6", md5_hex("The quick brown fox jumps over the lazy dog"));
}

TEST(MD5Test, md5raw_emptyString_returnMd5Binary) {
    const unsigned char expectedBuf[] = {0xd4, 0x1d, 0x8c, 0xd9, 0x8f, 0x00, 0xb2, 0x04,
                                         0xe9, 0x80, 0x09, 0x98, 0xec, 0xf8, 0x42, 0x7e};
    const std::string expected(expectedBuf, expectedBuf+16);
    ASSERT_EQ(expected, md5_raw(""));
}

TEST(MD5Test, md5raw_quickBrownFox_returnMd5Binary) {
    const unsigned char expectedBuf[] = {0x9e, 0x10, 0x7d, 0x9d, 0x37, 0x2b, 0xb6, 0x82,
                                         0x6b, 0xd8, 0x1d, 0x35, 0x42, 0xa4, 0x19, 0xd6};
    const std::string expected(expectedBuf, expectedBuf+16);
    ASSERT_EQ(expected, md5_raw("The quick brown fox jumps over the lazy dog"));
}

}

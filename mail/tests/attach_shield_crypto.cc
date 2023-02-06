#include <mail_getter/AttachShieldCrypto.h>
#include <butil/butil.h>
#include <boost/algorithm/string/join.hpp>
#include <gtest/gtest.h>

namespace mail_getter {
namespace part_id {

bool operator ==(const Old& lhs, const Old& rhs) {
    return lhs.stid == rhs.stid && lhs.hid == rhs.hid;
}

bool operator ==(const Temporary& lhs, const Temporary& rhs) {
    return lhs.stid == rhs.stid && lhs.hid == rhs.hid;
}

bool operator ==(const SingleMessagePart& lhs, const SingleMessagePart& rhs) {
    return lhs.uid == rhs.uid && lhs.mid == rhs.mid && lhs.hid == rhs.hid;
}

bool operator ==(const MultipleMessagePart& lhs, const MultipleMessagePart& rhs) {
    return lhs.uid == rhs.uid && lhs.mid == rhs.mid && lhs.hids == rhs.hids;
}

std::ostream& operator <<(std::ostream& stream, const Old& value) {
    return stream << "mail_getter::part_id::Old {" << value.stid << ", " << value.hid << "}";
}

std::ostream& operator <<(std::ostream& stream, const Temporary& value) {
    return stream << "mail_getter::part_id::Temporary {" << value.stid << ", " << value.hid << "}";
}

std::ostream& operator <<(std::ostream& stream, const SingleMessagePart& value) {
    return stream << "mail_getter::part_id::SingleMessagePart {" << value.uid << ", "  << value.mid << ", " << value.hid << "}";
}

std::ostream& operator <<(std::ostream& stream, const std::vector<Hid>& values) {
    return stream << "{" << boost::algorithm::join(values, ", ") << "}";
}

std::ostream& operator <<(std::ostream& stream, const MultipleMessagePart& value) {
    return stream << "mail_getter::part_id::MultipleMessagePart {" << value.uid << ", "  << value.mid << ", " << value.hids << "}";
}

template <class T>
struct IsEqual {
    using result_type = bool;

    const T& lhs;

    template <class V>
    result_type operator ()(const V&) const { return false; }

    result_type operator ()(const T& rhs) const { return lhs == rhs; }
};

template <class T>
auto isEqual(const T& lhs) {
    return IsEqual<std::decay_t<T>> {lhs};
}

template <class T>
bool operator ==(const T& lhs, const Variant& rhs) {
    return boost::apply_visitor(isEqual(lhs), rhs);
}

struct Write {
    using result_type = std::ostream&;

    std::ostream& stream;

    template <class T>
    result_type operator ()(const T& value) const { return stream << value; }
};

std::ostream& operator <<(std::ostream& stream, const Variant& value) {
    return boost::apply_visitor(Write {stream}, value);
}

} // namespace part_id
} // namespace mail_getter

namespace {

using namespace testing;
using namespace mail_getter;
using namespace mail_getter::attach_sid;

struct AttachShieldCryptoTest : public Test {
    const std::string aesKeyId = "42";
    const ::crypto::AesKey aesKey{"\x10\x85\x03\xC9\xF6\x40\xE1\x8C\xAE\x29\x0E\x29\xFB\x6F\xBD\xEC"
                                  "\xAD\xC1\x2C\xC4\xF8\xDA\xE2\xAD\x1F\xCD\xF3\x0A\x5A\xCB\x15\x04"};
    const std::string hmacKeyId = "42";
    const ::crypto::HmacKey hmacKey{"\xC4\xF2\xFD\xA3\x94\x49\xFF\x4D\xC8\xA2\x0A\x74\x69\x7F\xE8\x1F"
                                    "\x9F\x1B\xF5\x5C\xF4\xA7\xAA\x21\x71\xDF\xF1\x0C\xD3\xC5\xE6\xC1"};

    KeyContainer keyContainer = {  {{aesKeyId, aesKey}}, {{hmacKeyId, hmacKey}}  };
    Keys keys = Keys(keyContainer, aesKeyId, hmacKeyId);
    crypto::blob iv = { 0x3c, 0x2b, 0x07, 0xd8, 0x90, 0xc4, 0x5c, 0x94, 0x59, 0xd8, 0x3e,
                        0x92, 0xba, 0x58, 0xef, 0xbe, 0x0b, 0xe8, 0x16, 0xb6, 0x3b, 0xa3,
                        0xe3, 0xe0, 0x77, 0xd8, 0x75, 0xbd, 0xff, 0x1c, 0xcb, 0x86 };
    Packer packer = Packer(std::time_t(111548344196), std::chrono::seconds(1000), keys, iv);
};

TEST_F(AttachShieldCryptoTest, pack_then_unpack_old_should_return_original) {
    const part_id::Old value{"stid", "hid"};

    const std::string expectedPacked = "YWVzX3NpZDp7ImFlc0tleUlkIjoiNDIiLCJobWFjS2V5SWQiOiI0MiIsIm"
                                       "l2QmFzZTY0IjoiUENzSDJKREVYSlJaMkQ2U3VsanZ2Z3ZvRnJZN28rUGdk"
                                       "OWgxdmY4Y3k0WT0iLCJzaWRCYXNlNjQiOiI2dThkYlRFRE9ocWxacjlyKz"
                                       "c5V1czZW9TbVh6dSt2aUg3NEJYMFo1cFFNPSIsImhtYWNCYXNlNjQiOiJU"
                                       "TFN1b0RYLzhKUitjSXB3VURva20zQS82U3BDb0hMa09yNUhTVFdaejhvPSJ9";
    const auto packed = packer(value);

    EXPECT_EQ(expectedPacked, packed);

    const auto unpacked = Unpacker(keyContainer)(packed);
    EXPECT_EQ(value, unpacked);
}

TEST_F(AttachShieldCryptoTest, pack_then_unpack_temporary_should_return_original) {
    const part_id::Temporary value {"stid", "hid"};

    const std::string expectedPacked = "YWVzX3NpZDp7ImFlc0tleUlkIjoiNDIiLCJobWFjS2V5SWQiOiI0MiIsIml"
                                       "2QmFzZTY0IjoiUENzSDJKREVYSlJaMkQ2U3VsanZ2Z3ZvRnJZN28rUGdkOW"
                                       "gxdmY4Y3k0WT0iLCJzaWRCYXNlNjQiOiI4dGl2MEF5YytjSUVFV3M5V1pCe"
                                       "FhFT3U4TG1LajF6QlM3UExmdEpzYnhHRXl0ZisvNCt6WGdDY3FZc0FqT21O"
                                       "WVJNWVovTlhKSjlkcGpzSjg0TXBsZz09IiwiaG1hY0Jhc2U2NCI6IktHZXJ"
                                       "INzhUTHlvRGZZaW80Z1dTYkN4VHhUZzhIRVdqcndyRnBGSlM1Yzg9In0=";
    const auto packed = packer(value);

    EXPECT_EQ(expectedPacked, packed);

    const auto unpacked = Unpacker(keyContainer)(packed);
    EXPECT_EQ(value, unpacked);
}

TEST_F(AttachShieldCryptoTest, pack_then_unpack_single_message_part_should_return_original) {
    const part_id::SingleMessagePart value {"uid", "mid", "hid"};

    const std::string expectedPacked = "YWVzX3NpZDp7ImFlc0tleUlkIjoiNDIiLCJobWFjS2V5SWQiOiI0MiIsIml"
                                       "2QmFzZTY0IjoiUENzSDJKREVYSlJaMkQ2U3VsanZ2Z3ZvRnJZN28rUGdkOW"
                                       "gxdmY4Y3k0WT0iLCJzaWRCYXNlNjQiOiJYUlpxRXRwR3FWTDR0NlJMZ2M4V"
                                       "1BlbmM3eUQwcWdTT2c3UXZwelFRdnoydUFiTTljRXJXRjVEdkpHYWc1YUw5"
                                       "Z2VoTHpkNFF2U3ltbXVhTEdUck1QR0s1WHZaZDIvL3EyTVFSS2pQQTNOWT0"
                                       "iLCJobWFjQmFzZTY0IjoiTDNORGVMd3VLMzcrUmFiMWZBUnNIcjlZN0xIOS"
                                       "tIS2JKMjFZQ0VCZFpMYz0ifQ==";
    const auto packed = packer(value);

    EXPECT_EQ(expectedPacked, packed);

    const auto unpacked = Unpacker(keyContainer)(packed);
    EXPECT_EQ(value, unpacked);
}

TEST_F(AttachShieldCryptoTest, pack_then_unpack_multiple_message_part_should_return_original) {
    const part_id::MultipleMessagePart value {"uid", "mid", {"hid1", "hid2"}};

    const std::string expectedPacked = "YWVzX3NpZDp7ImFlc0tleUlkIjoiNDIiLCJobWFjS2V5SWQiOiI0MiIsIml"
                                       "2QmFzZTY0IjoiUENzSDJKREVYSlJaMkQ2U3VsanZ2Z3ZvRnJZN28rUGdkOW"
                                       "gxdmY4Y3k0WT0iLCJzaWRCYXNlNjQiOiIyMy9kUjNnaEFpNWVIOFlBK0MwV"
                                       "E8vN3YwZFVkaC9wZ2s1NHNkVWtESjlmZkFjY2tJUGNGNTZhMjROWVQ5RlZT"
                                       "TWlYTStuemZleEFvU2NhVlN6R1FpYXpraXplRzAyblJEUVNESkRoTWJRUnB"
                                       "uc0FBRlU3S01ObDA0d2JWcTdmYyIsImhtYWNCYXNlNjQiOiJaV1NJQm00dW"
                                       "9TM05aYmxEZURmYmh3Ujh4Yloyc1hOVUtkNmlFSFlhYmhZPSJ9";
    const auto packed = packer(value);

    EXPECT_EQ(expectedPacked, packed);

    const auto unpacked = Unpacker(keyContainer)(packed);
    EXPECT_EQ(value, unpacked);
}

TEST_F(AttachShieldCryptoTest, pack_with_empty_key_container) {
    const part_id::MultipleMessagePart value {"uid", "mid", {"hid1", "hid2"}};

    const std::string expectedPacked = "YWVzX3NpZDp7ImFlc0tleUlkIjoiNDIiLCJobWFjS2V5SWQiOiI0MiIsIml"
                                       "2QmFzZTY0IjoiUENzSDJKREVYSlJaMkQ2U3VsanZ2Z3ZvRnJZN28rUGdkOW"
                                       "gxdmY4Y3k0WT0iLCJzaWRCYXNlNjQiOiIyMy9kUjNnaEFpNWVIOFlBK0MwV"
                                       "E8vN3YwZFVkaC9wZ2s1NHNkVWtESjlmZkFjY2tJUGNGNTZhMjROWVQ5RlZT"
                                       "TWlYTStuemZleEFvU2NhVlN6R1FpYXpraXplRzAyblJEUVNESkRoTWJRUnB"
                                       "uc0FBRlU3S01ObDA0d2JWcTdmYyIsImhtYWNCYXNlNjQiOiJaV1NJQm00dW"
                                       "9TM05aYmxEZURmYmh3Ujh4Yloyc1hOVUtkNmlFSFlhYmhZPSJ9";
    const auto packed = packer(value);

    EXPECT_EQ(expectedPacked, packed);

    EXPECT_THROW(Unpacker(KeyContainer{})(packed), std::runtime_error);
}

TEST_F(AttachShieldCryptoTest, should_throw_exception_on_sid_packed_with_unknown_key) {
    const part_id::MultipleMessagePart value {"uid", "mid", {"hid1", "hid2"}};
    const auto packed = packer(value);
    KeyContainer invalidKeyContainer = {  {{"1", aesKey}}, {{"3", hmacKey}}  };
    EXPECT_THROW(Unpacker{invalidKeyContainer}(packed), std::runtime_error);
}

TEST(ConstructKeys, should_throw_exception_on_construction_with_unknown_key_ids) {
    KeyContainer invalidKeyContainer;
    EXPECT_THROW(Keys(invalidKeyContainer, "1", "2"), std::invalid_argument);
}

} // namespace

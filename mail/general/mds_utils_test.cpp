#include <mail/nwsmtp/src/mds/utils.h>
#include <mail/nwsmtp/src/utils.h>

#include <gtest/gtest.h>

using namespace NNwSmtp;
using NNwSmtp::NUtil::ToString;
using THeaders = THeaderStorage<TBufferRange>;
using std::literals::string_literals::operator""s;

constexpr char CRLF[] = "\r\n";

TEST(MdsMakeMessage, Basic) {
    std::string originalMessage{"Header: 1\r\n\r\nBody"};
    std::string addedHeaders{"AddedHeader-1: 1\r\nAddedHeader-2: 1\r\n"};
    std::string mdsMessage{"AddedHeader-1: 1\r\nAddedHeader-2: 1\r\nHeader: 1\r\n\r\nBody"};

    auto buffer = NUtil::MakeSegment(originalMessage);
    auto addedHeadersBuffer = NUtil::MakeSegment(addedHeaders);
    auto [headers, body] = ParseMessage(buffer);
    ASSERT_EQ(headers.Size(), 1ul);
    ASSERT_TRUE(body);

    auto message = NMds::MakeMessage(headers, addedHeadersBuffer, body, {});
    ASSERT_EQ(message, mdsMessage);
}

TEST(MdsMakeMessage, OnlyHeaders) {
    std::string originalMessage{"Header: 1\r\nHeader: 2\r\n"};
    std::string addedHeaders;

    auto buffer = NUtil::MakeSegment(originalMessage);
    auto addedHeadersBuffer = NUtil::MakeSegment(addedHeaders);
    auto [headers, body] = ParseMessage(buffer);
    ASSERT_EQ(headers.Size(), 2ul);
    ASSERT_FALSE(body);

    auto message = NMds::MakeMessage(headers, addedHeadersBuffer, body, {});
    ASSERT_EQ(message, originalMessage + CRLF);
}

TEST(MdsMakeMessage, OnlyBody) {
    std::string originalMessage{"Body"};
    std::string addedHeaders;
    auto buffer = NUtil::MakeSegment(originalMessage);
    auto addedHeadersBuffer = NUtil::MakeSegment(addedHeaders);
    TBufferRange body{buffer.cbegin(), buffer.cend()};
    ASSERT_THROW(NMds::MakeMessage(THeaders{}, addedHeadersBuffer, body, {}), std::runtime_error);
}

TEST(MdsMakeMessage, RemoveSingleHeader) {
    std::string originalMessage{"Header-1: 1\r\nHeader-2: 2\r\n\r\nBody"};
    std::string addedHeaders;

    auto buffer = NUtil::MakeSegment(originalMessage);
    auto addedHeadersBuffer = NUtil::MakeSegment(addedHeaders);
    auto [headers, body] = ParseMessage(buffer);
    ASSERT_EQ(headers.Size(), 2ul);
    ASSERT_TRUE(body);

    auto message = NMds::MakeMessage(headers, addedHeadersBuffer, body, {"header-1"s});
    ASSERT_EQ(message, "Header-2: 2\r\n\r\nBody"s);
}

TEST(MdsMakeMessage, RemoveAllHeaders) {
    std::string originalMessage{"Header-1: 1\r\nHeader-2: 2\r\n\r\nBody"};
    std::string addedHeaders;

    auto buffer = NUtil::MakeSegment(originalMessage);
    auto addedHeadersBuffer = NUtil::MakeSegment(addedHeaders);
    auto [headers, body] = ParseMessage(buffer);
    ASSERT_EQ(headers.Size(), 2ul);
    ASSERT_TRUE(body);

    ASSERT_THROW(NMds::MakeMessage(headers, addedHeadersBuffer, body, {"header-1"s, "header-2"s}), std::runtime_error);
}

TEST(MdsMakeMessage, EmptyMessage) {
    std::string addedHeaders;
    auto addedHeadersBuffer = NUtil::MakeSegment(addedHeaders);
    ASSERT_THROW(NMds::MakeMessage(THeaders{}, addedHeadersBuffer, TBufferRange{}, {}), std::runtime_error);
}

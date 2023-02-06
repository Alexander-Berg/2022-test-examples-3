#include <mail/notsolitesrv/src/session.h>
#include <gtest/gtest.h>

using namespace testing;
using namespace NNotSoLiteSrv;

using TAddrV4 = boost::asio::ip::address_v4;
using TAddrV6 = boost::asio::ip::address_v6;
using TResult = std::pair<std::string, std::string>;

TEST(GetRemoteAddressString, IPv4) {
    EXPECT_EQ(GetRemoteAddressString(TAddrV4((127 << 24) + 1)), TResult("127.0.0.1", "1.0.0.127.in-addr.arpa."));
}

TEST(GetRemoteAddressString, IPv6) {
    std::array<uint8_t, 16> addr{{
        0x2a, 0x02, 0x06, 0xb8, 0x00, 0x00, 0x04, 0x08,
        0x44, 0x0d, 0xb2, 0x24, 0xbc, 0x05, 0x49, 0x4d
    }};
    EXPECT_EQ(GetRemoteAddressString(TAddrV6(addr, 0x00)),
        TResult("2a02:6b8:0:408:440d:b224:bc05:494d", "d.4.9.4.5.0.c.b.4.2.2.b.d.0.4.4.8.0.4.0.0.0.0.0.8.b.6.0.2.0.a.2.ip6.arpa."));
}

TEST(GetRemoteAddressString, IPv6Folded) {
    std::array<uint8_t, 16> addr{{
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01
    }};
    EXPECT_EQ(GetRemoteAddressString(TAddrV6(addr, 0x00)),
        TResult("::1", "1.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.ip6.arpa."));

    addr[0] = 0x01;
    EXPECT_EQ(GetRemoteAddressString(TAddrV6(addr, 0x00)),
        TResult("100::1", "1.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.1.0.ip6.arpa."));
}

TEST(GetRemoteAddressString, IPv4MappedIntoV6) {
    std::array<uint8_t, 16> addr{{
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0xff, 0xff, 0x8d, 0x08, 0x94, 0x1f
    }};
    EXPECT_EQ(GetRemoteAddressString(TAddrV6(addr, 0x00)), TResult("141.8.148.31", "31.148.8.141.in-addr.arpa."));
}

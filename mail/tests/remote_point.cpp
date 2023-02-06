#include <gtest/gtest.h>
#include <mail/sova/include/nwsmtp/remote_point.h>

TEST(remote_point, url) {
    std::string str = "http://abook.yandex.net/compat/colabook_feed_addrdb";
    remote_point rp;
    ASSERT_TRUE(parse_remotepoint(str, rp));
    ASSERT_EQ(rp.proto_, "http");
    ASSERT_EQ(rp.host_name_, "abook.yandex.net");
    ASSERT_EQ(rp.port_, 0UL);
    ASSERT_EQ(rp.url_, "/compat/colabook_feed_addrdb");
}

TEST(remote_point, url_with_port) {
    std::string str = "smtp://mxbacks.mail.yandex.net:25";
    remote_point rp;
    ASSERT_TRUE(parse_remotepoint(str, rp));
    ASSERT_EQ(rp.proto_, "smtp");
    ASSERT_EQ(rp.host_name_, "mxbacks.mail.yandex.net");
    ASSERT_EQ(rp.port_, 25UL);
    ASSERT_EQ(rp.url_, "/");
}

TEST(remote_point, hostname_with_port) {
    std::string str = "so-in.yandex.ru:2525";
    remote_point rp;
    ASSERT_TRUE(parse_remotepoint(str, rp));
    ASSERT_EQ(rp.host_name_, "so-in.yandex.ru");
    ASSERT_EQ(rp.port_, 2525UL);
    ASSERT_EQ(rp.url_, "/");
}

TEST(remote_point, ipv4_address_with_port) {
    std::string str = "127.0.0.1:443";
    remote_point rp;
    ASSERT_TRUE(parse_remotepoint(str, rp));
    ASSERT_EQ(rp.host_name_, "127.0.0.1");
    ASSERT_EQ(rp.port_, 443UL);
    ASSERT_EQ(rp.url_, "/");
}

TEST(remote_point, ipv6_address_with_port) {
    std::string str = "[::1]:443";
    remote_point rp;
    ASSERT_TRUE(parse_remotepoint(str, rp));
    ASSERT_EQ(rp.host_name_, "::1");
    ASSERT_EQ(rp.port_, 443UL);
    ASSERT_EQ(rp.url_, "/");
}

TEST(from_string, url_with_proto_and_port) {
    std::string str = "http://abook.yandex.net:8080/compat/colabook_feed_addrdb";
    remote_point rp = remote_point::from_string(str);
    ASSERT_EQ(rp.proto_, "http");
    ASSERT_EQ(rp.host_name_, "abook.yandex.net");
    ASSERT_EQ(rp.port_, 8080UL);
    ASSERT_EQ(rp.url_, "/compat/colabook_feed_addrdb");
}

TEST(from_string, no_port_specfied_fallback_to_http_80) {
    std::string str = "http://abook.yandex.net/compat/colabook_feed_addrdb";
    remote_point rp = remote_point::from_string(str);
    ASSERT_EQ(rp.proto_, "http");
    ASSERT_EQ(rp.host_name_, "abook.yandex.net");
    ASSERT_EQ(rp.port_, 80UL);
    ASSERT_EQ(rp.url_, "/compat/colabook_feed_addrdb");
}

TEST(from_string, no_proto_and_no_port_specified_throws) {
    std::string str = "abook.yandex.net/compat/colabook_feed_addrdb";
    EXPECT_THROW(remote_point::from_string(str), std::runtime_error);
}

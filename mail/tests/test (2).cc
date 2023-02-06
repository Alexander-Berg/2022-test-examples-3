#include <gtest/gtest.h>
#include <gmock/gmock.h>
#include <mail/webmail/commondb/include/logger.h>
#include <mail/webmail/commondb/include/settings.h>
#include <mail/webmail/commondb/include/settings_reflection.h>
#include <yplatform/application/config/loader.h>

using namespace ::testing;

namespace commondb {

std::string pgSettings(bool targetSessionAttrs) {
    const std::string yaml = R"(
        log_pa: true
        max_connections: 10
        max_total_pools_capacity: 20
        queue_timeout_ms: 100
        connect_timeout_ms: 200
        query_timeout_ms: 300
        query_conf: '/path/to/query.conf'
        async_resolve: true
        ipv6_only: true
        dns_cache_ttl_sec: 120
        user: 'sendbernar'
        password_file: '/path/to/password'
        connection_string: 'host=localhost dbname=cachedb.cache port=6432)";

    return yaml + (targetSessionAttrs ? " target_session_attrs=any'" : "'");
}

TEST(Settings, shouldParsePgSettings) {
    yplatform::ptree cfg;
    utils::config::loader::from_str(pgSettings(false), cfg);

    const PgSettings pg {
        .log_pa=true,
        .max_connections=10ul,
        .max_total_pools_capacity=20ul,
        .queue_timeout_ms=pgg::Milliseconds{100},
        .connect_timeout_ms=pgg::Milliseconds{200},
        .query_timeout_ms=pgg::Milliseconds{300},
        .query_conf="/path/to/query.conf",
        .connection_string="host=localhost dbname=cachedb.cache port=6432",
        .user="sendbernar",
        .password_file=std::make_optional<std::string>("/path/to/password"),
        .async_resolve=true,
        .ipv6_only=true,
        .dns_cache_ttl_sec=pgg::Seconds(120)
    };

    EXPECT_EQ(readPgSettings(cfg), pg);
}

TEST(Settings, shouldThrowAnExceptionInCaseOfConnectionStringWithTargetSessionAttr) {
    yplatform::ptree cfg;
    utils::config::loader::from_str(pgSettings(true), cfg);

    EXPECT_THROW(readPgSettings(cfg), std::runtime_error);
}

TEST(HidePassword, shouldHidePassword) {
    std::string info = "string with password ";
    hidePassword(info, "password");
    EXPECT_EQ(info, "string with ******** ");
}

TEST(HidePassword, shouldNotChangeString) {
    std::string info = "string with password";
    hidePassword(info, "");
    EXPECT_EQ(info, info);
}

}

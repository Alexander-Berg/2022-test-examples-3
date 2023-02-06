#include "mod_apns/p8_bearer.h"
#include "mock_clock.h"
#include <catch.hpp>

namespace yxiva::mobile::apns {

using p8_bearer_mocked = p8_bearer_impl<mock_clock>;

static const string key = R"(
-----BEGIN PRIVATE KEY-----
MIGTAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBHkwdwIBAQQgir767IOFOYHsYtNQ
wsvLeJVu3bxCLL/SURQvMZw6QumgCgYIKoZIzj0DAQehRANCAARuwGOLtHY99zLl
iyACJp6xmj6YfE8bOLxHTZGkoC/+yNgf/fBpwf5Nin2pzyM8FUOYXg1R1v2bQqJy
wHYtSkc1
-----END PRIVATE KEY-----)";

struct T_p8_bearer
{
    T_p8_bearer() : token(key, "key_id", "issuer_key", "topic", "development")
    {
        settings.p8_lifetime = minutes(40);
        settings.p8_lifetime_gap = minutes(5);

        auto webserver_ctx = boost::make_shared<ymod_webserver::context>();
        auto req = boost::make_shared<ymod_webserver::request>(webserver_ctx);
        ctx = boost::make_shared<mobile_task_context>(req);

        mock_clock::set_now(1000);
        bearer = std::make_unique<p8_bearer_mocked>(token, settings);
    }

    apns_settings settings;
    p8_token token;
    mobile_task_context_ptr ctx;
    std::unique_ptr<p8_bearer_mocked> bearer;
};

TEST_CASE_METHOD(T_p8_bearer, "p8_bearer/does_update_when_expires", "")
{

    string sign1 = bearer->get(ctx);
    mock_clock::set_now(1000 + 60 * (40 + 6));
    string sign2 = bearer->get(ctx);
    REQUIRE(sign1 != sign2);
}

TEST_CASE_METHOD(T_p8_bearer, "p8_bearer/does_not_update_when_not_expires", "")
{
    string sign1 = bearer->get(ctx);
    mock_clock::set_now(1000 + 60 * (40 - 6));
    string sign2 = bearer->get(ctx);
    REQUIRE(sign1 == sign2);
}

}

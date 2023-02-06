#include "apns_test_certificates.h"
#include "web/webui/app_config_transformer.h"
#include <yxiva/core/platforms.h>
#include <yxiva/core/packing.hpp>
#include <yplatform/encoding/base64.h>
#include <yplatform/zerocopy/streambuf.h>
#include <catch.hpp>
#include <functional>
#include <map>
#include <optional>
#include <variant>

namespace yxiva::web::webui {

namespace cert = yxiva::web::webui::test_certificates;

struct T_app_config_transformer
{
    typedef application_config (app_config_transformer::*app_config_transformer_method)(
        const application_config&) const;

    void check_apply_ok(
        const json_value& json,
        const application_config& old_conf,
        const std::optional<application_config>& exp_conf = std::nullopt)
    {
        check_method_ok(&app_config_transformer::apply, json, old_conf, exp_conf);
    }

    void check_revoke_ok(
        const json_value& json,
        const application_config& old_conf,
        const std::optional<application_config>& exp_conf = std::nullopt)
    {
        check_method_ok(&app_config_transformer::revoke, json, old_conf, exp_conf);
    }

    void check_revert_ok(
        const json_value& json,
        const application_config& old_conf,
        const std::optional<application_config>& exp_conf = std::nullopt)
    {
        check_method_ok(&app_config_transformer::revert, json, old_conf, exp_conf);
    }

    void check_all_methods_fail(
        const json_value& json,
        const application_config& old_conf,
        const string& exp_err)
    {
        check_apply_fail(json, old_conf, exp_err);
        check_revoke_fail(json, old_conf, exp_err);
        check_revert_fail(json, old_conf, exp_err);
    }

    void check_apply_fail(
        const json_value& json,
        const application_config& old_conf,
        const string& exp_err)
    {
        check_method_fail(&app_config_transformer::apply, json, old_conf, exp_err);
    }

    void check_revoke_fail(
        const json_value& json,
        const application_config& old_conf,
        const string& exp_err)
    {
        check_method_fail(&app_config_transformer::revoke, json, old_conf, exp_err);
    }

    void check_revert_fail(
        const json_value& json,
        const application_config& old_conf,
        const string& exp_err)
    {
        check_method_fail(&app_config_transformer::revert, json, old_conf, exp_err);
    }

    void check_method_ok(
        app_config_transformer_method f,
        const json_value& json,
        const application_config& old_conf,
        const std::optional<application_config>& exp_conf)
    {
        auto req = make_json_request(json);
        application_config conf;
        REQUIRE_NOTHROW(conf = std::invoke(f, app_config_transformer(req), old_conf));
        if (exp_conf)
        {
            REQUIRE(conf.bb_client_id == exp_conf->bb_client_id);
            REQUIRE(conf.xiva_service == exp_conf->xiva_service);
            REQUIRE(conf.platform == exp_conf->platform);
            REQUIRE(conf.app_name == exp_conf->app_name);
            REQUIRE(conf.ttl == exp_conf->ttl);
            REQUIRE(conf.secret_key == exp_conf->secret_key);
            REQUIRE(conf.expiration_time == exp_conf->expiration_time);
            REQUIRE(conf.key_backup == exp_conf->key_backup);
            REQUIRE(conf.environment == exp_conf->environment);
            REQUIRE(conf.updated_at >= exp_conf->updated_at - 1);
            REQUIRE(conf.updated_at <= exp_conf->updated_at + 1);
        }
    }

    void check_method_fail(
        app_config_transformer_method f,
        const json_value& json,
        const application_config& old_conf,
        const string& exp_err)
    {
        auto req = make_json_request(json);
        REQUIRE_THROWS_WITH(
            std::invoke(f, app_config_transformer(req), old_conf), Catch::Contains(exp_err));
    }

    ymod_webserver::request_ptr make_json_request(const json_value& json)
    {
        auto req = boost::make_shared<ymod_webserver::request>();
        req->context = boost::make_shared<ymod_webserver::context>();
        req->content.type = "application";
        req->content.subtype = "json";
        req->raw_body = to_zerocopy_segment(json.stringify());
        return req;
    }

    yplatform::zerocopy::segment to_zerocopy_segment(const string& str)
    {
        yplatform::zerocopy::streambuf streambuf;
        auto buffers = streambuf.prepare(str.size());
        size_t offset = 0;
        for (auto& buffer : buffers)
        {
            std::copy(
                str.begin() + offset,
                str.begin() + offset + buffer.size(),
                boost::asio::buffers_begin(buffer));
            offset += buffer.size();
        }
        streambuf.commit(str.size());
        return streambuf.detach(streambuf.end());
    }

    using value_type = std::variant<string, int>;
    json_value make_request_json(std::map<string, value_type> content)
    {
        json_value json;
        for (const auto& [key, val] : content)
        {
            if (std::holds_alternative<string>(val))
            {
                json[key] = std::get<string>(val);
            }
            else if (std::holds_alternative<int>(val))
            {
                json[key] = std::get<int>(val);
            }
        }
        return json;
    }
};

TEST_CASE_METHOD(T_app_config_transformer, "app_config_transformer/missing_required_fields", "")
{
    auto json = make_request_json({ { "app_name", "app" }, { "service", "mail" } });

    check_all_methods_fail(json, {}, "missing or invalid value");
}

TEST_CASE_METHOD(
    T_app_config_transformer,
    "app_config_transformer/wrong_type_of_required_fields",
    "")
{
    auto json = make_request_json({ { "app_name", "app" },
                                    { "service", 123 },
                                    { "platform", "gcm" },
                                    { "environment", "auto" } });

    check_all_methods_fail(json, {}, "missing or invalid value");
}

TEST_CASE_METHOD(
    T_app_config_transformer,
    "app_config_transformer/app_name_and_platform_getters",
    "")
{
    auto json = make_request_json({ { "app_name", "test.app" },
                                    { "service", "service" },
                                    { "platform", "wns" },
                                    { "environment", "production" } });
    auto req = make_json_request(json);
    app_config_transformer transformer(req);

    REQUIRE(transformer.app_name() == "test.app");
    REQUIRE(transformer.platform() == "wns");
}

TEST_CASE_METHOD(T_app_config_transformer, "app_config_transformer/platform/unsupported", "")
{
    auto json = make_request_json({ { "app_name", "app" },
                                    { "service", "service" },
                                    { "platform", "xyz" },
                                    { "environment", "production" } });

    check_all_methods_fail(json, {}, "unsupported platform");
}

TEST_CASE_METHOD(
    T_app_config_transformer,
    "app_config_transformer/platform/fcm/wrong_type_of_fields",
    "")
{
    auto json = make_request_json({ { "app_name", "app" },
                                    { "service", "service" },
                                    { "platform", "fcm" },
                                    { "environment", "production" },
                                    { "apikey", 567 } });

    check_all_methods_fail(json, {}, "missing or invalid value");
}

TEST_CASE_METHOD(
    T_app_config_transformer,
    "app_config_transformer/platform/fcm/apply_revoke_revert",
    "")
{
    application_config old_config_template;
    old_config_template.xiva_service = "auto.ru";
    old_config_template.platform = "gcm";
    old_config_template.app_name = "fcm.app";
    old_config_template.ttl = 0;
    old_config_template.secret_key = "current_apikey";
    old_config_template.key_backup = "backup_apikey";
    old_config_template.environment = app_environment::automatic;
    old_config_template.updated_at = std::time(nullptr);

    auto json = make_request_json({ { "app_name", "fcm.app" },
                                    { "service", "mail" },
                                    { "platform", "fcm" },
                                    { "environment", "production" },
                                    { "apikey", "new_apikey" } });

    SECTION("app_config_transformer/platform/fcm/apply")
    {
        application_config exp_conf;
        exp_conf.xiva_service = "mail";
        exp_conf.platform = "gcm";
        exp_conf.app_name = "fcm.app";
        exp_conf.ttl = 0;
        exp_conf.secret_key = "new_apikey";
        exp_conf.key_backup = "current_apikey";
        exp_conf.environment = app_environment::production;
        exp_conf.updated_at = std::time(nullptr);

        check_apply_ok(json, old_config_template, exp_conf);
    }

    SECTION("app_config_transformer/platform/fcm/revokes")
    {
        application_config exp_conf;
        exp_conf.xiva_service = "auto.ru";
        exp_conf.platform = "gcm";
        exp_conf.app_name = "fcm.app";
        exp_conf.ttl = 0;
        exp_conf.secret_key = "";
        exp_conf.key_backup = "current_apikey";
        exp_conf.environment = app_environment::automatic;
        exp_conf.updated_at = std::time(nullptr);

        SECTION("app_config_transformer/platform/fcm/revoke_once")
        {
            check_revoke_ok(json, old_config_template, exp_conf);
        }

        SECTION("app_config_transformer/platform/fcm/revoke_twice")
        {
            check_revoke_ok(json, exp_conf, exp_conf);
        }
    }

    SECTION("app_config_transformer/platform/fcm/reverts")
    {
        application_config exp_conf;
        exp_conf.xiva_service = "auto.ru";
        exp_conf.platform = "gcm";
        exp_conf.app_name = "fcm.app";
        exp_conf.ttl = 0;
        exp_conf.secret_key = "backup_apikey";
        exp_conf.key_backup = "current_apikey";
        exp_conf.environment = app_environment::automatic;
        exp_conf.updated_at = std::time(nullptr);

        SECTION("app_config_transformer/platform/fcm/revert_once")
        {
            check_revert_ok(json, old_config_template, exp_conf);
        }

        SECTION("app_config_transformer/platform/fcm/revert_twice")
        {
            check_revert_ok(json, exp_conf, old_config_template);
        }
    }

    SECTION("app_config_transformer/platform/fcm/revert_without_backup")
    {
        application_config exp_conf;
        exp_conf.xiva_service = "auto.ru";
        exp_conf.platform = "gcm";
        exp_conf.app_name = "fcm.app";
        exp_conf.ttl = 0;
        exp_conf.secret_key = "backup_apikey";
        exp_conf.key_backup = "";
        exp_conf.environment = app_environment::automatic;
        exp_conf.updated_at = std::time(nullptr);

        check_revert_fail(json, exp_conf, "no backup record");
    }
}

TEST_CASE_METHOD(
    T_app_config_transformer,
    "app_config_transformer/platform/apns/wrong_type_of_fields/pem",
    "")
{
    auto json = make_request_json({ { "app_name", "test.app" },
                                    { "service", "service" },
                                    { "platform", "apns" },
                                    { "environment", "production" },
                                    { "cert", 908765 } });

    check_all_methods_fail(json, {}, "missing or invalid value");
}

TEST_CASE_METHOD(
    T_app_config_transformer,
    "app_config_transformer/platform/apns/wrong_type_of_fields/p8",
    "")
{
    auto json = make_request_json({ { "app_name", "test.app" },
                                    { "service", "service" },
                                    { "platform", "apns" },
                                    { "environment", "production" },
                                    { "issuer_key", 123 } });

    check_all_methods_fail(json, {}, "value");
}

TEST_CASE_METHOD(
    T_app_config_transformer,
    "app_config_transformer/platform/apns/invalid_combination_of_fields",
    "")
{
    auto json = make_request_json({ { "app_name", "test.app" },
                                    { "service", "service" },
                                    { "platform", "apns" },
                                    { "environment", "production" },
                                    { "cert", "pemp12" },
                                    { "key", "p8" } });

    check_all_methods_fail(json, {}, "invalid combination of apns fields");
}

TEST_CASE_METHOD(
    T_app_config_transformer,
    "app_config_transformer/platform/apns/bad_base64_cert",
    "")
{
    auto json = make_request_json({ { "app_name", "test.app" },
                                    { "service", "service" },
                                    { "platform", "apns" },
                                    { "environment", "production" },
                                    { "cert", "some_sert?" } });

    check_all_methods_fail(json, {}, "bad base64");
}

TEST_CASE_METHOD(
    T_app_config_transformer,
    "app_config_transformer/platform/apns/pem/apply_revoke_revert",
    "")
{
    application_config old_config_template;
    old_config_template.xiva_service = "auto.ru";
    old_config_template.platform = "apns";
    old_config_template.app_name = "apns.app";
    old_config_template.ttl = 0;
    old_config_template.secret_key = cert::current::pem;
    old_config_template.expiration_time = 1896189474;
    old_config_template.key_backup = cert::backup::pem;
    old_config_template.environment = app_environment::automatic;
    old_config_template.updated_at = std::time(nullptr);

    auto json = make_request_json({ { "app_name", "apns.app" },
                                    { "service", "mail" },
                                    { "platform", "apns" },
                                    { "environment", "auto" },
                                    { "cert", yplatform::base64_encode_str(cert::fresh::pem) } });

    SECTION("app_config_transformer/platform/apns/pem/apply")
    {
        application_config exp_conf;
        exp_conf.xiva_service = "mail";
        exp_conf.platform = "apns";
        exp_conf.app_name = "apns.app";
        exp_conf.ttl = 0;
        exp_conf.secret_key = cert::fresh::pem;
        exp_conf.expiration_time = 1896189675;
        exp_conf.key_backup = cert::current::pem;
        exp_conf.environment = app_environment::automatic;
        exp_conf.updated_at = std::time(nullptr);

        check_apply_ok(json, old_config_template, exp_conf);
    }

    SECTION("app_config_transformer/platform/apns/pem/revokes")
    {
        application_config exp_conf;
        exp_conf.xiva_service = "auto.ru";
        exp_conf.platform = "apns";
        exp_conf.app_name = "apns.app";
        exp_conf.ttl = 0;
        exp_conf.secret_key = "";
        exp_conf.expiration_time = 0;
        exp_conf.key_backup = cert::current::pem;
        exp_conf.environment = app_environment::automatic;
        exp_conf.updated_at = std::time(nullptr);

        SECTION("app_config_transformer/platform/apns/pem/revoke_once")
        {
            check_revoke_ok(json, old_config_template, exp_conf);
        }

        SECTION("app_config_transformer/platform/apns/pem/revoke_twice")
        {
            check_revoke_ok(json, exp_conf, exp_conf);
        }
    }

    SECTION("app_config_transformer/platform/apns/pem/reverts")
    {
        application_config exp_conf;
        exp_conf.xiva_service = "auto.ru";
        exp_conf.platform = "apns";
        exp_conf.app_name = "apns.app";
        exp_conf.ttl = 0;
        exp_conf.secret_key = cert::backup::pem;
        exp_conf.expiration_time = 1896189477;
        exp_conf.key_backup = cert::current::pem;
        exp_conf.environment = app_environment::automatic;
        exp_conf.updated_at = std::time(nullptr);

        SECTION("app_config_transformer/platform/apns/pem/revert_once")
        {
            check_revert_ok(json, old_config_template, exp_conf);
        }

        SECTION("app_config_transformer/platform/apns/pem/revert_twice")
        {
            check_revert_ok(json, exp_conf, old_config_template);
        }
    }

    SECTION("app_config_transformer/platform/apns/pem/revert_without_backup")
    {
        application_config exp_conf;
        exp_conf.xiva_service = "auto.ru";
        exp_conf.platform = "apns";
        exp_conf.app_name = "apns.app";
        exp_conf.ttl = 0;
        exp_conf.secret_key = cert::backup::pem;
        exp_conf.expiration_time = 1896189477;
        exp_conf.key_backup = "";
        exp_conf.environment = app_environment::automatic;
        exp_conf.updated_at = std::time(nullptr);

        check_revert_fail(json, exp_conf, "no backup record");
    }
}

TEST_CASE_METHOD(
    T_app_config_transformer,
    "app_config_transformer/platform/apns/p12/apply_revoke_revert",
    "")
{
    application_config old_config_template;
    old_config_template.xiva_service = "auto.ru";
    old_config_template.platform = "apns";
    old_config_template.app_name = "apns.app";
    old_config_template.ttl = 0;
    old_config_template.secret_key = cert::current::pem;
    old_config_template.expiration_time = 1896189474;
    old_config_template.key_backup = cert::backup::pem;
    old_config_template.environment = app_environment::automatic;
    old_config_template.updated_at = std::time(nullptr);

    auto json = make_request_json({ { "app_name", "apns.app" },
                                    { "service", "mail" },
                                    { "platform", "apns" },
                                    { "environment", "auto" },
                                    { "cert", yplatform::base64_encode_str(cert::fresh::p12) },
                                    { "cert-pass", "password" } });

    SECTION("app_config_transformer/platform/apns/p12/apply")
    {
        application_config exp_conf;
        exp_conf.xiva_service = "mail";
        exp_conf.platform = "apns";
        exp_conf.app_name = "apns.app";
        exp_conf.ttl = 0;
        exp_conf.secret_key = cert::fresh::pem;
        exp_conf.expiration_time = 1896189675;
        exp_conf.key_backup = cert::current::pem;
        exp_conf.environment = app_environment::automatic;
        exp_conf.updated_at = std::time(nullptr);

        check_apply_ok(json, old_config_template, exp_conf);
    }

    SECTION("app_config_transformer/platform/apns/p12/revokes")
    {
        application_config exp_conf;
        exp_conf.xiva_service = "auto.ru";
        exp_conf.platform = "apns";
        exp_conf.app_name = "apns.app";
        exp_conf.ttl = 0;
        exp_conf.secret_key = "";
        exp_conf.expiration_time = 0;
        exp_conf.key_backup = cert::current::pem;
        exp_conf.environment = app_environment::automatic;
        exp_conf.updated_at = std::time(nullptr);

        SECTION("app_config_transformer/platform/apns/p12/revoke_once")
        {
            check_revoke_ok(json, old_config_template, exp_conf);
        }

        SECTION("app_config_transformer/platform/apns/p12/revoke_twice")
        {
            check_revoke_ok(json, exp_conf, exp_conf);
        }
    }

    SECTION("app_config_transformer/platform/apns/p12/reverts")
    {
        application_config exp_conf;
        exp_conf.xiva_service = "auto.ru";
        exp_conf.platform = "apns";
        exp_conf.app_name = "apns.app";
        exp_conf.ttl = 0;
        exp_conf.secret_key = cert::backup::pem;
        exp_conf.expiration_time = 1896189477;
        exp_conf.key_backup = cert::current::pem;
        exp_conf.environment = app_environment::automatic;
        exp_conf.updated_at = std::time(nullptr);

        SECTION("app_config_transformer/platform/apns/p12/revert_once")
        {
            check_revert_ok(json, old_config_template, exp_conf);
        }

        SECTION("app_config_transformer/platform/apns/p12/revert_twice")
        {
            check_revert_ok(json, exp_conf, old_config_template);
        }
    }

    SECTION("app_config_transformer/platform/apns/p12/revert_without_backup")
    {
        application_config exp_conf;
        exp_conf.xiva_service = "auto.ru";
        exp_conf.platform = "apns";
        exp_conf.app_name = "apns.app";
        exp_conf.ttl = 0;
        exp_conf.secret_key = cert::backup::pem;
        exp_conf.expiration_time = 1896189477;
        exp_conf.key_backup = "";
        exp_conf.environment = app_environment::automatic;
        exp_conf.updated_at = std::time(nullptr);

        check_revert_fail(json, exp_conf, "no backup record");
    }
}

TEST_CASE_METHOD(
    T_app_config_transformer,
    "app_config_transformer/platform/apns/p8/apply_revoke_revert",
    "")
{
    application_config old_config_template;
    old_config_template.xiva_service = "auto.ru";
    old_config_template.platform = "apns";
    old_config_template.app_name = "apns.app";
    old_config_template.ttl = 0;
    old_config_template.secret_key = cert::current::p8.to_string();
    old_config_template.expiration_time = 0;
    old_config_template.key_backup = cert::backup::p8.to_string();
    old_config_template.environment = app_environment::automatic;
    old_config_template.updated_at = std::time(nullptr);

    auto json = make_request_json({ { "app_name", "apns.app" },
                                    { "service", "mail" },
                                    { "platform", "apns" },
                                    { "environment", "auto" },
                                    { "key", yplatform::base64_encode_str(cert::fresh::p8.key) },
                                    { "key_id", cert::fresh::p8.key_id },
                                    { "issuer_key", cert::fresh::p8.issuer_key },
                                    { "topic", cert::fresh::p8.topic },
                                    { "type", cert::fresh::p8.type } });

    SECTION("app_config_transformer/platform/apns/p8/apply")
    {
        application_config exp_conf;
        exp_conf.xiva_service = "mail";
        exp_conf.platform = "apns";
        exp_conf.app_name = "apns.app";
        exp_conf.ttl = 0;
        exp_conf.secret_key = cert::fresh::p8.to_string();
        exp_conf.expiration_time = 0;
        exp_conf.key_backup = cert::current::p8.to_string();
        exp_conf.environment = app_environment::automatic;
        exp_conf.updated_at = std::time(nullptr);

        check_apply_ok(json, old_config_template, exp_conf);
    }

    SECTION("app_config_transformer/platform/apns/p8/revokes")
    {
        application_config exp_conf;
        exp_conf.xiva_service = "auto.ru";
        exp_conf.platform = "apns";
        exp_conf.app_name = "apns.app";
        exp_conf.ttl = 0;
        exp_conf.secret_key = "";
        exp_conf.expiration_time = 0;
        exp_conf.key_backup = cert::current::p8.to_string();
        exp_conf.environment = app_environment::automatic;
        exp_conf.updated_at = std::time(nullptr);

        SECTION("app_config_transformer/platform/apns/p8/revoke_once")
        {
            check_revoke_ok(json, old_config_template, exp_conf);
        }

        SECTION("app_config_transformer/platform/apns/p8/revoke_twice")
        {
            check_revoke_ok(json, exp_conf, exp_conf);
        }
    }

    SECTION("app_config_transformer/platform/apns/p8/reverts")
    {
        application_config exp_conf;
        exp_conf.xiva_service = "auto.ru";
        exp_conf.platform = "apns";
        exp_conf.app_name = "apns.app";
        exp_conf.ttl = 0;
        exp_conf.secret_key = cert::backup::p8.to_string();
        exp_conf.expiration_time = 0;
        exp_conf.key_backup = cert::current::p8.to_string();
        exp_conf.environment = app_environment::automatic;
        exp_conf.updated_at = std::time(nullptr);

        SECTION("app_config_transformer/platform/apns/p8/revert_once")
        {
            check_revert_ok(json, old_config_template, exp_conf);
        }

        SECTION("app_config_transformer/platform/apns/p8/revert_twice")
        {
            check_revert_ok(json, exp_conf, old_config_template);
        }
    }

    SECTION("app_config_transformer/platform/apns/p8/revert_without_backup")
    {
        application_config exp_conf;
        exp_conf.xiva_service = "auto.ru";
        exp_conf.platform = "apns";
        exp_conf.app_name = "apns.app";
        exp_conf.ttl = 0;
        exp_conf.secret_key = cert::backup::p8.to_string();
        exp_conf.expiration_time = 0;
        exp_conf.key_backup = "";
        exp_conf.environment = app_environment::automatic;
        exp_conf.updated_at = std::time(nullptr);

        check_revert_fail(json, exp_conf, "no backup record");
    }
}

TEST_CASE_METHOD(T_app_config_transformer, "app_config_transformer/platform/apns/revert/pem_p8", "")
{
    application_config old_config_template;
    old_config_template.xiva_service = "auto.ru";
    old_config_template.platform = "apns";
    old_config_template.app_name = "apns.app";
    old_config_template.ttl = 0;
    old_config_template.secret_key = cert::current::p8.to_string();
    old_config_template.expiration_time = 0;
    old_config_template.key_backup = cert::backup::pem;
    old_config_template.environment = app_environment::automatic;
    old_config_template.updated_at = std::time(nullptr);

    auto json = make_request_json({ { "app_name", "apns.app" },
                                    { "service", "mail" },
                                    { "platform", "apns" },
                                    { "environment", "auto" } });

    application_config exp_conf;
    exp_conf.xiva_service = "auto.ru";
    exp_conf.platform = "apns";
    exp_conf.app_name = "apns.app";
    exp_conf.ttl = 0;
    exp_conf.secret_key = cert::backup::pem;
    exp_conf.expiration_time = 1896189477;
    exp_conf.key_backup = cert::current::p8.to_string();
    exp_conf.environment = app_environment::automatic;
    exp_conf.updated_at = std::time(nullptr);

    SECTION("app_config_transformer/platform/apns/revert/p8_to_pem")
    {
        check_revert_ok(json, old_config_template, exp_conf);
    }

    SECTION("app_config_transformer/platform/apns/revert/pem_to_p8")
    {
        check_revert_ok(json, exp_conf, old_config_template);
    }
}

TEST_CASE_METHOD(
    T_app_config_transformer,
    "app_config_transformer/platform/apns/update/pem_to_p8",
    "")
{
    application_config old_config_template;
    old_config_template.xiva_service = "auto.ru";
    old_config_template.platform = "apns";
    old_config_template.app_name = "apns.app";
    old_config_template.ttl = 0;
    old_config_template.secret_key = cert::current::pem;
    old_config_template.expiration_time = 1896189474;
    old_config_template.key_backup = cert::backup::pem;
    old_config_template.environment = app_environment::automatic;
    old_config_template.updated_at = std::time(nullptr);

    auto json = make_request_json({ { "app_name", "apns.app" },
                                    { "service", "auto.ru" },
                                    { "platform", "apns" },
                                    { "environment", "auto" },
                                    { "key", yplatform::base64_encode_str(cert::fresh::p8.key) },
                                    { "key_id", cert::fresh::p8.key_id },
                                    { "issuer_key", cert::fresh::p8.issuer_key },
                                    { "topic", cert::fresh::p8.topic },
                                    { "type", cert::fresh::p8.type } });

    application_config exp_conf;
    exp_conf.xiva_service = "auto.ru";
    exp_conf.platform = "apns";
    exp_conf.app_name = "apns.app";
    exp_conf.ttl = 0;
    exp_conf.secret_key = cert::fresh::p8.to_string();
    exp_conf.expiration_time = 0;
    exp_conf.key_backup = cert::current::pem;
    exp_conf.environment = app_environment::automatic;
    exp_conf.updated_at = std::time(nullptr);

    check_apply_ok(json, old_config_template, exp_conf);
}

TEST_CASE_METHOD(
    T_app_config_transformer,
    "app_config_transformer/platform/apns/pem_with_zero_exp_to_p8/update",
    "")
{
    application_config old_config_template;
    old_config_template.xiva_service = "auto.ru";
    old_config_template.platform = "apns";
    old_config_template.app_name = "apns.app";
    old_config_template.ttl = 0;
    old_config_template.secret_key = cert::current::pem;
    old_config_template.expiration_time = 0;
    old_config_template.key_backup = "";
    old_config_template.environment = app_environment::automatic;
    old_config_template.updated_at = std::time(nullptr);

    auto json = make_request_json({ { "app_name", "apns.app" },
                                    { "service", "auto.ru" },
                                    { "platform", "apns" },
                                    { "environment", "auto" },
                                    { "key", yplatform::base64_encode_str(cert::fresh::p8.key) },
                                    { "key_id", cert::fresh::p8.key_id },
                                    { "issuer_key", cert::fresh::p8.issuer_key },
                                    { "topic", cert::fresh::p8.topic },
                                    { "type", cert::fresh::p8.type } });

    application_config exp_conf;
    exp_conf.xiva_service = "auto.ru";
    exp_conf.platform = "apns";
    exp_conf.app_name = "apns.app";
    exp_conf.ttl = 0;
    exp_conf.secret_key = cert::fresh::p8.to_string();
    exp_conf.expiration_time = 0;
    exp_conf.key_backup = cert::current::pem;
    exp_conf.environment = app_environment::automatic;
    exp_conf.updated_at = std::time(nullptr);

    check_apply_ok(json, old_config_template, exp_conf);
}

TEST_CASE_METHOD(
    T_app_config_transformer,
    "app_config_transformer/platform/apns/pem_with_zero_exp_to_p8/revert",
    "")
{
    application_config old_config_template;
    old_config_template.xiva_service = "auto.ru";
    old_config_template.platform = "apns";
    old_config_template.app_name = "apns.app";
    old_config_template.ttl = 0;
    old_config_template.secret_key = cert::fresh::p8.to_string();
    old_config_template.expiration_time = 0;
    old_config_template.key_backup = cert::current::pem;
    old_config_template.environment = app_environment::automatic;
    old_config_template.updated_at = std::time(nullptr);

    auto json = make_request_json({ { "app_name", "apns.app" },
                                    { "service", "auto.ru" },
                                    { "platform", "apns" },
                                    { "environment", "auto" } });

    application_config exp_conf;
    exp_conf.xiva_service = "auto.ru";
    exp_conf.platform = "apns";
    exp_conf.app_name = "apns.app";
    exp_conf.ttl = 0;
    exp_conf.secret_key = cert::current::pem;
    exp_conf.expiration_time = 1896189474;
    exp_conf.key_backup = cert::fresh::p8.to_string();
    exp_conf.environment = app_environment::automatic;
    exp_conf.updated_at = std::time(nullptr);

    check_revert_ok(json, old_config_template, exp_conf);
}

TEST_CASE_METHOD(
    T_app_config_transformer,
    "app_config_transformer/platform/wns/wrong_type_of_fields",
    "")
{
    auto json = make_request_json({ { "app_name", "test.app" },
                                    { "service", "service" },
                                    { "platform", "wns" },
                                    { "environment", "production" },
                                    { "sid", 3245 } });

    check_all_methods_fail(json, {}, "missing or invalid value");
}

TEST_CASE_METHOD(
    T_app_config_transformer,
    "app_config_transformer/platform/wns/apply_revoke_revert",
    "")
{
    application_config old_config_template;
    old_config_template.xiva_service = "auto.ru";
    old_config_template.platform = "wns";
    old_config_template.app_name = "wns.app";
    old_config_template.ttl = 0;
    old_config_template.secret_key = "current_sid\ncurrent_secret";
    old_config_template.key_backup = "backup_sid\nbackup_secret";
    old_config_template.environment = app_environment::automatic;
    old_config_template.updated_at = std::time(nullptr);

    auto json = make_request_json({ { "app_name", "wns.app" },
                                    { "service", "mail" },
                                    { "platform", "wns" },
                                    { "environment", "production" },
                                    { "sid", "new_sid" } });

    SECTION("app_config_transformer/platform/wns/apply")
    {
        application_config exp_conf;
        exp_conf.xiva_service = "mail";
        exp_conf.platform = "wns";
        exp_conf.app_name = "wns.app";
        exp_conf.ttl = 0;
        exp_conf.secret_key = "new_sid\ncurrent_secret";
        exp_conf.key_backup = "current_sid\ncurrent_secret";
        exp_conf.environment = app_environment::production;
        exp_conf.updated_at = std::time(nullptr);

        check_apply_ok(json, old_config_template, exp_conf);
    }

    SECTION("app_config_transformer/platform/wns/revokes")
    {
        application_config exp_conf;
        exp_conf.xiva_service = "auto.ru";
        exp_conf.platform = "wns";
        exp_conf.app_name = "wns.app";
        exp_conf.ttl = 0;
        exp_conf.secret_key = "";
        exp_conf.key_backup = "current_sid\ncurrent_secret";
        exp_conf.environment = app_environment::automatic;
        exp_conf.updated_at = std::time(nullptr);

        SECTION("app_config_transformer/platform/wns/revoke_once")
        {
            check_revoke_ok(json, old_config_template, exp_conf);
        }

        SECTION("app_config_transformer/platform/wns/revoke_twice")
        {
            check_revoke_ok(json, exp_conf, exp_conf);
        }
    }

    SECTION("app_config_transformer/platform/wns/reverts")
    {
        application_config exp_conf;
        exp_conf.xiva_service = "auto.ru";
        exp_conf.platform = "wns";
        exp_conf.app_name = "wns.app";
        exp_conf.ttl = 0;
        exp_conf.secret_key = "backup_sid\nbackup_secret";
        exp_conf.key_backup = "current_sid\ncurrent_secret";
        exp_conf.environment = app_environment::automatic;
        exp_conf.updated_at = std::time(nullptr);

        SECTION("app_config_transformer/platform/wns/revert_once")
        {
            check_revert_ok(json, old_config_template, exp_conf);
        }

        SECTION("app_config_transformer/platform/wns/revert_twice")
        {
            check_revert_ok(json, exp_conf, old_config_template);
        }
    }

    SECTION("app_config_transformer/platform/wns/revert_without_backup")
    {
        application_config exp_conf;
        exp_conf.xiva_service = "auto.ru";
        exp_conf.platform = "wns";
        exp_conf.app_name = "wns.app";
        exp_conf.ttl = 0;
        exp_conf.secret_key = "backup_sid\nbackup_secret";
        exp_conf.key_backup = "";
        exp_conf.environment = app_environment::automatic;
        exp_conf.updated_at = std::time(nullptr);

        check_revert_fail(json, exp_conf, "no backup record");
    }
}

TEST_CASE_METHOD(
    T_app_config_transformer,
    "app_config_transformer/platform/hms/wrong_type_of_fields",
    "")
{
    auto json = make_request_json({ { "app_name", "test.app" },
                                    { "service", "service" },
                                    { "platform", "hms" },
                                    { "environment", "production" },
                                    { "client_id", 7 } });

    check_all_methods_fail(json, {}, "missing or invalid value");
}

TEST_CASE_METHOD(
    T_app_config_transformer,
    "app_config_transformer/platform/hms/apply_revoke_revert",
    "")
{
    application_config old_config_template;
    old_config_template.xiva_service = "auto.ru";
    old_config_template.platform = "hms";
    old_config_template.app_name = "hms.app";
    old_config_template.ttl = 0;
    old_config_template.secret_key = "current_client_id\ncurrent_secret";
    old_config_template.key_backup = "backup_client_id\nbackup_secret";
    old_config_template.environment = app_environment::automatic;
    old_config_template.updated_at = std::time(nullptr);

    auto json = make_request_json({ { "app_name", "hms.app" },
                                    { "service", "mail" },
                                    { "platform", "hms" },
                                    { "environment", "production" },
                                    { "secret", "new_secret" } });

    SECTION("app_config_transformer/platform/hms/apply")
    {
        application_config exp_conf;
        exp_conf.xiva_service = "mail";
        exp_conf.platform = "hms";
        exp_conf.app_name = "hms.app";
        exp_conf.ttl = 0;
        exp_conf.secret_key = "current_client_id\nnew_secret";
        exp_conf.key_backup = "current_client_id\ncurrent_secret";
        exp_conf.environment = app_environment::production;
        exp_conf.updated_at = std::time(nullptr);

        check_apply_ok(json, old_config_template, exp_conf);
    }

    SECTION("app_config_transformer/platform/hms/revokes")
    {
        application_config exp_conf;
        exp_conf.xiva_service = "auto.ru";
        exp_conf.platform = "hms";
        exp_conf.app_name = "hms.app";
        exp_conf.ttl = 0;
        exp_conf.secret_key = "";
        exp_conf.key_backup = "current_client_id\ncurrent_secret";
        exp_conf.environment = app_environment::automatic;
        exp_conf.updated_at = std::time(nullptr);

        SECTION("app_config_transformer/platform/hms/revoke_once")
        {
            check_revoke_ok(json, old_config_template, exp_conf);
        }

        SECTION("app_config_transformer/platform/hms/revoke_twice")
        {
            check_revoke_ok(json, exp_conf, exp_conf);
        }
    }

    SECTION("app_config_transformer/platform/hms/reverts")
    {
        application_config exp_conf;
        exp_conf.xiva_service = "auto.ru";
        exp_conf.platform = "hms";
        exp_conf.app_name = "hms.app";
        exp_conf.ttl = 0;
        exp_conf.secret_key = "backup_client_id\nbackup_secret";
        exp_conf.key_backup = "current_client_id\ncurrent_secret";
        exp_conf.environment = app_environment::automatic;
        exp_conf.updated_at = std::time(nullptr);

        SECTION("app_config_transformer/platform/hms/revert_once")
        {
            check_revert_ok(json, old_config_template, exp_conf);
        }

        SECTION("app_config_transformer/platform/hms/revert_twice")
        {
            check_revert_ok(json, exp_conf, old_config_template);
        }
    }

    SECTION("app_config_transformer/platform/hms/revert_without_backup")
    {
        application_config exp_conf;
        exp_conf.xiva_service = "auto.ru";
        exp_conf.platform = "hms";
        exp_conf.app_name = "hms.app";
        exp_conf.ttl = 0;
        exp_conf.secret_key = "backup_client_id\nbackup_secret";
        exp_conf.key_backup = "";
        exp_conf.environment = app_environment::automatic;
        exp_conf.updated_at = std::time(nullptr);

        check_revert_fail(json, exp_conf, "no backup record");
    }
}

}

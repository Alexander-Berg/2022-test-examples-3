#include "idm_helpers.h"
#include "stream_mock.h"
#include "web/idm/idm.h"
#include "web/idm/add_role.h"
#include "web/idm/remove_role.h"
#include "service_manager/interface.h"
#include <ymod_httpclient/client.h>
#include <ymod_webserver/codes.h>
#include <ymod_webserver/request.h>
#include <catch.hpp>

using namespace boost::system;
using namespace yplatform;

namespace yxiva {

template <typename Mock>
std::shared_ptr<Mock> init_module_mock(const string& name)
{
    auto mock_ptr = std::dynamic_pointer_cast<Mock>(yplatform::repository::instance().find(name));
    if (!mock_ptr)
    {
        mock_ptr = std::make_shared<Mock>();
        yplatform::repository::instance().add_service<Mock>(name, mock_ptr);
    }
    return mock_ptr;
}

struct service_manager_mock
    : service_manager
    , yplatform::module
{
    std::shared_ptr<const service_data> find_service_by_send_token(
        const string& /*token*/) const override
    {
        throw std::runtime_error("not implemented");
    }

    std::shared_ptr<const service_data> find_service_by_listen_token(
        const string& /*token*/) const override
    {
        throw std::runtime_error("not implemented");
    }

    std::shared_ptr<const service_data> find_service_by_name(const string& name) const override
    {
        auto it = std::find_if(services.begin(), services.end(), [&](auto service_data) {
            return service_data->properties.name == name;
        });
        return it != services.end() ? *it : std::shared_ptr<const service_data>{};
    }

    std::shared_ptr<const application_config> find_app(
        const string& /*platform*/,
        const string& /*name*/) const override
    {
        throw std::runtime_error("not implemented");
    }

    void find_services_by_owner(
        const task_context_ptr& /*ctx*/,
        const string& /*owner*/,
        const services_callback_t& /*cb*/) override
    {
        throw std::runtime_error("not implemented");
    }

    void find_services_by_owners(
        const task_context_ptr& /*ctx*/,
        std::shared_ptr<const std::vector<string>> /*owners*/,
        const services_callback_t& /*cb*/) override
    {
        throw std::runtime_error("not implemented");
    }

    void get_all_services(const task_context_ptr& /*ctx*/, const services_callback_t& cb) override
    {
        cb(*result, services);
    }

    const string& environment() const override
    {
        throw std::runtime_error("not implemented");
    }

    void update_service_properties(
        task_context_ptr ctx,
        const service_properties& data,
        const update_callback_t& cb) override
    {
        auto replace_properties = [data](auto&&) { return std::move(data); };
        update_service_properties(ctx, data.name, std::move(replace_properties), cb);
    }

    void update_service_properties(
        task_context_ptr,
        const string& service_name,
        const service_properties_transform& transform,
        const update_callback_t& cb) override
    {
        auto it = std::find_if(services.begin(), services.end(), [&](const auto& service_data) {
            return service_data->properties.name == service_name;
        });
        if (it == services.end()) return cb("service doesn't exist");
        *it = make_service_data(transform((*it)->properties));
        cb(*result);
    }

    void update_send_token(
        task_context_ptr /*ctx*/,
        const string& /*env_str*/,
        const send_token_properties& /*data*/,
        const string& /*service_owner*/,
        const token_update_callback_t& /*cb*/) override
    {
        throw std::runtime_error("not implemented");
    }

    void update_listen_token(
        task_context_ptr /*ctx*/,
        const string& /*env_str*/,
        const listen_token_properties& /*data*/,
        const string& /*service_owner*/,
        const token_update_callback_t& /*cb*/) override
    {
        throw std::runtime_error("not implemented");
    }

    void create_service(
        task_context_ptr /*ctx*/,
        const service_properties& /*data*/,
        const create_service_callback_t& /*cb*/) override
    {
        throw std::runtime_error("not implemented");
    }

    void update_app(
        task_context_ptr /*ctx*/,
        const application_config& /*data*/,
        const string& /*service_owner*/,
        const update_callback_t& /*cb*/) override
    {
        throw std::runtime_error("not implemented");
    }

    void reset_mock(const operation::result& res = {})
    {
        result = std::make_shared<operation::result>(res);
        services = services_type{ make_service_data(
            "service1",
            "kinopoisk",
            { { "sandbox", { 1, "app1" } },
              { "corp", { 3, "app3" } },
              { "production", { 3, "app3" } } },
            { { "sandbox", { 4, "app4" } },
              { "corp", { 6, "app6" } },
              { "production", { 6, "app6" } } }) };
    }

    services_type services;
    std::shared_ptr<operation::result> result;
};

}

namespace ymod_httpclient {

struct http_client_mock
    : call
    , yplatform::module
{
    response run(task_context_ptr /*ctx*/, request /*req*/) override
    {
        throw std::runtime_error("not implemented");
    }

    response run(task_context_ptr /*ctx*/, request /*req*/, const options& /*options*/) override
    {
        throw std::runtime_error("not implemented");
    }

    void async_run(task_context_ptr /*ctx*/, request req, callback_type callback) override
    {
        requests.push_back(req);
        callback(err, resp);
    }

    void async_run(
        task_context_ptr /*ctx*/,
        request req,
        const options& /*options*/,
        callback_type callback) override
    {
        requests.push_back(req);
        callback(err, resp);
    }

    future_void_t get_url(
        task_context_ptr /*ctx*/,
        response_handler_ptr /*handler*/,
        const remote_point_info_ptr /*host*/,
        const string& /*req*/,
        const string& /*headers*/) override
    {
        throw std::runtime_error("not implemented");
    }

    future_void_t head_url(
        task_context_ptr /*ctx*/,
        response_handler_ptr /*handler*/,
        const remote_point_info_ptr /*host*/,
        const string& /*req*/,
        const string& /*headers*/) override
    {
        throw std::runtime_error("not implemented");
    }

    future_void_t post_url(
        task_context_ptr /*ctx*/,
        response_handler_ptr /*handler*/,
        const remote_point_info_ptr /*host*/,
        const string& /*req*/,
        const string_ptr& /*post*/,
        const string& /*headers*/,
        bool /*log_post_args*/) override
    {
        throw std::runtime_error("not implemented");
    }

    future_void_t mpost_url(
        task_context_ptr /*context*/,
        response_handler_ptr /*handler*/,
        const remote_point_info_ptr /*host*/,
        const string& /*req*/,
        post_chunks&& /*post*/,
        const string& /*headers*/,
        bool /*log_post_args*/) override
    {
        throw std::runtime_error("not implemented");
    }

    remote_point_info_ptr make_rm_info(const string& /*host*/) override
    {
        throw std::runtime_error("not implemented");
    }

    remote_point_info_ptr make_rm_info(const string& /*host*/, bool /*reuse_connection*/) override
    {
        throw std::runtime_error("not implemented");
    }

    remote_point_info_ptr make_rm_info(const string& /*host*/, const timeouts& /*timeouts*/)
        override
    {
        throw std::runtime_error("not implemented");
    }

    remote_point_info_ptr make_rm_info(
        const string& /*host*/,
        const timeouts& /*timeouts*/,
        bool /*reuse_connection*/) override
    {
        throw std::runtime_error("not implemented");
    }

    void reset_mock()
    {
        requests.clear();
        err = error_code();
        resp = { 200, {}, R"({"name": "molotok", "status": "ok"})", "OK" };
    }

    std::vector<request> requests;
    error_code err;
    response resp;
};

}

namespace yxiva::web::idm {

struct T_idm
{
    T_idm() : stream(std::make_shared<stream_mock>()), settings(std::make_shared<struct settings>())
    {
        service_manager_mock_ptr = init_module_mock<service_manager_mock>("service_manager_mock");
        service_manager_mock_ptr->reset_mock();

        http_client_mock_ptr =
            init_module_mock<ymod_httpclient::http_client_mock>("http_client_mock");
        http_client_mock_ptr->reset_mock();

        settings->service_manager = "service_manager_mock";
        settings->http_client = "http_client_mock";
        settings->tvm_info_url = "https://tvm.yandex-team.ru/client/%d/info";
    }

    const service_properties& properties(const string& service) const
    {
        return service_manager_mock_ptr->find_service_by_name(service)->properties;
    }

    std::shared_ptr<stream_mock> stream;
    std::shared_ptr<service_manager_mock> service_manager_mock_ptr;
    std::shared_ptr<ymod_httpclient::http_client_mock> http_client_mock_ptr;
    std::shared_ptr<struct settings> settings;
    json_value role;
};

TEST_CASE_METHOD(T_idm, "info/not_configured")
{
    settings->service_manager = "not_configured";

    REQUIRE_THROWS_WITH(
        info{ settings }(stream), Catch::Contains("module \"not_configured\" not found"));
}

TEST_CASE_METHOD(T_idm, "info/service_manager_error")
{
    service_manager_mock_ptr->reset_mock("service_manager_error");
    info{ settings }(stream);

    REQUIRE(*stream->code == 500);
    REQUIRE(*stream->body == "idm list task failed");
}

TEST_CASE_METHOD(T_idm, "info/ok")
{
    info{ settings }(stream);

    REQUIRE(*stream->code == 200);
    REQUIRE(*stream->body == R"({
    "code": 0,
    "roles": {
        "slug": "project",
        "name": "project",
        "values": {
            "service1": {
                "name": "service1",
                "roles": {
                    "slug": "environment",
                    "name": "environment",
                    "values": {
                        "sandbox": {
                            "name": "sandbox",
                            "roles": {
                                "slug": "role",
                                "name": "role",
                                "values": {
                                    "subscriber": "subscriber",
                                    "publisher": "publisher"
                                }
                            }
                        },
                        "corp": {
                            "name": "corp",
                            "roles": {
                                "slug": "role",
                                "name": "role",
                                "values": {
                                    "subscriber": "subscriber",
                                    "publisher": "publisher"
                                }
                            }
                        },
                        "production": {
                            "name": "production",
                            "roles": {
                                "slug": "role",
                                "name": "role",
                                "values": {
                                    "subscriber": "subscriber",
                                    "publisher": "publisher"
                                }
                            }
                        }
                    }
                },
                "aliases": [
                    {
                        "type": "default",
                        "name": "service1%%kinopoisk"
                    }
                ]
            }
        }
    }
})");
}

TEST_CASE_METHOD(T_idm, "get_all_roles/not_configured")
{
    settings->service_manager = "not_configured";

    REQUIRE_THROWS_WITH(
        get_all_roles{ settings }(stream), Catch::Contains("module \"not_configured\" not found"));
}

TEST_CASE_METHOD(T_idm, "get_all_roles/service_manager_error")
{
    service_manager_mock_ptr->reset_mock("service_manager_error");
    get_all_roles{ settings }(stream);

    REQUIRE(*stream->code == 500);
    REQUIRE(*stream->body == "idm list task failed");
}

TEST_CASE_METHOD(T_idm, "get_all_roles/ok")
{
    get_all_roles{ settings }(stream);

    REQUIRE(*stream->code == 200);
    REQUIRE(*stream->body == R"({
    "code": 0,
    "users": [
        {
            "login": "3",
            "subject_type": "tvm_app",
            "roles": [
                {
                    "project": "service1",
                    "environment": "corp",
                    "role": "publisher"
                }
            ]
        },
        {
            "login": "3",
            "subject_type": "tvm_app",
            "roles": [
                {
                    "project": "service1",
                    "environment": "production",
                    "role": "publisher"
                }
            ]
        },
        {
            "login": "1",
            "subject_type": "tvm_app",
            "roles": [
                {
                    "project": "service1",
                    "environment": "sandbox",
                    "role": "publisher"
                }
            ]
        },
        {
            "login": "6",
            "subject_type": "tvm_app",
            "roles": [
                {
                    "project": "service1",
                    "environment": "corp",
                    "role": "subscriber"
                }
            ]
        },
        {
            "login": "6",
            "subject_type": "tvm_app",
            "roles": [
                {
                    "project": "service1",
                    "environment": "production",
                    "role": "subscriber"
                }
            ]
        },
        {
            "login": "4",
            "subject_type": "tvm_app",
            "roles": [
                {
                    "project": "service1",
                    "environment": "sandbox",
                    "role": "subscriber"
                }
            ]
        }
    ]
})");
}

TEST_CASE_METHOD(T_idm, "add_role/http_client_not_configured")
{
    settings->http_client = "not_configured";
    role["project"] = "service1";
    role["role"] = "publisher";
    role["environment"] = "corp";

    REQUIRE_THROWS_WITH(
        add_role(settings, stream, "tvm_app", role.stringify(), "12345"),
        Catch::Contains("module \"not_configured\" not found"));
}

TEST_CASE_METHOD(T_idm, "add_role/correct_tvm_info_url")
{
    role["project"] = "service1";
    role["role"] = "publisher";
    role["environment"] = "corp";
    spawn(add_role{ settings, stream, "tvm_app", role.stringify(), "911" });

    REQUIRE(http_client_mock_ptr->requests.size() == 1);
    REQUIRE(http_client_mock_ptr->requests[0].url == "https://tvm.yandex-team.ru/client/911/info");
}

TEST_CASE_METHOD(T_idm, "add_role/inacceptable_subject_type")
{
    spawn(add_role{ settings, stream, "user", "", "912" });

    REQUIRE(*stream->code == 400);
    REQUIRE(*stream->body == "wrong subject_type");
}

TEST_CASE_METHOD(T_idm, "add_role/json_role_parse_error")
{
    spawn(add_role{ settings, stream, "tvm_app", "-/*", "" });

    REQUIRE(*stream->code == 400);
    REQUIRE(*stream->body == "json parse error");
}

TEST_CASE_METHOD(T_idm, "add_role/not_configured")
{
    settings->service_manager = "not_configured";
    role["project"] = "service1";
    role["role"] = "publisher";
    role["environment"] = "corp";

    REQUIRE_THROWS_WITH(
        add_role(settings, stream, "tvm_app", role.stringify(), "12345"),
        Catch::Contains("module \"not_configured\" not found"));
}

TEST_CASE_METHOD(T_idm, "add_role/unknown_service")
{
    role["project"] = "unknown_service";
    role["role"] = "publisher";
    role["environment"] = "corp";
    spawn(add_role{ settings, stream, "tvm_app", role.stringify(), "12345" });

    REQUIRE(*stream->code == 400);
    REQUIRE(*stream->body == "project not found");
}

TEST_CASE_METHOD(T_idm, "add_role/inacceptable_role")
{
    role["project"] = "service1";
    role["role"] = "darthvader";
    role["environment"] = "corp";
    spawn(add_role{ settings, stream, "tvm_app", role.stringify(), "12345" });

    REQUIRE(*stream->code == 400);
    REQUIRE(*stream->body == "wrong role");
}

TEST_CASE_METHOD(T_idm, "add_role/inacceptable_environment")
{
    role["project"] = "service1";
    role["role"] = "publisher";
    role["environment"] = "openspace";
    spawn(add_role{ settings, stream, "tvm_app", role.stringify(), "12345" });

    REQUIRE(*stream->code == 400);
    REQUIRE(*stream->body == "wrong environment");
}

TEST_CASE_METHOD(T_idm, "add_role/inacceptable_login")
{
    role["project"] = "service1";
    role["role"] = "publisher";
    role["environment"] = "corp";
    spawn(add_role{ settings, stream, "tvm_app", role.stringify(), "idkfa" });

    REQUIRE(*stream->code == 400);
    REQUIRE(*stream->body == "wrong login");
}

TEST_CASE_METHOD(T_idm, "add_role/publisher")
{
    REQUIRE(properties("service1").tvm_publishers.at("corp").count({ 12345, "" }) == 0);

    role["project"] = "service1";
    role["role"] = "publisher";
    role["environment"] = "corp";
    spawn(add_role{ settings, stream, "tvm_app", role.stringify(), "12345" });

    REQUIRE(*stream->code == 200);
    REQUIRE(*stream->body == R"({"code": 0})");
    REQUIRE(properties("service1").tvm_publishers.at("corp").count({ 12345, "" }) == 1);
}

TEST_CASE_METHOD(T_idm, "add_role/subscriber")
{
    REQUIRE(properties("service1").tvm_subscribers.at("sandbox").count({ 98765, "" }) == 0);

    role["project"] = "service1";
    role["role"] = "subscriber";
    role["environment"] = "sandbox";
    spawn(add_role{ settings, stream, "tvm_app", role.stringify(), "98765" });

    REQUIRE(*stream->code == 200);
    REQUIRE(*stream->body == R"({"code": 0})");
    REQUIRE(properties("service1").tvm_subscribers.at("sandbox").count({ 98765, "" }) == 1);
}

TEST_CASE_METHOD(T_idm, "add_role/app_name_from_tvm_info")
{
    role["project"] = "service1";
    role["role"] = "subscriber";
    role["environment"] = "sandbox";
    spawn(add_role{ settings, stream, "tvm_app", role.stringify(), "98765" });

    REQUIRE(
        properties("service1").tvm_subscribers.at("sandbox").find({ 98765, "" })->name ==
        "molotok");
}

TEST_CASE_METHOD(T_idm, "add_role/unknown_app_name_on_bad_tvm_info_response")
{
    role["project"] = "service1";
    role["role"] = "subscriber";
    role["environment"] = "sandbox";
    http_client_mock_ptr->resp = { 200, {}, "Mugcina vy chto ne vidite y nas obed", "OK" };
    spawn(add_role{ settings, stream, "tvm_app", role.stringify(), "98765" });

    REQUIRE(
        properties("service1").tvm_subscribers.at("sandbox").find({ 98765, "" })->name ==
        "unknown");
}

TEST_CASE_METHOD(T_idm, "remove_role/inacceptable_subject_type")
{
    spawn(remove_role{ settings, stream, "user", "", "12345" });

    REQUIRE(*stream->code == 400);
    REQUIRE(*stream->body == "wrong subject_type");
}

TEST_CASE_METHOD(T_idm, "remove_role/json_role_parse_error")
{
    spawn(remove_role{ settings, stream, "tvm_app", "-/*", "" });

    REQUIRE(*stream->code == 400);
    REQUIRE(*stream->body == "json parse error");
}

TEST_CASE_METHOD(T_idm, "remove_role/not_configured")
{
    settings->service_manager = "not_configured";
    role["project"] = "service1";
    role["role"] = "publisher";
    role["environment"] = "corp";

    REQUIRE_THROWS_WITH(
        remove_role(settings, stream, "tvm_app", role.stringify(), "12345"),
        Catch::Contains("module \"not_configured\" not found"));
}

TEST_CASE_METHOD(T_idm, "remove_role/unknown_service")
{
    role["project"] = "unknown_service";
    role["role"] = "publisher";
    role["environment"] = "corp";
    spawn(remove_role{ settings, stream, "tvm_app", role.stringify(), "12345" });

    REQUIRE(*stream->code == 400);
    REQUIRE(*stream->body == "project not found");
}

TEST_CASE_METHOD(T_idm, "remove_role/inacceptable_role")
{
    role["project"] = "service1";
    role["role"] = "barbrastreisand";
    role["environment"] = "corp";
    spawn(remove_role{ settings, stream, "tvm_app", role.stringify(), "12345" });

    REQUIRE(*stream->code == 400);
    REQUIRE(*stream->body == "wrong role");
}

TEST_CASE_METHOD(T_idm, "remove_role/inacceptable_environment")
{
    role["project"] = "service1";
    role["role"] = "publisher";
    role["environment"] = "narnia";
    spawn(remove_role{ settings, stream, "tvm_app", role.stringify(), "12345" });

    REQUIRE(*stream->code == 400);
    REQUIRE(*stream->body == "wrong environment");
}

TEST_CASE_METHOD(T_idm, "remove_role/inacceptable_login")
{
    role["project"] = "service1";
    role["role"] = "publisher";
    role["environment"] = "production";
    spawn(remove_role{ settings, stream, "tvm_app", role.stringify(), "glitteringprizes9999" });

    REQUIRE(*stream->code == 400);
    REQUIRE(*stream->body == "wrong login");
}

TEST_CASE_METHOD(T_idm, "remove_role/publisher")
{
    REQUIRE(properties("service1").tvm_publishers.at("sandbox").count({ 1, "" }) == 1);

    role["project"] = "service1";
    role["role"] = "publisher";
    role["environment"] = "sandbox";
    spawn(remove_role{ settings, stream, "tvm_app", role.stringify(), "1" });

    REQUIRE(*stream->code == 200);
    REQUIRE(*stream->body == R"({"code": 0})");
    REQUIRE(properties("service1").tvm_publishers.at("sandbox").count({ 1, "" }) == 0);
}

TEST_CASE_METHOD(T_idm, "remove_role/subscriber")
{
    REQUIRE(properties("service1").tvm_subscribers.at("production").count({ 6, "" }) == 1);

    role["project"] = "service1";
    role["role"] = "subscriber";
    role["environment"] = "production";
    spawn(remove_role{ settings, stream, "tvm_app", role.stringify(), "6" });

    REQUIRE(*stream->code == 200);
    REQUIRE(*stream->body == R"({"code": 0})");
    REQUIRE(properties("service1").tvm_subscribers.at("production").count({ 6, "" }) == 0);
}

TEST_CASE_METHOD(T_idm, "remove_role/successful_on_tvm_info_error")
{
    REQUIRE(properties("service1").tvm_publishers.at("sandbox").count({ 1, "" }) == 1);

    role["project"] = "service1";
    role["role"] = "publisher";
    role["environment"] = "sandbox";
    http_client_mock_ptr->err = errc::make_error_code(errc::timed_out);
    spawn(remove_role{ settings, stream, "tvm_app", role.stringify(), "1" });

    REQUIRE(*stream->code == 200);
    REQUIRE(*stream->body == R"({"code": 0})");
    REQUIRE(properties("service1").tvm_publishers.at("sandbox").count({ 1, "" }) == 0);
}

TEST_CASE_METHOD(T_idm, "remove_role/successful_on_tvm_info_bad_response")
{
    REQUIRE(properties("service1").tvm_publishers.at("sandbox").count({ 1, "" }) == 1);

    role["project"] = "service1";
    role["role"] = "publisher";
    role["environment"] = "sandbox";
    http_client_mock_ptr->resp = { 503, {}, R"({"name": "molotok", "status": "ok"})", "OK" };
    spawn(remove_role{ settings, stream, "tvm_app", role.stringify(), "1" });

    REQUIRE(*stream->code == 200);
    REQUIRE(*stream->body == R"({"code": 0})");
    REQUIRE(properties("service1").tvm_publishers.at("sandbox").count({ 1, "" }) == 0);
}

}

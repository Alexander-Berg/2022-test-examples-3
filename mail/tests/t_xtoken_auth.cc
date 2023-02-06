#include <catch.hpp>
#include "stream_mock.h"
#include "service_manager_mocks.h"
#include "web/auth/methods/xtoken.h"

using namespace yxiva;
using namespace yxiva::web;
using namespace yxiva::web::auth;
using namespace std::string_literals;

using stream_ptr = std::shared_ptr<stream_mock>;

template <typename TestSubjectType, typename AuthorizationType>
struct xtoken_auth_fixture
{
    using handler = std::function<void(boost::system::error_code, service_authorization)>;

    settings_ptr st = std::make_shared<settings>();
    stream_ptr stream = std::make_shared<stream_mock>();
    std::shared_ptr<service_manager_mock> service_manager = service_manager_mock::create_manager();
    item_builder b;

    boost::system::error_code auth_ec;
    AuthorizationType authorization;

    TestSubjectType test_subject{ { service_manager } };

    void authorize(const string& token = "", const std::vector<string> service_names = {})
    {
        if (token.size())
        {
            stream->request()->headers["authorization"] = "xiva " + token;
        }
        test_subject(st, stream, service_names, [this](auto ec, auto&& b) {
            auth_ec = ec;
            authorization = b;
        });
    }

    auto& url()
    {
        return stream->request()->url;
    }

    auto& custom_log_data()
    {
        return stream->request()->context->custom_log_data;
    }
};

template <xtoken_check_type token_check_type>
using t_xtoken_auth = xtoken_auth_fixture<xtoken<token_check_type>, service_authorization>;
using t_stoken_auth = t_xtoken_auth<xtoken_check_type::send>;
using t_ltoken_auth = t_xtoken_auth<xtoken_check_type::listen>;
using t_multi_xtoken_auth = xtoken_auth_fixture<multi_xtoken, multi_service_authorization>;

TEST_CASE_METHOD(t_stoken_auth, "returns no_credentials error if token cannot be found")
{
    authorize();
    REQUIRE(auth_ec == make_error(auth_error::no_credentials));
}

TEST_CASE_METHOD(t_stoken_auth, "accepts send tokens")
{
    service_manager->add_conf({ b.token("stoken1").stoken_data("svc1", "st1").build() });
    authorize("stoken1");
    REQUIRE(auth_ec == boost::system::error_code{});
    REQUIRE(authorization.service.name == "svc1");
}

TEST_CASE_METHOD(t_stoken_auth, "fails if no service can be found by the token")
{
    authorize("stoken1");
    REQUIRE(auth_ec == make_error(auth_error::bad_token));
}

TEST_CASE_METHOD(t_stoken_auth, "fails if no token properties can be found")
{
    service_manager->add_conf({ b.svc_data("svc1", "o1").build() });
    authorize("stoken1");
    REQUIRE(auth_ec == make_error(auth_error::bad_token));
}

TEST_CASE_METHOD(t_stoken_auth, "sets service and token as custom log data")
{
    service_manager->add_conf({ b.token("stoken1").stoken_data("svc1", "st1").build() });
    authorize("stoken1");
    REQUIRE(custom_log_data()["service"] == "svc1");
    REQUIRE(custom_log_data()["token"] == "st1");
}

TEST_CASE_METHOD(t_stoken_auth, "gets token from parameter if absent in header")
{
    service_manager->add_conf({ b.token("stoken1").stoken_data("svc1", "st1").build() });

    SECTION("default aliases")
    {
        stream->request()->url.params.insert({ "token", "stoken1" });
        authorize();
        REQUIRE(auth_ec == boost::system::error_code{});
        REQUIRE(authorization.service.name == "svc1");
    }
    SECTION("custom aliases")
    {
        test_subject.token_aliases = { "token", "shmocken" };
        stream->request()->url.params.insert({ "shmocken", "stoken1" });
        authorize();
        REQUIRE(auth_ec == boost::system::error_code{});
        REQUIRE(authorization.service.name == "svc1");
    }
}

TEST_CASE_METHOD(t_stoken_auth, "token from header has priority")
{
    service_manager->add_conf({ b.token("stoken1").stoken_data("svc1", "st1").build() });
    stream->request()->url.params.insert({ "token", "XXX" });
    authorize("stoken1");
    REQUIRE(auth_ec == boost::system::error_code{});
    REQUIRE(authorization.service.name == "svc1");
}

TEST_CASE_METHOD(t_ltoken_auth, "accepts listen tokens")
{
    service_manager->add_conf(
        { b.token("stoken1").stoken_data("svc1", "st1").build(),
          b.token("ltoken1").ltoken_data("svc1", "lt1", false, "test_client").build() });
    SECTION("accepts listen token")
    {
        authorize("ltoken1", { "svc1" });
        REQUIRE(auth_ec == boost::system::error_code{});
        REQUIRE(authorization.service.name == "svc1");
        REQUIRE(authorization.client == "test_client");
    }
    SECTION("does not accept send token")
    {
        authorize("stoken1", { "svc1" });
        REQUIRE(auth_ec == make_error(auth_error::bad_token));
    }
}

TEST_CASE_METHOD(t_ltoken_auth, "listen tokens and strict check mode")
{
    st->api.strict_token_check_mode = true;
    service_manager->add_conf(
        { b.token("ltoken1")
              .ltoken_data("svc1", "lt1", false, "test_client", { "listener_1", "listener_2" })
              .build() });
    SECTION("fails if listen is not allowed")
    {
        authorize("ltoken1", { "unknown_listener" });
        REQUIRE(auth_ec == make_error(auth_error::forbidden_service));
    }
    SECTION("fails if the list of services is empty")
    {
        authorize("ltoken1", {});
        REQUIRE(auth_ec == make_error(auth_error::empty_service_list));
    }
    SECTION("authorizes a known listener")
    {
        authorize("ltoken1", { "listener_1" });
        REQUIRE(auth_ec == boost::system::error_code{});
    }
    SECTION("when strict check mode is disabled, allows any listener")
    {
        st->api.strict_token_check_mode = false;
        authorize("ltoken1", { "unknown_listener" });
        REQUIRE(auth_ec == boost::system::error_code{});
    }
}

TEST_CASE_METHOD(t_stoken_auth, "fails for stream services by default")
{
    service_manager->add_conf(
        { b.svc_data("stream_service", "o", false, true).build(),
          b.token("stream_stoken").stoken_data("stream_service", "st2").build() });
    authorize("stream_stoken");
    REQUIRE(auth_ec == make_error(auth_error::bad_token));
}

TEST_CASE_METHOD(
    t_xtoken_auth<xtoken_check_type::send_stream>,
    "treats service as stream when expect_stream_service is set to true")
{
    service_manager->add_conf(
        { b.token("normal_stoken").stoken_data("normal_service", "st1").build(),
          b.svc_data("stream_service", "o", false, true).build(),
          b.token("stream_stoken").stoken_data("stream_service", "st2").build() });
    SECTION("fails to authorize normal service when expecting a stream service")
    {
        authorize("normal_stoken");
        REQUIRE(auth_ec == make_error(auth_error::bad_token));
    }
    SECTION("authorizes stoken for stream service")
    {
        authorize("stream_stoken");
        REQUIRE(auth_ec == boost::system::error_code{});
    }
}

TEST_CASE_METHOD(
    t_xtoken_auth<xtoken_check_type::listen_no_service>,
    "authorizes listen tokens without service check in listen_no_service mode")
{
    st->api.strict_token_check_mode = true;
    service_manager->add_conf(
        { b.token("ltoken1")
              .ltoken_data("svc1", "lt1", false, "test_client", { "listener_1", "listener_2" })
              .build() });
    authorize("ltoken1", {});
    REQUIRE(auth_ec == boost::system::error_code{});
}

TEST_CASE_METHOD(t_stoken_auth, "fails if more than one token passed as send token")
{
    service_manager->add_conf({ b.token("stoken1").stoken_data("svc1", "st1").build(),
                                b.token("stoken2").stoken_data("svc1", "st1").build(),
                                b.token("stoken1,stoken2").stoken_data("svc1", "st1").build() });
    authorize("stoken1,stoken2");
    REQUIRE(auth_ec == make_error(auth_error::bad_token));
}

TEST_CASE_METHOD(t_multi_xtoken_auth, "authorizes multiple listen tokens")
{
    st->api.strict_token_check_mode = true;
    service_manager->add_conf(
        { b.token("ltoken1")
              .ltoken_data("svc1", "lt1", false, "test_client_1", { "listener_1" })
              .build(),
          b.token("ltoken2")
              .ltoken_data("svc1", "lt2", false, "test_client_2", { "listener_2" })
              .build() });
    SECTION("accepts multiple listen tokens")
    {
        authorize("ltoken1,ltoken2", { "listener_1", "listener_2" });
        REQUIRE(auth_ec == boost::system::error_code{});
        REQUIRE(authorization.size() == 2);
        REQUIRE(authorization[0].service.name == "svc1");
        REQUIRE(authorization[0].client == "test_client_1");
        REQUIRE(authorization[1].service.name == "svc1");
        REQUIRE(authorization[1].client == "test_client_2");
    }
    SECTION("fails when at least one service is not allowed")
    {
        authorize("ltoken1,ltoken2", { "XXX", "listener_2" });
        REQUIRE(auth_ec == make_error(auth_error::multi_token_error));
        REQUIRE(custom_log_data()["error"] == "XXX");
    }
    SECTION("when multiple services are not allowed, dumps them in custom_log_data as csv")
    {
        authorize("ltoken1,ltoken2", { "XXX", "YYY" });
        REQUIRE(auth_ec == make_error(auth_error::multi_token_error));
        REQUIRE(custom_log_data()["error"] == "XXX, YYY");
    }
    SECTION("fails when at least one token is bad")
    {
        authorize("ltoken1,XXX", { "listener_1", "listener_2" });
        REQUIRE(auth_ec == make_error(auth_error::multi_token_error));
        REQUIRE(custom_log_data()["error"] == "listener_2");
    }
    SECTION("returns forbidden service error for single forbidden service")
    {
        authorize("ltoken1", { "XXX" });
        REQUIRE(auth_ec == make_error(auth_error::multi_token_error));
        REQUIRE(custom_log_data()["error"] == "forbidden service");
    }
}

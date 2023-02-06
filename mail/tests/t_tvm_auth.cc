#include <catch.hpp>
#include "stream_mock.h"
#include "service_manager_mocks.h"
#include "web/auth/methods/tvm.h"

using namespace yxiva;
using namespace yxiva::web;
using namespace yxiva::web::auth;
using namespace std::string_literals;

using stream_ptr = std::shared_ptr<stream_mock>;

struct tvm_ticket_mock
{
    uint32_t src = 0;
    uint64_t issuer_uid = 0;

    bool operator!() const
    {
        return src == 0;
    }

    auto GetStatus() const
    {
        // Does not matter which specific error to return for bad mock tickets, why not this one?
        return src == 0 ? make_error(auth_error::request_failed) : boost::system::error_code();
    }

    uint32_t GetSrc() const
    {
        return src;
    }

    uint64_t GetIssuerUid() const
    {
        return issuer_uid;
    }
};

struct tvm_module_mock
{
    std::unordered_map<string, tvm_ticket_mock> tickets;

    std::optional<tvm_ticket_mock> get_native_service_ticket(
        task_context_ptr,
        const std::string& ticket)
    {
        if (tickets.count(ticket))
        {
            return { tickets[ticket] };
        }
        return {};
    }
};

template <tvm_check_type check_type>
struct t_tvm_auth
{
    using authorization_type = std::conditional_t<
        check_type == tvm_check_type::multi_subscriber,
        multi_service_authorization,
        service_authorization>;

    static const uint32_t PUBLISHER_SRC = 111;
    static const uint32_t OTHER_PUBLISHER_SRC = 222;
    static const uint32_t SUBSCRIBER_SRC = 333;

    settings_ptr st = std::make_shared<settings>();
    stream_ptr stream = std::make_shared<stream_mock>();
    std::shared_ptr<tvm_module_mock> tvm_module = std::make_shared<tvm_module_mock>();
    std::shared_ptr<service_manager_mock> service_manager = service_manager_mock::create_manager();

    tvm<check_type, tvm_module_mock> test_subject{ tvm_module, service_manager };

    boost::system::error_code auth_ec;
    authorization_type authorization;

    item_builder ib;

    void configure_ticket(const string& ticket, const tvm_ticket_mock& ticket_mock)
    {
        tvm_module->tickets[ticket] = ticket_mock;
    }

    void configure_service(const service_properties& props)
    {
        service_manager->add_conf({ ib.svc_data(props).build() });
    }

    void authorize(const string& ticket = "", const std::vector<string> service_names = {})
    {
        if (ticket.size())
        {
            stream->request()->headers["x-ya-service-ticket"] = ticket;
        }
        test_subject(st, stream, service_names, [this](auto&& ec, auto&& a) {
            auth_ec = ec;
            authorization = a;
        });
    }

    void configure_environment(const string& environment)
    {
        service_manager = service_manager_mock::create_manager(nullptr, environment);
        test_subject.service_manager = service_manager;
    }

    auto& custom_log_data()
    {
        return stream->request()->context->custom_log_data;
    }
};

using t_publisher_auth = t_tvm_auth<tvm_check_type::publisher>;
using t_stream_publisher_auth = t_tvm_auth<tvm_check_type::stream_publisher>;
using t_subscriber_auth = t_tvm_auth<tvm_check_type::subscriber>;
using t_multi_subscriber_auth = t_tvm_auth<tvm_check_type::multi_subscriber>;

TEST_CASE_METHOD(t_publisher_auth, "returns no_credentials if no ticket can be found")
{
    authorize("", {});
    REQUIRE(auth_ec == make_error(auth_error::no_credentials));
}

TEST_CASE_METHOD(t_publisher_auth, "returns invalid_tvm_ticket if tvm module rejects the ticket")
{
    SECTION("unknown ticket")
    {
        authorize("unknown_ticket", { "svc1" });
    }

    SECTION("invalid ticket")
    {
        configure_ticket("invalid_ticket", {});
        authorize("invalid_ticket", { "svc1" });
    }

    REQUIRE(auth_ec == make_error(auth_error::invalid_tvm_ticket));
}

TEST_CASE_METHOD(
    t_publisher_auth,
    "returns empty_service_list when given a ticket, but no services")
{
    configure_ticket("test_ticket", { PUBLISHER_SRC });
    authorize("test_ticket", {});
    REQUIRE(auth_ec == make_error(auth_error::empty_service_list));
}

TEST_CASE_METHOD(t_publisher_auth, "returns unknown_service when service not configured")
{
    configure_ticket("test_ticket", { PUBLISHER_SRC });
    authorize("test_ticket", { "some_service" });
    REQUIRE(auth_ec == make_error(auth_error::unknown_service));
}

TEST_CASE_METHOD(
    t_publisher_auth,
    "returns forbidden_service when service has no publishers for env")
{
    configure_environment("production");
    configure_ticket("test_ticket", { PUBLISHER_SRC });
    configure_service(service_properties_builder{}
                          .name("svc1")
                          .owner_id("o1")
                          .tvm_publishers({ { "sandbox", { { PUBLISHER_SRC, "publisher" } } } })
                          .build());
    authorize("test_ticket", { "svc1" });
    REQUIRE(auth_ec == make_error(auth_error::forbidden_service));
}

TEST_CASE_METHOD(
    t_publisher_auth,
    "returns forbidden_service when service has no publishers for src")
{
    configure_ticket("test_ticket", { PUBLISHER_SRC });
    configure_service(
        service_properties_builder{}
            .name("svc1")
            .owner_id("o1")
            .tvm_publishers({ { "sandbox", { { OTHER_PUBLISHER_SRC, "publisher" } } } })
            .build());
    authorize("test_ticket", { "svc1" });
    REQUIRE(auth_ec == make_error(auth_error::forbidden_service));
}

TEST_CASE_METHOD(t_publisher_auth, "returns forbidden_service when publisher is suspended")
{
    configure_ticket("test_ticket", { PUBLISHER_SRC });
    configure_service(
        service_properties_builder{}
            .name("svc1")
            .owner_id("o1")
            .tvm_publishers({ { "sandbox", { { PUBLISHER_SRC, "publisher", true } } } })
            .build());
    authorize("test_ticket", { "svc1" });
    REQUIRE(auth_ec == make_error(auth_error::forbidden_service));
}

TEST_CASE_METHOD(t_publisher_auth, "authorizes single ticket")
{
    configure_ticket("test_ticket", { PUBLISHER_SRC });
    configure_service(service_properties_builder{}
                          .name("svc1")
                          .owner_id("o1")
                          .tvm_publishers({ { "sandbox", { { PUBLISHER_SRC, "publisher" } } } })
                          .build());
    authorize("test_ticket", { "svc1" });
    REQUIRE(auth_ec == boost::system::error_code{});
    REQUIRE(authorization.service.name == "svc1");
}

TEST_CASE_METHOD(t_publisher_auth, "logs both numeric tvm id and textual name, when available")
{
    configure_service(
        service_properties_builder{}
            .name("svc1")
            .owner_id("o1")
            .tvm_publishers(
                { { "sandbox", { { PUBLISHER_SRC, "publisher" }, { OTHER_PUBLISHER_SRC, "" } } } })
            .build());
    SECTION("logs src and app name, when available")
    {
        configure_ticket("test_ticket", { PUBLISHER_SRC });
        authorize("test_ticket", { "svc1" });
        REQUIRE(custom_log_data()["tvm_src"] == std::to_string(PUBLISHER_SRC));
        REQUIRE(custom_log_data()["tvm_src_name"] == "publisher");
    }
    SECTION("logs src, when name is not available")
    {
        configure_ticket("test_ticket_2", { OTHER_PUBLISHER_SRC });
        authorize("test_ticket_2", { "svc1" });
        REQUIRE(custom_log_data()["tvm_src"] == std::to_string(OTHER_PUBLISHER_SRC));
        REQUIRE(custom_log_data().count("tvm_src_name") == 0);
    }
}

TEST_CASE_METHOD(t_publisher_auth, "returns too_many_services when there is more than 1 service")
{
    configure_ticket("test_ticket", { PUBLISHER_SRC });
    configure_service(service_properties_builder{}
                          .name("svc1")
                          .owner_id("o1")
                          .tvm_publishers({ { "sandbox", { { PUBLISHER_SRC, "publisher" } } } })
                          .build());
    configure_service(service_properties_builder{}
                          .name("svc2")
                          .owner_id("o2")
                          .tvm_publishers({ { "sandbox", { { PUBLISHER_SRC, "publisher" } } } })
                          .build());
    authorize("test_ticket", { "svc1", "svc2" });
    REQUIRE(auth_ec == make_error(auth_error::too_many_services));
}

TEST_CASE_METHOD(t_publisher_auth, "authorizes single ticket even if tvm app name is empty")
{
    configure_ticket("test_ticket", { PUBLISHER_SRC });
    configure_service(service_properties_builder{}
                          .name("svc1")
                          .owner_id("o1")
                          .tvm_publishers({ { "sandbox", { { PUBLISHER_SRC, "" } } } })
                          .build());
    authorize("test_ticket", { "svc1" });
    REQUIRE(auth_ec == boost::system::error_code{});
    REQUIRE(authorization.service.name == "svc1");
}

TEST_CASE_METHOD(t_publisher_auth, "returns forbidden_service for stream services")
{
    configure_ticket("test_ticket", { PUBLISHER_SRC });
    configure_service(service_properties_builder{}
                          .name("svc1")
                          .owner_id("o1")
                          .tvm_publishers({ { "sandbox", { { PUBLISHER_SRC, "publisher" } } } })
                          .is_stream(true)
                          .build());
    authorize("test_ticket", { "svc1" });
    REQUIRE(auth_ec == make_error(auth_error::forbidden_service));
}

TEST_CASE_METHOD(t_publisher_auth, "returns forbidden_service in case app is only a subscriber")
{
    configure_ticket("test_ticket", { SUBSCRIBER_SRC });
    configure_service(service_properties_builder{}
                          .name("svc1")
                          .owner_id("o1")
                          .tvm_subscribers({ { "sandbox", { { SUBSCRIBER_SRC, "subscriber" } } } })
                          .build());
    authorize("test_ticket", { "svc1" });
    REQUIRE(auth_ec == make_error(auth_error::forbidden_service));
}

TEST_CASE_METHOD(t_stream_publisher_auth, "returns forbidden_service for non-stream services")
{
    configure_ticket("test_ticket", { PUBLISHER_SRC });
    configure_service(service_properties_builder{}
                          .name("svc1")
                          .owner_id("o1")
                          .tvm_publishers({ { "sandbox", { { PUBLISHER_SRC, "publisher" } } } })
                          .is_stream(false)
                          .build());
    authorize("test_ticket", { "svc1" });
    REQUIRE(auth_ec == make_error(auth_error::forbidden_service));
}

TEST_CASE_METHOD(t_subscriber_auth, "returns forbidden_service in case app is only a publisher")
{
    configure_ticket("test_ticket", { PUBLISHER_SRC });
    configure_service(service_properties_builder{}
                          .name("svc1")
                          .owner_id("o1")
                          .tvm_publishers({ { "sandbox", { { PUBLISHER_SRC, "publisher" } } } })
                          .build());
    authorize("test_ticket", { "svc1" });
    REQUIRE(auth_ec == make_error(auth_error::forbidden_service));
}

TEST_CASE_METHOD(t_multi_subscriber_auth, "authorizes multiple services")
{
    configure_ticket("test_ticket", { SUBSCRIBER_SRC });
    configure_service(service_properties_builder{}
                          .name("svc1")
                          .owner_id("o1")
                          .tvm_subscribers({ { "sandbox", { { SUBSCRIBER_SRC, "subscriber" } } } })
                          .build());
    configure_service(service_properties_builder{}
                          .name("svc2")
                          .owner_id("o2")
                          .tvm_subscribers({ { "sandbox", { { SUBSCRIBER_SRC, "subscriber2" } } } })
                          .build());
    configure_service(service_properties_builder{}.name("svc3").owner_id("o3").build());
    SECTION("yields multiple service authorizations when service list is valid")
    {
        authorize("test_ticket", { "svc1", "svc2" });
        REQUIRE(auth_ec == boost::system::error_code{});
        REQUIRE(authorization.size() == 2);
        REQUIRE(authorization[0].service.name == "svc1");
        REQUIRE(authorization[1].service.name == "svc2");
    }

    SECTION("fails when one of the services is not known")
    {
        authorize("test_ticket", { "svc1", "svc2", "unknown_service" });
        REQUIRE(auth_ec == make_error(auth_error::unknown_service));
    }

    SECTION("fails when one of the services is not allowed to subscribe")
    {
        authorize("test_ticket", { "svc1", "svc2", "svc3" });
        REQUIRE(auth_ec == make_error(auth_error::forbidden_service));
    }
}

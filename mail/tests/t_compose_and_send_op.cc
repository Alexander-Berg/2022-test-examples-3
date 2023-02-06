#include "common.h"
#include "mailbox_mocks.h"

#include <xeno/operations/user/compose_and_send_op.h>

#include <catch.hpp>

using namespace xeno;
using namespace xeno;

struct compose_and_send_test : yplatform::log::contains_logger
{
    std::string empty_operation_id = "";
    std::string operation_id1 = "123";
    std::string operation_id2 = "234";

    compose_and_send_test()
    {
    }

    auto prepare_env()
    {
        auto env = make_env(
            &io, ctx, logger(), stat, handler, test_struct_wrapper<compose_and_send_test>(this));
        env.ext_mailbox = external_mailbox;
        env.loc_mailbox = local_mailbox;
        env.cache_mailbox = mailbox_cache;
        env.sync_settings = sync_settings;
        return env;
    }

    void run_compose_and_send_op(send_request_ptr request)
    {
        auto op = xeno::user::compose_and_send_op("user_ticket", request);
        io.post([env = prepare_env(), op = std::move(op)]() mutable { op(std::move(env)); });
        io.reset();
        io.run();
    }

    send_request_ptr create_send_request(const string& op_id)
    {
        auto headers = std::make_shared<send_request::header_map_t>();
        auto params = std::make_shared<send_request::param_map_t>();
        auto post_params = std::make_shared<json::value>();
        auto request = std::make_shared<send_request>(headers, params, post_params);
        request->add_value("x-real-ip", "127.0.0.1");
        request->add_value("x-request-id", "4hnD000Wfa61");
        request->add_value("operation_id", op_id);
        return request;
    }

    void operator()(error ec, json::value /*res*/ = {})
    {
        this->ec = ec;
    }

    boost::asio::io_service io;
    std::shared_ptr<interrupt_handler> handler = std::make_shared<interrupt_handler>();
    xeno::context_ptr ctx = boost::make_shared<xeno::context>();

    ext_mb::ext_mailbox_mock_ptr external_mailbox = create_from_json(
        EXTERNAL_MAILBOX_FOR_REDOWNLOAD_MESSAGES_TESTS_PATH,
        external_mock_type::type_normal);
    loc_mb::loc_mailbox_mock_ptr local_mailbox = create_from_json(
        LOCAL_MAILBOX_FOR_REDOWNLOAD_MESSAGES_TESTS_PATH,
        local_mock_type::type_normal);
    mb::cache_mailbox_ptr mailbox_cache = std::make_shared<mb::cache_mailbox>();
    xeno::synchronization_settings_ptr sync_settings;
    xeno::iteration_stat_ptr stat = std::make_shared<xeno::iteration_stat>();

    error ec;
};

TEST_CASE_METHOD(compose_and_send_test, "simple send case")
{
    run_compose_and_send_op(create_send_request(empty_operation_id));
    REQUIRE(!ec);
    REQUIRE(external_mailbox->get_sent_count() == 1);
}

TEST_CASE_METHOD(compose_and_send_test, "sends with same operation id should merge")
{
    run_compose_and_send_op(create_send_request(operation_id1));
    run_compose_and_send_op(create_send_request(operation_id1));
    REQUIRE(!ec);
    REQUIRE(external_mailbox->get_sent_count() == 1);
}

TEST_CASE_METHOD(compose_and_send_test, "sends with different operation id shouldn't merge")
{
    run_compose_and_send_op(create_send_request(operation_id1));
    run_compose_and_send_op(create_send_request(operation_id2));
    REQUIRE(!ec);
    REQUIRE(external_mailbox->get_sent_count() == 2);
}

TEST_CASE_METHOD(compose_and_send_test, "multiple sends with empty operation id shouldn't merge")
{
    run_compose_and_send_op(create_send_request(empty_operation_id));
    run_compose_and_send_op(create_send_request(empty_operation_id));
    REQUIRE(!ec);
    REQUIRE(external_mailbox->get_sent_count() == 2);
}

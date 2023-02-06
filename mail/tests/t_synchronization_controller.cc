#include "common.h"
#include "mailbox_mocks.h"
#include "operations/user_op.h"
#include "operations/main_op.h"

#include <src/common/account.h>
#include <src/common/context.h>
#include <src/mailbox/data_types/cache_mailbox.h>
#include <src/xeno/xeno_settings.h>
#include <src/xeno/synchronization_controller.h>

#include <catch.hpp>

#include <memory>
#include <set>
#include <limits>

using io_service = boost::asio::io_service;
using code = xeno::code;
using sync_state = xeno::sync_state;
using test_controller_type = xeno::synchronization_controller_impl<
    ext_mb::ext_mailbox_mock_ptr,
    loc_mb::loc_mailbox_mock_ptr,
    mock::main_op>;
using test_controller_type_ptr = std::shared_ptr<test_controller_type>;

const auto IO_RUN_TIMEOUT = xeno::time_traits::milliseconds(10);
const auto ITERATION_TIMEOUT = IO_RUN_TIMEOUT / 10;
const auto USER_OPERATION_TIMEOUT = IO_RUN_TIMEOUT / 10;

struct synchronization_controller_test
{
    synchronization_controller_test()
        : ext_mailbox{ std::make_shared<ext_mb::ext_mailbox_mock>() }
        , loc_mailbox{ std::make_shared<loc_mb::loc_mailbox_mock>() }
        , cache_mailbox{ std::make_shared<mb::cache_mailbox>() }
        , io_internal{ std::make_shared<io_service>() }
        , io(io_internal.get())
        , ctx{ boost::make_shared<xeno::context>() }
        , xeno_settings{ std::make_shared<xeno::xeno_settings>() }
    {
        ext_mailbox = create_from_json(EXTERNAL_MAILBOX_PATH, external_mock_type::type_normal);
        loc_mailbox = create_from_json(LOCAL_MAILBOX_PATH, local_mock_type::type_normal);
        loc_mailbox->get_account([this](error /*ec*/, const xeno::account_t& account) {
            cache_mailbox->set_account(account);
        });

        xeno_settings->iteration_timeout = ITERATION_TIMEOUT;
        xeno_settings->user_operation_timeout = USER_OPERATION_TIMEOUT;

        controller = std::make_shared<test_controller_type>(
            io, xeno_settings, loc_mailbox, ext_mailbox, cache_mailbox, ctx);

        mock::main_op::finished_iterations = 0;
    }

    void run_and_reset_io()
    {
        io->run();
        io->reset();
    }

    void run_with_timeout_and_reset_io()
    {
        io->run_for(IO_RUN_TIMEOUT);
        io->reset();
    }

    ext_mb::ext_mailbox_mock_ptr ext_mailbox;
    loc_mb::loc_mailbox_mock_ptr loc_mailbox;
    mb::cache_mailbox_ptr cache_mailbox;
    xeno::synchronization_settings_ptr sync_settings;
    std::shared_ptr<io_service> io_internal;
    io_service* io;
    xeno::context_ptr ctx;
    xeno::xeno_settings_ptr xeno_settings;
    test_controller_type_ptr controller;
    std::shared_ptr<error> err{ std::make_shared<error>() };
};

TEST_CASE_METHOD(synchronization_controller_test, "should be in initial state after creation")
{
    auto current_state = controller->dump().state;

    REQUIRE(current_state == sync_state::initial);
}

TEST_CASE_METHOD(synchronization_controller_test, "should succeccfully finish iteration")
{
    io->post([this]() { controller->start(); });
    run_with_timeout_and_reset_io();

    REQUIRE(mock::main_op::finished_iterations > 0);
}

TEST_CASE_METHOD(synchronization_controller_test, "should start new iteration after finishing one")
{
    io->post([this]() { controller->start(); });
    run_with_timeout_and_reset_io();

    REQUIRE(mock::main_op::finished_iterations > 1);
}

TEST_CASE_METHOD(synchronization_controller_test, "should execute user operation")
{
    std::size_t finished_user_operations = 0;
    io->post([this]() { controller->start(); });
    io->post([this, &finished_user_operations]() {
        controller->add_user_operation(
            mock::user_op(), [this, &finished_user_operations](error ec) {
                *err = ec;
                ++finished_user_operations;
            });
    });
    run_with_timeout_and_reset_io();

    REQUIRE(!(*err));
    REQUIRE(finished_user_operations == 1);
}

TEST_CASE_METHOD(
    synchronization_controller_test,
    "should continue sync cycle after running user operation")
{
    std::size_t iterations_before_user_op = std::numeric_limits<std::size_t>::max();

    io->post([this]() { controller->start(); });
    io->post([this, &iterations_before_user_op]() {
        controller->add_user_operation(mock::user_op(), [&iterations_before_user_op](error /*ec*/) {
            iterations_before_user_op = mock::main_op::finished_iterations;
        });
    });
    run_with_timeout_and_reset_io();

    std::size_t total_iterations = mock::main_op::finished_iterations;
    REQUIRE(iterations_before_user_op > 0);
    REQUIRE(total_iterations > iterations_before_user_op);
}

TEST_CASE_METHOD(
    synchronization_controller_test,
    "should set no_auth_data state if account has no authorization data")
{
    cache_mailbox->account().auth_data.clear();

    io->post([this]() { controller->start(); });
    run_and_reset_io();

    auto current_state = controller->dump().state;
    REQUIRE(current_state == sync_state::no_auth_data);
}

TEST_CASE_METHOD(
    synchronization_controller_test,
    "should recover from no_auth_data state after updating account")
{
    cache_mailbox->account().auth_data.clear();

    io->post([this]() { controller->start(); });
    run_and_reset_io();

    io->post([this]() { controller->update_account(); });
    run_with_timeout_and_reset_io();

    auto current_state = controller->dump().state;
    REQUIRE(current_state != sync_state::no_auth_data);
}

TEST_CASE_METHOD(synchronization_controller_test, "should run user operation before sync iteration")
{
    std::size_t iterations_before_running_user_op = std::numeric_limits<std::size_t>::max();

    io->post([&]() {
        controller->add_user_operation(
            mock::user_op(), [&iterations_before_running_user_op](error /*ec*/) {
                iterations_before_running_user_op = mock::main_op::finished_iterations;
            });
    });
    run_and_reset_io();

    io->post([&]() { controller->start(); });
    run_with_timeout_and_reset_io();

    REQUIRE(iterations_before_running_user_op == 0);
}

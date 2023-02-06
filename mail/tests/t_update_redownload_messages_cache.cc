#include "common.h"
#include "mailbox_mocks.h"

#include <common/context.h>
#include <xeno/operations/environment.h>
#include <xeno/operations/sync/update_redownload_messages_cache_op.h>

#include <catch.hpp>

#include <algorithm>
#include <memory>

using io_service = boost::asio::io_service;
using io_service_ptr = std::shared_ptr<io_service>;
using code = xeno::code;

using namespace xeno::mailbox;

struct update_redownload_messages_cache_test : yplatform::log::contains_logger
{
    update_redownload_messages_cache_test()
    {
        reload_mailbox_data(local_mock_type::type_normal, external_mock_type::type_normal);
        xeno::synchronization_settings settings;
        settings.redownload_messages_delay_groups.insert({ 3, time_traits::seconds(0) });
        settings.redownload_messages_delay_groups.insert({ 7, time_traits::seconds(0) });
        settings.redownload_messages_delay_groups.insert({ 15, time_traits::seconds(0) });
        settings.redownload_messages_cache_size = 5;
        settings.max_message_size = 50;
        sync_settings = std::make_shared<const xeno::synchronization_settings>(settings);
        mailbox_cache->redownload_messages_state()->current_group = { 3, time_traits::seconds(0) };
    }

    template <typename Operation, typename... Args>
    auto run_operation(Args&&... args)
    {
        auto env = xeno::make_env<
            interrupt_handler,
            test_struct_wrapper<update_redownload_messages_cache_test>,
            ext_mb::ext_mailbox_mock_ptr,
            loc_mb::loc_mailbox_mock_ptr>(
            &io,
            ctx,
            logger(),
            stat,
            handler,
            test_struct_wrapper<update_redownload_messages_cache_test>(this));
        env.ext_mailbox = external_mailbox;
        env.loc_mailbox = local_mailbox;
        env.cache_mailbox = mailbox_cache;
        env.sync_settings = sync_settings;

        auto op = std::make_shared<Operation>(std::forward<Args>(args)...);
        io.post([env = std::move(env), op = std::move(op)]() mutable {
            yplatform::spawn(op, std::move(env));
        });

        io.reset();
        io.run();
    }

    void operator()(error ec)
    {
        this->ec = ec;
    }

    void operator()(error ec, mid_t mid)
    {
        this->ec = ec;
        this->mid = mid;
    }

    void reload_mailbox_data(local_mock_type local_mock, external_mock_type external_mock)
    {
        local_mailbox =
            create_from_json(LOCAL_MAILBOX_FOR_REDOWNLOAD_MESSAGES_TESTS_PATH, local_mock);
        local_mailbox->get_folder_vector([this](error ec, folder_vector_ptr res) {
            REQUIRE(!ec);
            mailbox_cache->update_folders_from_local(res);
        });

        external_mailbox =
            create_from_json(EXTERNAL_MAILBOX_FOR_REDOWNLOAD_MESSAGES_TESTS_PATH, external_mock);
        external_mailbox->get_folder_vector([this](error ec, folder_vector_ptr res) {
            REQUIRE(!ec);
            mailbox_cache->update_folders_from_external(res);
        });
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
    mid_t mid;
};

TEST_CASE_METHOD(
    update_redownload_messages_cache_test,
    "delete message when message not found in external mailbox")
{
    fid_t folder_fid = "1";
    auto state = mailbox_cache->redownload_messages_state();

    run_operation<xeno::update_redownload_messages_cache_op>();
    local_mailbox->get_messages_info_by_id(
        folder_fid, { 14 }, msg_info_type::with_flags, [](auto ec, message_vector_ptr messages) {
            REQUIRE(!ec);
            REQUIRE(messages->size() == 0);
        });
    REQUIRE(state->messages->size() == 4);
}

TEST_CASE_METHOD(update_redownload_messages_cache_test, "stop on delete mailish entry error")
{
    reload_mailbox_data(
        local_mock_type::type_err_delete_mailish_entry, external_mock_type::type_normal);
    fid_t folder_fid = "1";
    auto state = mailbox_cache->redownload_messages_state();

    run_operation<xeno::update_redownload_messages_cache_op>();
    REQUIRE(ec);
}

TEST_CASE_METHOD(
    update_redownload_messages_cache_test,
    "still has new failures when error occurs on updating")
{
    reload_mailbox_data(
        local_mock_type::type_err_get_not_downloaded_messages, external_mock_type::type_normal);
    auto state = mailbox_cache->redownload_messages_state();

    run_operation<xeno::update_redownload_messages_cache_op>();

    REQUIRE(ec);
    REQUIRE(state->has_new_failures);
}

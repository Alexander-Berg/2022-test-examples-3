#include "common.h"
#include "mailbox_mocks.h"

#include <common/context.h>
#include <xeno/operations/environment.h>
#include <xeno/operations/sync/redownload_messages_op.h>
#include <catch.hpp>

#include <algorithm>
#include <memory>

using io_service = boost::asio::io_service;
using io_service_ptr = std::shared_ptr<io_service>;
using code = xeno::code;

using namespace xeno::mailbox;

struct redownload_messages_test : yplatform::log::contains_logger
{
    redownload_messages_test()
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
            test_struct_wrapper<redownload_messages_test>,
            ext_mb::ext_mailbox_mock_ptr,
            loc_mb::loc_mailbox_mock_ptr>(
            &io, ctx, logger(), stat, handler, test_struct_wrapper<redownload_messages_test>(this));
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

TEST_CASE_METHOD(redownload_messages_test, "detect group after downloading")
{
    run_operation<xeno::redownload_messages_op>();
    REQUIRE(!ec);
    auto state = mailbox_cache->redownload_messages_state();
    REQUIRE(state->current_group.errors_count == 15);
}

TEST_CASE_METHOD(redownload_messages_test, "stop downloading after switch to next group")
{
    fid_t folder_fid = "1";
    run_operation<xeno::redownload_messages_op>();
    REQUIRE(!ec);
    local_mailbox->get_messages_info_by_id(
        folder_fid,
        { 15, 16, 17 },
        msg_info_type::with_flags,
        [](auto ec, message_vector_ptr messages) {
            REQUIRE(!ec);
            REQUIRE(messages->size() == 3);
            for (auto& msg : *messages)
            {
                REQUIRE(msg.mid != 0);
            }
        });
    local_mailbox->get_messages_info_by_id(
        folder_fid,
        { 18, 19 },
        msg_info_type::with_flags,
        [](auto ec, message_vector_ptr messages) {
            REQUIRE(!ec);
            REQUIRE(messages->size() == 2);
            for (auto& msg : *messages)
            {
                REQUIRE(msg.mid == 0);
            }
        });
    auto state = mailbox_cache->redownload_messages_state();
    REQUIRE(!state->has_new_failures);
    REQUIRE(state->current_group.errors_count == 15);
}

TEST_CASE_METHOD(redownload_messages_test, "errors count increments if error occures")
{
    reload_mailbox_data(local_mock_type::type_err_store, external_mock_type::type_normal);
    fid_t folder_fid = "1";
    run_operation<xeno::redownload_messages_op>();
    REQUIRE(ec);

    auto state = mailbox_cache->redownload_messages_state();
    mb::imap_id_t redownloading_message_id = 15;
    auto it = std::find_if(
        state->messages->begin(),
        state->messages->end(),
        [&redownloading_message_id](const mb::message& msg) {
            return msg.id == redownloading_message_id;
        });
    REQUIRE(it->errors_count == 6);
    REQUIRE(it->saved_errors_count == 6);
}

TEST_CASE_METHOD(
    redownload_messages_test,
    "errors count increments if error occures and saved errors count doesn't change if error "
    "hasn't been saved")
{
    reload_mailbox_data(
        local_mock_type::type_err_increment_mailish_errors,
        external_mock_type::type_err_download_body);
    fid_t folder_fid = "1";
    run_operation<xeno::redownload_messages_op>();
    REQUIRE(ec);

    auto state = mailbox_cache->redownload_messages_state();
    mb::imap_id_t redownloading_message_id = 15;
    auto it = std::find_if(
        state->messages->begin(),
        state->messages->end(),
        [&redownloading_message_id](const mb::message& msg) {
            return msg.id == redownloading_message_id;
        });
    REQUIRE(it->errors_count == 6);
    REQUIRE(it->saved_errors_count == 5);
}

TEST_CASE_METHOD(redownload_messages_test, "detect group when not download anything")
{
    reload_mailbox_data(local_mock_type::type_normal, external_mock_type::type_err_download_body);
    run_operation<xeno::redownload_messages_op>();
    REQUIRE(ec);
    auto state = mailbox_cache->redownload_messages_state();
    REQUIRE(state->current_group.errors_count == 7);
}

TEST_CASE_METHOD(redownload_messages_test, "not update delay if loaded message from current group")
{
    run_operation<xeno::update_redownload_messages_cache_op>();
    auto state = mailbox_cache->redownload_messages_state();
    state->current_delay = time_traits::hours(1);
    state->current_group = { 7, time_traits::seconds(10) };

    run_operation<xeno::redownload_messages_op>();
    REQUIRE(state->current_delay == time_traits::hours(1));
    REQUIRE(!ec);
}

TEST_CASE_METHOD(
    redownload_messages_test,
    "update delay if loaded message is not from current group")
{
    run_operation<xeno::update_redownload_messages_cache_op>();
    auto state = mailbox_cache->redownload_messages_state();
    state->current_delay = time_traits::milliseconds(10);

    std::this_thread::sleep_for(time_traits::milliseconds(10));
    run_operation<xeno::redownload_messages_op>();
    REQUIRE(state->current_delay != time_traits::milliseconds(10));
    REQUIRE(!ec);
}

TEST_CASE_METHOD(redownload_messages_test, "update delay if redownload failed")
{
    reload_mailbox_data(local_mock_type::type_normal, external_mock_type::type_err_download_body);
    run_operation<xeno::update_redownload_messages_cache_op>();
    auto state = mailbox_cache->redownload_messages_state();
    state->current_delay = time_traits::milliseconds(10);
    state->current_group = { 7, time_traits::seconds(0) };

    std::this_thread::sleep_for(time_traits::milliseconds(10));
    run_operation<xeno::redownload_messages_op>();
    REQUIRE(ec);
    REQUIRE(state->current_delay != time_traits::milliseconds(10));
}

TEST_CASE_METHOD(redownload_messages_test, "wait delay")
{
    run_operation<xeno::update_redownload_messages_cache_op>();
    auto state = mailbox_cache->redownload_messages_state();
    state->current_delay = time_traits::milliseconds(10);
    state->current_group = { 7, time_traits::seconds(10) };
    fid_t folder_fid = "1";

    run_operation<xeno::redownload_messages_op>();
    REQUIRE(state->current_delay == time_traits::milliseconds(10));
    REQUIRE(!ec);
    local_mailbox->get_messages_info_by_id(
        folder_fid,
        { 15, 16, 17, 18, 19 },
        msg_info_type::with_flags,
        [](auto ec, message_vector_ptr messages) {
            REQUIRE(!ec);
            REQUIRE(messages->size() == 5);
            for (auto& msg : *messages)
            {
                REQUIRE(msg.mid == 0);
            }
        });

    std::this_thread::sleep_for(time_traits::milliseconds(10));

    run_operation<xeno::redownload_messages_op>();
    REQUIRE(state->current_delay != time_traits::milliseconds(10));
    REQUIRE(!ec);
    local_mailbox->get_messages_info_by_id(
        folder_fid,
        { 15, 16, 17 },
        msg_info_type::with_flags,
        [](auto ec, message_vector_ptr messages) {
            REQUIRE(!ec);
            REQUIRE(messages->size() == 3);
            for (auto& msg : *messages)
            {
                REQUIRE(msg.mid != 0);
            }
        });
}

TEST_CASE_METHOD(redownload_messages_test, "update last downloading time when redownload successed")
{
    auto state = mailbox_cache->redownload_messages_state();
    auto time_of_start = state->last_message_download_time;
    fid_t folder_fid = "1";

    run_operation<xeno::redownload_messages_op>();
    REQUIRE(!ec);
    local_mailbox->get_messages_info_by_id(
        folder_fid,
        { 15, 16, 17 },
        msg_info_type::with_flags,
        [](auto ec, message_vector_ptr messages) {
            REQUIRE(!ec);
            REQUIRE(messages->size() == 3);
            for (auto& msg : *messages)
            {
                REQUIRE(msg.mid != 0);
            }
        });
    REQUIRE(state->last_message_download_time != time_of_start);
}

TEST_CASE_METHOD(redownload_messages_test, "update last download time when redownload failed")
{
    reload_mailbox_data(local_mock_type::type_normal, external_mock_type::type_err_download_body);
    auto state = mailbox_cache->redownload_messages_state();
    auto time_of_start = state->last_message_download_time;
    fid_t folder_fid = "1";

    run_operation<xeno::redownload_messages_op>();
    REQUIRE(ec);
    local_mailbox->get_messages_info_by_id(
        folder_fid, { 15 }, msg_info_type::with_flags, [](auto ec, message_vector_ptr messages) {
            REQUIRE(!ec);
            REQUIRE(messages->size() == 1);
            REQUIRE(messages->begin()->mid == 0);
            REQUIRE(messages->begin()->errors_count == 6);
        });
    REQUIRE(state->last_message_download_time != time_of_start);
}

TEST_CASE_METHOD(redownload_messages_test, "update flags in state")
{
    auto state = mailbox_cache->redownload_messages_state();

    run_operation<xeno::redownload_messages_op>();
    REQUIRE(!ec);
    REQUIRE(!state->has_new_failures);

    for (int i = 0; i < 2; i++)
    {
        run_operation<xeno::redownload_messages_op>();
        REQUIRE(!ec);
        REQUIRE(!state->has_new_failures);
        REQUIRE(state->has_another_messages_in_local_mb);
        REQUIRE(state->messages->empty());
    }

    run_operation<xeno::redownload_messages_op>();
    REQUIRE(!ec);
    REQUIRE(!state->has_new_failures);
    REQUIRE(!state->has_another_messages_in_local_mb);
    REQUIRE(state->messages->empty());
}

TEST_CASE_METHOD(redownload_messages_test, "stop downloading after error occurs")
{
    reload_mailbox_data(local_mock_type::type_normal, external_mock_type::type_err_download_body);
    fid_t folder_fid = "1";

    run_operation<xeno::redownload_messages_op>();
    REQUIRE(ec);
    local_mailbox->get_messages_info_by_id(
        folder_fid,
        { 15, 16, 17, 18, 19 },
        msg_info_type::with_flags,
        [](auto ec, message_vector_ptr messages) {
            REQUIRE(!ec);
            REQUIRE(messages->size() == 5);
            for (auto& msg : *messages)
            {
                REQUIRE(msg.mid == 0);
            }
        });
    auto state = mailbox_cache->redownload_messages_state();
    REQUIRE(state->messages->back().errors_count == 6);
}

TEST_CASE_METHOD(redownload_messages_test, "update cache when errors count reaches maximum")
{
    reload_mailbox_data(local_mock_type::type_normal, external_mock_type::type_err_download_body);
    auto state = mailbox_cache->redownload_messages_state();
    mb::imap_id_t message_not_in_cache_id = 19;
    for (int i = 0; i < 10; i++)
    {
        run_operation<xeno::redownload_messages_op>();
        REQUIRE(!state->has_new_failures);
        auto it = std::find_if(
            state->messages->begin(),
            state->messages->end(),
            [&message_not_in_cache_id](const mb::message& msg) {
                return msg.id == message_not_in_cache_id;
            });
        REQUIRE(it == state->messages->end());
    }
    REQUIRE(state->messages->size() == 4);
    REQUIRE(state->messages->back().errors_count == 9);

    run_operation<xeno::redownload_messages_op>();
    REQUIRE(!state->has_new_failures);
    REQUIRE(state->messages->size() == 5);
    auto it = std::find_if(
        state->messages->begin(),
        state->messages->end(),
        [&message_not_in_cache_id](const mb::message& msg) {
            return msg.id == message_not_in_cache_id;
        });
    REQUIRE(it != state->messages->end());
}

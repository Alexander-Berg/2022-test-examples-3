#include "common.h"
#include "mailbox_mocks.h"

#include <src/common/context.h>
#include <src/xeno/operations/environment.h>
#include <src/mailbox/data_types/cache_mailbox.h>
#include <src/xeno/operations/sync/load_folder_top_op.h>
#include <catch.hpp>

#include <algorithm>
#include <memory>

using io_service = boost::asio::io_service;
using io_service_ptr = std::shared_ptr<io_service>;
using code = xeno::code;
using namespace xeno::mailbox;

static const uint32_t TOP_MESSAGES_COUNT = 2;

struct load_folder_top_test : yplatform::log::contains_logger
{
    load_folder_top_test()
    {
        reload_mailbox_data(local_mock_type::type_normal);
        xeno::synchronization_settings settings;
        settings.newest_count = TOP_MESSAGES_COUNT;
        sync_settings = std::make_shared<const xeno::synchronization_settings>(settings);
    }

    template <typename Operation, typename... Args>
    auto run_operation(Args&&... args)
    {
        auto env = xeno::make_env<
            interrupt_handler,
            test_struct_wrapper<load_folder_top_test>,
            ext_mb::ext_mailbox_mock_ptr,
            loc_mb::loc_mailbox_mock_ptr>(
            &io, ctx, logger(), stat, handler, test_struct_wrapper<load_folder_top_test>(this));
        env.ext_mailbox = std::make_shared<ext_mb::ext_mailbox_mock>();
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

    void reload_mailbox_data(local_mock_type local_mock)
    {
        local_mailbox = create_from_json(LOCAL_MAILBOX_FOR_LOAD_FOLDER_TOP_TESTS_PATH, local_mock);
        local_mailbox->get_folder_vector([this](error ec, folder_vector_ptr res) {
            REQUIRE(!ec);
            mailbox_cache->update_folders_from_local(res);
        });
    }

    boost::asio::io_service io;
    std::shared_ptr<interrupt_handler> handler = std::make_shared<interrupt_handler>();
    xeno::context_ptr ctx = boost::make_shared<xeno::context>();

    local::loc_mailbox_mock_ptr local_mailbox = create_from_json(
        LOCAL_MAILBOX_FOR_LOAD_FOLDER_TOP_TESTS_PATH,
        local_mock_type::type_normal);
    cache_mailbox_ptr mailbox_cache = std::make_shared<cache_mailbox>();
    xeno::synchronization_settings_ptr sync_settings;
    xeno::iteration_stat_ptr stat = std::make_shared<xeno::iteration_stat>();
    error ec;
};

TEST_CASE_METHOD(load_folder_top_test, "ensure correct messages status loading")
{
    path_t path{ "user_folder_with_exactly_top_count_messages", '|' };
    auto folder = mailbox_cache->get_folder_by_path(path);
    REQUIRE(static_cast<bool>(folder));

    run_operation<xeno::load_folder_top_op>(*folder);
    REQUIRE(!ec);
    auto folders = mailbox_cache->sync_newest_state()->folders;
    auto folder_state = folders.find(path);
    REQUIRE(folder_state != folders.end());
    size_t msgs_with_mid_count = 0;
    size_t msgs_without_mid_count = 0;
    for (auto& [id, message] : folder_state->second.messages_top)
    {
        if (message.mid)
        {
            REQUIRE(message.status == message::status_t::ok);
            ++msgs_with_mid_count;
        }
        else
        {
            REQUIRE(message.status == message::status_t::to_download_body);
            ++msgs_without_mid_count;
        }
    }
    REQUIRE(msgs_with_mid_count == 1);
    REQUIRE(msgs_without_mid_count == 1);
}

TEST_CASE_METHOD(
    load_folder_top_test,
    "unlock api_read flag for folders with no less than top messages count")
{
    auto state = mailbox_cache->sync_newest_state();
    uint32_t cnt_api_read_lock = 0;

    auto& folders = mailbox_cache->folders();
    for (auto& [path, folder] : folders)
    {
        auto folder_state = state->folders.find(path);
        REQUIRE(folder_state == state->folders.end());

        run_operation<xeno::load_folder_top_op>(folder);
        REQUIRE(!ec);

        folder_state = state->folders.find(path);
        REQUIRE(folder_state != state->folders.end());

        if (folder_state->second.messages_top.size() < TOP_MESSAGES_COUNT)
        {
            REQUIRE(folder.api_read_lock);
            ++cnt_api_read_lock;
        }
        else
        {
            REQUIRE(!folder.api_read_lock);
        }
    }

    REQUIRE(cnt_api_read_lock > 0);
    REQUIRE(cnt_api_read_lock < folders.size());
}

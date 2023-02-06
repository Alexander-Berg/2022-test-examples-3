#include "common.h"
#include "mailbox_mocks.h"

#include <common/context.h>
#include <src/xeno/load_cache_op.h>
#include <xeno/operations/sync/update_oldest_messages_cache_op.h>

#include <catch.hpp>
#include <boost/asio/io_service.hpp>

using namespace xeno::mailbox;

static const num_t FOLDER_TOP_SIZE = 1;
static const num_t CACHE_SIZE = 3;

struct update_oldest_messages_cache_test : yplatform::log::contains_logger
{
    update_oldest_messages_cache_test()
    {
        reload_mailbox_data(local_mock_type::type_normal, external_mock_type::type_normal);

        xeno::synchronization_settings settings;
        settings.oldest_cache_size = CACHE_SIZE;
        sync_settings = std::make_shared<const xeno::synchronization_settings>(settings);
    }

    template <typename Operation, typename... Args>
    auto run_operation(Args&&... args)
    {
        auto env = make_env(
            &io,
            ctx,
            logger(),
            stat,
            handler,
            test_struct_wrapper<update_oldest_messages_cache_test>(this));
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

    void operator()(error ec, mb::cache_mailbox_ptr mailbox_cache)
    {
        this->ec = ec;
        this->mailbox_cache = mailbox_cache;
    }

    void reload_mailbox_data(local_mock_type local_mock, external_mock_type external_mock)
    {
        local_mailbox = create_from_json(LOCAL_MAILBOX_PATH, local_mock);
        xeno::load_cache_op load_op(ctx, local_mailbox, [this](error ec, cache_mailbox_ptr cache) {
            REQUIRE(!ec);
            mailbox_cache = cache;
            auto& folders = mailbox_cache->folders();
            for (auto& [folder_path, folder] : folders)
            {
                local_mailbox->get_messages_info_top(
                    folder.fid,
                    FOLDER_TOP_SIZE,
                    [this, path = folder_path](error ec, message_vector_ptr messages) {
                        REQUIRE(!ec);
                        set_folder_top(mailbox_cache, path, messages);
                    });
            }
        });

        io.post(load_op);
        io.reset();
        io.run();

        external_mailbox = create_from_json(EXTERNAL_MAILBOX_PATH, external_mock);
        external_mailbox->get_folder_vector([this](error ec, folder_vector_ptr res) {
            REQUIRE(!ec);
            mailbox_cache->update_folders_from_external(res);
        });

        auto folders = mailbox_cache->folders_copy();
        for (auto folder : *folders)
        {
            if (folder.status != folder::status_t::ok) continue;

            external_mailbox->get_folder_info(folder.path, [this](error ec, folder_ptr res) {
                REQUIRE(!ec);
                mailbox_cache->update_folder_info_from_external(*res);
            });
        }
    }

    boost::asio::io_service io;
    std::shared_ptr<interrupt_handler> handler = std::make_shared<interrupt_handler>();
    xeno::context_ptr ctx = boost::make_shared<xeno::context>();

    ext_mb::ext_mailbox_mock_ptr external_mailbox;
    loc_mb::loc_mailbox_mock_ptr local_mailbox;
    mb::cache_mailbox_ptr mailbox_cache = std::make_shared<mb::cache_mailbox>();
    xeno::synchronization_settings_ptr sync_settings;
    xeno::iteration_stat_ptr stat = std::make_shared<xeno::iteration_stat>();

    error ec;
};

TEST_CASE_METHOD(update_oldest_messages_cache_test, "basic cache updation")
{
    path_t folder_path = { "INBOX", '|' };
    auto folder = mailbox_cache->get_folder(folder_path);

    run_operation<xeno::update_oldest_messages_cache_op>(folder);
    REQUIRE(!ec);

    auto cache = mailbox_cache->get_oldest_messages_for_sync(folder_path);
    REQUIRE(cache);
    REQUIRE(cache->size() == 3);

    imap_id_vector cached_ids;
    std::transform(
        cache->begin(), cache->end(), std::back_inserter(cached_ids), [](const message& msg) {
            return msg.id;
        });

    REQUIRE(cached_ids == (imap_id_vector{ 111, 112, 115 }));
}

TEST_CASE_METHOD(
    update_oldest_messages_cache_test,
    "cache contains only values after downloaded range")
{
    path_t folder_path = { "INBOX", '|' };
    auto& folder = mailbox_cache->get_folder(folder_path);
    folder.downloaded_range = imap_range{ 115, 112 };

    run_operation<xeno::update_oldest_messages_cache_op>(folder);
    REQUIRE(!ec);

    auto cache = mailbox_cache->get_oldest_messages_for_sync(folder_path);
    REQUIRE(cache);
    REQUIRE(cache->size() == 3);

    imap_id_vector cached_ids;
    std::transform(
        cache->begin(), cache->end(), std::back_inserter(cached_ids), [](const message& msg) {
            return msg.id;
        });

    REQUIRE(cached_ids == (imap_id_vector{ 110, 111, 112 }));
}

TEST_CASE_METHOD(update_oldest_messages_cache_test, "empty cache when all downloaded")
{
    path_t folder_path = { "INBOX", '|' };
    auto& folder = mailbox_cache->get_folder(folder_path);
    folder.downloaded_range = imap_range{ 115, 109 };

    run_operation<xeno::update_oldest_messages_cache_op>(folder);
    REQUIRE(!ec);

    auto cache = mailbox_cache->get_oldest_messages_for_sync(folder_path);
    REQUIRE(cache);
    REQUIRE(cache->size() == 0);
}

TEST_CASE_METHOD(update_oldest_messages_cache_test, "empty folders are ok")
{
    path_t folder_path = { "test_main_folder", '|' };
    auto folder = mailbox_cache->get_folder(folder_path);

    run_operation<xeno::update_oldest_messages_cache_op>(folder);
    REQUIRE(!ec);

    auto cache = mailbox_cache->get_oldest_messages_for_sync(folder_path);
    REQUIRE(cache);
    REQUIRE(cache->size() == 0);
}

TEST_CASE_METHOD(update_oldest_messages_cache_test, "cache for unprocessed folder")
{
    path_t folder_path = { "INBOX", '|' };
    auto& folder = mailbox_cache->get_folder(folder_path);
    local_mailbox->clear([](error ec) { REQUIRE(!ec); });

    folder.top_id = 0;
    folder.downloaded_range = { 0, 0 };

    run_operation<xeno::update_oldest_messages_cache_op>(folder);
    REQUIRE(!ec);

    auto cache = mailbox_cache->get_oldest_messages_for_sync(folder_path);
    REQUIRE(cache);
    REQUIRE(cache->size() == 3);

    imap_id_vector cached_ids;
    std::transform(
        cache->begin(), cache->end(), std::back_inserter(cached_ids), [](const message& msg) {
            return msg.id;
        });
    REQUIRE(cached_ids == (imap_id_vector{ 111, 112, 115 }));
}

TEST_CASE_METHOD(update_oldest_messages_cache_test, "cache combined from multiple message chunks")
{
    path_t folder_path = { "INBOX", '|' };
    auto& folder = mailbox_cache->get_folder(folder_path);
    folder.downloaded_range = imap_range{ 115, 111 };

    run_operation<xeno::update_oldest_messages_cache_op>(folder);
    REQUIRE(!ec);

    auto cache = mailbox_cache->get_oldest_messages_for_sync(folder_path);
    REQUIRE(cache);
    REQUIRE(cache->size() == 2);

    imap_id_vector cached_ids;
    std::transform(
        cache->begin(), cache->end(), std::back_inserter(cached_ids), [](const message& msg) {
            return msg.id;
        });

    REQUIRE(cached_ids == (imap_id_vector{ 110, 111 }));
}

TEST_CASE_METHOD(update_oldest_messages_cache_test, "second call returns same data")
{
    path_t folder_path = { "INBOX", '|' };
    auto& folder = mailbox_cache->get_folder(folder_path);
    folder.downloaded_range = imap_range{ 115, 111 };

    run_operation<xeno::update_oldest_messages_cache_op>(folder);
    REQUIRE(!ec);

    auto cache = mailbox_cache->get_oldest_messages_for_sync(folder_path);
    REQUIRE(cache);
    REQUIRE(cache->size() == 2);

    imap_id_vector cached_ids;
    std::transform(
        cache->begin(), cache->end(), std::back_inserter(cached_ids), [](const message& msg) {
            return msg.id;
        });

    REQUIRE(cached_ids == (imap_id_vector{ 110, 111 }));

    mailbox_cache->update_oldest_messages_for_sync(folder_path, std::make_shared<message_vector>());
    run_operation<xeno::update_oldest_messages_cache_op>(folder);
    REQUIRE(!ec);

    cache = mailbox_cache->get_oldest_messages_for_sync(folder_path);
    REQUIRE(cache);

    imap_id_vector second_cached_ids;
    std::transform(
        cache->begin(),
        cache->end(),
        std::back_inserter(second_cached_ids),
        [](const message& msg) { return msg.id; });

    REQUIRE(cached_ids == second_cached_ids);
}

TEST_CASE_METHOD(update_oldest_messages_cache_test, "second call returns next chunk")
{
    path_t folder_path = { "INBOX", '|' };
    auto& folder = mailbox_cache->get_folder(folder_path);

    run_operation<xeno::update_oldest_messages_cache_op>(folder);
    REQUIRE(!ec);

    auto cache = mailbox_cache->get_oldest_messages_for_sync(folder_path);
    REQUIRE(cache);
    REQUIRE(cache->size() == 3);

    imap_id_vector cached_ids;
    std::transform(
        cache->begin(), cache->end(), std::back_inserter(cached_ids), [](const message& msg) {
            return msg.id;
        });

    REQUIRE(cached_ids == (imap_id_vector{ 111, 112, 115 }));

    folder.downloaded_range = imap_range{ 115, 111 };
    mailbox_cache->update_oldest_messages_for_sync(folder_path, std::make_shared<message_vector>());
    run_operation<xeno::update_oldest_messages_cache_op>(folder);
    REQUIRE(!ec);

    cache = mailbox_cache->get_oldest_messages_for_sync(folder_path);
    REQUIRE(cache);
    REQUIRE(cache->size() == 2);

    cached_ids.clear();
    std::transform(
        cache->begin(), cache->end(), std::back_inserter(cached_ids), [](const message& msg) {
            return msg.id;
        });

    REQUIRE(cached_ids == (imap_id_vector{ 110, 111 }));
}

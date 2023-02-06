#include "common.h"
#include "mailbox_mocks.h"

#include <src/common/context.h>
#include <src/xeno/operations/environment.h>
#include <src/mailbox/data_types/cache_mailbox.h>
#include <src/xeno/operations/sync/sync_oldest_op.h>
#include <src/xeno/operations/sync/sync_folders_op.h>

#include <catch.hpp>

#include <algorithm>
#include <chrono>
#include <memory>
#include <src/mdb/errors.h>

using io_service = boost::asio::io_service;
using io_service_ptr = std::shared_ptr<io_service>;
using code = xeno::code;

static const mb::num_t CHUNK = 40;
static const mb::path_t PATH = { "INBOX", '|' };

struct sync_oldest_test
{
    sync_oldest_test()
        : ext_mailbox{ create_from_json(EXTERNAL_MAILBOX_PATH, external_mock_type::type_normal) }
        , loc_mailbox{ create_from_json(LOCAL_MAILBOX_PATH, local_mock_type::type_normal) }
        , cache_mailbox{ std::make_shared<mb::cache_mailbox>() }
        , sync_settings{ std::make_shared<const xeno::synchronization_settings>() }
        , io_internal{ std::make_shared<io_service>() }
        , io(io_internal.get())
        , ctx{ boost::make_shared<xeno::context>() }
        , logger_internal{ std::make_shared<xeno::logger_t>() }
    {
        xeno::synchronization_settings sync_settings;
        sync_settings.oldest_count = CHUNK;
        sync_settings.oldest_cache_size = CHUNK;
        this->sync_settings =
            std::make_shared<const xeno::synchronization_settings>(std::move(sync_settings));
    }

    void sync_folders()
    {
        auto env = make_env(io, ctx, *logger_internal.get(), stat, handler, *this);
        env.ext_mailbox = ext_mailbox;
        env.loc_mailbox = loc_mailbox;
        env.cache_mailbox = cache_mailbox;
        env.sync_settings = sync_settings;

        xeno::spawn<xeno::sync_folders_op>(std::move(env));
    }

    void sync_oldest()
    {
        auto env = make_env(io, ctx, *logger_internal.get(), stat, handler, *this);
        env.ext_mailbox = ext_mailbox;
        env.loc_mailbox = loc_mailbox;
        env.cache_mailbox = cache_mailbox;
        env.sync_settings = sync_settings;

        xeno::spawn<xeno::sync_oldest_op>(std::move(env));
    }

    void operator()(error ec = {})
    {
        *err = ec;
    }

    template <typename Handler>
    auto io_wrap(Handler&& h) const
    {
        return io->wrap(std::forward<Handler>(h));
    }

    ext_mb::ext_mailbox_mock_ptr ext_mailbox;
    loc_mb::loc_mailbox_mock_ptr loc_mailbox;
    mb::cache_mailbox_ptr cache_mailbox;
    xeno::synchronization_settings_ptr sync_settings;
    io_service_ptr io_internal;
    boost::asio::io_service* io;
    std::shared_ptr<error> err{ std::make_shared<error>() };
    xeno::context_ptr ctx;
    xeno::logger_ptr logger_internal;
    xeno::iteration_stat_ptr stat = std::make_shared<xeno::iteration_stat>();

    std::shared_ptr<interrupt_handler> handler = std::make_shared<interrupt_handler>();
};

TEST_CASE_METHOD(sync_oldest_test, "sync oldest")
{
    io->post([this]() {
        loc_mailbox->clear([this](error ec) {
            REQUIRE(!ec);
            cache_mailbox->clear_folders();
        });
        loc_mailbox->get_folder_vector([this](error ec, mb::folder_vector_ptr folder_vector) {
            REQUIRE(!ec);
            cache_mailbox->update_folders_from_local(folder_vector);
        });

        ext_mailbox->create_folder(mb::path_t{ "DRAFTS", '|' }, [](error ec) { REQUIRE(!ec); });

        ext_mailbox->create_folder(mb::path_t{ "spam", '|' }, [](error ec) { REQUIRE(!ec); });

        ext_mailbox->get_folder_vector([this](error ec, mb::folder_vector_ptr folder_vector) {
            REQUIRE(!ec);
            cache_mailbox->update_folders_from_external(folder_vector);
            for (auto& folder : *folder_vector)
            {
                cache_mailbox->update_folder_info_from_external(folder);
            }
        });
        ext_mailbox->get_messages_info_by_id(
            PATH, mb::imap_range(110, 110), [this](error ec, mb::message_vector_ptr messages) {
                REQUIRE(!ec);
                REQUIRE(messages->size() == 1);
                auto& msg = messages->front();
                auto& folders = cache_mailbox->sync_newest_state()->folders;
                folders[PATH].messages_top[msg.id] = msg;
                cache_mailbox->update_message_if_exists(PATH, 110, 110);
            });
    });
    io->run();
    io->reset();

    {
        auto folders = cache_mailbox->folders();
        auto folder_it = folders.find(PATH);
        REQUIRE(folder_it != folders.end());
        auto& folder = folder_it->second;
        REQUIRE(folder.top_id == 110);
        REQUIRE(folder.downloaded_range.top() == 110);
        REQUIRE(folder.downloaded_range.bottom() == 110);
    }

    io->post([this]() { sync_oldest(); });
    io->run();
    io->reset();

    REQUIRE(!(*err));
    {
        auto folders = cache_mailbox->folders();
        auto folder_it = folders.find(PATH);
        REQUIRE(folder_it != folders.end());
        auto& folder = folder_it->second;
        REQUIRE(folder.downloaded_range.top() == 110);
        REQUIRE(folder.downloaded_range.bottom() == 13);
    }

    io->post([this]() {
        auto fid = cache_mailbox->get_fid_by_path(PATH);
        loc_mailbox->get_messages_info_top(
            *fid, 20, [](error ec, mb::message_vector_ptr messages) {
                REQUIRE(!ec);
                REQUIRE(messages->size() == 3);
            });
    });

    for (auto i = 0; i < 3; ++i)
    {
        io->post([this]() { sync_oldest(); });
        io->run();
        io->reset();

        REQUIRE(!(*err));
    }

    {
        auto folders = cache_mailbox->folders();
        auto folder_it = folders.find(PATH);
        REQUIRE(folder_it != folders.end());
        auto& folder = folder_it->second;
        REQUIRE(folder.downloaded_range.top() == 110);
        REQUIRE(folder.downloaded_range.bottom() == 1);
    }

    io->post([this]() {
        auto fid = cache_mailbox->get_fid_by_path(PATH);
        loc_mailbox->get_messages_info_top(
            *fid, 20, [](error ec, mb::message_vector_ptr messages) {
                REQUIRE(!ec);
                REQUIRE(messages->size() == 3);
            });
    });
}

TEST_CASE_METHOD(sync_oldest_test, "sync oldest errors")
{
    auto reload_cache = [this]() {
        cache_mailbox = std::make_shared<mb::cache_mailbox>();
        ext_mailbox =
            create_from_json(EXTERNAL_MAILBOX_PATH, external_mock_type::type_err_download_body);
        loc_mailbox->clear([this](error ec) {
            REQUIRE(!ec);
            cache_mailbox->clear_folders();
        });
        loc_mailbox->get_folder_vector([this](error ec, mb::folder_vector_ptr folder_vector) {
            REQUIRE(!ec);
            cache_mailbox->update_folders_from_local(folder_vector);
        });

        ext_mailbox->create_folder(mb::path_t{ "DRAFTS", '|' }, [](error ec) { REQUIRE(!ec); });

        ext_mailbox->create_folder(mb::path_t{ "spam", '|' }, [](error ec) { REQUIRE(!ec); });

        ext_mailbox->get_folder_vector([this](error ec, mb::folder_vector_ptr folder_vector) {
            REQUIRE(!ec);
            cache_mailbox->update_folders_from_external(folder_vector);
            for (auto& folder : *folder_vector)
            {
                cache_mailbox->update_folder_info_from_external(folder);
            }
        });

        ext_mailbox->get_messages_info_by_id(
            PATH, mb::imap_range(115, 115), [this](error ec, mb::message_vector_ptr messages) {
                REQUIRE(!ec);
                REQUIRE(messages->size() == 1);
                auto& msg = messages->front();
                auto& folders = cache_mailbox->sync_newest_state()->folders;
                folders[PATH].messages_top[msg.id] = msg;
            });

        auto folders = cache_mailbox->folders();
        auto folder_it = folders.find(PATH);
        REQUIRE(folder_it != folders.end());

        auto& folder = folder_it->second;
        REQUIRE(folder.top_id == 0);
        REQUIRE(folder.downloaded_range.top() == 0);
        REQUIRE(folder.downloaded_range.bottom() == 0);
    };

    io->post(reload_cache);
    io->run();
    io->reset();

    for (auto i = 0; i < 2; ++i)
    {
        io->post([this]() { sync_oldest(); });
        io->run();
        io->reset();
        REQUIRE(!(*err));
    }

    {
        auto folders = cache_mailbox->folders();
        auto folder_it = folders.find(PATH);
        REQUIRE(folder_it != folders.end());

        auto& folder = folder_it->second;
        REQUIRE(folder.downloaded_range.top() == 115);
        REQUIRE(folder.downloaded_range.bottom() == 1);

        auto& messages = loc_mailbox->get_folder_messages(folder.fid);
        REQUIRE(messages.size() == 6);

        for (auto& msg : messages)
        {
            REQUIRE(msg.errors_count == 1);
            REQUIRE(msg.mid == 0);
        }
    }

    loc_mailbox =
        create_from_json(LOCAL_MAILBOX_PATH, local_mock_type::type_err_increment_mailish_errors);

    io->post([this, reload_cache]() {
        reload_cache();
        sync_oldest();
    });
    io->run();
    io->reset();
    REQUIRE(*err == xeno::mdb::tmp_error);

    {
        auto folders = cache_mailbox->folders();
        auto folder_it = folders.find(PATH);
        REQUIRE(folder_it != folders.end());

        auto& folder = folder_it->second;
        REQUIRE(folder.downloaded_range.top() == 0);
        REQUIRE(folder.downloaded_range.bottom() == 0);

        auto& messages = loc_mailbox->get_folder_messages(folder.fid);
        REQUIRE(messages.size() == 0);
    }
}

TEST_CASE_METHOD(sync_oldest_test, "ensure correct downloading on non-important folders")
{
    mb::fid_t folder_fid = "1";
    mb::path_t folder_path = { "DRAFTS", '|' };

    mb::imap_id_t fake_message_id = 1u;

    ext_mailbox = create_from_json(LOCAL_MAILBOX_PATH, external_mock_type::type_normal);
    loc_mailbox->clear_folder(folder_fid, [](error ec) { REQUIRE(!ec); });

    loc_mailbox->add_message(
        folder_fid, mb::message{ fake_message_id, std::time(0), mb::flags_t() });

    loc_mailbox->get_folder_vector([this](error ec, mb::folder_vector_ptr res) {
        REQUIRE(!ec);
        cache_mailbox->set_initial_folders(res);
        cache_mailbox->update_folders_from_local(res);
    });

    ext_mailbox->get_folder_vector([this](error ec, mb::folder_vector_ptr res) {
        REQUIRE(!ec);
        cache_mailbox->update_folders_from_external(res);
    });

    loc_mailbox->get_messages_info_top(
        folder_fid,
        20,
        [&folder_path, this](error ec, mb::message_vector_ptr res) {
            REQUIRE(!ec);
            set_folder_top(cache_mailbox, folder_path, res);
            cache_mailbox->update_downloaded_range(folder_path, mb::imap_range{ 1, 1 });
        });

    io->post([this]() { sync_oldest(); });
    io->run();
    io->reset();

    REQUIRE(!(*err));
    REQUIRE(loc_mailbox->get_folder_messages(folder_fid).size() == 3);
}

TEST_CASE_METHOD(sync_oldest_test, "ensure skip folders with non ok status in sync oldest")
{
    mb::path_t folder_path{ "INBOX", '|' };

    loc_mailbox = create_from_json(LOCAL_MAILBOX_PATH, local_mock_type::type_err_folder_operations);
    loc_mailbox->get_folder_vector([this](error ec, mb::folder_vector_ptr folders) {
        REQUIRE(!ec);
        cache_mailbox->set_initial_folders(folders);
        cache_mailbox->update_folders_from_local(folders);
    });
    ext_mailbox =
        create_from_json(EXTERNAL_MAILBOX_PATH, external_mock_type::type_err_folder_operations);

    io->post([this]() { sync_folders(); });
    io->run();
    io->reset();

    REQUIRE(!(*err));
    auto folders_ = cache_mailbox->folders();
    mb::path_vector paths;
    for (auto& [path, folder] : folders_)
    {
        if (folder.status == mb::folder::status_t::ok)
        {
            paths.push_back(folder.path);
        }
    }
    REQUIRE(paths.size() == 1);
    REQUIRE(paths.front() == folder_path);

    io->post([this]() { sync_oldest(); });
    io->run();
    io->reset();

    REQUIRE(!(*err));
}

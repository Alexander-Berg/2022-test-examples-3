#include "common.h"
#include "mailbox_mocks.h"

#include <src/common/account.h>
#include <src/common/context.h>
#include <src/mailbox/data_types/cache_mailbox.h>
#include <src/xeno/operations/environment.h>
#include <src/xeno/operations/sync/list_status_op.h>
#include <src/xeno/operations/sync/clear_mailbox_op.h>
#include <src/xeno/operations/sync/create_local_folder_op.h>
#include <src/xeno/operations/sync/create_external_folder_op.h>
#include <src/xeno/operations/sync/delete_local_folder_op.h>
#include <src/xeno/operations/sync/sync_folders_op.h>
#include <src/xeno/operations/sync/main_op.h>
#include <src/xeno/xeno_settings.h>

#include <yplatform/log.h>
#include <catch.hpp>

#include <algorithm>
#include <memory>

using io_service = boost::asio::io_service;
using io_service_ptr = std::shared_ptr<io_service>;
using code = xeno::code;

static const mb::num_t CHUNK = 5;
static const uint32_t newest_count = 20;
static const uint32_t oldest_count = 100;
static const uint32_t newest_downloading_retries = 3;
static const uint32_t redownload_messages_cache_size = 0;

struct sync_folders_test
{
    sync_folders_test()
        : ext_mailbox{ std::make_shared<ext_mb::ext_mailbox_mock>() }
        , loc_mailbox{ std::make_shared<loc_mb::loc_mailbox_mock>() }
        , cache_mailbox{ std::make_shared<mb::cache_mailbox>() }
        , io_internal{ std::make_shared<io_service>() }
        , io(io_internal.get())
        , ctx{ boost::make_shared<xeno::context>() }
        , logger_internal{ std::make_shared<xeno::logger_t>() }
    {
        xeno::synchronization_settings settings;
        settings.newest_count = newest_count;
        settings.oldest_count = oldest_count;
        settings.newest_downloading_retries = newest_downloading_retries;
        settings.redownload_messages_cache_size = redownload_messages_cache_size;
        sync_settings = std::make_shared<const xeno::synchronization_settings>(std::move(settings));
        cache_mailbox->logger() = *logger_internal.get();
    }

    void list_status()
    {
        auto env = make_env(io, ctx, *logger_internal.get(), stat, handler, *this);
        env.ext_mailbox = ext_mailbox;
        env.loc_mailbox = loc_mailbox;
        env.cache_mailbox = cache_mailbox;
        env.sync_settings = sync_settings;

        xeno::spawn<xeno::list_status_op>(std::move(env));
    }

    void clear_mailbox()
    {
        auto env = make_env(io, ctx, *logger_internal.get(), stat, handler, *this);
        env.ext_mailbox = ext_mailbox;
        env.loc_mailbox = loc_mailbox;
        env.cache_mailbox = cache_mailbox;
        env.sync_settings = sync_settings;

        xeno::spawn<xeno::clear_mailbox_op>(std::move(env));
    }

    void create_local_folder(const mb::folder& folder)
    {
        auto env = make_env(io, ctx, *logger_internal.get(), stat, handler, *this);
        env.ext_mailbox = ext_mailbox;
        env.loc_mailbox = loc_mailbox;
        env.cache_mailbox = cache_mailbox;
        env.sync_settings = sync_settings;

        xeno::spawn<xeno::create_local_folder_op>(std::move(env), folder);
    }

    void delete_local_folder(const mb::fid_t& fid)
    {
        auto env = make_env(io, ctx, *logger_internal.get(), stat, handler, *this);
        env.ext_mailbox = ext_mailbox;
        env.loc_mailbox = loc_mailbox;
        env.cache_mailbox = cache_mailbox;
        env.sync_settings = sync_settings;

        xeno::spawn<xeno::delete_local_folder_op>(
            std::move(env), *cache_mailbox->get_folder_by_fid(fid));
    }

    void create_external_folder(const mb::path_t& path)
    {
        auto env = make_env(io, ctx, *logger_internal.get(), stat, handler, *this);
        env.ext_mailbox = ext_mailbox;
        env.loc_mailbox = loc_mailbox;
        env.cache_mailbox = cache_mailbox;
        env.sync_settings = sync_settings;

        xeno::spawn<xeno::create_external_folder_op>(std::move(env), path);
    }

    void full_iteration()
    {
        auto env = make_env(io, ctx, *logger_internal.get(), stat, handler, *this);
        env.ext_mailbox = ext_mailbox;
        env.loc_mailbox = loc_mailbox;
        env.cache_mailbox = cache_mailbox;
        env.sync_settings = sync_settings;

        xeno::spawn<xeno::main_op>(std::move(env));
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

TEST_CASE_METHOD(sync_folders_test, "sync folders")
{
    ext_mailbox = create_from_json(EXTERNAL_MAILBOX_PATH, external_mock_type::type_normal);
    loc_mailbox = create_from_json(LOCAL_MAILBOX_PATH, local_mock_type::type_normal);
    io->post([this]() {
        auto folders = std::make_shared<mb::folder_vector>();
        loc_mailbox->get_folder_vector([&folders](error ec, mb::folder_vector_ptr local_folders) {
            REQUIRE(!ec);
            folders = local_folders;
        });
        cache_mailbox->update_folders_from_local(folders);

        for (auto& folder : *folders)
        {
            loc_mailbox->get_messages_info_top(
                folder.fid, CHUNK, [this, &folder](error ec, mb::message_vector_ptr messages) {
                    REQUIRE(ec == code::ok);
                    set_folder_top(cache_mailbox, folder.path, messages);
                });
        }
    });
    io->run();
    io->reset();

    auto& folders = cache_mailbox->folders();

    REQUIRE(folders.size() == 8);

    io->post([this]() { sync_folders(); });
    io->run();
    io->reset();

    REQUIRE(!(*err));
    REQUIRE(folders.size() == 7);

    int to_delete_local{ 0 }, to_create_local{ 0 }, to_create_external{ 0 };
    for (const auto& folder_it : folders)
    {
        auto& folder = folder_it.second;
        if (folder.status == mb::folder::status_t::to_clear_and_delete)
        {
            ++to_delete_local;
        }
        else if (folder.status == mb::folder::status_t::to_create_local)
        {
            ++to_create_local;
        }
        else if (folder.status == mb::folder::status_t::to_create_external)
        {
            ++to_create_external;
        }
    }
    REQUIRE(to_create_local == 0);
    REQUIRE(to_create_external == 0);
    REQUIRE(to_delete_local == 0);

    ext_mailbox->change_uidvalidity(false, { mb::path_t{ "INBOX", '|' } });
    auto auth = xeno::auth_data();
    auth.xtoken_id = "IamToken";
    auth.security_lock = true;
    cache_mailbox->account().auth_data.push_back(std::move(auth));
    REQUIRE(cache_mailbox->account().has_security_lock() == true);
    io->post([this]() { sync_folders(); });
    io->run();
    io->reset();
    REQUIRE(!(*err));
    REQUIRE(cache_mailbox->account().has_security_lock() == true);
    io->post([this]() { full_iteration(); });
    io->run();
    io->reset();
    REQUIRE(cache_mailbox->account().has_security_lock() == false);
    REQUIRE(cache_mailbox->folders().size() == 7);

    ext_mailbox->change_uidvalidity();

    auth.xtoken_id = "IamToken2";
    cache_mailbox->account().auth_data.push_back(std::move(auth));
    REQUIRE(cache_mailbox->account().has_security_lock() == true);
    io->post([this]() { full_iteration(); });
    io->run();
    io->reset();
    REQUIRE(*err == code::need_restart);
    REQUIRE(cache_mailbox->account().has_security_lock());
    REQUIRE(cache_mailbox->folders().size() == 3);
}

TEST_CASE_METHOD(sync_folders_test, "sync folders/list status")
{
    {
        mb::folder_vector_ptr folders = std::make_shared<mb::folder_vector>();
        mb::folder folder({ "INBOX", '|' }, "1", 77777, 1, 0);
        folder.type = mb::folder::type_t::inbox;
        folders->emplace_back(folder);
        cache_mailbox->update_folders_from_local(folders);
    }
    ext_mailbox = create_from_json(EXTERNAL_MAILBOX_PATH, external_mock_type::type_normal);
    {
        io->post([this]() { list_status(); });
        io->run();

        REQUIRE(!(*err));
        const auto& folders = cache_mailbox->folders();
        REQUIRE(folders.size() == 5);
        const auto& inbox_it = folders.find(mb::path_t{ "INBOX", '|' });
        REQUIRE(inbox_it != folders.end());
        const auto inbox = inbox_it->second;
        REQUIRE(inbox.type != mb::folder::type_t::user);
        REQUIRE(inbox.status == mb::folder::status_t::ok);
        REQUIRE(inbox.count == 0);
        REQUIRE(inbox.uidvalidity == 77777);

        io->reset();
    }

    {
        io->post([this]() {
            auto folders = std::make_shared<mb::folder_vector>();
            mb::folder drafts_folder(mb::path_t{ "drafts", '|' });
            drafts_folder.type = mb::folder::type_t::drafts;
            drafts_folder.fid = "6";
            folders->push_back(std::move(drafts_folder));
            mb::folder user_folder(mb::path_t{ "tst_list_status", '|' });
            user_folder.fid = "55";
            folders->push_back(mb::folder(std::move(user_folder)));
            cache_mailbox->update_folders_from_local(folders);

            ext_mailbox->create_folder(mb::path_t{ "asd|asd", '|' }, *this);
            ext_mailbox->delete_folder(
                mb::path_t{ "test_main_folder|child_folder_two", '|' }, *this);
            ext_mailbox->update_folder(
                mb::path_t{ "INBOX", '|' }, mb::path_t{ "Входящие", '|' }, 77777, *this);

            list_status();
        });
        io->run();

        const auto& folders = cache_mailbox->folders();
        REQUIRE(folders.size() == 8);
        int to_delete_local{ 0 }, to_create_local{ 0 }, to_create_external{ 0 },
            to_update_and_clear{ 0 };
        for (const auto& folder_it : folders)
        {
            auto& folder = folder_it.second;
            REQUIRE(folder_it.first == folder_it.second.path);
            if (folder.status == mb::folder::status_t::to_delete)
            {
                ++to_delete_local;
            }
            else if (folder.status == mb::folder::status_t::to_create_local)
            {
                ++to_create_local;
            }
            else if (folder.status == mb::folder::status_t::to_create_external)
            {
                ++to_create_external;
            }
            else if (folder.status == mb::folder::status_t::to_update_and_clear)
            {
                ++to_update_and_clear;
            }
        }
        REQUIRE(to_create_local == 5);
        // TODO No marking for create external right now
        REQUIRE(to_create_external == 0);
        REQUIRE(to_delete_local == 1);
        REQUIRE(to_update_and_clear == 1);

        io->reset();
    }
}

TEST_CASE_METHOD(sync_folders_test, "sync folders/clear mailbox")
{
    loc_mailbox = create_from_json(LOCAL_MAILBOX_PATH, local_mock_type::type_normal);
    {
        auto folders = std::make_shared<mb::folder_vector>();
        mb::folder folder(mb::path_t{ "drafts", '|' });
        folder.type = mb::folder::type_t::drafts;
        folders->push_back(std::move(folder));
        folders->push_back(mb::folder(mb::path_t{ "tst_clear_mailbox", '|' }));
        cache_mailbox->update_folders_from_local(folders);
        REQUIRE(cache_mailbox->folders().size() == 2);
    }

    {
        io->post([this]() { clear_mailbox(); });
        io->run();

        REQUIRE(*err == code::need_restart);

        const auto& folders = cache_mailbox->folders();
        REQUIRE(folders.size() == 3);
        const auto& inbox_it = folders.find(mb::path_t{ "INBOX", '|' });
        REQUIRE(inbox_it != folders.end());
        const auto inbox = inbox_it->second;
        REQUIRE(inbox.type != mb::folder::type_t::user);
        REQUIRE(inbox.status == mb::folder::status_t::ok);
        REQUIRE(inbox.count == 0);
        REQUIRE(inbox.uidvalidity == 0);

        io->reset();
    }
}

TEST_CASE_METHOD(sync_folders_test, "sync folders/create local folder")
{
    {
        auto folders = std::make_shared<mb::folder_vector>();
        mb::folder folder(mb::path_t{ "drafts", '|' });
        folder.type = mb::folder::type_t::drafts;
        folder.fid = "123";
        folders->push_back(std::move(folder));
        cache_mailbox->update_folders_from_local(folders);
        folders->push_back(mb::folder(mb::path_t{ "tst_create_local_1", '|' }));
        folders->push_back(mb::folder(mb::path_t{ "tst_create_local_2", '|' }));
        cache_mailbox->update_folders_from_external(folders);
    }

    auto& folders = cache_mailbox->folders();
    REQUIRE(folders.size() == 3);
    std::vector<mb::folder> to_create;
    for (const auto& folder_it : folders)
    {
        const auto& folder = folder_it.second;
        if (folder.status == mb::folder::status_t::to_create_local)
        {
            to_create.emplace_back(folder);
        }
    }
    REQUIRE(to_create.size() == 2);

    for (const auto& folder : to_create)
    {
        io->post([this, &folder]() { create_local_folder(folder); });
        io->run();
        io->reset();

        REQUIRE(!(*err));
    }

    REQUIRE(folders.size() == 3);
    int to_create_local = 0;
    for (const auto& folder_it : folders)
    {
        auto& folder = folder_it.second;
        if (folder.status == mb::folder::status_t::to_create_local)
        {
            ++to_create_local;
        }
    }
    REQUIRE(to_create_local == 0);
}

TEST_CASE_METHOD(sync_folders_test, "sync folders/create external folder")
{
    ext_mailbox = create_from_json(EXTERNAL_MAILBOX_PATH, external_mock_type::type_normal);
    {
        auto folders = std::make_shared<mb::folder_vector>();
        mb::folder drafts_folder(mb::path_t{ "drafts", '|' });
        drafts_folder.type = mb::folder::type_t::drafts;
        drafts_folder.fid = "6";
        folders->push_back(std::move(drafts_folder));
        mb::folder user_folder(mb::path_t{ "tst_create_external_1", '|' });
        user_folder.fid = "77";
        folders->push_back(std::move(user_folder));
        cache_mailbox->update_folders_from_local(folders);
        cache_mailbox->update_folders_from_external(std::make_shared<mb::folder_vector>());
    }

    auto& folders = cache_mailbox->folders();

    REQUIRE(folders.size() == 2);

    std::vector<mb::path_t> paths;
    for (const auto& folder_it : folders)
    {
        const auto& folder = folder_it.second;
        if (folder.status == mb::folder::status_t::to_create_external)
        {
            paths.emplace_back(folder.path);
        }
    }

    // TODO No marking for create external right now
    // REQUIRE(paths.size() == 0);
    //{
    //    io->post([this, &paths](){
    //        create_external_folder(paths.at(0));
    //    });
    //    io->run();
    //    io->reset();
    //
    //    REQUIRE(!(*err));
    //}

    int to_create_external = 0;
    for (const auto& folder_it : folders)
    {
        auto& folder = folder_it.second;
        if (folder.status == mb::folder::status_t::to_create_external)
        {
            ++to_create_external;
        }
    }
    REQUIRE(to_create_external == 0);
}

TEST_CASE_METHOD(sync_folders_test, "sync folders/delete local folder")
{
    loc_mailbox = create_from_json(LOCAL_MAILBOX_PATH, local_mock_type::type_normal);
    {
        auto folders = std::make_shared<mb::folder_vector>();
        io->post([this, &folders]() {
            loc_mailbox->get_folder_vector(
                [&folders](error ec, mb::folder_vector_ptr folder_vector) {
                    folders = folder_vector;
                    REQUIRE(!ec);
                });
        });
        io->run();
        io->reset();

        cache_mailbox->update_folders_from_local(folders);
        cache_mailbox->update_folders_from_external(std::make_shared<mb::folder_vector>());
    }

    auto& folders = cache_mailbox->folders();
    REQUIRE(folders.size() == 8);

    using path_fid_pair = std::pair<mb::path_t, mb::fid_t>;

    std::vector<path_fid_pair> to_delete_vec;
    for (const auto& folder_it : folders)
    {
        const auto& folder = folder_it.second;
        if (folder.status == mb::folder::status_t::to_delete)
        {
            to_delete_vec.emplace_back(path_fid_pair(folder.path, folder.fid));
        }
    }
    REQUIRE(to_delete_vec.size() == 5);

    for (const auto& path_fid : to_delete_vec)
    {
        auto parent_path = path_fid.first.get_parent_path();
        if (!parent_path.empty())
        {
            auto path_it = std::find_if(
                to_delete_vec.begin(),
                to_delete_vec.end(),
                [&parent_path](const std::pair<mb::path_t, const mb::fid_t>& path_fid) {
                    return parent_path == path_fid.first;
                });
            if (path_it != to_delete_vec.end())
            {
                continue;
            }
        }

        io->post([this, &path_fid]() { delete_local_folder(path_fid.second); });
        io->run();
        io->reset();
    }

    REQUIRE(folders.size() == 7);
}

TEST_CASE_METHOD(sync_folders_test, "sync folders/check clear system folder in remote imap")
{
    ext_mailbox = create_from_json(EXTERNAL_MAILBOX_PATH, external_mock_type::type_normal);
    loc_mailbox = create_from_json(LOCAL_MAILBOX_PATH, local_mock_type::type_normal);
    io->post([this]() {
        auto folders = std::make_shared<mb::folder_vector>();
        loc_mailbox->get_folder_vector([&folders](error ec, mb::folder_vector_ptr local_folders) {
            REQUIRE(!ec);
            folders = local_folders;
        });
        cache_mailbox->update_folders_from_local(folders);
    });
    io->run();
    io->reset();

    auto& folders = cache_mailbox->folders();
    REQUIRE(folders.size() == 8);

    io->post([this]() { sync_folders(); });
    io->run();
    io->reset();

    REQUIRE(!(*err));
    REQUIRE(folders.size() == 7);

    io->post([this]() {
        ext_mailbox->clear_folder(mb::path_t{ "INBOX", '|' }, [](error ec) { REQUIRE(!ec); });
        list_status();
    });
    io->run();
    io->reset();

    auto it = folders.find(mb::path_t{ "INBOX", '|' });
    REQUIRE(it != folders.end());
}

TEST_CASE_METHOD(sync_folders_test, "deleting folder with empty fid")
{
    mb::path_t folder_path = { "test_main_folder", '|' };
    ext_mailbox = create_from_json(EXTERNAL_MAILBOX_PATH, external_mock_type::type_normal);
    loc_mailbox = create_from_json(LOCAL_MAILBOX_PATH, local_mock_type::type_normal);
    io->post([this]() {
        loc_mailbox->get_folder_vector([this](error ec, mb::folder_vector_ptr folders) {
            REQUIRE(!ec);
            cache_mailbox->update_folders_from_local(folders);
        });

        ext_mailbox->get_folder_vector([this](error ec, mb::folder_vector_ptr folders) {
            REQUIRE(!ec);
            cache_mailbox->update_folders_from_external(folders);
        });
    });
    io->run();
    io->reset();

    ext_mailbox->delete_folder(folder_path, [](error ec) { REQUIRE(!ec); });

    io->post([this]() { sync_folders(); });
    io->run();
    io->reset();

    REQUIRE(!(*err));
    auto folders = cache_mailbox->folders_copy();
    auto it =
        std::find_if(folders->begin(), folders->end(), [&folder_path](const mb::folder& folder) {
            return folder.path == folder_path;
        });

    REQUIRE(it == folders->end());

    loc_mailbox->get_folder_vector([&folder_path](error ec, mb::folder_vector_ptr folders) {
        REQUIRE(!ec);
        auto it = std::find_if(
            folders->begin(), folders->end(), [&folder_path](const mb::folder& folder) {
                return folder.path == folder_path;
            });

        REQUIRE(it == folders->end());
    });
}

TEST_CASE_METHOD(sync_folders_test, "check that fake hierarchial folders created properly")
{
    mb::path_t folder_path{ "test_main_folder", '|' };

    loc_mailbox = create_from_json(LOCAL_MAILBOX_PATH, local_mock_type::type_normal);
    ext_mailbox = create_from_json(EXTERNAL_MAILBOX_PATH, external_mock_type::type_normal);
    ext_mailbox->make_folder_fake(folder_path);

    io->post([this]() { sync_folders(); });
    io->run();
    io->reset();

    REQUIRE(!(*err));
    ext_mailbox->get_folder_vector([&folder_path](error ec, mb::folder_vector_ptr folders) {
        REQUIRE(!ec);
        auto it =
            std::find_if(folders->begin(), folders->end(), [&folder_path](const mb::folder& f) {
                return f.path == folder_path;
            });

        REQUIRE(it != folders->end());
        REQUIRE(it->status == mb::folder::status_t::ok);
    });

    loc_mailbox->get_folder_vector([&folder_path](error ec, mb::folder_vector_ptr folders) {
        REQUIRE(!ec);
        auto it =
            std::find_if(folders->begin(), folders->end(), [&folder_path](const mb::folder& f) {
                return f.path == folder_path;
            });

        REQUIRE(it != folders->end());
        REQUIRE(it->status == mb::folder::status_t::ok);
    });
}

TEST_CASE_METHOD(sync_folders_test, "check that sync folders errors does not break synchronization")
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

    io->post([this]() { list_status(); });
    io->run();
    io->reset();
    REQUIRE(!(*err));
    auto folders = cache_mailbox->folders_copy();
    int errors_count = 0;
    for (auto& folder : *folders)
    {
        if (folder.status == mb::folder::status_t::sync_error)
        {
            errors_count++;
        }
    }

    REQUIRE(errors_count == 2);
}

TEST_CASE_METHOD(sync_folders_test, "check that we remove folder from cache")
{
    mb::path_t test_path{ "test_path", '|' };

    ext_mailbox = create_from_json(EXTERNAL_MAILBOX_PATH, external_mock_type::type_normal);
    loc_mailbox = create_from_json(LOCAL_MAILBOX_PATH, local_mock_type::type_err_folder_operations);
    cache_mailbox = std::make_shared<mb::cache_mailbox>();

    ext_mailbox->create_folder(test_path, [](error ec) { REQUIRE(!ec); });
    io->post([this]() { sync_folders(); });
    io->run();
    io->reset();

    REQUIRE(!(*err));
    auto folder_in_cache = cache_mailbox->get_folder_by_path(test_path);
    REQUIRE(static_cast<bool>(folder_in_cache));
    REQUIRE(folder_in_cache->fid.empty());
    REQUIRE(folder_in_cache->status == mb::folder::status_t::sync_error);

    ext_mailbox->delete_folder(test_path, [](error ec) { REQUIRE(!ec); });
    io->post([this]() { sync_folders(); });
    io->run();
    io->reset();

    REQUIRE(!(*err));
    folder_in_cache = cache_mailbox->get_folder_by_path(test_path);
    REQUIRE(!folder_in_cache);
}

TEST_CASE_METHOD(sync_folders_test, "check that we set delimeter to folder from ext mailbox")
{
    auto test_path = mb::path_t("test_path", 0);
    auto folders = std::make_shared<mb::folder_vector>(1, mb::folder(test_path));
    cache_mailbox->update_folders_from_local(folders);
    REQUIRE(cache_mailbox->get_folder_by_path(test_path)->path.delim == 0);
    folders->back().path.delim = '/';
    cache_mailbox->update_folders_from_external(folders);
    REQUIRE(cache_mailbox->get_folder_by_path(test_path)->path.delim == '/');
}

TEST_CASE_METHOD(sync_folders_test, "list_status for changing folder types")
{
    ext_mailbox = create_from_json(
        EXTERNAL_MAILBOX_FOR_CHANGING_FOLDER_TYPE_TESTS, external_mock_type::type_normal);
    loc_mailbox = create_from_json(
        LOCAL_MAILBOX_FOR_CHANGING_FOLDER_TYPE_TESTS, local_mock_type::type_normal);
    io->post([this]() {
        auto folders = std::make_shared<mb::folder_vector>();
        loc_mailbox->get_folder_vector([&folders](error ec, mb::folder_vector_ptr local_folders) {
            REQUIRE(!ec);
            folders = local_folders;
        });
        cache_mailbox->update_folders_from_local(folders);
    });

    io->post([this]() { list_status(); });
    io->run();
    io->reset();
    auto folders = cache_mailbox->folders();

    auto folder_it = folders.find({ "inbox_to_user", '|' });
    REQUIRE(folder_it != folders.end());
    REQUIRE(folder_it->second.status == mb::folder::status_t::to_delete);

    folder_it = folders.find({ "user_to_inbox", '|' });
    REQUIRE(folder_it != folders.end());
    REQUIRE(folder_it->second.status == mb::folder::status_t::to_delete);

    folder_it = folders.find({ "sent_to_draft", '|' });
    REQUIRE(folder_it != folders.end());
    REQUIRE(folder_it->second.status == mb::folder::status_t::to_delete);

    folder_it = folders.find({ "draft_to_sent", '|' });
    REQUIRE(folder_it != folders.end());
    REQUIRE(folder_it->second.status == mb::folder::status_t::to_delete);
}

TEST_CASE_METHOD(sync_folders_test, "reiniting folders after type change")
{
    ext_mailbox = create_from_json(
        EXTERNAL_MAILBOX_FOR_CHANGING_FOLDER_TYPE_TESTS, external_mock_type::type_normal);
    loc_mailbox = create_from_json(
        LOCAL_MAILBOX_FOR_CHANGING_FOLDER_TYPE_TESTS, local_mock_type::type_normal);
    io->post([this]() {
        auto folders = std::make_shared<mb::folder_vector>();
        loc_mailbox->get_folder_vector([&folders](error ec, mb::folder_vector_ptr local_folders) {
            REQUIRE(!ec);
            folders = local_folders;
        });

        cache_mailbox->update_folders_from_local(folders);
        for (auto& folder : *folders)
        {
            loc_mailbox->get_messages_info_top(
                folder.fid, CHUNK, [this, &folder](error ec, mb::message_vector_ptr messages) {
                    REQUIRE(ec == code::ok);
                    set_folder_top(cache_mailbox, folder.path, messages);
                });
        }
    });
    io->run();
    io->reset();

    io->post([this]() { sync_folders(); });
    io->run();
    io->reset();
    loc_mailbox->get_folder_vector([](error ec, mb::folder_vector_ptr local_folders) {
        REQUIRE(!ec);
        REQUIRE(local_folders->size() == 3);
        for (auto folder_it = local_folders->begin(); folder_it != local_folders->end();
             ++folder_it)
        {
            REQUIRE(folder_it->path.empty());
        }
    });

    io->post([this]() { sync_folders(); });
    io->run();
    io->reset();

    std::vector<std::pair<mb::path_t, mb::folder::type_t>> check_list = {
        { { "inbox_to_user", '|' }, mb::folder::type_t::user },
        { { "user_to_inbox", '|' }, mb::folder::type_t::inbox },
        { { "sent_to_draft", '|' }, mb::folder::type_t::drafts },
        { { "draft_to_sent", '|' }, mb::folder::type_t::sent }
    };
    loc_mailbox->get_folder_vector([&check_list](error ec, mb::folder_vector_ptr local_folders) {
        REQUIRE(!ec);
        for (auto& [path, type] : check_list)
        {
            auto obj = path;
            auto folder_it = std::find_if(
                local_folders->begin(), local_folders->end(), [obj](const mb::folder& folder) {
                    return obj == folder.path;
                });
            REQUIRE(folder_it != local_folders->end());
            REQUIRE(folder_it->type == type);
        }
    });
}

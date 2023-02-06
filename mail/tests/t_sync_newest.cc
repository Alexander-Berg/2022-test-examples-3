#include "common.h"
#include "mailbox_mocks.h"

#include <src/common/context.h>
#include <src/xeno/operations/environment.h>
#include <src/mailbox/data_types/cache_mailbox.h>
#include <src/xeno/operations/sync/sync_newest_messages_op.h>
#include <src/xeno/operations/sync/sync_newest_op.h>
#include <src/xeno/load_cache_op.h>

#include <catch.hpp>

#include <algorithm>
#include <memory>

using io_service = boost::asio::io_service;
using io_service_ptr = std::shared_ptr<io_service>;
using code = xeno::code;

static const mb::num_t CHUNK = 5;
static const mb::num_t TRIPLE_CHUNK = 15;
static const mb::num_t TURBO_SYNC_CHUNK = 2 * CHUNK;
static const uint32_t RETRIES = 3;

struct sync_newest_test
{
    sync_newest_test()
        : ext_mailbox{ create_from_json(EXTERNAL_MAILBOX_PATH, external_mock_type::type_normal) }
        , loc_mailbox{ create_from_json(LOCAL_MAILBOX_PATH, local_mock_type::type_normal) }
        , cache_mailbox{ std::make_shared<mb::cache_mailbox>() }
        , sync_settings{ std::make_shared<const xeno::synchronization_settings>() }
        , io_internal{ std::make_shared<io_service>() }
        , io(io_internal.get())
        , ctx{ boost::make_shared<xeno::context>() }
        , logger_internal{ std::make_shared<xeno::logger_t>() }
    {
    }

    void sync_newest(mb::num_t chunk = CHUNK, const xeno::turbo_sync_settings& turbo_sync = {})
    {
        auto env = make_env(io, ctx, *logger_internal.get(), stat, handler, *this);
        env.ext_mailbox = ext_mailbox;
        env.loc_mailbox = loc_mailbox;
        env.cache_mailbox = cache_mailbox;

        xeno::synchronization_settings sync_settings;
        sync_settings.newest_count = chunk;
        sync_settings.newest_downloading_retries = RETRIES;
        sync_settings.turbo_sync = turbo_sync;
        env.sync_settings =
            std::make_shared<const xeno::synchronization_settings>(std::move(sync_settings));

        mb::path_vector paths_to_sync;
        {
            auto folders = env.cache_mailbox->folders();
            for (auto& [path, folder] : folders)
            {
                paths_to_sync.emplace_back(folder.path);
            }
        }
        xeno::spawn<xeno::sync_newest_op>(std::move(env), paths_to_sync);
    }

    void operator()(error ec = {})
    {
        *err = ec;
        if (ec == code::store_message_error)
        {
            ++(*store_error_cnt);
        }
    }

    void run(const std::function<void()>& handler)
    {
        io->post(handler);
        io->run();
        io->reset();
    }

    template <typename Handler>
    auto io_wrap(Handler&& h) const
    {
        return io->wrap(std::forward<Handler>(h));
    }

    bool exist_message_in_cache_mb(mb::path_t path, mb::imap_id_t id)
    {
        auto messages_top = get_folder_top(cache_mailbox, path);
        return messages_top.count(id) ? true : false;
    };

    bool exist_message_in_local_mb(mb::fid_t fid, mb::imap_id_t id, mb::num_t chunk)
    {
        bool exist = false;
        run([this, fid, id, &exist, chunk]() mutable {
            loc_mailbox->get_messages_info_top(
                fid, chunk, [id, &exist](error ec, mb::message_vector_ptr messages) mutable {
                    REQUIRE(!ec);
                    auto it = std::find_if(
                        messages->begin(), messages->end(), [id](const mb::message& msg) {
                            return msg.id == id;
                        });
                    exist = (it == messages->end()) ? false : true;
                });
        });

        return exist;
    };

    ext_mb::ext_mailbox_mock_ptr ext_mailbox;
    loc_mb::loc_mailbox_mock_ptr loc_mailbox;
    mb::cache_mailbox_ptr cache_mailbox;
    xeno::synchronization_settings_ptr sync_settings;
    io_service_ptr io_internal;
    boost::asio::io_service* io;
    std::shared_ptr<error> err{ std::make_shared<error>() };
    xeno::context_ptr ctx;
    xeno::logger_ptr logger_internal;
    std::shared_ptr<int> store_error_cnt{ std::make_shared<int>(0) };
    mb::path_t inbox_path = mb::path_t{ "INBOX", '|' };
    xeno::iteration_stat_ptr stat = std::make_shared<xeno::iteration_stat>();

    std::shared_ptr<interrupt_handler> handler = std::make_shared<interrupt_handler>();
};

TEST_CASE_METHOD(sync_newest_test, "sync newest")
{
    run([this]() {
        loc_mailbox->get_folder_vector([this](error ec, mb::folder_vector_ptr folder_vector) {
            REQUIRE(!ec);
            cache_mailbox->update_folders_from_local(folder_vector);
            REQUIRE(cache_mailbox->folders().size() == 8);
        });
        ext_mailbox->get_folder_vector([this](error ec, mb::folder_vector_ptr folder_vector) {
            REQUIRE(!ec);
            cache_mailbox->update_folders_from_external(folder_vector);
        });
    });

    auto fid = *cache_mailbox->get_fid_by_path(inbox_path);
    run([this, &fid]() {
        loc_mailbox->get_messages_info_top(
            fid, CHUNK, [this](error ec, mb::message_vector_ptr messages) {
                REQUIRE(!ec);
                REQUIRE(messages->size() == 5);
                set_folder_top(cache_mailbox, inbox_path, messages);
            });
        loc_mailbox->get_messages_info_top(
            fid, TRIPLE_CHUNK, [](error ec, mb::message_vector_ptr messages) {
                REQUIRE(!ec);
                REQUIRE(messages->size() == 10);
            });
    });

    {
        auto folders = cache_mailbox->folders();
        auto folder_it = folders.find(inbox_path);
        REQUIRE(folder_it != folders.end());
        REQUIRE(folder_it->second.downloaded_range.top() == 0);
        REQUIRE(folder_it->second.downloaded_range.bottom() == 0);
        auto msg_map = get_folder_top(cache_mailbox, inbox_path);

        REQUIRE(msg_map.size() == 5);
        for (auto& id_msg_pair : msg_map)
        {
            REQUIRE(id_msg_pair.first <= 14);
            REQUIRE(id_msg_pair.first >= 10);
        }
    }

    run([this]() { sync_newest(); });

    {
        auto folders = cache_mailbox->folders();
        auto folder_it = folders.find(inbox_path);
        REQUIRE(folder_it != folders.end());
        REQUIRE(folder_it->second.downloaded_range.top() != 0);
        REQUIRE(folder_it->second.downloaded_range.bottom() != 0);
        auto msg_map = get_folder_top(cache_mailbox, inbox_path);

        REQUIRE(msg_map.size() == 5);
        for (auto& id_msg_pair : msg_map)
        {
            REQUIRE(id_msg_pair.first <= 115);
            REQUIRE(id_msg_pair.first >= 13);
        }
    }

    run([this, &fid]() {
        loc_mailbox->get_messages_info_top(
            fid, TRIPLE_CHUNK, [](error ec, mb::message_vector_ptr messages) {
                REQUIRE(!ec);
                REQUIRE(messages->size() == 14);
            });
    });
}

TEST_CASE_METHOD(sync_newest_test, "error store message")
{
    loc_mailbox = create_from_json(LOCAL_MAILBOX_PATH, local_mock_type::type_err_store);

    run([this]() {
        loc_mailbox->get_folder_vector([this](error ec, mb::folder_vector_ptr folder_vector) {
            REQUIRE(!ec);
            cache_mailbox->update_folders_from_local(folder_vector);
        });
        ext_mailbox->get_folder_vector([this](error ec, mb::folder_vector_ptr folder_vector) {
            REQUIRE(!ec);
            cache_mailbox->update_folders_from_external(folder_vector);
        });
        set_folder_top(cache_mailbox, inbox_path, std::make_shared<mb::message_vector>());
    });

    run([this]() { sync_newest(); });

    {
        auto msg_map = get_folder_top(cache_mailbox, inbox_path);
        int cnt_status_to_download_body = 0;
        for (auto& id_msg_pair : msg_map)
        {
            if (id_msg_pair.second.status == mb::message::status_t::to_download_body)
            {
                ++cnt_status_to_download_body;
            }
        }
        REQUIRE(cnt_status_to_download_body == 5);
    }

    REQUIRE(*store_error_cnt == 0);

    run([this]() { sync_newest(); });

    {
        auto msg_map = get_folder_top(cache_mailbox, inbox_path);
        int cnt_status_to_download_body = 0;
        for (auto& id_msg_pair : msg_map)
        {
            if (id_msg_pair.second.status == mb::message::status_t::to_download_body)
            {
                ++cnt_status_to_download_body;
            }
        }
        REQUIRE(cnt_status_to_download_body == 5);
    }
}

TEST_CASE_METHOD(sync_newest_test, "error delete message")
{
    loc_mailbox = create_from_json(LOCAL_MAILBOX_PATH, local_mock_type::type_err_del);

    run([this]() {
        loc_mailbox->get_folder_vector([this](error ec, mb::folder_vector_ptr folder_vector) {
            REQUIRE(!ec);
            cache_mailbox->update_folders_from_local(folder_vector);
            for (auto& folder : *folder_vector)
            {
                set_folder_top(cache_mailbox, folder.path, std::make_shared<mb::message_vector>());
            }
        });
        ext_mailbox->get_folder_vector([this](error ec, mb::folder_vector_ptr folder_vector) {
            REQUIRE(!ec);
            cache_mailbox->update_folders_from_external(folder_vector);
        });
    });

    run([this]() { sync_newest(); });

    mb::imap_id_t id_to_delete = 115;

    run([this, id_to_delete]() {
        ext_mailbox->delete_messages(
            inbox_path, std::make_shared<mb::imap_id_vector>(1, id_to_delete), [](error ec) {
                REQUIRE(!ec);
            });
    });

    for (auto sync_cnt = 0; sync_cnt < 2; ++sync_cnt)
    {
        run([this]() { sync_newest(); });

        auto msg_map = get_folder_top(cache_mailbox, inbox_path);

        bool marked_to_delete = false;
        for (auto& id_msg_pair : msg_map)
        {
            auto& msg = id_msg_pair.second;
            if (msg.id == id_to_delete)
            {
                marked_to_delete = (msg.status == mb::message::status_t::to_delete);
            }
        }
        REQUIRE(marked_to_delete);
    }
}

TEST_CASE_METHOD(sync_newest_test, "error download body")
{
    ext_mailbox =
        create_from_json(EXTERNAL_MAILBOX_PATH, external_mock_type::type_err_download_body);

    run([this]() {
        loc_mailbox->get_folder_vector([this](error ec, mb::folder_vector_ptr folder_vector) {
            REQUIRE(!ec);
            cache_mailbox->update_folders_from_local(folder_vector);
        });
        for (auto& folder : cache_mailbox->folders())
        {
            loc_mailbox->clear_folder(folder.second.fid, [](error ec) { REQUIRE(!ec); });
        }
        ext_mailbox->get_folder_vector([this](error ec, mb::folder_vector_ptr folder_vector) {
            REQUIRE(!ec);
            cache_mailbox->update_folders_from_external(folder_vector);
        });
    });

    run([this]() { sync_newest(); });

    REQUIRE(!(*err));
    {
        auto msg_map = get_folder_top(cache_mailbox, inbox_path);
        auto inbox_fid = *cache_mailbox->get_fid_by_path(inbox_path);
        for (auto& id_msg : msg_map)
        {
            auto& msg = id_msg.second;
            INFO("Message: " << msg.id);
            REQUIRE(msg.errors_count == 1);
            REQUIRE(msg.saved_errors_count == 1);
            REQUIRE(msg.mid == 0);

            loc_mailbox->get_messages_info_by_id(
                inbox_fid,
                mb::imap_id_vector(1, msg.id),
                mb::msg_info_type::without_flags,
                [&msg](error err, mb::message_vector_ptr msgs) {
                    REQUIRE(!err);
                    REQUIRE(msgs->size() == 1);
                    auto& local_msg = msgs->front();
                    INFO("Message: " << local_msg.id);
                    REQUIRE(local_msg.errors_count == msg.errors_count);
                    REQUIRE(local_msg.mid == 0);
                });
        }
    }

    run([this]() { sync_newest(); });

    REQUIRE(!(*err));

    {
        auto msg_map = get_folder_top(cache_mailbox, inbox_path);
        auto inbox_fid = *cache_mailbox->get_fid_by_path(inbox_path);
        for (auto& id_msg : msg_map)
        {
            auto& msg = id_msg.second;
            INFO("Message: " << msg.id);
            REQUIRE(msg.errors_count == 2);
            REQUIRE(msg.saved_errors_count == 2);
            REQUIRE(msg.mid == 0);

            loc_mailbox->get_messages_info_by_id(
                inbox_fid,
                mb::imap_id_vector(1, msg.id),
                mb::msg_info_type::without_flags,
                [&msg](error err, mb::message_vector_ptr msgs) {
                    REQUIRE(!err);
                    REQUIRE(msgs->size() == 1);
                    auto& local_msg = msgs->front();
                    INFO("Message: " << local_msg.id);
                    REQUIRE(local_msg.errors_count == msg.errors_count);
                    REQUIRE(local_msg.mid == 0);
                });
        }
    }
}

TEST_CASE_METHOD(
    sync_newest_test,
    "shouldn't download messages again when top in external mailbox was deleted")
{
    ext_mailbox =
        create_from_json(EXTERNAL_MAILBOX_WITH_TOP_DELETED_PATH, external_mock_type::type_normal);

    run([this]() {
        auto cb = [this](error ec, mb::cache_mailbox_ptr res) {
            REQUIRE(!ec);
            cache_mailbox = res;
        };
        xeno::load_cache_op(ctx, loc_mailbox, std::move(cb))();

        ext_mailbox->get_folder_vector([this](error ec, mb::folder_vector_ptr folder_vector) {
            REQUIRE(!ec);
            cache_mailbox->update_folders_from_external(folder_vector);
        });
    });

    run([this]() { sync_newest(); });

    REQUIRE(!(*err));
    REQUIRE(loc_mailbox->store_errors_count() == 0);

    run([this]() { sync_newest(); });

    REQUIRE(!(*err));
    REQUIRE(loc_mailbox->store_errors_count() == 0);
}

TEST_CASE_METHOD(sync_newest_test, "delete messages from top in external mailbox")
{
    ext_mailbox = create_from_json(EXTERNAL_MAILBOX_PATH, external_mock_type::type_normal);
    loc_mailbox = create_from_json(EXTERNAL_MAILBOX_PATH, local_mock_type::type_normal);

    xeno::load_cache_op(ctx, loc_mailbox, [this](error ec, mb::cache_mailbox_ptr res) {
        REQUIRE(!ec);
        cache_mailbox = res;
    })();

    auto inbox = cache_mailbox->get_folder_by_type(mb::folder::type_t::inbox);
    mb::num_t top = static_cast<mb::num_t>(ext_mailbox->get_folder_messages(inbox->path).size());
    mb::num_t bottom = (top > TRIPLE_CHUNK) ? (top - TRIPLE_CHUNK + 1) : 1;
    mb::imap_id_t min_imap_id, max_imap_id;
    ext_mailbox->get_messages_info_by_num(
        inbox->path,
        top,
        bottom,
        [&min_imap_id, &max_imap_id](error ec, mb::message_vector_ptr messages) mutable {
            REQUIRE(!ec);
            auto min_max_messages = std::minmax_element(messages->begin(), messages->end());
            min_imap_id = min_max_messages.first->id;
            max_imap_id = min_max_messages.second->id;
        });

    run([this]() { sync_newest(TRIPLE_CHUNK); });

    REQUIRE(exist_message_in_cache_mb(inbox->path, min_imap_id));
    REQUIRE(exist_message_in_cache_mb(inbox->path, max_imap_id));
    REQUIRE(exist_message_in_local_mb(inbox->fid, min_imap_id, TRIPLE_CHUNK));
    REQUIRE(exist_message_in_local_mb(inbox->fid, max_imap_id, TRIPLE_CHUNK));

    mb::imap_id_vector ids = { min_imap_id, max_imap_id };
    ext_mailbox->delete_messages(
        inbox->path, std::make_shared<mb::imap_id_vector>(ids), [](error ec) { REQUIRE(!ec); });

    run([this]() { sync_newest(TRIPLE_CHUNK); });

    REQUIRE(!exist_message_in_cache_mb(inbox->path, min_imap_id));
    REQUIRE(!exist_message_in_cache_mb(inbox->path, max_imap_id));
    REQUIRE(!exist_message_in_local_mb(inbox->fid, max_imap_id, TRIPLE_CHUNK));
    REQUIRE(!exist_message_in_local_mb(inbox->fid, min_imap_id, TRIPLE_CHUNK));
}

TEST_CASE_METHOD(sync_newest_test, "ensure correct flags updation on first iteration")
{
    xeno::load_cache_op(ctx, loc_mailbox, [this](error ec, mb::cache_mailbox_ptr res) {
        REQUIRE(!ec);
        cache_mailbox = res;
    })();

    ext_mailbox->get_folder_vector([this](error ec, mb::folder_vector_ptr folder_vector) {
        REQUIRE(!ec);
        cache_mailbox->update_folders_from_external(folder_vector);
    });

    run([this]() { sync_newest(); });
    REQUIRE(!(*err));

    auto inbox = cache_mailbox->get_folder_by_type(mb::folder::type_t::inbox);
    mb::message_vector_ptr local_messages;
    loc_mailbox->get_messages_info_top(
        inbox->fid, CHUNK, [&local_messages](error ec, mb::message_vector_ptr messages) {
            REQUIRE(!ec);
            REQUIRE(messages->size() == CHUNK);
            local_messages = messages;
        });

    auto ext_count = static_cast<mb::num_t>(ext_mailbox->get_folder_messages(inbox->path).size());
    mb::message_vector_ptr external_messages;
    ext_mailbox->get_messages_info_by_num(
        inbox->path,
        ext_count,
        ext_count - CHUNK + 1,
        [&external_messages](error ec, mb::message_vector_ptr messages) {
            REQUIRE(!ec);
            REQUIRE(messages->size() == CHUNK);
            external_messages = messages;
        });

    for (auto& ext_msg : *external_messages)
    {
        auto it = std::find_if(
            local_messages->begin(),
            local_messages->end(),
            [&ext_msg](const mb::message& local_msg) { return local_msg.id == ext_msg.id; });

        REQUIRE(it != local_messages->end());
        REQUIRE(ext_msg.flags == it->flags);
    }
}

TEST_CASE_METHOD(sync_newest_test, "unlock api_read flag after sync newest")
{
    xeno::load_cache_op(ctx, loc_mailbox, [this](error ec, mb::cache_mailbox_ptr res) {
        REQUIRE(!ec);
        cache_mailbox = res;
    })();

    auto folder_path = mb::path_t{ "INBOX", '|' };
    REQUIRE(cache_mailbox->get_folder(folder_path).api_read_lock);

    run([this]() { sync_newest(); });
    REQUIRE(!(*err));

    REQUIRE(cache_mailbox->get_folder(folder_path).api_read_lock == false);
}

TEST_CASE_METHOD(sync_newest_test, "turbo sync")
{
    ext_mailbox =
        create_from_json(EXTERNAL_MAILBOX_FOR_TURBO_SYNC_PATH, external_mock_type::type_normal);
    loc_mailbox = create_from_json(LOCAL_MAILBOX_FOR_TURBO_SYNC_PATH, local_mock_type::type_normal);

    xeno::load_cache_op(ctx, loc_mailbox, [this](error ec, mb::cache_mailbox_ptr res) {
        REQUIRE(!ec);
        cache_mailbox = res;
    })();

    REQUIRE(
        cache_mailbox->sync_newest_state()->turbo_sync.folders[inbox_path].status ==
        mb::turbo_sync_status::required);

    run([this]() {
        xeno::turbo_sync_settings settings;
        settings.folders[mb::folder::type_t::inbox].count = TURBO_SYNC_CHUNK;
        sync_newest(CHUNK, settings);
    });
    REQUIRE(!(*err));

    auto inbox = cache_mailbox->get_folder(inbox_path);
    auto sent = cache_mailbox->get_folder(mb::path_t("Sent", '|'));
    loc_mailbox->get_messages_info_top(
        inbox.fid, 2 * TURBO_SYNC_CHUNK, [inbox](error ec, mb::message_vector_ptr messages) {
            REQUIRE(!ec);
            REQUIRE(messages->size() == TURBO_SYNC_CHUNK);
            for (auto& message : *messages)
            {
                REQUIRE(message.mid);
                REQUIRE(message.id <= inbox.count);
                REQUIRE(message.id > inbox.count - TURBO_SYNC_CHUNK);
            }
        });
    loc_mailbox->get_messages_info_top(
        sent.fid, 2 * TURBO_SYNC_CHUNK, [sent](error ec, mb::message_vector_ptr messages) {
            REQUIRE(!ec);
            REQUIRE(messages->size() == CHUNK);
            for (auto& message : *messages)
            {
                REQUIRE(message.mid);
                REQUIRE(message.id <= sent.count);
                REQUIRE(message.id > sent.count - CHUNK);
            }
        });
    REQUIRE(
        cache_mailbox->sync_newest_state()->turbo_sync.folders[inbox_path].status ==
        mb::turbo_sync_status::finished);
}

TEST_CASE_METHOD(sync_newest_test, "turbo sync new messages")
{
    ext_mailbox =
        create_from_json(EXTERNAL_MAILBOX_FOR_TURBO_SYNC_PATH, external_mock_type::type_normal);
    loc_mailbox = create_from_json(LOCAL_MAILBOX_FOR_TURBO_SYNC_PATH, local_mock_type::type_normal);

    xeno::load_cache_op(ctx, loc_mailbox, [this](error ec, mb::cache_mailbox_ptr res) {
        REQUIRE(!ec);
        cache_mailbox = res;
    })();

    cache_mailbox->sync_newest_state()->turbo_sync.folders[inbox_path].status =
        mb::turbo_sync_status::finished;

    run([this]() {
        xeno::turbo_sync_settings settings;
        settings.folders[mb::folder::type_t::inbox].count = TURBO_SYNC_CHUNK;
        sync_newest(CHUNK, settings);
    });
    REQUIRE(!(*err));

    auto inbox = cache_mailbox->get_folder(inbox_path);
    loc_mailbox->get_messages_info_top(
        inbox.fid, 2 * TURBO_SYNC_CHUNK, [inbox](error ec, mb::message_vector_ptr messages) {
            REQUIRE(!ec);
            REQUIRE(messages->size() == TURBO_SYNC_CHUNK);
            for (auto& message : *messages)
            {
                REQUIRE(message.mid);
                REQUIRE(message.id <= inbox.count);
                REQUIRE(message.id > inbox.count - TURBO_SYNC_CHUNK);
            }
        });
}

TEST_CASE_METHOD(sync_newest_test, "turbo sync timeout")
{
    ext_mailbox =
        create_from_json(EXTERNAL_MAILBOX_FOR_TURBO_SYNC_PATH, external_mock_type::type_normal);
    loc_mailbox = create_from_json(LOCAL_MAILBOX_FOR_TURBO_SYNC_PATH, local_mock_type::type_normal);

    xeno::load_cache_op(ctx, loc_mailbox, [this](error ec, mb::cache_mailbox_ptr res) {
        REQUIRE(!ec);
        cache_mailbox = res;
    })();

    run([this]() {
        ext_mailbox->get_folder_vector([this](error ec, mb::folder_vector_ptr folder_vector) {
            REQUIRE(!ec);
            for (auto folder : *folder_vector)
            {
                cache_mailbox->update_folder_info_from_external(folder); // update uidnext
            }
        });
    });

    REQUIRE(
        cache_mailbox->sync_newest_state()->turbo_sync.folders[inbox_path].status ==
        mb::turbo_sync_status::required);

    run([this]() {
        xeno::turbo_sync_settings settings;
        settings.folders[mb::folder::type_t::inbox].count = TURBO_SYNC_CHUNK;
        settings.folders[mb::folder::type_t::inbox].timeout = xeno::time_traits::milliseconds(1);
        std::this_thread::sleep_for(xeno::time_traits::milliseconds(2));
        sync_newest(CHUNK, settings);
    });
    REQUIRE(!(*err));

    auto inbox = cache_mailbox->get_folder(inbox_path);
    loc_mailbox->get_messages_info_top(
        inbox.fid, 2 * TURBO_SYNC_CHUNK, [inbox](error ec, mb::message_vector_ptr messages) {
            REQUIRE(!ec);
            REQUIRE(messages->size() == CHUNK);
            for (auto& message : *messages)
            {
                REQUIRE(message.mid);
                REQUIRE(message.id <= inbox.count);
                REQUIRE(message.id > inbox.count - CHUNK);
            }
        });
}

TEST_CASE_METHOD(sync_newest_test, "turbo sync download message errors")
{
    ext_mailbox = create_from_json(
        EXTERNAL_MAILBOX_FOR_TURBO_SYNC_PATH, external_mock_type::type_err_download_body);
    loc_mailbox = create_from_json(LOCAL_MAILBOX_FOR_TURBO_SYNC_PATH, local_mock_type::type_normal);

    xeno::load_cache_op(ctx, loc_mailbox, [this](error ec, mb::cache_mailbox_ptr res) {
        REQUIRE(!ec);
        cache_mailbox = res;
    })();

    REQUIRE(
        cache_mailbox->sync_newest_state()->turbo_sync.folders[inbox_path].status ==
        mb::turbo_sync_status::required);

    run([this]() {
        xeno::turbo_sync_settings settings;
        settings.folders[mb::folder::type_t::inbox].count = TURBO_SYNC_CHUNK;
        sync_newest(CHUNK, settings);
    });
    REQUIRE(!(*err));

    auto inbox_fid = *cache_mailbox->get_fid_by_path(inbox_path);
    loc_mailbox->get_messages_info_top(
        inbox_fid, 2 * TURBO_SYNC_CHUNK, [](error ec, mb::message_vector_ptr messages) {
            REQUIRE(!ec);
            REQUIRE(messages->size() == TURBO_SYNC_CHUNK);
            for (auto& message : *messages)
            {
                REQUIRE(!message.mid);
                REQUIRE(message.errors_count);
            }
        });
    REQUIRE(
        cache_mailbox->sync_newest_state()->turbo_sync.folders[inbox_path].status ==
        mb::turbo_sync_status::finished);
}

TEST_CASE_METHOD(sync_newest_test, "turbo sync finish on empty folder")
{
    ext_mailbox =
        create_from_json(EXTERNAL_MAILBOX_FOR_TURBO_SYNC_PATH, external_mock_type::type_normal);
    loc_mailbox = create_from_json(LOCAL_MAILBOX_FOR_TURBO_SYNC_PATH, local_mock_type::type_normal);

    xeno::load_cache_op(ctx, loc_mailbox, [this](error ec, mb::cache_mailbox_ptr res) {
        REQUIRE(!ec);
        cache_mailbox = res;
    })();

    auto spam_path = mb::path_t("Spam", '|');
    auto spam = cache_mailbox->get_folder(spam_path);
    REQUIRE(
        cache_mailbox->sync_newest_state()->turbo_sync.folders[spam_path].status ==
        mb::turbo_sync_status::required);

    run([this]() {
        xeno::turbo_sync_settings settings;
        settings.folders[mb::folder::type_t::spam].count = TURBO_SYNC_CHUNK;
        sync_newest(CHUNK, settings);
    });
    REQUIRE(!(*err));
    REQUIRE(
        cache_mailbox->sync_newest_state()->turbo_sync.folders[spam_path].status ==
        mb::turbo_sync_status::finished);
}

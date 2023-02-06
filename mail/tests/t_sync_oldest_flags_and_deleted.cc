#include "common.h"
#include "mailbox_mocks.h"

#include <src/common/context.h>
#include <src/xeno/operations/environment.h>
#include <src/mailbox/data_types/cache_mailbox.h>
#include <src/xeno/operations/sync/sync_oldest_flags_and_deleted_op.h>
#include <src/xeno/operations/sync/sync_folders_op.h>
#include <src/xeno/load_cache_op.h>

#include <catch.hpp>

#include <algorithm>
#include <memory>

using io_service = boost::asio::io_service;
using io_service_ptr = std::shared_ptr<io_service>;
using code = xeno::code;

static const mb::num_t CHUNK = 5;
static const mb::num_t BIG_CHUNK = 30;

struct sync_oldest_flags_and_deleted_test
{
    sync_oldest_flags_and_deleted_test()
        : ext_mailbox{ create_from_json(EXTERNAL_MAILBOX_PATH, external_mock_type::type_normal) }
        , loc_mailbox{ create_from_json(LOCAL_MAILBOX_PATH, local_mock_type::type_normal) }
        , cache_mailbox{ std::make_shared<mb::cache_mailbox>() }
        , io_internal{ std::make_shared<io_service>() }
        , io(io_internal.get())
        , ctx{ boost::make_shared<xeno::context>() }
        , logger_internal{ std::make_shared<xeno::logger_t>() }
    {
    }

    void sync_oldest_flags_and_deleted(mb::num_t chunk)
    {
        auto env = make_env(io, ctx, *logger_internal.get(), stat, handler, *this);
        env.ext_mailbox = ext_mailbox;
        env.loc_mailbox = loc_mailbox;
        env.cache_mailbox = cache_mailbox;

        xeno::spawn<xeno::sync_oldest_flags_and_deleted_op>(std::move(env), chunk);
    }

    void sync_folders()
    {
        auto env = make_env(io, ctx, *logger_internal.get(), stat, handler, *this);
        env.ext_mailbox = ext_mailbox;
        env.loc_mailbox = loc_mailbox;
        env.cache_mailbox = cache_mailbox;

        xeno::spawn<xeno::sync_folders_op>(std::move(env));
    }

    void prepare_cache()
    {
        xeno::load_cache_op(ctx, loc_mailbox, [this](error ec, mb::cache_mailbox_ptr res) {
            REQUIRE(!ec);
            cache_mailbox = res;
            auto& folders = cache_mailbox->folders();
            for (auto& [folder_path, folder] : folders)
            {
                loc_mailbox->get_messages_info_top(
                    folder.fid,
                    CHUNK,
                    [this, path = folder_path](error ec, mb::message_vector_ptr messages) {
                        REQUIRE(!ec);
                        set_folder_top(cache_mailbox, path, messages);
                    });
            }
        })();
        ext_mailbox->get_folder_vector([this](error ec, mb::folder_vector_ptr folder_vector) {
            REQUIRE(!ec);
            cache_mailbox->update_folders_from_external(folder_vector);
        });

        auto folders = cache_mailbox->folders_copy();
        for (auto& folder : *folders)
        {
            if (folder.status != mb::folder::status_t::ok) continue;

            ext_mailbox->get_folder_info(folder.path, [this](error ec, mb::folder_ptr folder) {
                REQUIRE(!ec);
                cache_mailbox->update_folder_info_from_external(*folder);
            });
        }
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
    io_service_ptr io_internal;
    boost::asio::io_service* io;
    std::shared_ptr<error> err{ std::make_shared<error>() };
    xeno::context_ptr ctx;
    xeno::logger_ptr logger_internal;
    xeno::iteration_stat_ptr stat = std::make_shared<xeno::iteration_stat>();

    std::shared_ptr<interrupt_handler> handler = std::make_shared<interrupt_handler>();
};

TEST_CASE_METHOD(sync_oldest_flags_and_deleted_test, "sync oldest flags and deleted")
{
    auto path = mb::path_t{ "INBOX", '|' };

    io->post([this, &path]() {
        prepare_cache();
        auto folder = cache_mailbox->get_folder_by_path(path);
        REQUIRE(static_cast<bool>(folder));
        ext_mailbox->get_messages_info_by_num(
            path,
            folder->count,
            folder->count - CHUNK + 1,
            [this, &path](error ec, mb::message_vector_ptr messages) {
                REQUIRE(!ec);
                set_folder_top(cache_mailbox, path, messages);
            });
    });
    io->run();
    io->reset();

    io->post([this]() { sync_oldest_flags_and_deleted(CHUNK); });
    io->run();
    io->reset();

    {
        auto state = cache_mailbox->sync_oldest_flags_and_deleted_state();
        auto folder = cache_mailbox->get_folder_by_path(path);
        REQUIRE(static_cast<bool>(folder));
        REQUIRE(state->sync_positions.find(folder->path) != state->sync_positions.end());
        REQUIRE(state->sync_positions.find(folder->path)->second == 9);
    }

    io->post([this]() { sync_oldest_flags_and_deleted(BIG_CHUNK); });
    io->run();
    io->reset();
    REQUIRE(!(*err));

    {
        auto state = cache_mailbox->sync_oldest_flags_and_deleted_state();
        auto folder = cache_mailbox->get_folder_by_path(path);
        REQUIRE(static_cast<bool>(folder));
        REQUIRE(state->sync_positions.find(folder->path) != state->sync_positions.end());
        REQUIRE(state->sync_positions.find(folder->path)->second == 1);

        auto fid = *cache_mailbox->get_fid_by_path(path);
        loc_mailbox->get_messages_info_top(
            fid, BIG_CHUNK, [](error ec, mb::message_vector_ptr messages) {
                REQUIRE(!ec);
                REQUIRE(messages->size() == 2);
                for (auto& msg : *messages)
                {
                    REQUIRE(msg.id >= 13);
                    REQUIRE(msg.id <= 14);
                }
            });
    }
}

TEST_CASE_METHOD(
    sync_oldest_flags_and_deleted_test,
    "ensure skip folders with non ok status in sync oldest flags and deleted")
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

    io->post([this]() { sync_oldest_flags_and_deleted(CHUNK); });
    io->run();
    io->reset();

    REQUIRE(!(*err));
}

TEST_CASE_METHOD(
    sync_oldest_flags_and_deleted_test,
    "should start from top, when reached bottom of folder")
{
    ext_mailbox = create_from_json(LOCAL_MAILBOX_PATH, external_mock_type::type_normal);
    loc_mailbox = create_from_json(LOCAL_MAILBOX_PATH, local_mock_type::type_normal);

    auto inbox_path = mb::path_t("INBOX", '|');
    prepare_cache();
    auto state = cache_mailbox->sync_oldest_flags_and_deleted_state();
    auto folder = cache_mailbox->get_folder_by_path(inbox_path);
    REQUIRE(static_cast<bool>(folder));
    REQUIRE(state->sync_positions.find(inbox_path) == state->sync_positions.end());
    ext_mailbox->get_messages_info_by_num(
        inbox_path,
        folder->count,
        folder->count - CHUNK + 1,
        [this, &inbox_path](error ec, mb::message_vector_ptr messages) {
            REQUIRE(!ec);
            set_folder_top(cache_mailbox, inbox_path, messages);
        });

    auto msg_count = static_cast<mb::num_t>(loc_mailbox->get_folder_messages(folder->fid).size());
    io->post([this, msg_count]() { sync_oldest_flags_and_deleted(msg_count); });
    io->run();
    io->reset();

    state = cache_mailbox->sync_oldest_flags_and_deleted_state();
    folder = cache_mailbox->get_folder_by_path(inbox_path);
    REQUIRE(static_cast<bool>(folder));
    REQUIRE(state->sync_positions.find(folder->path) != state->sync_positions.end());
    REQUIRE(state->sync_positions.find(folder->path)->second == 1);

    io->post([this]() { sync_oldest_flags_and_deleted(CHUNK); });
    io->run();
    io->reset();

    state = cache_mailbox->sync_oldest_flags_and_deleted_state();
    folder = cache_mailbox->get_folder_by_path(inbox_path);
    REQUIRE(static_cast<bool>(folder));
    REQUIRE(state->sync_positions.find(folder->path) != state->sync_positions.end());
    REQUIRE(state->sync_positions.find(folder->path)->second == 0);
}

TEST_CASE_METHOD(sync_oldest_flags_and_deleted_test, "sync deleted")
{
    ext_mailbox = create_from_json(
        EXTERNAL_MAILBOX_FOR_SYNC_OLDEST_FLAGS_AND_DELETED_TESTS_PATH,
        external_mock_type::type_normal);
    loc_mailbox = create_from_json(
        LOCAL_MAILBOX_FOR_SYNC_OLDEST_FLAGS_AND_DELETED_TESTS_PATH, local_mock_type::type_normal);

    auto drafts_path = mb::path_t("DRAFTS", '|');
    prepare_cache();
    auto state = cache_mailbox->sync_oldest_flags_and_deleted_state();
    auto drafts_folder = cache_mailbox->get_folder_by_path(drafts_path);
    REQUIRE(static_cast<bool>(drafts_folder));
    REQUIRE(state->sync_positions.find(drafts_path) == state->sync_positions.end());

    loc_mailbox->get_messages_info_by_id(
        drafts_folder->fid,
        mb::imap_id_vector{ 7, 8 },
        mb::msg_info_type::with_flags,
        [](error /*ec*/, mb::message_vector_ptr msgs) { REQUIRE(msgs->size() == 2); });

    io->post([this]() { sync_oldest_flags_and_deleted(CHUNK); });
    io->run();
    io->reset();

    loc_mailbox->get_messages_info_by_id(
        drafts_folder->fid,
        mb::imap_id_vector{ 7, 8 },
        mb::msg_info_type::with_flags,
        [](error /*ec*/, mb::message_vector_ptr msgs) {
            REQUIRE(msgs->size() == 1);
            REQUIRE(msgs->begin()->id == 8);
        });
}

TEST_CASE_METHOD(sync_oldest_flags_and_deleted_test, "sync flags")
{
    ext_mailbox = create_from_json(
        EXTERNAL_MAILBOX_FOR_SYNC_OLDEST_FLAGS_AND_DELETED_TESTS_PATH,
        external_mock_type::type_normal);
    loc_mailbox = create_from_json(
        LOCAL_MAILBOX_FOR_SYNC_OLDEST_FLAGS_AND_DELETED_TESTS_PATH, local_mock_type::type_normal);

    auto inbox_path = mb::path_t("INBOX", '|');
    prepare_cache();
    auto state = cache_mailbox->sync_oldest_flags_and_deleted_state();
    auto inbox_folder = cache_mailbox->get_folder_by_path(inbox_path);
    REQUIRE(static_cast<bool>(inbox_folder));
    mb::imap_id_t BIGGEST_IMAP_ID_FOR_SYNC = 5;
    state->sync_positions[inbox_path] = BIGGEST_IMAP_ID_FOR_SYNC;

    loc_mailbox->get_messages_info_by_id(
        inbox_folder->fid,
        mb::imap_id_vector{ 2, 3, 5 },
        mb::msg_info_type::with_flags,
        [](error /*ec*/, mb::message_vector_ptr msgs) {
            auto first_msg = std::find_if(
                msgs->begin(), msgs->end(), [](mb::message msg) { return msg.num == 1; });
            REQUIRE(first_msg != msgs->end());
            REQUIRE(first_msg->flags.system_flags.size() == 1);

            auto second_msg = std::find_if(
                msgs->begin(), msgs->end(), [](mb::message msg) { return msg.num == 2; });
            REQUIRE(second_msg != msgs->end());
            REQUIRE(second_msg->flags.system_flags.empty());

            auto third_msg = std::find_if(
                msgs->begin(), msgs->end(), [](mb::message msg) { return msg.num == 3; });
            REQUIRE(third_msg != msgs->end());
            REQUIRE(third_msg->flags.system_flags.size() == 1);
            REQUIRE(
                third_msg->flags.system_flags.find(mb::system_flag_t::seen) !=
                third_msg->flags.system_flags.end());
        });

    io->post([this]() { sync_oldest_flags_and_deleted(CHUNK); });
    io->run();
    io->reset();

    loc_mailbox->get_messages_info_by_id(
        inbox_folder->fid,
        mb::imap_id_vector{ 2, 3, 5 },
        mb::msg_info_type::with_flags,
        [](error /*ec*/, mb::message_vector_ptr msgs) {
            auto first_msg = std::find_if(
                msgs->begin(), msgs->end(), [](mb::message msg) { return msg.num == 1; });
            REQUIRE(first_msg != msgs->end());
            REQUIRE(first_msg->flags.system_flags.empty());

            auto second_msg = std::find_if(
                msgs->begin(), msgs->end(), [](mb::message msg) { return msg.num == 2; });
            REQUIRE(second_msg != msgs->end());
            REQUIRE(second_msg->flags.system_flags.size() == 1);

            auto third_msg = std::find_if(
                msgs->begin(), msgs->end(), [](mb::message msg) { return msg.num == 3; });
            REQUIRE(third_msg != msgs->end());
            REQUIRE(third_msg->flags.system_flags.size() == 1);
            REQUIRE(
                third_msg->flags.system_flags.find(mb::system_flag_t::flagged) !=
                third_msg->flags.system_flags.end());
        });
}

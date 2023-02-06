#include "common.h"

#include "mailbox_mocks.h"

#include <src/common/context.h>
#include <common/json.h>

#include <xeno/operations/environment.h>
#include <xeno/operations/user/set_read_flag_op.h>
#include <xeno/operations/user/clear_folder_op.h>
#include <xeno/operations/user/create_folder_op.h>
#include <xeno/operations/user/delete_folder_op.h>
#include <xeno/operations/user/set_spam_status_op.h>
#include <xeno/operations/user/compose_and_save_draft_op.h>
#include <xeno/operations/user/update_folder_op.h>

#include <catch.hpp>
#include <boost/asio/io_service.hpp>

using namespace xeno::mailbox;

static const num_t CHUNK = 20;

struct user_operations_test
{

    user_operations_test()
    {
        reload_mailbox_data(local_mock_type::type_normal, external_mock_type::type_normal);
    }

    template <typename Operation, typename... Args>
    auto post_operation(Args&&... args)
    {
        auto env = make_env(io.get(), ctx, logger, stat, handler, *this);
        env.ext_mailbox = external_mailbox;
        env.loc_mailbox = local_mailbox;
        env.cache_mailbox = mailbox_cache;

        xeno::synchronization_settings sync_settings;
        sync_settings.user_op_chunk_size = 3;
        env.sync_settings =
            std::make_shared<const xeno::synchronization_settings>(std::move(sync_settings));

        Operation op(std::forward<Args>(args)...);
        io->post([env = std::move(env), op = std::move(op)]() mutable { op(std::move(env)); });
    }

    void run_io()
    {
        io->reset();
        io->run();
    }

    void operator()(error ec)
    {
        *(this->ec) = ec;
    }

    void operator()(error ec, const fid_t& new_fid)
    {
        *(this->ec) = ec;
        *(created_fid) = new_fid;
    }

    void operator()(error ec, const xeno::json::value& /*store_res*/)
    {
        *(this->ec) = ec;
    }

    void reload_mailbox_data(local_mock_type local_mock, external_mock_type external_mock)
    {
        local_mailbox = create_from_json(LOCAL_MAILBOX_PATH, local_mock);
        ;
        local_mailbox->get_folder_vector([this](error ec, folder_vector_ptr res) {
            REQUIRE(!ec);
            mailbox_cache->update_folders_from_local(res);
        });

        external_mailbox = create_from_json(LOCAL_MAILBOX_PATH, external_mock);
        external_mailbox->get_folder_vector([this](error ec, folder_vector_ptr res) {
            REQUIRE(!ec);
            mailbox_cache->update_folders_from_external(res);
        });
    }

    std::shared_ptr<boost::asio::io_service> io = std::make_shared<boost::asio::io_service>();
    std::shared_ptr<interrupt_handler> handler = std::make_shared<interrupt_handler>();
    xeno::context_ptr ctx = boost::make_shared<xeno::context>();
    yplatform::log::source logger;

    ext_mb::ext_mailbox_mock_ptr external_mailbox;
    loc_mb::loc_mailbox_mock_ptr local_mailbox;
    mb::cache_mailbox_ptr mailbox_cache = std::make_shared<mb::cache_mailbox>();
    xeno::iteration_stat_ptr stat = std::make_shared<xeno::iteration_stat>();

    std::shared_ptr<error> ec = std::make_shared<error>();
    std::shared_ptr<fid_t> created_fid = std::make_shared<fid_t>();
};

TEST_CASE_METHOD(user_operations_test, "set read flag")
{
    fid_t folder_fid = "2";
    path_t folder_path = { "INBOX", '|' };

    imap_id_vector ids_marked_read, ids_marked_unread;
    mid_vector mids_to_mark_read, mids_to_mark_unread;
    {
        auto& messages = local_mailbox->get_folder_messages(folder_fid);
        for (auto& msg : messages)
        {
            if (msg.flags.system_flags.count(system_flag_t::seen))
            {
                mids_to_mark_unread.push_back(msg.mid);
                ids_marked_unread.push_back(msg.id);
            }
            else
            {
                mids_to_mark_read.push_back(msg.mid);
                ids_marked_read.push_back(msg.id);
            }
        }
    }

    REQUIRE(ids_marked_read.size());
    REQUIRE(ids_marked_unread.size());
    REQUIRE(mids_to_mark_read.size());
    REQUIRE(mids_to_mark_unread.size());

    post_operation<xeno::user::set_read_flag_op>(
        mids_to_mark_read, xeno::mailbox::tid_vector(), true);
    run_io();

    REQUIRE(!(*ec));

    post_operation<xeno::user::set_read_flag_op>(
        mids_to_mark_unread, xeno::mailbox::tid_vector(), false);
    run_io();

    REQUIRE(!(*ec));

    auto read_messages = external_mailbox->get_specific_messages(folder_path, ids_marked_read);
    REQUIRE(read_messages.size() == ids_marked_read.size());
    for (auto& msg : read_messages)
    {
        REQUIRE(msg.flags.system_flags.count(system_flag_t::seen) == 1);
    }

    auto unread_messages = external_mailbox->get_specific_messages(folder_path, ids_marked_unread);
    REQUIRE(unread_messages.size() == ids_marked_unread.size());
    for (auto& msg : unread_messages)
    {
        REQUIRE(msg.flags.system_flags.count(system_flag_t::seen) == 0);
    }

    local_mailbox->get_messages_info_without_flags_by_mid(
        mids_to_mark_read, [&mids_to_mark_read](error err, message_vector_ptr messages) {
            REQUIRE(!err);
            REQUIRE(mids_to_mark_read.size() == messages->size());

            for (auto& msg : *messages)
            {
                REQUIRE(msg.flags.system_flags.count(system_flag_t::seen) == 1);
            }
        });

    local_mailbox->get_messages_info_without_flags_by_mid(
        mids_to_mark_unread, [&mids_to_mark_unread](error err, message_vector_ptr messages) {
            REQUIRE(!err);
            REQUIRE(mids_to_mark_unread.size() == messages->size());

            for (auto& msg : *messages)
            {
                REQUIRE(msg.flags.system_flags.count(system_flag_t::seen) == 0);
            }
        });
}

TEST_CASE_METHOD(user_operations_test, "clear folder")
{
    fid_t folder_fid = "2";
    path_t folder_path = { "INBOX", '|' };

    REQUIRE(local_mailbox->get_folder_messages(folder_fid).size());
    REQUIRE(external_mailbox->get_folder_messages(folder_path).size());

    post_operation<xeno::user::clear_folder_op>(folder_fid);
    run_io();
    REQUIRE(!(*ec));

    REQUIRE(local_mailbox->get_folder_messages(folder_fid).size() == 0);
    REQUIRE(external_mailbox->get_folder_messages(folder_path).size() == 0);
    auto folder = mailbox_cache->get_folder_by_fid(folder_fid);
    REQUIRE(static_cast<bool>(folder));
    REQUIRE(folder->count == 0);
    REQUIRE(get_folder_top(mailbox_cache, folder->path).empty());
}

TEST_CASE_METHOD(user_operations_test, "create folder")
{
    std::string folder_name = "IWannabeCreated";

    post_operation<xeno::user::create_folder_op>(folder_name, "", "");
    run_io();

    REQUIRE(!(*ec));
    REQUIRE(created_fid->size());

    external_mailbox->get_folder_vector([&folder_name](error err, folder_vector_ptr folders) {
        REQUIRE(!err);
        auto it =
            std::find_if(folders->begin(), folders->end(), [&folder_name](const folder& folder) {
                return folder.path.to_string() == folder_name;
            });

        REQUIRE(it != folders->end());
    });

    local_mailbox->get_folder_vector([&folder_name, this](error err, folder_vector_ptr folders) {
        REQUIRE(!err);
        auto it = std::find_if(folders->begin(), folders->end(), [this](const folder& folder) {
            return folder.fid == *created_fid;
        });

        REQUIRE(it != folders->end());
        REQUIRE(it->path.to_string() == folder_name);
    });

    {
        auto folder = mailbox_cache->get_folder_by_fid(*created_fid);
        REQUIRE(static_cast<bool>(folder));
        REQUIRE(folder->fid == *created_fid);
        REQUIRE(folder->path.to_string() == folder_name);
    }

    fid_t parent_fid = "2";
    std::string result_name = "INBOX|IWannabeCreated";
    *created_fid = "";

    post_operation<xeno::user::create_folder_op>(folder_name, parent_fid, "");
    run_io();

    REQUIRE(!(*ec));
    REQUIRE(created_fid->size());

    external_mailbox->get_folder_vector([&result_name](error err, folder_vector_ptr folders) {
        REQUIRE(!err);
        auto it =
            std::find_if(folders->begin(), folders->end(), [&result_name](const folder& folder) {
                return folder.path.to_string() == result_name;
            });

        REQUIRE(it != folders->end());
    });

    local_mailbox->get_folder_vector([this, &parent_fid, &result_name](
                                         error err, folder_vector_ptr folders) {
        REQUIRE(!err);
        auto it = std::find_if(folders->begin(), folders->end(), [this](const folder& folder) {
            return folder.fid == *created_fid;
        });

        REQUIRE(it != folders->end());
        REQUIRE(it->path.to_string() == result_name);

        it = std::find_if(folders->begin(), folders->end(), [&parent_fid](const folder& folder) {
            return folder.fid == parent_fid;
        });

        REQUIRE(it != folders->end());
        auto& children = it->childs;

        auto child_it =
            std::find_if(children.begin(), children.end(), [&result_name](const path_t& path) {
                return path.to_string() == result_name;
            });
        REQUIRE(child_it != children.end());
    });

    {
        auto folder = mailbox_cache->get_folder_by_fid(*created_fid);
        REQUIRE(static_cast<bool>(folder));
        REQUIRE(folder->fid == *created_fid);
        REQUIRE(folder->path.to_string() == result_name);
    }
}

TEST_CASE_METHOD(user_operations_test, "delete folder")
{
    fid_t folder_fid = "7";
    path_t folder_path = { "non_empty_folder_for_deletion", '|' };

    REQUIRE(local_mailbox->get_folder_messages(folder_fid).size());
    REQUIRE(external_mailbox->get_folder_messages(folder_path).size());

    post_operation<xeno::user::delete_folder_op>(folder_fid);
    run_io();
    REQUIRE(!(*ec));

    external_mailbox->get_folder_vector([&folder_path](error err, folder_vector_ptr folders) {
        REQUIRE(!err);
        auto it =
            std::find_if(folders->begin(), folders->end(), [&folder_path](const folder& folder) {
                return folder.path == folder_path;
            });

        REQUIRE(it == folders->end());
    });
    local_mailbox->get_folder_vector([&folder_path](error err, folder_vector_ptr folders) {
        REQUIRE(!err);
        auto it =
            std::find_if(folders->begin(), folders->end(), [&folder_path](const folder& folder) {
                return folder.path == folder_path;
            });

        REQUIRE(it == folders->end());
    });

    auto cur_folder = mailbox_cache->get_folder_by_fid(folder_fid);
    REQUIRE(!cur_folder);
}

TEST_CASE_METHOD(user_operations_test, "mark message as spam")
{
    fid_t folder_fid = "2";
    path_t folder_path = { "INBOX", '|' };

    fid_t spam_fid = "8";
    path_t spam_path = { "spam", '|' };

    mid_vector mids_to_mark_spam;
    {
        auto& messages = local_mailbox->get_folder_messages(folder_fid);
        for (auto& msg : messages)
        {
            if (!msg.flags.system_flags.count(system_flag_t::seen))
            {
                mids_to_mark_spam.push_back(msg.mid);
            }
        }
    }

    REQUIRE(mids_to_mark_spam.size());

    post_operation<xeno::user::set_spam_status_op>(
        mids_to_mark_spam, xeno::mailbox::tid_vector(), true);
    run_io();

    REQUIRE(!(*ec));

    auto spam_messages = external_mailbox->get_folder_messages(spam_path);
    REQUIRE(spam_messages.size() == mids_to_mark_spam.size());

    local_mailbox->get_messages_info_without_flags_by_mid(
        mids_to_mark_spam, [&mids_to_mark_spam, &spam_fid](error err, message_vector_ptr messages) {
            REQUIRE(!err);
            REQUIRE(mids_to_mark_spam.size() == messages->size());

            for (auto& msg : *messages)
            {
                REQUIRE(msg.fid == spam_fid);
            }
        });

    auto local_spam_messages = local_mailbox->get_folder_messages(spam_fid);
    REQUIRE(local_spam_messages.size() == mids_to_mark_spam.size());
    for (auto& message : local_spam_messages)
    {
        REQUIRE(
            std::find(mids_to_mark_spam.begin(), mids_to_mark_spam.end(), message.mid) !=
            mids_to_mark_spam.end());
    }

    post_operation<xeno::user::set_spam_status_op>(
        mids_to_mark_spam, xeno::mailbox::tid_vector(), false);
    run_io();

    REQUIRE(!(*ec));

    spam_messages = external_mailbox->get_folder_messages(spam_path);
    REQUIRE(spam_messages.empty());

    local_mailbox->get_messages_info_without_flags_by_mid(
        mids_to_mark_spam,
        [&mids_to_mark_spam, &folder_fid](error err, message_vector_ptr messages) {
            REQUIRE(!err);
            REQUIRE(mids_to_mark_spam.size() == messages->size());

            for (auto& msg : *messages)
            {
                REQUIRE(msg.fid == folder_fid);
            }
        });

    local_spam_messages = local_mailbox->get_folder_messages(spam_fid);
    REQUIRE(local_spam_messages.empty());
}

TEST_CASE_METHOD(user_operations_test, "corret store draft test")
{
    fid_t folder_fid = "1";
    path_t folder_path = { "DRAFTS", '|' };

    REQUIRE(local_mailbox->get_folder_messages(folder_fid).size() == 2);
    REQUIRE(external_mailbox->get_folder_messages(folder_path).size() == 2);

    auto store_request = std::make_shared<xeno::store_request>(
        std::make_shared<xeno::store_request::header_map_t>(),
        std::make_shared<xeno::store_request::param_map_t>(),
        std::make_shared<xeno::json::value>());

    post_operation<xeno::user::compose_and_save_draft_op>("user_ticket", store_request, "");
    run_io();

    REQUIRE(local_mailbox->get_folder_messages(folder_fid).size() == 3);
    REQUIRE(external_mailbox->get_folder_messages(folder_path).size() == 3);
}

TEST_CASE_METHOD(user_operations_test, "proper download error handling in store draft")
{
    fid_t folder_fid = "1";
    path_t folder_path = { "DRAFTS", '|' };

    reload_mailbox_data(local_mock_type::type_err_store, external_mock_type::type_normal);
    REQUIRE(local_mailbox->get_folder_messages(folder_fid).size() == 2);
    REQUIRE(external_mailbox->get_folder_messages(folder_path).size() == 2);

    auto store_request = std::make_shared<xeno::store_request>(
        std::make_shared<xeno::store_request::header_map_t>(),
        std::make_shared<xeno::store_request::param_map_t>(),
        std::make_shared<xeno::json::value>());

    post_operation<xeno::user::compose_and_save_draft_op>("user_ticket", store_request, "");
    run_io();

    REQUIRE(local_mailbox->get_folder_messages(folder_fid).size() == 2);
    REQUIRE(external_mailbox->get_folder_messages(folder_path).size() == 2);
}

TEST_CASE_METHOD(user_operations_test, "move messages")
{
    fid_t src_fid = "2";
    path_t src_path = { "INBOX", '|' };

    fid_t dst_fid = "3";
    path_t dst_path = { "test_main_local_folder", '|' };

    local_mailbox->get_messages_info_top(
        src_fid, CHUNK, [this, &src_path](error ec, message_vector_ptr messages) {
            REQUIRE(!ec);
            set_folder_top(mailbox_cache, src_path, messages);
        });
    set_folder_top(mailbox_cache, dst_path, std::make_shared<message_vector>());

    mid_vector mids_to_move;
    {
        auto& messages = local_mailbox->get_folder_messages(src_fid);
        for (auto& msg : messages)
        {
            if (msg.flags.system_flags.count(system_flag_t::seen))
            {
                mids_to_move.push_back(msg.mid);
            }
        }
    }

    REQUIRE(mids_to_move.size());

    auto src_external_count = external_mailbox->get_folder_messages(src_path).size();
    auto dst_external_count = external_mailbox->get_folder_messages(dst_path).size();
    auto src_count = get_folder_top(mailbox_cache, src_path).size();
    auto dst_count = get_folder_top(mailbox_cache, dst_path).size();

    post_operation<xeno::user::move_messages_op>(mids_to_move, tid_vector(), dst_fid, std::nullopt);
    run_io();

    REQUIRE(!(*ec));

    REQUIRE(
        external_mailbox->get_folder_messages(src_path).size() ==
        src_external_count - mids_to_move.size());
    REQUIRE(
        external_mailbox->get_folder_messages(dst_path).size() ==
        dst_external_count + mids_to_move.size());

    local_mailbox->get_messages_info_without_flags_by_mid(
        mids_to_move, [&mids_to_move, &dst_fid](error err, message_vector_ptr messages) {
            REQUIRE(!err);
            REQUIRE(mids_to_move.size() == messages->size());

            for (auto& msg : *messages)
            {
                REQUIRE(msg.fid == dst_fid);
            }
        });

    auto local_dst_messages = local_mailbox->get_folder_messages(dst_fid);
    REQUIRE(local_dst_messages.size() == mids_to_move.size());
    for (auto& message : local_dst_messages)
    {
        REQUIRE(
            std::find(mids_to_move.begin(), mids_to_move.end(), message.mid) != mids_to_move.end());
    }

    auto src_folder_top = get_folder_top(mailbox_cache, src_path);
    auto src_folder_fid = *mailbox_cache->get_fid_by_path(src_path);
    auto dst_folder_top = get_folder_top(mailbox_cache, dst_path);
    auto dst_folder_fid = *mailbox_cache->get_fid_by_path(dst_path);
    REQUIRE(src_folder_top.size() == src_count - mids_to_move.size());
    REQUIRE(dst_folder_top.size() == dst_count + mids_to_move.size());

    for (auto& id_msg_pair : dst_folder_top)
    {
        auto& id = id_msg_pair.first;
        auto& msg = id_msg_pair.second;
        REQUIRE(msg.id == id);
        REQUIRE(msg.fid == dst_folder_fid);
        REQUIRE(std::find(mids_to_move.begin(), mids_to_move.end(), msg.mid) != mids_to_move.end());
    }

    for (auto& id_msg_pair : src_folder_top)
    {
        auto& id = id_msg_pair.first;
        auto& msg = id_msg_pair.second;
        REQUIRE(msg.id == id);
        REQUIRE(msg.fid == src_folder_fid);
        REQUIRE(std::find(mids_to_move.begin(), mids_to_move.end(), msg.mid) == mids_to_move.end());
    }
}

TEST_CASE_METHOD(user_operations_test, "rename folder")
{
    fid_t folder_fid = "3";
    std::string old_folder_name = "test_main_local_folder";
    std::string new_folder_name = "new_folder_name";

    size_t ext_folders_count = external_mailbox->get_folders_count();
    size_t local_folders_count = local_mailbox->get_folders_count();
    size_t cache_folders_count = mailbox_cache->folders().size();

    post_operation<xeno::user::update_folder_op>(folder_fid, new_folder_name, fid_t_opt());
    run_io();

    REQUIRE(!(*ec));

    external_mailbox->get_folder_vector([ext_folders_count, &new_folder_name, &old_folder_name](
                                            error err, folder_vector_ptr folders) {
        REQUIRE(!err);
        REQUIRE(ext_folders_count == folders->size());

        REQUIRE(folder_name_exist(new_folder_name, folders));
        REQUIRE(!folder_name_exist(old_folder_name, folders));
    });

    local_mailbox->get_folder_vector([local_folders_count, &new_folder_name, &old_folder_name](
                                         error err, folder_vector_ptr folders) {
        REQUIRE(!err);
        REQUIRE(local_folders_count == folders->size());

        REQUIRE(folder_name_exist(new_folder_name, folders));
        REQUIRE(!folder_name_exist(old_folder_name, folders));
    });

    auto folders = mailbox_cache->folders_copy();
    REQUIRE(cache_folders_count == folders->size());

    REQUIRE(folder_name_exist(new_folder_name, folders));
    REQUIRE(!folder_name_exist(old_folder_name, folders));
    for (auto& folder : *folders)
    {
        if (folder.path.to_string() == new_folder_name)
        {
            for (auto& child : folder.childs)
            {
                REQUIRE(child.get_parent_path().to_string() == new_folder_name);
            }
        }
    }
}

TEST_CASE_METHOD(user_operations_test, "move folder to new parent")
{
    fid_t folder_fid = "4";
    std::string folder_name = "child_folder_one";
    std::string old_folder_full_name = "test_main_local_folder|child_folder_one";
    std::string old_child_full_name = "test_main_local_folder|child_folder_one|grandson_folder";

    fid_t new_parent_fid = "7";
    std::string new_folder_full_name = "non_empty_folder_for_deletion|child_folder_one";
    std::string new_child_full_name =
        "non_empty_folder_for_deletion|child_folder_one|grandson_folder";

    size_t ext_folders_count = external_mailbox->get_folders_count();
    size_t local_folders_count = local_mailbox->get_folders_count();
    size_t cache_folders_count = mailbox_cache->folders().size();

    post_operation<xeno::user::update_folder_op>(folder_fid, folder_name, new_parent_fid);
    run_io();

    REQUIRE(!(*ec));

    external_mailbox->get_folder_vector(
        [ext_folders_count,
         old_folder_full_name,
         old_child_full_name,
         new_folder_full_name,
         new_child_full_name](error err, folder_vector_ptr folders) {
            REQUIRE(!err);
            REQUIRE(ext_folders_count == folders->size());

            REQUIRE(folder_name_exist(new_child_full_name, folders));
            REQUIRE(folder_name_exist(new_folder_full_name, folders));
            REQUIRE(!folder_name_exist(old_folder_full_name, folders));
            REQUIRE(!folder_name_exist(old_child_full_name, folders));
        });

    local_mailbox->get_folder_vector([local_folders_count,
                                      old_folder_full_name,
                                      old_child_full_name,
                                      new_folder_full_name,
                                      new_child_full_name](error err, folder_vector_ptr folders) {
        REQUIRE(!err);
        REQUIRE(local_folders_count == folders->size());

        REQUIRE(folder_name_exist(new_child_full_name, folders));
        REQUIRE(folder_name_exist(new_folder_full_name, folders));
        REQUIRE(!folder_name_exist(old_folder_full_name, folders));
        REQUIRE(!folder_name_exist(old_child_full_name, folders));
    });

    auto folders = mailbox_cache->folders_copy();
    REQUIRE(cache_folders_count == folders->size());

    REQUIRE(folder_name_exist(new_child_full_name, folders));
    REQUIRE(folder_name_exist(new_folder_full_name, folders));
    REQUIRE(!folder_name_exist(old_folder_full_name, folders));
    REQUIRE(!folder_name_exist(old_child_full_name, folders));
    for (auto& folder : *folders)
    {
        if (folder.path.to_string() == new_folder_full_name)
        {
            REQUIRE(folder_has_child(new_child_full_name, folder));
        }
    }

    auto parent_folder = mailbox_cache->get_folder_by_fid(new_parent_fid);
    REQUIRE(folder_has_child(new_folder_full_name, *parent_folder));
}

TEST_CASE_METHOD(
    user_operations_test,
    "move root folder to new parent doesn't creates additional folders")
{
    fid_t folder_fid = "3";
    std::string folder_name = "test_main_local_folder";

    fid_t new_parent_fid = "7";

    size_t ext_folders_count = external_mailbox->get_folders_count();
    size_t local_folders_count = local_mailbox->get_folders_count();
    size_t cache_folders_count = mailbox_cache->folders().size();

    post_operation<xeno::user::update_folder_op>(folder_fid, folder_name, new_parent_fid);
    run_io();

    REQUIRE(!(*ec));
    REQUIRE(ext_folders_count == external_mailbox->get_folders_count());
    REQUIRE(local_folders_count == local_mailbox->get_folders_count());
    REQUIRE(cache_folders_count == mailbox_cache->folders().size());
}

TEST_CASE_METHOD(user_operations_test, "rename non-existent folder should produce error")
{
    fid_t folder_fid = "100500";
    std::string new_folder_name = "new_name_for_nothing";

    post_operation<xeno::user::update_folder_op>(folder_fid, new_folder_name, fid_t_opt());
    run_io();

    REQUIRE(*ec);
}

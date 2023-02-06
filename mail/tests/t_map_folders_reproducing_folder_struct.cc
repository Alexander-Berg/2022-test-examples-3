#include "catch.hpp"
#include "common.h"

#include <streamer/operations/get_or_create_folder_op.h>
#include <streamer/operations/map_folders_reproducing_folder_struct_op.h>

using namespace collectors;
using namespace collectors::streamer;

static const std::string SRC_MAILBOX_DATA{ "data/sync_folders_src.json" };
static const std::string DST_MAILBOX_DATA{ "data/sync_folders_dst.json" };

class map_folders_reproducing_folder_struct_op_test
{
public:
    map_folders_reproducing_folder_struct_op_test()
    {
        env = make_default_env(&io);
        meta = std::dynamic_pointer_cast<fake_meta>(env->meta);
        env->src_mailbox = src_mailbox = make_mailbox(SRC_MAILBOX_DATA);
        env->dst_mailbox = dst_mailbox = make_mailbox(DST_MAILBOX_DATA);
    }

    error run_map_folders_reproducing_folder_struct()
    {
        return run_op<operations::map_folders_reproducing_folder_struct_op>(env);
    }

    error run_get_or_create_folder(const folder& folder)
    {
        return run_op<operations::get_or_create_folder_op>(env, folder);
    }

    void check_correct_mapping()
    {
        boost::asio::dispatch(io, [this]() {
            auto src_folders = src_mailbox->folders_;
            auto dst_folders = dst_mailbox->folders_;
            for (auto folder : src_folders)
            {
                auto current_fid = folder.fid;
                do
                {
                    auto mapped_fid = env->state->folders_mapping.at(current_fid);
                    auto src_folder_it = find_folder_by_fid(src_folders, current_fid);
                    REQUIRE(src_folder_it != src_folders.end());
                    auto dst_folder_it = find_folder_by_fid(dst_folders, mapped_fid);
                    REQUIRE(dst_folder_it != dst_folders.end());
                    check_folders_same(*src_folder_it, *dst_folder_it);
                    current_fid = src_folder_it->parent_fid;
                } while (!is_empty_id(current_fid));
            }
        });
        io.reset();
        io.run();
    }

    folders::const_iterator find_folder_by_fid(const folders& all_folders, const fid& fid)
    {
        return std::find_if(all_folders.begin(), all_folders.end(), [fid](const auto& folder) {
            return folder.fid == fid;
        });
    }

    void check_folders_same(const folder& src, const folder& dst)
    {
        if (!is_empty_id(env->meta->root_folder_id()))
        {
            REQUIRE(dst.symbol.empty());
        }
        else
        {
            REQUIRE(src.symbol == dst.symbol);
        }

        REQUIRE(env->state->folders_mapping[src.parent_fid] == dst.parent_fid);
        if (dst.symbol.empty())
        {
            REQUIRE(src.name == dst.name);
        }
    }

    fake_mailbox_ptr src_mailbox;
    fake_mailbox_ptr dst_mailbox;
    fake_meta_ptr meta;
    environment_ptr env;
    boost::asio::io_context io;
};

TEST_CASE_METHOD(map_folders_reproducing_folder_struct_op_test, "sync_to_mailbox_root")
{
    auto ec = run_map_folders_reproducing_folder_struct();
    REQUIRE(!ec);
    check_correct_mapping();
    REQUIRE(env->state->cached_folders.size() == dst_mailbox->folders_.size());
    for (auto& folder : dst_mailbox->folders_)
    {
        REQUIRE(env->state->cached_folders.count(folder));
    }
}

TEST_CASE_METHOD(map_folders_reproducing_folder_struct_op_test, "sync_to_folder")
{
    const fid root_folder_id = "8";

    meta->set_root_folder(root_folder_id);
    auto ec = run_map_folders_reproducing_folder_struct();
    REQUIRE(!ec);
    check_correct_mapping();
}

TEST_CASE_METHOD(map_folders_reproducing_folder_struct_op_test, "already_synced_folders")
{
    // load same data
    env->dst_mailbox = dst_mailbox = make_mailbox(DST_MAILBOX_DATA);
    env->src_mailbox = src_mailbox = make_mailbox(DST_MAILBOX_DATA);
    auto src_folders = src_mailbox->folders_;
    auto dst_folders = dst_mailbox->folders_;

    auto ec = run_map_folders_reproducing_folder_struct();
    REQUIRE(!ec);
    // check nothing changed
    REQUIRE(src_folders == src_mailbox->folders_);
    REQUIRE(dst_folders == dst_mailbox->folders_);
    check_correct_mapping();
}

TEST_CASE_METHOD(map_folders_reproducing_folder_struct_op_test, "error_create_folder")
{
    env->dst_mailbox = dst_mailbox =
        make_mailbox(DST_MAILBOX_DATA, fake_mailbox_type::type_err_create_folder);

    auto ec = run_map_folders_reproducing_folder_struct();
    REQUIRE(ec == code::macs_error);
    REQUIRE(env->state->cached_folders.empty());
    REQUIRE(env->state->folders_mapping.empty());
}

TEST_CASE_METHOD(map_folders_reproducing_folder_struct_op_test, "no_folders_cache_updates")
{
    env->dst_mailbox = dst_mailbox =
        make_mailbox(DST_MAILBOX_DATA, fake_mailbox_type::type_err_get_folders);
    env->state->cached_folders.insert(dst_mailbox->folders_.begin(), dst_mailbox->folders_.end());

    for (auto&& folder : src_mailbox->folders_)
    {
        auto ec = run_get_or_create_folder(folder);
        REQUIRE(!ec);
    }
}

TEST_CASE_METHOD(map_folders_reproducing_folder_struct_op_test, "using_folders_cache")
{
    env->dst_mailbox = dst_mailbox =
        make_mailbox(DST_MAILBOX_DATA, fake_mailbox_type::type_err_create_folder);
    env->state->cached_folders.insert(src_mailbox->folders_.begin(), src_mailbox->folders_.end());

    for (auto&& folder : src_mailbox->folders_)
    {
        env->state->folders_mapping[folder.parent_fid] = folder.parent_fid;
        auto ec = run_get_or_create_folder(folder);
        REQUIRE(!ec);
    }
}

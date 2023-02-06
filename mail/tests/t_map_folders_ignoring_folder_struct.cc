#include "catch.hpp"
#include "common.h"

#include <streamer/operations/map_folders_ignoring_folder_struct_op.h>

using namespace collectors;
using namespace collectors::streamer;

static const std::string SRC_MAILBOX_DATA{ "data/sync_folders_src.json" };
static const std::string DST_MAILBOX_DATA{ "data/sync_folders_dst.json" };

class map_folders_ignoring_folder_struct_op_test
{
public:
    map_folders_ignoring_folder_struct_op_test()
    {
        env = make_default_env(&io);
        meta = std::dynamic_pointer_cast<fake_meta>(env->meta);
        meta->set_ignore_folders_struct(true);
        env->src_mailbox = src_mailbox = make_mailbox(SRC_MAILBOX_DATA);
        env->dst_mailbox = dst_mailbox = make_mailbox(DST_MAILBOX_DATA);
    }

    error run_map_folders_ignoring_folder_struct()
    {
        return run_op<operations::map_folders_ignoring_folder_struct_op>(env);
    }

    fake_mailbox_ptr src_mailbox;
    fake_mailbox_ptr dst_mailbox;
    fake_meta_ptr meta;
    environment_ptr env;
    boost::asio::io_context io;
};

TEST_CASE_METHOD(map_folders_ignoring_folder_struct_op_test, "ignore_folders_struct")
{
    auto ec = run_map_folders_ignoring_folder_struct();
    REQUIRE(!ec);

    for (auto& [from_fid, to_fid] : env->state->folders_mapping)
    {
        auto inbox_fid = "1";
        REQUIRE(to_fid == inbox_fid);
    }
}

TEST_CASE_METHOD(
    map_folders_ignoring_folder_struct_op_test,
    "ignore_folders_struct_with_root_folder")
{
    const fid root_folder_id = "8";
    meta->set_root_folder(root_folder_id);
    auto ec = run_map_folders_ignoring_folder_struct();
    REQUIRE(!ec);

    for (auto& [from_fid, to_fid] : env->state->folders_mapping)
    {
        REQUIRE(to_fid == root_folder_id);
    }
}

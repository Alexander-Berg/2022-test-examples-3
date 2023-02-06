#include "catch.hpp"
#include "common.h"

#include <streamer/operations/map_folders_reproducing_folder_struct_op.h>
#include <streamer/operations/sync_messages_op.h>

using namespace collectors;
using namespace collectors::streamer;

static const std::string SRC_MAILBOX_DATA{ "data/sync_messages_src.json" };
static const std::string DST_MAILBOX_DATA{ "data/sync_messages_dst.json" };

static std::size_t CACHE_SIZE = 10;
static std::size_t CHUNK_SIZE = 3;

class sync_messages_op_test
{
public:
    sync_messages_op_test()
    {
        env = make_env();
        meta = prepare_meta(env);
        run_map_folders();
    }

    error run_map_folders()
    {
        return run_op<operations::map_folders_reproducing_folder_struct_op>(env);
    }

    error run_sync_messages()
    {
        return run_op<operations::sync_messages_op>(env);
    }

    void check_messages()
    {
        auto& folders_mapping = env->state->folders_mapping;

        for (auto message : dst_mailbox->messages_)
        {
            auto src_mid = dst_mailbox->mids_mapping_.at(message.mid);
            auto src_message_it = find_message_by_mid(src_mailbox->messages_, src_mid);
            REQUIRE(src_message_it != src_mailbox->messages_.end());

            REQUIRE(message.fid == folders_mapping.at(src_message_it->fid));
            REQUIRE(message.stid == src_message_it->stid);
            REQUIRE(message.date == src_message_it->date);

            for (auto src_label : src_message_it->labels)
            {
                if (env->settings->is_label_syncable(src_label))
                {
                    auto dst_label_it = find_label(message.labels, src_label);
                    REQUIRE(dst_label_it != message.labels.end());
                }
            }
        }
    }

    void check_skipped_mids_size(size_t expected_size)
    {
        boost::asio::dispatch(io, [this, expected_size]() {
            REQUIRE(env->meta->skipped_mids().size() == expected_size);
        });
        io.reset();
        io.run();
    }

    messages::const_iterator find_message_by_mid(const messages& all_messages, const mid& mid)
    {
        return std::find_if(all_messages.begin(), all_messages.end(), [mid](const auto& message) {
            return message.mid == mid;
        });
    }

    labels::const_iterator find_label(const labels& all_labels, const label& label)
    {
        return std::find_if(all_labels.begin(), all_labels.end(), [label](const auto& s) {
            return std::tie(s.symbol, s.type, s.name, s.color) ==
                std::tie(label.symbol, label.type, label.name, label.color);
        });
    }

    environment_ptr make_env()
    {
        auto env = make_default_env(&io);
        env->settings->message_cache_size = CACHE_SIZE;
        env->settings->message_chunk_size = CHUNK_SIZE;
        env->settings->allowed_label_types = { "user" };
        env->settings->allowed_system_labels = { "seen_label", "flagged_label" };
        env->src_mailbox = src_mailbox = make_mailbox(SRC_MAILBOX_DATA);
        env->dst_mailbox = dst_mailbox = make_mailbox(DST_MAILBOX_DATA);
        return env;
    }

    fake_meta_ptr prepare_meta(environment_ptr env)
    {
        auto meta = std::dynamic_pointer_cast<fake_meta>(env->meta);
        meta->set_creation_ts(std::time(nullptr));
        return meta;
    }

    void prepare_skipped_folders()
    {
        for (auto& folder : src_mailbox->folders_)
        {
            folder.skip_messages = true;
            env->state->cached_src_folders[folder.fid] = folder;
        }
    }

    fake_mailbox_ptr src_mailbox;
    fake_mailbox_ptr dst_mailbox;
    fake_meta_ptr meta;
    environment_ptr env;
    boost::asio::io_context io;
};

TEST_CASE_METHOD(sync_messages_op_test, "simple_messages_case")
{
    auto ec = run_sync_messages();
    REQUIRE(!ec);
    check_messages();

    REQUIRE(dst_mailbox->messages_.size() == CHUNK_SIZE);
    REQUIRE(env->state->cached_messages.size() == CACHE_SIZE - CHUNK_SIZE);
    REQUIRE(dst_mailbox->messages_wihout_push_.size() == dst_mailbox->messages_.size());
}

TEST_CASE_METHOD(sync_messages_op_test, "next_iterations")
{
    int iterations_count = 3;
    for (int i = 0; i < iterations_count; ++i)
    {
        auto ec = run_sync_messages();
        REQUIRE(!ec);
        check_messages();
    }

    REQUIRE(dst_mailbox->messages_.size() == CHUNK_SIZE * iterations_count);
    REQUIRE(env->state->cached_messages.size() == CACHE_SIZE - CHUNK_SIZE * iterations_count);

    auto ec = run_sync_messages();
    REQUIRE(!ec);
    check_messages();
    REQUIRE(dst_mailbox->messages_.size() == CACHE_SIZE + 2);
    REQUIRE(env->state->cached_messages.size() == 0);

    REQUIRE(dst_mailbox->messages_wihout_push_.size() == dst_mailbox->messages_.size());
}

TEST_CASE_METHOD(sync_messages_op_test, "error_store_message")
{
    env->dst_mailbox = dst_mailbox =
        make_mailbox(DST_MAILBOX_DATA, fake_mailbox_type::type_err_store_message);
    env->settings->retries_limit = 1;
    check_skipped_mids_size(0);
    REQUIRE(env->state->last_message_retries == 0);
    mid last_mid;
    boost::asio::dispatch(io, [&last_mid, this]() { last_mid = env->meta->last_mid(); });
    io.reset();
    io.run();

    auto ec = run_sync_messages();
    REQUIRE(ec);
    check_skipped_mids_size(0);
    REQUIRE(env->state->last_message.mid == src_mailbox->messages_.front().mid);
    REQUIRE(env->state->last_message_retries == 1);
    REQUIRE(env->state->cached_messages.size() == 0);
    REQUIRE(env->state->last_mid == last_mid);
    REQUIRE(dst_mailbox->messages_.size() == 0);
    boost::asio::dispatch(io, [last_mid, this]() { REQUIRE(env->meta->last_mid() == last_mid); });
    io.reset();
    io.run();
}

TEST_CASE_METHOD(sync_messages_op_test, "error_store_message_with_retry")
{
    env->dst_mailbox = dst_mailbox =
        make_mailbox(DST_MAILBOX_DATA, fake_mailbox_type::type_err_store_message);
    env->settings->retries_limit = 1;
    mid last_mid;
    boost::asio::dispatch(io, [&last_mid, this]() { last_mid = env->meta->last_mid(); });
    io.reset();
    io.run();

    error ec;
    for (uint32_t i = 0; i < env->settings->retries_limit + 1; ++i)
    {
        ec = run_map_folders();
        REQUIRE(!ec);
        ec = run_sync_messages();
    }
    REQUIRE(!ec);

    check_skipped_mids_size(1);
    REQUIRE(env->state->last_message_retries == 1);
    REQUIRE(dst_mailbox->messages_.size() == 0);
    REQUIRE(env->state->cached_messages.size() == 0);
    REQUIRE(env->state->last_mid != last_mid);
    boost::asio::dispatch(
        io, [last_mid, this]() { REQUIRE(env->meta->last_mid() == env->state->last_mid); });
    io.reset();
    io.run();
}

TEST_CASE_METHOD(sync_messages_op_test, "error_on_skipping_mid")
{
    env->dst_mailbox = dst_mailbox =
        make_mailbox(DST_MAILBOX_DATA, fake_mailbox_type::type_err_store_message);
    env->meta = make_meta(&io, EMPTY_ID, EMPTY_ID, fake_meta_type::type_err_update_skipped_mids);
    meta = std::dynamic_pointer_cast<fake_meta_err_update_skipped_mids>(env->meta);

    auto ec = run_sync_messages();
    REQUIRE(ec);
    REQUIRE(env->state->last_message_retries == 1);
    check_skipped_mids_size(0);
    REQUIRE(dst_mailbox->messages_.size() == 0);
    REQUIRE(env->state->cached_messages.size() == CACHE_SIZE);
}

TEST_CASE_METHOD(sync_messages_op_test, "error_sync_labels")
{
    env->dst_mailbox = dst_mailbox =
        make_mailbox(DST_MAILBOX_DATA, fake_mailbox_type::type_err_create_label);
    mids syncing_msg_mids = { "1", "2" };

    auto ec = run_sync_messages();
    REQUIRE(!ec);
    check_messages();
    REQUIRE(env->state->last_message_retries == 0);
    check_skipped_mids_size(0);
    REQUIRE(dst_mailbox->messages_.size() == syncing_msg_mids.size());
    for (auto& msg : dst_mailbox->messages_)
    {
        auto it = std::find(syncing_msg_mids.begin(), syncing_msg_mids.end(), msg.mid);
        REQUIRE(it != syncing_msg_mids.end());
    }

    REQUIRE(env->state->cached_messages.size() == CACHE_SIZE - syncing_msg_mids.size());
}

TEST_CASE_METHOD(sync_messages_op_test, "skip_ignored_folders")
{
    prepare_skipped_folders();
    auto ec = run_sync_messages();
    REQUIRE(!ec);
    check_messages();

    REQUIRE(dst_mailbox->messages_.empty());
    REQUIRE(env->state->cached_messages.empty());
}

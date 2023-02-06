#include "catch.hpp"
#include "common.h"

#include <streamer/operations/sync_labels_op.h>

using namespace collectors;
using namespace collectors::streamer;

static const std::string SRC_MAILBOX_DATA{ "data/sync_labels_src.json" };
static const std::string DST_MAILBOX_DATA{ "data/sync_labels_dst.json" };

class sync_labels_op_test
{
public:
    sync_labels_op_test()
    {
        env = make_default_env(&io);
        env->settings->allowed_label_types = { "user" };
        env->settings->allowed_system_labels = { "seen_label", "flagged_label" };
        env->src_mailbox = src_mailbox = make_mailbox(SRC_MAILBOX_DATA);
        env->dst_mailbox = dst_mailbox = make_mailbox(DST_MAILBOX_DATA);
    }

    error run_sync_labels()
    {
        return run_op<operations::sync_labels_op>(env, src_mailbox->labels_);
    }

    void check_correct_mapping(std::set<lid> same_color_lids = { "11", "12" })
    {
        auto src_labels = src_mailbox->labels_;
        auto dst_labels = dst_mailbox->labels_;
        for (auto& label : src_labels)
        {
            if (env->settings->is_label_syncable(label))
            {
                auto mapped_lid = env->state->labels_mapping.at(label.lid);
                auto dst_label_it = find_label_by_lid(dst_labels, mapped_lid);
                REQUIRE(dst_label_it != dst_labels.end());
                check_labels_same(label, *dst_label_it, same_color_lids.count(label.lid));
            }
            else
            {
                REQUIRE(
                    env->state->labels_mapping.find(label.lid) == env->state->labels_mapping.end());
            }
        }
    }

    labels::const_iterator find_label_by_lid(const labels& all_labels, const lid& lid)
    {
        return std::find_if(all_labels.begin(), all_labels.end(), [lid](const auto& label) {
            return label.lid == lid;
        });
    }

    void check_labels_same(const label& src, const label& dst, bool same_color = false)
    {
        if (!is_empty_id(src.symbol))
        {
            REQUIRE(src.symbol == dst.symbol);
        }
        else
        {
            REQUIRE(src.name == dst.name);
            REQUIRE(src.type == dst.type);
            if (same_color)
            {
                REQUIRE(src.color == dst.color);
            }
            else
            {
                REQUIRE(src.color != dst.color);
            }
        }
    }

    fake_mailbox_ptr src_mailbox;
    fake_mailbox_ptr dst_mailbox;
    environment_ptr env;
    boost::asio::io_context io;
};

TEST_CASE_METHOD(sync_labels_op_test, "simple_labels_case")
{
    auto ec = run_sync_labels();
    REQUIRE(!ec);
    check_correct_mapping();
    REQUIRE(env->state->cached_labels.size() == dst_mailbox->labels_.size());
    for (auto& label : dst_mailbox->labels_)
    {
        REQUIRE(env->state->cached_labels.count(label));
    }
}

TEST_CASE_METHOD(sync_labels_op_test, "already_synced_labels")
{
    // load same data
    env->dst_mailbox = dst_mailbox = make_mailbox(DST_MAILBOX_DATA);
    env->src_mailbox = src_mailbox = make_mailbox(DST_MAILBOX_DATA);
    auto src_labels = src_mailbox->labels_;
    auto dst_labels = dst_mailbox->labels_;

    auto ec = run_sync_labels();
    REQUIRE(!ec);
    // check nothing changed
    REQUIRE(src_labels == src_mailbox->labels_);
    REQUIRE(dst_labels == dst_mailbox->labels_);
    std::set<lid> same_color_lids;
    for (auto& label : src_labels)
    {
        same_color_lids.insert(label.lid);
    }
    check_correct_mapping(same_color_lids);
}

TEST_CASE_METHOD(sync_labels_op_test, "error_create_label")
{
    env->dst_mailbox = dst_mailbox =
        make_mailbox(DST_MAILBOX_DATA, fake_mailbox_type::type_err_create_label);

    auto ec = run_sync_labels();
    REQUIRE(ec == code::macs_error);
    REQUIRE(env->state->cached_labels.empty());
    REQUIRE(env->state->labels_mapping.empty());
}

TEST_CASE_METHOD(sync_labels_op_test, "no_labels_cache_updates")
{
    env->dst_mailbox = dst_mailbox =
        make_mailbox(DST_MAILBOX_DATA, fake_mailbox_type::type_err_get_labels);
    env->state->cached_labels.insert(dst_mailbox->labels_.begin(), dst_mailbox->labels_.end());

    auto ec = run_sync_labels();
    REQUIRE(!ec);
    check_correct_mapping();
}

TEST_CASE_METHOD(sync_labels_op_test, "using_labels_cache")
{
    env->dst_mailbox = dst_mailbox =
        make_mailbox(DST_MAILBOX_DATA, fake_mailbox_type::type_err_create_folder);
    env->state->cached_labels.insert(src_mailbox->labels_.begin(), src_mailbox->labels_.end());

    auto ec = run_sync_labels();
    REQUIRE(!ec);
}

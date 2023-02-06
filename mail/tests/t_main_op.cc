#include "catch.hpp"
#include "common.h"

#include <streamer/operations/main_op.h>

using namespace collectors;
using namespace collectors::streamer;

static const std::string SRC_MESSAGES_MAILBOX_DATA{ "data/sync_messages_src.json" };
const std::size_t CACHE_SIZE = 10;
const std::size_t CHUNK_SIZE = 3;

class main_op_test
{
public:
    main_op_test()
    {
        env = make_default_env(&io);
        meta = std::dynamic_pointer_cast<fake_meta>(env->meta);
        env->passport = passport = make_passport();

        env->src_mailbox = src_mailbox = make_mailbox(SRC_MESSAGES_MAILBOX_DATA);
        env->dst_mailbox = dst_mailbox = make_mailbox();
        env->settings->message_cache_size = CACHE_SIZE;
        env->settings->message_chunk_size = CHUNK_SIZE;
        env->settings->allowed_label_types = { "user" };
        env->settings->allowed_system_labels = { "seen_label", "flagged_label" };

        meta->set_creation_ts(std::time(nullptr));
    }

    error run_main_op()
    {
        return run_op<operations::main_op>(env);
    }

    void check_auth_token(const std::string& expected_auth_token)
    {
        boost::asio::dispatch(io, [this, expected_auth_token]() {
            REQUIRE(meta->auth_token() == expected_auth_token);
        });
        io.reset();
        io.run();
    }

    void check_state(collector_state state)
    {
        boost::asio::dispatch(io, [this, state]() { REQUIRE(meta->state() == state); });
        io.reset();
        io.run();
    }

    void check_sync(bool was = true)
    {
        REQUIRE(was != env->state->folders_mapping.empty());
        REQUIRE(was != env->state->labels_mapping.empty());
        REQUIRE(was != dst_mailbox->messages_.empty());
        REQUIRE(was != env->state->cached_messages.empty());
        boost::asio::dispatch(io, [this]() { REQUIRE(env->meta->skipped_mids().empty()); });
        io.reset();
        io.run();
    }

    fake_mailbox_ptr src_mailbox;
    fake_mailbox_ptr dst_mailbox;
    fake_meta_ptr meta;
    fake_passport_ptr passport;
    environment_ptr env;
    boost::asio::io_context io;
};

TEST_CASE_METHOD(main_op_test, "disabled_state")
{
    meta->update_state(collector_state::disabled, [](const error& ec) { REQUIRE(!ec); });
    auto ec = run_main_op();
    REQUIRE(!ec);
    bool was = false;
    check_sync(was);
}

TEST_CASE_METHOD(main_op_test, "update_empty_dst_email")
{
    run_main_op();
    REQUIRE(env->state->dst_email == DST_UID + "@imap.yandex.ru");
}

TEST_CASE_METHOD(main_op_test, "error_update_empty_dst_email")
{
    env->passport = passport = make_passport(fake_passport_type::type_err_get_userinfo);
    auto ec = run_main_op();
    REQUIRE(ec == code::passport_error);
    REQUIRE(env->state->dst_email.empty());
}

TEST_CASE_METHOD(main_op_test, "invalid_auth_token")
{
    env->passport = passport = make_passport(fake_passport_type::type_err_invalid_auth_token);
    meta->edit("not_empty", {}, {}, [](const error& ec) { REQUIRE(!ec); });

    auto ec = run_main_op();
    REQUIRE(!ec);
    check_auth_token("");
    bool was = false;
    check_sync(was);
}

TEST_CASE_METHOD(main_op_test, "auth_token_for_wrong_uid")
{
    meta->edit(DST_UID, {}, {}, [](const error& ec) { REQUIRE(!ec); });

    auto ec = run_main_op();
    REQUIRE(ec == code::invalid_auth_token);
    check_auth_token(DST_UID);
    bool was = false;
    check_sync(was);
}

TEST_CASE_METHOD(main_op_test, "empty_auth_token")
{
    auto ec = run_main_op();
    REQUIRE(ec == code::no_auth_token);
    bool was = false;
    check_sync(was);
}

TEST_CASE_METHOD(main_op_test, "error_reset_invalid_auth_token")
{
    env->passport = passport = make_passport(fake_passport_type::type_err_invalid_auth_token);
    env->meta = make_meta(&io, EMPTY_ID, EMPTY_ID, fake_meta_type::type_err_reset_token);
    meta = std::dynamic_pointer_cast<fake_meta_err_reset_token>(env->meta);
    meta->edit("not_empty", {}, {}, [](const error& ec) { REQUIRE(!ec); });

    auto ec = run_main_op();
    REQUIRE(ec == code::macs_error);
    check_auth_token("not_empty");
    bool was = false;
    check_sync(was);
}

TEST_CASE_METHOD(main_op_test, "error_check_auth_token")
{
    env->passport = passport =
        make_passport(fake_passport_type::type_internal_err_check_auth_token);
    meta->edit("not_empty", {}, {}, [](const error& ec) { REQUIRE(!ec); });

    auto ec = run_main_op();
    REQUIRE(ec == code::passport_error);
    check_auth_token("not_empty");
    bool was = false;
    check_sync(was);
}

TEST_CASE_METHOD(main_op_test, "error_add_alias")
{
    env->passport = passport = make_passport(fake_passport_type::type_err_alias_operations);
    meta->edit(SRC_UID, {}, {}, [](const error& ec) { REQUIRE(!ec); });
    meta->update_state(collector_state::created, [](const error& ec) { REQUIRE(!ec); });

    auto ec = run_main_op();
    REQUIRE(!ec);
    REQUIRE(passport->aliases_.empty());
    check_state(collector_state::created);
    check_sync();
}

TEST_CASE_METHOD(main_op_test, "created_state_with_empty_auth_token")
{
    meta->update_state(collector_state::created, [](const error& ec) { REQUIRE(!ec); });

    auto ec = run_main_op();
    REQUIRE(ec == code::no_auth_token);
    REQUIRE(passport->aliases_.empty());
    check_state(collector_state::created);
    bool was = false;
    check_sync(was);
}

TEST_CASE_METHOD(main_op_test, "created_state_with_invalid_auth_token")
{
    env->passport = passport = make_passport(fake_passport_type::type_err_invalid_auth_token);
    meta->edit("not_empty", {}, {}, [](const error& ec) { REQUIRE(!ec); });
    meta->update_state(collector_state::created, [](const error& ec) { REQUIRE(!ec); });

    auto ec = run_main_op();
    REQUIRE(!ec);
    REQUIRE(passport->aliases_.empty());
    check_state(collector_state::created);
    bool was = false;
    check_sync(was);
}

TEST_CASE_METHOD(main_op_test, "created_state")
{
    meta->edit(SRC_UID, {}, {}, [](const error& ec) { REQUIRE(!ec); });
    meta->update_state(collector_state::created, [](const error& ec) { REQUIRE(!ec); });

    auto ec = run_main_op();
    REQUIRE(!ec);
    check_state(collector_state::ready);
    REQUIRE(passport->aliases_.size() == 1);
    REQUIRE(
        passport->aliases_[DST_UID].find(SRC_UID + "@imap.yandex.ru") !=
        passport->aliases_[DST_UID].end());
    check_sync();
}

TEST_CASE_METHOD(main_op_test, "error_remove_alias")
{
    env->passport = passport = make_passport(fake_passport_type::type_err_alias_operations);
    passport->set_aliases({ { DST_UID, { SRC_UID + "@imap.yandex.ru" } } });
    auto aliases = passport->aliases_;
    meta->edit("not_empty", {}, {}, [](const error& ec) { REQUIRE(!ec); });
    meta->update_state(collector_state::deleted, [](const error& ec) { REQUIRE(!ec); });

    auto ec = run_main_op();
    REQUIRE(ec == code::passport_error);
    REQUIRE(passport->aliases_ == aliases);
    bool was = false;
    check_sync(was);
}

TEST_CASE_METHOD(main_op_test, "error_delete_collector")
{
    passport->set_aliases({ { DST_UID, { SRC_UID + "@imap.yandex.ru" } } });
    env->meta = make_meta(&io, EMPTY_ID, EMPTY_ID, fake_meta_type::type_err_delete_collector);
    meta = std::dynamic_pointer_cast<fake_meta_err_delete_collector>(env->meta);
    meta->edit("not_empty", {}, {}, [](const error& ec) { REQUIRE(!ec); });
    meta->update_state(collector_state::deleted, [](const error& ec) { REQUIRE(!ec); });

    auto ec = run_main_op();
    REQUIRE(ec == code::macs_error);
    REQUIRE(passport->aliases_.empty());
    check_state(collector_state::deleted);
    bool was = false;
    check_sync(was);
}

TEST_CASE_METHOD(main_op_test, "deleted_state_with_empty_auth_token")
{
    passport->set_aliases({ { DST_UID, { SRC_UID + "@imap.yandex.ru" } } });
    meta->update_state(collector_state::deleted, [](const error& ec) { REQUIRE(!ec); });

    auto ec = run_main_op();
    REQUIRE(ec == code::deleted_collector);
    REQUIRE(passport->aliases_.empty());
    bool was = false;
    check_sync(was);
}

TEST_CASE_METHOD(main_op_test, "deleted_state_with_invalid_auth_token")
{
    env->passport = passport = make_passport(fake_passport_type::type_err_invalid_auth_token);
    passport->set_aliases({ { DST_UID, { SRC_UID + "@imap.yandex.ru" } } });
    meta->edit("not_empty", {}, {}, [](const error& ec) { REQUIRE(!ec); });
    meta->update_state(collector_state::deleted, [](const error& ec) { REQUIRE(!ec); });

    auto ec = run_main_op();
    REQUIRE(ec == code::deleted_collector);
    REQUIRE(passport->aliases_.empty());
    bool was = false;
    check_sync(was);
}

TEST_CASE_METHOD(main_op_test, "deleted_state")
{
    passport->set_aliases({ { DST_UID, { SRC_UID + "@imap.yandex.ru" } } });
    meta->edit("not_empty", {}, {}, [](const error& ec) { REQUIRE(!ec); });
    meta->update_state(collector_state::deleted, [](const error& ec) { REQUIRE(!ec); });

    auto ec = run_main_op();
    REQUIRE(ec == code::deleted_collector);
    REQUIRE(passport->aliases_.empty());
    bool was = false;
    check_sync(was);
}

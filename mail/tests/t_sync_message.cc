#include "common.h"
#include "mailbox_mocks.h"

#include <common/context.h>
#include <xeno/operations/sync/sync_message_op.h>

#include <catch.hpp>
#include <boost/asio/io_service.hpp>

using namespace xeno::mailbox;

struct sync_message_test : yplatform::log::contains_logger
{
    fid_t test_folder_fid = "2";
    path_t test_folder_path = { "INBOX", '|' };
    imap_id_t test_message_id = 110;
    std::string low_priority_provider = "outlook";

    sync_message_test()
    {
        reload_mailbox_data(local_mock_type::type_normal, external_mock_type::type_normal);

        xeno::synchronization_settings settings;
        settings.max_message_size = MAX_MESSAGE_SIZE;
        settings.low_priority_providers.insert(low_priority_provider);
        sync_settings = std::make_shared<const xeno::synchronization_settings>(settings);
    }

    template <typename Operation, typename... Args>
    auto run_operation(Args&&... args)
    {
        auto env = xeno::make_env<
            interrupt_handler,
            test_struct_wrapper<sync_message_test>,
            ext_mb::ext_mailbox_mock_ptr,
            loc_mb::loc_mailbox_mock_ptr>(
            &io, ctx, logger(), stat, handler, test_struct_wrapper<sync_message_test>(this));
        env.ext_mailbox = external_mailbox;
        env.loc_mailbox = local_mailbox;
        env.cache_mailbox = mailbox_cache;
        env.sync_settings = sync_settings;
        env.sync_phase = sync_phase;

        auto op = std::make_shared<Operation>(std::forward<Args>(args)...);
        io.post([env = std::move(env), op = std::move(op)]() mutable {
            yplatform::spawn(op, std::move(env));
        });

        io.reset();
        io.run();
    }

    void operator()(error ec)
    {
        this->ec = ec;
    }

    void operator()(error ec, const mb::message& msg)
    {
        this->ec = ec;
        this->mid = msg.mid;
    }

    void reload_mailbox_data(local_mock_type local_mock, external_mock_type external_mock)
    {
        local_mailbox = create_from_json(LOCAL_MAILBOX_PATH, local_mock);
        ;
        local_mailbox->get_folder_vector([this](error ec, folder_vector_ptr res) {
            REQUIRE(!ec);
            mailbox_cache->update_folders_from_local(res);
        });

        external_mailbox = create_from_json(EXTERNAL_MAILBOX_PATH, external_mock);
        external_mailbox->get_folder_vector([this](error ec, folder_vector_ptr res) {
            REQUIRE(!ec);
            mailbox_cache->update_folders_from_external(res);
        });
    }

    boost::asio::io_service io;
    std::shared_ptr<interrupt_handler> handler = std::make_shared<interrupt_handler>();
    xeno::context_ptr ctx = boost::make_shared<xeno::context>();

    ext_mb::ext_mailbox_mock_ptr external_mailbox;
    loc_mb::loc_mailbox_mock_ptr local_mailbox;
    mb::cache_mailbox_ptr mailbox_cache = std::make_shared<mb::cache_mailbox>();
    xeno::synchronization_settings_ptr sync_settings;
    xeno::sync_phase::sync_phase_t sync_phase = xeno::sync_phase::initial;
    xeno::iteration_stat_ptr stat = std::make_shared<xeno::iteration_stat>();

    error ec;
    mid_t mid;
};

TEST_CASE_METHOD(sync_message_test, "message stored correctly in local_mailbox")
{
    auto state = mailbox_cache->redownload_messages_state();
    state->has_new_failures = false;

    fid_t folder_fid = "2";
    path_t folder_path = { "INBOX", '|' };

    imap_id_t message_id = 110;

    REQUIRE(!message_exists(local_mailbox->get_folder_messages(folder_fid), message_id));
    REQUIRE(message_exists(external_mailbox->get_folder_messages(folder_path), message_id));

    run_operation<xeno::sync_message_op>(folder_path, message_id, "sync_message_test");
    REQUIRE(!ec);
    REQUIRE(!state->has_new_failures);

    auto local_messages = local_mailbox->get_folder_messages(folder_fid);
    auto local_it = std::find_if(
        local_messages.begin(), local_messages.end(), [message_id](const message& msg) {
            return msg.id == message_id;
        });
    REQUIRE(local_it != local_messages.end());
    REQUIRE(local_it->mid == mid);
}

TEST_CASE_METHOD(sync_message_test, "too big message is not stored")
{
    fid_t folder_fid = "2";
    path_t folder_path = { "INBOX", '|' };

    imap_id_t message_id = 111;

    REQUIRE(!message_exists(local_mailbox->get_folder_messages(folder_fid), message_id));
    REQUIRE(message_exists(external_mailbox->get_folder_messages(folder_path), message_id));

    run_operation<xeno::sync_message_op>(
        folder_path,
        message_id,
        "sync_message_test",
        message_opt(),
        notification_type::normal,
        false);
    REQUIRE(ec == code::message_too_big);
    REQUIRE(!message_exists(local_mailbox->get_folder_messages(folder_fid), message_id));
}

TEST_CASE_METHOD(sync_message_test, "too big message with wrong size is not stored")
{
    auto state = mailbox_cache->redownload_messages_state();
    state->has_new_failures = false;

    fid_t folder_fid = "2";
    path_t folder_path = { "INBOX", '|' };

    imap_id_t message_id = 112;

    REQUIRE(!message_exists(local_mailbox->get_folder_messages(folder_fid), message_id));
    REQUIRE(message_exists(external_mailbox->get_folder_messages(folder_path), message_id));

    run_operation<xeno::sync_message_op>(
        folder_path,
        message_id,
        "sync_message_test",
        message_opt(),
        notification_type::normal,
        false);
    REQUIRE(ec == code::message_too_big);
    REQUIRE(!state->has_new_failures);
    REQUIRE(!message_exists(local_mailbox->get_folder_messages(folder_fid), message_id));
}

TEST_CASE_METHOD(sync_message_test, "increment errors count when message too big")
{
    fid_t folder_fid = "2";
    path_t folder_path = { "INBOX", '|' };

    imap_id_t message_id = 111;

    REQUIRE(!message_exists(local_mailbox->get_folder_messages(folder_fid), message_id));
    REQUIRE(message_exists(external_mailbox->get_folder_messages(folder_path), message_id));

    run_operation<xeno::sync_message_op>(
        folder_path, message_id, "sync_message_test", message_opt(), notification_type::normal);
    REQUIRE(ec == code::message_saved_with_error);

    local_mailbox->get_messages_info_by_id(
        folder_fid,
        { message_id },
        msg_info_type::without_flags,
        [](error err, mb::message_vector_ptr msgs) {
            REQUIRE(!err);
            REQUIRE(msgs->size() == 1);
            REQUIRE(msgs->front().saved_errors_count == 1);
        });
}

TEST_CASE_METHOD(sync_message_test, "increment errors count when message with wrong size too big")
{
    auto state = mailbox_cache->redownload_messages_state();
    state->has_new_failures = false;

    fid_t folder_fid = "2";
    path_t folder_path = { "INBOX", '|' };

    imap_id_t message_id = 112;

    REQUIRE(!message_exists(local_mailbox->get_folder_messages(folder_fid), message_id));
    REQUIRE(message_exists(external_mailbox->get_folder_messages(folder_path), message_id));

    run_operation<xeno::sync_message_op>(
        folder_path, message_id, "sync_message_test", message_opt(), notification_type::normal);
    REQUIRE(ec == code::message_saved_with_error);
    REQUIRE(state->has_new_failures);

    local_mailbox->get_messages_info_by_id(
        folder_fid,
        { message_id },
        msg_info_type::without_flags,
        [](error err, mb::message_vector_ptr msgs) {
            REQUIRE(!err);
            REQUIRE(msgs->size() == 1);
            REQUIRE(msgs->front().saved_errors_count == 1);
        });
}

TEST_CASE_METHOD(
    sync_message_test,
    "update redownload messages state when fail to sync new message")
{
    reload_mailbox_data(local_mock_type::type_err_store, external_mock_type::type_normal);
    auto state = mailbox_cache->redownload_messages_state();
    state->has_new_failures = false;

    fid_t folder_fid = "2";
    path_t folder_path = { "INBOX", '|' };

    imap_id_t message_id = 13;

    auto msgs = local_mailbox->get_folder_messages(folder_fid);
    auto message = std::find_if(msgs.begin(), msgs.end(), [message_id](const mb::message& msg) {
        return msg.id == message_id;
    });
    REQUIRE(message != msgs.end());
    REQUIRE(message_exists(external_mailbox->get_folder_messages(folder_path), message_id));

    run_operation<xeno::sync_message_op>(
        folder_path,
        message_id,
        "sync_message_test",
        mb::message_opt{ *message },
        notification_type::normal);
    REQUIRE(ec);
    REQUIRE(state->has_new_failures);

    local_mailbox->get_messages_info_by_id(
        folder_fid,
        { message_id },
        msg_info_type::without_flags,
        [](error err, mb::message_vector_ptr msgs) {
            REQUIRE(!err);
            REQUIRE(msgs->size() == 1);
            REQUIRE(msgs->front().saved_errors_count == 1);
        });
}

TEST_CASE_METHOD(
    sync_message_test,
    "don't update redownload messages state when fail sync message with errors")
{
    reload_mailbox_data(local_mock_type::type_err_store, external_mock_type::type_normal);
    auto state = mailbox_cache->redownload_messages_state();
    state->has_new_failures = false;

    fid_t folder_fid = "2";
    path_t folder_path = { "INBOX", '|' };

    imap_id_t message_id = 13;

    auto msgs = local_mailbox->get_folder_messages(folder_fid);
    auto message = std::find_if(msgs.begin(), msgs.end(), [message_id](const mb::message& msg) {
        return msg.id == message_id;
    });
    REQUIRE(message != msgs.end());
    message->errors_count = 1;
    REQUIRE(message_exists(external_mailbox->get_folder_messages(folder_path), message_id));

    run_operation<xeno::sync_message_op>(
        folder_path,
        message_id,
        "sync_message_test",
        mb::message_opt{ *message },
        notification_type::normal);
    REQUIRE(ec);
    REQUIRE(!state->has_new_failures);

    local_mailbox->get_messages_info_by_id(
        folder_fid,
        { message_id },
        msg_info_type::without_flags,
        [](error err, mb::message_vector_ptr msgs) {
            REQUIRE(!err);
            REQUIRE(msgs->size() == 1);
            REQUIRE(msgs->front().saved_errors_count == 2);
        });
}

TEST_CASE_METHOD(sync_message_test, "store with high priority on sync_newest")
{
    sync_phase = xeno::sync_phase::sync_newest;
    run_operation<xeno::sync_message_op>(test_folder_path, test_message_id, "sync_message_test");
    REQUIRE(!ec);
    REQUIRE(local_mailbox->last_store_priority == "high");
}

TEST_CASE_METHOD(sync_message_test, "store with high priority on user_op")
{
    sync_phase = xeno::sync_phase::user_op;
    run_operation<xeno::sync_message_op>(test_folder_path, test_message_id, "sync_message_test");
    REQUIRE(!ec);
    REQUIRE(local_mailbox->last_store_priority == "high");
}

TEST_CASE_METHOD(sync_message_test, "store with low priority on sync_oldest")
{
    sync_phase = xeno::sync_phase::sync_oldest;
    run_operation<xeno::sync_message_op>(test_folder_path, test_message_id, "sync_message_test");
    REQUIRE(!ec);
    REQUIRE(local_mailbox->last_store_priority == "low");
}

TEST_CASE_METHOD(sync_message_test, "store with low priority for specific provider")
{
    sync_phase = xeno::sync_phase::sync_newest;
    external_mailbox->set_provider(low_priority_provider);
    run_operation<xeno::sync_message_op>(test_folder_path, test_message_id, "sync_message_test");
    REQUIRE(!ec);
    REQUIRE(local_mailbox->last_store_priority == "low");
}

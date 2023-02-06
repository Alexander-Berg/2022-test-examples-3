#pragma once

#include "mailbox_mocks.h"
#include <common/errors.h>
#include <src/xeno/operations/environment.h>
#include <src/mailbox/data_types/folder.h>
#include <src/mailbox/data_types/cache_mailbox.h>

using error = xeno::error;

struct interrupt_handler
{
    template <typename Environment, typename... Args>
    void handle_operation_interrupt(error error, Environment&& env, Args&&... args)
    {
        auto& handler = env.get_operation_handler();
        handler(error, std::forward<Args>(args)...);
    }

    void handle_operation_finish(xeno::iteration_stat_ptr /*stat*/, error /*error*/)
    {
    }
};

bool folder_name_exist(const std::string& name, xeno::mailbox::folder_vector_ptr folders);
bool folder_has_child(const std::string& name, const xeno::mailbox::folder& folder);

xeno::mailbox::imap_id_message_map get_folder_top(
    xeno::mailbox::cache_mailbox_ptr cache_mailbox,
    const xeno::mailbox::path_t& path);

void set_folder_top(
    xeno::mailbox::cache_mailbox_ptr cache_mailbox,
    const xeno::mailbox::path_t& path,
    const xeno::mailbox::message_vector_ptr& messages);

inline bool message_exists(
    const xeno::mailbox::message_vector& messages,
    xeno::mailbox::imap_id_t message_id)
{
    auto it = std::find_if(
        messages.begin(), messages.end(), [message_id](const xeno::mailbox::message& msg) {
            return msg.id == message_id;
        });
    return it != messages.end();
}

inline bool message_exists(
    const xeno::mailbox::message_body_pairs_vector& messages,
    xeno::mailbox::imap_id_t message_id)
{
    auto it = std::find_if(
        messages.begin(),
        messages.end(),
        [message_id](const xeno::mailbox::message_body_pair& msg_pair) {
            return msg_pair.first.id == message_id;
        });
    return it != messages.end();
}

template <typename TestStruct>
struct test_struct_wrapper
{
    test_struct_wrapper(TestStruct* test_struct) : test_struct(test_struct)
    {
    }

    template <typename... Args>
    void operator()(Args&&... args)
    {
        (*test_struct)(std::forward<Args>(args)...);
    }

    TestStruct* test_struct;
};

template <typename OperationHandler>
auto make_env(
    boost::asio::io_service* io,
    yplatform::task_context_ptr ctx,
    const yplatform::log::source& logger,
    xeno::iteration_stat_ptr stat,
    std::weak_ptr<interrupt_handler> handler,
    OperationHandler&& op_handler)
{
    return xeno::make_env<
        interrupt_handler,
        OperationHandler,
        ext_mb::ext_mailbox_mock_ptr,
        loc_mb::loc_mailbox_mock_ptr>(
        io, ctx, logger, stat, handler, std::forward<OperationHandler>(op_handler));
}

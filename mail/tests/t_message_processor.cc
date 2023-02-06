#include "fake_rate_controller.h"
#include "fake_gate.h"
#include "fake_sender.h"
#include "fake_db.h"
#include "fake_account_provider.h"
#include <message_processor/module.h>
#include <yplatform/app_service.h>
#include <catch.hpp>

namespace botserver::message_processor {

struct fake_command
{
    void operator()(context_ptr context)
    {
        context->gate->send_message(context, context->botpeer, "pong").get();
    }
};

struct test_traits
{
    using forward = fake_command;
    using ping = fake_command;
    using start = fake_command;
    using email = fake_command;
    using code = fake_command;
};

using test_module = module_impl<test_traits>;
using test_module_ptr = shared_ptr<test_module>;

struct t_message_processor
{
    boost::asio::io_service io;
    task_context_ptr ctx = boost::make_shared<task_context>();
    shared_ptr<messenger::fake_gate> gate = make_shared<messenger::fake_gate>();
    shared_ptr<fake_rate_controller> rc = make_shared<fake_rate_controller>();
    shared_ptr<mail::fake_sender> mail_sender = make_shared<mail::fake_sender>();
    shared_ptr<db::fake_db> db = make_shared<db::fake_db>();
    shared_ptr<auth::fake_account_provider> account_provider =
        make_shared<auth::fake_account_provider>();
    botpeer peer = { platform_name::telegram, "test_bot_id", "test_chat_id", "test_username" };
    settings st;
    test_module_ptr processor;

    t_message_processor()
    {
        st.gate_module = "gate";
        yplatform::register_module(io, st.gate_module, gate);
        yplatform::register_module(io, "rate_controller", rc);
        yplatform::register_module(io, "mail_sender", mail_sender);
        yplatform::register_module(io, "botdb", db);
        yplatform::register_module(io, "auth", account_provider);
        processor = make_message_processor();
    }

    test_module_ptr make_message_processor()
    {
        auto io_pool = std::make_shared<yplatform::io_pool>(io, 2);
        auto reactor = boost::make_shared<yplatform::reactor>(io_pool);
        auto processor = make_shared<test_module>(*reactor, st);
        processor->init();
        return processor;
    }

    auto make_message(string text)
    {
        auto message = make_shared<gate_message>();
        message->text = text;
        return message;
    }

    void dont_run_rate_controller_tasks()
    {
        rc->run_tasks = false;
    }
};

TEST_CASE_METHOD(t_message_processor, "dont_process_without_io_run")
{
    processor->on_message_received(ctx, peer, make_message("/ping"));
    REQUIRE(gate->sent_messages.size() == 0);
}

TEST_CASE_METHOD(t_message_processor, "dont_process_when_rate_controller_doesnt_allow")
{
    dont_run_rate_controller_tasks();
    processor->on_message_received(ctx, peer, make_message("/ping"));
    io.poll();
    REQUIRE(gate->sent_messages.size() == 0);
}

TEST_CASE_METHOD(t_message_processor, "process_command")
{
    auto res = processor->on_message_received(ctx, peer, make_message("/ping"));
    io.poll();
    REQUIRE(res.ready());
    REQUIRE_NOTHROW(res.get());
    REQUIRE(gate->sent_messages.size() == 1);
    REQUIRE(gate->sent_messages[0].first.platform == peer.platform);
    REQUIRE(gate->sent_messages[0].first.chat_id == peer.chat_id);
    REQUIRE(gate->sent_messages[0].first.bot_id == peer.bot_id);
    REQUIRE(gate->sent_messages[0].second == "pong");
}

}
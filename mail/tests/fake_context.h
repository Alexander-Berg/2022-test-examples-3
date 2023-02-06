#pragma once

#include <message_processor/context.h>
#include "fake_db.h"
#include "fake_gate.h"
#include "fake_sender.h"
#include "fake_account_provider.h"

namespace botserver::message_processor {

const string bot_name = "@testbot";
const string chat_id = "testchatid";
const string username = "testuser";
const struct botpeer fake_botpeer = { platform_name::telegram, bot_name, chat_id, username };
const struct mail_account fake_mail_account = { "test_uid", "yapoptest@yandex.ru" };

struct fake_context : context
{
    shared_ptr<messenger::fake_gate> fake_gate = make_shared<messenger::fake_gate>();
    shared_ptr<db::fake_db> fake_db = make_shared<db::fake_db>();
    shared_ptr<mail::fake_sender> fake_sender = make_shared<mail::fake_sender>();
    shared_ptr<auth::fake_account_provider> fake_account_provider =
        make_shared<auth::fake_account_provider>();
    struct mail_account mail_account = fake_mail_account;

    fake_context() : context(task_context())
    {
        message = make_shared<gate_message>();
        botpeer = fake_botpeer;
        links = fake_db;
        otp = fake_db;
        gate = fake_gate;
        mail_sender = fake_sender;
        account_provider = fake_account_provider;
        otp_limiter = make_shared<otp_rate_limiter>(
            optional<rate_limit_settings>{}, optional<rate_limit_settings>{});
    }
};

inline auto create_fake_context(command command)
{
    auto ctx = boost::make_shared<fake_context>();
    ctx->command = command;
    return ctx;
}

}
#include "fake_context.h"
#include <message_processor/email.h>
#include <catch.hpp>

namespace botserver::message_processor {

struct t_email
{
    boost::shared_ptr<fake_context> ctx = create_fake_context(command{ command_name::email, {} });
    mail_account test_account = ctx->mail_account;

    t_email()
    {
        add_mail_user(test_account);
    }

    void add_mail_user(mail_account user)
    {
        ctx->fake_account_provider->accounts[user.email] = user;
    }

    void run_command(string email_addr)
    {
        ctx->command.args["email"] = email_addr;
        email()(ctx);
    }

    auto sent_gate_messages()
    {
        return ctx->fake_gate->sent_messages;
    }

    auto sent_gate_message(size_t index)
    {
        if (index >= sent_gate_messages().size())
        {
            throw std::runtime_error("no such message at index " + std::to_string(index));
        }
        return sent_gate_messages()[index];
    }

    auto sent_mail_messages()
    {
        return ctx->fake_sender->sent_emails;
    }

    auto sent_mail_message(size_t index)
    {
        if (index >= sent_mail_messages().size())
        {
            throw std::runtime_error("no such message at index " + std::to_string(index));
        }
        return sent_mail_messages()[index];
    }

    auto generated_otp_codes()
    {
        return ctx->fake_db->codes_;
    }
};

TEST_CASE_METHOD(t_email, "respond_with_error_on_empty_email")
{
    run_command("");
    REQUIRE(sent_gate_message(0).second == (string)i18n::missing_email(i18n::language::ru));
}

TEST_CASE_METHOD(t_email, "no_codes_generated_on_empty_email")
{
    run_command("");
    REQUIRE(generated_otp_codes().count(ctx->botpeer) == 0);
}

TEST_CASE_METHOD(t_email, "respond_with_error_on_invalid_email")
{
    run_command("invalidemail");
    REQUIRE(sent_gate_message(0).second == i18n::invalid_email(i18n::language::ru));
}

TEST_CASE_METHOD(t_email, "gen_otp_code")
{
    run_command(test_account.email);
    REQUIRE(generated_otp_codes().count(ctx->botpeer) == 1);
    REQUIRE(generated_otp_codes()[ctx->botpeer].mail_account.email == test_account.email);
    REQUIRE(generated_otp_codes()[ctx->botpeer].mail_account.uid == test_account.uid);
}

TEST_CASE_METHOD(t_email, "send_mail_message")
{
    add_mail_user(test_account);
    run_command(test_account.email);
    REQUIRE(sent_mail_message(0)->to_email == test_account.email);
    REQUIRE(
        sent_mail_message(0)->text ==
        i18n::otp_code_is(
            i18n::language::ru, ctx->botpeer.username, generated_otp_codes()[ctx->botpeer].code));
}

TEST_CASE_METHOD(t_email, "reply_code_sent")
{
    add_mail_user(test_account);
    run_command(test_account.email);
    REQUIRE(
        sent_gate_message(0).second ==
        (string)i18n::send_code_from_email(i18n::language::ru, test_account.email));
}

TEST_CASE_METHOD(t_email, "reply_code_sent_on_non_existent_account")
{
    string email = "non_existent_email@yandex.ru";
    run_command(email);
    REQUIRE(
        sent_gate_message(0).second ==
        (string)i18n::send_code_from_email(i18n::language::ru, email));
}

}
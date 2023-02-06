#include "fake_context.h"
#include <message_processor/code.h>
#include <catch.hpp>

namespace botserver::message_processor {

struct t_code
{
    boost::shared_ptr<fake_context> ctx = create_fake_context(command{ command_name::code, {} });
    string otp_code = ctx->fake_db->gen_code(ctx, ctx->botpeer, ctx->mail_account);

    void run_command(string code_arg)
    {
        ctx->command.args["code"] = code_arg;
        code()(ctx);
    }

    auto sent_gate_messages()
    {
        return ctx->fake_gate->sent_messages;
    }

    bool has_link()
    {
        return ctx->fake_db->lookup(ctx, ctx->botpeer).has_value();
    }
};

TEST_CASE_METHOD(t_code, "wrong_code")
{
    run_command("wrong_code");
    REQUIRE(sent_gate_messages().size() == 1);
    REQUIRE(
        sent_gate_messages()[0].second == (string)i18n::invalid_code_number(i18n::language::ru));
    REQUIRE(!has_link());
}

TEST_CASE_METHOD(t_code, "create_link")
{
    run_command(otp_code);
    REQUIRE(has_link());
}

TEST_CASE_METHOD(t_code, "reply_account_binded")
{
    run_command(otp_code);
    REQUIRE(sent_gate_messages().size() == 1);
    REQUIRE(sent_gate_messages()[0].second == i18n::email_binded_successfully(i18n::language::ru));
}

}
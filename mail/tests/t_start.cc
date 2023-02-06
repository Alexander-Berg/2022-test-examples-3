#include "fake_context.h"
#include <message_processor/start.h>
#include <catch.hpp>

namespace botserver::message_processor {

struct t_start
{
    boost::shared_ptr<fake_context> ctx = create_fake_context(command{ command_name::start, {} });

    auto sent_messages()
    {
        return ctx->fake_gate->sent_messages;
    }

    void add_link()
    {
        ctx->fake_db->add(ctx, { ctx->botpeer, ctx->mail_account });
    }
};

TEST_CASE_METHOD(t_start, "invite_to_bind_email")
{
    start()(ctx);
    REQUIRE(sent_messages().size() == 2);
    REQUIRE(
        sent_messages()[0].second ==
        (string)i18n::forward_and_it_will_be_send_to_email(i18n::language::ru));
    REQUIRE(
        sent_messages()[1].second == (string)i18n::to_start_bind_email_address(i18n::language::ru));
}

TEST_CASE_METHOD(t_start, "invite_to_foward")
{
    add_link();
    start()(ctx);
    REQUIRE(sent_messages().size() == 1);
    REQUIRE(
        sent_messages()[0].second ==
        i18n::forward_and_it_will_be_send_to_email(i18n::language::ru));
}

}
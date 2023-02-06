#include "fake_context.h"
#include <message_processor/forward.h>
#include <catch.hpp>

namespace botserver::message_processor {

using namespace Catch::Matchers;

const time_t MSG_RECEIVED_TS = 1339471282;

struct t_forward
{
    t_forward()
    {
        context = create_fake_context({ command_name::forward, {} });
        context->fake_gate->file_to_download = "Au-au";
        context->links->add({}, { fake_botpeer, fake_mail_account });
    }

    gate_message_ptr make_fake_gate_message(
        message_author forwarded_from,
        vector<attachment_meta> attachments = {})
    {
        auto msg = make_shared<gate_message>();
        msg->text = "Привет!";
        msg->received_date = MSG_RECEIVED_TS;
        msg->attachments = attachments;
        msg->forwarded_from = forwarded_from;
        return msg;
    }

    gate_message_ptr make_fake_gate_message(vector<attachment_meta> attachments)
    {
        auto msg = make_shared<gate_message>();
        msg->text = "Привет!";
        msg->received_date = 0;
        msg->attachments = attachments;
        return msg;
    }

    auto sent_messages()
    {
        return context->fake_gate->sent_messages;
    }

    auto downloaded_files()
    {
        return context->fake_gate->downloaded_files;
    }

    auto sent_emails()
    {
        return context->fake_sender->sent_emails;
    }

    boost::shared_ptr<fake_context> context;
};

TEST_CASE_METHOD(t_forward, "no_link")
{
    context->message = make_fake_gate_message(vector<attachment_meta>{});
    context->botpeer.chat_id = "not_linked";
    forward()(context);
    REQUIRE(sent_messages().size() == 1);
    REQUIRE(sent_messages()[0].second == (string)i18n::bind_email_address(i18n::language::ru));
    REQUIRE(downloaded_files().size() == 0);
    REQUIRE(sent_emails().size() == 0);
}

TEST_CASE_METHOD(t_forward, "sent_message")
{
    context->message = make_fake_gate_message(vector<attachment_meta>{});
    forward()(context);
    REQUIRE(sent_emails().size() == 1);
    REQUIRE(
        sent_emails()[0]->subject ==
        i18n::forwarded_from_messenger(i18n::language::ru, platform_name::telegram));
    REQUIRE(sent_emails()[0]->to_email == fake_mail_account.email);
    REQUIRE(sent_emails()[0]->text == "Привет!");
    REQUIRE(sent_emails()[0]->attachments.size() == 0);
}

TEST_CASE_METHOD(t_forward, "forwarded_message")
{
    context->message = make_fake_gate_message({ "someone@yandex-team.ru" });
    forward()(context);
    REQUIRE(sent_emails().size() == 1);
    REQUIRE(
        sent_emails()[0]->subject ==
        i18n::forwarded_from_user_on_messenger(
            i18n::language::ru, { "someone@yandex-team.ru" }, platform_name::telegram));
    REQUIRE(sent_emails()[0]->to_email == fake_mail_account.email);
    REQUIRE(
        sent_emails()[0]->text ==
        i18n::received_from_user(
            i18n::language::ru, "Привет!", { "someone@yandex-team.ru" }, MSG_RECEIVED_TS));
}

TEST_CASE_METHOD(t_forward, "message_with_attachment")
{
    context->message = make_fake_gate_message(vector<attachment_meta>{ {
        .id = "iddqd",
        .file_name = "ty_chego_molchish.ogg",
        .mime_type = "audio/ogg",
        .size = 12345,
    } });
    forward()(context);
    REQUIRE(sent_emails().size() == 1);
    REQUIRE(sent_emails()[0]->attachments.size() == 1);
    REQUIRE(sent_emails()[0]->attachments[0].name == "ty_chego_molchish.ogg");
    REQUIRE(sent_emails()[0]->attachments[0].mime_type == "audio/ogg");
    REQUIRE(*sent_emails()[0]->attachments[0].content == "Au-au");
}

TEST_CASE_METHOD(t_forward, "message_too_big")
{
    context->settings.max_message_size = 100;
    context->message = make_fake_gate_message(
        vector<attachment_meta>{ { .size = context->settings.max_message_size + 1 } });
    forward()(context);
    REQUIRE(sent_emails().size() == 0);
    REQUIRE_THAT(
        sent_messages(),
        VectorContains(pair{ context->botpeer, i18n::message_too_big(i18n::language::ru) }));
}

}

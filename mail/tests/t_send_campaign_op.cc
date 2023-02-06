#include "fakes/send_tasks.h"
#include "fakes/delivery.h"
#include "callback.h"
#include <send/send_campaign_op.h>
#include <common/errors.h>
#include <boost/algorithm/string.hpp>
#include <boost/format.hpp>
#include <catch.hpp>

namespace fan::send {

using boost::format;
using boost::algorithm::contains;

struct t_send_campaign_op
{
    using send_op_type = send_campaign_op<fake_send_tasks, fake_delivery>;

    string EML_TEMPLATE = "From: Alice <alice@example.com>\r\n"
                          "To: %recipient%\r\n"
                          "Subject: Testing 123\r\n"
                          "Date: 1 Jan 2000 12:00:00\r\n"
                          "Content-Type: text/plain; charset=UTF-8;\r\n"
                          "\r\n"
                          "Hello World!\r\n";
    recipient_data RECIPIENT = { "yapoptest@yandex.ru", {} };
    recipient_data ANOTHER_RECIPIENT = { "yapoptest02@yandex.ru", {} };
    recipient_data INVALID_RECIPIENT = { "yapoptest03#yandex", {} };

    task_context_ptr ctx = boost::make_shared<task_context>();
    campaign campaign;
    shared_ptr<fake_send_tasks> tasks = make_shared<fake_send_tasks>();
    shared_ptr<fake_delivery> delivery = make_shared<fake_delivery>();
    callback<error_code> cb;
    shared_ptr<send_op_type> op;

    t_send_campaign_op()
    {
        setup_campaign();
        op = std::make_shared<send_op_type>(ctx, campaign, tasks, delivery, cb);
    }

    void setup_campaign()
    {
        tasks->eml_template = EML_TEMPLATE;
        add_recipient(RECIPIENT);
        add_recipient(ANOTHER_RECIPIENT);
    }

    void clear_recipients()
    {
        tasks->recipients.clear();
    }

    void unsubscribe(recipient_data recipient)
    {
        tasks->unsubscribe_list.emplace_back(recipient.email);
    }

    void add_recipient(recipient_data recipient)
    {
        tasks->recipients.emplace_back(recipient);
        tasks->template_params[recipient.email] =
            map<string, string>{ { "recipient", recipient.email } };
    }

    void set_error_on_mark_sent()
    {
        tasks->mark_sent_err = boost::asio::error::operation_aborted;
    }

    void set_error_on_get_eml_template()
    {
        tasks->get_eml_template_err = boost::asio::error::operation_aborted;
    }

    void set_error_on_delivery(recipient_data recipient)
    {
        delivery->recipient_errors[recipient.email] = boost::asio::error::operation_aborted;
    }

    error_code err()
    {
        return std::get<0>(cb.args());
    }

    auto&& sent_recipients()
    {
        return delivery->sent_recipients;
    }

    auto&& sent_emls()
    {
        return delivery->sent_emls;
    }

    string campaign_state()
    {
        return tasks->state;
    }

    void run()
    {
        yplatform::spawn(op);
    }
};

TEST_CASE_METHOD(t_send_campaign_op, "send_campaign_op/mark_sent")
{
    run();
    REQUIRE(!err());
    REQUIRE(campaign_state() == "sent");
}

TEST_CASE_METHOD(t_send_campaign_op, "send_campaign_op/send_by_smtp")
{
    run();
    REQUIRE(sent_recipients().count(RECIPIENT.email));
    REQUIRE(sent_recipients().count(ANOTHER_RECIPIENT.email));
}

TEST_CASE_METHOD(t_send_campaign_op, "send_campaign_op/replace_params")
{
    clear_recipients();
    add_recipient(RECIPIENT);
    run();
    REQUIRE(sent_emls().size() == 1);
    REQUIRE(contains(sent_emls()[0], (format("To: %1%\r\n") % RECIPIENT.email).str()));
}

TEST_CASE_METHOD(t_send_campaign_op, "send_campaign_op/dont_send_to_invalid_email")
{
    add_recipient(INVALID_RECIPIENT);
    run();
    REQUIRE(sent_recipients().count(INVALID_RECIPIENT.email) == 0);
}

TEST_CASE_METHOD(t_send_campaign_op, "send_campaign_op/send_only_once_to_duplicated_recipient")
{
    add_recipient(RECIPIENT);
    add_recipient(RECIPIENT);
    add_recipient(RECIPIENT);
    run();
    REQUIRE(sent_recipients().count(RECIPIENT.email) == 1);
}

TEST_CASE_METHOD(t_send_campaign_op, "send_campaign_op/dont_send_to_unsubscribed")
{
    unsubscribe(RECIPIENT);
    run();
    REQUIRE(sent_recipients().count(RECIPIENT.email) == 0);
}

TEST_CASE_METHOD(t_send_campaign_op, "send_campaign_op/dont_send_campaign_op_on_mark_sent_error")
{
    set_error_on_mark_sent();
    run();
    REQUIRE(err());
    REQUIRE(sent_recipients().size() == 0);
}

TEST_CASE_METHOD(t_send_campaign_op, "send_campaign_op/dont_send_campaign_op_on_render_error")
{
    set_error_on_get_eml_template();
    run();
    REQUIRE(err());
    REQUIRE(sent_recipients().size() == 0);
}

TEST_CASE_METHOD(t_send_campaign_op, "send_campaign_op/ignore_send_errors")
{
    set_error_on_delivery(RECIPIENT);
    run();
    REQUIRE(!err());
    REQUIRE(sent_recipients().count(ANOTHER_RECIPIENT.email));
}

}

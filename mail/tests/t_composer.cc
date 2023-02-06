#include <mail/composer.h>
#include <catch.hpp>

namespace botserver::mail {

struct t_composer
{
    string sender_email = "yapoptest@yandex.ru";
    string receiver_email = "yapoptest02@yandex.ru";
    composer composer;

    t_composer()
    {
    }

    mail_attachment make_attachment(string name, string type, string content)
    {
        mail_attachment attachment;
        attachment.name = name;
        attachment.mime_type = type;
        attachment.content = make_shared<string>(content);
        return attachment;
    }

    mail_message_ptr make_message(string text, vector<mail_attachment> attachments = {})
    {
        auto message = make_shared<mail_message>();
        message->text = text;
        message->to_email = receiver_email;
        message->attachments = attachments;
        return message;
    }
};

TEST_CASE_METHOD(t_composer, "text_message")
{
    string text = "Hello from bot";
    string base64_encoded_text = "SGVsbG8gZnJvbSBib3Q=";
    string subject = "Forwarded";
    string base64_encoded_subject = "Rm9yd2FyZGVk";
    auto message = make_message(text);
    message->subject = subject;
    auto eml = composer.compose_text_message(message, sender_email);
    REQUIRE(eml);
    REQUIRE(Catch::contains(*eml, "From: " + sender_email + "\r\n"));
    REQUIRE(Catch::contains(*eml, "To: " + receiver_email + "\r\n"));
    REQUIRE(Catch::contains(*eml, "Subject: =?UTF-8?B?" + base64_encoded_subject + "?=\r\n"));
    REQUIRE(Catch::contains(*eml, "Content-Type: text/plain; charset=utf-8\r\n"));
    REQUIRE(Catch::endsWith(*eml, "\r\n\r\n" + base64_encoded_text));
}

TEST_CASE_METHOD(t_composer, "attachment")
{
    string content = "12345";
    string base64_encoded_content = "MTIzNDU=";
    string subject = "Forwarded";
    string base64_encoded_subject = "Rm9yd2FyZGVk";
    auto message = make_message("", { make_attachment("test_attachment", "image/jpeg", content) });
    message->subject = subject;
    auto eml = composer.compose_message_with_attachments(message, sender_email);
    REQUIRE(eml);
    REQUIRE(Catch::contains(*eml, "From: " + sender_email + "\r\n"));
    REQUIRE(Catch::contains(*eml, "To: " + receiver_email + "\r\n"));
    REQUIRE(Catch::contains(*eml, "Subject: =?UTF-8?B?" + base64_encoded_subject + "?=\r\n"));
    REQUIRE(Catch::contains(*eml, "Content-Type: multipart/mixed; boundary="));
    REQUIRE(Catch::contains(*eml, "Content-Type: image/jpeg"));
    REQUIRE(Catch::contains(*eml, "Content-Disposition: attachment; filename=test_attachment\r\n"));
    REQUIRE(Catch::contains(*eml, "\r\n\r\n" + base64_encoded_content));
}

TEST_CASE_METHOD(t_composer, "attachment_without_name")
{
    auto message = make_message("", { make_attachment("", "image/jpeg", "12345") });
    auto eml = composer.compose_message_with_attachments(message, sender_email);
    REQUIRE(eml);
    REQUIRE(Catch::contains(*eml, "Content-Disposition: attachment\r\n"));
}

TEST_CASE_METHOD(t_composer, "two_attachments")
{
    string first_content = "12345";
    string first_base64_encoded_content = "MTIzNDU=";
    string second_content = "54321";
    string second_base64_encoded_content = "NTQzMjE=";
    auto message = make_message(
        "",
        { make_attachment("", "image/jpeg", first_content),
          make_attachment("", "image/jpeg", second_content) });
    auto eml = composer.compose_message_with_attachments(message, sender_email);
    REQUIRE(eml);
    REQUIRE(Catch::contains(*eml, "From: " + sender_email + "\r\n"));
    REQUIRE(Catch::contains(*eml, "To: " + receiver_email + "\r\n"));
    REQUIRE(Catch::contains(*eml, "Content-Type: multipart/mixed; boundary="));
    REQUIRE(Catch::contains(*eml, "\r\n\r\n" + first_base64_encoded_content));
    REQUIRE(Catch::contains(*eml, "\r\n\r\n" + second_base64_encoded_content));
}

}
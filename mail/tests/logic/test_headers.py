# -*- coding: utf-8 -*-
from __future__ import unicode_literals

import pytest

from base64 import b64encode
from hamcrest import assert_that, equal_to, contains_string, has_length, \
    has_entries, matches_regexp, has_key, not_

from mail.nwsmtp.tests.lib.default_conf import make_conf
from mail.nwsmtp.tests.lib.env import make_env
from mail.nwsmtp.tests.lib.util import make_plain_message, make_message_from_string
from aiosmtplib.errors import SMTPDataError

HEADERS_COUNT_LIMIT = 10

TEMPLATE_HEADERS_COUNT = 7

MESSAGE_DATE_PATTERN = "^(Sun|Mon|Tue|Wed|Thu|Fri|Sat), [0-9]{2} (Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec) [0-9]{4} [0-9]{2}:[0-9]{2}:[0-9]{2} \\+[0-9]{4}$"

MESSAGE_ID_PATTERN = "^<[0-9]{14}\\.[A-Za-z0-9]{8}@.*>$"


def set_headers_count_limit(conf):
    conf.nwsmtp.message_processing.headers_count_limit = HEADERS_COUNT_LIMIT


def gen_extra_headers(headers_count):
    return [("Header{i}".format(i=i), str(i)) for i in range(TEMPLATE_HEADERS_COUNT, headers_count)]


def make_header(value, email):
    return "=?utf-8?B?=" + b64encode(value.encode("UTF-8")).decode("UTF-8") + "?= <" + email + ">"


@pytest.mark.mxfront
async def test_correct_count_headers(cluster, sender, users, rcpt):
    headers = gen_extra_headers(HEADERS_COUNT_LIMIT)
    _, msg = make_plain_message(sender, rcpt, headers=headers)
    with make_conf(cluster, customize_with=set_headers_count_limit) as conf:
        async with make_env(cluster, users, conf) as env:
            client = await env.nwsmtp.get_client()
            code, reply = await client.send_message(msg, sender.email, rcpt.email)

    assert_that(reply, contains_string("2.0.0 Ok: queued on"))


@pytest.mark.mxfront
async def test_send_message_without_headers(env, sender, rcpt):
    msg = make_message_from_string("Test message\r\n")
    client = await env.nwsmtp.get_client()

    code, reply = await client.send_message(msg, sender.email, rcpt.email)
    assert_that(reply, contains_string("2.0.0 Ok: queued on"))

    msgs = await env.relays.local.wait_msgs()
    assert_that(msgs, has_length(1))
    assert_that(msgs[0], has_length(2))
    assert_that(msgs[0][1], has_length(1))

    mime = msgs[0][1][0].mime
    assert_that(
        mime,
        has_entries(
            From="MAILER-DAEMON",
            To="undisclosed-recipients:;",
            Date=matches_regexp(MESSAGE_DATE_PATTERN),
            **{
                "Return-Path": sender.email,
                "Message-Id": matches_regexp(MESSAGE_ID_PATTERN)
            },
        )
    )
    assert_that(mime, not_(has_key("Subject")))
    assert_that(mime.get_payload(), "Test message")


@pytest.mark.mxfront
async def test_send_message_with_headers(env, sender, rcpt):
    msg = make_message_from_string("Subject: subject\r\nTo: to\r\nFrom: from\r\nMessage-Id: id\r\n\r\nTest message\r\n")
    client = await env.nwsmtp.get_client()

    code, reply = await client.send_message(msg, sender.email, rcpt.email)
    assert_that(reply, contains_string("2.0.0 Ok: queued on"))

    msgs = await env.relays.local.wait_msgs()
    assert_that(msgs, has_length(1))
    assert_that(msgs[0], has_length(2))
    assert_that(msgs[0][1], has_length(1))

    mime = msgs[0][1][0].mime
    assert_that(
        mime,
        has_entries(
            From="from",
            To="to",
            Subject="subject",
            Date=matches_regexp(MESSAGE_DATE_PATTERN),
            **{
                "Return-Path": sender.email,
                "Message-Id": "id"
            },
        )
    )
    assert_that(mime.get_payload(), "Test message")


@pytest.mark.mxfront
async def test_too_many_headers(cluster, sender, users, rcpt):
    headers = gen_extra_headers(HEADERS_COUNT_LIMIT + 1)
    _, msg = make_plain_message(sender, rcpt, headers=headers)
    with make_conf(cluster, customize_with=set_headers_count_limit) as conf:
        async with make_env(cluster, users, conf) as env:
            client = await env.nwsmtp.get_client()
            with pytest.raises(SMTPDataError) as exc:
                await client.send_message(msg, sender.email, rcpt.email)

    assert_that(exc.value.code, 500)
    assert_that(exc.value.message, contains_string("5.3.4 Too many headers"))


@pytest.mark.mxfront
@pytest.mark.parametrize("mail_from", [
    ("Имя со странными значками Ю—В–њ—А–∞–≤–Є—В–µ–ї—М –њ–Є—Б—М–Љ–∞"),
    ("Sender, comma"),
    ("Sender; semi"),
])
async def test_mail_from_header(env, sender, rcpt, mail_from):
    mail_from_header = make_header(mail_from, sender.email)
    msg_id, msg = make_plain_message(mail_from_header, rcpt)
    client = await env.nwsmtp.get_client()
    code, reply = await client.send_message(msg, sender.email, rcpt.email)
    msg = await env.relays.wait_msg(msg_id)

    assert_that(reply, contains_string("2.0.0 Ok: queued on"))
    assert_that(mail_from_header, equal_to(msg.mime["From"]))
    assert_that(msg.envelope.mail_from,  equal_to(sender.email))


@pytest.mark.mxfront
@pytest.mark.parametrize("rcpt_to", [
    ("Имя со странными значками Ю—В–њ—А–∞–≤–Є—В–µ–ї—М –њ–Є—Б—М–Љ–∞"),
    ("Receiver, comma"),
    ("Receiver; semi"),
])
async def test_rcpt_to_header(env, sender, rcpt, rcpt_to):
    rcpt_to_header = make_header(rcpt_to, sender.email)
    msg_id, msg = make_plain_message(sender, rcpt_to_header)
    client = await env.nwsmtp.get_client()
    code, reply = await client.send_message(msg, sender.email, rcpt.email)
    msg = await env.relays.wait_msg(msg_id)

    assert_that(reply, contains_string("2.0.0 Ok: queued on "))
    assert_that(rcpt_to_header, equal_to(msg.mime["To"]))
    assert_that(msg.envelope.rcpt_tos, equal_to([rcpt.email]))

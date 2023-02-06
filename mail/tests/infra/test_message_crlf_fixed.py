# -*- coding: utf-8 -*-
import pytest
import uuid

from hamcrest import assert_that, contains_string

from mail.nwsmtp.tests.lib.env import make_env, make_conf
from mail.nwsmtp.tests.lib.util import make_message_from_string


def enable_fix_crlf(conf):
    conf.ymod_smtp_server.fix_crlf = True


def disable_fix_crlf(conf):
    conf.ymod_smtp_server.fix_crlf = False


@pytest.mark.mxbackout
async def test_send_message_with_incorrect_crlf_should_be_fixed_when_fix_crlf_enabled(cluster, users, sender, rcpt):
    letter = "From: {}\nTo: {}\nMessage-Id: msgid-{}\n\nThis is test message\nFix bad \r\n\r\r\r\ncrlf\n".format(sender.email, rcpt.email, uuid.uuid4())
    orig_msg = make_message_from_string(letter)
    msg_id = orig_msg["Message-Id"]

    with make_conf(cluster, customize_with=enable_fix_crlf) as conf:
        async with make_env(cluster, users, conf) as env:
            client = await env.nwsmtp.get_client()

            code, reply = await client.execute_command("EHLO tests-host".encode("ascii"))
            code, reply = await client.execute_command("MAIL FROM:<{}>".format(sender.email).encode("ascii"))
            code, reply = await client.execute_command("RCPT TO:<{}>".format("second@yandex.ru").encode("ascii"))
            code, reply = await client.execute_command("DATA".encode("ascii"))
            code, reply = await client.execute_command(letter.encode('ascii') + b"\r\n.")

            assert_that(reply, contains_string("2.0.0 Ok: queued on"))

            msg = await env.relays.wait_msg(msg_id)
            assert msg.mime.get_payload() == "This is test message\r\nFix bad \r\n\r\ncrlf\r\n"


@pytest.mark.mxbackout
async def test_send_message_with_incorrect_crlf_should_not_be_fixed_when_fix_crlf_disabled(cluster, users, sender, rcpt):
    text = "This is test message\nFix bad \r\n\r\r\r\ncrlf\n"
    letter = "From: {}\nTo: {}\nMessage-Id: msgid-{}\n\n".format(sender.email, rcpt.email, uuid.uuid4()) + text
    orig_msg = make_message_from_string(letter)
    msg_id = orig_msg["Message-Id"]

    with make_conf(cluster, customize_with=disable_fix_crlf) as conf:
        async with make_env(cluster, users, conf) as env:
            client = await env.nwsmtp.get_client()

            code, reply = await client.execute_command("EHLO tests-host".encode("ascii"))
            code, reply = await client.execute_command("MAIL FROM:<{}>".format(sender.email).encode("ascii"))
            code, reply = await client.execute_command("RCPT TO:<{}>".format("second@yandex.ru").encode("ascii"))
            code, reply = await client.execute_command("DATA".encode("ascii"))
            code, reply = await client.execute_command(letter.encode('ascii') + b"\r\n.")

            assert_that(reply, contains_string("2.0.0 Ok: queued on"))

            msg = await env.relays.wait_msg(msg_id)
            assert msg.mime.get_payload() == text

# -*- coding: utf-8 -*-
import pytest
import json

from base64 import b64decode
from hamcrest import assert_that, equal_to, contains_string

from yatest.common import work_path

from mail.nwsmtp.tests.lib.env import make_env, make_conf
from mail.nwsmtp.tests.lib.util import make_plain_message, make_message_from_string


def disable_so_and_fix_crlf(conf):
    conf.modules.nwsmtp.configuration.so.check = False
    conf.ymod_smtp_server.fix_crlf = False


@pytest.mark.mxbackout
@pytest.mark.parametrize(
    "source_file",
    ["book-war-and-peace.txt"]
)
async def test_send_big_file(cluster, sender, rcpt, source_file, users):
    text = ''
    with open(work_path(source_file), "r") as f:
        text = f.read()
    msg_id, orig_msg = make_plain_message(sender, rcpt, text=text)

    with make_conf(cluster, customize_with=disable_so_and_fix_crlf) as conf:
        async with make_env(cluster, users, conf) as env:
            client = await env.nwsmtp.get_client()
            code, reply = await client.send_message(orig_msg)

            assert_that(reply, contains_string("2.0.0 Ok: queued on"))

            msg = await env.relays.wait_msg(msg_id)
            assert_that(text, equal_to(b64decode(msg.mime.get_payload()).decode('utf8')))


@pytest.mark.mxbackout
@pytest.mark.parametrize(
    "source_file", [
        "aeronews.eml",
        "news_s_grouponsite.eml",
        "personalnews_s_social.eml",
        "rzd.eml"
    ]
)
async def test_send_eml_with_html_canon(cluster, sender, rcpt, users, source_file):
    text = ''
    with open(work_path(source_file), "r") as f:
        text = f.read()
    orig_msg = make_message_from_string(text)
    msg_id = orig_msg["Message-Id"]

    canonical_file = work_path("canonical_" + source_file)
    output = {"payload": [], "headers": []}
    with make_conf(cluster, customize_with=disable_so_and_fix_crlf) as conf:
        async with make_env(cluster, users, conf) as env:
            client = await env.nwsmtp.get_client()

            code, reply = await client.execute_command('EHLO tests-host'.encode('ascii'))
            code, reply = await client.execute_command('MAIL FROM:<{}>'.format(sender.email).encode('ascii'))
            code, reply = await client.execute_command('RCPT TO:<{}>'.format('second@yandex.ru').encode('ascii'))
            code, reply = await client.execute_command('DATA'.encode('ascii'))
            code, reply = await client.execute_command(orig_msg.as_string().encode('utf8') + b'\r\n.')

            assert_that(reply, contains_string("2.0.0 Ok: queued on"))

            msg = await env.relays.wait_msg(msg_id)

            if isinstance(msg.mime.get_payload(), list):
                for k in range(len(msg.mime.get_payload())):
                    output["payload"].append(msg.mime.get_payload()[k].get_payload())
            else:
                output["payload"].append(msg.mime.get_payload())

            for hdr in ["From", "To", "DKIM-Signature", "Message-Id", "Subject"]:
                output["headers"].append(msg.mime.get(hdr))

    with open(canonical_file, "r") as fd:
        loaded_data = json.load(fd)

    assert loaded_data == output

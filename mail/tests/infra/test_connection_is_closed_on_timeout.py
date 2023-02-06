# -*- coding: utf-8 -*-
import asyncio
import pytest
import aiosmtplib

from hamcrest import assert_that, contains_string

from mail.nwsmtp.tests.lib.env import make_env, make_conf
from mail.nwsmtp.tests.lib.util import make_plain_message


SMALL_AMOUNT_OF_TIME_SEC = 5
GREATER_THAN_SMALL_AMOUNT_OF_TIME_SEC = SMALL_AMOUNT_OF_TIME_SEC + 2


def enable_timeout(conf):
    conf.modules.nwsmtp.configuration.smtp_connection.session_time_limit.use = True
    conf.modules.nwsmtp.configuration.smtp_connection.session_time_limit.timeout = SMALL_AMOUNT_OF_TIME_SEC


def disable_timeout(conf):
    conf.modules.nwsmtp.configuration.smtp_connection.session_time_limit.use = False


@pytest.mark.mxfront
async def test_connection_is_closed_when_timeout_is_enabled(cluster, sender, rcpt, users):
    text = 'Subject:Hello\r\n\r\nHello'
    msg_id, orig_msg = make_plain_message(sender, rcpt, text=text)

    with make_conf(cluster, customize_with=enable_timeout) as conf:
        async with make_env(cluster, users, conf) as env:
            client = await env.nwsmtp.get_client()
            await asyncio.sleep(GREATER_THAN_SMALL_AMOUNT_OF_TIME_SEC)
            with pytest.raises(aiosmtplib.errors.SMTPServerDisconnected):
                code, reply = await client.send_message(orig_msg)


@pytest.mark.mxfront
async def test_connection_is_not_closed_when_timeout_is_disabled(cluster, sender, rcpt, users):
    text = 'Subject:Hello\r\n\r\nHello'
    msg_id, orig_msg = make_plain_message(sender, rcpt, text=text)

    with make_conf(cluster, customize_with=disable_timeout) as conf:
        async with make_env(cluster, users, conf) as env:
            client = await env.nwsmtp.get_client()
            await asyncio.sleep(GREATER_THAN_SMALL_AMOUNT_OF_TIME_SEC)
            code, reply = await client.send_message(orig_msg)
            assert_that(reply, contains_string("2.0.0 Ok: queued on"))

import pytest

from hamcrest import assert_that, contains_string, is_

from mail.nwsmtp.tests.lib.util import make_plain_message
from mail.nwsmtp.tests.lib.default_conf import make_conf
from mail.nwsmtp.tests.lib.env import make_env

from aiosmtplib.errors import SMTPDataError


def switch_off_rpop_auth(conf):
    conf.nwsmtp.yarm.use_rpop_auth = False


@pytest.mark.mxbackout
async def test_send_as_collector_failed_when_no_collectors_found(env, sender, rcpt):
    client = await env.nwsmtp.get_client()
    _, msg = make_plain_message("bad_collector@mail.ru", rcpt)

    with pytest.raises(SMTPDataError) as ex:
        await client.send_message(msg, sender.email, rcpt.email)

    assert_that(ex.value.code, is_(554))
    assert_that(ex.value.message, contains_string("5.7.0 Failed to authorize the sender"))


@pytest.mark.mxbackout
async def test_send_as_collector_failed_when_yarm_is_down(env, sender, rcpt):
    client = await env.nwsmtp.get_client()
    _, msg = make_plain_message("collector@mail.ru", rcpt)

    await env.stubs.yarm.stop()

    with pytest.raises(SMTPDataError) as ex:
        await client.send_message(msg, sender.email, rcpt.email)

    assert_that(ex.value.code, is_(451))
    assert_that(ex.value.message, contains_string(
        "4.7.1 Sorry, the service is currently unavailable. Please come back later."
    ))


@pytest.mark.mxbackout
async def test_send_as_collector_with_rpop_auth_off(cluster, users, env, sender, rcpt):
    msg_id, msg = make_plain_message("collector@mail.ru", "foreign@gmail.com")

    with make_conf(cluster, customize_with=switch_off_rpop_auth) as conf:
        async with make_env(cluster, users, conf) as env:
            client = await env.nwsmtp.get_client()
            code, reply = await client.send_message(msg, sender.email, "foreign@gmail.com")
            await env.relays.mail.wait_msg(msg_id)

    assert_that(bool(code), is_(False))
    assert_that(reply, contains_string("2.0.0 Ok: queued on"))


@pytest.mark.mxbackout
async def test_send_as_collector(env, sender, rcpt):
    client = await env.nwsmtp.get_client()
    _, msg = make_plain_message("collector@mail.ru", rcpt)

    code, reply = await client.send_message(msg, sender.email, rcpt.email)

    assert_that(bool(code), is_(False))
    assert_that(reply, contains_string("2.0.0 Ok: queued on"))

import pytest

from hamcrest import assert_that, contains_inanyorder, has_entries

from unittest.mock import patch

from mail.nwsmtp.tests.lib.hint import get_hint_values
from mail.nwsmtp.tests.lib.users import gen_big_ml
from mail.nwsmtp.tests.lib.util import make_plain_message
from mail.nwsmtp.tests.lib.stubs import bigml


@pytest.mark.mxbackout
@pytest.mark.mxfront
@pytest.mark.smtp
@pytest.mark.yaback
async def test_delivery_to_ml(env, sender, big_ml):
    assert env.conf.nwsmtp.big_ml.use, "Test requires big_ml enabled"

    client = await env.nwsmtp.get_client()
    if env.nwsmtp.is_auth_required():
        await client.login(sender.email, sender.passwd)
    msg_id, msg = make_plain_message(sender, big_ml)

    await client.send_message(msg)
    msg = await env.relays.fallback.wait_msg(msg_id)

    rcpts = list(big_ml.subscribers.keys())
    if env.conf.nwsmtp.delivery_to_sender_control.use:
        rcpts.append(sender.email)

    assert_that(msg.envelope.rcpt_tos, contains_inanyorder(*rcpts))


@pytest.mark.mxfront
async def test_delivery_to_ml_when_subscribers_more_than_recipients_limit(env, users, sender):
    """ Test that subscribers is not limited by rcpts limit, because that limit is about
        amount of rcpt-to commands in smtp session.
    """
    assert env.conf.nwsmtp.big_ml.use, "Test requires big_ml enabled"

    rcpts_limit = env.conf.nwsmtp.smtp_connection.proto_constraints.recipient_limit
    assert rcpts_limit < 100, "Fallback to custom nwsmtp config?"

    ml = gen_big_ml(rcpts_limit + 1, users)
    client = await env.nwsmtp.get_client()
    msg_id, msg = make_plain_message(sender, ml)

    await client.send_message(msg)
    msg = await env.relays.fallback.wait_msg(msg_id)

    rcpts = list(ml.subscribers.keys())

    assert_that(msg.envelope.rcpt_tos, contains_inanyorder(*rcpts))


@pytest.mark.mxbackout
async def test_delivery_to_empty_ml(env, sender, empty_big_ml):
    assert env.conf.nwsmtp.big_ml.use, "Test requires big_ml enabled"

    client = await env.nwsmtp.get_client()
    msg_id, msg = make_plain_message(sender, empty_big_ml)

    await client.send_message(msg)
    msg = await env.relays.local.wait_msg(msg_id)

    rcpts = [empty_big_ml.email]
    if env.conf.nwsmtp.delivery_to_sender_control.use:
        rcpts.append(sender.email)

    assert_that(msg.envelope.rcpt_tos, contains_inanyorder(*rcpts))


@pytest.mark.mxbackout
@pytest.mark.smtp
async def test_delivery_to_ml_when_sender_in_subscribers(env, sender, rcpt, big_ml):
    assert env.conf.nwsmtp.big_ml.use, "Test requires big_ml enabled"

    client = await env.nwsmtp.get_client()
    if env.nwsmtp.is_auth_required():
        await client.login(sender.email, sender.passwd)
    msg_id, msg = make_plain_message(sender, big_ml)

    with patch.object(bigml, "get_additional_subscribers") as patched:
        patched.return_value = [{"email": sender.email, "uid": sender.uid}]
        await client.send_message(msg, sender.email, [big_ml.email, rcpt.email])
        msg = await env.relays.fallback.wait_msg(msg_id)
        assert patched.call_once

    rcpts = [*big_ml.subscribers.keys(), rcpt.email]
    if env.conf.nwsmtp.delivery_to_sender_control.use:
        rcpts.append(sender.email)

    assert_that(msg.envelope.rcpt_tos, contains_inanyorder(*rcpts))
    assert_that(
        get_hint_values(msg),
        has_entries(
            save_to_sent=["0"],
            notify=["0"],
            email=[sender.email],
            filters=["0"],
            skip_loop_prevention=["1"],
            copy_to_inbox=["1"],
        )
    )


@pytest.mark.mxbackout
async def test_delivery_to_ml_when_user_with_bad_karma_in_subscribers(env, sender, big_ml, bad_karma_rcpt):
    assert env.conf.nwsmtp.big_ml.use, "Test requires big_ml enabled"

    client = await env.nwsmtp.get_client()
    msg_id, msg = make_plain_message(sender, big_ml)

    with patch.object(bigml, "get_additional_subscribers") as patched:
        patched.return_value = [{"email": bad_karma_rcpt.email, "uid": bad_karma_rcpt.uid}]
        await client.send_message(msg)
        msg = await env.relays.fallback.wait_msg(msg_id)
        assert patched.call_once

    rcpts = list(big_ml.subscribers.keys())
    rcpts.append(bad_karma_rcpt.email)
    if env.conf.nwsmtp.delivery_to_sender_control.use:
        rcpts.append(sender.email)

    assert_that(msg.envelope.rcpt_tos, contains_inanyorder(*rcpts))

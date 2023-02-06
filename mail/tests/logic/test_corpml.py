import pytest

from hamcrest import assert_that, contains_inanyorder, contains_string

from unittest.mock import patch

from aiosmtplib.errors import SMTPRecipientsRefused

from mail.nwsmtp.tests.lib.util import make_plain_message
from mail.nwsmtp.tests.lib.stubs import corpml


@pytest.mark.mxcorp
async def test_delivery_when_ml_list_is_not_in_bb(env, sender, corp_ml_not_registered_in_blackbox):
    assert env.conf.nwsmtp.corp_maillist.use, "Test requires corp_maillist enabled"

    client = await env.nwsmtp.get_client()
    msg_id, msg = make_plain_message(sender, corp_ml_not_registered_in_blackbox)

    await client.send_message(msg)
    msg = await env.relays.wait_msg(msg_id)

    rcpts = list(corp_ml_not_registered_in_blackbox.subscribers.keys())
    assert_that(msg.envelope.rcpt_tos, contains_inanyorder(*rcpts))


@pytest.mark.mxcorp
async def test_delivery_to_empty_ml_list(env, sender, empty_corp_ml):
    assert env.conf.nwsmtp.corp_maillist.use, "Test requires corp_maillist enabled"

    client = await env.nwsmtp.get_client()
    msg_id, msg = make_plain_message(sender, empty_corp_ml)

    await client.send_message(msg)
    msg = await env.relays.local.wait_msg(msg_id)

    assert_that(msg.envelope.rcpt_tos, contains_inanyorder(empty_corp_ml.email))


@pytest.mark.mxcorp
async def test_delivery_to_read_only_ml_list(env, sender, corp_ml):
    assert env.conf.nwsmtp.corp_maillist.use, "Test requires corp_maillist enabled"

    msg_id, msg = make_plain_message(sender, corp_ml)
    with patch.object(corpml, "get_readonly") as patched:
        patched.return_value = True
        client = await env.nwsmtp.get_client()
        with pytest.raises(SMTPRecipientsRefused) as exc:
            await client.send_message(msg)

    assert_that(exc.value.recipients[0], 530)
    assert_that(str(exc), contains_string("5.7.2 This maillist is readonly for you!"))


@pytest.mark.mxcorp
async def test_delivery_to_internal_ml_list_from_external_sender(env, corp_ml):
    assert env.conf.nwsmtp.corp_maillist.use, "Test requires corp_maillist enabled"

    msg_id, msg = make_plain_message("foreign@gmail.com", corp_ml)
    with patch.object(corpml, "get_is_internal") as patched:
        patched.return_value = True
        client = await env.nwsmtp.get_client()
        with pytest.raises(SMTPRecipientsRefused) as exc:
            await client.send_message(msg)

    assert_that(exc.value.recipients[0], 530)
    assert_that(str(exc), contains_string("5.7.2 This maillist is only for internal addresses!"))


@pytest.mark.mxcorp
@pytest.mark.smtpcorp
async def test_delivery_to_ml_list(env, sender, corp_ml):
    assert env.conf.nwsmtp.corp_maillist.use, "Test requires corp_maillist enabled"

    client = await env.nwsmtp.get_client()
    msg_id, msg = make_plain_message(sender, corp_ml)

    if env.nwsmtp.is_auth_required():
        await client.login(sender.email, sender.passwd)
    await client.send_message(msg)
    msg = await env.relays.fallback.wait_msg(msg_id)

    rcpts = list(corp_ml.subscribers.keys())
    assert_that(msg.envelope.rcpt_tos, contains_inanyorder(*rcpts))


@pytest.mark.mxbackcorp
@pytest.mark.smtpcorp
async def test_delivery_to_rcpt_with_exchange(env, sender, rcpt_with_exchange):
    assert env.conf.nwsmtp.corp_maillist.use, "Test requires corp_maillist enabled"

    client = await env.nwsmtp.get_client()
    msg_id, msg = make_plain_message(sender, rcpt_with_exchange)

    if env.nwsmtp.is_auth_required():
        await client.login(sender.email, sender.passwd)
    await client.send_message(msg)
    msg_exchange = await env.relays.external.wait_msg(msg_id)
    rcpts_exchange = [rcpt for rcpt in rcpt_with_exchange.subscribers.keys() if "ld.yandex.ru" in rcpt]

    msg_local = await env.relays.fallback.wait_msg(msg_id)
    rcpts_local = [rcpt for rcpt in rcpt_with_exchange.subscribers.keys() if "mail.yandex-team.ru" in rcpt]
    if env.conf.nwsmtp.delivery_to_sender_control.use:
        rcpts_local.append(sender.email)

    assert_that(msg_exchange.envelope.rcpt_tos, contains_inanyorder(*rcpts_exchange))
    assert_that(msg_local.envelope.rcpt_tos, contains_inanyorder(*rcpts_local))

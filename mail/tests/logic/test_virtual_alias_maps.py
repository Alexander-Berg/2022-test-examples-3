import pytest

from base64 import b64decode
from hamcrest import assert_that, contains_inanyorder

from mail.nwsmtp.tests.lib.util import make_plain_message


@pytest.mark.mxfront
async def test_virtual_alias_maps(env, sender, virtual_alias_rcpt):
    client = await env.nwsmtp.get_client()

    msg_id, msg = make_plain_message(sender, virtual_alias_rcpt)

    await client.send_message(msg)
    msg = await env.relays.wait_msg(msg_id)

    assert_that(msg.envelope.rcpt_tos, contains_inanyorder("virtualalias@yandex.ru", "second@yandex.ru"))
    assert_that("copy_to_inbox" not in b64decode(msg.mime["X-Yandex-Hint"]).decode())


@pytest.mark.mxbackout
@pytest.mark.smtp
async def test_virtual_alias_maps_copy_to_inbox_for_sender_not_in_recipient_list(
        env, sender, virtual_alias_rcpt):
    client = await env.nwsmtp.get_client()
    if env.nwsmtp.is_auth_required():
        await client.login(sender.email, sender.passwd)

    msg_id, msg = make_plain_message(sender, virtual_alias_rcpt)

    await client.send_message(msg)
    msg = await env.relays.fallback.wait_msg(msg_id)

    assert_that(msg.envelope.rcpt_tos, contains_inanyorder(
        "first@yandex.ru", "virtualalias@yandex.ru", "second@yandex.ru"))
    assert_that("copy_to_inbox=0" in b64decode(msg.mime["X-Yandex-Hint"]).decode())


@pytest.mark.mxbackout
@pytest.mark.smtp
async def test_virtual_alias_maps_copy_to_inbox_for_sender_in_recipient_list(env, virtual_alias_rcpt):
    client = await env.nwsmtp.get_client()
    if env.nwsmtp.is_auth_required():
        await client.login(virtual_alias_rcpt.email, virtual_alias_rcpt.passwd)

    msg_id, msg = make_plain_message(virtual_alias_rcpt, virtual_alias_rcpt)

    await client.send_message(msg)
    msg = await env.relays.fallback.wait_msg(msg_id)

    assert_that(msg.envelope.rcpt_tos, contains_inanyorder("virtualalias@yandex.ru", "second@yandex.ru"))
    assert_that("copy_to_inbox=1" in b64decode(msg.mime["X-Yandex-Hint"]).decode())

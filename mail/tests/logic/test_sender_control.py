import pytest

from base64 import b64decode
from hamcrest import assert_that, contains_string, has_length, contains_inanyorder
from mail.nwsmtp.tests.lib.hint import make_hint

from mail.nwsmtp.tests.lib.util import make_plain_message


def get_personal_hint_value(msg, email):
    raw_headers = [b64decode(value).decode("utf-8").split("\n")
                   for header, value in msg.mime.items() if header == "X-Yandex-Hint"]
    return [value[:-1] for value in raw_headers if f"email={email}" in value]


def get_personal_hint(email):
    return make_hint(**{
        "copy_to_inbox": "1",
        "email": email,
        "filters": "1",
        "notify": "1",
        "save_to_sent": "1",
        "skip_loop_prevention": "1"
    })


@pytest.mark.mxbackout
@pytest.mark.smtp
async def test_delivery_when_sender_not_in_rcpts_and_no_hints(env, sender, rcpt):
    msg_id, msg = make_plain_message(sender, rcpt)

    client = await env.nwsmtp.get_client()
    if env.nwsmtp.is_auth_required():
        await client.login(sender.email, sender.passwd)
    code, reply = await client.send_message(msg)

    assert_that(reply, contains_string("2.0.0 Ok: queued on"))

    msg = await env.relays.local.wait_msg(msg_id)
    assert_that(msg.envelope.rcpt_tos, contains_inanyorder(rcpt.email, sender.email))

    hint_value = get_personal_hint_value(msg, sender.email)

    assert_that(hint_value, has_length(1))
    assert_that(
        hint_value[0],
        contains_inanyorder(
            "copy_to_inbox=0",
            "email=first@yandex.ru",
            "filters=0",
            "notify=0",
            "save_to_sent=0",
            "skip_loop_prevention=1"
        )
    )


@pytest.mark.mxbackout
@pytest.mark.smtp
async def test_delivery_when_sender_in_rcpts_and_no_hints(env, sender):
    msg_id, msg = make_plain_message(sender, sender)

    client = await env.nwsmtp.get_client()
    if env.nwsmtp.is_auth_required():
        await client.login(sender.email, sender.passwd)
    code, reply = await client.send_message(msg)

    assert_that(reply, contains_string("2.0.0 Ok: queued on"))

    msg = await env.relays.local.wait_msg(msg_id)
    assert_that(msg.envelope.rcpt_tos, contains_inanyorder(sender.email))

    hint_value = get_personal_hint_value(msg, sender.email)

    assert_that(hint_value, has_length(1))
    assert_that(
        hint_value[0],
        contains_inanyorder(
            "copy_to_inbox=1",
            "email=first@yandex.ru",
            "filters=0",
            "notify=0",
            "save_to_sent=0",
            "skip_loop_prevention=1"
        )
    )


@pytest.mark.mxbackout
async def test_delivery_when_sender_not_in_rcpts(env, sender, rcpt):
    msg_id, msg = make_plain_message(sender, rcpt, headers=(
        ("X-Yandex-Hint", get_personal_hint(sender.email)),
    ))

    client = await env.nwsmtp.get_client()
    code, reply = await client.send_message(msg)

    assert_that(reply, contains_string("2.0.0 Ok: queued on"))

    msg = await env.relays.local.wait_msg(msg_id)
    assert_that(msg.envelope.rcpt_tos, contains_inanyorder(sender.email, rcpt.email))

    hint_value = get_personal_hint_value(msg, sender.email)

    assert_that(hint_value, has_length(1))
    assert_that(
        hint_value[0],
        contains_inanyorder(
            "copy_to_inbox=0",
            "email=first@yandex.ru",
            "filters=1",
            "notify=1",
            "save_to_sent=1",
            "skip_loop_prevention=1"
        )
    )


@pytest.mark.mxbackout
async def test_delivery_when_sender_in_rcpts(env, sender):
    msg_id, msg = make_plain_message(sender, sender, headers=(
        ("X-Yandex-Hint", get_personal_hint(sender.email)),
    ))

    client = await env.nwsmtp.get_client()
    code, reply = await client.send_message(msg)

    assert_that(reply, contains_string("2.0.0 Ok: queued on"))

    msg = await env.relays.local.wait_msg(msg_id)
    assert_that(msg.envelope.rcpt_tos, contains_inanyorder(sender.email))

    hint_value = get_personal_hint_value(msg, sender.email)

    assert_that(hint_value, has_length(1))
    assert_that(
        hint_value[0],
        contains_inanyorder(
            "copy_to_inbox=1",
            "email=first@yandex.ru",
            "filters=1",
            "notify=1",
            "save_to_sent=1",
            "skip_loop_prevention=1"
        )
    )

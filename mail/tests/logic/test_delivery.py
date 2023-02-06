import pytest

from hamcrest import assert_that, has_items, has_length, contains_inanyorder, contains_string

from email.utils import make_msgid
from mail.nwsmtp.tests.lib.headers import Header
from mail.nwsmtp.tests.lib import CLUSTERS
from mail.nwsmtp.tests.lib.util import make_plain_message, make_message_from_string
from mail.nwsmtp.tests.lib.users import gen_users
from mail.nwsmtp.tests.lib.default_conf import make_conf
from mail.nwsmtp.tests.lib.env import make_env
from mail.nwsmtp.tests.lib.stubs import so

from unittest.mock import patch


def change_routing_conf(conf):
    conf.nwsmtp.delivery.routing.primary = "map"
    conf.nwsmtp.delivery.routing.my_dest_domains = ["@yandex.ru"]


def assert_mime_equals(left, right):
    for k, v in left.items():
        assert left[k] == right[k], f"Header '{k}' is not same"
    assert left.get_payload().strip() == right.get_payload().strip(), "Body is not same"


@pytest.mark.cluster(CLUSTERS)
async def test_two_messages_over_one_connection_have_good_structures(env, sender, rcpt):
    client = await env.nwsmtp.get_client()
    if env.nwsmtp.is_auth_required():
        await client.login(sender.email, sender.passwd)

    msg_id, left_msg = make_plain_message(sender, rcpt, "First body")
    await client.send_message(left_msg, sender.email, rcpt.email)
    right_msg = await env.relays.wait_msg(msg_id)

    assert_mime_equals(left_msg, right_msg.mime)

    msg_id, left_msg = make_plain_message(sender, rcpt, "Second body")
    await client.send_message(left_msg, sender.email, rcpt.email)
    right_msg = await env.relays.wait_msg(msg_id)

    assert_mime_equals(left_msg, right_msg.mime)


@pytest.mark.smtp
@pytest.mark.mxfront
@pytest.mark.yaback
async def test_rcpts_limit_exceeded(env, sender, rcpt, users):
    client = await env.nwsmtp.get_client()
    if env.nwsmtp.is_auth_required():
        await client.login(sender.email, sender.passwd)

    rcpts_limit = env.conf.nwsmtp.smtp_connection.proto_constraints.recipient_limit
    assert rcpts_limit <= 100, "Amount of rcpts is too big, really need it?"
    rcpts = gen_users(rcpts_limit + 1, users, env.is_corp())
    emails = [rcpt.email for rcpt in rcpts[:-1]]

    msg_id, msg = make_plain_message(sender, rcpt)
    info, _ = await client.send_message(msg, sender.email, [rcpt.email for rcpt in rcpts])
    code, reply = info[rcpts[-1].email]

    assert_that(code, 452)
    assert_that(reply, contains_string("4.5.3 Error: too many recipients"))
    assert_that(info, has_length(1))

    msg = await env.relays.wait_msg(msg_id)

    if env.conf.nwsmtp.delivery_to_sender_control.use:
        emails.append(sender.email)
    assert_that(msg.envelope.rcpt_tos, contains_inanyorder(*emails))


@pytest.mark.mxfront
@pytest.mark.parametrize("msg", [
    "only_header_or_body",
    "\r\nonly_body",
    "\ronly_body",
    "\nonly_body",
    "\r\n",
    ""
])
async def test_msg_containing_only_body(env, sender, rcpt, msg):
    client = await env.nwsmtp.get_client()
    _, reply = await client.sendmail(sender.email, rcpt.email, msg)
    assert "2.0.0 Ok: queued on" in reply


@pytest.mark.mxfront
@pytest.mark.mxbackout
@pytest.mark.smtp
@pytest.mark.yaback
@pytest.mark.parametrize("divider", ["\r\n", "\n", "\r"])
@pytest.mark.parametrize("number_of_whitespaces_after_headers", [0, 1, 2])
async def test_msg_containing_two_headers(env, sender, rcpt, divider, number_of_whitespaces_after_headers):
    client = await env.nwsmtp.get_client()
    if env.nwsmtp.is_auth_required():
        await client.login(sender.email, sender.passwd)

    msg_id = make_msgid()
    subject = "cat"
    headers = [
        Header.SUBJECT + ": " + subject,
        Header.MESSAGE_ID + ": " + msg_id]
    msg = divider.join(headers) + number_of_whitespaces_after_headers * divider

    _, reply = await client.sendmail(sender.email, rcpt.email, msg)
    assert "2.0.0 Ok: queued on" in reply
    right_msg = await env.relays.wait_msg(msg_id)

    assert right_msg.mime[Header.SUBJECT] == subject


@pytest.mark.mxfront
@pytest.mark.mxbackout
@pytest.mark.smtp
@pytest.mark.yaback
async def test_mandatory_headers_addition_to_not_full_msgs(env, sender, rcpt):
    client = await env.nwsmtp.get_client()
    if env.nwsmtp.is_auth_required():
        await client.login(sender.email, sender.passwd)

    msg_id = make_msgid()
    msg = Header.MESSAGE_ID + ": " + msg_id

    _, reply = await client.sendmail(sender.email, rcpt.email, msg)
    assert "2.0.0 Ok: queued on" in reply
    right_msg = await env.relays.wait_msg(msg_id)

    assert right_msg.mime[Header.DATE] is not None
    assert right_msg.mime[Header.FROM] == "MAILER-DAEMON"
    assert right_msg.mime[Header.TO] == "undisclosed-recipients:;"


@pytest.mark.mxbackout
async def test_message_delivery_with_secret_headers_to_foreign_mx(env, cluster, users, sender, rcpt):
    removable_headers = [h["__text"] for h in env.conf.nwsmtp.delivery.remove_headers]
    assert removable_headers, "No headers to remove!"

    headers = "\r\n".join([f"{header}: 1" for header in removable_headers])

    message = (
        f"{headers}\r\n"
        "Date: 1\r\n"
        "Message-Id: id\r\n"
        "From: foreign@gmail.com\r\n"
        "To: foreign@gmail.com\r\n"
        "Subject: Test\r\n\r\n"
        "Hello"
    )

    msg = make_message_from_string(message)

    with make_conf(cluster, customize_with=change_routing_conf) as conf:
        async with make_env(cluster, users, conf) as env:
            client = await env.nwsmtp.get_client()
            _, reply = await client.send_message(msg, sender.email, "foreign@gmail.com")
            assert "2.0.0" in reply
            out_msg = await env.relays.external.wait_msg("id")

    assert "From" in out_msg.mime
    assert "To" in out_msg.mime
    assert "Subject" in out_msg.mime
    assert "Message-Id" in out_msg.mime
    assert "Date" in out_msg.mime
    assert not any(header for header in removable_headers
                    if header.lower() in out_msg.mime.as_string().lower())


@pytest.mark.mxbackout
@pytest.mark.smtp
@pytest.mark.parametrize("foreign", ["gre", "bla", "whi", "none"])
async def test_delivery_to_foreign_mx(cluster, foreign, users, sender, rcpt):
    msg_id, msg = make_plain_message(sender, rcpt)
    with make_conf(cluster, customize_with=change_routing_conf) as conf:
        async with make_env(cluster, users, conf) as env:
            with patch.object(so, "make_out_parameters") as patched_out_parameters:
                client = await env.nwsmtp.get_client()
                if env.nwsmtp.is_auth_required():
                    await client.login(sender.email, sender.passwd)
                if foreign == "bla":
                    forward_type = {"forward_type": "FORWARD_TYPE_MXBACK"}
                    relay = env.relays.black
                elif foreign == "whi":
                    forward_type = {"forward_type": "FORWARD_TYPE_WHITE"}
                    relay = env.relays.white
                elif foreign == "gre":
                    forward_type = {"forward_type": "FORWARD_TYPE_GRAY"}
                    relay = env.relays.grey
                else:
                    forward_type = {}
                    relay = env.relays.external

                patched_out_parameters.return_value = forward_type
                _, reply = await client.send_message(msg, sender.email, "foreign@gmail.com")
                assert_that(reply, contains_string("2.0.0 Ok: queued on"))
                out_msg = await relay.wait_msg(msg_id)

    assert_that(out_msg.envelope.rcpt_tos, has_items("foreign@gmail.com"))


@pytest.mark.mxcorp
async def test_plus_hack(env, sender, rcpt):
    client = await env.nwsmtp.get_client()
    msg_id, msg = make_plain_message(sender, rcpt)

    rcpt_email = rcpt.login + "+rcpt@yandex-team.ru"
    _, reply = await client.send_message(msg, sender.email, rcpt_email)
    assert "2.0.0" in reply

    out_msg = await env.relays.wait_msg(msg_id)
    assert_that(out_msg.envelope.rcpt_tos, has_items(rcpt_email))


@pytest.mark.mxbackout
@pytest.mark.yaback
async def test_delivery_to_rcpt_with_non_existent_domain(env, rcpt, sender):
    client = await env.nwsmtp.get_client()
    if env.nwsmtp.is_auth_required():
        await client.login(sender.email, sender.passwd)
    msg_id, msg = make_plain_message(sender, rcpt, headers=(
        ("X-Yandex-ForeignMX", "tutu"),
        ("X-Yandex-Spam", "1"),
    ))

    _, reply = await client.send_message(msg, sender.email, ["one@nonexistentdomain.one", rcpt.email])
    out_msg = await env.relays.external.wait_msg(msg_id)

    assert "2.0.0" in reply
    assert_that(out_msg.envelope.rcpt_tos, has_items("one@nonexistentdomain.one"))

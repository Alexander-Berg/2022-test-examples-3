import pytest
import json

from hamcrest import assert_that, contains, contains_inanyorder, has_entries

from unittest.mock import patch

from mail.nwsmtp.tests.lib.hint import get_hint_values
from mail.nwsmtp.tests.lib.util import make_message
from mail.nwsmtp.tests.lib.default_conf import make_conf
from mail.nwsmtp.tests.lib.env import make_env
from mail.nwsmtp.tests.lib.stubs import so, avir


def get_error(response):
    return json.loads(response)["error"]


def change_recipient_limit(conf):
    conf.nwsmtp.smtp_connection.proto_constraints.recipient_limit = 1


@pytest.fixture
def http_client(env):
    return env.nwsmtp.get_http_client()


pytestmark = [pytest.mark.mxbackout]


async def test_send_system_mail(env, sender, rcpt, http_client):
    msg_id, msg = make_message(rcpt, sender, text="Text", subject="Tests send mail")

    status, _ = await http_client.send_system_mail(sender, msg, [rcpt.email])
    msg = await env.relays.wait_msg(msg_id)

    assert status == 200

    assert msg.mime["Subject"] == "Tests send mail"
    assert msg.mime["To"] == "second@yandex.ru"
    assert msg.mime["X-Yandex-Spam"] == "1"
    assert msg.mime["X-Yandex-Avir"] == "1"
    assert msg.mime.get_payload() == "Text\r\n"

    assert_that(msg.envelope.rcpt_tos, contains(rcpt.email))


async def test_hint_headers(env, sender, rcpt, http_client):
    msg_id, msg = make_message(rcpt, sender)

    lids = ["piglid1", "piglid12"]
    labels = ["piglabel1", "piglabel2"]

    status, _ = await http_client.send_system_mail(sender, msg, [rcpt.email], labels, lids)
    msg = await env.relays.wait_msg(msg_id)

    assert status == 200

    assert_that(
        get_hint_values(msg),
        has_entries(
            label=contains_inanyorder(
                "symbol:encrypted_label",
                "piglabel1",
                "piglabel2",
            ),
            ipfrom=["::1"],
            lid=contains_inanyorder("piglid1", "piglid12"),
        )
    )


async def test_send_system_mail_without_spam_check(env, sender, rcpt, http_client):
    msg_id, msg = make_message(rcpt, sender)

    with patch.object(so, "get_so_resolution") as patched:
        patched.return_value = "SO_RESOLUTION_SPAM"
        status, answer = await http_client.send_system_mail(sender, msg, [rcpt.email])
        msg = await env.relays.wait_msg(msg_id)
        assert patched.not_called

    assert status == 200

    assert_that(msg.envelope.rcpt_tos, contains(rcpt.email))
    assert msg.mime["X-Yandex-Spam"] == "1"


async def test_send_system_mail_without_virus_check(sender, rcpt, http_client):
    msg_id, msg = make_message(rcpt, sender)

    with patch.object(avir, "is_virus") as patched:
        patched.return_value = True
        status, answer = await http_client.send_system_mail(sender,  msg, [rcpt.email])
        assert patched.not_called

    assert status == 200


async def test_send_system_mail_from_nonexistent_sender(env, nonexistent_sender, rcpt, http_client):
    msg_id, msg = make_message(rcpt, nonexistent_sender.email)

    status, _ = await http_client.send_system_mail(nonexistent_sender, msg, [rcpt.email])
    msg = await env.relays.wait_msg(msg_id)

    assert status == 200

    assert_that(msg.envelope.rcpt_tos, contains(rcpt.email))


async def test_send_system_mail_with_number_of_recipients_more_than_the_limit(cluster, users, sender, rcpt):
    msg_id, msg = make_message(rcpt, sender)

    with make_conf(cluster, customize_with=change_recipient_limit) as conf:
        async with make_env(cluster, users, conf) as env:
            status, answer = await env.nwsmtp.get_http_client().send_system_mail(sender,  msg, [rcpt.email, "peppa@gig.com"])

            assert status == 406
            assert get_error(answer) == "ToManyRecipients"


async def test_send_mail_recipient_with_bad_karma(sender, temp_bad_karma_rcpt, http_client):
    msg_id, msg = make_message(temp_bad_karma_rcpt, sender)

    status, answer = await http_client.send_system_mail(sender, msg, [temp_bad_karma_rcpt.email])

    assert status == 406
    assert get_error(answer) == "BadKarmaBanTime"

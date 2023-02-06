import pytest
import json

from hamcrest import assert_that, contains_inanyorder, has_entries

from unittest.mock import patch

from mail.nwsmtp.tests.lib.hint import get_hint_values
from mail.nwsmtp.tests.lib.util import make_message
from mail.nwsmtp.tests.lib.default_conf import make_conf
from mail.nwsmtp.tests.lib.env import make_env
from mail.nwsmtp.tests.lib.stubs import so, avir


def get_error(response):
    return json.loads(response)["error"]


def get_ban_reason(response):
    return json.loads(response)["ban_reason"]


def change_recipient_limit(conf):
    conf.nwsmtp.smtp_connection.proto_constraints.recipient_limit = 1


def change_message_size_limit(conf):
    conf.nwsmtp.smtp_connection.proto_constraints.message_size_limit = 0


pytestmark = [pytest.mark.mxbackout]


async def test_send_mail(env, sender, rcpt, http_client):
    msg_id, msg = make_message(rcpt, sender, text="Text", subject="Tests send mail")

    status, _ = await http_client.send_mail(sender, {"to": [rcpt.email]}, msg)
    msg = await env.relays.wait_msg(msg_id)

    assert status == 200

    assert msg.mime["Subject"] == "Tests send mail"
    assert msg.mime["To"] == "second@yandex.ru"
    assert msg.mime["X-Yandex-Spam"] == "1"
    assert msg.mime["X-Yandex-Avir"] == "1"
    assert msg.mime.get_payload() == "Text\r\n"

    assert_that(msg.envelope.rcpt_tos, contains_inanyorder(rcpt.email, sender.email))


async def test_delivery_to_maillist(env, sender, big_ml, http_client):
    msg_id, msg = make_message(big_ml, sender)

    status, _ = await http_client.send_mail(sender, {"to": [big_ml.email]}, msg)
    msg = await env.relays.wait_msg(msg_id)

    rcpts = list(big_ml.subscribers.keys())
    rcpts.append(sender.email)

    assert_that(msg.envelope.rcpt_tos, contains_inanyorder(*rcpts))


async def test_send_mail_without_to_parameter(sender, rcpt, http_client):
    _, msg = make_message(sender, rcpt)

    status, answer = await http_client.send_mail(sender, {}, msg)

    assert status == 400
    assert answer["message"] == "No such node (to)"


async def test_send_mail_with_bad_formated_message(sender, rcpt, http_client):
    status, answer = await http_client.send_mail(sender, {"to": [rcpt.email]}, "")

    assert status == 400
    assert answer["message"] == "Empty message in request body"


async def test_send_mail_to_recipient_with_bad_syntax(sender, http_client):
    msg_id, msg = make_message("peppa", sender)

    status, answer = await http_client.send_mail(sender, {"to": ["peppa"]}, msg)

    assert status == 406
    assert get_error(answer) == "BadRecipient"


async def test_send_mail_recipient_with_bad_karma(sender, temp_bad_karma_rcpt, http_client):
    msg_id, msg = make_message(temp_bad_karma_rcpt, sender)

    status, answer = await http_client.send_mail(sender, {"to": [temp_bad_karma_rcpt.email]}, msg)

    assert status == 406
    assert get_error(answer) == "BadKarmaBanTime"


async def test_send_virus_mail(sender, rcpt, http_client):
    msg_id, msg = make_message(rcpt, sender)

    with patch.object(avir, "is_virus") as patched:
        patched.return_value = True
        status, answer = await http_client.send_mail(sender, {"to": [rcpt.email]}, msg, "0", "1")

    assert status == 406
    assert get_error(answer) == "Virus"


async def test_send_as_collector_when_no_collectors_found(rcpt, sender, http_client):
    msg_id, msg = make_message(rcpt, "bad_collector@mail.ru")

    status, answer = await http_client.send_mail(sender, {"to": [rcpt.email]}, msg)

    assert status == 406
    assert get_error(answer) == "FailedToAuthSender"


async def test_send_mail_with_number_of_recipients_more_than_the_limit(cluster, users, sender, rcpt):
    msg_id, msg = make_message(rcpt, sender)

    with make_conf(cluster, customize_with=change_recipient_limit) as conf:
        async with make_env(cluster, users, conf) as env:
            status, answer = await env.nwsmtp.get_http_client().send_mail(sender, {"to": [rcpt.email, "peppa@gig.com"]}, msg)

            assert status == 406
            assert get_error(answer) == "ToManyRecipients"


async def test_send_mail_with_size_is_greater_than_the_limit(cluster, users, sender, rcpt):
    msg_id, msg = make_message(rcpt, sender)

    with make_conf(cluster, customize_with=change_message_size_limit) as conf:
        async with make_env(cluster, users, conf) as env:
            status, answer = await env.nwsmtp.get_http_client().send_mail(sender, {"to": [rcpt.email]}, msg)

            assert status == 406
            assert get_error(answer) == "SizeLimitExceeded"


async def test_hint_headers(env, sender, rcpt, http_client):
    msg_id, msg = make_message(rcpt, sender)

    send_mail_args = {
        "to": [rcpt.email],
        "ip": "::1",
        "сс": ["mummy@ya.ru", "daddy@ya.ru"],
        "bcc": ["grandpa@ya.ru", "grandma@ya.ru"],
        "lid": ["piglid1", "piglid12"],
        "sender_label": ["piglabel1", "piglabel2"],
        "common_label": ["piglabel3", "piglabel4"],
        "personal_labels": {
            "grandpa@ya.ru": ["piglabel5", "piglabel6"],
            "grandma@ya.ru": ["piglabel7", "piglabel8"]
        },
        "notify": "1",
        "save_to_sent": "1",
        "host": "pighost"
    }

    status, _ = await http_client.send_mail(sender, send_mail_args, msg)
    msg = await env.relays.wait_msg(msg_id)

    assert status == 200

    assert_that(
        get_hint_values(msg),
        has_entries(
            label=contains_inanyorder(
                "symbol:encrypted_label",
                "piglabel1",
                "piglabel2",
                "piglabel3",
                "piglabel4",
                "piglabel5",
                "piglabel6",
                "piglabel7",
                "piglabel8"
            ),
            save_to_sent=["1"],
            notify=["1"],
            ipfrom=["::1"],
            host=["pighost"],
            lid=contains_inanyorder("piglid1", "piglid12"),
            bcc=contains_inanyorder("grandma@ya.ru", "grandpa@ya.ru"),
            email=contains_inanyorder("first@yandex.ru", "grandma@ya.ru", "grandpa@ya.ru"),
            filters=["0"],
            skip_loop_prevention=["1"]
        )
    )


async def BadRecipient(nonexistent_sender, rcpt, http_client):
    msg_id, msg = make_message(rcpt, nonexistent_sender.email)

    status, answer = await http_client.send_mail(nonexistent_sender, {"to": [rcpt.email]}, msg)

    assert status == 406
    assert get_error(answer) == "BadRecipient"


async def test_send_spam_mail(sender, rcpt, http_client):
    _, msg = make_message(rcpt, sender)

    with patch.object(so, "get_so_resolution") as patched:
        patched.return_value = "SO_RESOLUTION_SPAM"
        status, answer = await http_client.send_mail(sender, {"to": [rcpt.email]}, msg, "1")
        assert patched.called_once

    assert status == 406
    assert get_error(answer) == "Spam"


async def test_send_personal_spam_with_captcha_header(env, sender, rcpt, http_client):
    msg_id, msg = make_message(rcpt, sender, headers=[("X-Yandex-Captcha-Entered", "yes")])

    with patch.object(so, "get_so_resolution") as patched:
        patched.return_value = "SO_RESOLUTION_SPAM"
        status, answer = await http_client.send_mail(sender, {"to": [rcpt.email]}, msg, "1")
        msg = await env.relays.wait_msg(msg_id)
        assert patched.called_once

    assert status == 200
    assert get_error(answer) == "Success"
    assert msg.mime["X-Yandex-Spam"] == "4"


async def test_send_strongspam_mail(sender, rcpt, http_client):
    msg_id, msg = make_message(rcpt, sender)

    with patch.object(so, "get_so_resolution") as patched:
        patched.return_value = "SO_RESOLUTION_REJECT"
        status, answer = await http_client.send_mail(sender, {"to": [rcpt.email]}, msg, "1")
        assert patched.called_once

    assert status == 406
    assert get_error(answer) == "StrongSpam"


@pytest.mark.mxbackout
@pytest.mark.parametrize("so_type, ban_reason", [
    ("res_url_rbl", "UrlRbl"),
    ("res_rfc_fail", "RfcFail"),
    ("res_bad_karma", "BadKarma"),
    ("res_mail_limits", "MailLimits"),
    ("res_pdd_admin_karma", "PddAdminKarma"),
    ("res_spam_compl", "SpamCompl"),
    ("res_bounces", "Bounces"),
], ids=["url_rbl", "rfc_fail", "bad_karma", "mail_limits", "pdd_admin_karma", "spam_compl", "bounces"])
async def test_send_strongspam_with_ban_reason(sender, rcpt, http_client, so_type, ban_reason):
    msg_id, msg = make_message(rcpt, sender)

    with patch.object(so, "get_so_resolution") as patched_resolution, \
         patch.object(so, "get_so_types") as patched_so_types:

        patched_resolution.return_value = "SO_RESOLUTION_REJECT"
        patched_so_types.return_value = [so_type]
        status, answer = await http_client.send_mail(sender, {"to": [rcpt.email]}, msg, "1")
        assert patched_resolution.called_once
        assert patched_so_types.called_once

    assert status == 406
    assert get_error(answer) == "StrongSpam"
    assert get_ban_reason(answer) == ban_reason

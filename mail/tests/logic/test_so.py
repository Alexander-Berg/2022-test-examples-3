import pytest

from base64 import b64decode
from hamcrest import assert_that, equal_to, contains_inanyorder, has_items

from unittest.mock import patch

from mail.nwsmtp.tests.lib.default_conf import make_conf
from mail.nwsmtp.tests.lib.env import make_env
from mail.nwsmtp.tests.lib.stubs import so
from mail.nwsmtp.tests.lib.util import make_plain_message
from mail.so.api.so_api_pb2 import EmailType
from aiosmtplib.errors import SMTPDataError


def set_trust_headers(conf):
    conf.nwsmtp.message_processing.trust_headers = []


def get_personal_so_hint_value(msg):
    raw_headers = [b64decode(value).decode("utf-8").split("\n") for header, value in msg.mime.items() if header == "X-Yandex-Hint"]
    return {value[0].split("=")[1]: value[:-1] for value in raw_headers if "replace_so_labels=1" in value}


def get_hint_value(msg):
    raw_headers = [b64decode(value).decode("utf-8").split("\n")
                   for header, value in msg.mime.items() if header == "X-Yandex-Hint"]
    return [value[:-1] for value in raw_headers if "replace_so_labels=1" not in value]


@pytest.mark.mxbackout
@pytest.mark.mxfront
@pytest.mark.smtp
@pytest.mark.yaback
async def test_remove_internal_headers(cluster, users, env, sender, rcpt):
    internal_headers = [item["__text"] for item in env.nwsmtp.conf.nwsmtp.so.internal_headers]
    headers = [[internal_header, "excluded"] for internal_header in internal_headers]
    _, msg = make_plain_message(sender, rcpt, text="body", headers=headers)

    with make_conf(cluster, customize_with=set_trust_headers) as conf:
        async with make_env(cluster, users, conf) as env:
            client = await env.nwsmtp.get_client()
            if env.nwsmtp.is_auth_required():
                await client.login(sender.email, sender.passwd)
            await client.send_message(msg)
            so_message = so.get_so_cluster(env).get_message()
            assert so_message

    for k, v in headers:
        header = f"{k}: {v}\r\n"
        assert bytes(header, "utf-8") not in so_message


@pytest.mark.mxbackout
@pytest.mark.mxfront
@pytest.mark.yaback
@pytest.mark.smtp
@pytest.mark.smtpcorp
async def test_reject_for_send_strongspam_mail(env, sender, rcpt):
    _, msg = make_plain_message(sender, rcpt)

    with patch.object(so, "get_so_resolution") as patched:
        patched.return_value = "SO_RESOLUTION_REJECT"
        client = await env.nwsmtp.get_client()
        if env.nwsmtp.is_auth_required():
            await client.login(sender.email, sender.passwd)
        with pytest.raises(SMTPDataError) as exc:
            await client.send_message(msg)
        assert patched.called_once

    assert "554" in str(exc)


@pytest.mark.mxbackout
@pytest.mark.smtp
@pytest.mark.smtpcorp
async def test_reject_for_send_spam_mail(env, sender, rcpt):
    _, msg = make_plain_message(sender, rcpt)

    with patch.object(so, "get_so_resolution") as patched:
        patched.return_value = "SO_RESOLUTION_SPAM"
        client = await env.nwsmtp.get_client()
        if env.nwsmtp.is_auth_required():
            await client.login(sender.email, sender.passwd)
        with pytest.raises(SMTPDataError) as exc:
            await client.send_message(msg)
        assert patched.called_once

    assert "554" in str(exc)


@pytest.mark.mxfront
@pytest.mark.yaback
async def test_mark_for_send_spam_mail(env, sender, rcpt):
    _, msg = make_plain_message(sender, rcpt)

    with patch.object(so, "get_so_resolution") as patched:
        patched.return_value = "SO_RESOLUTION_SPAM"
        client = await env.nwsmtp.get_client()
        if env.nwsmtp.is_auth_required():
            await client.login(sender.email, sender.passwd)
        code, reply = await client.send_message(msg)
        assert patched.called_once

    assert not code
    assert "2.0.0 Ok: queued on " in reply


@pytest.mark.smtp
async def test_spam_request(env, sender, rcpt, big_ml):
    _, msg = make_plain_message(sender, rcpt)

    client = await env.nwsmtp.get_client()
    if env.nwsmtp.is_auth_required():
        await client.login(sender.email, sender.passwd)
    code, reply = await client.send_message(msg, sender.email, [rcpt.email, big_ml.email])
    so_request = so.get_so_cluster(env).get_request()

    assert not code
    assert "2.0.0 Ok: queued on " in reply

    so_envelope = so_request.smtp_envelope
    so_mail_from = so_envelope.mail_from.address.email
    so_rcpts = [rcpt.address.email for rcpt in so_envelope.recipients]
    so_ml_rcpts = [rcpt.address.email for rcpt in so_envelope.recipients if rcpt.is_maillist]
    so_email_type = so_envelope.email_type

    assert_that(so_mail_from, equal_to(sender.email))
    assert_that(so_rcpts, contains_inanyorder(rcpt.email, *big_ml.subscribers.keys()))
    assert_that(so_ml_rcpts, contains_inanyorder(*big_ml.subscribers.keys()))
    assert_that(so_email_type, EmailType.EMAIL_TYPE_REGULAR)


@pytest.mark.smtp
async def test_spam_response(env, sender, rcpt, big_ml):
    msg_id, msg = make_plain_message(sender, rcpt)

    with patch.object(so, "get_so_types") as patched_so_types, \
         patch.object(so, "make_out_parameters") as patched_out_parameters, \
         patch.object(so, "make_peronal_resolutions") as patched_peronal_resolutions:

        patched_so_types.return_value = ["firstmail", "news"]
        patched_out_parameters.return_value = {"forward_type": "FORWARD_TYPE_MXBACK"}
        patched_peronal_resolutions.return_value = [{"uid": rcpt.uid, "resolution": "SO_RESOLUTION_SPAM", "so_classes": ["firstmail", "news"]}]
        client = await env.nwsmtp.get_client()
        if env.nwsmtp.is_auth_required():
            await client.login(sender.email, sender.passwd)
        code, reply = await client.send_message(msg, sender.email, [rcpt.email, big_ml.email])
        out_msg = await env.relays.wait_msg(msg_id)
        assert patched_so_types.called_once
        assert patched_out_parameters.called_once
        assert patched_peronal_resolutions.called_once

    assert not code
    assert "2.0.0 Ok: queued on " in reply

    hint = get_hint_value(out_msg)[1]
    personal_so_hint = get_personal_so_hint_value(out_msg)[rcpt.email]
    assert_that(out_msg.mime["X-Yandex-Spam"], equal_to("1"))
    assert_that(out_msg.mime["X-Yandex-ForeignMX"], equal_to("bla"))
    assert_that(out_msg.mime["X-Yandex-Uid-Status"], equal_to("4 {}".format(rcpt.uid)))
    assert_that(hint, has_items('label=SystMetkaSO:firstmail', 'label=SystMetkaSO:news'))
    assert_that(personal_so_hint, contains_inanyorder('email=second@yandex.ru',
                                                      'replace_so_labels=1',
                                                      'label=SystMetkaSO:firstmail',
                                                      'label=SystMetkaSO:news'))


@pytest.mark.mxfront
async def test_reject_by_inactivity(env, sender, rcpt, big_ml):
    msg_id, msg = make_plain_message(sender, rcpt)

    with patch.object(so, "get_so_types") as patched_so_types, \
         patch.object(so, "get_activity_infos") as patched_so_activity_infos:

        patched_so_types.return_value = ["news", "s_grouponsite"]
        patched_so_activity_infos.return_value = [{"uid": rcpt.uid, "activity_status": 101}]
        client = await env.nwsmtp.get_client()
        if env.nwsmtp.is_auth_required():
            await client.login(sender.email, sender.passwd)
        with pytest.raises(SMTPDataError) as exc:
            await client.send_message(msg, sender.email, [rcpt.email, big_ml.email])
        assert patched_so_types.called_once
        assert patched_so_activity_infos.called_once

    assert "5.2.2 Mailbox size limit exceeded" in str(exc)

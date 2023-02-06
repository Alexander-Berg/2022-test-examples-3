import pytest

from unittest.mock import patch

from mail.nwsmtp.tests.lib.default_conf import make_conf
from mail.nwsmtp.tests.lib.env import make_env
from mail.nwsmtp.tests.lib.stubs import so
from mail.nwsmtp.tests.lib.util import make_plain_message
from aiosmtplib.errors import SMTPDataError


def add_x_yandex_spam_header_to_trust_headers(conf):
    conf.nwsmtp.message_processing.trust_headers = ['x-yandex-spam']
    conf.nwsmtp.so.action = {"malicious": "reject"}


def remove_x_yandex_spam_header_from_trust_headers(conf):
    conf.nwsmtp.message_processing.trust_headers = []


@pytest.mark.mxbackout
@pytest.mark.parametrize("x_yandex_spam_header_value", ["1", "4"])
async def test_trust_x_yandex_spam_header(cluster, users, env, sender, rcpt, x_yandex_spam_header_value):
    _, msg = make_plain_message(sender, rcpt, headers=(
        ("X-Yandex-Spam", x_yandex_spam_header_value),
    ))
    with make_conf(cluster, customize_with=add_x_yandex_spam_header_to_trust_headers) as conf:
        async with make_env(cluster, users, conf) as env:
            with patch.object(so, "get_so_resolution") as patched:
                client = await env.nwsmtp.get_client()
                code, reply = await client.send_message(msg)
                assert patched.not_called

    assert not code
    assert "2.0.0 Ok: queued on " in reply


@pytest.mark.mxbackout
async def test_reject_message_with_malicious_value_x_yandex_spam_header(cluster, users, env, sender, rcpt):
    _, msg = make_plain_message(sender, rcpt, headers=(
        ("X-Yandex-Spam", "256"),
    ))
    with make_conf(cluster, customize_with=add_x_yandex_spam_header_to_trust_headers) as conf:
        async with make_env(cluster, users, conf) as env:
            with patch.object(so, "get_so_resolution") as patched:
                client = await env.nwsmtp.get_client()
                with pytest.raises(SMTPDataError) as exc:
                    await client.send_message(msg)
                assert patched.not_called

    assert "554" in str(exc)


@pytest.mark.mxbackout
async def test_skip_wrong_value_x_yandex_spam_header(cluster, users, env, sender, rcpt):
    _, msg = make_plain_message(sender, rcpt, headers=(
        ("X-Yandex-Spam", "10"),
    ))
    with make_conf(cluster, customize_with=add_x_yandex_spam_header_to_trust_headers) as conf:
        async with make_env(cluster, users, conf) as env:
            with patch.object(so, "get_so_resolution") as patched:
                patched.return_value = "SO_RESOLUTION_ACCEPT"
                client = await env.nwsmtp.get_client()
                code, reply = await client.send_message(msg)
                assert patched.called_once

    assert not code
    assert "2.0.0 Ok: queued on " in reply


@pytest.mark.mxbackout
async def test_not_trust_x_yandex_spam_header(cluster, users, env, sender, rcpt):
    _, msg = make_plain_message(sender, rcpt, headers=(
        ("X-Yandex-Spam", "1"),
    ))
    with make_conf(cluster, customize_with=remove_x_yandex_spam_header_from_trust_headers) as conf:
        async with make_env(cluster, users, conf) as env:
            with patch.object(so, "get_so_resolution") as patched:
                patched.return_value = "SO_RESOLUTION_ACCEPT"
                client = await env.nwsmtp.get_client()
                code, reply = await client.send_message(msg)
                assert patched.called_once

    assert not code
    assert "2.0.0 Ok: queued on " in reply

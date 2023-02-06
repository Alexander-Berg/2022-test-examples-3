import pytest

from unittest.mock import patch

from mail.nwsmtp.tests.lib.util import make_plain_message
from mail.nwsmtp.tests.lib.stubs import avir


@pytest.mark.smtp
@pytest.mark.mxfront
@pytest.mark.mxcorp
async def test_discard_virus(env, sender, rcpt):
    _, msg = make_plain_message(sender, rcpt)

    with patch.object(avir, "is_virus") as patched:
        patched.return_value = True
        client = await env.nwsmtp.get_client()
        if env.nwsmtp.is_auth_required():
            await client.login(sender.email, sender.passwd)
        code, reply = await client.send_message(msg)
        assert patched.called_once

    assert not code
    assert "2.0.0 Ok: queued on" in reply


@pytest.mark.mxbackout
async def test_error_response_avir(env, sender, rcpt):
    _, msg = make_plain_message(sender, rcpt)

    with patch.object(avir, "is_error") as patched:
        patched.return_value = True
        client = await env.nwsmtp.get_client()
        code, reply = await client.send_message(msg)
        assert patched.called_once

    assert not code
    assert "2.0.0 Ok: queued on" in reply


@pytest.mark.mxbackcorp
@pytest.mark.smtpcorp
@pytest.mark.yaback
async def test_send_virus(env, sender, rcpt):
    _, msg = make_plain_message(sender, rcpt)

    with patch.object(avir, "is_virus") as patched:
        patched.return_value = True
        client = await env.nwsmtp.get_client()
        if env.nwsmtp.is_auth_required():
            await client.login(sender.email, sender.passwd)
        code, reply = await client.send_message(msg)
        assert patched.not_called

    assert not code
    assert "2.0.0 Ok: queued on" in reply

import sys
import pytest
from unittest.mock import patch

from aiosmtplib import SMTPResponseException

from mail.nwsmtp.tests.lib import CLUSTERS
from mail.nwsmtp.tests.lib.env import make_env, get_env
from mail.nwsmtp.tests.lib.stubs.blackbox import Blackbox
from mail.nwsmtp.tests.lib.util import make_plain_message


@pytest.mark.cluster(CLUSTERS)
async def test_make_env(cluster, users, sender, rcpt):
    async with make_env(cluster, users) as env:
        client = await env.nwsmtp.get_client()
        if env.nwsmtp.is_auth_required():
            await client.login(sender.email, sender.passwd)

        msg_id, msg = make_plain_message(sender, rcpt)
        code, reply = await client.send_message(msg)
        assert not code
        assert "2.0.0 Ok: queued on " in reply

        msg = await env.relays.wait_msg(msg_id)
        assert msg.msg_id == msg_id


@pytest.mark.mxbackout
async def test_get_env(env, sender, rcpt):
    client = await env.nwsmtp.get_client()
    _, msg = make_plain_message(sender, rcpt)
    code, reply = await client.send_message(msg)
    assert "2.0.0 Ok: queued on " in reply


async def test_get_env_is_caching(users):
    async with get_env("mxbackout", users) as env:
        first_ids = id(env), id(env.nwsmtp)
    async with get_env("mxbackout", users) as env:
        second_ids = id(env), id(env.nwsmtp)

    assert first_ids == second_ids


class MyExc(RuntimeError):
    pass


async def raise_my_exception(*args, **kwargs):
    raise MyExc("MyExc expected")


async def test_get_env_raises_when_stub_failed(users):
    if sys.version_info >= (3, 8):
        side_effect = MyExc("MyExc expected")
    else:
        side_effect = raise_my_exception
    with patch.object(Blackbox, "start", side_effect=side_effect):
        with pytest.raises(Exception) as exc_info:
            async with get_env("mxbackout", users):
                pass

        assert isinstance(exc_info.value, MyExc)

    # now get_env renewed the stub
    async with get_env("mxbackout", users):
        pass


async def respond_404(*args, **kwargs):
    import aiohttp
    return aiohttp.web.Response(status=404)


@pytest.mark.mxbackout
async def test_get_env_mocked_blackbox_response(env, sender, rcpt):
    client = await env.nwsmtp.get_client()
    msg_id, msg = make_plain_message(sender, rcpt)

    with patch("mail.nwsmtp.tests.lib.stubs.blackbox.handle_user_info",
               return_value=respond_404()) as mock:
        with pytest.raises(SMTPResponseException) as exc:
            await client.send_message(msg)

        assert exc.value.code == 454
        mock.assert_called()

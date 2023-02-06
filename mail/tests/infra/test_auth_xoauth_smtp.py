from collections import namedtuple
from base64 import b64encode
from aiosmtplib.errors import SMTPSenderRefused
from mail.nwsmtp.tests.lib.util import make_plain_message

import pytest

Case = namedtuple("Case", ["auth_xoauth_pattern", "expected_code", "expected_response"])
SIMPLE_MSG = "Subject:cat\r\n\r\n\r\nqwerty"


@pytest.mark.smtp
@pytest.mark.parametrize("case", [
    Case("user={auth_login}\1auth=Bearer {auth_token}\1\1", 235, "2.7.0 Authentication successful."),
    Case("user={auth_login}\1auth=Bearer {auth_token}pepper\1\1", 535, "5.7.8 Error: authentication failed: Invalid user or password!"),
    Case("user\1authbearer\1", 535, "5.7.8 Error: authentication failed: Invalid format.")])
async def test_auth_xoauth(env, case, prod_sender):
    env.stubs.blackbox.set_allow_only_login(True)
    env.stubs.blackbox.set_check_password_on_login(True)

    client = await env.nwsmtp.get_client()
    await client._ehlo_or_helo_if_needed()

    auth_cmd = b"AUTH XOAUTH2 " + b64encode(case.auth_xoauth_pattern.format(auth_login=prod_sender.login, auth_token=prod_sender.token).encode())
    code, msg = await client.execute_command(auth_cmd)

    assert code == case.expected_code
    assert case.expected_response in msg


@pytest.mark.smtp
@pytest.mark.parametrize("case", [
    Case("user={auth_login}\1auth=Bearer {auth_token}\1\1", 235, "2.7.0 Authentication successful."),
    Case("user={auth_login}\1auth=Bearer {auth_token}pepper\1\1", 535, "5.7.8 Error: authentication failed: Invalid user or password!"),
    Case("user\1authbearer\1", 535, "5.7.8 Error: authentication failed: Invalid format.")])
async def test_auth_xoauth_two_steps(env, case, prod_sender):
    env.stubs.blackbox.set_allow_only_login(True)
    env.stubs.blackbox.set_check_password_on_login(True)

    client = await env.nwsmtp.get_client()
    await client._ehlo_or_helo_if_needed()

    auth_cmd = b"AUTH XOAUTH2 "
    code, msg = await client.execute_command(auth_cmd)
    assert code == 334

    auth_cmd = b64encode(case.auth_xoauth_pattern.format(auth_login=prod_sender.login, auth_token=prod_sender.token).encode())
    code, msg = await client.execute_command(auth_cmd)

    assert code == case.expected_code
    assert case.expected_response in msg


@pytest.mark.smtp
async def test_auth_xoauth_cancellation(env, prod_sender):
    env.stubs.blackbox.set_allow_only_login(True)
    env.stubs.blackbox.set_check_password_on_login(True)

    client = await env.nwsmtp.get_client()
    await client._ehlo_or_helo_if_needed()

    auth_cmd = b"AUTH XOAUTH2 "
    code, msg = await client.execute_command(auth_cmd)
    assert code == 334

    auth_cmd = b"*"
    code, msg = await client.execute_command(auth_cmd)

    assert 501 == code
    assert "Cancelled." in msg


@pytest.mark.smtp
async def test_delivery_after_successful_auth_xoauth(env, prod_sender, prod_rcpt):
    env.stubs.blackbox.set_allow_only_login(True)
    env.stubs.blackbox.set_check_password_on_login(True)

    client = await env.nwsmtp.get_client()
    await client._ehlo_or_helo_if_needed()

    auth_cmd = b"AUTH XOAUTH2 " + b64encode("user={auth_login}\1auth=Bearer {auth_token}\1\1".format(auth_login=prod_sender.login, auth_token=prod_sender.token).encode())
    code, msg = await client.execute_command(auth_cmd)

    assert code == 235
    assert "2.7.0 Authentication successful." in msg

    _, msg = make_plain_message(prod_sender, prod_rcpt, "test message", "test subject")
    code, reply = await client.send_message(msg)
    assert not code
    assert "2.0.0 Ok: queued on " in reply


@pytest.mark.smtp
async def test_nodelivery_after_successful_auth_xoauth_but_send_from_another(env, prod_sender, prod_rcpt):
    env.stubs.blackbox.set_allow_only_login(True)
    env.stubs.blackbox.set_check_password_on_login(True)

    client = await env.nwsmtp.get_client()
    await client._ehlo_or_helo_if_needed()

    auth_cmd = b"AUTH XOAUTH2 " + b64encode("user={auth_login}\1auth=Bearer {auth_token}\1\1".format(auth_login=prod_sender.login, auth_token=prod_sender.token).encode())
    code, msg = await client.execute_command(auth_cmd)

    assert code == 235
    assert "2.7.0 Authentication successful." in msg

    _, msg = make_plain_message(prod_rcpt, prod_sender, "test message", "test subject")
    try:
        await client.send_message(msg)
    except SMTPSenderRefused as err:
        assert err.code == 553
        assert "Sender address rejected: not owned by auth user." in err.message


@pytest.mark.smtp
async def test_nodelivery_after_invalid_auth_xoauth(env, prod_sender, prod_rcpt):
    env.stubs.blackbox.set_allow_only_login(True)
    env.stubs.blackbox.set_check_password_on_login(True)

    client = await env.nwsmtp.get_client()
    await client._ehlo_or_helo_if_needed()

    auth_cmd = b"AUTH XOAUTH2 " + b64encode("user={auth_login}\1auth=Bearer {auth_token}pepper\1\1".format(auth_login=prod_sender.login, auth_token=prod_sender.token).encode())
    code, msg = await client.execute_command(auth_cmd)

    assert code == 535
    assert "5.7.8 Error: authentication failed: Invalid user or password!" in msg

    _, msg = make_plain_message(prod_rcpt, prod_sender, "test message", "test subject")
    try:
        await client.send_message(msg)
    except SMTPSenderRefused as err:
        assert err.code == 503
        assert "5.5.4 Error: send AUTH command first." in err.message

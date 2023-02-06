from collections import namedtuple
from base64 import b64encode
from aiosmtplib.errors import SMTPSenderRefused
from mail.nwsmtp.tests.lib.util import make_plain_message

import pytest

Case = namedtuple("Case", ["pwd_papper", "expected_code", "expected_response"])
SIMPLE_MSG = "Subject:cat\r\n\r\n\r\nqwerty"


@pytest.mark.smtp
@pytest.mark.parametrize("case", [
    Case("", 235, "2.7.0 Authentication successful."),
    Case("pepper", 535, "5.7.8 Error: authentication failed: Invalid user or password!")])
async def test_auth_login(env, case, prod_sender):
    env.stubs.blackbox.set_allow_only_login(True)
    env.stubs.blackbox.set_check_password_on_login(True)

    client = await env.nwsmtp.get_client()
    await client._ehlo_or_helo_if_needed()

    auth_cmd = b"AUTH LOGIN " + b64encode(prod_sender.login.encode())
    code, msg = await client.execute_command(auth_cmd)

    passwd = b64encode((prod_sender.passwd + case.pwd_papper).encode())
    code, msg = await client.execute_command(passwd)

    assert code == case.expected_code
    assert case.expected_response in msg


@pytest.mark.smtp
@pytest.mark.parametrize("case", [
    Case("", 235, "2.7.0 Authentication successful."),
    Case("pepper", 535, "5.7.8 Error: authentication failed: Invalid user or password!")])
async def test_auth_login_three_steps(env, case, prod_sender):
    env.stubs.blackbox.set_allow_only_login(True)
    env.stubs.blackbox.set_check_password_on_login(True)

    client = await env.nwsmtp.get_client()
    await client._ehlo_or_helo_if_needed()

    auth_cmd = b"AUTH LOGIN"
    code, msg = await client.execute_command(auth_cmd)
    assert code == 334

    auth_cmd = b64encode(prod_sender.login.encode())
    code, msg = await client.execute_command(auth_cmd)
    assert code == 334

    passwd = b64encode((prod_sender.passwd + case.pwd_papper).encode())
    code, msg = await client.execute_command(passwd)

    assert code == case.expected_code
    assert case.expected_response in msg


@pytest.mark.smtp
async def test_auth_login_cancellation(env, prod_sender):
    env.stubs.blackbox.set_allow_only_login(True)
    env.stubs.blackbox.set_check_password_on_login(True)

    client = await env.nwsmtp.get_client()
    await client._ehlo_or_helo_if_needed()

    auth_cmd = b"AUTH LOGIN"
    code, msg = await client.execute_command(auth_cmd)
    assert code == 334

    auth_cmd = b"*"
    code, msg = await client.execute_command(auth_cmd)
    assert code == 501
    assert "Cancelled." in msg


@pytest.mark.smtp
async def test_delivery_after_successful_auth_login(env, prod_sender, prod_rcpt):
    env.stubs.blackbox.set_allow_only_login(True)
    env.stubs.blackbox.set_check_password_on_login(True)

    client = await env.nwsmtp.get_client()
    await client._ehlo_or_helo_if_needed()

    auth_cmd = b"AUTH LOGIN " + b64encode(prod_sender.login.encode())
    code, msg = await client.execute_command(auth_cmd)

    passwd = b64encode(prod_sender.passwd.encode())
    code, msg = await client.execute_command(passwd)

    assert code == 235
    assert "2.7.0 Authentication successful." in msg

    _, msg = make_plain_message(prod_sender, prod_rcpt, "test message", "test subject")
    code, reply = await client.send_message(msg)
    assert not code
    assert "2.0.0 Ok: queued on " in reply


@pytest.mark.smtp
async def test_nodelivery_after_successful_auth_login_but_send_from_another(env, prod_sender, prod_rcpt):
    env.stubs.blackbox.set_allow_only_login(True)
    env.stubs.blackbox.set_check_password_on_login(True)

    client = await env.nwsmtp.get_client()
    await client._ehlo_or_helo_if_needed()

    auth_cmd = b"AUTH LOGIN " + b64encode(prod_sender.login.encode())
    code, msg = await client.execute_command(auth_cmd)

    passwd = b64encode(prod_sender.passwd.encode())
    code, msg = await client.execute_command(passwd)

    assert code == 235
    assert "2.7.0 Authentication successful." in msg

    _, msg = make_plain_message(prod_rcpt, prod_sender, "test message", "test subject")
    try:
        await client.send_message(msg)
    except SMTPSenderRefused as err:
        assert err.code == 553
        assert "Sender address rejected: not owned by auth user." in err.message


@pytest.mark.smtp
async def test_nodelivery_after_invalid_auth_login(env, prod_sender, prod_rcpt):
    env.stubs.blackbox.set_allow_only_login(True)
    env.stubs.blackbox.set_check_password_on_login(True)

    client = await env.nwsmtp.get_client()
    await client._ehlo_or_helo_if_needed()

    auth_cmd = b"AUTH LOGIN " + b64encode(prod_sender.login.encode())
    code, msg = await client.execute_command(auth_cmd)

    passwd = b64encode((prod_sender.passwd + "pepper").encode())
    code, msg = await client.execute_command(passwd)

    assert code == 535
    assert "5.7.8 Error: authentication failed: Invalid user or password!" in msg

    _, msg = make_plain_message(prod_rcpt, prod_sender, "test message", "test subject")
    try:
        await client.send_message(msg)
    except SMTPSenderRefused as err:
        assert err.code == 503
        assert "5.5.4 Error: send AUTH command first." in err.message

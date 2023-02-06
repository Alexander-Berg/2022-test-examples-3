import pytest


@pytest.mark.smtp
async def test_auth_invalid_method(env):
    client = await env.nwsmtp.get_client()
    await client._ehlo_or_helo_if_needed()

    auth_cmd = b"AUTH WEIRDMETHOD"
    code, msg = await client.execute_command(auth_cmd)

    assert code == 535
    assert "5.7.8 Error: Method is not supported." in msg

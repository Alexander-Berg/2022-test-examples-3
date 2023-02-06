import pytest


@pytest.mark.mxbackout
@pytest.mark.mxbackcorp
async def test_ping(env):
    client = env.nwsmtp.get_http_client()
    status, body = await client.fetch("/ping")
    assert status == 200
    assert body == b"pong"

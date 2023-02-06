import pytest


@pytest.mark.asyncio
async def test_ping(payments_client):
    r = await payments_client.get('/ping')
    assert await r.read() == b'pong'

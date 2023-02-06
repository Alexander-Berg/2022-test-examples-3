import pytest


@pytest.mark.asyncio
async def test_ping(client):
    r = await client.get('/ping')
    assert await r.text() == 'pong'

import pytest

from travel.rasp.pathfinder_maps.tests.utils import HandlerFactory


@pytest.mark.asyncio
async def test_ping():
    handler = HandlerFactory()
    res = await handler.ping(None)
    assert res.status == 200

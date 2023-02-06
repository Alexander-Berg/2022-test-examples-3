import pytest

from travel.rasp.pathfinder_maps.services.geobase_service import GeobaseService
from travel.rasp.pathfinder_maps.tests.utils.fixtures import geobase_client  # noqa: F401
from travel.rasp.pathfinder_maps.utils import RoutePoint


@pytest.mark.asyncio
async def test_geobase_service(geobase_client):  # noqa: F811
    geobase_service = GeobaseService(geobase_client)
    assert geobase_service.get_settlement_by_location(RoutePoint(37.622504, 55.753215)) == 213

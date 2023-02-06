import pytest

from travel.rasp.pathfinder_maps.const import TTYPE
from travel.rasp.pathfinder_maps.services.geobase_service import GeobaseService
from travel.rasp.pathfinder_maps.services.nearest_point_finder import NearestPointFinder
from travel.rasp.pathfinder_maps.tests.utils.fixtures import geobase_client, protobuf_data_provider  # noqa: F401
from travel.rasp.pathfinder_maps.utils import RoutePoint


def init_npf(data_provider, geobase_client):  # noqa: F811
    geobase_service = GeobaseService(geobase_client)
    npf = NearestPointFinder(data_provider, geobase_service)
    npf.prepare()
    return npf


@pytest.mark.asyncio
async def test_nearest_point_finder_prepare(protobuf_data_provider, geobase_client):  # noqa: F811
    npf = init_npf(protobuf_data_provider, geobase_client)

    reference_db_coords = {
        TTYPE.aero.value: [(37.896818, 55.415133), (60.804833, 56.750107)],
        TTYPE.bus.value: [(37.459250812, 55.5702823427), (60.606374, 56.858276)],
        TTYPE.train.value: [(37.640771, 55.729498), (60.606052, 56.858761), (45.998115, 51.542009)]
    }
    reference_db_points = {
        TTYPE.aero.value: [('station', 9600216), ('station', 9600370)],
        TTYPE.bus.value: [('station', 9873805), ('station', 9655216)],
        TTYPE.train.value: [('station', 2000005), ('station', 9607404), ('station', 9623135)]
    }

    for k in [TTYPE.aero.value, TTYPE.bus.value, TTYPE.train.value]:
        coords, points = npf._db[k]
        assert coords == reference_db_coords[k]
        assert points == reference_db_points[k]

    reference_settlement_coords = [(60.605514, 56.838607), (46.03582, 51.531528), (37.619899, 55.753676)]
    reference_settlement_points = [('settlement', 54), ('settlement', 194), ('settlement', 213)]

    assert npf._settlement_coords == reference_settlement_coords
    assert npf._settlement_points == reference_settlement_points


@pytest.mark.asyncio
async def test_nearest_point_finder_get_settlement_from_geobase(protobuf_data_provider, geobase_client):  # noqa: F811
    npf = init_npf(protobuf_data_provider, geobase_client)
    assert await npf.get_settlement(RoutePoint(37.622504, 55.753215)) == ('settlement', 213)


@pytest.mark.asyncio
async def test_nearest_point_finder_get_settlement_from_protobufs(protobuf_data_provider, geobase_client):  # noqa: F811
    npf = init_npf(protobuf_data_provider, geobase_client)
    assert await npf.get_settlement(RoutePoint(60.597465, 56.838011)) == ('settlement', 54)


@pytest.mark.asyncio
async def test_nearest_point_finder_get_stations_by_ttype(protobuf_data_provider, geobase_client):  # noqa: F811
    npf = init_npf(protobuf_data_provider, geobase_client)

    reference_coords = {
        TTYPE.aero: [RoutePoint(60.804833, 56.750107)],
        TTYPE.bus: [RoutePoint(60.606374, 56.858276)],
        TTYPE.train: [RoutePoint(60.606052, 56.858761)]
    }
    reference_points = {
        TTYPE.aero: [('station', 9600370)],
        TTYPE.bus: [('station', 9655216)],
        TTYPE.train: [('station', 9607404)]
    }
    coords, points = await npf.get_stations_by_ttype(RoutePoint(60.597465, 56.838011))

    assert reference_coords == coords
    assert reference_points == points

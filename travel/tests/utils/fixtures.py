from datetime import datetime

import numpy as np
import pytest

from travel.rasp.pathfinder_maps.clients.geobase_client import GeobaseClientStub
from travel.rasp.pathfinder_maps.const import UTC_TZ
from travel.rasp.pathfinder_maps.models.route import Route
from travel.rasp.pathfinder_maps.protos.builders import (
    build_pathfinder_section, build_railway_stop, build_railway_polyline, build_stop_protobuf, build_wait_section
)
from travel.rasp.pathfinder_maps.services.data_provider import ProtobufDataProvider, PickleDataProvider
from travel.rasp.pathfinder_maps.utils import RoutePoint

from travel.rasp.pathfinder_maps.tests.utils import create_settlement, create_station, MapsClientStub
from travel.rasp.pathfinder_maps.tests.utils.protobufs import (
    create_settlement_proto, create_station_proto, create_timezone_proto
)


@pytest.fixture
def geobase_client():
    data = {
        'region_55.753215_37.622504': 120540,
        'parents_120540': [120540, 20279, 213, 1, 3, 225, 10001, 10000],
        'region_120540': {'type': 13},
        'region_20279': {'type': 8},
        'region_213': {'type': 6},
        'region_1': {'type': 5},
        'region_3': {'type': 4},
        'region_225': {'type': 3},
        'region_10001': {'type': 1},
        'region_10000': {'type': 0}
    }
    return GeobaseClientStub(data)


@pytest.fixture
def protobuf_data_provider():
    protobuf_data_provider = ProtobufDataProvider()

    stations = [
        (2000005, 225, 'Москва (Павелецкий вокзал)', 0, 213, 37.640771, 55.729498, 1, 1, 'tablo,train,suburban,aeroex'),
        (9607404, 225, 'Екатеринбург-Пасс.', 5, 54, 60.606052, 56.858761, 1, 1, 'tablo,train,suburban'),
        (9623135, 225, 'Саратов-1-Пасс.', 25, 194, 45.998115, 51.542009, 1, 1, 'tablo,train,suburban'),

        (9600216, 225, 'Домодедово', 0, 213, 37.896818, 55.415133, 2, 2, 'tablo,suburban,aeroex'),
        (9600370, 225, 'Кольцово', 5, 54, 60.804833, 56.750107, 2, 2, 'tablo,suburban'),

        (9873805, 225, 'м. Ольховая', 0, 213, 37.459250812, 55.5702823427, 3, 2, 'shedule'),
        (9655216, 225, 'Площадь ж/д вокзала', 5, 54, 60.606374, 56.858276, 3, 2, 'shedule')
    ]
    for station in stations:
        protobuf_data_provider.station_repo.add(create_station_proto(*station).SerializeToString())

    timezones = [
        (0, 'Europe/Moscow'),
        (5, 'Asia/Yekaterinburg'),
        (25, 'Europe/Saratov')
    ]
    for timezone in timezones:
        protobuf_data_provider.timezone_repo.add(create_timezone_proto(*timezone).SerializeToString())

    settlements = [
        (54, 'Екатеринбург', 60.605514, 56.838607, 2),
        (194, 'Саратов', 46.03582, 51.531528, 2),
        (213, 'Москва', 37.619899, 55.753676, 1)
    ]
    for settlement in settlements:
        protobuf_data_provider.settlement_repo.add(create_settlement_proto(*settlement).SerializeToString())

    return protobuf_data_provider


@pytest.fixture
def pickle_data_provider():
    pdp = PickleDataProvider(None)
    railways = [
        ((2000005, 9623135, '047Й'), [[3764077100000, 5572949800000], [835734400000, -418748900000]]),
        ((9623135, 9607404, '105Ж'), [[4599811500000, 5154200900000], [1460793700000, 531675200000]])
    ]
    for k, v in railways:
        v = [(np.array(v)).reshape(4).astype(np.int64).tobytes().decode('latin1')]
        pdp.railway_geometry.add(k, v)

    return pdp


@pytest.fixture
def maps_client():
    data = {}
    return MapsClientStub(data)


@pytest.fixture
def moscow_station():
    return create_station(
        2000005, 'Москва (Павелецкий вокзал)', 37.640771, 55.729498, 'Europe/Moscow',
        create_settlement(213, 'Москва')
    )


@pytest.fixture
def saratov_station():
    return create_station(
        9623135, 'Саратов-1-Пасс.', 45.998115, 51.542009, 'Europe/Saratov',
        create_settlement(194, 'Саратов')
    )


@pytest.fixture
def ekb_station():
    return create_station(
        9607404, 'Екатеринбург-Пасс.', 60.606052, 56.858761, 'Asia/Yekaterinburg',
        create_settlement(54, 'Екатеринбург')
    )


@pytest.fixture
def pathfinder_protobuf(moscow_station, saratov_station, ekb_station):
    moscow_point = RoutePoint(37.640771, 55.729498)
    saratov_point = RoutePoint(45.998115, 51.542009)
    ekb_point = RoutePoint(60.606052, 56.858761)

    route_1_departure = datetime(2020, 2, 8, 10, 44, tzinfo=UTC_TZ)
    route_1_arrival = datetime(2020, 2, 9, 2, 29, tzinfo=UTC_TZ)
    route_3_departure = datetime(2020, 2, 9, 4, 0, tzinfo=UTC_TZ)
    route_3_arrival = datetime(2020, 2, 10, 11, 20, tzinfo=UTC_TZ)

    moscow_station_pb = create_station_proto(2000005, 225, 'Москва (Павелецкий вокзал)', 0, 213, 37.640771, 55.729498, 1, 1, 'tablo,train,suburban,aeroex')
    ekb_station_pb = create_station_proto(9607404, 225, 'Екатеринбург-Пасс.', 5, 54, 60.606052, 56.858761, 1, 1, 'tablo,train,suburban')
    saratov_station_pb = create_station_proto(9623135, 225, 'Саратов-1-Пасс.', 25, 194, 45.998115, 51.542009, 1, 1, 'tablo,train,suburban')

    thread_1 = 578026383, b'047\xd0\x99', 'train', 2
    thread_2 = 597522912, b'105\xd0\x96', 'train', 2

    route_1 = Route('047J_0_2', route_1_departure, 2000005, route_1_arrival, 9623135)
    route_1.departure_station = moscow_station
    route_1.arrival_station = saratov_station
    route_1.thread_info = (578026383, '047Й', 1, (2000005, 9623135), [2000005, 9623135])

    route_3 = Route('105ZH_1_2', route_3_departure, 9623135, route_3_arrival, 9607404)
    route_3.departure_station = saratov_station
    route_3.arrival_station = ekb_station
    route_3.thread_info = (597522912, '105Ж', 1, (9623135, 9607404), [9623135, 9607404])

    sections = [
        build_stop_protobuf(moscow_point, 2000005, 'Москва (Павелецкий вокзал)'),
        build_pathfinder_section(
            'rasp_link',
            route_1,
            [
                build_railway_stop(moscow_station_pb),
                build_railway_polyline([moscow_point, saratov_point]),
                build_railway_stop(saratov_station_pb)
            ],
            *thread_1
        ),
        build_stop_protobuf(saratov_point, 9623135, 'Саратов-1-Пасс.'),
        build_wait_section((route_3_departure - route_1_arrival).total_seconds()),
        build_pathfinder_section(
            'rasp_link',
            route_3,
            [
                build_railway_stop(saratov_station_pb),
                build_railway_polyline([saratov_point, ekb_point]),
                build_railway_stop(ekb_station_pb)
            ],
            *thread_2
        ),
        build_stop_protobuf(ekb_point, 9607404, 'Екатеринбург-Пасс.')
    ]
    return [moscow_point, saratov_point, ekb_point], sections

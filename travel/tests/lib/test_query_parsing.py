# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

import pytest
from django.http.request import QueryDict

from common.tester.factories import create_country, create_settlement, create_station
from travel.rasp.wizards.proxy_api.lib.query_parsing import (
    NoContent, get_direction_query, get_general_query, get_route_query, get_station_query
)
from travel.rasp.wizards.proxy_api.lib.station.settlement_stations_cache import SettlementStationsCache
from travel.rasp.wizards.proxy_api.lib.tests_utils import (
    create_indexed_points, make_direction_query, make_general_query, make_plane_station_query,
    make_suburban_station_query
)
from travel.rasp.wizards.wizard_lib.experiment_flags import ExperimentFlag
from travel.rasp.wizards.wizard_lib.serialization.thread_express_type import ThreadExpressType

pytestmark = pytest.mark.dbuser
create_settlement = create_settlement.mutate(type_choices='bus,suburban')
create_station = create_station.mutate(t_type='suburban')


def test_get_direction_query_blank():
    assert get_direction_query(QueryDict('foo=1&bar=baz'), frozenset()) is None


@pytest.mark.parametrize('params, point_factories', (
    # direction points are not found
    ('point=unknown_point&point_to=unknown_point', ()),
    ('departure_settlement_geoid=999&arrival_settlement_geoid=999', ()),

    # some of direction points are countries
    ('point=departure_point&point_to=arrival_country', (
        create_settlement.mutate(title_en='departure_point'), create_country.mutate(title_en='arrival_country'),
    )),
    ('point=departure_country&point_to=arrival_point', (
        create_country.mutate(title_en='departure_country'), create_settlement.mutate(title_en='arrival_point'),
    )),
    ('point=departure_country&point_to=arrival_country', (
        create_country.mutate(title_en='departure_country'), create_country.mutate(title_en='arrival_country'),
    )),

    # an airport is not found
    ('point=unknown_airport&transport=suburban&query=aeroexpress', ()),

    # found an airport without a settlement
    ('point=airport&transport=suburban&query=aeroexpress', (
        create_station.mutate(title_en='airport', t_type='plane'),
    )),
))
def test_get_direction_query_nocontent(params, point_factories):
    create_indexed_points(*point_factories)
    experiment_flags = frozenset([ExperimentFlag.ENABLE_DIRECTION_WIZARD])

    with pytest.raises(NoContent):
        get_direction_query(QueryDict(params), experiment_flags)


@pytest.mark.parametrize('params, point_factories, expected', (
    # point name search
    (
        'point=departure_point&point_to=arrival_point&exp_flags=RASPWIZARDS-ENABLE-DIRECTION',
        (create_settlement.mutate(title_en='departure_point'), create_settlement.mutate(title_en='arrival_point')),
        make_direction_query(experiment_flags = frozenset([ExperimentFlag.ENABLE_DIRECTION_WIZARD]))
    ),
    # geoid search
    (
        'departure_settlement_geoid=1&arrival_settlement_geoid=2&exp_flags=RASPWIZARDS-ENABLE-DIRECTION',
        (create_settlement.mutate(_geo_id=1), create_settlement.mutate(_geo_id=2)),
        make_direction_query(experiment_flags = frozenset([ExperimentFlag.ENABLE_DIRECTION_WIZARD]))
    ),
    # transport parameter
    (
        'point=departure_point&point_to=arrival_point&transport=bus&exp_flags=RASPWIZARDS-ENABLE-DIRECTION',
        (create_settlement.mutate(title_en='departure_point'), create_settlement.mutate(title_en='arrival_point')),
        make_direction_query(
            transport_code='bus', experiment_flags = frozenset([ExperimentFlag.ENABLE_DIRECTION_WIZARD])
        )
    ),
    # transport is an empty parameter
    (
        'point=departure_point&point_to=arrival_point&transport=&exp_flags=RASPWIZARDS-ENABLE-DIRECTION',
        (create_settlement.mutate(title_en='departure_point'), create_settlement.mutate(title_en='arrival_point')),
        make_direction_query(experiment_flags = frozenset([ExperimentFlag.ENABLE_DIRECTION_WIZARD]))
    ),
    # 'train' is replaced with 'suburban'
    (
        'point=departure_point&point_to=arrival_point&transport=train&exp_flags=RASPWIZARDS-ENABLE-DIRECTION',
        (create_settlement.mutate(title_en='departure_point'), create_settlement.mutate(title_en='arrival_point')),
        make_direction_query(
            transport_code='suburban', experiment_flags = frozenset([ExperimentFlag.ENABLE_DIRECTION_WIZARD])
        )
    ),

))
def test_get_direction_query_regular(params, point_factories, expected):
    departure_point, arrival_point = create_indexed_points(*point_factories)
    experiment_flags = frozenset([ExperimentFlag.ENABLE_DIRECTION_WIZARD])

    assert get_direction_query(QueryDict(params), experiment_flags) == expected._replace(
        departure_point=departure_point, arrival_point=arrival_point
    )


def test_get_direction_query_aeroexpress():
    (airport,) = create_indexed_points(create_station.mutate(title_en='airport', t_type='plane', settlement={}))
    experiment_flags = frozenset([ExperimentFlag.ENABLE_DIRECTION_WIZARD])

    assert get_direction_query(
        QueryDict('point=airport&transport=suburban&query=aeroexpress&exp_flags=RASPWIZARDS-ENABLE-DIRECTION'),
        experiment_flags
    ) == make_direction_query(
        departure_point=airport.settlement,
        arrival_point=airport,
        experiment_flags = experiment_flags,
        transport_code='suburban',
        thread_express_type=ThreadExpressType.AEROEXPRESS
    )

    bidirectional_aeroexpress_flags = frozenset([
        ExperimentFlag.ENABLE_DIRECTION_WIZARD,
        ExperimentFlag.BIDIRECTIONAL_AEROEXPRESS,
    ])

    assert get_direction_query(
        QueryDict('point=airport&transport=suburban&query=aeroexpress&exp_flags=RASPWIZARDS-ENABLE-DIRECTION'),
        bidirectional_aeroexpress_flags
    ) == make_direction_query(
        departure_point=airport,
        arrival_point=airport.settlement,
        transport_code='suburban',
        thread_express_type=ThreadExpressType.AEROEXPRESS,
        experiment_flags=bidirectional_aeroexpress_flags
    )

    assert get_direction_query(
        QueryDict('point_to=airport&transport=suburban&query=aeroexpress&exp_flags=RASPWIZARDS-ENABLE-DIRECTION'),
        bidirectional_aeroexpress_flags
    ) == make_direction_query(
        departure_point=airport.settlement,
        arrival_point=airport,
        transport_code='suburban',
        thread_express_type=ThreadExpressType.AEROEXPRESS,
        experiment_flags=bidirectional_aeroexpress_flags
    )


def test_get_station_query_blank():
    experiment_flags = frozenset([ExperimentFlag.ENABLE_STATION_WIZARD])
    assert get_station_query(QueryDict('foo=1&bar=baz'), experiment_flags) is None


def test_get_station_query_nocontent():
    experiment_flags = frozenset([ExperimentFlag.ENABLE_STATION_WIZARD])
    with SettlementStationsCache.using_precache(), pytest.raises(NoContent):
        get_station_query(QueryDict('point=unknown_point'), experiment_flags)


def test_get_station_query():
    station, airport = create_indexed_points(
        create_station.mutate(title_en='station'),
        create_station.mutate(title_en='airport', t_type='plane', settlement={})
    )
    experiment_flags = frozenset([ExperimentFlag.ENABLE_STATION_WIZARD])

    with SettlementStationsCache.using_precache():
        assert get_station_query(
            QueryDict('point=station'), experiment_flags
        ) == make_suburban_station_query(station, experiment_flags)
        assert get_station_query(
            QueryDict('point=airport&transport=plane'), experiment_flags
        ) == make_plane_station_query(airport, experiment_flags)


def test_get_station_query_transport_param():
    station, bus_station = create_indexed_points(
        create_station.mutate(title_en='station'),
        create_station.mutate(title_en='station', t_type='bus'),
    )
    experiment_flags = frozenset([ExperimentFlag.ENABLE_STATION_WIZARD])

    with SettlementStationsCache.using_precache():
        assert get_station_query(
            QueryDict('point=station&transport='), experiment_flags
        ) == make_suburban_station_query(station, experiment_flags)
        assert get_station_query(
            QueryDict('point=station&transport=suburban'), experiment_flags
        ) == make_suburban_station_query(station, experiment_flags)
        assert get_station_query(
            QueryDict('point=station&transport=bus'), experiment_flags
        ) == make_suburban_station_query(bus_station, experiment_flags)

        with pytest.raises(NoContent):
            get_station_query(QueryDict('point=station&transport=plane'), experiment_flags)


def test_get_station_query_geo_id_param():
    country = create_country()
    create_settlement(_geo_id=123, country=country)
    local_station, major_station = create_indexed_points(
        create_station.mutate(title_en='station', majority='in_tablo', country=country),
        create_station.mutate(title_en='station', majority='main_in_city'),
    )
    experiment_flags = frozenset([ExperimentFlag.ENABLE_STATION_WIZARD])

    with SettlementStationsCache.using_precache():
        assert get_station_query(
            QueryDict('point=station'), experiment_flags
        ) == make_suburban_station_query(major_station, experiment_flags)
        assert get_station_query(
            QueryDict('point=station&geo_id='), experiment_flags
        ) == make_suburban_station_query(major_station, experiment_flags)
        assert get_station_query(
            QueryDict('point=station&geo_id=123'), experiment_flags
        ) == make_suburban_station_query(local_station, experiment_flags)


def test_get_route_query():
    experiment_flags = frozenset([ExperimentFlag.ENABLE_ROUTE_WIZARD])

    assert get_route_query(QueryDict('foo=bar'), experiment_flags) is None

    with pytest.raises(NoContent):
        get_route_query(QueryDict('number=123'), experiment_flags)

    with pytest.raises(NoContent):
        get_route_query(QueryDict('brand=some_brand'), experiment_flags)


def test_get_general_query():
    settlement = create_settlement(_geo_id=123)
    experiment_flags = frozenset([ExperimentFlag.ENABLE_GENERAL_WIZARD])

    assert get_general_query(QueryDict(''), experiment_flags) is None
    assert get_general_query(
        QueryDict('geo_id=123'), experiment_flags
    ) == make_general_query(settlement, experiment_flags)

    with pytest.raises(NoContent):
        get_general_query(QueryDict('geo_id=456'), experiment_flags)

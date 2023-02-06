# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import mock
import pytest
from hamcrest import assert_that, contains_inanyorder

from common.data_api.search_stats.search_stats import search_stats
from common.models.transport import TransportType
from common.models.geo import Country, Settlement
from common.tester.factories import create_settlement, create_station, create_thread, create_country

from travel.rasp.morda_backend.morda_backend.data_layer.popular_directions import (
    _get_init_settlement_and_station, _settlement_has_one_station, _make_znr_filters, ComplexPointsSegment,
    _make_popular_point, _fill_segments_from_znr, _get_popular_stats_points, get_point_popular_directions
)


pytestmark = [pytest.mark.dbuser]

create_thread = create_thread.mutate(__={'calculate_noderoute': True})


def test_get_init_settlement_and_station():
    settlement = create_settlement()
    station = create_station()
    station_in_settlement = create_station(settlement=settlement)

    assert _get_init_settlement_and_station(station) == (None, station)
    assert _get_init_settlement_and_station(settlement) == (settlement, None)
    assert _get_init_settlement_and_station(station_in_settlement) == (settlement, station_in_settlement)


def test_settlement_has_one_station():
    settlement = create_settlement(country=Country.RUSSIA_ID)
    create_station(
        t_type=TransportType.BUS_ID, settlement=settlement, type_choices='schedule', country=Country.RUSSIA_ID
    )
    create_station(
        t_type=TransportType.BUS_ID, settlement=settlement, type_choices='schedule', country=Country.RUSSIA_ID
    )
    create_station(
        t_type=TransportType.TRAIN_ID, settlement=settlement, type_choices='suburban', country=Country.RUSSIA_ID
    )
    create_station(
        t_type=TransportType.PLANE_ID, settlement=settlement, type_choices='tablo', country=Country.RUSSIA_ID
    )

    assert _settlement_has_one_station(None, TransportType.PLANE_ID) is False
    assert _settlement_has_one_station(settlement, TransportType.PLANE_ID) is True
    assert _settlement_has_one_station(settlement, TransportType.SUBURBAN_ID) is True
    assert _settlement_has_one_station(settlement, TransportType.TRAIN_ID) is False
    assert _settlement_has_one_station(settlement, TransportType.BUS_ID) is False


def _get_and_check_stats_points(direction, point):
    with mock.patch.object(
        search_stats, 'get_top_{}'.format(direction),
        return_value=[('c100', 100), ('s101', 99), ('c200', 98), ('c300', 97)]
    ):
        keys_list, points_dict = _get_popular_stats_points(direction, point, TransportType.get_train_type(), 'all', 5)
        assert keys_list == ['c100', 's101', 'c200', 'c300']

        assert len(points_dict) == 3
        assert 'c100' in points_dict
        assert points_dict['c100'].id == 100
        assert 's101' in points_dict
        assert points_dict['s101'].id == 101
        assert 'c200' in points_dict
        assert points_dict['c200'].id == 200


def test_get_popular_stats_points():
    city = create_settlement(id=100)
    create_settlement(id=200)
    create_station(id=101)

    _get_and_check_stats_points('from', city)
    _get_and_check_stats_points('to', city)


def test_make_znr_filters():
    city = create_settlement(id=300)
    from_city = create_settlement(id=200)
    to_city = create_settlement(id=400)

    bus_station = create_station(id=301, settlement=city, t_type=TransportType.BUS_ID)
    train_station = create_station(id=302, settlement=city, t_type=TransportType.TRAIN_ID)
    from_city_bus_station = create_station(id=201, settlement=from_city, t_type=TransportType.BUS_ID)
    from_city_train_station = create_station(id=202, settlement=from_city, t_type=TransportType.TRAIN_ID)
    from_train_station = create_station(id=102, t_type=TransportType.TRAIN_ID)
    to_city_bus_station = create_station(id=401, settlement=to_city, t_type=TransportType.BUS_ID)
    to_city_train_station = create_station(id=402, settlement=to_city, t_type=TransportType.TRAIN_ID)
    to_train_station = create_station(id=502, t_type=TransportType.TRAIN_ID)

    create_thread(t_type=TransportType.BUS_ID, schedule_v1=[
        [None, 0, from_city_bus_station], [10, 15, bus_station], [20, None, to_city_bus_station]
    ])
    create_thread(t_type=TransportType.TRAIN_ID, schedule_v1=[
        [None, 0, from_city_train_station], [10, 15, train_station], [20, None, to_city_train_station]
    ])
    create_thread(t_type=TransportType.TRAIN_ID, schedule_v1=[
        [None, 0, from_train_station], [10, 15, train_station], [20, None, to_train_station]
    ])

    # From and to station

    filters = _make_znr_filters('to', bus_station, TransportType.BUS_ID, {from_city_bus_station, from_city})
    assert len(filters) == 1
    assert_that(list(filters[0]), contains_inanyorder(
        (300, 301, 200, 201, TransportType.BUS_ID)
    ))

    filters = _make_znr_filters(
        'to', train_station, TransportType.TRAIN_ID, {from_city_train_station, from_city, from_train_station}
    )
    assert len(filters) == 1
    assert_that(list(filters[0]), contains_inanyorder(
        (300, 302, 200, 202, TransportType.TRAIN_ID)
    ))

    filters = _make_znr_filters('to', bus_station, None, {from_city_bus_station, from_city})
    assert len(filters) == 1
    assert_that(list(filters[0]), contains_inanyorder(
        (300, 301, 200, 201, TransportType.BUS_ID)
    ))

    filters = _make_znr_filters('from', bus_station, TransportType.BUS_ID, {to_city_bus_station, to_city})
    assert len(filters) == 1
    assert_that(list(filters[0]), contains_inanyorder(
        (300, 301, 400, 401, TransportType.BUS_ID)
    ))

    filters = _make_znr_filters(
        'from', train_station, TransportType.TRAIN_ID, {to_city_train_station, to_city, to_train_station}
    )
    assert len(filters) == 1
    assert_that(list(filters[0]), contains_inanyorder(
        (300, 302, 400, 402, TransportType.TRAIN_ID)
    ))

    filters = _make_znr_filters(
        'from', train_station, None, {to_city_train_station, to_city, to_train_station}
    )
    assert len(filters) == 1
    assert_that(list(filters[0]), contains_inanyorder(
        (300, 302, 400, 402, TransportType.TRAIN_ID)
    ))

    # From and to settlement

    filters = _make_znr_filters('to', city, TransportType.BUS_ID, {from_city_bus_station, from_city})
    assert len(filters) == 2
    assert_that(list(filters[0]), contains_inanyorder(
        (300, 301, 200, 201, TransportType.BUS_ID),
    ))
    assert_that(list(filters[1]), contains_inanyorder(
        (300, 301, 200, 201, TransportType.BUS_ID),
    ))

    filters = _make_znr_filters(
        'to', city, TransportType.TRAIN_ID, {from_city_train_station, from_city, from_train_station}
    )
    assert len(filters) == 2
    assert_that(list(filters[0]), contains_inanyorder(
        (300, 302, 200, 202, TransportType.TRAIN_ID)
    ))
    assert_that(list(filters[1]), contains_inanyorder(
        (300, 302, 200, 202, TransportType.TRAIN_ID),
        (300, 302, None, 102, TransportType.TRAIN_ID)
    ))

    filters = _make_znr_filters(
        'to', city, None, {from_city_bus_station, from_city_train_station, from_train_station, from_city}
    )
    assert len(filters) == 2
    assert_that(list(filters[0]), contains_inanyorder(
        (300, 301, 200, 201, TransportType.BUS_ID),
        (300, 302, 200, 202, TransportType.TRAIN_ID)
    ))
    assert_that(list(filters[1]), contains_inanyorder(
        (300, 301, 200, 201, TransportType.BUS_ID),
        (300, 302, 200, 202, TransportType.TRAIN_ID),
        (300, 302, None, 102, TransportType.TRAIN_ID),
    ))

    filters = _make_znr_filters('from', city, TransportType.BUS_ID, {to_city_bus_station, to_city})
    assert len(filters) == 2
    assert_that(list(filters[0]), contains_inanyorder(
        (300, 301, 400, 401, TransportType.BUS_ID),
    ))
    assert_that(list(filters[1]), contains_inanyorder(
        (300, 301, 400, 401, TransportType.BUS_ID),
    ))

    filters = _make_znr_filters(
        'from', city, TransportType.TRAIN_ID, {to_city_train_station, to_city, to_train_station}
    )
    assert len(filters) == 2
    assert_that(list(filters[0]), contains_inanyorder(
        (300, 302, 400, 402, TransportType.TRAIN_ID)
    ))
    assert_that(list(filters[1]), contains_inanyorder(
        (300, 302, 400, 402, TransportType.TRAIN_ID),
        (300, 302, None, 502, TransportType.TRAIN_ID)
    ))

    filters = _make_znr_filters(
        'from', city, None, {to_city_bus_station, to_city_train_station, to_train_station, to_city}
    )
    assert len(filters) == 2
    assert_that(list(filters[0]), contains_inanyorder(
        (300, 301, 400, 401, TransportType.BUS_ID),
        (300, 302, 400, 402, TransportType.TRAIN_ID)
    ))
    assert_that(list(filters[1]), contains_inanyorder(
        (300, 301, 400, 401, TransportType.BUS_ID),
        (300, 302, 400, 402, TransportType.TRAIN_ID),
        (300, 302, None, 502, TransportType.TRAIN_ID),
    ))


def test_fill_segments_from_znr():
    city_1 = create_settlement(id=100)
    city_2 = create_settlement(id=200)
    city_3 = create_settlement(id=300)

    station_1_1 = create_station(settlement=city_1, id=101)
    station_1_2 = create_station(settlement=city_1, id=102)
    station_2_1 = create_station(settlement=city_2, id=201)
    station_3_1 = create_station(settlement=city_3, id=301)
    station_3_2 = create_station(settlement=city_3, id=302)

    create_thread(t_type=TransportType.TRAIN_ID, schedule_v1=[
        [None, 0, station_1_1], [10, 15, station_2_1], [20, None, station_3_1]
    ])
    create_thread(t_type=TransportType.TRAIN_ID, schedule_v1=[
        [None, 0, station_1_2], [10, 15, station_2_1], [20, None, station_3_2]
    ])

    point_dict = {
        'c100': city_1, 'c200': city_2, 'c300': city_3,
        's101': station_1_1, 's102': station_1_2,
        's201': station_2_1,
        's301': station_3_1, 's302': station_3_2
    }

    t_type_id = TransportType.TRAIN_ID

    # Станция

    segments_by_init_points = {
        ('s201', 's101'): ComplexPointsSegment(station_2_1, station_1_1, t_type_id, need_znr_filtration=True),
        ('s201', 'c100'): ComplexPointsSegment(station_2_1, city_1, t_type_id, need_znr_filtration=True),
        ('s201', 's301'): ComplexPointsSegment(station_2_1, station_3_1, t_type_id, need_znr_filtration=True),
        ('s201', 'c300'): ComplexPointsSegment(station_2_1, city_3, t_type_id, need_znr_filtration=True),
    }

    _fill_segments_from_znr('to', station_2_1, TransportType.TRAIN_ID, segments_by_init_points, point_dict)

    assert len(segments_by_init_points[('s201', 's101')].stations_segments) == 0
    assert len(segments_by_init_points[('s201', 'c100')].stations_segments) == 2

    assert_that(segments_by_init_points[('s201', 'c100')].stations_segments, contains_inanyorder(
        (201, 101, t_type_id), (201, 102, t_type_id)
    ))

    assert len(segments_by_init_points[('s201', 's301')].stations_segments) == 0
    assert len(segments_by_init_points[('s201', 'c300')].stations_segments) == 0

    _fill_segments_from_znr('from', station_2_1, TransportType.TRAIN_ID, segments_by_init_points, point_dict)

    assert len(segments_by_init_points[('s201', 's101')].stations_segments) == 0
    assert len(segments_by_init_points[('s201', 'c100')].stations_segments) == 2

    assert len(segments_by_init_points[('s201', 's301')].stations_segments) == 0
    assert len(segments_by_init_points[('s201', 'c300')].stations_segments) == 2

    assert_that(segments_by_init_points[('s201', 'c300')].stations_segments, contains_inanyorder(
        (201, 301, t_type_id), (201, 302, t_type_id)
    ))

    # Город

    segments_by_init_points = {
        ('s201', 'c100'): ComplexPointsSegment(station_2_1, city_1, t_type_id, need_znr_filtration=True),
        ('c200', 'c100'): ComplexPointsSegment(city_2, city_1, t_type_id, need_znr_filtration=True),
        ('s201', 'c300'): ComplexPointsSegment(station_2_1, city_3, t_type_id, need_znr_filtration=True),
        ('c200', 'c300'): ComplexPointsSegment(city_2, city_3, t_type_id, need_znr_filtration=True)
    }

    _fill_segments_from_znr('to', city_2, TransportType.TRAIN_ID, segments_by_init_points, point_dict)

    assert len(segments_by_init_points[('s201', 'c100')].stations_segments) == 2
    assert_that(segments_by_init_points[('s201', 'c100')].stations_segments, contains_inanyorder(
        (201, 101, t_type_id), (201, 102, t_type_id)
    ))

    assert len(segments_by_init_points[('c200', 'c100')].stations_segments) == 2
    assert_that(segments_by_init_points[('c200', 'c100')].stations_segments, contains_inanyorder(
        (201, 101, t_type_id), (201, 102, t_type_id)
    ))

    assert len(segments_by_init_points[('c200', 'c300')].stations_segments) == 0
    assert len(segments_by_init_points[('s201', 'c300')].stations_segments) == 0

    _fill_segments_from_znr('from', city_2, TransportType.TRAIN_ID, segments_by_init_points, point_dict)

    assert len(segments_by_init_points[('s201', 'c100')].stations_segments) == 2
    assert len(segments_by_init_points[('c200', 'c100')].stations_segments) == 2

    assert len(segments_by_init_points[('s201', 'c300')].stations_segments) == 2
    assert_that(segments_by_init_points[('s201', 'c300')].stations_segments, contains_inanyorder(
        (201, 301, t_type_id), (201, 302, t_type_id)
    ))

    assert len(segments_by_init_points[('c200', 'c300')].stations_segments) == 2
    assert_that(segments_by_init_points[('c200', 'c300')].stations_segments, contains_inanyorder(
        (201, 301, t_type_id), (201, 302, t_type_id)
    ))


def test_add_points_to_complex_segment():
    inner_station = create_station()
    outer_station = create_station()
    inner_settlement = create_settlement()
    outer_settlement = create_settlement()

    complex_segment = ComplexPointsSegment(inner_station, outer_station, TransportType.PLANE_ID, False)
    assert complex_segment.t_type_id == TransportType.PLANE_ID
    assert complex_segment.inner_station == inner_station
    assert complex_segment.inner_settlement is None
    assert complex_segment.outer_station == outer_station
    assert complex_segment.outer_settlement is None

    complex_segment.add_inner_point(inner_station)
    complex_segment.add_outer_point(outer_station)
    assert complex_segment.inner_station == inner_station
    assert complex_segment.inner_settlement is None
    assert complex_segment.outer_station == outer_station
    assert complex_segment.outer_settlement is None

    complex_segment.add_inner_point(inner_settlement)
    complex_segment.add_outer_point(outer_settlement)
    assert complex_segment.inner_station == inner_station
    assert complex_segment.inner_settlement == inner_settlement
    assert complex_segment.outer_station == outer_station
    assert complex_segment.outer_settlement == outer_settlement

    complex_segment = ComplexPointsSegment(inner_settlement, outer_settlement, TransportType.TRAIN_ID, False)
    assert complex_segment.t_type_id == TransportType.TRAIN_ID
    assert complex_segment.inner_station is None
    assert complex_segment.inner_settlement == inner_settlement
    assert complex_segment.outer_station is None
    assert complex_segment.outer_settlement == outer_settlement

    complex_segment.add_inner_point(inner_settlement)
    complex_segment.add_outer_point(outer_settlement)
    assert complex_segment.inner_station is None
    assert complex_segment.inner_settlement == inner_settlement
    assert complex_segment.outer_station is None
    assert complex_segment.outer_settlement == outer_settlement

    complex_segment.add_inner_point(inner_station)
    complex_segment.add_outer_point(outer_station)
    assert complex_segment.inner_station == inner_station
    assert complex_segment.inner_settlement == inner_settlement
    assert complex_segment.outer_station == outer_station
    assert complex_segment.outer_settlement == outer_settlement


def test_make_popular_point():
    settlement = create_settlement(
        id=100, slug='city', title='Город', title_ru_genitive='Города', title_ru_accusative='Город')
    station = create_station(id=101, slug='station', title='Станция')

    popular_point = _make_popular_point('from', station, station)
    assert popular_point.id == 101
    assert popular_point.point_key == 's101'
    assert popular_point.slug == 'station'
    assert popular_point.title == 'Станция'
    assert popular_point.title_phrase is None

    popular_point = _make_popular_point('from', station, settlement)
    assert popular_point.id == 101
    assert popular_point.point_key == 's101'
    assert popular_point.slug == 'station'
    assert popular_point.title == 'Город'
    assert popular_point.title_phrase == 'из\xa0Города'

    popular_point = _make_popular_point('to', station, settlement)
    assert popular_point.id == 101
    assert popular_point.point_key == 's101'
    assert popular_point.slug == 'station'
    assert popular_point.title == 'Город'
    assert popular_point.title_phrase == 'в\xa0Город'

    popular_point = _make_popular_point('to', settlement, settlement)
    assert popular_point.id == 100
    assert popular_point.point_key == 'c100'
    assert popular_point.slug == 'city'
    assert popular_point.title == 'Город'
    assert popular_point.title_phrase == 'в\xa0Город'


def test_add_alone_stations_and_types():
    city_inner = create_settlement()
    city_outer = create_settlement()
    station_inner_1 = create_station(id=101)
    station_inner_2 = create_station(id=102)
    station_outer_1 = create_station(id=201)
    station_outer_2 = create_station(id=202)

    stations_by_id = {101: station_inner_1, 102: station_inner_2, 201: station_outer_1, 202: station_outer_2}

    complex_segment = ComplexPointsSegment(city_inner, city_outer, TransportType.TRAIN_ID, False)
    complex_segment._add_alone_stations_and_types(stations_by_id)
    assert complex_segment.inner_settlement == city_inner
    assert complex_segment.outer_settlement == city_outer
    assert complex_segment.inner_station is None
    assert complex_segment.outer_station is None
    assert complex_segment.t_type_id == TransportType.TRAIN_ID

    complex_segment = ComplexPointsSegment(city_inner, city_outer, TransportType.TRAIN_ID, True)
    complex_segment.stations_segments.add((101, 201, TransportType.TRAIN_ID))
    complex_segment._add_alone_stations_and_types(stations_by_id)
    assert complex_segment.inner_settlement == city_inner
    assert complex_segment.outer_settlement == city_outer
    assert complex_segment.inner_station == station_inner_1
    assert complex_segment.outer_station == station_outer_1
    assert complex_segment.t_type_id == TransportType.TRAIN_ID

    complex_segment = ComplexPointsSegment(city_inner, city_outer, TransportType.TRAIN_ID, True)
    complex_segment.stations_segments.add((101, 201, TransportType.TRAIN_ID))
    complex_segment.stations_segments.add((102, 202, TransportType.TRAIN_ID))
    complex_segment._add_alone_stations_and_types(stations_by_id)
    assert complex_segment.inner_settlement == city_inner
    assert complex_segment.outer_settlement == city_outer
    assert complex_segment.inner_station is None
    assert complex_segment.outer_station is None
    assert complex_segment.t_type_id == TransportType.TRAIN_ID

    complex_segment = ComplexPointsSegment(city_inner, city_outer, None, True)
    complex_segment._add_alone_stations_and_types(stations_by_id)
    assert complex_segment.inner_settlement == city_inner
    assert complex_segment.outer_settlement == city_outer
    assert complex_segment.inner_station is None
    assert complex_segment.outer_station is None
    assert complex_segment.t_type_id == TransportType.PLANE_ID

    complex_segment = ComplexPointsSegment(city_inner, city_outer, None, True)
    complex_segment.stations_segments.add((101, 201, TransportType.TRAIN_ID))
    complex_segment._add_alone_stations_and_types(stations_by_id)
    assert complex_segment.inner_settlement == city_inner
    assert complex_segment.outer_settlement == city_outer
    assert complex_segment.inner_station == station_inner_1
    assert complex_segment.outer_station == station_outer_1
    assert complex_segment.t_type_id == TransportType.TRAIN_ID

    complex_segment = ComplexPointsSegment(city_inner, city_outer, None, True)
    complex_segment.stations_segments.add((101, 201, TransportType.TRAIN_ID))
    complex_segment.stations_segments.add((102, 202, TransportType.TRAIN_ID))
    complex_segment._add_alone_stations_and_types(stations_by_id)
    assert complex_segment.inner_settlement == city_inner
    assert complex_segment.outer_settlement == city_outer
    assert complex_segment.inner_station is None
    assert complex_segment.outer_station is None
    assert complex_segment.t_type_id == TransportType.TRAIN_ID

    complex_segment = ComplexPointsSegment(city_inner, city_outer, None, True)
    complex_segment.stations_segments.add((101, 201, TransportType.TRAIN_ID))
    complex_segment.stations_segments.add((102, 202, TransportType.PLANE_ID))
    complex_segment._add_alone_stations_and_types(stations_by_id)
    assert complex_segment.inner_settlement == city_inner
    assert complex_segment.outer_settlement == city_outer
    assert complex_segment.inner_station is None
    assert complex_segment.outer_station is None
    assert complex_segment.t_type_id is None


def test_complex_segment_get_popular_direction():
    station_inner_hidden = create_station(hidden=True)
    station_outer_hidden = create_station(hidden=True)
    station_inner = create_station(id=101)
    station_outer = create_station(id=201)
    stations_by_id = {101: station_inner, 201: station_outer}

    complex_segment = ComplexPointsSegment(station_inner_hidden, station_outer, None, False)
    assert complex_segment.get_popular_direction('from', stations_by_id) is None

    complex_segment = ComplexPointsSegment(station_inner, station_outer_hidden, None, False)
    assert complex_segment.get_popular_direction('from', stations_by_id) is None

    complex_segment = ComplexPointsSegment(station_inner, station_outer, None, False)
    popular_direction = complex_segment.get_popular_direction('from', stations_by_id)
    assert popular_direction is not None
    assert popular_direction.t_type is None
    assert popular_direction.inner_point.id == station_inner.id
    assert popular_direction.inner_point.slug == station_inner.slug
    assert popular_direction.inner_point.title == station_inner.title
    assert popular_direction.outer_point.id == station_outer.id
    assert popular_direction.outer_point.slug == station_outer.slug
    assert popular_direction.outer_point.title == station_outer.title


def test_get_point_popular_directions():
    foreign_country = create_country(id=Country.FINLAND_ID)
    foreign_city = create_settlement(id=100, title='Foreign City', country=foreign_country)
    foreign_airport = create_station(
        id=101, title='Foreign Station', settlement=foreign_city, t_type=TransportType.PLANE_ID, type_choices='tablo'
    )
    create_settlement(id=200, title='City')
    create_station(id=201, title='Station')
    create_station(id=202, title='Station2')

    with mock.patch.object(
        search_stats, 'get_top_from',
        return_value=[('s201', 99), ('s299', 98), ('c200', 97), ('s202', 96)]
    ):
        popular_directions = get_point_popular_directions('from', foreign_airport, None, 2, 'all')
        assert len(popular_directions) == 2

        assert popular_directions[0].t_type == TransportType.get_plane_type()
        assert popular_directions[0].inner_point.id == 101
        assert popular_directions[0].inner_point.title == 'Foreign City'
        assert popular_directions[0].outer_point.id == 201
        assert popular_directions[0].outer_point.title == 'Station'

        assert popular_directions[1].t_type == TransportType.get_plane_type()
        assert popular_directions[1].inner_point.id == 101
        assert popular_directions[1].inner_point.title == 'Foreign City'
        assert popular_directions[1].outer_point.id == 200
        assert popular_directions[1].outer_point.title == 'City'

    our_city = create_settlement(id=300, title='Our City', country=Country.RUSSIA_ID)
    our_station = create_station(
        id=301, title='Our Station', country=Country.RUSSIA_ID, settlement=our_city,
        t_type=TransportType.TRAIN_ID, type_choices='train'
    )
    city = create_settlement(id=400, title='City')
    station1 = create_station(id=351, title='Station')
    station2 = create_station(id=401, title='City Station', settlement=city)

    create_thread(t_type=TransportType.TRAIN_ID, schedule_v1=[
        [None, 0, station2], [10, 15, station1], [20, None, our_station]
    ])

    with mock.patch.object(search_stats, 'get_top_to', return_value=[('c400', 98)]):
        popular_directions = get_point_popular_directions('to', our_city, TransportType.get_train_type(), 2, 'c')
        assert len(popular_directions) == 1

        assert popular_directions[0].t_type == TransportType.get_train_type()
        assert popular_directions[0].inner_point.id == 301
        assert popular_directions[0].inner_point.title == 'Our City'
        assert popular_directions[0].outer_point.id == 401
        assert popular_directions[0].outer_point.title == 'City'

    with mock.patch.object(
        search_stats, 'get_top_to',
        return_value=[('s351', 99), ('c400', 98), ('s401', 97)]
    ):
        popular_directions = get_point_popular_directions('to', our_station, TransportType.get_train_type(), 4, 'all')
        assert len(popular_directions) == 2

        assert popular_directions[0].t_type == TransportType.get_train_type()
        assert popular_directions[0].inner_point.id == 301
        assert popular_directions[0].inner_point.title == 'Our City'
        assert popular_directions[0].outer_point.id == 351
        assert popular_directions[0].outer_point.title == 'Station'

        assert popular_directions[1].t_type == TransportType.get_train_type()
        assert popular_directions[1].inner_point.id == 301
        assert popular_directions[1].inner_point.title == 'Our City'
        assert popular_directions[1].outer_point.id == 401
        assert popular_directions[1].outer_point.title == 'City'

    moscow = Settlement.objects.get(id=Settlement.MOSCOW_ID)

    with mock.patch.object(
        search_stats, 'get_top_to',
        return_value=[('c300', 99), ('c100', 98)]
    ):
        popular_directions = get_point_popular_directions('to', moscow, None, 2, 'c')
        assert len(popular_directions) == 1

        assert popular_directions[0].t_type is None
        assert popular_directions[0].inner_point.id == Settlement.MOSCOW_ID
        assert popular_directions[0].inner_point.title == 'Москва'
        assert popular_directions[0].outer_point.id == 300
        assert popular_directions[0].outer_point.title == 'Our City'

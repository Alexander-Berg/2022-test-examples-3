# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

import hamcrest
import pytest
from django.conf import settings
from django.http.request import QueryDict

from common.tester.factories import create_settlement, create_station
from common.tester.utils.datetime import replace_now
from common.tester.utils.replace_setting import replace_setting
from travel.rasp.wizards.proxy_api.lib.router import (
    get_general_response, get_response, get_plane_station_response,
    get_suburban_station_response, make_direction_response
)
from travel.rasp.wizards.proxy_api.lib.requests_pool import default_pool
from travel.rasp.wizards.proxy_api.lib.station.settlement_stations_cache import SettlementStationsCache
from travel.rasp.wizards.proxy_api.lib.tests_utils import (
    create_indexed_points, make_direction_query, make_direction_response_body, make_general_query,
    make_plane_station_query, make_suburban_station_query
)
from travel.rasp.wizards.suburban_wizard_api.lib.tests_utils import make_station_response_body  # TODO move it to wizard_lib
from travel.rasp.wizards.wizard_lib.tests_utils import (
    make_dummy_baris_tablo_response, make_dummy_raw_segment, make_dummy_segment, make_empty_baris_tablo_response
)


@pytest.fixture(autouse=True)
def do_before_tests():
    with replace_setting('BARIS_API_URL', 'http://plane/station/'):   # request pool cleanup run before test
        with replace_setting('PROXY_API_REQUEST_SOURCES_TIMEOUT', 999.9):  # for debug
            default_pool.cleanup()  # reinit baris client with new settings
            yield


def _baris_tablo_url(station):
    return '{}api/v1/flight-board/{}/'.format(settings.BARIS_API_URL, station.id)


@pytest.mark.dbuser
def test_make_direction_response_error_handling(httpretty):
    httpretty.register_uri(httpretty.GET, 'http://direction/suburban', status=500)
    response = make_direction_response(
        query=make_direction_query(create_station(), create_station()),
        source_urls=(
            ('suburban', 'http://direction/suburban'),
        ),
    )

    assert response.status_code == 502
    hamcrest.assert_that(response.data, hamcrest.has_entries({
        'error': 'cannot get direction response',
        'exception': hamcrest.starts_with('500 Server Error'),
        'transport_code': 'suburban',
        'url': 'http://direction/suburban',
    }))
    assert len(httpretty.latest_requests) == 1


@pytest.mark.dbuser
def test_make_direction_response_empty_handling(httpretty):
    httpretty.register_uri(httpretty.GET, 'http://direction/bus', status=204, body='')
    httpretty.register_uri(httpretty.GET, 'http://direction/suburban', status=204, body='')

    assert make_direction_response(
        query=make_direction_query(create_station(), create_station()),
        source_urls=(
            ('bus', 'http://direction/bus'),
            ('suburban', 'http://direction/suburban'),
        ),
    ) is None
    assert len(httpretty.latest_requests) == 2


@pytest.mark.dbuser
def test_make_direction_response_single_transport_handling(httpretty):
    query = make_direction_query(create_station(), create_station())
    segments = (make_dummy_segment(query.departure_point, query.arrival_point),)
    httpretty.register_uri(
        httpretty.GET, 'http://direction/bus', status=204, body=''
    )
    httpretty.register_uri(
        httpretty.GET, 'http://direction/suburban', body=make_direction_response_body(segments, query)
    )
    response = make_direction_response(
        query=query,
        source_urls=(
            ('bus', 'http://direction/bus'),
            ('suburban', 'http://direction/suburban'),
        ),
    )

    assert response.status_code == 200
    hamcrest.assert_that(response.data, hamcrest.has_entry('type', 'transports_with_default'))
    assert len(httpretty.latest_requests) == 2


@pytest.mark.dbuser
def test_make_direction_response_multiple_transports_handling(httpretty):
    query = make_direction_query(create_station(), create_station())
    segments = (make_dummy_segment(query.departure_point, query.arrival_point),)
    dummy_response = make_direction_response_body(segments, query)
    httpretty.register_uri(httpretty.GET, 'http://direction/bus', body=dummy_response)
    httpretty.register_uri(httpretty.GET, 'http://direction/suburban', body=dummy_response)
    response = make_direction_response(
        query=query,
        source_urls=(
            ('bus', 'http://direction/bus'),
            ('suburban', 'http://direction/suburban'),
        ),
    )

    assert response.status_code == 200
    hamcrest.assert_that(response.data, hamcrest.has_entries({'type': 'transports', 'content': hamcrest.has_length(2)}))
    assert len(httpretty.latest_requests) == 2


@pytest.mark.dbuser
@replace_setting('PROXY_API_SUBURBAN_STATION_URL', 'http://suburban/station')
def test_get_suburban_station_response_error_handling(httpretty):
    httpretty.register_uri(httpretty.GET, 'http://suburban/station', status=500)
    response = get_suburban_station_response(make_suburban_station_query(create_station()), QueryDict('foo=1&bar=baz'))

    assert response.status_code == 502
    hamcrest.assert_that(response.data, hamcrest.has_entries({
        'error': 'cannot get suburban station response',
        'exception': hamcrest.starts_with('500 Server Error'),
        'url': hamcrest.starts_with('http://suburban/station?'),
    }))
    assert len(httpretty.latest_requests) == 1


@pytest.mark.dbuser
@replace_setting('PROXY_API_SUBURBAN_STATION_URL', 'http://suburban/station')
def test_get_suburban_station_response_empty_handling(httpretty):
    httpretty.register_uri(httpretty.GET, 'http://suburban/station', status=204)
    response = get_suburban_station_response(make_suburban_station_query(create_station()), QueryDict('foo=1&bar=baz'))

    assert response is None
    assert len(httpretty.latest_requests) == 1


@pytest.mark.dbuser
@replace_setting('PROXY_API_SUBURBAN_STATION_URL', 'http://suburban/station')
def test_get_suburban_station_response(httpretty):
    station = create_station()
    dummy_response = make_station_response_body(station, [make_dummy_raw_segment(create_station(), station)])
    httpretty.register_uri(httpretty.GET, 'http://suburban/station', body=dummy_response)
    response = get_suburban_station_response(make_suburban_station_query(station), QueryDict('foo=1&bar=baz'))

    assert response.status_code == 200
    hamcrest.assert_that(response.data, hamcrest.has_entries({
        'type': 'suburban_directions',
        'content': hamcrest.has_length(1)
    }))
    assert len(httpretty.latest_requests) == 1


@pytest.mark.dbuser
def test_get_plane_station_response_error_handling(httpretty):
    station = create_station()
    baris_tablo_url = _baris_tablo_url(station)
    httpretty.register_uri(httpretty.GET, baris_tablo_url, status=500)
    response = get_plane_station_response(make_plane_station_query(station), QueryDict('foo=1&bar=baz'))

    assert response.status_code == 502
    hamcrest.assert_that(response.data, hamcrest.has_entries({
        'error': 'cannot get plane station response',
        'exception': hamcrest.starts_with('500 Server Error'),
        'url': hamcrest.starts_with('<BARIS>/flight_board'),
    }))
    assert len(httpretty.latest_requests) == 1


@pytest.mark.dbuser
def test_get_plane_station_response_empty_handling(httpretty):
    station = create_station()
    baris_tablo_url = _baris_tablo_url(station)
    httpretty.register_uri(
        httpretty.GET,
        baris_tablo_url,
        status=200,
        body=make_empty_baris_tablo_response(station, 'arrival')
    )
    response = get_plane_station_response(make_plane_station_query(station), QueryDict('foo=1&bar=baz'))

    assert response is None
    assert len(httpretty.latest_requests) == 1


@pytest.mark.dbuser
@replace_now('2000-01-01 02:02:02')
def test_get_plane_station_response(httpretty):
    station = create_station(time_zone='Etc/GMT-5')
    other_station = create_station()
    baris_tablo_url = _baris_tablo_url(station)
    dummy_fl = {'station_from': station, 'station_to': other_station}
    dummy_response = make_dummy_baris_tablo_response(station, 'arrival', [dummy_fl, dummy_fl, dummy_fl])
    httpretty.register_uri(httpretty.GET, baris_tablo_url, body=dummy_response)
    response = get_plane_station_response(make_plane_station_query(station), QueryDict('foo=1&bar=baz'))

    assert response.status_code == 200
    hamcrest.assert_that(response.data, hamcrest.has_entries({
        'type': 'airport_panel_with_event',
        'content': hamcrest.has_length(2)
    }))
    assert len(httpretty.latest_requests) == 1
    assert httpretty.last_request.querystring['after'] == ['2000-01-01T04:02:02']
    assert httpretty.last_request.querystring['before'] == ['2000-01-02T04:02:02']


@pytest.mark.dbuser
def test_get_general_response():
    departure_settlement = create_settlement()
    query = make_general_query(departure_settlement)
    response = get_general_response(query, None)

    assert response.status_code == 200
    hamcrest.assert_that(response.data, hamcrest.has_entry('type', 'direction_query'))


@pytest.mark.parametrize('params', (
    '',
    'point=&point_to=station',
    'departure_settlement_geoid=1&arrival_settlement_geoid=',
    'departure_settlement_geoid=&arrival_settlement_geoid=2',
))
def test_get_response_invalid_routing(params):
    assert get_response(QueryDict(params)) is None


@pytest.mark.dbuser
@replace_setting('PROXY_API_BUS_DIRECTION_URL', 'http://direction/bus')
@replace_setting('PROXY_API_SUBURBAN_DIRECTION_URL', 'http://direction/suburban')
@pytest.mark.parametrize('params, expected_paths', (
    # no requests when points are unknown
    ('point=unknown_point&point_to=unknown_point&exp_flags=RASPWIZARDS-ENABLE-DIRECTION', ()),

    # requesting all sources when *transport_code* is not supplied
    ('point=departure_point&point_to=arrival_point&exp_flags=RASPWIZARDS-ENABLE-DIRECTION', ('/bus', '/suburban',)),

    # requesting specific source when *transport_code* is supplied
    (
        'transport=bus&point=departure_point&point_to=arrival_point&exp_flags=RASPWIZARDS-ENABLE-DIRECTION',
        ('/bus', '/suburban',)
    ),
    (
        'transport=suburban&point=departure_point&point_to=arrival_point&exp_flags=RASPWIZARDS-ENABLE-DIRECTION',
        ('/suburban', '/bus')
    ),

    # when *query* is supplied only suburban source is requested
    ('query=&point=departure_point&point_to=arrival_point&exp_flags=RASPWIZARDS-ENABLE-DIRECTION', ('/bus', '/suburban',)),
    ('query=express&point=departure_point&point_to=arrival_point&exp_flags=RASPWIZARDS-ENABLE-DIRECTION', ('/suburban',)),
    ('query=express&transport=bus&point=departure_point&point_to=arrival_point&exp_flags=RASPWIZARDS-ENABLE-DIRECTION', ()),
))
def test_get_response_direction_routing(httpretty, params, expected_paths):
    httpretty.register_uri(httpretty.GET, 'http://direction/bus', status=204, body='')
    httpretty.register_uri(httpretty.GET, 'http://direction/suburban', status=204, body='')
    create_indexed_points(
        create_settlement.mutate(title_en='departure_point', type_choices='bus,suburban'),
        create_settlement.mutate(title_en='arrival_point', type_choices='bus,suburban')
    )

    assert get_response(QueryDict(params)) is None
    hamcrest.assert_that(
        [request.path.partition('?')[0] for request in httpretty.latest_requests],
        hamcrest.contains_inanyorder(*expected_paths)
    )


@pytest.mark.dbuser
@pytest.mark.parametrize('params', (
    'number=123',
    'brand=123',
))
def test_get_response_disabled_routes(httpretty, params):
    assert get_response(QueryDict(params)) is None


@pytest.mark.dbuser
@replace_setting('PROXY_API_SUBURBAN_STATION_URL', 'http://suburban/station')
def test_get_response_suburban_station_routing(httpretty):
    httpretty.register_uri(httpretty.GET, 'http://suburban/station', status=204, body='')
    create_indexed_points(create_station.mutate(title_en='station', t_type='suburban'))

    with SettlementStationsCache.using_precache():
        assert get_response(QueryDict('point=unknown_station&exp_flags=RASPWIZARDS-ENABLE-STATION')) is None
        assert len(httpretty.latest_requests) == 0

        assert get_response(QueryDict('point=station&exp_flags=RASPWIZARDS-ENABLE-STATION')) is None
        assert len(httpretty.latest_requests) == 1


@pytest.mark.dbuser
def test_get_response_plane_station_routing(httpretty):
    station = create_station(title_en='station', t_type='plane')
    baris_tablo_url = _baris_tablo_url(station)
    httpretty.register_uri(
        httpretty.GET,
        baris_tablo_url,
        status=200,
        body=make_empty_baris_tablo_response(station, 'arrival')
    )

    create_indexed_points(create_station.mutate(title_en='station', t_type='plane'))

    with SettlementStationsCache.using_precache():
        assert get_response(
            QueryDict('point=unknown_station&transport=plane&exp_flags=RASPWIZARDS-ENABLE-STATION')
        ) is None
        assert len(httpretty.latest_requests) == 0

        assert get_response(QueryDict('point=station&transport=plane&exp_flags=RASPWIZARDS-ENABLE-STATION')) is None
        assert len(httpretty.latest_requests) == 1


@pytest.mark.dbuser
def test_get_response_general_routing():
    assert get_response(QueryDict('geo_id=123&exp_flags=RASPWIZARDS-ENABLE-GENERAL')) is None

    create_settlement(_geo_id=123)
    assert get_response(QueryDict('geo_id=123&exp_flags=RASPWIZARDS-ENABLE-GENERAL')) is not None

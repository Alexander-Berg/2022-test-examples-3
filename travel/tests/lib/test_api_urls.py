# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

from datetime import date

import pytest
from django.http.request import QueryDict

from common.tester.factories import create_station
from common.tester.utils.replace_setting import replace_setting
from travel.rasp.wizards.proxy_api.lib.api_urls import (
    format_bus_direction_url, format_suburban_direction_url, format_suburban_station_url
)
from travel.rasp.wizards.proxy_api.lib.tests_utils import make_direction_query, make_plane_station_query, make_suburban_station_query
from travel.rasp.wizards.wizard_lib.serialization.limit import DEFAULT_SEGMENTS_LIMIT
from travel.rasp.wizards.wizard_lib.serialization.thread_express_type import ThreadExpressType


@pytest.mark.dbuser
@replace_setting('PROXY_API_BUS_DIRECTION_URL', 'http://bus-direction/')
@replace_setting('PROXY_API_SUBURBAN_DIRECTION_URL', 'http://suburban-direction/')
@pytest.mark.parametrize('url_formatter, expected_head', (
    (format_bus_direction_url, b'http://bus-direction/'),
    (format_suburban_direction_url, b'http://suburban-direction/'),
))
@pytest.mark.parametrize('query, expected_params', (
    (
        make_direction_query(),
        'lang=ru&limit={}'.format(DEFAULT_SEGMENTS_LIMIT)
    ),
    (
        make_direction_query(language='uk'),
        'lang=uk&limit={}'.format(DEFAULT_SEGMENTS_LIMIT)
    ),
    (
        make_direction_query(departure_date=date(2000, 1, 1)),
        'departure_date=2000-01-01&lang=ru&limit={}'.format(DEFAULT_SEGMENTS_LIMIT)
    ),
    (
        make_direction_query(thread_number='123'),
        'thread_number=123&lang=ru&limit={}'.format(DEFAULT_SEGMENTS_LIMIT)
    ),
    (
        make_direction_query(thread_express_type=ThreadExpressType.EXPRESS),
        'thread_express_type=express&lang=ru&limit={}'.format(DEFAULT_SEGMENTS_LIMIT)
    ),
    (
        make_direction_query(tld='com'),
        'lang=ru&limit={}&tld=com'.format(DEFAULT_SEGMENTS_LIMIT)
    )
))
def test_format_direction_urls(url_formatter, expected_head, query, expected_params):
    departure_station = create_station()
    arrival_station = create_station()
    head, _, params = url_formatter(
        query=query._replace(departure_point=departure_station, arrival_point=arrival_station),
        query_params=QueryDict('reqid=some_random_reqid&foo=bar&exp_flags=some_random_flags')
    ).partition('?')

    assert head == expected_head
    assert params == (
        'exp_flags=some_random_flags&reqid=some_random_reqid&departure_point_key={}&arrival_point_key={}&{}'
        .format(departure_station.point_key, arrival_station.point_key, expected_params)
    )


@pytest.mark.dbuser
@replace_setting('PROXY_API_SUBURBAN_STATION_URL', 'http://suburban/station')
@pytest.mark.parametrize('url_formatter, query_factory, expected_head', (
    (format_suburban_station_url, make_suburban_station_query, b'http://suburban/station'),
))
@pytest.mark.parametrize('query_kwargs, expected_params', (
    (dict(), 'lang=ru'),
    (dict(event_date=date(2000, 1, 1)), 'event_date=2000-01-01&lang=ru'),
    (dict(language='uk'), 'lang=uk'),
    (dict(tld='com'), 'lang=ru&tld=com')
))
def test_format_station_urls(url_formatter, query_factory, expected_head, query_kwargs, expected_params):
    station = create_station()
    head, _, params = url_formatter(
        query_factory(station, **query_kwargs),
        QueryDict('reqid=some_random_reqid&foo=bar&exp_flags=some_random_flags')
    ).partition('?')

    assert head == expected_head
    assert params == (
        'exp_flags=some_random_flags&reqid=some_random_reqid&station_id={}&{}'
        .format(station.id, expected_params)
    )

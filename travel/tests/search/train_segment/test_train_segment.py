# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import json
from datetime import datetime
from itertools import product

import pytest
from django.test import Client
from hamcrest import assert_that, has_entries, any_of, has_entry

from common.models.schedule import RThreadType
from common.tester.factories import create_station, create_thread, create_country
from common.tester.utils.datetime import replace_now
from common.utils.date import RunMask


def test_return_errors_if_wrong_with_query():
    _make_check(
        with_query={'when': '2016-01-01'},
        expected_result=has_entries(
            errors=has_entries(
                number=['Missing data for required field.'],
                pointFrom=['Missing data for required field.'],
                pointTo=['Missing data for required field.'],
                departure=['Missing data for required field.'],
                _schema=[
                   {'point_to': 'no_such_point', 'point_from': 'no_such_point'}
                ]
            )
        ),
        expected_code=400
    )


@replace_now('2000-01-01 00:00:00')
@pytest.mark.dbuser
def test_no_segments_at_all():
    station_from, station_to = create_station(), create_station()
    _make_check(
        with_query={
            'pointFrom': station_from.point_key,
            'pointTo': station_to.point_key,
            'number': '',
            'departure': datetime.utcnow()
        },
        expected_result=has_entries(
            errors=has_entries(
                wrongRequest=['no_such_segment']
            )
        ),
        expected_code=400
    )


@replace_now('2000-01-01 00:00:00')
@pytest.mark.dbuser
def test_no_segments_with_such_number():
    station_from, station_to, _ = create_segment()
    _make_check(
        with_query={
            'pointFrom': station_from.point_key,
            'pointTo': station_to.point_key,
            'number': 'some-number',
            'departure': '2001-01-02T00:10'
        },
        expected_result=has_entries(
            errors=has_entries(
                wrongRequest=['no_such_segment']
            )
        ),
        expected_code=400
    )


@replace_now('2000-01-01 00:00:00')
@pytest.mark.dbuser
def test_no_segments_with_such_departure():
    station_from, station_to, _ = create_segment()
    _make_check(
        with_query={
            'pointFrom': station_from.point_key,
            'pointTo': station_to.point_key,
            'number': 'doesnt-matter',
            'departure': '2000-02-02T00:10'
        },
        expected_result=has_entries(
            errors=has_entries(
                wrongRequest=['no_such_segment']
            )
        ),
        expected_code=400
    )


@replace_now('2000-01-01 00:00:00')
@pytest.mark.dbuser
def test_segment_found():
    station_from, station_to, _ = create_segment()
    _make_check(
        with_query={
            'pointFrom': station_from.point_key,
            'pointTo': station_to.point_key,
            'number': 'number-2',
            'departure': '2002-01-02T00:10'
        },
        expected_result=has_entries(
            result=has_entries(
                departure='2002-01-01T21:10:00+00:00',
                arrival='2002-01-01T21:10:00+00:00',
                thread=has_entries(
                    uid='number-2',
                    title=None,
                    number='number-2',
                    firstCountryCode='from',
                    lastCountryCode='to'
                ),
                stationFrom=has_entries(
                    title=station_from.title,
                    id=station_from.id
                ),
                stationTo=has_entries(
                    title=station_to.title,
                    id=station_to.id
                )
            )
        ),
        expected_code=200
    )


@replace_now('2000-01-01 00:00:00')
@pytest.mark.dbuser
def test_meta_segment_found():
    station_from, station_to, _ = create_segment_keys(keys=['303Я', '303М'])
    expected_result = has_entries(
        result=has_entries(
            number='303МЯ',
            thread=any_of(has_entry('number', '303М'), has_entry('number', '303Я'))
        )
    )
    _make_check(
        with_query={
            'pointFrom': station_from.point_key,
            'pointTo': station_to.point_key,
            'number': '303Я',
            'departure': '2002-01-02T00:10'
        },
        expected_result=expected_result,
        expected_code=200
    )
    _make_check(
        with_query={
            'pointFrom': station_from.point_key,
            'pointTo': station_to.point_key,
            'number': '303М',
            'departure': '2002-01-02T00:10'
        },
        expected_result=expected_result,
        expected_code=200
    )
    _make_check(
        with_query={
            'pointFrom': station_from.point_key,
            'pointTo': station_to.point_key,
            'number': '303МЯ',
            'departure': '2002-01-02T00:10'
        },
        expected_result=expected_result,
        expected_code=200
    )


def _make_check(with_query, expected_code=200, expected_result=None, lang='ru'):
    response = Client().get('/{lang}/search/train-segment/'.format(lang=lang), with_query)
    assert response.status_code == expected_code
    if expected_result is not None:
        assert_that(json.loads(response.content), expected_result)


def create_segment():
    country_from = create_country(code='from')
    country_to = create_country(code='to')

    station_from = create_station(country=country_from)
    station_to = create_station(country=country_to)

    for shift, t_type in product(range(1, 11), ['train']):
        key = 'number-{}'.format(shift)
        create_thread(
            __={'calculate_noderoute': True},

            uid=key,
            number=key,
            t_type=t_type,
            type=RThreadType.BASIC_ID,
            year_days=RunMask.range(datetime(2000, 1, shift), datetime(2000, 1, shift + 1)),
            schedule_v1=[
                [None, 10, station_from],
                [10, None, station_to],
            ],
        )
    return station_from, station_to, []


def create_segment_keys(keys):
    country_from = create_country(code='from')
    country_to = create_country(code='to')

    station_from = create_station(country=country_from)
    station_to = create_station(country=country_to)

    for key in keys:
        create_thread(
            __={'calculate_noderoute': True},

            uid=key,
            number=key,
            t_type='train',
            type=RThreadType.BASIC_ID,
            year_days=RunMask.range(datetime(2000, 1, 1), datetime(2000, 1, 10)),
            schedule_v1=[
                [None, 10, station_from],
                [10, None, station_to],
            ],
        )
    return station_from, station_to, []

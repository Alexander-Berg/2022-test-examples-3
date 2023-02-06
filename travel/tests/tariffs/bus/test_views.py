# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import json
from datetime import date, datetime
from functools import partial
from urllib import urlencode

import mock
import pytest
from django.test import Client
from freezegun import freeze_time
from hamcrest import assert_that, has_entries, contains, anything

from common.data_api.yandex_bus.factories import create_segment
from common.models.currency import Price
from common.tester.factories import create_station
from travel.rasp.morda_backend.morda_backend.tariffs.bus.service import make_ybus_tariff_keys


def _check_response(query, matcher):
    response = Client().get('/ru/segments/bus-tariffs/?{}'.format(urlencode(query, doseq=True)))
    assert response.status_code == 200
    if matcher is not None:
        assert_that(json.loads(response.content), matcher)


@pytest.yield_fixture
def m_collect_results():
    with mock.patch(
        'travel.rasp.morda_backend.morda_backend.tariffs.bus.views.collect_yandex_buses_results',
        return_value=[]
    ) as m_collect_results:
        yield m_collect_results


@pytest.mark.dbuser
def test_tariffs(m_collect_results):
    point_from, point_to = create_station(), create_station()
    new_segment = partial(create_segment, station_from=point_from, station_to=point_to, departure=datetime(2001, 1, 1))
    segment1 = new_segment(title='Foo', price=Price(100, 'RUB'), seats=40)
    segment2 = new_segment(title='Bar', seats=0, number='000')
    m_collect_results.return_value = ([segment1, segment2], False)
    _check_response({
        'pointFrom': point_from.point_key,
        'pointTo': point_to.point_key,
        'date': ['2001-01-01', '2001-01-02'],
        'nationalVersion': 'ru',
    }, has_entries(
        segments=contains(
            has_entries(
                title=segment1.title,
                keys=make_ybus_tariff_keys(segment1),
                tariffs=has_entries(
                    classes=has_entries(
                        bus=has_entries(
                            price={'value': 100, 'currency': 'RUB'},
                            seats=40
                        )
                    )
                )
            ),
            has_entries(
                title=segment2.title,
                keys=make_ybus_tariff_keys(segment2),
                tariffs=has_entries(
                    classes=has_entries(
                        bus=has_entries(
                            price=None,
                            seats=0
                        )
                    )
                )
            )
        ),
        querying=False
    ))
    m_collect_results.assert_called_once_with(point_from, point_to, [date(2001, 1, 1), date(2001, 1, 2)], False)


@freeze_time('2001-01-01')
@pytest.mark.dbuser
def test_tariffs_no_date(m_collect_results):
    point_from, point_to = create_station(), create_station()
    m_collect_results.return_value = ([], False)
    _check_response({
        'pointFrom': point_from.point_key,
        'pointTo': point_to.point_key,
        'nationalVersion': 'ru',
    }, has_entries(segments=[], querying=False))
    m_collect_results.assert_called_once_with(point_from, point_to, [date(2001, 1, 1)], False)


@pytest.mark.dbuser
def test_tariffs_wrong_request(m_collect_results):
    segment1, segment2 = create_segment(title='Foo'), create_segment(title='Bar')
    m_collect_results.return_value = [
        segment1, segment2
    ]
    point_from, point_to = create_station(), create_station()
    _check_response({
        'pointFrom': point_from.point_key,
        'pointTo': point_to.point_key,
        'date': ['2001-01-01', '2001-01-02'],
    }, has_entries(errors=anything()))
    assert not m_collect_results.called

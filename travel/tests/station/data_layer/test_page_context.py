# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import date, time

import pytest

from common.tester.utils.datetime import replace_now
from common.tester.factories import create_station
from travel.rasp.library.python.common23.date import environment

from travel.rasp.morda_backend.morda_backend.station.data_layer.page_type import FindStationError
from travel.rasp.morda_backend.morda_backend.station.data_layer.page_context import StationPageContext


pytestmark = [pytest.mark.dbuser]


def _check_context(request_date, request_event, is_all_days=None, special=None, date=None, ex_message_date=None):
    station = create_station()
    now_dt = environment.now_aware()

    if ex_message_date:
        with pytest.raises(FindStationError) as ex:
            StationPageContext(station, request_date, request_event, now_dt)
        assert ex.value.message == 'В БД нет информации об отправлениях на дату: {}'.format(ex_message_date)
        return

    context = StationPageContext(station, request_date, request_event, now_dt)

    assert context.event == request_event
    assert context.is_all_days == is_all_days
    if special:
        assert context.special == special
    else:
        assert hasattr(context, 'special') is False
    if date:
        assert context.date == date
    else:
        assert hasattr(context, 'date') is False
    assert context.when == context
    assert context.dt_now.isoformat() == '2020-02-01T00:00:00+03:00'


@replace_now('2020-02-01')
def test_station_page_context():
    _check_context('today', 'departure', is_all_days=False, special='today', date=date(2020, 2, 1))
    _check_context('today', 'arrival', is_all_days=False, special='today', date=date(2020, 2, 1))
    _check_context('tomorrow', 'departure', is_all_days=False, special='tomorrow', date=date(2020, 2, 2))
    _check_context('tomorrow', 'arrival', is_all_days=False, special='tomorrow', date=date(2020, 2, 2))
    _check_context('all-days', 'departure', is_all_days=True, special='all-days')
    _check_context('all-days', 'arrival', is_all_days=True, special='all-days')
    _check_context('2020-03-03', 'departure', is_all_days=False, date=date(2020, 3, 3))
    _check_context('2020-03-03', 'arrival', is_all_days=False, date=date(2020, 3, 3))
    _check_context('2019-12-30', 'departure', ex_message_date='2019-12-30')
    _check_context('2021-01-10', 'departure', ex_message_date='2021-01-10')


def _check_context_time(request_date, request_time_after=None, request_time_before=None, dt_after=None, dt_before=None):
    station = create_station()
    context = StationPageContext(station, request_date, 'departure', environment.now_aware())
    context.set_time(request_time_after, request_time_before)

    if dt_after:
        assert context.dt_after.isoformat() == dt_after
    else:
        assert not hasattr(context, 'dt_after')

    if dt_before:
        assert context.dt_before.isoformat() == dt_before
    else:
        assert not hasattr(context, 'dt_before')

    assert context.dt_now.isoformat() == '2020-02-01T00:00:00+03:00'


@replace_now('2020-02-01')
def test_station_page_context_time():
    _check_context_time('all-days', None, None, None, None)
    _check_context_time('all-days', time(1), time(2), None, None)
    _check_context_time('today', None, None, '2020-02-01T00:00:00+03:00', '2020-02-02T00:00:00+03:00')
    _check_context_time('today', time(1), time(2), '2020-02-01T01:00:00+03:00', '2020-02-01T02:00:00+03:00')
    _check_context_time('tomorrow', None, None, '2020-02-02T00:00:00+03:00', '2020-02-03T00:00:00+03:00')
    _check_context_time('tomorrow', time(1), time(2), '2020-02-02T01:00:00+03:00', '2020-02-02T02:00:00+03:00')
    _check_context_time('2020-02-04', None, None, '2020-02-04T00:00:00+03:00', '2020-02-05T00:00:00+03:00')
    _check_context_time('2020-02-04', time(1), time(2), '2020-02-04T01:00:00+03:00', '2020-02-04T02:00:00+03:00')

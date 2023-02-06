# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import datetime

import mock
import pytest

from common.data_api.yandex_bus.factories import create_segment
from common.tester.factories import create_settlement, create_station
from travel.rasp.morda_backend.morda_backend.tariffs.bus.service import collect_yandex_buses_results, make_ybus_tariff_keys


@pytest.mark.dbuser
@pytest.mark.parametrize("data_by_dates,dates,expected", [
    ({'2001-01-01': [1, 2, 3], '2001-01-02': [4, 5]}, ['2001-01-01', '2001-01-02'], [1, 2, 3, 4, 5]),
    ({'2001-01-01': [1, 2, 3]}, ['2001-01-01', '2001-01-02'], [1, 2, 3]),
    ({}, ['2001-01-01', '2001-01-02'], []),
])
def test_collect_result(data_by_dates, dates, expected):
    def get_yandex_buses_results(point_from, point_to, date, bus_settlement_keys):
        return data_by_dates.get(date, []), False

    with mock.patch('travel.rasp.morda_backend.morda_backend.tariffs.bus.service.get_yandex_buses_results', side_effect=get_yandex_buses_results):
        assert collect_yandex_buses_results(None, None, dates, bus_settlement_keys=False) == (expected, False)


def test_collect_result_querying_true():
    def get_yandex_buses_results(point_from, point_to, date, bus_settlement_keys):
        return [], date == '2001-01-01'

    with mock.patch('travel.rasp.morda_backend.morda_backend.tariffs.bus.service.get_yandex_buses_results', side_effect=get_yandex_buses_results):
        assert collect_yandex_buses_results(None, None, ['2001-01-01', '2001-01-02'], bus_settlement_keys=False) == ([], True)


@pytest.mark.dbuser
def test_make_ybus_tariff_keys():
    station_from = create_station()
    station_to = create_station()
    departure = datetime(2001, 1, 1)
    segment = create_segment(station_from=station_from, station_to=station_to, departure=departure, number='000')
    assert make_ybus_tariff_keys(segment) == _format_keys(segment)

    segment = segment._replace(number=None)
    assert make_ybus_tariff_keys(segment) == _format_keys(segment)

    segment = segment._replace(
        station_from=create_station(settlement=create_settlement()),
        station_to=create_station(settlement=create_settlement()))
    assert make_ybus_tariff_keys(segment, bus_settlement_keys=True) == _format_keys(segment)
    assert make_ybus_tariff_keys(segment, bus_settlement_keys=False) == _format_keys(segment, settlement_keys=False)

    segment = segment._replace(
        station_from=create_station(),
        station_to=create_settlement())
    assert make_ybus_tariff_keys(segment, bus_settlement_keys=True) == _format_keys(segment)
    assert make_ybus_tariff_keys(segment, bus_settlement_keys=False) == _format_keys(segment, settlement_keys=False)


def _format_keys(segment, settlement_keys=True):
    from_key = segment.station_from.point_key
    departure = segment.departure
    station_to = segment.station_to
    to_station_key = station_to.point_key
    number = segment.number
    keys = [
        'ybus {} {:%Y-%m-%dT%H:%M} {} {}'.format(from_key, departure, to_station_key, number),
        'ybus {} {:%Y-%m-%dT%H:%M} {}'.format(from_key, departure, to_station_key),
    ]

    if not settlement_keys:
        return keys

    if hasattr(station_to, 'settlement') and station_to.settlement:
        to_settlement_key = station_to.settlement.point_key
        keys += [
            'ybus {} {:%Y-%m-%dT%H:%M} {} {}'.format(from_key, departure, to_settlement_key, number),
            'ybus {} {:%Y-%m-%dT%H:%M} {}'.format(from_key, departure, to_settlement_key),
        ]
    return keys

# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import json
from datetime import datetime, time

import pytest
from django.test import Client
from hamcrest import assert_that, contains, has_entries

from common.models.currency import Price
from common.models.geo import StationMajority
from common.models.transport import TransportType
from common.tester.factories import create_station, create_settlement, create_rthread_segment, create_thread, create_supplier
from common.tester.utils.datetime import replace_now
from common.tester.utils.replace_setting import replace_dynamic_setting
from common.utils.date import MSK_TZ
from travel.rasp.library.python.common23.models.tariffs.tester.factories import create_thread_tariff
from travel.rasp.morda_backend.morda_backend.tariffs.static.serialization import StaticTariffQuery
from travel.rasp.morda_backend.morda_backend.tariffs.static.service import (
    get_static_tariffs, make_static_tariff_key, make_static_segment_keys, make_thread_key
)


def _get_static_tariffs(point_from, point_to, *dates):
    return list(get_static_tariffs(StaticTariffQuery(point_from=point_from, point_to=point_to,
                                                     dates=list(dates))))


@pytest.mark.dbuser
def test_thread_tariffs():
    tariff = create_thread_tariff()
    departure = datetime(2000, 1, 1)

    tariffs = _get_static_tariffs(tariff.station_from, tariff.station_to, departure)
    expected = contains(
        has_entries({'key': make_static_tariff_key(tariff, departure),
                     'classes': {tariff.t_type.code: {'price': Price(tariff.tariff), 'several_prices': False}},
                     'supplier': tariff.supplier,
                     }))

    assert_that(tariffs, expected)


@pytest.mark.dbuser
@replace_dynamic_setting('MORDA_BACKEND_ADD_TARIFF_FEE', True)
def test_e_traffic_fee():
    tariff = create_thread_tariff(supplier=create_supplier(code='e-traffic', can_buy_ru=True), tariff='100')
    departure = datetime(2000, 1, 1)

    tariffs = _get_static_tariffs(tariff.station_from, tariff.station_to, departure)

    assert tariffs[0]['classes']['bus']['price'].value == 107.0


@pytest.mark.dbuser
@pytest.mark.parametrize('partner_code, now, order_data, expected', (
    ('Donavto', datetime(2000, 1, 1, 7), {'station_from_code': 1,
                                          'station_to_code': 2},
     has_entries({'partner_order_request': {'url': u'http://www.donbilet.ru/main',
                                            'headers': {u'Content-Type': u'text/html; charset=utf-8'},
                                            'http_method': u'POST',
                                            'params': {u'pto': 2, u'pdate': 0, u'pfrom': 1}}})),
    ('e-traffic', datetime(2000, 1, 1), {'etraffic_races': {'2000-01-01 12:00:00': True},
                                         'depot': 'siktivkar',
                                         'to_code': 117},
     has_entries({'partner_order_request': {'url': u'http://e-traffic.ru/purchase',
                                            'http_method': u'GET',
                                            'params': {u'date': '2000-01-01 12:00:00',
                                                       u'station': 117,
                                                       u'depot': u'siktivkar',
                                                       u'race': True}}})),
    ('infobus', datetime(1999, 12, 30), {},
     has_entries({'order_url': '/buy/?arrival=2000-01-01+12%3A42%3A00&departure=2000-01-01+12%3A00%3A00&'
                               'station_to=122&station_from=121&date=2000-01-01&point_to=s122&tariff=100.5+RUR&'
                               'thread=THREAD_ID&title=from+-+to&t_type=bus&point_from=s121'})),
))
def test_get_order_url(partner_code, now, order_data, expected):
    with replace_now(now):
        departure = MSK_TZ.localize(datetime(2000, 1, 1, 12))

        tariff = create_thread_tariff(supplier=create_supplier(code=partner_code, can_buy_ru=True),
                                      time_from=time(12, 0),
                                      data=json.dumps(order_data),
                                      station_from=create_station(id=121, title='from'),
                                      station_to=create_station(id=122, title='to'),
                                      thread_uid='THREAD_ID')
        tariffs = _get_static_tariffs(tariff.station_from, tariff.station_to, departure)
        assert_that(tariffs, contains(has_entries({
            'classes': has_entries({
                tariff.t_type.code: expected,
            }),
        })))


@pytest.mark.dbuser
def test_static_tariffs():
    station_from = create_station()
    station_to = create_station()
    departure = MSK_TZ.localize(datetime(2000, 1, 1, 12))
    tariff = create_thread_tariff(station_from=station_from, station_to=station_to, tariff=100.)
    response = json.loads(Client().get('/ru/tariffs/static-tariffs/', {
        'pointFrom': station_from.point_key,
        'pointTo': station_to.point_key,
        'date': '2000-01-01',
        'national_version': 'ru'
    }).content)

    assert 'tariffs' in response
    assert len(response['tariffs']) == 1

    assert_that(response['tariffs'][0], has_entries({
        'classes': has_entries({
            tariff.t_type.code: has_entries({
                'price': has_entries({'value': 100., 'currency': 'RUR'})
            })
        }),
        'key': make_static_tariff_key(tariff, departure),
    }))


@pytest.mark.dbuser
def test_static_tariffs_thread():
    station_from = create_station()
    station_to = create_station()
    departure = MSK_TZ.localize(datetime(2000, 1, 1, 12))
    create_thread_tariff(station_from=station_from, station_to=station_to, tariff=10., thread_uid='THREAD_ID1')
    tariff = create_thread_tariff(station_from=station_from, station_to=station_to, tariff=20., thread_uid='THREAD_ID2')
    response = json.loads(Client().get('/ru/tariffs/static-tariffs/', {
        'pointFrom': station_from.point_key,
        'pointTo': station_to.point_key,
        'date': '2000-01-01',
        'national_version': 'ru',
        'thread_uid': 'THREAD_ID2'
    }).content)

    assert 'tariffs' in response
    assert len(response['tariffs']) == 1

    assert_that(response['tariffs'][0], has_entries({
        'classes': has_entries({
            tariff.t_type.code: has_entries({
                'price': has_entries({'value': 20., 'currency': 'RUR'})
            })
        }),
        'key': make_static_tariff_key(tariff, departure),
        'supplier': has_entries({'code': tariff.supplier.code,
                                 'logo': ''}),
    }))


@pytest.mark.dbuser
@pytest.mark.parametrize('partner_code, now, order_data, expected', (
    ('Donavto', datetime(2000, 1, 1, 7), {'station_from_code': 1,
                                          'station_to_code': 2},
     has_entries({'partnerOrderRequest': {'url': u'http://www.donbilet.ru/main',
                                          'headers': {'Content-Type': 'text/html; charset=utf-8'},
                                          'httpMethod': u'POST',
                                          'params': {'pto': '2',
                                                     'pdate': '0',
                                                     'pfrom': '1'}}})),
    ('e-traffic', datetime(2000, 1, 1), {'etraffic_races': {'2000-01-01 12:00:00': True},
                                         'depot': 'siktivkar',
                                         'to_code': 117},
     has_entries({'partnerOrderRequest': {'url': u'http://e-traffic.ru/purchase',
                                          'httpMethod': u'GET',
                                          'params': {'date': '2000-01-01 12:00:00',
                                                     'station': '117',
                                                     'depot': 'siktivkar',
                                                     'race': 'True'}}})),
    ('infobus', datetime(1999, 12, 30), {},
     has_entries({'orderUrl': '/buy/?arrival=2000-01-01+12%3A42%3A00&departure=2000-01-01+12%3A00%3A00&'
                              'station_to=122&station_from=121&date=2000-01-01&point_to=s122&tariff=100.5+RUR&'
                              'thread=THREAD_ID&title=from+-+to&t_type=bus&point_from=s121'})),
))
def test_static_tariffs_bus_partners(partner_code, now, order_data, expected):
    with replace_now(now):
        departure = MSK_TZ.localize(datetime(2000, 1, 1, 12))
        tariff = create_thread_tariff(supplier=create_supplier(code=partner_code, can_buy_ru=True),
                                      time_from=time(12, 0),
                                      data=json.dumps(order_data),
                                      station_from=create_station(id=121, title='from'),
                                      station_to=create_station(id=122, title='to'),
                                      thread_uid='THREAD_ID')
        response = json.loads(Client().get('/ru/tariffs/static-tariffs/', {
            'pointFrom': tariff.station_from.point_key,
            'pointTo': tariff.station_to.point_key,
            'date': '2000-01-01',
            'national_version': 'ru'
        }).content)

        assert 'tariffs' in response
        assert len(response['tariffs']) == 1

        assert_that(response['tariffs'][0], has_entries({
            'classes': has_entries({
                tariff.t_type.code: expected
            }),
            'key': make_static_tariff_key(tariff, departure),
        }))


@pytest.mark.dbuser
def test_thread_tariffs_settlement():
    settlement = create_settlement()
    tariff = create_thread_tariff(station_from={'majority': StationMajority.MAIN_IN_CITY_ID, 'settlement': settlement},
                                  settlement_from=settlement)

    departure = datetime(2000, 1, 1)

    tariffs = _get_static_tariffs(settlement, tariff.station_to, departure)
    expected = contains(
        has_entries({'key': make_static_tariff_key(tariff, departure),
                     'classes': {tariff.t_type.code: {'price': Price(tariff.tariff), 'several_prices': False}}
                     }))

    assert_that(tariffs, expected)


@pytest.mark.dbuser
def test_thread_tariffs_station2settlement():
    settlement_a = create_settlement()
    station_a = create_station(majority=StationMajority.MAIN_IN_CITY_ID)
    settlement_a.station2settlement_set.create(station=station_a)

    settlement_b = create_settlement()
    station_b = create_station(majority=StationMajority.MAIN_IN_CITY_ID, settlement=settlement_b)

    tariff_ab = create_thread_tariff(tariff=10, station_from=station_a, station_to=station_b)
    tariff_ba = create_thread_tariff(tariff=20, station_from=station_b, station_to=station_a)

    departure = datetime(2000, 1, 1)

    forward_tariffs = _get_static_tariffs(settlement_a, settlement_b, departure)
    assert len(forward_tariffs) == 1
    assert forward_tariffs[0]['classes']['bus']['price'].value == tariff_ab.tariff

    backward_tariffs = _get_static_tariffs(settlement_b, settlement_a, departure)
    assert len(backward_tariffs) == 1
    assert backward_tariffs[0]['classes']['bus']['price'].value == tariff_ba.tariff


@pytest.mark.dbuser
def test_make_static_tariff_key():
    """
    Проверяем генерацию ключей для сегмента и тарифа.
    1. Ключ генерируется только для нужных сегментов.
    2. Ключ для сегмента и ключ для тарифа совпадают.
    """
    station_from = create_station()
    station_to = create_station()
    t_type = TransportType.objects.get(id=TransportType.BUS_ID)
    thread = create_thread(t_type=t_type)
    segment = create_rthread_segment(station_from=station_from, station_to=station_to, thread=thread, number='118')
    assert (make_thread_key(segment.departure, station_from.id, station_to.id, thread.uid)
            in make_static_segment_keys(segment))

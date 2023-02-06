# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import json

import mock
import pytest
from django.conf import settings
from django.test import Client
from hamcrest import assert_that, has_entries, contains_inanyorder, has_property, contains

from common.models.currency import Currency
from travel.rasp.library.python.common23.tester.factories import create_station, create_settlement, create_currency
from travel.rasp.library.python.common23.models.tariffs.tester.factories import create_thread_tariff


@pytest.yield_fixture(scope='module', autouse=True)
def mock_rates():
    with mock.patch.object(Currency, 'fetch_rates') as m_fetch_rates:
        def fetch_rates(currencies, geoid, base_currency):
            assert_that(currencies, contains_inanyorder(has_property('code', 'RUR'), has_property('code', 'UAH')))

            if geoid == settings.MOSCOW_GEO_ID:
                return None, {'RUB': 1, 'UAH': 2.5}
            elif geoid == settings.KIEV_GEO_ID:
                return None, {'RUB': 1, 'UAH': 2.}
        m_fetch_rates.side_effect = fetch_rates
        yield


@pytest.yield_fixture(scope='module', autouse=True)
def currencies_objects():
    c1 = create_currency(code='RUR', iso_code='RUB')
    c2 = create_currency(code='UAH', iso_code='UAH')
    yield
    c1.delete()
    c2.delete()


@pytest.mark.dbuser
def test_station2station():
    station_from = create_station()
    station_to = create_station()
    tariff = create_thread_tariff(station_from=station_from, station_to=station_to, tariff=100.)
    response = json.loads(Client().get('/ru/tariffs/min-static-tariffs/', {
        'pointFrom': station_from.point_key,
        'pointTo': station_to.point_key,
        'national_version': 'ru'
    }).content)

    assert 'tariffs' in response
    assert len(response['tariffs']) == 1

    assert_that(response['tariffs'][0], has_entries({
        'classes': has_entries({
            tariff.t_type.code: has_entries({
                'price': has_entries({'value': 100., 'currency': 'RUB'})
            })
        }),
        'key': 'static {} {} {}'.format(station_from.id, station_to.id, tariff.thread_uid),
    }))


@pytest.mark.dbuser
def test_settlement2station():
    settlement_from = create_settlement()
    station_from = create_station(settlement=settlement_from)
    station_to = create_station()
    tariff = create_thread_tariff(station_from=station_from, station_to=station_to, tariff=100.)
    response = json.loads(Client().get('/ru/tariffs/min-static-tariffs/', {
        'pointFrom': settlement_from.point_key,
        'pointTo': station_to.point_key,
        'national_version': 'ru'
    }).content)

    assert_that(response['tariffs'][0], has_entries({
        'classes': has_entries({
            tariff.t_type.code: has_entries({
                'price': has_entries({'value': 100., 'currency': 'RUB'})
            })
        }),
        'key': 'static {} {} {}'.format(station_from.id, station_to.id, tariff.thread_uid),
    }))


@pytest.mark.dbuser
def test_several_prices():
    station_from = create_station()
    station_to = create_station()
    thread_uid = 'thread_uid'
    min_tariff = create_thread_tariff(station_from=station_from, station_to=station_to, tariff=100.,
                                      thread_uid=thread_uid)
    create_thread_tariff(station_from=station_from, station_to=station_to, tariff=120., thread_uid=thread_uid)
    response = json.loads(Client().get('/ru/tariffs/min-static-tariffs/', {
        'pointFrom': station_from.point_key,
        'pointTo': station_to.point_key,
        'national_version': 'ru'
    }).content)

    assert_that(response['tariffs'][0], has_entries({
        'classes': has_entries({
            min_tariff.t_type.code: has_entries({
                'price': has_entries({'value': 100., 'currency': 'RUB'})
            })
        }),
        'key': 'static {} {} {}'.format(station_from.id, station_to.id, thread_uid),
    }))


@pytest.mark.dbuser
def test_several_prices_other_currency():
    station_from = create_station()
    station_to = create_station()
    thread_uid_1 = 'thread_uid_1'
    min_tariff_1 = create_thread_tariff(station_from=station_from, station_to=station_to, tariff=200.,
                                        thread_uid=thread_uid_1)
    create_thread_tariff(station_from=station_from, station_to=station_to, tariff=100., thread_uid=thread_uid_1,
                         currency='UAH')

    thread_uid_2 = 'thread_uid_2'
    create_thread_tariff(station_from=station_from, station_to=station_to, tariff=100., thread_uid=thread_uid_2)
    min_tariff_2 = create_thread_tariff(station_from=station_from, station_to=station_to, tariff=10.,
                                        thread_uid=thread_uid_2, currency='UAH')

    response = json.loads(Client().get('/ru/tariffs/min-static-tariffs/', {
        'pointFrom': station_from.point_key,
        'pointTo': station_to.point_key,
        'national_version': 'ru'
    }).content)

    assert_that(response['tariffs'], contains_inanyorder(
        has_entries({
            'classes': has_entries({
                min_tariff_1.t_type.code: has_entries({
                    'price': has_entries({'value': 200., 'currency': 'RUB'}),
                })
            }),
            'key': 'static {} {} {}'.format(station_from.id, station_to.id, thread_uid_1),
        }),
        has_entries({
            'classes': has_entries({
                min_tariff_2.t_type.code: has_entries({
                    'price': has_entries({'value': 10., 'currency': 'UAH'}),
                })
            }),
            'key': 'static {} {} {}'.format(station_from.id, station_to.id, thread_uid_2),
        }),
    ))


@pytest.mark.dbuser
def test_several_prices_other_currency_different_national_version():
    station_from = create_station()
    station_to = create_station()
    thread_uid_1 = 'thread_uid_1'
    tariff_1 = create_thread_tariff(station_from=station_from, station_to=station_to,
                                    tariff=220., thread_uid=thread_uid_1)
    tariff_2 = create_thread_tariff(station_from=station_from, station_to=station_to,
                                    tariff=100., thread_uid=thread_uid_1, currency='UAH')

    response = json.loads(Client().get('/ru/tariffs/min-static-tariffs/', {
        'pointFrom': station_from.point_key,
        'pointTo': station_to.point_key,
        'national_version': 'ru'
    }).content)

    assert_that(response['tariffs'], contains(
        has_entries({
            'classes': has_entries({
                tariff_1.t_type.code: has_entries({
                    'price': has_entries({'value': 220., 'currency': 'RUB'}),
                })
            }),
            'key': 'static {} {} {}'.format(station_from.id, station_to.id, thread_uid_1),
        }),
    ), 'Конвертация по курсу 2.5')

    response = json.loads(Client().get('/ru/tariffs/min-static-tariffs/', {
        'pointFrom': station_from.point_key,
        'pointTo': station_to.point_key,
        'national_version': 'ua'
    }).content)

    assert_that(response['tariffs'], contains(
        has_entries({
            'classes': has_entries({
                tariff_2.t_type.code: has_entries({
                    'price': has_entries({'value': 100., 'currency': 'UAH'}),
                })
            }),
            'key': 'static {} {} {}'.format(station_from.id, station_to.id, thread_uid_1),
        }),
    ), 'Конвертация по курсу 2.')

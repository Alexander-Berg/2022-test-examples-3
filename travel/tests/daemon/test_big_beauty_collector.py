# -*- coding: utf-8 -*-
from datetime import datetime

import pytz
from mock import Mock, patch

from travel.avia.library.python.common.models.partner import Partner
from travel.avia.library.python.common.models.geo import Settlement
from travel.avia.library.python.tester.testcase import TestCase
from travel.avia.library.python.tester.factories import create_top_flight
from travel.avia.library.python.ticket_daemon.protobuf_converting.big_wizard.search_result_converter import SearchResultConverter
from travel.proto.avia.wizard.search_result_pb2 import SearchResult

from travel.avia.ticket_daemon.ticket_daemon.api.flights import Variant
from travel.avia.library.python.ticket_daemon.memo import reset_all_caches
from travel.avia.ticket_daemon.ticket_daemon.api.query import Query
from travel.avia.ticket_daemon.ticket_daemon.api.result import Result
import travel.avia.ticket_daemon.ticket_daemon.daemon.big_beauty_collector as bbc
from travel.avia.ticket_daemon.ticket_daemon.lib.currency import Price
from travel.avia.library.python.ticket_daemon.date import unixtime

DEFAULT_PRICE = 1000.
DEFAULT_PARTNER_CODE = 'test_partner'
DEFAULT_QID = 'test_qid'
FLIGHT_DEFAULTS = {
    'departure': '2017-09-01T03:00:00',
    'arrival': '2017-09-01T05:00:00',
    'airline_id': 42,
    'from_id': 2130,
    'to_id': 20,
}


def create_flight(departure, arrival, airline_id, from_id, to_id, number):
    key = departure[2:-2].replace('-', '').replace(':', '').replace('T', '') + number.replace(' ', '')
    return {
        "departure": {
            "local": departure,
            "tzname": "Europe/Moscow",
            "offset": 180.0,
        },
        "arrival": {
            "local": arrival,
            "offset": 180.0,
            "tzname": "Europe/Moscow"
        },
        "aviaCompany": airline_id,
        "company": airline_id,
        "companyTariff": 4200,
        "from": from_id,
        "key": key,
        "number": number,
        "to": to_id
    }


def create_fare(route, baggage, partner_code=DEFAULT_PARTNER_CODE, price=DEFAULT_PRICE, created=1504224000, expire=1504224001):
    tariff = {
        "currency": "RUR",
        "value": price
    }
    return {
        "charter": False,
        "created": created,
        "expire": expire,
        "route": route,
        "baggage": baggage,
        "tariff": tariff,
        'partner': partner_code,
        'conversion_partner': partner_code,
        'tariffs': {
            'with_baggage': {
                'price': tariff,
                'partner': partner_code,
                'conversion_partner': partner_code,
                'baggage': baggage,
                'created_at': created,
                'expire_at': expire,
            }
        },
        'popularity': 0,
        'promo': {
            'code': 'promo',
            'end_ts': expire,
        },
    }


def get_test_result(flights, fares, is_experimental=False):
    return bbc.BigBeautyResult(
        query=Mock(),
        flights=flights,
        fares=fares,
        version=0,
        polling_status=bbc.PollingStatus([]),
        partners=set(),
        is_experimental=is_experimental,
    )


def get_test_result_by_partner(flights, fares, partner_code=DEFAULT_PARTNER_CODE):
    return bbc.BigBeautyResultByPartner(
        query=Mock(),
        flights=flights,
        fares=fares,
        version=0,
        polling_status=bbc.PollingStatus([]),
        partners=set(),
        partner_code=partner_code,
    )


def get_search_result():
    return SearchResult(
        qid=DEFAULT_QID,
        version=0,
        flights={},
        fares={},
        offers_count=0,
        polling_status={},
    )


class TestBigBeautyCollector(TestCase):
    def setUp(self):
        mow = Settlement(id=213)
        sip = Settlement(id=2)
        self.query = Query(
            point_from=mow,
            point_to=sip,
            passengers={'adults': 1, 'children': 0, 'infants': 0},
            date_forward=pytz.UTC.localize(datetime.now()),
            service='ticket',
            national_version='ru',
        )
        self.query.id = 'test_qid'

    def test_remove_outdated_variants_from_stored_saas_variants(self):
        """Тест должен проверять, что устаревшие предложения удаляются"""
        unixtime_ = unixtime()
        flight_for_old = create_flight(number='SU 0', **FLIGHT_DEFAULTS)
        flight_for_new = create_flight(number='SU 1', **FLIGHT_DEFAULTS)

        flights = {
            flight_for_old['key']: flight_for_old,
            flight_for_new['key']: flight_for_new
        }

        old_price_fare = create_fare(
            route=((flight_for_old['key'],), ()),
            baggage=[['1d1d23d'], []],
            price=1000.,
            created=unixtime_ - 100, expire=unixtime_ - 1,
        )
        actual_price_fare = create_fare(
            route=((flight_for_new['key'],), ()),
            baggage=[['1d1d23d'], []],
            price=2000.,
            created=unixtime_ - 100, expire=unixtime_ + 200,
        )
        fares = [actual_price_fare, old_price_fare]

        stored_result_mock = SearchResultConverter().to_protobuf(
            {
                'qid': 'test_qid',
                'version': 1,
                'fares': fares,
                'flights': flights,
                'offers_count': len(fares),
            }
        )

        self.query.partners = [Partner(code=DEFAULT_PARTNER_CODE)]
        collector = bbc.BigBeautyCollector(self.query, stored_result_mock, Mock())
        assert len(collector._variants_by_route) == 1
        assert collector._version == 1
        self.assertDictEqual(
            actual_price_fare, collector._variants_by_route.values()[0]
        )

    def test_save_from_aviacompany_partners_prices_if_prices_are_equal(self):
        """Сохраняем цены от АК, если цена совпала"""
        partner = Partner(
            code='test_partner', id=13, enabled_in_wizard_ru=True
        )
        airline_partner = Partner(
            code='test_airline_partner', id=14, is_aviacompany=True,
            enabled_in_wizard_ru=True
        )

        def get_variants(p):
            variant = Variant()
            variant.tariff = variant.national_tariff = Price(1000.)
            variant.partner = p
            return [variant]

        collector = bbc.BigBeautyCollector(self.query, SearchResult(), Mock())
        collector.store = Mock()
        result = Result(
            self.query, partner, get_variants(partner), store_time=1
        )
        collector.add(result)
        aviacompany_result = Result(
            self.query, airline_partner, get_variants(airline_partner), store_time=1
        )
        collector.add(aviacompany_result)
        assert collector._updates == 2
        assert len(collector._variants_by_route.values()) == 1
        assert collector._variants_by_route.values()[0]['partner'] == airline_partner.code

        collector.add(result)
        assert collector._updates == 2

    def test_update_partner_variants_from_stored_saas_variants(self):
        """Проверяем обновление цены от одного и того же партнера"""
        cache_ttl = 5
        partner = Partner(
            code='test_partner',
            id=13,
            enabled_in_wizard_ru=True,
            variant_cache_ttl=cache_ttl,
        )
        unixtime_now = unixtime()

        def get_variants(p, price):
            variant = Variant()
            variant.tariff = variant.national_tariff = Price(price)
            variant.partner = p
            return [variant]

        collector = bbc.BigBeautyCollector(self.query, SearchResult(), Mock())

        collector.store = Mock()
        result = Result(
            self.query, partner, get_variants(partner, 1000.), store_time=1,
        )
        result.created = unixtime_now - ((cache_ttl + 5) * 60 + 10)
        new_result = Result(
            self.query, partner, get_variants(partner, 2000.), store_time=1,
        )
        last_result = Result(
            self.query, partner, get_variants(partner, 3000.), store_time=1,
        )

        collector.add(result)
        collector.add(new_result)
        assert collector._updates == 2
        assert len(collector._variants_by_route.values()) == 1
        assert collector._variants_by_route.values()[0]['tariff']['value'] == 2000.

        collector.add(last_result)
        assert collector._updates == 2
        assert len(collector._variants_by_route.values()) == 1
        assert collector._variants_by_route.values()[0]['tariff']['value'] == 2000.

    def test_partner_done(self):
        partners = [
            Partner(code='test_partner_1', id=1),
            Partner(code='test_partner_2', id=2),
        ]
        self.query.partners = partners
        collector = bbc.BigBeautyCollector(self.query, SearchResult(), Mock())
        collector.store = Mock()

        for p in partners:
            assert collector.all_replied is False
            collector.partner_done(p)

        assert collector.all_replied is True

    def test_empty_partners(self):
        self.query.partners = []
        collector = bbc.BigBeautyCollector(self.query, SearchResult(), Mock())
        collector.store = Mock()

        assert collector.all_replied is True

    def test_add_better_conversion_partner(self):
        reset_all_caches()
        cache_ttl = 5
        partner = Partner(
            code='test_partner',
            id=13,
            billing_order_id=13,
            enabled_in_wizard_ru=True,
            variant_cache_ttl=cache_ttl,
        )
        conversion_partner = Partner(
            code='test_conversion_partner',
            id=14,
            billing_order_id=14,
            enabled_in_wizard_ru=True,
            variant_cache_ttl=cache_ttl,
        )

        def get_variants(p, price):
            variant = Variant()
            variant.tariff = variant.national_tariff = Price(price)
            variant.partner = p
            return [variant]

        collector = bbc.BigBeautyCollector(self.query, SearchResult(), Mock())
        collector.store = Mock()
        result = Result(
            self.query, partner, get_variants(partner, 1000.), store_time=1,
        )
        conversion_partner_result = Result(
            self.query, conversion_partner, get_variants(conversion_partner, 1000.), store_time=1,
        )

        with patch(
            'travel.avia.ticket_daemon.ticket_daemon.api.models_utils.conversions._conversions',
            return_value={partner.billing_order_id: 0.1, conversion_partner.billing_order_id: 0.5}
        ), patch(
            'travel.avia.ticket_daemon.ticket_daemon.api.models_utils.partners._partner_by_code',
            return_value={partner.code: partner, conversion_partner.code: conversion_partner}
        ):
            collector.add(result)
            collector.add(conversion_partner_result)
            assert collector._updates == 2
            assert len(collector._variants_by_route.values()) == 1
            actual_route = collector._variants_by_route.values()[0]
            assert actual_route['tariff']['value'] == 1000.
            assert actual_route['partner'] == partner.code
            assert actual_route['conversion_partner'] == conversion_partner.code
            assert actual_route['tariffs']['with_baggage']['partner'] == partner.code
            assert actual_route['tariffs']['with_baggage']['conversion_partner'] == conversion_partner.code
            collector.add(result)
            assert collector._updates == 2


class TestBigBeautyResult(TestCase):
    def setUp(self):
        reset_all_caches()
        mow = Settlement(id=213)
        sip = Settlement(id=2)
        self.query = Query(
            point_from=mow,
            point_to=sip,
            passengers={'adults': 1, 'children': 0, 'infants': 0},
            date_forward=pytz.UTC.localize(datetime.now()),
            service='ticket',
            national_version='ru',
        )
        self.query.id = 'test_qid'

    def test_sorting_by_popularity(self):
        flight_defaults = {
            'departure': '2017-09-01T03:00:00',
            'arrival': '2017-09-01T05:00:00',
            'airline_id': 42,
            'from_id': 2130,
            'to_id': 20,
        }
        unpopular_flight = create_flight(number='SU 0', **flight_defaults)
        most_popular_flight = create_flight(number='SU 1', **flight_defaults)
        popular_flight = create_flight(number='SU 2', **flight_defaults)
        cheapest_flight = create_flight(number='SU 3', **flight_defaults)

        create_top_flight(flights=most_popular_flight['number'], redirects=10)
        create_top_flight(flights=unpopular_flight['number'], redirects=1)
        create_top_flight(flights=popular_flight['number'], redirects=4)

        expected_flight_prices_order = (
            (cheapest_flight, 1000.),
            (most_popular_flight, 4000.),
            (popular_flight, 3000),
            (unpopular_flight, 2000.)
        )

        flights = {f['key']: f for f, _ in expected_flight_prices_order}

        fares = {
            str(idx): create_fare(
                route=[[f['key']], []],
                baggage=[['1d1d23d'], []],
                price=price
            )
            for idx, (f, price) in enumerate(expected_flight_prices_order, start=1)
        }

        actual_result = get_test_result(flights, fares)

        for (flight, price), actual_fare in zip(
            expected_flight_prices_order, actual_result.to_dict()['fares']
        ):
            assert actual_fare['route'][0][0] == flight['key']
            assert actual_fare['tariff']['value'] == price

    def test_write_into_ydb_if_not_experimental(self):
        """Проверяем запись в основной кэш"""
        collector_path = 'travel.avia.ticket_daemon.ticket_daemon.daemon.big_beauty_collector'
        with patch(collector_path + '.BigBeautyResult.wizard_cache', Mock()):
            result = get_test_result({}, {}, is_experimental=False)

            result.store()
            assert result.wizard_cache.set.call_count == 1

    def test_write_into_experimental_ydb_if_experimental(self):
        """Проверяем запись в экспериментальный кэш"""
        collector_path = 'travel.avia.ticket_daemon.ticket_daemon.daemon.big_beauty_collector'
        with patch(collector_path + '.BigBeautyResult.wizard_cache_experimental', Mock()):
            result = get_test_result({}, {}, is_experimental=True)

            result.store()
            assert result.wizard_cache_experimental.set.call_count == 1

    def test_write_into_partner_ydb(self):
        """Проверяем запись в partner кэш"""
        collector_path = 'travel.avia.ticket_daemon.ticket_daemon.daemon.big_beauty_collector'

        with patch(collector_path + '.BigBeautyResultByPartner.wizard_cache_by_partner', Mock()):
            result = get_test_result_by_partner({}, {})

            result.store()
            assert result.wizard_cache_by_partner.set.call_count == 1

    def test_write_into_ydb_defaults_on_empty_result(self):
        """Проверяем запись в ydb дефолтных значений при пустом SearchResult"""

        result_path = 'travel.avia.ticket_daemon.ticket_daemon.daemon.big_beauty_collector.BigBeautyResult'
        with patch(
                result_path + '.wizard_cache', Mock()
        ):
            def _set(self, query, search_result, filter_state, min_price, expires_at):
                assert min_price == bbc.MAX_PRICE
                assert expires_at == 0

            bbc.BigBeautyResult.wizard_cache.set = _set
            result = get_test_result({}, {})
            result.store()

    def test_write_into_partner_ydb_defaults_on_empty_result(self):
        """Проверяем запись в partner ydb дефолтных значений при пустом SearchResult"""

        result_path = 'travel.avia.ticket_daemon.ticket_daemon.daemon.big_beauty_collector.BigBeautyResultByPartner'
        with patch(
            result_path + '.wizard_cache_by_partner', Mock()
        ):
            def _set(self, query, search_result, filter_state, min_price, expires_at, expires_at_by_partner, ttl_expires_at, partner_code):
                assert min_price == bbc.MAX_PRICE
                assert expires_at == 0
                assert partner_code == DEFAULT_PARTNER_CODE

            bbc.BigBeautyResultByPartner.wizard_cache_by_partner.set = _set
            result = get_test_result_by_partner({}, {}, DEFAULT_PARTNER_CODE)
            result.store()


class TestFilterState(TestCase):
    maxDiff = None

    def test_one_way_one_route(self):
        f1 = create_flight('2017-09-01T03:00:00', '2017-09-01T05:00:00', 42, 2130, 20, 'SU 0')
        f2 = create_flight('2017-09-01T06:00:00', '2017-09-01T09:00:00', 42, 20, 540, 'SU 1')
        flights = {
            f1['key']: f1,
            f2['key']: f2
        }

        fares = {
            '1': create_fare(
                route=[[f1['key'], f2['key']], []],
                baggage=[['1d1d23d', '1d1d23d'], []],
                price=1000.
            )
        }

        expected_result = {
            'transfer': {
                'count': 1, 'hasAirportChange': None, 'hasNight': None,
                'minDuration': 60.0, 'maxDuration': 60.0,
            },
            'airlines': {42},
            'all_airlines': {42},
            'partners': set(),
            'airport': {
                'forwardDeparture': {2130},
                'forwardArrival': {540},
                'forwardTransfers': {20},
                'backwardDeparture': set(),
                'backwardArrival': set(),
                'backwardTransfers': set(),
            },
            'withBaggage': None,
            'time': {
                'forwardDepartureMin': '2017-09-01T03:00:00',
                'forwardDepartureMax': '2017-09-01T03:00:00',
                'forwardArrivalMin': '2017-09-01T09:00:00',
                'forwardArrivalMax': '2017-09-01T09:00:00',
                'backwardDepartureMin': None,
                'backwardDepartureMax': None,
                'backwardArrivalMin': None,
                'backwardArrivalMax': None,

            },
            'prices': {
                'directFlight': None,
                'withBaggage': {'currency': 'RUR', 'value': 1000.0},
                'airports': {
                    'from': {2130: {'currency': 'RUR', 'value': 1000.0}},
                    'to': {540: {'currency': 'RUR', 'value': 1000.0}}
                },
                'airlines': {42: {'currency': 'RUR', 'value': 1000.0}},
                'directAirlines': set(),
                'transfers': {
                    0: None,
                    1: {'currency': 'RUR', 'value': 1000.0},
                    2: {'currency': 'RUR', 'value': 1000.0}
                },
            },
        }

        filter_state = bbc.FilterState(get_test_result(flights, fares))
        self.assertDictEqual(filter_state.to_dict(), expected_result)

    def test_one_way_direct_route(self):
        f1 = create_flight('2017-09-01T03:00:00', '2017-09-01T05:00:00', 42, 2130, 20, 'SU 0')
        flights = {
            f1['key']: f1,
        }

        fares = {
            '1': create_fare(
                route=[[f1['key']], []],
                baggage=[['1d1d23d'], []],
                price=1000.
            )
        }

        expected_result = {
            'transfer': {
                'count': 0, 'hasAirportChange': None, 'hasNight': None,
                'minDuration': None, 'maxDuration': None,
            },
            'airlines': {42},
            'all_airlines': {42},
            'partners': set(),
            'airport': {
                'forwardDeparture': {2130},
                'forwardArrival': {20},
                'forwardTransfers': set(),
                'backwardDeparture': set(),
                'backwardArrival': set(),
                'backwardTransfers': set(),
            },
            'withBaggage': None,
            'time': {
                'forwardDepartureMin': '2017-09-01T03:00:00',
                'forwardDepartureMax': '2017-09-01T03:00:00',
                'forwardArrivalMin': '2017-09-01T05:00:00',
                'forwardArrivalMax': '2017-09-01T05:00:00',
                'backwardDepartureMin': None,
                'backwardDepartureMax': None,
                'backwardArrivalMin': None,
                'backwardArrivalMax': None,

            },
            'prices': {
                'directFlight': {'currency': 'RUR', 'value': 1000.0},
                'withBaggage': {'currency': 'RUR', 'value': 1000.0},
                'airports': {
                    'from': {2130: {'currency': 'RUR', 'value': 1000.0}},
                    'to': {20: {'currency': 'RUR', 'value': 1000.0}}
                },
                'airlines': {42: {'currency': 'RUR', 'value': 1000.0}},
                'directAirlines': {42},
                'transfers': {
                    0: {'currency': 'RUR', 'value': 1000.0},
                    1: {'currency': 'RUR', 'value': 1000.0},
                    2: {'currency': 'RUR', 'value': 1000.0}
                },
            },
        }

        filter_state = bbc.FilterState(get_test_result(flights, fares))
        self.assertDictEqual(filter_state.to_dict(), expected_result)

    def test_one_way_two_routes(self):
        f1 = create_flight('2017-09-01T03:00:00', '2017-09-01T05:00:00', 42,
                           2130, 20, 'SU 0')
        f2 = create_flight('2017-09-01T06:00:00', '2017-09-01T09:00:00', 42,
                           20, 540, 'SU 1')

        f3 = create_flight('2017-09-01T04:00:00', '2017-09-01T06:00:00', 43,
                           2130, 21, 'AB 0')
        f4 = create_flight('2017-09-01T08:00:00', '2017-09-01T11:00:00', 43,
                           21, 541, 'AB 1')

        flights = {
            f1['key']: f1,
            f2['key']: f2,
            f3['key']: f3,
            f4['key']: f4,
        }

        fares = {
            '1': create_fare(
                route=[[f1['key'], f2['key']], []],
                baggage=[['1d1d23d', '1d1d23d'], []],
                price=1000.
            ),
            '2': create_fare(
                route=[[f3['key'], f4['key']], []],
                baggage=[['1d1d23d', '1d1d23d'], []],
                price=900.
            )
        }

        expected_result = {
            'transfer': {
                'count': 1, 'hasAirportChange': None, 'hasNight': None,
                'minDuration': 60.0, 'maxDuration': 120.0,
            },
            'airlines': {42, 43},
            'all_airlines': {42, 43},
            'partners': set(),
            'airport': {
                'forwardDeparture': {2130},
                'forwardArrival': {540, 541},
                'forwardTransfers': {20, 21},
                'backwardDeparture': set(),
                'backwardArrival': set(),
                'backwardTransfers': set(),
            },
            'withBaggage': None,
            'time': {
                'forwardDepartureMin': '2017-09-01T03:00:00',
                'forwardDepartureMax': '2017-09-01T04:00:00',
                'forwardArrivalMin': '2017-09-01T09:00:00',
                'forwardArrivalMax': '2017-09-01T11:00:00',
                'backwardDepartureMin': None,
                'backwardDepartureMax': None,
                'backwardArrivalMin': None,
                'backwardArrivalMax': None,
            },
            'prices': {
                'directFlight': None,
                'withBaggage': {'currency': 'RUR', 'value': 900.0},
                'airports': {
                    'from': {2130: {'currency': 'RUR', 'value': 900.0}},
                    'to': {
                        540: {'currency': 'RUR', 'value': 1000.0},
                        541: {'currency': 'RUR', 'value': 900.0},
                    },
                },
                'airlines': {
                    42: {'currency': 'RUR', 'value': 1000.0},
                    43: {'currency': 'RUR', 'value': 900.0},
                },
                'directAirlines': set(),
                'transfers': {
                    0: None,
                    1: {'currency': 'RUR', 'value': 900.0},
                    2: {'currency': 'RUR', 'value': 900.0}
                },
            },
        }

        filter_state = bbc.FilterState(get_test_result(flights, fares))
        self.assertDictEqual(filter_state.to_dict(), expected_result)

    def test_one_way_two_transfers(self):
        f1 = create_flight('2017-09-01T03:00:00', '2017-09-01T05:00:00', 42,
                           2130, 20, 'SU 0')
        f2 = create_flight('2017-09-01T06:00:00', '2017-09-01T09:00:00', 42,
                           20, 540, 'SU 1')
        f3 = create_flight('2017-09-01T14:00:00', '2017-09-01T16:00:00', 43,
                           540, 123, 'AB 0')

        flights = {
            f1['key']: f1,
            f2['key']: f2,
            f3['key']: f3,
        }

        fares = {
            '1': create_fare(
                route=[[f1['key'], f2['key'], f3['key']], []],
                baggage=[['1d1d23d'] * 3, []],
                price=1000.
            ),
        }

        expected_result = {
            'transfer': {
                'count': 2, 'hasAirportChange': None, 'hasNight': None,
                'minDuration': 60.0, 'maxDuration': 300.0,
            },
            'airlines': set(),
            'all_airlines': {42, 43},
            'partners': set(),
            'airport': {
                'forwardDeparture': {2130},
                'forwardArrival': {123},
                'forwardTransfers': {20, 540},
                'backwardDeparture': set(),
                'backwardArrival': set(),
                'backwardTransfers': set(),
            },
            'withBaggage': None,
            'time': {
                'forwardDepartureMin': '2017-09-01T03:00:00',
                'forwardDepartureMax': '2017-09-01T03:00:00',
                'forwardArrivalMin': '2017-09-01T16:00:00',
                'forwardArrivalMax': '2017-09-01T16:00:00',
                'backwardDepartureMin': None,
                'backwardDepartureMax': None,
                'backwardArrivalMin': None,
                'backwardArrivalMax': None,

            },
            'prices': {
                'directFlight': None,
                'withBaggage': {'currency': 'RUR', 'value': 1000.0},
                'airports': {
                    'from': {2130: {'currency': 'RUR', 'value': 1000.0}},
                    'to': {123: {'currency': 'RUR', 'value': 1000.0}},
                },
                'airlines': dict(),
                'directAirlines': set(),
                'transfers': {
                    0: None,
                    1: None,
                    2: {'currency': 'RUR', 'value': 1000.0},
                },
            },
        }

        filter_state = bbc.FilterState(get_test_result(flights, fares))
        self.assertDictEqual(filter_state.to_dict(), expected_result)

    def test_return_one_route(self):
        f1 = create_flight('2017-09-01T03:00:00', '2017-09-01T05:00:00', 42, 2130, 20, 'SU 0')
        f2 = create_flight('2017-09-01T06:00:00', '2017-09-01T09:00:00', 42, 20, 540, 'SU 1')
        b1 = create_flight('2017-09-10T06:00:00', '2017-09-10T08:00:00', 42, 540, 20, 'SU 00')
        b2 = create_flight('2017-09-10T10:00:00', '2017-09-10T12:00:00', 42, 20, 2130, 'SU 11')
        flights = {
            f1['key']: f1,
            f2['key']: f2,
            b1['key']: b1,
            b2['key']: b2,
        }

        fares = {
            '1': create_fare(
                route=[[f1['key'], f2['key']], [b1['key'], b2['key']]],
                baggage=[['1d1d23d', '1d1d23d'] * 2],
                price=1000.
            )
        }

        expected_result = {
            'transfer': {
                'count': 1, 'hasAirportChange': None, 'hasNight': None,
                'minDuration': 60.0, 'maxDuration': 120.0,
            },
            'airlines': {42},
            'all_airlines': {42},
            'partners': set(),
            'airport': {
                'forwardDeparture': {2130},
                'forwardArrival': {540},
                'forwardTransfers': {20},
                'backwardDeparture': {540},
                'backwardArrival': {2130},
                'backwardTransfers': {20},
            },
            'withBaggage': None,
            'time': {
                'forwardDepartureMin': '2017-09-01T03:00:00',
                'forwardDepartureMax': '2017-09-01T03:00:00',
                'forwardArrivalMin': '2017-09-01T09:00:00',
                'forwardArrivalMax': '2017-09-01T09:00:00',
                'backwardDepartureMin': '2017-09-10T06:00:00',
                'backwardDepartureMax': '2017-09-10T06:00:00',
                'backwardArrivalMin': '2017-09-10T12:00:00',
                'backwardArrivalMax': '2017-09-10T12:00:00',

            },
            'prices': {
                'directFlight': None,
                'withBaggage': {'currency': 'RUR', 'value': 1000.0},
                'airports': {
                    'from': {2130: {'currency': 'RUR', 'value': 1000.0}},
                    'to': {540: {'currency': 'RUR', 'value': 1000.0}},
                },
                'airlines': {42: {'currency': 'RUR', 'value': 1000.0}},
                'directAirlines': set(),
                'transfers': {
                    0: None,
                    1: {'currency': 'RUR', 'value': 1000.0},
                    2: {'currency': 'RUR', 'value': 1000.0},
                },
            },
        }

        filter_state = bbc.FilterState(get_test_result(flights, fares))
        self.assertDictEqual(filter_state.to_dict(), expected_result)

    def test_return_direct_route(self):
        f1 = create_flight('2017-09-01T03:00:00', '2017-09-01T05:00:00', 42, 2130, 20, 'SU 0')
        b1 = create_flight('2017-09-10T14:00:00', '2017-09-10T16:00:00', 42, 21, 2130, 'SU 3')
        flights = {
            f1['key']: f1,
            b1['key']: b1,
        }

        fares = {
            '1': create_fare(
                route=[[f1['key']], [b1['key']]],
                baggage=[['1d1d23d'] * 2],
                price=1000.
            )
        }

        expected_result = {
            'transfer': {
                'count': 0, 'hasAirportChange': None, 'hasNight': None,
                'minDuration': None, 'maxDuration': None,
            },
            'airlines': {42},
            'all_airlines': {42},
            'partners': set(),
            'airport': {
                'forwardDeparture': {2130},
                'forwardArrival': {20},
                'forwardTransfers': set(),
                'backwardDeparture': {21},
                'backwardArrival': {2130},
                'backwardTransfers': set(),
            },
            'withBaggage': None,
            'time': {
                'forwardDepartureMin': '2017-09-01T03:00:00',
                'forwardDepartureMax': '2017-09-01T03:00:00',
                'forwardArrivalMin': '2017-09-01T05:00:00',
                'forwardArrivalMax': '2017-09-01T05:00:00',
                'backwardDepartureMin': '2017-09-10T14:00:00',
                'backwardDepartureMax': '2017-09-10T14:00:00',
                'backwardArrivalMin': '2017-09-10T16:00:00',
                'backwardArrivalMax': '2017-09-10T16:00:00',

            },
            'prices': {
                'directFlight': {'currency': 'RUR', 'value': 1000.0},
                'withBaggage': {'currency': 'RUR', 'value': 1000.0},
                'airports': {
                    'from': {2130: {'currency': 'RUR', 'value': 1000.0}},
                    'to': dict(),
                },
                'airlines': {42: {'currency': 'RUR', 'value': 1000.0}},
                'directAirlines': {42},
                'transfers': {
                    0: {'currency': 'RUR', 'value': 1000.0},
                    1: {'currency': 'RUR', 'value': 1000.0},
                    2: {'currency': 'RUR', 'value': 1000.0},
                },
            },
        }

        filter_state = bbc.FilterState(get_test_result(flights, fares))
        self.assertDictEqual(filter_state.to_dict(), expected_result)

    def test_None_baggage_keys(self):
        f1 = create_flight('2017-09-01T03:00:00', '2017-09-01T05:00:00', 42,
                           2130, 20, 'SU 0')
        b1 = create_flight('2017-09-10T14:00:00', '2017-09-10T16:00:00', 42,
                           21, 2130, 'SU 3')
        flights = {
            f1['key']: f1,
            b1['key']: b1,
        }

        fares = {
            '1': create_fare(
                route=[[f1['key']], [b1['key']]],
                baggage=[[None] * 2],
                price=1000.
            )
        }

        expected_result = {
            'transfer': {
                'count': 0, 'hasAirportChange': None, 'hasNight': None,
                'minDuration': None, 'maxDuration': None,
            },
            'airlines': {42},
            'all_airlines': {42},
            'partners': set(),
            'airport': {
                'forwardDeparture': {2130},
                'forwardArrival': {20},
                'forwardTransfers': set(),
                'backwardDeparture': {21},
                'backwardArrival': {2130},
                'backwardTransfers': set(),
            },
            'withBaggage': None,
            'time': {
                'forwardDepartureMin': '2017-09-01T03:00:00',
                'forwardDepartureMax': '2017-09-01T03:00:00',
                'forwardArrivalMin': '2017-09-01T05:00:00',
                'forwardArrivalMax': '2017-09-01T05:00:00',
                'backwardDepartureMin': '2017-09-10T14:00:00',
                'backwardDepartureMax': '2017-09-10T14:00:00',
                'backwardArrivalMin': '2017-09-10T16:00:00',
                'backwardArrivalMax': '2017-09-10T16:00:00',

            },
            'prices': {
                'directFlight': {'currency': 'RUR', 'value': 1000.0},
                'withBaggage': None,
                'airports': {
                    'from': {2130: {'currency': 'RUR', 'value': 1000.0}},
                    'to': dict(),
                },
                'airlines': {42: {'currency': 'RUR', 'value': 1000.0}},
                'directAirlines': {42},
                'transfers': {
                    0: {'currency': 'RUR', 'value': 1000.0},
                    1: {'currency': 'RUR', 'value': 1000.0},
                    2: {'currency': 'RUR', 'value': 1000.0},
                },
            },
        }

        filter_state = bbc.FilterState(get_test_result(flights, fares))
        self.assertDictEqual(filter_state.to_dict(), expected_result)

    def test_different_airlines(self):
        f1 = create_flight('2017-09-01T03:00:00', '2017-09-01T05:00:00', 42,
                           2130, 20, 'SU 0')
        f2 = create_flight('2017-09-01T06:00:00', '2017-09-01T09:00:00', 43,
                           20, 540, 'AB 1')

        flights = {
            f1['key']: f1,
            f2['key']: f2,
        }

        fares = {
            '1': create_fare(
                route=[[f1['key'], f2['key']], []],
                baggage=[['1d1d23d'] * 3, []],
                price=1000.
            ),
        }

        filter_state = bbc.FilterState(get_test_result(flights, fares))
        result = filter_state.to_dict()
        assert result['airlines'] == set()
        assert result['all_airlines'] == {42, 43}

        assert result['prices']['directFlight'] is None
        assert result['prices']['withBaggage'] == {'currency': 'RUR', 'value': 1000.0}
        assert result['prices']['airlines'] == dict()
        assert result['prices']['airports'] == {
            'from': {2130: {'currency': 'RUR', 'value': 1000.0}},
            'to': {540: {'currency': 'RUR', 'value': 1000.0}}
        }
        assert result['prices']['transfers'] == {
            0: None,
            1: {'currency': 'RUR', 'value': 1000.0},
            2: {'currency': 'RUR', 'value': 1000.0},
        }

    def test_minprice_for_airport_can_be_set_using_only_flights_with_this_airport_1(self):
        f1 = create_flight('2017-09-01T04:00:00', '2017-09-01T06:00:00', 42,
                           2130, 20, 'SU 0')
        b1 = create_flight('2017-09-10T15:00:00', '2017-09-10T17:00:00', 42,
                           21, 2130, 'SU 1')

        f2 = create_flight('2017-09-01T03:00:00', '2017-09-01T05:00:00', 42,
                           2130, 20, 'SU 0')
        b2 = create_flight('2017-09-10T14:00:00', '2017-09-10T16:00:00', 42,
                           20, 2131, 'SU 1')
        flights = {
            f1['key']: f1,
            b1['key']: b1,
            f2['key']: f2,
            b2['key']: b2,
        }

        fares = {
            '1': create_fare(
                route=[[f1['key']], [b1['key']]],
                baggage=[[None] * 2],
                price=1000.
            ),
            '2': create_fare(
                route=[[f2['key']], [b2['key']]],
                baggage=[[None] * 2],
                price=900.
            )
        }

        filter_state = bbc.FilterState(get_test_result(flights, fares))
        result = filter_state.to_dict()

        assert result['prices']['airports'] == {
            'from': {2130: {'currency': 'RUR', 'value': 1000.0}},
            'to': {20: {'currency': 'RUR', 'value': 900.0}}
        }

    def test_transfers_filter_prices_(self):
        f1 = create_flight('2017-09-01T03:00:00', '2017-09-01T05:00:00', 42, 20, 30, 'SU 1')
        f2 = create_flight('2017-09-01T08:00:00', '2017-09-01T10:00:00', 42, 30, 40, 'SU 2')

        ff = create_flight('2017-09-01T03:00:00', '2017-09-01T06:00:00', 42, 20, 40, 'SU 3')

        flights = {
            f1['key']: f1,
            f2['key']: f2,

            ff['key']: ff,
        }

        fares = {
            '1': create_fare(
                route=[[f1['key'], f2['key']], []],
                baggage=[['1d1d23d'] * 2, []],
                price=500.
            ),
            '2': create_fare(
                route=[[ff['key']], []],
                baggage=[['1d1d23d'], []],
                price=1000.
            )
        }

        filter_state = bbc.FilterState(get_test_result(flights, fares))
        result = filter_state.to_dict()

        assert result['prices']['transfers'] == {
            0: {'currency': 'RUR', 'value': 1000.0},
            1: {'currency': 'RUR', 'value': 500.0},
            2: {'currency': 'RUR', 'value': 500.0},
        }

    def test_minprice_for_airport_can_be_set_using_only_flights_with_this_airport_2(self):
        f1 = create_flight('2017-09-01T03:00:00', '2017-09-01T05:00:00', 42,
                           2130, 20, 'SU 0')
        b1 = create_flight('2017-09-10T14:00:00', '2017-09-10T16:00:00', 42,
                           21, 2131, 'SU 1')
        flights = {
            f1['key']: f1,
            b1['key']: b1,
        }

        fares = {
            '1': create_fare(
                route=[[f1['key']], [b1['key']]],
                baggage=[[None] * 2],
                price=1000.
            ),
        }

        filter_state = bbc.FilterState(get_test_result(flights, fares))
        result = filter_state.to_dict()

        assert result['prices']['airports'] == {'from': {}, 'to': {}}


class TestPollingStatus(TestCase):
    def test_format(self):
        partners = [
            Partner(code='test_partner_1', id=1),
            Partner(code='test_partner_2', id=2),
        ]
        ps = bbc.PollingStatus(partners)
        expected_result = {
            'asked_partners': [],
            'asked_partners_count': 0,
            'remaining_partners': ['test_partner_1', 'test_partner_2'],
            'remaining_partners_count': 2,
        }
        self.assertDictEqual(expected_result, ps.to_dict())

    def test_finalize_should_be_safe(self):
        partners = [
            Partner(code='test_partner_1', id=1),
            Partner(code='test_partner_2', id=2),
        ]
        ps = bbc.PollingStatus(partners)
        expected_result = {
            'asked_partners': [],
            'asked_partners_count': 0,
            'remaining_partners': ['test_partner_1', 'test_partner_2'],
            'remaining_partners_count': 2,
        }
        self.assertDictEqual(expected_result, ps.to_dict())

        for _ in range(2):
            ps.finalize('test_partner_1')
            expected_result = {
                'asked_partners': ['test_partner_1'],
                'asked_partners_count': 1,
                'remaining_partners': ['test_partner_2'],
                'remaining_partners_count': 1,
            }
            self.assertDictEqual(expected_result, ps.to_dict())

# -*- coding: utf-8 -*-
from datetime import datetime

import pytz
import ujson
from django.conf import settings
from library.python import resource
from mock import patch

from travel.avia.library.python.avia_data.models.company import AviaCompany, CompanyTariff
from travel.avia.library.python.common.models.geo import Settlement, Station
from travel.avia.library.python.common.models.partner import Partner
from travel.avia.library.python.common.models.schedule import Company

from travel.avia.ticket_daemon.ticket_daemon.api.flights import Variant, FlightFabric, _localize_by_station, \
    OperatingFlight
from travel.avia.ticket_daemon.ticket_daemon.api.query import Query
from travel.avia.ticket_daemon.ticket_daemon.api.result import Result
from travel.avia.ticket_daemon.ticket_daemon.lib.currency import Price
from travel.avia.library.python.ticket_daemon.date import aware_to_timestamp

NOW = pytz.UTC.localize(datetime(2017, 9, 1))
UNIXTIME_NOW = aware_to_timestamp(NOW)
RESULT_PATH = 'resfs/file/travel/avia/ticket_daemon/tests/api/result_serialization_data.json'
RESULT_WITH_FARE_FAMILIES_PATH = 'resfs/file/travel/avia/ticket_daemon/tests/api/result_serialization_with_fare_families_data.json'


def get_partner():
    return Partner(code='zzz', id=13)


def get_query():
    mow = Settlement(id=213)
    sip = Settlement(id=2)
    query = Query(
        point_from=mow,
        point_to=sip,
        passengers={
            'adults': 1,
            'children': 0,
            'infants': 0,
        },
        date_forward=NOW,
        service='ticket'
    )
    query.id = 'test_qid'
    return query


def get_variants(count_flights=2, count_variants=2):
    a_mow = Station(id=2130, time_zone='Europe/Moscow')
    a_sip = Station(id=20, time_zone='Europe/Moscow')
    company = Company(id=42)
    avia_company = AviaCompany(rasp_company=company)
    tariff_company = CompanyTariff(id=4200)

    variants = []
    flight_fabric = FlightFabric()
    for j in range(count_variants):
        v = Variant()
        v.created_at = 1504224000
        v.tariff = Price(float(1000))

        segment = flight_fabric.create(
            company_iata='ZZZ',
            number='SU %d' % j,
            fare_code='ggg',
            station_from_iata='MOW',
            station_to_iata='SIP',
            local_departure=NOW,
            local_arrival=NOW,
            t_model_name='kukuruznic',
            operating=OperatingFlight(company_iata='ZZZ', number='FV %d' % j),
            selfconnect=bool(j),
        )

        flight = segment._flight
        flight.station_from = a_mow
        flight.station_to = a_sip
        flight.departure = _localize_by_station(flight.local_departure, flight.station_from)
        flight.arrival = _localize_by_station(flight.local_arrival, flight.station_to)
        flight.company = company
        flight.operating.company = company
        flight.avia_company = avia_company
        flight.company_tariff = tariff_company

        for i in range(count_flights):
            v.forward.segments.append(segment)

        v.url = 'yandex.ru'
        v.klass = 'econom'
        v.order_data = {
            'url': v.url
        }
        if j == 0:
            v.order_data['promo'] = {
                'code': 'white-monday',
                'endActionDt': '2020-12-09T00:00:00',
                'endActionTz': 'UTC',
            }
        v.partner = get_partner()

        variants.append(v)
    return variants


@patch('travel.avia.ticket_daemon.ticket_daemon.api.result.result.unixtime', return_value=UNIXTIME_NOW)
def get_result(*mocks):
    result = Result(
        query=get_query(),
        partner=get_partner(),
        variants=get_variants(),
        query_time=1,
        status='done',
        store_time=1,
    )

    for v in result.variants:
        v.partner = get_partner()

    return result


@patch('travel.avia.ticket_daemon.ticket_daemon.api.flights.feature_flags.store_min_tariff_per_fare_code', return_value=True)
def test_new_format(*mocks):
    expected = ujson.loads(resource.find(RESULT_PATH))
    result = get_result()
    actual_result = result.serialize_variants(result.variants)
    assert ujson.dumps(actual_result, sort_keys=True) == ujson.dumps(expected, sort_keys=True)


@patch('travel.avia.ticket_daemon.ticket_daemon.api.flights.feature_flags.store_min_tariff_per_fare_code', return_value=True)
@patch(
    'travel.avia.ticket_daemon.ticket_daemon.api.result.fare_families.FareFamiliesProvider.get_fare_families_for_variants',
    return_value={
        'data': [{
            'fareFamilies': {
                'ff_key1': {
                    'base_class': 'ECONOMY',
                    'brand': 'LITE',
                    'terms': [],
                    'key': 'ff_key1',
                },
                'ff_key2': {
                    'base_class': 'ECONOMY',
                    'brand': 'OPTIMUM',
                    'terms': [{
                        'code': 'baggage',
                        'rules': [{
                            'availability': '',
                            'places': 2,
                            'size': 158,
                            'weight': 23,
                        }],
                    }],
                    'key': 'ff_key2',
                },
            },
            'variantsMap': {
                '1c8ab062227ad307d6bf8d1c6b8ce3d4': {
                    'fare_families': [['ff_key1'], []],
                    'fare_families_hash': '22dbb04331d1628e249aee92bc1aba20',
                },
                '34c99539eead43df21e1af41a4a430ed': {
                    'fare_families': [['ff_key2'], []],
                    'fare_families_hash': 'd90e740ec68a30d829ecd7767b8dfdfc',
                },
            },
        }],
    },
)
def test_new_format_with_fare_families(*mocks):
    settings.FARE_FAMILIES_ENABLED = True
    expected = ujson.loads(resource.find(RESULT_WITH_FARE_FAMILIES_PATH))
    result = get_result()
    actual_result = result.serialize_variants_and_fare_families(result.variants)
    assert ujson.dumps(actual_result, sort_keys=True) == ujson.dumps(expected, sort_keys=True)

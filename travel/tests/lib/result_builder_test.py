# -*- encoding: utf-8 -*-
from datetime import datetime
from freezegun import freeze_time
from logging import Logger
from mock import Mock
from typing import cast
from unittest import TestCase

from travel.avia.price_index.lib.result_builder import ResultBuilder
from travel.avia.price_index.lib.indexer import fare_index_builder
from travel.avia.price_index.lib.price_converter import price_converter
from travel.avia.price_index.lib.rates_provider import RatesProvider
from travel.avia.price_index.lib.currency_provider import CurrencyProvider, CurrencyModel
from travel.avia.price_index.lib.settings import Settings
from travel.avia.price_index.lib.national_version_provider import NationalVersionModel
from travel.avia.library.proto.search_result.v1.result_pb2 import Result as SearchResult, Flight, Variant, FlightSegment
from travel.avia.library.proto.common.v1.common_pb2 import (
    NATIONAL_VERSION_RU,
    Baggage,
    Passengers,
    Point,
    POINT_TYPE_SETTLEMENT,
    SERVICE_CLASS_ECONOMY,
    Price,
)
from travel.proto.commons_pb2 import TDate


class ResultBuilderTestCase(TestCase):
    def setUp(self):
        settings = Settings()
        settings.BACKEND_HOST = 'http://mock-avia-backend-host.yandex-team.ru'
        self._fake_national_version_provider = Mock()

        currency_provider = Mock(CurrencyProvider)
        currency_provider.get_by_code = Mock(return_value=CurrencyModel(pk=2, code='EUR'))

        nv_model = NationalVersionModel(
            pk=1,
            code='nv_code',
        )

        national_version_provider = Mock()
        national_version_provider.get_by_code = Mock(return_value=nv_model)
        national_version_provider.get_all = Mock(return_value=[nv_model])

        rates_provider = Mock(RatesProvider)
        rates_provider.get_base_currency_id = Mock(return_value=1)
        rates_provider.get_rates_for = Mock(return_value={2: 100})

        self._result_builder = ResultBuilder(
            logger=cast(Logger, Mock()),
            fare_index_builder=fare_index_builder,
            price_converter=price_converter,
            rates_provider=rates_provider,
            currency_provider=currency_provider,
            national_version_provider=national_version_provider,
        )

    @freeze_time(datetime(2016, 1, 3, 17, 0, 0))
    def test_oneway_without_stopovers(self):
        search_result = SearchResult()
        search_result.qid = 'qid'
        search_result.point_from.CopyFrom(Point(id=1, type=POINT_TYPE_SETTLEMENT))
        search_result.point_to.CopyFrom(Point(id=2, type=POINT_TYPE_SETTLEMENT))
        search_result.date_forward.CopyFrom(TDate(Year=2021, Month=7, Day=10))
        search_result.service_class = SERVICE_CLASS_ECONOMY
        search_result.passengers.CopyFrom(Passengers(adults=1))
        search_result.national_version = NATIONAL_VERSION_RU
        search_result.flights['flight_1'].CopyFrom(
            Flight(
                key='flight_1',
                number='AB 123',
                company_id=1,
                station_from_id=3,
                station_to_id=4,
                local_departure='2021-07-10T10:25:00',
                local_arrival='2021-07-10T12:25:00',
                utc_departure='2021-07-10T07:25:00',
                utc_arrival='2021-07-10T10:25:00',
            )
        )
        search_result.flights['flight_2'].CopyFrom(
            Flight(
                key='flight_2',
                number='AB 456',
                company_id=1,
                station_from_id=3,
                station_to_id=4,
                local_departure='2021-07-10T12:25:00',
                local_arrival='2021-07-10T14:25:00',
                utc_departure='2021-07-10T09:25:00',
                utc_arrival='2021-07-10T12:25:00',
            )
        )
        search_result.variants.extend(
            [
                Variant(
                    partner_code='partner_1',
                    forward=[FlightSegment(flight_key='flight_1', baggage=Baggage())],
                    price=Price(currency='EUR', value=1000),
                ),
                Variant(
                    partner_code='partner_2',
                    forward=[FlightSegment(flight_key='flight_1', baggage=Baggage(included=True))],
                    price=Price(currency='EUR', value=2000),
                ),
                Variant(
                    partner_code='partner_1',
                    forward=[FlightSegment(flight_key='flight_2', baggage=Baggage())],
                    price=Price(currency='EUR', value=500),
                ),
                Variant(
                    partner_code='partner_2',
                    forward=[FlightSegment(flight_key='flight_2', baggage=Baggage(included=True))],
                    price=Price(currency='EUR', value=2500),
                ),
            ]
        )

        result = self._result_builder.build_from_proto(search_result)

        self.assertEqual(result.national_version_id, 1)
        self.assertEqual(result.from_id, 1)
        self.assertEqual(result.to_id, 2)
        self.assertEqual(result.adults_count, 1)
        self.assertEqual(result.infants_count, 0)
        self.assertEqual(result.children_count, 0)
        self.assertEqual(result.forward_date, '2021-07-10')
        self.assertEqual(result.base_value, 50000)
        self.assertEqual(result.value, 500)
        self.assertEqual(result.currency_id, 2)
        self.assertEqual(
            result.data,
            [
                {
                    'duration_transfer': None,
                    'has_baggage': False,
                    'has_airport_change': False,
                    'forward_arrival_airport': 4,
                    'backward_transfer_airports': [],
                    'has_night_transfer': False,
                    'forward_arrival_time_type': 2,
                    'backward_arrival_time_type': 0,
                    'airlines': [1],
                    'value': 500.0,
                    'backward_departure_airport': None,
                    'backward_arrival_airport': None,
                    'currency_id': 2,
                    'backward_departure_time_type': 0,
                    'forward_departure_time_type': 2,
                    'forward_transfer_airports': [],
                    'count_transfer': 0,
                    'forward_departure_airport': 3,
                    'base_value': 50000,
                },
                {
                    'duration_transfer': None,
                    'has_baggage': False,
                    'has_airport_change': False,
                    'forward_arrival_airport': 4,
                    'backward_transfer_airports': [],
                    'has_night_transfer': False,
                    'forward_arrival_time_type': 2,
                    'backward_arrival_time_type': 0,
                    'airlines': [1],
                    'value': 1000.0,
                    'backward_arrival_airport': None,
                    'currency_id': 2,
                    'backward_departure_time_type': 0,
                    'forward_departure_time_type': 1,
                    'forward_transfer_airports': [],
                    'backward_departure_airport': None,
                    'count_transfer': 0,
                    'forward_departure_airport': 3,
                    'base_value': 100000,
                },
                {
                    'duration_transfer': None,
                    'has_baggage': True,
                    'has_airport_change': False,
                    'forward_arrival_airport': 4,
                    'backward_transfer_airports': [],
                    'has_night_transfer': False,
                    'forward_arrival_time_type': 2,
                    'backward_arrival_time_type': 0,
                    'airlines': [1],
                    'value': 2000.0,
                    'backward_arrival_airport': None,
                    'currency_id': 2,
                    'backward_departure_time_type': 0,
                    'forward_departure_time_type': 1,
                    'forward_transfer_airports': [],
                    'backward_departure_airport': None,
                    'count_transfer': 0,
                    'forward_departure_airport': 3,
                    'base_value': 200000,
                },
                {
                    'duration_transfer': None,
                    'has_baggage': True,
                    'has_airport_change': False,
                    'forward_arrival_airport': 4,
                    'backward_transfer_airports': [],
                    'has_night_transfer': False,
                    'forward_arrival_time_type': 2,
                    'backward_arrival_time_type': 0,
                    'airlines': [1],
                    'value': 2500.0,
                    'backward_arrival_airport': None,
                    'currency_id': 2,
                    'backward_departure_time_type': 0,
                    'forward_departure_time_type': 2,
                    'forward_transfer_airports': [],
                    'backward_departure_airport': None,
                    'count_transfer': 0,
                    'forward_departure_airport': 3,
                    'base_value': 250000,
                },
            ],
        )

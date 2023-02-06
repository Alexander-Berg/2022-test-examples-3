# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

import logging
from datetime import datetime
from decimal import Decimal

import mock
import pytest
import requests
from django.conf import settings
from django.test import override_settings
from mock import call

from common.apps.train.models import CoachType
from common.models.currency import Price
from common.tester.factories import create_station, create_company, create_thread
from common.tester.testcase import TestCase
from common.tester.utils.replace_setting import replace_dynamic_setting_keys
from common.utils.date import MSK_TZ
from travel.proto.dicts.trains.facility_pb2 import TFacility
from travel.rasp.train_api.pb_provider import PROTOBUF_DATA_PROVIDER
from travel.rasp.train_api.tariffs.train.factories.base import create_train_tariff, create_train_tariffs_result
from travel.rasp.train_api.train_partners.base.factories.coach_schemas import (
    create_suite_schema, create_usual_platzkarte_schema, create_usual_compartment_schema
)
from travel.rasp.train_api.train_partners.base.train_details.parsers import BaseCoachDetails, BaseTrainDetails, PlaceDetails
from travel.rasp.train_api.wizard_api.client import TrainWizardApiClient

pytestmark = pytest.mark.dbuser


def _msk_dt(*args, **kwargs):
    return MSK_TZ.localize(datetime(*args, **kwargs))


class TestTrainWizardApiClient(TestCase):
    def setUp(self):
        self._fake_response = mock.Mock(spec=requests.Response)
        self._fake_transport = mock.Mock(spec=requests)
        self._fake_transport.post = mock.Mock(return_value=self._fake_response)
        self._fake_logger = mock.Mock(spec=logging.Logger)
        self._client = TrainWizardApiClient(
            transport=self._fake_transport,
            logger=self._fake_logger
        )

        self._departure_station = create_station(t_type='train', __={'codes': {'express': '200'}})
        self._arrival_station = create_station(t_type='train', __={'codes': {'express': '2000'}})

        self._tariffs = create_train_tariffs_result({
            'departure_point': self._departure_station,
            'arrival_point': self._arrival_station,
            'departure_date': '2000-01-01'
        }, segments=(
            {
                'thread': create_thread(),
                'station_from': self._departure_station,
                'station_to': self._arrival_station,
                'departure': _msk_dt(2000, 1, 1),
                'arrival': _msk_dt(2000, 1, 2),
                'original_number': '123G',
                'number': '123Ж',
                'coach_owners': ['ФПК', 'МВФ'],
                'has_dynamic_pricing': True,
                'two_storey': True,
                'is_suburban': True,
                'provider': 'P1',
                'tariffs': {
                    'electronic_ticket': True,
                    'classes': {
                        'compartment': create_train_tariff(
                            'compartment', Price(Decimal(1000)),
                            service_price=Price(Decimal(100)), fee=Price(Decimal(100)),
                            seats=14,
                            lower_seats=0,
                            upper_seats=14,
                            lower_side_seats=1,
                            upper_side_seats=2,
                            max_seats_in_the_same_car=10,
                            several_prices=True,
                            service_class='2Л',
                        ),
                        'suite': create_train_tariff(
                            'suite', Price(Decimal(1000), 'EUR'),
                            service_price=Price(Decimal(100)), fee=Price(Decimal(100), 'EUR'),
                            seats=5,
                            lower_seats=1,
                            upper_seats=4,
                            lower_side_seats=2,
                            upper_side_seats=5,
                            max_seats_in_the_same_car=3,
                            service_class='2Л',
                        ),
                    }
                },
                'title_common': '{"type":"default","t_type":"train","title_parts":["c1","c2"]}'
            },
            {
                'thread': create_thread(),
                'station_from': self._departure_station,
                'station_to': self._arrival_station,
                'departure': _msk_dt(2000, 1, 1, 12),
                'arrival': _msk_dt(2000, 1, 2, 13),
                'original_number': '456F',
                'number': '456Ф',
                'coach_owners': ['ФПК'],
                'raw_train_name': 'Волга',
                'tariffs': {
                    'electronic_ticket': False,
                    'classes': {
                        'platzkarte': create_train_tariff(
                            'platzkarte', Price(Decimal(500)),
                            service_price=Price(Decimal(100)), fee=Price(Decimal(50)),
                            seats=34,
                            lower_seats=14,
                            upper_seats=20,
                            lower_side_seats=3,
                            upper_side_seats=4,
                            max_seats_in_the_same_car=30,
                            service_class='2Л',
                        ),
                    }
                },
                'title_common': '{"type":"default","t_type":"train","title_parts":["c3","c4"]}'
            },
        ))
        self._tariffs.segments[0].thread.first_country_code = 'RU'
        self._tariffs.segments[0].thread.last_country_code = 'UA'
        self._tariffs.segments[1].thread.first_country_code = 'KZ'
        self._tariffs.segments[1].thread.last_country_code = 'USA'

        for proto in (
            TFacility(Id=1, Code='EAT'),
            TFacility(Id=2, Code='PAP'),
            TFacility(Id=3, Code='TV'),
            TFacility(Id=4, Code='TRAN'),
            TFacility(Id=5, Code='COND'),
            TFacility(Id=6, Code='BED'),
            TFacility(Id=7, Code='SAN'),
            TFacility(Id=8, Code='WIFI'),
            TFacility(Id=9, Code='nearToilet'),
            TFacility(Id=10, Code='side'),
            TFacility(Id=11, Code='upper'),
        ):
            PROTOBUF_DATA_PROVIDER.facility_repo.add(proto.SerializeToString())

    def _make_details(self, electronic_ticket):
        platzkarte_schema = create_usual_platzkarte_schema()
        compartment_schema = create_usual_compartment_schema()
        suite_schema = create_suite_schema()
        return mock.Mock(
            spec=BaseTrainDetails,
            express_from=200,
            express_to=2000,
            ticket_number='123Ж',
            departure=_msk_dt(2000, 1, 1, 12),
            arrival=_msk_dt(2000, 1, 2, 13),
            schemas=[platzkarte_schema, compartment_schema, suite_schema],
            electronic_ticket=electronic_ticket,
            coaches=[
                mock.Mock(
                    spec=BaseCoachDetails,
                    facilities=['WIFI', 'EAT'],
                    number='02',
                    owner='ФПК',
                    places=[PlaceDetails(12, adult_tariff=Price(100)), PlaceDetails(1, adult_tariff=Price(100))],
                    schema_id=platzkarte_schema.id,
                    type='platzkarte',
                    errors=set(),
                ),
                mock.Mock(
                    spec=BaseCoachDetails,
                    facilities=['WIFI'],
                    number='01',
                    owner='РЖД',
                    places=[PlaceDetails(2, adult_tariff=Price(1000)), PlaceDetails(1, adult_tariff=Price(1000))],
                    schema_id=compartment_schema.id,
                    type='compartment',
                    errors=set(),
                ),
                mock.Mock(
                    spec=BaseCoachDetails,
                    facilities=[],
                    number='03',
                    owner='РЖД',
                    places=[PlaceDetails(2, adult_tariff=Price(1000)), PlaceDetails(1, adult_tariff=Price(1000))],
                    type='compartment',
                    errors=set(),
                ),
                mock.Mock(
                    spec=BaseCoachDetails,
                    facilities=[],
                    number='04',
                    owner='РЖД',
                    places=[PlaceDetails(2, adult_tariff=Price(10000))],
                    schema_id=suite_schema.id,
                    type='suite',
                    errors=set(),
                ),
                mock.Mock(
                    spec=BaseCoachDetails,
                    facilities=[],
                    number='05',
                    owner='РЖД',
                    places=[PlaceDetails(2, adult_tariff=Price(10000))],
                    schema_id=suite_schema.id,
                    type='unknown',
                    errors=set(),
                )
            ],
            broken_coaches=[],
        )

    @override_settings(TRAIN_WIZARD_API_INDEXER_HOST='localhost')
    def test_store_tariffs_ok(self):
        self._client.store_tariffs(self._tariffs)

        expected_path = (
            '/indexer/public-api/direction/'
            '?departure_point_express_id=200'
            '&arrival_point_express_id=2000'
            '&departure_date=2000-01-01'
        )
        assert self._fake_logger.info.call_args_list == [
            call('Start index: [%s]', expected_path),
            call('Finish index: [%s]', expected_path)
        ]
        self._fake_transport.post.assert_called_once_with(
            'https://localhost' + expected_path,
            timeout=settings.TRAIN_WIZARD_API_INDEXER_TIMEOUT,
            headers={'Content-Type': 'application/json'},
            json=[
                {
                    'places': [
                        {
                            'count': 5,
                            'lower_count': 1,
                            'upper_count': 4,
                            'lower_side_count': 2,
                            'upper_side_count': 5,
                            'max_seats_in_the_same_car': 3,
                            'price': {
                                'currency': 'EUR', 'value': '1100',
                            },
                            'price_details': {
                                'service_price': '100',
                                'ticket_price': '1000',
                                'fee': '100',
                                'several_prices': False,
                            },
                            'service_class': '2Л',
                            'coach_type': 'suite',
                            'has_non_refundable_tariff': False,
                        },
                        {
                            'count': 14,
                            'lower_count': 0,
                            'upper_count': 14,
                            'lower_side_count': 1,
                            'upper_side_count': 2,
                            'max_seats_in_the_same_car': 10,
                            'price': {
                                'currency': 'RUR', 'value': '1100',
                            },
                            'price_details': {
                                'service_price': '100',
                                'ticket_price': '1000',
                                'fee': '100',
                                'several_prices': True,
                            },
                            'service_class': '2Л',
                            'coach_type': 'compartment',
                            'has_non_refundable_tariff': False,
                        }
                    ],
                    'broken_classes': None,
                    'number': '123G',
                    'display_number': '123Ж',
                    'coach_owners': ['ФПК', 'МВФ'],
                    'has_dynamic_pricing': True,
                    'two_storey': True,
                    'is_suburban': True,
                    'departure_dt': '2000-01-01T00:00:00+03:00',
                    'arrival_dt': '2000-01-02T00:00:00+03:00',
                    'title_dict': {
                        'type': 'default',
                        't_type': 'train',
                        'title_parts': ['c1', 'c2']
                    },
                    'electronic_ticket': True,
                    'departure_station_id': self._departure_station.id,
                    'arrival_station_id': self._arrival_station.id,
                    'first_country_code': 'RU',
                    'last_country_code': 'UA',
                    'provider': 'P1',
                    'raw_train_name': None,
                }, {
                    'places': [
                        {
                            'count': 34,
                            'lower_count': 14,
                            'upper_count': 20,
                            'lower_side_count': 3,
                            'upper_side_count': 4,
                            'max_seats_in_the_same_car': 30,
                            'price': {
                                'currency': 'RUR',
                                'value': '550',
                            },
                            'price_details': {
                                'service_price': '100',
                                'ticket_price': '500',
                                'fee': '50',
                                'several_prices': False,
                            },
                            'service_class': '2Л',
                            'coach_type': 'platzkarte',
                            'has_non_refundable_tariff': False,
                        }
                    ],
                    'broken_classes': None,
                    'number': '456F',
                    'display_number': '456Ф',
                    'coach_owners': ['ФПК'],
                    'has_dynamic_pricing': False,
                    'two_storey': False,
                    'is_suburban': False,
                    'departure_dt': '2000-01-01T12:00:00+03:00',
                    'arrival_dt': '2000-01-02T13:00:00+03:00',
                    'title_dict': {
                        'type': 'default',
                        't_type': 'train',
                        'title_parts': [
                            'c3',
                            'c4'
                        ]
                    },
                    'electronic_ticket': False,
                    'departure_station_id': self._departure_station.id,
                    'arrival_station_id': self._arrival_station.id,
                    'first_country_code': 'KZ',
                    'last_country_code': 'USA',
                    'provider': None,
                    'raw_train_name': 'Волга',
                }
            ]
        )
        assert self._fake_logger.warn.call_count == 0

    @override_settings(TRAIN_WIZARD_API_INDEXER_HOST=None)
    def test_store_tariffs_train_wizard_data_host_is_unknown(self):
        self._client.store_tariffs(self._tariffs)

        assert self._fake_transport.post.call_count == 0
        assert self._fake_logger.info.call_count == 0
        self._fake_logger.warn.assert_called_once_with(
            'Train wizard api host is not initialized'
        )

    @override_settings(TRAIN_WIZARD_API_INDEXER_HOST='localhost')
    def test_store_tariffs_train_wizard_api_has_returned_500(self):
        self._fake_response.raise_for_status.side_effect = requests.HTTPError()
        self._client.store_tariffs(self._tariffs)

        expected_path = (
            '/indexer/public-api/direction/'
            '?departure_point_express_id=200'
            '&arrival_point_express_id=2000'
            '&departure_date=2000-01-01'
        )
        self._fake_logger.info.assert_called_once_with('Start index: [%s]', expected_path)
        self._fake_logger.warn.assert_called_once_with(
            'Can not index document to train wizard api: [%s]',
            expected_path,
            exc_info=True
        )

    def _test_store_details_ok(self, electronic_ticket):
        rgd = create_company(express_code='РЖД')
        self._client.store_details(self._make_details(electronic_ticket=electronic_ticket))

        expected_path = (
            '/indexer/public-api/train/'
            '?departure_point_express_id=200'
            '&arrival_point_express_id=2000'
            '&departure_dt=2000-01-01T09%3A00%3A00%2B00%3A00'
            '&number=123%D0%96'
        )
        assert self._fake_logger.info.call_args_list == [
            call('Start index: [%s]', expected_path),
            call('Finish index: [%s]', expected_path)
        ]
        self._fake_transport.post.assert_called_once_with(
            'https://localhost' + expected_path,
            timeout=settings.TRAIN_WIZARD_API_INDEXER_TIMEOUT,
            headers={'Content-Type': 'application/json'},
            json={
                'coaches': [
                    {
                        'owner': 'РЖД',
                        'company_id': rgd.id,
                        'facilities': ['WIFI'],
                        'errors': [],
                        'facilities_ids': [TFacility.EType.Value('WIFI_ID')],
                        'type': 'compartment',
                        'type_id': CoachType.COMPARTMENT_ID,
                        'number': '01',
                        'places': [
                            {
                                'number': 1,
                                'price': {'currency': 'RUR', 'value': 1000.0}
                            },
                            {
                                'facilities_ids': [TFacility.EType.Value('UPPER_PLACE_ID')],
                                'number': 2,
                                'price': {'currency': 'RUR', 'value': 1000.0}
                            }
                        ]
                    },
                    {
                        'owner': 'ФПК',
                        'company_id': None,
                        'facilities': ['EAT', 'WIFI'],
                        'errors': [],
                        'facilities_ids': [
                            TFacility.EType.Value('EATING_ID'),
                            TFacility.EType.Value('WIFI_ID'),
                        ],
                        'type': 'platzkarte',
                        'type_id': CoachType.PLATZKARTE_ID,
                        'number': '02',
                        'places': [
                            {
                                'number': 1,
                                'price': {'currency': 'RUR', 'value': 100.0},
                            },
                            {
                                'facilities_ids': [
                                    TFacility.EType.Value('NEAR_TOILET_ID'),
                                    TFacility.EType.Value('UPPER_PLACE_ID'),
                                ],
                                'number': 12,
                                'price': {'currency': 'RUR', 'value': 100.0},
                            }
                        ]
                    },
                    {
                        'owner': 'РЖД',
                        'company_id': rgd.id,
                        'facilities': [],
                        'errors': [],
                        'facilities_ids': [],
                        'type': 'compartment',
                        'type_id': CoachType.COMPARTMENT_ID,
                        'number': '03',
                        'places': [
                            {
                                'number': 1,
                                'price': {'currency': 'RUR', 'value': 1000.0},
                            },
                            {
                                'number': 2,
                                'price': {'currency': 'RUR', 'value': 1000.0},
                            }
                        ]
                    },
                    {
                        'owner': 'РЖД',
                        'company_id': rgd.id,
                        'facilities': [],
                        'errors': [],
                        'facilities_ids': [],
                        'type': 'suite',
                        'type_id': CoachType.SUITE_ID,
                        'number': '04',
                        'places': [
                            {
                                'facilities_ids': [
                                    TFacility.EType.Value('UPPER_PLACE_ID')
                                ],
                                'group_number': 1,
                                'number': 2,
                                'price': {'currency': 'RUR', 'value': 10000.0},
                            }
                        ]
                    }
                ],
                'electronic_ticket': electronic_ticket,
            }
        )
        assert self._fake_logger.warn.call_count == 0

    @override_settings(TRAIN_WIZARD_API_INDEXER_HOST='localhost')
    def test_store_details_ok_with_electronic_ticket(self):
        with replace_dynamic_setting_keys('TRAIN_BACKEND_USE_PROTOBUFS', schemas=False, facility=False):
            self._test_store_details_ok(electronic_ticket=True)

    @override_settings(TRAIN_WIZARD_API_INDEXER_HOST='localhost')
    def test_store_details_ok_with_electronic_ticket_with_protobufs(self):
        with replace_dynamic_setting_keys('TRAIN_BACKEND_USE_PROTOBUFS', schemas=True, facility=True):
            self._test_store_details_ok(electronic_ticket=True)

    @override_settings(TRAIN_WIZARD_API_INDEXER_HOST='localhost')
    def test_store_details_ok_without_electronic_ticket(self):
        with replace_dynamic_setting_keys('TRAIN_BACKEND_USE_PROTOBUFS', schemas=False, facility=False):
            self._test_store_details_ok(electronic_ticket=False)

    @override_settings(TRAIN_WIZARD_API_INDEXER_HOST='localhost')
    def test_store_details_ok_without_electronic_ticket_with_protobufs(self):
        with replace_dynamic_setting_keys('TRAIN_BACKEND_USE_PROTOBUFS', schemas=True, facility=True):
            self._test_store_details_ok(electronic_ticket=False)

    @override_settings(TRAIN_WIZARD_API_INDEXER_HOST=None)
    def test_store_details_train_wizard_data_host_is_unknown(self):
        with replace_dynamic_setting_keys('TRAIN_BACKEND_USE_PROTOBUFS', schemas=False, facility=False):
            self._client.store_details(self._make_details(electronic_ticket=False))

            assert self._fake_transport.post.call_count == 0

            assert self._fake_logger.info.call_count == 0
            self._fake_logger.warn.assert_called_once_with(
                'Train wizard api host is not initialized'
            )

    @override_settings(TRAIN_WIZARD_API_INDEXER_HOST=None)
    def test_store_details_train_wizard_data_host_is_unknown_with_protobufs(self):
        with replace_dynamic_setting_keys('TRAIN_BACKEND_USE_PROTOBUFS', schemas=True, facility=True):
            self._client.store_details(self._make_details(electronic_ticket=False))

            assert self._fake_transport.post.call_count == 0

            assert self._fake_logger.info.call_count == 0
            self._fake_logger.warn.assert_called_once_with(
                'Train wizard api host is not initialized'
            )

    @override_settings(TRAIN_WIZARD_API_INDEXER_HOST='localhost')
    def test_store_details_train_wizard_api_has_returned_500(self):
        with replace_dynamic_setting_keys('TRAIN_BACKEND_USE_PROTOBUFS', schemas=False, facility=False):
            self._fake_response.raise_for_status.side_effect = requests.HTTPError()
            self._client.store_details(self._make_details(electronic_ticket=False))

            expected_path = (
                '/indexer/public-api/train/'
                '?departure_point_express_id=200'
                '&arrival_point_express_id=2000'
                '&departure_dt=2000-01-01T09%3A00%3A00%2B00%3A00'
                '&number=123%D0%96'
            )
            self._fake_logger.info.assert_called_once_with('Start index: [%s]', expected_path)
            self._fake_logger.warn.assert_called_once_with(
                'Can not index document to train wizard api: [%s]',
                expected_path,
                exc_info=True
            )

    @override_settings(TRAIN_WIZARD_API_INDEXER_HOST='localhost')
    def test_store_details_train_wizard_api_has_returned_500_with_protobufs(self):
        with replace_dynamic_setting_keys('TRAIN_BACKEND_USE_PROTOBUFS', schemas=True, facility=True):
            self._fake_response.raise_for_status.side_effect = requests.HTTPError()
            self._client.store_details(self._make_details(electronic_ticket=False))

            expected_path = (
                '/indexer/public-api/train/'
                '?departure_point_express_id=200'
                '&arrival_point_express_id=2000'
                '&departure_dt=2000-01-01T09%3A00%3A00%2B00%3A00'
                '&number=123%D0%96'
            )
            self._fake_logger.info.assert_called_once_with('Start index: [%s]', expected_path)
            self._fake_logger.warn.assert_called_once_with(
                'Can not index document to train wizard api: [%s]',
                expected_path,
                exc_info=True
            )

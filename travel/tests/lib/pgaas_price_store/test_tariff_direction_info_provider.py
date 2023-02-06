# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from concurrent.futures import Future
from datetime import datetime, date
from django.conf import settings
from logging import Logger
from mock import Mock

from pytz import UTC

from common.tester.factories import create_settlement
from common.tester.testcase import TestCase
from travel.rasp.wizards.train_wizard_api.lib.express_system_provider import ExpressSystemProvider
from travel.rasp.wizards.train_wizard_api.lib.pgaas_price_store.tariff_direction_info_provider import (
    TariffDirectionInfoProvider,
    TariffDirectionInfoSource
)
from travel.rasp.wizards.train_wizard_api.lib.storage_timed_execute import wait_for_future_and_build_info


def _get_future_with_result(value):
    future = Future()
    future.set_result(value)
    return future


class TestTariffDirectionInfoProviderBase(TestCase):
    def setUp(self):
        self._m_tariff_direction_info_source = Mock(spec=TariffDirectionInfoSource)
        self._m_express_system_provider = Mock(spec=ExpressSystemProvider)
        self._m_railway_utils = Mock()
        self._m_logger = Mock(spec=Logger)

        self._m_railway_utils.get_railway_tz_by_point = Mock(return_value=UTC)

        self._provider = TariffDirectionInfoProvider(
            tariff_direction_info_source=self._m_tariff_direction_info_source,
            express_system_provider=self._m_express_system_provider,
            railway_utils=self._m_railway_utils,
            logger=self._m_logger
        )

        self._from_point = create_settlement()
        self._to_point = create_settlement()
        self._timeout = settings.DBAAS_TRAIN_WIZARD_API_SELECT_TIMEOUT

    def _make_raw_tariff_train_info_record(self, departure_dt):
        return {
            'departure_dt': departure_dt,
            'arrival_dt': '{}_some_arrival_dt'.format(departure_dt),
            'arrival_station_id': 'some_arrival_station_id'.format(departure_dt),
            'departure_station_id': 'some_departure_station_id'.format(departure_dt),
            'number': 'some_number'.format(departure_dt),
            'display_number': '776Ж',
            'electronic_ticket': True,
            'has_dynamic_pricing': False,
            'is_suburban': True,
            'two_storey': False,
            'coach_owners': ['ФПК'],
            'title_dict': 'some_title_dict'.format(departure_dt),
            'first_country_code': 'RU',
            'last_country_code': 'RU',
            'places': [
                {
                    'coach_type': '{}_some_coach_type'.format(departure_dt),
                    'count': '{}_some_count'.format(departure_dt),
                    'max_seats_in_the_same_car': '{}_some_max_seats_in_the_same_car'.format(departure_dt),
                    'price': {
                        'currency': 'RUB',
                        'value': 123
                    },
                    'price_details': {
                        'fee': '3',
                        'service_price': '20',
                        'several_prices': True,
                        'ticket_price': '100'
                    },
                },
            ]
        }


class TestFind(TestTariffDirectionInfoProviderBase):
    def test_points_with_unknown_express_id(self):
        self._m_express_system_provider.find_express_id = Mock(return_value=None)

        future, context = self._provider.find(
            departure_point=self._from_point,
            arrival_point=self._to_point,
            departure_date=date(2017, 1, 1)
        )
        records, updated_info = wait_for_future_and_build_info(self._provider, future, context, self._timeout)

        self._m_logger.warn.assert_called_with(
            'Can not find prices by [%s-%s], because can not find express codes for one of points [%s-%s]',
            None, None,
            self._from_point.point_key, self._to_point.point_key
        )

        assert self._m_tariff_direction_info_source.find.call_count == 0
        assert len(records) == 0
        assert len(updated_info.records) == 0

    def test_empty_direction(self):
        self._m_express_system_provider.find_express_id = Mock(side_effect=[1, 2])
        future = _get_future_with_result(())
        self._m_tariff_direction_info_source.find = Mock(return_value=future)

        future, context = self._provider.find(
            departure_point=self._from_point,
            arrival_point=self._to_point,
            departure_date=date(2017, 1, 1)
        )
        records, updated_info = wait_for_future_and_build_info(self._provider, future, context, self._timeout)

        self._m_logger.info.assert_called_with(
            u'Start search: [%s(%s)-%s(%s)-%s-%s]',
            self._from_point.point_key, 1,
            self._to_point.point_key, 2,
            date(2017, 1, 1),
            1
        )
        self._m_tariff_direction_info_source.find.assert_called_with(
            departure_point_express_id=1,
            arrival_point_express_id=2,
            left_border=date(2016, 12, 31),
            right_border=date(2017, 1, 2),
        )

        assert len(records) == 0
        assert len(updated_info.records) == 3
        assert all(r.updated_at is None for r in updated_info.records)

    def test_result_found(self):
        self._m_express_system_provider.find_express_id = Mock(side_effect=[1, 2])
        future = _get_future_with_result([
            (
                [
                    self._make_raw_tariff_train_info_record('2017-01-01T'),
                    self._make_raw_tariff_train_info_record('2016-01-01T'),
                    self._make_raw_tariff_train_info_record('2018-01-01T')
                ],
                date(2017, 1, 1),
                datetime(2019, 1, 1),
            ),
        ])
        self._m_tariff_direction_info_source.find = Mock(return_value=future)

        future, context = self._provider.find(
            departure_point=self._from_point,
            arrival_point=self._to_point,
            departure_date=date(2017, 1, 1)
        )
        records, updated_info = wait_for_future_and_build_info(self._provider, future, context, self._timeout)

        self._m_logger.info.assert_called_with(
            u'Start search: [%s(%s)-%s(%s)-%s-%s]',
            self._from_point.point_key, 1,
            self._to_point.point_key, 2,
            date(2017, 1, 1),
            1
        )
        self._m_tariff_direction_info_source.find.assert_called_with(
            departure_point_express_id=1,
            arrival_point_express_id=2,
            left_border=date(2016, 12, 31),
            right_border=date(2017, 1, 2),
        )

        assert len(records) == 1
        assert records[0].departure_dt == '2017-01-01T'
        assert records[0].places[0].price_details == {
            'fee': '3',
            'service_price': '20',
            'several_prices': True,
            'ticket_price': '100'
        }
        assert len(updated_info.records) == 3


class TestFindTariffsByDirections(TestTariffDirectionInfoProviderBase):
    def test_points_with_unknown_express_id(self):
        self._m_express_system_provider.find_express_id = Mock(return_value=None)

        records = self._provider.find_tariffs_by_directions(
            directions=[(self._from_point, self._to_point)],
            min_departure_date=date(2017, 1, 1),
            max_departure_date=date(2017, 1, 2)
        )

        self._m_logger.warn.assert_called_with(
            'Skipping prices for [%s-%s], because can not find express codes for one of points [%s-%s]',
            None, None,
            self._from_point.point_key, self._to_point.point_key
        )

        assert self._m_tariff_direction_info_source.find_tariffs_by_directions.call_count == 0
        assert len(records) == 0

    def test_empty_direction(self):
        self._m_express_system_provider.find_express_id = Mock(side_effect=[1, 2])
        self._m_tariff_direction_info_source.find_tariffs_by_directions = Mock(return_value=())

        records = self._provider.find_tariffs_by_directions(
            directions=[(self._from_point, self._to_point)],
            min_departure_date=date(2017, 1, 1),
            max_departure_date=date(2017, 1, 2)
        )

        self._m_logger.info.assert_called_with(
            'Start search for directions: [%s]',
            [(self._from_point.point_key, self._to_point.point_key)]
        )
        self._m_tariff_direction_info_source.find_tariffs_by_directions.assert_called_with(
            [(1, 2)], date(2017, 1, 1), date(2017, 1, 2),
        )
        assert len(records) == 0

    def test_find_price_info(self):
        self._m_express_system_provider.find_express_id = Mock(side_effect=[1, 2])
        self._m_tariff_direction_info_source.find_tariffs_by_directions = Mock(return_value=[
            (
                [
                    self._make_raw_tariff_train_info_record('2017-01-01T'),
                    self._make_raw_tariff_train_info_record('2016-01-01T'),
                    self._make_raw_tariff_train_info_record('2018-01-01T')
                ],
                date(2017, 1, 1),
                datetime(2019, 1, 1),
            ),
        ])

        records = self._provider.find_tariffs_by_directions(
            [(self._from_point, self._to_point)],
            min_departure_date=date(2017, 1, 1),
            max_departure_date=date(2017, 1, 2)
        )

        self._m_logger.info.assert_called_with(
            'Start search for directions: [%s]',
            [(self._from_point.point_key, self._to_point.point_key)]
        )
        self._m_tariff_direction_info_source.find_tariffs_by_directions.assert_called_with(
            [(1, 2)],
            date(2017, 1, 1),
            date(2017, 1, 2),
        )

        assert len(records) == 1
        assert records[0].departure_dt == '2017-01-01T'
        assert records[0].places[0].price_details == {
            'fee': '3',
            'service_price': '20',
            'several_prices': True,
            'ticket_price': '100'
        }

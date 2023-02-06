# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

from datetime import date

import hamcrest
import mock
from django.utils.datastructures import MultiValueDict

from common.tester.factories import create_settlement, create_station
from common.tester.testcase import TestCase
from travel.rasp.wizards.wizard_lib.direction.filters import BaseFilters, DepartureTimeFilter, ArrivalTimeFilter
from travel.rasp.wizards.wizard_lib.direction.logger import DirectionLogger
from travel.rasp.wizards.wizard_lib.direction.sorting import DEFAULT_SORTING
from travel.rasp.wizards.wizard_lib.experiment_flags import ExperimentFlag
from travel.rasp.wizards.wizard_lib.serialization.direction import DirectionQuery


class FiltersForTest(BaseFilters):
    FACTORIES = (
        ('departure_time', DepartureTimeFilter.load),
        ('arrival_time', ArrivalTimeFilter.load),
    )


class TestDirectionLogger(TestCase):
    def setUp(self):
        self._departure_point = create_settlement()
        self._arrival_point = create_station()
        self._query = DirectionQuery(
            departure_point=self._departure_point,
            arrival_point=self._arrival_point,
            departure_date=date(2017, 9, 1),
            language='ru',
            experiment_flags=[ExperimentFlag.EXPERIMENTAL_SEARCH],
            sorting=DEFAULT_SORTING,
            filters=FiltersForTest.load(MultiValueDict({
                'departure_time': ['00:00-06:00'],
                'arrival_time': ['06:00-12:00']
            })),
            tld='ru',
            limit=None,
        )

        self._fake_time_provider = mock.Mock(**{'time.return_value': 100500})
        self._fake_yt_logger = mock.Mock()
        self._logger = DirectionLogger(self._fake_time_provider, self._fake_yt_logger)

    def test_store_query(self):
        context = self._logger.start_log()
        context.store_query(query=self._query)

        hamcrest.assert_that(context.get_data(), hamcrest.has_entries({
            'departure_settlement_id': self._departure_point.id,
            'departure_station_id': None,
            'arrival_settlement_id': None,
            'arrival_station_id': self._arrival_point.id,
            'departure_date': 1504224000,  # GMT: Friday, September 1, 2017 12:00:00 AM

            'filters_departure_time': ['00:00-06:00'],
            'filters_arrival_time': ['06:00-12:00'],

            'sorting': 'departure',
            'language': 'ru',
            'experiment_flags': ['RASPWIZARDS-557']
        }))

    def test_store_segments(self):
        context = self._logger.start_log()
        context.store_segments([mock.Mock(), mock.Mock(), mock.Mock()])

        hamcrest.assert_that(context.get_data(), hamcrest.has_entry('before_filters_all_segments_count', 3))

    def test_store_filtered_segments(self):
        context = self._logger.start_log()
        context.store_filtered_segments([mock.Mock()])

        hamcrest.assert_that(context.get_data(), hamcrest.has_entry('filters_all_segments_count', 1))

    def test_all_cases(self):
        context = self._logger.start_log()
        context.store_query(self._query)
        context.store_segments([mock.Mock(), mock.Mock(), mock.Mock()])
        context.store_filtered_segments([mock.Mock()])

        hamcrest.assert_that(context.get_data(), hamcrest.has_entries({
            'departure_settlement_id': self._departure_point.id,
            'departure_station_id': None,
            'arrival_settlement_id': None,
            'arrival_station_id': self._arrival_point.id,
            'departure_date': 1504224000,
            'experiment_flags': ['RASPWIZARDS-557'],
            'filters_arrival_time': ['06:00-12:00'],
            'filters_departure_time': ['00:00-06:00'],
            'language': 'ru',
            'sorting': 'departure',

            'before_filters_all_segments_count': 3,
            'filters_all_segments_count': 1,
        }))

    def test_decorate(self):
        func = mock.MagicMock()
        self._logger.decorate(func)(10, foo=1)

        func.assert_called_once_with(10, foo=1, log_context=mock.ANY)

    def test_defaults(self):
        logger = DirectionLogger(
            self._fake_time_provider, self._fake_yt_logger, defaults={'foo': 'bar', 'baz': 42}
        )
        context = logger.start_log()

        assert context.get_data() == {
            'foo': 'bar',
            'baz': 42,
            'unixtime': 100500,
        }

    def test_adapters(self):
        query_adapter = mock.Mock(return_value={'query_data_1': 123, 'query_data_2': 456})
        segments_adapter = mock.Mock(return_value={'segments_data_1': 789})
        logger = DirectionLogger(
            self._fake_time_provider,
            self._fake_yt_logger,
            query_adapter=query_adapter,
            segments_adapter=segments_adapter
        )
        context = logger.start_log()
        context.store_query(self._query)
        context.store_segments(mock.sentinel.segments)
        context.store_filtered_segments(mock.sentinel.filtered_segments)

        hamcrest.assert_that(context.get_data(), hamcrest.has_entries({
            'query_data_1': 123,
            'query_data_2': 456,
            'before_filters_segments_data_1': 789,
            'filters_segments_data_1': 789,
        }))
        query_adapter.assert_called_once_with(self._query)
        assert segments_adapter.mock_calls == [
            mock.call(mock.sentinel.segments), mock.call(mock.sentinel.filtered_segments)
        ]

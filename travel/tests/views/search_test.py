from datetime import date, datetime
from freezegun import freeze_time
from logging import getLogger
from mock import Mock
from typing import cast

from unittest import TestCase

from travel.avia.price_index.lib.base_currency_provider import BaseCurrencyProvider
from travel.avia.price_index.lib.passengers_multiplier import PassengersMultiplier
from travel.avia.price_index.lib.query_searcher import QuerySearcher
from travel.avia.price_index.lib.search_query_finder import History, QueryFinder
from travel.avia.price_index.models.daterange import DateRange
from travel.avia.price_index.models.query import Query
from travel.avia.price_index.models.search_form import SearchForm
from travel.avia.price_index.schemas.filters import FiltersSchema
from travel.avia.price_index.views.search import SearchView, SearchPreciseLogger


class PassengersMultiplierTest(TestCase):
    def _create_query(self, forward_date, backward_date=None, adults=1, children=0, infants=0):
        return Query(
            national_version_id=1,
            from_id=10,
            to_id=100,
            forward_date=forward_date,
            backward_date=backward_date,
            adults_count=adults,
            children_count=children,
            infants_count=infants,
        )

    def setUp(self):
        self.maxDiff = None
        self._session = Mock()
        self._search_precise_logger = Mock()
        self._passengers_multiplier = PassengersMultiplier()
        self._query_finder = Mock()
        self._query_searcher = Mock()
        self._base_currency_provider = Mock()
        self._base_currency_provider.get_code = Mock(return_value='ru')

        self._logger = getLogger('debug')

        self._view = SearchView(
            search_precise_logger=cast(SearchPreciseLogger, self._search_precise_logger),
            passengers_multiplier=self._passengers_multiplier,
            query_searcher=cast(QuerySearcher, self._query_searcher),
            search_query_finder=cast(QueryFinder, self._query_finder),
            base_currency_provider=cast(BaseCurrencyProvider, self._base_currency_provider),
            logger=self._logger,
        )

        self.query1 = self._create_query(date(2017, 1, 1), adults=2)
        self.query2 = self._create_query(date(2017, 1, 2), adults=2)
        self.query3 = self._create_query(date(2017, 1, 3), adults=2)

        self.approximate_query1 = self._create_query(date(2017, 1, 1), adults=1)
        self.approximate_query2 = self._create_query(date(2017, 1, 2), adults=1)
        self.approximate_query3 = self._create_query(date(2017, 1, 3), adults=1)

        self.search_form = SearchForm(
            date_range=DateRange(date(2017, 1, 1), date(2017, 1, 3)),
            query=self.query1,
            filters=FiltersSchema().load({}),
            format_version="1.1.0",
        )

    def test_empty(self):
        self._query_finder.find_by_date_range = Mock(return_value={})
        self._query_searcher.batch_find = Mock(return_value={})

        data = self._view.process(self._session, self.search_form)

        self.assertDictEqual({}, data)

    @freeze_time(datetime(2016, 1, 3, 17, 0, 0))
    def test_all_find_by_exac_queries_all_prices(self):
        self._query_finder.find_by_date_range = Mock(
            return_value={
                self.query1: History(datetime(2016, 1, 1, 17, 24, 30, 444999), True, requested_query=self.query1),
                self.query2: History(datetime(2016, 1, 2, 7, 14, 56), True, requested_query=self.query2),
                self.query3: History(datetime(2016, 1, 3, 16, 7, 34), True, requested_query=self.query3),
            }
        )
        self._query_searcher.batch_find = Mock(
            return_value={
                self.query1: 1000,
                self.query2: 2000,
                self.query3: 3000,
            }
        )

        data = self._view.process(self._session, self.search_form)

        self.assertDictEqual(
            {
                '2017-01-01': {
                    'status': 'has-data',
                    'baseValue': 1000,
                    'roughly': False,
                    'value': 1000,
                    'currency': 'ru',
                    'updatedAt': '2016-01-01T17:24:30.444999',
                    'expired': True,
                },
                '2017-01-02': {
                    'status': 'has-data',
                    'baseValue': 2000,
                    'roughly': False,
                    'value': 2000,
                    'currency': 'ru',
                    'updatedAt': '2016-01-02T07:14:56',
                    'expired': True,
                },
                '2017-01-03': {
                    'status': 'has-data',
                    'baseValue': 3000,
                    'roughly': False,
                    'value': 3000,
                    'currency': 'ru',
                    'updatedAt': '2016-01-03T16:07:34',
                    'expired': False,
                },
            },
            data,
        )

    @freeze_time(datetime(2016, 1, 1, 23, 0, 0))
    def test_all_find_by_exac_queries_some_prices_and_we_does_not_expected_it(self):
        self._query_finder.find_by_date_range = Mock(
            return_value={
                self.query1: History(datetime(2016, 1, 1), True, requested_query=self.query1),
                self.query2: History(datetime(2016, 1, 2), True, requested_query=self.query2),
                self.query3: History(datetime(2016, 1, 3), True, requested_query=self.query3),
            }
        )
        self._query_searcher.batch_find = Mock(
            return_value={
                self.query1: 1000,
                self.query3: 3000,
            }
        )

        data = self._view.process(self._session, self.search_form)

        self.assertDictEqual(
            {
                '2017-01-01': {
                    'status': 'has-data',
                    'baseValue': 1000,
                    'roughly': False,
                    'value': 1000,
                    'currency': 'ru',
                    'updatedAt': '2016-01-01T00:00:00',
                    'expired': True,
                },
                '2017-01-02': {
                    'status': 'no-data',
                    'updatedAt': '2016-01-02T00:00:00',
                    'expired': False,
                },
                '2017-01-03': {
                    'status': 'has-data',
                    'baseValue': 3000,
                    'roughly': False,
                    'value': 3000,
                    'currency': 'ru',
                    'updatedAt': '2016-01-03T00:00:00',
                    'expired': False,
                },
            },
            data,
        )

    @freeze_time(datetime(2016, 1, 1, 0, 0, 0))
    def test_all_find_by_exac_queries_some_prices_and_because_filter_is_too_strong(self):
        self._query_finder.find_by_date_range = Mock(
            return_value={
                self.query1: History(datetime(2016, 1, 1), True, requested_query=self.query1),
                self.query2: History(datetime(2016, 1, 2), True, requested_query=self.query2),
                self.query3: History(datetime(2016, 1, 3), True, requested_query=self.query3),
            }
        )
        self._query_searcher.batch_find = Mock(
            return_value={
                self.query1: 1000,
                self.query3: 3000,
            }
        )

        self.search_form = SearchForm(
            date_range=DateRange(date(2017, 1, 1), date(2017, 1, 3)),
            query=self.query1,
            filters=FiltersSchema().load({"withBaggage": True}),
            format_version="1.1.0",
        )

        data = self._view.process(self._session, self.search_form)

        self.assertDictEqual(
            {
                '2017-01-01': {
                    'status': 'has-data',
                    'baseValue': 1000,
                    'roughly': False,
                    'value': 1000,
                    'currency': 'ru',
                    'updatedAt': '2016-01-01T00:00:00',
                    'expired': False,
                },
                '2017-01-02': {
                    'status': 'no-filter-data',
                    'updatedAt': '2016-01-02T00:00:00',
                    'expired': False,
                },
                '2017-01-03': {
                    'status': 'has-data',
                    'baseValue': 3000,
                    'roughly': False,
                    'value': 3000,
                    'currency': 'ru',
                    'updatedAt': '2016-01-03T00:00:00',
                    'expired': False,
                },
            },
            data,
        )

    @freeze_time(datetime(2015, 12, 31, 23, 0, 0))
    def test_all_find_by_approximate_queries_some_prices_and_we_expected_it(self):
        self._query_finder.find_by_date_range = Mock(
            return_value={
                self.query1: History(datetime(2016, 1, 1), True, requested_query=self.query1),
                self.query2: History(datetime(2016, 1, 2), False, requested_query=self.query2),
                self.query3: History(datetime(2016, 1, 3), True, requested_query=self.query3),
            }
        )
        self._query_searcher.batch_find = Mock(
            return_value={
                self.query1: 1000,
                self.query3: 3000,
            }
        )

        data = self._view.process(self._session, self.search_form)

        self.assertDictEqual(
            {
                '2017-01-01': {
                    'status': 'has-data',
                    'baseValue': 1000,
                    'roughly': False,
                    'value': 1000,
                    'currency': 'ru',
                    'updatedAt': '2016-01-01T00:00:00',
                    'expired': False,
                },
                '2017-01-02': {
                    'status': 'no-data',
                    'updatedAt': '2016-01-02T00:00:00',
                    'expired': False,
                },
                '2017-01-03': {
                    'status': 'has-data',
                    'baseValue': 3000,
                    'roughly': False,
                    'value': 3000,
                    'currency': 'ru',
                    'updatedAt': '2016-01-03T00:00:00',
                    'expired': False,
                },
            },
            data,
        )

    @freeze_time(datetime(2017, 1, 1, 0, 0, 0))
    def test_all_find_by_approximate_queries_all_prices(self):
        self._query_finder.find_by_date_range = Mock(
            return_value={
                self.query1: History(datetime(2016, 1, 1), True, requested_query=self.approximate_query1),
                self.query2: History(datetime(2016, 1, 2), True, requested_query=self.approximate_query2),
                self.query3: History(datetime(2016, 1, 3), True, requested_query=self.approximate_query3),
            }
        )
        self._query_searcher.batch_find = Mock(
            return_value={
                self.approximate_query1: 1000,
                self.approximate_query2: 2000,
                self.approximate_query3: 3000,
            }
        )

        data = self._view.process(self._session, self.search_form)

        self.assertDictEqual(
            {
                '2017-01-01': {
                    'status': 'has-data',
                    'baseValue': 2000,
                    'roughly': True,
                    'value': 2000,
                    'currency': 'ru',
                    'updatedAt': '2016-01-01T00:00:00',
                    'expired': True,
                },
                '2017-01-02': {
                    'status': 'has-data',
                    'baseValue': 4000,
                    'roughly': True,
                    'value': 4000,
                    'currency': 'ru',
                    'updatedAt': '2016-01-02T00:00:00',
                    'expired': True,
                },
                '2017-01-03': {
                    'status': 'has-data',
                    'baseValue': 6000,
                    'roughly': True,
                    'value': 6000,
                    'currency': 'ru',
                    'updatedAt': '2016-01-03T00:00:00',
                    'expired': True,
                },
            },
            data,
        )

    @freeze_time(datetime(2016, 1, 3, 0, 0, 0))
    def test_all_find_by_approximate_queries_some_prices_and_we_does_not_expected_it(self):
        self._query_finder.find_by_date_range = Mock(
            return_value={
                self.query1: History(datetime(2016, 1, 1), True, requested_query=self.approximate_query1),
                self.query2: History(datetime(2016, 1, 2), True, requested_query=self.approximate_query2),
                self.query3: History(datetime(2016, 1, 3), True, requested_query=self.approximate_query3),
            }
        )
        self._query_searcher.batch_find = Mock(
            return_value={
                self.approximate_query1: 1000,
                self.approximate_query3: 3000,
            }
        )

        data = self._view.process(self._session, self.search_form)

        self.assertDictEqual(
            {
                '2017-01-01': {
                    'status': 'has-data',
                    'baseValue': 2000,
                    'roughly': True,
                    'value': 2000,
                    'currency': 'ru',
                    'updatedAt': '2016-01-01T00:00:00',
                    'expired': True,
                },
                '2017-01-02': {
                    'status': 'no-data',
                    'updatedAt': '2016-01-02T00:00:00',
                    'expired': True
                },
                '2017-01-03': {
                    'status': 'has-data',
                    'baseValue': 6000,
                    'roughly': True,
                    'value': 6000,
                    'currency': 'ru',
                    'updatedAt': '2016-01-03T00:00:00',
                    'expired': False,
                },
            },
            data,
        )

    @freeze_time(datetime(2016, 1, 3, 0, 0, 0))
    def test_all_find_by_approximate_queries_some_prices_and_we_expect_it(self):
        self._query_finder.find_by_date_range = Mock(
            return_value={
                self.query1: History(datetime(2016, 1, 1), True, requested_query=self.approximate_query1),
                self.query2: History(datetime(2016, 1, 2), False, requested_query=self.approximate_query2),
                self.query3: History(datetime(2016, 1, 3), True, requested_query=self.approximate_query3),
            }
        )
        self._query_searcher.batch_find = Mock(
            return_value={
                self.approximate_query1: 1000,
                self.approximate_query3: 3000,
            }
        )

        data = self._view.process(self._session, self.search_form)

        self.assertDictEqual(
            {
                '2017-01-01': {
                    'status': 'has-data',
                    'baseValue': 2000,
                    'roughly': True,
                    'value': 2000,
                    'currency': 'ru',
                    'updatedAt': '2016-01-01T00:00:00',
                    'expired': True,
                },
                '2017-01-02': {
                    'status': 'no-data',
                    'updatedAt': '2016-01-02T00:00:00',
                    'expired': True,
                },
                '2017-01-03': {
                    'status': 'has-data',
                    'baseValue': 6000,
                    'roughly': True,
                    'value': 6000,
                    'currency': 'ru',
                    'updatedAt': '2016-01-03T00:00:00',
                    'expired': False,
                },
            },
            data,
        )

    @freeze_time(datetime(2016, 1, 2, 0, 0, 0))
    def test_all_find_by_approximate_queries_some_prices_and_because_filter_is_too_strong(self):
        self._query_finder.find_by_date_range = Mock(
            return_value={
                self.query1: History(datetime(2016, 1, 1), True, requested_query=self.approximate_query1),
                self.query2: History(datetime(2016, 1, 2), True, requested_query=self.approximate_query2),
                self.query3: History(datetime(2016, 1, 3), True, requested_query=self.approximate_query3),
            }
        )
        self._query_searcher.batch_find = Mock(
            return_value={
                self.approximate_query1: 1000,
                self.approximate_query3: 3000,
            }
        )

        self.search_form = SearchForm(
            date_range=DateRange(date(2017, 1, 1), date(2017, 1, 3)),
            query=self.query1,
            filters=FiltersSchema().load({"withBaggage": True}),
            format_version="1.1.0",
        )

        print(f'asdflkjasdf;lkj {self.search_form.filters}')
        data = self._view.process(self._session, self.search_form)

        self.assertDictEqual(
            {
                '2017-01-01': {
                    'status': 'has-data',
                    'baseValue': 2000,
                    'roughly': True,
                    'value': 2000,
                    'currency': 'ru',
                    'updatedAt': '2016-01-01T00:00:00',
                    'expired': True,
                },
                '2017-01-02': {
                    'status': 'no-filter-data',
                    'updatedAt': '2016-01-02T00:00:00',
                    'expired': False,
                },
                '2017-01-03': {
                    'status': 'has-data',
                    'baseValue': 6000,
                    'roughly': True,
                    'value': 6000,
                    'currency': 'ru',
                    'updatedAt': '2016-01-03T00:00:00',
                    'expired': False,
                },
            },
            data,
        )

# flake8: noqa

from datetime import date, datetime
from freezegun import freeze_time
from logging import getLogger
from mock import Mock, patch
from typing import cast

from unittest import TestCase

from travel.avia.price_index.lib.currency_provider import CurrencyModel, currency_provider
from travel.avia.price_index.lib.query_searcher.fast_min_query_searcher import FastMinQuerySearcher
from travel.avia.price_index.lib.rates_provider import rates_provider
from travel.avia.price_index.lib.search_query_finder import History, QueryFinder
from travel.avia.price_index.models.batch_prices_form import BatchPricesForm
from travel.avia.price_index.models.query import Query
from travel.avia.price_index.views.min_price_batch_search_view import MinPriceBatchSearchView, BatchPreciseLogger


@patch('travel.avia.price_index.lib.rates_provider.rates_provider', Mock())
class MinPriceBatchSearchTests(TestCase):
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
        self._query_finder = Mock()
        self._batch_precision_logger = Mock()
        self._fast_min_query_searcher = Mock()
        self._logger = getLogger('debug')

        self._view = MinPriceBatchSearchView(
            batch_precision_logger=cast(BatchPreciseLogger, self._batch_precision_logger),
            fast_min_query_searcher=cast(FastMinQuerySearcher, self._fast_min_query_searcher),
            search_query_finder=cast(QueryFinder, self._query_finder),
            logger=self._logger,
        )

        self.query1 = self._create_query(date(2021, 12, 20), adults=1)

        self.batch_prices_form = BatchPricesForm(
            national_version_id='ru',
            query_source=None,
            queries=[self.query1],
        )

        global rates_provider
        rates_provider._base_currency_id_by_nv_id = {'ru': 'ggg'}
        global currency_provider
        currency_provider.get_by_id = lambda *args: CurrencyModel(pk=4, code='asdf')

    @freeze_time(datetime(2016, 1, 3, 17, 0, 0))
    def test_all_find_by_exac_queries_all_prices(self):
        self._fast_min_query_searcher.batch_find = Mock(
            return_value=({self.query1: 555}, {self.query1: None})
        )

        data = self._view.process(self._session, self.batch_prices_form)

        self.assertListEqual(
            [
                {
                    'from_id': 10,
                    'to_id': 100,
                    'adults_count': 1,
                    'children_count': 0,
                    'infants_count': 0,
                    'forward_date': '2021-12-20',
                    'min_price': {'value': 555, 'currency': 'asdf'},
                    'updatedAt': None,
                    'expired': None,
                },
            ],
            data,
        )

    @freeze_time(datetime(2016, 1, 3, 17, 0, 0))
    def test_with_none_updated_at(self):
        self._fast_min_query_searcher.batch_find = Mock(
            return_value=(
                {
                    self.query1: 555,
                },
                {
                    self.query1: None,
                },
            ),
        )

        data = self._view.process(self._session, self.batch_prices_form)

        self.assertListEqual(
            [
                {
                    'from_id': 10,
                    'to_id': 100,
                    'adults_count': 1,
                    'children_count': 0,
                    'infants_count': 0,
                    'forward_date': '2021-12-20',
                    'min_price': {'value': 555, 'currency': 'asdf'},
                    'updatedAt': None,
                    'expired': None,
                },
            ],
            data,
        )

    @freeze_time(datetime(2016, 1, 3, 17, 0, 0))
    def test_simple(self):
        self._fast_min_query_searcher.batch_find = Mock(
            return_value=(
                {
                    self.query1: 555,
                },
                {
                    self.query1: History(datetime(2016, 1, 3, 17, 10, 0), True, self.query1),
                },
            ),
        )

        data = self._view.process(self._session, self.batch_prices_form)

        self.assertListEqual(
            [
                {
                    'from_id': 10,
                    'to_id': 100,
                    'adults_count': 1,
                    'children_count': 0,
                    'infants_count': 0,
                    'forward_date': '2021-12-20',
                    'min_price': {'value': 555, 'currency': 'asdf'},
                    'updatedAt': '2016-01-03T17:10:00',
                    'expired': False,
                },
            ],
            data,
        )

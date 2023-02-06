from unittest import TestCase

from datetime import date, datetime
from mock import Mock
from requests import Session
from typing import cast

from travel.avia.price_index.lib.date_range_iterator import DateRangeIterator
from travel.avia.price_index.lib.search_query_finder import (
    History,
    HistoryFetcher,
    QueryFinderForMulti,
    QueryFinderForSingle,
)
from travel.avia.price_index.models.daterange import DateRange
from travel.avia.price_index.models.query import Query
from travel.avia.price_index.models.search_form import SearchForm
from travel.avia.price_index.schemas.filters import FiltersSchema


def _create_query(forward_date, backward_date, adults_count=1):
    return Query(
        national_version_id=1,
        from_id=10,
        to_id=100,
        forward_date=forward_date,
        backward_date=backward_date,
        adults_count=adults_count,
        children_count=0,
        infants_count=0,
    )


class QueryFinderTestCase(TestCase):
    def setUp(self):
        self.maxDiff = None
        self._fake_session = Mock()
        self._history_fetcher = Mock

        self.search_query = None
        self.query1 = None
        self.query2 = None
        self.query3 = None

        self._finder = QueryFinderForSingle(
            history_fetcher=cast(HistoryFetcher, Mock()), date_range_iterator=DateRangeIterator()
        )

    def _run_test(self, mockerd_history, history_queries):
        self._history_fetcher.butch_fetch = Mock(return_value=mockerd_history)

        search_form = SearchForm(
            date_range=DateRange(date(2017, 1, 1), date(2017, 1, 3)),
            query=self.search_query,
            filters=FiltersSchema().load({}),
        )

        result = self._finder.find_by_date_range(cast(Session, self._fake_session), search_form)

        expected = {
            self.query1: history_queries[0] and History(datetime(2016, 1, 1), True, history_queries[0]),
            self.query2: history_queries[1] and History(datetime(2016, 2, 1), False, history_queries[1]),
            self.query3: history_queries[2] and History(datetime(2016, 3, 1), True, history_queries[2]),
        }

        self.assertDictEqual(expected, result)


class QueryFinderForSingleTestCase(QueryFinderTestCase):
    def setUp(self):
        super(QueryFinderForSingleTestCase, self).setUp()
        self._finder = QueryFinderForSingle(
            history_fetcher=cast(HistoryFetcher, Mock()), date_range_iterator=DateRangeIterator()
        )


class QueryFinderForMultiTestCase(QueryFinderTestCase):
    def setUp(self):
        super(QueryFinderForMultiTestCase, self).setUp()
        self._finder = QueryFinderForMulti(
            history_fetcher=cast(HistoryFetcher, Mock()), date_range_iterator=DateRangeIterator()
        )


class QueryFinderForSingleForOneWaySearchTest(QueryFinderForSingleTestCase):
    def setUp(self):
        super(QueryFinderForSingleForOneWaySearchTest, self).setUp()

        self.search_query = _create_query(date(2017, 1, 1), None)
        self.query1 = _create_query(date(2017, 1, 1), None)
        self.query2 = _create_query(date(2017, 1, 2), None)
        self.query3 = _create_query(date(2017, 1, 3), None)

    def test_history_for_queries_unknown(self):
        self._run_test({}, [None, None, None])

    def test_history_for_queries_with_known_history(self):
        self._run_test(
            {
                self.query1: (datetime(2016, 1, 1), True),
                self.query2: (datetime(2016, 2, 1), False),
                self.query3: (datetime(2016, 3, 1), True),
            },
            [self.query1, self.query2, self.query3],
        )

    def test_history_for_queries_with_hole(self):
        self._run_test(
            {
                self.query1: (datetime(2016, 1, 1), True),
                self.query3: (datetime(2016, 3, 1), True),
            },
            [self.query1, None, self.query3],
        )


class QueryFinderForSingleForTwoWaySearchTest(QueryFinderForSingleForOneWaySearchTest):
    def setUp(self):
        super(QueryFinderForSingleForTwoWaySearchTest, self).setUp()

        self.search_query = _create_query(date(2017, 1, 1), date(2017, 1, 2))
        self.query1 = _create_query(date(2017, 1, 1), date(2017, 1, 2))
        self.query2 = _create_query(date(2017, 1, 2), date(2017, 1, 3))
        self.query3 = _create_query(date(2017, 1, 3), date(2017, 1, 4))


class QueryFinderForMultiForOneWaySearchTest(QueryFinderForMultiTestCase):
    def setUp(self):
        super(QueryFinderForMultiForOneWaySearchTest, self).setUp()

        self.search_query = _create_query(date(2017, 1, 1), None, 2)
        self.query1 = _create_query(date(2017, 1, 1), None, 2)
        self.query2 = _create_query(date(2017, 1, 2), None, 2)
        self.query3 = _create_query(date(2017, 1, 3), None, 2)
        self.approximate_query1 = _create_query(date(2017, 1, 1), None, 1)
        self.approximate_query2 = _create_query(date(2017, 1, 2), None, 1)
        self.approximate_query3 = _create_query(date(2017, 1, 3), None, 1)

    def test_history_for_queries_unknown(self):
        self._run_test({}, [None, None, None])

    def test_history_for_queries_with_known_exec_only_history(self):
        self._run_test(
            {
                self.query1: (datetime(2016, 1, 1), True),
                self.query2: (datetime(2016, 2, 1), False),
                self.query3: (datetime(2016, 3, 1), True),
            },
            [self.query1, self.query2, self.query3],
        )

    def test_history_for_queries_with_known_history_and_approximate_history_too_old(self):
        self._run_test(
            {
                self.query1: (datetime(2016, 1, 1), True),
                self.query2: (datetime(2016, 2, 1), False),
                self.query3: (datetime(2016, 3, 1), True),
                self.approximate_query1: (datetime(2015, 1, 1), True),
                self.approximate_query2: (datetime(2015, 2, 1), False),
                self.approximate_query3: (datetime(2015, 3, 1), True),
            },
            [self.query1, self.query2, self.query3],
        )

    def test_history_for_queries_with_known_history_and_approximate_history_too_new(self):
        self._run_test(
            {
                self.query1: (datetime(2015, 1, 1), True),
                self.query2: (datetime(2015, 2, 1), False),
                self.query3: (datetime(2015, 3, 1), True),
                self.approximate_query1: (datetime(2016, 1, 1), True),
                self.approximate_query2: (datetime(2016, 2, 1), False),
                self.approximate_query3: (datetime(2016, 3, 1), True),
            },
            [self.approximate_query1, self.approximate_query2, self.approximate_query3],
        )

    def test_history_for_queries_with_known_history_and_some_approximate_history_too_new(self):
        self._run_test(
            {
                self.query1: (datetime(2016, 1, 1), True),
                self.query2: (datetime(2015, 2, 1), False),
                self.query3: (datetime(2016, 3, 1), True),
                self.approximate_query1: (datetime(2016, 1, 1), True),
                self.approximate_query2: (datetime(2016, 2, 1), False),
                self.approximate_query3: (datetime(2016, 3, 1), True),
            },
            [self.query1, self.approximate_query2, self.query3],
        )

    def test_history_for_queries_with_hole(self):
        self._run_test(
            {
                self.query1: (datetime(2016, 1, 1), True),
                self.query3: (datetime(2016, 3, 1), True),
                self.approximate_query1: (datetime(2016, 1, 1), True),
                self.approximate_query3: (datetime(2016, 3, 1), True),
            },
            [self.query1, None, self.query3],
        )

    def test_history_for_queries_with_hole_in_exac_history(self):
        self._run_test(
            {
                self.query1: (datetime(2016, 1, 1), True),
                self.query3: (datetime(2016, 3, 1), True),
                self.approximate_query1: (datetime(2016, 1, 1), True),
                self.approximate_query2: (datetime(2016, 2, 1), False),
                self.approximate_query3: (datetime(2016, 3, 1), True),
            },
            [self.query1, self.approximate_query2, self.query3],
        )

    def test_history_for_queries_with_hole_in_approximate_history(self):
        self._run_test(
            {
                self.query1: (datetime(2015, 1, 1), True),
                self.query2: (datetime(2016, 2, 1), False),
                self.query3: (datetime(2015, 3, 1), True),
                self.approximate_query1: (datetime(2016, 1, 1), True),
                self.approximate_query3: (datetime(2016, 3, 1), True),
            },
            [self.approximate_query1, self.query2, self.approximate_query3],
        )


class QueryFinderForMultiForTwoWaySearchTest(QueryFinderForMultiForOneWaySearchTest):
    def setUp(self):
        super(QueryFinderForMultiForTwoWaySearchTest, self).setUp()

        self.search_query = _create_query(date(2017, 1, 1), date(2017, 1, 2), 2)
        self.query1 = _create_query(date(2017, 1, 1), date(2017, 1, 2), 2)
        self.query2 = _create_query(date(2017, 1, 2), date(2017, 1, 3), 2)
        self.query3 = _create_query(date(2017, 1, 3), date(2017, 1, 4), 2)
        self.approximate_query1 = _create_query(date(2017, 1, 1), date(2017, 1, 2), 1)
        self.approximate_query2 = _create_query(date(2017, 1, 2), date(2017, 1, 3), 1)
        self.approximate_query3 = _create_query(date(2017, 1, 3), date(2017, 1, 4), 1)

from datetime import date
from unittest import TestCase

from travel.avia.price_index.lib.date_range_iterator import DateRangeIterator
from travel.avia.price_index.models.daterange import DateRange
from travel.avia.price_index.models.query import Query


class DateRangeIteratorTest(TestCase):
    def setUp(self):
        self._iterator = DateRangeIterator()

    def _create_query(self, forward_date, backward_date=None):
        return Query(
            national_version_id=1,
            from_id=10,
            to_id=100,
            forward_date=forward_date,
            backward_date=backward_date,
            adults_count=1,
            children_count=0,
            infants_count=0,
        )

    def test_one_way_and_one_day_trip(self):
        query = self._create_query(forward_date=date(2017, 9, 1))
        date_range = DateRange(
            start=date(2017, 9, 1),
            end=date(2017, 9, 1),
        )

        result = list(self._iterator.iterate(query, date_range))

        assert result == [(date(2017, 9, 1), None)]

    def test_one_way_and_many_days(self):
        query = self._create_query(forward_date=date(2017, 9, 1))
        date_range = DateRange(
            start=date(2017, 9, 1),
            end=date(2017, 9, 10),
        )

        result = list(self._iterator.iterate(query, date_range))

        assert result == [(date(2017, 9, i), None) for i in range(1, 11, 1)]

    def test_one_day_trip(self):
        query = self._create_query(
            forward_date=date(2017, 9, 1),
            backward_date=date(2017, 9, 10),
        )
        date_range = DateRange(
            start=date(2017, 9, 1),
            end=date(2017, 9, 1),
        )

        result = list(self._iterator.iterate(query, date_range))

        assert result == [(date(2017, 9, 1), date(2017, 9, 10))]

    def test_many_days(self):
        query = self._create_query(
            forward_date=date(2017, 9, 1),
            backward_date=date(2017, 9, 10),
        )
        date_range = DateRange(
            start=date(2017, 9, 1),
            end=date(2017, 9, 10),
        )

        result = list(self._iterator.iterate(query, date_range))

        assert result == [(date(2017, 9, i), date(2017, 9, 9 + i)) for i in range(1, 11, 1)]

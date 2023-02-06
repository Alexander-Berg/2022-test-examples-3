from unittest import TestCase

from datetime import date

from travel.avia.price_index.lib.passengers_multiplier import PassengersMultiplier
from travel.avia.price_index.models.query import Query


class PassengersMultiplierTest(TestCase):
    def setUp(self):
        self._multiplier = PassengersMultiplier()

    def _create_query(self, adults, children, infants):
        return Query(
            national_version_id=1,
            from_id=10,
            to_id=100,
            forward_date=date(2017, 1, 1),
            backward_date=None,
            adults_count=adults,
            children_count=children,
            infants_count=infants,
        )

    def test_multiply_for_single(self):
        assert self._multiplier.multiply(666, 1, 0, 0) == 666

    def test_multiply_for_multi(self):
        assert self._multiplier.multiply(1000, 4, 3, 2) == 7000

    def test_normalize_for_single(self):
        assert self._multiplier.normalize(1000, self._create_query(1, 0, 0), self._create_query(1, 0, 0)) == 1000

    def test_normalize_single_to_multiple(self):
        assert self._multiplier.normalize(1000, self._create_query(1, 0, 0), self._create_query(2, 0, 0)) == 2000

    def test_normalize_unsupported_normalization(self):
        self.assertRaises(
            RuntimeError, self._multiplier.normalize, 1000, self._create_query(2, 0, 0), self._create_query(1, 0, 0)
        )

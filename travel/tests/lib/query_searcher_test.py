# -*- encoding: utf-8 -*-
from unittest import TestCase

from travel.avia.price_index.lib.query_searcher.filter_query_searcher import VariantsFilterApplier
from travel.avia.price_index.schemas.filters import FiltersSchema


class FilterTestCase(TestCase):
    def setUp(self):
        self._filters_applier = VariantsFilterApplier()

        variant_fields = [
            'forward_arrival_time_type',
            'forward_departure_time_type',
            'backward_arrival_time_type',
            'backward_departure_time_type',
        ]
        self.value1 = 3
        self.value2 = 4
        self.value3 = 1
        self.variant1 = dict.fromkeys(variant_fields, self.value1)
        self.variant2 = dict.fromkeys(variant_fields, self.value2)
        self.variant3 = dict.fromkeys(variant_fields, self.value3)

    def test_time_filter__as_int(self):
        keys = [
            'forwardArrival',
            'forwardDeparture',
            'backwardArrival',
            'backwardDeparture',
        ]
        for key in keys:
            self._run_test(
                [self.variant2],
                as_time_filter({key: self.value2}),
            )

    def test_time_filter__as_list(self):
        keys = [
            'forwardArrival',
            'forwardDeparture',
            'backwardArrival',
            'backwardDeparture',
        ]
        for key in keys:
            self._run_test(
                [self.variant2, self.variant3],
                as_time_filter({key: [self.value2, self.value3]}),
            )

    def test_time_filter__complex_filter(self):
        self._run_test(
            [self.variant3],
            as_time_filter(
                {
                    'forwardArrival': self.value3,
                    'forwardDeparture': [self.value1, self.value3],
                    'backwardArrival': [self.value2, self.value3],
                    'backwardDeparture': None,
                }
            ),
        )

    def _run_test(self, expected_variants, filters):
        actual_variants = [self.variant1, self.variant2, self.variant3]
        actual_variants = list(self._filters_applier.apply_filters(actual_variants, filters))

        self.assertTrue(len(actual_variants) == len(expected_variants))
        for actual, expected in zip(actual_variants, expected_variants):
            self.assertDictEqual(actual, expected)


def as_time_filter(filters):
    return FiltersSchema().load({'time': filters})

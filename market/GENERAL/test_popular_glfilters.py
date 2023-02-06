#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import FiltersPopularity, HyperCategory, HyperCategoryType


class T(TestCase):
    @classmethod
    def prepare_popular_glfilters(cls):
        cls.index.hypertree += [HyperCategory(hid=5, name='phones', output_type=HyperCategoryType.GURU)]
        cls.index.filters_popularity += [
            FiltersPopularity(hid=5, key=7893318, value=3, popularity=0),
            FiltersPopularity(hid=5, key=7893318, value=4, popularity=1),
            FiltersPopularity(hid=5, key=7893318, value=5, popularity=2),
        ]

        cls.index.filters_popularity += [
            FiltersPopularity(hid=5, key=7893319, value=i, popularity=i) for i in range(500)
        ]

    def test_popular_glfilters(self):
        response = self.report.request_json('place=popular_glfilters&hid=5&filters-key=7893318')
        self.assertFragmentIn(
            response, {"search": {"results": [3, 4, 5]}}, allow_different_len=False, preserve_order=True
        )

    def test_numdoc(self):
        response = self.report.request_json('place=popular_glfilters&hid=5&filters-key=7893318&numdoc=2')
        self.assertFragmentIn(response, {"search": {"results": [3, 4]}}, allow_different_len=False, preserve_order=True)

        response = self.report.request_json('place=popular_glfilters&hid=5&filters-key=7893318&numdoc=10')
        self.assertFragmentIn(
            response, {"search": {"results": [3, 4, 5]}}, allow_different_len=False, preserve_order=True
        )

        response = self.report.request_json('place=popular_glfilters&hid=5&filters-key=7893319')
        self.assertFragmentIn(
            response, {"search": {"results": [i for i in range(20)]}}, allow_different_len=False, preserve_order=True
        )


if __name__ == '__main__':
    main()

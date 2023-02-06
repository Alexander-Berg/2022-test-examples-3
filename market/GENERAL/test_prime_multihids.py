#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import HyperCategory, Offer


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.hypertree += [HyperCategory(hid=1), HyperCategory(hid=2)]

        cls.index.offers += [
            Offer(title='offer1', hid=1),
            Offer(title='offer1', hid=2),
        ]

    def test_prime_multihids(self):
        response = self.report.request_json('place=prime&hid=1')
        self.assertFragmentIn(response, {"total": 1})
        response = self.report.request_json('place=prime&hid=2')
        self.assertFragmentIn(response, {"total": 1})
        response = self.report.request_json('place=prime&hid=1,2')
        self.assertFragmentIn(response, {"total": 2})


if __name__ == '__main__':
    main()

#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import Offer


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.report_subrole = 'goods'
        cls.index.offers += [
            Offer(title="used good", is_used_good=True),
            Offer(title="not used good"),
        ]

    def test_filter_used_goods_only(self):
        for place in ['prime', 'parallel']:
            response = self.report.request_json(
                'place={}&text=good&rearr-factors=goods_filter_used_goods=1'.format(place)
            )

            self.assertFragmentIn(response, {"titles": {"raw": "used good"}})
            self.assertFragmentNotIn(response, {"titles": {"raw": "not used good"}})

    def test_filter_without_used_goods_forced(self):
        for place in ['prime', 'parallel']:
            response = self.report.request_json(
                'place={}&text=good&rearr-factors=goods_enable_used_goods_filtering=1'.format(place)
            )

            self.assertFragmentIn(response, {"titles": {"raw": "not used good"}})
            self.assertFragmentNotIn(response, {"titles": {"raw": "used good"}})

    def test_filter_without_used_goods_default(self):
        for place in ['prime', 'parallel']:
            response = self.report.request_json('place={}&text=good'.format(place))

            self.assertFragmentIn(response, {"titles": {"raw": "not used good"}})
            self.assertFragmentNotIn(response, {"titles": {"raw": "used good"}})

    def test_empty_request(self):
        response = self.report.request_json('place=prime&text=', strict=False)
        self.assertFragmentIn(response, {"error": {'code': 'EMPTY_REQUEST', 'message': "Request is empty"}})

        response = self.report.request_json('place=prime&text=&rearr-factors=goods_filter_used_goods=1', strict=False)
        self.assertFragmentIn(response, {"error": {'code': 'EMPTY_REQUEST', 'message': "Request is empty"}})


if __name__ == '__main__':
    main()

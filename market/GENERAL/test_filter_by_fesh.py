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
            Offer(title="Goods from odd shop id", fesh=1),
            Offer(title="Goods from even shop id", fesh=2),
            Offer(title="Goods from odd shop id", fesh=3),
            Offer(title="Goods from even shop id", fesh=4),
            Offer(title="Goods from odd shop id", fesh=5),
            Offer(title="Goods from even shop id", fesh=6),
        ]

    def test_filter_fesh(self):
        for place in ['prime', 'parallel']:
            response = self.report.request_json('place={}&text=Goods&fesh=1,3,5'.format(place))

            self.assertFragmentIn(response, {"titles": {"raw": "Goods from odd shop id"}})
            self.assertFragmentNotIn(response, {"titles": {"raw": "Goods from even shop id"}})

    def test_filter_without_fesh_default(self):
        for place in ['prime', 'parallel']:
            response = self.report.request_json('place={}&text=Goods'.format(place))

            self.assertFragmentIn(response, {"titles": {"raw": "Goods from odd shop id"}})
            self.assertFragmentIn(response, {"titles": {"raw": "Goods from even shop id"}})


if __name__ == '__main__':
    main()

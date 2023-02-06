#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import Model, Offer, Shop
from core.testcase import TestCase, main
from core.matcher import NoKey
import datetime
import calendar


class T(TestCase):
    past_sale_date = (datetime.datetime.now() - datetime.timedelta(days=10)).date()
    future_sale_date = (datetime.datetime.now() + datetime.timedelta(days=62)).date()

    @classmethod
    def prepare_sale_begin_date(cls):
        timestamp = calendar.timegm(cls.past_sale_date.timetuple())
        future_timestamp = calendar.timegm(cls.future_sale_date.timetuple())
        cls.index.models += [
            Model(hyperid=1, title='anvil no. 1', hid=1, sale_begin_ts=timestamp),
            Model(hyperid=2, title='furnace no. 2', hid=2),
            Model(hyperid=3, title='new anvil no. 2', hid=3, sale_begin_ts=future_timestamp),
        ]

        cls.index.shops += [
            Shop(fesh=1, priority_region=213, regions=[225]),
        ]

        cls.index.offers += [
            Offer(title='anvil offer', fesh=1, hyperid=1, model_sale_begin_ts=timestamp),
            Offer(title='new anvil offer', fesh=1, hyperid=2),
        ]

    def test_sale_begin_date(self):
        """Check that 'saleBeginDate' field appears on model with 'sale_begin_ts' property"""
        response = self.report.request_json('place=prime&text=anvil')
        self.assertFragmentIn(
            response,
            [
                {
                    'entity': 'product',
                    'id': 1,
                    'saleBeginDate': T.past_sale_date.isoformat(),
                },
                {
                    'entity': 'product',
                    'id': 3,
                    'saleBeginDate': T.future_sale_date.isoformat(),
                },
            ],
        )

        # no attribute - no saleBeginDate
        response = self.report.request_json('place=prime&text=furnace')
        self.assertFragmentNotIn(
            response,
            [
                {
                    'entity': 'product',
                    'id': 1,
                    'saleBeginDate': NoKey('saleBeginDate'),
                },
            ],
        )

        # check it also in modelinfo place
        response = self.report.request_json('place=modelinfo&hyperid=1&rids=213')
        self.assertFragmentIn(
            response,
            [
                {
                    'entity': 'product',
                    'id': 1,
                    'saleBeginDate': T.past_sale_date.isoformat(),
                },
            ],
        )


if __name__ == '__main__':
    main()

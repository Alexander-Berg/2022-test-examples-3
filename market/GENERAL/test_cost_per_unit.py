#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import Offer, Shop


class T(TestCase):
    @classmethod
    def prepare_model_quantity(cls):
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']

        cls.index.shops += [
            Shop(fesh=2, priority_region=213, regions=[225]),
        ]

        cls.index.offers += [
            Offer(
                title='offer 1', fesh=2, hyperid=10, hid=20, price=100, model_quantity_value=3, model_quantity_unit='шт'
            ),
        ]

    def test_model_quantity(self):
        response = self.report.request_json('place=prime&hid=20&rids=213')
        self.assertFragmentIn(
            response,
            [
                {
                    'entity': 'offer',
                    'pack': {
                        'partPrice': {'currency': 'RUR', 'value': '33.33'},
                        'partSize': '1',
                        'partUnit': 'шт',
                        'packSize': '3',
                    },
                },
            ],
        )


if __name__ == '__main__':
    main()

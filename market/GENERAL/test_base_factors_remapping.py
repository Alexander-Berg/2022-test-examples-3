#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import EntityCtr, HyperCategory, Model, Offer
from core.testcase import TestCase, main
from core.matcher import Round


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.relevance_tread_count_percentage = 50
        cls.settings.default_search_experiment_flags += ['base_factors_remapping=1']

        cls.index.hypertree += [
            HyperCategory(hid=100, tovalid=100100),
        ]

        for i in range(0, 55):
            cls.index.models += [Model(title='free', hyperid=520 + i, hid=100, vendor_id=4501)]

        cls.index.offers += [
            Offer(title='free', hid=100, ts=100520),
        ]

        cls.index.ctr.free.model += [EntityCtr(520, 15, 300)]
        cls.index.ctr.free.ts += [EntityCtr(100520, 5, 500)]
        cls.index.ctr.free.ip_geo += [EntityCtr(0, 10, 100)]
        cls.index.ctr.free.geo_settings += [EntityCtr(0, 1, 10)]
        cls.index.ctr.free.categ += [EntityCtr(100100, 34, 600)]
        cls.index.ctr.free.vendor += [EntityCtr(4501, 80, 350)]
        cls.index.ctr.orders.blue_category += [EntityCtr(100, 204, 458)]

    def test_free_ctr(self):
        response = self.report.request_json('place=prime&text=free%20model&hid=100&pp=7&debug=1')

        self.assertFragmentIn(
            response,
            {
                "entity": "product",
                "titles": {"raw": "free"},
                "debug": {
                    "factors": {
                        "IS_MODEL": "1",
                        "MODEL_SHOWS": "600",
                        "MODEL_CLICKS": "34",
                        "MODEL_CTR": "0.0566666685",
                        "MODEL_CTR_ADJ": "0.05684210733",
                        "OFFER_SHOWS": "600",
                        "OFFER_CLICKS": "34",
                        "OFFER_CTR": "0.0566666685",
                        "OFFER_CTR_ADJ": "0.05667638406",
                        "CATEG_CLICKS": "34",
                        "CATEG_SHOWS": "600",
                        "CATEG_CTR": Round(34.0 / 600.0),
                        "VENDOR_CLICKS": "80",
                        "VENDOR_SHOWS": "350",
                        "VENDOR_CTR": Round(80.0 / 350.0),
                        "GEO_IP_SHOWS": "100",
                        "GEO_IP_CLICKS": "10",
                        "GEO_IP_CTR": "0.1000000015",
                        "GEO_IP_CTR_ADJ": "0.1000000015",
                        "GEO_SETTINGS_SHOWS": "10",
                        "GEO_SETTINGS_CLICKS": "1",
                        "GEO_SETTINGS_CTR": "0.1000000015",
                        "GEO_SETTINGS_CTR_ADJ": "0.1000000015",
                        'BLUE_CATEGORY_ORDERS_DIV_MODEL_SHOWS': Round(204.0 / 600),
                        'BLUE_CATEGORY_ORDERS_DIV_MODEL_CLICKS': Round(204.0 / 34),
                        'BM15_P_BODY': '0.4993757606',
                    }
                },
            },
        )


if __name__ == '__main__':
    main()

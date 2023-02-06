#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import EntityCtr, HyperCategory, Model, Offer
from core.testcase import TestCase, main
from core.matcher import Round, Absent
from core.types.hypercategory import (
    EATS_CATEG_ID,
    CategoryStreamRecord,
    Stream,
)

FOOD_CATEGORY = EATS_CATEG_ID


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.market_access_settings.enabled = True
        cls.settings.market_access_settings.download_catstreams = True
        cls.index.category_streams += [
            CategoryStreamRecord(FOOD_CATEGORY, Stream.FMCG.value),
        ]
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']

        cls.index.hypertree += [
            HyperCategory(hid=100, tovalid=100100),
        ]

        cls.index.models += [Model(title='free', hyperid=520, hid=100, vendor_id=4501)]

        cls.index.offers += [
            Offer(title='free', hid=100, ts=100520),
        ]

        cls.index.ctr.free.model += [EntityCtr(520, 15, 300)]
        cls.index.ctr.free.ts += [EntityCtr(100520, 5, 500)]
        cls.index.ctr.free.ip_geo += [EntityCtr(0, 10, 100)]
        cls.index.ctr.free.geo_settings += [EntityCtr(0, 1, 10)]
        cls.index.ctr.free.categ += [EntityCtr(100100, 34, 600)]
        cls.index.ctr.free.vendor += [EntityCtr(4501, 80, 350)]

        cls.index.ctr.orders.blue_model += [EntityCtr(520, 3, 12)]
        cls.index.ctr.orders.blue_category += [EntityCtr(100, 204, 458)]
        cls.index.ctr.orders.white_model += [EntityCtr(520, 45, 348)]

    def test_free_ctr(self):
        response = self.report.request_json('place=prime&text=free&hid=100&pp=7&debug=1')

        self.assertFragmentIn(
            response,
            {
                "entity": "product",
                "titles": {"raw": "free"},
                "debug": {
                    "factors": {
                        "MODEL_SHOWS": "300",
                        "MODEL_CLICKS": "15",
                        "MODEL_CTR": "0.05000000075",
                        "MODEL_CTR_ADJ": "0.05099999905",
                        "OFFER_SHOWS": "300",
                        "OFFER_CLICKS": "15",
                        "OFFER_CTR": "0.05000000075",
                        "OFFER_CTR_ADJ": "0.05011560395",
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
                        'BLUE_MODEL_ORDERS': "3",
                        'BLUE_MODEL_VIEWS': "12",
                        'BLUE_MODEL_VIEW_TO_ORDER_CONVERSION': Round(3.0 / 12.0),
                        'BLUE_CATEGORY_ORDERS_DIV_MODEL_SHOWS': Round(204.0 / 300),
                        'BLUE_CATEGORY_ORDERS_DIV_MODEL_CLICKS': Round(204.0 / 15),
                        'BLUE_MODEL_ORDERS_IN_CATEGORY_ORDERS': Round(3.0 / 204.0),
                        'WHITE_MODEL_VIEWS': "348",
                        'WHITE_MODEL_ORDERS': "3",
                        'WHITE_MODEL_VIEW_TO_ORDER_CONVERSION': Round(3.0 / 348.0),
                    }
                },
            },
        )

        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "titles": {"raw": "free"},
                "debug": {
                    "factors": {
                        "OFFER_SHOWS": "500",
                        "OFFER_CLICKS": "5",
                        "OFFER_CTR": "0.009999999776",
                        "OFFER_CTR_ADJ": "0.01307958458",
                    }
                },
            },
        )

    @classmethod
    def prepare_eats(cls):

        cls.index.hypertree += [
            HyperCategory(hid=FOOD_CATEGORY, tovalid=100101),
        ]

        cls.index.models += [
            Model(title='dsbs_model', hyperid=601, hid=FOOD_CATEGORY),
            Model(title='eats_model', hyperid=602, hid=FOOD_CATEGORY),
            Model(title='lavka_model', hyperid=603, hid=FOOD_CATEGORY),
        ]

        cls.index.offers += [
            Offer(title='dsbs_offer', hid=FOOD_CATEGORY, hyperid=601),
            Offer(title='eats_offer', hid=FOOD_CATEGORY, hyperid=602, is_eda=True),
            Offer(title='lavka_offer', hid=FOOD_CATEGORY, hyperid=603, is_lavka=True),
        ]

        for hyperid in [601, 602, 603]:
            cls.index.ctr.free.model += [EntityCtr(hyperid, 15, 300)]

            cls.index.ctr.orders.blue_model += [EntityCtr(hyperid, 3, 12)]
            cls.index.ctr.orders.blue_category += [EntityCtr(FOOD_CATEGORY, 204, 458)]
            cls.index.ctr.orders.white_model += [EntityCtr(hyperid, 45, 348)]

    def test_eats_ctr(self):
        '''
        Для клиентов еды и лавки факторы заказов не учитываются для оферов еды и лавки
        '''

        def check_offer(response, title, expect_order_factors):
            self.assertFragmentIn(
                response,
                {
                    "entity": "offer",
                    "titles": {"raw": title},
                    "debug": {
                        "factors": {
                            "BLUE_MODEL_ORDERS": "3" if expect_order_factors else Absent(),
                            "BLUE_CATEGORY_ORDERS_DIV_MODEL_CLICKS": Round(204.0 / 15)
                            if expect_order_factors
                            else Absent(),
                            "BLUE_CATEGORY_ORDERS_DIV_MODEL_SHOWS": Round(204.0 / 300)
                            if expect_order_factors
                            else Absent(),
                            "BLUE_MODEL_ORDERS_IN_CATEGORY_ORDERS": Round(3.0 / 204.0)
                            if expect_order_factors
                            else Absent(),
                            "WHITE_MODEL_ORDERS": "3" if expect_order_factors else Absent(),
                        }
                    },
                },
            )

        def check(flags, expect_order_factors):
            response = self.report.request_json(
                'place=prime&hid={}&pp=7&debug=1&allow-collapsing=0&enable-foodtech-offers=eda_retail,eda_restaurants,lavka'.format(
                    FOOD_CATEGORY
                )
                + flags
            )

            check_offer(response, "dsbs_offer", True)
            check_offer(response, "eats_offer", expect_order_factors)
            check_offer(response, "lavka_offer", expect_order_factors)

        check('', True)
        check('&client=eats', False)
        check('&client=lavka', False)
        check('&client=some_other_client', True)


if __name__ == '__main__':
    main()

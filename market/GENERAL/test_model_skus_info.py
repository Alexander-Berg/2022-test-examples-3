#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import BlueOffer, MarketSku, Model
from core.testcase import TestCase, main


def create_right_response(model_id, sku_ids, titles):
    result = {}
    result['model_id'] = model_id
    result['skus'] = []
    for sku_id, title in zip(sku_ids, titles):
        result['skus'].append(
            {
                'id': str(sku_id),
                'titles': {'raw': title},
                'entity': 'sku',
            }
        )

    return result


class T(TestCase):
    @classmethod
    def prepare(cls):

        cls.index.models += [Model(hyperid=1, hid=1, title='Sword'), Model(hyperid=2, hid=2, title='Axe')]

        cls.index.mskus += [
            MarketSku(title='Stone Sword', blue_offers=[BlueOffer(), BlueOffer()], sku=11, hyperid=1),
            MarketSku(title='Diamond Axe', blue_offers=[BlueOffer()], sku=22, hyperid=2),
            MarketSku(title='Iron Axe', sku=23, hyperid=2),
            MarketSku(title='Wooden Shovel', sku=31, hyperid=314, auto_creating_model=False),  # sku with virtual model
            MarketSku(title='Gold Shovel', sku=32, hyperid=314, auto_creating_model=False),  # sku with virtual model
        ]

        cls.settings.dont_put_sku_to_blue_shard = True

    def test_model_skus_info(self):
        response = self.report.request_json('place=model_skus_info&modelid=1')
        self.assertFragmentIn(response, create_right_response(1, [11], ['Stone Sword']))

        response = self.report.request_json('place=model_skus_info&modelid=2')
        self.assertFragmentIn(response, create_right_response(2, [22, 23], ['Diamond Axe', 'Iron Axe']))

        response = self.report.request_json('place=model_skus_info&modelid=314')
        self.assertFragmentIn(
            response, create_right_response(314, [31, 32], ['Wooden Shovel', 'Gold Shovel'])
        )  # skus with virtual model

        response = self.report.request_json('place=model_skus_info&modelid=1,2')
        self.assertFragmentIn(
            response,
            [
                create_right_response(1, [11], ['Stone Sword']),
                create_right_response(2, [22, 23], ['Diamond Axe', 'Iron Axe']),
            ],
            allow_different_len=False,
        )

    def test_get_without_modelid(self):
        response = self.report.request_json('place=model_skus_info')
        self.error_log.expect(code=3043)
        self.assertFragmentIn(
            response, {"error": {"code": "INVALID_USER_CGI", "message": "Model ID should be specified"}}
        )

    def test_bad_modelid(self):
        response = self.report.request_json('place=model_skus_info&modelid=wrong')
        self.assertFragmentIn(response, [])  # empty answer for invalid modelid

        response = self.report.request_json('place=model_skus_info&modelid=125')
        self.assertFragmentIn(response, [])  # empty answer for uncreated model

    def test_not_auto_creating_model(self):
        # model 314 - uncreated, however skus 31, 32 are linked with it
        response = self.report.request_json('place=modelinfo&hyperid=314&rids=0')
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "adult": False,
                    "isDeliveryIncluded": False,
                    "isPickupIncluded": False,
                    "maxDiscountPercent": 0,
                    "results": [],
                    "salesDetected": False,
                    "total": 0,
                    "totalModels": 0,
                    "totalOffers": 0,
                    "totalOffersBeforeFilters": 0,
                    "totalPassedAllGlFilters": 0,
                }
            },
        )


if __name__ == '__main__':
    main()

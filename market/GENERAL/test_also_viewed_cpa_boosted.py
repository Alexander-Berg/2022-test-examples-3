#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import Const, Model, Offer, Shop, YamarecPlace, YamarecSettingPartition
from core.testcase import TestCase, main


class T(TestCase):
    """
    Набор тестов для place=also_viewed
    """

    @classmethod
    def prepare(cls):
        """
        Модели, офферы и конфигурация для выдачи place=also_viewed
        """
        cls.index.models += [
            Model(hyperid=101),
            Model(hyperid=102),
            Model(hyperid=103),
            Model(hyperid=104),
        ]
        cls.index.shops += [
            Shop(fesh=1001, home_region=Const.ROOT_COUNTRY, cpa=Shop.CPA_REAL),
        ]

        cls.index.offers += [
            Offer(hyperid=101, cpa=Offer.CPA_REAL, fesh=1001, price=50, waremd5="AccDelPrice1_________g"),
            Offer(hyperid=102, cpa=Offer.CPA_REAL, fesh=1001, price=20, waremd5="AccDelPrice2_________g"),
            Offer(hyperid=103, cpa=Offer.CPA_NO, fesh=1001, price=30, waremd5="AccDelPrice3_________g"),
            Offer(hyperid=104, cpa=Offer.CPA_REAL, fesh=1001, price=10, waremd5="AccDelPrice4_________g"),
        ]

        cls.index.yamarec_places += [
            YamarecPlace(
                name=YamarecPlace.Name.ALSO_VIEWED_PRODUCTS,
                kind=YamarecPlace.Type.SETTING,
                split_rule=YamarecPlace.SplitRule.ABT,
                partitions=[
                    # partitions with data
                    YamarecSettingPartition(
                        params={'version': 'SIBLINGS1_CPA_BOOSTED'}, splits=[{'split': 'siblings'}]
                    ),
                ],
            ),
        ]
        cls.recommender.on_request_accessory_models(
            model_id=101, item_count=1000, version='SIBLINGS1_CPA_BOOSTED'
        ).respond({'models': ['102', '103', '104']})

    def test_also_viewed_cpa_not_boosted(self):

        main_model = 101
        query = "place=also_viewed&rearr-factors=split=siblings&hyperid={id}".format(id=main_model)

        response = self.report.request_json(query)

        self.assertFragmentIn(
            response,
            {
                "search": {
                    'total': 3,
                    'results': [
                        {
                            'entity': 'product',
                            'id': 102,
                        },
                        {
                            'entity': 'product',
                            'id': 103,
                        },
                        {
                            'entity': 'product',
                            'id': 104,
                        },
                    ],
                }
            },
            preserve_order=True,
        )

    def test_also_viewed_cpa_boosted(self):

        main_model = 101
        query = "place=also_viewed&hyperid={id}&rearr-factors=split=siblings;market_boost_cpa_offered_models=1;market_send_cpa_snippets_count=1".format(
            id=main_model
        )

        response = self.report.request_json(query)
        self.assertFragmentIn(
            response,
            {
                "search": {
                    'total': 3,
                    'recomParams': {'countOfCpaSnippets': 2},
                    'results': [
                        {
                            'entity': 'product',
                            'id': 102,
                        },
                        {
                            'entity': 'product',
                            'id': 104,
                        },
                        {
                            'entity': 'product',
                            'id': 103,
                        },
                    ],
                }
            },
            preserve_order=True,
        )

    def test_also_viewed_cpa_boosted_no_info(self):

        main_model = 101
        query = "place=also_viewed&hyperid={id}&rearr-factors=split=siblings;market_boost_cpa_offered_models=1;".format(
            id=main_model
        )

        response = self.report.request_json(query)
        self.assertFragmentIn(
            response,
            {
                "search": {
                    'total': 3,
                    'results': [
                        {
                            'entity': 'product',
                            'id': 102,
                        },
                        {
                            'entity': 'product',
                            'id': 104,
                        },
                        {
                            'entity': 'product',
                            'id': 103,
                        },
                    ],
                }
            },
            preserve_order=True,
        )


if __name__ == '__main__':
    main()

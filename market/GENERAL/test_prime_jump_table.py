#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa


from core.types import GLParam, GLType, Model, Opinion
from core.types.model import UngroupedModel
from core.testcase import (
    TestCase,
    main,
)
from core.types.sku import (
    MarketSku,
    BlueOffer,
)


class T(TestCase):
    sku1_offer1 = BlueOffer(
        price=5,
        price_old=8,
        feedid=3,
        offerid='blue.offer.1.1',
        waremd5='Sku1Price5-IiLVm1Goleg',
        randx=12,
    )
    sku1_offer2 = BlueOffer(
        price=7,
        price_old=8,
        feedid=3,
        offerid='blue.offer.1.2',
        waremd5='Sku1Price7-IiLVm1Goleg',
        randx=11,
    )
    sku2_offer1 = BlueOffer(
        price=6,
        price_old=8,
        feedid=3,
        offerid='blue.offer.2.1',
        waremd5='Sku2Price6-IiLVm1Goleg',
    )
    sku3_offer1 = BlueOffer(
        price=6,
        price_old=8,
        feedid=3,
        offerid='blue.offer.3.1',
        waremd5='Sku3Price6-IiLVm1Goleg',
    )

    @classmethod
    def prepare(cls):
        cls.index.gltypes += [
            GLType(param_id=101, hid=1, cluster_filter=False, gltype=GLType.ENUM, values=[1, 2]),
            GLType(param_id=102, hid=1, cluster_filter=False, gltype=GLType.NUMERIC),
            GLType(param_id=103, hid=1, cluster_filter=False, gltype=GLType.BOOL, hasboolno=False),
            GLType(param_id=104, hid=1, cluster_filter=False, gltype=GLType.BOOL, hasboolno=True),
            GLType(param_id=201, hid=1, cluster_filter=True, model_filter_index=3, gltype=GLType.ENUM),
            GLType(param_id=202, hid=1, cluster_filter=True, model_filter_index=2, gltype=GLType.ENUM),
            GLType(param_id=205, hid=1, cluster_filter=True, model_filter_index=4, gltype=GLType.NUMERIC),
            GLType(param_id=206, hid=2, cluster_filter=True, model_filter_index=5, gltype=GLType.NUMERIC),
            GLType(
                param_id=207,
                hid=2,
                cluster_filter=True,
                model_filter_index=6,
                gltype=GLType.NUMERIC,
                unit_name='Parrots',
            ),
        ]

        cls.index.models += [
            Model(
                hyperid=1,
                hid=1,
                glparams=[
                    GLParam(param_id=101, value=1),
                    GLParam(param_id=102, value=1),
                    GLParam(param_id=103, value=1),
                    GLParam(param_id=104, value=1),
                ],
                opinion=Opinion(reviews=100500, total_count=3, rating=4.5, rating_count=17),
            ),
            Model(
                hyperid=2,
                hid=2,
                ungrouped_blue=[
                    UngroupedModel(
                        group_id=1,
                        key='1_1',
                    ),
                    UngroupedModel(
                        group_id=2,
                        key='1_2',
                    ),
                ],
                opinion=Opinion(reviews=100500, total_count=3, rating=4.5, rating_count=17),
            ),
        ]

        cls.index.mskus += [
            MarketSku(
                hyperid=1,
                sku=1,
                blue_offers=[T.sku1_offer1, T.sku1_offer2],
                glparams=[
                    GLParam(param_id=201, value=1),
                    GLParam(param_id=202, value=1),
                    GLParam(param_id=205, value=1),
                ],
                randx=1,
            ),
            MarketSku(
                hyperid=1,
                sku=2,
                blue_offers=[T.sku2_offer1],
                glparams=[
                    GLParam(param_id=201, value=2),
                    GLParam(param_id=202, value=1),
                    GLParam(param_id=205, value=1),
                ],
                randx=2,
            ),
            MarketSku(
                hyperid=1,
                sku=3,
                blue_offers=[T.sku3_offer1],
                glparams=[
                    GLParam(param_id=201, value=3),
                    GLParam(param_id=202, value=2),
                    GLParam(param_id=205, value=3),
                ],
                randx=3,
            ),
            MarketSku(
                hyperid=2,
                sku=4,
                blue_offers=[
                    BlueOffer(price=5, feedid=2, ts=3, waremd5='Ws3Jyl2Zrmav3-HuoOOyaw'),
                ],
                glparams=[
                    GLParam(param_id=206, value=1),
                    GLParam(param_id=207, value=1),
                ],
                ungrouped_model_blue=1,
            ),
            MarketSku(
                hyperid=2,
                sku=5,
                blue_offers=[
                    BlueOffer(price=6, feedid=2, ts=3, waremd5='iWX3ZjLXZy59PPKch-yqDA'),
                ],
                glparams=[
                    GLParam(param_id=206, value=1),
                    GLParam(param_id=207, value=2),
                ],
                ungrouped_model_blue=1,
            ),
            MarketSku(
                hyperid=2,
                sku=6,
                glparams=[
                    GLParam(param_id=206, value=3),
                    GLParam(param_id=207, value=2),
                ],
                ungrouped_model_blue=2,
            ),
            MarketSku(
                hyperid=2,
                sku=7,
                blue_offers=[
                    BlueOffer(price=10, feedid=2, ts=9, waremd5='1aKPgYNLbfK2220-XiY1xw'),
                ],
                glparams=[
                    GLParam(param_id=206, value=4),
                    GLParam(param_id=207, value=2),
                ],
                ungrouped_model_blue=2,
            ),
        ]

    def test_prime_jump_table(self):
        response = self.report.request_json('place=prime&hid=1&use-default-offers=1')

        self.assertFragmentIn(
            response,
            {
                "jumpTable": [
                    {
                        "id": "201",
                        "type": "enum",
                        "valuesCount": 3,
                        "values": [
                            {"value": "VALUE-1", "marketSku": "1", "checked": True},
                            {
                                "value": "VALUE-2",
                                "marketSku": "2",
                            },
                            {
                                "value": "VALUE-3",
                                "marketSku": "3",
                            },
                        ],
                    },
                    {
                        "id": "202",
                        "valuesCount": 2,
                        "values": [
                            {
                                "checked": True,
                                "value": "VALUE-1",
                                "marketSku": "1",
                            },
                            {
                                "value": "VALUE-2",
                                "marketSku": "3",
                            },
                        ],
                    },
                    {
                        "id": "205",
                        "values": [
                            {
                                "checked": True,
                                "value": "1",
                                "marketSku": "1",
                            },
                            {
                                "value": "3",
                                "marketSku": "3",
                            },
                        ],
                    },
                ],
            },
            preserve_order=False,
        )

    # два вхождения расхлопнутой модели, каждая с jump table
    def test_prime_jump_table_ungrouping(self):
        response = self.report.request_json(
            'place=prime&hid=2&allow-ungrouping=1&allow-collapsing=1&use-default-offers=1'
        )

        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "type": "model",
                        "id": 2,
                        "jumpTable": [
                            {
                                "id": "206",
                                "values": [
                                    {
                                        "checked": True,
                                        "value": "1",
                                        "marketSku": "4",
                                    },
                                    {
                                        "value": "4",
                                        "marketSku": "7",
                                    },
                                ],
                            },
                            {
                                "id": "207",
                                "values": [
                                    {
                                        "checked": True,
                                        "value": "1",
                                        "marketSku": "4",
                                    },
                                    {
                                        "value": "2",
                                        "marketSku": "5",
                                    },
                                ],
                                "unit": "Parrots",
                            },
                        ],
                    },
                    {
                        "type": "model",
                        "id": 2,
                        "jumpTable": [
                            {
                                "id": "206",
                                "values": [
                                    {
                                        "value": "1",
                                        "marketSku": "5",
                                    },
                                    {
                                        "checked": True,
                                        "value": "4",
                                        "marketSku": "7",
                                    },
                                ],
                            },
                            {
                                "id": "207",
                                "values": [
                                    {
                                        "value": "1",
                                        "marketSku": "4",
                                    },
                                    {
                                        "checked": True,
                                        "value": "2",
                                        "marketSku": "7",
                                    },
                                ],
                                "unit": "Parrots",
                            },
                        ],
                    },
                ]
            },
            preserve_order=False,
            allow_different_len=False,
        )


if __name__ == '__main__':
    main()

#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import BlueOffer, Const, GLParam, GLType, MarketSku, Model, Offer, Region, Shop, VirtualModel


class T(TestCase):
    @classmethod
    def prepare(cls):

        cls.index.regiontree += [
            Region(rid=213, name='Москва'),
        ]

        cls.index.gltypes += [
            GLType(param_id=Const.PSKU2_GL_PARAM_ID, hid=14, gltype=GLType.BOOL),
            GLType(param_id=Const.PSKU2_LITE_GL_PARAM_ID, hid=15, gltype=GLType.BOOL),
        ]

        cls.index.models += [
            Model(hyperid=1001, title='pskuModel', is_pmodel=True, hid=100, vendor_id=1),
            Model(hyperid=1002, title='mskuModel', hid=100),
        ]

        cls.index.virtual_models += [
            VirtualModel(virtual_model_id=100500),
        ]

        cls.index.mskus += [
            # test_sku_by_partner
            MarketSku(
                hyperid=1001,
                forbidden_market_mask=Offer.IS_PSKU,
                sku=1,
                blue_offers=[
                    BlueOffer(
                        price=100, forbidden_market_mask=Offer.IS_PSKU, offerid='1', waremd5='__wareId_psku_1001___w'
                    )
                ],
            ),
            # test_sku_by_partner 2
            MarketSku(
                hyperid=1001,
                forbidden_market_mask=Offer.IS_PSKU,
                sku=2,
                blue_offers=[
                    BlueOffer(
                        price=100, forbidden_market_mask=Offer.IS_PSKU, offerid='2', waremd5='__wareId_psku2_1001__w'
                    )
                ],
                glparams=[
                    GLParam(param_id=Const.PSKU2_GL_PARAM_ID, value=1)
                ],  # наличие этого параметра - признак PSKU 2.0
            ),
            # test_sku_by_partner 2 lite
            MarketSku(
                hyperid=1001,
                forbidden_market_mask=Offer.IS_PSKU,
                sku=3,
                blue_offers=[
                    BlueOffer(
                        price=100, forbidden_market_mask=Offer.IS_PSKU, offerid='3', waremd5='wareId_psku2lite_1001w'
                    )
                ],
                glparams=[
                    GLParam(param_id=Const.PSKU2_LITE_GL_PARAM_ID, value=1)
                ],  # наличие этого параметра - признак PSKU 2.0 lite
            ),
            # test_sku_by_market
            MarketSku(
                hyperid=1002,
                sku=4,
                blue_offers=[BlueOffer(price=100, offerid='4', feedid=2001, waremd5='__wareId_msku________w')],
            ),
        ]

        cls.index.shops += [
            Shop(
                fesh=42,
                priority_region=213,
                regions=[213],
                client_id=11,
                cpa=Shop.CPA_REAL,
            )
        ]

        cls.index.offers += [
            Offer(
                title='offer with virtual model',
                virtual_model_id=100500,
                fesh=42,
                cpa=Offer.CPA_REAL,
                forbidden_market_mask=Offer.IS_PSKU,
                waremd5="_wareId_msku_virtual_w",
            ),
        ]

    def test_market_offer(self):
        # Проверка marketSkuCreator==market в place=offerinfo

        response = self.report.request_json(
            'place=offerinfo&rids=213&show-urls=direct&offerid=__wareId_msku________w&regset=1'
        )
        self.assertFragmentIn(
            response,
            [
                {
                    "marketSkuCreator": "market",
                    'wareId': '__wareId_msku________w',
                }
            ],
        )

    def test_partner_offer(self):
        # Проверка marketSkuCreator==partner в place=offerinfo

        response = self.report.request_json(
            'place=offerinfo&rids=213&show-urls=direct&offerid=__wareId_psku_1001___w&regset=1'
        )
        self.assertFragmentIn(
            response,
            [
                {
                    "marketSkuCreator": "partner",
                    'wareId': '__wareId_psku_1001___w',
                }
            ],
        )

    def test_partner2_offer(self):
        # Проверка marketSkuCreator==partner2 в place=offerinfo

        response = self.report.request_json(
            'place=offerinfo&rids=213&show-urls=direct&offerid=__wareId_psku2_1001__w&regset=1'
        )
        self.assertFragmentIn(
            response,
            [
                {
                    "marketSkuCreator": "partner2",
                    'wareId': '__wareId_psku2_1001__w',
                }
            ],
        )

    def test_partner2lite_offer(self):
        # Проверка marketSkuCreator==partner2lite в place=offerinfo

        response = self.report.request_json(
            'place=offerinfo&rids=213&show-urls=direct&offerid=wareId_psku2lite_1001w&regset=1'
        )
        self.assertFragmentIn(
            response,
            [
                {
                    "marketSkuCreator": "partner2lite",
                    'wareId': 'wareId_psku2lite_1001w',
                }
            ],
        )

    def test_virtual_offer(self):
        # Проверка marketSkuCreator==virtual в place=offerinfo

        response = self.report.request_json(
            'place=offerinfo&rids=213&show-urls=direct&offerid=_wareId_msku_virtual_w&regset=1'
        )
        self.assertFragmentIn(
            response,
            [
                {
                    "marketSkuCreator": "virtual",
                    'wareId': '_wareId_msku_virtual_w',
                }
            ],
        )

    def test_sku_offers_partner(self):
        # Проверка marketSkuCreator==partner в place=sku_offers

        response = self.report.request_json("place=sku_offers&market-sku=1")
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "sku",
                        "id": "1",
                        "marketSkuCreator": "partner",
                        "offers": {
                            "items": [
                                {
                                    "marketSku": "1",
                                    "marketSkuCreator": "partner",
                                    "model": {"id": 1001},
                                    'wareId': '__wareId_psku_1001___w',
                                }
                            ]
                        },
                    },
                ]
            },
        )

    def test_sku_offers_partner2(self):
        # Проверка marketSkuCreator==partner2 в place=sku_offers

        response = self.report.request_json("place=sku_offers&market-sku=2")
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "sku",
                        "id": "2",
                        "marketSkuCreator": "partner2",
                        "offers": {
                            "items": [
                                {
                                    "marketSku": "2",
                                    "marketSkuCreator": "partner2",
                                    "model": {"id": 1001},
                                    "wareId": "__wareId_psku2_1001__w",
                                }
                            ]
                        },
                    },
                ]
            },
        )

    def test_sku_offers_partner2lite(self):
        # Проверка marketSkuCreator==partner2lite в place=sku_offers

        response = self.report.request_json("place=sku_offers&market-sku=3")
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "sku",
                        "id": "3",
                        "marketSkuCreator": "partner2lite",
                        "offers": {
                            "items": [
                                {
                                    "marketSku": "3",
                                    "marketSkuCreator": "partner2lite",
                                    "model": {"id": 1001},
                                    "wareId": "wareId_psku2lite_1001w",
                                }
                            ]
                        },
                    },
                ]
            },
        )

    def test_sku_offers_market(self):
        # Проверка marketSkuCreator==market в place=sku_offers

        response = self.report.request_json("place=sku_offers&market-sku=4")
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "sku",
                        "id": "4",
                        "marketSkuCreator": "market",
                        "offers": {
                            "items": [
                                {
                                    "marketSku": "4",
                                    "marketSkuCreator": "market",
                                    "model": {"id": 1002},
                                    "wareId": "__wareId_msku________w",
                                }
                            ]
                        },
                    },
                ]
            },
        )

    def test_sku_offers_virtual(self):
        # Проверка marketSkuCreator==virtual в place=sku_offers

        response = self.report.request_json(
            "place=sku_offers&market-sku=100500&rgb=blue&rearr-factors=market_white_cpa_on_blue=2;market_cards_everywhere_range=100400:100900"
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "sku",
                        "id": "100500",
                        "marketSkuCreator": "virtual",
                        "offers": {
                            "items": [
                                {
                                    "marketSku": "100500",
                                    "marketSkuCreator": "virtual",
                                    "model": {"id": 100500},
                                    "wareId": "_wareId_msku_virtual_w",
                                }
                            ]
                        },
                    },
                ]
            },
        )


if __name__ == '__main__':
    main()

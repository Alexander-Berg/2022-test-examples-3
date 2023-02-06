#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    HyperCategory,
    HyperCategoryType,
    Model,
    Offer,
    Picture,
    VirtualModel,
)
from core.testcase import TestCase, main
from core.bigb import BeruSkuOrderLastTimeCounter, SkuLastOrderEvent
from core.types.sku import MarketSku, BlueOffer
from core.matcher import Absent, Regex
from core.logs import ErrorCodes

import re


Model1 = 264711001
Model2 = 264711002
Model4 = 264711004
Model5 = 264711005
Model7 = 264711007

pic1 = Picture(width=100, height=100, group_id=1001, picture_id='pic1')
pic2 = Picture(width=100, height=100, group_id=1002, picture_id='pic2')
pic3 = Picture(width=100, height=100, group_id=1003, picture_id='pic3')
pic4 = Picture(width=100, height=100, group_id=1004, picture_id='pic4')
pic5 = Picture(width=100, height=100, group_id=1005, picture_id='pic5')
pic6 = Picture(width=100, height=100, group_id=1006, picture_id='pic6')


class _Offers(object):
    sku1_offer1 = BlueOffer(
        price=1,
        feedid=6,
        offerid='blue.offer.1.1',
        waremd5='Sku1Price5-IiLVm1Goleg',
        randx=2,
        cpa=Offer.CPA_REAL,
        picture=pic1,
    )
    sku2_offer1 = BlueOffer(
        price=5,
        feedid=4,
        offerid='blue.offer.2.1',
        waremd5='Sku2Price5-IiLVm1Goleg',
        randx=1,
        cpa=Offer.CPA_REAL,
        picture=pic2,
    )
    sku4_offer1 = BlueOffer(
        price=15,
        feedid=2,
        offerid='blue.offer.4.1',
        waremd5='Sku4Price6-IiLVm1Goleg',
        randx=5,
        cpa=Offer.CPA_REAL,
        picture=pic3,
    )
    sku4_offer2 = BlueOffer(
        price=14,
        feedid=9,
        offerid='blue.offer.4.2',
        waremd5='Sku4Price7-IiLVm1Goleg',
        randx=6,
        cpa=Offer.CPA_REAL,
        picture=pic4,
    )
    sku5_offer1 = BlueOffer(
        price=10,
        feedid=7,
        offerid='blue.offer.5.1',
        waremd5='Sku5Price6-IiLVm1Goleg',
        randx=7,
        cpa=Offer.CPA_REAL,
        picture=pic5,
    )
    sku7_offer1 = BlueOffer(
        price=4,
        feedid=10,
        offerid='blue.offer.7.1',
        waremd5='Sku7Price7-IiLVm1Goleg',
        randx=9,
        cpa=Offer.CPA_REAL,
        picture=pic6,
    )


class T(TestCase):
    @classmethod
    def prepare_wares(cls):
        cls.index.hypertree += [
            HyperCategory(
                hid=264711,
                children=[
                    HyperCategory(
                        hid=264712,
                        children=[
                            HyperCategory(hid=264714, output_type=HyperCategoryType.GURU),
                            HyperCategory(hid=264715, output_type=HyperCategoryType.GURU),
                        ],
                    ),
                    HyperCategory(
                        hid=264713,
                        children=[
                            HyperCategory(hid=264716, output_type=HyperCategoryType.GURU),
                            HyperCategory(hid=264717, output_type=HyperCategoryType.GURU),
                        ],
                    ),
                ],
            ),
        ]

        cls.index.models += [
            Model(hyperid=Model1, title='Яндекс.Поиск', hid=264711),
            Model(hyperid=Model2, title='Яндекс.Маркет', hid=264712),
            Model(hyperid=Model4, title='Яндекс.Такси', hid=264714),
            Model(hyperid=Model5, title='Яндекс.Еда', hid=264715),
            Model(hyperid=Model7, title='Яндекс.Таланты', hid=264717),
        ]

        counters = [
            BeruSkuOrderLastTimeCounter(
                sku_order_events=[
                    SkuLastOrderEvent(sku_id=264711001, timestamp=418419200),
                    SkuLastOrderEvent(sku_id=264711007, timestamp=488418200),
                    SkuLastOrderEvent(sku_id=264711002, timestamp=488419100),
                ]
            ),
        ]

        cls.bigb.on_request(yandexuid=26471001, client='merch-machine').respond(counters=counters)

        cls.index.mskus += [
            MarketSku(
                title="разработчик 4",
                hyperid=Model4,
                sku=264711004,
                waremd5='Sku4-wdDXWsIiLVm1goleg',
                blue_offers=[_Offers.sku4_offer1, _Offers.sku4_offer2],
                randx=1,
            ),
            MarketSku(
                title="разработчик 5",
                hyperid=Model5,
                sku=264711005,
                waremd5='Sku5-wdDXWsIiLVm1goleg',
                blue_offers=[_Offers.sku5_offer1],
                randx=2,
            ),
            MarketSku(
                title="разработчик 2",
                hyperid=Model2,
                sku=264711002,
                waremd5='Sku2-wdDXWsIiLVm1goleg',
                blue_offers=[_Offers.sku2_offer1],
                randx=3,
            ),
            MarketSku(
                title="разработчик 7",
                hyperid=Model7,
                sku=264711007,
                waremd5="Sku7-wdDXWsIiLVm1goleg",
                blue_offers=[_Offers.sku7_offer1],
                randx=4,
            ),
            MarketSku(
                title="разработчик 1",
                hyperid=Model1,
                sku=264711001,
                waremd5="Sku1-wdDXWsIiLVm1goleg",
                blue_offers=[_Offers.sku1_offer1],
                randx=5,
            ),
        ]

        cls.index.virtual_models += [
            VirtualModel(virtual_model_id=80001),
        ]

        cls.index.offers += [
            Offer(
                title='white.8.1',
                ts=8101,
                price=10,
                feedid=11,
                offerid='white.offer.8.1',
                waremd5='Sku8Price1-IiLVm1Ggggg',
                randx=10,
                cpa=Offer.CPA_REAL,
                virtual_model_id=80001,
                sku=264711001,
            )
        ]

    BaseRequest = 'debug=1&pp=18&place=prime&cpa=real&use-default-offers=1&yandexuid=26471001'

    def test_purchased_collapsed_models(self, opt_v2_en=0):
        """
        Проверяем, что добавляется pastPurchase: true для моделей, полученных схлопыванием sku, если sku id есть в истории покупок пользователя в профиле из BigB.
        Также проверяем, что отладочный параметр market_force_past_purchase_skus работает.
        """
        request = (
            T.BaseRequest
            + '&rearr-factors=market_show_past_purchase_badge_textless=1;market_force_past_purchase_skus=264711004;market_white_boost_bought_days=0;market_metadoc_search=offers'
            + '&hid=264711&allow-collapsing=1'
            + '&rearr-factors=market_optimize_default_offers_search_v2={}'.format(opt_v2_en)
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {'entity': 'product', 'id': 264711001, 'pastPurchase': True},  # from BigB
                        {'entity': 'product', 'id': 264711005, 'pastPurchase': Absent()},
                        {'entity': 'product', 'id': 264711004, 'pastPurchase': True},  # from rearr-factors
                    ]
                }
            },
        )

    def test_purchased_collapsed_models_optimize_do_search(self):
        """
        Тест test_purchased_collapsed_models с включенной оптимизацией поиска ДО
        """
        self.test_purchased_collapsed_models(1)

        # Ошибка возникает, так как ДО для SKU=264711001 не находится в запросе за ДО.
        # Это происходит и в исходном коде без оптимизации, только не генерируется ошибка.
        # Нужно будет исследовать масштаб появления таких ошибок.
        messageRegex = (
            re.escape(
                "Default offer don't found for ungrouped item (SKU, MODEL, ts): [({}, {}, ".format(264711001, 264711001)
            )
            + "[0-9]{1,}"
            + re.escape("), ] - try old way (explicit)... Explicitly found: 0/1")
        )
        messageRegex = Regex("^" + messageRegex + "$")
        self.error_log.expect(code=ErrorCodes.DEFAULT_OFFER_DONT_FOUND_FOR_UNGROUPED, message=messageRegex).once()

    def test_purchased_offers(self):
        """
        Проверяем, что добавляется pastPurchase: true для офферов, если msku id оффера есть в истории покупок пользователя.
        """
        request = (
            T.BaseRequest
            + '&hid=264711&rearr-factors=market_show_past_purchase_badge_textless=1;market_white_boost_bought_days=0;market_metadoc_search=offers&allow-collapsing=0'
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {'entity': 'offer', 'marketSku': '264711001', 'pastPurchase': True},  # from BigB
                        {'entity': 'offer', 'marketSku': '264711005', 'pastPurchase': Absent()},
                    ]
                }
            },
        )

    def test_disabled_badges(self):
        """
        Проверяем, что по умолчанию pastPurchase не добавляется
        """
        request = (
            T.BaseRequest
            + '&hid=264711&rearr-factors=market_white_boost_bought_days=0;market_metadoc_search=offers&allow-collapsing=0'
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {'entity': 'offer', 'marketSku': '264711001', 'pastPurchase': Absent()},  # from BigB
                        {'entity': 'offer', 'marketSku': '264711005', 'pastPurchase': Absent()},
                    ]
                }
            },
        )

    def test_purchased_skus(self):
        """
        Проверяем, что добавляется pastPurchase: true для ранее купленных entity: sku (market_metadoc_search=skus)
        """
        request = (
            T.BaseRequest
            + '&hid=264711&rearr-factors=market_show_past_purchase_badge_textless=1;market_white_boost_bought_days=0;market_metadoc_search=skus&allow-collapsing=0'
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {'entity': 'sku', 'id': '264711001', 'pastPurchase': True},  # from BigB
                        {'entity': 'sku', 'id': '264711005', 'pastPurchase': Absent()},
                    ]
                }
            },
        )

    def test_purchased_virtual_models(self):
        """
        Проверяем, что добавляется pastPurchase: true для виртуальных моделей, если msku id исходного оффера есть в истории покупок пользователя.
        """
        request = (
            T.BaseRequest
            + '&text=white&rearr-factors=market_show_past_purchase_badge_text=1;market_white_boost_bought_days=0;market_metadoc_search=offers&allow-collapsing=1'
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {
                            'entity': 'product',
                            'id': 80001,  # generated from offer with msku id 264711001 which is purchased via BigB
                            'pastPurchase': True,
                        },
                    ]
                }
            },
        )


if __name__ == '__main__':
    main()

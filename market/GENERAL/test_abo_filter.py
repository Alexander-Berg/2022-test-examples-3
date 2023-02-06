#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa

from market.pylibrary.lite.matcher import EmptyList, Absent
from core.testcase import TestCase, main
from core.types import BlueOffer, DynamicMarketSku, MarketSku, Offer, Shop

SKU_ID = 1
OTHER_SKU_ID = 2

MODEL_ID = 100
OTHER_MODEL_ID = 101

BLUE_OFFER_SHOP_SKU = 'blue_offer_shop_sku'
WHITE_OFFER_ID = 'white_offer_id'

BLUE_SHOP_ID = 1
WHITE_SHOP_ID = 2

HID = 1000


def make_hide_rules_requests(prefix):
    # Original request (no rearr flags)
    requests = [prefix]

    # Requests with all possible values of flags
    requests += [
        prefix + '&rearr-factors=hide_rules_strategy={}'.format(strat)
        for strat in ['use_dynamic', 'use_unified_hide_rules', 'use_all_sources']
    ]
    return requests


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.mskus += [
            MarketSku(
                sku=SKU_ID,
                hyperid=MODEL_ID,
                hid=HID,
                blue_offers=[
                    BlueOffer(
                        offerid=BLUE_OFFER_SHOP_SKU,
                        feedid=BLUE_SHOP_ID,
                    )
                ],
            )
        ]
        cls.index.offers += [Offer(offerid=WHITE_OFFER_ID, sku=SKU_ID, fesh=WHITE_SHOP_ID, hyperid=MODEL_ID, hid=HID)]
        cls.index.shops += [
            Shop(
                fesh=BLUE_SHOP_ID,
                datafeed_id=BLUE_SHOP_ID,
                fulfillment_program=True,
                blue=Shop.BLUE_REAL,
                cpa=Shop.CPA_REAL,
            )
        ]

    def check_offers(self, blue, white):
        blue_offer_expected = {'shopSku': BLUE_OFFER_SHOP_SKU}
        white_offer_expected = {'feed': {'offerId': WHITE_OFFER_ID}}

        requests = make_hide_rules_requests(
            'place=offerinfo&market-sku={msku}&show-urls=&rids=0&regset=1'.format(msku=SKU_ID)
        )

        for request in requests:
            response = self.report.request_json(request)
            if blue:
                self.assertFragmentIn(response, blue_offer_expected)
            else:
                self.assertFragmentNotIn(response, blue_offer_expected)
            if white:
                self.assertFragmentIn(response, white_offer_expected)
            else:
                self.assertFragmentNotIn(response, white_offer_expected)

    def test_without_abo_filter(self):
        '''
        Без динамика показаны оба офера
        '''
        self.check_offers(True, True)

    def test_abo_filter_by_sku(self):
        '''
        Блокируем по МСКУ.
        Оба офера скрыты
        '''
        self.dynamic.market_dynamic.disabled_market_sku = [DynamicMarketSku(market_sku=str(SKU_ID))]
        self.check_offers(False, False)

    def test_abo_filter_by_supplier_and_shop_sku(self):
        '''
        Блокируем офер поставщика по shop_sku
        Скрыт офер только этого поставщика
        '''
        # Скроем белый офер
        self.dynamic.market_dynamic.disabled_market_sku = [
            DynamicMarketSku(supplier_id=WHITE_SHOP_ID, shop_sku=WHITE_OFFER_ID)
        ]

        self.check_offers(True, False)

        # Теперь блокируем синий офер. Белый должен остаться на выдаче
        self.dynamic.market_dynamic.disabled_market_sku = [
            DynamicMarketSku(supplier_id=BLUE_SHOP_ID, shop_sku=BLUE_OFFER_SHOP_SKU)
        ]
        self.check_offers(False, True)

        # Отдельными записями скрываем оба поставщика
        self.dynamic.market_dynamic.disabled_market_sku = [
            DynamicMarketSku(supplier_id=WHITE_SHOP_ID, shop_sku=WHITE_OFFER_ID),
            DynamicMarketSku(supplier_id=BLUE_SHOP_ID, shop_sku=BLUE_OFFER_SHOP_SKU),
        ]

        self.check_offers(False, False)

    def test_abo_filter_by_supplier_and_market_sku(self):
        '''
        Блокируем офер поставщика по msku
        Скрыт офер только этого поставщика
        '''
        # Белый поставщик
        self.dynamic.market_dynamic.disabled_market_sku = [
            DynamicMarketSku(supplier_id=WHITE_SHOP_ID, market_sku=str(SKU_ID))
        ]
        self.check_offers(True, False)

        # Теперь блокируем синий офер. Белый должен остаться на выдаче
        self.dynamic.market_dynamic.disabled_market_sku = [
            DynamicMarketSku(supplier_id=BLUE_SHOP_ID, market_sku=str(SKU_ID))
        ]

        self.check_offers(False, True)

        # Отдельными записями скрываем оба поставщика
        self.dynamic.market_dynamic.disabled_market_sku = [
            DynamicMarketSku(supplier_id=BLUE_SHOP_ID, market_sku=str(SKU_ID)),
            DynamicMarketSku(supplier_id=WHITE_SHOP_ID, market_sku=str(SKU_ID)),
        ]

        self.check_offers(False, False)

    def test_abo_filter_other_offer(self):
        '''
        Проверяем, что скрытие одного офера поставщика не влияет на показ других оферов этого поставщика
        '''
        self.dynamic.market_dynamic.disabled_market_sku = [
            DynamicMarketSku(supplier_id=WHITE_SHOP_ID, market_sku=str(OTHER_SKU_ID))
        ]
        self.check_offers(True, True)

        self.dynamic.market_dynamic.disabled_market_sku = [
            DynamicMarketSku(supplier_id=WHITE_SHOP_ID, shop_sku=WHITE_OFFER_ID + '_other')
        ]
        self.check_offers(True, True)

    def test_abo_filter_prime(self):
        '''
        Проверяем фильтрацию оферов, мску и моделей в прайме
        '''
        requests = make_hide_rules_requests('place=prime&hid={}'.format(HID))

        blue_offer_expected = {'shopSku': BLUE_OFFER_SHOP_SKU}
        white_offer_expected = {'feed': {'offerId': WHITE_OFFER_ID}}

        # Блокируем модель. На выдаче нет оферов и моделей
        self.dynamic.market_dynamic.disabled_market_sku = [DynamicMarketSku(model_id=MODEL_ID)]
        for request in requests:
            response = self.report.request_json(request)
            self.assertFragmentIn(response, {'search': {'results': EmptyList()}})

        # Блокируем другую модель. На выдаче есть офера
        self.dynamic.market_dynamic.disabled_market_sku = [DynamicMarketSku(model_id=OTHER_MODEL_ID)]
        for request in requests:
            response = self.report.request_json(request)
            self.assertFragmentIn(response, blue_offer_expected)

        # Блокируем только синий офер по модели и поставщику
        self.dynamic.market_dynamic.disabled_market_sku = [
            DynamicMarketSku(model_id=MODEL_ID, supplier_id=BLUE_SHOP_ID)
        ]
        for request in requests:
            response = self.report.request_json(request)
            self.assertFragmentIn(response, white_offer_expected)

        # Блокируем только белый офер по модели и поставщику
        self.dynamic.market_dynamic.disabled_market_sku = [
            DynamicMarketSku(model_id=MODEL_ID, supplier_id=WHITE_SHOP_ID)
        ]
        for request in requests:
            response = self.report.request_json(request)
            self.assertFragmentIn(response, blue_offer_expected)

        # Блокируем оферы по МСКУ. Модель остается на выдаче
        self.dynamic.market_dynamic.disabled_market_sku = [DynamicMarketSku(market_sku=str(SKU_ID))]
        for request in requests:
            response = self.report.request_json(request)
            self.assertFragmentIn(
                response, {'entity': 'product', 'id': MODEL_ID, 'offers': {'count': 0, 'list': Absent()}}
            )

    def test_abo_filter_modelinfo(self):
        '''
        Проверяем скрытие модели в modelinfo
        '''
        requests = make_hide_rules_requests('place=modelinfo&hyperid={}&rids=0'.format(MODEL_ID))

        # Пустой динамик
        self.dynamic.market_dynamic.disabled_market_sku = []
        for request in requests:
            response = self.report.request_json(request)
            self.assertFragmentIn(
                response,
                {
                    'entity': 'product',
                    'id': MODEL_ID,
                },
            )

        # Блокируем оферы по МСКУ. Модель остается на выдаче
        self.dynamic.market_dynamic.disabled_market_sku = [DynamicMarketSku(market_sku=str(SKU_ID))]
        for request in requests:
            response = self.report.request_json(request)
            self.assertFragmentIn(
                response,
                {
                    'entity': 'product',
                    'id': MODEL_ID,
                },
            )

        # Блокируем модель. На выдаче нет оферов и моделей
        self.dynamic.market_dynamic.disabled_market_sku = [DynamicMarketSku(model_id=MODEL_ID)]
        for request in requests:
            response = self.report.request_json(request)
            self.assertFragmentIn(response, {'search': {'results': EmptyList()}})


if __name__ == '__main__':
    main()

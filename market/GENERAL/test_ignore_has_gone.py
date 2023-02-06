#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import BlueOffer, DynamicSkuOffer, MarketSku, Offer, Shop
from core.matcher import Contains
from core.logs import ErrorCodes


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ["market_money_disable_bids=0"]
        cls.index.shops += [
            Shop(fesh=3001, datafeed_id=3001, priority_region=213, name='shop_3001', cpa=Shop.CPA_REAL),
            Shop(fesh=444, datafeed_id=444, name='ShadesOfBlue', blue=Shop.BLUE_REAL, cpa=Shop.CPA_REAL),
        ]
        cls.index.offers += [
            Offer(
                feedid=3001,
                fesh=3001,
                offerid='1',
                waremd5='HasGone_LZST2Ekoq4xEgg',
                has_gone=True,
                price=376,
                cpa=Offer.CPA_REAL,
            ),
        ]

        # blue market
        cls.index.mskus += [
            MarketSku(sku=1, hyperid=1, blue_offers=[BlueOffer(feedid=666, waremd5='CeruleanOffer________g')]),
            MarketSku(
                sku=2, hyperid=2, has_gone=True, blue_offers=[BlueOffer(feedid=555, waremd5='AzureOfferHasGone____g')]
            ),
            MarketSku(
                sku=3,
                hyperid=3,
                blue_offers=[BlueOffer(feedid=444, waremd5='GlaucousOfferDynamic_g', offerid='GlaucousOffer')],
            ),
            MarketSku(sku=4, hyperid=4, price=0, blue_offers=[BlueOffer(feedid=333, waremd5='TealOfferFreeOfChargeg')]),
        ]
        cls.dynamic.disabled_sku_offers += [
            DynamicSkuOffer(shop_id=444, sku='GlaucousOffer'),
        ]

    def test_offerinfo_ignore_has_gone(self):
        '''
        Что проверяем: выдачу белых офферов с флагом ignore-has-gone, даже если оффер скрыт
        '''
        offer = {
            'wareId': 'HasGone_LZST2Ekoq4xEgg',
            'prices': {'value': '376'},
            'cpa': 'real',
        }
        places = (
            ('offerinfo', False),  # Проверяем, что без флага этот оффер скрыт
            ('offerinfo&ignore-has-gone=1', True),  # Проверяем наличие оффера, если флаг есть
            ('offerinfo_with_hidden_offers', True),
        )  # offerinfo_with_hidden_offers отображает скрытые офферы
        request = 'place={}&rids=0&show-urls=external&regset=1&{}'

        for place, is_available in places:
            for condition in ['offerid=HasGone_LZST2Ekoq4xEgg', 'feed_shoffer_id=3001-1']:
                response = self.report.request_json(request.format(place, condition))
                if is_available:
                    self.assertFragmentIn(response, offer)
                else:
                    self.assertFragmentNotIn(response, {'wareId': 'HasGone_LZST2Ekoq4xEgg'})

    BLUE_OFFERS = (
        # (waremd5, is_available)
        ("CeruleanOffer________g", True),
        ("AzureOfferHasGone____g", False),
        ("GlaucousOfferDynamic_g", False),
        ("TealOfferFreeOfChargeg", False),
    )

    BLUE_REQUESTS = (
        "place=offerinfo&rgb=blue&rids=0&show-urls=external&regset=1&offerid={}",
        "place=actual_delivery&rgb=blue&rids=213&offers-list={}:1",
        "place=prime&rgb=blue&offerid={}",
        "place=sku_offers&rgb=blue&market-sku=1,2,3,4&offerid={}",
    )

    def test_ignore_has_gone_blue_no_flag(self):
        '''
        Что проверяем: выдачу синих офферов — сначала без флага
        Наличие должно соответствовать is_available
        '''
        for waremd5, is_available in T.BLUE_OFFERS:
            offer_in_response = {'wareId': waremd5}
            missing_offer_for_actual_delivery = {"wareId": waremd5, "problems": ["NONEXISTENT_OFFER"]}

            for request in T.BLUE_REQUESTS:
                response = self.report.request_json(request.format(waremd5))
                if is_available:
                    self.assertFragmentIn(response, offer_in_response)
                else:
                    if request.startswith('place=actual_delivery'):
                        self.assertFragmentIn(response, missing_offer_for_actual_delivery)
                    else:
                        self.assertFragmentNotIn(response, offer_in_response)

    def test_ignore_has_gone_blue_with_flag(self):
        '''
        Что проверяем: выдачу синих офферов с флагом ignore-has-gone
        Оферы всегда должны быть в наличии
        '''
        for waremd5, _ in T.BLUE_OFFERS:
            offer_in_response = {'wareId': waremd5}

            for request in T.BLUE_REQUESTS:
                response = self.report.request_json(request.format(waremd5) + '&ignore-has-gone=1')
                self.assertFragmentIn(response, offer_in_response)

        self.base_logs_storage.error_log.expect(
            code=ErrorCodes.OFFER_HAS_NO_PRICE,
            message=Contains('Offer has no price', 'wareMd5: TealOfferFreeOfChargeg, feedId: 333'),
        )


if __name__ == '__main__':
    main()

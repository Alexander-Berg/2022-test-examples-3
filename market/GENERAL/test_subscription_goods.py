#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import Model, Offer, Shop, MarketSku, BlueOffer
from core.testcase import TestCase, main
from core.matcher import Absent, NotEmpty, ElementCount
from core.types.delivery import BlueDeliveryTariff


# Tests for MARKETOUT-43116: show subscription goods only when requested


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.shops += [
            Shop(
                fesh=11317159,
                business_fesh=11317160,
                datafeed_id=1,
                priority_region=213,
                blue=Shop.BLUE_REAL,
                cpa=Shop.CPA_REAL,
            ),
            Shop(fesh=12345, priority_region=213, cpa=Shop.CPA_REAL),
        ]

        cls.index.models += [Model(hyperid=100)]

        cls.index.mskus += [
            MarketSku(
                hyperid=100,
                sku=1001,
                blue_offers=[BlueOffer(title='subscription offer', business_id=11317160, feedid=1, price=799)],
            ),
            MarketSku(
                hyperid=100,
                sku=2002,
                blue_offers=[BlueOffer(title='subscription offer 2', business_id=11317160, feedid=1)],
            ),
        ]

        cls.index.offers += [
            Offer(hyperid=100, sku=1001, title='simple offer 1', fesh=12345),
            Offer(hyperid=100, sku=1001, title='simple offer 2', fesh=12345),
        ]

    def aggregate_filters(self, response):
        filters = response["debug"]["brief"]["filters"].copy()
        for subrequest in response["debug"]["metasearch"]["subrequests"]:
            if isinstance(subrequest, dict):
                filters.update(subrequest["brief"]["filters"])
        return filters

    def test_subscription_goods(self):
        request = "place=productoffers&market-sku=1001&debug=1&show-subscription-goods=1"
        response = self.report.request_json(request)
        all_filters = self.aggregate_filters(response)
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {
                            'slug': 'subscription-offer',
                            'supplier': {
                                'id': 11317159,
                                'business_id': 11317160,
                            },
                        },
                        {'slug': 'simple-offer-1'},
                        {'slug': 'simple-offer-2'},
                    ]
                },
            },
            allow_different_len=False,
        )
        self.assertFalse('SUBSCRIPTION_GOODS' in all_filters)

    def test_without_subscription_goods(self):
        request = "place=productoffers&market-sku=1001&debug=1"
        response = self.report.request_json(request)
        all_filters = self.aggregate_filters(response)
        self.assertFragmentIn(
            response,
            {
                'search': {'results': [{'slug': 'simple-offer-1'}, {'slug': 'simple-offer-2'}]},
            },
            allow_different_len=False,
        )
        self.assertTrue(all_filters['SUBSCRIPTION_GOODS'])

    def test_subscription_goods_in_product_offer(self):
        request = 'place=productoffers&market-sku=2002&offers-set=defaultList&show-subscription-goods=1'
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {'search': {'results': [{'benefit': {'type': NotEmpty()}, 'isYaSubscriptionOffer': True}]}},
            allow_different_len=False,
        )

    def test_subscription_goods_in_product_offer_no_default_list(self):
        request = 'place=productoffers&market-sku=1001&show-subscription-goods=1'
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {'benefit': Absent(), 'isYaSubscriptionOffer': True},
                        {'benefit': Absent(), 'isYaSubscriptionOffer': Absent()},
                        {'benefit': Absent(), 'isYaSubscriptionOffer': Absent()},
                    ]
                }
            },
            allow_different_len=False,
        )

    def test_sku_offers(self):
        request = 'place=sku_offers&market-sku=2002'
        response = self.report.request_json(request)
        self.assertFragmentIn(response, {'entity': 'sku', 'id': '2002', 'offers': {'items': Absent()}})

        response = self.report.request_json(request + '&show-subscription-goods=1')
        self.assertFragmentIn(response, {'entity': 'sku', 'id': '2002', 'offers': {'items': ElementCount(1)}})

    @classmethod
    def prepare_free_delivery_for_subscription_goods(cls):
        cls.index.blue_delivery_modifiers.set_default_modifier(
            tariffs=[
                BlueDeliveryTariff(user_price=99),
            ]
        )
        cls.index.blue_delivery_modifiers.add_modifier(
            tariffs=[
                BlueDeliveryTariff(user_price=0, for_plus=1),
            ],
            regions=[213],
        )

    def test_free_delivery_for_subscription_goods(self):
        request = "place=productoffers&market-sku=1001&debug=1&show-subscription-goods=1&rids=213"
        response = self.report.request_json(request)
        self.assertFragmentIn(response, {'betterWithPlus': Absent()})


if __name__ == '__main__':
    main()

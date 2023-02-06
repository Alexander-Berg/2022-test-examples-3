#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa

from core.testcase import TestCase, main
from core.types import BundleOfferId, HyperCategory, HyperCategoryType, Model, Offer, Promo, PromoType, Shop


# MARKETOUT-24159
class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']
        cls.index.shops += [
            Shop(fesh=1, datafeed_id=1),
        ]
        cls.index.hypertree += [
            HyperCategory(hid=1, output_type=HyperCategoryType.CLUSTERS, show_offers=True, visual=True),
            HyperCategory(hid=2, output_type=HyperCategoryType.CLUSTERS, show_offers=True, visual=True),
            HyperCategory(hid=3, output_type=HyperCategoryType.CLUSTERS, show_offers=True, visual=True),
        ]

        cls.index.models += [
            Model(hyperid=1, hid=1),
            Model(hyperid=2, hid=2),
            Model(hyperid=3, hid=3),
        ]

        cls.index.offers += [
            Offer(
                offerid='1',
                fesh=1,
                feedid=1,
                hyperid=1,
                hid=1,
                price=100,
                promo=Promo(
                    promo_type=PromoType.BUNDLE,
                    key='promo_1',
                    bundle_offer_ids=[
                        BundleOfferId(feed_id=1, offer_id='1'),
                        BundleOfferId(feed_id=1, offer_id='2'),
                        BundleOfferId(feed_id=1, offer_id='3'),
                    ],
                    feed_id=1,
                ),
            ),
            Offer(
                offerid='2',
                fesh=1,
                feedid=1,
                hid=2,
                hyperid=2,
                price=60,
                promo_price=30,
                promo=Promo(
                    promo_type=PromoType.FLASH_DISCOUNT,
                    key='promo_2',
                    bundle_offer_ids=[
                        BundleOfferId(feed_id=1, offer_id='2'),
                        BundleOfferId(feed_id=1, offer_id='3'),
                    ],
                    feed_id=1,
                ),
            ),
            Offer(offerid='3', fesh=1, feedid=1, hid=3, hyperid=3, price=50),
        ]

    def test_promos_disabled(self):
        response = self.report.request_json('place=prime&hid=1&hide-offer-promo=1')
        self.assertFragmentNotIn(
            response,
            {
                'results': [
                    {'entity': 'offer', 'promos': [{'key': 'promo_1'}]},
                ]
            },
        )

        response = self.report.request_json('place=prime&hid=1')
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {'entity': 'offer', 'promos': [{'key': 'promo_1'}]},
                ]
            },
        )

    def test_flash_discount_not_affected(self):
        response = self.report.request_json('place=prime&hid=2&hide-offer-promo=1')
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {'entity': 'offer', 'promos': [{'key': 'promo_2'}]},
                ]
            },
        )

        response = self.report.request_json('place=prime&hid=2')
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {'entity': 'offer', 'promos': [{'key': 'promo_2'}]},
                ]
            },
        )


if __name__ == '__main__':
    main()

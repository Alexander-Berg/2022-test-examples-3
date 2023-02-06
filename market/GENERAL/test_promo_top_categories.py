#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import BundleOfferId, HyperCategory, Offer, Promo, PromoType, Shop


def getPromoBundle(promoId, offerIds):
    return Promo(
        promo_type=PromoType.BUNDLE,
        key=promoId,
        feed_id=1,
        bundle_offer_ids=[BundleOfferId(feed_id=1, offer_id=offerId) for offerId in offerIds],
    )


def getPromoGift(giftId, gifts):
    return Promo(promo_type=PromoType.GIFT_WITH_PURCHASE, key=giftId, feed_id=1, gift_gifts=gifts)


class T(TestCase):

    # https://st.yandex-team.ru/MARKETOUT-21787
    @classmethod
    def prepare_promo_top_categories(cls):
        cls.index.shops += [
            Shop(fesh=1, datafeed_id=1),
        ]

        cls.index.hypertree += [
            HyperCategory(
                hid=1,
                children=[
                    HyperCategory(hid=11),
                    HyperCategory(hid=12),
                    HyperCategory(hid=13),
                ],
            ),
            HyperCategory(
                hid=2,
                children=[
                    HyperCategory(hid=21),
                    HyperCategory(hid=22),
                    HyperCategory(hid=23),
                ],
            ),
            HyperCategory(
                hid=3,
                children=[
                    HyperCategory(hid=31),
                    HyperCategory(hid=32),
                    HyperCategory(hid=33),
                ],
            ),
        ]

        cls.index.offers += [
            Offer(
                offerid='11',
                hid=11,
                fesh=1,
                feedid=1,
                price=100,
                promo=getPromoBundle(promoId='bundle1', offerIds=['1', '5']),
            ),
            Offer(
                offerid='12',
                hid=11,
                fesh=1,
                feedid=1,
                price=100,
                promo=getPromoBundle(promoId='bundle2', offerIds=['2', '5']),
            ),
            Offer(
                offerid='13',
                hid=11,
                fesh=1,
                feedid=1,
                price=100,
                promo=getPromoBundle(promoId='bundle3', offerIds=['3', '5']),
            ),
            Offer(
                offerid='14',
                hid=12,
                fesh=1,
                feedid=1,
                price=100,
                promo=getPromoBundle(promoId='bundle4', offerIds=['4', '5']),
            ),
            Offer(offerid='15', hid=12, fesh=1, feedid=1, price=50),
            Offer(
                offerid='16', hid=12, fesh=1, feedid=1, price=100, promo=getPromoGift(giftId='gift1', gifts=['1', '2'])
            ),
            Offer(
                offerid='17', hid=12, fesh=1, feedid=1, price=100, promo=getPromoGift(giftId='gift2', gifts=['1', '2'])
            ),
            Offer(
                offerid='18', hid=13, fesh=1, feedid=1, price=100, promo=getPromoGift(giftId='gift3', gifts=['1', '2'])
            ),
            Offer(
                offerid='20', hid=21, fesh=1, feedid=1, price=100, promo=getPromoGift(giftId='gift4', gifts=['1', '2'])
            ),
            Offer(
                offerid='21', hid=21, fesh=1, feedid=1, price=100, promo=getPromoGift(giftId='gift5', gifts=['1', '2'])
            ),
            Offer(
                offerid='22', hid=22, fesh=1, feedid=1, price=100, promo=getPromoGift(giftId='gift6', gifts=['1', '2'])
            ),
        ]

    def test_promo_type_filter(self):
        request_template = 'place=promo_top_categories&hid=%s&promo-type=%s&promo-top-categories-count=%s'

        response = self.report.request_json(request_template % (1, PromoType.BUNDLE, 2))
        self.assertFragmentIn(
            response,
            {
                "categories": [
                    {"entity": "category", "id": 11},
                    {"entity": "category", "id": 12},
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

        response = self.report.request_json(request_template % (1, PromoType.GIFT_WITH_PURCHASE, 2))
        self.assertFragmentIn(
            response,
            {
                "categories": [
                    {"entity": "category", "id": 12},
                    {"entity": "category", "id": 13},
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def test_hid_filter(self):
        request_template = 'place=promo_top_categories&hid=%s&promo-type=%s&promo-top-categories-count=%s'

        response = self.report.request_json(request_template % (1, PromoType.GIFT_WITH_PURCHASE, 2))
        self.assertFragmentIn(
            response,
            {
                "categories": [
                    {"entity": "category", "id": 12},
                    {"entity": "category", "id": 13},
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

        response = self.report.request_json(request_template % (2, PromoType.GIFT_WITH_PURCHASE, 2))
        self.assertFragmentIn(
            response,
            {
                "categories": [
                    {"entity": "category", "id": 21},
                    {"entity": "category", "id": 22},
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def test_top_count(self):
        request_template = 'place=promo_top_categories&hid=%s&promo-type=%s'

        response = self.report.request_json(
            (request_template % (1, PromoType.BUNDLE)) + '&promo-top-categories-count=2'
        )
        self.assertFragmentIn(
            response,
            {
                "categories": [
                    {"entity": "category", "id": 11},
                    {"entity": "category", "id": 12},
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

        response = self.report.request_json(
            (request_template % (1, PromoType.BUNDLE)) + '&promo-top-categories-count=1'
        )
        self.assertFragmentIn(
            response,
            {
                "categories": [
                    {"entity": "category", "id": 11},
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

        response = self.report.request_json((request_template % (1, PromoType.BUNDLE)))
        self.assertFragmentIn(
            response,
            {
                "categories": [
                    {"entity": "category", "id": 11},
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def test_manual_categories(self):
        response = self.report.request_json(
            'place=promo_top_categories&hid=1&promo-type=bundle&promo-top-categories-count=3&extra-promo-hid=32,31'
        )
        self.assertFragmentIn(
            response,
            {
                "categories": [
                    {"entity": "category", "id": 32},
                    {"entity": "category", "id": 31},
                    {"entity": "category", "id": 11},
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )


if __name__ == '__main__':
    main()

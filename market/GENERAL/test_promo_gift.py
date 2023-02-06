#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import HyperCategory, Model, Offer, Promo, PromoGift, PromoType
from core.matcher import Absent

from datetime import datetime


gifts = [
    PromoGift.Create(1, 1),
    PromoGift.Create(1, 2),
    PromoGift.Create(2, 2),
    PromoGift.Create(2, 3),
]


def gift2answer(gift):
    return {
        "entity": "gift",
        "name": gift.name,
        "feed": {
            "id": gift.feed_id,
            "giftId": gift.gift_id,
        },
        "picture": {
            "namespace": gift.pic_namespace,
            "groupId": gift.pic_group_id,
            "key": gift.pic_key,
        },
    }


def gift_offer_title(feedid, offer_id):
    return 'gift %d:%s' % (feedid, offer_id)


def promo_offer_title(feedid, offer_id):
    return 'promo offer %d:%s' % (feedid, offer_id)


def gift_offer(feedid, offer_id):
    return Offer(
        title=gift_offer_title(feedid, offer_id),
        fesh=17,
        feedid=feedid,
        hid=7,
        offerid=offer_id,
        bid=1,
        pull_to_min_bid=False,
        price=10000,
    )


def promo_offer(feedid, offer_id, key, offers, gifts, required_items=None, required_quantity=None, required_sum=None):
    return Offer(
        title=promo_offer_title(feedid, offer_id),
        fesh=17,
        hid=42,
        feedid=feedid,
        offerid=offer_id,
        promo=Promo(
            promo_type=PromoType.GIFT_WITH_PURCHASE,
            start_date=datetime(1980, 1, 1),
            end_date=datetime(2050, 1, 1),
            key=key,
            url='http://my.url',
            required_items=required_items,
            required_quantity=required_quantity,
            required_sum=required_sum,
            feed_id=feedid,
            gift_offers=offers,
            gift_gifts=gifts,
        ),
    )


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.hypertree += [HyperCategory(hid=42, uniq_name='Тракторы')]

    @classmethod
    def prepare_promo_code(cls):
        cls.index.offers += [
            gift_offer(1, 1),
            gift_offer(1, 2),
            gift_offer(1, 3),
            gift_offer(2, "abc"),
            gift_offer(2, "ABC"),
            promo_offer(1, 17, 'offers_only', [1, 2], [], required_quantity=2),
            promo_offer(1, 18, 'gifts_only', [], [1, 2], required_items=5),
            promo_offer(1, 19, 'gifts_and_offers', [1, 3], [1, 2], required_sum={'value': 200, 'currency': 'RUR'}),
            promo_offer(2, 20, 'gifts_and_offers_other_feed', ["abc", "ABC"], [2, 3]),
            # 'xMpQQQC5I4INzFCab3WEmw' 'xMpQQQC5I4INzQQab3WEmw'
        ]

        cls.index.promo_gifts = gifts

    def test_offers(self):
        response = self.report.request_json('place=prime&text=offer')
        self.assertFragmentIn(
            response,
            [
                {
                    "entity": "offer",
                    "titles": {"raw": promo_offer_title(1, 17)},
                    "promos": [
                        {
                            "type": "gift-with-purchase",
                            "key": "offers_only",
                            "url": "http://my.url",
                            "gifts": {
                                "giftsQuantity": 0,
                                "offersQuantity": 2,
                            },
                            "startDate": "1980-01-01T00:00:00Z",
                            "endDate": "2050-01-01T00:00:00Z",
                        }
                    ],
                },
                {
                    "entity": "offer",
                    "titles": {"raw": promo_offer_title(1, 18)},
                    "promos": [
                        {
                            "type": "gift-with-purchase",
                            "key": "gifts_only",
                            "url": "http://my.url",
                            "gifts": {
                                "giftsQuantity": 2,
                                "offersQuantity": 0,
                            },
                            "startDate": "1980-01-01T00:00:00Z",
                            "endDate": "2050-01-01T00:00:00Z",
                        }
                    ],
                },
                {
                    "entity": "offer",
                    "titles": {"raw": promo_offer_title(1, 19)},
                    "promos": [
                        {
                            "type": "gift-with-purchase",
                            "key": "gifts_and_offers",
                            "url": "http://my.url",
                            "gifts": {
                                "giftsQuantity": 2,
                                "offersQuantity": 2,
                            },
                            "startDate": "1980-01-01T00:00:00Z",
                            "endDate": "2050-01-01T00:00:00Z",
                        }
                    ],
                },
                {
                    "entity": "offer",
                    "titles": {"raw": promo_offer_title(2, 20)},
                    "promos": [
                        {
                            "type": "gift-with-purchase",
                            "key": "gifts_and_offers_other_feed",
                            "url": "http://my.url",
                            "gifts": {
                                "giftsQuantity": 2,
                                "offersQuantity": 2,
                            },
                            "startDate": "1980-01-01T00:00:00Z",
                            "endDate": "2050-01-01T00:00:00Z",
                        }
                    ],
                },
            ],
        )

    def test_promo_place_offers_only(self):
        response = self.report.request_json('place=promo&promoid=offers_only')
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "promos": [
                        {
                            "type": "gift-with-purchase",
                            "key": "offers_only",
                            "parameters": {
                                "requiredQuantity": 2,
                                "requiredItems": Absent(),
                                "requiredSum": Absent(),
                            },
                            "bonusItems": [
                                {"entity": "offer", "titles": {"raw": gift_offer_title(1, 1)}},
                                {"entity": "offer", "titles": {"raw": gift_offer_title(1, 2)}},
                            ],
                        }
                    ]
                }
            },
        )

    def test_promo_place_gifts_only(self):
        response = self.report.request_json('place=promo&promoid=gifts_only')
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "promos": [
                        {
                            "type": "gift-with-purchase",
                            "key": "gifts_only",
                            "parameters": {
                                "requiredItems": 5,
                                "requiredQuantity": Absent(),
                                "requiredSum": Absent(),
                            },
                            "bonusItems": [
                                gift2answer(gifts[0]),
                                gift2answer(gifts[1]),
                            ],
                        }
                    ]
                }
            },
        )

    def test_promo_place_gifts_and_offers(self):
        response = self.report.request_json('place=promo&promoid=gifts_and_offers')
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "promos": [
                        {
                            "type": "gift-with-purchase",
                            "key": "gifts_and_offers",
                            "parameters": {
                                "requiredSum": {'value': '200', 'currency': 'RUR'},
                                "requiredQuantity": Absent(),
                                "requiredItems": Absent(),
                            },
                            "bonusItems": [
                                {"entity": "offer", "titles": {"raw": gift_offer_title(1, 1)}},
                                {"entity": "offer", "titles": {"raw": gift_offer_title(1, 3)}},
                                gift2answer(gifts[0]),
                                gift2answer(gifts[1]),
                            ],
                        }
                    ]
                }
            },
        )

    def test_promo_place_gifts_and_offers_other_feed(self):
        response = self.report.request_json('place=promo&promoid=gifts_and_offers_other_feed')
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "promos": [
                        {
                            "type": "gift-with-purchase",
                            "key": "gifts_and_offers_other_feed",
                            "parameters": Absent(),
                            "bonusItems": [
                                {"entity": "offer", "titles": {"raw": gift_offer_title(2, "abc")}},
                                {"entity": "offer", "titles": {"raw": gift_offer_title(2, "ABC")}},
                                gift2answer(gifts[2]),
                                gift2answer(gifts[3]),
                            ],
                        }
                    ]
                }
            },
        )

    @classmethod
    def prepare_gift_offer_with_model(cls):
        cls.index.models += [
            Model(hyperid=19448000, title='boring_model', hid=40),
        ]

        cls.index.offers += [
            Offer(
                title=gift_offer_title(1, 19448001),
                fesh=17,
                feedid=1,
                hid=40,
                offerid=19448001,
                hyperid=19448000,
            )
        ]

        cls.index.offers += [
            promo_offer(1, 19448002, 'offer_with_model', [19448001], []),
        ]

    def test_model_title_in_offers(self):
        response = self.report.request_json('place=promo&promoid=offer_with_model')
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "promos": [
                        {
                            "type": "gift-with-purchase",
                            "key": "offer_with_model",
                            "bonusItems": [
                                {
                                    "entity": "offer",
                                    "titles": {
                                        "modelTitle": "boring_model",
                                    },
                                },
                            ],
                        }
                    ]
                }
            },
        )


if __name__ == '__main__':
    main()

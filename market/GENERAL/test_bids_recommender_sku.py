#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import MnPlace, Offer, Shop
from core.testcase import TestCase, main


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.matrixnet.on_default_place(MnPlace.BASE_SEARCH).respond(0.01)

        cls.index.shops += [
            Shop(fesh=1, priority_region=213),
            Shop(fesh=2, priority_region=213),
            Shop(fesh=3, priority_region=213),
        ]

        cls.index.offers += [
            Offer(hyperid=1, fesh=1, bid=110, sku=11, feedid=1, offerid='1'),
            Offer(hyperid=1, fesh=2, bid=120, sku=11, feedid=2, offerid='1'),
            Offer(hyperid=1, fesh=3, bid=130, sku=11, feedid=3, offerid='1'),
            Offer(hyperid=1, fesh=1, bid=210, sku=12, feedid=1, offerid='2'),
            Offer(hyperid=1, fesh=2, bid=220, sku=12, feedid=2, offerid='2'),
            Offer(hyperid=1, fesh=3, bid=230, sku=12, feedid=3, offerid='2'),
            Offer(hyperid=1, fesh=1, bid=310, feedid=1, offerid='3'),
            Offer(hyperid=1, fesh=2, bid=320, feedid=2, offerid='3'),
        ]

    def test_single_bids_recommender_msku(self):
        """
        Проверяем поиск по одному MSKU
        """
        response = self.report.request_json('place=bids_recommender&rids=213&feed_shoffer_id=1-1')
        self.assertFragmentIn(
            response,
            {
                "position": [
                    {'bid': 2},
                    {'bid': 2},
                    {'bid': 1},
                    {'bid': 1},
                    {'bid': 1},
                    {'bid': 1},
                    {'bid': 1},
                    {'bid': 1},
                    {'bid': 1},
                    {'bid': 1},
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def test_single_bids_recommender_no_msku(self):
        """
        Проверяем, что если msku у оффера нет - используется вся карточка
        """
        response = self.report.request_json('place=bids_recommender&rids=213&feed_shoffer_id=1-3')
        self.assertFragmentIn(
            response,
            {
                "position": [
                    {'bid': 2},
                    {'bid': 2},
                    {'bid': 1},
                    {'bid': 1},
                    {'bid': 1},
                    {'bid': 1},
                    {'bid': 1},
                    {'bid': 1},
                    {'bid': 1},
                    {'bid': 1},
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def test_batch_bids_recommender(self):
        """
        Проверяем батчевый поиск - есть и SKU и модели
        """
        response = self.report.request_json(
            'place=bids_recommender&batch-bids-recommendations=1&rids=213&feed_shoffer_id=1-1,1-2,1-3'
        )
        self.assertFragmentIn(
            response,
            {
                "position": [
                    {'bid': 2},
                    {'bid': 2},
                    {'bid': 1},
                    {'bid': 1},
                    {'bid': 1},
                    {'bid': 1},
                    {'bid': 1},
                    {'bid': 1},
                    {'bid': 1},
                    {'bid': 1},
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )
        self.assertFragmentIn(
            response,
            {
                "position": [
                    {'bid': 2},
                    {'bid': 2},
                    {'bid': 1},
                    {'bid': 1},
                    {'bid': 1},
                    {'bid': 1},
                    {'bid': 1},
                    {'bid': 1},
                    {'bid': 1},
                    {'bid': 1},
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )
        self.assertFragmentIn(
            response,
            {
                "position": [
                    {'bid': 2},
                    {'bid': 2},
                    {'bid': 1},
                    {'bid': 1},
                    {'bid': 1},
                    {'bid': 1},
                    {'bid': 1},
                    {'bid': 1},
                    {'bid': 1},
                    {'bid': 1},
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )


if __name__ == '__main__':
    main()

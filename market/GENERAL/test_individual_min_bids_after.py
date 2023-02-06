#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import HyperCategory, HyperCategoryType, Model, Offer, Region, Shop
from core.testcase import TestCase, main
from core.types.min_bids import MinBidsCategory, MinBidsModel, MinBidsPriceGroup

MIN_BID2_CARD_1000 = 34  # ceil(0.01 * 0.1 * 10000 / 0.3) = 34
MIN_BID2_CARD_2000 = 67  # ceil(0.01 * 0.1 * 20000 / 0.3) = 67


class T(TestCase):
    """
    Проверяем работу индивидуальных ставок ПОСЛЕ конца перехода
    """

    @classmethod
    def prepare_min_bids2(cls):
        cls.index.creation_time = 1583830800  # Tue Mar 10 12:00:00 STD 2020 , last day of experiment (full on)
        cls.index.regiontree += [
            Region(rid=213, name='Москва'),
        ]

        cls.index.hypertree += [
            HyperCategory(hid=100, output_type=HyperCategoryType.GURU),
        ]
        cls.index.models += [
            Model(hyperid=1000, hid=100),
        ]
        cls.index.shops += [
            Shop(fesh=1000, priority_region=213),
            Shop(fesh=2000, priority_region=213),
        ]
        cls.index.offers += [
            Offer(hid=100, hyperid=1000, price=20000),
            Offer(hid=100, hyperid=1000, price=10000),
        ]
        cls.index.min_bids_category_stats += [
            MinBidsCategory(
                category_id=100,
                geo_group_id=0,
                price_group_id=100,
                drr=0.22,
                search_conversion=0.233,
                card_conversion=1.0,
                full_card_conversion=1.0,
            ),
        ]
        cls.index.min_bids_model_stats += [
            MinBidsModel(
                model_id=1000,
                geo_group_id=0,
                drr=0.01,
                search_clicks=1,
                search_orders=3,
                card_clicks=144,
                card_orders=9,
                full_card_orders=0,
                full_card_clicks=0,
            )
        ]
        cls.index.min_bids_price_groups += [MinBidsPriceGroup(0), MinBidsPriceGroup(100)]

    def test_individual_min_bids_off(self):
        '''
        Выключаем индивидуальные ставки.
        Проверяем, что значение минимального бида не зависит от цены оффера.
        '''
        self.report.request_json(
            'place=productoffers&hyperid=1000&show-urls=external&rearr-factors=market_use_individual_min_bid=0'
        )
        self.show_log.expect(min_bid=MIN_BID2_CARD_1000, price=10000)
        self.show_log.expect(min_bid=MIN_BID2_CARD_1000, price=20000)

    def test_individual_min_bids_on(self):
        '''
        Проверяем, что значение минимального бида зависит целиком от цены оффера.
        '''
        self.report.request_json('place=productoffers&hyperid=1000&show-urls=external')
        self.show_log.expect(min_bid=MIN_BID2_CARD_1000, price=10000)
        self.show_log.expect(min_bid=MIN_BID2_CARD_2000, price=20000)


if __name__ == '__main__':
    main()

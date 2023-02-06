#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import HyperCategory, HyperCategoryType, Model, Offer, Region, Shop
from core.testcase import TestCase, main
from core.types.min_bids import MinBidsCategory, MinBidsModel, MinBidsPriceGroup

# Значение минимальной ставки на карточке модели определяется по формуле:
# drr * conversion * model.min_price / 0.3
# drr - доля рекламных расходов. Берется из статистики по модели (MinBidsModel), а если она там нулевая или
#       статистика для сегмента модели отсутствует, то из статистики по категории (MinBidsCategory)
# conversion - в случае, если есть статистика по модели, то
#       conversion = (card_orders + alpha) / (card_clicks + alpha / category_conversion)
#       alpha в лайтах задана равной 6-ти
#       в случае, если статистика по модели отсутствует, то она равна конверсии категории.
# model.min_price - минимальная цена модели. Лайты определеяют её по цене офферов и запихивают в MRS, откуда её и
#       использует формула.
# 0.3 - делим на это число для перевода из рублей в фишко-центы
#
# Подробности тут: https://wiki.yandex-team.ru/market/money/fairness/dev/#raschetminimalnojjstavki1
#
# При изменении формулы, нужно будет привести константы в соответствие с новой формулой
#
# conversion = (9 + 6) / (144 + 6 / 1) = 0.1
MIN_BID2_CARD = 34  # ceil(0.01 * 0.1 * 10000 / 0.3) = 34
MIN_BID2_CARD_EXPERIMENTAL_DRR_OFF = 4  # ceil(0.005 * 0.02 * 10000 / 0.3) = 4

MIN_BID2_SEARCH_EXPERIMENTAL_DRR = 47  # ceil(0.0350000000000000 * 0.04 * 10000 / 0.3) = 46
MIN_BID2_SEARCH = 7  # ceil(0.005 * 0.04 * 10000 / 0.3) = 6
# conversion = (23 + 6) / (140 + 6 / 0.04) = 0.1
MIN_BID2_SEARCH2 = 67  # ceil(0.02 * 0.1 * 10000 / 0.3) = 66
MIN_BID2_SEARCH2_AUCTION = 101  # bid for offer with min_bid 67 to overbid offer with bid 40 and min_bid 7


class T(TestCase):
    @classmethod
    def prepare_min_bids2(cls):
        cls.settings.default_search_experiment_flags += ["market_money_disable_bids=0"]
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']
        cls.index.regiontree += [
            Region(rid=213, name='Москва'),
        ]

        cls.index.hypertree += [
            HyperCategory(hid=100, output_type=HyperCategoryType.GURU),
            HyperCategory(hid=91596, output_type=HyperCategoryType.GURU),
        ]
        cls.index.models += [
            Model(hyperid=1000, hid=100, title='model 1000'),
            Model(hyperid=1001, hid=91596, title='model 1001'),
            Model(hyperid=1002, hid=91596, title='model 1002'),
        ]
        cls.index.shops += [
            Shop(fesh=1000, priority_region=213),
            Shop(fesh=2000, priority_region=213),
        ]
        cls.index.offers += [
            Offer(title='offer1', hid=100, hyperid=1000, price=20000, bid=1000),
            Offer(title='offer2', hid=100, hyperid=1000, price=10000, bid=900),
            Offer(title='offer3', hid=91596, hyperid=1001, price=10000, bid=100, discount=50),
            Offer(title='offer4 1', fesh=1000, hid=91596, hyperid=1002, price=10000, bid=200),
            Offer(title='offer4 2', fesh=2000, hid=91596, hyperid=1002, price=10000, bid=100),
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
            MinBidsCategory(
                category_id=91596,
                geo_group_id=0,
                price_group_id=100,
                drr=0.005,
                search_conversion=0.04,
                card_conversion=0.02,
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

    def test_min_bids2_is_used_when_new_mode_is_on(self):
        '''
        Запрашиваем плейс productoffers

        Проверяем, что значение минимального бида соответствует новой формуле.

        Примечание: в данном тесте мы не проверяем саму формулу. Формула уже проверена в юнит-тестах.
        '''
        self.report.request_json('place=productoffers&hyperid=1000&show-urls=external')

        self.show_log.expect(min_bid=MIN_BID2_CARD, title="offer1")

    def test_min_bids2_are_used_as_click_price_on_price_sort(self):
        '''
        Запрашиваем плейс productoffers c включенной сортировкой по цене

        Проверяем, что стоимость клика соответствует посчитанному по новой формуле минимальному биду.
        '''
        self.report.request_json('place=productoffers&hyperid=1000&show-urls=external&how=aprice')

        self.show_log.expect(click_price=MIN_BID2_CARD, title="offer1")

    @classmethod
    def prepare_min_bids2_default_offer(cls):
        cls.index.shops += [
            Shop(fesh=1000, priority_region=213, cpa=Shop.CPA_REAL),
        ]
        cls.index.models += [
            Model(hyperid=1010, hid=91596, title='model1010'),
        ]
        cls.index.offers += [
            Offer(title='offer10', fesh=1000, hid=91596, hyperid=1010, price=10000, bid=0),
        ]

    def test_min_bids2_drr_experiment_default_offer(self):
        '''
        Запрашиваем ДО на плейсе productoffers

        Проверяем, что fuid соответствует посчитанному по новой формуле минимальному биду.
        '''
        _ = self.report.request_json('place=productoffers&hyperid=1010&rids=213&offers-set=default&show-urls=external')

        self.show_log.expect(click_price=MIN_BID2_CARD_EXPERIMENTAL_DRR_OFF, title="offer10")

    @classmethod
    def prepare_min_bids2_search(cls):
        cls.index.models += [
            Model(hyperid=2001, hid=91596, title='model 1001'),
            Model(hyperid=2002, hid=91596, title='model 1002'),
        ]
        cls.index.shops += [
            Shop(fesh=2001, priority_region=213),
            Shop(fesh=2002, priority_region=213),
        ]
        cls.index.offers += [
            Offer(title='offer20 1', fesh=2001, hid=91596, hyperid=2001, price=10000, bid=1000),
            Offer(title='offer20 2', fesh=2002, hid=91596, hyperid=2002, price=10000, bid=40),
        ]

    def test_min_bids2_search(self):
        '''
        Запрашиваем плейс prime

        Проверяем, что стоимость клика соответствует посчитанному по новой формуле минимальному биду
        Должна браться карточная конверсия.
        '''
        self.report.request_json('place=prime&text=offer3&show-urls=external')

        self.show_log.expect(click_price=MIN_BID2_CARD_EXPERIMENTAL_DRR_OFF, title="offer3")

    def test_min_bids2_search_auction(self):
        '''
        Запрашиваем плейс prime с включённым экспериментом на поиске

        Проверяем, что ставка расчитана с учётом нового мин бида.
        Проверяем, что порядок офферов НЕ поменялся.
        '''
        rearr = '&rearr-factors=market_disable_auction_for_offers_with_model=0'
        response = self.report.request_json(
            'place=prime&text=offer20&show-urls=external&rids=213&hyperid=2001,2002' + rearr
        )
        self.assertFragmentIn(
            response,
            [
                {"entity": "offer", "titles": {"raw": "offer20 1"}},
                {"entity": "offer", "titles": {"raw": "offer20 2"}},
            ],
            preserve_order=True,
        )
        self.show_log.expect(click_price=41, min_bid=MIN_BID2_CARD_EXPERIMENTAL_DRR_OFF, title="offer20 1")
        self.show_log.expect(
            click_price=MIN_BID2_CARD_EXPERIMENTAL_DRR_OFF,
            min_bid=MIN_BID2_CARD_EXPERIMENTAL_DRR_OFF,
            title="offer20 2",
        )


if __name__ == '__main__':
    main()

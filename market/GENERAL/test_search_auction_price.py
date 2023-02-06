#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    BidSettings,
    CategoryBidSettings,
    CpaCategory,
    CpaCategoryType,
    HyperCategory,
    HyperCategoryType,
    MnPlace,
    Model,
    Offer,
    Shop,
)
from core.testcase import TestCase, main
from core.matcher import Contains, Round
from math import exp


class T(TestCase):
    '''
    market_force_search_auction=PriceExpensive
    market_force_search_auction=Price
    Аукцион по цене PRICE_EXPENSIVE поднимает вверх предложения с большей ценой

        Множитель аукциона вычисляется:
        x = 30*price/avgCategoryPrice
        AUCTION_MULTIPLIER = 1. + alpha * (1. / (gamma * exp(-beta * x) + 1.) - 1. / (1. + gamma))

    Аукцион по цене PRICE_CHEAP поднимает вверх более дешевые предложения

        Множитель аукциона вычисляется:
        x = 10*avgCategoryPrice/price
        AUCTION_MULTIPLIER = 1. + alpha * (1. / (gamma * exp(-beta * x) + 1.) - 1. / (1. + gamma))
    '''

    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ["market_money_disable_bids=0"]
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']
        cls.index.shops += [
            Shop(fesh=1, priority_region=213, regions=[213], name="Cpa shop 1 ", cpa=Shop.CPA_REAL),
            Shop(fesh=2, priority_region=213, regions=[213], name="Cpc shop 2", cpa=Shop.CPA_NO),
            Shop(fesh=3, priority_region=213, regions=[213], name="Cpa shop 3", cpa=Shop.CPA_REAL),
            Shop(fesh=4, priority_region=213, regions=[213], name="Cpc shop 4", cpa=Shop.CPA_NO),
            Shop(fesh=5, priority_region=213, regions=[213], name="Cpa shop 5", cpa=Shop.CPA_REAL),
            Shop(fesh=6, priority_region=213, regions=[213], name="Cpc shop 6", cpa=Shop.CPA_NO),
        ]

        cls.index.hypertree += [
            HyperCategory(hid=100, uniq_name="Модельки", output_type=HyperCategoryType.GURU),
            HyperCategory(hid=200, uniq_name="Дельные предложения", output_type=HyperCategoryType.SIMPLE),
        ]

        cls.index.cpa_categories += [
            CpaCategory(hid=100, fee=100, regions=[225], cpa_type=CpaCategoryType.CPC_AND_CPA),
            CpaCategory(hid=200, fee=200, regions=[225], cpa_type=CpaCategoryType.CPC_AND_CPA),
        ]

        cls.index.category_bid_settings += [
            CategoryBidSettings(category=100, search_settings=BidSettings(coefficient=0.15, power=0.56, maximumBid=10)),
            CategoryBidSettings(category=200, search_settings=BidSettings(coefficient=0.15, power=0.56, maximumBid=20)),
        ]

        cls.index.models += [
            Model(hyperid=101, hid=100, title="Модель смартфона"),
            Model(hyperid=102, hid=100, title="Модель пылесоса"),
        ]

        cls.index.offers += [
            Offer(
                hyperid=101,
                hid=100,
                fesh=1,
                title="Предложение: смартфон за 10000 рублей на Маркете",
                price=10000,
                cpa=Offer.CPA_REAL,
                fee=500,
                randx=1,
            ),
            Offer(
                hyperid=101,
                hid=100,
                fesh=2,
                title="Предложение: китайский смартфон за 100 рублей в переходе",
                price=100,
                cpa=Offer.CPA_NO,
                bid=200,
                randx=2,
            ),
            Offer(
                hyperid=102,
                hid=100,
                fesh=3,
                title="Предложение: робот-пылесос",
                price=5000,
                cpa=Offer.CPA_REAL,
                randx=3,
            ),
            Offer(
                hyperid=102,
                hid=100,
                fesh=4,
                title="Предложение: электровеник",
                price=4000,
                cpa=Offer.CPA_NO,
                randx=4,
            ),
            Offer(
                hid=200,
                fesh=5,
                title="Предложение: 3D телевизор за 5 000 000 руб.",
                price=5000000,
                cpa=Offer.CPA_REAL,
                fee=230,
                randx=5,
            ),
            Offer(
                hid=200,
                fesh=6,
                title="Предложение: бабушкин черно-белый телик за 500р.",
                price=500,
                cpa=Offer.CPA_NO,
                bid=130,
                randx=6,
            ),
        ]

        cls.matrixnet.on_default_place(MnPlace.BASE_SEARCH).respond(0.3)

    def test_price_expensive_without_collapsing(self):
        '''Проверка значений аукциона по цене (вначале дорогие)
        без схлопывания берется цена оффера/среднюю цену в категории
        '''
        auction_cgi = '&rearr-factors=market_force_search_auction=Price;market_tweak_search_auction_price_params=0.13,0.14,5;market_disable_auction_for_offers_with_model=0'
        alpha = 0.13
        beta = 0.14
        gamma = 5.0

        response = self.report.request_json('place=prime&text=Предложение&rids=213&debug=da' + auction_cgi)

        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {"titles": {"raw": "Предложение: смартфон за 10000 рублей на Маркете"}},
                        {"titles": {"raw": "Предложение: 3D телевизор за 5 000 000 руб."}},
                        {"titles": {"raw": "Предложение: робот-пылесос"}},
                        {"titles": {"raw": "Предложение: электровеник"}},
                        {"titles": {"raw": "Предложение: китайский смартфон за 100 рублей в переходе"}},
                        {"titles": {"raw": "Предложение: бабушкин черно-белый телик за 500р."}},
                    ]
                }
            },
            allow_different_len=False,
            preserve_order=True,
        )

        self.assertFragmentIn(
            response,
            {
                "logicTrace": [
                    Contains(
                        'Using search auction with parameters: ',
                        'priceAlpha=0.13 priceBeta=0.14 priceGamma=5 ',
                        'auction type: 3',
                    )
                ]
            },
        )

        x = 30.0 * 10000 / 4775
        multiplier = 1.0 + alpha * (1.0 / (gamma * exp(-beta * x) + 1.0) - 1.0 / (1.0 + gamma))

        self.assertFragmentIn(
            response,
            {
                "titles": {"raw": "Предложение: смартфон за 10000 рублей на Маркете"},
                "debug": {
                    "properties": {
                        "PRICE": "10000",
                        "AVG_CATEGORY_PRICE": "4775",
                        "MATRIXNET_VALUE": Round(0.3),
                        "AUCTION_MULTIPLIER": Round(multiplier, 3),  # 1.108235002
                        "CPM": "33247",  # 33247 = 1.108235002 * 30 000
                    }
                },
            },
        )

        x = 30.0 * 5000000 / 2500250
        multiplier = 1.0 + alpha * (1.0 / (gamma * exp(-beta * x) + 1.0) - 1.0 / (1.0 + gamma))
        self.assertFragmentIn(
            response,
            {
                "titles": {"raw": "Предложение: 3D телевизор за 5 000 000 руб."},
                "debug": {
                    "properties": {
                        "PRICE": "5000000",
                        "AVG_CATEGORY_PRICE": "2500250",
                        "MATRIXNET_VALUE": Round(0.3),
                        "AUCTION_MULTIPLIER": Round(multiplier, 3),  # 1.108187199
                        "CPM": "33245",  # 33245 = 1.108187199 * 30 000
                    }
                },
            },
        )

        x = 30.0 * 4000 / 4775
        multiplier = 1.0 + alpha * (1.0 / (gamma * exp(-beta * x) + 1.0) - 1.0 / (1.0 + gamma))
        self.assertFragmentIn(
            response,
            {
                "titles": {"raw": "Предложение: электровеник"},
                "debug": {
                    "properties": {
                        "DOCUMENT_AUCTION_TYPE": "PRICE_EXPENSIVE",
                        "PRICE": "4000",
                        "AVG_CATEGORY_PRICE": "4775",
                        "MATRIXNET_VALUE": Round(0.3),
                        "AUCTION_MULTIPLIER": Round(multiplier, 3),  # 1.091549516
                        "CPM": "32746",  # 32746 = 1.091549516 * 30 000
                    }
                },
            },
        )

    def test_price_expensive_with_collapsing(self):
        '''Проверка значений аукциона по цене (вначале дорогие)
        со схлопыванием берется минимальная цена модели
        (при разделённой коллекции берётся наиболее релевантный)
        '''

        auction_cgi = '&rearr-factors=market_force_search_auction=PriceExpensive;market_tweak_search_auction_price_params=0.13,0.14,5;market_disable_auction_for_offers_with_model=0'
        alpha = 0.13
        beta = 0.14
        gamma = 5.0

        # модели не бустятся т.к. у нас мультикатегорийная выдача
        response = self.report.request_json(
            'place=prime&text=Предложение&rids=213&allow-collapsing=1&debug=da' + auction_cgi
        )
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {"titles": {"raw": "Предложение: 3D телевизор за 5 000 000 руб."}},
                        {"titles": {"raw": "Модель пылесоса"}},
                        {"titles": {"raw": "Модель смартфона"}},
                        {"titles": {"raw": "Предложение: бабушкин черно-белый телик за 500р."}},
                    ]
                }
            },
            allow_different_len=False,
            preserve_order=True,
        )

        self.assertFragmentIn(
            response,
            {
                "logicTrace": [
                    Contains(
                        'Using search auction with parameters: ',
                        'priceAlpha=0.13 priceBeta=0.14 priceGamma=5 ',
                        'auction type: 3',
                    )
                ]
            },
        )

        x = 30.0 * 4000 / 4775
        multiplier = 1.0 + alpha * (1.0 / (gamma * exp(-beta * x) + 1.0) - 1.0 / (1.0 + gamma))

        # для схлопнутого оффера берется минимальная цена модели
        self.assertFragmentIn(
            response,
            {
                "titles": {"raw": "Модель пылесоса"},
                "debug": {
                    "properties": {
                        "DOCUMENT_AUCTION_TYPE": "PRICE_EXPENSIVE",
                        "PRICE": "4000",
                        "AVG_CATEGORY_PRICE": "4775",
                        "MATRIXNET_VALUE": Round(0.3),
                        "AUCTION_MULTIPLIER": Round(multiplier, 2),  # 1.091549516
                        "CPM": "32746",  # 32746 = 1.091549516 * 30 000
                    }
                },
            },
        )

        # и в том числе в тех случаях когда статистика модели будет обновлена на мете (например из-за фильтра по магазину)
        response = self.report.request_json(
            'place=prime&text=Предложение&rids=213&allow-collapsing=1&debug=da&fesh=3' + auction_cgi
        )
        self.assertFragmentIn(
            response,
            {
                "titles": {"raw": "Модель пылесоса"},
                "prices": {"min": "5000", "max": "5000"},
                "debug": {
                    "properties": {
                        "DOCUMENT_AUCTION_TYPE": "PRICE_EXPENSIVE",
                        "PRICE": "4000",
                        "AVG_CATEGORY_PRICE": "4775",
                        "MATRIXNET_VALUE": Round(0.3),
                        "AUCTION_MULTIPLIER": Round(multiplier, 3),  # 1.091549516
                        "CPM": "32746",  # 32746 = 1.091549516 * 30 000
                    }
                },
            },
        )

    def test_min_bid(self):
        '''Проверяем что списывается минимальная ставка bid'''

        auction_cgi = (
            '&rearr-factors=market_force_search_auction=Price;market_tweak_search_auction_price_params=0.13,0.14,5'
        )
        response = self.report.request_json('place=prime&text=Предложение&rids=213&debug=da' + auction_cgi)

        # cpc-оффер - по настройкам из category_bid_settings вычисляется minBid
        self.assertFragmentIn(
            response,
            {
                "titles": {"raw": "Предложение: бабушкин черно-белый телик за 500р."},
                "categories": [{"id": 200}],
                "debug": {
                    "sale": {
                        "bidType": "mbid",
                        "bid": 130,
                        "minBid": 1,
                        "clickPrice": 1,
                        "brokeredClickPrice": 1,
                        "vBid": 0,
                        "vendorClickPrice": 0,
                    }
                },
            },
        )

    def test_price_cheap_without_collapsing(self):
        '''Проверка значений аукциона по цене (вначале дешевые)
        без схлопывания берется цена оффера/среднюю цену в категории
        '''
        auction_cgi = '&rearr-factors=market_force_search_auction=PriceCheap;market_tweak_search_auction_price_params=0.2,0.25,10;market_disable_auction_for_offers_with_model=0'
        alpha = 0.2
        beta = 0.25
        gamma = 10.0

        response = self.report.request_json('place=prime&text=Предложение&rids=213&debug=da' + auction_cgi)

        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {"titles": {"raw": "Предложение: бабушкин черно-белый телик за 500р."}},
                        {"titles": {"raw": "Предложение: китайский смартфон за 100 рублей в переходе"}},
                        {"titles": {"raw": "Предложение: электровеник"}},
                        {"titles": {"raw": "Предложение: робот-пылесос"}},
                        {"titles": {"raw": "Предложение: 3D телевизор за 5 000 000 руб."}},
                        {"titles": {"raw": "Предложение: смартфон за 10000 рублей на Маркете"}},
                    ]
                }
            },
            allow_different_len=False,
            preserve_order=True,
        )

        self.assertFragmentIn(
            response,
            {
                "logicTrace": [
                    Contains(
                        'Using search auction with parameters: ',
                        'priceAlpha=0.2 priceBeta=0.25 priceGamma=10 ',
                        'auction type: 4',
                    )
                ]
            },
        )

        x = 10.0 * 4775 / 10000
        multiplier = 1.0 + alpha * (1.0 / (gamma * exp(-beta * x) + 1.0) - 1.0 / (1.0 + gamma))

        self.assertFragmentIn(
            response,
            {
                "titles": {"raw": "Предложение: смартфон за 10000 рублей на Маркете"},
                "debug": {
                    "properties": {
                        "DOCUMENT_AUCTION_TYPE": "PRICE_CHEAP",
                        "PRICE": "10000",
                        "AVG_CATEGORY_PRICE": "4775",
                        "MATRIXNET_VALUE": Round(0.3),
                        "AUCTION_MULTIPLIER": Round(multiplier, 3),  # 1.031435847
                        "CPM": "30943",  # 30943 = 1.031435847 * 30 000
                    }
                },
            },
        )

        x = 10.0 * 2500250 / 5000000
        multiplier = 1.0 + alpha * (1.0 / (gamma * exp(-beta * x) + 1.0) - 1.0 / (1.0 + gamma))
        self.assertFragmentIn(
            response,
            {
                "titles": {"raw": "Предложение: 3D телевизор за 5 000 000 руб."},
                "debug": {
                    "properties": {
                        "DOCUMENT_AUCTION_TYPE": "PRICE_CHEAP",
                        "PRICE": "5000000",
                        "AVG_CATEGORY_PRICE": "2500250",
                        "MATRIXNET_VALUE": Round(0.3),
                        "AUCTION_MULTIPLIER": Round(multiplier, 3),  # 1.03356874
                        "CPM": "31007",  # 31007 = 1.03356874 * 30 000
                    }
                },
            },
        )

        x = 10.0 * 4775 / 4000
        multiplier = 1.0 + alpha * (1.0 / (gamma * exp(-beta * x) + 1.0) - 1.0 / (1.0 + gamma))
        self.assertFragmentIn(
            response,
            {
                "titles": {"raw": "Предложение: электровеник"},
                "debug": {
                    "properties": {
                        "DOCUMENT_AUCTION_TYPE": "PRICE_CHEAP",
                        "PRICE": "4000",
                        "AVG_CATEGORY_PRICE": "4775",
                        "MATRIXNET_VALUE": Round(0.3),
                        "AUCTION_MULTIPLIER": Round(multiplier, 3),  # 1.114645839
                        "CPM": "33439",  # 33439 = 1.114645839 * 30 000
                    }
                },
            },
        )

    def test_price_cheap_with_collapsing(self):
        '''Проверка значений аукциона по цене (вначале дешевые)
        со схлопыванием берется минимальная цена модели
        (при разделённой коллекции берётся наиболее релевантный)
        '''

        auction_cgi = '&rearr-factors=market_force_search_auction=PriceCheap;market_tweak_search_auction_price_params=0.2,0.25,10;market_disable_auction_for_offers_with_model=0'
        alpha = 0.2
        beta = 0.25
        gamma = 10.0

        x_telik = 10.0 * 2500250 / 500
        multiplier_telik = 1.0 + alpha * (1.0 / (gamma * exp(-beta * x_telik) + 1.0) - 1.0 / (1.0 + gamma))

        x_smartfon = 10.0 * 4775 / 100
        multiplier_smartfon = 1.0 + alpha * (1.0 / (gamma * exp(-beta * x_smartfon) + 1.0) - 1.0 / (1.0 + gamma))

        # телефон за 100р по отношению к средней цене 4775
        # аболютно также хорош как и телик за 500 по отношению 25002500
        self.assertTrue(abs(multiplier_telik - multiplier_smartfon) < 0.0001)

        # модели не бустятся т.к. у нас мультикатегорийная выдача
        response = self.report.request_json(
            'place=prime&text=Предложение&rids=213&allow-collapsing=1&debug=da' + auction_cgi
        )
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {"titles": {"raw": "Модель смартфона"}},
                        {"titles": {"raw": "Предложение: бабушкин черно-белый телик за 500р."}},
                        {"titles": {"raw": "Модель пылесоса"}},
                        {"titles": {"raw": "Предложение: 3D телевизор за 5 000 000 руб."}},
                    ]
                }
            },
            allow_different_len=False,
            preserve_order=True,
        )

        self.assertFragmentIn(
            response,
            {
                "logicTrace": [
                    Contains(
                        'Using search auction with parameters: ',
                        'priceAlpha=0.2 priceBeta=0.25 priceGamma=10 ',
                        'auction type: 4',
                    )
                ]
            },
        )

        x = 10.0 * 4775 / 4000
        multiplier = 1.0 + alpha * (1.0 / (gamma * exp(-beta * x) + 1.0) - 1.0 / (1.0 + gamma))

        # для схлопнутого оффера берется минимальная цена модели
        self.assertFragmentIn(
            response,
            {
                "titles": {"raw": "Модель пылесоса"},
                "debug": {
                    "properties": {
                        "DOCUMENT_AUCTION_TYPE": "PRICE_CHEAP",
                        "PRICE": "4000",
                        "AVG_CATEGORY_PRICE": "4775",
                        "MATRIXNET_VALUE": Round(0.3),
                        "AUCTION_MULTIPLIER": Round(multiplier, 3),
                        "CPM": "33439",  # multiplier * mn_value * 30 000
                    }
                },
            },
        )

        # и в том числе в тех случаях когда статистика модели будет обновлена на мете (например из-за фильтра по магазину)
        response = self.report.request_json(
            'place=prime&text=Предложение&rids=213&allow-collapsing=1&debug=da&fesh=3' + auction_cgi
        )
        self.assertFragmentIn(
            response,
            {
                "titles": {"raw": "Модель пылесоса"},
                "prices": {"min": "5000", "max": "5000"},
                "debug": {
                    "properties": {
                        "DOCUMENT_AUCTION_TYPE": "PRICE_CHEAP",
                        "PRICE": "4000",
                        "AVG_CATEGORY_PRICE": "4775",
                        "MATRIXNET_VALUE": Round(0.3),
                        "AUCTION_MULTIPLIER": Round(multiplier, 3),
                        "CPM": "33439",  # multiplier * 30 000
                    }
                },
            },
        )


if __name__ == '__main__':
    main()

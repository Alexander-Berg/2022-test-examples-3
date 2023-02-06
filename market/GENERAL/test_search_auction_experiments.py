#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa

from core.types import (
    BidSettings,
    CategoryBidSettings,
    Currency,
    ExchangeRate,
    HyperCategory,
    MinBidsCategory,
    MinBidsModel,
    MnPlace,
    Model,
    Offer,
    Shop,
)
from core.testcase import TestCase, main
from core.matcher import Round, Absent

from math import ceil, pow, exp


FROM_RUR_TO_UE = 0.0333333
CENTS_IN_FISHKA = 100
DRR = 0.014
CONVERSION = 0.04
CARD_CONVERSION = 0.09
MODEL_DRR = 0.021
COEFFICIENT = 0.245
CARD_COEFFICIENT = 0.311
POWER = 0.25
CARD_POWER = 0.3

RELEVANCE_MULTIPLIER = 100000

ZATIROCHKA_PRICE = 1000
KUKOZHECHKA_PRICE = 700

KUKOZHECHKA_1_BID = 20
KUKOZHECHKA_1_REL = 0.65
KUKOZHECHKA_2_BID = 21
KUKOZHECHKA_2_REL = 0.64
KUKOZHECHKA_3_BID = 22
KUKOZHECHKA_3_REL = 0.63
KUKOZHECHKA_4_BID = 23
KUKOZHECHKA_4_REL = 0.62

KUKOZHECHKA_5_BID = 90
KUKOZHECHKA_6_BID = 10

KUKOZHECHKA_5_META_REL = 0.4
KUKOZHECHKA_6_META_REL = 0.401


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ["market_money_disable_bids=0"]
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']
        """
        Общее в prepare: завести такую категорию и валюту, чтобы зафиксировать
        в тесте минставку
        """
        cls.index.currencies += [
            Currency(name=Currency.UE, exchange_rates=[ExchangeRate(fr=Currency.RUR, rate=FROM_RUR_TO_UE)])
        ]

        cls.index.min_bids_category_stats += [
            MinBidsCategory(
                category_id=1,
                geo_group_id=0,
                price_group_id=0,
                drr=DRR,
                search_conversion=CONVERSION,
                card_conversion=CARD_CONVERSION,
                full_card_conversion=1.0,
            )
        ]

        cls.index.category_bid_settings += [
            CategoryBidSettings(
                category=1,
                search_settings=BidSettings(coefficient=COEFFICIENT, power=POWER, maximumBid=100),
                card_settings=BidSettings(coefficient=CARD_COEFFICIENT, power=CARD_POWER, maximumBid=100),
            )
        ]

    @classmethod
    def prepare_use_min_bid_and_write_cp_to_fuid(cls):
        cls.index.shops += [Shop(fesh=1, regions=[213]), Shop(fesh=2, regions=[213])]

        # Второй оффер нужен, чтобы у первого не получилась минставка по дефолту
        cls.index.offers += [
            Offer(fesh=1, title="затирочка 1", hid=1, price=ZATIROCHKA_PRICE, bid=4, offerid=1234),
            Offer(fesh=2, title="затирочка 2", hid=1, price=ZATIROCHKA_PRICE, bid=3, offerid=5678),
        ]

    def test_use_min_bid_and_write_cp_to_fuid(self):
        """
        Проверяем, что под флагом market_use_min_bid_on_search в качестве click_price
        на поиске используется min_bid, а под флагом market_write_click_price_to_fuid
        исходная цена клика независимо от market_use_min_bid_on_search пишется в fuid
        """
        min_bid_rur = DRR * CONVERSION * ZATIROCHKA_PRICE
        min_bid = ceil(min_bid_rur * FROM_RUR_TO_UE * CENTS_IN_FISHKA)

        exp_flags = "market_use_min_bid_on_search=%d;" "market_write_click_price_to_fuid=%d"

        def run(use_min_bid, write_cp_to_fuid, reqid):
            return self.report.request_json(
                "rids=213&place=prime&text=затирочка&rearr-factors={}&reqid={}&debug=da".format(
                    exp_flags % (use_min_bid, write_cp_to_fuid), reqid
                )
            )

        run(use_min_bid=0, write_cp_to_fuid=0, reqid="1")
        self.show_log.expect(title="затирочка 1", min_bid=Round(min_bid, 2), click_price=4, fuid=None, reqid="1")
        self.click_log.expect(offer_id=1234, min_bid=Round(min_bid, 2), cp=4, fuid=None, reqid="1")

        run(use_min_bid=1, write_cp_to_fuid=0, reqid="2")
        self.show_log.expect(
            title="затирочка 1", min_bid=Round(min_bid, 2), click_price=Round(min_bid, 2), fuid=None, reqid="2"
        )
        self.click_log.expect(offer_id=1234, min_bid=Round(min_bid, 2), cp=Round(min_bid, 2), fuid=None, reqid="2")

        run(use_min_bid=0, write_cp_to_fuid=1, reqid="3")
        self.show_log.expect(
            title="затирочка 1", min_bid=Round(min_bid, 2), click_price=4, fuid="cp=4;vcp=0", reqid="3"
        )
        self.click_log.expect(offer_id=1234, min_bid=Round(min_bid, 2), cp=4, fuid="cp=4;vcp=0", reqid="3")

        run(use_min_bid=1, write_cp_to_fuid=1, reqid="4")
        self.show_log.expect(
            title="затирочка 1", min_bid=Round(min_bid, 2), click_price=Round(min_bid, 2), fuid="cp=4;vcp=0", reqid="4"
        )
        self.click_log.expect(
            offer_id=1234, min_bid=Round(min_bid, 2), cp=Round(min_bid, 2), fuid="cp=4;vcp=0", reqid="4"
        )

    @classmethod
    def prepare_disable_auction_for_offers_with_model(cls):
        cls.index.shops += [
            Shop(fesh=3, regions=[213]),
            Shop(fesh=4, regions=[213]),
            Shop(fesh=5, regions=[213]),
        ]

        for model_id in (1, 2, 3):
            cls.index.models += [
                Model(hyperid=model_id, hid=1),
            ]

            cls.index.min_bids_model_stats += [
                MinBidsModel(
                    model_id=model_id,
                    geo_group_id=0,
                    drr=MODEL_DRR,
                    search_clicks=1,
                    search_orders=3,
                    card_clicks=4,
                    card_orders=9,
                    full_card_clicks=0,
                    full_card_orders=0,
                )
            ]

        cls.index.offers += [
            Offer(
                title="кукожечка с моделью 1",
                bid=KUKOZHECHKA_1_BID,
                ts=1,
                hid=1,
                fesh=3,
                price=KUKOZHECHKA_PRICE,
                hyperid=1,
            ),
            Offer(
                title="кукожечка с моделью 2",
                bid=KUKOZHECHKA_2_BID,
                ts=2,
                hid=1,
                fesh=3,
                price=KUKOZHECHKA_PRICE,
                hyperid=2,
            ),
            Offer(title="кукожечка без модели 3", bid=KUKOZHECHKA_3_BID, ts=3, hid=1, fesh=3, price=KUKOZHECHKA_PRICE),
            Offer(title="кукожечка без модели 4", bid=KUKOZHECHKA_4_BID, ts=4, hid=1, fesh=3, price=KUKOZHECHKA_PRICE),
            Offer(
                title="кукожечка с моделью 5",
                bid=KUKOZHECHKA_5_BID,
                ts=5,
                hid=1,
                fesh=4,
                price=KUKOZHECHKA_PRICE,
                hyperid=3,
            ),
            Offer(title="кукожечка без модели 6", bid=KUKOZHECHKA_6_BID, ts=6, hid=1, fesh=5, price=KUKOZHECHKA_PRICE),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1).respond(KUKOZHECHKA_1_REL)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 2).respond(KUKOZHECHKA_2_REL)

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 3).respond(KUKOZHECHKA_3_REL)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 4).respond(KUKOZHECHKA_4_REL)

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 5).respond(0.8)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 6).respond(0.8)

        cls.matrixnet.on_place(MnPlace.META_REARRANGE, 5).respond(KUKOZHECHKA_5_META_REL)
        cls.matrixnet.on_place(MnPlace.META_REARRANGE, 6).respond(KUKOZHECHKA_6_META_REL)

    def test_disable_auction_for_offers_with_model(self):
        """
        Проверяем, что влияние аукциона на ранжирование на базовом и
        переранжирование на мете отключено для офферов с hyperid.
        Проверяем, что обратный флаг включает его обратно
        """
        cpc_alpha, cpc_beta, cpc_gamma = 0.21, 0.22, 0.23

        def multiplier(bid, min_bid):
            return 1.0 + cpc_alpha * (
                1.0 / (cpc_gamma * exp(-cpc_beta * (bid - min_bid)) + 1.0) - 1.0 / (cpc_gamma + 1.0)
            )

        # Минбиды на ранжировании на базовом зависят от настроек для категории
        with_model_min_bid_for_ranking = ceil(pow(KUKOZHECHKA_PRICE, CARD_POWER) * CARD_COEFFICIENT)
        no_model_min_bid_for_ranking = ceil(pow(KUKOZHECHKA_PRICE, POWER) * COEFFICIENT)

        multiplier_1 = multiplier(KUKOZHECHKA_1_BID, with_model_min_bid_for_ranking)
        multiplier_2 = multiplier(KUKOZHECHKA_2_BID, with_model_min_bid_for_ranking)
        multiplier_3 = multiplier(KUKOZHECHKA_3_BID, no_model_min_bid_for_ranking)
        multiplier_4 = multiplier(KUKOZHECHKA_4_BID, no_model_min_bid_for_ranking)

        # Для первых двух офферов (с моделями) посчитаем отдельно cpm с учётом ставки и без
        cpm_1_no_bid = RELEVANCE_MULTIPLIER * KUKOZHECHKA_1_REL
        cpm_1 = cpm_1_no_bid * multiplier_1
        cpm_2_no_bid = RELEVANCE_MULTIPLIER * KUKOZHECHKA_2_REL
        cpm_2 = cpm_2_no_bid * multiplier_2

        cpm_3 = RELEVANCE_MULTIPLIER * KUKOZHECHKA_3_REL * multiplier_3
        cpm_4 = RELEVANCE_MULTIPLIER * KUKOZHECHKA_4_REL * multiplier_4

        # Минбиды на мете зависят от статистик по категориям и пересчитываются в у.е.
        with_model_min_bid_for_meta_rur = MODEL_DRR * CARD_CONVERSION * KUKOZHECHKA_PRICE
        with_model_min_bid_for_meta = ceil(with_model_min_bid_for_meta_rur * FROM_RUR_TO_UE * CENTS_IN_FISHKA)

        no_model_min_bid_for_meta_rur = DRR * CONVERSION * KUKOZHECHKA_PRICE
        no_model_min_bid_for_meta = ceil(no_model_min_bid_for_meta_rur * FROM_RUR_TO_UE * CENTS_IN_FISHKA)

        cpms_meta_5_no_bid = KUKOZHECHKA_5_META_REL * RELEVANCE_MULTIPLIER
        cpms_meta_5 = cpms_meta_5_no_bid * multiplier(KUKOZHECHKA_5_BID, no_model_min_bid_for_meta)

        cpms_meta_6 = (
            KUKOZHECHKA_6_META_REL * RELEVANCE_MULTIPLIER * multiplier(KUKOZHECHKA_6_BID, with_model_min_bid_for_meta)
        )

        auction_settings = "market_tweak_search_auction_params=%.2f,%.2f,%.2f" % (cpc_alpha, cpc_beta, cpc_gamma)

        # 1. С обратным флагом

        rearr_factors = ';'.join(
            [auction_settings, "market_max_pics_duplicates_count=10", "market_disable_auction_for_offers_with_model=0"]
        )
        response = self.report.request_json(
            "place=prime&rids=213&text=кукожечка&numdoc=20&debug=da&rearr-factors={}".format(rearr_factors)
        )

        # 1.1. Базовый

        self.assertTrue(cpm_1 > cpm_2 > cpm_3 > cpm_4)
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "titles": {"raw": "кукожечка с моделью 1"},
                        "debug": {
                            "properties": {"CPM": Round(cpm_1, -1), "AUCTION_MULTIPLIER": Round(multiplier_1, 2)}
                        },
                    },
                    {
                        "titles": {"raw": "кукожечка с моделью 2"},
                        "debug": {
                            "properties": {"CPM": Round(cpm_2, -1), "AUCTION_MULTIPLIER": Round(multiplier_2, 2)}
                        },
                    },
                    {
                        "titles": {"raw": "кукожечка без модели 3"},
                        "debug": {
                            "properties": {"CPM": Round(cpm_3, -1), "AUCTION_MULTIPLIER": Round(multiplier_3, 2)}
                        },
                    },
                    {
                        "titles": {"raw": "кукожечка без модели 4"},
                        "debug": {
                            "properties": {"CPM": Round(cpm_4, -1), "AUCTION_MULTIPLIER": Round(multiplier_4, 2)}
                        },
                    },
                ]
            },
            preserve_order=True,
        )

        # 1.2. Мета

        self.assertTrue(cpms_meta_5 > cpms_meta_6)
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "titles": {"raw": "кукожечка с моделью 5"},
                    },
                    {
                        "titles": {"raw": "кукожечка без модели 6"},
                    },
                ]
            },
            preserve_order=True,
        )

        # 2. Без флага

        rearr_factors = ';'.join([auction_settings, "market_max_pics_duplicates_count=10"])
        response = self.report.request_json(
            "place=prime&rids=213&text=кукожечка&numdoc=20&debug=da&rearr-factors={}".format(rearr_factors)
        )

        # 2.1. Базовый

        self.assertTrue(cpm_3 > cpm_1_no_bid > cpm_4 > cpm_2_no_bid)
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "titles": {"raw": "кукожечка без модели 3"},
                        "debug": {
                            "properties": {"CPM": Round(cpm_3, -1), "AUCTION_MULTIPLIER": Round(multiplier_3, 2)}
                        },
                    },
                    {
                        "titles": {"raw": "кукожечка с моделью 1"},
                        "debug": {"properties": {"CPM": Round(cpm_1_no_bid, -1), "AUCTION_MULTIPLIER": Absent()}},
                    },
                    {
                        "titles": {"raw": "кукожечка без модели 4"},
                        "debug": {
                            "properties": {"CPM": Round(cpm_4, -1), "AUCTION_MULTIPLIER": Round(multiplier_4, 2)}
                        },
                    },
                    {
                        "titles": {"raw": "кукожечка с моделью 2"},
                        "debug": {"properties": {"CPM": Round(cpm_2_no_bid, -1), "AUCTION_MULTIPLIER": Absent()}},
                    },
                ]
            },
            preserve_order=True,
        )

        # 2.2. Мета

        self.assertTrue(cpms_meta_6 > cpms_meta_5_no_bid)
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "titles": {"raw": "кукожечка без модели 6"},
                    },
                    {
                        "titles": {"raw": "кукожечка с моделью 5"},
                    },
                ]
            },
            preserve_order=True,
        )

    @classmethod
    def prepare_models_auction(cls):

        cls.index.hypertree += [HyperCategory(hid=236, name='Кони мои кони')]
        cls.index.models += [
            Model(hyperid=23492102, hid=236, title='Лошадка-Коняшка со ставками 50, 10', ts=849330001, randx=1),
            Model(hyperid=23923894, hid=236, title='Лошадка-Коняшка без ставок', ts=849330002, randx=2),
        ]

        cls.index.shops += [
            Shop(fesh=93833, priority_region=14, regions=[14]),
            Shop(fesh=93834, priority_region=14, regions=[14]),
            Shop(fesh=93835, priority_region=14, regions=[14]),
            Shop(fesh=93836, priority_region=14, regions=[14]),
            Shop(fesh=93837, priority_region=14, regions=[14]),
            Shop(fesh=93838, priority_region=14, regions=[14]),
            Shop(fesh=93839, priority_region=14, regions=[14]),
        ]

        cls.index.offers += [
            Offer(
                hyperid=23492102,
                hid=236,
                fesh=93833,
                bid=50,
                title='Коняшка-Поняшка со ставкой 50',
                ts=849330003,
                randx=3,
            ),
            Offer(
                hyperid=23492102,
                hid=236,
                fesh=93834,
                bid=10,
                title='Коняшка-Поняшка со ставкой 10',
                ts=849330004,
                randx=4,
            ),
            Offer(hyperid=23923894, hid=236, fesh=93835, title='Коняшка-Поняшка без ставки', ts=849330005, randx=5),
            Offer(hyperid=23923894, hid=236, fesh=93836, title='Коняшка-Поняшка без ставки', ts=849330006, randx=6),
            Offer(hid=236, fesh=93837, bid=80, title='Лошадка-Поняшка со ставкой 80', ts=849330007, randx=7),
            Offer(hid=236, fesh=93838, bid=20, title='Лошадка-Поняшка со ставкой 20', ts=849330008, randx=8),
            Offer(hid=236, fesh=93839, title='Лошадка-Поняшка без ставок', ts=849330009, randx=9),
        ]

    def test_zero_model_bids_for_cpc_auction(self):
        '''ставка shopBid для модели (несхлопнутой) равна 0'''

        # По умолчанию ставка shopBid для моделей становится равной 0 и они имеют auctionMultiplier=1
        response = self.report.request_json(
            'place=prime&text=лошадка&rids=14&allow-collapsing=1&debug=da&rearr-factors=market_force_search_auction=Cpc'
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'entity': 'offer',
                        'titles': {'raw': 'Лошадка-Поняшка со ставкой 80'},
                        'debug': {'tech': {'auctionMultiplier': Round(1.109)}, 'sale': {'bid': 80}},
                    },
                    {
                        'entity': 'offer',
                        'titles': {'raw': 'Лошадка-Поняшка со ставкой 20'},
                        'debug': {'tech': {'auctionMultiplier': Round(1.024)}, 'sale': {'bid': 20}},
                    },
                    {
                        'entity': 'offer',
                        'titles': {'raw': 'Лошадка-Поняшка без ставок'},
                        'debug': {'tech': {'auctionMultiplier': Round(1.011)}, 'sale': {'bid': 10}},
                    },
                    {
                        'entity': 'product',
                        'titles': {'raw': 'Лошадка-Коняшка без ставок'},
                        'debug': {'isCollapsed': False, 'tech': {'auctionMultiplier': "1"}, 'sale': {'bid': 0}},
                    },
                    {
                        'entity': 'product',
                        'titles': {'raw': 'Лошадка-Коняшка со ставками 50, 10'},
                        'debug': {'isCollapsed': False, 'tech': {'auctionMultiplier': "1"}, 'sale': {'bid': 0}},
                    },
                ]
            },
            allow_different_len=False,
            preserve_order=True,
        )

    def test_auction_bids_for_collapsed_models(self):
        '''Для схлопнутых моделей берется bid равный bid-у схлопнутого оффера'''

        expected = {
            'entity': 'product',
            'titles': {'raw': 'Лошадка-Коняшка со ставками 50, 10'},
            'debug': {
                'isCollapsed': True,
                'offerTitle': "Коняшка-Поняшка со ставкой 10",
                'tech': {'auctionMultiplier': Round(0)},
                'sale': {'bid': 10},
            },
        }

        response = self.report.request_json(
            'place=prime&text=поняшка&rids=14&allow-collapsing=1&debug=da&rearr-factors=market_force_search_auction=Cpc'
        )
        self.assertFragmentIn(response, expected)


if __name__ == '__main__':
    main()

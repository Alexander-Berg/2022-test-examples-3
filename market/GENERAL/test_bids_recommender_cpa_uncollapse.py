#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    BlueOffer,
    CpaCategory,
    CpaCategoryType,
    Currency,
    DeliveryBucket,
    DeliveryOption,
    Elasticity,
    ExchangeRate,
    HybridAuctionParam,
    MarketSku,
    MinBidsCategory,
    MinBidsModel,
    MnPlace,
    Model,
    Offer,
    Region,
    RegionalDelivery,
    Shop,
)
from core.testcase import TestCase, main
from core.matcher import NotEmpty

import re


class T(TestCase):
    title_regex = re.compile(r'(<raw-title>)[^<]*(</raw-title>)')

    @classmethod
    def optional_title(cls, title):
        return title if not cls.settings.disable_snippet_request else ''

    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ["market_money_disable_bids=0"]
        cls.matrixnet.on_default_place(MnPlace.BASE_SEARCH).respond(0.05)

        cls.index.hybrid_auction_settings += [
            HybridAuctionParam(
                category=90401,
                cpc_ctr_for_cpc=0.1,
                cpc_ctr_for_cpc_msk=0.1,
            )
        ]

        cls.index.regiontree += [Region(rid=213, region_type=Region.CITY)]

        # cls.index.hypertree += [
        #     HyperCategory(hid=101, output_type=HyperCategoryType.GURU)
        # ]

        cls.index.currencies += [
            Currency(name=Currency.UE, exchange_rates=[ExchangeRate(fr=Currency.RUR, rate=0.0333333)])
        ]

        cls.index.cpa_categories += [
            CpaCategory(hid=900000, regions=[213], cpa_type=CpaCategoryType.CPC_AND_CPA),
        ]

        cls.index.shops += [
            Shop(fesh=9000, priority_region=213, regions=[213]),
            Shop(fesh=9001, priority_region=213, regions=[213]),
            Shop(fesh=9002, priority_region=213, regions=[213]),
            Shop(
                fesh=10002,
                datafeed_id=10002,
                priority_region=213,
                regions=[213],
                blue=Shop.BLUE_REAL,
                supplier_type=Shop.THIRD_PARTY,
                warehouse_id=145,
                name="3P-Магазин 10002",
                business_fesh=1,
            ),
            Shop(
                fesh=10003,
                datafeed_id=10003,
                priority_region=213,
                regions=[213],
                blue=Shop.BLUE_REAL,
                supplier_type=Shop.THIRD_PARTY,
                warehouse_id=145,
                name="3P-Магазин 10003",
                business_fesh=2,
            ),
            Shop(
                fesh=10004,
                datafeed_id=10004,
                priority_region=213,
                regions=[213],
                blue=Shop.BLUE_REAL,
                supplier_type=Shop.THIRD_PARTY,
                warehouse_id=145,
                name="3P-Магазин 10004",
                business_fesh=3,
            ),
            Shop(fesh=11001, priority_region=213, cpa=Shop.CPA_REAL, name="DSBS shop"),
        ]

        cls.index.models += [
            Model(hid=900000, hyperid=900001),
            # Model(hid=900000, hyperid=900002),
        ]

        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=1234,
                carriers=[157],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
                regional_options=[
                    RegionalDelivery(rid=225, options=[DeliveryOption(price=45, day_from=3, day_to=5)]),
                ],
            ),
            DeliveryBucket(
                bucket_id=418,
                fesh=16,
                carriers=[99],
                regional_options=[
                    RegionalDelivery(
                        rid=213,
                        options=[
                            DeliveryOption(day_from=0, day_to=15, order_before=6, shop_delivery_price=140),
                        ],
                    )
                ],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
        ]

        elasticity = [
            Elasticity(price_variant=100, demand_mean=20),
            Elasticity(price_variant=110, demand_mean=10),
            Elasticity(price_variant=120, demand_mean=5),
        ]

        cls.index.mskus += [
            MarketSku(
                hid=900000,
                sku=100003,
                hyperid=900001,
                delivery_buckets=[1234],
                blue_offers=[
                    BlueOffer(
                        title="bunny blue 2",
                        price=115,
                        feedid=10003,
                        offerid=10003,
                        fesh=10003,
                        bid=113,
                        business_id=2,
                        waremd5="BLUE-100003-FEED-10003",
                    ),
                    BlueOffer(
                        title="bunny blue 1",
                        price=130,
                        feedid=10002,
                        offerid=10002,
                        fesh=10002,
                        bid=115,
                        business_id=1,
                        waremd5="BLUE-100002-FEED-10002",
                    ),
                    BlueOffer(
                        title="bunny blue 3",
                        price=120,
                        feedid=10004,
                        offerid=10004,
                        fesh=10004,
                        bid=8,
                        business_id=3,
                        waremd5="BLUE-100004-FEED-10004",
                    ),
                ],
                buybox_elasticity=elasticity,
            )
        ]

        cls.index.offers += [
            Offer(
                title="bunny",
                hid=900000,
                hyperid=900001,
                bid=10,
                price=110,
                fesh=9000,
                feedid=9000,
                offerid=9001,
                business_id=4,
            ),
            Offer(
                title="bunny zayts",
                hid=900000,
                hyperid=900001,
                bid=11,
                price=110,
                fesh=9001,
                feedid=9001,
                offerid=9001,
                business_id=5,
            ),
            Offer(
                title="bunny zayts krolik",
                hid=900000,
                hyperid=900001,
                bid=12,
                price=110,
                fesh=9002,
                feedid=9002,
                offerid=9001,
                business_id=6,
            ),
            Offer(
                title="bunny dsbs",
                hid=900000,
                hyperid=900001,
                bid=20,
                price=110,
                fesh=11001,
                feedid=11001,
                offerid=11001,
                cpa=Offer.CPA_REAL,
                delivery_buckets=[418],
                business_id=7,
            ),
        ]

        cls.index.min_bids_category_stats += [
            MinBidsCategory(
                category_id=1,
                geo_group_id=0,
                price_group_id=0,
                drr=0.0095,
                search_conversion=0.04,
                card_conversion=0.04,
                full_card_conversion=0.05,
            ),
            MinBidsCategory(
                category_id=90401,
                geo_group_id=0,
                price_group_id=0,
                drr=0.0095,
                search_conversion=0.04,
                card_conversion=0.04,
                full_card_conversion=0.05,
            ),
            MinBidsCategory(
                category_id=1,
                geo_group_id=1,
                price_group_id=0,
                drr=0.0095,
                search_conversion=0.04,
                card_conversion=0.04,
                full_card_conversion=0.05,
            ),
            MinBidsCategory(
                category_id=90401,
                geo_group_id=1,
                price_group_id=0,
                drr=0.0095,
                search_conversion=0.04,
                card_conversion=0.04,
                full_card_conversion=0.05,
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
                full_card_clicks=0,
                full_card_orders=0,
            )
        ]

    # default
    def test_card_output_without_uncollapsing(self):
        """
        Проверяем батч запрос для карточных офферов. Tип выдачи JSON. MARKETOUT-28173
        """
        response = self.report.request_json(
            'place=bids_recommender&batch-bids-recommendations=1&rids=213&type=card&feed_shoffer_id=9000-9001&bsformat=2'
            '&rearr-factors=market_buybox_by_supplier_on_white=0;use_offer_type_priority_as_main_factor_in_top=0;'
            'market_ranging_cpa_by_ue_in_top=0;market_uncollapse_supplier=0;'
        )

        self.assertFragmentIn(
            response,
            {
                "searchResults": {
                    "bidsRecommendations": [
                        {
                            "feedShofferId": "9000-9001",
                            "offer": {
                                "rawTitle": self.optional_title("bunny"),
                                "url": NotEmpty(),
                                "hyperId": 900001,
                                "hid": 900000,
                                "wareMd5": NotEmpty(),
                                "price": {"currency": "RUR", "value": "110"},
                                "bids": {"bid": 10, "pulToMinBid": True},
                                "cpcEnabled": True,
                                "cpaEnabled": False,
                                "qualityRating": 0.6,
                            },
                            "recommendations": {
                                "minBid": 1,
                                "cardPriceRecommendations": {
                                    "currentPosAll": 5,
                                    "topOffersCount": 4,
                                    "position": [
                                        {"bid": 114, "code": 0},
                                        {"bid": 21, "code": 0},
                                        {"bid": 13, "code": 0},
                                        {"bid": 12, "code": 0},
                                    ],
                                },
                                "cardRecommendations": {
                                    "currentPosAll": 5,
                                    "topOffersCount": 4,
                                    "position": [
                                        {"bid": 114, "code": 0},
                                        {"bid": 21, "code": 0},
                                        {"bid": 13, "code": 0},
                                        {"bid": 12, "code": 0},
                                    ],
                                },
                                "cardTopRecommendations": {
                                    "currentPosAll": 5,
                                    "topOffersCount": 4,
                                    "position": [
                                        {"bid": 114, "code": 0},
                                        {"bid": 21, "code": 0},
                                        {"bid": 13, "code": 0},
                                        {"bid": 12, "code": 0},
                                    ],
                                },
                            },
                        }
                    ]
                }
            },
        )

    def test_card_output_with_uncollapsing(self):
        """
        Проверяем батч запрос для карточных офферов. Tип выдачи JSON. MARKETOUT-28173
        """

        def test_body(changing_group_flag):
            response = self.report.request_json(
                'place=bids_recommender&batch-bids-recommendations=1&rids=213&type=card&grhow=offer&feed_shoffer_id=9000-9001&bsformat=2'
                '&rearr-factors=market_ranging_cpa_by_ue_in_top=0;market_buybox_by_supplier_on_white=1;use_offer_type_priority_as_main_factor_in_top=0;'
                'market_uncollapse_supplier=1;market_ranging_cpa_by_ue_in_top=0;enable_business_id=1;{flag};'.format(
                    flag=changing_group_flag
                )
            )
            self.assertFragmentIn(
                response,
                {
                    "searchResults": {
                        "bidsRecommendations": [
                            {
                                "feedShofferId": "9000-9001",
                                "offer": {
                                    "rawTitle": self.optional_title("bunny"),
                                    "url": NotEmpty(),
                                    "hyperId": 900001,
                                    "hid": 900000,
                                    "wareMd5": NotEmpty(),
                                    "price": {"currency": "RUR", "value": "110"},
                                    "bids": {"bid": 10, "pulToMinBid": True},
                                    "cpcEnabled": True,
                                    "cpaEnabled": False,
                                    "qualityRating": 0.6,
                                },
                                "recommendations": {
                                    "minBid": 1,
                                    "cardPriceRecommendations": {
                                        "topOffersCount": 6,
                                        "position": [
                                            {"bid": 116, "code": 0},
                                            {"bid": 114, "code": 0},
                                            {"bid": 21, "code": 0},
                                            {"bid": 13, "code": 0},
                                            {"bid": 12, "code": 0},
                                            {"bid": 9, "code": 0},
                                        ],
                                    },
                                    "cardRecommendations": {
                                        "topOffersCount": 6,
                                        "position": [
                                            {"bid": 116, "code": 0},
                                            {"bid": 114, "code": 0},
                                            {"bid": 21, "code": 0},
                                            {"bid": 13, "code": 0},
                                            {"bid": 12, "code": 0},
                                            {"bid": 9, "code": 0},
                                        ],
                                    },
                                    "cardTopRecommendations": {
                                        "topOffersCount": 6,
                                        "position": [
                                            {"bid": 116, "code": 0},
                                            {"bid": 114, "code": 0},
                                            {"bid": 21, "code": 0},
                                            {"bid": 13, "code": 0},
                                            {"bid": 12, "code": 0},
                                            {"bid": 9, "code": 0},
                                        ],
                                    },
                                },
                            }
                        ]
                    }
                },
            )

        flag = ["market_enable_buybox_by_business=0", "market_enable_buybox_by_business=1"]
        for changing_group_flag in flag:
            test_body(changing_group_flag)

    def test_blue_offers_without_priority(self):
        """
        Проверяем запрос ставок с выключенным флагом use_offer_type_priority_as_main_factor_in_top
        Синие офферы и DSBS представлены в списке
        """

        response = self.report.request_json(
            'place=bids_recommender&fesh=9000&rids=213&hyperid=900001&bsformat=2'
            '&rearr-factors=use_offer_type_priority_as_main_factor_in_top=0;'
            'market_ranging_cpa_by_ue_in_top=0;market_ranging_cpa_by_ue_in_top=0;market_uncollapse_supplier=1;market_ranging_cpa_by_ue_in_top=0;'
        )

        self.assertFragmentIn(
            response,
            {
                'cardPriceRecommendations': {
                    'position': [
                        {'bid': 116, 'code': 0},
                        {'bid': 114, 'code': 0},
                        {'bid': 21, 'code': 0},
                        {'bid': 13, 'code': 0},
                        {'bid': 12, 'code': 0},
                        {'bid': 9, 'code': 0},
                    ]
                }
            },
            preserve_order=True,
        )

    def test_blue_offers_with_priority(self):
        """
        Проверяем запрос ставок с включенным флагом use_offer_type_priority_as_main_factor_in_top
        Синие офферы и DSBS занимают верхние строчки с кодом 2. Эти строки недоступны для cpc офферов
        """

        response = self.report.request_json(
            'place=bids_recommender&fesh=9000&rids=213&hyperid=900001&bsformat=2&rearr-factors=use_offer_type_priority_as_main_factor_in_top=1;market_uncollapse_supplier=1'
        )

        self.assertFragmentIn(
            response,
            {
                'cardPriceRecommendations': {
                    'position': [
                        {'code': 4},
                        {'code': 4},
                        {'code': 4},
                        {'code': 4},
                        {'bid': 13, 'code': 0},
                        {'bid': 12, 'code': 0},
                    ]
                }
            },
            preserve_order=True,
        )


if __name__ == '__main__':
    main()

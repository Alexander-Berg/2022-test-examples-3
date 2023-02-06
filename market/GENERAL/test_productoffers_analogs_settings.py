#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    BlueOffer,
    MarketSku,
    MnPlace,
    Model,
    Offer,
    Opinion,
    Region,
    Shop,
    YamarecPlace,
    YamarecSettingPartition,
)
from core.testcase import TestCase, main
from core.matcher import NotEmpty
from core.types.analogs_settings import AnalogsSettings


class T(TestCase):
    @classmethod
    def prepare(cls):
        """
        Модели, офферы и конфигурация для выдачи аналогов на КМ
        """

        cls.matrixnet.on_default_place(MnPlace.BASE_SEARCH).respond(0.01)

        cls.index.regiontree += [
            Region(rid=213, region_type=Region.CITY),
            Region(rid=2, region_type=Region.CITY),
        ]

        cls.index.shops += [
            Shop(fesh=10, priority_region=213, blue=Shop.BLUE_REAL),
            Shop(fesh=20, priority_region=213, cpa=Shop.CPA_REAL),
            Shop(fesh=40, priority_region=213),
        ]

        cls.index.models += [
            Model(
                hyperid=110,
                hid=110,
                opinion=Opinion(
                    total_count=100, positive_count=80, rating=4.5, precise_rating=4.6, rating_count=200, reviews=5
                ),
            ),
            Model(
                hyperid=120,
                hid=120,
                opinion=Opinion(
                    total_count=100, positive_count=60, rating=3.5, precise_rating=3.4, rating_count=200, reviews=5
                ),
            ),
            Model(
                hyperid=130,
                hid=130,
                opinion=Opinion(
                    total_count=100, positive_count=65, rating=3.5, precise_rating=3.7, rating_count=200, reviews=5
                ),
            ),  # no CPA model
            Model(
                hyperid=140,
                hid=140,
                opinion=Opinion(
                    total_count=100, positive_count=55, rating=3.5, precise_rating=3.3, rating_count=200, reviews=5
                ),
            ),
            Model(
                hyperid=150,
                hid=150,
                opinion=Opinion(
                    total_count=100, positive_count=65, rating=3.5, precise_rating=3.7, rating_count=200, reviews=5
                ),
            ),  # no CPA model
        ]
        cls.index.mskus += [
            MarketSku(
                hyperid=110,
                sku=110,
                blue_offers=[
                    BlueOffer(price=1100, feedid=10, waremd5='OFFER-p1100-F10-OOOOWW', ts=109000),
                ],
            ),
            MarketSku(
                hyperid=120,
                sku=120,
                blue_offers=[
                    BlueOffer(price=1200, feedid=10, waremd5='OFFER-p1200-F10-OOOOWW', ts=109002),
                ],
            ),
            MarketSku(
                hyperid=140,
                sku=140,
                blue_offers=[
                    BlueOffer(price=200, feedid=10, waremd5='OFFER-p0200-F10-OOOOWW', ts=109004),
                ],
            ),
        ]

        cls.index.offers += [
            Offer(fesh=20, hyperid=110, price=1120, ts=107001, cpa=Offer.CPA_REAL),
            Offer(fesh=40, hyperid=110, price=1140, ts=107003),
            Offer(fesh=20, hyperid=120, price=1220, ts=107006, cpa=Offer.CPA_REAL),
            Offer(fesh=40, hyperid=120, price=1240, ts=107007),
            Offer(fesh=40, hyperid=130, sku=130, price=1340, ts=107009),
            Offer(fesh=40, hyperid=150, sku=150, price=1540, ts=107009),
        ]

        cls.index.yamarec_places += [
            YamarecPlace(
                name=YamarecPlace.Name.COMPETITIVE_MODEL,
                kind=YamarecPlace.Type.SETTING,
                split_rule=YamarecPlace.SplitRule.ABT,
                partitions=[
                    # no partition with split 'noconfig'
                    # partitions with data
                    YamarecSettingPartition(params={'version': 'SIBLINGS1_AUGMENTED'}, splits=[{'split': 'empty'}]),
                ],
            ),
        ]

        cls.recommender.on_request_accessory_models(
            model_id=130, item_count=1000, version='SIBLINGS1_AUGMENTED'
        ).respond({'models': ['110:0.7', '120:0.6', '140:0.65']})
        cls.recommender.on_request_accessory_models(
            model_id=120, item_count=1000, version='SIBLINGS1_AUGMENTED'
        ).respond({'models': ['110:0.6', '130:0.6', '140:0.65']})
        cls.recommender.on_request_accessory_models(
            model_id=150, item_count=1000, version='SIBLINGS1_AUGMENTED'
        ).respond({'models': ['110:0.7', '120:0.6', '140:0.65']})

        cls.index.card_analogs_settings += [
            AnalogsSettings(
                original_model=-1,
                original_list_category=130,
                original_offer_type=-1,
                analog_model=-1,
                is_forbidden=True,
            ),  # отключаем подбор аналогов в категории 130
            AnalogsSettings(
                original_model=-1, original_list_category=-1, original_offer_type=3, analog_model=-1, is_forbidden=True
            ),  # отключаем подбор аналогов для cpa-моделей
        ]

    def test_model_with_analogs_top_without_management(self):
        """
        Проверяем, что в ответ попадают модели-аналоги если market_enable_analogs_yt_management=0
        """

        request = (
            "place=productoffers&hyperid=130&offers-set=top&show-card-analogs=1&no-vbid-filter=1&"
            "rearr-factors=market_card_analogs_hide_models_in_output=0;split=empty;market_enable_analogs_yt_management=0;market_card_analogs_dont_show_analogs_for_cpc_only=0&debug=1"
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "analogs": [
                    {
                        "model": {"id": 110, "isAnalogModel": True},
                        "offer": {
                            "isAnalogOffer": True,
                            "cpa": "real",
                            "model": {"id": 110},
                            "prices": {"value": "1100"},
                        },
                    }
                ]
            },
        )

    def test_model_with_analogs_top_with_management(self):
        """
        Проверяем, что в ответ попадают не модели-аналоги если market_enable_analogs_yt_management=1
        """

        request = (
            "place=productoffers&hyperid=130&offers-set=top&show-card-analogs=1&no-vbid-filter=1&"
            "rearr-factors=market_card_analogs_hide_models_in_output=0;split=empty;market_enable_analogs_yt_management=1;"
            "market_card_analogs_score_rand_low=1.0;market_card_analogs_score_rand_delta=0.0&debug=1"
        )
        response = self.report.request_json(request)

        # аналоги для категории 130 не находятся
        self.assertFragmentNotIn(response, {"isAnalogOffer": True})
        self.assertFragmentNotIn(response, {"isAnalogModel": True})
        self.assertFragmentNotIn(response, {"model": {"id": 140}})
        self.assertFragmentNotIn(response, {"model": {"id": 110}})
        self.assertFragmentNotIn(response, {"model": {"id": 120}})

    def test_model_with_analogs_top_with_management_not_excluded(self):
        """
        Проверяем, что в ответ попадают модели-аналоги для 150 категории даже если market_enable_analogs_yt_management=1
        """

        request = (
            "place=productoffers&hyperid=150&offers-set=top&show-card-analogs=1&no-vbid-filter=1&"
            "rearr-factors=market_card_analogs_hide_models_in_output=0;split=empty;market_enable_analogs_yt_management=1;"
            "market_card_analogs_score_rand_low=1.0;market_card_analogs_score_rand_delta=0.0;market_card_analogs_dont_show_analogs_for_cpc_only=0&debug=1"
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "analogs": [
                    {
                        "model": {"id": 110, "isAnalogModel": True},
                        "offer": {
                            "isAnalogOffer": True,
                            "cpa": "real",
                            "model": {"id": 110},
                            "prices": {"value": "1100"},
                        },
                    }
                ]
            },
        )

    def test_model_with_analogs_default_list_cpa_model_without_management(self):
        """
        Проверяем, что ограничения не действуют если market_enable_analogs_yt_management=0
        """

        request = (
            "place=productoffers&hyperid=120&offers-set=defaultList,list&show-card-analogs=1&no-vbid-filter=1&"
            "rearr-factors=market_force_request_card_analogs=1;market_card_analogs_hide_models_in_output=0;split=empty;market_card_analog_min_base_score=0.55;"
            "market_enable_analogs_yt_management=0;market_card_analogs_score_rand_low=1.0;market_card_analogs_score_rand_delta=0.0;market_card_analogs_dont_show_analogs_for_cpc_only=0&debug=1"
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "isAnalogOffer": False,
                "model": {"id": 120},
                "prices": {"value": "1200"},
                "benefit": NotEmpty(),
            },
        )  # ДО

        self.assertFragmentIn(
            response,
            {
                "analogs": [
                    {
                        "model": {"id": 140, "isAnalogModel": True},
                        "offer": {
                            "isAnalogOffer": True,
                            "cpa": "real",
                            "model": {"id": 140},
                            "prices": {"value": "200"},
                        },
                        "reason": "Price",
                    },
                    {
                        "model": {"id": 110, "isAnalogModel": True},
                        "offer": {
                            "isAnalogOffer": True,
                            "cpa": "real",
                            "model": {"id": 110},
                            "prices": {"value": "1100"},
                        },
                        "reason": "ModelRating",
                    },
                ]
            },
        )

        self.assertFragmentNotIn(response, {"model": {"id": 130}})  # модель только с cpc офферами не встретилась

    def test_model_with_analogs_default_list_cpa_model_with_management(self):
        """
        Проверяем, что в ответ не попадают модели-аналоги (поскольку они отключены для CPA-моделей) если market_enable_analogs_yt_management=1
        """

        request = (
            "place=productoffers&hyperid=120&offers-set=defaultList,list&show-card-analogs=1&no-vbid-filter=1&"
            "rearr-factors=market_force_request_card_analogs=1;market_card_analogs_hide_models_in_output=0;split=empty;"
            "market_enable_analogs_yt_management=1;market_card_analogs_score_rand_low=1.0;market_card_analogs_score_rand_delta=0.0&debug=1"
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "isAnalogOffer": False,
                "model": {"id": 120},
                "prices": {"value": "1200"},
                "benefit": NotEmpty(),
            },
        )  # ДО

        # аналоги для cpa-модели не находятся
        self.assertFragmentNotIn(response, {"isAnalogOffer": True})
        self.assertFragmentNotIn(response, {"isAnalogModel": True})
        self.assertFragmentNotIn(response, {"model": {"id": 140}})
        self.assertFragmentNotIn(response, {"model": {"id": 110}})
        self.assertFragmentNotIn(response, {"model": {"id": 130}})


if __name__ == '__main__':
    main()

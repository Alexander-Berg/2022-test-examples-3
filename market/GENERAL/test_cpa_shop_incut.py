#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    Autostrategy,
    AutostrategyType,
    AutostrategyWithDatasourceId,
    BlueOffer,
    ClickType,
    GLParam,
    GLType,
    GLValue,
    EntityCtr,
    HyperCategory,
    IncutBlackListFb,
    MarketSku,
    Model,
    ModelDescriptionTemplates,
    MnPlace,
    Offer,
    Opinion,
    Region,
    ReservePriceFee,
    Shop,
    UrlType,
    NavCategory,
    PriceThreshold,
)
from core.types.fashion_parameters import FashionCategory
from core.types.recommended_fee import RecommendedFee
from core.testcase import TestCase, main
from core.matcher import Contains, Round, ElementCount, NoKey, EmptyList, NotEmpty, Capture, Absent


def dict_to_rearr(rearr_flags_dict):
    return ';'.join([rearr_name + '=' + str(rearr_flags_dict[rearr_name]) for rearr_name in rearr_flags_dict])


class T(TestCase):
    @classmethod
    def prepare(cls):
        # cls.disable_check_empty_output()

        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']

        cls.index.shops += [
            Shop(fesh=1, priority_region=213, cpa=Shop.CPA_REAL, name='CPA Магазин в Москве #1'),
            Shop(fesh=2, priority_region=213, cpa=Shop.CPA_REAL, name='CPA Магазин в Москве #2'),
            Shop(fesh=3, priority_region=213, cpa=Shop.CPA_REAL, name='CPA Магазин в Москве #3'),
            Shop(fesh=4, priority_region=213, cpa=Shop.CPA_REAL, name='CPA Магазин в Москве #4'),
            Shop(fesh=5, priority_region=213, cpa=Shop.CPA_REAL, name='CPA Магазин в Москве #5'),
            Shop(
                fesh=6,
                datafeed_id=6,
                priority_region=213,
                cpa=Shop.CPA_REAL,
                regions=[213],
                name='1P Магазин в Москве #6',
                supplier_type=Shop.FIRST_PARTY,
                is_supplier=True,
                blue=Shop.BLUE_REAL,
                warehouse_id=145,
                fulfillment_program=True,
            ),
        ]
        cls.index.navtree += [
            NavCategory(nid=11, hid=11, primary=True),
        ]
        cls.index.offers += [
            Offer(hyperid=1, hid=11, fesh=1, ts=100010, price=100, fee=500, title='CPA офер #1-1', cpa=Offer.CPA_REAL),
            Offer(
                hyperid=1,
                hid=11,
                fesh=2,
                ts=100011,
                price=110,
                fee=1000,
                title='CPA офер #1-2',
                waremd5="FIRST-PARTY-1-2-1-XXXg",
                cpa=Offer.CPA_REAL,
            ),
            Offer(
                hyperid=1,
                hid=11,
                fesh=6,
                ts=100012,
                price=110,
                fee=2500,
                title='CPA офер #1-3',
                waremd5="FIRST-PARTY-1-3-1-XXXg",
                cpa=Offer.CPA_REAL,
            ),
            Offer(hyperid=2, hid=22, fesh=2, ts=100020, price=200, fee=800, title='CPA офер #2-1', cpa=Offer.CPA_REAL),
            Offer(hyperid=2, hid=22, fesh=3, ts=100021, price=200, fee=80, title='CPA офер #2-2', cpa=Offer.CPA_REAL),
            Offer(hyperid=3, hid=33, fesh=4, ts=100030, price=300, fee=1600, title='CPA офер #3-1', cpa=Offer.CPA_REAL),
            Offer(
                hyperid=3,
                hid=33,
                fesh=3,
                ts=100031,
                price=303,
                fee=2000,
                title='CPA лучший офер #3-2',
                cpa=Offer.CPA_REAL,
            ),
            Offer(
                hyperid=4,
                hid=44,
                fesh=4,
                ts=100040,
                price=40,
                fee=400,
                title='CPA худший офер #4-1',
                cpa=Offer.CPA_REAL,
            ),
            Offer(hyperid=4, hid=44, fesh=5, ts=100040, price=100, fee=500, title='Ставка ниже', cpa=Offer.CPA_REAL),
            Offer(hyperid=4, hid=44, fesh=4, ts=100040, price=100, fee=1000, title='Ставка выше', cpa=Offer.CPA_REAL),
            Offer(hyperid=4, hid=44, fesh=5, ts=100040, price=100, fee=1000, title='Цена ниже', cpa=Offer.CPA_REAL),
            Offer(hyperid=4, hid=44, fesh=4, ts=100040, price=200, fee=1000, title='Цена выше', cpa=Offer.CPA_REAL),
            Offer(hyperid=5, hid=55, fesh=5, ts=100040, price=100, fee=0, title='ZeroFee', cpa=Offer.CPA_REAL),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 100010).respond(0.01)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 100011).respond(0.011)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 100012).respond(0.025)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 100020).respond(0.02)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 100030).respond(0.03)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 100031).respond(0.031)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 100040).respond(0.004)

    def test_fee_inpact(self):
        # При прочих равных выбирается офер с большей ставкой

        response = self.report.request_json(
            'place=cpa_shop_incut&text=ставка&show-urls=cpa&debug=1&rearr-factors=market_premium_ads_gallery_default_min_num_doc=0;'
            'market_premium_ads_gallery_shop_incut_logarithm_price=0'
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'titles': {'raw': 'Ставка выше'},
                        'debug': {'rank': [{'name': 'CPA_SHOP_INCUT', 'value': '400'}]},
                        "urls": {
                            "cpa": Contains("/shop_fee_ab=500/", "/shop_fee=1000/"),
                        },
                    }
                ],
                'debug': {
                    'report': {
                        'logicTrace': [
                            Contains("Take first offer in group [Ставка выше], shopId: 4 relevance: 400"),
                            Contains(
                                "Calc multiplier: 0.5 for offer [Ставка выше] by next offer in group [Ставка ниже], shopId: 5 relevance: 200"
                            ),
                        ]
                    }
                },
            },
        )
        self.show_log_tskv.expect(title='Ставка выше', shop_fee_ab=500, shop_fee=1000)

    def test_price_inpact(self):
        # При прочих равных выбирается офер с большей ценой

        response = self.report.request_json(
            'place=cpa_shop_incut&text=цена&debug=1&rearr-factors=market_premium_ads_gallery_default_min_num_doc=0;'
            'market_premium_ads_gallery_shop_incut_logarithm_price=0'
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {'titles': {'raw': 'Цена выше'}, 'debug': {'rank': [{'name': 'CPA_SHOP_INCUT', 'value': '800'}]}},
                ]
            },
        )

    def test_hid_result(self):
        # Проверяем что в каждой категории выберется 1 лучший офер (by fee, price, relevance)

        self.assertFragmentIn(
            self.report.request_json(
                'place=cpa_shop_incut&hid=11&debug=1&rearr-factors=market_premium_ads_gallery_shop_fee_threshold=0;market_premium_ads_gallery_default_min_num_doc=0;market_premium_ads_gallery_shop_incut_logarithm_price=0'  # noqa
            ),
            {
                'results': [
                    {
                        'titles': {'raw': 'CPA офер #1-2'},
                        "debug": {
                            "properties": {
                                "MATRIXNET_VALUE": Round(0.011),
                            }
                        },
                    }
                ]
            },
        )
        self.assertFragmentIn(
            self.report.request_json(
                'place=cpa_shop_incut&hid=33&debug=1&rearr-factors=market_premium_ads_gallery_default_min_num_doc=0;market_premium_ads_gallery_shop_incut_logarithm_price=0'
            ),
            {
                'results': [
                    {
                        'titles': {'raw': 'CPA лучший офер #3-2'},
                        "debug": {
                            "properties": {
                                "MATRIXNET_VALUE": Round(0.031),
                            }
                        },
                    }
                ]
            },
        )
        self.assertFragmentIn(
            self.report.request_json(
                'place=cpa_shop_incut&hid=33&debug=1&rearr-factors=market_premium_ads_gallery_default_min_num_doc=0;market_premium_ads_gallery_shop_incut_logarithm_price=1'
            ),
            {
                'results': [
                    {
                        'titles': {'raw': 'CPA лучший офер #3-2'},
                        "debug": {
                            "properties": {
                                "MATRIXNET_VALUE": Round(0.031),
                            }
                        },
                    }
                ]
            },
        )

    def test_nid_result(self):
        # Проверяем что фильтрация по nid работает

        self.assertFragmentIn(
            self.report.request_json(
                'place=cpa_shop_incut&hid=11&debug=1&rearr-factors=market_premium_ads_gallery_shop_fee_threshold=0;market_premium_ads_gallery_default_min_num_doc=0;market_premium_ads_gallery_shop_incut_logarithm_price=0'  # noqa
            ),
            {
                'results': [
                    {
                        'titles': {'raw': 'CPA офер #1-2'},
                        "debug": {
                            "properties": {
                                "MATRIXNET_VALUE": Round(0.011),
                            }
                        },
                    }
                ]
            },
        )

    def test_nid_result_1p_zero_fee(self):
        # Проверяем что фильтрация по nid работает с флагом market_premium_ads_gallery_shop_incut_1p_zero_fee=1

        self.assertFragmentIn(
            self.report.request_json(
                'place=cpa_shop_incut&hid=11&debug=1&rearr-factors=market_premium_ads_gallery_shop_incut_1p_zero_fee=1;market_premium_ads_gallery_shop_fee_threshold=0;market_premium_ads_gallery_default_min_num_doc=0;market_premium_ads_gallery_shop_incut_logarithm_price=0'  # noqa
            ),
            {
                'results': [
                    {
                        'titles': {'raw': 'CPA офер #1-2'},
                        "debug": {
                            "properties": {
                                "MATRIXNET_VALUE": Round(0.011),
                            }
                        },
                    }
                ]
            },
        )

    def test_filter_zero_fee(self):
        # При market_premium_ads_gallery_shop_fee_threshold > 0 фильтруются оферы с нулевой ставкой
        self.assertFragmentIn(
            self.report.request_json(
                'place=cpa_shop_incut&hid=55&pp=18&debug=1&rearr-factors=market_premium_ads_gallery_shop_fee_threshold=0;market_premium_ads_gallery_default_min_num_doc=0;market_premium_ads_gallery_shop_incut_logarithm_price=0'  # noqa
            ),
            {'results': [{'titles': {'raw': 'ZeroFee'}}]},
        )
        self.assertFragmentIn(
            self.report.request_json(
                'place=cpa_shop_incut&hid=55&pp=18&debug=1&rearr-factors=market_premium_ads_gallery_shop_fee_threshold=1;market_premium_ads_gallery_default_min_num_doc=0;market_premium_ads_gallery_shop_incut_logarithm_price=0'  # noqa
            ),
            {"debug": {"brief": {"filters": {"SHOP_FEE_THRESHOLD": 1}}}},
        )

    def test_text_result(self):
        response = self.report.request_json(
            'place=cpa_shop_incut&text=офер&debug=1&rearr-factors=market_premium_ads_gallery_default_min_num_doc=0;market_premium_ads_gallery_shop_incut_logarithm_price=0'
        )

        # В каждой группе должен выбраться один офер, с учетом ставок
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'titles': {'raw': 'CPA лучший офер #3-2'},
                        'debug': {
                            'rank': [{'name': 'CPA_SHOP_INCUT', 'value': '18786'}],
                            "sale": {
                                "shopFee": 2000,
                                "brokeredFee": 1534,
                            },
                        },
                    },
                    {
                        'titles': {'raw': 'CPA офер #2-1'},
                        'debug': {
                            'rank': [{'name': 'CPA_SHOP_INCUT', 'value': '3200'}],
                            "sale": {
                                "shopFee": 800,
                                "brokeredFee": 303,
                            },
                        },
                    },
                    {
                        'titles': {'raw': 'CPA офер #1-2'},
                        'debug': {
                            'rank': [{'name': 'CPA_SHOP_INCUT', 'value': '1210'}],
                            "sale": {
                                "shopFee": 1000,
                                "brokeredFee": 414,
                            },
                        },
                    },
                    {
                        'titles': {'raw': 'CPA худший офер #4-1'},
                        'debug': {
                            'rank': [{'name': 'CPA_SHOP_INCUT', 'value': '64'}],
                            "sale": {
                                "shopFee": 400,
                                "brokeredFee": 1,
                            },
                        },
                    },
                ]
            },
            preserve_order=True,
        )

        # Проверяем как посчитались multiplier коэффициенты
        self.assertFragmentIn(
            response,
            {
                'logicTrace': [
                    Contains("Take first offer in group [CPA лучший офер #3-2], shopId: 3 relevance: 18786"),
                    Contains(
                        "Calc multiplier: 0.766528 for offer [CPA лучший офер #3-2] by next offer in group [CPA офер #3-1], shopId: 4 relevance: 14400"
                    ),
                    Contains("Take first offer in group [CPA офер #2-1], shopId: 2 relevance: 3200"),
                    Contains(
                        "Calc multiplier: 0.199687 for offer [CPA офер #2-1] by next offer in group [CPA офер #2-2], shopId: 3 relevance: 639"
                    ),
                    Contains(
                        "Calc multiplier: 0.378125 for offer [CPA офер #2-1] by first offer of next group, shopId: 2 relevance: 1210"
                    ),
                    Contains("Take first offer in group [CPA офер #1-2], shopId: 2 relevance: 1210"),
                    Contains(
                        "Calc multiplier: 0.413223 for offer [CPA офер #1-2] by next offer in group [CPA офер #1-1], shopId: 1 relevance: 500"
                    ),
                    Contains("Take first offer in group [CPA худший офер #4-1], shopId: 4 relevance: 64"),
                ]
            },
        )

    def test_text_result_in_avg_autostrategy_raised(self):
        response = self.report.request_json(
            'place=cpa_shop_incut&text=офер&debug=1&rearr-factors=market_premium_ads_gallery_default_min_num_doc=0;'
            'market_premium_ads_gallery_shop_incut_logarithm_price=0;market_cs_incut_enable_online_avg_autostrategy=1;market_cs_incut_avg_autostrategy_prob_rnd=0.1'
        )

        # В каждой группе должен выбраться один оффер, с учетом ставок
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'titles': {'raw': 'CPA офер #2-1'},
                        'debug': {
                            'rank': [{'name': 'CPA_SHOP_INCUT', 'value': '3200'}],
                            "sale": {
                                "shopFee": 800,  # fee increased on 1477 to 1780
                                "brokeredFee": 303,
                            },
                        },
                    },
                    {
                        'titles': {'raw': 'CPA лучший офер #3-2'},
                        'debug': {
                            'rank': [{'name': 'CPA_SHOP_INCUT', 'value': '18786'}],
                            "sale": {
                                "shopFee": 2000,
                                "brokeredFee": 1534,
                            },
                        },
                    },
                    {
                        'titles': {'raw': 'CPA офер #1-2'},
                        'debug': {
                            'rank': [{'name': 'CPA_SHOP_INCUT', 'value': '1210'}],
                            "sale": {
                                "shopFee": 1000,
                                "brokeredFee": 414,
                            },
                        },
                    },
                    {
                        'titles': {'raw': 'CPA худший офер #4-1'},
                        'debug': {
                            'rank': [{'name': 'CPA_SHOP_INCUT', 'value': '64'}],
                            "sale": {
                                "shopFee": 400,
                                "brokeredFee": 1,
                            },
                        },
                    },
                ]
            },
            preserve_order=True,
        )
        self.assertFragmentIn(response, "on position=1; mandatoryFeeIncrease=1477; resultCurrentOfferFee=1780;")

        self.assertFragmentIn(response, "Offer moved to pos=1 from 2")
        self.assertFragmentIn(response, "Offer moved to pos=2 from 1")
        self.assertFragmentIn(response, "Offer moved to pos=3 from 3")
        self.assertFragmentIn(response, "Offer moved to pos=4 from 4")

        self.show_log.expect(
            url_type=6,
            shop_fee=800,
            shop_fee_ab=303,
            shop_fee_ab_search=303,
            analog_general_score=1477,
            analogs_choose_detail_info="1477,1780,3200,18786,0.05280528053,0.04041720991",
        )

    def test_text_result_in_avg_autostrategy_not_raised_because_of_decr_coef(self):
        response = self.report.request_json(
            'place=cpa_shop_incut&text=офер&debug=1&rearr-factors=market_premium_ads_gallery_default_min_num_doc=0;'
            'market_premium_ads_gallery_shop_incut_logarithm_price=0;market_cs_incut_enable_online_avg_autostrategy=1;'
            'market_cs_incut_avg_autostrategy_decreasing_coef=10.1;market_buybox_enable_advert_buybox=0;'
        )

        # В каждой группе должен выбраться один оффер, с учетом ставок
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'titles': {'raw': 'CPA лучший офер #3-2'},
                        'debug': {
                            'rank': [{'name': 'CPA_SHOP_INCUT', 'value': '18786'}],
                            "sale": {
                                "shopFee": 2000,
                                "brokeredFee": 1534,
                            },
                        },
                    },
                    {
                        'titles': {'raw': 'CPA офер #2-1'},
                        'debug': {
                            'rank': [{'name': 'CPA_SHOP_INCUT', 'value': '3200'}],
                            "sale": {
                                "shopFee": 800,
                                "brokeredFee": 303,
                            },
                        },
                    },
                    {
                        'titles': {'raw': 'CPA офер #1-2'},
                        'debug': {
                            'rank': [{'name': 'CPA_SHOP_INCUT', 'value': '1210'}],
                            "sale": {
                                "shopFee": 1000,
                                "brokeredFee": 414,
                            },
                        },
                    },
                    {
                        'titles': {'raw': 'CPA худший офер #4-1'},
                        'debug': {
                            'rank': [{'name': 'CPA_SHOP_INCUT', 'value': '64'}],
                            "sale": {
                                "shopFee": 400,
                                "brokeredFee": 1,
                            },
                        },
                    },
                ]
            },
            preserve_order=True,
        )
        self.assertFragmentNotIn(response, "on position=1; mandatoryFeeIncrease=1477; resultCurrentOfferFee=1780;")

        self.assertFragmentIn(response, "Offer moved to pos=1 from 1")
        self.assertFragmentIn(response, "Offer moved to pos=2 from 2")
        self.assertFragmentIn(response, "Offer moved to pos=3 from 3")
        self.assertFragmentIn(response, "Offer moved to pos=4 from 4")

        self.show_log.expect(
            url_type=6,
            shop_fee=800,
            shop_fee_ab=303,
            shop_fee_ab_search=303,
            analog_general_score=Absent(),
            analogs_choose_detail_info="",
        )

    def test_text_result_in_avg_autostrategy_not_raised_because_of_rnd(self):
        response = self.report.request_json(
            'place=cpa_shop_incut&text=офер&debug=1&rearr-factors=market_premium_ads_gallery_default_min_num_doc=0;'
            'market_premium_ads_gallery_shop_incut_logarithm_price=0;market_cs_incut_enable_online_avg_autostrategy=1;market_cs_incut_avg_autostrategy_prob_rnd=0.2'
        )

        # В каждой группе должен выбраться один оффер, с учетом ставок
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'titles': {'raw': 'CPA лучший офер #3-2'},
                        'debug': {
                            'rank': [{'name': 'CPA_SHOP_INCUT', 'value': '18786'}],
                            "sale": {
                                "shopFee": 2000,
                                "brokeredFee": 1534,
                            },
                        },
                    },
                    {
                        'titles': {'raw': 'CPA офер #2-1'},
                        'debug': {
                            'rank': [{'name': 'CPA_SHOP_INCUT', 'value': '3200'}],
                            "sale": {
                                "shopFee": 800,
                                "brokeredFee": 303,
                            },
                        },
                    },
                    {
                        'titles': {'raw': 'CPA офер #1-2'},
                        'debug': {
                            'rank': [{'name': 'CPA_SHOP_INCUT', 'value': '1210'}],
                            "sale": {
                                "shopFee": 1000,
                                "brokeredFee": 414,
                            },
                        },
                    },
                    {
                        'titles': {'raw': 'CPA худший офер #4-1'},
                        'debug': {
                            'rank': [{'name': 'CPA_SHOP_INCUT', 'value': '64'}],
                            "sale": {
                                "shopFee": 400,
                                "brokeredFee": 1,
                            },
                        },
                    },
                ]
            },
            preserve_order=True,
        )
        self.assertFragmentNotIn(response, "on position=1; mandatoryFeeIncrease=1477; resultCurrentOfferFee=1780;")

        self.assertFragmentIn(response, "Offer moved to pos=1 from 1")
        self.assertFragmentIn(response, "Offer moved to pos=2 from 2")
        self.assertFragmentIn(response, "Offer moved to pos=3 from 3")
        self.assertFragmentIn(response, "Offer moved to pos=4 from 4")

        self.show_log.expect(
            url_type=6,
            shop_fee=800,
            shop_fee_ab=303,
            shop_fee_ab_search=303,
            analog_general_score=Absent(),
            analogs_choose_detail_info="",
        )

    def test_text_result_with_meta_input_trace(self):
        response = self.report.request_json(
            'place=cpa_shop_incut&text=офер&debug=1&rearr-factors=market_premium_ads_gallery_shop_incut_trace_meta_input=1;market_premium_ads_gallery_default_min_num_doc=0;market_premium_ads_gallery_shop_incut_logarithm_price=0'  # noqa
        )
        self.assertFragmentIn(
            response,
            {
                'logicTrace': [
                    Contains("ShopCpaIncutTraceMetaInput:start"),
                    Contains("OriginalRelevance=0.03;MixedRelevance=14400;"),
                    Contains("OriginalRelevance=0.04;MixedRelevance=639;"),
                    Contains("ShopCpaIncutTraceMetaInput:end"),
                ]
            },
        )

    def test_text_result_with_meta_input_trace_off(self):
        response = self.report.request_json(
            'place=cpa_shop_incut&text=офер&debug=1&rearr-factors=market_premium_ads_gallery_default_min_num_doc=0;market_premium_ads_gallery_shop_incut_logarithm_price=0'
        )
        self.assertFragmentNotIn(response, 'ShopCpaIncutTraceMetaInput:')

    def test_text_result_with_price_rel_threshold(self):
        response = self.report.request_json(
            'place=cpa_shop_incut&text=офер&debug=1&rearr-factors=market_premium_ads_gallery_default_min_num_doc=0;market_premium_ads_gallery_shop_incut_logarithm_price=0;market_premium_ads_gallery_shop_incut_min_rel_price_threshold_coef=0.9;market_premium_ads_gallery_shop_incut_max_rel_price_threshold_coef=1.3;market_premium_ads_gallery_shop_incut_rel_price_info_docs_count=4'  # noqa
        )

        self.assertFragmentNotIn(response, 'CPA офер #1-2')

        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'titles': {'raw': 'CPA лучший офер #3-2'},
                        'debug': {
                            'rank': [{'name': 'CPA_SHOP_INCUT', 'value': '18786'}],
                            "sale": {
                                "shopFee": 2000,
                                "brokeredFee": 1534,
                            },
                        },
                    },
                    {
                        'titles': {'raw': 'CPA офер #2-1'},
                        'debug': {
                            'rank': [{'name': 'CPA_SHOP_INCUT', 'value': '3200'}],
                            "sale": {
                                "shopFee": 800,
                                "brokeredFee": 160,
                            },
                        },
                    },
                ]
            },
        )

        # Проверяем как посчитались multiplier коэффициенты
        self.assertFragmentIn(
            response,
            {
                'logicTrace': [
                    Contains("Take first offer in group [CPA лучший офер #3-2], shopId: 3 relevance: 18786"),
                    Contains(
                        "Calc multiplier: 0.766528 for offer [CPA лучший офер #3-2] by next offer in group [CPA офер #3-1], shopId: 4 relevance: 14400"
                    ),
                    Contains("Take first offer in group [CPA офер #2-1], shopId: 2 relevance: 3200"),
                    Contains(
                        "Calc multiplier: 0.199687 for offer [CPA офер #2-1] by next offer in group [CPA офер #2-2], shopId: 3 relevance: 639"
                    ),
                ]
            },
        )

    def test_text_result_with_price_rel_threshold_high_count(self):
        response = self.report.request_json(
            'place=cpa_shop_incut&text=офер&debug=1&rearr-factors=market_premium_ads_gallery_default_min_num_doc=0;market_premium_ads_gallery_shop_incut_logarithm_price=0;market_premium_ads_gallery_shop_incut_min_rel_price_threshold_coef=0.9;market_premium_ads_gallery_shop_incut_max_rel_price_threshold_coef=1.3;market_premium_ads_gallery_shop_incut_rel_price_info_docs_count=100'  # noqa
        )

        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'titles': {'raw': 'CPA лучший офер #3-2'},
                        'debug': {
                            'rank': [{'name': 'CPA_SHOP_INCUT', 'value': '18786'}],
                            "sale": {
                                "shopFee": 2000,
                                "brokeredFee": 1534,
                            },
                        },
                    },
                    {
                        'titles': {'raw': 'CPA офер #2-1'},
                        'debug': {
                            'rank': [{'name': 'CPA_SHOP_INCUT', 'value': '3200'}],
                            "sale": {
                                "shopFee": 800,
                                "brokeredFee": 303,
                            },
                        },
                    },
                    {
                        'titles': {'raw': 'CPA офер #1-2'},
                        'debug': {
                            'rank': [{'name': 'CPA_SHOP_INCUT', 'value': '1210'}],
                            "sale": {
                                "shopFee": 1000,
                                "brokeredFee": 414,
                            },
                        },
                    },
                ]
            },
        )

        # Проверяем как посчитались multiplier коэффициенты
        self.assertFragmentIn(
            response,
            {
                'logicTrace': [
                    Contains("Take first offer in group [CPA лучший офер #3-2], shopId: 3 relevance: 18786"),
                    Contains(
                        "Calc multiplier: 0.766528 for offer [CPA лучший офер #3-2] by next offer in group [CPA офер #3-1], shopId: 4 relevance: 14400"
                    ),
                    Contains("Take first offer in group [CPA офер #2-1], shopId: 2 relevance: 3200"),
                    Contains(
                        "Calc multiplier: 0.199687 for offer [CPA офер #2-1] by next offer in group [CPA офер #2-2], shopId: 3 relevance: 639"
                    ),
                    Contains(
                        "Calc multiplier: 0.378125 for offer [CPA офер #2-1] by first offer of next group, shopId: 2 relevance: 1210"
                    ),
                    Contains("Take first offer in group [CPA офер #1-2], shopId: 2 relevance: 1210"),
                ]
            },
        )

    def test_text_result_with_relevance_threshold(self):
        response0 = self.report.request_json(
            'place=cpa_shop_incut&text=офер&debug=1&rearr-factors=market_premium_ads_gallery_default_min_num_doc=0;market_premium_ads_gallery_shop_incut_logarithm_price=0;market_auction_high_relevance_formula_threshold=0.9997'  # noqa
        )

        self.assertFragmentIn(response0, {'results': []}, allow_different_len=False)

        response1 = self.report.request_json(
            'place=cpa_shop_incut&text=офер&debug=1&rearr-factors=market_premium_ads_gallery_default_min_num_doc=0;market_premium_ads_gallery_shop_incut_logarithm_price=0;market_auction_high_relevance_formula_threshold=0.018'  # noqa
        )

        # В каждой группе должен выбраться один офер, с учетом ставок
        self.assertFragmentIn(
            response1,
            {
                'results': [
                    {
                        'titles': {'raw': 'CPA лучший офер #3-2'},
                        'debug': {
                            'rank': [{'name': 'CPA_SHOP_INCUT', 'value': '18786'}],
                            "sale": {
                                "shopFee": 2000,
                                "brokeredFee": 1534,
                            },
                        },
                    },
                    {
                        'titles': {'raw': 'CPA офер #2-1'},
                        'debug': {
                            'rank': [{'name': 'CPA_SHOP_INCUT', 'value': '3200'}],
                            "sale": {
                                "shopFee": 800,
                                "brokeredFee": 160,
                            },
                        },
                    },
                ]
            },
        )

        self.assertFragmentNotIn(
            response1,
            {
                'results': [
                    {
                        'titles': {'raw': 'CPA офер #1-2'},
                    },
                ]
            },
        )

        self.assertFragmentNotIn(
            response1,
            {
                'results': [
                    {
                        'titles': {'raw': 'CPA худший офер #4-1'},
                    },
                ]
            },
        )

    @classmethod
    def prepare_numdoc(cls):
        cls.index.shops += [
            Shop(
                fesh=10 + i,
                shop_fee=100,
                priority_region=213,
                cpa=Shop.CPA_REAL,
                name='CPA Магазин лунного грунта #{}'.format(i),
            )
            for i in range(1, 10)
        ]

        cls.index.offers += [
            Offer(
                hyperid=10 * i + j,
                hid=11 + j,
                fesh=10 + i,
                ts=100000 + 100 * i + j,
                price=100,
                fee=5 + i + j,
                title='марсианский грунт #{}-{}'.format(i, j),
                cpa=Offer.CPA_REAL,
            )
            for i in range(1, 10)
            for j in range(1, 5)
        ]

        cls.index.offers += [
            Offer(
                hyperid=10001,
                hid=1100,
                fesh=10 + i,
                ts=100100 + i,
                price=100,
                fee=5 + i,
                title='лунные булыжники #1-{}'.format(i),
                cpa=Offer.CPA_REAL,
            )
            for i in range(1, 10)
        ]
        cls.index.offers += [
            Offer(
                hyperid=10002,
                hid=1200,
                fesh=10 + i,
                ts=100200 + i,
                price=100,
                fee=6 + i,
                title='лунные булыжники #2-{}'.format(i),
                cpa=Offer.CPA_REAL,
            )
            for i in range(1, 10)
        ]
        cls.index.offers += [
            Offer(
                hyperid=10003,
                hid=1300,
                fesh=10 + i,
                ts=100300 + i,
                price=100,
                fee=7 + i,
                title='лунные булыжники #3-{}'.format(i),
                cpa=Offer.CPA_REAL,
            )
            for i in range(1, 10)
        ]

        for i in range(1, 10):
            for j in range(1, 5):
                cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 100000 + 100 * i + j).respond(0.04)

    def test_numdoc(self):
        """
        Проверяем работу параметра numdoc
        """
        response = self.report.request_json(
            'place=cpa_shop_incut&text=марсианский грунт&numdoc=3&debug=1&rearr-factors=market_premium_ads_gallery_default_min_num_doc=0;'
            'market_premium_ads_gallery_shop_incut_logarithm_price=0;market_premium_ads_gallery_min_num_doc_to_request_from_base=1'
        )

        self.assertFragmentIn(response, {'results': ElementCount(3)})

        response = self.report.request_json(
            'place=cpa_shop_incut&text=марсианский грунт&numdoc=3&debug=1&rearr-factors=market_premium_ads_gallery_default_min_num_doc=0;'
            'market_premium_ads_gallery_shop_incut_logarithm_price=0;market_premium_ads_gallery_min_num_doc_to_request_from_base=4'
        )

        self.assertFragmentIn(response, {'results': ElementCount(4)})

        response = self.report.request_json(
            'place=cpa_shop_incut&text=марсианский грунт&numdoc=30&debug=1&rearr-factors=market_premium_ads_gallery_default_min_num_doc=0;'
            'market_premium_ads_gallery_shop_incut_logarithm_price=0;market_premium_ads_gallery_min_num_doc_to_request_from_base=1'
        )

        self.assertFragmentIn(response, {'results': ElementCount(30)})

    def test_minnumdoc_and_numdoc(self):
        """
        Проверяем работу параметров min-num-doc и numdoc вместе
        """
        response = self.report.request_json(
            'place=cpa_shop_incut&text=марсианский грунт&numdoc=4&min-num-doc=4&rearr-factors=market_premium_ads_gallery_shop_incut_logarithm_price=0'
            ';market_premium_ads_gallery_min_num_doc_to_request_from_base=1'
        )

        self.assertFragmentIn(response, {'results': ElementCount(4)})

        response = self.report.request_json(
            'place=cpa_shop_incut&text=марсианский грунт&min-num-doc=6&numdoc=30&rearr-factors=market_premium_ads_gallery_shop_incut_logarithm_price=0'
            ';market_premium_ads_gallery_min_num_doc_to_request_from_base=1'
        )

        self.assertFragmentIn(response, {'results': ElementCount(30)})

        response = self.report.request_json(
            'place=cpa_shop_incut&text=лунные булыжники&min-num-doc=3&numdoc=30&rearr-factors=market_premium_ads_gallery_shop_incut_logarithm_price=0;'
            'market_premium_ads_gallery_shop_incut_rel_hid_filter_enable=0;market_premium_ads_gallery_min_num_doc_to_request_from_base=1'
        )

        self.assertFragmentIn(response, {'results': ElementCount(3)})  # вернули сколько есть но не меньше min-num-doc=3

        response = self.report.request_json(
            'place=cpa_shop_incut&text=марсианский грунт&min-num-doc=6&numdoc=6&rearr-factors=market_premium_ads_gallery_shop_incut_logarithm_price=0;'
            'market_premium_ads_gallery_shop_incut_rel_hid_filter_enable=0;market_premium_ads_gallery_min_num_doc_to_request_from_base=1'
        )

        self.assertFragmentIn(
            response, {'results': ElementCount(6)}
        )  # при явном рассогласовании min-num-doc и numdoc приоритет в пользу min-num-doc, потому что так хочет луна,
        # на самом деле чтоб логика совпадала с логикой numdoc по дефолту

    def test_minnumdoc_default_value(self):
        """
        Проверяем работу без параметра minnumdoc, в этом случае работает флаг market_premium_ads_gallery_default_min_num_doc (значение по умолчанию - 5)
        """

        response = self.report.request_json(
            'place=cpa_shop_incut&text=марсианский грунт&debug=1&rearr-factors=market_premium_ads_gallery_shop_incut_logarithm_price=0'
            ';market_premium_ads_gallery_min_num_doc_to_request_from_base=1'
        )

        self.assertFragmentIn(response, {'results': ElementCount(5)})  # дефолтное количество - пять, офферов хватает

        response = self.report.request_json(
            'place=cpa_shop_incut&text=лунные булыжники&debug=1&rearr-factors=market_premium_ads_gallery_shop_incut_logarithm_price=0'
            ';market_premium_ads_gallery_min_num_doc_to_request_from_base=1'
        )

        self.assertFragmentIn(
            response, {'results': ElementCount(0)}
        )  # дефолтное количество - пять, офферов НЕ хватает, моделек всего три

        response = self.report.request_json(
            'place=cpa_shop_incut&text=лунные булыжники&debug=1&rearr-factors=market_premium_ads_gallery_default_min_num_doc=3;'
            'market_premium_ads_gallery_shop_incut_logarithm_price=0;market_premium_ads_gallery_shop_incut_rel_hid_filter_enable=0'
            ';market_premium_ads_gallery_min_num_doc_to_request_from_base=1'
        )

        self.assertFragmentIn(
            response, {'results': ElementCount(3)}
        )  # минимальное дефолтное количество пеменяли на 3, офферов хватает, получили три

        response = self.report.request_json(
            'place=cpa_shop_incut&text=лунные булыжники&debug=1&rearr-factors=market_premium_ads_gallery_default_min_num_doc=33;'
            'market_premium_ads_gallery_shop_incut_logarithm_price=0;market_premium_ads_gallery_shop_incut_rel_hid_filter_enable=0'
            ';market_premium_ads_gallery_min_num_doc_to_request_from_base=1'
        )

        self.assertFragmentIn(
            response, {'results': ElementCount(0)}
        )  # минимальное дефолтное количество пеменяли на 33, офферов НЕ хватает

        response = self.report.request_json(
            'place=cpa_shop_incut&text=марсианский грунт&debug=1&numdoc=22&rearr-factors=market_premium_ads_gallery_default_min_num_doc=22;'
            'market_premium_ads_gallery_shop_incut_logarithm_price=0;market_premium_ads_gallery_shop_incut_rel_hid_filter_enable=0'
            ';market_premium_ads_gallery_min_num_doc_to_request_from_base=1'
        )

        self.assertFragmentIn(
            response, {'results': ElementCount(22)}
        )  # минимальное дефолтное количество пеменяли на 22, офферов хватает, запрошено больше дефолтного numdoc вернем больше

    @classmethod
    def prepare_min_fee(cls):
        cls.index.shops += [
            Shop(
                fesh=20 + i,
                priority_region=213,
                cpa=Shop.CPA_REAL,
                name='CPA Магазин марсианского воздуха #{}'.format(i),
            )
            for i in range(1, 5)
        ]

        cls.index.offers += [
            Offer(
                hyperid=20,
                hid=21,
                fee=100 if i == 1 else 0,
                fesh=20 + i,
                ts=100020 + i,
                price=100,
                title='марсиансктй воздух #{}-1'.format(i),
                cpa=Offer.CPA_REAL,
            )
            for i in range(1, 5)
        ]

        for i in range(1, 5):
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 100020 + i).respond(0.04)

    def test_minfee(self):
        """
        Проверяем работу флага market_premium_ads_gallery_shop_fee_threshold
        """
        response = self.report.request_json(
            'place=cpa_shop_incut&text=марсиансктй воздух&'
            'rearr-factors=market_premium_ads_gallery_shop_fee_threshold=7;market_premium_ads_gallery_default_min_num_doc=0;'
            'market_premium_ads_gallery_shop_incut_logarithm_price=0'
            '&numdoc=10&debug=1'
        )

        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        "debug": {
                            "sale": {
                                "shopFee": 100,
                                "brokeredFee": 7,
                            }
                        }
                    },
                ]
            },
        )

    @classmethod
    def prepare_fee_is_less_then_minfee(cls):
        cls.index.shops += [
            Shop(
                fesh=30 + i, priority_region=213, cpa=Shop.CPA_REAL, name='CPA Магазин венерианской воды #{}'.format(i)
            )
            for i in range(1, 5)
        ]

        cls.index.offers += [
            Offer(
                hyperid=30,
                hid=31,
                fee=100 if i == 1 else 800 if i == 2 else 0,
                fesh=30 + i,
                ts=300000 + i,
                price=100,
                title='венерианская вода #{}-1'.format(i),
                cpa=Offer.CPA_REAL,
            )
            for i in range(1, 5)
        ]

        for i in range(1, 5):
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 300000 + i).respond(0.9 if i == 1 else 0.004)

    def test_fee_is_less_then_minfee(self):
        """
        Проверяем что предложения с ставкой меньше минимальной не попадают во врезку
        """
        response = self.report.request_json(
            'place=cpa_shop_incut&text=венерианская вода&'
            'rearr-factors=market_premium_ads_gallery_default_min_num_doc=0;market_premium_ads_gallery_shop_fee_threshold=700;'
            'market_premium_ads_gallery_shop_incut_logarithm_price=0'
            '&numdoc=10&send-sale-data=1'
        )

        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        "sale": {
                            "shopFee": 800,
                            "brokeredFee": 700,
                        }
                    },
                ]
            },
        )

    def test_output_no_send_sale_data(self):
        """
        Проверяем что без флага send-sale-data=1 и без debug=1 инфа о ставках не появится в выдаче
        """
        response = self.report.request_json(
            'place=cpa_shop_incut&text=венерианская вода&'
            'rearr-factors=market_premium_ads_gallery_default_min_num_doc=0;market_premium_ads_gallery_shop_fee_threshold=700;'
            'market_premium_ads_gallery_shop_incut_logarithm_price=0'
            '&numdoc=10'
        )

        self.assertFragmentNotIn(response, {"shopFee": NotEmpty()})
        self.assertFragmentNotIn(response, {"brokeredFee": NotEmpty()})

    def test_minimum_number_of_documents(self):
        """
        Проверяем, что если подходящих документов меньше требуемого, то вернется пустой ответ
        """

        response = self.report.request_json(
            'place=cpa_shop_incut&text=венерианская вода&'
            'rearr-factors=market_premium_ads_gallery_shop_fee_threshold=700;market_premium_ads_gallery_default_min_num_doc=0;'
            'market_premium_ads_gallery_shop_incut_logarithm_price=0'
            '&numdoc=10&min-num-doc=2&debug=1'
        )

        self.assertFragmentIn(response, {'results': ElementCount(0)})

    @classmethod
    def prepare_vendor_cpa_auction(cls):
        cls.index.hypertree = [
            HyperCategory(
                hid=1009,
                children=[
                    HyperCategory(hid=1010, children=[HyperCategory(hid=1011)]),
                ],
            ),
        ]
        cls.index.offers += [
            Offer(
                hyperid=101,
                hid=1011,
                fesh=1,
                ts=101001,
                price=10000,
                fee=100,
                cpa=Offer.CPA_REAL,
                autostrategy_vendor_with_datasource_id=AutostrategyWithDatasourceId(
                    id=444,
                    datasource_id=1,
                    strategy=Autostrategy(type=AutostrategyType.POSITIONAL, position=1, maxbid=100),
                ),
            ),  # fee = 600
            Offer(
                hyperid=101,
                hid=1011,
                fesh=2,
                ts=101002,
                price=10000,
                fee=500,
                cpa=Offer.CPA_REAL,
                autostrategy_vendor_with_datasource_id=AutostrategyWithDatasourceId(
                    id=444,
                    datasource_id=1,
                    strategy=Autostrategy(type=AutostrategyType.POSITIONAL, position=1, maxbid=10),
                ),
            ),  # fee = 60
            Offer(hyperid=102, hid=1011, fesh=1, ts=102001, price=10000, fee=400, cpa=Offer.CPA_REAL),
            Offer(
                hyperid=103,
                hid=1011,
                fesh=1,
                ts=103001,
                price=10000,
                fee=0,
                cpa=Offer.CPA_REAL,
                autostrategy_vendor_with_datasource_id=AutostrategyWithDatasourceId(
                    id=444,
                    datasource_id=1,
                    strategy=Autostrategy(type=AutostrategyType.POSITIONAL, position=1, maxbid=50),
                ),
            ),  # fee = 300
        ]

    def test_vendor_cpa_auction(self):
        response = self.report.request_json(
            'place=cpa_shop_incut&hid=1011&min-num-doc=0&show-urls=cpa,promotion&rearr-factors=market_premium_ads_gallery_shop_incut_logarithm_price=0;'
            'market_premium_ads_gallery_shop_incut_vendor_bid_to_fee_coef=0.05'  # noqa
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'shop': {'id': 1},
                        'model': {'id': 101},
                    },
                    {
                        'shop': {'id': 1},
                        'model': {'id': 102},
                    },
                    {
                        'shop': {'id': 1},
                        'model': {'id': 103},
                    },
                ]
            },
            preserve_order=True,
        )

        self.click_log.expect(clicktype=ClickType.CPA, shop_id=1, hyper_id=101, shop_fee=100, shop_fee_ab=80)
        self.click_log.expect(clicktype=ClickType.PROMOTION, shop_id=1, hyper_id=101, cb_vnd=100, cp_vnd=80, cb=0, cp=0)

        self.click_log.expect(clicktype=ClickType.CPA, shop_id=1, hyper_id=102, shop_fee=400, shop_fee_ab=300)
        self.click_log.expect(clicktype=ClickType.PROMOTION, shop_id=1, hyper_id=102, cb_vnd=0, cp_vnd=0, cb=0, cp=0)

        self.click_log.expect(clicktype=ClickType.CPA, shop_id=1, hyper_id=103, shop_fee=0, shop_fee_ab=0)
        self.click_log.expect(clicktype=ClickType.PROMOTION, shop_id=1, hyper_id=103, cb_vnd=50, cp_vnd=1, cb=0, cp=0)

    def test_vendor_cpa_auction_correction(self):
        '''
        Тестируем флаг market_premium_ads_gallery_shop_incut_vendor_fee_correction=0.5
        Этим флагом мы уменьшаем влиение вендорской ставки в 2 раза. Вот что изменилось, по сравнению с предыдущим тестом:
        - На первое место выбрался оффер 'shop': {'id': 2}. У него fee=500, а вендорская fee = 60 (учитываем как 30), итого 530. В предыдущем тесте побеждал
        оффер с fee=100 и вендорской fee = 600. Так как влияние вендорской fee стали учитывать в 2 раза ниже, итого получилось 400. Побеждать стал другой
        оффер
        - Изменилось списание денег. Деньги амнистируются пропорционально итоговым очкам CPA_SHOP_INCUT. Ставка амнистируется пропорционально максимуму,
        либо следующему офферу в группе, либо первому офферу в следующей группе. В нашем случае, первый оффер амнистируется пропорционально следующему оферу
        в первой группе. В предыдущем тесте, первый оффер набирал 2100000 очков, второй 1680000 - соотношение 0.8. Поэтому все списания домножались на 0.8
        В этом примере первый оффер стал набирать 1590000, второй 1200000. Очки изменились из-за уменьшения влияения вендорской ставки. Соотношение стало
        0.754717. Поэтому, с магазина мы возьмем 378 fee, а с вендора 10 * 0.5 * 0.754717 = 4

        Изменилось списание денег со второго места. Списание второго второго оффера амнистируется до третьего места. На третьем оффере есть только вендорская
        ставка. Ее влияение мы уменьшили в 2 раза, поэтому третий оффер стал набирать в 2 раза меньше очков - 450000. Соответственно в 2 раза изменися
        коэффициент амнистирования для 2-го оффера. Раньше он был 900000 / 1200000 = 0.75, а стал 450000 / 1200000 = 0.375
        '''
        response = self.report.request_json(
            'place=cpa_shop_incut&hid=1011&min-num-doc=0&show-urls=cpa,promotion&rearr-factors=market_premium_ads_gallery_shop_incut_vendor_fee_correction=0.5;'
            'market_premium_ads_gallery_shop_incut_vendor_bid_to_fee_coef=0.05'  # noqa
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'shop': {'id': 2},
                        'model': {'id': 101},
                    },
                    {
                        'shop': {'id': 1},
                        'model': {'id': 102},
                    },
                    {
                        'shop': {'id': 1},
                        'model': {'id': 103},
                    },
                ]
            },
            preserve_order=True,
        )

        self.click_log.expect(clicktype=ClickType.CPA, shop_id=2, hyper_id=101, shop_fee=500, shop_fee_ab=378)
        self.click_log.expect(clicktype=ClickType.PROMOTION, shop_id=2, hyper_id=101, cb_vnd=10, cp_vnd=4, cb=0, cp=0)

        self.click_log.expect(clicktype=ClickType.CPA, shop_id=1, hyper_id=102, shop_fee=400, shop_fee_ab=150)
        self.click_log.expect(clicktype=ClickType.PROMOTION, shop_id=1, hyper_id=102, cb_vnd=0, cp_vnd=0, cb=0, cp=0)

        self.click_log.expect(clicktype=ClickType.CPA, shop_id=1, hyper_id=103, shop_fee=0, shop_fee_ab=0)
        self.click_log.expect(clicktype=ClickType.PROMOTION, shop_id=1, hyper_id=103, cb_vnd=50, cp_vnd=1, cb=0, cp=0)

    def test_vendor_cpa_auction_bid2fee_coef(self):
        '''
        Тестируем флаг market_premium_ads_gallery_shop_incut_vendor_bid_to_fee_coef
        Он меняет коэффициент преобразования вендорской ставки в fee. По умолчанию, значение этого коэффициент 0.05.
        Зависимость обратная. То есть увеличивая коэффициент в 2 раза мы уменьшаем итоговое fee в 2 раза.
        В результате в этом тесте офферы стали набирать в 4 раза больше очков CPA_SHOP_INCUT, чем в предыдущем тесте.
        Но итоговое соотношение не изменилось. В результате коэффициенты амнистирования совпадают.
        Но так как мы вендорскую ставку не уменьшали, то с вендора списывается в 2 раза больше денег, чем в предыдущем тесте
        '''
        response = self.report.request_json(
            'place=cpa_shop_incut&hid=1011&min-num-doc=0&show-urls=cpa,promotion&rearr-factors=market_premium_ads_gallery_shop_incut_vendor_bid_to_fee_coef=0.1'
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'shop': {'id': 2},
                        'model': {'id': 101},
                    },
                    {
                        'shop': {'id': 1},
                        'model': {'id': 102},
                    },
                    {
                        'shop': {'id': 1},
                        'model': {'id': 103},
                    },
                ]
            },
            preserve_order=True,
        )

        self.click_log.expect(clicktype=ClickType.CPA, shop_id=2, hyper_id=101, shop_fee=500, shop_fee_ab=378)
        self.click_log.expect(clicktype=ClickType.PROMOTION, shop_id=2, hyper_id=101, cb_vnd=10, cp_vnd=8, cb=0, cp=0)

        self.click_log.expect(clicktype=ClickType.CPA, shop_id=1, hyper_id=102, shop_fee=400, shop_fee_ab=150)
        self.click_log.expect(clicktype=ClickType.PROMOTION, shop_id=1, hyper_id=102, cb_vnd=0, cp_vnd=0, cb=0, cp=0)

        self.click_log.expect(clicktype=ClickType.CPA, shop_id=1, hyper_id=103, shop_fee=0, shop_fee_ab=0)
        self.click_log.expect(clicktype=ClickType.PROMOTION, shop_id=1, hyper_id=103, cb_vnd=50, cp_vnd=1, cb=0, cp=0)

    def test_cpa_incut_disabled(self):
        """
        Проверяем работу флага, выключающего врезку
        """
        response = self.report.request_json(
            'place=cpa_shop_incut&hid=1011&min-num-doc=0&show-urls=cpa,promotion'
            '&rearr-factors=market_money_use_vendor_cpa_bid_on_cpa_incut=1;market_premium_ads_gallery_disable=1;market_premium_ads_gallery_shop_incut_logarithm_price=0'
        )

        self.assertFragmentIn(response, {"results": ElementCount(0)})

    def test_cpa_incut_sku_required(self):
        response = self.report.request_json(
            'place=cpa_shop_incut&hid=1011&min-num-doc=0&show-urls=cpa,promotion'
            '&rearr-factors=market_money_use_vendor_cpa_bid_on_cpa_incut=1;market_premium_ads_gallery_shop_incut_logarithm_price=0&sku-required=1'
        )

        self.assertFragmentIn(response, {"results": ElementCount(0)})

    def test_cpa_incut_in_android(self):
        """
        Проверяем работу параметра client=ANDROID: по умолчанию, в приложеньках (client == ANDROID || client == IOS) врезку показываем
        """
        response = self.report.request_json(
            'place=cpa_shop_incut&hid=1011&min-num-doc=0&show-urls=cpa,promotion' '&client=ANDROID'
        )

        self.assertFragmentIn(response, {"results": ElementCount(3)})

        """
        Не показываем врезку в приложении под флагом
        """
        response = self.report.request_json(
            'place=cpa_shop_incut&hid=1011&min-num-doc=0&show-urls=cpa,promotion'
            '&client=ANDROID&rearr-factors=market_premium_ads_gallery_disable_in_application=1'
        )

        self.assertFragmentIn(response, {"results": ElementCount(0)})

    def test_cpa_incut_in_ios(self):
        """
        Проверяем работу параметра client=IOS: по умолчанию, в приложеньках (client == ANDROID || client == IOS) врезку показываем
        """
        response = self.report.request_json(
            'place=cpa_shop_incut&hid=1011&min-num-doc=0&show-urls=cpa,promotion' '&client=IOS'
        )

        self.assertFragmentIn(response, {"results": ElementCount(3)})

        """
        Не показываем врезку в приложении под флагом
        """
        response = self.report.request_json(
            'place=cpa_shop_incut&hid=1011&min-num-doc=0&show-urls=cpa,promotion'
            '&client=IOS&rearr-factors=market_premium_ads_gallery_disable_in_application=1'
        )

        self.assertFragmentIn(response, {"results": ElementCount(0)})

    def test_cpa_incut_disabled_on_blacklist_rearr(self):
        """
        Проверяем работу флага market_output_advert_request_blacklist_texts,
        задающего дополнительные фразы в запросе
        """
        response = self.report.request_json(
            'place=cpa_shop_incut&text=венерианская вода&show-urls=cpa,promotion&min-num-doc=0&numdoc=1'
        )
        self.assertFragmentIn(response, {"results": ElementCount(1)})
        response = self.report.request_json(
            'place=cpa_shop_incut&text=венерианская вода&show-urls=cpa,promotion&min-num-doc=0&numdoc=1'
            '&rearr-factors=market_output_advert_request_blacklist_texts=венерианская'
        )
        self.assertFragmentIn(response, {"results": ElementCount(0)})
        response = self.report.request_json(
            'place=cpa_shop_incut&text=венерианская вода&show-urls=cpa,promotion&min-num-doc=0&numdoc=1'
            '&rearr-factors=market_output_advert_request_blacklist_texts=венерианская:33'
        )
        self.assertFragmentIn(response, {"results": ElementCount(1)})
        response = self.report.request_json(
            'place=cpa_shop_incut&text=венерианская вода&show-urls=cpa,promotion&min-num-doc=0&numdoc=1'
            '&rearr-factors=market_output_advert_request_blacklist_texts=венерианская:33:2'
        )
        self.assertFragmentIn(response, {"results": ElementCount(0)})

    def test_cpa_incut_disabled_on_blacklist_hid_rearr(self):
        """
        Проверяем работу флага market_output_advert_request_blacklist_hids,
        задающего дополнительные hid в запросе
        """
        response = self.report.request_json('place=cpa_shop_incut&hid=1011&min-num-doc=0&show-urls=cpa,promotion')
        self.assertFragmentIn(response, {"results": ElementCount(3)})
        response = self.report.request_json(
            'place=cpa_shop_incut&hid=1011&min-num-doc=0&show-urls=cpa,promotion'
            '&rearr-factors=market_output_advert_request_blacklist_hids=1011'
        )
        self.assertFragmentIn(response, {"results": ElementCount(0)})
        response = self.report.request_json(
            'place=cpa_shop_incut&hid=1011&min-num-doc=0&show-urls=cpa,promotion'
            '&rearr-factors=market_output_advert_request_blacklist_hids=1011:33'
        )
        self.assertFragmentIn(response, {"results": ElementCount(3)})
        response = self.report.request_json(
            'place=cpa_shop_incut&hid=1011&min-num-doc=0&show-urls=cpa,promotion'
            '&rearr-factors=market_output_advert_request_blacklist_hids=1011:33:2'
        )
        self.assertFragmentIn(response, {"results": ElementCount(0)})
        response = self.report.request_json(
            'place=cpa_shop_incut&hid=1011&min-num-doc=0&show-urls=cpa,promotion'
            '&rearr-factors=market_output_advert_request_blacklist_hids=+1011'
        )
        self.assertFragmentIn(response, {"results": ElementCount(0)})
        response = self.report.request_json(
            'place=cpa_shop_incut&hid=1011&min-num-doc=0&show-urls=cpa,promotion'
            '&rearr-factors=market_output_advert_request_blacklist_hids=+1009'
        )
        self.assertFragmentIn(response, {"results": ElementCount(3)})
        response = self.report.request_json(
            'place=cpa_shop_incut&hid=1011&min-num-doc=0&show-urls=cpa,promotion'
            '&rearr-factors=market_output_advert_request_blacklist_hids=1009'
        )
        self.assertFragmentIn(response, {"results": ElementCount(0)})

    @classmethod
    def prepare_incut_black_list_fb(cls):
        cls.index.incut_black_list_fb += [
            IncutBlackListFb(subtreeHids=[318], inclids=['PremiumAds']),
            IncutBlackListFb(texts=['bnw 528', 'бнв 528'], inclids=['AllAdv']),
        ]
        cls.index.offers += [
            Offer(
                hyperid=318101,
                hid=318,
                fee=110,
                fesh=1,
                ts=100400,
                price=77990,
                title='Смартфон bnw 528',
                cpa=Offer.CPA_REAL,
            ),
            Offer(
                hyperid=318102,
                hid=318,
                fee=100,
                fesh=2,
                ts=100400,
                price=69800,
                title='Смартфон bnw 528 mini',
                cpa=Offer.CPA_REAL,
            ),
            Offer(
                hyperid=318103,
                hid=318,
                fee=130,
                fesh=3,
                ts=100400,
                price=99990,
                title='Смартфон bnw 528 Pro',
                cpa=Offer.CPA_REAL,
            ),
        ]
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 100400).respond(0.4)

    def test_cpa_incut_disabled_on_blacklist_fb(self):
        """
        Проверяем работу флага, выключающего врезку при наличии фраз из блэклиста в запросе
        """
        response = self.report.request_json(
            'place=cpa_shop_incut&text=bnw 528&min-num-doc=0'
            '&rearr-factors=market_output_advert_request_blacklist_fb=0'
        )
        self.assertFragmentIn(response, {"results": ElementCount(3)})
        response_incut_off = self.report.request_json(
            'place=cpa_shop_incut&text=bnw 528&min-num-doc=0'
            '&rearr-factors=market_output_advert_request_blacklist_fb=1'
        )
        self.assertFragmentIn(response_incut_off, {"results": ElementCount(0)})
        response_incut_off = self.report.request_json(
            'place=cpa_shop_incut&text=продам bnw и куплю гараж за 528 тысяч гульденов&min-num-doc=0'
            '&rearr-factors=market_output_advert_request_blacklist_fb=1'
        )
        self.assertFragmentIn(response_incut_off, {"results": ElementCount(0)})
        response_incut_off_russian = self.report.request_json(
            'place=cpa_shop_incut&text=528 бнв&min-num-doc=0'
            '&rearr-factors=market_output_advert_request_blacklist_fb=1'
        )
        self.assertFragmentIn(response_incut_off_russian, {"results": ElementCount(0)})

    def test_cpa_incut_disabled_on_blacklist_hid_fb(self):
        """
        Проверяем работу флага, выключающего врезку при наличии hid из блэклиста в запросе
        """
        response = self.report.request_json(
            'place=cpa_shop_incut&hid=318&min-num-doc=0' '&rearr-factors=market_output_advert_request_blacklist_fb=0'
        )
        self.assertFragmentIn(response, {"results": ElementCount(3)})
        response_incut_off = self.report.request_json(
            'place=cpa_shop_incut&hid=318&min-num-doc=0' '&rearr-factors=market_output_advert_request_blacklist_fb=1'
        )
        self.assertFragmentIn(response_incut_off, {"results": ElementCount(0)})

    def test_cpa_incut_enabled_on_whitelist_hid_exp(self):
        """
        Проверяем работу флага, который добавляет категорию в white list
        """
        response = self.report.request_json(
            'place=cpa_shop_incut&hid=318&min-num-doc=0' '&rearr-factors=market_output_advert_request_blacklist_fb=1'
        )
        self.assertFragmentIn(response, {"results": ElementCount(0)})

        response = self.report.request_json(
            'place=cpa_shop_incut&hid=318&min-num-doc=0'
            '&rearr-factors=market_output_advert_request_blacklist_fb=1;market_output_advert_request_whitelist_hids=318:2'
        )
        self.assertFragmentIn(response, {"results": ElementCount(3)})

    @classmethod
    def prepare_conversion(cls):
        cls.index.offers += [
            Offer(
                hyperid=501,
                hid=1051,
                fesh=1,
                price=11000,
                fee=100,
                ts=500501,
                title='Блины с мясом',
                cpa=Offer.CPA_REAL,
            ),
            Offer(
                hyperid=501,
                hid=1051,
                fesh=2,
                price=11000,
                fee=500,
                ts=500511,
                title='Блины со сметаной',
                cpa=Offer.CPA_REAL,
            ),
            Offer(
                hyperid=502,
                hid=1051,
                fesh=1,
                price=11000,
                fee=400,
                ts=500502,
                title='Блины с грибами',
                cpa=Offer.CPA_REAL,
            ),
            Offer(
                hyperid=503,
                hid=1051,
                fesh=1,
                price=11000,
                fee=300,
                ts=500503,
                title='Блины с сиропом',
                cpa=Offer.CPA_REAL,
            ),
            Offer(
                hyperid=504,
                hid=1051,
                fesh=1,
                price=11000000,
                fee=200,
                ts=500504,
                title='Блины с припёком',
                cpa=Offer.CPA_REAL,
            ),
        ]
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 500501).respond(0.35)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 500511).respond(0.45)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 500502).respond(0.55)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 500503).respond(0.70)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 500504).respond(0.35)
        cls.index.ctr.orders.white_model += [EntityCtr(501, 45, 348)]
        cls.index.ctr.orders.white_model += [EntityCtr(502, 46, 351)]
        cls.index.ctr.orders.white_model += [EntityCtr(504, 48, 353)]

    def test_conversion(self):
        """
        Сначала запрашиваем cpa_shop_incut без флагов конверсии.
        """
        response = self.report.request_json(
            'place=cpa_shop_incut&rids=213&hid=1051&min-num-doc=0&show-urls=cpa,promotion&debug=da'
            '&rearr-factors=market_premium_ads_gallery_shop_incut_logarithm_price=0'
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'entity': 'offer',
                        'debug': {
                            'fesh': 1,
                            'properties': {
                                'CPA_SHOP_INCUT': '770000000',
                                'FEE': '200',
                                'MODEL_ID': '504',
                                'OFFER_CONVERSION': NoKey('OFFER_CONVERSION'),
                            },
                        },
                    },
                    {
                        'entity': 'offer',
                        'debug': {
                            'fesh': 2,
                            'properties': {
                                'CPA_SHOP_INCUT': '2475000',
                                'FEE': '500',
                                'MODEL_ID': '501',
                                'OFFER_CONVERSION': NoKey('OFFER_CONVERSION'),
                            },
                        },
                    },
                    {
                        'entity': 'offer',
                        'debug': {
                            'fesh': 1,
                            'properties': {
                                'CPA_SHOP_INCUT': '2420000',
                                'FEE': '400',
                                'MODEL_ID': '502',
                                'OFFER_CONVERSION': NoKey('OFFER_CONVERSION'),
                            },
                        },
                    },
                    {
                        'entity': 'offer',
                        'debug': {
                            'fesh': 1,
                            'properties': {
                                'CPA_SHOP_INCUT': '2310000',
                                'FEE': '300',
                                'MODEL_ID': '503',
                                'OFFER_CONVERSION': NoKey('OFFER_CONVERSION'),
                            },
                        },
                    },
                ]
            },
            preserve_order=True,
        )

        """
        Запрашиваем cpa_shop_incut с флагом, cpa_shop_incut_enable_conversion. В выдаче появится конверсия и значение
        CPA_SHOP_INCUT будет умножено на значение конверсии. В fullFormulaInfo будет значение ML формулы с тэгом "CpaBuy".
        А в properties будет это же значение в OFFER_CONVERSION. В лайт-тестах ML формулы не вычисляются, а подпираются через
        cls.matrixnet, поэтому сами значения проверить не получится, но видно, что они были учтены в формулах и они есть в выдаче
        В результате оффер, с hyperid=503, поднялся на второе место, потому что у него самая высокая конверсия
        Заодно убеждаемся, что флаг market_premium_ads_gallery_shop_incut_logarithm_price перебивается флагом cpa_shop_incut_enable_conversion
        """
        response = self.report.request_json(
            'place=cpa_shop_incut&rids=213&hid=1051&min-num-doc=0&show-urls=cpa,promotion&debug=da'
            '&rearr-factors=cpa_shop_incut_enable_conversion=1;'
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'entity': 'offer',
                        'debug': {
                            'fesh': 1,
                            'fullFormulaInfo': [
                                {'tag': "Default", 'value': Round(0.35)},
                                {'tag': "CpaBuy", 'value': Round(0.35)},
                            ],
                            'properties': {
                                'CPA_SHOP_INCUT': '269500000',
                                'FEE': '200',
                                'MODEL_ID': '504',
                                'OFFER_CONVERSION': Round(0.35),
                            },
                        },
                    },
                    {
                        'entity': 'offer',
                        'debug': {
                            'fesh': 1,
                            'fullFormulaInfo': [
                                {'tag': "Default", 'value': Round(0.7)},
                                {'tag': "CpaBuy", 'value': Round(0.7)},
                            ],
                            'properties': {
                                'CPA_SHOP_INCUT': '1617000',
                                'FEE': '300',
                                'MODEL_ID': '503',
                                'OFFER_CONVERSION': Round(0.7),
                            },
                        },
                    },
                    {
                        'entity': 'offer',
                        'debug': {
                            'fesh': 1,
                            'fullFormulaInfo': [
                                {'tag': "Default", 'value': Round(0.55)},
                                {'tag': "CpaBuy", 'value': Round(0.55)},
                            ],
                            'properties': {
                                'CPA_SHOP_INCUT': '1331000',
                                'FEE': '400',
                                'MODEL_ID': '502',
                                'OFFER_CONVERSION': Round(0.55),
                            },
                        },
                    },
                    {
                        'entity': 'offer',
                        'debug': {
                            'fesh': 2,
                            'fullFormulaInfo': [
                                {'tag': "Default", 'value': Round(0.45)},
                                {'tag': "CpaBuy", 'value': Round(0.45)},
                            ],
                            'properties': {
                                'CPA_SHOP_INCUT': '1113750',
                                'FEE': '500',
                                'MODEL_ID': '501',
                                'OFFER_CONVERSION': Round(0.45),
                            },
                        },
                    },
                ]
            },
            preserve_order=True,
        )

        """
        Запрашиваем cpa_shop_incut с флагом, cpa_shop_incut_min_conversion. Офферы с конверсией меньше заданной будут отброшены
        """
        response = self.report.request_json(
            'place=cpa_shop_incut&rids=213&hid=1051&min-num-doc=0&show-urls=cpa,promotion&debug=da'
            '&rearr-factors=cpa_shop_incut_min_conversion=0.5;market_premium_ads_gallery_shop_incut_logarithm_price=0'
        )
        self.assertFragmentIn(
            response,
            {
                'debug': {'brief': {'filters': {'CPA_SHOP_INCUT_BY_CONVERSION': 3}}},
                'results': [
                    {
                        'entity': 'offer',
                        'debug': {
                            'fesh': 1,
                            'properties': {
                                'CPA_SHOP_INCUT': '2420000',
                                'FEE': '400',
                                'OFFER_CONVERSION': NoKey('OFFER_CONVERSION'),
                            },
                        },
                    },
                    {
                        'entity': 'offer',
                        'debug': {
                            'fesh': 1,
                            'properties': {
                                'CPA_SHOP_INCUT': '2310000',
                                'FEE': '300',
                                'MODEL_ID': '503',
                                'OFFER_CONVERSION': NoKey('OFFER_CONVERSION'),
                            },
                        },
                    },
                ],
            },
            preserve_order=True,
        )

    def test_white_conversion(self):
        '''
        Проверяем работу конверсии из поиска. Она включается флагом market_premium_ads_gallery_shop_incut_enable_white_conversion
        В тесте конверсия заведеня для 501, 502 и 504 модели. Для 503 конверсии нет, должна быть использована конверсия по умолчанию.
        Значение по умолчанию берем 0.00000001
        '''
        response = self.report.request_json(
            'place=cpa_shop_incut&rids=213&hid=1051&min-num-doc=0&show-urls=cpa,promotion&debug=da'
            '&rearr-factors=market_premium_ads_gallery_shop_incut_enable_white_conversion=1;'
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'entity': 'offer',
                        'debug': {
                            'fesh': 1,
                            'properties': {
                                'CPA_SHOP_INCUT': '1623334912',
                                'FEE': '200',
                                'MODEL_ID': '504',
                                'OFFER_CONVERSION': Round(0.136),
                            },
                        },
                    },
                    {
                        'entity': 'offer',
                        'debug': {
                            'fesh': 2,
                            'properties': {
                                'CPA_SHOP_INCUT': '320043104',
                                'FEE': '500',
                                'MODEL_ID': '501',
                                'OFFER_CONVERSION': Round(0.129),
                            },
                        },
                    },
                    {
                        'entity': 'offer',
                        'debug': {
                            'fesh': 1,
                            'properties': {
                                'CPA_SHOP_INCUT': '317151008',
                                'FEE': '400',
                                'MODEL_ID': '502',
                                'OFFER_CONVERSION': Round(0.131),
                            },
                        },
                    },
                    {
                        'entity': 'offer',
                        'debug': {
                            'fesh': 1,
                            'properties': {
                                'CPA_SHOP_INCUT': '23',
                                'FEE': '300',
                                'MODEL_ID': '503',
                                # У 503 модели нет конверсии
                                'OFFER_CONVERSION': Round(0.00000001),
                            },
                        },
                    },
                ]
            },
            preserve_order=True,
        )

    def test_sigmoid(self):
        """
        Проверяем работу флага market_premium_ads_gallery_shop_incut_enable_sigmoid
        """
        response = self.report.request_json(
            'place=cpa_shop_incut&rids=213&hid=1051&min-num-doc=0&show-urls=cpa,promotion&debug=da'
            '&rearr-factors=market_premium_ads_gallery_shop_incut_enable_sigmoid=1;'
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'entity': 'offer',
                        'debug': {
                            'fesh': 1,
                            'properties': {
                                'CPA_SHOP_INCUT': '113749',
                                'FEE': '200',
                                'MODEL_ID': '504',
                            },
                        },
                    },
                    {
                        'entity': 'offer',
                        'debug': {
                            'fesh': 1,
                            'properties': {
                                'CPA_SHOP_INCUT': '108204',
                                'FEE': '300',
                                'MODEL_ID': '503',
                            },
                        },
                    },
                    {
                        'entity': 'offer',
                        'debug': {
                            'fesh': 1,
                            'properties': {
                                'CPA_SHOP_INCUT': '94416',
                                'FEE': '400',
                                'MODEL_ID': '502',
                            },
                        },
                    },
                    {
                        'entity': 'offer',
                        'debug': {
                            'fesh': 2,
                            'properties': {
                                'CPA_SHOP_INCUT': '84547',
                                'FEE': '500',
                                'MODEL_ID': '501',
                            },
                        },
                    },
                ]
            },
            preserve_order=True,
        )

        """
        Проверяем настройку параметров сигмоиды флагом market_tweak_search_auction_cpa_shop_incut_params
        Значения CPA_SHOP_INCUT получились немного другими
        """
        response = self.report.request_json(
            'place=cpa_shop_incut&rids=213&hid=1051&min-num-doc=0&show-urls=cpa,promotion&debug=da'
            '&rearr-factors=market_premium_ads_gallery_shop_incut_enable_sigmoid=1;market_tweak_search_auction_cpa_shop_incut_params=22,0.0015,1'
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'entity': 'offer',
                        'debug': {
                            'fesh': 1,
                            'properties': {
                                'CPA_SHOP_INCUT': '419999',
                                'FEE': '200',
                                'MODEL_ID': '504',
                            },
                        },
                    },
                    {
                        'entity': 'offer',
                        'debug': {
                            'fesh': 1,
                            'properties': {
                                'CPA_SHOP_INCUT': '256776',
                                'FEE': '300',
                                'MODEL_ID': '503',
                            },
                        },
                    },
                    {
                        'entity': 'offer',
                        'debug': {
                            'fesh': 1,
                            'properties': {
                                'CPA_SHOP_INCUT': '247705',
                                'FEE': '400',
                                'MODEL_ID': '502',
                            },
                        },
                    },
                    {
                        'entity': 'offer',
                        'debug': {
                            'fesh': 2,
                            'properties': {
                                'CPA_SHOP_INCUT': '238343',
                                'FEE': '500',
                                'MODEL_ID': '501',
                            },
                        },
                    },
                ]
            },
            preserve_order=True,
        )

    def test_text_conversion(self):
        """
        Повторяем тест test_conversion. Но делаем текстовый поисковый запрос
        """
        response = self.report.request_json(
            'place=cpa_shop_incut&rids=213&text=блины&min-num-doc=0&show-urls=cpa,promotion&debug=da'
            '&rearr-factors=market_premium_ads_gallery_shop_incut_logarithm_price=0'
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'entity': 'offer',
                        'debug': {
                            'fesh': 1,
                            'properties': {
                                'CPA_SHOP_INCUT': '770000000',
                                'FEE': '200',
                                'MODEL_ID': '504',
                                'OFFER_CONVERSION': NoKey('OFFER_CONVERSION'),
                            },
                        },
                    },
                    {
                        'entity': 'offer',
                        'debug': {
                            'fesh': 2,
                            'properties': {
                                'CPA_SHOP_INCUT': '2475000',
                                'FEE': '500',
                                'MODEL_ID': '501',
                                'OFFER_CONVERSION': NoKey('OFFER_CONVERSION'),
                            },
                        },
                    },
                    {
                        'entity': 'offer',
                        'debug': {
                            'fesh': 1,
                            'properties': {
                                'CPA_SHOP_INCUT': '2420000',
                                'FEE': '400',
                                'MODEL_ID': '502',
                                'OFFER_CONVERSION': NoKey('OFFER_CONVERSION'),
                            },
                        },
                    },
                    {
                        'entity': 'offer',
                        'debug': {
                            'fesh': 1,
                            'properties': {
                                'CPA_SHOP_INCUT': '2310000',
                                'FEE': '300',
                                'MODEL_ID': '503',
                                'OFFER_CONVERSION': NoKey('OFFER_CONVERSION'),
                            },
                        },
                    },
                ]
            },
            preserve_order=True,
        )

        response = self.report.request_json(
            'place=cpa_shop_incut&rids=213&text=блины&min-num-doc=0&show-urls=cpa,promotion&debug=da'
            '&rearr-factors=cpa_shop_incut_enable_conversion=1;market_premium_ads_gallery_shop_incut_logarithm_price=0'
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'entity': 'offer',
                        'debug': {
                            'fesh': 1,
                            'fullFormulaInfo': [
                                {'tag': "Default", 'value': Round(0.35)},
                                {'tag': "CpaBuy", 'value': Round(0.35)},
                            ],
                            'properties': {
                                'CPA_SHOP_INCUT': '269500000',
                                'FEE': '200',
                                'MODEL_ID': '504',
                                'OFFER_CONVERSION': Round(0.35),
                            },
                        },
                    },
                    {
                        'entity': 'offer',
                        'debug': {
                            'fesh': 1,
                            'fullFormulaInfo': [
                                {'tag': "Default", 'value': Round(0.7)},
                                {'tag': "CpaBuy", 'value': Round(0.7)},
                            ],
                            'properties': {
                                'CPA_SHOP_INCUT': '1617000',
                                'FEE': '300',
                                'MODEL_ID': '503',
                                'OFFER_CONVERSION': Round(0.7),
                            },
                        },
                    },
                    {
                        'entity': 'offer',
                        'debug': {
                            'fesh': 1,
                            'fullFormulaInfo': [
                                {'tag': "Default", 'value': Round(0.55)},
                                {'tag': "CpaBuy", 'value': Round(0.55)},
                            ],
                            'properties': {
                                'CPA_SHOP_INCUT': '1331000',
                                'FEE': '400',
                                'MODEL_ID': '502',
                                'OFFER_CONVERSION': Round(0.55),
                            },
                        },
                    },
                    {
                        'entity': 'offer',
                        'debug': {
                            'fesh': 2,
                            'fullFormulaInfo': [
                                {'tag': "Default", 'value': Round(0.45)},
                                {'tag': "CpaBuy", 'value': Round(0.45)},
                            ],
                            'properties': {
                                'CPA_SHOP_INCUT': '1113750',
                                'FEE': '500',
                                'MODEL_ID': '501',
                                'OFFER_CONVERSION': Round(0.45),
                            },
                        },
                    },
                ]
            },
            preserve_order=True,
        )

        response = self.report.request_json(
            'place=cpa_shop_incut&rids=213&text=блины&min-num-doc=0&show-urls=cpa,promotion&debug=da'
            '&rearr-factors=cpa_shop_incut_min_conversion=0.5;market_premium_ads_gallery_shop_incut_logarithm_price=0'
        )
        self.assertFragmentIn(
            response,
            {
                'debug': {'brief': {'filters': {'CPA_SHOP_INCUT_BY_CONVERSION': 3}}},
                'results': [
                    {
                        'entity': 'offer',
                        'debug': {
                            'fesh': 1,
                            'properties': {
                                'CPA_SHOP_INCUT': '2420000',
                                'FEE': '400',
                                'OFFER_CONVERSION': NoKey('OFFER_CONVERSION'),
                            },
                        },
                    },
                    {
                        'entity': 'offer',
                        'debug': {
                            'fesh': 1,
                            'properties': {
                                'CPA_SHOP_INCUT': '2310000',
                                'FEE': '300',
                                'MODEL_ID': '503',
                                'OFFER_CONVERSION': NoKey('OFFER_CONVERSION'),
                            },
                        },
                    },
                ],
            },
            preserve_order=True,
        )

    def test_cpa_formula_setup(self):
        """
        Проверям найстройку ML формулы для конверсии
        Сначала делаем обычный запрос и проверяем, что без флагов нет вычисления ML формулы
        """
        response = self.report.request_json(
            'place=cpa_shop_incut&rids=213&text=блины&min-num-doc=0&show-urls=cpa,promotion&debug=da' '&rearr-factors='
        )
        self.assertFragmentIn(
            response, {'fullFormulaInfo': [{'tag': 'Default'}]}, preserve_order=True, allow_different_len=False
        )

        """
        Проверяем, что с флагом cpa_shop_incut_enable_conversion ML формула вычисляется
        """
        response = self.report.request_json(
            'place=cpa_shop_incut&rids=213&text=блины&min-num-doc=0&show-urls=cpa,promotion&debug=da'
            '&rearr-factors=cpa_shop_incut_enable_conversion=1'
        )
        self.assertFragmentIn(
            response,
            {
                'entity': 'offer',
                'titles': {'raw': 'Блины с припёком'},
                'debug': {
                    'fesh': 1,
                    'offerTitle': 'Блины с припёком',
                    'fullFormulaInfo': [
                        {'tag': 'CpaBuy', 'name': 'MNA_P_Purchase_log_loss_full_factors_6w_20210311'},
                    ],
                },
            },
        )

        """
        Выбираем другую формулу
        """
        response = self.report.request_json(
            'place=cpa_shop_incut&rids=213&text=блины&min-num-doc=0&show-urls=cpa,promotion&debug=da'
            '&rearr-factors=cpa_shop_incut_enable_conversion=1;market_premium_ads_gallery_shop_incut_cpa_formula=MNA_HybridAuctionCpcCtr2430'
        )
        self.assertFragmentIn(
            response,
            {
                'entity': 'offer',
                'titles': {'raw': 'Блины с припёком'},
                'debug': {
                    'fesh': 1,
                    'offerTitle': 'Блины с припёком',
                    'fullFormulaInfo': [
                        {'tag': "CpaBuy", 'name': 'MNA_HybridAuctionCpcCtr2430'},
                    ],
                },
            },
        )

    @classmethod
    def prepare_logarithm(cls):
        cls.index.offers += [
            Offer(hyperid=601, hid=1061, fesh=1, price=11000, fee=100, cpa=Offer.CPA_REAL),
            Offer(hyperid=601, hid=1061, fesh=2, price=11000, fee=500, cpa=Offer.CPA_REAL),
            Offer(hyperid=602, hid=1061, fesh=1, price=11000, fee=400, cpa=Offer.CPA_REAL),
            Offer(hyperid=603, hid=1061, fesh=1, price=11000, fee=300, cpa=Offer.CPA_REAL),
            Offer(hyperid=604, hid=1061, fesh=1, price=11000000, fee=200, cpa=Offer.CPA_REAL),
        ]

    def test_prun_panther_settings(self):
        """
        Проверяем работу флагов настройки прюнинга и пантерного топа.
        """

        """
        Добавляем флаг market_premium_ads_gallery_shop_incut_prun_count с настройками прюнинга
        Судя по этому(@see https://a.yandex-team.ru/arc/trunk/arcadia/market/report/library/tweak_basesearch_behavior/tweak_basesearch_behavior.cpp?rev=r8219117#L101)
        коду в выдаче pruncount должно быть примерно равно 2/3 от переданного параметра
        """
        response = self.report.request_json(
            'place=cpa_shop_incut&rids=213&text=блины&min-num-doc=0&show-urls=cpa,promotion&debug=da'
            '&rearr-factors=market_premium_ads_gallery_shop_incut_prun_count=5000;market_premium_ads_gallery_shop_incut_enable_panther=0'
        )
        self.assertFragmentIn(
            response,
            {
                'debug': {
                    'report': {'context': {'collections': {'*': {'pron': ["tbs200000", "prune", "pruncount3334"]}}}}
                }
            },
        )

        """
        К предыдущему запросу добавляем флаг market_premium_ads_gallery_shop_incut_enable_panther, который включает пантерный индекс.
        В выдаче будут параметры пантерного индекса, размер пантерного топа по умолчанию - 250
        """
        response = self.report.request_json(
            'place=cpa_shop_incut&rids=213&text=блины&min-num-doc=0&show-urls=cpa,promotion&debug=da'
            '&rearr-factors=market_premium_ads_gallery_shop_incut_prun_count=5000;'
            'market_premium_ads_gallery_shop_incut_enable_panther=1;market_premium_ads_gallery_shop_incut_panther_coef=1'
        )
        self.assertFragmentIn(
            response,
            {
                'debug': {
                    'report': {
                        'context': {
                            'collections': {
                                '*': {'pron': ["tbs200000", "prune", "pruncount3334"]},
                                'SHOP': {'pron': ["panther_top_size_=250"]},
                            }
                        }
                    }
                }
            },
        )

        """
        Уменьшаем размер пантерного топа и прюнинга.
        К предыдущему запросу добавляем флаг market_premium_ads_gallery_shop_incut_panther_coef, который изменяет размер пантерного топа.
        А также заодно проверяем изменение размера прюнинга
        В выдаче будут параметры пантерного индекса, размер пантерного топа по умолчанию - 250
        """
        response = self.report.request_json(
            'place=cpa_shop_incut&rids=213&text=блины&min-num-doc=0&show-urls=cpa,promotion&debug=da'
            '&rearr-factors=market_premium_ads_gallery_shop_incut_prun_count=3000;'
            'market_premium_ads_gallery_shop_incut_enable_panther=1;'
            'market_premium_ads_gallery_shop_incut_panther_coef=0.5'
        )
        self.assertFragmentIn(
            response,
            {
                'debug': {
                    'report': {
                        'context': {
                            'collections': {
                                '*': {'pron': ["tbs200000", "prune", "pruncount2000"]},
                                'SHOP': {'pron': ["panther_top_size_=125"]},
                            }
                        }
                    }
                }
            },
        )

    def test_text_fix(self):
        """
        Проверяем что поисковый запрос - корректный и совпадает с place=prime
        """

        response = self.report.request_json(
            'place=cpa_shop_incut&rids=213&text=пульсометр купить медицинский&min-num-doc=0&show-urls=cpa,promotion&debug=da'
        )
        self.assertFragmentIn(
            response,
            {
                'debug': {
                    'report': {
                        'context': {
                            'collections': {
                                'SHOP': {
                                    'text': [
                                        "пульсометр::1124746168 &&/(-32768 32768) купить::3022 &&/(-32768 32768) медицинский::1124746168 is_b2c:\"1\" << (blue_doctype:\"b\" | blue_doctype:\"w_cpa\")"
                                    ]
                                },
                            }
                        }
                    }
                }
            },
        )

        """
        Делаем запрос в prime. Поле текст такое же как и в cpa_shop_incut.
        """
        response = self.report.request_json(
            'place=prime&rids=213&text=пульсометр купить медицинский&min-num-doc=0&show-urls=cpa,promotion&debug=da'
        )
        self.assertFragmentIn(
            response,
            {
                'debug': {
                    'report': {
                        'context': {
                            'collections': {
                                'SHOP': {
                                    'text': [
                                        "пульсометр::1124746168 &&/(-32768 32768) купить::3022 &&/(-32768 32768) медицинский::1124746168 is_b2c:\"1\" is_not_empty_metadoc:\"1\" << (blue_doctype:\"b\" | blue_doctype:\"w\" | blue_doctype:\"m\")"  # noqa
                                    ]
                                },
                            }
                        }
                    }
                }
            },
        )

    @classmethod
    def prepare_cpa_shop_incut_force_filter_adult_for_incuts(cls):
        cls.index.offers += [
            Offer(
                fesh=866 + i,
                hyperid=866 + i,
                hid=16155466,
                fee=890 + i,
                price=100,
                cpa=Offer.CPA_REAL,
                adult=1,
                title="Пенетрылда {} см".format(i * 2 + 15),
            )
            for i in range(1, 10)
        ]

    def test_cpa_shop_incut_force_filter_adult_for_incuts(self):
        request = (
            "place=cpa_shop_incut&pp=18&text=пенетрылда&numdoc=5&adult=1&"
            "rearr-factors=market_premium_ads_gallery_min_num_doc_to_request_from_base=1"
            ";market_adult_incuts_on_adult_hids_only=0;market_force_filter_adult_for_incuts="
        )
        response = self.report.request_json(request + "0")
        self.assertFragmentIn(response, {'results': ElementCount(5)})
        response = self.report.request_json(request + "1")
        self.assertFragmentIn(response, {'results': EmptyList()})

    def test_cpa_shop_incut_adult_incut_on_adult_hids(self):
        request = (
            "place=cpa_shop_incut&pp=18&text=пенетрылда&hid=18540670&numdoc=5&adult=1&"
            "rearr-factors=market_premium_ads_gallery_min_num_doc_to_request_from_base=1"
            ";market_force_filter_adult_for_incuts=0;market_adult_incuts_on_adult_hids_only="
        )
        response = self.report.request_json(request + "0")
        self.assertFragmentIn(response, {'results': ElementCount(5)})
        response = self.report.request_json(request + '1')
        self.assertFragmentIn(response, {'results': EmptyList()})
        request_adult_hid = (
            "place=cpa_shop_incut&pp=18&text=пенетрылда&hid=16155466&numdoc=5&adult=1&"
            "rearr-factors=market_premium_ads_gallery_min_num_doc_to_request_from_base=1"
            ";market_force_filter_adult_for_incuts=0;market_adult_incuts_on_adult_hids_only="
        )
        response = self.report.request_json(request_adult_hid + '0')
        self.assertFragmentIn(response, {'results': ElementCount(5)})
        response = self.report.request_json(request_adult_hid + '1')
        self.assertFragmentIn(response, {'results': ElementCount(5)})

    @classmethod
    def prepare_cpa_shop_incut_relevance_threshold(cls):
        cls.index.offers += [
            Offer(
                fesh=1866 + i,
                hyperid=1866 + i,
                hid=1866,
                fee=1890 + i,
                price=1100,
                ts=1866 + i,
                cpa=Offer.CPA_REAL,
                title="Транклюкатор {} квт".format(i * 2 + 15),
            )
            for i in range(1, 10)
        ]

        for i in range(1, 10):
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1866 + i).respond(0.105 + i * 0.01)

    # проверяем работу флага market_premium_ads_gallery_shop_incut_relevance_threshold
    def test_cpa_shop_incut_relevance_threshold(self):
        # с порогом 0 ничего не отфильтровывается
        request = (
            "place=cpa_shop_incut&pp=18&text=Транклюкатор&numdoc=8&min-num-doc=1&debug=1&"
            "rearr-factors=market_premium_ads_gallery_min_num_doc_to_request_from_base=1;market_premium_ads_gallery_shop_incut_relevance_threshold="
        )
        response = self.report.request_json(request + "0.0")
        self.assertFragmentIn(
            response,
            {
                'results': ElementCount(8),
            },
        )
        # если порог немного поднять, отсеивается часть документов
        response = self.report.request_json(request + "0.15")
        self.assertFragmentIn(
            response,
            {
                'results': ElementCount(5),
                'debug': {
                    'brief': {
                        'filters': {
                            'CPA_SHOP_INCUT_BY_RELEVANCE': 4,
                        }
                    }
                },
            },
        )

    # тест работы автоброкера с флагом market_cpa_shop_incut_rp_fee_coef_w
    @classmethod
    def prepare_cpa_shop_incut_autobroker_with_rp_fee(cls):
        cls.index.offers += [
            Offer(
                hid=1953,
                hyperid=19531,
                price=10000,
                fee=100,
                cpa=Offer.CPA_REAL,
                autostrategy_vendor_with_datasource_id=AutostrategyWithDatasourceId(
                    id=444,
                    datasource_id=1,
                    strategy=Autostrategy(type=AutostrategyType.POSITIONAL, position=1, maxbid=100),
                ),
            ),
            Offer(
                hid=1953,
                hyperid=19532,
                price=10000,
                fee=40,
                cpa=Offer.CPA_REAL,
                autostrategy_vendor_with_datasource_id=AutostrategyWithDatasourceId(
                    id=444,
                    datasource_id=1,
                    strategy=Autostrategy(type=AutostrategyType.POSITIONAL, position=1, maxbid=90),
                ),
            ),
        ]
        cls.index.reserveprice_fee += [
            ReservePriceFee(hyper_id=1953, reserveprice_fee=0.005),
        ]

    def test_cpa_shop_incut_autobroker_with_rp_fee(self):
        # второй оффер не подперт ничем, поэтому
        # с выключенным флагом total_fee падает до 1 очка, и его заплатит вендор
        request = (
            "place=cpa_shop_incut&pp=18&hid=1953&numdoc=2&min-num-doc=1&debug=1"
            "&rearr-factors=market_premium_ads_gallery_shop_incut_vendor_fee_correction=1"
            ";market_premium_ads_gallery_shop_incut_vendor_bid_to_fee_coef=0.075"
            ";market_cpa_shop_incut_reserve_price_threshold=1"
            ";market_cpa_shop_incut_rp_fee_coef_w="
        )
        response = self.report.request_json(request + "0")
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'debug': {
                            'modelId': 19531,
                            'sale': {
                                'vBid': 100,
                                'vendorClickPrice': 80,
                                'shopFee': 100,
                                'brokeredFee': 80,
                            },
                        }
                    },
                    {
                        'debug': {
                            'modelId': 19532,
                            'sale': {
                                'vBid': 90,
                                'vendorClickPrice': 1,
                                'shopFee': 40,
                                'brokeredFee': 0,
                            },
                        }
                    },
                ]
            },
        )

        # с включенным флагом total_fee подпирается до rp_fee
        response = self.report.request_json(request + "1")
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'debug': {
                            'modelId': 19531,
                            'sale': {
                                'vBid': 100,
                                'vendorClickPrice': 80,
                                'shopFee': 100,
                                'brokeredFee': 80,
                            },
                        }
                    },
                    {
                        'debug': {
                            'modelId': 19532,
                            'sale': {
                                'vBid': 90,
                                'vendorClickPrice': 12,
                                'shopFee': 40,
                                'brokeredFee': 5,
                            },
                        }
                    },
                ]
            },
        )

    # понизим влияние вендорской ставки так, чтобы второй оффер опустился ниже порога,
    # первый при этом подопрется rp_fee
    def test_cpa_shop_incut_rp_threshold_with_low_vendor_bids_coeff(self):
        request = (
            "place=cpa_shop_incut&pp=18&hid=1953&numdoc=2&min-num-doc=1&debug=1"
            "&rearr-factors=market_premium_ads_gallery_shop_incut_vendor_fee_correction=0.5"
            ";market_premium_ads_gallery_shop_incut_vendor_bid_to_fee_coef=1.5"
            ";market_cpa_shop_incut_reserve_price_threshold=1"
            ";market_cpa_shop_incut_rp_fee_coef_w=2"
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'debug': {
                            'modelId': 19531,
                            'sale': {
                                'vBid': 100,
                                'vendorClickPrice': 46,
                                'shopFee': 100,
                                'brokeredFee': 90,
                            },
                        }
                    }
                ],
                'debug': {'brief': {'filters': {'OFFER_FILTERED_OUT_BY_RESERVE_PRICE': 1}}},
            },
        )

    @classmethod
    def prepare_cpa_shop_incut_autobroker_with_delivery_type(cls):
        cls.index.regiontree += [
            Region(rid=1149, region_type=Region.COUNTRY, name='Как Беларусь, только не фильтруется CPA-only'),
        ]

        cls.index.shops += [
            Shop(fesh=741, priority_region=213, regions=[213], cpa=Shop.CPA_REAL, name='s741'),
            Shop(fesh=742, priority_region=213, regions=[213], cpa=Shop.CPA_REAL, name='s742'),
            Shop(fesh=743, priority_region=2, regions=[225], cpa=Shop.CPA_REAL, name='s743'),
            Shop(fesh=744, priority_region=2, regions=[225], cpa=Shop.CPA_REAL, name='s744'),
            Shop(fesh=745, priority_region=1149, cpa=Shop.CPA_REAL, name='s745', home_region=1149, regions=[213]),
            Shop(fesh=746, priority_region=1149, cpa=Shop.CPA_REAL, name='s746', home_region=1149, regions=[213]),
        ]

        cls.index.offers += [
            # 1 группа
            Offer(hid=753, hyperid=7531, ts=7531, price=10000, fee=100, cpa=Offer.CPA_REAL, fesh=741),
            Offer(hid=753, hyperid=7531, ts=7531, price=10000, fee=200, cpa=Offer.CPA_REAL, fesh=743),
            Offer(hid=753, hyperid=7531, ts=7531, price=10000, fee=200, cpa=Offer.CPA_REAL, fesh=745),
            Offer(hid=753, hyperid=7531, ts=7531, price=10000, fee=80, cpa=Offer.CPA_REAL, fesh=742),
            # 2 группа
            Offer(hid=753, hyperid=7532, ts=7531, price=10000, fee=200, cpa=Offer.CPA_REAL, fesh=743),
            Offer(hid=753, hyperid=7532, ts=7531, price=10000, fee=200, cpa=Offer.CPA_REAL, fesh=745),
            Offer(hid=753, hyperid=7532, ts=7531, price=10000, fee=90, cpa=Offer.CPA_REAL, fesh=742),
            # 3 группа
            Offer(hid=753, hyperid=7533, ts=7531, price=10000, fee=100, cpa=Offer.CPA_REAL, fesh=743),
            Offer(hid=753, hyperid=7533, ts=7531, price=10000, fee=200, cpa=Offer.CPA_REAL, fesh=745),
            Offer(hid=753, hyperid=7533, ts=7531, price=10000, fee=80, cpa=Offer.CPA_REAL, fesh=744),
            # 4 группа
            Offer(hid=753, hyperid=7534, ts=7531, price=10000, fee=200, cpa=Offer.CPA_REAL, fesh=745),
            Offer(hid=753, hyperid=7534, ts=7531, price=10000, fee=90, cpa=Offer.CPA_REAL, fesh=744),
            # 5 группа
            Offer(hid=753, hyperid=7535, ts=7531, price=10000, fee=100, cpa=Offer.CPA_REAL, fesh=745),
            Offer(hid=753, hyperid=7535, ts=7531, price=10000, fee=80, cpa=Offer.CPA_REAL, fesh=746),
            # 6 группа
            Offer(hid=753, hyperid=7536, ts=7531, price=10000, fee=90, cpa=Offer.CPA_REAL, fesh=746),
        ]

        cls.index.reserveprice_fee += [
            ReservePriceFee(hyper_id=753, reserveprice_fee=0.005),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 7531).respond(0.01)

    def test_cpa_shop_incut_autobroker_with_delivery_type(self):
        request = (
            "place=cpa_shop_incut&pp=18&hid=753&numdoc=8&min-num-doc=1&rids=213&debug=1"
            "&rearr-factors=market_cpa_shop_incut_reserve_price_threshold=1"
            ";market_cpa_shop_incut_rp_fee_coef_w=1"
            # Для этого теста включаем ранжирование по типу доставки
            # Это deprecated-логика, её вырубаем
            ";market_cpa_shop_incut_do_not_use_delivery_type_in_ranking=0"
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'debug': {
                            'modelId': 7531,
                            'fesh': 741,
                            'sale': {
                                'shopFee': 100,
                                'brokeredFee': 90,
                            },
                        }
                    },
                    {
                        'debug': {
                            'modelId': 7532,
                            'fesh': 742,
                            'sale': {
                                'shopFee': 90,
                                'brokeredFee': 50,
                            },
                        }
                    },
                    {
                        'debug': {
                            'modelId': 7533,
                            'fesh': 743,
                            'sale': {
                                'shopFee': 100,
                                'brokeredFee': 90,
                            },
                        }
                    },
                    {
                        'debug': {
                            'modelId': 7534,
                            'fesh': 744,
                            'sale': {
                                'shopFee': 90,
                                'brokeredFee': 50,
                            },
                        }
                    },
                    {
                        'debug': {
                            'modelId': 7535,
                            'fesh': 745,
                            'sale': {
                                'shopFee': 100,
                                'brokeredFee': 90,
                            },
                        }
                    },
                    {
                        'debug': {
                            'modelId': 7536,
                            'fesh': 746,
                            'sale': {
                                'shopFee': 90,
                                'brokeredFee': 50,
                            },
                        },
                    },
                ],
                'debug': {
                    'report': {
                        'logicTrace': [
                            Contains('Take first offer in group', 'shopId: 741', 'deliveryType: 3'),
                            Contains('Calc multiplier', 'by next offer in group', 'shopId: 742'),
                            Contains('Calc multiplier', 'by first offer of next group', 'shopId: 742'),
                            Contains('Take first offer in group', 'shopId: 742', 'deliveryType: 3'),
                            Contains('Take first offer in group', 'shopId: 743', 'deliveryType: 2'),
                            Contains('Calc multiplier', 'by next offer in group', 'shopId: 744'),
                            Contains('Calc multiplier', 'by first offer of next group', 'shopId: 744'),
                            Contains('Take first offer in group', 'shopId: 744', 'deliveryType: 2'),
                            Contains('Take first offer in group', 'shopId: 745', 'deliveryType: 1'),
                            Contains('Calc multiplier', 'by next offer in group', 'shopId: 746'),
                            Contains('Calc multiplier', 'by first offer of next group', 'shopId: 746'),
                            Contains('Take first offer in group', 'shopId: 746', 'deliveryType: 1'),
                        ]
                    }
                },
            },
        )

    def test_cpa_shop_incut_delivery_type_independence(self):
        """
        Проверяем, что ранжирование не зависит от delivery type
        """
        request = (
            "place=cpa_shop_incut&pp=18&hid=753&numdoc=8&min-num-doc=1&rids=213&debug=1"
            "&rearr-factors=market_cpa_shop_incut_reserve_price_threshold=1"
            ";market_cpa_shop_incut_rp_fee_coef_w=1"
        )
        response = self.report.request_json(request)

        # Готовимся парсить выдачу
        offers_count = 6  # Кол-во офферов в выдаче
        stable_relevances = [Capture() for _ in range(offers_count)]

        # Проверяем размер выдачи
        self.assertFragmentIn(response, {"results": ElementCount(offers_count)})

        # Парсим выдачу
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "debug": {
                            "properties": {
                                "CPA_SHOP_INCUT": NotEmpty(capture=capture),
                            },
                        },
                    }
                    for capture in stable_relevances
                ],
            },
        )

        # Проверяем, что офферы в выдаче расположены по невозрастанию stable relevance
        # Это обеспечивает корректность автоброкера
        for curr in range(len(stable_relevances) - 1):
            curr_rel = int(stable_relevances[curr].value)
            next_rel = int(stable_relevances[curr + 1].value)
            self.assertTrue(curr_rel >= next_rel)

    # тест работы порога отсечения офферов по высокой цене
    @classmethod
    def prepare_cpa_shop_incut_high_price_threshold(cls):
        cls.index.offers += [
            Offer(
                hid=1953,
                hyperid=29531,
                fesh=1,
                price=10000,
                fee=100,
                cpa=Offer.CPA_REAL,
                title="оффер 1",
                ts=8881,
            ),
            Offer(
                hid=1954,
                hyperid=29533,
                fesh=3,
                price=6424,
                fee=100,
                cpa=Offer.CPA_REAL,
                title="оффер 2",
                ts=8882,
            ),
            Offer(
                hid=1954,
                hyperid=29534,
                fesh=4,
                price=6423,
                fee=40,
                cpa=Offer.CPA_REAL,
                title="оффер 3",
                ts=8883,
            ),
        ]
        cls.index.price_threshold += [
            PriceThreshold(hid=1954, price=6423.159999999949),
        ]
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 8881).respond(0.01)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 8882).respond(0.02)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 8883).respond(0.03)

    def test_cpa_shop_incut_high_price_threshold_enabled(self):
        # проверим что отрезаются оффер у которого hid=1953 и цена price=6424
        request = (
            "place=cpa_shop_incut&pp=18&text=оффер&numdoc=4&min-num-doc=2&debug=1"
            "&rearr-factors=market_cpa_shop_incut_high_price_threshold=1;market_cpa_shop_incut_high_price_clip_to_threshold=0"
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'titles': {'raw': 'оффер 1'},
                    },
                    {
                        'titles': {'raw': 'оффер 3'},
                    },
                ]
            },
        )
        self.assertFragmentNotIn(
            response,
            {
                'results': [
                    {
                        'titles': {'raw': 'оффер 2'},
                    },
                ]
            },
        )

    def test_cpa_shop_incut_high_price_threshold_disabled(self):
        # проверим что оффер остается если порог выключен
        request = (
            "place=cpa_shop_incut&pp=18&text=оффер&numdoc=4&min-num-doc=2&debug=1"
            "&rearr-factors=market_cpa_shop_incut_high_price_threshold_base=0;market_cpa_shop_incut_high_price_clip_to_threshold=0;"
            "market_cpa_shop_incut_high_price_threshold=0;market_premium_ads_gallery_shop_incut_rel_hid_filter_enable=0"
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'titles': {'raw': 'оффер 1'},
                    },
                    {
                        'titles': {'raw': 'оффер 2'},
                    },
                    {
                        'titles': {'raw': 'оффер 3'},
                    },
                ]
            },
        )

    def test_cpa_shop_incut_high_price_threshold_base_enabled(self):
        # проверим что отрезаются оффер у которого hid=1953 и цена price=6424
        request = (
            "place=cpa_shop_incut&pp=18&text=оффер&numdoc=4&min-num-doc=2&debug=1"
            "&rearr-factors=market_cpa_shop_incut_high_price_threshold_base=1;market_cpa_shop_incut_high_price_clip_to_threshold=0"
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'titles': {'raw': 'оффер 1'},
                    },
                    {
                        'titles': {'raw': 'оффер 3'},
                    },
                ]
            },
        )
        self.assertFragmentNotIn(
            response,
            {
                'results': [
                    {
                        'titles': {'raw': 'оффер 2'},
                    },
                ]
            },
        )

    def test_cpa_shop_incut_high_price_threshold_base_disabled(self):
        # проверим что оффер остается если порог выключен
        request = (
            "place=cpa_shop_incut&pp=18&text=оффер&numdoc=4&min-num-doc=2&debug=1"
            "&rearr-factors=market_cpa_shop_incut_high_price_threshold_base=0;market_cpa_shop_incut_high_price_clip_to_threshold=0;"
            "market_cpa_shop_incut_high_price_threshold=0;market_premium_ads_gallery_shop_incut_rel_hid_filter_enable=0"
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'titles': {'raw': 'оффер 1'},
                    },
                    {
                        'titles': {'raw': 'оффер 2'},
                    },
                    {
                        'titles': {'raw': 'оффер 3'},
                    },
                ]
            },
        )

    def test_cpa_shop_incut_high_price_threshold_with_high_price_clip(self):
        # проверим что оффер остается если порог включен вместе с market_cpa_shop_incut_high_price_clip_to_threshold
        request = (
            "place=cpa_shop_incut&pp=18&text=оффер&numdoc=4&min-num-doc=2&debug=1"
            "&rearr-factors=market_cpa_shop_incut_high_price_threshold_base=1;market_cpa_shop_incut_high_price_threshold=1;"
            "market_cpa_shop_incut_high_price_clip_to_threshold=1"
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'titles': {'raw': 'оффер 1'},
                    },
                    {
                        'titles': {'raw': 'оффер 2'},
                    },
                    {
                        'titles': {'raw': 'оффер 3'},
                    },
                ]
            },
        )

    def test_cpa_shop_incut_hid_rel_limit(self):
        # market_premium_ads_gallery_shop_incut_rel_price_info_docs_count=1 - проверяем, что останутся оффера только из категории самого релевантноого оффера
        request = (
            "place=cpa_shop_incut&pp=18&text=оффер&numdoc=4&min-num-doc=2&debug=1"
            "&rearr-factors=market_premium_ads_gallery_shop_incut_rel_price_info_docs_count=1;market_premium_ads_gallery_shop_incut_rel_hid_filter_enable=1"
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'titles': {'raw': 'оффер 2'},
                    },
                    {
                        'titles': {'raw': 'оффер 3'},
                    },
                ]
            },
        )
        self.assertFragmentNotIn(
            response,
            {
                'results': [
                    {
                        'titles': {'raw': 'оффер 1'},
                    },
                ]
            },
        )

    # тест включения аукциона в baybox-е
    @classmethod
    def prepare_cpa_shop_incut_buybox_auction(cls):
        cls.index.shops += [
            Shop(
                fesh=801,
                priority_region=213,
                cpa=Shop.CPA_REAL,
                cis=Shop.CIS_REAL,
                cpc=Shop.CPC_NO,
                name='DSBS магазин 1',
            ),
            Shop(
                fesh=802,
                priority_region=213,
                fulfillment_program=False,
                supplier_type=Shop.THIRD_PARTY,
                blue=Shop.BLUE_REAL,
                name='3P поставщик 1',
            ),
            Shop(
                fesh=803,
                priority_region=213,
                fulfillment_program=False,
                supplier_type=Shop.THIRD_PARTY,
                blue=Shop.BLUE_REAL,
                name='3P поставщик 2',
            ),
        ]
        cls.index.models += [
            Model(hid=3002, hyperid=4002, ts=501, title='model_1', vbid=11),
        ]
        cls.index.mskus += [
            MarketSku(
                title="msku_1",
                hid=3002,
                hyperid=4002,
                sku=100001,
                delivery_buckets=[1234],
                blue_offers=[
                    BlueOffer(
                        price=1640,
                        feedid=802,
                        fee=200,
                        waremd5="BLUE-100001-FEED-0001Q",
                        title="3P buybox offer 1",
                        ts=7002,
                    ),
                    BlueOffer(
                        price=1680,
                        feedid=803,
                        fee=100,
                        waremd5="BLUE-100001-FEED-0002Q",
                        title="3P buybox offer 2",
                        ts=7003,
                    ),
                ],
            ),
        ]
        cls.index.offers += [
            Offer(
                hid=3002,
                hyperid=4002,
                fesh=801,
                price=1800,
                fee=150,
                sku=100001,
                cpa=Offer.CPA_REAL,
                title="DSBS buybox offer 1",
                waremd5='sgf1xWYFqdGiLh4TT-111Q',
                ts=7000,
            ),
        ]
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 7000).respond(0.01)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 7001).respond(0.02)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 7002).respond(0.03)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 7003).respond(0.04)

    def test_cpa_shop_incut_buybox_auction(self):
        # проверяем включение аукциона в buybox-е. флаг market_buybox_auction_cpa_shop_incut
        request = (
            "place=cpa_shop_incut&pp=18&text=buybox&numdoc=10&min-num-doc=1&debug=1"
            "&rearr-factors=market_buybox_auction_cpa_shop_incut=1;market_blue_buybox_shop_incut_price_rel_max_threshold=1.05;"
            "market_buybox_auction_coef_b_multiplicative_bid_coef_cs_incut=0.001;market_buybox_enable_advert_buybox=0;"
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'titles': {'raw': '3P buybox offer 1'},
                        'wareId': "BLUE-100001-FEED-0001Q",
                        'debug': {
                            'buyboxDebug': {
                                'Offers': [
                                    {
                                        'WareMd5': 'BLUE-100001-FEED-0001Q',
                                        'AuctionedShopFee': 33,
                                    },
                                    {
                                        'WareMd5': 'BLUE-100001-FEED-0002Q',
                                    },
                                ],
                                'RejectedOffers': [{'Offer': {'WareMd5': 'sgf1xWYFqdGiLh4TT-111Q'}}],
                            }
                        },
                    }
                ]
            },
        )

    def test_cpa_shop_incut_buybox_auction_adv(self):
        # проверяем включение аукциона в buybox-е. флаг market_buybox_auction_cpa_shop_incut
        request = (
            "place=cpa_shop_incut&pp=18&text=buybox&numdoc=10&min-num-doc=1&debug=1"
            "&rearr-factors=market_buybox_auction_cpa_shop_incut=1;market_blue_buybox_shop_incut_price_rel_max_threshold=1.05;"
            "market_buybox_auction_coef_b_multiplicative_bid_coef_cs_incut=0.001;"
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'titles': {'raw': '3P buybox offer 1'},
                        'wareId': "BLUE-100001-FEED-0001Q",
                        'debug': {
                            'buyboxDebug': {
                                'Offers': [
                                    {
                                        'WareMd5': 'sgf1xWYFqdGiLh4TT-111Q',
                                    },
                                    {
                                        'WareMd5': 'BLUE-100001-FEED-0001Q',
                                        'AuctionedShopFee': 96,
                                    },
                                    {
                                        'WareMd5': 'BLUE-100001-FEED-0002Q',
                                    },
                                ],
                            }
                        },
                    }
                ]
            },
        )

    def test_cpa_shop_incut_buybox_max_threshold(self):
        # проверяем работу max price порога в baybox-е. Флаг market_blue_buybox_shop_incut_price_rel_max_threshold
        # выставляем порог 2% и BLUE-100001-FEED-0002Q оффер не должен проходить
        request = (
            "place=cpa_shop_incut&pp=18&text=buybox&numdoc=10&min-num-doc=1&debug=1"
            "&rearr-factors=market_buybox_auction_cpa_shop_incut=1;market_blue_buybox_shop_incut_price_rel_max_threshold=1.02;market_buybox_enable_advert_buybox=0;"
            "market_buybox_adv_buybox_price_rel_max_threshold=1.02; market_blue_buybox_max_price_rel_add_diff_adv=0;market_buybox_enable_advert_buybox=0;"
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'titles': {'raw': '3P buybox offer 1'},
                        'wareId': "BLUE-100001-FEED-0001Q",
                        'debug': {
                            'buyboxDebug': {
                                'Offers': [
                                    {
                                        'WareMd5': 'BLUE-100001-FEED-0001Q',
                                    },
                                ],
                                'RejectedOffers': [
                                    {'Offer': {'WareMd5': 'sgf1xWYFqdGiLh4TT-111Q'}},
                                    {'Offer': {'WareMd5': 'BLUE-100001-FEED-0002Q'}},
                                ],
                            }
                        },
                    }
                ]
            },
        )

    def test_cpa_shop_incut_buybox_max_threshold_adv(self):
        # проверяем работу max price порога в baybox-е. Флаг market_buybox_adv_buybox_price_rel_max_threshold
        # выставляем порог 2% и market_blue_buybox_max_price_rel_add_diff_adv выставляем в 0
        # поэтому BLUE-100001-FEED-0002Q оффер не должен проходить
        request = (
            "place=cpa_shop_incut&pp=18&text=buybox&numdoc=10&min-num-doc=1&debug=1"
            "&rearr-factors=market_buybox_auction_cpa_shop_incut=1;market_buybox_enable_advert_buybox=1;"
            "market_blue_buybox_max_price_rel_add_diff_adv=0;market_buybox_adv_buybox_price_rel_max_threshold=1.02;"
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'titles': {'raw': '3P buybox offer 1'},
                        'wareId': "BLUE-100001-FEED-0001Q",
                        'debug': {
                            'buyboxDebug': {
                                'Offers': [
                                    {
                                        'WareMd5': 'BLUE-100001-FEED-0001Q',
                                    },
                                ],
                                'RejectedOffers': [
                                    {'Offer': {'WareMd5': 'sgf1xWYFqdGiLh4TT-111Q'}},
                                    {'Offer': {'WareMd5': 'BLUE-100001-FEED-0002Q'}},
                                ],
                            }
                        },
                    }
                ]
            },
        )

    def test_cpa_shop_incut_buybox_auction_coefficients(self):
        """
        Проверяем работу флагов-коэффициентов для аукциона в байбоксе в cpa_shop_incut
        """

        def get_auction_value(gmv, shop_fee, vendor_fee, rp_fee, ue_add, price, A=0, B=0, C=0, D=0, E=0, F=0, G=0, W=0):
            # В полной формуле есть ещё функция f, но она сейчас в проде просто возвращает аргумент
            return (
                gmv
                * (
                    A
                    + B * (max(0, shop_fee + vendor_fee - W * rp_fee) + G * ue_add + E + float(F) / price)
                    + float(C) / price
                )
                + D
            )

        def parse_auction_in_response(response, buybox_auction_coeffs):
            # Парсим ответ репорта и возвращаем пересчитанное значение аукциона и значение аукциона, полученное репортом
            shop_fee, offer_vendor_fee, gmv_randomized, price, auction_value_parsed = (
                Capture(),
                Capture(),
                Capture(),
                Capture(),
                Capture(),
            )
            ware_md5_winner = 'BLUE-100001-FEED-0001Q'
            self.assertFragmentIn(
                response,
                {
                    "wareId": ware_md5_winner,
                    "buyboxDebug": {
                        "Offers": [
                            {
                                "IsWinnerByRandom": False,
                                "ShopFee": NotEmpty(capture=shop_fee),
                                "OfferVendorFee": NotEmpty(capture=offer_vendor_fee),
                                "AuctionedShopFee": NotEmpty(),
                                "GmvRandomized": NotEmpty(capture=gmv_randomized),
                                "WareMd5": ware_md5_winner,
                                # Тут нет rp_fee
                                # Юнит-экономика фактически не попадает в формулу аукциона, поэтому мы её тут не берём
                                "PriceAfterCashback": NotEmpty(capture=price),
                                "AuctionValueRandomized": NotEmpty(capture=auction_value_parsed),
                            },
                        ],
                        "WonMethod": "WON_BY_AUCTION",
                    },
                },
            )
            auction_value_recomputed = get_auction_value(
                gmv=gmv_randomized.value,
                shop_fee=shop_fee.value,
                vendor_fee=offer_vendor_fee.value,
                rp_fee=0,
                ue_add=0,
                price=price.value,
                **buybox_auction_coeffs
            )
            return auction_value_recomputed, auction_value_parsed.value

        request_base_cpa_shop_incut = "place=cpa_shop_incut&pp=18&text=buybox&numdoc=10&min-num-doc=1&debug=da"
        rearr_flags_base = "&rearr-factors=market_buybox_auction_cpa_shop_incut=1;"

        def check_recomputed_equals_parsed(
            rearr_flags_dict, buybox_auction_coeffs, request_base=request_base_cpa_shop_incut
        ):
            rearr_flags_str = rearr_flags_base + ';'.join(
                [str(key) + "=" + str(rearr_flags_dict[key]) for key in rearr_flags_dict]
            )
            response = self.report.request_json(request_base + rearr_flags_base + rearr_flags_str)
            auction_value_recomputed, auction_value_parsed = parse_auction_in_response(response, buybox_auction_coeffs)
            self.assertAlmostEqual(auction_value_recomputed, auction_value_parsed, delta=100)

        # Проверяем, что на аукцион влияют специальные флаги для премиальной
        check_recomputed_equals_parsed(
            {
                'market_buybox_auction_coef_a_additive_bid_coef_cs_incut': 0.1,
                'market_buybox_auction_coef_b_multiplicative_bid_coef_cs_incut': 0.01,
                'market_buybox_auction_coef_e_additive_coef_inside_bid_cs_incut': 0.2,
                'market_buybox_auction_coef_f_div_price_coef_in_bid_cs_incut': 20,
                'market_buybox_auction_coef_w_rp_fee_coef_cs_incut': 1,
            },
            {
                'A': 0.1,
                'B': 0.01,
                'E': 0.2,
                'F': 20,
                'W': 1,
            },
        )

    def test_buybox_auction_minimal_amnesty(self):
        """
        Во врезке есть 2 аукциона - "врезочный" и аукцион в байбоксе. Проверяем, что с помощью флага market_use_minimal_amnesty_in_cpa_shop_incut
        включается выбор наименьшей амнистии из этих аукционов (наименьшая амнистия соответствует наибольшей списанной ставке)
        """

        def get_expected_response(with_minimal_amnesty):
            return {
                "results": [
                    {
                        "wareId": "BLUE-100001-FEED-0001Q",
                        "debug": {
                            "wareId": "BLUE-100001-FEED-0001Q",
                            "sale": {
                                "shopFee": 200,
                                # Списанная ставка тут должна равняться амнистированной ставке в байбоксе, если выбираем минимальную амнистию
                                "brokeredFee": 33 if with_minimal_amnesty else 1,
                            },
                            "buyboxDebug": {
                                "Offers": [
                                    {
                                        "WareMd5": "BLUE-100001-FEED-0001Q",
                                        "AuctionedShopFee": 33,  # Эта ставка должна попасть в brokeredFee, если выбираем минимальную амнистию
                                    },
                                ],
                                "WonMethod": "WON_BY_AUCTION",
                            },
                        },
                    },
                ],
            }

        request_base_cpa_shop_incut = (
            "place=cpa_shop_incut&pp=18&text=buybox&numdoc=10&min-num-doc=1&debug=da&rearr-factors="
        )
        rearr_flags_dict = {
            'market_use_minimal_amnesty_in_cpa_shop_incut': 1,  # Включаем флаг взятия минимальной амнистии при аукционе во врезке
            'market_buybox_auction_cpa_shop_incut': 1,
            'market_blue_buybox_shop_incut_price_rel_max_threshold': 1.05,
            'market_buybox_auction_coef_b_multiplicative_bid_coef_cs_incut': 0.001,
            'market_use_additional_brokered_prices_info_in_cpa_shop_incut': 1,  # Под этим флагом хотим различать по логам, какая амнистия была выбрана
            'market_buybox_enable_advert_buybox': 0,
        }
        response = self.report.request_json(request_base_cpa_shop_incut + dict_to_rearr(rearr_flags_dict))
        self.assertFragmentIn(response, get_expected_response(with_minimal_amnesty=True))
        self.show_log.expect(
            url_type=6, shop_fee_ab=33, shop_fee_ab_bb=33, shop_fee_ab_search=1, ware_md5="BLUE-100001-FEED-0001Q"
        )
        # Выключаем флаг взятия минимальной амнистии - списываем амнистированную ставку аукциона врезки, амнистированную ставку аукциона в байбоксе игнорируем
        rearr_flags_dict['market_use_minimal_amnesty_in_cpa_shop_incut'] = 0
        response = self.report.request_json(request_base_cpa_shop_incut + dict_to_rearr(rearr_flags_dict))
        self.assertFragmentIn(response, get_expected_response(with_minimal_amnesty=False))
        self.show_log.expect(
            url_type=6,
            shop_fee_ab=1,
            shop_fee_ab_bb=Absent(),
            shop_fee_ab_search=Absent(),
            ware_md5="BLUE-100001-FEED-0001Q",
        )
        # Проверяем флаг market_use_additional_brokered_prices_info_in_cpa_shop_incut - в логи не должны попасть shop_fee_ab_bb/shop_fee_ab_search
        rearr_flags_dict['market_use_minimal_amnesty_in_cpa_shop_incut'] = 1
        rearr_flags_dict['market_use_additional_brokered_prices_info_in_cpa_shop_incut'] = 0
        response = self.report.request_json(request_base_cpa_shop_incut + dict_to_rearr(rearr_flags_dict))
        self.assertFragmentIn(response, get_expected_response(with_minimal_amnesty=True))
        self.show_log.expect(
            url_type=6,
            shop_fee_ab=33,
            shop_fee_ab_bb=Absent(),
            shop_fee_ab_search=Absent(),
            ware_md5="BLUE-100001-FEED-0001Q",
        )

    @classmethod
    def prepare_buybox_auction_1p_pessimization(cls):
        cls.index.shops += [
            Shop(
                fesh=2,
                datafeed_id=2,
                priority_region=213,
                regions=[213],
                name="Один 1P поставщик",
                supplier_type=Shop.FIRST_PARTY,
                is_supplier=True,
                blue=Shop.BLUE_REAL,
                warehouse_id=145,
                fulfillment_program=True,
            ),
        ]
        cls.index.models += [
            Model(hid=3009, hyperid=3009, ts=509, title='model_9', vbid=11),
        ]
        cls.index.mskus += [
            MarketSku(
                title="model_3009_msku_1",
                hid=3009,
                hyperid=3009,
                sku=100009,
                delivery_buckets=[1234],
                blue_offers=[
                    BlueOffer(
                        price=1640,
                        feedid=2,
                        fee=220,  # Это fee игнорируется, потому что 1p. Будет взята рекомендованная ставка
                        waremd5="BLUE-100009-FEED-0001Q",
                        title="model_3009 1P buybox offer 1",
                        ts=7015,
                    ),
                    BlueOffer(
                        price=1640,
                        feedid=803,
                        fee=200,
                        waremd5="BLUE-100009-FEED-0002Q",
                        title="model_3009 3P buybox offer 2",
                        ts=7016,
                    ),
                ],
            ),
        ]
        cls.index.recommended_fee += [
            RecommendedFee(hyper_id=3009, recommended_bid=0.0220),
        ]
        cls.index.reserveprice_fee += [
            ReservePriceFee(hyper_id=3009, reserveprice_fee=0.01),
        ]

    def test_buybox_auction_1p_pessimization(self):
        """
        Проверяем, что в аукционе в байбоксе обычно побеждает 1p оффер, так как у него ставка выше, но за счёт коэффициента пессимизации ставки 1p
        market_buybox_auction_coef_1p_pessimization_kkm может победить 3p оффер
        """

        def get_auction_value(
            gmv,
            shop_fee,
            vendor_fee,
            price,
            shop_fee_pessimization_coef,
            rp_fee=0,
            ue_add=0,
            A=1,
            B=0.001,
            C=0,
            D=0,
            E=0,
            F=0,
            G=0,
            W=1,
        ):
            return (
                gmv
                * (
                    A
                    + B
                    * (
                        max(0, shop_fee * shop_fee_pessimization_coef + vendor_fee - W * rp_fee)
                        + G * ue_add
                        + E
                        + float(F) / price
                    )
                    + float(C) / price
                )
                + D
            )

        def get_expected_response(pessimize_1p):
            return {
                "slug": "model-3009-3p-buybox-offer-2" if pessimize_1p else "model-3009-1p-buybox-offer-1",
                "wareId": "BLUE-100009-FEED-0002Q" if pessimize_1p else "BLUE-100009-FEED-0001Q",
                "debug": {
                    "sale": {
                        "shopFee": 200 if pessimize_1p else 220,
                        "brokeredFee": 163 if pessimize_1p else 191,
                    },
                    "buyboxDebug": {
                        "Offers": [
                            {
                                "WareMd5": "BLUE-100009-FEED-0002Q" if pessimize_1p else "BLUE-100009-FEED-0001Q",
                                "AuctionedShopFee": 163 if pessimize_1p else 191,
                            },
                        ],
                        "WonMethod": "WON_BY_AUCTION",
                    },
                },
            }

        def parse_1p_auction(response):
            captures = {
                key: Capture()
                for key in ['auction_value_parsed', 'shop_fee', 'offer_vendor_fee', 'gmv_randomized', 'price']
            }
            self.assertFragmentIn(
                response,
                {
                    "WareMd5": "BLUE-100009-FEED-0001Q",  # 1p offer
                    "AuctionValueRandomized": NotEmpty(capture=captures['auction_value_parsed']),
                    "ShopFee": NotEmpty(capture=captures['shop_fee']),
                    "OfferVendorFee": NotEmpty(capture=captures['offer_vendor_fee']),
                    "GmvRandomized": NotEmpty(capture=captures['gmv_randomized']),
                    "PriceAfterCashback": NotEmpty(capture=captures['price']),
                },
            )
            return captures

        def check_for_rearrs(rearr_flags_dict, pessimization_coef):
            request_base_cpa_shop_incut = (
                "place=cpa_shop_incut&pp=18&text=3009&numdoc=10&min-num-doc=1&debug=da&rearr-factors="
            )
            rearr_flags_str = '&rearr-factors=' + dict_to_rearr(rearr_flags_dict) + ';'
            response = self.report.request_json(request_base_cpa_shop_incut + rearr_flags_str)
            # Проверяем, что победил нужный оффер
            self.assertFragmentIn(response, get_expected_response(pessimize_1p=(pessimization_coef < 1)))
            # Парсим данные об 1p, переразыгрываем аукцион, чтобы убедиться, что коэффициент правильно учитывается
            captures = parse_1p_auction(response)
            auction_value_recomputed = get_auction_value(
                gmv=captures['gmv_randomized'].value,
                shop_fee=captures['shop_fee'].value,
                vendor_fee=captures['offer_vendor_fee'].value,
                price=captures['price'].value,
                shop_fee_pessimization_coef=pessimization_coef,
                rp_fee=100,
            )
            self.assertAlmostEqual(auction_value_recomputed, captures['auction_value_parsed'].value, delta=0.01)

        rearr_flags_dict = {
            'market_use_minimal_amnesty_in_cpa_shop_incut': 1,  # Включаем флаг взятия минимальной амнистии при аукционе во врезке
            'market_buybox_auction_cpa_shop_incut': 1,
            'market_blue_buybox_shop_incut_price_rel_max_threshold': 1.05,
            'market_buybox_auction_coef_b_multiplicative_bid_coef_cs_incut': 0.001,
            'market_buybox_auction_coef_1p_pessimization_cs_incut': 1.0,
        }

        # Сначала проверяем поведение без пессимизации
        rearr_flags_dict['market_buybox_auction_coef_1p_pessimization_cs_incut'] = 1.0
        check_for_rearrs(rearr_flags_dict, pessimization_coef=1)

        # Включаем пессимизацию
        rearr_flags_dict['market_buybox_auction_coef_1p_pessimization_cs_incut'] = 0.7
        check_for_rearrs(rearr_flags_dict, pessimization_coef=0.7)

    @classmethod
    def prepare_model_ratings_logging(cls):
        cls.index.gltypes += [  # Фильтр для проверки skuAwareSpecs-полей
            GLType(
                hid=3010,
                param_id=10123,
                cluster_filter=True,
                gltype=GLType.ENUM,
                values=[
                    GLValue(value_id=1, text='gl_filter_value1'),
                ],
                model_filter_index=1,
                xslname='sku_filter',
            ),
        ]
        cls.index.model_description_templates += [
            ModelDescriptionTemplates(
                hid=3010,
                friendlymodel=['model friendly {sku_filter}'],
                model=[("Основное", {'model full': '{sku_filter}'})],
            ),
        ]
        cls.index.models += [
            Model(
                hid=3010,
                hyperid=3010,
                ts=510,
                title='model_10_ratings',
                vbid=11,
                opinion=Opinion(
                    total_count=100, positive_count=95, rating=4.5, precise_rating=4.58, rating_count=200, reviews=5
                ),
            ),
            Model(
                hid=3011,
                hyperid=3011,
                ts=511,
                title='model_11_ratings',
                vbid=13,
                opinion=Opinion(
                    total_count=100, positive_count=92, rating=4.2, precise_rating=4.22, rating_count=205, reviews=4
                ),
            ),
        ]
        cls.index.mskus += [
            MarketSku(
                title="model_3010_msku_1_ratings",
                hid=3010,
                hyperid=3010,
                sku=100010,
                delivery_buckets=[1234],
                blue_offers=[
                    BlueOffer(
                        price=1600,
                        feedid=803,
                        fee=200,
                        waremd5="BLUE-100010-FEED-0001Q",
                        title="model_3010 3P buybox offer ratings",
                        ts=7017,
                    ),
                ],
                glparams=[GLParam(param_id=10123, value=1)],
            ),
            MarketSku(
                title="model_3011_msku_1_ratings",
                hid=3011,
                hyperid=3011,
                sku=100011,
                delivery_buckets=[1234],
                blue_offers=[
                    BlueOffer(
                        price=1700,
                        feedid=803,
                        fee=300,
                        waremd5="BLUE-100011-FEED-0001Q",
                        title="model_3011 3P buybox offer ratings",
                        ts=7018,
                    ),
                ],
            ),
        ]

    def test_model_ratings_logging(self):
        """
        Проверяем, что в cpa_shop_incut в ответе репорта есть рейтинги моделей и что значение preciseRating пишется в поле analogs_reason_score в shows-log,
        когда включен флаг market_cpa_shop_incut_model_rating_to_analogs_fields
        """
        request_base_cpa_shop_incut = (
            "place=cpa_shop_incut&pp=18&text=ratings&numdoc=10&min-num-doc=1&debug=da&rearr-factors="
        )
        rearr_flags_dict = {
            "market_cpa_shop_incut_model_rating_to_analogs_fields": 1,
        }
        response = self.report.request_json(request_base_cpa_shop_incut + dict_to_rearr(rearr_flags_dict))
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "slug": "model-3011-3p-buybox-offer-ratings",
                        "wareId": "BLUE-100011-FEED-0001Q",
                        "model": {
                            "id": 3011,
                            "rating": 4.2,
                            "preciseRating": 4.22,
                        },
                    },
                    {
                        "slug": "model-3010-3p-buybox-offer-ratings",
                        "wareId": "BLUE-100010-FEED-0001Q",
                        "model": {
                            "id": 3010,
                            "rating": 4.5,
                            "preciseRating": 4.58,
                        },
                    },
                ]
            },
        )
        self.show_log_tskv.expect(ware_md5="BLUE-100011-FEED-0001Q", analog_reason_score=4.22, url_type=6)
        self.show_log_tskv.expect(ware_md5="BLUE-100010-FEED-0001Q", analog_reason_score=4.58, url_type=6)

    @classmethod
    def prepare_fashion_blacklist(cls):
        cls.index.hypertree += [
            HyperCategory(
                hid=7877999,
                name='Одежда, обувь и аксессуары',
            ),
        ]
        cls.index.incut_black_list_fb += [
            IncutBlackListFb(texts=['fashion'], inclids=['PremiumAds']),
            IncutBlackListFb(subtreeHids=[7877999], inclids=['PremiumAds']),
        ]
        cls.index.fashion_categories += [
            FashionCategory("CATEGORY_COMMON", 7877999),
        ]
        cls.index.models += [
            Model(
                hid=7877999,
                hyperid=4010,
                ts=610,
                title='model_10_fashion',
                vbid=11,
            ),
        ]
        cls.index.mskus += [
            MarketSku(
                title="model_4010_msku_1_fashion",
                hid=7877999,
                hyperid=4010,
                sku=120010,
                delivery_buckets=[1234],
                blue_offers=[
                    BlueOffer(
                        price=1600,
                        feedid=803,
                        fee=200,
                        waremd5="BLUE-120010-FEED-0001Q",
                        title="model_4010 3P buybox offer fashion",
                        ts=8017,
                    ),
                ],
            ),
        ]

    def test_fashion_blacklist(self):
        """
        Проверяем работу флага market_premium_ads_gallery_no_blacklist_for_fashion, который выключает blacklist для fashion
        """
        request_base_cpa_shop_incut = (
            "place=cpa_shop_incut&pp=162&numdoc=10&min-num-doc=1&hid=7877999&text=fashion&rearr-factors="
        )
        rearr_flags_dict = {}
        # Сначала ожидаем пустой ответ, так как категория fashion в blacklist
        response = self.report.request_json(request_base_cpa_shop_incut + dict_to_rearr(rearr_flags_dict))
        self.assertFragmentIn(
            response,
            {
                "results": EmptyList(),
            },
        )
        # Выставляем флаг, который отключит blacklist на категорию fashion, и врезка соберётся
        rearr_flags_dict["market_premium_ads_gallery_no_blacklist_for_fashion"] = 1
        response = self.report.request_json(request_base_cpa_shop_incut + dict_to_rearr(rearr_flags_dict))
        self.assertFragmentIn(
            response,
            {
                "results": ElementCount(1),
            },
        )

    def test_sku_aware_fields(self):
        """
        Проверяем, что в cpa_shop_incut в ответе репорта есть skuAware поля, когда включён флаг market_cpa_shop_incut_request_sku_aware_fields
        """
        request_base_cpa_shop_incut = "place=cpa_shop_incut&pp=18&text=ratings&numdoc=10&min-num-doc=1&show-models-specs=msku-friendly,msku-full&debug=da&rearr-factors="
        rearr_flags_dict = {
            "market_cpa_shop_incut_request_sku_aware_fields": 1,
        }
        response = self.report.request_json(request_base_cpa_shop_incut + dict_to_rearr(rearr_flags_dict))
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "slug": "model-3010-3p-buybox-offer-ratings",
                        "wareId": "BLUE-100010-FEED-0001Q",
                        "skuAwareTitles": {
                            "raw": "model_3010_msku_1_ratings",  # title, пришедший не от оффера, а от msku
                            "highlighted": [
                                {
                                    "value": "model_3010_msku_1_",
                                },
                                {
                                    "value": "ratings",
                                },
                            ],
                        },
                        "skuAwarePictures": [
                            {
                                "entity": "picture",
                                "original": NotEmpty(),
                            },
                        ],
                        "skuAwareSpecs": {  # ModelDescriptionTemplates для hid=3010
                            "full": [
                                {
                                    "groupName": "Основное",
                                    "groupSpecs": [
                                        {
                                            "name": "model full",
                                            "value": "gl_filter_value1",
                                        },
                                    ],
                                },
                            ],
                            "friendly": [
                                "model friendly gl_filter_value1",
                            ],
                            "friendlyext": [
                                {
                                    "value": "model friendly gl_filter_value1",
                                    "usedParams": [10123],  # id GL-фильтра
                                },
                            ],
                        },
                    },
                ],
            },
        )
        # Проверяем, что можно выключить флагом запрос за SKU
        rearr_flags_dict["market_cpa_shop_incut_request_sku_aware_fields"] = 0
        response = self.report.request_json(request_base_cpa_shop_incut + dict_to_rearr(rearr_flags_dict))
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "slug": "model-3010-3p-buybox-offer-ratings",
                        "wareId": "BLUE-100010-FEED-0001Q",
                        "skuAwareTitles": Absent(),
                        "skuAwarePictures": Absent(),
                        "skuAwareSpecs": Absent(),
                    },
                ],
            },
        )

    @classmethod
    def prepare_1p_avg_autostrategy(cls):
        cls.index.shops += [
            Shop(
                fesh=2000,
                datafeed_id=2000,
                priority_region=213,
                regions=[213],
                name="Один 1P поставщик",
                supplier_type=Shop.FIRST_PARTY,
                is_supplier=True,
                blue=Shop.BLUE_REAL,
                warehouse_id=145,
                fulfillment_program=True,
            ),
            Shop(fesh=2001, priority_region=213, cpa=Shop.CPA_REAL, name='CPA Магазин в Москве #2001'),
            Shop(fesh=2002, priority_region=213, cpa=Shop.CPA_REAL, name='CPA Магазин в Москве #2002'),
            Shop(fesh=2003, priority_region=213, cpa=Shop.CPA_REAL, name='CPA Магазин в Москве #2003'),
            Shop(fesh=2004, priority_region=213, cpa=Shop.CPA_REAL, name='CPA Магазин в Москве #2004'),
            Shop(fesh=2005, priority_region=213, cpa=Shop.CPA_REAL, name='CPA Магазин в Москве #2005'),
        ]
        cls.index.models += [
            Model(hid=3018, hyperid=3018, ts=509, title='model_9', vbid=11),
        ]
        cls.index.mskus += [
            MarketSku(
                title="model_3018_msku_1",
                hid=3018,
                hyperid=30090,
                sku=300909,
                delivery_buckets=[1234],
                blue_offers=[
                    BlueOffer(
                        price=1640,
                        feedid=2000,
                        fee=220,  # если включен market_buybox_auction_coef_1p_pessimization_cs_incut, это fee игнорируется, потому что 1p. Будет взята рекомендованная ставка
                        waremd5="BLUE-300909-FEED-0001Q",
                        title="model_3018 1P автостратегия",
                        ts=80015,
                    ),
                    BlueOffer(
                        price=1640,
                        feedid=803,
                        fee=200,
                        waremd5="BLUE-300909-FEED-0002Q",
                        title="model_3018 3P автостратегия",
                        ts=80016,
                    ),
                ],
            ),
            MarketSku(
                title="model_3018_msku_2",
                hid=3018,
                hyperid=30096,
                sku=300906,
                delivery_buckets=[1234],
                blue_offers=[
                    BlueOffer(
                        price=1641,
                        feedid=2000,
                        fee=220,
                        waremd5="BLUE-300906-FEED-0001Q",
                        title="model_3018 1P автостратегия 2",
                        ts=80017,
                    ),
                ],
            ),
        ]
        cls.index.recommended_fee += [
            RecommendedFee(hyper_id=3018, recommended_bid=0.0220),
        ]
        cls.index.reserveprice_fee += [
            ReservePriceFee(hyper_id=3018, reserveprice_fee=0.01),
        ]
        cls.index.offers += [
            Offer(
                hyperid=30091,
                hid=3018,
                fesh=2001,
                ts=200011,
                price=1000,
                fee=150,
                title='CPA автостратегия 1',
                cpa=Offer.CPA_REAL,
            ),
            Offer(
                hyperid=30092,
                hid=3018,
                fesh=2002,
                ts=200012,
                price=1000,
                fee=120,
                title='CPA автостратегия 2',
                cpa=Offer.CPA_REAL,
            ),
            Offer(
                hyperid=30093,
                hid=3018,
                fesh=2003,
                ts=200013,
                price=1000,
                fee=100,
                title='CPA автостратегия 3',
                cpa=Offer.CPA_REAL,
            ),
            Offer(
                hyperid=30094,
                hid=3018,
                fesh=2004,
                ts=200014,
                price=1000,
                fee=90,
                title='CPA автостратегия 4',
                cpa=Offer.CPA_REAL,
            ),
            Offer(
                hyperid=30095,
                hid=3018,
                fesh=2005,
                ts=200015,
                price=1000,
                fee=80,
                title='CPA автостратегия 5',
                cpa=Offer.CPA_REAL,
            ),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 80015).respond(0.012)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 80016).respond(0.009)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 80017).respond(0.01)

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 200011).respond(0.02)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 200012).respond(0.0016)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 200013).respond(0.0017)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 200014).respond(0.0018)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 200015).respond(0.0019)

    def test_text_result_in_avg_autostrategy_raised_not_1p_prior_disabled(self):
        response = self.report.request_json(
            'place=cpa_shop_incut&text=автостратегия&debug=1&rearr-factors=market_premium_ads_gallery_default_min_num_doc=0;market_cs_incut_avg_autostrategy_raise_not_1p=0;'
            'market_cs_incut_enable_online_avg_autostrategy=1;market_cs_incut_avg_autostrategy_prob_rnd=0.001;market_buybox_auction_coef_1p_pessimization_cs_incut=0;'
        )
        self.assertFragmentIn(response, "on position=1; mandatoryFeeIncrease=24; resultCurrentOfferFee=125")
        self.assertFragmentIn(response, "on position=2; mandatoryFeeIncrease=32; resultCurrentOfferFee=180")

        self.assertFragmentIn(response, "Offer moved to pos=1 from 3")
        self.assertFragmentIn(response, "Offer moved to pos=2 from 2")
        self.assertFragmentIn(response, "Offer moved to pos=3 from 1")
        self.assertFragmentIn(response, "Offer moved to pos=4 from 4")
        self.assertFragmentIn(response, "Offer moved to pos=5 from 5")

    def test_text_result_in_avg_autostrategy_raised_not_1p_prior_enabled(self):
        response = self.report.request_json(
            'place=cpa_shop_incut&text=автостратегия&debug=1&rearr-factors=market_premium_ads_gallery_default_min_num_doc=0;market_cs_incut_avg_autostrategy_raise_not_1p=1;'
            'market_cs_incut_enable_online_avg_autostrategy=1;market_cs_incut_avg_autostrategy_prob_rnd=0.001;market_buybox_auction_coef_1p_pessimization_cs_incut=0;'
        )
        self.assertFragmentIn(response, "on position=1; mandatoryFeeIncrease=1906; resultCurrentOfferFee=2013")
        self.assertFragmentIn(response, "on position=2; mandatoryFeeIncrease=24; resultCurrentOfferFee=125")
        self.assertFragmentIn(response, "on position=3; mandatoryFeeIncrease=32; resultCurrentOfferFee=180")

        self.assertFragmentIn(response, "Offer moved to pos=1 from 4")
        self.assertFragmentIn(response, "Offer moved to pos=2 from 3")
        self.assertFragmentIn(response, "Offer moved to pos=3 from 2")
        self.assertFragmentIn(response, "Offer moved to pos=4 from 1")
        self.assertFragmentIn(response, "Offer moved to pos=5 from 5")

    def test_shop_fields_in_vendor_urls(self):
        """
        Проверяем, что в promotion-ссылки (вендорские) попадает информация о мерче
        https://st.yandex-team.ru/MARKETOUT-46914
        Выключается флагом market_money_add_shop_params_to_vendor_clicks_log
        """
        request_base_cpa_shop_incut = (
            "place=cpa_shop_incut&pp=18&text=автостратегия&numdoc=10&min-num-doc=1&debug=da&show-urls=cpa,promotion"
            "&puid=456789&uuid=12345"
        )
        rearr_flags_dict = {}
        response = self.report.request_json(request_base_cpa_shop_incut)
        # Парсим характеристики оффера из выдачи
        feed_id_captured, offer_id_captured = Capture(), Capture()
        supplier_id_captured, shop_id_captured = Capture(), Capture()
        shop_fee_captured, brokered_fee_captured = Capture(), Capture()
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "supplier": {
                            "id": NotEmpty(capture=supplier_id_captured),
                        },
                        "shop": {
                            "id": NotEmpty(capture=shop_id_captured),
                        },
                        "debug": {
                            "feed": {
                                "id": NotEmpty(capture=feed_id_captured),
                                "offerId": NotEmpty(capture=offer_id_captured),
                            },
                            "sale": {
                                "shopFee": NotEmpty(capture=shop_fee_captured),
                                "brokeredFee": NotEmpty(capture=brokered_fee_captured),
                            },
                        },
                    },
                ],
            },
        )
        # Проверяем, что требуемые характеристики оффера попали в promotion-ссылку
        self.click_log.expect(
            ClickType.PROMOTION,
            shop_id=shop_id_captured.value,
            supplier_id=supplier_id_captured.value,
            feed_id=feed_id_captured.value,
            offer_id=offer_id_captured.value,
            shop_fee=shop_fee_captured.value,
            shop_fee_ab=brokered_fee_captured.value,
            url_type=UrlType.PROMOTION,
            puid=456789,  # Заодно проверяем, что puid попадает в лог
            uuid=12345,
        )

        # Проверим, что флагом market_money_add_shop_params_to_vendor_clicks_log можно выключить
        rearr_flags_dict["market_money_add_shop_params_to_vendor_clicks_log"] = 0
        response = self.report.request_json(
            request_base_cpa_shop_incut + "&rearr-factors=" + dict_to_rearr(rearr_flags_dict)
        )
        self.click_log.expect(
            ClickType.PROMOTION,
            shop_id=None,
            supplier_id=None,
            feed_id=None,
            offer_id=None,
            shop_fee=None,
            shop_fee_ab=None,
            url_type=UrlType.PROMOTION,
        )

    @classmethod
    def prepare_group_by_msku(cls):
        # Хотим, чтобы для теста группировок по msku выдача искалась по hid=3019
        # или по текстовому запросу "grouping"
        cls.index.models += [
            Model(hid=3019, hyperid=3019, ts=301900, title='model_3019', vbid=11),
            Model(hid=3019, hyperid=3020, ts=302000, title='model_3020', vbid=11),
            Model(hid=3019, hyperid=3021, ts=302100, title='model_3021', vbid=11),
        ]
        cls.index.mskus += [
            MarketSku(
                title="model_3019_msku_1",
                hid=3019,
                hyperid=3019,
                sku=301901,
                delivery_buckets=[1234],
                blue_offers=[
                    BlueOffer(
                        price=2000,
                        feedid=803,
                        fee=200,
                        waremd5="BLUE-301901-FEED-0001Q",
                        title="model_3019_sku_301901 grouping 1",
                        ts=3019011,
                    ),
                    BlueOffer(
                        # Этот оффер текущего мскю выигрывает байбокс за счёт ставки
                        price=2000,
                        feedid=803,
                        fee=250,
                        waremd5="BLUE-301901-FEED-0002Q",
                        title="model_3019_sku_301901 grouping 2",
                        ts=3019012,
                    ),
                ],
            ),
            MarketSku(
                title="model_3019_msku_2",
                hid=3019,
                hyperid=3019,
                sku=301902,
                delivery_buckets=[1234],
                blue_offers=[
                    BlueOffer(
                        # Этот оффер текущего мскю выигрывает байбокс за счёт ставки
                        # У этой же модели есть победитель от другого мскю, но этот оффер
                        # тоже покажется, если включена группировка по мскю
                        price=2000,
                        feedid=803,
                        fee=240,
                        waremd5="BLUE-301902-FEED-0001Q",
                        title="model_3019_sku_301902 grouping 1",
                        ts=3019021,
                    ),
                    BlueOffer(
                        price=2000,
                        feedid=803,
                        fee=220,
                        waremd5="BLUE-301902-FEED-0002Q",
                        title="model_3019_sku_301902 grouping 2",
                        ts=3019022,
                    ),
                ],
            ),
            MarketSku(
                title="model_3020_msku_1",
                hid=3019,
                hyperid=3020,
                sku=302001,
                delivery_buckets=[1234],
                blue_offers=[
                    BlueOffer(
                        # Это оффер-победитель этого мскю
                        # Но у модели 3020 есть ещё оффер без мскю со ставкой выше
                        price=2000,
                        feedid=803,
                        fee=170,
                        waremd5="BLUE-302001-FEED-0001Q",
                        title="model_3020_sku_302001 grouping 1",
                        ts=3020011,
                    ),
                    BlueOffer(
                        price=2000,
                        feedid=803,
                        fee=160,
                        waremd5="BLUE-302001-FEED-0002Q",
                        title="model_3020_sku_302001 grouping 2",
                        ts=3020012,
                    ),
                ],
            ),
            MarketSku(
                title="model_3021_msku_1",
                hid=3019,
                hyperid=3021,
                sku=302101,
                delivery_buckets=[1234],
                blue_offers=[
                    BlueOffer(
                        price=2000,
                        feedid=803,
                        fee=165,
                        waremd5="BLUE-302101-FEED-0001Q",
                        title="model_3021_sku_302101 grouping 1",
                        ts=3021011,
                    ),
                ],
            ),
        ]
        cls.index.offers += [
            Offer(
                hyperid=3020,
                hid=3019,
                fesh=801,
                ts=302001,
                price=2000,
                fee=180,
                title='model_3020_no_msku grouping',
                cpa=Offer.CPA_REAL,
                waremd5="off-no-msku-3020-bid1Q",
            ),
        ]
        # Всем офферам ставим одинаковую релевантность, чтобы ранжирование было по ставке
        for ts in [3019011, 3019012, 3019021, 3019022, 3020011, 3020012, 3021011, 302001]:
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, ts).respond(0.3)

    def test_group_by_msku(self):
        """
        Хотим проверить, что в cpa_shop_incut работает группировка по msku.
        Так как может быть оффер без msku, то если у документа нет msku,
        то ключом группы будет hyperid (id модели)

        При этом если есть другая модель, у которой msku_id равен hyperid первой модели,
        то офферы обоих моделей попадут в одну группу

        Проверяем, что в выдаче могут оказаться офферы разных мскю одной модели,
        то есть, что группировка по msku работает
        """
        request_cgi = "place=cpa_shop_incut&pp=18&text=grouping&numdoc=30&min-num-doc=1&debug=da&rearr-factors="
        rearr_flags_dict = {
            # Включаем флаг, включающий группировку по msku
            "market_group_by_msku_in_cpa_shop_incut": 1,
        }
        response = self.report.request_json(request_cgi + dict_to_rearr(rearr_flags_dict))
        self.assertFragmentIn(
            response,
            {
                # Проверяем, что в выдаче есть разные msku от одной модели
                # От модели 3019 есть мскю 301901 и 301902
                # От модели 3020 есть отдельно мскю 302001 и отдельно
                # оффер off-no-msku-3020-bid1Q этой модели, но без мскю
                "results": [
                    {
                        "marketSku": str(msku) if msku is not None else Absent(),
                    }
                    for msku in [301901, 301902, None, 302001, 302101]
                ],
            },
            allow_different_len=False,  # важно кол-во офферов в выдаче
            preserve_order=True,
        )

        # Убираем флаг, проверяем, что вернулась группировка по моделям
        # TODO: убрать эту часть теста, когда будем убирать флаг
        del rearr_flags_dict["market_group_by_msku_in_cpa_shop_incut"]
        response = self.report.request_json(request_cgi + dict_to_rearr(rearr_flags_dict))
        self.assertFragmentIn(
            response,
            {
                # От модели 3019 оффера обеих мскю попали в одну группу
                # И от модели 3020 оффер с мскю и оффер без мскю попали в одну группу
                "results": [
                    {
                        "debug": {"modelId": modelid},
                    }
                    for modelid in [3019, 3020, 3021]
                ],
            },
            allow_different_len=False,  # важно кол-во офферов в выдаче
            preserve_order=True,
        )

    def test_group_by_msku_autobroker(self):
        """
        Проверяем, что при группировке по msku не ломается автоброкер
        """
        request_cgi = "place=cpa_shop_incut&pp=18&text=grouping&numdoc=30&min-num-doc=1&debug=da&rearr-factors="
        rearr_flags_dict = {
            # Включаем флаг, включающий группировку по msku
            "market_group_by_msku_in_cpa_shop_incut": 1,
        }
        response = self.report.request_json(request_cgi + dict_to_rearr(rearr_flags_dict))
        # Релевантности одинаковые, поэтому амнистированная ставка равна ставке подпирающего оффера
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "debug": {
                            "wareId": "BLUE-301901-FEED-0002Q",
                            "sale": {
                                "shopFee": 250,
                                # Подпирается оффером следующего msku этой же модели
                                "brokeredFee": 240,
                            },
                        },
                    },
                    {
                        "debug": {
                            "wareId": "BLUE-301902-FEED-0001Q",
                            "sale": {
                                "shopFee": 240,
                                # Подпирается оффером этого же msku
                                "brokeredFee": 220,
                            },
                        },
                    },
                    {
                        # DSBS-оффер без мскю, но от модели 3020
                        # Так как у него нет мскю, он попадает в отдельную группу по hyperid
                        # В его байбоксе не учитываются другие офферы модели 3020, потому что у тех есть msku
                        "debug": {
                            "wareId": "off-no-msku-3020-bid1Q",
                            "sale": {
                                "shopFee": 180,
                                "brokeredFee": 170,
                            },
                        },
                    },
                    {
                        "debug": {
                            "wareId": "BLUE-302001-FEED-0001Q",
                            "sale": {
                                "shopFee": 170,
                                # Подпирается оффером следующего msku
                                "brokeredFee": 165,
                            },
                        },
                    },
                    {
                        "debug": {
                            "wareId": "BLUE-302101-FEED-0001Q",
                            "sale": {
                                "shopFee": 165,
                                # Оффер без подпорки
                                "brokeredFee": 1,
                            },
                        },
                    },
                ],
            },
            preserve_order=True,
            allow_different_len=False,
        )


if __name__ == '__main__':
    main()

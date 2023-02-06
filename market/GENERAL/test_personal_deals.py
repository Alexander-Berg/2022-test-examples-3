#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

import random

from core.types import (
    CategoryStatsRecord,
    Currency,
    HyperCategory,
    HyperCategoryType,
    Model,
    Offer,
    Promo,
    PromoMSKU,
    PromoType,
    Region,
    Shop,
    Tax,
    Vat,
    YamarecCategoryRanksPartition,
    YamarecFeaturePartition,
    YamarecPlace,
)
from core.testcase import TestCase, main
from core.types.sku import MarketSku, BlueOffer

from core.dj import DjModel
from core.matcher import NotEmptyList

from unittest import skip


def prepare_categories(cls):
    """ """
    cls.settings.rgb_blue_is_cpa = True

    cls.index.regiontree += [
        Region(rid=213, name='Москва'),
        Region(rid=2, name='Санкт-Петербург'),
        Region(rid=64, name='Екатеринбург'),
    ]

    cls.index.hypertree += [
        HyperCategory(
            hid=1111,
            output_type=HyperCategoryType.GURU,
            children=[
                HyperCategory(hid=201, output_type=HyperCategoryType.GURU),
                HyperCategory(hid=202, output_type=HyperCategoryType.GURU),
                HyperCategory(hid=203, output_type=HyperCategoryType.GURU),
                HyperCategory(hid=204, output_type=HyperCategoryType.GURU),
                HyperCategory(
                    hid=207,
                    children=[
                        HyperCategory(hid=205, output_type=HyperCategoryType.GURU),
                        HyperCategory(hid=206, output_type=HyperCategoryType.GURU),
                    ],
                ),
                HyperCategory(hid=208, output_type=HyperCategoryType.GURULIGHT),
            ],
        ),
    ]

    cls.index.yamarec_places += [
        YamarecPlace(
            name=YamarecPlace.Name.CATEGORY_GENERIC,
            kind=YamarecPlace.Type.FORMULA,
            split_rule=YamarecPlace.SplitRule.ABT,
            partitions=[
                YamarecFeaturePartition(
                    feature_names=['category_id', 'position'],
                    feature_keys=['category_id'],
                    features=[
                        [201, 6],
                        [202, 5],
                        [203, 4],
                        [204, 3],
                        [205, 2],
                        [206, 1],
                        [208, 7],
                    ],
                    splits=[{'split': '1'}],
                ),
            ],
        ),
    ]

    cls.index.yamarec_places += [
        YamarecPlace(
            name=YamarecPlace.Name.COLDSTART_CATEGORIES,
            kind=YamarecPlace.Type.CATEGORY_RANKS,
            partitions=[
                YamarecCategoryRanksPartition(category_list=[90799], splits=['*']),
            ],
        )
    ]


class _Offers(object):
    waremd5s = [
        'Sku1Price5-IiLVm1Goleg',
        'Sku2Price50-iLVm1Goleg',
        'Sku3Price45-iLVm1Goleg',
        'Sku4Price36-iLVm1Goleg',
        'Sku5Price15-iLVm1Goleg',
        'Sku5Price16-iLVm1Goleg',
        'Sku6Price14-iLVm1Goleg',
    ]
    feed_ids = [2] * len(waremd5s)
    prices = [500, 50, 45, 36, 15, 16, 14]
    discounts = [9, 10, 45, 36, 15, None, 1]
    """
    categories = [201, 202, 201, 203, 202, 201, 208]
    """
    shop_skus = ['Feed_{feedid}_sku{i}'.format(feedid=feedid, i=i + 1) for i, feedid in enumerate(feed_ids)]
    model_ids = list(range(1, len(waremd5s) + 1))
    sku_offers = [
        BlueOffer(price=price, vat=Vat.VAT_10, offerid=shop_sku, feedid=feedid, waremd5=waremd5, discount=discount)
        for feedid, waremd5, price, shop_sku, discount in zip(feed_ids, waremd5s, prices, shop_skus, discounts)
    ]


class T(TestCase):
    @classmethod
    def prepare(cls):
        prepare_categories(cls)

        # shops
        cls.index.shops += [
            Shop(
                fesh=1,
                datafeed_id=1,
                priority_region=213,
                name='blue_shop_1',
                currency=Currency.RUR,
                fulfillment_virtual=True,
                virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
                tax_system=Tax.OSN,
                cpa=Shop.CPA_REAL,
            ),
            Shop(
                fesh=3,
                datafeed_id=3,
                priority_region=213,
                name='blue_shop_3',
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                cpa=Shop.CPA_REAL,
                warehouse_id=145,
            ),
        ]

        # models with randomly selected ts
        random.seed(0)
        random_ts = list(range(1, len(_Offers.model_ids) + 1))
        random.shuffle(random_ts)

        categories = [201, 202, 201, 203, 202, 201, 208]
        cls.index.models += [
            Model(hyperid=model_id, hid=category, ts=ts)
            for model_id, category, ts in zip(_Offers.model_ids, categories, random_ts)
        ]

        # market skus
        cls.index.mskus += [
            MarketSku(
                title='Blue offer {sku}'.format(sku=shop_sku),
                hyperid=hyperid,
                sku='{i}'.format(i=hyperid),
                waremd5='Sku{i}-wdDXWsIiLVm1goleg'.format(i=hyperid),
                blue_offers=[sku_offer],
            )
            for hyperid, shop_sku, sku_offer in zip(_Offers.model_ids, _Offers.shop_skus, _Offers.sku_offers)
        ]

        cls.recommender.on_request_models_of_interest(user_id='yandexuid:').respond({'models': []})
        cls.recommender.on_request_models_of_interest(user_id='yandexuid:1001').respond({'models': ['2']})
        '''
        cls.index.offers += [
            Offer(hyperid=1, title="Фен Saturn ST-HC7355", fesh=1, price_old=1213, price=342)
        ]
        '''

        cls.index.blue_category_region_stat += [
            CategoryStatsRecord(201, 213, n_offers=3, n_discounts=3),
            CategoryStatsRecord(202, 213, n_offers=2, n_discounts=2),
            CategoryStatsRecord(203, 213, n_offers=1, n_discounts=1),
            CategoryStatsRecord(208, 213, n_offers=1, n_discounts=1),
        ]

    def test_discount_p_sorting(self):
        response = self.report.request_json(
            "place=deals&pp=18&numdoc=100&rgb=BLUE&rearr-factors=split=1&allow-collapsing=1&rids=213&filter-discount-only=1&how=discount_p&hid=201,202,203&show-personal=0"
        )
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 5,
                    "results": [
                        {"categories": [{"id": 201}], "id": 3},
                        {"categories": [{"id": 203}], "id": 4},
                        {"categories": [{"id": 202}], "id": 5},
                        {"categories": [{"id": 201}], "id": 1},
                        {"categories": [{"id": 202}], "id": 2},
                    ],
                }
            },
            preserve_order=True,
        )

    def test_discount_p_sorting_cpa(self):
        response = self.report.request_json(
            "place=deals&pp=18&numdoc=100&cpa=real&rearr-factors=split=1&allow-collapsing=1&rids=213&filter-discount-only=1&how=discount_p&hid=201,202,203&show-personal=0"
        )
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 5,
                    "results": [
                        {"categories": [{"id": 201}], "id": 3},
                        {"categories": [{"id": 203}], "id": 4},
                        {"categories": [{"id": 202}], "id": 5},
                        {"categories": [{"id": 202}], "id": 2},
                        {"categories": [{"id": 201}], "id": 1},
                    ],
                }
            },
            preserve_order=True,
        )

    @skip('test does not work now (bug in category rearrange?)')
    def test_ddiscount_sorting(self):
        request = "place=deals&pp=18&numdoc=100&rearr-factors=split=1&allow-collapsing=1&rids=213&filter-discount-only=1&how=ddiscount&hid=201,202,203&show-personal=0"
        for cgi in ["rgb=blue", "cpa=real"]:
            r = "&".join([request, cgi])
            response = self.report.request_json(r)
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "total": 5,
                        "results": [
                            {"categories": [{"id": 201}], "id": 1},  # abs discount 45
                            {"categories": [{"id": 203}], "id": 4},  # abs discount 12.96, new category
                            {"categories": [{"id": 202}], "id": 2},  # abs discount 5, new category
                            {"categories": [{"id": 201}], "id": 3},  # abs discount 20.25
                            {"categories": [{"id": 202}], "id": 5},  # abs discount 2.25
                        ],
                    }
                },
                preserve_order=True,
            )

    def test_personal_no_config(self):
        request = "place=deals&pp=18&numdoc=100&allow-collapsing=1&rids=213&filter-discount-only=1&how=discount_p&hid=201,202,203"
        for cgi in ["rgb=blue", "cpa=real"]:
            r = "&".join([request, cgi])
            response = self.report.request_json(r)
            self.error_log.ignore("Personal category config is not available")
            self.assertFragmentIn(
                response,
                {
                    "search": {"total": 0},
                },
                preserve_order=True,
            )

    def personalized_request(self, hids, personal):
        request = "place=deals&pp=18&numdoc=100&allow-collapsing=1&rids=213&filter-discount-only=1&how=discount_p{}&yandexuid=1001{}&hid=202".format(
            personal, hids
        )
        for cgi in ["rgb=blue", "cpa=real"]:
            r = "&".join([request, cgi])
            response = self.report.request_json(r)
            self.error_log.ignore("Personal category config is not available for user 1001")

            # models '2' and '5' are in 202 category, '5' model has a greater discount than '2'
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "total": 2,
                        "results": [
                            {"categories": [{"id": 202}], "id": 5},
                            {"categories": [{"id": 202}], "id": 2},
                        ],
                    }
                },
                preserve_order=True,
            )

    def test_discount_p_sorting_personal(self):
        for personal in ['', '&show-personal=1']:
            self.personalized_request(hids='&hid=201,202,203', personal=personal)

    def test_nonpersonal_request_without_hids_on_blue(self):
        """Проверяем, что если указан show-personal=0 для Синего Маркета,
        то неперсонализированная выдача будет только в том случае, если были указаны категории.
        Если в неперсонализированном запросе категории не указаны, то запрос будет персонализирован."""
        self.personalized_request(hids='', personal='&show-personal=0')

    def test_nonpersonal_request_with_hids_on_blue(self):
        """Проверяем, что если указан show-personal=0 для Синего Маркета,
        то неперсонализированная выдача будет только в том случае, если были указаны категории.
        Если в неперсонализированном запросе категории указаны, то запрос будет без изменений."""
        request = "place=deals&pp=18&numdoc=100&allow-collapsing=1&rids=213&filter-discount-only=1&how=discount_p&show-personal=0&hid=201"
        for cgi in ["rgb=blue", "cpa=real"]:
            r = "&".join([request, cgi])
            response = self.report.request_json(r)
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "total": 2,
                        "results": [{"categories": [{"id": 201}], "id": 3}, {"categories": [{"id": 201}], "id": 1}],
                    },
                },
                preserve_order=True,
            )

    @classmethod
    def prepare_nonpersonal_request_with_hids_on_blue_with_dj_flag(cls):
        recommended_models = [DjModel(id="2", title='model#2')]

        cls.dj.on_request(exp='blue_deals', yandexuid='1001').respond(recommended_models)

    def test_nonpersonal_request_with_hids_on_blue_with_dj_flag(self):
        """
        https://st.yandex-team.ru/MARKETRECOM-2011
        Проверяем, что при наличии параметра hid выдача на синем происходит без
        обращения к dj, если используется старый флаг

        Проверяем, что если указан show-personal=0 для Синего Маркета,
        то неперсонализированная выдача будет только в том случае, если были указаны категории.
        Если в неперсонализированном запросе категории указаны, то запрос будет без изменений.
        """
        request = "place=deals&pp=18&numdoc=100&allow-collapsing=1&rids=213&filter-discount-only=1&how=discount_p&show-personal=0&hid=201&rearr-factors=market_dj_exp_for_blue_deals=blue_deals&puid=12345"  # noqa
        for cgi in ["rgb=blue", "cpa=real"]:
            r = "&".join([request, cgi])
            response = self.report.request_json(r)
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "total": 2,
                        "results": [{"categories": [{"id": 201}], "id": 3}, {"categories": [{"id": 201}], "id": 1}],
                    },
                },
                preserve_order=True,
            )

            self.assertFragmentNotIn(
                response,
                {
                    "search": {"results": [{"categories": [{"id": 202}], "id": 2}]},
                },
            )

    def test_nonpersonal_request_without_hids_on_green(self):
        """Проверяем, что если указан show-personal=0 для Белого Маркета,
        то запрос будет неперсонализирован даже если категорий не было."""
        for colour in ['GREEN', 'GREEN_WITH_BLUE']:
            response = self.report.request_json(
                "place=deals&pp=18&numdoc=100&rgb={}&allow-collapsing=1&rids=213&filter-discount-only=1&how=discount_p&show-personal=0".format(
                    colour
                )
            )
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "total": 21,
                        "results": [{"categories": [{"id": 201}], "id": 3}, {"categories": [{"id": 202}], "id": 5}],
                    },
                },
                preserve_order=True,
            )

    def test_collapsed_offer_is_do_in_case_hidd_grouping(self):
        """deals иногда вообще обходится одной единственной группировкой hidd
        и в качестве ДО берет оффер с минимальной ценой
        """
        # ожидаем 4 документа с непустыми ДО
        results = {"results": [{"entity": "product", "offers": {"items": NotEmptyList()}}] * 4}
        # в группировке используется только hidd
        groupping = {
            "report": {
                "context": {
                    "collections": {
                        "*": {
                            "g": ["1.hidd.100.20.-1"]
                        },  # в категории запрашивается минимальное количество документов необходимое для отображения
                    }
                }
            }
        }

        request = (
            'place=deals&numdoc=20&rgb=BLUE&allow-collapsing=1&rids=213&filter-discount-only=1&how=discount_p'
            + '&yandexuid=1001&show-personal=1&debug=da'
        )

        response = self.report.request_json(request)
        self.assertFragmentIn(response, results, allow_different_len=False)
        self.assertFragmentIn(response, groupping, allow_different_len=False)

    def test_force_guru_popularity(self):
        """Проверка того, что в ручке deals для синего маркета принудительно используется сортировка SF_GURU_POPULARITY"""
        response = self.report.request_json("place=deals&rgb=BLUE&hid=208&show-personal=0&debug=1")
        self.assertFragmentIn(response, "Sorting: SF_GURU_POPULARITY")
        response = self.report.request_json("place=deals&cpa=real&hid=208&show-personal=0&debug=1")
        self.assertFragmentIn(response, "Sorting: SF_GURU_POPULARITY")

        response = self.report.request_json("place=deals&rgb=GREEN&hid=208&debug=1")
        self.assertFragmentNotIn(response, "Sorting: SF_GURU_POPULARITY")

        # Если явно указана другая сортировка, не переключаемся на GURU_POPULARITY
        response = self.report.request_json("place=deals&rgb=BLUE&hid=208&show-personal=0&how=aprice&debug=1")
        self.assertFragmentIn(response, "Sorting: SF_PRICE")
        response = self.report.request_json("place=deals&cpa=real&hid=208&show-personal=0&how=aprice&debug=1")
        self.assertFragmentIn(response, "Sorting: SF_PRICE")

    def test_pruning(self):
        pruning_enabled_100000 = {
            "debug": {
                "report": {"context": {"collections": {"*": {"pron": ["prune", "pruncount66667"]}}}}  # 100000 * 2 / 3
            }
        }

        pruning_enabled_BLUE_1000 = {  # blue reqest has only search subrequests
            "debug": {
                "metasearch": {
                    "subrequests": [
                        {
                            "report": {
                                "context": {"collections": {"*": {"pron": ["prune", "pruncount667"]}}}  # 1000 * 2 / 3
                            }
                        }
                    ]
                }
            }
        }

        response = self.report.request_json('place=deals&rgb=GREEN&hid=208&debug=da')
        self.assertFragmentIn(response, pruning_enabled_100000)

        response = self.report.request_json('place=deals&hid=208&debug=da')
        self.assertFragmentIn(response, pruning_enabled_100000)

        response = self.report.request_json('place=deals&rgb=BLUE&hid=208&show-personal=0&debug=da')
        self.assertFragmentIn(response, pruning_enabled_BLUE_1000)
        response = self.report.request_json('place=deals&cpa=real&hid=208&show-personal=0&debug=da')
        self.assertFragmentIn(response, pruning_enabled_BLUE_1000)

        response = self.report.request_json(
            'place=deals&rgb=BLUE&hid=208&show-personal=0&filter-discount-only=1&debug=da'
        )
        self.assertFragmentIn(response, pruning_enabled_BLUE_1000)
        response = self.report.request_json(
            'place=deals&cpa=real&hid=208&show-personal=0&filter-discount-only=1&debug=da'
        )
        self.assertFragmentIn(response, pruning_enabled_BLUE_1000)

    @classmethod
    def prepare_buybox_offer(cls):
        cls.index.hypertree += [
            HyperCategory(
                hid=1112,
                output_type=HyperCategoryType.GURU,
                children=[
                    HyperCategory(hid=101, output_type=HyperCategoryType.GURU),
                    HyperCategory(hid=102, output_type=HyperCategoryType.GURU),
                    HyperCategory(hid=103, output_type=HyperCategoryType.GURU),
                    HyperCategory(hid=104, output_type=HyperCategoryType.GURU),
                    HyperCategory(hid=105, output_type=HyperCategoryType.GURU),
                ],
            ),
        ]

        # blue offer with blue_market_sku_1 - buybox, with discount 10%
        blue_offer_1 = BlueOffer(
            price=1350,
            offerid='shop_sku_1',
            feedid=2,
            waremd5='BlueOffer-1-WithDisc-w',
            price_old=1500,
            price_history=1500,
        )
        # blue offer with blue_market_sku_1 - not buybox, with discount 10%
        blue_offer_2 = BlueOffer(
            price=1800,
            offerid='shop_sku_2',
            feedid=2,
            waremd5='BlueOffer-2-WithDisc-w',
            price_old=2000,
            price_history=2000,
        )
        # blue offer with blue_market_sku_2 - buybox, without discount
        blue_offer_3 = BlueOffer(price=1000, offerid='shop_sku_3', feedid=2, waremd5='BlueOffer-3-NoDiscou-w')
        # blue offer with blue_market_sku_2 - not buybox, with discount 10%
        blue_offer_4 = BlueOffer(
            price=1350,
            offerid='shop_sku_4',
            feedid=2,
            waremd5='BlueOffer-4-WithDisc-w',
            price_old=1500,
            price_history=1500,
            history_price_is_valid=True,
        )
        # blue offer with blue_market_sku_3 - buybox, without discount, but in market promo
        blue_offer_5 = BlueOffer(price=1350, offerid='shop_sku_5', feedid=2, waremd5='BlueOffer-5-NoDiscou-w')
        # blue offer with blue_market_sku_3 - not buybox, without discount
        blue_offer_6 = BlueOffer(price=1450, offerid='shop_sku_6', feedid=2, waremd5='BlueOffer-6-NoDiscou-w')
        # blue offer with blue_market_sku_4 - buybox, with valid discount and market promo (not in market promo)
        blue_offer_7 = BlueOffer(
            price=1450,
            offerid='shop_sku_7',
            feedid=2,
            waremd5='BlueOffer-7-WithDisc-w',
            price_old=1500,
            price_history=1500,
        )
        # blue offer with blue_market_sku_5 - buybox, with valid discount (but for test it's category is not in blue buybox category regional stat)
        blue_offer_8 = BlueOffer(
            price=1350,
            offerid='shop_sku_8',
            feedid=2,
            waremd5='BlueOffer-8-WithDisc-w',
            price_old=1500,
            price_history=1500,
            history_price_is_valid=True,
        )

        # random timestamp
        num_categories = 6
        random.seed(0)
        random_ts = list(range(1, num_categories + 1))
        random.shuffle(random_ts)

        # models
        cls.index.models += [Model(hyperid=1234 + i, hid=101 + i, ts=random_ts[i]) for i in range(num_categories)]

        # market skus
        cls.index.mskus += [
            MarketSku(
                title='blue_market_sku_1',
                hyperid=1234,
                sku=11200001,
                waremd5='Sku1-wdDYWsIiLVm1goleg',
                blue_offers=[blue_offer_1, blue_offer_2],
            ),
            MarketSku(
                title='blue_market_sku_2',
                hyperid=1235,
                sku=11200002,
                waremd5='Sku2-wdDYWsIiLVm1goleg',
                blue_offers=[blue_offer_3, blue_offer_4],
            ),
            MarketSku(
                title='blue_market_sku_3',
                hyperid=1236,
                sku=11200003,
                waremd5='Sku3-wdDYWsIiLVm1goleg',
                blue_offers=[blue_offer_5, blue_offer_6],
            ),
            MarketSku(
                title='blue_market_sku_4',
                hyperid=1237,
                sku=11200004,
                waremd5='Sku4-wdDYWsIiLVm1goleg',
                blue_offers=[blue_offer_7],
            ),
            MarketSku(
                title='blue_market_sku_5',
                hyperid=1238,
                sku=11200005,
                waremd5='Sku5-msDYWsIiLVm1goleg',
                blue_offers=[blue_offer_8],
            ),
        ]

        # market promos
        cls.index.promos += [
            # forever promo on msku 11200003
            Promo(
                promo_type=PromoType.BLUE_3P_FLASH_DISCOUNT,
                key='JVvklxUgdnawSJPG4UhZGA',
                mskus=[
                    PromoMSKU(msku='11200003', market_promo_price=1400, market_old_price=1500),
                ],
            ),
            # forever promo on msku 11200004
            Promo(
                promo_type=PromoType.BLUE_3P_FLASH_DISCOUNT,
                key='JVvklxUgdnawSJPG4UhZGB',
                mskus=[
                    PromoMSKU(msku='11200004', market_promo_price=1500, market_old_price=2000),
                ],
            ),
        ]

        cls.recommender.on_request_models_of_interest(user_id='yandexuid:1002').respond({'models': ['1234']})
        cls.recommender.on_request_models_of_interest(user_id='yandexuid:1003').respond({'models': ['1235']})
        cls.recommender.on_request_models_of_interest(user_id='yandexuid:1004').respond({'models': ['1236']})
        cls.recommender.on_request_models_of_interest(user_id='yandexuid:1005').respond({'models': ['1237']})
        cls.recommender.on_request_models_of_interest(user_id='yandexuid:1006').respond({'models': ['1238']})

        cls.index.blue_category_region_stat += [
            CategoryStatsRecord(101, 213, n_offers=1, n_discounts=1),
            CategoryStatsRecord(103, 213, n_offers=1, n_discounts=1),
            CategoryStatsRecord(102, 213, n_offers=1, n_discounts=0),
            CategoryStatsRecord(104, 213, n_offers=1, n_discounts=0),
            CategoryStatsRecord(105, 213, n_offers=1, n_discounts=0),
        ]

    def check_buybox_with_discount(self, colour, user_id, id, category_id, wareId, discount, discount_filter=''):
        request = "place=deals&pp=18&numdoc=100&rgb={}&allow-collapsing=1&rids=213&how=discount_p&hid=1112&yandexuid={}{}".format(
            colour, user_id, discount_filter
        )
        response = self.report.request_json(request)
        self.error_log.ignore("Personal category config is not available for user {}".format(user_id))
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 1,
                    'results': [
                        {
                            'categories': [{'id': category_id}],
                            'id': id,
                            'offers': {'items': [{'wareId': wareId, 'prices': {'discount': {'percent': discount}}}]},
                        }
                    ],
                },
            },
            preserve_order=True,
        )

    def chech_buybox_without_discount(self, colour, user_id, discount_filter=''):
        request = "place=deals&pp=18&numdoc=100&rgb={}&allow-collapsing=1&rids=213&how=discount_p&hid=1112&yandexuid={}{}".format(
            colour, user_id, discount_filter
        )
        response = self.report.request_json(request)
        self.error_log.ignore("Personal category config is not available for user {}".format(user_id))
        self.assertFragmentIn(
            response,
            {
                "search": {"total": 0},
            },
            preserve_order=True,
        )

    def test_buybox_offer_with_discount_blue_market(self):
        """Проверяем, что по msku из персональной категории выдается только байбоксовый оффер.
        Если у байбоксового оффера была скидка, то он будет на выдаче."""
        self.check_buybox_with_discount(
            colour='BLUE', user_id=1002, id=1234, category_id=101, wareId='BlueOffer-1-WithDisc-w', discount=10
        )

    def test_buybox_offer_without_discount_blue_market(self):
        """Проверяем, что по msku из персональной категории выдается только байбоксовый оффер.
        Если у байбоксового оффера не было скидки, то никакого оффера с его msku не будет на выдаче,
        вне завизимости от того, была ли у других офферов этого msku  скидка."""
        self.chech_buybox_without_discount(colour='BLUE', user_id=1003)

    def test_buybox_with_market_promo_blue_market(self):
        """Проверяем, что по msku из персональной категории выдается только байбоксовый оффер.
        Если у байбоксовый оффер попал в маркетную акцию, то он будет на выдаче."""
        self.check_buybox_with_discount(
            colour='BLUE', user_id=1004, id=1236, category_id=103, wareId='BlueOffer-5-NoDiscou-w', discount=10
        )

    def test_buybox_without_market_promo_blue_market(self):
        """Проверяем, что по msku из персональной категории выдается только байбоксовый оффер.
        Если байбоксовый оффера не попадает в маркетныю акцию, то никакого оффера с его msku не будет на выдаче,
        вне завизимости от того, была ли у других офферов этого msku скидка."""
        self.chech_buybox_without_discount(colour='BLUE', user_id=1005)

    def test_buybox_offers_without_discount_filter_in_request_blue_market(self):
        """Проверяем, что на Синем маркете для плейса deals будет стоять
        фильтр filter-discount-only вне зависимоти от того, что передали."""
        for discount_filter in ['', '&filter-discount-only=1', '&filter-discount-only=0']:
            self.check_buybox_with_discount(
                colour='BLUE',
                user_id=1002,
                id=1234,
                category_id=101,
                wareId='BlueOffer-1-WithDisc-w',
                discount=10,
                discount_filter=discount_filter,
            )
            self.chech_buybox_without_discount(colour='BLUE', user_id=1003, discount_filter=discount_filter)

    def test_buybox_offer_without_discount_green_with_blue_market(self):
        """Проверяем, что на GREEN_WITH_BLUE маркете,
        если у байбоксового оффера не было скидки, а у какого-то другого оффера с таким же msku была,
        то скидочный оффер попадет в выдачу."""
        request = "place=deals&pp=18&numdoc=100&rgb=GREEN_WITH_BLUE&allow-collapsing=1&rids=213&how=discount_p&yandexuid=1003&filter-discount-only=1&hid=102"
        response = self.report.request_json(request)
        self.error_log.ignore("Personal category config is not available for user 1003")
        self.assertFragmentIn(
            response,
            {
                "search": {"total": 1, "results": [{"categories": [{"id": 102}], "id": 1235}]},
            },
        )

    def test_buybox_offer_with_discount_not_in_buybox_stat_blue_market(self):
        """Проверяем, что если в категорийной байбоксовой статистке у какой-то категории нет скидки,
        то она не попадет в выдачу на Синем,
        но если оффер попал в белую категорийную статистику, то на Белом он на выдаче будет."""
        self.chech_buybox_without_discount(colour='BLUE', user_id=1006)
        request = "place=deals&pp=18&numdoc=100&rgb=GREEN_WITH_BLUE&allow-collapsing=1&rids=213&how=discount_p&yandexuid=1006&filter-discount-only=1&show-personal=1"
        response = self.report.request_json(request)
        self.error_log.ignore("Personal category config is not available for user 1006")
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 1,
                    "results": [{"categories": [{"id": 105}], "id": 1238, 'prices': {'discount': {'percent': 10}}}],
                },
            },
        )

    @classmethod
    def prepare_category_ordering(cls):
        categories = list(range(901, 904))
        cls.index.hypertree += [HyperCategory(hid=hid, output_type=HyperCategoryType.GURU) for hid in categories]
        models = [hid * 10 + i for hid in categories for i in range(1, 3)]
        categories = [hyperid / 10 for hyperid in models]
        cls.index.models += [Model(hyperid=hyperid, hid=hid) for hyperid, hid in zip(models, categories)]
        discounts = [30 + i for i in range(len(models))]
        # green offers
        cls.index.shops += [
            Shop(fesh=2, priority_region=213),
        ]
        cls.index.offers += [
            Offer(fesh=2, hyperid=hyperid, price=100, price_old=100 + discount)
            for hyperid, discount in zip(models, discounts)
        ]
        # market skus
        cls.index.mskus += [
            MarketSku(
                hyperid=hyperid,
                sku=hyperid * 1000,
                blue_offers=[BlueOffer(vat=Vat.VAT_10, feedid=2, discount=discount)],
            )
            for hyperid, discount in zip(models, discounts)
        ]
        # personal history
        cls.recommender.on_request_models_of_interest(user_id='yandexuid:1009').respond(
            {'models': ['9021', '9032', '9011']}
        )

    def test_category_ordering(self):
        """Проверяем, что персональные скидки ранжируются с учётом порядка персональных категорий:
        сначала идёт первая модель первой категории, затем - первая модель второй категории, и т.д.,
        далее - вторые модели и т.д"""
        for rgb in ['blue', 'green']:
            response = self.report.request_json(
                'place=deals&rids=213&filter-discount-only=1&show-personal=1&yandexuid=1009&rgb={rgb}'.format(rgb=rgb)
            )
            self.assertFragmentIn(
                response,
                {
                    'search': {
                        'results': [
                            {'entity': 'product', 'categories': [{'id': 902}]},
                            {'entity': 'product', 'categories': [{'id': 903}]},
                            {'entity': 'product', 'categories': [{'id': 901}]},
                        ]
                        * 2
                    }
                },
                preserve_order=True,
            )

    @classmethod
    def prepare_duplication(cls):
        """Модели, имеющие по нескольку MarketSku из категорий, имеющих аксессуарные категории
        и из аксессуарных категорий"""
        cls.recommender.on_request_models_of_interest(user_id='yandexuid:222').respond(
            {'models': ['99993', '99991', '99992', '889', '99933']}
        )
        cls.index.hypertree += [
            HyperCategory(
                hid=9999,
                children=[
                    HyperCategory(hid=989024),
                    HyperCategory(hid=90799),  # coldstart category
                    HyperCategory(hid=13475238),
                    HyperCategory(hid=12345),
                ],
            ),
        ]

        cls.index.blue_category_region_stat += [
            CategoryStatsRecord(hid=hid, region=213, n_offers=3, n_discounts=3)
            for hid in [989024, 90799, 13475238, 12345]
        ]
        cls.index.models += [
            Model(hyperid=99993, hid=989024),
            Model(hyperid=99933, hid=989024),
            Model(hyperid=99991, hid=90799),
            Model(hyperid=99992, hid=13475238),
            Model(hyperid=888, hid=90799),
            Model(hyperid=887, hid=13475238),
            Model(hyperid=889, hid=12345),
        ]

        model_ids = [887, 888, 889, 99991, 99992, 99993] * 2
        prices = list(range(1000, 1000 + len(model_ids)))
        discounts = [20] * len(model_ids)
        cls.index.mskus += [
            MarketSku(
                ts=10000 + i,
                title='Blue offer {hyperid}'.format(hyperid=hyperid),
                hyperid=hyperid,
                sku=hyperid * 100 + i * 2 + ungrouped_model_blue,
                blue_offers=[
                    BlueOffer(ts=1000 + i * 2 + j, price=price, vat=Vat.VAT_10, feedid=2, discount=discount)
                    for j in range(2)
                ],
                ungrouped_model_blue=ungrouped_model_blue,
            )
            for hyperid, price, discount, i in zip(model_ids, prices, discounts, list(range(len(model_ids))))
            for ungrouped_model_blue in range(2)
        ]

    def test_duplication(self):
        """Проверяем, что нет дублей даже с включенным "расхлопыванием" моделей"""
        for rearrs in ['', '&rearr-factors=dsk_product_ungroup=old', '&rearr-factors=dsk_product_ungroup=new']:
            request = self.report.request_json(
                'place=deals&pp=18&numdoc=100&rgb=blue&rids=213&how=discount_p&yandexuid=222&filter-discount-only=1&show-personal=1&hid=9999{rearrs}'.format(
                    rearrs=rearrs
                )
            )
            self.assertFragmentIn(
                request,
                {
                    'search': {
                        'total': 6,
                        'results': [
                            {'entity': 'product', 'id': 887},
                            {'entity': 'product', 'id': 888},
                            {'entity': 'product', 'id': 889},
                            {'entity': 'product', 'id': 99991},
                            {'entity': 'product', 'id': 99992},
                            {'entity': 'product', 'id': 99993},
                        ],
                    }
                },
                preserve_order=False,
                allow_different_len=True,
            )


if __name__ == '__main__':
    main()

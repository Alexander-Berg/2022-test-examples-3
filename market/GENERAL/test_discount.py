#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    BlueOffer,
    Currency,
    GLParam,
    GLType,
    HyperCategory,
    HyperCategoryType,
    MarketSku,
    Model,
    ModelGroup,
    Offer,
    RtyOffer,
    Shop,
    Tax,
    UserSplit,
    Vat,
)
from core.testcase import TestCase, main
from core.matcher import Absent, Contains, NoKey


class T(TestCase):

    rty_offers_to_index = None

    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']

        cls.index.hypertree += [
            HyperCategory(hid=100, output_type=HyperCategoryType.GURU),
        ]

        cls.settings.rty_qpipe = True
        cls.index.shops += [
            Shop(fesh=200, priority_region=213),
            Shop(fesh=201, priority_region=777, regions=[213, 777]),
            Shop(fesh=777, datafeed_id=777, priority_region=213, supplier_type=Shop.THIRD_PARTY, blue=Shop.BLUE_REAL),
            Shop(
                fesh=1,
                datafeed_id=100,
                priority_region=213,
                name='virtual_shop',
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                fulfillment_virtual=True,
                cpa=Shop.CPA_REAL,
                virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
            ),
        ]

        cls.index.offers += [
            Offer(
                fesh=200, title='offer 1', price=100000, price_old=150000, price_history=None, feedid=1, offerid=1
            ),  # истории нет
            Offer(
                fesh=200,
                title='offer 2',
                price=100000,
                price_old=150000,
                price_history=120000,
                hyperid=100,
                feedid=1,
                offerid=2,
            ),
            Offer(
                fesh=201,
                title='offer 3',
                price=100000,
                price_old=150000,
                price_history=200000,
                hyperid=101,
                feedid=2,
                offerid=3,
            ),
        ]

        cls.index.models += [
            Model(hyperid=100, hid=100, title='model with offers 1'),
            Model(hyperid=101, hid=100, title='model with offers 2'),
        ]

        cls.rty_offers_to_index = [
            RtyOffer(feedid=1, offerid=1, price=100000, old_price=150000),
            RtyOffer(feedid=1, offerid=2, price=100000, old_price=150000),
            RtyOffer(feedid=2, offerid=3, price=100000, old_price=150000),
        ]

        cls.recommender.on_request_models_of_interest(user_id='yandexuid:1001').respond({'models': ['100', '101']})

    @classmethod
    def prepare_white_small_discount(cls):
        cls.index.offers += [
            Offer(fesh=200, waremd5='WhiteLessThat1RubDiscg', price=4, price_old=4.99),
            Offer(fesh=200, waremd5='WhiteMoreThat1RubDiscg', price=4, price_old=5.01),
        ]

    def test_white_small_discount(self):
        """
        проверяем, что если размер скидки не больше рубля, то она снимется на мете
        """
        self._test_no_discount('WhiteLessThat1RubDiscg', 'GREEN_WITH_BLUE')
        self._test_valid_discount('WhiteMoreThat1RubDiscg', 'GREEN_WITH_BLUE', 20)

    @classmethod
    def prepare_white_big_discount(cls):
        cls.index.offers += [
            Offer(fesh=200, waremd5='White99PercentDiscoung', price=1, price_old=100),
            Offer(fesh=200, waremd5='White95PercentDiscoung', price=5, price_old=100),
            Offer(fesh=200, waremd5='White90PercentDiscoung', price=10, price_old=100),
            Offer(fesh=200, waremd5='White75PercentDiscoung', price=25, price_old=100),
            Offer(fesh=200, waremd5='White70PercentDiscoung', price=30, price_old=100),
            Offer(fesh=200, waremd5='White50PercentDiscoung', price=50, price_old=100),
        ]

    def test_white_big_discount(self):
        """Проверяем что на белом маркете мы отсеиваем офера со слишким большой скидкой"""

        # по умлочанию верхняя граница - 95%
        # Скидки в размере >75% блокируются.
        self._test_no_discount('White99PercentDiscoung', 'GREEN_WITH_BLUE')
        self._test_no_discount('White95PercentDiscoung', 'GREEN_WITH_BLUE')
        self._test_no_discount('White90PercentDiscoung', 'GREEN_WITH_BLUE')
        self._test_valid_discount('White75PercentDiscoung', 'GREEN_WITH_BLUE', 75)
        self._test_valid_discount('White70PercentDiscoung', 'GREEN_WITH_BLUE', 70)
        self._test_valid_discount('White50PercentDiscoung', 'GREEN_WITH_BLUE', 50)

    def test_personal_discounts(self):
        # фильтруем скидки по региону
        response = self.report.request_json(
            "place=deals&pp=18&numdoc=100&rgb=green_with_blue&allow-collapsing=1&how=discount_p&show-personal=1&yandexuid=1001&use-default-offers=1&rids=213"
        )
        self.error_log.ignore("Personal category config is not available for user 1001")

        self.assertFragmentIn(
            response,
            {
                "search": {
                    "totalOffers": 0,
                    "totalModels": 2,
                    "results": [
                        {
                            "entity": "product",
                            "titles": {
                                "raw": "model with offers 1",
                            },
                        },
                        {
                            "entity": "product",
                            "titles": {
                                "raw": "model with offers 2",
                            },
                        },
                    ],
                }
            },
        )

        # фильтруем скидки по _домашнему_ региону
        response = self.report.request_json(
            "place=deals&pp=18&numdoc=100&rgb=green_with_blue&allow-collapsing=1&how=discount_p&show-personal=1&yandexuid=1001&use-default-offers=1&rids=213&min-delivery-priority=priority"
        )
        self.error_log.ignore("Personal category config is not available for user 1001")

        self.assertFragmentIn(
            response,
            {
                "search": {
                    "totalOffers": 0,
                    "totalModels": 1,
                    "results": [
                        {
                            "entity": "product",
                            "titles": {
                                "raw": "model with offers 1",
                            },
                        }
                    ],
                }
            },
        )

    @classmethod
    def prepare_discount_and_promodiscount_use_onstock_always(cls):
        # у нас есть гуру-категория и моделька, у которой 2 оффера
        # один со скидкой, второй без
        # запросы делаем с filter-discount-only=1&allow-collapsing=1&use-default-offers=1
        # запрашиваем прайм без гл-фитров по хиду - проверяем что модель есть, и в ней лежит ДО который со скидкой
        # запрашиваем прайм с гл-фильтрами для оффера без скидки - проверяем что выдача пустая
        cls.index.hypertree += [
            HyperCategory(hid=18796000, output_type=HyperCategoryType.GURU),
        ]

        cls.index.shops += [
            Shop(fesh=18796000, priority_region=213, regions=[213]),
        ]

        RED, GREEN = 1, 2
        cls.index.gltypes += [
            GLType(
                param_id=18796000,
                hid=18796000,
                gltype=GLType.ENUM,
                values=[
                    RED,
                    GREEN,
                ],
                unit_name="Color",
                cluster_filter=True,
            ),
        ]

        cls.index.models += [
            Model(hyperid=18796000, title='just_model', hid=18796000),
        ]

        cls.index.offers += [
            Offer(
                fesh=18796000,
                hyperid=18796000,
                title='18796000_with_discount',
                price=50,
                price_old=100,
                glparams=[GLParam(param_id=18796000, value=RED)],
            ),
            Offer(
                fesh=18796000,
                hyperid=18796000,
                title='18796000_without_discount',
                price=100,
                price_old=100,
                glparams=[GLParam(param_id=18796000, value=GREEN)],
            ),
        ]

    def test_discount_request_filter_models_without_offers(self):
        response = self.report.request_json(
            'place=prime&hid=18796000&filter-discount-only=1&allow-collapsing=1&use-default-offers=1'
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "product",
                        "offers": {
                            "count": 1,
                            "items": [
                                {
                                    "entity": "offer",
                                    "titles": {
                                        "raw": "18796000_with_discount",
                                    },
                                }
                            ],
                        },
                    }
                ]
            },
            allow_different_len=False,
        )

        response = self.report.request_json(
            'place=prime&hid=18796000&filter-discount-only=1&allow-collapsing=1&use-default-offers=1&glfilter=18796000:2'
        )
        self.assertFragmentIn(response, {"results": []}, allow_different_len=False)

        response = self.report.request_json(
            'place=prime&hid=18796000&filter-promo-or-discount=1&allow-collapsing=1&use-default-offers=1&glfilter=18796000:2'
        )
        self.assertFragmentIn(response, {"results": []}, allow_different_len=False)

    @classmethod
    def prepare_show_log_write_correct_discount(cls):
        # у нас есть 2 гуру-категории по 1 модельке в каждой, у каждой модельки по 1 офферу
        # у одной модельки оффер со скидкой, у второй без
        # проверям что в shows.log пишется корректный discount
        cls.index.hypertree += [
            HyperCategory(hid=18925000, output_type=HyperCategoryType.GURU),
            HyperCategory(hid=18925001, output_type=HyperCategoryType.GURU),
        ]

        cls.index.shops += [
            Shop(fesh=18925000, priority_region=213, regions=[213]),
        ]

        cls.index.models += [
            Model(hyperid=18925000, title='with_discount_model', hid=18925000),
            Model(hyperid=18925001, title='no_discount_model', hid=18925001),
        ]

        cls.index.offers += [
            Offer(fesh=18925000, hyperid=18925000, price=50, price_old=100),
            Offer(fesh=18925000, hyperid=18925001, price=50, price_history=25),
        ]

    def test_discount_in_showlog_correct(self):
        response = self.report.request_json('place=prime&hid=18925000&allow-collapsing=1&use-default-offers=1')
        self.assertFragmentIn(response, {"results": [{"entity": "product"}]}, allow_different_len=False)
        self.show_log.expect(discount=1, hyper_id=18925000)

        response = self.report.request_json('place=prime&hid=18925001&allow-collapsing=1&use-default-offers=1')
        self.assertFragmentIn(response, {"results": [{"entity": "product"}]}, allow_different_len=False)
        self.show_log.expect(discount=0, hyper_id=18925001)

    @classmethod
    def prepare_skip_group_model_offers_data(cls):
        # https://st.yandex-team.ru/MARKETOUT-19304
        # у нас есть 1 гуру-категория с 2 модельками, у каждой модельки по 1 офферу со скидкой
        # но одна моделька простая, а вторая - групповая
        # так вот при запросе без skip-group-model-offers или skip-group-model-offers=0  у нас на выходе должно быть 2 модели
        # а с запросом skip-group-model-offers=1 - только 1
        cls.index.hypertree += [
            HyperCategory(hid=19304000, output_type=HyperCategoryType.GURU),
        ]

        cls.index.shops += [
            Shop(fesh=19304000, priority_region=213, regions=[213]),
        ]

        cls.index.model_groups += [
            ModelGroup(hyperid=19304001, title='group_model', hid=19304000),
        ]

        cls.index.models += [
            Model(hyperid=19304000, title='simple_model_1', hid=19304000),
            Model(hyperid=19304002, title='simple_model_2', hid=19304000, group_hyperid=19304001),
        ]

        cls.index.offers += [
            Offer(fesh=19304000, hyperid=19304000, price=50, price_old=100, price_history=100),
            Offer(fesh=19304000, hyperid=19304002, price=50, price_old=90, price_history=100),
        ]

    def test_skip_group_model_offers(self):
        for param in ["", "skip-group-model-offers=0"]:
            response = self.report.request_json(
                'place=prime&hid=19304000&allow-collapsing=1&filter-promo-or-discount=1&use-default-offers=1&{}'.format(
                    param
                )
            )
            self.assertFragmentIn(
                response, {"results": [{"entity": "product"}, {"entity": "product"}]}, allow_different_len=False
            )

        response = self.report.request_json(
            'place=prime&hid=19304000&allow-collapsing=1&use-default-offers=1&filter-promo-or-discount=1&skip-group-model-offers=1'
        )
        self.assertFragmentIn(response, {"results": [{"entity": "product"}]}, allow_different_len=False)

    def _test_no_discount(self, wareid, color, additional_flags=''):
        params = 'place=offerinfo&rids=213&show-urls=&regset=1&pp=42&offerid={}&rgb={}{}'
        response = self.report.request_json(params.format(wareid, color, additional_flags))
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'totalOffers': 1,
                    'results': [
                        {
                            'entity': 'offer',
                            'wareId': wareid,
                            'prices': {
                                'discount': Absent(),
                            },
                        },
                    ],
                }
            },
        )

    def _test_discount_drop_reason(self, wareid, color, discount_skip_reason, additional_flags=''):
        params = 'place=offerinfo&rids=213&show-urls=&regset=1&pp=42&offerid={}&rgb={}&debug=1{}'
        response = self.report.request_json(params.format(wareid, color, additional_flags))
        self.assertFragmentIn(response, {"logicTrace": [Contains(discount_skip_reason)]})

    def _test_valid_discount(self, wareid, color, discount, additional_flags=''):
        params = 'place=offerinfo&rids=213&show-urls=&regset=1&pp=42&offerid={}&rgb={}{}'
        response = self.report.request_json(params.format(wareid, color, additional_flags))
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'totalOffers': 1,
                    'results': [
                        {
                            'entity': 'offer',
                            'wareId': wareid,
                            'prices': {
                                'discount': {
                                    'percent': discount,
                                }
                            },
                        },
                    ],
                }
            },
        )

    def _expect_price_and_discount(self, wareid, price, oldprice=None, discount=None):
        return {
            'search': {
                'totalOffers': 1,
                'results': [
                    {
                        'entity': 'offer',
                        'wareId': wareid,
                        'prices': {
                            'value': str(price),
                            'discount': {'percent': discount, 'oldMin': str(oldprice)}
                            if discount is not None
                            else NoKey('discount'),
                        },
                    },
                ],
            }
        }

    @classmethod
    def prepare_blue_1p_offer_discount(cls):
        cls.index.shops += [
            Shop(
                fesh=888,
                datafeed_id=888,
                priority_region=213,
                supplier_type=Shop.FIRST_PARTY,
                blue=Shop.BLUE_REAL,
                warehouse_id=145,
            ),
        ]
        blue_offers = [
            # history price < old price, no history
            BlueOffer(
                waremd5='Blue1POffer-1-NO-Dis-w',
                price=1000,
                price_old=1500,
                price_history=None,
                feedid=888,
                offerid='shop_sku_1',
            ),
            # history price < old price, old price > price_history
            BlueOffer(
                waremd5='Blue1POffer-2-NO-Dis-w',
                price=1000,
                price_old=1500,
                price_history=1200,
                feedid=888,
                offerid='shop_sku_2',
            ),
            # old price < history price
            BlueOffer(
                waremd5='Blue1POffer-3-IN-Dis-w',
                price=1000,
                price_old=1500,
                price_history=2000,
                feedid=888,
                offerid='shop_sku_3',
            ),
            # old price = price_history
            BlueOffer(
                waremd5='Blue1POffer-4-IN-Dis-w',
                price=1000,
                price_old=1500,
                price_history=1500,
                feedid=888,
                offerid='shop_sku_4',
            ),
            # no oldprice, but there is history price and dco price, history_price = max(history_price, dco_price)
            BlueOffer(
                waremd5='Blue1POffer-5-IN-Dis-w',
                price=1000,
                price_old=-1,
                price_history=1500,
                history_price_is_valid=True,
                price_reference=1200,
                feedid=888,
                offerid='shop_sku_5',
            ),
            # no oldprice, but there is history price and dco price, dco_price = max(history_price, dco_price)
            BlueOffer(
                waremd5='Blue1POffer-6-IN-Dis-w',
                price=1000,
                price_old=-1,
                price_history=1200,
                history_price_is_valid=True,
                price_reference=1500,
                feedid=888,
                offerid='shop_sku_6',
            ),
            # no oldprice, but there is dco price and no history_price
            BlueOffer(
                waremd5='Blue1POffer-7-IN-Dis-w',
                price=1000,
                price_old=-1,
                price_history=None,
                history_price_is_valid=True,
                price_reference=1500,
                feedid=888,
                offerid='shop_sku_7',
            ),
            # no oldprice, but there is dco price and no history_price
            BlueOffer(
                waremd5='Blue1POffer-8-IN-Dis-w',
                price=1000,
                price_old=-1,
                price_history=1500,
                history_price_is_valid=True,
                price_reference=None,
                feedid=888,
                offerid='shop_sku_8',
            ),
            # no oldprice, but there are dco price and history_price, but discount will not pass market checks
            BlueOffer(
                waremd5='Blue1POffer-9-NO-Dis-w',
                price=1000,
                price_old=-1,
                price_reference=1001,
                price_history=1001,
                history_price_is_valid=True,
                feedid=888,
                offerid='shop_sku_9',
            ),
            BlueOffer(
                waremd5='Blue1POffer-10_Adult_g',
                price=1000,
                price_old=1500,
                price_history=1200,
                feedid=888,
                offerid='shop_sku_10',
                adult=1,
            ),
            BlueOffer(
                waremd5='Blue1POffer-11-NO-DisQ',
                price=1000,
                price_old=-1,
                price_history=1200,
                history_price_is_valid=True,
                price_reference=1500,
                feedid=888,
                offerid='shop_sku_11',
                enable_auto_discounts=False,
            ),
            BlueOffer(
                waremd5='Blue1POffer-12-NO-DisQ',
                price=1000,
                price_old=1200,
                feedid=888,
                offerid='shop_sku_12',
                discount_restricted=True,
            ),
            BlueOffer(
                waremd5='Blue1POffer-13-NO-DisQ',
                price=100,
                price_old=-1,
                price_history=200,
                history_price_is_valid=True,
                feedid=888,
                offerid='shop_sku_13',
            ),
            BlueOffer(
                waremd5='Blue1POffer-14-NO-DisQ',
                price=100,
                price_old=-1,
                price_history=0,
                history_price_is_valid=True,
                feedid=888,
                offerid='shop_sku_14',
            ),
            BlueOffer(
                waremd5='Blue1P99Dggggggggggggg',
                price=1,
                price_old=100,
                price_history=100,
                history_price_is_valid=True,
                feedid=888,
            ),
            BlueOffer(
                waremd5='Blue1P95Dggggggggggggg',
                price=5,
                price_old=100,
                price_history=100,
                history_price_is_valid=True,
                feedid=888,
            ),
            BlueOffer(
                waremd5='Blue1P90Dggggggggggggg',
                price=10,
                price_old=100,
                price_history=100,
                history_price_is_valid=True,
                feedid=888,
            ),
            BlueOffer(
                waremd5='Blue1P75Dggggggggggggg',
                price=25,
                price_old=100,
                price_history=100,
                history_price_is_valid=True,
                feedid=888,
            ),
            BlueOffer(
                waremd5='Blue1P70Dggggggggggggg',
                price=30,
                price_old=100,
                price_history=100,
                history_price_is_valid=True,
                feedid=888,
            ),
        ]

        cls.index.mskus += [
            MarketSku(
                title='blue market sku1p',
                hyperid=112,
                sku=11200001,
                waremd5='MarketSku1-IiLVm1goleg',
                enable_auto_discounts=True,
                blue_offers=blue_offers,
            ),
        ]

        cls.index.models += [
            Model(hyperid=112, hid=112, title='blue model 1'),
        ]

    def test_blue_1p_offer_big_discount(self):
        """Проверяем что мы не показываем сллшком большие скидки у 1p оферов"""

        # по умолчанию порог - 90%
        wareid = 'Blue1P99Dggggggggggggg'
        # Скидки в размере >75% блокируются.
        self._test_no_discount(wareid, 'GREEN_WITH_BLUE')
        self._test_no_discount(wareid, 'BLUE')

        wareid = 'Blue1P95Dggggggggggggg'
        # Скидки в размере >75% блокируются.
        self._test_no_discount(wareid, 'GREEN_WITH_BLUE')
        self._test_no_discount(wareid, 'BLUE')

        wareid = 'Blue1P90Dggggggggggggg'
        # Скидки в размере >75% блокируются.
        self._test_no_discount(wareid, 'GREEN_WITH_BLUE')
        self._test_no_discount(wareid, 'BLUE')

        wareid = 'Blue1P75Dggggggggggggg'
        self._test_valid_discount(wareid, 'GREEN_WITH_BLUE', 75)
        self._test_valid_discount(wareid, 'BLUE', 75)

        wareid = 'Blue1P70Dggggggggggggg'
        self._test_valid_discount(wareid, 'GREEN_WITH_BLUE', 70)
        self._test_valid_discount(wareid, 'BLUE', 70)

    def test_blue_1p_offer_discount_with_no_history(self):
        """Tests that discount wll be shown for blue 1P offer with old price and without history price
        on GREEN_WITH_BLUE and BLUE markets, because there is no price history validation"""
        wareid = 'Blue1POffer-1-NO-Dis-w'
        self._test_valid_discount(wareid, 'GREEN_WITH_BLUE', 33)
        self._test_valid_discount(wareid, 'BLUE', 33)

    def test_blue_1p_offer_discount_with_invalid_oldprice(self):
        """Tests that discount wll be shown for blue 1P offer with old price and without history price
        on GREEN_WITH_BLUE and BLUE markets, because there is no price history validation"""
        wareid = 'Blue1POffer-2-NO-Dis-w'
        self._test_valid_discount(wareid, 'GREEN_WITH_BLUE', 33)
        self._test_valid_discount(wareid, 'BLUE', 33)

    def test_blue_1p_offer_discount_with_valid_oldprice(self):
        """Tests that discount will be shown for blue 1P offer with valid old price (<= history price).
        Case valid for BLUE and GREEN_WITH_BLUE markets."""
        wareid = 'Blue1POffer-3-IN-Dis-w'
        self._test_valid_discount(wareid, 'GREEN_WITH_BLUE', 33)
        self._test_valid_discount(wareid, 'BLUE', 33)

    def test_blue_1p_offer_discount_with_oldprice_equal_history(self):
        """Tests that discount will be shown for blue 1P offer with valid old price (= history price).
        Case valid for BLUE and GREEN_WITH_BLUE markets."""
        wareid = 'Blue1POffer-4-IN-Dis-w'
        self._test_valid_discount(wareid, 'GREEN_WITH_BLUE', 33)
        self._test_valid_discount(wareid, 'BLUE', 33)

    def test_blue_1p_offer_no_auto_discount_without_experiment_flag(self):
        """Tests that autodiscount without experiment flag is not set for 1p blue offers."""
        wareid = 'Blue1POffer-5-IN-Dis-w'
        self._test_no_discount(wareid, 'GREEN_WITH_BLUE')
        self._test_no_discount(wareid, 'BLUE')

        wareid = 'Blue1POffer-6-IN-Dis-w'
        self._test_no_discount(wareid, 'GREEN_WITH_BLUE')
        self._test_no_discount(wareid, 'BLUE')

        wareid = 'Blue1POffer-7-IN-Dis-w'
        self._test_no_discount(wareid, 'GREEN_WITH_BLUE')
        self._test_no_discount(wareid, 'BLUE')

    def test_blue_1p_offer_oldprice_with_restricted_discount(self):
        """For 1P-suppliers oldprice no discount will be applied and trace will contain information about discount
        restriction
        """
        wareid = 'Blue1POffer-12-NO-DisQ'
        for report in ['GREEN_WITH_BLUE', 'BLUE']:
            self._test_no_discount(
                wareid,
                report,
            )
            self._test_discount_drop_reason(
                wareid,
                report,
                discount_skip_reason='any discounts restricted for this offer',
            )

    @classmethod
    def prepare_blue_3p_offer_discount(cls):
        blue_offers = [
            # history price < old price, no history
            BlueOffer(waremd5='BlueOffer-1-NO-Disco-w', price=1000, price_old=1500, price_history=None, feedid=777),
            # history price < old price, old price > price_history
            BlueOffer(waremd5='BlueOffer-2-NO-Disco-w', price=1000, price_old=1500, price_history=1200, feedid=777),
            # history price > old price, old price < price_history
            BlueOffer(waremd5='BlueOffer-3-IN-Disco-w', price=1000, price_old=1500, price_history=2000, feedid=777),
            # history price > old price, old price = price_history
            BlueOffer(waremd5='BlueOffer-4-IN-Disco-w', price=1000, price_old=1500, price_history=1500, feedid=777),
            # old price > price_reference && old price < price_history
            BlueOffer(
                waremd5='BlueOffer-5-IN-Disco-w',
                price=1000,
                price_old=1500,
                price_history=2000,
                price_reference=1200,
                feedid=777,
            ),
            # old price < price_reference && old price > price_history
            BlueOffer(
                waremd5='BlueOffer-6-IN-Disco-w',
                price=1000,
                price_old=1500,
                price_history=1200,
                price_reference=2000,
                feedid=777,
            ),
            # old price < price_reference && old price < price_history
            BlueOffer(
                waremd5='BlueOffer-7-IN-Disco-w',
                price=1000,
                price_old=1500,
                price_history=1800,
                price_reference=2000,
                feedid=777,
            ),
            # old price < price_reference && no price_history
            BlueOffer(
                waremd5='BlueOffer-8-IN-Disco-w',
                price=1000,
                price_old=1500,
                price_history=None,
                price_reference=2000,
                feedid=777,
                offerid='shop_sku_8',
            ),
            # old price > price_reference && no price_history
            BlueOffer(
                waremd5='BlueOffer-9-IN-Disco-w',
                price=1000,
                price_old=1500,
                price_history=None,
                price_reference=1200,
                feedid=777,
            ),
            # no discount
            BlueOffer(waremd5='BlueOffer-10-NO-Disco-', price=1000, price_old=-1, feedid=777),
            # small discount
            BlueOffer(
                waremd5='BlueOffer-11-NO-Disco-',
                price=100000,
                price_old=100010,
                price_history=200000,
                feedid=777,
                offerid='shop_sku_11',
            ),
            # no oldprice, but there is history price and dco price, history_price = max(history_price, dco_price)
            BlueOffer(
                waremd5='Blue3POffer-12-IN-Di-w',
                price=1000,
                price_old=-1,
                price_history=1500,
                history_price_is_valid=True,
                price_reference=1200,
                feedid=777,
            ),
            # no oldprice, but there is history price and dco price, dco_price = max(history_price, dco_price)
            BlueOffer(
                waremd5='Blue3POffer-13-IN-Di-w',
                price=1000,
                price_old=-1,
                price_history=1200,
                history_price_is_valid=True,
                price_reference=1500,
                feedid=777,
            ),
            # no oldprice, but there is dco price and no history_price
            BlueOffer(
                waremd5='Blue3POffer-14-IN-Di-w',
                price=1000,
                price_old=-1,
                price_history=None,
                history_price_is_valid=True,
                price_reference=1500,
                feedid=777,
            ),
            # no oldprice, but there is dco price and no history_price
            BlueOffer(
                waremd5='Blue3POffer-15-IN-Di-w',
                price=1000,
                price_old=-1,
                price_history=1500,
                history_price_is_valid=True,
                price_reference=None,
                feedid=777,
            ),
            # no oldprice, but there are dco price and history_price, but discount will not pass market checks
            BlueOffer(
                waremd5='Blue3POffer-16-IN-Di-w',
                price=1000,
                price_old=-1,
                price_reference=1001,
                price_history=1001,
                history_price_is_valid=True,
                feedid=777,
            ),
            # autodiscount doesn't work if discount_restricted is set in offer base props
            BlueOffer(
                waremd5='Blue3POffer-17-IN-Di-w',
                price=1000,
                price_old=-1,
                price_history=1500,
                price_reference=1200,
                history_price_is_valid=True,
                feedid=777,
                discount_restricted=True,
            ),
            BlueOffer(
                waremd5='Blue3POffer-18-NO-DisQ',
                price=1000,
                price_old=-1,
                price_history=1200,
                history_price_is_valid=True,
                price_reference=1500,
                feedid=777,
                enable_auto_discounts=False,
            ),
            BlueOffer(
                waremd5='Blue3POffer-19-NO-DisQ',
                price=1000,
                price_old=1200,
                feedid=777,
                offerid='shop_sku_19',
                discount_restricted=True,
            ),
            BlueOffer(
                waremd5='Blue3POffer-20-NO-DisQ',
                price=100,
                price_old=-1,
                price_history=200,
                history_price_is_valid=True,
                feedid=777,
            ),
            BlueOffer(
                waremd5='Blue3POffer-21-NO-DisQ',
                price=100,
                price_old=-1,
                price_history=0,
                history_price_is_valid=True,
                feedid=777,
            ),
            BlueOffer(
                waremd5='Blue3POffer-22-NO-DisQ',
                price=100,
                price_old=200,
                price_history=200,
                history_price_is_valid=False,
                feedid=777,
            ),
            BlueOffer(
                waremd5='Blue3P99Dggggggggggggg',
                price=1,
                price_old=100,
                price_history=100,
                history_price_is_valid=True,
                feedid=777,
            ),
            BlueOffer(
                waremd5='Blue3P95Dggggggggggggg',
                price=5,
                price_old=100,
                price_history=100,
                history_price_is_valid=True,
                feedid=777,
            ),
            BlueOffer(
                waremd5='Blue3P90Dggggggggggggg',
                price=10,
                price_old=100,
                price_history=100,
                history_price_is_valid=True,
                feedid=777,
            ),
            BlueOffer(
                waremd5='Blue3P75Dggggggggggggg',
                price=25,
                price_old=100,
                price_history=100,
                history_price_is_valid=True,
                feedid=777,
            ),
            BlueOffer(
                waremd5='Blue3P70Dggggggggggggg',
                price=30,
                price_old=100,
                price_history=100,
                history_price_is_valid=True,
                feedid=777,
            ),
            BlueOffer(
                waremd5='Blue3P76Dggggggggggggg',
                price=24,
                price_old=100,
                feedid=777,
            ),
        ]

        blue_offers_fashion = [
            BlueOffer(
                waremd5='Blue3P76Dfashiongggggg',
                price=24,
                price_old=100,
                feedid=777,
            ),
        ]

        cls.index.mskus += [
            MarketSku(
                title='blue market sku1',
                hyperid=113,
                sku=1130001,
                waremd5='MarketSku2-IiLVm1goleg',
                blue_offers=blue_offers,
            ),
            MarketSku(title='Клатч луис вентон', hyperid=14335073, sku=666, blue_offers=blue_offers_fashion),
        ]

        cls.index.models += [
            Model(hyperid=113, hid=113, title='blue model 2'),
            Model(hyperid=14335073, hid=14335073, title='blue model clutches'),
        ]

        cls.index.hypertree += [
            HyperCategory(
                hid=7877999,
                name='Одежда, обувь и аксессуары',
                output_type=HyperCategoryType.GURU,
                children=[
                    HyperCategory(
                        hid=7812195,
                        name='Сумки и чемоданы',
                        output_type=HyperCategoryType.GURU,
                        children=[
                            HyperCategory(
                                hid=14335073,
                                name='Клатчи',
                                output_type=HyperCategoryType.GURU,
                            )
                        ],
                    ),
                ],
            )
        ]

    def test_blue_3p_offer_discount_with_invalid_history_price(self):
        """Проверяем что офер oldprice < history_price но c history_price_is_valid == False мы показываем и на белом, и на синем,
        так как для 3p-офферов проверки на невалидную историческую цену нет."""
        wareid = 'Blue3POffer-22-NO-DisQ'

        self._test_valid_discount(wareid, 'GREEN_WITH_BLUE', 50)
        self._test_valid_discount(wareid, 'BLUE', 50)

    def test_blue_3p_offer_discount_with_valid_oldprice(self):
        """Tests that discount will be shown for blue 3P offer with valid old price (<= history price).
        Case valid for BLUE and GREEN_WITH_BLUE markets."""
        wareid = 'BlueOffer-3-IN-Disco-w'
        self._test_valid_discount(wareid, 'GREEN_WITH_BLUE', 33)
        self._test_valid_discount(wareid, 'BLUE', 33)

        # с флагом market_disable_blue_3p_discount_profitability_check ничего не должно поменяться
        self._test_valid_discount(
            wareid, 'GREEN_WITH_BLUE', 33, '&rearr-factors=market_disable_blue_3p_discount_profitability_check=1'
        )
        self._test_valid_discount(
            wareid, 'BLUE', 33, '&rearr-factors=market_disable_blue_3p_discount_profitability_check=1'
        )

    def test_blue_3p_offer_discount_with_oldprice_equal_history(self):
        """Tests that discount will be shown for blue 3P offer with valid old price (= history price).
        Case valid for BLUE and GREEN_WITH_BLUE markets."""
        wareid = 'BlueOffer-4-IN-Disco-w'
        self._test_valid_discount(wareid, 'GREEN_WITH_BLUE', 33)
        self._test_valid_discount(wareid, 'BLUE', 33)

        # с флагом market_disable_blue_3p_discount_profitability_check ничего не должно поменяться
        self._test_valid_discount(
            wareid, 'GREEN_WITH_BLUE', 33, '&rearr-factors=market_disable_blue_3p_discount_profitability_check=1'
        )
        self._test_valid_discount(
            wareid, 'BLUE', 33, '&rearr-factors=market_disable_blue_3p_discount_profitability_check=1'
        )

    def test_blue_3p_invalid_dco_discount_with_valid_history(self):
        """3P цена больше референсной, но меньше исторической.
        Скидку показываем на Синем и Белом."""
        wareid = 'BlueOffer-5-IN-Disco-w'
        self._test_valid_discount(wareid, 'GREEN_WITH_BLUE', 33)
        self._test_valid_discount(wareid, 'BLUE', 33)

        # с флагом market_disable_blue_3p_discount_profitability_check ничего не должно поменяться
        self._test_valid_discount(
            wareid, 'GREEN_WITH_BLUE', 33, '&rearr-factors=market_disable_blue_3p_discount_profitability_check=1'
        )
        self._test_valid_discount(
            wareid, 'BLUE', 33, '&rearr-factors=market_disable_blue_3p_discount_profitability_check=1'
        )

    def test_blue_3p_dco_discount_with_valid_history(self):
        """3P цена меньше референсной и меньше исторической.
        Скидку показываем везде."""
        wareid = 'BlueOffer-7-IN-Disco-w'
        self._test_valid_discount(wareid, 'GREEN_WITH_BLUE', 33)
        self._test_valid_discount(wareid, 'BLUE', 33)

        # с флагом market_disable_blue_3p_discount_profitability_check ничего не должно поменяться
        self._test_valid_discount(
            wareid, 'GREEN_WITH_BLUE', 33, '&rearr-factors=market_disable_blue_3p_discount_profitability_check=1'
        )
        self._test_valid_discount(
            wareid, 'BLUE', 33, '&rearr-factors=market_disable_blue_3p_discount_profitability_check=1'
        )

    def test_offer_with_no_oldprice_correct_trace(self):
        """Проверяет, что для офферов без oldprice при debug=1 будет корректный трейс."""
        wareid = 'BlueOffer-10-NO-Disco-'
        self._test_discount_drop_reason(wareid, 'BLUE', 'old price was not set by shop')

    def test_blue_offer_with_small_invalid_discount_trace(self):
        """Проверяем, что при наличии debug=1 для офферов появится трейс о причинах отсутствия скидки."""
        wareid = 'BlueOffer-11-NO-Disco-'
        params = 'place=offerinfo&rids=213&show-urls=&regset=1&pp=42&offerid={}&rgb={}&debug=1'
        response = self.report.request_json(params.format(wareid, 'BLUE'))
        self.assertFragmentIn(
            response,
            {
                "logicTrace": [
                    Contains(
                        'old price is disabled by validation algorithm, discount should be <= 95%, >= 5% or (>= 500rub and >= 1% for blue)'
                    )
                ]
            },
        )

    def test_blue_3p_offer_no_auto_discount_without_experiment_flag(self):
        """Tests that autodiscount without experiment flag is not set for 3p blue offers."""
        wareid = 'Blue3POffer-12-IN-Di-w'
        self._test_no_discount(wareid, 'GREEN_WITH_BLUE')
        self._test_no_discount(wareid, 'BLUE')

        wareid = 'Blue3POffer-13-IN-Di-w'
        self._test_no_discount(wareid, 'GREEN_WITH_BLUE')
        self._test_no_discount(wareid, 'BLUE')

        wareid = 'Blue3POffer-14-IN-Di-w'
        self._test_no_discount(wareid, 'GREEN_WITH_BLUE')
        self._test_no_discount(wareid, 'BLUE')

    def test_blue_3p_offer_oldprice_with_restricted_discount(self):
        """For 3P-suppliers oldprice no discount will be applied and trace will contain information about discount
        restriction
        """
        wareid = 'Blue3POffer-19-NO-DisQ'
        for report in ['GREEN_WITH_BLUE', 'BLUE']:
            self._test_no_discount(
                wareid,
                report,
            )
            self._test_discount_drop_reason(
                wareid,
                report,
                discount_skip_reason='any discounts restricted for this offer',
            )

    def test_blue_3p_offer_big_discount(self):
        """Проверяем что мы не показываем сллшком большие скидки у 3p оферов"""

        # по умолчанию порог - 90%
        wareid = 'Blue3P99Dggggggggggggg'
        self._test_no_discount(wareid, 'GREEN_WITH_BLUE')
        self._test_no_discount(wareid, 'BLUE')

        wareid = 'Blue3P95Dggggggggggggg'
        self._test_no_discount(wareid, 'GREEN_WITH_BLUE')
        self._test_no_discount(wareid, 'BLUE')

        wareid = 'Blue3P90Dggggggggggggg'
        self._test_no_discount(wareid, 'GREEN_WITH_BLUE')
        self._test_no_discount(wareid, 'BLUE')

        # для "не фэшн" оффера скидка 76 не срабатывает
        wareid = 'Blue3P76Dggggggggggggg'
        self._test_no_discount(wareid, 'GREEN_WITH_BLUE')
        self._test_no_discount(wareid, 'BLUE')

        # для "фэшн" оффера скидка 76 срабатывает
        wareid = 'Blue3P76Dfashiongggggg'
        self._test_valid_discount(wareid, 'GREEN_WITH_BLUE', 76)
        self._test_valid_discount(wareid, 'BLUE', 76)

        wareid = 'Blue3P75Dggggggggggggg'
        self._test_valid_discount(wareid, 'GREEN_WITH_BLUE', 75)
        self._test_valid_discount(wareid, 'BLUE', 75)

        wareid = 'Blue3P70Dggggggggggggg'
        self._test_valid_discount(wareid, 'GREEN_WITH_BLUE', 70)
        self._test_valid_discount(wareid, 'BLUE', 70)

    def test_market_hide_discount(self):
        """Tests that discount will be hidden for blue 1P offer with valid old price
        under flag market_hide_discount_on_basesearch or market_hide_discount_on_output"""
        wareid1 = 'Blue1POffer-4-IN-Dis-w'
        wareid2 = 'BlueOffer-6-IN-Disco-w'

        for flag in ['market_hide_discount_on_basesearch', 'market_hide_discount_on_output']:

            self._test_valid_discount(wareid1, 'BLUE', 33)
            # market_hide_discount_on_basesearch=1.0 скрывает 100% скидок
            rearr = '&rearr-factors={}=1.0'.format(flag)
            self._test_no_discount(wareid1, 'BLUE', rearr)
            self._test_no_discount(wareid2, 'BLUE', rearr)

            # одна и та же скушка в разные дни может давать скидку а в другой день не давать
            # разные скушки в один и тот же день могут давать или не давать скидку
            rearr = '&rearr-factors={}=0.5;'.format(flag)
            self._test_no_discount(wareid1, 'BLUE', rearr + 'market_promo_datetime=20200413T000000')
            self._test_valid_discount(wareid1, 'BLUE', 33, rearr + 'market_promo_datetime=20200414T000000')
            self._test_no_discount(wareid1, 'BLUE', rearr + 'market_promo_datetime=20200415T000000')
            self._test_valid_discount(wareid1, 'BLUE', 33, rearr + 'market_promo_datetime=20200418T000000')
            self._test_valid_discount(wareid1, 'BLUE', 33, rearr + 'market_promo_datetime=20200423T000000')

            self._test_no_discount(wareid2, 'BLUE', rearr + 'market_promo_datetime=20200414T000000')
            self._test_no_discount(wareid2, 'BLUE', rearr + 'market_promo_datetime=20200415T000000')
            self._test_no_discount(wareid2, 'BLUE', rearr + 'market_promo_datetime=20200418T000000')
            self._test_no_discount(wareid2, 'BLUE', rearr + 'market_promo_datetime=20200423T000000')

    @classmethod
    def prepare_change_price(cls):

        cls.index.hypertree += [
            HyperCategory(
                hid=101, children=[HyperCategory(hid=102, children=[HyperCategory(hid=103)]), HyperCategory(hid=104)]
            )
        ]

        cls.index.mskus += [
            MarketSku(
                title='Big discount 103',
                sku=1031,
                hyperid=1031,
                blue_offers=[BlueOffer(waremd5='Big-Discount-103-xxxxw', price=1000, price_old=2000, feedid=888)],
            ),
            MarketSku(
                title='Low discount 103',
                sku=1032,
                hyperid=1032,
                blue_offers=[BlueOffer(waremd5='Low-Discount-103-xxxxw', price=1000, price_old=1250, feedid=888)],
            ),
            MarketSku(
                title='No discount 103',
                sku=1033,
                hyperid=1033,
                blue_offers=[BlueOffer(waremd5='No-Discount-103-xxxxxw', price=1000, feedid=888)],
            ),
            MarketSku(
                title='Big discount 104',
                sku=1041,
                hyperid=1041,
                blue_offers=[BlueOffer(waremd5='Big-Discount-104-xxxxw', price=1000, price_old=2000, feedid=888)],
            ),
            MarketSku(
                title='Low discount 104',
                sku=1042,
                hyperid=1042,
                blue_offers=[BlueOffer(waremd5='Low-Discount-104-xxxxw', price=1000, price_old=1250, feedid=888)],
            ),
            MarketSku(
                title='No discount 104',
                sku=1043,
                hyperid=1043,
                blue_offers=[BlueOffer(waremd5='No-Discount-104-xxxxxw', price=1000, feedid=888)],
            ),
        ]

        cls.index.models += [
            Model(hyperid=1031, hid=103),
            Model(hyperid=1032, hid=103),
            Model(hyperid=1033, hid=103),
            Model(hyperid=1041, hid=104),
            Model(hyperid=1042, hid=104),
            Model(hyperid=1043, hid=104),
        ]

        cls.index.user_split += [
            # сплит "значение метки пользователя = 1"
            UserSplit(uuid=12345, value=0),
            UserSplit(yandexuid=1, value=0),
            # сплит "значение метки пользователя = 2"
            UserSplit(yandexuid=2, value=1),
            UserSplit(uuid=67890, value=1),
        ]

    def test_market_hide_discount_grow_price(self):
        """ " значение метки пользователя = 1:
        повышать saleprice до oldprice, если oldprice превышает saleprice не более, чем на x%
        если oldprice превышает x%, то повышать saleprice до (1+x)*saleprice. В любом случае oldprice убирается.
        """

        # пользователь с yandexuid=2 или uuid=67890 попал в сплит с меткой 2 (понижение oldprice)
        # поэтому для него в офферах ничего не меняется
        # т.к. выставлен только флаг market_hide_discount_on_basesearch_grow_price=1.0
        for user in ['yandexuid=2', 'uuid=67890']:
            query = (
                'place=offerinfo&debug=da&offerid={offerid}&rids=213&regset=2&{user}&debug=da'
                '&rearr-factors=market_hide_discount_on_basesearch_grow_price=1.0'
            )
            for offerid, expect in [
                (
                    'Big-Discount-104-xxxxw',
                    self._expect_price_and_discount('Big-Discount-104-xxxxw', price=1000, oldprice=2000, discount=50),
                ),
                (
                    'Low-Discount-104-xxxxw',
                    self._expect_price_and_discount('Low-Discount-104-xxxxw', price=1000, oldprice=1250, discount=20),
                ),
                ('No-Discount-104-xxxxxw', self._expect_price_and_discount('No-Discount-104-xxxxxw', price=1000)),
                (
                    'Big-Discount-103-xxxxw',
                    self._expect_price_and_discount('Big-Discount-103-xxxxw', price=1000, oldprice=2000, discount=50),
                ),
                (
                    'Low-Discount-103-xxxxw',
                    self._expect_price_and_discount('Low-Discount-103-xxxxw', price=1000, oldprice=1250, discount=20),
                ),
                ('No-Discount-103-xxxxxw', self._expect_price_and_discount('No-Discount-103-xxxxxw', price=1000)),
            ]:
                response = self.report.request_json(query.format(user=user, offerid=offerid))
                self.assertFragmentIn(response, {"how": [{"args": Contains("change_price_split: 2")}]})
                self.assertFragmentIn(response, expect)

        # пользователь с yandexuid=1 или uuid=12345 попал в сплит с меткой 1 (повышение price) поэтому
        # в категории 104 с ограничением в 30% цена поднимается до oldprice или на 30%
        # в категории 103 c ограничением в 150% цена поднимается до oldprice
        # скидка везде убирается
        for user in ['yandexuid=1', 'uuid=12345']:
            query = (
                'place=offerinfo&debug=da&offerid={offerid}&rids=213&regset=2&{user}&debug=da'
                '&rearr-factors=market_hide_discount_on_basesearch_grow_price=1.0'
            )
            for offerid, expect in [
                ('Big-Discount-104-xxxxw', self._expect_price_and_discount('Big-Discount-104-xxxxw', price=1300)),
                ('Low-Discount-104-xxxxw', self._expect_price_and_discount('Low-Discount-104-xxxxw', price=1250)),
                ('No-Discount-104-xxxxxw', self._expect_price_and_discount('No-Discount-104-xxxxxw', price=1000)),
                ('Big-Discount-103-xxxxw', self._expect_price_and_discount('Big-Discount-103-xxxxw', price=2000)),
                ('Low-Discount-103-xxxxw', self._expect_price_and_discount('Low-Discount-103-xxxxw', price=1250)),
                ('No-Discount-103-xxxxxw', self._expect_price_and_discount('No-Discount-103-xxxxxw', price=1000)),
            ]:
                response = self.report.request_json(query.format(user=user, offerid=offerid))
                self.assertFragmentIn(response, {"how": [{"args": Contains("change_price_split: 1")}]})
                self.assertFragmentIn(response, expect)

    def test_market_hide_discount_down_old_price(self):
        """значение метки пользователя 2:
        понижать oldprice до (1+x)*saleprice, если oldprice превышает saleprice более, чем на x%.
        В противном случае ничего не делать.
        """

        # пользователь с yandexuid=2 или uuid=67890 попал в сплит с меткой 2 (понижение oldprice)
        # в категории 104 с ограничением в 30% oldprice опускается до saleprice+30%
        # в категории 103 c ограничением в 150% oldprice не меняется
        for user in ['yandexuid=2', 'uuid=67890']:
            query = (
                'place=offerinfo&debug=da&offerid={offerid}&rids=213&regset=2&{user}&debug=da'
                '&rearr-factors=market_hide_discount_on_basesearch_down_old_price=1.0'
            )
            for offerid, expect in [
                (
                    'Big-Discount-104-xxxxw',
                    self._expect_price_and_discount('Big-Discount-104-xxxxw', price=1000, oldprice=1300, discount=23),
                ),
                (
                    'Low-Discount-104-xxxxw',
                    self._expect_price_and_discount('Low-Discount-104-xxxxw', price=1000, oldprice=1250, discount=20),
                ),
                ('No-Discount-104-xxxxxw', self._expect_price_and_discount('No-Discount-104-xxxxxw', price=1000)),
                (
                    'Big-Discount-103-xxxxw',
                    self._expect_price_and_discount('Big-Discount-103-xxxxw', price=1000, oldprice=2000, discount=50),
                ),
                (
                    'Low-Discount-103-xxxxw',
                    self._expect_price_and_discount('Low-Discount-103-xxxxw', price=1000, oldprice=1250, discount=20),
                ),
                ('No-Discount-103-xxxxxw', self._expect_price_and_discount('No-Discount-103-xxxxxw', price=1000)),
            ]:
                response = self.report.request_json(query.format(user=user, offerid=offerid))
                self.assertFragmentIn(response, {"how": [{"args": Contains("change_price_split: 2")}]})
                self.assertFragmentIn(response, expect)

        # пользователь с yandexuid=1 или uuid=12345 попал в сплит с меткой 1 (повышение price)
        # поэтому для него в офферах ничего не меняется
        # т.к. выставлен только флаг market_hide_discount_on_basesearch_down_old_price=1.0
        for user in ['yandexuid=1', 'uuid=12345']:
            query = (
                'place=offerinfo&debug=da&offerid={offerid}&rids=213&regset=2&{user}&debug=da'
                '&rearr-factors=market_hide_discount_on_basesearch_down_old_price=1.0'
            )
            for offerid, expect in [
                (
                    'Big-Discount-104-xxxxw',
                    self._expect_price_and_discount('Big-Discount-104-xxxxw', price=1000, oldprice=2000, discount=50),
                ),
                (
                    'Low-Discount-104-xxxxw',
                    self._expect_price_and_discount('Low-Discount-104-xxxxw', price=1000, oldprice=1250, discount=20),
                ),
                ('No-Discount-104-xxxxxw', self._expect_price_and_discount('No-Discount-104-xxxxxw', price=1000)),
                (
                    'Big-Discount-103-xxxxw',
                    self._expect_price_and_discount('Big-Discount-103-xxxxw', price=1000, oldprice=2000, discount=50),
                ),
                (
                    'Low-Discount-103-xxxxw',
                    self._expect_price_and_discount('Low-Discount-103-xxxxw', price=1000, oldprice=1250, discount=20),
                ),
                ('No-Discount-103-xxxxxw', self._expect_price_and_discount('No-Discount-103-xxxxxw', price=1000)),
            ]:
                response = self.report.request_json(query.format(user=user, offerid=offerid))
                self.assertFragmentIn(response, {"how": [{"args": Contains("change_price_split: 1")}]})
                self.assertFragmentIn(response, expect)

    def test_no_change_price_for_not_splitted_user(self):
        """значение метки пользователя 0:
        ничего не делать
        """
        for user in ['yandexuid=3', 'uuid=13579']:
            query = (
                'place=offerinfo&debug=da&offerid={offerid}&rids=213&regset=2&{user}&debug=da'
                '&rearr-factors=market_hide_discount_on_basesearch_grow_price=1.0'
            )
            for offerid, expect in [
                (
                    'Big-Discount-104-xxxxw',
                    self._expect_price_and_discount('Big-Discount-104-xxxxw', price=1000, oldprice=2000, discount=50),
                ),
                (
                    'Low-Discount-104-xxxxw',
                    self._expect_price_and_discount('Low-Discount-104-xxxxw', price=1000, oldprice=1250, discount=20),
                ),
                ('No-Discount-104-xxxxxw', self._expect_price_and_discount('No-Discount-104-xxxxxw', price=1000)),
                (
                    'Big-Discount-103-xxxxw',
                    self._expect_price_and_discount('Big-Discount-103-xxxxw', price=1000, oldprice=2000, discount=50),
                ),
                (
                    'Low-Discount-103-xxxxw',
                    self._expect_price_and_discount('Low-Discount-103-xxxxw', price=1000, oldprice=1250, discount=20),
                ),
                ('No-Discount-103-xxxxxw', self._expect_price_and_discount('No-Discount-103-xxxxxw', price=1000)),
            ]:
                response = self.report.request_json(query.format(user=user, offerid=offerid))
                self.assertFragmentIn(response, {"how": [{"args": Contains("change_price_split: 0")}]})
                self.assertFragmentIn(response, expect)

    @classmethod
    def prepare_discount_for_alcohol(cls):
        cls.index.hypertree += [
            HyperCategory(
                hid=16155381,
                name='Алкоголь',
                output_type=HyperCategoryType.GURU,
                children=[
                    HyperCategory(hid=16155466, name='Вино', output_type=HyperCategoryType.GURU),
                    HyperCategory(hid=16155647, name='Виски, бурбон', output_type=HyperCategoryType.GURU),
                    HyperCategory(hid=16155455, name='Водка', output_type=HyperCategoryType.GURU),
                    HyperCategory(hid=16155448, name='Коньяк, арманьяк, бренди', output_type=HyperCategoryType.GURU),
                    HyperCategory(hid=16155587, name='Крепкий алкоголь', output_type=HyperCategoryType.GURU),
                    HyperCategory(hid=16155526, name='Креплёное вино', output_type=HyperCategoryType.GURU),
                    HyperCategory(hid=16155651, name='Ликёры, настойки, аперитивы', output_type=HyperCategoryType.GURU),
                    HyperCategory(hid=16155476, name='Пиво и пивные напитки', output_type=HyperCategoryType.GURU),
                    HyperCategory(hid=16155504, name='Слабоалкогольные напитки', output_type=HyperCategoryType.GURU),
                    HyperCategory(hid=16155560, name='Шампанское и игристое вино', output_type=HyperCategoryType.GURU),
                ],
            )
        ]

    @classmethod
    def prepare_discount_one_run(cls):
        cls.index.hypertree += [
            HyperCategory(hid=27568000, name="Дешманская категория", output_type=HyperCategoryType.GURU)
        ]

        cls.index.offers += [
            Offer(title="19rub_offer", hid=27568000, price=19, price_old=20, price_history=20),
            Offer(title="20rub_offer", hid=27568000, price=22, price_old=23, price_history=23),
        ]

    def test_discount_one_run(self):
        response = self.report.request_json("place=prime&hid=27568000")
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'totalOffers': 2,
                    'results': [
                        {
                            'entity': 'offer',
                            'titles': {'raw': '19rub_offer'},
                            "prices": {
                                "value": "19",
                                "discount": {
                                    "oldMin": "20",
                                    "percent": 5,
                                },
                            },
                        },
                        {
                            'entity': 'offer',
                            'titles': {'raw': '20rub_offer'},
                            "prices": {
                                "value": "22",
                                "discount": NoKey("discount"),
                            },
                        },
                    ],
                }
            },
        )

    @classmethod
    def prepare_blue_absolute_discount(cls):
        cls.index.mskus += [
            MarketSku(
                title="less 200 rubles",
                hyperid=1100,
                sku=1,
                blue_offers=[
                    BlueOffer(
                        price=199,
                        price_old=250,
                        vat=Vat.VAT_10,
                        feedid=3,
                        offerid='PriceLess200',
                    )
                ],
            ),
            MarketSku(
                title="upper 200 rubles",
                hyperid=1100,
                sku=2,
                blue_offers=[
                    BlueOffer(
                        price=200,
                        price_old=250,
                        vat=Vat.VAT_10,
                        feedid=3,
                        offerid='PriceUpper200',
                    )
                ],
            ),
        ]

    def test_blue_absolute_discount(self):
        '''
        Проверяем выдачу абсолютной скидки в рублях
        '''

        def sample_answer():
            return [
                {
                    "entity": "offer",
                    "supplierSku": "PriceLess200",
                    "prices": {"value": "199", "discount": {"oldMin": "250", "absolute": "51"}},
                },
                {
                    "entity": "offer",
                    "supplierSku": "PriceUpper200",
                    "prices": {"value": "200", "discount": {"oldMin": "250", "absolute": "50"}},
                },
            ]

        request = "place=offerinfo&rids=0&regset=1&show-urls=cpa&market-sku=1,2"

        # Абсолютная скидка показывается на белом и синем маркете для любой цены (не важно больше или меньше 200)
        for additional in ['', '&rgb=blue', '&rgb=green']:
            response = self.report.request_json(request + additional)
            self.assertFragmentIn(response, sample_answer())

    @classmethod
    def prepare_dsbs_offer_discount(cls):
        cls.index.shops += [
            # DSBS магазин
            Shop(fesh=1001, priority_region=213, cpa=Shop.CPA_REAL, cpc=Shop.CPC_NO),
        ]

        cls.index.hypertree += [
            HyperCategory(hid=9876),
        ]

        cls.index.models += [
            Model(hyperid=9001, hid=9876),
        ]

        cls.index.mskus += [
            MarketSku(hyperid=9001, sku=90010),
        ]

        cls.index.offers += [
            Offer(
                waremd5='DsbsOffer-1----------w',
                price=1000,
                price_old=1500,
                price_history=None,
                cpa=Offer.CPA_REAL,
                hyperid=9001,
                fesh=1001,
                sku=90010,
            ),
            Offer(
                waremd5='DsbsOffer-2----------w',
                price=1000,
                price_old=1500,
                price_history=1500,
                cpa=Offer.CPA_REAL,
                hyperid=9001,
                fesh=1001,
                sku=90010,
            ),
            Offer(
                waremd5='DsbsOffer-3----------w',
                price=200,
                price_old=1000,
                price_history=1000,
                cpa=Offer.CPA_REAL,
                hyperid=9001,
                fesh=1001,
                sku=90010,
            ),
            Offer(
                waremd5='DsbsOffer-4----------w',
                price=96000,
                price_old=100000,
                price_history=100000,
                cpa=Offer.CPA_REAL,
                hyperid=9001,
                fesh=1001,
                sku=90010,
            ),
        ]

    def test_dsbs_offer_discount_with_invalid_history_price(self):
        """Проверяем что офер oldprice < history_price но c history_price_is_valid == False мы показываем и на белом, и на синем,
        так как для dsbs-офферов проверки на невалидную историческую цену нет."""
        wareid = 'DsbsOffer-2----------w'
        # Показываем скидку на белом и синем маркетах для любых значений флага
        self._test_valid_discount(wareid, 'GREEN_WITH_BLUE', 33)
        self._test_valid_discount(wareid, 'BLUE', 33)
        self._test_valid_discount(wareid, 'GREEN_WITH_BLUE', 33, '&rearr-factors=market_enable_dsbs_discount_check=0')
        self._test_valid_discount(wareid, 'BLUE', 33, '&rearr-factors=market_enable_dsbs_discount_check=0')
        self._test_valid_discount(wareid, 'GREEN_WITH_BLUE', 33, '&rearr-factors=market_enable_dsbs_discount_check=1')
        self._test_valid_discount(wareid, 'BLUE', 33, '&rearr-factors=market_enable_dsbs_discount_check=1')

    def test_dsbs_offer_high_discount(self):
        """Проверяем что скидкa > 75% блокируется"""
        wareid = 'DsbsOffer-3----------w'
        self._test_no_discount(wareid, 'GREEN_WITH_BLUE')
        self._test_no_discount(wareid, 'BLUE')
        self._test_no_discount(wareid, 'GREEN_WITH_BLUE', '&rearr-factors=market_enable_dsbs_discount_check=0')
        self._test_no_discount(wareid, 'BLUE', '&rearr-factors=market_enable_dsbs_discount_check=0')
        self._test_no_discount(wareid, 'GREEN_WITH_BLUE', '&rearr-factors=market_enable_dsbs_discount_check=1')
        self._test_no_discount(wareid, 'BLUE', '&rearr-factors=market_enable_dsbs_discount_check=1')

    def test_dsbs_offer_low_discount(self):
        """Проверяем что скидкa < 5% блокируется без флага market_enable_dsbs_discount_check.
        Если флаг market_enable_dsbs_discount_check=1, то валидной считается скидка >= 5% или  (>= 1% и >= 500 руб)
        """
        wareid = 'DsbsOffer-4----------w'
        self._test_no_discount(wareid, 'GREEN_WITH_BLUE')
        self._test_no_discount(wareid, 'BLUE')
        self._test_no_discount(wareid, 'GREEN_WITH_BLUE', '&rearr-factors=market_enable_dsbs_discount_check=0')
        self._test_no_discount(wareid, 'BLUE', '&rearr-factors=market_enable_dsbs_discount_check=0')
        self._test_valid_discount(wareid, 'GREEN_WITH_BLUE', 4, '&rearr-factors=market_enable_dsbs_discount_check=1')
        self._test_valid_discount(wareid, 'BLUE', 4, '&rearr-factors=market_enable_dsbs_discount_check=1')


if __name__ == '__main__':
    main()

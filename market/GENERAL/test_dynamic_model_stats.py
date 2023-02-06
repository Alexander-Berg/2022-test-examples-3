#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa

from core.testcase import TestCase, main
from core.types import (
    DeliveryBucket,
    DeliveryOption,
    DynamicShop,
    GLParam,
    GLType,
    HyperCategory,
    HyperCategoryType,
    MnPlace,
    Model,
    ModelGroup,
    NewShopRating,
    Offer,
    Outlet,
    PickupBucket,
    PickupOption,
    RegionalDelivery,
    RegionalModel,
    Shop,
)
from core.matcher import NoKey, NotEmpty


class T(TestCase):
    @classmethod
    def prepare_single_model(cls):
        cls.settings.default_search_experiment_flags += ['market_new_cpm_iterator=0']
        cls.settings.default_search_experiment_flags += ["market_money_disable_bids=0"]
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']
        cls.settings.enable_panther = False
        cls.index.models += [Model(hid=1, title='super model', hyperid=100, ts=100500)]
        for seq in range(1, 20):
            cls.index.shops += [Shop(fesh=100 + seq, priority_region=213)]
            cls.index.offers += [
                # each offer has discount
                Offer(title='super offer', fesh=100 + seq, price=1000 + 50 * seq, hyperid=100)
            ]

    def test_filtration_with_entities_product(self):
        expect = {
            "entity": "product",
            "offers": {"count": 19},
            "prices": {"min": "1050", "max": "1950", "currency": "RUR", "avg": "1500"},
        }
        response = self.report.request_json('place=prime&rids=213&text=super+model&entities=product')
        self.assertFragmentIn(response, expect)
        response = self.report.request_json('place=prime&rids=213&hid=1&entities=product')
        self.assertFragmentIn(response, expect)
        response = self.report.request_json('place=modelinfo&rids=213&hyperid=100&entities=product')
        self.assertFragmentIn(response, expect)

    def test_statistics_upadate_simple_model(self):
        """
        Check simple model realtime statistics calculation for prime and modelinfo:
        1) Check initial price range values for model
        2) Disable shops with minimul and maximum prices and check price range values for model
        """
        for additional_params in ['', '&use-default-offers=1']:
            expect = {
                "entity": "product",
                "offers": {"count": 19},
                "prices": {"min": "1050", "max": "1950", "currency": "RUR", "avg": "1500"},
            }

            response = self.report.request_json('place=prime&rids=213&text=super+model' + additional_params)
            self.assertFragmentIn(response, expect)

            response = self.report.request_json('place=modelinfo&rids=213&hyperid=100' + additional_params)
            self.assertFragmentIn(response, expect)
            self.dynamic.market_dynamic.disabled_cpc_shops += [DynamicShop(101), DynamicShop(119)]

            expect = {
                "entity": "product",
                "offers": {"count": 17},
                "prices": {"min": "1100", "max": "1900", "currency": "RUR", "avg": "1500"},
            }
            response = self.report.request_json('place=prime&rids=213&text=super+model' + additional_params)
            self.assertFragmentIn(response, expect)
            response = self.report.request_json('place=modelinfo&rids=213&hyperid=100' + additional_params)
            self.assertFragmentIn(response, expect)
            self.dynamic.market_dynamic.disabled_cpc_shops.clear()

    @classmethod
    def prepare_group_model(cls):
        cls.index.model_groups += [ModelGroup(title='group model', hyperid=500)]
        for seq in range(1, 50):
            fesh = hyperid = 500 + seq
            cls.index.models += [Model(title='modification model', hyperid=hyperid, group_hyperid=500)]
            cls.index.shops += [Shop(fesh=fesh, priority_region=213)]
            cls.index.offers += [Offer(title='modification offer', fesh=fesh, price=1000 + 50 * seq, hyperid=hyperid)]

    def test_statistics_update_group_model(self):
        """
        Check group models realtime statistics calculation for prime and modelinfo:
        1) Check initial price range values for group model
        2) Disable shops with minimul and maximum prices of a modification and check price range values for group model
        test with and without default offer mode
        """

        for additionalParams in ['', '&use-default-offers=1']:
            expect_initial = {
                "entity": "product",
                "titles": {"raw": "group model"},
                "offers": {"count": 49},
                "prices": {"min": "1050", "max": "3450", "currency": "RUR"},
            }

            response = self.report.request_json('place=prime&rids=213&text=group+model&numdoc=100' + additionalParams)
            self.assertFragmentIn(response, expect_initial)
            response = self.report.request_json('place=modelinfo&rids=213&hyperid=500' + additionalParams)
            self.assertFragmentIn(response, expect_initial)

            self.dynamic.market_dynamic.disabled_cpc_shops += [
                DynamicShop(501),
                DynamicShop(549),
            ]
            expect_updated = {
                "entity": "product",
                "titles": {"raw": "group model"},
                "offers": {"count": 47},
                "prices": {"min": "1100", "max": "3400", "currency": "RUR"},
            }
            response = self.report.request_json('place=prime&rids=213&text=group+model&numdoc=100' + additionalParams)
            self.assertFragmentIn(response, expect_updated)

            response = self.report.request_json('place=modelinfo&rids=213&hyperid=500' + additionalParams)
            self.assertFragmentIn(response, expect_updated)

            self.dynamic.market_dynamic.disabled_cpc_shops.clear()

    def test_model_modifications(self):
        """Проверяем как работают динамические статистики на вкладке модификации на КМ"""

        # находится 49 модификаций моделей
        response = self.report.request_json('place=model_modifications&hyperid=500&rids=213')
        self.assertFragmentIn(response, {'search': {'total': 49}})

        # отключим все магазины кроме четырех наиболее дорогих (с ценой от 1000 + 50*45 = 3250)
        self.dynamic.market_dynamic.disabled_cpc_shops += [DynamicShop(500 + seq) for seq in range(1, 46)]

        # находится по прежнему 49 модификаций моделей
        response = self.report.request_json('place=model_modifications&hyperid=500&rids=213')
        self.assertFragmentIn(response, {'search': {'total': 49}})
        response = self.report.request_json('place=model_modifications&hyperid=500&rids=213&numdoc=50')
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {
                            # одна из дорогих модификаций - с ценой
                            "id": 549,
                            "offers": {"count": 1},
                            "prices": {"min": "3450", "max": "3450"},
                        },
                        {
                            # одна из дешевых модификаций - не в продаже
                            "id": 503,
                            "offers": {"count": 0},
                            "prices": NoKey("prices"),
                        },
                    ]
                }
            },
            allow_different_len=True,
            preserve_order=False,
        )

        # с флагом onstock находится только 4 модели из дорогих
        response = self.report.request_json('place=model_modifications&hyperid=500&rids=213&onstock=1')
        self.assertFragmentIn(
            response,
            {'search': {'total': 4, 'results': [{"id": 546}, {"id": 547}, {"id": 548}, {"id": 549}]}},
            allow_different_len=False,
            preserve_order=False,
        )

    @classmethod
    def prepare_group_model_no_modifications(cls):
        cls.index.hypertree += [HyperCategory(hid=23538000, output_type=HyperCategoryType.GURU, has_groups=True)]
        cls.index.model_groups += [ModelGroup(title='group model', hyperid=23538000, hid=23538000)]

        cls.index.models += [
            Model(title='modification model', hyperid=23538001, group_hyperid=23538000, hid=23538000, ts=2353002)
        ]

        cls.index.shops += [Shop(fesh=23538000, priority_region=213)]
        cls.index.offers += [
            Offer(
                title='gm23538000 offer1',
                fesh=23538000,
                price=1000,
                price_history=5000,
                price_old=3000,
                hyperid=23538000,
                ts=2353001,
            ),
            Offer(
                title='gm23538000 offer2',
                fesh=23538000,
                price=1500,
                price_history=5000,
                price_old=3000,
                hyperid=23538001,
                ts=2353002,
            ),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 2353001).respond(0.4)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 2353002).respond(0.3)

    def test_group_model_has_do_for_benefit_requests(self):
        """Group model contains DO in case filter-promo-or-discount=1"""

        response = self.report.request_json(
            'place=prime&rids=213&allow-collapsing=1&hid=23538000&use-default-offers=1&filter-promo-or-discount=1'
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "product",
                        "titles": {"raw": "group model"},
                        "offers": {"count": 2, "items": [{"entity": "offer"}]},
                        "prices": {"min": "1000", "max": "1500", "currency": "RUR"},
                    }
                ]
            },
        )

    @classmethod
    def prepare_model_for_discount(cls):
        cls.index.model_groups += [ModelGroup(title='mega group model', hyperid=299, hid=10682611)]
        cls.index.models += [
            Model(title='mega model', hyperid=300, group_hyperid=299, hid=10682611),
        ]
        cls.index.shops += [
            Shop(fesh=301, priority_region=213),
            Shop(fesh=302, priority_region=213),
            Shop(fesh=303, priority_region=213),
        ]
        cls.index.gltypes += [
            GLType(param_id=901, hid=10682611, gltype=GLType.ENUM, values=[1, 2], cluster_filter=True)
        ]
        cls.index.offers += [
            Offer(
                title='mega offer w/o discount',
                fesh=301,
                price=100,
                hyperid=300,
                glparams=[GLParam(param_id=901, value=1)],
            ),
            Offer(
                title='mega offer with 18% discount',
                fesh=302,
                price=90,
                price_old=110,
                hyperid=300,
                glparams=[GLParam(param_id=901, value=1)],
            ),
            Offer(
                title='mega offer with 23% discount',
                fesh=303,
                price=80,
                price_old=120,
                hyperid=300,
                glparams=[GLParam(param_id=901, value=1)],
            ),
        ]

    def test_statistics_upadate_model_discount(self):
        """
        Check simple model discount realtime statistics calculation for prime and modelinfo:
        1) Check initial discount values for model
        2) Disable shop with offer enabling max discount badge and check discount values for model
        2) Disable all shops with offers enabling max discount badge and check thre is no discount for model
        """
        """
        All three "mega offers"
        """
        expect = {
            "entity": "product",
            "offers": {"count": 3},
            "prices": {
                "min": "80",
                "max": "100",
                "discount": {"oldMin": "100", "percent": 33},
                "onlyOneShopAndAllOffersAreEqual": False,
            },
        }
        response = self.report.request_json('place=prime&rids=213&text=mega&glfilter=901:1&hid=10682611')
        self.assertFragmentIn(response, expect)
        """
        Disable offer with max discount:
        disabled "mega offer" for shop 3
        """
        self.dynamic.market_dynamic.disabled_cpc_shops += [DynamicShop(303)]

        expect = {
            "entity": "product",
            "offers": {"count": 2},
            "prices": {
                "min": "90",
                "max": "100",
                "discount": {"oldMin": "100", "percent": 18},
                "onlyOneShopAndAllOffersAreEqual": True,
            },
        }
        response = self.report.request_json('place=prime&rids=213&text=mega&glfilter=901:1&hid=10682611')
        self.assertFragmentIn(response, expect)
        """
        All offers with discount are disabled:
        disabled "mega offers" for shop 2 and 3
        """
        self.dynamic.market_dynamic.disabled_cpc_shops += [DynamicShop(302)]

        expect = {
            "entity": "product",
            "offers": {"count": 1},
            "prices": {"min": "100", "max": "100", "discount": NoKey("discount")},
            "onlyOneShopAndAllOffersAreEqual": NoKey("onlyOneShopAndAllOffersAreEqual"),
        }
        response = self.report.request_json('place=prime&rids=213&text=mega&glfilter=901:1&hid=10682611')
        self.assertFragmentIn(response, expect)

    def test_statistics_discount_on_group_model(self):
        """
        Проверяем, что для групповой модели не считается скидка
        """
        response = self.report.request_json('place=prime&rids=213&text=mega')
        self.assertFragmentIn(response, {"type": "group", "id": 299, "prices": {"discount": NoKey("discount")}})
        # проверем, что при фильтрации по скидке групповая модель отфильтровывается
        response = self.report.request_json('place=prime&rids=213&text=mega&filter-discount-only=1')
        self.assertFragmentNotIn(response, {"type": "group", "id": 299})

    @classmethod
    def prepare_model_for_discount_condition(cls):
        cls.index.models += [Model(title='hyper model', hyperid=400)]
        cls.index.shops += [Shop(fesh=401, priority_region=213), Shop(fesh=402, priority_region=213)]
        cls.index.offers += [
            Offer(title='hyper offer w/o discount', fesh=401, price=100, hyperid=400),
            Offer(title='hyper offer with 8% discount', fesh=402, price=110, price_old=120, hyperid=400),
        ]

    def test_statistics_discount_condition(self):
        """
        Discount badge is shown for models only if new price is a new minimum
        Model 400 cheapest offer has no discount and there is expensive one with discount
        """
        expect = {
            "entity": "product",
            "offers": {"count": 2},
            "prices": {"min": "100", "max": "110", "discount": NoKey("discount")},
        }
        response = self.report.request_json('place=prime&rids=213&text=hyper')
        self.assertFragmentIn(response, expect)

    @classmethod
    def prepare_for_price_sort(cls):
        for seq in range(15):
            cls.index.models += [Model(title='nano model ' + str(seq), hyperid=2000 + seq)]
            for offer_seq in range(5):
                cls.index.shops += [Shop(fesh=2000 + 5 * seq + offer_seq, priority_region=213)]
                cls.index.offers += [
                    Offer(
                        title='nano offer %s%s' % (seq, offer_seq),
                        fesh=2000 + 5 * seq + offer_seq,
                        price=1000 + 10 * seq + 100 * offer_seq,
                        hyperid=2000 + seq,
                    )
                ]

    def test_prime_model_reordering(self):
        """
        Check models rearrangement based on realtime statistics calculation for prime
        1) Check initial models and offer order
        2) Disable shops with minimal prices for two models and check that models were rearranged
        """
        expect_initial = {
            "results": [
                {
                    "titles": {
                        "raw": "nano model 10",
                    }
                },
                {
                    "titles": {
                        "raw": "nano model 11",
                    }
                },
                {
                    "titles": {
                        "raw": "nano model 12",
                    }
                },
                {
                    "titles": {
                        "raw": "nano model 13",
                    }
                },
                {
                    "titles": {
                        "raw": "nano model 14",
                    }
                },
                {
                    "titles": {
                        "raw": "nano offer 00",
                    }
                },
                {
                    "titles": {
                        "raw": "nano offer 10",
                    }
                },
                {
                    "titles": {
                        "raw": "nano offer 20",
                    }
                },
                {
                    "titles": {
                        "raw": "nano offer 30",
                    }
                },
                {
                    "titles": {
                        "raw": "nano offer 40",
                    }
                },
            ]
        }
        response = self.report.request_json('place=prime&rids=213&text=nano&how=aprice&page=2')
        self.assertFragmentIn(response, expect_initial, preserve_order=True)
        self.dynamic.market_dynamic.disabled_cpc_shops += [DynamicShop(2050), DynamicShop(2065)]

        expect_updated = {
            "results": [
                {
                    "titles": {
                        "raw": "nano model 11",
                    }
                },
                {
                    "titles": {
                        "raw": "nano model 12",
                    }
                },
                {
                    "titles": {
                        "raw": "nano model 14",
                    }
                },
                {
                    "titles": {
                        "raw": "nano model 10",
                    }
                },
                {
                    "titles": {
                        "raw": "nano model 13",
                    }
                },
                {
                    "titles": {
                        "raw": "nano offer 00",
                    }
                },
                {
                    "titles": {
                        "raw": "nano offer 10",
                    }
                },
                {
                    "titles": {
                        "raw": "nano offer 20",
                    }
                },
                {
                    "titles": {
                        "raw": "nano offer 30",
                    }
                },
                {
                    "titles": {
                        "raw": "nano offer 40",
                    }
                },
            ]
        }
        response = self.report.request_json('place=prime&rids=213&text=nano&how=aprice&page=2')
        self.assertFragmentIn(response, expect_updated, preserve_order=True)

    def test_prime_model_reordering_desc(self):
        """
        Check models rearrangement based on realtime statistics calculation for prime for desc sort
        1) Check initial models and offer order
        2) Disable shops with maximum prices for two models and check that models were rearranged
        """
        expect_initial = {
            "results": [
                {
                    "titles": {
                        "raw": "nano model 4",
                    }
                },
                {
                    "titles": {
                        "raw": "nano model 3",
                    }
                },
                {
                    "titles": {
                        "raw": "nano model 2",
                    }
                },
                {
                    "titles": {
                        "raw": "nano model 1",
                    }
                },
                {
                    "titles": {
                        "raw": "nano model 0",
                    }
                },
                {
                    "titles": {
                        "raw": "nano offer 144",
                    }
                },
                {
                    "titles": {
                        "raw": "nano offer 134",
                    }
                },
                {
                    "titles": {
                        "raw": "nano offer 124",
                    }
                },
                {
                    "titles": {
                        "raw": "nano offer 114",
                    }
                },
                {
                    "titles": {
                        "raw": "nano offer 104",
                    }
                },
            ]
        }
        response = self.report.request_json('place=prime&rids=213&text=nano&how=dprice&page=2')
        self.assertFragmentIn(response, expect_initial, preserve_order=True)
        self.dynamic.market_dynamic.disabled_cpc_shops += [DynamicShop(2000), DynamicShop(2010), DynamicShop(2015)]

        expect_updated = {
            "results": [
                {
                    "titles": {
                        "raw": "nano model 7",
                    }
                },
                {
                    "titles": {
                        "raw": "nano model 6",
                    }
                },
                {
                    "titles": {
                        "raw": "nano model 5",
                    }
                },
                {
                    "titles": {
                        "raw": "nano model 4",
                    }
                },
                {
                    "titles": {
                        "raw": "nano model 1",
                    }
                },
                {
                    "titles": {
                        "raw": "nano offer 144",
                    }
                },
                {
                    "titles": {
                        "raw": "nano offer 134",
                    }
                },
                {
                    "titles": {
                        "raw": "nano offer 124",
                    }
                },
                {
                    "titles": {
                        "raw": "nano offer 114",
                    }
                },
                {
                    "titles": {
                        "raw": "nano offer 104",
                    }
                },
            ]
        }
        response = self.report.request_json('place=prime&rids=213&text=nano&how=dprice&page=2')
        self.assertFragmentIn(response, expect_updated, preserve_order=True)

    @classmethod
    def prepare_offer_count_sort(cls):
        cls.index.models += [
            Model(title='count model 1', hyperid=701),
            Model(title='count model 2', hyperid=702),
        ]
        cls.index.shops += [
            Shop(fesh=711, priority_region=213),
            Shop(fesh=712, priority_region=213),
        ]
        cls.index.offers += [
            Offer(title='model 1 offer 1', hyperid=701, fesh=711, price=100),
            Offer(title='model 1 offer 2', hyperid=701, fesh=711, price=100),
            Offer(title='model 1 offer 3', hyperid=701, fesh=712, price=100),
            Offer(title='model 2 offer 1', hyperid=702, fesh=712, price=100),
            Offer(title='model 2 offer 2', hyperid=702, fesh=712, price=100),
        ]

    def test_offer_count_sort(self):
        """Проверяем сортировку по числу офферов с учетом онлайн-статистик.
        У модели 701 3 оффера, у модели 702 2 оффера, поэтому модель 701 выше в выдаче.
        Отключаем магазин с двумя офферами модели 701, теперь она ниже в выдаче.
        """
        response = self.report.request_json('place=prime&rids=213&text=count+model&how=dnoffers')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"id": 701},
                    {"id": 702},
                ]
            },
            preserve_order=True,
        )
        self.dynamic.market_dynamic.disabled_cpc_shops += [DynamicShop(711)]

        response = self.report.request_json('place=prime&rids=213&text=count&how=dnoffers')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"id": 702},
                    {"id": 701},
                ]
            },
            preserve_order=True,
        )

    @classmethod
    def prepare_dynamic_model_stats_with_filters(cls):
        '''Тестируем: динамический рассчет цены и количества офферов для моделей
        Цена "от", цена "до" и количество предложений на кнопке "Цены" должны
        рассчитываться только по офферам удовлетворяющим пользовательским фильтрам
        (gl-фильтрам, фильтрам по цене, по магазину, рейтингу, доставке и т.д. и т.п.)
        При сортировке по цене модели должны пересортировываться по возрастанию (убыванию)
        уже перерассчитанной цены
        Заводим приличное количество разных моделей и офферов к ним с разными параметрами
        gl-фильтров, цены, рейтинга, доставки, cpa/cpc и т.д.
        '''
        cls.index.hypertree += [
            HyperCategory(hid=10682610, uniq_name='Мягкие игрушки', output_type=HyperCategoryType.GURU),
            HyperCategory(hid=11111111, uniq_name='Кошельки', output_type=HyperCategoryType.GURU),
        ]
        cls.index.gltypes += [
            # model filters
            GLType(param_id=201, hid=10682610, gltype=GLType.ENUM, name=u'Материал', values=[201011, 201012, 201013]),
            GLType(param_id=202, hid=10682610, gltype=GLType.BOOL, name=u'Антистрессовая'),
            # second kind filters for modifications
            GLType(param_id=203, hid=10682610, gltype=GLType.BOOL, name=u'Домик в комплекте', cluster_filter=True),
            # second kind filters for offers
            GLType(
                param_id=204,
                hid=10682610,
                gltype=GLType.ENUM,
                name=u'Цвет',
                values=[1, 2, 3, 4, 5],
                cluster_filter=True,
            ),
            GLType(param_id=205, hid=10682610, gltype=GLType.NUMERIC, name=u'Высота (см)', cluster_filter=True),
        ]
        cls.index.shops += [
            Shop(
                fesh=1,
                priority_region=191,
                regions=[213, 191],
                name='Новые игрушки',
                cpa=Shop.CPA_REAL,
                new_shop_rating=NewShopRating(new_rating_total=3.0),
            ),
            Shop(
                fesh=2,
                priority_region=213,
                regions=[213],
                name='ЦУМ-ГУМ',
                cpa=Shop.CPA_REAL,
                new_shop_rating=NewShopRating(new_rating_total=5.0),
            ),
            Shop(
                fesh=3,
                priority_region=213,
                regions=[],
                name='Лавочка у метро',
                new_shop_rating=NewShopRating(new_rating_total=4.0),
            ),
        ]
        cls.index.outlets += [
            Outlet(fesh=1, region=213, point_id=1),
            Outlet(fesh=2, region=213, point_id=2),
            Outlet(fesh=3, region=213, point_id=3),
        ]

        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=5001,
                fesh=1,
                carriers=[99],
                options=[PickupOption(outlet_id=1)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=5002,
                fesh=2,
                carriers=[99],
                options=[PickupOption(outlet_id=2)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=5003,
                fesh=3,
                carriers=[99],
                options=[PickupOption(outlet_id=3)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
        ]

        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=1001,
                fesh=1,
                carriers=[99],
                regional_options=[
                    RegionalDelivery(
                        rid=213, options=[DeliveryOption(price=300, day_from=0, day_to=1, order_before=24)]
                    ),
                ],
            ),
        ]
        cls.index.model_groups += [
            ModelGroup(
                hyperid=10600,
                hid=10682610,
                title='Кошка с котятами',
                randx=10,
                ts=1,
                glparams=[GLParam(param_id=201, value=201013), GLParam(param_id=202, value=1)],
            ),
        ]
        cls.index.models += [
            Model(
                group_hyperid=10600,
                hyperid=1060001,
                hid=10682610,
                title='Кошка с котятами и кошкин дом',
                randx=9,
                ts=2,
                glparams=[
                    GLParam(param_id=201, value=201013),
                    GLParam(param_id=202, value=1),
                    GLParam(param_id=203, value=1),
                ],
            ),
            Model(
                group_hyperid=10600,
                hyperid=1060002,
                hid=10682610,
                title='Кошка без дома и котята',
                randx=8,
                glparams=[
                    GLParam(param_id=201, value=201013),
                    GLParam(param_id=202, value=1),
                    GLParam(param_id=203, value=0),
                ],
            ),
            Model(
                hyperid=10601,
                hid=10682610,
                title='Кот обормот',
                randx=7,
                glparams=[GLParam(param_id=201, value=201011), GLParam(param_id=202, value=1)],
            ),
            Model(
                hyperid=10602, hid=10682610, title='Кот Федот', randx=6, glparams=[GLParam(param_id=201, value=201011)]
            ),
            Model(
                hyperid=10603,
                hid=10682610,
                title='Кот Матроскин',
                randx=5,
                glparams=[GLParam(param_id=201, value=201012)],
            ),
            Model(hyperid=10604, hid=10682610, title='Кошка гуляющая сама по себе', randx=4),
            Model(hyperid=10605, hid=10682610, title='Кошка Матроска', randx=3),
            Model(hyperid=10606, hid=10682610, title='Котенок Гав', randx=2),
            Model(hyperid=10607, hid=10682610, title='Simon cat', randx=1),
        ]
        cls.index.offers += [
            # кошкин дом
            Offer(
                hyperid=1060001,
                fesh=1,
                price=1700,
                manufacturer_warranty=True,
                delivery_buckets=[1001],
                has_delivery_options=True,
                pickup=False,
                store=False,
                post_term_delivery=False,
                glparams=[GLParam(param_id=203, value=1), GLParam(param_id=204, value=2)],
                randx=91,
                pickup_buckets=[5001],
            ),
            Offer(
                hyperid=1060001,
                fesh=2,
                price=1000,
                manufacturer_warranty=False,
                has_delivery_options=False,
                pickup=True,
                store=False,
                post_term_delivery=False,
                glparams=[GLParam(param_id=203, value=1), GLParam(param_id=204, value=3)],
                randx=92,
                pickup_buckets=[5002],
            ),
            Offer(
                hyperid=1060002,
                fesh=3,
                price=1300,
                manufacturer_warranty=False,
                has_delivery_options=False,
                pickup=False,
                store=True,
                post_term_delivery=False,
                glparams=[GLParam(param_id=203, value=0), GLParam(param_id=204, value=4)],
                randx=81,
                pickup_buckets=[5003],
            ),
            # кот обормот
            Offer(
                hyperid=10601,
                fesh=1,
                has_delivery_options=True,
                delivery_buckets=[1001],
                pickup=False,
                cpa=Offer.CPA_REAL,
                price=1500,
                glparams=[GLParam(param_id=204, value=1), GLParam(param_id=205, value=35)],
                randx=71,
                pickup_buckets=[5001],
            ),
            Offer(
                hyperid=10601,
                fesh=2,
                has_delivery_options=False,
                pickup=True,
                cpa=Offer.CPA_REAL,
                price=2300,
                glparams=[GLParam(param_id=204, value=1), GLParam(param_id=205, value=50)],
                randx=72,
                pickup_buckets=[5002],
            ),
            Offer(
                hyperid=10601,
                fesh=3,
                has_delivery_options=False,
                pickup=True,
                store=True,
                price=1000,
                price_old=1200,
                glparams=[GLParam(param_id=204, value=2), GLParam(param_id=205, value=25)],
                randx=73,
                pickup_buckets=[5003],
            ),
            # кот Федот
            Offer(
                hyperid=10602,
                fesh=1,
                price=900,
                manufacturer_warranty=False,
                glparams=[GLParam(param_id=204, value=1), GLParam(param_id=205, value=10)],
                randx=61,
                pickup_buckets=[5001],
            ),
            Offer(
                hyperid=10602,
                fesh=2,
                price=910,
                manufacturer_warranty=False,
                glparams=[GLParam(param_id=204, value=2), GLParam(param_id=205, value=10)],
                randx=62,
                pickup_buckets=[5002],
            ),
            Offer(
                hyperid=10602,
                fesh=2,
                price=915,
                manufacturer_warranty=False,
                glparams=[GLParam(param_id=204, value=3), GLParam(param_id=205, value=10)],
                randx=63,
                pickup_buckets=[5002],
            ),
            Offer(
                hyperid=10602,
                fesh=2,
                price=1100,
                manufacturer_warranty=True,
                glparams=[GLParam(param_id=204, value=3), GLParam(param_id=205, value=10)],
                randx=64,
                pickup_buckets=[5002],
            ),
            Offer(
                hyperid=10602,
                fesh=3,
                price=1115,
                manufacturer_warranty=True,
                glparams=[GLParam(param_id=204, value=4), GLParam(param_id=205, value=10)],
                randx=65,
                pickup_buckets=[5003],
            ),
            Offer(
                hyperid=10602,
                fesh=3,
                price=1125,
                manufacturer_warranty=True,
                glparams=[GLParam(param_id=204, value=5), GLParam(param_id=205, value=10)],
                randx=66,
                pickup_buckets=[5003],
            ),
            # кот Матроскин
            Offer(
                hyperid=10603,
                fesh=1,
                price=500,
                glparams=[GLParam(param_id=204, value=2), GLParam(param_id=205, value=10)],
                randx=51,
                pickup_buckets=[5001],
            ),
            Offer(
                hyperid=10603,
                fesh=1,
                price=800,
                glparams=[GLParam(param_id=204, value=2), GLParam(param_id=205, value=25)],
                randx=52,
                pickup_buckets=[5001],
            ),
            Offer(
                hyperid=10603,
                fesh=1,
                price=1100,
                glparams=[GLParam(param_id=204, value=2), GLParam(param_id=205, value=30)],
                randx=53,
                pickup_buckets=[5001],
            ),
            Offer(
                hyperid=10603,
                fesh=1,
                price=1800,
                glparams=[GLParam(param_id=204, value=2), GLParam(param_id=205, value=55)],
                randx=54,
                pickup_buckets=[5001],
            ),
            # кошка гуляющая сама по себе
            Offer(
                hyperid=10604,
                fesh=2,
                price=300,
                glparams=[GLParam(param_id=204, value=2), GLParam(param_id=205, value=20)],
                randx=41,
                pickup_buckets=[5002],
            ),
            Offer(
                hyperid=10604,
                fesh=3,
                price=3000,
                glparams=[GLParam(param_id=204, value=3), GLParam(param_id=205, value=20)],
                randx=42,
                pickup_buckets=[5003],
            ),
            # кошка Матроска
            Offer(
                hyperid=10605,
                fesh=2,
                price=2900,
                glparams=[GLParam(param_id=204, value=2), GLParam(param_id=205, value=20)],
                randx=31,
                pickup_buckets=[5002],
            ),
            Offer(
                hyperid=10605,
                fesh=3,
                price=200,
                glparams=[GLParam(param_id=204, value=3), GLParam(param_id=205, value=20)],
                randx=32,
                pickup_buckets=[5003],
            ),
            # котенок Гав
            Offer(
                hyperid=10606,
                fesh=2,
                price=1020,
                discount=20,
                glparams=[GLParam(param_id=204, value=2), GLParam(param_id=205, value=20)],
                randx=21,
                pickup_buckets=[5002],
            ),
            Offer(
                hyperid=10606,
                fesh=2,
                price=1080,
                discount=10,
                glparams=[GLParam(param_id=204, value=2), GLParam(param_id=205, value=20)],
                randx=22,
                pickup_buckets=[5002],
            ),
            Offer(
                hyperid=10606,
                fesh=2,
                price=1140,
                discount=5,
                glparams=[GLParam(param_id=204, value=2), GLParam(param_id=205, value=20)],
                randx=23,
                pickup_buckets=[5002],
            ),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1).respond(0.5)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 2).respond(0.4)

    class ExpectResults:
        def __init__(self):
            self.items = []

        def model(self, title, offers, no_price=False, min=None, max=None, discount=None):
            self.items.append(
                {
                    "entity": "product",
                    "titles": {"raw": title},
                    "offers": {"count": offers},
                    "prices": (
                        NoKey("prices")
                        if no_price
                        else {
                            "min": str(min) or NoKey("min"),
                            "max": str(max) or NoKey("max"),
                            "discount": {"percent": discount} if discount else NoKey("discount"),
                        }
                    ),
                }
            )
            return self

        def regional_delimiter(self):
            self.items.append({"entity": "regionalDelimiter"})
            return self

        def results(self):
            return {"results": self.items}

    def test_dms_with_delivery_included(self):
        '''Цена модели аггрегирует цены офферов с учетом цены доставки'''

        unified_off_flags = ';market_dsbs_tariffs=0;market_unified_tariffs=0'
        cgi = (
            '&allow-collapsing=1&rearr-factors=market_rearrange_top_n_pages=1;market_update_model_stats_with_filters=1'
            + unified_off_flags
            + '&hid=10682610&glfilter=203:1'
        )
        # галка "Цена с учетом доставки" выключена
        response = self.report.request_json(
            'place=prime&text=кошка+с+котятами+и+кошкин+дом&rids=213&deliveryincluded=0' + cgi
        )
        self.assertFragmentIn(
            response,
            T.ExpectResults().model('Кошка с котятами и кошкин дом', offers=2, min=1000, max=1700).results(),
            allow_different_len=True,
            preserve_order=False,
        )
        # галка "Цена с учетом доставки" включена - верхняя цена модели увеличилась на 300 (цену доставки оффера за 1700)
        response = self.report.request_json(
            'place=prime&text=кошка+с+котятами+и+кошкин+дом&rids=213&deliveryincluded=1' + cgi
        )
        self.assertFragmentIn(
            response,
            T.ExpectResults().model('Кошка с котятами и кошкин дом', offers=2, min=1000, max=2000).results(),
            allow_different_len=True,
            preserve_order=False,
        )
        # галка "Цена с учетом доставки" включена - выставлено ограничение цены до 1900 рублей
        # оффер за 1700+300 не подходит под условия, в результате остается один оффер за 1000
        response = self.report.request_json(
            'place=prime&text=кошка+с+котятами+и+кошкин+дом&rids=213&deliveryincluded=1&mcpriceto=1900' + cgi
        )
        self.assertFragmentIn(
            response,
            T.ExpectResults().model('Кошка с котятами и кошкин дом', offers=1, min=1000, max=1000).results(),
            allow_different_len=True,
            preserve_order=False,
        )

    def test_dms_with_gl_filters(self):
        '''Как работает обновление статистик по gl-фильтрам'''
        cgi = (
            '&allow-collapsing=1&rearr-factors=market_rearrange_top_n_pages=1;market_update_model_stats_with_filters=1'
        )
        # запрос  без gl-фильтров на дефолтную сортировку
        response = self.report.request_json('place=prime&hid=10682610&text=кот&rids=213' + cgi)
        self.assertFragmentIn(
            response,
            T.ExpectResults()
            .model('Кот Матроскин', offers=4, min=500, max=1800)
            .model('Кот обормот', offers=3, min=1000, max=2300, discount=NotEmpty())
            .model('Кот Федот', offers=6, min=900, max=1125)
            .results(),
            allow_different_len=False,
            preserve_order=False,
        )
        # запрос с gl-фильтрами на дефолтную сортировку (высота до 25 см)
        # цены и количество офферов у модели отображаются с учетом данного фильтра
        response = self.report.request_json('place=prime&hid=10682610&text=кот&rids=213&glfilter=205:,25' + cgi)
        self.assertFragmentIn(
            response,
            T.ExpectResults()
            .model('Кот Матроскин', offers=2, min=500, max=800)
            .model('Кот обормот', offers=1, min=1000, max=1000, discount=NotEmpty())
            .model('Кот Федот', offers=6, min=900, max=1125)
            .results(),
            allow_different_len=False,
            preserve_order=False,
        )
        # запрос с двумя gl-фильтрами
        response = self.report.request_json(
            'place=prime&hid=10682610&text=кот&rids=213&glfilter=205:,25&glfilter=204:3' + cgi
        )
        self.assertFragmentIn(
            response,
            T.ExpectResults().model('Кот Федот', offers=2, min=915, max=1100).results(),
            allow_different_len=False,
            preserve_order=False,
        )
        # проверяем как фильтруются групповая модель Кошка с котятами
        # запрос без gl-фильтров
        response = self.report.request_json('place=prime&hid=10682610&text=кошка+котята&rids=213' + cgi)
        self.assertFragmentIn(
            response,
            T.ExpectResults().model('Кошка с котятами', offers=3, min=1000, max=1700).results(),
            allow_different_len=True,
            preserve_order=False,
        )
        # интересно что групповая модель сама по себе не подходит под параметры
        response = self.report.request_json(
            'place=prime&hid=10682610&text=кошка+котята&rids=213&glfilter=203:1&glfilter=204:3' + cgi
        )
        self.assertFragmentNotIn(response, {"results": [{"titles": {"raw": 'Кошка с котятами'}}]})
        # под параметры подходит только модификация у которой подходит один из офферов за 1000
        self.assertFragmentIn(
            response,
            T.ExpectResults().model('Кошка с котятами и кошкин дом', offers=1, min=1000, max=1000).results(),
            allow_different_len=True,
            preserve_order=False,
        )

    def test_dms_with_price_filters(self):
        '''Как работает обновление статистик при фильтрации по цене'''
        # сортировать будем по цене
        cgi = '&how=aprice&allow-collapsing=1&rearr-factors=market_rearrange_top_n_pages=1;market_update_model_stats_with_filters=1;market_metadoc_search=no'
        # обычный запрос - все модели отсортированы, все прекрасно
        # модификации отфильтровываются ещё на релевантности, см. MARKETOUT-19760
        response = self.report.request_json('place=prime&hid=10682610&rids=213&onstock=0' + cgi)
        self.assertFragmentIn(
            response,
            T.ExpectResults()
            .model('Кошка Матроска', offers=2, min=200, max=2900)
            .model('Кошка гуляющая сама по себе', offers=2, min=300, max=3000)
            .model('Кот Матроскин', offers=4, min=500, max=1800)
            .model('Кот Федот', offers=6, min=900, max=1125)
            .model('Кот обормот', offers=3, min=1000, max=2300, discount=NotEmpty())
            .model('Кошка с котятами', offers=3, min=1000, max=1700)
            .model('Котенок Гав', offers=3, min=1020, max=1140, discount=NotEmpty())
            .regional_delimiter()
            .model('Simon cat', offers=0, no_price=True)
            .results(),
            allow_different_len=False,
            preserve_order=True,
        )
        # ограничиваем цену (от 1000 рублей)
        # две самые дешевые модели сразу становятся самыми дорогими
        # Simon Cat - модель не в продаже не отображается в выдаче
        response = self.report.request_json('place=prime&hid=10682610&rids=213&onstock=0&mcpricefrom=1000' + cgi)
        self.assertFragmentIn(
            response,
            T.ExpectResults()
            .model('Кошка с котятами и кошкин дом', offers=2, min=1000, max=1700)
            .model('Кот обормот', offers=3, min=1000, max=2300, discount=NotEmpty())
            .model('Кошка с котятами', offers=3, min=1000, max=1700)
            .model('Котенок Гав', offers=3, min=1020, max=1140, discount=NotEmpty())
            .model('Кот Матроскин', offers=2, min=1100, max=1800)
            .model('Кот Федот', offers=3, min=1100, max=1125)
            .model('Кошка без дома и котята', offers=1, min=1300, max=1300)
            .model('Кошка Матроска', offers=1, min=2900, max=2900)
            .model('Кошка гуляющая сама по себе', offers=1, min=3000, max=3000)
            .results(),
            allow_different_len=False,
            preserve_order=True,
        )
        # задаем дополнительно еще и ограничение сверху (до 2000 рублей)
        # модели оказавшиеся не в продаже убираются
        response = self.report.request_json(
            'place=prime&hid=10682610&rids=213&onstock=0&mcpricefrom=1000&mcpriceto=2000' + cgi
        )
        self.assertFragmentIn(
            response,
            T.ExpectResults()
            .model('Кошка с котятами и кошкин дом', offers=2, min=1000, max=1700)
            .model('Кот обормот', offers=2, min=1000, max=1500, discount=NotEmpty())
            .model('Кошка с котятами', offers=3, min=1000, max=1700)
            .model('Котенок Гав', offers=3, min=1020, max=1140, discount=NotEmpty())
            .model('Кот Матроскин', offers=2, min=1100, max=1800)
            .model('Кот Федот', offers=3, min=1100, max=1125)
            .model('Кошка без дома и котята', offers=1, min=1300, max=1300)
            .results(),
            allow_different_len=False,
            preserve_order=True,
        )
        # проверяем что при разбиении на страницы сохраняется тот же порядок
        response = self.report.request_json(
            'place=prime&hid=10682610&rids=213&onstock=0&mcpricefrom=1000&on-page=5&page=1' + cgi
        )
        self.assertFragmentIn(
            response,
            T.ExpectResults()
            .model('Кошка с котятами и кошкин дом', offers=2, min=1000, max=1700)
            .model('Кот обормот', offers=3, min=1000, max=2300, discount=NotEmpty())
            .model('Кошка с котятами', offers=3, min=1000, max=1700)
            .model('Котенок Гав', offers=3, min=1020, max=1140, discount=NotEmpty())
            .model('Кот Матроскин', offers=2, min=1100, max=1800)
            .results(),
            allow_different_len=False,
            preserve_order=True,
        )
        response = self.report.request_json(
            'place=prime&hid=10682610&rids=213&onstock=0&mcpricefrom=1000&on-page=5&page=2' + cgi
        )
        self.assertFragmentIn(
            response,
            T.ExpectResults()
            .model('Кот Федот', offers=3, min=1100, max=1125)
            .model('Кошка без дома и котята', offers=1, min=1300, max=1300)
            .model('Кошка Матроска', offers=1, min=2900, max=2900)
            .model('Кошка гуляющая сама по себе', offers=1, min=3000, max=3000)
            .results(),
            allow_different_len=False,
            preserve_order=True,
        )

    def test_dms_with_non_gl_filters(self):
        '''Тестируем всякие фильтры вроде fesh, offer-shipping, manufacturer-warranty и т.д.
        Такие параметры относятся к офферам, поэтому модели по ним неплохо фильтруются за счет того
        что в выдачу попадают только офферы удовлетворяющие параметрам
        '''
        cgi = (
            '&allow-collapsing=1&rearr-factors=market_rearrange_top_n_pages=1;market_update_model_stats_with_filters=1'
        )

        # Дефолтная сортировка, интересуют модели представленные в магазине с fesh=2
        response = self.report.request_json('place=prime&hid=10682610&rids=213&onstock=0&fesh=2' + cgi)
        # Количество офферов и цены посчитаны только по офферам соответствующего магазина
        self.assertFragmentIn(
            response,
            T.ExpectResults()
            .model('Кот обормот', offers=1, min=2300, max=2300)
            .model('Кот Федот', offers=3, min=910, max=1100)
            .results(),
            allow_different_len=True,
            preserve_order=False,
        )

        # Интересуют модели с офферами имеющими гаранитю производителя
        # Пример - Кот Федот - из 6 офферов 3 имеют гарантию производителя
        response = self.report.request_json('place=prime&hid=10682610&rids=213&onstock=0&manufacturer_warranty=1' + cgi)
        self.assertFragmentIn(
            response,
            T.ExpectResults().model('Кот Федот', offers=3, min=1100, max=1125).results(),
            allow_different_len=True,
            preserve_order=False,
        )

        # Интересуют модели, офферы которых доставляются курьером
        response = self.report.request_json('place=prime&hid=10682610&rids=213&onstock=0&offer-shipping=delivery' + cgi)
        self.assertFragmentIn(
            response,
            T.ExpectResults().model('Кот обормот', offers=1, min=1500, max=1500).results(),
            allow_different_len=True,
            preserve_order=False,
        )

        # Интересуют модели офферы которых доступны для самовывоза
        # Пример: Кот обормот - Из 3х офферов находится только 2
        response = self.report.request_json('place=prime&hid=10682610&rids=213&onstock=0&offer-shipping=pickup' + cgi)
        self.assertFragmentIn(
            response,
            T.ExpectResults().model('Кот обормот', offers=2, min=1000, max=2300, discount=NotEmpty()).results(),
            allow_different_len=True,
            preserve_order=False,
        )

        # Модели у которых доставка до 1 дня - находятся всего 2 модели (модели и сами умеют фильтроваться по этому параметру)
        # У модели Кошка с котятами и кошкин дом такое предложение единственное и оно из другого региона
        response = self.report.request_json('place=prime&hid=10682610&rids=213&onstock=0&delivery_interval=1' + cgi)
        self.assertFragmentIn(
            response,
            T.ExpectResults()
            .model('Кот обормот', offers=1, min=1500, max=1500)
            .regional_delimiter()
            .model('Кошка с котятами и кошкин дом', offers=1, min=1700, max=1700)
            .results(),
            allow_different_len=False,
            preserve_order=False,
        )

    def test_dms_problems_onstock(self):
        # Модификации отфильтровываются ещё на релевантности, см. MARKETOUT-19760

        # Модели которые после обновления статистик остались без офферов - остаются на своем месте
        # Отображаются как не в продаже (если не задан onstock=1 или фильтр по цене)
        # дефолтная сортировка, запрос без фильтров
        response = self.report.request_json(
            'place=prime&hid=10682610&text=кошка&rids=213&onstock=1&allow-collapsing=1'
            '&rearr-factors=market_metadoc_search=no'
        )
        self.assertFragmentIn(
            response,
            T.ExpectResults()
            .model('Кошка с котятами', offers=3, min=1000, max=1700)
            .model('Кошка гуляющая сама по себе', offers=2, min=300, max=3000)
            .model('Кошка Матроска', offers=2, min=200, max=2900)
            .results(),
            allow_different_len=False,
            preserve_order=True,
        )
        # дефолтная сортировка, запрос с фильтром по цене, приводящим к тому что некоторые модели не имеют подходящих офферов
        # модели не в продаже удаляются
        response = self.report.request_json(
            'place=prime&hid=10682610&text=кошка&rids=213&onstock=0&mcpricefrom=1000&mcpriceto=2000&allow-collapsing=1'
            '&rearr-factors=market_metadoc_search=no'
        )
        self.assertFragmentIn(
            response,
            T.ExpectResults().model('Кошка с котятами', offers=3, min=1000, max=1700).results(),
            allow_different_len=False,
            preserve_order=True,
        )
        # дефолтная сортировка c флагом onstock=1 - модели не в продаже удаляются
        response = self.report.request_json(
            'place=prime&hid=10682610&text=кошка&rids=213&onstock=1&mcpricefrom=1000&mcpriceto=2000&allow-collapsing=1'
            '&rearr-factors=market_metadoc_search=no'
        )
        self.assertFragmentIn(
            response,
            T.ExpectResults().model('Кошка с котятами', offers=3, min=1000, max=1700).results(),
            allow_different_len=False,
            preserve_order=True,
        )
        # недефолтная сортировка - модели не в продаже удаляются
        response = self.report.request_json(
            'place=prime&hid=10682610&text=кошка&rids=213&onstock=1&mcpricefrom=1000&mcpriceto=2000&how=aprice&allow-collapsing=1'
            '&rearr-factors=market_metadoc_search=no'
        )
        self.assertFragmentIn(
            response,
            T.ExpectResults().model('Кошка с котятами', offers=3, min=1000, max=1700).results(),
            allow_different_len=False,
            preserve_order=True,
        )

    def test_dms_problems_total(self):
        '''Проблема с неверным total:
        из-за фильтрации на мете total может показывать больше,
        особенно заметно при небольшом количестве документов.
        Пока при фильтрации документа на мете уменьшаем счетчик.
        Модификации отфильтровываются ещё на релевантности, см. MARKETOUT-19760
        '''
        # фильтруются модели не в продаже
        # находятся только модели
        response = self.report.request_json(
            'place=prime&hid=10682610&text=кошка&rids=213&onstock=1&mcpricefrom=1000&mcpriceto=2000&allow-collapsing=1'
            '&rearr-factors=market_metadoc_search=no'
        )
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 1,
                    "totalOffers": 0,
                    "totalModels": 1,
                }
            },
        )
        self.assertEqual(response.count({"entity": "product"}), 1)
        self.assertEqual(response.count({"entity": "offer"}), 0)

        # находятся модели и оферы
        response = self.report.request_json(
            'place=prime&hid=10682610&rids=213&onstock=1&mcpricefrom=1000&mcpriceto=2000&allow-collapsing=0&numdoc=25'
            '&rearr-factors=market_metadoc_search=no'
        )
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 18,
                    "totalOffers": 13,
                    "totalModels": 5,
                }
            },
        )
        self.assertEqual(response.count({"entity": "product"}), 5)
        self.assertEqual(response.count({"entity": "offer"}), 13)
        response = self.report.request_json(
            'place=prime&hid=10682610&rids=213&onstock=1&mcpricefrom=1000&mcpriceto=2000&allow-collapsing=1'
            '&rearr-factors=market_metadoc_search=no'
        )
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 7,
                    "totalOffers": 0,
                    "totalModels": 7,
                }
            },
        )
        self.assertEqual(response.count({"entity": "product"}), 7)
        self.assertEqual(response.count({"entity": "offer"}), 0)

    def test_dms_problems_glfilters_text(self):
        # Проблема №3
        # Фильтры характерные для офферов ограничивают поиск по моделям с использованием текста
        # ну может это и плюс конечно - иначе бы мы сели по производительности
        # без текста находится модель из-за схлапывания оффера в модель
        response = self.report.request_json(
            'place=prime&hid=10682610&rids=213&onstock=0&manufacturer_warranty=1&allow-collapsing=1'
        )
        self.assertFragmentIn(
            response,
            T.ExpectResults().model('Кот Федот', offers=3, min=1100, max=1125).results(),
            allow_different_len=True,
            preserve_order=False,
        )
        # а с текстом - модель не находится, т.к. не находится подходящий оффер. Выглядит очень странным
        response = self.report.request_json(
            'place=prime&hid=10682610&rids=213&onstock=0&manufacturer_warranty=1&text=кот+федот&allow-collapsing=1'
        )
        self.assertFragmentIn(response, {"search": {"total": 0, "results": []}}, allow_different_len=False)

    @classmethod
    def prepare_dms_problem_4(cls):
        '''
        Пороблема №4
        Статистики обновляются только для моделей находящихся среди первых N документов
        За пределами первых N документов (с учетом того что некоторые из этих N могут быть пофильтрованы как не в продаже)
        начинается "старый маркет": цены и количество не соответствуют действительности, внутри может не быть офферов и т.д. и т.п.
        Создаем большое количество моделек с несколькими офферами
        При применении фильтра по fesh=2 в модельках должен остаться один оффер со средней ценой
        '''
        hid = 11111111
        for hyperid in range(10000, 10250):
            cls.index.models += [
                Model(hyperid=hyperid, ts=hyperid, hid=hid, title='Кошелек с {} рублями'.format(hyperid))
            ]
            cls.index.offers += [
                Offer(hyperid=hyperid, fesh=1, title='кошелек', price=hyperid - 1, pickup_buckets=[5001]),
                Offer(hyperid=hyperid, fesh=2, title='кошелек', price=hyperid, pickup_buckets=[5002]),
                Offer(hyperid=hyperid, fesh=1, title='кошелек', price=hyperid + 1, pickup_buckets=[5001]),
            ]

    def test_dms_problem_4(self):
        flags = (
            'market_rearrange_top_n_pages=1;'
            'market_update_model_stats_with_filters=1;'
            'output_max_page_count=0'  # убираем лимит на кол-во страниц
        )
        cgi = '&allow-collapsing=1&on-page=5&rearr-factors=' + flags
        # страница 1
        # никакие офферы не отфильтрованы - обновленная статистика соответствует статистике из индексатора
        # (при этом на первой странице она все-таки считается динамически)
        response = self.report.request_json('place=prime&text=Кошелек&rids=213&how=aprice' + cgi)
        self.assertFragmentIn(
            response,
            T.ExpectResults()
            .model('Кошелек с 10000 рублями', offers=3, min=9999, max=10001)
            .model('Кошелек с 10001 рублями', offers=3, min=10000, max=10002)
            .model('Кошелек с 10002 рублями', offers=3, min=10001, max=10003)
            .model('Кошелек с 10003 рублями', offers=3, min=10002, max=10004)
            .model('Кошелек с 10004 рублями', offers=3, min=10003, max=10005)
            .results(),
            allow_different_len=False,
            preserve_order=True,
        )
        # последняя страница
        # статистика для моделей не обновляется, но т.к. офферы не отфильтровываются - то все выглядит прилично
        response = self.report.request_json('place=prime&page=50&text=Кошелек&rids=213&how=aprice' + cgi)
        self.assertFragmentIn(
            response,
            T.ExpectResults()
            .model('Кошелек с 10245 рублями', offers=3, min=10244, max=10246)
            .model('Кошелек с 10246 рублями', offers=3, min=10245, max=10247)
            .model('Кошелек с 10247 рублями', offers=3, min=10246, max=10248)
            .model('Кошелек с 10248 рублями', offers=3, min=10247, max=10249)
            .model('Кошелек с 10249 рублями', offers=3, min=10248, max=10250)
            .results(),
            allow_different_len=False,
            preserve_order=True,
        )
        # страница 1 - с применением фильтра по fesh=2
        # в каждой модели должен остаться один оффер
        # цены моделей и количество офферов были обновлены корректо
        response = self.report.request_json('place=prime&text=Кошелек&rids=213&fesh=2&how=aprice' + cgi)
        self.assertFragmentIn(
            response,
            T.ExpectResults()
            .model('Кошелек с 10000 рублями', offers=1, min=10000, max=10000)
            .model('Кошелек с 10001 рублями', offers=1, min=10001, max=10001)
            .model('Кошелек с 10002 рублями', offers=1, min=10002, max=10002)
            .model('Кошелек с 10003 рублями', offers=1, min=10003, max=10003)
            .model('Кошелек с 10004 рублями', offers=1, min=10004, max=10004)
            .results(),
            allow_different_len=False,
            preserve_order=True,
        )
        # последняя страница - с применением фильтра по fesh=2
        # в каждой модели должен остаться один оффер
        # но цены моделей и количество офферов остались прежними
        # и считаются по данным из индексатора
        # за пределами первых N документов мы видим "старый маркет"
        # каким он был до внедрения динамических статистик
        response = self.report.request_json('place=prime&page=50&text=Кошелек&rids=213&fesh=2&how=aprice' + cgi)
        self.assertFragmentIn(
            response,
            T.ExpectResults()
            .model('Кошелек с 10245 рублями', offers=3, min=10244, max=10246)
            .model('Кошелек с 10246 рублями', offers=3, min=10245, max=10247)
            .model('Кошелек с 10247 рублями', offers=3, min=10246, max=10248)
            .model('Кошелек с 10248 рублями', offers=3, min=10247, max=10249)
            .model('Кошелек с 10249 рублями', offers=3, min=10248, max=10250)
            .results(),
            allow_different_len=False,
            preserve_order=True,
        )

    def test_dms_debug(self):
        # Фильтруем офферы по наличию гарантии производителя
        # Пример - Кот Федот - из 6 офферов 3 имеют гарантию производителя
        response = self.report.request_json(
            'place=prime&hid=10682610&rids=213&onstock=0&manufacturer_warranty=1&allow-collapsing=1&debug=da'
        )
        self.assertFragmentIn(response, T.ExpectResults().model('Кот Федот', offers=3, min=1100, max=1125).results())

        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "titles": {"raw": "Кот Федот"},
                        "debug": {
                            "modelStats": {
                                "original": {
                                    "minPrice": "900",
                                    "maxPrice": "1125",
                                    "offers": 6,
                                    "delivery": "Priority",
                                },
                                "updated": {
                                    "minPrice": "1100",
                                    "maxPrice": "1125",
                                    "offers": 3,
                                    "delivery": "Priority",
                                },
                            }
                        },
                    }
                ]
            },
        )

    @classmethod
    def prepare_update_models_delivery(cls):
        # проверяем как обноляется информация о доставке для моделей

        cls.index.shops += [
            Shop(fesh=21, priority_region=191, regions=[191, 213]),
            Shop(fesh=22, priority_region=213, regions=[213]),
        ]

        hid = 222222222
        cls.index.hypertree += [HyperCategory(hid=hid, output_type=HyperCategoryType.GURU)]

        cls.index.gltypes += [GLType(hid=hid, param_id=221, gltype=GLType.ENUM, cluster_filter=True, values=[1, 2, 3])]

        cls.index.models += [
            Model(
                hid=hid,
                hyperid=222201,
                ts=222201,
                title="Модель с локальным оффером (из диффа), но по статистике - не в продаже",
            ),
            Model(
                hid=hid,
                hyperid=222202,
                ts=222202,
                title="Модель с региональным оффером (из диффа), по статистике имеет локальные",
            ),
            Model(hid=hid, hyperid=222203, ts=222203, title="Модель без офферов, по статистике имеет локальные офферы"),
            Model(
                hid=hid, hyperid=222204, ts=222204, title="Модель без офферов, по статистике не имеет локальных офферов"
            ),
            Model(hid=hid, hyperid=222205, ts=222205, title="Модель с локальными и региональными офферами"),
            Model(
                hyperid=222206,
                ts=222206,
                title="Ещё одна модель с локальным оффером (из диффа), но по статистике - не в продаже",
            ),
        ]

        cls.index.regional_models += [
            RegionalModel(hyperid=222201, rids=[213], offers=0, onstock=0, local_offers=0),
            RegionalModel(hyperid=222202, rids=[213], offers=5, onstock=5, local_offers=5),
            RegionalModel(hyperid=222203, rids=[213], offers=2, onstock=2, local_offers=2),
            RegionalModel(hyperid=222204, rids=[213], offers=2, onstock=2, local_offers=0),
            RegionalModel(hyperid=222205, rids=[213], offers=9, onstock=5, local_offers=7),
            RegionalModel(hyperid=222206, rids=[213], offers=0, onstock=0, local_offers=0),
        ]

        cls.index.offers += [
            Offer(hyperid=222201, fesh=22, ts=22220101, title="локальный"),
            Offer(hyperid=222202, fesh=21, ts=22220201, title="региональный"),
            Offer(
                hyperid=222205,
                fesh=21,
                ts=22220501,
                title="Региональный оффер 9Fc_xxxzHVw2Lw14YJRbiA",
                long_description="Региональный оффер 9Fc_xxxzHVw2Lw14YJRbiA который должен схлопнуться",
                waremd5='9Fc_xxxzHVw2Lw14YJRbiA',
            ),
            Offer(
                hyperid=222205,
                fesh=22,
                ts=22220502,
                title="Локальный оффер aKg-60lKMI_lKV2mlCilBw",
                long_description="Локальный оффер aKg-60lKMI_lKV2mlCilBw который должен схлопнуться",
                waremd5='aKg-60lKMI_lKV2mlCilBw',
            ),
            Offer(
                hyperid=222205,
                fesh=21,
                ts=22220503,
                title="региональный с параметром 221:1",
                glparams=[GLParam(param_id=221, value=1)],
            ),
            Offer(
                hyperid=222205,
                fesh=22,
                ts=22220504,
                title="локальный с параметром 221:2",
                glparams=[GLParam(param_id=221, value=2)],
            ),
            Offer(hyperid=222206, fesh=22, ts='22220601', title="Оффер с моделью без статистик", price=14755),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 222201).respond(0.11)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 222202).respond(0.12)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 222203).respond(0.13)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 222204).respond(0.14)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 222205).respond(0.15)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 222206).respond(0.16)

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 22220101).respond(0.31)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 22220201).respond(0.32)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 22220501).respond(0.35)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 22220502).respond(0.35)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 22220503).respond(0.35)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 22220504).respond(0.35)

    def test_delivery_of_models_with_offers_from_diff_index(self):
        '''Статистика для модели из индексатора отстает от реального положения
        Офферы могут быстровключаться или быстроисключаться
        Динамические статистики призваны исправить это положение
        вычисляя для обновляемых моделей есть ли у нее офферы в локальном регионе
        '''
        response = self.report.request_json(
            'place=prime&hid=222222222&rids=213&allow-collapsing=1&local-offers-first=1'
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"titles": {"raw": "Модель с локальными и региональными офферами"}},
                    # модель для которой динстатистики получились без офферов будет иметь DELIVERY_TYPE от исходного документа
                    # в данном случае DELIVERY_TYPE=3 от самой модели в соответствии с ее индексаторными статистиками
                    {"titles": {"raw": "Модель без офферов, по статистике имеет локальные офферы"}},
                    # модель по динстатистикам имеет локальные офферы, т.е. DELIVERY_TYPE=3, хотя изначально она была не в продаже
                    {"titles": {"raw": "Модель с локальным оффером (из диффа), но по статистике - не в продаже"}},
                    {"entity": "regionalDelimiter"},
                    # несмотря на то что по статистикам индексатора модель имеет локальные офферы,
                    # при обновлении через динстатистики эта модель оказывается под чертой т.к. локальных офферов не нашлось
                    {"titles": {"raw": "Модель с региональным оффером (из диффа), по статистике имеет локальные"}},
                    # аналогично предыдущей модели без офферов, данная модель под чертой т.к. в соответствии с
                    # ее статистиками из индексатора она не имеет локальных офферов DELIVERY_TYPE=2 у исходного документа (модели)
                    {"titles": {"raw": "Модель без офферов, по статистике не имеет локальных офферов"}},
                ]
            },
            allow_different_len=False,
            preserve_order=True,
        )

    def test_delivery_of_models_collapsed_from_diff_index(self):
        '''динамические статистики правильно вычисляются для моделей схлопнутых из офферов
        несмотря на то что схлопнутый оффер мог быть под чертой или над ней
        (модель будет над чертой, т.к. имеет и локальные и региональные офферы)
        '''

        response = self.report.request_json(
            'place=prime&text=Локальный+оффер+aKg-60lKMI_lKV2mlCilBw&rids=213&allow-collapsing=1&local-offers-first=1&debug=da'
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "titles": {"raw": "Модель с локальными и региональными офферами"},
                        # исходный документ имел приоритетную доставку
                        "debug": {
                            "isCollapsed": True,
                            "wareId": "aKg-60lKMI_lKV2mlCilBw",
                            "properties": {"DELIVERY_COURIER": "Priority"},
                        },
                    }
                ]
            },
            allow_different_len=False,
            preserve_order=True,
        )

        response = self.report.request_json(
            'place=prime&text=Региональный+оффер+9Fc_xxxzHVw2Lw14YJRbiA&rids=213&allow-collapsing=1&local-offers-first=1&debug=da'
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "titles": {"raw": "Модель с локальными и региональными офферами"},
                        # исходный документ имел доставку из другого региона
                        "debug": {
                            "isCollapsed": True,
                            "wareId": "9Fc_xxxzHVw2Lw14YJRbiA",
                            "properties": {"DELIVERY_COURIER": "Country"},
                        },
                    }
                ]
            },
            allow_different_len=False,
            preserve_order=True,
        )

    def test_delivery_of_models_with_filtered_documents(self):
        '''Если часть документов была отфильтровала фильтрами пользователя
        DELIVERY_TYPE для модели высчитывается по оставшимся после фильтрации документам
        '''

        # Фильтруем офферы с помощью glfilter=221:1 так что в модели останется один региональный оффер
        # модель оказывается под чертой
        response = self.report.request_json(
            'place=prime&text=Модель+с+локальными+и+региональными+офферами&rids=213&allow-collapsing=1&local-offers-first=1&hid=222222222&glfilter=221:1'
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"entity": "regionalDelimiter"},
                    {"titles": {"raw": "Модель с локальными и региональными офферами"}},
                ]
            },
            allow_different_len=False,
            preserve_order=True,
        )

        # Фильтруем офферы с помощью glfilter=221:2 так что остается один локальный оффер
        # та же модель оказывается над чертой
        response = self.report.request_json(
            'place=prime&text=Модель+с+локальными+и+региональными+офферами&rids=213&allow-collapsing=1&local-offers-first=1&hid=222222222&glfilter=221:2'
        )
        self.assertFragmentIn(
            response,
            {"results": [{"titles": {"raw": "Модель с локальными и региональными офферами"}}]},
            allow_different_len=False,
            preserve_order=True,
        )

    def test_offer_rank_price_with_model_without_stats(self):
        """На сортировке по цене не меняем цену оффера на цену модели, если её нет
        https://st.yandex-team.ru/MARKETOUT-14755
        """

        response = self.report.request_json(
            'place=prime&text=оффер+с+моделью+без+статистик&how=aprice&allow-collapsing=1&rids=213&debug=da'
        )
        self.assertFragmentIn(
            response,
            {
                "properties": {"TS": "22220601"},
                "rank": [
                    {
                        "name": "PRICE",
                        "value": "1475500",
                    },
                ],
            },
        )

    @classmethod
    def prepare_default_offer_delivery(cls):
        '''
        MARKETOUT-16012
        Подготовка данных ддя проверки определения типа доставки
        по статисткиам из дефолтного оффера
        '''

        # Создаем две модели и привязывем к ним по одному офферу
        # Один оффер должен иметь курьерскую доставку в регион 213,
        # а второй должен иметь только точку самовыоза в этом регионе

        delivery = [DeliveryOption(day_from=0, day_to=1)]
        cls.index.shops += [
            Shop(fesh=6010, regions=[213], priority_region=213),
            Shop(fesh=6011, regions=[213], priority_region=213),
        ]
        cls.index.outlets += [Outlet(fesh=6011, region=213, point_type=Outlet.FOR_PICKUP, point_id=1000)]

        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=5011,
                fesh=6011,
                carriers=[99],
                options=[PickupOption(outlet_id=1000)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
        ]

        cls.index.models += [
            Model(title='defaultOfferDelivery', hyperid=6000),
            Model(title='defaultOfferDelivery', hyperid=6001),
        ]
        cls.index.offers += [
            Offer(title='DefaultOfferWithDelivery', fesh=6010, hyperid=6000, delivery_options=delivery),
            Offer(
                title='DefaultOfferWithDelivery',
                fesh=6011,
                hyperid=6001,
                has_delivery_options=False,
                pickup=True,
                pickup_buckets=[5011],
            ),
        ]

    def test_default_offer_delivery(self):
        '''
        MARKETOUT-16012
        Проверка определения типа доставки
        по статисткиам из дефолтного оффера
        '''

        # Проверяем, что тип доставки при обновлении статистик
        # модели через дефолтные офферы считается правильно для
        # моделей, которые имеет только оффер с точкой самовывоза.
        # Т.е. в выдаче, где одна модель имеет оффер с курьерской доставкой,
        # а вторая с точкой самовывоза не должно быть черты,
        # т.к. обе модели должны иметь тип доставки priority

        respose = self.report.request_json(
            'place=prime' '&text=defaultOfferDelivery' '&use-default-offers=1' '&rids=213' '&local-offers-first=1'
        )

        self.assertFragmentNotIn(respose, {"entity": "regionalDelimiter"})

    @classmethod
    def prepare_many_models(cls):

        cls.index.hypertree += [HyperCategory(hid=3243928, output_type=HyperCategoryType.GURU)]

        for seq in range(1, 60):
            fesh = hyperid = 1000 + seq
            cls.index.models += [Model(hyperid=hyperid, hid=3243928)]
            cls.index.shops += [Shop(fesh=fesh, priority_region=213)]
            cls.index.offers += [Offer(fesh=fesh, hyperid=hyperid, hid=3243928)]

    def test_dynamic_model_stats_is_valid_for_many_models(self):
        """
        Проверяем что при запросе большого количества моделей через запрос за
        ДО динстатистики не нулевые MARKETOUT-25700
        """

        response = self.report.request_json(
            'place=prime&hid=3243928&rids=213&use-default-offers=1&allow-collapsing=1&numdoc=100'
        )
        self.assertFragmentIn(
            response,
            {"search": {"total": 59, "results": [{"offers": {"count": 1, "items": NotEmpty()}} for _ in range(1, 60)]}},
            allow_different_len=False,
        )

    @classmethod
    def prepare_update_models_stats_with_dyn_stats_when_model_reg_stats_empty(cls):
        cls.index.shops += [
            Shop(fesh=700, priority_region=213),
        ]
        cls.index.models += [
            Model(hid=700, hyperid=7001, title="model with price=200"),
            Model(hid=700, hyperid=7002, title="model with price=300 and empty msk regional stats"),
            Model(hid=700, hyperid=7003, title="model with price=400"),
        ]
        cls.index.offers += [
            Offer(hyperid=7001, price=100, fesh=700),
            Offer(hyperid=7002, price=200, fesh=700),
            Offer(hyperid=7003, price=300, fesh=700),
        ]
        cls.index.regional_models += [
            RegionalModel(hyperid=7002, offers=1, price_min=50, rids=[2]),
        ]

    def test_update_models_stats_with_dyn_stats_when_model_reg_stats_empty(self):
        """
        Бывает редкий случай, когда на момент формирования индекса оффер был отключен,
        а затем его включили. В таком случае окажется, что приматченая модель будет иметь пустые
        региональные статистики, и при этом иметь дин. статистики в текущем регионе.
        В таком случае мы должны правильно подтянуть дин. модельные статистики.

        Ожидается, что модель с пустыми региональными статистиками не пессимизируется, и у неё ONSTOCK=1.
        """
        response = self.report.request_json("hid=700&place=prime&rids=213&allow-collapsing=1&how=aprice&debug=da")
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "id": 7001,
                        "titles": {"raw": "model with price=200"},
                        "debug": {
                            "rank": [{"name": "ONSTOCK", "value": "1"}],
                            "metaRank": [{"name": "ONSTOCK", "value": "1"}],
                        },
                    },
                    {
                        "id": 7002,
                        "titles": {"raw": "model with price=300 and empty msk regional stats"},
                        "debug": {
                            "rank": [{"name": "ONSTOCK", "value": "1"}],
                            "metaRank": [{"name": "ONSTOCK", "value": "1"}],
                        },
                    },
                    {
                        "id": 7003,
                        "titles": {"raw": "model with price=400"},
                        "debug": {
                            "rank": [{"name": "ONSTOCK", "value": "1"}],
                            "metaRank": [{"name": "ONSTOCK", "value": "1"}],
                        },
                    },
                ]
            },
            preserve_order=True,
        )

    @classmethod
    def prepare_dynstat_count_experiment(cls):
        cls.index.models += [
            Model(hid=800, title="model with price=" + str(100 + i), hyperid=800 + i) for i in range(8)
        ]
        cls.index.offers += [Offer(hid=800, price=100 + i, hyperid=800 + i) for i in range(8)]

    def test_dynstat_count_experiment(self):
        """
        Проверяю, что флаг market_dynstat_top переопределяет кол-во документов для
        подсчёта дин. статистик.
        Задаю market_dynstat_top=5, numdoc=4.
        На 1й странице все 4 документа должны войти в dynstat_top.
        На 2й - только 1.

        Задаю сортировку по цене, чтобы зафиксировать порядок офферов.
        """
        response = self.report.request_json(
            "debug=da&hid=800&place=prime&how=aprice&allow-collapsing=1&numdoc=4"
            "&rearr-factors=market_dynstat_count=5"
        )
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 8,
                    "results": [
                        {
                            "titles": {"raw": "model with price=100"},
                            "debug": {"metaRank": [{"name": "IS_IN_DYNSTAT_TOP", "value": "1"}]},
                        },
                        {
                            "titles": {"raw": "model with price=101"},
                            "debug": {"metaRank": [{"name": "IS_IN_DYNSTAT_TOP", "value": "1"}]},
                        },
                        {
                            "titles": {"raw": "model with price=102"},
                            "debug": {"metaRank": [{"name": "IS_IN_DYNSTAT_TOP", "value": "1"}]},
                        },
                        {
                            "titles": {"raw": "model with price=103"},
                            "debug": {"metaRank": [{"name": "IS_IN_DYNSTAT_TOP", "value": "1"}]},
                        },
                    ],
                }
            },
            preserve_order=True,
        )

        response = self.report.request_json(
            "debug=da&hid=800&place=prime&how=aprice&allow-collapsing=1&numdoc=4&page=2"
            "&rearr-factors=market_dynstat_count=5"
        )
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 8,
                    "results": [
                        {
                            "titles": {"raw": "model with price=104"},
                            "debug": {"metaRank": [{"name": "IS_IN_DYNSTAT_TOP", "value": "1"}]},
                        },
                        {
                            "titles": {"raw": "model with price=105"},
                            "debug": {"metaRank": [{"name": "IS_IN_DYNSTAT_TOP", "value": "0"}]},
                        },
                        {
                            "titles": {"raw": "model with price=106"},
                            "debug": {"metaRank": [{"name": "IS_IN_DYNSTAT_TOP", "value": "0"}]},
                        },
                        {
                            "titles": {"raw": "model with price=107"},
                            "debug": {"metaRank": [{"name": "IS_IN_DYNSTAT_TOP", "value": "0"}]},
                        },
                    ],
                }
            },
            preserve_order=True,
        )

    def test_dynstat_count_experiment__override_with_numdoc(self):
        """
        Проверяю, что для первой страницы в любом случае запросятся статистики
        для всех окументов, даже если market_dynstat_top < numdoc.
        Задаю market_dynstat_top=3, numdoc=5.
        На 1й странице все 5 документов должны войти в dynstat_top.
        На 2й - ни один.

        Задаю сортировку по цене, чтобы зафиксировать порядок офферов.
        """
        response = self.report.request_json(
            "debug=da&hid=800&place=prime&how=aprice&allow-collapsing=1&numdoc=5"
            "&rearr-factors=market_dynstat_count=3"
        )
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 8,
                    "results": [
                        {
                            "titles": {"raw": "model with price=100"},
                            "debug": {"metaRank": [{"name": "IS_IN_DYNSTAT_TOP", "value": "1"}]},
                        },
                        {
                            "titles": {"raw": "model with price=101"},
                            "debug": {"metaRank": [{"name": "IS_IN_DYNSTAT_TOP", "value": "1"}]},
                        },
                        {
                            "titles": {"raw": "model with price=102"},
                            "debug": {"metaRank": [{"name": "IS_IN_DYNSTAT_TOP", "value": "1"}]},
                        },
                        {
                            "titles": {"raw": "model with price=103"},
                            "debug": {"metaRank": [{"name": "IS_IN_DYNSTAT_TOP", "value": "1"}]},
                        },
                        {
                            "titles": {"raw": "model with price=104"},
                            "debug": {"metaRank": [{"name": "IS_IN_DYNSTAT_TOP", "value": "1"}]},
                        },
                    ],
                }
            },
            preserve_order=True,
        )

        response = self.report.request_json(
            "debug=da&hid=800&place=prime&how=aprice&allow-collapsing=1&numdoc=5&page=2"
            "&rearr-factors=market_dynstat_count=3"
        )
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 8,
                    "results": [
                        {
                            "titles": {"raw": "model with price=105"},
                            "debug": {"metaRank": [{"name": "IS_IN_DYNSTAT_TOP", "value": "0"}]},
                        },
                        {
                            "titles": {"raw": "model with price=106"},
                            "debug": {"metaRank": [{"name": "IS_IN_DYNSTAT_TOP", "value": "0"}]},
                        },
                        {
                            "titles": {"raw": "model with price=107"},
                            "debug": {"metaRank": [{"name": "IS_IN_DYNSTAT_TOP", "value": "0"}]},
                        },
                    ],
                }
            },
            preserve_order=True,
        )

        response = self.report.request_json(
            "debug=da&hid=800&place=prime&how=aprice&allow-collapsing=1&numdoc=5&page=2"
            "&rearr-factors=market_dynstat_count=3"
            "&rearr-factors=dyn_stat_by_page_num=1"
        )
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 8,
                    "results": [
                        {
                            "titles": {"raw": "model with price=105"},
                            "debug": {"metaRank": [{"name": "IS_IN_DYNSTAT_TOP", "value": "1"}]},
                        },
                        {
                            "titles": {"raw": "model with price=106"},
                            "debug": {"metaRank": [{"name": "IS_IN_DYNSTAT_TOP", "value": "1"}]},
                        },
                        {
                            "titles": {"raw": "model with price=107"},
                            "debug": {"metaRank": [{"name": "IS_IN_DYNSTAT_TOP", "value": "1"}]},
                        },
                    ],
                }
            },
            preserve_order=True,
        )

    @classmethod
    def prepare_user_sorting_with_doc_pessimization(cls):
        cls.index.models += [
            Model(hid=501, hyperid=50101, title="dynstatTop model with min_price=100"),
            Model(hid=501, hyperid=50102, title="dynstatTop model with min_price=101"),
            Model(hid=501, hyperid=50103, title="dynstatTop empty model with min_price=140"),
            Model(hid=501, hyperid=50104, title="dynstatTop empty model with min_price=141"),
            Model(hid=501, hyperid=50105, title="dynstatTop empty model with min_price=142"),
            Model(hid=501, hyperid=50106, title="dynstatTop model with min_price=160"),
            Model(hid=501, hyperid=50107, title="empty model"),
        ]
        cls.index.offers += [
            Offer(hyperid=50101, price=100),
            Offer(hyperid=50102, price=101),
            Offer(hyperid=50106, price=160),
        ]
        cls.index.regional_models += [
            RegionalModel(hyperid=50103, price_min=140, offers=1),
            RegionalModel(hyperid=50104, price_min=141, offers=1),
            RegionalModel(hyperid=50105, price_min=142, offers=1),
        ]
        cls.index.models += [
            Model(hid=501, hyperid=50110 + i, title="model with min_price=" + str(200 + i)) for i in range(7)
        ]
        cls.index.offers += [Offer(hid=501, hyperid=50110 + i, price=200 + i) for i in range(7)]

    def test_user_sorting_with_doc_pessimization(self):
        """
        Проверяем стабильность пользовательских сортировок (по цене, по скидке),
        если после запроса за дин. статистиками часть документов пессимизировалась.
        Документы не должны прыгать со страницы на страницу.

        Ожидаемый порядок:
        - непустые документы, попавшие в топ дин. статистик
        - непустые документы, не попавшие в топ дин. статистик
        - пустые документы, попавшие в топ и оказавшиеся пустыми после запроса дин. статистик
        - пустые документы
        """
        request = (
            "place=prime&hid=501&how=aprice&allow-collapsing=1&numdoc=4&debug=da"
            "&rearr-factors=market_dynstat_count=6"
        )
        response = self.report.request_json(request + "&page=1")
        self.assertFragmentIn(
            response,
            {
                "total": 11,  # для 1й страницы запрашиваем numdoc(=4)*page(=1)+market_dynstat_count(=6)+1
                "results": [
                    {
                        "titles": {"raw": "dynstatTop model with min_price=100"},
                        "debug": {"metaRank": [{"name": "IS_IN_DYNSTAT_TOP", "value": "1"}]},
                    },
                    {
                        "titles": {"raw": "dynstatTop model with min_price=101"},
                        "debug": {"metaRank": [{"name": "IS_IN_DYNSTAT_TOP", "value": "1"}]},
                    },
                    {
                        "titles": {"raw": "dynstatTop model with min_price=160"},
                        "debug": {"metaRank": [{"name": "IS_IN_DYNSTAT_TOP", "value": "1"}]},
                    },
                    {
                        "titles": {"raw": "model with min_price=200"},
                        "debug": {"metaRank": [{"name": "IS_IN_DYNSTAT_TOP", "value": "0"}]},
                    },
                ],
            },
            preserve_order=True,
        )
        self.assertEquals(len(response["search"]["results"]), 4)

        response = self.report.request_json(request + "&page=2")
        self.assertFragmentIn(
            response,
            {
                # Запрашиваем numdoc*page+market_dynstat_count+1
                # Так как всего документов 14, то для всех страниц, начиная со 2й, total=14
                "total": 14,
                "results": [
                    {
                        "titles": {"raw": "model with min_price=201"},
                        "debug": {"metaRank": [{"name": "IS_IN_DYNSTAT_TOP", "value": "0"}]},
                    },
                    {
                        "titles": {"raw": "model with min_price=202"},
                        "debug": {"metaRank": [{"name": "IS_IN_DYNSTAT_TOP", "value": "0"}]},
                    },
                    {
                        "titles": {"raw": "model with min_price=203"},
                        "debug": {"metaRank": [{"name": "IS_IN_DYNSTAT_TOP", "value": "0"}]},
                    },
                    {
                        "titles": {"raw": "model with min_price=204"},
                        "debug": {"metaRank": [{"name": "IS_IN_DYNSTAT_TOP", "value": "0"}]},
                    },
                ],
            },
            preserve_order=True,
        )
        self.assertEquals(len(response["search"]["results"]), 4)

        response = self.report.request_json(request + "&page=3")
        self.assertFragmentIn(
            response,
            {
                "total": 14,
                "results": [
                    {
                        "titles": {"raw": "model with min_price=205"},
                        "debug": {"metaRank": [{"name": "IS_IN_DYNSTAT_TOP", "value": "0"}]},
                    },
                    {
                        "titles": {"raw": "model with min_price=206"},
                        "debug": {"metaRank": [{"name": "IS_IN_DYNSTAT_TOP", "value": "0"}]},
                    },
                    {
                        "titles": {"raw": "dynstatTop empty model with min_price=140"},
                        "debug": {"metaRank": [{"name": "IS_IN_DYNSTAT_TOP", "value": "1"}]},
                    },
                    {
                        "titles": {"raw": "dynstatTop empty model with min_price=141"},
                        "debug": {"metaRank": [{"name": "IS_IN_DYNSTAT_TOP", "value": "1"}]},
                    },
                ],
            },
            preserve_order=True,
        )
        self.assertEquals(len(response["search"]["results"]), 4)

        response = self.report.request_json(request + "&page=4")
        self.assertFragmentIn(
            response,
            {
                "total": 14,
                "results": [
                    {
                        "titles": {"raw": "dynstatTop empty model with min_price=142"},
                        "debug": {"metaRank": [{"name": "IS_IN_DYNSTAT_TOP", "value": "1"}]},
                    },
                    {
                        "titles": {"raw": "empty model"},
                        "debug": {"metaRank": [{"name": "IS_IN_DYNSTAT_TOP", "value": "0"}]},
                    },
                ],
            },
            preserve_order=True,
        )
        self.assertEquals(len(response["search"]["results"]), 2)

    @classmethod
    def prepare_cpa_only_in_model_statistic(cls):
        cls.index.models += [Model(hyperid=112233, title="cpa only toy", hid=123)]

        cls.index.offers += [
            Offer(hyperid=112233, fesh=1, cpa=Offer.CPA_REAL, price=100000),
            Offer(hyperid=112233, fesh=2, cpa=Offer.CPA_REAL, price=105000),
            Offer(hyperid=112233, fesh=3, price=90000),
        ]

    def test_cpa_only_in_model_statistic(self):
        # 3 оффера, cpc + cpa. Мин цена у cpc
        response = self.report.request_json(
            'place=prime&hid=123&rearr-factors=market_cpa_only_fix_model_statistics=0&debug=da'
        )
        self.assertFragmentIn(
            response, T.ExpectResults().model('cpa only toy', offers=3, min=90000, max=105000).results()
        )

        # 3 оффера, но используем мин. цену cpa
        response = self.report.request_json(
            'place=prime&hid=123&rearr-factors=market_cpa_only_fix_model_statistics=1&debug=da'
        )
        self.assertFragmentIn(
            response, T.ExpectResults().model('cpa only toy', offers=3, min=100000, max=105000).results()
        )


if __name__ == '__main__':
    main()

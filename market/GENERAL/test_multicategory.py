#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    CpaCategory,
    CpaCategoryType,
    DeliveryBucket,
    DeliveryOption,
    GLParam,
    GLType,
    GLValue,
    HyperCategory,
    HyperCategoryType,
    Model,
    Offer,
    OverallModel,
    Promo,
    PromoType,
    RegionalDelivery,
    RegionalModel,
    Shop,
)
from core.testcase import TestCase, main
from core.matcher import Absent, GreaterEq
from core.types.autogen import Const
from datetime import datetime


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['market_write_category_redirect_features=20']
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']

        # RANDX randomizing is disabled because these tests don't work with it. See MARKETOUT-21319
        cls.disable_randx_randomize()

        cls.index.hypertree += [
            HyperCategory(hid=101, output_type=HyperCategoryType.GURU),
            HyperCategory(hid=102, output_type=HyperCategoryType.GURU),
            HyperCategory(hid=103, output_type=HyperCategoryType.GURU),
        ]

        cls.index.cpa_categories += [
            CpaCategory(hid=101, regions=[213], cpa_type=CpaCategoryType.CPA_WITH_CPC_PESSIMIZATION),
            CpaCategory(hid=102, regions=[213], cpa_type=CpaCategoryType.CPA_WITH_CPC_PESSIMIZATION),
            CpaCategory(hid=103, regions=[213], cpa_type=CpaCategoryType.CPA_WITH_CPC_PESSIMIZATION),
        ]

        cls.index.gltypes += [
            GLType(
                param_id=501,
                hid=101,
                gltype=GLType.ENUM,
                values=[GLValue(value_id=1, text='cheburashka'), GLValue(value_id=2, text='gena')],
            ),
            GLType(
                param_id=501,
                hid=102,
                gltype=GLType.ENUM,
                values=[GLValue(value_id=1, text='cheburashka'), GLValue(value_id=2, text='gena')],
            ),
        ]

        cls.index.models += [
            Model(hyperid=201, hid=101, title='Cheburashka picture 1', glparams=[GLParam(param_id=501, value=1)]),
            Model(hyperid=202, hid=101, title='Cheburashka picture 2', glparams=[GLParam(param_id=501, value=1)]),
            Model(hyperid=203, hid=101, title='Crocodile Gena picture 1', glparams=[GLParam(param_id=501, value=2)]),
            Model(hyperid=204, hid=101, title='Crocodile Gena picture 2', glparams=[GLParam(param_id=501, value=2)]),
        ]

        cls.index.models += [
            Model(hyperid=211, hid=102, title='Cheburashka toy 1', glparams=[GLParam(param_id=501, value=1)]),
            Model(hyperid=212, hid=102, title='Cheburashka toy 2', glparams=[GLParam(param_id=501, value=1)]),
            Model(hyperid=213, hid=102, title='Crocodile Gena toy 1', glparams=[GLParam(param_id=501, value=2)]),
            Model(hyperid=214, hid=102, title='Crocodile Gena toy 2', glparams=[GLParam(param_id=501, value=2)]),
        ]

        cls.index.shops += [Shop(fesh=999, cpa=Shop.CPA_REAL, is_cpa_partner=True, priority_region=213)]

        cls.index.offers += [
            Offer(
                hyperid=201,
                discount=15,
                promo=Promo(
                    promo_type=PromoType.N_PLUS_ONE,
                    start_date=datetime(1980, 1, 1),
                    end_date=datetime(2050, 1, 1),
                    key='xMpCOKC5I4INzFCab3WEmw',
                    required_quantity=3,
                    free_quantity=34,
                ),
                delivery_options=[DeliveryOption(price=0, day_to=3)],
                fesh=999,
                cpa=Offer.CPA_REAL,
            ),
            Offer(hyperid=202, fesh=999),
            Offer(hyperid=203, fesh=999),
            Offer(hyperid=204, fesh=999),
            Offer(hyperid=211, fesh=999),
            Offer(hyperid=212, fesh=999),
            Offer(hyperid=213, fesh=999),
            Offer(hyperid=214, fesh=999),
            Offer(
                hid=101,
                glparams=[GLParam(param_id=501, value=1)],
                title="Hat Cheburashka",
                fesh=999,
                cpa=Offer.CPA_REAL,
            ),
        ]

    def test_output_format(self):
        """
        Делаем запрос с 2 категориями и 1 параметром.
        Проверяем, что в ответе есть нужные поля
        Проверяем, что все модели схлопнулись + в выдаче одна виртуальная модель из оффера Hat Cheburashka
        """
        unified_off_flags = '&rearr-factors=market_dsbs_tariffs=0;market_unified_tariffs=0'
        req = 'place=multi_category&text=&glfilter=501:1&hid=101&hid=102&rids=213&rearr-factors=market_do_not_split_promo_filter=1'
        offers_out = {"count": 1, "items": Absent()}
        response = self.report.request_json(req + unified_off_flags)
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 5,
                    "totalOffers": 1,
                    "totalModels": 4,
                    "results": [
                        {"entity": "product", "titles": {"raw": "Cheburashka toy 2"}, "id": 212, "offers": offers_out},
                        {"entity": "product", "titles": {"raw": "Cheburashka toy 1"}, "id": 211, "offers": offers_out},
                        {
                            "entity": "product",
                            "titles": {"raw": "Cheburashka picture 2"},
                            "id": 202,
                            "offers": offers_out,
                        },
                        {
                            "entity": "product",
                            "titles": {"raw": "Cheburashka picture 1"},
                            "id": 201,
                            "offers": offers_out,
                        },
                        {
                            "entity": "product",
                            "id": GreaterEq(Const.VMID_START),
                            "offers": {
                                "count": 1,
                                "items": [
                                    {"titles": {"raw": "Hat Cheburashka"}, "marketSku": GreaterEq(Const.VMID_START)}
                                ],
                            },
                        },
                    ],
                },
                "intents": [
                    {
                        "defaultOrder": 0,
                        "ownCount": 3,
                        "category": {
                            "name": "HID-101",
                            "hid": 101,
                        },
                    },
                    {
                        "defaultOrder": 0,
                        "ownCount": 2,
                        "category": {
                            "name": "HID-102",
                            "hid": 102,
                        },
                    },
                ],
                "sorts": [
                    {"text": "по популярности"},
                    {"text": "по цене", "options": [{"id": "aprice", "type": "asc"}, {"id": "dprice", "type": "desc"}]},
                ],
                "filters": [
                    {"id": "glprice"},
                    {"id": "manufacturer_warranty"},
                    {"id": "onstock"},
                    {"id": "filter-promo-or-discount"},
                    {"id": "qrfrom"},
                    {"id": "free-delivery"},
                    {"id": "offer-shipping"},
                    {"id": "delivery-interval"},
                    {"id": "fesh"},
                ],
            },
        )

        self.assertFragmentNotIn(response, {"filters": [{"id": "home_region"}]})

    @classmethod
    def prepare_test_mixing(cls):
        """
        Делаем категорию 201 самой популярной
        Популярность моделей тоже контроллируем, чтобы получить контроллируемое ранжирование:
        308 - я внутри категории самая популярная, до 301 убывает
        """

        for hid in range(201, 206):
            cls.index.hypertree += [HyperCategory(hid=hid, output_type=HyperCategoryType.GURU)]

            cls.index.gltypes += [
                GLType(
                    param_id=601,
                    hid=hid,
                    gltype=GLType.ENUM,
                    values=[GLValue(value_id=1, text='rosomaha'), GLValue(value_id=2, text='alien')],
                )
            ]

            cls.index.models += [
                Model(
                    hyperid=hid * 1000 + 301,
                    hid=hid,
                    title='Rosomaha toy hid %d' % hid,
                    glparams=[GLParam(param_id=601, value=1)],
                    model_clicks=300 if hid == 201 else 30,
                ),
                Model(
                    hyperid=hid * 1000 + 302,
                    hid=hid,
                    title='Rosomaha photo hid %d' % hid,
                    glparams=[GLParam(param_id=601, value=1)],
                    model_clicks=400 if hid == 201 else 40,
                ),
                Model(
                    hyperid=hid * 1000 + 303,
                    hid=hid,
                    title='Alien toy hid %d' % hid,
                    glparams=[GLParam(param_id=601, value=2)],
                    model_clicks=500 if hid == 201 else 50,
                ),
                Model(
                    hyperid=hid * 1000 + 304,
                    hid=hid,
                    title='Alien photo hid %d' % hid,
                    glparams=[GLParam(param_id=601, value=2)],
                    model_clicks=600 if hid == 201 else 60,
                ),
                Model(
                    hyperid=hid * 1000 + 305,
                    hid=hid,
                    title='Rosomaha plastic hid %d' % hid,
                    glparams=[GLParam(param_id=601, value=1)],
                    model_clicks=700 if hid == 201 else 70,
                ),
                Model(
                    hyperid=hid * 1000 + 306,
                    hid=hid,
                    title='Rosomaha knife hid %d' % hid,
                    glparams=[GLParam(param_id=601, value=1)],
                    model_clicks=800 if hid == 201 else 80,
                ),
                Model(
                    hyperid=hid * 1000 + 307,
                    hid=hid,
                    title='Rosomaha hid %d' % hid,
                    glparams=[GLParam(param_id=601, value=1)],
                    model_clicks=900 if hid == 201 else 90,
                ),
                Model(
                    hyperid=hid * 1000 + 308,
                    hid=hid,
                    title='Rosomaha X hid %d' % hid,
                    glparams=[GLParam(param_id=601, value=1)],
                    model_clicks=1000 if hid == 201 else 100,
                ),
            ]

            for i in range(2):
                cls.index.offers += [
                    Offer(hyperid=hid * 1000 + 301, price=hid * 100 + i * 10 + 301, ts=1000 * hid + i + 10),
                    Offer(hyperid=hid * 1000 + 302, price=hid * 100 + i * 20 + 302, ts=1000 * hid + i + 20),
                    Offer(hyperid=hid * 1000 + 303, price=hid * 100 + i * 20 + 303, ts=1000 * hid + i + 30),
                    Offer(hyperid=hid * 1000 + 304, price=hid * 100 + 304, ts=1000 * hid + i + 40),
                    Offer(
                        hid=hid,
                        glparams=[GLParam(param_id=601, value=1)],
                        title="Rosomaha suit",
                        ts=1000 * hid + i + 50,
                        price=hid * 100 + 500,
                    ),
                    Offer(hyperid=hid * 1000 + 305, price=hid * 100 + 305, ts=1000 * hid + i + 60),
                    Offer(hyperid=hid * 1000 + 306, price=hid * 100 + 306, ts=1000 * hid + i + 70),
                    Offer(hyperid=hid * 1000 + 307, price=hid * 100 + 307, ts=1000 * hid + i + 80),
                    Offer(hyperid=hid * 1000 + 308, price=hid * 100 + 308, ts=1000 * hid + i + 90),
                ]

    def test_default_sorting(self):
        """
        Товары из категорий замешаны через одну
        """
        # FIXME: https://st.yandex-team.ru/MARKETOUT-19042
        response = self.report.request_json(
            'place=multi_category&text=&glfilter=601:1&hid=201&hid=202&hid=203&hid=204&hid=205&local-offers-first=0'
        )

        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"entity": "product", "categories": [{"id": 201}], "titles": {"raw": "Rosomaha X hid 201"}},
                    {"entity": "product", "categories": [{"id": 202}], "titles": {"raw": "Rosomaha X hid 202"}},
                    {"entity": "product", "categories": [{"id": 203}], "titles": {"raw": "Rosomaha X hid 203"}},
                    {"entity": "product", "categories": [{"id": 204}], "titles": {"raw": "Rosomaha X hid 204"}},
                    {"entity": "product", "categories": [{"id": 205}], "titles": {"raw": "Rosomaha X hid 205"}},
                    {"entity": "product", "categories": [{"id": 201}], "titles": {"raw": "Rosomaha hid 201"}},
                    {"entity": "product", "categories": [{"id": 202}], "titles": {"raw": "Rosomaha hid 202"}},
                    {"entity": "product", "categories": [{"id": 203}], "titles": {"raw": "Rosomaha hid 203"}},
                    {"entity": "product", "categories": [{"id": 204}], "titles": {"raw": "Rosomaha hid 204"}},
                    {"entity": "product", "categories": [{"id": 205}], "titles": {"raw": "Rosomaha hid 205"}},
                ]
            },
            preserve_order=True,
        )

    def test_price_sorting(self):
        """
        Сортировка по цене. Проверяем, что все отосортировалось по цене, не взирая на смешивание
        """
        response = self.report.request_json(
            'place=multi_category&text=&glfilter=601:1&hid=201&hid=202&hid=203&hid=204&hid=205&local-offers-first=0&how=aprice'
        )

        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"entity": "product", "categories": [{"id": 201}], "prices": {"min": "20401"}},
                    {"entity": "product", "categories": [{"id": 201}], "prices": {"min": "20402"}},
                    {"entity": "product", "categories": [{"id": 201}], "prices": {"min": "20405"}},
                    {"entity": "product", "categories": [{"id": 201}], "prices": {"min": "20406"}},
                    {"entity": "product", "categories": [{"id": 201}], "prices": {"min": "20407"}},
                    {"entity": "product", "categories": [{"id": 201}], "prices": {"min": "20408"}},
                    {"entity": "product", "categories": [{"id": 202}], "prices": {"min": "20501"}},
                    {"entity": "product", "categories": [{"id": 202}], "prices": {"min": "20502"}},
                    {"entity": "product", "categories": [{"id": 202}], "prices": {"min": "20505"}},
                    {"entity": "product", "categories": [{"id": 202}], "prices": {"min": "20506"}},
                ]
            },
            preserve_order=True,
        )

    @classmethod
    def prepare_local_offers_first(cls):
        """
        Готовим три категории:
         2 - с товарами в локальном регионе,
         1 - с товарами в другом.

        301 - самая популярная категория
        Проверяем, что все, что сверху отранжировалось вниз, если включена черта,
        и что все три категории равномерно распределены в выдаче между 301-й
        """

        cls.index.shops += [Shop(fesh=101, priority_region=213), Shop(fesh=102, priority_region=19, regions=[213])]

        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=211,
                fesh=102,
                carriers=[99],
                regional_options=[
                    RegionalDelivery(
                        rid=213, options=[DeliveryOption(price=400, day_from=1, day_to=3, order_before=23)]
                    )
                ],
            ),
        ]

        for hid in range(301, 304):
            cls.index.hypertree += [HyperCategory(hid=hid, output_type=HyperCategoryType.GURU)]

            cls.index.gltypes += [
                GLType(
                    param_id=701,
                    hid=hid,
                    gltype=GLType.ENUM,
                    values=[GLValue(value_id=1, text='rosomaha'), GLValue(value_id=2, text='alien')],
                )
            ]

            cls.index.models += [
                Model(
                    hyperid=hid * 1000 + 1,
                    hid=hid,
                    title='Rosomaha toy hid %d' % hid,
                    glparams=[GLParam(param_id=701, value=1)],
                    model_clicks=9000 if hid == 301 else 900,
                ),
                Model(
                    hyperid=hid * 1000 + 2,
                    hid=hid,
                    title='Rosomaha photo hid %d' % hid,
                    glparams=[GLParam(param_id=701, value=1)],
                    model_clicks=10000 if hid == 301 else 1000,
                ),
            ]

            if hid == 303:
                cls.index.offers += [
                    Offer(hyperid=hid * 1000 + 1, price=hid * 100 + 500, delivery_buckets=[211], fesh=102),
                    Offer(hyperid=hid * 1000 + 1, price=hid * 100 + 3000, delivery_buckets=[211], fesh=102),
                ]
            else:
                for i in range(2):
                    cls.index.offers += [
                        Offer(
                            hyperid=hid * 1000 + 1,
                            price=hid * 100 + 500,
                            delivery_options=[DeliveryOption(day_to=0, price=299)],
                            fesh=101,
                        ),
                        Offer(
                            hyperid=hid * 1000 + 2,
                            price=hid * 100 + 3000,
                            delivery_options=[DeliveryOption(day_to=0, price=199)],
                            fesh=101,
                        ),
                    ]

        cls.index.regional_models += [RegionalModel(hyperid=301, offers=5)]

    def test_local_offers_first(self):
        """
        303 категория - есть только в регионах
        Т.к. выдача пересортируется в рамках одной страницы, проверяем на странице из 4 - х элементов,
        без local_offers_first - есть 2 модели 301 категории, по одной 302 и 303
        с local_offers_first - есть 2 модели 301 категории и две 302 302
        """
        response = self.report.request_json(
            'place=multi_category&text=&glfilter=701:1&hid=301&hid=302&hid=303&rids=213&local-offers-first=1&numdoc=4'
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"entity": "product", "categories": [{"id": 301}]},
                    {"entity": "product", "categories": [{"id": 302}]},
                    {"entity": "product", "categories": [{"id": 301}]},
                    {"entity": "product", "categories": [{"id": 302}]},
                ]
            },
            preserve_order=True,
        )

        # без черты все впереимешку
        response = self.report.request_json(
            'place=multi_category&text=&glfilter=701:1&hid=301&hid=302&hid=303&rids=213&local-offers-first=0&numdoc=4'
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"entity": "product", "categories": [{"id": 301}]},
                    {"entity": "product", "categories": [{"id": 302}]},
                    {"entity": "product", "categories": [{"id": 301}]},
                    {"entity": "product", "categories": [{"id": 303}]},
                ]
            },
            preserve_order=True,
        )

    @classmethod
    def prepare_category_hierarchy(cls):
        cls.index.hypertree += [
            HyperCategory(
                hid=1000,
                output_type=HyperCategoryType.GURU,
                children=[
                    HyperCategory(
                        hid=1001,
                        output_type=HyperCategoryType.GURU,
                        children=[
                            HyperCategory(hid=1002, output_type=HyperCategoryType.GURU),
                            HyperCategory(hid=1003, output_type=HyperCategoryType.GURU),
                        ],
                    ),
                    HyperCategory(hid=1004, output_type=HyperCategoryType.GURU),
                ],
            ),
            HyperCategory(hid=1005, output_type=HyperCategoryType.GURU),
        ]

        cls.index.gltypes += [
            GLType(
                param_id=5001,
                hid=hid,
                gltype=GLType.ENUM,
                values=[GLValue(value_id=1, text='cheburashka'), GLValue(value_id=2, text='gena')],
            )
            for hid in range(1000, 1006)
        ]

        cls.index.models += [
            Model(
                hyperid=1000 + hid, hid=hid, title='Cheburashka picture 1', glparams=[GLParam(param_id=5001, value=1)]
            )
            for hid in range(1000, 1006)
        ]

        cls.index.offers += [Offer(hyperid=hyperid) for hyperid in range(2000, 2006)]

    def test_category_tree_output(self):
        """
        задаем мультикатегорийный запрос с разными категориями и проверяем, что дерево построилось корректно
        """
        response = self.report.request_json(
            'place=multi_category&text=&glfilter=5001:1&hid=1002&hid=1003&hid=1004&hid=1005'
        )
        self.assertFragmentIn(
            response,
            {
                "intents": [
                    {
                        "category": {"hid": 1000},
                        "intents": [
                            {
                                "category": {"hid": 1001},
                                "intents": [{"category": {"hid": 1002}}, {"category": {"hid": 1003}}],
                            },
                            {
                                "category": {"hid": 1004},
                            },
                        ],
                    },
                    {
                        "category": {"hid": 1005},
                    },
                ]
            },
        )

    @classmethod
    def prepare_adult_warning(cls):
        """
        Готовим три категории:
         2 - с товарами в локальном регионе,
         1 - с товарами в другом.

        301 - самая популярная категория
        Проверяем, что все, что сверху отранжировалось вниз, если включена черта,
        и что все три категории равномерно распределены в выдаче между 301-й
        """
        for hid in range(401, 404):
            cls.index.hypertree += [HyperCategory(hid=hid, output_type=HyperCategoryType.GURU)]

            gl_param_id = 801

            cls.index.gltypes += [
                GLType(
                    param_id=801,
                    hid=hid,
                    gltype=GLType.ENUM,
                    values=[GLValue(value_id=1, text='vur'), GLValue(value_id=2, text='ur')],
                )
            ]

            # Rossomaha toy hid 401 будет иметь adult=1
            cls.index.models += [
                Model(
                    hyperid=hid * 1000 + 1,
                    hid=hid,
                    title='Rosomaha toy hid %d' % hid,
                    glparams=[GLParam(param_id=gl_param_id, value=1)],
                    model_clicks=9000 if hid == 401 else 900,
                ),
                Model(
                    hyperid=hid * 1000 + 2,
                    hid=hid,
                    title='Rosomaha photo hid %d' % hid,
                    glparams=[GLParam(param_id=gl_param_id, value=1)],
                    model_clicks=10000 if hid == 401 else 1000,
                ),
            ]

            if hid == 401:
                cls.index.overall_models += [
                    OverallModel(hyperid=hid * 1000 + 1, price_med=hid * 100 + 500, is_adult=True)
                ]

            for i in range(2):
                cls.index.offers += [
                    Offer(
                        hyperid=hid * 1000 + 1,
                        price=hid * 100 + 500,
                        delivery_options=[DeliveryOption(day_to=0, price=299)],
                        adult=True if hid == 401 else False,
                    ),
                    Offer(
                        hyperid=hid * 1000 + 2,
                        price=hid * 100 + 3000,
                        delivery_options=[DeliveryOption(day_to=0, price=199)],
                    ),
                ]

        cls.index.regional_models += [RegionalModel(hyperid=401, offers=5)]

    def test_adult(self):
        # FIXME: https://st.yandex-team.ru/MARKETOUT-19042

        # c adult = 1, и есть adult и не-adult предложения
        response = self.report.request_json('place=multi_category&text=&glfilter=801:1&hid=401&hid=402&hid=403&adult=1')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"entity": "product", "titles": {"raw": "Rosomaha photo hid 401"}},
                    {"entity": "product", "titles": {"raw": "Rosomaha photo hid 402"}},
                    {"entity": "product", "titles": {"raw": "Rosomaha photo hid 403"}},
                    {"entity": "product", "titles": {"raw": "Rosomaha toy hid 401"}},
                    {"entity": "product", "titles": {"raw": "Rosomaha toy hid 402"}},
                    {"entity": "product", "titles": {"raw": "Rosomaha toy hid 403"}},
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # c adult = 0, только не-adult предложения
        response = self.report.request_json('place=multi_category&text=&glfilter=801:1&hid=401&hid=402&hid=403&adult=0')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"entity": "product", "titles": {"raw": "Rosomaha photo hid 401"}},
                    {"entity": "product", "titles": {"raw": "Rosomaha photo hid 402"}},
                    {"entity": "product", "titles": {"raw": "Rosomaha photo hid 403"}},
                    # {"entity": "product", "titles": {"raw": "Rosomaha toy hid 401"}}, skipped as adult
                    {"entity": "product", "titles": {"raw": "Rosomaha toy hid 402"}},
                    {"entity": "product", "titles": {"raw": "Rosomaha toy hid 403"}},
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

    @classmethod
    def prepare_search_by_literals(cls):
        """
        Некоторые документы будут иметь как поисковый литерал лицензиар/франшиза/персонаж,
        так и соотв. gl-параметр.
        Некоторые -  что-то одно, и не будут найдены (для теста, такого в реальности быть не должно)
        """
        cls.index.hypertree += [
            HyperCategory(hid=104, output_type=HyperCategoryType.GURU),
        ]

        cls.index.gltypes += [
            GLType(
                param_id=14020987,
                xslname='hero_global',
                hid=104,
                gltype=GLType.ENUM,
                values=[
                    GLValue(value_id=1, text='Angry Birds'),
                    GLValue(value_id=2, text='Тransformers'),
                ],
            ),
            GLType(
                param_id=15060326,
                xslname='licensor',
                hid=104,
                gltype=GLType.ENUM,
                values=[
                    GLValue(value_id=1, text='Disney'),
                    GLValue(value_id=2, text='FIFA'),
                ],
            ),
            GLType(
                param_id=15086295,
                xslname='pers_model',
                hid=104,
                gltype=GLType.ENUM,
                values=[
                    GLValue(value_id=1, text='R2D2'),
                    GLValue(value_id=2, text='Забивака'),
                ],
            ),
        ]

        cls.index.models += [
            # Модель будет найдена по фильтрам лицензиар/франшиза/персонаж = 1
            Model(
                hyperid=501,
                hid=104,
                title='model501',
                hero_global=1,
                licensor=1,
                pers_model=1,
                glparams=[
                    GLParam(param_id=14020987, value=1),
                    GLParam(param_id=15060326, value=1),
                    GLParam(param_id=15086295, value=1),
                ],
            ),
            # франшиза - нет поискового литерала
            Model(
                hyperid=502,
                hid=104,
                title='model502',
                licensor=1,
                pers_model=1,
                glparams=[
                    GLParam(param_id=14020987, value=1),
                    GLParam(param_id=15060326, value=1),
                    GLParam(param_id=15086295, value=1),
                ],
            ),
            # лицензиар - неверное значение поискового литерала
            Model(
                hyperid=503,
                hid=104,
                title='model503',
                hero_global=1,
                licensor=2,
                pers_model=1,
                glparams=[
                    GLParam(param_id=14020987, value=1),
                    GLParam(param_id=15060326, value=1),
                    GLParam(param_id=15086295, value=1),
                ],
            ),
            # персонаж - нет gl-параметра
            Model(
                hyperid=504,
                hid=104,
                title='model504',
                hero_global=1,
                licensor=1,
                pers_model=1,
                glparams=[GLParam(param_id=14020987, value=1), GLParam(param_id=15060326, value=1)],
            ),
        ]

    def test_search_by_literals(self):
        """
        Проверяем, что при фильтрации по gl-параметрам лицензиар/франшиза/персонаж
        репорт дополнительно включает фильтрацию по соотв. поисковым литералам.
        Это нужно, чтобы не терять данные на больших запросах из-за прюнинга (см. MARKETOUT-19787)
        """
        # Ищем по франшизе (она же тема)
        response = self.report.request_json('place=multi_category&glfilter=14020987:1&hid=104')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"entity": "product", "titles": {"raw": "model501"}},
                    {"entity": "product", "titles": {"raw": "model503"}},
                    {"entity": "product", "titles": {"raw": "model504"}},
                ]
            },
            allow_different_len=False,
        )
        # По лицензиару
        response = self.report.request_json('place=multi_category&glfilter=15060326:1&hid=104')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"entity": "product", "titles": {"raw": "model501"}},
                    {"entity": "product", "titles": {"raw": "model502"}},
                    {"entity": "product", "titles": {"raw": "model504"}},
                ]
            },
            allow_different_len=False,
        )
        # По персонажу
        response = self.report.request_json('place=multi_category&glfilter=15086295:1&hid=104')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"entity": "product", "titles": {"raw": "model501"}},
                    {"entity": "product", "titles": {"raw": "model502"}},
                    {"entity": "product", "titles": {"raw": "model503"}},
                ]
            },
            allow_different_len=False,
        )


if __name__ == '__main__':
    main()

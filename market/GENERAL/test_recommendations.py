#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    ClickType,
    GLParam,
    GLType,
    HyperCategory,
    Model,
    Offer,
    Picture,
    Region,
    Shop,
    YamarecCategoryPartition,
    YamarecFeaturePartition,
    YamarecPlace,
    YamarecSettingPartition,
)
from core.testcase import TestCase, main
from core.types.autogen import Const
from core.cpc import Cpc
from core.crypta import CryptaName, CryptaFeature


PROMOTED_CATEGORIES = [
    90555,
    90796,
    90490,
    91491,
    10498025,
    2724669,
    91259,
    90601,
    90569,
    281935,
    91650,
    91013,
    6427100,
    91611,
    6269371,
    91148,
    166068,
    7286125,
    10785222,
    90710,
    8476097,
    90523,
    7070735,
    91664,
    15685457,
    15685787,
    15927546,
    4854062,
    16044621,
]

PROMOTED_CATEGORIES_W = [
    512743,
    90796,
    10682592,
    10682618,
    14808696,
    10682610,
    10682647,
    989023,
    90787,
    10683227,
    10470548,
    91183,
    91184,
    4854062,
    13239503,
    13239527,
    16336734,
    8476099,
    8476097,
    8476101,
    8476098,
    8476102,
]


class T(TestCase):
    """
    Набор тестов для ручки рекомендаций аксессуаров
        MARKETOUT-10152
        MARKETOUT-10265
        MARKETOUT-10266
        MARKETOUT-10272
    """

    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ["market_money_disable_bids=0"]
        cls.settings.rgb_blue_is_cpa = True

        cls.index.regiontree += [
            Region(rid=213, name='Москва'),
            Region(rid=2, name='Санкт-Петербург'),
        ]

        cls.index.yamarec_places += [
            YamarecPlace(
                name=YamarecPlace.Name.PROMOTED_CATEGORIES,
                kind=YamarecPlace.Type.CATEGORY,
                partitions=[
                    YamarecCategoryPartition(category_list=PROMOTED_CATEGORIES, splits=['*']),
                ],
            ),
            YamarecPlace(
                name=YamarecPlace.Name.PROMOTED_CATEGORIES_FEMALE,
                kind=YamarecPlace.Type.CATEGORY,
                partitions=[
                    YamarecCategoryPartition(category_list=PROMOTED_CATEGORIES_W, splits=['*']),
                ],
            ),
        ]

        cls.index.models += [
            Model(hyperid=1, hid=101, title='Samsung Galaxy A3', accessories=[3, 4, 6]),
            Model(hyperid=2, hid=101, title='iPhone 6', accessories=[5, 4]),
            Model(hyperid=3, hid=102, title='Samsung Gear S2', accessories=[]),
            Model(hyperid=4, hid=102, title='Xiaomi Mi Band', accessories=[]),
            Model(hyperid=5, hid=102, title='Apple Watch Sport', accessories=[]),
            Model(hyperid=6, hid=102, title='Alcatel One Touch', accessories=[]),
        ]

        cls.index.offers += [
            Offer(
                hyperid=1,
                hid=101,
                price=50,
                cpa=Offer.CPA_REAL,
                fesh=1001,
                offerid=201,
                title='Samsung Galaxy A3 for 50',
            ),
            Offer(hyperid=2, hid=101, price=90, cpa=Offer.CPA_REAL, fesh=1001, offerid=202, title='iPhone 6 for 90'),
            Offer(
                hyperid=3, hid=102, price=30, cpa=Offer.CPA_REAL, fesh=1001, offerid=203, title='Samsung Gear S2 for 30'
            ),
            Offer(
                hyperid=5,
                hid=102,
                price=80,
                cpa=Offer.CPA_REAL,
                fesh=1001,
                offerid=204,
                title='Apple Watch Sport for 80',
            ),
            Offer(
                hyperid=1,
                hid=101,
                price=150,
                cpa=Offer.CPA_REAL,
                fesh=1002,
                offerid=205,
                title='Samsung Galaxy A3 for 150',
            ),
            Offer(hyperid=2, hid=101, price=190, cpa=Offer.CPA_REAL, fesh=1002, offerid=206, title='iPhone 6 for 190'),
            Offer(
                hyperid=3,
                hid=102,
                price=130,
                cpa=Offer.CPA_REAL,
                fesh=1002,
                offerid=207,
                title='Samsung Gear S2 for 130',
            ),
            Offer(
                hyperid=4, hid=102, price=10, cpa=Offer.CPA_REAL, fesh=1001, offerid=299, title='Xiaomi Mi Band for 10'
            ),
            Offer(
                hyperid=4,
                hid=102,
                price=110,
                cpa=Offer.CPA_REAL,
                fesh=1002,
                offerid=208,
                cbid=123,
                waremd5="qtZDmKlp7DGGgA1BL6erMQ",
                title='Xiaomi Mi Band for 110',
            ),
            Offer(
                hyperid=5,
                hid=102,
                price=180,
                cpa=Offer.CPA_REAL,
                fesh=1002,
                offerid=209,
                waremd5="91t1fTRZw-k-mN2re5A5OA",
                title='Apple Watch Sport for 180',
            ),
        ]

        cls.index.shops += [
            Shop(fesh=1001, name='Good Shop', home_region=Const.ROOT_COUNTRY, cpa=Shop.CPA_REAL),
            Shop(fesh=1002, name='Bad Shop', home_region=Const.ROOT_COUNTRY, cpa=Shop.CPA_REAL),
        ]

    @classmethod
    def prepare_accessories(cls):
        cls.index.gltypes = [GLType(param_id=501, hid=102, gltype=GLType.ENUM, cluster_filter=True)]

        cls.index.models += [
            Model(hyperid=7, hid=101, title='Sony Xperia Z5', accessories=[4, 6]),
            Model(hyperid=8, hid=101, title='Telephone 1.0', accessories=[5]),
            Model(hyperid=9, hid=101, title='Unknown phone', accessories=[]),
            Model(hyperid=10, hid=101, title='Acer Liquid Zest Plus', accessories=[]),
        ]
        cls.index.shops += [
            Shop(fesh=1010, name='The Shop', home_region=Const.ROOT_COUNTRY, cpa=Shop.CPA_REAL),
        ]
        waremd5s = {
            220: 'BH8EPLtKmdLQhLUasgaOnA',
            221: 'bpQ3a9LXZAl_Kz34vaOpSg',
            222: 'V5Y7eJkIdDh0sMeCecijqw',
            223: 'xzFUFhFuAvI1sVcwDnxXPQ',
            225: 'gpQxwKBuLtj5OIlRrvGwTw',
            226: 'yRgmzyBD4j8r4rkCby6Iuw',
            227: 'KXGI8T3GP_pqjgdd7HfoHQ',
        }
        cls.index.offers += [
            Offer(
                hyperid=4,
                hid=102,
                price=120,
                cpa=Offer.CPA_REAL,
                fesh=1010,
                offerid=220,
                waremd5=waremd5s[220],
                title='Xiaomi Mi Band',
                descr='Xiaomi descr',
                glparams=[
                    GLParam(param_id=501, value=1),
                ],
            ),
            Offer(
                hyperid=6,
                hid=102,
                price=70,
                cpa=Offer.CPA_REAL,
                fesh=1010,
                offerid=221,
                waremd5=waremd5s[221],
                title='Alcatel One Touch',
            ),
            Offer(
                hyperid=7,
                hid=101,
                price=65,
                cpa=Offer.CPA_REAL,
                fesh=1010,
                offerid=222,
                waremd5=waremd5s[222],
                title='Sony Xperia Z5',
                rec=[waremd5s[220], waremd5s[221]],
            ),
            Offer(
                hyperid=8,
                hid=101,
                price=262,
                cpa=Offer.CPA_REAL,
                fesh=1010,
                offerid=223,
                waremd5=waremd5s[223],
                title='Telephone 1.0',
            ),
            Offer(
                hyperid=5,
                hid=102,
                price=100,
                cpa=Offer.CPA_REAL,
                fesh=1010,
                offerid=225,
                waremd5=waremd5s[225],
                title='Apple Watch Sport',
                descr='Apple descr',
            ),
            Offer(
                hyperid=10,
                hid=101,
                price=1999,
                cpa=Offer.CPA_REAL,
                fesh=1010,
                offerid=226,
                waremd5=waremd5s[226],
                title='Acer Liquid Zest Plus',
                rec=[
                    waremd5s[225],
                ],
            ),
            Offer(
                hyperid=9,
                hid=101,
                price=11,
                cpa=Offer.CPA_REAL,
                fesh=1010,
                offerid=227,
                waremd5=waremd5s[227],
                title='Unkown phone',
            ),
        ]

    def test_accessories(self):
        """
        Тест новой ручки place=accessories
        Кейс аксессуаров по формуле без аксессуаров из тэга rec
        1. Все источники - formula
        2. Выводятся аксессуары только первого магазина в корзине
        """

        # surprisingly only 2 results
        response = self.report.request_json(
            'place=accessories&pp=143&fesh=1002,1001&hyperid=2,1' '&offerid=206,201&price=190,50'
        )
        cpc209 = Cpc.create_for_offer(
            click_price=10,
            click_price_before_bid_correction=10,
            offer_id='91t1fTRZw-k-mN2re5A5OA',
            hid=102,
            bid=10,
            shop_id=1002,
            minimal_bid=1,
            bid_type="cbid",
            pp=143,
        )

        cpc208 = Cpc.create_for_offer(
            click_price=123,
            click_price_before_bid_correction=123,
            offer_id='qtZDmKlp7DGGgA1BL6erMQ',
            hid=102,
            bid=123,
            shop_id=1002,
            minimal_bid=1,
            bid_type="cbid",
            pp=143,
        )

        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 2,
                    "results": [
                        {
                            "cpc": str(cpc209),
                            "meta": {"recommendationSource": "formula"},
                            "shop": {
                                "id": 1002,
                                "feed": {"offerId": "209"},
                            },
                        },
                        {
                            "cpc": str(cpc208),
                            "shop": {
                                "id": 1002,
                                "feed": {"offerId": "208"},
                            },
                            "meta": {"recommendationSource": "formula"},
                        },
                    ],
                }
            },
            preserve_order=True,
        )

        # no results at all since no accessories from first shop - #1010
        response = self.report.request_json(
            'place=accessories&pp=143&fesh=1010,1001&hyperid=6,1&offerid=bpQ3a9LXZAl_Kz34vaOpSg,201&price=70,50'
        )
        self.assertFragmentIn(response, {"search": {"total": 0, "results": []}})

    def test_accessories_feed(self):
        """
        Тест новой ручки place=accessories
        Кейс выдачи аксессуаров только из тэга rec. (Для одного из оферов есть рекомендации по формуле,
        но они полностью перекрываются тэгом rec, поскольку приоритет у тэга rec, поэтому источник везде - shop)
        1. Все источники - shop
        """

        response = self.report.request_json(
            'place=accessories&pp=143&fesh=1010,1010&hyperid=7,10&offerid=V5Y7eJkIdDh0sMeCecijqw,yRgmzyBD4j8r4rkCby6Iuw&price=65,1999'
        )
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 3,
                    "results": [
                        {
                            "shop": {
                                "id": 1010,
                                "feed": {"offerId": "225"},
                            },
                            "meta": {"recommendationSource": "shop"},
                        },
                        {
                            "shop": {
                                "id": 1010,
                                "feed": {"offerId": "220"},
                            },
                            "meta": {"recommendationSource": "shop"},
                        },
                        {
                            "shop": {
                                "id": 1010,
                                "feed": {"offerId": "221"},
                            },
                            "meta": {"recommendationSource": "shop"},
                        },
                    ],
                },
            },
            preserve_order=True,
        )

    def test_accessories_mixed(self):
        """
        Тест новой ручки place=accessories
        Кейс выдачи аксессуаров из обоих источников: из тэга rec и по формуле. Участвует один магазин
        1. Первый аксессуар - от самого дорогого офера корзины, поэтому в итоговом json formula выше shop в данном случае
        2. Аксессуары конкретного офера сохраняют порядок, в котором они лежат в источнике
        """

        response = self.report.request_json(
            'place=accessories&pp=143&fesh=1010,1010&hyperid=7,8&offerid=V5Y7eJkIdDh0sMeCecijqw,xzFUFhFuAvI1sVcwDnxXPQ&price=65,262'
        )
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 3,
                    "results": [
                        {
                            "shop": {
                                "id": 1010,
                                "feed": {"offerId": "225"},
                            },
                            "meta": {"recommendationSource": "formula"},
                        },
                        {
                            "shop": {
                                "id": 1010,
                                "feed": {"offerId": "220"},
                            },
                            "meta": {"recommendationSource": "shop"},
                        },
                        {
                            "shop": {
                                "id": 1010,
                                "feed": {"offerId": "221"},
                            },
                            "meta": {"recommendationSource": "shop"},
                        },
                    ],
                },
            },
            preserve_order=True,
        )

    def test_accessories_empty(self):
        """
        Тест новой ручки place=accessories
        Кейс пустсой выдачи: нигде не заданы рекомендации для офера
        """

        # unknown phone offer #227 hasnt any recocmmendations
        response = self.report.request_json(
            'place=accessories&pp=143&fesh=1010&hyperid=9&offerid=KXGI8T3GP_pqjgdd7HfoHQ&price=11'
        )
        self.assertFragmentIn(response, {"search": {"total": 0}})

    def test_accessories_filter_second_kind(self):
        """
        Что проверяем: наличие оферных фильтров (параметров второго рода) в выдаче place=accessories
        """
        response = self.report.request_json(
            'place=accessories&pp=143&fesh=1010&hyperid=7&offerid=V5Y7eJkIdDh0sMeCecijqw&price=65'
        )
        self.assertFragmentIn(
            response,
            {
                "filters": [
                    {"id": "501", "kind": 2},
                ]
            },
            preserve_order=True,
        )

    @classmethod
    def prepare_accessories_cpa(cls):
        cls.index.models += [
            Model(hyperid=11, hid=101, title='Xiaomi Redmi Note 3 Pro', accessories=[4]),
        ]
        cls.index.shops += [
            Shop(fesh=1011, name='Shop#1011', home_region=Const.ROOT_COUNTRY, cpa=Shop.CPA_REAL),
        ]
        waremd5s = {
            228: 'rZt32gv6_zQKoq7OqTXqeQ',
            229: 'AHZO1SOUQX-bEaFqMBPdOQ',
        }
        cls.index.offers += [
            Offer(
                hyperid=11,
                hid=101,
                price=156,
                cpa=Offer.CPA_REAL,
                fesh=1011,
                offerid=228,
                waremd5=waremd5s[228],
                title='Xiaomi Redmi Note 3 Pro',
                rec=[
                    waremd5s[229],
                ],
            ),
            Offer(
                hyperid=4,
                hid=102,
                price=112,
                cpa=Offer.CPA_NO,
                fesh=1011,
                offerid=229,
                waremd5=waremd5s[229],
                title='Xiaomi Mi Band - non-cpa offer',
            ),
        ]

    def test_accessories_cpa(self):
        """
        Тест новой ручки place=accessories
        Выдаём только cpa-офферы
        """
        response = self.report.request_json(
            'place=accessories&pp=143&fesh=1011&hyperid=11&offerid=rZt32gv6_zQKoq7OqTXqeQ&price=156'
        )
        self.assertFragmentIn(response, {"search": {"total": 0}})

    def test_accessories_basket_filter(self):
        """
        Тест новой ручки place=accessories
        Проверка того, что в рекомендациях нет оферов из корзины. По некоторому офферу в корзине получаем 2 аксессуара,
        затем поочерёдно добавляем аксессуары в запрос (в корзину) и наблюдаем исключение добавленного в запрос из выдачи
        """

        # all accessories
        response = self.report.request_json(
            'place=accessories&pp=143&fesh=1010&hyperid=7&offerid=V5Y7eJkIdDh0sMeCecijqw&price=65'
        )
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 2,
                    "results": [
                        {
                            "shop": {
                                "feed": {"offerId": "220"},
                            },
                        },
                        {
                            "shop": {
                                "feed": {"offerId": "221"},
                            },
                        },
                    ],
                },
            },
            preserve_order=False,
        )

        # exclude first
        response = self.report.request_json(
            'place=accessories&pp=143&fesh=1010,1010&hyperid=7,4&offerid=V5Y7eJkIdDh0sMeCecijqw,BH8EPLtKmdLQhLUasgaOnA&price=65,120'
        )
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 1,
                    "results": [
                        {
                            "shop": {
                                "feed": {"offerId": "221"},
                            },
                        },
                    ],
                },
            },
            preserve_order=False,
        )

        # exclude second
        response = self.report.request_json(
            'place=accessories&pp=143&fesh=1010,1010&hyperid=7,6&offerid=V5Y7eJkIdDh0sMeCecijqw,bpQ3a9LXZAl_Kz34vaOpSg&price=65,70'
        )
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 1,
                    "results": [
                        {
                            "shop": {
                                "feed": {"offerId": "220"},
                            },
                        },
                    ],
                },
            },
            preserve_order=False,
        )

    def test_accessories_content(self):
        """
        Тест новой ручки place=accessories
        Проверка результирующих данных в выдаче: на месте цена, описание офера и название магазина
        """
        response = self.report.request_json(
            'place=accessories&pp=143&fesh=1010,1010&hyperid=7,8&offerid=V5Y7eJkIdDh0sMeCecijqw,xzFUFhFuAvI1sVcwDnxXPQ&price=65,262'
        )
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 3,
                    "results": [
                        {
                            "titles": {
                                "raw": "Apple Watch Sport",
                            },
                            "description": "Apple descr",
                            "categories": [
                                {
                                    "entity": "category",
                                    "id": 102,
                                }
                            ],
                            "shop": {
                                "id": 1010,
                                "name": "The Shop",
                                "feed": {"offerId": "225"},
                            },
                            "prices": {"currency": "RUR", "value": "100", "rawValue": "100"},
                            "meta": {"masterOffer": "xzFUFhFuAvI1sVcwDnxXPQ", "recommendationSource": "formula"},
                        },
                        {
                            "titles": {
                                "raw": "Xiaomi Mi Band",
                            },
                            "description": "Xiaomi descr",
                            "categories": [
                                {
                                    "entity": "category",
                                    "id": 102,
                                }
                            ],
                            "shop": {
                                "id": 1010,
                                "name": "The Shop",
                                "feed": {"offerId": "220"},
                            },
                            "prices": {"currency": "RUR", "value": "120", "rawValue": "120"},
                            "meta": {
                                "masterOffer": "V5Y7eJkIdDh0sMeCecijqw",
                                "recommendationSource": "shop",
                            },
                        },
                    ],
                },
            },
            preserve_order=True,
        )

    @classmethod
    def prepare_accessories_source_priority(cls):
        cls.index.shops += [
            Shop(fesh=1012, name='Shop#1012', home_region=Const.ROOT_COUNTRY, cpa=Shop.CPA_REAL),
        ]
        waremd5s = {
            230: 'C8F82cPC9N6I0Lp4MuA9Rg',
            231: 'UmLumoChESezp14MttYSKw',
            232: '1GLQ3RgNgEoQ_6A1LRrQuQ',
            233: 'DZwvm0NwXva1iu5jGIgcuA',
        }
        cls.index.offers += [
            Offer(
                hyperid=2,
                hid=101,
                price=125,
                cpa=Offer.CPA_REAL,
                fesh=1012,
                offerid=230,
                waremd5=waremd5s[230],
                title='iPhone 6 for 145',
                rec=[
                    waremd5s[232],
                ],
            ),
            Offer(
                hyperid=4,
                hid=102,
                price=135,
                cpa=Offer.CPA_REAL,
                fesh=1012,
                offerid=231,
                waremd5=waremd5s[231],
                title='Xiaomi Mi Band for 135',
            ),
            Offer(
                hyperid=3,
                hid=102,
                price=49,
                cpa=Offer.CPA_REAL,
                fesh=1012,
                offerid=232,
                waremd5=waremd5s[232],
                title='Samsung Gear S2 for 49',
            ),
            Offer(
                hyperid=2,
                hid=101,
                price=500,
                cpa=Offer.CPA_REAL,
                fesh=1012,
                offerid=233,
                waremd5=waremd5s[233],
                title='iPhone 6 for 500',
                rec=[
                    waremd5s[231],
                ],
            ),
        ]

    def test_accessories_source_priority(self):
        """
        Тест новой ручки place=accessories
        Проверка приоритета источника: из тэга rec(shop) - в первую очередь, по формуле(formula) - во вторую.
        В корзине один офер, для которого заданы аксессуары из разных источников
        1. Результат - два разнородных результата, фидовый(shop) - на первом месте
        2. Некоторый офер подходит в аксессуары к данному оферу как из формулы, так и из тэга rec. Проставляем ему источник shop
        """

        # shop source on top
        response = self.report.request_json(
            'place=accessories&pp=143&fesh=1012&hyperid=2&offerid=C8F82cPC9N6I0Lp4MuA9Rg&price=145'
        )
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 2,
                    "results": [
                        {
                            "shop": {
                                "id": 1012,
                                "feed": {"offerId": "232"},
                            },
                            "meta": {"recommendationSource": "shop"},
                        },
                        {
                            "shop": {
                                "id": 1012,
                                "feed": {"offerId": "231"},
                            },
                            "meta": {"recommendationSource": "formula"},
                        },
                    ],
                },
            },
            preserve_order=True,
        )

        # formula and shop sources conflicts and shop wins
        response = self.report.request_json(
            'place=accessories&pp=143&fesh=1012&hyperid=2&offerid=DZwvm0NwXva1iu5jGIgcuA&price=500'
        )
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 1,
                    "results": [
                        {
                            "shop": {
                                "id": 1012,
                                "feed": {"offerId": "231"},
                            },
                            "meta": {"recommendationSource": "shop"},
                        },
                    ],
                },
            },
            preserve_order=True,
        )

    @classmethod
    def prepare_accessories_paging(cls):
        cls.index.shops += [
            Shop(fesh=1013, name='Shop#1013', home_region=Const.ROOT_COUNTRY, cpa=Shop.CPA_REAL),
        ]
        waremd5s = {
            234: 'kpEvtlmvtEyOJa_PP16udw',
            235: '-3jZER7O-kToRhrbb7_a_w',
            236: '7Zwvvb_Wrcf4M1SbB1h_rA',
            237: 'twB_iWFBTXpPQmM4d73nyQ',
            238: 's8AkgbavhO88OkOhmAGhwg',
        }

        cls.index.offers += [
            Offer(
                hyperid=2,
                hid=101,
                price=1,
                cpa=Offer.CPA_REAL,
                fesh=1013,
                offerid=234,
                waremd5=waremd5s[234],
                title='another iPhone 6',
                rec=[
                    waremd5s[235],
                    waremd5s[236],
                    waremd5s[237],
                    waremd5s[238],
                ],
            ),
            Offer(
                hyperid=3,
                hid=102,
                price=1,
                cpa=Offer.CPA_REAL,
                fesh=1013,
                offerid=235,
                waremd5=waremd5s[235],
                title='another Samsung Gear S2',
            ),
            Offer(
                hyperid=4,
                hid=102,
                price=1,
                cpa=Offer.CPA_REAL,
                fesh=1013,
                offerid=236,
                waremd5=waremd5s[236],
                title='another Xiaomi Mi Band',
            ),
            Offer(
                hyperid=5,
                hid=102,
                price=1,
                cpa=Offer.CPA_REAL,
                fesh=1013,
                offerid=237,
                waremd5=waremd5s[237],
                title='another Apple Watch Sport',
            ),
            Offer(
                hyperid=6,
                hid=102,
                price=1,
                cpa=Offer.CPA_REAL,
                fesh=1013,
                offerid=238,
                waremd5=waremd5s[238],
                title='another Alcatel One Touch',
            ),
        ]

    def test_accessories_paging(self):
        """
        Тест новой ручки place=accessories
        Тестирование пэйджинга по некоторой выдаче длиной в 4
        1. Одна первая страница покрывает всё
        2. Одна первая страница и ей не хватает данных
        3. Одна последняя страница и ей не хватает данных
        4. Страница в середине коллекции
        5. Последняя полная страница
        """

        total4_query = 'place=accessories&pp=143&fesh=1013&hyperid=2&offerid=kpEvtlmvtEyOJa_PP16udw&price=1'
        result_collection = [
            {
                "shop": {
                    "feed": {"offerId": "235"},
                },
            },
            {
                "shop": {
                    "feed": {"offerId": "236"},
                },
            },
            {
                "shop": {
                    "feed": {"offerId": "237"},
                },
            },
            {
                "shop": {
                    "feed": {"offerId": "238"},
                },
            },
        ]

        # numdoc=total
        response = self.report.request_json(total4_query + '&numdoc=4&page=1')
        self.assertFragmentIn(
            response,
            {
                "search": {"total": 4, "results": result_collection},
            },
            allow_different_len=False,
            preserve_order=False,
        )
        # page out of range
        response = self.report.request_json(total4_query + '&numdoc=5&page=1')
        self.assertFragmentIn(
            response,
            {
                "search": {"total": 4, "results": result_collection},
            },
            allow_different_len=False,
            preserve_order=False,
        )
        # incomplete
        response = self.report.request_json(total4_query + '&numdoc=3&page=2')
        self.assertFragmentIn(
            response,
            {
                "search": {"total": 4, "results": result_collection[3:]},
            },
            allow_different_len=False,
            preserve_order=False,
        )
        # regular
        response = self.report.request_json(total4_query + '&numdoc=1&page=2')
        self.assertFragmentIn(
            response,
            {
                "search": {"total": 4, "results": result_collection[1:2]},
            },
            allow_different_len=False,
            preserve_order=False,
        )
        # last
        response = self.report.request_json(total4_query + '&numdoc=2&page=2')
        self.assertFragmentIn(
            response,
            {
                "search": {"total": 4, "results": result_collection[2:]},
            },
            allow_different_len=False,
            preserve_order=False,
        )

        """Проверяется, что общее количество для показа = total"""
        self.access_log.expect(total_renderable='4').times(5)

    def test_accessories_logging(self):
        """
        Тест новой ручки place=accessories
        Тестирование show log & click log
        Устанавливаем соответствие записей в логе результатам:
        Для каждой позиции в выдаче имеется запись в логах с ожидаемыми значениями в полях position, shop_id, hyper_id
        """
        self.report.request_json(
            'place=accessories&pp=143&fesh=1013&hyperid=2&offerid=kpEvtlmvtEyOJa_PP16udw&price=1&show-urls=encrypted'
        )
        logs = [
            {'position': 0, 'shop_id': 1013, 'hyper_id': 3},
            {'position': 1, 'shop_id': 1013, 'hyper_id': 4},
            {'position': 2, 'shop_id': 1013, 'hyper_id': 5},
            {'position': 3, 'shop_id': 1013, 'hyper_id': 6},
        ]
        for item in logs:
            self.click_log.expect(ClickType.EXTERNAL, **item)
            self.show_log.expect(**item)

    @classmethod
    def prepare_accessories_pictures(cls):
        cls.index.shops += [
            Shop(fesh=1014, name='Shop#1014', home_region=Const.ROOT_COUNTRY, cpa=Shop.CPA_REAL),
        ]
        waremd5s = {
            239: '9fb43joW6zZ0ZHzpHDfpnA',
            240: 'u5MI-crEIVo-HOu81oQkeg',
            241: 'K3RhV5-LY9XL2eybX4yvFg',
            242: '0NtLt5RsCPzO2CFtTM1FIQ',
            243: 'XKkwTsf0kvemSnwPPl054A',
        }

        pic1 = Picture(width=100, height=100, group_id=1234, picture_id='RR8iTgTu9FlO4dHjozyM7g')
        pic2 = Picture(width=100, height=100, group_id=1234, picture_id='vifaCUAnS2zzEHNTZPhqMQ')

        cls.index.offers += [
            Offer(
                hyperid=2,
                hid=101,
                price=1,
                cpa=Offer.CPA_REAL,
                fesh=1014,
                offerid=239,
                waremd5=waremd5s[239],
                title='iPhone 6 with accessories',
                rec=[
                    waremd5s[240],
                    waremd5s[241],
                ],
            ),
            Offer(
                hyperid=3,
                hid=102,
                price=1,
                cpa=Offer.CPA_REAL,
                fesh=1014,
                offerid=240,
                waremd5=waremd5s[240],
                title='Samsung Gear S2 with pictures',
                picture=pic1,
            ),
            Offer(
                hyperid=3,
                hid=102,
                price=1,
                cpa=Offer.CPA_REAL,
                fesh=1014,
                offerid=241,
                waremd5=waremd5s[241],
                title='Samsung Gear S2 pictureless',
                no_picture=True,
            ),
            Offer(
                hyperid=5,
                hid=102,
                price=1,
                cpa=Offer.CPA_REAL,
                fesh=1014,
                offerid=242,
                waremd5=waremd5s[242],
                title='Apple Watch Sport with pictures',
                picture=pic2,
            ),
            Offer(
                hyperid=4,
                hid=102,
                price=1,
                cpa=Offer.CPA_REAL,
                fesh=1014,
                offerid=243,
                waremd5=waremd5s[243],
                title='Xiaomi Mi Band pictureless',
                no_picture=True,
            ),
        ]

    def test_accessories_pictures(self):
        """
        Тест новой ручки place=accessories
        Тестирование фильтрации офферов без картинок. Имееются рекомендации по формуле и из тэга REC,
        среди тех и других есть оферы без картинок.
        1. Выдача содержит все рекомендуемые оферы с картинками и не содержит оферов без картинок
        """

        response = self.report.request_json(
            'place=accessories&pp=143&fesh=1014&hyperid=2&offerid=9fb43joW6zZ0ZHzpHDfpnA&price=1'
        )
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 2,
                    "results": [
                        {"shop": {"feed": {"offerId": "240"}}},
                        {"shop": {"feed": {"offerId": "242"}}},
                    ],
                },
            },
            preserve_order=False,
        )

    def test_accessories_no_model(self):
        """
        Тест MARKETOUT-10654: для получения рекомендациЙ из фидов необходимо укаазание идентификаторов магазина и офера
        в запросе и не требуется модель
        """

        response = self.report.request_json(
            'place=accessories&pp=143&fesh=1013&hyperid=&offerid=kpEvtlmvtEyOJa_PP16udw&price=1'
        )
        self.assertFragmentIn(response, {"search": {"total": 4}})

        response = self.report.request_json('place=accessories&pp=143&fesh=1013&offerid=kpEvtlmvtEyOJa_PP16udw&price=1')
        self.assertFragmentIn(response, {"search": {"total": 4}})

    def test_accessories_no_price(self):
        """
        Получение рекомендациЙ из фидов без указания цены
        """
        response = self.report.request_json(
            'place=accessories&pp=143&fesh=1013&hyperid=2&offerid=kpEvtlmvtEyOJa_PP16udw&price='
        )
        self.assertFragmentIn(response, {"search": {"total": 4}})

        response = self.report.request_json(
            'place=accessories&pp=143&fesh=1013&hyperid=2&offerid=kpEvtlmvtEyOJa_PP16udw'
        )
        self.assertFragmentIn(response, {"search": {"total": 4}})

    def test_accessories_params(self):
        """
        Для получения рекомендациЙ из фидов необходимо укаазание идентификаторов магазина и офера
        """

        # -fesh
        response = self.report.request_json('place=accessories&pp=143&hyperid=2&offerid=kpEvtlmvtEyOJa_PP16udw')
        self.assertFragmentIn(response, {"search": {"total": 0}})

        # -offerid
        response = self.report.request_json('place=accessories&pp=143&hyperid=2&fesh=1013&price=1')
        self.assertFragmentIn(response, {"search": {"total": 0}})

    @classmethod
    def prepare_accessories_hidfilter(cls):
        """
        Данные для теста фильтра по категориям
        * Ключевая модель 50
        * Две модели аксессуаров для формульной выдачи
        * Один аксессуар для тэга рек
        """
        cls.index.models += [
            Model(hyperid=50, hid=150, title='Model#50', accessories=[51, 52]),
            Model(hyperid=51, hid=151, title='Accessory moidel#51'),
            Model(hyperid=52, hid=152, title='Accessory model#52'),
            Model(hyperid=53, hid=153, title='Accessory model#53'),
        ]
        cls.index.shops += [
            Shop(fesh=1050, name='The Shop#50', home_region=Const.ROOT_COUNTRY, cpa=Shop.CPA_REAL),
        ]
        waremd5s = {
            250: '913hQM8vnO3yz1ZLo0doKA',
            251: 'JvNF67E4xIgzNLWl8--7xQ',
            252: 'Bxiwj634OX5c0WYeJ9xa4w',
            253: '9S5I0NpFxql8VDNZaUeD0w',
        }
        cls.index.offers += [
            Offer(
                hyperid=50,
                hid=150,
                price=100500,
                cpa=Offer.CPA_REAL,
                fesh=1050,
                offerid=250,
                waremd5=waremd5s[250],
                rec=[waremd5s[253]],
            ),
            Offer(
                hyperid=51,
                hid=151,
                price=100500,
                cpa=Offer.CPA_REAL,
                fesh=1050,
                offerid=251,
                waremd5=waremd5s[251],
            ),
            Offer(
                hyperid=52,
                hid=152,
                price=100500,
                cpa=Offer.CPA_REAL,
                fesh=1050,
                offerid=252,
                waremd5=waremd5s[252],
            ),
            Offer(
                hyperid=53,
                hid=153,
                price=100500,
                cpa=Offer.CPA_REAL,
                fesh=1050,
                offerid=253,
                waremd5=waremd5s[253],
            ),
        ]

    def test_accessories_hidfilter(self):
        """
        Тест фильтра по категориям для офферных аксессуаров
        * Проверка пустого фильтра, не ограничивающего выдачу
        * Мультифильтр со всеми категориями даёт всю выдачу
        * Проверка одиночного фильтра для случаев формульной выдачи и из тэга рек
        * Кейсы с мультифильтрами и однородными и смешанными выдачами (формула или тэг рек)
        * Проверка смешанных фильтров: если нет конфликтов, то исключающие элементы не играют, + на - даёт пустое множество
        """
        whole_collection = {
            "search": {
                "total": 3,
                "results": [
                    {"shop": {"feed": {"offerId": "251"}}},
                    {"shop": {"feed": {"offerId": "252"}}},
                    {"shop": {"feed": {"offerId": "253"}}},
                ],
            }
        }
        empty_collection = {"search": {"total": 0, "results": []}}

        def request_json(filter_param):
            return self.report.request_json(
                'place=accessories&fesh=1050&hyperid=50&price=100500&offerid=913hQM8vnO3yz1ZLo0doKA{filter_param}'.format(
                    filter_param=filter_param
                )
            )

        # empty filters
        response = request_json('')
        self.assertFragmentIn(response, whole_collection, preserve_order=False)
        response = request_json('&hid=')
        self.assertFragmentIn(response, whole_collection, preserve_order=False)

        # non-empty filter and empty result
        response = request_json('&hid=150')
        self.assertFragmentIn(response, empty_collection, preserve_order=False)

        # all hids in including filter
        response = request_json('&hid=151,152,153')
        self.assertFragmentIn(response, whole_collection, preserve_order=False)

        # simple filter and some formula accs only
        response = request_json('&hid=151')
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 1,
                    "results": [
                        {"shop": {"feed": {"offerId": "251"}}},
                    ],
                }
            },
        )

        # simple and some rec accs only
        response = request_json('&hid=153')
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 1,
                    "results": [
                        {"shop": {"feed": {"offerId": "253"}}},
                    ],
                }
            },
        )

        # multifilter and some formula accs only
        response = request_json('&hid=151,152')
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 2,
                    "results": [
                        {"shop": {"feed": {"offerId": "251"}}},
                        {"shop": {"feed": {"offerId": "252"}}},
                    ],
                }
            },
            preserve_order=False,
        )

        # multifilter and some rec accs only
        response = request_json('&hid=150,153')
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 1,
                    "results": [
                        {"shop": {"feed": {"offerId": "253"}}},
                    ],
                }
            },
        )

        # mulitfilter and mixed results
        response = request_json('&hid=152,153')
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 2,
                    "results": [
                        {"shop": {"feed": {"offerId": "252"}}},
                        {"shop": {"feed": {"offerId": "253"}}},
                    ],
                }
            },
            preserve_order=False,
        )

        # simple excluding filter and whole collection
        response = request_json('&hid=-150')
        self.assertFragmentIn(response, whole_collection, preserve_order=False)

        # simple excluding filter and all formula results
        response = request_json('&hid=-153')
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 2,
                    "results": [
                        {"shop": {"feed": {"offerId": "251"}}},
                        {"shop": {"feed": {"offerId": "252"}}},
                    ],
                }
            },
            preserve_order=False,
        )

        # simple excluding filter and mixed eresults
        response = request_json('&hid=-152')
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 2,
                    "results": [
                        {"shop": {"feed": {"offerId": "251"}}},
                        {"shop": {"feed": {"offerId": "253"}}},
                    ],
                }
            },
            preserve_order=False,
        )

        # excluding multifilter and all rec eresults
        response = request_json('&hid=-151,-152')
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 1,
                    "results": [
                        {"shop": {"feed": {"offerId": "253"}}},
                    ],
                }
            },
        )

        # excluding multifilter and all formula eresults
        response = request_json('&hid=-150,-153')
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 2,
                    "results": [
                        {"shop": {"feed": {"offerId": "251"}}},
                        {"shop": {"feed": {"offerId": "252"}}},
                    ],
                }
            },
            preserve_order=False,
        )

        # excluding multifilter and mixed eresults
        response = request_json('&hid=-150,-151')
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 2,
                    "results": [
                        {"shop": {"feed": {"offerId": "252"}}},
                        {"shop": {"feed": {"offerId": "253"}}},
                    ],
                }
            },
            preserve_order=False,
        )

        # excluding total multifilter
        response = request_json('&hid=-152,-151,-153')
        self.assertFragmentIn(response, empty_collection, preserve_order=False)

        # weird mixed multifilters
        response = request_json('&hid=-151,151')
        self.assertFragmentIn(response, empty_collection, preserve_order=False)
        response = request_json('&hid=-151,152')
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 1,
                    "results": [
                        {"shop": {"feed": {"offerId": "252"}}},
                    ],
                }
            },
        )
        response = request_json('&hid=-151,-152,153')
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 1,
                    "results": [
                        {"shop": {"feed": {"offerId": "253"}}},
                    ],
                }
            },
        )
        response = request_json('&hid=151,-152')
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 1,
                    "results": [
                        {"shop": {"feed": {"offerId": "251"}}},
                    ],
                }
            },
        )
        response = request_json('&hid=-153,150')
        self.assertFragmentIn(response, empty_collection, preserve_order=False)

    @classmethod
    def prepare_accessories_sources(cls):
        cls.index.models += [
            Model(hyperid=12, hid=101, accessories=[13]),
            Model(hyperid=13, hid=102),
            Model(hyperid=14, hid=102),
            Model(hyperid=15, hid=102),
        ]
        cls.index.offers += [
            Offer(
                hyperid=12,
                hid=101,
                price=120,
                cpa=Offer.CPA_REAL,
                fesh=1010,
                waremd5='EpnWVxDQxj4wg7vVI1ElnA',
                rec=['TTnVlqbztMi95ithBNMa3g'],
            ),
            Offer(hyperid=13, hid=102, price=120, cpa=Offer.CPA_REAL, fesh=1010),
            Offer(hyperid=14, hid=102, price=120, cpa=Offer.CPA_REAL, fesh=1010, waremd5='TTnVlqbztMi95ithBNMa3g'),
            Offer(hyperid=15, hid=102, price=120, cpa=Offer.CPA_REAL, fesh=1010, waremd5='pCl2on9YL4fCV8poq57hRg'),
        ]

        cls.index.yamarec_places += [
            YamarecPlace(
                name=YamarecPlace.Name.OFFER_ACCESSORY,
                kind=YamarecPlace.Type.SETTING,
                partitions=[
                    YamarecSettingPartition(
                        params={
                            'use-external': '1',
                            'version': '1',
                            'use-product': '1',
                            'use-shop': '1',
                        },
                        splits=['1'],
                    ),
                    YamarecSettingPartition(
                        params={
                            'use-external': '0',
                            'version': '1',
                            'use-product': '1',
                            'use-shop': '1',
                        },
                        splits=['2'],
                    ),
                    YamarecSettingPartition(
                        params={
                            'use-external': '1',
                            'version': '1',
                            'use-product': '0',
                            'use-shop': '1',
                        },
                        splits=['3'],
                    ),
                    YamarecSettingPartition(
                        params={
                            'use-external': '1',
                            'version': '1',
                            'use-product': '1',
                            'use-shop': '0',
                        },
                        splits=['4'],
                    ),
                ],
            )
        ]
        cls.recommender.on_request_accessory_offers(
            offer_id='EpnWVxDQxj4wg7vVI1ElnA', item_count=100, version='1'
        ).respond({'offers': ['pCl2on9YL4fCV8poq57hRg']})

    def test_sources(self):
        """
        Проверяем аксессуары к офферу модели 12 из разных источников:
          -рекомендации магазина (оффер модели 14)
          -рекомендации по офферной формуле (оффер модели 15)
          -рекомендации по модельной формуле (оффер модели 13)
        Приоритет: магазин > офферная формула > модельная формула
        Тесты:
          -делаем запрос с пользователем(yandexuid=001) для которого включены все источники
           получаем 3 оффера от моделей 14, 15, 13 в нужном порядке
          -делаем запрос с пользователем(yandexuid=002) для которого включены источники: магазин, модельная формула
           получаем 2 оффера от моделей 14, 13 в нужном порядке
          -делаем запрос с пользователем(yandexuid=002) для которого включены источники: магазин, офферная формула
           получаем 2 оффера от моделей 14, 15 в нужном порядке
          -делаем запрос с пользователем(yandexuid=003) для которого включены источники: офферная формула, модельная формула
           получаем 2 оффера от моделей 15, 13 в нужном порядке
        """

        response = self.report.request_json(
            'place=accessories&pp=143&fesh=1010&hyperid=12&offerid=EpnWVxDQxj4wg7vVI1ElnA&price=120&yandexuid=001'
        )
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 3,
                    "results": [
                        {"model": {"id": 14}},
                        {"model": {"id": 15}},
                        {"model": {"id": 13}},
                    ],
                }
            },
            preserve_order=True,
        )

        response = self.report.request_json(
            'place=accessories&pp=143&fesh=1010&hyperid=12&offerid=EpnWVxDQxj4wg7vVI1ElnA&price=120&yandexuid=002'
        )
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 2,
                    "results": [
                        {"model": {"id": 14}},
                        {"model": {"id": 13}},
                    ],
                }
            },
            preserve_order=True,
        )

        response = self.report.request_json(
            'place=accessories&pp=143&fesh=1010&hyperid=12&offerid=EpnWVxDQxj4wg7vVI1ElnA&price=120&yandexuid=003'
        )
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 2,
                    "results": [
                        {"model": {"id": 14}},
                        {"model": {"id": 15}},
                    ],
                }
            },
            preserve_order=True,
        )

        response = self.report.request_json(
            'place=accessories&pp=143&fesh=1010&hyperid=12&offerid=EpnWVxDQxj4wg7vVI1ElnA&price=120&yandexuid=004'
        )
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 2,
                    "results": [
                        {"model": {"id": 15}},
                        {"model": {"id": 13}},
                    ],
                }
            },
            preserve_order=True,
        )

    @classmethod
    def prepare_promoted_categories(cls):
        """Создаем категории с разной степенью принадлежности к мужским/женским
        Категории в разных регионах
        """
        cls.index.hypertree += [HyperCategory(hid=hid) for hid in set(PROMOTED_CATEGORIES + PROMOTED_CATEGORIES_W)]

        cls.index.shops += [
            Shop(fesh=1704311, name='Магазин в Москве', priority_region=213),
            Shop(fesh=1704312, name='Магазин в С.-Пб.', priority_region=2),
        ]

        cls.index.offers += [
            Offer(hid=PROMOTED_CATEGORIES[0], fesh=1704312),
        ]

        cls.index.offers += [
            Offer(hid=PROMOTED_CATEGORIES_W[0], fesh=1704312),
        ]

        cls.index.offers += [
            Offer(hid=hid, fesh=1704311) for hid in set(PROMOTED_CATEGORIES[1:] + PROMOTED_CATEGORIES_W[1:])
        ]

        # 1704301 - 55% male
        cls.crypta.on_request_profile(yandexuid="1704301").respond(
            features=[
                CryptaFeature(name=CryptaName.GENDER_MALE, value=550000),
            ]
        )
        # 1704302 - 55% female
        cls.crypta.on_request_profile(yandexuid="1704302").respond(
            features=[
                CryptaFeature(name=CryptaName.GENDER_MALE, value=450000),
            ]
        )
        feature_values = [
            # более мужские категории
            [950000, 100],
            [950000, 100000],
            [950000, 250000],
            [850000, 200],
            [800000, 100000],
            [750000, 150000],
            [550000, 450000],
            [500000, 450000],
            [50000, 500000],
            [350000, 650000],
            [100000, 650000],
            [100000, 800000],
            # более женские категории
        ]

        cls.index.yamarec_places += [
            YamarecPlace(
                name=YamarecPlace.Name.CATEGORY_GENERIC,
                kind=YamarecPlace.Type.FORMULA,
                partitions=[
                    YamarecFeaturePartition(
                        feature_names=['category_id', '174:0_avg', 'transit_count'],
                        feature_keys=['category_id'],
                        features=[
                            [hid, f[0], f[1]]
                            for hid, f in zip(PROMOTED_CATEGORIES[: len(feature_values)], feature_values)
                        ]
                        + [
                            [hid, f[0], f[1]]
                            for hid, f in zip(PROMOTED_CATEGORIES_W[: len(feature_values)], feature_values)
                        ],
                        splits=['1704301', '1704302'],
                    ),
                ],
            )
        ]

    def test_promoted_categories(self):
        """Проверяем продвигаемые категории для мужского и женского варантов
        Для мужского варианта подходят все со значением фичи 174:0_avg
        не менее 300000.
        Для женского варианта подходят все со значением фичи 174:0_avg
        не более 700000 (включительно). Некоторые категории
        не убираются, т.к. статистика считается недостоверной
        """
        data = (
            ('rgb=green', PROMOTED_CATEGORIES_W),
            ('cpa=real', PROMOTED_CATEGORIES),
            ('rgb=blue', PROMOTED_CATEGORIES),
        )
        for suffix, hids in data:
            # Запрос с "мужским" yandexuid
            response = self.report.request_json(
                'place=promoted_categories&rids=0&yandexuid=1704301&{suffix}'.format(suffix=suffix)
            )
            self.assertFragmentIn(
                response,
                {"search": {"total": 8, "results": [{"link": {"params": {"hid": str(hid)}}} for hid in hids[:8]]}},
                preserve_order=True,
                allow_different_len=False,
            )

            # Запрос с "женским" yandexuid
            response = self.report.request_json(
                'place=promoted_categories&rids=0&yandexuid=1704302&{suffix}'.format(suffix=suffix)
            )
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "total": 8,
                        "results": [{"link": {"params": {"hid": str(hids[i])}}} for i in [0, 3, 6, 7, 8, 9, 10, 11]],
                    }
                },
                preserve_order=True,
                allow_different_len=False,
            )

    def test_promoted_categories_region_filter(self):
        """Проверяем продвигаемые категории для мужского и женского варантов
        для региона пользователя
        """
        data = (
            ('rgb=green', PROMOTED_CATEGORIES_W),
            ('cpa=real', PROMOTED_CATEGORIES),
            ('rgb=blue', PROMOTED_CATEGORIES),
        )
        for rgb, hids in data:
            # Запрос с "мужским" yandexuid
            response = self.report.request_json(
                'place=promoted_categories&rids=213&yandexuid=1704301&{rgb}'.format(rgb=rgb)
            )
            self.assertFragmentIn(
                response,
                {"search": {"total": 7, "results": [{"link": {"params": {"hid": str(hid)}}} for hid in hids[1:8]]}},
                preserve_order=True,
                allow_different_len=False,
            )

            # Запрос с "женским" yandexuid
            response = self.report.request_json(
                'place=promoted_categories&rids=213&yandexuid=1704302&{rgb}'.format(rgb=rgb)
            )
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "total": 7,
                        "results": [{"link": {"params": {"hid": str(hids[i])}}} for i in [3, 6, 7, 8, 9, 10, 11]],
                    }
                },
                preserve_order=True,
                allow_different_len=False,
            )


if __name__ == '__main__':
    main()

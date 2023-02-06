#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    BlueOffer,
    DateSwitchTimeAndRegionInfo,
    DeliveryBucket,
    DeliveryOption,
    DynamicDeliveryServiceInfo,
    DynamicWarehouseAndDeliveryServiceInfo,
    DynamicWarehouseInfo,
    DynamicWarehousesPriorityInRegion,
    GLParam,
    GLType,
    HyperCategory,
    HyperCategoryType,
    MarketSku,
    MnPlace,
    Model,
    RegionalDelivery,
    Shop,
    SponsoredMsku,
    YamarecPlace,
    YamarecSettingPartition,
    ShopOperationalRating,
)
from core.testcase import TestCase, main
from core.matcher import EmptyList, NoKey, NotEmptyList


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.set_default_reqid = False
        cls.settings.rgb_blue_is_cpa = True

        cls.index.shops += [
            Shop(
                fesh=1,
                datafeed_id=1,
                priority_region=213,
                name='ВиртуальныйМагазинНаБеру',
                fulfillment_virtual=True,
                virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
            ),
            Shop(
                fesh=1111,
                datafeed_id=1111,
                priority_region=213,
                regions=[225],
                name="Беру!",
                supplier_type=Shop.FIRST_PARTY,
                blue=Shop.BLUE_REAL,
                warehouse_id=145,
            ),
            Shop(
                fesh=2222,
                datafeed_id=2222,
                priority_region=213,
                regions=[225],
                name="РазмещаюсьНаБеру!",
                supplier_type=Shop.THIRD_PARTY,
                blue=Shop.BLUE_REAL,
                warehouse_id=145,
            ),
        ]

        cls.index.shop_operational_rating += [
            ShopOperationalRating(
                shop_id=2222,
                late_ship_rate=0.0019,
                cancellation_rate=0.0027,
                return_rate=0.001,
                total=0,
            )
        ]

        cls.index.hypertree += [
            HyperCategory(
                hid=187,
                name="Материалы",
                output_type=HyperCategoryType.GURU,
                children=[
                    HyperCategory(hid=1875, name="Секретные материалы", output_type=HyperCategoryType.GURU),
                    HyperCategory(hid=1876, name="Публичные материалы", output_type=HyperCategoryType.GURU),
                ],
            ),
            HyperCategory(hid=1880, name="Нематериальное", output_type=HyperCategoryType.GURU),
        ]

        cls.index.gltypes += [
            GLType(
                param_id=1000001,
                hid=1875,
                name="Уровень секретности",
                gltype=GLType.ENUM,
                hidden=True,
                values=[1, 2, 3],
            ),
            GLType(
                param_id=1000001,
                hid=1876,
                name="Уровень доступности",
                gltype=GLType.ENUM,
                hidden=True,
                values=[1, 2, 3],
            ),
            GLType(
                param_id=1000001,
                hid=1880,
                name="Уровень нематериальности",
                gltype=GLType.ENUM,
                hidden=True,
                values=[1, 2, 3],
            ),
        ]

        def create_material(hid, id, title, value, randx, mn, is_pmodel=False):

            cls.index.models += [
                Model(
                    hid=hid,
                    hyperid=id,
                    title=title,
                    is_pmodel=is_pmodel,
                    glparams=[GLParam(param_id=1000001, value=value)],
                ),
            ]

            cls.index.mskus += [
                MarketSku(
                    title=title,
                    hyperid=id,
                    sku=id * 10,
                    hid=hid,
                    delivery_buckets=[1234],
                    blue_offers=[
                        BlueOffer(
                            randx=randx, ts=id * 10001, feedid=1111, waremd5="BLUE-Model{}-f1111-----".format(id)[0:22]
                        ),
                        BlueOffer(
                            randx=randx, ts=id * 10002, feedid=2222, waremd5="BLUE-Model{}-f2222-----".format(id)[0:22]
                        ),
                    ],
                )
            ]

            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, id * 10001).respond(mn)
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, id * 10002).respond(mn)

        create_material(
            hid=1875, id=110, title="Ретроавтомобили в каршеринге", value=1, randx=110, mn=0.3, is_pmodel=True
        )  # партнерские модели тоже в деле
        create_material(hid=1875, id=111, title="Золотая Акция 2.0", value=1, randx=111, mn=0.31)
        create_material(hid=1875, id=120, title="Брингли закрывают", value=2, randx=120, mn=0.2)
        create_material(hid=1875, id=121, title="Яндекс снимет сезон Смешариков", value=2, randx=121, mn=0.21)
        create_material(hid=1875, id=131, title="Император уехал в Microsoft", value=3, randx=131, mn=0.4)
        create_material(hid=1875, id=132, title="Алиса умеет составлять список покупок", value=3, randx=132, mn=0.1)
        create_material(hid=1875, id=133, title="Алиса умеет зачитывать сайты", value=3, randx=133, mn=0.45)
        create_material(hid=1875, id=134, title="Маркет + Сбербанк = Большой Бизнес", value=3, randx=134, mn=0.22)
        create_material(hid=1875, id=135, title="Маркет переедет в Lotte Plaza", value=3, randx=135, mn=0.42)

        create_material(hid=1876, id=210, title="Годовой доход Путина", value=1, randx=120, mn=0.23)
        create_material(hid=1876, id=220, title="Материалы Архива Саратова", value=2, randx=100, mn=0.13)
        create_material(
            hid=1876, id=231, title="Инсайдерская информация для торговли акциями", value=3, randx=80, mn=0.16
        )
        create_material(hid=1876, id=232, title="Персональные данные", value=3, randx=60, mn=0.53)
        create_material(hid=1876, id=233, title="Данные банковских карт клиентов Сбербанка", value=3, randx=40, mn=0.43)
        create_material(
            hid=1876, id=234, title="NDA всех сортов в публичных чатиках Телеграма", value=3, randx=20, mn=0.32
        )

        cls.index.sponsored_mskus += [
            # приоритет этой скушки будет зависеть от того какой оффер выиграл buybox
            SponsoredMsku(hid=1875, msku=1100, supplier_id=1111, priority=1),
            SponsoredMsku(hid=1875, msku=1100, supplier_id=2222, priority=2),
            SponsoredMsku(hid=1875, msku=1100, priority=3),
            # приоритет этой скушки будет всегда 0 т.к. приортитет скушки без учета байбокса наиболее высокий
            SponsoredMsku(hid=1875, msku=1110, priority=0),
            SponsoredMsku(hid=1875, msku=1110, supplier_id=1111, priority=1),
            SponsoredMsku(hid=1875, msku=1110, supplier_id=2222, priority=2),
            # эти скушки покажутся только если их buybox выиграл магазин 1111
            SponsoredMsku(hid=1875, msku=1200, supplier_id=1111, priority=10),
            SponsoredMsku(hid=1875, msku=1210, supplier_id=1111, priority=10),
            # эти скушки покажутся только если их buybox выиграл магазин 2222
            SponsoredMsku(hid=1875, msku=1310, supplier_id=2222, priority=10),
            SponsoredMsku(hid=1875, msku=1320, supplier_id=2222, priority=10),
            SponsoredMsku(hid=1875, msku=1330, supplier_id=2222, priority=10),
            SponsoredMsku(hid=1875, msku=1340, supplier_id=2222, priority=10),
            SponsoredMsku(hid=1875, msku=1350, supplier_id=2222, priority=10),
        ]

        cls.settings.lms_autogenerate = False
        cls.dynamic.lms += [
            DynamicWarehouseInfo(id=145, home_region=2),
            DynamicWarehouseInfo(id=147, home_region=213),
            DynamicWarehouseAndDeliveryServiceInfo(
                warehouse_id=145,
                delivery_service_id=157,
                date_switch_time_infos=[DateSwitchTimeAndRegionInfo(date_switch_hour=23, region_to=225)],
            ),
            DynamicWarehouseAndDeliveryServiceInfo(
                warehouse_id=147,
                delivery_service_id=157,
                date_switch_time_infos=[DateSwitchTimeAndRegionInfo(date_switch_hour=23, region_to=225)],
            ),
            DynamicDeliveryServiceInfo(id=157, rating=2),
            DynamicWarehousesPriorityInRegion(region=225, warehouses=[145, 147]),
        ]

        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=1234,
                carriers=[157],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
                regional_options=[
                    RegionalDelivery(rid=225, options=[DeliveryOption(price=15, day_from=1, day_to=2)]),
                ],
            )
        ]

    def test_sponsored_products(self):
        """Проверяем формат ответа place=sponsored_products
        Модели с ДО и признаком рекламы adv.type=sponsored
        Сортировка по RANDOM
        Учитываются glfilter, hid не учитывается текст
        Документы фильтруются на базовых в зависимости от того какой им выпал buybox
        """

        response = self.report.request_json(
            'debug=da&place=sponsored_products&rids=213&hid=1875&rgb=blue&debug=da&numdoc=3'
        )
        self.assertFragmentIn(
            response,
            {
                "knownThumbnails": NotEmptyList(),
                'search': {
                    'results': [
                        {
                            'entity': 'product',
                            'titles': {'raw': 'Золотая Акция 2.0'},
                            'offers': {'items': NotEmptyList()},
                            'adv': [{'type': 'sponsored'}],
                            'debug': {
                                'rank': [
                                    {
                                        'name': 'SPONSORED_PRIORITY',
                                        'value': '0',
                                    },  # приоритет ску с любым buybox больше чем приоритет любого из магазинов
                                    {'name': 'RANDOM'},
                                    {'name': 'RANDX'},
                                ]
                            },
                        },
                        {
                            'entity': 'product',
                            'titles': {'raw': 'Ретроавтомобили в каршеринге'},
                            'offers': {
                                'items': [{'wareId': 'BLUE-Model110-f2222--w'}]
                            },  # оффер из магазина 2222 выиграл buybox значит SPONSORED_PRIORITY=2
                            'adv': [{'type': 'sponsored'}],
                            'debug': {
                                'rank': [
                                    {'name': 'SPONSORED_PRIORITY', 'value': '2'},
                                    {'name': 'RANDOM'},
                                    {'name': 'RANDX'},
                                ]
                            },
                        },
                        {'titles': {'raw': 'Маркет переедет в Lotte Plaza'}},
                    ]
                },
            },
            preserve_order=True,
            allow_different_len=False,
        )

        rearr = '&sponsored-place=block-on-search&rearr-factors=market_sponsored_products=block-on-search:3:0;market_blue_buybox_disable_old_buybox_algo=0'
        response = self.report.request_json(
            'debug=da&place=sponsored_products&rids=213&hid=1875&rgb=blue&debug=da' + rearr
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {'titles': {'raw': 'Золотая Акция 2.0'}},
                        {'titles': {'raw': 'Ретроавтомобили в каршеринге'}},
                        {'titles': {'raw': 'Маркет + Сбербанк = Большой Бизнес'}},
                    ]
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # текст не влияет
        response = self.report.request_json(
            'debug=da&place=sponsored_products&rids=213&hid=1875&rgb=blue&debug=da&text=текст' + rearr
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {'titles': {'raw': 'Золотая Акция 2.0'}},
                        {'titles': {'raw': 'Ретроавтомобили в каршеринге'}},
                        {'titles': {'raw': 'Маркет + Сбербанк = Большой Бизнес'}},
                    ]
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # находятся только модели соответсвующие фильтрам
        response = self.report.request_json(
            'debug=da&place=sponsored_products&rids=213&hid=1875&rgb=blue&debug=da&glfilter=1000001:3' + rearr
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {'titles': {'raw': 'Маркет + Сбербанк = Большой Бизнес'}},
                        {'titles': {'raw': 'Алиса умеет составлять список покупок'}},
                        {'titles': {'raw': 'Император уехал в Microsoft'}},
                    ]
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # часть документов не подойдет по причине SUPPLIER_ID т.к. buybox выиграл магазин не имеющий спонсорского размещения
        response = self.report.request_json(
            'debug=da&place=sponsored_products&rids=213&hid=1875&rgb=blue&debug=da&numdoc=6&rearr-factors=market_blue_buybox_disable_old_buybox_algo=0'
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {'titles': {'raw': 'Золотая Акция 2.0'}},
                        {'titles': {'raw': 'Ретроавтомобили в каршеринге'}},
                        {'titles': {'raw': 'Маркет + Сбербанк = Большой Бизнес'}},
                        {'titles': {'raw': 'Алиса умеет составлять список покупок'}},
                        {'titles': {'raw': 'Император уехал в Microsoft'}},
                        {'titles': {'raw': 'Яндекс снимет сезон Смешариков'}},
                    ]
                },
                'debug': {'brief': {'filters': {'SUPPLIER_ID': 3}}},  # 3 + 6 = 9 моделей
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def test_sponsored_products_numdoc(self):
        """Количество документов показываемых в рекламе определяется с помощью market_sponsored_products
        Формат флага: market_sponsored_products=place:count:position,place:count:position
        Возможные плейсы:
           block-on-search - блок в выдаче на поиске или на бестексте
           mimic-on-search - мимикрирующие предложения которые выглядят как обычные в поиске или на бестексте
           also-viewed-on-km - мимикрирующие предложения в блоке "С этим товаром смотрят" на КМ
           accessories-on-km - мимикрирующие предложения в блоке "С этим товаром покупают" на КМ
           block-on-km - отдельный блок с рекламными предложениями на КМ
        """

        rearr = (
            '&rearr-factors=market_blue_buybox_disable_old_buybox_algo=0;market_sponsored_products='
            'block-on-search:4:0,'
            'mimic-on-search:2:0,'
            'also-viewed-on-km:1:3,'
            'block-on-km:6:0'
        )
        request = 'place=sponsored_products&rids=213&hid=1875&rgb=blue'

        response = self.report.request_json(request + rearr + '&sponsored-place=block-on-search')
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {'titles': {'raw': 'Золотая Акция 2.0'}},
                        {'titles': {'raw': 'Ретроавтомобили в каршеринге'}},
                        {'titles': {'raw': 'Маркет + Сбербанк = Большой Бизнес'}},
                        {'titles': {'raw': 'Алиса умеет составлять список покупок'}},
                    ]
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

        response = self.report.request_json(request + rearr + '&sponsored-place=mimic-on-search')
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {'titles': {'raw': 'Золотая Акция 2.0'}},
                        {'titles': {'raw': 'Ретроавтомобили в каршеринге'}},
                    ]
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

        response = self.report.request_json(request + rearr + '&sponsored-place=also-viewed-on-km')
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {'titles': {'raw': 'Золотая Акция 2.0'}},
                    ]
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

        response = self.report.request_json(request + rearr + '&sponsored-place=block-on-km')
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {'titles': {'raw': 'Золотая Акция 2.0'}},
                        {'titles': {'raw': 'Ретроавтомобили в каршеринге'}},
                        {'titles': {'raw': 'Маркет + Сбербанк = Большой Бизнес'}},
                        {'titles': {'raw': 'Алиса умеет составлять список покупок'}},
                        {'titles': {'raw': 'Император уехал в Microsoft'}},
                        {'titles': {'raw': 'Яндекс снимет сезон Смешариков'}},
                    ]
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # accessories-on-km не задан в rearr-factors
        response = self.report.request_json(request + rearr + '&sponsored-place=accessories-on-km')
        self.assertFragmentIn(response, {'search': {'results': []}}, preserve_order=True, allow_different_len=False)

        # cgi-параметр sponsored-place не задан (по умолчанию no)
        response = self.report.request_json(request + rearr)
        self.assertFragmentIn(response, {'search': {'results': []}}, preserve_order=True, allow_different_len=False)

    def test_no_sponsored_products_if_no_sponsored_mskus(self):
        """В категории 1876 нет спонсорских скушек, значит не должно их быть и на выдаче"""

        response = self.report.request_json(
            'debug=da&place=sponsored_products&rids=213&hid=1876&rgb=blue&debug=da&numdoc=3'
        )
        self.assertFragmentNotIn(response, {'adv': [{'type': 'sponsored'}]})
        self.assertFragmentIn(response, {'search': {'results': EmptyList()}}, allow_different_len=False)

    def test_sponsored_products_mimic_on_search(self):
        """Проверяем как в выдаче на place=prime появляются мимикрирующие товары
        Рекламные товары выбираются из той же категории с учетом glfilter
        Рекламные модели помечены тегом type:sponsored и сортируются по SF_SPONSORED
        """

        # выдача без рекламы
        response = self.report.request_json(
            'place=prime&hid=1875&glfilter=1000001:3&rids=213&debug=da&numdoc=10&page=1&rgb=blue&viewtype=grid'
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'view': 'grid',
                    'results': [
                        {'titles': {'raw': 'Алиса умеет зачитывать сайты'}},
                        {'titles': {'raw': 'Маркет переедет в Lotte Plaza'}},
                        {'titles': {'raw': 'Император уехал в Microsoft'}},
                        {'titles': {'raw': 'Маркет + Сбербанк = Большой Бизнес'}},
                        {'titles': {'raw': 'Алиса умеет составлять список покупок'}},
                    ],
                }
            },
            allow_different_len=False,
            preserve_order=True,
        )

        # выдача с рекламой (поскольку все документы входят на страницу то никакие документы не были отсечены даже при view=grid)
        # вставляем 2 рекламы на 3 позицию
        rearr = '&additional_entities=sponsored_mimic&rearr-factors=market_sponsored_products=mimic-on-search:2:3:0;market_blue_buybox_disable_old_buybox_algo=0'
        response = self.report.request_json(
            'place=prime&hid=1875&glfilter=1000001:3&rids=213&debug=da&numdoc=10&page=1&rgb=blue&viewtype=grid' + rearr
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'view': 'grid',
                    'results': [
                        {'titles': {'raw': 'Алиса умеет зачитывать сайты'}, 'adv': NoKey('adv')},
                        {'titles': {'raw': 'Маркет переедет в Lotte Plaza'}, 'adv': NoKey('adv')},
                        {
                            'titles': {'raw': 'Император уехал в Microsoft'},
                            'adv': NoKey('adv'),
                            'debug': {
                                'rank': [
                                    {'name': 'HAS_PICTURE'},
                                    {'name': 'DELIVERY_TYPE'},
                                    {'name': 'IS_MODEL'},
                                    {'name': 'CPM'},
                                    {'name': 'MODEL_TYPE'},
                                    {'name': 'POPULARITY'},
                                    {'name': 'ONSTOCK'},
                                    {'name': 'RANDX'},
                                ]
                            },
                        },
                        # рекламные документы на 3 и 4 позиции
                        {
                            'titles': {'raw': 'Маркет + Сбербанк = Большой Бизнес'},
                            'adv': [{'type': 'sponsored'}],
                            'debug': {
                                'rank': [
                                    {'name': 'SPONSORED_PRIORITY'},
                                    {'name': 'RANDOM'},
                                    {'name': 'RANDX'},
                                ]
                            },
                        },
                        {'titles': {'raw': 'Алиса умеет составлять список покупок'}, 'adv': [{'type': 'sponsored'}]},
                        # обычный товар
                        {'titles': {'raw': 'Маркет + Сбербанк = Большой Бизнес'}, 'adv': NoKey('adv')},
                        {'titles': {'raw': 'Алиса умеет составлять список покупок'}, 'adv': NoKey('adv')},
                    ],
                }
            },
            allow_different_len=False,
            preserve_order=True,
        )

        # если документов недостаточно для показа рекламы - она не отображается
        response = self.report.request_json(
            'place=prime&hid=1875&glfilter=1000001:2&rids=213&debug=da&numdoc=6&page=1&rgb=blue' + rearr
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'view': 'list',
                    'results': [
                        {'titles': {'raw': 'Яндекс снимет сезон Смешариков'}},
                        {'titles': {'raw': 'Брингли закрывают'}},
                    ],
                }
            },
            allow_different_len=False,
            preserve_order=True,
        )
        self.assertFragmentIn(response, 'Not enough documents to show sponsored products in ads')
        self.assertFragmentIn(
            response,
            {
                'brief': {
                    "filters": {"GURULIGHT": 5, "SUPPLIER_ID": 2},
                    "counters": {
                        "TOTAL_DOCUMENTS_PROCESSED": 8,
                        "TOTAL_DOCUMENTS_ACCEPTED": 1,
                    },
                }
            },
        )

    def test_sponsored_mimic_on_search_for_grid(self):
        """Вставка мимикрирующих товаров в выдаче на place=prime
        При гридовой выдаче количество документов не должно нарушаться - лишние документы откидываются
        При листовой выдаче разное количество документов допустимо"""

        # обычная выдача, сортировка по умолчанию - документы упорядочены по mn_value
        response = self.report.request_json('place=prime&hid=1875&rids=213&debug=da&numdoc=6&page=1&rgb=blue')
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {'titles': {'raw': 'Алиса умеет зачитывать сайты'}},
                        {'titles': {'raw': 'Маркет переедет в Lotte Plaza'}},
                        {'titles': {'raw': 'Император уехал в Microsoft'}},
                        {'titles': {'raw': 'Золотая Акция 2.0'}},
                        {'titles': {'raw': 'Ретроавтомобили в каршеринге'}},
                        {'titles': {'raw': 'Маркет + Сбербанк = Большой Бизнес'}},
                    ]
                }
            },
            allow_different_len=False,
            preserve_order=True,
        )
        self.assertFragmentNotIn(response, {'adv': [{'type': 'sponsored'}]})
        self.assertFragmentIn(
            response,
            'Sponsored products for sponsored-place=mimic-on-search is disabled: count=0 in flag market_sponosored_place',
        )

        # вставляем 2 рекламы на 3 позицию минимальное количество документов выдаче 2
        rearr = '&additional_entities=sponsored_mimic&rearr-factors=market_sponsored_products=mimic-on-search:2:3:2'

        # по умолчанию view=list поэтому отображаются все документы
        response = self.report.request_json('place=prime&hid=1875&rids=213&debug=da&numdoc=6&page=1&rgb=blue' + rearr)
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'view': 'list',
                    'results': [
                        {'titles': {'raw': 'Алиса умеет зачитывать сайты'}},
                        {'titles': {'raw': 'Маркет переедет в Lotte Plaza'}},
                        {'titles': {'raw': 'Император уехал в Microsoft'}},
                        # 2 рекламных документа на 3 и 4 позиции (считая с 0)
                        {'titles': {'raw': 'Золотая Акция 2.0'}, 'adv': [{'type': 'sponsored'}]},
                        {'titles': {'raw': 'Ретроавтомобили в каршеринге'}, 'adv': [{'type': 'sponsored'}]},
                        # обычные документы
                        {'titles': {'raw': 'Золотая Акция 2.0'}},
                        {'titles': {'raw': 'Ретроавтомобили в каршеринге'}},
                        {'titles': {'raw': 'Маркет + Сбербанк = Большой Бизнес'}},
                    ],
                }
            },
            allow_different_len=False,
            preserve_order=True,
        )

        # при view=grid лишние документы обрезаются (например Ретроавтомобили в каршеринге)
        response = self.report.request_json(
            'place=prime&hid=1875&rids=213&debug=da&numdoc=6&page=1&rgb=blue&viewtype=grid' + rearr
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'view': 'grid',
                    'results': [
                        {'titles': {'raw': 'Алиса умеет зачитывать сайты'}},
                        {'titles': {'raw': 'Маркет переедет в Lotte Plaza'}},
                        {'titles': {'raw': 'Император уехал в Microsoft'}},
                        # 2 рекламных документа на 3 и 4 позиции (считая с 0)
                        {'titles': {'raw': 'Золотая Акция 2.0'}, 'adv': [{'type': 'sponsored'}]},
                        {'titles': {'raw': 'Ретроавтомобили в каршеринге'}, 'adv': [{'type': 'sponsored'}]},
                        # обычные документы
                        {'titles': {'raw': 'Золотая Акция 2.0'}},
                    ],
                }
            },
            allow_different_len=False,
            preserve_order=True,
        )

        # на второй странице рекламные документы не показываются, показываются оставшиеся 3 документа из 9
        # отброшенные документы (Ретроавтомобили в каршеринге) при viewtype=grid тоже не добавляются (они вообще пропадают O_o)
        response = self.report.request_json(
            'place=prime&hid=1875&rids=213&debug=da&numdoc=6&page=2&rgb=blue&viewtype=grid' + rearr
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'view': 'grid',
                    'results': [
                        {'titles': {'raw': 'Яндекс снимет сезон Смешариков'}},
                        {'titles': {'raw': 'Брингли закрывают'}},
                        {'titles': {'raw': 'Алиса умеет составлять список покупок'}},
                    ],
                }
            },
            allow_different_len=False,
            preserve_order=True,
        )
        self.assertFragmentNotIn(response, {'adv': [{'type': 'sponsored'}]})
        self.assertFragmentIn(
            response, 'Sponsored products for sponsored-place=mimic-on-search is disabled: requested page > 1'
        )

    def test_no_sponsored_mimic_on_search(self):
        """Мимикрирующая реклама в выдаче на place=prime отображается
        Только на первой странице
        Только из данной категории + с учетом glфильтров
        Только на дефолтной сортировке
        Только на бестексте
        Только при наличии additional_entities=sponsored_mimic
        Только при заданном rearr-factors=market_sponsored_products=mimic-on-search:count:position:mincount
        Только если количество результатов поиска >= mincount
        """

        def check_no_sponsored(query, reason):
            response = self.report.request_json(query)
            self.assertFragmentNotIn(response, {'adv': [{'type': 'sponsored'}]})
            self.assertFragmentIn(
                response, 'Sponsored products for sponsored-place=mimic-on-search is disabled: {}'.format(reason)
            )

        # вставляем 1 рекламу на 4 позицию минимальное количество документов выдаче 3
        rearr = '&rearr-factors=market_sponsored_products=mimic-on-search:1:4:3'
        add = '&additional_entities=sponsored_mimic'
        check_no_sponsored(
            'place=prime&hid=1875&rids=213&debug=da&numdoc=6&page=2&rgb=blue' + add + rearr, 'requested page > 1'
        )

        check_no_sponsored(
            'place=prime&hid=1875&rids=213&debug=da&numdoc=6&how=aprice&rgb=blue' + add + rearr, 'user sorting'
        )

        check_no_sponsored(
            'place=prime&hid=1875&rids=213&debug=da&numdoc=6&text=алиса&rgb=blue' + add + rearr, 'text search'
        )

        check_no_sponsored(
            'place=prime&hid=1875&rids=213&debug=da&numdoc=6&rgb=blue' + rearr,
            'additional_entities does not contain AE_SPONSORED_MIMIC',
        )

        check_no_sponsored(
            'place=prime&hid=1875&rids=213&debug=da&numdoc=6&rgb=blue' + add, 'count=0 in flag market_sponosored_place'
        )

        check_no_sponsored(
            'place=prime&hid=1875&glfilter=1000001:1&rids=213&debug=da&numdoc=6&rgb=blue' + add + rearr,
            'too low search results',
        )

        check_no_sponsored(
            'place=prime&hid=1875&rids=213&debug=da&numdoc=3&rgb=blue' + add + rearr,
            'position > count documents on page',
        )

    def test_sponsored_block_on_search(self):
        """Проверяем как в выдаче на place=prime появляется реклама в виде блока"""

        # обычная выдача, сортировка по умолчанию - документы упорядочены по mn_value
        response = self.report.request_json('place=prime&hid=1875&rids=213&debug=da&numdoc=6&page=1&rgb=blue')
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {'titles': {'raw': 'Алиса умеет зачитывать сайты'}},
                        {'titles': {'raw': 'Маркет переедет в Lotte Plaza'}},
                        {'titles': {'raw': 'Император уехал в Microsoft'}},
                        {'titles': {'raw': 'Золотая Акция 2.0'}},
                        {'titles': {'raw': 'Ретроавтомобили в каршеринге'}},
                        {'titles': {'raw': 'Маркет + Сбербанк = Большой Бизнес'}},
                    ]
                }
            },
            allow_different_len=False,
            preserve_order=True,
        )
        self.assertFragmentNotIn(response, {'adv': [{'type': 'sponsored'}]})
        self.assertFragmentIn(
            response,
            'Sponsored products for sponsored-place=mimic-on-search is disabled: count=0 in flag market_sponosored_place',
        )

        # выдача с рекламой (поскольку все документы входят на страницу то никакие документы не были отсечены даже при view=grid)
        # вставляем блок рекламы на 2 позицию
        rearr = '&additional_entities=sponsored_block&rearr-factors=market_sponsored_products=block-on-search:4:2:0'
        response = self.report.request_json('place=prime&hid=1875&rids=213&debug=da&numdoc=6&page=1&rgb=blue' + rearr)
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {'titles': {'raw': 'Алиса умеет зачитывать сайты'}},
                        {'titles': {'raw': 'Маркет переедет в Lotte Plaza'}},
                        {'entity': 'sponsoredBlockDelimiter'},
                        {'titles': {'raw': 'Император уехал в Microsoft'}},
                        {'titles': {'raw': 'Золотая Акция 2.0'}},
                        {'titles': {'raw': 'Ретроавтомобили в каршеринге'}},
                        {'titles': {'raw': 'Маркет + Сбербанк = Большой Бизнес'}},
                    ]
                }
            },
            allow_different_len=False,
            preserve_order=True,
        )

        # если документов на странице меньше чем позиция блока - блок вставляется в конец
        rearr = '&additional_entities=sponsored_block&rearr-factors=market_sponsored_products=block-on-search:4:20:0'
        response = self.report.request_json('place=prime&hid=1875&rids=213&debug=da&numdoc=48&page=1&rgb=blue' + rearr)
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {'titles': {'raw': 'Алиса умеет зачитывать сайты'}},
                        {'titles': {'raw': 'Маркет переедет в Lotte Plaza'}},
                        {'titles': {'raw': 'Император уехал в Microsoft'}},
                        {'titles': {'raw': 'Золотая Акция 2.0'}},
                        {'titles': {'raw': 'Ретроавтомобили в каршеринге'}},
                        {'titles': {'raw': 'Маркет + Сбербанк = Большой Бизнес'}},
                        {'titles': {'raw': 'Яндекс снимет сезон Смешариков'}},
                        {'titles': {'raw': 'Брингли закрывают'}},
                        {'titles': {'raw': 'Алиса умеет составлять список покупок'}},
                        {'entity': 'sponsoredBlockDelimiter'},
                    ]
                }
            },
            allow_different_len=False,
            preserve_order=True,
        )

    def test_no_sponsored_block_on_search(self):
        """Блок рекламы в выдаче на place=prime отображается
        Только на первой странице
        Только из данной категории + с учетом glфильтров
        Только на дефолтной сортировке
        Только на бестексте
        Только при наличии additional_entities=sponsored_mimic
        Только при заданном rearr-factors=market_sponsored_products=mimic-on-search:count:position:mincount
        Только если количество результатов поиска >= mincount
        """

        def check_no_sponsored(query, reason):
            response = self.report.request_json(query)
            self.assertFragmentNotIn(response, {'entity': 'sponsoredBlockDelimiter'})
            self.assertFragmentIn(
                response, 'Sponsored products for sponsored-place=block-on-search is disabled: {}'.format(reason)
            )

        # вставляем рекламный блок на 4 позицию минимальное количество документов выдаче 2
        rearr = '&rearr-factors=market_sponsored_products=block-on-search:1:4:3'
        add = '&additional_entities=sponsored_block'
        check_no_sponsored(
            'place=prime&hid=1875&rids=213&debug=da&numdoc=6&page=2&rgb=blue' + add + rearr, 'requested page > 1'
        )

        check_no_sponsored(
            'place=prime&hid=1875&rids=213&debug=da&numdoc=6&how=aprice&rgb=blue' + add + rearr, 'user sorting'
        )

        check_no_sponsored(
            'place=prime&hid=1875&rids=213&debug=da&numdoc=6&text=алиса&rgb=blue' + add + rearr, 'text search'
        )

        check_no_sponsored(
            'place=prime&hid=1875&rids=213&debug=da&numdoc=6&rgb=blue' + rearr,
            'additional_entities does not contain AE_SPONSORED_BLOCK',
        )

        check_no_sponsored(
            'place=prime&hid=1875&rids=213&debug=da&numdoc=6&rgb=blue' + add, 'count=0 in flag market_sponosored_place'
        )

        check_no_sponsored(
            'place=prime&hid=1875&glfilter=1000001:1&rids=213&debug=da&numdoc=6&rgb=blue' + add + rearr,
            'too low search results',
        )

        check_no_sponsored(
            'place=prime&hid=1875&rids=213&debug=da&numdoc=3&rgb=blue' + add + rearr,
            'position > count documents on page',
        )

    @classmethod
    def prepare_mimic_accessories_on_km(cls):

        cls.index.yamarec_places += [
            YamarecPlace(
                name=YamarecPlace.Name.EXTERNAL_PRODUCT_ACCESSORY_BLUE_MARKET,
                kind=YamarecPlace.Type.SETTING,
                split_rule=YamarecPlace.SplitRule.ABT,
                partitions=[
                    YamarecSettingPartition(
                        params={'version': '1'},
                        splits=[{}],
                    ),
                ],
            )
        ]

        cls.recommender.on_request_accessory_models(model_id=110, item_count=1000, version='1').respond(
            {'models': ['132', '133', '220', '231']}
        )

    def test_mimic_accessories_on_km(self):
        """Вставка мимикрирующих товаров в выдаче на place=product_accessories"""

        # выдача без рекламы
        response = self.report.request_json(
            'place=product_accessories&hyperid=110&rids=213&debug=da&rgb=blue&rearr-factors=market_disable_product_accessories=0'
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {'id': 132, 'titles': {'raw': 'Алиса умеет составлять список покупок'}},
                        {'id': 133, 'titles': {'raw': 'Алиса умеет зачитывать сайты'}},
                        {'id': 220, 'titles': {'raw': 'Материалы Архива Саратова'}},
                        {'id': 231, 'titles': {'raw': 'Инсайдерская информация для торговли акциями'}},
                    ]
                }
            },
            allow_different_len=False,
            preserve_order=True,
        )

        # выдача с рекламой (поскольку все документы входят на страницу то никакие документы не были отсечены даже при view=grid)
        # вставляем 1 рекламу на 1 позицию
        rearr = '&additional_entities=sponsored_mimic&rearr-factors=market_sponsored_products=accessories-on-km:1:1:0;market_disable_product_accessories=0'
        response = self.report.request_json('place=product_accessories&hyperid=110&rids=213&debug=da&rgb=blue' + rearr)

        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {'id': 132, 'titles': {'raw': 'Алиса умеет составлять список покупок'}},
                        {'id': 111, 'titles': {'raw': 'Золотая Акция 2.0'}, 'adv': [{'type': 'sponsored'}]},
                        {'id': 133, 'titles': {'raw': 'Алиса умеет зачитывать сайты'}},
                        {'id': 220, 'titles': {'raw': 'Материалы Архива Саратова'}},
                        {'id': 231, 'titles': {'raw': 'Инсайдерская информация для торговли акциями'}},
                    ]
                }
            },
            allow_different_len=False,
            preserve_order=True,
        )

        # если позиция больше чем количество документов полученных от рекоммендаций - добавляем в конец
        rearr = '&additional_entities=sponsored_mimic&rearr-factors=market_sponsored_products=accessories-on-km:1:6:0;market_disable_product_accessories=0'
        response = self.report.request_json('place=product_accessories&hyperid=110&rids=213&debug=da&rgb=blue' + rearr)
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {'id': 132, 'titles': {'raw': 'Алиса умеет составлять список покупок'}},
                        {'id': 133, 'titles': {'raw': 'Алиса умеет зачитывать сайты'}},
                        {'id': 220, 'titles': {'raw': 'Материалы Архива Саратова'}},
                        {'id': 231, 'titles': {'raw': 'Инсайдерская информация для торговли акциями'}},
                        {'id': 111, 'titles': {'raw': 'Золотая Акция 2.0'}, 'adv': [{'type': 'sponsored'}]},
                    ]
                }
            },
            allow_different_len=False,
            preserve_order=True,
        )

    @classmethod
    def prepare_mimic_also_viewed(cls):
        cls.index.yamarec_places += [
            YamarecPlace(
                name=YamarecPlace.Name.ALSO_VIEWED_PRODUCTS,
                kind=YamarecPlace.Type.SETTING,
                split_rule=YamarecPlace.SplitRule.ABT,
                partitions=[
                    # no partition with split 'noconfig'
                    # empty matching partition
                    YamarecSettingPartition(params={'version': '2'}, splits=[{}]),
                ],
            ),
        ]
        # also_viewed_blue is based on product accessories ichwill method
        cls.recommender.on_request_accessory_models(model_id=110, item_count=1000, version='2').respond(
            {'models': ['231', '234', '131']}
        )

    def test_mimic_also_viewed_on_km(self):
        """Вставка мимикрирующих товаров в выдаче на place=also_viewed"""

        # выдача без рекламы
        response = self.report.request_json('place=also_viewed&hyperid=110&rids=213&debug=da&rgb=blue')
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {'id': 231, 'titles': {'raw': 'Инсайдерская информация для торговли акциями'}},
                        {'id': 234, 'titles': {'raw': 'NDA всех сортов в публичных чатиках Телеграма'}},
                        {'id': 131, 'titles': {'raw': 'Император уехал в Microsoft'}},
                    ]
                }
            },
            allow_different_len=False,
            preserve_order=True,
        )

        # выдача с рекламой
        # вставляем 1 рекламу на 2 позицию
        rearr = '&additional_entities=sponsored_mimic&rearr-factors=market_sponsored_products=also-viewed-on-km:1:2:0'
        response = self.report.request_json('place=also_viewed&hyperid=110&rids=213&debug=da&rgb=blue' + rearr)
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {'id': 231, 'titles': {'raw': 'Инсайдерская информация для торговли акциями'}},
                        {'id': 234, 'titles': {'raw': 'NDA всех сортов в публичных чатиках Телеграма'}},
                        {'id': 111, 'titles': {'raw': 'Золотая Акция 2.0'}, 'adv': [{'type': 'sponsored'}]},
                        {'id': 131, 'titles': {'raw': 'Император уехал в Microsoft'}},
                    ]
                }
            },
            allow_different_len=False,
            preserve_order=True,
        )

        # выдача с рекламой
        # вставляем 1 рекламу на 2 позицию
        # т.к. количество товаров в выдаче на place=also_viewed превышает numdoc то лишние товары будут обрезаны
        rearr = '&additional_entities=sponsored_mimic&rearr-factors=market_sponsored_products=also-viewed-on-km:1:2:0'
        response = self.report.request_json('place=also_viewed&hyperid=110&rids=213&debug=da&rgb=blue&numdoc=3' + rearr)
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {'id': 231, 'titles': {'raw': 'Инсайдерская информация для торговли акциями'}},
                        {'id': 234, 'titles': {'raw': 'NDA всех сортов в публичных чатиках Телеграма'}},
                        {'id': 111, 'titles': {'raw': 'Золотая Акция 2.0'}, 'adv': [{'type': 'sponsored'}]},
                    ]
                }
            },
            allow_different_len=False,
            preserve_order=True,
        )

        # если позиция больше чем количество документов полученных от рекоммендаций - добавляем в конец
        rearr = '&additional_entities=sponsored_mimic&rearr-factors=market_sponsored_products=also-viewed-on-km:1:4:0'
        response = self.report.request_json('place=also_viewed&hyperid=110&rids=213&debug=da&rgb=blue' + rearr)
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {'id': 231, 'titles': {'raw': 'Инсайдерская информация для торговли акциями'}},
                        {'id': 234, 'titles': {'raw': 'NDA всех сортов в публичных чатиках Телеграма'}},
                        {'id': 131, 'titles': {'raw': 'Император уехал в Microsoft'}},
                        {'id': 111, 'titles': {'raw': 'Золотая Акция 2.0'}, 'adv': [{'type': 'sponsored'}]},
                    ]
                }
            },
            allow_different_len=False,
            preserve_order=True,
        )


if __name__ == '__main__':
    main()

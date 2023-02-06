#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from simple_testcase import get_timestamp
from core.testcase import TestCase, main
from core.types.models_grade_agitations import ModelGradeAgitations
from core.types import (
    BidSettings,
    BlueOffer,
    CategoryBidSettings,
    CategoryRestriction,
    Currency,
    DynamicVendorModelBid,
    HyperCategory,
    MarketSku,
    MnPlace,
    Model,
    NewShopRating,
    Offer,
    Opinion,
    Region,
    RegionalModel,
    RegionalMsku,
    RegionalRestriction,
    Shop,
    Tax,
    Vendor,
)

from core.matcher import NotEmpty, Absent, Contains, ElementCount


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.regiontree += [
            Region(
                rid=1,
                name='Московская область',
                region_type=Region.FEDERATIVE_SUBJECT,
                children=[
                    Region(rid=213, name='Москва'),
                ],
            )
        ]

        cls.index.offers += [
            Offer(
                title='phone iphone 7',
                vendor_id=1,
                hyperid=1,
                hid=1,
                price=100,
                fesh=1,
                waremd5='5AvY3NaPMZRaBLyPr8qI_A',
            ),
            Offer(
                title='phone iphone 7 plus',
                vendor_id=1,
                hyperid=1,
                hid=1,
                price=200,
                fesh=1,
                waremd5='S4gNYZu7y76iKxO_owoazg',
            ),
            Offer(
                title='phone iphone special',
                vendor_id=1,
                hyperid=1,
                price=300,
                fesh=2,
                waremd5='OpepoVTy9iY4OTRGgVRW_Q',
            ),
        ]
        cls.index.offers += [Offer(title='phone galaxy s7', vendor_id=2, hyperid=2, hid=2, fesh=3) for _ in range(26)]
        cls.index.offers += [
            Offer(
                title='HTC One',
                vendor_id=3,
                hyperid=3,
                bid=100,
                randx=1300,
                price=200,
                fesh=3,
                waremd5='Pamdhwpy9iY4OTRGgVRW_Q',
                is_recommended=True,
            ),
            Offer(
                title='HTC Desire 828',
                vendor_id=3,
                hyperid=4,
                bid=1000,
                randx=1200,
                price=300,
                fesh=1,
                waremd5='WksdadsfkpdjOTRGgVRW_F',
                is_recommended=True,
            ),
            Offer(
                title='HTC Desire 620',
                vendor_id=3,
                hyperid=4,
                bid=500,
                randx=1400,
                price=600,
                fesh=2,
                waremd5='Yedweodedbjkejnwjejn_S',
                is_recommended=True,
            ),
            Offer(
                title='HTC Incredible S',
                vendor_id=3,
                hyperid=5,
                bid=200,
                randx=1500,
                price=400,
                fesh=7,
                waremd5='Mlwpwkxmkwpwafbbrtyf_A',
            ),
        ]

        cls.index.shops += [
            Shop(
                fesh=1,
                priority_region=213,
                regions=[225],
                name='test_shop_1',
                new_shop_rating=NewShopRating(new_rating_total=3.0),
            ),
            Shop(
                fesh=2,
                priority_region=213,
                regions=[225],
                name='test_shop_2',
                new_shop_rating=NewShopRating(new_rating_total=4.0),
            ),
            Shop(
                fesh=3,
                priority_region=213,
                regions=[225],
                name='test_shop_3',
                new_shop_rating=NewShopRating(new_rating_total=5.0),
            ),
        ]

        cls.index.models += [
            Model(
                hyperid=1,
                hid=1,
                title='iphone',
                vendor_id=1,
                opinion=Opinion(total_count=3, rating=4.5),
                vbid=100,
                datasource_id=10,
            ),
            Model(hyperid=2, hid=2, title='samsung', vendor_id=2),
            Model(
                hyperid=3,
                hid=1,
                title='One',
                created_ts=get_timestamp(2017, 2, 21),
                vendor_id=3,
                opinion=Opinion(total_count=4, rating=4.0),
                model_clicks=100,
                randx=3,
            ),
            Model(
                hyperid=5,
                hid=1,
                title='Incredible S',
                created_ts=get_timestamp(2017, 2, 20),
                vendor_id=3,
                opinion=Opinion(total_count=5, rating=3.5),
                model_clicks=1500,
                randx=5,
            ),
            Model(
                hyperid=4,
                hid=1,
                title='Desire',
                created_ts=get_timestamp(2017, 2, 22),
                vendor_id=3,
                opinion=Opinion(total_count=2, rating=4.5),
                model_clicks=5000,
                randx=4,
            ),
        ]

        cls.index.vendors += [
            Vendor(vendor_id=1, name='Apple'),
            Vendor(vendor_id=2, name='Samsung'),
            Vendor(vendor_id=3, name='HTC'),
        ]

        # Для тестирования фильтров
        cls.index.regiontree += [
            Region(
                rid=226,
                region_type=Region.COUNTRY,
                children=[
                    Region(
                        rid=227,
                        region_type=Region.FEDERATIVE_SUBJECT,
                        children=[Region(rid=229, region_type=Region.CITY)],
                    ),
                    Region(rid=228, region_type=Region.CITY),
                ],
            )
        ]

        cls.index.vendors += [
            Vendor(vendor_id=4, name='Lenovo'),
            Vendor(vendor_id=5, name='Xiaomi'),
        ]

        cls.index.models += [
            Model(hyperid=6, hid=1, title='Vibe Shot', vendor_id=4, vbid=0, datasource_id=4, barcode='12345'),
            Model(hyperid=7, hid=1, title='A1000', vendor_id=4, barcode='67890'),
            Model(hyperid=8, hid=3, title='IdeaPad 100 15', vendor_id=4, vbid=200, datasource_id=4, barcode='11111'),
            Model(hyperid=9, hid=3, title='B50 45', vendor_id=4, vbid=250, datasource_id=4, barcode='22222'),
        ]

        cls.index.shops += [
            Shop(fesh=4, priority_region=213, regions=[225], name='test_shop_4'),
            Shop(fesh=5, priority_region=229, regions=[227], name='test_shop_5'),
            Shop(fesh=6, priority_region=228, regions=[226], name='test_shop_6'),
            Shop(
                fesh=7,
                priority_region=213,
                regions=[225],
                name='test_shop_7',
                new_shop_rating=NewShopRating(new_rating_total=2.0),
            ),
        ]

        cls.index.offers += [
            Offer(
                title='Lenovo Vibe Shot',
                vendor_id=4,
                hyperid=6,
                descr='12345',
                price=1000,
                fesh=4,
                waremd5='dcasdcasdcd4OTRGgVRW_Q',
            ),
            Offer(title='Lenovo A1000', vendor_id=4, hyperid=7, price=1200, fesh=4, waremd5='cccxxzxxccxcOTRGgVRW_F'),
            Offer(
                title='Смартфон Lenovo A1000',
                vendor_id=4,
                hyperid=7,
                price=1300,
                fesh=5,
                waremd5='nmnxmnxmncxooxniidei_F',
            ),
            Offer(
                title='Lenovo IdeaPad 100 15',
                vendor_id=4,
                hyperid=8,
                price=2000,
                fesh=6,
                waremd5='vdcdccredbjkejnwjejn_S',
            ),
            Offer(title='Lenovo B50 45', vendor_id=4, hyperid=9, price=2100, fesh=4, waremd5='zzrrrkxmkwpwafbbrtyf_A'),
        ]

        # Для тестирования выдачи минимальных ставок на модель
        cls.index.category_bid_settings += [
            CategoryBidSettings(
                category=1,
                search_settings=BidSettings(coefficient=0.08, power=0.7, maximumBid=45),
            ),
            CategoryBidSettings(
                category=3,
                search_settings=BidSettings(coefficient=0.055, power=0.56, maximumBid=45),
            ),
        ]
        cls.index.regional_models += [
            RegionalModel(hyperid=7, offers=2, price_min=1200, price_max=1300, price_med=1250, rids=[0])
        ]

        # Для тестирование быстроотключения ставок на модели
        cls.index.models += [
            Model(hyperid=10, hid=1, title='model10', vendor_id=5, vbid=100, datasource_id=1),
            Model(hyperid=11, hid=1, title='model11', vendor_id=5, vbid=200, datasource_id=2),
            Model(hyperid=12, hid=1, title='model12', vendor_id=5, vbid=300, datasource_id=3),
            Model(hyperid=13, hid=1, title='model13', vendor_id=5, vbid=400, datasource_id=3),
        ]

        # Для тестирования выдачи msku
        blue_virtual_shop = Shop(
            fesh=1000,
            datafeed_id=1,
            priority_region=213,
            fulfillment_virtual=True,
            virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
            currency=Currency.RUR,
            tax_system=Tax.OSN,
            supplier_type=Shop.FIRST_PARTY,
        )
        cls.index.shops += [blue_virtual_shop]

        cls.index.mskus += [
            MarketSku(
                vendor_id=3,
                hyperid=3,
                sku=10000,
                title='HTC One msku',
                waremd5='sku1000000000000000000',
                blue_offers=[],
                randx=13,
            ),
            MarketSku(
                hyperid=4,
                sku=10001,
                vendor_id=3,
                title='HTC Desire msku',
                waremd5='sku1000000000000000001',
                blue_offers=[],
                randx=14,
            ),
            MarketSku(
                hyperid=5,
                sku=10002,
                vendor_id=3,
                title='HTC Incredible S msku',
                waremd5='sku1000000000000000002',
                blue_offers=[],
                randx=15,
            ),
            MarketSku(
                hyperid=6,
                sku=10003,
                vendor_id=4,
                title='Lenovo VibeShot msku',
                waremd5='sku1000000000000000003',
                blue_offers=[
                    BlueOffer(
                        price=345,
                        feedid=4,
                        offerid='sku10003offer1',
                        waremd5='sku1000300000000000011',
                    ),
                    BlueOffer(
                        price=678,
                        feedid=3,
                        offerid='sku10003offer2',
                        waremd5='sku1000300000000000022',
                    ),
                    BlueOffer(
                        price=567,
                        feedid=3,
                        offerid='sku10003offer3',
                        waremd5='sku1000300000000000033',
                    ),
                ],
                randx=16,
            ),
            MarketSku(
                fesh=blue_virtual_shop.fesh,
                hyperid=5,
                sku=10004,
                vendor_id=3,
                title='HTC Incredible S msku2',
                waremd5='sku1000000000000000004',
                blue_offers=[],
                randx=17,
            ),
        ]

        cls.index.blue_regional_mskus += [RegionalMsku(msku_id=10000, offers=2, price_min=100, price_max=500, rids=[0])]

        # Для тестирования фильтрации по лицензиару/франшизе/персонажу
        cls.index.vendors += [Vendor(vendor_id=6, name='vendor6'), Vendor(vendor_id=7, name='vendor7')]

        cls.index.offers += [
            Offer(title='offer6_1', vendor_id=6, hyperid=14, fesh=3, waremd5='Pamdhwpy9iY4OTRGgVRW_w'),
            Offer(
                title='offer6_2',
                vendor_id=6,
                licensor=1,
                hero_global=1,
                pers_model=2,
                hyperid=15,
                fesh=3,
                waremd5='Pamdhwpy9iY4OTRGgVRW_e',
            ),
            Offer(
                title='offer7_1',
                vendor_id=7,
                licensor=2,
                hero_global=2,
                pers_model=2,
                hyperid=16,
                fesh=3,
                waremd5='Pamdhwpy9iY4OTRGgVRW_r',
            ),
            Offer(
                title='offer7_2',
                vendor_id=7,
                licensor=3,
                hero_global=3,
                pers_model=3,
                hyperid=17,
                fesh=3,
                waremd5='Pamdhwpy9iY4OTRGgVRW_t',
            ),
            Offer(
                title='offer7_3',
                vendor_id=7,
                licensor=3,
                hero_global=3,
                pers_model=4,
                hyperid=18,
                fesh=3,
                waremd5='Pamdhwpy9iY4OTRGgVRW_y',
            ),
        ]

        cls.index.models += [
            Model(hyperid=14, hid=1, title='model14', vendor_id=6),
            Model(hyperid=15, hid=1, title='model15', vendor_id=6, licensor=1, hero_global=1, pers_model=2),
            Model(hyperid=16, hid=1, title='model16', vendor_id=7, licensor=2, hero_global=2, pers_model=2),
            Model(hyperid=17, hid=1, title='model17', vendor_id=7, licensor=3, hero_global=3, pers_model=3),
            Model(hyperid=18, hid=1, title='model18', vendor_id=7, licensor=3, hero_global=3, pers_model=4),
        ]

        cls.index.models += [
            Model(hyperid=25, hid=100500, title='model_with_grade_agitations0', vendor_id=10),
            Model(hyperid=26, hid=100501, title='model_with_grade_agitations1', vendor_id=10),
            Model(hyperid=27, hid=100502, title='model_with_zero_grade_agitations', vendor_id=20),
            Model(hyperid=28, hid=100503, title='model_with_without_grade_agitations', vendor_id=20),
        ]

        cls.index.models_grade_agitations += [
            ModelGradeAgitations(model_id=25, grade_agitations=125),
            ModelGradeAgitations(model_id=26, grade_agitations=5),
            ModelGradeAgitations(model_id=27, grade_agitations=0),
        ]

    def test_place(self):
        response = self.report.request_json('place=brand_products&vendor_id=1&rids=213')
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 4,
                    'totalOffers': 3,
                    'totalModels': 1,
                    'adult': False,
                    'salesDetected': False,
                    'shops': 2,
                    'cpaCount': 0,
                    'isFuzzySearch': Absent(),
                    'isParametricSearch': False,
                    'isDeliveryIncluded': False,
                    'results': [
                        {
                            'entity': 'product',
                            'id': 1,
                            'hid': 1,
                            'titles': {'raw': 'iphone'},
                            'prices': {'min': '100', 'max': '300'},
                            'pictures': [
                                {
                                    'entity': 'picture',
                                    'thumbnails': [
                                        {'containerWidth': 50, 'containerHeight': 50, 'width': 50, 'height': 50},
                                        {'containerWidth': 100, 'containerHeight': 100, 'width': 100, 'height': 100},
                                        {'containerWidth': 75, 'containerHeight': 75, 'width': 75, 'height': 75},
                                        {'containerWidth': 150, 'containerHeight': 150, 'width': 150, 'height': 150},
                                        {'containerWidth': 200, 'containerHeight': 200, 'width': 200, 'height': 200},
                                        {'containerWidth': 250, 'containerHeight': 250, 'width': 250, 'height': 250},
                                        {'containerWidth': 120, 'containerHeight': 120, 'width': 120, 'height': 120},
                                        {'containerWidth': 240, 'containerHeight': 240, 'width': 240, 'height': 240},
                                        {'containerWidth': 500, 'containerHeight': 500, 'width': 500, 'height': 500},
                                    ],
                                }
                            ],
                            'retailerCount': '2',
                            'opinions': 3,
                            'rating': 4.5,
                            'minVBid': 1,
                        },
                        {
                            'entity': 'offer',
                            'wareId': '5AvY3NaPMZRaBLyPr8qI_A',
                            'titles': {'raw': 'phone iphone 7'},
                            'price': {
                                'currency': 'RUR',
                                'value': '100',
                                'isDeliveryIncluded': False,
                                'rawValue': '100',
                            },
                            'pictures': [
                                {
                                    'entity': 'picture',
                                    'thumbnails': [
                                        {'containerWidth': 100, 'containerHeight': 100, 'width': 100, 'height': 100}
                                    ],
                                }
                            ],
                            'shop': {
                                'id': 1,
                                'name': 'test_shop_1',
                                'gradesCount': 0,
                                'outletsCount': 0,
                                'storesCount': 0,
                                'pickupStoresCount': 0,
                                'postomatStoresCount': 0,
                                'bookNowStoresCount': 0,
                                'qualityRating': 3,
                                'isGlobal': False,
                            },
                            'hid': 1,
                        },
                        {
                            'entity': 'offer',
                            'wareId': 'S4gNYZu7y76iKxO_owoazg',
                            'titles': {'raw': 'phone iphone 7 plus'},
                            'price': {
                                'currency': 'RUR',
                                'value': '200',
                                'isDeliveryIncluded': False,
                                'rawValue': '200',
                            },
                            'pictures': [
                                {
                                    'entity': 'picture',
                                    'thumbnails': [
                                        {'containerWidth': 100, 'containerHeight': 100, 'width': 100, 'height': 100}
                                    ],
                                }
                            ],
                            'shop': {
                                'id': 1,
                                'name': 'test_shop_1',
                                'gradesCount': 0,
                                'outletsCount': 0,
                                'storesCount': 0,
                                'pickupStoresCount': 0,
                                'postomatStoresCount': 0,
                                'bookNowStoresCount': 0,
                                'qualityRating': 3,
                                'isGlobal': False,
                            },
                            'hid': 1,
                        },
                        {
                            'entity': 'offer',
                            'wareId': 'OpepoVTy9iY4OTRGgVRW_Q',
                            'titles': {'raw': 'phone iphone special'},
                            'price': {
                                'currency': 'RUR',
                                'value': '300',
                                'isDeliveryIncluded': False,
                                'rawValue': '300',
                            },
                            'pictures': [
                                {
                                    'entity': 'picture',
                                    'thumbnails': [
                                        {'containerWidth': 100, 'containerHeight': 100, 'width': 100, 'height': 100}
                                    ],
                                }
                            ],
                            'shop': {
                                'id': 2,
                                'name': 'test_shop_2',
                                'gradesCount': 0,
                                'outletsCount': 0,
                                'storesCount': 0,
                                'pickupStoresCount': 0,
                                'postomatStoresCount': 0,
                                'bookNowStoresCount': 0,
                                'qualityRating': 4,
                                'isGlobal': False,
                            },
                            'hid': 1,
                        },
                    ],
                },
            },
        )

    def test_pager(self):
        response = self.report.request_json('place=brand_products&vendor_id=2&rids=213')
        self.assertFragmentIn(response, {'search': {'total': 27}})
        self.assertEqual(1, response.count({'entity': 'product'}))
        self.assertEqual(24, response.count({'entity': 'offer'}))
        response = self.report.request_json('place=brand_products&vendor_id=2&rids=213&page=1')
        self.assertFragmentIn(response, {'search': {'total': 27}})
        self.assertEqual(1, response.count({'entity': 'product'}))
        self.assertEqual(24, response.count({'entity': 'offer'}))
        response = self.report.request_json('place=brand_products&vendor_id=2&rids=213&page=2')
        self.assertFragmentIn(response, {'search': {'total': 27}})
        self.assertEqual(0, response.count({'entity': 'product'}))
        self.assertEqual(2, response.count({'entity': 'offer'}))

        """Проверяется, что общее количество для показа = total"""
        self.access_log.expect(total_renderable='27').times(3)

    def test_bids(self):
        """
        Что тестируем: секцию bids в ответе
        """
        # Находим верные ставки для vendor_id = 1, если верно зададим pp=18
        response = self.report.request_json('place=brand_products&vendor_id=1&rids=213&pp=18')
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'entity': 'product',
                        'id': 1,
                        'titles': {'raw': 'iphone'},
                        'bids': {'vbid': {'value': 100, 'vendorDatasourceId': 10}},
                    }
                ]
            },
        )

        # Без задания pp - ставок на модель не находим (см. NCgiExtensions::UseVendorBid)
        # В lite-тестах елси не указать pp, то подставится как раз pp=18, что в данном случае не подходит
        response = self.report.request_json('place=brand_products&vendor_id=1&rids=213&pp=-1')
        self.assertFragmentNotIn(
            response,
            {
                'results': [
                    {
                        'entity': 'product',
                        'id': 1,
                        'titles': {'raw': 'iphone'},
                        'bids': {'vbid': {'value': 100, 'vendorDatasourceId': 10}},
                    }
                ]
            },
        )

        # Для vendor_id = 2 нет, ставок - не находим в выдаче моделей поле 'bids'
        response = self.report.request_json('place=brand_products&vendor_id=2&pp=18')
        self.assertFragmentNotIn(
            response,
            {
                'results': [
                    {'entity': 'product', 'bids': NotEmpty()},
                ]
            },
        )

    def test_sortings_format(self):
        """
        Что тестируем: формат секции sorts в ответе
        Вообще-то мы всегда тут выдавали какую-то чушь, просто тесты тестировали порядок ключей в хеше
        """
        response = self.report.request_json('place=brand_products&vendor_id=3')
        self.assertFragmentIn(
            response,
            {
                "sorts": [
                    {"text": "по популярности"},
                    {"text": "по популярности", "options": [{"id": "guru_popularity"}]},
                    {"text": "по цене", "options": [{"id": "aprice", "type": "asc"}, {"id": "dprice", "type": "desc"}]},
                    {"text": "по цене", "options": [{"id": "dprice", "type": "desc"}]},
                    {"text": "по новизне", "options": [{"id": "ddate"}]},
                    {"text": "по рейтингу", "options": [{"id": "quality"}]},
                    {"text": "по отзывам", "options": [{"id": "opinions"}]},
                    {"text": "по количеству магазинов, продающих модель", "options": [{"id": "number_of_shops"}]},
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def test_sorting_aprice(self):
        """
        Что тестируем: сортировку по возрастанию цены
        """
        response = self.report.request_json('place=brand_products&vendor_id=3&how=aprice')
        self.assertFragmentIn(
            response, {"search": {"total": 7, "totalOffers": 4, "totalModels": 3}}, preserve_order=True
        )
        self.assertFragmentIn(
            response,
            [
                {
                    "entity": "product",
                    "id": 3,
                    "titles": {"raw": "One"},
                    "prices": {"min": "200", "max": "200"},
                    "opinions": 4,
                    "rating": 4,
                },
                {
                    "entity": "product",
                    "id": 4,
                    "titles": {"raw": "Desire"},
                    "prices": {"min": "300", "max": "600"},
                    "opinions": 2,
                    "rating": 4.5,
                },
                {
                    "entity": "product",
                    "id": 5,
                    "titles": {"raw": "Incredible S"},
                    "prices": {"min": "400", "max": "400"},
                    "opinions": 5,
                    "rating": 3.5,
                },
                {"entity": "offer", "titles": {"raw": "HTC One"}, "price": {"value": "200"}},
                {"entity": "offer", "titles": {"raw": "HTC Desire 828"}, "price": {"value": "300"}},
                {"entity": "offer", "titles": {"raw": "HTC Incredible S"}, "price": {"value": "400"}},
                {"entity": "offer", "titles": {"raw": "HTC Desire 620"}, "price": {"value": "600"}},
            ],
            preserve_order=True,
            allow_different_len=False,
        )

    def test_sorting_dprice(self):
        """
        Что тестируем: сортировку по убыванию цены
        """
        response = self.report.request_json('place=brand_products&vendor_id=3&how=dprice&show-msku=1')
        self.assertFragmentIn(
            response, {"search": {"total": 7, "totalOffers": 4, "totalModels": 3}}, preserve_order=True
        )
        self.assertFragmentIn(
            response,
            [
                {
                    "entity": "product",
                    "id": 5,
                    "titles": {"raw": "Incredible S"},
                    "prices": {"min": "400", "max": "400"},
                    "opinions": 5,
                    "rating": 3.5,
                },
                {
                    "entity": "product",
                    "id": 4,
                    "titles": {"raw": "Desire"},
                    "prices": {"min": "300", "max": "600"},
                    "opinions": 2,
                    "rating": 4.5,
                },
                {
                    "entity": "product",
                    "id": 3,
                    "titles": {"raw": "One"},
                    "prices": {"min": "200", "max": "200"},
                    "opinions": 4,
                    "rating": 4,
                },
                {"entity": "offer", "titles": {"raw": "HTC Desire 620"}, "price": {"value": "600"}},
                {"entity": "offer", "titles": {"raw": "HTC Incredible S"}, "price": {"value": "400"}},
                {"entity": "offer", "titles": {"raw": "HTC Desire 828"}, "price": {"value": "300"}},
                {"entity": "offer", "titles": {"raw": "HTC One"}, "price": {"value": "200"}},
            ],
            preserve_order=True,
            allow_different_len=False,
        )

    def test_sorting_popularity(self):
        """Что тестируем: сортировку по популярности (guru_popularity).
        Просто сортировка по guru_popularity (без ставок)
        Т.к. у офферов одинаковая guru_popularity то их сортировака получается по randx
        """
        response = self.report.request_json('place=brand_products&vendor_id=3&rids=213&how=guru_popularity&debug=da')

        self.assertFragmentNotIn(response, {"how": {"main": Contains("search_auction_params {")}})
        self.assertFragmentIn(
            response, {"search": {"total": 7, "totalOffers": 4, "totalModels": 3}}, preserve_order=True
        )
        self.assertFragmentIn(
            response,
            [
                {
                    "entity": "product",
                    "id": 4,
                    "titles": {"raw": "Desire"},
                    "prices": {"min": "300", "max": "600"},
                    "opinions": 2,
                    "rating": 4.5,
                },
                {
                    "entity": "product",
                    "id": 5,
                    "titles": {"raw": "Incredible S"},
                    "prices": {"min": "400", "max": "400"},
                    "opinions": 5,
                    "rating": 3.5,
                },
                {
                    "entity": "product",
                    "id": 3,
                    "titles": {"raw": "One"},
                    "prices": {"min": "200", "max": "200"},
                    "opinions": 4,
                    "rating": 4,
                },
                {"entity": "offer", "titles": {"raw": "HTC Incredible S"}, "price": {"value": "400"}},
                {"entity": "offer", "titles": {"raw": "HTC Desire 620"}, "price": {"value": "600"}},
                {"entity": "offer", "titles": {"raw": "HTC One"}, "price": {"value": "200"}},
                {"entity": "offer", "titles": {"raw": "HTC Desire 828"}, "price": {"value": "300"}},
            ],
            preserve_order=True,
            allow_different_len=False,
        )

    def test_sorting_date(self):
        """
        Что тестируем: сортировку по новизне. Не применимо для офферов, у них нет created_ts
        """
        response = self.report.request_json('place=brand_products&vendor_id=3&how=ddate')
        self.assertFragmentIn(
            response, {"search": {"total": 7, "totalOffers": 4, "totalModels": 3}}, preserve_order=True
        )
        self.assertFragmentIn(
            response,
            [
                {
                    "entity": "product",
                    "id": 4,
                    "titles": {"raw": "Desire"},
                    "prices": {"min": "300", "max": "600"},
                    "opinions": 2,
                    "rating": 4.5,
                },
                {
                    "entity": "product",
                    "id": 3,
                    "titles": {"raw": "One"},
                    "prices": {"min": "200", "max": "200"},
                    "opinions": 4,
                    "rating": 4,
                },
                {
                    "entity": "product",
                    "id": 5,
                    "titles": {"raw": "Incredible S"},
                    "prices": {"min": "400", "max": "400"},
                    "opinions": 5,
                    "rating": 3.5,
                },
            ],
            preserve_order=True,
        )

    def test_sorting_quality(self):
        """
        Что тестируем: сортировку по рейтингу.
        Для офферов рейтинг - qualityRating магазина, для моделей - rating
        """
        response = self.report.request_json('place=brand_products&vendor_id=3&how=quality')
        self.assertFragmentIn(
            response, {"search": {"total": 7, "totalOffers": 4, "totalModels": 3}}, preserve_order=True
        )
        self.assertFragmentIn(
            response,
            [
                {
                    "entity": "product",
                    "id": 4,
                    "titles": {"raw": "Desire"},
                    "prices": {"min": "300", "max": "600"},
                    "opinions": 2,
                    "rating": 4.5,
                },
                {
                    "entity": "product",
                    "id": 3,
                    "titles": {"raw": "One"},
                    "prices": {"min": "200", "max": "200"},
                    "opinions": 4,
                    "rating": 4,
                },
                {
                    "entity": "product",
                    "id": 5,
                    "titles": {"raw": "Incredible S"},
                    "prices": {"min": "400", "max": "400"},
                    "opinions": 5,
                    "rating": 3.5,
                },
                {
                    "entity": "offer",
                    "titles": {"raw": "HTC One"},
                    "shop": {"id": 3, "qualityRating": 5},
                    "price": {"value": "200"},
                },
                {
                    "entity": "offer",
                    "titles": {"raw": "HTC Desire 620"},
                    "shop": {"id": 2, "qualityRating": 4},
                    "price": {"value": "600"},
                },
                {
                    "entity": "offer",
                    "titles": {"raw": "HTC Desire 828"},
                    "shop": {"id": 1, "qualityRating": 3},
                    "price": {"value": "300"},
                },
                {
                    "entity": "offer",
                    "titles": {"raw": "HTC Incredible S"},
                    "shop": {"id": 7, "qualityRating": 2},
                    "price": {"value": "400"},
                },
            ],
            preserve_order=True,
            allow_different_len=False,
        )

    def test_sorting_opinions(self):
        """
        Что тестируем: сортировку по количеству отзывов. Не применимо для офферов (не матчатся к моделям при сортировке SF_OPINIONS)
        """
        response = self.report.request_json('place=brand_products&vendor_id=3&how=opinions')
        self.assertFragmentIn(
            response, {"search": {"total": 7, "totalOffers": 4, "totalModels": 3}}, preserve_order=True
        )
        self.assertFragmentIn(
            response,
            [
                {
                    "entity": "product",
                    "id": 5,
                    "titles": {"raw": "Incredible S"},
                    "prices": {"min": "400", "max": "400"},
                    "opinions": 5,
                    "rating": 3.5,
                },
                {
                    "entity": "product",
                    "id": 3,
                    "titles": {"raw": "One"},
                    "prices": {"min": "200", "max": "200"},
                    "opinions": 4,
                    "rating": 4,
                },
                {
                    "entity": "product",
                    "id": 4,
                    "titles": {"raw": "Desire"},
                    "prices": {"min": "300", "max": "600"},
                    "opinions": 2,
                    "rating": 4.5,
                },
            ],
            preserve_order=True,
        )

    @classmethod
    def prepare_sorting_shops(cls):
        cls.index.models += [
            Model(hyperid=143, hid=123, title='Dayakkaiser', vendor_id=143, ts=3),
            Model(hyperid=144, hid=123, title='King Kittan', vendor_id=143, ts=2),
            Model(hyperid=145, hid=123, title='Gurren Lagann', vendor_id=143, ts=1),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1).respond(0.02)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 2).respond(0.03)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 3).respond(0.04)

        cls.index.offers += [
            Offer(
                title='Dayakkaiser',
                vendor_id=143,
                hyperid=143,
                hid=123,
                price=100,
                fesh=11,
                waremd5='5AvY3NaPMZRGBLyPr8qI_C',
            ),
            Offer(
                title='Space Dayakkaiser',
                vendor_id=143,
                hyperid=143,
                hid=123,
                price=100,
                fesh=12,
                waremd5='5AvY3NaPMZRGBLyPr8qI_B',
                cpa=Offer.CPA_REAL,
            ),
            Offer(
                title='King Kittan',
                vendor_id=143,
                hyperid=144,
                hid=123,
                price=100,
                fesh=11,
                waremd5='5AvY3NaPMZRGBLyPr8qI_F',
            ),
            Offer(
                title='King Kittan Giga Drill Break',
                vendor_id=143,
                hyperid=144,
                hid=123,
                price=100,
                fesh=12,
                waremd5='5AvY3NaPMZRGBLyPr8qI_E',
            ),
            Offer(
                title='King Kittan Stinger',
                vendor_id=143,
                hyperid=144,
                hid=123,
                price=100,
                fesh=13,
                waremd5='5AvY3NaPMZRGBLyPr8qI_D',
            ),
            Offer(
                title='Tengen Toppa Gurren Lagann',
                vendor_id=143,
                hyperid=145,
                hid=123,
                price=100,
                fesh=11,
                waremd5='5AvY3NaPMZRGBLyPr8qI_G',
            ),
            Offer(
                title='Super Galaxy Gurren Lagann',
                vendor_id=143,
                hyperid=145,
                hid=123,
                price=200,
                fesh=12,
                waremd5='S4gNYZG7y76iKxO_owoazI',
            ),
            Offer(
                title='Arc-Gurren Lagann',
                vendor_id=143,
                hyperid=145,
                hid=123,
                price=300,
                fesh=13,
                waremd5='OpepoVTyGiY4OTRGgVRW_H',
            ),
            Offer(
                title='Gurren Lagann',
                vendor_id=143,
                hyperid=145,
                hid=123,
                price=300,
                fesh=14,
                waremd5='OpepoVTy9iYGOTRGgVRW_j',
                cpa=Offer.CPA_REAL,
            ),
        ]

    def test_sorting_shops(self):
        """
        Что тестируем: сортировку по количеству магазинов, продающих модель. Не применимо для офферов
        (просто не имеет смысла сортировать оффера данным образом)
        """
        response = self.report.request_json('place=brand_products&vendor_id=143&how=number_of_shops&entities=product')
        self.assertFragmentIn(
            response,
            [
                {
                    "entity": "product",
                    "id": 145,
                },
                {
                    "entity": "product",
                    "id": 144,
                },
                {
                    "entity": "product",
                    "id": 143,
                },
            ],
            preserve_order=True,
        )

    def test_check_are_models_on_sale(self):
        """
        Что тестируем: при установленном параметре check-models ручка должна поискать оффера для моделей и вернуть список моделей у которых нашлись CPA оффера
        """
        response = self.report.request_json('place=brand_products&check-models=1&hyperid=143,144,145')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"modelId": 143},
                    {"modelId": 145},
                ]
            },
            allow_different_len=False,
        )
        self.assertFragmentNotIn(
            response,
            {
                "results": [
                    {"modelId": 144},
                ]
            },
        )

    def test_check_are_models_on_sale_wrong_cgi(self):
        """
        Что тестируем: при установленном параметре check-models ручка должна ругаться, если не заданно ни одной модели
        """
        response = self.report.request_json('place=brand_products&check-models=1')
        self.assertFragmentIn(
            response,
            {
                "error": {
                    "code": "INVALID_USER_CGI",
                    "message": "In case of check-models is set at least one hyper id should be specified",
                }
            },
            allow_different_len=False,
        )
        self.error_log.expect(code=3043)

    def test_filter_hyperid(self):
        """
        Что тестируем: фильтр по hyperid (id модели)
        """
        response = self.report.request_json('place=brand_products&vendor_id=4&hyperid=7')
        self.assertFragmentIn(response, {"search": {"total": 3, "totalOffers": 2, "totalModels": 1}})
        self.assertFragmentIn(
            response,
            [
                {"entity": "product", "id": 7, "titles": {"raw": "A1000"}},
                {"entity": "offer", "titles": {"raw": "Lenovo A1000"}, "shop": {"id": 4}, "price": {"value": "1200"}},
                {
                    "entity": "offer",
                    "titles": {"raw": "Смартфон Lenovo A1000"},
                    "shop": {"id": 5},
                    "price": {"value": "1300"},
                },
            ],
        )

    def test_filter_entity(self):
        """
        Что тестируем: фильтр по типу товара (оффер/модель)
        """
        response = self.report.request_json('place=brand_products&vendor_id=4&entities=product')
        self.assertFragmentIn(response, {"search": {"total": 4, "totalOffers": 0, "totalModels": 4}})
        self.assertFragmentIn(
            response,
            [
                {"entity": "product", "id": 6, "titles": {"raw": "Vibe Shot"}},
                {"entity": "product", "id": 7, "titles": {"raw": "A1000"}},
                {"entity": "product", "id": 8, "titles": {"raw": "IdeaPad 100 15"}},
                {"entity": "product", "id": 9, "titles": {"raw": "B50 45"}},
            ],
        )

        response = self.report.request_json('place=brand_products&vendor_id=4&entities=offer')
        self.assertFragmentIn(response, {"search": {"total": 6, "totalOffers": 6, "totalModels": 0}})
        self.assertFragmentIn(
            response,
            [
                {
                    "entity": "offer",
                    "titles": {"raw": "Lenovo Vibe Shot"},
                    "shop": {"id": 4},
                    "price": {"value": "1000"},
                },
                {"entity": "offer", "titles": {"raw": "Lenovo A1000"}, "shop": {"id": 4}, "price": {"value": "1200"}},
                {
                    "entity": "offer",
                    "titles": {"raw": "Смартфон Lenovo A1000"},
                    "shop": {"id": 5},
                    "price": {"value": "1300"},
                },
                {
                    "entity": "offer",
                    "titles": {"raw": "Lenovo IdeaPad 100 15"},
                    "shop": {"id": 6},
                    "price": {"value": "2000"},
                },
                {"entity": "offer", "titles": {"raw": "Lenovo B50 45"}, "shop": {"id": 4}, "price": {"value": "2100"}},
                {"entity": "offer", "titles": {"raw": "Lenovo VibeShot msku"}},  # какой-то оффер из синих офферов msku
            ],
        )

    def test_filter_price(self):
        """
        Что тестируем: фильтр по цене
        """
        response = self.report.request_json('place=brand_products&vendor_id=4&mcpricefrom=1250&mcpriceto=2000')
        self.assertFragmentIn(response, {"search": {"total": 4, "totalOffers": 2, "totalModels": 2}})
        self.assertFragmentIn(
            response,
            [
                {"entity": "product", "id": 7, "titles": {"raw": "A1000"}, "prices": {"min": "1200", "max": "1300"}},
                {
                    "entity": "product",
                    "id": 8,
                    "titles": {"raw": "IdeaPad 100 15"},
                    "prices": {"min": "2000", "max": "2000"},
                },
                {
                    "entity": "offer",
                    "titles": {"raw": "Смартфон Lenovo A1000"},
                    "shop": {"id": 5},
                    "price": {"value": "1300"},
                },
                {
                    "entity": "offer",
                    "titles": {"raw": "Lenovo IdeaPad 100 15"},
                    "shop": {"id": 6},
                    "price": {"value": "2000"},
                },
            ],
        )

    def test_filter_hid(self):
        """
        Что тестируем: фильтр по hid (id категории)
        """
        response = self.report.request_json('place=brand_products&vendor_id=4&hid=3')
        self.assertFragmentIn(response, {"search": {"total": 4, "totalOffers": 2, "totalModels": 2}})
        self.assertFragmentIn(
            response,
            [
                {"entity": "product", "id": 8, "titles": {"raw": "IdeaPad 100 15"}},
                {"entity": "product", "id": 9, "titles": {"raw": "B50 45"}},
                {
                    "entity": "offer",
                    "titles": {"raw": "Lenovo IdeaPad 100 15"},
                    "shop": {"id": 6},
                    "price": {"value": "2000"},
                },
                {"entity": "offer", "titles": {"raw": "Lenovo B50 45"}, "shop": {"id": 4}, "price": {"value": "2100"}},
            ],
        )

    def test_filter_vbid(self):
        """
        Что тестируем: фильтр по вендорской ставке. При данной фильтрации выдаются только модели!
        """
        response = self.report.request_json('place=brand_products&vendor_id=4&vbid-from=200&vbid-to=210&show-msku=1')
        self.assertFragmentIn(response, {"search": {"total": 1, "totalOffers": 0, "totalModels": 1}})
        self.assertFragmentIn(
            response,
            [
                {
                    "entity": "product",
                    "id": 8,
                    "titles": {"raw": "IdeaPad 100 15"},
                    "bids": {"vbid": {"value": 200, "vendorDatasourceId": 4}},
                },
            ],
        )

        # При данной фильтрации модели со ставкой = 0 в индексе идентичны моделям без ставки в индексе
        response = self.report.request_json('place=brand_products&vendor_id=4&vbid-from=0')
        self.assertFragmentIn(response, {"search": {"total": 4, "totalOffers": 0, "totalModels": 4}})
        self.assertFragmentIn(
            response,
            [
                {
                    "entity": "product",
                    "id": 6,
                    "titles": {"raw": "Vibe Shot"},
                    "bids": {"vbid": {"value": 0, "vendorDatasourceId": 4}},
                },
                {"entity": "product", "id": 7, "titles": {"raw": "A1000"}},
                {
                    "entity": "product",
                    "id": 8,
                    "titles": {"raw": "IdeaPad 100 15"},
                    "bids": {"vbid": {"value": 200, "vendorDatasourceId": 4}},
                },
                {
                    "entity": "product",
                    "id": 9,
                    "titles": {"raw": "B50 45"},
                    "bids": {"vbid": {"value": 250, "vendorDatasourceId": 4}},
                },
            ],
        )

    def test_filter_text(self):
        """
        Что тестируем: фильтр по тексту
        """
        response = self.report.request_json('place=brand_products&vendor_id=4&text=A1000')
        self.assertFragmentIn(response, {"search": {"total": 3, "totalOffers": 2, "totalModels": 1}})
        self.assertFragmentIn(
            response,
            [
                {"entity": "product", "id": 7, "titles": {"raw": "A1000"}},
                {"entity": "offer", "titles": {"raw": "Lenovo A1000"}, "shop": {"id": 4}, "price": {"value": "1200"}},
                {
                    "entity": "offer",
                    "titles": {"raw": "Смартфон Lenovo A1000"},
                    "shop": {"id": 5},
                    "price": {"value": "1300"},
                },
            ],
        )

    def test_filter_barcode(self):
        """
        Что тестируем: фильтр по баркоду
        """
        response = self.report.request_json('place=brand_products&vendor_id=4&text=barcode:12345')
        self.assertFragmentIn(response, {"search": {"total": 1, "totalOffers": 0, "totalModels": 1}})
        self.assertFragmentIn(
            response,
            [
                {"entity": "product", "id": 6, "titles": {"raw": "Vibe Shot"}},
            ],
        )

    def test_filter_rids(self):
        """
        Что тестируем: фильтр по регионам доставки
        """
        response = self.report.request_json('place=brand_products&vendor_id=4&rids=228&onstock=1')
        self.assertFragmentIn(response, {"search": {"total": 2, "totalOffers": 1, "totalModels": 1}})
        self.assertFragmentIn(
            response,
            [
                {"entity": "product", "id": 8, "titles": {"raw": "IdeaPad 100 15"}},
                {
                    "entity": "offer",
                    "titles": {"raw": "Lenovo IdeaPad 100 15"},
                    "shop": {"id": 6},
                    "price": {"value": "2000"},
                },
            ],
        )

    def test_minimal_vbid(self):
        """
        Что тестируем: выдачу минимальной вендорской ставки (minVBid)
        Минимальная ставка присутствует в выдаче для моделей даже если нет фактической ставки vbid
        Считаем мин. ставку для модели по поисковой категорийной формуле для офферов,
        в качестве цены берем средняя цена модели по всем регионам (а не только по офферам, найденным в регионе запроса)
        """

        # имеем 2 оффера, средняя цена на модель = 1250
        # hid = 1 -> коэф. для формулы заданы: coefficient=0.08, power=0.7, maximumBid=45
        # 0.08*1250^0.7=11.77 -> ceil -> 12 -> *0.8 = 9.6 -> trunk -> 9
        response = self.report.request_json('place=brand_products&vendor_id=4&hyperid=7')
        self.assertFragmentIn(response, {"search": {"total": 3, "totalOffers": 2, "totalModels": 1}})
        self.assertFragmentIn(
            response,
            [
                {"entity": "product", "id": 7, "prices": {"min": "1200", "max": "1300"}, "minVBid": 1},
            ],
        )

        # офферов запросу с заданным регионом нет, но т.к. сред. цену считаем по всем регионам - мин. ставка есть
        response = self.report.request_json('place=brand_products&vendor_id=4&hyperid=7&rids=228')
        self.assertFragmentIn(response, {"search": {"total": 1, "totalOffers": 0, "totalModels": 1}})
        self.assertFragmentIn(
            response,
            [
                {"entity": "product", "id": 7, "minVBid": 1},
            ],
        )

        # hid = 3 -> коэф. для формулы заданы: coefficient=0.055, power=0.56, maximumBid=45
        # 0.0055*2000^0.56=3.88 -> ceil -> 4 -> *0.8 = 3.2 -> trunk -> 3
        response = self.report.request_json('place=brand_products&vendor_id=4&hyperid=8')
        self.assertFragmentIn(response, {"search": {"total": 2, "totalOffers": 1, "totalModels": 1}})
        self.assertFragmentIn(
            response,
            [
                {"entity": "product", "id": 8, "minVBid": 2},
            ],
        )

    def test_modelbid_cutoff(self):
        """Что проверяем: отключение ставок на модель для vendor_ds_id, перечисленных в cutoff_modelbids_datasources.tsv
        Ставки на модели с такими vendor_ds_id устанавливаются в 0 (и в таком виде остаются в выводе плейса)
        """
        self.dynamic.disabled_vendor_model_bids += [
            DynamicVendorModelBid(1),
            DynamicVendorModelBid(3),
        ]

        response = self.report.request_json('place=brand_products&vendor_id=5')
        self.assertFragmentIn(
            response,
            [
                {"entity": "product", "id": 10, "bids": {"vbid": {"value": 0, "vendorDatasourceId": 1}}},
                {"entity": "product", "id": 11, "bids": {"vbid": {"value": 200, "vendorDatasourceId": 2}}},
                {"entity": "product", "id": 12, "bids": {"vbid": {"value": 0, "vendorDatasourceId": 3}}},
                {"entity": "product", "id": 13, "bids": {"vbid": {"value": 0, "vendorDatasourceId": 3}}},
            ],
        )

        self.dynamic.disabled_vendor_model_bids.clear()

        response = self.report.request_json('place=brand_products&vendor_id=5')
        self.assertFragmentIn(
            response,
            [
                {"entity": "product", "id": 10, "bids": {"vbid": {"value": 100, "vendorDatasourceId": 1}}},
                {"entity": "product", "id": 11, "bids": {"vbid": {"value": 200, "vendorDatasourceId": 2}}},
                {"entity": "product", "id": 12, "bids": {"vbid": {"value": 300, "vendorDatasourceId": 3}}},
                {"entity": "product", "id": 13, "bids": {"vbid": {"value": 400, "vendorDatasourceId": 3}}},
            ],
        )

    def test_place_msku_only(self):
        response = self.report.request_json('place=brand_products&vendor_id=3&msku-only=1')
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 4,
                    'totalOffers': 0,
                    'totalModels': 0,
                    'results': [
                        {
                            'entity': 'sku',
                            'id': '10002',
                            'titles': {'raw': 'HTC Incredible S msku'},
                            'product': {'id': 5},
                            'hid': 1,
                            'vendor': {'id': 3, 'name': 'HTC'},
                            'pictures': [
                                {
                                    'entity': 'picture',
                                    'thumbnails': [
                                        {'containerWidth': 100, 'containerHeight': 100, 'width': 100, 'height': 100}
                                    ],
                                }
                            ],
                        },
                        {
                            'entity': 'sku',
                            'id': '10004',
                            'titles': {'raw': 'HTC Incredible S msku2'},
                            'product': {'id': 5},
                            'hid': 1,
                            'vendor': {'id': 3, 'name': 'HTC'},
                            'pictures': [
                                {
                                    'entity': 'picture',
                                    'thumbnails': [
                                        {'containerWidth': 100, 'containerHeight': 100, 'width': 100, 'height': 100}
                                    ],
                                }
                            ],
                        },
                        {
                            'entity': 'sku',
                            'id': '10001',
                            'titles': {'raw': 'HTC Desire msku'},
                            'product': {'id': 4},
                            'hid': 1,
                            'vendor': {'id': 3, 'name': 'HTC'},
                            'pictures': [
                                {
                                    'entity': 'picture',
                                    'thumbnails': [
                                        {'containerWidth': 100, 'containerHeight': 100, 'width': 100, 'height': 100}
                                    ],
                                }
                            ],
                        },
                        {
                            'entity': 'sku',
                            'id': '10000',
                            'titles': {'raw': 'HTC One msku'},
                            'product': {'id': 3},
                            'hid': 1,
                            'vendor': {'id': 3, 'name': 'HTC'},
                            'pictures': [
                                {
                                    'entity': 'picture',
                                    'thumbnails': [
                                        {'containerWidth': 100, 'containerHeight': 100, 'width': 100, 'height': 100}
                                    ],
                                }
                            ],
                        },
                    ],
                },
            },
        )

    def test_place_and_msku(self):
        response = self.report.request_json('place=brand_products&vendor_id=3&show-msku=1')
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 7,
                    'totalOffers': 4,
                    'totalModels': 3,
                    'results': [
                        {
                            'entity': 'product',
                            'id': 4,
                            'titles': {'raw': 'Desire'},
                            'mskus': [
                                {
                                    'entity': 'sku',
                                    'id': '10001',
                                    'titles': {'raw': 'HTC Desire msku'},
                                }
                            ],
                        },
                        {
                            'entity': 'product',
                            'id': 5,
                            'titles': {'raw': 'Incredible S'},
                            'mskus': [
                                {
                                    'entity': 'sku',
                                    'id': '10002',
                                    'titles': {'raw': 'HTC Incredible S msku'},
                                },
                                {
                                    'entity': 'sku',
                                    'id': '10004',
                                    'titles': {'raw': 'HTC Incredible S msku2'},
                                },
                            ],
                        },
                        {
                            'entity': 'product',
                            'id': 3,
                            'titles': {'raw': 'One'},
                            'mskus': [
                                {
                                    'entity': 'sku',
                                    'id': '10000',
                                    'titles': {'raw': 'HTC One msku'},
                                }
                            ],
                        },
                        {'entity': 'offer', 'wareId': 'Mlwpwkxmkwpwafbbrtyf_A', 'titles': {'raw': 'HTC Incredible S'}},
                        {'entity': 'offer', 'wareId': 'Yedweodedbjkejnwjejn_Q', 'titles': {'raw': 'HTC Desire 620'}},
                        {'entity': 'offer', 'wareId': 'WksdadsfkpdjOTRGgVRW_A', 'titles': {'raw': 'HTC Desire 828'}},
                        {'entity': 'offer', 'wareId': 'Pamdhwpy9iY4OTRGgVRW_Q', 'titles': {'raw': 'HTC One'}},
                    ],
                },
            },
        )

    def test_filter_hyperid_msku(self):
        """
        Что тестируем: фильтр по hyperid для msku
        """
        response = self.report.request_json('place=brand_products&vendor_id=3&hyperid=3&msku-only=1')
        self.assertFragmentIn(
            response,
            [
                {"entity": "sku", "id": "10000", "titles": {"raw": "HTC One msku"}},
            ],
            allow_different_len=False,
        )

    def test_filter_hid_msku(self):
        """
        Что тестируем: фильтр по hid для msku
        """
        response = self.report.request_json('place=brand_products&vendor_id=3&hid=1&msku-only=1')
        self.assertFragmentIn(
            response,
            [
                {"entity": "sku", "id": "10002", "titles": {"raw": "HTC Incredible S msku"}},
                {"entity": "sku", "id": "10001", "titles": {"raw": "HTC Desire msku"}},
                {"entity": "sku", "id": "10000", "titles": {"raw": "HTC One msku"}},
                {"entity": "sku", "id": "10004", "titles": {"raw": "HTC Incredible S msku2"}},
            ],
            allow_different_len=False,
        )

        response = self.report.request_json('place=brand_products&vendor_id=3&hid=2&msku-only=1')
        self.assertFragmentIn(response, {"search": {"total": 0, "totalOffers": 0, "totalModels": 0}})

    def test_filter_text_msku(self):
        """
        Что тестируем: фильтр по тексту для msku
        """
        response = self.report.request_json('place=brand_products&vendor_id=3&text=One&msku-only=1')
        self.assertFragmentIn(
            response,
            [
                {"entity": "sku", "id": "10000", "titles": {"raw": "HTC One msku"}},
            ],
            allow_different_len=False,
        )

    def test_filter_by_licensor(self):
        """
        Что тестируем: фильтр по лицензиару
        """
        response = self.report.request_json('place=brand_products&licensor=1')
        self.assertFragmentIn(
            response,
            [
                {"entity": "product", "titles": {"raw": "model15"}},
                {"entity": "offer", "titles": {"raw": "offer6_2"}},
            ],
            allow_different_len=False,
        )

    def test_filter_by_franchise(self):
        """
        Что тестируем: фильтр по франшизе
        """
        response = self.report.request_json('place=brand_products&franchise=3')
        self.assertFragmentIn(
            response,
            [
                {"entity": "product", "titles": {"raw": "model17"}},
                {"entity": "product", "titles": {"raw": "model18"}},
                {"entity": "offer", "titles": {"raw": "offer7_2"}},
                {"entity": "offer", "titles": {"raw": "offer7_3"}},
            ],
            allow_different_len=False,
        )

    def test_filter_by_character(self):
        """
        Что тестируем: фильтр по персонажу
        """
        response = self.report.request_json('place=brand_products&character=4')
        self.assertFragmentIn(
            response,
            [
                {"entity": "product", "titles": {"raw": "model18"}},
                {"entity": "offer", "titles": {"raw": "offer7_3"}},
            ],
            allow_different_len=False,
        )

    def test_filter_by_vendor_and_character(self):
        """
        Что тестируем: фильтр по вендору и персонажу
        """
        response = self.report.request_json('place=brand_products&vendor_id=7&character=2')
        self.assertFragmentIn(
            response,
            [
                {"entity": "product", "titles": {"raw": "model16"}},
                {"entity": "offer", "titles": {"raw": "offer7_1"}},
            ],
            allow_different_len=False,
        )

    def test_filter_by_vendor_licensor_and_characters(self):
        """
        Что тестируем: фильтр по вендору, лицензиару и нескольким персонажам
        """
        response = self.report.request_json('place=brand_products&vendor_id=7&licensor=3&character=2,4')
        self.assertFragmentIn(
            response,
            [
                {"entity": "product", "titles": {"raw": "model18"}},
                {"entity": "offer", "titles": {"raw": "offer7_3"}},
            ],
            allow_different_len=False,
        )

    def test_filter_by_text_only(self):
        """
        Что тестируем: фильтр по тексту
        """
        response = self.report.request_json('place=brand_products&text=incredible')
        self.assertFragmentIn(
            response,
            [
                {"entity": "product", "titles": {"raw": "Incredible S"}},
                {"entity": "offer", "titles": {"raw": "HTC Incredible S"}},
            ],
            allow_different_len=False,
        )

    def test_msku_prices(self):
        """
        Что тестируем: вывод мин. и макс. цены msku
        """
        response = self.report.request_json('place=brand_products&vendor_id=4&msku-only=1')
        self.assertFragmentIn(
            response,
            [
                {
                    "entity": "sku",
                    "id": "10003",
                    "titles": {"raw": "Lenovo VibeShot msku"},
                    'prices': {'min': '345', 'max': '678'},
                },
            ],
        )

        response = self.report.request_json('place=brand_products&vendor_id=3&msku-only=1')
        self.assertFragmentIn(
            response,
            [
                {
                    "entity": "sku",
                    "id": "10000",
                    "titles": {"raw": "HTC One msku"},
                    'prices': {'min': '100', 'max': '500'},
                },
            ],
        )

    def test_no_search_results(self):
        """
        Тестируем корректность счетчиков и отсутвие результатов при nosearchresults=1
        """
        response = self.report.request_json('place=brand_products&vendor_id=3&nosearchresults=1')
        self.assertFragmentIn(response, {"search": {"total": 7, "totalOffers": 4, "totalModels": 3, "results": []}})

    def test_recommended_retailers_count(self):
        """
        Тестируем счетчик рекомендованных вендором магазинов
        """
        response = self.report.request_json('place=brand_products&vendor_id=3&rids=213')
        self.assertFragmentIn(
            response,
            [
                {"entity": "product", "id": 4, "retailerCount": "2", "recommendedRetailerCount": "2"},
                {"entity": "product", "id": 5, "retailerCount": "1", "recommendedRetailerCount": "0"},
                {"entity": "product", "id": 3, "retailerCount": "1", "recommendedRetailerCount": "1"},
            ],
        )

    @classmethod
    def prepare_priority_model(cls):
        cls.index.models += [
            Model(
                hyperid=126,
                hid=121,
                title='Daedric Battle Axe',
                vendor_id=141,
                vbid=0,
                opinion=Opinion(total_count=3, rating=4.5),
            ),
            Model(
                hyperid=127,
                hid=121,
                title='Daedric Claymore',
                vendor_id=141,
                opinion=Opinion(total_count=3, rating=4.0),
            ),
            Model(
                hyperid=128,
                hid=121,
                title='Daedric Club',
                vendor_id=141,
                vbid=200,
                opinion=Opinion(total_count=3, rating=3.5),
            ),
            Model(
                hyperid=129,
                hid=121,
                title='Daedric Dagger',
                vendor_id=141,
                vbid=250,
                opinion=Opinion(total_count=3, rating=3.0),
            ),
            Model(
                hyperid=130,
                hid=122,
                title='Ebony Battle Axe',
                vendor_id=141,
                vbid=0,
                opinion=Opinion(total_count=3, rating=2.5),
            ),
            Model(
                hyperid=131, hid=122, title='Ebony Claymore', vendor_id=141, opinion=Opinion(total_count=3, rating=2.0)
            ),
            Model(
                hyperid=132,
                hid=122,
                title='Ebony Club',
                vendor_id=141,
                vbid=200,
                opinion=Opinion(total_count=3, rating=1.5),
            ),
            Model(
                hyperid=133,
                hid=122,
                title='Ebony Dagger',
                vendor_id=141,
                vbid=250,
                opinion=Opinion(total_count=3, rating=1.0),
            ),
        ]

        cls.index.regional_models += [RegionalModel(hyperid=126 + i, has_cpa=i % 2, rids=[0]) for i in range(8)]

    def test_priority_model(self):
        """
        Проверяем всплывание приоритетных моделей, указанных в параметре priority-models: снача модели упорядочены по убывванию рейтенга, потом высплывают заданные
        """
        response = self.report.request_json('place=brand_products&vendor_id=141&how=quality')

        self.assertFragmentIn(
            response,
            [
                {
                    "entity": "product",
                    "id": 126,
                },
                {
                    "entity": "product",
                    "id": 127,
                },
                {
                    "entity": "product",
                    "id": 128,
                },
                {
                    "entity": "product",
                    "id": 129,
                },
                {
                    "entity": "product",
                    "id": 130,
                },
                {
                    "entity": "product",
                    "id": 131,
                },
                {
                    "entity": "product",
                    "id": 132,
                },
                {
                    "entity": "product",
                    "id": 133,
                },
            ],
            preserve_order=True,
            allow_different_len=False,
        )

        response = self.report.request_json(
            'place=brand_products&vendor_id=141&how=quality&priority-models=132,127,128'
        )

        self.assertFragmentIn(
            response,
            [
                {
                    "entity": "product",
                    "id": 127,
                },
                {
                    "entity": "product",
                    "id": 128,
                },
                {
                    "entity": "product",
                    "id": 132,
                },
                {
                    "entity": "product",
                    "id": 126,
                },
                {
                    "entity": "product",
                    "id": 129,
                },
                {
                    "entity": "product",
                    "id": 130,
                },
                {
                    "entity": "product",
                    "id": 131,
                },
                {
                    "entity": "product",
                    "id": 133,
                },
            ],
            preserve_order=True,
            allow_different_len=False,
        )

    def test_priority_model_and_cpa_first(self):
        """
        Проверяем всплывание приоритетных моделей, указанных в параметре priority-models:
        на всплывание параметр with-cpa-offers-first=1 не влияет
        """
        response = self.report.request_json(
            'place=brand_products&vendor_id=141&how=quality&priority-models=132,127,128&with-cpa-offers-first=1'
        )

        self.assertFragmentIn(
            response,
            [
                {
                    "entity": "product",
                    "id": 127,
                },
                {
                    "entity": "product",
                    "id": 128,
                },
                {
                    "entity": "product",
                    "id": 132,
                },
                {
                    "entity": "product",
                    "id": 129,
                },
                {
                    "entity": "product",
                    "id": 131,
                },
                {
                    "entity": "product",
                    "id": 133,
                },
                {
                    "entity": "product",
                    "id": 126,
                },
                {
                    "entity": "product",
                    "id": 130,
                },
            ],
            preserve_order=True,
            allow_different_len=False,
        )

    def test_filter_text_and_model_id(self):
        """
        Проверяем поиск по тексту и айди модели
        """
        response = self.report.request_json('place=brand_products&text=Claymore&hyperid=127')

        self.assertFragmentIn(response, {"search": {"total": 1, "totalOffers": 0, "totalModels": 1}})
        self.assertFragmentIn(
            response,
            [
                {"entity": "product", "id": 127, "titles": {"raw": "Daedric Claymore"}},
            ],
            preserve_order=True,
            allow_different_len=False,
        )

    @classmethod
    def prepare_is_models_on_sale(cls):
        cls.index.models += [
            Model(
                hyperid=134,
                hid=122,
                title='Knightmare Frame Bamides',
                vendor_id=142,
                vbid=0,
            ),
            Model(
                hyperid=135,
                hid=122,
                title='Knightmare Frame Gekka',
                vendor_id=142,
            ),
        ]
        cls.index.mskus += [
            MarketSku(
                title="Gekka-123",
                hyperid=135,
                sku=11,
                blue_offers=[
                    BlueOffer(feedid=1001),
                ],
            ),
        ]

        cls.index.offers += [
            Offer(hyperid=135),
        ]

    def test_is_models_on_sale(self):
        """
        Проверяем, что проставляется признак наличия активных офферов у модели, т.е. что модель реально продается в данный момент времени
        """
        response = self.report.request_json('place=brand_products&text=Knightmare&vendor_id=142')

        self.assertFragmentIn(response, {"search": {"total": 2, "totalOffers": 0, "totalModels": 2}})
        self.assertFragmentIn(
            response,
            [
                {"entity": "product", "id": 134, "titles": {"raw": "Knightmare Frame Bamides"}, "isOnSale": False},
                {"entity": "product", "id": 135, "titles": {"raw": "Knightmare Frame Gekka"}, "isOnSale": True},
            ],
            preserve_order=False,
            allow_different_len=False,
        )

    def test_is_models_on_sale_disabled(self):
        """
        Проверяем, что НЕ проставляется признак наличия активных офферов у модели, если он выключен флагом
        """
        response = self.report.request_json(
            'place=brand_products&text=Knightmare&vendor_id=142&rearr-factors=market_brand_products_disable_on_sale_check=1'
        )

        self.assertFragmentIn(response, {"search": {"total": 2, "totalOffers": 0, "totalModels": 2}})
        self.assertFragmentIn(
            response,
            [
                {"entity": "product", "id": 134, "titles": {"raw": "Knightmare Frame Bamides"}, "isOnSale": Absent()},
                {"entity": "product", "id": 135, "titles": {"raw": "Knightmare Frame Gekka"}, "isOnSale": Absent()},
            ],
            preserve_order=False,
            allow_different_len=False,
        )

    @classmethod
    def prepare_enrich_request(cls):
        cls.index.vendors += [
            Vendor(vendor_id=15, name='Смысл природы'),
        ]

        cls.index.models += [
            Model(
                hyperid=136,
                hid=124,
                title='Подушка Лебяжий пух',
                vendor_id=15,
            ),
            Model(
                hyperid=137,
                hid=124,
                title='Одеяло Лебяжий Пух',
                vendor_id=15,
            ),
        ]

    def test_enrich_request(self):
        """
        Проверяем, что текстовый поиск работает даже при лишних словах в запросе
        """
        response = self.report.request_json('place=brand_products&text=Лебяжий Пух для линчивания&entities=product')

        self.assertFragmentIn(response, {"search": {"total": 2, "totalOffers": 0, "totalModels": 2}})
        self.assertFragmentIn(
            response,
            [
                {"entity": "product", "id": 136, "titles": {"raw": "Подушка Лебяжий пух"}},
                {"entity": "product", "id": 137, "titles": {"raw": "Одеяло Лебяжий Пух"}},
            ],
            preserve_order=False,
            allow_different_len=False,
        )

    @classmethod
    def prepare_show_medicine_content(cls):
        cls.index.vendors += [
            Vendor(vendor_id=8, name='vendor8'),
        ]

        cls.index.category_restrictions += [
            CategoryRestriction(
                name='medicine',
                hids=[905901],
                regional_restrictions=[
                    RegionalRestriction(
                        show_offers=False,
                        display_only_matched_offers=False,
                        delivery=True,
                        rids=[213],
                    ),
                ],
            )
        ]

        cls.index.hypertree += [
            HyperCategory(hid=905901, name='Лекарства'),
        ]

        cls.index.models += [Model(hyperid=19, title='Лекарство', hid=905901, vendor_id=8)]

        cls.index.offers += [
            Offer(title='Лекарство', vendor_id=8, hyperid=19, price=3000, fesh=1, waremd5='OpepoVTy9iY4OTRGgVRWmn'),
        ]

    def test_show_medicine_content(self):
        """
        Тестируем, что выводим медицинсие товары по умолчанию
        """
        response = self.report.request_json('place=brand_products&vendor_id=8&rids=213')
        self.assertFragmentIn(
            response,
            [
                {"entity": "product", "titles": {"raw": "Лекарство"}},
                {"entity": "offer", "titles": {"raw": "Лекарство"}},
            ],
            allow_different_len=False,
        )

    def test_show_models_grade_agitations(self):
        """
        Проверяем, что для моделек из файла model_grade_agitations.csv
        проставляется grade_agitations
        """

        response = self.report.request_json('place=brand_products&vendor_id=10&rids=213')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "product",
                        "gradeAgitationsCount": 125,
                        "hid": 100500,
                        "id": 25,
                    },
                    {
                        "entity": "product",
                        "gradeAgitationsCount": 5,
                        "hid": 100501,
                        "id": 26,
                    },
                ]
            },
            allow_different_len=False,
        )

        # Если модельки нет в файле или ее grade_agitations == 0,
        # то gradeAgitationsCount не выводится
        response = self.report.request_json('place=brand_products&vendor_id=20&rids=213')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "product",
                        "gradeAgitationsCount": Absent(),
                        "hid": 100502,
                        "id": 27,
                    },
                    {
                        "entity": "product",
                        "gradeAgitationsCount": Absent(),
                        "hid": 100503,
                        "id": 28,
                    },
                ]
            },
            allow_different_len=False,
        )

    @classmethod
    def prepare_with_CPA_only(cls):
        cls.index.models += [
            Model(
                hid=8,
                hyperid=1257 + i,
                vbid=10,
                vendor_id=9,
                datasource_id=1,
            )
            for i in range(10)
        ]
        cls.index.regional_models += [RegionalModel(hyperid=1257 + i, has_cpa=True, rids=[213]) for i in range(10)]
        cls.index.models += [
            Model(
                hid=8,
                hyperid=1267 + i,
                vbid=12,
                vendor_id=9,
                datasource_id=1,
            )
            for i in range(10)
        ]

    def test_with_CPA_only(self):
        """
        Тестируем, что возвращаем только модели с CPA офферами, что не поломали ничего другого, что нормально работает фильтрация по регионам
        """
        response = self.report.request_json('place=brand_products&vendor_id=9&rids=213&has-cpa-offers=1')
        self.assertFragmentIn(
            response,
            {
                "totalModels": 10,
                "results": ElementCount(10),
            },
        )

        response = self.report.request_json('place=brand_products&vendor_id=9&rids=213')
        self.assertFragmentIn(
            response,
            {
                "totalModels": 20,
                "results": ElementCount(20),
            },
        )

        response = self.report.request_json('place=brand_products&vendor_id=9&rids=229&has-cpa-offers=1')
        self.assertFragmentIn(
            response,
            {
                "totalModels": 0,
            },
        )

    @classmethod
    def prepare_with_CPA_offers_first(cls):
        cls.index.models += [
            Model(
                hid=9,
                hyperid=1277 + i,
                vbid=10,
                vendor_id=11,
                datasource_id=1,
                created_ts=get_timestamp(2000, 1, 1 + i),
            )
            for i in range(10)
        ]
        cls.index.regional_models += [RegionalModel(hyperid=1277 + i, has_cpa=i % 2, rids=[213]) for i in range(10)]

    def test_with_CPA_offers_first(self):
        """
        Проверяем, что сначала возвращаются модели с CPA офферами, а только потом без. Сейчас проверяем толкьо для моделей
        """

        response = self.report.request_json(
            'place=brand_products&vendor_id=11&rids=213&how=ddate&with-cpa-offers-first=1'
        )
        self.assertFragmentIn(
            response,
            {
                "totalModels": 10,
                "results": [
                    {
                        "entity": "product",
                        "id": 1286,
                    },
                    {
                        "entity": "product",
                        "id": 1284,
                    },
                    {
                        "entity": "product",
                        "id": 1282,
                    },
                    {
                        "entity": "product",
                        "id": 1280,
                    },
                    {
                        "entity": "product",
                        "id": 1278,
                    },
                    {
                        "entity": "product",
                        "id": 1285,
                    },
                    {
                        "entity": "product",
                        "id": 1283,
                    },
                    {
                        "entity": "product",
                        "id": 1281,
                    },
                    {
                        "entity": "product",
                        "id": 1279,
                    },
                    {
                        "entity": "product",
                        "id": 1277,
                    },
                ],
            },
            preserve_order=True,
            allow_different_len=True,
        )

        """
        На всякий случай, обратный тест
        """

        response = self.report.request_json('place=brand_products&vendor_id=11&rids=213&how=ddate')
        self.assertFragmentIn(
            response,
            {
                "totalModels": 10,
                "results": [
                    {
                        "entity": "product",
                        "id": 1286,
                    },
                    {
                        "entity": "product",
                        "id": 1285,
                    },
                    {
                        "entity": "product",
                        "id": 1284,
                    },
                    {
                        "entity": "product",
                        "id": 1283,
                    },
                    {
                        "entity": "product",
                        "id": 1282,
                    },
                    {
                        "entity": "product",
                        "id": 1281,
                    },
                    {
                        "entity": "product",
                        "id": 1280,
                    },
                    {
                        "entity": "product",
                        "id": 1279,
                    },
                    {
                        "entity": "product",
                        "id": 1278,
                    },
                    {
                        "entity": "product",
                        "id": 1277,
                    },
                ],
            },
            preserve_order=True,
            allow_different_len=True,
        )

    @classmethod
    def prepare_CPA_offers_first(cls):
        cls.index.models += [Model(hid=9, hyperid=1287 + i, vbid=10, vendor_id=12, datasource_id=1) for i in range(10)]

        cls.index.shops += [
            Shop(fesh=8, priority_region=213, regions=[225], name='happy CPA shop', cpa=Shop.CPA_REAL),
            Shop(fesh=9, priority_region=213, regions=[225], name='unhappy cpc shop'),
        ]

        cls.index.offers += [
            Offer(
                title="offer {}".format(i + 1),
                fesh=8 + (i % 2),
                hyperid=1287 + i,
                price=100 * (i + 1),
                vendor_id=12,
                cpa=Offer.CPA_REAL if (i + 1) % 2 else Offer.CPA_NO,
                waremd5="PAmdDwpT9iY4OTRGgVRW_{}".format(i),
            )
            for i in range(10)
        ]

    def test_CPA_offers_first(self):
        """
        Проверяем приоритизацию CPA офферов
        """

        response = self.report.request_json(
            'place=brand_products&vendor_id=12&how=aprice&entities=offer&with-cpa-offers-first=1'
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"entity": "offer", "titles": {"raw": "offer 1"}},
                    {"entity": "offer", "titles": {"raw": "offer 3"}},
                    {"entity": "offer", "titles": {"raw": "offer 5"}},
                    {"entity": "offer", "titles": {"raw": "offer 7"}},
                    {"entity": "offer", "titles": {"raw": "offer 9"}},
                    {"entity": "offer", "titles": {"raw": "offer 2"}},
                    {"entity": "offer", "titles": {"raw": "offer 4"}},
                    {"entity": "offer", "titles": {"raw": "offer 6"}},
                    {"entity": "offer", "titles": {"raw": "offer 8"}},
                    {"entity": "offer", "titles": {"raw": "offer 10"}},
                ],
            },
            preserve_order=True,
            allow_different_len=True,
        )

        """
        и без флага
        """

        response = self.report.request_json(
            'place=brand_products&vendor_id=12&how=aprice&entities=offer&with-cpa-offers-first=0'
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"entity": "offer", "titles": {"raw": "offer 1"}},
                    {"entity": "offer", "titles": {"raw": "offer 2"}},
                    {"entity": "offer", "titles": {"raw": "offer 3"}},
                    {"entity": "offer", "titles": {"raw": "offer 4"}},
                    {"entity": "offer", "titles": {"raw": "offer 5"}},
                    {"entity": "offer", "titles": {"raw": "offer 6"}},
                    {"entity": "offer", "titles": {"raw": "offer 7"}},
                    {"entity": "offer", "titles": {"raw": "offer 8"}},
                    {"entity": "offer", "titles": {"raw": "offer 9"}},
                    {"entity": "offer", "titles": {"raw": "offer 10"}},
                ],
            },
            preserve_order=True,
            allow_different_len=True,
        )

    @classmethod
    def prepare_with_CPA_offers_first_rearrange_on_meta(cls):
        cls.index.models += [
            Model(
                hid=9,
                hyperid=1400 + i,
                vbid=10,
                vendor_id=14,
                created_ts=get_timestamp(2000, 1, 1 + i),
            )
            for i in range(10)
        ]
        cls.index.regional_models += [RegionalModel(hyperid=1400 + i, has_cpa=i % 2, rids=[213]) for i in range(10)]

        cls.index.offers += [
            Offer(
                title="offer {}".format(i + 1),
                fesh=8 + (i % 2),
                hyperid=1400 + i,
                price=100 * (i + 1),
                vendor_id=14,
                cpa=Offer.CPA_REAL if i % 2 else Offer.CPA_NO,
                waremd5="PBmdDwpT9iY4OTRGgVRW_{}".format(i),
            )
            for i in range(4, 10)
        ]

    def test_with_CPA_offers_first_rearrange_on_meta(self):
        """
        Проверяем, что преоритезация моделей с CPA офферами работает, даже если в модельной статистике ошибка: есть два кейса:
         * в статистике сказано, что у модели есть CPA оффера, а на самом деле их нет,
         * стаитстика утверждает, что у модели нет офферов, а они есть.
        Проверяем только первый, второй мы не можем побороть, так как на базовых нет возможности проверить качество статистики.
        """
        response = self.report.request_json(
            'place=brand_products&entities=product&vendor_id=14&rids=213&how=ddate&with-cpa-offers-first=1'
        )
        self.assertFragmentIn(
            response,
            {
                "totalModels": 10,
                "results": [
                    {
                        "entity": "product",
                        "id": 1409,
                    },
                    {
                        "entity": "product",
                        "id": 1407,
                    },
                    {
                        "entity": "product",
                        "id": 1405,
                    },
                    {
                        "entity": "product",
                        "id": 1403,
                    },
                    {
                        "entity": "product",
                        "id": 1401,
                    },
                    {
                        "entity": "product",
                        "id": 1408,
                    },
                    {
                        "entity": "product",
                        "id": 1406,
                    },
                    {
                        "entity": "product",
                        "id": 1404,
                    },
                    {
                        "entity": "product",
                        "id": 1402,
                    },
                    {
                        "entity": "product",
                        "id": 1400,
                    },
                ],
            },
            preserve_order=True,
            allow_different_len=False,
        )

        """
        Проверяем что, если не попросить только модели, то ручка вернет, в том чилсе и оффера
        """
        response = self.report.request_json(
            'place=brand_products&vendor_id=14&rids=213&how=ddate&with-cpa-offers-first=1'
        )
        self.assertFragmentIn(
            response,
            {
                "totalModels": 10,
                "totalOffers": 3,
                "results": ElementCount(13),
            },
        )

    @classmethod
    def prepare_more_and_more_pages(cls):
        cls.index.models += [
            Model(
                hid=10,
                hyperid=1300 + i,
                vbid=10,
                vendor_id=13,
                datasource_id=1,
                opinion=Opinion(rating=5.0 - i * 0.1),
            )
            for i in range(42)
        ]

    def test_more_and_more_pages(self):
        """
        Сейчас для всех плейсов максимальное количество страниц - 15, для нашей ручки надо отдавать любое коилчество моделей, проверяем, что это так
        Проверяем, что больше можно получить больше 15 страниц
        """
        response15 = self.report.request_json(
            'place=brand_products&vendor_id=13&how=quality&entities=product&with-cpa-offers-first=0&numdoc=2&page=15'
        )

        self.assertFragmentIn(
            response15,
            {
                "results": [
                    {
                        "entity": "product",
                        "id": 1328,
                    },
                    {
                        "entity": "product",
                        "id": 1329,
                    },
                ],
            },
            preserve_order=True,
            allow_different_len=True,
        )

        response16 = self.report.request_json(
            'place=brand_products&vendor_id=13&how=quality&entities=product&with-cpa-offers-first=0&numdoc=2&page=16'
        )

        self.assertFragmentIn(
            response16,
            {
                "results": [
                    {
                        "entity": "product",
                        "id": 1330,
                    },
                    {
                        "entity": "product",
                        "id": 1331,
                    },
                ],
            },
            preserve_order=True,
            allow_different_len=True,
        )


if __name__ == '__main__':
    main()

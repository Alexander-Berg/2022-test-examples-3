#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    BlueOffer,
    Const,
    Currency,
    DeliveryBucket,
    DeliveryOption,
    GLParam,
    GLType,
    HyperCategory,
    MarketSku,
    Model,
    ModelDescriptionTemplates,
    NavCategory,
    Offer,
    Opinion,
    Picture,
    RegionalDelivery,
    Shop,
    Tax,
    VCluster,
    Vendor,
    VirtualModel,
    DynamicMarketSku,
    DynamicWarehouseInfo,
    DynamicWarehouseToWarehouseInfo,
)
from core.testcase import TestCase, main
from core.matcher import NotEmpty, NoKey, NotEmptyList
from core.types.picture import thumbnails_config


class T(TestCase):
    @classmethod
    def prepare(cls):

        cls.index.vendors += [
            Vendor(vendor_id=100500, website="https://www.cia.gov/index.html"),
        ]

        cls.index.models += [
            Model(
                hyperid=101,
                hid=11,
                title="Cia Lopata",
                vendor_id=100500,
                new=True,
                opinion=Opinion(total_count=10, rating=4.5, precise_rating=4.71, rating_count=15, reviews=5),
                model_name="Lopata",
            ),
            Model(hyperid=102, hid=12, full_description="very good description"),
        ]

        cls.index.shops += [
            Shop(fesh=1001, priority_region=213, regions=[225]),
            Shop(fesh=1002, priority_region=213, regions=[225]),
        ]

        cls.index.offers += [
            Offer(hyperid=101, fesh=1001, price=300),
            Offer(hyperid=102, fesh=1002),
        ]

        # specify main, max, avg prices
        cls.index.offers += [
            Offer(hyperid=101, fesh=1001, price=100),
            Offer(hyperid=101, fesh=1001, price=500),
        ]

        # MARKETOUT-8559 vendor name in output
        cls.index.gltypes += [
            GLType(
                param_id=201,
                hid=11,
                gltype=GLType.ENUM,
                values=[100501, 100502],
                unit_name="Производитель",
                cluster_filter=True,
                vendor=True,
            ),
            GLType(
                param_id=201,
                hid=12,
                gltype=GLType.ENUM,
                values=[100501, 100502],
                unit_name="Производитель",
                cluster_filter=True,
                vendor=True,
            ),
            GLType(
                param_id=201,
                hid=13,
                gltype=GLType.ENUM,
                values=[100501, 100502],
                unit_name="Производитель",
                cluster_filter=True,
                vendor=True,
            ),
            GLType(param_id=Const.PSKU2_GL_PARAM_ID, hid=14, gltype=GLType.BOOL),
            GLType(param_id=Const.PSKU2_LITE_GL_PARAM_ID, hid=15, gltype=GLType.BOOL),
        ]

        cls.index.vendors += [Vendor(vendor_id=100501, name="Roskosmos"), Vendor(vendor_id=100502, name="CIA")]

        cls.index.models += [
            Model(
                hyperid=103,
                title="Grabli",
                vendor_id=100501,
                hid=14,
                glparams=[
                    GLParam(param_id=201, vendor=True, value=100501),
                ],
            ),
            Model(
                hyperid=104,
                title="Vily",
                vendor_id=100501,
                hid=15,
                glparams=[
                    GLParam(param_id=201, vendor=True, value=100501),
                ],
            ),
        ]

        cls.index.offers += [
            Offer(hyperid=103, fesh=1001, price=200, glparams=[GLParam(param_id=201, vendor=True, value=100501)]),
            Offer(hyperid=104, fesh=1001, price=200, glparams=[GLParam(param_id=201, vendor=True, value=100501)]),
        ]

        cls.index.vclusters += [
            VCluster(
                hid=13, vclusterid=1100000101, title="instruction", description="spy instruction", vendor_id=100502
            )
        ]

        cls.index.shops += [Shop(fesh=213, priority_region=213, regions=[213])]

        cls.index.offers += [
            Offer(
                vclusterid=1100000101,
                glparams=[
                    GLParam(param_id=201, value=100502, vendor=True),
                ],
                hid=13,
                price=100,
                fesh=212,
            ),
            Offer(
                vclusterid=1100000101,
                glparams=[
                    GLParam(param_id=201, value=100502, vendor=True),
                ],
                hid=13,
                price=110,
                fesh=213,
            ),
        ]

        cls.index.models += [
            Model(hyperid=105),
            Model(hyperid=106),
        ]

        # Для тестирование выдачи всех mbo параметров модели в секции gl_params
        cls.index.gltypes += [
            GLType(
                param_id=301,
                hid=11,
                gltype=GLType.ENUM,
                values=[100501, 100502],
                name='Enum parameter',
                xslname='enum param',
            ),
            GLType(
                param_id=302,
                hid=11,
                gltype=GLType.NUMERIC,
                unit_name="unit",
                name='Numeric parameter',
                xslname='numeric param',
            ),
            GLType(param_id=303, hid=11, gltype=GLType.BOOL, name='Bool parameter', xslname='bool param'),
            GLType(param_id=304, hid=11, gltype=GLType.STRING, name='String parameter', xslname='string param'),
        ]

        cls.index.models += [
            Model(
                hyperid=108,
                hid=11,
                title="Grabli2",
                vendor_id=100501,
                glparams=[
                    GLParam(param_id=301, value=100501),
                    GLParam(param_id=301, value=100502),
                    GLParam(param_id=302, value=34.5),
                    GLParam(param_id=303, value=0),
                    GLParam(param_id=304, string_value='Some string'),
                ],
            ),
            Model(
                hyperid=109,
                hid=11,
                title="Grabli3",
                vendor_id=100501,
                glparams=[
                    GLParam(param_id=301, value=100502, is_filter=False),
                    GLParam(param_id=302, value=22.6, is_filter=False),
                    GLParam(param_id=303, value=1, is_filter=False),
                    GLParam(param_id=304, string_value='String1', additional_string_values=['String2', 'String3']),
                ],
            ),
        ]

    def test_no_offers_found(self):
        response = self.report.request_json('place=modelinfo&hyperid=101&hyperid=102&rids=213')
        self.assertFragmentIn(
            response, [{"type": "model", "id": 101}, {"type": "model", "id": 102}], allow_different_len=False
        )

    def test_format_data(self):
        response = self.report.request_json('place=modelinfo&hyperid=101&rids=213')
        self.assertFragmentIn(response, {"prices": {"min": "100", "max": "500", "avg": "300"}})
        self.assertFragmentIn(
            response, {"vendor": {"entity": "vendor", "id": 100500, "website": "https://www.cia.gov/index.html"}}
        )
        self.assertFragmentIn(response, {"titles": {"raw": "Cia Lopata"}})
        self.assertFragmentIn(response, {"modelName": {"raw": "Lopata"}})
        self.assertFragmentIn(
            response, {"categories": [{"entity": "category", "id": 11, "name": "HID-11", "slug": "hid-11"}]}
        )
        self.assertFragmentIn(response, {"pictures": [{"entity": "picture", "thumbnails": []}]})
        self.assertFragmentIn(response, {"offers": {"count": 3}})
        self.assertFragmentIn(response, {"type": "model"})
        self.assertFragmentIn(response, {"id": 101})
        self.assertFragmentIn(response, {"isNew": True})
        self.assertFragmentIn(
            response, {"opinions": 10, "rating": 4.5, "preciseRating": 4.71, "ratingCount": 15, "reviews": 5}
        )

    def test_vendor_name_and_filter_for_model(self):
        response = self.report.request_json('place=modelinfo&hyperid=103&rids=213')
        self.assertFragmentIn(response, {"entity": "vendor", "id": 100501, "name": "Roskosmos", "filter": "201:100501"})

    def test_vendor_name_and_filter_for_vcluster(self):
        response = self.report.request_json('place=modelinfo&hyperid=1100000101&rids=213')
        self.assertFragmentIn(
            response,
            {
                "entity": "product",
                "vendor": {"entity": "vendor", "id": 100502, "name": "CIA", "filter": "201:100502"},
            },
        )

    def test_description_in_vcluster(self):
        response = self.report.request_json('place=modelinfo&hyperid=1100000101&rids=213')
        self.assertFragmentIn(response, {"entity": "product", "description": "spy instruction"})

    def test_missing_pp(self):
        self.report.request_xml('place=modelinfo&hyperid=101&rids=213&ip=127.0.0.1', add_defaults=False)

    def test_search_stats(self):
        response = self.report.request_json('place=modelinfo&hyperid=105&rids=213')
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 1,
                    "adult": False,
                    "salesDetected": False,
                }
            },
        )

        response = self.report.request_json('place=modelinfo&hyperid=105&hyperid=106&rids=213')
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 2,
                    "adult": False,
                    "salesDetected": False,
                    "results": [
                        {"id": 105},
                        {"id": 106},
                    ],
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

        response = self.report.request_json('place=modelinfo&hyperid=107&rids=213')
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 0,
                    "adult": False,
                    "salesDetected": False,
                }
            },
        )

        """Проверяется, что общее количество для показа = total"""
        self.access_log.expect(total_renderable='1')
        self.access_log.expect(total_renderable='2')
        self.access_log.expect(total_renderable='0')

    def test_model_order(self):
        """
        Request all possible models and check that ID order in the results and the CGI params is the same.
        """
        response = self.report.request_json(
            'place=modelinfo&hyperid=101&hyperid=102&hyperid=103&hyperid=104&hyperid=105&hyperid=106&rids=213'
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"id": 101},
                    {"id": 102},
                    {"id": 103},
                    {"id": 104},
                    {"id": 105},
                    {"id": 106},
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def test_all_model_params_mmap(self):
        """Тестируем: выдачу секции gl_params, содержащую mbo параметры модели как из gl_filters.mmap,
        так и из модельного индекса (секция "MboModel").
        В данном тесте все параметры из gl_models.mmap, кроме строкового
        """
        response = self.report.request_json('place=modelinfo&hyperid=108&rids=0&bsformat=2&all-gl-mbo-params=true')
        self.assertFragmentIn(
            response,
            {
                "gl_params": [
                    {
                        "id": "301",
                        "type": "enum",
                        "name": "Enum parameter",
                        "xslname": "enum param",
                        "values": [
                            {"value": "VALUE-100501", "id": "100501"},
                            {"value": "VALUE-100502", "id": "100502"},
                        ],
                    },
                    {
                        "id": "302",
                        "type": "number",
                        "name": "Numeric parameter",
                        "xslname": "numeric param",
                        "unit": "unit",
                        "values": [
                            {"value": "34.5"},
                        ],
                    },
                    {
                        "id": "303",
                        "type": "boolean",
                        "name": "Bool parameter",
                        "xslname": "bool param",
                        "values": [
                            {"value": "0"},
                        ],
                    },
                    {
                        "id": "304",
                        "type": "string",
                        "name": "String parameter",
                        "xslname": "string param",
                        "values": [
                            {"value": "Some string"},
                        ],
                    },
                ]
            },
        )

    def test_all_model_params_index(self):
        """Тестируем: выдачу секции gl_params, содержащую mbo параметры модели как из gl_filters.mmap,
        так и из модельного индекса (секция "MboModel").
        В данном тесте все параметры из модельного индекса
        """
        response = self.report.request_json('place=modelinfo&hyperid=109&rids=0&bsformat=2&all-gl-mbo-params=true')
        self.assertFragmentIn(
            response,
            {
                "gl_params": [
                    {
                        "id": "301",
                        "type": "enum",
                        "name": "Enum parameter",
                        "xslname": "enum param",
                        "values": [
                            {"value": "VALUE-100502", "id": "100502"},
                        ],
                    },
                    {
                        "id": "302",
                        "type": "number",
                        "name": "Numeric parameter",
                        "xslname": "numeric param",
                        "unit": "unit",
                        "values": [
                            {"value": "22.6"},
                        ],
                    },
                    {
                        "id": "303",
                        "type": "boolean",
                        "name": "Bool parameter",
                        "xslname": "bool param",
                        "values": [
                            {"value": "1"},
                        ],
                    },
                    {
                        "id": "304",
                        "type": "string",
                        "name": "String parameter",
                        "xslname": "string param",
                        "values": [
                            {"value": "String1"},
                            {"value": "String2"},
                            {"value": "String3"},
                        ],
                    },
                ]
            },
        )

    def test_all_model_params_disabled(self):
        """Тестируем: не выдаем секцию gl_params без спец. флажка"""
        response = self.report.request_json('place=modelinfo&hyperid=109&rids=0&bsformat=2')
        self.assertFragmentNotIn(response, {"gl_params": NotEmpty()})

    @classmethod
    def prepare_modelinfo_runtime_stats_filters(cls):
        """Создаем модель и офферы от трех магазинов с разными ценами
        Два магазина CPC=NO
        """
        cls.index.models += [
            Model(hyperid=1530411),
        ]

        cls.index.shops += [
            Shop(fesh=1530401, priority_region=213, cpc=Shop.CPC_NO),
            Shop(fesh=1530402, priority_region=213),
            Shop(fesh=1530403, priority_region=213, cpc=Shop.CPC_NO),
        ]

        cls.index.offers += [
            Offer(hyperid=1530411, fesh=1530401, price=100),
            Offer(hyperid=1530411, fesh=1530402, price=200),
            Offer(hyperid=1530411, fesh=1530403, price=300),
        ]

    def test_modelinfo_runtime_stats_filters(self):
        """Что тестируем: при запросе рантацмовых статистик в modelinfo
        применяются все фильтры, использующиеся в релевантности основного,
        в т.ч. фильтр по наличию CPC
        Офферы магазинов с CPC=NO отфильтровываются при запросе рантаймовых
        статистиках и не учитываются при выдаче min/max/avg prices
        """
        response = self.report.request_json('place=modelinfo&hyperid=1530411&rids=213&bsformat=2')
        self.assertFragmentIn(response, {"prices": {"min": "200", "max": "200", "currency": "RUR", "avg": "200"}})

    @classmethod
    def prepare_model_with_mskus(cls):

        cls.index.shops += [
            Shop(
                # Виртуальный магазин синего маркета
                fesh=104,
                datafeed_id=1,
                priority_region=213,
                regions=[213],
                name='virtual_shop',
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                fulfillment_virtual=True,
                virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
                cpa=Shop.CPA_REAL,
            ),
        ]

        cls.index.models += [
            Model(hyperid=1, hid=2, title="Мскушечная моделька"),
            Model(hyperid=5, hid=2, title="Еще одна Мскушечная моделька"),
        ]
        cls.index.mskus += [
            MarketSku(title="Мскушка1", blue_offers=[BlueOffer(), BlueOffer()], sku=100210864757, hyperid=1),
            MarketSku(title="Мскушка2", blue_offers=[BlueOffer()], sku=100210864759, hyperid=1),
            MarketSku(title="Мскушка5", blue_offers=[BlueOffer()], sku=100210864584, hyperid=5),
        ]

        cls.index.offers += [Offer(hyperid=1, fesh=213)]

    def test_model_info_sku_stats(self):
        """Првоверяем что на place=modelinfo отображаются skuStats
        Примечание: сейчас modelstats учитывает все синие офферы а не байбоксы
        кажется это немного не то что мы хотим, но пока так
        """

        response = self.report.request_json('place=modelinfo&hyperid=1&rids=213&rearr-factors=market_nordstream=0')
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {
                            "entity": "product",
                            "id": 1,
                            "offers": {
                                "count": 4,  # 3 синих оффера (не совсем то что нужно) и один белый
                                "items": NoKey("items"),
                            },
                            "skuStats": {"totalCount": 2},
                        }
                    ]
                }
            },
        )

        response = self.report.request_json(
            'place=modelinfo&hyperid=1&rids=213&use-default-offers=1&rearr-factors=market_nordstream=0'
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {
                            "entity": "product",
                            "id": 1,
                            "offers": {"count": 3, "items": NotEmptyList()},  # 2 синих байбокса и один белый
                            "skuStats": {"totalCount": 2},
                        }
                    ]
                }
            },
        )

        response = self.report.request_json(
            'place=modelinfo&hyperid=1&rgb=blue&rids=213&rearr-factors=market_nordstream=0'
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {
                            "entity": "product",
                            "id": 1,
                            "offers": {"count": 3, "items": NoKey("items")},  # 3 синих оффера (не совсем то что нужно)
                            "skuStats": {"totalCount": 2},
                        }
                    ]
                }
            },
        )

        response = self.report.request_json(
            'place=modelinfo&hyperid=1&rgb=blue&rids=213&use-default-offers=1&rearr-factors=market_nordstream=0'
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {
                            "entity": "product",
                            "id": 1,
                            "offers": {"count": 2, "items": NotEmptyList()},  # 2 синих байбокса
                            "skuStats": {"totalCount": 2},
                        }
                    ]
                }
            },
        )

    virtual_model_id_range_start = int(2 * 1e12)
    virtual_model_id_range_finish = int(virtual_model_id_range_start + 1e15)
    virtual_model_id = (virtual_model_id_range_start + virtual_model_id_range_finish) // 2
    second_virtual_model_id = virtual_model_id + 1

    @classmethod
    def prepare_virtual_model_info(cls):
        cls.index.virtual_models += [
            VirtualModel(
                virtual_model_id=T.virtual_model_id,
                opinion=Opinion(total_count=44, rating=4.3, precise_rating=4.31, rating_count=43, reviews=3),
            )
        ]

        cls.index.vendors += [
            Vendor(vendor_id=42, name="Virtucon", website="https://www.virtucon.com"),
        ]

        cls.index.hypertree += [
            HyperCategory(hid=4242, name='Наковальни'),
            HyperCategory(hid=4243, name='Ультранаковальни'),
        ]

        cls.index.navtree += [NavCategory(nid=424242, hid=4242, name='Все для кузен')]

        cls.index.model_description_templates += [
            ModelDescriptionTemplates(
                hid=4242,
                micromodel="{Quality#ifnz}Качественная{#endif} наковальня",
                friendlymodel=["{Quality#ifnz}Качественная{#endif}" "Наковальня"],
                model=[
                    (
                        "Технические характеристики",
                        {
                            "Качество": "{Quality}",
                        },
                    ),
                ],
                seo="{return $Quality; #exec}",
            )
        ]

        cls.index.gltypes += [
            GLType(param_id=42, hid=4242, gltype=GLType.NUMERIC, xslname="Quality"),
        ]

        cls.index.offers += [
            Offer(
                waremd5='OfferNoModel_________g',
                title="Наковальня #10",
                fesh=213,
                vendor_id=42,
                hid=4242,
                picture=Picture(
                    picture_id='iyC4nHslqLtqZJLygVAHeA',
                    width=200,
                    height=200,
                    thumb_mask=thumbnails_config.get_mask_by_names(['200x200']),
                    group_id=1234,
                ),
                glparams=[GLParam(param_id=42, value=1)],
                virtual_model_id=T.virtual_model_id,
            ),
            Offer(
                waremd5='OfferNoModel2________g',
                title="Наковальня #11",
                fesh=213,
                vendor_id=42,
                hid=4243,
                virtual_model_id=T.second_virtual_model_id,
            ),
        ]

    def test_virtual_model_info(self):
        # Также проверяем работу с БК
        for use_bk in ['', 'use_fast_cards=1']:
            flags = 'rearr-factors=market_cards_everywhere_model_info=1;market_cards_everywhere_range={}:{};{}'.format(
                T.virtual_model_id_range_start, T.virtual_model_id_range_finish, use_bk
            )
            response = self.report.request_json(
                'place=modelinfo&hyperid={}&rids=213&show-models-specs=full&{}'.format(T.virtual_model_id, flags)
            )
            self.assertFragmentIn(
                response,
                {
                    'search': {
                        "total": 1,
                        "totalModels": 0,
                        "totalOffers": 0,
                        "totalOffersBeforeFilters": 0,
                        "totalPassedAllGlFilters": 0,
                        'results': [
                            {
                                "showUid": "",
                                "entity": "product",
                                "id": T.virtual_model_id,
                                "vendor": {
                                    "entity": "vendor",
                                    "id": 42,
                                    "name": "Virtucon",
                                    "slug": "virtucon",
                                    "website": "https://www.virtucon.com",
                                },
                                "titles": {
                                    "raw": "Наковальня #10",
                                    "highlighted": [{"value": "Наковальня #10"}],
                                },
                                "urls": {},
                                "slug": "nakovalnia-10",
                                "description": "Качественная наковальня",
                                "lingua": {
                                    "type": {
                                        "accusative": "1.00-accusative",
                                        "dative": "1.00-dative",
                                        "genitive": "1.00-genitive",
                                        "nominative": "1.00-nominative",
                                    }
                                },
                                "specs": {
                                    "full": [
                                        {
                                            "groupName": "Технические характеристики",
                                            "groupSpecs": [
                                                {
                                                    "name": "Качество",
                                                    "usedParams": [{"id": 42, "name": "GLPARAM-42"}],
                                                    "value": "1",
                                                },
                                            ],
                                        }
                                    ]
                                },
                                "categories": [
                                    {
                                        "cpaType": "cpc_and_cpa",
                                        "entity": "category",
                                        "fullName": "UNIQ-HID-4242",
                                        "id": 4242,
                                        "isLeaf": True,
                                        "kinds": [],
                                        "name": "Наковальни",
                                        "nid": 424242,
                                        "slug": "nakovalni",
                                        "type": "simple",
                                    }
                                ],
                                "navnodes": [
                                    {
                                        "entity": "navnode",
                                        "fullName": "UNIQ-NID-424242",
                                        "id": 424242,
                                        "isLeaf": True,
                                        "name": "Все для кузен",
                                        "rootNavnode": {},
                                        "slug": "vse-dlia-kuzen",
                                    }
                                ],
                                "pictures": [
                                    {
                                        "entity": "picture",
                                        "thumbnails": [
                                            {
                                                "containerWidth": 200,
                                                "containerHeight": 200,
                                                "url": "http://avatars.mdst.yandex.net/get-marketpic/1234/market_iyC4nHslqLtqZJLygVAHeA/200x200",
                                                "width": 200,
                                                "height": 200,
                                            }
                                        ],
                                    }
                                ],
                                "filters": [
                                    {
                                        "id": "42",
                                        "isGuruLight": True,
                                        "kind": 1,
                                        "meta": {},
                                        "name": "GLPARAM-42",
                                        "noffers": 1,
                                        "position": 1,
                                        "precision": 0,
                                        "subType": "",
                                        "type": "number",
                                        "values": [
                                            {
                                                "id": "found",
                                                "initialMax": "1",
                                                "initialMin": "1",
                                                "max": "1",
                                                "min": "1",
                                            }
                                        ],
                                        "xslname": "Quality",
                                    }
                                ],
                                "modelCreator": "partner",
                                "offers": {"count": 1, "cutPriceCount": 0},
                                "prices": {
                                    "avg": "100",
                                    "currency": "RUR",
                                    "max": "100",
                                    "min": "100",
                                },
                                "opinions": 44,
                                "rating": 4.3,
                                "preciseRating": 4.31,
                                "ratingCount": 43,
                                "reviews": 3,
                            }
                        ],
                    }
                },
            )

            # test that with-rebuilt-model=1 we didn't stop searching virtual models
            response = self.report.request_json(
                'place=modelinfo&hyperid={}&rids=213&with-rebuilt-model=1&{}'.format(T.virtual_model_id, flags)
            )

            self.assertFragmentIn(
                response,
                {
                    'search': {
                        "total": 1,
                        'results': [
                            {
                                "showUid": "",
                                "entity": "product",
                                "id": T.virtual_model_id,
                            }
                        ],
                    }
                },
            )

            # запрос без virtual_id
            self.error_log.expect(code=3043)
            flags = 'rearr-factors=market_cards_everywhere_model_info=1;market_cards_everywhere_range={}:{}'.format(
                T.virtual_model_id_range_start, T.virtual_model_id_range_finish
            )
            response = self.report.request_json(
                'place=modelinfo&hyperid={}&rids=213&show-models-specs=full&{}'.format("", flags)
            )
            self.assertFragmentIn(
                response,
                {
                    "error": {
                        "code": "INVALID_USER_CGI",
                        "message": "Model ID or mSKU should be specified",
                    }
                },
            )

            # запрос c несуществующим virtual_id
            flags = 'rearr-factors=market_cards_everywhere_model_info=1;market_cards_everywhere_range={}:{}'.format(
                T.virtual_model_id_range_start, T.virtual_model_id_range_finish
            )
            response = self.report.request_json(
                'place=modelinfo&hyperid={}&rids=213&show-models-specs=full&{}'.format(T.virtual_model_id + 404, flags)
            )
            self.assertFragmentIn(
                response,
                {
                    'totalOffers': 0,
                    'results': [],
                },
            )

    def test_several_virtual_models_ids(self):
        '''Test that when we request serveral model ids and one of them is virtual output is correct with & without flag'''

        for use_bk in ['', 'use_fast_cards=1']:
            flags = 'rearr-factors=market_cards_everywhere_model_info=1;market_cards_everywhere_range={}:{};{}'.format(
                T.virtual_model_id_range_start, T.virtual_model_id_range_finish, use_bk
            )

            # virtual model with normal
            response = self.report.request_json(
                'place=modelinfo&hyperid={}&hyperid={}&rids=213&{}'.format(T.virtual_model_id, 101, flags)
            )
            self.assertFragmentIn(
                response,
                {
                    'search': {
                        "total": 2,
                        'results': [
                            {
                                "entity": "product",
                                "id": T.virtual_model_id,
                                "isVirtual": True,
                            },
                            {
                                "entity": "product",
                                "id": 101,
                                "isVirtual": False,
                            },
                        ],
                    }
                },
            )

            # two virtual models with normal
            response = self.report.request_json(
                'place=modelinfo&hyperid={}&hyperid={}&hyperid={}&rids=213&{}'.format(
                    T.virtual_model_id, T.second_virtual_model_id, 101, flags
                )
            )
            self.assertFragmentIn(
                response,
                {
                    'search': {
                        "total": 3,
                        'results': [
                            {
                                "entity": "product",
                                "id": T.virtual_model_id,
                                "isVirtual": True,
                            },
                            {
                                "entity": "product",
                                "id": T.second_virtual_model_id,
                                "isVirtual": True,
                            },
                            {
                                "entity": "product",
                                "id": 101,
                                "isVirtual": False,
                            },
                        ],
                    }
                },
            )

            # two virtual models with with two normal
            # + проверяем, что отработает под флагом БК
            response = self.report.request_json(
                'place=modelinfo&hyperid={}&hyperid={}&hyperid={}&hyperid={}&rids=213&{}'.format(
                    T.virtual_model_id, T.second_virtual_model_id, 101, 102, flags
                )
            )
            self.assertFragmentIn(
                response,
                {
                    'search': {
                        "total": 4,
                        'results': [
                            {
                                "entity": "product",
                                "id": T.virtual_model_id,
                                "isVirtual": True,
                            },
                            {
                                "entity": "product",
                                "id": T.second_virtual_model_id,
                                "isVirtual": True,
                            },
                            {
                                "entity": "product",
                                "id": 101,
                                "isVirtual": False,
                            },
                            {
                                "entity": "product",
                                "id": 102,
                                "isVirtual": False,
                            },
                        ],
                    }
                },
            )

            # only normal model
            response = self.report.request_json('place=modelinfo&hyperid={}&rids=213&{}'.format(101, flags))
            self.assertFragmentIn(
                response,
                {
                    'search': {
                        "total": 1,
                        'results': [
                            {
                                "entity": "product",
                                "id": 101,
                                "isVirtual": False,
                            },
                        ],
                    }
                },
            )

            response = self.report.request_json(
                'place=modelinfo&hyperid={}&hyperid={}&rids=213&{}'.format(101, 102, flags)
            )
            self.assertFragmentIn(
                response,
                {
                    'search': {
                        "total": 2,
                        'results': [
                            {
                                "entity": "product",
                                "id": 101,
                                "isVirtual": False,
                            },
                            {
                                "entity": "product",
                                "id": 102,
                                "isVirtual": False,
                            },
                        ],
                    }
                },
            )

            # Теперь все флаги включены по-умолчанию, поэтому проверям, выключив market_cards_everywhere_model_info
            flags = 'rearr-factors=market_cards_everywhere_model_info=0;market_cards_everywhere_range={}:{}'.format(
                T.virtual_model_id_range_start, T.virtual_model_id_range_finish
            )
            response = self.report.request_json(
                'place=modelinfo&hyperid={}&hyperid={}&hyperid={}&rids=213&{}'.format(
                    T.virtual_model_id, T.second_virtual_model_id, 101, flags
                )
            )
            self.assertFragmentIn(
                response,
                {
                    'search': {
                        "total": 1,
                        'results': [
                            {
                                "entity": "product",
                                "id": 101,
                            },
                        ],
                    }
                },
            )

    def test_virtual_model_info_range(self):

        # virtual_id в диапазоне - [start, finish]
        flags = 'rearr-factors=market_cards_everywhere_model_info=1;market_cards_everywhere_range={}:{}'.format(
            T.virtual_model_id_range_start + 404, T.virtual_model_id_range_finish - 404
        )
        response = self.report.request_json(
            'place=modelinfo&hyperid={}&rids=213&show-models-specs=full&{}'.format(T.virtual_model_id, flags)
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        "entity": "product",
                        "id": T.virtual_model_id,
                    },
                ],
            },
        )

        # virtual_id вне диапазона - [vimd + 404, finish]
        flags = 'rearr-factors=market_cards_everywhere_model_info=1;market_cards_everywhere_range={}:{}'.format(
            T.virtual_model_id + 404, T.virtual_model_id_range_finish - 404
        )
        response = self.report.request_json(
            'place=modelinfo&hyperid={}&rids=213&show-models-specs=full&{}'.format(T.virtual_model_id, flags)
        )
        self.assertFragmentIn(
            response,
            {
                'totalOffers': 0,
                'results': [],
            },
        )

        # проверка, что все хорошо при невалидных диапазонах
        invalid_ranges = [
            [T.virtual_model_id_range_finish, T.virtual_model_id_range_start],
            ['', T.virtual_model_id_range_finish],
            [T.virtual_model_id_range_start, ''],
            ['', ''],
        ]

        for range_ in invalid_ranges:
            flags = 'rearr-factors=market_cards_everywhere_model_info=1;market_cards_everywhere_range={}:{}'.format(
                range_[0], range_[1]
            )
            response = self.report.request_json(
                'place=modelinfo&hyperid={}&rids=213&show-models-specs=full&{}'.format(T.virtual_model_id, flags)
            )

            self.assertFragmentIn(
                response,
                {
                    'totalOffers': 0,
                    'results': [],
                },
            )

        # запрос без range
        flags = 'rearr-factors=market_cards_everywhere_model_info=1'
        response = self.report.request_json(
            'place=modelinfo&hyperid={}&rids=213&show-models-specs=full&{}'.format(T.virtual_model_id, flags)
        )
        self.assertFragmentIn(
            response,
            {
                'totalOffers': 0,
                'results': [],
            },
        )

    def test_hide_descriptions_flag(self):
        """Проверяем, что под флагом market_hide_descriptions не показываются описания
        https://st.yandex-team.ru/MARKETOUT-36826
        """
        response = self.report.request_json("place=modelinfo&hyperid=102&rids=213")
        self.assertFragmentIn(response, {"fullDescription": "very good description"})

        response = self.report.request_json(
            "place=modelinfo&hyperid=102&rids=213&rearr-factors=market_hide_descriptions=1"
        )
        self.assertFragmentNotIn(response, {"fullDescription": "very good description"})

    @classmethod
    def prepare_fast_cards_model_info(cls):
        cls.index.virtual_models += [
            VirtualModel(
                virtual_model_id=1580,
                opinion=Opinion(total_count=44, rating=4.3, precise_rating=4.31, rating_count=43, reviews=3),
            ),
            VirtualModel(
                virtual_model_id=1582,
                opinion=Opinion(total_count=47, rating=4.5, precise_rating=4.51, rating_count=45, reviews=5),
            ),
            VirtualModel(
                virtual_model_id=1583,
                opinion=Opinion(total_count=50, rating=4.5, precise_rating=4.51, rating_count=25, reviews=15),
            ),
            VirtualModel(
                virtual_model_id=1584,
                opinion=Opinion(total_count=50, rating=4.5, precise_rating=4.51, rating_count=25, reviews=15),
            ),
            VirtualModel(
                virtual_model_id=1585,
                opinion=Opinion(total_count=50, rating=4.5, precise_rating=4.51, rating_count=25, reviews=15),
            ),
        ]

        cls.index.vendors += [
            Vendor(vendor_id=142, name="Гвинт", website="https://www.gwent.com"),
        ]

        cls.index.hypertree += [
            HyperCategory(hid=14242),
            HyperCategory(hid=14243),
        ]

        cls.index.navtree += [NavCategory(nid=104242, hid=14242, name='Гвинтокарты')]
        cls.index.navtree += [NavCategory(nid=104243, hid=14243, name='Гвинтокарточки')]

        cls.index.model_description_templates += [
            ModelDescriptionTemplates(
                hid=14242,
                micromodel="{Quality#ifnz}Качественная{#endif} карта",
                friendlymodel=["{Quality#ifnz}Качественная{#endif}" "Карта"],
                model=[
                    (
                        "Технические характеристики",
                        {
                            "Качество": "{Quality}",
                        },
                    ),
                ],
                seo="{return $Quality; #exec}",
            )
        ]

        cls.index.gltypes += [
            GLType(param_id=420, hid=14242, gltype=GLType.NUMERIC, xslname="Quality"),
        ]

        cls.index.shops += [
            Shop(fesh=12710, datafeed_id=14240, priority_region=213, regions=[213], client_id=12, cpa=Shop.CPA_REAL),
            Shop(
                fesh=12711,
                datafeed_id=14241,
                priority_region=213,
                regions=[213],
                client_id=13,
                blue=Shop.BLUE_REAL,
                cpa=Shop.CPA_REAL,
            ),
        ]

        cls.dynamic.lms += [
            DynamicWarehouseInfo(id=145, home_region=213),
            DynamicWarehouseToWarehouseInfo(warehouse_from=145, warehouse_to=145),
        ]

        cls.index.offers += [
            Offer(
                sku=1580,
                virtual_model_id=1580,
                vmid_is_literal=False,
                waremd5='OfferFastModel0CPC___g',
                title="Геральт cpc",
                fesh=213,
                vendor_id=142,
                hid=14242,
                picture=Picture(
                    picture_id='iyC4nHslqLtqZJLygVAHeA',
                    width=200,
                    height=200,
                    thumb_mask=thumbnails_config.get_mask_by_names(['200x200']),
                    group_id=1234,
                ),
                glparams=[GLParam(param_id=420, value=1)],
                price=150,
            ),
            Offer(
                sku=1580,
                virtual_model_id=1580,
                vmid_is_literal=False,
                waremd5='OfferFastModel0CPA___g',
                title="Геральт cpa",
                fesh=12710,
                vendor_id=142,
                hid=14242,
                picture=Picture(
                    picture_id='iyC4nHslqLtqZJLygVAHeA',
                    width=300,
                    height=300,
                    thumb_mask=thumbnails_config.get_mask_by_names(['200x200']),
                    group_id=1234,
                ),
                glparams=[GLParam(param_id=420, value=2)],
                price=175,
                cpa=Offer.CPA_REAL,
                delivery_buckets=[14240],
            ),
            Offer(
                sku=1580,
                virtual_model_id=1580,
                vmid_is_literal=False,
                waremd5='OfferFastModel0BLUE__g',
                title='Геральт синий',
                fesh=12711,
                vendor_id=142,
                hid=14242,
                picture=Picture(
                    picture_id='iyC4nHslqLtqZJLygVAHeA',
                    width=400,
                    height=400,
                    thumb_mask=thumbnails_config.get_mask_by_names(['200x200']),
                    group_id=1234,
                ),
                glparams=[GLParam(param_id=420, value=3)],
                price=250,
                delivery_buckets=[14241],
                blue_without_real_sku=True,
            ),
            Offer(
                sku=1582,
                virtual_model_id=1582,
                vmid_is_literal=False,
                waremd5='OfferFastModel1BLUE__g',
                title='Мильва синяя',
                fesh=12711,
                vendor_id=142,
                hid=14242,
                picture=Picture(
                    picture_id='iyC4nHslqLtqZJLygVAHeA',
                    width=400,
                    height=400,
                    thumb_mask=thumbnails_config.get_mask_by_names(['200x200']),
                    group_id=1234,
                ),
                glparams=[GLParam(param_id=420, value=3)],
                price=333,
                delivery_buckets=[14241],
                blue_without_real_sku=True,
            ),
            # Оффер, скрытый динамиком
            Offer(
                sku=1583,
                virtual_model_id=1583,
                vmid_is_literal=False,
                waremd5='OfferFastModel2BLUE__g',
                title='Цири синяя',
                fesh=12711,
                vendor_id=142,
                hid=14242,
                picture=Picture(
                    picture_id='iyC4nHslqLtqZJLygVAHeA',
                    width=400,
                    height=400,
                    thumb_mask=thumbnails_config.get_mask_by_names(['200x200']),
                    group_id=1234,
                ),
                price=500,
                delivery_buckets=[14241],
                blue_without_real_sku=True,
                has_gone=True,
            ),
            # Оффер, карточка которого скрыта динамиком
            Offer(
                sku=1584,
                virtual_model_id=1584,
                vmid_is_literal=False,
                waremd5='OfferFastModel3BLUE__g',
                title='Йен синяя',
                fesh=12711,
                vendor_id=142,
                hid=14243,
                picture=Picture(
                    picture_id='iyC4nHslqLtqZJLygVAHeA',
                    width=400,
                    height=400,
                    thumb_mask=thumbnails_config.get_mask_by_names(['200x200']),
                    group_id=1234,
                ),
                price=600,
                delivery_buckets=[14241],
                blue_without_real_sku=True,
            ),
            Offer(
                sku=1585,
                virtual_model_id=1585,
                vmid_is_literal=False,
                waremd5='OfferFastModel4BLUE__g',
                title='Лютик синий',
                fesh=12711,
                vendor_id=142,
                hid=14243,
                picture=Picture(
                    picture_id='iyC4nHslqLtqZJLygVAHeA',
                    width=400,
                    height=400,
                    thumb_mask=thumbnails_config.get_mask_by_names(['200x200']),
                    group_id=1234,
                ),
                price=100,
                delivery_buckets=[14241],
                blue_without_real_sku=True,
            ),
        ]

        # Добавим побольше, чтобы убедиться, что карточка строится из оффера с минимальным оффсетом
        cls.index.offers += [
            Offer(
                sku=1581,
                virtual_model_id=1581,
                vmid_is_literal=False,
                waremd5='OfferFastModel{}______g'.format(i),
                title="Цири cpc 1581 {}".format(i),
                fesh=213,
                vendor_id=142,
                hid=14242,
            )
            for i in range(7)
        ]

        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=14240,
                fesh=12710,
                carriers=[99],
                regional_options=[RegionalDelivery(rid=213, options=[DeliveryOption(price=100, day_from=1, day_to=1)])],
            ),
            DeliveryBucket(
                bucket_id=14241,
                fesh=12711,
                carriers=[99],
                regional_options=[RegionalDelivery(rid=213, options=[DeliveryOption(price=100, day_from=1, day_to=1)])],
            ),
        ]

        cls.dynamic.market_dynamic.disabled_market_sku += [DynamicMarketSku(market_sku='1584')]

    def test_fast_cards(self):
        """
        Проверяем работу быстрых карточек
        По сути - те же виртуальне карточки, только могут иметь несколько офферов и айдишник неотличим от мску
        Под флагом: use_fast_cards
        """

        def check_right_response(response, offers_count):
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "results": [
                            {
                                "categories": [
                                    {
                                        "entity": "category",
                                        "id": 14242,
                                        "nid": 104242,
                                        "slug": "hid-14242",
                                        "type": "simple",
                                    }
                                ],
                                "description": "Качественная карта",
                                "entity": "product",
                                "filters": [
                                    {
                                        "id": "420",
                                        "type": "number",
                                        "values": [
                                            {
                                                "id": "found",
                                                "initialMax": "1",
                                                "initialMin": "1",
                                                "max": "1",
                                                "min": "1",
                                            }
                                        ],
                                        "xslname": "Quality",
                                    },
                                ],
                                "id": 1580,
                                "isVirtual": True,
                                "navnodes": [
                                    {
                                        "entity": "navnode",
                                        "id": 104242,
                                        "name": "Гвинтокарты",
                                    }
                                ],
                                "offers": {"count": offers_count, "cutPriceCount": 0},
                                "opinions": 44,
                                "pictures": [
                                    {
                                        "entity": "picture",
                                        "original": {
                                            "containerHeight": 200,
                                            "containerWidth": 200,
                                            "height": 200,
                                            "url": "http://avatars.mdst.yandex.net/get-marketpic/1234/market_iyC4nHslqLtqZJLygVAHeA/orig",
                                            "width": 200,
                                        },
                                    },
                                ],
                                "preciseRating": 4.31,
                                "rating": 4.3,
                                "ratingCount": 43,
                                "retailersCount": 0,
                                "reviews": 3,
                                "specs": {
                                    "full": [
                                        {
                                            "groupName": "Технические характеристики",
                                            "groupSpecs": [
                                                {
                                                    "desc": "Качество parameter description",
                                                    "name": "Качество",
                                                    "usedParams": [{"id": 420, "name": "GLPARAM-420"}],
                                                    "value": "1",
                                                }
                                            ],
                                        }
                                    ],
                                },
                                "titles": {"highlighted": [{"value": "Геральт cpc"}], "raw": "Геральт cpc"},
                                "type": "model",
                                "vendor": {
                                    "entity": "vendor",
                                    "id": 142,
                                    "name": "Гвинт",
                                    "website": "https://www.gwent.com",
                                },
                            }
                        ],
                        "total": 1,
                    }
                },
            )

        # Простой запрос с id быстрой карточки вместо модели,
        # моделька должна построиться из минимального по ts оффера - OfferFastModel0BLUE__g
        flags = '&rearr-factors=use_fast_cards=1'
        response = self.report.request_json('place=modelinfo&hyperid=1580&rids=213&show-models-specs=full' + flags)
        check_right_response(response, 3)
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "product",
                        "id": 1580,
                        "isVirtual": True,
                        "prices": {
                            # Сейчас по-умолчанию включен флаг market_cpa_only_fix_model_statistics (и market_cpa_only_enabled)
                            # Без расчета ДО есть поход в плейс model_statistics, под ним цены cpc не учитываются
                            "avg": "213",
                            "currency": "RUR",
                            "max": "250",
                            "min": "175",
                        },
                    }
                ]
            },
        )

        # Проверяем, что cpc учитывается в модельных статистиках для быстрых карточек
        flags = '&rearr-factors=use_fast_cards=1;market_cpa_only_enabled=0'
        response = self.report.request_json('place=modelinfo&hyperid=1580&rids=213&show-models-specs=full' + flags)
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "product",
                        "id": 1580,
                        "isVirtual": True,
                        "prices": {"avg": "200", "currency": "RUR", "max": "250", "min": "150"},
                    }
                ]
            },
        )

        # Проверяем, что при запросе с use-default-offers вернется OfferFastModel0CPA___g в блоке offers
        flags = '&rearr-factors=use_fast_cards=1'
        response = self.report.request_json(
            'place=modelinfo&hyperid=1580&rids=213&show-models-specs=full&use-default-offers=1' + flags
        )
        check_right_response(response, 2)
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "product",
                        "id": 1580,
                        "offers": {
                            # При использовании use-default-offers=1 для CPA применяется логика байбокса
                            # из-за этого синий оффер отфильтровывается и не участвует в статистике
                            # Такое поведение выглядит норм
                            "count": 2,
                            "items": [
                                {
                                    "cpa": "real",
                                    "wareId": "OfferFastModel0CPA___g",
                                }
                            ],
                        },
                        "prices": {
                            # При расчете ДО модельные статистики считаются с учетом cpc
                            "avg": "163",
                            "currency": "RUR",
                            "max": "175",
                            "min": "150",
                        },
                    }
                ]
            },
            allow_different_len=False,
        )

        # Карточка 1581 должна построиться из оффера с тайтлом 'Цири cpc 1581 6'
        flags = '&rearr-factors=use_fast_cards=1'
        response = self.report.request_json('place=modelinfo&hyperid=1581&rids=213&show-models-specs=full' + flags)
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "id": 1581,
                            "isVirtual": True,
                            "offers": {"count": 7, "cutPriceCount": 0},
                            "titles": {"highlighted": [{"value": "Цири cpc 1581 0"}], "raw": "Цири cpc 1581 0"},
                        }
                    ],
                    "total": 1,
                }
            },
            allow_different_len=False,
        )

        # Проверяем, что работает с несколькими БК-шными айдишниками
        flags = '&rearr-factors=use_fast_cards=1'
        response = self.report.request_json(
            'place=modelinfo&hyperid=1581&hyperid=1580&rids=213&show-models-specs=full' + flags
        )
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "id": 1581,
                            "isVirtual": True,
                            "offers": {"count": 7, "cutPriceCount": 0},
                            "titles": {"highlighted": [{"value": "Цири cpc 1581 0"}], "raw": "Цири cpc 1581 0"},
                        },
                        {
                            "id": 1580,
                            "isVirtual": True,
                            "offers": {"count": 3, "cutPriceCount": 0},
                            "titles": {"highlighted": [{"value": "Геральт cpc"}], "raw": "Геральт cpc"},
                        },
                    ],
                    "total": 2,
                }
            },
            allow_different_len=False,
        )

        # Проверяем, что работает айдишник БК + обычный
        flags = '&rearr-factors=use_fast_cards=1'
        response = self.report.request_json('place=modelinfo&hyperid=1580&hyperid=101&rids=213' + flags)
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "id": 101,
                            "offers": {
                                "count": 3,
                            },
                        },
                        {
                            "id": 1580,
                            "offers": {
                                "count": 3,
                            },
                        },
                    ],
                    "total": 2,
                }
            },
            allow_different_len=False,
        )

        # Проверяем, что работает айдишник БК + виртуальный
        flags = '&rearr-factors=use_fast_cards=1'
        response = self.report.request_json(
            'place=modelinfo&hyperid=1580&hyperid={}&rids=213'.format(T.virtual_model_id) + flags
        )
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "id": T.virtual_model_id,
                            "offers": {
                                "count": 1,
                            },
                        },
                        {
                            "id": 1580,
                            "offers": {
                                "count": 3,
                            },
                        },
                    ],
                    "total": 2,
                }
            },
            allow_different_len=False,
        )

        # Проверим, что карточка нормально строится из синего оффера
        flags = '&rearr-factors=use_fast_cards=1'
        response = self.report.request_json(
            'place=modelinfo&hyperid=1582&rids=213&show-models-specs=full&use-default-offers=1' + flags
        )
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "categories": [
                                {
                                    "entity": "category",
                                    "id": 14242,
                                    "nid": 104242,
                                    "slug": "hid-14242",
                                    "type": "simple",
                                }
                            ],
                            "description": "Качественная карта",
                            "entity": "product",
                            "filters": [
                                {
                                    "id": "420",
                                    "type": "number",
                                    "values": [
                                        {"id": "found", "initialMax": "3", "initialMin": "3", "max": "3", "min": "3"}
                                    ],
                                    "xslname": "Quality",
                                },
                            ],
                            "id": 1582,
                            "isVirtual": True,
                            "navnodes": [
                                {
                                    "entity": "navnode",
                                    "id": 104242,
                                    "name": "Гвинтокарты",
                                }
                            ],
                            "offers": {
                                "count": 1,
                                "cutPriceCount": 0,
                                "items": [
                                    {
                                        "cpa": "real",
                                        "wareId": "OfferFastModel1BLUE__g",
                                        "offerColor": "blue",
                                    }
                                ],
                            },
                            "opinions": 47,
                            "pictures": [
                                {
                                    "entity": "picture",
                                    "original": {
                                        "containerHeight": 400,
                                        "containerWidth": 400,
                                        "height": 400,
                                        "url": "http://avatars.mdst.yandex.net/get-marketpic/1234/market_iyC4nHslqLtqZJLygVAHeA/orig",
                                        "width": 400,
                                    },
                                },
                            ],
                            "prices": {"avg": "333", "currency": "RUR", "max": "333", "min": "333"},
                            "preciseRating": 4.51,
                            "rating": 4.5,
                            "ratingCount": 45,
                            "retailersCount": 0,
                            "reviews": 5,
                            "specs": {
                                "full": [
                                    {
                                        "groupName": "Технические характеристики",
                                        "groupSpecs": [
                                            {
                                                "desc": "Качество parameter description",
                                                "name": "Качество",
                                                "usedParams": [{"id": 420, "name": "GLPARAM-420"}],
                                                "value": "3",
                                            }
                                        ],
                                    }
                                ],
                            },
                            "titles": {"highlighted": [{"value": "Мильва синяя"}], "raw": "Мильва синяя"},
                            "type": "model",
                            "vendor": {
                                "entity": "vendor",
                                "id": 142,
                                "name": "Гвинт",
                                "website": "https://www.gwent.com",
                            },
                        }
                    ],
                    "total": 1,
                }
            },
        )

        # Без флага должен быть пустой результат
        response = self.report.request_json(
            'place=modelinfo&hyperid=1580&rids=213&show-models-specs=full&rearr-factors=use_fast_cards=0'
        )
        self.assertFragmentIn(
            response,
            {
                "total": 0,
                "results": [],
            },
        )

    def test_vmid_filters(self):
        """
        Проверям, что под флагом no_vmid_modelinfo_filter оффера
        виртуальных и быстрых карточек в modelinfo не фильтруются в common расчете релевантности,
        тк мы хотим отображать карточку по прямой ссылке, но без предложений
        """

        # Без флага карточка не будет отображаться, тк у оффера есть скрытие OFFER_HAS_GONE
        response = self.report.request_json(
            'place=modelinfo&hyperid=1583&rids=213&show-models-specs=full&rearr-factors=no_vmid_modelinfo_filter=0'
        )
        self.assertFragmentIn(
            response,
            {
                "total": 0,
                "results": [],
            },
        )

        # Под флагом карточку покажем
        response = self.report.request_json(
            'place=modelinfo&hyperid=1583&rids=213&show-models-specs=full&rearr-factors=no_vmid_modelinfo_filter=1'
        )
        self.assertFragmentIn(
            response,
            {
                "total": 1,
                "results": [
                    {
                        "categories": [{"id": 14242}],
                        "entity": "product",
                        "id": 1583,
                        "isVirtual": True,
                        "offers": {
                            "count": 0,  # в модельных статистиках нет оффера, тк он скрыт
                        },
                        "opinions": 50,
                        "pictures": NotEmpty(),
                        "preciseRating": 4.51,
                        "rating": 4.5,
                        "ratingCount": 25,
                        "reviews": 15,
                        "titles": {"raw": "Цири синяя"},
                        "type": "model",
                        "vendor": {
                            "entity": "vendor",
                            "name": "Гвинт",
                            "slug": "gvint",
                            "website": "https://www.gwent.com",
                        },
                    }
                ],
            },
        )

        # Айдишник мску 1584 скрыт динамиком, это скрытие мы учитываем для
        # быстрых и виртуальных карточек в model_info под тем же флагом
        response = self.report.request_json(
            'place=modelinfo&hyperid=1584&rids=213&show-models-specs=full&rearr-factors=no_vmid_modelinfo_filter=1'
        )
        self.assertFragmentIn(
            response,
            {
                "total": 0,
                "results": [],
            },
        )

        # Без флага мы покажем карточку, не учитывая скрытие динамиком
        response = self.report.request_json(
            'place=modelinfo&hyperid=1584&rids=213&show-models-specs=full&rearr-factors=no_vmid_modelinfo_filter=0'
        )
        self.assertFragmentIn(
            response,
            {
                "total": 1,
                "results": [
                    {
                        "categories": [{"id": 14243}],
                        "entity": "product",
                        "id": 1584,
                        "isVirtual": True,
                        "offers": {
                            "count": 0,  # Тут 0 офферов, тк в main релевантности проверяется скрытие мскю
                        },
                        "opinions": 50,
                        "pictures": NotEmpty(),
                        "preciseRating": 4.51,
                        "rating": 4.5,
                        "ratingCount": 25,
                        "reviews": 15,
                        "titles": {"raw": "Йен синяя"},
                        "type": "model",
                        "vendor": {
                            "entity": "vendor",
                            "name": "Гвинт",
                            "slug": "gvint",
                            "website": "https://www.gwent.com",
                        },
                    }
                ],
            },
        )

        # без скрытия динамиком по модели видим её
        response = self.report.request_json(
            'place=modelinfo&hyperid=1585&rids=213&show-models-specs=full&rearr-factors=no_vmid_modelinfo_filter=1'
        )
        self.assertFragmentIn(
            response,
            {
                "total": 1,
                "results": [
                    {
                        "categories": [{"id": 14243}],
                        "entity": "product",
                        "id": 1585,
                        "isVirtual": True,
                        "offers": {
                            "count": 1,
                        },
                        "opinions": 50,
                        "pictures": NotEmpty(),
                        "preciseRating": 4.51,
                        "rating": 4.5,
                        "ratingCount": 25,
                        "reviews": 15,
                        "titles": {"raw": "Лютик синий"},
                        "type": "model",
                        "vendor": {
                            "entity": "vendor",
                            "name": "Гвинт",
                            "slug": "gvint",
                            "website": "https://www.gwent.com",
                        },
                    }
                ],
            },
        )

        self.dynamic.market_dynamic.disabled_market_sku += [DynamicMarketSku(model_id=1585)]

        # Айдишник модели 1585 скрыт динамиком, это скрытие мы учитываем для
        # быстрых и виртуальных карточек в model_info под тем же флагом
        response = self.report.request_json(
            'place=modelinfo&hyperid=1585&rids=213&show-models-specs=full&rearr-factors=no_vmid_modelinfo_filter=1'
        )
        self.assertFragmentIn(
            response,
            {
                "total": 0,
                "results": [],
            },
        )

        # Без флага мы не учитываем скрытие по msku, но учтываем скрыти по модели (TTrivialInorderRelCalc::Calculate)
        response = self.report.request_json(
            'place=modelinfo&hyperid=1585&rids=213&show-models-specs=full&rearr-factors=no_vmid_modelinfo_filter=0'
        )
        self.assertFragmentIn(
            response,
            {
                "total": 0,
                "results": [],
            },
        )

    def test_models_for_several_mksu(self):
        """
        Проверям, что при запросе нескольких мску с разными моделями,
        корректно покажем для каждой мску нужную модель
        """

        response = self.report.request_json(
            'place=modelinfo&rids=213&show-models=1&market-sku=100210864759&market-sku=100210864584'
        )
        self.assertFragmentIn(
            response,
            {
                "total": 2,
                "results": [
                    {
                        "entity": "sku",
                        "id": "100210864759",
                        "product": {
                            "showUid": "",
                            "entity": "product",
                            "titles": {
                                "raw": "Мскушечная моделька",
                            },
                            "type": "model",
                            "id": 1,
                        },
                    },
                    {
                        "entity": "sku",
                        "id": "100210864584",
                        "product": {
                            "showUid": "",
                            "entity": "product",
                            "titles": {
                                "raw": "Еще одна Мскушечная моделька",
                            },
                            "type": "model",
                            "id": 5,
                        },
                    },
                ],
            },
        )


if __name__ == '__main__':
    main()

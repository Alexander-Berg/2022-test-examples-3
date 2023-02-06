#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from unittest import skip

from core.types import (
    GLParam,
    GLType,
    GLValue,
    HyperCategory,
    MnPlace,
    Model,
    Offer,
    YamarecPlace,
    YamarecSettingPartition,
)
from core.testcase import TestCase, main
from core.matcher import NotEmpty, Contains, ElementCount, Absent
from market.proto.recom.exported_dj_user_profile_pb2 import (
    TEcomVersionedDjUserProfile,
    TVersionedProfileData,
    TFashionDataV1,
    TFashionSizeDataV1,
)  # noqa pylint: disable=import-error


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.set_default_reqid = False

        cls.index.models += [
            Model(hyperid=101, accessories=[102, 103, 104, 105, 106, 107, 108, 0, 110, 111, 114]),
            Model(hyperid=102, accessories=[103, 104, 105, 0]),
            Model(hyperid=103, accessories=[0, 104, 105]),
            Model(hyperid=104, accessories=[1, 103, 105]),
            Model(hyperid=105, accessories=[103, 104, 1]),
            Model(hyperid=106, accessories=[102, 103, 104, 105, 1, 107, 108]),
            Model(hyperid=107),
            Model(hyperid=108),
            Model(hyperid=109, hid=100),
            Model(hyperid=110, hid=101, accessories=[112, 0, 113, 114, 115, 116, 117, 118]),
            Model(hyperid=111, hid=102),
            Model(hyperid=112, hid=101, accessories=[113, 114, 115, 116, 117, 118, 0, 111, 112]),
            Model(hyperid=113, hid=102),
            Model(hyperid=114, hid=101),
            Model(hyperid=115, hid=102),
            Model(hyperid=116, hid=101),
            Model(hyperid=117, hid=102),
            Model(hyperid=118, hid=102),
            # Модели 119-126 используются в prepare_separated_placements_for_accessories_and_complementary_products
            # Модели 127-130 используются в test_clothes_boosting_from_dj_profile
            Model(hyperid=0),
            Model(hyperid=1),
        ]
        offer_md5s = [
            "AccDelPrice1_________g",
            "AccDelPrice2_________g",
            "AccDelPrice3_________g",
            "AccDelPrice4_________g",
            "AccDelPrice5_________g",
            "AccDelPrice6_________g",
            "AccDelPrice7_________g",
            "AccDelPrice8_________g",
            # report filtering and merge testing starts from here
            "AccDelPrice9_________g",
            "AccDelPrice10________g",
            "AccDelPrice11________g",
            "AccDelPrice12________g",
            "AccDelPrice13________g",
        ]

        cls.index.offers += [
            Offer(hyperid=101, waremd5=offer_md5s[0]),
            Offer(hyperid=102, waremd5=offer_md5s[1]),
            Offer(hyperid=103, waremd5=offer_md5s[2]),
            Offer(hyperid=104, waremd5=offer_md5s[3]),
            Offer(hyperid=105, waremd5=offer_md5s[4]),
            Offer(hyperid=106, waremd5=offer_md5s[5]),
            Offer(hyperid=107, waremd5=offer_md5s[6]),
            Offer(hyperid=108, waremd5=offer_md5s[7]),
            # report filtering and merge testing starts from here
            Offer(hyperid=110, waremd5=offer_md5s[8]),
            Offer(hyperid=111, waremd5=offer_md5s[9]),
            Offer(hyperid=114, waremd5=offer_md5s[10]),
            Offer(hyperid=116, waremd5=offer_md5s[11]),
            Offer(hyperid=118, waremd5=offer_md5s[12]),
        ]

        cls.index.yamarec_places += [
            YamarecPlace(
                name=YamarecPlace.Name.EXTERNAL_PRODUCT_ACCESSORY_AND_COMPLEMENTARY_BLUE_MARKET,
                kind=YamarecPlace.Type.SETTING,
                split_rule=YamarecPlace.SplitRule.ABT,
                partitions=[
                    YamarecSettingPartition(
                        params={
                            'version': 'v1',
                            'use-local': '0',
                        },
                        splits=[{'split': '1'}],
                    ),
                    YamarecSettingPartition(
                        params={
                            'version': 'v1',
                            'use-local': '1',
                        },
                        splits=[{'split': '2'}],
                    ),
                    YamarecSettingPartition(params={'version': 'v1'}, splits=[{}]),
                ],
            )
        ]

        cls.recommender.on_request_accessory_models(model_id=101, item_count=1000, version='v1').respond(
            {'models': ['102', '103', '104', '105', '106', '107', '108', '0', '110', '111', '114']}
        )

        cls.recommender.on_request_accessory_models(model_id=102, item_count=1000, version='v1').respond(
            {'models': ['103', '104', '105', '0']}
        )

        cls.recommender.on_request_accessory_models(model_id=103, item_count=1000, version='v1').respond(
            {'models': ['0', '104', '105']}
        )

        cls.recommender.on_request_accessory_models(model_id=104, item_count=1000, version='v1').respond(
            {'models': ['1', '103', '105']}
        )

        cls.recommender.on_request_accessory_models(model_id=105, item_count=1000, version='v1').respond(
            {'models': ['103', '104', '1']}
        )

        cls.recommender.on_request_accessory_models(model_id=106, item_count=1000, version='v1').respond(
            {'models': ['102', '103', '104', '105', '1', '107', '108']}
        )

        cls.recommender.on_request_accessory_models(model_id=110, item_count=1000, version='v1').respond(
            {'models': ['112', '0', '113', '114', '115', '116', '117', '118']}
        )

        cls.recommender.on_request_accessory_models(model_id=112, item_count=1000, version='v1').respond(
            {'models': ['113', '114', '115', '116', '117', '118', '0', '111', '112']}
        )

        cls.recommender.on_request_accessory_models(model_id=107, item_count=1000, version='v1').respond({'models': []})

    def test_complementary_product_groups1(self):

        main_model = 101
        _ = ['102', '103', '104', '105', '106', '107', '108', '110', '111', '114']
        offer_md5s = [
            "AccDelPrice2_________g",
            "AccDelPrice3_________g",
            "AccDelPrice4_________g",
            "AccDelPrice5_________g",
            "AccDelPrice6_________g",
            "AccDelPrice7_________g",
            "AccDelPrice8_________g",
            "AccDelPrice9_________g",
            "AccDelPrice10________g",
            "AccDelPrice11________g",
        ]

        for split in range(1, 2):
            query = "place=complementary_product_groups&hyperid={id}&yandexuid=001&pp=143&rgb=green&rearr-factors=split={split}".format(
                id=main_model, split=split
            )
            response = self.report.request_json(query)
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        'total': 10,
                        'results': [
                            {
                                'entity': 'group',
                                'total': 7,
                                'id': NotEmpty(),
                                'results': [
                                    {'offers': {'items': [{'wareId': offer_md5s[0]}]}},
                                    {'offers': {'items': [{'wareId': offer_md5s[1]}]}},
                                    {'offers': {'items': [{'wareId': offer_md5s[2]}]}},
                                    {'offers': {'items': [{'wareId': offer_md5s[3]}]}},
                                    {'offers': {'items': [{'wareId': offer_md5s[4]}]}},
                                    {'offers': {'items': [{'wareId': offer_md5s[5]}]}},
                                    {'offers': {'items': [{'wareId': offer_md5s[6]}]}},
                                ],
                            },
                            {
                                'entity': 'group',
                                'total': 3,
                                'id': NotEmpty(),
                                'results': [
                                    {'offers': {'items': [{'wareId': offer_md5s[7]}]}},
                                    {'offers': {'items': [{'wareId': offer_md5s[8]}]}},
                                    {'offers': {'items': [{'wareId': offer_md5s[9]}]}},
                                ],
                            },
                        ],
                    }
                },
            )

    def test_complementary_product_groups2(self):

        main_model = 102
        _ = ['103', '104', '105']
        offer_md5s = ["AccDelPrice3_________g", "AccDelPrice4_________g", "AccDelPrice5_________g"]

        for split in range(1, 2):
            query = "place=complementary_product_groups&hyperid={id}&yandexuid=001&pp=143&rgb=green&rearr-factors=split={split}".format(
                id=main_model, split=split
            )
            response = self.report.request_json(query)
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        'total': 3,
                        'results': [
                            {
                                'entity': 'group',
                                'total': 3,
                                'id': NotEmpty(),
                                'results': [
                                    {'offers': {'items': [{'wareId': offer_md5s[0]}]}},
                                    {'offers': {'items': [{'wareId': offer_md5s[1]}]}},
                                    {'offers': {'items': [{'wareId': offer_md5s[2]}]}},
                                ],
                            },
                            {'entity': 'group', 'total': 0, 'id': NotEmpty(), 'results': []},
                        ],
                    }
                },
            )

    def test_complementary_product_groups3(self):

        main_model = 103
        _ = []
        offer_md5s = ["AccDelPrice4_________g", "AccDelPrice5_________g"]
        for split in range(1, 2):
            query = "place=complementary_product_groups&hyperid={id}&yandexuid=001&pp=143&rgb=green&rearr-factors=split={split}".format(
                id=main_model, split=split
            )
            response = self.report.request_json(query)
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        'total': 2,
                        'results': [
                            {
                                'entity': 'group',
                                'total': 2,
                                'id': NotEmpty(),
                                'results': [
                                    {'offers': {'items': [{'wareId': offer_md5s[0]}]}},
                                    {'offers': {'items': [{'wareId': offer_md5s[1]}]}},
                                ],
                            },
                            {'entity': 'group', 'total': 0, 'id': NotEmpty(), 'results': []},
                        ],
                    }
                },
            )

    def test_complementary_product_groups4(self):

        main_model = 104
        _ = ['103', '105']
        offer_md5s = ["AccDelPrice3_________g", "AccDelPrice5_________g"]

        for split in range(1, 2):
            query = "place=complementary_product_groups&hyperid={id}&yandexuid=001&pp=143&rgb=green&rearr-factors=split={split}".format(
                id=main_model, split=split
            )
            response = self.report.request_json(query)
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        'total': 2,
                        'results': [
                            {'entity': 'group', 'total': 0, 'id': NotEmpty(), 'results': []},
                            {
                                'entity': 'group',
                                'total': 2,
                                'id': NotEmpty(),
                                'results': [
                                    {'offers': {'items': [{'wareId': offer_md5s[0]}]}},
                                    {'offers': {'items': [{'wareId': offer_md5s[1]}]}},
                                ],
                            },
                        ],
                    }
                },
            )

    def test_complementary_product_groups5(self):

        main_model = 105
        _ = []
        for split in range(1, 2):
            query = "place=complementary_product_groups&hyperid={id}&yandexuid=001&pp=143&rgb=green&rearr-factors=split={split}".format(
                id=main_model, split=split
            )
            response = self.report.request_json(query)
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        'total': 0,
                        'results': [
                            {'entity': 'group', 'total': 0, 'id': NotEmpty(), 'results': []},
                            {'entity': 'group', 'total': 0, 'id': NotEmpty(), 'results': []},
                        ],
                    }
                },
            )

    def test_complementary_product_groups6(self):

        main_model = 106
        _ = ['107', '108']
        offer_md5s = ["AccDelPrice7_________g", "AccDelPrice8_________g"]
        for split in range(1, 2):
            query = "place=complementary_product_groups&hyperid={id}&yandexuid=001&pp=143&rgb=green&rearr-factors=split={split}".format(
                id=main_model, split=split
            )
            response = self.report.request_json(query)
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        'total': 2,
                        'results': [
                            {'entity': 'group', 'total': 0, 'id': NotEmpty(), 'results': []},
                            {
                                'entity': 'group',
                                'total': 2,
                                'id': NotEmpty(),
                                'results': [
                                    {'offers': {'items': [{'wareId': offer_md5s[0]}]}},
                                    {'offers': {'items': [{'wareId': offer_md5s[1]}]}},
                                ],
                            },
                        ],
                    }
                },
            )

    def test_complementary_product_groups8(self):

        main_model = 107
        _ = []
        for split in range(1, 2):
            query = "place=complementary_product_groups&hyperid={id}&yandexuid=001&pp=143&rgb=green&rearr-factors=split={split}".format(
                id=main_model, split=split
            )
            response = self.report.request_json(query)
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        'total': 0,
                        'results': [
                            {'entity': 'group', 'total': 0, 'id': NotEmpty(), 'results': []},
                            {'entity': 'group', 'total': 0, 'id': NotEmpty(), 'results': []},
                        ],
                    }
                },
            )

    def test_min_count(self):

        main_model = 101
        _ = ['102', '103', '104', '105', '106', '107', '108']
        offer_md5s = [
            "AccDelPrice2_________g",
            "AccDelPrice3_________g",
            "AccDelPrice4_________g",
            "AccDelPrice5_________g",
            "AccDelPrice6_________g",
            "AccDelPrice7_________g",
            "AccDelPrice8_________g",
        ]

        for split in range(1, 2):
            query = "place=complementary_product_groups&hyperid={id}&yandexuid=001&pp=143&rgb=green&min-count-to-show=4&rearr-factors=split={split}".format(
                id=main_model, split=split
            )
            response = self.report.request_json(query)
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        'total': 7,
                        'results': [
                            {
                                'entity': 'group',
                                'total': 7,
                                'id': NotEmpty(),
                                'results': [
                                    {'offers': {'items': [{'wareId': offer_md5s[0]}]}},
                                    {'offers': {'items': [{'wareId': offer_md5s[1]}]}},
                                    {'offers': {'items': [{'wareId': offer_md5s[2]}]}},
                                    {'offers': {'items': [{'wareId': offer_md5s[3]}]}},
                                    {'offers': {'items': [{'wareId': offer_md5s[4]}]}},
                                    {'offers': {'items': [{'wareId': offer_md5s[5]}]}},
                                    {'offers': {'items': [{'wareId': offer_md5s[6]}]}},
                                ],
                            },
                            {'entity': 'group', 'total': 0, 'id': NotEmpty(), 'results': []},
                        ],
                    }
                },
            )

    def test_show_all_overrides_min_count(self):
        main_model = 101

        for split in range(1, 2):
            for show_all in (None, "0", "1"):
                query = "place=complementary_product_groups&hyperid={id}&yandexuid=001&pp=143&rgb=green&min-count-to-show=8&rearr-factors=split={split}{show_all_param}".format(
                    id=main_model,
                    split=split,
                    show_all_param=("" if show_all is None else "&show-all-complementary-products=" + show_all),
                )
                response = self.report.request_json(query)
                self.assertFragmentIn(
                    response,
                    {
                        "search": {
                            'total': 0 if (show_all != "1") else 10,
                        }
                    },
                )

    def test_show_all_overrides_accessory_category_condition(self):
        main_model = 102
        _ = ['103', '104', '105']
        offer_md5s = ["AccDelPrice3_________g", "AccDelPrice4_________g", "AccDelPrice5_________g"]

        for split in range(1, 2):
            for show_all in (None, "0", "1"):
                query = "place=complementary_product_groups&hyperid={id}&yandexuid=001&pp=143&rgb=green&min-count-to-show=4&rearr-factors=split={split}{show_all_param}".format(
                    id=main_model,
                    split=split,
                    show_all_param=("" if show_all is None else "&show-all-complementary-products=" + show_all),
                )
                response = self.report.request_json(query)
                full_results = [
                    {
                        'entity': 'group',
                        'total': 3,
                        'id': NotEmpty(),
                        'results': [
                            {'offers': {'items': [{'wareId': offer_md5s[0]}]}},
                            {'offers': {'items': [{'wareId': offer_md5s[1]}]}},
                            {'offers': {'items': [{'wareId': offer_md5s[2]}]}},
                        ],
                    },
                    {'entity': 'group', 'total': 0, 'id': NotEmpty(), 'results': []},
                ]
                empty_results = [
                    {'entity': 'group', 'total': 0, 'id': NotEmpty(), 'results': []},
                    {'entity': 'group', 'total': 0, 'id': NotEmpty(), 'results': []},
                ]
                self.assertFragmentIn(
                    response,
                    {
                        "search": {
                            'total': 3 if (show_all == "1") else 0,
                            'results': full_results if (show_all == "1") else empty_results,
                        }
                    },
                )

    def test_report_diversification_after_filtering(self):
        main_model = 110
        _ = ['114', '116', '118']
        offer_md5s = ["AccDelPrice11________g", "AccDelPrice13________g", "AccDelPrice12________g"]

        for split in range(1, 2):
            query = "place=complementary_product_groups&hyperid={id}&yandexuid=001&pp=143&rgb=green&rearr-factors=split={split}&show-all-complementary-products=1".format(
                id=main_model, split=split
            )
            response = self.report.request_json(query)
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        'total': 3,
                        'results': [
                            {'entity': 'group', 'total': 0, 'id': NotEmpty(), 'results': []},
                            {
                                'entity': 'group',
                                'total': 3,
                                'id': NotEmpty(),
                                'results': [
                                    {'offers': {'items': [{'wareId': offer_md5s[0]}]}},
                                    {'offers': {'items': [{'wareId': offer_md5s[1]}]}},
                                    {'offers': {'items': [{'wareId': offer_md5s[2]}]}},
                                ],
                            },
                        ],
                    }
                },
            )

    def test_merge_accessories(self):
        main_model = 112
        _ = ['114', '118', '111', '116']
        offer_md5s = [
            "AccDelPrice11________g",
            "AccDelPrice10________g",
            "AccDelPrice13________g",
            "AccDelPrice12________g",
        ]

        for split in range(1, 2):
            query = "place=complementary_product_groups&hyperid={id}&yandexuid=001&pp=143&rgb=green&rearr-factors=split={split}&show-all-complementary-products=1".format(
                id=main_model, split=split
            )
            response = self.report.request_json(query)
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        'total': 4,
                        'results': [
                            {'entity': 'group', 'total': 0, 'id': NotEmpty(), 'results': []},
                            {
                                'entity': 'group',
                                'total': 4,
                                'id': NotEmpty(),
                                'results': [
                                    {'offers': {'items': [{'wareId': offer_md5s[0]}]}},
                                    {'offers': {'items': [{'wareId': offer_md5s[1]}]}},
                                    {'offers': {'items': [{'wareId': offer_md5s[2]}]}},
                                    {'offers': {'items': [{'wareId': offer_md5s[3]}]}},
                                ],
                            },
                        ],
                    }
                },
            )

    @classmethod
    def prepare_separated_placements_for_accessories_and_complementary_products(cls):
        cls.index.models += [
            # accessories
            Model(hyperid=119, hid=103, accessories=[120]),
            Model(hyperid=120, hid=104),
            Model(hyperid=121, hid=105),
            Model(hyperid=122, hid=106),
            Model(hyperid=123, hid=107),
            Model(hyperid=124, hid=108),
            Model(hyperid=125, hid=109),
            # complementary products
            Model(hyperid=126, hid=110),
        ]

        cls.index.offers += [
            # accessories
            Offer(hyperid=119, cpa=Offer.CPA_REAL),
            Offer(hyperid=120, cpa=Offer.CPA_REAL),
            Offer(hyperid=121, cpa=Offer.CPA_REAL),
            Offer(hyperid=122, cpa=Offer.CPA_REAL),
            Offer(hyperid=123, cpa=Offer.CPA_REAL),
            Offer(hyperid=124, cpa=Offer.CPA_REAL),
            Offer(hyperid=125, cpa=Offer.CPA_REAL),
            # complementary products
            Offer(hyperid=126, cpa=Offer.CPA_REAL),
        ]

        cls.recommender.on_request_accessory_models(model_id=119, item_count=1000, version="v1").respond(
            {"models": ['119', '120', '121', '122', '123', '124', '125', '0', '126']}
        )

    def test_separated_placements_for_accessories_and_complementary_products(self):
        """
        Тестируем, что в ответе на запрос в complementary_product_groups у акксесуаров и у "с этим товаром покупают" в урлах
        будут разные рр
        """

        # первый запрос в плейсмент десктопа, рр=140. Ожидаем в ответе для аксессуаров 140, у сопутствующих товаров 149
        query = "place=complementary_product_groups&hyperid=119&pp=140&show-urls=cpa"
        response = self.report.request_json(query)
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "key": "accessories",
                            "results": [{"offers": {"items": [{"urls": {"cpa": Contains('pp=140')}}]}}],
                        },
                        {
                            "key": "complementary_items",
                            "results": [{"offers": {"items": [{"urls": {"cpa": Contains('pp=149')}}]}}],
                        },
                    ]
                }
            },
        )

        # второй запрос в плейсмент тача, рр=640. Ожидаем в ответе для аксессуаров 640, у сопутствующих товаров 649
        query = "place=complementary_product_groups&hyperid=119&pp=640&show-urls=cpa"
        response = self.report.request_json(query)
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "key": "accessories",
                            "results": [{"offers": {"items": [{"urls": {"cpa": Contains('pp=640')}}]}}],
                        },
                        {
                            "key": "complementary_items",
                            "results": [{"offers": {"items": [{"urls": {"cpa": Contains('pp=649')}}]}}],
                        },
                    ]
                }
            },
        )

    @classmethod
    def prepare_clothes_boosting_from_dj_profile(cls):
        cls.settings.set_default_reqid = False

        ClothesSizeParamId = 26417130
        ClothesSizeParamValues = [
            (27016810, 'XS'),
            (27016830, 'S'),
            (27016891, 'M'),
            (27016892, 'L'),
            (27016910, 'XL'),
        ]

        cls.index.hypertree += [
            HyperCategory(
                hid=7877999,
                name='Одежда, обувь и аксессуары',
                children=[
                    HyperCategory(
                        hid=7811873,
                        name='Женская одежда',
                        children=[
                            HyperCategory(hid=7811945, name='Женские платья'),
                        ],
                    ),
                ],
            ),
            HyperCategory(hid=11111, name='Какая-то категория не одежды'),
        ]

        cls.index.gltypes += [
            GLType(
                param_id=ClothesSizeParamId,
                hid=7811945,
                gltype=GLType.ENUM,
                xslname='size_clothes_new',
                values=[
                    GLValue(value_id=value_id, text=param_name) for (value_id, param_name) in ClothesSizeParamValues
                ],
            ),
            GLType(
                param_id=ClothesSizeParamId,
                hid=7811873,
                gltype=GLType.ENUM,
                xslname='size_clothes_new',
                values=[
                    GLValue(value_id=value_id, text=param_name) for (value_id, param_name) in ClothesSizeParamValues
                ],
            ),
        ]

        cls.index.models += [
            Model(hyperid=127, hid=7812186, title='Куртка мужская Adidas', accessories=[128, 0]),
            Model(hyperid=128, hid=7811945, title='Платье женское Baon'),
            Model(hyperid=129, hid=11111, title='Модель не одежды', accessories=[130, 0]),
            Model(hyperid=130, hid=11111, title='Модель не одежды 2'),
        ]

        for seq, (value_id, param_name) in enumerate(ClothesSizeParamValues):
            cls.index.offers += [
                Offer(
                    hid=7811945,
                    hyperid=128,
                    price=100 - seq * 10,
                    title='Платье женское Baon размер ' + param_name,
                    glparams=[
                        GLParam(param_id=ClothesSizeParamId, value=value_id),
                    ],
                    ts=4423750 + seq,
                    randx=seq,
                ),
            ]

        for seq in range(15, 20):
            cls.index.offers += [
                Offer(
                    hid=11111,
                    hyperid=130,
                    price=100,
                    title='Не одежда (hyperid = 130) ' + str(seq),
                    glparams=[
                        GLParam(param_id=15, value=30 - seq),
                    ],
                    ts=4423850 + seq,
                    randx=seq,
                ),
            ]

        for seq in range(0, 50):
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 4423750 + seq).respond(0.5 - seq * 0.01)
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 4423850 + seq).respond(0.5 - seq * 0.01)

        cls.recommender.on_request_accessory_models(model_id=127, item_count=1000, version='v1').respond(
            {'models': ['128', '0']}
        )
        cls.recommender.on_request_accessory_models(model_id=129, item_count=1000, version='v1').respond(
            {'models': ['130', '0']}
        )

        # настраиваем Dj, чтобы возвращал нам профиль для пользователя с yandexuid = 011
        profile = TEcomVersionedDjUserProfile(
            ProfileData=TVersionedProfileData(
                FashionV1=TFashionDataV1(
                    SizeClothesFemale=TFashionSizeDataV1(
                        Sizes={"46": 0.288602501, "48": 0.355698764, "S": 0.395698764, "M": 0.288602501}
                    ),
                )
            )
        )

        cls.dj.on_request(yandexuid='011', exp='fetch_user_profile_versioned').respond(
            profile_data=profile.SerializeToString(), is_binary_data=True
        )

    @skip('deleted old booster')
    def test_clothes_boosting_from_dj_profile(self):
        """
        Тестируем, что просходит загрузка еком-профиля из Dj. Он используется для бустинга тех офферов, у которых размер
        совпадает с размером одежды пользователя из этого самого профиля
        """

        # если одежда и не выставлены флаги, то ничего не бустим. Побеждает XS, потому что у него значение матрикснет меньше
        for rearr in ('', ';fetch_recom_profile_for_model_place=0'):
            for split in range(1, 2):
                request = "place=complementary_product_groups&hyperid=127&yandexuid=011&rearr-factors=split={split}{rearr}".format(
                    split=split, rearr=rearr
                )
                response = self.report.request_json(request)
                self.assertFragmentIn(
                    response,
                    {
                        "search": {
                            "results": [
                                {
                                    "key": "complementary_items",
                                    "results": [
                                        {
                                            "id": 128,
                                            "offers": {
                                                "items": [
                                                    {
                                                        "titles": {"raw": "Платье женское Baon размер XS"},
                                                    }
                                                ]
                                            },
                                        }
                                    ],
                                }
                            ]
                        }
                    },
                )

        # если одежда и выставлены флаги, то побеждает оффер размера S, потому что его бустят
        # (потому что он указан в еком профиле)
        rearr_factors = [
            'market_boost_single_personal_gl_param_coeff=1.2',
            'fetch_recom_profile_for_model_place=1',
            'split=normal',
        ]

        for rearr in ('', ';fetch_recom_profile_for_model_place=0'):
            for split in range(1, 2):
                request = "place=complementary_product_groups&hyperid=127&yandexuid=011&rearr-factors=split={split}{rearr};{enable_flags}".format(
                    split=split, rearr=rearr, enable_flags=';'.join(rearr_factors)
                )
                response = self.report.request_json(request)
                self.assertFragmentIn(
                    response,
                    {
                        "search": {
                            "results": [
                                {
                                    "key": "complementary_items",
                                    "results": [
                                        {
                                            "id": 128,
                                            "offers": {
                                                "items": [
                                                    {
                                                        "titles": {"raw": "Платье женское Baon размер S"},
                                                    }
                                                ]
                                            },
                                        }
                                    ],
                                }
                            ]
                        }
                    },
                )

        # если не одежда - бустинга не происходит, что с флагами, что без
        for rearr in (
            "",
            "&rearr-factors=market_boost_single_personal_gl_param_coeff=1.2;fetch_recom_profile_for_model_place=1",
            "&rearr-factors=market_boost_single_personal_gl_param_coeff=1.2;fetch_recom_profile_for_model_place=0",
        ):
            for split in range(1, 2):
                request = "place=complementary_product_groups&hyperid=129&yandexuid=011&rearr-factors=split={split}{rearr}".format(
                    split=split, rearr=rearr
                )
                response = self.report.request_json(request)
                self.assertFragmentIn(
                    response,
                    {
                        "search": {
                            "results": [
                                {
                                    "key": "complementary_items",
                                    "results": [
                                        {
                                            "id": 130,
                                            "offers": {
                                                "items": [
                                                    {
                                                        "titles": {"raw": "Не одежда (hyperid = 130) 15"},
                                                    }
                                                ]
                                            },
                                        }
                                    ],
                                }
                            ]
                        }
                    },
                )

    @classmethod
    def prepare_unique_categories_generation(cls):
        cls.index.models += [
            Model(hyperid=131, hid=8811321),
            Model(hyperid=132, hid=8811321),
            Model(hyperid=133, hid=8811322, accessories=[131, 132, 0, 134, 135]),
            Model(hyperid=134, hid=8811323),
            Model(hyperid=135, hid=8811324),
            Model(hyperid=136, hid=8811325),
            Model(hyperid=137, hid=8811326),
            Model(hyperid=138, hid=8811327, accessories=[131, 132, 133, 139, 0, 134, 135, 136, 140]),
            Model(hyperid=139, hid=8811328),
            Model(hyperid=140, hid=8811329),
        ]

        cls.index.offers += [
            Offer(hyperid=131, cpa=Offer.CPA_REAL),
            Offer(hyperid=132, cpa=Offer.CPA_REAL),
            Offer(hyperid=133, cpa=Offer.CPA_REAL),
            Offer(hyperid=134, cpa=Offer.CPA_REAL),
            Offer(hyperid=135, cpa=Offer.CPA_REAL),
            Offer(hyperid=136, cpa=Offer.CPA_REAL),
            Offer(hyperid=137, cpa=Offer.CPA_REAL),
            Offer(hyperid=138, cpa=Offer.CPA_REAL),
            Offer(hyperid=139, cpa=Offer.CPA_REAL),
            Offer(hyperid=140, cpa=Offer.CPA_REAL),
        ]

        cls.recommender.on_request_accessory_models(model_id=133, item_count=1000, version='v1').respond(
            {'models': ['131', '132', '0', '134', '135']}
        )

        cls.recommender.on_request_accessory_models(model_id=138, item_count=1000, version='v1').respond(
            {'models': ['131', '132', '133', '139', '134', '135', '136', '0', '140']}
        )

    def test_unique_categories_generation(self):

        query = "place=complementary_product_groups&hyperid=133&pp=140&show-urls=cpa"
        response = self.report.request_json(query)
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "key": "complementary_items",
                            "categories": [
                                {
                                    "hid": 8811321,
                                },
                                {
                                    "hid": 8811323,
                                },
                                {
                                    "hid": 8811324,
                                },
                            ],
                        }
                    ]
                }
            },
        )

        query = "place=complementary_product_groups&hyperid=138&pp=140&show-urls=cpa"
        response = self.report.request_json(query)
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {"key": "accessories", "categories": ElementCount(6)},
                        {"key": "complementary_items", "categories": ElementCount(1)},
                    ]
                }
            },
        )

    def test_i2i_group_key_filter(self):
        query = "place=complementary_product_groups&hyperid=138&pp=140&show-urls=cpa&i2i-group-key-filter=accessories"
        response = self.report.request_json(query)
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {"key": "accessories", "categories": ElementCount(6), "results": ElementCount(7)},
                    ]
                }
            },
        )
        self.assertFragmentNotIn(
            response,
            {
                "search": {
                    "results": [
                        {"key": "complementary_items"},
                        {"key": "accessories_and_complementary_items"},
                    ]
                }
            },
        )

        query = "place=complementary_product_groups&hyperid=138&pp=140&show-urls=cpa&i2i-group-key-filter=complementary_items"
        response = self.report.request_json(query)
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {"key": "complementary_items", "categories": ElementCount(1), "results": ElementCount(1)},
                    ]
                }
            },
        )
        self.assertFragmentNotIn(
            response,
            {
                "search": {
                    "results": [
                        {"key": "accessories"},
                        {"key": "accessories_and_complementary_items"},
                    ]
                }
            },
        )

        query = "place=complementary_product_groups&hyperid=138&pp=140&show-urls=cpa&i2i-group-key-filter=accessories_and_complementary_items"
        response = self.report.request_json(query)
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "key": "accessories_and_complementary_items",
                            "categories": ElementCount(7),
                            "results": ElementCount(8),
                        },
                    ]
                }
            },
        )
        self.assertFragmentNotIn(
            response,
            {
                "search": {
                    "results": [
                        {"key": "accessories"},
                        {"key": "complementary_items"},
                    ]
                }
            },
        )

        query = "place=complementary_product_groups&hyperid=138&pp=140&show-urls=cpa&i2i-group-key-filter=accessories_and_complementary_items&hid=8811321"
        response = self.report.request_json(query)
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "key": "accessories_and_complementary_items",
                            "categories": ElementCount(1),
                            "results": ElementCount(2),
                        },
                    ]
                }
            },
        )

    def test_hid_filtration(self):
        query = "place=complementary_product_groups&hyperid=138&pp=140&show-urls=cpa&hid=8811321"
        response = self.report.request_json(query)
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {"key": "accessories", "categories": ElementCount(1), "results": ElementCount(2)},
                        {"key": "complementary_items", "categories": ElementCount(0), "results": ElementCount(0)},
                    ]
                }
            },
        )
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {"key": "accessories", "categories": [{"hid": 8811321}]},
                    ]
                }
            },
        )

        # in case we set group-key-filter it forces items to go under the specific bucket
        # 8811321 - is accessory hid
        query = "place=complementary_product_groups&hyperid=138&pp=140&show-urls=cpa&hid=8811321&i2i-group-key-filter=complementary_items"
        response = self.report.request_json(query)
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {"key": "complementary_items", "categories": ElementCount(1), "results": ElementCount(2)},
                    ]
                }
            },
        )

        # 8811323 - is complementary hid
        query = "place=complementary_product_groups&hyperid=138&pp=140&show-urls=cpa&hid=8811329&i2i-group-key-filter=accessories"
        response = self.report.request_json(query)
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {"key": "accessories", "categories": ElementCount(1), "results": ElementCount(1)},
                    ]
                }
            },
        )

    @classmethod
    def prepare_filters_test(cls):
        cls.index.models += [
            Model(hyperid=201, glparams=[GLParam(param_id=1, value=2)]),
            Model(hyperid=202, accessories=[201, 0]),
        ]

        cls.index.offers += [
            Offer(hyperid=201, cpa=Offer.CPA_REAL),
            Offer(hyperid=202, cpa=Offer.CPA_REAL),
        ]

        cls.recommender.on_request_accessory_models(model_id=202, item_count=1000, version='v1').respond(
            {'models': ['201', '0']}
        )

    def test_filters_in_output(self):
        """
        Проверяем, что с флагом market_complementary_product_groups_show_filters=1 у модели есть фильтры
        """
        query = "place=complementary_product_groups&hyperid=202&pp=140&show-urls=cpa"
        rearr = "&rearr-factors=market_complementary_product_groups_show_filters=1"
        response = self.report.request_json(query + rearr)
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "key": "complementary_items",
                            "results": [
                                {
                                    "entity": "product",
                                    "id": 201,
                                    "filters": [
                                        {
                                            "id": "1",
                                            "values": [
                                                {
                                                    "id": "2",
                                                },
                                            ],
                                        },
                                    ],
                                },
                            ],
                        }
                    ]
                }
            },
        )

    def test_filters_not_in_output(self):
        """
        Проверяем, что с флагом market_complementary_product_groups_show_filters=0 и по умолчанию у модели нет фильтров
        """
        query = "place=complementary_product_groups&hyperid=202&pp=140&show-urls=cpa"
        for rearr in ["&rearr-factors=market_complementary_product_groups_show_filters=0", ""]:
            response = self.report.request_json(query + rearr)
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "results": [
                            {
                                "key": "complementary_items",
                                "results": [
                                    {
                                        "entity": "product",
                                        "id": 201,
                                        "filters": Absent(),
                                    },
                                ],
                            }
                        ]
                    }
                },
            )


if __name__ == '__main__':
    main()

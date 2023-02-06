#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import Model, RegionalModel, Shop, YamarecPlace, YamarecSettingPartition
from core.testcase import TestCase, main
from core.types.sku import MarketSku, BlueOffer
from core.types.taxes import Vat
from core.matcher import Absent, NotEmpty
from simple_testcase import create_model_with_default_offer


class _Offers(object):
    sku_offer1 = BlueOffer(price=1, vat=Vat.VAT_0, offerid='Shop_sku1', feedid=100, waremd5='Sku1Price1-IiLVm1Goleg')
    sku_offer2 = BlueOffer(price=1, vat=Vat.VAT_0, offerid='Shop_sku2', feedid=100, waremd5='Sku2Price1-IiLVm1Goleg')
    sku_offer3 = BlueOffer(price=1, vat=Vat.VAT_0, offerid='Shop_sku3', feedid=100, waremd5='Sku3Price1-IiLVm1Goleg')
    sku_offer4 = BlueOffer(price=1, vat=Vat.VAT_0, offerid='Shop_sku4', feedid=100, waremd5='Sku4Price1-IiLVm1Goleg')
    sku_offer5 = BlueOffer(price=1, vat=Vat.VAT_0, offerid='Shop_sku5', feedid=100, waremd5='Sku5Price1-IiLVm1Goleg')
    sku_offer6 = BlueOffer(price=1, vat=Vat.VAT_0, offerid='Shop_sku6', feedid=100, waremd5='Sku6Price1-IiLVm1Goleg')
    sku_offer7 = BlueOffer(price=1, vat=Vat.VAT_0, offerid='Shop_sku7', feedid=100, waremd5='Sku7Price1-IiLVm1Goleg')
    sku_offer8 = BlueOffer(price=1, vat=Vat.VAT_0, offerid='Shop_sku8', feedid=100, waremd5='Sku8Price1-IiLVm1Goleg')


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.set_default_reqid = False

        cls.index.models += [
            Model(hyperid=101, hid=101, accessories=[102, 103, 104, 105, 0, 106, 107, 108]),
            Model(hyperid=102, hid=101, accessories=[103, 104, 105, 0]),
            Model(hyperid=103, hid=101, accessories=[0, 104, 105]),
            Model(hyperid=104, hid=101, accessories=[1, 103, 105]),
            Model(hyperid=105, hid=101, accessories=[103, 104, 1]),
            Model(hyperid=106, hid=101, accessories=[102, 103, 104, 105, 1, 107, 108]),
            Model(hyperid=107, hid=101),
            Model(hyperid=108, hid=101),
            Model(hyperid=0, hid=101),
            Model(hyperid=1, hid=101),
        ]

        cls.index.regional_models += [
            RegionalModel(hyperid=101, offers=1, rids=[213]),
            RegionalModel(hyperid=102, offers=1, rids=[213]),
            RegionalModel(hyperid=103, offers=1, rids=[213]),
            RegionalModel(hyperid=104, offers=1, rids=[213]),
            RegionalModel(hyperid=105, offers=1, rids=[213]),
            RegionalModel(hyperid=106, offers=1, rids=[213]),
            RegionalModel(hyperid=107, offers=1, rids=[213]),
            RegionalModel(hyperid=108, offers=1, rids=[213]),
        ]

        cls.index.shops += [
            Shop(
                fesh=2,
                priority_region=213,
                cpa=Shop.CPA_REAL,
            ),
        ]

        cls.index.mskus += [
            MarketSku(title="blue offer sku1", hyperid=101, sku=1, blue_offers=[_Offers.sku_offer1]),
            MarketSku(title="blue offer sku2", hyperid=102, sku=2, blue_offers=[_Offers.sku_offer2]),
            MarketSku(title="blue offer sku3", hyperid=103, sku=3, blue_offers=[_Offers.sku_offer3]),
            MarketSku(title="blue offer sku4", hyperid=104, sku=4, blue_offers=[_Offers.sku_offer4]),
            MarketSku(title="blue offer sku5", hyperid=105, sku=5, blue_offers=[_Offers.sku_offer5]),
            MarketSku(title="blue offer sku6", hyperid=106, sku=6, blue_offers=[_Offers.sku_offer6]),
            MarketSku(title="blue offer sku7", hyperid=107, sku=7, blue_offers=[_Offers.sku_offer7]),
            MarketSku(title="blue offer sku8", hyperid=108, sku=8, blue_offers=[_Offers.sku_offer8]),
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
                    YamarecSettingPartition(
                        params={
                            'version': 'MODEL/MSKUv1',
                            'use-external': '1',
                            'use-local': '0',
                        },
                        splits=[{'split': 'model_with_msku'}],
                    ),
                    YamarecSettingPartition(
                        params={
                            'version': 'v1',
                            'use-external': '1',
                            'use-local': '0',
                        },
                        splits=[{}],
                    ),
                ],
            )
        ]

        cls.recommender.on_request_accessory_models(model_id=101, item_count=1000, version='v1').respond(
            {'models': ['102', '103', '104', '105', '0', '106', '107', '108']}
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

        cls.recommender.on_request_accessory_models(model_id=107, item_count=1000, version='v1').respond({'models': []})

    def test_complementary_product_groups_blue1(self):

        main_model = 101
        _ = ['102', '103', '104', '105', '106', '107', '108']
        offer_md5s = [
            "Sku2Price1-IiLVm1Goleg",
            "Sku3Price1-IiLVm1Goleg",
            "Sku4Price1-IiLVm1Goleg",
            "Sku5Price1-IiLVm1Goleg",
            "Sku6Price1-IiLVm1Goleg",
            "Sku7Price1-IiLVm1Goleg",
            "Sku8Price1-IiLVm1Goleg",
        ]

        for split in range(1, 2):
            query = "place=complementary_product_groups&hyperid={id}&yandexuid=001&pp=143&rgb=blue&rearr-factors=split={split}".format(
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
                                'total': 0,
                                'id': NotEmpty(),
                                'results': [],  # < 7 items, everything should be merged into complementary block
                            },
                            {
                                'entity': 'group',
                                'total': 7,
                                'id': NotEmpty(),
                                'results': [
                                    create_model_with_default_offer(model_id=106, waremd5=offer_md5s[4], price=1),
                                    create_model_with_default_offer(model_id=102, waremd5=offer_md5s[0], price=1),
                                    create_model_with_default_offer(model_id=103, waremd5=offer_md5s[1], price=1),
                                    create_model_with_default_offer(model_id=107, waremd5=offer_md5s[5], price=1),
                                    create_model_with_default_offer(model_id=104, waremd5=offer_md5s[2], price=1),
                                    create_model_with_default_offer(model_id=108, waremd5=offer_md5s[6], price=1),
                                    create_model_with_default_offer(model_id=105, waremd5=offer_md5s[3], price=1),
                                ],
                            },
                        ],
                    }
                },
            )

    def test_complementary_product_groups_blue2(self):

        main_model = 102
        models = [103, 104, 105]
        offer_md5s = ["Sku3Price1-IiLVm1Goleg", "Sku4Price1-IiLVm1Goleg", "Sku5Price1-IiLVm1Goleg"]

        for split in range(1, 2):
            query = "place=complementary_product_groups&hyperid={id}&yandexuid=001&pp=143&rgb=blue&rearr-factors=split=normal".format(
                id=main_model
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
                                    create_model_with_default_offer(models[0], waremd5=offer_md5s[0], price=1),
                                    create_model_with_default_offer(models[1], waremd5=offer_md5s[1], price=1),
                                    create_model_with_default_offer(models[2], waremd5=offer_md5s[2], price=1),
                                ],
                            },
                            {'entity': 'group', 'total': 0, 'id': NotEmpty(), 'results': []},
                        ],
                    }
                },
            )

    def test_complementary_product_groups_blue3(self):

        main_model = 103
        _ = []
        offer_md5s = ["Sku4Price1-IiLVm1Goleg", "Sku5Price1-IiLVm1Goleg"]
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

    def test_complementary_product_groups_blue4(self):

        main_model = 104
        models = [103, 105]
        offer_md5s = ["Sku3Price1-IiLVm1Goleg", "Sku5Price1-IiLVm1Goleg"]

        for split in range(1, 2):
            query = "place=complementary_product_groups&hyperid={id}&yandexuid=001&pp=143&rgb=blue&rearr-factors=split={split}".format(
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
                                    create_model_with_default_offer(models[0], waremd5=offer_md5s[0], price=1),
                                    create_model_with_default_offer(models[1], waremd5=offer_md5s[1], price=1),
                                ],
                            },
                        ],
                    }
                },
            )

    def test_complementary_product_groups_blue5(self):

        main_model = 105
        _ = []

        for split in range(1, 2):
            query = "place=complementary_product_groups&hyperid={id}&yandexuid=001&pp=143&rgb=blue&rearr-factors=split={split}".format(
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

    def test_complementary_product_groups_blue6(self):

        main_model = 106
        models = [107, 108]
        offer_md5s = ["Sku7Price1-IiLVm1Goleg", "Sku8Price1-IiLVm1Goleg"]

        for split in range(1, 2):
            query = "place=complementary_product_groups&hyperid={id}&yandexuid=001&pp=143&rgb=blue&rearr-factors=split={split}".format(
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
                                    create_model_with_default_offer(models[0], waremd5=offer_md5s[0], price=1),
                                    create_model_with_default_offer(models[1], waremd5=offer_md5s[1], price=1),
                                ],
                            },
                        ],
                    }
                },
            )

    def test_complementary_product_groups_blue7(self):

        main_model = 107
        _ = []
        for split in range(1, 2):
            query = "place=complementary_product_groups&hyperid={id}&yandexuid=001&pp=143&rgb=blue&rearr-factors=split={split}".format(
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

    def test_complementary_product_groups_blue_min_count(self):

        main_model = 101
        models = [102, 103, 104, 105, 106, 107, 108]
        offer_md5s = [
            "Sku2Price1-IiLVm1Goleg",
            "Sku3Price1-IiLVm1Goleg",
            "Sku4Price1-IiLVm1Goleg",
            "Sku5Price1-IiLVm1Goleg",
            "Sku6Price1-IiLVm1Goleg",
            "Sku7Price1-IiLVm1Goleg",
            "Sku8Price1-IiLVm1Goleg",
        ]

        for split in range(1, 2):
            query = "place=complementary_product_groups&hyperid={id}&yandexuid=001&pp=143&rgb=blue&min-count-to-show=4&rearr-factors=split={split}&new-picture-format=1".format(
                id=main_model, split=split
            )
            response = self.report.request_json(query)
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        'total': 4,
                        'knownThumbnails': NotEmpty(),
                        'results': [
                            {
                                'entity': 'group',
                                'total': 4,
                                'id': NotEmpty(),
                                'knownThumbnails': Absent(),
                                'results': [
                                    create_model_with_default_offer(models[0], waremd5=offer_md5s[0], price=1),
                                    create_model_with_default_offer(models[1], waremd5=offer_md5s[1], price=1),
                                    create_model_with_default_offer(models[2], waremd5=offer_md5s[2], price=1),
                                    create_model_with_default_offer(models[3], waremd5=offer_md5s[3], price=1),
                                ],
                            },
                            {
                                'entity': 'group',
                                'total': 0,
                                'id': NotEmpty(),
                                'knownThumbnails': Absent(),
                                'results': [],
                            },
                        ],
                    }
                },
            )

    @classmethod
    def prepare_complementary_product_groups_blue_with_msku(cls):
        cls.index.models += [
            Model(hyperid=1001, hid=101, accessories=[1002, 1003]),
            Model(hyperid=1002, hid=101),
            Model(hyperid=1003, hid=101),
        ]

        cls.index.mskus += [
            MarketSku(title="blue offer sku10010", hyperid=1001, sku=10010, blue_offers=[BlueOffer()]),
            MarketSku(title="blue offer sku10020", hyperid=1002, sku=10020, blue_offers=[BlueOffer()]),
            MarketSku(title="blue offer sku10021", hyperid=1002, sku=10021, blue_offers=[BlueOffer()]),
            MarketSku(title="blue offer sku10030", hyperid=1003, sku=10030, blue_offers=[BlueOffer()]),
            MarketSku(title="blue offer sku10031", hyperid=1003, sku=10031, blue_offers=[BlueOffer()]),
        ]

        cls.recommender.on_request_accessory_models_with_msku(
            model_id=1001, item_count=1000, version='MODEL/MSKUv1'
        ).respond({'models': ['1002/10021', '0', '1003/10031']})

    def test_complementary_product_groups_blue_with_msku(self):

        main_model = 1001
        query = "place=complementary_product_groups&hyperid={id}&yandexuid=001&pp=143&rgb=blue&min-count-to-show=1&new-picture-format=1&disable-accessories-merge=1&rearr-factors=split=model_with_msku".format(  # noqa
            id=main_model
        )
        response = self.report.request_json(query)
        self.assertFragmentIn(
            response,
            {
                "search": {
                    'total': 2,
                    'knownThumbnails': NotEmpty(),
                    'results': [
                        {
                            'entity': 'group',
                            'total': 1,
                            'id': NotEmpty(),
                            'knownThumbnails': Absent(),
                            'results': [
                                {'offers': {'items': [{'marketSku': "10021"}]}},
                            ],
                        },
                        {
                            'entity': 'group',
                            'total': 1,
                            'id': NotEmpty(),
                            'knownThumbnails': Absent(),
                            'results': [
                                {'offers': {'items': [{'marketSku': "10031"}]}},
                            ],
                        },
                    ],
                }
            },
        )


if __name__ == '__main__':
    main()

#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from unittest import skip

from core.types import (
    Picture,
    HyperCategory,
    HyperCategoryType,
    NavCategory,
    BoosterConfigRecord,
    BoosterConfigFactor,
    WarehouseWithPriority,
    WarehousesPriorityInRegion,
    ExpressSupplier,
)
from core.types import GLParam, GLType, GLValue, MnPlace, Offer, Shop
from core.types import Model
from core.testcase import TestCase, main
from core.bigb import BigBKeyword, WeightedValue
from core.matcher import NoKey, Round

from market.proto.recom.exported_dj_user_profile_pb2 import (
    TEcomVersionedDjUserProfile,
    TVersionedProfileData,
    TBrandsInDepartmentDataV1,
    TBrandsDataV1,
    TFashionDataV1,
    TFashionSizeDataV1,
)  # noqa pylint: disable=import-error

Vendor1 = 1001
Vendor2 = 1002

Business1 = 2001
Business2 = 2002

Shop1 = 3001
Shop2 = 3002
Shop3 = 3003
Shop4 = 3004

Warehouse1 = 10
FeedId1 = 1

Pictures = [Picture() for i in range(4)]

CLOTHES_SIZE_PARAM_ID = 26417130
CLOTHES_SIZE_PARAM_VALUES = [
    (27016810, 'XS'),
    (27016830, 'S'),
    (27016891, 'M'),
    (27016892, 'L'),
    (27016910, 'XL'),
]

KIDS_CLOTHES_SIZE_PARAM_ID = 28646118
KIDS_CLOTHES_SIZE_PARAM_VALUES = [
    (28765873, '0 мес'),
    (28765872, '3-6 мес'),
    (28765876, '8 лет'),
    (28765886, '14 лет'),
    (28765877, '16 лет'),
]

PERFUME_GENDER_PARAM_ID = 15927831
PERFUME_GENDER_PARAM_VALUES = [
    (15927836, 'женский'),
    (15927847, 'мужской'),
    (15927853, 'унисекс'),
]

PERFUME_BRAND_VALUES_MEN = [
    (4484001, 'Hugo Boss'),
    (4484002, 'ARMANI'),
    (4484003, 'Paco Rabanne'),
]

PERFUME_BRAND_VALUES_WOMEN = [
    (4484001, 'Chanel'),
    (4484002, 'Givenchy'),
    (4484003, 'Yves Saint Laurent'),
]

PERFUME_BRAND_VALUES_UNISEX = [
    (4484001, 'Calvin Klein'),
    (4484002, 'Hermes'),
    (4484003, 'Escentric Molecules'),
]

FASHION_BRAND_VALUES = [
    (4450901, 'NIKE'),
    (4450902, 'FiNN FLARE'),
    (4450903, 'LACOSTE'),
    (4450904, 'Tom Tailor'),
    (4450905, 'ТВОЕ'),
]

KIDS_GENDER_PARAM_ID = 12401459
KIDS_GENDER_PARAM_VALUES = [
    (12401470, 'для мальчика'),
    (12401464, 'для девочки'),
]
KIDS_POL_PARAM_ID = 14805991
KIDS_POL_PARAM_VALUES = [
    (28577760, 'для девочек'),
    (28577774, 'для девочек, для мальчиков'),
    (28577821, 'для мальчиков'),
]

FASHION_SEASON_PARAM_ID = 27142893
FASHION_SEASON_PARAM_VALUES = [
    (28575668, 'всесезон'),
    (28575663, 'зима'),
    (28575659, 'лето'),
    (32034092, 'демисезон/лето'),
    (32034070, 'демисезон/зима'),
]

WOMAN_PROFILE = [
    BigBKeyword(
        id=BigBKeyword.GENDER,
        weighted_uint_values=[
            WeightedValue(value=BigBKeyword.GENDER_MALE, weight=21947),
            WeightedValue(value=BigBKeyword.GENDER_FEMALE, weight=975515),
        ],
    ),
]

MAN_PROFILE = [
    BigBKeyword(
        id=BigBKeyword.GENDER,
        weighted_uint_values=[
            WeightedValue(value=BigBKeyword.GENDER_MALE, weight=921947),
            WeightedValue(value=BigBKeyword.GENDER_FEMALE, weight=75515),
        ],
    ),
]

UNKNOWN_PROFILE = [
    BigBKeyword(
        id=BigBKeyword.GENDER,
        weighted_uint_values=[
            WeightedValue(value=BigBKeyword.GENDER_MALE, weight=521947),
            WeightedValue(value=BigBKeyword.GENDER_FEMALE, weight=575515),
        ],
    ),
]


def make_response(expected_result, boosted_docs, boost_name, boost_coef=1.5, noise=0):
    length = len(expected_result)
    return {
        "search": {
            "total": length,
            "totalOffers": length,
            "results": [
                {
                    'entity': 'offer',
                    'titles': {'raw': expected_result[i]},
                    'debug': {
                        'properties': {
                            'BOOSTER_LOG': '[{name:'
                            + boost_name
                            + '; base_coef:'
                            + str(boost_coef)
                            + '; noise:'
                            + str(noise)
                            + '; prob:1; resulting_coef:'
                            + str(boost_coef + noise)
                            + '}]'
                            if boosted_docs[i]
                            else '[]',
                        }
                    },
                }
                for i in range(length)
            ],
        },
    }


def make_single_test(test, request, expected_result, boosted_docs, boost_name, boost_coef, noise):
    response = test.report.request_json(request)
    test.assertFragmentIn(
        response,
        make_response(expected_result, boosted_docs, boost_name, boost_coef, noise),
        preserve_order=True,
        allow_different_len=False,
    )


def make_testing(
    test,
    expected_nums,
    boosted_docs,
    boost_name,
    text=False,
    textless=False,
    query='',
    hid=0,
    boost_coef=1.5,
    noise=0,
    additional_rearr='',
):
    expected_result = ['{} {}'.format(query, num) for num in expected_nums]
    base_request = (
        'place=prime&allow-collapsing=0&entities=offer&debug=da&rearr-factors=market_enable_new_booster=1'
        + additional_rearr
    )

    if text:
        request = base_request + '&text={}'.format(query)
        make_single_test(test, request, expected_result, boosted_docs, boost_name, boost_coef, noise)

    if textless:
        request = base_request + '&hid={}'.format(hid)
        make_single_test(test, request, expected_result, boosted_docs, boost_name, boost_coef, noise)


class T(TestCase):
    @classmethod
    def prepare(cls):
        # Numeration rules:
        # - hid = {100, 199}
        # - nid = {300, 399}
        # - hyperid = {500, 599}
        # - glparam = {10, 99}
        # - glvalue = {1, 9}

        cls.settings.disable_random = True

        # common for most tests
        cls.index.hypertree += [
            HyperCategory(
                hid=100,
                children=[
                    # brand boost test
                    HyperCategory(hid=101, output_type=HyperCategoryType.GURU),
                    # business boost test
                    HyperCategory(hid=102, output_type=HyperCategoryType.GURU),
                    # shop boost test
                    HyperCategory(hid=103, output_type=HyperCategoryType.GURU),
                    # gl-filter boost test
                    HyperCategory(hid=104, output_type=HyperCategoryType.GURU),
                    # random boost test
                    HyperCategory(hid=105, output_type=HyperCategoryType.GURU),
                    # normalized coefs test
                    HyperCategory(hid=106, output_type=HyperCategoryType.GURU),
                    # no coefs test
                    HyperCategory(hid=107, output_type=HyperCategoryType.GURU),
                    # express boost test
                    HyperCategory(hid=108, output_type=HyperCategoryType.GURU),
                ],
            ),
        ]

        # gl-filter boost test
        cls.index.gltypes += [
            GLType(param_id=10, hid=104, gltype=GLType.ENUM),
            GLType(param_id=11, hid=104, gltype=GLType.BOOL),
            GLType(param_id=12, hid=104, gltype=GLType.NUMERIC),
        ]

        # shop boost test
        cls.index.shops += [
            Shop(fesh=Shop1),
            Shop(fesh=Shop2),
            Shop(fesh=Shop3, with_express_warehouse=True, datafeed_id=FeedId1),
            Shop(fesh=Shop4),
        ]

        # allowed hids test
        cls.index.hypertree += [
            HyperCategory(
                hid=150,
                children=[
                    HyperCategory(hid=151, output_type=HyperCategoryType.GURU),
                ],
            ),
            HyperCategory(hid=152, output_type=HyperCategoryType.GURU),
            HyperCategory(hid=112, output_type=HyperCategoryType.GURU),
        ]

        # allowed nids test
        cls.index.navtree += [
            NavCategory(nid=300, hid=111),
            NavCategory(
                nid=301,
                hid=112,
                children=[
                    NavCategory(nid=302, children=[NavCategory(nid=303, hid=112)]),
                ],
            ),
        ]

        # OFFERS
        # brand boost test
        cls.index.offers += [
            Offer(
                hid=101,
                vendor_id=Vendor1 if i < 2 else Vendor2,
                picture=Pictures[i],
                title='колонка ' + str(i),
                randx=i,
            )
            for i in range(4)
        ]

        # business boost test
        cls.index.offers += [
            Offer(
                hid=102,
                business_id=Business1 if i < 2 else Business2,
                picture=Pictures[i],
                title='телевизор ' + str(i),
                randx=i,
            )
            for i in range(4)
        ]

        # shop boost test
        cls.index.offers += [
            Offer(hid=103, fesh=Shop1 if i < 2 else Shop2, picture=Pictures[i], title='компьютер ' + str(i), randx=i)
            for i in range(4)
        ]

        # gl-filter boost test
        cls.index.offers += [
            Offer(
                hid=104,
                picture=Pictures[i],
                title='мультиварка ' + str(i),
                randx=i,
                glparams=[
                    GLParam(param_id=10 + i, value=i),
                ],
            )
            for i in range(3)
        ]

        # random boost test
        cls.index.offers += [
            Offer(
                hid=105,
                picture=Pictures[i],
                title='случайка ' + str(i),
                randx=i,
            )
            for i in range(2)
        ]

        # normalized coefs test
        cls.index.offers += [
            Offer(
                hid=106,
                vendor_id=Vendor1 if i < 2 else Vendor2,
                picture=Pictures[i],
                title='нормалька ' + str(i),
                randx=i,
            )
            for i in range(4)
        ]

        # no coef test
        cls.index.offers += [
            Offer(
                hid=107,
                vendor_id=Vendor1 if i < 2 else Vendor2,
                picture=Pictures[i],
                title='пустышка ' + str(i),
                randx=i,
            )
            for i in range(4)
        ]

        # express boost test
        cls.index.offers += [
            Offer(hid=108, fesh=Shop3 if i < 2 else Shop4, picture=Pictures[i], title='экспрессор ' + str(i), randx=i)
            for i in range(4)
        ]

        # nid without hid test
        cls.index.offers += [
            Offer(
                hid=112,
                navigation_node_ids=[302],
                vendor_id=Vendor1 if i < 2 else Vendor2,
                picture=Pictures[i],
                title='нидка ' + str(i),
                randx=i,
            )
            for i in range(4)
        ]

        cls.bigb.on_request(yandexuid=26471001, client='merch-machine').respond()
        cls.bigb.on_request(yandexuid=26471002, client='merch-machine').respond()

        cls.index.warehouse_priorities += [
            WarehousesPriorityInRegion(
                regions=[225, 213],
                warehouse_with_priority=[
                    WarehouseWithPriority(warehouse_id=Warehouse1, priority=1),
                ],
            ),
        ]

        cls.index.express_partners.suppliers += [
            ExpressSupplier(
                feed_id=FeedId1,
                supplier_id=Shop3,
                warehouse_id=Warehouse1,
            ),
        ]

        # BOOSTER CONFIGS
        cls.index.booster_config_records += [
            BoosterConfigRecord(
                name='brand_boost',
                type_name='brand_boost',
                args={'@type': 'type.googleapis.com/MarketSearch.Booster.TBrandBoostArgs', 'brands': [Vendor1]},
                base_coeffs={
                    'text': 1.5,
                    'textless': 1.5,
                },
                hids=[101],
            )
        ]

        cls.index.booster_config_records += [
            BoosterConfigRecord(
                name='business_boost',
                type_name='business_boost',
                args={
                    '@type': 'type.googleapis.com/MarketSearch.Booster.TBusinessBoostArgs',
                    'businesses': [Business1],
                },
                base_coeffs={
                    'text': 1.5,
                    'textless': 1.5,
                },
                hids=[102],
            )
        ]

        cls.index.booster_config_records += [
            BoosterConfigRecord(
                name='shop_boost',
                type_name='shop_boost',
                args={'@type': 'type.googleapis.com/MarketSearch.Booster.TShopBoostArgs', 'shops': [Shop1]},
                base_coeffs={
                    'text': 1.5,
                    'textless': 1.5,
                },
                hids=[103],
            )
        ]

        cls.index.booster_config_records += [
            BoosterConfigRecord(
                name='gl_filter_boost',
                type_name='gl_filter_boost',
                args={
                    '@type': 'type.googleapis.com/MarketSearch.Booster.TGlFiltersBoostArgs',
                    'gl_filters': [
                        {
                            'param_id': 10,
                            'value': '0',
                        },
                        {
                            'param_id': 11,
                            'value': '1',
                        },
                        {
                            'param_id': 12,
                            'value': '1~4',
                        },
                    ],
                },
                base_coeffs={
                    'text': 1.5,
                    'textless': 1.5,
                },
                hids=[104],
            )
        ]

        cls.index.booster_config_records += [
            BoosterConfigRecord(
                name='random_doc_boost',
                type_name='random_boost',
                args={
                    '@type': 'type.googleapis.com/MarketSearch.Booster.TRandomBoostArgs',
                    'random_seed_keys': ['yandexuid'],
                },
                base_coeffs={
                    'text': 1.5,
                    'textless': 1.5,
                },
                hids=[105],
            )
        ]

        cls.index.booster_config_records += [
            BoosterConfigRecord(
                name='brand_boost_normalize_1',
                type_name='brand_boost',
                args={'@type': 'type.googleapis.com/MarketSearch.Booster.TBrandBoostArgs', 'brands': [Vendor1]},
                base_coeffs={
                    'text': 1.1,
                    'textless': 1.1,
                },
                hids=[106],
            )
        ]

        cls.index.booster_config_records += [
            BoosterConfigRecord(
                name='brand_boost_normalize_2',
                type_name='brand_boost',
                args={'@type': 'type.googleapis.com/MarketSearch.Booster.TBrandBoostArgs', 'brands': [Vendor1]},
                base_coeffs={
                    'text': 1.1,
                    'textless': 1.1,
                },
                hids=[106],
            )
        ]

        cls.index.booster_config_records += [
            BoosterConfigRecord(
                name='no_boost',
                type_name='brand_boost',
                args={'@type': 'type.googleapis.com/MarketSearch.Booster.TBrandBoostArgs', 'brands': [Vendor1]},
                base_coeffs={},
                hids=[107],
            )
        ]

        cls.index.booster_config_records += [
            BoosterConfigRecord(
                name='express_boost',
                type_name='express_boost',
                args={
                    '@type': 'type.googleapis.com/MarketSearch.Booster.TNoArgs',
                },
                base_coeffs={
                    'text': 1.5,
                    'textless': 1.5,
                },
                hids=[108],
            )
        ]

        cls.index.booster_config_records += [
            BoosterConfigRecord(
                name='nid_without_hid_boost',
                type_name='brand_boost',
                args={'@type': 'type.googleapis.com/MarketSearch.Booster.TBrandBoostArgs', 'brands': [Vendor1]},
                base_coeffs={
                    'text': 1.5,
                    'textless': 1.5,
                },
                request_hids=[112],
                hids=[112],
            )
        ]

    def test_boost_brands(self):
        expected_nums = [1, 0, 3, 2]
        boosted_docs = [1, 1, 0, 0]
        make_testing(
            self,
            expected_nums=expected_nums,
            boosted_docs=boosted_docs,
            text=True,
            textless=True,
            query='колонка',
            hid=101,
            boost_name='brand_boost',
        )

    def test_normalized_coefs(self):
        expected = {
            "search": {
                "results": [
                    {'debug': {'properties': {'BOOST_MULTIPLIER': '1.183495164'}}},
                    {'debug': {'properties': {'BOOST_MULTIPLIER': '1.183495164'}}},
                    {'debug': {'properties': {'BOOST_MULTIPLIER': '1'}}},
                    {'debug': {'properties': {'BOOST_MULTIPLIER': '1'}}},
                ],
            },
        }

        response = self.report.request_json(
            'hid=106&place=prime&allow-collapsing=0&entities=offer&debug=da&rearr-factors=market_enable_new_booster=1;market_boost_normalizing_coefs=1.3,0.3,1.3,0.3'
        )
        self.assertFragmentIn(
            response,
            expected,
            preserve_order=True,
            allow_different_len=False,
        )

    def test_modified_coefs(self):
        expected_nums = [1, 0, 3, 2]
        boosted_docs = [1, 1, 0, 0]
        make_testing(
            self,
            expected_nums=expected_nums,
            boosted_docs=boosted_docs,
            text=True,
            textless=False,
            query='колонка',
            hid=101,
            boost_coef=2,
            additional_rearr=';market_modify_boosts_text=brand_boost:2.0;market_modify_boosts_text=business_boost:2.0',
            boost_name='brand_boost',
        )
        make_testing(
            self,
            expected_nums=expected_nums,
            boosted_docs=boosted_docs,
            text=False,
            textless=True,
            query='колонка',
            hid=101,
            boost_coef=2,
            additional_rearr=';market_modify_boosts_textless=brand_boost:2.0;market_modify_boosts_textless=business_boost:2.0',
            boost_name='brand_boost',
        )
        make_testing(
            self,
            expected_nums=expected_nums,
            boosted_docs=boosted_docs,
            text=True,
            textless=False,
            query='телевизор',
            hid=102,
            boost_coef=2,
            additional_rearr=';market_modify_boosts_text=brand_boost:2.0;market_modify_boosts_text=business_boost:2.0',
            boost_name='business_boost',
        )
        make_testing(
            self,
            expected_nums=expected_nums,
            boosted_docs=boosted_docs,
            text=False,
            textless=True,
            query='телевизор',
            hid=102,
            boost_coef=2,
            additional_rearr=';market_modify_boosts_textless=brand_boost:2.0;market_modify_boosts_textless=business_boost:2.0',
            boost_name='business_boost',
        )

    def test_noise(self):
        expected_nums = [1, 0, 3, 2]
        boosted_docs = [1, 1, 0, 0]
        make_testing(
            self,
            expected_nums=expected_nums,
            boosted_docs=boosted_docs,
            text=True,
            textless=False,
            query='колонка',
            hid=101,
            boost_coef=2,
            noise=0.1,
            additional_rearr=';market_modify_boosts_text=brand_boost:2.0:0.1;market_modify_boosts_text=business_boost:2.0:0.1',
            boost_name='brand_boost',
        )

    def test_feature_log(self):
        expected_nums = [1, 0, 3, 2]
        boosted_docs = [1, 1, 0, 0]
        make_testing(
            self,
            expected_nums=expected_nums,
            boosted_docs=boosted_docs,
            text=True,
            textless=True,
            query='колонка',
            hid=101,
            boost_name='brand_boost',
        )
        self.feature_log.expect(
            booster_log='[{name:brand_boost; base_coef:1.5; noise:0; prob:1; resulting_coef:1.5}]'
        ).times(4)
        self.feature_log.expect(booster_log='[]').times(4)

    def test_access_log(self):
        expected_nums = [1, 0, 3, 2]
        boosted_docs = [1, 1, 0, 0]
        make_testing(
            self,
            expected_nums=expected_nums,
            boosted_docs=boosted_docs,
            text=True,
            textless=True,
            query='колонка',
            hid=101,
            boost_name='brand_boost',
        )
        self.access_log.expect(booster_request_log='brand_boost:0.6,').times(2)

    def test_boost_businesses(self):
        expected_nums = [1, 0, 3, 2]
        boosted_docs = [1, 1, 0, 0]
        make_testing(
            self,
            expected_nums=expected_nums,
            boosted_docs=boosted_docs,
            text=True,
            textless=True,
            query='телевизор',
            hid=102,
            boost_name='business_boost',
        )

    def test_boost_shops(self):
        expected_nums = [1, 0, 3, 2]
        boosted_docs = [1, 1, 0, 0]
        make_testing(
            self,
            expected_nums=expected_nums,
            boosted_docs=boosted_docs,
            text=True,
            textless=True,
            query='компьютер',
            hid=103,
            boost_name='shop_boost',
        )

    def test_boost_gl_filters(self):
        expected_nums = [2, 1, 0]
        boosted_docs = [1, 1, 1]
        make_testing(
            self,
            expected_nums=expected_nums,
            boosted_docs=boosted_docs,
            text=True,
            textless=True,
            query='мультиварка',
            hid=104,
            boost_name='gl_filter_boost',
        )

    def test_random(self):
        expected = {
            "search": {
                "total": 2,
                "totalOffers": 2,
                "results": [
                    {
                        'entity': 'offer',
                        'debug': {
                            'properties': {
                                'BOOSTER_LOG': '[{name:random_doc_boost; base_coef:1.5; noise:0; prob:0.6508132408; resulting_coef:1.32540662}]',
                            }
                        },
                    },
                    {
                        'entity': 'offer',
                        'debug': {
                            'properties': {
                                'BOOSTER_LOG': '[{name:random_doc_boost; base_coef:1.5; noise:0; prob:0.352469528; resulting_coef:1.176234764}]',
                            }
                        },
                    },
                ],
            },
        }

        response = self.report.request_json(
            'hid=105&yandexuid=26471001&place=prime&allow-collapsing=0&entities=offer&debug=da&rearr-factors=market_enable_new_booster=1'
        )
        self.assertFragmentIn(
            response,
            expected,
            preserve_order=True,
            allow_different_len=False,
        )

        response = self.report.request_json(
            'hid=105&yandexuid=26471002&place=prime&allow-collapsing=0&entities=offer&debug=da&rearr-factors=market_enable_new_booster=1'
        )
        self.assertFragmentNotIn(
            response,
            expected,
            preserve_order=True,
        )

    def test_no_coefs(self):
        expected_nums = [3, 2, 1, 0]
        boosted_docs = [0, 0, 0, 0]
        make_testing(
            self,
            expected_nums=expected_nums,
            boosted_docs=boosted_docs,
            text=True,
            textless=True,
            query='пустышка',
            hid=107,
            boost_name='brand_boost',
        )
        make_testing(
            self,
            expected_nums=expected_nums,
            boosted_docs=boosted_docs,
            text=True,
            textless=True,
            query='колонка',
            hid=101,
            boost_name='brand_boost',
            additional_rearr=';market_modify_boosts_text=brand_boost:-1.0;market_modify_boosts_textless=brand_boost:-1.0',
        )

    def test_boost_express(self):
        expected_nums = [1, 0, 3, 2]
        boosted_docs = [1, 1, 0, 0]
        make_testing(
            self,
            expected_nums=expected_nums,
            boosted_docs=boosted_docs,
            text=True,
            textless=True,
            query='экспрессор',
            hid=108,
            boost_name='express_boost',
        )

    @classmethod
    def prepare_boost_popular_gl_params(cls):
        cls.index.hypertree += [
            HyperCategory(
                hid=7877999,
                name='Одежда, обувь и аксессуары',
                children=[
                    HyperCategory(
                        hid=7811877,
                        name='Мужская одежда',
                        children=[
                            HyperCategory(
                                hid=7812139,
                                name='Верхняя мужская одежда',
                                children=[
                                    HyperCategory(hid=7812186, name='Мужские куртки'),
                                    HyperCategory(hid=7812188, name='Мужские пальто'),
                                ],
                            ),
                            HyperCategory(hid=7812157, name='Мужские футболки'),
                        ],
                    ),
                    HyperCategory(
                        hid=7811873,
                        name='Женская одежда',
                        children=[
                            HyperCategory(hid=7811901, name='Женские платья'),
                            HyperCategory(hid=7811908, name='Женские толстовки'),
                        ],
                    ),
                    HyperCategory(
                        hid=7811879,
                        name='Детская одежда',
                        children=[
                            HyperCategory(
                                hid=7812006,
                                name='Для девочек',
                                children=[
                                    HyperCategory(hid=7812065, name='Рубашки'),
                                ],
                            ),
                            HyperCategory(
                                hid=7812009,
                                name='Для мальчиков',
                                children=[
                                    HyperCategory(hid=7812106, name='Джинсы'),
                                ],
                            ),
                            HyperCategory(
                                hid=7812011,
                                name='Для малышей',
                                children=[
                                    HyperCategory(hid=7812048, name='Комбинезоны'),
                                ],
                            ),
                        ],
                    ),
                ],
            ),
        ]

        for hid in [7812186, 7812139, 7811901, 7811877, 7811873, 7812188]:
            cls.index.gltypes += [
                GLType(
                    param_id=CLOTHES_SIZE_PARAM_ID,
                    hid=hid,
                    gltype=GLType.ENUM,
                    xslname='size_clothes_new',
                    cluster_filter=True,
                    values=[
                        GLValue(value_id=value_id, text=param_name)
                        for (value_id, param_name) in CLOTHES_SIZE_PARAM_VALUES
                    ],
                ),
            ]

        for seq, (value_id, param_name) in enumerate(CLOTHES_SIZE_PARAM_VALUES):
            cls.index.offers += [
                Offer(
                    hid=7812186,
                    picture=Pictures[0],
                    title='Куртка мужская Adidas размер ' + param_name,
                    glparams=[
                        GLParam(param_id=CLOTHES_SIZE_PARAM_ID, value=value_id),
                    ],
                    ts=4423700 + seq,
                ),
                Offer(
                    hid=7811901,
                    picture=Pictures[0],
                    title='Платье женское Baon размер ' + param_name,
                    glparams=[
                        GLParam(param_id=CLOTHES_SIZE_PARAM_ID, value=value_id),
                    ],
                    ts=4423750 + seq,
                ),
            ]

        for seq in range(0, 50):
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 4423750 + seq).respond(60.0 - seq * 0.01)
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 4423700 + seq).respond(60.0 - seq * 0.01)

        cls.index.booster_config_records += [
            BoosterConfigRecord(
                name='boost_popular',
                type_name='first_factor_boost',
                args={
                    '@type': 'type.googleapis.com/MarketSearch.Booster.TFactorBoostArgs',
                    'factors': [
                        {
                            'factor_name': 'POPULAR_SIZE_PROBABILITY',
                        }
                    ],
                },
                base_coeffs={
                    'text': 1.2,
                    'textless': 1.2,
                },
                hids=[7877999],
            ),
            BoosterConfigRecord(
                name='boost_sigmoid_1',
                type_name='first_factor_boost',
                args={
                    '@type': 'type.googleapis.com/MarketSearch.Booster.TFactorBoostArgs',
                    'factors': [
                        {
                            'factor_name': 'USER_ECOM_SIZE_COINCIDE',
                            'category_args': [
                                {'left_boundary': 0.0, 'right_boundary': 1.0, 'factor_multiplier': 1.0},
                                {'exact_value': 0.01, 'probability': 0.0, 'factor_multiplier': 0.0},
                            ],
                        },
                        {
                            'factor_name': 'POPULAR_SIZE_PROBABILITY',
                        },
                    ],
                    'multi_args': {'avg': 0.35, 'slope': 10.0, 'min_probability': 0.0001},
                },
                base_coeffs={
                    'text': 1.2,
                    'textless': 1.2,
                },
                hids=[7877999],
            ),
            BoosterConfigRecord(
                name='boost_sigmoid_2',
                type_name='first_factor_boost',
                args={
                    '@type': 'type.googleapis.com/MarketSearch.Booster.TFactorBoostArgs',
                    'factors': [
                        {
                            'factor_name': 'USER_ECOM_SIZE_COINCIDE',
                            'category_args': [
                                {'left_boundary': 0.0, 'right_boundary': 1.0, 'factor_multiplier': 1.0},
                                {'exact_value': 0.01, 'probability': 0.0, 'factor_multiplier': 0.0},
                            ],
                        },
                        {
                            'factor_name': 'POPULAR_SIZE_PROBABILITY',
                        },
                    ],
                    'multi_args': {'avg': 0.175, 'slope': 10.0, 'min_probability': 0.0001},
                },
                base_coeffs={
                    'text': 1.2,
                    'textless': 1.2,
                },
                hids=[7877999],
            ),
        ]

    def test_boost_popular_gl_params(self):
        rearr_factors = [
            'market_boost_popular_gl_params_coeff=1.2',
            'market_disable_boosts=boost_sigmoid_1,boost_sigmoid_2,boost_personal',
            'market_enable_new_booster=1',
        ]
        request_base = "place=prime"
        for req in ['&hid=7812186', '&text=куртка']:
            response = self.report.request_json(request_base + req)
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "results": [
                            {'entity': 'offer', 'titles': {'raw': 'Куртка мужская Adidas размер M'}},
                            {'entity': 'offer', 'titles': {'raw': 'Куртка мужская Adidas размер L'}},
                            {'entity': 'offer', 'titles': {'raw': 'Куртка мужская Adidas размер XL'}},
                            {'entity': 'offer', 'titles': {'raw': 'Куртка мужская Adidas размер XS'}},
                            {'entity': 'offer', 'titles': {'raw': 'Куртка мужская Adidas размер S'}},
                        ]
                    }
                },
                preserve_order=True,
                allow_different_len=False,
            )

            # под реарр-флагом
            response = self.report.request_json(
                request_base + req + '&rearr-factors={}'.format(';'.join(rearr_factors))
            )
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "results": [
                            {'entity': 'offer', 'titles': {'raw': 'Куртка мужская Adidas размер M'}},
                            {'entity': 'offer', 'titles': {'raw': 'Куртка мужская Adidas размер L'}},
                            {'entity': 'offer', 'titles': {'raw': 'Куртка мужская Adidas размер XL'}},
                            {'entity': 'offer', 'titles': {'raw': 'Куртка мужская Adidas размер XS'}},
                            {'entity': 'offer', 'titles': {'raw': 'Куртка мужская Adidas размер S'}},
                        ]
                    }
                },
                preserve_order=True,
                allow_different_len=False,
            )

        for req in ['&hid=7811901', '&text=платье']:
            response = self.report.request_json(request_base + req)
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "results": [
                            {'entity': 'offer', 'titles': {'raw': 'Платье женское Baon размер S'}},
                            {'entity': 'offer', 'titles': {'raw': 'Платье женское Baon размер M'}},
                            {'entity': 'offer', 'titles': {'raw': 'Платье женское Baon размер L'}},
                            {'entity': 'offer', 'titles': {'raw': 'Платье женское Baon размер XS'}},
                            {'entity': 'offer', 'titles': {'raw': 'Платье женское Baon размер XL'}},
                        ]
                    }
                },
                preserve_order=True,
                allow_different_len=False,
            )

            # под реарр-флагом
            response = self.report.request_json(
                request_base + req + '&rearr-factors={}'.format(';'.join(rearr_factors))
            )
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "results": [
                            {'entity': 'offer', 'titles': {'raw': 'Платье женское Baon размер S'}},
                            {'entity': 'offer', 'titles': {'raw': 'Платье женское Baon размер M'}},
                            {'entity': 'offer', 'titles': {'raw': 'Платье женское Baon размер L'}},
                            {'entity': 'offer', 'titles': {'raw': 'Платье женское Baon размер XS'}},
                            {'entity': 'offer', 'titles': {'raw': 'Платье женское Baon размер XL'}},
                        ]
                    }
                },
                preserve_order=True,
                allow_different_len=False,
            )

    @classmethod
    def prepare_boost_personal_gl_params(cls):
        cls.settings.set_default_reqid = False

        profile = TEcomVersionedDjUserProfile(
            ProfileData=TVersionedProfileData(
                FashionV1=TFashionDataV1(
                    SizeClothesFemale=TFashionSizeDataV1(
                        Sizes={"46": 0.288602501, "48": 0.355698764, "S": 0.395698764, "M": 0.288602501}
                    ),
                    SizeClothesMale=TFashionSizeDataV1(
                        Sizes={"46": 0.188602501, "48": 0.155698764, "L": 0.155698764, "M": 0.198602501}
                    ),
                )
            )
        )

        cls.bigb.on_request(yandexuid='4423701', client='merch-machine').respond(keywords=MAN_PROFILE)
        cls.dj.on_request(yandexuid='4423701', exp='fetch_user_profile_versioned').respond(
            profile_data=profile.SerializeToString(), is_binary_data=True
        )

        cls.index.booster_config_records += [
            BoosterConfigRecord(
                name='boost_personal',
                type_name='first_factor_boost',
                args={
                    '@type': 'type.googleapis.com/MarketSearch.Booster.TFactorBoostArgs',
                    'factors': [
                        {
                            'factor_name': 'USER_ECOM_SIZE_COINCIDE',
                            'category_args': [
                                {'left_boundary': 0.0, 'right_boundary': 1.0, 'factor_multiplier': 1.0},
                                {'exact_value': 0.01, 'probability': 0.0, 'factor_multiplier': 0.0},
                            ],
                        },
                        {
                            'factor_name': 'POPULAR_SIZE_PROBABILITY',
                        },
                    ],
                    'multi_args': {
                        'slope': 1.0,
                    },
                },
                base_coeffs={
                    'text': 1.2,
                    'textless': 1.2,
                },
                hids=[7812186, 7811901],
            ),
        ]

    def test_boost_all_personal_gl_params(self):
        request_base = "place=prime&debug=da"
        for req in ['&hid=7812186', '&text=куртка']:
            response = self.report.request_json(request_base + req)
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "results": [
                            {
                                'entity': 'offer',
                                'titles': {'raw': 'Куртка мужская Adidas размер M'},
                                'debug': {'factors': {'USER_ECOM_SIZE_COINCIDE': NoKey('USER_ECOM_SIZE_COINCIDE')}},
                            },
                            {
                                'entity': 'offer',
                                'titles': {'raw': 'Куртка мужская Adidas размер L'},
                                'debug': {'factors': {'USER_ECOM_SIZE_COINCIDE': NoKey('USER_ECOM_SIZE_COINCIDE')}},
                            },
                        ]
                    }
                },
                preserve_order=True,
            )

        rearr_factors = [
            'market_boost_all_personal_gl_params_coeff=1.2',
            'market_boost_personal_gl_params_slope=1',
            'fetch_recom_profile_for_prime=1',
            'market_disable_boosts=boost_popular',
            'market_enable_new_booster=1',
        ]
        request_base = "place=prime&yandexuid=4423701&debug=da"
        for req in ['&hid=7812186', '&text=куртка']:
            response = self.report.request_json(
                request_base + req + '&rearr-factors={}'.format(';'.join(rearr_factors))
            )
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "results": [
                            {
                                'entity': 'offer',
                                'titles': {'raw': 'Куртка мужская Adidas размер M'},
                                'debug': {
                                    'factors': {
                                        'USER_ECOM_SIZE_COINCIDE': Round(0.198602501),
                                        'USER_ECOM_SIZE_COINCIDE_NORMALIZED': Round(
                                            0.198602501 / (0.198602501 + 0.155698764)
                                        ),
                                    }
                                },
                            },
                            {
                                'entity': 'offer',
                                'titles': {'raw': 'Куртка мужская Adidas размер L'},
                                'debug': {
                                    'factors': {
                                        'USER_ECOM_SIZE_COINCIDE': Round(0.155698764),
                                        'USER_ECOM_SIZE_COINCIDE_NORMALIZED': Round(
                                            0.155698764 / (0.198602501 + 0.155698764)
                                        ),
                                    }
                                },
                            },
                            {
                                'entity': 'offer',
                                'titles': {'raw': 'Куртка мужская Adidas размер XS'},
                                'debug': {
                                    'factors': {
                                        'USER_ECOM_SIZE_COINCIDE': Round(0.01),
                                        'USER_ECOM_SIZE_COINCIDE_NORMALIZED': Round(0.01),
                                    }
                                },
                            },
                            {
                                'entity': 'offer',
                                'titles': {'raw': 'Куртка мужская Adidas размер S'},
                                'debug': {
                                    'factors': {
                                        'USER_ECOM_SIZE_COINCIDE': Round(0.01),
                                        'USER_ECOM_SIZE_COINCIDE_NORMALIZED': Round(0.01),
                                    }
                                },
                            },
                            {
                                'entity': 'offer',
                                'titles': {'raw': 'Куртка мужская Adidas размер XL'},
                                'debug': {
                                    'factors': {
                                        'USER_ECOM_SIZE_COINCIDE': Round(0.01),
                                        'USER_ECOM_SIZE_COINCIDE_NORMALIZED': Round(0.01),
                                    }
                                },
                            },
                        ]
                    }
                },
                preserve_order=True,
                allow_different_len=False,
            )

        for req in ['&hid=7811901', '&text=платье']:
            response = self.report.request_json(request_base + req)
            response = self.report.request_json(
                request_base + req + '&rearr-factors={}'.format(';'.join(rearr_factors))
            )
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "results": [
                            {
                                'entity': 'offer',
                                'titles': {'raw': 'Платье женское Baon размер S'},
                                'debug': {
                                    'factors': {
                                        'USER_ECOM_SIZE_COINCIDE': Round(0.395698764),
                                        'USER_ECOM_SIZE_COINCIDE_NORMALIZED': Round(
                                            0.395698764 / (0.395698764 + 0.288602501)
                                        ),
                                    }
                                },
                            },
                            {
                                'entity': 'offer',
                                'titles': {'raw': 'Платье женское Baon размер M'},
                                'debug': {
                                    'factors': {
                                        'USER_ECOM_SIZE_COINCIDE': Round(0.288602501),
                                        'USER_ECOM_SIZE_COINCIDE_NORMALIZED': Round(
                                            0.288602501 / (0.395698764 + 0.288602501)
                                        ),
                                    }
                                },
                            },
                            {
                                'entity': 'offer',
                                'titles': {'raw': 'Платье женское Baon размер XS'},
                                'debug': {
                                    'factors': {
                                        'USER_ECOM_SIZE_COINCIDE': Round(0.01),
                                        'USER_ECOM_SIZE_COINCIDE_NORMALIZED': Round(0.01),
                                    }
                                },
                            },
                            {
                                'entity': 'offer',
                                'titles': {'raw': 'Платье женское Baon размер L'},
                                'debug': {
                                    'factors': {
                                        'USER_ECOM_SIZE_COINCIDE': Round(0.01),
                                        'USER_ECOM_SIZE_COINCIDE_NORMALIZED': Round(0.01),
                                    }
                                },
                            },
                            {
                                'entity': 'offer',
                                'titles': {'raw': 'Платье женское Baon размер XL'},
                                'debug': {
                                    'factors': {
                                        'USER_ECOM_SIZE_COINCIDE': Round(0.01),
                                        'USER_ECOM_SIZE_COINCIDE_NORMALIZED': Round(0.01),
                                    }
                                },
                            },
                        ]
                    }
                },
                preserve_order=True,
                allow_different_len=False,
            )

        rearr_factors = [
            'market_boost_all_personal_gl_params_coeff=1.2',
            'market_boost_personal_gl_params_slope=1',
            'fetch_recom_profile_for_prime=1',
            'market_boost_personal_gl_params_threshold=0.5',
            'market_disable_boosts=boost_sigmoid_1,boost_sigmoid_2,boost_popular',
            'market_enable_new_booster=1',
        ]

        request_base = "place=prime&yandexuid=4423701"
        for req in ['&hid=7812186', '&text=куртка']:
            response = self.report.request_json(
                request_base + req + '&rearr-factors={}'.format(';'.join(rearr_factors))
            )
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "results": [
                            {'entity': 'offer', 'titles': {'raw': 'Куртка мужская Adidas размер M'}},
                            {'entity': 'offer', 'titles': {'raw': 'Куртка мужская Adidas размер L'}},
                            {'entity': 'offer', 'titles': {'raw': 'Куртка мужская Adidas размер XL'}},
                            {'entity': 'offer', 'titles': {'raw': 'Куртка мужская Adidas размер XS'}},
                            {'entity': 'offer', 'titles': {'raw': 'Куртка мужская Adidas размер S'}},
                        ]
                    }
                },
                preserve_order=True,
                allow_different_len=False,
            )

        for req in ['&hid=7811901', '&text=платье']:
            response = self.report.request_json(request_base + req)
            response = self.report.request_json(
                request_base + req + '&rearr-factors={}'.format(';'.join(rearr_factors))
            )
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "results": [
                            {'entity': 'offer', 'titles': {'raw': 'Платье женское Baon размер S'}},
                            {'entity': 'offer', 'titles': {'raw': 'Платье женское Baon размер M'}},
                            {'entity': 'offer', 'titles': {'raw': 'Платье женское Baon размер L'}},
                            {'entity': 'offer', 'titles': {'raw': 'Платье женское Baon размер XS'}},
                            {'entity': 'offer', 'titles': {'raw': 'Платье женское Baon размер XL'}},
                        ]
                    }
                },
                preserve_order=True,
                allow_different_len=False,
            )

    def test_boost_personal_gl_params_sigmoid(self):
        rearr_factors = [
            'market_boost_all_personal_gl_params_coeff=1.2',
            'fetch_recom_profile_for_prime=1',
            'market_boost_personal_gl_params_avg=0.35',
            'market_boost_personal_gl_params_slope=10.0',
            'market_boost_personal_gl_params_min_probability=0.00001',
            'market_disable_boosts=boost_popular,boost_personal,boost_sigmoid_2',
            'market_enable_new_booster=1',
        ]

        request_base = "place=prime&yandexuid=4423701"
        for req in ['&hid=7812186', '&text=куртка']:
            response = self.report.request_json(
                request_base + req + '&rearr-factors={}'.format(';'.join(rearr_factors))
            )
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "results": [
                            {'entity': 'offer', 'titles': {'raw': 'Куртка мужская Adidas размер XS'}},
                            {'entity': 'offer', 'titles': {'raw': 'Куртка мужская Adidas размер S'}},
                            {'entity': 'offer', 'titles': {'raw': 'Куртка мужская Adidas размер M'}},
                            {'entity': 'offer', 'titles': {'raw': 'Куртка мужская Adidas размер L'}},
                            {'entity': 'offer', 'titles': {'raw': 'Куртка мужская Adidas размер XL'}},
                        ]
                    }
                },
                preserve_order=True,
                allow_different_len=False,
            )

        for req in ['&hid=7811901', '&text=платье']:
            response = self.report.request_json(request_base + req)
            response = self.report.request_json(
                request_base + req + '&rearr-factors={}'.format(';'.join(rearr_factors))
            )
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "results": [
                            {'entity': 'offer', 'titles': {'raw': 'Платье женское Baon размер S'}},
                            {'entity': 'offer', 'titles': {'raw': 'Платье женское Baon размер XS'}},
                            {'entity': 'offer', 'titles': {'raw': 'Платье женское Baon размер M'}},
                            {'entity': 'offer', 'titles': {'raw': 'Платье женское Baon размер L'}},
                            {'entity': 'offer', 'titles': {'raw': 'Платье женское Baon размер XL'}},
                        ]
                    }
                },
                preserve_order=True,
                allow_different_len=False,
            )

        rearr_factors = [
            'market_boost_all_personal_gl_params_coeff=1.2',
            'fetch_recom_profile_for_prime=1',
            'market_boost_personal_gl_params_avg=0.175',
            'market_boost_personal_gl_params_slope=10.0',
            'market_boost_personal_gl_params_min_probability=0.00001',
            'market_disable_boosts=boost_popular,boost_personal,boost_sigmoid_1',
            'market_enable_new_booster=1',
        ]

        request_base = "place=prime&yandexuid=4423701"
        for req in ['&hid=7812186', '&text=куртка']:
            response = self.report.request_json(
                request_base + req + '&rearr-factors={}'.format(';'.join(rearr_factors))
            )
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "results": [
                            {'entity': 'offer', 'titles': {'raw': 'Куртка мужская Adidas размер M'}},
                            {'entity': 'offer', 'titles': {'raw': 'Куртка мужская Adidas размер XS'}},
                            {'entity': 'offer', 'titles': {'raw': 'Куртка мужская Adidas размер S'}},
                            {'entity': 'offer', 'titles': {'raw': 'Куртка мужская Adidas размер L'}},
                            {'entity': 'offer', 'titles': {'raw': 'Куртка мужская Adidas размер XL'}},
                        ]
                    }
                },
                preserve_order=True,
                allow_different_len=False,
            )

    @classmethod
    def prepare_boost_personal_gl_params_single_gender(cls):
        profile = TEcomVersionedDjUserProfile(
            ProfileData=TVersionedProfileData(
                FashionV1=TFashionDataV1(
                    SizeClothesMale=TFashionSizeDataV1(
                        Sizes={"46": 0.188602501, "48": 0.155698764, "L": 0.155698764, "M": 0.198602501}
                    ),
                )
            )
        )

        cls.bigb.on_request(yandexuid='4423702', client='merch-machine').respond(keywords=MAN_PROFILE)
        cls.dj.on_request(yandexuid='4423702', exp='fetch_user_profile_versioned').respond(
            profile_data=profile.SerializeToString(), is_binary_data=True
        )

    def test_boost_personal_gl_params_single_gender(self):
        rearr_factors_1 = [
            'market_boost_single_personal_gl_param_coeff=1.2',
            'fetch_recom_profile_for_prime=1',
            'market_disable_boosts=boost_sigmoid_1,boost_sigmoid_2',
            'market_enable_new_booster=1',
        ]
        rearr_factors_2 = [
            'market_boost_all_personal_gl_param_coeff=1.2',
            'fetch_recom_profile_for_prime=1',
            'market_disable_boosts=boost_sigmoid_1,boost_sigmoid_2',
            'market_enable_new_booster=1',
        ]
        for rearr in [rearr_factors_1, rearr_factors_2]:
            request_base = "place=prime&yandexuid=4423702"
            for req in ['&hid=7811901', '&text=платье']:
                response = self.report.request_json(request_base + req + '&rearr-factors={}'.format(';'.join(rearr)))

                # В еком-профиле нет женских размеров,
                # проверяем, что бустятся популярные
                self.assertFragmentIn(
                    response,
                    {
                        "search": {
                            "results": [
                                {'entity': 'offer', 'titles': {'raw': 'Платье женское Baon размер S'}},
                                {'entity': 'offer', 'titles': {'raw': 'Платье женское Baon размер M'}},
                                {'entity': 'offer', 'titles': {'raw': 'Платье женское Baon размер L'}},
                                {'entity': 'offer', 'titles': {'raw': 'Платье женское Baon размер XS'}},
                                {'entity': 'offer', 'titles': {'raw': 'Платье женское Baon размер XL'}},
                            ]
                        }
                    },
                    preserve_order=True,
                    allow_different_len=False,
                )

    @classmethod
    def prepare_boost_personal_brands(cls):
        for seq, (brand_id, brand_name) in enumerate(FASHION_BRAND_VALUES):
            cls.index.offers += [
                Offer(
                    hid=7812157,
                    picture=Pictures[0],
                    title='Футболка мужская ' + brand_name,
                    vendor_id=brand_id,
                    ts=4450900 + seq,
                ),
                Offer(
                    hid=7811908,
                    picture=Pictures[0],
                    title='Толстовка женская ' + brand_name,
                    vendor_id=brand_id,
                    ts=4450950 + seq,
                ),
            ]

        for seq in range(0, 10):
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 4450950 + seq).respond(60.0 - seq * 0.01)
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 4450900 + seq).respond(60.0 - seq * 0.01)

        profile = TEcomVersionedDjUserProfile(
            ProfileData=TVersionedProfileData(
                BrandsInDepartmentV1=TBrandsInDepartmentDataV1(
                    Departments={7811908: TBrandsDataV1(Brands={4450903: 0.5, 4450905: 0.5})}
                ),
                BrandsV1=TBrandsDataV1(Brands={4450902: 0.5, 4450904: 0.5}),
            )
        )

        cls.bigb.on_request(yandexuid='4450901', client='merch-machine').respond(keywords=MAN_PROFILE)
        cls.dj.on_request(yandexuid='4450901', exp='fetch_user_profile_versioned').respond(
            profile_data=profile.SerializeToString(), is_binary_data=True
        )

        cls.index.booster_config_records += [
            BoosterConfigRecord(
                name='personal_brand_boost',
                type_name='personal_brand_boost',
                args={
                    '@type': 'type.googleapis.com/MarketSearch.Booster.TMultiBoostArgs',
                },
                base_coeffs={
                    'text': 1.2,
                    'textless': 1.2,
                },
                hids=[7877999],
            )
        ]

    def test_boost_personal_brands(self):
        rearr_factors_text = [
            'market_boost_personal_brands_coeff=1.2',
            'market_disable_boost_types=personal_gl_filter_boost',
            'market_enable_new_booster=1',
        ]
        rearr_factors_textless = [
            'market_boost_personal_brands_coeff_textless=1.2',
            'market_disable_boost_types=personal_gl_filter_boost',
            'market_enable_new_booster=1',
        ]
        request_base = "place=prime&yandexuid=4450901&rearr-factors=fetch_recom_profile_for_prime=1"

        for req in [
            '&hid=7812157&rearr-factors={}'.format(';'.join(rearr_factors_textless)),
            '&text=футболка&rearr-factors={}'.format(';'.join(rearr_factors_text)),
        ]:
            # под флагом - бустятся вендоры по Всем товарам
            response = self.report.request_json(request_base + req)
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "results": [
                            {'entity': 'offer', 'titles': {'raw': 'Футболка мужская FiNN FLARE'}},
                            {'entity': 'offer', 'titles': {'raw': 'Футболка мужская Tom Tailor'}},
                            {'entity': 'offer', 'titles': {'raw': 'Футболка мужская NIKE'}},
                            {'entity': 'offer', 'titles': {'raw': 'Футболка мужская LACOSTE'}},
                            {'entity': 'offer', 'titles': {'raw': 'Футболка мужская ТВОЕ'}},
                        ]
                    }
                },
                preserve_order=True,
                allow_different_len=False,
            )

        for req in [
            '&hid=7811908&rearr-factors={}'.format(';'.join(rearr_factors_textless)),
            '&text=толстовка&rearr-factors={}'.format(';'.join(rearr_factors_text)),
        ]:
            # под флагом - бустятся вендоры по категории
            response = self.report.request_json(request_base + req)
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "results": [
                            {'entity': 'offer', 'titles': {'raw': 'Толстовка женская LACOSTE'}},
                            {'entity': 'offer', 'titles': {'raw': 'Толстовка женская ТВОЕ'}},
                            {'entity': 'offer', 'titles': {'raw': 'Толстовка женская NIKE'}},
                            {'entity': 'offer', 'titles': {'raw': 'Толстовка женская FiNN FLARE'}},
                            {'entity': 'offer', 'titles': {'raw': 'Толстовка женская Tom Tailor'}},
                        ]
                    }
                },
                preserve_order=True,
                allow_different_len=False,
            )

    @classmethod
    def prepare_boost_gender_gl_params(cls):
        cls.index.hypertree += [
            HyperCategory(
                hid=90509,
                name='Товары для красоты',
                children=[
                    HyperCategory(
                        hid=91157,
                        name='Косметика, парфюмерия и уход',
                        children=[
                            HyperCategory(hid=15927546, name='Парфюмерия'),
                            HyperCategory(hid=8480738, name='Наборы'),
                        ],
                    ),
                ],
            ),
        ]

        cls.index.gltypes += [
            GLType(
                param_id=PERFUME_GENDER_PARAM_ID,
                hid=15927546,
                gltype=GLType.ENUM,
                xslname='sex',
                values=[
                    GLValue(value_id=value_id, text=param_name)
                    for (value_id, param_name) in PERFUME_GENDER_PARAM_VALUES
                ],
            ),
        ]

        for seq, (men_brand, women_brand, unisex_brand) in enumerate(
            zip(PERFUME_BRAND_VALUES_MEN, PERFUME_BRAND_VALUES_WOMEN, PERFUME_BRAND_VALUES_UNISEX)
        ):
            cls.index.offers += [
                Offer(
                    hid=15927546,
                    title='Туалетная вода ' + str(men_brand[1]),
                    ts=4484000 + seq,
                    glparams=[
                        GLParam(param_id=PERFUME_GENDER_PARAM_ID, value=PERFUME_GENDER_PARAM_VALUES[1][0]),
                    ],
                ),
                Offer(
                    hid=15927546,
                    title='Туалетная вода ' + str(women_brand[1]),
                    ts=4484010 + seq,
                    glparams=[
                        GLParam(param_id=PERFUME_GENDER_PARAM_ID, value=PERFUME_GENDER_PARAM_VALUES[0][0]),
                    ],
                ),
                Offer(
                    hid=15927546,
                    title='Туалетная вода ' + str(unisex_brand[1]),
                    ts=4484020 + seq,
                    glparams=[
                        GLParam(param_id=PERFUME_GENDER_PARAM_ID, value=PERFUME_GENDER_PARAM_VALUES[2][0]),
                    ],
                ),
            ]

        for seq in range(0, 10):
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 4484000 + seq).respond(60.9 - seq)
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 4484010 + seq).respond(60.8 - seq)
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 4484020 + seq).respond(60.7 - seq)

        cls.bigb.on_request(yandexuid='4484001', client='merch-machine').respond(keywords=MAN_PROFILE)
        cls.bigb.on_request(yandexuid='4484002', client='merch-machine').respond(keywords=WOMAN_PROFILE)
        cls.bigb.on_request(yandexuid='4484003', client='merch-machine').respond(keywords=UNKNOWN_PROFILE)

        cls.index.booster_config_records += [
            BoosterConfigRecord(
                name='gender_gl_boost',
                type_name='gender_gl_boost',
                args={
                    '@type': 'type.googleapis.com/MarketSearch.Booster.TMultiBoostArgs',
                    'avg': 0.5,
                    'slope': 1.0,
                    'min_probability': 0.0,
                },
                base_coeffs={
                    'text': 1.2,
                    'textless': 1.2,
                },
                hids=[],
            )
        ]

    def test_boost_gender_gl_params(self):
        expected_result = [
            # men
            [
                'Hugo Boss',
                'ARMANI',
                'Paco Rabanne',
                'Calvin Klein',
                'Hermes',
                'Escentric Molecules',
                'Chanel',
                'Givenchy',
                'Yves Saint Laurent',
            ],
            # women
            [
                'Chanel',
                'Givenchy',
                'Yves Saint Laurent',
                'Calvin Klein',
                'Hermes',
                'Escentric Molecules',
                'Hugo Boss',
                'ARMANI',
                'Paco Rabanne',
            ],
            # unknown gender
            [
                'Hugo Boss',
                'Chanel',
                'Calvin Klein',
                'ARMANI',
                'Givenchy',
                'Hermes',
                'Paco Rabanne',
                'Yves Saint Laurent',
                'Escentric Molecules',
            ],
        ]
        rearr_factors = [
            'market_boost_gender_coeff_text=1.2',
            'market_boost_gender_coeff_textless=1.2',
            'market_enable_new_booster=1',
        ]

        # Перебираем yandexuid для пользователей разного пола
        # Проверяем, что порядок брендов для пользователя
        # соответствует expected_result
        request_base = "place=prime&rearr-factors={}".format(';'.join(rearr_factors))
        for seq, yandexuid in enumerate(['4484001', '4484002', '4484003']):
            for req in ['&hid=15927546', '&text=туалетная вода']:
                request = request_base + req + '&yandexuid={}'.format(yandexuid)
                response = self.report.request_json(request)
                self.assertFragmentIn(
                    response,
                    {
                        "results": [
                            {'entity': 'offer', 'titles': {'raw': 'Туалетная вода ' + expected_result[seq][i]}}
                            for i in range(0, 9)
                        ]
                    },
                    preserve_order=True,
                    allow_different_len=False,
                )

    @classmethod
    def prepare_boost_gender_categories(cls):
        cls.index.hypertree += [
            HyperCategory(
                hid=198118,
                name='Бытовая техника',
                children=[
                    HyperCategory(
                        hid=922553,
                        name='Техника для красоты',
                        children=[
                            HyperCategory(hid=90570, name='Электробритвы мужские'),
                            HyperCategory(hid=91161, name='Эпиляторы и женские электробритвы'),
                        ],
                    ),
                ],
            ),
        ]

        cls.index.offers += [
            Offer(hid=90570, title='Электробритва мужская Braun', ts=4484050),
            Offer(hid=91161, title='Электробритва женская Braun', ts=4484051),
            Offer(hid=91161, title='Электробритва женская Philips', ts=4484052),
            Offer(hid=90570, title='Электробритва мужская Philips', ts=4484053),
        ]

        for seq in range(0, 10):
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 4484050 + seq).respond(60.9 - seq)

        cls.index.booster_config_records += [
            BoosterConfigRecord(
                name='gender_hid_boost',
                type_name='gender_hid_boost',
                args={
                    '@type': 'type.googleapis.com/MarketSearch.Booster.TMultiBoostArgs',
                    'avg': 0.5,
                    'slope': 1.0,
                    'min_probability': 0.0,
                },
                base_coeffs={
                    'text': 1.2,
                    'textless': 1.2,
                },
                hids=[],
            )
        ]

    def test_boost_gender_categories(self):
        expected_result = [
            # men
            ['мужская Braun', 'мужская Philips', 'женская Braun', 'женская Philips'],
            # women
            ['женская Braun', 'женская Philips', 'мужская Braun', 'мужская Philips'],
            # unknown
            ['мужская Braun', 'женская Braun', 'женская Philips', 'мужская Philips'],
        ]
        rearr_factors = [
            'market_boost_gender_coeff_text=1.2',
            'market_boost_gender_coeff_textless=1.2',
            'market_enable_new_booster=1',
        ]

        # Перебираем yandexuid для пользователей разного пола
        # Проверяем, что порядок офферов для пользователя
        # соответствует expected_result
        request_base = "place=prime&rearr-factors={}".format(';'.join(rearr_factors))
        for seq, yandexuid in enumerate(['4484001', '4484002', '4484003']):
            for req in ['&hid=922553', '&text=электробритва']:
                request = request_base + req + '&yandexuid={}'.format(yandexuid)
                response = self.report.request_json(request)
                self.assertFragmentIn(
                    response,
                    {
                        "results": [
                            {'entity': 'offer', 'titles': {'raw': 'Электробритва ' + expected_result[seq][i]}}
                            for i in range(0, 4)
                        ]
                    },
                    preserve_order=True,
                    allow_different_len=False,
                )

    @classmethod
    def prepare_factor_fashion_suitable_season_boost(cls):

        # Current date 21/01/2022 @ 12:00
        cls.settings.microseconds_for_disabled_random = 1642755642000000

        cls.index.hypertree += [
            HyperCategory(
                hid=78779992,
                name='Одежда, обувь и аксессуары',
                children=[
                    HyperCategory(
                        hid=78118772,
                        name='Мужская одежда',
                        children=[
                            HyperCategory(hid=78121632, name='Домашняя одежда'),
                        ],
                    ),
                    HyperCategory(
                        hid=7812191,
                        name='Обувь',
                        children=[
                            HyperCategory(
                                hid=7815020,
                                name="Обувь для мальчиков",
                                children=[HyperCategory(hid=7815018, name="Обувь для малышей")],
                            ),
                        ],
                    ),
                ],
            ),
        ]
        cls.index.gltypes += [
            GLType(
                param_id=FASHION_SEASON_PARAM_ID,
                hid=78118772,
                gltype=GLType.ENUM,
                xslname='weather_season_gl',
                values=[
                    GLValue(value_id=value_id, text=param_name)
                    for (value_id, param_name) in FASHION_SEASON_PARAM_VALUES
                ],
            ),
            GLType(
                param_id=FASHION_SEASON_PARAM_ID,
                hid=7815018,
                gltype=GLType.ENUM,
                xslname='weather_season_gl',
                values=[
                    GLValue(value_id=value_id, text=param_name)
                    for (value_id, param_name) in FASHION_SEASON_PARAM_VALUES
                ],
            ),
        ]
        for seq, (value_id, param_name) in enumerate(FASHION_SEASON_PARAM_VALUES):
            cls.index.offers += [
                Offer(
                    hid=78121632,
                    title='Домашняя одежда ' + param_name,
                    ts=1016767 + seq,
                    glparams=[
                        GLParam(param_id=FASHION_SEASON_PARAM_ID, value=value_id),
                    ],
                ),
                Offer(
                    hid=7815018,
                    title='Обувь для малышей ' + param_name,
                    ts=1056767 + seq,
                    glparams=[
                        GLParam(param_id=FASHION_SEASON_PARAM_ID, value=value_id),
                    ],
                ),
            ]

        for seq in range(0, 10):
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1016767 + seq).respond(60.9 - seq)
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1056767 + seq).respond(60.9 - seq)

        cls.index.booster_config_factors += [
            BoosterConfigFactor(
                type_name='fashion_suitable_season',
                args={
                    '@type': 'type.googleapis.com/MarketSearch.Booster.TFashionSeasonFactorArgs',
                    'xsl_name': 'weather_season_gl',
                    'periods': [
                        {
                            'hids': [78118772],
                            'param_value': 'лето',
                            'start': {'month': 5, 'day': 28},
                            'end': {'month': 9, 'day': 7},
                        },
                        {
                            'hids': [78118772],
                            'param_value': 'зима',
                            'start': {'month': 10, 'day': 21},
                            'end': {'month': 1, 'day': 31},
                        },
                        {
                            'hids': [78118772],
                            'param_value': 'демисезон/зима',
                            'start': {'month': 1, 'day': 20},
                            'end': {'month': 2, 'day': 28},
                        },
                        {
                            'hids': [78118772],
                            'param_value': 'демисезон/лето',
                            'start': {'month': 8, 'day': 7},
                            'end': {'month': 8, 'day': 28},
                        },
                        {
                            'hids': [78118772],
                            'param_value': 'всесезон',
                            'start': {'month': 5, 'day': 15},
                            'end': {'month': 5, 'day': 14},
                            # Проверяем, что работает при end < start
                        },
                    ],
                },
            ),
        ]

        cls.index.booster_config_records += [
            BoosterConfigRecord(
                name='fashion_suitable_season_boost',
                type_name='first_factor_boost',
                args={
                    '@type': 'type.googleapis.com/MarketSearch.Booster.TFactorBoostArgs',
                    'factors': [
                        {
                            'factor_name': 'FASHION_SUITABLE_SEASON',
                            'category_args': [
                                {'left_boundary': 0, 'right_boundary': 1, 'probability': 0, 'factor_multiplier': 1},
                            ],
                        },
                    ],
                },
                base_coeffs={
                    'text': 1.0,
                    'textless': 1.0,
                },
            ),
            BoosterConfigRecord(
                name='fashion_suitable_season_deboost',
                type_name='first_factor_boost',
                args={
                    '@type': 'type.googleapis.com/MarketSearch.Booster.TFactorBoostArgs',
                    'factors': [
                        {
                            'factor_name': 'FASHION_SUITABLE_SEASON',
                            'category_args': [
                                {'left_boundary': -1, 'right_boundary': 0, 'probability': 0, 'factor_multiplier': -1},
                            ],
                        },
                    ],
                },
                base_coeffs={
                    'text': 1,
                    'textless': 1,
                },
            ),
        ]

    @skip('customized seasons disabled')
    def test_factor_fashion_suitable_season_boost(self):
        rearr_factors_exp = [
            'market_enable_new_booster=1',
            'market_modify_boosts_text=fashion_suitable_season_boost:1.5',
            'market_modify_boosts_textless=fashion_suitable_season_boost:1.5',
        ]
        rearr_factors_default = [
            'market_enable_new_booster=1',
        ]
        request_base = "place=prime&debug=da"

        for req in [
            '&hid=78121632&rearr-factors={}'.format(';'.join(rearr_factors_default)),
            '&text=домашняя одежда&rearr-factors={}'.format(';'.join(rearr_factors_default)),
        ]:
            # под флагом default нет буста
            response = self.report.request_json(request_base + req)
            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {'entity': 'offer', 'titles': {'raw': 'Домашняя одежда всесезон'}},
                        {'entity': 'offer', 'titles': {'raw': 'Домашняя одежда зима'}},
                        {'entity': 'offer', 'titles': {'raw': 'Домашняя одежда лето'}},
                        {'entity': 'offer', 'titles': {'raw': 'Домашняя одежда демисезон/лето'}},
                        {'entity': 'offer', 'titles': {'raw': 'Домашняя одежда демисезон/зима'}},
                    ]
                },
                preserve_order=True,
            )

        for req in [
            '&hid=78121632&rearr-factors={}'.format(';'.join(rearr_factors_exp)),
            '&text=домашняя одежда&rearr-factors={}'.format(';'.join(rearr_factors_exp)),
        ]:
            # под флагом exp есть буст для значения фильтра 'всесезон', 'зима' и 'демисезон/лето'
            response = self.report.request_json(request_base + req)
            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {'entity': 'offer', 'titles': {'raw': 'Домашняя одежда всесезон'}},
                        {'entity': 'offer', 'titles': {'raw': 'Домашняя одежда зима'}},
                        {'entity': 'offer', 'titles': {'raw': 'Домашняя одежда демисезон/зима'}},
                        {'entity': 'offer', 'titles': {'raw': 'Домашняя одежда лето'}},
                        {'entity': 'offer', 'titles': {'raw': 'Домашняя одежда демисезон/лето'}},
                    ]
                },
                preserve_order=True,
            )

            # Для летней одежды должен посчитаться фактор
            self.assertFragmentIn(
                response,
                {
                    'results': [
                        {
                            'entity': 'offer',
                            'titles': {'raw': 'Домашняя одежда всесезон'},
                            'debug': {'factors': {'FASHION_SUITABLE_SEASON': '1'}},
                        },
                        {
                            'entity': 'offer',
                            'titles': {'raw': 'Домашняя одежда зима'},
                            'debug': {'factors': {'FASHION_SUITABLE_SEASON': '1'}},
                        },
                        {
                            'entity': 'offer',
                            'titles': {'raw': 'Домашняя одежда демисезон/зима'},
                            'debug': {'factors': {'FASHION_SUITABLE_SEASON': '1'}},
                        },
                        {
                            'entity': 'offer',
                            'titles': {'raw': 'Домашняя одежда лето'},
                            'debug': {'factors': {'FASHION_SUITABLE_SEASON': NoKey('FASHION_SUITABLE_SEASON')}},
                        },
                    ]
                },
            )

    def test_factor_fashion_suitable_hardcoded_season_boost(self):
        rearr_factors_exp = [
            'market_enable_new_booster=1',
            'market_modify_boosts_text=fashion_suitable_season_deboost:0.5',
            'market_modify_boosts_textless=fashion_suitable_season_deboost:0.5',
            'market_modify_boosts_text=fashion_suitable_season_boost:1.5',
            'market_modify_boosts_textless=fashion_suitable_season_boost:1.5',
        ]
        rearr_factors_default = [
            'market_enable_new_booster=1',
        ]
        request_base = "place=prime&debug=da"

        for req in [
            '&hid=7815018&rearr-factors={}'.format(';'.join(rearr_factors_default)),
        ]:
            # под флагом default нет буста
            response = self.report.request_json(request_base + req)
            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {'entity': 'offer', 'titles': {'raw': 'Обувь для малышей всесезон'}},
                        {'entity': 'offer', 'titles': {'raw': 'Обувь для малышей зима'}},
                        {'entity': 'offer', 'titles': {'raw': 'Обувь для малышей лето'}},
                        {'entity': 'offer', 'titles': {'raw': 'Обувь для малышей демисезон/лето'}},
                        {'entity': 'offer', 'titles': {'raw': 'Обувь для малышей демисезон/зима'}},
                    ]
                },
                preserve_order=True,
            )

        for req in [
            '&hid=7815018&rearr-factors={}'.format(';'.join(rearr_factors_exp)),
        ]:
            response = self.report.request_json(request_base + req)
            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {'entity': 'offer', 'titles': {'raw': 'Обувь для малышей зима'}},
                        {'entity': 'offer', 'titles': {'raw': 'Обувь для малышей демисезон/зима'}},
                        {'entity': 'offer', 'titles': {'raw': 'Обувь для малышей всесезон'}},
                        {'entity': 'offer', 'titles': {'raw': 'Обувь для малышей демисезон/лето'}},
                        {'entity': 'offer', 'titles': {'raw': 'Обувь для малышей лето'}},
                    ]
                },
                preserve_order=True,
            )

            self.assertFragmentIn(
                response,
                {
                    'results': [
                        {
                            'entity': 'offer',
                            'titles': {'raw': 'Обувь для малышей всесезон'},
                            'debug': {'factors': {'FASHION_SUITABLE_SEASON': Round(-1.0 / 3)}},
                        },
                        {
                            'entity': 'offer',
                            'titles': {'raw': 'Обувь для малышей зима'},
                            'debug': {'factors': {'FASHION_SUITABLE_SEASON': '1'}},
                        },
                        {
                            'entity': 'offer',
                            'titles': {'raw': 'Обувь для малышей демисезон/зима'},
                            'debug': {'factors': {'FASHION_SUITABLE_SEASON': Round(2.0 / 3)}},
                        },
                        {
                            'entity': 'offer',
                            'titles': {'raw': 'Обувь для малышей демисезон/лето'},
                            'debug': {'factors': {'FASHION_SUITABLE_SEASON': Round(-2.0 / 3)}},
                        },
                        {
                            'entity': 'offer',
                            'titles': {'raw': 'Обувь для малышей лето'},
                            'debug': {'factors': {'FASHION_SUITABLE_SEASON': '-1'}},
                        },
                    ]
                },
            )

    @staticmethod
    def create_consecutive_offer(hyperid, size_index, seq):
        return Offer(
            hid=7812188,
            hyperid=hyperid,
            fesh=4576910,
            picture=Pictures[0],
            title='Пальто мужское размер ' + CLOTHES_SIZE_PARAM_VALUES[size_index][1],
            glparams=[
                GLParam(param_id=CLOTHES_SIZE_PARAM_ID, value=CLOTHES_SIZE_PARAM_VALUES[size_index][0]),
            ],
            ts=4576900 + seq,
        )

    @classmethod
    def prepare_boost_consecutive_glparams(cls):
        cls.index.models += [
            Model(hyperid=4576901, hid=7812188, title='Пальто мужское, разм. XS'),
            Model(hyperid=4576902, hid=7812188, title='Пальто мужское, разм. M L'),
            Model(hyperid=4576903, hid=7812188, title='Пальто мужское, разм. XS S L XL'),
        ]

        cls.index.offers += [
            cls.create_consecutive_offer(4576901, 0, 0),
            cls.create_consecutive_offer(4576902, 3, 1),
            cls.create_consecutive_offer(4576902, 4, 2),
            cls.create_consecutive_offer(4576903, 0, 3),
            cls.create_consecutive_offer(4576903, 1, 4),
            cls.create_consecutive_offer(4576903, 3, 5),
            cls.create_consecutive_offer(4576903, 4, 6),
        ]

        for seq in range(0, 50):
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 4576900 + seq).respond(60.9 - seq)

        cls.index.shops += [
            Shop(fesh=4576910, priority_region=213, regions=[213]),
        ]

        profile_with_xs = TEcomVersionedDjUserProfile(
            ProfileData=TVersionedProfileData(
                FashionV1=TFashionDataV1(
                    SizeClothesMale=TFashionSizeDataV1(Sizes={"XS": 0.955698764}),
                )
            )
        )

        profile_no_size = TEcomVersionedDjUserProfile(ProfileData=TVersionedProfileData())

        cls.bigb.on_request(yandexuid='4576901', client='merch-machine').respond(keywords=UNKNOWN_PROFILE)
        cls.bigb.on_request(yandexuid='4576902', client='merch-machine').respond(keywords=UNKNOWN_PROFILE)
        cls.dj.on_request(yandexuid='4576901', exp='fetch_user_profile_versioned').respond(
            profile_data=profile_with_xs.SerializeToString(), is_binary_data=True
        )
        cls.dj.on_request(yandexuid='4576902', exp='fetch_user_profile_versioned').respond(
            profile_data=profile_no_size.SerializeToString(), is_binary_data=True
        )

        cls.index.booster_config_records += [
            #            BoosterConfigRecord(
            #                name='consecutive_gl_filters_boost',
            #                type_name='consecutive_gl_filters_boost',
            #                args={
            #                    '@type': 'type.googleapis.com/MarketSearch.Booster.TNoArgs',
            #                },
            #                base_coeffs={
            #                    'text': 1.0,
            #                    'textless': 1.0,
            #                },
            #            )
        ]

    @skip('Broken, needs fix')
    def test_boost_consecutive_glparams(self):
        expected_result = [
            # default
            ['XS', 'M L', 'XS S L XL'],
            # personal XS boost
            ['XS S L XL', 'XS', 'M L'],
            # popular M L XL boost
            ['M L', 'XS S L XL', 'XS'],
        ]

        rearr_factors_exp = [
            'fetch_recom_profile_for_prime=1',
            'market_enable_new_booster=1',
            'market_modify_boosts_text=consecutive_gl_filters_boost:1.5',
            'market_modify_boosts_textless=consecutive_gl_filters_boost:1.5',
            'market_disable_boosts=boost_sigmoid_1,boost_sigmoid_2,boost_personal,boost_popular',
        ]
        rearr_factors_default = [
            'fetch_recom_profile_for_prime=1',
            'market_enable_new_booster=1',
            'market_disable_boosts=boost_sigmoid_1,boost_sigmoid_2,boost_personal,boost_popular',
        ]
        request_base = "place=prime&yandexuid=4576901&allow-collapsing=1&entities=offer"

        for req in [
            '&hid=7812188&rearr-factors={}'.format(';'.join(rearr_factors_default)),
            '&text=пальто&rearr-factors={}'.format(';'.join(rearr_factors_default)),
        ]:
            # под флагом default нет буста
            response = self.report.request_json(request_base + req)
            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {'entity': 'product', 'titles': {'raw': 'Пальто мужское, разм. ' + expected_result[0][i]}}
                        for i in range(0, 3)
                    ]
                },
                preserve_order=True,
                allow_different_len=False,
            )

        for req in [
            '&hid=7812188&rearr-factors={}'.format(';'.join(rearr_factors_exp)),
            '&text=пальто&rearr-factors={}'.format(';'.join(rearr_factors_exp)),
        ]:
            # есть буст параметров XS-S
            response = self.report.request_json(request_base + req)
            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {'entity': 'product', 'titles': {'raw': 'Пальто мужское, разм. ' + expected_result[1][i]}}
                        for i in range(0, 3)
                    ]
                },
                preserve_order=True,
                allow_different_len=False,
            )

        request_base = "place=prime&yandexuid=4576902&allow-collapsing=1&entities=offer"
        for req in [
            '&hid=7812188&rearr-factors={}'.format(';'.join(rearr_factors_exp)),
            '&text=пальто&rearr-factors={}'.format(';'.join(rearr_factors_exp)),
        ]:
            # есть буст параметров M-L-XL из популярных
            response = self.report.request_json(request_base + req)
            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {'entity': 'product', 'titles': {'raw': 'Пальто мужское, разм. ' + expected_result[2][i]}}
                        for i in range(0, 3)
                    ]
                },
                preserve_order=True,
                allow_different_len=False,
            )

    def test_nid_without_hid(self):
        expected_nums = [1, 0, 3, 2]
        boosted_docs = [1, 1, 0, 0]
        response = self.report.request_json(
            'nid=302&use-multi-navigation-trees=1&place=prime&allow-collapsing=0&entities=offer&debug=da&rearr-factors=market_enable_new_booster=1'
        )

        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        'entity': 'offer',
                        'titles': {'raw': 'нидка ' + str(expected_nums[i])},
                        'debug': {'properties': {'BOOST_MULTIPLIER': '1.5' if boosted_docs[i] else '1'}},
                    }
                    for i in range(4)
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )


if __name__ == '__main__':
    main()

#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import MnPlace, Offer, GLValue, GLParam, GLType, HyperCategory
from core.testcase import TestCase, main
from core.bigb import BigBKeyword, WeightedValue
from core.matcher import NoKey, Round

from market.proto.recom.exported_dj_user_profile_pb2 import (
    TEcomVersionedDjUserProfile,
    TVersionedProfileData,
    TBrandsDataV1,
    TChildrenDataV1,
    TIndicatorData,
    TChildGenderDataV1,
    TChildAgeDataV1,
)  # noqa pylint: disable=import-error


WOMAN_PROFILE = [
    BigBKeyword(
        id=BigBKeyword.GENDER,
        weighted_uint_values=[
            WeightedValue(value=BigBKeyword.GENDER_MALE, weight=21947),
            WeightedValue(value=BigBKeyword.GENDER_FEMALE, weight=941232),
        ],
    ),
]

MAN_PROFILE = [
    BigBKeyword(
        id=BigBKeyword.GENDER,
        weighted_uint_values=[
            WeightedValue(value=BigBKeyword.GENDER_MALE, weight=941232),
            WeightedValue(value=BigBKeyword.GENDER_FEMALE, weight=21947),
        ],
    ),
]

GENDER_PARAM_ID = 15927831
GENDER_PARAM_VALUES = [
    (15927836, 'женский'),
    (15927847, 'мужской'),
    (15927853, 'унисекс'),
]


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.set_default_reqid = False

        cls.index.hypertree += [
            HyperCategory(
                hid=7877999,
                name='Одежда, обувь и аксессуары',
                children=[
                    HyperCategory(
                        hid=7811873, name='Женская одежда', children=[HyperCategory(hid=7811901, name='Женские платья')]
                    ),
                    HyperCategory(
                        hid=7811877,
                        name='Мужская одежда',
                        children=[
                            HyperCategory(hid=7812156, name='Мужские костюмы'),
                            HyperCategory(hid=7812157, name='Мужские футболки'),
                            HyperCategory(
                                hid=7812139, name='Верхняя одежда', children=[HyperCategory(hid=7812186, name='Куртки')]
                            ),
                        ],
                    ),
                    HyperCategory(
                        hid=7811881,
                        name='Аксессуары',
                        children=[
                            HyperCategory(hid=7812196, name='Брелоки и ключницы'),
                            HyperCategory(hid=7812173, name='Галстуки'),
                            HyperCategory(hid=17738783, name='Кигуруми'),
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
                                    HyperCategory(hid=7812065, name='Рубашки и блузы'),
                                ],
                            ),
                        ],
                    ),
                ],
            ),
        ]

    @classmethod
    def prepare_user_favourite_brand_factor(cls):
        cls.settings.set_default_reqid = False

        BRAND_VALUES = [
            (4450901, 'NIKE'),
            (4450902, 'FiNN FLARE'),
            (4450903, 'LACOSTE'),
            (4450904, 'Tom Tailor'),
            (4450905, 'ТВОЕ'),
        ]

        for seq, (brand_id, brand_name) in enumerate(BRAND_VALUES):
            cls.index.offers += [
                Offer(hid=7811908, title='Толстовка женская ' + brand_name, vendor_id=brand_id, ts=4450950 + seq),
            ]

        for seq in range(0, 10):
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 4450950 + seq).respond(60.0 - seq * 0.01)

        profile = TEcomVersionedDjUserProfile(
            ProfileData=TVersionedProfileData(
                BrandsV1=TBrandsDataV1(Brands={4450903: 0.5, 4450905: 0.5}),
                BrandsV2=TBrandsDataV1(Brands={4450903: 0.8, 4450905: 0.8}),
            )
        )

        cls.bigb.on_request(yandexuid='4450901', client='merch-machine').respond(keywords=MAN_PROFILE)
        cls.dj.on_request(yandexuid='4450901', exp='fetch_user_profile_versioned').respond(
            profile_data=profile.SerializeToString(), is_binary_data=True
        )

    def test_user_favourite_brand_factor(self):
        request = (
            'place=prime&yandexuid=4450901{}&debug=da&rearr-factors=fetch_recom_profile_for_prime=1;'
            'use_versioned_dj_profile=0'
        )

        for query in ['&text=толстовка', '&hid=7811908']:
            response = self.report.request_json(request.format(query))
            self.assertFragmentIn(
                response,
                [
                    {
                        'entity': 'offer',
                        'titles': {'raw': 'Толстовка женская LACOSTE'},
                        'debug': {'factors': {'USER_FAVOURITE_BRAND_PROBABILITY': Round('0.5')}},
                    },
                    {
                        'entity': 'offer',
                        'titles': {'raw': 'Толстовка женская ТВОЕ'},
                        'debug': {'factors': {'USER_FAVOURITE_BRAND_PROBABILITY': Round('0.5')}},
                    },
                    {
                        'entity': 'offer',
                        'titles': {'raw': 'Толстовка женская NIKE'},
                        'debug': {
                            'factors': {'USER_FAVOURITE_BRAND_PROBABILITY': NoKey('USER_FAVOURITE_BRAND_PROBABILITY')}
                        },
                    },
                ],
            )

        request = (
            'place=prime&yandexuid=4450901{}&debug=da&rearr-factors=fetch_recom_profile_for_prime=1;'
            'use_versioned_dj_profile=1'
        )

        for query in ['&text=толстовка', '&hid=7811908']:
            response = self.report.request_json(request.format(query))
            self.assertFragmentIn(
                response,
                [
                    {
                        'entity': 'offer',
                        'titles': {'raw': 'Толстовка женская LACOSTE'},
                        'debug': {
                            'factors': {
                                'USER_FAVOURITE_BRAND_PROBABILITY': Round('0.5'),
                                'USER_FAVOURITE_BRAND_PROBABILITY_V2': Round('0.8'),
                            }
                        },
                    },
                    {
                        'entity': 'offer',
                        'titles': {'raw': 'Толстовка женская ТВОЕ'},
                        'debug': {
                            'factors': {
                                'USER_FAVOURITE_BRAND_PROBABILITY': Round('0.5'),
                                'USER_FAVOURITE_BRAND_PROBABILITY_V2': Round('0.8'),
                            }
                        },
                    },
                    {
                        'entity': 'offer',
                        'titles': {'raw': 'Толстовка женская NIKE'},
                        'debug': {
                            'factors': {
                                'USER_FAVOURITE_BRAND_PROBABILITY': NoKey('USER_FAVOURITE_BRAND_PROBABILITY'),
                                'USER_FAVOURITE_BRAND_PROBABILITY_V2': NoKey('USER_FAVOURITE_BRAND_PROBABILITY_V2'),
                            }
                        },
                    },
                ],
            )

    @classmethod
    def prepare_gl_params_factor(cls):
        CLOTHES_SIZE_PARAM_ID = 26417130
        CLOTHES_SIZE_PARAM_VALUES = [
            (27016810, 'XS'),
            (27016830, 'S'),
            (27016891, 'M'),
            (27016892, 'L'),
            (27016910, 'XL'),
        ]

        for hid in [7811877, 7812139, 7812186]:
            cls.index.gltypes += [
                GLType(
                    param_id=CLOTHES_SIZE_PARAM_ID,
                    hid=hid,
                    gltype=GLType.ENUM,
                    xslname='size_clothes_new',
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
                    title='Мужская куртка размера {}'.format(param_name),
                    glparams=[
                        GLParam(param_id=CLOTHES_SIZE_PARAM_ID, value=value_id),
                    ],
                ),
            ]

    def test_gl_params_factor(self):
        request_base = 'place=prime&debug=da'

        for req in ['&hid=7812186', '&text=куртка']:
            response = self.report.request_json(request_base + req)

            self.assertFragmentIn(
                response,
                [
                    {
                        'entity': 'offer',
                        'titles': {'raw': 'Мужская куртка размера S'},
                        'debug': {
                            'factors': {
                                'POPULAR_SIZE_PROBABILITY': NoKey('POPULAR_SIZE_PROBABILITY'),
                            }
                        },
                    },
                    {
                        'entity': 'offer',
                        'titles': {'raw': 'Мужская куртка размера M'},
                        'debug': {'factors': {'POPULAR_SIZE_PROBABILITY': Round('1')}},
                    },
                    {
                        'entity': 'offer',
                        'titles': {'raw': 'Мужская куртка размера L'},
                        'debug': {
                            'factors': {
                                'POPULAR_SIZE_PROBABILITY': Round('1'),
                            }
                        },
                    },
                ],
            )

    @classmethod
    def prepare_user_gender_coincide(cls):
        categories_with_gender_param = [7812173, 17738783]

        cls.index.gltypes += [
            GLType(
                param_id=GENDER_PARAM_ID,
                hid=hid,
                gltype=GLType.ENUM,
                xslname='sex',
                values=[GLValue(value_id=value_id, text=param_name) for (value_id, param_name) in GENDER_PARAM_VALUES],
            )
            for hid in categories_with_gender_param
        ]

        # Добавляе офферы с gl-фильтром
        for hid in categories_with_gender_param:
            for param_value, param_name in GENDER_PARAM_VALUES:
                cls.index.offers += [
                    Offer(
                        hid=hid,
                        title='offer_' + param_name + '_' + str(hid),
                        glparams=[
                            GLParam(param_id=GENDER_PARAM_ID, value=param_value),
                        ],
                    )
                ]

        # Добавляем unisex оффер
        for hid in categories_with_gender_param:
            cls.index.offers += [
                Offer(
                    hid=hid,
                    title='offer' + '_unisex2_' + str(hid),
                    glparams=[
                        GLParam(param_id=GENDER_PARAM_ID, value=15927836),  # женский
                        GLParam(param_id=GENDER_PARAM_ID, value=15927847),  # мужской
                    ],
                )
            ]

        # Добавляем офферы в гендерные категории
        for hid in [7812156, 7811901]:
            for param_value, param_name in GENDER_PARAM_VALUES:
                cls.index.offers += [Offer(hid=hid, title='offer_' + param_name + '_' + str(hid))]

        cls.index.offers += [Offer(hid=7812196, title='offer_without_gender')]

        cls.bigb.on_request(yandexuid='4484001', client='merch-machine').respond(keywords=MAN_PROFILE)
        cls.bigb.on_request(yandexuid='4484002', client='merch-machine').respond(keywords=WOMAN_PROFILE)

    def test_user_gender_coincide(self):
        request_base = 'place=prime&hid={hid}&yandexuid={yandexuid}&debug=da'

        # [Female, Male]
        uids = [4484002, 4484001]

        # Проверяем, что в гендерных категориях фактор считается
        for hid, uid in zip([7812156, 7811901], uids):
            response = self.report.request_json(request_base.format(hid=hid, yandexuid=uid))
            self.assertFragmentIn(
                response, {'debug': {'factors': {'USER_GENDER_COINCIDE': Round(2.0 * (21947.0 / 963179) - 1)}}}
            )

        for hid, uid in zip([7811901, 7812156], uids):
            response = self.report.request_json(request_base.format(hid=hid, yandexuid=uid))
            self.assertFragmentIn(
                response, {'debug': {'factors': {'USER_GENDER_COINCIDE': Round(2.0 * (941232.0 / 963179) - 1)}}}
            )

        # Проверяем, что для обычных товаров фактор не считается
        for uid in uids:
            response = self.report.request_json(request_base.format(hid=7812196, yandexuid=uid))
            self.assertFragmentIn(
                response, {'debug': {'factors': {'USER_GENDER_COINCIDE': NoKey('USER_GENDER_COINCIDE')}}}
            )

        categories_with_gender_param = [7812173, 17738783]
        # Проверяем, что фактор считается по гендерному gl-фильтру
        for hid in categories_with_gender_param:
            # Female
            response = self.report.request_json(request_base.format(hid=hid, yandexuid=4484002))
            self.assertFragmentIn(
                response,
                {
                    'results': [
                        {
                            'entity': 'offer',
                            'titles': {'raw': 'offer_женский_' + str(hid)},
                            'debug': {'factors': {'USER_GENDER_COINCIDE': Round(2.0 * (941232.0 / 963179) - 1)}},
                        },
                        {
                            'entity': 'offer',
                            'titles': {'raw': 'offer_мужской_' + str(hid)},
                            'debug': {'factors': {'USER_GENDER_COINCIDE': Round(2.0 * (21947.0 / 963179) - 1)}},
                        },
                        {
                            'entity': 'offer',
                            'titles': {'raw': 'offer_унисекс_' + str(hid)},
                            'debug': {'factors': {'USER_GENDER_COINCIDE': Round(0.5)}},
                        },
                        {
                            'entity': 'offer',
                            'titles': {'raw': 'offer_unisex2_' + str(hid)},
                            'debug': {'factors': {'USER_GENDER_COINCIDE': Round(0.5)}},
                        },
                    ]
                },
            )

            # Male
            response = self.report.request_json(request_base.format(hid=hid, yandexuid=4484001))
            self.assertFragmentIn(
                response,
                {
                    'results': [
                        {
                            'entity': 'offer',
                            'titles': {'raw': 'offer_мужской_' + str(hid)},
                            'debug': {'factors': {'USER_GENDER_COINCIDE': Round(2.0 * (941232.0 / 963179) - 1)}},
                        },
                        {
                            'entity': 'offer',
                            'titles': {'raw': 'offer_женский_' + str(hid)},
                            'debug': {'factors': {'USER_GENDER_COINCIDE': Round(2.0 * (21947.0 / 963179) - 1)}},
                        },
                        {
                            'entity': 'offer',
                            'titles': {'raw': 'offer_унисекс_' + str(hid)},
                            'debug': {'factors': {'USER_GENDER_COINCIDE': Round(0.5)}},
                        },
                        {
                            'entity': 'offer',
                            'titles': {'raw': 'offer_unisex2_' + str(hid)},
                            'debug': {'factors': {'USER_GENDER_COINCIDE': Round(0.5)}},
                        },
                    ]
                },
            )

    @classmethod
    def prepare_children_gender_factors(cls):
        cls.settings.set_default_reqid = False

        cls.index.offers += [Offer(hid=7812009, title='Игрушка для мальчиков (кат) ' + str(i)) for i in range(4)]

        cls.index.offers += [Offer(hid=7812006, title='Игрушка для девочек (кат) ' + str(i)) for i in range(4)]

        cls.index.gltypes += [
            GLType(
                param_id=12401459,
                hid=90764,
                gltype=GLType.ENUM,
                xslname='for_sex',
                values=[
                    GLValue(value_id=12401470, text='для мальчика'),
                    GLValue(value_id=12401464, text='для девочки'),
                ],
            )
        ]

        cls.index.offers += [
            Offer(
                hid=90764,
                title='Кубики для мальчиков (gl)',
                glparams=[GLParam(param_id=12401459, value=12401470)],
            ),
            Offer(
                hid=90764,
                title='Кубики для девочек (gl)',
                glparams=[GLParam(param_id=12401459, value=12401464)],
            ),
        ]

        no_children_profile = TEcomVersionedDjUserProfile(
            ProfileData=TVersionedProfileData(
                ChildrenV2=TChildrenDataV1(
                    Indicator=TIndicatorData(IsInterested=0, IsNotInterested=1),
                    ByGender=TChildGenderDataV1(Male=0, Female=0),
                )
            )
        )

        boys_profile = TEcomVersionedDjUserProfile(
            ProfileData=TVersionedProfileData(
                ChildrenV2=TChildrenDataV1(
                    Indicator=TIndicatorData(IsInterested=1, IsNotInterested=0),
                    ByGender=TChildGenderDataV1(Male=1, Female=0),
                )
            )
        )

        girls_profile = TEcomVersionedDjUserProfile(
            ProfileData=TVersionedProfileData(
                ChildrenV2=TChildrenDataV1(
                    Indicator=TIndicatorData(IsInterested=1, IsNotInterested=0),
                    ByGender=TChildGenderDataV1(Male=0, Female=1),
                )
            )
        )

        boys_and_girls_profile = TEcomVersionedDjUserProfile(
            ProfileData=TVersionedProfileData(
                ChildrenV2=TChildrenDataV1(
                    Indicator=TIndicatorData(IsInterested=1, IsNotInterested=0),
                    ByGender=TChildGenderDataV1(Male=0.67, Female=0.33),
                )
            )
        )

        cls.bigb.on_request(yandexuid='1001', client='merch-machine').respond()
        cls.bigb.on_request(yandexuid='1002', client='merch-machine').respond()
        cls.bigb.on_request(yandexuid='1003', client='merch-machine').respond()
        cls.bigb.on_request(yandexuid='1004', client='merch-machine').respond()

        cls.dj.on_request(yandexuid='1001', exp='fetch_user_profile_versioned').respond(
            profile_data=no_children_profile.SerializeToString(), is_binary_data=True
        )

        cls.dj.on_request(yandexuid='1002', exp='fetch_user_profile_versioned').respond(
            profile_data=boys_profile.SerializeToString(), is_binary_data=True
        )

        cls.dj.on_request(yandexuid='1003', exp='fetch_user_profile_versioned').respond(
            profile_data=girls_profile.SerializeToString(), is_binary_data=True
        )

        cls.dj.on_request(yandexuid='1004', exp='fetch_user_profile_versioned').respond(
            profile_data=boys_and_girls_profile.SerializeToString(), is_binary_data=True
        )

    def test_children_gender_factor_no_children(self):
        base_request = '{req}&place=prime&yandexuid={yuid}&debug=da&rearr-factors=fetch_recom_profile_for_prime=1;use_versioned_dj_profile=1'

        response = self.report.request_json(base_request.format(req='&hid=7812009', yuid=1001))
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'debug': {
                            'factors': {
                                'USER_HAS_CHILDREN': '-1',
                                'USER_CHILDREN_GENDER_COINCIDE': NoKey('USER_CHILDREN_GENDER_COINCIDE'),
                            }
                        }
                    }
                    for i in range(4)
                ],
            },
        )

        response = self.report.request_json(base_request.format(req='&hid=7812006', yuid=1001))
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'debug': {
                            'factors': {
                                'USER_HAS_CHILDREN': '-1',
                                'USER_CHILDREN_GENDER_COINCIDE': NoKey('USER_CHILDREN_GENDER_COINCIDE'),
                            }
                        }
                    }
                    for i in range(4)
                ],
            },
        )

    def test_children_gender_factor_boys_cat(self):
        base_request = '{req}&place=prime&yandexuid={yuid}&debug=da&rearr-factors=fetch_recom_profile_for_prime=1;use_versioned_dj_profile=1'

        response = self.report.request_json(base_request.format(req='&hid=7812009', yuid=1002))
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {'debug': {'factors': {'USER_HAS_CHILDREN': '1', 'USER_CHILDREN_GENDER_COINCIDE': '3'}}}
                    for i in range(4)
                ],
            },
        )

        response = self.report.request_json(base_request.format(req='&hid=7812009', yuid=1003))
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {'debug': {'factors': {'USER_HAS_CHILDREN': '1', 'USER_CHILDREN_GENDER_COINCIDE': '2'}}}
                    for i in range(4)
                ],
            },
        )

        response = self.report.request_json(base_request.format(req='&hid=7812009', yuid=1004))
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {'debug': {'factors': {'USER_HAS_CHILDREN': '1', 'USER_CHILDREN_GENDER_COINCIDE': Round('2.67')}}}
                    for i in range(4)
                ],
            },
        )

    def test_children_gender_factor_girls_cat(self):
        base_request = '{req}&place=prime&yandexuid={yuid}&debug=da&rearr-factors=fetch_recom_profile_for_prime=1;use_versioned_dj_profile=1'

        response = self.report.request_json(base_request.format(req='&hid=7812006', yuid=1002))
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {'debug': {'factors': {'USER_HAS_CHILDREN': '1', 'USER_CHILDREN_GENDER_COINCIDE': '2'}}}
                    for i in range(4)
                ],
            },
        )

        response = self.report.request_json(base_request.format(req='&hid=7812006', yuid=1003))
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {'debug': {'factors': {'USER_HAS_CHILDREN': '1', 'USER_CHILDREN_GENDER_COINCIDE': '3'}}}
                    for i in range(4)
                ],
            },
        )

        response = self.report.request_json(base_request.format(req='&hid=7812006', yuid=1004))
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {'debug': {'factors': {'USER_HAS_CHILDREN': '1', 'USER_CHILDREN_GENDER_COINCIDE': Round('2.33')}}}
                    for i in range(4)
                ],
            },
        )

    def test_children_gender_factor_gl(self):
        base_request = '{req}&place=prime&yandexuid={yuid}&debug=da&rearr-factors=fetch_recom_profile_for_prime=1;use_versioned_dj_profile=1'

        response = self.report.request_json(base_request.format(req='&hid=90764', yuid=1002))
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'titles': {
                            'raw': 'Кубики для мальчиков (gl)',
                        },
                        'debug': {
                            'factors': {
                                'USER_HAS_CHILDREN': '1',
                                'USER_CHILDREN_GENDER_COINCIDE': '3',
                            },
                        },
                    },
                    {
                        'titles': {
                            'raw': 'Кубики для девочек (gl)',
                        },
                        'debug': {
                            'factors': {
                                'USER_HAS_CHILDREN': '1',
                                'USER_CHILDREN_GENDER_COINCIDE': '2',
                            },
                        },
                    },
                ],
            },
        )

        response = self.report.request_json(base_request.format(req='&hid=90764', yuid=1003))
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'titles': {
                            'raw': 'Кубики для мальчиков (gl)',
                        },
                        'debug': {
                            'factors': {
                                'USER_HAS_CHILDREN': '1',
                                'USER_CHILDREN_GENDER_COINCIDE': '2',
                            },
                        },
                    },
                    {
                        'titles': {
                            'raw': 'Кубики для девочек (gl)',
                        },
                        'debug': {
                            'factors': {
                                'USER_HAS_CHILDREN': '1',
                                'USER_CHILDREN_GENDER_COINCIDE': '3',
                            },
                        },
                    },
                ],
            },
        )

        response = self.report.request_json(base_request.format(req='&hid=90764', yuid=1004))
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'titles': {
                            'raw': 'Кубики для мальчиков (gl)',
                        },
                        'debug': {
                            'factors': {
                                'USER_HAS_CHILDREN': '1',
                                'USER_CHILDREN_GENDER_COINCIDE': Round('2.67'),
                            },
                        },
                    },
                    {
                        'titles': {
                            'raw': 'Кубики для девочек (gl)',
                        },
                        'debug': {
                            'factors': {
                                'USER_HAS_CHILDREN': '1',
                                'USER_CHILDREN_GENDER_COINCIDE': Round('2.33'),
                            },
                        },
                    },
                ],
            },
        )

    @classmethod
    def prepare_children_age_factors(cls):
        cls.settings.set_default_reqid = False

        cls.index.gltypes += [
            GLType(
                param_id=12401459,
                hid=7812065,
                gltype=GLType.ENUM,
                xslname='pol',
                values=[
                    GLValue(value_id=12401464, text='для девочек'),
                ],
            ),
            GLType(
                param_id=28646118,
                hid=7812065,
                gltype=GLType.ENUM,
                xslname='size_kid_clothes_new',
                values=[
                    GLValue(value_id=28765883, text='10 лет'),
                    GLValue(value_id=28765886, text='14 лет'),
                ],
            ),
        ]

        cls.index.offers += [
            Offer(
                hid=7812065,
                title='Блуза 10 лет',
                glparams=[
                    GLParam(param_id=28646118, value=28765883),
                    GLParam(param_id=12401459, value=12401464),
                ],
            ),
            Offer(
                hid=7812065,
                title='Блуза 14 лет',
                glparams=[
                    GLParam(param_id=28646118, value=28765886),
                    GLParam(param_id=12401459, value=12401464),
                ],
            ),
            Offer(
                hid=7812065,
                title='Блуза 10-14 лет',
                glparams=[
                    GLParam(param_id=28646118, value=28765883),
                    GLParam(param_id=28646118, value=28765886),
                    GLParam(param_id=12401459, value=12401464),
                ],
            ),
        ]

        too_young_profile = TEcomVersionedDjUserProfile(
            ProfileData=TVersionedProfileData(
                ChildrenV2=TChildrenDataV1(
                    Indicator=TIndicatorData(IsInterested=1, IsNotInterested=0),
                    ByAge=TChildAgeDataV1(HalfToOneYear=1),
                )
            )
        )

        one_doc_profile = TEcomVersionedDjUserProfile(
            ProfileData=TVersionedProfileData(
                ChildrenV2=TChildrenDataV1(
                    Indicator=TIndicatorData(IsInterested=1, IsNotInterested=0),
                    ByAge=TChildAgeDataV1(NineToTwelveYears=1),
                )
            )
        )

        two_doc_profile = TEcomVersionedDjUserProfile(
            ProfileData=TVersionedProfileData(
                ChildrenV2=TChildrenDataV1(
                    Indicator=TIndicatorData(IsInterested=1, IsNotInterested=0),
                    ByAge=TChildAgeDataV1(NineToTwelveYears=0.33, FourteenToSixteenYears=0.67),
                )
            )
        )

        cls.bigb.on_request(yandexuid='1011', client='merch-machine').respond()
        cls.bigb.on_request(yandexuid='1012', client='merch-machine').respond()
        cls.bigb.on_request(yandexuid='1013', client='merch-machine').respond()

        cls.dj.on_request(yandexuid='1011', exp='fetch_user_profile_versioned').respond(
            profile_data=too_young_profile.SerializeToString(), is_binary_data=True
        )

        cls.dj.on_request(yandexuid='1012', exp='fetch_user_profile_versioned').respond(
            profile_data=one_doc_profile.SerializeToString(), is_binary_data=True
        )

        cls.dj.on_request(yandexuid='1013', exp='fetch_user_profile_versioned').respond(
            profile_data=two_doc_profile.SerializeToString(), is_binary_data=True
        )

    def test_children_age_factor(self):
        base_request = '{req}&place=prime&yandexuid={yuid}&debug=da&rearr-factors=fetch_recom_profile_for_prime=1;use_versioned_dj_profile=1'

        response = self.report.request_json(base_request.format(req='&hid=7812065', yuid=1011))
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {'debug': {'factors': {'USER_HAS_CHILDREN': '1', 'USER_CHILDREN_AGE_COINCIDE': '2'}}}
                    for i in range(3)
                ],
            },
        )

        response = self.report.request_json(base_request.format(req='&hid=7812065', yuid=1012))
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'titles': {
                            'raw': 'Блуза 10 лет',
                        },
                        'debug': {
                            'factors': {
                                'USER_HAS_CHILDREN': '1',
                                'USER_CHILDREN_AGE_COINCIDE': '3',
                            },
                        },
                    },
                    {
                        'titles': {
                            'raw': 'Блуза 14 лет',
                        },
                        'debug': {
                            'factors': {
                                'USER_HAS_CHILDREN': '1',
                                'USER_CHILDREN_AGE_COINCIDE': '2',
                            },
                        },
                    },
                    {
                        'titles': {
                            'raw': 'Блуза 10-14 лет',
                        },
                        'debug': {
                            'factors': {
                                'USER_HAS_CHILDREN': '1',
                                'USER_CHILDREN_AGE_COINCIDE': '3',
                            },
                        },
                    },
                ],
            },
        )

        response = self.report.request_json(base_request.format(req='&hid=7812065', yuid=1013))
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'titles': {
                            'raw': 'Блуза 10 лет',
                        },
                        'debug': {
                            'factors': {
                                'USER_HAS_CHILDREN': '1',
                                'USER_CHILDREN_AGE_COINCIDE': Round('2.33'),
                            },
                        },
                    },
                    {
                        'titles': {
                            'raw': 'Блуза 14 лет',
                        },
                        'debug': {
                            'factors': {
                                'USER_HAS_CHILDREN': '1',
                                'USER_CHILDREN_AGE_COINCIDE': Round('2.67'),
                            },
                        },
                    },
                    {
                        'titles': {
                            'raw': 'Блуза 10-14 лет',
                        },
                        'debug': {
                            'factors': {
                                'USER_HAS_CHILDREN': '1',
                                'USER_CHILDREN_AGE_COINCIDE': Round('2.67'),
                            },
                        },
                    },
                ],
            },
        )


if __name__ == '__main__':
    main()

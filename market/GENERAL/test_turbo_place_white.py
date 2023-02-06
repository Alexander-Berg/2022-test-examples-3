#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.matcher import NotEmptyList, EmptyList, NotEmpty, Absent, Contains
from core.testcase import TestCase, main
from core.types import (
    CategoryRestriction,
    Currency,
    Disclaimer,
    GLParam,
    GLType,
    GLValue,
    HyperCategory,
    ImagePickerData,
    Model,
    ModelDescriptionTemplates,
    NewShopRating,
    Offer,
    Opinion,
    ParameterValue,
    Region,
    RegionalRestriction,
    Shop,
    NavCategory,
)


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

        cls.index.category_restrictions += [
            CategoryRestriction(
                name='age',
                hids=[1],
                regional_restrictions=[
                    RegionalRestriction(
                        rids=[213],
                        show_offers=True,
                        disclaimers=[
                            Disclaimer(name='age', text='Для взрослых', short_text='Для взрослых'),
                        ],
                    )
                ],
            ),
        ]

        cls.index.hypertree += [
            HyperCategory(hid=1, name='Телефоны'),
        ]

        cls.index.navtree += [
            NavCategory(
                nid=300,
                children=[NavCategory(nid=301, hid=1, primary=True, name="Смартфоны")],
            )
        ]

        cls.index.gltypes += [
            GLType(
                param_id=1,
                hid=1,
                gltype=GLType.BOOL,
                cluster_filter=True,
                hidden=True,
                position=3,
            ),
            GLType(
                hid=1,
                param_id=2,
                xslname="OS",
                gltype=GLType.ENUM,
                values=[GLValue(value_id=1, text='iOS'), GLValue(value_id=2, text='Android')],
            ),
            GLType(hid=1, param_id=3, xslname="WiFi", gltype=GLType.BOOL),
            GLType(
                hid=1, param_id=4, xslname="Материал", gltype=GLType.ENUM, subtype='image_picker', cluster_filter=True
            ),
        ]

        cls.index.model_description_templates += [
            ModelDescriptionTemplates(
                hid=1,
                friendlymodel=["Телефон {WiFi:с поддержкой WiFi}"],
                micromodel="Телефон {WiFi:с поддержкой WiFi}",
            )
        ]

        cls.index.shops += [
            Shop(
                fesh=1,
                priority_region=213,
                name='Shop 1',
                currency=Currency.RUR,
                new_shop_rating=NewShopRating(
                    new_rating=3.5,
                    new_grades_count_3m=10,
                    new_grades_count=100,
                    rec_and_nonrec_pub_count=100,
                ),
            ),
        ]

        cls.index.models += [
            Model(
                hyperid=1,
                hid=1,
                title='Cisco phone',
                vendor_id=1,
                glparams=[
                    GLParam(param_id=2, value=2),
                    GLParam(param_id=3, value=1),
                ],
                parameter_value_links=[
                    ParameterValue(
                        param_id=4,
                        option_id=3,
                        picture=ImagePickerData(
                            url='//avatars.mds.yandex.net/get-mpic/466729/img_model1_201_3/orig',
                            namespace='get-mpic',
                            group_id='466729',
                            image_name='img_model1_201_3',
                        ),
                    ),
                    ParameterValue(
                        param_id=4,
                        option_id=2,
                        picture=ImagePickerData(
                            url='//avatars.mds.yandex.net/get-mpic/466729/img_model1_201_2/orig',
                            namespace='get-mpic',
                            group_id='466729',
                            image_name='img_model1_201_2',
                        ),
                    ),
                ],
                new=True,
                disclaimers_model='age',
                opinion=Opinion(
                    reviews=100500,
                    total_count=3,
                    rating=4.5,
                    rating_count=17,
                ),
            ),
            Model(hyperid=2, hid=1, title='Samsung Galaxy S10'),
            Model(hyperid=3, hid=1, title='Apple iPhone Xr'),
            Model(hyperid=4, hid=1, title='Old Mobile Phone'),
        ]

        cls.index.offers += [
            Offer(
                title='Cisco phone 1',
                price=100,
                hyperid=1,
                glparams=[GLParam(param_id=1, value=1), GLParam(param_id=4, value=2)],
                fesh=1,
                price_old=150,
                waremd5='qAwB74BChgyNFRxLsy3iyQ',
                randx=236,
            ),
            Offer(
                title='Cisco phone 2',
                price=200,
                hyperid=1,
                glparams=[GLParam(param_id=1, value=1), GLParam(param_id=4, value=3)],
                fesh=1,
                price_old=150,
                waremd5='IPK_gjsGpUmy7cLt_EPxtw',
                randx=235,
            ),
            Offer(title='Samsung Galaxy S10 offer', price=1000, hyperid=2, fesh=1),
        ]

    def test_invalid_model(self):
        response = self.report.request_json('place=turbo&hyperid=100500&rids=0')
        self.assertFragmentIn(
            response,
            {
                'knownThumbnails': NotEmptyList(),
                'result': {},
            },
        )

    def test_model(self):
        response = self.report.request_json(
            'place=turbo&hyperid=1&show-urls=external&show-models-specs=friendly&rids=0'
        )
        self.assertFragmentIn(
            response,
            {
                'knownThumbnails': NotEmptyList(),
                'result': {
                    'name': 'Cisco phone',
                    'type': 'model',
                    'id': 1,
                },
                'shops': {
                    '1': {
                        'id': 1,
                        'name': 'Shop 1',
                        'gradesCount': 100,
                        'rating': 3.5,
                        'slug': 'shop-1',
                    },
                },
                'sorts': NotEmptyList(),
                'filters': NotEmptyList(),
            },
        )

        self.assertFragmentIn(
            response,
            {
                'result': {
                    'name': 'Cisco phone',
                    'slug': 'cisco-phone',
                    'description': 'Телефон с поддержкой WiFi',
                    'type': 'model',
                    'id': 1,
                    'defaultOffer': {},
                    'prices': {
                        'currency': 'RUR',
                        'minPrice': 100,
                        'maxPrice': 200,
                    },
                    'offersCount': 1,  # потому что это от одного магазина
                    'offers': NotEmptyList(),
                    'labels': {
                        'isNew': True,
                        'customersChoice': False,
                    },
                    'specs': {
                        'friendly': [
                            'Телефон с поддержкой WiFi',
                        ],
                    },
                    'pictures': NotEmptyList(),
                    'vendor': {
                        'id': 1,
                        'name': 'VENDOR-1',
                        'slug': 'vendor-1',
                    },
                    "reviews": 100500,
                }
            },
        )

        # Проверяем, что возращается корректное описание навигационной ноды
        self.assertFragmentIn(
            response,
            {
                'result': {
                    'navnodes': {
                        'entity': 'navnode',
                        'id': 301,
                        'name': 'Смартфоны',
                        'slug': 'smartfony',
                        'isLeaf': True,
                    },
                },
            },
        )

        self.assertFragmentIn(
            response,
            {
                'defaultOffer': {
                    'delivery': {
                        'shopPriorityRegion': {
                            'entity': 'region',
                            'id': 213,
                            'name': 'Москва',
                        },
                        'shopPriorityCountry': {
                            'entity': 'region',
                            'id': 225,
                            'name': 'Россия',
                        },
                        'isPriorityRegion': False,
                        'isCountrywide': False,
                        'isAvailable': True,
                        'hasPickup': True,
                        'hasPost': False,
                        'isFree': False,
                        'isDownloadable': False,
                        'inStock': True,
                        'postAvailable': True,
                        'options': EmptyList(),
                    },
                    'wareId': 'qAwB74BChgyNFRxLsy3iyQ',
                    'shop': 1,
                    'prices': {
                        'currency': 'RUR',
                        'value': 100,
                        'isDeliveryIncluded': False,
                        'discount': {
                            'oldMin': 150,
                            'percent': 33,
                        },
                    },
                    'filters': [
                        {
                            'id': '1',
                            'type': 'boolean',
                            'name': 'GLPARAM-1',
                            'kind': 2,
                            'values': [
                                {
                                    'value': '1',
                                    'found': 1,
                                },
                            ],
                        }
                    ],
                    'pictures': NotEmptyList(),
                }
            },
        )

    def test_filters(self):
        """Проверяем что filters в выдаче содержит picker"""

        response = self.report.request_json(
            'place=turbo&hyperid=1&show-urls=external&show-models-specs=friendly&rids=0&hid=1'
        )
        # гуру-лайт фильтры 1 и 2 есть у офферов принадлежащих этой модели
        self.assertFragmentIn(
            response,
            {
                'filters': [
                    {
                        "id": "1",
                        "type": "boolean",
                        "name": "GLPARAM-1",
                        "xslname": "GLPARAM1",
                        "subType": "",
                        "kind": 2,
                        "position": 3,
                        "noffers": 2,
                        "values": [
                            {
                                "initialFound": 2,
                                "found": 2,
                                "value": "1",
                                "priceMin": {"currency": "RUR", "value": "100"},
                                "id": "1",
                            },
                            {"initialFound": 0, "found": 0, "value": "0", "id": "0"},
                        ],
                    },
                    {
                        "id": "4",
                        "type": "enum",
                        "xslname": "Материал",
                        "subType": "image_picker",
                        "kind": 2,
                        "position": 1,
                        "noffers": 2,
                        "valuesCount": 2,
                        "values": [
                            {
                                "picker": {
                                    "groupId": "466729",
                                    "entity": "photo",
                                    "imageName": "img_model1_201_2",
                                    "namespace": "get-mpic",
                                },
                                "initialFound": 1,
                                "image": "//avatars.mds.yandex.net/get-mpic/466729/img_model1_201_2/orig",
                                "found": 1,
                                "value": "VALUE-2",
                                "priceMin": {"currency": "RUR", "value": "100"},
                                "id": "2",
                            },
                            {
                                "picker": {
                                    "groupId": "466729",
                                    "entity": "photo",
                                    "imageName": "img_model1_201_3",
                                    "namespace": "get-mpic",
                                },
                                "initialFound": 1,
                                "image": "//avatars.mds.yandex.net/get-mpic/466729/img_model1_201_3/orig",
                                "found": 1,
                                "value": "VALUE-3",
                                "priceMin": {"currency": "RUR", "value": "200"},
                                "id": "3",
                            },
                        ],
                        "valuesGroups": [{"type": "all", "valuesIds": ["2", "3"]}],
                    },
                ]
            },
        )

    def test_warnings(self):
        response = self.report.request_json('place=turbo&hyperid=1&rids=213')
        self.assertFragmentIn(
            response,
            {
                'warnings': {
                    'common': [
                        {
                            'type': 'age',
                            'value': {
                                'full': 'Для взрослых',
                            },
                        }
                    ],
                },
            },
        )

    def test_default_offers_pp(self):
        response = self.report.request_json('place=turbo&hyperid=1&rids=0&pp=616&show-urls=encryptedmodel')
        self.assertFragmentIn(
            response,
            {
                'result': {
                    'name': 'Cisco phone',
                    'defaultOffer': {
                        'urls': {'encrypted': Contains("/pp=631/")},
                    },
                },
            },
        )

    def test_region(self):
        response = self.report.request_json('place=turbo&hyperid=1&rids=213')
        self.assertFragmentIn(
            response,
            {
                'region': {
                    'id': 213,
                    'name': NotEmpty(),
                    'locative': NotEmpty(),
                    'preposition': NotEmpty(),
                    'genetive': NotEmpty(),
                    'accusative': NotEmpty(),
                }
            },
        )

        for region_id in ('0', ''):
            response = self.report.request_json('place=turbo&hyperid=1&rids={}'.format(region_id))
            self.assertFragmentIn(
                response,
                {
                    'region': {
                        'id': 0,
                        'name': Absent(),
                        'locative': Absent(),
                        'preposition': Absent(),
                        'genetive': Absent(),
                        'accusative': Absent(),
                    }
                },
            )


if __name__ == '__main__':
    main()

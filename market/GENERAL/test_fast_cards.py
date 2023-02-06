import json

from market.content_storage_service.lite.core.types.model import FastCard
from market.content_storage_service.lite.core.types.hid import Hid
from market.content_storage_service.lite.core.types.nid import Nid
from market.content_storage_service.lite.core.testcase import TestCase, main
from market.content_storage_service.lite.core.types.picture import FastCardPicture
from market.content_storage_service.lite.core.types.gl_param import GLType, GLParam
from market.content_storage_service.lite.core.types.vendor import Vendor, Logo
from market.content_storage_service.lite.core.types.gumoful_template import GumofulTemplate


class T(TestCase):
    # Для тестов описания скю. Взято из репортовских
    SHORT_HTML_DESCRIPTION_BASE = ''.join(
        ['<b>Text{}</b>'.format(i) for i in range(100, 164)] + ['<b>Text{}</b>'.format(i) for i in range(200, 209)]
    )

    SHORT_HTML_DESCRIPTION = '<u>' + SHORT_HTML_DESCRIPTION_BASE + '</u>'
    FULL_HTML_DESCRIPTION = '<u>' + SHORT_HTML_DESCRIPTION_BASE + '<b>World!</b>' * 10 + '</u>'

    SHORT_PLAIN_DESCRIPTION = ''.join(['Text{} '.format(i) for i in range(100, 164)])[:-1]  # Последний пробел выкидывается
    FULL_PLAIN_DESCRIPTION = ''.join(
        [SHORT_PLAIN_DESCRIPTION, ' ', ''.join(['Text{} '.format(i) for i in range(200, 209)]), 'World! ' * 10]
    )[:-1]

    @classmethod
    def prepare(cls):
        cls.market_content_storage.with_saas_server = True
        cls.market_content_storage.with_report_server = True

        cls.index.fast_cards += [
            FastCard(
                id=20001,
                hid=10001,
                title="Cool fast card 1",
                title_no_vendor='Cool fast card 1 (no vendor)',
                pictures=[
                    FastCardPicture(key="k-20001", height=100, width=200, namespace="ns-20001", groupId=11)
                ],
                vendor_id=111,
                description='Описание быстрой модельки',
                rating=4.5,
                ratingCount=25,
                opinions=20,
                glparams=[
                    GLParam(id=21, value=[42]),
                    GLParam(id=22, value=50)
                ]
            ),
            FastCard(id=20002, hid=10001, title="Cool fast card 2", glparams=[GLParam(id=21, value=[42]), GLParam(id=22, value=33)]),
            FastCard(id=30001, hid=10002, title="Cool fast card 3", description='Описание быстрой модельки 30001'),
            FastCard(
                id=30002,
                title='Cool fast card 4 (for sku)',
                hid=10001,
                vendor_id=111,
                description=cls.FULL_HTML_DESCRIPTION,
                glparams=[
                    GLParam(id=21, value=[42]),
                    GLParam(id=22, value=30)
                ],
                pictures=[
                    FastCardPicture(key="p-30002-0", height=150, width=350, namespace="mpic", groupId=12),
                    FastCardPicture(key="p-30002-1", height=250, width=450, namespace="mpic", groupId=12)
                ]
            ),
        ]

        cls.index.hids += [
            Hid(hid=10001, name='Категория быстрых карточек', unique_name='Категория 10001', output_type='GURU', leaf=True),
            Hid(hid=10002, name='Категория быстрых карточек 10002', unique_name='Категория 10002', output_type='GURU', leaf=True)
        ]

        cls.index.nids += [
            Nid(nid=100010, hid=10001, name='Нид быстрых карточек', unique_name='Нид 100010', main=True),
        ]

        cls.index.gltypes += [
            GLType(
                hid=10001,
                id=21,
                gltype=GLType.ENUM,
                xslname="Type",
                options={41: "виртуальная", 42: "быстрая"},
                common_filter_index=1
            ),
            GLType(
                hid=10001,
                id=22,
                xslname="Length",
                gltype=GLType.NUMERIC,
                unit="см",
                max_value=150,
                min_value=26,
                precision=2,
                common_filter_index=2
            )
        ]

        cls.index.vendors += [
            Vendor(
                id=111,
                name='Проверенный быстрых карточек',
                website='111.ru',
                logo=Logo(url='//avatars.mds.yandex.net/get-mpic/1/logo_vendor_111.jpeg/orig')
            ),
        ]

        cls.index.gumoful_templates += [
            GumofulTemplate(
                hid=10001,
                micromodel="Карточка {Type}, а длиной {Length} см",
                friendlymodel=[
                    "Карточка длиной {Length} см",
                ],
                model=[
                    (
                        "Характеристики",
                        {
                            "Тип": "{Type}",
                            "Длина": "{Length} см",
                        },
                    ),
                ],
                seo="{return $Type; #exec}",
            ),
        ]

    def test_fastcard_info(self):
        '''
        Проверяем поля быстрых карточек
        '''
        request = {
            "model_ids": [20001, 20002, 30001],
            "market_skus": [30001, 30002]
        }

        response = self.market_content_storage.request_json(
            'card_info',
            method='GET',
            body=json.dumps(request)
        )

        # Проверяем, что возвращаются и модельки и скю карточки
        # + проверяем поля модельки
        self.assertFragmentIn(
            response,
            {
                "models": [
                    {
                        "id": 20001,
                        "title": {
                            "raw": "Cool fast card 1"
                        },
                        "titleWithoutVendor": {
                            "raw": "Cool fast card 1 (no vendor)"
                        },
                        "isVirtual": True,
                        "modelCreator": "market",
                        "description": "Карточка быстрая, а длиной 50 см",
                        "categories": [
                            {
                                "id": 10001,
                                "name": "Категория быстрых карточек",
                                "fullName": "Категория 10001",
                                "isLeaf": True,
                                "nid": 100010,
                                "type": "GURU"
                            }
                        ],
                        "navnodes": [
                            {
                                "id": 100010,
                                "name": "Нид быстрых карточек",
                                "fullName": "Нид 100010",
                            }
                        ],
                        "pictures": [{
                            "original": {
                                "groupId": 11,
                                "height": 100,
                                "key": "k-20001",
                                "namespace": "ns-20001",
                                "width": 200
                            }
                        }],
                        "rating": 4.5,
                        "preciseRating": 4.5,
                        "ratingCount": 25,
                        "opinions": 20,
                        "filters": [
                            {
                                "id": 21,
                                "name": "ENUM_10001_21",
                                "xslname": "Type",
                                "position": 1,
                                "type": "enum",
                                "values": [
                                    {
                                        "id": 42,
                                        "value": "быстрая",
                                        "found": 1,
                                        "initialFound": 1,
                                    }
                                ],
                                "valuesCount": 1,
                                "valuesGroups": [
                                    {
                                        "type": "all",
                                        "valuesIds": [
                                            42
                                        ]
                                    }
                                ]
                            },
                            {
                                "id": 22,
                                "name": "NUMERIC_10001_22",
                                "xslname": "Length",
                                "position": 2,
                                "precision": 2,
                                "type": "number",
                                "unit": "см",
                                "values": [
                                    {
                                        "initialMax": 50,
                                        "initialMin": 50,
                                        "max": 50,
                                        "min": 50
                                    }
                                ]
                            }
                        ],
                        "lingua": {
                            "accusative": "  быстрая-accusative  ",
                            "dative": "быстрая-dative ",
                            "genitive": " быстрая-genitive  ",
                            "nominative": "  быстрая-nominative "
                        },
                        "vendor": {
                            "id": 111,
                            "name": "Проверенный быстрых карточек",
                            "logo": {
                                "namespace": "mpic",
                                "groupId": 1,
                                "key": "logo_vendor_111.jpeg"
                            },
                            "website": "111.ru"
                        },
                        "specs": {
                            "friendly": [
                                "Карточка длиной 50 см"
                            ],
                            "friendlyext": [
                                {
                                    "type" : "spec",
                                    "value": "Карточка длиной 50 см",
                                    "usedParams": [
                                        22
                                    ],
                                }
                            ],
                            "full": [
                                {
                                    "groupName": "Характеристики",
                                    "groupSpecs": [
                                        {
                                            "name": "Тип",
                                            "value": "быстрая",
                                            "desc": "Тип parameter description",
                                            "usedParams": [
                                                {
                                                    "id": 21,
                                                    "name": "ENUM_10001_21"
                                                }
                                            ],
                                        },
                                        {
                                            "name": "Длина",
                                            "value": "50 см",
                                            "desc": "Длина parameter description",
                                            "usedParams": [
                                                {
                                                    "id": 22,
                                                    "name": "NUMERIC_10001_22"
                                                }
                                            ],
                                        }
                                    ]
                                }
                            ]
                        }
                    },
                    {
                        "id": 20002,
                        "title": {"raw": "Cool fast card 2"},
                        "modelCreator": "market",
                        "filters": [
                            {
                                "id": 21,
                                "values": [{"id": 42}],
                            },
                            {
                                "id": 22,
                                "type": "number",
                                "unit": "см",
                                "values": [
                                    {
                                        "initialMax": 33,
                                        "initialMin": 33,
                                        "max": 33,
                                        "min": 33
                                    }
                                ],
                            }
                        ],
                    },
                    {
                        "id": 30001,
                        "title": {"raw": "Cool fast card 3"},
                        "modelCreator": "market",
                        # Если у категории нет микроописания, до берем сырое из карточки
                        "description": 'Описание быстрой модельки 30001'
                    }
                ],
                "mskus": [
                    {
                        "id": 30001,
                        "title": {"raw": "Cool fast card 3"},
                        "marketSkuCreator": "virtual"
                    },
                    {
                        "id": 30002,
                        "title": {"raw": "Cool fast card 4 (for sku)"},
                        "marketSkuCreator": "virtual"
                    },
                ]
            },
            allow_different_len=False
        )

        # Проверяем поля быстрой скю
        self.assertFragmentIn(
            response,
            {
                "mskus": [
                    {
                        "id": 30001,
                        "title": {"raw": "Cool fast card 3"}
                    },
                    {
                        "id": 30002,
                        "title": {
                            "raw": "Cool fast card 4 (for sku)"
                        },
                        "categories": [
                            {
                                "id": 10001,
                                "name": "Категория быстрых карточек",
                                "fullName": "Категория 10001",
                                "nid": 100010,
                                "type": "GURU"
                            }
                        ],
                        "navnodes": [
                            {
                                "id": 100010,
                                "name": "Нид быстрых карточек",
                                "fullName": "Нид 100010"
                            }
                        ],
                        "description": self.SHORT_PLAIN_DESCRIPTION,
                        "formattedDescription": {
                            "fullHtml": self.FULL_HTML_DESCRIPTION,
                            "fullPlain": self.FULL_PLAIN_DESCRIPTION,
                            "shortHtml": self.SHORT_HTML_DESCRIPTION,
                            "shortPlain": self.SHORT_PLAIN_DESCRIPTION
                        },
                        "modelId": 30002,
                        "pictures": [
                            {
                                "original": {
                                    "groupId": 12,
                                    "key": "p-30002-0",
                                    "namespace": "mpic",
                                    "height": 150,
                                    "width": 350
                                }
                            },
                            {
                                "original": {
                                    "groupId": 12,
                                    "key": "p-30002-1",
                                    "namespace": "mpic",
                                    "height": 250,
                                    "width": 450
                                }
                            }
                        ],
                        "vendor": {
                            "id": 111,
                            "name": "Проверенный быстрых карточек",
                            "logo": {
                                "groupId": 1,
                                "key": "logo_vendor_111.jpeg",
                                "namespace": "mpic"
                            },
                            "website": "111.ru"
                        },
                        "specs": {
                            "friendly": [
                                "Карточка длиной 30 см"
                            ],
                            "friendlyext": [
                                {
                                    "type" : "spec",
                                    "value": "Карточка длиной 30 см",
                                    "usedParams": [
                                        22
                                    ],
                                }
                            ],
                            "full": [
                                {
                                    "groupName": "Характеристики",
                                    "groupSpecs": [
                                        {
                                            "name": "Тип",
                                            "value": "быстрая",
                                            "desc": "Тип parameter description",
                                            "usedParams": [
                                                {
                                                    "id": 21,
                                                    "name": "ENUM_10001_21"
                                                }
                                            ],
                                        },
                                        {
                                            "name": "Длина",
                                            "value": "30 см",
                                            "desc": "Длина parameter description",
                                            "usedParams": [
                                                {
                                                    "id": 22,
                                                    "name": "NUMERIC_10001_22"
                                                }
                                            ],
                                        }
                                    ]
                                }
                            ]
                        }
                    },
                ]
            },
            allow_different_len=False
        )

    def test_fastcard_combinations(self):
        '''
        Проверяем разные типы запросов
        '''

        # Только моделька
        request = {
            "model_ids": [20001],
            "market_skus": []
        }
        response = self.market_content_storage.request_json('card_info', method='GET', body=json.dumps(request))
        self.assertFragmentIn(
            response,
            {
                'models': [{'id': 20001}],
                'mskus': []
            },
            allow_different_len=False
        )

        # Только скю
        request = {
            "model_ids": [],
            "market_skus": [20001]
        }
        response = self.market_content_storage.request_json('card_info', method='GET', body=json.dumps(request))
        self.assertFragmentIn(
            response,
            {
                'models': [],
                'mskus': [{'id': 20001}]
            },
            allow_different_len=False
        )

        # Модель + скю
        request = {
            "model_ids": [20001],
            "market_skus": [20001]
        }
        response = self.market_content_storage.request_json('card_info', method='GET', body=json.dumps(request))
        self.assertFragmentIn(
            response,
            {
                'models': [{'id': 20001}],
                'mskus': [{'id': 20001}]
            },
            allow_different_len=False
        )

        # Несуществующая быстрая моделька
        request = {
            "model_ids": [20011],
            "market_skus": [20011]
        }
        response = self.market_content_storage.request_json('card_info', method='GET', body=json.dumps(request))
        self.assertFragmentIn(
            response,
            {
                'models': [],
                'mskus': []
            },
            allow_different_len=False
        )


if __name__ == '__main__':
    main()

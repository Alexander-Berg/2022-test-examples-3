import json

from market.content_storage_service.lite.core.types.gl_param import GLType, GLParam, GLParamFromSku
from market.content_storage_service.lite.core.types.model import Model, Sku
from market.content_storage_service.lite.core.types.vendor import Vendor, Logo
from market.content_storage_service.lite.core.types.picture import Picture
from market.content_storage_service.lite.core.types.gumoful_template import GumofulTemplate
from market.content_storage_service.lite.core.types.hid import Hid
from market.content_storage_service.lite.core.types.nid import Nid
from market.content_storage_service.lite.core.testcase import TestCase, main
from market.pylibrary.lite.matcher import NotEmpty, NoKey
import market.proto.content.mbo.CsGumoful_pb2 as CsGumoful


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.market_content_storage.with_saas_server = True
        cls.saas = T.market_content_storage.saas_server.connect()

        # Models creation example:
        cls.index.models += [
            Model(hyperid=10001, hid=20001, vendor_id=30001, glparams=[GLParam(id=21, value=[41]), GLParam(id=22, value=33)]),
            Model(hyperid=650905184, rating=3.66666, ratingCount=63, opinions=36),
        ]

        # Sku creation example:
        cls.index.skus += [
            Sku(sku_id=90001, model_id=10001),
        ]

    def test_yt_saas(self):
        request = {
            "model_ids": [10001]
        }

        response = self.market_content_storage.request_json(
            'card_info',
            method='GET',
            body=json.dumps(request)
        )
        self.assertFragmentIn(response, {
            "models": [
                {
                    "id": 10001,
                    "vendor": {
                        "id": 30001,
                        "name": "Name-30001",
                        "website": "vendor-30001.su"
                    },
                    "categories": [
                        {
                            "fullName": "hid-20001-u",
                            "id": 20001,
                            "isLeaf": False,
                            "name": "hid-20001",
                            "type": "GURU"
                        }
                    ],
                    "filters": [
                        {
                            "id": 21,
                            "values": [{"id": 41}],
                        },
                        {
                            "id": 22,
                            "type": "number",
                            "unit": "????",
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
                }
            ],
        })

    def test_sku(self):
        request = {
            "market_skus": [90001]
        }

        response = self.market_content_storage.request_json(
            'card_info',
            method='GET',
            body=json.dumps(request)
        )

        self.assertFragmentIn(response, {
            "mskus": [
                {
                    "id": 90001
                }
            ],
        })

    def test_pers(self):
        request = {
            "model_ids": [650905184]
        }

        response = self.market_content_storage.request_json(
            'card_info',
            method='GET',
            body=json.dumps(request)
        )

        self.assertFragmentIn(response, {
            "models": [
                {
                    "id": 650905184,
                    "opinions": 36,
                    "preciseRating": 3.670000076,
                    "rating": 3.5,
                    "ratingCount": 63
                }
            ],
        })

    @classmethod
    def prepare_data(cls):
        # Vendors creation example:
        cls.index.vendors += [
            Vendor(id=30001, name="Name-30001", website="vendor-30001.su")
        ]
        cls.index.nids += [
            Nid(nid=100500, hid=200500, name="nid-100500", unique_name="nid-100500-u", main=True),
            Nid(nid=100501, hid=200501, name="nid-100501", unique_name="nid-100501-u", main=False)
        ]
        cls.index.hids += [
            Hid(hid=300500, name="hid-300500", unique_name="hid-300500-u", output_type="GURULIGHT", leaf=True),
            Hid(hid=20001, name="hid-20001", unique_name="hid-20001-u", output_type="GURU", leaf=False)
        ]

        cls.index.gltypes += [
            GLType(
                hid=20001,
                id=21,
                gltype=GLType.ENUM,
                options={41: "Name-41", 42: "Name-42"},
                common_filter_index=1
            ),
            GLType(
                hid=20001,
                id=22,
                gltype=GLType.NUMERIC,
                unit="????",
                max_value=150,
                min_value=26,
                precision=2,
                common_filter_index=2
            )
        ]

    # ?????? ???????????? ???????????????? ??????. ?????????? ???? ????????????????????????
    SHORT_HTML_DESCRIPTION_BASE = ''.join(
        ['<b>Text{}</b>'.format(i) for i in range(100, 164)] + ['<b>Text{}</b>'.format(i) for i in range(200, 209)]
    )

    SHORT_HTML_DESCRIPTION = '<u>' + SHORT_HTML_DESCRIPTION_BASE + '</u>'
    FULL_HTML_DESCRIPTION = '<u>' + SHORT_HTML_DESCRIPTION_BASE + '<b>World!</b>' * 10 + '</u>'

    SHORT_PLAIN_DESCRIPTION = ''.join(['Text{} '.format(i) for i in range(100, 164)])[:-1]  # ?????????????????? ???????????? ????????????????????????
    FULL_PLAIN_DESCRIPTION = ''.join(
        [SHORT_PLAIN_DESCRIPTION, ' ', ''.join(['Text{} '.format(i) for i in range(200, 209)]), 'World! ' * 10]
    )[:-1]

    @classmethod
    def prepare_cards(cls):
        cls.index.vendors += [
            Vendor(
                id=100500,
                name='?????????????????????? ??????????????????????????',
                website='100500.ru',
                logo=Logo(url='//avatars.mds.yandex.net/get-mpic/1/logo_100500.jpeg/orig')
            ),
        ]

        cls.index.hids += [
            Hid(hid=1, name='????????????', unique_name='???????????? (unique)', output_type='GURU', leaf=True)
        ]

        cls.index.nids += [
            Nid(nid=101, hid=1, name='???????????? (??????)', unique_name='???????????? (unique ??????)', main=True),
        ]

        cls.index.gltypes += [
            GLType(
                hid=1,
                id=90,
                name='????????????????',
                xslname="Material",
                gltype=GLType.ENUM,
                options={
                    901: "????????????????",
                    902: "??????????????????????",
                    903: "?????????????????? (???? ??????)",
                    904: "???????????????? (???? ??????)"
                },
                common_filter_index=1,
                is_gurulight=True,
                kind=1
            ),
            GLType(
                hid=1,
                id=91,
                name='??????????',
                xslname="Length",
                gltype=GLType.NUMERIC,
                unit="????",
                max_value=100,
                min_value=10,
                precision=2,
                common_filter_index=2,
                kind=2
            ),
            GLType(
                hid=1,
                id=92,
                name='???????????? ??????????????',
                xslname="ForMonsters",
                gltype=GLType.BOOL,
                common_filter_index=3,
                kind=2
            ),
            GLType(
                hid=1,
                id=93,
                xslname="Width",
                gltype=GLType.NUMERIC_ENUM,
                options={101: "10", 111: "11", 222: "22"},
                unit="????",
                max_value=100,
                min_value=0,
                precision=2,
                common_filter_index=4
            ),
        ]

        cls.index.gumoful_templates += [
            GumofulTemplate(
                hid=1,
                micromodel="{Material} ??????, {Length} ????, {ForMonsters:???????????? ????????????????}",
                friendlymodel=[
                    "?????? {ForMonsters#ifnz}{ForMonsters:???????????? ??????????????}{#endif}",
                    "?????????????? {Width} ????"
                ],
                model=[
                    (
                        "????????????????????????????",
                        {
                            "????????????????": "{Material}",
                            "??????????": "{Length} ????",
                            "???????????? ??????????????": "{ForMonsters}",
                            "????????????": "{Width} ????",
                        },
                    ),
                ],
                seo="{return $Material; #exec}",
            ),
        ]

        cls.index.models += [
            Model(
                hyperid=1252,
                hid=1,
                vendor_id=100500,
                title='??????',
                title_no_vendor='?????? (?????? ??????????????)',
                description='????????????????: Zireael',
                full_description='???????????????? (????????????): Zireael',
                pictures=[
                    Picture(url='//avatars.mds.yandex.net/get-mpic/100/picture_1.jpeg/orig', height=100, width=200),
                    Picture(url='//avatars.mds.yandex.net/get-mpic/200/picture_2.jpeg/orig', height=200, width=100)
                ],
                glparams=[
                    GLParam(id=90, value=[901, 902]),
                    GLParam(id=91, value=55.556),
                    GLParam(id=92, value=1),
                    GLParam(id=93, value=101)
                ],
                additional_params=[
                    GLParamFromSku(sku_id=91, params=[GLParam(id=90, value=[903])]),
                    GLParamFromSku(sku_id=92, params=[GLParam(id=90, value=[903, 904])])
                ],
                rating=4.5,
                ratingCount=101,
                opinions=50
            ),
            Model(hyperid=100, hid=1),
            Model(hyperid=200, hid=1),
            Model(hyperid=2111, hid=1)
        ]

        cls.index.skus += [
            Sku(
                sku_id=12520,
                model_id=1252,
                title='?????? ??????????????',
                hid=1,
                vendor_id=100500,
                description=cls.FULL_HTML_DESCRIPTION,
                glparams=[
                    GLParam(id=90, value=[901]),
                    GLParam(id=91, value=48.1),
                    GLParam(id=92, value=0),
                    GLParam(id=93, value=111)
                ],
                pictures=[
                    Picture(url='//avatars.mds.yandex.net/get-mpic/100/picture_sku_1.jpeg/orig', height=50, width=100),
                    Picture(url='//avatars.mds.yandex.net/get-mpic/200/picture_sku_2.jpeg/orig', height=100, width=50)
                ]
            ),
            Sku(sku_id=1001, model_id=100, hid=1),
            Sku(sku_id=2001, model_id=200, hid=1),
            # ?????? ?? ???????????????????? ??????, ?? ????????????
            Sku(sku_id=2111, model_id=2111, hid=1),
        ]

    def test_model_all_fields(self):
        '''
        ???????????????? ????????????

        ?????????? ??????????????????:
        1) ????????????
        2) ??????????
        3) ?????????? ?????? ??????????????
        4) ???????????????? + FullDescription
        5) ??????????????????
        6) ?????? ??????????????????
        7) ????????????????
        8) ???????????????????? ???????????????? ???? ????????????????????
        9) IsVirtual (???????????? ??????????????????????????)
        10) ???????????? ????????????
        11) ?????????? + Lingua
        '''

        request = {
            "model_ids": [1252]
        }
        response = self.market_content_storage.request_json(
            'card_info',
            method='GET',
            body=json.dumps(request)
        )

        self.assertFragmentIn(response, {
            "models": [
                {
                    "id": 1252,
                    "title": {
                        "raw": "??????",
                        "highlighted": []
                    },
                    "titleWithoutVendor": {
                        "raw": "?????? (?????? ??????????????)",
                        "highlighted": [],
                    },
                    "categories": [
                        {
                            "id": 1,
                            "name": "????????????",
                            "fullName": "???????????? (unique)",
                            "nid": 101,
                            "isLeaf": True,
                            "type": "GURU"
                        }
                    ],
                    "navnodes": [
                        {
                            "id": 101,
                            "name": "???????????? (??????)",
                            "fullName": "???????????? (unique ??????)",
                            "isLeaf": False
                        }
                    ],
                    "description": "????????????????, ??????????????????????, ?????????????????? (???? ??????), ???????????????? (???? ??????) ??????, 55.556 ????, ???????????? ????????????????",  # micro model template
                    "fullDescription": "???????????????? (????????????): Zireael",
                    "vendor": {
                        "id": 100500,
                        "name": "?????????????????????? ??????????????????????????",
                        "website": "100500.ru",
                        "logo": {
                            "groupId": 1,
                            "key": "logo_100500.jpeg",
                            "namespace": "mpic"
                        },
                    },
                    "lingua": {
                        "accusative": "  ????????????????, ??????????????????????, ?????????????????? (???? ??????), ???????????????? (???? ??????)-accusative  ",
                        "dative": "????????????????, ??????????????????????, ?????????????????? (???? ??????), ???????????????? (???? ??????)-dative ",
                        "genitive": " ????????????????, ??????????????????????, ?????????????????? (???? ??????), ???????????????? (???? ??????)-genitive  ",
                        "nominative": "  ????????????????, ??????????????????????, ?????????????????? (???? ??????), ???????????????? (???? ??????)-nominative "
                    },
                    "opinions": 50,
                    "preciseRating": 4.5,
                    "rating": 4.5,
                    "ratingCount": 101,
                    "pictures": [
                        {
                            "original": {
                                "groupId": 100,
                                "height": 100,
                                "key": "picture_1.jpeg",
                                "namespace": "mpic",
                                "width": 200
                            },
                        },
                        {
                            "original": {
                                "groupId": 200,
                                "height": 200,
                                "key": "picture_2.jpeg",
                                "namespace": "mpic",
                                "width": 100
                            },
                        }
                    ],
                    "specs": {
                        "friendly": [
                            "?????? ???????????? ??????????????",
                            "?????????????? 10 ????"
                        ],
                        "friendlyext": [
                            {
                                "type" : "spec",
                                "value": "?????? ???????????? ??????????????",
                                "usedParams": [
                                    92
                                ],
                                "usedParamsWithValues": [
                                    {
                                        "id": 92,
                                        "values": [
                                            {
                                                "isFilterable": True,
                                                "value": "1"
                                            }
                                        ]
                                    }
                                ],
                            },
                            {
                                "type" : "spec",
                                "value": "?????????????? 10 ????",
                                "usedParams": [
                                    93
                                ],
                                "usedParamsWithValues": [
                                    {
                                        "id": 93,
                                        "values": [
                                            {
                                                "isFilterable": True,
                                                "value": "101"
                                            }
                                        ]
                                    }
                                ]
                            }
                        ],
                        "full": [
                            {
                                "groupName": "????????????????????????????",
                                "groupSpecs": [
                                    {
                                        "name": "????????????????",
                                        "value": "????????????????, ??????????????????????, ?????????????????? (???? ??????), ???????????????? (???? ??????)",
                                        "desc": "???????????????? parameter description",
                                        "mainProperty": False,  # TODO: ??????????????, ???? ?????????????? ?????? ???????????? ??????????????????
                                        "usedParams": [
                                            {
                                                "id": 90,
                                                "name": "????????????????"
                                            }
                                        ],
                                        "usedParamsWithValues": [
                                            {
                                                "id": 90,
                                                "name": "????????????????",
                                                "values": [
                                                    {
                                                        "isFilterable": True,
                                                        "value": "901"
                                                    },
                                                    {
                                                        "isFilterable": True,
                                                        "value": "902"
                                                    },
                                                    {
                                                        "isFilterable": True,
                                                        "value": "903"
                                                    },
                                                    {
                                                        "isFilterable": True,
                                                        "value": "904"
                                                    }
                                                ]
                                            }
                                        ],
                                    },
                                    {
                                        "name": "??????????",
                                        "value": "55.556 ????",
                                        "desc": "?????????? parameter description",
                                        "mainProperty": False,
                                        "usedParams": [
                                            {
                                                "id": 91,
                                                "name": "??????????"
                                            }
                                        ],
                                        "usedParamsWithValues": [
                                            {
                                                "id": 91,
                                                "name": "??????????",
                                                "values": [
                                                    {
                                                        "isFilterable": True,
                                                        "value": "55.556"
                                                    }
                                                ]
                                            }
                                        ],
                                    },
                                    {
                                        "name": "???????????? ??????????????",
                                        "value": "????????",
                                        "desc": "???????????? ?????????????? parameter description",
                                        "mainProperty": False,
                                        "usedParams": [
                                            {
                                                "id": 92,
                                                "name": "???????????? ??????????????"
                                            }
                                        ],
                                        "usedParamsWithValues": [
                                            {
                                                "id": 92,
                                                "name": "???????????? ??????????????",
                                                "values": [
                                                    {
                                                        "isFilterable": True,
                                                        "value": "1"
                                                    }
                                                ]
                                            }
                                        ],
                                    },
                                    {
                                        "name": "????????????",
                                        "value": "10 ????",
                                        "desc": "???????????? parameter description",
                                        "usedParams": [
                                            {
                                                "id": 93,
                                                "name": "NUMERIC_ENUM_1_93",
                                            }
                                        ],
                                        "usedParamsWithValues": [
                                            {
                                                "id": 93,
                                                "name": "NUMERIC_ENUM_1_93",
                                                "values": [
                                                    {
                                                        "isFilterable": True,
                                                        "value": "101"
                                                    }
                                                ]
                                            }
                                        ],
                                    }
                                ]
                            }
                        ]
                    },
                }
            ],
        })

        # ?????????????? ???????????????? ????????????????, ???? ?? ?????? ?????????? ??????????????
        self.assertFragmentIn(
            response,
            {
                "models": [
                    {
                        "id": 1252,
                        "filters": [
                            {
                                "id": 90,
                                "type": "enum",
                                "isGuruLight": True,
                                "kind": 1,
                                "name": "????????????????",
                                "xslname": "Material",
                                "position": 1,
                                "values": [
                                    {
                                        "found": 1,
                                        "id": 904,
                                        "initialFound": 1,
                                        "popularity": 1,
                                        "value": "???????????????? (???? ??????)"
                                    },
                                    {
                                        "found": 1,
                                        "id": 903,
                                        "initialFound": 1,
                                        "popularity": 1,
                                        "value": "?????????????????? (???? ??????)"
                                    },
                                    {
                                        "found": 1,
                                        "id": 902,
                                        "initialFound": 1,
                                        "popularity": 1,
                                        "value": "??????????????????????"
                                    },
                                    {
                                        "found": 1,
                                        "id": 901,
                                        "initialFound": 1,
                                        "popularity": 1,
                                        "value": "????????????????"
                                    }
                                ],
                                "valuesCount": 4,
                                "valuesGroups": [
                                    {
                                        "type": "all",
                                        "valuesIds": [
                                            904,
                                            903,
                                            902,
                                            901
                                        ]
                                    }
                                ],
                            },
                            {
                                "id": 91,
                                "isGuruLight": False,
                                "kind": 2,
                                "name": "??????????",
                                "position": 2,
                                "precision": 2,
                                "type": "number",
                                "unit": "????",
                                "values": [
                                    {
                                        "initialMax": 55.556,
                                        "initialMin": 55.556,
                                        "max": 55.556,
                                        "min": 55.556
                                    }
                                ],
                                "valuesGroups": [],
                                "xslname": "Length"
                            },
                            {
                                "id": 92,
                                "isGuruLight": False,
                                "kind": 2,
                                "name": "???????????? ??????????????",
                                "position": 3,
                                "type": "boolean",
                                "values": [
                                    {
                                        "found": 1,
                                        "id": 1,
                                        "initialFound": 1,
                                        "value": "1"
                                    },
                                    {
                                        "found": 0,
                                        "id": 0,
                                        "initialFound": 0,
                                        "value": "0"
                                    }
                                ],
                                "valuesGroups": [],
                                "xslname": "ForMonsters"
                            },
                            {
                                "id": 93,
                                "isGuruLight": False,
                                "kind": 1,
                                "name": "NUMERIC_ENUM_1_93",
                                "xslname": "Width",
                                "position": 4,
                                "precision": 2,
                                "type": "number",
                                "unit": "????",
                                "values": [
                                    {
                                    "initialMax": 10,
                                    "initialMin": 10,
                                    "max": 10,
                                    "min": 10
                                    }
                                ],
                            }
                        ],
                    }
                ]
            },
            allow_different_len=False,
            preserve_order=True
        )

    def test_sku_all_fields(self):
        '''
        ???????????????? ??????

        ?????????? ??????????????????:
        1) ????????????
        2) ??????????
        3) ????????????????
        4) ??????????????????
        5) ?????? ??????????????????
        6) ????????????????
        7) ?????????? ?? ??????????????????
        8) FormattedDescription
        9)?????????? + Lingua
        '''
        request = {
            "market_skus": [12520]
        }
        response = self.market_content_storage.request_json(
            'card_info',
            method='GET',
            body=json.dumps(request)
        )

        self.assertFragmentIn(
            response,
            {
                "mskus": [
                    {
                        "id": 12520,
                        "modelId": 1252,
                        "title": {
                            "highlighted": [],
                            "raw": "?????? ??????????????"
                        },
                        "vendor": {
                            "id": 100500,
                            "name": "?????????????????????? ??????????????????????????",
                            "website": "100500.ru",
                            "logo": {
                                "key": "logo_100500.jpeg",
                                "namespace": "mpic",
                                "groupId": 1
                            }
                        },
                        "categories": [
                            {
                                "id": 1,
                                "name": "????????????",
                                "fullName": "???????????? (unique)",
                                "isLeaf": True,
                                "nid": 101,
                                "type": "GURU"
                            }
                        ],
                        "navnodes": [
                            {
                                "id": 101,
                                "name": "???????????? (??????)",
                                "fullName": "???????????? (unique ??????)",
                            }
                        ],
                        "description": self.SHORT_PLAIN_DESCRIPTION,
                        "formattedDescription": {
                            "fullHtml": self.FULL_HTML_DESCRIPTION,
                            "fullPlain": self.FULL_PLAIN_DESCRIPTION,
                            "shortHtml": self.SHORT_HTML_DESCRIPTION,
                            "shortPlain": self.SHORT_PLAIN_DESCRIPTION
                        },
                        "pictures": [
                            {
                                "original": {
                                    "key": "picture_sku_1.jpeg",
                                    "namespace": "mpic",
                                    "groupId": 100,
                                    "height": 50,
                                    "width": 100
                                },
                            },
                            {
                                "original": {
                                    "key": "picture_sku_2.jpeg",
                                    "namespace": "mpic",
                                    "groupId": 200,
                                    "height": 100,
                                    "width": 50
                                },
                            }
                        ],
                        "specs": {
                            "friendly": [
                                "??????",
                                "?????????????? 11 ????"
                            ],
                            "friendlyext": [
                                {
                                    "type" : "spec",
                                    "value": "??????",
                                    "usedParams": [
                                        92
                                    ],
                                    "usedParamsWithValues": [
                                        {
                                            "id": 92,
                                            "values": [
                                                {
                                                    "isFilterable": True,
                                                    "value": "0"
                                                }
                                            ]
                                        }
                                    ],
                                },
                                {
                                    "type" : "spec",
                                    "value": "?????????????? 11 ????",
                                    "usedParams": [
                                        93
                                    ],
                                    "usedParamsWithValues": [
                                        {
                                            "id": 93,
                                            "values": [
                                                {
                                                    "isFilterable": True,
                                                    "value": "111"
                                                }
                                            ]
                                        }
                                    ]
                                }
                            ],
                            "full": [
                                {
                                    "groupName": "????????????????????????????",
                                    "groupSpecs": [
                                        {
                                            "name": "????????????????",
                                            "desc": "???????????????? parameter description",
                                            "mainProperty": False,
                                            "usedParams": [
                                                {
                                                    "id": 90,
                                                    "name": "????????????????"
                                                }
                                            ],
                                            "usedParamsWithValues": [
                                                {
                                                    "id": 90,
                                                    "name": "????????????????",
                                                    "values": [
                                                        {
                                                            "isFilterable": True,
                                                            "value": "901"
                                                        }
                                                    ]
                                                }
                                            ],
                                            "value": "????????????????"
                                        },
                                        {
                                            "name": "??????????",
                                            "desc": "?????????? parameter description",
                                            "mainProperty": False,
                                            "usedParams": [
                                                {
                                                    "id": 91,
                                                    "name": "??????????"
                                                }
                                            ],
                                            "usedParamsWithValues": [
                                                {
                                                    "id": 91,
                                                    "name": "??????????",
                                                    "values": [
                                                        {
                                                            "isFilterable": True,
                                                            "value": "48.1"
                                                        }
                                                    ]
                                                }
                                            ],
                                            "value": "48.1 ????"
                                        },
                                        {
                                            "name": "???????????? ??????????????",
                                            "desc": "???????????? ?????????????? parameter description",
                                            "mainProperty": False,
                                            "usedParams": [
                                                {
                                                    "id": 92,
                                                    "name": "???????????? ??????????????"
                                                }
                                            ],
                                            "usedParamsWithValues": [
                                                {
                                                    "id": 92,
                                                    "name": "???????????? ??????????????",
                                                    "values": [
                                                        {
                                                            "isFilterable": True,
                                                            "value": "0"
                                                        }
                                                    ]
                                                }
                                            ],
                                            "value": "??????"
                                        },
                                        {
                                            "name": "????????????",
                                            "desc": "???????????? parameter description",
                                            "value": "11 ????",
                                            "usedParams": [
                                                {
                                                    "id": 93,
                                                    "name": "NUMERIC_ENUM_1_93",
                                                }
                                            ],
                                            "usedParamsWithValues": [
                                                {
                                                    "id": 93,
                                                    "name": "NUMERIC_ENUM_1_93",
                                                    "values": [
                                                        {
                                                            "isFilterable": True,
                                                            "value": "111"
                                                        }
                                                    ]
                                                }
                                            ]
                                        }
                                    ]
                                }
                            ]
                        },
                    }
                ]
            },
            allow_different_len=False
        )

    def test_card_combinations(self):
        '''
        ????????????????, ?????? ???????????????? ?????????????? ?? ?????????????? ???????????????????????? ????????????????????
        '''

        # ???????????? + ????????
        request = {
            "model_ids": [100],
            "market_skus": [1001]
        }
        response = self.market_content_storage.request_json(
            'card_info',
            method='GET',
            body=json.dumps(request)
        )

        self.assertFragmentIn(response, {
            'models': [{'id': 100}],
            'mskus': [{'id': 1001}]
        }, allow_different_len=False)

        # ?????????????????? ??????????????
        request = {
            "model_ids": [100, 200],
            "market_skus": []
        }
        response = self.market_content_storage.request_json(
            'card_info',
            method='GET',
            body=json.dumps(request)
        )

        self.assertFragmentIn(response, {
            'models': [{'id': 100}, {'id': 200}],
            'mskus': []
        }, allow_different_len=False)

        # ?????????????????? ??????
        request = {
            "model_ids": [],
            "market_skus": [1001, 2001]
        }
        response = self.market_content_storage.request_json(
            'card_info',
            method='GET',
            body=json.dumps(request)
        )

        self.assertFragmentIn(response, {
            'models': [],
            'mskus': [{'id': 1001}, {'id': 2001}]
        }, allow_different_len=False)

        # ?????? ????????????
        request = {
            "model_ids": [100, 200],
            "market_skus": [1001, 2001]
        }
        response = self.market_content_storage.request_json(
            'card_info',
            method='GET',
            body=json.dumps(request)
        )

        self.assertFragmentIn(response, {
            'models': [{'id': 100}, {'id': 200}],
            'mskus': [{'id': 1001}, {'id': 2001}]
        }, allow_different_len=False)

    @classmethod
    def prepare_rendered_templates(cls):
        cls.index.hids += [
            Hid(hid=2, name='????????????????????', unique_name='???????????????????? (unique)', output_type='GURU', leaf=True)
        ]

        cls.index.nids += [
            Nid(nid=201, hid=2, name='???????????????????? (??????)', unique_name='???????????????????? (unique ??????)', main=True),
        ]

        cls.index.gltypes += [
            GLType(
                hid=2,
                id=300,
                name='????????????????',
                xslname="Material",
                gltype=GLType.ENUM,
                options={
                    3001: "????????????",
                    3002: "????????????"
                },
                common_filter_index=1,
                is_gurulight=True,
                kind=1
            ),
            GLType(
                hid=2,
                id=301,
                name='???????????????????????? ????????????????',
                xslname="Max-Speed",
                gltype=GLType.NUMERIC,
                unit="????/??",
                max_value=200,
                min_value=0,
                precision=2,
                common_filter_index=2,
                kind=2
            ),
            GLType(
                hid=2,
                id=302,
                name='?? ????????????????????',
                xslname="With-Gears",
                gltype=GLType.BOOL,
                common_filter_index=3,
                kind=2
            ),
        ]

        # friendly
        friendly = [
            '???????????? ???? ??????????????',
            '???????????????? ???? 100 ????/??',
            '?????????? ?? ????????????????'
        ]

        # friendly_ext
        friendly_ext = []
        value = CsGumoful.FriendlyExtValue()
        value.Value = '???????????? ???? ??????????????'
        value.UsedParams.append(300)
        usedParam = CsGumoful.UsedParamsType(Id=300)
        usedParam.UsedValues.append(CsGumoful.UsedValueType(Value="3001", IsFilterable=True))
        value.UsedParamsWithValues.append(usedParam)
        value.Type = 'spec'
        friendly_ext.append(value)

        value = CsGumoful.FriendlyExtValue()
        value.Value = '???????????????? ???? 100 ????/??'
        value.UsedParams.append(301)
        usedParam = CsGumoful.UsedParamsType(Id=301)
        usedParam.UsedValues.append(CsGumoful.UsedValueType(Value="100", IsFilterable=False))
        value.UsedParamsWithValues.append(usedParam)
        value.Type = 'spec'
        friendly_ext.append(value)

        value = CsGumoful.FriendlyExtValue()
        value.Value = '?????????? ?? ????????????????'
        value.Type = 'spec'
        friendly_ext.append(value)

        # full
        full = []
        full_value = CsGumoful.FullSpecGroup()
        full_value.GroupName = '???????????????? ????????????????????????????'

        value = CsGumoful.FullSpecValue()
        value.Name = '????????????????'
        value.Value = '????????????'
        value.Description = '???????????????? (????????????????)'
        usedParam = CsGumoful.FullUsedValue(Id=300, Name='????????????????')
        value.UsedParams.append(usedParam)
        usedParam = CsGumoful.UsedParamsType(Id=300, Name='????????????????')
        usedParam.UsedValues.append(CsGumoful.UsedValueType(Value="3001", IsFilterable=True))
        value.UsedParamsWithValues.append(usedParam)
        full_value.GroupSpecs.append(value)

        value = CsGumoful.FullSpecValue()
        value.Name = '???????????????????????? ????????????????'
        value.Value = '100 ????/??'
        value.Description = '???????????????????????? ???????????????? (????????????????)'
        usedParam = CsGumoful.FullUsedValue(Id=301, Name='???????????????????????? ????????????????')
        value.UsedParams.append(usedParam)
        usedParam = CsGumoful.UsedParamsType(Id=301, Name='???????????????????????? ????????????????')
        usedParam.UsedValues.append(CsGumoful.UsedValueType(Value="100", IsFilterable=False))
        value.UsedParamsWithValues.append(usedParam)
        full_value.GroupSpecs.append(value)

        value = CsGumoful.FullSpecValue()
        value.Name = '?? ????????????????????'
        value.Value = '????'
        value.Description = '?? ???????????????????? (????????????????)'
        usedParam = CsGumoful.FullUsedValue(Id=302, Name='?? ????????????????????')
        value.UsedParams.append(usedParam)
        usedParam = CsGumoful.UsedParamsType(Id=302, Name='?? ????????????????????')
        usedParam.UsedValues.append(CsGumoful.UsedValueType(Value="1", IsFilterable=True))
        value.UsedParamsWithValues.append(usedParam)
        full_value.GroupSpecs.append(value)
        full.append(full_value)

        # seo
        lingua = CsGumoful.Lingua()
        lingua.Nominative = '????????????????-nominative'
        lingua.Genitive = '????????????????-genitive'
        lingua.Dative = '????????????????-dative'
        lingua.Accusative = '????????????????-accusative'

        cls.index.models += [
            Model(
                hyperid=222,
                hid=2,
                vendor_id=100500,
                title='??????????????????',
                description='?????????????? ??????????????????',
                glparams=[
                    GLParam(id=300, value=[3001]),
                    GLParam(id=301, value=100),
                    GLParam(id=302, value=1)
                ],
                rendered_specs=CsGumoful.CsGumoful(
                    Friendly=friendly,
                    FriendlyExt=friendly_ext,
                    Full=full,
                    Seo=lingua,
                    Micro='?????????????? ???????????????? ????????????????????'
                )
            ),
        ]

        # ?????????????? ????????????????, ?? ?????????????? ?????????????? ?????????????????????? ???? ??????????????
        # ?????? ?????????? ???? ???????????????? ?? ??????????????????
        cls.index.gumoful_templates += [
            GumofulTemplate(
                hid=2,
                micromodel="?????????????????? ???? {Material}",
                friendlymodel=[
                    "???????????? ?????????????????? ???? {Material}",
                ],
                model=[
                    (
                        "????????????????????????????",
                        {
                            "????????????????": "{Material}",
                        },
                    ),
                ],
                seo="{return $Material; #exec}",
            ),
        ]

        # friendly
        friendly = [
            '???????????? ???? ?????????????? (???? ??????????)',
        ]

        # friendly_ext
        friendly_ext = []
        value = CsGumoful.FriendlyExtValue()
        value.Value = '???????????? ???? ??????????????'
        value.UsedParams.append(300)
        value.Type = 'spec'
        friendly_ext.append(value)

        # full
        full = []
        full_value = CsGumoful.FullSpecGroup()
        full_value.GroupName = '???????????????? ????????????????????????????'

        value = CsGumoful.FullSpecValue()
        value.Name = '????????????????'
        value.Value = '????????????'
        value.Description = '???????????????? (????????????????)'
        usedParam = CsGumoful.FullUsedValue(Id=300, Name='????????????????')
        value.UsedParams.append(usedParam)
        full_value.GroupSpecs.append(value)

        cls.index.models += [
            Model(
                hyperid=333,
                hid=2,
                vendor_id=100500,
                title='?????????????????? v2',
                description='?????????????? ??????????????????',
                glparams=[
                    GLParam(id=300, value=[3001]),
                    GLParam(id=301, value=100),
                    GLParam(id=302, value=1)
                ],
                rendered_specs=CsGumoful.CsGumoful(
                    Friendly=friendly,
                    FriendlyExt=friendly_ext,
                    Full=full,
                    Seo=lingua,
                    Micro='?????????????? ???????????????? ???????????????????? (????????)'
                )
            )
        ]

    def test_rendered_templates(self):
        '''
        ???????? ?????????????? ?????? ???????? ?? ???????????? ?????????? ???? ?????????? ???? ???????????? (???????? cs_gumoful)
        '''
        request = {
            "model_ids": [222]
        }
        response = self.market_content_storage.request_json(
            'card_info',
            method='GET',
            body=json.dumps(request)
        )
        self.assertFragmentIn(
            response,
            {
                'models': [
                    {
                        'id': 222,
                        "description": "?????????????? ???????????????? ????????????????????",
                        "specs": {
                            "friendly": [
                                "???????????? ???? ??????????????",
                                "???????????????? ???? 100 ????/??",
                                "?????????? ?? ????????????????"
                            ],
                            "friendlyext": [
                                {
                                    "type" : "spec",
                                    "value": "???????????? ???? ??????????????",
                                    "usedParams": [
                                        300
                                    ],
                                    "usedParamsWithValues": [
                                        {
                                            "id": 300,
                                            "values": [
                                                {
                                                    "value": "3001",
                                                    "isFilterable": True
                                                }
                                            ]
                                        }
                                    ]
                                },
                                {
                                    "type" : "spec",
                                    "value": "???????????????? ???? 100 ????/??",
                                    "usedParams": [
                                        301
                                    ],
                                    "usedParamsWithValues": [
                                        {
                                            "id": 301,
                                            "values": [
                                                {
                                                    "value": "100",
                                                    "isFilterable": False
                                                }
                                            ]
                                        }
                                    ]
                                },
                                {
                                    "type" : "spec",
                                    "value": "?????????? ?? ????????????????",
                                    "usedParams": [],
                                    "usedParamsWithValues": []
                                }
                            ],
                            "full": [
                                {
                                    "groupName": "???????????????? ????????????????????????????",
                                    "groupSpecs": [
                                        {
                                            "name": "????????????????",
                                            "value": "????????????",
                                            "desc": "???????????????? (????????????????)",
                                            "usedParams": [
                                                {
                                                    "id": 300,
                                                    "name": "????????????????"
                                                }
                                            ],
                                            "usedParamsWithValues": [
                                                {
                                                    "id": 300,
                                                    "name": "????????????????",
                                                    "values": [
                                                        {
                                                            "value": "3001",
                                                            "isFilterable": True
                                                        }
                                                    ]
                                                }
                                            ]
                                        },
                                        {
                                            "name": "???????????????????????? ????????????????",
                                            "value": "100 ????/??",
                                            "desc": "???????????????????????? ???????????????? (????????????????)",
                                            "usedParams": [
                                                {
                                                    "id": 301,
                                                    "name": "???????????????????????? ????????????????"
                                                }
                                            ],
                                            "usedParamsWithValues": [
                                                {
                                                    "id": 301,
                                                    "name": "???????????????????????? ????????????????",
                                                    "values": [
                                                        {
                                                            "isFilterable": False,
                                                            "value": "100"
                                                        }
                                                    ]
                                                }
                                            ],
                                        },
                                        {
                                            "name": "?? ????????????????????",
                                            "value": "????",
                                            "desc": "?? ???????????????????? (????????????????)",
                                            "usedParams": [
                                                {
                                                    "id": 302,
                                                    "name": "?? ????????????????????"
                                                }
                                            ],
                                            "usedParamsWithValues": [
                                                {
                                                    "id": 302,
                                                    "name": "?? ????????????????????",
                                                    "values": [
                                                        {
                                                            "isFilterable": True,
                                                            "value": "1"
                                                        }
                                                    ]
                                                }
                                            ],
                                        }
                                    ]
                                }
                            ]
                        },
                        "lingua": {
                            "accusative": "????????????????-accusative",
                            "dative": "????????????????-dative",
                            "genitive": "????????????????-genitive",
                            "nominative": "????????????????-nominative"
                        },
                    }
                ]
            },
            allow_different_len=False
        )

    def test_new_spec_format(self):
        '''
        ???????????? ???????? ?????????? ?????????????? ?????????? ?????? usedParamsWithValues, ????-???? ?????????? ?????????? ????????????????????
        ?????? ?????????????????????? ???????????????? ????????????, ?????????? ???? ???????????? ?????? ???????????????? ????????????
        ?? ?????????? ???????? ???? ?????????????? ???????? ???? ??????????
        '''
        request = {
            "model_ids": [333]
        }
        response = self.market_content_storage.request_json(
            'card_info',
            method='GET',
            body=json.dumps(request)
        )
        self.assertFragmentIn(
            response,
            {
                'models': [
                    {
                        'id': 333,
                        "title": {
                            "raw": "?????????????????? v2"
                        },
                        "description": "?????????????????? ???? ????????????",  # micro
                        "specs": {
                            "friendly": [
                                "???????????? ?????????????????? ???? ????????????"
                            ],
                            "friendlyext": [
                                {
                                    "type" : "spec",
                                    "value": "???????????? ?????????????????? ???? ????????????",
                                    "usedParams": [
                                        300
                                    ],
                                    "usedParamsWithValues": [
                                        {
                                            "id": 300,
                                            "values": [
                                                {
                                                    "value": "3001",
                                                    "isFilterable": True
                                                }
                                            ]
                                        }
                                    ]
                                }
                            ],
                            "full": [
                                {
                                    "groupName": "????????????????????????????",
                                    "groupSpecs": [
                                        {
                                            "name": "????????????????",
                                            "value": "????????????",
                                            "desc": "???????????????? parameter description",
                                            "usedParams": [
                                                {
                                                    "id": 300,
                                                    "name": "????????????????"
                                                }
                                            ],
                                            "usedParamsWithValues": [
                                                {
                                                    "id": 300,
                                                    "name": "????????????????",
                                                    "values": [
                                                        {
                                                            "value": "3001",
                                                            "isFilterable": True
                                                        }
                                                    ]
                                                }
                                            ]
                                        }
                                    ]
                                }
                            ]
                        },
                       "lingua": {
                            "accusative": "  ????????????-accusative  ",
                            "dative": "????????????-dative ",
                            "genitive": " ????????????-genitive  ",
                            "nominative": "  ????????????-nominative "
                        },
                    }
                ]
            },
            allow_different_len=False
        )

    def test_specs_flags(self):
        '''
        ?????????????????? ???????????? ???????????????? ??????????????:
        show_full_model_specs;
        show_full_sku_specs;
        show_friendly_model_specs;
        show_friendly_sku_specs
        '''

        # ?????????????? ?????????????? - ????????????
        request = {
            'model_ids': [222],
            'market_skus': [12520],
            'show_friendly_model_specs': False
        }
        response = self.market_content_storage.request_json(
            'card_info',
            method='GET',
            body=json.dumps(request)
        )

        self.assertFragmentIn(
            response,
            {
                'models': [
                    {
                        'id': 222,
                        'specs': {
                            'friendly': [],
                            'friendlyext': [],
                            'full': NotEmpty()
                        }
                    }
                ],
                'mskus': [
                    {
                        'id': 12520,
                        'specs': {
                            'friendly': NotEmpty(),
                            'friendlyext': NotEmpty(),
                            'full': NotEmpty()
                        }
                    }
                ]
            },
            allow_different_len=False
        )

        # ?????????????? ???????? - ????????????
        request = {
            'model_ids': [222],
            'market_skus': [12520],
            'show_full_model_specs': False
        }
        response = self.market_content_storage.request_json(
            'card_info',
            method='GET',
            body=json.dumps(request)
        )

        self.assertFragmentIn(
            response,
            {
                'models': [
                    {
                        'id': 222,
                        'specs': {
                            'friendly': NotEmpty(),
                            'friendlyext': NotEmpty(),
                            'full': []
                        }
                    }
                ],
                'mskus': [
                    {
                        'id': 12520,
                        'specs': {
                            'friendly': NotEmpty(),
                            'friendlyext': NotEmpty(),
                            'full': NotEmpty()
                        }
                    }
                ]
            },
            allow_different_len=False
        )

        # ?????????????? ?????? ?????????? - ????????????
        request = {
            'model_ids': [222],
            'market_skus': [12520],
            'show_full_model_specs': False,
            'show_friendly_model_specs': False
        }
        response = self.market_content_storage.request_json(
            'card_info',
            method='GET',
            body=json.dumps(request)
        )

        self.assertFragmentIn(
            response,
            {
                'models': [
                    {
                        'id': 222,
                        'specs': {
                            'friendly': [],
                            'friendlyext': [],
                            'full': []
                        }
                    }
                ],
                'mskus': [
                    {
                        'id': 12520,
                        'specs': {
                            'friendly': NotEmpty(),
                            'friendlyext': NotEmpty(),
                            'full': NotEmpty()
                        }
                    }
                ]
            },
            allow_different_len=False
        )

        # ?????????????? ?????????????? - ??????
        request = {
            'model_ids': [222],
            'market_skus': [12520],
            'show_friendly_sku_specs': False
        }
        response = self.market_content_storage.request_json(
            'card_info',
            method='GET',
            body=json.dumps(request)
        )

        self.assertFragmentIn(
            response,
            {
                'models': [
                    {
                        'id': 222,
                        'specs': {
                            'friendly': NotEmpty(),
                            'friendlyext': NotEmpty(),
                            'full': NotEmpty()
                        }
                    }
                ],
                'mskus': [
                    {
                        'id': 12520,
                        'specs': {
                            'friendly': [],
                            'friendlyext': [],
                            'full': NotEmpty()
                        }
                    }
                ]
            },
            allow_different_len=False
        )

        # ?????????????? ???????? - ??????
        request = {
            'model_ids': [222],
            'market_skus': [12520],
            'show_full_sku_specs': False
        }
        response = self.market_content_storage.request_json(
            'card_info',
            method='GET',
            body=json.dumps(request)
        )

        self.assertFragmentIn(
            response,
            {
                'models': [
                    {
                        'id': 222,
                        'specs': {
                            'friendly': NotEmpty(),
                            'friendlyext': NotEmpty(),
                            'full': NotEmpty()
                        }
                    }
                ],
                'mskus': [
                    {
                        'id': 12520,
                        'specs': {
                            'friendly': NotEmpty(),
                            'friendlyext': NotEmpty(),
                            'full': []
                        }
                    }
                ]
            },
            allow_different_len=False
        )

        # ?????????????? ?????? ?????????? - ??????
        request = {
            'model_ids': [222],
            'market_skus': [12520],
            'show_full_sku_specs': False,
            'show_friendly_sku_specs': False
        }
        response = self.market_content_storage.request_json(
            'card_info',
            method='GET',
            body=json.dumps(request)
        )

        self.assertFragmentIn(
            response,
            {
                'models': [
                    {
                        'id': 222,
                        'specs': {
                            'friendly': NotEmpty(),
                            'friendlyext': NotEmpty(),
                            'full': NotEmpty()
                        }
                    }
                ],
                'mskus': [
                    {
                        'id': 12520,
                        'specs': {
                            'friendly': [],
                            'friendlyext': [],
                            'full': []
                        }
                    }
                ]
            },
            allow_different_len=False
        )

    def test_uniq_cards(self):
        '''
        ???????????? ???????????? ?? ??????????????????????, ?????? ?? ????????????????
        ????????????????, ?????? ?? ?????????? ???????????????? ?????? ????????????
        '''
        # ???????????? ????????????
        request = {
            'model_ids': [2111],
            'market_skus': [],
        }
        response = self.market_content_storage.request_json(
            'card_info',
            method='GET',
            body=json.dumps(request)
        )
        self.assertFragmentIn(
            response,
            {
                'models': [{'id': 2111}],
                'mskus': []
            },
            allow_different_len=False
        )
        # ???????????? ??????
        request = {
            'model_ids': [],
            'market_skus': [2111],
        }
        response = self.market_content_storage.request_json(
            'card_info',
            method='GET',
            body=json.dumps(request)
        )
        self.assertFragmentIn(
            response,
            {
                'models': [],
                'mskus': [{'id': 2111}]
            },
            allow_different_len=False
        )
        # ??????
        request = {
            'model_ids': [2111],
            'market_skus': [2111],
        }
        response = self.market_content_storage.request_json(
            'card_info',
            method='GET',
            body=json.dumps(request)
        )
        self.assertFragmentIn(
            response,
            {
                'models': [{'id': 2111}],
                'mskus': [{'id': 2111}]
            },
            allow_different_len=False
        )
        # ???????????? ????????????
        request = {
            'model_ids': [],
            'market_skus': [],
        }
        response = self.market_content_storage.request_json(
            'card_info',
            method='GET',
            body=json.dumps(request)
        )
        self.assertFragmentIn(
            response,
            {
                'models': [],
                'mskus': []
            },
            allow_different_len=False
        )

    def test_thumbs(self):
        '''
        ??????????????????, ?????? ?????????? ???????????????????????? ?? ???????????? ??????????????
        '''

        request = {
            'model_ids': [],
            'market_skus': [],
        }
        response = self.market_content_storage.request_json(
            'card_info',
            method='GET',
            body=json.dumps(request)
        )

        good_tumbs = [
            {
                "height": 50,
                "name": "50x50",
                "width": 50
            },
            {
                "height": 70,
                "name": "55x70",
                "width": 55
            },
            {
                "height": 80,
                "name": "60x80",
                "width": 60
            },
            {
                "height": 100,
                "name": "74x100",
                "width": 74
            },
            {
                "height": 75,
                "name": "75x75",
                "width": 75
            },
            {
                "height": 120,
                "name": "90x120",
                "width": 90
            },
            {
                "height": 100,
                "name": "100x100",
                "width": 100
            },
            {
                "height": 160,
                "name": "120x160",
                "width": 120
            },
            {
                "height": 150,
                "name": "150x150",
                "width": 150
            },
            {
                "height": 240,
                "name": "180x240",
                "width": 180
            },
            {
                "height": 250,
                "name": "190x250",
                "width": 190
            },
            {
                "height": 200,
                "name": "200x200",
                "width": 200
            },
            {
                "height": 320,
                "name": "240x320",
                "width": 240
            },
            {
                "height": 300,
                "name": "300x300",
                "width": 300
            },
            {
                "height": 400,
                "name": "300x400",
                "width": 300
            },
            {
                "height": 600,
                "name": "600x600",
                "width": 600
            },
            {
                "height": 800,
                "name": "600x800",
                "width": 600
            },
            {
                "height": 1200,
                "name": "900x1200",
                "width": 900
            },
            {
                "height": 124,
                "name": "x124_trim",
                "width": 166
            },
            {
                "height": 166,
                "name": "x166_trim",
                "width": 248
            },
            {
                "height": 248,
                "name": "x248_trim",
                "width": 332
            },
            {
                "height": 332,
                "name": "x332_trim",
                "width": 496
            }
        ]

        self.assertFragmentIn(
            response,
            {
                "knownThumbnails": [
                    {
                        "namespace": "marketpic",
                        "thumbnails": good_tumbs
                    },
                    {
                        "namespace": "marketpic_scaled",
                        "thumbnails": good_tumbs
                    },
                    {
                        "namespace": "mpic",
                        "thumbnails": [
                            {
                                "height": 50,
                                "name": "1hq",
                                "width": 50
                            },
                            {
                                "height": 100,
                                "name": "2hq",
                                "width": 100
                            },
                            {
                                "height": 75,
                                "name": "3hq",
                                "width": 75
                            },
                            {
                                "height": 150,
                                "name": "4hq",
                                "width": 150
                            },
                            {
                                "height": 200,
                                "name": "5hq",
                                "width": 200
                            },
                            {
                                "height": 250,
                                "name": "6hq",
                                "width": 250
                            },
                            {
                                "height": 120,
                                "name": "7hq",
                                "width": 120
                            },
                            {
                                "height": 240,
                                "name": "8hq",
                                "width": 240
                            },
                            {
                                "height": 500,
                                "name": "9hq",
                                "width": 500
                            },
                            {
                                "height": 124,
                                "name": "x124_trim",
                                "width": 166
                            },
                            {
                                "height": 166,
                                "name": "x166_trim",
                                "width": 248
                            },
                            {
                                "height": 248,
                                "name": "x248_trim",
                                "width": 332
                            },
                            {
                                "height": 332,
                                "name": "x332_trim",
                                "width": 496
                            }
                        ]
                    }
                ]
            },
            allow_different_len=False
        )

    @classmethod
    def prepare_creator_type(cls):
        cls.index.gltypes += [
            GLType(
                hid=1,
                id=17693310,  # PSKU2_GL_PARAM_ID = 17693310
                name='?????????????????????? ???????????????? 2',
                xslname="partner2",
                gltype=GLType.BOOL,
                is_gurulight=False,
            ),
            GLType(
                hid=1,
                id=20840910,  # PSKU2_LITE_GL_PARAM_ID = 20840910
                name='?????????????????????? ???????????????? 2 (lite)',
                xslname="partner2lite",
                gltype=GLType.BOOL,
                is_gurulight=False,
            ),
        ]

        cls.index.models += [
            Model(hyperid=550, hid=1, title='???????????? ???? ??????????????'),
            Model(hyperid=551, hid=1, title='???????????? ???? ????????????????', created_by_partner=True)
        ]

        cls.index.skus += [
            Sku(sku_id=1550, model_id=550, hid=1, title='C???? ???? ??????????????'),
            Sku(sku_id=1551, model_id=550, hid=1, title='C???? ???? ????????????????', created_by_partner=True),
            Sku(sku_id=1552, model_id=550, hid=1, title='C???? ???? ???????????????? (v2)', created_by_partner=True, glparams=[GLParam(id=17693310, value=1)]),
            Sku(sku_id=1553, model_id=550, hid=1, title='C???? ???? ???????????????? (v2 - lite)', created_by_partner=True, glparams=[GLParam(id=20840910, value=1)])
        ]

    def test_card_creator(cls):
        '''
        ?????????????????? ???????????? ?????????????????????? modelCreator ?? marketSkuCreator
        '''

        request = {
            'model_ids': [550, 551],
            'market_skus': [1550, 1551, 1552, 1553]
        }
        response = cls.market_content_storage.request_json(
            'card_info',
            method='GET',
            body=json.dumps(request)
        )

        cls.assertFragmentIn(
            response,
            {
                'models': [
                    {
                        'id': 550,
                        'title': {
                            'raw': '???????????? ???? ??????????????'
                        },
                        'modelCreator': 'market'
                    },
                    {
                        'id': 551,
                        'title': {
                            'raw': '???????????? ???? ????????????????'
                        },
                        'modelCreator': 'partner'
                    }
                ],
                'mskus': [
                    {
                        'id': 1550,
                        'title': {
                            'raw': 'C???? ???? ??????????????'
                        },
                        'marketSkuCreator': 'market'
                    },
                    {
                        'id': 1551,
                        'title': {
                            'raw': 'C???? ???? ????????????????'
                        },
                        'marketSkuCreator': 'partner'
                    },
                    {
                        'id': 1552,
                        'title': {
                            'raw': 'C???? ???? ???????????????? (v2)'
                        },
                        'marketSkuCreator': 'partner2'
                    },
                    {
                        'id': 1553,
                        'title': {
                            'raw': 'C???? ???? ???????????????? (v2 - lite)'
                        },
                        'marketSkuCreator': 'partner2lite'
                    }
                ]
            },
            allow_different_len=False
        )

    @classmethod
    def prepare_model_name(cls):
        cls.index.gltypes += [
            GLType(
                hid=1,
                id=111999,
                name='?????? ????????????',
                xslname="name",
                gltype=GLType.STRING,
                is_gurulight=False,
            )
        ]

        cls.index.models += [
            Model(hyperid=552, hid=1, title='???????????? ?? modelName', glparams=[GLParam(id=111999, value='??????????????')]),
            Model(hyperid=553, hid=1, title='???????????? ?????? modelName'),
        ]

    def test_model_name(cls):
        '''
        ?????????????????? ???????????? ?????????????????????? modelCreator ?? marketSkuCreator
        '''

        request = {
            'model_ids': [552, 553],
        }
        response = cls.market_content_storage.request_json(
            'card_info',
            method='GET',
            body=json.dumps(request)
        )
        cls.assertFragmentIn(
            response,
            {
                'models': [
                    {
                        'id': 552,
                        'title': {
                            'raw': '???????????? ?? modelName'
                        },
                        'modelName': '??????????????'
                    },
                    {
                        'id': 553,
                        'title': {
                            'raw': '???????????? ?????? modelName'
                        },
                        'modelName': NoKey('modelName')
                    }
                ],
            },
            allow_different_len=False
        )

    @classmethod
    def prepare_sku_description_param(cls):
        cls.index.gltypes += [
            GLType(
                hid=1,
                id=15341921,
                name='????????????????',
                xslname="description",
                gltype=GLType.STRING,
                is_gurulight=False,
            )
        ]

        cls.index.skus += [
            Sku(sku_id=10090, model_id=552, hid=1, title='C???? c ?????????????????? ?? ??????????????????', glparams=[GLParam(id=15341921, value='???????????????? ???? ??????????????????')]),
        ]

    def test_sku_description_param(cls):
        '''
        ??????????????????, ?????? ?????? ???????????????????? ???????????????? ?? ??????, ?????? ?????????????? ???? ?????????????????? 15341921
        '''

        request = {
            'market_skus': [10090],
        }
        response = cls.market_content_storage.request_json(
            'card_info',
            method='GET',
            body=json.dumps(request)
        )

        cls.assertFragmentIn(
            response,
            {
                'mskus': [
                    {
                        'id': 10090,
                        'title': {
                            'raw': 'C???? c ?????????????????? ?? ??????????????????'
                        },
                        'description': '???????????????? ???? ??????????????????'
                    },
                ],
            },
            allow_different_len=False
        )

    @classmethod
    def prepare_card_video(cls):
        cls.index.gltypes += [
            GLType(
                hid=1,
                id=1540,
                name='??????????',
                xslname="models_video",
                gltype=GLType.STRING,
                is_gurulight=False,
            )
        ]

        cls.index.models += [
            Model(hyperid=1122, hid=1, video=['video_model_0', 'video_model_1']),
            Model(hyperid=1133, hid=1, glparams=[GLParam(id=1540, value=['video_model_2', 'video_model_3'])]),
            Model(hyperid=1144, hid=1, video=['video_model_4'], glparams=[GLParam(id=1540, value=['video_model_4', 'video_model_5'])]),
        ]

        cls.index.skus += [
            Sku(sku_id=11220, model_id=1122, hid=1, video=['video_sku_0']),
            Sku(sku_id=11330, model_id=1133, hid=1, glparams=[GLParam(id=1540, value=['video_sku_1', 'video_sku_2'])]),
            Sku(sku_id=11440, model_id=1144, hid=1, video=['video_sku_3'], glparams=[GLParam(id=1540, value=['video_sku_3'])]),
        ]

    def test_card_video(cls):
        '''
        ?????????????????? ?????????? ???? ????????????????
        ?????? ?????????????? ???? ???????? videos ?? ???? ???????????????????? ????????????????, ???? ???????? ??????????????, ?????? videos ?????????????????????? + ???????? ???????????? ???????? ??????????????????
        '''
        request = {
            'model_ids': [1122, 1133, 1144],
            'market_skus': [11220, 11330, 11440],
        }
        response = cls.market_content_storage.request_json(
            'card_info',
            method='GET',
            body=json.dumps(request)
        )
        cls.assertFragmentIn(
            response,
            {
                'models': [
                    {
                        'id': 1122,
                        'video': [
                            'video_model_0',
                            'video_model_1'
                        ]
                    },
                    {
                        'id': 1133,
                        'video': [
                            'video_model_2',
                            'video_model_3'
                        ]
                    },
                    {
                        'id': 1144,
                        'video': [
                            'video_model_4',
                            'video_model_5'
                        ]
                    }
                ],
                'mskus': [
                    {
                        'id': 11220,
                        'video': [
                            'video_sku_0'
                        ]
                    },
                    {
                        'id': 11330,
                        'video': [
                            'video_sku_1',
                            'video_sku_2'
                        ]
                    },
                    {
                        'id': 11440,
                        'video': [
                            'video_sku_3'
                        ]
                    }
                ],
            },
            allow_different_len=False
        )

if __name__ == '__main__':
    main()

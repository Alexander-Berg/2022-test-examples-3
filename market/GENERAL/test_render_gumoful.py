from market.content_storage_service.lite.core.types.gl_param import GLType, GLParam, GLHypothesis, GLParamFromSku
from market.content_storage_service.lite.core.types.gumoful_template import GumofulTemplate
from market.content_storage_service.lite.core.types.model import Model
from market.content_storage_service.lite.core.types.vendor import Vendor
from market.content_storage_service.lite.core.types.hid import Hid
from market.content_storage_service.lite.core.testcase import TestCase, main
from market.proto.cs import CsGumofulService_pb2


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.market_content_storage.with_saas_server = True
        cls.saas = T.market_content_storage.saas_server.connect()

        cls.index.models += [
            Model(
                hyperid=10001,
                hid=23,
                vendor_id=30001,
                glparams=[
                    GLParam(id=100, value=1),
                    GLParam(id=101, value=0),
                    GLParam(id=102, value=1),
                    GLParam(id=103, value=1),
                    GLParam(id=104, value=7),
                    GLParam(id=105, value=[1]),
                    GLParam(id=106, value=[2]),
                    GLParam(id=110, value=332),
                ],
                param_hypothesis=[
                    GLHypothesis(param_id=106, values=['Умный телефон', 'Не айфон'])
                ],
                additional_params=[
                    GLParamFromSku(sku_id=91, params=[GLParam(id=105, value=[3])]),
                    GLParamFromSku(sku_id=92, params=[GLParam(id=105, value=[3, 4])])
                ],
            ),
             Model(
                hyperid=10002,
                hid=23,
            )
        ]

        cls.index.vendors += [
            Vendor(id=30001, name="Name-30001", website="vendor-30001.su")
        ]

        cls.index.hids += [
            Hid(hid=23)
        ]

        cls.index.gumoful_templates += [
            GumofulTemplate(
                hid=23,
                micromodel="{Type}, {*comma-on}{Retina:ретина}{2SimCards:2 сим карты}{WiFi:вайфай}{*comma-off}",
                friendlymodel=[
                    "Телефон {Retina#ifnz}{Retina:с ретиной}{#endif}",
                    "{2SimCards:с 2-мя сим-картами}",
                    "{InfraRedPort#ifz}без инфракрасного порта{#endif}",
                    "Длина: {Length} см"
                ],
                model=[
                    (
                        "Технические характеристики",
                        {
                            "Ретина": "{Retina}",
                            "2 сим-карты": "{2SimCards}",
                            "Диагональ экрана": "{DisplaySize}",
                            "ОС": "{OS}",
                            "Тип телефона": "{Type}",
                            "Rare Parameter": "{RareParam}",
                            "Empty Parameter": "{EmptyParam}",
                            "Lenght": "{Length}"
                        },
                    ),
                    ("Прочее", {"Дополнительная информация": "{AdditionalInfo}"}),
                    (
                        "Секция с пустыми параметрами",
                        {"Rare Parameter": "{RareParam}", "Empty Parameter": "{EmptyParam}"},
                    ),
                ],
                seo="{return $Type; #exec}",
            ),
        ]

        # Создаем фильтры, на которые ссылаются в шаблонах
        cls.index.gltypes += [
            GLType(hid=23, id=100, xslname="Retina", gltype=GLType.BOOL, common_filter_index=1),
            GLType(hid=23, id=101, xslname="2SimCards", gltype=GLType.BOOL, common_filter_index=2),
            GLType(hid=23, id=102, xslname="InfraRedPort", gltype=GLType.BOOL, common_filter_index=3),
            GLType(hid=23, id=103, xslname="WiFi", gltype=GLType.BOOL, common_filter_index=4),
            GLType(hid=23, id=104, xslname="DisplaySize", gltype=GLType.NUMERIC, common_filter_index=5),
            GLType(
                hid=23,
                id=105,
                xslname="OS",
                gltype=GLType.ENUM,
                options={1: "iOS", 2: "Android", 3: "Операционка из скю - 1", 4: "Операционка из скю - 2"},
                common_filter_index=6
            ),
            GLType(
                hid=23,
                id=106,
                xslname="Type",
                gltype=GLType.ENUM,
                options={1: "мобильный телефон", 2: "смартфон"},
                common_filter_index=7
            ),
            GLType(hid=23, id=107, xslname="AdditionalInfo", gltype=GLType.STRING, common_filter_index=8),
            GLType(hid=23, id=108, xslname="RareParam", gltype=GLType.STRING, common_filter_index=9),
            GLType(hid=23, id=109, xslname="EmptyParam", gltype=GLType.STRING, common_filter_index=10),
            GLType(
                hid=23,
                id=110,
                xslname="Length",
                gltype=GLType.NUMERIC_ENUM,
                common_filter_index=11,
                options={331: "10", 332: "15"},
            )
        ]

    def test_render_gumoful(self):
        request = CsGumofulService_pb2.CsGumofulRequest()
        request.RequestList.extend([
            super().render_gumoful_request_card(10001),
        ])

        response = self.market_content_storage.request_json(
            'render_gumoful',
            method='GET',
            body=request.SerializeToString()
        )

        self.assertFragmentIn(
            response,
            {
                "friendly": [
                    "Телефон с ретиной",
                    "Длина: 15 см"
                ],
            },
            allow_different_len=False
        )

        self.assertFragmentIn(
            response,
            {
                "friendlyExt": [
                    {
                        "type" : "spec",
                        "usedParams": [100],
                        "usedParamsWithValues": [
                            {
                                "id": 100,
                                "values": [
                                    {
                                        "value": "1",
                                        "isFilterable": True
                                    }
                                ]
                            }
                        ],
                        "value": "Телефон с ретиной",
                    },
                    {
                        "type" : "spec",
                        "value": "Длина: 15 см",
                        "usedParams": [
                            110
                        ],
                        "usedParamsWithValues": [
                            {
                                "id": 110,
                                "values": [
                                    {
                                        "isFilterable": True,
                                        "value": "332"
                                    }
                                ]
                            }
                        ]
                    }
                ],
            },
            allow_different_len=False
        )

        self.assertFragmentIn(
            response,
            {
                "full": [{
                    "groupName": "Технические характеристики",
                    "groupSpecs": [
                        {
                            "desc": "Ретина parameter description",
                            "mainProperty": False,
                            "name": "Ретина",
                            "usedParams": [
                                {
                                    "id": 100,
                                    "name": "BOOLEAN_23_100"
                                }
                            ],
                            "usedParamsWithValues": [
                                {
                                    "id": 100,
                                    "name": "BOOLEAN_23_100",
                                    "values": [
                                        {
                                            "isFilterable": True,
                                            "value": "1"
                                        }
                                    ]
                                }
                            ],
                            "value": "есть"
                        },
                        {
                            "desc": "2 сим-карты parameter description",
                            "mainProperty": False,
                            "name": "2 сим-карты",
                            "usedParams": [
                                {
                                    "id": 101,
                                    "name": "BOOLEAN_23_101"
                                }
                            ],
                            "usedParamsWithValues": [
                                {
                                    "id": 101,
                                    "name": "BOOLEAN_23_101",
                                    "values": [
                                        {
                                            "isFilterable": True,
                                            "value": "0"
                                        }
                                    ]
                                }
                            ],
                            "value": "нет"
                        },
                        {
                            "desc": "Диагональ экрана parameter description",
                            "mainProperty": False,
                            "name": "Диагональ экрана",
                            "usedParams": [
                                {
                                    "id": 104,
                                    "name": "NUMERIC_23_104"
                                }
                            ],
                            "usedParamsWithValues": [
                                {
                                    "id": 104,
                                    "name": "NUMERIC_23_104",
                                    "values": [
                                        {
                                            "isFilterable": True,
                                            "value": "7"
                                        }
                                    ]
                                }
                            ],
                            "value": "7"
                        },
                        {
                            "desc": "ОС parameter description",
                            "mainProperty": False,
                            "name": "ОС",
                            "usedParams": [
                                {
                                    "id": 105,
                                    "name": "ENUM_23_105"
                                }
                            ],
                            "usedParamsWithValues": [
                                {
                                    "id": 105,
                                    "name": "ENUM_23_105",
                                    "values": [
                                        {
                                            "isFilterable": True,
                                            "value": "1"
                                        },
                                        {
                                            "isFilterable": True,
                                            "value": "3"
                                        },
                                        {
                                            "isFilterable": True,
                                            "value": "4"
                                        }
                                    ]
                                }
                            ],
                            "value": "iOS, Операционка из скю - 1, Операционка из скю - 2"
                        },
                        {
                            "name": "Тип телефона",
                            "value": "смартфон, Умный телефон, Не айфон",  # С добавлением гипотез
                            "desc": "Тип телефона parameter description",
                            "mainProperty": False,
                            "usedParams": [
                                {
                                    "id": 106,
                                    "name": "ENUM_23_106"
                                }
                            ],
                            "usedParamsWithValues": [
                                {
                                    "id": 106,
                                    "name": "ENUM_23_106",
                                    "values": [
                                        {
                                            "isFilterable": True,
                                            "value": "2"
                                        },
                                        {
                                            "isFilterable": False,
                                            "value": "Умный телефон"
                                        },
                                        {
                                            "isFilterable": False,
                                            "value": "Не айфон"
                                        }
                                    ]
                                }
                            ],
                        },
                        {
                            "name": "Lenght",
                            "value": "15",
                            "desc": "Lenght parameter description",
                            "usedParams": [
                                {
                                    "id": 110,
                                    "name": "NUMERIC_ENUM_23_110",
                                }
                            ],
                            "usedParamsWithValues": [
                                {
                                    "id": 110,
                                    "name": "NUMERIC_ENUM_23_110",
                                    "values": [
                                        {
                                            "isFilterable": True,
                                            "value": "332"
                                        }
                                    ]
                                }
                            ]
                        }
                    ]
                }],
            },
            allow_different_len=False
        )

        self.assertFragmentIn(
            response,
            {
                "micro": "смартфон, Умный телефон, Не айфон, ретина, вайфай",
            }
        )

        self.assertFragmentIn(
            response,
            {
                "seo": {
                    "accusative": "  смартфон, Умный телефон, Не айфон-accusative  ",
                    "dative": "смартфон, Умный телефон, Не айфон-dative ",
                    "genitive": " смартфон, Умный телефон, Не айфон-genitive  ",
                    "nominative": "  смартфон, Умный телефон, Не айфон-nominative "
                }
            }
        )

    def test_batch_request(self):
        request = CsGumofulService_pb2.CsGumofulRequest()

        request.RequestList.extend([
            super().render_gumoful_request_card(10001),
            super().render_gumoful_request_card(10002),
        ])

        response = self.market_content_storage.request_json(
            'render_gumoful',
            method='GET',
            body=request.SerializeToString()
        )

        self.assertFragmentIn(
            response,
            {
                "resultList": [
                    {"cardId": 10001},
                    {"cardId": 10002},
                ]
            }
        )


if __name__ == '__main__':
    main()

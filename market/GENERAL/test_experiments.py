import json

from market.content_storage_service.lite.core.types.model import Model, Sku, VirtualCard
from market.content_storage_service.lite.core.types.hid import Hid
from market.content_storage_service.lite.core.types.gl_param import GLType, GLParam, GLParamFromSku, GLHypothesis
from market.content_storage_service.lite.core.types.gumoful_template import GumofulTemplate
from market.content_storage_service.lite.core.testcase import TestCase, main
from market.content_storage_service.lite.core.types.picture import Picture


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.market_content_storage.with_saas_server = True
        cls.market_content_storage.with_report_server = True
        cls.market_content_storage.set_virtual_range(20000, 200000)
        cls.saas = T.market_content_storage.saas_server.connect()
        cls.report = T.market_content_storage.report_server.connect()

        cls.index.hids += [
            Hid(hid=1, name='Цинтра', unique_name='Цинтра (unique)', output_type='GURU', leaf=True),
            Hid(hid=15756910, name='Лекарства', unique_name='Лекарства (unique)', output_type='GURU', leaf=True)
        ]

        cls.index.gltypes += [
            GLType(
                hid=1,
                id=90,
                name='Материал',
                xslname="Material",
                gltype=GLType.ENUM,
                options={
                    901: "Железный",
                    902: "Серебрянный",
                    903: "Бронзовый (из скю)",
                    904: "Каменный (из скю)"
                },
                common_filter_index=1,
                is_gurulight=True,
                kind=1,
                important=True
            ),
            GLType(
                hid=1,
                id=91,
                name='Длина',
                xslname="Length",
                gltype=GLType.NUMERIC,
                unit="см",
                max_value=100,
                min_value=10,
                precision=2,
                common_filter_index=2,
                kind=2
            ),
            GLType(
                hid=1,
                id=92,
                name='Против чудовищ',
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
                unit="см",
                max_value=100,
                min_value=0,
                precision=2,
                common_filter_index=4,
                important=True
            ),
            GLType(
                hid=1,
                id=94,
                name='Тип',
                xslname="Type",
                gltype=GLType.ENUM,
                options={
                    941: "Длинный",
                    942: "Двуручный",
                },
                common_filter_index=5,
                is_gurulight=True,
                kind=1
            ),
            GLType(
                hid=15756910,
                id=100,
                name='Тип лекартсва',
                xslname="Type",
                gltype=GLType.ENUM,
                options={
                    1: "От головы",
                    2: "От горла",
                },
                common_filter_index=1,
                is_gurulight=True,
                kind=1
            ),
        ]

        cls.index.gumoful_templates += [
            GumofulTemplate(
                hid=1,
                friendlymodel=[
                    "Меч {ForMonsters#ifnz}{ForMonsters:против чудовищ}{#endif}",
                    "Шириной {Width} см"
                ],
                model=[
                    (
                        "Характеристики",
                        {
                            "Материал": "{Material}",
                            "Длина": "{Length} см",
                            "Против чудовищ": "{ForMonsters}",
                            "Ширина": "{Width} см",
                            "Тип": "{Type}"
                        },
                    ),
                ]
            ),
        ]

        cls.index.models += [
            Model(
                hyperid=1252,
                hid=1,
                title='Меч',
                description='Кот',
                full_description='Кот полный',
                pictures=[
                    Picture(url='//avatars.mds.yandex.net/get-mpic/100/picture_1.jpeg/orig', height=100, width=200),
                    Picture(url='//avatars.mds.yandex.net/get-mpic/200/picture_2.jpeg/orig', height=200, width=100),
                    Picture(url='//avatars.mds.yandex.net/get-mpic/300/picture_3.jpeg/orig', height=300, width=300)
                ],
                glparams=[
                    GLParam(id=90, value=[901, 902]),
                    GLParam(id=91, value=55.556),
                    GLParam(id=92, value=1),
                    GLParam(id=93, value=101)
                ],
                additional_params=[
                    GLParamFromSku(sku_id=92, params=[GLParam(id=90, value=[903])]),
                    GLParamFromSku(sku_id=93, params=[GLParam(id=94, value=[941, 942])])
                ],
                param_hypothesis=[
                    GLHypothesis(param_id=90, values=['Алмазный']),
                    GLHypothesis(param_id=94, values=['Короткий'])
                ],
                video=[
                    'video1',
                    'video2'
                ]
            ),
            Model(
                hyperid=1253,
                hid=15756910,
                title='Какое-то лекарство',
                glparams=[
                    GLParam(id=100, value=[1]),
                ],
            ),
        ]

        cls.index.skus += [
            Sku(
                sku_id=12520,
                model_id=1252,
                title='Меч золотой',
                description='Кот котовий',
                hid=1,
                pictures=[
                    Picture(url='//avatars.mds.yandex.net/get-mpic/100/picture_sku_1.jpeg/orig', height=50, width=100),
                    Picture(url='//avatars.mds.yandex.net/get-mpic/200/picture_sku_2.jpeg/orig', height=100, width=50)
                ],
                glparams=[
                    GLParam(id=90, value=[901]),
                    GLParam(id=91, value=48.1),
                    GLParam(id=92, value=0),
                    GLParam(id=93, value=111)
                ],
                video=[
                    'video1',
                    'video2'
                ]
            )
        ]

        cls.index.virtual_cards += [
            VirtualCard(
                id=20001,
                hid=1,
                glparams=[
                    GLParam(id=90, value=[901]),
                    GLParam(id=91, value=33.556),
                    GLParam(id=92, value=0),
                    GLParam(id=93, value=22),  # В репорте сразу значение
                    GLParam(id=94, value=[941]),
                ]
            ),
            VirtualCard(
                id=20002,
                hid=15756910,
                glparams=[
                    GLParam(id=100, value=[2]),
                ],
            ),
        ]

        cls.index.default_experiments += [
            'test_cs_exp_bool=0',
            'test_cs_exp_int=1580'
        ]

    def test_experiments_handler(self):
        '''
        Проверяем работу ручки экспериментов
        Она возвращает текущие дефолтные значения экспов + значения с реаррами
        '''
        # Дефолтные значения
        response = self.market_content_storage.request_json('experiments', method='GET')
        self.assertFragmentIn(
            response,
            {
                "Current default experiments": [
                    {
                        "description": "Test bool exp flag",
                        "name": "test_cs_exp_bool",
                        "value": 0
                    },
                    {
                        "description": "Test bool int flag",
                        "name": "test_cs_exp_int",
                        "value": 1580
                    },
                    {
                        "description": "Test str exp flag",
                        "name": "test_cs_exp_str",
                        "value": "fizzbuzz"  # Значение из кода
                    }
                ],
            }
        )
        # Применение реарров
        response = self.market_content_storage.request_json(
            'experiments?rearr-factors=test_cs_exp_bool=1;test_cs_exp_str=geralt',
            method='GET'
        )
        self.assertFragmentIn(
            response,
            {
                "Experiments with such rearr-flags": [
                    {
                        "description": "Test bool exp flag",
                        "name": "test_cs_exp_bool",
                        "value": 1
                    },
                    {
                        "description": "Test bool int flag",
                        "name": "test_cs_exp_int",
                        "value": 1580
                    },
                    {
                        "description": "Test str exp flag",
                        "name": "test_cs_exp_str",
                        "value": "geralt"
                    }
                ],
            }
        )

    def test_description_is_title_experiment(self):
        '''
        Проверяем экспермент по замене дескрипшна тайтлом
        '''
        request = {
            "model_ids": [1252]
        }
        response = self.market_content_storage.request_json(
            'card_info',
            method='GET',
            body=json.dumps(request)
        )
        self.assertFragmentIn(
            response,
            {
                "models": [
                    {
                        "id": 1252,
                        "description": "Кот",
                        "fullDescription": "Кот полный",
                    }
                ],
            },
        )
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
                        "description": "Кот котовий",
                        "formattedDescription": {
                            "fullHtml": "Кот котовий",
                            "fullPlain": "Кот котовий",
                            "shortHtml": "Кот котовий",
                            "shortPlain": "Кот котовий"
                        }
                    }
                ],
            },
        )

        request = {
            "model_ids": [1252]
        }
        response = self.market_content_storage.request_json(
            'card_info?rearr-factors=description_is_title=1',
            method='GET',
            body=json.dumps(request)
        )
        self.assertFragmentIn(
            response,
            {
                "models": [
                    {
                        "id": 1252,
                        "description": "Меч",
                        "fullDescription": "Меч",
                    }
                ],
            },
        )
        request = {
            "market_skus": [12520]
        }
        response = self.market_content_storage.request_json(
            'card_info?rearr-factors=description_is_title=1',
            method='GET',
            body=json.dumps(request)
        )
        self.assertFragmentIn(
            response,
            {
                "mskus": [
                    {
                        "id": 12520,
                        "description": "Меч золотой",
                        "formattedDescription": {
                            "fullHtml": "Меч золотой",
                            "fullPlain": "Меч золотой",
                            "shortHtml": "Меч золотой",
                            "shortPlain": "Меч золотой"
                        },
                    }
                ],
            },
        )

    def test_video_experiment(self):
        '''
        Поверяем эксперимент с очисткой видео-урлов в ответе
        '''

        request = {
            "model_ids": [1252]
        }
        response = self.market_content_storage.request_json(
            'card_info',
            method='GET',
            body=json.dumps(request)
        )
        self.assertFragmentIn(
            response,
            {
                "models": [
                    {
                        "id": 1252,
                        "video": [
                            "video1",
                            "video2"
                        ],
                    }
                ],
            },
            allow_different_len=False,
            preserve_order=False
        )

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
                        "video": [
                            "video1",
                            "video2"
                        ],
                    }
                ],
            },
            allow_different_len=False,
            preserve_order=False
        )

        request = {
            "model_ids": [1252]
        }
        response = self.market_content_storage.request_json(
            'card_info?rearr-factors=disable_video_urls=1',
            method='GET',
            body=json.dumps(request)
        )
        self.assertFragmentIn(
            response,
            {
                "models": [
                    {
                        "id": 1252,
                        "video": [
                        ],
                    }
                ],
            },
            allow_different_len=False,
            preserve_order=False
        )

        request = {
            "market_skus": [12520]
        }
        response = self.market_content_storage.request_json(
            'card_info?rearr-factors=disable_video_urls=1',
            method='GET',
            body=json.dumps(request)
        )
        self.assertFragmentIn(
            response,
            {
                "mskus": [
                    {
                        "id": 12520,
                        "video": [
                        ],
                    }
                ],
            },
            allow_different_len=False,
            preserve_order=False
        )

    def test_pictures_experiment(self):
        '''
        Проверяем эксперимент с ограничением числа картинок
        '''
        response = self.market_content_storage.request_json('card_info?model_ids=1252')
        self.assertFragmentIn(
            response,
            {
                "models": [
                    {
                        "id": 1252,
                        "pictures": [
                            {
                                "original": {
                                    "key": "picture_1.jpeg",
                                },
                            },
                            {
                                "original": {
                                    "key": "picture_2.jpeg",
                                },
                            },
                            {
                                "original": {
                                    "key": "picture_3.jpeg",
                                },
                            }
                        ],
                    }
                ],
            },
            allow_different_len=False
        )
        request = {
            "market_skus": [12520]
        }
        response = self.market_content_storage.request_json('card_info?market_skus=12520')
        self.assertFragmentIn(
            response,
            {
                "mskus": [
                    {
                        "id": 12520,
                        "pictures": [
                            {
                                "original": {
                                    "key": "picture_sku_1.jpeg",
                                },
                            },
                            {
                                "original": {
                                    "key": "picture_sku_2.jpeg",
                                },
                            }
                        ],
                    }
                ]
            },
            allow_different_len=False
        )

        request = {
            "model_ids": [1252]
        }
        response = self.market_content_storage.request_json(
            'card_info?rearr-factors=limit_card_photos=1',
            method='GET',
            body=json.dumps(request)
        )
        self.assertFragmentIn(
            response,
            {
                "models": [
                    {
                        "id": 1252,
                        "pictures": [
                            {
                                "original": {
                                    "key": "picture_1.jpeg",
                                },
                            },
                        ],
                    }
                ],
            },
            allow_different_len=False
        )

        request = {
            "market_skus": [12520]
        }
        response = self.market_content_storage.request_json(
            'card_info?rearr-factors=limit_card_photos=1',
            method='GET',
            body=json.dumps(request)
        )
        self.assertFragmentIn(
            response,
            {
                "mskus": [
                    {
                        "id": 12520,
                        "pictures": [
                            {
                                "original": {
                                    "key": "picture_sku_1.jpeg",
                                },
                            },
                        ],
                    }
                ]
            },
            allow_different_len=False
        )

        # проверим, что при limit_card_photos=0 удаляются все картинки
        request = {
            "market_skus": [12520]
        }
        response = self.market_content_storage.request_json(
            'card_info?rearr-factors=limit_card_photos=0',
            method='GET',
            body=json.dumps(request)
        )
        self.assertFragmentIn(
            response,
            {
                "mskus": [
                    {
                        "id": 12520,
                        "pictures": [],
                    }
                ]
            },
            allow_different_len=False
        )
        request = {
            "model_ids": [1252]
        }
        response = self.market_content_storage.request_json(
            'card_info?rearr-factors=limit_card_photos=0',
            method='GET',
            body=json.dumps(request)
        )
        self.assertFragmentIn(
            response,
            {
                "models": [
                    {
                        "id": 1252,
                        "pictures": [],
                    }
                ],
            },
            allow_different_len=False
        )

        # Так же проверим логику приоритета реарров из запроса
        # Первый приоритет - из параметров запроса
        # Второй приоритет - из тела запроса
        #
        # Тут возьмем экспы из тела запроса
        request = {
            "model_ids": [1252],
            "rearr-factors": "limit_card_photos=1"
        }
        response = self.market_content_storage.request_json(
            'card_info',
            method='GET',
            body=json.dumps(request)
        )
        self.assertFragmentIn(
            response,
            {
                "models": [
                    {
                        "id": 1252,
                        "pictures": [
                            {
                                "original": {
                                    "key": "picture_1.jpeg",
                                },
                            },
                        ],
                    }
                ],
            },
            allow_different_len=False
        )

        # Тут возьмем экспы из cgi параметров
        request = {
            "model_ids": [1252],
            "rearr-factors": "limit_card_photos=1"
        }
        response = self.market_content_storage.request_json(
            'card_info?rearr-factors=limit_card_photos=2',
            method='GET',
            body=json.dumps(request)
        )
        self.assertFragmentIn(
            response,
            {
                "models": [
                    {
                        "id": 1252,
                        "pictures": [
                            {
                                "original": {
                                    "key": "picture_1.jpeg",
                                },
                            },
                            {
                                "original": {
                                    "key": "picture_2.jpeg",
                                },
                            }
                        ],
                    }
                ]
            },
            allow_different_len=False
        )

    def test_important_params_experiment(self):
        '''
        Проверяем эксперимент, в котором оставляем только "Важные" параметры
        '''
        # Без флага все параметры должны попадать в спеки моделей и скю и в фильтры моделей
        request = {
            "model_ids": [1252],
            "market_skus": [12520],
        }
        response = self.market_content_storage.request_json(
            'card_info',
            method='GET',
            body=json.dumps(request)
        )
        self.assertFragmentIn(
            response,
            {
                "models": [
                    {
                        "id": 1252,
                        "filters": [
                            {
                                "id": 90,
                            },
                            {
                                "id": 91,
                            },
                            {
                                "id": 92,
                            },
                            {
                                "id": 93,
                            },
                            {
                                "id": 94,
                            }
                        ],
                        "specs": {
                            "friendly": [
                                "Меч против чудовищ",
                                "Шириной 10 см"
                            ],
                            "friendlyext": [
                                {
                                    "value": "Меч против чудовищ",
                                    "usedParams": [92]
                                },
                                {
                                    "value": "Шириной 10 см",
                                    "usedParams": [93]
                                }
                            ],
                            "full": [
                                {
                                    "groupSpecs": [
                                        {
                                            "name": "Материал",
                                            "value": "Железный, Серебрянный, Бронзовый (из скю), Алмазный",
                                            "usedParams": [{"id": 90}]
                                        },
                                        {
                                            "name": "Длина",
                                            "value": "55.556 см",
                                            "usedParams": [{"id": 91}]
                                        },
                                        {
                                            "name": "Против чудовищ",
                                            "value": "есть",
                                            "usedParams": [{"id": 92}]
                                        },
                                        {
                                            "name": "Ширина",
                                            "value": "10 см",
                                            "usedParams": [{"id": 93}]
                                        },
                                        {
                                            "name": "Тип",
                                            "value": "Длинный, Двуручный, Короткий",
                                            "usedParams": [{"id": 94}]
                                        }
                                    ]
                                }
                            ]
                        }
                    }
                ],
                "mskus": [
                    {
                        "id": 12520,
                        "specs": {
                            "friendly": [
                                "Меч",
                                "Шириной 11 см"
                            ],
                            "friendlyext": [
                                {
                                    "value": "Меч",
                                    "usedParams": [92]
                                },
                                {
                                    "value": "Шириной 11 см",
                                    "usedParams": [93]
                                },
                            ],
                            "full": [
                                {
                                    "groupSpecs": [
                                        {
                                            "name": "Материал",
                                            "value": "Железный",
                                            "usedParams": [{"id": 90}]
                                        },
                                        {
                                            "name": "Длина",
                                            "value": "48.1 см",
                                            "usedParams": [{"id": 91}]
                                        },
                                        {
                                            "name": "Против чудовищ",
                                            "value": "нет",
                                            "usedParams": [{"id": 92}]
                                        },
                                        {
                                            "name": "Ширина",
                                            "value": "11 см",
                                            "usedParams": [{"id": 93}]
                                        }
                                    ]
                                },
                            ]
                        }
                    }
                ]
            },
            allow_different_len=False
        )

        # С флагом only_important_params должны остаться только "Важные" параметры (90 и 93)
        # В том числе параметры не должны пролезать за счет гипотез и дополнительных параметров из мскю
        # В фильтрах "неважных" параметров совсем не будет, а в шаблонах не будут заполняться значения
        request = {
            "model_ids": [1252],
            "market_skus": [12520],
        }
        response = self.market_content_storage.request_json(
            'card_info?rearr-factors=only_important_params=1',
            method='GET',
            body=json.dumps(request)
        )
        self.assertFragmentIn(
            response,
            {
                "models": [
                    {
                        "id": 1252,
                        "filters": [
                            {
                                "id": 90,
                            },
                            {
                                "id": 93,
                            },
                        ],
                        "specs": {
                            "friendly": [
                                "Меч",
                                "Шириной 10 см"
                            ],
                            "friendlyext": [
                                {
                                    "value": "Меч",
                                    "usedParams": [92]
                                },
                                {
                                    "value": "Шириной 10 см",
                                    "usedParams": [93]
                                },
                            ],
                            "full": [
                                {
                                    "groupSpecs": [
                                        {
                                            "name": "Материал",
                                            "value": "Железный, Серебрянный, Бронзовый (из скю), Алмазный",
                                            "usedParams": [{"id": 90}]
                                        },
                                        {
                                            "name": "Длина",
                                            "value": "-- см",
                                            "usedParams": [{"id": 91}]
                                        },
                                        {
                                            "name": "Ширина",
                                            "value": "10 см",
                                            "usedParams": [{"id": 93}]
                                        },
                                        # Параметр 'Type' и 'ForMonsters' совсем пропадают из full спеков
                                        # Тк весь шаблон состоит только из {<имя параметра>} (без дополнительного текста)
                                        # Вот такой принцип работы ¯\_(ツ)_/¯
                                    ]
                                }
                            ]
                        }
                    }
                ],
                "mskus": [
                    {
                        "id": 12520,
                        "specs": {
                            "friendly": [
                                "Меч",
                                "Шириной 11 см"
                            ],
                            "friendlyext": [
                                {
                                    "value": "Меч",
                                    "usedParams": [92]
                                },
                                {
                                    "value": "Шириной 11 см",
                                    "usedParams": [93]
                                },
                            ],
                            "full": [
                                {
                                    "groupSpecs": [
                                        {
                                            "name": "Материал",
                                            "value": "Железный",
                                            "usedParams": [{"id": 90}]
                                        },
                                        {
                                            "name": "Длина",
                                            "value": "-- см",
                                            "usedParams": [{"id": 91}]
                                        },
                                        {
                                            "name": "Ширина",
                                            "value": "11 см",
                                            "usedParams": [{"id": 93}]
                                        }
                                    ]
                                },
                            ]
                        }
                    }
                ]
            },
            allow_different_len=False
        )

        # Проверяем, что работает с виртуальными/быстрыми
        request = {
            "model_ids": [20001],
            "market_skus": [20001],
        }
        response = self.market_content_storage.request_json(
            'card_info',
            method='GET',
            body=json.dumps(request)
        )
        virtual_specs = {
            "friendly": [
                "Меч",
                "Шириной 22 см"
            ],
            "friendlyext": [
                {
                    "value": "Меч",
                    "usedParams": [92]
                },
                {
                    "value": "Шириной 22 см",
                    "usedParams": [93]
                }
            ],
            "full": [
                {
                    "groupSpecs": [
                        {
                            "name": "Материал",
                            "value": "Железный",
                            "usedParams": [{"id": 90}]
                        },
                        {
                            "name": "Длина",
                            "value": "33.556 см",
                            "usedParams": [{"id": 91}]
                        },
                        {
                            "name": "Против чудовищ",
                            "value": "нет",
                            "usedParams": [{"id": 92}]
                        },
                        {
                            "name": "Ширина",
                            "value": "22 см",
                            "usedParams": [{"id": 93}]
                        },
                        {
                            "name": "Тип",
                            "value": "Длинный",
                            "usedParams": [{"id": 94}]
                        }
                    ]
                }
            ]
        }
        self.assertFragmentIn(
            response,
            {
                "models": [
                    {
                        "id": 20001,
                        "filters": [
                            {
                                "id": 90,
                            },
                            {
                                "id": 91,
                            },
                            {
                                "id": 92,
                            },
                            {
                                "id": 93,
                            },
                            {
                                "id": 94,
                            }
                        ],
                        "specs": virtual_specs
                    }
                ],
                "mskus": [
                    {
                        "id": 20001,
                        "specs": virtual_specs
                    }
                ]
            },
            allow_different_len=False
        )

        # Добавляем флаги
        request = {
            "model_ids": [20001],
            "market_skus": [20001],
        }
        response = self.market_content_storage.request_json(
            'card_info?rearr-factors=only_important_params=1',
            method='GET',
            body=json.dumps(request)
        )
        exp_virtual_specs = {
            "friendly": [
                "Меч",
                "Шириной 22 см"
            ],
            "friendlyext": [
                {
                    "value": "Меч",
                    "usedParams": [92]
                },
                {
                    "value": "Шириной 22 см",
                    "usedParams": [93]
                }
            ],
            "full": [
                {
                    "groupSpecs": [
                        {
                            "name": "Материал",
                            "value": "Железный",
                            "usedParams": [{"id": 90}]
                        },
                        {
                            "name": "Длина",
                            "value": "-- см",
                            "usedParams": [{"id": 91}]
                        },
                        {
                            "name": "Ширина",
                            "value": "22 см",
                            "usedParams": [{"id": 93}]
                        },
                    ]
                }
            ]
        }
        self.assertFragmentIn(
            response,
            {
                "models": [
                    {
                        "id": 20001,
                        "filters": [
                            {
                                "id": 90,
                            },
                            {
                                "id": 93,
                            },
                        ],
                        "specs": exp_virtual_specs
                    }
                ],
                "mskus": [
                    {
                        "id": 20001,
                        "specs": exp_virtual_specs
                    }
                ]
            },
            allow_different_len=False
        )

    def test_delete_all_params_experiment(self):
        '''
        Проверяем эксперимент, в котором отрываем все параметры
        '''
        # Они должны пропасть из спеков моделей/cкю и в фильтров моделей
        request = {
            "model_ids": [1252, 20001],
            "market_skus": [12520, 20001],
        }
        response = self.market_content_storage.request_json(
            'card_info?rearr-factors=del_all_params=1',
            method='GET',
            body=json.dumps(request)
        )
        self.assertFragmentIn(
            response,
            {
                "models": [
                    {
                        "id": 1252,
                        "filters": [],
                        "specs": {
                            "friendly": [
                                "Меч",
                                "Шириной -- см"
                            ],
                            "friendlyext": [
                                {
                                    "value": "Меч",
                                    "usedParams": [92]
                                },
                                {
                                    "value": "Шириной -- см",
                                    "usedParams": [93]
                                }
                            ],
                            "full": [
                                {
                                    "groupSpecs": [
                                        {
                                            "name": "Длина",
                                            "value": "-- см",
                                            "usedParams": [{"id": 91}]
                                        },
                                        {
                                            "name": "Ширина",
                                            "value": "-- см",
                                            "usedParams": [{"id": 93}]
                                        },
                                    ]
                                }
                            ]
                        }
                    },
                    {
                        "id": 20001,
                        "filters": [],
                        "specs": {
                            "friendly": [
                                "Меч",
                                "Шириной -- см"
                            ],
                            "friendlyext": [
                                {
                                    "value": "Меч",
                                    "usedParams": [92]
                                },
                                {
                                    "value": "Шириной -- см",
                                    "usedParams": [93]
                                }
                            ],
                            "full": [
                                {
                                    "groupSpecs": [
                                        {
                                            "name": "Длина",
                                            "value": "-- см",
                                            "usedParams": [{"id": 91}]
                                        },
                                        {
                                            "name": "Ширина",
                                            "value": "-- см",
                                            "usedParams": [{"id": 93}]
                                        },
                                    ]
                                }
                            ]
                        }
                    }
                ],
                "mskus": [
                    {
                        "id": 12520,
                        "specs": {
                            "friendly": [
                                "Меч",
                                "Шириной -- см"
                            ],
                            "friendlyext": [
                                {
                                    "value": "Меч",
                                    "usedParams": [92]
                                },
                                {
                                    "value": "Шириной -- см",
                                    "usedParams": [93]
                                },
                            ],
                            "full": [
                                {
                                    "groupSpecs": [
                                        {
                                            "name": "Длина",
                                            "value": "-- см",
                                            "usedParams": [{"id": 91}]
                                        },
                                        {
                                            "name": "Ширина",
                                            "value": "-- см",
                                            "usedParams": [{"id": 93}]
                                        }
                                    ]
                                },
                            ]
                        }
                    },
                    {
                        "id": 20001,
                        "specs": {
                            "friendly": [
                                "Меч",
                                "Шириной -- см"
                            ],
                            "friendlyext": [
                                {
                                    "value": "Меч",
                                    "usedParams": [92]
                                },
                                {
                                    "value": "Шириной -- см",
                                    "usedParams": [93]
                                }
                            ],
                            "full": [
                                {
                                    "groupSpecs": [
                                        {
                                            "name": "Длина",
                                            "value": "-- см",
                                            "usedParams": [{"id": 91}]
                                        },
                                        {
                                            "name": "Ширина",
                                            "value": "-- см",
                                            "usedParams": [{"id": 93}]
                                        },
                                    ]
                                }
                            ]
                        }
                    }
                ]
            },
            allow_different_len=False
        )

    def test_params_exp_ban_list(self):
        for exp in ['del_all_params', 'only_important_params']:
            request = {
                "model_ids": [1253, 20002],
            }
            response = self.market_content_storage.request_json(
                'card_info?rearr-factors={}=1'.format(exp),
                method='GET',
                body=json.dumps(request)
            )
            self.assertFragmentIn(
                response,
                {
                    "models": [
                        {
                            "id": 1253,
                            "filters": [
                                {
                                    "id": 100,
                                    "values": [{"value": "От головы"}],
                                },
                            ],
                        },
                        {
                            "id": 20002,
                            "filters": [
                                {
                                    "id": 100,
                                    "values": [{"value": "От горла"}],
                                },
                            ],
                        }
                    ],
                },
                allow_different_len=False
        )


if __name__ == '__main__':
    main()

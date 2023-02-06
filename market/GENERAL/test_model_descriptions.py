#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa

from core.matcher import Absent, NotEmpty
from core.types import (
    Book,
    Const,
    GLParam,
    GLType,
    GLValue,
    HyperCategory,
    Model,
    ModelDescriptionTemplates,
    ModelGroup,
    Offer,
)
from core.testcase import TestCase, main
from core.matcher import Contains


class T(TestCase):
    @classmethod
    def prepare(cls):
        """
        Создаем категорию и задаем в ней шаблоны описаний моделей, подробнее про различные виды шаблонов:
        https://st.yandex-team.ru/MARKETOUT-10310#1476996308000
        """

        cls.index.model_description_templates += [
            ModelDescriptionTemplates(
                hid=23,
                micromodel="{Type}, {*comma-on}{Retina:ретина}{2SimCards:2 сим карты}{WiFi:вайфай}{*comma-off}",
                friendlymodel=[
                    "Телефон {Retina#ifnz}{Retina:с ретиной}{#endif}",
                    "{2SimCards:с 2-мя сим-картами}",
                    "{InfraRedPort#ifz}без инфракрасного порта{#endif}",
                    ["", "{InfraRedPort#ifz}нет{#else}да{#endif}", "Инфракрасный порт"],
                ],
                model=[
                    (
                        "Технические характеристики",
                        {
                            "Ретина": "{Retina}",
                            "2 сим-карты": "{2SimCards}",
                            "Диагональ экрана": "{DisplaySize}",
                            "ОС": "{OS}",
                            "Rare Parameter": "{RareParam}",
                            "Empty Parameter": "{EmptyParam}",
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
            ModelDescriptionTemplates(hid=25, micromodel="{ somefunc() ;  #exec}"),
        ]

        # Создаем фильтры, на которые ссылаются в шаблонах
        cls.index.gltypes += [
            GLType(hid=23, param_id=100, xslname="Retina", gltype=GLType.BOOL),
            GLType(hid=23, param_id=101, xslname="2SimCards", gltype=GLType.BOOL),
            GLType(hid=23, param_id=102, xslname="InfraRedPort", gltype=GLType.BOOL),
            GLType(hid=23, param_id=103, xslname="WiFi", gltype=GLType.BOOL),
            GLType(hid=23, param_id=104, xslname="DisplaySize", gltype=GLType.NUMERIC),
            GLType(
                hid=23,
                param_id=105,
                xslname="OS",
                gltype=GLType.ENUM,
                values=[GLValue(value_id=1, text='iOS'), GLValue(value_id=2, text='Android')],
            ),
            GLType(
                hid=23,
                param_id=106,
                xslname="Type",
                gltype=GLType.ENUM,
                values=[GLValue(value_id=1, text='мобильный телефон'), GLValue(value_id=2, text='смартфон')],
            ),
            GLType(hid=23, param_id=107, xslname="AdditionalInfo", gltype=GLType.STRING),
            GLType(hid=23, param_id=108, xslname="RareParam", gltype=GLType.STRING),
            GLType(hid=23, param_id=109, xslname="EmptyParam", gltype=GLType.STRING),
            GLType(hid=23, param_id=Const.MODELS_VIDEO_PARAM_ID, xslname="models_video", gltype=GLType.STRING),
        ]

        # Создаем две модели: с параметрами в категорию, где описаны шаблоны и без параметров в категории без шаблонов
        cls.index.models += [
            Model(
                hid=23,
                title="gumoful model",
                hyperid=1,
                description="NOT_SHOULD_SEE_THIS",
                glparams=[
                    GLParam(param_id=100, value=1),
                    GLParam(param_id=101, value=0),
                    GLParam(param_id=103, value=1),
                    GLParam(param_id=104, value=4.7),
                    GLParam(param_id=105, value=1),
                    GLParam(param_id=106, value=2),
                    GLParam(param_id=107, string_value="Наушники EarPods в комплекте"),
                    GLParam(param_id=109, string_value=""),
                    GLParam(param_id=Const.MODELS_VIDEO_PARAM_ID, string_value="video_url_0"),
                    GLParam(param_id=Const.MODELS_VIDEO_PARAM_ID, string_value="video_url_1"),
                    GLParam(param_id=Const.MODELS_VIDEO_PARAM_ID, string_value="video_url_0"),
                    GLParam(param_id=Const.MODELS_VIDEO_PARAM_ID, string_value="video_url_1"),
                    GLParam(param_id=Const.MODELS_VIDEO_PARAM_ID, string_value="video_url_2"),
                    GLParam(param_id=Const.MODELS_VIDEO_PARAM_ID, string_value="video_url_3"),
                    GLParam(param_id=Const.MODELS_VIDEO_PARAM_ID, string_value="video_url_3"),
                    GLParam(param_id=Const.MODELS_VIDEO_PARAM_ID, string_value="video_url_2"),
                    GLParam(param_id=Const.MODELS_VIDEO_PARAM_ID, string_value="video_url_3"),
                    GLParam(param_id=Const.MODELS_VIDEO_PARAM_ID, string_value="video_url_4"),
                    GLParam(param_id=Const.MODELS_VIDEO_PARAM_ID, string_value="video_url_0"),
                ],
            ),
            Model(hid=24, title="model", hyperid=2, description="SHOULD_SEE_THIS"),
            Model(
                hid=23,
                title="multivalue gumoful model",
                hyperid=3,
                glparams=[
                    GLParam(param_id=105, value=1),
                    GLParam(param_id=105, value=2),
                ],
            ),
            Model(
                hid=25,
                title="wrong template",
                hyperid=4,
            ),
            Model(hid=23, hyperid=5, video=['video_5_1', 'video_5_2']),
            Model(
                hid=23,
                hyperid=6,
                video=['video_6_1', 'video_6_2'],
                glparams=[
                    GLParam(param_id=Const.MODELS_VIDEO_PARAM_ID, string_value='video_6_1'),
                    GLParam(param_id=Const.MODELS_VIDEO_PARAM_ID, string_value='video_6_2'),
                ],
            ),
        ]

        # создаем необходимое количество офферов для модели, чтобы показался колдунщик модели
        cls.index.offers += [Offer(hyperid=1, title='Offer %s' % x, price=400) for x in range(10)]

    def test_models_video(self):
        """
        Видео модели - параметр models_video из мбо. Пока планируется использовать только на КМ.
        Поэтому, что бы лишний раз не десерилизовать MboModel, достаю этот параметр рядом с рендерингом шаблонов.
        Без show-models-specs видео показываться не будет
        """

        def gen_req(modelid, spec=None):
            req = 'place=modelinfo&hyperid={}&rids=0'.format(modelid)
            return req if spec is None else req + '&show-models-specs={}'.format(spec)

        response = self.report.request_json(gen_req(1))
        self.assertFragmentIn(
            response,
            {
                "id": 1,
                "specs": NotEmpty(),
                "video": Absent(),
            },
        )

        for spec in ('friendly', 'full'):
            # у модели 1 видео в старом формате
            response = self.report.request_json(gen_req(1, spec))
            self.assertFragmentIn(
                response,
                {
                    "id": 1,
                    "specs": NotEmpty(),
                    "video": [
                        "video_url_0",
                        "video_url_1",
                        "video_url_2",
                        "video_url_3",
                        "video_url_4",
                    ],
                },
                preserve_order=True,
                allow_different_len=False,  # у модели не должны быть дубли в video + важен порядок
            )

            # у модели 5 видео в новом формате
            response = self.report.request_json(gen_req(5, spec))
            self.assertFragmentIn(
                response,
                {
                    "id": 5,
                    "specs": NotEmpty(),
                    "video": [
                        "video_5_1",
                        "video_5_2",
                    ],
                },
                preserve_order=True,
                allow_different_len=False,
            )

            # модель 6 - переходная, видео дублируется в обоих полях, но дубликаты должны удаляться
            response = self.report.request_json(gen_req(6, spec))
            self.assertFragmentIn(
                response,
                {
                    "id": 6,
                    "specs": NotEmpty(),
                    "video": [
                        "video_6_1",
                        "video_6_2",
                    ],
                },
                preserve_order=True,
                allow_different_len=False,
            )

        # у модели 3 нет видео
        response = self.report.request_json(gen_req(3, 'friendly'))
        self.assertFragmentIn(
            response,
            {
                "id": 3,
                "specs": NotEmpty(),
                "video": Absent(),
            },
        )

        # Под флагом disable_video_urls видео должны отсутствовать
        for model_id in [1, 5, 6]:
            response = self.report.request_json(gen_req(model_id, spec) + '&rearr-factors=disable_video_urls=1')
            self.assertFragmentIn(
                response,
                {
                    "id": model_id,
                    "video": Absent(),
                },
                preserve_order=True,
                allow_different_len=False,
            )

    def test_model_micro_descrtiption(self):
        """
        Model description now must be generated using category MICROMODEL template if such exist, otherwise
        old description field is used.
        """
        response = self.report.request_json('place=prime&text=model')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"id": 1, "description": "смартфон, ретина, вайфай"},
                    {"id": 2, "description": "SHOULD_SEE_THIS"},
                ]
            },
        )
        self.assertFragmentNotIn(response, "NOT_SHOULD_SEE_THIS")

        # Same tests for pararallel. Request is longer in order to call wizard
        response = self.report.request_bs('place=parallel&text=gumoful model')
        self.assertFragmentIn(
            response,
            {
                "market_model": [
                    {
                        "title": {"__hl": {"text": "Gumoful model", "raw": True}},
                        "text": [{"__hl": {"text": "смартфон, ретина, вайфай", "raw": True}}],
                    }
                ]
            },
        )

    def test_lingua_descriptions(self):
        """
        SEO-описания рендерятся по шаблону seo, записываются в объект lingua
        """

        response = self.report.request_json('place=prime&text=model')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "id": 1,
                        "lingua": {
                            "type": {
                                "nominative": "смартфон-nominative",
                                "genitive": "смартфон-genitive",
                                "dative": "смартфон-dative",
                                "accusative": "смартфон-accusative",
                            }
                        },
                    }
                ]
            },
        )

    def test_friendly_descriptions(self):
        """
        Friendly описания рендерятся в виде массива строк по шаблону friendlymodel
        Мы должны явно указать параметр &show-models-specs=friendly, чтобы отрендерить их
        Если отрендеренная характеристика оказалась пустой ('с 2-мя сим-картами'), мы должны ее игнорировать и не рендерить
        (allow_different_len=False проверяет это)
        У модели без шаблона характеристика отсутствует
        """
        response = self.report.request_json('place=prime&text=model&show-models-specs=friendly')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "id": 1,
                        "specs": {
                            "friendly": [
                                "Телефон с ретиной",
                                "без инфракрасного порта",
                                "Инфракрасный порт: нет",
                            ],
                            "friendlyext": [
                                {
                                    "type": "spec",
                                    "value": "Телефон с ретиной",
                                    "usedParams": [100],
                                    "usedParamsWithValues": [
                                        {"id": 100, "values": [{"isFilterable": True, "value": "1"}]}
                                    ],
                                },
                                {
                                    "type": "spec",
                                    "value": "без инфракрасного порта",
                                    "usedParams": [102],
                                    "usedParamsWithValues": [
                                        {"id": 102, "values": [{"isFilterable": True, "value": "0"}]}
                                    ],
                                },
                                {
                                    "title": "Инфракрасный порт",
                                    "type": "spec",
                                    "value": "нет",
                                    "usedParams": [102],
                                    "usedParamsWithValues": [
                                        {"id": 102, "values": [{"isFilterable": True, "value": "0"}]}
                                    ],
                                },
                            ],
                        },
                    },
                    {
                        "id": 2,
                        "specs": {"friendly": Absent()},
                    },
                    {"id": 3, "specs": {"friendly": ["Телефон", "без инфракрасного порта", "Инфракрасный порт: нет"]}},
                ]
            },
            allow_different_len=False,
        )

        # Без &show-models-specs=friendly specs отсутствуют
        response = self.report.request_json('place=prime&text=model')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"id": 1, "specs": {"friendly": Absent()}},
                    {"id": 2, "specs": {"friendly": Absent()}},
                    {"id": 3, "specs": {"friendly": Absent()}},
                ]
            },
        )

    def test_full_descriptions(self):
        """
        Full описания рендерятся в виде объекта full по шаблону
        https://github.yandex-team.ru/market/microformats/blob/feature/guru-card-in-report/product/product.sample.js#L118
        Мы должны явно указать параметр &show-models-specs=full, чтобы отрендерить их

        Характеристики с отсутствующим значением (RareParam) и пустым значением (EmptyParam) рендериться не должны
        Если секция состоит целиком из пустых характеристик ('Секция с пустыми параметрами'), она рендериться не должна
        (allow_different_len=False проверяет все это)
        """

        response = self.report.request_json('place=modelinfo&hyperid=1&rids=0&show-models-specs=full')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "id": 1,
                        "specs": {
                            "full": [
                                {
                                    "groupName": "Технические характеристики",
                                    "groupSpecs": [
                                        {
                                            "desc": "Ретина parameter description",
                                            "name": "Ретина",
                                            "usedParams": [{"id": 100, "name": "GLPARAM-100"}],
                                            "usedParamsWithValues": [
                                                {
                                                    "id": 100,
                                                    "name": "GLPARAM-100",
                                                    "values": [{"isFilterable": True, "value": "1"}],
                                                }
                                            ],
                                            "value": "есть",
                                        },
                                        {
                                            "desc": "ОС parameter description",
                                            "name": "ОС",
                                            "usedParams": [{"id": 105, "name": "GLPARAM-105"}],
                                            "usedParamsWithValues": [
                                                {
                                                    "id": 105,
                                                    "name": "GLPARAM-105",
                                                    "values": [{"isFilterable": True, "value": "1"}],
                                                }
                                            ],
                                            "value": "iOS",
                                        },
                                        {
                                            "desc": "Диагональ экрана parameter description",
                                            "name": "Диагональ экрана",
                                            "usedParams": [{"id": 104, "name": "GLPARAM-104"}],
                                            "usedParamsWithValues": [
                                                {
                                                    "id": 104,
                                                    "name": "GLPARAM-104",
                                                    "values": [{"isFilterable": True, "value": "4.7"}],
                                                }
                                            ],
                                            "value": "4.7",
                                        },
                                        {
                                            "desc": "2 сим-карты parameter description",
                                            "name": "2 сим-карты",
                                            "usedParams": [{"id": 101, "name": "GLPARAM-101"}],
                                            "usedParamsWithValues": [
                                                {
                                                    "id": 101,
                                                    "name": "GLPARAM-101",
                                                    "values": [{"isFilterable": True, "value": "0"}],
                                                }
                                            ],
                                            "value": "нет",
                                        },
                                    ],
                                },
                                {
                                    "groupName": "Прочее",
                                    "groupSpecs": [
                                        {
                                            "desc": "Дополнительная информация parameter description",
                                            "name": "Дополнительная информация",
                                            "usedParams": [{"id": 107, "name": "GLPARAM-107"}],
                                            "usedParamsWithValues": [
                                                {
                                                    "id": 107,
                                                    "name": "GLPARAM-107",
                                                    "values": [
                                                        {"isFilterable": False, "value": "Наушники EarPods в комплекте"}
                                                    ],
                                                }
                                            ],
                                            "value": "Наушники EarPods в комплекте",
                                        }
                                    ],
                                },
                            ]
                        },
                    }
                ]
            },
            allow_different_len=False,
        )

        # У модели без шаблона характеристика по-прежнему отсутствует
        response = self.report.request_json('place=modelinfo&hyperid=2&&rids=0&show-models-specs=full')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "id": 2,
                        "specs": {"full": Absent()},
                    }
                ]
            },
        )

        # Без  &show-models-specs=full specs отсутствуют
        response = self.report.request_json('place=modelinfo&hyperid=1&&rids=0')
        self.assertFragmentIn(response, {"results": [{"id": 1, "specs": {"full": Absent()}}]})

        # мультизначения рендерятся через запятую в порядке возрастания ID ENUM-а
        # в usedParamsWithValues будет два значения для одного параметра
        response = self.report.request_json('place=modelinfo&hyperid=3&rids=0&show-models-specs=full')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "id": 3,
                        "specs": {
                            "full": [
                                {
                                    "groupName": "Технические характеристики",
                                    "groupSpecs": [
                                        {
                                            "desc": "ОС parameter description",
                                            "name": "ОС",
                                            "usedParams": [{"id": 105, "name": "GLPARAM-105"}],
                                            "usedParamsWithValues": [
                                                {
                                                    "id": 105,
                                                    "name": "GLPARAM-105",
                                                    "values": [
                                                        {"isFilterable": True, "value": "1"},
                                                        {"isFilterable": True, "value": "2"},
                                                    ],
                                                }
                                            ],
                                            "value": "iOS, Android",
                                        },
                                    ],
                                },
                            ],
                        },
                    },
                ],
            },
        )

    def test_multiple_descriptions(self):
        """
        В show-models-specs можно указывать несколько типо описаний
        Убедимся, что без show-models-specs описания пусты
        """

        response = self.report.request_json('place=modelinfo&hyperid=1&rids=0')
        self.assertFragmentIn(response, {"results": [{"id": 1, "specs": {"full": Absent(), "friendly": Absent()}}]})

        # А с show-models-specs оба описания не пусты
        response = self.report.request_json('place=modelinfo&hyperid=1&rids=0&show-models-specs=friendly,full')
        self.assertFragmentIn(response, {"results": [{"id": 1, "specs": {"full": NotEmpty(), "friendly": NotEmpty()}}]})

    @classmethod
    def prepare_book_micro_description(cls):
        """
        Создаем две книжки: одну с полным описанием, вторую с частичным (отсутствует серия)
        """
        cls.index.books += [
            Book(
                hid=30,
                hyperid=20,
                title="book_with_full_description",
                description="book with full description",
                isbn="123",
                author="lognick",
                publisher="AST",
                publishing_year='2005',
                series='world fantasy',
            ),
            Book(
                hid=30,
                hyperid=21,
                title="book_without_series",
                description="book without series",
                isbn="124",
                author="lognick",
                publisher="PUB",
                publishing_year='2004',
            ),
        ]

    def test_book_micro_description(self):
        """
        Тестируем, что микро-описания берутся из поля description
        """
        response = self.report.request_json('place=prime&hid=30')
        self.assertFragmentIn(
            response,
            {"results": [{"description": "book with full description"}, {"description": "book without series"}]},
        )

    def test_book_friendly_description(self):
        """
        Тестируем, что friendly-описание рендерится правильно:
        1. Параметры в нужном порядке
        2. У каждого параметра правильное название
        """
        response = self.report.request_json(
            'place=prime&hid=30&text=book_with_full_description&show-models-specs=friendly'
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "specs": {
                            "friendly": [
                                "ISBN: 123",
                                "Автор: lognick",
                                "Издательство: AST",
                                "Серия: world fantasy",
                                "Год издания: 2005",
                                "book with full description",
                            ]
                        }
                    }
                ]
            },
            preserve_order=True,
        )

        # проверяем описания в friendlyext
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "specs": {
                            "friendlyext": [
                                {"type": "spec", "key": "ISBN", "value": "123", "usedParams": []},
                                {"type": "spec", "key": "Автор", "value": "lognick", "usedParams": []},
                                {"type": "spec", "key": "Издательство", "value": "AST", "usedParams": []},
                                {"type": "spec", "key": "Серия", "value": "world fantasy", "usedParams": []},
                                {"type": "spec", "key": "Год издания", "value": "2005", "usedParams": []},
                                {"type": "desc", "description": "book with full description"},
                            ]
                        }
                    }
                ]
            },
            preserve_order=True,
        )

        # У описания книги, у которой нет серии, отсутствует рендеринг серии
        response = self.report.request_json('place=prime&hid=30&text=book_without_series&show-models-specs=friendly')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "specs": {
                            "friendly": [
                                "ISBN: 124",
                                "Автор: lognick",
                                "Издательство: PUB",
                                "Год издания: 2004",
                                "book without series",
                            ]
                        }
                    }
                ]
            },
            preserve_order=True,
        )

    def test_book_full_description(self):
        """
        Тестируем full-описание книги, которая заполнена полностью.
        """
        response = self.report.request_json('place=modelinfo&hyperid=20&rids=0&show-models-specs=full')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "specs": {
                            "full": [
                                {
                                    "groupName": "Общие характеристики",
                                    "groupSpecs": [
                                        {"name": "ISBN", "value": "123"},
                                        {"name": "Автор", "value": "lognick"},
                                        {"name": "Издательство", "value": "AST"},
                                        {"name": "Серия", "value": "world fantasy"},
                                        {"name": "Год издания", "value": "2005"},
                                    ],
                                },
                                {
                                    "groupName": "Описание",
                                    "groupSpecs": [{"name": Absent(), "value": "book with full description"}],
                                },
                            ]
                        }
                    }
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # Тестируем full-описание книги, у которой не заполнена серия. Серия должна отсутствовать в описании.
        response = self.report.request_json('place=modelinfo&hyperid=21&rids=0&show-models-specs=full')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "specs": {
                            "full": [
                                {
                                    "groupName": "Общие характеристики",
                                    "groupSpecs": [
                                        {"name": "ISBN", "value": "124"},
                                        {"name": "Автор", "value": "lognick"},
                                        {"name": "Издательство", "value": "PUB"},
                                        {"name": "Год издания", "value": "2004"},
                                    ],
                                },
                                {
                                    "groupName": "Описание",
                                    "groupSpecs": [{"name": Absent(), "value": "book without series"}],
                                },
                            ]
                        }
                    }
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

    @classmethod
    def prepare_model_groups_description(cls):
        """
        0. Задаем шаблон для full-характеристик (desgin_group_params)
        1. Создаем групповую категорию, в range_fields перечислям свойства, который должны отображаться в micro-описании
        и friendly карточки
        2. В seo задаем шаблон, который будет использоваться для  lingua-карточки
        """

        design_group_params = '''
        <category name='notebooks'>
        <b-params name='Тип'>
         <value>OS</value>
        </b-params>

        <b-params name='Процессор'>
         <value>ProcType</value>
         <value>ProcCore</value>
         <value>CacheL2</value>
         <value>NegativeParameter</value>
        </b-params>

        <b-params name='Память'>
         <value>Memory</value>
         <value>HDD</value>
        </b-params>

        <b-params name='Интерфейсы'>
         <value>WiFi</value>
         <value>LTE</value>
         <value>DVD</value>
         <value>Bluetooth</value>
         <value>bluray</value>
         <value>RareBooleanParameter</value>
        </b-params>

        <b-params name='Секция с пустыми параметрами'>
         <value>RareBooleanParameter</value>
         <value>RareEnumParameter</value>
         <value>RareNumericParameter</value>
        </b-params>
        <b-params name='Особенности'>
         <value>additionalinfo</value>
        </b-params>
       </category>
        '''

        cls.index.hypertree += [
            HyperCategory(
                hid=40,
                has_groups=True,
                range_fields=[
                    "ProcType",
                    "OS",
                    "GraphicsCard",
                    "ProcFreq",
                    "Memory",
                    "HDD",
                    "DVD",
                    "Bluetooth",
                    "WiFi",
                    "LTE",
                ],
                design_group_params=design_group_params,
            )
        ]

        cls.index.model_description_templates += [ModelDescriptionTemplates(hid=40, seo="{return $ProcType; #exec}")]

        '''Создаем 3 группы параметров (по группе на каждый тип - enum, bool, numeric)
        В каждой из этих групп есть параметр:
            1. Который есть только у групповой модели
            2. Который есть только у модификаций, и он среди них отличается
            3. Который есть только у модификаций, но его значение всегда одинаково
        '''

        cls.index.gltypes += [
            # enum только у группы
            GLType(
                param_id=401,
                hid=40,
                xslname="ProcType",
                name=u"Процессор",
                gltype=GLType.ENUM,
                values=[
                    GLValue(value_id=1, text="Core i5"),
                    GLValue(value_id=2, text="Core i7"),
                ],
            ),
            # enum у модификаций, значения будут разными
            GLType(
                param_id=402,
                hid=40,
                xslname="OS",
                name=u"ОС",
                gltype=GLType.ENUM,
                values=[
                    GLValue(value_id=1, text="MacOS"),
                    GLValue(value_id=2, text="Windows"),
                ],
                has_description=False,
            ),
            # enum у модификаций, значения будут одинаковыми
            GLType(
                param_id=403,
                hid=40,
                xslname="GraphicsCard",
                name=u"Видеокарта",
                gltype=GLType.ENUM,
                values=[
                    GLValue(value_id=1, text="NVIDIA GeForce"),
                    GLValue(value_id=2, text="ATI Radeon"),
                ],
            ),
            # numeric только у группы
            GLType(
                param_id=404,
                hid=40,
                xslname="ProcFreq",
                name=u"Частота процессора",
                gltype=GLType.NUMERIC,
                unit_name="Мгц",
            ),
            # numeric у модификаций, значения будут разными
            GLType(param_id=405, hid=40, xslname="Memory", name=u"ОЗУ", gltype=GLType.NUMERIC, unit_name="Гб"),
            # numeric у модификаций, значения будут одинаковыми
            GLType(param_id=406, hid=40, xslname="HDD", name=u"HDD", gltype=GLType.NUMERIC, unit_name="Gb"),
            # bool только у группы
            GLType(param_id=407, hid=40, xslname="DVD", name="DVD", gltype=GLType.BOOL),
            # bool у модификаций, значения будут разными
            GLType(param_id=408, hid=40, xslname="Bluetooth", name="Bluetooth", gltype=GLType.BOOL),
            # bool у модификаций, значения будут одинаковыми (true)
            GLType(param_id=409, hid=40, xslname="WiFi", name="WiFi", gltype=GLType.BOOL),
            # bool у модификаций, значения будут одинаковыми (false)
            GLType(param_id=410, hid=40, xslname="LTE", name="LTE", gltype=GLType.BOOL),
        ]

        # Параметры, которые есть только в full-шаблоне (designGroupParams):
        cls.index.gltypes += [
            # 1. Группа несуществующих параметров у модификаций
            GLType(
                param_id=411, hid=40, xslname="RareBooleanParameter", name="RareBooleanParameter", gltype=GLType.BOOL
            ),
            GLType(
                param_id=412,
                hid=40,
                xslname="RareEnumParameter",
                name="RareEnumParameter",
                gltype=GLType.ENUM,
                values=[1, 2, 3],
            ),
            GLType(
                param_id=413, hid=40, xslname="RareNumericParameter", name="RareNumericParameter", gltype=GLType.NUMERIC
            ),
            # 2. Группа присутствующих параметров у модификаций
            GLType(param_id=414, hid=40, xslname="bluray", name="Blu-Ray", gltype=GLType.BOOL),
            GLType(
                param_id=415,
                hid=40,
                xslname="ProcCore",
                name=u"Ядро процессора",
                gltype=GLType.ENUM,
                values=[GLValue(value_id=1, text="Broadwell"), GLValue(value_id=2, text="Haswell")],
            ),
            GLType(
                param_id=416, hid=40, xslname="CacheL2", name=u"Объем кэша L2", unit_name="Кб", gltype=GLType.NUMERIC
            ),
            GLType(
                param_id=417, hid=40, xslname="additionalinfo", name=u"Дополнительная информация", gltype=GLType.STRING
            ),
            GLType(
                param_id=418,
                hid=40,
                xslname="NegativeParameter",
                name=u"Отрицательное нечто",
                unit_name="Кг",
                gltype=GLType.NUMERIC,
            ),
        ]

        '''
        Создаем групповую модель и две модификации к ней, присваиваем сущностям параметры в соответствие со схемой,
        описанной выше
        '''
        cls.index.model_groups += [
            ModelGroup(
                hid=40,
                title='asus notebook groupspec',
                hyperid=4000,
                glparams=[
                    GLParam(param_id=401, value=1),
                    GLParam(param_id=404, value=2200),
                    GLParam(param_id=407, value=0),
                ],
            )
        ]

        cls.index.models += [
            Model(
                hyperid=4001,
                hid=40,
                title='asus notebook',
                group_hyperid=4000,
                glparams=[
                    GLParam(param_id=402, value=2),
                    GLParam(param_id=403, value=1),
                    GLParam(param_id=405, value=1.024),
                    GLParam(param_id=406, value=512),
                    GLParam(param_id=408, value=0),
                    GLParam(param_id=409, value=1),
                    GLParam(param_id=410, value=0),
                    GLParam(param_id=414, value=0),
                    GLParam(param_id=415, value=1),
                    GLParam(param_id=416, value=512),
                    GLParam(param_id=417, string_value="Подписка на Office"),
                    GLParam(param_id=418, value=-66.77),
                ],
            ),
            Model(
                hyperid=4002,
                hid=40,
                title='asus notebook',
                group_hyperid=4000,
                glparams=[
                    GLParam(param_id=402, value=1),
                    GLParam(param_id=403, value=1),
                    GLParam(param_id=405, value=2.048),
                    GLParam(param_id=406, value=512),
                    GLParam(param_id=408, value=1),
                    GLParam(param_id=409, value=1),
                    GLParam(param_id=410, value=0),
                    GLParam(param_id=414, value=1),
                    GLParam(param_id=415, value=2),
                    GLParam(param_id=416, value=128),
                    GLParam(param_id=417, string_value="Подписка на Photoshop"),
                    GLParam(param_id=418, value=-77.66),
                ],
            ),
            # добавляем еще 3 модификации, чтобы сработал колдунщик групповой модели
            Model(hyperid=4003, hid=40, title='asus notebook', group_hyperid=4000),
            Model(hyperid=4004, hid=40, title='asus notebook', group_hyperid=4000),
            Model(hyperid=4005, hid=40, title='asus notebook', group_hyperid=4000),
        ]

    def test_model_group_micro_description(self):
        """
        Проверяем, что:
        1. Все параметры нашлись и присутствуют в описании
        2. Для bool параметров в зависимости от значений в модификациях выводится соответствующее описание
        3. Для enum параметров выводятся значения в модификациях через / или просто значение, если оно одно
        4. Для numeric параметров выводится минимальное-максимальное значение или просто значение, если оно одно
        """

        response = self.report.request_json('place=prime&hid=40&text=asus')
        self.assertFragmentIn(
            response,
            {
                "description": u"Core i5, MacOS / Windows, NVIDIA GeForce, 2200 Мгц, 1.024-2.048 Гб, 512 Gb, DVD \u2014 нет, "
                u"Bluetooth (опционально), WiFi, LTE \u2014 нет"
            },
        )

    def test_model_group_lingua_description(self):
        """
        SEO-описания рендерятся по шаблону seo, записываются в объект lingua
        """
        response = self.report.request_json('place=modelinfo&hyperid=4000&rids=0')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "id": 4000,
                        "lingua": {
                            "type": {
                                "nominative": "Core i5-nominative",
                                "genitive": "Core i5-genitive",
                                "dative": "Core i5-dative",
                                "accusative": "Core i5-accusative",
                            }
                        },
                    }
                ]
            },
        )

    def test_model_group_friendly_description(self):
        """
        Проверяем, что:
        1. Все параметры нашлись и присутствуют в описании
        2. Для bool параметров в зависимости от значений в модификациях выводится соответствующее описание
        3. Для enum параметров выводятся значения в модификациях через / или просто значение, если оно одно
        4. Для numeric параметров выводится минимальное-максимальное значение или просто значение, если оно одно
        5. Каждый параметр предваряется именем
        """
        response = self.report.request_json('place=modelinfo&hyperid=4000&rids=0&show-models-specs=friendly')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "id": 4000,
                        "specs": {
                            "friendly": [
                                "Процессор: Core i5",
                                "ОС: MacOS / Windows",
                                "Видеокарта: NVIDIA GeForce",
                                "Частота процессора: 2200 Мгц",
                                "ОЗУ: 1.024...2.048 Гб",
                                "HDD: 512 Gb",
                                "DVD: нет",
                                "Bluetooth: опционально",
                                "WiFi: есть",
                                "LTE: нет",
                            ]
                        },
                    }
                ]
            },
            allow_different_len=False,
        )

    def test_model_group_full_description(self):
        """
        Full описания рендерятся в виде объекта full по шаблону
        https://github.yandex-team.ru/market/microformats/blob/feature/guru-card-in-report/product/product.sample.js#L118
        Мы должны явно указать параметр &show-models-specs=full, чтобы отрендерить их

        Характеристики с отсутствующим значением (RareParams) рендериться не должны
        Если секция состоит целиком из пустых характеристик ('Секция с пустыми параметрами'), она рендериться не должна
        (allow_different_len=False проверяет все это)

        Как обычно, проверяем, что:
        1. Все параметры нашлись и присутствуют в описании
        2. Для bool параметров в зависимости от значений в модификациях выводится соответствующее описание
        3. Для enum и string параметров выводятся значения в модификациях через / или просто значение, если оно одно
        4. Для numeric параметров выводится минимальное-максимальное значение или просто значение, если оно одно
        """

        response = self.report.request_json('place=modelinfo&hyperid=4000&rids=0&show-models-specs=full')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "id": 4000,
                        "specs": {
                            "full": [
                                {"groupName": "Тип", "groupSpecs": [{"name": "ОС", "value": "MacOS / Windows"}]},
                                {
                                    "groupName": "Процессор",
                                    "groupSpecs": [
                                        {"name": "Процессор", "value": "Core i5"},
                                        {"name": "Ядро процессора", "value": "Broadwell / Haswell"},
                                        {"name": "Объем кэша L2", "value": "128...512 Кб"},
                                        {"name": "Отрицательное нечто", "value": "-77.66...-66.77 Кг"},
                                    ],
                                },
                                {
                                    "groupName": "Память",
                                    "groupSpecs": [
                                        {"name": "ОЗУ", "value": "1.024...2.048 Гб"},
                                        {"name": "HDD", "value": "512 Gb"},
                                    ],
                                },
                                {
                                    "groupName": "Интерфейсы",
                                    "groupSpecs": [
                                        {"name": "WiFi", "value": "есть"},
                                        {"name": "LTE", "value": "нет"},
                                        {"name": "DVD", "value": "нет"},
                                        {"name": "Bluetooth", "value": "опционально"},
                                        {"name": "Blu-Ray", "value": "опционально"},
                                    ],
                                },
                                {
                                    "groupName": "Особенности",
                                    "groupSpecs": [
                                        {
                                            "name": "Дополнительная информация",
                                            "value": "Подписка на Office / Подписка на Photoshop",
                                        }
                                    ],
                                },
                            ]
                        },
                    }
                ]
            },
            allow_different_len=False,
            preserve_order=True,
        )

    @classmethod
    def prepare_empty_friendly_description(cls):
        """
        Задаем шаблон для friendly-описаний с символами пробелов внутри
        Создаем параметры, упомянутые в шаблоне
        Создаем модель с одним из этих параметров
        """
        cls.index.model_description_templates += [
            ModelDescriptionTemplates(
                hid=50,
                friendlymodel=[
                    " \n{2SimCards:с 2-мя сим-картами}\n{WiFi:+вайфай}\n",
                    "{Retina#ifnz}{Retina:Телефон с ретиной}{#endif}\n ",
                ],
            )
        ]

        cls.index.gltypes += [
            GLType(hid=50, param_id=500, xslname="Retina", gltype=GLType.BOOL, has_description=False),
            GLType(hid=50, param_id=501, xslname="2SimCards", gltype=GLType.BOOL, has_description=False),
            GLType(hid=50, param_id=503, xslname="WiFi", gltype=GLType.BOOL, has_description=False),
        ]

        cls.index.models += [
            Model(
                hid=50,
                title="partly_empty",
                hyperid=5001,
                glparams=[
                    GLParam(param_id=500, value=1),
                ],
            ),
        ]

    def test_empty_friendly_descriptions(self):
        """
        Что тестируем: если в строке friendly-описания модели есть только
        пробельные символы, то она считается пустой и отбрасывается
        Делаем запрос модели partly_empty у которой в описании есть
        строка только из пробелов и "обычная" строка
        Проверяем, что на выдаче только "обычная" строка без пробелов в конце
        """
        response = self.report.request_json('place=prime&text=partly_empty&show-models-specs=friendly')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"id": 5001, "specs": {"friendly": ["Телефон с ретиной"]}},
                ]
            },
            allow_different_len=False,
        )

    @classmethod
    def prepare_multi_used_params(cls):
        cls.index.models += [
            Model(
                hid=50,
                title="has_multiparams",
                hyperid=5021,
                glparams=[
                    GLParam(param_id=501, value=1),
                    GLParam(param_id=503, value=1),
                ],
            ),
        ]

    def test_multi_used_params(self):
        """
        Тестируем, что для составной характеристики в usedParams попадает два значения параметров, а в usedParamsWithValues
        будет два объекта с id и значениями
        """
        response = self.report.request_json('place=prime&text=has_multiparams&show-models-specs=friendly')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "id": 5021,
                        "specs": {
                            "friendlyext": [
                                {
                                    "type": "spec",
                                    "value": "с 2-мя сим-картами+вайфай",
                                    "usedParams": [501, 503],
                                    "usedParamsWithValues": [
                                        {"id": 501, "values": [{"isFilterable": True, "value": "1"}]},
                                        {"id": 503, "values": [{"isFilterable": True, "value": "1"}]},
                                    ],
                                },
                            ],
                        },
                    },
                ]
            },
            allow_different_len=False,
        )

    def test_hyper_group_used_params(self):
        """
        Проверяем, что параметры спеков возвращаются и для групповых моделей
        """
        response = self.report.request_json('place=prime&place=prime&hid=40&show-models-specs=friendly&text=groupspec')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "id": 4000,
                        "specs": {
                            "friendly": [
                                "Процессор: Core i5",
                                "ОС: MacOS / Windows",
                                "Видеокарта: NVIDIA GeForce",
                                "Частота процессора: 2200 Мгц",
                                "ОЗУ: 1.024...2.048 Гб",
                                "HDD: 512 Gb",
                                "DVD: нет",
                                "Bluetooth: опционально",
                                "WiFi: есть",
                                "LTE: нет",
                            ],
                            "friendlyext": [
                                {"type": "spec", "key": "Процессор", "value": "Core i5", "usedParams": [401]},
                                {"type": "spec", "key": "ОС", "value": "MacOS / Windows", "usedParams": [402]},
                                {"type": "spec", "key": "Видеокарта", "value": "NVIDIA GeForce", "usedParams": [403]},
                                {"type": "spec", "key": "Частота процессора", "value": "2200 Мгц", "usedParams": [404]},
                                {"type": "spec", "key": "ОЗУ", "value": "1.024...2.048 Гб", "usedParams": [405]},
                                {"type": "spec", "key": "HDD", "value": "512 Gb", "usedParams": [406]},
                                {"type": "spec", "key": "DVD", "value": "нет", "usedParams": [407]},
                                {"type": "spec", "key": "Bluetooth", "value": "опционально", "usedParams": [408]},
                                {"type": "spec", "key": "WiFi", "value": "есть", "usedParams": [409]},
                                {"type": "spec", "key": "LTE", "value": "нет", "usedParams": [410]},
                            ],
                        },
                    }
                ]
            },
        )

    def test_trace_log(self):
        """
        Что тестируем: Ошибка в шаблоне не прорастает до выдачи, при этом происходит запись в logicTrace и лог ошибок
        """
        response = self.report.request_json(
            'place=modelinfo&hyperid=4&bsformat=2&rids=0&show-models-specs=full&debug=1'
        )
        self.error_log.expect("exception in gumoful")

        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"description": ""},
                ]
            },
            allow_different_len=False,
        )

        self.assertFragmentIn(response, {"logicTrace": [Contains("Gumoful syntax error:")]})


if __name__ == '__main__':
    main()

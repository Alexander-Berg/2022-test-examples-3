#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa

from core.matcher import Absent, NotEmpty
from core.types import (
    Const,
    FiltersRule,
    GLParam,
    GLType,
    GLValue,
    Model,
    ModelDescriptionTemplates,
    Offer,
    Shop,
    VendorToGlobalColor,
)
from core.testcase import TestCase, main


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']

        cls.index.models += [
            Model(
                hyperid=301,
                hid=101,
                title='model_301',
                glparams=[
                    GLParam(param_id=1011, value=2),
                    GLParam(param_id=1012, value=1),
                    GLParam(param_id=1013, value=5),
                    GLParam(param_id=1014, value=7),
                ],
            ),
        ]

        cls.index.gltypes += [
            GLType(param_id=1011, hid=101, gltype=GLType.ENUM, values=[1, 2]),
            GLType(param_id=1012, hid=101, gltype=GLType.BOOL),
            GLType(param_id=1013, hid=101, gltype=GLType.NUMERIC),
            GLType(param_id=1014, hid=101, gltype=GLType.NUMERIC),
        ]

        cls.index.filters_lists += [
            FiltersRule(experiment=1, hid=101, filters_ids=[1011, 1012, 1014]),
            FiltersRule(experiment=2, hid=101, filters_ids=[1013, 1014]),
            FiltersRule(experiment=5, hid=101, filters_ids=[1011]),
        ]

        cls.index.shops += [Shop(fesh=601, priority_region=213, regions=[225])]

        cls.index.offers += [
            Offer(hyperid=301, fesh=601),
        ]

    # Тест выдачи фильтров в place=prime с нулевым флагом market_filters_exp.
    # Ожидаем выдачу всех фильтров.
    def test_prime_hid_0(self):
        response = self.report.request_json('place=prime&hid=101')
        self.assertFragmentIn(
            response,
            {
                "filters": [{"id": "1011"}, {"id": "1012"}, {"id": "1013"}, {"id": "1014"}],
                "search": {
                    "results": [
                        {
                            "id": 301,
                            "filters": [{"id": "1011"}, {"id": "1012"}, {"id": "1013"}, {"id": "1014"}],
                        }
                    ]
                },
            },
        )

    # Тест выдачи фильтров в place=prime с флагом market_filters_exp=1.
    # Ожидаем выдачу фильтров, отсутствующих в списке experiment=1.
    def test_prime_hid_1(self):
        response = self.report.request_json('place=prime&hid=101&rearr-factors=market_filters_exp=1')
        self.assertFragmentIn(
            response,
            {
                "filters": [{"id": "1013"}],
                "search": {
                    "results": [
                        {
                            "id": 301,
                            "filters": [{"id": "1013"}],
                        }
                    ]
                },
            },
        )
        self.assertFragmentNotIn(response, {"id": "1011"})
        self.assertFragmentNotIn(response, {"id": "1012"})
        self.assertFragmentNotIn(response, {"id": "1014"})

    # Тест выдачи фильтров в place=prime с флагом market_filters_exp=2.
    # Ожидаем выдачу фильтров, отсутствующих в списке experiment=2.
    def test_prime_hid_2(self):
        response = self.report.request_json('place=prime&hid=101&rearr-factors=market_filters_exp=2')
        self.assertFragmentIn(
            response,
            {
                "filters": [{"id": "1011"}, {"id": "1012"}],
                "search": {
                    "results": [
                        {
                            "id": 301,
                            "filters": [{"id": "1011"}, {"id": "1012"}],
                        }
                    ]
                },
            },
        )
        self.assertFragmentNotIn(response, {"id": "1013"})
        self.assertFragmentNotIn(response, {"id": "1014"})

    # Тест выдачи фильтров в place=prime с флагом market_filters_exp=5.
    # Ожидаем выдачу фильтров, отсутствующих в списке experiment=5.
    def test_prime_hid_3(self):
        response = self.report.request_json('place=prime&hid=101&rearr-factors=market_filters_exp=5')
        self.assertFragmentIn(
            response,
            {
                "filters": [{"id": "1012"}, {"id": "1013"}, {"id": "1014"}],
                "search": {
                    "results": [
                        {
                            "id": 301,
                            "filters": [{"id": "1012"}, {"id": "1013"}, {"id": "1014"}],
                        }
                    ]
                },
            },
        )
        self.assertFragmentNotIn(response, {"id": "1011"})

    # Тест выдачи фильтров в place=prime с флагом market_filters_exp=-1.
    # Фильтры не должны выдаваться.
    def test_prime_hid_4(self):
        response = self.report.request_json('place=prime&hid=101&rearr-factors=market_filters_exp=-1')
        self.assertFragmentNotIn(response, {"id": "1011"})
        self.assertFragmentNotIn(response, {"id": "1012"})
        self.assertFragmentNotIn(response, {"id": "1013"})
        self.assertFragmentNotIn(response, {"id": "1014"})

    # Тест выдачи фильтров в place=modelinfo с нулевым флагом market_filters_exp.
    # Ожидаем выдачу всех фильтров.
    def test_modelinfo_0(self):
        response = self.report.request_json('place=modelinfo&hyperid=301&rids=213&rearr-factors=market_filters_exp=0')
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "filters": [{"id": "1011"}, {"id": "1012"}, {"id": "1013"}, {"id": "1014"}],
                            "id": 301,
                        }
                    ],
                }
            },
        )

    # Тест выдачи фильтров в place=modelinfo с флагом market_filters_exp=1.
    # Ожидаем выдачу фильтров, отсутствующих в списке experiment=1.
    def test_modelinfo_1(self):
        response = self.report.request_json('place=modelinfo&hyperid=301&rids=213&rearr-factors=market_filters_exp=1')
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "filters": [{"id": "1013"}],
                            "id": 301,
                        }
                    ],
                }
            },
        )
        self.assertFragmentNotIn(response, {"id": "1011"})
        self.assertFragmentNotIn(response, {"id": "1012"})
        self.assertFragmentNotIn(response, {"id": "1014"})

    # Тест выдачи фильтров в place=modelinfo с флагом market_filters_exp=2.
    # Ожидаем выдачу фильтров, отсутствующих в списке experiment=2.
    def test_modelinfo_2(self):
        response = self.report.request_json('place=modelinfo&hyperid=301&rids=213&rearr-factors=market_filters_exp=2')
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "filters": [{"id": "1011"}, {"id": "1012"}],
                            "id": 301,
                        }
                    ],
                }
            },
        )
        self.assertFragmentNotIn(response, {"id": "1013"})
        self.assertFragmentNotIn(response, {"id": "1014"})

    # Тест выдачи фильтров в place=modelinfo с флагом market_filters_exp=5.
    # Ожидаем выдачу фильтров, отсутствующих в списке experiment=5.
    def test_modelinfo_3(self):
        response = self.report.request_json('place=modelinfo&hyperid=301&rids=213&rearr-factors=market_filters_exp=5')
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "filters": [{"id": "1012"}, {"id": "1013"}, {"id": "1014"}],
                            "id": 301,
                        }
                    ],
                }
            },
        )
        self.assertFragmentNotIn(response, {"id": "1011"})

    # Тест выдачи фильтров в place=modelinfo с флагом market_filters_exp=-1.
    # Фильтры не должны выдаваться.
    def test_modelinfo_4(self):
        response = self.report.request_json('place=modelinfo&hyperid=301&rids=213&rearr-factors=market_filters_exp=-1')
        self.assertFragmentNotIn(response, {"id": "1011"})
        self.assertFragmentNotIn(response, {"id": "1012"})
        self.assertFragmentNotIn(response, {"id": "1013"})
        self.assertFragmentNotIn(response, {"id": "1014"})

    @classmethod
    def prepare_root_attributes(cls):
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
                title="gumoful model descr_tst",
                hyperid=1,
                # description="NOT_SHOULD_SEE_THIS",
                glparams=[
                    GLParam(param_id=100, value=1),
                    GLParam(param_id=101, value=0),
                    GLParam(param_id=103, value=1),
                    GLParam(param_id=104, value=4.7),
                    GLParam(param_id=105, value=1),
                    GLParam(param_id=106, value=2),
                    GLParam(param_id=107, string_value="Наушники EarPods в комплекте"),
                    GLParam(param_id=109, string_value=""),
                    GLParam(param_id=Const.MODELS_VIDEO_PARAM_ID, string_value="video_url_1"),
                    GLParam(param_id=Const.MODELS_VIDEO_PARAM_ID, string_value="video_url_2"),
                ],
            ),
            Model(hid=24, title="model descr_tst", hyperid=2, description="SHOULD_SEE_THIS"),
            Model(
                hid=23,
                title="multivalue gumoful model descr_tst",
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
        ]

        # создаем необходимое количество офферов для модели, чтобы показался колдунщик модели
        # cls.index.offers += [Offer(hyperid=1, title='Offer %s' % x, price=400) for x in range(10)]

        cls.index.filters_lists += [
            FiltersRule(experiment=1, hid=23, filters_ids=[100, 102]),
            FiltersRule(experiment=2, hid=23, filters_ids=[100]),
            FiltersRule(experiment=5, hid=23, filters_ids=[102]),
        ]

    def test_friendly_descriptions_0(self):
        """
        Friendly описания рендерятся в виде массива строк по шаблону friendlymodel
        Мы должны явно указать параметр &show-models-specs=friendly, чтобы отрендерить их
        Если отрендеренная характеристика оказалась пустой ('с 2-мя сим-картами'), мы должны ее игнорировать и не рендерить
        (allow_different_len=False проверяет это)
        У модели без шаблона характеристика отсутствует
        """
        response = self.report.request_json(
            'place=prime&text=descr_tst&show-models-specs=friendly&rearr-factors=market_filters_exp=0'
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "id": 1,
                        "specs": {
                            "friendly": ["Телефон с ретиной", "без инфракрасного порта"],
                            "friendlyext": [
                                {"type": "spec", "value": "Телефон с ретиной", "usedParams": [100]},
                                {"type": "spec", "value": "без инфракрасного порта", "usedParams": [102]},
                            ],
                        },
                    },
                    {
                        "id": 2,
                        "specs": {"friendly": Absent()},
                    },
                    {"id": 3, "specs": {"friendly": ["Телефон", "без инфракрасного порта"]}},
                ]
            },
            allow_different_len=False,
        )

    # Параметры из черного списка experiment=1 не должны попасть во Friendly-описания
    def test_friendly_descriptions_1(self):
        response = self.report.request_json(
            'place=prime&text=descr_tst&show-models-specs=friendly&rearr-factors=market_filters_exp=1'
        )
        self.assertFragmentNotIn(
            response, {"results": [{"specs": {"friendlyext": [{"type": "spec", "usedParams": [100]}]}}]}
        )
        self.assertFragmentNotIn(
            response, {"results": [{"specs": {"friendlyext": [{"type": "spec", "usedParams": [102]}]}}]}
        )

    # Параметры из черного списка experiment=2 не должны попасть во Friendly-описания
    def test_friendly_descriptions_2(self):
        response = self.report.request_json(
            'place=prime&text=descr_tst&show-models-specs=friendly&rearr-factors=market_filters_exp=2'
        )
        self.assertFragmentNotIn(
            response, {"results": [{"specs": {"friendlyext": [{"type": "spec", "usedParams": [100]}]}}]}
        )

    # Параметры из черного списка experiment=5 не должны попасть во Friendly-описания
    def test_friendly_descriptions_3(self):
        response = self.report.request_json(
            'place=prime&text=descr_tst&show-models-specs=friendly&rearr-factors=market_filters_exp=5'
        )
        self.assertFragmentNotIn(
            response, {"results": [{"specs": {"friendlyext": [{"type": "spec", "usedParams": [102]}]}}]}
        )

    # Никакие параметры не должны попасть во Friendly-описания
    def test_friendly_descriptions_4(self):
        response = self.report.request_json(
            'place=prime&text=descr_tst&show-models-specs=friendly&rearr-factors=market_filters_exp=-1'
        )
        self.assertFragmentNotIn(
            response, {"results": [{"specs": {"friendlyext": [{"type": "spec", "usedParams": [100]}]}}]}
        )
        self.assertFragmentNotIn(
            response, {"results": [{"specs": {"friendlyext": [{"type": "spec", "usedParams": [102]}]}}]}
        )

    def get_filters_content(self, matcher):
        return [
            {"type": "number", "kind": matcher},
            {"type": "boolean", "kind": matcher},
            {"type": "enum", "kind": matcher},
        ]

    # Проверяем, что minimize-output=1 убирает некоторые поля из фильтров в корне
    def test_minimize_output_filters_root(self):
        response = self.report.request_json('place=prime&hid=101')
        self.assertFragmentIn(response, {"filters": self.get_filters_content(NotEmpty())})
        response = self.report.request_json('place=prime&hid=101&minimize-output=1')
        self.assertFragmentIn(response, {"filters": self.get_filters_content(Absent())})

    # Проверяем, что minimize-output=1 убирает некоторые поля из фильтров в результатах
    def test_minimize_output_filters_results(self):
        response = self.report.request_json('place=prime&hid=101')
        self.assertFragmentIn(response, {"search": {"results": [{"filters": self.get_filters_content(NotEmpty())}]}})
        response = self.report.request_json('place=prime&hid=101&minimize-output=1')
        self.assertFragmentIn(response, {"search": {"results": [{"filters": self.get_filters_content(Absent())}]}})

    class groups:
        first = ["#FFFF00", "#FFD700"]
        second = ["#42AAFF", "#0000FF"]
        global_color_param_id = 13887626
        vendor_color_param_id = 14871214
        short_global_values = [first[0], "#FFFFFF", second[0]]
        good_global_values = [first[0], "#FFFFFA", "#FFFFFF", second[0]]
        good_vendor_values = [first[0], "#FFFFFA", second[0]]

    @classmethod
    def prepare_test_mix_colors(cls):
        def add_colors(id, colors, sub_type="color"):
            values = []
            position = 1
            for color in colors:
                values += [GLValue(position=position, value_id=position, code=color, text=color)]
                cls.index.offers += [
                    Offer(
                        hid=id,
                        hyperid=id,
                        title="offer title " + color,
                        glparams=[
                            GLParam(param_id=id, value=position),
                        ],
                    )
                ]

                position = position + 1

            cls.index.models += [Model(hid=id, title="model title " + color, hyperid=id)]

            cls.index.gltypes += [
                GLType(
                    param_id=id,
                    hid=id,
                    gltype=GLType.ENUM,
                    values=values,
                    xslname="color_glob",
                    subtype=sub_type,
                    cluster_filter=True,
                )
            ]

        def add_colors_global_and_vendor(id, colors, skip_vendor_color, sub_type="color"):
            values_glob = []
            values_vendor = []
            position = 1
            for color in colors:
                skip = skip_vendor_color == color
                values_glob += [GLValue(position=position, value_id=position, code=color, text=color)]
                offer_params = [GLParam(param_id=T.groups.global_color_param_id, value=position)]
                if not skip:
                    values_vendor += [GLValue(position=position, value_id=position, code=color, text=color)]
                    cls.index.vendor_to_glob_colors += [
                        VendorToGlobalColor(id, position, [position]),
                    ]
                    offer_params += [GLParam(param_id=T.groups.vendor_color_param_id, value=position)]

                cls.index.offers += [Offer(hid=id, hyperid=id, title="offer title " + color, glparams=offer_params)]

                position = position + 1

            cls.index.models += [Model(hid=id, title="model title " + color, hyperid=id)]

            cls.index.gltypes += [
                GLType(
                    param_id=T.groups.global_color_param_id,
                    hid=id,
                    gltype=GLType.ENUM,
                    values=values_glob,
                    xslname="color_glob",
                    subtype="color",
                    cluster_filter=True,
                )
            ]

            cls.index.gltypes += [
                GLType(
                    param_id=T.groups.vendor_color_param_id,
                    hid=id,
                    gltype=GLType.ENUM,
                    values=values_vendor,
                    xslname="color_vendor",
                    subtype="image_picker",
                    cluster_filter=True,
                )
            ]

        add_colors_global_and_vendor(2000, T.groups.short_global_values, "#FFFFFF")
        add_colors_global_and_vendor(2001, T.groups.good_global_values, "#FFFFFF")
        # только по одному цвету из группы - надо перемешать
        add_colors(1000, [T.groups.second[0], T.groups.first[0], "#FFFFFF"])
        add_colors(1001, ["#FFFFFF", T.groups.first[0], T.groups.second[0]])
        # нет возможности перемешать - надо скрыть
        add_colors(1002, [T.groups.first[0], T.groups.second[0]])
        # больше одного цвета в группе - надо перемешать
        add_colors(1007, [T.groups.second[0], T.groups.first[0], T.groups.second[1], "#FFFFFF"])
        add_colors(1008, [T.groups.second[0], "#FFFFFF", T.groups.first[0], T.groups.first[1], T.groups.second[1]])
        add_colors(1009, [T.groups.second[0], T.groups.first[1], "#FFFFFF", T.groups.second[1], T.groups.first[0]])
        add_colors(1010, ["#FFFFFF", T.groups.second[0], T.groups.first[0], T.groups.second[1], "#FFFFFF"])
        add_colors(
            1011, ["#FFFFFF", T.groups.second[0], "#FFFFFF", T.groups.first[0], T.groups.first[1], T.groups.second[1]]
        )
        add_colors(
            1012, ["#FFFFFF", T.groups.second[0], T.groups.first[0], "#FFFFFF", T.groups.second[1], T.groups.first[1]]
        )
        # наиболее вероятный кейс перемешивания
        add_colors(
            1013,
            [
                "#FFFFFF",
                "#FFFFFF",
                "#FFFFFF",
                "#FFFFFF",
                "#FFFFFF",
                T.groups.second[0],
                T.groups.first[0],
                "#FFFFFF",
                "#FFFFFF",
                "#FFFFFF",
            ],
        )
        # нет возможности перемешать когда более однго значения в группе
        add_colors(1014, [T.groups.first[0], T.groups.second[0], T.groups.second[0]])
        add_colors(1015, [T.groups.first[0], T.groups.first[0], T.groups.second[0], T.groups.second[0]])
        # нет возможности перемешать одно значение
        add_colors(1016, [T.groups.first[0]])
        # нет необходимости перемешивать, т.к. групп нет или она одна
        add_colors(1017, ["#FFFFFF", T.groups.first[0], T.groups.first[1], "#FFFFFA"])
        add_colors(1018, [T.groups.second[0], T.groups.second[1], "#FFFFFF", "#FFFFFA"])
        add_colors(1019, ["#FFFFFF", T.groups.second[1], "#FFFFFF", "#FFFFFA"])
        add_colors(1020, ["#FFFFFF", "#FFFFFF", T.groups.first[1]])
        add_colors(1021, ["#FFFFFF", "#FFFFFA"])
        # нет возможности перемешать - надо скрыть другой тип фильтра image_picker
        add_colors(1022, [T.groups.first[0], T.groups.second[0]], "image_picker")
        # больше одного цвета в группе - надо перемешать другой тип фильтра image_picker
        add_colors(1023, [T.groups.second[0], T.groups.first[0], T.groups.second[1], "#FFFFFF"], "image_picker")

    def check_order(self, id, new_order, rearrs=None):
        response = self.report.request_json('place=prime&hid={}'.format(id) + (rearrs if rearrs else ""))
        self.assertFragmentIn(
            response,
            {
                "filters": [{"id": str(id), "values": [{"value": value} for value in new_order]}],
                "search": {
                    "results": [
                        {
                            "filters": [{"id": str(id), "values": [{"value": value} for value in new_order]}],
                        }
                    ]
                },
            },
            preserve_order=True,
        )

    def test_no_mix_colors(self):
        """
        Проверяем, что при market_mix_filter_values = 0 цвета НЕ перемешиваются
        """
        self.check_order(
            1000, [T.groups.second[0], T.groups.first[0], "#FFFFFF"], "&rearr-factors=market_mix_filter_values=0"
        )
        """
        1 значение присутствует
        """
        self.check_order(1016, [T.groups.first[0]])
        """
        нет необходимости перемешивать, т.к. групп нет или она одна
        """
        self.check_order(1017, ["#FFFFFF", T.groups.first[0], T.groups.first[1], "#FFFFFA"])
        self.check_order(1018, [T.groups.second[0], T.groups.second[1], "#FFFFFF", "#FFFFFA"])
        self.check_order(1019, ["#FFFFFF", T.groups.second[1], "#FFFFFF", "#FFFFFA"])
        self.check_order(1020, ["#FFFFFF", "#FFFFFF", T.groups.first[1]])
        self.check_order(1021, ["#FFFFFF", "#FFFFFA"])

    def test_mix_colors(self):
        """
        Проверяем, что без флага по умолчанию, market_mix_filter_values = 1, цвета перемешиваются
        """
        self.check_order(1000, [T.groups.first[0], "#FFFFFF", T.groups.second[0]])
        self.check_order(1001, [T.groups.first[0], "#FFFFFF", T.groups.second[0]])
        """
        Проверяем, что перемешиваются наборы где больше одного цвета в группе
        """
        self.check_order(1007, [T.groups.first[0], "#FFFFFF", T.groups.second[1], T.groups.second[0]])
        self.check_order(
            1008, [T.groups.first[0], T.groups.first[1], "#FFFFFF", T.groups.second[1], T.groups.second[0]]
        )
        self.check_order(
            1009, [T.groups.first[1], T.groups.first[0], "#FFFFFF", T.groups.second[1], T.groups.second[0]]
        )
        self.check_order(1010, [T.groups.first[0], "#FFFFFF", "#FFFFFF", T.groups.second[1], T.groups.second[0]])
        self.check_order(
            1011, [T.groups.first[0], T.groups.first[1], "#FFFFFF", "#FFFFFF", T.groups.second[1], T.groups.second[0]]
        )
        self.check_order(
            1012, [T.groups.first[0], T.groups.first[1], "#FFFFFF", "#FFFFFF", T.groups.second[1], T.groups.second[0]]
        )
        """
        Проверяем, что перемешиваются наборы где больше одного цвета в группе тип image_picker
        """
        # больше одного цвета в группе - надо перемешать другой тип фильтра image_picker
        self.check_order(1023, [T.groups.first[0], "#FFFFFF", T.groups.second[1], T.groups.second[0]])
        """
        Проверяем, что перемешивается наиболее вероятный случай
        """
        self.check_order(
            1013,
            [
                T.groups.first[0],
                "#FFFFFF",
                "#FFFFFF",
                "#FFFFFF",
                "#FFFFFF",
                "#FFFFFF",
                "#FFFFFF",
                "#FFFFFF",
                "#FFFFFF",
                T.groups.second[0],
            ],
        )

    def check_hidden(self, id):
        response = self.report.request_json('place=prime&hid={}'.format(id))
        self.assertFragmentNotIn(
            response,
            {
                "search": {},
                "filters": [
                    {
                        "id": str(id),
                    }
                ],
            },
        )
        # у офферов есть 1 значение
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "filters": [
                                {
                                    "id": str(id),
                                    "valuesCount": 1,
                                }
                            ],
                        }
                    ]
                },
            },
        )
        # у модели фильтр скрылся
        self.assertFragmentNotIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "filters": [
                                {
                                    "entity": "product",
                                    "id": str(id),
                                }
                            ],
                        }
                    ]
                },
            },
        )

    def test_mix_colors_hide(self):
        """
        Проверяем, что без флага по умолчанию market_mix_filter_values = 1, если цвета перемешать не удаётся, например их только 2, то фильтр будет скрыт
        """
        # невозможно замешать по одному значеню в группе
        self.check_hidden(1002)
        # невозможно замешать по одному значеню в группе тип "image_picker"
        self.check_hidden(1022)
        # невозможно замешать больше одного значения в группе
        self.check_hidden(1014)
        self.check_hidden(1015)

    def test_global_and_vendor_color_hide(self):
        """
        Проверяем, что вендорный фильтр не прорастет в модели т.к. там всего два значения, при этом глобальный фильтр останется
        """
        response = self.report.request_json('place=prime&hid={}&add-vendor-color=1'.format(2000))
        # проверяем, т.к. есть промежуточный цвет, то глобальный фильтр должен прорасти
        self.assertFragmentIn(
            response,
            {
                "filters": [
                    {
                        "id": str(T.groups.global_color_param_id),
                        "values": [{"value": value} for value in T.groups.short_global_values],
                    }
                ],
                "search": {
                    "results": [
                        {
                            "filters": [
                                {
                                    "id": str(T.groups.global_color_param_id),
                                    "values": [{"value": value} for value in T.groups.short_global_values],
                                }
                            ],
                        }
                    ]
                },
            },
            preserve_order=True,
        )
        # проверяем, что вендор фильтр не пророс глобально, т.к. там только два значения рядом
        self.assertFragmentNotIn(
            response,
            {
                "filters": [{"id": str(T.groups.vendor_color_param_id)}],
                "search": {},
            },
            preserve_order=True,
        )
        # проверяем, что вендор фильтр не пророс в модели, т.к. там только два значения рядом
        self.assertFragmentNotIn(
            response,
            {
                "filters": [],
                "search": {
                    "results": [
                        {
                            "entity": "product",
                            "filters": [{"id": str(T.groups.vendor_color_param_id)}],
                        }
                    ]
                },
            },
            preserve_order=True,
        )

    def test_global_and_vendor_color_no_hide(self):
        """
        Проверяем, что вендорный фильтр прорастет в модели т.к. там есть промежуточное значение
        """
        response = self.report.request_json('place=prime&hid={}&add-vendor-color=1'.format(2001))
        # проверяем, т.к. есть промежуточный цвет, то глобальный фильтр должен прорасти
        self.assertFragmentIn(
            response,
            {
                "filters": [
                    {
                        "id": str(T.groups.global_color_param_id),
                        "values": [{"value": value} for value in T.groups.good_global_values],
                    }
                ],
                "search": {
                    "results": [
                        {
                            "filters": [
                                {
                                    "id": str(T.groups.global_color_param_id),
                                    "values": [{"value": value} for value in T.groups.good_global_values],
                                }
                            ],
                        }
                    ]
                },
            },
            preserve_order=True,
        )
        # проверяем, т.к. есть промежуточный цвет, то и вендорный фильтр должен прорасти
        self.assertFragmentIn(
            response,
            {
                "filters": [
                    {
                        "id": str(T.groups.global_color_param_id),
                        "values": [{"value": value} for value in T.groups.good_vendor_values],
                    }
                ],
                "search": {
                    "results": [
                        {
                            "filters": [
                                {
                                    "id": str(T.groups.global_color_param_id),
                                    "values": [{"value": value} for value in T.groups.good_vendor_values],
                                }
                            ],
                        }
                    ]
                },
            },
            preserve_order=True,
        )


if __name__ == '__main__':
    main()

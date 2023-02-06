#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import GLParam, GLType, GLValue, HyperCategory, Model, Offer
from core.matcher import ElementCount, NoKey

# Вводная:
#
# Этот тест посвящен "превращению" numeric-фильтров в enum'ы
# Фактически должно произойти следующее:
# Если в numeric-фильтре (offer-level) во всей выдаче <= 32 значений, то добавить поле extraType=set и
# массив distinctValues со значениями
#
# См. тикеты MARKETOUT-14349, MARKETOUT-14350, MARKETOUT-14352, MARKETOUT-14353


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']
        # RANDX randomizing is disabled because these tests don't work with it. See MARKETOUT-21319
        cls.disable_randx_randomize()

    @classmethod
    def prepare_data(cls):
        # заводим numeric-параметр второго рода
        cls.index.gltypes += [
            GLType(
                param_id=101,
                hid=1,
                gltype=GLType.NUMERIC,
                hidden=False,
                unit_name='Количество в упаковке',
                cluster_filter=True,
            ),
            GLType(
                param_id=102,
                hid=2,
                gltype=GLType.NUMERIC,
                hidden=False,
                unit_name='Количество колёс',
                cluster_filter=True,
            ),
            GLType(
                param_id=103,
                hid=3,
                gltype=GLType.NUMERIC,
                hidden=False,
                unit_name='Диафрагма',
                cluster_filter=True,
                precision=14,
            ),
        ]

        cls.index.models += [
            Model(hyperid=1001, hid=1, title='Детские салфетки Хаггис'),
            Model(hyperid=1002, hid=2, title='Беговел Конёк-Горбунёк'),
            Model(hyperid=1003, hid=3),
        ]

        cls.index.offers += [
            # салфетки - меньше 32 значений
            Offer(hyperid=1001, glparams=[GLParam(param_id=101, value=64)], manufacturer_warranty=1, randx=8),
            Offer(hyperid=1001, glparams=[GLParam(param_id=101, value=128)], manufacturer_warranty=0, randx=16),
            # беговелы - больше 32 значений
            Offer(hyperid=1002, glparams=[GLParam(param_id=102, value=1)], manufacturer_warranty=True),
            Offer(hyperid=1002, glparams=[GLParam(param_id=102, value=2)], manufacturer_warranty=True),
            Offer(hyperid=1002, glparams=[GLParam(param_id=102, value=3)], manufacturer_warranty=False),
            Offer(hyperid=1002, glparams=[GLParam(param_id=102, value=4)], manufacturer_warranty=False),
            Offer(hyperid=1002, glparams=[GLParam(param_id=102, value=5)]),
            Offer(hyperid=1002, glparams=[GLParam(param_id=102, value=6)]),
            Offer(hyperid=1002, glparams=[GLParam(param_id=102, value=7)]),
            Offer(hyperid=1002, glparams=[GLParam(param_id=102, value=8)]),
            Offer(hyperid=1002, glparams=[GLParam(param_id=102, value=9)]),
            Offer(hyperid=1002, glparams=[GLParam(param_id=102, value=10)]),
            Offer(hyperid=1002, glparams=[GLParam(param_id=102, value=11)]),
            Offer(hyperid=1002, glparams=[GLParam(param_id=102, value=12)]),
            Offer(hyperid=1002, glparams=[GLParam(param_id=102, value=13)]),
            Offer(hyperid=1002, glparams=[GLParam(param_id=102, value=14)]),
            Offer(hyperid=1002, glparams=[GLParam(param_id=102, value=15)]),
            Offer(hyperid=1002, glparams=[GLParam(param_id=102, value=16)]),
            Offer(hyperid=1002, glparams=[GLParam(param_id=102, value=17)]),
            Offer(hyperid=1002, glparams=[GLParam(param_id=102, value=18)]),
            Offer(hyperid=1002, glparams=[GLParam(param_id=102, value=19)]),
            Offer(hyperid=1002, glparams=[GLParam(param_id=102, value=20)]),
            Offer(hyperid=1002, glparams=[GLParam(param_id=102, value=21)]),
            Offer(hyperid=1002, glparams=[GLParam(param_id=102, value=22)]),
            Offer(hyperid=1002, glparams=[GLParam(param_id=102, value=23)]),
            Offer(hyperid=1002, glparams=[GLParam(param_id=102, value=24)]),
            Offer(hyperid=1002, glparams=[GLParam(param_id=102, value=25)]),
            Offer(hyperid=1002, glparams=[GLParam(param_id=102, value=26)]),
            Offer(hyperid=1002, glparams=[GLParam(param_id=102, value=27)]),
            Offer(hyperid=1002, glparams=[GLParam(param_id=102, value=28)]),
            Offer(hyperid=1002, glparams=[GLParam(param_id=102, value=29)]),
            Offer(hyperid=1002, glparams=[GLParam(param_id=102, value=30)]),
            Offer(hyperid=1002, glparams=[GLParam(param_id=102, value=31)]),
            Offer(hyperid=1002, glparams=[GLParam(param_id=102, value=32)]),
            Offer(hyperid=1002, glparams=[GLParam(param_id=102, value=33)]),
            Offer(hyperid=1003, glparams=[GLParam(param_id=103, value=0.00000000011122)]),
            Offer(hyperid=1003, glparams=[GLParam(param_id=103, value=0.0000000005)]),
        ]

    def test_numeric_filter_is_numeric_in_offers(self):
        """
        Делаем запрос офферов для товара с hyperid=1001 в эксперименте.

        Проверяем, что в выдаче фильтр id=101 как в офферах остался с типом "number" без "extraType"

        Больше нигде ниже мы проверять неконвертированность numeric-фильтров в самих офферах не будем, т.к. этого достаточно.
        Фильтры в корне проверим отдельным тестом ниже.
        """
        response = self.report.request_json('place=productoffers&hid=1&hyperid=1001')
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        # это первый оффер с количеством штук 64
                        {
                            "entity": "offer",
                            "filters": [
                                {
                                    "id": "101",
                                    "type": "number",
                                    "values": [
                                        {
                                            "min": "64",
                                            "max": "64",
                                        }
                                    ],
                                }
                            ],
                        },
                        # это второй оффер с количеством штук 128
                        {
                            "entity": "offer",
                            "filters": [
                                {
                                    "id": "101",
                                    "type": "number",
                                    "values": [
                                        {
                                            "min": "128",
                                            "max": "128",
                                        }
                                    ],
                                }
                            ],
                        },
                    ]
                },
            },
        )

    def test_numeric_filter_is_converted(self):
        """
        Собственно, делаем запрос за фильтрами под флагом.

        Выдача должна содержать нужные поля.
        numeric превращается в enum
        """
        response = self.report.request_json('place=productoffers&hid=1&hyperid=1001')
        self.assertFragmentIn(
            response,
            {
                "search": {},
                # это корневые фильтры для фильтрации предложений на КМ
                "filters": [
                    {
                        "id": "101",
                        "type": "enum",
                        "subType": "radio",
                        "values": [
                            {"id": "64~64", "value": "64", "found": 1, "initialFound": 1},
                            {"id": "128~128", "value": "128", "found": 1, "initialFound": 1},
                        ],
                    }
                ],
            },
        )

    def test_numeric_filter_is_converted_inside_the_experiment_default_offer(self):
        """
        проверяем, что при задании диапазона в фильтре, по умолчанию чекается фильтр со значением из деф. оффера
        и ничего не чекается, если диапазон не задан
        """
        response = self.report.request_json('place=productoffers&hid=1&hyperid=1001&offers-set=default,list')
        self.assertFragmentIn(
            response,
            {
                "search": {"results": ElementCount(3)},
                # это корневые фильтры для фильтрации предложений на КМ
                "filters": [
                    {
                        "id": "101",
                        "type": "enum",
                        "values": [
                            {"id": "64~64", "value": "64", "found": 1, "initialFound": 1, "checked": NoKey("checked")},
                            {
                                "id": "128~128",
                                "value": "128",
                                "found": 1,
                                "initialFound": 1,
                                "checked": NoKey("checked"),
                            },
                        ],
                    }
                ],
            },
        )

        response = self.report.request_json(
            'place=productoffers&hid=1&hyperid=1001&offers-set=default,list&glfilter=101:0~150'
        )
        self.assertFragmentIn(
            response,
            {
                "search": {"results": ElementCount(1)},
                "filters": [
                    {
                        "id": "101",
                        "type": "enum",
                        "values": [
                            {"id": "64~64", "value": "64", "found": 1, "initialFound": 1, "checked": NoKey("checked")},
                            {"id": "128~128", "value": "128", "found": 1, "initialFound": 1, "checked": True},
                        ],
                    }
                ],
            },
        )

    def test_numeric_filter_is_converted_filtered(self):
        """
        Собственно, делаем запрос за фильтрами под флагом.

        Выдача должна содержать нужные поля.
        initialFound везде 1
        found 1 только у 64
        numeric превращается в enum
        """
        response = self.report.request_json('place=productoffers&hid=1&hyperid=1001&manufacturer_warranty=1')
        self.assertFragmentIn(
            response,
            {
                "search": {},
                # это корневые фильтры для фильтрации предложений на КМ
                "filters": [
                    {
                        "id": "101",
                        "type": "enum",
                        "values": [
                            {"id": "64~64", "value": "64", "found": 1, "initialFound": 1},
                            {"id": "128~128", "value": "128", "found": 0, "initialFound": 1},
                        ],
                    }
                ],
            },
        )

    def test_numeric_filter_is_converted_checked(self):
        """
        делаем запрос за фильтрами под флагом с установленным numeric фильтром
        в одно значение. Проверяем, что

        Выдача должна содержать нужные поля.
        numeric превращается в enum
        """
        response = self.report.request_json('place=productoffers&hid=1&hyperid=1001&glfilter=101:64~64')
        self.assertFragmentIn(
            response,
            {
                "search": {},
                # это корневые фильтры для фильтрации предложений на КМ
                "filters": [
                    {
                        "id": "101",
                        "type": "enum",
                        "values": [
                            {"id": "64~64", "value": "64", "found": 1, "initialFound": 1, "checked": True},
                            {
                                "id": "128~128",
                                "value": "128",
                                "found": 1,
                                "initialFound": 1,
                                "checked": NoKey("checked"),
                            },
                        ],
                    }
                ],
            },
        )

    def test_numeric_filter_more_than_32_values(self):
        """
        Делаем запрос за фильтрами под флагом, но значений у нас больше 32.
        с фильтрами и без (проверяем, что реагируем на 32 - общее число, до фильтров)
        Выдача НЕ должна содержать нужные поля.
        """

        for query in [
            'place=productoffers&hid=2&hyperid=1002',
            'place=productoffers&hid=2&hyperid=1002&manufacturer_warranty=1',
        ]:
            response = self.report.request_json(query)
            self.assertFragmentIn(
                response,
                {
                    "search": {},
                    # это корневые фильтры для фильтрации предложений на КМ
                    "filters": [
                        {
                            "id": "102",
                            "type": "number",
                            "values": [
                                {
                                    "min": "1",
                                    "max": "33",
                                }
                            ],
                        }
                    ],
                },
            )

    def test_numeric_filter_is_converted_big_double(self):
        """
        делаем запрос за фильтрами под флагом.

        проверяем, что double адекватно выводятся
        """
        response = self.report.request_json('place=productoffers&hid=3&hyperid=1003')
        self.assertFragmentIn(
            response,
            {
                "search": {},
                # это корневые фильтры для фильтрации предложений на КМ
                "filters": [
                    {
                        "id": "103",
                        "type": "enum",
                        "values": [
                            {
                                "id": "0.00000000011122~0.00000000011122",
                                "value": "0.00000000011122",
                                "found": 1,
                                "initialFound": 1,
                            },
                            {"id": "0.0000000005~0.0000000005", "value": "0.0000000005", "found": 1, "initialFound": 1},
                        ],
                    }
                ],
            },
        )

    def test_in_prime_place(self):
        """
        Делаем запрос за фильтрами, но в place=prime

        Выдача НЕ должна содержать нужные поля.
        """
        response = self.report.request_json('place=prime&hid=2&hyperid=1002&rearr-factors=market_numeric_to_enum=1')
        self.assertFragmentIn(
            response,
            {
                "search": {},
                # это корневые фильтры для фильтрации предложений на КМ
                "filters": [
                    {
                        "id": "102",
                        "type": "number",
                        "values": [
                            {
                                "min": "1",
                                "max": "33",
                            }
                        ],
                    }
                ],
            },
        )

    # А теперь готовим данные для групповой модели и модификаций
    @classmethod
    def prepare_test_filters_for_group_model_and_modifications(cls):
        """
        Создаем по два параметра numeric для модификаций, по одному из параметров модификации
        будут отличаться, по другому параметру они все будут идентичны
        """
        cls.index.gltypes += [
            GLType(param_id=405, hid=4, gltype=GLType.NUMERIC, cluster_filter=True, xslname="HDD"),
            GLType(param_id=406, hid=4, gltype=GLType.NUMERIC, cluster_filter=True, xslname="Memory"),
        ]

        # создаем две модификации и присваиваем им параметры в соответствии со схемой выше
        cls.index.models += [
            Model(
                group_hyperid=4000,
                hid=4,
                glparams=[
                    GLParam(param_id=405, value=500),
                    GLParam(param_id=406, value=4),
                ],
            ),
            Model(
                group_hyperid=4000,
                hid=4,
                glparams=[
                    GLParam(param_id=405, value=500),
                    GLParam(param_id=406, value=8),
                ],
            ),
            Model(
                group_hyperid=4000,
                hid=4,
                glparams=[
                    GLParam(param_id=405, value=600),
                    GLParam(param_id=406, value=8),
                ],
            ),
        ]

    def test_model_modifications(self):
        response = self.report.request_json('place=model_modifications&hyperid=4000&hid=4')
        self.assertFragmentIn(
            response,
            {
                "search": {},
                # это корневые фильтры для фильтрации предложений на КМ
                "filters": [
                    {
                        "id": "405",
                        "type": "enum",
                        "values": [
                            {
                                "id": "500~500",
                                "value": "500",
                            },
                            {
                                "id": "600~600",
                                "value": "600",
                            },
                        ],
                    },
                    {
                        "id": "406",
                        "type": "enum",
                        "values": [
                            {
                                "id": "4~4",
                                "value": "4",
                            },
                            {
                                "id": "8~8",
                                "value": "8",
                            },
                        ],
                    },
                ],
            },
        )

    @classmethod
    def prepare_disabled_enum(cls):
        cls.index.hypertree += [
            HyperCategory(hid=8000, name="laptops"),
        ]

        """
        По фильтру 500:1 у оферов указан фильтр 501, а по 500:2 - нет.
        """

        cls.index.offers += [
            Offer(
                title='MacBook Silver',
                hyperid=8000,
                hid=8000,
                glparams=[GLParam(param_id=500, value=1), GLParam(param_id=501, value=1024)],
            ),
            Offer(
                title='MacBook Gold',
                hyperid=8000,
                hid=8000,
                glparams=[GLParam(param_id=500, value=1), GLParam(param_id=501, value=2048)],
            ),
            Offer(title='MacBook Grey', hyperid=8000, hid=8000, glparams=[GLParam(param_id=500, value=2)]),
        ]
        cls.index.models += [Model(title='MacBook', hyperid=8000, hid=8000)]
        cls.index.gltypes += [
            GLType(
                param_id=500,
                hid=8000,
                gltype=GLType.ENUM,
                cluster_filter=True,
                values=[GLValue(value_id=1, text='i5'), GLValue(value_id=2, text='M3')],
                unit_name="CPU",
            ),
            GLType(param_id=501, hid=8000, gltype=GLType.NUMERIC, cluster_filter=True, unit_name="video memory"),
        ]

    def test_disabled_enum(self):

        """
        Проверяем, что если ни у одного из оферов не указано значение некоторого фильтра, то он будет также преобразовываться в список.
        """

        response = self.report.request_json('place=productoffers&hid=8000&hyperid=8000&glfilter=500:1')
        self.assertFragmentIn(
            response,
            {
                "filters": [
                    {
                        "id": "501",
                        "type": "enum",
                        "subType": "radio",
                        "values": [
                            {
                                "value": "1024",
                                "found": 1,
                                "initialFound": 1,
                            },
                            {
                                "value": "2048",
                                "found": 1,
                                "initialFound": 1,
                            },
                        ],
                    }
                ]
            },
        )

        response = self.report.request_json('place=productoffers&hid=8000&hyperid=8000&glfilter=500:2')
        self.assertFragmentIn(
            response,
            {
                "filters": [
                    {
                        "id": "501",
                        "type": "enum",
                        "subType": "radio",
                        "values": [
                            {
                                "value": "1024",
                                "found": 0,
                                "initialFound": 1,
                            },
                            {
                                "value": "2048",
                                "found": 0,
                                "initialFound": 1,
                            },
                        ],
                    }
                ]
            },
        )


if __name__ == '__main__':
    main()

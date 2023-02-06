#!/usr/bin/env python
# -*- coding: utf-8 -*-

import __classic_import  # noqa
import market.media_adv.incut_search.mt.env as env

from market.media_adv.incut_search.beam.region import Region
from market.media_adv.incut_search.beam.regional_model import RegionalModel
from market.media_adv.incut_search.beam.incut import IncutModelsList
from market.media_adv.incut_search.beam.model import ModelWithBid
from market.pylibrary.lite.matcher import ElementCount


class T(env.MediaAdvIncutSearchSuite):
    @classmethod
    def setUpClass(cls):
        """
        переопределенный метод для дополнительного вызова настроек
        """
        cls.settings.access_using = True
        super(T, cls).setUpClass()

    @classmethod
    def setup_market_access_resources(cls):
        cls.access_resources.region_models = [
            RegionalModel(
                hyperid=100 + i,
                offers=10,
                has_cpa=True,
                rids=[345],
            )
            for i in range(0, 5)  # первые пять моделей есть в статистике
        ]

        cls.access_resources.region_models.extend(
            [  # для теста с cpa
                RegionalModel(
                    hyperid=200 + i,
                    offers=1,
                    has_cpa=(True if i % 2 else False),  # половина только cpa
                    rids=[345],
                )
                for i in range(12)
            ]
        )

        cls.access_resources.region_models.extend(
            [  # для теста с кол-вом офферов
                RegionalModel(
                    hyperid=500 + i,
                    offers=(2 if i % 2 else 0),
                    rids=[345],
                    has_cpa=True,
                )
                for i in range(12)
            ]
        )

        cls.access_resources.region_tree += [
            Region(
                rid=45,
                name="Северные королевства",
                region_type=Region.FEDERAL_DISTRICT,
                children=[
                    Region(
                        rid=345,
                        name="Темерия",
                        children=[
                            Region(
                                rid=371,
                                name="Вызима",
                            )
                        ],
                    ),
                    Region(
                        rid=245,
                        name="Редания",
                    ),
                ],
            )
        ]

    @classmethod
    def prepare_regions(cls):
        cls.content.incuts += [
            # вендор у которого половины моделей нет в статистике
            IncutModelsList(
                hid=21,
                vendor_id=1,
                datasource_id=11,
                models=[ModelWithBid(model_id=100 + i) for i in range(0, 10)],
                bid=50,
            ),
            # другая категория
            IncutModelsList(
                hid=30,
                vendor_id=2,
                datasource_id=12,
                models=[ModelWithBid(model_id=100 + i) for i in range(0, 5)],  # все модели в статистике
                bid=50,  # маленькая ставка
            ),
            IncutModelsList(
                hid=30,
                vendor_id=3,
                datasource_id=13,
                models=[ModelWithBid(model_id=100 + i) for i in range(6, 10)],  # все модели отсутствуют в статистике
                bid=100,  # но большая ставка
            ),
        ]

    def test_regions_one_vendor(self):
        """
        В категории один вендор, половина моделей у которого не попадает в МРС
        При включении проверки статистики, врезка должна сократиться
        Запрос выполняется по тому же региону, что есть в статистике (без поиска по дереву)
        """
        response = self.request(
            {
                'hid': 21,
            },
            exp_flags={
                'market_madv_use_model_regional_stat': 0,
            },
        )

        self.assertFragmentIn(
            response,
            {
                'incutLists': [
                    [
                        {
                            'entity': 'incut',
                            'id': '1',
                        },
                    ],
                ],
                'entities': {
                    'model': ElementCount(10),
                    'incut': {
                        '1': {
                            'incutType': 'ModelsList',
                            'bidInfo': {
                                'bid': 50,
                            },
                        },
                    },
                    'vendor': {
                        '1': {
                            'vendorId': 1,
                        },
                    },
                },
            },
        )

        # тот же запрос с использованием региональной статистики
        response = self.request(
            {
                'hid': 21,
                'region': 345,
            },
            exp_flags={
                'market_madv_use_model_regional_stat': 1,
            },
            debug=True,
        )

        self.assertFragmentIn(
            response,
            {
                'incutLists': [
                    [
                        {
                            'entity': 'incut',
                            'id': '1',
                        },
                    ],
                ],
                "debug": {
                    "counters": {
                        "incuts": {
                            "Passed": 1,
                            "Total": 1,
                        },
                        "models": {
                            "0": {
                                "NotFoundInStat": 5,
                                "Passed": 5,
                                "Total": 10,
                            }
                        },
                    },
                },
                'entities': {
                    'model': ElementCount(5),
                    'incut': {
                        '1': {
                            'incutType': 'ModelsList',
                            'bidInfo': {
                                'bid': 50,
                            },
                        },
                    },
                    'vendor': {
                        '1': {
                            'vendorId': 1,
                        },
                    },
                },
            },
        )

    @classmethod
    def prepare_region_tree(cls):
        pass

    def test_region_tree(self):
        """
        поиск региональной статистики с учетом дерева регионов
        """
        # тот же запрос с использованием региональной статистики
        response = self.request(
            {
                'hid': 21,
                'region': 371,  # конкретно в это регионе нет, но есть в родительском
            },
            exp_flags={
                'market_madv_use_model_regional_stat': 1,
                'market_madv_use_region_tree_for_mrs': 1,
            },
            debug=True,
        )

        self.assertFragmentIn(
            response,
            {
                'incutLists': [
                    [
                        {
                            'entity': 'incut',
                            'id': '1',
                        },
                    ],
                ],
                'entities': {
                    'model': ElementCount(5),
                    'incut': {
                        '1': {
                            'incutType': 'ModelsList',
                            'bidInfo': {
                                'bid': 50,
                            },
                        },
                    },
                    'vendor': {
                        '1': {
                            'vendorId': 1,
                        },
                    },
                },
            },
        )

        # тот же запрос но без использования дерева регионов
        response = self.request(
            {
                'hid': 21,
                'region': 371,  # конкретно в это регионе нет, но есть в родительском
            },
            exp_flags={
                'market_madv_use_model_regional_stat': 1,
                'market_madv_use_region_tree_for_mrs': 0,
            },
            debug=True,
        )

        # lолжна быть пустая врезка, т.к. конкретно в этом регионе нет моделей
        self.assertFragmentIn(
            response,
            {
                'incutLists': [
                    [
                        {
                            'entity': 'incut',
                            'id': '1',
                        },
                    ],
                ],
                'entities': {
                    'incut': {
                        '1': {
                            'incutType': 'Empty',
                        },
                    },
                },
            },
        )

        # тот же запрос но без использования дерева регионов и МРС
        response = self.request(
            {
                'hid': 21,
                'region': 371,
            },
            exp_flags={
                'market_madv_use_model_regional_stat': 0,
                'market_madv_use_region_tree_for_mrs': 0,
            },
            debug=True,
        )

        # врезка должна быть с моделями, которых нет в статистике
        self.assertFragmentIn(
            response,
            {
                'incutLists': [
                    [
                        {
                            'entity': 'incut',
                            'id': '1',
                        },
                    ],
                ],
                'entities': {
                    'model': ElementCount(10),
                    'incut': {
                        '1': {
                            'incutType': 'ModelsList',
                            'bidInfo': {
                                'bid': 50,
                            },
                        },
                    },
                    'vendor': {
                        '1': {
                            'vendorId': 1,
                        },
                    },
                },
            },
        )

    @classmethod
    def prepare_cpa_offers(cls):
        cls.content.incuts += [
            IncutModelsList(
                hid=41,
                vendor_id=1,
                datasource_id=11,
                models=[ModelWithBid(model_id=200 + i) for i in range(1, 12, 2)],  # есть в МРС c CPA оффером
                bid=50,
            ),
            IncutModelsList(
                hid=41,
                vendor_id=2,
                datasource_id=12,
                models=[ModelWithBid(model_id=200 + i) for i in range(0, 12, 2)],  # в МРС, но не CPA
                bid=100,  # больше ставка
            ),
        ]

    def test_cpa_offers(self):
        """
        проверка в МРС только CPA офферов
        """
        response = self.request(
            {
                'hid': 41,
                'region': 345,
            },
            exp_flags={
                'market_madv_use_model_regional_stat': 0,
            },
            debug=True,
        )

        self.assertFragmentIn(
            response,
            {
                'incutLists': [
                    [
                        {
                            'entity': 'incut',
                            'id': '1',
                        },
                    ],
                ],
                "debug": {
                    "counters": {
                        "incuts": {"Passed": 2, "Total": 2},
                        "models": {
                            "0": {
                                "Passed": 12,
                                "Total": 12,
                            }
                        },
                    },
                },
                'entities': {
                    'model': ElementCount(12),  # все модели от двух врезок
                    'incut': {
                        '1': {
                            'incutType': 'ModelsList',
                            'bidInfo': {
                                'bid': 100,
                            },
                        },
                    },
                    'vendor': {
                        '1': {
                            'vendorId': 2,
                        },
                    },
                },
            },
        )

        # при включении МРС должны быть только CPA
        response = self.request(
            {
                'hid': 41,
                'region': 345,
            },
            exp_flags={
                'market_madv_use_model_regional_stat': 1,
            },
            debug=True,
        )

        self.assertFragmentIn(
            response,
            {
                'incutLists': [
                    [
                        {
                            'entity': 'incut',
                            'id': '1',
                        },
                    ],
                ],
                "debug": {
                    "counters": {
                        "incuts": {"NotEnoughModels": 1, "Passed": 1, "Total": 2},
                        "models": {
                            "0": {
                                "NotFoundInRegion": 6,
                                "Passed": 6,
                                "Total": 12,
                            }
                        },
                    },
                },
                'entities': {
                    'model': ElementCount(6),
                    'incut': {
                        '1': {
                            'incutType': 'ModelsList',
                            'bidInfo': {
                                'bid': 50,
                            },
                        },
                    },
                    'vendor': {
                        '1': {
                            'vendorId': 1,  # врезка вендора с меньшей ставкой
                        },
                    },
                },
            },
        )

    @classmethod
    def prepare_offer_counters(cls):
        cls.content.incuts += [
            # есть статистика в МРС и c офферами
            IncutModelsList(
                hid=51,
                vendor_id=1,
                datasource_id=11,
                models=[ModelWithBid(model_id=500 + i) for i in range(1, 12, 2)],
                bid=50,
            ),
            # в МРС статистика есть, но без офферов
            IncutModelsList(
                hid=51,
                vendor_id=2,
                datasource_id=12,
                models=[ModelWithBid(model_id=500 + i) for i in range(0, 12, 2)],
                bid=100,  # больше ставка
            ),
        ]

    def test_offer_counters(self):
        """
        проверка кол-ва офферов в статистике
        """
        response = self.request(
            {
                'hid': 51,
                'region': 345,
            },
            exp_flags={
                'market_madv_use_model_regional_stat': 0,
            },
            debug=True,
        )

        self.assertFragmentIn(
            response,
            {
                'incutLists': [
                    [
                        {
                            'entity': 'incut',
                            'id': '1',
                        },
                    ],
                ],
                "debug": {
                    "counters": {"incuts": {"Passed": 2, "Total": 2}, "models": {"0": {"Passed": 12, "Total": 12}}},
                },
                'entities': {
                    'model': ElementCount(12),  # все модели от двух врезок
                    'incut': {
                        '1': {
                            'incutType': 'ModelsList',
                            'bidInfo': {
                                'bid': 100,
                            },
                        },
                    },
                    'vendor': {
                        '1': {
                            'vendorId': 2,
                        },
                    },
                },
            },
        )

        # при включении МРС должна быть врезка только та, что с офферами
        response = self.request(
            {
                'hid': 51,
                'region': 345,
            },
            exp_flags={
                'market_madv_use_model_regional_stat': 1,
            },
            debug=True,
        )

        self.assertFragmentIn(
            response,
            {
                'incutLists': [
                    [
                        {
                            'entity': 'incut',
                            'id': '1',
                        },
                    ],
                ],
                "debug": {
                    "counters": {
                        "incuts": {"NotEnoughModels": 1, "Passed": 1, "Total": 2},
                        "models": {
                            "0": {
                                "NotFoundInRegion": 6,
                                "Passed": 6,
                                "Total": 12,
                            }
                        },
                    },
                },
                'entities': {
                    'model': ElementCount(6),
                    'incut': {
                        '1': {
                            'incutType': 'ModelsList',
                            'bidInfo': {
                                'bid': 50,
                            },
                        },
                    },
                    'vendor': {
                        '1': {
                            'vendorId': 1,  # врезка вендора с меньшей ставкой
                        },
                    },
                },
            },
        )


if __name__ == '__main__':
    env.main()

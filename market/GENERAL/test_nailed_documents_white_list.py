#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa

from core.types import (
    BlueOffer,
    GLParam,
    GLType,
    GLValue,
    HyperCategory,
    HyperCategoryType,
    MarketSku,
    MnPlace,
    Model,
    ModelDescriptionTemplates,
    NavCategory,
    QueryIntList,
)
from core.testcase import TestCase, main
from core.matcher import Absent


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['market_new_cpm_iterator=0']

        cls.index.hypertree += [
            HyperCategory(hid=1, name='Мобильные телефоны', output_type=HyperCategoryType.GURU),
        ]

        cls.index.navtree += [NavCategory(nid=10, hid=1)]

        cls.index.models += [
            Model(ts=101, hyperid=101, title='Iphone 13', hid=1),
            Model(ts=102, hyperid=102, title='Samsung Galaxy S20', hid=1),
            Model(ts=103, hyperid=103, title='Nokia 3310', hid=1),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 101).respond(0.9)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 102).respond(0.8)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 103).respond(0.7)

        cls.index.gltypes += [
            GLType(
                hid=1,
                param_id=101,
                cluster_filter=True,
                gltype=GLType.ENUM,
                values=[
                    GLValue(value_id=1, text='value1'),
                    GLValue(value_id=2, text='value2'),
                ],
                model_filter_index=1,
                xslname='sku_filter',
            ),
        ]

        cls.index.model_description_templates += [
            ModelDescriptionTemplates(
                hid=1,
                friendlymodel=['model friendly {sku_filter}'],
                model=[("Основное", {'model full': '{sku_filter}'})],
            ),
        ]

        cls.index.mskus += [
            MarketSku(
                sku=1001,
                hyperid=101,
                blue_offers=[BlueOffer(ts=1001, title='Телефон Iphone 13')],
                glparams=[GLParam(param_id=101, value=1)],
            ),
            MarketSku(
                sku=1002,
                hyperid=102,
                blue_offers=[BlueOffer(ts=1002, title='Телефон Samsung Galaxy S20')],
                glparams=[GLParam(param_id=101, value=1)],
            ),
            MarketSku(
                sku=1003,
                hyperid=103,
                title='Nokia 3310 sku 1',
                blue_offers=[BlueOffer(ts=1003, title='Nokia 3310 offer 1', waremd5='hUElnbhS0nTF3o1F_CnqIQ')],
                price=100,
                glparams=[GLParam(param_id=101, value=1)],
            ),
            MarketSku(
                sku=1004,
                hyperid=103,
                title='Nokia 3310 sku 2',
                blue_offers=[BlueOffer(ts=1004, title='Nokia 3310 offer 2', waremd5='WVe1mzsz2_ZcX2auywOU9w')],
                price=200,
                glparams=[GLParam(param_id=101, value=2)],
            ),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1001).respond(0.9)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1002).respond(0.8)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1003).respond(0.7)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1004).respond(0.5)

        cls.index.nailed_docs_white_list += [
            QueryIntList(query='телефон', integer_ids=['1004']),
            QueryIntList(query='Nokia', integer_ids=['1004']),
            QueryIntList(query='1', integer_ids=['1004']),
            QueryIntList(query='10', integer_ids=['1004']),
            QueryIntList(query='1 101:1', integer_ids=['1003']),
        ]

    def test_nailed_docs_white_list(self):
        """Проверяем что под флагом market_nailed_docs_white_list=1
        в выдачу прибиваются модели с ДО от СКУ из файла nailed-docs-white-list.db
        https://st.yandex-team.ru/MARKETOUT-42577
        """
        param_models_specs = '&show-models-specs=msku-friendly,msku-full'

        request_params = 'place=prime&text=телефон&use-default-offers=1&allow-collapsing=1' + param_models_specs
        request_hid_params = (
            'place=prime&hid=1&suggest_text=телефон&was_redir=1&use-default-offers=1&allow-collapsing=1'
            + param_models_specs
        )
        request_nid_params = (
            'place=prime&nid=10&suggest_text=телефон&was_redir=1&use-default-offers=1&allow-collapsing=1'
            + param_models_specs
        )
        request_hid_gl_filter_params = (
            'place=prime&hid=1&glfilter=101:1&suggest_text=телефон&was_redir=1&use-default-offers=1&allow-collapsing=1'
            + param_models_specs
        )

        for request in (request_params, request_hid_params, request_nid_params):
            response = self.report.request_json(request + '&add-sku-stats=1')
            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {
                            "entity": "product",
                            "id": 103,
                            "titles": {"raw": "Nokia 3310"},
                            "offers": {
                                "items": [
                                    {
                                        "sku": "1004",
                                        "marketSku": "1004",
                                        "titles": {
                                            "raw": "Nokia 3310 offer 2",
                                            "highlighted": [{"value": "Nokia 3310 offer 2"}],
                                        },
                                        "skuAwareTitles": {
                                            "raw": "Nokia 3310 sku 2",
                                            "highlighted": [{"value": "Nokia 3310 sku 2"}],
                                        },
                                        "skuAwarePictures": [{"original": {"width": 100}}],
                                        "skuAwareSpecs": {
                                            "friendly": ["model friendly value2"],
                                            "full": [
                                                {
                                                    "groupName": "Основное",
                                                    "groupSpecs": [{"name": "model full", "value": "value2"}],
                                                }
                                            ],
                                        },
                                    }
                                ]
                            },
                        },
                        {"entity": "product", "id": 101, "titles": {"raw": "Iphone 13"}},
                        {"entity": "product", "id": 102, "titles": {"raw": "Samsung Galaxy S20"}},
                    ]
                },
                preserve_order=True,
                allow_different_len=False,
            )

        response = self.report.request_json(request_hid_gl_filter_params)
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "product",
                        "id": 103,
                        "titles": {"raw": "Nokia 3310"},
                        "offers": {
                            "items": [
                                {
                                    "sku": "1003",
                                    "marketSku": "1003",
                                    "titles": {
                                        "raw": "Nokia 3310 offer 1",
                                        "highlighted": [{"value": "Nokia 3310 offer 1"}],
                                    },
                                    "skuAwareTitles": {
                                        "raw": "Nokia 3310 sku 1",
                                        "highlighted": [{"value": "Nokia 3310 sku 1"}],
                                    },
                                    "skuAwarePictures": [{"original": {"width": 100}}],
                                    "skuAwareSpecs": {
                                        "friendly": ["model friendly value1"],
                                        "full": [
                                            {
                                                "groupName": "Основное",
                                                "groupSpecs": [{"name": "model full", "value": "value1"}],
                                            }
                                        ],
                                    },
                                }
                            ]
                        },
                    },
                    {"entity": "product", "id": 101, "titles": {"raw": "Iphone 13"}},
                    {"entity": "product", "id": 102, "titles": {"raw": "Samsung Galaxy S20"}},
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def test_nailed_docs_filtering(self):
        """Проверяем что под флагом market_nailed_docs_white_list=1
        прибитые модели не дублируются в выдаче
        ни при allow-collapsing=0 ни при allow-collapsing=1
        https://st.yandex-team.ru/MARKETOUT-42577
        """

        response = self.report.request_json('place=prime&text=nokia&use-default-offers=1&allow-collapsing=0&debug=da')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    # прибитая скуха
                    {
                        "entity": "product",
                        "id": 103,
                        "titles": {"raw": "Nokia 3310"},
                        "offers": {"items": [{"marketSku": "1004", "titles": {"raw": "Nokia 3310 offer 2"}}]},
                        "debug": {"tech": {"docPriority": "DocRangeNailed"}},
                    },
                    # модель нашлась в модельном шарде и получила другой ДО чем прибитая скуха
                    {
                        "entity": "product",
                        "id": 103,
                        "titles": {"raw": "Nokia 3310"},
                        "offers": {"items": [{"marketSku": "1003", "titles": {"raw": "Nokia 3310 offer 1"}}]},
                        "debug": {"tech": {"docPriority": "DocRangeHead"}, "wareId": Absent()},
                    },
                    # оффер нашелся сам
                    {
                        "entity": "offer",
                        "marketSku": "1003",
                        "titles": {"raw": "Nokia 3310 offer 1"},
                        "debug": {"wareId": "hUElnbhS0nTF3o1F_CnqIQ"},
                    },
                    # другой оффер - тоже нашелся сам
                    {
                        "entity": "offer",
                        "marketSku": "1004",
                        "titles": {"raw": "Nokia 3310 offer 2"},
                        "debug": {"wareId": "WVe1mzsz2_ZcX2auywOU9w"},
                    },
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # если включено схлопывание - то все схлопывается до одной модели
        response = self.report.request_json('place=prime&text=nokia&use-default-offers=1&allow-collapsing=1&debug=da')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "product",
                        "id": 103,
                        "titles": {"raw": "Nokia 3310"},
                        "offers": {"items": [{"marketSku": "1004", "titles": {"raw": "Nokia 3310 offer 2"}}]},
                        "debug": {"tech": {"docPriority": "DocRangeNailed"}},
                    },
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )


if __name__ == '__main__':
    main()

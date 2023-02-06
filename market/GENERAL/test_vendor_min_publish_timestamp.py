#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

import json

from core.testcase import TestCase, main
from core.types import HyperCategory, HyperCategoryType, Model, Offer
from core.matcher import NoKey


class T(TestCase):
    @classmethod
    def prepare_simple_data(cls):
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']
        cls.settings.disable_random = 1
        cls.settings.microseconds_for_disabled_random = 1483228800000000  # 01/01/2017 @ 12:00am (UTC)

        cls.index.hypertree += [
            HyperCategory(hid=24068000, output_type=HyperCategoryType.GURU),
        ]

        cls.index.models += [
            Model(
                title='simple_hidden_model24068001',
                hyperid=24068001,
                hid=24068000,
                vendor_min_publish_timestamp=2052777600,
            ),  # 01/19/2035 @ 12:00am (UTC)
            Model(
                title='simple_hidden_model24068011',
                hyperid=24068011,
                hid=24068010,
                vendor_min_publish_timestamp=2052777600,
            ),  # 01/19/2035 @ 12:00am (UTC)
            Model(title='simple_model_just_24068002', hyperid=24068002, hid=24068000),
            Model(
                title='simple_model_with_text24068003',
                hyperid=24068003,
                hid=24068000,
                vendor_min_publish_timestamp=1451606400,
            ),  # 01/01/2016 @ 12:00am (UTC)
        ]

        cls.index.offers += [
            Offer(
                title='some_offer11',
                hyperid=24068011,
                waremd5='offer_24068011_waremd5',
                hid=24068010,
            )
        ]

    def test_later_models_hidden_on_prime(self):
        for ihg in ["", "debug-ignore-vendor-min-publish-timestamp=0"]:
            response = self.report.request_json('place=prime&hid=24068000&debug=1&{}'.format(ihg))
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "total": 2,
                        "totalModels": 2,
                        "results": [
                            {"id": 24068002},
                            {"id": 24068003},
                        ],
                    },
                    "debug": {"brief": {"filters": {"DENY_TO_PUBLISH_BY_VENDOR_TIMESTAMP": 1}}},
                },
            )

    def test_printdoc_show_vendor_min_publish_timestamp_info(self):
        response = self.report.request_json('place=print_doc&hid=24068000')
        self.assertFragmentIn(
            response,
            {
                "documents": [
                    {
                        "title": "simple_hidden_model24068001",
                        "properties": {
                            "vendor_min_publish_timestamp": {
                                "value": "2052777600",
                                "currentTimestamp": 1483228800,
                                "hidden": True,
                            }
                        },
                    },
                    {
                        "title": "simple_model_with_text24068003",
                        "properties": {
                            "vendor_min_publish_timestamp": {
                                "value": "1451606400",
                                "currentTimestamp": 1483228800,
                                "hidden": False,
                            }
                        },
                    },
                    {
                        "title": "simple_model_just_24068002",
                        "properties": {
                            "vendor_min_publish_timestamp": NoKey("vendor_min_publish_timestamp"),
                        },
                    },
                ]
            },
        )

    def test_hidden_offer_appear_if_debug_ignore_vendor_min_publish_timestamp(self):
        response = self.report.request_json('place=prime&hid=24068000&debug-ignore-vendor-min-publish-timestamp=1')
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 3,
                    "totalModels": 3,
                    "results": [
                        {"id": 24068001},
                        {"id": 24068002},
                        {"id": 24068003},
                    ],
                },
            },
        )

    def test_later_models_hidden_on_modelinfo(self):
        response = self.report.request_json('place=modelinfo&bsformat=2&rids=0&hyperid=24068001')
        self.assertFragmentIn(
            response,
            {
                "search": {"total": 0, "results": []},
            },
            allow_different_len=False,
        )

        for hyperid in [24068002, 24068003]:
            response = self.report.request_json('place=modelinfo&bsformat=2&rids=0&hyperid={}'.format(hyperid))
            self.assertFragmentIn(
                response,
                {
                    "search": {"total": 1, "results": [{"entity": "product", "id": hyperid}]},
                },
                allow_different_len=False,
            )

    def test_later_models_hidden_on_parallel(self):
        response = self.report.request_bs_pb('place=parallel&text=24068001&debug=1')
        model_factors = json.loads(response.get_searcher_props()['Market.Debug.modelFactors'])
        self.assertEqual(0, len(model_factors))

        response = self.report.request_bs_pb('place=parallel&text=24068002&debug=1')
        model_factors = json.loads(response.get_searcher_props()['Market.Debug.modelFactors'])
        self.assertEqual(1, len(model_factors))
        self.assertDictContainsSubset({"title": "simple_model_just_24068002"}, model_factors[0])

        response = self.report.request_bs_pb('place=parallel&text=24068003&debug=1')
        model_factors = json.loads(response.get_searcher_props()['Market.Debug.modelFactors'])
        self.assertEqual(1, len(model_factors))
        self.assertDictContainsSubset({"title": "simple_model_with_text24068003"}, model_factors[0])

    def test_prime_text_search(self):
        response = self.report.request_json('place=prime&text=simple_hidden_model24068001')
        self.assertFragmentNotIn(
            response,
            {
                "results": [
                    {"id": 24068001},
                ]
            },
        )

    def test_prime_collapsing(self):
        for param in ["text=some_offer11", "hid=24068010"]:
            response = self.report.request_json('place=prime&allow-collapsing=1&{}'.format(param))
            # модели нет
            self.assertFragmentNotIn(response, {"results": [{"entity": "product", "id": 24068011}]})
            # но оффер есть
            self.assertFragmentIn(response, {"results": [{"entity": "offer", "model": {"id": 24068011}}]})

        response = self.report.request_json('place=prime&allow-collapsing=1&text=model24068011&ignore-has-gone=1')
        # нашлась модель с оффером
        self.assertFragmentIn(
            response,
            {"results": [{"entity": "product", "id": 24068011}]},
            allow_different_len=True,
            preserve_order=False,
        )

        response = self.report.request_json('place=prime&allow-collapsing=1&text=some_offer11&ignore-has-gone=1')
        # нашелся только оффер, потому что на ModelById не проверяется ignore-has-gone, а тут модель приходит из схлопывания

        self.assertFragmentIn(response, {"results": [{"entity": "offer", "model": {"id": 24068011}}]})


if __name__ == '__main__':
    main()

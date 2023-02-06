#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import Model, Offer, Shop
from core.testcase import TestCase, main
from core.matcher import NoKey


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.models += [
            Model(hyperid=2, title="apple iphone", hid=1, title_no_vendor='iphone'),
            Model(hyperid=3, title="samsung", hid=1),
        ]

        cls.index.shops += [
            Shop(fesh=1001, priority_region=213, regions=[225]),
        ]

        cls.index.offers += [
            Offer(title='apple iphone', title_no_vendor='iphone'),
            Offer(title='samsung'),
            Offer(hyperid=2, fesh=1001),
        ]

    def test_no_highlight(self):
        response = self.report.request_json('place=prime&text=apple')
        self.assertFragmentIn(
            response,
            [
                {
                    "entity": "product",
                    "titles": {
                        "raw": "apple iphone",
                        "highlighted": [
                            {
                                "value": "apple",
                                "highlight": True,
                            },
                            {
                                "value": " iphone",
                                "highlight": NoKey("highlight"),
                            },
                        ],
                    },
                    "titlesWithoutVendor": {
                        "raw": "iphone",
                        "highlighted": [
                            {
                                "value": "iphone",
                                "highlight": NoKey("highlight"),
                            },
                        ],
                    },
                },
                {
                    "entity": "offer",
                    "titles": {
                        "raw": "apple iphone",
                        "highlighted": [
                            {
                                "value": "apple",
                                "highlight": True,
                            },
                            {
                                "value": " iphone",
                                "highlight": NoKey("highlight"),
                            },
                        ],
                    },
                    "titlesWithoutVendor": {
                        "raw": "iphone",
                        "highlighted": [
                            {
                                "value": "iphone",
                                "highlight": NoKey("highlight"),
                            },
                        ],
                    },
                },
            ],
            allow_different_len=False,
        )

    def test_with_highlight(self):
        response = self.report.request_json('place=prime&text=iphone')
        self.assertFragmentIn(
            response,
            [
                {
                    "entity": "product",
                    "titles": {
                        "raw": "apple iphone",
                        "highlighted": [
                            {
                                "value": "apple ",
                                "highlight": NoKey("highlight"),
                            },
                            {
                                "value": "iphone",
                                "highlight": True,
                            },
                        ],
                    },
                    "titlesWithoutVendor": {
                        "raw": "iphone",
                        "highlighted": [
                            {
                                "value": "iphone",
                                "highlight": True,
                            },
                        ],
                    },
                },
                {
                    "entity": "offer",
                    "titles": {
                        "raw": "apple iphone",
                        "highlighted": [
                            {
                                "value": "apple ",
                                "highlight": NoKey("highlight"),
                            },
                            {
                                "value": "iphone",
                                "highlight": True,
                            },
                        ],
                    },
                    "titlesWithoutVendor": {
                        "raw": "iphone",
                        "highlighted": [
                            {
                                "value": "iphone",
                                "highlight": True,
                            },
                        ],
                    },
                },
            ],
            allow_different_len=False,
        )

    def test_no_no_vendor_title(self):
        response = self.report.request_json('place=prime&text=samsung')
        self.assertFragmentIn(
            response,
            [
                {
                    "entity": "product",
                    "titles": {
                        "raw": "samsung",
                    },
                    "titlesWithoutVendor": NoKey("titlesWithoutVendor"),
                },
                {
                    "entity": "offer",
                    "titles": {
                        "raw": "samsung",
                    },
                    "titlesWithoutVendor": NoKey("titlesWithoutVendor"),
                },
            ],
            allow_different_len=False,
        )

    def test_modelinfo(self):
        response = self.report.request_json('place=modelinfo&hyperid=2&rids=213')
        self.assertFragmentIn(
            response, {"id": 2, "titles": {"raw": "apple iphone"}, "titlesWithoutVendor": {"raw": "iphone"}}
        )


if __name__ == '__main__':
    main()

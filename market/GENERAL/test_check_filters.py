#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import GLType, NavCategory
from core.testcase import TestCase, main


class T(TestCase):
    @classmethod
    def prepare(cls):
        # Numeration rules:
        # - nid = {1001, 1002}
        # - hid = {101, 102}
        # - glparam = {201, 202}
        # - glvalue = {301, 302, 303, 401, 402}
        cls.index.navtree_blue = [
            NavCategory(hid=101, nid=1001, is_blue=True, name="Category 1"),
            NavCategory(hid=102, nid=1002, is_blue=True, name="Category 2"),
        ]
        cls.index.navtree = [
            NavCategory(hid=101, nid=11001, is_blue=True, name="Category 1"),
            NavCategory(hid=102, nid=11002, is_blue=True, name="Category 2"),
        ]
        cls.index.gltypes = [
            GLType(param_id=201, hid=101, gltype=GLType.ENUM, values=[301, 302, 303]),
            GLType(param_id=202, hid=101, gltype=GLType.ENUM, values=[401, 402]),
            GLType(param_id=203, hid=102, gltype=GLType.BOOL),
        ]

    def test_valid_glfilter(self):
        response = self.report.request_json("place=check_filters&hid=101&glfilter=201:302")
        self.assertFragmentIn(
            response,
            {
                "result": {
                    "filters": [
                        {
                            "id": "201",
                            "valueIds": ["302"],
                        }
                    ]
                }
            },
        )

    def test_multiple_valid_glfilters(self):
        response = self.report.request_json("place=check_filters&hid=101&glfilter=201:302&glfilter=202:401,402")
        self.assertFragmentIn(
            response,
            {
                "result": {
                    "filters": [
                        {
                            "id": "201",
                            "valueIds": ["302"],
                        },
                        {"id": "202", "valueIds": ["401", "402"]},
                    ]
                }
            },
        )

    def test_invalid_glfilter(self):
        response = self.report.request_json("place=check_filters&hid=101&glfilter=201:302;999:000")
        self.assertFragmentIn(
            response,
            {
                "result": {
                    "filters": [
                        {
                            "id": "201",
                            "valueIds": ["302"],
                        }
                    ]
                }
            },
        )
        self.error_log.not_expect(
            "GlFactory returned null (wrong parameter or value ID?), glfilters: 201:302;999:000, offending filter: 999:000"
        )

    def test_glfilter_with_empty_param_value(self):
        response = self.report.request_json("place=check_filters&hid=101&glfilter=201")
        self.assertFragmentIn(response, {"result": {"filters": []}})
        self.error_log.not_expect("Parameter name or value is empty, glfilters: 201, offending filter: 201")

    def test_glfilter_with_invalid_param_name(self):
        response = self.report.request_json("place=check_filters&hid=101&glfilter=QQQ:302")
        self.assertFragmentIn(response, {"result": {"filters": []}})
        self.error_log.not_expect("Parameter ID is not integer, glfilters: QQQ:302, offending filter: QQQ:302")

    def test_valid_glfilter_with_invalid_value(self):
        response = self.report.request_json("place=check_filters&hid=101&glfilter=201:301,404,303")
        self.assertFragmentIn(
            response,
            {
                "result": {
                    "filters": [
                        {
                            "id": "201",
                            "valueIds": ["301", "303"],
                        }
                    ]
                }
            },
        )

    def test_glfilter_by_nid(self):
        # Required: rgb=blue&use-multi-navigation-trees=1
        response = self.report.request_json(
            "place=check_filters&nid=11001&glfilter=201:302&rgb=blue&use-multi-navigation-trees=1"
        )
        self.assertFragmentIn(
            response,
            {
                "result": {
                    "filters": [
                        {
                            "id": "201",
                            "valueIds": ["302"],
                        }
                    ]
                }
            },
        )
        response = self.report.request_json(
            "place=check_filters&nid=11001&glfilter=201:303;202:401,402&rgb=blue&use-multi-navigation-trees=1"
        )
        self.assertFragmentIn(
            response,
            {
                "result": {
                    "filters": [
                        {
                            "id": "201",
                            "valueIds": ["303"],
                        },
                        {"id": "202", "valueIds": ["401", "402"]},
                    ]
                }
            },
        )


if __name__ == '__main__':
    main()

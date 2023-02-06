#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

import os
from core.types import Model, Offer, Shop
from core.testcase import TestCase, main
from core.matcher import Wildcard

from core.report import DefaultFlags


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.offers += [
            Offer(title='iphone'),
        ]

        cls.index.models += [
            Model(title='iphone 0'),
        ]

        # test home_region_filter
        cls.index.shops += [Shop(fesh=10, home_region=1), Shop(fesh=20, home_region=2)]
        cls.index.offers += [Offer(fesh=10, title="Good 10"), Offer(fesh=20, title="Good 20")]

    def test_prime_textquery_validation(self):
        response = self.report.request_json('place=prime&text=iphone%1A', strict=False)  # bad query
        self.assertFragmentIn(response, {'results': [{'titles': {'raw': 'iphone'}}]})
        self.common_log.expect('Skipped invalid symbol(s) in text query. Parameter: text')

        response = self.report.request_json('place=prime&text=iphone', strict=False)  # valid query
        self.assertFragmentIn(response, {'results': [{'titles': {'raw': 'iphone'}}]})

    def test_pp_absence_generates_error(self):
        response = self.report.request_json(
            'place=prime&text=iphone', strict=False, add_defaults=DefaultFlags.BS_FORMAT
        )
        self.error_log.expect('Some client has not set PP value. Find and punish him violently')
        self.assertEqual(500, response.code)

    def test_home_region_filter_format(self):
        response = self.report.request_json('place=prime&text=Good&home_region_filter=1,2')
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {"entity": "offer", "shop": {"entity": "shop", "homeRegion": {"entity": "region", "id": 1}}},
                        {"entity": "offer", "shop": {"entity": "shop", "homeRegion": {"entity": "region", "id": 2}}},
                    ]
                }
            },
        )

    def test_home_region_filter_apply_only_one(self):
        response = self.report.request_json('place=prime&text=Good&home_region_filter=1')
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {"entity": "offer", "shop": {"entity": "shop", "homeRegion": {"entity": "region", "id": 1}}}
                    ]
                }
            },
        )
        self.assertFragmentNotIn(
            response,
            {
                "search": {
                    "results": [
                        {"entity": "offer", "shop": {"entity": "shop", "homeRegion": {"entity": "region", "id": 2}}}
                    ]
                }
            },
        )

    def test_exec_stats(self):
        """
        Проверяем наличие execution stats в отладочной выдаче.
        """
        log_path = os.path.join(self.meta_paths.logs, 'exec-stats.log')
        self.assertFragmentIn(self.report.request_xml('admin_action=flushlogs'), '<status>Logs flushed ok</status>')
        size_before = os.path.getsize(log_path)

        response = self.report.request_plain('place=prime&text=iphone&debug=1&exec-stats=1')
        self.assertFragmentIn(response, '<!DOCTYPE html>')

        self.assertFragmentIn(self.report.request_xml('admin_action=flushlogs'), '<status>Logs flushed ok</status>')
        size_after = os.path.getsize(log_path)
        # Что-то записалось в лог.
        self.assertGreater(size_after, size_before)

    def test_rearr_factors(self):
        """Проверяем что rearr-factors можно задавать несколько раз и все значения будут сконкатенированы"""

        response = self.report.request_json(
            'place=prime&text=iphone&debug=1&rearr-factors=factor1=1&rearr-factors=factor2=2',
            add_defaults=DefaultFlags.STANDARD,
        )

        self.assertFragmentIn(
            response, {"how": [{"relevance": "main", "args": Wildcard('*rearr_factors: "*factor1=1;*factor2=2*"*')}]}
        )


if __name__ == '__main__':
    main()

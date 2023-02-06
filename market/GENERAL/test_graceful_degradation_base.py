#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa

import json
import re

from core.types import HyperCategory, Offer, Shop
from core.testcase import TestCase, main
from core.emergency_flags import Expression


META_FORCE_LEVEL = 8


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.rty_qpipe = True
        cls.report_log_level = 'All'
        cls.emergency_flags.add_flags(log_every_n_base_request=1)

        cls.index.shops += [
            Shop(fesh=1, priority_region=213),
        ]

        cls.index.offers += [
            Offer(title='lenovo laptop', hid=21, fesh=1),
            Offer(title='nokia', hid=13, fesh=1),
            Offer(title='iphone', hid=44, fesh=1, feedid=100, offerid=1000, price=300),
        ]

        cls.index.hypertree += [
            HyperCategory(hid=21),
            HyperCategory(hid=13),
            HyperCategory(hid=44),
        ]

    def set_gd_params(self, rules=None, force_level=None):
        def make_expression(value, default_value=0):
            return Expression(default_value=default_value, conditions={'value': str(value), 'condition': 'IS_BASE'})

        self.experiment_flags.reset()
        self.experiment_flags.add_flags(
            graceful_degradation_rules=make_expression(json.dumps(rules)),
            graceful_degradation_force_level=make_expression(force_level, default_value=META_FORCE_LEVEL),
        )
        self.experiment_flags.save()
        flag_store_line = 'Store flag graceful_degradation_force_level={}'.format(force_level)
        self.base_common_log.wait_line(flag_store_line)

    def wait_metric_value(self, log_template, times=1):
        prun_count = set()
        regex = re.compile(r'{}: ([-\.\d]+)'.format(log_template))
        for log_line in self.base_common_log.wait_lines(log_template, times):
            mo = regex.search(log_line)
            self.assertTrue(mo is not None)
            prun_count.add(mo.group(1))
        self.assertEqual(len(prun_count), 1)
        return prun_count.pop()

    def assert_prun_count(self, expected):
        expected_prun_count = (expected + 1) * 2 // 3
        prun_count = self.wait_metric_value('Pruning document count')
        self.assertEqual(str(expected_prun_count), prun_count)

    def assert_smm(self, expected):
        smm = self.wait_metric_value('Super mind mult')
        self.assertEqual(str(expected), smm)

    def test_prun_count(self):
        rules = [
            {
                'conditions': ['is_text=0', 'base_level_from=3', 'base_level_to=10'],
                'actions': ['prun_count=1000,0,100,100,0.0888'],
            },
            {
                'conditions': ['is_text=1', 'base_level_from=3', 'base_level_to=10'],
                'actions': ['prun_count=10000,0,100,100,0.0888'],
            },
        ]

        # Check default prun count
        self.set_gd_params(rules=rules, force_level=2)
        self.report.request_json('place=prime&hid=21&debug=da')
        self.assert_prun_count(80000)

        self.set_gd_params(rules=rules, force_level=11)
        self.report.request_json('place=prime&hid=21&debug=da')
        self.assert_prun_count(80000)

        # Check prun count for degradation levels on the range bounds
        self.set_gd_params(rules=rules, force_level=3)
        self.report.request_json('place=prime&hid=21&debug=da')
        self.assert_prun_count(700)  # 1000 - 3 * 100

        self.set_gd_params(rules=rules, force_level=10)
        self.report.request_json('place=prime&hid=21&debug=da')
        self.assert_prun_count(100)

        # Check prun count for request with text
        self.set_gd_params(rules=rules, force_level=3)
        self.report.request_json('place=prime&hid=21&text=iphone&debug=da')
        self.assert_prun_count(9700)

        # Get minimal prun count
        self.set_gd_params(rules=rules, force_level=3)
        self.report.request_json('place=prime&hid=21&text=iphone&prun-count=100&debug=da')
        self.assert_prun_count(100)

    def test_smm(self):
        rules = [
            {'conditions': ['is_text=0', 'base_level_from=3', 'base_level_to=10'], 'actions': ['smm=1,0,0.1,0,0.0888']},
            {
                'conditions': ['is_text=1', 'base_level_from=3', 'base_level_to=10'],
                'actions': ['smm=1,0,0.2,0.1,0.0888'],
            },
        ]

        # Check default smm
        self.set_gd_params(rules=rules, force_level=2)
        self.report.request_json('place=prime&hid=21&debug=da')
        self.assert_smm(1)

        # Check smm degradation levels on the range bounds
        self.set_gd_params(rules=rules, force_level=3)
        self.report.request_json('place=prime&hid=21&debug=da')
        self.assert_smm(0.7)  # 1 - 3 * 0.1

        self.set_gd_params(rules=rules, force_level=10)
        self.report.request_json('place=prime&hid=21&debug=da')
        self.assert_smm(0.02)  # hardcoded min

        # Check prun count for request with text
        self.set_gd_params(rules=rules, force_level=3)
        self.report.request_json('place=prime&hid=21&text=iphone&debug=da')
        self.assert_smm(0.4)

    def test_error(self):
        rules = [
            {"conditions": ["is_text=1", "level_from=3"], "actions": ["error=500"]},
        ]

        self.set_gd_params(rules=rules, force_level=3)

        # Check textless request works fine
        response = self.report.request_json('place=prime&hid=21&debug=da')
        self.assertFragmentIn(response, {"search": {"total": 1}})

        # Check base report doesn't send answer
        response = self.report.request_json('place=prime&hid=21&text=iphone&debug=da')
        self.assertFragmentIn(response, {"search": {"total": 0}})

    def test_disabled_gd(self):
        rules = [
            {
                'conditions': ['base_level_from=0'],
                'actions': ['prun_count=1000,0,100,100,0.0888'],
            },
        ]

        # Check that rule is ok
        self.set_gd_params(rules=rules, force_level=3)
        self.report.request_json('place=prime&hid=21&debug=da')
        self.assert_prun_count(700)

        # Check default prun count with empty rules
        self.set_gd_params(force_level=3)
        self.report.request_json('place=prime&hid=21&debug=da')
        self.assert_prun_count(80000)

        # Check default prun count with rearr
        self.set_gd_params(rules=rules, force_level=3)
        self.report.request_json('place=prime&hid=21&debug=da&rearr-factors=graceful_degradation_force_level=0')
        self.assert_prun_count(80000)


if __name__ == '__main__':
    main()

#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

import os
import sys

import itertools

from core.types import Model, Offer
from core.testcase import TestCase, main
from core.paths import SRCROOT

REARR_VALUES = ["null", "0", "!@#!@#:;"]

DEFAULT_NAME = "default"
SKIP_PRIME_FOR_REARRS_NAME = "skip_prime_for_rearrs"

SKIP_PRIME_FOR_REARRS = set(
    (
        'market_calc_relevance_limit',
        'market_hid_relevance_threshold',
        'output_max_numdoc',
    )
)


class T(TestCase):
    @classmethod
    def read_exp_flags(cls):
        group_names = [DEFAULT_NAME, SKIP_PRIME_FOR_REARRS_NAME]
        groups = [[], []]

        exp_flags_path = os.path.join(SRCROOT, "market/report/library/experiment_flags/experiment_flags")
        with open(exp_flags_path) as fn:
            for line in fn:
                exp_flag = line.strip()

                if exp_flag not in SKIP_PRIME_FOR_REARRS:
                    groups[0].append(exp_flag)
                else:
                    groups[1].append(exp_flag)
        return group_names, groups

    @classmethod
    def beforePrepare(cls):
        cls.settings.use_multiconnect_http_client = True

    @classmethod
    def prepare(cls):
        # disable all default experiments
        cls.settings.default_search_experiment_flags = []

        cls.index.offers += [Offer(title='offer_1', waremd5='ZRK9Q9nKpuAsmQsKgmUtyg')]
        cls.index.models += [Model(title='model_1', hyperid=300)]

    @classmethod
    def _make_rearr_string(cls, rearr_flags_names, rearr_value):
        value_pattern = '={}'.format(rearr_value)
        rearr_flags = (value_pattern + ';').join(rearr_flags_names) + value_pattern
        return "&rearr-factors={}".format(rearr_flags)

    def _check_skip_prime_for_rearrs(self, rearr):
        response = self.report.request_json('place=modelinfo&hyperid=300&rids=213' + rearr)
        self.assertFragmentIn(response, {"titles": {"raw": "model_1"}})

    def _check_default(self, rearr):
        self._check_skip_prime_for_rearrs(rearr)

        response = self.report.request_json('place=prime&text=offer_1 | model_1' + rearr)
        self.assertFragmentIn(response, {"wareId": "ZRK9Q9nKpuAsmQsKgmUtyg"})
        self.assertFragmentIn(response, {"titles": {"raw": "model_1"}})

    def _check_or_find_broken(self, rearr_flags_names, rearr_value, check_response):
        if not rearr_flags_names:
            return None, None

        rearr = self._make_rearr_string(rearr_flags_names, rearr_value)

        try:
            check_response(rearr)
            return None, None
        except Exception as error:
            if len(rearr_flags_names) == 1:
                return rearr_flags_names[0], error

        middle = len(rearr_flags_names) // 2

        left_result = self._check_or_find_broken(rearr_flags_names[:middle], rearr_value, check_response)
        if left_result[0] is not None:
            return left_result

        return self._check_or_find_broken(rearr_flags_names[middle:], rearr_value, check_response)

    def test_rearr_flags(self):
        """
        Проверяем, что во все rearr-флаги можно передать REARR_VALUES и при этом ничего не взорвется
        """

        self.error_log.ignore()  # Ignore flags parse errors
        self.base_logs_storage.error_log.ignore()  # Ignore flags parse errors

        for group, rearr_value in itertools.product(zip(*self.read_exp_flags()), REARR_VALUES):
            group_name, exp_flags = group[0], group[1]

            if group_name == SKIP_PRIME_FOR_REARRS_NAME and rearr_value == "0":
                broken_flag, error = self._check_or_find_broken(
                    exp_flags, rearr_value, self._check_skip_prime_for_rearrs
                )
            else:
                broken_flag, error = self._check_or_find_broken(exp_flags, rearr_value, self._check_default)

            if broken_flag is not None:
                print >> sys.stderr, "Error with rearr flag {}={} : {}".format(broken_flag, rearr_value, error)
                raise error


if __name__ == '__main__':
    main()

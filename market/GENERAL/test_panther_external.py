#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from test_prime import T as TBase

from core.testcase import main

# from unittest import skip


class T(TBase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ["market_money_disable_bids=0"]
        cls.settings.enable_panther_external = True
        cls.settings.default_search_experiment_flags += ['use_external_panther_docs=1']
        cls.settings.default_search_experiment_flags += ['market_new_cpm_iterator=0']

        super(T, cls).prepare()

    def test_external_panther_used(self):
        """Проверяем что все обычные тесты в тесткейсе test_prime.py будут запущены с внешней пантерой"""

        response = self.report.request_json('place=prime&text=samsung&debug=da')
        self.assertFragmentIn(response, {'debug': {'experiments': {'use_external_panther_docs': '1'}}})


if __name__ == '__main__':
    main()

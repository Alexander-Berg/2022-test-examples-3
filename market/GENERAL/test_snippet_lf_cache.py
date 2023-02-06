#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa

from test_snippet import T as TBase
from core.testcase import main


class T(TBase):
    @classmethod
    def prepare_flags(cls):
        cls.emergency_flags.add_flags(ext_snippet_use_global_cache_version='LfWithInterning')
        cls.snippets.global_cache_size = 10000

    def test_tass_lf_cache(self):
        def action():
            self.report.reset_unistats()
            self.report.request_json('place=print_doc&text=sony&rearr-factors=ext_snippet=1')
            self.report.request_json('place=print_doc&text=sony&rearr-factors=ext_snippet=1')
            response = self.report.request_tass_or_wait(wait_hole='market_snippets_global_cache_real_size_attt')

            self.assertGreaterEqual(response.get("market_snippets_global_cache_real_size_attt"), 1)
            self.assertGreaterEqual(response.get("market_snippets_interned_strings_size_sum_attt"), 10)

        self.__retry_action(action)


if __name__ == '__main__':
    main()

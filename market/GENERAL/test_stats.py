#!/usr/bin/env python
# -*- coding: utf-8 -*-
import runner  # noqa

from core.matcher import ValueMatcher
from core.types import Offer
from core.testcase import TestCase, main


STATS = {
    'search_elapsed': ValueMatcher(),
    'base_search_elapsed': ValueMatcher(),
    'meta_search_elapsed': ValueMatcher(),
    'external_requests_time': ValueMatcher(),
    'estimated_max_memory_usage': ValueMatcher(),
    'base_cpu_time_us': ValueMatcher(),
    'meta_cpu_time_us': ValueMatcher(),
    'total_cpu_time_us': ValueMatcher(),
    'wait_time_us': ValueMatcher(),
    'major_faults': ValueMatcher(),
    'external_snippet_stall_time': ValueMatcher(),
    'fetch_time': ValueMatcher(),
    'snippets_fetched': ValueMatcher(),
    'snippet_requests_made': ValueMatcher(),
    'total_documents_processed': ValueMatcher(),
    'total_documents_accepted': ValueMatcher(),
    'docs_before_accept': ValueMatcher(),
    'basesearch_called': ValueMatcher(),
    'response_size': ValueMatcher(),
}


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.offers += [
            Offer(title='iphone'),
        ]

    def test_stats(self):
        response = self.report.request_json('place=prime&text=iphone')
        self.assertFragmentNotIn(response, {'stats': {}})

        response = self.report.request_json('place=prime&text=iphone&show-stats=yes')
        self.assertFragmentIn(response, {'search': {}, 'stats': STATS})

        response = self.report.request_json('place=prime&text=iphone&show-stats=only_stats')
        self.assertFragmentIn(response, {'stats': STATS})
        self.assertFragmentNotIn(response, {'search': {}})


if __name__ == '__main__':
    main()

#!/usr/bin/env python
# -*- coding: utf-8 -*-

import pytest
from mock import call

from context import ReductorForTests
from reductor.reductor import BACKCTLD_PORT


CONFIG = {
    'dcgroups': {
        'market_search-testing-trusty@dummy_dc': {
            "simultaneous_restart": 1,
            "hosts": {
                "msh04et.market.yandex.net": {
                    "redundancy": 1,
                    "name": "msh04et.market.yandex.net",
                    "service": "marketsearch3",
                    "diff_dists": {
                        "search-diff-part-3": {},
                        "search-diff-data": {}
                    },
                    "cluster": 0,
                    "dists": {
                        "search-wizard": {},
                        "search-stats": {},
                        "book-part-3": {},
                        "search-part-3": {},
                        "model-part-3": {},
                        "search-part-11": {},
                        "search-report-data": {},
                        "search-cards": {}
                    }
                },
                "msh-off01et.market.yandex.net": {
                    "cluster": 0,
                    "redundancy": 1,
                    "dists": {
                        "book-snippet-0": {},
                        "search-snippet-data": {},
                        "search-snippet-0": {},
                        "model-snippet-0": {}
                    },
                    "name": "msh-off01et.market.yandex.net",
                    "service": "marketsearchsnippet"
                }
            }
        }
    }
}


@pytest.fixture(scope='module')
def revert_to_base():
    reductor = ReductorForTests(CONFIG)
    for key, mock in reductor.backend_mocks.iteritems():
        mock.do.return_value = 'ok'
    reductor.do_revert_to_base('')
    return reductor


def test_revert_to_base(revert_to_base):
    '''
    msh04et.market.yandex.net содержит diff_dists,
    поэтому там есть сервис marketsearchdiff
    и мы должны дернуть у него ручку revert_to_base
    '''
    backend_key = 'msh04et.market.yandex.net@dummy_dc:{}'.format(BACKCTLD_PORT)
    revert_call = call('marketsearchdiff revert_to_base')
    assert 1 == revert_to_base.backend_mocks[backend_key].do.mock_calls.count(revert_call)


def test_no_revert_for_snippets(revert_to_base):
    '''
    msh-off01et.market.yandex.net не содержит diff_dists,
    поэтому там нечего откатывать.
    '''
    snippet_backend_key = 'msh-off01et.market.yandex.net@dummy_dc:{}'.format(BACKCTLD_PORT)
    assert 0 == len(revert_to_base.backend_mocks[snippet_backend_key].do.mock_calls)

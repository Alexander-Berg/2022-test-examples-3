#!/usr/bin/env python
# -*- coding: utf-8 -*-

import pytest
import mock

from context import ReductorForTests
from reductor.reductor import BACKCTLD_PORT


CONFIG = {
    "reload_timeout": "300",
    'dcgroups': {
        'market_search-testing-trusty@dummy_dc': {
            "simultaneous_restart": 1,
            "close_firewall_sleep": 1,
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
                "msh-par02et.market.yandex.net": {
                    "redundancy": 1,
                    "name": "msh-par02et.market.yandex.net",
                    "service": "marketsearch3",
                    "cluster": 2,
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
                    "redundancy": 1,
                    "name": "msh-off01et.market.yandex.net",
                    "service": "marketsearchsnippet",
                    "cluster": 0,
                    "dists": {
                        "search-snippet-data": {},
                        "search-snippet-4": {},
                        "search-snippet-3": {},
                        "search-snippet-2": {},
                        "search-snippet-1": {},
                        "search-snippet-0": {},
                    }
                }
            }
        }
    }
}

BACKEND_KEY = 'msh04et.market.yandex.net@dummy_dc:{}'.format(BACKCTLD_PORT)
SNIPPET_BACKEND_KEY = 'msh-off01et.market.yandex.net@dummy_dc:{}'.format(BACKCTLD_PORT)
PAR_BACKEND_KEY = 'msh-par02et.market.yandex.net@dummy_dc:{}'.format(BACKCTLD_PORT)


@pytest.fixture(scope='module', params=['both', 'diff'])
def reload_diff(request):
    reductor = ReductorForTests(CONFIG)
    reductor.backend_mocks[BACKEND_KEY].do.return_value = 'ok'
    reductor.backend_mocks[PAR_BACKEND_KEY].do.return_value = 'ok'
    reductor.do_reload('--mode ' + request.param)
    reductor.mode = request.param
    return reductor


def test_close_ipruler_was_called_for_diff(reload_diff):
    close_call = mock.call('marketsearchdiff close_iptruler')
    assert reload_diff.backend_mocks[BACKEND_KEY].do.mock_calls.count(close_call) == 1


def test_open_ipruler_was_called_for_diff(reload_diff):
    open_call = mock.call('marketsearchdiff open_iptruler')
    assert reload_diff.backend_mocks[BACKEND_KEY].do.mock_calls.count(open_call) == 1


def test_reload_called_for_diff(reload_diff):
    reload_call = mock.call('marketsearchdiff reload')
    assert reload_diff.backend_mocks[BACKEND_KEY].do.mock_calls.count(reload_call) == 1


def test_reload_diff_for_host_without_diff_dists(reload_diff):
    # for mode == 'both' expect 4 calls: open, close, reload, check
    expected = 0 if reload_diff.mode == 'diff' else 4
    assert len(reload_diff.backend_mocks[PAR_BACKEND_KEY].do.mock_calls) == expected


@pytest.fixture(scope='module')
def reload_both(request):
    reductor = ReductorForTests(CONFIG)
    reductor.backend_mocks[BACKEND_KEY].do.return_value = 'ok'
    reductor.backend_mocks[PAR_BACKEND_KEY].do.return_value = 'ok'
    reductor.do_reload('--mode both')
    return reductor


def test_open_ipturler_called_for_host_without_diff_dists_while_reload_both(reload_both):
    reload_both.backend_mocks[PAR_BACKEND_KEY].do.assert_any_call('marketsearch3 open_iptruler')


def test_close_ipturler_called_for_host_without_diff_dists_while_reload_both(reload_both):
    reload_both.backend_mocks[PAR_BACKEND_KEY].do.assert_any_call('marketsearch3 close_iptruler')


def test_reload_called_for_host_without_diff_dists_while_reload_both(reload_both):
    reload_both.backend_mocks[PAR_BACKEND_KEY].do.assert_any_call('marketsearch3 reload')


def test_check_called_for_host_without_diff_dists(reload_both):
    reload_both.backend_mocks[PAR_BACKEND_KEY].do.assert_any_call('marketsearch3 check')


@pytest.fixture(scope='module')
def reload_full():
    reductor = ReductorForTests(CONFIG)
    reductor.backend_mocks[BACKEND_KEY].do.return_value = 'ok'
    reductor.backend_mocks[PAR_BACKEND_KEY].do.return_value = 'ok'
    reductor.do_reload('--mode full')
    return reductor


def test_close_ipruler_called_for_full(reload_full):
    close_call = mock.call('marketsearch3 close_iptruler')
    assert reload_full.backend_mocks[BACKEND_KEY].do.mock_calls.count(close_call) == 1


def test_open_ipruler_called_for_full(reload_full):
    open_call = mock.call('marketsearch3 open_iptruler')
    assert reload_full.backend_mocks[BACKEND_KEY].do.mock_calls.count(open_call) == 1


def test_reload_called_for_full(reload_full):
    reload_call = mock.call('marketsearch3 reload')
    assert reload_full.backend_mocks[BACKEND_KEY].do.mock_calls.count(reload_call) == 1


def test_reload_snippets_for_full_mode(reload_full):
    reload_call = mock.call('marketsearchsnippet reload')
    assert reload_full.backend_mocks[SNIPPET_BACKEND_KEY].do.mock_calls.count(reload_call) == 1


@pytest.fixture()
def reductor_for_tests():
    reductor = ReductorForTests(CONFIG)
    reductor.backend_mocks[BACKEND_KEY].do.return_value = 'ok'
    reductor.backend_mocks[PAR_BACKEND_KEY].do.return_value = 'ok'

    def snippet_do_mock(command):
        if command.startswith('marketsearchsnippet get_generation'):
            return '20180117_1756'
        return 'ok'
    reductor.backend_mocks[SNIPPET_BACKEND_KEY].do.side_effect = snippet_do_mock
    return reductor


def test_reload_snippets_when_generation_is_different(reductor_for_tests):
    '''
    Релоадим сниппеты eсли на них лежит не правильное полное поколение.
    '''
    reductor_for_tests.do_reload('--mode diff --diff 20180117_1830 --full 20180116_0000')
    snippet_calls = reductor_for_tests.backend_mocks[SNIPPET_BACKEND_KEY].do.mock_calls
    reload_call = mock.call('marketsearchsnippet reload 20180116_0000 300')
    assert reload_call in snippet_calls


def test_dont_reload_snippets_when_generation_is_correct(reductor_for_tests):
    '''
    Не релоадим сниппеты когда на них уже лежит правильное полное поколение.
    '''
    reductor_for_tests.do_reload('--mode diff --diff 20180117_1830 --full 20180117_1756')
    snippet_get_generation = mock.call('marketsearchsnippet get_generation search-snippet-data')
    snippet_calls = reductor_for_tests.backend_mocks[SNIPPET_BACKEND_KEY].do.mock_calls
    assert snippet_calls == [snippet_get_generation]


def test_dont_reload_snippets_if_we_dont_know_full_generation(reload_diff):
    '''
    Не релоадим сниппеты когда вызываем редуктор по старому, без указания полного поколения.
    '''
    if reload_diff.mode != 'diff':
        return
    snippet_calls = reload_diff.backend_mocks[SNIPPET_BACKEND_KEY].do.mock_calls
    assert not snippet_calls

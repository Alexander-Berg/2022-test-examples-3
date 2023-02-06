# coding: utf-8

from datetime import datetime

from mail.pypg.pypg.fake_cursor import FakeCursor
from pymdb.queries import filters_fetcher

import pytest


FILTER_CREATED = datetime.now()
TEST_FILTER_ROW = [
    1, 'Son marks',
    True, 1, False, FILTER_CREATED, 'user',
]


def make_filters_cursor(conditions, actions):
    return FakeCursor(
        ['rule_id', 'name', 'enabled',
         'prio', 'stop', 'created', 'type',
         'conditions', 'actions'],
        [TEST_FILTER_ROW + [conditions, actions]]
    )


@pytest.mark.parametrize('empty_conditions', [
    None,
    []
])
def test_get_filters_without_conditions(empty_conditions):
    cur = make_filters_cursor(
        empty_conditions,
        [{
            'action_id': 4,
            'oper': 'forward',
            'param': 'wife@',
            'verified': True,
        }]
    )
    result = filters_fetcher(cur)

    assert len(result) == 1, \
        'Expect one filter got %r' % result
    conditions = result[0].conditions
    assert not conditions, \
        'Expect empty conditions got %r' % conditions


@pytest.mark.parametrize('empty_actions', [
    None,
    []
])
def test_raise_on_filters_without_actions(empty_actions):
    cur = make_filters_cursor(
        [{
            'field_type': 'header',
            'field': 'from',
            'pattern': 'school',
            'oper': 'contains',
            'link': 'or',
            'negative': False,
        }],
        empty_actions
    )
    result = filters_fetcher(cur)

    assert len(result) == 1, \
        'Expect one filter got %r' % result
    actions = result[0].actions
    assert not actions, \
        'Expect empty actions got %r' % actions

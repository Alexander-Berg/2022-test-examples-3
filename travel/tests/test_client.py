# -*- encoding: utf-8 -*-
from feature_flag_client import ABFlagsContent
from tests.utils import dump_flag, flag_test_set


def test_with_flags(m, url, client, logger):
    m.get(url, text=dump_flag(['TEST1', 'TEST2']))

    context = client.create_context()
    assert logger.exception.call_count == 0
    assert context.flag_enabled('TEST1')
    assert context.flag_enabled('TEST2')
    assert not context.flag_enabled('TEST3')


def test_without_flags(m, url, client, logger):
    m.get(url, text=dump_flag([]))

    context = client.create_context()
    assert logger.exception.call_count == 0
    assert not context.flag_enabled('TEST1')
    assert not context.flag_enabled('TEST2')


def test_not_ok_status(m, url, client, logger):
    m.get(url, status_code=500)

    context = client.create_context()
    assert logger.exception.call_count == 1

    assert not context.flag_enabled('TEST1')
    assert not context.flag_enabled('TEST2')


@flag_test_set
def test_all_flags(m, url, client, key, value, expected):
    m.get(url, text=dump_flag(['TEST1'], ['AB_TEST1']))

    context = client.create_context()
    ab_content = ABFlagsContent({key: value})
    assert expected == context.flag_enabled(key, ab_content)

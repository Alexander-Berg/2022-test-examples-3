# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import logging

from builtins import range

import pytest
from hamcrest import assert_that, contains, has_entries, contains_string

from travel.rasp.library.python.common23.tester.helpers.caplog import filter_out_runtest_call_records
from travel.rasp.library.python.common23.logging.logging_call_func import logging_call_func


@pytest.fixture(autouse=True)
def caplog_set_info(caplog):
    caplog.set_level(logging.INFO)
    return caplog


@logging_call_func()
def complex_function(raise_error=None, return_value=None, *args, **kwargs):
    if raise_error:
        raise raise_error
    return return_value


def test_simple_func_ok(caplog):
    simple_func_counter = []

    @logging_call_func()
    def simple_func():
        simple_func_counter.append(1)

    simple_func()
    assert len(simple_func_counter) == 1
    records = filter_out_runtest_call_records(caplog.records)
    assert len(records) == 1
    record = records[0]
    assert record.msg == 'Called method %s with *args=%s, **kwargs=%s%s%s'
    assert_that(record.args, contains(
        'simple_func from {}'.format(test_simple_func_ok.__module__),
        (),
        {},
        '\nReturn value: None',
        contains_string('test_simple_func_ok')
    ))


def test_complex_function_ok(caplog):
    res = complex_function(return_value=list(range(10)), some_param=123)
    assert res == list(range(10))
    records = filter_out_runtest_call_records(caplog.records)
    assert len(records) == 1
    record = records[0]
    assert record.msg == 'Called method %s with *args=%s, **kwargs=%s%s%s'
    assert_that(record.args, contains(
        'complex_function from {}'.format(test_complex_function_ok.__module__),
        (),
        has_entries(some_param=123),
        '\nReturn value: [0, 1, 2, 3, 4, 5, 6, 7, 8, 9]',
        contains_string('test_complex_function_ok')
    ))


def test_complex_function_error(caplog):
    expected_error = Exception('expected error message')
    with pytest.raises(Exception):
        complex_function(expected_error, [42], some_param=123)
    records = filter_out_runtest_call_records(caplog.records)
    assert len(records) == 1
    record = records[0]
    assert record.msg == 'Called method %s with *args=%s, **kwargs=%s%s%s'
    assert_that(record.args, contains(
        'complex_function from {}'.format(test_complex_function_error.__module__),
        (expected_error, [42]),
        {'some_param': 123},
        "\nError: expected error message",
        contains_string('test_complex_function_error')
    ))


def test_decorator_params(caplog):
    my_list = []

    @logging_call_func(enabled=False)
    def append_list(list, item):
        list.append(item)
    append_list(my_list, 'disabled_log_call')

    @logging_call_func(log_results=False)
    def append_list(list, item):
        list.append(item)
    append_list(my_list, 'disabled_log_results')

    @logging_call_func(log_trace=False)
    def append_list(list, item):
        list.append(item)
    append_list(my_list, 'disabled_log_trace')

    assert len(my_list) == 3
    records = filter_out_runtest_call_records(caplog.records)
    assert len(records) == 2
    assert_that(records[0].args, contains(
        'append_list from {}'.format(test_decorator_params.__module__),
        (my_list, 'disabled_log_results'),
        {},
        '',
        contains_string('test_decorator_params')
    ))
    assert_that(records[1].args, contains(
        'append_list from {}'.format(test_decorator_params.__module__),
        (my_list, 'disabled_log_trace'),
        {},
        '\nReturn value: None',
        ''
    ))

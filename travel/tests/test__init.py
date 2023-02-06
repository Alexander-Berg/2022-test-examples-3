# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from calendar import timegm
from datetime import datetime
from logging import DEBUG, LogRecord

import mock
import pytest
from hamcrest import assert_that, has_entries, has_entry, anything
from ylog.context import log_context

from travel.rasp.library.python.common23.tester.matchers import has_json
from travel.rasp.library.python.common23.tester.utils.logging import _create_log_record
from travel.rasp.library.python.common23.logging import FieldsInContextFilter, AddContextFilter, JsonFormatter, WarningFilterOnce


@pytest.mark.parametrize('context_data, expected', [
    ({}, False),
    ({'foo': 1}, False),
    ({'foo': 1, 'bar': 2}, True),
])
def test_fields_in_context_filter(context_data, expected):
    filter_instance = FieldsInContextFilter(required_fields=['foo', 'bar'])
    with log_context(**context_data):
        assert filter_instance.filter({}) == expected


@pytest.mark.parametrize('as_dict, expected', [
    (True, {'foo': 1, 'bar': 2}),
    (False, 'foo=1, bar=2')
])
def test_add_context_filter(as_dict, expected):
    filter_instance = AddContextFilter(as_dict=as_dict)
    with log_context(foo=1, bar=2):
        record = mock.Mock()
        filter_instance.filter(record)
        assert record.context == expected

    filter_instance = AddContextFilter(as_dict=as_dict, context_field='my_context')
    with log_context(foo=1, bar=2):
        record = mock.Mock()
        filter_instance.filter(record)
        assert record.my_context == expected


def test_json_formatter():
    utc_dt = datetime(2017, 10, 10, 10, 10, 10, 123456)
    log_record = _create_log_record()
    log_record.context = {'foo': 'bar'}
    log_record.created = timegm(utc_dt.timetuple()) + utc_dt.microsecond / 1000000
    result = JsonFormatter().format(log_record)
    assert_that(result, has_json(has_entries(
        name='name',
        levelname='DEBUG',
        message='',
        created_utc='2017-10-10T10:10:10.123456Z',
        created=anything(),
        context=has_entry('foo', 'bar'),
        process=anything()
    )))


def test_json_formatter_omit_mandatory():
    utc_dt = datetime(2017, 10, 10, 10, 10, 10, 123456)
    log_record = _create_log_record()
    log_record.context = {'foo': 'bar'}
    log_record.created = timegm(utc_dt.timetuple()) + utc_dt.microsecond / 1000000
    result = JsonFormatter(omit_mandatory=True).format(log_record)
    assert_that(result, has_json(has_entries(
        name='name',
        levelname='DEBUG',
        context=has_entry('foo', 'bar'),
        process=anything()
    )))


def test_json_formatter_type_error():
    log_record = _create_log_record()
    log_record.context = {'foo': object()}
    result = JsonFormatter().format(log_record)
    assert_that(result, has_json(has_entries(
        message='',
        created=anything()
    )))


def test_warningfilteronce():
    filter = WarningFilterOnce()
    lr = LogRecord('mog.log', DEBUG, 'aa/ss', 20,
                   'mymsg', (1, 2, 3), None, func=None)
    assert filter.filter(lr)
    assert not filter.filter(lr)


def test_warningfilteronce_unhashable_record():
    filter = WarningFilterOnce()
    lr = LogRecord('mog.log', DEBUG, 'aa/ss', 20,
                   'mymsg', ([1], 2, 3), None, func=None)
    assert filter.filter(lr)
    assert filter.filter(lr)

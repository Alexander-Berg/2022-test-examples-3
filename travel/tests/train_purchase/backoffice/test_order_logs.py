# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import datetime, timedelta

import pytest
from hamcrest import has_entries, assert_that, contains, has_entry

from travel.rasp.train_api.train_purchase.backoffice.order_logs import _format_log_time, database

pytestmark = [pytest.mark.mongouser, pytest.mark.dbuser]


def _create_log_record(**kwargs):
    record = {
        'created_utc': _format_log_time(
            datetime(2017, 12, 1, 13, microsecond=1) + kwargs.pop('timeoffset', timedelta(0))
        ),
        'context': {
            'order_uid': kwargs.pop('order_uid', 'abcdef123456'),
            'process_name': kwargs.pop('process_name', None)
        },
        'name': 'log.name',
        'message': 'Log message'
    }
    record.update(kwargs)
    database.order_logs.insert_one(record)


def test_no_args(backoffice_client):
    _create_log_record()
    data = backoffice_client.get('/ru/train-purchase-backoffice/order-logs/').data

    assert_that(data, has_entries({
        'count': 1,
        'results': contains(has_entries({
            'created_utc': '2017-12-01T13:00:00.000001Z',
            'message': 'Log message'
        }))
    }))


@pytest.mark.parametrize('query, result_matcher', [
    (None, contains(has_entry('message', 'Message 1'), has_entry('message', 'Message 2'),
                    has_entry('message', 'Message 3'))),
    ({'offset': 1}, contains(has_entry('message', 'Message 2'), has_entry('message', 'Message 3'))),
    ({'limit': 1}, contains(has_entry('message', 'Message 1'))),
    ({'limit': 1, 'offset': 1}, contains(has_entry('message', 'Message 2'))),
])
def test_limit_offset(backoffice_client, query, result_matcher):
    _create_log_record(message='Message 1')
    _create_log_record(message='Message 2', timeoffset=timedelta(seconds=1))
    _create_log_record(message='Message 3', timeoffset=timedelta(seconds=2))

    data = backoffice_client.get('/ru/train-purchase-backoffice/order-logs/', query).data
    assert_that(data, has_entries({
        'count': 3,
        'results': result_matcher
    }))


def test_filter_order_uid(backoffice_client):
    _create_log_record(order_uid='1')
    _create_log_record(order_uid='2')

    data = backoffice_client.get('/ru/train-purchase-backoffice/order-logs/', {'orderUID': '2'}).data
    assert_that(data, has_entries({
        'count': 1,
        'results': contains(has_entry('context', has_entry('order_uid', '2')))
    }))


class TestFilterTimeFromTo(object):
    def setup_method(self, method):
        _create_log_record(message='1')
        _create_log_record(message='2', timeoffset=timedelta(seconds=2))

    def test_filter_time_from(self, backoffice_client):
        data = backoffice_client.get('/ru/train-purchase-backoffice/order-logs/',
                                     {'timeFrom': '2017-12-01T13:00:01.000001Z'}).data
        assert_that(data, has_entries({
            'count': 1,
            'results': contains(has_entry('message', '2'))
        }))

    def test_filter_time_to(self, backoffice_client):
        data = backoffice_client.get('/ru/train-purchase-backoffice/order-logs/',
                                     {'timeTo': '2017-12-01T13:00:01.000001Z'}).data
        assert_that(data, has_entries({
            'count': 1,
            'results': contains(has_entry('message', '1'))
        }))


@pytest.mark.parametrize('query, names', [
    ('a', ['a', 'a.b', 'a.b.c']),
    ('a.', ['a.b', 'a.b.c']),
    ('b', ['b.c']),
    ('c', []),
    ('', ['a', 'a.b', 'a.b.c', 'b.c']),
])
def test_filter_by_name(backoffice_client, query, names):
    _create_log_record(name='a.b.c')
    _create_log_record(name='a.b')
    _create_log_record(name='a')
    _create_log_record(name='b.c')

    data = backoffice_client.get('/ru/train-purchase-backoffice/order-logs/', {'name': query}).data
    assert sorted(r['name'] for r in data['results']) == names


@pytest.mark.parametrize('query, levelnames', [
    ('ERROR', ['ERROR']),
    ('WARNING', ['WARNING']),
    ('INFO', ['INFO']),
    ('', ['ERROR', 'INFO', 'WARNING']),
    ('DEBUG', []),
])
def test_filter_by_levelname(backoffice_client, query, levelnames):
    _create_log_record(levelname='ERROR')
    _create_log_record(levelname='WARNING')
    _create_log_record(levelname='INFO')

    data = backoffice_client.get('/ru/train-purchase-backoffice/order-logs/', {'levelname': query}).data
    assert sorted(r['levelname'] for r in data['results']) == levelnames


@pytest.mark.parametrize('query, lognames', [
    ('марка', ['contains.marka', 'startwith.marka']),
    ('мАрКа', ['contains.marka', 'startwith.marka']),
    ('сварка', []),
    ('', ['contains.marka', 'no.marka', 'startwith.marka']),
])
def test_filter_by_message(backoffice_client, query, lognames):
    _create_log_record(name='startwith.marka', message='марка шмарка')
    _create_log_record(name='contains.marka', message='шмаркамарка')
    _create_log_record(name='no.marka', message='шкварка')

    data = backoffice_client.get('/ru/train-purchase-backoffice/order-logs/', {'message': query}).data
    assert sorted(r['name'] for r in data['results']) == lognames


@pytest.mark.parametrize('message, lognames', [
    ('booking', ['process.booking', 'process.empty']),
    ('book', ['process.empty', 'process.payment']),
    ('', ['process.booking', 'process.empty', 'process.payment']),
])
def test_filter_by_process_name_in_message(backoffice_client, message, lognames):
    _create_log_record(name='process.booking', process_name='booking', message='no b00ks here')
    _create_log_record(name='process.payment', process_name='payment', message='django cookbook')
    _create_log_record(name='process.empty', message='message about booking')

    data = backoffice_client.get('/ru/train-purchase-backoffice/order-logs/', {'message': message}).data
    assert sorted(r['name'] for r in data['results']) == lognames

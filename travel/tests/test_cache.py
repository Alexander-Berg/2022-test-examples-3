# coding: utf8

from __future__ import absolute_import, division, print_function, unicode_literals

from datetime import datetime

import ydb
from mock import Mock

from travel.rasp.library.python.ydb.tests.util import create_cache_with_mock


def test_cache_add():
    cache, _, session_pool = create_cache_with_mock()

    add_param = [{
        'key' : 'key string',
        'value' : '{"x":"y"}',
        'expire_at' : datetime.now().strftime('%s') * 1000000  # in milliseconds
    }]

    session, transaction = Mock(), Mock()
    session.transaction.return_value = transaction
    transaction.execute.return_value = []

    def stub_retry_operation_sync(func):
        res = func(session)
        return res

    session_pool.retry_operation_sync.side_effect = stub_retry_operation_sync

    cache.add(add_param)

    session.prepare.assert_called_once_with("""
            DECLARE $data AS "List<Struct<
                key: Utf8,
                value: JsonDocument,
                expire_at: Timestamp
            >>";

            UPSERT INTO `/test/db/name/cache/cache` (key, value, expire_at)
            SELECT
                key, value, expire_at
            FROM AS_TABLE($data);
        """)
    session.transaction.assert_called_once()
    transaction.execute.assert_called_once()
    session_pool.retry_operation_sync.assert_called_once()


def test_cache_get():
    cache, _, session_pool = create_cache_with_mock()

    key, columns = 'some_key', ['key', 'value', 'expire_at']
    row = ydb.convert._Row(columns)
    row['key'] = 'some_key'
    row['value'] = 'some_value'
    row['expire_at'] = 1645218000000000

    session, transaction = Mock(), Mock()
    session.transaction.return_value = transaction
    transaction.execute.return_value = [ydb.convert._ResultSet(columns, [row], None)]

    def stub_retry_operation_sync(func):
        res = func(session)
        return res

    session_pool.retry_operation_sync.side_effect = stub_retry_operation_sync

    rs = cache.get(key)

    session.prepare.assert_called_once_with("""
            DECLARE $key as Utf8;

            SELECT *
            FROM `/test/db/name/cache/cache`
            WHERE
                key = $key AND
                expire_at >= CurrentUtcTimestamp();
        """)
    session.transaction.assert_called_once()
    transaction.execute.assert_called_once()
    session_pool.retry_operation_sync.assert_called_once()

    assert rs == row

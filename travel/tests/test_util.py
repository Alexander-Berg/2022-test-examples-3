# coding: utf8

from __future__ import absolute_import, division, print_function, unicode_literals

import ydb
from mock import Mock

from travel.rasp.library.python.ydb import ensure_path_exists, ensure_table_exists
from travel.rasp.library.python.ydb.tests.util import create_cache_with_mock


def test_path_exists():
    driver = Mock()
    driver.scheme_client.describe_path = Mock(side_effect=ydb.SchemeError('path error'))
    ensure_path_exists(driver, '/x/y')
    driver.scheme_client.make_directory.assert_called_once_with('/x/y')


def test_table_exists():
    cache, driver, session_pool = create_cache_with_mock()

    path1, path2 = Mock(), Mock()
    driver.scheme_client.describe_path.side_effect = [path1, path2]
    path1.is_directory.return_value = True
    path2.is_table.return_value = False

    session = Mock()

    def stub_retry_operation_sync(func):
        res = func(session)
        return res

    session_pool.retry_operation_sync.side_effect = stub_retry_operation_sync

    ensure_table_exists(cache)

    assert driver.scheme_client.describe_path.call_args_list == [(('/test/db/name/cache',),), (('/test/db/name/cache/cache',),)]

    session.create_table.assert_called_once()
    assert session.create_table.call_args.args[0] == '/test/db/name/cache/cache'

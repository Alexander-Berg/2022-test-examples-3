# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import logging

import mock
import pytest
from django.conf import settings
from django.db import connection, reset_queries

from travel.rasp.library.python.common23.db.backends.dbwrapper_base import DatabaseWrapper, MysqlDatabaseWrapper, CursorLoggingWrapper, Database
from travel.rasp.library.python.common23.tester.utils.replace_setting import replace_setting


def test_get_connection_params():
    settings_dict = {'a': 1, 'HOST': 'somehost'}
    dw = DatabaseWrapper(settings_dict)

    params = dw.get_connection_params()
    assert params == settings_dict
    assert params is not settings_dict


@pytest.mark.dbuser
def test_get_db_name():
    db_settings = settings.DATABASES['default']
    dw = DatabaseWrapper(db_settings)

    assert dw.get_db_name() == db_settings['NAME']


@pytest.mark.parametrize('enable_ping', [True, False])
def test_ensure_connection(enable_ping):
    dw = DatabaseWrapper({'HOST': 'somehost'})
    dw.connection = True

    with mock.patch.object(DatabaseWrapper, 'is_usable', mock.Mock(return_value=False)), \
         mock.patch.object(DatabaseWrapper, 'close') as m_close, \
         mock.patch.object(MysqlDatabaseWrapper, 'ensure_connection') as m_ensure_super, \
         replace_setting('PING_MYSQL_BEFORE_EACH_REQUEST', enable_ping):

        dw.ensure_connection()
        assert bool(m_close.call_args_list) is enable_ping
        m_ensure_super.assert_called_once_with()


def test_ping_crush():
    dw = DatabaseWrapper({'HOST': 'somehost'})
    dw.connection = mock.Mock()
    dw.connection.ping.side_effect = Database.Error('Connection error')

    with mock.patch.object(DatabaseWrapper, 'close') as m_close, \
         mock.patch.object(MysqlDatabaseWrapper, 'ensure_connection') as m_ensure_super, \
         replace_setting('PING_MYSQL_BEFORE_EACH_REQUEST', True):

        dw.ensure_connection()
        assert bool(m_close.call_args_list) is True
        m_ensure_super.assert_called_once_with()


@pytest.mark.dbignore
@pytest.mark.parametrize('debug_setting', (True, False))
@pytest.mark.parametrize('log_queries_setting', (True, False))
def test_query_logging(caplog, debug_setting, log_queries_setting):
    reset_queries()
    CursorLoggingWrapper.get_cumulative_time_and_reset()

    with replace_setting('DEBUG', debug_setting), \
         replace_setting('MYSQL_LOG_QUERIES', log_queries_setting), \
         caplog.at_level(logging.DEBUG, logger='travel.rasp.library.python.common23.db.backends.dbwrapper_base'), \
         connection.cursor() as cursor:
        cursor.execute('SELECT 1')

    has_mysql_records = any(record.name == 'travel.rasp.library.python.common23.db.backends.dbwrapper_base'
                            for record in caplog.records)
    if log_queries_setting:
        assert CursorLoggingWrapper.get_cumulative_time_and_reset() > 0
        assert has_mysql_records
    else:
        assert not has_mysql_records

    if debug_setting:
        assert connection.queries
    else:
        assert not connection.queries

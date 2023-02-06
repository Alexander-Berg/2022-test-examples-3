# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from contextlib import contextmanager

import mock
import pytest

from common.tester.utils.replace_setting import replace_setting

from travel.rasp.suburban_tasks.suburban_tasks import rzd_utils
from travel.rasp.suburban_tasks.suburban_tasks.rzd_utils import (
    get_raw_connection, ManagerRequiredError, get_connect, rzd_db_manager, connection_store,
    ManagerAlreadyInitializedError, use_rzd_db_manager, get_connection_to_any_host
)


@mock.patch('ibm_db.connect')
@mock.patch('ibm_db.autocommit')
def test_get_raw_connection(m_ibm_autocommit, m_ibm_connect):
    m_connection = object()

    def mock_connect(connection_string, var1, var2):
        assert 'DATABASE=rzddb' in connection_string
        assert 'HOSTNAME=myhost' in connection_string
        assert 'PORT=555' in connection_string

        return m_connection

    m_ibm_connect.side_effect = mock_connect

    assert get_raw_connection('myhost', 555, 'rzddb') is m_connection
    m_ibm_connect.assert_called_once_with(mock.ANY, '', '')
    m_ibm_autocommit.assert_called_once_with(m_connection, False)


def test_rzd_db_manager_without_connections():
    with rzd_db_manager():
        assert connection_store.connection is None

    assert not hasattr(connection_store, 'connection')


def test_rzd_db_manager_double_initialization():
    with rzd_db_manager():
        with pytest.raises(ManagerAlreadyInitializedError):
            with rzd_db_manager():
                pass


@mock.patch('ibm_db.active')
@mock.patch('ibm_db.close')
def test_rzd_db_manager_close_if_active(m_ibm_close, m_ibm_active):
    m_ibm_active.return_value = True
    connect = object()

    with rzd_db_manager():
        connection_store.connection = connect

    m_ibm_active.assert_called_once_with(connect)
    m_ibm_close.assert_called_once_with(connect)


@mock.patch('travel.rasp.suburban_tasks.suburban_tasks.rzd_utils.rzd_db_manager')
def test_use_rzd_db_manager(m_rzd_db_manager):
    state = []

    @contextmanager
    def context_stub():
        state.append('begin')
        yield
        state.append('end')

    m_rzd_db_manager.side_effect = context_stub

    @use_rzd_db_manager
    def test_fuction():
        assert state == ['begin']

    test_fuction()

    assert state == ['begin', 'end']


def test_get_connect_without_manager():
    with pytest.raises(ManagerRequiredError):
        get_connect()


@mock.patch('ibm_db.active')
@mock.patch('ibm_db.close')
@mock.patch('travel.rasp.suburban_tasks.suburban_tasks.rzd_utils.get_connection_to_any_host')
@mock.patch.object(rzd_utils, 'get_rzd_hosts_and_ports')
@mock.patch('random.shuffle')
def test_get_connect(m_shuffle, m_get_rzd_hosts_and_ports, m_get_connection_to_any_host, m_ibm_close, m_ibm_active):
    m_conn = mock.sentinel.connection
    m_get_rzd_hosts_and_ports.return_value = [('rzdhost1', 42), ('rzdhost2', 43)]
    m_get_connection_to_any_host.return_value = m_conn
    m_ibm_active.side_effect = [False, True]

    with rzd_db_manager():
        result = get_connect('db_name')
        assert connection_store.connection == m_conn

    assert result is m_conn
    assert m_shuffle.call_args_list[0][0][0] is m_get_rzd_hosts_and_ports.return_value
    m_get_connection_to_any_host.assert_called_once_with([('rzdhost1', 42), ('rzdhost2', 43)], 'db_name')
    m_ibm_active.assert_has_calls([mock.call(None), mock.call(m_conn)])
    m_ibm_close.assert_called_once_with(m_conn)


@mock.patch('ibm_db.active')
@mock.patch('ibm_db.close')
@mock.patch('travel.rasp.suburban_tasks.suburban_tasks.rzd_utils.get_connection_to_any_host')
@mock.patch.object(rzd_utils, 'get_rzd_hosts_and_ports')
@mock.patch('random.shuffle')
def test_get_connect_default_db(m_shuffle, m_get_rzd_hosts_and_ports, m_get_connection_to_any_host, m_ibm_close, m_ibm_active):
    m_conn = mock.sentinel.connection
    m_get_rzd_hosts_and_ports.return_value = [('rzdhost1', 42), ('rzdhost2', 43)]
    m_get_connection_to_any_host.return_value = m_conn
    m_ibm_active.side_effect = [False, True]

    with replace_setting('RZD_DATABASE_NAME', 'myrzddb42'):
        with rzd_db_manager():
            result = get_connect()
            assert connection_store.connection == m_conn

        assert result is m_conn
        m_get_connection_to_any_host.assert_called_once_with([('rzdhost1', 42), ('rzdhost2', 43)], 'myrzddb42')
        m_ibm_active.assert_has_calls([mock.call(None), mock.call(m_conn)])
        m_ibm_close.assert_called_once_with(m_conn)


def test_get_connection_to_any_host():
    with mock.patch.object(rzd_utils, 'get_raw_connection') as m_get_raw_connection:

        # can connect
        m_get_raw_connection.side_effect = [Exception, mock.sentinel.conn1]
        result_conn = get_connection_to_any_host([('rzdhost1', 42), ('rzdhost2', 43), ('rzdhost3', 44)], 'rzddb')
        assert result_conn is mock.sentinel.conn1
        assert m_get_raw_connection.call_args_list == [
            mock.call('rzdhost1', 42, 'rzddb'),
            mock.call('rzdhost2', 43, 'rzddb'),
        ]

        # can't connect
        m_get_raw_connection.reset_mock()
        m_get_raw_connection.side_effect = [Exception]
        with pytest.raises(Exception) as ex:
            get_connection_to_any_host([('rzdhost1', 42), ('rzdhost2', 43), ('rzdhost3', 44)], 'rzddb')

        assert "Can't connect" in str(ex)
        assert m_get_raw_connection.call_args_list == [
            mock.call('rzdhost1', 42, 'rzddb'),
            mock.call('rzdhost2', 43, 'rzddb'),
            mock.call('rzdhost3', 44, 'rzddb'),
        ]

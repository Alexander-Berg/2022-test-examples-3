# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from builtins import str
import hamcrest
import mock
import pytest
from mongoengine import connection

from travel.rasp.library.python.common23.data_api.dbaas.client import DbaasClient, DbType
from travel.rasp.library.python.common23.data_api.dbaas.host_info import HostInfo
from travel.rasp.library.python.common23.db.mongo import mongo
from travel.rasp.library.python.common23.db.mongo.command_logger import CommandLogger
from travel.rasp.library.python.common23.db.mongo.mongo import (
    ConnectionProxy, DatabaseError, Databases, get_conn_string, register_mongo_connections
)
from travel.rasp.library.python.common23.tester.utils.replace_setting import replace_setting


class TestGetConnString(object):
    def test_basic(self):
        conf = {'host': 'host42', 'port': 27018}
        assert get_conn_string(conf) == 'mongodb://host42:27018'

    def test_hosts_from_dbaas(self):
        conf = {
            'host': 'host42',
            'dbaas_id': 'group42',
            'db': 'db42',
        }

        with mock.patch.object(DbaasClient, 'get_hosts') as m_get_hosts:
            hosts = [HostInfo('host1', ''), HostInfo('host2', '')]
            m_get_hosts.side_effect = lambda g, t: {'group42': {DbType.MONGODB: hosts}}[g][t]

            assert get_conn_string(conf) == 'mongodb://host1:27017,host2:27017'

    def test_fallback_hosts(self):
        with mock.patch.object(DbaasClient, 'get_hosts') as m_get_hosts:
            conf = {
                'host': 'host42',
                'dbaas_id': 'group42',
                'db': 'db42',
                'fallback_hosts': ['fallback42', 'fallback43'],
            }

            m_get_hosts.side_effect = ValueError('aaaa')
            assert get_conn_string(conf) == 'mongodb://fallback42:27017,fallback43:27017'

            conf.pop('fallback_hosts')
            with pytest.raises(ValueError) as ex:
                get_conn_string(conf)
            assert 'aaaa' in str(ex)

    def test_yandexcloud_not_imported(self):
        @property
        def fail_sdk(self):
            raise ImportError('yandexcloud')

        with mock.patch.object(DbaasClient, 'sdk', fail_sdk):
            conf = {
                'host': 'host42',
                'dbaas_id': 'group42',
                'db': 'db42',
                'fallback_hosts': ['fallback42', 'fallback43'],
            }

            assert get_conn_string(conf) == 'mongodb://fallback42:27017,fallback43:27017'
            conf.pop('fallback_hosts')
            with pytest.raises(ImportError):
                get_conn_string(conf)


@replace_setting('MONGO_DATABASES', {'foo': {'db': 'foo_db'}, 'bar': {'db': 'bar_db'}})
@replace_setting('MONGO_LOG_COMMANDS', False)
@mock.patch.object(mongo, 'register_connection', autospec=True)
def test_register_mongo_connections(m_register_connection):
    register_mongo_connections()

    m_register_connection.assert_has_calls([
        mock.call('foo', name='foo_db', host=mock.ANY, username=None, password=None),
        mock.call('bar', name='bar_db', host=mock.ANY, username=None, password=None)
    ], any_order=True)


@replace_setting('MONGO_DATABASES', {'foo': {'db': 'foo_db'}, 'bar': {'db': 'bar_db'}})
@replace_setting('MONGO_LOG_COMMANDS', True)
@mock.patch.object(mongo, 'register_connection', autospec=True)
def test_register_mongo_connections_with_debug(m_register_connection):
    register_mongo_connections()

    command_logger = hamcrest.match_equality(hamcrest.instance_of(CommandLogger))
    m_register_connection.assert_has_calls([
        mock.call('foo', name='foo_db', host=mock.ANY, username=None, password=None,
                  event_listeners=[command_logger]),
        mock.call('bar', name='bar_db', host=mock.ANY, username=None, password=None,
                  event_listeners=[command_logger])
    ], any_order=True)


class TestDatabases(object):
    def test_valid(self):
        dbs = Databases()

        with replace_setting('MONGO_DATABASES', {}):
            with pytest.raises(DatabaseError):
                db = dbs['db42']

        db_mock = mock.Mock()
        db_mock.name = 'db42_name'
        with replace_setting('MONGO_DATABASES', {'db42': {'some_setting': '42'}}):
            with mock.patch.object(connection, 'get_db') as m_get_db:
                m_get_db.return_value = db_mock
                db = dbs['db42']
                assert isinstance(db, ConnectionProxy)
                assert db.alias == 'db42'
                assert db.name == 'db42_name'
                m_get_db.assert_called_once_with('db42')

                prev_db_alias = db

                db = dbs['db42']
                assert db is prev_db_alias
                assert db.name == 'db42_name'
                assert m_get_db.call_count == 1

    def test_instance(self):
        assert isinstance(mongo.databases, Databases)


class TestDatabaseAliasProxy(object):
    def test_valid(self):
        connect_mock = mock.Mock(some_attr=mock.sentinel.some_attr)
        db_proxy = ConnectionProxy('alias42')
        with mock.patch.object(connection, 'get_db') as m_get_db:
            m_get_db.return_value = connect_mock

            assert db_proxy.some_attr == mock.sentinel.some_attr
            m_get_db.assert_called_once_with('alias42')

    def test_instance(self):
        assert isinstance(mongo.database, ConnectionProxy)
        assert mongo.database.alias == 'default'

    @mock.patch.object(ConnectionProxy, 'connections_registry')
    def test_close_connections(self, m_connections_registry):
        d = {'foo': mock.Mock()}
        m_connections_registry.__getitem__.side_effect = d.__getitem__
        m_connections_registry.items.side_effect = d.items

        proxy = ConnectionProxy('bar')
        proxy.close_connections()
        assert proxy.connections_registry['foo'].mock_calls == [mock.call.client.close()]

    def test_connection_saved_in_registry(self):
        db_proxy = ConnectionProxy('alias41')
        with mock.patch.object(connection, 'get_db', autospec=True) as m_get_db:
            m_get_db.return_value = mock.MagicMock(foo=mock.sentinel.foo)
            assert db_proxy.foo == mock.sentinel.foo
            assert m_get_db.call_count == 1

            assert db_proxy.foo == mock.sentinel.foo
            assert m_get_db.call_count == 1

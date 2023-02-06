# -*- coding: utf-8 -*-

import unittest
import mock
import urlparse
import psycopg2

from nose_parameterized import parameterized
from sqlalchemy.engine import base
from sqlalchemy.exc import InternalError
from sqlalchemy.engine.base import Connection as _Connection

from mpfs.metastorage.postgres.exceptions import ReadOnlyDatabaseError
from mpfs.metastorage.postgres.services import (Sharpei, MasterNotFoundError, SlavesNotFoundError,
                                                SharpeiJsonResponseError,
                                                SharpeiIncompleteResponseError, MasterIsDeadError, Shard)
from mpfs.metastorage.postgres import query_executer
from mpfs.metastorage.postgres.query_executer import PGQueryExecuter, Connection, ReadPreference
from mpfs.metastorage.postgres.schema import files


class FakeRequestsResponse(object):
    def __init__(self, raw_resp):
        self.content = raw_resp


sharpei_response = FakeRequestsResponse("""
{
    "shard": {
        "id": "7",
        "name": "disk-mpfs-test-777",
        "databases": [
            {
                "address": {
                    "host": "xdb01e.mail.yandex.net",
                    "port": "6432",
                    "dbname": "maildb",
                    "dataCenter": "IVA"
                },
                "role": "master",
                "status":"alive",
                "state":{
                    "lag":0
                }
            },
            {
                "address": {
                    "host": "xdb01f.mail.yandex.net",
                    "port": "6432",
                    "dbname": "maildb",
                    "dataCenter": "MYT"
                },
                "role": "replica",
                "status":"alive",
                "state":{
                    "lag":10
                }
            },
            {
                "address": {
                    "host": "xdb01g.mail.yandex.net",
                    "port": "6432",
                    "dbname": "maildb",
                    "dataCenter": "FOL"
                },
                "role": "replica",
                "status":"alive",
                "state":{
                    "lag":40
                }
            }
        ]
    }
}
""")

sharpei_response_with_dead_replica = FakeRequestsResponse("""
{
    "shard": {
        "id": "7",
        "name": "disk-mpfs-test-777",
        "databases": [
            {
                "address": {
                    "host": "xdb01e.mail.yandex.net",
                    "port": "6432",
                    "dbname": "maildb",
                    "dataCenter": "IVA"
                },
                "role": "master",
                "status":"alive",
                "state":{
                    "lag":0
                }
            },
            {
                "address": {
                    "host": "xdb01f.mail.yandex.net",
                    "port": "6432",
                    "dbname": "maildb",
                    "dataCenter": "MYT"
                },
                "role": "replica",
                "status":"dead",
                "state":{
                    "lag":0
                }
            },
            {
                "address": {
                    "host": "xdb01g.mail.yandex.net",
                    "port": "6432",
                    "dbname": "maildb",
                    "dataCenter": "FOL"
                },
                "role": "replica",
                "status":"alive",
                "state":{
                    "lag":0
                }
            }
        ]
    }
}
""")

sharpei_response_with_dead_master = FakeRequestsResponse("""
{
    "shard": {
        "id": "7",
        "name": "disk-mpfs-test-777",
        "databases": [
            {
                "address": {
                    "host": "xdb01e.mail.yandex.net",
                    "port": "6432",
                    "dbname": "maildb",
                    "dataCenter": "IVA"
                },
                "role": "master",
                "status":"dead",
                "state":{
                    "lag":0
                }
            }
        ]
    }
}
""")

sharpei_masterless_response = FakeRequestsResponse("""
{
    "shard": {
        "id": "7",
        "name": "disk-mpfs-test-777",
        "databases": [
            {
                "address": {
                    "host": "xdb01f.mail.yandex.net",
                    "port": "6432",
                    "dbname": "maildb",
                    "dataCenter": "MYT"
                },
                "role": "replica",
                "status":"alive",
                "state":{
                    "lag":10
                }
            },
            {
                "address": {
                    "host": "xdb01g.mail.yandex.net",
                    "port": "6432",
                    "dbname": "maildb",
                    "dataCenter": "FOL"
                },
                "role": "replica",
                "status":"alive",
                "state":{
                    "lag":40
                }
            }
        ]
    }
}
""")

sharpei_slaveless_response = FakeRequestsResponse("""
{
    "shard": {
        "id": "7",
        "name": "disk-mpfs-test-777",
        "databases": [
            {
                "address": {
                    "host": "xdb01e.mail.yandex.net",
                    "port": "6432",
                    "dbname": "maildb",
                    "dataCenter": "IVA"
                },
                "role": "master",
                "status":"alive",
                "state":{
                    "lag":0
                }
            }
        ]
    }
}
""")

sharpei_incomplete_response = FakeRequestsResponse("""
{
    "shard": {
        "id": "7",
        "name": "disk-mpfs-test-777",
        "databases": [
            {
                "address": {
                    "host": "xdb01e.mail.yandex.net",
                    "port": "6432",
                    "dataCenter": "IVA"
                },
                "role": "master",
                "status":"alive",
                "state":{
                    "lag":0
                }
            }
        ]
    }
}
""")

sharpei_common_shard = Shard({
    "id": 0,
    "name": "disk_commondb",
    "databases": [],
})


sharpei_several_shards_response = FakeRequestsResponse("""
{
    "7": {
        "id": "7",
        "name": "disk-mpfs-test-777",
        "databases": [
            {
                "address": {
                    "host": "xdb01e.mail.yandex.net",
                    "port": "6432",
                    "dbname": "maildb",
                    "dataCenter": "IVA"
                },
                "role": "master",
                "status":"alive",
                "state":{
                    "lag":0
                }
            },
            {
                "address": {
                    "host": "xdb01f.mail.yandex.net",
                    "port": "6432",
                    "dbname": "maildb",
                    "dataCenter": "MYT"
                },
                "role": "replica",
                "status":"alive",
                "state":{
                    "lag":10
                }
            },
            {
                "address": {
                    "host": "xdb01g.mail.yandex.net",
                    "port": "6432",
                    "dbname": "maildb",
                    "dataCenter": "FOL"
                },
                "role": "replica",
                "status":"alive",
                "state":{
                    "lag":40
                }
            }
        ]
    },
    "42": {
        "id": "42",
        "name": "disk-mpfs-test-777",
        "databases": [
            {
                "address": {
                    "host": "xdb02f.mail.yandex.net",
                    "port": "6432",
                    "dbname": "maildb",
                    "dataCenter": "MYT"
                },
                "role": "replica",
                "status":"alive",
                "state":{
                    "lag":10
                }
            },
            {
                "address": {
                    "host": "xdb02g.mail.yandex.net",
                    "port": "6432",
                    "dbname": "maildb",
                    "dataCenter": "FOL"
                },
                "role": "replica",
                "status":"alive",
                "state":{
                    "lag":40
                }
            }
        ]
    }
}
""")


class PGWrapperTestCase(unittest.TestCase):
    class FakeConnection(Connection):
        def __init__(self, host, dbname):
            self.host = host
            self.dbname = dbname

    class FakeEngine(base.Connectable):
        def __init__(self, host, dbname):
            self.host = host
            self.dbname = dbname

        def connect(self):
            return PGWrapperTestCase.FakeConnection(self.host, self.dbname)

        @staticmethod
        def create_engine(connection_string, **kwargs):
            parts = urlparse.urlparse(connection_string)
            dbname = parts.path.strip('/')
            return PGWrapperTestCase.FakeEngine(parts.hostname, dbname)

    def setUp(self):
        self.uid = '1234567890'
        self.shard_id = '7'
        PGQueryExecuter.reset_shapei_cache()

    def run(self, result=None):
        with mock.patch.object(query_executer, 'POSTGRES_SHARPEI_REPLICA_FORBIDDEN_DATACENTERS', []), \
             mock.patch.object(query_executer, 'POSTGRES_SHARPEI_REPLICATION_LAG_THESHOLD', 100500):
            return super(PGWrapperTestCase, self).run(result)

    def test_get_direct_connection(self):
        """
        Тестируем получение прямого коннекта к хосту с базой
        """
        with mock.patch.object(Sharpei, 'request', return_value=sharpei_response),\
                mock.patch.object(query_executer, 'create_engine', PGWrapperTestCase.FakeEngine.create_engine):

            pg_query_executer = PGQueryExecuter()
            connection = pg_query_executer.get_connection_by_shard_id(self.shard_id)

            assert isinstance(connection, PGWrapperTestCase.FakeConnection)
            assert connection.host == 'xdb01e.mail.yandex.net'
            assert connection.dbname == 'maildb'

    def test_filter_replica_with_filtered_datacenter(self):
        """
        Тестируем фильтрацию хостов из определенного ДЦ, если спрашиваем слейва
        """
        with mock.patch.object(Sharpei, 'request', return_value=sharpei_response), \
                mock.patch.object(query_executer, 'create_engine', PGWrapperTestCase.FakeEngine.create_engine), \
                mock.patch.object(query_executer, 'POSTGRES_SHARPEI_REPLICA_FORBIDDEN_DATACENTERS', ['MYT']):
            pg_query_executer = PGQueryExecuter()
            connection = pg_query_executer.get_connection_by_shard_id(self.shard_id,
                                                                      read_preference=ReadPreference.secondary)

            assert isinstance(connection, PGWrapperTestCase.FakeConnection)
            assert connection.host == 'xdb01g.mail.yandex.net'

    @parameterized.expand([
        (50, ['xdb01f.mail.yandex.net', 'xdb01g.mail.yandex.net'], False),
        (20, ['xdb01f.mail.yandex.net'], False),
        (1, None, True)
    ])
    def test_filter_replica_with_large_lag(self, replication_lag_theshold, expected_hosts, is_not_found_exception_expected):
        # Тестируем фильтрацию хостов слейвов, если лаг больше определенного значения
        with mock.patch.object(Sharpei, 'request', return_value=sharpei_response), \
             mock.patch.object(query_executer, 'create_engine', PGWrapperTestCase.FakeEngine.create_engine), \
             mock.patch.object(query_executer, 'POSTGRES_SHARPEI_REPLICA_FORBIDDEN_DATACENTERS', []), \
             mock.patch.object(query_executer, 'POSTGRES_SHARPEI_REPLICATION_LAG_THESHOLD', replication_lag_theshold):
            pg_query_executer = PGQueryExecuter()

            for _ in xrange(10):  # ретраим, потому что внутри шарды могут перемешиваться
                try:
                    connection = pg_query_executer.get_connection_by_shard_id(self.shard_id,
                                                                              read_preference=ReadPreference.secondary)
                    assert isinstance(connection, PGWrapperTestCase.FakeConnection)
                    assert connection.host in expected_hosts
                except SlavesNotFoundError:
                    if not is_not_found_exception_expected:
                        self.fail('SlavesNotFoundError raised but was not expected')
                else:
                    if is_not_found_exception_expected:
                        self.fail('SlavesNotFoundError raised but was not expected')

    def test_filter_replica_with_dead_state(self):
        # Тестируем фильтрацию хостов слейвов, если у них статус dead
        with mock.patch.object(Sharpei, 'request', return_value=sharpei_response_with_dead_replica), \
             mock.patch.object(query_executer, 'create_engine', PGWrapperTestCase.FakeEngine.create_engine), \
             mock.patch.object(query_executer, 'POSTGRES_SHARPEI_REPLICA_FORBIDDEN_DATACENTERS', []):
            pg_query_executer = PGQueryExecuter()

            for _ in xrange(10):  # ретраим, потому что внутри шарды могут перемешиваться
                connection = pg_query_executer.get_connection_by_shard_id(self.shard_id,
                                                                          read_preference=ReadPreference.secondary)
                assert isinstance(connection, PGWrapperTestCase.FakeConnection)
                assert connection.host in 'xdb01g.mail.yandex.net'

    def test_master_in_dead_state(self):
        # Тестируем фильтрацию хостов слейвов, если у них статус dead
        with mock.patch.object(Sharpei, 'request', return_value=sharpei_response_with_dead_master), \
             mock.patch.object(query_executer, 'create_engine', PGWrapperTestCase.FakeEngine.create_engine), \
             mock.patch.object(query_executer, 'POSTGRES_SHARPEI_REPLICA_FORBIDDEN_DATACENTERS', []):
            pg_query_executer = PGQueryExecuter()

            with self.assertRaises(MasterIsDeadError):
                pg_query_executer.get_connection_by_shard_id(self.shard_id,
                                                             read_preference=ReadPreference.primary)

    def test_get_connection_without_master(self):
        """
        Тестируем получение коннекта, если в ответе от шарпея нету мастера
        """
        with mock.patch.object(Sharpei, 'request', return_value=sharpei_masterless_response),\
                mock.patch.object(query_executer, 'create_engine', PGWrapperTestCase.FakeEngine.create_engine):
            pg_query_executer = PGQueryExecuter()

            try:
                pg_query_executer.get_connection_by_shard_id(self.shard_id)
            except MasterNotFoundError:
                pass
            else:
                self.assertTrue(False, 'MasterNotFoundError expected but there wasn\'t one')

    def test_get_connection_without_slaves(self):
        """
        Тестируем получение коннекта, если хотим получить коннект до слейва, а его нет
        """
        with mock.patch.object(Sharpei, 'request', return_value=sharpei_slaveless_response),\
                mock.patch.object(query_executer, 'create_engine', PGWrapperTestCase.FakeEngine.create_engine):
            pg_query_executer = PGQueryExecuter()

            try:
                pg_query_executer.get_connection_by_shard_id(self.shard_id, read_preference=ReadPreference.secondary)
            except SlavesNotFoundError:
                pass
            else:
                self.assertTrue(False, 'SlavesNotFoundError expected but there wasn\'t one')

    def test_broken_sharpei_response(self):
        """
        Тестируем, что будет, если шарпей возвращает какой-то невалидный json
        """
        with mock.patch.object(Sharpei, 'request', return_value=FakeRequestsResponse('{123749812739487')),\
                mock.patch.object(query_executer, 'create_engine', PGWrapperTestCase.FakeEngine.create_engine):
            pg_query_executer = PGQueryExecuter()

            try:
                pg_query_executer.get_connection_by_shard_id(self.shard_id, read_preference=ReadPreference.secondary)
            except SharpeiJsonResponseError:
                pass
            else:
                self.assertTrue(False, 'SharpeiJsonResponseError expected but there wasn\'t one')

    def test_incomplete_sharpei_response(self):
        """
        Тестируем, что будет, если шарпей возвращает валидный json, но каких-то нужных данных там нет
        """
        with mock.patch.object(Sharpei, 'request', return_value=sharpei_incomplete_response),\
                mock.patch.object(query_executer, 'create_engine', PGWrapperTestCase.FakeEngine.create_engine):
            pg_query_executer = PGQueryExecuter()

            try:
                pg_query_executer.get_connection_by_shard_id(self.shard_id, read_preference=ReadPreference.secondary)
            except SharpeiIncompleteResponseError:
                pass
            else:
                self.assertTrue(False, 'SharpeiIncompleteResponseError expected but there wasn\'t one')

    def test_read_only_insert(self):
        """
        Тестируем, что будет, если сделать insert в рид-онли базу
        """
        def fake_execute(*args, **kwargs):
            error = psycopg2.InternalError()
            error.pgcode = '25006'
            raise InternalError('', '', error)

        with mock.patch.object(Sharpei, 'request', return_value=sharpei_response),\
                mock.patch.object(query_executer, 'create_engine', PGWrapperTestCase.FakeEngine.create_engine),\
                mock.patch.object(_Connection, 'execute', fake_execute):
            pg_query_executer = PGQueryExecuter()
            c = pg_query_executer.get_connection_by_shard_id(self.shard_id, read_preference=ReadPreference.secondary)
            try:
                c.execute(files.insert().values(uid=self.uid))
            except ReadOnlyDatabaseError:
                pass
            else:
                self.assertTrue(False, 'ReadOnlyDatabaseError expected but there wasn\'t one')

    @parameterized.expand([
        (True,),
        (False,),
    ])
    def test_get_connection_to_all_shards(self, use_threads):
        with mock.patch.object(Sharpei, 'request', return_value=sharpei_several_shards_response), \
             mock.patch.object(Sharpei, 'get_common_shard', return_value=sharpei_common_shard), \
             mock.patch.object(query_executer, 'create_engine', PGWrapperTestCase.FakeEngine.create_engine):
            pg_query_executer = PGQueryExecuter()
            connections = pg_query_executer.get_connection_for_all_shards(
                skip_unavailable_shards=False,
                read_preference=ReadPreference.secondary_preferred,
                use_threads=use_threads,
            )
            assert len(connections) == 2
            for conn in connections:
                if conn.host.startswith('xdb01'):
                    assert conn.host in ('xdb01f.mail.yandex.net', 'xdb01g.mail.yandex.net')
                elif conn.host.startswith('xdb02'):
                    assert conn.host in ('xdb02f.mail.yandex.net', 'xdb02g.mail.yandex.net')
                else:
                    self.fail('unknown connection host')

    @parameterized.expand([
        (True,),
        (False,),
    ])
    def test_get_connection_to_all_shard_masters(self, use_threads):
        with mock.patch.object(Sharpei, 'request', return_value=sharpei_several_shards_response), \
             mock.patch.object(Sharpei, 'get_common_shard', return_value=sharpei_common_shard), \
             mock.patch.object(query_executer, 'create_engine', PGWrapperTestCase.FakeEngine.create_engine):
            pg_query_executer = PGQueryExecuter()
            try:
                pg_query_executer.get_connection_for_all_shards(
                    skip_unavailable_shards=False,
                    read_preference=ReadPreference.primary,
                    use_threads=use_threads,
                )
            except MasterNotFoundError:
                pass
            else:
                self.fail('MasterNotFoundError expected')

    @parameterized.expand([
        (True,),
        (False,),
    ])
    def test_get_connection_to_all_shards_with_skip_unavailable(self, use_threads):
        with mock.patch.object(Sharpei, 'request', return_value=sharpei_several_shards_response), \
             mock.patch.object(Sharpei, 'get_common_shard', return_value=sharpei_common_shard), \
             mock.patch.object(query_executer, 'create_engine', PGWrapperTestCase.FakeEngine.create_engine):
            pg_query_executer = PGQueryExecuter()
            connections = pg_query_executer.get_connection_for_all_shards(
                skip_unavailable_shards=True,
                read_preference=ReadPreference.primary,
                use_threads=use_threads,
            )
            assert len(connections) == 1
            connection = connections[0]
            assert connection.host == 'xdb01e.mail.yandex.net'

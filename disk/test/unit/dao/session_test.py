# -*- coding: utf-8 -*-
import threading
import mock
import unittest

import time

from mpfs.dao.session import Session
from mpfs.metastorage.postgres.query_executer import PGQueryExecuter


class SessionTestCase(unittest.TestCase):
    uid = '123123123'
    shard_id = '1'

    def setup_method(self, method):
        Session.clear_cache()

    def test_creation_by_uid(self):
        connection = mock.Mock()

        with mock.patch.object(PGQueryExecuter, 'get_shard_id', return_value=self.shard_id), \
                mock.patch.object(PGQueryExecuter, 'get_connection_by_shard_id',
                                  return_value=connection) as fake_get_connection:
            session = Session.create_from_uid(self.uid)
            session.execute('some sql query')

            assert fake_get_connection.call_count == 1
            assert fake_get_connection.call_args[0][0] == self.shard_id

    def test_cache_by_uid(self):
        connection = mock.Mock()

        with mock.patch.object(PGQueryExecuter, 'get_shard_id', return_value=self.shard_id), \
                mock.patch.object(PGQueryExecuter, 'get_connection_by_shard_id',
                                  return_value=connection) as fake_get_connection:
            session = Session.create_from_uid(self.uid)

            session.execute('some sql query')
            session.execute('some sql query 2')
            session.execute('some sql query 3')

            assert fake_get_connection.call_count == 1
            assert fake_get_connection.call_args[0][0] == self.shard_id

    def test_multithreaded_cache_by_uid(self):
        connection = mock.Mock()

        def worker_func():
            session = Session.create_from_uid(self.uid)
            session.execute('some sql query')
            session.execute('some sql query 2')
            session.execute('some sql query 3')
            time.sleep(0.1)

        thread1 = threading.Thread(target=worker_func)
        thread2 = threading.Thread(target=worker_func)
        thread3 = threading.Thread(target=worker_func)

        with mock.patch.object(PGQueryExecuter, 'get_shard_id', return_value=self.shard_id), \
                mock.patch.object(PGQueryExecuter, 'get_connection_by_shard_id',
                                  return_value=connection) as fake_get_connection:
            for t in (thread1, thread2, thread3):
                t.start()
            for t in (thread1, thread2, thread3):
                t.join()

            assert fake_get_connection.call_count == 3
            for i in xrange(3):
                assert fake_get_connection.call_args_list[i][0][0] == self.shard_id

    def test_context_manager(self):
        connection = mock.Mock()

        with mock.patch.object(PGQueryExecuter, 'get_shard_id', return_value=self.shard_id), \
                mock.patch.object(PGQueryExecuter, 'get_connection_by_shard_id', return_value=connection):
            session = Session.create_from_uid(self.uid)
            session.execute('SELECT * FROM test')
            with session.begin():
                session.execute('SELECT id FROM test')

                with session.begin():
                    session.execute('SELECT id FROM test')
                    session.execute('SELECT name FROM test')

                    assert len(session._pg_transactions) == 2

                session.execute('SELECT name FROM test')

                assert len(session._pg_transactions) == 1

            session.execute('SELECT * FROM test')
            session.execute('SELECT * FROM test')

            with session.begin():
                session.execute('SELECT id FROM test')
                session.execute('SELECT name FROM test')

                assert len(session._pg_transactions) == 1

            session.execute('SELECT * FROM test')

            assert len(session._pg_transactions) == 0

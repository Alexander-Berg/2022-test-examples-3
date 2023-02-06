# -*- coding: utf-8 -*-
import mock
import itertools

from pymongo.errors import AutoReconnect

from test.helpers.stubs import base
from mpfs.config import settings
import mpfs.engine.process


class MpfsDbHelper(base.BaseStub):
    """
    Помощник при работе с БД в тестах

    Все запросы от одного теста идут в одну БД для каждого коннекта
    Ключевой прием - патчинг `dbnaming.dbname`
    Поэтому при выборе БД через connection всегда используйте этот метод для правильного именования БД:
        conn = dbctl().connection('common')
        correct_for_tests_db = conn[dbnaming.dbname('my_db')]
    """
    DROP_DB_ATTEMPS = 2

    def __init__(self, test_db_name):
        self._test_db_name = test_db_name
        self.dbctl = mpfs.engine.process.dbctl()
        self._patch = None
        self.connections_names = settings.mongo['connections'].keys()

    def start(self):
        self._patch = mock.patch('mpfs.common.util.dbnaming.dbname', return_value=self._test_db_name)
        self._patch.start()

        self.dbctl._db.pop('queue', None)
        self.drop_test_db()

    def stop(self):
        self.drop_test_db()
        self._patch.stop()

    def _drop_test_db_with_autoreconnect(self, conn):
        attemp = 1
        while True:
            try:
                conn.drop_database(self._test_db_name)
                return
            except AutoReconnect:
                if attemp >= self.DROP_DB_ATTEMPS:
                    raise
            attemp += 1

    def drop_test_db(self):
        pool = self.dbctl.mapper.rspool
        sharded_connections = (pool.get_connection_for_rs_name(n) for n in pool.get_all_shards_names())
        config_connections = (self.dbctl.connection(c) for c in self.connections_names)

        for conn in itertools.chain(sharded_connections, config_connections):
            try:
                self._drop_test_db_with_autoreconnect(conn)
            except Exception:
                pass

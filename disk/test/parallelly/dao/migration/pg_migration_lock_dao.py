# -*- coding: utf-8 -*-
import datetime

from mpfs.core.user.dao.migration import PgMigrationLockDao
from mpfs.dao.session import Session
from mpfs.dao.shard_endpoint import ShardEndpoint
from mpfs.metastorage.postgres.query_executer import PGQueryExecuter
from test.base import DiskTestCase
from test.base import time_machine
from test.parallelly.dao.base import PostgresUnits


class PgMigrationLockDAOTestCase(DiskTestCase):
    def setup_method(self, method):
        super(PgMigrationLockDAOTestCase, self).setup_method(method)
        self.lock_dao = PgMigrationLockDao()
        self.postgres_shard_1 = ShardEndpoint.parse('postgres:%s' % PostgresUnits.UNIT1)
        self.owner_mark = 'TEST_%s' % method.__name__
        PGQueryExecuter().reset_cache()
        Session.clear_cache()

    def set_lock(self, method='acquire', until=None):
        if method == 'acquire':
            f = self.lock_dao.acquire
        elif method == 'set_until':
            f = self.lock_dao.set_until
        else:
            raise NotImplemented()
        f(self.uid, until=until, owner_mark=self.owner_mark)

    def test_common(self):
        self.set_lock()
        assert self.lock_dao.is_locked(self.uid)
        self.lock_dao.release(self.uid, owner_mark=self.owner_mark)
        assert not self.lock_dao.is_locked(self.uid)

    def test_second_raises(self):
        self.set_lock()
        assert self.lock_dao.is_locked(self.uid)
        self.assertRaises(RuntimeError, self.lock_dao.acquire, self.uid, owner_mark=self.owner_mark + '_SECOND')
        assert self.lock_dao.is_locked(self.uid)

    def test_until(self):
        self.set_lock(until=datetime.datetime.now() + datetime.timedelta(minutes=10))
        with time_machine(datetime.datetime.now() + datetime.timedelta(minutes=6)):
            assert self.lock_dao.is_locked(self.uid)
        with time_machine(datetime.datetime.now() + datetime.timedelta(minutes=11)):
            assert not self.lock_dao.is_locked(self.uid)

    def test_renew(self):
        self.set_lock()
        with time_machine(datetime.datetime.now() + datetime.timedelta(minutes=6)):
            assert not self.lock_dao.is_locked(self.uid)

        self.set_lock(method='set_until', until=datetime.datetime.now() + datetime.timedelta(minutes=10))
        with time_machine(datetime.datetime.now() + datetime.timedelta(minutes=6)):
            assert self.lock_dao.is_locked(self.uid)

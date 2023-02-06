# -*- coding: utf-8 -*-
import pytest

from mpfs.core.filesystem.dao.trash_cleaner_queue import TrashCleanerQueueMigration
from test.conftest import COMMON_DB_IN_POSTGRES
from test.parallelly.dao.base import BaseMigrationTestCase


pytestmark = pytest.mark.skipif(COMMON_DB_IN_POSTGRES, reason='Tests use migration from mongo to postgres explicitly')


class TrashCleanerQueueMigrationTestCase(BaseMigrationTestCase):
    migrator = TrashCleanerQueueMigration()

    def test_forward_migration(self):
        self.migrator.dao.get_mongo_impl().insert({'_id': 'test', 'uid': self.uid, 'date': 20200625})
        assert self.migrator.run()
        assert self.migrator.dao.get_pg_impl().count() > 0

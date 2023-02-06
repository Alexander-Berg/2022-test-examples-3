# -*- coding: utf-8 -*-

import pytest

from mpfs.core.filesystem.dao.recount import RecountMigration
from test.conftest import COMMON_DB_IN_POSTGRES
from test.parallelly.dao.base import BaseMigrationTestCase

pytestmark = pytest.mark.skipif(COMMON_DB_IN_POSTGRES, reason='Tests use migration from mongo to postgres explicitly')


class RecountMigrationTestCase(BaseMigrationTestCase):
    migrator = RecountMigration()

    def test_forward_migration(self):
        self.migrator.dao.get_mongo_impl().add(self.uid)
        assert self.migrator.run()
        assert self.migrator.dao.get_pg_impl().count() == 1

# -*- coding: utf-8 -*-

import pytest

from mpfs.core.organizations.dao.migrator import OrganizationMigration
from test.conftest import COMMON_DB_IN_POSTGRES
from test.parallelly.dao.base import BaseMigrationTestCase

pytestmark = pytest.mark.skipif(COMMON_DB_IN_POSTGRES, reason='Tests use migration from mongo to postgres explicitly')


class OrganizationMigrationTestCase(BaseMigrationTestCase):
    migrator = OrganizationMigration()

    def test_forward_migration(self):
        item = self.migrator.dao.dao_item_cls.create_from_mongo_dict(
            {"_id": "1", "is_paid": True, "quota_limit": 0, "quota_free": 0, "quota_used_by_disk": 0})
        self.migrator.dao.get_mongo_impl().insert(item.get_mongo_representation())
        assert self.migrator.run()
        assert self.migrator.dao.get_pg_impl().count() == 1

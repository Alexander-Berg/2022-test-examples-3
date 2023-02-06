# -*- coding: utf-8 -*-

import pytest

from mpfs.core.billing.inapp.dao.unprocessed_receipt import UnprocessedReceiptMigration
from test.conftest import COMMON_DB_IN_POSTGRES
from test.parallelly.dao.base import BaseMigrationTestCase

pytestmark = pytest.mark.skipif(COMMON_DB_IN_POSTGRES, reason='Tests use migration from mongo to postgres explicitly')


class UnprocessedReceiptMigrationTestCase(BaseMigrationTestCase):
    migrator = UnprocessedReceiptMigration()

    def test_forward_migration(self):
        item = self.migrator.dao.dao_item_cls.create_from_mongo_dict({
            "_id": "036dcad2c443e6029a5754e85bcc5eab",
             "syncronization_datetime": 1512558285,
             "receipt": "json must be here",
             "traceback": "Exception(test it)",
             "uid": self.uid,
        })
        self.migrator.dao.get_mongo_impl().insert(item.get_mongo_representation())
        assert self.migrator.run()
        assert self.migrator.dao.get_pg_impl().count() == 1

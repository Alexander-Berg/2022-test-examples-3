# -*- coding: utf-8 -*-
import pytest
from bson import ObjectId

from mpfs.core.billing.dao.migrator import BillingLocksMigration, BillingOrdersMigration, BillingOrdersHistoryMigration, \
    BillingServiceAttributesMigration, BillingServiceAttributesHistoryMigration, BillingServicesMigration, \
    BillingServicesHistoryMigration, BillingSubscriptionsMigration
from test.conftest import COMMON_DB_IN_POSTGRES
from test.parallelly.dao.base import BaseMigrationTestCase

pytestmark = pytest.mark.skipif(COMMON_DB_IN_POSTGRES, reason='Tests use migration from mongo to postgres explicitly')


class BillingMigrationTmpl(object):
    migrator = None
    mongo_dict = {}

    def test_forward_migration(self):
        item = self.migrator.dao.dao_item_cls.create_from_mongo_dict(self.mongo_dict)
        self.migrator.dao.get_mongo_impl().insert(item.get_mongo_representation())
        assert self.migrator.dao.get_mongo_impl().count() > 0
        assert self.migrator.run()


class BillingLocksMigrationTestCase(BaseMigrationTestCase, BillingMigrationTmpl):
    migrator = BillingLocksMigration()
    mongo_dict = {'_id': 'test_lock', 'host': 'test', 'lock': 1}


class BillingOrdersMigrationTestCase(BaseMigrationTestCase, BillingMigrationTmpl):
    migrator = BillingOrdersMigration()
    mongo_dict = {
        "_id": "test",
        "uid": BaseMigrationTestCase.uid,
        "payment_method": "bankcard",
        "auto": True,
        "price": 300,
        "pid": "test_product",
        "currency": "RUB",
        "market": "RU",
        "otype": "buy_new",
        "locale": "ru",
        "state": "new",
        "bb_pid": "test_product_subs",
        "v": 1,
    }


class BillingOrdersHistoryMigrationTestCase(BaseMigrationTestCase, BillingMigrationTmpl):
    migrator = BillingOrdersHistoryMigration()
    mongo_dict = {
        "_id": ObjectId(),
        "uid": BaseMigrationTestCase.uid,
        "payment_method": "bankcard",
        "auto": True,
        "price": 300,
        "pid": "test_product",
        "currency": "RUB",
        "market": "RU",
        "otype": "buy_new",
        "locale": "ru",
        "state": "new",
        "bb_pid": "test_product_subs",
        "v": 1,
    }


class BillingServiceAttributesMigrationTestCase(BaseMigrationTestCase, BillingMigrationTmpl):
    migrator = BillingServiceAttributesMigration()
    mongo_dict = {
        "_id": '036dcad2c443e6029a5754e85bcc5eab',
        "amount": 1,
        "uid": BaseMigrationTestCase.uid,
        "v": 1,
    }


class BillingServiceAttributesHistoryMigrationTestCase(BaseMigrationTestCase, BillingMigrationTmpl):
    migrator = BillingServiceAttributesHistoryMigration()
    mongo_dict = {
        "_id": '036dcad2c443e6029a5754e85bcc5eab',
        "amount": 1,
        "uid": BaseMigrationTestCase.uid,
        "v": 1,
    }


class BillingServicesMigrationTestCase(BaseMigrationTestCase, BillingMigrationTmpl):
    migrator = BillingServicesMigration()
    mongo_dict = {
        '_id': '036dcad2c443e6029a5754e85bcc5eab',
        'auto': False,
        'btime': 1512558290,
        'child_sids': None,
        'ctime': 1512558285,
        'enabled': True,
        'group': False,
        'group_name': None,
        'lbtime': 1512558285,
        'mtime': 1512558285,
        'order': '564476235',
        'parent_sid': None,
        'state': None,
        'uid': BaseMigrationTestCase.uid,
        'v': 1512558285034209,
    }


class BillingServicesHistoryMigrationTestCase(BaseMigrationTestCase, BillingMigrationTmpl):
    migrator = BillingServicesHistoryMigration()
    mongo_dict = {
        '_id': ObjectId(),
        'auto': False,
        'btime': 1512558290,
        'child_sids': None,
        'ctime': 1512558285,
        'enabled': True,
        'group': False,
        'group_name': None,
        'lbtime': 1512558285,
        'mtime': 1512558285,
        'order': '564476235',
        'parent_sid': None,
        'state': None,
        'uid': BaseMigrationTestCase.uid,
        'v': 1512558285034209,
    }


class BillingSubscriptionsMigrationTestCase(BaseMigrationTestCase, BillingMigrationTmpl):
    migrator = BillingSubscriptionsMigration()
    mongo_dict = {
        '_id': 'test',
        'ctime': 1512558285,
        'description': 'test subscription',
        'sid': '036dcad2c443e6029a5754e85bcc5eab',
        'uid': BaseMigrationTestCase.uid,
        'v': 1512558285034209,
    }

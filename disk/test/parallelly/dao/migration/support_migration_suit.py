# -*- coding: utf-8 -*-
import datetime

import pytest
from bson import ObjectId, Binary

from mpfs.core.support.dao.migrator import SupportBlockHistoryMigration, SupportBlockedHidsMigration, \
    SupportModerationQueueMigration, SupportMpfsMigration, SupportProhibitedCleaningUsersMigration
from test.conftest import COMMON_DB_IN_POSTGRES
from test.parallelly.dao.base import BaseMigrationTestCase

pytestmark = pytest.mark.skipif(COMMON_DB_IN_POSTGRES, reason='Tests use migration from mongo to postgres explicitly')


class SupportMigrationTmpl(object):
    migrator = None
    mongo_dict = {}

    def test_forward_migration(self):
        item = self.migrator.dao.dao_item_cls.create_from_mongo_dict(self.mongo_dict)
        self.migrator.dao.get_mongo_impl().insert(item.get_mongo_representation())
        assert self.migrator.dao.get_mongo_impl().count() > 0
        assert self.migrator.run()


class SupportBlockHistoryMigrationTestCase(BaseMigrationTestCase, SupportMigrationTmpl):
    migrator = SupportBlockHistoryMigration()
    mongo_dict = {
        '_id': '036dcad2c443e6029a5754e85bcc5eab',
        'ctime': 1512558285,
        'hids': "036dcad2c443e6029a5754e85bcc5eab,036dcad2c443e6029a5754e85bcc5eac",
        'moderator': 'mpfs-test',
        'public_hashes': '',
        'type': 'block',
        'uids': '%s,%s' % (BaseMigrationTestCase.uid, BaseMigrationTestCase.user_1['uid'])
    }


class SupportBlockedHidsMigrationTestCase(BaseMigrationTestCase, SupportMigrationTmpl):
    migrator = SupportBlockedHidsMigration()
    mongo_dict = {
        "_id": ObjectId(),
        "block_type": "only_view_delete",
        "ctime": datetime.datetime.utcnow(),
        "hid": Binary('036dcad2c443e6029a5754e85bcc5eab'),
    }


class SupportModerationQueueMigrationTestCase(BaseMigrationTestCase, SupportMigrationTmpl):
    migrator = SupportModerationQueueMigration()
    mongo_dict = {
        "_id": ObjectId(),
        "created": datetime.datetime.utcnow(),
        "description": 'test',
        "hid": Binary('036dcad2c443e6029a5754e85bcc5eab'),
        "links": [{'type': 'dir', 'url': 'https://yadi.sk/d/test_url'}, {'type': 'file', 'url': 'https://yadi.sk/i/test_url'}],
        "moderation_time": 1512558585,
        "moderator": 'mpfs-test',
        "source": "public_views",
        "status": "moderated",
    }


class SupportMpfsMigrationTestCase(BaseMigrationTestCase, SupportMigrationTmpl):
    migrator = SupportMpfsMigration()
    mongo_dict = {
        '_id': ObjectId(),
        'data': {
            'address': '%s:/disk/test.bin' % BaseMigrationTestCase.uid,
            'comment': '12070122050934775',
            'ctime': 1341383746,
            'moderator': 'test',
            'stids': ['1.yadisk:2.3', '4.yadisk:5.6'],
            'uid': BaseMigrationTestCase.uid,
            'id': '036dcad2c443e6029a5754e85bcc5eab',
            'hash': 'Some_hash_base64==',
        },
        'uid': BaseMigrationTestCase.uid,
    }


class SupportProhibitedCleaningUsersMigrationTestCase(BaseMigrationTestCase, SupportMigrationTmpl):
    migrator = SupportProhibitedCleaningUsersMigration()
    mongo_dict = {
        '_id': ObjectId(),
        'comment': 'test user',
        'ctime': 1341383746,
        'moderator': 'mpfstest',
        'uid': BaseMigrationTestCase.user_1['uid'],
    }

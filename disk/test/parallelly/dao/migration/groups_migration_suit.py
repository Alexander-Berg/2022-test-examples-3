# -*- coding: utf-8 -*-
import time
import uuid

import pytest
from mpfs.core.social.dao.migrator import GroupsMigration, GroupLinksMigration, GroupInvitesMigration
from test.conftest import COMMON_DB_IN_POSTGRES
from test.parallelly.dao.base import BaseMigrationTestCase

pytestmark = pytest.mark.skipif(COMMON_DB_IN_POSTGRES, reason='Tests use migration from mongo to postgres explicitly')


class GroupsMigrationTmpl(object):
    migrator = None
    mongo_dicts = []

    def test_forward_migration(self):
        for mongo_dict in self.mongo_dicts:
            item = self.migrator.dao.dao_item_cls.create_from_mongo_dict(mongo_dict)
            self.migrator.dao.get_mongo_impl().insert(item.get_mongo_representation())
        assert self.migrator.dao.get_mongo_impl().count() == len(self.mongo_dicts)
        assert self.migrator.run()


class GroupsMigrationTestCase(BaseMigrationTestCase, GroupsMigrationTmpl):
    migrator = GroupsMigration()
    mongo_dicts = [
        {
            '_id': uuid.uuid4().hex,
            'owner': BaseMigrationTestCase.uid,
            'path': '/disk/test_group',
            'size': 42,
            'v': 1,
        },
        {
            '_id': uuid.uuid4().hex,
            'owner': BaseMigrationTestCase.uid,
            'size': 0,
            'v': 2,
        },
        {
            '_id': uuid.uuid4().hex,
            'owner': BaseMigrationTestCase.uid,
            'size': -100500,
        },
    ]


class GroupLinksMigrationTestCase(BaseMigrationTestCase, GroupsMigrationTmpl):
    migrator = GroupLinksMigration()
    mongo_dicts = [
        {
            '_id': uuid.uuid4().hex,
            'gid': uuid.uuid4().hex,
            'uid': BaseMigrationTestCase.uid,
            'path': '/disk/test_group',
            'v': 1,
            'rights': 0600,
            'b2b_key': 'disk',
            'universe_login': BaseMigrationTestCase.email,
            'universe_service': 'email',
            'ctime': time.time(),
        },
        {
            '_id': uuid.uuid4().hex,
            'gid': uuid.uuid4().hex,
            'uid': BaseMigrationTestCase.uid,
            'path': '/disk/test_group2',
            'v': 1,
            'rights': 0777,
            'ctime': time.time(),
        },
        {
            '_id': uuid.uuid4().hex,
            'gid': uuid.uuid4().hex,
            'rights': 0777,
            'ctime': time.time(),
        },
    ]


class GroupInvitesMigrationTestCase(BaseMigrationTestCase, GroupsMigrationTmpl):
    migrator = GroupInvitesMigration()
    mongo_dicts = [
        {
            '_id': uuid.uuid4().hex,
            'gid': uuid.uuid4().hex,
            'uid': BaseMigrationTestCase.uid,
            'name': '/disk/test_group',
            'v': 1,
            'rights': 0600,
            'universe_login': BaseMigrationTestCase.email,
            'universe_service': 'email',
            'avatar': 'https://ya.ru/logo.png',
            'status': 'activated',
            'ctime': time.time(),
        },
        {
            '_id': uuid.uuid4().hex,
            'gid': uuid.uuid4().hex,
            'uid': BaseMigrationTestCase.uid,
            'name': '/disk/test_group2',
            'rights': 0777,
            'status': 'rejected',
            'ctime': time.time(),
        },
        {
            '_id': uuid.uuid4().hex,
            'gid': uuid.uuid4().hex,
            'v': 1,
            'rights': 0777,
            'ctime': time.time(),
        },
    ]

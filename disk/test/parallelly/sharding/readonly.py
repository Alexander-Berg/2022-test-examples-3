# -*- coding: utf-8 -*-
import pytest

from base import CommonShardingMethods

import mpfs.engine.process

from mpfs.config import settings
from test.conftest import REAL_MONGO

dbctl = mpfs.engine.process.dbctl()


@pytest.mark.skipif(not REAL_MONGO,
                    reason='https://st.yandex-team.ru/CHEMODAN-34246')
class ReadonlyTestCase(CommonShardingMethods):
    def setup_method(self, method):
        super(ReadonlyTestCase, self).setup_method(method)
        settings.mongo['options']['new_registration'] = True
        self.original_is_shard_writeable = dbctl.mapper.rspool.is_shard_writeable

        import mpfs.core.metastorage.decorators
        mpfs.core.metastorage.decorators.ALLOW_SHARD_CHECK_SYNC = True

        import mpfs.core.operations.base
        mpfs.core.operations.base.ALLOW_SHARD_CHECK_SYNC = True
        mpfs.core.operations.base.ALLOW_SHARD_CHECK_ASYNC = True

    def teardown_method(self, method):
        dbctl.mapper.rspool.is_shard_writeable = self.original_is_shard_writeable
        super(ReadonlyTestCase, self).teardown_method(method)

    def test_user_create_with_shard(self):
        """
        Проверка заведения юзера с указанным шардом
        """
        self.json_ok('user_init', {'uid': self.uid, 'shard': self.mongodb_unit2})
        user_info = self.json_ok('user_info', {'uid': self.uid})
        assert user_info['db']['shard'] == self.mongodb_unit2

    def test_user_create_with_bad_shard(self):
        """
        Проверка заведения юзера на неправильный шард
        """
        self.json_error('user_init', {'uid': self.uid, 'shard': 'mongos'}, code=162)
        self.json_error('user_init', {'uid': self.uid, 'shard': 'daniil migalin'}, code=162)

    def test_user_create_with_all_dead_shards(self):
        """
        Проверяем ситуацию, когда все шарды умерли
        """
        # ломаем проверялку шардов
        dbctl.mapper.rspool.is_shard_writeable = lambda x: False

        # пытаемся завести пользователя, получаем ошибки
        self.json_error('user_init', {'uid': self.uid}, code=162)

    def test_user_create_with_one_dead_shard(self):
        """
        Проверяем ситуацию, когда один шард умер
        """
        # ломаем проверялку шардов, говорим что disk_test_mongodb-unit1 - плохой
        dbctl.mapper.rspool.is_shard_writeable = lambda x: False if x == self.mongodb_unit1 else True

        # пытаемся завести пользователя на disk_test_mongodb-unit1, получаем ошибки
        self.json_error('user_init', {'uid': self.uid, 'shard': self.mongodb_unit1}, code=162)

        # пытаемся завести пользователя на disk_test_mongodb-unit2 - заводится
        self.json_ok('user_init', {'uid': self.uid, 'shard': self.mongodb_unit2})

    @pytest.mark.skipif(True, reason='https://st.yandex-team.ru/CHEMODAN-32396')
    def test_readonly_sync_check(self):
        """
        Проверяем как работает проверялка шарда на синхронные запросы
        """
        self.json_ok('user_init', {'uid': self.uid, 'shard': self.mongodb_unit1})
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/folder'})

        # ломаем проверялку шардов, говорим что disk_test_mongodb-unit1 - плохой
        dbctl.mapper.rspool.is_shard_writeable = lambda x: False if x == self.mongodb_unit1 else True

        # пытаемся сделать синхронные запросы, получаем ошибки
        self.json_error('mkdir', {'uid': self.uid, 'path': '/disk/folder/subfolder'}, code=150)
        self.json_error('copy', {'uid': self.uid, 'src': '/disk/folder', 'dst': '/disk/copy'}, code=150)
        self.json_error('move', {'uid': self.uid, 'src': '/disk/folder', 'dst': '/disk/moved'}, code=150)

        # пытаемся поставить задачу в очередь, получаем ошибки
        self.json_error('async_copy', {'uid': self.uid, 'src': '/disk/folder', 'dst': '/disk/copy'}, code=150)
        self.json_error('async_move', {'uid': self.uid, 'src': '/disk/folder', 'dst': '/disk/moved'}, code=150)

        # отключаем синхронную проверку
        import mpfs.core.metastorage.decorators
        mpfs.core.metastorage.decorators.ALLOW_SHARD_CHECK_SYNC = False

        # синхронные запросы пошли
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/folder/subfolder'})
        self.json_ok('copy', {'uid': self.uid, 'src': '/disk/folder', 'dst': '/disk/copy'})

        # при этом асинхронщина фейлится
        operation = self.json_ok('async_move', {'uid': self.uid, 'src': '/disk/copy', 'dst': '/disk/moved'})
        status = self.json_ok('status', {'uid': self.uid, 'oid': operation['oid']})
        assert status['status'] == 'FAILED'

        # отключаем асинхронную проверку
        import mpfs.core.operations.base
        mpfs.core.operations.base.ALLOW_SHARD_CHECK_ASYNC = False

        # асинхронные пошли
        operation = self.json_ok('async_move', {'uid': self.uid, 'src': '/disk/copy', 'dst': '/disk/moved'})
        status = self.json_ok('status', {'uid': self.uid, 'oid': operation['oid']})
        assert status['status'] == 'DONE'

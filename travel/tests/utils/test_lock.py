# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import pytest
from freezegun import freeze_time
from mongolock import MongoLockLocked, MongoLock

from common.utils.lock import lock


@pytest.mark.mongouser
class TestLock(object):
    def test_context_manager(self):
        with lock('lock_name', timeout=10, expire=20, owner='my42') as lock_obj:
            assert isinstance(lock_obj.lock, MongoLock)
            assert lock_obj.owner == 'my42'
            assert lock_obj.timeout == 10
            assert lock_obj.expire == 20

            # второй раз не можем взять лок
            with pytest.raises(MongoLockLocked):
                with lock('lock_name'):
                    pass

            # можем взять такой же лок в другой коллекции
            with lock('lock_name', collection='mycoll'):
                pass

        # вышли из первого лока, можем взять заново
        with lock('lock_name'):
            pass

    def test_decorator(self):
        @lock('lock_name')
        def foo():

            # второй раз не можем взять лок
            with pytest.raises(MongoLockLocked):
                with lock('lock_name'):
                    pass

        foo()

        # вышли из первого лока, можем взять заново
        with lock('lock_name'):
            pass

    def test_lock_renew(self):
        with freeze_time() as frozen_datetime:
            with lock('lock_name', expire=1, renew_interval=0.01, notify_about_renewals=True) as lock_instance:
                mongolock = lock_instance.lock
                expire = mongolock.get_lock_info('lock_name')['expire']

                # лок должен постоянно обновляться, пока взят. Проверяем это
                frozen_datetime.tick()
                with lock_instance.lock_update_condition:
                    lock_instance.lock_update_condition.wait()

                expire_after_sleep = mongolock.get_lock_info('lock_name')['expire']
                assert expire_after_sleep > expire

            assert not lock_instance.lock_updater.is_alive()

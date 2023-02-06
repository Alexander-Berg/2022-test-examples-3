from datetime import timedelta
from hamcrest import (
    assert_that,
    has_properties,
)
from .misc import get_user_lock, get_last_change_log


def test_release_old_without_existing_locks(context):
    with context.reflect_db() as db:
        res = db.util.release_old_locks()
        assert res == 0, 'No locks should be released'


def test_release_old_should_delete_only_expired_locks(context):
    with context.reflect_db() as db:
        db.code.acquire_lock(1001, timedelta(minutes=10), 'launch', 'host')
        db.code.acquire_lock(1002, timedelta(seconds=0), 'launch', 'host')
        db.code.acquire_lock(1003, timedelta(minutes=10), 'launch', 'host')
        db.code.acquire_lock(1004, timedelta(seconds=0), 'launch', 'host')

        context.mopsdb.execute('DELETE FROM operations.change_log')

        res = db.util.release_old_locks()
        assert res == 2, '2 Locks should be released'

        get_user_lock(db, 1001, should_exist=True)
        get_user_lock(db, 1002, should_exist=False)
        get_user_lock(db, 1003, should_exist=True)
        get_user_lock(db, 1004, should_exist=False)

        get_last_change_log(db, 1001, expected_total=0)
        assert_that(get_last_change_log(db, 1002, expected_total=1), has_properties(type='lock-delete'))
        get_last_change_log(db, 1003, expected_total=0)
        assert_that(get_last_change_log(db, 1004, expected_total=1), has_properties(type='lock-delete'))

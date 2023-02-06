from datetime import timedelta
from hamcrest import (
    assert_that,
    has_properties,
)
from .misc import get_user_lock, get_last_change_log

USER_ID = 1000


def test_release_non_existing_lock(context):
    with context.reflect_db() as db:
        res = db.code.release_lock(USER_ID, 'launch')
        assert res, 'Lock should be released'

        get_user_lock(db, USER_ID, should_exist=False)
        get_last_change_log(db, USER_ID, expected_total=0)


def test_release_lock_with_same_launch_id(context):
    with context.reflect_db() as db:
        res = db.code.acquire_lock(USER_ID, timedelta(seconds=0), 'launch', 'host')
        assert res, 'Lock should be acquired'

        res = db.code.release_lock(USER_ID, 'launch')
        assert res, 'Lock should be released'

        get_user_lock(db, USER_ID, should_exist=False)

        change_log = get_last_change_log(db, USER_ID, expected_total=2)
        assert_that(change_log, has_properties(
            uid=USER_ID,
            type='lock-release',
            hostname='host',
        ))


def test_release_lock_with_other_launch_id(context):
    with context.reflect_db() as db:
        res = db.code.acquire_lock(USER_ID, timedelta(seconds=0), 'launch', 'host')
        assert res, 'Lock should be acquired'

        res = db.code.release_lock(USER_ID, 'other')
        assert not res, 'Lock should not be released'

        get_user_lock(db, USER_ID, should_exist=True)

        change_log = get_last_change_log(db, USER_ID, expected_total=1)
        assert_that(change_log, has_properties(
            uid=USER_ID,
            type='lock-acquire',
            hostname='host',
        ))

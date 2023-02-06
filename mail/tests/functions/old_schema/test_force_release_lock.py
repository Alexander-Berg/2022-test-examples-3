from datetime import timedelta
from hamcrest import (
    assert_that,
    has_properties,
)
from .misc import get_user_lock, get_last_change_log

USER_ID = 1000


def test_force_release_non_existing_lock(context):
    with context.reflect_db() as db:
        res = db.util.force_release_lock(USER_ID)
        assert not res, 'Should return false if no locks were actually released'

        get_user_lock(db, USER_ID, should_exist=False)
        get_last_change_log(db, USER_ID, expected_total=0)


def test_force_release_existing_lock(context):
    with context.reflect_db() as db:
        res = db.code.acquire_lock(USER_ID, timedelta(minutes=10), 'launch', 'host')
        assert res, 'Lock should be acquired'

        res = db.util.force_release_lock(USER_ID)
        assert res, 'Lock should be released'

        get_user_lock(db, USER_ID, should_exist=False)

        change_log = get_last_change_log(db, USER_ID, expected_total=2)
        assert_that(change_log, has_properties(
            uid=USER_ID,
            type='lock-delete',
            hostname='host',
        ))

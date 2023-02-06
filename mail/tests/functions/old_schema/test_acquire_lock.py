from datetime import timedelta
from hamcrest import (
    assert_that,
    equal_to,
    less_than,
    has_properties,
)
from .misc import get_user_lock, get_last_change_log

USER_ID = 1000


def test_acquire_non_existing_lock(context):
    with context.reflect_db() as db:
        res = db.code.acquire_lock(USER_ID, timedelta(minutes=1), 'launch', 'host')
        assert res, 'Lock should be acquired'

        user_lock = get_user_lock(db, USER_ID)
        assert_that(user_lock, has_properties(
            uid=USER_ID,
            ttl=timedelta(minutes=1),
            launch_id='launch',
            hostname='host',
        ))
        assert_that(user_lock.assigned, equal_to(user_lock.heartbeated))

        change_log = get_last_change_log(db, USER_ID, expected_total=1)
        assert_that(change_log, has_properties(
            change_date=user_lock.heartbeated,
            uid=USER_ID,
            type='lock-acquire',
            hostname='host',
        ))


def test_acquire_expired_lock(context):
    with context.reflect_db() as db:
        res = db.code.acquire_lock(USER_ID, timedelta(seconds=0), 'launch1', 'host1')
        assert res, 'First lock should be acquired'

        res = db.code.acquire_lock(USER_ID, timedelta(minutes=1), 'launch2', 'host2')
        assert res, 'Second lock should be acquired'

        user_lock = get_user_lock(db, USER_ID)
        assert_that(user_lock, has_properties(
            uid=USER_ID,
            ttl=timedelta(minutes=1),
            launch_id='launch2',
            hostname='host2',
        ))
        assert_that(user_lock.assigned, equal_to(user_lock.heartbeated))

        change_log = get_last_change_log(db, USER_ID, expected_total=2)
        assert_that(change_log, has_properties(
            change_date=user_lock.heartbeated,
            uid=USER_ID,
            type='lock-acquire',
            hostname='host2',
        ))


def test_acquire_non_expired_lock(context):
    with context.reflect_db() as db:
        res = db.code.acquire_lock(USER_ID, timedelta(minutes=10), 'launch1', 'host1')
        assert res, 'First lock should be acquired'

        res = db.code.acquire_lock(USER_ID, timedelta(minutes=1), 'launch2', 'host2')
        assert not res, 'Second lock should not be acquired'

        user_lock = get_user_lock(db, USER_ID)
        assert_that(user_lock, has_properties(
            uid=USER_ID,
            ttl=timedelta(minutes=10),
            launch_id='launch1',
            hostname='host1',
        ))
        assert_that(user_lock.assigned, equal_to(user_lock.heartbeated))

        get_last_change_log(db, USER_ID, expected_total=1)


def test_acquire_lock_with_same_launch_id(context):
    with context.reflect_db() as db:
        res = db.code.acquire_lock(USER_ID, timedelta(minutes=10), 'launch', 'host')
        assert res, 'First lock should be acquired'

        res = db.code.acquire_lock(USER_ID, timedelta(minutes=1), 'launch', 'host')
        assert res, 'Second lock should be acquired'

        user_lock = get_user_lock(db, USER_ID)
        assert_that(user_lock, has_properties(
            uid=USER_ID,
            ttl=timedelta(minutes=1),
            launch_id='launch',
            hostname='host',
        ))
        assert_that(user_lock.assigned, less_than(user_lock.heartbeated))

        change_log = get_last_change_log(db, USER_ID, expected_total=2)
        assert_that(change_log, has_properties(
            change_date=user_lock.heartbeated,
            uid=USER_ID,
            type='lock-acquire',
            hostname='host',
        ))

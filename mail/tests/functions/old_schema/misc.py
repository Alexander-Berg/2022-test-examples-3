from hamcrest import (
    assert_that,
    has_length,
    empty,
)


def get_user_lock(db, uid, should_exist=True):
    user_locks = db.operations.user_locks.select(uid=uid)
    if should_exist:
        assert_that(user_locks, has_length(1))
        return user_locks[0]
    else:
        assert_that(user_locks, empty())
        return None


def get_last_change_log(db, uid, expected_total=1):
    change_log = db.operations.change_log.select('ORDER BY cid DESC', uid=uid)
    if expected_total > 0:
        assert_that(change_log, has_length(expected_total))
        return change_log[0]
    else:
        assert_that(change_log, empty())
        return None

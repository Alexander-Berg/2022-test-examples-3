from datetime import timedelta
from hamcrest import (
    assert_that,
    equal_to,
)
from mail.pypg.hamcrest_support.concurrency import (
    sequence_caller,
    with_rollback,
    concurrent_call,
    is_serialized,
)

USER_ID = 1000
CALLS_DELAY = 0.1


def test_concurrent_acquire_non_existing_lock(context):
    call = concurrent_call(sequence_caller([
        lambda d: d.code.acquire_lock(USER_ID, timedelta(minutes=1), 'launch1', 'host1'),
        lambda d: d.code.acquire_lock(USER_ID, timedelta(minutes=1), 'launch2', 'host2'),
    ])).on(context.dsn()).with_delay(CALLS_DELAY)

    assert_that(call, is_serialized(), "concurrent function calls should be serialized")
    assert_that(list(call.result().value), equal_to([True, False]))


def test_concurrent_acquire_non_existing_lock_with_rollback(context):
    call = concurrent_call(sequence_caller([
        with_rollback(lambda d: d.code.acquire_lock(USER_ID, timedelta(minutes=1), 'launch1', 'host1')),
        lambda d: d.code.acquire_lock(USER_ID, timedelta(minutes=1), 'launch2', 'host2'),
    ])).on(context.dsn()).with_delay(CALLS_DELAY)

    assert_that(call, is_serialized(), "concurrent function calls should be serialized")
    assert_that(list(call.result().value)[1], equal_to(True))


def test_concurrent_acquire_expired_lock(context):
    with context.reflect_db() as db:
        db.code.acquire_lock(USER_ID, timedelta(seconds=0), 'launch', 'host')

    call = concurrent_call(sequence_caller([
        lambda d: d.code.acquire_lock(USER_ID, timedelta(minutes=1), 'launch1', 'host1'),
        lambda d: d.code.acquire_lock(USER_ID, timedelta(minutes=1), 'launch2', 'host2'),
    ])).on(context.dsn()).with_delay(CALLS_DELAY)

    assert_that(call, is_serialized(), "concurrent function calls should be serialized")
    assert_that(list(call.result().value), equal_to([True, False]))


def test_concurrent_acquire_expired_lock_with_rollback(context):
    with context.reflect_db() as db:
        db.code.acquire_lock(USER_ID, timedelta(seconds=0), 'launch', 'host')

    call = concurrent_call(sequence_caller([
        with_rollback(lambda d: d.code.acquire_lock(USER_ID, timedelta(minutes=1), 'launch1', 'host1')),
        lambda d: d.code.acquire_lock(USER_ID, timedelta(minutes=1), 'launch2', 'host2'),
    ])).on(context.dsn()).with_delay(CALLS_DELAY)

    assert_that(call, is_serialized(), "concurrent function calls should be serialized")
    assert_that(list(call.result().value)[1], equal_to(True))


def test_concurrent_acquire_and_release_non_expired_lock(context):
    with context.reflect_db() as db:
        db.code.acquire_lock(USER_ID, timedelta(minutes=10), 'launch', 'host')

    call = concurrent_call(sequence_caller([
        lambda d: d.code.acquire_lock(USER_ID, timedelta(minutes=1), 'launch1', 'host1'),
        lambda d: d.code.release_lock(USER_ID, 'launch'),
    ])).on(context.dsn()).with_delay(CALLS_DELAY)

    assert_that(call, is_serialized(), "concurrent function calls should be serialized")
    assert_that(list(call.result().value), equal_to([False, True]))


def test_concurrent_release_and_acquire_non_expired_lock(context):
    with context.reflect_db() as db:
        db.code.acquire_lock(USER_ID, timedelta(minutes=10), 'launch', 'host')

    call = concurrent_call(sequence_caller([
        lambda d: d.code.release_lock(USER_ID, 'launch'),
        lambda d: d.code.acquire_lock(USER_ID, timedelta(minutes=1), 'launch1', 'host1'),
    ])).on(context.dsn()).with_delay(CALLS_DELAY)

    assert_that(call, is_serialized(), "concurrent function calls should be serialized")
    assert_that(list(call.result().value), equal_to([True, True]))


def test_concurrent_acquire_and_release_expired_lock(context):
    with context.reflect_db() as db:
        db.code.acquire_lock(USER_ID, timedelta(seconds=0), 'launch', 'host')

    call = concurrent_call(sequence_caller([
        lambda d: d.code.acquire_lock(USER_ID, timedelta(minutes=1), 'launch1', 'host1'),
        lambda d: d.code.release_lock(USER_ID, 'launch'),
    ])).on(context.dsn()).with_delay(CALLS_DELAY)

    assert_that(call, is_serialized(), "concurrent function calls should be serialized")
    assert_that(list(call.result().value), equal_to([True, False]))


def test_concurrent_release_and_acquire_expired_lock(context):
    with context.reflect_db() as db:
        db.code.acquire_lock(USER_ID, timedelta(seconds=0), 'launch', 'host')

    call = concurrent_call(sequence_caller([
        lambda d: d.code.release_lock(USER_ID, 'launch'),
        lambda d: d.code.acquire_lock(USER_ID, timedelta(minutes=1), 'launch1', 'host1'),
    ])).on(context.dsn()).with_delay(CALLS_DELAY)

    assert_that(call, is_serialized(), "concurrent function calls should be serialized")
    assert_that(list(call.result().value), equal_to([True, True]))

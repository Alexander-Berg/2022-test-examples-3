from datetime import datetime, timedelta
from dateutil.tz.tz import tzlocal
from psycopg2 import InternalError
from hamcrest import (
    assert_that,
    has_properties,
    contains,
    calling,
    raises,
    greater_than_or_equal_to,
)

from .misc import add_test_task, change_task_state


def test_fail_task(context):
    with context.reflect_db(user='barbet') as db:
        task_id = add_test_task(db, uid=3000)
        change_task_state(db, task_id, 'in_progress', 'test_worker')
        db.code.fail_task(
            i_task_id=task_id,
            i_worker='test_worker',
            i_reason='some error',
            i_max_retries=0,
            i_delay=timedelta(minutes=5),
        )

        assert len(db.queue.tasks.select(task_id=task_id)) == 0, 'Task should not remain in queue'
        assert_that(db.queue.processed_tasks.select(task_id=task_id), contains(
            has_properties(
                state='error',
                tries=0,
                try_notices=['some error'],
            )
        ), "should contain this task in processed_tasks in 'error' state")


def test_fail_task_with_retries(context):
    with context.reflect_db(user='barbet') as db:
        task_id = add_test_task(db, uid=3001)
        change_task_state(db, task_id, 'in_progress', 'test_worker')
        current_date = datetime.now(tzlocal())
        db.code.fail_task(
            i_task_id=task_id,
            i_worker='test_worker',
            i_reason='some error',
            i_max_retries=1,
            i_delay=timedelta(minutes=5),
        )

        assert_that(db.queue.tasks.select(task_id=task_id), contains(
            has_properties(
                state='pending',
                tries=1,
                try_notices=['some error'],
                processing_date=greater_than_or_equal_to(current_date + timedelta(minutes=5)),
            )
        ), "should contain this task in 'pending' state")


def test_fail_absent_task(context):
    absent_task_id = 1111111111111
    with context.reflect_db(user='barbet') as db:
        assert_that(
            calling(db.code.fail_task).with_args(absent_task_id, 'test_worker', 'some error', 0, timedelta(minutes=5)),
            raises(InternalError, 'can not update task'),
            "should throw exception on absent task"
        )


def test_fail_pending_task(context):
    with context.reflect_db(user='barbet') as db:
        task_id = add_test_task(db, uid=3002)
        change_task_state(db, task_id, 'pending', 'test_worker')

        assert_that(
            calling(db.code.fail_task).with_args(task_id, 'test_worker', 'some error', 0, timedelta(minutes=5)),
            raises(InternalError, 'can not update task'),
            "should throw exception for pending task"
        )


def test_fail_task_by_different_worker(context):
    with context.reflect_db(user='barbet') as db:
        task_id = add_test_task(db, uid=3003)
        change_task_state(db, task_id, 'in_progress', 'test_worker1')

        assert_that(
            calling(db.code.fail_task).with_args(task_id, 'test_worker2', 'some error', 0, timedelta(minutes=5)),
            raises(InternalError, 'can not update task'),
            "should throw exception for different worker"
        )


def test_fail_task_by_different_service(context):
    with context.reflect_db(user='barbet') as db:
        task_id = add_test_task(db, uid=3003)
        change_task_state(db, task_id, 'in_progress', 'test_worker')

    with context.reflect_db(user='tech') as db:
        assert_that(
            calling(db.code.fail_task).with_args(task_id, 'test_worker', 'some error', 0, timedelta(minutes=5)),
            raises(InternalError, 'can not update task'),
            "should throw exception for different service"
        )

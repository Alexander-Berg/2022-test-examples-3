from datetime import datetime, timedelta
from dateutil.tz.tz import tzlocal
from psycopg2 import InternalError
from hamcrest import (
    assert_that,
    greater_than_or_equal_to,
    has_properties,
    contains,
    calling,
    raises,
)

from .misc import add_test_task, change_task_state


def test_delay_task(context):
    with context.reflect_db(user='barbet') as db:
        task_id = add_test_task(db, uid=4000)
        current_date = datetime.now(tzlocal())
        change_task_state(db, task_id, 'in_progress', 'test_worker', current_date - timedelta(minutes=5))
        db.code.delay_task(
            i_task_id=task_id,
            i_worker='test_worker',
            i_delay=timedelta(minutes=5)
        )

        assert_that(db.queue.tasks.select(task_id=task_id), contains(
            has_properties(
                task_id=task_id,
                state='pending',
                processing_date=greater_than_or_equal_to(current_date + timedelta(minutes=5)))
        ), "should contain this task with delayed 'processing_date'")


def test_delay_absent_task(context):
    absent_task_id = 1111111111111
    with context.reflect_db(user='barbet') as db:
        assert_that(
            calling(db.code.delay_task).with_args(absent_task_id, 'test_worker', timedelta(minutes=5)),
            raises(InternalError, 'can not update task'),
            "should throw exception on absent task"
        )


def test_delay_pending_task(context):
    with context.reflect_db(user='barbet') as db:
        task_id = add_test_task(db, uid=4001)
        change_task_state(db, task_id, 'pending', 'test_worker')

        assert_that(
            calling(db.code.delay_task).with_args(task_id, 'test_worker', timedelta(minutes=5)),
            raises(InternalError, 'can not update task'),
            "should throw exception for pending task"
        )


def test_delay_task_by_different_worker(context):
    with context.reflect_db(user='barbet') as db:
        task_id = add_test_task(db, uid=4002)
        change_task_state(db, task_id, 'in_progress', 'test_worker1')

        assert_that(
            calling(db.code.delay_task).with_args(task_id, 'test_worker2', timedelta(minutes=5)),
            raises(InternalError, 'can not update task'),
            "should throw exception for different worker"
        )


def test_delay_task_by_different_service(context):
    with context.reflect_db(user='barbet') as db:
        task_id = add_test_task(db, uid=4003)
        change_task_state(db, task_id, 'in_progress', 'test_worker')

    with context.reflect_db(user='tech') as db:
        assert_that(
            calling(db.code.delay_task).with_args(task_id, 'test_worker', timedelta(minutes=5)),
            raises(InternalError, 'can not update task'),
            "should throw exception for different service"
        )

from psycopg2 import InternalError
from hamcrest import (
    assert_that,
    has_properties,
    contains,
    calling,
    raises,
)

from .misc import add_test_task, change_task_state


def test_complete_task(context):
    with context.reflect_db(user='barbet') as db:
        task_id = add_test_task(db, uid=2000)
        change_task_state(db, task_id, 'in_progress', 'test_worker')
        db.code.complete_task(
            i_task_id=task_id,
            i_worker='test_worker',
        )

        assert len(db.queue.tasks.select(task_id=task_id)) == 0, 'Task should not remain in queue'
        assert_that(db.queue.processed_tasks.select(task_id=task_id), contains(
            has_properties(state='complete')
        ), "should contain this task in processed_tasks in 'complete' state")


def test_complete_absent_task(context):
    absent_task_id = 1111111111111
    with context.reflect_db(user='barbet') as db:
        assert_that(
            calling(db.code.complete_task).with_args(absent_task_id, 'test_worker'),
            raises(InternalError, 'can not update task'),
            "should throw exception on absent task"
        )


def test_complete_pending_task(context):
    with context.reflect_db(user='barbet') as db:
        task_id = add_test_task(db, uid=2001)
        change_task_state(db, task_id, 'pending', 'test_worker')

        assert_that(
            calling(db.code.complete_task).with_args(task_id, 'test_worker'),
            raises(InternalError, 'can not update task'),
            "should throw exception for pending task"
        )


def test_complete_task_by_different_worker(context):
    with context.reflect_db(user='barbet') as db:
        task_id = add_test_task(db, uid=2002)
        change_task_state(db, task_id, 'in_progress', 'test_worker1')

        assert_that(
            calling(db.code.complete_task).with_args(task_id, 'test_worker2'),
            raises(InternalError, 'can not update task'),
            "should throw exception for different worker"
        )


def test_complete_task_by_different_service(context):
    with context.reflect_db(user='barbet') as db:
        task_id = add_test_task(db, uid=2003)
        change_task_state(db, task_id, 'in_progress', 'test_worker')

    with context.reflect_db(user='tech') as db:
        assert_that(
            calling(db.code.complete_task).with_args(task_id, 'test_worker'),
            raises(InternalError, 'can not update task'),
            "should throw exception for different service"
        )

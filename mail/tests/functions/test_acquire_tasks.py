from datetime import timedelta, datetime
from dateutil.tz.tz import tzlocal

from hamcrest import (
    assert_that,
    has_properties,
    contains,
)

from .misc import add_test_task, change_task_state


def test_acquire_tasks(context):
    with context.reflect_db(user='barbet') as db:
        db.queue.tasks.delete()
        add_test_task(db, uid=5000)
        add_test_task(db, uid=5001)
        add_test_task(db, uid=5002)

        returned_tasks = db.code.acquire_tasks(
            i_worker='test_worker',
            i_tasks_limit=2,
        )
        db_tasks = db.queue.tasks.select()

        assert len(returned_tasks) == 2

        assert_that(returned_tasks, contains(
            has_properties(worker='test_worker', state='in_progress', reassignment_count=0),
            has_properties(worker='test_worker', state='in_progress', reassignment_count=0),
        ), "should contain acquired tasks in 'in_progress' state")

        assert_that(db_tasks, contains(
            has_properties(worker='test_worker', state='in_progress', reassignment_count=0),
            has_properties(worker='test_worker', state='in_progress', reassignment_count=0),
            has_properties(state='pending'),
        ), "should contain acquired tasks in 'in_progress' state and other tasks in 'pending' state")


def test_acquire_tasks_by_different_service(context):
    with context.reflect_db(user='barbet') as db:
        db.queue.tasks.delete()
        add_test_task(db, uid=5000)

    with context.reflect_db(user='tech') as db:
        returned_tasks = db.code.acquire_tasks(
            i_worker='test_worker',
            i_tasks_limit=1,
        )

        assert len(returned_tasks) == 0


def test_acquire_delayed_tasks(context):
    with context.reflect_db(user='barbet') as db:
        db.queue.tasks.delete()
        task_id = add_test_task(db, uid=5000)
        processing_date = datetime.now(tzlocal()) + timedelta(minutes=10)
        change_task_state(db, task_id, processing_date=processing_date)

        returned_tasks = db.code.acquire_tasks(
            i_worker='test_worker',
            i_tasks_limit=1,
        )

        assert len(returned_tasks) == 0


def test_acquire_active_in_progress_tasks(context):
    with context.reflect_db(user='barbet') as db:
        db.queue.tasks.delete()
        task_id = add_test_task(db, uid=5000)
        change_task_state(db, task_id, 'in_progress', 'test_worker')

        returned_tasks = db.code.acquire_tasks(
            i_worker='test_worker',
            i_tasks_limit=1,
        )

        assert len(returned_tasks) == 0


def test_acquire_outdated_in_progress_tasks(context):
    with context.reflect_db(user='barbet') as db:
        db.queue.tasks.delete()
        task_id = add_test_task(db, uid=5000, timeout=timedelta(minutes=15))
        processing_date = datetime.now(tzlocal()) - timedelta(minutes=20)
        change_task_state(db, task_id, 'in_progress', 'test_worker', processing_date)

        returned_tasks = db.code.acquire_tasks(
            i_worker='test_worker2',
            i_tasks_limit=1,
        )
        db_tasks = db.queue.tasks.select()

        assert len(returned_tasks) == 1

        assert_that(returned_tasks, contains(
            has_properties(worker='test_worker2', state='in_progress', reassignment_count=1),
        ), "should contain acquired tasks with new worker")

        assert_that(db_tasks, contains(
            has_properties(worker='test_worker2', state='in_progress', reassignment_count=1),
        ), "should contain acquired tasks with new worker")

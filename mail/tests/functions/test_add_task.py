from datetime import timedelta
from hamcrest import (
    assert_that,
    has_properties,
    contains,
)


def test_add_task(context):
    with context.reflect_db(user='barbet') as db:
        task_id = db.code.add_task(
            i_uid=1001,
            i_task='backup_user',
            i_task_args='{"arg1": "val1"}',
            i_timeout=timedelta(minutes=13),
            i_request_id='test_add_task',
        )

        assert_that(db.queue.tasks.select(task_id=task_id), contains(
            has_properties(
                task_id=task_id,
                uid=1001,
                service='barbet',
                task='backup_user',
                task_args={"arg1": "val1"},
                timeout=timedelta(minutes=13),
                request_id='test_add_task',
                state='pending')
        ), "should contain inserted task")


def test_add_several_same_tasks_for_user(context):
    with context.reflect_db(user='barbet') as db:
        task_id1 = db.code.add_task(
            i_uid=1003,
            i_task='backup_user',
            i_task_args=None,
            i_timeout=timedelta(minutes=15),
            i_request_id='test_add_several_same_tasks_for_user',
        )

        assert_that(db.queue.tasks.select(task_id=task_id1), contains(
            has_properties(
                task_id=task_id1,
                uid=1003,
                service='barbet',
                task='backup_user',
                task_args=None,
                timeout=timedelta(minutes=15),
                request_id='test_add_several_same_tasks_for_user',
                state='pending')
        ), "should contain inserted task")

        task_id2 = db.code.add_task(
            i_uid=1003,
            i_task='backup_user',
            i_task_args=None,
            i_timeout=timedelta(minutes=15),
            i_request_id='test_add_several_same_tasks_for_user',
        )

        assert_that(db.queue.tasks.select(task_id=task_id2), contains(
            has_properties(
                task_id=task_id2,
                uid=1003,
                service='barbet',
                task='backup_user',
                task_args=None,
                timeout=timedelta(minutes=15),
                request_id='test_add_several_same_tasks_for_user',
                state='pending')
        ), "should contain inserted task")

from .misc import add_test_task

from hamcrest import assert_that, has_entries, contains_inanyorder
from datetime import datetime, timedelta
from dateutil.tz.tz import tzlocal


def test_acquire_tasks(context):
    current_date = datetime.now(tzlocal())
    task_id1 = add_test_task(
        db=context.queuedb,
        uid=11000,
        state='pending',
        processing_date=current_date - timedelta(minutes=3),
    )
    task_id2 = add_test_task(
        db=context.queuedb,
        uid=11001,
        state='pending',
        processing_date=current_date - timedelta(minutes=2),
    )
    add_test_task(
        db=context.queuedb,
        uid=11002,
        state='pending',
        processing_date=current_date - timedelta(minutes=1),
    )

    api_response = context.queuedb_api.request_get(
        'acquire_tasks',
        worker='test_worker',
        tasks_limit=2,
    )
    assert api_response.status_code == 200
    returned_tasks = api_response.json()
    assert len(returned_tasks) == 2
    assert_that(returned_tasks, contains_inanyorder(
        has_entries(taskId=task_id1, worker='test_worker', state='inProgress'),
        has_entries(taskId=task_id2, worker='test_worker', state='inProgress'),
    ), "should return acquired tasks in 'in_progress' state")

    res = context.queuedb.query(
        '''
            SELECT count(*) FROM queue.tasks
             WHERE state='in_progress'
               AND worker='test_worker'
        ''',
    )
    assert res[0][0] == 2, "should contain acquired tasks in DB in 'in_progress' state"

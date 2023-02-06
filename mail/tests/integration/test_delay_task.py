from datetime import datetime, timedelta
from dateutil.tz.tz import tzlocal
from .misc import add_test_task
from hamcrest import assert_that, has_properties, contains_string


def test_delay_task(context):
    task_id = add_test_task(db=context.queuedb)
    current_date = datetime.now(tzlocal())

    api_response = context.queuedb_api.request_get(
        'delay_task',
        task_id=task_id,
        worker='test_worker',
        delay=300,
    )
    assert api_response.status_code == 200

    res = context.queuedb.query(
        '''
            SELECT count(*) FROM queue.tasks
             WHERE task_id = %(task_id)s
               AND state='pending'
               AND tries=0
               AND processing_date >= %(processing_date)s
        ''',
        task_id=task_id,
        processing_date=current_date + timedelta(minutes=5),
    )
    assert res[0][0] == 1, "should contain this task in tasks in 'pending' state with delayed 'processing_date'"


def test_delay_absent_task(context):
    absent_task_id = 1111111111111
    api_response = context.queuedb_api.request_get(
        'delay_task',
        task_id=absent_task_id,
        worker='test_worker',
        delay=300,
    )
    assert_that(api_response, has_properties(
        status_code=500,
        text=contains_string('can not update task')),
        'should return error for absent task'
    )

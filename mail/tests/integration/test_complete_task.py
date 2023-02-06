from .misc import add_test_task
from hamcrest import assert_that, has_properties, contains_string


def test_complete_task(context):
    task_id = add_test_task(db=context.queuedb)

    api_response = context.queuedb_api.request_get(
        'complete_task',
        task_id=task_id,
        worker='test_worker',
    )
    assert api_response.status_code == 200

    res = context.queuedb.query(
        "SELECT count(*) FROM queue.tasks WHERE task_id = %(task_id)s",
        task_id=task_id,
    )
    assert res[0][0] == 0, "task should not remain in queue"

    res = context.queuedb.query(
        '''
            SELECT count(*) FROM queue.processed_tasks
             WHERE task_id = %(task_id)s
               AND state='complete'
               AND worker='test_worker'
        ''',
        task_id=task_id,
    )
    assert res[0][0] == 1, "should contain this task in processed_tasks in 'complete' state"


def test_complete_absent_task(context):
    absent_task_id = 1111111111111
    api_response = context.queuedb_api.request_get(
        'complete_task',
        task_id=absent_task_id,
        worker='test_worker',
    )
    assert_that(api_response, has_properties(
        status_code=500,
        text=contains_string('can not update task')),
        'should return error for absent task'
    )

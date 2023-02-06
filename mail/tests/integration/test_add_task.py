from hamcrest import assert_that, has_properties, contains_string


def test_add_task(context):
    api_response = context.queuedb_api.request_get(
        'add_task',
        uid=10001,
        task='backup_user',
        task_args='{"arg1": "val1"}',
        timeout=1200,
    )
    assert api_response.status_code == 200
    task_id = api_response.json()['taskId']

    res = context.queuedb.query(
        '''
            SELECT count(*) FROM queue.tasks
             WHERE task_id = %(task_id)s
               AND uid = 10001
               AND state = 'pending'
               AND service = 'barbet'
               AND task = 'backup_user'
               AND task_args = '{"arg1": "val1"}'
               AND timeout = interval '20 minutes'
        ''',
        task_id=task_id,
    )
    assert res[0][0] == 1, "should contain inserted task"


def test_add_task_with_no_task_args(context):
    api_response = context.queuedb_api.request_get(
        'add_task',
        uid=10002,
        task='restore_user',
        timeout=600,
    )
    assert api_response.status_code == 200
    task_id = api_response.json()['taskId']

    res = context.queuedb.query(
        '''
            SELECT count(*) FROM queue.tasks
             WHERE task_id = %(task_id)s
               AND uid = 10002
               AND state = 'pending'
               AND service = 'barbet'
               AND task = 'restore_user'
               AND task_args is NULL
               AND timeout = interval '10 minutes'
        ''',
        task_id=task_id,
    )
    assert res[0][0] == 1, "should contain inserted task"


def test_add_task_with_bad_task_args(context):
    api_response = context.queuedb_api.request_get(
        'add_task',
        uid=10003,
        task='backup_user',
        task_args='{"arg1":}',
        timeout=1200,
    )
    assert_that(api_response, has_properties(
        status_code=500,
        text=contains_string('invalid input syntax for type json')),
        'should return error for bad task_args'
    )

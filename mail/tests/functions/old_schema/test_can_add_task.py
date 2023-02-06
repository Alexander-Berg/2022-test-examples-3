import json

from uuid import uuid4 as rand_uuid

USER_ID = 1000
TASKS_LIMIT = 3


def test_can_add_task_returns_false_for_banned_user(context):
    context.mopsdb.execute('''
        INSERT INTO operations.banned_users (uid)
             VALUES ({uid})
    '''.format(uid=USER_ID))
    with context.reflect_db() as db:
        assert not db.code.can_add_task(USER_ID, TASKS_LIMIT)


def test_can_add_tasks_returns_false_if_recent_tasks_count_greater_than_limit(context):
    with context.reflect_db() as db:
        db.code.add_task(
            i_uid=USER_ID,
            i_task_id=rand_uuid(),
            i_task_info=json.dumps({}),
            i_chunks=json.dumps([{'id': str(rand_uuid()), 'mids': list(range(TASKS_LIMIT))}]),
        )
        assert not db.code.can_add_task(USER_ID, TASKS_LIMIT)


def test_can_add_tasks_returns_true_if_recent_tasks_less_than_limit(context):
    with context.reflect_db() as db:
        db.code.add_task(
            i_uid=USER_ID,
            i_task_id=rand_uuid(),
            i_task_info=json.dumps({}),
            i_chunks=json.dumps([{'id': str(rand_uuid()), 'mids': list(range(TASKS_LIMIT - 1))}]),
        )
        assert db.code.can_add_task(USER_ID, TASKS_LIMIT)


def test_can_add_tasks_removes_old_recent_tasks(context):
    task_id = rand_uuid()
    with context.reflect_db() as db:
        db.code.add_task(
            i_uid=USER_ID,
            i_task_id=task_id,
            i_task_info=json.dumps({}),
            i_chunks=json.dumps([{'id': str(rand_uuid()), 'mids': list(range(TASKS_LIMIT - 1))}]),
        )
        context.mopsdb.execute('''
            UPDATE operations.recent_tasks
               SET created = created - '1 hour'::interval
        ''')

        db.code.can_add_task(USER_ID, TASKS_LIMIT)
        assert len(db.operations.recent_tasks.select(uid=USER_ID, task_id=task_id)) == 0

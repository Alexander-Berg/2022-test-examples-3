import json

from uuid import uuid4 as rand_uuid
from mopsdb_cron import get_cron


def test_remove_old_tasks(context):
    uid = 1000
    full_task_id = rand_uuid()
    with context.reflect_db() as db:
        db.code.add_task(
            i_uid=uid,
            i_task_id=full_task_id,
            i_task_info=json.dumps({'type': 'test', 'param': 'value'}),
            i_chunks=json.dumps([{'id': str(rand_uuid()), 'mids': [11, 12, 13]}]),
        )

        old_task_id = rand_uuid()
        db.code.add_task(
            i_uid=uid,
            i_task_id=old_task_id,
            i_task_info=json.dumps({'type': 'test', 'param': 'value'}),
            i_chunks=json.dumps([{'id': str(rand_uuid()), 'mids': [11, 12, 13]}]),
        )
        context.mopsdb.execute('''
            UPDATE operations.tasks
            SET created = created - '1 month'::interval
            WHERE task_id = '{}'
        '''.format(old_task_id))

        empty_task_id = rand_uuid()
        db.code.add_task(
            i_uid=uid,
            i_task_id=empty_task_id,
            i_task_info=json.dumps({'type': 'test', 'param': 'value'}),
            i_chunks=json.dumps([]),
        )

        cron = get_cron('remove_old_tasks', context.config)
        cron.run()

        assert len(db.operations.tasks.select(uid=uid, task_id=full_task_id)) == 1, 'Task should remain in queue'
        assert len(db.operations.tasks.select(uid=uid, task_id=old_task_id)) == 0, 'Task should not remain in queue'
        assert len(db.operations.tasks.select(uid=uid, task_id=empty_task_id)) == 0, 'Task should not remain in queue'

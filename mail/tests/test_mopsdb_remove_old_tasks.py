import json
import asyncio

from uuid import uuid4 as random_uuid

from .context import reflect_mopsdb

from mail.puli.lib.dbtask.base_task import create_task


def test_mopsdb_remove_old_tasks(context):
    uid = 1000
    full_task_id = random_uuid()
    with reflect_mopsdb(context) as db:
        db.code.add_task(
            i_uid=uid,
            i_task_id=full_task_id,
            i_task_info=json.dumps({'type': 'test', 'param': 'value'}),
            i_chunks=json.dumps([{'id': str(random_uuid()), 'mids': [11, 12, 13]}]),
        )

        old_task_id = random_uuid()
        db.code.add_task(
            i_uid=uid,
            i_task_id=old_task_id,
            i_task_info=json.dumps({'type': 'test', 'param': 'value'}),
            i_chunks=json.dumps([{'id': str(random_uuid()), 'mids': [11, 12, 13]}]),
        )
        context.mopsdb.execute('''
            UPDATE operations.tasks
            SET created = created - '1 month'::interval
            WHERE task_id = '{}'
        '''.format(old_task_id))

        empty_task_id = random_uuid()
        db.code.add_task(
            i_uid=uid,
            i_task_id=empty_task_id,
            i_task_info=json.dumps({'type': 'test', 'param': 'value'}),
            i_chunks=json.dumps([]),
        )

        task = create_task('mopsdb_remove_old_tasks', context.config)

        asyncio.get_event_loop().run_until_complete(task.run('mopsdb'))

        assert len(db.operations.tasks.select(uid=uid, task_id=full_task_id)) == 1, 'Task should remain in queue'
        assert len(db.operations.tasks.select(uid=uid, task_id=old_task_id)) == 0, 'Task should not remain in queue'
        assert len(db.operations.tasks.select(uid=uid, task_id=empty_task_id)) == 0, 'Task should not remain in queue'

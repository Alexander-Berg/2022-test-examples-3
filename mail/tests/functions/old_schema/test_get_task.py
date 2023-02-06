import json

from uuid import uuid4 as rand_uuid


def test_get_task_returns_requested_task(context):
    uid = 1000
    task_id = rand_uuid()
    task_info = {'type': 'test', 'param': 'value'}

    with context.reflect_db() as db:
        db.code.add_task(
            i_uid=uid,
            i_task_id=task_id,
            i_task_info=json.dumps(task_info),
            i_chunks=json.dumps([
                {'id': str(rand_uuid()), 'mids': [1]},
                {'id': str(rand_uuid()), 'mids': [2]},
            ])
        )
        db.code.add_task(
            i_uid=uid,
            i_task_id=rand_uuid(),
            i_task_info=json.dumps({'task': 'other'}),
            i_chunks=json.dumps([{'id': str(rand_uuid()), 'mids': [10]}])
        )

        res_task = db.code.get_task(uid, task_id)
        assert len(res_task) == 1
        assert res_task[0].o_task_info == task_info
        assert res_task[0].o_count == 2

import json

from uuid import uuid4 as rand_uuid, UUID


def test_add_task(context):
    uid = 1000
    task_id = rand_uuid()
    task_info = {'type': 'test', 'param': 'value'}
    chunks = [
        {'id': str(rand_uuid()), 'mids': [11, 12, 13]},
        {'id': str(rand_uuid()), 'mids': [21, 22]},
    ]

    with context.reflect_db() as db:
        db.code.add_task(
            i_uid=uid,
            i_task_id=task_id,
            i_task_info=json.dumps(task_info),
            i_chunks=json.dumps(chunks),
        )

        res_tasks = db.operations.tasks.select(uid=uid, task_id=task_id)
        assert len(res_tasks) == 1
        assert res_tasks[0].task_info == task_info

        res_chunks = db.operations.chunks.select(task_id=task_id)
        for chunk in chunks:
            assert UUID(chunk['id']) in [c.chunk_id for c in res_chunks]
            res_mids = [m.mid for m in db.operations.mids.select(chunk_id=chunk['id'])]
            assert res_mids == chunk['mids']

        res_recent = db.operations.recent_tasks.select(uid=uid, task_id=task_id)
        assert len(res_recent) == 1
        assert res_recent[0].size == 5

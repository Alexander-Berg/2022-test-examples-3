import json

from uuid import uuid4 as rand_uuid


def test_get_user_stat_returns_tasks_for_requested_users(context):
    uid = 1000
    tasks = [
        {'task_id': rand_uuid(), 'task_info': {'type': 'test', 'param': 1}, 'chunks': 2},
        {'task_id': rand_uuid(), 'task_info': {'type': 'test', 'param': 2}, 'chunks': 3},
    ]

    with context.reflect_db() as db:
        for task in tasks:
            chunks = []
            for i in range(task['chunks']):
                chunks.append({'id': str(rand_uuid()), 'mids': [i]})
            db.code.add_task(
                i_uid=uid,
                i_task_id=task['task_id'],
                i_task_info=json.dumps(task['task_info']),
                i_chunks=json.dumps(chunks)
            )
        db.code.add_task(
            i_uid=uid + 1,
            i_task_id=rand_uuid(),
            i_task_info=json.dumps({'task': 'other'}),
            i_chunks=json.dumps([{'id': str(rand_uuid()), 'mids': [10]}])
        )

        res_stat = db.code.user_stat(uid)
        assert len(res_stat) == len(tasks)

        def get_task(stat, task_id):
            for s in stat:
                if s.o_task_id == task_id:
                    return s
            assert False, 'task %s not found in result' % task_id

        for task in tasks:
            res_task = get_task(res_stat, task['task_id'])
            assert res_task.o_task_info == task['task_info']
            assert res_task.o_count == task['chunks']

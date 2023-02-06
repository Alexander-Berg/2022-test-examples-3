import json

from uuid import uuid4 as rand_uuid


def test_has_tasks_returns_false_if_no_tasks(context):
    uid = 1000
    with context.reflect_db() as db:
        assert not db.code.has_tasks(uid)


def test_has_tasks_returns_true_if_tasks(context):
    uid = 1000
    with context.reflect_db() as db:
        db.code.add_task(
            i_uid=uid,
            i_task_id=rand_uuid(),
            i_task_info=json.dumps({}),
            i_chunks=json.dumps([]),
        )
        assert db.code.has_tasks(uid)

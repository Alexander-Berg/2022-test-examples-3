import pytest
import json

from uuid import uuid4 as rand_uuid

LIMIT = 3


@pytest.mark.parametrize("user_count", [LIMIT - 1, LIMIT + 1])
def test_choose_users_returns_users_with_older_tasks_less_than_limit(context, user_count):
    users = list(range(user_count))
    with context.reflect_db() as db:
        for uid in users:
            for i in range(2):
                db.code.add_task(
                    i_uid=uid,
                    i_task_id=rand_uuid(),
                    i_task_info=json.dumps({}),
                    i_chunks=json.dumps([{'id': str(rand_uuid()), 'mids': [i]}]),
                )
        assert db.code.choose_users(LIMIT) == users[:LIMIT]


def test_choose_users_ignores_users_with_empty_tasks(context):
    empty_task_user = 1001
    filled_task_user = 1002

    with context.reflect_db() as db:
        db.code.add_task(empty_task_user, rand_uuid(), json.dumps({}), json.dumps([]))
        db.code.add_task(filled_task_user, rand_uuid(), json.dumps({}), json.dumps([]))
        db.code.add_task(filled_task_user, rand_uuid(), json.dumps({}),
                         json.dumps([{'id': str(rand_uuid()), 'mids': []}]))

        result = db.code.choose_users(LIMIT)
        assert filled_task_user in result
        assert empty_task_user not in result

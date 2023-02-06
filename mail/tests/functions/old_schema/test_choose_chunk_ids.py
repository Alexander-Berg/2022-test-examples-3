import json

from uuid import uuid4 as rand_uuid

USER_ID = 1000
LIMIT = 3


def test_choose_chunk_ids_returns_chunks(context):
    chunk_id = rand_uuid()

    with context.reflect_db() as db:
        db.code.add_task(
            i_uid=USER_ID,
            i_task_id=rand_uuid(),
            i_task_info=json.dumps({}),
            i_chunks=json.dumps([{'id': str(chunk_id), 'mids': []}])
        )
        assert db.code.choose_chunk_ids(USER_ID, LIMIT) == [chunk_id]


def test_choose_chunk_ids_returns_chunks_less_than_limit(context):
    with context.reflect_db() as db:
        db.code.add_task(
            i_uid=USER_ID,
            i_task_id=rand_uuid(),
            i_task_info=json.dumps({}),
            i_chunks=json.dumps([{'id': str(rand_uuid()), 'mids': [i]} for i in range(LIMIT + 1)])
        )
        assert len(db.code.choose_chunk_ids(USER_ID, LIMIT)) == LIMIT

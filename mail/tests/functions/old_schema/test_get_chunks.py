import json

from uuid import uuid4 as rand_uuid


def test_get_chunks_returns_requested_chunks(context):
    uid = 1000
    requested_chunk = rand_uuid()
    other_chunk = rand_uuid()

    with context.reflect_db() as db:
        db.code.add_task(
            i_uid=uid,
            i_task_id=rand_uuid(),
            i_task_info=json.dumps({}),
            i_chunks=json.dumps([
                {'id': str(requested_chunk), 'mids': []},
                {'id': str(other_chunk), 'mids': []},
            ]),
        )

        result = [c.chunk_id for c in db.code.get_chunks(uid, [requested_chunk])]
        assert requested_chunk in result
        assert other_chunk not in result

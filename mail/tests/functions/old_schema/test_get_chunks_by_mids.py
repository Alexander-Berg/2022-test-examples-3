import json

from uuid import uuid4 as rand_uuid, UUID


def test_get_chunks_by_mids_returns_chunks_with_requested_mids(context):
    uid = 1000
    chunks = [
        {'id': str(rand_uuid()), 'mids': [11, 12, 13]},
        {'id': str(rand_uuid()), 'mids': [21]},
        {'id': str(rand_uuid()), 'mids': [31, 32]},
    ]
    chunk_ids = [UUID(c['id']) for c in chunks]

    with context.reflect_db() as db:
        db.code.add_task(
            i_uid=uid,
            i_task_id=rand_uuid(),
            i_task_info=json.dumps({}),
            i_chunks=json.dumps(chunks),
        )

        result = [c.o_chunk_id for c in db.code.get_chunks_by_mids(uid, [21, 12])]
        assert (chunk_ids[0] in result) and (chunk_ids[1] in result)
        assert chunk_ids[2] not in result

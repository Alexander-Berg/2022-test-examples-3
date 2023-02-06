import json

from uuid import uuid4 as rand_uuid, UUID

USER_ID = 1000


def test_remove_chunk_deletes_chunk(context):
    task_id = rand_uuid()
    chunk_deleted = {'id': str(rand_uuid()), 'mids': []}
    chunk_saved = {'id': str(rand_uuid()), 'mids': []}

    with context.reflect_db() as db:
        db.code.add_task(
            i_uid=USER_ID,
            i_task_id=task_id,
            i_task_info=json.dumps({}),
            i_chunks=json.dumps([chunk_deleted, chunk_saved]),
        )
        db.code.remove_chunk(USER_ID, UUID(chunk_deleted['id']), i_hostname='iamhost')

        res_chunks = [c.chunk_id for c in db.operations.chunks.select(task_id=task_id)]
        assert res_chunks == [UUID(chunk_saved['id'])]


def test_remove_chunk_writes_to_changelog(context):
    task_id = rand_uuid()
    chunk_deleted = {'id': str(rand_uuid()), 'mids': []}

    with context.reflect_db() as db:
        db.code.add_task(
            i_uid=USER_ID,
            i_task_id=task_id,
            i_task_info=json.dumps({}),
            i_chunks=json.dumps([chunk_deleted]),
        )
        db.code.remove_chunk(USER_ID, UUID(chunk_deleted['id']), i_hostname='iamhost')

        last_log_line = db.operations.change_log.select('ORDER BY cid DESC', uid=USER_ID)[0]
        assert last_log_line.type == 'delete-chunk'
        assert last_log_line.changed['chunk_id'] == chunk_deleted['id']
        assert last_log_line.hostname == 'iamhost'


def test_remove_chunk_deletes_empty_task(context):
    task_id = rand_uuid()
    chunk_deleted = {'id': str(rand_uuid()), 'mids': []}

    with context.reflect_db() as db:
        db.code.add_task(
            i_uid=USER_ID,
            i_task_id=task_id,
            i_task_info=json.dumps({}),
            i_chunks=json.dumps([chunk_deleted]),
        )
        db.code.remove_chunk(USER_ID, UUID(chunk_deleted['id']), i_hostname='iamhost')

        assert len(db.operations.tasks.select(uid=USER_ID, task_id=task_id)) == 0

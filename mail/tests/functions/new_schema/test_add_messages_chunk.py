from psycopg2 import IntegrityError
from hamcrest import (
    has_properties,
    has_entries,
    assert_that,
    calling,
    raises,
    has_length,
    contains,
)
from .misc import last_change_log_entry, add_test_operation


def test_add_chunk_with_given_mids(context):
    uid = 1000
    with context.reflect_db() as db:
        op = add_test_operation(db, context, uid=uid)

        db.impl.add_messages_chunk(
            i_op_id=op.id, i_id=1, i_mids=[9, 8, 7, 6, 5, 4, 3, 2, 1], i_request_id=context.request_id,
        )

        assert_that(db.mops.message_chunks.select(op_id=op.id, id=1), contains(
            has_properties(id=1, op_id=op.id, mids=[9, 8, 7, 6, 5, 4, 3, 2, 1], state='initial')
        ), "should contain inserted chunk in initial state")

        assert_that(last_change_log_entry(db, uid), has_properties(
            op_id=op.id, type='chunk_create', uid=1000, request_id=context.request_id,
            changed=has_entries(id=1, count=9)
        ), "change log should contain an entry about chunk")


def test_add_two_chunks_with_same_id(context):
    with context.reflect_db() as db:
        op = add_test_operation(db, context)

        db.impl.add_messages_chunk(
            i_op_id=op.id, i_id=1, i_mids=[3, 2, 1], i_request_id=context.request_id,
        )
        assert_that(
            calling(db.impl.add_messages_chunk).with_args(op.id, 1, [3, 2, 1], context.request_id),
            raises(IntegrityError, 'duplicate key value violates unique constraint "pk_chunks"'),
            "should throw exception on key conflict action"
        )

        chunks = db.mops.message_chunks.select(op_id=op.id, id=1)

        assert_that(chunks, has_length(1), "only one chunk should be inserted")

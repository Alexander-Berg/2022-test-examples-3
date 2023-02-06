from uuid import uuid4 as rand_uuid
from .misc import create_test_chunk, last_change_log_entry
from hamcrest import (
    has_properties,
    has_entries,
    assert_that,
    calling,
    raises,
    equal_to,
)
from psycopg2 import InternalError


def test_transit_chunk_state_initial_to_in_progress(context):
    uid = 1000
    op_id = rand_uuid()

    with context.reflect_db() as db:
        create_test_chunk(db, context, uid, op_id)

        res = db.impl.transit_chunk_state(op_id, 1, 'process_chunk', context.request_id)

        assert_that(res, equal_to('in_progress'), "should return new chunk state")

        assert_that(
            db.mops.message_chunks.select(id=1)[0].state, equal_to('in_progress'),
            "chunk state should be changed into a new state"
        )

        assert_that(last_change_log_entry(db, uid), has_properties(
            op_id=op_id, type='chunk_change_state', uid=1000, request_id=context.request_id,
            changed=has_entries(
                action='process_chunk', id=1, from_state='initial', new_state='in_progress'
            )
        ), "change log should contain an entry about chunk state has been changed")


def test_transit_chunk_state_with_unexpected_action(context):
    with context.reflect_db() as db:
        res = create_test_chunk(db, context)

        assert_that(
            calling(db.impl.transit_chunk_state).with_args(res.operation.id, res.chunks[0].id, 'revert_chunk', context.request_id),
            raises(InternalError, "no transition from initial state with revert_chunk action")
        )

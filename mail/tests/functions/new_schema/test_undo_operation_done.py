import pytest
import re

from psycopg2 import InternalError
from hamcrest import (
    assert_that,
    calling,
    raises,
    equal_to,
)
from .misc import add_test_operation, create_test_chunk


def test_undo_operation_done_for_operation_in_revert_with_no_chunks(context):
    with context.reflect_db() as db:
        op = add_test_operation(db, context, state='in_revert')

        res = db.code.undo_operation_done(i_op_id=op.id, i_request_id=context.request_id)

        assert_that(
            res.state, equal_to('reverted'),
            "operation should be in reverted state after function call"
        )


def test_undo_operation_done_for_operation_in_revert_with_chunk_in_initial_state(context):
    with context.reflect_db() as db:
        op_id = create_test_chunk(db, context).operation.id
        db.mops.operations.update({'state': 'in_revert'}, id=op_id)
        res = db.code.undo_operation_done(i_op_id=op_id, i_request_id=context.request_id)

        assert_that(
            res.state, equal_to('reverted'),
            "operation should be in reverted state after function call"
        )


@pytest.mark.parametrize(('state'), ('in_revert', 'in_progress', 'done'))
def test_undo_operation_done_for_operation_in_revert_with_chunk_not_in_initial_state(context, state):
    with context.reflect_db() as db:
        op_id = create_test_chunk(db, context, state=state).operation.id
        db.mops.operations.update({'state': 'in_revert'}, id=op_id)

        assert_that(
            calling(db.code.undo_operation_done).with_args(op_id, context.request_id),
            raises(InternalError, re.escape("found chunks {1} are not in initial state")),
            "should throw on associated chunk is not in 'initial' state"
        )


@pytest.mark.parametrize(('state'), ('fresh', 'in_progress', 'complete', 'end'))
def test_undo_operation_done_for_operation_in_invalid_state(context, state):
    with context.reflect_db() as db:
        op_id = create_test_chunk(db, context).operation.id
        db.mops.operations.update({'state': state}, id=op_id)

        assert_that(
            calling(db.code.undo_operation_done).with_args(op_id, context.request_id),
            raises(InternalError, "no transition"),
            "should throw on unexpected action for the state"
        )

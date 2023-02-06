import pytest

from uuid import uuid4 as rand_uuid
from psycopg2 import InternalError
from hamcrest import (
    assert_that,
    calling,
    raises,
    equal_to,
    contains,
    has_properties,
)
from .misc import add_test_operation


def test_process_operation_after_addition(context):
    with context.reflect_db() as db:
        op = add_test_operation(db, context)

        chunks = [
            db.code.messages_chunk([9, 8, 7, 6, 5, 4, 3, 2, 1], ''),
            db.code.messages_chunk([10, 20, 30, 40, 50], ''),
        ]

        res = db.code.process_operation(
            i_op_id=op.id, i_chunks=chunks, i_request_id=context.request_id,
        )

        assert_that(res.state, equal_to('in_progress'), "should be in progress after function call")

        chunks = db.mops.message_chunks.select('ORDER BY id ASC', op_id=op.id)

        assert_that(chunks, contains(
            has_properties(mids=[9, 8, 7, 6, 5, 4, 3, 2, 1]),
            has_properties(mids=[10, 20, 30, 40, 50])
        ), "task associated chunks should be in the database")


@pytest.mark.parametrize(('state'), ('in_revert', 'in_progress', 'complete', 'reverted', 'end'))
def test_process_operation_for_operation_not_in_fresh_state(context, state):
    with context.reflect_db() as db:
        op = add_test_operation(db, context, state=state)

        assert_that(
            calling(db.code.process_operation).with_args(op.id, None, context.request_id),
            raises(InternalError, "no transition"),
            "should throw exception for unexpected action is the state"
        )


def test_process_operation_for_task_which_does_not_exist(context):
    with context.reflect_db() as db:
        assert_that(
            calling(db.code.process_operation).with_args(rand_uuid(), None, context.request_id),
            raises(InternalError, "no operation found"),
            "should throw exception for the operation does not exist"
        )

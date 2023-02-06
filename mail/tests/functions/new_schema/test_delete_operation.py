import pytest

from psycopg2 import InternalError
from hamcrest import (
    assert_that,
    calling,
    raises,
    equal_to,
)
from .misc import add_test_operation


def test_delete_operation_for_the_recent_operation_in_complete_state(context):
    with context.reflect_db() as db:
        op = add_test_operation(db, context, state='complete')

        res = db.code.delete_operation(i_op_id=op.id, i_request_id=context.request_id)

        assert_that(
            res.state, equal_to('complete'),
            "operation should stay in complete state after the function call"
        )


def test_delete_operation_for_the_recent_operation_and_expired_ttl_in_complete_state(context):
    with context.reflect_db() as db:
        op = add_test_operation(db, context, state='complete')
        db.mops.operations.update({'created': op.created - db.impl.operation_ttl()}, id=op.id)

        res = db.code.delete_operation(i_op_id=op.id, i_request_id=context.request_id)

        assert_that(
            res.state, equal_to('end'),
            "operation should become end after the function call due to the operation TTL has been expired"
        )


def test_delete_operation_for_not_the_recent_operation_in_complete_state(context):
    with context.reflect_db() as db:
        op = add_test_operation(db, context, state='complete')
        add_test_operation(db, context, uid=op.uid)

        res = db.code.delete_operation(i_op_id=op.id, i_request_id=context.request_id)

        assert_that(
            res.state, equal_to('end'),
            "operation should become end after the function call"
        )


def test_delete_operation_in_reverted_state(context):
    with context.reflect_db() as db:
        op = add_test_operation(db, context, state='reverted')

        res = db.code.delete_operation(i_op_id=op.id, i_request_id=context.request_id)

        assert_that(
            res.state, equal_to('end'),
            "operation should become end after the function call"
        )


@pytest.mark.parametrize(('state'), ('fresh', 'in_revert', 'in_progress', 'end'))
def test_delete_operation_for_operation_in_invalid_state(context, state):
    with context.reflect_db() as db:
        op = add_test_operation(db, context, state=state)

        assert_that(
            calling(db.code.delete_operation).with_args(op.id, context.request_id),
            raises(InternalError, "no transition"),
            "should throw on unexpected action for the state"
        )

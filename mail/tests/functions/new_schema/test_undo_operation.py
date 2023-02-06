import pytest

from uuid import uuid4 as rand_uuid
from psycopg2 import InternalError
from hamcrest import (
    assert_that,
    calling,
    raises,
    equal_to,
)
from .misc import add_test_operation


@pytest.mark.parametrize(('state', 'target_state'), (('fresh', 'reverted'), ('in_progress', 'in_revert'), ('complete', 'in_revert')))
def test_undo_operation_for_the_recent_operation_in_valid_state(context, state, target_state):
    with context.reflect_db() as db:
        op = add_test_operation(db, context, state=state)

        res = db.code.undo_operation(i_op_id=op.id, i_request_id=context.request_id)

        assert_that(
            res.state, equal_to(target_state),
            "operation should be in {} state after function call".format(target_state)
        )


@pytest.mark.parametrize(('state'), ('fresh', 'in_progress', 'complete'))
def test_undo_operation_for_the_not_recent_operation_in_valid_state(context, state):
    with context.reflect_db() as db:
        op = add_test_operation(db, context, state=state)
        add_test_operation(db, context, uid=op.uid)

        res = db.code.undo_operation(i_op_id=op.id, i_request_id=context.request_id)

        assert_that(
            res.state, equal_to(state),
            "should stay in same state since undo is impossible for not the recent operation"
        )


@pytest.mark.parametrize(('state'), ('in_revert', 'reverted', 'end'))
def test_undo_operation_for_operation_in_invalid_state(context, state):
    with context.reflect_db() as db:
        op = add_test_operation(db, context, state=state)

        assert_that(
            calling(db.code.undo_operation).with_args(op.id, context.request_id),
            raises(InternalError, "no transition"),
            "should throw on unexpected action for the state"
        )


def test_undo_operation_for_operation_which_does_not_exist(context):
    with context.reflect_db() as db:
        assert_that(
            calling(db.code.undo_operation).with_args(rand_uuid(), context.request_id),
            raises(InternalError, "no operation found"),
            "should throw exception for the operation does not exist"
        )

import pytest

from mail.pypg.hamcrest_support.concurrency import concurrent_call, is_serialized
from hamcrest import (
    assert_that,
    equal_to,
    empty,
    not_,
)
from .misc import add_test_operation, create_test_chunk


def add_operations(db, context, state, count):
    return [add_test_operation(db, context, state=state) for i in range(count)]


def test_purge_operation_deletes_operation_in_end_state_and_its_chunks(context):
    with context.reflect_db() as db:
        res = create_test_chunk(db, context, state='done')
        db.mops.operations.update({'state': 'end'}, id=res.operation.id)

        db.code.purge_operations(i_count=10)

        assert_that(
            db.mops.message_chunks.select(op_id=res.operation.id, id=res.chunks[0].id), empty(),
            "all operation chunks should be deleted"
        )

        assert_that(
            db.mops.operations.select(id=res.operation.id), empty(),
            "operation should be deleted"
        )


@pytest.mark.parametrize(('state'), ('fresh', 'in_progress', 'in_revert', 'complete', 'reverted'))
def test_purge_operation_preserves_operations_not_in_end_state(context, state):
    with context.reflect_db() as db:
        op = add_test_operation(db, context, state=state)

        db.code.purge_operations(i_count=10)

        assert_that(
            db.mops.operations.select(id=op.id), not_(empty()),
            "operation in state {} should not be deleted".format(state)
        )


@pytest.mark.parametrize(('total', 'count'), ((15, 10), (10, 10), (5, 10), (0, 10)))
def test_purge_operation_deletes_no_more_than_count(context, total, count):
    with context.reflect_db() as db:
        add_operations(db, context, state='end', count=total)

        should_delete_count = min(total, count)
        assert_that(
            db.code.purge_operations(i_count=count), equal_to(should_delete_count),
            "function should return number of deleted operations"
        )

        should_stay_count = max(0, total-count)
        assert_that(
            len(db.mops.operations.select()), equal_to(should_stay_count),
            "function should delete no more than {} operations and {} operations should stay".format(count, should_stay_count)
        )


def test_purge_operation_concurrent_call(context):
    with context.reflect_db() as db:
        add_operations(db, context, state='end', count=20)

        assert_that(
            concurrent_call(lambda d: d.code.purge_operations(i_count=10)).on(context.dsn()),
            not_(is_serialized()),
            "concurrent function calls should not be serialized"
        )

        assert_that(
            db.mops.operations.select(), empty(),
            "function calls should delete all the operations"
        )

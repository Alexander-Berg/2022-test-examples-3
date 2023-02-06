from mail.pypg.hamcrest_support.concurrency import concurrent_call, is_serialized
from psycopg2 import InternalError
from hamcrest import (
    has_properties,
    has_entries,
    assert_that,
    calling,
    raises,
    equal_to,
)
from .misc import last_change_log_entry, add_test_operation


def test_transit_operation_state_fresh_to_reverted(context):
    uid = 1000

    with context.reflect_db() as db:
        op = add_test_operation(db, context, uid=uid)

        res = db.impl.transit_operation_state(
            i_id=op.id, i_action='undo_operation', i_request_id=context.request_id
        )
        assert_that(res.state, equal_to('reverted'), "should transit operation in reverted state")

        assert_that(last_change_log_entry(db, uid), has_properties(
            op_id=op.id, type='operation_change_state', uid=uid, request_id=context.request_id,
            changed=has_entries(
                action='undo_operation', is_recent=True, from_state='fresh', new_state='reverted'
            )
        ), "change log should contain an entry about operation state has been changed")


def test_transit_operation_state_fresh_for_not_last_op_and_undo(context):
    with context.reflect_db() as db:
        op = add_test_operation(db, context)
        add_test_operation(db, context, uid=op.uid)

        res = db.impl.transit_operation_state(
            i_id=op.id, i_action='undo_operation', i_request_id=context.request_id
        )
        assert_that(res.state, equal_to('fresh'), "should not transit operation in other state")


def test_transit_operation_state_with_unexpected_action(context):
    with context.reflect_db() as db:
        op = add_test_operation(db, context)

        assert_that(
            calling(db.impl.transit_operation_state).with_args(op.id, 'delete_operation', context.request_id),
            raises(InternalError, "no transition from fresh state with delete_operation action"),
            "should throw on unexpected action for the state"
        )


def test_transit_operation_state_concurrent_call(context):
    with context.reflect_db() as db:
        op = add_test_operation(db, context)

        state = iter(['process_operation', 'undo_operation'])

        def transit_operation_state(d):
            return d.impl.transit_operation_state(op.id, next(state), context.request_id)

        assert_that(
            concurrent_call(transit_operation_state).on(context.dsn()),
            is_serialized(),
            "concurrent function calls should be serialized"
        )

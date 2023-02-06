import json

from uuid import uuid4 as rand_uuid
from .misc import last_change_log_entry
from hamcrest import (
    has_properties,
    has_entries,
    assert_that,
    less_than,
    equal_to,
    contains,
)


def test_add_operation_with_given_data(context):
    uid = 1000
    id = rand_uuid()
    data = {'type': 'test', 'param': 'value'}
    with context.reflect_db() as db:
        res = db.code.add_operation(
            i_id=id, i_type='complex_move', i_uid=uid, i_data=json.dumps(data), i_request_id=context.request_id,
        )

        assert_that(res.inserted, equal_to(True), "should be true on insertion success")

        assert_that(res.operation, has_properties(
            type='complex_move', uid=uid, data=data, state='fresh'
        ), "should insert operation in 'fresh' state")

        assert_that(db.mops.operations.select(id=id), contains(
            has_properties(type='complex_move', uid=uid, data=data, state='fresh')
        ), "should contain inserted operation")

        assert_that(last_change_log_entry(db, uid), has_properties(
            op_id=id, type='operation_create', uid=1000, request_id=context.request_id,
            changed=has_entries(type='complex_move', data=data)
        ), "change log should contain an entry about the operation")


def test_add_operation_twice_with_same_id(context):
    id = rand_uuid()
    data = {'type': 'test', 'param': 'value'}

    with context.reflect_db() as db:
        db.code.add_operation(id, 'complex_move', 1000, json.dumps(data), context.request_id)
        res = db.code.add_operation(id, 'label', 1001, json.dumps(data), context.request_id)

        assert_that(res.inserted, equal_to(False), "should be false since no insertion should be made")

        assert_that(res.operation, has_properties(
            type='complex_move', uid=1000, data=data, state='fresh'
        ), "should return the 1st operation already stored with given id")

        assert_that(db.mops.operations.select(id=id), contains(
            has_properties(type='complex_move', uid=1000, data=data, state='fresh')
        ), "should contain the 1st operation as it should remain unmodified")


def test_add_two_operations_with_same_uid(context):
    data = {'type': 'test', 'param': 'value'}

    with context.reflect_db() as db:
        op1 = db.code.add_operation(rand_uuid(), 'label', 1000, json.dumps(data), context.request_id).operation
        op2 = db.code.add_operation(rand_uuid(), 'label', 1000, json.dumps(data), context.request_id).operation

    assert_that(op1.seq_id, less_than(op2.seq_id), "seq_id should increase")

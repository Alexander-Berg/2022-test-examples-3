# coding: utf-8

import logging

import yaml
from hamcrest import (assert_that,
                      not_,
                      empty,
                      has_item,
                      has_property,
                      equal_to,
                      all_of,
                      has_properties, )

from pymdb.operations import OperationError
from pymdb.tools import Diag
from tests_common.pytest_bdd import then, when  # pylint: disable=E0611
from tools import ok_

log = logging.getLogger(__name__)


def get_locks(context, op):
    locks = Diag(context.conn).get_locks(op)
    return locks


@then('"{op_id}" should wait for "{lock_type}" lock')
def step_wait_for_lock(context, op_id, lock_type):
    op = context.operations[op_id]
    assert_that(op.in_transaction(), '%r in transaction' % op)
    assert_that(
        get_locks(context, op),
        has_item(
            has_properties(
                'lock_type', lock_type,
                'granted', False
            )
        )
    )


@then('"{op_id}" should not wait for any lock')
def step_not_waiting(context, op_id):
    assert_that(
        get_locks(context, context.operations[op_id]),
        all_of(
            not_(empty()),
            not_(has_item(has_property('granted', False))),
        )
    )


@when(u'we commit "{op_id}"')
def step_commit(context, op_id):
    context.operations[op_id].commit()


@when(u'we rollback "{op_id}"')
def step_rollback(context, op_id):
    context.operations[op_id].rollback()


@then('commit "{op_id}" should produce "{error}"')
def step_commit_with_error(context, op_id, error):
    op = context.operations[op_id]
    try:
        op.commit()
    except OperationError as exc:
        if exc.__class__.__name__ == error:
            return
        raise
    raise AssertionError(
        "expect {0}, at {1} but no exception raised".format(error, op)
    )


def assert_one_row_in_result(context, op_id):
    op = context.operations[op_id]
    ok_(
        len(op.result) == 1,
        "operation: {0} has more then one result: {1}".format(op, op.result)
    )
    return op.result[0]


@then('"{op_id}" result has one row')
def step_result_one(context, op_id):
    res = assert_one_row_in_result(context, op_id)
    if context.table:
        row = context.table[0]
        for h in row.headings:
            real = getattr(res, h)
            expected = row[h]
            if h == 'mid':
                expected = context.res.get_mid(expected)
            else:
                expected = real.__class__(expected)
            ok_(
                expected == real,
                '{h} {expected}!={real} on result: {res}'.format(
                    **locals()
                )
            )


@then('"{op_id}" result is empty')
def step_result_empty(context, op_id):
    op = context.operations[op_id]
    ok_(
        len(op.result) == 0,
        "operation: {0} result: {1} is not empty".format(op, op.result)
    )


def get_yaml_from_step_text(context):
    if not context.text:
        raise SyntaxError('This step require text')
    return yaml.safe_load(context.text)


@then('"{op_id:OpID}" result has one row with')
def step_result_contains_one_row_with(context, op_id):
    res = assert_one_row_in_result(context, op_id)
    expected = get_yaml_from_step_text(context)
    if not isinstance(expected, dict):
        raise NotImplementedError(
            "Can compare only dictionary with result"
        )

    assert_that(
        res, has_properties(**expected)
    )


@when('"{op_id:OpID}" result produce one message "{mid:Mid}"')
def step_set_mid_from_result(context, op_id, mid):
    context.res[mid] = assert_one_row_in_result(context, op_id)


@then('"{op_id}" result has unchanged revision')
def step_result_unchanged(context, op_id):
    op = context.operations[op_id]
    result = op.result
    if isinstance(result, list):
        ok_(
            len(result) == 1,
            "{op} result has more then one row: {result}".format(**locals())
        )
        result = result[0]
    ok_(
        result.revision == 0,
        '{op} result is not unchanged: {result}'.format(**locals())
    )


def get_result(context, result_key):
    if result_key in context.res:
        return context.res[result_key]
    return context.operations[result_key].result


@then(u'"{left_key:OpID}" is same result as "{right_key:OpID}"')
def step_impl(context, left_key, right_key):
    left = get_result(context, left_key)
    right = get_result(context, right_key)
    assert_that(
        left, equal_to(right)
    )


@then(u'{name:w} in "{left_key:OpID}" result {matcher:Matcher} in "{right_key:OpID}" result')
def step_left_property_less_then_right(context, name, matcher, left_key, right_key):
    left = assert_one_row_in_result(context, left_key)
    right = assert_one_row_in_result(context, right_key)
    assert_that(right, has_property(name))
    assert_that(
        left,
        has_property(
            name,
            matcher(getattr(right, name))
        )
    )

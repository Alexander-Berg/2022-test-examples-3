# coding: utf-8

from pymdb.operations import ResetFresh
from tests_common.pytest_bdd import when, then  # pylint: disable=E0611


@when(u'we reset fresh as "{op_id}"')
def step_reset_fresh(context, op_id):
    context.operations[op_id] = context.make_async_operation(ResetFresh)()


@when(u'we reset fresh')
def step_reset_fresh_sync(context):
    context.apply_op(ResetFresh)


@then(u'"{op_id}" result is "{value}"')
def step_check_reset_fresh_result(context, op_id, value):
    op = context.operations[op_id]
    op.commit()
    real_res = op.result[0]['reset_fresh']
    assert value == str(real_res), \
        'supposed to be {0}, actual {1}'.format(value, real_res)


@then('fresh counter is "{fc}" and has revision "{cr}"')
def step_check_fresh_counter(context, fc, cr):
    real_fresh_counter = context.qs.fresh_counter()
    assert fc == str(real_fresh_counter), \
        'fresh count supposed to be {0}, actual {1}'.format(
            fc, real_fresh_counter)
    dont_check_revision = cr == 'any'
    if not dont_check_revision:
        real_counters_revision = context.qs.counters_revision()
        assert cr == str(real_counters_revision), \
            'counters revision supposed to be {0}, actual {1}'.format(
                cr, real_counters_revision)


@then('global revision is "{rev}"')
def step_check_global_revision(context, rev):
    real_rev = context.qs.global_revision()
    assert rev == str(real_rev), \
        'supposed to be {0}, actual {1}'.format(
            rev, real_rev)

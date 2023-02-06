# coding: utf-8

import logging

from pymdb.operations import DeleteMessages
from tests_common.pytest_bdd import when, then  # pylint: disable=E0611

log = logging.getLogger(__name__)


def log_operation_result(result):
    log.info('result is: %r', result)


def make_op(context, operation_maker, mids):
    mids = context.res.get_mids(mids)
    op = operation_maker(DeleteMessages)(mids)
    op.add_result_callback(log_operation_result)
    return op


@when('we try delete "{mids:MidsRange}" as "{op_id}"')
def step_try_delete_no_wait(context, mids, op_id):
    context.operations[op_id] = make_op(
        context, context.make_async_operation, mids
    )


@then('"{mids:MidsRange}" is deleted')
@then('"{mids:MidsRange}" are deleted')
def step_check_is_deleted(context, mids):
    expected = set(context.res.get_mids(mids))
    deleted = set([d.mid for d in context.qs.deleted_messages()])
    assert deleted.issuperset(expected), \
        'Not all of mids: {0} exists in deleted: {1}'.format(
            expected,
            deleted
        )


@then('"{mids:MidsRange}" is not deleted')
@then('"{mids:MidsRange}" are not deleted')
def step_check_is_not_deleted(context, mids):
    expected_not_deleted = set(context.res.get_mids(mids))
    deleted = set([d.mid for d in context.qs.deleted_messages()])
    assert deleted.isdisjoint(expected_not_deleted), \
        'Some of mids: {0} exists in deleted: {1}'.format(
            expected_not_deleted,
            deleted
        )


@then('"{mids:MidsRange}" is purged from messages')
@then('"{mids:MidsRange}" are purged from messages')
def step_check_is_purged(context, mids):
    expected_purged = set(context.res.get_mids(mids))
    messages = set([d['mid'] for d in context.qs.messages_table()])
    assert messages.isdisjoint(expected_purged), \
        'Some of mids: {0} exists in messages: {1}'.format(
            expected_purged,
            messages,
        )


@when(r'he delete "{mids:MidsRange}"')
@when(r'we delete "{mids:MidsRange}"')
def step_delete(context, mids):
    make_op(context, context.make_operation, mids).commit()


@when('user "{user}" deletes "{mid_keys:MidsRange}"')
def step_user_delete(context, user, mid_keys):
    uid = context.users[user]
    mids = [context.res.get_mid(mid_key) for mid_key in mid_keys]
    DeleteMessages(context.conn, uid)(mids).commit()

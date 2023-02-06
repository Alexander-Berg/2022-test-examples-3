# coding: utf-8
from hamcrest import (assert_that,
                      not_,
                      has_entry,
                      has_property,
                      is_in,
                      empty)

from pymdb.replication import (Commit,
                               is_dml_change,
                               get_changes_with_timeout,
                               get_current_wal_lsn)
from tests_common.pytest_bdd import given, when, then


@when('we setup replication stream')
def step_init_replication(context):
    return init_replication(context)


@given('replication stream')
def step_set_replication(context):
    init_replication(context)


def init_replication(context):
    if 'replica' not in context:
        context.scenario.skip('Due no replica')
        return
    current_lsn = get_current_wal_lsn(context.conn)
    context.replication_stream = context.replica.start_replication(current_lsn)


def read_changes_until_commit(replication_stream, xid):
    changes = []
    for c in get_changes_with_timeout(replication_stream, 3):
        changes.append(c)
        if isinstance(c, Commit) and c.xid == xid:
            return changes


@then('there are only our user changes in replication stream')
def step_check_user_in_replication_log(context):
    check_uids_in_replication_log(**locals())


@then('there are only "{user:UserName}" changes in replication stream')
def step_check_target_user_in_replication_log(context, user):
    check_uids_in_replication_log(**locals())


def check_uids_in_replication_log(context, user=None):
    if user:
        our_uids = {context.users[user]}
    else:
        our_uids = set(context.users.values())

    last_opearation_xid = context.operations.last().xid

    replication_changes = read_changes_until_commit(
        context.replication_stream, last_opearation_xid)

    dml_changes = (c for c in replication_changes if is_dml_change(c))
    dml_changes_with_uid = list(
        c for c in dml_changes if 'uid' in c.attributes
    )

    assert_that(
        dml_changes_with_uid,
        not_(empty()),
        "There are no DML changes with uid")

    for change in dml_changes_with_uid:
        assert_that(
            change,
            has_property(
                'attributes',
                has_entry(
                    'uid',
                    has_property(
                        'value',
                        is_in(our_uids)))),
            'we change unexpected user'
        )

# coding: utf-8

from pymdb.operations import BlacklistAdd, BlacklistRemove
from tests_common.pytest_bdd import when, then  # pylint: disable=E0611


@then(u'blacklist is empty')
def step_blacklist_is_empty(context):
    bl = context.qs.blacklist()
    assert not bl, \
        'Blacklist is not empty: %r' % bl


@then('blacklist is')
def step_blacklist_is(context):
    assert context.table, \
        'blacklist is requires emails as table'
    bl = context.qs.blacklist()
    black_emails = set([b['email'] for b in bl])
    test_emails = set(row['email'] for row in context.table)
    assert black_emails == test_emails, \
        'Got different emails ' \
        'expected {test_emails} ' \
        'found: {black_emails} ' \
        'full blacklist {bl}'.format(**locals())
    for email in test_emails:
        assert email in black_emails, \
            "Can't find {email} in" \
            " blacklist emails {black_emails}" \
            " full blacklist {bl}".format(**locals())


@when(u'we add "{email}" to blacklist')
def step_add_to_blacklist(context, email):
    context.apply_op(BlacklistAdd, email=email)


@when(u'we add emails to blacklist')
def step_add_to_blacklist_from_table(context):
    assert context.table, 'This step require table'
    for row in context.table:
        context.apply_op(BlacklistAdd, email=row['email'])


@when(u'we try add "{email}" to blacklist as "{op_id}"')
def step_try_add_to_blacklist(context, email, op_id):
    context.operations[op_id] = context.make_async_operation(
        BlacklistAdd
    )(email)


@when(u'we remove "{email}" from blacklist')
def step_remove_from_blacklist(context, email):
    context.apply_op(BlacklistRemove, email=email)


@when(u'we remove emails from blacklist')
def step_remove_from_blacklist_many(context):
    emails = [row['email'] for row in context.table]
    assert emails, 'No emails found in %r' % context.table
    context.apply_op(BlacklistRemove, emails=emails)

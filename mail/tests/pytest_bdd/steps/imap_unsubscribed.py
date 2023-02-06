# coding: utf-8

from pymdb.operations import IMAPAddUnsubscribed, IMAPDeleteUnsubscribed
from pymdb.types import IMAPUnsubscribedFolder
from tests_common.pytest_bdd import then, when


def is_null_str(s):
    return s.upper() == 'NULL'


def conv_null_str(s):
    if is_null_str(s):
        return None
    return s


def parse_full_name(full_name):
    full_name = full_name.strip()
    if is_null_str(full_name):
        return None
    assert full_name.startswith('{') and full_name.endswith('}'), \
        'Expect full_name in `array` format: {INBOX,foo}, ' \
        'got %r' % full_name
    full_name = full_name.strip('{}')
    return [conv_null_str(f) for f in full_name.split(',')]


def check_unsubscribed_fname(real, expected):
    cmp_uf = lambda seq: list(sorted((f.as_dict() for f in seq), key=lambda x: x['full_name']))
    assert cmp_uf(real) == cmp_uf(expected), \
        'Got different imap_unsubscribed_folders, ' \
        'real: %r, expected: %r' % (
            real, expected)


@then(u'there is one unsubscribed folder "{full_name}" at revision "{revision:d}"')
def step_unsubscribed_folder(context, full_name, revision):
    expected = IMAPUnsubscribedFolder(
        full_name=parse_full_name(full_name),
        revision=revision)
    real = context.qs.imap_unsubscribed_folders()
    assert len(real) == 1, \
        'Expect one unsubscribed, got %r' % real
    check_unsubscribed_fname(
        real,
        [expected])


@then(u'there are "{expected_count:d}" unsubscribed folders')
def step_unsubscribed_folders(context, expected_count):
    real = context.qs.imap_unsubscribed_folders()
    assert len(real) == expected_count, \
        'Expect %d got %d unsubscribed %r' % (
            expected_count, len(real), real)
    assert context.table, 'This step require table'
    for th in ['revision', 'full_name']:
        assert th in context.table.headings, \
            'This step require table with %s' % th
    expected = []
    for row in context.table:
        expected.append(IMAPUnsubscribedFolder(
            full_name=parse_full_name(row['full_name']),
            revision=int(row['revision'])))
    check_unsubscribed_fname(
        real, expected)


@then(u'there are no unsubscribed folders')
def step_no_unsubscribed_folders(context):
    iuf = context.qs.imap_unsubscribed_folders()
    assert not iuf, \
        'Expect empty imap_unsubscribed_folder got %r' % iuf


@when(u'we remove "{full_name}" from unsubscribed')
def step_remove_unsubscribed(context, full_name):
    context.apply_op(
        IMAPDeleteUnsubscribed,
        full_name=parse_full_name(full_name)
    )


@when(u'we add "{full_name}" to unsubscribed')
def step_add_unsubscribed(context, full_name):
    context.apply_op(
        IMAPAddUnsubscribed,
        full_name=full_name
    )


@when(u'we try add "{full_name}" to unsubscribed as "{op_id}"')
def step_try_add_unsubscribed(context, full_name, op_id):
    context.operations[op_id] = context.make_async_operation(
        IMAPAddUnsubscribed
    )(
        parse_full_name(full_name)
    )

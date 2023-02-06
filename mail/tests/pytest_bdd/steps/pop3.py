# coding: utf-8

from hamcrest import (assert_that,
                      empty,
                      equal_to,
                      )
from pytest_bdd import parsers

from pymdb.helpers import cast_bool
from pymdb.operations import (POP3FoldersEnable,
                              POP3FoldersDisable,
                              InitializePOP3Folder,
                              POP3Delete)
from tests_common.pytest_bdd import when, then


@then(u'he has no pop3 folders')
def step_empty_folders(context):
    assert_that(context.qs.pop3_folders(), empty())


@then(u'pop3 box is empty')
def step_empty_box(context):
    assert_that(context.qs.pop3_box(), empty())


@then(u'pop3 box is')
def step_pop3_box_is(context):
    pop3_box = context.qs.pop3_box()
    assert pop3_box, \
        'Got empty pop3 box: %r' % pop3_box
    assert context.table, \
        'This step require table'
    assert 'mid' in context.table.headings, \
        'Add mid to step table'
    pop3_by_mid = dict((r['mid'], r) for r in pop3_box)
    unsupported_keys = \
        set(context.table.headings) - set(['mid', 'seen', 'spam', 'folder'])
    assert not unsupported_keys, \
        'This step does not support %r ' % unsupported_keys
    folders = None
    for row in context.table:
        mid_key = row['mid']
        mid = context.res.get_mid(mid_key)
        assert mid in pop3_by_mid, \
            'Can\'t find %s - %r in pop3 - %r' % (
                mid_key, mid, pop3_box)
        pm = pop3_by_mid[mid]
        for bool_field in ['seen', 'spam']:
            if bool_field in row.headings:
                expected = cast_bool(row[bool_field])
                assert expected == pm[bool_field], \
                    'Expect that %s is %s, at: %r' % (
                        bool_field, expected, pm)
        if 'folder' in row.headings:
            if folders is None:
                folders = context.qs.folders()
            fld = [f for f in folders if f.type == row['folder']]
            assert fld, \
                'Can\'t find folder %r in folders %r' % (
                    row['folder'], folders)
            fld = fld[0]
            real_fld = [f for f in folders if f.fid == pm['fid']][0]
            assert fld == real_fld, \
                'Expected folder %r, got %r' % (
                    fld, real_fld)


@then(u'pop3 box is "{mids:MidsRange}"')
def step_pop3_box_is_mids_only(context, mids):
    pop3_box = context.qs.pop3_box()
    pop3_mids = set([m['mid'] for m in pop3_box])
    assert_that(pop3_mids, equal_to(set(context.res.get_mids(mids))))


def get_pop3_folder(context, folder_type):
    fld = context.qs.folder_by_type(folder_type)
    return fld


@then(u'pop3 "{folder_type:w}" at revision "{revision:d}"')
def step_pop3_folder_revision(context, folder_type, revision):
    pop3_fld = get_pop3_folder(context, folder_type)
    assert revision == pop3_fld.revision, \
        'Expect %d revision got %d on %r' % (
            revision, pop3_fld.revision, pop3_fld)


POP3_FOLDER_STS = {
    'enabled': lambda pf: pf.pop3state.enabled,
    'disabled': lambda pf: not pf.pop3state.enabled,
    'initialized': lambda pf: pf.pop3state.initialized,
    'not initialized': lambda pf: not pf.pop3state.initialized
}
POP3_STATUS_RE = \
    r'pop3 is ' \
    r'(?P<sts>({sts})( and ({sts}))?) ' \
    r'for "(?P<folder_type>\w+)"' \
    r'( at revision "(?P<revision>\d+)")?'.format(
        sts=u'|'.join(POP3_FOLDER_STS.keys()))


@then(POP3_STATUS_RE, parse_builder=parsers.re)
def step_pop3_folder_status(context):
    kwargs = context.args
    pop3_fld = get_pop3_folder(context, kwargs['folder_type'])
    sts = kwargs['sts'].split(' and ')
    for status in sts:
        assert POP3_FOLDER_STS[status](pop3_fld), \
            'Expect %s on folder %r' % (
                status, pop3_fld)
    if kwargs.get('revision'):
        expected_revision = int(kwargs['revision'])
        real_revision = pop3_fld.revision
        assert expected_revision == real_revision, \
            'Expect %d revision, got %d on %r' % (
                expected_revision, real_revision, pop3_fld)


POP3_ACTION_RE = r'''
we
((?P<toggle>enable|disable)|(?P<init>initialize)|( and ))+
pop3 for "(?P<folder_type>\w+)"
'''.strip().replace(u'\n', u' ')


@when(POP3_ACTION_RE, parse_builder=parsers.re)
def step_pop3_folder_action(context):
    kwargs = context.args
    fld = context.qs.folder_by_type(kwargs['folder_type'])
    assert kwargs.get('toggle') or kwargs.get('init'), \
        'No action defined in %r' % kwargs

    if kwargs.get('toggle') and kwargs['toggle'] == 'enable':
        op = POP3FoldersEnable(context.conn, context.uid)
        fids = [fld.fid]
        op(fids)
        op.commit()

    if kwargs.get('toggle') and kwargs['toggle'] == 'disable':
        op = POP3FoldersDisable(context.conn, context.uid)
        fids = [fld.fid]
        op(fids)
        op.commit()

    if kwargs.get('init'):
        op = InitializePOP3Folder(context.conn, context.uid)
        op(fld.fid)
        op.commit()


@when(u'we pop3-delete "{mids:MidsRange}"')
def step_pop3_delete(context, mids):
    context.apply_op(
        POP3Delete,
        mids=context.res.get_mids(mids)
    )


@when(u'we try pop3-delete "{mids:MidsRange}" as "{op_id:OpID}"')
def step_try_pop3_delete(context, mids, op_id):
    context.operations[op_id] = context.make_async_operation(
        POP3Delete
    )(
        mids=context.res.get_mids(mids)
    )

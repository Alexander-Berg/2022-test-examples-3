# coding: utf-8

from datetime import datetime

from pymdb.helpers import split_list
from pymdb.operations import (
    FixUser,
    FixNotPositiveLIDs,
    FixSoLabelTypes,
    RecreateImapChains,
)
from pymdb.tools import strip_q
from pymdb.types import ListOfFixes
from pymdb.types import MailLabelDef
from mail.pypg.pypg.common import qexec as qexec_no_commit
from mail.pypg.pypg.query_conf import load_from_my_file
from tests_common.pytest_bdd import when, then

Q = load_from_my_file(__file__)


def do_commit(conn, cur):
    conn.wait()
    cur.execute('COMMIT')
    conn.wait()


def qexec_raw_str(conn, query, *args):
    cur = conn.cursor()
    cur.execute(strip_q(query), args)
    do_commit(conn, cur)
    return cur


def qexec(conn, query, **query_args):
    cur = qexec_no_commit(conn, query, **query_args)
    do_commit(conn, cur)


@when(u'we accidentally delete "{label_type:w}" label "{label_name:w}"')
def step_delete_label(context, label_type, label_name):
    all_labels = context.qs.labels()
    label = [
        l for l in all_labels
        if l.type == label_type and l.name == label_name
    ]
    assert label, \
        "Can't find label with name: {label_name} " \
        "and type: {label_type} in {all_labels}".format(
            **locals())
    lid = label[0].lid

    qexec(
        context.conn,
        Q.accidently_delete_label,
        uid=context.uid,
        lid=lid)


@when(u'we accidentally set serial "{serial_type:SerialColumn}" to "{br_serial:d}"')
def step_br_serial(context, serial_type, br_serial):
    qexec_raw_str(
        context.conn,
        ''' /* br serials accident */
           UPDATE mail.serials
              SET %s = %%s
            WHERE uid = %%s''' % serial_type,
        br_serial, context.uid)


@when(u'we accidentally delete "{folder_type:w}" folder')
def step_delete_folder(context, folder_type):
    fld = context.qs.folder_by_type(folder_type)
    qexec(
        context.conn,
        Q.accidently_delete_folder,
        uid=context.uid,
        fid=fld.fid)


def null_or_number(s):
    s = s.strip()
    if s.lower() == 'null':
        return None
    return int(s)


def parse_bool(s):
    assert s in ('true', 'false'), \
        'Unexpected boolean value %r' % s
    if s == 'true':
        return True
    return False


def folder_column(column_name, column_value):
    if column_name in ['name', 'type']:
        return column_value.strip()
    elif column_name == 'unique_type':
        return parse_bool(column_value)
    return null_or_number(column_value)


def set_folders_columns(conn, uid, fld, column_values):
    columns = []
    values = []
    for c, v in column_values.items():
        columns.append('{0} = %s'.format(c))
        values.append(folder_column(c, v))
    qexec_raw_str(
        conn,
        ''' /* update folder accident */
            UPDATE mail.folders
               SET {0}
             WHERE uid = %s
               AND fid = %s '''.format('\n,'.join(columns)),
        *(values + [uid, fld.fid])
    )


@when(u'we accidentally set "{folder_type:w}" {column} to "{br_value}"')
def step_br_folder(context, folder_type, column, br_value):
    fld = context.qs.folder_by_type(folder_type)
    set_folders_columns(
        context.conn,
        context.uid,
        fld,
        {column: br_value})


@when(u'we accidentally set "{folder_type:w}" counters')
def step_br_folder_many_columns(context, folder_type):
    fld = context.qs.folder_by_type(folder_type)
    assert context.table, 'this step require table'
    column_values = {}
    for row in context.table:
        column_values[row['column']] = row['invalid_value']
    set_folders_columns(
        context.conn,
        context.uid,
        fld,
        column_values)


@then('check produce')
def step_check(context):
    violations = context.qs.violations()
    assert violations, 'No violations found %r' % violations
    real_violations = set(v['name'] for v in violations)
    assert context.table, 'This step require table'
    assert len(context.table.rows), 'This step require non empty table'
    row = context.table.rows[0]
    assert row.headings == [u'name'], \
        'Only name supported: got %r' % row.headings
    expeted_violations = set(r[u'name'] for r in context.table.rows)
    assert expeted_violations == real_violations, \
        'Expected %r, got %r' % (
            expeted_violations, real_violations)


@then('check produce nothing')
def step_check_all_ok(context):
    violations = context.qs.violations()
    assert not violations, \
        'Found %r' % violations


@then('fix log contains')
def step_fix_log_contins(context):
    fix_log = context.qs.fix_log()
    for row in context.table:
        fix_key = row['name']
        fix_revision = None
        if 'revision' in context.table.headings:
            fix_revision = int(row['revision'].strip())
        for fr in fix_log:
            if fr.fix_key == fix_key:
                if fix_revision is not None:
                    assert fr.revision == fix_revision, \
                        'Expect %d revision got %d on %r' % (
                            fix_revision, fr.revision, fr)
                break
        else:
            raise AssertionError(
                "Can't find %s in fix log % r" % (
                    fix_key, fix_log))


@then('fix log is empty')
def step_fix_log_is_empty(context):
    fix_log = context.qs.fix_log()
    assert not fix_log, \
        'Expect empty fix_log got %r' % fix_log


def cast_fix_targets(fix_targets):
    return ListOfFixes(
        [] if fix_targets == 'all' else split_list(fix_targets)
    )


@when('we recreate chains for "{folder_type}" with step "{chain_size:w}"')
def step_recreate_imap_chain(context, folder_type, chain_size):
    folder = context.qs.folder_by_type(folder_type)
    RecreateImapChains(context.conn, context.uid)(folder.fid, chain_size).commit()


@when('we fix "{fix_targets}"')
def step_fix_user(context, fix_targets):
    o = context.make_operation(FixUser)
    o(cast_fix_targets(fix_targets))
    o.commit()


@when('we try fix "{fix_targets}" as "{op_id}"')
def step_try_fix_user(context, fix_targets, op_id):
    context.operations[op_id] = context.make_async_operation(
        FixUser
    )(cast_fix_targets(fix_targets))


def set_label_column(context, label_type, label_name, column_name, new_value):
    label = context.qs.find_one_label(
        MailLabelDef(name=label_name, type=label_type)
    )
    qexec_raw_str(
        context.conn,
        '''UPDATE mail.labels
              SET {0} = %s
            WHERE uid = %s
              AND lid = %s
        '''.format(column_name),
        new_value,
        context.uid,
        label.lid
    )


@when(u'we accidentally set lid for "{label_type:w}" label "{label_name:w}" to "{accident_lid:d}"')
def step_accidentally_set_lid(
        context, label_type, label_name, accident_lid):
    set_label_column(
        context, label_type, label_name,
        column_name='lid',
        new_value=accident_lid
    )


@when(u'we accidentally set message_count to "{new_count:d}" for "{label_type:w}" label "{label_name:w}"')
def step_accidentally_set_label_messagecount(
        context, new_count, label_type, label_name):
    set_label_column(
        context, label_type, label_name,
        column_name='message_count',
        new_value=new_count
    )


@when(u'we accidentally set message_seen to "{new_count:d}" for "{label_type:w}" label "{label_name:w}"')
def step_accidentally_set_label_messageseen(
        context, new_count, label_type, label_name):
    set_label_column(
        context, label_type, label_name,
        column_name='message_seen',
        new_value=new_count
    )


@when('we accidentally set chain to "{new_chain}" on "{mid}"')
def step_accidentally_set_chain(context, new_chain, mid):
    if new_chain == 'NULL':
        new_chain = None
    else:
        new_chain = int(new_chain)
    mid = context.res.get_mid(mid)
    qexec(
        context.conn,
        Q.accidently_set_box_chain,
        chain=new_chain,
        uid=context.uid,
        mid=mid,
    )


@when(u'we fix not positive lids')
def step_we_fix_not_positive_lids(context):
    context.apply_op(FixNotPositiveLIDs)


@when(u'"{step_mid:Mid}" message accidentally appears in pop3 box')
def step_accidentally_add_message_to_pop3_box(context, step_mid):
    accidentally_add_message_to_pop3_box(**locals())


@when(u'"{step_mid:Mid}" message accidentally appears in pop3 box with "{step_folder:w}" folder')
def step_accidentally_add_message_to_pop3_box_foldered(context, step_mid, step_folder):
    accidentally_add_message_to_pop3_box(**locals())


def accidentally_add_message_to_pop3_box(context, step_mid, step_folder=None):
    mid = context.res.get_mid(step_mid)
    msg = context.qs.message(mid=mid)
    fid = msg['fid']
    if step_folder:
        fid = context.qs.folder_by_type(step_folder).fid
    qexec(
        context.conn,
        Q.accidently_insert_into_pop3_box,
        uid=context.uid,
        mid=mid,
        fid=fid,
        size=msg['size'],
    )


@when(u'we execute "{util:w}" util')
def step_execute_util(context, util):
    query = "SELECT * FROM util.{0}(%s)".format(util)
    qexec_raw_str(
        context.conn,
        query,
        context.uid,
    )


_DEL_ROW_Q = '''
DELETE FROM {SCHEMA}.{TABLE}
 WHERE {KEY} = %s AND uid=%s'''


@when(u'row from "{table:w}" by "{key:w}" key and value "{value}" disappears')
def step_delete_from_table(context, table, key, value):
    query = _DEL_ROW_Q.format(
        schema='mail',
        table=table,
        key=key,
    )
    if key == 'mid':
        value = context.res.get_mid(value)
    qexec_raw_str(context.conn, query, value, context.uid)


@when(u'we accidentally set doom_date for "{mid:Mid}" to "{doom_date:w}"')
def step_set_doom_date(context, mid, doom_date):
    if doom_date not in ('NULL', 'now'):
        raise NotImplementedError(
            'Unsupported doome_date - %r' % doom_date
        )
    if doom_date == 'NULL':
        doom_date = None
    else:
        doom_date = datetime.now()
    qexec(
        context.conn,
        Q.accidently_set_doom_date,
        doom_date=doom_date,
        uid=context.uid,
        mid=context.res.get_mid(mid)
    )


def queries_chain(conn, *queries_with_args):
    cur = conn.cursor()
    for query, query_args in queries_with_args:
        cur.execute(query.query, query_args)
        conn.wait()
    cur.execute('commit')
    conn.wait()


@when(u'message "{mid_var:Mid}" accidentally lose thread')
def step_message_lose_thread(context, mid_var):
    mid = context.res.get_mid(mid_var)
    tid = context.qs.message(mid=mid)['tid']
    if tid is None:
        raise RuntimeError(
            'This step require message with tid IS NOT NULL'
        )

    queries_chain(
        context.conn,
        [Q.accidentally_delete_from_threads,
         dict(uid=context.uid, tid=tid)],
        [Q.accidentally_set_tid_and_newest_tif,
         dict(uid=context.uid, mid=mid, tid=None, newest_tif=False)]
    )


@when(u'message "{mid_var:Mid}" accidentally gain thread')
def step_message_gain_thread(context, mid_var):
    mid = context.res.get_mid(mid_var)
    message = context.qs.message(mid=mid)
    if message['tid'] is not None:
        raise RuntimeError(
            'This step require message without tid %r' % message
        )
    found_tid = message['found_tid']
    if found_tid is None:
        raise RuntimeError(
            'Message %r does not have found_tid' % message
        )

    queries_chain(
        context.conn,
        [Q.accidentally_add_thread,
         dict(
             uid=context.uid,
             tid=found_tid,
             revision=context.qs.global_revision(),
             newest_mid=mid,
             newest_date=message['received_date'],
             message_count=1,
             message_seen=int(message['seen']))],
        [Q.accidentally_set_tid_and_newest_tif,
         dict(uid=context.uid, mid=mid, tid=found_tid, newest_tif=True)]
    )


@when(u'we fix_so_label_types')
def step_fix_so_labels(context):
    context.apply_op(FixSoLabelTypes)


def tab_column(column_name, column_value):
    if column_name == 'tab':
        return column_value.strip()
    return null_or_number(column_value)


def set_tabs_columns(conn, uid, tab, column_values):
    columns = []
    values = []
    for c, v in column_values.items():
        columns.append('{0} = %s'.format(c))
        values.append(tab_column(c, v))
    qexec_raw_str(
        conn,
        ''' /* update tab accident */
            UPDATE mail.tabs
               SET {0}
             WHERE uid = %s
               AND tab = %s '''.format('\n,'.join(columns)),
        *(values + [uid, tab])
    )


@when(u'we accidentally set tab "{tab_type:w}" {column} to "{br_value}"')
def step_br_tab(context, tab_type, column, br_value):
    set_tabs_columns(
        context.conn,
        context.uid,
        tab_type,
        {column: br_value})


@when(u'we accidentally set tab "{tab_type:w}" counters')
def step_br_tab_many_columns(context, tab_type):
    assert context.table, 'this step require table'
    column_values = {}
    for row in context.table:
        column_values[row['column']] = row['invalid_value']
    set_tabs_columns(
        context.conn,
        context.uid,
        tab_type,
        column_values)


@when(u'we accidentally break thread "{tid:d}"')
def step_br_thread(context, tid):
    assert context.table, 'this step require table'
    for row in context.table:
        mid = context.res.get_mid(row['mid'])
        msg = context.qs.message(mid=mid)
        assert msg['tid'] == tid, 'Message %r is not in thread %s' % (msg, tid)

        qexec(
            context.conn,
            Q.accidentally_set_newest_tit_and_newest_tif,
            uid=context.uid,
            mid=mid,
            newest_tif=(row.get('newest_tif') or msg['newest_tif']),
            newest_tit=(row.get('newest_tit') or msg['newest_tit']),
        )


def set_counters_columns(conn, uid, column_values):
    columns = []
    values = []
    for c, v in column_values.items():
        columns.append('{0} = %s'.format(c))
        values.append(tab_column(c, v))
    qexec_raw_str(
        conn,
        ''' /* update counters accident */
            UPDATE mail.counters
               SET {0}
             WHERE uid = %s '''.format('\n,'.join(columns)),
        *(values + [uid])
    )


@when(u'we accidentally set attach counter {column} to "{br_value}"')
def step_br_attach_counter_columns(context, column, br_value):
    set_counters_columns(
        context.conn,
        context.uid,
        {column: br_value})


@when(u'we accidentally set attach counters')
def step_br_attach_counters(context):
    assert context.table, 'this step require table'
    column_values = {}
    for row in context.table:
        column_values[row['column']] = row['invalid_value']
    set_counters_columns(
        context.conn,
        context.uid,
        column_values)

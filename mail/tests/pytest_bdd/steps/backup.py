# coding: utf-8

import logging

import pymdb.operations as OPS
import re

from datetime import datetime
from parse_type import TypeBuilder
from pytest_bdd import parsers

from tests_common.pytest_bdd import BehaveParser, given, when, then  # pylint: disable=E0611
from mail.pypg.pypg.common import qexec
from mail.pypg.pypg.query_conf import load_from_my_file
from pymdb.types import BackupMidsMapping, BackupFidsMapping, BackupSettings
from pymdb.queries import BackupDoesNotExist
from .lists import compare_list

from hamcrest import (
    assert_that,
    not_,
    is_,
    has_length,
    equal_to,
    has_property,
    greater_than,
    has_items,
)

Q = load_from_my_file(__file__)

log = logging.getLogger(__name__)


BACKUP_SETTINGS_RE = r'''
(?:
(,| and)?\s*
(?:
(?:folders with types "(?P<folder_types>(([\w-]+[\s,]*)+))")|
(?:folders with names "(?P<folder_names>(([\w-]+[\s,]*)+))")|
(?:tabs "(?P<tabs>(([\w-]+[\s,]*)+))")
)
)+
'''.strip().replace('\n', '')


def parse_names_list(value):
    if not value:
        return []
    return BehaveParser.extra_types['NameList'](value)


def parse_settings_params(context):
    kwargs = context.args
    for k in ['folder_types', 'folder_names', 'tabs']:
        kwargs[k] = parse_names_list(kwargs.get(k))
    return kwargs


def get_fids(context, folder_types=None, folder_names=None):
    all_folders = context.qs.folders()
    by_type = [f.fid for f in all_folders if f.type in (folder_types or [])]
    by_name = [f.fid for f in all_folders if f.name in (folder_names or [])]
    return list(set(by_type + by_name))


def make_set_backup_settings_op(context, folder_types=None, folder_names=None, tabs=None):
    fids = get_fids(context, folder_types, folder_names)
    return OPS.UpdateBackupSettings(context.conn, context.uid)(
        BackupSettings(
            fids=fids or [],
            tabs=tabs or [],
        ))


def set_backup_settings(context):
    kwargs = parse_settings_params(context)
    op = make_set_backup_settings_op(context, **kwargs)
    op.commit()
    context.current_backup_settings = op.result[0]


def reserve_backup_id(context):
    op = OPS.ReserveBackupId(context.conn, context.uid)()
    op.commit()
    context.current_backup_id = op.simple_result


def create_backup_in_state(context, max_messages=10, use_tabs=False, state=None, op_id=None):
    reserve_backup_id(context)
    op = OPS.CreateBackup(context.conn, context.uid)(context.current_backup_id, max_messages, use_tabs)
    if op_id:
        context.operations[op_id] = op
    op.commit()
    if state:
        qexec(
            context.conn,
            Q.update_backup_state,
            uid=context.uid,
            backup_id=context.current_backup_id,
            state=state,
        )
        context.conn.commit()


def create_restore_in_state(context, mapping=None, state=None, method='restored_folder', op_id=None):
    now = datetime.now()
    context.latest_restore_date = now
    backup_id = getattr(context, 'current_backup_id', 0)
    fids_mapping = [BackupFidsMapping(**m) for m in (mapping or [])]

    op = OPS.CreateRestore(context.conn, context.uid)(backup_id, now, method, fids_mapping)
    if op_id:
        context.operations[op_id] = op
    op.commit()
    if state:
        qexec(
            context.conn,
            Q.update_restore_state,
            uid=context.uid,
            backup_id=backup_id,
            created=now,
            state=state,
        )
        context.conn.commit()


def fill_backup(context, use_tabs=False, op_id=None):
    backup_id = getattr(context, 'current_backup_id', 0)
    op = OPS.FillBackup(context.conn, context.uid)(backup_id, use_tabs)
    if op_id:
        context.operations[op_id] = op
    op.commit()


def updated_mid(context, mid):
    new_mid = mid % 1000000
    assert_that(context.res.values(), not_(has_items(new_mid)), 'Already existing new_mid is very unexpected')
    return new_mid


GIVEN_BACKUP_SETTINGS_RE = r'{} are in backup settings'.format(BACKUP_SETTINGS_RE).strip().replace('\n', '')


@given(GIVEN_BACKUP_SETTINGS_RE, parse_builder=parsers.re)
def step_backup_settings(context):
    set_backup_settings(context)


@given('user has backup in "{state:w}" state')
def step_user_has_backup(context, state):
    create_backup_in_state(context, state=state)


@when('user has backup in "{state:w}" state')
def step_when_user_has_backup(context, state):
    create_backup_in_state(context, state=state)


@given('user has filled backup')
def step_user_has_filled_backup(context):
    create_backup_in_state(context)
    fill_backup(context)


@when('user has filled backup')
def when_user_has_filled_backup(context):
    create_backup_in_state(context)
    fill_backup(context)


@given('user has restore in "{state:w}" state')
def step_user_has_restore(context, state):
    create_restore_in_state(context, state=state)


@when('user has restore in "{state:w}" state')
def when_user_has_restore(context, state):
    create_restore_in_state(context, state=state)


SET_BACKUP_SETTINGS_RE = r'we set to backup {}'.format(BACKUP_SETTINGS_RE).strip().replace('\n', '')


@when(SET_BACKUP_SETTINGS_RE, parse_builder=parsers.re)
def step_set_backup_settings(context):
    set_backup_settings(context)


@when('we reserve backup_id')
def step_reserve_backup_id(context):
    reserve_backup_id(context)


@when('we try reserve backup_id as "{op_id:OpID}"')
def step_try_reserve_backup_id(context, op_id):
    context.operations[op_id] = OPS.ReserveBackupId(context.conn, context.uid)()


@when('we create backup with limit "{limit:d}" and with tabs {use_tabs:Enabled} as "{op_id:OpID}"')
def step_create_backup(context, limit, use_tabs, op_id):
    create_backup_in_state(context, max_messages=int(limit), use_tabs=use_tabs, op_id=op_id)


@when('we create backup without reserving backup_id as "{op_id:OpID}"')
def step_create_backup_no_reserve(context, op_id):
    backup_id = context.qs.serials().next_backup_id
    op = OPS.CreateBackup(context.conn, context.uid)(backup_id, 0, False)
    context.operations[op_id] = op
    op.commit()


@when('we create backup with same id with limit "{limit:d}" as "{op_id:OpID}"')
def step_create_backup_same_id(context, limit, op_id):
    op = OPS.CreateBackup(context.conn, context.uid)(context.current_backup_id, limit, False)
    context.operations[op_id] = op
    op.commit()


@when('we fail backup with notice "{notice}"')
def step_fail_backup(context, notice):
    OPS.FailBackup(context.conn, context.uid)(notice).commit()


@when('we deactivate backup as "{op_id:OpID}"')
def step_deactivate_backup(context, op_id):
    op = OPS.DeactivateBackup(context.conn, context.uid)()
    context.operations[op_id] = op
    op.commit()


@when('we fill backup with tabs {use_tabs:Enabled} as "{op_id:OpID}"')
def step_fill_backup(context, use_tabs, op_id):
    fill_backup(context, use_tabs=use_tabs, op_id=op_id)


@when('we create restore as "{op_id:OpID}"')
def step_create_restore(context, op_id):
    create_restore_in_state(context, op_id=op_id)


@when('we create restore as "{op_id:OpID}" for previous backup')
def step_create_restore_for_previous(context, op_id):
    backup_id = getattr(context, 'current_backup_id')
    if backup_id is None:
        raise Exception('empty current_backup_id')

    setattr(context, 'current_backup_id', backup_id - 1)
    create_restore_in_state(context, op_id=op_id)


@when('we create restore with same created as "{op_id:OpID}"')
def step_create_restore_same_created(context, op_id):
    op = OPS.CreateRestore(context.conn, context.uid)(
        context.current_backup_id, context.latest_restore_date, 'restored_folder', [])
    context.operations[op_id] = op
    op.commit()


@when('we create "{method:w}" restore as "{op_id:OpID}" with mapping')
def step_create_restore_with_mapping(context, method, op_id):
    create_restore_in_state(context, mapping=context.table.to_dicts(), method=method, op_id=op_id)


@when('we fail restore with notice "{notice}"')
def step_fail_restore(context, notice):
    OPS.FailRestore(context.conn, context.uid)(notice).commit()


@when('we complete restore')
def step_complete_restore(context):
    OPS.CompleteRestore(context.conn, context.uid)().commit()


@when('we update mids for "{mids:MidsRange}"')
def step_update_mids(context, mids):
    mids_mapping = [
        BackupMidsMapping(original=mid, renewed=updated_mid(context, mid))
        for mid in context.res.get_mids(mids)
    ]
    OPS.RestoreUpdateMids(context.conn, context.uid)(
        context.current_backup_id, mids_mapping).commit()


@then('"{op_id:OpID}" produce "{result}"')
def step_check_result(context, op_id, result):
    assert_that(context.operations[op_id].simple_result, equal_to(result))


@then('"{op_id:OpID}" produce nothing')
def step_check_empty_result(context, op_id):
    assert_that(context.operations[op_id].simple_result, equal_to(None))


THEN_BACKUP_SETTINGS_RE = r'{} are in backup settings'.format(BACKUP_SETTINGS_RE).strip().replace('\n', '')


@then(THEN_BACKUP_SETTINGS_RE, parse_builder=parsers.re)
def step_check_backup_settings(context):
    params = parse_settings_params(context)
    fids = get_fids(context, folder_types=params['folder_types'], folder_names=params['folder_names'])
    tabs = params['tabs']
    assert_that(sorted(context.qs.backup_settings().fids), equal_to(sorted(fids)))
    assert_that(sorted(context.qs.backup_settings().tabs), equal_to(sorted(tabs)))


@then('update returns current backup settings')
def step_check_update_backup_settings_response(context):
    assert_that(context.qs.backup_settings(), equal_to(context.current_backup_settings))


@then('next_backup_id is "{bid:d}"')
def step_check_next_backup_id(context, bid):
    assert_that(context.qs.serials().next_backup_id, equal_to(bid))


@then('reserved backup_id is "{bid:d}"')
def step_check_current_backup_id(context, bid):
    assert_that(context.current_backup_id, equal_to(bid))


@then('reserved backup_ids in "{first_op_id:OpID}" and "{second_op_id:OpID}" are different')
def step_check_reserved_backup_ids(context, first_op_id, second_op_id):
    assert_that(context.operations[first_op_id].simple_result,
                not_(equal_to(context.operations[second_op_id].simple_result)))


@then('user has no backups')
def step_check_no_backups(context):
    assert_that(context.qs.backups(), has_length(0))


RELEVANCE = ['current', 'previous']

BehaveParser.extra_types.update(dict(Relevance=TypeBuilder.make_choice(RELEVANCE)))


def get_backup_id(context, relevance):
    if relevance == 'current':
        return context.current_backup_id
    elif relevance == 'previous':
        return context.current_backup_id - 1
    return None


def check_backup_field(context, relevance, field, value):
    backup_id = get_backup_id(context, relevance)
    backup = context.qs.backup_by_id(backup_id)
    assert_that(backup, has_property(field, equal_to(value)))


def int_value(value):
    m = re.match(r"(\d+)L", value)
    if m is not None:
        return int(m.group(1))
    return value


@when(u'we purge {relevance:Relevance} backup')
def step_purge_old_backup(context, relevance):
    backup_id = get_backup_id(context, relevance)
    OPS.PurgeOldBackup(context.conn, context.uid)(backup_id).commit()


@then('{relevance:Relevance} backup "{field:w}" is "{value}"')
def step_check_backup_field_value(context, relevance, field, value):
    check_backup_field(context, relevance, field, int_value(value))


@then('{relevance:Relevance} backup "{field:w}" is null')
def step_check_backup_field_value_null(context, relevance, field):
    check_backup_field(context, relevance, field, None)


@then('{relevance:Relevance} backup is empty')
def step_check_backup_empty(context, relevance):
    backup_id = get_backup_id(context, relevance)
    assert_that(context.qs.backup_folders_by_id(backup_id), has_length(0))
    assert_that(context.qs.backup_box_by_id(backup_id), has_length(0))
    assert_that(context.qs.restores_by_id(backup_id), has_length(0))


@then('{relevance:Relevance} backup is filled')
def step_check_backup_filled(context, relevance):
    backup_id = get_backup_id(context, relevance)
    assert_that(context.qs.backup_folders_by_id(backup_id), has_length(greater_than(0)))
    assert_that(context.qs.backup_box_by_id(backup_id), has_length(greater_than(0)))


@then('restore for {relevance:Relevance} backup exists')
def step_check_restore_exists(context, relevance):
    backup_id = get_backup_id(context, relevance)
    assert_that(context.qs.restores_by_id(backup_id), has_length(greater_than(0)))


@then('{relevance:Relevance} backup is the only backup')
def step_check_backup_only(context, relevance):
    backup_id = get_backup_id(context, relevance)
    all_backups = context.qs.backups()
    assert_that(all_backups, has_length(1))
    assert_that(all_backups[0].backup_id, equal_to(backup_id))


@then('{relevance:Relevance} backup does not exist')
def step_check_backup_not_exist(context, relevance):
    backup_id = get_backup_id(context, relevance)
    try:
        backup = context.qs.backup_by_id(backup_id)
        assert_that(backup, equal_to(None), 'Backup should not exist')
    except BackupDoesNotExist:
        pass


def get_fid_by_string(context, folder_string):
    if not folder_string.startswith('#'):
        return folder_string
    search_by, value = folder_string.split(':', 1)
    if search_by == '#type':
        return str(context.qs.folder_by_type(value).fid)
    elif search_by == '#name':
        return str(context.qs.folder_by_name(value.replace('/', '|')).fid)
    return folder_string


def resolve_fids(context):
    for row in context.table:
        for col in row.headings:
            if col in ['fid', 'parent_fid']:
                row[col] = get_fid_by_string(context, row[col])


@then('in backup_folders there are')
def step_check_backup_folders(context):
    resolve_fids(context)
    folders = [f.as_dict() for f in context.qs.backup_folders_by_id(context.current_backup_id)]
    count = len(folders)
    compare_list(
        context=context,
        obj_name='folder',
        seq=folders,
        count=count,
        pk='fid',
    )


@then('in backup_box there are messages')
def step_check_backup_box(context):
    resolve_fids(context)
    messages = [m.as_dict() for m in context.qs.backup_box_by_id(context.current_backup_id)]
    count = len(context.table.rows)
    compare_list(
        context=context,
        obj_name='message',
        seq=messages,
        count=count,
        pk='mid',
    )


@then('user has no restores')
def step_check_no_restores(context):
    assert_that(context.qs.restores(), has_length(0))


def check_restore_field(context, field, value):
    all_restores = context.qs.restores_by_id(context.current_backup_id)
    assert_that(all_restores, has_length(greater_than(0)), 'Cannot find restores for current backup')
    latest_restore = all_restores[0]
    assert_that(latest_restore, has_property(field, equal_to(value)))


@then('latest restore "{field:w}" is "{value}"')
def step_check_restore_field_value(context, field, value):
    check_restore_field(context, field, int_value(value))


@then('latest restore "{field:w}" is null')
def step_check_restore_field_value_null(context, field):
    check_restore_field(context, field, None)


@then('latest restore "mapping" is')
def step_check_restore_mapping_value(context):
    expected = [
        BackupFidsMapping(original=int(r['original']), renewed=int(r['renewed']))
        for r in context.table
    ]
    check_restore_field(context, 'fids_mapping', expected)


MIDS_TYPE = ['old', 'new', 'new and old']

BehaveParser.extra_types.update(dict(MidsType=TypeBuilder.make_choice(MIDS_TYPE)))


def check_mid_list_with_expected(context, found_mids, has, mids_type, expected_mids, message=''):
    old_mids = context.res.get_mids(expected_mids)
    new_mids = [updated_mid(context, m) for m in old_mids]
    expected_mids = []
    if mids_type == 'old':
        expected_mids = old_mids
    elif mids_type == 'new':
        expected_mids = new_mids
    elif mids_type == 'new and old':
        expected_mids = old_mids + new_mids

    matcher = is_ if has else not_

    assert_that(found_mids, matcher(has_items(*expected_mids)),
                'check if {0} {1} mids'.format(message, 'has' if has else 'has not'))


@then('backup_box {has:Has} {mids_type:MidsType} mids for "{mids:MidsRange}"')
def step_check_backup_box_mids(context, has, mids_type, mids):
    backup_mids = [b.mid for b in context.qs.backup_box_by_id(context.current_backup_id)]
    check_mid_list_with_expected(context, backup_mids, has, mids_type, mids, 'backup_box')


@then('deleted_box {has:Has} {mids_type:MidsType} mids for "{mids:MidsRange}"')
def step_check_deleted_box(context, has, mids_type, mids):
    deleted_mids = [b.mid for b in context.qs.deleted_messages()]
    check_mid_list_with_expected(context, deleted_mids, has, mids_type, mids, 'deleted_box')

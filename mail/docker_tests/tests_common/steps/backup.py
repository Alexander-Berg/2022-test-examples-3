import pymdb.operations as OPS

from datetime import datetime
from pytest_bdd import parsers

from tests_common.mdb import current_user_connection, Queries
from tests_common.pytest_bdd import BehaveParser, given
from pymdb.types import BackupFidsMapping, BackupSettings


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

GIVEN_BACKUP_SETTINGS_RE = r'{} are in backup settings'.format(BACKUP_SETTINGS_RE)


def parse_names_list(value):
    if not value:
        return []
    return BehaveParser.extra_types['NameList'](value)


def parse_settings_params(context):
    kwargs = context.args
    for k in ['folder_types', 'folder_names', 'tabs']:
        kwargs[k] = parse_names_list(kwargs.get(k))
    return kwargs


def get_fids(conn, uid, folder_types=None, folder_names=None):
    qs = Queries(conn, uid)
    all_folders = qs.folders()
    by_type = [f.fid for f in all_folders if f.type in (folder_types or [])]
    by_name = [f.fid for f in all_folders if f.name in (folder_names or [])]
    return list(set(by_type + by_name))


def make_set_backup_settings_op(context, folder_types=None, folder_names=None, tabs=None):
    uid = context.user.uid
    with current_user_connection(context) as conn:
        fids = get_fids(conn, uid, folder_types, folder_names)
        return OPS.UpdateBackupSettings(conn, uid)(
            BackupSettings(
                fids=fids or [],
                tabs=tabs or [],
            )).commit()


def set_backup_settings(context):
    kwargs = parse_settings_params(context)
    make_set_backup_settings_op(context, **kwargs)


def reserve_backup_id(conn, uid):
    op = OPS.ReserveBackupId(conn, uid)().commit()
    return op.simple_result


def create_backup(context, max_messages=10, use_tabs=False):
    uid = context.user.uid
    with current_user_connection(context) as conn:
        context.current_backup_id = reserve_backup_id(conn, uid)
        OPS.CreateBackup(conn, uid)(context.current_backup_id, max_messages, use_tabs).commit()
        OPS.FillBackup(conn, uid)(context.current_backup_id, use_tabs).commit()


def create_restore(context, mapping=None, method='restored_folder'):
    uid = context.user.uid
    now = datetime.now()
    fids_mapping = [BackupFidsMapping(**m) for m in (mapping or [])]

    with current_user_connection(context) as conn:
        OPS.CreateRestore(conn, uid)(context.current_backup_id, now, method, fids_mapping).commit()


@given(GIVEN_BACKUP_SETTINGS_RE, parse_builder=parsers.re)
def step_backup_settings(context):
    set_backup_settings(context)


@given('user has filled backup')
def step_user_has_filled_backup(context):
    create_backup(context)


@given('user has restore')
def step_user_has_restore(context):
    create_restore(context)

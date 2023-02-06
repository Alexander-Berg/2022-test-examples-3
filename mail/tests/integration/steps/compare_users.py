from tests_common.pytest_bdd import given, then
import pymdb.types
from functools import partial
from ora2pg.compare import check_users_are_equal, AreEqual, is_seq
from ora2pg.sharpei import get_pg_dsn_from_sharpei
from ora2pg.pg_get import get_user
from ora2pg.pg_types import PgUser

from tests_common.mdb import user_connection
from .instant_delete_user import mdb_connection


def get_user_in_memory(db_user):
    user = PgUser()
    for fieldname in PgUser.__slots__:
        field = getattr(db_user, fieldname, None)
        if is_seq(field):
            setattr(user, fieldname, list(field))
        else:
            setattr(user, fieldname, field)
    return user


@given('he has metadata named "{meta_name}" in shard "{shard_id:d}"')
def given_get_metadata_in_shard(context, meta_name, shard_id):
    return step_get_metadata_in_shard(context, meta_name, shard_id)


@then('he has metadata named "{meta_name}" in shard "{shard_id:d}"')
def then_get_metadata_in_shard(context, meta_name, shard_id):
    return step_get_metadata_in_shard(context, meta_name, shard_id)


def step_get_metadata_in_shard(context, meta_name, shard_id):
    with mdb_connection(context, shard_id) as mdb_conn:
        context.metadata[meta_name] = get_user_in_memory(
            get_user(uid=context.user.uid, conn=mdb_conn))
        print(context.metadata[meta_name])


def compare_metadata_except_users(context, meta_name1, meta_name2):
    exc_fields = []
    assert AreEqual(volatile_getter=lambda o: set(exc_fields))(
        l=context.metadata[meta_name1],
        r=context.metadata[meta_name2],
        name='User')


@then('metadata "{meta_name1}" and "{meta_name2}" are equal')
def step_compare_same_mails_except_users(context, meta_name1, meta_name2):
    compare_metadata_except_users(context, meta_name1, meta_name2)


@given('"{user_name:w}" has metadata named "{meta_name}"')
def given_get_metadata(context, user_name, meta_name):
    return step_get_metadata(**locals())


@then('metadata "{meta_name}" user.is_here attribute is {is_true:IsTrue}')
def step_metadata_check_is_here(context, meta_name, is_true):
    assert context.metadata[meta_name].user.is_here == is_true


@then('"{user_name:w}" has metadata named "{meta_name}"')
def then_get_metadata(context, user_name, meta_name):
    return step_get_metadata(**locals())


def step_get_metadata(context, user_name, meta_name):
    user = context.users.get(user_name)
    with user_connection(context, user.uid) as mdb_conn:
        context.metadata[meta_name] = get_user_in_memory(
            get_user(uid=context.user.uid, conn=mdb_conn))


def compare_same_mails_with_exceptions(context, meta_name1, meta_name2, exc_fields):
    def cmpable_item(item):
        if not hasattr(item, 'headers'):
            return 42
        return item.headers.hdr_message_id

    exc_fields += ['mid', 'fid', 'revision', 'thread_rule', 'deleted', 'st_id', 'imap_id']
    assert AreEqual(
        sorter=partial(sorted, key=cmpable_item),
        volatile_getter=lambda o: set(exc_fields),
    )(
        l=context.metadata[meta_name1].mails,
        r=context.metadata[meta_name2].mails,
        name='mails')


@then('metadata "{meta_name1}" and "{meta_name2}" have same mails except stids, lids and seen flags')
def step_compare_same_mails_except_stids(context, meta_name1, meta_name2):
    compare_same_mails_with_exceptions(context, meta_name1, meta_name2, ['st_id', 'lids', 'seen'])


@then('metadata "{meta_name1}" and "{meta_name2}" have same mails except metadata')
def step_compare_same_mails_except_metadata(context, meta_name1, meta_name2):
    exc_fields = ['st_id', 'lids', 'doom_date', 'coords']
    compare_same_mails_with_exceptions(context, meta_name1, meta_name2, exc_fields)


@then('metadata "{meta_name}" contains folder "{folder_name}"')
def step_compare_same_mails(context, meta_name, folder_name):
    assert any(folder.name == folder_name for folder in context.metadata[meta_name].folders)


def get_maildb_dsn(context, uid):
    return get_pg_dsn_from_sharpei(
        sharpei=context.config.sharpei,
        uid=uid,
        dsn_suffix=context.config.maildb_dsn_suffix)


class MarkStIDsAndUserStateAsViolative(object):
    def __enter__(self):
        pymdb.types.User._volatile.append('state')
        pymdb.types.User._volatile.append('notifies_count')
        pymdb.types.MailCoordinates._volatile.append('st_id')
        pymdb.types.DeletedCoords._volatile.append('st_id')

    def __exit__(self, exc_type, exc_val, exc_tb):
        pymdb.types.User._volatile.remove('state')
        pymdb.types.User._volatile.remove('notifies_count')
        pymdb.types.MailCoordinates._volatile.remove('st_id')
        pymdb.types.DeletedCoords._volatile.remove('st_id')


@then(
    '"{first_user_name:w}" has same mails as "{second_user_name:w}" '
    'except st_ids and user state are different')
def step_compare_users(context, first_user_name, second_user_name):
    first_user = context.users.get(first_user_name)
    second_user = context.users.get(second_user_name)

    maildb_dsn = get_maildb_dsn(context, first_user.uid)
    assert maildb_dsn == get_maildb_dsn(context, second_user.uid), \
        'Compare user require user from same shard'

    with MarkStIDsAndUserStateAsViolative():
        assert check_users_are_equal(
            first_uid=first_user.uid,
            second_uid=second_user.uid,
            maildb=maildb_dsn), \
            'Users are not equal first: %r, second: %r' % (
                first_user, second_user)


def get_user_data(context, uid):
    with user_connection(context, uid) as mdb_conn:
        user = get_user_in_memory(get_user(uid=uid, conn=mdb_conn))
        return user


def metadata_structure_volatile(obj):
    if isinstance(obj, PgUser):
        return set(PgUser.__slots__) - set([
            'user',
            'folders',
            'folder_archivation_rules',
            'labels',
            'imap_unsubscribed_folders',
            'filters',
            'filter_elists',
            'counters',
            'serials',
            'fix_log',
            'settings',
        ])
    if isinstance(obj, pymdb.types.Folder):
        return set([
            'message_count',
            'message_seen',
            'message_recent',
            'message_size',
            'first_unseen',
            'first_unseen_id',
            'attach_count',
            'attach_size'])
    if isinstance(obj, pymdb.types.Label):
        return set([
            'message_count',
            'message_seen',
        ])
    return set()


@then('"{user_name1}" has same metadata structure as "{user_name2}"')
def step_has_same_metadata_structure(context, user_name1, user_name2):
    assert AreEqual(volatile_getter=metadata_structure_volatile)(
        l=get_user_data(context, context.users.get(user_name1).uid),
        r=get_user_data(context, context.users.get(user_name2).uid),
        name='User')


@then('"{user_name}" has empty mails, mailish and shared info')
def step_has_empty_mails(context, user_name):
    user = get_user_data(context, context.users.get(user_name).uid)
    for fieldname in ([
        'mails',
        'deleted_mails',
        'message_references',
        'threads_hashes',
        'windat_messages',
        'shared_folders',
        'shared_folder_subscriptions',
        'subscribed_folders',
        'synced_messages',
        'mailish_accounts',
        'mailish_auth_data',
        'mailish_folders',
        'mailish_messages',
    ]):
        assert not getattr(user, fieldname, None)


@then('"{user_name}" has {message_count:d} mails')
def step_has_count_mails(context, user_name, message_count):
    user = get_user_data(context, context.users.get(user_name).uid)
    assert len(user.mails) == message_count


@then('"{user_name}" has empty backups')
def step_has_empty_backups(context, user_name):
    user = get_user_data(context, context.users.get(user_name).uid)
    for fieldname in ([
        'backups',
        'restores',
        'backup_folders',
        'backup_box',
        'folders_to_backup',
        'tabs_to_backup',
    ]):
        assert not getattr(user, fieldname, None)


@then('"{user_name1}" has same metadata as "{user_name2}" except st_ids, state and backups')
def step_has_same_metadata_structure_except_st_ids_and_backups(context, user_name1, user_name2):
    exc_fields = [
        'st_id',
        'state',
        'notifies_count',
        'backups',
        'restores',
        'backup_folders',
        'backup_box',
        'folders_to_backup',
        'tabs_to_backup',
    ]
    assert AreEqual(volatile_getter=lambda o: set(exc_fields))(
        l=get_user_data(context, context.users.get(user_name1).uid),
        r=get_user_data(context, context.users.get(user_name2).uid),
        name='User')

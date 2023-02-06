import json
from contextlib import contextmanager
from datetime import timedelta, datetime

from hamcrest import assert_that, empty, has_length, equal_to, contains_inanyorder, has_entry
from pytest_bdd import parsers

from pymdb.types import BackupSettings
from pymdb import operations as OPS
from pymdb.queries import Queries, fetch_one_row_with_one_element
from pymdb.types import register_types
from pymdb.vegetarian import fill_messages_in_folder
from mail.pypg.pypg.common import transaction, qexec
from mail.pypg.pypg.query_conf import load_from_my_file
from mail.pypg.pypg.query_handler import ExpectOneItemError
from tests_common.pytest_bdd import given, when, then
from .user import create_stids

Q = load_from_my_file(__file__)


def dsn_from_context(context, is_main_shard=True):
    return context.config['maildb'] if is_main_shard else context.config['maildb2']


@contextmanager
def mdb_connection(context, mdb_user=None, is_main_shard=True):
    dsn = dsn_from_context(context, is_main_shard)
    if mdb_user:
        dsn = dsn + ' user=' + mdb_user
    with transaction(dsn) as conn:
        register_types(conn)
        yield conn


@contextmanager
def mdb_queries(context, user):
    with mdb_connection(context) as conn:
        yield Queries(conn, user.uid)


def apply_op(OpClass, conn, user, **op_args):
    o = OpClass(conn, user.uid)
    o(**op_args)
    o.commit()


@given('she moved messages from "{from_folder_type:w}" to "{to_folder_type:w}" "{days:d}" days ago')
def step_move_messages(context, from_folder_type, to_folder_type, days):
    user = context.get_user()
    with mdb_connection(context) as conn:
        qs = Queries(conn, user.uid)
        from_folder = qs.folder_by_type(from_folder_type)
        mids = [m['mid'] for m in qs.messages(fid=from_folder.fid)]
        if not mids:
            raise RuntimeError('There are no messages in %r' % from_folder)
        to_folder = qs.folder_by_type(to_folder_type)
        to_tab = 'relevant' if to_folder.type == 'inbox' else None
        apply_op(OPS.MoveMessages, conn, user, mids=mids, new_fid=to_folder.fid, new_tab=to_tab)
        qexec(
            conn,
            Q.move_doom_date,
            uid=user.uid,
            mids=mids,
            days=timedelta(days))
        qexec(
            conn,
            Q.move_chained_date,
            uid=user.uid,
            mids=mids,
            days=timedelta(days))


@given('he deletes messages from "{from_folder_type:w}" right now')
def step_delete_messages(context, from_folder_type):
    return step_delete_messages_impl(**locals())


@given('he deletes messages from "{from_folder_type:w}" "{days:d}" days ago')
def step_delete_messages_by_days(context, from_folder_type, days):
    return step_delete_messages_impl(**locals())


def step_delete_messages_impl(context, from_folder_type, days=None):
    user = context.get_user()
    with mdb_connection(context) as conn:
        qs = Queries(conn, user.uid)
        from_folder = qs.folder_by_type(from_folder_type)
        mids = [m['mid'] for m in qs.messages(fid=from_folder.fid)]
        if not mids:
            raise RuntimeError('There are no messages in %r' % from_folder)
        apply_op(OPS.DeleteMessages, conn, user, mids=mids)
        if days is not None:
            qexec(
                conn,
                Q.move_deleted_date_in_deleted_box,
                uid=user.uid,
                mids=mids,
                days=timedelta(days)
            )


def get_deleted_messages_for_user(context, user):
    with mdb_queries(context, user) as qs:
        return qs.deleted_messages()


def put_stid_in_storage_delete_queue_manually(conn, uid, stid):
    with conn.cursor() as cur:
        cur.execute("""
            INSERT INTO mail.storage_delete_queue (uid, st_id, deleted_date)
            VALUES ('{}', '{}', now() - interval '10 days')
            """.format(uid, stid))


@when('we put "{message_name:Var}" in storage_delete_queue manually')
def step_put_stid_in_storage_delete_queue_manually(context, message_name):
    user = context.get_user()
    message = step_expect_only_one_message(context.messages[message_name])
    with mdb_connection(context) as conn:
        put_stid_in_storage_delete_queue_manually(conn, user.uid, message['st_id'])


@given('user has filled backup for "{folder_types:w}"')
def step_backup_settings(context, folder_types):
    step_make_backup_impl(**locals())


@given('user has filled backup for "{folder_types:w}" "{days:d}" days ago in "{state}" state')
def step_make_backup_days_ago(context, folder_types, days, state):
    step_make_backup_impl(**locals())


def step_make_backup_impl(context, folder_types, days=None, state=None):
    user = context.get_user()
    with mdb_connection(context) as conn:
        qs = Queries(conn, user.uid)
        all_folders = qs.folders()
        fids = [f.fid for f in all_folders if f.type in (folder_types or [])]
        OPS.UpdateBackupSettings(conn, user.uid)(
            BackupSettings(
                fids=fids or [],
                tabs=[],
            )).commit()
        reserve_backup_id(context, conn, user.uid)
        OPS.CreateBackup(conn, user.uid)(context.current_backup_id, max_messages=10, use_tabs=False).commit()
        OPS.FillBackup(conn, user.uid)(context.current_backup_id, use_tabs=False).commit()
        step_change_backup_impl(context, days, state)


def reserve_backup_id(context, conn, uid):
    op = OPS.ReserveBackupId(conn, uid)()
    op.commit()
    context.current_backup_id = op.simple_result


@given('backup change state to "{state}" "{days:d}" days ago')
def step_change_backup(context, days, state):
    step_change_backup_impl(**locals())


def step_change_backup_impl(context, days, state):
    user = context.get_user()
    with mdb_connection(context) as conn:
        if days is not None:
            qexec(
                conn,
                Q.move_backup_updated_date,
                uid=user.uid,
                backup_id=context.current_backup_id,
                days=timedelta(days)
            )
        if state is not None:
            qexec(
                conn,
                Q.change_backup_state,
                uid=user.uid,
                backup_id=context.current_backup_id,
                state=state
            )


@given('user has restore')
def step_make_restore(context):
    step_make_restore_impl(**locals())


def step_make_restore_impl(context, method='restored_folder'):
    user = context.get_user()
    now = datetime.now()
    with mdb_connection(context) as conn:
        OPS.CreateRestore(conn, user.uid)(context.current_backup_id, now, method, []).commit()


def get_backups(context, user_name):
    user = context.get_user(user_name)
    with mdb_queries(context, user) as qs:
        return qs.backups()


def get_restores(context, user_name):
    user = context.get_user(user_name)
    with mdb_queries(context, user) as qs:
        return qs.restores()


@then('there are {has_some:NoOrSome} backups for "{user_name:w}"')
def step_check_backups_exists(context, user_name, has_some):
    backups = get_backups(context, user_name)
    if has_some:
        assert backups, 'Expect there are some backups for user.'
    else:
        assert not backups, 'Expect there are no backups for user.'


@then('there are {has_some:NoOrSome} restores for "{user_name:w}"')
def step_check_restores_exists(context, user_name, has_some):
    backups = get_restores(context, user_name)
    if has_some:
        assert backups, 'Expect there are some restores for user.'
    else:
        assert not backups, 'Expect there are no restores for user.'


@then('there are {has_some:NoOrSome} "{user_name:w}" deleted messages')
def step_check_deleted_messages(context, has_some, user_name):
    deleted_messages = get_deleted_messages_for_user(
        context, context.get_user(user_name))
    if has_some:
        assert deleted_messages, (
            'Expect that there are some deleted messages, '
            'but nothing found %r' % deleted_messages
        )
    else:
        assert not deleted_messages, (
            'Expect that there are no deleted messages, '
            'but found them: %r' % deleted_messages
        )


def get_storage_delete_queue(context, user):
    with mdb_queries(context, user) as qs:
        return qs.storage_delete_queue()


@then('there are {has_some:NoOrSome} "{user_name:w}" records in storage_delete_queue')
def step_there_are_records_in_storage_delete_queue(context, has_some, user_name):
    user = context.get_user(user_name)
    del_queue = get_storage_delete_queue(context, user)
    if has_some:
        assert del_queue, (
            'Expect there are some messages in '
            'store_delete_queue, noting found')
    else:
        assert not del_queue, (
            'Expect there are no messages in '
            'store_delete_queue, got: %r' % del_queue)


def get_user_folder(context, user_name, folder_type):
    user = context.get_user(user_name)
    with mdb_connection(context) as conn:
        qs = Queries(conn, user.uid)
        return qs.folder_by_type(folder_type)


@then('"{user_name:w}" "{folder_type:w}" folder is {is_empty:Empty}')
def step_check_folder_is_empty_named(context, folder_type, is_empty, user_name):
    return step_check_folder_is_empty_impl(**locals())


@then('her "{folder_type:w}" folder is empty')
def step_check_folder_is_empty(context, folder_type):
    return step_check_folder_is_empty_impl(**locals())


def step_check_folder_is_empty_impl(context, folder_type, is_empty=True, user_name=None):
    folder = get_user_folder(context, user_name, folder_type)
    assert (folder.message_count == 0) == is_empty, \
        'Expect %s folder, got %r' % ('empty' if is_empty else 'not empty', folder)


def get_chained_rec_count(context, user_name):
    user = context.get_user(user_name)
    with mdb_connection(context) as conn:
        qs = Queries(conn, user.uid)
        return qs.chained_rec_count()


@then('"{user_name:w}" chained_log is {is_empty:Empty}')
def step_check_chained_log_isempty(context, is_empty, user_name=None):
    records = get_chained_rec_count(context, user_name)
    assert (records['count'] == 0) == is_empty, \
        'Expect %s chained_log, got %d records' % (
            'empty' if is_empty else 'not empty', records['count'])


@when('she enable pop3 for "{folder_type:w}" folder')
def step_enable_pop3(context, folder_type):
    user = context.get_user()
    with mdb_connection(context) as conn:
        folder = Queries(conn, user.uid).folder_by_type(folder_type)
        apply_op(
            OPS.POP3FoldersEnable,
            conn,
            user,
            fids=[folder.fid]
        )


@then('"{user_name:w}" has pop3 {initialized:Initialized} for "{folder_type:w}"')
def step_pop3_initialized_in_folder(context, user_name, folder_type, initialized):
    folder = get_user_folder(context, user_name, folder_type)
    assert folder.pop3state.initialized == initialized, \
        'Expect %s pop3 for folder got %r' % (
            'initialized' if initialized else 'uninitialized',
            folder)


def set_user_not_here_days(context, user_name, days):
    user = context.get_user(user_name)
    with mdb_connection(context) as conn:
        qexec(
            conn,
            Q.shift_purge_date,
            uid=user.uid,
            days=timedelta(days)
        )


@then('"{user_name:w}" {has:HasOrNot} metadata in our shard')
def step_user_has_metadata_in_our_shard(context, user_name, has):
    user = context.get_user(user_name)
    with mdb_queries(context, user) as qs:
        folders = qs.folders()
        if has:
            assert folders, \
                'User does not have metadata - folders are empty'
        else:
            assert not folders, \
                'User has metadata, folders aren\'t empty %r' % folders


@then('"{group_name:w}" group of the {limit:d} users {has:HasOrNot} metadata in our shard')
def step_group_of_users_has_metadata_in_our_shard(context, group_name, limit, has):
    for id in range(limit):
        user_name = group_name + str(id)
        step_user_has_metadata_in_our_shard(context, user_name, has)


def add_messages_to_context(context, message_name, messages):
    if not hasattr(context, 'messages'):
        context.messages = {}
    context.messages[message_name] = messages


@when('we mark message from "{folder_type:w}" as "{message_name:Var}"')
def step_when_set_message_name(context, folder_type, message_name):
    return step_set_message_name_impl(**locals())


@given('we mark all messages from "{folder_type:w}" as "{message_name:Var}"')
def step_given_set_message_name(context, folder_type, message_name):
    return step_set_message_name_impl(**locals())


def step_set_message_name_impl(context, folder_type, message_name):
    user = context.get_user(None)
    with mdb_connection(context) as conn:
        qs = Queries(conn, user.uid)
        folder = qs.folder_by_type(folder_type)
        messages = qs.messages(fid=folder.fid)
        add_messages_to_context(
            context, message_name, messages
        )


@when('"{message_name:Var}" message was purged "{days:d}" days ago')
@when('"{message_name:Var}" messages was purged "{days:d}" days ago')
def step_purge_message(context, message_name, days):
    mids = []
    stids = []
    for m in context.messages[message_name]:
        mids.append(m['mid'])
        stids.append(m['st_id'])

    user = context.get_user()
    with mdb_connection(context) as conn:
        OPS.DeleteMessages(
            conn, user.uid
        )(mids=mids).commit()
        OPS.PurgeDeletedMessages(conn, user.uid)(mids=mids).commit()
        qexec(
            conn,
            Q.move_deleted_date_in_storage_delete_queue,
            uid=user.uid,
            days=timedelta(days),
            st_ids=stids,
        )


def step_expect_only_one_message(messages):
    if len(messages) > 1:
        raise RuntimeError(
            'This step expect only one message got %d: %r' % (
                len(messages), messages))
    return messages[0]


@given('"{from_message_name:Var}" message has "{to_message_name:Var}" copy in "{folder_type:w}"')
def step_make_message_copy(context, from_message_name, to_message_name, folder_type):
    from_message = step_expect_only_one_message(
        context.messages[from_message_name])
    user = context.get_user()
    with mdb_connection(context) as conn:
        qs = Queries(conn, user.uid)
        folder = qs.folder_by_type(folder_type)
        op = OPS.CopyMessages(
            conn, user.uid
        )
        op(mids=[from_message['mid']], dst_fid=folder.fid)
        op.commit()
        result_mid = op.result[0].mids[0]
        to_message = qs.message(mid=result_mid)
        add_messages_to_context(
            context, to_message_name, [to_message]
        )


@given('there are no tasks in storage delete queue')
def step_clear_storage_delete_queue(context):
    with mdb_connection(context) as conn:
        qexec(conn, Q.clear_storage_delete_queue)


def step_expect_only_one_task(context, message_name):
    message = step_expect_only_one_message(
        context.messages[message_name])
    user = context.get_user()
    with mdb_connection(context) as conn:
        qs = Queries(conn, user.uid)
        found_tasks = [t for t in qs.storage_delete_queue() if t['st_id'] == message['st_id']]
        assert_that(found_tasks, has_length(1))
        return found_tasks[0]


@given('storage delete queue has bad task for message "{message_name:Var}"')
def step_put_stid_in_queue(context, message_name):
    context.execute_steps(u'''
       Given she has "{this}" message in "inbox"
        When "{this}" message was purged "10" days ago
         And we make purge_storage request
        Then shiva responds ok
        And all shiva tasks finished
         And "{this}" message does not exist in storage
         And storage delete queue is empty
    '''.format(this=message_name))
    message = step_expect_only_one_message(
        context.messages[message_name])
    stid_parts = message['st_id'].split('.')
    stid_parts[0] = '0'
    message['st_id'] = '.'.join(stid_parts)
    user = context.get_user()
    with mdb_connection(context) as conn:
        qexec(
            conn,
            Q.add_to_storage_delete_queue,
            uid=user.uid,
            st_id=message['st_id'])
        qexec(
            conn,
            Q.move_deleted_date_in_storage_delete_queue,
            uid=user.uid,
            days=timedelta(10),
            st_ids=[message['st_id']],
        )
    context.current_task = step_expect_only_one_task(context, message_name)


@given('task for message "{message_name:Var}" failed {count:d} times')
def step_set_fails_count_for_task(context, message_name, count):
    message = step_expect_only_one_message(
        context.messages[message_name])
    user = context.get_user()
    with mdb_connection(context) as conn:
        qexec(
            conn,
            Q.set_fails_count_in_storage_delete_queue,
            uid=user.uid,
            st_ids=[message['st_id']],
            count=count)


@then('storage delete queue is empty')
def step_check_storage_delete_queue_is_empty(context):
    user = context.get_user()
    with mdb_connection(context) as conn:
        qs = Queries(conn, user.uid)
        assert_that(qs.storage_delete_queue(), empty())


@then('storage delete queue has tasks for stids "{stids}"')
def step_check_queue_has_tasks_for_stids(context, stids):
    user = context.get_user()
    matchers = [has_entry('st_id', stid) for stid in stids.split(',')]
    with mdb_connection(context) as conn:
        qs = Queries(conn, user.uid)
        found_tasks = qs.storage_delete_queue()
        assert_that(found_tasks, contains_inanyorder(*matchers))
        return found_tasks[0]


@then('storage delete queue has task for "{message_name:Var}" message')
def step_check_queue_has_task(context, message_name):
    step_expect_only_one_task(context, message_name)


@then('storage delete queue has task for "{message_name:Var}" message '
      'with "{column}" increased by {delta:d}')
def step_check_queue_has_task_with_params(context, message_name, column, delta):
    task = step_expect_only_one_task(context, message_name)
    diff = task[column] - context.current_task[column]
    assert_that(diff, equal_to(type(diff)(delta)))


@given('he received one message in "{folder_type:w}" "{days:d}" days ago')
def step_given_put_messages_with_date(context, folder_type, days):
    return step_put_messages_with_date_impl(**locals())


@when('he received one message in "{folder_type:w}" "{days:d}" days ago')
def step_when_put_messages_with_date(context, folder_type, days):
    return step_put_messages_with_date_impl(**locals())


@given('he received "{limit:d}" messages in "{folder_type:w}" "{days:d}" days ago')
def step_put_messages_with_date_limit(context, folder_type, days, limit):
    return step_put_messages_with_date_impl(**locals())


def step_put_messages_with_date_impl(context, folder_type, days, limit=1):
    context.execute_steps(u'''
        When she has "{limit}" messages in "{folder_type}"
    '''.format(
        limit=limit,
        folder_type=folder_type,
    ))
    user = context.get_user()
    with mdb_connection(context) as conn:
        qs = Queries(conn, user.uid)
        folder = qs.folder_by_type(folder_type)
        mids = [m['mid'] for m in qs.messages(fid=folder.fid)]
        if not mids:
            raise RuntimeError('There are no messages in %r' % folder)
        qexec(
            conn,
            Q.move_received_date,
            uid=user.uid,
            mids=mids,
            days=timedelta(days))


@given('he received a message with mid "{mid_name:w}" in "{folder_type:w}" "{days:d}" days ago')
def step_given_put_message_with_date(context, folder_type, mid_name, days):
    return step_put_message_with_date_impl(**locals())


@when('he received a message with mid "{mid_name:w}" in "{folder_type:w}" "{days:d}" days ago')
def step_put_message_with_date(context, folder_type, mid_name, days):
    return step_put_message_with_date_impl(**locals())


def step_put_message_with_date_impl(context, folder_type, mid_name, days):
    limit = 1
    user = context.get_user()
    with transaction(context.config['maildb']) as conn:
        qs = Queries(conn, user.uid)
        folder = qs.folder_by_type(folder_type)
        mids = fill_messages_in_folder(
            conn=conn,
            uid=user.uid,
            folder=folder,
            limit=limit,
            stids=create_stids(
                context.config,
                user.suid,
                limit,
            )
        )

        qexec(
            conn,
            Q.move_received_date,
            uid=user.uid,
            mids=mids,
            days=timedelta(days))
        if not hasattr(user, 'mid'):
            user.mid = {}
        user.mid[mid_name] = mids[0]


def get_message_in_folder(qs, folder, mid):
    try:
        message = qs.message(mid=mid)
        if message['fid'] == folder.fid:
            return message
    except ExpectOneItemError:
        return None


@then('"{folder_type:w}" contains messages with mids "{mid_names:OneOrMore}"')
def step_contains_message(context, folder_type, mid_names):
    user = context.get_user()
    with mdb_connection(context) as conn:
        qs = Queries(conn, user.uid)
        for mid_name in mid_names:
            mid = user.mid[mid_name]
            folder = qs.folder_by_type(folder_type)
            msg = get_message_in_folder(qs, folder, mid)
            assert msg is not None, \
                "Expect message with mid %s in folder %s" % (mid, folder_type)


@then('"{folder_type:w}" does not contain messages with mids "{mid_names:OneOrMore}"')
def step_does_not_contain_message(context, folder_type, mid_names):
    user = context.get_user()
    with mdb_connection(context) as conn:
        qs = Queries(conn, user.uid)
        for mid_name in mid_names:
            mid = user.mid[mid_name]
            folder = qs.folder_by_type(folder_type)
            msg = get_message_in_folder(qs, folder, mid)
            assert msg is None, \
                "Expect no message with mid %s in folder %s" % (mid, folder_type)


@given('he has "{rule_type:RuleType}" rule for "{folder_type:w}" with "{days:d}" days ttl')
def step_set_archivation_rule(context, rule_type, folder_type, days):
    user = context.get_user()
    with mdb_connection(context) as conn:
        qs = Queries(conn, user.uid)
        folder = qs.folder_by_type(folder_type)
        qexec(
            conn,
            Q.set_archivation_rule,
            uid=user.uid,
            fid=folder.fid,
            rule_type=rule_type,
            days=days,
            max_size=None)


@given('he has "{rule_type:RuleType}" rule for "{folder_type:w}" with "{max_size:d}" max message count')
def step_set_max_size_rule(context, rule_type, folder_type, max_size):
    user = context.get_user()
    with mdb_connection(context) as conn:
        qs = Queries(conn, user.uid)
        folder = qs.folder_by_type(folder_type)
        qexec(
            conn,
            Q.set_archivation_rule,
            uid=user.uid,
            fid=folder.fid,
            rule_type=rule_type,
            days=365,
            max_size=max_size)


@given('"{user_name:w}" "{folder_type:w}" is shared')
def step_make_shared(context, user_name, folder_type):
    user = context.get_user(user_name)
    with mdb_connection(context) as conn:
        qs = Queries(conn, user.uid)
        folder = qs.folder_by_type(folder_type)
        qexec(
            conn,
            Q.make_shared_folder,
            uid=user.uid,
            fid=folder.fid)


@then('"{user_name:w}" "{folder_type:w}" has no subfolders')
def step_check_subfolder(context, user_name, folder_type):
    user = context.get_user(user_name)
    with mdb_connection(context) as conn:
        qs = Queries(conn, user.uid)
        folder = qs.folder_by_type(folder_type)
        subfolders = [f for f in qs.folders() if f.parent_fid == folder.fid]
        assert (len(subfolders) == 0), \
            'Expected empty subfolders, but got %r' % subfolders


def get_year_named_subfolder(qs, folder_type):
    folder = qs.folder_by_type(folder_type)
    return qs.folder_by_attribute('parent_fid', folder.fid)


@then(
    '"{user_name:w}" "{folder_type:w}" has year-named {shared:IsShared?}subfolder with messages',
    parse_builder=parsers.cfparse
)
def step_check_year_subfolder(context, user_name, folder_type, shared):
    user = context.get_user(user_name)
    with mdb_connection(context) as conn:
        qs = Queries(conn, user.uid)
        subfolder = get_year_named_subfolder(qs, folder_type)
        if shared:
            shared_fids = [sf.fid for sf in qs.shared_folders()]
            assert (subfolder.fid in shared_fids), \
                'Expected shared subfolder, but folder %r is not shared' % subfolder
        messages = qs.messages(fid=subfolder.fid)
        for m in messages:
            assert subfolder.name == str(m['received_date'].year), \
                'Expected folder named by message received date, ' \
                'but got folder %r with message %r' % (subfolder, m)


@then('"{folder_type:w}" year-named subfolder contains messages with mids "{mid_names:OneOrMore}"')
def step_year_subfolder_contains_message(context, folder_type, mid_names):
    user = context.get_user()
    with mdb_connection(context) as conn:
        qs = Queries(conn, user.uid)
        for mid_name in mid_names:
            mid = user.mid[mid_name]
            folder = get_year_named_subfolder(qs, folder_type)
            msg = get_message_in_folder(qs, folder, mid)
            assert msg is not None, \
                "Expect message with mid %s in folder %s" % (mid, folder_type)


@given('there are no active users in shard')
def step_clear_users(context):
    with mdb_connection(context) as conn:
        qexec(conn, Q.clean_users)


@given('"{user_name:w}" statistics was updated "{days:d}" days ago')
def step_shift_stats_update_date(context, user_name, days):
    user = context.get_user(user_name)
    with mdb_connection(context) as conn:
        qexec(
            conn,
            Q.shift_stats_update_date,
            uid=user.uid,
            days=timedelta(days))


@then('"{user_name:w}" {has:HasOrNot} fresh mailbox statistics')
def step_check_fresh_stats(context, user_name, has):
    user = context.get_user(user_name)
    with mdb_connection(context) as conn:
        cur = qexec(
            conn,
            Q.get_fresh_stats_count,
            uid=user.uid)
        fresh_stats_count = fetch_one_row_with_one_element(cur)

        if has:
            assert fresh_stats_count, 'Expect there are fresh statistics for user.'
        else:
            assert not fresh_stats_count, 'Expect there are no fresh statistics for user.'


@given('sharpei creates prepared transaction for "{user_name:w}" in maildb')
def step_create_prepared_transaction(context, user_name):
    user = context.get_user(user_name)
    transaction_id = 'reg_mdb_u{uid}_s{shard_id}_sessionId'.format(
        uid=user.uid,
        shard_id=context.config['shard_id'])
    with mdb_connection(context, 'sharpei') as conn:
        cur = conn.cursor()
        cur.execute('BEGIN')
        cur.execute("SELECT code.register_user(%s, '', '', false)" % user.uid)
        cur.execute("PREPARE TRANSACTION '%s'" % transaction_id)


@then('there are no prepared transaction in maildb')
def step_check_prepared_transaction(context):
    with mdb_connection(context) as conn:
        cur = qexec(
            conn,
            Q.get_prepared_transaction_count)
        prepared_transaction_count = fetch_one_row_with_one_element(cur)
        assert not prepared_transaction_count, 'Expect there are no prepared transaction in maildb.'


def get_partitions(conn, schema_name, table_name):
    with conn.cursor() as cur:
        cur.execute("""
            SELECT tablename from pg_tables where schemaname='{}'
            AND tablename like '{}_p%'
            """.format(schema_name, table_name))
        return sorted([row[0] for row in cur.fetchall()])


def remove_partition(conn, schema_name, table_name):
    with conn.cursor() as cur:
        cur.execute("DROP TABLE {}.{}".format(schema_name, table_name))


def step_some_partitions(context, conn, table):
    schema_name, table_name = table.split('.')
    partitions = get_partitions(conn, schema_name, table_name)
    context.partitions = {}
    context.partitions[table] = partitions


def step_drop_partitions(conn, table, count):
    schema_name, table_name = table.split('.')
    partitions = get_partitions(conn, schema_name, table_name)
    for partition_name in partitions[-count:]:
        remove_partition(conn, schema_name, partition_name)


def step_check_partitions(context, conn, table):
    schema_name, table_name = table.split('.')
    partitions = get_partitions(conn, schema_name, table_name)
    assert set(context.partitions[table]).issubset(set(partitions)), 'Expect there are {expected} partitions, but got {real}'.format(
        expected=context.partitions[table],
        real=partitions
    )


@given('there are some partitions for "{table}" in maildb')
def step_some_partitions_in_maildb(context, table):
    with mdb_connection(context) as conn:
        step_some_partitions(context, conn, table)


@when('we drop "{count:d}" partitions for "{table}" in maildb')
def step_drop_partitions_in_maildb(context, table, count):
    with mdb_connection(context) as conn:
        step_drop_partitions(conn, table, count)


@then('there are the same partitions for "{table}" in maildb')
def step_check_partitions_in_maildb(context, table):
    with mdb_connection(context) as conn:
        step_check_partitions(context, conn, table)


@given('his messages have synced attribute')
def step_add_synced_attribute_to_messages(context):
    user = context.get_user()
    with mdb_connection(context) as conn:
        qexec(
            conn,
            Q.add_synced_attribute_to_messages,
            uid=user.uid,
        )


@given('his change_log is empty')
def step_clean_user_change_log(context):
    user = context.get_user()
    with mdb_connection(context) as conn:
        qexec(
            conn,
            Q.clean_user_change_log,
            uid=user.uid,
        )


@given('his change_log is non empty')
def step_fill_user_change_log(context):
    user = context.get_user()
    with mdb_connection(context) as conn:
        qexec(
            conn,
            Q.fill_user_change_log,
            uid=user.uid,
        )


def create_settings(context, user_name=None, settings={}, signs_count=0):
    user = context.users[user_name] if user_name else context.get_user()
    signs = [dict(text='sing')] * signs_count
    settings_with_signs = dict(single_settings=settings, signs=signs)
    with mdb_connection(context) as conn:
        apply_op(OPS.CreateSettings, conn, user, settings=json.dumps(settings_with_signs))


@given('"{user_name:w}" has setting "{setting:w}" with value "{value}"')
def step_user_has_settings(context, user_name, setting, value):
    create_settings(context, user_name, settings={setting: value})


@given('"{user_name:w}" has enabled admin_search')
def step_enabled_admin_search(context, user_name):
    settings = {'mail_b2b_admin_search_allowed': 'on', 'mail_b2b_admin_search_enabled': 'on'}
    create_settings(context, user_name, settings=settings)


@given('"{user_name:w}" has {signs_count:d} signs in settings')
def step_user_has_signs(context, user_name, signs_count):
    create_settings(context, user_name, signs_count=signs_count)


@given('"{user_name:w}" updates setting "{setting:w}" with value "{value}"')
def step_user_modify_settings(context, user_name, setting, value):
    user = context.users[user_name] if user_name else context.get_user()
    with mdb_connection(context) as conn:
        apply_op(OPS.UpdateSettings, conn, user, settings=json.dumps(dict(single_settings={setting: value})))


@given('he is in "{state_name}" state "{days:d}" days')
def step_given_user_is_in_state(context, state_name, days):
    user = context.get_user()
    with mdb_connection(context) as conn:
        qexec(
            conn,
            Q.set_user_state,
            uid=user.uid,
            state=state_name,
            days=timedelta(days),
        )


@given('he is in "{state_name}" archivation state "{days:d}" days')
def step_given_user_is_in_archivation_state(context, state_name, days):
    user = context.get_user()
    with mdb_connection(context) as conn:
        qexec(
            conn,
            Q.set_user_archivation_state,
            uid=user.uid,
            state=state_name,
            days=timedelta(days),
        )


@given('his archive has "{message_count}" messages')
def step_given_his_archive_is_empty(context, message_count):
    user = context.get_user()
    with mdb_connection(context) as conn:
        qexec(
            conn,
            Q.set_user_archive_message_count,
            uid=user.uid,
            message_count=message_count,
        )


@then('"{user_name:w}" has no archivation data')
def step_then_no_archivation_data(context, user_name):
    user = context.get_user(user_name)
    with mdb_connection(context) as conn:
        cur = qexec(conn, Q.get_user_archivation_state, uid=user.uid)
        assert cur.fetchone() is None


@then('"{user_name:w}" has empty archive')
def step_then_user_has_empty_archive(context, user_name):
    user = context.get_user(user_name)
    with mdb_connection(context) as conn:
        cur = qexec(conn, Q.get_user_archive_message_count, uid=user.uid)
        assert fetch_one_row_with_one_element(cur) == 0


def get_user_is_here(conn, user):
    cur = qexec(
        conn,
        Q.get_user_is_here,
        uid=user.uid)
    return fetch_one_row_with_one_element(cur)


def set_user_is_here(conn, user, is_here):
    qexec(
        conn,
        Q.set_user_is_here,
        uid=user.uid,
        is_here=is_here,
    )


@then('"{user_name:w}" {has:HasOrNot} record in {is_main:IsMain} shard')
def step_user_has_record(context, user_name, has, is_main):
    user = context.get_user(user_name)
    with mdb_connection(context, is_main_shard=is_main) as conn:
        is_here = qexec(
            conn,
            Q.get_user_is_here,
            uid=user.uid).fetchone()
        if has:
            assert is_here is not None, \
                'User does not have record'
        else:
            assert is_here is None, \
                'User has record, it is not empty, is_here = %r' % is_here


@then('"{user_name:w}" is_here attribute in {is_main:IsMain} shard is {is_true:IsTrue}')
def step_check_user_is_here_attribute(context, user_name, is_main, is_true):
    user = context.get_user(user_name)
    with mdb_connection(context, is_main_shard=is_main) as conn:
        assert get_user_is_here(conn, user) == is_true


def get_user_state(conn, user):
    cur = qexec(
        conn,
        Q.get_user_state,
        uid=user.uid)
    return fetch_one_row_with_one_element(cur)


@then('"{user_name:w}" is in "{state_name}" state')
def step_then_user_is_in_state(context, user_name, state_name):
    user = context.get_user(user_name)
    with mdb_connection(context) as conn:
        assert get_user_state(conn, user) == state_name


@then('"{user_name:w}" in {is_main:IsMain} shard is in "{state_name}" state')
def step_check_user_in_given_shard_is_in_state(context, user_name, is_main, state_name):
    user = context.get_user(user_name)
    with mdb_connection(context, is_main_shard=is_main) as conn:
        assert get_user_state(conn, user) == state_name


def get_user_archivation_state(conn, user):
    cur = qexec(
        conn,
        Q.get_user_archivation_state,
        uid=user.uid)
    return fetch_one_row_with_one_element(cur)


@then('"{user_name:w}" in {is_main:IsMain} shard is in "{state_name}" archivation state')
def step_check_user_in_given_shard_is_in_archivation_state(context, user_name, is_main, state_name):
    user = context.get_user(user_name)
    with mdb_connection(context, is_main_shard=is_main) as conn:
        assert get_user_archivation_state(conn, user) == state_name


@given('he is in "{state_name}" state "{days:d}" days and has "{notifies_count:d}" notifies')
def step_given_user_is_in_state_with_notifies_count(context, state_name, days, notifies_count):
    user = context.get_user()
    with mdb_connection(context) as conn:
        qexec(
            conn,
            Q.set_user_state_and_notifies_count,
            uid=user.uid,
            state=state_name,
            days=timedelta(days),
            notifies_count=notifies_count,
        )


@then('"{user_name:w}" has "{notifies_count:d}" notifies')
def step_then_user_has_notifies(context, user_name, notifies_count):
    user = context.get_user(user_name)
    with mdb_connection(context) as conn:
        cur = qexec(
            conn,
            Q.get_user_notifies_count,
            uid=user.uid)
        real_notifies_count = fetch_one_row_with_one_element(cur)

        assert real_notifies_count == notifies_count


@then('"{user_name:w}" is in "{state_name}" state with "{notifies_count:d}" notifies')
def step_then_user_is_in_state_with_notifies(context, user_name, state_name, notifies_count):
    context.execute_steps(u'''
        Then "{user_name}" is in "{state_name}" state
        And "{user_name}" has "{notifies_count}" notifies
    '''.format(
        user_name=user_name,
        state_name=state_name,
        notifies_count=notifies_count,
    ))


@given(u'he is in shard since "{here_since}"')
def step_given_user_is_in_shard_since(context, here_since):
    user = context.get_user()
    with mdb_connection(context) as conn:
        qexec(
            conn,
            Q.set_user_here_since,
            uid=user.uid,
            here_since=here_since,
        )


@given('she has folder "{folder_name:w}" with type "{folder_type:w}"')
def step_has_folder(context, folder_name, folder_type):
    user = context.get_user()
    with mdb_connection(context) as conn:
        apply_op(OPS.GetOrCreateFolder, conn, user, name=folder_name, type=folder_type)

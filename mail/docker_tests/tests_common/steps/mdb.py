# coding: utf-8

import datetime
import dateutil.parser

import pytz

from itertools import count
from hamcrest import (
    assert_that,
    has_item,
    has_items,
    has_entries,
    all_of,
    not_,
    anything,
)

from pymdb.operations import (
    POP3FoldersEnable,
    InitializePOP3Folder,
    POP3Delete,
    DeleteMessages,
    CreateCollector,
    UpdateMessages,
    MoveMessages,
    CreateLabel,
    CreateFolder,
    CreateReplyLaterSticker,
)
from pymdb.tools import mark_user_as_moved_from_here
from pymdb.types import Folder, Pop3State
from mail.pypg.pypg.common import qexec
from mail.pypg.pypg.query_conf import load_from_package
from tests_common.mdb import (
    current_user_connection,
    current_user_mdb_queries,
    make_maildb_conn,
    Queries,
)
from tests_common.pytest_bdd import given, when, then
from pymdb.vegetarian import (
    fill_messages_in_folder,
    SAMPLE_STIDS,
)

QUERIES = load_from_package(__package__, __file__)


def get_half_mids_from_fid(maildb_queries, fid):
    all_inbox_mids = [
        m['mid']
        for m in maildb_queries.messages(fid=fid)
    ]
    return all_inbox_mids[:int(len(all_inbox_mids)/2)]


@given('he has POP3 meta in mdb')
def step_enable_and_initizlize_pop3(context):
    uid = context.user.uid
    with make_maildb_conn(
        uid=uid,
        sharpei=context.config.sharpei,
        maildb_dsn_suffix='',
    ) as conn:
        maildb_queries = Queries(conn, uid)
        inbox_fid = maildb_queries.folder_by_type(Folder.INBOX).fid
        POP3FoldersEnable(conn, uid)(fids=[inbox_fid])
        InitializePOP3Folder(conn, uid)(fid=inbox_fid)
        POP3Delete(conn, uid)(mids=get_half_mids_from_fid(maildb_queries, inbox_fid))


def check_inbox_pop3_state(context, expected_state):
    with current_user_mdb_queries(context) as maildb_queries:
        inbox = maildb_queries.folder_by_type(Folder.INBOX)

        assert inbox.pop3state == expected_state, 'Expect %r state on inbox, got %r' % (expected_state, inbox.pop3state)


@then('user has POP3 meta in mdb')
def step_check_has_pop3_enabled_and_initialized(context):
    check_inbox_pop3_state(
        context,
        Pop3State(enabled=True, initialized=True))


@then('user has not POP3 meta in mdb')
def step_check_has_not_pop3_enabled_and_initialized(context):
    check_inbox_pop3_state(
        context,
        Pop3State(enabled=False, initialized=False))


@then(u'in changelog there are {change_types:QuotedWords} changes')
@then(u'in changelog there is {change_types:QuotedWords} change')
def step_changes_in_changelog(context, change_types):
    change_types = set(change_types)
    with current_user_mdb_queries(context) as maildb_queries:
        changelog = maildb_queries.changelog()
        assert changelog, 'Changelog is empty!'
        all_change_types = set([c['type'] for c in changelog])
        assert set(all_change_types) == change_types, \
            'Expect only %s, got %r change types' % (
                change_types, set(all_change_types))


@given('he delete some messages from inbox')
def step_delete_messages_from_inbox(context):
    with current_user_connection(context) as maildb_conn:
        maildb_queries = Queries(maildb_conn, context.user.uid)
        inbox_fid = maildb_queries.folder_by_type(Folder.INBOX).fid

        DeleteMessages(maildb_conn, context.user.uid)(
            mids=get_half_mids_from_fid(maildb_queries, inbox_fid)
        ).commit()


@when('he has collectors')
def step_create_collectors(context):
    uid = context.user.uid
    with current_user_connection(context) as conn:
        CreateCollector(conn, uid)(src_uid=context.get_free_uid(), auth_token="fake_oauth_token")


@when('we mark him as not is_here')
def step_mark_user_as_not_here(context):
    with current_user_connection(context) as maidb_conn:
        mark_user_as_moved_from_here(maidb_conn, context.user.uid)


@given('"{user_name:w}" was inited "{days:d}" days ago')
def step_user_was_inited_some_time_ago(context, user_name, days):
    user = context.users.get(user_name)
    with make_maildb_conn(
        uid=user.uid,
        sharpei=context.config.sharpei,
        maildb_dsn_suffix=context.config.maildb_dsn_suffix,
    ) as conn:
        qexec(
            conn,
            QUERIES.set_inbox_create,
            cr=datetime.datetime.now(pytz.utc) - datetime.timedelta(days=days),
            uid=user.uid
        )


def step_store_messages_impl(context, limit=1, folder_type='inbox'):
    user = context.user
    context.stids_generator = (['{st_id}0000{i}'.format(st_id=st_id, i=i) for st_id in SAMPLE_STIDS] for i in count())
    with current_user_connection(context) as conn:
        folder = Queries(conn, user.uid).folder_by_type(folder_type)
        context.mids = fill_messages_in_folder(
            conn=conn,
            uid=user.uid,
            folder=folder,
            limit=limit,
            stids=next(context.stids_generator),
        )
    return context.mids


def create_label(context, type, name):
    uid = context.user.uid
    with current_user_connection(context) as conn:
        op = CreateLabel(conn, uid)(
            name=name,
            type=type,
            color=None,
        ).commit()
    return op.result[0].lid


def create_so_label(context, so_type):
    return create_label(context, name=so_type, type='type')


@given('user has SO labels')
def step_user_has_so_label(context):
    lids = {}
    for row in context.table:
        lids[row['lid']] = create_so_label(context, row['so_type'])
    context.lids = lids


@given('user has labels')
def step_user_has_labels(context):
    lids = {}
    for row in context.table:
        lids[row['lid']] = create_label(context, name=row['name'], type=row['type'])
    context.lids = lids


def store_messages(context, folder_type, messages):
    mids = step_store_messages_impl(context, limit=len(messages), folder_type=folder_type)
    with current_user_connection(context) as conn:
        UpdateMessages(conn, context.user.uid)(
            mids=mids,
            seen=None, recent=None, deleted=None,
            lids_del=list(context.lids.values())
        )
    return dict(zip([r['mid'] for r in messages], mids))


def clear_mailbox(context):
    uid = context.user.uid
    with current_user_connection(context) as conn:
        qs = Queries(conn, uid)
        for folder in qs.folders():
            mids = [m['mid'] for m in qs.messages(fid=folder.fid)]
            if len(mids) > 0:
                DeleteMessages(conn, uid)(mids)


def get_lids(context, labels):
    return list(filter(None, (context.lids.get(l.strip()) for l in labels.split(','))))


def update_meta(context):
    uid = context.user.uid
    with current_user_connection(context) as conn:
        for row in context.table:
            dst_tab = row.get('tab') or None
            update_mid = context.stored_mids[row['mid']]
            MoveMessages(conn, uid)(
                mids=[update_mid],
                new_fid=Queries(conn, uid).message(mid=update_mid)['fid'],
                new_tab=dst_tab,
            )

            lids = get_lids(context, row.get('lids', ''))
            if len(lids) > 0:
                UpdateMessages(conn, uid)(
                    mids=[context.stored_mids[row['mid']]],
                    seen=None, recent=None, deleted=None,
                    lids_add=lids
                )

            new_date = row.get('received_date')
            if new_date:
                qexec(
                    conn,
                    QUERIES.set_received_date,
                    uid=uid,
                    mid=context.stored_mids[row['mid']],
                    received_date=dateutil.parser.parse(new_date)
                )


@given('user has messages in "{folder_type:w}"')
def step_store_user_messages_in_folder(context, folder_type):
    clear_mailbox(context)
    context.stored_mids = store_messages(context, folder_type, context.table.rows)
    update_meta(context)


@given('user has messages')
def step_store_user_messages(context):
    clear_mailbox(context)
    messages_by_folder = {}
    for row in context.table:
        if row['folder'] in messages_by_folder:
            messages_by_folder[row['folder']] += [row]
        else:
            messages_by_folder[row['folder']] = [row]
    context.stored_mids = {}
    for folder, messages in messages_by_folder.items():
        context.stored_mids.update(store_messages(context, folder, messages))
    update_meta(context)


@then('user has messages in "{folder_type:w}"')
def step_user_has_messages(context, folder_type):
    uid = context.user.uid
    with current_user_connection(context) as conn:
        qs = Queries(conn, uid)
        folder = qs.folder_by_type(folder_type)
        messages = qs.messages(fid=folder.fid)
    for row in context.table:
        expected = {}
        for k, v in row.items():
            if k == 'mid':
                expected[k] = context.stored_mids[v]
            elif k == 'lids':
                expected_lids = set(get_lids(context, v))
                has_expected_lids = has_items(*expected_lids) if expected_lids else anything()
                unexpected_lids = set(context.lids.values()) - expected_lids
                has_unexpected_lids = has_items(*unexpected_lids) if unexpected_lids else not_(anything())
                expected[k] = all_of(has_expected_lids, not_(has_unexpected_lids))
            else:
                expected[k] = v or None
        assert_that(messages, has_item(has_entries(expected)))


@given('user has folder "{folder_name:w}" with symbol "{symbol:w}"')
def step_create_folder(context, folder_name, symbol):
    uid = context.user.uid
    with current_user_connection(context) as conn:
        CreateFolder(conn, uid)(
            name=folder_name,
            type=symbol,
        ).commit()


@given('user has reply later stickers for messages')
def step_create_reply_later_stickers_for_messages(context):
    uid = context.user.uid
    with current_user_connection(context) as conn:
        qs = Queries(conn, uid)
        folder = qs.folder_by_type('reply_later')
        for row in context.table:
            CreateReplyLaterSticker(conn, uid)(
                mid=context.stored_mids[row['mid']],
                fid=folder.fid,
                date=datetime.datetime.now(),
                tab=row.get('tab') or None
            ).commit()

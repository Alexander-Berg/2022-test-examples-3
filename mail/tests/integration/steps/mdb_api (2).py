# coding:utf-8
from json import loads
from mail.pypg.pypg.common import qexec
from mail.york.tests.integration.lib.mdb_api import QUERIES as Q
from pymdb.queries import Queries
from pymdb.operations import DeleteMessages
from pymdb.vegetarian import fill_messages_in_folder, SAMPLE_STIDS
from tests_common.mdb import (
    user_connection,
    user_mdb_queries,
)
from behave import (
    given,
    when,
)


def create_folder(context, uid, name, parent_fid):
    with user_connection(context, uid) as conn:
        create_params = dict(
            uid=uid,
            name=name,
            parent_fid=parent_fid,
        )
        cur = qexec(
            conn, Q.create_folder,
            **create_params
        )
        fid = cur.fetchone()[0]
        return fid


def create_shared_folder(context, uid, name, parent_fid=None):
    fid = create_folder(context, uid, name, parent_fid)
    with user_connection(context, uid) as conn:
        add_params = dict(
            uid=uid,
            fid=fid,
        )
        qexec(
            conn, Q.add_shared_folder,
            **add_params
        )
        return fid


@given(u'message was stored in subscribed folder')
@given(u'message was stored in "{subscribed_folder_name}"')
def step_message_was_stored(context, subscribed_folder_name=None):
    uid = context.subscriber_uid
    if subscribed_folder_name is None:
        subscribed_folder_name = loads(context.payload['destination_folder_path'])[-1]
    with user_connection(context, uid) as conn:
        folder = Queries(conn, uid).folder_by_name(subscribed_folder_name)
        fill_messages_in_folder(conn, uid, folder, 1, SAMPLE_STIDS)


@given(u'subscription was deleted from DB')
def step_subscription_was_deleted(context):
    uid = context.owner_uid
    shared_fids = context.shared_folders_parents.keys()
    with user_connection(context, uid) as conn:
        for fid in shared_fids:
            delete_param = dict(
                uid=uid,
                fid=fid,
            )
            qexec(
                conn, Q.remove_subscription,
                **delete_param
            )


@given(u'message was synced to subscribed folder')
@given(u'{msg_count:d} messages was synced to subscribed folder')
def step_messages_synced(context, msg_count=1):
    uid = context.subscriber_uid
    folder_name = loads(context.payload['destination_folder_path'])[-1]
    with user_connection(context, uid) as conn:
        folder = Queries(conn, uid).folder_by_name(folder_name)
        while msg_count > 0:
            mcount = min(10, msg_count)
            mids = fill_messages_in_folder(conn, uid, folder, mcount, SAMPLE_STIDS)
            with user_mdb_queries(context, context.owner_uid) as q:
                subs = q.shared_folder_subscriptions()
                subscription_id = next((s.subscription_id for s in subs if s.fid == context.shared_folder_fid), None)
            for mid in mids:
                add_params = dict(
                    uid=uid,
                    mid=mid,
                    subscription_id=subscription_id,
                )
                qexec(
                    conn, Q.add_to_synced_messages,
                    **add_params
                )
            msg_count -= mcount


@when(u'all subscriptions were terminated')
def step_terminate_subscriptions(context):
    owner_uid = context.owner_uid
    subscriber_uid = context.subscriber_uid
    fids = context.shared_folders_parents.keys()

    with user_connection(context, owner_uid) as conn:
        subs = Queries(conn, owner_uid).shared_folder_subscriptions()
        sub_ids = [sub.subscription_id for sub in subs if sub.fid in fids and sub.subscriber_uid == subscriber_uid]
        for sub_id in sub_ids:
            terminate_params = dict(
                uid=owner_uid,
                subscription_id=sub_id,
            )
            qexec(
                conn, Q.terminate_subscription,
                **terminate_params
            )


@given(u'subscriber has {empty} folder "{folder_name}" in "{parent_name}"')
@given(u'subscriber has folder "{folder_name}" in root')
def step_subscriber_has_folder(context, folder_name, empty=None, parent_name=None):
    uid = context.subscriber_uid
    with user_connection(context, uid) as conn:
        if parent_name:
            parent = Queries(conn, uid).folder_by_name(parent_name)
            parent_fid = parent.fid
        else:
            parent_fid = None
        create_params = dict(
            uid=uid,
            name=folder_name,
            parent_fid=parent_fid,
        )
        cur = qexec(
            conn, Q.create_folder,
            **create_params
        )
        fid = cur.fetchone()[0]
        folder = Queries(conn, uid).folder_by_id(fid)
        if empty == 'none-empty':
            fill_messages_in_folder(conn, uid, folder, 1, SAMPLE_STIDS)
        if parent_fid:
            context.saved_folder = folder


@given(u'previous unsubscribe failed on deleting subscription')
def step_add_subscription(context):
    with user_connection(context, context.owner_uid) as conn:
        qexec(
            conn,
            Q.add_subscription,
            owner_uid=context.owner_uid,
            owner_fid=context.shared_folder_fid,
            subscriber_uid=context.subscriber_uid
        )


def get_subscriber_fid(context):
    try:
        folder_name = loads(context.payload['destination_folder_path'])[-1]
        with user_connection(context, context.subscriber_uid) as conn:
            folder = Queries(conn, context.subscriber_uid).folder_by_name(folder_name)
            return folder.fid
    except KeyError:
        return 1


@given(u'york was restarted during unsubscribe')
def step_add_task(context):
    with user_connection(context, context.owner_uid) as conn:
        qexec(
            conn,
            Q.add_unsubscribe_task,
            task_request_id='some_request_id',
            owner_uid=context.owner_uid,
            owner_fids=[context.shared_folder_fid],
            subscriber_uid=context.subscriber_uid,
            root_subscriber_fid=get_subscriber_fid(context),
        )


@when(u'doberman clears subscriber folders')
def step_doberman_clear_folders(context):
    subscriber_uid = context.subscriber_uid
    subscription_ids = [s.subscription_id for s in context.subscribed_folders]

    with user_connection(context, subscriber_uid) as conn:
        synced_messages = Queries(conn, subscriber_uid).synced_messages()
        found_mids = [sm.mid for sm in synced_messages if sm.subscription_id in subscription_ids]
        DeleteMessages(
            conn, subscriber_uid
        )(mids=found_mids).commit()

    context.execute_steps(u'''
        When all subscriptions were terminated
    ''')

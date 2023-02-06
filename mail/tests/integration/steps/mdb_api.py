# coding:utf-8
from contextlib import contextmanager
from collections import namedtuple
import logging

from behave import given, when, then
from hamcrest import (
    assert_that,
    has_length,
    has_property,
    has_properties,
    empty,
    has_item,
    only_contains,
    has_entry,
    is_,
    is_not,
    equal_to,
)

from mail.pypg.pypg.common import qexec, transaction, fetch_as_dicts

from pymdb.queries import Queries
from pymdb import operations as OPS
from pymdb.types import (OwnerCoordinates,
                         SyncCoordinates,
                         SyncThreading,
                         SyncHeaders,
                         SyncAttach,
                         SyncRecipient,
                         SyncMimePart,)
from pymdb.vegetarian import fill_messages_in_folder, SAMPLE_STIDS

from ora2pg.sharpei import get_shard_id, get_connstring_by_id

from mail.doberman.tests.integration.lib.retries_matcher import RetriesMatcher
from mail.doberman.tests.integration.lib.mdb_api import QUERIES as Q

log = logging.getLogger(__name__)
Subscription = namedtuple('Subscription', ['owner', 'id'])


SLEEP_TIMES = [0.5] * 30  # + [1.] * 30


def with_retries(matcher):
    return RetriesMatcher(SLEEP_TIMES, matcher)


@contextmanager
def get_connection_to_shard(context, shard_id):
    dsn = get_connstring_by_id(context.config.sharpei, shard_id, context.config.mdb_dsn_suffix)
    with transaction(dsn) as conn:
        yield conn


@contextmanager
def get_connection(context, user):
    shard_id = get_shard_id(user.uid, context.config.sharddb_dsn)
    with get_connection_to_shard(context, shard_id) as conn:
        yield conn


@given('"{folder_type:w}" at "{user_name:UserName}" shared folder')
def step_make_folder_shared(context, folder_type, user_name):
    user = context.users[user_name]
    with get_connection(context, user) as conn:
        folder = Queries(conn, user.uid).folder_by_type(folder_type)
        OPS.AddFolderToSharedFolders(conn, user.uid)(folder.fid)


@given('"{subscriber_folder_name:w}" at "{subscriber_name:UserName}"'
       ' subscribed to "{owner_folder_type:w}" at "{owner_name:UserName}"')
@when('I subscribe "{subscriber_folder_name:w}" at "{subscriber_name:UserName}"'
      ' to "{owner_folder_type:w}" at "{owner_name:UserName}"')
def step_subscribe_user(
        context,
        subscriber_folder_name, subscriber_name,
        owner_folder_type, owner_name):
    subscriber = context.users[subscriber_name]
    owner = context.users[owner_name]
    with get_connection(context, subscriber) as subscriber_conn, \
            get_connection(context, owner) as owner_conn:
        owner_qs = Queries(owner_conn, owner.uid)
        subscriber_folder = OPS.CreateFolder(
            subscriber_conn, subscriber.uid
        )(subscriber_folder_name)
        subscriber_folder = Queries(
            subscriber_conn, subscriber.uid
        ).folder_by_name(subscriber_folder_name)
        owner_folder = owner_qs.folder_by_type(owner_folder_type)

        OPS.AddFolderToSubscribedFolders(
            subscriber_conn, subscriber.uid
        )(subscriber_folder.fid, owner.uid, owner_folder.fid)

        OPS.AddSubscriberToSharedFolders(
            owner_conn, owner.uid
        )(owner_folder.fid, subscriber.uid)

        for s in owner_qs.shared_folder_subscriptions():
            if s.subscriber_uid == subscriber.uid:
                context.subscription = Subscription(owner, s.subscription_id)
                break
        else:
            raise RuntimeError("can't find our subscription")


@when('I assign this subscription to "{worker_id}"')
def step_assign_subscription(context, worker_id):
    with get_connection(context, context.subscription.owner) as conn:
        query_params = dict(
            uid=context.subscription.owner.uid,
            subscription_id=context.subscription.id,
            worker_id=worker_id,
        )
        qexec(
            conn, Q.assign_subscription,
            **query_params
        )


@when('I reset subscription state to init')
def step_reset_subscription_to_init(context):
    with get_connection(context, context.subscription.owner) as conn:
        query_params = dict(
            uid=context.subscription.owner.uid,
            subscription_id=context.subscription.id,
        )
        qexec(
            conn, Q.reset_subscription_state_to_init,
            **query_params
        )


def clear_doberman_jobs(context, worker_id):
    for shard in context.config.shards:
        with get_connection_to_shard(context, shard.value) as conn:
            qexec(conn, Q.clear_other_doberman_jobs, worker_id=worker_id)


@given('free worker id "{worker_id}"')
@when('free worker id "{worker_id}"')
def step_add_doberman_job(context, worker_id):
    clear_doberman_jobs(context, worker_id)
    '''
    Add our `dobby` job to first shard
    '''
    with get_connection_to_shard(context, context.config.shards.first.value) as conn:
        qexec(conn, Q.reset_worker_id, worker_id=worker_id)


def check_worker_id_property(context, worker_id, property, matcher):
    with get_connection_to_shard(context, context.config.shards.first.value) as conn:
        def get_worker_id():
            cur = qexec(conn, Q.get_worker_id, worker_id=worker_id)
            return list(fetch_as_dicts(cur))
        assert_that(
            get_worker_id,
            with_retries(
                has_item(has_entry(
                    property, matcher,
                ))))


@then('worker id "{worker_id}" is assigned')
def step_check_doberman_job(context, worker_id):
    check_worker_id_property(context, worker_id, 'launch_id', is_not(equal_to(None)))


def check_subscription_property(context, property, value):
    owner = context.subscription.owner
    with get_connection(context, owner) as conn:
        owner_qs = Queries(conn, owner.uid)
        def get_subscription():
            for s in owner_qs.shared_folder_subscriptions():
                if s.subscription_id == context.subscription.id:
                    return s
            raise RuntimeError(
                "Can't find our subscription with id %r" % context.subscription.id)
        assert_that(
            get_subscription,
            with_retries(
                has_property(
                    property, value,
                )))


@then('our doberman assigned to this subscription')
def step_check_subscription_worker(context):
    check_subscription_property(context, 'worker_id', context.config.worker_id)


@then('doberman put this subscription to "{state:SubscriptionState}" state')
def step_check_subscription_status(context, state):
    check_subscription_property(context, 'state', state.value)


@then('doberman apply this change')
@then('doberman apply this changes')
@given('doberman apply this changes')
@given('doberman apply this change')
def step_wait_for_empty_change_queue(context):
    owner = context.subscription.owner
    with get_connection(context, owner) as conn:
        owner_qs = Queries(conn, owner.uid)

        def get_change_queue():
            return owner_qs.shared_folder_change_queue(subscription_id=context.subscription.id)

        assert_that(
            get_change_queue,
            with_retries(is_(empty()))
        )


@when('I apply "{action:SubscriptionAction}" action on this subscription')
def step_transit_subscription_by_action(context, action):
    owner = context.subscription.owner
    with get_connection(context, owner) as conn:
        OPS.TransitSubscriptionState(conn, owner.uid)(context.subscription.id, action)


@given('new user "{user_name:UserName}" in {shard:Shard} shard '
       'with "{folder_type:w}" shared folder')
def step_new_user_with_shared_folder(context, user_name, shard, folder_type):
    context.execute_steps(u'''
        Given new user "{user_name}" in {shard} shard
         And "{folder_type}" at "{user_name}" shared folder
    '''.format(user_name=user_name, shard=shard.name, folder_type=folder_type))


@given('new user "{user_name:UserName}" in {shard:Shard} shard '
       'subscribed to "{owner_folder_type:w}" at "{owner_name:UserName}"')
def step_new_user_with_subscription(context, user_name, shard, owner_folder_type, owner_name):
    context.execute_steps(u'''
        Given new user "{user_name}" in {shard} shard
         And "{subscriber_folder_name}" at "{user_name}" subscribed to "{owner_folder_type}" at "{owner_name}"
    '''.format(
        user_name=user_name,
        shard=shard.name,
        subscriber_folder_name=owner_name,
        owner_folder_type=owner_folder_type,
        owner_name=owner_name))


@when('I interrupt synchronization')
def step_interrupt_sync(context):
    context.execute_steps(u'''
        Then doberman put this subscription to "sync" state
        When I stop doberman
         And free worker id "dobby"
         And I reset subscription state to init
    ''')


@when('I store message into "{folder_type:w}" at "{user_name:UserName}"')
@when('I store "{message_count:d}" messages into "{folder_type:w}" at "{user_name:UserName}"')
@given('"{message_count:d}" messages in "{folder_type:w}" at "{user_name:UserName}"')
@given('message in "{folder_type:w}" at "{user_name:UserName}"')
def step_store_messsge(context, folder_type, user_name, message_count=1):
    user = context.users[user_name]
    with get_connection(context, user) as conn:
        folder = Queries(conn, user.uid).folder_by_type(folder_type)
        fill_messages_in_folder(conn, user.uid, folder, message_count, SAMPLE_STIDS)


@given('all messages from "{owner_folder_type:w}" at "{owner_name:UserName}" synced '
       'to "{subscriber_name:UserName}"')
def step_sync_all_messages_from_folder(context, owner_folder_type, owner_name, subscriber_name):
    def cast(obj, As):
        return As(**obj.as_dict())

    def cast_seq(seq, As):
        if seq is None:
            return None
        return [cast(obj, As) for obj in seq]

    subscriber = context.users[subscriber_name]
    owner = context.users[owner_name]
    with get_connection(context, subscriber) as subscriber_conn, \
            get_connection(context, owner) as owner_conn:
        owner_qs = Queries(owner_conn, owner.uid)
        owner_folder = owner_qs.folder_by_type(owner_folder_type)
        owner_messages = (m for m in owner_qs.mails() if m.coords.fid == owner_folder.fid)

        for m in owner_messages:
            OPS.SyncMessage(subscriber_conn, subscriber.uid)(
                owner_coords=OwnerCoordinates(
                    uid=owner.uid,
                    fid=m.coords.fid,
                    mid=m.mid,
                    tid=m.coords.tid,
                    revision=owner_folder.revision,
                ),
                sync_coords=cast(m.coords, SyncCoordinates),
                headers=cast(m.headers, SyncHeaders),
                recipients=cast_seq(m.recipients, SyncRecipient),
                attaches=cast_seq(m.attaches, SyncAttach),
                mime=cast_seq(m.mime, SyncMimePart),
                lids=[],
                threads=SyncThreading(
                    references_hashes=[],
                    in_reply_to_hash=None)
            )


def get_all_mids_from_folder(qs, folder_type):
    folder = qs.folder_by_type(folder_type)
    return [m['mid'] for m in qs.messages(fid=folder.fid)]


@when('I copy all messages from "{src_folder_type:w}" '
      'to "{dst_folder_type:w}" at "{user_name:UserName}"')
def step_copy_messages_from_folder(context, src_folder_type, dst_folder_type, user_name):
    user = context.users[user_name]
    with get_connection(context, user) as conn:
        qs = Queries(conn, user.uid)
        dst_folder = qs.folder_by_type(dst_folder_type)
        mids = get_all_mids_from_folder(qs, src_folder_type)
        OPS.CopyMessages(conn, user.uid)(mids=mids, dst_fid=dst_folder.fid)


@when('I create {label_def:LabelDef} at "{user_name:UserName}"')
def step_create_new_label(context, label_def, user_name):
    user = context.users[user_name]
    with get_connection(context, user) as conn:
        OPS.CreateLabel(conn, user.uid)(
            name=label_def.name,
            type=label_def.type,
            color='green'
        )


@then('"{user_name:UserName}" has {label_def:LabelDef}')
def step_user_has_label(context, user_name, label_def):
    user = context.users[user_name]
    with get_connection(context, user) as conn:
        qs = Queries(conn, user.uid)
        assert_that(
            qs.labels(),
            has_item(
                has_properties(
                    'name', label_def.name,
                    'type', label_def.type,
                )))


@when('I "{action}" all message from "{folder_type:w}" at "{user_name:UserName}" by {label_def:LabelDef}')
@when('I "{action}" seen recent deleted all message from "{folder_type:w}" at "{user_name:UserName}"')
@given('all message from "{folder_type:w}" at "{user_name:UserName}" are "{action}" seen recent deleted')
def step_mark_messages(context, action, folder_type, user_name, label_def=None):
    user = context.users[user_name]
    with get_connection(context, user) as conn:
        qs = Queries(conn, user.uid)
        mids = get_all_mids_from_folder(qs, folder_type)
        if label_def is not None:
            label = qs.find_one_label(label_def)

        params = {}
        if action == 'mark':
            params['lids_add'] = [label.lid]
        if action == 'unmark':
            params['lids_del'] = [label.lid]
        if action == 'set':
            params['seen'] = True
            params['recent'] = True
            params['deleted'] = True
        if action == 'unset':
            params['seen'] = False
            params['recent'] = False
            params['deleted'] = False

        OPS.UpdateMessages(conn, user.uid)(
            mids=mids,
            seen=params.get('seen'),
            recent=params.get('recent'),
            deleted=params.get('deleted'),
            lids_add=params.get('lids_add', []),
            lids_del=params.get('lids_del', []),
        )


@when('I delete one message from "{folder_type:w}" at "{user_name:UserName}"')
def step_delete_one_message_from_folder(context, folder_type, user_name):
    user = context.users[user_name]
    with get_connection(context, user) as conn:
        qs = Queries(conn, user.uid)
        mid = get_all_mids_from_folder(qs, folder_type)[0]
        OPS.DeleteMessages(conn, user.uid)(mids=[mid])


@when('I delete all messages from "{folder_type:w}" at "{user_name:UserName}"')
def step_delete_messages_from_folder(context, folder_type, user_name):
    user = context.users[user_name]
    with get_connection(context, user) as conn:
        qs = Queries(conn, user.uid)
        mids = get_all_mids_from_folder(qs, folder_type)
        OPS.DeleteMessages(conn, user.uid)(mids=mids)


def get_folder_messages(context, user_name, folder_name):
    user = context.users[user_name]
    with get_connection(context, user) as conn:
        qs = Queries(conn, user.uid)
        folder = qs.folder_by_name(folder_name)
        return qs.messages(fid=folder.fid)


def get_all_tids_from_folder(qs, folder_type):
    folder = qs.folder_by_type(folder_type)
    retval = set(m['tid'] for m in qs.messages(fid=folder.fid))
    return list(retval)


@when('I join all threads in one from "{folder_type:w}" at "{user_name:UserName}"')
def step_join_threads_by_mids_from_folder(context, folder_type, user_name):
    user = context.users[user_name]
    with get_connection(context, user) as conn:
        qs = Queries(conn, user.uid)
        join_tids = get_all_tids_from_folder(qs, folder_type)
        tid = join_tids[0]
        del join_tids[0]
        OPS.JoinThreads(conn, user.uid)(tid, join_tids)


@then('in "{user_name:UserName}" folder "{folder_name:w}" there {message_count:IsMessageCount}')
@then('in "{user_name:UserName}" folder "{folder_name:w}" all messages has {label_def:LabelDef}')
@then('in "{user_name:UserName}" folder "{folder_name:w}" all messages have deleted label')
def step_has_messages_in_folder(
        context, user_name, folder_name,
        message_count=None, label_def=None):
    user = context.users[user_name]
    with get_connection(context, user) as conn:
        qs = Queries(conn, user.uid)
        folder = qs.folder_by_name(folder_name)
        messages = qs.messages(fid=folder.fid)
        if message_count is not None:
            matcher = has_length(message_count)
        elif label_def is not None:
            label = qs.find_one_label(label_def)
            # pylint: disable=R0204
            matcher = only_contains(has_entry('lids', has_item(label.lid)))
        else:
            matcher = only_contains(has_entry('deleted', equal_to(True)))
        assert_that(messages, matcher)


@then('in "{user_name:UserName}" folder "{folder_name:w}" all messages has not {label_def:LabelDef}')
@then('in "{user_name:UserName}" folder "{folder_name:w}" all messages do not have deleted label')
def step_has_messages_in_folder_not(
        context, user_name, folder_name,
        label_def=None):
    user = context.users[user_name]
    with get_connection(context, user) as conn:
        qs = Queries(conn, user.uid)
        folder = qs.folder_by_name(folder_name)
        messages = qs.messages(fid=folder.fid)
        if label_def is not None:
            label = qs.find_one_label(label_def)
            # pylint: disable=R0204
            matcher = only_contains(has_entry('lids', is_not(has_item(label.lid))))
        else:
            matcher = only_contains(has_entry('deleted', equal_to(False)))
        assert_that(messages, matcher)


@then('synced revision of "{subscriber_folder_name:w}" at "{subscriber_name:UserName}"'
      ' is equal to "{owner_folder_type:w}" at "{owner_name:UserName}" revision')
def step_synced_revision_equal(
        context,
        subscriber_folder_name, subscriber_name,
        owner_folder_type, owner_name):
    owner = context.users[owner_name]
    subscriber = context.users[subscriber_name]
    with get_connection(context, owner) as owner_conn, \
            get_connection(context, subscriber) as subscriber_conn:
        owner_folder = Queries(owner_conn, owner.uid).folder_by_type(owner_folder_type)
        subscriber_qs = Queries(subscriber_conn, subscriber.uid)
        folder = subscriber_qs.folder_by_name(subscriber_folder_name)
        subscribed_folders = subscriber_qs.subscribed_folders()
        assert_that(
            subscribed_folders,
            has_item(
                has_properties(
                    'fid', folder.fid,
                    'synced_revision', owner_folder.revision
                )
            )
        )


@then('in "{user_name:UserName}" folder "{folder_name:w}" all messages has the same thread')
def step_one_thread_in_folder(context, user_name, folder_name):
    user = context.users[user_name]
    with get_connection(context, user) as conn:
        qs = Queries(conn, user.uid)
        folder = qs.folder_by_name(folder_name)
        tids = get_all_tids_from_folder(qs, folder.type)
        assert_that(tids, has_length(1))


def get_last_changelog_entry(context, user_name):
    user = context.users[user_name]
    with get_connection(context, user) as conn:
        full_changelog = Queries(conn, user.uid).changelog()
        assert full_changelog, \
            'changelog is empty %r' % full_changelog
        max_revision = max(c['revision'] for c in full_changelog)
        entries_at_max_revision = [
            c for c in full_changelog
            if c['revision'] == max_revision]
        assert len(entries_at_max_revision) == 1, \
            'Expect one entry at revision %r, got %d: %r' % (
                max_revision,
                len(entries_at_max_revision),
                entries_at_max_revision)
        return entries_at_max_revision[0]


def get_last_changelog_entry_expect_change_type(context, user_name, change_type):
    changelog_entry = get_last_changelog_entry(context, user_name)
    assert changelog_entry['type'] == change_type, \
        'Expect %r change_type, got %r, on %r' % (
            change_type, changelog_entry['type'], changelog_entry)
    return changelog_entry


@then('"{change_type}" is last changelog entry for "{user_name:UserName}" with suppressed notification')
def step_last_changlog_entry_with_quiet(context, change_type, user_name):
    changelog_entry = get_last_changelog_entry_expect_change_type(context, user_name, change_type)
    assert changelog_entry['quiet'] is True, \
        'Expect quiet True got %r, on %r' % (
            changelog_entry['quiet'],
            changelog_entry
            )


@given('I store user message into "{folder_name:w}" at "{user_name:UserName}"')
@given('I store "{message_count:d}" user messages into "{folder_name:w}" at "{user_name:UserName}"')
def step_store_user_messsge(context, folder_name, user_name, message_count=1):
    user = context.users[user_name]
    with get_connection(context, user) as conn:
        folder = Queries(conn, user.uid).folder_by_name(folder_name)
        fill_messages_in_folder(conn, user.uid, folder, message_count, SAMPLE_STIDS)

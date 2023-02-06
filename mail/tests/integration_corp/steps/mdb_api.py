import logging
import time
from collections import namedtuple
from datetime import timedelta

from hamcrest import (
    assert_that,
    has_length,
    has_property,
    empty,
    is_,
)
from hamcrest.core.base_matcher import BaseMatcher

from pymdb import operations as OPS
from pymdb.queries import Queries
from pymdb.types import SubscriptionState
from pymdb.vegetarian import fill_messages_in_folder, SAMPLE_STIDS
from mail.pypg.pypg.common import qexec
from mail.pypg.pypg.query_conf import load_from_my_file
from tests_common.pytest_bdd import given, when, then
from .common import get_connection
from .user import create_new_user

log = logging.getLogger(__name__)
Q = load_from_my_file(__file__)
Subscription = namedtuple('Subscription', ['owner', 'id'])


class RetriesMatcher(BaseMatcher):
    def __init__(self, sleep_times, matcher):
        self.sleep_times = sleep_times
        self.matcher = matcher
        self._last_result = None

    def _matches(self, item):
        if not callable(item):
            raise RuntimeError('expect callable item, got %r', item)
        for try_sleep in self.sleep_times:
            self._last_result = item()
            if self.matcher.matches(self._last_result):
                return True
            time.sleep(try_sleep)
        return False

    def describe_mismatch(self, item, mismatch_description):
        mismatch_description.append_text('last result of ') \
                            .append_description_of(item) \
                            .append_text(' ')
        self.matcher.describe_mismatch(self._last_result, mismatch_description)

    def describe_to(self, description):
        retries_td = timedelta(seconds=sum(self.sleep_times))
        description.append_description_of(self.matcher) \
                   .append_text(' [') \
                   .append_text('after %d retries ' % len(self.sleep_times)) \
                   .append_text('with total duration %s' % str(retries_td)) \
                   .append_text(']')


SLEEP_TIMES = [0.5] * 30


def with_retries(matcher):
    return RetriesMatcher(SLEEP_TIMES, matcher)


DEFAULT_SHARED_FOLDER_TYPE = 'inbox'


@given('shared folder "{shared_folder_name:w}" in {shard:Shard} shard with "{message_count:d}" messages')
def step_make_shared_folder(context, shared_folder_name, shard, message_count):
    user = create_new_user(context, shared_folder_name, shard)
    with get_connection(context, user) as conn:
        folder = Queries(conn, user.uid).folder_by_type(DEFAULT_SHARED_FOLDER_TYPE)
        OPS.AddFolderToSharedFolders(conn, user.uid)(folder.fid)
        fill_messages_in_folder(conn, user.uid, folder, message_count, SAMPLE_STIDS)


def check_subscription_state(context, state):
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
                    'state', state.value,
                )))


def subscribe_user(context, subscriber, shared_folder_name):
    owner = context.users[shared_folder_name]
    with \
        get_connection(context, subscriber) as subscriber_conn, \
        get_connection(context, owner) as owner_conn \
    :
        owner_qs = Queries(owner_conn, owner.uid)
        OPS.CreateFolder(
            subscriber_conn, subscriber.uid
        )(shared_folder_name)
        subscriber_folder = Queries(
            subscriber_conn, subscriber.uid
        ).folder_by_name(shared_folder_name)
        owner_folder = owner_qs.folder_by_type(DEFAULT_SHARED_FOLDER_TYPE)

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


@given('new user "{user_name:w}" in {shard:Shard} shard subscribed to "{shared_folder_name:w}"')
def step_make_new_user_with_subscription(context, user_name, shard, shared_folder_name):
    subscriber = create_new_user(context, user_name, shard)
    subscribe_user(context, subscriber, shared_folder_name)
    check_subscription_state(context, SubscriptionState.sync)


def terminate_subscription(context):
    owner = context.subscription.owner
    with get_connection(context, owner) as conn:
        OPS.TransitSubscriptionState(conn, owner.uid)(context.subscription.id, 'unsubscription')
    check_subscription_state(context, SubscriptionState.terminated)


@given('new user "{user_name:w}" in {shard:Shard} shard with terminated subscription to "{shared_folder_name:w}"')
def step_make_new_user_with_terminated_subscription(context, user_name, shard, shared_folder_name):
    subscriber = create_new_user(context, user_name, shard)
    subscribe_user(context, subscriber, shared_folder_name)
    terminate_subscription(context)


def mark_subscription_failed(context):
    owner = context.subscription.owner
    with get_connection(context, owner) as conn:
        OPS.MarkSubscriptionFailed(conn, owner.uid)(context.subscription.id, "something nasty happens")


@given('new user "{user_name:w}" in {shard:Shard} shard with failed subscription to "{shared_folder_name:w}"')
def step_make_new_user_with_failed_subscription(context, user_name, shard, shared_folder_name):
    subscriber = create_new_user(context, user_name, shard)
    subscribe_user(context, subscriber, shared_folder_name)
    check_subscription_state(context, SubscriptionState.sync)
    mark_subscription_failed(context)


# def init_doberman_jobs(context, shard, worker_id):
#     with get_connection_to_shard(context, shard.value) as conn:
#         qexec(conn, Q.reset_worker, worker_id='husky')
#         qexec(conn, Q.reset_worker, worker_id=worker_id)
#
#
# @given('working dobermans on both shards')
# def step_working_dobermans(context):  # pylint: disable=W0613
#     assert_that(dobermans_should_be_stopped, with_retries(contains_inanyorder(*DOBBY_HOSTS)))
#     init_doberman_jobs(context, context.shards.first, 'dobby')
#     init_doberman_jobs(context, context.shards.second, 'dobby2')
#     start_dobermans()


@then('doberman apply this changes')
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


@when('I store "{message_count:d}" messages into shared folder "{shared_folder_name:w}"')
def step_store_messsge(context, shared_folder_name, message_count):
    user = context.users[shared_folder_name]
    with get_connection(context, user) as conn:
        folder = Queries(conn, user.uid).folder_by_type(DEFAULT_SHARED_FOLDER_TYPE)
        fill_messages_in_folder(conn, user.uid, folder, message_count, SAMPLE_STIDS)


@then('in "{user_name:w}" folder "{folder_name:w}" there {message_count:IsMessageCount}')
def step_has_messages_in_folder(context, user_name, folder_name, message_count):
    user = context.users[user_name]
    with get_connection(context, user) as conn:
        qs = Queries(conn, user.uid)
        folder = qs.folder_by_name(folder_name)
        messages = qs.messages(fid=folder.fid)
        matcher = has_length(message_count)
        assert_that(messages, matcher)


@then('there is no subscription now')
def step_has_no_subscription(context):
    owner = context.subscription.owner
    with get_connection(context, owner) as conn:
        cur = qexec(
            conn=conn,
            query=Q.subscription_exists,
            uid=owner.uid,
            subscription_id=context.subscription.id
        )
        return cur.rowcount == 0


@given('folder "{shared_folder_name:w}" has "{rule_type:RuleType}" archivation rule with "{days:d}" days ttl')
def step_make_shared_folder_with_rule_type(context, shared_folder_name, rule_type, days):
    user = context.users.get(shared_folder_name)
    with get_connection(context, user) as conn:
        folder = Queries(conn, user.uid).folder_by_type(DEFAULT_SHARED_FOLDER_TYPE)
        OPS.SetFolderArchivationRule(conn, user.uid)(folder.fid, rule_type, days)

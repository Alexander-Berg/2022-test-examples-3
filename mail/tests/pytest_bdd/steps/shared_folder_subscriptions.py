# coding: utf-8

from hamcrest import (assert_that,
                      has_length,
                      all_of,
                      has_item,
                      has_property,
                      empty,
                      only_contains, )

from pymdb.operations import (TransitSubscriptionState,
                              MarkSubscriptionFailed,
                              GetFreeSubscriptions,
                              ReleaseSubscription,
                              AddDobermanJob,
                              DeleteSharedFolderSubscriptions, )
from pymdb.queries import Queries
from mail.pypg.pypg.common import qexec
from mail.pypg.pypg.query_conf import load_from_my_file
from tests_common.pytest_bdd import given, when, then

Q = load_from_my_file(__file__)
TEST_WORKER_ID = 'test-worker-id'


def set_worker_for_subscription(conn, uid, subscription_id, worker_id):
    qexec(
        conn,
        Q.set_worker_for_subscription,
        uid=uid,
        subscription_id=subscription_id,
        worker_id=worker_id
    )
    conn.commit()


@given('shared folder with subscription')
def step_make_shared_folder_with_subscriber(context):
    context.execute_steps(
        u'''
        When we initialize new user "Anonymous"
        And we initialize new user "FBR" with "inbox" shared folder
        And we add "Anonymous" to "inbox@FBR" subscribers'''
    )


def get_subscriptions(context):
    return context.qs.shared_folder_subscriptions()


def get_our_subscription(context):
    subs = get_subscriptions(context)
    assert_that(subs, has_length(1))
    return subs[0]


def get_our_subscription_id(context):
    return get_our_subscription(context).subscription_id


def expect_that_our_one_subscription(context, matcher):
    assert_that(
        get_subscriptions(context),
        all_of(
            has_length(1),
            has_item(matcher)
        )
    )


@when('we assign this subscription to "{worker_id:DashedWord}" worker')
# def step_assign_subscription(context, worker_id=TEST_WORKER_ID):
def step_assign_subscription(context, worker_id):
    set_worker_for_subscription(
        context.conn,
        context.uid,
        get_our_subscription_id(context),
        worker_id=worker_id
    )


@given('this subscription is assigned to worker')
def step_add_worker_and_assign_subscription(context):
    context.execute_steps(u'''
    Given "{0:s}" worker
     When we assign this subscription to "{0:s}" worker
    '''.format(TEST_WORKER_ID))


@given('shared folder with subscription assigned to worker')
def step_make_shared_folder_with_assigned_subscription(context):
    context.execute_steps(
        u'''
       Given shared folder with subscription
         And this subscription is assigned to worker'''
    )


@then('this subscription in "{value:YAML}" {name:w}')
@then('this subscription has "{value:YAML}" {name:w}')
def step_check_subscription_property(context, name, value):
    check_subscription_property(**locals())


@then('this subscription {name:w} is NULL')
def step_check_subscription_is_null(context, name):
    check_subscription_property(**locals())


def check_subscription_property(context, name, value=None):
    assert_that(
        get_subscriptions(context),
        all_of(
            has_length(1),
            has_item(
                has_property(name, value)
            )
        )
    )


@when('we apply {actions:SubscriptionActionAndMore} action on this subscription')
def step_when_transit_subscription(context, actions):
    transit_subscription(**locals())


@given('we apply {actions:SubscriptionActionAndMore} on this subscription')
def step_given_transit_subscription(context, actions):
    transit_subscription(**locals())


def transit_subscription(context, actions):
    for a in actions:
        context.apply_op(
            TransitSubscriptionState,
            subscription_id=get_our_subscription_id(context),
            action=a
        )


@when('we try apply {action:SubscriptionAction} action as "{op_id:OpID}"')
def step_async_transit_subscription(context, action, op_id):
    context.operations[op_id] = context.make_async_operation(
        TransitSubscriptionState
    )(
        subscription_id=get_our_subscription_id(context),
        action=action
    )


@when('we try apply {action:SubscriptionAction} action '
      'on nonexistent subscription as "{op_id:OpID}"')
def step_async_transit_nonexistent_subscription(context, action, op_id):
    nonexistent_subscription_id = 42
    subscriptions = get_subscriptions(context)
    if subscriptions:
        nonexistent_subscription_id = max(s.subscription_id for s in subscriptions) + 1
    context.operations[op_id] = context.make_async_operation(
        TransitSubscriptionState
    )(
        subscription_id=nonexistent_subscription_id,
        action=action
    )


@when('we mark this subscription failed')
def step_transit_subscription_to_failed(context):
    transit_subscription_to_failed(**locals())


@when('we mark this subscription failed cause "{fail_reason}"')
def step_transit_subscription_to_failed_reasoned(context, fail_reason):
    transit_subscription_to_failed(**locals())


def transit_subscription_to_failed(context, fail_reason="something nasty happens"):
    context.apply_op(
        MarkSubscriptionFailed,
        subscription_id=get_our_subscription_id(context),
        fail_reason=fail_reason
    )


@when('we try mark this subscription failed as "{op_id:OpID}"')
def step_try_transit_subscription_to_failed(context, op_id):
    context.operations[op_id] = context.make_async_operation(
        MarkSubscriptionFailed
    )(
        subscription_id=get_our_subscription_id(context),
        fail_reason='something nasty happens'
    )


@given('shared folder with subscription after {actions:SubscriptionActionAndMore}')
def step_make_shared_folder_and_apply_actions(context, actions):
    actions_str = u', '.join(a.value for a in actions)
    context.execute_steps(u'''
        Given shared folder with subscription
          And this subscription is assigned to worker
         When we apply %s action on this subscription
    ''' % actions_str)


@given('there are no free subscriptions')
def step_assign_all_free_subscriptions(context):
    worker_id = context.feature_name
    context.apply_op(AddDobermanJob, worker_id=worker_id)
    qexec(
        context.conn,
        Q.assign_all_free_subscriptions,
        worker_id=worker_id
    )
    context.conn.commit()


@given('shared folder with {count:d} free subscriptions')
def step_make_shared_folder_with_subscribers(context, count):
    make_shared_folder_with_subscribers(**locals())


@given('shared folder with one free subscription')
def step_make_shared_folder_with_one_subscriber(context):
    make_shared_folder_with_subscribers(**locals())


def make_shared_folder_with_subscribers(context, count=1):
    new_users_step = [
        u'When we initialize new user "Anonymous{}"'.format(i)
        for i in range(0, count)]
    add_subscribers_step = [
        u'When we add "Anonymous{}" to "inbox@FBR" subscribers'.format(i)
        for i in range(0, count)]
    context.execute_steps(u'\n'.join(
        new_users_step +
        [u'When we initialize new user "FBR" with "inbox" shared folder'] +
        add_subscribers_step
    ))


@given('shared folder without subscriptions')
def step_make_shared_folder_without_subscriber(context):
    context.execute_steps(
        u'When we initialize new user "FBR" with "inbox" shared folder'
    )


@given('this shared folder has extra subscription')
def step_add_extra_subscription(context):
    context.execute_steps(u'''
         When we initialize new user "Unexpected"
         When we add "Unexpected" to "inbox@FBR" subscribers
    ''')


@when('we get {limit:d} free subscriptions for "{worker_id:DashedWord}" worker')
def step_get_free_subscriptions(context, limit, worker_id):
    context.operations[worker_id] = context.make_operation(
        GetFreeSubscriptions
    )(
        worker_id=worker_id,
        subscription_limit=limit
    )
    context.operations[worker_id].commit()


@when('we try get {limit:d} free subscriptions for "{worker_id:DashedWord}" worker as "{op_id:OpID}"')
def step_try_get_free_subscriptions(context, limit, worker_id, op_id):
    context.operations[op_id] = context.make_async_operation(
        GetFreeSubscriptions
    )(
        worker_id=worker_id,
        subscription_limit=limit
    )


@then('all new subscriptions are set to worker "{worker_id:DashedWord}"')
def step_subscriptions_has_worker(context, worker_id):
    workers = [sub.worker_id for sub in context.operations[worker_id].result]
    assert_that(workers, only_contains(worker_id))


@then('"{worker_id:DashedWord}" has {count:d} new subscriptions')
def step_worker_has_subscriptions(context, worker_id, count):
    assert_that(context.operations[worker_id].result, has_length(count))


@then('there are {count:d} free subscriptions left')
def step_free_subscriptions_left(context, count):
    all_subs = get_subscriptions(context)
    free_subs = [sub for sub in all_subs if sub.worker_id is None]
    assert_that(free_subs, has_length(count))


@then('subscriptions in "{first_op:OpID}" and "{second_op:OpID}" are different')
def step_subscriptions_in_ops_are_different(context, first_op, second_op):
    subs_id_first = [sub.subscription_id for sub in context.operations[first_op].result]
    subs_id_second = [sub.subscription_id for sub in context.operations[second_op].result]

    common = set(subs_id_first).intersection(subs_id_second)
    assert_that(common, empty())


@when('we release this subscription')
def step_release_subscription(context):
    our_subscription = get_our_subscription(context)
    context.apply_op(
        ReleaseSubscription,
        subscription_id=our_subscription.subscription_id,
        worker_id=our_subscription.worker_id
    )


@when('we try release this subscription as "{op_id:OpID}"')
def step_try_release_subscription(context, op_id):
    our_subscription = get_our_subscription(context)
    context.operations[op_id] = context.make_async_operation(
        ReleaseSubscription
    )(
        subscription_id=our_subscription.subscription_id,
        worker_id=our_subscription.worker_id
    )


@when('we try release this subscription from different worker as "{op_id:OpID}"')
def step_try_release_subscription_with_different_worker(context, op_id):
    our_subscription = get_our_subscription(context)
    other_worker_id = str(our_subscription.worker_id) + '-other'
    context.operations[op_id] = context.make_async_operation(
        ReleaseSubscription
    )(
        subscription_id=our_subscription.subscription_id,
        worker_id=other_worker_id
    )


def remove_subscription(context, subscription_id):
    context.del_subscription_op = context.make_operation(
        DeleteSharedFolderSubscriptions
    )(
        subscription_ids=[subscription_id]
    )
    context.del_subscription_op.commit()


@when('we delete this subscription')
def step_remove_this_subscription(context):
    context.our_subscription = get_our_subscription(context)
    remove_subscription(context,
                        context.our_subscription.subscription_id)


@when('we delete same subscription')
def step_remove_subscription(context):
    remove_subscription(context,
                        context.our_subscription.subscription_id)


@when('we delete "{user_name:w}" subscription to "{owner_ref:FolderRef}"')
def step_remove_subscription_by_user_name(context, user_name, owner_ref):
    user_uid = context.users[user_name]
    owner_uid = context.users[owner_ref.user_name]

    owner_qs = Queries(context.conn, owner_uid)
    owner_folder = owner_qs.folder_by(
        folder_type=owner_ref.folder_type,
        folder_name=owner_ref.folder_name)

    owner_subscriptions = list(owner_qs.shared_folder_subscriptions())
    for sub in owner_subscriptions:
        if sub.subscriber_uid == user_uid and sub.fid == owner_folder.fid:
            context.apply_op(
                DeleteSharedFolderSubscriptions,
                uid=owner_uid,
                subscription_ids=[sub.subscription_id],
            )
            remove_subscription(context, sub.subscription_id)
            return
    raise AssertionError(
        "Can't find %r subscription to %r in %r" % (
            user_uid, owner_ref, owner_subscriptions
        )
    )


@then('shared folder does not have this subscription')
def step_shared_folder_does_not_have_this_subscription(context):
    assert_that([s for s in get_subscriptions(context)
                 if s.subscription_id == context.our_subscription.subscription_id],
                empty())


@then('deleted subscription is returned')
def step_check_deleted_subscription(context):
    assert_that(context.del_subscription_op.result,
                only_contains(context.our_subscription))


@then('nothing is returned')
def step_empty_result(context):
    assert_that(context.del_subscription_op.result, empty())

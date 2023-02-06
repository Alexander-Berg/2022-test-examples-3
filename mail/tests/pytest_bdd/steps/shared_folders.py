# coding: utf-8

from hamcrest import (assert_that,
                      has_property,
                      has_properties,
                      has_item,
                      has_length,
                      all_of,
                      not_,
                      is_)

from pymdb import operations as OPS
from pymdb.queries import Queries
from tests_common.pytest_bdd import given, when, then


@when('we add "{folder_type:w}" to shared folders')
def step_mark_folder_as_master_shared(context, folder_type):
    fld = context.qs.folder_by_type(folder_type)
    context.apply_op(
        OPS.AddFolderToSharedFolders,
        fid=fld.fid,
    )


@given('he has "{folder_type:w}" shared folder')
def step_create_folder_and_mark_it_shared(context, folder_type):
    context.execute_steps(u'''
        When we create "{folder_type}" folder "{folder_name}"
         And we add "{folder_type}" to shared folders
    '''.format(
        folder_type=folder_type,
        folder_name=folder_type.capitalize()
    ))


@given('he has "{folder_type:w}" shared folder with "{mids:MidsRange}"')
def step_create_shared_folder_with_letters(context, folder_type, mids):
    context.execute_steps(u'''
       Given he has "{folder_type}" shared folder
        When we store "{mids}" into "{folder_type}"
    '''.format(
        folder_type=folder_type,
        mids=",".join(mids)
    ))


@then('"{folder_type:w}" is shared folder')
def step_check_folder_is_master_shared(context, folder_type):
    folder = context.qs.folder_by_type(folder_type)
    assert_that(context.qs.shared_fids(), has_item(folder.fid))


def get_shared_folder_subscriptions(context, folder):
    return [
        s for s in context.qs.shared_folder_subscriptions()
        if s.fid == folder.fid]


@then('"{folder_type:w}" has "{count:d}" subscribers')
@then('"{folder_type:w}" has "{count:d}" subscriber')
def step_check_shared_folder_subsribers_count(context, folder_type, count):
    folder = context.qs.folder_by_type(folder_type)
    assert_that(
        get_shared_folder_subscriptions(context, folder),
        has_length(count)
    )


@then('"{user_name}" is "{folder_type:w}" subscriber')
def step_check_user_is_subscriber(context, user_name, folder_type):
    assert_that(
        get_shared_folder_subscriptions(
            context,
            context.qs.folder_by_type(folder_type)),
        has_item(has_property('subscriber_uid', context.users[user_name])))


@given('new initialized user "{user_name:UserName}" with "{folder_type:w}" shared folder')
def step_given_user_named_with_shared_folder(context, folder_type, user_name):
    make_user_with_shared_folder(**locals())


@when('we initialize new user "{user_name:UserName}" with "{folder_type:w}" shared folder')
def step_when_user_named_with_shared_folder(context, folder_type, user_name):
    make_user_with_shared_folder(**locals())


@given('new initialized user with "{folder_type:w}" shared folder')
def step_given_user_with_shared_folder(context, folder_type):
    make_user_with_shared_folder(**locals())


@when('we initialize new user with "{folder_type:w}" shared folder')
def step_when_user_with_shared_folder(context, folder_type):
    make_user_with_shared_folder(**locals())


def make_user_with_shared_folder(context, folder_type, user_name=u'Anonymous'):
    context.execute_steps(u'''
        When we initialize new user "{user_name}"
        And we add "{folder_type}" to shared folders
    '''.format(
        user_name=user_name,
        folder_type=folder_type,
    ))


@given('new initialized user "{user_name:UserName}" '
       'with "{mids:MidsRange}" in "{folder_type:w}" shared folder')
def step_given_user_with_shared_folder_and_message_in_it(context, folder_type, user_name, mids):
    user_with_shared_folder_and_message_in_it(**locals())


@when('we initialize new user "{user_name:UserName}" '
      'with "{mids:MidsRange}" in "{folder_type:w}" shared folder')
def step_when_user_with_shared_folder_and_message_in_it(context, folder_type, user_name, mids):
    user_with_shared_folder_and_message_in_it(**locals())


def user_with_shared_folder_and_message_in_it(context, folder_type, user_name, mids):
    context.execute_steps(u'''
        When we initialize new user "{user_name}"
        And we add "{folder_type}" to shared folders
        And we store "{mids}" into "{folder_type}"
    '''.format(
        user_name=user_name,
        folder_type=folder_type,
        mids=u",".join(mids)))


def get_queries_by_user(context, user_name):
    uid = context.users[user_name]
    return Queries(context.conn, uid)


def make_add_folder_op(context, operation_maker, owner_ref, folder_name):
    owner_qs = get_queries_by_user(context, owner_ref.user_name)
    return operation_maker(
        OPS.AddFolderToSubscribedFolders,
    )(
        fid=context.qs.folder_by_name(folder_name).fid,
        owner_uid=owner_qs.uid,
        owner_fid=owner_qs.folder_by(
            folder_type=owner_ref.folder_type,
            folder_name=owner_ref.folder_name).fid
    )


@when('we add "{owner_ref:FolderRef}" as "{folder_name:w}" to subscribed folders')
def step_add_folder_to_subscribed_folders(context, owner_ref, folder_name):
    make_add_folder_op(context, context.make_operation, owner_ref, folder_name).commit()


@when(u'we try add "{owner_ref:FolderRef}" as "{folder_name:w}" to subscribed folders'
      ' as "{op_id:OpID}"')
def step_try_add_folder_to_subscribed_folders(context, owner_ref, folder_name, op_id):
    context.operations[op_id] = make_add_folder_op(
        context, context.make_async_operation, owner_ref, folder_name)


@given('he has "{folder_name:w}" subscribed to "{owner_ref:FolderRef}"')
def step_create_and_add_subsribed_folder(context, owner_ref, folder_name):
    context.execute_steps(
        u'''When we create "user" folder "{folder_name}"
            And we add "{owner_ref}" as "{folder_name}" to subscribed folders
         '''.format(
            folder_name=folder_name,
            owner_ref=str(owner_ref)))


@then('folder "{folder_name:w}" is subscribed to "{owner_ref:FolderRef}"')
def step_folder_in_subscribed_folders(context, folder_name, owner_ref):
    owner_qs = get_queries_by_user(context, owner_ref.user_name)
    owner_folder = owner_qs.folder_by(
        folder_type=owner_ref.folder_type,
        folder_name=owner_ref.folder_name)
    folder = context.qs.folder_by_name(folder_name)
    assert_that(folder, has_properties(subscribed_for_shared_folder=True))
    assert_that(
        context.qs.subscribed_folders(),
        has_item(
            has_properties(
                fid=folder.fid,
                owner_uid=owner_qs.uid,
                owner_fid=owner_folder.fid, )))


def get_subscribed_folder_by_subscriber(context, folder):
    for f in context.qs.subscribed_folders():
        if f.fid == folder.fid:
            return f


@then('folder "{folder_name:w}" is not subscribed for shared folder')
def step_folder_is_not_subscribed_for_shared_folder(context, folder_name):
    folder = context.qs.folder_by_name(folder_name)
    assert_that(folder, not_(has_properties(subscribed_for_shared_folder=True)))

    subscribed_folder = get_subscribed_folder_by_subscriber(context, folder)
    assert_that(subscribed_folder, is_(None))


@when('delete folder "{folder_name:w}" from subscribed')
def step_delete_folder_from_subscribed(context, folder_name):
    folder = context.qs.folder_by_name(folder_name)
    subscribed_folder = get_subscribed_folder_by_subscriber(context, folder)
    context.apply_op(
        OPS.DeleteFolderFromSubscribedFolders,
        i_uid=context.uid,
        i_owner_uid=subscribed_folder.owner_uid,
        i_owner_fids=[subscribed_folder.owner_fid]
    )


@then('subscribed folder "{folder_name:w}" has {matchers:SubscribedMatcherAndMore}')
def step_check_subscribed_folder(context, folder_name, matchers):
    # Get folder, cause don't know fid
    folder = context.qs.folder_by_name(folder_name)
    all_subscribed_folders = context.qs.subscribed_folders()
    assert_that(
        all_subscribed_folders,
        has_item(
            all_of(
                has_property('fid', folder.fid),
                *matchers
            )))


@given('new initialized user with "{folder_name:w}" subscribed to "{owner_ref:FolderRef}"')
def step_given_new_user_with_subscription(context, folder_name, owner_ref):
    make_new_user_with_subscription(**locals())


@when('we initialize new user with "{folder_name:w}" subscribed to "{owner_ref:FolderRef}"')
def step_when_new_user_with_subscription(context, folder_name, owner_ref):
    make_new_user_with_subscription(**locals())


@given('new initialized user "{user_name:w}" with "{folder_name:w}" subscribed to "{owner_ref:FolderRef}"')
def step_given_new_user_named_with_subscription(context, user_name, folder_name, owner_ref):
    make_new_user_with_subscription(**locals())


@when('we initialize new user "{user_name:w}" with "{folder_name:w}" subscribed to "{owner_ref:FolderRef}"')
def step_when_new_user_named_with_subscription(context, user_name, folder_name, owner_ref):
    make_new_user_with_subscription(**locals())


def make_new_user_with_subscription(context, folder_name, owner_ref, user_name=u'Anonymous'):
    context.execute_steps(
        u'''
        When we initialize new user "{user_name}"
        When we create "user" folder "{folder_name}"
        And we add "{owner_ref}" as "{folder_name}" to subscribed folders
        '''.format(
            folder_name=folder_name,
            owner_ref=owner_ref,
            user_name=user_name
        )
    )


@given('"{subscriber_name:UserName}" is "{owner_ref:FolderRef}" subscriber')
def step_given_add_subscriber_to_shared_folder(context, subscriber_name, owner_ref):
    add_subscriber_to_shared_folder(**locals())


@when('we subscribe "{subscriber_name:UserName}" to "{owner_ref:FolderRef}"')
@when('we add "{subscriber_name:UserName}" to "{owner_ref:FolderRef}" subscribers')
def step_when_add_subscriber_to_shared_folder(context, subscriber_name, owner_ref):
    add_subscriber_to_shared_folder(**locals())


def add_subscriber_to_shared_folder(context, subscriber_name, owner_ref):
    owner_qs = get_queries_by_user(context, owner_ref.user_name)
    context.apply_op(
        OPS.AddSubscriberToSharedFolders,
        uid=owner_qs.uid,
        fid=owner_qs.folder_by(
            folder_type=owner_ref.folder_type,
            folder_name=owner_ref.folder_name).fid,
        subscriber=context.users[subscriber_name]
    )


@given('"{owner_ref:FolderRef}" shared folder '
       'with "{subscriber_name:UserName}" subscriber')
def step_new_user_subscribed_to_owner_folder(context, subscriber_name, owner_ref):
    context.execute_steps(
        u'''
        When we initialize new user "{subscriber_name}"
        And we initialize new user "{owner_name}" with "{folder_type}" shared folder
        And we add "{subscriber_name}" to "{owner_ref}" subscribers
        '''.format(
            subscriber_name=subscriber_name,
            owner_name=owner_ref.user_name,
            folder_type=owner_ref.folder_type,
            owner_ref=str(owner_ref),
        )
    )

from tests_common.user import make_user_oneline
from behave import given

from request import request_create_list
from mdb_api import (
    create_folder,
    create_shared_folder,
)


DEFAULT_SHARED_FOLDER_NAME = "ImmaSharedFolder"


@given(u'test user')
def step_test_user(context):
    user = make_user_oneline(context, user_name='User')
    context.uid = context.payload['uid'] = user.uid


@given(u'test user with shared folder')
@given(u'test user with shared folder "{shared_folder_name}"')
def step_test_user_with_shared_folder(context, shared_folder_name=DEFAULT_SHARED_FOLDER_NAME):
    user = make_user_oneline(context, user_name='User')
    context.uid = context.payload['uid'] = user.uid
    context.payload['shared_folder_fid'] = context.shared_folder_fid\
        = create_shared_folder(context, user.uid, shared_folder_name)


@given(u'test subscriber')
def step_test_subscriber(context):
    user = make_user_oneline(context, user_name='SubscriberUser')
    context.payload['subscriber_uid'] = context.subscriber_uid = user.uid


@given(u'test owner')
def step_test_owner(context):
    user = make_user_oneline(context, user_name='OwnerUser')
    context.payload['owner_uid'] = context.owner_uid = user.uid


@given(u'owner has shared folder')
@given(u'owner has shared folder "{shared_folder_name}"')
def step_has_shared(context, shared_folder_name=DEFAULT_SHARED_FOLDER_NAME):
    r = request_create_list(context.york, context.owner_uid, shared_folder_name)
    assert r.status_code == 200, 'Expect response status code to be 200. Response: {}'.format(r)
    context.shared_folder_fid = int(r.json()['shared_folder_fid'])
    context.payload['shared_folder_fid'] = context.shared_folder_fid
    context.shared_folders_parents[context.shared_folder_fid] = None


def create_shared_subfolder(ctx, uid, folder_name, parent_fid):
    fid = create_shared_folder(ctx, uid, folder_name, parent_fid)
    ctx.shared_folders_parents[fid] = parent_fid
    return fid


@given(u'owner has shared folders tree')
def step_owner_has_shared_tree(context):
    new_folders = {}
    for row in context.table:
        parent_fid = new_folders.get(row['parent'])
        fid = create_shared_subfolder(context, context.owner_uid, row['name'], parent_fid)
        if not parent_fid:
            context.shared_folder_fid = fid
        new_folders[row['name']] = fid
    context.payload['shared_folder_fid'] = context.shared_folder_fid


@given(u'test subscriber with {empty} folder named "{folder_name}"')
def step_test_subscriber_with_folder(context, empty, folder_name):
    context.execute_steps(u'Given test subscriber')
    create_folder(context, context.subscriber_uid, folder_name, None)
    if empty == 'none-empty':
        context.execute_steps(u'''
            Given message was stored in "{folder_name}"
        '''.format(folder_name=folder_name))

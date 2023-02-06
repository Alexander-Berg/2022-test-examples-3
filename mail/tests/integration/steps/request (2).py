from tests_common.fbbdb import is_user_exists
from tests_common.mdb import user_mdb_queries
from behave import (
    given,
    when,
)


@given(u'invalid uid as "{param_name}" in request')
def step_invalid_uid(context, param_name):
    invalid_uid = 666
    # Hopefully this will converge
    while is_user_exists(context.fbbdb_conn, invalid_uid):
        invalid_uid *= 10
        if invalid_uid > 2 ** 63:
            invalid_uid /= 2 ** 60

    context.payload[param_name] = invalid_uid


@when(u'we request "{endpoint}" with no params')
def step_request(context, endpoint):
    context.req = context.york.request_get(endpoint)


@when(u'we request "{endpoint}" with params')
def step_request_with_params(context, endpoint):
    for row in context.table:
        context.payload[row['param']] = row['value']
    context.req = context.york.request_get(endpoint, **context.payload)


@when(u'we request "{endpoint}" with different {diff_par} and params')
def step_request_with_diff_params(context, endpoint, diff_par):
    if diff_par:
        context.payload[diff_par] = 'diff_' + str(context.payload[diff_par])
    for row in context.table:
        context.payload[row['param']] = row['value']
    context.req = context.york.request_get(endpoint, **context.payload)


@when(u'we request "{endpoint}" with same params')
@when(u'we request "{endpoint}"')
def step_request_with_same(context, endpoint):
    context.req = context.york.request_get(endpoint, **context.payload)


@when(u'we request "{endpoint}" as post')
def step_request_post(context, endpoint):
    context.req = context.york.request_post(endpoint)


def request_create_list(york, uid, shared_folder_name):
    params = {'uid': uid, 'shared_folder_name': shared_folder_name}
    return york.request_get('create_list', **params)


def request_create_list_by_context(context):
    return request_create_list(
        context.york,
        uid=context.payload['uid'],
        shared_folder_name=context.payload['shared_folder_name']
    )


@given(u'he has a created list with name "{shared_folder_name}"')
def step_has_created_list(context, shared_folder_name):
    context.payload['shared_folder_name'] = shared_folder_name
    r = request_create_list_by_context(context)
    assert r.status_code == 200, 'Precondition failed, cannot create list, code=%d' % r.status_code


@given(u'subscriber is subscribed to it into the path "{path}"')
@given(u'subscriber is "{recursive}" subscribed to it into the path "{path}"')
def step_subscribed_to_path(context, path, recursive=None):
    context.payload['destination_folder_path'] = path
    if recursive == 'recursively':
        context.payload['recursive'] = 'yes'
    r = context.york.request_get('subscribe', **context.payload)
    assert r.status_code == 200, 'Precondition failed, cannot subscribe, code=%d' % r.status_code

    owner_uid = context.owner_uid
    subscriber_uid = context.subscriber_uid
    shared_fids = context.shared_folders_parents.keys()
    with user_mdb_queries(context, subscriber_uid) as q:
        subs = q.subscribed_folders()
        context.subscribed_folders = [s for s in subs if s.owner_uid == owner_uid and s.owner_fid in shared_fids]


@given(u'subscribe into the path "{path}" failed before create subscription')
@given(u'subscribe "{recursive}" into the path "{path}" failed before create subscription')
def step_subscribe_failed(context, path, recursive=None):
    if recursive == 'recursively':
        subscribe_step = u'''
            Given subscriber is "recursively" subscribed to it into the path "{path}"
        '''.format(path=path)
    else:
        subscribe_step = u'''
            Given subscriber is subscribed to it into the path "{path}"
        '''.format(path=path)
    context.execute_steps(subscribe_step +
                          u'And subscription was deleted from DB')


@given(u'subscriber is subscribed to it into the path "{path}" with messages synced')
@given(u'subscriber is subscribed to it into the path "{path}" with {msg_count:d} messages synced')
@given(u'subscriber is "{recursive}" subscribed to it into the path "{path}" with messages synced')
def step_user_subscribed_with_messages(context, path, msg_count=1, recursive=None):
    context.execute_steps(u'''
            Given subscriber is "{recursive}" subscribed to it into the path "{path}"
              And {msg_count} messages was synced to subscribed folder
        '''.format(path=path, recursive=recursive, msg_count=msg_count))


@given(u'user has archivation rule')
@given(u'user has archivation rule "{archive_type}"')
def step_given_archivation_rule(context, archive_type='archive'):
    context.payload['type'] = archive_type
    context.payload['keep_days'] = '42'
    r = context.york.request_get('set_archivation_rule', **context.payload)
    assert r.status_code == 200, 'Precondition failed, cannot set_archivation_rule, code=%d' % r.status_code

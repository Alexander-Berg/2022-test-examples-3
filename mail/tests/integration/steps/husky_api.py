import json
import logging

from mail.devpack.lib.components.sharddb import ShardDb
from mail.husky.devpack.components.api import HuskyApi
from ora2pg.tools import http

from tests_common.pytest_bdd import when, then
from hamcrest import assert_that, has_entry, has_length, all_of, equal_to, has_property, has_key, contains_string
from hamcrest.core.base_matcher import BaseMatcher


log = logging.getLogger(__name__)


def make_clone_url(husky_api, source_uid, dest_uid, with_deleted_box=False):
    url = 'http://{addr}/clone_user/{src}/into/{dst}'.format(
        addr=husky_api,
        src=source_uid,
        dst=dest_uid,
    )
    if with_deleted_box:
        url += '/with_deleted_box'
    return url


def husky_api_call(url, post=True):
    with http.request(
        url,
        data='' if post else None
    ) as fd:
        response = fd.read()
        log.info('Response is %r ', response)
        return json.loads(response)


class IsJsonThat(BaseMatcher):
    def __init__(self, value_matcher):
        self.value_matcher = value_matcher

    def _matches(self, item):
        return self.value_matcher.matches(json.loads(item))

    def describe_to(self, description):
        description.append_text('text in JSON format').append_description_of(self.value_matcher)


def is_json_that(match):
    return IsJsonThat(match)


@when('we clone "{source_name:w}" into "{dest_name:w}"')
@when('we clone "{source_name:w}" into "{dest_name:w}" with params')
def step_clone_user(context, source_name, dest_name):
    return step_clone_user_impl(**locals())


@when('we clone "{source_name:w}" into "{dest_name:w}"{with_restore_deleted:WithRestoreDeleted}')
@when('we clone "{source_name:w}" into "{dest_name:w}"{with_restore_deleted:WithRestoreDeleted} and params')
def step_clone_user_with_restore_deleted(context, source_name, dest_name, with_restore_deleted):
    return step_clone_user_impl(**locals())


@when('we use "add task" to clone "{source_name:w}" into "{dest_name:w}"')
def step_clone_user_using_add_task(context, source_name, dest_name):
    return step_clone_user_using_add_task_impl(**locals())


@when('we add task with unknown type')
def step_add_task_with_unknown_type(context):
    return step_add_task_with_unknown_type_impl(**locals())


def step_add_task_with_unknown_type_impl(context):
    task_data = json.dumps({
        "uid": 1,
        "task": "unknown_type",
        "task_args": {
            "source_user_uid": 1,
            "dest_shard": 1,
            "restore_deleted_box": False,
        }
    }).encode("utf-8")

    response = context.coordinator.components[HuskyApi].post(
        "/add_task",
        data=task_data,
        headers={'Content-Type' : 'application/json'}
    ).json()
    context.response = response


@then("response status is error")
def step_response_status_is_error(context):
    assert context.response['status'] == 'error'
    assert 'error' in context.response.keys()


@then('error in response is "{error_value}"')
def step_error_in_response(context, error_value):
    assert context.response['error'] == error_value


@when('we plan clone "{source_name:w}" into "{dest_name:w}" with params')
def step_plan_clone_user(context, source_name, dest_name):
    return step_plan_clone_user_impl(**locals())


@then('response status is error with reason "{reason}"')
def step_status_is_error(context, reason):
    assert context.response['status'] == 'error'
    assert context.response['reason'] == reason


def step_plan_clone_user_impl(context, source_name, dest_name, with_restore_deleted=False):
    task_args = json.loads(context.text or '{}')
    source_user = context.users.get(source_name)
    dest_user = context.users.get(dest_name)

    context.response = context.coordinator.components[HuskyApi].create_clone_user_task(
        source_user.uid, dest_user.uid, with_restore_deleted, task_args
    ).json()


def step_clone_user_impl(context, source_name, dest_name, with_restore_deleted=False):
    step_plan_clone_user_impl(context, source_name, dest_name, with_restore_deleted)
    response = context.response

    assert 'task' in response, \
        'Expect task in response, got %r' % response
    assert 'transfer_id' in response['task'], \
        'Expect transfer_id in response["task"], got %r' % response

    context.transfer_id = response['task']['transfer_id']


def step_clone_user_using_add_task_impl(context, source_name, dest_name, with_restore_deleted=False):
    source_user = context.users.get(source_name)
    dest_user = context.users.get(dest_name)
    shard_id = context.coordinator.components[ShardDb].query(
        "SELECT shard_id FROM shards.users LIMIT 1"
    )[0][0]

    task_data = json.dumps({
        "uid": dest_user.uid,
        "task": "clone_user",
        "task_args": {
            "source_user_uid": source_user.uid,
            "dest_shard": shard_id,
            "restore_deleted_box": with_restore_deleted,
        }
    }).encode("utf-8")

    response = context.coordinator.components[HuskyApi].post(
        "/add_task",
        data=task_data,
        headers={'Content-Type' : 'application/json'}
    ).json()

    assert 'task' in response, \
        'Expect task in response, got %r' % response
    assert 'transfer_id' in response['task'], \
        'Expect transfer_id in response["task"], got %r' % response

    context.transfer_id = response['task']['transfer_id']


def get_clone_status(context, source_name, dest_name):
    source_user = context.users.get(source_name)
    dest_user = context.users.get(dest_name)

    response = context.coordinator.components[HuskyApi].get_clone_user_task(
        source_user.uid, dest_user.uid
    ).json()
    log.info('Clone response: %r', response)

    try:
        return response['status']
    except KeyError:
        raise AssertionError(
            'Can\'t find status key in response: %r' % response
        )


@then('clone "{source_name:w}" into "{dest_name:w}" is completed')
def step_check_clone_completed(context, source_name, dest_name):
    clone_status = get_clone_status(context, source_name, dest_name)
    assert clone_status == 'complete', \
        'Expect clone status is completed, got %r' % clone_status


@then('clone "{source_name:w}" into "{dest_name:w}" is not completed')
def step_check_clone_not_completed(context, source_name, dest_name):
    clone_status = get_clone_status(context, source_name, dest_name)
    assert clone_status != 'complete', \
        'Expect clone status is not completed, got %r' % clone_status


@when('passport requests delete user{right_now:RightNow?}')
def step_delete_user_via_api(context, right_now):
    context.response = context.coordinator.components[HuskyApi].create_delete_user_task(
        uid=context.user.uid,
        right_now=right_now,
    )


@when('passport requests delete nonexistent user')
def step_delete_user_via_api_nonexistent_user(context):
    context.response = context.coordinator.components[HuskyApi].create_delete_user_task(uid=0)


@then('last delete user request is successful')
def step_delete_user_via_api_successful(context):
    log.info(context.response.text)
    assert_that(context.response, all_of(
        has_property('status_code', equal_to(200)),
        has_property('headers', has_entry('Content-Type', equal_to('application/json'))),
        has_property('text', is_json_that(
            all_of(has_entry('status', 'ok'), has_entry('tasks', has_length(2)))
        )),
    ))
    context.transfer_id_list = [t['transfer_id'] for t in context.response.json()['tasks']]


@then('last delete user request is failed with status "{status:d}" and code "{code:d}"')
def step_delete_user_via_api_failed(context, status, code):
    assert_that(context.response, all_of(
        has_property('status_code', equal_to(status)),
        has_property('headers', has_entry('Content-Type', equal_to('application/json'))),
        has_property('text', is_json_that(
            all_of(
                has_entry('status', 'error'),
                has_entry('error', all_of(
                    has_entry('code', code),
                    has_key('message'),
                    has_key('traceback'),
                ))
            )
        )),
    ))


@when('we call add_shard_task with')
def step_add_shard_task(context):
    return step_add_shard_task_impl(**locals())


def step_add_shard_task_impl(context):
    context.response = context.coordinator.components[HuskyApi].post(
        "/add_shard_task",
        data=context.text,
        headers={'Content-Type' : 'application/json'}
    )


@then('add_shard_task request is successful with tasks_count "{tasks_count:d}" and users_count "{users_count:d}"')
def step_add_shard_task_response_is_successful(context, tasks_count, users_count):
    assert_that(context.response, all_of(
        has_property('status_code', equal_to(200)),
        has_property('headers', has_entry('Content-Type', equal_to('application/json'))),
        has_property('text', is_json_that(
            all_of(
                has_entry('status', 'ok'),
                has_entry('tasks_count', equal_to(tasks_count)),
                has_entry('users_count', equal_to(users_count)),
            )
        )),
    ), 'bad response %r' % context.response.text)


@then('add_shard_task request is failed with error containing "{error_text}"')
def step_add_shard_task_response_is_failed(context, error_text):
    assert_that(context.response, all_of(
        has_property('status_code', equal_to(500)),
        has_property('headers', has_entry('Content-Type', equal_to('application/json'))),
        has_property('text', is_json_that(
            all_of(
                has_entry('status', 'error'),
                has_entry('error', has_entry('message', contains_string(error_text))),
            )
        )),
    ), 'unexpected response %r' % context.response.text)

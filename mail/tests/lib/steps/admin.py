import json as jsn

from hamcrest import has_entries
from hamcrest.core import assert_that

from tests_common.pytest_bdd import when, then


@when('we make ping request')
def step_execute_ping_request(context):
    context.response = context.shiva.client().ping()


@then(u'shiva responds pong')
def step_ping_request_executed_succesfully(context):
    assert context.response.status_code == 200, \
        'Expected: status_code 200, but was: "{}"'.format(context.response.status_code)
    assert context.response.text == 'pong', \
        'Expected: response text "pong", but was: "{}"'.format(context.response.text)


@when('we make shards request to shiva')
def step_execute_shards_request(context):
    context.response = context.shiva.client().admin().shards()


@then(u'shiva returns all this shards')
def step_shards_request_executed_succesfully(context):
    assert context.response.status_code == 200, \
        'Expected: status_code 200, but was: "{}"'.format(context.response.status_code)
    actual = context.response.json()
    expected = context.shiva_shards
    assert expected == actual, \
        'Expected: shards "{}", but was: "{}"'.format(expected, actual)


@when('we make shards request to shiva with "{param_name:w}" "{param_value:w}"')
def step_execute_shards_with_filter_request(context, param_name, param_value):
    context.req_params = {param_name: param_value}
    context.response = context.shiva.client().admin().shards(**context.req_params)


@then(u'shiva returns "{shard_id:w}" shard')
def step_shiva_returns_specified_shard(context, shard_id):
    assert context.response.status_code == 200, \
        'Expected: status_code 200, but was: "{}" and {}'.format(context.response.status_code, context.response.text)
    actual = context.response.json()
    expected = {shard_id: context.shiva_shards[shard_id]}
    assert_that(
        actual,
        {k: has_entries(v) for k, v in expected.items()},
        'Expected: shards "{}", but was: "{}"'.format(expected, actual)
    )


@when('we make add_shard request to shiva with')
def step_execute_add_shard_request(context):
    context.req_params = jsn.loads(context.text)
    context.response = context.shiva.client().admin().add_shard(**context.req_params)


@then(u'shiva returns 200 OK')
def step_shiva_returns_ok(context):
    assert context.response.status_code == 200, \
        'Expected: status_code "200", but was: "{}"'.format(context.response.status_code)


@then(u'shiva returns error with status code "{status:d}"')  # noqa: F811
def step_shiva_returns_error(context, status):
    assert context.response.status_code == status, \
        'Expected: status_code "{}", but was: "{}" and {}'.format(status, context.response.status_code, context.response.text)


@when('we make delete_shard request to shiva with shard_id "{shard_id:d}"')
def step_execute_delete_shard_request(context, shard_id):
    context.req_params = {'shard_id': shard_id}
    context.response = context.shiva.client().admin().delete_shard(**context.req_params)


@when('we make delete_shard request to shiva without shard_id')
def step_execute_delete_shard_request_without_shard_id(context):
    context.response = context.shiva.client().admin().delete_shard()


@when('we make update_shard request to shiva without shard_id')
def step_execute_update_shard_request_without_shard_id(context):
    context.response = context.shiva.client().admin().update_shard()


@when('we make update_shard request to shiva with shard_id "{shard_id:d}" and "{param_name:w}" "{param_value:w}"')
def step_execute_update_shard_request(context, shard_id, param_name, param_value):
    context.req_params = {
        'shard_id': shard_id,
        param_name: param_value}
    context.response = context.shiva.client().admin().update_shard(**context.req_params)


@then(u'"{shard_id:w}" shard has "{field_name:w}" "{field_value:w}"')
def step_shard_has_field(context, shard_id, field_name, field_value):
    actual = str(context.shiva_shards[shard_id][field_name])
    assert actual == field_value, \
        'Expected: shards "{}", but was: "{}"'.format(field_value, actual)

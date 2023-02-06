import requests

from tests_common.pytest_bdd import (
    when,
    then,
)

from hamcrest import (
    assert_that,
    equal_to,
)

from .common_steps import (
    ShardSchema,
    check_stat_response_for_shard_matches_shard,
    check_stat_response_matches_all_shards,
    match_shard,
    match_shard_instances_with_role,
    match_shard_with_order,
)


@when('we request sharpei for /get_user with uid "{uid:d}" and mode "{mode}" and force is "{force}"')
@when('we request sharpei for /get_user with uid "{uid:d}" and mode <mode> and force is "{force}"')
def step_request_sharpei_for_get_user(context, uid, mode=None, force=None):
    context.response = context.sharpei_api.get_user(
        uid=uid,
        mode=mode,
        force=force,
        request_id=context.request_id,
    )
    context.pyremock.assert_expectations()


@when('we request sharpei for /get_user with uid "{uid:d}" and mode "{mode}"')
@when('we request sharpei for /get_user with uid "{uid:d}" and mode <mode>')
def _step_request_sharpei_for_get_user(context, uid, mode):
    step_request_sharpei_for_get_user(context, uid, mode)


@when('we request sharpei for /get_user with uid equals "{uid}" and mode "{mode}"')
def _step_request_sharpei_for_get_user_with_any_uid(context, uid, mode):
    step_request_sharpei_for_get_user(context, uid, mode)


@when('we request sharpei for /get_user with uid "{uid:d}" and without mode')
def ____step_request_sharpei_for_get_user(context, uid):
    context.response = requests.get(
        context.sharpei_api.location + '/get_user?uid={uid}'.format(uid=uid),
        timeout=3,
        headers=context.sharpei_api.make_headers(context.request_id),
    )
    context.pyremock.assert_expectations()


@when('we successfully request sharpei for /get_user with uid "{uid:d}" and mode "{mode}"')
def step_successfully_request_sharpei_for_get_user(context, uid, mode):
    step_request_sharpei_for_get_user(context, uid, mode)
    assert_that(context.response.status_code, equal_to(200))


@when('we successfully request sharpei for /get_user with uid equals "{uid}" and mode "{mode}"')
def step_successfully_request_sharpei_for_get_user_with_any_uid(context, uid, mode):
    step_request_sharpei_for_get_user(context, uid, mode)
    assert_that(context.response.status_code, equal_to(200))


@when('we request sharpei for /create_user with uid "{uid:d}" and shard_id "{shard_id}"')
def step_request_sharpei_for_create_user(context, uid=None, shard_id=None):
    context.response = context.sharpei_api.create_user(
        uid=uid,
        shard_id=shard_id,
        request_id=context.request_id,
    )
    context.pyremock.assert_expectations()


@when('we request sharpei for /create_user with uid "{uid:d}"')
def _step_request_sharpei_for_create_user(context, uid=None):
    step_request_sharpei_for_create_user(context, uid, shard_id=1)


@when('we request sharpei for /create_user with uid equals "{uid}"')
def _step_request_sharpei_for_create_user_with_any_uid(context, uid=None):
    step_request_sharpei_for_create_user(context, uid, shard_id=1)


@when('we request sharpei for /create_user without uid')
def __step_request_sharpei_for_create_user_without_uid(context):
    context.response = requests.post(
        context.sharpei_api.location + '/create_user',
        timeout=3,
        headers=context.sharpei_api.make_headers(context.request_id),
    )
    context.pyremock.assert_expectations()


@when('we successfully request sharpei for /create_user with uid "{uid:d}" and shard_id "{shard_id:d}"')
def _step_successfully_request_sharpei_for_create_user(context, uid, shard_id):
    step_request_sharpei_for_create_user(context, uid, shard_id)
    assert_that(context.response.status_code, equal_to(200))


@when('we successfully request sharpei for /create_user with uid equals "{uid}" and shard_id "{shard_id:d}"')
def _step_successfully_request_sharpei_for_create_user_with_any_uid(context, uid, shard_id):
    step_request_sharpei_for_create_user(context, uid, shard_id)
    assert_that(context.response.status_code, equal_to(200))


@when('we request sharpei for /update_user with uid "{uid:d}" shard_id "{shard_id:d}" new_shard_id "{new_shard_id:d}" and data "{data}"')
def step_request_sharpei_for_update_user(context, uid, shard_id=None, new_shard_id=None, data=None):
    context.response = context.sharpei_api.update_user(
        uid=uid,
        shard_id=shard_id,
        new_shard_id=new_shard_id,
        data=data,
        request_id=context.request_id,
    )
    context.pyremock.assert_expectations()


@when('we request sharpei for /update_user with uid "{uid:d}" and data "{data}"')
def __step_request_sharpei_for_update_user(context, uid, data):
    step_request_sharpei_for_update_user(context, uid, shard_id=None, new_shard_id=None, data=data)


@when('we request sharpei for /update_user with uid "{uid:d}" shard_id "{shard_id:d}" and data "{data}"')
def ___step_request_sharpei_for_update_user(context, uid, shard_id, data):
    step_request_sharpei_for_update_user(context, uid, shard_id, new_shard_id=None, data=data)


@when('we request sharpei for /update_user with uid "{uid:d}" shard_id "{shard_id:d}" and new_shard_id "{new_shard_id:d}"')
def ____step_request_sharpei_for_update_user(context, uid, shard_id, new_shard_id):
    step_request_sharpei_for_update_user(context, uid, shard_id, new_shard_id)


@when('we request /update_user with uid "{uid:d}" shard_id "{shard_id:d}" new_shard_id "{new_shard_id:d}" and data "{data}"')
def _____step_request_sharpei_for_update_user(context, uid, shard_id, new_shard_id, data):
    step_request_sharpei_for_update_user(context, uid, shard_id, new_shard_id, data)


@when('we request sharpei for /update_user with uid "{uid:d}" new_shard_id "{new_shard_id:d}"')
def ______step_request_sharpei_for_update_user(context, uid, new_shard_id):
    step_request_sharpei_for_update_user(context, uid, shard_id=None, new_shard_id=new_shard_id)


@when('we request /update_user with uid "{uid:d}" new_shard_id "{new_shard_id:d}" and data "{data}"')
def _______step_request_sharpei_for_update_user(context, uid, new_shard_id, data):
    step_request_sharpei_for_update_user(context, uid, shard_id=None, new_shard_id=new_shard_id, data=data)


@when('we successfully request sharpei for /update_user with uid "{uid:d}" shard_id "{shard_id:d}" new_shard_id "{new_shard_id:d}" and data "{data}"')
def step_successfully_request_sharpei_for_update_user(context, uid, shard_id=None, new_shard_id=None, data=None):
    step_request_sharpei_for_update_user(context, uid, shard_id, new_shard_id, data)
    assert_that(context.response.status_code, equal_to(200))
    assert_that(context.response.text, equal_to("\"done\""))
    assert_that(context.response.headers['Content-Type'], 'application/json')


@when('we successfully request sharpei for /update_user with uid equals "{uid}" shard_id "{shard_id:d}" new_shard_id "{new_shard_id:d}" and data "{data}"')
def step_successfully_request_sharpei_for_update_user_with_any_uid(context, uid, shard_id=None, new_shard_id=None, data=None):
    step_request_sharpei_for_update_user(context, uid, shard_id, new_shard_id, data)
    assert_that(context.response.status_code, equal_to(200))
    assert_that(context.response.text, equal_to("\"done\""))
    assert_that(context.response.headers['Content-Type'], 'application/json')


@when('we successfully request sharpei for /update_user with uid "{uid:d}" and data "{data}"')
def _step_successfully_request_sharpei_for_update_user(context, uid, data):
    step_successfully_request_sharpei_for_update_user(context, uid, shard_id=None, new_shard_id=None, data=data)


@when('we successfully request sharpei for /update_user with uid "{uid:d}" shard_id "{shard_id:d}" and data "{data}"')
def __step_successfully_request_sharpei_for_update_user(context, uid, shard_id, data):
    step_successfully_request_sharpei_for_update_user(context, uid, shard_id, new_shard_id=None, data=data)


@when('we successfully request sharpei for /update_user with uid "{uid:d}" shard_id "{shard_id:d}" and new_shard_id "{new_shard_id:d}"')
def ___step_successfully_request_sharpei_for_update_user(context, uid, shard_id, new_shard_id):
    step_successfully_request_sharpei_for_update_user(context, uid, shard_id, new_shard_id)


@when('we request sharpei for /update_user with uid equals "{uid}"')
def _step_request_sharpei_for_update_user(context, uid):
    step_request_sharpei_for_update_user(context, uid)


@when('we request sharpei for /update_user without uid')
def step_request_sharpei_for_update_user_without_uid(context):
    context.response = requests.post(
        context.sharpei_api.location + '/update_user',
        timeout=3,
        headers=context.sharpei_api.make_headers(context.request_id),
    )
    context.pyremock.assert_expectations()


@when('we request sharpei for /stat')
def step_request_sharpei_for_stat(context):
    context.response = context.sharpei_api.stat()
    context.pyremock.assert_expectations()


@when('we request sharpei for /stat with shard_id "{shard_id}"')
def step_request_stat_with_shard_id(context, shard_id):
    context.response = context.sharpei_api.stat(
        request_id=context.request_id,
        shard_id=shard_id,
    )
    context.pyremock.assert_expectations()


@then('response shard is "{shard_id:d}"')
def step_response_shard_is(context, shard_id):
    assert_that(context.response.json()['shard']['id'], equal_to(shard_id))


@then('response data is "{data}"')
def step_response_data_is(context, data):
    assert_that(context.response.json()['data'], equal_to(data))


@then('response content is "{content}"')
def step_response_content_is(context, content):
    assert_that(context.response.headers['Content-Type'], content)


@then('/get_user response matches shard "{shard_id:d}"')
def step_get_user_response_matches_expected_all_where_shard(context, shard_id):
    shard_description = context.response.json()['shard']
    match_shard(shard_description, context, shard_id)


@then('response matches "{role}" hosts from shard "{shard_id:d}"')
def step_get_user_response_matches_expected_role_where_shard(context, role, shard_id):
    shard_description = context.response.json()['shard']
    match_shard_instances_with_role(role, shard_description, context, shard_id)


@then('/stat response matches all shards')
def step_stat_response_for_shard_matches_shard(context):
    check_stat_response_matches_all_shards(context, ShardSchema.v3)


@then('/stat response matches shard "{shard_id:d}"')  # noqa: F811
def step_stat_response_for_shard_matches_shard(context, shard_id):
    assert_that(len(context.response.json()), equal_to(1))
    check_stat_response_for_shard_matches_shard(context, shard_id, ShardSchema.v3)


@then('/get_user response matches instances in order "{first_role}"-"{second_role}"-"{third_role}" from shard "{shard_id:d}"')
def step_get_user_response_matches_expected_first_is_second_is_where_shard(context, first_role, second_role, third_role, shard_id):
    shard_description = context.response.json()['shard']
    match_shard_with_order(context, shard_description, shard_id, [first_role, second_role, third_role])


@when('we request sharpei for /reset_cache where shard "{shard:d}"')
def step_request_sharpei_for_reset_cache(context, shard):
    context.response = context.sharpei_api.reset_cache(
        shard=shard,
        request_id=context.request_id,
    )
    context.pyremock.assert_expectations()


@when('we successfully request sharpei for /reset_cache where shard "{shard:d}"')
def step_successfully_request_sharpei_for_reset_cache(context, shard):
    step_request_sharpei_for_reset_cache(context, shard)
    assert_that(context.response.status_code, equal_to(200))
    assert_that(context.response.headers['Content-Type'], 'text/plain')
    assert_that(context.response.text, equal_to('ok'))


def erroneously_request_sharpei_for_reset_cache(context, shard, miss_shard=False, use_get=False):
    make_request = requests.get if use_get else requests.post
    url = context.sharpei_api.location + '/reset_cache'
    if not miss_shard:
        url += '?shard={shard}'.format(shard=shard)
    return make_request(
        url,
        timeout=3,
        headers=context.sharpei_api.make_headers(context.request_id),
    )


@when('we request sharpei for /reset_cache without shard')
def step_request_sharpei_for_reset_cache_without_shard(context):
    context.response = erroneously_request_sharpei_for_reset_cache(context, shard=None, miss_shard=True)
    context.pyremock.assert_expectations()


@when('we request sharpei for /reset_cache with GET method where shard "{shard:d}"')
def step_request_sharpei_for_reset_cache_with_get(context, shard):
    context.response = erroneously_request_sharpei_for_reset_cache(context, shard, use_get=True)
    context.pyremock.assert_expectations()

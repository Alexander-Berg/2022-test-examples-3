from copy import deepcopy

from hamcrest import (
    assert_that,
    has_entry,
)
from tests_common.pytest_bdd import (
    when,
    then,
)


def fill_params(context):
    params = {
        'uid': 0,
        'fid': 0,
        'first': 0,
        'count': 100,
    }
    params.update(context.params)
    context.params = params


@when(u'we request "{endpoint}"')
def step_request_hound(context, endpoint):
    fill_params(context)
    context.response = context.hound.request_get(endpoint, **context.params)


@when(u'we request "{endpoint}" with args:')
def step_request_hound_with_args(context, endpoint):
    for row in context.table:
        context.params[row['arg_name']] = row['arg_value']
    fill_params(context)
    context.response = context.hound.request_get(endpoint, **context.params)


@when(u'we request "{endpoint}" without any argument')
def step_request_hound_without_args(context, endpoint):
    context.response = context.hound.request_get(endpoint)


@when(u'we request "{endpoint}" with an invalid uid')
def step_request_hound_with_invalid_uid(context, endpoint):
    fill_params(context)
    params = deepcopy(context.params)
    params.update({'uid': 'lolshto'})
    context.response = context.hound.request_get(endpoint, **params)


@then(u'response is OK')
def step_check_ok(context):
    assert context.response.status_code == 200


@then(u'response status is {code:d}')
def step_check_response_status(context, code):
    assert context.response.status_code == code


@then(u'there is error in response with "{code:d}" code')
def step_check_error(context, code):
    assert_that(context.response.json(), has_entry(
        'error', has_entry(
            'code', code)))

from pytest_bdd import scenarios
from .conftest import get_path

from hamcrest import (
    assert_that,
    any_of,
    has_item,
    has_entry,
    has_value,
    has_length,
    is_not,
)
from tests_common.pytest_bdd import then, when


scenarios(
    "hidden_trash.feature",
    features_base_dir=get_path("mail/hound/tests/integration/features/hidden_trash.feature"),
    strict_gherkin=False
)


@then(u'there are no folder "{folder_type:w}" in response')
def step_check_no_folder_in_response(context, folder_type):
    assert_that(context.response.json()['folders'], is_not(any_of(
        has_item(has_entry('symbol', folder_type)),
        has_value(has_entry('symbolicName', has_entry('title', folder_type)))
    )))


@then(u'there are folder "{folder_type:w}" in response')
def step_check_folder_in_response(context, folder_type):
    assert_that(context.response.json()['folders'], any_of(
        has_item(has_entry('symbol', folder_type)),
        has_value(has_entry('symbolicName', has_entry('title', folder_type)))
    ))


@when(u'we request "messages_by_folder" for "{fid}" folder')
def step_request_messages_by_folder(context, fid):
    context.params['fid'] = context.folders[fid]
    context.execute_steps(u'When we request "messages_by_folder"')


@then(u'there are "{msg_count:d}" messages in response')
def step_check_message_count_in_response(context, msg_count):
    assert_that(context.response.json()['envelopes'], has_length(msg_count))

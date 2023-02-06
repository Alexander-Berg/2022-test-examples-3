import pytest
import functools
import datetime

import pytest_bdd
from pytest_bdd import (
    given,
    then,
    when,
    parsers
)

from hamcrest import anything, has_entry, is_
from library.python.testing.pyremock.lib.pyremock import HttpMethod, MatchRequest, MockHttpServer, MockResponse

from furita_common import get_path
import furita_helpers as helpers


scenario = functools.partial(
    pytest_bdd.scenario,
    features_base_dir=get_path("mail/furita/tests/isolated/feature/furita_preview.feature"),
)


@scenario("furita_preview.feature", "Calling with empty condition fields")
def test_preview_with_empty_condition():
    pass


@scenario("furita_preview.feature", "Calling with some condition")
def test_preview_with_nonempty_condition():
    pass


@scenario("furita_preview.feature", "Calling with existing rule")
def test_preview_with_existing_rule():
    pass


@scenario("furita_preview.feature", "Calling with nonexistent rule")
def test_preview_with_nonexistent_rule():
    pass


def set_up_msearch_mock(context):
    request = MatchRequest(method=is_(HttpMethod.GET),
                           path=is_('/api/async/mail/furita'),
                           params=has_entry('uid', anything()))
    mock_response = MockResponse(status=200, body=b'{"envelopes":[]}')
    context.msearch = MockHttpServer(context.furita.search_port)
    context.msearch.start()
    context.msearch.expect(request, mock_response)


@given("furita is up and running")
def furita_started(context):
    assert context.furita
    context.furita.last_response = None


@given(parsers.parse('new user "{name}"'))
def create_user(context, name):
    context.create_user(name)


@given(parsers.parse('user "{name}" has a rule "{rule_name}"'))
def create_named_rule(context, name, rule_name):
    context.furita.just_created_rule[rule_name] = helpers.create_named_rule(context, context.get_uid(name), rule_name)


@when(parsers.parse('we preview empty rule of the user "{name}"'))
def preview_empty_rule(context, name):
    context.furita.last_response = context.furita_api.api_preview(uid=context.get_uid(name))


@when(parsers.parse('we preview rule "" of user "{name}"'))
def preview_non_existent_rule(context, name):
    context.furita.last_response = context.furita_api.api_preview(
        uid=context.get_uid(name),
        id=42,
    )


@when(parsers.parse('we preview rule "{rule_name}" of user "{name}"'))
def preview_created_rule(context, rule_name, name):
    set_up_msearch_mock(context)
    context.furita.last_response = context.furita_api.api_preview(
        uid=context.get_uid(name),
        id=context.furita.just_created_rule[rule_name],
    )


@when(parsers.parse('we preview rule of user "{name}" with params:\n{param_lines}'))
def preview_rule_by_params(context, name, param_lines):
    set_up_msearch_mock(context)
    params = [
        tuple(line.strip().split('=', 1))
        for line in param_lines.split('\n')
    ]
    context.furita.last_response = context.furita_api.api_preview(uid=context.get_uid(name), params=params)


@then(parsers.parse('furita replies with {response_code:d}'))
def furita_replies(context, response_code):
    if context.msearch:
        context.msearch.wait_for(max_timeout=datetime.timedelta(seconds=10))
        context.msearch.assert_expectations()
    assert context.furita.last_response.status_code == response_code, context.furita.last_response.text


@pytest.fixture(scope='function', autouse=True)
def pertest_setup(request, context):
    context.furita.just_created_rule = {}
    context.msearch = None

    def mocks_teardown():
        if context.msearch:
            context.msearch.stop()

    request.addfinalizer(mocks_teardown)

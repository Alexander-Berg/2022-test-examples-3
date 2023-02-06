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

from furita_common import get_path, get_rule
import furita_helpers as helpers


scenario = functools.partial(
    pytest_bdd.scenario,
    features_base_dir=get_path("mail/furita/tests/isolated/feature/furita_apply.feature"),
)


@scenario("furita_apply.feature", "Calling without params")
def test_apply_without_params():
    pass


@scenario("furita_apply.feature", "Applying nonexistent rule")
def test_apply_for_nonexistent_rule():
    pass


@scenario("furita_apply.feature", "Applying existing rule")
def test_apply_for_existing_rule():
    pass


def set_up_msearch_mock(context):
    request = MatchRequest(method=is_(HttpMethod.GET),
                           path=is_('/api/async/mail/furita'),
                           params=has_entry('uid', anything()))
    mock_response = MockResponse(status=200, body=b'{"envelopes":["111", "222", "333"]}')
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
    helpers.create_named_rule(context, context.get_uid(name), rule_name)


@when(parsers.parse('we try to apply rule "{rule_name}" of the user "{name}"'))
def try_apply_rule(context, rule_name, name):
    set_up_msearch_mock(context)
    uid = context.get_uid(name)
    rule = get_rule(context.furita_api, uid, rule_name)
    context.furita.last_response = context.furita_api.api_apply(uid=uid, id=rule["id"])


@when(parsers.parse('we try to apply rule "" of the user "{name}"'))
def try_apply_wrong_rule(context, name):
    uid = context.get_uid(name)
    context.furita.last_response = context.furita_api.api_apply(uid=uid, id=42)


@when("we call apply without uid")
def try_apply_no_uid(context):
    context.furita.last_response = context.furita_api.api_apply(uid=None, id=42)


@then(parsers.parse('furita replies with {response_code:d}'))
def furita_replies(context, response_code):
    if context.msearch:
        context.msearch.wait_for(max_timeout=datetime.timedelta(seconds=10))
        context.msearch.assert_expectations()
    assert context.furita.last_response.status_code == response_code


@pytest.fixture(scope='function', autouse=True)
def pertest_setup(request, context):
    context.msearch = None

    def mocks_teardown():
        if context.msearch:
            context.msearch.stop()

    request.addfinalizer(mocks_teardown)

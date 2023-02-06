# -*- coding: utf-8 -*-
import functools

import pytest_bdd
from pytest_bdd import (
    given,
    then,
    when,
    parsers
)
from furita_common import (
    get_path,
    get_names_from_lines
)


scenario = functools.partial(
    pytest_bdd.scenario,
    features_base_dir=get_path("mail/furita/tests/isolated/feature/furita_multilist.feature"),
    strict_gherkin=False
)


@scenario("furita_multilist.feature", "Obtaining the multilist of the rules for one user")
def test_furita_api_multilist_obtain_one_user():
    pass


@scenario("furita_multilist.feature", "Obtaining the multilist of the rules for two users")
def test_furita_api_multilist_obtain_two_users():
    pass


@scenario("furita_multilist.feature", "Obtaining the multilist of the rules for three users")
def test_furita_api_multilist_obtain_three_users():
    pass


@given("furita is up and running")
def furita_started(context):
    assert context.furita
    context.furita.last_response = None


@given(parsers.parse('new users: {names}'))
def create_users(context, names):
    for name in names.split(','):
        context.create_user(name)


@then(parsers.parse('user "{name}" has the following list of the rules:\n{rules_lines}'))
def create_rules(context, name, rules_lines):
    uid = context.get_uid(name)
    rules = get_names_from_lines(rules_lines)

    for rule_name in rules:
        response = context.furita_api.api_edit(uid=uid, name=rule_name)
        assert response.status_code == 200


@when(parsers.parse('we obtain rules for the following users: {users}'))
def obtain_rules_for_users(context, users):
    uids = [str(context.get_uid(user.strip())) for user in users.split(',')]
    context.furita.last_response = context.furita_api.api_multilist(uids=uids)


@then(parsers.parse('user "{user}" have ok result with {rules_count} rules'))
def check_user_with_ok_result(context, user, rules_count):
    json_list = context.furita.last_response.json()
    uid = context.get_uid(user)
    items = list(filter(lambda user_result: user_result['uid'] == uid, json_list['users']))
    assert len(items) == 1, 'Multiply users with same name in multilist answer'
    assert items[0]['result'] == 'ok'
    assert len(items[0]['rules']) == int(rules_count)


@then(parsers.parse('user "{user}" have error result with message "{message}"'))
def check_user_with_error_result(context, user, message):
    json_list = context.furita.last_response.json()
    uid = context.get_uid(user)
    items = list(filter(lambda user_result: user_result['uid'] == uid, json_list['users']))
    assert len(items) == 1, 'Multiply users with same name in multilist answer'
    assert items[0]['result'] == 'error'
    assert items[0]['message'] == message


@then(parsers.parse('furita replies with {response_code:d}'))
def furita_replies(context, response_code):
    assert context.furita.last_response is not None
    assert context.furita.last_response.status_code == response_code

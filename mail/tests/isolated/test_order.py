# coding=utf-8
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
    get_rule,
    get_rules,
    get_names_from_lines
)


scenario = functools.partial(
    pytest_bdd.scenario,
    features_base_dir=get_path("mail/furita/tests/isolated/feature/furita_order.feature"),
    strict_gherkin=False
)


@scenario("furita_order.feature", "Calling order rules method for the existent rules")
def test_furita_order_existent_rules():
    pass


@scenario("furita_order.feature", "Calling order rules method for incompleted list of the rules")
def test_furita_order_incomplete_rules():
    pass


@scenario("furita_order.feature", "Calling order rules method with duplicated ids")
def test_furita_order_duplicates_rules():
    pass


@scenario("furita_order.feature", "Calling order rules method without list of the rules")
def test_furita_order_without_rules():
    pass


@given("furita is up and running")
def furita_started(context):
    assert context.furita
    context.furita.last_status_code = None


@given(parsers.parse('new user "{name}"'))
def create_user(context, name):
    context.create_user(name)


@then(parsers.parse('furita replies with {response_code:d}'))
def furita_replies(context, response_code):
    """ Эта функция имеет смысл только если её вызывать сразу """
    """ после order_many_rules(...), """
    """ так как переменная context.furita.last_status_code устанавливается там и только там """
    assert context.furita.last_status_code == response_code


@when(parsers.parse('we create the following list of the rules for the user "{user_name}":\n{rules_lines}'))
def create_many_rules(context, user_name, rules_lines):
    uid = context.get_uid(user_name)

    for rule_name in get_names_from_lines(rules_lines):
        response = context.furita_api.api_edit(uid=uid, name=rule_name)
        assert response.status_code == 200


@when(parsers.parse('we order the rules of the user "{user_name}" as follow:\n{rules_lines}'))
def order_many_rules(context, user_name, rules_lines):
    uid = context.get_uid(user_name)

    ids = []
    for rule_name in get_names_from_lines(rules_lines):
        rule = get_rule(context.furita_api, uid, rule_name)
        assert rule is not None
        ids.append(rule["id"])

    response = context.furita_api.api_order(uid, ids)
    context.furita.last_status_code = response.status_code


@then(parsers.parse('user "{user_name}" has the following ordered by priority list of the rules:\n{rules_lines}'))
def has_ordere_many_rules(context, user_name, rules_lines):
    uid = context.get_uid(user_name)
    requested_list = get_names_from_lines(rules_lines)

    rules = get_rules(context.furita_api, uid)
    rules.sort(key=lambda rule: rule["priority"])
    actual_list = list(map(lambda rule: rule["name"], rules))

    assert requested_list == actual_list

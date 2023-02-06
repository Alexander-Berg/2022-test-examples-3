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
    features_base_dir=get_path("mail/furita/tests/isolated/feature/furita_remove.feature"),
    strict_gherkin=False
)


@scenario("furita_remove.feature", "Calling remove rule method for the existent rule")
def test_furita_remove_existent_rule():
    pass


@scenario("furita_remove.feature", "Calling remove rule method for many existent rules")
def test_furita_remove_many_rules():
    pass


@scenario("furita_remove.feature", "Calling remove rule method with empty id argument")
def test_furita_remove_rule_with_empty_id():
    pass


@scenario("furita_remove.feature", "Calling remove rule method without id argument")
def test_furita_remove_rule_without_id():
    pass


@given("furita is up and running")
def furita_started(context):
    assert context.furita
    context.furita.last_response = None


@given(parsers.parse('new user "{name}"'))
def create_user(context, name):
    context.create_user(name)


@when(parsers.parse('we create a rule "{rule_name}" for the user "{user_name}"'))
def create_rule(context, rule_name, user_name):
    uid = context.get_uid(user_name)
    response = context.furita_api.api_edit(uid=uid, name=rule_name)
    assert response.status_code == 200


@then(parsers.parse('user "{user_name}" has {rules_count:d} rule'))
@then(parsers.parse('user "{user_name}" has {rules_count:d} rules'))
def has_user_a_rule(context, user_name, rules_count):
    uid = context.get_uid(user_name)

    rules = get_rules(context.furita_api, uid)
    assert len(rules) == rules_count


@then(parsers.parse('furita replies with {response_code:d}'))
def furita_replies(context, response_code):
    """ Эта функция имеет смысл только если её вызывать сразу """
    """ после remove_one_rule, remove_one_wrong_rule или remove_many_rules, """
    """ так как переменная context.furita.last_response устанавливается там и только там """
    assert context.furita.last_response is not None
    assert context.furita.last_response.status_code == response_code


@when(parsers.parse('we remove rule "" of the user "{user_name}"'))
def remove_one_wrong_rule(context, user_name):
    """ Эта функция нужна для того, чтобы в remove_one_rule """
    """ установить параметр rule_name в '' """
    remove_one_rule(context, user_name, "")


@when(parsers.parse('we remove rule "{rule_name}" of the user "{user_name}"'))
def remove_one_rule(context, user_name, rule_name):
    uid = context.get_uid(user_name)

    rule_id = None
    if rule_name != "":
        rule = get_rule(context.furita_api, uid, rule_name)
        rule_id = rule["id"] if rule is not None else None
    else:
        rule_id = ""

    context.furita.last_response = context.furita_api.api_remove(uid, [rule_id] if rule_id is not None else [])


@when(parsers.parse('we remove the following rules of the user "{user_name}":\n{rules_lines}'))
def remove_many_rules(context, user_name, rules_lines):
    uid = context.get_uid(user_name)

    ids = []
    for rule_name in get_names_from_lines(rules_lines):
        rule = get_rule(context.furita_api, uid, rule_name)
        if rule and rule["id"] is not None:
            ids.append(rule["id"])

    context.furita.last_response = context.furita_api.api_remove(uid, ids)

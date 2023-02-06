# coding=utf-8
import functools

import pytest
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
import furita_helpers as helpers

scenario = functools.partial(
    pytest_bdd.scenario,
    features_base_dir=get_path("mail/furita/tests/isolated/feature/furita_list.feature"),
    strict_gherkin=False
)


@scenario("furita_list.feature", "Obtaining the list of the rules")
def test_furita_api_list_just_obtain():
    pass


@scenario("furita_list.feature", "Obtaining the list of the rules with wrong type")
def test_furita_api_list_obtain_with_wrong_type():
    pass


@scenario("furita_list.feature", "Obtaining one rule by id without detalization")
def test_furita_api_list_obtain_one():
    pass


@scenario("furita_list.feature", "Obtaining one rule by id with detalization")
def test_furita_api_list_obtain_one_detailed():
    pass


@scenario("furita_list.feature", "Obtaining the list of the rules without uid")
def test_furita_api_list_without_uid():
    pass


@scenario("furita_list.feature", "Obtaining the list of the rules with wrong uid (uid given but no user with such id)")
def test_furita_api_list_wrong_uid():
    pass


@given("furita is up and running")
def furita_started(context):
    assert context.furita
    context.furita.last_response = None


@given(parsers.parse('new user "{name}"'))
def create_user(context, name):
    context.create_user(name)


@given(parsers.parse('user "{name}" has the following list of the rules:\n{rules_lines}'))
def create_rules(context, name, rules_lines):
    uid = context.get_uid(name)
    rules = get_names_from_lines(rules_lines)

    for rule_name in rules:
        response = context.furita_api.api_edit(uid=uid, name=rule_name)
        assert response.status_code == 200


@given(parsers.parse('user "{name}" has a rule "{rule_name}"'))
def create_named_rule(context, name, rule_name):
    context.furita.just_created_rule[rule_name] = helpers.create_named_rule(context, context.get_uid(name), rule_name)


def obtain_rules(context, uid, type=None, id=None, detailed=False):
    return context.furita_api.api_list(uid=uid, id=id, detailed=detailed, type=type)


@when(parsers.parse('we obtain rules of the user "{name}"'))
def obtain_all_rules_for_the_particular_user(context, name):
    uid = context.get_uid(name)
    context.furita.last_response = obtain_rules(context=context, uid=uid)


@when(parsers.parse('we obtain rules with type "{type}" of the user "{name}"'))
def obtain_rules_for_the_particular_user_by_given_type(context, name, type):
    uid = context.get_uid(name)
    context.furita.last_response = obtain_rules(context=context, uid=uid, type=type)


@when(parsers.parse('we obtain {detailed} rule "{rule_name}" of the user "{name}"'))
def obtain_one_particular_rule_for_the_given_user(context, name, detailed, rule_name):
    assert detailed in ["the", "detailed"]
    uid = context.get_uid(name)
    assert rule_name in context.furita.just_created_rule
    id = context.furita.just_created_rule[rule_name]
    context.furita.last_response = obtain_rules(context=context, uid=uid, id=id, detailed=(detailed == "detailed"))


@when(parsers.parse('we obtain rules of the user ""'))
def obtain_rules_not_for_the_particular_user_at_all(context):
    context.furita.last_response = obtain_rules(context=context, uid=None)


@then(parsers.parse('furita replies with {response_code:d}'))
def furita_replies(context, response_code):
    """ Эта функция имеет смысл только если её вызывать после obtain_rules, """
    """ так как переменная context.furita.last_response устанавливается там и только там """
    assert context.furita.last_response is not None
    assert context.furita.last_response.status_code == response_code


@then(parsers.parse('there are {rules_count:d} rules in the obtained list'))
@then(parsers.parse('there is {rules_count:d} rule in the obtained list'))
def how_many_rules(context, rules_count):
    """ Эта функция имеет смысл только если её вызывать после obtain_rules, """
    """ так как переменная context.furita.last_response устанавливается там и только там """
    json_list = context.furita.last_response.json()

    assert "rules" in json_list
    assert len(json_list["rules"]) == rules_count


@then(parsers.parse('rule\'s "query" property is {filled}'))
def is_rule_detailed(context, filled):
    """ Эта функция имеет смысл только если её вызывать после obtain_rules, """
    """ так как переменная context.furita.last_response устанавливается там и только там """
    assert filled in ["empty", "filled"]
    json_list = context.furita.last_response.json()

    assert "rules" in json_list and len(json_list["rules"]) == 1
    rule = json_list["rules"][0]

    if filled == "filled":
        assert "query" in rule and rule["query"] != ""
    else:
        assert "query" not in rule


@pytest.fixture(scope="function", autouse=True)
def last_rule_id(request, context):
    """ Это всё надо, чтобы для каждого теста сбрасывать только что созданное правило """
    context.furita.just_created_rule = {}

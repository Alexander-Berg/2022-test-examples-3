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
    get_rule
)


scenario = functools.partial(
    pytest_bdd.scenario,
    features_base_dir=get_path("mail/furita/tests/isolated/feature/furita_enable.feature"),
    strict_gherkin=False
)


@scenario("furita_enable.feature", "Calling disable rule method for the existent rule in enabled state")
def test_furita_enable_disabling_existent_rule():
    pass


@scenario("furita_enable.feature", "Calling disable rule method with empty id argument")
def test_furita_enable_disabling_rule_with_empty_id():
    pass


@scenario("furita_enable.feature", "Calling disable rule method without id argument")
def test_furita_enable_disabling_rule_without_id():
    pass


@given("furita is up and running")
def furita_started(context):
    assert context.furita
    context.furita.last_status_code = None


@given(parsers.parse('new user "{name}"'))
def create_user(context, name):
    context.create_user(name)


@when(parsers.parse('we create a rule "{rule_name}" for the user "{user_name}"'))
def create_rule(context, rule_name, user_name):
    uid = context.get_uid(user_name)
    response = context.furita_api.api_edit(uid=uid, name=rule_name)
    assert response.status_code == 200


@then(parsers.parse('user "{user_name}" has the rule "{rule_name}" {rule_mode}'))
def has_user_a_rule(context, user_name, rule_name, rule_mode):
    uid = context.get_uid(user_name)
    mode = get_mode(rule_mode)

    rule = get_rule(context.furita_api, uid, rule_name)

    assert rule is not None and rule["enabled"] == mode


@then(parsers.parse('furita replies with {response_code:d}'))
def furita_replies(context, response_code):
    """ Эта функция имеет смысл только если её вызывать сразу после switch_enable_flag_for_the_rule, """
    """ так как переменная context.last_status_code устанавливается там и только там """
    assert context.furita.last_status_code == response_code


@when(parsers.parse('user "{user_name}" switches {rule_enable} the rule ""'))
def switch_enable_flag_for_the_wrong_rule(context, user_name, rule_enable):
    """ Эта функция нужна для того, чтобы в switch_enable_flag_for_the_rule """
    """ установить параметр rule_name в '' """
    switch_enable_flag_for_the_rule(context, user_name, rule_enable, "")


@when(parsers.parse('user "{user_name}" switches {rule_enable} the rule "{rule_name}"'))
def switch_enable_flag_for_the_rule(context, user_name, rule_enable, rule_name):
    uid = context.get_uid(user_name)
    enable = get_enable(rule_enable)

    rule_id = None
    if rule_name != "":
        rule = get_rule(context.furita_api, uid, rule_name)
        rule_id = rule["id"] if rule else None
    else:
        rule_id = ""

    response = context.furita_api.api_enable(uid, rule_id, enable)
    context.furita.last_status_code = response.status_code


def get_mode(rule_mode):
    """ По слову 'enabled'/'disabled' возщвращает True/False соответственно """
    modes = {
        'enabled': True,
        'disabled': False
    }
    assert rule_mode in modes
    return modes[rule_mode]


def get_enable(rule_enable):
    """ По слову 'on'/'off' возщвращает True/False соответственно """
    switches = {
        'on': True,
        'off': False
    }
    assert rule_enable in switches
    return switches[rule_enable]

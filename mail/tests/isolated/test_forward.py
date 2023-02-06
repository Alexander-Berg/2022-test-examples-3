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
    get_rules
)
from furita import generate_and_store_config


scenario = functools.partial(
    pytest_bdd.scenario,
    features_base_dir=get_path("mail/furita/tests/isolated/feature/furita_forward.feature"),
    strict_gherkin=False
)


@scenario("furita_forward.feature", "Trying to create new forwarding rule while such behaviour is not allowed in the config")
def test_create_forward_rule_for_furita_not_allow_forward():
    pass


@scenario("furita_forward.feature", "Creating forwarding rule while such behaviour is allowed in the config")
def test_create_forward_rule_for_furita_allow_forward():
    pass


@scenario("furita_forward.feature", "Trying to create new forwardwithstore rule while such behaviour is not allowed in the config")
def test_create_forwardwithstore_rule_for_furita_not_allow_forward():
    pass


@scenario("furita_forward.feature", "Creating notify rule while forward and forwardwithstore rules is not allowed in the config")
def test_create_notify_rule_for_furita_not_allow_forward():
    pass


@given(parsers.parse('furita with allow_forward "{allow_forward}" is up and running'))
def furita_started(context, allow_forward):
    params = context.furita.config_params
    params["__FURITA_ALLOW_FORWARD__"] = allow_forward
    generate_and_store_config(params)
    context.furita.restart()

    assert context.furita
    context.furita.last_status_code = None


@given(parsers.parse('new user "{name}"'))
def create_user_for_furita(context, name):
    context.create_user(name)


@when(parsers.parse('create a "{clicker}" rule for user "{user_name}"'))
def furita_create_rule(context, clicker, user_name):
    assert clicker in ['forward', 'forwardwithstore', 'notify']
    uid = context.get_uid(user_name)
    params = {
        'clicker': clicker,
        'auth_domain': 'test.ru',
        'confirm_domain': 'test.ru',
        'from': 'test@test.ru',
    }
    response = context.furita_api.api_edit(uid=uid, name='test_'+clicker, params=params)
    context.furita.last_status_code = response.status_code


@then(parsers.parse('furita replies with {response_code:d}'))
def furita_replies(context, response_code):
    assert context.furita.last_status_code == response_code


@then(parsers.parse('user "{user_name}" has {rules_count:d} rule'))
@then(parsers.parse('user "{user_name}" has {rules_count:d} rules'))
def has_user_a_rule(context, user_name, rules_count):
    uid = context.get_uid(user_name)
    rules = get_rules(context.furita_api, uid)
    assert len(rules) == rules_count

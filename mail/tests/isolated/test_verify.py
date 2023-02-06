import functools

import pytest_bdd
from pytest_bdd import (
    given,
    then,
    when,
    parsers
)

from furita_common import get_path, get_rule
from .verify import make_verification_token


scenario = functools.partial(
    pytest_bdd.scenario,
    features_base_dir=get_path("mail/furita/tests/isolated/feature/furita_verify.feature"),
)


@scenario("furita_verify.feature", "Calling without verification code")
def test_verify_no_code():
    pass


@scenario("furita_verify.feature", "Calling with incorrect verification code")
def test_verify_incorrect_code():
    pass


@scenario("furita_verify.feature", "Verifying rule with correct code")
def test_verify_correct_code():
    pass


@scenario("furita_verify.feature", "Verifying the same rule twice")
def test_verify_correct_code_twice():
    pass


@given("furita is up and running")
def furita_started(context):
    assert context.furita
    context.furita.last_response = None


@given(parsers.parse('new user "{name}"'))
def create_user(context, name):
    context.create_user(name)


def check_rule_verification_status(rule, status):
    action = rule["actions"][0]
    assert action["type"] == "forward" and action["verified"] is status


@given(parsers.parse('user "{name}" has a rule "{rule_name}" forwarding to "{forward_to}"'))
def create_forward_rule(context, name, rule_name, forward_to):
    uid = context.get_uid(name)
    response = context.furita_api.api_edit(
        uid=uid,
        name=rule_name,
        params={
            'clicker': 'forward',
            'forward_address': forward_to,
            'noconfirm': '0',
            'from': '{}@yandex.ru'.format(name),
            'confirm_domain': 'yandex.ru',
            'auth_domain': 'yandex.ru',
        },
    )
    assert response.status_code == 200, response.text
    rule_id = response.json()["id"]

    rule = get_rule(context.furita_api, uid, rule_name)
    assert rule is not None
    check_rule_verification_status(rule, False)

    return rule_id


@when('we verify with code ""')
def verify_no_code(context):
    context.furita.last_response = context.furita_api.api_verify(code='')


@when('we verify with fake code')
def verify_fake_code(context):
    context.furita.last_response = context.furita_api.api_verify(code='fakecode4321')


@when(parsers.parse('we verify with correct code for rule "{rule_name}" of user "{name}" {repeat:d} times'))
def verify_with_code(context, rule_name, name, repeat):
    uid = context.get_uid(name)
    rule = get_rule(context.furita_api, uid, rule_name)
    assert rule is not None
    code = make_verification_token(int(rule["id"]), uid, '{}@yandex.ru'.format(name).encode('utf-8'))
    for i in range(repeat):
        context.furita.last_response = context.furita_api.api_verify(code=code)


@then(parsers.parse('furita replies with {response_code:d}'))
def furita_replies(context, response_code):
    assert context.furita.last_response.status_code == response_code


@then(parsers.parse('the rule "{rule_name}" of user "{name}" is verified'))
def rule_is_verified(context, rule_name, name):
    rule = get_rule(context.furita_api, context.get_uid(name), rule_name)
    assert rule is not None
    check_rule_verification_status(rule, True)

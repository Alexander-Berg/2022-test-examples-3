# -*- coding: utf-8 -*-
import time
import functools
import pytest
import pytest_bdd
from pytest_bdd import (
    then,
    when,
    given,
    parsers
)
from mail.furita.tests.common.utils.utils import (
    get_real_path,
    exec_remote_command
)
from mail.furita.tests.common.furita.furita import Furita
from mail.furita.tests.common.user.user import init_test_user
from mail.furita.tests.common.utils.env import get_env_type

scenario = functools.partial(
    pytest_bdd.scenario,
    features_base_dir=get_real_path("mail/furita/tests/integration/feature/furita_integration.feature"),
    strict_gherkin=False
)

_PARAMS = ['furita_host', 'user', 'password', 'passport_host', 'akita_host', 'wmi_host', 'mops_host', 'x_original',
           'sendbernar_host']
_VALUES = {
    'bigml': (
        'furita-1.furita.qa.furita.mail.stable.qloud-d.yandex.net',
        'robbitter-8961290165@yandex.ru',
        'simple123456',
        'passport.yandex.ru',
        'akita-qa.mail.yandex.net',
        'meta-qa.mail.yandex.net',
        'mops-qa.mail.yandex.net',
        'mail.yandex.ru',
        'sendbernar-qa.mail.yandex.net'
    ),
    'corp': (
        'furita-1.furita.qa.furita-corp.mail.stable.qloud-d.yandex.net',
        'furita-test-13@mail.yandex-team.ru',
        's8Igl3mT/zjX',
        'passport.yandex-team.ru',
        'akitacorp-qa.mail.yandex.net',
        'metacorp-qa.mail.yandex.net',
        'mopscorp-qa.mail.yandex.net',
        'mail.yandex-team.ru',
        'sendbernarcorp-qa.mail.yandex.net'
    )
}


@pytest.mark.parametrize(_PARAMS, [_VALUES[get_env_type()]])
@scenario("furita_integration.feature", "Creating a couple of rules then obtaining them")
def test_create_a_couple_of_rules(furita_host, user, password, passport_host, akita_host, wmi_host, mops_host,
                                  x_original, sendbernar_host):
    pass


@pytest.mark.parametrize(_PARAMS, [_VALUES[get_env_type()]])
@scenario("furita_integration.feature", "Applying a rule to the messages")
def test_apply_a_rule(furita_host, user, password, passport_host, akita_host, wmi_host, mops_host,
                      x_original, sendbernar_host):
    pass


@pytest.mark.parametrize(_PARAMS, [_VALUES[get_env_type()]])
@scenario("furita_integration.feature", "Applying a rule with stars to the messages")
def test_apply_a_rule_with_stars(furita_host, user, password, passport_host, akita_host, wmi_host, mops_host,
                                 x_original, sendbernar_host):
    pass


@pytest.mark.parametrize(_PARAMS, [_VALUES[get_env_type()]])
@scenario("furita_integration.feature", "Check how furita sends confirmation messages")
def test_send_messages(furita_host, user, password, passport_host, akita_host, wmi_host, mops_host,
                       x_original, sendbernar_host):
    pass


@when('we restart furita at <furita_host>')
def restart_furita(context, furita_host):
    response = exec_remote_command(furita_host, "supervisorctl restart furita")
    assert response == "furita: stopped\nfurita: started\n"


@then('furita at <furita_host> is up and running')
def furita_is_up_and_running(context, furita_host):
    context.furita = Furita(host=furita_host)
    context.response = None

    response = context.furita.ping()
    assert response.status_code == 200 and response.text == "pong"


@given(
    'initialised <user> with <password> at <passport_host>, <wmi_host>, <mops_host>, <akita_host>, <sendbernar_host> with <x_original>')
def initailise_user(context, user, password, passport_host, wmi_host, mops_host, akita_host, sendbernar_host,
                    x_original):
    credentials = {
        'login': user,
        'passwd': password
    }
    context.user = init_test_user(credentials, passport_host, akita_host, x_original, wmi_host, mops_host,
                                  sendbernar_host)


@when('we obtain all rules')
def obtain_all_rules(context):
    context.response = context.furita_api.api_list(uid=context.user.uid)
    assert context.response.status_code == 200
    context.response = context.response.json()
    assert "rules" in context.response


@then('furita responded with some list of the rules')
def furita_returns_rules(context):
    assert "rules" in context.response
    assert isinstance(context.response["rules"], list)


@when('remove all obtained rules')
def remove_all_rules(context):
    ids = []
    for rule in context.response["rules"]:
        ids.append(rule["id"])
    if len(ids) != 0:
        context.response = context.furita_api.api_remove(uid=context.user.uid, ids=ids)
        assert context.response.status_code == 200
    else:
        context.response = None


def check_rules_count(context, rules_count):
    assert len(context.response["rules"]) == rules_count


@then('there are no rules')
def rules_count_no_rules(context):
    check_rules_count(context, 0)


@then(parsers.parse('there are {rules_count:d} rules'))
@then(parsers.parse('there is {rules_count:d} rule'))
def rules_count_X_rules(context, rules_count):
    check_rules_count(context, rules_count)


@when(parsers.parse('we create a rule "{rule_name}"'))
def create_rule(context, rule_name):
    context.response = context.furita_api.api_edit(uid=context.user.uid, name=rule_name)
    assert context.response.status_code == 200


@when(parsers.parse('we create the rule which founds "{subj_substr}" string in the subject with name "{rule_name}"'))
def create_specific_rule(context, subj_substr, rule_name):
    context.response = context.furita_api.api_edit(
        uid=context.user.uid,
        name=rule_name,
        params={
            'field1': 'subject',
            'field2': '3',
            'field3': subj_substr,
            'clicker': 'delete'
        }
    )
    assert context.response.status_code == 200


@when('we create selfnotification rule for the <user>')
def create_selfnotification_rule(context, user):
    context.response = context.furita_api.api_edit(
        uid=context.user.uid,
        name='selfnotification test rule',
        params={
            'field1': 'subject',
            'field2': '3',
            'field3': 'BLAH',
            'clicker': 'notify',
            'notify_address': user,
            'lang': 'en',
            'from': user,
            'confirm_domain': 'localhost',
            'auth_domain': 'localhost',
            'noconfirm': '0'
        }
    )
    assert context.response.status_code == 200


def check_envelopes_count(context, folder, envelopes_count):
    fid = context.user.get_fid_by_name(folder)
    assert fid is not None
    envelopes = context.user.get_messages_by_fid(fid)
    assert len(envelopes) == envelopes_count


@then(parsers.parse('there are no envelopes in the folder "{folder_name}"'))
def envelopes_count_no_envelopes(context, folder_name):
    check_envelopes_count(context, folder_name, 0)


@then(parsers.parse('there are {envelopes_count:d} envelopes in the folder "{folder_name}"'))
@then(parsers.parse('there is {envelopes_count:d} envelope in the folder "{folder_name}"'))
def envelopes_count_X_envelopes(context, folder_name, envelopes_count):
    check_envelopes_count(context, folder_name, envelopes_count)


@when(parsers.parse('we remove all envelopes from the folder "{folder_name}"'))
def remove_all_envelopes(context, folder_name):
    fid = context.user.get_fid_by_name(folder_name)
    assert fid is not None
    assert context.user.remove_messages_from_fid(fid)


@when(parsers.parse('we send mail to the <user> with the subject "{subject}"'))
def send_mail(context, user, subject):
    fid = context.user.get_fid_by_name("inbox")
    assert fid is not None
    message_id = context.user.simple_send_message(user, subject)
    retry = 5
    received = False
    while retry > 0:
        envelopes = context.user.get_messages_by_fid(fid)
        for envelope in envelopes:
            if "rfcId" in envelope and envelope["rfcId"] == message_id:
                received = True
                break
        time.sleep(1)
        retry -= 1
    assert received


@then(parsers.parse('there is an envelope with the subject "{subject}" in the "{folder_name}"'))
def envelope_with_subject(context, folder_name, subject):
    fid = context.user.get_fid_by_name(folder_name)
    assert fid is not None
    envelopes = context.user.get_messages_by_fid(fid)
    found = False
    for envelope in envelopes:
        if "subject" in envelope and envelope["subject"] == subject:
            found = True
            break
    assert found


@when(parsers.parse('we apply the rule "{rule_name}"'))
def apply_rule(context, rule_name):
    r = context.furita_api.api_list(uid=context.user.uid)
    assert r.status_code == 200
    r = r.json()
    assert "rules" in r

    rule_id = None
    for rule in r["rules"]:
        if rule["name"] == rule_name:
            rule_id = rule["id"]
            break
    assert rule_id is not None

    r = context.furita_api.api_apply(context.user.uid, rule_id)
    assert r.status_code == 200


@when(parsers.parse('wait {seconds:d} second'))
@when(parsers.parse('wait {seconds:d} seconds'))
def just_wait(context, seconds):
    time.sleep(seconds)


# HELPERS #


@pytest.fixture(scope="function", autouse=True)
def context():
    """ Тут мы будем хранить контекст всех тестов """

    class Context(object):
        pass

    return Context()

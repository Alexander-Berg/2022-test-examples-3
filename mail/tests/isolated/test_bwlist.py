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
    get_names_from_lines
)

scenario = functools.partial(
    pytest_bdd.scenario,
    features_base_dir=get_path("mail/furita/tests/isolated/feature/furita_bwlist.feature"),
    strict_gherkin=False
)


@scenario(
    "furita_bwlist.feature",
    "Creating and obtaining entity from the black or white lists",
    example_converters=dict(
        user=str,
        list_type=str
    )
)
def test_create_and_obtain():
    pass


@scenario(
    "furita_bwlist.feature",
    "Creating and removing the entity from the black or white lists",
    example_converters=dict(
        user=str,
        list_type=str
    )
)
def test_create_and_remove():
    pass


@scenario(
    "furita_bwlist.feature",
    "Creating the same entity twice in the black or white lists",
    example_converters=dict(
        user=str,
        list_type=str,
        errmsg=str
    )
)
def test_create_the_same_twice():
    pass


@scenario(
    "furita_bwlist.feature",
    "Removing unexistent entity from the black or white lists",
    example_converters=dict(
        user=str,
        list_type=str
    )
)
def test_remove_unexistent():
    pass


@scenario(
    "furita_bwlist.feature",
    "Creating entity in the one list type while the same entity is already in another list type",
    example_converters=dict(
        user=str,
        one_list_type=str,
        another_list_type=str
    )
)
def test_add_to_one_list_while_already_in_another_list():
    pass


@given("furita is up and running")
def furita_started(context):
    assert context.furita
    context.furita.last_response = None


@given('new <user>')
def create_user(context, user):
    context.create_user(user)


def add_to_the_list(context, email, list_type, user):
    uid = context.get_uid(user)
    return context.furita_api.api_bwlist_add(uid, list_type, email)


@when(parsers.parse('we add "{email}" to the <list_type> of the <user>'))
def regular_add_to_the_list(context, email, list_type, user):
    context.furita.last_response = add_to_the_list(context, email, list_type, user)


@when(parsers.parse('we add "{email}" to the <one_list_type> of the <user>'))
def add_to_the_one_list_type(context, email, one_list_type, user):
    context.furita.last_response = add_to_the_list(context, email, one_list_type, user)


@when(parsers.parse('we add "{email}" to the <another_list_type> of the <user>'))
def regular_add_to_the_another_list_type(context, email, another_list_type, user):
    context.furita.last_response = add_to_the_list(context, email, another_list_type, user)


@then(parsers.parse('furita replies with {response_code:d}'))
def furita_replies(context, response_code):
    assert context.furita.last_response.status_code == response_code


@then(parsers.parse('furita replies with {response_code:d} and error message is "{errmsg}"'))
def furita_replies_with_error_msg(context, response_code, errmsg):
    assert context.furita.last_response.status_code == response_code
    response = context.furita.last_response.json()
    assert response['status'] == 'error'
    assert response['report'] == errmsg


@when('we obtain the <list_type> of the <user>')
def obtain_list(context, list_type, user):
    uid = context.get_uid(user)
    context.furita.last_response = context.furita_api.api_bwlist(uid, list_type)


@then(parsers.parse('ordered obtained list is the following:\n{email_strings}'))
def last_list_is_the_following(context, email_strings):
    emails = get_names_from_lines(email_strings)
    emails.sort()

    response = context.furita.last_response.json()
    assert "addresses" in response
    addresses = response["addresses"]
    addresses.sort()

    assert addresses == emails


@when(parsers.parse('we remove the following records from the <list_type> of the <user>:\n{email_strings}'))
def remove_from_the_list(context, list_type, user, email_strings):
    uid = context.get_uid(user)
    emails = get_names_from_lines(email_strings)
    context.furita.last_response = context.furita_api.api_bwlist_remove(uid, list_type, emails)


@then('there are no records in the obtained list')
def empty_list(context):
    response = context.furita.last_response.json()
    assert "addresses" in response
    assert len(response["addresses"]) == 0

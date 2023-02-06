import extsearch.geo.kernel.pymod.pytest_bdd_patched  # noqa

from pytest_bdd import given, when, then, parsers

import pytest


@pytest.fixture
def context():
    return {}


@given(parsers.parse('первое число равно {a}'))
def step_first_number_is(context, a):
    context['a'] = int(a)


@given(parsers.parse('второе число равно {b}'))
def step_second_number_is(context, b):
    context['b'] = int(b)


@when('пользователь вычисляет сумму')
def step_sum(context):
    context['s'] = context['a'] + context['b']


@then(parsers.parse('он получает ответ {s}'))
def step_answer(context, s):
    assert context['s'] == int(s)

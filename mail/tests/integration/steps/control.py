# coding: utf-8

from functools import partial

from behave import given, when, then
from hamcrest import (
    assert_that,
    equal_to,
    has_entry,
)
from parse_type import TypeBuilder
from behave import register_type

from mail.doberman.tests.integration.lib.retries_matcher import RetriesMatcher

SLEEP_TIMES = [0.5] * 30


def with_retries(matcher):
    return RetriesMatcher(SLEEP_TIMES, matcher)


def get_doberman_info(context):
    return context.dobby.info()


def send_doberman_command(context, command):
    return context.dobby.command(command)


@given('doberman is running')
@when('I start doberman')
def step_start_doberman(context):
    context.dobby.start()


@given('doberman is stopped')
@when('I stop doberman')
def step_stop_doberman(context):
    context.doberman_cluster.stop()


STATE_MATCHERS = {
    'is running': has_entry('state', has_entry('started', True)),
    'is stopped': has_entry('state', has_entry('started', False)),
}


register_type(
    ProcessStateMatcher=TypeBuilder.make_enum(STATE_MATCHERS)
)


@then('doberman process {satisfy_matcher:ProcessStateMatcher}')
def step_check_doberman_status(context, satisfy_matcher):
    assert_that(
        partial(get_doberman_info, context),
        with_retries(satisfy_matcher)
    )


@when('I send "{command:Words}" command to doberman')
def step_send_control_command(context, command):
    context.doberman_control_response = str(send_doberman_command(context, command), encoding='utf-8')


@then('doberman response is "{response:Words}"')
def step_check_doberman_response(context, response):
    assert_that(context.doberman_control_response.strip(), equal_to(response))


@given('doberman acquired worker id "{worker_id}"')
def step_doberman_acquired_worker_id(context, worker_id):
    try:
        if get_doberman_info(context)['state']['started']:
            if context.dobby.command('stat')['job']['worker_id'] == worker_id:
                return
    except:
        pass

    context.execute_steps(u'''
       Given doberman is stopped
         And free worker id "dobby"
        When I start doberman
    ''')

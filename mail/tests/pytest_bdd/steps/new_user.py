# coding: utf-8
from pytest_bdd import parsers

from pymdb.operations import Init
from pymdb.queries import Queries
from pymdb.tools import find_free_uid, mark_user_as_moved_from_here
from tests_common.pytest_bdd import given, when, then

DEFAULT_USER = 'Anonymous'


def set_uid_and_queries(context, uid):
    context.uid = uid
    context.qs = Queries(context.conn, context.uid)
    return context.uid


def init_user(operation_maker, uid, need_welcomes=False, need_filters=False):
    return operation_maker(Init, uid)(need_welcomes, need_filters)


def make_new_user(context, name):
    assert name not in context.users, \
        'User name: %s already used %r' % (
            name, context.users)
    uid = find_free_uid(context.conn, max_in_memory_uid=max(context.users.values()) if context.users else 0)
    context.users[name] = uid
    return uid


@given(u'new user')
def step_new_user(context):
    set_uid_and_queries(context, make_new_user(context, DEFAULT_USER))


@when(u'we make user "{name:UserName}"')
def step_new_named_user(context, name):
    set_uid_and_queries(context, make_new_user(context, name))


@given(u'new initialized user')
def step_given_new_initialized_user(context):
    make_new_initialized_user(context)


@when(u'we initialize new user')
def step_when_new_initialized_user(context):
    make_new_initialized_user(context)


def make_new_initialized_user(context):
    uid = make_new_user(context, DEFAULT_USER)
    set_uid_and_queries(context, uid)
    init_user(context.make_operation, uid).commit()


@given(u'new initialized user "{name:UserName}"')
def step_given_new_named_initialized_user(context, name):
    make_new_named_initialized_user(**locals())


@when(u'we initialize new user "{name:UserName}"')
def step_when_new_named_initialized_user(context, name):
    make_new_named_initialized_user(**locals())


def make_new_named_initialized_user(context, name):
    uid = make_new_user(context, name)
    set_uid_and_queries(context, uid)
    init_user(context.make_operation, uid).commit()


@when(u'"{name:UserName}" comeback')
@when(u'we switch to "{name:UserName}"')
def step_switch_user(context, name):
    assert hasattr(context, 'users'), \
        'OMG no users defined?'
    assert name in context.users, \
        'Can\'t find %s in users: %r' % (name, context.users)
    set_uid_and_queries(context, context.users[name])


@when('we initialize user with sending welcome mails')
def step_init_user_with_welcome_emails(context):
    init_user(context.make_operation, context.uid, True).wait().commit()


@when('we initialize user with filters')
def step_init_user_with_filters(context):
    init_user(context.make_operation, context.uid, False, True).wait().commit()


@when('we initialize user as "{op_id}"')
def step_init_user_async(context, op_id):
    context.operations[op_id] = init_user(context.make_async_operation, context.uid)


def check_where_is_user(context, is_here):
    user = context.qs.user()
    assert user.is_here == is_here, \
        'Expect is_here=%r but %r found on %r' % (
            is_here, user.is_here, user)


def check_if_user_deleted(context, is_deleted):
    user = context.qs.user()
    assert (user.is_deleted or False) == is_deleted, \
        'Expect is_deleted=%r but %r found on %r' % (
            is_deleted, user.is_deleted, user)


@then(r'(?:user|he) is here', parse_builder=parsers.re)
def step_user_is_here(context):
    check_where_is_user(context, is_here=True)


@then(r'(?:user|he) is not here', parse_builder=parsers.re)
def step_user_is_not_here(context):
    check_where_is_user(context, is_here=False)


@then(r'(?:user|he) is deleted', parse_builder=parsers.re)
def step_user_is_deleted(context):
    check_if_user_deleted(context, is_deleted=True)


@then(r'(?:user|he) is not deleted', parse_builder=parsers.re)
def step_user_is_not_deleted(context):
    check_if_user_deleted(context, is_deleted=False)


@when(r'we transfer (?:him|user) to another shard', parse_builder=parsers.re)
def step_transfer_him_to_another_shard(context):
    mark_user_as_moved_from_here(context.conn, context.uid)

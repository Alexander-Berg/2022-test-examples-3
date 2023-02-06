# coding: utf-8

import logging

from hamcrest import assert_that, has_entry

from ora2pg.sharpei import init_in_sharpei
from mail.pypg.pypg.common import qexec, describe_cursor
from mail.pypg.pypg.query_conf import load_from_package
from tests_common import (
    add_user as add_user_to_fbb,
    make_maildb_conn,
    make_maildb_conn_by_shard_id,
)
from tests_common.pytest_bdd import given, when, then
from tests_common.user import (
    make_new_user,
    remember_user_in_context,
    register_user,
    register_user_in_mdb_and_fbb,
    make_user_oneline,
)

log = logging.getLogger(__name__)

QUERIES = load_from_package(__package__, __file__)


def add_user_in_stoplist(conn, uid):
    qexec(
        conn,
        QUERIES.add_user_to_stoplist,
        uid=uid
    )


@given('new user')
@given('new user in first shard')
@given('new user with pg db in blackbox')
def given_new_user(context):
    return given_new_user_impl(**locals())


@when('we make new user')
def when_new_user(context):
    return given_new_user_impl(**locals())


@given('new user "{user_name:w}"')
def given_new_user_named(context, user_name):
    return given_new_user_impl(**locals())


@when('we make new user "{user_name:w}"')
def when_we_make_new_user_named(context, user_name):
    return given_new_user_impl(**locals())


def given_new_user_impl(context, user_name=None):
    return make_user_oneline(context, user_name)


@given('new empty user')
@given('new empty user in first shard')
def given_new_empty_user(context):
    return make_user_oneline(context, empty=True)


@given('new empty user "{user_name:w}"')
def given_new_empty_user_named(context, user_name):
    return make_user_oneline(context, user_name, empty=True)


@given('new user "{user_name:w}" with messages in mulca')
def step_new_user_with_messages_in_mulca(context, user_name):
    user = make_new_user(context, user_name)
    register_user(context, user, user_name)
    add_user_to_fbb(context.fbbdb_conn, context.user)


@given('new user without mail suid and db')
def step_user_without_mail_sid(context):
    user = make_new_user(context)
    user.suid = None
    user.db = None
    register_user_in_mdb_and_fbb(context, user)


@given('new user absent in blackbox')
def step_new_user_absent_in_blackbox(context):
    register_user(context, make_new_user(context))


@given('new unregistered user "{user_name:w}" with pg db in blackbox')
def step_unregistered_user_with_pg_db(context, user_name):
    user = make_new_user(context, user_name)
    user.db = 'pg'
    remember_user_in_context(context, user, user_name)
    add_user_to_fbb(context.fbbdb_conn, context.user)


@given('new user uninitialized in mdb')
def given_user_unregistered_in_mdb(context):
    return given_user_unregistered_in_mdb_impl(**locals())


@given('new user "{user_name:w}" uninitialized in mdb')
def given_user_unregistered_in_mdb_named(context, user_name):
    return given_user_unregistered_in_mdb_impl(**locals())


def given_user_unregistered_in_mdb_impl(context, user_name=None):
    user = make_new_user(context, user_name)
    remember_user_in_context(context, user, user_name)
    init_in_sharpei(
        uid=user.uid,
        dsn=context.config.sharddb,
        allow_inited=False,
        shard_id=context.config.default_shard_id)
    add_user_to_fbb(context.fbbdb_conn, user)


@given('new user in stoplist')
def step_new_in_stoplist(context):
    user = make_new_user(context)
    register_user(context, user)
    add_user_to_fbb(context.fbbdb_conn, context.user)
    add_user_in_stoplist(context.huskydb_conn, context.user.uid)
    log.info('Current user is %r', context.user)


@given('he is in "{state}" state with "{notifies_count}" notifies')
def step_given_he_is_in_state(context, state, notifies_count):
    with make_maildb_conn(
        uid=context.user.uid,
        sharpei=context.config.sharpei,
        maildb_dsn_suffix=context.config.maildb_dsn_suffix,
    ) as conn:
        qexec(
            conn=conn,
            query=QUERIES.set_user_state,
            uid=context.user.uid,
            state=state,
            notifies_count=notifies_count,
        )


@given('he is in "{state}" archivation state')
def step_given_he_is_in_archivation_state(context, state):
    with make_maildb_conn(
        uid=context.user.uid,
        sharpei=context.config.sharpei,
        maildb_dsn_suffix=context.config.maildb_dsn_suffix,
    ) as conn:
        qexec(
            conn=conn,
            query=QUERIES.set_user_archivation_state,
            uid=context.user.uid,
            state=state,
        )


@then('user is not in shards.users')
def step_user_not_in_shards_user(context):
    cur = qexec(
        context.sharddb_conn,
        QUERIES.get_user_shard_id,
        uid=context.user.uid,
    )
    assert not cur.fetchone()


@then('user is in shards.users')
def step_user_exists_in_shards_user(context):
    cur = qexec(
        context.sharddb_conn,
        QUERIES.get_user_shard_id,
        uid=context.user.uid,
    )
    assert cur.fetchone()[0] == 1


@then('user is not in shards.deleted_users')
def step_user_not_in_shards_deleted_user(context):
    cur = qexec(
        context.sharddb_conn,
        QUERIES.get_deleted_user_shard_id,
        uid=context.user.uid,
    )
    assert not cur.fetchone()


@then('user is in shards.deleted_users')
def step_user_exists_in_shards_deleted_user(context):
    cur = qexec(
        context.sharddb_conn,
        QUERIES.get_deleted_user_shard_id,
        uid=context.user.uid,
    )
    assert cur.fetchone()[0] == 1


def one_row_as_dict(cur):
    row = cur.fetchone()
    if not row:
        return row
    return dict(zip(describe_cursor(cur), row))


@then('user is not here')
def step_user_is_not_here(context):
    with make_maildb_conn(
        uid=context.user.uid,
        sharpei=context.config.sharpei,
        maildb_dsn_suffix=context.config.maildb_dsn_suffix,
    ) as conn:
        cur = qexec(
            conn=conn,
            query=QUERIES.get_user,
            uid=context.user.uid,
        )
        assert_that(
            one_row_as_dict(cur),
            has_entry('is_here', False)
        )


@then('user {matcher:HasMetadataMatcher} in first shard')
def step_user_has_not_metadata(context, matcher):
    with make_maildb_conn_by_shard_id(
        sharpei=context.config.sharpei,
        shard_id=context.config.default_shard_id,
        maildb_dsn_suffix=context.config.maildb_dsn_suffix,
    ) as conn:
        cur = qexec(
            conn=conn,
            query=QUERIES.get_user,
            uid=context.user.uid,
        )
        assert_that(one_row_as_dict(cur), matcher)


@given(u'new user with POP3 meta in first shard')
def step_new_user_with_pop3_meta(context):
    context.execute_steps(
        u'''Given new user
              And he has POP3 meta in mdb'''
    )


@given(u'new user with deleted messages in first shard')
def given_new_user_with_deleted_messages(context):
    return given_new_user_with_deleted_messages_impl(**locals())


@given(u'new user "{user_name:w}" with deleted messages in first shard')
def given_new_user_with_deleted_messages_named(context, user_name):
    return given_new_user_with_deleted_messages_impl(**locals())


def given_new_user_with_deleted_messages_impl(context, user_name=None):
    user_name = '"{}"'.format(user_name) if user_name else ''
    context.execute_steps(
        u'''When we make new user {}
            Given he delete some messages from inbox
         '''.format(user_name)
    )


@when(u'we have new user with collectors in first shard')
def step_new_user_with_collectors(context):
    context.execute_steps(
        u'''When we make new user
            And he has collectors'''
    )


@given('new mailish user in first shard')
def step_initialize_as_mailish(context):
    user = make_new_user(context)
    register_user(context, user, is_mailish=True)
    add_user_to_fbb(context.fbbdb_conn, user)
    log.info('Current user is %r', context.user)


@given('new user with tabs in first shard')
def step_initialize_with_tabs(context):
    context.execute_steps(
        u'''Given new user
            And user has tabs'''
    )


@given('new user with contacts in first shard')
def step_initialize_with_contacts(context):
    context.execute_steps(
        u'''Given new user
            And user has contacts'''
    )

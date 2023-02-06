# coding: utf-8

import logging
from contextlib import contextmanager
from datetime import datetime

from hamcrest import (
    is_,
    not_,
    assert_that,
    has_properties,
    empty,
    has_length,
    only_contains,
    all_of,
    has_entry,
)
from dateutil.tz import tzlocal

from ora2pg.sharpei import get_connstring_by_id
from pymdb import operations as OPS
from pymdb.queries import Queries
from pymdb.vegetarian import SAMPLE_STIDS
from mail.pypg.pypg.common import transaction, qexec
from mail.pypg.pypg.query_conf import load_from_my_file
from mail.pypg.pypg.query_handler import ExpectOneItemError
from tests_common.pytest_bdd import given, then

log = logging.getLogger(__name__)
Q = load_from_my_file(__file__)
EPOCH = datetime.fromtimestamp(0, tzlocal())


@contextmanager
def mdb_connection(context, shard_id=None):
    dsn = get_connstring_by_id(
        sharpei=context.config.sharpei,
        shard_id=shard_id or context.config.default_shard_id,
        dsn_suffix=context.config.maildb_dsn_suffix)
    with transaction(dsn) as conn:
        yield conn


@given('there are no tasks in storage delete queue')
def step_clear_storage_delete_queue(context):
    with mdb_connection(context) as conn:
        qexec(conn, Q.clear_storage_delete_queue)


@given('user has "{count:d}" deleted messages')
def step_has_deleted_messages(context, count):
    context.execute_steps(u'''
        When user has "{count}" messages
    '''.format(count=count))
    with mdb_connection(context) as conn:
        qs = Queries(conn, context.user.uid)
        folder = qs.folder_by_type('inbox')
        mids = [m['mid'] for m in qs.messages(fid=folder.fid)]
        OPS.DeleteMessages(conn, context.user.uid)(mids=mids)


@given('user has "{count:d}" stids in storage delete queue')
def step_has_messages_in_storage_del_q(context, count):
    with mdb_connection(context) as conn:
        qexec(conn, Q.fill_storage_delete_queue,
              uid=context.user.uid,
              stids=SAMPLE_STIDS[:count])
    context.execute_steps(u'''
        Then storage delete queue has "{count}" items with deleted date not in past
    '''.format(count=count))


def get_user(context):
    user = context.user
    try:
        with mdb_connection(context) as conn:
            user = Queries(conn, user.uid).user()
            return user
    except ExpectOneItemError:
        return None


@then('user is deleted with purge_date {in_past:InPastMatcher}')
def check_user_is_here(context, in_past):
    user = get_user(context)
    assert_that(user, has_properties(
        is_deleted=True,
        purge_date=in_past,
    ))


@then('user does not exist in our shard')
def check_user_not_exist(context):
    user = get_user(context)
    assert user is None, 'User exists: %s' % user


def get_storage_delete_queue(context):
    user = context.user
    with mdb_connection(context) as conn:
        queue = Queries(conn, user.uid).storage_delete_queue()
        return queue


@then('storage delete queue is empty')
def check_storage_delete_queue_empty(context):
    assert_that(get_storage_delete_queue(context), empty())


@then('storage delete queue is not empty')
def check_storage_delete_queue_not_empty(context):
    assert_that(get_storage_delete_queue(context), is_(not_(empty())))


@then('storage delete queue has "{count:d}" items with deleted date {in_past:InPastMatcher}')
def check_storage_delete_queue_empty_counted_dated(context, count, in_past):
    assert_that(get_storage_delete_queue(context), all_of(
        has_length(count),
        only_contains(has_entry('deleted_date', in_past)),
    ))

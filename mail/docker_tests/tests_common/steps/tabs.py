# coding: utf-8

import logging

from parse_type import TypeBuilder
from hamcrest import (
    assert_that,
    contains_inanyorder,
)

from pymdb import operations as OPS
from pymdb.queries import Queries
from mail.pypg.pypg.common import qexec
from mail.pypg.pypg.query_conf import load_from_package
from tests_common.pytest_bdd import BehaveParser, given, then
from ora2pg.tools import (
    DEFAULT_TAB,
    create_tabs,
    mark_user_as_can_read_tabs,
)

from tests_common.steps.mdb import current_user_connection

log = logging.getLogger(__name__)
Q = load_from_package(__package__, __file__)


def extra_parsers():
    return dict(
        TabsList=TypeBuilder.with_many(TypeBuilder.make_choice(['relevant', 'news', 'social']), listsep=","),
    )


BehaveParser.extra_types.update(extra_parsers())


@given('user does not have tabs')
def step_user_without_tabs(context):
    uid = context.user.uid
    with current_user_connection(context) as conn:
        qexec(conn, Q.delete_all_tabs, uid=uid)


@then('user has tabs "{tabs_list:TabsList}"')
def step_user_has_tabs(context, tabs_list):
    uid = context.user.uid
    with current_user_connection(context) as conn:
        qs = Queries(conn, uid)
        tabs = qs.tabs()
        assert_that([t.tab for t in tabs], contains_inanyorder(*tabs_list))


def get_can_read_tabs(context):
    uid = context.user.uid
    with current_user_connection(context) as conn:
        qs = Queries(conn, uid)
        flag = qs.user().can_read_tabs
        setting = qexec(conn, Q.get_can_read_tabs_setting, uid=uid).fetchone()[0] == 'on'
        return flag, setting


@then('user cannot read tabs')
def step_user_cannot_read_tabs(context):
    flag, setting = get_can_read_tabs(context)
    assert not flag, 'DB flag should be False'
    assert not setting, 'Setting should be False'


@then('user can read tabs')
def step_user_can_read_tabs(context):
    flag, setting = get_can_read_tabs(context)
    assert flag, 'DB flag should be True'
    assert setting, 'Setting should be True'


@given('user has tabs')
def step_given_user_has_tabs(context):
    uid = context.user.uid
    with current_user_connection(context) as conn:
        create_tabs(conn, uid, [DEFAULT_TAB])
        qs = Queries(conn, uid)
        inbox = qs.folder_by_type('inbox')
        mids = [m['mid'] for m in qs.messages(fid=inbox.fid)]
        if len(mids) > 0:
            OPS.MoveMessages(conn, uid)(mids, inbox.fid, DEFAULT_TAB)
        mark_user_as_can_read_tabs(conn, uid)


def set_can_read_tabs(context, value):
    uid = context.user.uid
    with current_user_connection(context) as conn:
        qexec(conn, Q.set_can_read_tabs, uid=uid, value=value)


@given('user cannot read tabs')
def step_given_user_cannot_read_tabs(context):
    set_can_read_tabs(context, False)


@given('user can read tabs')
def step_given_user_can_read_tabs(context):
    set_can_read_tabs(context, True)

# coding: utf-8

import logging

from pytest_bdd import parsers

from pymdb.operations import (
    GetOrCreateTab,
    ResetTabUnvisited,
    ResetTabFresh,
)
from pymdb.queries import TabDoesNotExist
from tests_common.pytest_bdd import given, then, when  # pylint: disable=E0611
from tools import ok_
from mail.pypg.pypg.common import qexec
from mail.pypg.pypg.query_conf import load_from_my_file
from .lists import compare_list
from .folder import parse_counter, parse_flag

from hamcrest import (
    assert_that,
    has_length,
    equal_to,
    less_than_or_equal_to,
    greater_than,
    has_properties,
)

Q = load_from_my_file(__file__)

log = logging.getLogger(__name__)


@given('user does not have tabs')
def step_rm_all_tabs(context):
    qexec(
        context.conn,
        Q.clear_tabs,
        uid=context.uid,
    )
    context.conn.commit()


def add_returned_tab(context, id, tab):
    context.returned_tabs = getattr(context, 'returned_tabs', dict())
    context.returned_tabs[id] = tab


@when('we create tab "{tab_type:w}" as "{result:OpID}"')
def step_create_tab_as(context, tab_type, result):
    op = context.make_operation(GetOrCreateTab)(tab_type)
    op.commit()
    add_returned_tab(context, result, op.result[0])


@when('we create tab "{tab_type:w}"')
def step_create_tab(context, tab_type):
    GetOrCreateTab(context.conn, context.uid)(tab_type).commit()


@when('we create unvisited tab "{tab_type:w}"')
def step_create_unvisited_tab(context, tab_type):
    GetOrCreateTab(context.conn, context.uid)(tab_type).commit()
    qexec(
        context.conn,
        Q.unvisit_tab,
        uid=context.uid,
        tab=tab_type,
    )
    context.conn.commit()


@when('we create tab "{tab_type:w}" with "{fresh_count:d}" fresh')
def step_create_tab_with_fresh(context, tab_type, fresh_count):
    GetOrCreateTab(context.conn, context.uid)(tab_type).commit()
    qexec(
        context.conn,
        Q.set_tab_fresh,
        uid=context.uid,
        tab=tab_type,
        count=fresh_count,
    )
    context.conn.commit()


def check_tab_exist(context, tab_type):
    try:
        return context.qs.tab_by_type(tab_type)
    except TabDoesNotExist:
        pass
    return None


@then('user has tab "{tab_type:w}" as "{result:OpID}"')
def step_has_tab_with_type_as(context, tab_type, result):
    found = check_tab_exist(context, tab_type)
    ok_(found is not None)
    add_returned_tab(context, result, found)


@then('user has tab "{tab_type:w}"')
def step_has_tab_with_type(context, tab_type):
    found = check_tab_exist(context, tab_type)
    ok_(found is not None)


@then('user does not have tab "{tab_type:w}"')
def step_no_tab_with_type(context, tab_type):
    found = check_tab_exist(context, tab_type)
    ok_(found is None)


@then('tabs "{first:OpID}" and "{second:OpID}" are equal')
def step_tabs_equal(context, first, second):
    assert_that(context.returned_tabs[first], equal_to(context.returned_tabs[second]))


@when('we reset tab "{tab_type:w}"')
def step_reset_tab_unvisited(context, tab_type):
    ResetTabUnvisited(context.conn, context.uid)(tab_type).commit()


@when('we reset fresh on tab "{tab_type:w}"')
def step_reset_tab_fresh(context, tab_type):
    ResetTabFresh(context.conn, context.uid)(tab_type).commit()


@then(r'tab "(?P<tab_type>\w+)" is\s*(?P<unvisited>(?:not)?) unvisited', parse_builder=parsers.re)
def step_check_unvisited_flag(context, tab_type):
    found = check_tab_exist(context, tab_type)
    ok_(found is not None)
    assert_that(found.unvisited, equal_to(parse_flag(context.args['unvisited'])))


COUNTERS_RE = r'zero|no|one|"\d+"'
TAB_COUNTERS_RE = r'''
tab "(?P<tab_type>\w+)" has
(?:
(,| and)?\s*
(?:
(?:(?P<message_unseen>({0})) unseen)|
(?:(?P<message_count>({0})) messages?)|
(?:(?P<message_size>({0})) size)|
(?:(?: at)? revision "(?P<revision>(\d+))")
)
)+
'''.format(COUNTERS_RE).strip().replace('\n', '')


@then(TAB_COUNTERS_RE, parse_builder=parsers.re)
def step_check_tab_counters(context, tab_type):
    kwargs = context.args
    counters = {}
    for k in [
            'message_count',
            'message_unseen',
            'message_size',
            'revision']:
        val = parse_counter(kwargs[k])
        if val is not None:
            counters[k] = val

    tab = context.qs.tab_by_type(tab_type)
    assert_that(tab, has_properties(counters))

    if counters.get('message_count') is not None:
        tab_messages = context.qs.messages_by_tab(tab=tab_type)
        assert_that(tab_messages, has_length(counters['message_count']))
        if counters['message_count'] > 0:
            if counters.get('message_unseen') is not None:
                assert_that([m for m in tab_messages if not m['seen']], has_length(counters['message_unseen']))
            if counters.get('message_size') is not None:
                assert_that(sum([int(m['size']) for m in tab_messages]), equal_to(counters['message_size']))
            if counters.get('revision') is not None:
                assert_that(max([m['revision'] for m in tab_messages]), less_than_or_equal_to(counters['revision']))


@then(r'tab "(?P<tab_type>\w+)" has (?P<fresh_count>({})) fresh'.format(COUNTERS_RE), parse_builder=parsers.re)
def step_check_fresh_counter(context, tab_type, fresh_count):
    fresh_count = parse_counter(fresh_count)
    found = check_tab_exist(context, tab_type)
    ok_(found is not None)
    assert_that(found.fresh_count, equal_to(fresh_count))


@then('tab "{tab_type:w}" is empty at revision "{revision:d}"')
def step_check_tab_empty(context, tab_type, revision):
    context.execute_steps('''
        Then tab "{type}" has no messages, no unseen, zero size at revision "{revision}"
    '''.format(type=tab_type, revision=revision))


@then('tab "{tab_type:w}" is not empty')
def step_check_tab_not_empty(context, tab_type):
    tab = context.qs.tab_by_type(tab_type)
    assert_that(tab.message_count, greater_than(0))


TAB_RE_T = r'''in tab "(?P<tab_type>\w+)" there (?:(are|is)) (?:(?P<count>({0})) {1}s?)'''
TAB_MESSAGES_RE = TAB_RE_T.format(COUNTERS_RE, 'message')
TAB_THREADS_RE = TAB_RE_T.format(COUNTERS_RE, 'thread')


@then(TAB_MESSAGES_RE, parse_builder=parsers.re)
def step_check_messages_in_tab(context, tab_type, count):
    count = parse_counter(count)
    tab_messages = context.qs.messages_by_tab(tab=tab_type)
    compare_list(
        context,
        'message',
        tab_messages,
        count,
        'mid',
        {'rule': 'thread_rule'}
    )


@then(TAB_THREADS_RE, parse_builder=parsers.re)
def step_check_threads_in_tab(context, tab_type, count):
    count = parse_counter(count)
    tab_threads = context.qs.threads_by_tab(tab=tab_type)
    compare_list(
        context,
        'thread',
        tab_threads,
        count,
        'tid',
        {
            'count': 'message_count',
            'unseen': 'message_unseen',
        }
    )


@then('user has tabs initialized at revision "{revision:d}"')
def step_initialized_at_rev_tabs(context, revision):
    tabs = context.qs.tabs()
    check_types = (row['type'] for row in context.table)
    for typ in check_types:
        tab = [t for t in tabs if t.tab == typ]
        ok_(tab, "Can't find %s in tabs: %r" % (typ, tabs))
        tab = tab[0]
        ok_(
            tab.message_count == 0
            and tab.message_unseen == 0
            and tab.revision == revision,
            "tab {0} not 'just initialized'".format(tab)
        )


@then('user has just initialized tabs')
def step_just_initialized_tabs(context):
    step_initialized_at_rev_tabs(context, 1)

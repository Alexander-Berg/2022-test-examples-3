# -*- coding: utf-8 -*-

import sys
# import os
import traceback
import pytest
import logging
from copy import deepcopy

from util.blackbox_session import BlackboxSession
from util.const import TLD, TEXT
from util.params import get_beta_host
from util.params import get_test_user_login, get_test_user_passwd
from util.params import get_test_user_xml_key, get_test_user_xml_ip
from util.query import WebQuery
from util.tsoy import TSoY
from util.helpers import Auth, TerminateTest


# Заводим сессию в Blackbox один раз для всех тестов
@pytest.fixture(scope='session')
def blackbox(request):
    return BlackboxSession(
        login=get_test_user_login(),
        password=get_test_user_passwd()
    )


# Залогин для XML
# Login: robot-srch-int-tests
# https://yandex.ru/search/xml?user=robot-srch-int-tests&key=03.1066981381:b8102598b7367fbe969e91a8f965651a
@pytest.fixture(scope='session')
def xml_auth(request):
    return {
        "ip": get_test_user_xml_ip(),
        "user": get_test_user_login(),
        "key": get_test_user_xml_key()
    }


# Заводим сессию с кукой одину для всех тестов
@pytest.fixture(scope='session')
def kuka(blackbox):
    bb = deepcopy(blackbox)
    for tld in TLD.AUTH_TLD:
        Auth.GetCookieRelevanceByTld(bb, tld)
    return bb


# Генерируем для каждого теста пустой Query
@pytest.fixture(scope='function')
def query():
    beta_host = get_beta_host()
    return WebQuery(beta_host=beta_host)


@pytest.fixture(scope='function')
def xml_query(method, path):
    beta_host = get_beta_host()
    q = WebQuery(beta_host=beta_host)
    if method is not None:
        q.SetMethod(method)
    if path is not None:
        q.SetPath(path)
    q.SetParams({
        'text': None,
    })
    if method is not None and method == 'POST':
        xml_data = """<?xml version="1.0" encoding="utf-8"?><request><query>{query}</query><groupings>{groupings}</groupings></request>""".format(
            query=TEXT,
            groupings='<groupby attr="d" mode="deep" groups-on-page="10" docs-in-group="1" />'
        )
        q.SetContentType(content_type='application/xml', charset='utf-8')
        q.SetData(xml_data)
    else:
        q.SetParams({
            'query': TEXT
        })
    return q


# Статистика по TSoY вызовам
# Нужно придумать как сквозь ya tool прорастать
# def pytest_report_teststatus(report):
#     if report.when == 'teardown':
#         line = '{} SoY requests:\t"{}"'.format(report.nodeid, TSoY.Size())
#         report.sections.append(('TSoY stat', line))
#
#
# def pytest_terminal_summary(terminalreporter, exitstatus):
#     reports = terminalreporter.getreports('')
#     content = os.linesep.join(text for report in reports for secname, text in report.sections)
#     if content:
#         terminalreporter.ensure_newline()
#         terminalreporter.section('TSoY STAT', sep='-', blue=True, bold=True)
#         terminalreporter.line(content)


def pytest_runtestloop(session):
    if session.config.option.collectonly:
        return True

    def getnextitem(i):
        try:
            return session.items[i+1]
        except IndexError:
            return None

    items_second_pass = []
    # First pass
    logging.debug("First pass. TSoY.mode={}".format(TSoY.mode))
    for i, item in enumerate(session.items):
        logging.debug("i={}".format(i))
        nextitem = getnextitem(i)
        tsoy_len1 = TSoY.Size()
        item_id = '{}'.format(item)
        if nextitem is None and len(items_second_pass) > 0:
            item.config.hook.pytest_runtest_protocol(item=item, nextitem=items_second_pass[0]['item'])
        else:
            item.config.hook.pytest_runtest_protocol(item=item, nextitem=nextitem)
        if session.shouldstop:
            raise session.Interrupted(session.shouldstop)
        tsoy_len2 = TSoY.Size()
        if tsoy_len1 != tsoy_len2:
            items_second_pass.append({'item': item, 'nextitem': None, 'item_id': item_id})
            if len(items_second_pass) > 1:
                items_second_pass[-2]['nextitem'] = items_second_pass[-1]['item']
            tsoy_len1 = tsoy_len2

    # Go to soy
    # print('TSoY_PASS LEN = {}'.format(len(items_second_pass)), file=sys.stderr)

    try:
        TSoY.process_queries()
    except TerminateTest as e:
        logging.exception("Exception: {}".format(e))
        raise session.Interrupted(True)
    except BaseException:
        ex_type, ex_value, ex_traceback = sys.exc_info()
        trace_back = traceback.extract_tb(ex_traceback)
        logging.exception("EXCEPTION [{}]: {}".format(ex_type.__name__, ex_value))
        for trace in trace_back:
            logging.error('{}:{}: in {}\n{}'.format(trace[0], trace[1], trace[2], trace[3]))

    # Second pass
    TSoY.mode = 'test'
    logging.debug("Second pass. TSoY.mode={} len(items_second_pass)={}".format(TSoY.mode, len(items_second_pass)))
    for i, it in enumerate(items_second_pass):
        logging.debug("Second pass. i={}".format(i))
        # SECOND_PASS = {'item': <Function 'test_long_text_redirect'>, 'nextitem': None, 'item_id': "<Function 'test_long_text_redirect'>"}
        # print('SECOND_PASS = {}'.format(it), file=sys.stderr)
        item.config.hook.pytest_runtest_protocol(item=it['item'], nextitem=it['nextitem'])
        if session.shouldstop:
            logging.debug("Should stop i={}".format(i))
            raise session.Interrupted(session.shouldstop)
    logging.debug("Before return")
    return True

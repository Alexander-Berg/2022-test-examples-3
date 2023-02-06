# -*- coding: utf-8 -*-

import logging

from mapreduce.yt.python.yt_stuff import YtConfig, YtStuff
import pytest

from app import App
from context import SessionContext
from yt_helper import YtHelper


@pytest.fixture(scope='session')
def local_yt(request):
    yt = YtStuff(YtConfig(wait_tablet_cell_initialization=True))
    yt.start_local_yt()
    request.addfinalizer(yt.stop_local_yt)
    return yt


@pytest.fixture(scope='session')
def new_context(local_yt):
    return SessionContext()


@pytest.fixture(scope='session')
def session_context(new_context: SessionContext, local_yt: YtStuff):
    logging.info('Running app')
    session_context = new_context

    yt_helper = YtHelper(local_yt.yt_client, local_yt.get_server())

    order_checker = session_context.order_checker
    table_mapping_checker = session_context.table_mapping_checker
    app = App(yt_helper)
    app.run(order_checker.get_tables())

    tables_to_read = set(order_checker.get_tables_to_read() + table_mapping_checker.get_tables_to_read())
    table_data = yt_helper.read_tables(tables_to_read)
    logging.info('Got table data: %s', table_data)
    lb_data = app.read_notifications()
    session_context.results.update(table_data, lb_data)
    return session_context


@pytest.fixture(scope='session', autouse=True)
def init_tests(request, new_context):
    logging.info('Preparing context')
    session_context = new_context
    session = request.node
    for test in session.items:
        if 'session_context' in test.fixturenames:
            test.obj(session_context)
    session_context.finish_preparing()
